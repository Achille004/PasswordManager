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

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class Utils {
    public static final Encoder BASE64ENC = Base64.getEncoder();
    public static final Decoder BASE64DEC = Base64.getDecoder();

    /**
     * Returns the selected index in a Combo Box with a first blank option. If
     * the value is -1, the selected index is the blank one.
     *
     * @param comboBox The combo box to extract the index from.
     * @return The index of the current selected item.
     */
    public static <T> int selectedItemInChoiceBox(@NotNull ChoiceBox<T> comboBox) {
        return comboBox.getSelectionModel().getSelectedIndex();
    }

    @SafeVarargs
    public static <T> void setChoiceBoxItems(@NotNull ChoiceBox<T> comboBox, @NotNull T... items) {
        comboBox.getSelectionModel().clearSelection();
        comboBox.setItems(FXCollections.observableArrayList(items));
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

    public static <T extends Enum<T>> String[] getEnumStringValues(Class<T> enumClass) {
        return getEnumStringValuesWithStringExtractor(enumClass, Enum::name);
    }

    public static <T extends Enum<T>> String[] getEnumStringValuesWithStringExtractor(
            Class<T> enumClass,
            Function<? super T, String> extractor) {
        return Stream.of(enumClass.getEnumConstants()).map(extractor).toArray(String[]::new);
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
}
