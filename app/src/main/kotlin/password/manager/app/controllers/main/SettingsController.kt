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

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.image.ImageView
import javafx.util.Callback
import javafx.util.StringConverter
import org.jetbrains.annotations.Contract
import password.manager.app.Utils
import password.manager.app.base.SortingOrder
import password.manager.app.base.SupportedLocale
import password.manager.app.controllers.AbstractController
import password.manager.app.security.UserPreferences
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.ReadablePasswordFieldWithStr
import java.net.URL
import java.text.Collator
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Function

class SettingsController : AbstractController() {
    @FXML
    private val settingsLangCB: ComboBox<SupportedLocale?>? = null

    @FXML
    private val settingsOrderCB: ComboBox<SortingOrder?>? = null

    @FXML
    private val settingsMasterPassword: ReadablePasswordFieldWithStr? = null

    @FXML
    private val settingsLangLbl: Label? = null

    @FXML
    private val settingsSortingOrderLbl: Label? = null

    @FXML
    private val settingsMasterPasswordLbl: Label? = null

    @FXML
    private val settingsMasterPasswordDesc: Label? = null

    @FXML
    private val settingsDriveConnLbl: Label? = null

    @FXML
    private val wip: Label? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

        val ioManager = IOManager.getInstance()
        val langResources = ObservableResourceFactory.getInstance()

        langResources.bindTextProperty(settingsLangLbl!!, "settings.language")
        langResources.bindTextProperty(settingsSortingOrderLbl!!, "settings.sorting_ord")
        langResources.bindTextProperty(settingsMasterPasswordLbl!!, "settings.master_pas")
        langResources.bindTextProperty(settingsMasterPasswordDesc!!, "settings.master_pas.desc")
        langResources.bindTextProperty(settingsDriveConnLbl!!, "settings.drive_con")
        langResources.bindTextProperty(wip!!, "settings.wip")

        // Combo boxes
        val userPreferences = ioManager.userPreferences
        setupLanguageCB(userPreferences)
        setupSortingOrderCB(userPreferences)

        // Master password
        settingsMasterPassword!!.onAction = { _ ->
            if (checkTextFields(settingsMasterPassword)) {
                ioManager.changeMasterPassword(settingsMasterPassword.text.trim())
            }
        }
    }

    override val fxmlPath = "/fxml/main/settings.fxml"

    override fun reset() {
        clearStyle(settingsMasterPassword!!)
        ObservableResourceFactory.getInstance().bindPromptTextProperty(settingsMasterPassword)

        IOManager.getInstance().displayMasterPassword(settingsMasterPassword)
        settingsMasterPassword.isReadable = false
    }

    private fun setupLanguageCB(userPreferences: UserPreferences) {
        val localeProperty = userPreferences.localeProperty()

        val languages = Utils.getFXSortedList(*SupportedLocale.entries.toTypedArray())
        val locStringConverterMapper = stringConverterMapper { locale: SupportedLocale, item: SupportedLocale ->
            Utils.capitalizeWord(item.locale.displayName)
        }

        settingsLangCB!!.cellFactory = { _ -> FlagListCell() }
        settingsLangCB.buttonCell = FlagListCell()

        settingsLangCB.items = languages
        settingsLangCB.getSelectionModel().select(localeProperty.get())
        bindValueConverter(settingsLangCB, localeProperty, locStringConverterMapper)
        bindValueComparator(languages, localeProperty, settingsLangCB)

        localeProperty.bind(notNullBinding(settingsLangCB.valueProperty()))
    }

    private fun setupSortingOrderCB(userPreferences: UserPreferences) {
        val localeProperty = userPreferences.localeProperty()
        val sortingOrderProperty = userPreferences.sortingOrderProperty()

        val sortingOrders = Utils.getFXSortedList(*SortingOrder.entries.toTypedArray())
        val sortOrdStringConverterMapper = stringConverterMapper { locale: SupportedLocale, item: SortingOrder ->
            ObservableResourceFactory.getInstance().getValue(item.i18nKey)
        }

        settingsOrderCB!!.items = sortingOrders
        settingsOrderCB.getSelectionModel().select(sortingOrderProperty.get())
        bindValueConverter(settingsOrderCB, localeProperty, sortOrdStringConverterMapper)
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB)

        sortingOrderProperty.bind(notNullBinding(settingsOrderCB.valueProperty()))
    }

    private class FlagListCell : ListCell<SupportedLocale?>() {
        private val imageView = ImageView()

        init {
            imageView.fitHeight = FLAG_SIZE
            imageView.fitWidth = FLAG_SIZE
            imageView.isPreserveRatio = true
        }

        override fun updateItem(item: SupportedLocale?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                text = null
                graphic = null
                return
            }

            val locale = item.locale
            val displayName = Utils.capitalizeWord(locale.getDisplayName(locale))
            text = displayName

            val flag = item.flagImage
            imageView.image = flag
            graphic = imageView
        }

        companion object {
            private const val FLAG_SIZE = 20.0
        }
    }

    companion object {
        @Contract(value = "_ -> new", pure = true)
        private fun <T> stringConverterMapper(converter: Function2<SupportedLocale, T, String>) = {
            locale: SupportedLocale? -> locale ?. let {
                object : StringConverter<T?>() {
                    override fun fromString(string: String?) = throw UnsupportedOperationException()
                    override fun toString(obj: T?) = obj ?.let { converter.invoke(locale, obj) }
                }
            }
        }

        @Contract(value = "_ -> new", pure = true)
        private fun <T> notNullBinding(property: ObjectProperty<T?>): ObjectBinding<T?> {
            return object : ObjectBinding<T?>() {
                private var cachedValue = property.getValue()

                init {
                    bind(property)
                }

                override fun computeValue() = property.getValue()
                    ?.also { cachedValue = it }
                    ?: cachedValue
            }
        }

        private fun <T> comparatorBinding(
            locale: ObjectProperty<SupportedLocale?>,
            converter: ObjectProperty<out StringConverter<T?>?>
        ) = Bindings.createObjectBinding(
             {
                Comparator.comparing(
                    { t: T? -> converter.getValue().toString(t) },
                    Collator.getInstance(locale.getValue()!!.locale)
                )
            },
            locale, converter
        )

        private fun <T> bindValueConverter(
            comboBox: ComboBox<T?>,
            locale: ObjectProperty<SupportedLocale?>,
            mapper: Function<SupportedLocale?, StringConverter<T?>?>
        ) = comboBox.converterProperty().bind(locale.map(mapper))

        private fun <T> bindValueComparator(
            sortedList: SortedList<T?>,
            locale: ObjectProperty<SupportedLocale?>,
            comboBox: ComboBox<T?>
        ) = sortedList.comparatorProperty().bind(comparatorBinding(locale, comboBox.converterProperty()))
    }
}
