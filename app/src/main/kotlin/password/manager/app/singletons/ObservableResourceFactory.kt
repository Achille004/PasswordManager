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
package password.manager.app.singletons

import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.Labeled
import javafx.scene.control.TextInputControl
import javafx.stage.Stage
import password.manager.app.base.SupportedLocale
import java.util.*

object ObservableResourceFactory : Singleton() {
    private const val LANG_BUNDLE_RESOURCE = "/bundles/Lang"

    private val resourcesInternal: ObjectProperty<ResourceBundle> = SimpleObjectProperty()
    private val emptyFieldPrompts: MutableList<StringProperty> = ArrayList()
    private var activeEmptyFieldPromptCount = 0
    private val rand: Random = Random()

    // Let only package classes instantiate this
    init {
        // Fill empty field prompts when resources change
        this.resourcesInternal.addListener { _, _, newValue: ResourceBundle ->
            val count =
                try {
                    val cntStr = newValue.getString("empty_field_prompts")
                    cntStr.trim { it <= ' ' }.toInt().coerceAtLeast(0)
                } catch (_: Exception) {
                    // Key missing or invalid: treat the bundle as having no prompts.
                    0
                }

            for (i in 0..<count) {
                val key = "empty_field_${i + 1}"
                val prompt =
                    try {
                        newValue.getString(key)
                    } catch (_: Exception) {
                        ""
                    }

                if (i < emptyFieldPrompts.size) {
                    emptyFieldPrompts[i].set(prompt)
                } else {
                    emptyFieldPrompts.add(SimpleStringProperty(prompt))
                }
            }

            // Keep the properties so already-bound controls can follow later locale changes,
            // but clear and exclude prompts that are no longer available in this bundle.
            for (i in count until emptyFieldPrompts.size) emptyFieldPrompts[i].set("")
            activeEmptyFieldPromptCount = count
        }

        // This will be the "placeholder" ResourceBundle until a locale is set
        val emptyResourceBundle: ResourceBundle = ResourceBundle.getBundle(LANG_BUNDLE_RESOURCE)
        this.resourcesInternal.set(emptyResourceBundle)

        registerSingleton()
    }

    fun resourcesProperty(): ObjectProperty<ResourceBundle> = resourcesInternal
    val resources: ResourceBundle
        get() = resourcesInternal.get()

    fun bindLocaleProperty(localeProperty: ObjectProperty<SupportedLocale>) =
        this.resourcesInternal.bind(
            localeProperty.map { supLoc: SupportedLocale ->
                ResourceBundle.getBundle(LANG_BUNDLE_RESOURCE, supLoc.locale)
            }
        )

    fun getStringBinding(key: String) =
        object : StringBinding() {
            init {
                bind(resourcesProperty())
            }

            public override fun computeValue() = resources.getString(key)
        }

    fun getValue(key: String): String = resources.getString(key)

    fun bindStringProperty(property: StringProperty, key: String) =
        if (key.isBlank())
            property.unbind()
        else
            property.bind(getStringBinding(key))

    fun bindTextProperty(field: Labeled, key: String) = bindStringProperty(field.textProperty(), key)

    fun bindTitleProperty(stage: Stage, key: String) = bindStringProperty(stage.titleProperty(), key)

    fun bindPromptTextProperty(vararg fields: TextInputControl) {
        if (activeEmptyFieldPromptCount == 0) return

        for (field in fields) {
            val index = rand.nextInt(activeEmptyFieldPromptCount)
            field.promptTextProperty().unbind()
            field.promptTextProperty().bind(emptyFieldPrompts[index])
        }
    }

    override fun close() = Unit // Nothing to close
}
