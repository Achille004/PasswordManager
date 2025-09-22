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
import com.fasterxml.jackson.databind.ObjectWriter;

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
import password.manager.lib.ReadablePasswordFieldWithStr;

public final class IOManager {
    private static final String DATA_FILE_NAME = "data.json";
    private static final int AUTOSAVE_TIMER_MINUTES = 2; 

    private static final String OS, USER_HOME;
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
    private @Getter UserPreferences userPreferences;

    private volatile String masterPassword;
    private volatile @Getter boolean isFirstRun, isAuthenticated;
    
    private final File DATA_FILE;
    private final AtomicBoolean HAS_CHANGED;
    
    private final ScheduledExecutorService FLUSH_SCHEDULER;
    private final ObjectWriter OBJECT_WRITER;
    private final ExecutorService ACCOUNT_EXECUTOR;

    private static IOManager instance = null;
    private static final String CLASS_NAME = IOManager.class.getName();

    /**
     * Creates the singleton IOManager.
     */
    public static synchronized void createInstance() throws IllegalStateException {
        if (instance != null) {
            throw new IllegalStateException(CLASS_NAME + " instance already created");
        }
        ObservableResourceFactory.createInstance(LANG_BUNDLE_RESOURCE);
        instance = new IOManager();
    }

    public static IOManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(CLASS_NAME + " instance not created yet");
        }
        return instance;
    }

    private IOManager() {
        ACCOUNT_LIST = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
        userPreferences = UserPreferences.empty();
        setupPreferencesListeners();

        masterPassword = null;
        isFirstRun = true;
        isAuthenticated = false;

        DATA_FILE = FILE_PATH.resolve(DATA_FILE_NAME).toFile();
        HAS_CHANGED = new AtomicBoolean(false);

        OBJECT_WRITER = new ObjectMapper().writer().withDefaultPrettyPrinter();
        ACCOUNT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

        FLUSH_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
        FLUSH_SCHEDULER.scheduleAtFixedRate(() -> saveDataFile(false),
                AUTOSAVE_TIMER_MINUTES, AUTOSAVE_TIMER_MINUTES, TimeUnit.MINUTES);

        loadData();
    }

    private void setupPreferencesListeners() {
        ChangeListener<? super Object> listener = (_, oldValue, newValue) -> {
            if (oldValue != newValue) {
                HAS_CHANGED.set(true);
            }
        };
        
        userPreferences.getLocaleProperty().addListener(listener);
        userPreferences.getSortingOrderProperty().addListener(listener);
    }

    private void loadData() {
        if (!userPreferences.isEmpty()) {
            Logger.getInstance().addError(new UnsupportedOperationException("Cannot read data file: it would overwrite non-empty user preferences"));
            return;
        }

        if (FILE_PATH.toFile().mkdirs()) {
            Logger.getInstance().addInfo("Directory '" + FILE_PATH + "' did not exist and was therefore created, skipping data loading");
            return;
        }

        Logger.getInstance().addInfo("Loading data (" + DATA_FILE + ")...");

        // if the data file exists, it will try to read its contents
        if (!DATA_FILE.exists()) {
            Logger.getInstance().addInfo("File not found");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AppData data = objectMapper.readValue(DATA_FILE, AppData.class);

            this.userPreferences = data.userPreferences();
            setupPreferencesListeners();

            ACCOUNT_LIST.addAll(Collections.nCopies(data.accountList().size(), null));
            FXCollections.copy(this.ACCOUNT_LIST, data.accountList());

            Logger.getInstance().addInfo("Data OK");
            isFirstRun = false;
        } catch (IOException e) {
            Logger.getInstance().addError(e);
            Logger.getInstance().addInfo("Data not OK, overwrite?");

            Alert alert = new Alert(AlertType.ERROR, ObservableResourceFactory.getInstance().getValue("data_error"), ButtonType.YES, ButtonType.NO);
            setDefaultButton(alert, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                Logger.getInstance().addInfo("Data overwriting accepted");
            } else {
                Logger.getInstance().addInfo("Data overwriting denied");
                System.exit(0);
            }
        }

        // TODO Add logging
        // userPreferences.getLocaleProperty().addListener((_, _, newValue) -> Logger.getInstance().addInfo("Changed locale to: " + newValue));
        // userPreferences.getSortingOrderProperty().addListener((_, _, newValue) -> Logger.getInstance().addInfo("Changed sorting order to: " + newValue));
    }

    public SortedList<Account> getSortedAccountList() {
        return this.ACCOUNT_LIST.sorted(null);
    }

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
                        return Account.of(software, username, password, masterPassword);
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
                        account.setData(software, username, password, masterPassword);
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

    public <T extends PasswordInputControl> void getAccountPassword(@NotNull T element, @NotNull Account account) {
        if (!isAuthenticated()) {
            Logger.getInstance().addError(new IllegalStateException("User is not authenticated [getAccountPassword]"));
            return;
        }
        
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return account.getPassword(masterPassword);
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
        String oldMasterPassword = this.masterPassword;
        if (oldMasterPassword != null && !isAuthenticated()) {
            return false;
        }
        
        try {
            userPreferences.setPasswordVerified(oldMasterPassword, newMasterPassword);
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
            return false;
        }

        this.masterPassword = newMasterPassword;
        if (oldMasterPassword != null) {
            Logger.getInstance().addInfo("Master password changed");

            accountListTaskExec(account -> {
                try {
                    account.changeMasterPassword(oldMasterPassword, newMasterPassword);
                } catch (GeneralSecurityException e) {
                    Logger.getInstance().addError(e);
                }
            });
            
            HAS_CHANGED.set(true);
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

        boolean isLatestSecurity = userPreferences.isLatestVersion();
        try {
            isAuthenticated = userPreferences.verifyPassword(masterPassword);
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
        }

        if (isAuthenticated) {
            this.masterPassword = masterPassword;
            Logger.getInstance().addInfo("User authenticated");

            if(!isLatestSecurity) {
                accountListTaskExec(account -> {
                    try {
                        account.updateToLatestVersion(masterPassword);
                    } catch (GeneralSecurityException e) {
                        Logger.getInstance().addError(e);
                    }
                });
                HAS_CHANGED.set(true);
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
                        String out = exporter.getExporter().apply(snapshot, masterPassword);
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
                } catch (Exception e) {
                    Logger.getInstance().addError(e);
                }
            }));
        }
        HAS_CHANGED.set(true);
    }

    private void saveDataFile(boolean shutdown) {
        if (isAuthenticated()) {
            if (HAS_CHANGED.compareAndSet(true, false)) {
                // Save asynchronously
                ACCOUNT_EXECUTOR.submit(() -> {
                    List<Account> snapshot;
                    synchronized(ACCOUNT_LIST) {
                        snapshot = new ArrayList<>(ACCOUNT_LIST);
                    }
                    
                    Logger.getInstance().addInfo("Saving data...");
                    try {
                        AppData data = new AppData(this.userPreferences, snapshot);
                        OBJECT_WRITER.writeValue(DATA_FILE, data);
                        Logger.getInstance().addInfo("Save OK");
                    } catch (IOException e) {
                        Logger.getInstance().addError(e);
                    }
                });
            } else {
                Logger.getInstance().addInfo("Nothing to save");
            }
        } else {
            Logger.getInstance().addInfo("Not authenticated, skipping save");
        }
            
        if(shutdown) {
            Logger.getInstance().addInfo("Shutting down executor services");
            FLUSH_SCHEDULER.shutdown();
            ACCOUNT_EXECUTOR.shutdown();
            try {
                ACCOUNT_EXECUTOR.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Logger.getInstance().addError(e);
                ACCOUNT_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Wrapper class for application data
    private record AppData(UserPreferences userPreferences, List<Account> accountList) {}

    public void saveAll() {
        // when the user shuts down the program on the first run, it won't save
        Logger.getInstance().addInfo("Shutdown requested");
        saveDataFile(true);
        Logger.getInstance().closeStreams();
    }
}
