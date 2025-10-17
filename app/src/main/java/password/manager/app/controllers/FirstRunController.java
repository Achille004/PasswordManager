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

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class FirstRunController extends AbstractController {
    private final BooleanProperty switchToMain;

    public FirstRunController(BooleanProperty switchToMain) {
        this.switchToMain = switchToMain;
    }

    @FXML
    private Label firstRunTitle, firstRunDescTop, firstRunDescBtm, firstRunTermsCred, firstRunDisclaimer;

    @FXML
    private ReadablePasswordFieldWithStr firstRunPassword;

    @FXML
    private CheckBox firstRunCheckBox;

    @FXML
    private Button firstRunSubmitBtn;

    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

        final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
        langResources.bindTextProperty(firstRunTitle, "first_run.title");
        langResources.bindTextProperty(firstRunDescTop, "first_run.desc.top");
        langResources.bindTextProperty(firstRunDescBtm, "first_run.desc.btm");
        langResources.bindTextProperty(firstRunCheckBox, "first_run.check_box");
        langResources.bindTextProperty(firstRunTermsCred, "terms_credits");
        langResources.bindTextProperty(firstRunSubmitBtn, "lets_go");
        langResources.bindTextProperty(firstRunDisclaimer, "first_run.disclaimer");

        firstRunPassword.setOnAction(_ -> doFirstRun());
        firstRunPassword.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                firstRunPassword.requestFocus();
            }
        });

        // Force the correct size to prevent unwanted stretching
        firstRunPassword.setPrefSize(560.0, 40.0);
    }

    @FXML
    public void doFirstRun() {
        if (checkTextFields(firstRunPassword.getTextField()) && firstRunCheckBox.isSelected()) {
            IOManager.getInstance().changeMasterPassword(firstRunPassword.getText().strip());
            switchToMain.set(true);
        }
    }
}
