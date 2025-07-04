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

import static password.manager.app.utils.Utils.*;

import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import password.manager.app.controllers.views.AbstractViewController;
import password.manager.app.controllers.views.ManagerController;
import password.manager.app.controllers.views.SettingsController;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.Logger;
import password.manager.app.utils.ObservableResourceFactory;

public class MainController extends AbstractController {
    // TODO
    // newSidebarButton.pseudoClassStateChanged(PSEUDOCLASS_NOTCH, true);
    // public static final PseudoClass PSEUDOCLASS_NOTCH = PseudoClass.getPseudoClass("notch");
    private static final String[] titleStages;

    static {
        final String title = "Password Manager";
        final char[] lower = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] upper = "ABCDEFGHIJKLMNOPQRTSUVWXYZ".toCharArray();

        final LinkedList<String> stages = new LinkedList<>();
        StringBuilder currString = new StringBuilder();

        for (char c : title.toCharArray()) {
            for (int i = 0; i < 26; i++) {
                if (Character.isLowerCase(c)) {
                    stages.add(currString.toString() + lower[i]);
                    if (lower[i] == c) {
                        break;
                    }
                } else if (Character.isUpperCase(c)) {
                    stages.add(currString.toString() + upper[i]);
                    if (upper[i] == c) {
                        break;
                    }
                } else {
                    stages.add(currString.toString() + c);
                    break;
                }
            }
            currString.append(c);
        }

        titleStages = stages.toArray(new String[0]);
    }

    public MainController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    private Timeline titleAnimation;

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label psmgTitle, mainTitle;

    @FXML
    private Button homeButton;

    // Keep views cached once they are loaded
    private Pane managerPane, settingsPane;
    private AbstractViewController managerController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), _ -> psmgTitle.setText(str)));
        }
        managerButton(null);
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    
    @FXML
    public void managerButton(ActionEvent event) {
        if(managerPane == null || managerController == null) {
            Logger.getInstance().addInfo("Loading manager pane...");
            managerController = new ManagerController(ioManager, langResources, hostServices);
            managerPane = (Pane) loadFxml("/fxml/views/manager.fxml", managerController);
            checkValidUi(managerPane, "manager", ioManager, langResources);
        }
        sidebarButtonAction(event, managerController, managerPane);
    }

    @FXML
    public void settingsButton(ActionEvent event) {
        if(settingsPane == null || settingsController == null) {
            Logger.getInstance().addInfo("Loading settings pane...");
            settingsController = new SettingsController(ioManager, langResources, hostServices);
            settingsPane = (Pane) loadFxml("/fxml/views/settings.fxml", settingsController);
            checkValidUi(settingsPane, "settings", ioManager, langResources);
        }
        sidebarButtonAction(event, settingsController, settingsPane);
    }

    private void sidebarButtonAction(ActionEvent event, @NotNull AbstractViewController destinationController, Pane destinationPane) {
        // Show selected pane
        destinationController.reset();
        mainPane.centerProperty().set(destinationPane);
    }
}
