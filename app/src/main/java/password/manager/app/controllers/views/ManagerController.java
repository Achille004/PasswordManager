package password.manager.app.controllers.views;

import static password.manager.app.utils.Utils.checkValidUi;

import java.net.URL;
import java.util.ResourceBundle;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.Account;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.Logger;
import password.manager.app.utils.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class ManagerController extends AbstractViewController {
    public ManagerController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    private ListView<Account> accountListView;

    @FXML
    private TabPane accountTabPane;
    
    @FXML
    private Tab addTab, homeTab;

    public void initialize(URL location, ResourceBundle resources) {
        ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();
        SortedList<Account> accountList = ioManager.getSortedAccountList();

        accountListView.setItems(accountList);

        // Set cell factory to control how Account objects are displayed
        accountListView.cellFactoryProperty().bind(sortingOrderProperty.map(this::accountCellFactory));

        accountList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            SortingOrder sortingOrder = sortingOrderProperty.getValue();
            return sortingOrder != null ? sortingOrder.getComparator() : null;
        }, sortingOrderProperty));

        /* accountListView.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newItem) -> {
                    resetKeepSelection();
                    if (newItem != null) {
                        // shows the software, username and account of the selected account
                        editorSoftware.setText(newItem.getSoftware());
                        editorUsername.setText(newItem.getUsername());
                        String accPassword = ioManager.getAccountPassword(newItem);
                        editorPassword.setText(accPassword);
                    }
                });*/

        loadHomeTab();
    }

    public void reset() {
        // No specific reset actions for the ManagerController
    }

    public void loadHomeTab() {
        Logger.getInstance().addInfo("Loading home pane...");
        AbstractViewController homeController = new HomeController(ioManager, langResources, hostServices);
        Pane homePane = (Pane) loadFxml("/fxml/views/manager/home.fxml", homeController);

        checkValidUi(homePane, "home", ioManager, langResources);
        homeTab.setContent(homePane);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull Callback<ListView<Account>, ListCell<Account>> accountCellFactory(SortingOrder order) {
        return _ -> new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                } else {
                    setText(order.convert(account));
                }
            }
        };
    }

    class HomeController extends AbstractViewController {
        public HomeController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
            super(ioManager, langResources, hostServices);
        }

        @FXML
        private Label homeDescTop, homeDescBtm;

        public void initialize(URL location, ResourceBundle resources) {
            langResources.bindTextProperty(homeDescTop, "home_desc.top");
            langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
        }

        public void reset() {
            homeDescTop.sceneProperty().addListener((_, _, newScene) -> {
                if (newScene != null) {
                    homeDescTop.requestFocus();
                }
            });
        }
    }

    class EditorController extends AbstractViewController {
        private @Setter @Getter Account account;
        
        public EditorController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
            super(ioManager, langResources, hostServices);
        }

        @FXML
        private TextField editorSoftware, editorUsername;
        @FXML
        private ReadablePasswordFieldWithStr editorPassword;
        
        @FXML
        private Button editorSaveBtn;
        private Timeline editorSaveTimeline;

        @FXML
        private Button editorDeleteBtn;
        private boolean editorDeleteCounter = false;
        
        @FXML
        private Label editorAccSelLbl, editorSoftwareLbl, editorUsernameLbl, editorPasswordLbl;

        public void initialize(URL location, ResourceBundle resources) {
            ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

            langResources.bindTextProperty(editorSoftwareLbl, "software");
            langResources.bindTextProperty(editorUsernameLbl, "username");
            langResources.bindTextProperty(editorPasswordLbl, "password");

            editorSoftware.setOnAction(_ -> editorUsername.requestFocus());
            editorUsername.setOnAction(_ -> editorPassword.requestFocus());
            editorPassword.setOnAction(this::editorSave);

            editorSoftware.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    editorSoftware.requestFocus();
                }
            });

            editorSaveTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, _ -> editorSaveBtn.setStyle("-fx-background-color: #0e0")),
                    new KeyFrame(Duration.seconds(1), _ -> clearStyle(editorSaveBtn)));
        }

        public void reset() {
            editorPassword.resetProgress();
            resetKeepSelection();
            clearTextFields(editorSoftware, editorUsername, editorPassword.getTextField());
            editorPassword.setReadable(false);
        }
        
        private void resetKeepSelection() {
            editorDeleteCounter = false;
            clearStyle(editorSoftware, editorUsername, editorPassword.getTextField(), editorDeleteBtn);
            editorSoftware.requestFocus();
        }

        @FXML
        public void editorSave(ActionEvent event) {
            // when the deleteCounter is true it means that the user has confirmed the
            // elimination
            if (checkTextFields(editorSoftware, editorUsername, editorPassword.getTextField())) {
                resetKeepSelection();
                editorSaveTimeline.playFromStart();

                // get the new software, username and password
                String software = editorSoftware.getText();
                String username = editorUsername.getText();
                String password = editorPassword.getText();
                // save the new attributes of the account

                if(account == null) {
                    ioManager.addAccount(software, username, password);
                    // TODO RESET
                    reset();
                } else {
                    ioManager.editAccount(account, software, username, password);
                }
            }
        }

        @FXML
        public void editorDelete(ActionEvent event) {
            // when the deleteCounter is true it means that the user has confirmed the
            // elimination
            if (editorDeleteCounter) {
                reset();

                // removes the selected account from the list
                ioManager.removeAccount(account);
            } else {
                editorDeleteBtn.setStyle("-fx-background-color: #ff5f5f");
                editorDeleteCounter = true;
            }
        }
    }
}
