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

import static main.utils.Utils.bindPasswordFields;
import static main.utils.Utils.bindValueComparator;
import static main.utils.Utils.bindValueConverter;
import static main.utils.Utils.capitalizeWord;
import static main.utils.Utils.checkTextFields;
import static main.utils.Utils.clearStyle;
import static main.utils.Utils.clearTextFields;
import static main.utils.Utils.getFXSortedList;
import static main.utils.Utils.selectedChoiceBoxIndex;
import static main.utils.Utils.selectedChoiceBoxItem;
import static main.utils.Utils.toStringConverter;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.transformation.SortedList;
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
import main.enums.SortingOrder;
import main.security.Account;
import main.utils.IOManager;
import main.utils.ObservableResourceFactory;

public class Controller implements Initializable {

    public static final Locale[] SUPPORTED_LOCALES = { Locale.ENGLISH, Locale.ITALIAN };

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

        // If account exists, its Locale will be used, else it will use the default Locale
        ObjectProperty<Locale> locale = settingsLangCB.valueProperty();
        langResources.resourcesProperty().bind(Bindings.createObjectBinding(
                () -> {
                    Locale localeValue = locale.getValue();
                    return ResourceBundle.getBundle("/bundles/Lang",
                            localeValue != null ? localeValue : Locale.getDefault());
                },
                locale));

        langResources.bindTextProperty(homeDescTop, "home_desc.top");
        langResources.bindTextProperty(homeDescBtm, "home_desc.btm");

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
        langResources.bindTextProperty(encryptSubmitBtn, "submit");
        langResources.bindTextProperty(encryptSoftwareLbl, "software");
        langResources.bindTextProperty(encryptUsernameLbl, "username");
        langResources.bindTextProperty(encryptPasswordLbl, "password");

        bindPasswordFields(encryptPasswordHidden, encryptPasswordVisible);
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
        clearStyle(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
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
    private ChoiceBox<Account> decryptCB;

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
        langResources.bindTextProperty(decryptAccSelLbl, "select_acc");
        langResources.bindTextProperty(decryptSoftwareLbl, "software");
        langResources.bindTextProperty(decryptUsernameLbl, "username");
        langResources.bindTextProperty(decryptPasswordLbl, "password");

        bindPasswordFields(decryptPasswordHidden, decryptPasswordVisible);

        SortedList<Account> accountList = ioManager.getSortedAccountList();
        decryptCB.setItems(accountList);
        bindValueConverter(decryptCB, settingsLangCB.valueProperty(), this::accountStringConverter);

        ObjectProperty<SortingOrder> sortingOrder = settingsOrderCB.valueProperty();
        accountList.comparatorProperty().bind(Bindings.createObjectBinding(
                () -> {
                    SortingOrder sortingOrderValue = sortingOrder.getValue();
                    return sortingOrderValue != null ? sortingOrderValue.getComparator()
                            : SortingOrder.Software.getComparator();
                },
                sortingOrder));

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
        decryptClear();

        decryptPane.toFront();
        setMainTitle(langResources.getValue("decryption"));

        highlightSidebarButton(event);
    }

    private void decryptClear() {
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
    }

    private void decryptResetStyle() {
        clearStyle(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden, decryptDelete);
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        int index = selectedChoiceBoxIndex(decryptCB);
        if (index >= 0) {
            if (checkTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden)) {
                // get the new software, username and password
                String software = decryptSoftware.getText();
                String username = decryptUsername.getText();
                String password = decryptPasswordVisible.getText();
                // save the new attributes of the account
                ioManager.replaceAccount(index, software, username, password);
            }
        }
    }

    @FXML
    public void decryptDelete(ActionEvent event) {
        int index = selectedChoiceBoxIndex(decryptCB);
        if (index < 0) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (decryptDeleteCounter) {
            clearStyle(decryptDelete);

            // removes the selected account from the list
            ioManager.deleteAccount(index);
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
    private ChoiceBox<SortingOrder> settingsOrderCB;

    @FXML
    private TextField settingsLoginPasswordVisible;
    @FXML
    private PasswordField settingsLoginPasswordHidden;

    @FXML
    private Button settingsChangePassButton;

    @FXML
    private Label settingsLangLbl, settingsSortingOrderLbl, settingsLoginPasswordLbl, settingsLoginPasswordDesc,
            settingsDriveConnLbl, wip;

    public void initializeSettings() {
        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsLoginPasswordLbl, "login_pas");
        langResources.bindTextProperty(settingsLoginPasswordDesc, "login_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        SortedList<Locale> langs = getFXSortedList(SUPPORTED_LOCALES);
        settingsLangCB.setItems(langs);
        settingsLangCB.getSelectionModel().select(ioManager.getLoginAccount().getLocale());
        bindValueConverter(settingsLangCB, settingsLangCB.valueProperty(), this::languageStringConverter);
        bindValueComparator(langs, settingsLangCB.valueProperty(), settingsLangCB);
        settingsLangCB.setOnAction(event -> {
            Locale locale = selectedChoiceBoxItem(settingsLangCB);
            ioManager.getLoginAccount().setLocale(locale);
            setMainTitle(langResources.getValue("settings"));
        });

        SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());
        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(ioManager.getLoginAccount().getSortingOrder());
        bindValueConverter(settingsOrderCB, settingsLangCB.valueProperty(), this::sortingOrderStringConverter);
        bindValueComparator(sortingOrders, settingsLangCB.valueProperty(), settingsOrderCB);
        settingsOrderCB.setOnAction(event -> {
            SortingOrder sortingOrder = settingsOrderCB.getSelectionModel().getSelectedItem();
            ioManager.getLoginAccount().setSortingOrder(sortingOrder);
        });

        bindPasswordFields(settingsLoginPasswordHidden, settingsLoginPasswordVisible);
        settingsLoginPasswordVisible.setOnAction(event -> {
            ioManager.changeLoginPassword(settingsLoginPasswordVisible.getText());
        });
        settingsLoginPasswordHidden.setOnAction(event -> {
            ioManager.changeLoginPassword(settingsLoginPasswordHidden.getText());
        });
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

    private StringConverter<Account> accountStringConverter(Locale locale) {
        return toStringConverter(
                item -> item != null ? ioManager.getLoginAccount().getSortingOrder().convert(item) : null);
    }

    private StringConverter<Locale> languageStringConverter(Locale locale) {
        return toStringConverter(item -> capitalizeWord(item.getDisplayLanguage(item)));
    }

    private StringConverter<SortingOrder> sortingOrderStringConverter(Locale locale) {
        return toStringConverter(item -> langResources.getValue(item.getI18nKey()));
    }
}