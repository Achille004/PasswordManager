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
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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
        accountList = FXCollections.observableArrayList();
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
            AccountData data = objectMapper.readValue(FILE_PATH.resolve(DATA_FILE).toFile(), AccountData.class);

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
        // userPreferences.getLocaleProperty().addListener((observable, oldValue, newValue) -> logger.addInfo("Changed locale to: " + newValue));
        // userPreferences.getSortingOrderProperty().addListener((observable, oldValue, newValue) -> logger.addInfo("Changed sorting order to: " + newValue));
    }

    public SortedList<Account> getSortedAccountList() {
        return this.accountList.sorted(null);
    }

    // #region Account methods
    public @NotNull Boolean addAccount(String software, String username, String password) {
        if (isAuthenticated()) {
            try {
                accountList.add(Account.of(software, username, password, masterPassword));
                logger.addInfo("Account added");
                return true;
            } catch (GeneralSecurityException e) {
                logger.addError(e);
            }
        }

        return false;
    }

    public @NotNull Boolean editAccount(@NotNull Account account, @Nullable String software, @Nullable String username, @Nullable String password) {
        if (software == null || username == null || password == null) {
            return false;
        }

        int index = accountList.indexOf(account);
        if (isAuthenticated() && index >= 0) {
            try {
                account.setData(software, username, password, masterPassword);
                // Substitute the account with itself to trigger the SortedList wrapper
                accountList.set(index, account);
                logger.addInfo("Account edited");
                return true;
            } catch (GeneralSecurityException e) {
                logger.addError(e);
            }
        }

        return false;
    }

    public @NotNull Boolean removeAccount(@NotNull Account account) {
        if (isAuthenticated() && accountList.remove(account)) {
            logger.addInfo("Account deleted");
            return true;
        }

        return false;
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
            accountListTaskMultiThreaded(account -> {
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
                accountListTaskMultiThreaded(account -> {
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

    private void accountListTaskMultiThreaded(Consumer<? super Account> action) {
        ArrayList<Thread> threads = new ArrayList<>(this.accountList.size());
        this.accountList.forEach(account -> threads.add(Thread.startVirtualThread(() -> action.accept(account))));

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.addError(e);
            }
        });
    }
    // #endregion

    public void export(@NotNull Exporter exporter, ObservableResourceFactory langResources) {
        try (FileWriter file = new FileWriter(DESKTOP_PATH.resolve("Passwords." + exporter.name().toLowerCase()).toFile())) {
            file.write(exporter.getExporter().apply(accountList, langResources, masterPassword));
            file.flush();
        } catch (Exception e) {
            logger.addError(e);
        }
    }

    private boolean saveAccountFile() {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

        try {
            AccountData data = new AccountData(this.userPreferences, this.accountList);
            objectWriter.writeValue(FILE_PATH.resolve(DATA_FILE).toFile(), data);

            logger.addInfo("Data file saved");
            return true;
        } catch (IOException e) {
            logger.addError(e);
            return false;
        }
    }

    // Wrapper class for data
    private record AccountData(UserPreferences userPreferences, List<Account> accountList) {
    }

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
