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
package password.manager.app.controllers.extra

import javafx.event.ActionEvent
import javafx.fxml.FXML
import password.manager.app.App
import password.manager.app.controllers.AbstractController
import password.manager.app.singletons.Logger
import java.net.URL
import java.util.*

class EulaController : AbstractController() {
    override fun initialize(location: URL, resources: ResourceBundle) = Logger.getInstance().addDebug("Initializing %s", javaClass.getSimpleName())

    override val fxmlPath = "/fxml/extra/eula.fxml"

    override fun reset() {}

    @FXML
    fun githubFM(event: ActionEvent?) = browse(FM_LINK)

    @FXML
    fun githubSS(event: ActionEvent?) =  browse(SS_LINK)

    private fun browse(uri: String) = App.getAppHostServices().showDocument(uri)

    companion object {
        const val FM_LINK = "https://github.com/Achille004"
        const val SS_LINK = "https://github.com/samustocco"
    }
}