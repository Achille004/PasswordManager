/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

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
import java.util.function.UnaryOperator;

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
import password.manager.app.App;
import password.manager.app.controllers.main.ManagerController;
import password.manager.app.controllers.main.SettingsController;
import password.manager.app.singletons.AppConfig;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.CustomPopup;


public class MainController extends AbstractController {
    private static final Duration TITLE_ANIM_TIME_UNIT = Duration.millis(8);
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

        titleStages = stages.toArray(String[]::new);
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing %s", getClass().getSimpleName());

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Logger.getInstance().addInfo("Unsupported action: Desktop.Action.OPEN");
            folderButton.setVisible(false);
        }

        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(TITLE_ANIM_TIME_UNIT.multiply(i), _ -> psmgTitle.setText(str)));
        }

        managerController = new ManagerController();
        managerPane = (Pane) loadFxml(managerController);

        createAutosavePopup();
        swapOnMainPane(managerController, managerPane);
    }

    @Override
    public String getFxmlPath() {
        return "/fxml/main.fxml";
    }

    @Override
    public void reset() {} // Not needed, will never reset

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    @FXML
    private void folderNavBarAction(ActionEvent event) {
        Thread.startVirtualThread(() -> {
            try {
                Desktop.getDesktop().open(AppConfig.getInstance().getBasePath().toFile());
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
            settingsPane = (Pane) loadFxml(settingsController);
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
        final int SPACING = 20; // px

        final CustomPopup popup = CustomPopup.Builder
                .create(
                    App.getAppScenePane().getScene().getWindow(),
                    CustomPopup.Alignment.BOTTOM_LEFT,
                    SPACING
                )
                .withStylesheets(App.ROOT_STYLESHEET, App.CUSTOMPOPUP_STYLESHEET)
                .withFadingAnimation(Duration.seconds(3))
                .build();

        popup.hidden(false); // Start hidden without animation

        final ObservableResourceFactory resources = ObservableResourceFactory.getInstance();
        IOManager.getInstance().savingProperty().addListener((_, _, newValue) -> {
            UnaryOperator<String> getString = i18nKey -> {
                String key = "popup." + i18nKey;
                try {
                    return resources.getResources().getString(key);
                } catch (Exception e) {
                    // Key missing: return key itself
                    return key;
                }
            };

            switch (newValue) {
                case SAVING -> {
                    popup.setState(getString.apply("saving"), "-fx-color-element-bg");
                    popup.visible();
                }

                case SUCCESS -> {
                    popup.setState(getString.apply("success"), "-fx-color-green");
                    popup.hidden();
                }

                case ERROR -> {
                    popup.setState(getString.apply("error"), "-fx-color-red");
                    popup.hidden();
                }
            }
        });
    }
}
