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

import javafx.beans.property.BooleanProperty
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.ReadablePasswordFieldWithStr
import java.net.URL
import java.util.ResourceBundle

class FirstRunController(private val switchToMain: BooleanProperty) : AbstractController() {
    @FXML
    private val firstRunTitle: Label? = null

    @FXML
    private val firstRunDescTop: Label? = null

    @FXML
    private val firstRunDescBtm: Label? = null

    @FXML
    private val firstRunTermsCred: Label? = null

    @FXML
    private val firstRunDisclaimer: Label? = null

    @FXML
    private val firstRunPassword: ReadablePasswordFieldWithStr? = null


    @FXML
    private val firstRunCheckBox: CheckBox? = null

    @FXML
    private val firstRunSubmitBtn: Button? = null

    override fun initialize(location: URL, resources: ResourceBundle) {
        Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

        val langResources = ObservableResourceFactory.getInstance()
        langResources.bindTextProperty(firstRunTitle!!, "first_run.title")
        langResources.bindTextProperty(firstRunDescTop!!, "first_run.desc.top")
        langResources.bindTextProperty(firstRunDescBtm!!, "first_run.desc.btm")
        langResources.bindTextProperty(firstRunCheckBox!!, "first_run.check_box")
        langResources.bindTextProperty(firstRunTermsCred!!, "terms_credits")
        langResources.bindTextProperty(firstRunSubmitBtn!!, "lets_go")
        langResources.bindTextProperty(firstRunDisclaimer!!, "first_run.disclaimer")

        firstRunPassword!!.onAction = { _ -> doFirstRun() }
        firstRunPassword.sceneProperty()
            .addListener{ _, _, newScene: Scene? ->
                newScene ?.run { firstRunPassword.requestFocus() }
            }

        ObservableResourceFactory.getInstance().bindPromptTextProperty(firstRunPassword)
    }

    override val fxmlPath =  "/fxml/first_run.fxml"

    override fun reset() {} // Not needed, will never reset

    @FXML
    fun doFirstRun() {
        if (checkTextFields(firstRunPassword!!) && firstRunCheckBox!!.isSelected) {
            IOManager.getInstance().changeMasterPassword(firstRunPassword.text.trim())
            switchToMain.set(true)
        }
    }
}