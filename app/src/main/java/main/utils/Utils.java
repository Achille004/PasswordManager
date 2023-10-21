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

package main.utils;

import java.awt.Component;
import java.util.Base64;

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
        for (Component component : components) {
            component.repaint();
        }
    }

    public static String byteToBase64(byte[] src) {
        // Base64-encode the encrypted password for a readable representation
        return Base64.getEncoder().encodeToString(src);
    }

    public static byte[] base64ToByte(String src) {
        // Base64-encode the encrypted password for a readable representation
        return Base64.getDecoder().decode(src);
    }
}