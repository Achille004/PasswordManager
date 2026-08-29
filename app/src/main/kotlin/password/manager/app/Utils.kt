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
package password.manager.app

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.transformation.SortedList
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.layout.Pane
import password.manager.app.controllers.AbstractController
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import java.awt.Desktop
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

object Utils {
    /**
     * Wraps the given items in a no-comparator JavaFX [SortedList].
     * @param T The type of the items.
     * @param items The items to include in the SortedList.
     * @return A SortedList containing the given items.
     */
    @JvmStatic
    fun <T> getFXSortedList(vararg items: T): SortedList<T> =
        FXCollections.observableArrayList(*items).sorted(null)

    /**
     * Capitalizes the first letter of the given word, leaving the rest of it untouched.
     * @param str The word to capitalize.
     * @param loc The locale whose title-casing rules are applied to the first letter.
     * @return The capitalized word, or [str] itself if it is empty or does not start with a lowercase letter.
     */
    @JvmStatic
    fun capitalizeWord(str: String, loc: Locale): String =
        str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }

    /**
     * Sets the default button for the given Alert dialog.
     * @param alert  The Alert dialog to modify.
     * @param defBtnType The ButtonType to set as default.
     */
    @JvmStatic
    fun setDefaultButton(alert: Alert, defBtnType: ButtonType) {
        val pane: DialogPane = alert.dialogPane
        pane.buttonTypes.forEach { type: ButtonType ->
            val btn = pane.lookupButton(type) as Button
            btn.isDefaultButton = (type == defBtnType)
        }
    }

    /**
     * Gets a FileWriter for the specified path.
     * @param path The path to the file.
     * @param append Whether to append to the file.
     * @return A FileWriter instance or null if an error occurs.
     */
    @JvmStatic
    fun getFileWriter(path: Path, append: Boolean): FileWriter? =
        getFileWriter(path.toFile(), append)

    /**
     * Gets a FileWriter for the specified file.
     * @param file The file to write to.
     * @param append Whether to append to the file.
     * @return A FileWriter instance or null if an error occurs.
     */
    @JvmStatic
    fun getFileWriter(file: File, append: Boolean): FileWriter? =
        try {
            FileWriter(file, append)
        } catch (_: IOException) {
            null
        }

    /**
     * Loads an FXML file and sets its controller.
     * @param controller The controller to set for the FXML.
     * @return The loaded Parent node.
     */
    @JvmStatic
    fun loadFxml(controller: AbstractController): Parent {
        val logger = Logger.getInstance()
        val path = controller.fxmlPath

        val uiElementPath = path.replace("/fxml/", "").replace(".fxml", "")
        logger.addDebug("Loading [%s] pane...", uiElementPath)

        val parent: Parent? = 
            try {
                val path = requireNotNull(Utils::class.java.getResource(path)) { "Missing FXML resource: $path" }
                FXMLLoader(path)
                    .apply { setController(controller) }
                    .load()
            } catch (e: IOException) {
                logger.addError(e)
                null
            }

        val outcome = if (parent != null) "Success" else "Error"
        logger.addDebug("%s [%s]", outcome, uiElementPath)

        if (parent != null) return parent

        // Since it's a one-time error, just create it during the error process
        val errMsg = ObservableResourceFactory.getInstance().getValue("ui_error")
        val alert = Alert(AlertType.ERROR, errMsg, ButtonType.YES, ButtonType.NO)
        setDefaultButton(alert, ButtonType.NO)

        alert.showAndWait()
        if (alert.result == ButtonType.YES) {
            Thread.startVirtualThread {
                try {
                    Desktop.getDesktop().open(logger.loggingPath.toFile())
                } catch (e: IOException) {
                    logger.addError(e)
                }
            }
        }

        Platform.exit() // Exit gracefully (saves data, etc.)
        return Pane() // return non-null dummy pane
    }

    /**
     * Schedules [action] on the JavaFX Application Thread when the toolkit is running,
     * or executes it synchronously when it is not (e.g. in unit-test environments).
     *
     * Returns a [CompletableFuture] that completes once the action has run, so callers
     * can detect (and propagate) any exception thrown by the action.
     */
    @JvmStatic
    fun <T> runOnFx(action: Callable<T>): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        val wrappedAction = Runnable {
            try {
                future.complete(action.call())
            } catch (t: Throwable) {
                future.completeExceptionally(t)
            }
        }

        if (Platform.isFxApplicationThread()) {
            wrappedAction.run()
        } else {
            try {
                Platform.runLater(wrappedAction)
            } catch (_: IllegalStateException) {
                // JavaFX toolkit not initialized (test environment) — run synchronously
                wrappedAction.run()
            }
        }

        return future
    }

    /**
     * Schedules [action] on the JavaFX Application Thread when the toolkit is running,
     * or executes it synchronously when it is not (e.g. in unit-test environments).
     *
     * Returns a [CompletableFuture] that completes once the action has run, so callers
     * can detect (and propagate) any exception thrown by the action.
     */
    @JvmStatic
    fun runOnFx(action: Runnable): CompletableFuture<Void?> =
        runOnFx<Void?> {
            action.run()
            null
        }
}
