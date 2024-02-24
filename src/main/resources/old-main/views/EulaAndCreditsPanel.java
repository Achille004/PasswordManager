package main.views;

import java.awt.*;
import javax.swing.*;

public class EulaAndCreditsPanel extends JPanel {
    public EulaAndCreditsPanel() {
        super();

        initComponents();
    }

    private void initComponents() {
        JLabel CreditsLabel = new JLabel(), EulaLabel = new JLabel();
        JTextArea CreditsTextArea = new JTextArea(), EulaTextArea = new JTextArea();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));
        setToolTipText("");

        CreditsLabel.setBackground(new Color(38, 38, 38));
        CreditsLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        CreditsLabel.setForeground(new Color(242, 242, 242));
        CreditsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        CreditsLabel.setLabelFor(CreditsTextArea);
        CreditsLabel.setText("Credits:");

        CreditsTextArea.setEditable(false);
        CreditsTextArea.setBackground(new Color(242, 65, 65));
        CreditsTextArea.setColumns(20);
        CreditsTextArea.setFont(new Font("Dialog", Font.PLAIN, 24)); // NOI18N
        CreditsTextArea.setForeground(new Color(38, 38, 38));
        CreditsTextArea.setRows(5);
        CreditsTextArea.setText("Creator and developer: Francesco Marras.\nSpecial thanks to \"Luca\".\n\n");
        CreditsTextArea.setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        EulaLabel.setBackground(new Color(38, 38, 38));
        EulaLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        EulaLabel.setForeground(new Color(242, 242, 242));
        EulaLabel.setHorizontalAlignment(SwingConstants.LEFT);
        EulaLabel.setLabelFor(EulaTextArea);
        EulaLabel.setText("EULA:");

        EulaTextArea.setEditable(false);
        EulaTextArea.setBackground(new Color(242, 65, 65));
        EulaTextArea.setColumns(20);
        EulaTextArea.setFont(new Font("Dialog", Font.PLAIN, 24)); // NOI18N
        EulaTextArea.setForeground(new Color(38, 38, 38));
        EulaTextArea.setRows(5);
        EulaTextArea.setText(
                "Password Manager: manages accounts given by user with encrypted password.\nCopyright (C) 2022-2024  Francesco Marras\n\nThis program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html.");
        EulaTextArea.setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));

        GroupLayout EulaAndCreditsPanelLayout = new GroupLayout(this);
        setLayout(EulaAndCreditsPanelLayout);
        EulaAndCreditsPanelLayout.setHorizontalGroup(
                EulaAndCreditsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(EulaAndCreditsPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(CreditsTextArea, GroupLayout.DEFAULT_SIZE, 1045,
                                                Short.MAX_VALUE)
                                        .addComponent(EulaLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(CreditsLabel, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(EulaTextArea, GroupLayout.Alignment.TRAILING,
                                                GroupLayout.DEFAULT_SIZE, 1045, Short.MAX_VALUE))
                                .addGap(50, 50, 50)));
        EulaAndCreditsPanelLayout.setVerticalGroup(
                EulaAndCreditsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(EulaAndCreditsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(CreditsLabel, GroupLayout.PREFERRED_SIZE, 35,
                                        GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CreditsTextArea, GroupLayout.PREFERRED_SIZE, 66,
                                        GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(EulaLabel, GroupLayout.PREFERRED_SIZE, 35,
                                        GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(EulaTextArea, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(50, 50, 50)));

    }
}
