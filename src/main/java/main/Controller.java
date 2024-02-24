/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

package main;

import static main.utils.Utils.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import main.enums.Language;
import main.enums.SavingOrder;
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

        initializeEncrypt();
        initializeDecrypt();
        initializeSettings();
    }

    // #region Encrypt
    @FXML
    private GridPane encryptPane;

    @FXML
    private TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    private PasswordField encryptPasswordHidden;

    private void initializeEncrypt() {
        encryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            encryptPasswordHidden.setText(newValue);
        });

        encryptPasswordHidden.textProperty().addListener((observable, oldValue, newValue) -> {
            encryptPasswordVisible.setText(newValue);
        });
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        encryptResetStyle();
        encryptClear();

        encryptPane.toFront();
        setMainTitle("Encryption");

        highlightSidebarButton(event);
    }

    private void encryptClear() {
        clearTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
    }

    private void encryptResetStyle() {
        encryptSoftware.setStyle("");
        encryptUsername.setStyle("");
        encryptPasswordVisible.setStyle("");
        encryptPasswordHidden.setStyle("");
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

    // #region Decrypt
    @FXML
    private GridPane decryptPane;

    @FXML
    private ChoiceBox<String> decryptCB;

    @FXML
    private TextField decryptSoftware, decryptUsername, decryptPasswordVisible;
    @FXML
    private PasswordField decryptPasswordHidden;

    @FXML
    private Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    private void initializeDecrypt() {
        decryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            decryptPasswordHidden.setText(newValue);
        });

        decryptPasswordHidden.textProperty().addListener((observable, oldValue, newValue) -> {
            decryptPasswordVisible.setText(newValue);
        });

        decryptCB.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, oldIndex, newIndex) -> {
                    decryptDeleteCounter = false;
                    decryptResetStyle();

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

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        decryptResetStyle();
        decryptChoiceBoxLoad();

        decryptPane.toFront();
        setMainTitle("Decryption");

        highlightSidebarButton(event);
    }

    private void decryptChoiceBoxLoad() {
        ArrayList<Account> accountList = fileManager.getAccountList();

        String[] items = new String[accountList.size()];
        StringBuilder strb;

        for (int i = 0; i < items.length; i++) {
            strb = new StringBuilder();

            strb.append(i + 1).append(") ");

            Account account = accountList.get(i);
            switch (fileManager.getLoginAccount().getSavingOrder()) {
                case Software -> strb.append(account.getSoftware()).append(" / ").append(account.getUsername());
                case Username -> strb.append(account.getUsername()).append(" / ").append(account.getSoftware());
                default -> throw new IllegalArgumentException(
                        "Invalid saving order: " + fileManager.getLoginAccount().getSavingOrder().name());
            }

            items[i] = strb.toString();
        }

        setChoiceBoxItems(decryptCB, items);
        decryptClear();
    }

    private void decryptClear() {
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
    }

    private void decryptResetStyle() {
        decryptSoftware.setStyle("");
        decryptUsername.setStyle("");
        decryptPasswordVisible.setStyle("");
        decryptPasswordHidden.setStyle("");
        decryptDelete.setStyle("");
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        int index = selectedItemInChoiceBox(decryptCB);
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
        int index = selectedItemInChoiceBox(decryptCB);
        if (index < 0) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (decryptDeleteCounter) {
            decryptDelete.setStyle("");

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
    private GridPane settingsPane;

    @FXML
    private ChoiceBox<String> settingsLangCB, settingsOrderCB;

    @FXML
    private Button settingsChangePassButton;

    public void initializeSettings() {
        // TODO Automate languages
        final String[] languages = getEnumStringValues(Language.class);
        setChoiceBoxItems(settingsLangCB, languages);

        int langIndex = Arrays.asList(languages).indexOf(fileManager.getLoginAccount().getLanguage().name());
        settingsLangCB.getSelectionModel().select(langIndex);

        settingsLangCB.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, oldIndex, newIndex) -> {
                    String selectedItem = languages[newIndex.intValue()];
                    Language lang = Language.valueOf(Language.class, selectedItem);
                    fileManager.getLoginAccount().setLanguage(lang);

                    // TODO reload language
                });

        final String[] savingOrders = getEnumStringValues(SavingOrder.class);
        setChoiceBoxItems(settingsOrderCB, savingOrders);

        int savingOrderIndex = Arrays.asList(savingOrders)
                .indexOf(fileManager.getLoginAccount().getSavingOrder().name());
        settingsOrderCB.getSelectionModel().select(savingOrderIndex);

        settingsOrderCB.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, oldIndex, newIndex) -> {
                    String selectedItem = savingOrders[newIndex.intValue()];
                    SavingOrder savingOrder = SavingOrder.valueOf(SavingOrder.class, selectedItem);
                    fileManager.getLoginAccount().setSavingOrder(savingOrder);

                    fileManager.sortAccountList();
                });
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        settingsPane.toFront();
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

        if (obj instanceof Node) {
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