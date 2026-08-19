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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Represents a transaction that encapsulates a series of operations that can be committed or rolled back.
 *
 * Operations are registered through [addOperation] as futures that are already running, each together with
 * the action that undoes it. This class executes nothing itself: who runs the operations, and on which
 * threads, is entirely the caller's concern, and a transaction is only the bookkeeping around them.
 * [commit] waits for all of them and succeeds only if every one of them did; otherwise the undo actions
 * are executed in reverse order of registration.
 *
 * Thread safety comes in two halves.
 *
 * The caller is responsible for registration: every [addOperation] call must come from the same thread, and
 * all of them must happen before [commit]. Registering an operation concurrently with a commit is not
 * supported, because the commit captures the set of operations at the moment it starts: an operation that
 * arrives later would still run, but no one would wait for it or notice its failure.
 *
 * This class is responsible for everything from the commit onwards, which is genuinely concurrent: [commit]
 * completes on whichever thread finishes the last operation, and [rollback] may be called from yet another
 * one. Every transition between lifecycle states is therefore a compare-and-set, which is what guarantees
 * that a transaction is committed or rolled back at most once, that the undo actions never run twice, and
 * that a committed transaction is never undone.
 */
class Transaction
/**
 * Constructs a new Transaction.
 *
 * @param transactionId the ID of the transaction (for logging purposes)
 * @param description a description of the transaction (for logging purposes)
 */
(private val transactionId: Int, private val description: String?) {
    private val operations: MutableList<Operation> = CopyOnWriteArrayList()
    private val state = AtomicReference(State.ACTIVE)
    // Used internally to determine if the transaction is in a terminal state (COMMITTED or ROLLED_BACK)
    // with a single read of the atomic reference, so that a positive check can be trusted without a CAS.
    private val isTerminalState: Boolean
        get() = state.get().let { it == State.COMMITTED || it == State.ROLLED_BACK }

    // Public API values
    val isCommitted: Boolean
        get() = (state.get() == State.COMMITTED)
    val isRolledBack: Boolean
        get() = (state.get() == State.ROLLED_BACK)

    /**
     * Adds an already running operation to this transaction with an optional rollback action.
     *
     * The future is registered as it is, without being wrapped in anything: the transaction waits for the
     * operation itself, and sees its failure or cancellation unaltered. As described in the class
     * documentation, this must be called from the single registering thread, and before [commit].
     *
     * Since the caller is the one who started the operation, a rejected registration leaves it running:
     * undoing it is then up to the caller, as is honouring the contract that makes this impossible.
     *
     * @param T the type of the operation result
     * @param operation the future to await as part of this transaction
     * @param rollback the rollback action to execute if the transaction fails (can be null)
     * @return the operation itself, for convenience
     * @throws IllegalStateException if the transaction is no longer accepting operations
     */
    fun <T> addOperation(operation: CompletableFuture<T?>, rollback: Runnable?): CompletableFuture<T?> {
        when (state.get()) {
            State.ACTIVE -> {}
            State.COMMITTING -> error("Transaction is already being committed")
            State.COMMITTED -> error("Transaction has already been committed")
            State.ROLLED_BACK -> error("Transaction has already been rolled back")
        }

        // A single append keeps an operation and its undo action inseparable: with two lists there was
        // a window in which a concurrent commit could see the operation but not yet its rollback
        return operation.also { future: CompletableFuture<T?> ->
            operations.add(Operation(future, rollback))
        }
    }

    /**
     * Commits the transaction, waiting for every operation registered so far to complete.
     *
     * If any of them fails, the transaction is automatically rolled back.
     *
     * @return a CompletableFuture that completes with true if all operations succeeded, and with false if any of
     *     them failed or if this transaction had already been committed or rolled back
     */
    fun commit(): CompletableFuture<Boolean> {
        // Fast path out of a terminal state, where a positive check needs no CAS to be trusted
        if (isTerminalState) return CompletableFuture.completedFuture(false)

        // Only the first caller gets to commit; any later one finds the transaction out of ACTIVE and reports failure
        val wasActive = state.compareAndSet(State.ACTIVE, State.COMMITTING)
        if (!wasActive) return CompletableFuture.completedFuture(false)

        Logger.getInstance().addDebug("Attempting to commit transaction %d (%s)", transactionId, description)
        return CompletableFuture.allOf(*operations.map(Operation::future).toTypedArray())
            .thenApply {
                // Succeeds only if every operation completed with a non-null result
                operations.none { (future, _) ->
                    future.isCompletedExceptionally || (future.isDone && future.join() == null)
                }
            }
            .handle { noneFailed: Boolean?, error: Throwable? ->
                // When no operation has failed it also means no exception has been thrown.
                // The CAS is what makes the outcome final: it loses only against a concurrent rollback
                val success = (noneFailed == true)
                        && state.compareAndSet(State.COMMITTING, State.COMMITTED)
                if (!success) rollback()

                val outcome = when {
                    success -> "committed successfully"
                    error != null -> "rolled back: an exception occurred during commit"
                    noneFailed == true -> "rolled back concurrently while committing"
                    else -> "rolled back: some operation failed during commit"
                }
                Logger.getInstance().addDebug("Transaction %d (%s) %s", transactionId, description, outcome)

                success
            }
    }

    /**
     * Rolls back the transaction by executing all registered rollback actions in reverse order.
     * This should not be called directly; it is automatically invoked if commit fails.
     *
     * Rolling back an in-flight commit is allowed and makes that commit report failure. A committed
     * or already rolled back transaction is left untouched, so the actions never run twice.
     */
    fun rollback() {
        // Fast path out of a terminal state, where a positive check needs no CAS to be trusted
        if (isTerminalState) return

        // A committed transaction is never undone, so COMMITTED is deliberately not a valid source state here.
        val wasActive = state.compareAndSet(State.ACTIVE, State.ROLLED_BACK)
        val wasCommitting = !wasActive && state.compareAndSet(State.COMMITTING, State.ROLLED_BACK)
        // If it wasn't either active or committing, just exit
        if ( !(wasActive || wasCommitting) ) return

        // Execute rollback actions in reverse order, skipping operations that registered none
        for (i in operations.indices.reversed()) {
            try {
                operations[i].rollback?.run()
            } catch (e: Exception) {
                // Log but continue with remaining rollbacks
                Logger.getInstance().addError(e)
            }
        }
    }

    /**
     * Lifecycle of a transaction. COMMITTING is the window in which the operations are being awaited:
     * it accepts no new operation and can still be turned into a rollback, but never into a second commit.
     *
     * COMMITTED and ROLLED_BACK are terminal states: the transaction is no longer usable and cannot be changed.
     * A positive check on these two can therefore be trusted without a CAS; a negative one cannot,
     * and must fall through to the compare-and-set that follows.
     */
    private enum class State { ACTIVE, COMMITTING, COMMITTED, ROLLED_BACK }

    /**
     * An operation and the action that undoes it. Holding them in one element is what makes their
     * registration atomic: a list of pairs can only ever be appended to as a whole.
     */
    private data class Operation(val future: CompletableFuture<*>, val rollback: Runnable?)
}
