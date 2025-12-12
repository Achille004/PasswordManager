/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

package testing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import password.manager.app.persistence.Transaction;
import password.manager.app.singletons.Logger;

public class TestTransaction {

    private ExecutorService executor;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        transaction = new Transaction(executor);
    }

    @AfterEach
    void tearDown() {
        if (!executor.isShutdown()) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Logger.destroyInstance();
    }

    @Test
    void testSuccessfulCommit() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger value = new AtomicInteger(0);

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return true;
        }, null);

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return true;
        }, null);

        CompletableFuture<Boolean> result = transaction.commit();
        assertTrue(result.get(5, TimeUnit.SECONDS), "Transaction should commit successfully");
        assertTrue(transaction.isCommitted(), "Transaction should be marked as committed");
        assertFalse(transaction.isRolledBack(), "Transaction should not be rolled back");
        assertEquals(2, value.get(), "Both operations should have executed");
    }

    @Test
    void testRollbackOnFailure() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger value = new AtomicInteger(0);
        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return true;
        }, () -> {
            value.decrementAndGet();
            rollbackExecuted.set(true);
        });

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return null; // Simulate failure by returning null
        }, value::decrementAndGet);

        CompletableFuture<Boolean> result = transaction.commit();
        assertFalse(result.get(5, TimeUnit.SECONDS), "Transaction should fail");
        assertTrue(transaction.isRolledBack(), "Transaction should be rolled back");
        assertFalse(transaction.isCommitted(), "Transaction should not be committed");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
        assertEquals(0, value.get(), "Value should be rolled back to 0");
    }

    @Test
    void testRollbackOnException() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        transaction.addOperation(() -> {
            throw new RuntimeException("Simulated failure");
        }, () -> rollbackExecuted.set(true));

        CompletableFuture<Boolean> result = transaction.commit();
        assertFalse(result.get(5, TimeUnit.SECONDS), "Transaction should fail on exception");
        assertTrue(transaction.isRolledBack(), "Transaction should be rolled back");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
    }

    @Test
    void testRollbackInReverseOrder() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger executionOrder = new AtomicInteger(0);
        int[] rollbackOrder = new int[3];

        transaction.addOperation(() -> true, () -> rollbackOrder[0] = executionOrder.incrementAndGet());
        transaction.addOperation(() -> true, () -> rollbackOrder[1] = executionOrder.incrementAndGet());
        transaction.addOperation(() -> null, () -> rollbackOrder[2] = executionOrder.incrementAndGet()); // Force rollback

        transaction.commit().get(5, TimeUnit.SECONDS);

        // Rollback should execute in reverse order: last added operation (index 2) rolls back first
        assertEquals(1, rollbackOrder[2], "Last operation should roll back first");
        assertEquals(2, rollbackOrder[1], "Second operation should roll back second");
        assertEquals(3, rollbackOrder[0], "First operation should roll back last");
    }

    @Test
    void testManualRollback() {
        TestingUtils.initLogger();

        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        transaction.addOperation(() -> true, () -> rollbackExecuted.set(true));

        transaction.rollback();

        assertTrue(transaction.isRolledBack(), "Transaction should be rolled back");
        assertTrue(rollbackExecuted.get(), "Rollback action should have been executed");
    }

    @Test
    void testRollbackIdempotent() {
        TestingUtils.initLogger();

        AtomicInteger rollbackCount = new AtomicInteger(0);

        transaction.addOperation(() -> true, rollbackCount::incrementAndGet);

        transaction.rollback();
        transaction.rollback();
        transaction.rollback();

        assertEquals(1, rollbackCount.get(), "Rollback should only execute once");
    }

    @Test
    void testAddOperationAfterCommit() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        transaction.addOperation(() -> true, null);
        transaction.commit().get(5, TimeUnit.SECONDS);

        assertThrows(IllegalStateException.class, () -> transaction.addOperation(() -> true, null),
            "Should not be able to add operation after commit");
    }

    @Test
    void testAddOperationAfterRollback() {
        TestingUtils.initLogger();

        transaction.addOperation(() -> true, null);
        transaction.rollback();

        assertThrows(IllegalStateException.class, () -> transaction.addOperation(() -> true, null),
            "Should not be able to add operation after rollback");
    }

    @Test
    void testCommitWithoutOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        CompletableFuture<Boolean> result = transaction.commit();
        assertTrue(result.get(5, TimeUnit.SECONDS), "Empty transaction should commit successfully");
        assertTrue(transaction.isCommitted(), "Transaction should be marked as committed");
    }

    @Test
    void testRollbackWithoutOperations() {
        TestingUtils.initLogger();

        transaction.rollback();
        assertTrue(transaction.isRolledBack(), "Empty transaction should roll back successfully");
    }

    @Test
    void testOperationWithoutRollback() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicBoolean executed = new AtomicBoolean(false);

        transaction.addOperation(() -> {
            executed.set(true);
            return true;
        }, null); // No rollback action

        CompletableFuture<Boolean> result = transaction.commit();
        assertTrue(result.get(5, TimeUnit.SECONDS), "Transaction should commit successfully");
        assertTrue(executed.get(), "Operation should have executed");
    }

    @Test
    void testMultipleOperationsWithMixedRollbacks() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger value = new AtomicInteger(0);
        AtomicBoolean rollback1 = new AtomicBoolean(false);
        AtomicBoolean rollback3 = new AtomicBoolean(false);

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return true;
        }, () -> rollback1.set(true));

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return true;
        }, null); // No rollback for this one

        transaction.addOperation(() -> {
            value.incrementAndGet();
            return null; // Force failure
        }, () -> rollback3.set(true));

        CompletableFuture<Boolean> result = transaction.commit();
        assertFalse(result.get(5, TimeUnit.SECONDS), "Transaction should fail");

        assertTrue(rollback1.get(), "First operation should have rolled back");
        assertTrue(rollback3.get(), "Third operation should have rolled back");
    }

    @Test
    void testRollbackContinuesOnException() {
        TestingUtils.initLogger();

        AtomicBoolean rollback1 = new AtomicBoolean(false);
        AtomicBoolean rollback3 = new AtomicBoolean(false);

        transaction.addOperation(() -> true, () -> rollback1.set(true));

        transaction.addOperation(() -> true, () -> {
            throw new RuntimeException("Rollback error");
        });

        transaction.addOperation(() -> true, () -> rollback3.set(true));

        transaction.rollback();

        // All rollbacks should execute despite the exception in the middle
        assertTrue(rollback1.get(), "First rollback should execute");
        assertTrue(rollback3.get(), "Third rollback should execute even after exception in second");
    }

    @Test
    void testConcurrentOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger counter = new AtomicInteger(0);
        int operationCount = 10;

        for (int i = 0; i < operationCount; i++) {
            transaction.addOperation(() -> {
                counter.incrementAndGet();
                return true;
            }, null);
        }

        CompletableFuture<Boolean> result = transaction.commit();
        assertTrue(result.get(5, TimeUnit.SECONDS), "Transaction should commit successfully");
        assertEquals(operationCount, counter.get(), "All operations should have executed");
    }

    @Test
    void testCommitAfterAlreadyCommitted() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        transaction.addOperation(() -> true, null);
        transaction.commit().get(5, TimeUnit.SECONDS);

        CompletableFuture<Boolean> result = transaction.commit();
        assertFalse(result.get(5, TimeUnit.SECONDS), "Should not be able to commit twice");
    }

    @Test
    void testCommitAfterRollback() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        transaction.addOperation(() -> true, null);
        transaction.rollback();

        CompletableFuture<Boolean> result = transaction.commit();
        assertFalse(result.get(5, TimeUnit.SECONDS), "Should not be able to commit after rollback");
    }

    @Test
    void testOperationReturningValue() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        String expectedValue = "test_value";

        CompletableFuture<String> operationFuture = transaction.addOperation(() -> expectedValue, null);

        transaction.commit().get(5, TimeUnit.SECONDS);

        String actualValue = operationFuture.get(5, TimeUnit.SECONDS);
        assertEquals(expectedValue, actualValue, "Operation should return correct value");
    }

    @Test
    void testOperationWithDelay() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicBoolean executed = new AtomicBoolean(false);

        transaction.addOperation(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executed.set(true);
            return true;
        }, null);

        CompletableFuture<Boolean> result = transaction.commit();
        assertTrue(result.get(5, TimeUnit.SECONDS), "Transaction should wait for operation to complete");
        assertTrue(executed.get(), "Delayed operation should have executed");
    }
}
