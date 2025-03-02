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

package password.manager.app.controllers.views;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordField;

public class EncrypterController extends AbstractViewController {
    public EncrypterController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    public TextField encryptSoftware, encryptUsername;

    @FXML
    public ReadablePasswordField encryptPassword;

    @FXML
    public Button encryptSubmitBtn;

    @FXML
    public Label encryptSoftwareLbl, encryptUsernameLbl, encryptPasswordLbl;

    @FXML
    public ProgressBar encryptPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        langResources.bindTextProperty(encryptSubmitBtn, "submit");
        langResources.bindTextProperty(encryptSoftwareLbl, "software");
        langResources.bindTextProperty(encryptUsernameLbl, "username");
        langResources.bindTextProperty(encryptPasswordLbl, "password");

        encryptPassword.bindPasswordStrength(encryptPassStr);

        encryptSoftware.setOnAction(_ -> encryptUsername.requestFocus());
        encryptUsername.setOnAction(_ -> encryptPassword.requestFocus());
        encryptPassword.setOnAction(this::encryptSave);
    }

    public void reset() {
        clearStyle(encryptSoftware, encryptUsername, encryptPassword.textField, encryptPassword.passwordField);
        clearTextFields(encryptSoftware, encryptUsername, encryptPassword.textField, encryptPassword.passwordField);
    }

    @FXML
    public void encryptSave(ActionEvent event) {
        if (checkTextFields(encryptSoftware, encryptUsername, encryptPassword.textField, encryptPassword.passwordField)) {
            // gets software, username and password written by the user
            String software = encryptSoftware.getText();
            String username = encryptUsername.getText();
            String password = encryptPassword.getText();
            // save the new account
            ioManager.addAccount(software, username, password);

            reset();
        }
    }
}
