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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import javafx.application.Platform;
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
    static final String OS, USER_HOME, DATA_FILE;
    public static final Path FILE_PATH, DESKTOP_PATH;

    private static final ExecutorService ACCOUNT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static {
        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        String WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        String OS_FALLBACK_PATH = ".password-manager";
        FILE_PATH = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : OS_FALLBACK_PATH);
        DESKTOP_PATH = Path.of(USER_HOME, "Desktop");

        // define data file name
        DATA_FILE = "data.json";
    }

    private final @Getter Logger logger;
    private final ObservableList<Account> accountList;
    private @Getter UserPreferences userPreferences;

    private String masterPassword;
    private @Getter boolean isFirstRun, isAuthenticated;

    public IOManager() {
        logger = new Logger(FILE_PATH);
        accountList = FXCollections.observableList(Collections.synchronizedList(new ArrayList<>()));
        userPreferences = UserPreferences.empty();

        masterPassword = null;
        isFirstRun = true;
        isAuthenticated = false;
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

        File data_file = FILE_PATH.resolve(DATA_FILE).toFile();
        logger.addInfo("Loading data (" + data_file + ")...");

        // if the data file exists, it will try to read its contents
        if (!data_file.exists()) {
            logger.addInfo("File not found");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AppData data = objectMapper.readValue(FILE_PATH.resolve(DATA_FILE).toFile(), AppData.class);

            this.userPreferences = data.userPreferences();
            accountList.addAll(Collections.nCopies(data.accountList().size(), null));
            FXCollections.copy(this.accountList, data.accountList());

            logger.addInfo("Success");
            isFirstRun = false;
        } catch (IOException e) {
            logger.addError(e);

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

        if (oldMasterPassword != null) {
            boolean[] error = new boolean[1];
            accountListTaskExec(account -> {
                try {
                    account.changeMasterPassword(oldMasterPassword, newMasterPassword);
                } catch (GeneralSecurityException e) {
                    logger.addError(e);
                    error[0] = true;
                }
            });

            if (error[0]) {
                return false;
            }
        }

        this.masterPassword = newMasterPassword;
        if (oldMasterPassword != null) {
            logger.addInfo("Master password changed");
        } else {
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

    private boolean saveAccountFile() {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

        ACCOUNT_EXECUTOR.shutdown();
        try {
            ACCOUNT_EXECUTOR.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.addError(e);
            ACCOUNT_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }

        List<Account> snapshot;
        synchronized(accountList) {
            snapshot = new ArrayList<>(accountList);
        }

        try {
            AppData data = new AppData(this.userPreferences, this.accountList);
            objectWriter.writeValue(FILE_PATH.resolve(DATA_FILE).toFile(), data);

            logger.addInfo("Data file saved");
            return true;
        } catch (IOException e) {
            logger.addError(e);
            return false;
        }
    }

    // Wrapper class for application data
    private record AppData(UserPreferences userPreferences, List<Account> accountList) {}

    public @NotNull Boolean saveAll() {    
        boolean result = false;
        // when the user shuts down the program on the first run, it won't save
        if (!isFirstRun() || isAuthenticated()) {
            logger.addInfo("Shutting down");
            result = saveAccountFile();
            logger.closeStreams();
        }

        return result;
    }
}
