/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2023  Francesco Marras

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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.*;

import main.security.*;
import main.utils.*;
import static main.utils.Utils.*;
import main.views.*;

/**
 * Main class.
 *
 * @version 2.0
 * @author 2004marras@gmail.com
 */
public class Main extends JFrame {

    private final Logger logger;

    private LoginAccount loginAccount;
    private ArrayList<Account> accountList;
    private final String filePath;

    private String loginPassword;

    /**
     * Constructor method, initializes objects and gets the data filepath, then
     * runs the program.
     */
    public Main() {
        initComponents();

        // initialize objects
        accountList = new ArrayList<>();

        MenuBar.setVisible(false);

        // gets the filepath
        boolean isWindows = System.getProperty("os.name").contains("Windows");
        filePath = isWindows ? System.getProperty("user.home") + "\\AppData\\Local\\Password Manager\\" : "";

        logger = new Logger(filePath);

        run();
    }

    // #region GUI methods
    public void EncrypterButtonActionPerformed(ActionEvent evt) {
        switch (loginAccount.getLanguage()) {
            case "e" -> EncrypterPanel.load("Save", "Username:");
            case "i" -> EncrypterPanel.load("Salva", "Nome utente:");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to encrypter panel
        replaceToDialogPanel(EncrypterPanel);
    }

    public void DecrypterButtonActionPerformed(ActionEvent evt) {
        switch (loginAccount.getLanguage()) {
            case "e" -> DecrypterPanel.load("Delete", "Save", "Username");
            case "i" -> DecrypterPanel.load("Elimina", "Salva", "Nome utente");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to decrypter panel
        replaceToDialogPanel(DecrypterPanel);
    }

    public void SettingsButtonActionPerformed(ActionEvent evt) {
        String[] languageSelectorItems, savingOrderSelectorItems;
        String languageLabelText, savingOrderLabelText, loginPasswordLabelText;
        String confirmButtonText;

        switch (loginAccount.getLanguage()) {
            case "e" -> {
                languageSelectorItems = new String[] { "English", "Italian" };
                savingOrderSelectorItems = new String[] { "Software", "Username" };
                languageLabelText = "Language";
                savingOrderLabelText = "Saving order";
                loginPasswordLabelText = "Login password";
                confirmButtonText = "Confirm";
            }

            case "i" -> {
                languageSelectorItems = new String[] { "Inglese", "Italiano" };
                savingOrderSelectorItems = new String[] { "Software", "Nome utente" };
                languageLabelText = "Linguaggio";
                savingOrderLabelText = "Ordine di salvataggio";
                loginPasswordLabelText = "Password d'accesso";
                confirmButtonText = "Conferma";
            }

            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        SettingsPanel.load(languageSelectorItems, savingOrderSelectorItems, languageLabelText, savingOrderLabelText,
                loginPasswordLabelText, confirmButtonText, loginPassword);

        // redirects to settings panel
        replaceToDialogPanel(SettingsPanel);
    }

    public void LogHistoryButtonActionPerformed(ActionEvent evt) {
        logger.addInfo("Log history showed");

        switch (loginAccount.getLanguage()) {
            case "e" -> LogHistoryPanel.load("Log History", "[Actions]    {Errors}");
            case "i" -> LogHistoryPanel.load("Cronologia del Registro", "[Azioni]    {Errori}");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to lohg history panel
        replaceToDialogPanel(LogHistoryPanel);
    }

    public void EulaAndCreditsButtonActionPerformed(ActionEvent evt) {
        logger.addInfo("EULA and Credits showed");

        // redirects to eula and credits panel
        replaceToDialogPanel(EulaAndCreditsPanel);
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
    // #endregion

    // #region Exporters
    private void htmlMenuItemActionPerformed(ActionEvent evt) {
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.html")) {
            file.write(Exporter.exportHtml(accountList, loginAccount.getLanguage(), loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }

        ExportAsMenu.dispatchEvent(new KeyEvent(ExportAsMenu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, '←'));
    }

    private void csvMenuItemActionPerformed(ActionEvent evt) {
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.csv")) {
            file.write(Exporter.exportCsv(accountList, loginPassword));
            file.flush();
        } catch (IOException e) {
            logger.addError(e);
        }

        ExportAsMenu.dispatchEvent(new KeyEvent(ExportAsMenu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, '←'));
    }
    // #endregion

    // #region Custom Methods
    /**
     * Redirects to the login or first run procedure, based on the password file
     * existence.
     */
    @SuppressWarnings("unchecked")
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
                accountList = (ArrayList<Account>) fIN.readObject();
            } catch (IOException | ClassNotFoundException e) {
                logger.addError(e);
            }
        }

        if (loginAccount != null) {
            // gets the log history
            logger.readFile();

            switch (loginAccount.getLanguage()) {
                case "e" -> LoginPanel.load("Login", "Login");
                case "i" -> LoginPanel.load("Accedi", "Accesso");
                default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
            }

            // redirects to login panel
            replaceToMainPanel(LoginPanel);
        } else {
            // redirects to first run panel
            replaceToMainPanel(FirstRunPanel);
        }
    }

    /**
     * Sorts the account list.
     */
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

    public void addAccount(String software, String username, String password) {
        accountList.add(Account.of(software, username, password, loginPassword));
        sortAccountList();

        logger.addInfo("Account added");

        MenuBar.setVisible(true);
    }

    public void replaceAccount(int index, String software, String username, String password) {
        if (index >= 0 && index < accountList.size()) {
            accountList.set(index, Account.of(software, username, password, loginPassword));
            sortAccountList();

            logger.addInfo("Account edited");

            MenuBar.setVisible(true);
        }
    }

    public void deleteAccount(int index) {
        if (index >= 0 && index < accountList.size()) {
            accountList.remove(index);

            logger.addInfo("Account deleted");

            if (accountList.isEmpty()) {
                MenuBar.setVisible(false);
            }
        }
    }

    public String getAccountPassword(Account account) {
        return account.getPassword(loginPassword);
    }

    public Logger getLogger() {
        return logger;
    }

    public LoginAccount getLoginAccount() {
        return this.loginAccount;
    }

    public void setLoginAccount(String savingOrder, String language, String loginPassword) {
        this.loginAccount = LoginAccount.of(savingOrder, language, loginPassword);

        if (!accountList.isEmpty()) {
            accountList.forEach(account -> account.changeLoginPassword(this.loginPassword, loginPassword));
            sortAccountList();
        }

        this.loginPassword = loginPassword;
    }

    public void setLoginPassword(String password) {
        this.loginPassword = password;
    }

    public ArrayList<Account> getAccountList() {
        return this.accountList;
    }

    public void switchToProgramPanel(String password) {
        this.loginPassword = password;

        switch (loginAccount.getLanguage()) {
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
                EncrypterButton.setText("Cripta");
                DecrypterButton.setText("Decifra");
                SettingsButton.setText("Impostazioni");
                LogHistoryButton.setText("Cronologia Registro");
                EulaAndCreditsButton.setText("Termini e Crediti");
            }

            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        if (!accountList.isEmpty()) {
            MenuBar.setVisible(true);
        }

        // redirects to program panel
        replaceToMainPanel(ProgramPanel);
    }

    public void replaceToMainPanel(JPanel NewPanel) {
        replacePanel(MainPanel, NewPanel);
    }

    public void replaceToDialogPanel(JPanel NewPanel) {
        replacePanel(DialogPanel, NewPanel);
    }
    // #endregion

    // #region Generated Code
    // #region Swing variables declaration
    private JButton DecrypterButton;
    private DecrypterPanel DecrypterPanel;
    private JPanel DialogBlankPanel;
    private JPanel DialogPanel;
    private JButton EncrypterButton;
    private EncrypterPanel EncrypterPanel;
    private JButton EulaAndCreditsButton;
    private EulaAndCreditsPanel EulaAndCreditsPanel;
    private JMenu ExportAsMenu;
    private FirstRunPanel FirstRunPanel;
    private JButton LogHistoryButton;
    private LogHistoryPanel LogHistoryPanel;
    private LoginPanel LoginPanel;
    private JPanel MainPanel;
    private JMenuBar MenuBar;
    private JPanel ProgramPanel;
    private JButton SettingsButton;
    private SettingsPanel SettingsPanel;
    // #endregion

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private void initComponents() {
        MainPanel = new JPanel();
        DialogPanel = new JPanel();
        DialogBlankPanel = new JPanel();
        DecrypterPanel = new DecrypterPanel(this);
        EncrypterPanel = new EncrypterPanel(this);
        EulaAndCreditsPanel = new EulaAndCreditsPanel();
        FirstRunPanel = new FirstRunPanel(this);
        LogHistoryPanel = new LogHistoryPanel(this);
        LoginPanel = new LoginPanel(this);
        ProgramPanel = new JPanel();
        SettingsPanel = new SettingsPanel(this);

        EncrypterButton = new JButton();
        DecrypterButton = new JButton();
        SettingsButton = new JButton();
        LogHistoryButton = new JButton();
        EulaAndCreditsButton = new JButton();

        JPanel butttonPanel = new JPanel();
        JLabel passwordManagerLabel = new JLabel();

        MenuBar = new JMenuBar();
        ExportAsMenu = new JMenu();
        JMenuItem htmlMenuItem = new JMenuItem();
        JMenuItem csvMenuItem = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Password Manager");
        setBackground(new Color(0, 0, 0));
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        setFocusCycleRoot(false);
        setFont(new Font("Dialog", Font.BOLD, 10)); // NOI18N
        setForeground(Color.lightGray);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing();
            }
        });

        MainPanel.setBackground(new Color(38, 38, 38));
        MainPanel.setForeground(new Color(242, 242, 242));
        MainPanel.setLayout(new CardLayout());

        MainPanel.add(FirstRunPanel, "card2");
        MainPanel.add(LoginPanel, "card3");

        ProgramPanel.setBackground(new Color(242, 65, 65));
        ProgramPanel.setForeground(new Color(242, 242, 242));
        ProgramPanel.setPreferredSize(new Dimension(1280, 720));

        DialogPanel.setBackground(new Color(38, 38, 38));
        DialogPanel.setBorder(new LineBorder(new Color(115, 41, 41), 2, true));
        DialogPanel.setForeground(new Color(242, 242, 242));
        DialogPanel.setLayout(new CardLayout());

        DialogBlankPanel.setBackground(new Color(38, 38, 38));
        DialogBlankPanel.setForeground(new Color(242, 242, 242));
        DialogBlankPanel.setToolTipText("");

        GroupLayout DialogBlankPanelLayout = new GroupLayout(DialogBlankPanel);
        DialogBlankPanel.setLayout(DialogBlankPanelLayout);
        DialogBlankPanelLayout.setHorizontalGroup(
                DialogBlankPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 1145, Short.MAX_VALUE));
        DialogBlankPanelLayout.setVerticalGroup(
                DialogBlankPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 879, Short.MAX_VALUE));

        DialogPanel.add(DialogBlankPanel, "card2");

        DialogPanel.add(EncrypterPanel, "card2");
        DialogPanel.add(DecrypterPanel, "card2");
        DialogPanel.add(SettingsPanel, "card2");
        DialogPanel.add(LogHistoryPanel, "card2");
        DialogPanel.add(EulaAndCreditsPanel, "card6");

        butttonPanel.setBackground(new Color(38, 38, 38));
        butttonPanel.setBorder(new LineBorder(new Color(115, 41, 41), 2, true));
        butttonPanel.setForeground(new Color(242, 242, 242));
        butttonPanel.setPreferredSize(new Dimension(235, 242));

        passwordManagerLabel.setBackground(new Color(38, 38, 38));
        passwordManagerLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        passwordManagerLabel.setForeground(new Color(242, 242, 242));
        passwordManagerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        passwordManagerLabel.setText("<html>Password<br>Manager</html>");
        passwordManagerLabel.setToolTipText("");
        passwordManagerLabel.setEnabled(false);

        styleButton(EncrypterButton, "Encrypter", this::EncrypterButtonActionPerformed);

        DecrypterButton.setBackground(new Color(38, 38, 38));
        DecrypterButton.setFont(new Font("Dialog", Font.BOLD, 18)); // NOI18N
        DecrypterButton.setForeground(new Color(242, 242, 242));
        DecrypterButton.setText("Decrypter");
        DecrypterButton.addActionListener(this::DecrypterButtonActionPerformed);

        SettingsButton.setBackground(new Color(38, 38, 38));
        SettingsButton.setFont(new Font("Dialog", Font.BOLD, 18)); // NOI18N
        SettingsButton.setForeground(new Color(242, 242, 242));
        SettingsButton.setText("Settings");
        SettingsButton.addActionListener(this::SettingsButtonActionPerformed);

        LogHistoryButton.setBackground(new Color(38, 38, 38));
        LogHistoryButton.setFont(new Font("Dialog", Font.BOLD, 12)); // NOI18N
        LogHistoryButton.setForeground(new Color(242, 242, 242));
        LogHistoryButton.setText("Log History");
        LogHistoryButton.addActionListener(this::LogHistoryButtonActionPerformed);

        EulaAndCreditsButton.setBackground(new Color(38, 38, 38));
        EulaAndCreditsButton.setFont(new Font("Dialog", Font.BOLD, 12)); // NOI18N
        EulaAndCreditsButton.setForeground(new Color(242, 242, 242));
        EulaAndCreditsButton.setText("EULA and Credits");
        EulaAndCreditsButton.addActionListener(this::EulaAndCreditsButtonActionPerformed);

        GroupLayout ButttonPanelLayout = new GroupLayout(butttonPanel);
        butttonPanel.setLayout(ButttonPanelLayout);
        ButttonPanelLayout.setHorizontalGroup(
                ButttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(passwordManagerLabel, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(EncrypterButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(DecrypterButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(SettingsButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(11, 11, 11))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(LogHistoryButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(51, 51, 51))
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(EulaAndCreditsButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(51, 51, 51)));
        ButttonPanelLayout.setVerticalGroup(
                ButttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ButttonPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(passwordManagerLabel, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(EncrypterButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(DecrypterButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(SettingsButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(LogHistoryButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(6, 6, 6)
                                .addComponent(EulaAndCreditsButton, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(7, 7, 7)));

        passwordManagerLabel.getAccessibleContext().setAccessibleName("");

        GroupLayout ProgramPanelLayout = new GroupLayout(ProgramPanel);
        ProgramPanel.setLayout(ProgramPanelLayout);
        ProgramPanelLayout.setHorizontalGroup(
                ProgramPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ProgramPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(butttonPanel, GroupLayout.PREFERRED_SIZE, 240,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(DialogPanel, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(5, 5, 5)));
        ProgramPanelLayout.setVerticalGroup(
                ProgramPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ProgramPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(ProgramPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(butttonPanel, GroupLayout.DEFAULT_SIZE, 883,
                                                Short.MAX_VALUE)
                                        .addComponent(DialogPanel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(5, 5, 5)));

        MainPanel.add(ProgramPanel, "card2");

        MenuBar.setBackground(new Color(38, 38, 38));
        MenuBar.setForeground(new Color(242, 242, 242));

        ExportAsMenu.setText("Export as");

        htmlMenuItem.setText("HTML");
        htmlMenuItem.addActionListener(this::htmlMenuItemActionPerformed);
        ExportAsMenu.add(htmlMenuItem);

        csvMenuItem.setText("CSV");
        csvMenuItem.addActionListener(this::csvMenuItemActionPerformed);
        ExportAsMenu.add(csvMenuItem);

        MenuBar.add(ExportAsMenu);

        setJMenuBar(MenuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(MainPanel, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(MainPanel, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        pack();
    }

    private static void styleButton(JButton Button, String text, ActionListener ActionListener) {
        Button.setBackground(new Color(38, 38, 38));
        Button.setFont(new Font("Dialog", Font.BOLD, 18)); // NOI18N
        Button.setForeground(new Color(242, 242, 242));
        // Button.setMnemonic('?');
        Button.setText("Encrypter");
        Button.addActionListener(ActionListener);
    }
    // #endregion

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
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>

        /* Create and display the form */
        EventQueue.invokeLater(() -> new Main().setVisible(true));
    }

}

/*
 * Colors in Hex
 * #8C8A87
 * #F24141
 * #732929;
 * #F2F2F2;
 * #262626;
 */