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

package password.manager.lib;

import static password.manager.lib.Utils.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import lombok.AccessLevel;
import lombok.Getter;

public class ReadablePasswordFieldWithStr extends AnchorPane implements Initializable, PasswordInputControl, AnimationAwareControl {

    @FXML
    private ReadablePasswordField passwordField;

    @FXML
    private @Getter(value=AccessLevel.PACKAGE) ProgressBar passwordStrengthBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert (passwordField != null) : "fx:id=\"passwordField\" was not injected.";
        assert (passwordStrengthBar != null) : "fx:id=\"passwordStrengthBar\" was not injected.";
    }

    public ReadablePasswordFieldWithStr() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/readablePasswordFieldWithStr/index.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            bindPasswordStrength(passwordStrengthBar);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public TextField getTextField() {
        return passwordField.getTextField();
    }

    // Adjust height to account for the ProgressBar
    // TODO also scale the ProgressBar

    @Override
    public void setPrefSize(double width, double height) {
        super.setPrefSize(width, height);
        if(height > 10) {
            passwordField.setPrefSize(width, height - 10);
            passwordStrengthBar.layoutYProperty().set(height - 10);
            passwordStrengthBar.setPrefSize(width, 10);
        }
    }

    @Override
    public void setMinSize(double width, double height) {
        super.setMinSize(width, height);
        if(height > 10) {
            passwordField.setMinSize(width, height - 10);
            passwordStrengthBar.setMinSize(width, 10);
        }
    }

    @Override
    public void setMaxSize(double width, double height) {
        super.setMaxSize(width, height);
        if(height > 10) {
            passwordField.setMaxSize(width, height - 10);
            passwordStrengthBar.setMaxSize(width, 10);
        }
    }

    ///// PASSWORD INPUT CONTROL METHODS /////

    public void setReadable(boolean readable) {
        passwordField.setReadable(readable);
    }

    public void toggleReadable() {
        passwordField.toggleReadable();
    }

    public boolean isReadable() {
        return passwordField.isReadable();
    }

    public void setText(String text) {
        passwordField.setText(text);
    }

    public String getText() {
        return passwordField.getText();
    }

    @Override
    public void requestFocus() {
        passwordField.requestFocus();
    }

    public void setOnAction(EventHandler<ActionEvent> value) {
        passwordField.setOnAction(value);
    }

    ///// ANIMATION AWARE CONTROL METHODS /////

    @Override
    public void hideExtraElements() {
        passwordStrengthBar.setVisible(false);
    }

    @Override
    public void showExtraElements() {
        passwordStrengthBar.setVisible(true);
    }

    ///// HELPER METHODS /////

    private void bindPasswordStrength(@NotNull ProgressBar progressBar) {
        Timeline[] previousTimeline = { null };

        ChangeListener<String> listener = (_, _, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double initialProgress = progressBar.getProgress();
            double progress = (passwordStrength - 20) / 30;

            if(progress == initialProgress) return;

            Node bar = progressBar.lookup(".bar");
            if(bar == null) return;

            KeyFrame[] keyFrames = new KeyFrame[200];
            for (int i = 0; i < 200; i++) {
                double curProg = initialProgress + (progress - initialProgress) * i / 200;
                keyFrames[i] = new KeyFrame(Duration.millis(i),
                        new KeyValue(progressBar.progressProperty(), curProg),
                        new KeyValue(bar.styleProperty(), "-fx-background-color:" + passwordStrengthGradient(curProg) + ";")
                );
            }

            if (previousTimeline[0] != null) previousTimeline[0].stop();

            previousTimeline[0] = new Timeline(keyFrames);
            previousTimeline[0].play();
        };

        // Listen for text changes
        TextField textField = passwordField.getTextField();
        textField.textProperty().addListener(listener);

        // Trigger initial update once the ProgressBar skin is ready
        // This is a workaround for the fact that the skin may not be ready immediately
        progressBar.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin != null) {
                listener.changed(textField.textProperty(), "", textField.getText());
            }
        });
    }
}
