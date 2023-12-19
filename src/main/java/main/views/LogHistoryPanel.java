package main.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

import main.Main;

public class LogHistoryPanel extends JPanel {

    private JTextArea HistoryTextArea;
    private JLabel HistoryLabel;
    private JLabel LegendLabel;

    private final Main appInstance;

    public LogHistoryPanel(Main appInstance) {
        super();
        this.appInstance = appInstance;

        initComponents();
    }

    public void load(String HistoryLabelText, String legendLabelText) {
        HistoryLabel.setText(HistoryLabelText);
        LegendLabel.setText(legendLabelText);

        // writes the log history to its text area
        HistoryTextArea.setText(appInstance.getLogger().getLogHistory());
    }

    private void initComponents() {
        JScrollPane HistoryScrollPane;

        HistoryLabel = new JLabel();
        HistoryScrollPane = new JScrollPane();
        HistoryTextArea = new JTextArea();
        LegendLabel = new JLabel();

        setBackground(new Color(38, 38, 38));
        setForeground(new Color(242, 242, 242));
        setToolTipText("");

        HistoryLabel.setBackground(new Color(38, 38, 38));
        HistoryLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        HistoryLabel.setForeground(new Color(242, 242, 242));
        HistoryLabel.setHorizontalAlignment(SwingConstants.LEFT);
        HistoryLabel.setText("Log History:");
        HistoryLabel.setInheritsPopupMenu(false);
        HistoryLabel.setName(""); // NOI18N

        HistoryScrollPane.setBorder(BorderFactory.createLineBorder(new Color(115, 41, 41)));
        HistoryScrollPane.setViewportView(HistoryTextArea);

        HistoryTextArea.setEditable(false);
        HistoryTextArea.setBackground(new Color(242, 65, 65));
        HistoryTextArea.setColumns(50);
        HistoryTextArea.setFont(new Font("Dialog", Font.PLAIN, 18)); // NOI18N
        HistoryTextArea.setForeground(new Color(38, 38, 38));
        HistoryTextArea.setRows(5);
        HistoryTextArea.setBorder(new LineBorder(new Color(115, 41, 41), 1, true));
        HistoryTextArea.setCaretColor(new Color(102, 102, 102));

        LegendLabel.setBackground(new Color(38, 38, 38));
        LegendLabel.setFont(new Font("Dialog", Font.BOLD, 36)); // NOI18N
        LegendLabel.setForeground(new Color(242, 242, 242));
        LegendLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        LegendLabel.setText(">>> Actions / !!! Errors");
        LegendLabel.setInheritsPopupMenu(false);
        LegendLabel.setName(""); // NOI18N

        GroupLayout LogHistoryPanelLayout = new GroupLayout(this);
        setLayout(LogHistoryPanelLayout);
        LogHistoryPanelLayout.setHorizontalGroup(
                LogHistoryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, LogHistoryPanelLayout
                                .createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LogHistoryPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                                                .addComponent(HistoryLabel, GroupLayout.PREFERRED_SIZE,
                                                        574, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(LegendLabel, GroupLayout.PREFERRED_SIZE,
                                                        465, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(HistoryScrollPane))
                                .addGap(50, 50, 50)));
        LogHistoryPanelLayout.setVerticalGroup(
                LogHistoryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(LogHistoryPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(LogHistoryPanelLayout
                                        .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(HistoryLabel)
                                        .addComponent(LegendLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(HistoryScrollPane, GroupLayout.DEFAULT_SIZE, 726,
                                        Short.MAX_VALUE)
                                .addGap(50, 50, 50)));
    }
}
