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

package password.manager.app.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    public @NotNull Transaction beginTransaction() {
        return new Transaction(executor);
    }

    /**
     * Executes a function within a transaction context.
     * <p>
     * The transaction is automatically committed if the function completes successfully,
     * or rolled back if an exception occurs.
     * </p>
     *
     * @param transactionFunction the function to execute within the transaction context
     * @param <T> the return type of the function
     * @return a CompletableFuture that completes with the result of the function, or null if the transaction failed
     */
    public <T> @NotNull CompletableFuture<T> executeInTransaction(@NotNull Function<Transaction, CompletableFuture<T>> transactionFunction) {
        Transaction transaction = beginTransaction();

        return transactionFunction.apply(transaction)
                .thenCompose(result ->
                    transaction.commit().thenApply(success -> success ? result : null)
                )
                .exceptionally(ex -> {
                    Logger.getInstance().addError(ex);
                    transaction.rollback();
                    return null;
                });
    }

    /**
     * Shorthand method to execute an operation within a transaction with a rollback action.
     * See {@link #executeInTransaction} for details.
     * 
     * @param <T> the return type of the operation
     * @param operation the operation to execute within the transaction
     * @param rollback the rollback action to perform if the transaction fails
     * @return a CompletableFuture that completes with the result of the operation, or null if the transaction failed
     */
    public <T> @NotNull CompletableFuture<T> executeInTransaction(@NotNull Supplier<T> operation, Runnable rollback) {
        return executeInTransaction(t -> t.addOperation(operation, rollback));
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
