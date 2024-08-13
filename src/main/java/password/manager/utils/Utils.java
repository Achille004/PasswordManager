/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import me.gosimple.nbvcxz.Nbvcxz;

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
    private static final Nbvcxz NBVCXZ = new Nbvcxz();

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

    public static void bindPasswordFields(@NotNull PasswordField hidden, @NotNull TextField visible) {
        hidden.textProperty().addListener((options, oldValue, newValue) -> visible.setText(newValue));
        visible.textProperty().addListener((options, oldValue, newValue) -> hidden.setText(newValue));
    }

    @SafeVarargs
    public static <T extends TextField> boolean checkTextFields(T @NotNull... fields) {
        boolean nonEmpty = true;

        for (@NotNull T field : fields) {
            if (field.getText().isBlank()) {
                nonEmpty = false;
                field.setStyle("-fx-border-color: #ff5f5f");
            } else {
                field.setStyle("-fx-border-color: #a7acb1");
            }
        }

        return nonEmpty;
    }

    @SafeVarargs
    public static <T extends TextField> void clearTextFields(T @NotNull... fields) {
        for (@NotNull T field : fields) {
            field.clear();
        }
    }

    @SafeVarargs
    public static <T extends Node> void clearStyle(T @NotNull... nodes) {
        for (@NotNull T node : nodes) {
            node.setStyle("");
        }
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

    public static String byteToBase64(@NotNull byte[] src) {
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

    @Contract(value = "_ -> new", pure = true)
    public static <T> @NotNull StringConverter<T> toStringConverter(@NotNull Callback<? super T, String> converter) {
        return new StringConverter<>() {
            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString(@NotNull T object) {
                return converter.call(object);
            }
        };
    }

    public static <T> @NotNull ObservableValue<Comparator<T>> comparatorBinding(@NotNull ObjectProperty<Locale> locale,
            @NotNull ObjectProperty<? extends StringConverter<T>> converter) {
        return Bindings.createObjectBinding(
                () -> Comparator.comparing(
                        converter.getValue()::toString,
                        Collator.getInstance(locale.getValue())),
                locale,
                converter);
    }

    public static <T> void bindValueConverter(@NotNull ComboBox<T> comboBox,
            @NotNull ObjectProperty<Locale> locale,
            @NotNull Function<Locale, StringConverter<T>> mapper) {
        comboBox.converterProperty().bind(locale.map(mapper));
    }

    public static <T> void bindValueComparator(@NotNull SortedList<T> sortedList,
            @NotNull ObjectProperty<Locale> locale,
            @NotNull ComboBox<T> comboBox) {
        sortedList.comparatorProperty().bind(comparatorBinding(locale, comboBox.converterProperty()));
    }

    @Contract("_, _ -> param1")
    public static @NotNull Alert setDefaultButton(@NotNull Alert alert, ButtonType defBtn) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes())
            ((Button) pane.lookupButton(t)).setDefaultButton(t == defBtn);
        return alert;
    }

    // Ideal gap is from 20 to 50, represented with linear progress bar with gaps of
    // 1
    public static double passwordStrength(@NotNull String password) {
        return NBVCXZ.estimate(password).getEntropy();
    }

    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("Progress must be between 0 and 1");
        }
        String gradientStr = "linear-gradient(to right, #f00 0%";
        boolean isHalfProgress = progress >= 0.5;

        if (isHalfProgress) {
            double halfProgress = progress - 0.5;

            gradientStr += ", #ff0 " + (1 - halfProgress) * 100 + "%";
            gradientStr += ", " +
                    (Color.YELLOW.interpolate(Color.GREEN, halfProgress * 2) + "x")
                            .replace("0x", "#").replace("ffx", "")
                    + " 100%";
        } else {
            gradientStr += ", " + (Color.RED.interpolate(Color.YELLOW, progress * 2) + "x")
                    .replace("0x", "#").replace("ffx", "")
                    + " 100%";
        }
        gradientStr += ")";

        return gradientStr;
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
}
