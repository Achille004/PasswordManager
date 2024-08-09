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

import java.util.Objects;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class App extends Application {
    private AppManager appManager;
    private HostServices hostServices;

    @Override
    public void start(Stage primaryStage) {
        hostServices = getHostServices();

        
        AnchorPane scenePane = new AnchorPane();
        appManager = new AppManager(scenePane, hostServices);

        primaryStage.setTitle("Password Manager");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(scenePane, 900, 600));
        primaryStage.show();
    }

    @Override
    public void stop() {
        appManager.getIoManager().saveAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
