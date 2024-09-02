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

package password.manager.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class LoginController extends AbstractController {
    private final BooleanProperty switchToMain;

    public LoginController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices, BooleanProperty switchToMain) {
        super(ioManager, langResources, hostServices);
        this.switchToMain = switchToMain;
    }

    @FXML
    public Label loginTitle;

    @FXML
    public TextField masterPasswordVisible;

    @FXML
    public PasswordField masterPasswordHidden;

    @FXML
    public Button loginSubmitBtn;

    private Timeline wrongPasswordsTimeline;

    public void initialize(URL location, ResourceBundle resources) {
        wrongPasswordsTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, evt -> {
                    loginSubmitBtn.setDisable(true);
                    loginSubmitBtn.setStyle("-fx-border-color: #ff5f5f");
                }),
                new KeyFrame(Duration.seconds(1), evt -> {
                    loginSubmitBtn.setDisable(false);
                    clearStyle(loginSubmitBtn);
                }));

        langResources.bindTextProperty(loginTitle, "welcome_back");
        langResources.bindTextProperty(loginSubmitBtn, "lets_go");

        bindTextProperty(masterPasswordHidden, masterPasswordVisible);
    }

    @FXML
    public void doLogin() {
        if (checkTextFields(masterPasswordVisible, masterPasswordHidden)) {
            wrongPasswordsTimeline.stop();
            ioManager.authenticate(masterPasswordVisible.getText());

            if (ioManager.isAuthenticated()) {
                switchToMain.set(true);
            } else {
                wrongPasswordsTimeline.play();
            }

            clearTextFields(masterPasswordHidden, masterPasswordVisible);
        }
    }
}
