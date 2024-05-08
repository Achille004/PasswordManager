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

import static password.manager.utils.Utils.setDefaultButton;

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
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;
import lombok.Getter;
import password.manager.enums.Exporter;
import password.manager.enums.SortingOrder;
import password.manager.security.Account;
import password.manager.security.LoginAccount;

public class IOManager {
    static final String WINDOWS_PATH, DATA_FILE, LOG_FILE;
    static {
        WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        DATA_FILE = "passwords.psmg";
        LOG_FILE = "report.log";
    }

    private final @Getter String OS, USER_HOME;

    private final @Getter Logger logger;

    private @Getter LoginAccount loginAccount;
    private final ObservableList<Account> accountList;
    private final @Getter Path filePath, desktopPath;

    private String loginPassword;

    public IOManager() {
        // initialize objects
        accountList = FXCollections.observableArrayList(new Account[0]);
        loginAccount = null;
        loginPassword = null;

        // gets system properties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        filePath = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : ".passwordmanager");
        desktopPath = Path.of(USER_HOME, "Desktop");

        logger = new Logger(filePath.resolve(LOG_FILE).toFile());
    }

    public boolean loadDataFile(final ObservableResourceFactory langResources) {
        logger.addInfo("os.name: '" + OS + "'");
        logger.addInfo("user.home: '" + USER_HOME + "'");

        if (filePath.toFile().mkdirs()) {
            logger.addInfo("Directory '" + filePath + "' did not exist and was therefore created");

            return true;
        }

        // gets the log history
        logger.readFile();

        File data_file = filePath.resolve(DATA_FILE).toFile();

        // if the data file exists, it will try to read its contents
        if (!data_file.exists()) {
            logger.addInfo("File not found: '" + data_file + "'");

            return true;
        }

        try (FileInputStream f = new FileInputStream(data_file)) {
            ObjectInputStream fIN = new ObjectInputStream(f);
            Object obj;

            obj = fIN.readObject();
            if (obj instanceof LoginAccount) {
                loginAccount = (LoginAccount) obj;
            } else {
                throw new InvalidClassException(
                        "Unexpected object class. Expecting: " + LoginAccount.class);
            }

            obj = fIN.readObject();
            if (obj instanceof Account[]) {
                accountList.addAll((Account[]) obj);
            } else {
                throw new InvalidClassException(
                        "Unexpected object class. Expecting: " + ArrayList.class);
            }

            logger.addInfo("File loaded: '" + data_file + "'");

            // All the data was loaded successfully, so user can now log in
            return false;
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

        return true;
    }

    public SortedList<Account> getSortedAccountList() {
        return this.accountList.sorted(null);
    }

    // #region Account methods
    public boolean addAccount(String software, String username, String password) {
        try {
            if (isAuthenticated() && accountList.add(Account.of(software, username, password, loginPassword))) {
                logger.addInfo("Account added");
                return true;
            }
        } catch (GeneralSecurityException e) {
            logger.addError(e);
        }

        return false;
    }

    public boolean editAccount(@NotNull Account account, String software, String username, String password) {
        try {
            if (isAuthenticated() && account.setData(software, username, password, loginPassword)) {
                logger.addInfo("Account edited");
                return true;
            }
        } catch (GeneralSecurityException e) {
            logger.addError(e);
        }

        return false;
    }

    public boolean removeAccount(@NotNull Account account) {
        if (isAuthenticated() && accountList.remove(account)) {
            logger.addInfo("Account deleted");
            return true;
        }

        return false;
    }

    public String getAccountPassword(@NotNull Account account) {
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
    public boolean setLoginAccount(SortingOrder sortingOrder, Locale locale, @NotNull String loginPassword) {
        try {
            this.loginAccount = LoginAccount.of(sortingOrder, locale, loginPassword);
            this.loginPassword = loginPassword;

            logger.addInfo("Login account created");
            return true;
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
        }

        return false;
    }

    public final boolean changeLoginPassword(String newLoginPassword) {
        if (!isAuthenticated()) {
            return false;
        }
        String oldLoginPassword = this.loginPassword;

        try {
            loginAccount.setPasswordVerified(oldLoginPassword, newLoginPassword);
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
            return false;
        }

        if (!accountList.isEmpty()) {
            accountList.forEach(account -> {
                new Thread(() -> {
                    try {
                        account.changeLoginPassword(oldLoginPassword, newLoginPassword);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        }

        this.loginPassword = newLoginPassword;
        logger.addInfo("Login password changed");

        return true;
    }

    @SafeVarargs
    public final <T extends TextInputControl> void displayLoginPassword(T... elements) {
        for (T element : elements) {
            element.setText(loginPassword);
        }
    }

    public boolean authenticate(String loginPassword) {
        try {
            if (isAuthenticated() || !loginAccount.verifyPassword(loginPassword)) {
                return false;
            }
        } catch (InvalidKeySpecException e) {
            logger.addError(e);
            return false;
        }

        this.loginPassword = loginPassword;
        logger.addInfo("User authenticated");

        return true;
    }

    public boolean isAuthenticated() {
        return loginPassword != null;
    }
    // #endregion

    public void export(Exporter exporter, ObservableResourceFactory langResources) {
        try (FileWriter file = new FileWriter(
                desktopPath.resolve("Passwords." + exporter.name().toLowerCase()).toFile())) {
            file.write(exporter.getExporter().apply(accountList, langResources, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    private boolean saveAccountFile() {
        try (ObjectOutputStream fOUT = new ObjectOutputStream(
                new FileOutputStream(filePath.resolve(DATA_FILE).toFile()))) {
            fOUT.writeObject(this.loginAccount);
            fOUT.writeObject(this.accountList.toArray(new Account[0]));
            fOUT.close();

            logger.addInfo("Data file saved");

            return true;
        } catch (IOException e) {
            logger.addError(e);
            return false;
        }
    }

    public boolean saveAll() {
        boolean result = true;

        // when the user shuts down the program on the first run, it won't save
        if (loginAccount != null) {
            logger.addInfo("Shutting down");
            result = saveAccountFile() && logger.save();
        }

        return result;
    }
}
