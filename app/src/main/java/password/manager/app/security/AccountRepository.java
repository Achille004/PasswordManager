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

package password.manager.app.security;

import static password.manager.app.Utils.runOnFx;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import password.manager.app.base.SecurityVersion;
import password.manager.app.persistence.TransactionManager;
import password.manager.app.security.Account.AccountData;
import password.manager.app.singletons.Logger;

/**
 * Repository for managing password accounts with CRUD operations.
 * <p>
 * This repository provides thread-safe access to a collection of encrypted password accounts.
 * All data modification operations (create, update, delete, read passwords) are executed
 * asynchronously using virtual threads to prevent blocking the JavaFX UI thread.
 * </p>
 * <p>
 * The repository maintains an {@link ObservableList} of accounts, allowing UI components
 * to react to changes automatically. Encryption and decryption operations are performed
 * in background threads and return {@link CompletableFuture} objects for handling results.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe. All transactional operations properly synchronize
 * access to the internal account list. Each transaction is isolated, and the repository uses
 * the Memento pattern to capture and restore account state for proper rollback support.
 * However, note that individual {@link password.manager.app.persistence.Transaction} objects
 * are NOT thread-safe and should not be shared between threads.
 * </p>
 *
 * @see Account
 * @see SecurityVersion
 * @see password.manager.app.persistence.TransactionManager
 */
public final class AccountRepository implements AutoCloseable {

    // This needs to internally be an observable list to allow UI to react to changes
    private final ObservableList<Account> accounts;
    private final TransactionManager transactionManager;

    private transient byte[] DEK;

    /**
     * Constructs a new AccountRepository with an empty synchronized observable list
     * and a transaction manager for asynchronous operations.
     */
    public AccountRepository() {
        // The synchronized wrapper was removed since the ListChangeBuilder of the wrapping ObservableList was
        // suffering from broken internal state due to concurrent modifications, now synchronization is manual.
        this.accounts = FXCollections.observableList(
            new ArrayList<>(),
            // This makes so that UPDATED events are fired when one of the observables in the extracted array changes
            account -> new Observable[] { account.softwareProperty(), account.usernameProperty() }
        );
        this.transactionManager = new TransactionManager();

        this.DEK = null;
    }

    public void setDEK(@NotNull byte[] DEK) {
        if (DEK == null) throw new IllegalArgumentException("Data encryption key cannot be null");
        if (this.DEK != null) throw new IllegalStateException("Data encryption key is already set.");
        this.DEK = DEK.clone(); // Defensive copy to prevent external modification
    }

    /**
     * Retrieves all accounts in the repository.
     *
     * @return an unmodifiable observable list of all accounts
     */
    public ObservableList<Account> findAll() {
        synchronized (accounts) {
            return FXCollections.unmodifiableObservableList(accounts);
        }
    }

    /**
     * Replaces all accounts in the repository with the provided list.
     *
     * @param newAccounts the new list of accounts to set
     */
    public void setAll(@NotNull List<Account> newAccounts) {
        synchronized (accounts) {
            if (!accounts.isEmpty()) throw new IllegalStateException("Accounts list is not empty.");
            accounts.addAll(newAccounts);
        }
    }

    /**
     * Creates and adds a new account to the repository within a transaction.
     * <p>
     * This operation encrypts the password and adds the account as a single atomic transaction.
     * If encryption fails, the transaction is rolled back and no account is added.
     * </p>
     *
     * @param data the data for the new account to create
     * @return a CompletableFuture that completes with the created Account, or null if the transaction fails
     */
    public @NotNull CompletableFuture<Account> add(@NotNull AccountData data) {
        if (DEK == null) throw new IllegalStateException("Data encryption key is not set. Master password may not have been verified yet.");
        if (data == null) throw new NullPointerException("Account data cannot be null");

        // Use a holder to capture the account reference for rollback
        final Account[] accountHolder = new Account[1];

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    accountHolder[0] = Account.of(data, DEK);

                    runOnFx(() -> {
                        synchronized (accounts) {
                            accounts.add(accountHolder[0]);
                        }
                    }).join();

                    return accountHolder[0];
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                    return null;
                }
            },
            () -> {
                if (accountHolder[0] != null) {
                    runOnFx(() -> {
                        synchronized (accounts) {
                            accounts.remove(accountHolder[0]);
                        }
                    }).join();
                }
            },
            "Adding Account"
        );
    }

    /**
     * Updates an existing account with new data within a transaction.
     * <p>
     * This operation re-encrypts the password with the new data as a single atomic transaction.
     * If any step fails, all changes are rolled back to the original state.
     * </p>
     *
     * @param account the account to update (must exist in the repository)
     * @param data the new data to set on the account
     * @return a CompletableFuture that completes with the updated Account, or null if the transaction fails
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Account> edit(@NotNull Account account, @NotNull AccountData data) {
        if (DEK == null) throw new IllegalStateException("Data encryption key is not set. Master password may not have been verified yet.");
        if (account == null) throw new NullPointerException("Account cannot be null");
        if (data == null) throw new NullPointerException("Account data cannot be null");

        final Account.AccountMemento originalState;
        synchronized (accounts) {
            if (!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");

            // Capture complete original state for rollback using Account's memento pattern
            originalState = account.captureState();
        }

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    account.setData(data, DEK);
                    return account;
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                    return null;
                }
            },
            () -> {
                // Rollback all changes using captured state
                account.restoreState(originalState);
            },
            "Editing Account"
        );
    }

    /**
     * Removes an account from the repository within a transaction.
     * <p>
     * This operation removes the account as a single atomic transaction.
     * If removal needs to be rolled back, the account is re-added at its original position.
     * </p>
     *
     * @param account the account to remove (must exist in the repository)
     * @return a CompletableFuture that completes with true if removal was successful, false otherwise
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Boolean> remove(@NotNull Account account) {
        // Although DEK is not used in this method, we check it to ensure that the master password has been verified before allowing any modifications to the accounts list.
        if (DEK == null) throw new IllegalStateException("Data encryption key is not set. Master password may not have been verified yet.");
        if (account == null) throw new NullPointerException("Account cannot be null");

        final int originalIndex;
        synchronized (accounts) {
            if (!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");

            // Capture index before transaction starts
            originalIndex = accounts.indexOf(account);
        }

        return transactionManager.executeInTransaction(
            () -> {
                runOnFx(() -> {
                    synchronized (accounts) {
                        accounts.remove(account);
                    }
                }).join();
                return true;
            },
            () -> {
                runOnFx(() -> {
                    synchronized (accounts) {
                        // Rollback: restore at original position
                        accounts.add(originalIndex, account);
                    }
                }).join();
            },
            "Removing Account"
        );
    }

    /**
     * Unlocks all accounts in the repository, using the provided legacy master password for upgrading accounts to DEK-based encryption if needed.
     * <p>
     * This method is intended to be called after successful master password verification to ensure all accounts are decrypted and ready for use.
     * The operation is performed as a single transaction that attempts to unlock each account. If any account fails to unlock, all accounts are rolled back to their original locked state.
     * </p>
     *
     * @param legacyMasterPassword the master password for legacy accounts, can be null if not in legacy mode.
     * @param legacyVersion the security version of the legacy accounts, can be null if not in legacy mode.
     * @return a CompletableFuture that completes with true if all accounts were successfully unlocked, false if any account failed to unlock
     */
    public @NotNull CompletableFuture<Boolean> unlockAll(@Nullable String legacyMasterPassword, @Nullable SecurityVersion legacyVersion) {
        if (DEK == null) throw new IllegalStateException("Data encryption key is not set. Master password may not have been verified yet.");

        List<Account> accountList;
        List<Account.AccountMemento> originalStates;
        synchronized (accounts) {
            accountList = new ArrayList<>(accounts);
            originalStates = accounts.stream().map(Account::captureState).toList();
        }

        return transactionManager.executeInTransaction(transaction -> {
            List<CompletableFuture<Boolean>> updateFutures = new ArrayList<>(accountList.size());

            for (int i = 0; i < accountList.size(); i++) {
                Account account = accountList.get(i);
                Account.AccountMemento originalState = originalStates.get(i);

                CompletableFuture<Boolean> updateFuture = transaction.addOperation(
                    () -> {
                        try {
                            account.unlock(DEK, legacyVersion, legacyMasterPassword);
                            return true;
                        } catch (GeneralSecurityException e) {
                            Logger.getInstance().addError(e);
                            return false;
                        }
                    },
                    () -> {
                        // Rollback using captured state
                        account.restoreState(originalState);
                    }
                );

                updateFutures.add(updateFuture);
            }

            return allSuccessful(updateFutures);
        }, "Unlocking All Accounts");
    }

    /**
     * Retrieves and decrypts the password for an account asynchronously.
     * <p>
     * This operation decrypts the password using the specified security version and master password
     * in a background thread to avoid blocking the UI. This is a read-only operation and does not
     * use transactions.
     * </p>
     * <p>
     * This method will wait for any ongoing master password or security version changes to complete
     * before attempting to decrypt the password, ensuring consistency.
     * </p>
     *
     * @param account the account whose password to retrieve
     * @return a CompletableFuture that completes with the decrypted password, or null if decryption fails
     */
    public @NotNull CompletableFuture<AccountData> getData(@NotNull Account account) {
        // Wait for any ongoing master password or security version changes to complete
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        return account.getData(DEK);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                });
    }

    /**
     * Closes the repository and shuts down the transaction manager.
     * <p>
     * This method should be called when the repository is no longer needed
     * to release resources held by the transaction manager.
     * </p>
     * <strong>Note:</strong> After calling this method, the account list is still accessible,
     * but no further transactions can be executed.
     */
    @Override
    public void close() {
        transactionManager.shutdown();
    }

    /**
     * Helper method to check if all CompletableFutures in a collection completed successfully with true.
     * @param futures the collection of CompletableFutures to check
     * @return a CompletableFuture that completes with true if all futures completed successfully with true, false otherwise
    */
    private static CompletableFuture<Boolean> allSuccessful(Collection<CompletableFuture<Boolean>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> futures.stream().allMatch(
                    f -> (f.isDone() && !f.isCompletedExceptionally() && f.join())
                ));
    }
}
