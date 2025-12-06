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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import password.manager.app.singletons.Logger;

/**
 * Represents a transaction that encapsulates a series of operations that can be committed or rolled back.
 * <p>
 * A transaction maintains a list of operations and their corresponding rollback actions.
 * All operations are executed asynchronously and can be committed as a unit or rolled back
 * if any operation fails.
 * </p>
 * <p>
 * Thread Safety: This class is NOT thread-safe. A transaction should be used by a single thread
 * or properly synchronized externally.
 * </p>
 */
public class Transaction {

    private final List<CompletableFuture<?>> operations = new ArrayList<>();
    private final List<Runnable> rollbackActions = new ArrayList<>();
    private final ExecutorService executor;

    private boolean committed = false;
    private boolean rolledBack = false;

    /**
     * Constructs a new Transaction with the specified executor service.
     *
     * @param executor the executor service to use for async operations
     */
    public Transaction(@NotNull ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Adds an operation to this transaction with an optional rollback action.
     *
     * @param operation the operation to execute
     * @param rollback the rollback action to execute if the transaction fails (can be null)
     * @param <T> the type of the operation result
     * @return a CompletableFuture representing the operation
     * @throws IllegalStateException if the transaction has already been committed or rolled back
     */
    public <T> CompletableFuture<T> addOperation(@NotNull Supplier<T> operation, Runnable rollback) {
        if (committed) throw new IllegalStateException("Transaction has already been committed");
        if (rolledBack) throw new IllegalStateException("Transaction has already been rolled back");

        CompletableFuture<T> future = CompletableFuture.supplyAsync(operation, executor);
        operations.add(future);

        if (rollback != null) rollbackActions.add(rollback);
        return future;
    }

    /**
     * Commits the transaction, waiting for all operations to complete.
     * <p>
     * If any operation fails, the transaction is automatically rolled back.
     * </p>
     *
     * @return a CompletableFuture that completes with true if all operations succeeded, false otherwise
     */
    public @NotNull CompletableFuture<Boolean> commit() {
        if (committed || rolledBack) return CompletableFuture.completedFuture(false);

        return CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Check if any operation failed
                    boolean anyFailed = operations.stream().anyMatch(
                        op -> op.isCompletedExceptionally() || (op.isDone() && op.join() == null)
                    );

                    if (anyFailed) {
                        rollback();
                        return false;
                    }

                    committed = true;
                    return true;
                })
                .exceptionally(ex -> {
                    rollback();
                    return false;
                });
    }

    /**
     * Rolls back the transaction by executing all registered rollback actions in reverse order.
     */
    public void rollback() {
        if (rolledBack) return;

        // Execute rollback actions in reverse order
        for (int i = rollbackActions.size() - 1; i >= 0; i--) {
            try {
                rollbackActions.get(i).run();
            } catch (Exception e) {
                // Log but continue with remaining rollbacks
                Logger.getInstance().addError(e);
            }
        }

        rolledBack = true;
    }

    /**
     * Checks if the transaction has been committed.
     *
     * @return true if committed, false otherwise
     */
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Checks if the transaction has been rolled back.
     *
     * @return true if rolled back, false otherwise
     */
    public boolean isRolledBack() {
        return rolledBack;
    }
}
