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

import org.jetbrains.annotations.NotNull;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Labeled;
import javafx.stage.Stage;

public final class ObservableResourceFactory implements AutoCloseable {
    private final ObjectProperty<ResourceBundle> resources;

    private ObservableResourceFactory(ResourceBundle resources) {
        this.resources = new SimpleObjectProperty<>(resources);
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
        resourcesProperty().set(resources);
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

    public void bindTextProperty(@NotNull Labeled field, @NotNull String key) {
        if (key.isBlank()) {
            field.textProperty().unbind();
        } else {
            field.textProperty().bind(getStringBinding(key));
        }
    }

    public void bindTitleProperty(@NotNull Stage stage, @NotNull String key) {
        if (key.isBlank()) {
            stage.titleProperty().unbind();
        } else {
            stage.titleProperty().bind(getStringBinding(key));
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