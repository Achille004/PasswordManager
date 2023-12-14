package main.views;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.Main;
import static main.utils.Utils.*;

public class SettingsPanel extends JPanel {

    private JButton SaveButton;
    private JComboBox<String> LanguageSelector, SavingOrderSelector;
    private JLabel LanguageLabel, LoginPasswordLabel, SavingOrderLabel;
    private JTextField SettingsLoginPasswordTextField;

    private final Main appInstance;

    public SettingsPanel(Main appInstance) {
        super();
        this.appInstance = appInstance;

        initComponents();
    }

    public void load(String[] languageSelectorItems, String[] savingOrderSelectorItems,
            String languageLabelText, String savingOrderLabelText, String loginPasswordLabelText,
            String confirmButtonText, String loginPassword) {

        setComboBoxItems(LanguageSelector, languageSelectorItems);
        setComboBoxItems(SavingOrderSelector, savingOrderSelectorItems);

        LanguageLabel.setText(languageLabelText + ":");
        SavingOrderLabel.setText(savingOrderLabelText + ":");
        LoginPasswordLabel.setText(loginPasswordLabelText + ":");
        SaveButton.setText(confirmButtonText);

        // sets the current language
        String language = appInstance.getLoginAccount().getLanguage();
        LanguageSelector.setSelectedIndex(
                switch (language) {
                    case "e" -> 0;
                    case "i" -> 1;
                    default -> throw new IllegalArgumentException("Invalid language: " + language);
                });

        // sets the current saving order
        String savingOrder = appInstance.getLoginAccount().getSavingOrder();
        SavingOrderSelector.setSelectedIndex(
                switch (savingOrder) {
                    case "s" -> 0;
                    case "u" -> 1;
                    default -> throw new IllegalArgumentException("Invalid saving order: " + savingOrder);
                });

        // sets the current login password
        SettingsLoginPasswordTextField.setText(loginPassword);

        repaintAll(LanguageSelector, SavingOrderSelector, SettingsLoginPasswordTextField);
    }

    private void SaveButtonActionPerformed(ActionEvent evt) {
        String newLoginPassword = SettingsLoginPasswordTextField.getText();

        if (!newLoginPassword.isBlank()) {
            // translates the index into the actual language
            String language = switch (LanguageSelector.getSelectedIndex()) {
                case 0 -> "e";
                case 1 -> "i";
                default -> throw new IllegalArgumentException("Invalid value.");
            };

            // translates the index into the actual saving order
            String savingOrder = switch (SavingOrderSelector.getSelectedIndex()) {
                case 0 -> "s";
                case 1 -> "u";
                default -> throw new IllegalArgumentException("Invalid value.");
            };

            appInstance.setLoginAccount(savingOrder, language, newLoginPassword);

            appInstance.getLogger().addInfo("Settings changed");

            // reloads Program Panel
            appInstance.SettingsButtonActionPerformed(evt);
        }
    }

    private void initComponents() {
        LanguageSelector = new JComboBox<>();
        LanguageLabel = new JLabel();
        SavingOrderLabel = new JLabel();
        LoginPasswordLabel = new JLabel();
        SettingsLoginPasswordTextField = new JTextField();
        SaveButton = new JButton();
        SavingOrderSelector = new JComboBox<>();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));
        setToolTipText("");

        LanguageSelector.setBackground(new Color(38, 38, 38));
        LanguageSelector.setForeground(new Color(242, 242, 242));
        LanguageSelector.setModel(
                new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        LanguageLabel.setBackground(new Color(38, 38, 38));
        LanguageLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LanguageLabel.setForeground(new Color(242, 242, 242));
        LanguageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        LanguageLabel.setText("Language:");

        SavingOrderLabel.setBackground(new Color(38, 38, 38));
        SavingOrderLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SavingOrderLabel.setForeground(new Color(242, 242, 242));
        SavingOrderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        SavingOrderLabel.setText("Saving order:");

        LoginPasswordLabel.setBackground(new Color(38, 38, 38));
        LoginPasswordLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        LoginPasswordLabel.setForeground(new Color(242, 242, 242));
        LoginPasswordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        LoginPasswordLabel.setText("Login password:");

        SettingsLoginPasswordTextField.setBackground(new Color(242, 65, 65));
        SettingsLoginPasswordTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SettingsLoginPasswordTextField.setForeground(new Color(38, 38, 38));
        SettingsLoginPasswordTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        SaveButton.setBackground(new Color(38, 38, 38));
        SaveButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SaveButton.setForeground(new Color(242, 242, 242));
        SaveButton.setText("Confirm");
        SaveButton.setOpaque(false);
        SaveButton.addActionListener(this::SaveButtonActionPerformed);

        SavingOrderSelector.setBackground(new Color(38, 38, 38));
        SavingOrderSelector.setForeground(new Color(242, 242, 242));
        SavingOrderSelector.setModel(
                new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        GroupLayout SettingsPanelLayout = new GroupLayout(this);
        setLayout(SettingsPanelLayout);
        SettingsPanelLayout.setHorizontalGroup(
                SettingsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(SettingsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(SavingOrderLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SaveButton, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(LanguageLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(LoginPasswordLabel, GroupLayout.DEFAULT_SIZE,
                                                168, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(SavingOrderSelector,
                                                GroupLayout.Alignment.TRAILING, 0, 859, Short.MAX_VALUE)
                                        .addComponent(SettingsLoginPasswordTextField,
                                                GroupLayout.Alignment.TRAILING)
                                        .addComponent(LanguageSelector, 0, GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE))
                                .addGap(50, 50, 50)));
        SettingsPanelLayout.setVerticalGroup(
                SettingsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(SettingsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(LanguageLabel, GroupLayout.PREFERRED_SIZE, 31,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LanguageSelector, GroupLayout.PREFERRED_SIZE,
                                                32, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(SavingOrderLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(SavingOrderSelector,
                                                GroupLayout.PREFERRED_SIZE, 32,
                                                GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(SettingsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(SettingsLoginPasswordTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LoginPasswordLabel,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(SaveButton)
                                .addGap(50, 50, 50)));

    }
}
