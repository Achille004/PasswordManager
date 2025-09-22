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

public final class ObservableResourceFactory {
    private final ObjectProperty<ResourceBundle> resources;

    private static ObservableResourceFactory instance = null;
    private static final String CLASS_NAME = ObservableResourceFactory.class.getName();

    /**
     * Creates the singleton ObservableResourceFactory.
     */
    public static synchronized void createInstance(String bundleName) throws IllegalStateException {
        if (instance != null) {
            throw new IllegalStateException(CLASS_NAME + " instance already created");
        }
        instance = new ObservableResourceFactory(bundleName);
    }

    public static ObservableResourceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException(CLASS_NAME + " instance not created yet");
        }
        return instance;
    }

    private ObservableResourceFactory(ResourceBundle resources) {
        // Initialize with default resources, can be set later
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
}