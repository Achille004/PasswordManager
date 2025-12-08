package testing;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
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

import password.manager.app.persistence.Transaction;
import password.manager.app.persistence.TransactionManager;
import password.manager.app.singletons.Logger;

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
        Logger.destroyInstance();
    }

    @Test
    void testBeginTransaction() {
        TestingUtils.initLogger();
        
        Transaction transaction = manager.beginTransaction();
        assertNotNull(transaction, "Transaction should not be null");
        assertFalse(transaction.isCommitted(), "New transaction should not be committed");
        assertFalse(transaction.isRolledBack(), "New transaction should not be rolled back");
    }

    @Test
    void testExecuteInTransactionSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Integer> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> {
                value.incrementAndGet();
                return true;
            }, null);

            return CompletableFuture.completedFuture(42);
        });

        Integer resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(42, resultValue, "Transaction should return correct value");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testExecuteInTransactionFailure() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        CompletableFuture<String> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> null, () -> rollbackExecuted.set(true)); // Force failure
            return CompletableFuture.completedFuture("test");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Failed transaction should return null");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
    }

    @Test
    void testExecuteInTransactionWithException() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicBoolean rollbackExecuted = new AtomicBoolean(false);

        CompletableFuture<String> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> {
                throw new RuntimeException("Test exception");
            }, () -> rollbackExecuted.set(true));

            return CompletableFuture.completedFuture("test");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Failed transaction should return null");
        assertTrue(rollbackExecuted.get(), "Rollback should have been executed");
    }

    @Test
    void testExecuteInTransactionShorthand() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Boolean> result = manager.executeInTransaction(
            () -> {
                value.incrementAndGet();
                return true;
            },
            () -> value.decrementAndGet()
        );

        Boolean resultValue = result.get(5, TimeUnit.SECONDS);
        assertTrue(resultValue, "Transaction should succeed");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testExecuteInTransactionShorthandWithFailure() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Boolean> result = manager.executeInTransaction(
            () -> {
                value.incrementAndGet();
                return null; // Force failure
            },
            () -> value.decrementAndGet()
        );

        Boolean resultValue = result.get(5, TimeUnit.SECONDS);
        assertNull(resultValue, "Failed transaction should return null");
        assertEquals(0, value.get(), "Rollback should have been executed");
    }

    @Test
    void testMultipleTransactionsConcurrently() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
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
                null
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        assertEquals(transactionCount, counter.get(), "All transactions should have executed");

        for (int i = 0; i < transactionCount; i++) {
            assertEquals(i, futures.get(i).get(), "Each transaction should return its value");
        }
    }

    @Test
    void testShutdown() {
        TestingUtils.initLogger();
        
        assertFalse(manager.isShutdown(), "Manager should not be shut down initially");

        manager.shutdown();

        assertTrue(manager.isShutdown(), "Manager should be shut down after shutdown()");
    }

    @Test
    void testShutdownIdempotent() {
        TestingUtils.initLogger();
        
        manager.shutdown();
        manager.shutdown();

        assertTrue(manager.isShutdown(), "Manager should remain shut down");
    }

    @Test
    void testTransactionExecutionAfterShutdown() {
        TestingUtils.initLogger();
        
        manager.shutdown();

        // This test verifies that transactions can still be created after shutdown,
        // but they may not execute properly due to the executor being shut down
        assertDoesNotThrow(() -> manager.beginTransaction(),
            "Should be able to create transaction even after shutdown");
    }

    @Test
    void testComplexTransactionWithMultipleOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> executionOrder = new ArrayList<>();

        CompletableFuture<String> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> {
                synchronized (executionOrder) {
                    executionOrder.add(1);
                }
                counter.incrementAndGet();
                return true;
            }, () -> counter.decrementAndGet());

            transaction.addOperation(() -> {
                synchronized (executionOrder) {
                    executionOrder.add(2);
                }
                counter.incrementAndGet();
                return true;
            }, () -> counter.decrementAndGet());

            transaction.addOperation(() -> {
                synchronized (executionOrder) {
                    executionOrder.add(3);
                }
                counter.incrementAndGet();
                return true;
            }, () -> counter.decrementAndGet());

            return CompletableFuture.completedFuture("success");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals("success", resultValue, "Transaction should succeed");
        assertEquals(3, counter.get(), "All three operations should have executed");
        assertEquals(3, executionOrder.size(), "All three operations should have been recorded");
    }

    @Test
    void testTransactionRollbackInComplexScenario() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> rollbackOrder = new ArrayList<>();

        CompletableFuture<String> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> {
                counter.incrementAndGet();
                return true;
            }, () -> {
                synchronized (rollbackOrder) {
                    rollbackOrder.add(1);
                }
                counter.decrementAndGet();
            });

            transaction.addOperation(() -> {
                counter.incrementAndGet();
                return true;
            }, () -> {
                synchronized (rollbackOrder) {
                    rollbackOrder.add(2);
                }
                counter.decrementAndGet();
            });

            transaction.addOperation(() -> {
                counter.incrementAndGet();
                return null; // Force failure
            }, () -> {
                synchronized (rollbackOrder) {
                    rollbackOrder.add(3);
                }
                counter.decrementAndGet();
            });

            return CompletableFuture.completedFuture("success");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
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
        TestingUtils.initLogger();
        
        AtomicBoolean operation1Complete = new AtomicBoolean(false);
        AtomicBoolean operation2Complete = new AtomicBoolean(false);

        CompletableFuture<String> result = manager.executeInTransaction(transaction -> {
            transaction.addOperation(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                operation1Complete.set(true);
                return true;
            }, null);

            transaction.addOperation(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                operation2Complete.set(true);
                return true;
            }, null);

            return CompletableFuture.completedFuture("delayed");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals("delayed", resultValue, "Transaction should complete");
        assertTrue(operation1Complete.get(), "First operation should complete");
        assertTrue(operation2Complete.get(), "Second operation should complete");
    }

    @Test
    void testNestedTransactionExecution() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger outerCounter = new AtomicInteger(0);
        AtomicInteger innerCounter = new AtomicInteger(0);

        CompletableFuture<String> result = manager.executeInTransaction(outerTransaction -> {
            outerTransaction.addOperation(() -> {
                outerCounter.incrementAndGet();

                // Execute an inner transaction
                CompletableFuture<Integer> innerResult = manager.executeInTransaction(innerTransaction -> {
                    innerTransaction.addOperation(() -> {
                        innerCounter.incrementAndGet();
                        return true;
                    }, null);

                    return CompletableFuture.completedFuture(innerCounter.get());
                });

                try {
                    innerResult.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return true;
            }, null);

            return CompletableFuture.completedFuture("nested");
        });

        String resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals("nested", resultValue, "Outer transaction should succeed");
        assertEquals(1, outerCounter.get(), "Outer operation should execute");
        assertEquals(1, innerCounter.get(), "Inner operation should execute");
    }

    @Test
    void testTransactionWithNullRollback() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();
        
        AtomicInteger value = new AtomicInteger(0);

        CompletableFuture<Integer> result = manager.executeInTransaction(
            () -> {
                value.incrementAndGet();
                return value.get();
            },
            null // No rollback action
        );

        Integer resultValue = result.get(5, TimeUnit.SECONDS);
        assertEquals(1, resultValue, "Transaction should succeed");
        assertEquals(1, value.get(), "Operation should have executed");
    }

    @Test
    void testMultipleSequentialTransactions() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            CompletableFuture<Boolean> result = manager.executeInTransaction(
                () -> {
                    counter.incrementAndGet();
                    return true;
                },
                null
            );
            assertTrue(result.get(5, TimeUnit.SECONDS), "Each transaction should succeed");
        }

        assertEquals(5, counter.get(), "All transactions should have executed");
    }
}
