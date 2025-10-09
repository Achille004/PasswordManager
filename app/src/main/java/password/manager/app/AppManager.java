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

import static password.manager.app.Utils.*;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import password.manager.app.controllers.FirstRunController;
import password.manager.app.controllers.LoginController;
import password.manager.app.controllers.MainController;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;

public class AppManager {
    private static final IOManager IO_MANAGER;

    static {
        IOManager.createInstance();
        IO_MANAGER = IOManager.getInstance();
    }

    private AppManager() {
        final ObjectProperty<Locale> locale = IOManager.getInstance().getUserPreferences().getLocaleProperty();
        ObservableResourceFactory.getInstance().resourcesProperty().bind(Bindings.createObjectBinding(
                () -> ResourceBundle.getBundle("/bundles/Lang", locale.getValue()), locale));

        final BooleanProperty switchToMain = new SimpleBooleanProperty(false);
        switchToMain.addListener((_, _, newValue) -> {
            if (newValue) {
                loadMainPane();
            }
        });

        final List<String> list = App.getAppParameters().getRaw();
        Logger.getInstance().addDebug("Found " + list.size() + " parameters");
        if (!IO_MANAGER.isFirstRun() && list.size() > 1 && ("-p".equals(list.get(0)) || "--password".equals(list.get(0)))) {
            Logger.getInstance().addInfo("Trying to authenticate via arguments");
            if (IO_MANAGER.authenticate(list.get(1))) {
                Logger.getInstance().addInfo("Correct password, skipping login");
                switchToMain.set(true);
            } else {
                Logger.getInstance().addInfo("Incorrect password, redirecting to login");
            }
        }

        if (!switchToMain.get()) {
            final AnchorPane pane = IO_MANAGER.isFirstRun()
                ? (AnchorPane) loadFxml("/fxml/first_run.fxml", new FirstRunController(switchToMain))
                : (AnchorPane) loadFxml("/fxml/login.fxml", new LoginController(switchToMain));

            final AnchorPane scenePane = App.getAppScenePane();
            scenePane.getChildren().clear();
            scenePane.getChildren().add(pane);
        }
    }

    private void loadMainPane() {
        final MainController mainController = new MainController();
        final BorderPane mainPane = (BorderPane) loadFxml("/fxml/main.fxml", mainController);

        final AnchorPane scenePane = App.getAppScenePane();
        scenePane.getChildren().clear();
        scenePane.getChildren().add(mainPane);
        mainController.mainTitleAnimation();
    }

    public static synchronized AppManager startApp() {
        return new AppManager();
    }
}