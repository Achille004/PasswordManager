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
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.image.ImageView
import javafx.util.StringConverter
import org.jetbrains.annotations.Contract
import password.manager.app.Utils
import password.manager.app.base.*
import password.manager.app.controllers.AbstractController
import password.manager.app.security.UserPreferences
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.ReadablePasswordFieldWithStr
import java.net.URL
import java.text.Collator
import java.util.ResourceBundle
import java.util.function.Function

class SettingsController : AbstractController() {
    @FXML
    private lateinit var settingsLangCB: ComboBox<SupportedLocale>

    @FXML
    private lateinit var settingsOrderCB: ComboBox<SortingOrder>

    @FXML
    private lateinit var settingsMasterPassword: ReadablePasswordFieldWithStr

    @FXML
    private lateinit var settingsLangLbl: Label

    @FXML
    private lateinit var settingsSortingOrderLbl: Label

    @FXML
    private lateinit var settingsMasterPasswordLbl: Label

    @FXML
    private lateinit var settingsMasterPasswordDesc: Label

    @FXML
    private lateinit var settingsDriveConnLbl: Label

    @FXML
    private lateinit var wip: Label

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        Logger.addDebug("Initializing %s", javaClass.getSimpleName())

        val langResources = ObservableResourceFactory
        langResources.bindTextProperty(settingsLangLbl!!, "settings.language")
        langResources.bindTextProperty(settingsSortingOrderLbl!!, "settings.sorting_ord")
        langResources.bindTextProperty(settingsMasterPasswordLbl!!, "settings.master_pas")
        langResources.bindTextProperty(settingsMasterPasswordDesc!!, "settings.master_pas.desc")
        langResources.bindTextProperty(settingsDriveConnLbl!!, "settings.drive_con")
        langResources.bindTextProperty(wip!!, "settings.wip")

        // Combo boxes
        val userPreferences = IOManager.userPreferences
        setupLanguageCB(userPreferences)
        setupSortingOrderCB(userPreferences)

        // Master password
        settingsMasterPassword!!.onAction = {
            if (checkTextFields(settingsMasterPassword)) {
                IOManager.changeMasterPassword(settingsMasterPassword.text.trim())
            }
        }
    }

    override val fxmlPath = "/fxml/main/settings.fxml"

    override fun reset() {
        clearStyle(settingsMasterPassword!!)
        ObservableResourceFactory.bindPromptTextProperty(settingsMasterPassword)

        IOManager.displayMasterPassword(settingsMasterPassword)
        settingsMasterPassword.isReadable = false
    }

    private fun setupLanguageCB(userPreferences: UserPreferences) {
        val localeProperty = userPreferences.localeProperty()

        val languages = Utils.getFXSortedList(*SupportedLocale.entries.toTypedArray())
        val locStringConverterMapper = stringConverterMapper {
            _, item: SupportedLocale -> item.displayName
        }

        settingsLangCB!!.cellFactory = { FlagListCell() }
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
        val sortOrdStringConverterMapper = stringConverterMapper {
            _, item: SortingOrder -> ObservableResourceFactory.getValue(item.i18nKey)
        }

        settingsOrderCB!!.items = sortingOrders
        settingsOrderCB.getSelectionModel().select(sortingOrderProperty.get())
        bindValueConverter(settingsOrderCB, localeProperty, sortOrdStringConverterMapper)
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB)

        sortingOrderProperty.bind(notNullBinding(settingsOrderCB.valueProperty()))
    }

    private class FlagListCell : ListCell<SupportedLocale?>() {
        private val imageView = ImageView().apply {
            fitHeight = FLAG_SIZE
            fitWidth = FLAG_SIZE
            isPreserveRatio = true
        }

        override fun updateItem(item: SupportedLocale?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                text = null
                graphic = null
                return
            }

            text = item.displayName

            imageView.image = item.flagImage
            graphic = imageView
        }

        companion object {
            private const val FLAG_SIZE = 20.0
        }
    }

    companion object {
        @Contract(value = "_ -> new", pure = true)
        private fun <T> stringConverterMapper(converter: Function2<SupportedLocale, T, String>):
                Function<SupportedLocale, StringConverter<T>> = {
            locale: SupportedLocale -> object : StringConverter<T>() {
                override fun fromString(string: String?) = throw UnsupportedOperationException()
                override fun toString(obj: T) = converter(locale, obj)
            }
        }

        @Contract(value = "_ -> new", pure = true)
        private fun <T> notNullBinding(property: ObjectProperty<T?>): ObjectBinding<T?> =
            object : ObjectBinding<T?>() {
                private var cachedValue: T? = property.value

                init {
                    bind(property)
                }

                override fun computeValue(): T? = property.value
                    ?.also { cachedValue = it }
                    ?: cachedValue
            }

        private fun <T> comparatorBinding(
            locale: ObjectProperty<SupportedLocale>,
            converter: ObjectProperty<out StringConverter<T>>
        ) = Bindings.createObjectBinding(
            {
                Comparator.comparing(
                    converter.value::toString,
                    Collator.getInstance(locale.value.locale)
                )
            },
            locale, converter
        )

        private fun <T> bindValueConverter(
            comboBox: ComboBox<T>,
            locale: ObjectProperty<SupportedLocale>,
            mapper: Function<SupportedLocale, StringConverter<T>>
        ) = comboBox.converterProperty().bind(locale.map(mapper))

        private fun <T> bindValueComparator(
            sortedList: SortedList<T>,
            locale: ObjectProperty<SupportedLocale>,
            comboBox: ComboBox<T>
        ) = sortedList.comparatorProperty().bind(comparatorBinding(locale, comboBox.converterProperty()))
    }
}
