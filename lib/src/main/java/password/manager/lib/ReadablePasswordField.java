package password.manager.lib;

import static password.manager.lib.Utils.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class ReadablePasswordField extends AnchorPane implements Initializable {

    private final Image showingImage = new Image(getClass().getResourceAsStream("/readablePasswordField/open-eye.png"));
    private final Image hiddenImage = new Image(getClass().getResourceAsStream("/readablePasswordField/closed-eye.png"));

    private final BooleanProperty readable = new SimpleBooleanProperty(false);

    @FXML
    public ImageView imageView;

    @FXML
    public TextField textField;

    @FXML
    public PasswordField passwordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert (imageView != null) : "fx:id=\"imageView\" was not injected.";
        assert (textField != null) : "fx:id=\"textField\" was not injected.";
        assert (passwordField != null) : "fx:id=\"passwordField\" was not injected.";

        textField.textProperty().bindBidirectional(passwordField.textProperty());

        this.prefWidthProperty().addListener((observable, oldValue, newValue) -> {
            double width = newValue.doubleValue(), height = this.getPrefHeight();
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
        });

        this.prefHeightProperty().addListener((observable, oldValue, newValue) -> {
            double width = this.getPrefWidth(), height = newValue.doubleValue();
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
        });

        readable.addListener((observable, oldValue, newValue) -> {
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

        imageView.setFitWidth(height * 1.2);
        imageView.setFitHeight(height * 1.2);

        imageView.setX(width - height * 1.3);
        imageView.setY(height * 0.066);

        AnchorPane.setLeftAnchor(imageView, width - height * 1.3);
        AnchorPane.setTopAnchor(imageView, height * 0.066);
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

    public void bindPasswordStrength(@NotNull ProgressBar progressBar) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            // TODO PROGRESSIVE BAR
            progressBar.setStyle("* > bar { -fx-background-color:" + passwordStrengthGradient(progress) + "; }");

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), progressBar.getProgress())),
                    new KeyFrame(Duration.millis(200), new KeyValue(progressBar.progressProperty(), progress)));

            timeline.play();
        });
    }
}
