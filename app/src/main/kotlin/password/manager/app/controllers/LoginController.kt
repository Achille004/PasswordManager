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

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.BooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.util.Duration
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.ReadablePasswordField
import java.net.URL
import java.util.*

class LoginController(private val switchToMain: BooleanProperty) : AbstractController() {
    @FXML
    private val loginTitle: Label? = null

    @FXML
    private val loginPassword: ReadablePasswordField? = null

    @FXML
    private var loginSubmitBtn: Button? = null

    private val wrongPasswordTimeline = Timeline(
        KeyFrame(Duration.ZERO,  { _ ->
            loginSubmitBtn!!.isDisable = true
            loginSubmitBtn!!.style = "-fx-border-color: -fx-color-red"
        }),
        KeyFrame(Duration.seconds(1.0),  { _ ->
            loginSubmitBtn!!.isDisable = false
            clearStyle(loginSubmitBtn!!)
        })
    )

    override fun initialize(location: URL, resources: ResourceBundle) {
        Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

        val langResources = ObservableResourceFactory.getInstance()
        langResources.bindTextProperty(loginTitle!!, "login.title")
        langResources.bindTextProperty(loginSubmitBtn!!, "lets_go")
        // Adding a whole method override just for line would be overkill, so just pass the property
        langResources.bindStringProperty(loginPassword!!.promptTextProperty(), "login.password")

        loginPassword.onAction = { _ -> doLogin() }
        loginPassword.sceneProperty()
            .addListener(ChangeListener { _, _, newScene: Scene? ->
                newScene ?.run { loginPassword.requestFocus() }
            })
    }

    override val fxmlPath =  "/fxml/login.fxml"

    override fun reset() {} // Not needed, will never reset

    @FXML
    fun doLogin() {
        if (checkTextFields(loginPassword!!)) {
            wrongPasswordTimeline.stop()
            IOManager.getInstance().authenticate(loginPassword.text.trim())

            if (IOManager.getInstance().isAuthenticated) {
                switchToMain.set(true)
            } else {
                wrongPasswordTimeline.playFromStart()
            }

            clearTextFields(loginPassword)
        }
    }
}
