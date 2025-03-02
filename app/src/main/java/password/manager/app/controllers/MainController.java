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

import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import password.manager.app.controllers.views.AbstractViewController;
import password.manager.app.controllers.views.DecrypterController;
import password.manager.app.controllers.views.EncrypterController;
import password.manager.app.controllers.views.HomeController;
import password.manager.app.controllers.views.SettingsController;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.ObservableResourceFactory;

public class MainController extends AbstractController {
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

    private Button lastSidebarButton = null;
    private Timeline titleAnimation;

    @FXML
    public BorderPane mainPane;

    @FXML
    public Label psmgTitle, mainTitle;

    @FXML
    public Button homeButton, folderButton;

    private AnchorPane homePane;
    private GridPane encrypterPane, decrypterPane, settingsPane;
    private AbstractViewController homeController, encrypterController, decrypterController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        homeController = new HomeController(ioManager, langResources, hostServices);
        homePane = (AnchorPane) loadFxml("/fxml/views/home.fxml", homeController);

        encrypterController = new EncrypterController(ioManager, langResources, hostServices);
        encrypterPane = (GridPane) loadFxml("/fxml/views/encrypter.fxml", encrypterController);

        decrypterController = new DecrypterController(ioManager, langResources, hostServices);
        decrypterPane = (GridPane) loadFxml("/fxml/views/decrypter.fxml", decrypterController);

        settingsController = new SettingsController(ioManager, langResources, hostServices);
        settingsPane = (GridPane) loadFxml("/fxml/views/settings.fxml", settingsController);

        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), _ -> psmgTitle.setText(str)));
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            ioManager.getLogger().addInfo("Unsupported action: Desktop.Action.OPEN");
            folderButton.setVisible(false);
        }

        super.loadEula();
        homeButton(null);
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    @FXML
    public void homeButton(ActionEvent event) {
        sidebarButtonAction(null, homeController, homePane, "");
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, encrypterController, encrypterPane, "encryption");
    }

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, decrypterController, decrypterPane, "decryption");
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, settingsController, settingsPane, "settings");
    }

    @FXML
    public void folderSidebarButton(ActionEvent event) {
        Platform.runLater(() ->
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(IOManager.FILE_PATH.toFile());
                } catch (IOException e) {
                    ioManager.getLogger().addError(e);
                }
            })
        );
    }

    private <T extends AbstractViewController, S extends Pane> void sidebarButtonAction(ActionEvent event, @NotNull T destinationController, S destinationPane, @NotNull String mainTitleKey) {
        // Show selected pane
        destinationController.reset();
        mainPane.centerProperty().set(destinationPane);

        // Set main title
        boolean isNotHome = !mainTitleKey.isBlank();
        homeButton.setVisible(isNotHome);
        mainTitle.setVisible(isNotHome);
        langResources.bindTextProperty(mainTitle, mainTitleKey);

        // Lowlight the previous button, if there is one
        if (lastSidebarButton != null) {
            lastSidebarButton.setStyle("-fx-background-color: #202428");
        }

        // Get and highlight the new button
        if (event != null) {
            lastSidebarButton = (Button) event.getSource();
            lastSidebarButton.setStyle("-fx-background-color: #42464a");
        }
    }
}
