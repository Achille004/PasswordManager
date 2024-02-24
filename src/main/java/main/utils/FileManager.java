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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import main.enums.Language;
import main.enums.SavingOrder;
import main.security.Account;
import main.security.LoginAccount;

public class FileManager {
    private final String OS, USER_HOME;

    private final Logger logger;

    private LoginAccount loginAccount;
    private ArrayList<Account> accountList;
    private final String filePath;

    private String loginPassword;

    public FileManager() {
        // initialize objects
        accountList = new ArrayList<>();

        // gets system paths
        OS = System.getProperty("os.name");
        USER_HOME = System.getProperty("user.home");

        // gets the filepath
        filePath = USER_HOME + (OS.contains("Windows") ? "\\AppData\\Local" : "") + "\\Password Manager\\";

        logger = new Logger(filePath);
    }

    @SuppressWarnings({ "unchecked" })
    public void loadDataFile() {
        File data_file = new File(filePath + "passwords.psmg");
        if (data_file.getParentFile().mkdirs()) {
            logger.addInfo("Created folder '" + data_file.getParentFile().getAbsolutePath() + "'");
        }

        // if the data file exists, it will try to read its contents
        if (data_file.exists()) {
            try (FileInputStream f = new FileInputStream(data_file)) {
                ObjectInputStream fIN = new ObjectInputStream(f);
                Object obj;

                obj = fIN.readObject();
                if (obj instanceof LoginAccount) {
                    loginAccount = (LoginAccount) obj;
                } else {
                    throw new ClassNotFoundException(
                            "Unexpected object class. Expecting: " + LoginAccount.class.toString());
                }

                obj = fIN.readObject();
                if (obj instanceof ArrayList) {
                    accountList = (ArrayList<Account>) obj;
                } else {
                    throw new ClassNotFoundException(
                            "Unexpected object class. Expecting: " + ArrayList.class.toString());
                }
            } catch (IOException e) {
                logger.addError(e);
            } catch (ClassNotFoundException e) {
                // TODO ask to overwrite
                logger.addError(e);
            }
        }

        if (loginAccount != null) {
            // gets the log history
            logger.readFile();
        } else {
            // TODO remove when first run works
            setLoginAccount(SavingOrder.Software, Language.English, "LoginPassword");
        }

        // TODO if loginAccount == null, then it is first run, else just login
        loginPassword = "LoginPassword";
    }

    // #region AccountList methods
    public ArrayList<Account> getAccountList() {
        return this.accountList;
    }

    public void sortAccountList() {
        switch (loginAccount.getSavingOrder()) {
            case Software -> this.accountList.sort((acc1, acc2) -> {
                int software = acc1.getSoftware().compareTo(acc2.getSoftware());
                return (software == 0) ? acc1.getUsername().compareTo(acc2.getUsername()) : software;
            });

            case Username -> this.accountList.sort((acc1, acc2) -> {
                int username = acc1.getUsername().compareTo(acc2.getUsername());
                return (username == 0) ? acc1.getSoftware().compareTo(acc2.getSoftware()) : username;
            });

            default ->
                throw new IllegalArgumentException("Invalid saving order: " + loginAccount.getSavingOrder().name());
        }
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
    public LoginAccount getLoginAccount() {
        return this.loginAccount;
    }

    public void setLoginAccount(SavingOrder savingOrder, Language language, String loginPassword) {
        this.loginAccount = LoginAccount.of(savingOrder, language, loginPassword);

        if (!accountList.isEmpty()) {
            accountList.forEach(account -> account.changeLoginPassword(this.loginPassword, loginPassword));
            sortAccountList();

            logger.addInfo("Login account changed");
        }

        this.loginPassword = loginPassword;
    }
    // #endregion

    public Logger getLogger() {
        return logger;
    }

    // #region Exporters
    public void exportHtml() {
        try (FileWriter file = new FileWriter(USER_HOME + (OS.contains("Windows") ? "\\Desktop" : "") + "\\Passwords.html")) {
            file.write(Exporter.exportHtml(accountList, loginAccount.getLanguage(), loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    public void csvMenuItemActionPerformed() {
        try (FileWriter file = new FileWriter(USER_HOME + (OS.contains("Windows") ? "\\Desktop" : "") + "\\Passwords.csv")) { 
            file.write(Exporter.exportCsv(accountList, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }
    // #endregion

    private void saveAccountFile() {
        try (ObjectOutputStream fOUT = new ObjectOutputStream(new FileOutputStream(filePath + "passwords.psmg"))) {
            fOUT.writeObject(this.loginAccount);
            fOUT.writeObject(this.accountList);
            fOUT.close();

            logger.addInfo("Files saved");
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    public void saveAll() {
        // when the user shuts down the program on the first run, it won't save
        if (loginAccount != null) {
            saveAccountFile();

            logger.addInfo("Program shut down");
            logger.save();
        }
    }
}
