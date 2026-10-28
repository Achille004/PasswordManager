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
package password.manager.app.singletons

import com.fasterxml.jackson.annotation.JsonProperty
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import password.manager.app.AppConfig
import password.manager.app.Utils
import password.manager.app.base.SortingOrder
import password.manager.app.base.SupportedLocale
import password.manager.app.security.*
import password.manager.lib.CustomPasswordField
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate
import kotlin.concurrent.Volatile
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

object IOManager : Singleton() {
    private const val DATA_FILE_NAME = "data.json"
    private const val BACKUP_FILE_NAME = "data.json.bak"

    private val DATA_FILE: File
    private val BACKUP_FILE: File
    private val PRESERVED_PATH: Path

    private const val AUTOSAVE_INTERVAL_MIN = 2L
    private const val AUTOSAVE_TERMINATION_SEC = 5L

    init {
        val basePath = AppConfig.basePath
        DATA_FILE = basePath.resolve(DATA_FILE_NAME).toFile()
        BACKUP_FILE = basePath.resolve(BACKUP_FILE_NAME).toFile()
        PRESERVED_PATH = basePath.resolve("preserved")
    }

    val userPreferences: UserPreferences
    val accountRepository: AccountRepository

    // Volatile fields publish the authentication state to readers on other threads.
    @Volatile
    private var masterPassword: String? = null
    @Volatile
    var isAuthenticated = false

    // Assume first run until data is loaded
    @Volatile
    var isFirstRun = true

    private val hasChanged: AtomicBoolean
    private val loadingLock: Lock

    enum class SaveState { SUCCESS, SAVING, ERROR }
    private val savingProperty: ReadOnlyObjectWrapper<SaveState>

    private val objectMapper: ObjectMapper
    private val autosaveScheduler: ScheduledExecutorService

    // Let only package classes instantiate this
    init {
        Logger.addDebug("DATA_FILE: '$DATA_FILE'")
        Logger.addDebug("BACKUP_FILE: '$BACKUP_FILE'")
        Logger.addDebug("PRESERVED_PATH: '$PRESERVED_PATH'")

        userPreferences = UserPreferences.empty()
        accountRepository = AccountRepository(userPreferences)

        hasChanged = AtomicBoolean(false)
        loadingLock = ReentrantLock()

        savingProperty = ReadOnlyObjectWrapper(SaveState.SUCCESS)
        setupListeners()

        objectMapper = ObjectMapper()
        autosaveScheduler = Executors.newSingleThreadScheduledExecutor()
        autosaveScheduler.scheduleAtFixedRate(
            this::saveData,
            AUTOSAVE_INTERVAL_MIN,
            AUTOSAVE_INTERVAL_MIN,
            TimeUnit.MINUTES
        )

        loadData()
        registerSingleton()
    }

    val accountList: ObservableList<Account>
        get() = this.accountRepository.findAll()

    /**
     * Returns a read-only property that indicates the current save state.
     * You can listen to this property to show/hide save status UI.
     * @return [ReadOnlyObjectProperty] of the current [SaveState]
     */
    fun savingProperty(): ReadOnlyObjectProperty<SaveState> =  this.savingProperty.readOnlyProperty

    // #region Persistence and lifecycle management
    private fun setupListeners() {
        val propListener = ChangeListener { _, _: Any?, _: Any? ->
            if (!loadingLock.tryLock()) return@ChangeListener  // prevents triggering when loading data

            hasChanged.set(true)
            loadingLock.unlock()
        }

        userPreferences.localeProperty().addListener(propListener)
        userPreferences.sortingOrderProperty().addListener(propListener)

        val listListener: ListChangeListener<Account> =
            ListChangeListener { change: ListChangeListener.Change<out Account> ->
                if (!loadingLock.tryLock()) return@ListChangeListener
                while (change.next()) {
                    if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                        hasChanged.set(true)
                        break // no need to check further changes
                    }
                }
                loadingLock.unlock()
            }

        this.accountList.addListener(listListener)

        userPreferences.localeProperty().addListener { _, _, newValue: SupportedLocale ->
            val newValueStr = newValue.locale.getDisplayLanguage(Locale.ENGLISH)
            Logger.addDebug("Changed locale to: $newValueStr")
        }
        userPreferences.sortingOrderProperty().addListener { _, _, newValue: SortingOrder ->
            Logger.addDebug("Changed sorting order to: $newValue")
        }
    }

    private fun loadData() {
        if (!(DATA_FILE.exists() || BACKUP_FILE.exists())) {
            Logger.addInfo("Neither DATA_FILE nor BACKUP_FILE exist, skipping data loading")
            return
        }

        // Try to load DATA_FILE
        try {
            loadDataFile(DATA_FILE, "DATA_FILE")

            Files.copy(DATA_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Logger.addInfo("Copied valid DATA_FILE to BACKUP_FILE")

            return
        } catch (_: FileNotFoundException) {
            // This is handled below
        } catch (e: IOException) {
            Logger.addError(e)
        }

        // Try to load BACKUP_FILE
        try {
            loadDataFile(BACKUP_FILE, "BACKUP_FILE")

            // Move corrupted DATA_FILE to preserved with timestamp, but only if it exists
            if (DATA_FILE.exists()) {
                val timestamp = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .format(LocalDateTime.now())

                PRESERVED_PATH.toFile().mkdirs()
                Files.move(
                    DATA_FILE.toPath(),
                    PRESERVED_PATH.resolve("$timestamp.json"),
                    StandardCopyOption.REPLACE_EXISTING
                )
                Logger.addInfo("Moved corrupted DATA_FILE to 'preserved/$timestamp.json'")
            }

            hasChanged.set(true) // Force save to recreate DATA_FILE from backup
            return
        } catch (_: FileNotFoundException) {
            // This is handled below
        } catch (e: IOException) {
            Logger.addError(e)
        }

        // If both main and backup failed, ask user to overwrite
        Logger.addInfo("Asking user to overwrite data")
        val errMsg = ObservableResourceFactory.getValue("data_error")
        val alert = Alert(Alert.AlertType.ERROR, errMsg, ButtonType.YES, ButtonType.NO)
        Utils.setDefaultButton(alert, ButtonType.NO)
        alert.showAndWait()

        if (alert.result == ButtonType.YES) {
            Logger.addInfo("Data overwriting accepted")
        } else {
            Logger.addInfo("Data overwriting denied")
            exitProcess(0)
        }
    }

    @Synchronized
    private fun saveData() {
        if (!isAuthenticated) {
            Logger.addInfo("Skipping save: Not authenticated")
            return
        }

        if (!hasChanged.get()) {
            Logger.addInfo("Skipping save: No changes")
            return
        }

        Logger.addInfo("Saving data...")
        Platform.runLater { savingProperty.set(SaveState.SAVING) }

        // Change preemptively to avoid losing if changes are added during save
        hasChanged.set(false)

        try {
            saveDataFile(DATA_FILE)
            Logger.addInfo("Save OK")
            Platform.runLater { savingProperty.set(SaveState.SUCCESS) }
        } catch (e: IOException) {
            Logger.addError(e)
            Platform.runLater { savingProperty.set(SaveState.ERROR) }
        }
    }
    // #endregion

    // #region Account methods
    @Throws(IllegalStateException::class)
    fun addAccount(data: Account.AccountData): CompletableFuture<Account?> {
        check(isAuthenticated) { "User is not authenticated [addAccount]" }

        return handleFuture<Account?>(
            accountRepository.add(data),
            Objects::nonNull, // success if the account is yielded
            "Account added",
            "Failed to create account"
        )
    }

    @Throws(IllegalStateException::class)
    fun editAccount(account: Account, data: Account.AccountData): CompletableFuture<Account?> {
        check(isAuthenticated) { "User is not authenticated [editAccount]" }

        return handleFuture<Account?>(
            accountRepository.edit(account, data),
            Objects::nonNull, // success if the account is yielded
            "Account edited",
            "Failed to edit account"
        )
    }

    @Throws(IllegalStateException::class)
    fun removeAccount(account: Account): CompletableFuture<Boolean?> {
        check(isAuthenticated) { "User is not authenticated [removeAccount]" }

        return handleFuture<Boolean?>(
            accountRepository.remove(account),
            { removed: Boolean? -> removed == true }, // success if true
            "Account deleted",
            "Failed to remove account"
        )
    }

    @Throws(IllegalStateException::class)
    fun getAccountData(account: Account): CompletableFuture<Account.AccountData> {
        check(isAuthenticated) { "User is not authenticated [getAccountData]" }

        return accountRepository.getData(account)
    }

    private fun <T> handleFuture(
        future: CompletableFuture<T?>,
        successCheck: Predicate<T?>,
        successMsg: String,
        failureMsg: String
    ): CompletableFuture<T?> =
        future
            .thenApply { result: T? ->
                val success = successCheck.test(result)
                if (!success) throw RuntimeException(failureMsg)

                hasChanged.set(true)
                Logger.addInfo(successMsg)
                result
            }
            .exceptionally { e: Throwable? ->
                val cause = checkNotNull(e) {
                    "Future completed exceptionally without a cause"
                }
                Logger.addError(cause)
                null
            }
    // #endregion

    // #region UserPreferences methods
    /**
     * Verifies the password and unlocks the accounts.
     *
     * @implNote This method is not thread-safe. Calls must be serialized with password changes.
     *
     * @param masterPassword the master password to authenticate
     * @return `true` if the password is valid and the user was not already authenticated;
     * `false` if already authenticated or the password is invalid
     */
    fun authenticate(masterPassword: String?): Boolean {
        // If already authenticated, no need to re-authenticate
        if (isAuthenticated) return false
        Logger.addInfo("Attempting user authentication...")

        if (!userPreferences.verifyPassword(masterPassword)) return false

        this.masterPassword = masterPassword
        this.isAuthenticated = true
        Logger.addInfo("User authenticated")

        accountRepository.unlockAll(masterPassword)
            .thenAccept { unlocked: Boolean? ->
                if (unlocked == true) Logger.addInfo("All accounts unlocked")
            }
            .exceptionally { e: Throwable ->
                Logger.addError(e)
                null
            }

        return true
    }

    /**
     * Sets or changes the master password. The password is stored before the authenticated flag is
     * published.
     *
     * @implNote This method is not thread-safe. Calls must be serialized with authentication and
     * other password changes.
     *
     * @param newMasterPassword the new master password to set
     * @return `true` if the password was set or changed successfully; `false` if the
     * current password could not be verified
     */
    fun changeMasterPassword(newMasterPassword: String): Boolean {
        require(newMasterPassword.isNotEmpty()) { "New master password cannot be null or empty [changeMasterPassword]" }

        val oldMasterPassword = this.masterPassword // Keep old password reference for later
        // This should never happen, but we check it just to prevent data loss from a potential bug in the authentication logic (better safe than sorry)
        check(isAuthenticated || oldMasterPassword == null) { "User is not authenticated but the master password is set" }

        val res = userPreferences.setPasswordVerified(oldMasterPassword, newMasterPassword)
        if (!res) return false

        this.masterPassword = newMasterPassword
        this.isAuthenticated = true

        Logger.addInfo("Master password %s", if (oldMasterPassword != null) "changed" else "set")
        // Either it was a set or a change, in both cases we need to flag the data as changed to ensure it gets saved
        hasChanged.set(true)

        return true
    }

    fun displayMasterPassword(element: CustomPasswordField) {
        // Use Platform.runLater to queue the update after the field is ready
        // (it also ensures that it gets called on the JavaFX Application Thread)
        Platform.runLater { element.setText(this.masterPassword) }
    }

    // #endregion
    @Synchronized
    @Throws(IOException::class)
    private fun loadDataFile(file: File, fileVarName: String?) {
        Logger.addInfo("Attempting to load $fileVarName...")

        if (!file.exists()) {
            Logger.addInfo("Load failed, file not found: $fileVarName")
            throw FileNotFoundException("File not found: $fileVarName")
        }

        loadingLock.withLock {
            val data: AppData = objectMapper.readValue(file, AppData::class.java)
            userPreferences.set(data.userPreferences)
            accountRepository.setAll(data.accountList)

            isFirstRun = false
            Logger.addInfo("Load OK")
        }
    }

    @Throws(IOException::class)
    private fun saveDataFile(file: File) {
        // Create snapshots to ensure consistency during serialization
        val prefsSnapshot = UserPreferences.empty() // Add a copy method if needed
        val accountSnapshot: MutableList<Account>

        synchronized(userPreferences) {
            prefsSnapshot.set(userPreferences)
        }

        synchronized(accountRepository) {
            accountSnapshot = ArrayList(accountRepository.findAll())
        }

        val data = AppData(prefsSnapshot, accountSnapshot)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    // Wrapper class for application data
    private class AppData(
        @JsonProperty("userPreferences") val userPreferences: UserPreferences,
        @JsonProperty("accountList") val accountList: MutableList<Account>
    ) {
        constructor() : this(UserPreferences.empty(), mutableListOf())
    }

    override fun close() {
        Logger.addInfo("Shutdown requested")

        Logger.addInfo("Shutting down executor services")
        autosaveScheduler.shutdown()
        try {
            val waitForTermination = { autosaveScheduler.awaitTermination(AUTOSAVE_TERMINATION_SEC, TimeUnit.SECONDS) }

            if (!waitForTermination()) {
                Logger.addInfo("Autosave scheduler did not stop in time; interrupting it")
                autosaveScheduler.shutdownNow()
                if (!waitForTermination()) {
                    Logger.addError(IllegalStateException("Autosave scheduler did not terminate"))
                }
            }
        } catch (e: InterruptedException) {
            autosaveScheduler.shutdownNow()
            Thread.currentThread().interrupt()
            Logger.addError(e)
        }

        accountRepository.close()

        // when the user shuts down the program on the first run, it won't save (not authenticated)
        saveData()
    }
}
