/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */
package password.manager.app.persistence

import password.manager.app.singletons.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function
import java.util.function.Supplier
import kotlin.concurrent.withLock

/**
 * Manages the lifecycle of transactions and provides a convenient API for transactional operations.
 *
 * This manager creates transactions and drives them to their outcome, committing them when every operation
 * has succeeded and rolling them back otherwise. A transaction itself is never handed out: it exists only
 * for the duration of the call that created it.
 *
 * The API comes in two families, both of which take the work as a factory, invoked once the transaction
 * exists. The `execute` ones take the operation itself and start it on a virtual thread executor owned by
 * this manager. The `complete` ones take something that starts the operation elsewhere and returns its
 * future, which they only await; they are what a caller needs when the work belongs to another thread,
 * such as the JavaFX application one. Both families have a `Batch` variant that puts one operation per
 * element of a collection, or of an array, into a single transaction.
 *
 * Thread Safety: This class is thread-safe. Multiple transactions can be executed concurrently.
 */
class TransactionManager {
    private val transactionProgressiveId = AtomicInteger(0)
    // Thread-local flag to identify if the current thread is a manager thread
    private val isManagerThread = ThreadLocal.withInitial { false }
    // Thread executor with customized virtual thread factory to set the thread-local flag
    private val executor = Executors.newThreadPerTaskExecutor { task: Runnable ->
        Thread.ofVirtual().name("transaction-", 0).unstarted {
            isManagerThread.set(true)
            task.run()
        }
    }

    // Transactions still running, kept as futures that never fail so that awaiting them cannot throw.
    // The executor alone is not enough to tell whether the manager is idle: an operation handed over as an
    // already running future is never submitted here, so nothing would keep the executor alive while it runs.
    private val inFlight: MutableSet<CompletableFuture<Unit>> = ConcurrentHashMap.newKeySet()
    private val inFlightLock = ReentrantLock()

    val isShutdown: Boolean
        get() = executor.isShutdown
    val isTerminated: Boolean
        get() = executor.isTerminated

    /**
     * Begins a new transaction.
     *
     * @param description a description of the transaction for logging purposes
     * @return a new Transaction instance
     */
    private fun beginTransaction(description: String?): Transaction =
        Transaction(transactionProgressiveId.incrementAndGet(), description)

    /**
     * Starts an operation on this manager's executor, so that it can be registered into a transaction.
     *
     * A [Transaction] runs nothing on its own: it awaits futures that are already in flight. This is the
     * counterpart that puts them in flight, and the only place where the executor is exposed.
     *
     * @param T the return type of the operation
     * @param operation the operation to start
     * @return a CompletableFuture that completes with the result of the operation
     * @throws java.util.concurrent.RejectedExecutionException if this manager has been shut down
     */
    private fun <T> submit(operation: Supplier<T?>): CompletableFuture<T?> =
        CompletableFuture.supplyAsync(operation, executor)

    /**
     * Runs a function against a fresh transaction, and drives that transaction to its outcome.
     *
     * The transaction is automatically committed if the function completes successfully,
     * or rolled back if an exception occurs.
     *
     * This is the general form every public one is built on, and the only place where a [Transaction] is
     * handed out. It stays private so that a transaction cannot outlive the call that created it, and it
     * is deliberately named apart from them: it takes the transaction itself, not the work to run in one.
     *
     * A manager that is shutting down starts nothing: the function is not even applied, and the returned
     * future fails with an IllegalStateException.
     *
     * @param T the return type of the function
     * @param transactionFunction the function to execute within the transaction context
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the function, with null if the
     *     transaction was rolled back, or exceptionally if the function or one of its operations failed
     */
    private fun <T> withTransaction(
        transactionFunction: Function<Transaction, CompletableFuture<T?>>,
        description: String?
    ): CompletableFuture<T?> {
        // Reserved before anything is started, so that a transaction is either refused outright or waited
        // for: reserving after the work had begun would leave a window in which a shutdown misses it
        val settled = reserveInFlight()
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("Transaction manager is shut down and accepts no new transaction")
            )

        val transaction = beginTransaction(description)
        val transactionFuture: CompletableFuture<T?>

        try {
            transactionFuture = transactionFunction.apply(transaction)
        } catch (ex: Throwable) {
            transaction.rollback()
            settled.complete(Unit)
            return CompletableFuture.failedFuture(ex)
        }

        return transactionFuture
            .thenCompose { result: T? ->
                transaction.commit().thenApply { success: Boolean -> if (success) result else null }
            }
            .handle { result: T?, ex: Throwable? ->
                ex ?: return@handle result

                transaction.rollback()
                // Unwrap and rethrow the operation's own failure
                throw (ex as? CompletionException)?.cause ?: ex
            }
            .whenComplete { _, _ -> settled.complete(Unit) }
    }

    /**
     * Reserves a slot in the in-flight registry for a transaction that is about to start.
     *
     * The check and the reservation happen under the same lock [shutdown] snapshots the registry with,
     * which is what makes them mutually exclusive: a transaction that gets a slot is necessarily in the
     * snapshot, and one that comes too late gets no slot, and does not start at all.
     *
     * @return the future to complete once the transaction has settled, or null if this manager is no
     *     longer accepting transactions
     */
    private fun reserveInFlight(): CompletableFuture<Unit>? = inFlightLock.withLock {
        if (isShutdown) return@withLock null

        CompletableFuture<Unit>().also { slot ->
            inFlight.add(slot)
            slot.thenRun { inFlightLock.withLock { inFlight.remove(slot) } }
        }
    }

    /**
     * Awaits an operation that runs outside this manager, within a transaction.
     *
     * The operation is started by [operation] itself, which is invoked only once the transaction exists,
     * so nothing runs for a transaction that could not be opened and a failure to even start the operation
     * is reported as a failed transaction rather than thrown at the caller.
     *
     * The future it returns is registered as it is, without being wrapped in another task: the pipeline
     * stays asynchronous end to end, and the failure the transaction reports is the one the operation
     * produced. The transaction is committed if the operation succeeds, and rolled back if it fails or
     * completes with null.
     *
     * @param T the return type of the operation
     * @param operation starts the operation and returns it
     * @param rollback the rollback action to perform if the transaction fails (can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the operation, with null if the
     *     transaction was rolled back because the operation reported failure by returning null, or
     *     exceptionally with the operation's own exception if it failed by throwing
     */
    fun <T> completeInTransaction(
        operation: Supplier<CompletableFuture<T?>>,
        rollback: Runnable?,
        description: String?
    ): CompletableFuture<T?> =
        withTransaction(
            { t: Transaction -> t.addOperation(operation.get(), rollback) },
            description
        )

    /**
     * Awaits one already running operation per element, all within a single transaction.
     *
     * The transaction succeeds only if every operation does; otherwise the undo actions are executed in
     * reverse order of registration.
     *
     * Both arguments are factories, applied to one element at a time: the rollback one runs first, so that
     * it can capture the state it will have to restore before the operation is started.
     *
     * @param T the type of the elements to operate on
     * @param R the return type of each operation
     * @param elements the elements to operate on, one operation each
     * @param operation starts the operation for an element and returns it
     * @param rollback builds the rollback action for an element (either it or its result can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the results, in the order of [elements], with null
     *     if the transaction was rolled back because an operation reported failure by returning null, or
     *     exceptionally with the failing operation's own exception if one failed by throwing
     */
    fun <T, R> completeBatchInTransaction(
        elements: Collection<T>,
        operation: Function<T, CompletableFuture<R?>>,
        rollback: Function<T, Runnable?>?,
        description: String?
    ): CompletableFuture<List<R?>?> =
        withTransaction(
            { t: Transaction ->
                val futures = elements.map { element: T ->
                    // The undo action is built first, so that it can capture the state it will have to restore:
                    // as an argument it would be evaluated after the operation had already started
                    val undo = rollback?.apply(element)
                    t.addOperation(operation.apply(element), undo)
                }

                CompletableFuture.allOf(*futures.toTypedArray<CompletableFuture<*>>())
                    .thenApply<List<R?>?> { futures.map { future -> future.join() } }
            },
            description
        )

    /**
     * Awaits one already running operation per element, all within a single transaction.
     * See [completeBatchInTransaction] for details.
     *
     * @param T the type of the elements to operate on
     * @param R the return type of each operation
     * @param elements the elements to operate on, one operation each
     * @param operation starts the operation for an element and returns it
     * @param rollback builds the rollback action for an element (either it or its result can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the results, in the order of [elements], with null
     *     if the transaction was rolled back because an operation reported failure by returning null, or
     *     exceptionally with the failing operation's own exception if one failed by throwing
     */
    fun <T, R> completeBatchInTransaction(
        elements: Array<out T>,
        operation: Function<T, CompletableFuture<R?>>,
        rollback: Function<T, Runnable?>?,
        description: String?
    ): CompletableFuture<List<R?>?> =
        // asList is a view, so the array is not copied
        completeBatchInTransaction(elements.asList(), operation, rollback, description)

    /**
     * Executes an operation within a transaction.
     *
     * The operation is started on this manager's executor, and the transaction is committed if it succeeds,
     * or rolled back if it fails or returns null. Use [completeInTransaction] for an operation that has to
     * run somewhere else.
     *
     * @param T the return type of the operation
     * @param operation the operation to execute within the transaction
     * @param rollback the rollback action to perform if the transaction fails (can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the operation, with null if the
     *     transaction was rolled back because the operation reported failure by returning null, or
     *     exceptionally with the operation's own exception if it failed by throwing
     */
    fun <T> executeInTransaction(
        operation: Supplier<T?>,
        rollback: Runnable?,
        description: String?
    ): CompletableFuture<T?> =
        completeInTransaction(
            { submit(operation) },
            rollback,
            description
        )

    /**
     * Executes one operation per element, all within a single transaction.
     *
     * Every operation is started on this manager's executor, and the transaction succeeds only if all of
     * them do; otherwise the undo actions are executed in reverse order of registration. Use
     * [completeBatchInTransaction] for operations that are already running.
     *
     * The rollback argument is a factory rather than an action: it is applied to each element before that
     * element's operation starts, so it can capture the state it will have to restore.
     *
     * @param T the type of the elements to operate on
     * @param R the return type of each operation
     * @param elements the elements to operate on, one operation each
     * @param operation the operation to execute on each element
     * @param rollback builds the rollback action for an element (either it or its result can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the results, in the order of [elements], with null
     *     if the transaction was rolled back because an operation reported failure by returning null, or
     *     exceptionally with the failing operation's own exception if one failed by throwing
     */
    fun <T, R> executeBatchInTransaction(
        elements: Collection<T>,
        operation: Function<T, R?>,
        rollback: Function<T, Runnable?>?,
        description: String?
    ): CompletableFuture<List<R?>?> =
        completeBatchInTransaction(
            elements,
            { element: T -> submit { operation.apply(element) } },
            rollback,
            description
        )

    /**
     * Executes one operation per element, all within a single transaction.
     * See [executeBatchInTransaction] for details.
     *
     * @param T the type of the elements to operate on
     * @param R the return type of each operation
     * @param elements the elements to operate on, one operation each
     * @param operation the operation to execute on each element
     * @param rollback builds the rollback action for an element (either it or its result can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the results, in the order of [elements], with null
     *     if the transaction was rolled back because an operation reported failure by returning null, or
     *     exceptionally with the failing operation's own exception if one failed by throwing
     */
    fun <T, R> executeBatchInTransaction(
        elements: Array<out T>,
        operation: Function<T, R?>,
        rollback: Function<T, Runnable?>?,
        description: String?
    ): CompletableFuture<List<R?>?> =
        // asList is a view, so the array is not copied
        executeBatchInTransaction(elements.asList(), operation, rollback, description)

    /**
     * Initiates an orderly shutdown of the executor service.
     *
     * From the moment this method is entered no new transaction is accepted: every entry point fails fast
     * instead of starting work nobody would wait for. It then blocks until every transaction that did
     * start has settled and all tasks have completed execution, or the thread is interrupted.
     *
     * A shutdown from one of the threads this manager runs its operations on would wait for its own
     * caller, and is refused. The same wait remains possible from outside, on an operation handed over
     * already running, where detecting it would cost bookkeeping on every transaction: a price that
     * outweighs the benefits. Strong caution is advised when checking for deadlocks caused by this
     * circular wait.
     *
     * @throws IllegalStateException if called from a thread this manager runs its operations on
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    fun shutdown() {
        // The marker is set by the thread factory, so it only ever covers threads this executor created
        check(!isManagerThread.get()) {
            "shutdown() cannot be called from a manager thread, as it would block the executor and deadlock"
        }

        if (this.isShutdown) return

        // Ask for shutdown first, so that no new operation is accepted while we wait for the current ones
        executor.shutdown()
        try {
            // Taken under the lock, so that no transaction can be reserving a slot while it is read:
            // everything that started before this point is in here, and nothing new can start after it
            val pending = inFlightLock.withLock { inFlight.toTypedArray() }

            // Transactions whose operations run outside this executor are only visible here
            CompletableFuture.allOf(*pending).join()

            // Wait without a timer (maximum time possible)
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            Logger.getInstance().addError(e)
            executor.shutdownNow()
            throw e
        }
    }
}
