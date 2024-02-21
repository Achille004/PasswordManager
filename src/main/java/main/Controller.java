package main;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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

        decryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            decryptPasswordHidden.setText(newValue);
        });
        decryptPasswordHidden.textProperty().addListener((observable, oldValue, newValue) -> {
            decryptPasswordVisible.setText(newValue);
        });
        decryptChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                decryptChoiceBoxSelected(observableValue, number, number2);
            }
        });
    }

    // #region Encrypter
    @FXML
    private GridPane encryptPane;

    @FXML
    private TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    private PasswordField encryptPasswordHidden;

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        encryptSoftware.clear();
        encryptUsername.clear();
        encryptPasswordVisible.clear();
        encryptPasswordHidden.clear();

        encryptPane.toFront();
        setMainTitle("Encryption");

        highlightSidebarButton(event);
    }

    @FXML
    public void encryptSave(ActionEvent event) {
        String software = encryptSoftware.getText();
        String username = encryptUsername.getText();
        String password = encryptPasswordVisible.getText();
        System.out.format("Account: %s, %s, %s\n", software, username, password);

        encryptSoftware.clear();
        encryptUsername.clear();
        encryptPasswordVisible.clear();
        encryptPasswordHidden.clear();
    }
    // #endregion

    // #region Decrypter
    @FXML
    private GridPane decryptPane;

    @FXML
    private ChoiceBox<String> decryptChoiceBox;

    @FXML
    private TextField decryptSoftware, decryptUsername, decryptPasswordVisible;

    @FXML
    private PasswordField decryptPasswordHidden;

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        decryptSoftware.clear();
        decryptUsername.clear();
        decryptPasswordVisible.clear();
        decryptPasswordHidden.clear();

        ObservableList<String> list = FXCollections.observableArrayList();
        for (int i = 1; i < 27; i++) {
            list.add(i + ") " + (char) (96 + i));
        }
        decryptChoiceBox.setItems(list);

        decryptPane.toFront();
        setMainTitle("Decryption");

        highlightSidebarButton(event);
    }

    public void decryptChoiceBoxSelected(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
        decryptSoftware.setText("Old: " + String.valueOf(number.intValue() + 1));
        decryptUsername.setText("New: " + String.valueOf(number2.intValue() + 1));
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        String software = decryptSoftware.getText();
        String username = decryptUsername.getText();
        String password = decryptPasswordVisible.getText();
        System.out.format("Account: %s, %s, %s\n", software, username, password);

        decryptSoftware.clear();
        decryptUsername.clear();
        decryptPasswordVisible.clear();
        decryptPasswordHidden.clear();
    }

    @FXML
    public void decryptDelete(ActionEvent event) {
        String software = decryptSoftware.getText();
        String username = decryptUsername.getText();
        String password = decryptPasswordVisible.getText();
        System.out.format("Account: %s, %s, %s\n", software, username, password);

        decryptSoftware.clear();
        decryptUsername.clear();
        decryptPasswordVisible.clear();
        decryptPasswordHidden.clear();
    }
    // #endregion

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
            button.setStyle("-fx-background-color: #202428;");
        }

        if (event != null) {
            button = (Button) event.getSource();
            button.setStyle("-fx-background-color: #42464a;");
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