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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    public static final PseudoClass PSEUDOCLASS_NOTCH = PseudoClass.getPseudoClass("notch");
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
    private String lastMainTitleKey = "";
    private Timeline titleAnimation;

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label psmgTitle, mainTitle;

    @FXML
    private Button homeButton, folderButton;

    // Keep views cached once they are loaded
    private AnchorPane homePane;
    private GridPane encrypterPane, decrypterPane, settingsPane;
    private AbstractViewController homeController, encrypterController, decrypterController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), _ -> psmgTitle.setText(str)));
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            ioManager.getLogger().addInfo("Unsupported action: Desktop.Action.OPEN");
            folderButton.setVisible(false);
        }
        homeButton(null);
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    @FXML
    public void homeButton(ActionEvent event) {
        if(homePane == null || homeController == null) {
            ioManager.getLogger().addInfo("Loading home pane...");
            homeController = new HomeController(ioManager, langResources, hostServices);
            homePane = (AnchorPane) loadFxml("/fxml/views/home.fxml", homeController);
            triggerUiErrorIfNull(homePane, ioManager, langResources);
            ioManager.getLogger().addInfo("Success [home]");
        }
        sidebarButtonAction(null, homeController, homePane, "");
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        if(encrypterPane == null || encrypterController == null) {
            ioManager.getLogger().addInfo("Loading encrypter pane...");
            encrypterController = new EncrypterController(ioManager, langResources, hostServices);
            encrypterPane = (GridPane) loadFxml("/fxml/views/encrypter.fxml", encrypterController);
            triggerUiErrorIfNull(encrypterPane, ioManager, langResources);
            ioManager.getLogger().addInfo("Success [encrypter]");
        }
        sidebarButtonAction(event, encrypterController, encrypterPane, "encryption");
    }

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        if(decrypterPane == null || decrypterController == null) {
            ioManager.getLogger().addInfo("Loading decrypter pane...");
            decrypterController = new DecrypterController(ioManager, langResources, hostServices);
            decrypterPane = (GridPane) loadFxml("/fxml/views/decrypter.fxml", decrypterController);
            triggerUiErrorIfNull(decrypterPane, ioManager, langResources);
            ioManager.getLogger().addInfo("Success [decrypter]");
        }
        sidebarButtonAction(event, decrypterController, decrypterPane, "decryption");
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        if(settingsPane == null || settingsController == null) {
            ioManager.getLogger().addInfo("Loading settings pane...");
            settingsController = new SettingsController(ioManager, langResources, hostServices);
            settingsPane = (GridPane) loadFxml("/fxml/views/settings.fxml", settingsController);
            triggerUiErrorIfNull(settingsPane, ioManager, langResources);
            ioManager.getLogger().addInfo("Success [settings]");
        }
        sidebarButtonAction(event, settingsController, settingsPane, "settings");
    }

    @FXML
    public void folderSidebarButton(ActionEvent event) {
        Thread.startVirtualThread(() -> {
            try {
                Desktop.getDesktop().open(IOManager.FILE_PATH.toFile());
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        });
    }

    private void sidebarButtonAction(ActionEvent event, @NotNull AbstractViewController destinationController, Pane destinationPane, @NotNull String mainTitleKey) {
        // Show selected pane
        destinationController.reset();
        mainPane.centerProperty().set(destinationPane);

        // Set main title
        boolean isNotHome = !mainTitleKey.isBlank();
        homeButton.setVisible(isNotHome);
        mainTitle.setVisible(isNotHome);
        langResources.bindTextProperty(mainTitle, mainTitleKey);

        // Lowlight the previous button, if there is one
        if (lastSidebarButton != null && lastSidebarButton.getStyleClass().contains("navBtn")) {
            lastSidebarButton.pseudoClassStateChanged(PSEUDOCLASS_NOTCH, false);
            lastSidebarButton.getChildrenUnmodifiable().filtered(node -> node instanceof ImageView).forEach(node -> {
                ImageView imageView = (ImageView) node;
                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource("/images/icons/sidebar/" + lastMainTitleKey + "-outlined.png")).toExternalForm()));
            });
        }

        // Get and highlight the new button, if it's a navigation button
        Button newSidebarButton = (event != null && event.getSource() instanceof Button btn) ? btn : null;
        // Checking isNotHome is optional, but it can help avoid unnecessary processing.
        if(isNotHome && newSidebarButton != null && newSidebarButton.getStyleClass().contains("navBtn")) {
            newSidebarButton.pseudoClassStateChanged(PSEUDOCLASS_NOTCH, true);
            newSidebarButton.getChildrenUnmodifiable().filtered(node -> node instanceof ImageView).forEach(node -> {
                ImageView imageView = (ImageView) node;
                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource("/images/icons/sidebar/" + mainTitleKey + "-solid.png")).toExternalForm()));
            });
        }
        
        lastMainTitleKey = mainTitleKey;
        lastSidebarButton = newSidebarButton;
    }
}
