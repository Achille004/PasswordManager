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

import static password.manager.utils.Utils.passwordStrength;
import static password.manager.utils.Utils.passwordStrengthGradient;

import java.io.IOException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import password.manager.controllers.extra.EulaController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public abstract class AbstractController implements Initializable {
    protected final IOManager ioManager;
    protected final ObservableResourceFactory langResources;
    protected final HostServices hostServices;
    protected final Stage eulaStage;

    protected AbstractController(@NotNull IOManager ioManager, @NotNull ObservableResourceFactory langResources, @NotNull HostServices hostServices) {
        this.ioManager = ioManager;
        this.langResources = langResources;
        this.hostServices = hostServices;

        eulaStage = new Stage();
        eulaStage.setTitle(langResources.getValue("terms_credits"));
        eulaStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
        eulaStage.setResizable(false);
        eulaStage.setScene(new Scene(loadFxml("/fxml/extra/eula.fxml", new EulaController(ioManager, hostServices)), 900, 600));
    }

    @FXML
    public void showPassword(@NotNull MouseEvent event) {
        Object obj = event.getSource();

        if (obj instanceof Node) {
            ((Node) obj).getParent().toBack();
        }
    }

    @FXML
    public void showEula(MouseEvent event) {
        if (eulaStage == null) {
            ioManager.getLogger().addError(new IOException("Could not load 'eula.fxml'"));
            return;
        }

        eulaStage.show();
    }

    protected <S extends Initializable> Parent loadFxml(String path, S controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(path)));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            ioManager.getLogger().addError(e);
            return null;
        }
    }

    protected static <T extends TextInputControl> void bindPasswordStrength(@NotNull ProgressBar progressBar, @NotNull T textElement) {
        ObservableList<Node> passStrChildren = progressBar.getChildrenUnmodifiable();
        textElement.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (!passStrChildren.isEmpty()) {
                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(progressBar.progressProperty(), progressBar.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(progressBar.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    protected static <T extends TextInputControl, S extends TextInputControl> void bindTextProperty(@NotNull T e1, @NotNull S e2) {
        e1.textProperty().addListener((options, oldValue, newValue) -> e2.setText(newValue));
        e2.textProperty().addListener((options, oldValue, newValue) -> e1.setText(newValue));
    }

    @SafeVarargs
    protected static <T extends TextInputControl> boolean checkTextFields(T @NotNull... fields) {
        boolean nonEmpty = true;

        for (@NotNull T field : fields) {
            if (field.getText().isBlank()) {
                nonEmpty = false;
                field.setStyle("-fx-border-color: #ff5f5f");
            } else {
                field.setStyle("-fx-border-color: #a7acb1");
            }
        }

        return nonEmpty;
    }

    @SafeVarargs
    protected static <T extends TextInputControl> void clearTextFields(T @NotNull... fields) {
        for (@NotNull T field : fields) {
            field.clear();
        }
    }

    @SafeVarargs
    protected static <T extends Node> void clearStyle(T @NotNull... nodes) {
        for (@NotNull T node : nodes) {
            node.setStyle("");
        }
    }
}