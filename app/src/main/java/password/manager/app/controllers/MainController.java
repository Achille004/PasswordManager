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

import static password.manager.app.Utils.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import password.manager.app.controllers.views.AbstractViewController;
import password.manager.app.controllers.views.ManagerController;
import password.manager.app.controllers.views.SettingsController;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;

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

    private final Image settingsImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icons/navbar/settings.png")));
    private final Image backImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icons/navbar/back.png")));

    private boolean isSettingsOpen = false;
    private Timeline titleAnimation;

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label psmgTitle;
    
    @FXML
    private ImageView settingsButtonImageView;

    @FXML
    private Button folderButton;

    // Keep views cached once they are loaded
    private Pane managerPane, settingsPane;
    private AbstractViewController managerController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Logger.getInstance().addInfo("Unsupported action: Desktop.Action.OPEN");
            folderButton.setVisible(false);
        }

        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), _ -> psmgTitle.setText(str)));
        }
        
        managerController = new ManagerController();
        managerPane = (Pane) loadFxml("/fxml/views/manager.fxml", managerController);

        swapOnMainPane(managerController, managerPane);
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    @FXML
    private void folderNavBarAction(ActionEvent event) {
        Thread.startVirtualThread(() -> {
            try {
                Desktop.getDesktop().open(IOManager.FILE_PATH.toFile());
            } catch (IOException e) {
                Logger.getInstance().addError(e);
            }
        });
    }

    @FXML
    public void settingsNavBarAction(ActionEvent event) {
        // Load lazily
        if(settingsPane == null || settingsController == null) {
            settingsController = new SettingsController();
            settingsPane = (Pane) loadFxml("/fxml/views/settings.fxml", settingsController);
        }

        isSettingsOpen = !isSettingsOpen;
        if (isSettingsOpen) {
            swapOnMainPane(settingsController, settingsPane);
            settingsButtonImageView.setImage(backImage);
        } else {
            swapOnMainPane(managerController, managerPane);
            settingsButtonImageView.setImage(settingsImage);
        }
    }

    private void swapOnMainPane(@NotNull AbstractViewController destinationController, @NotNull Pane destinationPane) {
        // Show selected pane
        destinationController.reset();
        mainPane.centerProperty().set(destinationPane);
    }
}
