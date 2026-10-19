package password.manager.app.base

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Owns a virtual-thread executor and tracks asynchronous operations that must settle before shutdown completes.
 *
 * Tracked operations may include work performed by other executors, so waiting for the underlying [ExecutorService] to
 * terminate alone is not enough. It implements [Executor] so it can be passed directly to asynchronous APIs, but not
 * [ExecutorService]: [shutdown] is a blocking close that also waits for external operations, while the standard
 * executor contract makes [ExecutorService.shutdown] initiate shutdown and return.
 */
class TrackedExecutor(private val threadNamePrefix: String) : Executor {
    private val isWorkerThread = ThreadLocal.withInitial { false }
    private val lifecycleLock = ReentrantLock()
    private val inFlight = HashSet<CompletableFuture<Unit>>()
    private var acceptingOperations = true

    private val executor: ExecutorService = Executors.newThreadPerTaskExecutor { task: Runnable ->
        Thread.ofVirtual().name(threadNamePrefix, 0).unstarted {
            isWorkerThread.set(true)
            try {
                task.run()
            } finally {
                isWorkerThread.remove()
            }
        }
    }

    /**
     * Whether shutdown has begun and this wrapper no longer accepts operations through [execute] or [track].
     */
    val isShutdown: Boolean
        get() = lifecycleLock.withLock { !acceptingOperations }

    /**
     * Whether the underlying executor has terminated, and all tracked operations have settled,
     * including operations that delegate work to another executor.
     */
    val isTerminated: Boolean
        get() = lifecycleLock.withLock { executor.isTerminated && inFlight.isEmpty() }

    /**
     * Starts and tracks an operation, rejecting it if shutdown has begun.
     *
     * The operation factory is invoked while holding the lifecycle lock. This makes its initial work submission atomic
     * with respect to shut down: accepted operations can finish scheduling before the underlying executor is shut down.
     *
     * @return the operation's future, or null if this executor no longer accepts operations
     */
    fun <T> track(operation: () -> CompletableFuture<T>): CompletableFuture<T>? = lifecycleLock.withLock {
        if (!acceptingOperations) return@withLock null

        val settled = CompletableFuture<Unit>()
        inFlight.add(settled)

        try {
            operation().whenComplete { _, _ ->
                lifecycleLock.withLock {
                    inFlight.remove(settled)
                    settled.complete(Unit)
                }
            }
        } catch (ex: Throwable) {
            inFlight.remove(settled)
            settled.complete(Unit)
            throw ex
        }
    }

    /**
     * Executes the given command at some time in the future or throws an exception if this executor is shut down.
     * The command executes on the owned virtual-thread executor.
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this executor is shut down
     */
    override fun execute(command: Runnable) = lifecycleLock.withLock {
        if (!acceptingOperations) throw RejectedExecutionException("This executor is shut down")
        executor.execute(command)
    }

    /**
     * Stops accepting operations and waits for tracked operations and executor tasks to finish.
     *
     * Calling this from one of this executor's worker threads would wait for the calling operation itself, so that call
     * is rejected.
     *
     * @throws IllegalStateException if called from one of this executor's worker threads
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    fun shutdown() {
        check(!isWorkerThread.get()) {
            "shutdown() cannot be called from a worker thread, as it would wait for the calling operation"
        }

        val pending = lifecycleLock.withLock {
            if (acceptingOperations) {
                acceptingOperations = false
                executor.shutdown()
            }
            inFlight.toTypedArray()
        }

        try {
            CompletableFuture.allOf(*pending).get()
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
        } catch (ex: InterruptedException) {
            executor.shutdownNow()
            throw ex
        } catch (ex: ExecutionException) {
            throw IllegalStateException("A tracked operation completed unexpectedly", ex.cause)
        }
    }
}
