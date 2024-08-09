package password.manager.controllers.views;

import static password.manager.utils.Utils.*;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import javafx.util.StringConverter;
import password.manager.enums.SortingOrder;
import password.manager.security.Account;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class DecrypterController extends AbstractViewController {
    public DecrypterController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    public ComboBox<Account> decryptCB;

    @FXML
    public TextField decryptSoftware, decryptUsername, decryptPasswordVisible;
    @FXML
    public PasswordField decryptPasswordHidden;

    @FXML
    public Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    @FXML
    public Label decryptAccSelLbl, decryptSelectedAccLbl, decryptSoftwareLbl, decryptUsernameLbl, decryptPasswordLbl;

    @FXML
    public ProgressBar decryptPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

        langResources.bindTextProperty(decryptAccSelLbl, "select_acc");
        langResources.bindTextProperty(decryptSoftwareLbl, "software");
        langResources.bindTextProperty(decryptUsernameLbl, "username");
        langResources.bindTextProperty(decryptPasswordLbl, "password");

        bindPasswordFields(decryptPasswordHidden, decryptPasswordVisible);

        ObservableList<Node> passStrChildren = decryptPassStr.getChildrenUnmodifiable();
        decryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!passStrChildren.isEmpty() && !decryptCB.getSelectionModel().isEmpty()) {
                double passwordStrength = passwordStrength(newValue);
                passwordStrength = Math.max(20d, passwordStrength);
                passwordStrength = Math.min(50d, passwordStrength);

                double progress = (passwordStrength - 20) / 30;

                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(decryptPassStr.progressProperty(), decryptPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(decryptPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });

        SortedList<Account> accountList = ioManager.getSortedAccountList();
        decryptCB.setItems(accountList);

        decryptCB.converterProperty().bind(sortingOrderProperty.map(this::accountStringConverter));
        accountList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            SortingOrder sortingOrder = sortingOrderProperty.getValue();
            return sortingOrder != null ? sortingOrder.getComparator() : null;
        }, sortingOrderProperty));

        decryptCB.getSelectionModel().selectedItemProperty().addListener(
                (options, oldItem, newItem) -> {
                    resetKeepSelection();
                    if (newItem != null) {
                        // shows the software, username and account of the selected account
                        decryptSoftware.setText(newItem.getSoftware());
                        decryptUsername.setText(newItem.getUsername());
                        String accPassword = ioManager.getAccountPassword(newItem);
                        decryptPasswordVisible.setText(accPassword);
                        decryptPasswordHidden.setText(accPassword);
                    }
                });

        ObjectProperty<Account> selectedAccount = decryptCB.valueProperty();
        decryptSelectedAccLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    SortingOrder sortingOrderValue = sortingOrderProperty.getValue();
                    return sortingOrderValue != null
                            ? accountStringConverter(sortingOrderValue).toString(selectedAccount.getValue())
                            : null;
                }, sortingOrderProperty, selectedAccount));
    }

    public void reset() {
        resetKeepSelection();
        decryptCB.getSelectionModel().clearSelection();
    }

    private void resetKeepSelection() {
        decryptDeleteCounter = false;
        clearStyle(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden, decryptDelete);
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        Account account = selectedComboBoxItem(decryptCB);
        if (account == null) {
            return;
        }

        if (checkTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden)) {
            // get the new software, username and password
            String software = decryptSoftware.getText();
            String username = decryptUsername.getText();
            String password = decryptPasswordVisible.getText();

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

    private StringConverter<Account> accountStringConverter(SortingOrder order) {
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
