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

import static password.manager.app.Utils.*;

import java.net.URL;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.Account;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class ManagerController extends AbstractViewController {
    public static final Duration SEARCH_DELAY = Duration.millis(300);

    // Cache for tabs associated with accounts
    private final IdentityHashMap<Account, Tab> TABS_CACHE = new IdentityHashMap<>();

    @FXML
    private ListView<Account> accountListView;

    @FXML
    private TabPane accountTabPane;
    
    @FXML
    private Tab addTab, homeTab;

    @FXML
    private TextField searchField;

    @FXML
    private Button matchCaseButton, matchWholeWordButton;

    private Timeline searchTimeline;

    // App state variables
    private volatile boolean editOperationInProgress = false;
    private volatile boolean isMatchCase = false, isMatchWholeWord = false;
    private List<String> possibleSoftwares, possibleUsernames;

    public void initialize(URL location, ResourceBundle resources) {
        final ObservableList<Tab> ACC_TABS = accountTabPane.getTabs();

        final ChangeListener<Account> LIST_VIEW_HANDLER = (_, _, newItem) -> {
            if (newItem != null && !editOperationInProgress) {
                // If the tab is cached, reuse it
                final Tab cachedTab = TABS_CACHE.get(newItem);
                if (cachedTab != null) {
                    selectTab(ACC_TABS, cachedTab, true);
                    return;
                }

                // Create a new tab for the selected account -> TODO store as singleton and deep clone
                final EditorController controller = new EditorController();
                final Pane pane = (Pane) loadFxml("/fxml/views/manager/editor.fxml", controller);
                
                controller.setAccount(newItem);

                final Tab tab = new Tab();
                tab.textProperty().bind(Bindings.createStringBinding(() -> {
                    final String software = newItem.getSoftware();
                    return software != null ? software : "";
                }, newItem.getSoftwareProperty()));
                tab.setContent(pane);
                tab.setOnSelectionChanged(_ -> {
                    if (tab.isSelected()) {
                        controller.reset();
                    }
                });

                selectTab(ACC_TABS, tab, true);
                TABS_CACHE.put(newItem, tab);
            }
        };

        final ListChangeListener<Account> ACCOUNT_LIST_CHANGE_HANDLER = change -> {
            if (editOperationInProgress) {
                return;
            }

            while(change.next()) {
                if (change.wasRemoved() && !change.wasAdded()) {
                    // This is a true removal
                    for (Account removedAccount : change.getRemoved()) {
                        final Tab tab = TABS_CACHE.remove(removedAccount);
                        if (tab != null) {
                            Platform.runLater(() -> ACC_TABS.remove(tab));
                        }
                    }
                }
            }
        };

        final ListChangeListener<Tab> ACC_TABS_CHANGE_HANDLER = change -> {
            while(change.next()) {
                if (change.wasAdded() && !change.getAddedSubList().contains(homeTab)) {
                    Platform.runLater(() -> ACC_TABS.remove(homeTab));
                } else if (change.wasRemoved() && ACC_TABS.size() <= 1) {
                    selectTab(ACC_TABS, homeTab, true);
                }
            }
        };

        final EventHandler<KeyEvent> SHORTCUTS_HANDLER = keyEvent -> {
            final Tab selectedTab = accountTabPane.getSelectionModel().getSelectedItem();
            if (!keyEvent.isControlDown() || selectedTab == null) {
                return;
            }

            switch(keyEvent.getCode()) {
                case W -> { 
                    keyEvent.consume();
                    if(selectedTab == homeTab) break;
                    if(selectedTab == addTab) {
                        selectTab(ACC_TABS, homeTab, false);
                    } else {
                        ACC_TABS.remove(selectedTab);
                    }
                }

                case T -> {
                    keyEvent.consume();
                    if (selectedTab == addTab) break;
                    selectTab(ACC_TABS, addTab, false);
                }

                case Q, E -> {
                    keyEvent.consume();
                    final int currentIndex = accountTabPane.getSelectionModel().getSelectedIndex();
                    if (currentIndex == -1) break;

                    final int newIndex = (keyEvent.getCode() == KeyCode.Q) ? currentIndex - 1 : currentIndex + 1;
                    if (newIndex >= 0 && newIndex < ACC_TABS.size()) {
                        accountTabPane.getSelectionModel().select(newIndex);
                    }
                }

                default -> {}
            }
        };

        final IOManager IO_MANAGER = IOManager.getInstance();
        final ObjectProperty<SortingOrder> SORTING_ORDER_PROPERTY = IO_MANAGER.getUserPreferences().getSortingOrderProperty();
        final SortedList<Account> SORTED_ACCOUNT_LIST = IO_MANAGER.getSortedAccountList();
        final FilteredList<Account> FILTERED_ACCOUNT_LIST = new FilteredList<>(SORTED_ACCOUNT_LIST);

        accountListView.setItems(FILTERED_ACCOUNT_LIST);
        // Set cell factory to control how Account objects are displayed
        accountListView.cellFactoryProperty().bind(SORTING_ORDER_PROPERTY.map(this::accountCellFactory));
        accountListView.getSelectionModel().selectedItemProperty().addListener(LIST_VIEW_HANDLER);
        
        SORTED_ACCOUNT_LIST.comparatorProperty().bind(Bindings.createObjectBinding(() -> {
            final SortingOrder sortingOrder = SORTING_ORDER_PROPERTY.getValue();
            return sortingOrder != null ? sortingOrder.getComparator() : null;
        }, SORTING_ORDER_PROPERTY));

        SORTED_ACCOUNT_LIST.addListener(ACCOUNT_LIST_CHANGE_HANDLER);
        ACC_TABS.addListener(ACC_TABS_CHANGE_HANDLER);

        accountTabPane.setOnKeyPressed(SHORTCUTS_HANDLER);

        // Handle search bar events
        searchTimeline = new Timeline(new KeyFrame(SEARCH_DELAY, _ -> {
            final String searchText = searchField.getText().trim();
            if (searchText.isEmpty()) {
                FILTERED_ACCOUNT_LIST.setPredicate(null); // Show all accounts
                return;
            }

            final String finalSearchText = isMatchCase ? searchText : searchText.toLowerCase();
            FILTERED_ACCOUNT_LIST.setPredicate(account -> {
                final String software = isMatchCase ? account.getSoftware() : account.getSoftware().toLowerCase();
                final String username = isMatchCase ? account.getUsername() : account.getUsername().toLowerCase();
                
                if (isMatchWholeWord) {
                    return Arrays.asList(software.split("[\\s\\p{P}]+")).contains(finalSearchText) || Arrays.asList(username.split("[\\s\\p{Punct}]+")).contains(finalSearchText);
                } else {
                    return software.contains(finalSearchText) || username.contains(finalSearchText);
                }
            });
        }));
        searchTimeline.setCycleCount(1);

        searchField.textProperty().addListener((_, _, _) -> {
            searchTimeline.stop();
            searchTimeline.playFromStart();
        });

        searchField.setOnAction(_ -> {
            searchTimeline.stop();
            searchTimeline.playFrom(SEARCH_DELAY);
        });

        possibleSoftwares = FILTERED_ACCOUNT_LIST.stream()
                .map(Account::getSoftware)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed())
                .map(Entry::getKey)
                .toList();

        possibleUsernames = FILTERED_ACCOUNT_LIST.stream()
                .map(Account::getUsername)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed())
                .map(Entry::getKey)
                .toList();

        loadHomeTab();
        loadAddTab();
    }

    public void reset() {}

    @FXML
    public void matchCaseAction(ActionEvent event) {
        if (isMatchCase) {
            isMatchCase = false;
            clearStyle(matchCaseButton);
        } else {
            isMatchCase = true;
            matchCaseButton.setStyle("-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;");
        }
        searchTimeline.playFrom(SEARCH_DELAY);
    }

    @FXML
    public void matchWholeWordAction(ActionEvent event) {
        if (isMatchWholeWord) {
            isMatchWholeWord = false;
            clearStyle(matchWholeWordButton);
        } else {
            isMatchWholeWord = true;
            matchWholeWordButton.setStyle("-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;");
        }
        searchTimeline.playFrom(SEARCH_DELAY);
    }

    private void loadHomeTab() {
        final HomeController homeController = new HomeController();
        final Pane homePane = (Pane) loadFxml("/fxml/views/manager/home.fxml", homeController);
        homeTab.setContent(homePane);
    }

    private void loadAddTab() {
        final EditorController addController = new EditorController();
        final Pane addPane = (Pane) loadFxml("/fxml/views/manager/editor.fxml", addController);

        addTab.setContent(addPane);
        addTab.setOnSelectionChanged(_ -> {
            if (addTab.isSelected()) {
                accountListView.getSelectionModel().clearSelection();
                addController.reset();
            }
        });
    }

    private void selectTab(ObservableList<Tab> ACC_TABS, Tab tab, boolean add) {
        if (tab == null) {
            return;
        }

        Platform.runLater(() -> {
            if(add && !accountTabPane.getTabs().contains(tab)) {
                accountTabPane.getTabs().add(accountTabPane.getTabs().size() - 1, tab);
            }
            accountTabPane.getSelectionModel().select(tab);
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

    static class HomeController extends AbstractViewController {
        @FXML
        private Label homeDescTop, homeDescBtm;

        public void initialize(URL location, ResourceBundle resources) {
            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
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

    private class EditorController extends AbstractViewController {
        private @Getter Account account;

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
            final ObjectProperty<SortingOrder> sortingOrderProperty = IOManager.getInstance().getUserPreferences().getSortingOrderProperty();

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
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

            TextFields.bindAutoCompletion(editorSoftware, possibleSoftwares);    
            TextFields.bindAutoCompletion(editorUsername, possibleUsernames);
        }

        public void reset() {
            if(account != null) {
                editorSoftware.setText(account.getSoftware());
                editorUsername.setText(account.getUsername());
                IOManager.getInstance().getAccountPassword(editorPassword, account);
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
                final String software = editorSoftware.getText();
                final String username = editorUsername.getText();
                final String password = editorPassword.getText();
                
                // save the new attributes of the account
                /*  
                  I know that the different reset handling is weird, but let me explain:
                    if the account is null, it means that the user is creating a new account,
                    so we just reset the editor, but if the account is not null, it means that
                    the user is editing an existing account, so we need to edit the account
                    and then reset the editor only if the edit was successful
                  This ensures maximum responsiveness when adding, while avoiding really weird behavior
                  when editing, like the editor creating duplicated tabs for the same account.
                */
                final boolean isNewAccount = account == null; // <- dummy variable to make the code more readable 
                if(isNewAccount) {
                    IOManager.getInstance().addAccount(software, username, password);
                    reset();
                } else {
                    editOperationInProgress = true;
                    IOManager.getInstance().editAccount(account, software, username, password)
                            .thenAccept(result -> Platform.runLater(() -> {
                                if (result) {
                                    final MultipleSelectionModel<Account> accountSelectionModel = accountListView.getSelectionModel();
                                    accountSelectionModel.clearSelection();
                                    accountSelectionModel.select(account);
                                } else {
                                    Logger.getInstance().addError(new RuntimeException("Failed to edit account."));
                                }
                                editOperationInProgress = false;
                            }))
                            .exceptionally(ex -> {
                                Logger.getInstance().addError(ex);
                                Platform.runLater(() -> editOperationInProgress = false);
                                return null;
                            });
                }
            }
        }

        @FXML
        public void editorDelete(ActionEvent event) {
            // when the deleteCounter is true it means that the user has confirmed the elimination
            if (editorDeleteCounter) {
                // clear the selection in order to avoid the default behavior of the list view (opening another account)
                accountListView.getSelectionModel().clearSelection();
                // removes the selected account from the list
                IOManager.getInstance().removeAccount(account);
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