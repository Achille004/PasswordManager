/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

import java.text.Collator;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class Utils {
    public static final Encoder BASE64ENC = Base64.getEncoder();
    public static final Decoder BASE64DEC = Base64.getDecoder();

    @SafeVarargs
    public static <T> SortedList<T> getFXSortedList(T... items) {
        return FXCollections.observableArrayList(items).sorted(null);
    }

    /**
     * Returns the selected index in a Combo Box with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @param comboBox The combo box to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> int selectedChoiceBoxIndex(@NotNull ChoiceBox<T> comboBox) {
        return comboBox.getSelectionModel().getSelectedIndex();
    }

    /**
     * Returns the selected item in a Combo Box.
     *
     * @param comboBox The combo box to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> T selectedChoiceBoxItem(@NotNull ChoiceBox<T> comboBox) {
        return comboBox.getSelectionModel().getSelectedItem();
    }

    public static void bindPasswordFields(PasswordField hidden, TextField visible) {
        hidden.textProperty().addListener((options, oldValue, newValue) -> visible.setText(newValue));
        visible.textProperty().addListener((options, oldValue, newValue) -> hidden.setText(newValue));
    }

    @SafeVarargs
    public static <T extends TextField> boolean checkTextFields(@NotNull T... fields) {
        boolean nonEmpty = true;

        for (T field : fields) {
            if (field.getText().isBlank()) {
                nonEmpty = false;
                field.setStyle("-fx-border-color: #ff5f5f;");
            } else {
                field.setStyle("-fx-border-color: #a7acb1;");
            }
        }

        return nonEmpty;
    }

    @SafeVarargs
    public static <T extends TextField> void clearTextFields(@NotNull T... fields) {
        for (T field : fields) {
            field.clear();
        }
    }

    @SafeVarargs
    public static <T extends Node> void clearStyle(T... nodes) {
        for (T node : nodes) {
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
    public static String addZerosToIndex(int listSize, int index) {
        int listDigits = (int) Math.log10(listSize) + 1;
        return String.format("%0" + listDigits + "d", index);
    }

    public static String byteToBase64(byte[] src) {
        // Base64-encode the encrypted password for a readable representation
        return BASE64ENC.encodeToString(src);
    }

    public static byte[] base64ToByte(String src) {
        // Base64-encode the encrypted password for a readable representation
        return BASE64DEC.decode(src);
    }

    public static String capitalizeWord(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static <T> StringConverter<T> toStringConverter(Callback<? super T, String> converter) {
        return new StringConverter<>() {

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString(T object) {
                return converter.call(object);
            }
        };
    }

    public static <T> ObservableValue<Comparator<T>> comparatorBinding(ObjectProperty<Locale> locale,
            ObjectProperty<? extends StringConverter<T>> converter) {
        return Bindings.createObjectBinding(
                () -> Comparator.comparing(
                        converter.getValue()::toString,
                        Collator.getInstance(locale.getValue())),
                locale,
                converter);
    }

    public static <T> void bindValueConverter(ChoiceBox<T> choiceBox, ObjectProperty<Locale> locale,
            Function<Locale, StringConverter<T>> mapper) {
        choiceBox.converterProperty().bind(locale.map(mapper));
    }

    public static <T> void bindValueComparator(SortedList<T> sortedList, ObjectProperty<Locale> locale,
            ChoiceBox<T> choiceBox) {
        sortedList.comparatorProperty().bind(comparatorBinding(locale, choiceBox.converterProperty()));
    }
}
