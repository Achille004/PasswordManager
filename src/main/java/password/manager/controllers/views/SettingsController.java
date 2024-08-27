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

package password.manager.controllers.views;

import static password.manager.utils.Utils.capitalizeWord;
import static password.manager.utils.Utils.getFXSortedList;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import password.manager.enums.SortingOrder;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;
import password.manager.utils.Utils;

public class SettingsController extends AbstractViewController {
    public SettingsController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    public ComboBox<Locale> settingsLangCB;
    @FXML
    public ComboBox<SortingOrder> settingsOrderCB;

    @FXML
    public TextField settingsLoginPasswordVisible;
    @FXML
    public PasswordField settingsLoginPasswordHidden;

    @FXML
    public Label settingsLangLbl, selectedLangLbl, settingsSortingOrderLbl, selectedOrderLbl,
            settingsLoginPasswordLbl, settingsLoginPasswordDesc, settingsDriveConnLbl, wip;

    @FXML
    public ProgressBar settingsLoginPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        ObjectProperty<Locale> localeProperty = ioManager.getUserPreferences().getLocaleProperty();
        ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsLoginPasswordLbl, "login_pas");
        langResources.bindTextProperty(settingsLoginPasswordDesc, "login_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        // Language box

        SortedList<Locale> languages = getFXSortedList(Utils.SUPPORTED_LOCALE);

        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(ioManager.getUserPreferences().getLocale());
        bindValueConverter(settingsLangCB, localeProperty, this::languageStringConverter);
        bindValueComparator(languages, localeProperty, settingsLangCB);

        selectedLangLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Locale locale = localeProperty.getValue();
                    return languageStringConverter(locale).toString(locale);
                },
                localeProperty));

        localeProperty.bind(settingsLangCB.valueProperty());

        // Sorting order box

        SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());

        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(ioManager.getUserPreferences().getSortingOrder());
        bindValueConverter(settingsOrderCB, localeProperty, this::sortingOrderStringConverter);
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB);

        selectedOrderLbl.textProperty().bind(Bindings.createStringBinding(
                () -> sortingOrderStringConverter(localeProperty.getValue()).toString(sortingOrderProperty.getValue()),
                localeProperty, sortingOrderProperty));

        sortingOrderProperty.bind(settingsOrderCB.valueProperty());

        // Login password

        EventHandler<ActionEvent> changeLoginPasswordEvent = event -> {
            if (checkTextFields(settingsLoginPasswordVisible, settingsLoginPasswordHidden)) {
                ioManager.changeLoginPassword(settingsLoginPasswordVisible.getText());
            }
        };
        settingsLoginPasswordVisible.setOnAction(changeLoginPasswordEvent);
        settingsLoginPasswordHidden.setOnAction(changeLoginPasswordEvent);

        bindTextProperty(settingsLoginPasswordHidden, settingsLoginPasswordVisible);
        bindPasswordStrength(settingsLoginPassStr, settingsLoginPasswordVisible);
    }

    public void reset() {
        clearStyle(settingsLoginPasswordHidden, settingsLoginPasswordVisible);
        ioManager.displayLoginPassword(settingsLoginPasswordVisible, settingsLoginPasswordHidden);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull StringConverter<Locale> languageStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? capitalizeWord(item.getDisplayLanguage(item)) : null);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull StringConverter<SortingOrder> sortingOrderStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? langResources.getValue(item.getI18nKey()) : null);
    }
}
