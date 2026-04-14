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
import javafx.scene.layout.AnchorPane
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
        eulaStage ?. also {
            it.show()
            it.toFront()
        }
    }

    companion object {
        // Store EULA stage as singleton to avoid multiple instances
        private var eulaStage: Stage? = null

        private fun loadEula() {
            if (eulaStage != null) return

            eulaStage = Stage() .also {
                ObservableResourceFactory.getInstance().bindTitleProperty(it, "terms_credits")
                it.icons.add(Image(App.MAIN_ICON))
                it.isResizable = false

                val eulaParent = Utils.loadFxml(EulaController())
                it.scene = Scene(eulaParent, 900.0, 600.0)
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
            var nonEmpty = true

            for (field in fields) {
                if (field.text.isBlank()) {
                    nonEmpty = false
                    field.style = "-fx-border-color: -fx-color-red"
                } else {
                    field.style = "-fx-border-color: -fx-color-grey"
                }
            }

            return nonEmpty
        }

        @JvmStatic
        protected fun clearStyle(vararg nodes: Node) = nodes.forEach { it.style = "" }

        @JvmStatic
        protected fun clearTextFields(vararg fields: TextInputControl) = fields.forEach(TextInputControl::clear)
    }
}