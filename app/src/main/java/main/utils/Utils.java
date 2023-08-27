package main.utils;

import java.awt.Component;

import javax.swing.JPanel;

public class Utils {
    /**
     * Replaces a panel with another one.
     *
     * @param actingPanel The panel on which will be done the replacement
     * @param showPanel   The new panel
     */
    public static void replacePanel(JPanel actingPanel, JPanel showPanel) {
        // removing old panel
        actingPanel.removeAll();
        actingPanel.repaint();
        actingPanel.revalidate();

        // adding new panel
        actingPanel.add(showPanel);
        actingPanel.repaint();
        actingPanel.revalidate();
    }

    /**
     * Returns a string containing the index, preceeded by zero to match up the same
     * number of digits of the size of the list.
     *
     * @param listSize The size of the list.
     * @param index    The index of the element.
     * @return The index.
     */
    public static String addZerosToIndex(int listSize, int ìndex) {
        int cifreLista = (int) Math.log10(listSize) + 1;
        return String.format("%0" + cifreLista + "d", ìndex);
    }

    public static void repaintAll(Component... components) {
        for (Component component: components) {
            component.repaint();
        }
    }
}
