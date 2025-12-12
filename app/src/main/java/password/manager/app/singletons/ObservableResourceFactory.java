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

package password.manager.app.singletons;

import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import password.manager.lib.PasswordInputControl;

public final class ObservableResourceFactory implements AutoCloseable {
    private final ObjectProperty<ResourceBundle> resources;
    private final List<StringProperty> emptyFieldPrompts;
    private final Random rand;

    private ObservableResourceFactory(ResourceBundle resources) {
        this.resources = new SimpleObjectProperty<>();
        this.emptyFieldPrompts = new ArrayList<>();
        this.rand = new Random();

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

        this.setResources(resources);
    }

    private ObservableResourceFactory(String bundleName) {
        this(ResourceBundle.getBundle(bundleName));
    }

    public ObjectProperty<ResourceBundle> resourcesProperty() {
        return resources;
    }

    public ResourceBundle getResources() {
        return resources.get();
    }

    public void setResources(ResourceBundle resources) {
        if (resources == null) return;
        this.resources.set(resources);
    }

    public void setResources(String bundleName) {
        this.setResources(ResourceBundle.getBundle(bundleName));
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

    public void bindPromptTextProperty(@NotNull Object... fields) {
        if (emptyFieldPrompts.isEmpty()) return;

        for (@NotNull Object field : fields) {
            int index = rand.nextInt(emptyFieldPrompts.size());

            if (field instanceof TextInputControl tic) {
                tic.promptTextProperty().unbind();
                tic.promptTextProperty().bind(emptyFieldPrompts.get(index));
            } else if (field instanceof PasswordInputControl pic) {
                pic.promptTextProperty().unbind();
                pic.promptTextProperty().bind(emptyFieldPrompts.get(index));
            }
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }

    // #region Singleton methods
    public static synchronized void createInstance(String bundleName) throws IllegalStateException {
        Singletons.register(ObservableResourceFactory.class, new ObservableResourceFactory(bundleName));
    }

    public static ObservableResourceFactory getInstance() throws IllegalStateException {
        return Singletons.get(ObservableResourceFactory.class);
    }

    public static boolean hasInstance() {
        return Singletons.isRegistered(ObservableResourceFactory.class);
    }

    public static synchronized void destroyInstance() throws IllegalStateException {
        Singletons.unregister(ObservableResourceFactory.class);
    }
    // #endregion
}