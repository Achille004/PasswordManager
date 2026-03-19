/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;
import org.controlsfx.control.textfield.TextFields;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.Getter;
import password.manager.app.base.SortingOrder;
import password.manager.app.controllers.AbstractController;
import password.manager.app.controllers.TabManager;
import password.manager.app.security.Account;
import password.manager.app.security.Account.AccountData;
import password.manager.app.singletons.IOManager;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import password.manager.lib.LoadingAnimation;
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
    private final AtomicInteger editOperationsCounter = new AtomicInteger(0);
    private volatile boolean isMatchCase = false, isMatchWholeWord = false;

    // Auto-completion data sources
    private List<String> possibleSoftwares, possibleUsernames;
    private final SimpleIntegerProperty suggestionsUpdateTrigger = new SimpleIntegerProperty(0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing %s", getClass().getSimpleName());

        final IOManager IO_MANAGER = IOManager.getInstance();

        // Wrapper scheme: ((( source_list ) sorted_wrapper ) filtered_wrapper )
        final ObservableList<Account> ACCOUNT_LIST = IO_MANAGER.getAccountList();
        final SortedList<Account> SORTED_ACCOUNT_LIST = new SortedList<>(ACCOUNT_LIST);
        final FilteredList<Account> FILTERED_ACCOUNT_LIST = new FilteredList<>(SORTED_ACCOUNT_LIST);

        final ObjectProperty<SortingOrder> SORTING_ORDER_PROPERTY = IO_MANAGER.getUserPreferences().sortingOrderProperty();
        final TabManager<Account, EditorController> TAB_MANAGER = new TabManager<>(
            accountTabPane, EditorController::new,
            (tab, account) -> tab.textProperty().bind(account.softwareProperty())
        );

        setupAutoCompletion(ACCOUNT_LIST);
        setupSearchFunctionality(FILTERED_ACCOUNT_LIST);
        setupAccountListView(SORTING_ORDER_PROPERTY, SORTED_ACCOUNT_LIST, FILTERED_ACCOUNT_LIST, TAB_MANAGER);
        setupKeyboardShortcuts(TAB_MANAGER);
        setupSpecialTabs(accountTabPane, TAB_MANAGER);
    }

    @Override
    public String getFxmlPath() {
        return "/fxml/main/manager.fxml";
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
                                      FilteredList<Account> filteredAccountList, TabManager<Account, EditorController> tabManager) {
        // #region Sorted Account List setup
        final ListChangeListener<Account> ACCOUNT_LIST_CHANGE_HANDLER = change -> {
            if (editOperationsCounter.get() > 0) return;

            while(change.next()) {
                if (change.wasRemoved() && !change.wasAdded()) {
                    // This is a true removal
                    change.getRemoved().forEach(tabManager::removeTab);
                }
            }
        };

        sortedAccountList.comparatorProperty().bind(sortingOrderProperty.map(order -> order != null ? order.getComparator() : null));
        sortedAccountList.addListener(ACCOUNT_LIST_CHANGE_HANDLER);
        // #endregion

        // #region Account ListView setup
        final Callback<ListView<Account>, ListCell<Account>> ACCOUNT_CELL_FACTORY = _ -> new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                textProperty().unbind();
                if (empty || account == null) {
                    setText(null);
                } else {
                    StringBinding textBinding = Bindings.createStringBinding(
                        () -> {
                            SortingOrder order = sortingOrderProperty.get();
                            return order != null ? order.convert(account.getSoftware(), account.getUsername()) : null;
                        },
                        sortingOrderProperty, account.softwareProperty(), account.usernameProperty()
                    );
                    textProperty().bind(textBinding);
                }
            }
        };

        final ChangeListener<Account> LIST_VIEW_HANDLER = (_, _, newItem) -> {
            if (newItem != null && editOperationsCounter.get() == 0) {
                tabManager.openTab(newItem);
                // Defer the task to avoid conflicts on the underlying list of selected accounts
                Platform.runLater(accountListView.getSelectionModel()::clearSelection);
            }
        };

        accountListView.setItems(filteredAccountList);
        accountListView.setCellFactory(ACCOUNT_CELL_FACTORY);
        accountListView.getSelectionModel().selectedItemProperty().addListener(LIST_VIEW_HANDLER);
        // #endregion
    }

    private void setupKeyboardShortcuts(TabManager<Account, EditorController> tabManager) {
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

    private void setupSpecialTabs(TabPane tabPane, TabManager<Account, EditorController> tabManager) {
        TabManager.loadTab(homeTab, new HomeController());
        TabManager.loadTab(addTab, new EditorController(null));

        final ObservableList<Tab> TAB_PANE_CONTENT = tabPane.getTabs();
        // It's better to just handle this manually
        final ListChangeListener<Tab> HOME_TAB_HANDLER = change -> {
            while(change.next()) {
                // Defer tasks to avoid conflicts on the underlying list of tabs
                if (change.wasAdded() && !change.getAddedSubList().contains(homeTab)) {
                    Platform.runLater(() -> TAB_PANE_CONTENT.remove(homeTab));
                } else if (change.wasRemoved() && TAB_PANE_CONTENT.size() <= 1) {
                    Platform.runLater(() -> {
                        TAB_PANE_CONTENT.addFirst(homeTab);
                        tabManager.selectTab(homeTab);
                    });
                }
            }
        };
        TAB_PANE_CONTENT.addListener(HOME_TAB_HANDLER);
    }

    static class HomeController extends AbstractController {
        @FXML
        private Label homeDescTop, homeDescBtm;

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            Logger.getInstance().addDebug("Initializing %s", getClass().getSimpleName());

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
            langResources.bindTextProperty(homeDescTop, "home_desc.top");
            langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
        }

        @Override
        public String getFxmlPath() {
            return "/fxml/main/manager/home.fxml";
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
        private Label editorSoftwareLbl, editorUsernameLbl, editorPasswordLbl;

        private final @Getter Account account;
        private final @Getter boolean isAddEditor;

        private Timeline editorSaveTimeline;
        private AutoCompletionBinding<String> softwareAutoCompletion;
        private AutoCompletionBinding<String> usernameAutoCompletion;

        private volatile boolean isErrBound;

        public EditorController(Account account) {
            this.account = account;
            this.isAddEditor = (account == null);
            this.isErrBound = false;
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            Logger.getInstance().addDebug("Initializing %s", getClass().getSimpleName());

            final ObservableResourceFactory langResources = ObservableResourceFactory.getInstance();
            langResources.bindTextProperty(editorSoftwareLbl, "software");
            langResources.bindTextProperty(editorUsernameLbl, "username");
            langResources.bindTextProperty(editorPasswordLbl, "password");

            editorSoftware.setOnAction(_ -> {
                editorUsername.requestFocus(); // Normally it would select the text, but that's ugly
                editorUsername.end();
            });
            editorUsername.setOnAction(_ -> {
                editorPassword.requestFocus(); // Normally it would select the text, but that's ugly
                editorPassword.end();
            });
            editorPassword.setOnAction(this::editorSave);

            editorSaveTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, _ -> editorSaveBtn.setStyle("-fx-background-color: -fx-color-green")),
                new KeyFrame(Duration.seconds(1), _ -> clearStyle(editorSaveBtn))
            );
            editorSaveTimeline.setCycleCount(1);

            // Setup auto-completion for software and username fields
            softwareAutoCompletion = TextFields.bindAutoCompletion(editorSoftware, getSuggestionProvider(possibleSoftwares));
            usernameAutoCompletion = TextFields.bindAutoCompletion(editorUsername, getSuggestionProvider(possibleUsernames));

            // Update auto-completion when suggestions change
            suggestionsUpdateTrigger.addListener((_, _, _) -> {
                if (this.isErrBound) return;
                unbindAutoCompletion();
                bindAutoCompletion();
            });

            // Disable the delete button if this is the add editor
            editorDeleteBtn.setVisible(!isAddEditor);
        }

        @Override
        public String getFxmlPath() {
            return "/fxml/main/manager/editor.fxml";
        }

        @Override
        public void reset() {
            if(isAddEditor) {
                clearTextFields(editorSoftware, editorUsername, editorPassword);
                editorPassword.setReadable(false);
                Platform.runLater(editorSoftware::requestFocus);
            } else {
                clearErrorLoadState();
                // Do operations HERE, if needed
                LoadingAnimation.start(editorSoftware, editorUsername, editorPassword, editorSaveBtn, editorDeleteBtn);

                IOManager.getInstance().getAccountData(account)
                        .whenComplete((data, ex) -> Platform.runLater(() -> {
                            LoadingAnimation.stop(editorSoftware, editorUsername, editorPassword, editorSaveBtn, editorDeleteBtn);

                            boolean success = (ex == null && data != null);
                            if (!success) {
                                applyErrorLoadState();
                                return;
                            }

                            clearErrorLoadState();
                            editorSoftware.setText(data.software());
                            editorUsername.setText(data.username());
                            editorPassword.setText(data.password());

                            // When everything is ready, focus the software field and set caret to the end of the text
                            // (not setting the caret would result in the text being selected, which is really weird when editing)
                            editorSoftware.requestFocus();
                            editorSoftware.end();
                        }));
            }

            ObservableResourceFactory.getInstance().bindPromptTextProperty(editorSoftware, editorUsername, editorPassword);

            editorDeleteCounter = false;
            clearStyle(editorSoftware, editorUsername, editorPassword, editorDeleteBtn);
        }

        @FXML
        public void editorSave(ActionEvent event) {
            if (checkTextFields(editorSoftware, editorUsername, editorPassword)) {
                editorSaveTimeline.playFromStart();

                // Reset the delete button state
                editorDeleteCounter = false;
                clearStyle(editorDeleteBtn);

                // get the new software, username and password
                final String software = editorSoftware.getText().strip();
                final String username = editorUsername.getText().strip();
                final String password = editorPassword.getText().strip();
                final AccountData data = new AccountData(software, username, password);

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
                    IOManager.getInstance().addAccount(data);
                    reset();
                } else {
                    editOperationsCounter.incrementAndGet();
                    LoadingAnimation.start(editorSoftware, editorUsername, editorPassword, editorSaveBtn, editorDeleteBtn);
                    IOManager.getInstance().editAccount(account, data)
                            .whenComplete((account, ex) -> {
                                LoadingAnimation.stop(editorSoftware, editorUsername, editorPassword, editorSaveBtn, editorDeleteBtn);
                                editOperationsCounter.decrementAndGet();

                                boolean success = (ex == null && account != null);
                                if (!success) {
                                    Platform.runLater(this::reset);
                                    return;
                                }

                                // Re-write the fields since the loading animation leaves "Loading..."
                                editorSoftware.setText(data.software());
                                editorUsername.setText(data.username());
                                editorPassword.setText(data.password());
                            });
                }
            }
        }

        @FXML
        public void editorDelete(ActionEvent event) {
            // when the deleteCounter is true it means that the user has confirmed the elimination
            if (editorDeleteCounter) {
                IOManager.getInstance().removeAccount(account);
            } else {
                editorDeleteBtn.setStyle("-fx-background-color: -fx-color-red");
                editorDeleteCounter = true;
            }
        }

        private Callback<ISuggestionRequest, Collection<String>> getSuggestionProvider(List<String> sourceList) {
            return request -> {
                String userText = request.getUserText();
                if (userText == null || userText.isEmpty()) return List.of();

                String lowerUserText = userText.toLowerCase();
                return sourceList.stream()
                        .filter(s -> s.toLowerCase().startsWith(lowerUserText))
                        .toList();
            };
        }

        private void bindAutoCompletion() {
            if (softwareAutoCompletion == null) {
                softwareAutoCompletion = TextFields.bindAutoCompletion(editorSoftware, getSuggestionProvider(possibleSoftwares));
            }
            if (usernameAutoCompletion == null) {
                usernameAutoCompletion = TextFields.bindAutoCompletion(editorUsername, getSuggestionProvider(possibleUsernames));
            }
        }

        private void clearErrorLoadState() {
            if (!this.isErrBound) return;

            editorSoftware.textProperty().unbind();
            editorSoftware.setDisable(false);
            editorUsername.textProperty().unbind();
            editorUsername.setDisable(false);
            editorPassword.textProperty().unbind();
            editorPassword.setDisable(false);
            editorSaveBtn.setDisable(false);
            editorDeleteBtn.setDisable(false);

            editorPassword.setReadable(false);

            this.isErrBound = false;
            bindAutoCompletion();
        }

        private void applyErrorLoadState() {
            clearErrorLoadState();
            unbindAutoCompletion();
            this.isErrBound = true;

            // Must be done before binding textProperty(): the skin refresh logic calls setText(...)
            // when readability changes, and that would fail on a bound text property.
            editorPassword.setReadable(true);

            ObservableResourceFactory resFact = ObservableResourceFactory.getInstance();
            resFact.bindStringProperty(editorSoftware.textProperty(), "editor.load_error");
            editorSoftware.setDisable(true);
            resFact.bindStringProperty(editorUsername.textProperty(), "editor.load_error");
            editorUsername.setDisable(true);
            resFact.bindStringProperty(editorPassword.textProperty(), "editor.load_error");
            editorPassword.setDisable(true);
            editorSaveBtn.setDisable(true);
            editorDeleteBtn.setDisable(true);
        }

        private void unbindAutoCompletion() {
            if (softwareAutoCompletion != null) {
                softwareAutoCompletion.dispose();
                softwareAutoCompletion = null;
            }
            if (usernameAutoCompletion != null) {
                usernameAutoCompletion.dispose();
                usernameAutoCompletion = null;
            }
        }
    }
}