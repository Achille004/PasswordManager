package password.manager.controllers;

import static password.manager.utils.Utils.*;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class LoginController extends AbstractController {
    private final BooleanProperty switchToMain;

    public LoginController(IOManager ioManager, ObservableResourceFactory langResources, BooleanProperty switchToMain) {
        super(ioManager, langResources);
        this.switchToMain = switchToMain;
    }

    @FXML
    public Label loginTitle;

    @FXML
    public TextField loginPasswordVisible;

    @FXML
    public PasswordField loginPasswordHidden;

    @FXML
    public Button loginSubmitBtn;

    private Timeline wrongPasswordsTimeline;

    public void initialize(URL location, ResourceBundle resources) {
        wrongPasswordsTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, evt -> loginSubmitBtn.setStyle("-fx-border-color: #ff5f5f")),
                new KeyFrame(Duration.seconds(1), evt -> clearStyle(loginSubmitBtn)));

        langResources.bindTextProperty(loginTitle, "welcome_back");
        langResources.bindTextProperty(loginSubmitBtn, "lets_go");

        bindPasswordFields(loginPasswordHidden, loginPasswordVisible);
    }

    @FXML
    public void doLogin() {
        if (checkTextFields(loginPasswordVisible, loginPasswordHidden)) {
            wrongPasswordsTimeline.stop();
            ioManager.authenticate(loginPasswordVisible.getText());

            if (ioManager.isAuthenticated()) {
                switchToMain.set(true);
            } else {
                wrongPasswordsTimeline.play();
            }

            clearTextFields(loginPasswordHidden, loginPasswordVisible);
        }
    }
}
