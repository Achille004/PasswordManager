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

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import lombok.Getter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

        this.prefWidthProperty().addListener((_, _, newValue) -> {
            double width = newValue.doubleValue(), height = this.getPrefHeight();
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
        });

        this.prefHeightProperty().addListener((_, _, newValue) -> {
            double width = this.getPrefWidth(), height = newValue.doubleValue();
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
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

        imageView.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> toggleReadable());
        // imageView.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> setReadable(true));
        // imageView.addEventFilter(MouseEvent.MOUSE_RELEASED, _ -> setReadable(false));

        imageView.setImage(hiddenImage);
    }

    public ReadablePasswordField() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/readablePasswordField/index.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setReadable(boolean readable) {
        if (readable ^ isReadable()) {
            this.readable.set(readable);
        }
    }

    public void toggleReadable() {
        setReadable(!isReadable());
    }

    public boolean isReadable() {
        return readable.get();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public String getText() {
        return textField.getText();
    }

    @Override
    public void setPrefSize(double width, double height) {
        super.setPrefSize(width, height);
        passwordField.setPrefSize(width, height);
        textField.setPrefSize(width, height);

        imageView.setFitWidth(height);
        imageView.setFitHeight(height);

        imageView.setX(width - height * 1.1);
        imageView.setY(0);

        AnchorPane.setLeftAnchor(imageView, width - height * 1.1);
        AnchorPane.setTopAnchor(imageView, 0.0);
    }

    @Override
    public void setMinSize(double width, double height) {
        super.setMinSize(width, height);
        passwordField.setMinSize(width, height);
        textField.setMaxSize(width, height);
    }

    @Override
    public void setMaxSize(double width, double height) {
        super.setMaxSize(width, height);
        passwordField.setMaxSize(width, height);
        textField.setMaxSize(width, height);
    }

    public void setOnAction(EventHandler<ActionEvent> value) {
        passwordField.setOnAction(value);
        textField.setOnAction(value);
    }

    @Override
    public void requestFocus() {
        if(isReadable()) {
            textField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }
}
