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
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.util.Duration
import java.lang.reflect.Method
import java.util.*

/**
 * Utility class to manage loading animations for CustomPasswordField elements.
 */
object LoadingAnimation {
    private val LOAD_ANIM_TIME_UNIT: Duration = Duration.millis(125.0)

    // Used for both mapping and caching timelines
    private val elementMap: MutableMap<Any, ElementWithDisabler> = IdentityHashMap<Any, ElementWithDisabler>()
    private val timelines: MutableMap<ElementWithDisabler, Timeline> = IdentityHashMap<ElementWithDisabler, Timeline>()

    @JvmStatic
    fun start(vararg elements: Any) = elements.forEach(LoadingAnimation::start)

    @Synchronized
    @JvmStatic
    fun start(element: Any) {
        check(!elementMap.containsKey(element)) { "Loading animation is already running for this element" }

        val wrappedElement =
            try {
                ElementWithDisabler(element)
            } catch (_: IllegalStateException) {
                return  // Element does not have the required methods, silently ignore
            }

        elementMap[element] = wrappedElement

        // Create and cache the timeline for this element if it has a setText method
        createTimeline(wrappedElement) ?.let {
            timelines[wrappedElement] = it
            it.playFromStart()
        }

        wrappedElement.callDisable(true)
        wrappedElement.callReadable(true)
        if (element is AnimationAwareControl) element.onListenerDetached()
    }

    @JvmStatic
    fun stop(vararg elements: Any) = elements.forEach(LoadingAnimation::stop)

    @Synchronized
    @JvmStatic
    fun stop(element: Any) {
        val wrappedElement: ElementWithDisabler = elementMap.remove(element)!!

        // Retrieve and remove the timeline associated with the element, if it exists, and stop it
        timelines.remove(wrappedElement)?.stop()

        wrappedElement.callDisable(false)
        wrappedElement.callReadable(false)
        if (element is AnimationAwareControl) element.onListenerAttached()
    }

    ///// HELPER METHODS //////
    private fun createTimeline(element: ElementWithDisabler): Timeline? {
        if (!element.canText()) return null
        return Timeline(
            KeyFrame(Duration.ZERO, { _: ActionEvent? -> element.callText("Loading") }),
            KeyFrame(LOAD_ANIM_TIME_UNIT, { _: ActionEvent? -> element.callText("Loading.") }),
            KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(2.0), { _: ActionEvent? -> element.callText("Loading..") }),
            KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(3.0), { _: ActionEvent? -> element.callText("Loading...") }),
            KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(4.0), { _: ActionEvent? -> }) // Wait another time unit before restarting
        ) .also { it.cycleCount = Timeline.INDEFINITE }
    }

    private class ElementWithDisabler (
        val element: Any,
        private val setDisableMethod: Method?,
        private val setTextMethod: Method?,
        private val setReadableMethod: Method?
    ) {
        constructor(element: Any) : this(element, requireMethodCache(element))

        private constructor(element: Any, methodCache: MethodCache) : this(
            element,
            methodCache.setDisableMethod,
            methodCache.setTextMethod,
            methodCache.setReadableMethod
        )

        fun canDisable() = setDisableMethod != null
        fun callDisable(disable: Boolean) = tryInvoke(setDisableMethod, disable)

        fun canText() = setTextMethod != null
        fun callText(text: String) = tryInvoke(setTextMethod, text)

        fun canReadable() = setReadableMethod != null
        fun callReadable(readable: Boolean) = tryInvoke(setReadableMethod, readable)

        private fun tryInvoke(method: Method?, vararg args: Any): Any? =
            try {
                method?.invoke(element, args)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to invoke method", e)
            }

        @JvmRecord
        private data class MethodCache(
            val setDisableMethod: Method?,
            val setTextMethod: Method?,
            val setReadableMethod: Method?
        ) {
            fun hasAnyMethod() = setDisableMethod != null || setTextMethod != null || setReadableMethod != null
        }

        companion object {
            private val METHOD_CACHE: ClassValue<MethodCache> = object : ClassValue<MethodCache>() {
                override fun computeValue(type: Class<*>): MethodCache {
                    val setDisableMethod = findMethod(type, "setDisable", java.lang.Boolean.TYPE, Boolean::class.java)
                    val setTextMethod = findMethod(type, "setText", String::class.java)
                    val setReadableMethod = findMethod(type, "setReadable", java.lang.Boolean.TYPE, Boolean::class.java)

                    return MethodCache(setDisableMethod, setTextMethod, setReadableMethod)
                }
            }

            private fun requireMethodCache(element: Any) = METHOD_CACHE.get(element.javaClass).also {
                check(it.hasAnyMethod()) { "Element must have at least one of the following methods: setDisable, setText, or setReadable" }
            }

            private fun findMethod(type: Class<*>, methodName: String, vararg parameterTypes: Class<*>?): Method? {
                for (parameterType in parameterTypes) {
                    try {
                        return type.getMethod(methodName, parameterType)
                    } catch (_: NoSuchMethodException) {
                        // Try next compatible signature.
                    }
                }

                try {
                    if (parameterTypes.isEmpty()) return type.getMethod(methodName)
                } catch (_: NoSuchMethodException) {
                    // No matching public method found.
                }

                return null
            }
        }
    }
}
