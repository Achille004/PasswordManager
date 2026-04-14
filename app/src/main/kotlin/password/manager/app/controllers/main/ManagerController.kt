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
package password.manager.app.controllers.main

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback
import javafx.util.Duration
import org.controlsfx.control.textfield.AutoCompletionBinding
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest
import org.controlsfx.control.textfield.TextFields
import password.manager.app.base.SortingOrder
import password.manager.app.base.SupportedLocale
import password.manager.app.controllers.AbstractController
import password.manager.app.controllers.TabManager
import password.manager.app.security.Account
import password.manager.app.security.Account.AccountData
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.LoadingAnimation
import password.manager.lib.ReadablePasswordFieldWithStr
import java.net.URL
import java.util.Map
import java.util.ResourceBundle
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.concurrent.Volatile

class ManagerController : AbstractController() {
    @FXML
    private val accountListView: ListView<Account?>? = null

    @FXML
    private val accountTabPane: TabPane? = null

    @FXML
    private val addTab: Tab? = null

    @FXML
    private val homeTab: Tab? = null

    @FXML
    private val searchField: TextField? = null

    @FXML
    private val matchCaseButton: Button? = null

    @FXML
    private val matchWholeWordButton: Button? = null

    private var searchTimeline: Timeline? = null

    // App state variables
    private val editOperationsCounter = AtomicInteger(0)

    @Volatile
    private var isMatchCase = false

    @Volatile
    private var isMatchWholeWord = false

    // Auto-completion data sources
    private var possibleSoftwares: MutableList<String?>? = null
    private var possibleUsernames: MutableList<String?>? = null
    private val suggestionsUpdateTrigger = SimpleIntegerProperty(0)

    override fun initialize(location: URL, resources: ResourceBundle) {
        Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

        val ioManager = IOManager.getInstance()

        // Wrapper scheme: ((( source_list ) sorted_wrapper ) filtered_wrapper )
        val accountList = ioManager.accountList
        val sortedAccountList = SortedList(accountList)
        val filteredAccountList = FilteredList(sortedAccountList)

        val sortingOrderProperty = ioManager.userPreferences.sortingOrderProperty()
        val tabManager = TabManager(accountTabPane!!, ::EditorController) {
            tab: Tab, account: Account -> tab.textProperty().bind(account.softwareProperty())
        }

        setupAutoCompletion(accountList)
        setupSearchFunctionality(filteredAccountList)
        setupAccountListView(sortingOrderProperty, sortedAccountList, filteredAccountList, tabManager)
        setupKeyboardShortcuts(tabManager)
        setupSpecialTabs(accountTabPane, tabManager)
    }

    override val fxmlPath = "/fxml/main/manager.fxml"

    override fun reset() {}

    @FXML
    fun matchCaseAction(event: ActionEvent?) {
        if (isMatchCase) {
            isMatchCase = false
            clearStyle(matchCaseButton!!)
        } else {
            isMatchCase = true
            matchCaseButton!!.style = "-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;"
        }
        searchTimeline!!.playFrom(SEARCH_DELAY)
    }

    @FXML
    fun matchWholeWordAction(event: ActionEvent?) {
        if (isMatchWholeWord) {
            isMatchWholeWord = false
            clearStyle(matchWholeWordButton!!)
        } else {
            isMatchWholeWord = true
            matchWholeWordButton!!.style = "-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;"
        }
        searchTimeline!!.playFrom(SEARCH_DELAY)
    }

    private fun setupAutoCompletion(accountList: ObservableList<Account>) {
        val fieldListExtractor = { fieldExtractor: Function1<Account, String> ->
            val fieldSortedByUsage = accountList.stream()
                .map { it?.let(fieldExtractor) }
                .collect(Collectors.groupingBy(Function { s: String? -> s }, Collectors.counting()))

            fieldSortedByUsage.entries.stream()
                .sorted(Map.Entry.comparingByValue<String?, Long>().reversed())
                .map { obj: MutableMap.MutableEntry<String?, Long>? -> obj!!.key }
                .toList()
        }

        // Listen for changes in the account list and update suggestions
        val accountListChangeHandler = ListChangeListener<Account> { _ ->
            possibleSoftwares = fieldListExtractor.invoke(Account::getSoftware)
            possibleUsernames = fieldListExtractor.invoke(Account::getSoftware)

            // Trigger update of auto-completion suggestions by just incrementing the property
            suggestionsUpdateTrigger.set(suggestionsUpdateTrigger.get() + 1)
        }
        accountList.addListener(accountListChangeHandler)

        // Initial population
        accountListChangeHandler.onChanged(null)
    }

    private fun setupSearchFunctionality(filteredAccountList: FilteredList<Account>) {
        searchTimeline = Timeline(KeyFrame(SEARCH_DELAY, EventHandler { `_`: ActionEvent? ->
            val searchText = searchField!!.text.trim()
            if (searchText.isEmpty()) {
                filteredAccountList.predicate = null // Show all accounts
                return@EventHandler
            }

            val defaultLocale = SupportedLocale.DEFAULT.locale

            val finalSearchText = if (isMatchCase) searchText else searchText.lowercase(defaultLocale)
            val punctSplitRegex = Regex("[\\s\\p{P}]+")
            val wholeWordPredicate: Predicate<String> = { str: String ->
                str.split(punctSplitRegex).dropLastWhile(String::isEmpty).contains(finalSearchText)
            }

            filteredAccountList.predicate = Predicate { account: Account ->
                val software = if (isMatchCase) account.software else account.software.lowercase(defaultLocale)
                val username = if (isMatchCase) account.username else account.username.lowercase(defaultLocale)
                return@Predicate if (isMatchWholeWord) {
                    wholeWordPredicate.test(software) || wholeWordPredicate.test(username)
                } else {
                    software.contains(finalSearchText) || username.contains(finalSearchText)
                }
            }
        }))
        searchTimeline!!.cycleCount = 1

        searchField!!.textProperty().addListener { _, _, _ ->
            searchTimeline!!.playFromStart()
        }
        searchField.onAction = { _ ->
            searchTimeline!!.stop()
            searchTimeline!!.playFrom(SEARCH_DELAY)
        }
    }

    private fun setupAccountListView(
        sortingOrderProperty: ObjectProperty<SortingOrder>, sortedAccountList: SortedList<Account>,
        filteredAccountList: FilteredList<Account>, tabManager: TabManager<Account, EditorController>
    ) {
        // #region Sorted Account List setup
        val accountListChangeHandler = ListChangeListener { change: ListChangeListener.Change<out Account> ->
            if (editOperationsCounter.get() > 0) return@ListChangeListener
            while (change.next()) {
                if (change.wasRemoved() && !change.wasAdded()) {
                    // This is a true removal
                    change.getRemoved().forEach { tabManager.removeTab(it) }
                }
            }
        }

        sortedAccountList.comparatorProperty().bind(
            sortingOrderProperty.map { order: SortingOrder? -> order?.comparator }
        )
        sortedAccountList.addListener(accountListChangeHandler)

        // #endregion

        // #region Account ListView setup
        val accountCellFactory: Callback<ListView<Account?>, ListCell<Account?>> = { _ ->
            object : ListCell<Account?>() {
                override fun updateItem(account: Account?, empty: Boolean) {
                    super.updateItem(account, empty)
                    textProperty().unbind()

                    if (empty || account == null) {
                        text = null
                        return
                    }

                    val textBinding = Bindings.createStringBinding(
                        { sortingOrderProperty.get()?.convert(account) },
                        sortingOrderProperty, account.softwareProperty(), account.usernameProperty()
                    )
                    textProperty().bind(textBinding)
                }
            }
        }

        val listViewHandler = ChangeListener { _, _, newItem: Account ->
            if (editOperationsCounter.get() > 0) return@ChangeListener

            tabManager.openTab(newItem)
            // Defer the task to avoid conflicts on the underlying list of selected accounts
            Platform.runLater(accountListView!!.getSelectionModel()::clearSelection)
        }

        accountListView!!.items = filteredAccountList
        accountListView.cellFactory = accountCellFactory
        accountListView.getSelectionModel().selectedItemProperty().addListener(listViewHandler)
        // #endregion
    }

    private fun setupKeyboardShortcuts(tabManager: TabManager<Account, EditorController>) {
        val shortcutsHandler = EventHandler { keyEvent: KeyEvent? ->
            val selectedTab = accountTabPane!!.selectionModel.getSelectedItem()
            if (!keyEvent!!.isControlDown || selectedTab == null) return@EventHandler
            when (keyEvent.code) {
                KeyCode.W ->  {
                    keyEvent.consume()
                    when (selectedTab) {
                        homeTab -> {}
                        // It's better to just handle this manually
                        addTab -> tabManager.selectTab(homeTab!!)
                        else -> tabManager.closeTab(selectedTab)
                    }
                }

                KeyCode.T -> {
                    keyEvent.consume()
                    when (selectedTab) {
                        addTab -> {}
                        else -> tabManager.selectTab(addTab!!)
                    }
                }

                KeyCode.Q, KeyCode.E -> {
                    keyEvent.consume()
                    tabManager.selectAdjacentTab(if (keyEvent.code == KeyCode.Q) -1 else 1)
                }

                else -> {}
            }
        }

        accountTabPane!!.onKeyPressed = shortcutsHandler
    }

    private fun setupSpecialTabs(tabPane: TabPane, tabManager: TabManager<Account, EditorController>) {
        TabManager.loadTab(homeTab!!, HomeController())
        TabManager.loadTab(addTab!!, EditorController(null))

        val tabPaneContent = tabPane.tabs
        // It's better to just handle this manually
        val homeTabHandler = ListChangeListener { change: ListChangeListener.Change<out Tab?>? ->
            while (change!!.next()) {
                // Defer tasks to avoid conflicts on the underlying list of tabs
                when {
                    change.wasAdded() && !change.getAddedSubList().contains(homeTab) -> Platform.runLater {
                        tabPaneContent.remove(homeTab)
                    }
                    change.wasRemoved() && tabPaneContent.size <= 1 -> Platform.runLater {
                        tabPaneContent.addFirst(homeTab)
                        tabManager.selectTab(homeTab)
                    }
                }
            }
        }
        tabPaneContent.addListener(homeTabHandler)
    }

    companion object {
        val SEARCH_DELAY: Duration = Duration.millis(300.0)
    }

    internal class HomeController : AbstractController() {
        @FXML
        private val homeDescTop: Label? = null

        @FXML
        private val homeDescBtm: Label? = null

        override fun initialize(location: URL, resources: ResourceBundle) {
            Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

            val langResources = ObservableResourceFactory.getInstance()
            langResources.bindTextProperty(homeDescTop!!, "home_desc.top")
            langResources.bindTextProperty(homeDescBtm!!, "home_desc.btm")
        }

        override val fxmlPath = "/fxml/main/manager/home.fxml"

        override fun reset() {
            // Just focus the top label to keep consistent behavior
            Platform.runLater(homeDescTop!!::requestFocus)
        }
    }

    private inner class EditorController(private val account: Account?) : AbstractController() {
        @FXML
        private val editorSoftware: TextField? = null

        @FXML
        private val editorUsername: TextField? = null

        @FXML
        private val editorPassword: ReadablePasswordFieldWithStr? = null

        @FXML
        private val editorSaveBtn: Button? = null

        @FXML
        private val editorDeleteBtn: Button? = null
        private var editorDeleteCounter = false

        @FXML
        private val editorSoftwareLbl: Label? = null

        @FXML
        private val editorUsernameLbl: Label? = null

        @FXML
        private val editorPasswordLbl: Label? = null

        val isAddEditor: Boolean = (account == null)

        private var editorSaveTimeline: Timeline? = null
        private var softwareAutoCompletion: AutoCompletionBinding<String?>? = null
        private var usernameAutoCompletion: AutoCompletionBinding<String?>? = null

        @Volatile
        private var isErrBound = false

        override fun initialize(location: URL, resources: ResourceBundle) {
            Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

            val langResources = ObservableResourceFactory.getInstance()
            langResources.bindTextProperty(editorSoftwareLbl!!, "software")
            langResources.bindTextProperty(editorUsernameLbl!!, "username")
            langResources.bindTextProperty(editorPasswordLbl!!, "password")

            editorSoftware!!.onAction = { _ ->
                editorUsername!!.requestFocus() // Normally it would select the text, but that's ugly
                editorUsername.end()
            }
            editorUsername!!.onAction = { _ ->
                editorPassword!!.requestFocus() // Normally it would select the text, but that's ugly
                editorPassword.end()
            }
            editorPassword!!.setOnAction { event: ActionEvent? -> this.editorSave(event) }

            editorSaveTimeline = Timeline(
                KeyFrame(
                    Duration.ZERO,
                    { _ -> editorSaveBtn!!.style = "-fx-background-color: -fx-color-green" }),
                KeyFrame(
                    Duration.seconds(1.0),
                    { _ -> clearStyle(editorSaveBtn!!) })
            )
            editorSaveTimeline!!.cycleCount = 1

            // Setup auto-completion for software and username fields
            softwareAutoCompletion = TextFields.bindAutoCompletion(
                editorSoftware,
                getSuggestionProvider(possibleSoftwares!!)
            )
            usernameAutoCompletion = TextFields.bindAutoCompletion(
                editorUsername,
                getSuggestionProvider(possibleUsernames!!)
            )

            // Update auto-completion when suggestions change
            suggestionsUpdateTrigger.addListener(ChangeListener { _, _, _ ->
                if (this.isErrBound) return@ChangeListener
                unbindAutoCompletion()
                bindAutoCompletion()
            })

            // Disable the delete button if this is the add editor
            editorDeleteBtn!!.isVisible = !isAddEditor
        }

        override val fxmlPath = "/fxml/main/manager/editor.fxml"

        override fun reset() {
            if (isAddEditor) {
                clearTextFields(editorSoftware!!, editorUsername!!, editorPassword!!)
                editorPassword.isReadable = false
                Platform.runLater(editorSoftware::requestFocus)
            } else {
                clearErrorLoadState()
                // Do operations HERE, if needed
                LoadingAnimation.start(
                    editorSoftware!!,
                    editorUsername!!,
                    editorPassword!!,
                    editorSaveBtn!!,
                    editorDeleteBtn!!
                )

                IOManager.getInstance().getAccountData(account!!)
                    .whenComplete(BiConsumer { data: AccountData?, ex: Throwable? ->
                        Platform.runLater(Runnable {
                            LoadingAnimation.stop(
                                editorSoftware,
                                editorUsername,
                                editorPassword,
                                editorSaveBtn,
                                editorDeleteBtn
                            )
                            val success = (ex == null && data != null)
                            if (!success) {
                                applyErrorLoadState()
                                return@Runnable
                            }

                            clearErrorLoadState()
                            editorSoftware.text = data.software
                            editorUsername.text = data.username
                            editorPassword.text = data.password

                            // When everything is ready, focus the software field and set caret to the end of the text
                            // (not setting the caret would result in the text being selected, which is really weird when editing)
                            editorSoftware.requestFocus()
                            editorSoftware.end()
                        })
                    })
            }

            ObservableResourceFactory.getInstance()
                .bindPromptTextProperty(editorSoftware, editorUsername, editorPassword)

            editorDeleteCounter = false
            clearStyle(
                editorSoftware,
                editorUsername,
                editorPassword,
                editorDeleteBtn!!
            )
        }

        @FXML
        fun editorSave(event: ActionEvent?) {
            if (checkTextFields(editorSoftware!!, editorUsername!!, editorPassword!!)) {
                editorSaveTimeline!!.playFromStart()

                // Reset the delete button state
                editorDeleteCounter = false
                clearStyle(editorDeleteBtn!!)

                // get the new software, username and password
                val software = editorSoftware.text.trim()
                val username = editorUsername.text.trim()
                val password = editorPassword.text.trim()
                val data = AccountData(software, username, password)

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
                if (isAddEditor) {
                    IOManager.getInstance().addAccount(data)
                    reset()
                } else {
                    editOperationsCounter.incrementAndGet()
                    LoadingAnimation.start(
                        editorSoftware,
                        editorUsername,
                        editorPassword,
                        editorSaveBtn!!,
                        editorDeleteBtn
                    )
                    IOManager.getInstance().editAccount(account!!, data)
                        .whenComplete(BiConsumer { account: Account?, ex: Throwable? ->
                            LoadingAnimation.stop(
                                editorSoftware,
                                editorUsername,
                                editorPassword,
                                editorSaveBtn,
                                editorDeleteBtn
                            )
                            editOperationsCounter.decrementAndGet()

                            val success = (ex == null && account != null)
                            if (!success) {
                                Platform.runLater(this::reset)
                                return@BiConsumer
                            }

                            // Re-write the fields since the loading animation leaves "Loading..."
                            editorSoftware.text = data.software
                            editorUsername.text = data.username
                            editorPassword.text = data.password
                        })
                }
            }
        }

        @FXML
        fun editorDelete(event: ActionEvent?) {
            // when the deleteCounter is true it means that the user has confirmed the elimination
            if (editorDeleteCounter) {
                IOManager.getInstance().removeAccount(account!!)
            } else {
                editorDeleteBtn!!.style = "-fx-background-color: -fx-color-red"
                editorDeleteCounter = true
            }
        }

        fun getSuggestionProvider(sourceList: MutableList<String?>): Callback<ISuggestionRequest?, MutableCollection<String?>?> {
            return Callback { request: ISuggestionRequest? ->
                val userText = request!!.userText
                if (userText == null || userText.isEmpty()) return@Callback mutableListOf()

                val defaultLocale = SupportedLocale.DEFAULT.locale
                val lowerUserText = userText.lowercase(defaultLocale)
                sourceList.stream()
                    .filter { s: String? -> s!!.lowercase(defaultLocale).startsWith(lowerUserText) }
                    .toList()
            }
        }

        fun bindAutoCompletion() {
            if (softwareAutoCompletion == null) {
                softwareAutoCompletion =
                    TextFields.bindAutoCompletion(editorSoftware, getSuggestionProvider(possibleSoftwares!!))
            }
            if (usernameAutoCompletion == null) {
                usernameAutoCompletion =
                    TextFields.bindAutoCompletion(editorUsername, getSuggestionProvider(possibleUsernames!!))
            }
        }

        fun clearErrorLoadState() {
            if (!this.isErrBound) return

            editorSoftware!!.textProperty().unbind()
            editorSoftware.isDisable = false
            editorUsername!!.textProperty().unbind()
            editorUsername.isDisable = false
            editorPassword!!.textProperty().unbind()
            editorPassword.isDisable = false
            editorSaveBtn!!.isDisable = false
            editorDeleteBtn!!.isDisable = false

            editorPassword.isReadable = false

            this.isErrBound = false
            bindAutoCompletion()
        }

        fun applyErrorLoadState() {
            clearErrorLoadState()
            unbindAutoCompletion()
            this.isErrBound = true

            // Must be done before binding textProperty(): the skin refresh logic calls setText(...)
            // when readability changes, and that would fail on a bound text property.
            editorPassword!!.isReadable = true

            val resFact = ObservableResourceFactory.getInstance()
            resFact.bindStringProperty(editorSoftware!!.textProperty(), "editor.load_error")
            editorSoftware.isDisable = true
            resFact.bindStringProperty(editorUsername!!.textProperty(), "editor.load_error")
            editorUsername.isDisable = true
            resFact.bindStringProperty(editorPassword.textProperty(), "editor.load_error")
            editorPassword.isDisable = true
            editorSaveBtn!!.isDisable = true
            editorDeleteBtn!!.isDisable = true
        }

        fun unbindAutoCompletion() {
            if (softwareAutoCompletion != null) {
                softwareAutoCompletion!!.dispose()
                softwareAutoCompletion = null
            }
            if (usernameAutoCompletion != null) {
                usernameAutoCompletion!!.dispose()
                usernameAutoCompletion = null
            }
        }
    }
}