package main.views;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.Main;
import static main.utils.Utils.*;

public class EncrypterPanel extends JPanel {

    private JTextField PasswordTextField;
    private JButton SaveButton;
    private JTextField SoftwareTextField;
    private JLabel UsernameLabel;
    private JTextField UsernameTextField;

    private final App appInstance;

    public EncrypterPanel(App appInstance) {
        super();
        this.appInstance = appInstance;

        initComponents();
    }

    public void load(String saveButtonText, String usernameLabelText) {
        SaveButton.setText(saveButtonText);
        UsernameLabel.setText(usernameLabelText + ":");
    }

    private void SaveButtonActionPerformed(ActionEvent evt) {
        
    }

    private void initComponents() {
        JLabel SoftwareLabel, PasswordLabel;

        SoftwareLabel = new JLabel();
        SoftwareTextField = new JTextField();
        UsernameLabel = new JLabel();
        UsernameTextField = new JTextField();
        PasswordLabel = new JLabel();
        PasswordTextField = new JTextField();
        SaveButton = new JButton();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));
        setToolTipText("");

        SoftwareLabel.setBackground(new Color(38, 38, 38));
        SoftwareLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SoftwareLabel.setForeground(new Color(242, 242, 242));
        SoftwareLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        SoftwareLabel.setText("Software:");

        SoftwareTextField.setBackground(new Color(242, 65, 65));
        SoftwareTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SoftwareTextField.setForeground(new Color(38, 38, 38));
        SoftwareTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        UsernameLabel.setBackground(new Color(38, 38, 38));
        UsernameLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        UsernameLabel.setForeground(new Color(242, 242, 242));
        UsernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        UsernameLabel.setText("Username:");

        UsernameTextField.setBackground(new Color(242, 65, 65));
        UsernameTextField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        UsernameTextField.setForeground(new Color(38, 38, 38));
        UsernameTextField
                .setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

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

        SaveButton.setBackground(new Color(38, 38, 38));
        SaveButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        SaveButton.setForeground(new Color(242, 242, 242));
        SaveButton.setText("Save");
        SaveButton.setOpaque(false);
        SaveButton.addActionListener(this::SaveButtonActionPerformed);

        GroupLayout EncrypterPanelLayout = new GroupLayout(this);
        setLayout(EncrypterPanelLayout);
        EncrypterPanelLayout.setHorizontalGroup(
                EncrypterPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(EncrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(PasswordLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(SoftwareLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(UsernameLabel,
                                                GroupLayout.Alignment.TRAILING,
                                                GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                                        .addComponent(SaveButton, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(SoftwareTextField,
                                                GroupLayout.Alignment.TRAILING,
                                                GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
                                        .addComponent(PasswordTextField)
                                        .addComponent(UsernameTextField,
                                                GroupLayout.Alignment.TRAILING))
                                .addGap(50, 50, 50)));
        EncrypterPanelLayout.setVerticalGroup(
                EncrypterPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(EncrypterPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(SoftwareLabel, GroupLayout.DEFAULT_SIZE, 31,
                                                Short.MAX_VALUE)
                                        .addComponent(SoftwareTextField))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(UsernameTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(UsernameLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(EncrypterPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(PasswordTextField,
                                                GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PasswordLabel, GroupLayout.PREFERRED_SIZE,
                                                33, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(SaveButton)
                                .addGap(473, 473, 473)));
    }
}
