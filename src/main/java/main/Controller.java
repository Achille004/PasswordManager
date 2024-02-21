package main;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class Controller implements Initializable {
    Button button = null;

    @FXML
    private AnchorPane homePane;

    @FXML
    private Button homeButton;

    @FXML
    private Label mainTitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        encryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            encryptPasswordHidden.setText(newValue);
        });
        encryptPasswordHidden.textProperty().addListener((observable, oldValue, newValue) -> {
            encryptPasswordVisible.setText(newValue);
        });
    }

    //#region Encrypter
    @FXML
    private GridPane encryptPane;

    @FXML
    private TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    private PasswordField encryptPasswordHidden;

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        encryptPane.toFront();
        setMainTitle("Encryption");

        highlightSidebarButton(event);
    }

    @FXML
    public void encryptSave(ActionEvent event) {
        String software = encryptSoftware.getText();
        String username = encryptUsername.getText();
        String password = encryptPasswordVisible.getText();
        System.out.format("Account: %s, %s, %s", software, username, password);

        encryptSoftware.clear();
        encryptUsername.clear();
        encryptPasswordVisible.clear();
        encryptPasswordHidden.clear();
    }
    //#endregion

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        setMainTitle("Decryption");

        highlightSidebarButton(event);
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        setMainTitle("Settings");

        highlightSidebarButton(event);
    }

    @FXML
    public void homeButton(ActionEvent event) {
        homePane.toFront();
        setMainTitle("");

        highlightSidebarButton(null);
    }

    @FXML
    public void showPassword(MouseEvent event) {
        Parent pane = ((ImageView) event.getSource()).getParent();
        pane.toBack();
    }

    private void highlightSidebarButton(ActionEvent event) {
        if (button != null) {
            button.setStyle(button.getStyle().replace("-fx-border-color: #0aba4d", "-fx-border-color: #202428"));
        }

        if (event != null) {
            button = (Button) event.getSource();
            button.setStyle(button.getStyle().replace("-fx-border-color: #202428", "-fx-border-color: #0aba4d"));
        }
    }

    private void setMainTitle(String title) {
        boolean homeButtonVisibility;
        if (title.isBlank()) {
            homeButtonVisibility = false;
        } else {
            title = " > " + title;
            homeButtonVisibility = true;
        }

        homeButton.setVisible(homeButtonVisibility);
        mainTitle.setText("Password Manager" + title);
    }
}