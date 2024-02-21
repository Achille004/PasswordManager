package main;

import static main.utils.Utils.selectedItemInChoiceBox;
import static main.utils.Utils.setChoiceBoxItems;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
import main.security.Account;
import main.utils.FileManager;

public class Controller implements Initializable {

    private final FileManager fileManager;

    public Controller(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    Button lastSidebarButton = null;

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
        decryptChoiceBox.getSelectionModel().selectedIndexProperty().addListener(
            (observableValue, oldIndex, newIndex) -> {
                decryptDeleteCounter = false;

                int index = newIndex.intValue();
                if (index >= 0) {
                    // gets the selected account
                    Account selectedAcc = fileManager.getAccountList().get(index);
                    String password = fileManager.getAccountPassword(selectedAcc);

                    // shows the software, username and account of the selected account
                    decryptSoftware.setText(selectedAcc.getSoftware());
                    decryptUsername.setText(selectedAcc.getUsername());
                    decryptPasswordVisible.setText(password);
                    decryptPasswordHidden.setText(password);
                } else {
                    decryptClear();
                }
            }
        );
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
        // gets software, username and password written by the user
        String software = encryptSoftware.getText();
        String username = encryptUsername.getText();
        String password = encryptPasswordVisible.getText();

        if (!(software.isBlank() || username.isBlank() || password.isBlank())) {
            fileManager.addAccount(software, username, password);

            encryptSoftware.clear();
            encryptUsername.clear();
            encryptPasswordVisible.clear();
            encryptPasswordHidden.clear();
        }
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
    private Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        decryptDelete.setStyle("-fx-background-color: #a7acb1;");
        decryptChoiceBoxLoad();

        decryptPane.toFront();
        setMainTitle("Decryption");

        highlightSidebarButton(event);
    }

    public void decryptChoiceBoxLoad() {
        ArrayList<Account> accountList = fileManager.getAccountList();

        String[] items = new String[accountList.size()];
        StringBuilder strb;

        for (int i = 0; i < items.length; i++) {
            strb = new StringBuilder();

            strb.append(i + 1)
                    .append(") ");

            Account account = accountList.get(i);
            switch (fileManager.getLoginAccount().getSavingOrder()) {
                case "s" -> strb.append(account.getSoftware()).append(" / ").append(account.getUsername());
                case "u" -> strb.append(account.getUsername()).append(" / ").append(account.getSoftware());
                default -> throw new IllegalArgumentException("Invalid language!");
            }

            items[i] = strb.toString();
        }

        setChoiceBoxItems(decryptChoiceBox, items);
        decryptClear();
    }

    public void decryptClear() {
        decryptSoftware.clear();
        decryptUsername.clear();
        decryptPasswordVisible.clear();
        decryptPasswordHidden.clear();
        
        decryptChoiceBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        int index = selectedItemInChoiceBox(decryptChoiceBox);
        if (index >= 0) {
            // get the new software, username and password
            String software = decryptSoftware.getText();
            String username = decryptUsername.getText();
            String password = decryptPasswordVisible.getText();

            if (!(software.isBlank() && username.isBlank() && password.isBlank())) {
                // save the new attributes of the account
                fileManager.replaceAccount(index, software, username, password);

                decryptChoiceBoxLoad();
            }
        }
    }

    @FXML
    public void decryptDelete(ActionEvent event) {
        int index = selectedItemInChoiceBox(decryptChoiceBox);
        if (index < 0) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (decryptDeleteCounter) {
            decryptDelete.setStyle("-fx-background-color: #a7acb1;");

            // removes the selected account from the list
            fileManager.deleteAccount(index);

            decryptChoiceBoxLoad();
        } else {
            decryptDelete.setStyle("-fx-background-color: #ff5f5f;");
        }

        decryptDeleteCounter = !decryptDeleteCounter;
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
        if (lastSidebarButton != null) {
            lastSidebarButton.setStyle("-fx-background-color: #202428;");
        }

        if (event != null) {
            lastSidebarButton = (Button) event.getSource();
            lastSidebarButton.setStyle("-fx-background-color: #42464a;");
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