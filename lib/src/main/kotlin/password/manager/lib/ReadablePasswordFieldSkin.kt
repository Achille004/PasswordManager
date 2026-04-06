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

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.control.PasswordField
import javafx.scene.control.skin.TextFieldSkin
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent

open class ReadablePasswordFieldSkin @JvmOverloads constructor(
    control: CustomPasswordField,
    initialReadable: Boolean = false
) : TextFieldSkin(control) {
    private val actionIcon = ImageView()
    private val readable: BooleanProperty = SimpleBooleanProperty(initialReadable)

    private val readableListener: ChangeListener<Boolean>

    init {
        actionIcon.isPreserveRatio = true
        actionIcon.isManaged = false
        actionIcon.isFocusTraversable = false
        actionIcon.isPickOnBounds = true

        actionIcon.cursor = Cursor.HAND
        actionIcon.isVisible = true
        actionIcon.toFront()

        actionIcon.image = (if (readable.get()) Icons.HIDDEN else Icons.SHOWING).image
        actionIcon.onMouseClicked = EventHandler { _: MouseEvent -> readable.set(!readable.get()) }
        children.add(actionIcon)

        readableListener =
            ChangeListener {_: ObservableValue<out Boolean>, _: Boolean, isReadable: Boolean ->
                actionIcon.image = (if (isReadable) Icons.HIDDEN else Icons.SHOWING).image
                control.text = control.text // Get-and-Set to trigger masking. Although not pretty, it's the best way to do it.
                control.end()
            }
        readable.addListener(readableListener)
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun maskText(txt: String?): String? {
        val isReadable = readable?.get() ?: false
        return txt
            ?.let { if (skinnable is PasswordField && !isReadable) super.maskText(it) else it }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        val iconSize = snapSizeX(skinnable.height)
        val spacing = (h - iconSize) / 2

        // Adjust the width of the text field to accommodate the icon
        val adjustedW = w - iconSize - spacing / 2
        super.layoutChildren(x, y, adjustedW, h)

        val buttonX = snapPositionX(x + w - iconSize - spacing / 2)
        val buttonY = snapPositionY(y + spacing)

        actionIcon?.resizeRelocate(buttonX, buttonY, iconSize, iconSize)
        actionIcon?.fitWidth = iconSize
        actionIcon?.fitHeight = iconSize
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun dispose() {
        readable?.removeListener(readableListener)
        actionIcon?.onMouseClicked = null
        super.dispose()
    }

    private enum class Icons(path: String) {
        SHOWING("/icons/open-eye.png"),
        HIDDEN("/icons/closed-eye.png");

        val image: Image = Image(javaClass.getResourceAsStream(path)!!)
    }

    ///// CUSTOM PASSWORD FIELD METHODS //////

    fun readableProperty(): BooleanProperty {
        return readable
    }
}
