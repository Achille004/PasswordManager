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

import javafx.animation.KeyValue
import javafx.scene.Node
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Background
import password.manager.lib.Utils.passwordStrength
import password.manager.lib.Utils.passwordStrengthGradient

class ReadablePasswordFieldWithStrSkin @JvmOverloads constructor(
    control: CustomPasswordField,
    initialReadable: Boolean = false
) : ReadablePasswordFieldSkin(control, initialReadable), AnimationAwareControl {
    private val strengthBar: ProgressBar = ProgressBar(0.0)
    private val animContr: AnimationController<String>

    init {
        strengthBar.isManaged = false
        strengthBar.isFocusTraversable = false
        strengthBar.isPickOnBounds = false

        strengthBar.isVisible = true
        strengthBar.background = Background.EMPTY

        strengthBar.toFront()
        children.add(strengthBar)

        val styleFunction: (Double) -> KeyValue? = { prog: Double ->
            val bar: Node? = strengthBar.lookup(".bar")
            bar?.let { KeyValue(it.styleProperty(), "-fx-background-color:${passwordStrengthGradient(prog)};") }
        }

        animContr = AnimationController<String>(
            control.textProperty(),
            PROGRESS_EXTRACTOR,
            strengthBar.progressProperty(),
            styleFunction,
            strengthBar
        )
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)

        val controlW = skinnable.width
        val controlH = skinnable.height

        val barH = 10.0
        val spacing = barH / 4
        strengthBar?.resizeRelocate(spacing / 2, y + controlH + 2 * spacing - barH, controlW - spacing, barH)
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun dispose() {
        animationController?.detach()
        super.dispose()
    }

    ///// ANIMATION AWARE CONTROL METHODS //////
    override val animationController: AnimationController<*>
        get() = animContr

    override fun onListenerDetached() {
        strengthBar.isVisible = false
    }

    override fun onListenerAttached() {
        strengthBar.isVisible = true
    }

    companion object {
        private val PROGRESS_EXTRACTOR: (String) -> Double = { pass: String ->
            val passStr = passwordStrength(pass)
            ((passStr.first - 20.0) / 30.0).coerceIn(0.0, 1.0)
        }
    }
}
