/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022  Francesco Marras

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

package main;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

import main.Utils.Exporter;
import main.accounts.Account;
import main.accounts.LoginAccount;

/**
 * Main class.
 *
 * @version 2.0
 * @author 2004marras@gmail.com
 */
public class Main extends javax.swing.JFrame {
    private final Logger logger;

    private LoginAccount loginAccount;
    private ActionListener timerTask;
    private ArrayList<Account> accountList;
    private byte loginCounter;
    private boolean deleteCounter;
    private int delay;
    private String filePath = "";

    private String language;
    private String loginPassword;

    /**
     * Constructor method, initialize objects and gets the data filepath, then
     * runs the program.
     */
    public Main() {
        initComponents();

        // initialize objects
        accountList = new ArrayList<>();
        deleteCounter = false;
        delay = loginCounter = 0;
        timerTask = null;

        MenuBar.setVisible(false);

        // gets the filepath
        if (System.getProperty("os.name").contains("Windows")) {
            filePath = System.getProperty("user.home") + "\\AppData\\Local\\Password Manager\\";
        }

        logger = new Logger(filePath);

        run();
    }

    /**
     * Main method, initializes JFrame and calls Main's constructor.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Main().setVisible(true));
    }

    private void FirstRunConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // gets the login password
        loginPassword = FirstRunLoginpasswordTextField.getText();

        // checks if all is inserted and EULA are accepted
        if (selectedItemInComboBox(FirstRunLanguageSelector) >= 0
                && selectedItemInComboBox(FirstRunSavingorderSelector) >= 0 && !(loginPassword.isBlank())
                && FirstRunAccepteulaCheckBox.isSelected()) {
            String savingOrder = "";

            // translates the index into the actual language
            switch (selectedItemInComboBox(FirstRunLanguageSelector)) {
                case 0 -> language = "e";
                case 1 -> language = "i";
            }

            // translates the index into the actual saving order
            switch (selectedItemInComboBox(FirstRunSavingorderSelector)) {
                case 0 -> savingOrder = "s";
                case 1 -> savingOrder = "u";
            }

            // saves all in the new login account
            try {
                loginAccount = LoginAccount.createAccount(savingOrder, language, loginPassword);
            } catch (NoSuchAlgorithmException e) {
                logger.addError(e);
            }

            logger.addInfo("First Run successful, accepted EULA)");

            switch (language) {
                case "e" -> {
                    EncrypterButton.setText("Encrypter");
                    DecrypterButton.setText("Decrypter");
                    SettingsButton.setText("Settings");
                    LogHistoryButton.setText("Log History");
                    EulaAndCreditsButton.setText("EULA and Credits");
                }

                case "i" -> {
                    EncrypterButton.setText("Crittografa");
                    DecrypterButton.setText("Decifra");
                    SettingsButton.setText("Impostazioni");
                    LogHistoryButton.setText("Cronologia Registro");
                    EulaAndCreditsButton.setText("Termini e Crediti");
                }
            }
            EncrypterButton.repaint();
            DecrypterButton.repaint();
            SettingsButton.repaint();
            LogHistoryButton.repaint();
            EulaAndCreditsButton.repaint();

            Utils.replacePanel(MainPanel, ProgramPanel);
        }
    }

    private void LoginButtonActionPerformed(java.awt.event.ActionEvent evt) {
        loginPassword = new String(LoginPasswordField.getPassword());
        LoginPasswordField.setText("");
        LoginPasswordField.repaint();

        if (loginPassword.isBlank()) {
            switch (language) {
                case "e" -> LoginUnsuccesfulLabel.setText("Password field empty.");
                case "i" -> LoginUnsuccesfulLabel.setText("Password non inserita.");
            }

            LoginUnsuccesfulLabel.repaint();

            delay = 800;
            timerTask = (ActionEvent e) -> {
                LoginButton.setEnabled(true);
                LoginUnsuccesfulLabel.setText("");
                LoginUnsuccesfulLabel.repaint();
            };

            return;
        }

        if (loginAccount.verifyPassword(loginPassword)) {
            logger.addInfo("Successful Login");

            switch (language) {
                case "e" -> {
                    ExportAsMenu.setText("Export as");
                    EncrypterButton.setText("Encrypter");
                    DecrypterButton.setText("Decrypter");
                    SettingsButton.setText("Settings");
                    LogHistoryButton.setText("Log History");
                    EulaAndCreditsButton.setText("EULA and Credits");
                }

                case "i" -> {
                    ExportAsMenu.setText("Esporta come");
                    EncrypterButton.setText("Crittografa");
                    DecrypterButton.setText("Decifra");
                    SettingsButton.setText("Impostazioni");
                    LogHistoryButton.setText("Cronologia Registro");
                    EulaAndCreditsButton.setText("Termini e Crediti");
                }
            }

            ExportAsMenu.repaint();
            EncrypterButton.repaint();
            DecrypterButton.repaint();
            SettingsButton.repaint();
            LogHistoryButton.repaint();
            EulaAndCreditsButton.repaint();

            if (!accountList.isEmpty()) {
                MenuBar.setVisible(true);
            }

            // redirects to main panel
            Utils.replacePanel(MainPanel, ProgramPanel);

            return;
        }

        // adds a failed attempt
        loginCounter++;

        if (loginCounter == 3) {
            switch (language) {
                case "e" -> LoginUnsuccesfulLabel
                        .setText("A wrong password has been insterted three times, program sutting down...");

                case "i" -> LoginUnsuccesfulLabel
                        .setText("È stata inserita una passowrd errata tre volte, programma in arresto...");
            }
            LoginUnsuccesfulLabel.repaint();

            logger.addInfo("Unsccessful Login");

            delay = 2000;
            timerTask = (ActionEvent e) -> {
                LoginButton.setEnabled(true);
                LoginUnsuccesfulLabel.setText("");
                LoginUnsuccesfulLabel.repaint();
                System.exit(0);
            };
        } else {
            switch (language) {
                case "e" -> LoginUnsuccesfulLabel.setText("Wrong password.");
                case "i" -> LoginUnsuccesfulLabel.setText("Passowrd errata.");
            }
            LoginUnsuccesfulLabel.repaint();

            delay = 800;
            timerTask = (ActionEvent e) -> {
                LoginButton.setEnabled(true);
                LoginUnsuccesfulLabel.setText("");
                LoginUnsuccesfulLabel.repaint();
            };
        }

        LoginUnsuccesfulLabel.repaint();
        LoginButton.setEnabled(false);

        Timer timer = new Timer(delay, timerTask);
        timer.setRepeats(false);
        timer.start();
    }

    // #region App navigation
    private void EncrypterButtonActionPerformed(java.awt.event.ActionEvent evt) {
        switch (language) {
            case "e" -> {
                EncrypterSaveButton.setText("Save");
                EncrypterUsernameLabel.setText("Username:");
            }

            case "i" -> {
                EncrypterSaveButton.setText("Salva");
                EncrypterUsernameLabel.setText("Nome utente:");
            }
        }

        EncrypterSaveButton.repaint();
        EncrypterUsernameLabel.repaint();

        // redirects to encrypter panel
        Utils.replacePanel(DialogPanel, EncrypterPanel);
    }

    private void DecrypterButtonActionPerformed(java.awt.event.ActionEvent evt) {
        switch (language) {
            case "e" -> {
                DecrypterDeleteButton.setText("Delete");
                DecrypterSaveButton.setText("Save");
                DecrypterUsernameLabel.setText("Username:");
            }

            case "i" -> {
                DecrypterDeleteButton.setText("Elimina");
                DecrypterSaveButton.setText("Salva");
                DecrypterUsernameLabel.setText("Nome utente:");
            }
        }

        DecrypterDeleteButton.setBackground(new java.awt.Color(51, 51, 51));
        DecrypterDeleteButton.repaint();
        DecrypterSaveButton.repaint();
        DecrypterUsernameLabel.repaint();

        // to make sure the decrypter delete button is in its first state
        deleteCounter = false;

        updateDecrypterAccountSelector();

        // redirects to decrypter panel
        Utils.replacePanel(DialogPanel, DecrypterPanel);
    }

    private void SettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        SettingsLanguageSelector.removeAllItems();
        SettingsSavingorderSelector.removeAllItems();

        switch (language) {
            case "e" -> {
                SettingsLanguageSelector.addItem("English");
                SettingsLanguageSelector.addItem("Italian");
                SettingsSavingorderSelector.addItem("Software");
                SettingsSavingorderSelector.addItem("Username");

                SettingsLanguageLabel.setText("Language:");
                SettingsSavingorderLabel.setText("Saving order:");
                SettingsLoginpassowrdLabel.setText("Login password:");
                SettingsSaveButton.setText("Confirm");

                SettingsLanguageSelector.setSelectedIndex(0);
            }

            case "i" -> {
                SettingsLanguageSelector.addItem("Inglese");
                SettingsLanguageSelector.addItem("Italiano");
                SettingsSavingorderSelector.addItem("Software");
                SettingsSavingorderSelector.addItem("Nome utente");

                SettingsLanguageLabel.setText("Linguaggio:");
                SettingsSavingorderLabel.setText("Ordine di salvataggio:");
                SettingsLoginpassowrdLabel.setText("Password d'accesso:");
                SettingsSaveButton.setText("Conferma");

                SettingsLanguageSelector.setSelectedIndex(1);
            }
        }

        SettingsLanguageSelector.repaint();
        SettingsSavingorderSelector.repaint();
        SettingsLanguageLabel.repaint();
        SettingsSavingorderLabel.repaint();
        SettingsLoginpassowrdLabel.repaint();
        SettingsSaveButton.repaint();

        // sets the current saving order
        SettingsSavingorderSelector.repaint();
        if (loginAccount.getSavingOrder().equals("s")) {
            SettingsSavingorderSelector.setSelectedIndex(0);
        } else if (loginAccount.getSavingOrder().equals("u")) {
            SettingsSavingorderSelector.setSelectedIndex(1);
        }

        // sets the current login password
        SettingsLoginPasswordTextField.setText(loginPassword);

        // redirects to settings panel
        Utils.replacePanel(DialogPanel, SettingsPanel);
    }

    private void LogHistoryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        logger.addInfo("Log history showed");

        switch (language) {
            case "e" -> {
                LogHystoryLabel.setText("Log History:");
                LogLegendLabel.setText("[Actions]    {Errors}");
            }

            case "i" -> {
                LogHystoryLabel.setText("Cronologia del Registro:");
                LogLegendLabel.setText("[Azioni]    {Errori}");
            }
        }

        LogHystoryLabel.repaint();
        LogLegendLabel.repaint();

        // writes the log history to its text area
        LogHistoryTextArea.setText(logger.getLogHistory());

        // redirects to lohg history panel
        Utils.replacePanel(DialogPanel, LogHistoryPanel);
    }

    private void EulaAndCreditsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        logger.addInfo("EULA and Credits showed");

        // redirects to eula and credits panel
        Utils.replacePanel(DialogPanel, EulaAndCreditsPanel);
    }
    // #endregion

    private void EncrypterSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // gets software, username and password written by the user
        String software = EncrypterSoftwareTextField.getText();
        String username = EncrypterUsernameTextField.getText();
        String password = EncrypterPasswordTextField.getText();

        if (!(software.isBlank() || username.isBlank() || password.isBlank())) {
            Account account;
            try {
                account = Account.of(software, username, password, loginPassword);
                this.accountList.add(account);
                resortAccountList();

                logger.addInfo("Account added");
            } catch (Exception e) {
                logger.addError(e);
            }

            EncrypterSoftwareTextField.setText("");
            EncrypterSoftwareTextField.repaint();

            EncrypterUsernameTextField.setText("");
            EncrypterUsernameTextField.repaint();

            EncrypterPasswordTextField.setText("");
            EncrypterPasswordTextField.repaint();

            MenuBar.setVisible(true);
        }

        MenuBar.setVisible(true);
    }

    private void DecrypterAccountSelectorActionPerformed(java.awt.event.ActionEvent evt) {
        deleteCounter = false;
        switch (language) {
            case "e" -> DecrypterDeleteButton.setText("Delete");
            case "i" -> DecrypterDeleteButton.setText("Elimina");
        }
        DecrypterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterDeleteButton.repaint();

        if (selectedItemInComboBox(DecrypterAccountSelector) >= 0) {
            // shows the software, username and account of the selected item
            DecrypterSoftwareTextField
                    .setText(accountList.get(selectedItemInComboBox(DecrypterAccountSelector)).getSoftware());
            DecrypterSoftwareTextField.repaint();

            DecrypterUsernameTextField
                    .setText(accountList.get(selectedItemInComboBox(DecrypterAccountSelector)).getUsername());
            DecrypterUsernameTextField.repaint();

            DecrypterPasswordTextField
                    .setText(accountList.get(selectedItemInComboBox(DecrypterAccountSelector))
                            .getPassword(loginPassword));
            DecrypterPasswordTextField.repaint();
        } else {
            DecrypterSoftwareTextField.setText("");
            DecrypterSoftwareTextField.repaint();

            DecrypterUsernameTextField.setText("");
            DecrypterUsernameTextField.repaint();

            DecrypterPasswordTextField.setText("");
            DecrypterPasswordTextField.repaint();
        }
    }

    private void DecrypterSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (selectedItemInComboBox(DecrypterAccountSelector) >= 0) {
            // get the new software, username and password
            String software = DecrypterSoftwareTextField.getText();
            String username = DecrypterUsernameTextField.getText();
            String password = DecrypterPasswordTextField.getText();

            if (!(software.isBlank() && username.isBlank() && password.isBlank())) {
                // save the new attributes of the account
                Account account = accountList.get(selectedItemInComboBox(DecrypterAccountSelector));
                try {
                    account.setSoftware(software);
                    account.setUsername(username);
                    account.setPassword(password, loginPassword);
                } catch (Exception e) {
                    logger.addError(e);
                }

                logger.addInfo("Account edited");

                updateDecrypterAccountSelector();
            }
        }
    }

    private void DecrypterDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (selectedItemInComboBox(DecrypterAccountSelector) == -1) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (deleteCounter) {
            switch (language) {
                case "e" -> DecrypterDeleteButton.setText("Delete");
                case "i" -> DecrypterDeleteButton.setText("Elimina");
            }
            DecrypterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
            DecrypterDeleteButton.repaint();

            // removes the selected account from the list
            accountList.remove(selectedItemInComboBox(DecrypterAccountSelector));

            updateDecrypterAccountSelector();

            if (accountList.isEmpty()) {
                MenuBar.setVisible(false);
            }

            logger.addInfo("Account deleted");
        } else {
            switch (language) {
                case "e" -> DecrypterDeleteButton.setText("Sure?");
                case "i" -> DecrypterDeleteButton.setText("Sicuro?");
            }

            DecrypterDeleteButton.setBackground(new java.awt.Color(219, 67, 67));
            DecrypterDeleteButton.repaint();
        }

        deleteCounter = !deleteCounter;
    }

    private void SettingsSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        loginPassword = SettingsLoginPasswordTextField.getText();

        if (!loginPassword.isBlank()) {
            // translates the index into the actual language
            if (SettingsLanguageSelector.getSelectedIndex() == 0) {
                loginAccount.setLanguage("e");
            } else if (SettingsLanguageSelector.getSelectedIndex() == 1) {
                loginAccount.setLanguage("i");
            }
            language = loginAccount.getLanguage();

            // translates the index into the actual saving order
            if (SettingsSavingorderSelector.getSelectedIndex() == 0) {
                loginAccount.setSavingOrder("s");
            } else if (SettingsSavingorderSelector.getSelectedIndex() == 1) {
                loginAccount.setSavingOrder("u");
            }
            resortAccountList();

            // gets and saves the new login password
            loginAccount.setPassword(loginPassword);

            logger.addInfo("Settings changed");

            // reloads Program Panel
            SettingsButtonActionPerformed(evt);
        }
    }

    private void formWindowClosing() {
        // when the user shuts down the program on the first run, it won't save
        if (loginAccount != null) {
            try (ObjectOutputStream fOUT = new ObjectOutputStream(new FileOutputStream(filePath + "passwords.psmg"))) {
                fOUT.writeObject(this.loginAccount);
                fOUT.writeObject(this.accountList);
                fOUT.flush();
            } catch (IOException e) {
                logger.addError(e);
            }

            logger.addInfo("Files saved and program shutted down");
            logger.save();
        }
    }

    // #region Exporters
    private void htmlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.html")) {
            file.write(Exporter.exportHtml(accountList, language, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }

    private void csvMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.csv")) {
            file.write(Exporter.exportCsv(accountList, language, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }
    }
    // #endregion

    // #region Custom Methods
    /**
     * Redirects to the login or first run procedure, based on the password file
     * existence.
     */
    private void run() {
        File data_file = new File(filePath + "passwords.psmg");
        if (data_file.getParentFile().mkdirs()) {
            logger.addInfo("Created folder " + data_file.getParentFile().getAbsolutePath());
        }

        // if the data file exists, it will try to read its contents
        if (data_file.exists()) {
            try (FileInputStream f = new FileInputStream(data_file)) {
                ObjectInputStream fIN = new ObjectInputStream(f);

                loginAccount = (LoginAccount) fIN.readObject();
                language = loginAccount.getLanguage();

                accountList = (ArrayList<Account>) fIN.readObject();
            } catch (IOException | ClassNotFoundException e) {
                logger.addError(e);
            }
        }

        if (loginAccount != null) {
            // gets the log history
            logger.readFile();

            switch (language) {
                case "e" -> {
                    LoginLabel.setText("Login");
                    LoginButton.setText("Login");
                }

                case "i" -> {
                    LoginLabel.setText("Accesso");
                    LoginButton.setText("Accesso");
                }
            }

            LoginLabel.repaint();
            LoginButton.repaint();

            // redirects to login panel
            Utils.replacePanel(MainPanel, LoginPanel);
        } else {
            FirstRunLanguageSelector.removeAllItems();
            FirstRunLanguageSelector.addItem("");
            FirstRunLanguageSelector.addItem("English");
            FirstRunLanguageSelector.addItem("Italian");
            FirstRunLanguageSelector.repaint();

            FirstRunSavingorderSelector.removeAllItems();
            FirstRunSavingorderSelector.addItem("");
            FirstRunSavingorderSelector.addItem("Software");
            FirstRunSavingorderSelector.addItem("Username");
            FirstRunSavingorderSelector.repaint();

            // redirects to first run panel
            Utils.replacePanel(MainPanel, FirstRunPanel);
        }
    }

    /**
     * Resorts the current account list.
     */
    private void resortAccountList() {
        switch (loginAccount.getSavingOrder()) {
            case "s" -> {
                this.accountList.sort((Account acc1, Account acc2) -> {
                    var software = acc1.getSoftware().compareTo(acc2.getSoftware());
                    var username = acc1.getUsername().compareTo(acc2.getUsername());

                    if (software == 0) {
                        return username;
                    } else {
                        return software;
                    }
                });
            }

            case "u" -> {
                this.accountList.sort((Account acc1, Account acc2) -> {
                    var software = acc1.getSoftware().compareTo(acc2.getSoftware());
                    var username = acc1.getUsername().compareTo(acc2.getUsername());

                    if (username == 0) {
                        return software;
                    } else {
                        return username;
                    }
                });
            }
        }
    }

    /**
     * Returns the selected index in a Combo Box with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @return the index of the current selected index
     */
    private int selectedItemInComboBox(JComboBox<String> comboBox) {
        return (comboBox.getSelectedIndex() - 1);
    }

    /**
     * Updates the DecrypterAccountSelector.
     */
    private void updateDecrypterAccountSelector() {
        // clears the account selector of the decrypter and rewrites it
        DecrypterAccountSelector.removeAllItems();
        DecrypterAccountSelector.addItem("");

        for (int i = 0; i < this.accountList.size(); i++) {
            DecrypterAccountSelector.addItem((i + 1) + ") " + this.accountList.get(i).getSoftware() + " / "
                    + this.accountList.get(i).getUsername());
        }

        DecrypterAccountSelector.repaint();

        // clears the text fields of the decrypter
        DecrypterSoftwareTextField.setText("");
        DecrypterSoftwareTextField.repaint();

        DecrypterUsernameTextField.setText("");
        DecrypterUsernameTextField.repaint();

        DecrypterPasswordTextField.setText("");
        DecrypterPasswordTextField.repaint();
    }
    // #endregion

    // #region Generated Code
    // #region Swing variables declaration
    private javax.swing.JComboBox<String> DecrypterAccountSelector;
    private javax.swing.JButton DecrypterButton;
    private javax.swing.JButton DecrypterDeleteButton;
    private javax.swing.JPanel DecrypterPanel;
    private javax.swing.JTextField DecrypterPasswordTextField;
    private javax.swing.JButton DecrypterSaveButton;
    private javax.swing.JTextField DecrypterSoftwareTextField;
    private javax.swing.JLabel DecrypterUsernameLabel;
    private javax.swing.JTextField DecrypterUsernameTextField;
    private javax.swing.JPanel DialogBlankPanel;
    private javax.swing.JPanel DialogPanel;
    private javax.swing.JButton EncrypterButton;
    private javax.swing.JPanel EncrypterPanel;
    private javax.swing.JTextField EncrypterPasswordTextField;
    private javax.swing.JButton EncrypterSaveButton;
    private javax.swing.JTextField EncrypterSoftwareTextField;
    private javax.swing.JLabel EncrypterUsernameLabel;
    private javax.swing.JTextField EncrypterUsernameTextField;
    private javax.swing.JButton EulaAndCreditsButton;
    private javax.swing.JPanel EulaAndCreditsPanel;
    private javax.swing.JMenu ExportAsMenu;
    private javax.swing.JCheckBox FirstRunAccepteulaCheckBox;
    private javax.swing.JComboBox<String> FirstRunLanguageSelector;
    private javax.swing.JTextField FirstRunLoginpasswordTextField;
    private javax.swing.JPanel FirstRunPanel;
    private javax.swing.JComboBox<String> FirstRunSavingorderSelector;
    private javax.swing.JButton LogHistoryButton;
    private javax.swing.JPanel LogHistoryPanel;
    private javax.swing.JTextArea LogHistoryTextArea;
    private javax.swing.JLabel LogHystoryLabel;
    private javax.swing.JLabel LogLegendLabel;
    private javax.swing.JButton LoginButton;
    private javax.swing.JLabel LoginLabel;
    private javax.swing.JPanel LoginPanel;
    private javax.swing.JPasswordField LoginPasswordField;
    private javax.swing.JLabel LoginUnsuccesfulLabel;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JPanel ProgramPanel;
    private javax.swing.JButton SettingsButton;
    private javax.swing.JLabel SettingsLanguageLabel;
    private javax.swing.JComboBox<String> SettingsLanguageSelector;
    private javax.swing.JLabel SettingsLoginpassowrdLabel;
    private javax.swing.JTextField SettingsLoginPasswordTextField;
    private javax.swing.JPanel SettingsPanel;
    private javax.swing.JButton SettingsSaveButton;
    private javax.swing.JLabel SettingsSavingorderLabel;
    private javax.swing.JComboBox<String> SettingsSavingorderSelector;
    // #endregion

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private void initComponents() {
        MainPanel = new javax.swing.JPanel();
        FirstRunPanel = new javax.swing.JPanel();
        FirstRunLanguageSelector = new javax.swing.JComboBox<>();
        javax.swing.JLabel firstRunLanguageLabel = new javax.swing.JLabel();
        javax.swing.JLabel firstRunSavingorderLabel = new javax.swing.JLabel();
        javax.swing.JLabel firstRunLoginpassowrdLabel = new javax.swing.JLabel();
        FirstRunLoginpasswordTextField = new javax.swing.JTextField();
        javax.swing.JSeparator firstRunSeparator = new javax.swing.JSeparator();
        javax.swing.JButton firstRunConfirmButton = new javax.swing.JButton();
        javax.swing.JLabel firstRunLabel = new javax.swing.JLabel();
        javax.swing.JLabel firstRunLabel2 = new javax.swing.JLabel();
        FirstRunSavingorderSelector = new javax.swing.JComboBox<>();
        javax.swing.JSeparator firstRunSeparator1 = new javax.swing.JSeparator();
        FirstRunAccepteulaCheckBox = new javax.swing.JCheckBox();
        javax.swing.JTextArea firstRunEULATextArea = new javax.swing.JTextArea();
        LoginPanel = new javax.swing.JPanel();
        LoginLabel = new javax.swing.JLabel();
        javax.swing.JLabel loginPasswordLabel = new javax.swing.JLabel();
        LoginPasswordField = new javax.swing.JPasswordField();
        LoginButton = new javax.swing.JButton();
        LoginUnsuccesfulLabel = new javax.swing.JLabel();
        ProgramPanel = new javax.swing.JPanel();
        DialogPanel = new javax.swing.JPanel();
        DialogBlankPanel = new javax.swing.JPanel();
        EncrypterPanel = new javax.swing.JPanel();
        javax.swing.JLabel encrypterSoftwareLabel = new javax.swing.JLabel();
        EncrypterSoftwareTextField = new javax.swing.JTextField();
        EncrypterUsernameLabel = new javax.swing.JLabel();
        EncrypterUsernameTextField = new javax.swing.JTextField();
        javax.swing.JLabel encrypterPasswordLabel = new javax.swing.JLabel();
        EncrypterPasswordTextField = new javax.swing.JTextField();
        EncrypterSaveButton = new javax.swing.JButton();
        DecrypterPanel = new javax.swing.JPanel();
        DecrypterAccountSelector = new javax.swing.JComboBox<>();
        javax.swing.JLabel decrypterSoftwareLabel = new javax.swing.JLabel();
        DecrypterUsernameLabel = new javax.swing.JLabel();
        javax.swing.JLabel decrypterPasswordLabel = new javax.swing.JLabel();
        DecrypterPasswordTextField = new javax.swing.JTextField();
        DecrypterUsernameTextField = new javax.swing.JTextField();
        DecrypterSoftwareTextField = new javax.swing.JTextField();
        javax.swing.JSeparator decrypterSeparator = new javax.swing.JSeparator();
        DecrypterSaveButton = new javax.swing.JButton();
        DecrypterDeleteButton = new javax.swing.JButton();
        SettingsPanel = new javax.swing.JPanel();
        SettingsLanguageSelector = new javax.swing.JComboBox<>();
        SettingsLanguageLabel = new javax.swing.JLabel();
        SettingsSavingorderLabel = new javax.swing.JLabel();
        SettingsLoginpassowrdLabel = new javax.swing.JLabel();
        SettingsLoginPasswordTextField = new javax.swing.JTextField();
        SettingsSaveButton = new javax.swing.JButton();
        SettingsSavingorderSelector = new javax.swing.JComboBox<>();
        LogHistoryPanel = new javax.swing.JPanel();
        LogHystoryLabel = new javax.swing.JLabel();
        javax.swing.JScrollPane logHistoryScrollPane = new javax.swing.JScrollPane();
        LogHistoryTextArea = new javax.swing.JTextArea();
        LogLegendLabel = new javax.swing.JLabel();
        EulaAndCreditsPanel = new javax.swing.JPanel();
        javax.swing.JLabel creditsLabel = new javax.swing.JLabel();
        javax.swing.JTextArea creditsTextArea = new javax.swing.JTextArea();
        javax.swing.JLabel EULALabel = new javax.swing.JLabel();
        javax.swing.JTextArea EULATextArea = new javax.swing.JTextArea();
        // Variables declaration - do not modify
        JPanel butttonPanel = new JPanel();
        javax.swing.JLabel passwordManagerLabel = new javax.swing.JLabel();
        EncrypterButton = new javax.swing.JButton();
        DecrypterButton = new javax.swing.JButton();
        SettingsButton = new javax.swing.JButton();
        LogHistoryButton = new javax.swing.JButton();
        EulaAndCreditsButton = new javax.swing.JButton();
        MenuBar = new javax.swing.JMenuBar();
        ExportAsMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem htmlMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem csvMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Password Manager");
        setBackground(new java.awt.Color(0, 0, 0));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setFocusCycleRoot(false);
        setFont(new java.awt.Font("Dialog", Font.BOLD, 10)); // NOI18N
        setForeground(java.awt.Color.lightGray);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing();
            }
        });

        MainPanel.setBackground(new java.awt.Color(38, 38, 38));
        MainPanel.setForeground(new java.awt.Color(242, 242, 242));
        MainPanel.setLayout(new java.awt.CardLayout());

        FirstRunPanel.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunPanel.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunPanel.setToolTipText("");

        FirstRunLanguageSelector.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunLanguageSelector.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunLanguageSelector.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FirstRunLanguageSelector.setBorder(null);

        firstRunLanguageLabel.setBackground(new java.awt.Color(38, 38, 38));
        firstRunLanguageLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunLanguageLabel.setForeground(new java.awt.Color(242, 242, 242));
        firstRunLanguageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        firstRunLanguageLabel.setText("Language:");

        firstRunSavingorderLabel.setBackground(new java.awt.Color(38, 38, 38));
        firstRunSavingorderLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunSavingorderLabel.setForeground(new java.awt.Color(242, 242, 242));
        firstRunSavingorderLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        firstRunSavingorderLabel.setText("Saving order:");

        firstRunLoginpassowrdLabel.setBackground(new java.awt.Color(38, 38, 38));
        firstRunLoginpassowrdLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunLoginpassowrdLabel.setForeground(new java.awt.Color(242, 242, 242));
        firstRunLoginpassowrdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        firstRunLoginpassowrdLabel.setText("Login password:");

        FirstRunLoginpasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        FirstRunLoginpasswordTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        FirstRunLoginpasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        FirstRunLoginpasswordTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        firstRunSeparator.setBackground(new java.awt.Color(38, 38, 38));
        firstRunSeparator.setForeground(new java.awt.Color(242, 242, 242));

        firstRunConfirmButton.setBackground(new java.awt.Color(38, 38, 38));
        firstRunConfirmButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunConfirmButton.setForeground(new java.awt.Color(242, 242, 242));
        firstRunConfirmButton.setText("Confirm");
        firstRunConfirmButton.setOpaque(false);
        firstRunConfirmButton.addActionListener(this::FirstRunConfirmButtonActionPerformed);

        firstRunLabel.setBackground(new java.awt.Color(38, 38, 38));
        firstRunLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunLabel.setForeground(new java.awt.Color(242, 242, 242));
        firstRunLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        firstRunLabel.setText("Hi!");

        firstRunLabel2.setBackground(new java.awt.Color(38, 38, 38));
        firstRunLabel2.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        firstRunLabel2.setForeground(new java.awt.Color(242, 242, 242));
        firstRunLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        firstRunLabel2.setText(
                "It's the developer here, before using this program i need you to insert some personalization informations and a password to protect your accounts.");

        FirstRunSavingorderSelector.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunSavingorderSelector.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunSavingorderSelector.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FirstRunSavingorderSelector.setBorder(null);

        firstRunSeparator1.setBackground(new java.awt.Color(38, 38, 38));
        firstRunSeparator1.setForeground(new java.awt.Color(242, 242, 242));

        FirstRunAccepteulaCheckBox.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunAccepteulaCheckBox.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunAccepteulaCheckBox.setText("Accept EULA");

        firstRunEULATextArea.setEditable(false);
        firstRunEULATextArea.setBackground(new java.awt.Color(242, 65, 65));
        firstRunEULATextArea.setColumns(20);
        firstRunEULATextArea.setFont(new java.awt.Font("Dialog", Font.PLAIN, 24)); // NOI18N
        firstRunEULATextArea.setForeground(new java.awt.Color(38, 38, 38));
        firstRunEULATextArea.setRows(5);
        firstRunEULATextArea.setText(
                "Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License \nas published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\nThis program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied \nwarranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for \nmore details.\n\nYou should have received a copy of the GNU General Public License along with this program.\nIf not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        firstRunEULATextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        javax.swing.GroupLayout FirstRunPanelLayout = new javax.swing.GroupLayout(FirstRunPanel);
        FirstRunPanel.setLayout(FirstRunPanelLayout);
        FirstRunPanelLayout.setHorizontalGroup(
                FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FirstRunPanelLayout
                                .createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(FirstRunAccepteulaCheckBox,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 102,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(firstRunConfirmButton,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 102,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(firstRunSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(firstRunSeparator, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                                .addGroup(FirstRunPanelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                false)
                                                        .addComponent(firstRunSavingorderLabel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(firstRunLoginpassowrdLabel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(firstRunLanguageLabel,
                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(18, 18, 18)
                                                .addGroup(FirstRunPanelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(FirstRunLoginpasswordTextField)
                                                        .addComponent(FirstRunLanguageSelector, 0,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(FirstRunSavingorderSelector, 0,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addComponent(firstRunLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(firstRunLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 1307,
                                                Short.MAX_VALUE)
                                        .addComponent(firstRunEULATextArea))
                                .addGap(50, 50, 50)));
        FirstRunPanelLayout.setVerticalGroup(
                FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(firstRunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(firstRunLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(firstRunSeparator, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(firstRunLanguageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FirstRunLanguageSelector, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                32, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(17, 17, 17)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(firstRunSavingorderLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FirstRunSavingorderSelector,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(FirstRunLoginpasswordTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(firstRunLoginpassowrdLabel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(firstRunSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(firstRunEULATextArea, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(firstRunConfirmButton)
                                        .addComponent(FirstRunAccepteulaCheckBox,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(50, 50, 50)));

        MainPanel.add(FirstRunPanel, "card2");

        LoginPanel.setBackground(new java.awt.Color(38, 38, 38));
        LoginPanel.setForeground(new java.awt.Color(242, 242, 242));

        LoginLabel.setBackground(new java.awt.Color(38, 38, 38));
        LoginLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        LoginLabel.setForeground(new java.awt.Color(242, 242, 242));
        LoginLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        LoginLabel.setText("Login");
        LoginLabel.setToolTipText("");
        LoginLabel.setEnabled(false);

        loginPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        loginPasswordLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        loginPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        loginPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        loginPasswordLabel.setText("Password:");

        LoginPasswordField.setBackground(new java.awt.Color(242, 65, 65));
        LoginPasswordField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginPasswordField.setForeground(new java.awt.Color(38, 38, 38));
        LoginPasswordField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        LoginButton.setBackground(new java.awt.Color(38, 38, 38));
        LoginButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginButton.setForeground(new java.awt.Color(242, 242, 242));
        LoginButton.setText("Login");
        LoginButton.setOpaque(false);
        LoginButton.addActionListener(this::LoginButtonActionPerformed);

        LoginUnsuccesfulLabel.setBackground(new java.awt.Color(38, 38, 38));
        LoginUnsuccesfulLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginUnsuccesfulLabel.setForeground(new java.awt.Color(242, 242, 242));

        javax.swing.GroupLayout LoginPanelLayout = new javax.swing.GroupLayout(LoginPanel);
        LoginPanel.setLayout(LoginPanelLayout);
        LoginPanelLayout.setHorizontalGroup(
                LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(LoginButton, javax.swing.GroupLayout.DEFAULT_SIZE, 126,
                                                Short.MAX_VALUE)
                                        .addComponent(loginPasswordLabel, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(LoginLabel, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                                .addGap(7, 7, 7)
                                                .addComponent(LoginPasswordField,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 590,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                LoginPanelLayout.createSequentialGroup()
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(LoginUnsuccesfulLabel,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 590,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(490, 490, 490)));
        LoginPanelLayout.setVerticalGroup(
                LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(LoginLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(loginPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LoginPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(LoginUnsuccesfulLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LoginButton))
                                .addGap(265, 265, 265)));

        MainPanel.add(LoginPanel, "card3");

        ProgramPanel.setBackground(new java.awt.Color(242, 65, 65));
        ProgramPanel.setForeground(new java.awt.Color(242, 242, 242));
        ProgramPanel.setPreferredSize(new java.awt.Dimension(1280, 720));

        DialogPanel.setBackground(new java.awt.Color(38, 38, 38));
        DialogPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(115, 41, 41), 2, true));
        DialogPanel.setForeground(new java.awt.Color(242, 242, 242));
        DialogPanel.setLayout(new java.awt.CardLayout());

        DialogBlankPanel.setBackground(new java.awt.Color(38, 38, 38));
        DialogBlankPanel.setForeground(new java.awt.Color(242, 242, 242));
        DialogBlankPanel.setToolTipText("");

        javax.swing.GroupLayout DialogBlankPanelLayout = new javax.swing.GroupLayout(DialogBlankPanel);
        DialogBlankPanel.setLayout(DialogBlankPanelLayout);
        DialogBlankPanelLayout.setHorizontalGroup(
                DialogBlankPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 1145, Short.MAX_VALUE));
        DialogBlankPanelLayout.setVerticalGroup(
                DialogBlankPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 879, Short.MAX_VALUE));

        DialogPanel.add(DialogBlankPanel, "card2");

        EncrypterPanel.setBackground(new java.awt.Color(38, 38, 38));
        EncrypterPanel.setForeground(new java.awt.Color(242, 242, 242));
        EncrypterPanel.setToolTipText("");

        encrypterSoftwareLabel.setBackground(new java.awt.Color(38, 38, 38));
        encrypterSoftwareLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        encrypterSoftwareLabel.setForeground(new java.awt.Color(242, 242, 242));
        encrypterSoftwareLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        encrypterSoftwareLabel.setText("Software:");

        EncrypterSoftwareTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncrypterSoftwareTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        EncrypterSoftwareTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncrypterSoftwareTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EncrypterUsernameLabel.setBackground(new java.awt.Color(38, 38, 38));
        EncrypterUsernameLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        EncrypterUsernameLabel.setForeground(new java.awt.Color(242, 242, 242));
        EncrypterUsernameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EncrypterUsernameLabel.setText("Username:");

        EncrypterUsernameTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncrypterUsernameTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        EncrypterUsernameTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncrypterUsernameTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        encrypterPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        encrypterPasswordLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        encrypterPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        encrypterPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        encrypterPasswordLabel.setText("Password:");

        EncrypterPasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncrypterPasswordTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        EncrypterPasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncrypterPasswordTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EncrypterSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        EncrypterSaveButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        EncrypterSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        EncrypterSaveButton.setText("Save");
        EncrypterSaveButton.setOpaque(false);
        EncrypterSaveButton.addActionListener(this::EncrypterSaveButtonActionPerformed);

        javax.swing.GroupLayout EncrypterPanelLayout = new javax.swing.GroupLayout(EncrypterPanel);
        EncrypterPanel.setLayout(EncrypterPanelLayout);
        EncrypterPanelLayout.setHorizontalGroup(
                EncrypterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(EncrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(encrypterPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(encrypterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(EncrypterUsernameLabel,
                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                                        .addComponent(EncrypterSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(EncrypterSoftwareTextField,
                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
                                        .addComponent(EncrypterPasswordTextField)
                                        .addComponent(EncrypterUsernameTextField,
                                                javax.swing.GroupLayout.Alignment.TRAILING))
                                .addGap(50, 50, 50)));
        EncrypterPanelLayout.setVerticalGroup(
                EncrypterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(EncrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(encrypterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 31,
                                                Short.MAX_VALUE)
                                        .addComponent(EncrypterSoftwareTextField))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(EncrypterUsernameTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(EncrypterUsernameLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(EncrypterPasswordTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(encrypterPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(EncrypterSaveButton)
                                .addGap(473, 473, 473)));

        DialogPanel.add(EncrypterPanel, "card2");

        DecrypterPanel.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterPanel.setForeground(new java.awt.Color(242, 242, 242));

        DecrypterAccountSelector.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterAccountSelector.setForeground(new java.awt.Color(242, 242, 242));
        DecrypterAccountSelector.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        DecrypterAccountSelector.addActionListener(this::DecrypterAccountSelectorActionPerformed);

        decrypterSoftwareLabel.setBackground(new java.awt.Color(38, 38, 38));
        decrypterSoftwareLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        decrypterSoftwareLabel.setForeground(new java.awt.Color(242, 242, 242));
        decrypterSoftwareLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        decrypterSoftwareLabel.setText("Software:");

        DecrypterUsernameLabel.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterUsernameLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterUsernameLabel.setForeground(new java.awt.Color(242, 242, 242));
        DecrypterUsernameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        DecrypterUsernameLabel.setText("Username:");

        decrypterPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        decrypterPasswordLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        decrypterPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        decrypterPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        decrypterPasswordLabel.setText("Password:");

        DecrypterPasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecrypterPasswordTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterPasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecrypterPasswordTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        DecrypterUsernameTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecrypterUsernameTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterUsernameTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecrypterUsernameTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        DecrypterSoftwareTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecrypterSoftwareTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterSoftwareTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecrypterSoftwareTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        decrypterSeparator.setBackground(new java.awt.Color(38, 38, 38));
        decrypterSeparator.setForeground(new java.awt.Color(242, 242, 242));

        DecrypterSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterSaveButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        DecrypterSaveButton.setText("Save");
        DecrypterSaveButton.setOpaque(false);
        DecrypterSaveButton.addActionListener(this::DecrypterSaveButtonActionPerformed);

        DecrypterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterDeleteButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DecrypterDeleteButton.setForeground(new java.awt.Color(242, 242, 242));
        DecrypterDeleteButton.setText("Delete");
        DecrypterDeleteButton.setOpaque(false);
        DecrypterDeleteButton.addActionListener(this::DecrypterDeleteButtonActionPerformed);

        javax.swing.GroupLayout DecrypterPanelLayout = new javax.swing.GroupLayout(DecrypterPanel);
        DecrypterPanel.setLayout(DecrypterPanelLayout);
        DecrypterPanelLayout.setHorizontalGroup(
                DecrypterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(decrypterSeparator)
                                        .addComponent(DecrypterAccountSelector,
                                                javax.swing.GroupLayout.Alignment.LEADING, 0,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, DecrypterPanelLayout
                                                .createSequentialGroup()
                                                .addGroup(DecrypterPanelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(decrypterPasswordLabel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(decrypterSoftwareLabel,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(DecrypterUsernameLabel,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 107,
                                                                Short.MAX_VALUE)
                                                        .addComponent(DecrypterDeleteButton,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(18, 18, 18)
                                                .addGroup(DecrypterPanelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(DecrypterPasswordTextField,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 920,
                                                                Short.MAX_VALUE)
                                                        .addComponent(DecrypterSoftwareTextField)
                                                        .addComponent(DecrypterUsernameTextField,
                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                .addComponent(DecrypterSaveButton,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 107,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))))
                                .addGap(50, 50, 50)));
        DecrypterPanelLayout.setVerticalGroup(
                DecrypterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(DecrypterAccountSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(decrypterSeparator, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(decrypterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 31,
                                                Short.MAX_VALUE)
                                        .addComponent(DecrypterSoftwareTextField))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(DecrypterUsernameTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(DecrypterUsernameLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(DecrypterPasswordTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(decrypterPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(DecrypterDeleteButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(DecrypterSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(50, 50, 50)));

        DialogPanel.add(DecrypterPanel, "card2");

        SettingsPanel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsPanel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsPanel.setToolTipText("");

        SettingsLanguageSelector.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLanguageSelector.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLanguageSelector.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        SettingsLanguageLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLanguageLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsLanguageLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLanguageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsLanguageLabel.setText("Language:");

        SettingsSavingorderLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSavingorderLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsSavingorderLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSavingorderLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsSavingorderLabel.setText("Saving order:");

        SettingsLoginpassowrdLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLoginpassowrdLabel.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsLoginpassowrdLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLoginpassowrdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsLoginpassowrdLabel.setText("Login password:");

        SettingsLoginPasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        SettingsLoginPasswordTextField.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsLoginPasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        SettingsLoginPasswordTextField
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        SettingsSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSaveButton.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSaveButton.setText("Confirm");
        SettingsSaveButton.setOpaque(false);
        SettingsSaveButton.addActionListener(this::SettingsSaveButtonActionPerformed);

        SettingsSavingorderSelector.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSavingorderSelector.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSavingorderSelector.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout SettingsPanelLayout = new javax.swing.GroupLayout(SettingsPanel);
        SettingsPanel.setLayout(SettingsPanelLayout);
        SettingsPanelLayout.setHorizontalGroup(
                SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(SettingsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(SettingsSavingorderLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SettingsSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SettingsLanguageLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SettingsLoginpassowrdLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                168, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(SettingsSavingorderSelector,
                                                javax.swing.GroupLayout.Alignment.TRAILING, 0, 859, Short.MAX_VALUE)
                                        .addComponent(SettingsLoginPasswordTextField,
                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(SettingsLanguageSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE))
                                .addGap(50, 50, 50)));
        SettingsPanelLayout.setVerticalGroup(
                SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(SettingsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(SettingsLanguageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SettingsLanguageSelector, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                32, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(SettingsSavingorderLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SettingsSavingorderSelector,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(SettingsLoginPasswordTextField,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SettingsLoginpassowrdLabel,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(SettingsSaveButton)
                                .addGap(50, 50, 50)));

        DialogPanel.add(SettingsPanel, "card2");

        LogHistoryPanel.setBackground(new java.awt.Color(38, 38, 38));
        LogHistoryPanel.setForeground(new java.awt.Color(242, 242, 242));
        LogHistoryPanel.setToolTipText("");

        LogHystoryLabel.setBackground(new java.awt.Color(38, 38, 38));
        LogHystoryLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        LogHystoryLabel.setForeground(new java.awt.Color(242, 242, 242));
        LogHystoryLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        LogHystoryLabel.setText("Log History:");
        LogHystoryLabel.setInheritsPopupMenu(false);
        LogHystoryLabel.setName(""); // NOI18N

        logHistoryScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        LogHistoryTextArea.setEditable(false);
        LogHistoryTextArea.setBackground(new java.awt.Color(242, 65, 65));
        LogHistoryTextArea.setColumns(50);
        LogHistoryTextArea.setFont(new java.awt.Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LogHistoryTextArea.setForeground(new java.awt.Color(38, 38, 38));
        LogHistoryTextArea.setRows(5);
        LogHistoryTextArea.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(115, 41, 41), 1, true));
        LogHistoryTextArea.setCaretColor(new java.awt.Color(102, 102, 102));
        logHistoryScrollPane.setViewportView(LogHistoryTextArea);

        LogLegendLabel.setBackground(new java.awt.Color(38, 38, 38));
        LogLegendLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        LogLegendLabel.setForeground(new java.awt.Color(242, 242, 242));
        LogLegendLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        LogLegendLabel.setText("[Actions]    {Errors}");
        LogLegendLabel.setInheritsPopupMenu(false);
        LogLegendLabel.setName(""); // NOI18N

        javax.swing.GroupLayout LogHistoryPanelLayout = new javax.swing.GroupLayout(LogHistoryPanel);
        LogHistoryPanel.setLayout(LogHistoryPanelLayout);
        LogHistoryPanelLayout.setHorizontalGroup(
                LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LogHistoryPanelLayout
                                .createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LogHistoryPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                                                .addComponent(LogHystoryLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        574, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(LogLegendLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        465, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(logHistoryScrollPane))
                                .addGap(50, 50, 50)));
        LogHistoryPanelLayout.setVerticalGroup(
                LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LogHistoryPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(LogHystoryLabel)
                                        .addComponent(LogLegendLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(logHistoryScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 726,
                                        Short.MAX_VALUE)
                                .addGap(50, 50, 50)));

        DialogPanel.add(LogHistoryPanel, "card2");

        EulaAndCreditsPanel.setBackground(new java.awt.Color(38, 38, 38));
        EulaAndCreditsPanel.setForeground(new java.awt.Color(242, 242, 242));
        EulaAndCreditsPanel.setToolTipText("");

        creditsLabel.setBackground(new java.awt.Color(38, 38, 38));
        creditsLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        creditsLabel.setForeground(new java.awt.Color(242, 242, 242));
        creditsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        creditsLabel.setLabelFor(creditsTextArea);
        creditsLabel.setText("Credits:");

        creditsTextArea.setEditable(false);
        creditsTextArea.setBackground(new java.awt.Color(242, 65, 65));
        creditsTextArea.setColumns(20);
        creditsTextArea.setFont(new java.awt.Font("Dialog", Font.PLAIN, 24)); // NOI18N
        creditsTextArea.setForeground(new java.awt.Color(38, 38, 38));
        creditsTextArea.setRows(5);
        creditsTextArea.setText("Creator and developer: Francesco Marras.\nSpecial thanks to \"Luca\".\n\n");
        creditsTextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EULALabel.setBackground(new java.awt.Color(38, 38, 38));
        EULALabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        EULALabel.setForeground(new java.awt.Color(242, 242, 242));
        EULALabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        EULALabel.setLabelFor(EULATextArea);
        EULALabel.setText("EULA:");

        EULATextArea.setEditable(false);
        EULATextArea.setBackground(new java.awt.Color(242, 65, 65));
        EULATextArea.setColumns(20);
        EULATextArea.setFont(new java.awt.Font("Dialog", Font.PLAIN, 24)); // NOI18N
        EULATextArea.setForeground(new java.awt.Color(38, 38, 38));
        EULATextArea.setRows(5);
        EULATextArea.setText(
                "Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        EULATextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        javax.swing.GroupLayout EulaAndCreditsPanelLayout = new javax.swing.GroupLayout(EulaAndCreditsPanel);
        EulaAndCreditsPanel.setLayout(EulaAndCreditsPanelLayout);
        EulaAndCreditsPanelLayout.setHorizontalGroup(
                EulaAndCreditsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EulaAndCreditsPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(creditsTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 1045,
                                                Short.MAX_VALUE)
                                        .addComponent(EULALabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(creditsLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(EULATextArea, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 1045, Short.MAX_VALUE))
                                .addGap(50, 50, 50)));
        EulaAndCreditsPanelLayout.setVerticalGroup(
                EulaAndCreditsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(creditsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(creditsTextArea, javax.swing.GroupLayout.PREFERRED_SIZE, 66,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(EULALabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(EULATextArea, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(50, 50, 50)));

        DialogPanel.add(EulaAndCreditsPanel, "card6");

        butttonPanel.setBackground(new java.awt.Color(38, 38, 38));
        butttonPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(115, 41, 41), 2, true));
        butttonPanel.setForeground(new java.awt.Color(242, 242, 242));
        butttonPanel.setPreferredSize(new java.awt.Dimension(235, 242));

        passwordManagerLabel.setBackground(new java.awt.Color(38, 38, 38));
        passwordManagerLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 36)); // NOI18N
        passwordManagerLabel.setForeground(new java.awt.Color(242, 242, 242));
        passwordManagerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        passwordManagerLabel.setText("<html>Password<br>Manager</html>");
        passwordManagerLabel.setToolTipText("");
        passwordManagerLabel.setEnabled(false);

        EncrypterButton.setBackground(new java.awt.Color(38, 38, 38));
        EncrypterButton.setFont(new java.awt.Font("Dialog", Font.BOLD, 18)); // NOI18N
        EncrypterButton.setForeground(new java.awt.Color(242, 242, 242));
        EncrypterButton.setMnemonic('1');
        EncrypterButton.setText("Encrypter");
        EncrypterButton.addActionListener(this::EncrypterButtonActionPerformed);

        DecrypterButton.setBackground(new java.awt.Color(38, 38, 38));
        DecrypterButton.setFont(new java.awt.Font("Dialog", Font.BOLD, 18)); // NOI18N
        DecrypterButton.setForeground(new java.awt.Color(242, 242, 242));
        DecrypterButton.setMnemonic('2');
        DecrypterButton.setText("Decrypter");
        DecrypterButton.addActionListener(this::DecrypterButtonActionPerformed);

        SettingsButton.setBackground(new java.awt.Color(38, 38, 38));
        SettingsButton.setFont(new java.awt.Font("Dialog", Font.BOLD, 18)); // NOI18N
        SettingsButton.setForeground(new java.awt.Color(242, 242, 242));
        SettingsButton.setText("Settings");
        SettingsButton.addActionListener(this::SettingsButtonActionPerformed);

        LogHistoryButton.setBackground(new java.awt.Color(38, 38, 38));
        LogHistoryButton.setFont(new java.awt.Font("Dialog", Font.BOLD, 12)); // NOI18N
        LogHistoryButton.setForeground(new java.awt.Color(242, 242, 242));
        LogHistoryButton.setText("Log History");
        LogHistoryButton.addActionListener(this::LogHistoryButtonActionPerformed);

        EulaAndCreditsButton.setBackground(new java.awt.Color(38, 38, 38));
        EulaAndCreditsButton.setFont(new java.awt.Font("Dialog", Font.BOLD, 12)); // NOI18N
        EulaAndCreditsButton.setForeground(new java.awt.Color(242, 242, 242));
        EulaAndCreditsButton.setText("EULA and Credits");
        EulaAndCreditsButton.addActionListener(this::EulaAndCreditsButtonActionPerformed);

        javax.swing.GroupLayout ButttonPanelLayout = new javax.swing.GroupLayout(butttonPanel);
        butttonPanel.setLayout(ButttonPanelLayout);
        ButttonPanelLayout.setHorizontalGroup(
                ButttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(passwordManagerLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(EncrypterButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(DecrypterButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(SettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(LogHistoryButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(51, 51, 51))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(EulaAndCreditsButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(51, 51, 51)));
        ButttonPanelLayout.setVerticalGroup(
                ButttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(passwordManagerLabel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(EncrypterButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(DecrypterButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(SettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(LogHistoryButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(EulaAndCreditsButton, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(7, 7, 7)));

        passwordManagerLabel.getAccessibleContext().setAccessibleName("");

        javax.swing.GroupLayout ProgramPanelLayout = new javax.swing.GroupLayout(ProgramPanel);
        ProgramPanel.setLayout(ProgramPanelLayout);
        ProgramPanelLayout.setHorizontalGroup(
                ProgramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ProgramPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(butttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 240,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(DialogPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(5, 5, 5)));
        ProgramPanelLayout.setVerticalGroup(
                ProgramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ProgramPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(ProgramPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(butttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 883,
                                                Short.MAX_VALUE)
                                        .addComponent(DialogPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(5, 5, 5)));

        MainPanel.add(ProgramPanel, "card2");

        MenuBar.setBackground(new java.awt.Color(38, 38, 38));
        MenuBar.setForeground(new java.awt.Color(242, 242, 242));

        ExportAsMenu.setText("Export as");

        htmlMenuItem.setText("HTML");
        htmlMenuItem.addActionListener(this::htmlMenuItemActionPerformed);
        ExportAsMenu.add(htmlMenuItem);

        csvMenuItem.setText("CSV");
        csvMenuItem.addActionListener(this::csvMenuItemActionPerformed);
        ExportAsMenu.add(csvMenuItem);

        MenuBar.add(ExportAsMenu);

        setJMenuBar(MenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        pack();
    }
    // #endregion
}

/*
 * Colors in Hex
 * #8C8A87
 * #F24141
 * #732929;
 * #F2F2F2;
 * #262626;
 */