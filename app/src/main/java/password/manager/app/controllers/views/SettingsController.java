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

package password.manager.app.controllers.views;

import static password.manager.app.Utils.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.Callback;
import javafx.util.StringConverter;
import password.manager.app.Utils;
import password.manager.app.enums.SortingOrder;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class SettingsController extends AbstractViewController {

    @FXML
    private ComboBox<Locale> settingsLangCB;
    @FXML
    private ComboBox<SortingOrder> settingsOrderCB;

    @FXML
    private ReadablePasswordFieldWithStr settingsMasterPassword;

    @FXML
    private Label settingsLangLbl, settingsSortingOrderLbl, settingsMasterPasswordLbl, settingsMasterPasswordDesc, settingsDriveConnLbl, wip;

    public void initialize(URL location, ResourceBundle resources) {
        final IOManager ioManager = IOManager.getInstance();
        final ObjectProperty<Locale> localeProperty = ioManager.getUserPreferences().getLocaleProperty();
        final ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

        final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsMasterPasswordLbl, "master_pas");
        langResources.bindTextProperty(settingsMasterPasswordDesc, "master_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        // Language box

        final SortedList<Locale> languages = getFXSortedList(Utils.SUPPORTED_LOCALE);

        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(ioManager.getUserPreferences().getLocale());
        bindValueConverter(settingsLangCB, localeProperty, this::languageStringConverter);
        bindValueComparator(languages, localeProperty, settingsLangCB);

        localeProperty.bind(settingsLangCB.valueProperty());

        // Sorting order box

        final SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());

        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(ioManager.getUserPreferences().getSortingOrder());
        bindValueConverter(settingsOrderCB, localeProperty, this::sortingOrderStringConverter);
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB);

        sortingOrderProperty.bind(settingsOrderCB.valueProperty());

        // Master password
        settingsMasterPassword.setOnAction(_ -> {
            if (checkTextFields(settingsMasterPassword.getTextField())) {
                ioManager.changeMasterPassword(settingsMasterPassword.getText());
            }
        });
        
        // Force the correct size to prevent unwanted stretching
        settingsMasterPassword.setPrefSize(465.0, 40.0);
    }

    public void reset() {
        clearStyle(settingsMasterPassword.getTextField());
        IOManager.getInstance().displayMasterPassword(settingsMasterPassword);
        settingsMasterPassword.setReadable(false);
    }

    ///// Utility methods /////

    @Contract(value = "_ -> new", pure = true)
    private @NotNull StringConverter<Locale> languageStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? capitalizeWord(item.getDisplayLanguage(item)) : null);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull StringConverter<SortingOrder> sortingOrderStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? ObservableResourceFactory.getInstance().getValue(item.getI18nKey()) : null);
    }

    @Contract(value = "_ -> new", pure = true)
    private static <T> @NotNull StringConverter<T> toStringConverter(@NotNull Callback<? super T, String> converter) {
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

    private static <T> @NotNull ObservableValue<Comparator<T>> comparatorBinding(@NotNull ObjectProperty<Locale> locale, @NotNull ObjectProperty<? extends StringConverter<T>> converter) {
        return Bindings.createObjectBinding(
                () -> Comparator.comparing(
                        converter.getValue()::toString,
                        Collator.getInstance(locale.getValue())),
                locale,
                converter);
    }

    private static <T> void bindValueConverter(@NotNull ComboBox<T> comboBox, @NotNull ObjectProperty<Locale> locale, @NotNull Function<Locale, StringConverter<T>> mapper) {
        comboBox.converterProperty().bind(locale.map(mapper));
    }

    private static <T> void bindValueComparator(@NotNull SortedList<T> sortedList, @NotNull ObjectProperty<Locale> locale, @NotNull ComboBox<T> comboBox) {
        sortedList.comparatorProperty().bind(comparatorBinding(locale, comboBox.converterProperty()));
    }
}
