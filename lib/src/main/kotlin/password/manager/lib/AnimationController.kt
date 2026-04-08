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

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.util.Duration
import password.manager.lib.Utils.doubleEquals
import kotlin.math.roundToInt

/**
 * Registers a generic property to be animated when its source property changes.
 * @param sourceProperty `T` source property to listen to
 * @param progressExtractor function that extracts the [Double] progress value from the `T` source property value
 * @param progressProperty [DoubleProperty] to animate
 * @param styleFunction function that generates the style [KeyValue] based on current [Double] progress
 * @param destinationControl control whose skin will be updated
 */
class AnimationController<T>(
    private val sourceProperty: Property<T>,
    private val progressExtractor: (T) -> Double,
    private val progressProperty: DoubleProperty,
    private val styleFunction: (Double) -> KeyValue?,
    destinationControl: Control
) {
    private val attached = SimpleBooleanProperty()
    private var currentTimeline: Timeline? = null

    init {
        attached.addListener(ChangeListener {_: ObservableValue<out Boolean>, _: Boolean, newValue: Boolean ->
            if (newValue) attach()
            else detach()
        })

        // Trigger the initial update once the ProgressBar skin is ready
        // This is a workaround for the fact that the skin may not be ready immediately
        destinationControl.skinProperty()
            .addListener(ChangeListener {_: ObservableValue<out Skin<*>>?, _: Skin<*>?, newSkin: Skin<*>? ->
                if (attached.get()) handleChange(sourceProperty, null, sourceProperty.getValue())
            })

        attach()
    }

    fun attach() {
        sourceProperty.addListener(this::handleChange)
    }

    fun detach() {
        sourceProperty.removeListener(this::handleChange)
        currentTimeline?.stop()
    }

    private fun handleChange(obs: ObservableValue<out T>, old: T?, newVal: T?) {
        // Don't need to remove since it will be replaced
        currentTimeline?.stop()

        val timeline: Timeline? = newVal
            ?.let { progressExtractor(it) }
            ?.let { genTimeline(progressProperty, it, styleFunction) }

        currentTimeline = timeline
        currentTimeline?.playFromStart()
    }

    companion object {
        // Number of keyframes for the animation (200ms at ~60fps)
        private const val ANIM_DURATION_MS = 200
        private const val ANIM_FPS = 60
        private val ANIM_KEYFRAMES: Int = (ANIM_FPS * ANIM_DURATION_MS / 1000f).roundToInt()
        private val ANIM_FRAME_TIME: Duration = Duration.millis(ANIM_DURATION_MS.toDouble() / ANIM_KEYFRAMES)

        /**
        * Provides a timeline for animating a double property of the while also updating its style.
        * The animation will target to about `ANIM_FPS` fps over `ANIM_DURATION_MS` milliseconds.
        *
        * @param property [DoubleProperty] to animate
        * @param destination destination value of `property`
        * @param styleFunction function that generates the style [KeyValue] based on current `property` progress
        * @return the generated [Timeline], or `null` if no animation is needed
        */
        private fun genTimeline(property: DoubleProperty, destination: Double, styleFunction: (Double) -> KeyValue?): Timeline? {
            val start: Double = property.get()
            if (doubleEquals(start, destination)) return null

            val step = (destination - start) / ANIM_KEYFRAMES
            val keyFrames = (1..ANIM_KEYFRAMES).map { i ->
                val progress = (start + step * i).coerceIn(0.0, 1.0)
                KeyFrame(
                    ANIM_FRAME_TIME.multiply(i.toDouble()),
                    KeyValue(property, progress),
                    styleFunction(progress)
                )
            }

            return Timeline(*keyFrames.toTypedArray())
        }
    }
}