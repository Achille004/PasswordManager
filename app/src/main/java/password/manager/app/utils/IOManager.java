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

package password.manager.app.utils;

import static password.manager.app.utils.Utils.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import password.manager.lib.ReadablePasswordField;

public final class IOManager {
    private static final String DATA_FILE_NAME = "data.json";
    private static final int AUTOSAVE_TIMER_MINUTES = 2; 

    private static final String OS, USER_HOME;
    public static final Path FILE_PATH, DESKTOP_PATH;

    static {
        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        String WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        String OS_FALLBACK_PATH = ".password-manager";
        FILE_PATH = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : OS_FALLBACK_PATH);
        DESKTOP_PATH = Path.of(USER_HOME, "Desktop");
    }

    private final @Getter Logger logger;
    private final ObservableList<Account> accountList;
    private @Getter UserPreferences userPreferences;

    private String masterPassword;
    private @Getter boolean isFirstRun, isAuthenticated;
    
    private File dataFile;
    private AtomicBoolean hasChanged = new AtomicBoolean(false);
    
    private final ScheduledExecutorService FLUSH_SCHEDULER;
    private final ObjectWriter OBJECT_WRITER;
    private final ExecutorService ACCOUNT_EXECUTOR;

    public IOManager() {
        logger = new Logger(FILE_PATH);
        accountList = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
        userPreferences = UserPreferences.empty();
        setupPreferencesListeners();

        masterPassword = null;
        dataFile = FILE_PATH.resolve(DATA_FILE_NAME).toFile();
        isFirstRun = true;
        isAuthenticated = false;

        OBJECT_WRITER = new ObjectMapper().writer().withDefaultPrettyPrinter();
        ACCOUNT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

        FLUSH_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
        FLUSH_SCHEDULER.scheduleAtFixedRate(() -> saveDataFile(false),
                AUTOSAVE_TIMER_MINUTES, AUTOSAVE_TIMER_MINUTES, TimeUnit.MINUTES);
    }

    private void setupPreferencesListeners() {
        ChangeListener<? super Object> listener = (_, oldValue, newValue) -> {
            if (oldValue != newValue) {
                hasChanged.set(true);
            }
        };
        
        userPreferences.getLocaleProperty().addListener(listener);
        userPreferences.getSortingOrderProperty().addListener(listener);
    }

    public void loadData(final ObservableResourceFactory langResources) {
        if (!userPreferences.isEmpty()) {
            logger.addError(new UnsupportedOperationException("Cannot read data file: it would overwrite non-empty user preferences"));
            return;
        }

        logger.addInfo("os.name: '" + OS + "'");
        logger.addInfo("user.home: '" + USER_HOME + "'");

        if (FILE_PATH.toFile().mkdirs()) {
            logger.addInfo("Directory '" + FILE_PATH + "' did not exist and was therefore created, skipping data loading");
            return;
        }

        logger.addInfo("Loading data (" + dataFile + ")...");

        // if the data file exists, it will try to read its contents
        if (!dataFile.exists()) {
            logger.addInfo("File not found");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AppData data = objectMapper.readValue(dataFile, AppData.class);

            this.userPreferences = data.userPreferences();
            setupPreferencesListeners();

            accountList.addAll(Collections.nCopies(data.accountList().size(), null));
            FXCollections.copy(this.accountList, data.accountList());

            logger.addInfo("Data OK");
            isFirstRun = false;
        } catch (IOException e) {
            logger.addError(e);
            logger.addInfo("Data not OK, overwrite?");

            Alert alert = new Alert(AlertType.ERROR, langResources.getValue("data_error"), ButtonType.YES, ButtonType.NO);
            setDefaultButton(alert, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                logger.addInfo("Data overwriting accepted");
            } else {
                logger.addInfo("Data overwriting denied");
                System.exit(0);
            }
        }

        // TODO Add logging
        // userPreferences.getLocaleProperty().addListener((_, _, newValue) -> logger.addInfo("Changed locale to: " + newValue));
        // userPreferences.getSortingOrderProperty().addListener((_, _, newValue) -> logger.addInfo("Changed sorting order to: " + newValue));
    }

    public SortedList<Account> getSortedAccountList() {
        return this.accountList.sorted(null);
    }

    // #region Account methods
    public @NotNull Boolean addAccount(@Nullable String software, @Nullable String username, @Nullable String password) {
        if (software == null || username == null || password == null) {
            return false;
        }
        
        if (!isAuthenticated()) {
            return false; 
        }

        boolean[] ok = {true};
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return Account.of(software, username, password, masterPassword);
                    } catch (GeneralSecurityException e) {
                        logger.addError(e);
                        ok[0] = false;
                        return null;
                    }
                }, ACCOUNT_EXECUTOR)
                .thenAccept(account -> Platform.runLater(() -> {
                    accountList.add(account);
                    hasChanged.set(true);
                    logger.addInfo("Account added");
                }))
                .exceptionally(t -> { 
                    logger.addError(t); 
                    ok[0] = false;
                    return null;
                });

        return ok[0];
    }

    public @NotNull Boolean editAccount(@NotNull Account account, @Nullable String software, @Nullable String username, @Nullable String password) {
        if (software == null || username == null || password == null) {
            return false;
        }

        int idx = accountList.indexOf(account);
        if (!isAuthenticated() || idx < 0) {
            return false;
        }

        boolean[] ok = {true};
        CompletableFuture
                .runAsync(() -> { 
                    try {
                        account.setData(software, username, password, masterPassword);
                    } catch (GeneralSecurityException e) {
                        logger.addError(e);
                        ok[0] = false;
                    }
                }, ACCOUNT_EXECUTOR)
                .thenRun(() -> Platform.runLater(() -> {
                    // trigger SortedList refresh
                    accountList.set(idx, account);   
                    hasChanged.set(true);
                    logger.addInfo("Account edited");
                }))
                .exceptionally(t -> {
                    logger.addError(t); 
                    ok[0] = false;
                    return null;
                });

        return ok[0];
    }

    public @NotNull Boolean removeAccount(Account account) {
        if (!isAuthenticated()) {
            return false;
        }

        boolean[] ok = {true};
        CompletableFuture
                .runAsync(() -> { /* nothing to do off-thread */ }, ACCOUNT_EXECUTOR)
                .thenRun(() -> Platform.runLater(() -> {
                    if (accountList.remove(account)) {
                        hasChanged.set(true);
                        logger.addInfo("Account deleted");
                    }
                }))
                .exceptionally(t -> { 
                    logger.addError(t); 
                    ok[0] = false;
                    return null;
                });

        return ok[0];
    }

    public @Nullable String getAccountPassword(@NotNull Account account) {
        try {
            if (isAuthenticated()) {
                return account.getPassword(masterPassword);
            }
        } catch (GeneralSecurityException e) {
            logger.addError(e);
        }

        return null;
    }
    // #endregion

    // #region LoginAccount methods
    public @NotNull Boolean changeMasterPassword(String newMasterPassword) {
        String oldMasterPassword = this.masterPassword;
        if (oldMasterPassword != null && !isAuthenticated()) {
            return false;
        }
        
        try {
            userPreferences.setPasswordVerified(oldMasterPassword, newMasterPassword);
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
            return false;
        }

        this.masterPassword = newMasterPassword;
        if (oldMasterPassword != null) {
            logger.addInfo("Master password changed");

            accountListTaskExec(account -> {
                try {
                    account.changeMasterPassword(oldMasterPassword, newMasterPassword);
                } catch (GeneralSecurityException e) {
                    logger.addError(e);
                }
            });
            
            hasChanged.set(true);
        } else {
            logger.addInfo("Master password set");
            isAuthenticated = true;
        }

        return true;
    }

    public void displayMasterPassword(ReadablePasswordField element) {
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
            logger.addError(e);
        }

        if (isAuthenticated) {
            this.masterPassword = masterPassword;
            logger.addInfo("User authenticated");

            if(!isLatestSecurity) {
                accountListTaskExec(account -> {
                    try {
                        account.updateToLatestVersion(masterPassword);
                    } catch (GeneralSecurityException e) {
                        logger.addError(e);
                    }
                });
                hasChanged.set(true);
                logger.addInfo("Updated to latest security version");
            }
        }

        return isAuthenticated;
    }

    private void accountListTaskExec(Consumer<? super Account> action) {
        // MUST wrap iteration in synchronized(...) when using Collections.synchronizedList
        synchronized(accountList) {
            accountList.forEach(account -> ACCOUNT_EXECUTOR.submit(() -> {
                try {
                    action.accept(account);
                } catch (Exception e) {
                    logger.addError(e);
                }
            }));
        }
        hasChanged.set(true);
    }
    // #endregion

    public void export(@NotNull Exporter exporter, ObservableResourceFactory langResources) {
        CompletableFuture
                .supplyAsync(() -> {
                    synchronized(accountList) {
                        return new ArrayList<>(accountList);
                    }
                }, ACCOUNT_EXECUTOR)
                .thenAcceptAsync(snapshot -> {
                    File exportFile = DESKTOP_PATH.resolve("passwords." + exporter.name().toLowerCase()).toFile();
                    try (FileWriter file = new FileWriter(exportFile)) {
                        String out = exporter.getExporter().apply(snapshot, langResources, masterPassword);
                        file.write(out);
                        logger.addInfo("Export succeeded: " + exportFile.getName());
                    } catch (Exception e) {
                        logger.addError(e);
                    }
                }, ACCOUNT_EXECUTOR)
                .exceptionally(t -> { 
                    logger.addError(t); 
                    return null;
                });
    }

    private void saveDataFile(boolean shutdown) {
        if (!isAuthenticated()) {
            logger.addInfo("Not authenticated, skipping save");
            return;
        }

        if (hasChanged.compareAndSet(true, false)) {
            // Save asynchronously
            ACCOUNT_EXECUTOR.submit(() -> {
                List<Account> snapshot;
                synchronized(accountList) {
                    snapshot = new ArrayList<>(accountList);
                }
                
                logger.addInfo("Saving data...");
                try {
                    AppData data = new AppData(this.userPreferences, this.accountList);
                    OBJECT_WRITER.writeValue(dataFile, data);
                    logger.addInfo("Save OK");
                } catch (IOException e) {
                    logger.addError(e);
                }
            });
        } else {
            logger.addInfo("Nothing to save");
        }
        
        if(shutdown) {
            logger.addInfo("Shutting down executor services");
            FLUSH_SCHEDULER.shutdown();
            ACCOUNT_EXECUTOR.shutdown();
            try {
                ACCOUNT_EXECUTOR.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                logger.addError(e);
                ACCOUNT_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Wrapper class for application data
    private record AppData(UserPreferences userPreferences, List<Account> accountList) {}

    public void saveAll() {
        // when the user shuts down the program on the first run, it won't save
        logger.addInfo("Shutdown requested");
        saveDataFile(true);
        logger.closeStreams();
    }
}
