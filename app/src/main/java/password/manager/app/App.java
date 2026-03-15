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

package password.manager.app;

import static password.manager.app.Utils.*;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import password.manager.app.base.SupportedLocale;
import password.manager.app.controllers.FirstRunController;
import password.manager.app.controllers.LoginController;
import password.manager.app.controllers.MainController;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.app.singletons.Singletons;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public class App extends Application {

    public static final String APP_NAME = System.getProperty("app.name", "Password Manager");
    public static final String APP_VERSION = System.getProperty("app.version", "3.1.1");

    public static final String ROOT_STYLESHEET = App.class.getResource("/fxml/css/root.css").toExternalForm();
    public static final String AUTOCOMPLETION_STYLESHEET = App.class.getResource("/fxml/css/auto-completion.css").toExternalForm();
    public static final String CUSTOMPOPUP_STYLESHEET = App.class.getResource("/fxml/css/custom-popup.css").toExternalForm();
    // Keep as String to prevent crashing when JavaFX is not available (e.g., during build processes)
    public static final String MAIN_ICON = App.class.getResource("/icon.png").toExternalForm();

    private static final int MIN_WIDTH = 900, MIN_HEIGHT = 600;

    private static @Getter HostServices appHostServices;
    private static @Getter Pane appScenePane;
    private static @Getter Parameters appParameters;

    @Override
    public void start(@NotNull Stage primaryStage) {
        appHostServices = getHostServices();
        appScenePane = new AnchorPane();
        appParameters = getParameters();

        appScenePane.setMinSize(MIN_WIDTH, MIN_HEIGHT);
        appScenePane.getStylesheets().addAll(ROOT_STYLESHEET, AUTOCOMPLETION_STYLESHEET);

        primaryStage.setTitle(APP_NAME);
        primaryStage.getIcons().add(new Image(MAIN_ICON));
        primaryStage.setOnCloseRequest(_ -> Platform.exit());
        primaryStage.setScene(new Scene(appScenePane, MIN_WIDTH, MIN_HEIGHT));

        startApp();
        primaryStage.show();

        // Set actual 900x600 as stage sizes contain also window decorations
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
        primaryStage.setResizable(true);
    }

    private void startApp() {
        // Start up background services
        final IOManager IO_MANAGER = IOManager.getInstance();

        final ObjectProperty<SupportedLocale> locale = IO_MANAGER.getUserPreferences().localeProperty();
        ObservableResourceFactory.getInstance().bindLocaleProperty(locale);

        final BooleanProperty switchToMain = getBooleanProperty();

        final List<String> list = App.getAppParameters().getRaw();
        Logger.getInstance().addDebug("Found " + list.size() + " parameters");
        if (!IO_MANAGER.isFirstRun() && list.size() > 1 && ("-p".equals(list.get(0)) || "--password".equals(list.get(0)))) {
            Logger.getInstance().addInfo("Trying to authenticate via arguments");
            if (IO_MANAGER.authenticate(list.get(1))) {
                Logger.getInstance().addInfo("Correct password, skipping login");
                switchToMain.set(true);
                return; // Exit early
            } else {
                Logger.getInstance().addInfo("Incorrect password, redirecting to login");
            }
        }

        final Pane pane = (Pane) loadFxml(IO_MANAGER.isFirstRun()
            ?  new FirstRunController(switchToMain)
            :  new LoginController(switchToMain)
        );

        setFullyResizable(pane);

        appScenePane.getChildren().clear();
        appScenePane.getChildren().add(pane);
    }

    private static @NotNull BooleanProperty getBooleanProperty() {
        final BooleanProperty switchToMain = new SimpleBooleanProperty(false);
        switchToMain.addListener((_, _, newValue) -> {
            if (newValue) {
                final MainController mainController = new MainController();
                final Pane mainPane = (Pane) loadFxml(mainController);

                setFullyResizable(mainPane);

                appScenePane.getChildren().clear();
                appScenePane.getChildren().add(mainPane);
                mainController.mainTitleAnimation();
            }
        });
        return switchToMain;
    }

    private static void setFullyResizable(Node child) {
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
    }

    @Override
    public void stop() {
        Singletons.shutdownAll();
    }

    static void main(String[] args) {
        launch(args);
    }
}
