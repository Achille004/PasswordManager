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

package password.manager.app.controllers.main;

import static password.manager.app.Utils.*;
import static password.manager.lib.Utils.*;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import password.manager.app.controllers.AbstractController;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.Account;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.ReadablePasswordFieldWithStr;

public class ManagerController extends AbstractController {
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

    // Auto-completion data sources
    private List<String> possibleSoftwares, possibleUsernames;
    private final SimpleIntegerProperty suggestionsUpdateTrigger = new SimpleIntegerProperty(0);

    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

        final IOManager IO_MANAGER = IOManager.getInstance();

        // Wrapper scheme: ((( source_list ) sorted_wrapper ) filtered_wrapper )
        final ObservableList<Account> ACCOUNT_LIST = IO_MANAGER.getAccountList();
        final SortedList<Account> SORTED_ACCOUNT_LIST = new SortedList<>(ACCOUNT_LIST);
        final FilteredList<Account> FILTERED_ACCOUNT_LIST = new FilteredList<>(SORTED_ACCOUNT_LIST);

        final ObjectProperty<SortingOrder> SORTING_ORDER_PROPERTY = IO_MANAGER.getUserPreferences().sortingOrderProperty();
        final TabManager TAB_MANAGER = new TabManager(accountTabPane, homeTab);

        setupAutoCompletion(ACCOUNT_LIST);
        setupSearchFunctionality(FILTERED_ACCOUNT_LIST);
        setupAccountListView(SORTING_ORDER_PROPERTY, SORTED_ACCOUNT_LIST, FILTERED_ACCOUNT_LIST, TAB_MANAGER);
        setupKeyboardShortcuts(TAB_MANAGER);
        setupSpecialTabs(TAB_MANAGER);
    }

    @Override
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

    private void setupAutoCompletion(ObservableList<Account> accountList) {
        // Listen for changes in the account list and update suggestions
        ListChangeListener<Account> ACCOUNT_LIST_CHANGE_HANDLER = _ -> {
            possibleSoftwares = accountList.stream()
                    .map(Account::getSoftware)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Entry.<String, Long>comparingByValue().reversed())
                    .map(Entry::getKey)
                    .toList();

            possibleUsernames = accountList.stream()
                    .map(Account::getUsername)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Entry.<String, Long>comparingByValue().reversed())
                    .map(Entry::getKey)
                    .toList();

            // Trigger update of auto-completion suggestions by just incrementing the property
            suggestionsUpdateTrigger.set(suggestionsUpdateTrigger.get() + 1);
        };
        accountList.addListener(ACCOUNT_LIST_CHANGE_HANDLER);

        // Initial population
        ACCOUNT_LIST_CHANGE_HANDLER.onChanged(null);
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

        searchField.textProperty().addListener((_, _, _) -> searchTimeline.playFromStart());
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
                        Platform.runLater(() -> setText((empty || account == null) ? null : order.convert(account)));
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

    private void setupKeyboardShortcuts(TabManager tabManager) {
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
    }

    static class HomeController extends AbstractController {
        @FXML
        private Label homeDescTop, homeDescBtm;

        public void initialize(URL location, ResourceBundle resources) {
            Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
            langResources.bindTextProperty(homeDescTop, "home_desc.top");
            langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
        }

        @Override
        public void reset() {
            // Just focus the top label to keep consistent behavior
            Platform.runLater(() -> homeDescTop.requestFocus());
        }
    }

    private class EditorController extends AbstractController {

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

        private Timeline editorSaveTimeline;
        private AutoCompletionBinding<String> softwareAutoCompletion;
        private AutoCompletionBinding<String> usernameAutoCompletion;

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

            editorSoftware.setOnAction(_ -> Platform.runLater(() -> editorUsername.requestFocus()));
            editorUsername.setOnAction(_ -> Platform.runLater(() -> editorPassword.requestFocus()));
            editorPassword.setOnAction(this::editorSave);

            editorSaveTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> editorSaveBtn.setStyle("-fx-background-color: -fx-color-green")),
                new KeyFrame(Duration.seconds(1), _ -> clearStyle(editorSaveBtn))
            );
            editorSaveTimeline.setCycleCount(1);

            // Force the correct size to prevent unwanted stretching
            editorPassword.setPrefSize(548.0, 40.0);

            // Setup auto-completion for software and username fields
            softwareAutoCompletion = TextFields.bindAutoCompletion(editorSoftware, getSuggetstionProvider(possibleSoftwares));
            usernameAutoCompletion = TextFields.bindAutoCompletion(editorUsername, getSuggetstionProvider(possibleUsernames));

            // Update auto-completion when suggestions change
            suggestionsUpdateTrigger.addListener((_, _, _) -> Platform.runLater(() -> {
                if (softwareAutoCompletion != null) {
                    softwareAutoCompletion.dispose();
                    softwareAutoCompletion = TextFields.bindAutoCompletion(editorSoftware, getSuggetstionProvider(possibleSoftwares));
                }
                if (usernameAutoCompletion != null) {
                    usernameAutoCompletion.dispose();
                    usernameAutoCompletion = TextFields.bindAutoCompletion(editorUsername, getSuggetstionProvider(possibleUsernames));
                }
            }));

            // Disable the delete button if this is the add editor
            editorDeleteBtn.setVisible(!isAddEditor);
        }

        @Override
        public void reset() {
            if(isAddEditor) {
                clearTextFields(editorSoftware, editorUsername, editorPassword.getTextField());
                editorPassword.setReadable(false);
            } else {
                editorSoftware.setText(account.getSoftware());
                editorUsername.setText(account.getUsername());

                IOManager.getInstance().getAccountPassword(editorPassword, account)
                        .exceptionally(e -> {
                            Logger.getInstance().addError(e);
                            return null;
                        });
            }

            ObservableResourceFactory.getInstance().bindPromptTextProperty(editorSoftware, editorUsername, editorPassword);

            editorDeleteCounter = false;
            clearStyle(editorSoftware, editorUsername, editorPassword.getTextField(), editorDeleteBtn);
            Platform.runLater(() -> editorSoftware.requestFocus());
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

        private Callback<ISuggestionRequest, Collection<String>> getSuggetstionProvider(List<String> sourceList) {
            return request -> {
                String userText = request.getUserText();
                if (userText == null || userText.isEmpty()) return List.of();

                String lowerUserText = userText.toLowerCase();
                return sourceList.stream()
                        .filter(s -> s.toLowerCase().startsWith(lowerUserText))
                        .toList();
            };
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

            final ChangeListener<Tab> TAB_FOCUS_HANDLER = (_, _, newTab) -> {
                if (newTab != null) TabManager.getController(newTab).reset();
            };
            TAB_PANE.getSelectionModel().selectedItemProperty().addListener(TAB_FOCUS_HANDLER);

            // It's better to just handle this manually
            final ListChangeListener<Tab> HOME_TAB_HANDLER = change -> {
                while(change.next()) {
                    if (change.wasAdded() && !change.getAddedSubList().contains(homeTab)) {
                        Platform.runLater(() -> TAB_PANE_CONTENT.remove(homeTab));
                    } else if (change.wasRemoved() && TAB_PANE_CONTENT.size() <= 1) {
                        TAB_PANE_CONTENT.addFirst(homeTab);
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
            tab.textProperty().bind(account.softwareProperty());

            return tab;
        }

        public void selectTab(@NotNull Tab tab, boolean addIfMissing) {
            Platform.runLater(() -> {
                if (addIfMissing && !TAB_PANE.getTabs().contains(tab)) {
                    TAB_PANE.getTabs().add(TAB_PANE.getTabs().size() - 1, tab);
                }
                TAB_PANE.getSelectionModel().select(tab);
            });
        }

        public void selectTab(@NotNull Tab tab) {
            selectTab(tab, false);
        }

        public void selectAdjacentTab(int direction) {
            int currentIndex = TAB_PANE.getSelectionModel().getSelectedIndex();
            int newIndex = intSquash(0, currentIndex + direction, TAB_PANE.getTabs().size() - 1);
            TAB_PANE.getSelectionModel().select(newIndex);
        }

        public void closeTab(@NotNull Tab tab) {
            Platform.runLater(() -> TAB_PANE.getTabs().remove(tab));
        }

        public void closeAccountTab(@NotNull Account account) {
            Tab tab = TABS_MAP.remove(account);
            if (tab != null) closeTab(tab);
        }

        // Static utility methods for tab management

        public static <T extends AbstractController> void loadTab(@NotNull Tab tab, @NotNull  String fxmlPath, @NotNull T controller) {
            Pane pane = (Pane) loadFxml(fxmlPath, controller);
            tab.setContent(pane);
            tab.getProperties().put("controller", controller);
        }

        public static AbstractController getController(@NotNull Tab tab) {
            return (AbstractController) tab.getProperties().get("controller");
        }
    }
}