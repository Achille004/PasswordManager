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

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.Getter;
import password.manager.app.singletons.IOManager;

import org.jetbrains.annotations.NotNull;

public class App extends Application {
    public static final String APP_NAME = "Password Manager";
    public static final String APP_VERSION = "3.1.0";

    public static final String ROOT_STYLESHEET = App.class.getResource("/fxml/css/root.css").toExternalForm();
    public static final String AUTOCOMPLETION_STYLESHEET = App.class.getResource("/fxml/css/autocompletion.css").toExternalForm();
    public static final Image MAIN_ICON = new Image(App.class.getResource("/images/icon.png").toExternalForm());

    private static @Getter HostServices appHostServices;
    private static @Getter AnchorPane appScenePane;
    private static @Getter Parameters appParameters;

    @Override
    public void start(@NotNull Stage primaryStage) {
        appHostServices = getHostServices();
        appScenePane = new AnchorPane();
        appParameters = getParameters();

        appScenePane.getStylesheets().addAll(ROOT_STYLESHEET, AUTOCOMPLETION_STYLESHEET);

        Font.loadFont(getClass().getResourceAsStream("/font/Roboto-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/font/Roboto-BoldItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/font/Roboto-Italic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/font/Roboto-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/font/Charm-Bold.ttf"), 14);

        primaryStage.setTitle(APP_NAME);
        primaryStage.getIcons().add(MAIN_ICON);
        primaryStage.setOnCloseRequest(_ -> Platform.exit());
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(appScenePane, 900, 600));

        AppManager.startApp();
        primaryStage.show();
    }

    @Override
    public void stop() {
        IOManager.getInstance().requestShutdown();
    }

    static void main(String[] args) {
        launch(args);
    }
}
