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
package password.manager.app.controllers

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TextInputControl
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import password.manager.app.App
import password.manager.app.Utils
import password.manager.app.controllers.extra.EulaController
import password.manager.app.singletons.ObservableResourceFactory

abstract class AbstractController : Initializable {

    abstract val fxmlPath: String

    abstract fun reset()

    @FXML
    protected fun showEula(event: MouseEvent?) {
        loadEula()
        eulaStage?.apply {
            show()
            toFront()
        }
    }

    companion object {
        // Store EULA stage as singleton to avoid multiple instances
        private var eulaStage: Stage? = null

        private fun loadEula() {
            if (eulaStage != null) return

            eulaStage = Stage().apply {
                ObservableResourceFactory.getInstance().bindTitleProperty(this, "terms_credits")
                icons.add(Image(App.MAIN_ICON))
                isResizable = false
                scene = Scene(Utils.loadFxml(EulaController()), 900.0, 600.0)
            }
        }

        /**
         * Check if text fields are non-empty. If empty, set red border style.
         * All calls made from the JavaFX Application Thread are safe without further synchronization.
         * @param fields the text fields to check
         * @return true if all fields are non-empty, false otherwise
         */
        @JvmStatic
        protected fun checkTextFields(vararg fields: TextInputControl): Boolean {
            val allNonEmpty = fields.all { it.text.isNotBlank() }
            fields.forEach { field ->
                field.style = if (field.text.isNotBlank()) "-fx-border-color: -fx-color-grey" else "-fx-border-color: -fx-color-red"
            }
            return allNonEmpty
        }

        @JvmStatic
        protected fun clearStyle(vararg nodes: Node) = nodes.forEach { it.style = "" }

        @JvmStatic
        protected fun clearTextFields(vararg fields: TextInputControl) = fields.forEach(TextInputControl::clear)
    }
}