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

package password.manager.app.singletons;

import static password.manager.app.Utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;

import tools.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import lombok.Getter;
import password.manager.app.security.Account;
import password.manager.app.security.AccountRepository;
import password.manager.app.security.UserPreferences;
import password.manager.lib.LoadingAnimation;
import password.manager.lib.PasswordInputControl;


public final class IOManager extends Singleton {

    private static final String DATA_FILE_NAME = "data.json";
    private static final String BACKUP_FILE_NAME = "data.json.bak";

    private static final File DATA_FILE, BACKUP_FILE;
    private static final Path PRESERVED_PATH;

    private static final int AUTOSAVE_INTERVAL = 2;

    static {
        Path basePath = AppConfig.getInstance().getBasePath();
        DATA_FILE = basePath.resolve(DATA_FILE_NAME).toFile();
        BACKUP_FILE = basePath.resolve(BACKUP_FILE_NAME).toFile();
        PRESERVED_PATH = basePath.resolve("preserved");
    }

    private final UserPreferences USER_PREFERENCES;
    private final AccountRepository ACCOUNT_REPOSITORY;

    private final StringProperty MASTER_PASSWORD_PROPERTY;
    private @Getter boolean isFirstRun, isAuthenticated;

    private final AtomicBoolean HAS_CHANGED;
    private final Lock LOADING_LOCK;

    public enum SaveState { SUCCESS, SAVING, ERROR }
    private final SimpleObjectProperty<SaveState> IS_SAVING;

    private final ObjectMapper OBJECT_MAPPER;
    private final ScheduledExecutorService AUTOSAVE_SCHEDULER;

    // Let only package classes instantiate this
    IOManager() {
        Logger.getInstance().addDebug("DATA_FILE: '" + DATA_FILE + "'");
        Logger.getInstance().addDebug("BACKUP_FILE: '" + BACKUP_FILE + "'");
        Logger.getInstance().addDebug("PRESERVED_PATH: '" + PRESERVED_PATH + "'");

        MASTER_PASSWORD_PROPERTY = new SimpleStringProperty(null);

        USER_PREFERENCES = UserPreferences.empty();
        ACCOUNT_REPOSITORY = new AccountRepository(USER_PREFERENCES.securityVersionProperty(), MASTER_PASSWORD_PROPERTY);

        isFirstRun = true; // Assume first run until data is loaded
        isAuthenticated = false;

        HAS_CHANGED = new AtomicBoolean(false);
        LOADING_LOCK = new ReentrantLock();

        IS_SAVING = new SimpleObjectProperty<>(SaveState.SUCCESS);
        setupListeners();

        OBJECT_MAPPER = new ObjectMapper();
        AUTOSAVE_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
        AUTOSAVE_SCHEDULER.scheduleAtFixedRate(this::saveData, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL, TimeUnit.MINUTES);

        loadData();
    }

    public ObservableList<Account> getAccountList() {
        return this.ACCOUNT_REPOSITORY.findAll();
    }

    public @NotNull UserPreferences getUserPreferences() {
        return this.USER_PREFERENCES;
    }

    /**
     * Returns a read-only property that indicates the current save state.
     * You can listen to this property to show/hide save status UI.
     * @return {@link ReadOnlyObjectProperty} of the current {@link SaveState}
     */
    public ReadOnlyObjectProperty<SaveState> savingProperty() {
        return this.IS_SAVING;
    }

    // #region Persistence and lifecycle management
    private void setupListeners() {
        final ChangeListener<? super Object> propListener = (_, oldValue, newValue) -> {
            if (!LOADING_LOCK.tryLock()) return; // prevents triggering when loading data
            HAS_CHANGED.set(true);
            LOADING_LOCK.unlock();
        };

        USER_PREFERENCES.localeProperty().addListener(propListener);
        USER_PREFERENCES.sortingOrderProperty().addListener(propListener);

        final ListChangeListener<Account> listListener = change -> {
            if (!LOADING_LOCK.tryLock()) return;
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    HAS_CHANGED.set(true);
                    break; // no need to check further changes
                }
            }
            LOADING_LOCK.unlock();
        };

        getAccountList().addListener(listListener);

        USER_PREFERENCES.localeProperty().addListener((_, _, newValue) ->
            Logger.getInstance().addDebug("Changed locale to: " + newValue.getLocale().getDisplayLanguage(Locale.ENGLISH))
        );
        USER_PREFERENCES.sortingOrderProperty().addListener((_, _, newValue) ->
            Logger.getInstance().addDebug("Changed sorting order to: " + newValue)
        );
    }

    private void loadData() {
        if (!(DATA_FILE.exists() || BACKUP_FILE.exists())) {
            Logger.getInstance().addInfo("Neither DATA_FILE nor BACKUP_FILE exists, skipping data loading");
            return;
        }

        // Try to load DATA_FILE
        try {
            loadDataFile(DATA_FILE, "DATA_FILE");

            Files.copy(DATA_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Logger.getInstance().addInfo("Copied valid DATA_FILE to BACKUP_FILE");

            return;
        } catch (IOException e) {
            Logger.getInstance().addError(e);
        }

        // Try to load BACKUP_FILE
        try {
            loadDataFile(BACKUP_FILE, "BACKUP_FILE");

            // Move corrupted DATA_FILE to preserved with timestamp, but only if it exists
            if(DATA_FILE.exists()) {
                final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                final String timestamp = DTF.format(LocalDateTime.now());

                PRESERVED_PATH.toFile().mkdirs();
                Files.move(DATA_FILE.toPath(), PRESERVED_PATH.resolve(timestamp + ".json"), StandardCopyOption.REPLACE_EXISTING);
                Logger.getInstance().addInfo("Moved corrupted DATA_FILE to 'preserved/" + timestamp + ".json'");
            }

            HAS_CHANGED.set(true); // Force save to recreate DATA_FILE from backup
            return;
        } catch (IOException e) {
            Logger.getInstance().addError(e);
        }

        // If both main and backup failed, ask user to overwrite
        Logger.getInstance().addInfo("Asking user to overwrite data");
        final String errMsg = ObservableResourceFactory.getInstance().getValue("data_error");
        final Alert alert = new Alert(AlertType.ERROR, errMsg, ButtonType.YES, ButtonType.NO);
        setDefaultButton(alert, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            Logger.getInstance().addInfo("Data overwriting accepted");
        } else {
            Logger.getInstance().addInfo("Data overwriting denied");
            System.exit(0);
        }
    }

    private void saveData() {
        if (!isAuthenticated()) {
            Logger.getInstance().addInfo("Skipping save: Not authenticated");
            return;
        }

        if (!HAS_CHANGED.get()) {
            Logger.getInstance().addInfo("Skipping save: No changes");
            return;
        }

        Logger.getInstance().addInfo("Saving data...");
        Platform.runLater(() -> IS_SAVING.set(SaveState.SAVING));

        // Change preemptively to avoid losing if changes are added during save
        HAS_CHANGED.set(false);

        try {
            saveDataFile(DATA_FILE);
            Logger.getInstance().addInfo("Save OK");
            Platform.runLater(() -> IS_SAVING.set(SaveState.SUCCESS));
        } catch (IOException e) {
            Logger.getInstance().addError(e);
            Platform.runLater(() -> IS_SAVING.set(SaveState.ERROR));
        }
    }
    // #endregion

    // #region Account methods
    public @NotNull CompletableFuture<Void> addAccount(@NotNull String software, @NotNull String username, @NotNull String password) throws IllegalStateException {
        if (!isAuthenticated()) throw new IllegalStateException("User is not authenticated [addAccount]");

        return ACCOUNT_REPOSITORY.add(software, username, password)
                .thenCompose(account -> {
                    if (account == null) throw new RuntimeException("Failed to create account");
                    final CompletableFuture<Void> uiUpdate = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        try {
                            HAS_CHANGED.set(true);
                            Logger.getInstance().addInfo("Account added");
                            uiUpdate.complete(null);
                        } catch (Exception e) {
                            Logger.getInstance().addError(e);
                            uiUpdate.completeExceptionally(e);
                        }
                    });
                    return uiUpdate;
                });
    }

    public @NotNull CompletableFuture<Void> editAccount(@NotNull Account account, @NotNull String software, @NotNull String username, @NotNull String password) throws IllegalStateException  {
        if (!isAuthenticated()) throw new IllegalStateException("User is not authenticated [editAccount]");

        return ACCOUNT_REPOSITORY.edit(account, software, username, password)
                .thenCompose(editedAcc -> {
                    if(editedAcc == null) throw new RuntimeException("Failed to edit account");
                    final CompletableFuture<Void> uiUpdate = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        HAS_CHANGED.set(true);
                        Logger.getInstance().addInfo("Account edited");
                        uiUpdate.complete(null);
                    });
                    return uiUpdate;
                });
    }

    public @NotNull CompletableFuture<Void> removeAccount(@NotNull Account account) throws IllegalStateException {
        if (!isAuthenticated()) throw new IllegalStateException("User is not authenticated [removeAccount]");

        return ACCOUNT_REPOSITORY.remove(account)
                .thenAccept(removed -> {
                    if (removed) {
                        HAS_CHANGED.set(true);
                        Logger.getInstance().addInfo("Account deleted");
                    }
                });
    }

    // Asynchronously retrieves and injects the password into the given PasswordInputControl
    public <T extends PasswordInputControl> @NotNull CompletableFuture<Void> getAccountPassword(@NotNull T element, @NotNull Account account) {
        if (!isAuthenticated()) throw new IllegalStateException("User is not authenticated [getAccountPassword]");

        LoadingAnimation.start(element);
        return ACCOUNT_REPOSITORY.getPassword(account)
                .thenAccept(password -> {
                    LoadingAnimation.stop(element);
                    if (password == null) throw new RuntimeException("Failed to retrieve password for account: " + account.getSoftware());
                    Platform.runLater(() -> element.setText(password));
                });
    }
    // #endregion

    // #region UserPreferences methods
    public @NotNull Boolean changeMasterPassword(String newMasterPassword) {
        String oldMasterPassword = this.MASTER_PASSWORD_PROPERTY.get();
        if (!(oldMasterPassword == null || isAuthenticated())) return false;

        boolean res = USER_PREFERENCES.setPasswordVerified(oldMasterPassword, newMasterPassword);
        if (!res) return false;

        this.MASTER_PASSWORD_PROPERTY.set(newMasterPassword);

        if (oldMasterPassword != null) {
            Logger.getInstance().addInfo("Master password changed");
            HAS_CHANGED.set(true);
        } else {
            Logger.getInstance().addInfo("Master password set");
            isAuthenticated = true;
        }

        return true;
    }

    public void displayMasterPassword(PasswordInputControl element) {
        // Use Platform.runLater to queue the update after the field is ready
        // (it also ensures that it gets called on the JavaFX Application Thread)
        Platform.runLater(() -> element.setText(MASTER_PASSWORD_PROPERTY.get()));
    }

    public @NotNull Boolean authenticate(String masterPassword) {
        // If already authenticated, no need to re-authenticate
        if (isAuthenticated()) return false;
        Logger.getInstance().addInfo("Attempting user authentication...");

        isAuthenticated = USER_PREFERENCES.verifyPassword(masterPassword);
        if (!isAuthenticated) return false;

        this.MASTER_PASSWORD_PROPERTY.set(masterPassword);
        Logger.getInstance().addInfo("User authenticated");
        return true;
    }
    // #endregion

    private synchronized void loadDataFile(File file, String fileVarName) throws IOException {
        Logger.getInstance().addInfo("Attempting to load " + fileVarName + "...");

        if (!file.exists()) throw new FileNotFoundException("File not found: " + file.getAbsolutePath());

        LOADING_LOCK.lock();

        AppData data;
        try {
            data = OBJECT_MAPPER.readValue(file, AppData.class);
            USER_PREFERENCES.set(data.userPreferences());
            ACCOUNT_REPOSITORY.setAll(data.accountList());
            isFirstRun = false;
            Logger.getInstance().addInfo("Load OK");
        } finally {
            LOADING_LOCK.unlock();
        }
    }

    private void saveDataFile(File file) throws IOException {
        // Create snapshots to ensure consistency during serialization
        UserPreferences prefsSnapshot = new UserPreferences(); // Add a copy method if needed
        List<Account> accountSnapshot;

        synchronized(USER_PREFERENCES) {
            prefsSnapshot.set(USER_PREFERENCES);
        }

        synchronized(ACCOUNT_REPOSITORY) {
            accountSnapshot = new ArrayList<>(ACCOUNT_REPOSITORY.findAll());
        }

        AppData data = new AppData(prefsSnapshot, accountSnapshot);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, data);
    }

    // Wrapper class for application data
    private record AppData(UserPreferences userPreferences, List<Account> accountList) {}

    // #region Singleton methods
    @Override
    public void close() {
        Logger.getInstance().addInfo("Shutdown requested");

        Logger.getInstance().addInfo("Shutting down executor services");
        AUTOSAVE_SCHEDULER.shutdown();
        ACCOUNT_REPOSITORY.close();

        // when the user shuts down the program on the first run, it won't save (not authenticated)
        saveData();
    }

    public static IOManager getInstance() {
        return Singletons.get(IOManager.class);
    }
    // #endregion
}
