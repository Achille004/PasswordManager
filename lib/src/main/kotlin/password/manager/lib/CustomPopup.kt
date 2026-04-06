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

import javafx.animation.Animation
import javafx.animation.FadeTransition
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.stage.Popup
import javafx.stage.Window
import javafx.util.Duration
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * CustomPopup is a utility class for creating customizable popups in JavaFX applications.
 * It provides a builder pattern for easy construction and configuration of the popup, including options for alignment, spacing, fading animations, and stylesheets.
 * The popup can be shown or hidden with optional animations, and its state can be updated to display different messages and colors.
 */
class CustomPopup private constructor(
    private val owner: Window,
    private val alignment: Alignment,
    private val spacing: Double
) : Popup() {
    private val content: Content

    private var disappearTransition: FadeTransition? = null

    init {
        this.content = Content()
        setup()
    }

    /**
     * Makes the popup visible. If stopAnimation is true, any ongoing disappear animation will be stopped and the popup will become fully visible immediately.
     * @param stopAnimation Whether to stop any ongoing disappear animation while making the popup visible, defaults to true.
     */
    @JvmOverloads
    fun visible(stopAnimation: Boolean = true) {
        if (stopAnimation && disappearTransition != null) disappearTransition!!.stop()
        content.setVisible(owner.isFocused)
        content.setOpacity(1.0)
    }

    /**
     * Hides the popup. If playAnimation is true and a disappear animation is set, the animation will be played before the popup is hidden.
     * @param playAnimation Whether to play the disappear animation (if set) while hiding the popup.
     */
    @JvmOverloads
    fun hidden(playAnimation: Boolean = true) {
        if (playAnimation && disappearTransition != null) disappearTransition!!.playFromStart()
        else content.setVisible(false)
    }

    /**
     * Sets the state of the popup, including the displayed text and the color of the bottom bar. This can be used to indicate different states such as success or error.
     * @param text The text to display in the popup.
     * @param bottomBarColor The color to set for the bottom bar, specified as a CSS color value (e.g. "-fx-color-green" or "#00FF00").
     */
    fun setState(text: String, bottomBarColor: String) {
        content.setState(text, bottomBarColor)
    }

    // Used by constructor
    private fun setup() {
        super.content.add(content)
        // DON'T EVEN TRY REMOVING THESE PROPERTIES, TWO HOURS OF MY LIFE WASTED!!
        isAutoHide = false
        isHideOnEscape = false

        // Set position when height is first computed
        content.heightProperty()
            .addListener(ChangeListener { `_`: ObservableValue<out Number>, oldValue: Number, newValue: Number ->
                // Only listen when height is first set
                if (oldValue.toDouble() != 0.0 || newValue.toDouble() <= 0) return@ChangeListener

                // Get dimensions for position calculations
                val width: Double = content.getWidth()
                val height: Double = content.getHeight()

                // Define position setters that can be reused for both initial set and listeners
                val setX = { newX: Double, newWidth: Double ->
                    x = alignment.calcX(
                        newX,
                        newWidth,
                        width,
                        spacing
                    )
                }
                val setY = { newY: Double, newHeight: Double ->
                    y = alignment.calcY(
                        newY,
                        newHeight,
                        height,
                        spacing
                    )
                }

                // Initial set
                setX(owner.x, owner.width)
                setY(owner.y, owner.height)

                // Update position when owner moves or resizes
                owner.xProperty()
                    .addListener(ChangeListener { _: ObservableValue<out Number>, _: Number, newX: Number ->
                        setX(newX.toDouble(), owner.width)
                    })
                owner.yProperty()
                    .addListener(ChangeListener { _: ObservableValue<out Number>, _: Number, newY: Number ->
                        setY(newY.toDouble(), owner.height)
                    })

                // Update position when owner resizes
                owner.widthProperty()
                    .addListener(ChangeListener { _: ObservableValue<out Number>, _: Number, newWidth: Number ->
                        setX(owner.x, newWidth.toDouble())
                    })
                owner.heightProperty()
                    .addListener(ChangeListener { _: ObservableValue<out Number>, _: Number, newHeight: Number ->
                        setY(owner.y, newHeight.toDouble())
                    })
            })

        // Track focused state to prevent unwanted popup visibility changes
        owner.focusedProperty()
            .addListener(ChangeListener { _: ObservableValue<out Boolean>, _: Boolean, isFocused: Boolean ->
                val isVisible: Boolean = content.isVisible()
                val shouldShow = disappearTransition?.status == Animation.Status.RUNNING

                when {
                    // Window lost focus while popup is visible - hide it
                    isVisible && !isFocused -> content.setVisible(false)
                    // Window gained focus while popup is hidden - show it
                    !isVisible && isFocused && shouldShow -> content.setVisible(true)
                }
            })
    }

    // Used by builder
    private fun setFadingAnimation(fadeDuration: Duration) {
        val transition = FadeTransition(fadeDuration, content)
        transition.fromValue = 1.0
        transition.toValue = 0.0
        transition.cycleCount = 1
        transition.onFinished = EventHandler { _: ActionEvent -> content.setVisible(false) }

        this.disappearTransition = transition
    }

    // Used by builder
    private fun ready() {
        // This ensures that everything is actually computed
        content.applyCss()
        content.layout()
        super.show(owner)
    }

    /**
     * Builder class for constructing a CustomPopup with a fluent interface.
     * Use [Builder.create(owner, alignment, spacing)][Builder.create] to start building a CustomPopup.
     */
    class Builder private constructor(
        owner: Window,
        alignment: Alignment,
        spacing: Double
    ) {
        private val popup: CustomPopup = CustomPopup(owner, alignment, spacing)

        /**
         * Sets a fading animation to be played when the popup is hidden. The animation will fade the popup out over the specified duration.
         * @param fadeDuration The duration of the fade-out animation.
         * @return The Builder instance, for chaining.
         * @throws IllegalArgumentException if the duration is less than or equal to zero
         */
        @Throws(IllegalArgumentException::class)
        fun withFadingAnimation(fadeDuration: Duration): Builder {
            require(!fadeDuration.lessThanOrEqualTo(Duration.ZERO)) { "Fade duration must be greater than zero" }
            popup.setFadingAnimation(fadeDuration)
            return this
        }

        /**
         * Adds the specified stylesheets to the popup's content. These stylesheets will be applied to the popup and can be used to customize its appearance.
         * @param stylesheets One or more stylesheet paths to add to the popup's content. These should be valid paths to CSS files.
         * @return The Builder instance, for chaining.
         * @throws IllegalArgumentException if no stylesheet was provided, or some is empty
         */
        @Throws(IllegalArgumentException::class)
        fun withStylesheets(vararg stylesheets: String): Builder {
            require(stylesheets.isNotEmpty()) { "Stylesheets array cannot be empty" }
            stylesheets.forEach { require(it.isNotEmpty()) { "Stylesheet cannot be empty" } }

            popup.content.getStylesheets().addAll(stylesheets)
            return this
        }

        /**
         * Finishes building the CustomPopup and returns the constructed instance.
         * This will also prepare the popup for display by computing its layout and showing it to ensure all dimensions are set.
         * @return The constructed CustomPopup instance.
         */
        fun build(): CustomPopup {
            popup.ready()
            return popup
        }

        companion object {
            /**
             * Starts building a CustomPopup with the specified owner, alignment, and spacing.
             * @param owner The window that the popup will be attached to.
             * @param alignment The alignment of the popup relative to the owner window.
             * @param spacing The spacing in pixels between the popup and the edges of the owner window, as determined by the alignment.
             * @return A new Builder instance.
             * @throws IllegalArgumentException if the space is negative
             */
            @JvmStatic
            @Throws(IllegalArgumentException::class)
            fun create(owner: Window, alignment: Alignment, spacing: Double): Builder {
                require(spacing >= 0) { "Spacing cannot be negative" }
                return Builder(owner, alignment, spacing)
            }
        }
    }

    /**
     * Defines the alignment of the popup relative to its owner window.
     * Each alignment option specifies how the popup should be positioned based on the owner's dimensions and the specified spacing.
     */
    enum class Alignment(
        private val calculatorX: (Double, Double, Double, Double) -> Double,
        private val calculatorY: (Double, Double, Double, Double) -> Double,
    ) {
        TOP_LEFT(::forwards, ::forwards),
        TOP_RIGHT(::backwards, ::forwards),
        BOTTOM_LEFT(::forwards, ::backwards),
        BOTTOM_RIGHT(::backwards, ::backwards);

        /**
         * Calculates the X coordinate for the popup based on the owner's position and dimensions, the popup's dimensions, and the specified spacing.
         * @param start The starting X coordinate of the owner window.
         * @param offset The width of the owner window.
         * @param itemSize The width of the popup.
         * @param spacing The spacing in pixels between the popup and the edge of the owner window, as determined by the alignment.
         * @return The calculated X coordinate for the popup.
         */
        fun calcX(start: Double, offset: Double, itemSize: Double, spacing: Double): Double {
            return calculatorX(start, offset, itemSize, spacing)
        }

        /**
         * Calculates the Y coordinate for the popup based on the owner's position and dimensions, the popup's dimensions, and the specified spacing.
         * @param start The starting Y coordinate of the owner window.
         * @param offset The height of the owner window.
         * @param itemSize The height of the popup.
         * @param spacing The spacing in pixels between the popup and the edge of the owner window, as determined by the alignment.
         * @return The calculated Y coordinate for the popup.
         */
        fun calcY(start: Double, offset: Double, itemSize: Double, spacing: Double): Double {
            return calculatorY(start, offset, itemSize, spacing)
        }
    }

    private class Content : AnchorPane(), Initializable {
        @FXML
        private var label: Label? = null

        @FXML
        private var bottomBar: AnchorPane? = null

        override fun initialize(location: URL?, resources: ResourceBundle?) {
            checkNotNull(label) { "fx:id=\"label\" was not injected." }
            checkNotNull(bottomBar) { "fx:id=\"bottomBar\" was not injected." }
        }

        init {
            val fxmlLoader = FXMLLoader(requireNotNull(javaClass.getResource("/customPopup/index.fxml")))
            fxmlLoader.setRoot(this)
            fxmlLoader.setController(this)
            fxmlLoader.classLoader = javaClass.classLoader

            try {
                fxmlLoader.load<Any?>()
            } catch (exception: IOException) {
                throw RuntimeException(exception)
            }
        }

        // Sets the text and color of the popup. This is used by the CustomPopup to show or hide the popup.
        fun setState(text: String, bottomBarColor: String) {
            label?.text = text
            bottomBar?.style = "-fx-background-color: $bottomBarColor;"
        }
    }
}

// Calculates the coordinate for the popup when aligned forwards (i.e. TOP_LEFT or BOTTOM_LEFT for X, TOP_LEFT or TOP_RIGHT for Y).
// This is done by adding the specified spacing to the starting coordinate.
private fun forwards(start: Double, offset: Double, itemSize: Double, spacing: Double): Double {
    return start + spacing
}

// Calculates the coordinate for the popup when aligned backwards (i.e. TOP_RIGHT or BOTTOM_RIGHT for X, BOTTOM_LEFT or BOTTOM_RIGHT for Y).
// This is done by starting from the end of the owner window (start + offset) and subtracting the popup's size and the specified spacing.
private fun backwards(start: Double, offset: Double, itemSize: Double, spacing: Double): Double {
    return start + offset - itemSize - spacing
}