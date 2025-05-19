/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.utils;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Locale;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;

public final class Utils {
    public static final Locale[] SUPPORTED_LOCALE;
    public static final Locale DEFAULT_LOCALE;

    static {
        SUPPORTED_LOCALE = new Locale[] { Locale.ENGLISH, Locale.ITALIAN };

        Locale systemLang = Locale.forLanguageTag(Locale.getDefault().getLanguage());
        DEFAULT_LOCALE = Arrays.asList(SUPPORTED_LOCALE).contains(systemLang)
                ? systemLang
                : Locale.ENGLISH;
    }

    private static final Encoder BASE64ENC = Base64.getEncoder();
    private static final Decoder BASE64DEC = Base64.getDecoder();

    @SafeVarargs
    public static <T> SortedList<T> getFXSortedList(T @NotNull... items) {
        return FXCollections.observableArrayList(items).sorted(null);
    }

    /**
     * Returns the selected index in a Combo Box with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @param comboBox The combo box to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> int selectedComboBoxIndex(@NotNull ComboBox<T> comboBox) {
        return comboBox.getSelectionModel().getSelectedIndex();
    }

    /**
     * Returns the selected item in a Combo Box.
     *
     * @param comboBox The combo box to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> T selectedComboBoxItem(@NotNull ComboBox<T> comboBox) {
        return comboBox.getSelectionModel().getSelectedItem();
    }

    /**
     * Returns a string containing the index, preceded by zero to match up the same
     * number of digits of the size of the list.
     *
     * @param listSize The size of the list.
     * @param index    The index of the element.
     * @return The index.
     */
    public static @NotNull String addZerosToIndex(@NotNull Integer listSize, @NotNull Integer index) {
        int listDigits = (int) Math.log10(listSize) + 1;
        return String.format("%0" + listDigits + "d", index);
    }

    public static String byteToBase64(byte[] src) {
        // Base64-encode the encrypted password for a readable representation
        return BASE64ENC.encodeToString(src);
    }

    public static byte[] base64ToByte(@NotNull String src) {
        // Base64-encode the encrypted password for a readable representation
        return BASE64DEC.decode(src);
    }

    public static @NotNull String capitalizeWord(@NotNull String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Contract("_, _ -> param1")
    public static @NotNull Alert setDefaultButton(@NotNull Alert alert, ButtonType defBtn) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes())
            ((Button) pane.lookupButton(t)).setDefaultButton(t == defBtn);
        return alert;
    }

    public static FileWriter getFileWriter(@NotNull Path path, @NotNull Boolean append) {
        return getFileWriter(path.toFile(), append);
    }

    public static FileWriter getFileWriter(@NotNull File file, @NotNull Boolean append) {
        try {
            return new FileWriter(file, append);
        } catch (IOException e) {
            return null;
        }
    }

    public static void triggerUiErrorIfNull(Object pane, @NotNull IOManager ioManager, @NotNull ObservableResourceFactory langResources) {
        if(pane == null) {
            // Since it's a one-time error, just create it during the error process
            Alert alert = new Alert(AlertType.ERROR, langResources.getValue("ui_error"), ButtonType.YES, ButtonType.NO);
            setDefaultButton(alert, ButtonType.NO);

            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                Thread.startVirtualThread(() -> {
                    try {
                        Desktop.getDesktop().open(Logger.getInstance().getLoggingPath().toFile());
                    } catch (IOException e) {
                        Logger.getInstance().addError(e);
                    }
                });
            }
            Platform.exit();
        }
    }
}
