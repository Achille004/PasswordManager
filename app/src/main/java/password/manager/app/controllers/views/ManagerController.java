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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

        final IOManager IO_MANAGER = IOManager.getInstance();

        final SortedList<Account> SORTED_ACCOUNT_LIST = IO_MANAGER.getSortedAccountList();
        final FilteredList<Account> FILTERED_ACCOUNT_LIST = new FilteredList<>(SORTED_ACCOUNT_LIST);

        final ObjectProperty<SortingOrder> SORTING_ORDER_PROPERTY = IO_MANAGER.getUserPreferences().getSortingOrderProperty();
        final TabManager TAB_MANAGER = new TabManager(accountTabPane, homeTab);

        setupAutoCompletion(FILTERED_ACCOUNT_LIST);
        setupSearchFunctionality(FILTERED_ACCOUNT_LIST);
        setupAccountListView(SORTING_ORDER_PROPERTY, SORTED_ACCOUNT_LIST, FILTERED_ACCOUNT_LIST, TAB_MANAGER);
        setupKeyboardShortcuts(FILTERED_ACCOUNT_LIST, TAB_MANAGER);
        setupSpecialTabs(TAB_MANAGER);
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

    private void setupAutoCompletion(FilteredList<Account> filteredAccountList) {
        possibleSoftwares = filteredAccountList.stream()
                .map(Account::getSoftware)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed())
                .map(Entry::getKey)
                .toList();

        possibleUsernames = filteredAccountList.stream()
                .map(Account::getUsername)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Entry.<String, Long>comparingByValue().reversed())
                .map(Entry::getKey)
                .toList();
    }

    private void setupSearchFunctionality(FilteredList<Account> filteredAccountList) {
        searchTimeline = new Timeline(new KeyFrame(SEARCH_DELAY, _ -> {
            final String searchText = searchField.getText().strip();
            if (searchText.isEmpty()) {
                filteredAccountList.setPredicate(null); // Show all accounts
                return;
            }

            final String finalSearchText = isMatchCase ? searchText : searchText.toLowerCase();
            filteredAccountList.setPredicate(account -> {
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
    }

    private void setupAccountListView(ObjectProperty<SortingOrder> sortingOrderProperty, SortedList<Account> sortedAccountList, 
                                      FilteredList<Account> filteredAccountList, TabManager tabManager) {
        // #region Sorted Account List setup
        final ListChangeListener<Account> ACCOUNT_LIST_CHANGE_HANDLER = change -> {
            if (editOperationInProgress) return;

            while(change.next()) {
                if (change.wasRemoved() && !change.wasAdded()) {
                    // This is a true removal
                    change.getRemoved().forEach(tabManager::closeAccountTab);
                }
            }
        };

        sortedAccountList.comparatorProperty().bind(sortingOrderProperty.map(order -> order != null ? order.getComparator() : null));
        sortedAccountList.addListener(ACCOUNT_LIST_CHANGE_HANDLER);
        // #endregion

        // #region Account ListView setup
        final Function<SortingOrder, Callback<ListView<Account>, ListCell<Account>>> ACCOUNT_CELL_FACTORY = order -> 
                _ -> new ListCell<>() {
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

        final ChangeListener<Account> LIST_VIEW_HANDLER = (_, _, newItem) -> {
            if (newItem != null && !editOperationInProgress) {
                tabManager.openAccountTab(newItem);
                Platform.runLater(accountListView.getSelectionModel()::clearSelection);
            }
        };

        accountListView.setItems(filteredAccountList);
        accountListView.cellFactoryProperty().bind(sortingOrderProperty.map(ACCOUNT_CELL_FACTORY));
        accountListView.getSelectionModel().selectedItemProperty().addListener(LIST_VIEW_HANDLER);
        // #endregion
    }

    private void setupKeyboardShortcuts(FilteredList<Account> filteredAccountList, TabManager tabManager) {
        final EventHandler<KeyEvent> SHORTCUTS_HANDLER = keyEvent -> {
            final Tab selectedTab = accountTabPane.getSelectionModel().getSelectedItem();
            if (!keyEvent.isControlDown() || selectedTab == null) return;

            switch(keyEvent.getCode()) {
                case W -> { 
                    keyEvent.consume();
                    if (selectedTab == homeTab) break; 
                    // It's better to just handle this manually
                    if (selectedTab == addTab) {
                        tabManager.selectTab(homeTab);
                    } else {
                        tabManager.closeTab(selectedTab);
                    }
                }

                case T -> {
                    keyEvent.consume();
                    if (selectedTab == addTab) break;
                    tabManager.selectTab(addTab);
                }

                case Q, E -> {
                    keyEvent.consume();
                    tabManager.selectAdjacentTab(keyEvent.getCode() == KeyCode.Q ? -1 : 1);
                }

                default -> {}
            }
        };

        accountTabPane.setOnKeyPressed(SHORTCUTS_HANDLER);
    }

    private void setupSpecialTabs(TabManager tabManager) {
        TabManager.loadTab(homeTab, "/fxml/views/manager/home.fxml", new HomeController());
        TabManager.loadTab(addTab, "/fxml/views/manager/editor.fxml", new EditorController(null));
        addTab.setOnSelectionChanged(_ -> { 
            if (addTab.isSelected()) TabManager.getController(addTab).reset(); 
        });
    }

    static class HomeController extends AbstractViewController {
        @FXML
        private Label homeDescTop, homeDescBtm;

        public void initialize(URL location, ResourceBundle resources) {
            Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
            langResources.bindTextProperty(homeDescTop, "home_desc.top");
            langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
        }

        public void reset() {
            homeDescTop.sceneProperty().addListener((_, _, newScene) -> {
                if (newScene != null) homeDescTop.requestFocus();
            });
        }
    }

    private class EditorController extends AbstractViewController {
        private static final Duration LOAD_ANIM_TIME_UNIT = Duration.millis(125);

        @FXML
        private TextField editorSoftware, editorUsername;
        @FXML
        private ReadablePasswordFieldWithStr editorPassword;

        @FXML
        private Button editorSaveBtn;

        @FXML
        private Button editorDeleteBtn;
        private boolean editorDeleteCounter = false;

        @FXML
        private Label editorAccSelLbl, editorSoftwareLbl, editorUsernameLbl, editorPasswordLbl;

        private final @Getter Account account;
        private final @Getter boolean isAddEditor;

        private Timeline editorSaveTimeline, passLoadTimeline;

        public EditorController(Account account) {
            this.account = account;
            this.isAddEditor = account == null;
        }

        public void initialize(URL location, ResourceBundle resources) {
            Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
            langResources.bindTextProperty(editorSoftwareLbl, "software");
            langResources.bindTextProperty(editorUsernameLbl, "username");
            langResources.bindTextProperty(editorPasswordLbl, "password");

            editorSoftware.setOnAction(_ -> editorUsername.requestFocus());
            editorUsername.setOnAction(_ -> editorPassword.requestFocus());
            editorPassword.setOnAction(this::editorSave);

            editorSoftware.sceneProperty().addListener((_, _, newScene) -> {
                if (newScene != null) {
                    editorSoftware.requestFocus();
                }
            });

            editorSaveTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> editorSaveBtn.setStyle("-fx-background-color: -fx-color-green")),
                new KeyFrame(Duration.seconds(1), _ -> clearStyle(editorSaveBtn))
            );
            editorSaveTimeline.setCycleCount(1);

            passLoadTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> editorPassword.setText("Loading")),
                new KeyFrame(LOAD_ANIM_TIME_UNIT, _ -> editorPassword.setText("Loading.")),
                new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(2), _ -> editorPassword.setText("Loading..")),
                new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(3), _ -> editorPassword.setText("Loading...")),
                new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(4), _ -> editorPassword.setText("Loading"))
            );
            passLoadTimeline.setCycleCount(Timeline.INDEFINITE);

            // Force the correct size to prevent unwanted stretching
            editorPassword.setPrefSize(548.0, 40.0);

            TextFields.bindAutoCompletion(editorSoftware, possibleSoftwares);    
            TextFields.bindAutoCompletion(editorUsername, possibleUsernames);

            editorDeleteBtn.setVisible(!isAddEditor);
        }

        public void reset() {
            if(isAddEditor) {
                clearTextFields(editorSoftware, editorUsername, editorPassword.getTextField());
                editorPassword.setReadable(false);
            } else {
                editorSoftware.setText(account.getSoftware());
                editorUsername.setText(account.getUsername());

                final ReadablePasswordFieldWithStr editorPasswordClone = this.editorPassword;
                editorPasswordClone.setDisable(true);
                editorPasswordClone.setReadable(true);
                passLoadTimeline.playFromStart();

                IOManager.getInstance().getAccountPassword(editorPasswordClone, account)
                        .thenRun(() -> {
                            passLoadTimeline.stop();
                            editorPasswordClone.setDisable(false);
                            editorPasswordClone.setReadable(false);
                        })
                        .exceptionally(e -> {
                            Logger.getInstance().addError(e);
                            return null;
                        });
            }

            editorDeleteCounter = false;
            clearStyle(editorSoftware, editorUsername, editorPassword.getTextField(), editorDeleteBtn);
            editorSoftware.requestFocus();
        }

        @FXML
        public void editorSave(ActionEvent event) {
            if (checkTextFields(editorSoftware, editorUsername, editorPassword.getTextField())) {
                editorSaveTimeline.playFromStart();

                // get the new software, username and password
                final String software = editorSoftware.getText().strip();
                final String username = editorUsername.getText().strip();
                final String password = editorPassword.getText().strip();

                // save the new attributes of the account
                /*  
                  I know that the different reset handling is weird, but let me explain:
                    if the account is null, it means that the user is creating a new account,
                    so we just reset the editor, but if the account is not null, it means that
                    the user is editing an existing account, so we need to edit the account
                    and then reset the editor only if the edit was successful
                  This ensures maximum responsiveness when adding, while avoiding really weird
                  behavior when editing.
                */
                if(isAddEditor) {
                    IOManager.getInstance().addAccount(software, username, password);
                    reset();
                } else {
                    editOperationInProgress = true;
                    IOManager.getInstance().editAccount(account, software, username, password)
                            .thenAccept(_ -> editOperationInProgress = false)
                            .exceptionally(e -> {
                                Logger.getInstance().addError(e);
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
                IOManager.getInstance().removeAccount(account)
                        .exceptionally(e -> {
                            Logger.getInstance().addError(e);
                            return null;
                        });
            } else {
                editorDeleteBtn.setStyle("-fx-background-color: -fx-color-red");
                editorDeleteCounter = true;
            }
        }
    }

    // Helper class for better encapsulation
    private class TabManager {
        // Map for tabs associated with accounts for two-way association and caching
        private static final IdentityHashMap<Account, Tab> TABS_MAP = new IdentityHashMap<>();

        private final TabPane TAB_PANE;
        private final ObservableList<Tab> TAB_PANE_CONTENT; // Convenience reference for better readability

        public TabManager(@NotNull TabPane tabPane, @NotNull Tab homeTab) {
            this.TAB_PANE = tabPane;
            this.TAB_PANE_CONTENT = tabPane.getTabs();

            // It's better to just handle this manually
            final ListChangeListener<Tab> HOME_TAB_HANDLER = change -> {
                while(change.next()) {
                    if (change.wasAdded() && !change.getAddedSubList().contains(homeTab)) {
                        Platform.runLater(() -> TAB_PANE_CONTENT.remove(homeTab));
                    } else if (change.wasRemoved() && TAB_PANE_CONTENT.size() <= 1) {
                        TAB_PANE_CONTENT.add(0, homeTab);
                        selectTab(homeTab);
                    }
                }
            };
            this.TAB_PANE_CONTENT.addListener(HOME_TAB_HANDLER);
        }

        public void openAccountTab(@NotNull Account account) {
            Tab tab = TABS_MAP.computeIfAbsent(account, this::createAccountTab);
            selectTab(tab, true);
        }

        public Tab createAccountTab(@NotNull Account account) {
            EditorController controller = new EditorController(account);
            Tab tab = new Tab();

            TabManager.loadTab(tab, "/fxml/views/manager/editor.fxml", controller);
            tab.textProperty().bind(account.getSoftwareProperty());
            tab.setOnSelectionChanged(_ -> {
                if (tab.isSelected()) controller.reset();
            });

            return tab;
        }

        public void selectTab(@NotNull Tab tab, boolean addIfMissing) {
            Platform.runLater(() -> {
                if (addIfMissing && !TAB_PANE.getTabs().contains(tab)) {
                    TAB_PANE.getTabs().add(TAB_PANE.getTabs().size() - 1, tab);
                }
                TAB_PANE.getSelectionModel().select(tab);
                tab.getContent().requestFocus();
            });
        }

        public void selectTab(@NotNull Tab tab) {
            selectTab(tab, false);
        }

        public void selectAdjacentTab(int direction) {
            int currentIndex = TAB_PANE.getSelectionModel().getSelectedIndex();
            int newIndex = Math.max(0, Math.min(TAB_PANE.getTabs().size() - 1, currentIndex + direction));
            TAB_PANE.getSelectionModel().select(newIndex);
        }

        public void closeTab(@NotNull Tab tab) {
            Platform.runLater(() -> {
                TAB_PANE.getTabs().remove(tab);
                TAB_PANE.getSelectionModel().getSelectedItem().getContent().requestFocus();
            });
        }

        public void closeAccountTab(@NotNull Account account) {
            Tab tab = TABS_MAP.remove(account);
            if (tab != null) closeTab(tab);
        }

        // Static utility methods for tab management

        public static <T extends AbstractViewController> void loadTab(@NotNull Tab tab, @NotNull  String fxmlPath, @NotNull T controller) {
            Pane pane = (Pane) loadFxml(fxmlPath, controller);
            tab.setContent(pane);
            tab.getProperties().put("controller", controller);
        }

        public static AbstractViewController getController(@NotNull Tab tab) {
            return (AbstractViewController) tab.getProperties().get("controller");
        }
    }
}