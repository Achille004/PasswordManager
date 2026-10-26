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
package password.manager.app.security

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import password.manager.app.Utils.runOnFx
import password.manager.app.persistence.TransactionManager
import password.manager.app.security.Account.AccountData
import password.manager.app.singletons.Logger
import java.lang.AutoCloseable
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

/**
 * Repository for managing password accounts with CRUD operations.
 *
 * This repository provides thread-safe access to a collection of encrypted password accounts.
 * Transactional operations (create, update, delete, and unlock) run asynchronously on virtual threads.
 * Password reads are asynchronous on the default [CompletableFuture] executor to prevent blocking the JavaFX UI thread.
 *
 * The repository maintains an [ObservableList] of accounts, allowing UI components
 * to react to changes automatically. Encryption and decryption operations are performed
 * in background threads and return [CompletableFuture] objects for handling results.
 *
 * Thread Safety: This class is thread-safe. All transactional operations properly synchronize
 * access to the internal account list. Each transaction is isolated, and the repository uses
 * the Memento pattern to capture and restore account state for proper rollback support.
 * However, note that individual [password.manager.app.persistence.Transaction] objects
 * are NOT thread-safe and should not be shared between threads.
 *
 * @see Account
 * @see UserPreferences
 * @see TransactionManager
 */
class AccountRepository(private val userPreferences: UserPreferences) : AutoCloseable {
    // This needs to internally be an observable list to allow UI to react to changes.
    // The synchronized wrapper was removed since the ListChangeBuilder of the wrapping ObservableList was
    // suffering from a broken internal state due to concurrent modifications, now synchronization is manual.
    private val accounts: ObservableList<Account> = FXCollections.observableList(ArrayList()) {
        // This makes so that UPDATED events are fired when one of the observables in the extracted array changes
        account: Account -> arrayOf(account.softwareProperty, account.usernameProperty)
    }
    private val accountsView = FXCollections.unmodifiableObservableList(accounts)
    private val transactionManager: TransactionManager = TransactionManager()

    /**
     * Retrieves all accounts in the repository.
     * The returned list is an unmodifiable view of just the account references.
     *
     * @return an unmodifiable observable list of all accounts
     */
    fun findAll(): ObservableList<Account> = accountsView

    /**
     * Sets all accounts in the repository with the provided list.
     *
     * @param newAccounts the new list of accounts to set
     * @return true if this list changed as a result of the call
     */
    fun setAll(newAccounts: List<Account>) = synchronized(accounts) {
        check(accounts.isEmpty()) { "Accounts list is not empty." }
        accounts.setAll(newAccounts)
    }

    /**
     * Sets all accounts in the repository with the provided array.
     *
     * @param newAccounts the new list of accounts to set
     * @return true if this list changed as a result of the call
     */
    fun setAll(vararg newAccounts: Account) = setAll(newAccounts.asList())

    /**
     * Creates and adds a new account to the repository within a transaction.
     *
     * This operation encrypts the password and adds the account as a single atomic transaction.
     * If encryption fails, the transaction is rolled back and no account is added.
     *
     * @param data the data for the new account to create
     * @return a CompletableFuture that completes with the created Account, or null if the transaction fails
     */
    fun add(data: AccountData): CompletableFuture<Account?> {
        // Use a holder to capture the account reference for rollback
        val accountHolder = arrayOf<Account?>(null)

        return transactionManager.executeInTransaction(
            {
                try {
                    accountHolder[0] = Account.of(data, userPreferences.dek)

                    runOnFx {
                        synchronized(accounts) {
                            accounts.add(accountHolder[0])
                        }
                    }.join()

                    return@executeInTransaction accountHolder[0]
                } catch (e: GeneralSecurityException) {
                    throw RuntimeException("Failed to encrypt account data", e)
                }
            },
            {
                accountHolder[0] ?: return@executeInTransaction

                runOnFx {
                    synchronized(accounts) {
                        accounts.remove(accountHolder[0])
                    }
                }.join()
            },
            "Adding account"
        )
    }

    /**
     * Updates an existing account with new data within a transaction.
     *
     * This operation re-encrypts the password with the new data as a single atomic transaction.
     * If any step fails, all changes are rolled back to the original state.
     *
     * @param account the account to update (must exist in the repository)
     * @param data the new data to set on the account
     * @return a CompletableFuture that completes with the updated Account, or null if the transaction fails
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    fun edit(account: Account, data: AccountData): CompletableFuture<Account?> {
        val originalState = synchronized(accounts) {
            require(accounts.contains(account)) { "Account not found in list" }
            // Capture complete original state for rollback using Account's memento pattern
            account.captureState()
        }

        return transactionManager.executeInTransaction(
            {
                try {
                    account.setData(data, userPreferences.dek)
                    return@executeInTransaction account
                } catch (e: GeneralSecurityException) {
                    throw RuntimeException("Failed to update account data", e)
                }
            },
            {
                // Roll back all changes using captured state
                account.restoreState(originalState)
            },
            "Editing account"
        )
    }

    /**
     * Removes an account from the repository within a transaction.
     *
     * This operation removes the account as a single atomic transaction.
     * If removal needs to be rolled back, the account is re-added at its original position.
     *
     * @param account the account to remove (must exist in the repository)
     * @return a CompletableFuture that completes with true if removal was successful, false otherwise
     * @throws IllegalArgumentException if the account is not found in the repository
     */
    fun remove(account: Account): CompletableFuture<Boolean?> {
        // Although DEK is not used in this method, we check it to ensure that the master password has been verified
        // before allowing any modifications to the accounts list.
        userPreferences.dek

        val originalIndex: Int = synchronized(accounts) {
            require(accounts.contains(account)) { "Account not found in list" }
            // Capture index before transaction starts
            accounts.indexOf(account)
        }

        return transactionManager.completeInTransaction(
            // Only scheduled once the transaction has been opened
            {
                runOnFx(Callable {
                    synchronized(accounts) {
                        return@Callable accounts.remove(account)
                    }
                })
            },
            // Leave as lambda so that the rollback action is only run if needed
            // Writing it as runOnFx(...)::join would result in join() being the runnable instead of the lambda itself
            {
                runOnFx {
                    synchronized(accounts) {
                        // Rollback: restore at original position
                        accounts.add(originalIndex, account)
                    }
                }.join()
            },
            "Removing account"
        )
    }

    /**
     * Unlocks all accounts in the repository, using the provided legacy master password for upgrading accounts to DEK-based encryption if needed.
     *
     * This method is intended to be called after successful master password verification to ensure all accounts are decrypted and ready for use.
     * The operation is performed as a single transaction that attempts to unlock each account. If any account fails to
     * unlock, all accounts are rolled back to their original locked state.
     *
     * @param legacyMasterPassword the master password for legacy accounts, can be null if not in legacy mode.
     * @return a CompletableFuture that completes with true if all accounts were successfully unlocked, false if any account failed to unlock
     */
    fun unlockAll(legacyMasterPassword: String?): CompletableFuture<Boolean> {
        val accountList: MutableList<Account> = synchronized(accounts) {
            // No accounts to unlock, consider it successful
            if (accounts.isEmpty()) return CompletableFuture.completedFuture(true)
            // Make a local copy of the list
            ArrayList(accounts)
        }

        return transactionManager.executeBatchInTransaction(
            accountList,
            { account: Account ->
                try {
                    account.unlock(userPreferences.dek, userPreferences.legacyVersion, legacyMasterPassword)
                    return@executeBatchInTransaction true
                } catch (e: GeneralSecurityException) {
                    throw RuntimeException("Failed to unlock account", e)
                }
            },  // Capture complete original state for rollback using Account's memento pattern
            { account: Account? ->
                val originalState = account!!.captureState()
                Runnable { account.restoreState(originalState) }
            },
            "Unlocking all accounts"
        )
            // A failed operation returns null, so a non-null result means every account was unlocked
            .thenApply(Objects::nonNull)
    }

    /**
     * Retrieves and decrypts the password for an account asynchronously.
     *
     * Decryption runs asynchronously on the default [CompletableFuture] executor (the common pool) to avoid blocking
     * the UI. This lightweight, read-only operation does not use transactions.
     *
     * This method will wait for any ongoing master password or security version changes to complete before attempting
     * to decrypt the password, ensuring consistency.
     *
     * @param account the account whose password to retrieve
     * @return a CompletableFuture that completes with the decrypted account data; if decryption fails, the future
     * completes exceptionally with a [RuntimeException]. Failures are logged and remain exceptional.
     */
    fun getData(account: Account): CompletableFuture<AccountData> =
        // Wait for any ongoing master password or security version changes to complete
        CompletableFuture.supplyAsync {
            try {
                return@supplyAsync account.getData(userPreferences.dek)
            } catch (e: GeneralSecurityException) {
                throw RuntimeException("Failed to decrypt account data", e)
            }
        }.whenComplete { _, error ->
            if (error != null) Logger.addError(error)
        }

    /**
     * Closes the repository and shuts down the transaction manager.
     *
     * This method should be called when the repository is no longer needed
     * to release resources held by the transaction manager.
     *
     * **Note:** After calling this method, the account list is still accessible,
     * but no further transactions can be executed.
     */
    override fun close() = transactionManager.shutdown()
}
