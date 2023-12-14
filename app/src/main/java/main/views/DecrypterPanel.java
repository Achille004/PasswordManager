package main.views;

import java.util.ArrayList;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.Main;
import main.security.*;
import static main.utils.Utils.*;

public class DecrypterPanel extends JPanel {

    private boolean deleteCounter;

    private JButton DeleteButton, SaveButton;
    private JComboBox<String> AccountSelector;
    private JLabel UsernameLabel;
    private JTextField PasswordTextField, SoftwareTextField, UsernameTextField;

    private final Main appInstance;

    public DecrypterPanel(Main appInstance) {
        super();
        this.appInstance = appInstance;

        deleteCounter = false;

        initComponents();
    }

    public void load(String deleteButtonText, String saveButtonText, String usernameLabelText) {
        DeleteButton.setText(deleteButtonText);
        SaveButton.setText(saveButtonText);
        UsernameLabel.setText(usernameLabelText + ":");

        DeleteButton.setBackground(new Color(51, 51, 51));
        DeleteButton.repaint();

        // to make sure the decrypter delete button is in its first state
        deleteCounter = false;

        updateDecrypterAccountSelector();
    }

    private void DecrypterAccountSelectorActionPerformed(ActionEvent evt) {
        deleteCounter = false;
        switch (appInstance.getLoginAccount().getLanguage()) {
            case "e" -> DeleteButton.setText("Delete");
            case "i" -> DeleteButton.setText("Elimina");
            default ->
                throw new IllegalArgumentException("Invalid language: " + appInstance.getLoginAccount().getLanguage());
        }
        DeleteButton.setBackground(new Color(38, 38, 38));

        if (selectedItemInComboBox(AccountSelector) >= 0) {
            // gets the selected account
            Account selectedAcc = appInstance.getAccountList().get(selectedItemInComboBox(AccountSelector));

            // shows the software, username and account of the selected account
            SoftwareTextField.setText(selectedAcc.getSoftware());
            UsernameTextField.setText(selectedAcc.getUsername());
            PasswordTextField.setText(appInstance.getAccountPassword(selectedAcc));
        } else {
            SoftwareTextField.setText("");
            UsernameTextField.setText("");
            PasswordTextField.setText("");
        }

        repaintAll(DeleteButton, SoftwareTextField, UsernameTextField, PasswordTextField);
    }

    private void SaveButtonActionPerformed(ActionEvent evt) {
        if (selectedItemInComboBox(AccountSelector) >= 0) {
            // get the new software, username and password
            String software = SoftwareTextField.getText();
            String username = UsernameTextField.getText();
            String password = PasswordTextField.getText();

            if (!(software.isBlank() && username.isBlank() && password.isBlank())) {
                // save the new attributes of the account
                int index = selectedItemInComboBox(AccountSelector);
                appInstance.replaceAccount(index, software, username, password);

                updateDecrypterAccountSelector();
            }
        }
    }

    private void DeleteButtonActionPerformed(ActionEvent evt) {
        if (selectedItemInComboBox(AccountSelector) == -1) {
            return;
        }

        LoginAccount loginAccount = appInstance.getLoginAccount();

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (deleteCounter) {
            switch (appInstance.getLoginAccount().getLanguage()) {
                case "e" -> DeleteButton.setText("Delete");
                case "i" -> DeleteButton.setText("Elimina");
                default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
            }
            DeleteButton.setBackground(new Color(38, 38, 38));

            // removes the selected account from the list
            appInstance.deleteAccount(selectedItemInComboBox(AccountSelector));

            updateDecrypterAccountSelector();
        } else {
            switch (loginAccount.getLanguage()) {
                case "e" -> DeleteButton.setText("Sure?");
                case "i" -> DeleteButton.setText("Sicuro?");
                default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
            }

            DeleteButton.setBackground(new Color(219, 67, 67));
        }
        DeleteButton.repaint();

        deleteCounter = !deleteCounter;
    }

    /**
     * Updates the AccountSelector.
     */
    private void updateDecrypterAccountSelector() {
        ArrayList<Account> accountList = appInstance.getAccountList();
        LoginAccount loginAccount = appInstance.getLoginAccount();

        // clears the account selector of the decrypter and rewrites it
        AccountSelector.removeAllItems();
        AccountSelector.addItem("");

        final int listSize = accountList.size();
        for (int i = 0; i < listSize; i++) {
            Account account = accountList.get(i);
            String item = addZerosToIndex(listSize, i + 1) + ") ";
            switch (loginAccount.getSavingOrder()) {
                case "s" -> item += account.getSoftware() + " / " + account.getUsername();
                case "u" -> item += account.getUsername() + " / " + account.getSoftware();
                default -> throw new IllegalArgumentException("Invalid saving order: " + loginAccount.getSavingOrder());
            }
            AccountSelector.addItem(item);
        }

        // clears the text fields of the decrypter
        SoftwareTextField.setText("");
        UsernameTextField.setText("");
        PasswordTextField.setText("");

        repaintAll(AccountSelector, SoftwareTextField, UsernameTextField, PasswordTextField);
    }

    private void initComponents() {
        JLabel PasswordLabel, SoftwareLabel;
        JSeparator Separator;

        AccountSelector = new JComboBox<>();
        DeleteButton = new JButton();
        PasswordLabel = new JLabel();
        PasswordTextField = new JTextField();
        SaveButton = new JButton();
        Separator = new JSeparator();
        SoftwareLabel = new JLabel();
        SoftwareTextField = new JTextField();
        UsernameLabel = new JLabel();
        UsernameTextField = new JTextField();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));

        AccountSelector.setBackground(new Color(38, 38, 38));
        AccountSelector.setForeground(new Color(242, 242, 242));
        AccountSelector.setModel(
                new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        AccountSelector.addActionListener(this::DecrypterAccountSelectorActionPerformed);

        SoftwareLabel.setBackground(new Color(38, 38, 38));
        SoftwareLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SoftwareLabel.setForeground(new Color(242, 242, 242));
        SoftwareLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        SoftwareLabel.setText("Software:");

        UsernameLabel.setBackground(new Color(38, 38, 38));
        UsernameLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        UsernameLabel.setForeground(new Color(242, 242, 242));
        UsernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        UsernameLabel.setText("Username:");

        PasswordLabel.setBackground(new Color(38, 38, 38));
        PasswordLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        PasswordLabel.setForeground(new Color(242, 242, 242));
        PasswordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        PasswordLabel.setText("Password:");

        PasswordTextField.setBackground(new Color(242, 65, 65));
        PasswordTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        PasswordTextField.setForeground(new Color(38, 38, 38));
        PasswordTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        UsernameTextField.setBackground(new Color(242, 65, 65));
        UsernameTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        UsernameTextField.setForeground(new Color(38, 38, 38));
        UsernameTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        SoftwareTextField.setBackground(new Color(242, 65, 65));
        SoftwareTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SoftwareTextField.setForeground(new Color(38, 38, 38));
        SoftwareTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        Separator.setBackground(new Color(38, 38, 38));
        Separator.setForeground(new Color(242, 242, 242));

        SaveButton.setBackground(new Color(38, 38, 38));
        SaveButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SaveButton.setForeground(new Color(242, 242, 242));
        SaveButton.setText("Save");
        SaveButton.setOpaque(false);
        SaveButton.addActionListener(this::SaveButtonActionPerformed);

        DeleteButton.setBackground(new Color(38, 38, 38));
        DeleteButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        DeleteButton.setForeground(new Color(242, 242, 242));
        DeleteButton.setText("Delete");
        DeleteButton.setOpaque(false);
        DeleteButton.addActionListener(this::DeleteButtonActionPerformed);

        GroupLayout DecrypterPanelLayout = new GroupLayout(this);
        setLayout(DecrypterPanelLayout);
        DecrypterPanelLayout.setHorizontalGroup(
                DecrypterPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(Separator)
                                        .addComponent(AccountSelector,
                                                GroupLayout.Alignment.LEADING, 0,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(GroupLayout.Alignment.LEADING, DecrypterPanelLayout
                                                .createSequentialGroup()
                                                .addGroup(DecrypterPanelLayout
                                                        .createParallelGroup(GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(PasswordLabel,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(SoftwareLabel,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(UsernameLabel,
                                                                GroupLayout.Alignment.TRAILING,
                                                                GroupLayout.DEFAULT_SIZE, 107,
                                                                Short.MAX_VALUE)
                                                        .addComponent(DeleteButton,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(18, 18, 18)
                                                .addGroup(DecrypterPanelLayout
                                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(PasswordTextField,
                                                                GroupLayout.DEFAULT_SIZE, 920,
                                                                Short.MAX_VALUE)
                                                        .addComponent(SoftwareTextField)
                                                        .addComponent(UsernameTextField,
                                                                GroupLayout.Alignment.TRAILING)
                                                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                .addComponent(SaveButton,
                                                                        GroupLayout.PREFERRED_SIZE, 107,
                                                                        GroupLayout.PREFERRED_SIZE)))))
                                .addGap(50, 50, 50)));
        DecrypterPanelLayout.setVerticalGroup(
                DecrypterPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(DecrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(AccountSelector, GroupLayout.PREFERRED_SIZE, 32,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(Separator, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(SoftwareLabel, GroupLayout.DEFAULT_SIZE, 31,
                                                Short.MAX_VALUE)
                                        .addComponent(SoftwareTextField))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(UsernameTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(UsernameLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(PasswordTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PasswordLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(DecrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(DeleteButton, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SaveButton, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(50, 50, 50)));

    }
}
