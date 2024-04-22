/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

package main.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TextInputControl;
import main.enums.SavingOrder;
import main.security.Account;
import main.security.LoginAccount;

public class IOManager {
    static final String WINDOWS_PATH, DATA_FILE, LOG_FILE;
    static {
        WINDOWS_PATH = Path.of("AppData", "Local", "Password Manager").toString();
        DATA_FILE = "passwords.psmg";
        LOG_FILE = "report.log";
    }

    private final String OS, USER_HOME;

    private final Logger logger;

    private LoginAccount loginAccount;
    private final ObservableList<Account> accountList;
    private final Path filePath, desktopPath;

    private String loginPassword;

    public IOManager() {
        // initialize objects
        accountList = FXCollections.observableArrayList(new Account[0]);
        loginAccount = null;
        loginPassword = null;

        // gets system preoperties
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the paths
        filePath = Path.of(USER_HOME, OS.toLowerCase().contains("windows") ? WINDOWS_PATH : ".passwordmanager");
        desktopPath = Path.of(USER_HOME, "Desktop");

        logger = new Logger(filePath.resolve(LOG_FILE).toFile());
    }

    public void loadDataFile() {
        boolean firstRun = true;
        if (!filePath.toFile().mkdirs()) {
            // gets the log history
            logger.readFile();

            logger.addInfo("os.name: '" + OS + "'");
            logger.addInfo("user.home: '" + USER_HOME + "'");

            File data_file = filePath.resolve(DATA_FILE).toFile();
            // if the data file exists, it will try to read its contents
            if (data_file.exists()) {
                try (FileInputStream f = new FileInputStream(data_file)) {
                    ObjectInputStream fIN = new ObjectInputStream(f);
                    Object obj;

                    obj = fIN.readObject();
                    if (obj instanceof LoginAccount) {
                        loginAccount = (LoginAccount) obj;
                    } else {
                        throw new InvalidClassException(
                                "Unexpected object class. Expecting: " + LoginAccount.class.toString());
                    }

                    obj = fIN.readObject();
                    if (obj instanceof Account[]) {
                        accountList.addAll((Account[]) obj);
                    } else {
                        throw new InvalidClassException(
                                "Unexpected object class. Expecting: " + ArrayList.class.toString());
                    }

                    logger.addInfo("File loaded: '" + data_file.toString() + "'");

                    // All the data was loaded successfully, so user can now login
                    firstRun = false;
                } catch (IOException | ClassNotFoundException e) {
                    // TODO invalid file version: ask to overwrite (exit application in case of
                    // denial)
                    // "There was a problem loading your passwords: ..."
                    // if (e.getCause() instanceof InvalidClassException) -> probably is a different
                    // file version

                    logger.addError(e);
                }
            } else {
                logger.addInfo("File not found: '" + data_file.toString() + "'");
            }
        } else {
            logger.addInfo("Directory '" + filePath.toString() + "' did not exist and was therefore created");
        }

        // TODO login and first run (remove everything following this comment)
        if (firstRun) {
            setLoginAccount(SavingOrder.Software, Locale.ENGLISH, "LoginPassword");
        }
    }

    // #region AccountList methods
    public ObservableList<Account> getAccountList() {
        return this.accountList;
    }

    public SortedList<Account> getSortedAccountList() {
        return this.accountList.sorted(null);
    }

    public void sortAccountList() {
        this.accountList.sort(loginAccount.getSavingOrder().getComparator());
    }
    // #endregion

    // #region Account methods
    /**
     * Sorts the account list.
     */
    public void addAccount(String software, String username, String password) {
        accountList.add(Account.of(software, username, password, loginPassword));
        sortAccountList();

        logger.addInfo("Account added");
    }

    public void replaceAccount(int index, String software, String username, String password) {
        if (index >= 0 && index < accountList.size()) {
            accountList.set(index, Account.of(software, username, password, loginPassword));
            sortAccountList();

            logger.addInfo("Account edited");
        }
    }

    public void deleteAccount(int index) {
        if (index >= 0 && index < accountList.size()) {
            accountList.remove(index);

            logger.addInfo("Account deleted");
        }
    }

    public String getAccountPassword(@NotNull Account account) {
        return account.getPassword(loginPassword);
    }
    // #endregion

    // #region LoginAccount methods
    public boolean setLoginAccount(SavingOrder savingOrder, Locale locale, String loginPassword) {
        if (loginAccount != null) {
            return false;
        }

        this.loginAccount = LoginAccount.of(savingOrder, locale, loginPassword);
        this.loginPassword = loginPassword;
        logger.addInfo("Login account created");

        return true;
    }

    public LoginAccount getLoginAccount() {
        return this.loginAccount;
    }

    public final boolean changeLoginPassword(String loginPassword) {
        if (!isAuthenticated()) {
            return false;
        }

        loginAccount.setPasswordVerified(this.loginPassword, loginPassword);

        if (!accountList.isEmpty()) {
            accountList.forEach(account -> account.changeLoginPassword(this.loginPassword, loginPassword));
            sortAccountList();
        }

        this.loginPassword = loginPassword;
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
        if (isAuthenticated() || !loginAccount.verifyPassword(loginPassword)) {
            return false;
        }

        logger.addInfo("Successful login");
        this.loginPassword = loginPassword;
        return true;
    }

    public boolean isAuthenticated() {
        return loginPassword != null;
    }
    // #endregion

    public Logger getLogger() {
        return logger;
    }

    // #region Exporters
    public void exportHtml() {
        try (FileWriter file = new FileWriter(desktopPath.resolve("Passwords.html").toFile())) {
            file.write(Exporter.exportHtml(accountList, loginAccount.getLocale(), loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    public void csvMenuItemActionPerformed() {
        try (FileWriter file = new FileWriter(desktopPath.resolve("Passwords.csv").toFile())) {
            file.write(Exporter.exportCsv(accountList, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }
    // #endregion

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

    public void saveAll() {
        // when the user shuts down the program on the first run, it won't save
        if (loginAccount != null) {
            logger.addInfo("Shutting down");

            saveAccountFile();
            logger.save();
        }
    }
}
