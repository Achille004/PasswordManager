package main;

import static main.utils.Utils.checkTextFields;
import static main.utils.Utils.clearTextFields;
import static main.utils.Utils.selectedItemInChoiceBox;
import static main.utils.Utils.setChoiceBoxItems;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        fileManager.loadDataFile();

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
        encryptSoftware.setStyle("-fx-border-color: #a7acb1;");
        encryptUsername.setStyle("-fx-border-color: #a7acb1;");
        encryptPasswordVisible.setStyle("-fx-border-color: #a7acb1;");
        encryptPasswordHidden.setStyle("-fx-border-color: #a7acb1;");

        encryptClear();

        encryptPane.toFront();
        setMainTitle("Encryption");

        highlightSidebarButton(event);
    }

    public void encryptClear() {
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
            fileManager.addAccount(software, username, password);

            encryptClear();
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
        decryptSoftware.setStyle("-fx-border-color: #a7acb1;");
        decryptUsername.setStyle("-fx-border-color: #a7acb1;");
        decryptPasswordVisible.setStyle("-fx-border-color: #a7acb1;");
        decryptPasswordHidden.setStyle("-fx-border-color: #a7acb1;");
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

            strb.append(i + 1).append(") ");

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
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
        decryptChoiceBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        int index = selectedItemInChoiceBox(decryptChoiceBox);
        if (index >= 0) {
            if (checkTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden)) {
                // get the new software, username and password
                String software = decryptSoftware.getText();
                String username = decryptUsername.getText();
                String password = decryptPasswordVisible.getText();
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

    // #region Settings
    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        setMainTitle("Settings");

        highlightSidebarButton(event);
    }
    // #endregion

    @FXML
    public void homeButton(ActionEvent event) {
        homePane.toFront();
        setMainTitle("");

        highlightSidebarButton(null);
    }

    @FXML
    public void showPassword(MouseEvent event) {
        Object obj = event.getSource();
        
        if(obj instanceof Node) {
            ((Node) obj).getParent().toBack();
        }
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