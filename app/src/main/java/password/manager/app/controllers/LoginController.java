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

package password.manager.app.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordField;

public class LoginController extends AbstractController {
    private final BooleanProperty switchToMain;

    public LoginController(BooleanProperty switchToMain) {
        this.switchToMain = switchToMain;
    }

    @FXML
    private Label loginTitle;

    @FXML
    private ReadablePasswordField loginPassword;

    @FXML
    private Button loginSubmitBtn;

    private Timeline wrongPasswordTimeline;

    public void initialize(URL location, ResourceBundle resources) {
        wrongPasswordTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> {
                    loginSubmitBtn.setDisable(true);
                    loginSubmitBtn.setStyle("-fx-border-color: -fx-color-red");
                }),
                new KeyFrame(Duration.seconds(1), _ -> {
                    loginSubmitBtn.setDisable(false);
                    clearStyle(loginSubmitBtn);
                }));

        final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
        langResources.bindTextProperty(loginTitle, "welcome_back");
        langResources.bindTextProperty(loginSubmitBtn, "lets_go");

        loginPassword.setOnAction(_ -> doLogin());
        loginPassword.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                loginPassword.requestFocus();
            }
        });
    }

    @FXML
    public void doLogin() {
        if (checkTextFields(loginPassword.getTextField())) {
            wrongPasswordTimeline.stop();
            IOManager.getInstance().authenticate(loginPassword.getText());

            if (IOManager.getInstance().isAuthenticated()) {
                switchToMain.set(true);
            } else {
                wrongPasswordTimeline.playFromStart();
            }

            clearTextFields(loginPassword.getTextField());
        }
    }
}
