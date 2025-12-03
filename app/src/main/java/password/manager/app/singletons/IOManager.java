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
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import lombok.Getter;
import password.manager.app.App;
import password.manager.app.security.Account;
import password.manager.app.security.AccountRepository;
import password.manager.app.security.UserPreferences;
import password.manager.lib.LoadingAnimation;
import password.manager.lib.PasswordInputControl;

public final class IOManager {
    public static final String OS, USER_HOME;
    public static final Path FILE_PATH, PRESERVED_PATH;
    
    public static final String LANG_BUNDLE_RESOURCE = "/bundles/Lang";

    private static final String DATA_FILE_NAME = "data.json";
    private static final String BACKUP_FILE_NAME = "data.json.bak";
    private static final File DATA_FILE, BACKUP_FILE;

    private static final int AUTOSAVE_INTERVAL = 2; 

    static {
        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        String WINDOWS_PATH = Path.of("AppData", "Local", App.APP_NAME).toString();
        String OS_FALLBACK_PATH = ".password-manager";
        FILE_PATH = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : OS_FALLBACK_PATH);
        PRESERVED_PATH = FILE_PATH.resolve("preserved");

        Logger.createInstance(FILE_PATH);
        Logger.getInstance().addDebug("os.name: '" + OS + "'");
        Logger.getInstance().addDebug("user.home: '" + USER_HOME + "'");

        DATA_FILE = FILE_PATH.resolve(DATA_FILE_NAME).toFile();
        BACKUP_FILE = FILE_PATH.resolve(BACKUP_FILE_NAME).toFile();
        Logger.getInstance().addDebug("DATA_FILE: '" + DATA_FILE + "'");
        Logger.getInstance().addDebug("BACKUP_FILE: '" + BACKUP_FILE + "'");
    }

    private final AccountRepository ACCOUNT_REPOSITORY;
    private final UserPreferences USER_PREFERENCES;

    private String masterPassword;
    private @Getter boolean isFirstRun, isAuthenticated;

    private final AtomicBoolean HAS_CHANGED, IS_LOADING;

    public enum SaveState { SUCCESS, SAVING, ERROR }
    private final SimpleObjectProperty<SaveState> IS_SAVING;

    private final ObjectMapper OBJECT_MAPPER;
    private final ScheduledExecutorService AUTOSAVE_SCHEDULER;

    private IOManager() {
        ACCOUNT_REPOSITORY = new AccountRepository();
        USER_PREFERENCES = UserPreferences.empty();

        masterPassword = null;
        isFirstRun = true; // Assume first run until data is loaded
        isAuthenticated = false;

        HAS_CHANGED = new AtomicBoolean(false);
        IS_LOADING = new AtomicBoolean(false);
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
            if (IS_LOADING.get()) return; // prevents triggering when loading data
            if (oldValue != newValue) HAS_CHANGED.set(true);
        };

        USER_PREFERENCES.getLocaleProperty().addListener(propListener);
        USER_PREFERENCES.getSortingOrderProperty().addListener(propListener);

        final ListChangeListener<Account> listListener = change -> {
            if (IS_LOADING.get()) return; // prevents triggering when loading data
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                    HAS_CHANGED.set(true);
                    return; // no need to check further changes
                }
            }
        };

        getAccountList().addListener(listListener);

        USER_PREFERENCES.getLocaleProperty().addListener((_, _, newValue) ->
            Logger.getInstance().addInfo("Changed locale to: " + newValue.getDisplayLanguage(Locale.ENGLISH))
        );
        USER_PREFERENCES.getSortingOrderProperty().addListener((_, _, newValue) ->
            Logger.getInstance().addInfo("Changed sorting order to: " + newValue)
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

        // Save in background virtual thread
        Thread.startVirtualThread(() -> {
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
        });
    }

    public void requestShutdown() {
        Logger.getInstance().addInfo("Shutdown requested");
        saveData(); // when the user shuts down the program on the first run, it won't save (not authenticated)

        Logger.getInstance().addInfo("Shutting down executor services");
        ACCOUNT_REPOSITORY.shutdown();
        AUTOSAVE_SCHEDULER.shutdown();

        Logger.getInstance().closeStreams();
    }
    // #endregion

    // #region Account methods
    public @NotNull CompletableFuture<Void> addAccount(@NotNull String software, @NotNull String username, @NotNull String password) throws IllegalStateException {
        if (!isAuthenticated()) throw new IllegalStateException("User is not authenticated [addAccount]");

        return ACCOUNT_REPOSITORY.add(USER_PREFERENCES.getSecurityVersion(), masterPassword, software, username, password)
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

        return ACCOUNT_REPOSITORY.edit(USER_PREFERENCES.getSecurityVersion(), masterPassword, account, software, username, password)
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
        return ACCOUNT_REPOSITORY.getPassword(USER_PREFERENCES.getSecurityVersion(), masterPassword,  account)
                .thenAccept(password -> {
                    LoadingAnimation.stop(element);
                    if (password == null) throw new RuntimeException("Failed to retrieve password for account: " + account.getSoftware());
                    Platform.runLater(() -> element.setText(password));
                });
    }

    private void accountListTaskExec(Consumer<? super Account> action) {
        // MUST wrap iteration in synchronized(...) when using Collections.synchronizedList
        ACCOUNT_REPOSITORY.executeOnAll(account -> {
            try {
                action.accept(account);
                // If an auto-save is triggered during this process, this ensures the remaining changes are saved anyway
                HAS_CHANGED.compareAndSet(false, true);
            } catch (Exception e) {
                Logger.getInstance().addError(e);
            }
        });
    }
    // #endregion

    // #region UserPreferences methods
    public @NotNull Boolean changeMasterPassword(String newMasterPassword) {
        final String oldMasterPassword = this.masterPassword;
        if (!(oldMasterPassword == null || isAuthenticated())) return false;

        final boolean res = USER_PREFERENCES.setPasswordVerified(oldMasterPassword, newMasterPassword);
        if (!res) return false;

        HAS_CHANGED.set(true);

        this.masterPassword = newMasterPassword;
        if (oldMasterPassword != null) {
            Logger.getInstance().addInfo("Master password changed");

            accountListTaskExec(account -> {
                try {
                    account.changeMasterPassword(USER_PREFERENCES.getSecurityVersion(), oldMasterPassword, newMasterPassword);
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                }
            });
        } else {
            Logger.getInstance().addInfo("Master password set");
            isAuthenticated = true;
        }

        return true;
    }

    public void displayMasterPassword(PasswordInputControl element) {
        element.setText(masterPassword);
    }

    public @NotNull Boolean authenticate(String masterPassword) {
        if (isAuthenticated()) {
            return false;
        }

        final boolean wasLatestSecurity = USER_PREFERENCES.isLatestVersion();
        isAuthenticated = USER_PREFERENCES.verifyPassword(masterPassword);

        if (isAuthenticated) {
            this.masterPassword = masterPassword;
            Logger.getInstance().addInfo("User authenticated");

            if(!wasLatestSecurity) {
                accountListTaskExec(account -> {
                    try {
                        account.updateToLatestVersion(USER_PREFERENCES.getSecurityVersion(), masterPassword);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                    }
                });
                Logger.getInstance().addInfo("Updated to latest security version");
            }
        }

        return isAuthenticated;
    }
    // #endregion

    private synchronized void loadDataFile(File file, String fileVarName) throws IOException {
        Logger.getInstance().addInfo("Attempting to load " + fileVarName + "...");

        if (!file.exists()) throw new FileNotFoundException("File not found: " + file.getAbsolutePath());

        IS_LOADING.set(true);

        AppData data;
        try {
            data = OBJECT_MAPPER.readValue(file, AppData.class);
            USER_PREFERENCES.set(data.userPreferences());
            ACCOUNT_REPOSITORY.setAll(data.accountList());
            isFirstRun = false;
            Logger.getInstance().addInfo(" => OK");
        } catch (IOException e) {
            Logger.getInstance().addInfo(" => Failed");
            throw e;
        } finally {
            IS_LOADING.set(false);
        }
    }

    private void saveDataFile(File file) throws IOException {
        // Create snapshots to ensure consistency during serialization
        UserPreferences prefsSnapshot = new UserPreferences(); // Add a copy method if needed
        List<Account> accountSnapshot = new ArrayList<>();

        synchronized(USER_PREFERENCES) {
            prefsSnapshot.set(USER_PREFERENCES);
        }

        synchronized(ACCOUNT_REPOSITORY) {
            accountSnapshot.addAll(ACCOUNT_REPOSITORY.findAll());
        }

        AppData data = new AppData(prefsSnapshot, accountSnapshot);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE, data);
    }

    // Wrapper class for application data
    private record AppData(UserPreferences userPreferences, List<Account> accountList) {}

    // #region Singleton methods
    public static synchronized void createInstance() throws IllegalStateException {
        ObservableResourceFactory.createInstance(LANG_BUNDLE_RESOURCE);
        Singletons.register(IOManager.class, new IOManager());
    }

    public static IOManager getInstance() throws IllegalStateException {
        return Singletons.get(IOManager.class);
    }
    // #endregion
}
