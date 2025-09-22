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

package password.manager.app;

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
import java.util.Objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;

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
     * Returns the selected index in a ListView with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @param listView The {@code ListView} to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> int selectedListViewIndex(@NotNull ListView<T> listView) {
        return listView.getSelectionModel().getSelectedIndex();
    }

    /**
     * Returns the selected item in a ListView.
     *
     * @param listView The ListView to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> T selectedListViewItem(@NotNull ListView<T> listView) {
        return listView.getSelectionModel().getSelectedItem();
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

    public static @Nullable FileWriter getFileWriter(@NotNull Path path, @NotNull Boolean append) {
        return getFileWriter(path.toFile(), append);
    }

    public static @Nullable FileWriter getFileWriter(@NotNull File file, @NotNull Boolean append) {
        try {
            return new FileWriter(file, append);
        } catch (IOException e) {
            return null;
        }
    }

    public static @NotNull Parent loadFxml(String path, Initializable controller, boolean... printLogsArg) {
        boolean printLogs = printLogsArg.length > 0 ? printLogsArg[0] : true;

        String loggedPath = "none";
        if(printLogs) {
            loggedPath = path.replace("/fxml/", "").replace(".fxml", "");
            Logger.getInstance().addInfo("Loading [" + loggedPath + "] pane...");
        }

        Parent parent = null;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Utils.class.getResource(path)));
            loader.setController(controller);
            parent = loader.load();
        } catch (IOException e) {
            Logger.getInstance().addError(e);
        }

        if(parent != null) {
            if (printLogs) {
                Logger.getInstance().addInfo("Success [" + loggedPath + "]");
            }
            return parent;
        }
        
        // Since it's a one-time error, just create it during the error process
        Alert alert = new Alert(AlertType.ERROR, ObservableResourceFactory.getInstance().getValue("ui_error"), ButtonType.YES, ButtonType.NO);
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
        return null;
    }
}
