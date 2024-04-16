/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

package main;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private Controller controller;

    @Override
    public void start(Stage primaryStage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/index.fxml"));

        controller = new Controller();
        loader.setController(controller);

        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            controller.getFileManager().getLogger().addError(e);
            e.printStackTrace();
        }

        try {
            Scene scene = new Scene(root, 900, 600);

            primaryStage.setTitle("Password Manager");
            primaryStage.setResizable(false);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (NullPointerException e) {
            controller.getFileManager().getLogger().addError(e);
        } finally {
            stop();
        }
    }

    @Override
    public void stop() {
        controller.getFileManager().saveAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
