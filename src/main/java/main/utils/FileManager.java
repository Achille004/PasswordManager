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

import main.security.Account;
import main.security.LoginAccount;

public class FileManager {
    private final Logger logger;

    private LoginAccount loginAccount;
    private ArrayList<Account> accountList;
    private final String filePath;

    private String loginPassword;

    public FileManager() {
        // initialize objects
        accountList = new ArrayList<>();

        // gets the filepath
        boolean isWindows = System.getProperty("os.name").contains("Windows");
        filePath = isWindows ? System.getProperty("user.home") + "\\AppData\\Local\\Password Manager\\" : "";

        logger = new Logger(filePath);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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

                    ArrayList list = (ArrayList) obj;
                    if (list.get(0) instanceof Account) {
                        accountList = (ArrayList<Account>) list;
                    } else {
                        throw new ClassNotFoundException(
                                "Unexpected object class. Expecting: " + Account.class.toString());
                    }
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
            setLoginAccount("s", "e", "LoginPassword");
        }
        loginPassword = "LoginPassword";

        // TODO if loginAccount == null, then it is first run, else just login
    }

    // #region AccountList methods
    public ArrayList<Account> getAccountList() {
        return this.accountList;
    }

    public void sortAccountList() {
        switch (loginAccount.getSavingOrder()) {
            case "s" -> this.accountList.sort((acc1, acc2) -> {
                int software = acc1.getSoftware().compareTo(acc2.getSoftware());
                return (software == 0) ? acc1.getUsername().compareTo(acc2.getUsername()) : software;
            });

            case "u" -> this.accountList.sort((acc1, acc2) -> {
                int username = acc1.getUsername().compareTo(acc2.getUsername());
                return (username == 0) ? acc1.getSoftware().compareTo(acc2.getSoftware()) : username;
            });

            default -> throw new IllegalArgumentException("Invalid saving order: " + loginAccount.getSavingOrder());
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

    public void setLoginAccount(String savingOrder, String language, String loginPassword) {
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
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passwords.html")) {
            file.write(Exporter.exportHtml(accountList, loginAccount.getLanguage(), loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    public void csvMenuItemActionPerformed() {
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passwords.csv")) {
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
