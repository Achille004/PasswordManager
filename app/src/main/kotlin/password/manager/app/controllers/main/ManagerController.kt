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
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.collections.transformation.SortedList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
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
import java.util.*
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.concurrent.Volatile

class ManagerController : AbstractController() {
    @FXML
    private lateinit var accountListView: ListView<Account>

    @FXML
    private lateinit var accountTabPane: TabPane

    @FXML
    private lateinit var addTab: Tab

    @FXML
    private lateinit var homeTab: Tab

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var matchCaseButton: Button

    @FXML
    private lateinit var matchWholeWordButton: Button

    private lateinit var searchTimeline: Timeline

    // App state variables
    private val editOperationsCounter = AtomicInteger(0)

    @Volatile
    private var isMatchCase = false

    @Volatile
    private var isMatchWholeWord = false

    // Auto-completion data sources
    private lateinit var possibleSoftwares: List<String?>
    private lateinit var possibleUsernames: List<String?>
    private val suggestionListeners: MutableSet<Function0<Unit>> = mutableSetOf()
    private val punctSplitRegex = Regex("[\\s\\p{P}]+")

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        Logger.addDebug("Initializing %s", javaClass.getSimpleName())

        // Wrapper scheme: ((( source_list ) sorted_wrapper ) filtered_wrapper )
        val accountList = IOManager.accountList
        val sortedAccountList = SortedList(accountList)
        val filteredAccountList = FilteredList(sortedAccountList)

        val sortingOrderProperty = IOManager.userPreferences.sortingOrderProperty()
        val tabManager = TabManager(accountTabPane, ::EditorController) {
            tab: Tab, account: Account -> tab.textProperty().bind(account.softwareProperty)
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
        isMatchCase = !isMatchCase
        if (isMatchCase) {
            matchCaseButton.style = "-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;"
        } else {
            clearStyle(matchCaseButton)
        }
        searchTimeline.playFrom(SEARCH_DELAY)
    }

    @FXML
    fun matchWholeWordAction(event: ActionEvent?) {
        isMatchWholeWord = !isMatchWholeWord
        if (isMatchWholeWord) {
            matchWholeWordButton.style = "-fx-background-color: -fx-color-green; -fx-background-radius: 2deg;"
        } else {
            clearStyle(matchWholeWordButton)
        }
        searchTimeline.playFrom(SEARCH_DELAY)
    }

    private fun setupAutoCompletion(accountList: ObservableList<Account>) {
        val fieldListExtractor = { fieldExtractor: Function1<Account, String> ->
            val fieldSortedByUsage: MutableMap<String, Long> = accountList.stream()
                .map(fieldExtractor)
                .collect( Collectors.groupingBy(
                    { s: String -> s },
                    Collectors.counting()
                ) )

            fieldSortedByUsage.entries.stream()
                .sorted(Map.Entry.comparingByValue<String, Long>().reversed())
                .map(MutableMap.MutableEntry<String, Long>::key)
                .toList()
        }

        // Listen for changes in the account list and update suggestions
        accountList.addListener(ListChangeListener {
            possibleSoftwares = fieldListExtractor(Account::software)
            possibleUsernames = fieldListExtractor(Account::username)
            // Notify every open editor that its suggestions need refreshing.
            suggestionListeners.toList().forEach(Function0<Unit>::invoke)
        })

        // Initial population
        possibleSoftwares = fieldListExtractor(Account::software)
        possibleUsernames = fieldListExtractor(Account::username)
    }

    private fun setupSearchFunctionality(filteredAccountList: FilteredList<Account>) {
        val defaultLocale = SupportedLocale.DEFAULT.locale

        searchTimeline = Timeline(KeyFrame(SEARCH_DELAY, {
            val searchText = searchField.text.trim()
            if (searchText.isEmpty()) {
                filteredAccountList.predicate = null // Show all accounts
                return@KeyFrame
            }

            val finalSearchText = if (isMatchCase) searchText else searchText.lowercase(defaultLocale)
            val wholeWordPredicate: Predicate<String> = { str: String ->
                str.split(punctSplitRegex).dropLastWhile(String::isEmpty).contains(finalSearchText)
            }

            filteredAccountList.predicate = { account: Account ->
                val software = if (isMatchCase) account.software else account.software.lowercase(defaultLocale)
                val username = if (isMatchCase) account.username else account.username.lowercase(defaultLocale)
                if (isMatchWholeWord) {
                    wholeWordPredicate.test(software) || wholeWordPredicate.test(username)
                } else {
                    software.contains(finalSearchText) || username.contains(finalSearchText)
                }
            }
        }))
        searchTimeline.cycleCount = 1

        searchField.textProperty().addListener { searchTimeline.playFromStart() }
        searchField.onAction = {
            searchTimeline.stop()
            searchTimeline.playFrom(SEARCH_DELAY)
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
                    change.getRemoved().forEach(tabManager::removeTab)
                }
            }
        }

        sortedAccountList.comparatorProperty().bind(
            sortingOrderProperty.map { order: SortingOrder? -> order?.comparator }
        )
        sortedAccountList.addListener(accountListChangeHandler)

        // #endregion

        // #region Account ListView setup
        val accountCellFactory: Callback<ListView<Account?>, ListCell<Account?>> = {
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
                        sortingOrderProperty, account.softwareProperty, account.usernameProperty
                    )
                    textProperty().bind(textBinding)
                }
            }
        }

        accountListView.items = filteredAccountList
        accountListView.cellFactory = accountCellFactory
        accountListView.getSelectionModel().selectedItemProperty().addListener { _, _, newItem: Account? ->
            // Return if the target account is null or any edit operation is being run
            newItem ?: return@addListener
            if (editOperationsCounter.get() > 0) return@addListener

            tabManager.openTab(newItem)
            // Defer the task to avoid conflicts on the underlying list of selected accounts
            Platform.runLater(accountListView.getSelectionModel()::clearSelection)
        }
        // #endregion
    }

    private fun setupKeyboardShortcuts(tabManager: TabManager<Account, EditorController>) {
        accountTabPane.onKeyPressed = EventHandler { keyEvent: KeyEvent ->
            val selectedTab = accountTabPane.selectionModel.selectedItem ?: return@EventHandler
            if (!keyEvent.isControlDown) return@EventHandler

            when (keyEvent.code) {
                KeyCode.W -> {
                    keyEvent.consume()
                    when (selectedTab) {
                        homeTab -> {}
                        addTab -> tabManager.selectTab(homeTab)
                        else -> tabManager.closeTab(selectedTab)
                    }
                }
                KeyCode.T -> {
                    keyEvent.consume()
                    when (selectedTab) {
                        addTab -> {}
                        else -> tabManager.selectTab(addTab)
                    }
                }
                KeyCode.Q, KeyCode.E -> {
                    keyEvent.consume()
                    tabManager.selectAdjacentTab(if (keyEvent.code == KeyCode.Q) -1 else 1)
                }
                else -> {}
            }
        }
    }

    private fun setupSpecialTabs(tabPane: TabPane, tabManager: TabManager<Account, EditorController>) {
        TabManager.loadTab(homeTab, HomeController())
        TabManager.loadTab(addTab, EditorController(null))

        val tabPaneContent = tabPane.tabs
        tabPaneContent.addListener(ListChangeListener { change ->
            while (change.next()) {
                // Dispose of any removed tabs that are EditorControllers
                if (change.wasRemoved()) {
                    change.removed
                        .mapNotNull { it.properties["controller"] as? EditorController }
                        .forEach(EditorController::dispose)
                }

                // Defer tasks to avoid conflicts on the underlying list of tabs
                when {
                    change.wasAdded() && !change.addedSubList.contains(homeTab) -> Platform.runLater {
                        tabPaneContent.remove(homeTab)
                    }
                    change.wasRemoved() && tabPaneContent.size <= 1 -> Platform.runLater {
                        tabPaneContent.addFirst(homeTab)
                        tabManager.selectTab(homeTab)
                    }
                }
            }
        })
    }

    companion object {
        val SEARCH_DELAY: Duration = Duration.millis(300.0)
    }

    internal class HomeController : AbstractController() {
        @FXML
        private lateinit var homeDescTop: Label

        @FXML
        private lateinit var homeDescBtm: Label

        override fun initialize(location: URL?, resources: ResourceBundle?) {
            Logger.addDebug("Initializing %s", javaClass.getSimpleName())

            val langResources = ObservableResourceFactory
            langResources.bindTextProperty(homeDescTop, "home_desc.top")
            langResources.bindTextProperty(homeDescBtm, "home_desc.btm")
        }

        override val fxmlPath = "/fxml/main/manager/home.fxml"

        override fun reset() {
            // Just focus on the top label to keep consistent behavior
            Platform.runLater(homeDescTop::requestFocus)
        }
    }

    private inner class EditorController(private val account: Account?) : AbstractController() {
        @FXML
        private lateinit var editorSoftware: TextField

        @FXML
        private lateinit var editorUsername: TextField

        @FXML
        private lateinit var editorPassword: ReadablePasswordFieldWithStr

        @FXML
        private lateinit var editorSaveBtn: Button

        @FXML
        private lateinit var editorDeleteBtn: Button
        private var editorDeleteCounter = false

        @FXML
        private lateinit var editorSoftwareLbl: Label

        @FXML
        private lateinit var editorUsernameLbl: Label

        @FXML
        private lateinit var editorPasswordLbl: Label

        val isAddEditor: Boolean = (account == null)

        private lateinit var editorSaveTimeline: Timeline
        private var softwareAutoCompletion: AutoCompletionBinding<String?>? = null
        private var usernameAutoCompletion: AutoCompletionBinding<String?>? = null
        private val suggestionListener: Function0<Unit> = {
            if (!isErrBound) {
                unbindAutoCompletion()
                bindAutoCompletion()
            }
        }

        @Volatile
        private var isErrBound = false

        override fun initialize(location: URL?, resources: ResourceBundle?) {
            Logger.addDebug("Initializing %s", javaClass.getSimpleName())

            val langResources = ObservableResourceFactory
            langResources.bindTextProperty(editorSoftwareLbl, "software")
            langResources.bindTextProperty(editorUsernameLbl, "username")
            langResources.bindTextProperty(editorPasswordLbl, "password")

            editorSoftware.onAction = {
                editorUsername.requestFocus()
                editorUsername.end()
            }
            editorUsername.onAction = {
                editorPassword.requestFocus()
                editorPassword.end()
            }
            editorPassword.setOnAction(this::editorSave)

            editorSaveTimeline = Timeline(
                KeyFrame(
                    Duration.ZERO,
                    { editorSaveBtn.style = "-fx-background-color: -fx-color-green" }),
                KeyFrame(
                    Duration.seconds(1.0),
                    { clearStyle(editorSaveBtn) })
            )
            editorSaveTimeline.cycleCount = 1

            // Set up auto-completion for software and username fields
            bindAutoCompletion()

            // Register this editor so it follows suggestion changes for all open tabs.
            suggestionListeners.add(suggestionListener)

            // Disable the delete button if this is the add editor
            editorDeleteBtn.isVisible = !isAddEditor
        }

        override val fxmlPath = "/fxml/main/manager/editor.fxml"

        fun dispose() {
            suggestionListeners.remove(suggestionListener)
            unbindAutoCompletion()
        }

        override fun reset() {
            if (isAddEditor) {
                clearTextFields(editorSoftware, editorUsername, editorPassword)
                editorPassword.isReadable = false
                Platform.runLater(editorSoftware::requestFocus)
            } else {
                val accNotNull = checkNotNull(account) {
                    "Existing account editor has no account"
                }
                
                clearErrorLoadState()
                // Do operations HERE, if needed
                LoadingAnimation.start(
                    editorSoftware,
                    editorUsername,
                    editorPassword,
                    editorSaveBtn,
                    editorDeleteBtn
                )

                IOManager.getAccountData(accNotNull)
                    .whenComplete { data, ex: Throwable? ->
                        Platform.runLater {
                            LoadingAnimation.stop(
                                editorSoftware,
                                editorUsername,
                                editorPassword,
                                editorSaveBtn,
                                editorDeleteBtn
                            )

                            if (ex != null) {
                                applyErrorLoadState()
                                return@runLater
                            }

                            clearErrorLoadState()
                            editorSoftware.text = data.software
                            editorUsername.text = data.username
                            editorPassword.text = data.password

                            // When everything is ready, focus the software field and set caret to the end of the text
                            // (not setting the caret would result in the text being selected, which is really weird when editing)
                            editorSoftware.requestFocus()
                            editorSoftware.end()
                        }
                    }
            }

            ObservableResourceFactory
                .bindPromptTextProperty(editorSoftware, editorUsername, editorPassword)

            editorDeleteCounter = false
            clearStyle(
                editorSoftware,
                editorUsername,
                editorPassword,
                editorDeleteBtn
            )
        }

        @FXML
        fun editorSave(event: ActionEvent?) {
            if (!checkTextFields(editorSoftware, editorUsername, editorPassword)) return
            editorSaveTimeline.playFromStart()

            editorDeleteCounter = false
            clearStyle(editorDeleteBtn)

            val software = editorSoftware.text.trim()
            val username = editorUsername.text.trim()
            val password = editorPassword.text.trim()
            val data = AccountData(software, username, password)

            if (isAddEditor) {
                IOManager.addAccount(data)
                reset()
            } else {
                val accNotNull = checkNotNull(account) {
                    "Existing account editor has no account"
                }

                editOperationsCounter.incrementAndGet()
                LoadingAnimation.start(
                    editorSoftware,
                    editorUsername,
                    editorPassword,
                    editorSaveBtn,
                    editorDeleteBtn
                )

                IOManager.editAccount(account, data)
                    .whenComplete { account: Account?, ex: Throwable? ->
                        Platform.runLater {
                            LoadingAnimation.stop(
                                editorSoftware,
                                editorUsername,
                                editorPassword,
                                editorSaveBtn,
                                editorDeleteBtn
                            )
                            editOperationsCounter.decrementAndGet()

                            if (ex != null || account == null) {
                                Platform.runLater(this::reset)
                                return@runLater
                            }

                            editorSoftware.text = data.software
                            editorUsername.text = data.username
                            editorPassword.text = data.password
                        }
                    }
            }
        }

        @FXML
        fun editorDelete(event: ActionEvent?) {
            if (editorDeleteCounter) {
                val accNotNull = checkNotNull(account) {
                    "Existing account editor has no account"
                }
                IOManager.removeAccount(accNotNull)
            } else {
                editorDeleteBtn.style = "-fx-background-color: -fx-color-red"
                editorDeleteCounter = true
            }
        }

        private fun getSuggestionProvider(sourceList: Collection<String?>): Callback<ISuggestionRequest?, Collection<String?>?> {
            return Callback { request: ISuggestionRequest? ->
                // Return if the request is null or the user text is null or empty
                val userText = request?.userText ?: return@Callback emptyList()
                if (userText.isEmpty()) return@Callback emptyList()

                val defaultLocale = SupportedLocale.DEFAULT.locale
                val lowerUserText = userText.lowercase(defaultLocale)
                // Filter the list while also discarding null values from possible suggestions
                sourceList.filter { it?.lowercase(defaultLocale)?.startsWith(lowerUserText) ?: false }
            }
        }

        private fun bindAutoCompletion() {
            if (softwareAutoCompletion == null) {
                softwareAutoCompletion = TextFields.bindAutoCompletion(
                    editorSoftware,
                    getSuggestionProvider(possibleSoftwares)
                )
            }
            if (usernameAutoCompletion == null) {
                usernameAutoCompletion = TextFields.bindAutoCompletion(
                    editorUsername,
                    getSuggestionProvider(possibleUsernames)
                )
            }
        }

        private fun clearErrorLoadState() {
            if (!isErrBound) return

            listOf(editorSoftware, editorUsername, editorPassword, editorSaveBtn, editorDeleteBtn).forEach {
                when (it) {
                    is TextInputControl -> {
                        it.textProperty().unbind()
                        it.isDisable = false
                    }
                    is Button -> it.isDisable = false
                }
            }

            editorPassword.isReadable = false
            isErrBound = false
            bindAutoCompletion()
        }

        private fun applyErrorLoadState() {
            clearErrorLoadState()
            unbindAutoCompletion()
            isErrBound = true

            editorPassword.isReadable = true

            val resFact = ObservableResourceFactory
            listOf(
                editorSoftware,
                editorUsername,
                editorPassword
            ).forEach {
                resFact.bindStringProperty(it.textProperty(), "editor.load_error")
                it.isDisable = true
            }
            editorSaveBtn.isDisable = true
            editorDeleteBtn.isDisable = true
        }

        private fun unbindAutoCompletion() {
            softwareAutoCompletion?.dispose()
            softwareAutoCompletion = null

            usernameAutoCompletion?.dispose()
            usernameAutoCompletion = null
        }
    }
}
