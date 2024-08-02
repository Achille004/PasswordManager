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
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
    public static final Locale[] SUPPORTED_LOCALES;
    public static final Locale DEFAULT_LOCALE;

    static {
        SUPPORTED_LOCALES = new Locale[] { Locale.ENGLISH, Locale.ITALIAN };

        Locale systemLang = Locale.forLanguageTag(Locale.getDefault().getLanguage());
        DEFAULT_LOCALE = Arrays.asList(SUPPORTED_LOCALES).contains(systemLang)
                ? systemLang
                : Locale.ENGLISH;
    }

    private final @Getter IOManager ioManager;
    private final @Getter ObservableResourceFactory langResources;

    private final AnchorPane scenePane;

    public AppManager(AnchorPane scenePane) {
        this.ioManager = new IOManager();
        this.langResources = new ObservableResourceFactory();

        this.scenePane = scenePane;
        initialize();
    }

    private void initialize() {
        langResources.resourcesProperty().set(ResourceBundle.getBundle("/bundles/Lang", ioManager.getLoginAccount() != null
                        ? ioManager.getLoginAccount().getLocale()
                        : AppManager.DEFAULT_LOCALE));
        Alert alert = new Alert(AlertType.ERROR, langResources.getValue("ui_error"), ButtonType.OK);

        AnchorPane pane;
        boolean firstRun = ioManager.loadDataFile(langResources);
        final BooleanProperty switchToMain = new SimpleBooleanProperty(false);
        if (firstRun) {
            pane = (AnchorPane) loadFxml("/fxml/first_run.fxml",
                    new FirstRunController(ioManager, langResources, switchToMain));
        } else {
            pane = (AnchorPane) loadFxml("/fxml/login.fxml",
                    new LoginController(ioManager, langResources, switchToMain));
        }
        if (pane == null) {
            alert.showAndWait();
            Platform.exit();
            return;
        }
        scenePane.getChildren().add(pane);
        
        final MainController mainController = new MainController(ioManager, langResources);
        final BorderPane mainPane = (BorderPane) loadFxml("/fxml/main.fxml", mainController);
        if (mainPane == null) {
            alert.showAndWait();
            Platform.exit();
            return;
        }

        switchToMain.addListener(value -> {
            scenePane.getChildren().remove(0);
            scenePane.getChildren().add(mainPane);
            mainController.mainTitleAnimation();
        });
    }

    private <T, S extends Initializable> Parent loadFxml(String path, S controller) {
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