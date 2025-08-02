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

package password.manager.lib;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        ReadablePasswordField custom1 = new ReadablePasswordField();
        ReadablePasswordFieldWithStr custom2 = new ReadablePasswordFieldWithStr();
        custom2.setPrefSize(548.0, 70.0);

        AnchorPane root = new AnchorPane();
        root.getChildren().addAll(custom1, custom2);
        AnchorPane.setTopAnchor(custom1, 0.0);
        AnchorPane.setTopAnchor(custom2, 200.0);

        stage.setScene(new Scene(root));
        stage.setTitle("Test");
        stage.setWidth(900);
        stage.setHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}