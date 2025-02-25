/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.controllers.views;

import static password.manager.app.utils.Utils.*;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.Account;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordField;

public class DecrypterController extends AbstractViewController {
    public DecrypterController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    public ComboBox<Account> decryptCB;

    @FXML
    public TextField decryptSoftware, decryptUsername;
    @FXML
    public ReadablePasswordField decryptPassword;

    @FXML
    public Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    @FXML
    public Label decryptAccSelLbl, decryptSoftwareLbl, decryptUsernameLbl, decryptPasswordLbl;

    @FXML
    public ProgressBar decryptPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

        langResources.bindTextProperty(decryptAccSelLbl, "select_acc");
        langResources.bindTextProperty(decryptSoftwareLbl, "software");
        langResources.bindTextProperty(decryptUsernameLbl, "username");
        langResources.bindTextProperty(decryptPasswordLbl, "password");

        decryptPassword.bindPasswordStrength(decryptPassStr);

        decryptSoftware.setOnAction(event -> {
            decryptUsername.requestFocus();
        });
        decryptUsername.setOnAction(event -> {
            decryptPassword.requestFocus();
        });
        decryptPassword.setOnAction(event -> {
            decryptSave(event);
        });

        SortedList<Account> accountList = ioManager.getSortedAccountList();
        decryptCB.setItems(accountList);

        decryptCB.converterProperty().bind(sortingOrderProperty.map(this::accountStringConverter));
        accountList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            SortingOrder sortingOrder = sortingOrderProperty.getValue();
            return sortingOrder != null ? sortingOrder.getComparator() : null;
        }, sortingOrderProperty));

        decryptCB.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldItem, newItem) -> {
                    resetKeepSelection();
                    if (newItem != null) {
                        // shows the software, username and account of the selected account
                        decryptSoftware.setText(newItem.getSoftware());
                        decryptUsername.setText(newItem.getUsername());
                        String accPassword = ioManager.getAccountPassword(newItem);
                        decryptPassword.setText(accPassword);
                    }
                });

        ObjectProperty<Account> selectedAccount = decryptCB.valueProperty();
    }

    public void reset() {
        resetKeepSelection();
        decryptCB.getSelectionModel().clearSelection();
    }

    private void resetKeepSelection() {
        decryptDeleteCounter = false;
        clearStyle(decryptSoftware, decryptUsername, decryptPassword.textField, decryptPassword.passwordField, decryptDelete);
        clearTextFields(decryptSoftware, decryptUsername, decryptPassword.textField, decryptPassword.passwordField);
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        Account account = selectedComboBoxItem(decryptCB);
        if (account == null) {
            return;
        }

        if (checkTextFields(decryptSoftware, decryptUsername, decryptPassword.textField, decryptPassword.passwordField)) {
            // get the new software, username and password
            String software = decryptSoftware.getText();
            String username = decryptUsername.getText();
            String password = decryptPassword.getText();

            reset();

            // save the new attributes of the account
            ioManager.editAccount(account, software, username, password);
        }
    }

    @FXML
    public void decryptDelete(ActionEvent event) {
        Account account = selectedComboBoxItem(decryptCB);
        if (account == null) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (decryptDeleteCounter) {
            reset();

            // removes the selected account from the list
            ioManager.removeAccount(account);
        } else {
            decryptDelete.setStyle("-fx-background-color: #ff5f5f");
            decryptDeleteCounter = true;
        }
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull StringConverter<Account> accountStringConverter(SortingOrder order) {
        return new StringConverter<>() {
            @Override
            public Account fromString(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString(Account account) {
                return account != null ? order.convert(account) : "";
            }
        };
    }
}
