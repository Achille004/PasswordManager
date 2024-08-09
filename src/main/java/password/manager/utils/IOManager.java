/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager.utils;

import static password.manager.utils.Utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;
import lombok.Getter;
import password.manager.enums.Exporter;
import password.manager.security.Account;
import password.manager.security.UserPreferences;

public final class IOManager {
    static final String WINDOWS_PATH, DATA_FILE, LOG_FILE;
    static final @Getter String OS, USER_HOME;
    public static final @Getter Path FILE_PATH, DESKTOP_PATH;

    static {
        WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        DATA_FILE = "passwords.psmg";
        LOG_FILE = "report.log";

        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        FILE_PATH = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : ".password-manager");
        DESKTOP_PATH = Path.of(USER_HOME, "Desktop");
    }

    private final @Getter Logger logger;
    private final ObservableList<Account> accountList;
    private @Getter UserPreferences userPreferences;

    private String loginPassword;
    private @Getter boolean isFirstRun, isAuthenticated;

    public IOManager() {
        logger = new Logger(FILE_PATH.resolve(LOG_FILE).toFile());
        accountList = FXCollections.observableArrayList(new Account[0]);
        userPreferences = UserPreferences.empty();

        loginPassword = null;
        isFirstRun = true;
        isAuthenticated = false;
    }

    public void loadDataFile(final ObservableResourceFactory langResources) {
        if (!userPreferences.isEmpty()) {
            logger.addError(new UnsupportedOperationException("Cannot read data file: it would overwrite non-empty user preferences."));
            return;
        }

        logger.addInfo("os.name: '" + OS + "'");
        logger.addInfo("user.home: '" + USER_HOME + "'");

        if (FILE_PATH.toFile().mkdirs()) {
            logger.addInfo("Directory '" + FILE_PATH + "' did not exist and was therefore created");
            return;
        }

        // gets the log history
        logger.readFile();

        File data_file = FILE_PATH.resolve(DATA_FILE).toFile();
        // if the data file exists, it will try to read its contents
        if (!data_file.exists()) {
            logger.addInfo("File not found: '" + data_file + "'");
            return;
        }

        try (FileInputStream f = new FileInputStream(data_file)) {
            ObjectInputStream fIN = new ObjectInputStream(f);
            Object obj;

            obj = fIN.readObject();
            if (obj instanceof UserPreferences usrPrfcs) {
                userPreferences = usrPrfcs;
            } else {
                throw new InvalidClassException("Unexpected object class. Expecting: " + UserPreferences.class);
            }

            obj = fIN.readObject();
            if (obj instanceof Account[] accounts) {
                accountList.addAll(accounts);
            } else {
                throw new InvalidClassException("Unexpected object class. Expecting: " + ArrayList.class);
            }

            logger.addInfo("File loaded: '" + data_file + "'");

            // All the data was loaded successfully
            isFirstRun = false;
        } catch (IOException | ClassNotFoundException e) {
            logger.addError(e);

            Alert alert = new Alert(AlertType.ERROR, langResources.getValue("load_error"), ButtonType.YES,
                    ButtonType.NO);
            setDefaultButton(alert, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() != ButtonType.YES) {
                logger.addInfo("Data overwriting denied");
                logger.save();
                System.exit(0);
            } else {
                logger.addInfo("Data overwriting accepted");
            }
        }
    }

    public SortedList<Account> getSortedAccountList() {
        return this.accountList.sorted(null);
    }

    // #region Account methods
    public @NotNull Boolean addAccount(String software, String username, String password) {
        if (isAuthenticated()) {
            try {
                accountList.add(Account.of(software, username, password, loginPassword));
                logger.addInfo("Account added");
                return true;
            } catch (GeneralSecurityException e) {
                logger.addError(e);
            }
        }

        return false;
    }

    public @NotNull Boolean editAccount(@NotNull Account account, @Nullable String software, @Nullable String username, @Nullable String password) {
        if(software == null || username == null || password == null) {
            return false;
        }

        try {
            int index = accountList.indexOf(account);
            if (isAuthenticated() && index >= 0) {
                account.setData(software, username, password, loginPassword);
                // Substitute the account with itself to trigger the SortedList wrapper
                accountList.set(index, account);
                logger.addInfo("Account edited");
                return true;
            }
        } catch (GeneralSecurityException e) {
            logger.addError(e);
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
                return account.getPassword(loginPassword);
            }
        } catch (GeneralSecurityException e) {
            logger.addError(e);
        }

        return null;
    }
    // #endregion

    // #region LoginAccount methods
    public @NotNull Boolean changeLoginPassword(String newLoginPassword) {
        String oldLoginPassword = this.loginPassword;
        if (oldLoginPassword != null && !isAuthenticated()) {
            return false;
        }

        try {
            userPreferences.setPasswordVerified(oldLoginPassword, newLoginPassword);
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
            return false;
        }

        boolean[] error = new boolean[1];
        accountList.forEach(account -> {
            Thread.startVirtualThread(() -> {
                try {
                    account.changeLoginPassword(oldLoginPassword, newLoginPassword);
                } catch (GeneralSecurityException e) {
                    logger.addError(e);
                    error[0] = true;
                }
            });
            // to wait until threads are finished, use threadInstance.join()
        });
        if(error[0]) {
            return false;
        }

        this.loginPassword = newLoginPassword;
        if (oldLoginPassword != null) {
            logger.addInfo("Login password changed");
        } else {
            isAuthenticated = true;
        }

        return true;
    }

    @SafeVarargs
    public final <T extends TextInputControl> void displayLoginPassword(T @NotNull ... elements) {
        for (T element : elements) {
            element.setText(loginPassword);
        }
    }

    public @NotNull Boolean authenticate(String loginPassword) {
        if (isAuthenticated()) {
            return false;
        }

        try {
            isAuthenticated = userPreferences.verifyPassword(loginPassword);
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
        }

        if (isAuthenticated) {
            this.loginPassword = loginPassword;
            logger.addInfo("User authenticated");
        }

        return isAuthenticated;
    }
    // #endregion

    public void export(@NotNull Exporter exporter, ObservableResourceFactory langResources) {
        try (FileWriter file = new FileWriter(DESKTOP_PATH.resolve("Passwords." + exporter.name().toLowerCase()).toFile())) {
            file.write(exporter.getExporter().apply(accountList, langResources, loginPassword));
            file.flush();
        } catch (Exception e) {
            logger.addError(e);
        }
    }

    private boolean saveAccountFile() {
        try (ObjectOutputStream fOUT = new ObjectOutputStream(new FileOutputStream(FILE_PATH.resolve(DATA_FILE).toFile()))) {
            fOUT.writeObject(this.userPreferences);
            fOUT.writeObject(this.accountList.toArray(new Account[0]));

            logger.addInfo("Data file saved");
            return true;
        } catch (IOException e) {
            logger.addError(e);
            return false;
        }
    }

    public @NotNull Boolean saveAll() {
        boolean result = false;

        // when the user shuts down the program on the first run, it won't save
        if (!isFirstRun() || isAuthenticated()) {
            logger.addInfo("Shutting down");
            result = saveAccountFile() && logger.save();
        }

        return result;
    }
}
