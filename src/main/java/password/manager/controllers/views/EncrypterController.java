package password.manager.controllers.views;

import static password.manager.utils.Utils.*;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class EncrypterController extends AbstractViewController {
    public EncrypterController(IOManager ioManager, ObservableResourceFactory langResources) {
        super(ioManager, langResources);
    }

    @FXML
    public GridPane encryptPane;

    @FXML
    public TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    public PasswordField encryptPasswordHidden;

    @FXML
    public Button encryptSubmitBtn;

    @FXML
    public Label encryptSoftwareLbl, encryptUsernameLbl, encryptPasswordLbl;

    @FXML
    public ProgressBar encryptPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        langResources.bindTextProperty(encryptSubmitBtn, "submit");
        langResources.bindTextProperty(encryptSoftwareLbl, "software");
        langResources.bindTextProperty(encryptUsernameLbl, "username");
        langResources.bindTextProperty(encryptPasswordLbl, "password");

        bindPasswordFields(encryptPasswordHidden, encryptPasswordVisible);

        ObservableList<Node> passStrChildren = encryptPassStr.getChildrenUnmodifiable();
        encryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (passStrChildren.size() != 0) {
                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(encryptPassStr.progressProperty(), encryptPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(encryptPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    public void reset() {
        clearStyle(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
        clearTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
    }

    @FXML
    public void encryptSave(ActionEvent event) {
        if (checkTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden)) {
            // gets software, username and password written by the user
            String software = encryptSoftware.getText();
            String username = encryptUsername.getText();
            String password = encryptPasswordVisible.getText();
            // save the new account
            ioManager.addAccount(software, username, password);

            reset();
        }
    }
}
