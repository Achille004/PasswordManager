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

package password.manager.app;

import static password.manager.app.utils.Utils.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import org.jetbrains.annotations.Nullable;

import javafx.application.Application.Parameters;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import password.manager.app.controllers.FirstRunController;
import password.manager.app.controllers.LoginController;
import password.manager.app.controllers.MainController;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.Logger;
import password.manager.app.utils.ObservableResourceFactory;

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

        final BooleanProperty switchToMain = new SimpleBooleanProperty(false);
        switchToMain.addListener((_, _, newValue) -> {
            if (newValue) {
                loadMainPane();
            }
        });

        List<String> list = this.parameters.getRaw();
        Logger.getInstance().addInfo("Found " + list.size() + " parameters");
        if (!ioManager.isFirstRun() && list.size() > 1 && ("-p".equals(list.get(0)) || "--password".equals(list.get(0)))) {
            Logger.getInstance().addInfo("Trying to authenticate via arguments");
            if (ioManager.authenticate(list.get(1))) {
                Logger.getInstance().addInfo("Correct password, skipping login");
                switchToMain.set(true);
            } else {
                Logger.getInstance().addInfo("Incorrect password, redirecting to login");
            }
        }

        if (!switchToMain.get()) {
            AnchorPane pane;
            String paneName;
            if (ioManager.isFirstRun()) {
                Logger.getInstance().addInfo("Loading first run pane...");
                pane = (AnchorPane) loadFxml("/fxml/first_run.fxml", new FirstRunController(ioManager, langResources, hostServices, switchToMain));
                paneName = "first_run";
            } else {
                Logger.getInstance().addInfo("Loading login pane...");
                pane = (AnchorPane) loadFxml("/fxml/login.fxml", new LoginController(ioManager, langResources, hostServices, switchToMain));
                paneName = "login";
            }

            checkValidUi(pane, paneName, ioManager, langResources);
            
            scenePane.getChildren().clear();
            scenePane.getChildren().add(pane);
        }
    }

    private void loadMainPane() {
        Logger.getInstance().addInfo("Loading main pane...");
        final MainController mainController = new MainController(ioManager, langResources, hostServices);
        final BorderPane mainPane = (BorderPane) loadFxml("/fxml/main.fxml", mainController);
        checkValidUi(mainPane, "main", ioManager, langResources);
        
        scenePane.getChildren().clear();
        scenePane.getChildren().add(mainPane);
        mainController.mainTitleAnimation();
    }

    private @Nullable Parent loadFxml(String path, Initializable controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(path)));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            Logger.getInstance().addError(e);
            return null;
        }
    }
}