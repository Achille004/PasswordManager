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

package password.manager.lib;

import static password.manager.lib.Utils.*;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import lombok.AccessLevel;
import lombok.Getter;

public class ReadablePasswordFieldWithStr extends AnchorPane implements Initializable, PasswordInputControl, AnimationAwareControl {

    private static final Function<String, Double> PROGRESS_EXTRACTOR = pass -> {
        double passwordStrength = passwordStrength(pass);
        return doubleSquash(0d, (passwordStrength - 20d) / 30d, 1d);
    };

    private final AnimationController<String> animationController;

    @FXML
    private ReadablePasswordField passwordField;

    @FXML
    private @Getter(value=AccessLevel.PACKAGE) ProgressBar passwordStrengthBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert (passwordField != null) : "fx:id=\"passwordField\" was not injected.";
        assert (passwordStrengthBar != null) : "fx:id=\"passwordStrengthBar\" was not injected.";

        this.layoutBoundsProperty().addListener((_, _, newValue) -> setPassStrSize(newValue));

        this.minWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMinWidth(v);
            passwordStrengthBar.setMinWidth(v);
        });

        this.minHeightProperty().addListener((_, _, newValue) -> {
            double v = Math.max(10, newValue.doubleValue());
            passwordField.setMinHeight(v - 10);
            passwordStrengthBar.setMinHeight(10);
        });

        this.prefWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setPrefWidth(v);
            passwordStrengthBar.setPrefWidth(v);
        });

        this.prefHeightProperty().addListener((_, _, newValue) -> {
            double v = Math.max(10, newValue.doubleValue());
            passwordField.setPrefHeight(v - 10);
            passwordStrengthBar.setPrefHeight(10);
        });

        this.maxWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMaxWidth(v);
            passwordStrengthBar.setMaxWidth(v);
        });

        this.maxHeightProperty().addListener((_, _, newValue) -> {
            double v = Math.max(10, newValue.doubleValue());
            passwordField.setMaxHeight(v - 10);
            passwordStrengthBar.setMaxHeight(10);
        });
    }

    public ReadablePasswordFieldWithStr() {
        FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/readablePasswordFieldWithStr/index.fxml")));
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        final Function<Double, KeyValue> STYLE_FUNCTION = prog -> {
            Node bar = passwordStrengthBar.lookup(".bar");
            if(bar == null) return null;
            return new KeyValue(bar.styleProperty(), "-fx-background-color:" + passwordStrengthGradient(prog) + ";");
        };

        try {
            fxmlLoader.load();
            animationController = new AnimationController<>(passwordField.textProperty(),
                    PROGRESS_EXTRACTOR, passwordStrengthBar.progressProperty(),
                    STYLE_FUNCTION, passwordStrengthBar);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    // Adjust height to account for the ProgressBar
    // TODO also scale the ProgressBar

    private void setPassStrSize(Bounds bounds) {
        double width = bounds.getWidth(), height = bounds.getHeight();
        if(height < 10) height = 10;

        passwordStrengthBar.setLayoutY(height - 10);
        passwordStrengthBar.setPrefSize(width, 10);
    }

    public TextField getTextField() {
        return passwordField.getTextField();
    }

    ///// PASSWORD INPUT CONTROL METHODS /////

    @Override
    public BooleanProperty readableProperty() {
        return passwordField.readableProperty();
    }

    @Override
    public void setReadable(boolean readable) {
        passwordField.setReadable(readable);
    }

    @Override
    public void toggleReadable() {
        passwordField.toggleReadable();
    }

    @Override
    public StringProperty textProperty() {
        return passwordField.textProperty();
    }

    @Override
    public boolean isReadable() {
        return passwordField.isReadable();
    }

    @Override
    public void setText(String text) {
        passwordField.setText(text);
    }

    @Override
    public String getText() {
        return passwordField.getText();
    }

    @Override
    public StringProperty promptTextProperty() {
        return passwordField.promptTextProperty();
    }

    @Override
    public void setPromptText(String text) {
        passwordField.setPromptText(text);
    }

    @Override
    public String getPromptText() {
        return passwordField.getPromptText();
    }

    @Override
    public ReadOnlyIntegerProperty caretPositionProperty() {
        return passwordField.caretPositionProperty();
    }

    @Override
    public void positionCaret(int pos) {
        passwordField.positionCaret(pos);
    }

    @Override
    public int getCaretPosition() {
        return passwordField.getCaretPosition();
    }

    @Override
    public void requestFocus() {
        passwordField.requestFocus();
    }

    @Override
    public void setOnAction(EventHandler<ActionEvent> value) {
        passwordField.setOnAction(value);
    }

    ///// ANIMATION AWARE CONTROL METHODS /////

    @Override
    public AnimationController<?> getAnimationController() {
        return animationController;
    }

    @Override
    public void onListenerDetached() {
        passwordStrengthBar.setVisible(false);
    }

    @Override
    public void onListenerAttached() {
        passwordStrengthBar.setVisible(true);
    }
}
