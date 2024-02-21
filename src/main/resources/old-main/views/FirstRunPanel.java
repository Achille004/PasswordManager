package main.views;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.Main;
import static main.utils.Utils.*;

public class FirstRunPanel extends JPanel {

    private JCheckBox AcceptEulaCheckBox;
    private JComboBox<String> LanguageSelector;
    private JTextField LoginPasswordTextField;
    private JComboBox<String> SavingOrderSelector;

    private final Main appInstance;

    public FirstRunPanel(Main appInstance) {
        super();
        this.appInstance = appInstance;

        initComponents();

        LanguageSelector.removeAllItems();
        LanguageSelector.addItem("");
        LanguageSelector.addItem("English");
        LanguageSelector.addItem("Italian");

        SavingOrderSelector.removeAllItems();
        SavingOrderSelector.addItem("");
        SavingOrderSelector.addItem("Software");
        SavingOrderSelector.addItem("Username");

        repaintAll(LanguageSelector, SavingOrderSelector);
    }

    private void ConfirmButtonActionPerformed(ActionEvent evt) {
        // gets the login password
        String loginPassword = LoginPasswordTextField.getText();

        if (!loginPassword.isBlank()) {
            int languageSelectorIndex = selectedItemInComboBox(LanguageSelector);
            int savingOrderSelectorIndex= selectedItemInComboBox(SavingOrderSelector);

            // checks if all is inserted and EULA is accepted
            if (languageSelectorIndex == -1 || savingOrderSelectorIndex == -1
                || loginPassword.isBlank() || !AcceptEulaCheckBox.isSelected()) {
                return;
            }



            // translates the index into the actual language
            String language = switch (languageSelectorIndex) {
                case 0 ->  "e";
                case 1 ->  "i";
                default -> throw new IllegalArgumentException("Invalid language.");
            };

            // translates the index into the actual saving order
            String savingOrder = switch (savingOrderSelectorIndex) {
                case 0 -> "s";
                case 1 -> "u";
                default -> throw new IllegalArgumentException("Invalid saving order.");
            };

            // saves all in the new login account
            appInstance.setLoginAccount(savingOrder, language, loginPassword);
            appInstance.getLogger().addInfo("First Run successful, accepted EULA)");

            appInstance.switchToProgramPanel(loginPassword);
        }
    }

    private void initComponents() {
        JButton ConfirmButton;
        JLabel Header1, Header2, LanguageLabel, LoginPassowrdLabel, SavingorderLabel;
        JSeparator Separator1, Separator2;
        JTextArea EulaTextArea;

        LanguageLabel = new JLabel();
        SavingorderLabel = new JLabel();
        LoginPassowrdLabel = new JLabel();
        Separator1 = new JSeparator();
        ConfirmButton = new JButton();
        Header1 = new JLabel();
        Header2 = new JLabel();
        Separator2 = new JSeparator();
        EulaTextArea = new JTextArea();
        LanguageSelector = new JComboBox<>();
        LoginPasswordTextField = new JTextField();
        SavingOrderSelector = new JComboBox<>();
        AcceptEulaCheckBox = new JCheckBox();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));
        setToolTipText("");

        LanguageSelector.setBackground(new Color(38, 38, 38));
        LanguageSelector.setForeground(new Color(242, 242, 242));
        LanguageSelector.setModel(
                new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        LanguageSelector.setBorder(null);

        LanguageLabel.setBackground(new Color(38, 38, 38));
        LanguageLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LanguageLabel.setForeground(new Color(242, 242, 242));
        LanguageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        LanguageLabel.setText("Language:");

        SavingorderLabel.setBackground(new Color(38, 38, 38));
        SavingorderLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SavingorderLabel.setForeground(new Color(242, 242, 242));
        SavingorderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        SavingorderLabel.setText("Saving order:");

        LoginPassowrdLabel.setBackground(new Color(38, 38, 38));
        LoginPassowrdLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginPassowrdLabel.setForeground(new Color(242, 242, 242));
        LoginPassowrdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        LoginPassowrdLabel.setText("Login password:");

        LoginPasswordTextField.setBackground(new Color(242, 65, 65));
        LoginPasswordTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginPasswordTextField.setForeground(new Color(38, 38, 38));
        LoginPasswordTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        Separator1.setBackground(new Color(38, 38, 38));
        Separator1.setForeground(new Color(242, 242, 242));

        ConfirmButton.setBackground(new Color(38, 38, 38));
        ConfirmButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        ConfirmButton.setForeground(new Color(242, 242, 242));
        ConfirmButton.setText("Confirm");
        ConfirmButton.setOpaque(false);
        ConfirmButton.addActionListener(this::ConfirmButtonActionPerformed);

        Header1.setBackground(new Color(38, 38, 38));
        Header1.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        Header1.setForeground(new Color(242, 242, 242));
        Header1.setHorizontalAlignment(SwingConstants.LEFT);
        Header1.setText("Hi!");

        Header2.setBackground(new Color(38, 38, 38));
        Header2.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        Header2.setForeground(new Color(242, 242, 242));
        Header2.setHorizontalAlignment(SwingConstants.LEFT);
        Header2.setText(
                "It's the developer here, before using this program i need you to insert some personalization informations and a password to protect your accounts.");

        SavingOrderSelector.setBackground(new Color(38, 38, 38));
        SavingOrderSelector.setForeground(new Color(242, 242, 242));
        SavingOrderSelector.setModel(
                new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        SavingOrderSelector.setBorder(null);

        Separator2.setBackground(new Color(38, 38, 38));
        Separator2.setForeground(new Color(242, 242, 242));

        AcceptEulaCheckBox.setBackground(new Color(38, 38, 38));
        AcceptEulaCheckBox.setForeground(new Color(242, 242, 242));
        AcceptEulaCheckBox.setText("Accept EULA");

        EulaTextArea.setEditable(false);
        EulaTextArea.setBackground(new Color(242, 65, 65));
        EulaTextArea.setColumns(20);
        EulaTextArea.setFont(new Font("Dialog", Font.PLAIN, 24)); // NOI18N
        EulaTextArea.setForeground(new Color(38, 38, 38));
        EulaTextArea.setRows(5);
        EulaTextArea.setText(
                "Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022-2023  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License \nas published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\nThis program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied \nwarranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for \nmore details.\n\nYou should have received a copy of the GNU General Public License along with this program.\nIf not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        EulaTextArea.setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        GroupLayout FirstRunPanelLayout = new GroupLayout(this);
        setLayout(FirstRunPanelLayout);
        FirstRunPanelLayout.setHorizontalGroup(
                FirstRunPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, FirstRunPanelLayout
                                .createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(AcceptEulaCheckBox,
                                                        GroupLayout.PREFERRED_SIZE, 102,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(ConfirmButton,
                                                        GroupLayout.PREFERRED_SIZE, 102,
                                                        GroupLayout.PREFERRED_SIZE))
                                        .addComponent(Separator2, GroupLayout.Alignment.TRAILING)
                                        .addComponent(Separator1, GroupLayout.Alignment.TRAILING)
                                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                                .addGroup(FirstRunPanelLayout
                                                        .createParallelGroup(GroupLayout.Alignment.TRAILING,
                                                                false)
                                                        .addComponent(SavingorderLabel,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(LoginPassowrdLabel,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(LanguageLabel,
                                                                GroupLayout.Alignment.LEADING,
                                                                GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGap(18, 18, 18)
                                                .addGroup(FirstRunPanelLayout
                                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(LoginPasswordTextField)
                                                        .addComponent(LanguageSelector, 0,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(SavingOrderSelector, 0,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addComponent(Header1, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(Header2, GroupLayout.DEFAULT_SIZE, 1307,
                                                Short.MAX_VALUE)
                                        .addComponent(EulaTextArea))
                                .addGap(50, 50, 50)));
        FirstRunPanelLayout.setVerticalGroup(
                FirstRunPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(FirstRunPanelLayout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(Header1, GroupLayout.PREFERRED_SIZE, 33,
                                        GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(Header2, GroupLayout.PREFERRED_SIZE, 33,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(Separator1, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(LanguageLabel, GroupLayout.PREFERRED_SIZE, 31,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LanguageSelector, GroupLayout.PREFERRED_SIZE,
                                                32, GroupLayout.PREFERRED_SIZE))
                                .addGap(17, 17, 17)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(SavingorderLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SavingOrderSelector,
                                                GroupLayout.PREFERRED_SIZE, 32,
                                                GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(LoginPasswordTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LoginPassowrdLabel,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(Separator2, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(EulaTextArea, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(FirstRunPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ConfirmButton)
                                        .addComponent(AcceptEulaCheckBox,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE))
                                .addGap(50, 50, 50)));
    }
}
