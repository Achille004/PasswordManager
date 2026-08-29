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

import javafx.application.Application
import javafx.application.HostServices
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.stage.Stage
import password.manager.app.controllers.AbstractController
import password.manager.app.controllers.FirstRunController
import password.manager.app.controllers.LoginController
import password.manager.app.controllers.MainController
import password.manager.app.singletons.IOManager
import password.manager.app.singletons.Logger
import password.manager.app.singletons.ObservableResourceFactory
import password.manager.app.singletons.Singletons

class App : Application() {
    override fun start(primaryStage: Stage) {
        appHostServices = hostServices
        appScenePane = AnchorPane()
        appParameters = parameters

        appScenePane!!.setMinSize(MIN_WIDTH.toDouble(), MIN_HEIGHT.toDouble())
        appScenePane!!.stylesheets.addAll(ROOT_STYLESHEET, AUTOCOMPLETION_STYLESHEET)

        primaryStage.title = APP_NAME
        primaryStage.icons.add(Image(MAIN_ICON))
        primaryStage.setOnCloseRequest { Platform.exit() }
        primaryStage.setScene(Scene(appScenePane, MIN_WIDTH.toDouble(), MIN_HEIGHT.toDouble()))

        startApp()
        primaryStage.show()

        // Set actual 900x600 as stage sizes contain also window decorations
        primaryStage.minWidth = primaryStage.width
        primaryStage.minHeight = primaryStage.height
        primaryStage.isResizable = true
    }

    private fun startApp() {
        // Start up background services
        val iOManager = IOManager.getInstance()

        val locale = iOManager.userPreferences.localeProperty()
        ObservableResourceFactory.getInstance().bindLocaleProperty(locale)

        val switchToMain = switchToMainFactory

        val list = appParameters!!.raw
        Logger.getInstance().addDebug("Found %d parameters", list.size)
        if (!iOManager.isFirstRun && list.size > 1 && ("-p" == list[0] || "--password" == list[0])) {
            Logger.getInstance().addInfo("Trying to authenticate via arguments")
            if (iOManager.authenticate(list[1])) {
                Logger.getInstance().addInfo("Correct password, skipping login")
                switchToMain.set(true)
                return  // Exit early
            }

            Logger.getInstance().addInfo("Incorrect password, redirecting to login")
        }

        val pane = Utils.loadFxml(
            if (iOManager.isFirstRun) FirstRunController(switchToMain)
            else LoginController(switchToMain)
        ) as Pane

        setFullyResizable(pane)

        appScenePane!!.children.apply {
            clear()
            add(pane)
        }
    }

    override fun stop() = Singletons.shutdownAll()

    companion object {
        @JvmField
        val APP_NAME: String = System.getProperty("app.name", "Password Manager")
        @JvmField
        val APP_VERSION: String = System.getProperty("app.version", "3.1.2")

        val ROOT_STYLESHEET: String = App::class.java.getResource("/fxml/css/root.css")!!.toExternalForm()
        val AUTOCOMPLETION_STYLESHEET: String = App::class.java.getResource("/fxml/css/auto-completion.css")!!.toExternalForm()
        val CUSTOMPOPUP_STYLESHEET: String = App::class.java.getResource("/fxml/css/custom-popup.css")!!.toExternalForm()

        // Keep as String to prevent crashing when JavaFX is not available (e.g., during build processes)
        val MAIN_ICON: String = App::class.java.getResource("/icon.png")!!.toExternalForm()

        private const val MIN_WIDTH = 900
        private const val MIN_HEIGHT = 600

        @JvmStatic
        var appHostServices: HostServices? = null
            private set

        @JvmStatic
        var appScenePane: Pane? = null
            private set

        @JvmStatic
        var appParameters: Parameters? = null
            private set

        private val switchToMainFactory: BooleanProperty
            get() = SimpleBooleanProperty(false).apply {
                addListener { _, _, newValue: Boolean? ->
                    if (newValue != true) return@addListener

                    val mainController = MainController()
                    val mainPane = Utils.loadFxml(mainController) as Pane

                    setFullyResizable(mainPane)

                    appScenePane!!.children.clear()
                    appScenePane!!.children.add(mainPane)
                    mainController.mainTitleAnimation()
                }
            }

        private fun setFullyResizable(child: Node?) {
            AnchorPane.setTopAnchor(child, 0.0)
            AnchorPane.setBottomAnchor(child, 0.0)
            AnchorPane.setLeftAnchor(child, 0.0)
            AnchorPane.setRightAnchor(child, 0.0)
        }

        @JvmStatic
        fun main(args: Array<String>) = launch(*args)
    }
}
