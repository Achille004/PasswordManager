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

package password.manager.app.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import password.manager.app.singletons.Logger;

/**
 * Manages the lifecycle of transactions and provides a convenient API for transactional operations.
 * <p>
 * This manager creates and executes transactions, handling their lifecycle including
 * commit and rollback operations. It uses a virtual thread executor for async execution.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe. Multiple transactions can be executed concurrently.
 * </p>
 */
public class TransactionManager {

    private final AtomicInteger transactionProgressiveId = new AtomicInteger(0);
    private final ExecutorService executor;

    /**
     * Constructs a new TransactionManager with a virtual thread executor.
     */
    public TransactionManager() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Begins a new transaction.
     *
     * @return a new Transaction instance
     */
    public @NotNull Transaction beginTransaction(String description) {
        return new Transaction(executor, transactionProgressiveId.incrementAndGet(), description);
    }

    /**
     * Executes a function within a transaction context.
     * <p>
     * The transaction is automatically committed if the function completes successfully,
     * or rolled back if an exception occurs.
     * </p>
     *
     * @param <T> the return type of the function
     * @param transactionFunction the function to execute within the transaction context
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the function, or null if the transaction failed
     */
    public <T> @NotNull CompletableFuture<T> executeInTransaction(@NotNull Function<Transaction, CompletableFuture<T>> transactionFunction, String description) {
        Transaction transaction = beginTransaction(description);
        final CompletableFuture<T> transactionFuture;

        try {
            transactionFuture = transactionFunction.apply(transaction);
        } catch (Throwable ex) {
            transaction.rollback();
            return CompletableFuture.failedFuture(ex);
        }

        return transactionFuture
                .thenCompose(result ->
                    transaction.commit().thenApply(success -> success ? result : null)
                )
                .exceptionally(ex -> {
                    transaction.rollback();
                    throw new RuntimeException("Transaction failed and was rolled back", ex);
                });
    }

    /**
     * Shorthand method to execute an operation within a transaction with a rollback action.
     * See {@link #executeInTransaction(Function, String)} for details.
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute within the transaction
     * @param rollback the rollback action to perform if the transaction fails (can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the operation, or null if the transaction failed
     */
    public <T> @NotNull CompletableFuture<T> executeInTransaction(@NotNull Supplier<T> operation, @Nullable Runnable rollback, String description) {
        return executeInTransaction(t -> t.addOperation(operation, rollback), description);
    }

    /**
     * Shorthand method to execute an operation within a transaction with a rollback action.
     * See {@link #executeInTransaction(Supplier, Runnable, String)} for details.
     *
     * @param <T> the return type of the operation
     * @param operation the future operation to execute within the transaction
     * @param rollback the future rollback action to perform if the transaction fails (can be null)
     * @param description a description of the transaction for logging purposes
     * @return a CompletableFuture that completes with the result of the operation, or null if the transaction failed
     */
    public <T> @NotNull CompletableFuture<T> executeInTransaction(@NotNull CompletableFuture<T> operation, @Nullable Runnable rollback, String description) {
        return executeInTransaction(operation::join, rollback, description);
    }

    /**
     * Initiates an orderly shutdown of the executor service.
     * <p>
     * This method blocks until all tasks have completed execution, or the thread is interrupted.
     * </p>
     */
    public void shutdown() {
        if (isShutdown()) return;

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Logger.getInstance().addError(e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks if the executor service has been shut down.
     *
     * @return true if the executor has been shut down, false otherwise
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
