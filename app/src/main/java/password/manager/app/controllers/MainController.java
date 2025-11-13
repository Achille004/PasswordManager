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

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import password.manager.app.App;
import password.manager.app.controllers.extra.PopupContentController;
import password.manager.app.controllers.main.ManagerController;
import password.manager.app.controllers.main.SettingsController;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.IOManager.SaveState;
import password.manager.app.singletons.Logger;

public class MainController extends AbstractController {
    private static final String[] titleStages;

    static {
        final String title = App.APP_NAME;
        final char[] lower = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] upper = "ABCDEFGHIJKLMNOPQRTSUVWXYZ".toCharArray();

        final LinkedList<String> stages = new LinkedList<>();
        StringBuilder currString = new StringBuilder();

        char[] target;
        boolean isLower, isUpper;
        for (char c : title.toCharArray()) {
            isLower = Character.isLowerCase(c);
            isUpper = Character.isUpperCase(c);

            if(!isLower && !isUpper) {
                currString.append(c);
                continue;
            }

            target = isLower ? lower : upper;
            for (int i = 0; i < 26; i++) {
                stages.add(currString.toString() + target[i]);
                if (target[i] == c) break;
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
    private AbstractController managerController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

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

        createAutosavePopup();
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

    private <T extends AbstractController> void swapOnMainPane(@NotNull T destinationController, @NotNull Pane destinationPane) {
        // Show selected pane
        mainPane.centerProperty().set(destinationPane);
        destinationController.reset();
    }

    private void createAutosavePopup() {
        final int SPACING = 20;
        
        final PopupContentController popupController = new PopupContentController();
        final AnchorPane popupContent = (AnchorPane) loadFxml("/fxml/extra/popup_content.fxml", popupController);
        popupContent.setVisible(false);

        final Popup testPopup = new Popup();
        testPopup.getContent().add(popupContent);
        // testPopup.setAutoFix(true); TODO test if useful
        // DONT EVEN TRY REMOVING THESE PROPERTIES, TWO HOURS OF MY LIFE WASTED!!
        testPopup.setAutoHide(false);
        testPopup.setHideOnEscape(false);

        // Bind position to bottom-left of window //

        final Window window = App.getAppScenePane().getScene().getWindow();
        
        // Set position when height is first computed
        popupContent.heightProperty().addListener((_, oldValue, newValue) -> {
            // Only listen when height is first set
            if(oldValue.doubleValue() != 0 || newValue.doubleValue() <= 0) return; 
            final double HEIGHT = newValue.doubleValue();
            
            testPopup.setX(window.getX() + SPACING);
            testPopup.setY(window.getY() + window.getHeight() - HEIGHT - SPACING);

            window.xProperty().addListener((_, _, newX) -> testPopup.setX(newX.doubleValue() + SPACING));
            window.yProperty().addListener((_, _, newY) -> testPopup.setY(newY.doubleValue() + window.getHeight() - HEIGHT - SPACING));
            window.heightProperty().addListener((_, _, newHeight) -> testPopup.setY(window.getY() + newHeight.doubleValue() - HEIGHT - SPACING));
        });

        // This ensures that everything is actually computed
        Platform.runLater(() ->{
            popupContent.applyCss();
            popupContent.layout();
            testPopup.show(window);
        });

        // Animation and styling //

        final FadeTransition disappearTransition = new FadeTransition(Duration.seconds(3), popupContent);
        disappearTransition.setFromValue(1.0);
        disappearTransition.setToValue(0.0);
        disappearTransition.setCycleCount(1);
        disappearTransition.setOnFinished(_ -> popupContent.setVisible(false));

        IOManager.getInstance().savingProperty().addListener((_, _, newValue) -> {
            switch (newValue) {
                case SAVING -> {
                    disappearTransition.stop();
                    popupController.setState("saving", "-fx-color-element-bg");
                    popupContent.setVisible(true);
                    popupContent.setOpacity(1.0);
                }

                case SUCCESS, ERROR -> {
                    if(newValue == SaveState.SUCCESS) {
                        popupController.setState("success", "-fx-color-green");
                    } else {
                        popupController.setState("error", "-fx-color-red");
                    }
                    disappearTransition.playFromStart();
                }
            }
        });
    }
}
