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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import lombok.Getter;
import password.manager.app.enums.Exporter;
import password.manager.app.security.Account;
import password.manager.app.security.UserPreferences;
import password.manager.lib.PasswordInputControl;

public final class IOManager {
    public static final String DATA_FILE_NAME = "data.json";
    public static final int AUTOSAVE_INTERVAL = 2; 

    public static final String OS, USER_HOME;
    public static final Path FILE_PATH, DESKTOP_PATH;

    public static final String LANG_BUNDLE_RESOURCE = "/bundles/Lang";

    static {
        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        String WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        String OS_FALLBACK_PATH = ".password-manager";
        FILE_PATH = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : OS_FALLBACK_PATH);
        DESKTOP_PATH = Path.of(USER_HOME, "Desktop");

        Logger.createInstance(FILE_PATH);
        Logger.getInstance().addInfo("os.name: '" + OS + "'");
        Logger.getInstance().addInfo("user.home: '" + USER_HOME + "'");
    }

    private final ObservableList<Account> ACCOUNT_LIST;
    private final UserPreferences USER_PREFERENCES;

    private volatile String masterPassword;
    private volatile @Getter boolean isFirstRun, isAuthenticated;
    
    private final File DATA_FILE;
    private final AtomicBoolean HAS_CHANGED;
    
    private final ObjectMapper OBJECT_MAPPER;
    private final ExecutorService ACCOUNT_EXECUTOR;

    private final ScheduledExecutorService AUTOSAVE_SCHEDULER;

    private IOManager() {
        ACCOUNT_LIST = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
        USER_PREFERENCES = UserPreferences.empty();
        
        masterPassword = null;
        isFirstRun = true;
        isAuthenticated = false;
        
        DATA_FILE = FILE_PATH.resolve(DATA_FILE_NAME).toFile();
        
        HAS_CHANGED = new AtomicBoolean(false);
        final ChangeListener<? super Object> listener = (_, oldValue, newValue) -> {
            if (oldValue != newValue) HAS_CHANGED.set(true);
        };
    
        USER_PREFERENCES.getLocaleProperty().addListener(listener);
        USER_PREFERENCES.getSortingOrderProperty().addListener(listener);

        OBJECT_MAPPER = new ObjectMapper();
        ACCOUNT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

        AUTOSAVE_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
        AUTOSAVE_SCHEDULER.scheduleAtFixedRate(this::saveData, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL, TimeUnit.MINUTES);

        loadData();
    }

    public SortedList<Account> getSortedAccountList() {
        return this.ACCOUNT_LIST.sorted(null);
    }

    public @NotNull UserPreferences getUserPreferences() {
        return this.USER_PREFERENCES;
    }

    // #region Persistence and lifecycle management
    private void loadData() {
        if (FILE_PATH.toFile().mkdirs()) {
            Logger.getInstance().addInfo("Directory '" + FILE_PATH + "' did not exist and was therefore created, skipping data loading");
            return;
        }

        Logger.getInstance().addInfo("Loading data from '" + DATA_FILE + "'...");

        // if the data file exists, it will try to read its contents
        if (!DATA_FILE.exists()) {
            Logger.getInstance().addInfo("File not found");
            return;
        }

        try {
            loadDataFile(DATA_FILE);

            Logger.getInstance().addInfo("Loaded user preferences and " + ACCOUNT_LIST.size() + " accounts");
            isFirstRun = false;
        } catch (IOException e) {
            Logger.getInstance().addError(e);
            Logger.getInstance().addInfo("Data not OK, overwrite?");

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
    }

    private void saveData() {
        if (!isAuthenticated()) {
            Logger.getInstance().addInfo("Not authenticated, skipping save");
            return;
        }

        if (!HAS_CHANGED.get()) {
            Logger.getInstance().addInfo("Nothing to save");
            return;
        }

        // Save asynchronously
        HAS_CHANGED.set(false);
        ACCOUNT_EXECUTOR.submit(() -> {
            Logger.getInstance().addInfo("Saving data...");
            try {
                saveDataFile(DATA_FILE);
                Logger.getInstance().addInfo("Save OK");
            } catch (IOException e) {
                Logger.getInstance().addError(e);
            }
        });
    }

    public void requestShutdown() {
        Logger.getInstance().addInfo("Shutdown requested");
        saveData(); // when the user shuts down the program on the first run, it won't save (not authenticated)
        
        Logger.getInstance().addInfo("Shutting down executor services");
        AUTOSAVE_SCHEDULER.shutdown();
        ACCOUNT_EXECUTOR.shutdown();

        try {
            ACCOUNT_EXECUTOR.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Logger.getInstance().addError(e);
            ACCOUNT_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Logger.getInstance().closeStreams();
    }
    // #endregion
    
    // #region Account methods
    public @NotNull CompletableFuture<Boolean> addAccount(@Nullable String software, @Nullable String username, @Nullable String password) {
        if (software == null || username == null || password == null) {
            Logger.getInstance().addError(new IllegalArgumentException("Software, username, and password cannot be null [addAccount]"));
            return CompletableFuture.completedFuture(false);
        }
        
        if (!isAuthenticated()) {
            Logger.getInstance().addError(new IllegalStateException("User is not authenticated [addAccount]"));
            return CompletableFuture.completedFuture(false); 
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return Account.of(USER_PREFERENCES.getSecurityVersion(), software, username, password, masterPassword);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                }, ACCOUNT_EXECUTOR)
                .thenCompose(account -> {
                    if (account == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    CompletableFuture<Boolean> uiUpdate = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        try {
                            ACCOUNT_LIST.add(account);
                            HAS_CHANGED.set(true);
                            Logger.getInstance().addInfo("Account added");
                            uiUpdate.complete(true);
                        } catch (Exception e) {
                            Logger.getInstance().addError(e);
                            uiUpdate.complete(false);
                        }
                    });
                    return uiUpdate;
                })
                .exceptionally(t -> { 
                    Logger.getInstance().addError(t); 
                    return false;
                });
    }

    public @NotNull CompletableFuture<Boolean> editAccount(@NotNull Account account, @Nullable String software, @Nullable String username, @Nullable String password) {
        if(!ACCOUNT_LIST.contains(account)) {
            Logger.getInstance().addError(new IllegalArgumentException("Account not found in list [editAccount]"));
            return CompletableFuture.completedFuture(false);
        }
        
        if (software == null || username == null || password == null) {
            Logger.getInstance().addError(new IllegalArgumentException("Software, username, and password cannot be null [editAccount]"));
            return CompletableFuture.completedFuture(false);
        }

        if (!isAuthenticated()) {
            Logger.getInstance().addError(new IllegalStateException("User is not authenticated [editAccount]"));
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture
                .supplyAsync(() -> { 
                    try {
                        account.setData(USER_PREFERENCES.getSecurityVersion(), software, username, password, masterPassword);
                        return true;
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return false;
                    }
                }, ACCOUNT_EXECUTOR)
                .thenCompose(success -> {
                    if (!success) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    CompletableFuture<Boolean> uiUpdate = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        // trigger the list change listeners
                        ACCOUNT_LIST.set(ACCOUNT_LIST.indexOf(account), account); 
                        HAS_CHANGED.set(true);
                        Logger.getInstance().addInfo("Account edited");
                        uiUpdate.complete(true);
                    });

                    return uiUpdate;
                })
                .exceptionally(t -> {
                    Logger.getInstance().addError(t); 
                    return false;
                });
    }

    public void removeAccount(@NotNull Account account) {
        if (!isAuthenticated()) {
            Logger.getInstance().addError(new IllegalStateException("User is not authenticated [removeAccount]"));
            return;
        }

        Platform.runLater(() -> {
            if (ACCOUNT_LIST.remove(account)) {
                HAS_CHANGED.set(true);
                Logger.getInstance().addInfo("Account deleted");
            } else {
                Logger.getInstance().addError(new IllegalArgumentException("Account not found in list [removeAccount]"));
            }
        });
    }

    // Asynchronously retrieves and injects the password into the given PasswordInputControl
    public <T extends PasswordInputControl> void getAccountPassword(@NotNull T element, @NotNull Account account) {
        if (!isAuthenticated()) {
            Logger.getInstance().addError(new IllegalStateException("User is not authenticated [getAccountPassword]"));
            return;
        }
        
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return account.getPassword(USER_PREFERENCES.getSecurityVersion(), masterPassword);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                        return null;
                    }
                }, ACCOUNT_EXECUTOR)
                .thenAccept(password -> Platform.runLater(() -> {
                    if (password != null) {
                        element.setText(password);
                    } else {
                        element.setText("");
                        Logger.getInstance().addError(new RuntimeException("Failed to retrieve password for account: " + account.getSoftware()));
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> element.setText(""));
                    Logger.getInstance().addError(throwable);
                    return null;
                });
    }
    // #endregion

    // #region UserPreferences methods
    public @NotNull Boolean changeMasterPassword(String newMasterPassword) {
        final String oldMasterPassword = this.masterPassword;
        if (!(oldMasterPassword == null || isAuthenticated())) return false;
        
        try {
            final boolean res = USER_PREFERENCES.setPasswordVerified(oldMasterPassword, newMasterPassword);
            if (res) HAS_CHANGED.set(true);
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
            return false;
        }

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

    public <T extends PasswordInputControl> void displayMasterPassword(T element) {
        element.setText(masterPassword);
    }

    public @NotNull Boolean authenticate(String masterPassword) {
        if (isAuthenticated()) {
            return false;
        }

        final boolean wasLatestSecurity = USER_PREFERENCES.isLatestVersion();
        try {
            isAuthenticated = USER_PREFERENCES.verifyPassword(masterPassword);
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
        }

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

    public void export(@NotNull Exporter exporter, ObservableResourceFactory langResources) {
        CompletableFuture
                .supplyAsync(() -> {
                    synchronized(ACCOUNT_LIST) {
                        return new ArrayList<>(ACCOUNT_LIST);
                    }
                }, ACCOUNT_EXECUTOR)
                .thenAcceptAsync(snapshot -> {
                    File exportFile = DESKTOP_PATH.resolve("passwords." + exporter.name().toLowerCase()).toFile();
                    try (FileWriter file = new FileWriter(exportFile)) {
                        String out = exporter.getExporter().apply(USER_PREFERENCES.getSecurityVersion(), snapshot, masterPassword);
                        file.write(out);
                        Logger.getInstance().addInfo("Export succeeded: " + exportFile.getName());
                    } catch (Exception e) {
                        Logger.getInstance().addError(e);
                    }
                }, ACCOUNT_EXECUTOR)
                .exceptionally(t -> { 
                    Logger.getInstance().addError(t); 
                    return null;
                });
    }

    private void accountListTaskExec(Consumer<? super Account> action) {
        // MUST wrap iteration in synchronized(...) when using Collections.synchronizedList
        synchronized(ACCOUNT_LIST) {
            ACCOUNT_LIST.forEach(account -> ACCOUNT_EXECUTOR.submit(() -> {
                try {
                    action.accept(account);
                    // If an auto-save is triggered during this process, this ensures the remaining changes are saved anyway
                    HAS_CHANGED.compareAndSet(false, true);
                } catch (Exception e) {
                    Logger.getInstance().addError(e);
                }
            }));
        }
    }

    private void loadDataFile(File file) throws IOException {
        final AppData data = OBJECT_MAPPER.readValue(file, AppData.class);
        USER_PREFERENCES.set(data.userPreferences());
        ACCOUNT_LIST.setAll(data.accountList());
    }

    private void saveDataFile(File file) throws IOException {
        List<Account> snapshot;
        synchronized(ACCOUNT_LIST) {
            snapshot = new ArrayList<>(ACCOUNT_LIST);
        }
        
        AppData data = new AppData(this.USER_PREFERENCES, snapshot);
        OBJECT_MAPPER.writeValue(DATA_FILE, data);
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
