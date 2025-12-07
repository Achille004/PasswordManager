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

    /**
     * Constructs a new AccountRepository with an empty synchronized observable list
     * and a transaction manager for asynchronous operations.
     */
    public AccountRepository() {
        accounts = Collections.synchronizedList(new ArrayList<>());
        transactionManager = new TransactionManager();

        observableAccountsUnmodifiable = FXCollections.unmodifiableObservableList(FXCollections.observableList(accounts));
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
     * @param securityVersion the encryption algorithm version to use
     * @param masterPassword the master password for encryption
     * @param software the name of the software/service for this account
     * @param username the username for this account
     * @param password the password to encrypt and store
     * @return a CompletableFuture that completes with the created Account, or null if the transaction fails
     */
    public @NotNull CompletableFuture<Account> add(
            @NotNull SecurityVersion securityVersion,
            @NotNull String masterPassword,
            @NotNull String software,
            @NotNull String username,
            @NotNull String password) {

        // Use a holder to capture the account reference for rollback
        final Account[] accountHolder = new Account[1];

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    accountHolder[0] = Account.of(securityVersion, software, username, password, masterPassword);
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
     * @param securityVersion the encryption algorithm version to use
     * @param masterPassword the master password for encryption
     * @param account the account to update (must exist in the repository)
     * @param software the new name of the software/service
     * @param username the new username
     * @param password the new password to encrypt and store
     * @return a CompletableFuture that completes with the updated Account, or null if the transaction fails
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Account> edit(
            @NotNull SecurityVersion securityVersion,
            @NotNull String masterPassword,
            @NotNull Account account,
            @NotNull String software,
            @NotNull String username,
            @NotNull String password) {

        if (!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");

        // Capture complete original state for rollback using Account's memento pattern
        final Account.AccountMemento originalState = account.captureState();

        return transactionManager.executeInTransaction(
            () -> {
                try {
                    account.setSoftware(software);
                    account.setUsername(username);
                    account.setPassword(securityVersion, password, masterPassword);
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
     *
     * @param securityVersion the encryption algorithm version used for this account
     * @param masterPassword the master password for decryption
     * @param account the account whose password to retrieve
     * @return a CompletableFuture that completes with the decrypted password, or null if decryption fails
     */
    public @NotNull CompletableFuture<String> getPassword(
                @NotNull SecurityVersion securityVersion,
                @NotNull String masterPassword,
                @NotNull Account account) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return account.getPassword(securityVersion, masterPassword);
            } catch (GeneralSecurityException e) {
                Logger.getInstance().addError(e);
                return null;
            }
        });
    }

    /**
     * Changes the master password for all accounts in the repository within a single transaction.
     * <p>
     * This operation re-encrypts all account passwords with the new master password.
     * All updates succeed or fail as a unit. If any update fails, all changes are rolled back.
     * </p>
     *
     * @param securityVersion the encryption algorithm version to use
     * @param oldMasterPassword the current master password for decryption
     * @param newMasterPassword the new master password for encryption
     * @return a CompletableFuture that completes with true if all passwords were changed successfully, false otherwise
     */
    public @NotNull CompletableFuture<Boolean> changeMasterPassword(
            @NotNull SecurityVersion securityVersion,
            @NotNull String oldMasterPassword,
            @NotNull String newMasterPassword) {

        // Capture all accounts and their states before starting transaction
        List<Account> accountList;
        List<Account.AccountMemento> originalStates;
        synchronized (accounts) {
            accountList = new ArrayList<>(accounts);
            originalStates = accounts.stream().map(Account::captureState).toList();
        }

        return transactionManager.executeInTransaction(transaction -> {
            List<CompletableFuture<Boolean>> changeFutures = new ArrayList<>(accountList.size());

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
    }

    /**
     * Updates all accounts to the latest security version within a single transaction.
     * <p>
     * This operation re-encrypts all account passwords using the latest security version.
     * All updates succeed or fail as a unit. If any update fails, all changes are rolled back.
     * </p>
     *
     * @param securityVersion the current security version of the accounts
     * @param masterPassword the master password for decryption and encryption
     * @return a CompletableFuture that completes with true if all accounts were updated successfully, false otherwise
     */
    public @NotNull CompletableFuture<Boolean> updateToLatestSecurityVersion(
            @NotNull SecurityVersion securityVersion,
            @NotNull String masterPassword) {

        // Capture all accounts and their states before starting transaction
        List<Account> accountList;
        List<Account.AccountMemento> originalStates;
        synchronized (accounts) {
            accountList = new ArrayList<>(accounts);
            originalStates = accounts.stream().map(Account::captureState).toList();
        }

        return transactionManager.executeInTransaction(transaction -> {
            List<CompletableFuture<Boolean>> updateFutures = new ArrayList<>();

            for (int i = 0; i < accountList.size(); i++) {
                Account account = accountList.get(i);
                Account.AccountMemento originalState = originalStates.get(i);

                CompletableFuture<Boolean> updateFuture = transaction.addOperation(
                    () -> {
                        try {
                            account.updateToLatestVersion(securityVersion, masterPassword);
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
    }

    @Override
    public void close() {
        transactionManager.shutdown();
    }

    private static CompletableFuture<Boolean> allSuccessful(Collection<CompletableFuture<Boolean>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(_ -> futures.stream().allMatch(
                    f -> (f.isDone() && !f.isCompletedExceptionally() && f.join())
                ));
    }
}
