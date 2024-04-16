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

import static main.utils.Utils.checkTextFields;
import static main.utils.Utils.clearTextFields;
import static main.utils.Utils.selectedItemInChoiceBox;
import static main.utils.Utils.setChoiceBoxItems;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
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
import javafx.util.StringConverter;
import main.enums.SavingOrder;
import main.security.Account;
import main.utils.IOManager;
import main.utils.ObservableResourceFactory;

public class Controller implements Initializable {

    public static final Locale[] supportedLocales = { Locale.ENGLISH, Locale.ITALIAN };

    private final IOManager ioManager;
    private final ObservableResourceFactory langResources;

    public Controller() {
        this.ioManager = new IOManager();
        this.langResources = new ObservableResourceFactory();
    }

    public IOManager getFileManager() {
        return ioManager;
    }

    public ObservableResourceFactory getLangResources() {
        return langResources;
    }

    private Button lastSidebarButton = null;

    @FXML
    private AnchorPane homePane;

    @FXML
    private Button homeButton;

    @FXML
    private Label mainTitle, homeDescTop, homeDescBtm;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ioManager.loadDataFile();

        // If account
        Locale locale = ioManager.getLoginAccount() != null ? ioManager.getLoginAccount().getLocale()
                : Arrays.stream(supportedLocales).anyMatch(Locale.getDefault()::equals) ? Locale.getDefault()
                        : Locale.ENGLISH;
        langResources.setResources(ResourceBundle.getBundle("/bundles/Lang", locale));

        homeDescTop.textProperty().bind(langResources.getStringBinding("home_desc.top"));
        homeDescBtm.textProperty().bind(langResources.getStringBinding("home_desc.btm"));

        initializeEncrypt();
        initializeDecrypt();
        initializeSettings();

        // TODO login and first run (remove everything following this comment)
        ioManager.authenticate("LoginPassword");
        System.out.println("User" + (ioManager.isAuthenticated() ? " " : " not ") + "authenticated");
    }

    // #region Encrypt
    @FXML
    private GridPane encryptPane;

    @FXML
    private TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    private PasswordField encryptPasswordHidden;

    @FXML
    private Button encryptSubmitBtn;

    @FXML
    private Label encryptSoftwareLbl, encryptUsernameLbl, encryptPasswordLbl;

    private void initializeEncrypt() {
        encryptSubmitBtn.textProperty().bind(langResources.getStringBinding("submit"));
        encryptSoftwareLbl.textProperty().bind(langResources.getStringBinding("software"));
        encryptUsernameLbl.textProperty().bind(langResources.getStringBinding("username"));
        encryptPasswordLbl.textProperty().bind(langResources.getStringBinding("password"));

        encryptPasswordVisible.textProperty().addListener((options, oldValue, newValue) -> {
            encryptPasswordHidden.setText(newValue);
        });

        encryptPasswordHidden.textProperty().addListener((options, oldValue, newValue) -> {
            encryptPasswordVisible.setText(newValue);
        });
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        encryptResetStyle();
        encryptClear();

        encryptPane.toFront();
        setMainTitle(langResources.getValue("encryption"));

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
            ioManager.addAccount(software, username, password);

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

    @FXML
    private Label decryptAccSelLbl, decryptSoftwareLbl, decryptUsernameLbl, decryptPasswordLbl;

    private void initializeDecrypt() {
        decryptAccSelLbl.textProperty().bind(langResources.getStringBinding("select_acc"));
        decryptSoftwareLbl.textProperty().bind(langResources.getStringBinding("software"));
        decryptUsernameLbl.textProperty().bind(langResources.getStringBinding("username"));
        decryptPasswordLbl.textProperty().bind(langResources.getStringBinding("password"));

        decryptPasswordVisible.textProperty().addListener((options, oldValue, newValue) -> {
            decryptPasswordHidden.setText(newValue);
        });

        decryptPasswordHidden.textProperty().addListener((options, oldValue, newValue) -> {
            decryptPasswordVisible.setText(newValue);
        });

        decryptCB.getSelectionModel().selectedIndexProperty().addListener(
                (options, oldIndex, newIndex) -> {
                    decryptDeleteCounter = false;
                    decryptResetStyle();

                    int index = newIndex.intValue();
                    if (index >= 0) {
                        // gets the selected account
                        Account selectedAcc = ioManager.getAccountList().get(index);
                        String password = ioManager.getAccountPassword(selectedAcc);

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
        setMainTitle(langResources.getValue("decryption"));

        highlightSidebarButton(event);
    }

    private void decryptChoiceBoxLoad() {
        ArrayList<Account> accountList = ioManager.getAccountList();

        String[] items = new String[accountList.size()];
        StringBuilder strb;

        for (int i = 0; i < items.length; i++) {
            strb = new StringBuilder();

            strb.append(i + 1).append(") ");

            Account account = accountList.get(i);
            switch (ioManager.getLoginAccount().getSavingOrder()) {
                case Software -> strb.append(account.getSoftware()).append(" / ").append(account.getUsername());
                case Username -> strb.append(account.getUsername()).append(" / ").append(account.getSoftware());
                default -> throw new IllegalArgumentException(
                        "Invalid saving order: " + ioManager.getLoginAccount().getSavingOrder().name());
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
                ioManager.replaceAccount(index, software, username, password);

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
            ioManager.deleteAccount(index);

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
    private ChoiceBox<Locale> settingsLangCB;
    @FXML
    private ChoiceBox<SavingOrder> settingsOrderCB;

    @FXML
    private TextField settingsLoginPasswordVisible;
    @FXML
    private PasswordField settingsLoginPasswordHidden;

    @FXML
    private Button settingsChangePassButton;

    @FXML
    private Label settingsLangLbl, settingsSavingOrderLbl, settingsLoginPaswordLbl, settingsLoginPaswordDesc,
            settingsDriveConnLbl, wip;

    public void initializeSettings() {
        settingsLangLbl.textProperty().bind(langResources.getStringBinding("language"));
        settingsSavingOrderLbl.textProperty().bind(langResources.getStringBinding("saving_ord"));
        settingsLoginPaswordLbl.textProperty().bind(langResources.getStringBinding("login_pas"));
        settingsLoginPaswordDesc.textProperty().bind(langResources.getStringBinding("login_pas.desc"));
        settingsDriveConnLbl.textProperty().bind(langResources.getStringBinding("drive_con"));
        wip.textProperty().bind(langResources.getStringBinding("wip"));

        setChoiceBoxItems(settingsLangCB, supportedLocales);
        settingsLangCB.setConverter(new StringConverter<Locale>() {
            @Override
            public String toString(Locale locale) {
                String str = locale.getDisplayLanguage();
                return str.substring(0, 1).toUpperCase() + str.substring(1);
            }

            @Override
            public Locale fromString(String string) {
                return null;
            }
        });
        settingsLangCB.getSelectionModel().select(ioManager.getLoginAccount().getLocale());
        settingsLangCB.setOnAction(event -> {
            Locale locale = settingsLangCB.getSelectionModel().getSelectedItem();
            langResources.setResources(ResourceBundle.getBundle("/bundles/Lang", locale));
            ioManager.getLoginAccount().setLocale(locale);
            setMainTitle(langResources.getValue("settings"));
        });

        setChoiceBoxItems(settingsOrderCB, SavingOrder.class.getEnumConstants());
        settingsOrderCB.setConverter(new StringConverter<SavingOrder>() {
            @Override
            public String toString(SavingOrder savingOrder) {
                return savingOrder.name();
            }

            @Override
            public SavingOrder fromString(String string) {
                return null;
            }
        });
        settingsOrderCB.getSelectionModel().select(ioManager.getLoginAccount().getSavingOrder());
        settingsOrderCB.setOnAction(event -> {
            SavingOrder savingOrder = settingsOrderCB.getSelectionModel().getSelectedItem();
            ioManager.getLoginAccount().setSavingOrder(savingOrder);
            ioManager.sortAccountList();
        });

        settingsLoginPasswordVisible.textProperty().addListener((options, oldValue, newValue) -> {
            settingsLoginPasswordHidden.setText(newValue);
        });
        settingsLoginPasswordVisible.setOnAction(event -> {
            ioManager.changeLoginPassword(settingsLoginPasswordVisible.getText());
        });

        settingsLoginPasswordHidden.textProperty().addListener((options, oldValue, newValue) -> {
            settingsLoginPasswordVisible.setText(newValue);
        });
        settingsLoginPasswordHidden.setOnAction(event -> {
            ioManager.changeLoginPassword(settingsLoginPasswordHidden.getText());
        });

        // TODO ACTION
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        ioManager.displayLoginPassword(settingsLoginPasswordVisible, settingsLoginPasswordHidden);

        settingsPane.toFront();
        setMainTitle(langResources.getValue("settings"));

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