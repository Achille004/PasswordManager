package password.manager.app.controllers.views;

import static password.manager.app.utils.Utils.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.Account;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.Logger;
import password.manager.app.utils.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class ManagerController extends AbstractViewController {
    // Cache for tabs associated with accounts
    private final Map<Account, Tab> tabsCache = new HashMap<>();

    // Tracks edit operations
    private volatile boolean editOperationInProgress = false;
    
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
        SortedList<Account> sortedAccountList = ioManager.getSortedAccountList();
        ObservableList<Tab> accTabs = accountTabPane.getTabs();

        accountListView.setItems(sortedAccountList);
        // Set cell factory to control how Account objects are displayed
        accountListView.cellFactoryProperty().bind(sortingOrderProperty.map(this::accountCellFactory));
        accountListView.getSelectionModel().selectedItemProperty().addListener(this.listViewHandler());
        
        sortedAccountList.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            SortingOrder sortingOrder = sortingOrderProperty.getValue();
            return sortingOrder != null ? sortingOrder.getComparator() : null;
        }, sortingOrderProperty));

        sortedAccountList.addListener((ListChangeListener<Account>) change -> {
            // Skip processing if an edit operation is in progress.
            // This is a shitty behavior of the sorted list which, in order
            // to sort, makes an adding change and then a removal change
            // separately (makes no sense, but whatever).
            if (editOperationInProgress) {
                return;
            }

            while(change.next()) {
                if (change.wasRemoved() && !change.wasAdded()) {
                    // Detect the manual trigger of the list, done by replacing the edited element with itself (same reference)
                    boolean isManualTrigger = change.wasReplaced() 
                            && change.getAddedSize() == 1 
                            && change.getRemovedSize() == 1
                            && change.getAddedSubList().get(0).equals(change.getRemoved().get(0));

                    // Skip processing if this is a manual trigger
                    if(isManualTrigger) {
                        continue; 
                    }
                    
                    // This is a true removal
                    for (Account removedAccount : change.getRemoved()) {
                        Tab tab = tabsCache.remove(removedAccount);
                        if (tab != null) {
                            accTabs.remove(tab);
                        }
                    }
                }
            }
        });

        accTabs.addListener((ListChangeListener<Tab>) change -> {
            while(change.next()) {
                if (change.wasAdded() && !change.getAddedSubList().contains(homeTab)) {
                    Platform.runLater(() -> accTabs.remove(homeTab));
                } else if (change.wasRemoved()) {
                    Platform.runLater(() -> { 
                        if(accTabs.size() <= 1) {
                            accTabs.add(0, homeTab);
                            accountTabPane.getSelectionModel().select(homeTab);
                        } else if(!change.getRemoved().contains(homeTab)) {
                            accountTabPane.getSelectionModel().select(addTab);
                        }
                    });
                }
            }
        });

        loadHomeTab();
        loadAddTab();
    }

    public void reset() {}

    private void loadHomeTab() {
        Logger.getInstance().addInfo("Loading home pane...");
        HomeController homeController = new HomeController(ioManager, langResources, hostServices);
        Pane homePane = (Pane) loadFxml("/fxml/views/manager/home.fxml", homeController);

        checkValidUi(homePane, "home", ioManager, langResources);
        homeTab.setContent(homePane);
    }

    private void loadAddTab() {
        Logger.getInstance().addInfo("Loading editor pane...");
        EditorController addController = new EditorController(ioManager, langResources, hostServices);
        Pane addPane = (Pane) loadFxml("/fxml/views/manager/editor.fxml", addController);
        checkValidUi(addPane, "editor", ioManager, langResources);

        addTab.setContent(addPane);
        addTab.setOnSelectionChanged(_ -> {
            if (addTab.isSelected()) {
                accountListView.getSelectionModel().clearSelection();
                addController.reset();
            }
        });
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

    @Contract(value = "_ -> new", pure = true)
    private @NotNull ChangeListener<Account> listViewHandler() {
        return (_, _, newItem) -> {
            if (newItem != null) {
                // If the tab is cached, reuse it
                Tab cachedTab = tabsCache.get(newItem);
                if (cachedTab != null) {
                    accountTabPane.getTabs().add(accountTabPane.getTabs().size() - 1, cachedTab);
                    accountTabPane.getSelectionModel().select(cachedTab);
                    return;
                }

                // Create a new tab for the selected account
                EditorController controller = new EditorController(ioManager, langResources, hostServices);
                Pane pane = (Pane) loadFxml("/fxml/views/manager/editor.fxml", controller);
                checkValidUi(pane, "editor", ioManager, langResources);
                
                controller.setAccount(newItem);

                Tab tab = new Tab();
                tab.textProperty().bind(Bindings.createStringBinding(() -> {
                    String software = newItem.getSoftware();
                    return software != null ? software : "";
                }, newItem.getSoftwareProperty()));
                tab.setContent(pane);
                tab.setOnSelectionChanged(_ -> {
                    if (tab.isSelected()) {
                        controller.reset();
                    }
                });

                accountTabPane.getTabs().add(accountTabPane.getTabs().size() - 1, tab);
                accountTabPane.getSelectionModel().select(tab);
                tabsCache.put(newItem, tab);
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
        private @Getter Account account;
        
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
                    new KeyFrame(Duration.ZERO, _ -> editorSaveBtn.setStyle("-fx-background-color: -fx-color-green")),
                    new KeyFrame(Duration.seconds(1), _ -> clearStyle(editorSaveBtn)));

            // Force the correct size to prevent unwanted stretching
            editorPassword.setPrefSize(548.0, 40.0);
        }

        public void reset() {
            if(account != null) {
                editorSoftware.setText(account.getSoftware());
                editorUsername.setText(account.getUsername());
                ioManager.getAccountPassword(editorPassword, account);
            } else {
                clearTextFields(editorSoftware, editorUsername, editorPassword.getTextField());
            }

            editorDeleteCounter = false;
            clearStyle(editorSoftware, editorUsername, editorPassword.getTextField(), editorDeleteBtn);
            editorSoftware.requestFocus();
            editorPassword.setReadable(false);
        }

        @FXML
        public void editorSave(ActionEvent event) {
            if (checkTextFields(editorSoftware, editorUsername, editorPassword.getTextField())) {
                editorSaveTimeline.playFromStart();

                // get the new software, username and password
                String software = editorSoftware.getText();
                String username = editorUsername.getText();
                String password = editorPassword.getText();
                
                // save the new attributes of the account
                /*  
                  I know that the different reset handling is weird, but lemme explain:
                    if the account is null, it means that the user is creating a new account,
                    so we just reset the editor, but if the account is not null, it means that
                    the user is editing an existing account, so we need to edit the account
                    and then reset the editor only if the edit was successful
                  This ensures maximum responsiveness when adding, while avoiding really weird behavior
                  when editing, like the editor creating duplicated tabs for the same account.
                */
                if(account == null) {
                    ioManager.addAccount(software, username, password);
                    reset();
                } else {
                    editOperationInProgress = true;
                    ioManager.editAccount(account, software, username, password)
                            .thenAccept(result -> Platform.runLater(() -> {
                                editOperationInProgress = false;
                                if (result) {
                                    reset();
                                } else {
                                    Logger.getInstance().addError(new RuntimeException("Failed to edit account."));
                                }
                            }))
                            .exceptionally(ex -> {
                                Platform.runLater(() -> editOperationInProgress = false);
                                Logger.getInstance().addError(ex);
                                return null;
                            });
                }
            }
        }

        @FXML
        public void editorDelete(ActionEvent event) {
            // when the deleteCounter is true it means that the user has confirmed the elimination
            if (editorDeleteCounter) {
                reset();
                // removes the selected account from the list
                ioManager.removeAccount(account);
            } else {
                editorDeleteBtn.setStyle("-fx-background-color: -fx-color-red");
                editorDeleteCounter = true;
            }
        }

        public void setAccount(Account account) {
            this.account = account;
            if (account != null) {
                editorDeleteBtn.setVisible(true);
            } 
            reset();
        }
    }
}
