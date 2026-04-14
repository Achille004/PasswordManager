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
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.util.Duration
import password.manager.app.App
import password.manager.app.Utils
import password.manager.app.controllers.main.ManagerController
import password.manager.app.controllers.main.SettingsController
import password.manager.app.singletons.AppConfig
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.IOManager.SaveState
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.lib.CustomPopup
import password.manager.lib.CustomPopup.Builder.Companion.create
import java.awt.Desktop
import java.io.IOException
import java.net.URL
import java.util.LinkedList
import java.util.ResourceBundle
import java.util.function.UnaryOperator

class MainController : AbstractController() {
    private val settingsImage = Image(javaClass.getResourceAsStream("/images/icons/navbar/settings.png")!!)
    private val backImage = Image(javaClass.getResourceAsStream("/images/icons/navbar/back.png")!!)

    private var isSettingsOpen = false
    private var titleAnimation: Timeline? = null

    @FXML
    private var mainPane: BorderPane? = null

    @FXML
    private var psmgTitle: Label? = null

    @FXML
    private var settingsButtonImageView: ImageView? = null

    @FXML
    private var folderButton: Button? = null

    // Keep views cached once they are loaded
    private var managerPane: Pane? = null
    private var settingsPane: Pane? = null
    private var managerController: AbstractController? = null
    private var settingsController: AbstractController? = null

    override fun initialize(location: URL, resources: ResourceBundle) {
        Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Logger.getInstance().addInfo("Unsupported action: Desktop.Action.OPEN")
            folderButton!!.isVisible = false
        }

        titleAnimation = Timeline()
        for (i in TITLE_STAGES.indices) {
            val str: String = TITLE_STAGES[i]
            titleAnimation!!.keyFrames.add(
                KeyFrame(
                    TITLE_ANIM_TIME_UNIT.multiply(i.toDouble()),
                    { _: ActionEvent? -> psmgTitle!!.text = str })
            )
        }

        managerController = ManagerController()
        managerPane = Utils.loadFxml(managerController!!) as Pane

        createAutosavePopup()
        swapOnMainPane(managerController, managerPane!!)
    }

    override val fxmlPath = "/fxml/main.fxml"

    override fun reset() {} // Not needed, will never reset

    fun mainTitleAnimation() {
        psmgTitle!!.text = ""
        titleAnimation!!.play()
    }

    @FXML
    private fun folderNavBarAction(event: ActionEvent?) {
        Thread.startVirtualThread {
            try {
                Desktop.getDesktop().open(AppConfig.getInstance().basePath.toFile())
            } catch (e: IOException) {
                Logger.getInstance().addError(e)
            }
        }
    }

    @FXML
    fun settingsNavBarAction(event: ActionEvent?) {
        // Load lazily
        if (settingsPane == null || settingsController == null) {
            settingsController = SettingsController()
            settingsPane = Utils.loadFxml(settingsController!!) as Pane
        }

        isSettingsOpen = !isSettingsOpen
        if (isSettingsOpen) {
            swapOnMainPane(settingsController, settingsPane!!)
            settingsButtonImageView!!.image = backImage
        } else {
            swapOnMainPane(managerController, managerPane!!)
            settingsButtonImageView!!.image = settingsImage
        }
    }

    private fun <T : AbstractController?> swapOnMainPane(destinationController: T, destinationPane: Pane) {
        // Show selected pane
        mainPane!!.centerProperty().set(destinationPane)
        destinationController!!.reset()
    }

    private fun createAutosavePopup() {
        val spacing = 20.0 // px

        val popup = create(
                App.getAppScenePane().scene.window,
                CustomPopup.Alignment.BOTTOM_LEFT,
                spacing
            )
            .withStylesheets(App.ROOT_STYLESHEET, App.CUSTOMPOPUP_STYLESHEET)
            .withFadingAnimation(Duration.seconds(3.0))
            .build()

        popup.hidden(false) // Start hidden without animation

        val resources = ObservableResourceFactory.getInstance()
        IOManager.getInstance().savingProperty()
            .addListener {_: ObservableValue<out SaveState?>?, _: SaveState?, newValue: SaveState? ->
                val getString = UnaryOperator { i18nKey: String ->
                    val key = "popup.$i18nKey"
                    return@UnaryOperator try {
                        resources.resources.getString(key)
                    } catch (_: Exception) {
                        // Key missing: return key itself
                        key
                    }
                }
                when (newValue) {
                    SaveState.SAVING -> {
                        popup.setState(getString.apply("saving"), "-fx-color-element-bg")
                        popup.visible()
                    }

                    SaveState.SUCCESS -> {
                        popup.setState(getString.apply("success"), "-fx-color-green")
                        popup.hidden()
                    }

                    SaveState.ERROR -> {
                        popup.setState(getString.apply("error"), "-fx-color-red")
                        popup.hidden()
                    }

                    else -> {}
                }
            }
    }

    companion object {
        private val TITLE_ANIM_TIME_UNIT: Duration = Duration.millis(8.0)
        private val TITLE_STAGES: Array<String>

        init {
            val title = App.APP_NAME
            val lower = "abcdefghijklmnopqrstuvwxyz".toCharArray()
            val upper = "ABCDEFGHIJKLMNOPQRTSUVWXYZ".toCharArray()

            val stages = LinkedList<String>()
            val currString = StringBuilder()

            for (c in title.toCharArray()) {
                var isLower: Boolean = Character.isLowerCase(c)
                var isUpper: Boolean = Character.isUpperCase(c)

                // If char can't be upper/lower, just append it
                if (!(isLower || isUpper)) {
                    currString.append(c)
                    continue
                }

                var target: CharArray = if (isLower) lower else upper
                for (i in 0..25) {
                    stages.add(currString.toString() + target[i])
                    if (target[i] == c) break
                }
                currString.append(c)
            }

            TITLE_STAGES = stages.toTypedArray()
        }
    }
}
