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

package password.manager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import org.jetbrains.annotations.Nullable;

import javafx.application.Application.Parameters;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import password.manager.controllers.FirstRunController;
import password.manager.controllers.LoginController;
import password.manager.controllers.MainController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class AppManager {
    private final @Getter IOManager ioManager;
    private final @Getter ObservableResourceFactory langResources;
    private final @Getter HostServices hostServices;

    private final Parameters parameters;
    private final AnchorPane scenePane;

    public AppManager(AnchorPane scenePane, HostServices hostServices, Parameters parameters) {
        this.ioManager = new IOManager();
        this.langResources = new ObservableResourceFactory();

        this.scenePane = scenePane;
        this.hostServices = hostServices;

        this.parameters = parameters;
        initialize();
    }

    private void initialize() {
        langResources.setResources(ResourceBundle.getBundle("/bundles/Lang"));
        ioManager.loadData(langResources);

        ObjectProperty<Locale> locale = ioManager.getUserPreferences().getLocaleProperty();
        langResources.resourcesProperty().bind(Bindings.createObjectBinding(
                () -> {
                    Locale localeValue = locale.getValue();
                    return ResourceBundle.getBundle("/bundles/Lang", localeValue);
                },
                locale));

        Alert alert = new Alert(AlertType.ERROR, langResources.getValue("ui_error"), ButtonType.OK);

        ioManager.getLogger().addInfo("Loading main pane...");
        final MainController mainController = new MainController(ioManager, langResources, hostServices);
        final BorderPane mainPane = (BorderPane) loadFxml("/fxml/main.fxml", mainController);
        if (mainPane == null) {
            alert.showAndWait();
            Platform.exit();
            return;
        }
        ioManager.getLogger().addInfo("Success");

        final BooleanProperty switchToMain = new SimpleBooleanProperty(false);
        switchToMain.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                scenePane.getChildren().clear();
                scenePane.getChildren().add(mainPane);
                mainController.mainTitleAnimation();
            }
        });

        List<String> list = this.parameters.getRaw();
        ioManager.getLogger().addInfo("Found " + list.size() + " parameters");
        if (list.size() > 1 && !ioManager.isFirstRun()
                && ("-p".equals(list.get(0)) || "--password".equals(list.get(0)))) {
            ioManager.getLogger().addInfo("Trying to authenticate via arguments");
            if (ioManager.authenticate(list.get(1))) {
                ioManager.getLogger().addInfo("Success, skipping login");
                switchToMain.set(true);
            } else {
                ioManager.getLogger().addInfo("Incorrect password, redirecting to login");
            }
        }

        if (!switchToMain.get()) {
            AnchorPane pane;
            if (ioManager.isFirstRun()) {
                ioManager.getLogger().addInfo("Loading first run pane...");
                pane = (AnchorPane) loadFxml("/fxml/first_run.fxml",
                        new FirstRunController(ioManager, langResources, hostServices, switchToMain));
            } else {
                ioManager.getLogger().addInfo("Loading login pane...");
                pane = (AnchorPane) loadFxml("/fxml/login.fxml",
                        new LoginController(ioManager, langResources, hostServices, switchToMain));
            }

            if (pane == null) {
                alert.showAndWait();
                Platform.exit();
                return;
            } else {
                ioManager.getLogger().addInfo("Success");
            }

            scenePane.getChildren().clear();
            scenePane.getChildren().add(pane);
        }
    }

    private <S extends Initializable> @Nullable Parent loadFxml(String path, S controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(path)));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            ioManager.getLogger().addError(e);
            return null;
        }
    }
}