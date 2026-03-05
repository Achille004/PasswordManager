/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.controllers.main;

import static password.manager.app.Utils.*;

import java.net.URL;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import password.manager.app.base.SortingOrder;
import password.manager.app.base.SupportedLocale;
import password.manager.app.controllers.AbstractController;
import password.manager.app.security.UserPreferences;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class SettingsController extends AbstractController {

    @FXML
    private ComboBox<SupportedLocale> settingsLangCB;
    @FXML
    private ComboBox<SortingOrder> settingsOrderCB;

    @FXML
    private ReadablePasswordFieldWithStr settingsMasterPassword;

    @FXML
    private Label settingsLangLbl, settingsSortingOrderLbl, settingsMasterPasswordLbl, settingsMasterPasswordDesc, settingsDriveConnLbl, wip;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

        final IOManager ioManager = IOManager.getInstance();
        final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();

        langResources.bindTextProperty(settingsLangLbl, "settings.language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "settings.sorting_ord");
        langResources.bindTextProperty(settingsMasterPasswordLbl, "settings.master_pas");
        langResources.bindTextProperty(settingsMasterPasswordDesc, "settings.master_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "settings.drive_con");
        langResources.bindTextProperty(wip, "settings.wip");

        // Combo boxes
        final UserPreferences userPreferences = ioManager.getUserPreferences();
        setupLanguageCB(userPreferences);
        setupSortingOrderCB(userPreferences);

        // Master password
        settingsMasterPassword.setOnAction(_ -> {
            if (checkTextFields(settingsMasterPassword.getTextField())) {
                ioManager.changeMasterPassword(settingsMasterPassword.getText().strip());
            }
        });
    }

    @Override
    public String getFxmlPath() {
        return "/fxml/main/settings.fxml";
    }

    @Override
    public void reset() {
        clearStyle(settingsMasterPassword.getTextField());
        ObservableResourceFactory.getInstance().bindPromptTextProperty(settingsMasterPassword);

        IOManager.getInstance().displayMasterPassword(settingsMasterPassword);
        settingsMasterPassword.setReadable(false);
    }

    private void setupLanguageCB(UserPreferences userPreferences) {
        final ObjectProperty<SupportedLocale> localeProperty = userPreferences.localeProperty();

        final SortedList<SupportedLocale> languages = getFXSortedList(SupportedLocale.values());
        final StringConverter<SupportedLocale> localeStringConverter = toStringConverter(item ->
            item != null ? capitalizeWord(item.getLocale().getDisplayName()) : null
        );

        settingsLangCB.setCellFactory(_ -> new FlagListCell());
        settingsLangCB.setButtonCell(new FlagListCell());

        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(localeProperty.get());
        bindValueConverter(settingsLangCB, localeProperty, _ -> localeStringConverter);
        bindValueComparator(languages, localeProperty, settingsLangCB);

        localeProperty.bind(notNullBinding(settingsLangCB.valueProperty()));
    }

    private void setupSortingOrderCB(UserPreferences userPreferences) {
        final ObjectProperty<SupportedLocale> localeProperty = userPreferences.localeProperty();
        final ObjectProperty<SortingOrder> sortingOrderProperty = userPreferences.sortingOrderProperty();

        final SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());
        final StringConverter<SortingOrder> sortingOrderStringConverter = toStringConverter(item ->
            item != null ? ObservableResourceFactory.getInstance().getValue(item.getI18nKey()) : null
        );

        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(sortingOrderProperty.get());
        bindValueConverter(settingsOrderCB, localeProperty, _ -> sortingOrderStringConverter);
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB);

        sortingOrderProperty.bind(notNullBinding(settingsOrderCB.valueProperty()));
    }

    ///// Utility methods /////

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

    @Contract(value = "_ -> new", pure = true)
    private static <T> @NotNull ObjectBinding<T> notNullBinding(@NotNull ObjectProperty<T> property) {
        return new ObjectBinding<>() {
            private T cachedValue = property.getValue();

            {
                bind(property);
            }

            @Override
            protected @NotNull T computeValue() {
                T val = property.getValue();
                if (val != null) {
                    cachedValue = val;
                }
                return cachedValue;
            }
        };
    }

    private static <T> @NotNull ObservableValue<Comparator<T>> comparatorBinding(@NotNull ObjectProperty<SupportedLocale> locale, @NotNull ObjectProperty<? extends StringConverter<T>> converter) {
        return Bindings.createObjectBinding(
                () -> Comparator.comparing(converter.getValue()::toString, Collator.getInstance(locale.getValue().getLocale())),
                locale, converter);
    }

    private static <T> void bindValueConverter(@NotNull ComboBox<T> comboBox, @NotNull ObjectProperty<SupportedLocale> locale, @NotNull Function<SupportedLocale, StringConverter<T>> mapper) {
        comboBox.converterProperty().bind(locale.map(mapper));
    }

    private static <T> void bindValueComparator(@NotNull SortedList<T> sortedList, @NotNull ObjectProperty<SupportedLocale> locale, @NotNull ComboBox<T> comboBox) {
        sortedList.comparatorProperty().bind(comparatorBinding(locale, comboBox.converterProperty()));
    }

    private static class FlagListCell extends ListCell<SupportedLocale> {
        private static final double FLAG_SIZE = 20;
        private final ImageView imageView = new ImageView();

        FlagListCell() {
            imageView.setFitHeight(FLAG_SIZE);
            imageView.setFitWidth(FLAG_SIZE);
            imageView.setPreserveRatio(true);
        }

        @Override
        protected void updateItem(SupportedLocale item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Locale locale = item.getLocale();
            String displayName = capitalizeWord(locale.getDisplayName(locale));
            setText(displayName);

            Image flag = item.getFlagImage();
            if (flag != null) {
                imageView.setImage(flag);
                setGraphic(imageView);
            } else {
                setGraphic(null);
            }
        }
    }
}
