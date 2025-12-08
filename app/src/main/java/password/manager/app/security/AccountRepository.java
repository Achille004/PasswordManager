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

package password.manager.app.security;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import password.manager.app.enums.SecurityVersion;
import password.manager.app.persistence.TransactionManager;
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
    private final List<Account> accounts;
    private final ObservableList<Account> observableAccountsUnmodifiable;
    private final TransactionManager transactionManager;

    private final ObjectProperty<SecurityVersion> securityVersionProperty;
    private final StringProperty masterPasswordProperty;
    
    // Tracks ongoing master password change operations
    private volatile CompletableFuture<Boolean> masterPasswordChangeFuture = CompletableFuture.completedFuture(true);
    private volatile CompletableFuture<Boolean> securityVersionChangeFuture = CompletableFuture.completedFuture(true);

    /**
     * Constructs a new AccountRepository with an empty synchronized observable list
     * and a transaction manager for asynchronous operations.
     */
    public AccountRepository(ObjectProperty<SecurityVersion> securityVersionProperty, StringProperty masterPasswordProperty) {
        this.accounts = Collections.synchronizedList(new ArrayList<>());
        this.transactionManager = new TransactionManager();

        this.observableAccountsUnmodifiable = FXCollections.unmodifiableObservableList(FXCollections.observableList(accounts));
        
        this.securityVersionProperty = securityVersionProperty;
        this.securityVersionProperty.addListener(this::securityVersionListener);

        this.masterPasswordProperty = masterPasswordProperty;
        this.masterPasswordProperty.addListener(this::masterPasswordListener);
    }

    /**
     * Retrieves all accounts in the repository.
     *
     * @return an unmodifiable observable list of all accounts
     */
    public ObservableList<Account> findAll() {
        return observableAccountsUnmodifiable;
    }

    /**
     * Replaces all accounts in the repository with the provided list.
     *
     * @param newAccounts the new list of accounts to set
     */
    public void setAll(@NotNull List<Account> newAccounts) {
        if (!accounts.isEmpty()) throw new IllegalStateException("Accounts list is not empty.");
        accounts.addAll(newAccounts);
    }

    /**
     * Creates and adds a new account to the repository within a transaction.
     * <p>
     * This operation encrypts the password and adds the account as a single atomic transaction.
     * If encryption fails, the transaction is rolled back and no account is added.
     * </p>
     *
     * @param software the name of the software/service for this account
     * @param username the username for this account
     * @param password the password to encrypt and store
     * @return a CompletableFuture that completes with the created Account, or null if the transaction fails
     */
    public @NotNull CompletableFuture<Account> add(@NotNull String software, @NotNull String username, @NotNull String password) {
        // Use a holder to capture the account reference for rollback
        final Account[] accountHolder = new Account[1];

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    Logger.getInstance().addDebug("Creating new account for security version: " + securityVersionProperty.get().name());
                    accountHolder[0] = Account.of(securityVersionProperty.get(), software, username, password, masterPasswordProperty.get());
                    accounts.add(accountHolder[0]);
                    return accountHolder[0];
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                    return null;
                }
            },
            () -> {
                if (accountHolder[0] != null) accounts.remove(accountHolder[0]);
            }
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
     * @param software the new name of the software/service
     * @param username the new username
     * @param password the new password to encrypt and store
     * @return a CompletableFuture that completes with the updated Account, or null if the transaction fails
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Account> edit(@NotNull Account account, @NotNull String software, @NotNull String username, @NotNull String password) {
        if (!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");

        // Capture complete original state for rollback using Account's memento pattern
        final Account.AccountMemento originalState = account.captureState();

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    account.setSoftware(software);
                    account.setUsername(username);
                    account.setPassword(securityVersionProperty.get(), password, masterPasswordProperty.get());
                    // Trigger ObservableList change notification
                    accounts.set(accounts.indexOf(account), account);
                    return account;
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                    return null;
                }
            },
            () -> {
                // Rollback all changes using captured state
                account.restoreState(originalState);
                // Trigger ObservableList change notification
                accounts.set(accounts.indexOf(account), account);
            }
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
        if (!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");

        // Capture index before transaction starts
        final int originalIndex = accounts.indexOf(account);

        return transactionManager.executeInTransaction(
            () -> accounts.remove(account),
            () -> accounts.add(originalIndex, account) // Rollback: restore at original position
        );
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
    public @NotNull CompletableFuture<String> getPassword(@NotNull Account account) {
        // Wait for any ongoing master password or security version changes to complete
        return CompletableFuture.allOf(masterPasswordChangeFuture, securityVersionChangeFuture)
            .thenCompose(_ -> CompletableFuture.supplyAsync(() -> {
                try {
                    return account.getPassword(securityVersionProperty.get(), masterPasswordProperty.get());
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                    return null;
                }
            }));
    }

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
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(_ -> futures.stream().allMatch(
                    f -> (f.isDone() && !f.isCompletedExceptionally() && f.join())
                ));
    }

    /**
     * Listener for changes in the security version property.
     * <p>
     * When the security version changes, this listener updates all
     * accounts to use the new security version within a single transaction. 
     * If any update fails, all changes are rolled back to the original states.
     * </p>
     * 
     * @param observable the observable value that changed
     * @param oldVersion the old security version
     * @param newVersion the new security version
     */
    private void securityVersionListener(ObservableValue<? extends SecurityVersion> observable, SecurityVersion oldVersion, SecurityVersion newVersion) {
        // Capture master password at the time of listener invocation
        final String currentMasterPassword = masterPasswordProperty.get();
        
        // Wait for any ongoing master password change to complete first
        securityVersionChangeFuture = masterPasswordChangeFuture.thenCompose(_ -> {
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
                                account.updateSecurityVersion(oldVersion, newVersion, currentMasterPassword);
                                // Trigger ObservableList change notification
                                accounts.set(accounts.indexOf(account), account);
                                return true;
                            } catch (GeneralSecurityException e) {
                                Logger.getInstance().addError(e);
                                return false;
                            }
                        },
                        () -> {
                            // Rollback using captured state
                            account.restoreState(originalState);
                            // Trigger ObservableList change notification
                            accounts.set(accounts.indexOf(account), account);
                        }
                    );

                    updateFutures.add(updateFuture);
                }

                return allSuccessful(updateFutures);
            });
        });

        securityVersionChangeFuture
                .thenAccept(success -> {
                    if (success) Logger.getInstance().addInfo("Updated all accounts to latest security version");
                    else Logger.getInstance().addError(new RuntimeException("Failed to update some accounts to latest security version"));
                })
                .exceptionally(e -> {
                    Logger.getInstance().addError(e);
                    return null;
                });
    }

    /**
     * Listener for changes in the master password property.
     * <p>
     * When the master password changes, this listener updates all accounts
     * to use the new password within a single transaction. If any update fails,
     * all changes are rolled back to the original states.
     * </p>
     * 
     * @param observable the observable value that changed
     * @param oldMasterPassword the old master password
     * @param newMasterPassword the new master password
     */
    private void masterPasswordListener(ObservableValue<? extends String> observable, String oldMasterPassword, String newMasterPassword) {
        // Wait for any ongoing security version change to complete first
        masterPasswordChangeFuture = securityVersionChangeFuture.thenCompose(_ -> {
            // Capture all accounts and their states before starting transaction
            List<Account> accountList;
            List<Account.AccountMemento> originalStates;
            synchronized (accounts) {
                accountList = new ArrayList<>(accounts);
                originalStates = accounts.stream().map(Account::captureState).toList();
            }

            return transactionManager.executeInTransaction(transaction -> {
                List<CompletableFuture<Boolean>> changeFutures = new ArrayList<>(accountList.size());
                SecurityVersion securityVersion = securityVersionProperty.get();

                for (int i = 0; i < accountList.size(); i++) {
                    Account account = accountList.get(i);
                    Account.AccountMemento originalState = originalStates.get(i);

                    CompletableFuture<Boolean> changeFuture = transaction.addOperation(
                        () -> {
                            try {
                                // Decrypt with old master password
                                String password = account.getPassword(securityVersion, oldMasterPassword);
                                // Re-encrypt with new master password
                                account.setPassword(securityVersion, password, newMasterPassword);
                                // Trigger ObservableList change notification
                                accounts.set(accounts.indexOf(account), account);
                                return true;
                            } catch (GeneralSecurityException e) {
                                Logger.getInstance().addError(e);
                                return false;
                            }
                        },
                        () -> {
                            // Rollback using captured state
                            account.restoreState(originalState);
                            // Trigger ObservableList change notification
                            accounts.set(accounts.indexOf(account), account);
                        }
                    );

                    changeFutures.add(changeFuture);
                }

                return allSuccessful(changeFutures);
            });
        });

        masterPasswordChangeFuture
                .thenAccept(success -> {
                    if (success) Logger.getInstance().addInfo("All account passwords re-encrypted successfully");
                    else Logger.getInstance().addError(new RuntimeException("Failed to re-encrypt some account passwords"));
                })
                .exceptionally(e -> {
                    Logger.getInstance().addError(e);
                    return null;
                });
    }
}
