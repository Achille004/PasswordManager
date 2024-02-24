package main.views;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import main.Main;
import main.security.LoginAccount;

public class LoginPanel extends JPanel {

    private ActionListener timerTask;
    private byte loginCounter;

    private JButton FormButton;
    private JLabel FormLabel;
    private JPasswordField FormPasswordField;
    private JLabel WrongPasswordLabel;

    private final Main appInstance;

    public LoginPanel(Main appInstance) {
        super();
        this.appInstance = appInstance;

        loginCounter = 0;
        timerTask = null;

        initComponents();
    }

    public void load(String formButtonText, String formLabelText) {
        FormButton.setText(formButtonText);
        FormLabel.setText(formLabelText);
    }

    private void FormButtonActionPerformed(ActionEvent evt) {
        LoginAccount loginAccount = appInstance.getLoginAccount();

        String loginPassword = new String(FormPasswordField.getPassword());
        FormPasswordField.setText("");

        String errorMessage = "";
        // To work around the closure capture error the array should be final or
        // effectively final so that the array doesn't change over time and doesn't
        // trigger the error, but yet still being able to modify its content.
        final boolean[] shouldExit = { false };

        if (loginPassword.isBlank()) {
            errorMessage = switch (loginAccount.getLanguage()) {
                case "eng" -> "No password entered.";
                case "ita" -> "Nessuna password inserita.";
                default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
            };
        } else {
            if (loginAccount.verifyPassword(loginPassword)) {
                appInstance.getLogger().addInfo("Successful login");

                appInstance.switchToProgramPanel(loginPassword);
                return;
            }

            // adds a failed attempt
            loginCounter++;

            if (loginCounter == 3) {
                errorMessage = switch (loginAccount.getLanguage()) {
                    case "eng" -> "A wrong password has been inserted three times, program shutting down...";
                    case "ita" -> "È stata inserita una password errata tre volte, programma in arresto...";
                    default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
                };
                shouldExit[0] = true;

                appInstance.getLogger().addInfo("Unsuccessful login");
                appInstance.getLogger().save();
            } else {
                errorMessage = switch (loginAccount.getLanguage()) {
                    case "eng" -> "Wrong password.";
                    case "ita" -> "Password errata.";
                    default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
                };
            }
        }

        WrongPasswordLabel.setText(errorMessage);
        FormButton.setEnabled(false);

        timerTask = (ActionEvent e) -> {
            FormButton.setEnabled(true);
            WrongPasswordLabel.setText("");

            if (shouldExit[0]) {
                System.exit(0);
            }
        };

        Timer timer = new Timer(shouldExit[0] ? 2000 : 800, timerTask);
        timer.setRepeats(false);
        timer.start();
    }

    private void initComponents() {
        JLabel FormPasswordLabel;

        FormLabel = new JLabel();
        FormPasswordField = new JPasswordField();
        FormPasswordLabel = new JLabel();
        FormButton = new JButton();
        WrongPasswordLabel = new JLabel();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));

        FormLabel.setBackground(new Color(38, 38, 38));
        FormLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        FormLabel.setForeground(new Color(242, 242, 242));
        FormLabel.setHorizontalAlignment(SwingConstants.LEFT);
        FormLabel.setText("Login");
        FormLabel.setToolTipText("");
        FormLabel.setEnabled(false);

        FormPasswordLabel.setBackground(new Color(38, 38, 38));
        FormPasswordLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        FormPasswordLabel.setForeground(new Color(242, 242, 242));
        FormPasswordLabel.setHorizontalAlignment(SwingConstants.LEFT);
        FormPasswordLabel.setText("Password:");

        FormPasswordField.setBackground(new Color(242, 65, 65));
        FormPasswordField.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        FormPasswordField.setForeground(new Color(38, 38, 38));
        FormPasswordField.setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        FormButton.setBackground(new Color(38, 38, 38));
        FormButton.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        FormButton.setForeground(new Color(242, 242, 242));
        FormButton.setText("Login");
        FormButton.setOpaque(false);
        FormButton.addActionListener(this::FormButtonActionPerformed);

        WrongPasswordLabel.setBackground(new Color(38, 38, 38));
        WrongPasswordLabel.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        WrongPasswordLabel.setForeground(new Color(242, 242, 242));

        GroupLayout LoginPanelLayout = new GroupLayout(this);
        setLayout(LoginPanelLayout);
        LoginPanelLayout.setHorizontalGroup(
                LoginPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(FormButton, GroupLayout.DEFAULT_SIZE, 126,
                                                Short.MAX_VALUE)
                                        .addComponent(FormPasswordLabel, GroupLayout.Alignment.TRAILING,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(FormLabel, GroupLayout.Alignment.TRAILING,
                                                GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                                .addGap(7, 7, 7)
                                                .addComponent(FormPasswordField,
                                                        GroupLayout.PREFERRED_SIZE, 590,
                                                        GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.TRAILING,
                                                LoginPanelLayout.createSequentialGroup()
                                                        .addPreferredGap(
                                                                LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(WrongPasswordLabel,
                                                                GroupLayout.PREFERRED_SIZE, 590,
                                                                GroupLayout.PREFERRED_SIZE)))
                                .addGap(490, 490, 490)));
        LoginPanelLayout.setVerticalGroup(
                LoginPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(LoginPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(FormLabel, GroupLayout.PREFERRED_SIZE, 47,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(FormPasswordLabel, GroupLayout.PREFERRED_SIZE, 33,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FormPasswordField, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(LoginPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(WrongPasswordLabel, GroupLayout.PREFERRED_SIZE, 32,
                                                GroupLayout.PREFERRED_SIZE)
                                        .addComponent(FormButton))
                                .addGap(265, 265, 265)));
    }
}