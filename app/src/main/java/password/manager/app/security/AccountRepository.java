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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import password.manager.app.enums.SecurityVersion;
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
 * Thread Safety: This class is thread-safe. The internal list is synchronized and all
 * operations are executed through a dedicated executor service.
 * </p>
 * 
 * @see Account
 * @see SecurityVersion
 */
public class AccountRepository {
    private final ObservableList<Account> accounts;
    private final ExecutorService executor;

    /**
     * Constructs a new AccountRepository with an empty synchronized observable list
     * and a virtual thread executor for asynchronous operations.
     */
    public AccountRepository() {
        accounts = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Retrieves all accounts in the repository.
     * 
     * @return an unmodifiable observable list of all accounts
     */
    public ObservableList<Account> findAll() {
        return FXCollections.unmodifiableObservableList(this.accounts);
    }

    /**
     * Replaces all accounts in the repository with the provided list.
     * 
     * @param newAccounts the new list of accounts to set
     */
    public void setAll(List<Account> newAccounts) {
        accounts.setAll(newAccounts);
    }

    /**
     * Creates and adds a new account to the repository asynchronously.
     * <p>
     * This operation encrypts the password using the specified security version and master password
     * in a background thread to avoid blocking the UI.
     * </p>
     * 
     * @param securityVersion the encryption algorithm version to use
     * @param masterPassword the master password for encryption
     * @param software the name of the software/service for this account
     * @param username the username for this account
     * @param password the password to encrypt and store
     * @return a CompletableFuture that completes with the created Account, or null if encryption fails
     */
    public @NotNull CompletableFuture<Account> add(
            @NotNull SecurityVersion securityVersion,
            @NotNull String masterPassword,
            @NotNull String software, 
            @NotNull String username, 
            @NotNull String password) {

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        Account a = Account.of(securityVersion, software, username, password, masterPassword);
                        accounts.add(a);
                        return a;
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                }, executor);
    }

    /**
     * Updates an existing account with new data asynchronously.
     * <p>
     * This operation re-encrypts the password with the new data using the specified security version
     * and master password in a background thread. The list change is triggered to notify listeners.
     * </p>
     * 
     * @param securityVersion the encryption algorithm version to use
     * @param masterPassword the master password for encryption
     * @param account the account to update (must exist in the repository)
     * @param software the new name of the software/service
     * @param username the new username
     * @param password the new password to encrypt and store
     * @return a CompletableFuture that completes with the updated Account, or null if encryption fails
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Account> edit(
                @NotNull SecurityVersion securityVersion,
                @NotNull String masterPassword,
                @NotNull Account account, 
                @NotNull String software, 
                @NotNull String username, 
                @NotNull String password) {

        if(!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        account.setData(securityVersion, software, username, password, masterPassword);
                        accounts.set(accounts.indexOf(account), account); // trigger the list change listeners, if any
                        return account;
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                }, executor);
    }

    /**
     * Removes an account from the repository asynchronously.
     * 
     * @param account the account to remove (must exist in the repository)
     * @return a CompletableFuture that completes with true if removal was successful, false otherwise
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    public @NotNull CompletableFuture<Boolean> remove(@NotNull Account account) {
        if(!accounts.contains(account)) throw new IllegalArgumentException("Account not found in list");
        return CompletableFuture.supplyAsync(() -> accounts.remove(account), executor);
    }

    /**
     * Retrieves and decrypts the password for an account asynchronously.
     * <p>
     * This operation decrypts the password using the specified security version and master password
     * in a background thread to avoid blocking the UI.
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

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return account.getPassword(securityVersion, masterPassword);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                }, executor);
    }

    /**
     * Executes an action on all accounts asynchronously.
     * <p>
     * Each account is processed in a separate virtual thread. The iteration over the list
     * is synchronized to ensure thread safety.
     * </p>
     * 
     * @param action the action to perform on each account
     */
    public void executeOnAll(Consumer<? super Account> action) {
        // MUST wrap iteration in synchronized(...) when using Collections.synchronizedList
        synchronized(accounts) {
            accounts.forEach(account -> executor.submit(() -> action.accept(account)));
        }
    }

    /**
     * Initiates an orderly shutdown of the executor service.
     * <p>
     * This method blocks until all tasks have completed execution, or the thread is interrupted.
     * If interrupted, it attempts to cancel currently executing tasks.
     * </p>
     */
    public void shutdown() {
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
