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

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import lombok.Getter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class ReadablePasswordField extends AnchorPane implements Initializable, PasswordInputControl {

    private final Image showingImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/open-eye.png")));
    private final Image hiddenImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/closed-eye.png")));

    private final BooleanProperty readable = new SimpleBooleanProperty(false);

    @FXML
    private ImageView imageView;

    @FXML
    private @Getter TextField textField;

    @FXML
    private PasswordField passwordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert (imageView != null) : "fx:id=\"imageView\" was not injected.";
        assert (textField != null) : "fx:id=\"textField\" was not injected.";
        assert (passwordField != null) : "fx:id=\"passwordField\" was not injected.";

        textField.textProperty().bindBidirectional(passwordField.textProperty());
        textField.styleProperty().bindBidirectional(passwordField.styleProperty());
        textField.promptTextProperty().bindBidirectional(passwordField.promptTextProperty());

        this.layoutBoundsProperty().addListener((_, _, newValue) -> setImageSize(newValue));

        this.minWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMinWidth(v);
            textField.setMinWidth(v);
        });

        this.minHeightProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMinHeight(v);
            textField.setMinHeight(v);
        });

        this.prefWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setPrefWidth(v);
            textField.setPrefWidth(v);
        });

        this.prefHeightProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setPrefHeight(v);
            textField.setPrefHeight(v);
        });

        this.maxWidthProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMaxWidth(v);
            textField.setMaxWidth(v);
        });

        this.maxHeightProperty().addListener((_, _, newValue) -> {
            double v = newValue.doubleValue();
            passwordField.setMaxHeight(v);
            textField.setMaxHeight(v);
        });

        readable.addListener((_, _, newValue) -> {
            if (newValue) {
                imageView.setImage(showingImage);
                textField.setVisible(true);
                passwordField.setVisible(false);
            } else {
                imageView.setImage(hiddenImage);
                textField.setVisible(false);
                passwordField.setVisible(true);
            }
        });

        AnchorPane.setTopAnchor(imageView, 0.0);
        imageView.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> toggleReadable());
        imageView.setImage(hiddenImage);
    }

    public ReadablePasswordField() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/readablePasswordField/index.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void setImageSize(Bounds bounds) {
        double width = bounds.getWidth(), height = bounds.getHeight();
        if (width <= 0 || height <= 0) return;

        imageView.setFitWidth(height);
        imageView.setFitHeight(height);

        // Leave a tiny gap between the image and the right edge of the control
        AnchorPane.setRightAnchor(imageView, height * 0.1);
    }

    ///// PASSWORD INPUT CONTROL METHODS /////

    @Override
    public BooleanProperty readableProperty() {
        return readable;
    }

    @Override
    public void setReadable(boolean readable) {
        // Use XOR to avoid unnecessary updates
        if (readable ^ isReadable()) {
            this.readable.set(readable);
        }
    }

    @Override
    public boolean isReadable() {
        return readable.get();
    }

    @Override
    public void toggleReadable() {
        setReadable(!isReadable());
    }

    @Override
    public StringProperty textProperty() {
        return passwordField.textProperty();
    }

    @Override
    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    public String getText() {
        return textField.getText();
    }

    @Override
    public StringProperty promptTextProperty() {
        return textField.promptTextProperty();
    }

    @Override
    public void setPromptText(String text) {
        textField.setPromptText(text);
    }

    @Override
    public String getPromptText() {
        return textField.getPromptText();
    }

    @Override
    public void requestFocus() {
        if(isReadable()) {
            textField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }

    @Override
    public void setOnAction(EventHandler<ActionEvent> value) {
        passwordField.setOnAction(value);
        textField.setOnAction(value);
    }
}
