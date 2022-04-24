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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import javax.swing.JPanel;
import javax.swing.Timer;
import mareas.accounts.AccountArrayList;

/**
 * Main class.
 *
 * @version 1.9
 * @author 2004marras@gmail.com
 */
public class Main extends javax.swing.JFrame {

    /*
        Colors in Hex
        #8C8A87
        #F24141
        #732929;
        #F2F2F2;
        #262626;
     */
    private Account login_account;
    private ActionListener timer_task;
    private AccountArrayList<Account> account_list;
    private byte login_counter;
    private boolean delete_counter;
    private final DateTimeFormatter dtf;
    private int delay;
    private String log_history;
    private static String file_path = "";
    private Timer timer;

    /**
     * Constructor method, initialize objects and gets the data filepath, then
     * runs the program.
     */
    public Main() {
        initComponents();

        //initialize objects
        account_list = new AccountArrayList<>();
        delete_counter = false;
        log_history = "";
        dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        delay = login_counter = 0;
        timer_task = (ActionEvent e) -> {
        };

        MenuBar.setVisible(false);

        //gets the filepath
        if (System.getProperty("os.name").contains("Windows")) {
            file_path = System.getProperty("user.home") + "\\AppData\\Local\\Password Manager\\";
        }

        run();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        MainPanel = new javax.swing.JPanel();
        FirstRunPanel = new javax.swing.JPanel();
        FirstRunLanguageSelector = new javax.swing.JComboBox<>();
        FirstRunLanguageLabel = new javax.swing.JLabel();
        FirstRunSavingorderLabel = new javax.swing.JLabel();
        FirstRunLoginpassowrdLabel = new javax.swing.JLabel();
        FirstRunLoginpasswordTextField = new javax.swing.JTextField();
        FirstRunSeparator = new javax.swing.JSeparator();
        FirstRunConfirmButton = new javax.swing.JButton();
        FirstRunLabel = new javax.swing.JLabel();
        FirstRunLabel2 = new javax.swing.JLabel();
        FirstRunSavingorderSelector = new javax.swing.JComboBox<>();
        FirstRunSeparator1 = new javax.swing.JSeparator();
        FirstRunAccepteulaCheckBox = new javax.swing.JCheckBox();
        FirstRunEULATextArea = new javax.swing.JTextArea();
        LoginPanel = new javax.swing.JPanel();
        LoginLabel = new javax.swing.JLabel();
        LoginPasswordLabel = new javax.swing.JLabel();
        LoginPasswordField = new javax.swing.JPasswordField();
        LoginButton = new javax.swing.JButton();
        LoginUnsuccesfulLabel = new javax.swing.JLabel();
        ProgramPanel = new javax.swing.JPanel();
        DialogPanel = new javax.swing.JPanel();
        DialogBlankPanel = new javax.swing.JPanel();
        EncripterPanel = new javax.swing.JPanel();
        EncripterSoftwareLabel = new javax.swing.JLabel();
        EncripterSoftwareTextField = new javax.swing.JTextField();
        EncripterUsernameLabel = new javax.swing.JLabel();
        EncripterUsernameTextField = new javax.swing.JTextField();
        EncripterPasswordLabel = new javax.swing.JLabel();
        EncripterPasswordTextField = new javax.swing.JTextField();
        EncripterSaveButton = new javax.swing.JButton();
        DecripterPanel = new javax.swing.JPanel();
        DecripterAccountSelector = new javax.swing.JComboBox<>();
        DecripterSoftwareLabel = new javax.swing.JLabel();
        DecripterUsernameLabel = new javax.swing.JLabel();
        DecripterPasswordLabel = new javax.swing.JLabel();
        DecripterPasswordTextField = new javax.swing.JTextField();
        DecripterUsernameTextField = new javax.swing.JTextField();
        DecripterSoftwareTextField = new javax.swing.JTextField();
        DecripterSeparator = new javax.swing.JSeparator();
        DecripterSaveButton = new javax.swing.JButton();
        DecripterDeleteButton = new javax.swing.JButton();
        SettingsPanel = new javax.swing.JPanel();
        SettingsLanguageSelector = new javax.swing.JComboBox<>();
        SettingsLanguageLabel = new javax.swing.JLabel();
        SettingsSavingorderLabel = new javax.swing.JLabel();
        SettingsLoginpassowrdLabel = new javax.swing.JLabel();
        SettingsLoginpasswordTextField = new javax.swing.JTextField();
        SettingsSaveButton = new javax.swing.JButton();
        SettingsSavingorderSelector = new javax.swing.JComboBox<>();
        LogHistoryPanel = new javax.swing.JPanel();
        LogHystoryLabel = new javax.swing.JLabel();
        LogHistoryScrollPane = new javax.swing.JScrollPane();
        LogHistoryTextArea = new javax.swing.JTextArea();
        LogLegendLabel = new javax.swing.JLabel();
        EulaAndCreditsPanel = new javax.swing.JPanel();
        CreditsLabel = new javax.swing.JLabel();
        CreditsTextArea = new javax.swing.JTextArea();
        EULALabel = new javax.swing.JLabel();
        EULATextArea = new javax.swing.JTextArea();
        ButttonPanel = new javax.swing.JPanel();
        PasswordManagerLabel = new javax.swing.JLabel();
        EncripterButton = new javax.swing.JButton();
        DecripterButton = new javax.swing.JButton();
        SettingsButton = new javax.swing.JButton();
        LogHistoryButton = new javax.swing.JButton();
        EulaAndCreditsButton = new javax.swing.JButton();
        MenuBar = new javax.swing.JMenuBar();
        ExportAsMenu = new javax.swing.JMenu();
        htmlMenuItem = new javax.swing.JMenuItem();
        csvMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Password Manager");
        setBackground(new java.awt.Color(0, 0, 0));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setFocusCycleRoot(false);
        setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        setForeground(java.awt.Color.lightGray);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
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
        FirstRunLanguageSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FirstRunLanguageSelector.setBorder(null);

        FirstRunLanguageLabel.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunLanguageLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunLanguageLabel.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunLanguageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        FirstRunLanguageLabel.setText("Language:");

        FirstRunSavingorderLabel.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunSavingorderLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunSavingorderLabel.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunSavingorderLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        FirstRunSavingorderLabel.setText("Saving order:");

        FirstRunLoginpassowrdLabel.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunLoginpassowrdLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunLoginpassowrdLabel.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunLoginpassowrdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        FirstRunLoginpassowrdLabel.setText("Login password:");

        FirstRunLoginpasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        FirstRunLoginpasswordTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunLoginpasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        FirstRunLoginpasswordTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        FirstRunSeparator.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunSeparator.setForeground(new java.awt.Color(242, 242, 242));

        FirstRunConfirmButton.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunConfirmButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunConfirmButton.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunConfirmButton.setText("Confirm");
        FirstRunConfirmButton.setOpaque(false);
        FirstRunConfirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FirstRunConfirmButtonActionPerformed(evt);
            }
        });

        FirstRunLabel.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunLabel.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        FirstRunLabel.setText("Hi!");

        FirstRunLabel2.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunLabel2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        FirstRunLabel2.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        FirstRunLabel2.setText("It's the developer here, before using this program i need you to insert some personalization informations and a password to protect your accounts.");

        FirstRunSavingorderSelector.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunSavingorderSelector.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunSavingorderSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FirstRunSavingorderSelector.setBorder(null);

        FirstRunSeparator1.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunSeparator1.setForeground(new java.awt.Color(242, 242, 242));

        FirstRunAccepteulaCheckBox.setBackground(new java.awt.Color(38, 38, 38));
        FirstRunAccepteulaCheckBox.setForeground(new java.awt.Color(242, 242, 242));
        FirstRunAccepteulaCheckBox.setText("Accept EULA");

        FirstRunEULATextArea.setEditable(false);
        FirstRunEULATextArea.setBackground(new java.awt.Color(242, 65, 65));
        FirstRunEULATextArea.setColumns(20);
        FirstRunEULATextArea.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        FirstRunEULATextArea.setForeground(new java.awt.Color(38, 38, 38));
        FirstRunEULATextArea.setRows(5);
        FirstRunEULATextArea.setText("Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License \nas published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\nThis program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied \nwarranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for \nmore details.\n\nYou should have received a copy of the GNU General Public License along with this program.\nIf not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        FirstRunEULATextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        javax.swing.GroupLayout FirstRunPanelLayout = new javax.swing.GroupLayout(FirstRunPanel);
        FirstRunPanel.setLayout(FirstRunPanelLayout);
        FirstRunPanelLayout.setHorizontalGroup(
            FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FirstRunPanelLayout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(FirstRunPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(FirstRunAccepteulaCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(FirstRunConfirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(FirstRunSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(FirstRunSeparator, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(FirstRunPanelLayout.createSequentialGroup()
                        .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(FirstRunSavingorderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FirstRunLoginpassowrdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FirstRunLanguageLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(FirstRunLoginpasswordTextField)
                            .addComponent(FirstRunLanguageSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FirstRunSavingorderSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(FirstRunLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(FirstRunLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 1307, Short.MAX_VALUE)
                    .addComponent(FirstRunEULATextArea))
                .addGap(50, 50, 50))
        );
        FirstRunPanelLayout.setVerticalGroup(
            FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FirstRunPanelLayout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(FirstRunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(FirstRunLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(FirstRunSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FirstRunLanguageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(FirstRunLanguageSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(17, 17, 17)
                .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FirstRunSavingorderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(FirstRunSavingorderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FirstRunLoginpasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(FirstRunLoginpassowrdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(FirstRunSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(FirstRunEULATextArea, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FirstRunPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FirstRunConfirmButton)
                    .addComponent(FirstRunAccepteulaCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50))
        );

        MainPanel.add(FirstRunPanel, "card2");

        LoginPanel.setBackground(new java.awt.Color(38, 38, 38));
        LoginPanel.setForeground(new java.awt.Color(242, 242, 242));

        LoginLabel.setBackground(new java.awt.Color(38, 38, 38));
        LoginLabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        LoginLabel.setForeground(new java.awt.Color(242, 242, 242));
        LoginLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        LoginLabel.setText("Login");
        LoginLabel.setToolTipText("");
        LoginLabel.setEnabled(false);

        LoginPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        LoginPasswordLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LoginPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        LoginPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        LoginPasswordLabel.setText("Password:");

        LoginPasswordField.setBackground(new java.awt.Color(242, 65, 65));
        LoginPasswordField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LoginPasswordField.setForeground(new java.awt.Color(38, 38, 38));
        LoginPasswordField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        LoginButton.setBackground(new java.awt.Color(38, 38, 38));
        LoginButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LoginButton.setForeground(new java.awt.Color(242, 242, 242));
        LoginButton.setText("Login");
        LoginButton.setOpaque(false);
        LoginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginButtonActionPerformed(evt);
            }
        });

        LoginUnsuccesfulLabel.setBackground(new java.awt.Color(38, 38, 38));
        LoginUnsuccesfulLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LoginUnsuccesfulLabel.setForeground(new java.awt.Color(242, 242, 242));

        javax.swing.GroupLayout LoginPanelLayout = new javax.swing.GroupLayout(LoginPanel);
        LoginPanel.setLayout(LoginPanelLayout);
        LoginPanelLayout.setHorizontalGroup(
            LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LoginPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(LoginButton, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                    .addComponent(LoginPasswordLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(LoginLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
                .addGroup(LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(LoginPanelLayout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(LoginPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, 590, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoginPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LoginUnsuccesfulLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 590, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(490, 490, 490))
        );
        LoginPanelLayout.setVerticalGroup(
            LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LoginPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(LoginLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LoginPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LoginPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(LoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LoginUnsuccesfulLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LoginButton))
                .addGap(265, 265, 265))
        );

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
            .addGap(0, 1145, Short.MAX_VALUE)
        );
        DialogBlankPanelLayout.setVerticalGroup(
            DialogBlankPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 879, Short.MAX_VALUE)
        );

        DialogPanel.add(DialogBlankPanel, "card2");

        EncripterPanel.setBackground(new java.awt.Color(38, 38, 38));
        EncripterPanel.setForeground(new java.awt.Color(242, 242, 242));
        EncripterPanel.setToolTipText("");

        EncripterSoftwareLabel.setBackground(new java.awt.Color(38, 38, 38));
        EncripterSoftwareLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterSoftwareLabel.setForeground(new java.awt.Color(242, 242, 242));
        EncripterSoftwareLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EncripterSoftwareLabel.setText("Software:");

        EncripterSoftwareTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncripterSoftwareTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterSoftwareTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncripterSoftwareTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EncripterUsernameLabel.setBackground(new java.awt.Color(38, 38, 38));
        EncripterUsernameLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterUsernameLabel.setForeground(new java.awt.Color(242, 242, 242));
        EncripterUsernameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EncripterUsernameLabel.setText("Username:");

        EncripterUsernameTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncripterUsernameTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterUsernameTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncripterUsernameTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EncripterPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        EncripterPasswordLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        EncripterPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        EncripterPasswordLabel.setText("Password:");

        EncripterPasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        EncripterPasswordTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterPasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        EncripterPasswordTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EncripterSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        EncripterSaveButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        EncripterSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        EncripterSaveButton.setText("Save");
        EncripterSaveButton.setOpaque(false);
        EncripterSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EncripterSaveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout EncripterPanelLayout = new javax.swing.GroupLayout(EncripterPanel);
        EncripterPanel.setLayout(EncripterPanelLayout);
        EncripterPanelLayout.setHorizontalGroup(
            EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EncripterPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(EncripterPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(EncripterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(EncripterUsernameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(EncripterSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(EncripterSoftwareTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
                    .addComponent(EncripterPasswordTextField)
                    .addComponent(EncripterUsernameTextField, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(50, 50, 50))
        );
        EncripterPanelLayout.setVerticalGroup(
            EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EncripterPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(EncripterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                    .addComponent(EncripterSoftwareTextField))
                .addGap(18, 18, 18)
                .addGroup(EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EncripterUsernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(EncripterUsernameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(EncripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(EncripterPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(EncripterPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(EncripterSaveButton)
                .addGap(473, 473, 473))
        );

        DialogPanel.add(EncripterPanel, "card2");

        DecripterPanel.setBackground(new java.awt.Color(38, 38, 38));
        DecripterPanel.setForeground(new java.awt.Color(242, 242, 242));

        DecripterAccountSelector.setBackground(new java.awt.Color(38, 38, 38));
        DecripterAccountSelector.setForeground(new java.awt.Color(242, 242, 242));
        DecripterAccountSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        DecripterAccountSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecripterAccountSelectorActionPerformed(evt);
            }
        });

        DecripterSoftwareLabel.setBackground(new java.awt.Color(38, 38, 38));
        DecripterSoftwareLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterSoftwareLabel.setForeground(new java.awt.Color(242, 242, 242));
        DecripterSoftwareLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        DecripterSoftwareLabel.setText("Software:");

        DecripterUsernameLabel.setBackground(new java.awt.Color(38, 38, 38));
        DecripterUsernameLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterUsernameLabel.setForeground(new java.awt.Color(242, 242, 242));
        DecripterUsernameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        DecripterUsernameLabel.setText("Username:");

        DecripterPasswordLabel.setBackground(new java.awt.Color(38, 38, 38));
        DecripterPasswordLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterPasswordLabel.setForeground(new java.awt.Color(242, 242, 242));
        DecripterPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        DecripterPasswordLabel.setText("Password:");

        DecripterPasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecripterPasswordTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterPasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecripterPasswordTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        DecripterUsernameTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecripterUsernameTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterUsernameTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecripterUsernameTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        DecripterSoftwareTextField.setBackground(new java.awt.Color(242, 65, 65));
        DecripterSoftwareTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterSoftwareTextField.setForeground(new java.awt.Color(38, 38, 38));
        DecripterSoftwareTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        DecripterSeparator.setBackground(new java.awt.Color(38, 38, 38));
        DecripterSeparator.setForeground(new java.awt.Color(242, 242, 242));

        DecripterSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        DecripterSaveButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        DecripterSaveButton.setText("Save");
        DecripterSaveButton.setOpaque(false);
        DecripterSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecripterSaveButtonActionPerformed(evt);
            }
        });

        DecripterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
        DecripterDeleteButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        DecripterDeleteButton.setForeground(new java.awt.Color(242, 242, 242));
        DecripterDeleteButton.setText("Delete");
        DecripterDeleteButton.setOpaque(false);
        DecripterDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecripterDeleteButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout DecripterPanelLayout = new javax.swing.GroupLayout(DecripterPanel);
        DecripterPanel.setLayout(DecripterPanelLayout);
        DecripterPanelLayout.setHorizontalGroup(
            DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DecripterPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(DecripterSeparator)
                    .addComponent(DecripterAccountSelector, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, DecripterPanelLayout.createSequentialGroup()
                        .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(DecripterPasswordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(DecripterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(DecripterUsernameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                            .addComponent(DecripterDeleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(DecripterPasswordTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
                            .addComponent(DecripterSoftwareTextField)
                            .addComponent(DecripterUsernameTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(DecripterPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(DecripterSaveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(50, 50, 50))
        );
        DecripterPanelLayout.setVerticalGroup(
            DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DecripterPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(DecripterAccountSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(DecripterSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(DecripterSoftwareLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                    .addComponent(DecripterSoftwareTextField))
                .addGap(18, 18, 18)
                .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DecripterUsernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DecripterUsernameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DecripterPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DecripterPasswordLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(DecripterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DecripterDeleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(DecripterSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(50, 50, 50))
        );

        DialogPanel.add(DecripterPanel, "card2");

        SettingsPanel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsPanel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsPanel.setToolTipText("");

        SettingsLanguageSelector.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLanguageSelector.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLanguageSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        SettingsLanguageLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLanguageLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        SettingsLanguageLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLanguageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsLanguageLabel.setText("Language:");

        SettingsSavingorderLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSavingorderLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        SettingsSavingorderLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSavingorderLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsSavingorderLabel.setText("Saving order:");

        SettingsLoginpassowrdLabel.setBackground(new java.awt.Color(38, 38, 38));
        SettingsLoginpassowrdLabel.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        SettingsLoginpassowrdLabel.setForeground(new java.awt.Color(242, 242, 242));
        SettingsLoginpassowrdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SettingsLoginpassowrdLabel.setText("Login password:");

        SettingsLoginpasswordTextField.setBackground(new java.awt.Color(242, 65, 65));
        SettingsLoginpasswordTextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        SettingsLoginpasswordTextField.setForeground(new java.awt.Color(38, 38, 38));
        SettingsLoginpasswordTextField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        SettingsSaveButton.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSaveButton.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        SettingsSaveButton.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSaveButton.setText("Confirm");
        SettingsSaveButton.setOpaque(false);
        SettingsSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SettingsSaveButtonActionPerformed(evt);
            }
        });

        SettingsSavingorderSelector.setBackground(new java.awt.Color(38, 38, 38));
        SettingsSavingorderSelector.setForeground(new java.awt.Color(242, 242, 242));
        SettingsSavingorderSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout SettingsPanelLayout = new javax.swing.GroupLayout(SettingsPanel);
        SettingsPanel.setLayout(SettingsPanelLayout);
        SettingsPanelLayout.setHorizontalGroup(
            SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(SettingsSavingorderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(SettingsSaveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(SettingsLanguageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(SettingsLoginpassowrdLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SettingsSavingorderSelector, javax.swing.GroupLayout.Alignment.TRAILING, 0, 859, Short.MAX_VALUE)
                    .addComponent(SettingsLoginpasswordTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(SettingsLanguageSelector, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(50, 50, 50))
        );
        SettingsPanelLayout.setVerticalGroup(
            SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SettingsLanguageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SettingsLanguageSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SettingsSavingorderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SettingsSavingorderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SettingsLoginpasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SettingsLoginpassowrdLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(SettingsSaveButton)
                .addGap(50, 50, 50))
        );

        DialogPanel.add(SettingsPanel, "card2");

        LogHistoryPanel.setBackground(new java.awt.Color(38, 38, 38));
        LogHistoryPanel.setForeground(new java.awt.Color(242, 242, 242));
        LogHistoryPanel.setToolTipText("");

        LogHystoryLabel.setBackground(new java.awt.Color(38, 38, 38));
        LogHystoryLabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        LogHystoryLabel.setForeground(new java.awt.Color(242, 242, 242));
        LogHystoryLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        LogHystoryLabel.setText("Log History:");
        LogHystoryLabel.setInheritsPopupMenu(false);
        LogHystoryLabel.setName(""); // NOI18N

        LogHistoryScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        LogHistoryTextArea.setEditable(false);
        LogHistoryTextArea.setBackground(new java.awt.Color(242, 65, 65));
        LogHistoryTextArea.setColumns(50);
        LogHistoryTextArea.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LogHistoryTextArea.setForeground(new java.awt.Color(38, 38, 38));
        LogHistoryTextArea.setRows(5);
        LogHistoryTextArea.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(115, 41, 41), 1, true));
        LogHistoryTextArea.setCaretColor(new java.awt.Color(102, 102, 102));
        LogHistoryScrollPane.setViewportView(LogHistoryTextArea);

        LogLegendLabel.setBackground(new java.awt.Color(38, 38, 38));
        LogLegendLabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        LogLegendLabel.setForeground(new java.awt.Color(242, 242, 242));
        LogLegendLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        LogLegendLabel.setText("[Actions]    {Errors}");
        LogLegendLabel.setInheritsPopupMenu(false);
        LogLegendLabel.setName(""); // NOI18N

        javax.swing.GroupLayout LogHistoryPanelLayout = new javax.swing.GroupLayout(LogHistoryPanel);
        LogHistoryPanel.setLayout(LogHistoryPanelLayout);
        LogHistoryPanelLayout.setHorizontalGroup(
            LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LogHistoryPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                        .addComponent(LogHystoryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 574, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(LogLegendLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 465, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(LogHistoryScrollPane))
                .addGap(50, 50, 50))
        );
        LogHistoryPanelLayout.setVerticalGroup(
            LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(LogHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LogHystoryLabel)
                    .addComponent(LogLegendLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LogHistoryScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
                .addGap(50, 50, 50))
        );

        DialogPanel.add(LogHistoryPanel, "card2");

        EulaAndCreditsPanel.setBackground(new java.awt.Color(38, 38, 38));
        EulaAndCreditsPanel.setForeground(new java.awt.Color(242, 242, 242));
        EulaAndCreditsPanel.setToolTipText("");

        CreditsLabel.setBackground(new java.awt.Color(38, 38, 38));
        CreditsLabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        CreditsLabel.setForeground(new java.awt.Color(242, 242, 242));
        CreditsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        CreditsLabel.setLabelFor(CreditsTextArea);
        CreditsLabel.setText("Credits:");

        CreditsTextArea.setEditable(false);
        CreditsTextArea.setBackground(new java.awt.Color(242, 65, 65));
        CreditsTextArea.setColumns(20);
        CreditsTextArea.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        CreditsTextArea.setForeground(new java.awt.Color(38, 38, 38));
        CreditsTextArea.setRows(5);
        CreditsTextArea.setText("Creator and developer: Francesco Marras.\nSpecial thanks to \"Luca\".\n\n");
        CreditsTextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        EULALabel.setBackground(new java.awt.Color(38, 38, 38));
        EULALabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        EULALabel.setForeground(new java.awt.Color(242, 242, 242));
        EULALabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        EULALabel.setLabelFor(EULATextArea);
        EULALabel.setText("EULA:");

        EULATextArea.setEditable(false);
        EULATextArea.setBackground(new java.awt.Color(242, 65, 65));
        EULATextArea.setColumns(20);
        EULATextArea.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        EULATextArea.setForeground(new java.awt.Color(38, 38, 38));
        EULATextArea.setRows(5);
        EULATextArea.setText("Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        EULATextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(115, 41, 41)));

        javax.swing.GroupLayout EulaAndCreditsPanelLayout = new javax.swing.GroupLayout(EulaAndCreditsPanel);
        EulaAndCreditsPanel.setLayout(EulaAndCreditsPanelLayout);
        EulaAndCreditsPanelLayout.setHorizontalGroup(
            EulaAndCreditsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(EulaAndCreditsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CreditsTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 1045, Short.MAX_VALUE)
                    .addComponent(EULALabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(CreditsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(EULATextArea, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1045, Short.MAX_VALUE))
                .addGap(50, 50, 50))
        );
        EulaAndCreditsPanelLayout.setVerticalGroup(
            EulaAndCreditsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addComponent(CreditsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CreditsTextArea, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(EULALabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(EULATextArea, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50))
        );

        DialogPanel.add(EulaAndCreditsPanel, "card6");

        ButttonPanel.setBackground(new java.awt.Color(38, 38, 38));
        ButttonPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(115, 41, 41), 2, true));
        ButttonPanel.setForeground(new java.awt.Color(242, 242, 242));
        ButttonPanel.setPreferredSize(new java.awt.Dimension(235, 242));

        PasswordManagerLabel.setBackground(new java.awt.Color(38, 38, 38));
        PasswordManagerLabel.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        PasswordManagerLabel.setForeground(new java.awt.Color(242, 242, 242));
        PasswordManagerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PasswordManagerLabel.setText("<html>Password<br>Manager</html>");
        PasswordManagerLabel.setToolTipText("");
        PasswordManagerLabel.setEnabled(false);

        EncripterButton.setBackground(new java.awt.Color(38, 38, 38));
        EncripterButton.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        EncripterButton.setForeground(new java.awt.Color(242, 242, 242));
        EncripterButton.setMnemonic('1');
        EncripterButton.setText("Encripter");
        EncripterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EncripterButtonActionPerformed(evt);
            }
        });

        DecripterButton.setBackground(new java.awt.Color(38, 38, 38));
        DecripterButton.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        DecripterButton.setForeground(new java.awt.Color(242, 242, 242));
        DecripterButton.setMnemonic('2');
        DecripterButton.setText("Decripter");
        DecripterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecripterButtonActionPerformed(evt);
            }
        });

        SettingsButton.setBackground(new java.awt.Color(38, 38, 38));
        SettingsButton.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        SettingsButton.setForeground(new java.awt.Color(242, 242, 242));
        SettingsButton.setText("Settings");
        SettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SettingsButtonActionPerformed(evt);
            }
        });

        LogHistoryButton.setBackground(new java.awt.Color(38, 38, 38));
        LogHistoryButton.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        LogHistoryButton.setForeground(new java.awt.Color(242, 242, 242));
        LogHistoryButton.setText("Log History");
        LogHistoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LogHistoryButtonActionPerformed(evt);
            }
        });

        EulaAndCreditsButton.setBackground(new java.awt.Color(38, 38, 38));
        EulaAndCreditsButton.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        EulaAndCreditsButton.setForeground(new java.awt.Color(242, 242, 242));
        EulaAndCreditsButton.setText("EULA and Credits");
        EulaAndCreditsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EulaAndCreditsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ButttonPanelLayout = new javax.swing.GroupLayout(ButttonPanel);
        ButttonPanel.setLayout(ButttonPanelLayout);
        ButttonPanelLayout.setHorizontalGroup(
            ButttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(PasswordManagerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(EncripterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(DecripterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(SettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(LogHistoryButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(51, 51, 51))
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(EulaAndCreditsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(51, 51, 51))
        );
        ButttonPanelLayout.setVerticalGroup(
            ButttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ButttonPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(PasswordManagerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6)
                .addComponent(EncripterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6)
                .addComponent(DecripterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6)
                .addComponent(SettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6)
                .addComponent(LogHistoryButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6)
                .addComponent(EulaAndCreditsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(7, 7, 7))
        );

        PasswordManagerLabel.getAccessibleContext().setAccessibleName("");

        javax.swing.GroupLayout ProgramPanelLayout = new javax.swing.GroupLayout(ProgramPanel);
        ProgramPanel.setLayout(ProgramPanelLayout);
        ProgramPanelLayout.setHorizontalGroup(
            ProgramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProgramPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(ButttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(DialogPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(5, 5, 5))
        );
        ProgramPanelLayout.setVerticalGroup(
            ProgramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProgramPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(ProgramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ButttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 883, Short.MAX_VALUE)
                    .addComponent(DialogPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(5, 5, 5))
        );

        MainPanel.add(ProgramPanel, "card2");

        MenuBar.setBackground(new java.awt.Color(38, 38, 38));
        MenuBar.setForeground(new java.awt.Color(242, 242, 242));

        ExportAsMenu.setText("Export as");

        htmlMenuItem.setText("HTML");
        htmlMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                htmlMenuItemActionPerformed(evt);
            }
        });
        ExportAsMenu.add(htmlMenuItem);

        csvMenuItem.setText("CSV");
        csvMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                csvMenuItemActionPerformed(evt);
            }
        });
        ExportAsMenu.add(csvMenuItem);

        MenuBar.add(ExportAsMenu);

        setJMenuBar(MenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>                        

    /**
     * Main method, initializes JFrame and calls Main's constructor.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        }
        );
    }

    private void EncripterButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        if (login_account.getSoftware().equals("e")) {
            EncripterSaveButton.setText("Save");
            EncripterUsernameLabel.setText("Username:");
        } else if (login_account.getSoftware().equals("i")) {
            EncripterSaveButton.setText("Salva");
            EncripterUsernameLabel.setText("Nome utente:");
        }
        EncripterSaveButton.repaint();
        EncripterUsernameLabel.repaint();

        //redirects to encripter panel
        replacePanel(DialogPanel, EncripterPanel);
    }                                               

    private void LogHistoryButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        log_history += dtf.format(LocalDateTime.now()) + " >>> [Log history showed]\n";

        if (login_account.getSoftware().equals("e")) {
            LogHystoryLabel.setText("Log History:");
            LogLegendLabel.setText("[Actions]    {Errors}");
        } else if (login_account.getSoftware().equals("i")) {
            LogHystoryLabel.setText("Cronologia del Registro:");
            LogLegendLabel.setText("[Azioni]    {Errori}");
        }
        LogHystoryLabel.repaint();
        LogLegendLabel.repaint();

        //writes the log history to its text area
        LogHistoryTextArea.setText(log_history);

        //redirects to lohg history panel
        replacePanel(DialogPanel, LogHistoryPanel);
    }                                                

    private void SettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {                                               
        SettingsLanguageSelector.removeAllItems();
        SettingsSavingorderSelector.removeAllItems();

        if (login_account.getSoftware().equals("e")) {
            SettingsLanguageSelector.addItem("English");
            SettingsLanguageSelector.addItem("Italian");
            SettingsSavingorderSelector.addItem("Software");
            SettingsSavingorderSelector.addItem("Username");

            SettingsLanguageLabel.setText("Language:");
            SettingsSavingorderLabel.setText("Saving order:");
            SettingsLoginpassowrdLabel.setText("Login password:");
            SettingsSaveButton.setText("Confirm");
        } else if (login_account.getSoftware().equals("i")) {
            SettingsLanguageSelector.addItem("Inglese");
            SettingsLanguageSelector.addItem("Italiano");
            SettingsSavingorderSelector.addItem("Software");
            SettingsSavingorderSelector.addItem("Nome utente");

            SettingsLanguageLabel.setText("Linguaggio:");
            SettingsSavingorderLabel.setText("Ordine di salvataggio:");
            SettingsLoginpassowrdLabel.setText("Password d'accesso:");
            SettingsSaveButton.setText("Conferma");
        }

        SettingsLanguageSelector.repaint();
        SettingsSavingorderSelector.repaint();
        SettingsLanguageLabel.repaint();
        SettingsSavingorderLabel.repaint();
        SettingsLoginpassowrdLabel.repaint();
        SettingsSaveButton.repaint();

        //sets the current language
        if (login_account.getSoftware().equals("e")) {
            SettingsLanguageSelector.setSelectedIndex(0);
        } else if (login_account.getSoftware().equals("i")) {
            SettingsLanguageSelector.setSelectedIndex(1);
        }

        //sets the current saving order
        SettingsSavingorderSelector.repaint();
        if (login_account.getUsername().equals("s")) {
            SettingsSavingorderSelector.setSelectedIndex(0);
        } else if (login_account.getUsername().equals("u")) {
            SettingsSavingorderSelector.setSelectedIndex(1);
        }

        //sets the current login password
        SettingsLoginpasswordTextField.setText(login_account.getPassword());

        //redirects to settings panel
        replacePanel(DialogPanel, SettingsPanel);
    }                                              

    private void DecripterButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                
        if (login_account.getSoftware().equals("e")) {
            DecripterDeleteButton.setText("Delete");
            DecripterSaveButton.setText("Save");
            DecripterUsernameLabel.setText("Username:");
        } else if (login_account.getSoftware().equals("i")) {
            DecripterDeleteButton.setText("Elimina");
            DecripterSaveButton.setText("Salva");
            DecripterUsernameLabel.setText("Nome utente:");
        }
        DecripterDeleteButton.setBackground(new java.awt.Color(51, 51, 51));
        DecripterDeleteButton.repaint();
        DecripterSaveButton.repaint();
        DecripterUsernameLabel.repaint();

        //to make sure the decripter delete button is in its first state
        delete_counter = false;

        updateDecripterAccountSelector();

        //redirects to decripter panel
        replacePanel(DialogPanel, DecripterPanel);
    }                                               

    private void EulaAndCreditsButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                     
        log_history += dtf.format(LocalDateTime.now()) + " >>> [EULA and Credits showed]\n";

        //redirects to eula and credits panel
        replacePanel(DialogPanel, EulaAndCreditsPanel);
    }                                                    

    private void LoginButtonActionPerformed(java.awt.event.ActionEvent evt) {                                            
        String password = String.valueOf(LoginPasswordField.getPassword());
        LoginPasswordField.setText("");
        LoginPasswordField.repaint();

        //if password is correct
        if (password.equals(login_account.getPassword())) {
            log_history += dtf.format(LocalDateTime.now()) + " >>> [Successful Login]\n";

            if (login_account.getSoftware().equals("e")) {
                ExportAsMenu.setText("Export as");
                EncripterButton.setText("Encripter");
                DecripterButton.setText("Decripter");
                SettingsButton.setText("Settings");
                LogHistoryButton.setText("Log History");
                EulaAndCreditsButton.setText("EULA and Credits");
            } else if (login_account.getSoftware().equals("i")) {
                ExportAsMenu.setText("Esporta come");
                EncripterButton.setText("Crittografa");
                DecripterButton.setText("Decifra");
                SettingsButton.setText("Impostazioni");
                LogHistoryButton.setText("Cronologia Registro");
                EulaAndCreditsButton.setText("Termini e Crediti");
            }
            ExportAsMenu.repaint();
            EncripterButton.repaint();
            DecripterButton.repaint();
            SettingsButton.repaint();
            LogHistoryButton.repaint();
            EulaAndCreditsButton.repaint();

            if (!account_list.isEmpty()) {
                MenuBar.setVisible(true);
            }

            //redirects to main panel
            replacePanel(MainPanel, ProgramPanel);
        } else {
            if (password.isBlank()) {
                if (login_account.getSoftware().equals("e")) {
                    LoginUnsuccesfulLabel.setText("Password field empty.");
                } else if (login_account.getSoftware().equals("i")) {
                    LoginUnsuccesfulLabel.setText("Password non inserita.");
                }
                LoginUnsuccesfulLabel.repaint();

                delay = 800;
                timer_task = (ActionEvent e) -> {
                    LoginButton.setEnabled(true);
                    LoginUnsuccesfulLabel.setText("");
                    LoginUnsuccesfulLabel.repaint();
                };
            } else {
                //counts the times a wrong password has been inserted
                login_counter++;

                if (login_counter == 3) {
                    if (login_account.getSoftware().equals("e")) {
                        LoginUnsuccesfulLabel.setText("A wrong password has been insterted three times, program sutting down...");
                    } else if (login_account.getSoftware().equals("i")) {
                        LoginUnsuccesfulLabel.setText("È ststa inserita una passowrd errata tre volte, programma in arresto...");
                    }
                    LoginUnsuccesfulLabel.repaint();

                    log_history += dtf.format(LocalDateTime.now()) + " >>> [Unsccessful Login]\n";

                    delay = 2000;
                    timer_task = (ActionEvent e) -> {
                        LoginButton.setEnabled(true);
                        LoginUnsuccesfulLabel.setText("");
                        LoginUnsuccesfulLabel.repaint();
                        System.exit(0);
                    };
                } else {
                    if (login_account.getSoftware().equals("e")) {
                        LoginUnsuccesfulLabel.setText("Wrong password.");
                    } else if (login_account.getSoftware().equals("i")) {
                        LoginUnsuccesfulLabel.setText("Passowrd errata.");
                    }
                    LoginUnsuccesfulLabel.repaint();

                    delay = 800;
                    timer_task = (ActionEvent e) -> {
                        LoginButton.setEnabled(true);
                        LoginUnsuccesfulLabel.setText("");
                        LoginUnsuccesfulLabel.repaint();
                    };
                }

                LoginUnsuccesfulLabel.repaint();
                LoginButton.setEnabled(false);

                timer = new Timer(delay, timer_task);
                timer.setRepeats(false);
                timer.start();
            }
        }
    }                                           

    private void EncripterSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                    
        //gets software, username and password written by the user
        String software = EncripterSoftwareTextField.getText();
        String username = EncripterUsernameTextField.getText();
        String password = EncripterPasswordTextField.getText();

        if (!(software.isBlank() || username.isBlank() || password.isBlank())) {
            addAccount(createAccount(software, username, password));

            log_history += dtf.format(LocalDateTime.now()) + " >>> [Account added]\n";

            EncripterSoftwareTextField.setText("");
            EncripterSoftwareTextField.repaint();

            EncripterUsernameTextField.setText("");
            EncripterUsernameTextField.repaint();

            EncripterPasswordTextField.setText("");
            EncripterPasswordTextField.repaint();

            MenuBar.setVisible(true);
        }
    }                                                   

    private void formWindowClosing(java.awt.event.WindowEvent evt) {                                   
        //when the user shuts down the program on the first run, it won't save 
        if (login_account != null) {
            try (ObjectOutputStream fOUT = new ObjectOutputStream(new FileOutputStream(file_path + "passwords.psmg"))) {
                fOUT.writeObject(this.login_account);
                fOUT.writeObject(this.account_list);
                fOUT.flush();
                fOUT.close();
            } catch (IOException e) {
                log_history += dtf.format(LocalDateTime.now()) + " >>> {Error: " + e.getMessage() + "}\n";
            }

            log_history += dtf.format(LocalDateTime.now()) + " >>> [Files saved and program shutted down]\n";

            try (FileWriter w = new FileWriter(file_path + "report.log")) {
                w.write(log_history);
                w.flush();
                w.close();
            } catch (IOException ex) {
            }
        }
    }                                  

    private void DecripterSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                    
        if (selectedItemInComboBox(DecripterAccountSelector) >= 0) {
            //get the new software, username and password
            String software = DecripterSoftwareTextField.getText();
            String username = DecripterUsernameTextField.getText();
            String password = DecripterPasswordTextField.getText();

            if (!(software.isBlank() && username.isBlank() && password.isBlank())) {
                //save the new attributes of the account
                account_list.get(selectedItemInComboBox(DecripterAccountSelector)).setSoftware(software);
                account_list.get(selectedItemInComboBox(DecripterAccountSelector)).setUsername(username);
                account_list.get(selectedItemInComboBox(DecripterAccountSelector)).setPassword(password);

                log_history += dtf.format(LocalDateTime.now()) + " >>> [Account edited]\n";

                updateDecripterAccountSelector();
            }
        }
    }                                                   

    private void DecripterDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                      
        if (selectedItemInComboBox(DecripterAccountSelector) >= 0) {
            //when the deleteCounter is true it means that the user has confirmed the elimination
            if (delete_counter) {
                delete_counter = false;
                if (login_account.getSoftware().equals("e")) {
                    DecripterDeleteButton.setText("Delete");
                } else if (login_account.getSoftware().equals("i")) {
                    DecripterDeleteButton.setText("Elimina");
                }
                DecripterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
                DecripterDeleteButton.repaint();

                //removes the selected account from the list
                account_list.remove(selectedItemInComboBox(DecripterAccountSelector));

                if (account_list.isEmpty()) {
                    MenuBar.setVisible(false);
                }

                log_history += dtf.format(LocalDateTime.now()) + " >>> [Account deleted]\n";

                updateDecripterAccountSelector();
            } else {
                delete_counter = true;

                if (login_account.getSoftware().equals("e")) {
                    DecripterDeleteButton.setText("Sure?");
                } else if (login_account.getSoftware().equals("i")) {
                    DecripterDeleteButton.setText("Sicuro?");
                }

                DecripterDeleteButton.setBackground(new java.awt.Color(242, 65, 65));
                DecripterDeleteButton.repaint();
            }
        }
    }                                                     

    private void DecripterAccountSelectorActionPerformed(java.awt.event.ActionEvent evt) {                                                         
        delete_counter = false;
        if (login_account.getSoftware().equals("e")) {
            DecripterDeleteButton.setText("Delete");
        } else if (login_account.getSoftware().equals("i")) {
            DecripterDeleteButton.setText("Elimina");
        }
        DecripterDeleteButton.setBackground(new java.awt.Color(38, 38, 38));
        DecripterDeleteButton.repaint();

        if (selectedItemInComboBox(DecripterAccountSelector) >= 0) {
            //shows the software, username and account of the selected item
            DecripterSoftwareTextField.setText(account_list.get(selectedItemInComboBox(DecripterAccountSelector)).getSoftware());
            DecripterSoftwareTextField.repaint();

            DecripterUsernameTextField.setText(account_list.get(selectedItemInComboBox(DecripterAccountSelector)).getUsername());
            DecripterUsernameTextField.repaint();

            DecripterPasswordTextField.setText(account_list.get(selectedItemInComboBox(DecripterAccountSelector)).getPassword());
            DecripterPasswordTextField.repaint();
        } else {
            DecripterSoftwareTextField.setText("");
            DecripterSoftwareTextField.repaint();

            DecripterUsernameTextField.setText("");
            DecripterUsernameTextField.repaint();

            DecripterPasswordTextField.setText("");
            DecripterPasswordTextField.repaint();
        }
    }                                                        

    private void FirstRunConfirmButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                      
        //gets the login password
        String login_password = FirstRunLoginpasswordTextField.getText();

        //checks if all is inserted and EULA are accepted
        if (selectedItemInComboBox(FirstRunLanguageSelector) >= 0 && selectedItemInComboBox(FirstRunSavingorderSelector) >= 0 && !(login_password.isBlank()) && FirstRunAccepteulaCheckBox.isSelected()) {
            String language = "", saveOrder = "";

            //translates the index into the actual language
            switch (selectedItemInComboBox(FirstRunLanguageSelector)) {
                case 0 ->
                    language = "e";
                case 1 ->
                    language = "i";
            }

            //translates the index into the actual saving order
            switch (selectedItemInComboBox(FirstRunSavingorderSelector)) {
                case 0 ->
                    saveOrder = "s";
                case 1 ->
                    saveOrder = "u";
            }

            //saves all in the new login account
            login_account = createAccount(language, saveOrder, login_password);

            log_history += dtf.format(LocalDateTime.now()) + " >>> [First Run successful, accepted EULA)]\n";

            if (login_account.getSoftware().equals("e")) {
                EncripterButton.setText("Encripter");
                DecripterButton.setText("Decripter");
                SettingsButton.setText("Settings");
                LogHistoryButton.setText("Log History");
                EulaAndCreditsButton.setText("EULA and Credits");
            } else if (login_account.getSoftware().equals("i")) {
                EncripterButton.setText("Crittografa");
                DecripterButton.setText("Decifra");
                SettingsButton.setText("Impostazioni");
                LogHistoryButton.setText("Cronologia Registro");
                EulaAndCreditsButton.setText("Termini e Crediti");
            }
            EncripterButton.repaint();
            DecripterButton.repaint();
            SettingsButton.repaint();
            LogHistoryButton.repaint();
            EulaAndCreditsButton.repaint();

            replacePanel(MainPanel, ProgramPanel);
        }
    }                                                     

    private void SettingsSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                   
        String password = SettingsLoginpasswordTextField.getText();

        if (!password.isBlank()) {
            //translates the index into the actual language
            if (SettingsLanguageSelector.getSelectedIndex() == 0) {
                login_account.setSoftware("e");
            } else if (SettingsLanguageSelector.getSelectedIndex() == 1) {
                login_account.setSoftware("i");
            }

            //translates the index into the actual saving order
            if (SettingsSavingorderSelector.getSelectedIndex() == 0) {
                login_account.setUsername("s");
            } else if (SettingsSavingorderSelector.getSelectedIndex() == 1) {
                login_account.setUsername("u");
            }
            resortAccountList();

            //gets and saves the new login password
            login_account.setPassword(password);

            log_history += dtf.format(LocalDateTime.now()) + " >>> [Settings changed]\n";

            //reloads Program Panel
            if (login_account.getSoftware().equals("e")) {
                EncripterButton.setText("Encripter");
                DecripterButton.setText("Decripter");
                SettingsButton.setText("Settings");
                LogHistoryButton.setText("Log History");
                EulaAndCreditsButton.setText("EULA and Credits");
            } else if (login_account.getSoftware().equals("i")) {
                EncripterButton.setText("Crittografa");
                DecripterButton.setText("Decifra");
                SettingsButton.setText("Impostazioni");
                LogHistoryButton.setText("Cronologia Registro");
                EulaAndCreditsButton.setText("Termini e Crediti");
            }
            EncripterButton.repaint();
            DecripterButton.repaint();
            SettingsButton.repaint();
            LogHistoryButton.repaint();
            EulaAndCreditsButton.repaint();
            replacePanel(DialogPanel, DialogBlankPanel);
        }
    }                                                  

    private void htmlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                             
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.html")) {
            file.write("<!DOCTYPE html>\n<html>\n<style>\n");

            //css
            file.write("body {background-color: rgb(51,51,51); color: rgb(204,204,204)}\n"
                    + "table, th, td {border: 0.1em solid rgb(204,204,204); border-collapse: collapse}\n"
                    + "th, td {padding: 1em}");

            file.write("\n</style>\n\n<body>\n<table style=\"width:100%\">");

            if (login_account.getSoftware().equals("e")) {
                file.write("<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Username</th>\n<th>Password</th>\n</tr>");
            } else if (login_account.getSoftware().equals("i")) {
                file.write("<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Nome Utente</th>\n<th>Password</th>\n</tr>");
            }

            int counter = 0;
            for (Account currentAccount : account_list) {
                counter++;
                file.write("<tr>\n<td>" + counter + "</td>\n<td>" + currentAccount.getSoftware() + "</td>\n<td>" + currentAccount.getUsername() + "</td>\n<td>" + currentAccount.getPassword() + "</td>\n</tr>");
            }

            file.write("</table>\n</body>\n</html>");

            file.flush();
            file.close();
        } catch (IOException e) {
            log_history += dtf.format(LocalDateTime.now()) + " >>> {Error: " + e.getMessage() + "}\n";
        }
    }                                            

    private void csvMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                            
        try (FileWriter file = new FileWriter(System.getProperty("user.home") + "\\Desktop\\Passswords.csv")) {
            int counter = 0;
            for (Account currentAccount : account_list) {
                counter++;
                file.write(counter + "," + currentAccount.getSoftware() + "," + currentAccount.getUsername() + "," + currentAccount.getPassword() + "\n");
            }

            file.flush();
            file.close();
        } catch (IOException e) {
            log_history += dtf.format(LocalDateTime.now()) + " >>> {Error: " + e.getMessage() + "}\n";
        }
    }                                           

    //custom methods
    /**
     * Adds an account to the account list ordinately.
     *
     * @param accountObj The account that will be added to the list.
     */
    private void addAccount(Account accountObj) {
        this.account_list.add(accountObj);
        resortAccountList();
    }

    /**
     * Creates a password-cripted account with the given informations.
     *
     * @param software The account's software.
     * @param username The account's username.
     * @param password The account's password, will be saved cripted.
     * @return The created account.
     */
    private Account createAccount(String software, String username, String password) {
        //creates the account, adding its attributes by constructor
        Account account = new Account(software, username, password);
        return account;
    }

    /**
     * Replaces a panel with another one.
     *
     * @param actingPanel The panel on which will be done the replacement
     * @param showPanel The new panel
     */
    private void replacePanel(JPanel actingPanel, JPanel showPanel) {
        //removing old panel
        actingPanel.removeAll();
        actingPanel.repaint();
        actingPanel.revalidate();

        //adding new panel
        actingPanel.add(showPanel);
        actingPanel.repaint();
        actingPanel.revalidate();
    }

    /**
     * Redirects to the login or first run procedure, based on the password file
     * existence.
     */
    private void run() {
        File data_file = new File(file_path + "passwords.psmg");
        data_file.getParentFile().mkdirs();

        //if the data file exists, it will try tro read its contents
        if (data_file.exists()) {
            try (FileInputStream f = new FileInputStream(file_path + "passwords.psmg")) {
                ObjectInputStream fIN = new ObjectInputStream(f);
                login_account = (Account) fIN.readObject();
                account_list = extractAccountList(fIN);
                f.close();
            } catch (IOException | ClassNotFoundException e) {
                log_history += dtf.format(LocalDateTime.now()) + " >>> {Error: " + e.getMessage() + "}\n";
            }
        }

        //loginAccount is never initialized, so if it is null, it means that this is the first run
        if (login_account != null) {
            //gets the log history
            try (Scanner scanner = new Scanner(new FileReader(file_path + "report.log"))) {
                while (true) {
                    log_history += scanner.nextLine() + "\n";
                    if (!scanner.hasNextLine()) {
                        break;
                    }
                }
                scanner.close();
            } catch (IOException e) {
                log_history += dtf.format(LocalDateTime.now()) + " >>> {Error: " + e.getMessage() + "}\n";
            }

            if (login_account.getSoftware().equals("e")) {
                LoginLabel.setText("Login");
                LoginButton.setText("Login");
            } else if (login_account.getSoftware().equals("i")) {
                LoginLabel.setText("Accesso");
                LoginButton.setText("Accesso");
            }
            LoginLabel.repaint();
            LoginButton.repaint();

            //redirects to login panel
            replacePanel(MainPanel, LoginPanel);
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

            //redirects to first run panel
            replacePanel(MainPanel, FirstRunPanel);
        }
    }

    @SuppressWarnings("unchecked")
    private AccountArrayList<Account> extractAccountList(ObjectInputStream fIN) throws IOException, ClassNotFoundException {
        return (AccountArrayList<Account>) fIN.readObject();
    }

    /**
     * Resorts the current account list.
     */
    private void resortAccountList() {
        if ("s".equals(this.login_account.getUsername())) {
            this.account_list.sort((Account acc1, Account acc2) -> {
                var software = acc1.getSoftware().compareTo(acc2.getSoftware());
                var username = acc1.getUsername().compareTo(acc2.getUsername());

                if (software == 0) {
                    return username;
                } else {
                    return software;
                }
            });
        } else if ("u".equals(this.login_account.getUsername())) {
            this.account_list.sort((Account acc1, Account acc2) -> {
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

    /**
     * Returns the selected index in a Combo Box with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @return the index of the current selected index
     */
    private int selectedItemInComboBox(javax.swing.JComboBox<String> comboBox) {
        return (comboBox.getSelectedIndex() - 1);
    }

    /**
     * Updates the DecripterAccountSelector.
     */
    private void updateDecripterAccountSelector() {
        //clears the account selector of the decripter and rewrites it
        DecripterAccountSelector.removeAllItems();
        DecripterAccountSelector.addItem("");

        for (int i = 0; i < this.account_list.size(); i++) {
            DecripterAccountSelector.addItem((i + 1) + ") " + this.account_list.get(i).getSoftware());
        }

        DecripterAccountSelector.repaint();

        //clears the text fields of the decripter
        DecripterSoftwareTextField.setText("");
        DecripterSoftwareTextField.repaint();

        DecripterUsernameTextField.setText("");
        DecripterUsernameTextField.repaint();

        DecripterPasswordTextField.setText("");
        DecripterPasswordTextField.repaint();
    }

    // Variables declaration - do not modify                     
    private javax.swing.JPanel ButttonPanel;
    private javax.swing.JLabel CreditsLabel;
    private javax.swing.JTextArea CreditsTextArea;
    private javax.swing.JComboBox<String> DecripterAccountSelector;
    private javax.swing.JButton DecripterButton;
    private javax.swing.JButton DecripterDeleteButton;
    private javax.swing.JPanel DecripterPanel;
    private javax.swing.JLabel DecripterPasswordLabel;
    private javax.swing.JTextField DecripterPasswordTextField;
    private javax.swing.JButton DecripterSaveButton;
    private javax.swing.JSeparator DecripterSeparator;
    private javax.swing.JLabel DecripterSoftwareLabel;
    private javax.swing.JTextField DecripterSoftwareTextField;
    private javax.swing.JLabel DecripterUsernameLabel;
    private javax.swing.JTextField DecripterUsernameTextField;
    private javax.swing.JPanel DialogBlankPanel;
    private javax.swing.JPanel DialogPanel;
    private javax.swing.JLabel EULALabel;
    private javax.swing.JTextArea EULATextArea;
    private javax.swing.JButton EncripterButton;
    private javax.swing.JPanel EncripterPanel;
    private javax.swing.JLabel EncripterPasswordLabel;
    private javax.swing.JTextField EncripterPasswordTextField;
    private javax.swing.JButton EncripterSaveButton;
    private javax.swing.JLabel EncripterSoftwareLabel;
    private javax.swing.JTextField EncripterSoftwareTextField;
    private javax.swing.JLabel EncripterUsernameLabel;
    private javax.swing.JTextField EncripterUsernameTextField;
    private javax.swing.JButton EulaAndCreditsButton;
    private javax.swing.JPanel EulaAndCreditsPanel;
    private javax.swing.JMenu ExportAsMenu;
    private javax.swing.JCheckBox FirstRunAccepteulaCheckBox;
    private javax.swing.JButton FirstRunConfirmButton;
    private javax.swing.JTextArea FirstRunEULATextArea;
    private javax.swing.JLabel FirstRunLabel;
    private javax.swing.JLabel FirstRunLabel2;
    private javax.swing.JLabel FirstRunLanguageLabel;
    private javax.swing.JComboBox<String> FirstRunLanguageSelector;
    private javax.swing.JLabel FirstRunLoginpassowrdLabel;
    private javax.swing.JTextField FirstRunLoginpasswordTextField;
    private javax.swing.JPanel FirstRunPanel;
    private javax.swing.JLabel FirstRunSavingorderLabel;
    private javax.swing.JComboBox<String> FirstRunSavingorderSelector;
    private javax.swing.JSeparator FirstRunSeparator;
    private javax.swing.JSeparator FirstRunSeparator1;
    private javax.swing.JButton LogHistoryButton;
    private javax.swing.JPanel LogHistoryPanel;
    private javax.swing.JScrollPane LogHistoryScrollPane;
    private javax.swing.JTextArea LogHistoryTextArea;
    private javax.swing.JLabel LogHystoryLabel;
    private javax.swing.JLabel LogLegendLabel;
    private javax.swing.JButton LoginButton;
    private javax.swing.JLabel LoginLabel;
    private javax.swing.JPanel LoginPanel;
    private javax.swing.JPasswordField LoginPasswordField;
    private javax.swing.JLabel LoginPasswordLabel;
    private javax.swing.JLabel LoginUnsuccesfulLabel;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JLabel PasswordManagerLabel;
    private javax.swing.JPanel ProgramPanel;
    private javax.swing.JButton SettingsButton;
    private javax.swing.JLabel SettingsLanguageLabel;
    private javax.swing.JComboBox<String> SettingsLanguageSelector;
    private javax.swing.JLabel SettingsLoginpassowrdLabel;
    private javax.swing.JTextField SettingsLoginpasswordTextField;
    private javax.swing.JPanel SettingsPanel;
    private javax.swing.JButton SettingsSaveButton;
    private javax.swing.JLabel SettingsSavingorderLabel;
    private javax.swing.JComboBox<String> SettingsSavingorderSelector;
    private javax.swing.JMenuItem csvMenuItem;
    private javax.swing.JMenuItem htmlMenuItem;
    // End of variables declaration                   
}
