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
package password.manager.lib

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        val custom1 = ReadablePasswordField()
        custom1.setPrefSize(400.0, 30.0)
        val custom2 = ReadablePasswordFieldWithStr()
        custom2.setPrefSize(542.0, 70.0)

        val root = AnchorPane()
        root.children.addAll(custom1, custom2)

        AnchorPane.setTopAnchor(custom1, 0.0)
        AnchorPane.setTopAnchor(custom2, 200.0)

        stage.scene= Scene(root)
        stage.title = "Test"
        stage.width = 900.0
        stage.height = 600.0
        stage.show()

        val popup1 = CustomPopup.Builder
            .create(
                root.scene.window,
                CustomPopup.Alignment.BOTTOM_RIGHT,
                10.0
            )
            .build()

        popup1.visible()

        val popup2 = CustomPopup.Builder
            .create(
                root.scene.window,
                CustomPopup.Alignment.TOP_RIGHT,
                20.0
            )
            .build()

        popup2.visible()
    }
}

fun main(args: Array<String>) {
    Application.launch(*args)
}