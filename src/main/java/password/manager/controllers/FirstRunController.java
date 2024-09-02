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

import javafx.application.HostServices;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class FirstRunController extends AbstractController {
    private final BooleanProperty switchToMain;

    public FirstRunController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices, BooleanProperty switchToMain) {
        super(ioManager, langResources, hostServices);
        this.switchToMain = switchToMain;
    }

    @FXML
    public Label firstRunTitle, firstRunDescTop, firstRunDescBtm, firstRunTermsCred, firstRunDisclaimer;

    @FXML
    public TextField firstRunPasswordVisible;

    @FXML
    public PasswordField firstRunPasswordHidden;

    @FXML
    public CheckBox firstRunCheckBox;

    @FXML
    public Button firstRunSubmitBtn;

    @FXML
    public ProgressBar firstRunPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        langResources.bindTextProperty(firstRunTitle, "hi");
        langResources.bindTextProperty(firstRunDescTop, "first_run.desc.top");
        langResources.bindTextProperty(firstRunDescBtm, "first_run.desc.btm");
        langResources.bindTextProperty(firstRunCheckBox, "first_run.check_box");
        langResources.bindTextProperty(firstRunTermsCred, "terms_credits");
        langResources.bindTextProperty(firstRunSubmitBtn, "lets_go");
        langResources.bindTextProperty(firstRunDisclaimer, "first_run.disclaimer");

        bindTextProperty(firstRunPasswordHidden, firstRunPasswordVisible);
        bindPasswordStrength(firstRunPassStr, firstRunPasswordVisible);
    }

    @FXML
    public void doFirstRun() {
        if (checkTextFields(firstRunPasswordVisible, firstRunPasswordHidden) && firstRunCheckBox.isSelected()) {
            ioManager.changeMasterPassword(firstRunPasswordVisible.getText());
            switchToMain.set(true);
        }
    }
}
