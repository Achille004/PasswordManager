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

package testing.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import password.manager.app.persistence.TransactionManager;
import password.manager.app.singletons.Singletons;
import testing.TestingUtils;

public class TestTransactionManager {

    private TransactionManager manager;

    @BeforeEach
    void setUp() {
        manager = new TransactionManager();
    }

    @AfterEach
    void tearDown() {
        if (!manager.isShutdown()) {
            manager.shutdown();
        }
        Singletons.shutdownAll();
    }

    @Test
    void testExecuteInTransactionSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<List<Integer>> result = manager.executeBatchInTransaction(
            List.of(42),
            element -> {
                value.incrementAndGet();
                return element;
            },
            null,
            "Test Transaction"
        );

        List<Integer> resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(List.of(42), resultValue, "Transaction should return the operation results");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testExecuteInTransactionFailure() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        CompletableFuture<List<String>> result = manager.executeBatchInTransaction(
            List.of("only"),
            element -> null, // Force failure
            element -> () -> rollbackExecuted.set(true),
            "Test Transaction"
        );

        List<String> resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Failed transaction should return null");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
    }

    @Test
    void testExecuteInTransactionWithException() {
        TestingUtils.injectBasePath();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        CompletableFuture<List<String>> result = manager.executeBatchInTransaction(
            List.of("only"),
            element -> {
                throw new RuntimeException("Test exception");
            },
            element -> () -> rollbackExecuted.set(true),
            "Test Transaction"
        );

        // The results are the operations' own, so an operation that throws makes the transaction fail
        assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS),
            "A throwing operation should fail the transaction");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
    }

    @Test
    void testExecuteSingleOperation() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Boolean> result = manager.executeInTransaction(() -> {
            value.incrementAndGet();
            return true;
        }, value::decrementAndGet, "Test Transaction");

        Boolean resultValue = result.get(5, TimeUnit.SECONDS);
        assertTrue(resultValue, "Transaction should succeed");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testExecuteSingleOperationWithFailure() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Boolean> result = manager.executeInTransaction(() -> {
            value.incrementAndGet();
            return null; // Force failure
        }, value::decrementAndGet, "Test Transaction");

        Boolean resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Failed transaction should return null");
        assertEquals(0, value.get(), "Rollback should have been executed");
    }

    @Test
    void testMultipleTransactionsConcurrently() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        int transactionCount = 10;
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < transactionCount; i++) {
            final int value = i;
            CompletableFuture<Integer> future = manager.executeInTransaction(
                () -> {
                    counter.incrementAndGet();
                    return value;
                },
                null,
                "Test Transaction " + i
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);

        assertEquals(transactionCount, counter.get(), "All transactions should have executed");

        for (int i = 0; i < transactionCount; i++) {
            assertEquals(i, futures.get(i).get(), "Each transaction should return its value");
        }
    }

    @Test
    void testShutdown() {
        TestingUtils.injectBasePath();

        assertFalse(manager.isShutdown(), "Manager should not be shut down initially");

        manager.shutdown();

        assertTrue(manager.isShutdown(), "Manager should be shut down after shutdown()");
    }

    @Test
    void testShutdownIdempotent() {
        TestingUtils.injectBasePath();

        manager.shutdown();
        manager.shutdown();

        assertTrue(manager.isShutdown(), "Manager should remain shut down");
    }

    @Test
    void testExternalFutureCommits() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicBoolean executed = new AtomicBoolean(false);

        // Completed by a scheduler thread, so the manager's executor never sees this operation
        CompletableFuture<Boolean> external = new CompletableFuture<>();
        external.completeAsync(
            () -> {
                executed.set(true);
                return true;
            },
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
        );

        CompletableFuture<Boolean> result = manager.completeInTransaction(() -> external, null, "External operation");

        assertTrue(result.get(5, TimeUnit.SECONDS), "Transaction should commit with the future's result");
        assertTrue(executed.get(), "The external operation should have run");
    }

    @Test
    void testExternalFutureFailureRollsBack() {
        TestingUtils.injectBasePath();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);
        CompletableFuture<Boolean> external =
            CompletableFuture.failedFuture(new IllegalArgumentException("Simulated failure"));

        CompletableFuture<Boolean> result =
            manager.completeInTransaction(() -> external, () -> rollbackExecuted.set(true), "Failing external operation");

        ExecutionException thrown =
            assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");

        // The transaction reports the operation's own exception, with nothing of its own wrapped around it
        Throwable cause = thrown.getCause();
        assertTrue(cause instanceof IllegalArgumentException, "The original failure should be reported as it is");
        assertEquals("Simulated failure", cause.getMessage(), "The original failure should be unaltered");
    }

    @Test
    void testFailureToStartFailsTheTransaction() {
        TestingUtils.injectBasePath();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        // Nothing reaches the caller: the operation is started from inside the transaction
        CompletableFuture<Boolean> result = manager.completeInTransaction(
            () -> {
                throw new IllegalStateException("Cannot start");
            },
            () -> rollbackExecuted.set(true),
            "Unstartable operation"
        );

        ExecutionException thrown =
            assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertTrue(thrown.getCause() instanceof IllegalStateException,
            "The transaction should report the failure that prevented the operation from starting");
        assertFalse(rollbackExecuted.get(), "Nothing ran, so there is nothing to roll back");
    }

    @Test
    void testTransactionRefusedAfterShutdown() {
        TestingUtils.injectBasePath();

        manager.shutdown();

        AtomicBoolean executed = new AtomicBoolean(false);
        CompletableFuture<Boolean> result = manager.executeInTransaction(
            () -> {
                executed.set(true);
                return true;
            },
            null,
            "Transaction after shutdown"
        );

        ExecutionException thrown =
            assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertTrue(thrown.getCause() instanceof IllegalStateException,
            "A manager that has been shut down should refuse the transaction");
        assertFalse(executed.get(), "The operation should not have been started");
    }

    @Test
    void testShutdownWaitsForOperationsOutsideExecutor()
            throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicBoolean executed = new AtomicBoolean(false);

        // Nothing is ever submitted to the executor here, so only the in-flight registry keeps shutdown waiting
        CompletableFuture<Boolean> external = new CompletableFuture<>();
        external.completeAsync(
            () -> {
                executed.set(true);
                return true;
            },
            CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS)
        );

        CompletableFuture<Boolean> result = manager.completeInTransaction(() -> external, null, "External operation");

        manager.shutdown();

        assertTrue(executed.get(), "shutdown() should not return while an operation is still running");
        assertTrue(result.isDone(), "The transaction should have settled before shutdown() returned");
        assertTrue(result.get(5, TimeUnit.SECONDS), "The transaction should have committed");
    }

    @Test
    void testComplexTransactionWithMultipleOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> executionOrder = new ArrayList<>();

        CompletableFuture<List<Integer>> result = manager.executeBatchInTransaction(
            List.of(1, 2, 3),
            element -> {
                synchronized (executionOrder) {
                    executionOrder.add(element);
                }
                counter.incrementAndGet();
                return element;
            },
            element -> counter::decrementAndGet,
            "Test Transaction"
        );

        List<Integer> resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(List.of(1, 2, 3), resultValue, "Results should come back in the order of the elements");
        assertEquals(3, counter.get(), "All three operations should have executed");
        assertEquals(3, executionOrder.size(), "All three operations should have been recorded");
    }

    @Test
    void testTransactionRollbackInComplexScenario() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> rollbackOrder = new ArrayList<>();

        CompletableFuture<List<Integer>> result = manager.executeBatchInTransaction(
            List.of(1, 2, 3),
            element -> {
                counter.incrementAndGet();
                return element == 3 ? null : element; // The last one forces a failure
            },
            element -> () -> {
                synchronized (rollbackOrder) {
                    rollbackOrder.add(element);
                }
                counter.decrementAndGet();
            },
            "Test Transaction"
        );

        List<Integer> resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Transaction should fail");
        assertEquals(0, counter.get(), "Counter should be rolled back to 0");
        assertEquals(3, rollbackOrder.size(), "All rollbacks should have executed");

        // Verify rollback order (should be 3, 2, 1)
        assertEquals(3, rollbackOrder.get(0), "Last operation should roll back first");
        assertEquals(2, rollbackOrder.get(1), "Second operation should roll back second");
        assertEquals(1, rollbackOrder.get(2), "First operation should roll back last");
    }

    @Test
    void testTransactionWithDelayedOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicBoolean operation1Complete = new AtomicBoolean(false);
        AtomicBoolean operation2Complete = new AtomicBoolean(false);

        CompletableFuture<List<Boolean>> result = manager.executeBatchInTransaction(
            List.of(50, 100),
            delayMillis -> {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                (delayMillis == 50 ? operation1Complete : operation2Complete).set(true);
                return true;
            },
            null,
            "Test Transaction"
        );

        List<Boolean> resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(List.of(true, true), resultValue, "Transaction should wait for every operation");
        assertTrue(operation1Complete.get(), "First operation should complete");
        assertTrue(operation2Complete.get(), "Second operation should complete");
    }

    @Test
    void testNestedTransactionExecution() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger outerCounter = new AtomicInteger(0);
        AtomicInteger innerCounter = new AtomicInteger(0);

        CompletableFuture<List<Boolean>> result = manager.executeBatchInTransaction(
            List.of("outer"),
            element -> {
                outerCounter.incrementAndGet();

                // Execute an inner transaction from within an operation of the outer one
                CompletableFuture<List<Integer>> innerResult = manager.executeBatchInTransaction(
                    List.of("inner"),
                    innerElement -> innerCounter.incrementAndGet(),
                    null,
                    "Inner Transaction"
                );

                try {
                    innerResult.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return true;
            },
            null,
            "Outer Transaction"
        );

        List<Boolean> resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(List.of(true), resultValue, "Outer transaction should succeed");
        assertEquals(1, outerCounter.get(), "Outer operation should execute");
        assertEquals(1, innerCounter.get(), "Inner operation should execute");
    }

    @Test
    void testTransactionWithNullRollback() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Integer> result = manager.executeInTransaction(
            () -> {
                value.incrementAndGet();
                return value.get();
            },
            null, // No rollback action
            "Test Transaction"
        );

        Integer resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(1, resultValue, "Transaction should succeed");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testMultipleSequentialTransactions() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            CompletableFuture<Boolean> result = manager.executeInTransaction(
                () -> {
                    counter.incrementAndGet();
                    return true;
                },
                null,
                "Test Transaction " + i
            );
            assertTrue(result.get(5, TimeUnit.SECONDS), "Each transaction should succeed");
        }

        assertEquals(5, counter.get(), "All transactions should have executed");
    }
}
