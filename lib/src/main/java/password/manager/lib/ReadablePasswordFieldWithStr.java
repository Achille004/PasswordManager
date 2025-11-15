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
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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

    public BooleanProperty readableProperty() {
        return passwordField.readableProperty();
    }

    public void setReadable(boolean readable) {
        passwordField.setReadable(readable);
    }

    public void toggleReadable() {
        passwordField.toggleReadable();
    }
    
    public StringProperty textProperty() {
        return passwordField.textProperty();
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
    public void doExtraStopSteps() {
        passwordStrengthBar.setVisible(false);
    }

    @Override
    public void doExtraStartSteps() {
        passwordStrengthBar.setVisible(true);
    }

    ///// HELPER METHODS /////

    private void bindPasswordStrength(@NotNull ProgressBar progressBar) {
        final Function<Double, KeyValue> styleFunction = prog -> {
            Node bar = progressBar.lookup(".bar");
            if(bar == null) return null;
            return new KeyValue(bar.styleProperty(), "-fx-background-color:" + passwordStrengthGradient(prog) + ";");
        };

        final Function<String, Double> progressExtractor = pass -> {
            double passwordStrength = passwordStrength(pass);
            passwordStrength = Math.max(0d, passwordStrength - 20d);
            return Math.min(1d, passwordStrength / 30d);
        };

        addAnimAwareProperty(passwordField.textProperty(),
                progressExtractor, progressBar.progressProperty(),
                styleFunction, progressBar);
    }
}
