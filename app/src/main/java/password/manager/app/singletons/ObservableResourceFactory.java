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

package password.manager.app.singletons;

import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import password.manager.app.base.SupportedLocale;

public final class ObservableResourceFactory extends Singleton {

    private static final String LANG_BUNDLE_RESOURCE = "/bundles/Lang";

    private final ObjectProperty<ResourceBundle> resources;
    private final List<StringProperty> emptyFieldPrompts;
    private final Random rand;

    // Let only package classes instantiate this
    ObservableResourceFactory() {
        this.resources = new SimpleObjectProperty<>();
        this.emptyFieldPrompts = new ArrayList<>();
        this.rand = new Random();

        // Fill empty field prompts when resources change
        this.resources.addListener((_, _, newValue) -> {
            if (newValue == null) return;

            int count;
            try {
                String cntStr = newValue.getString("empty_field_prompts");
                count = Integer.parseInt(cntStr.trim());
            } catch (Exception e) {
                // Key missing or not a number: do nothing
                count = 0;
            }

            for (int i = 0; i < count; i++) {
                String key = "empty_field_" + (i + 1);
                try {
                    String prompt = newValue.getString(key);

                    if(i < emptyFieldPrompts.size()) {
                        emptyFieldPrompts.get(i).set(prompt);
                    } else {
                        StringProperty prop = new SimpleStringProperty(prompt);
                        emptyFieldPrompts.add(prop);
                    }
                } catch (Exception e) {
                    // Key missing: skip
                }
            }
        });

        // This will be the "placeholder" ResourceBundle until a locale is set
        ResourceBundle emptyResourceBundle = ResourceBundle.getBundle(LANG_BUNDLE_RESOURCE);
        this.resources.set(emptyResourceBundle);
    }

    public ObjectProperty<ResourceBundle> resourcesProperty() {
        return resources;
    }

    public ResourceBundle getResources() {
        return resources.get();
    }

    public void bindLocaleProperty(ObjectProperty<SupportedLocale> localeProperty) {
        this.resources.bind(Bindings.createObjectBinding(
            () -> ResourceBundle.getBundle(LANG_BUNDLE_RESOURCE, localeProperty.get().getLocale()),
            localeProperty
        ));
    }

    public StringBinding getStringBinding(String key) {
        return new StringBinding() {
            {
                bind(resourcesProperty());
            }

            @Override
            public String computeValue() {
                return getResources().getString(key);
            }
        };
    }

    public String getValue(@NotNull String key) {
        return getResources().getString(key);
    }

    public void bindStringProperty(@NotNull StringProperty property, @NotNull String key) {
        if (key.isBlank()) {
            property.unbind();
        } else {
            property.bind(getStringBinding(key));
        }
    }

    public void bindTextProperty(@NotNull Labeled field, @NotNull String key) {
        bindStringProperty(field.textProperty(), key);
    }

    public void bindTitleProperty(@NotNull Stage stage, @NotNull String key) {
        bindStringProperty(stage.titleProperty(), key);
    }

    public void bindPromptTextProperty(@NotNull TextInputControl... fields) {
        if (emptyFieldPrompts.isEmpty()) return;

        for (@NotNull TextInputControl field : fields) {
            int index = rand.nextInt(emptyFieldPrompts.size());
            field.promptTextProperty().unbind();
            field.promptTextProperty().bind(emptyFieldPrompts.get(index));
        }
    }

    // #region Singleton methods
    @Override
    public void close() {
        // Nothing to close
    }

    public static ObservableResourceFactory getInstance() {
        return Singletons.get(ObservableResourceFactory.class);
    }
    // #endregion
}