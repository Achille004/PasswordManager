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

package password.manager.lib;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Utility class to manage loading animations for PasswordInputControl elements.
 */
public class LoadingAnimation {
    private static final Duration LOAD_ANIM_TIME_UNIT = Duration.millis(125);

    // Used for both mapping and caching timelines
    private static final Map<Object, ElementWithDisabler> elementMap = new IdentityHashMap<>();
    private static final Map<ElementWithDisabler, Timeline> timelines = new IdentityHashMap<>();

    public static void start(@NotNull Object... elements) {
        for (Object element : elements) start(element);
    }

    public static synchronized void start(@NotNull Object element) {
        if (element == null) throw new IllegalArgumentException("Element cannot be null");
        if (elementMap.containsKey(element)) throw new IllegalStateException("Loading animation is already running for this element");

        ElementWithDisabler wrappedElement;
        try {
            wrappedElement = new ElementWithDisabler(element);
        } catch (IllegalStateException e) {
            return; // Element does not have the required methods, silently ignore
        }

        elementMap.put(element, wrappedElement);

        // Create and cache the timeline for this element, if it has a setText method
        Timeline timeline = createTimeline(wrappedElement);
        if (timeline != null) {
            timelines.put(wrappedElement, timeline);
            timeline.playFromStart();
        }

        wrappedElement.setDisable(true);
        wrappedElement.setReadable(true);
        if(element instanceof AnimationAwareControl aControl) aControl.onListenerDetached();
    }

    public static void stop(@NotNull Object... elements) {
        for (Object element : elements) stop(element);
    }

    public static synchronized void stop(@NotNull Object element) {
        if (element == null) throw new IllegalArgumentException("Element cannot be null");

        ElementWithDisabler wrappedElement = elementMap.remove(element);
        if (wrappedElement == null) throw new IllegalStateException("No loading animation is running for this element");

        // Retrieve and remove the timeline associated with the element, if it exists, and stop it
        Timeline timeline = timelines.remove(wrappedElement);
        if(timeline != null) timeline.stop();

        wrappedElement.setDisable(false);
        wrappedElement.setReadable(false);
        if(element instanceof AnimationAwareControl aControl) aControl.onListenerAttached();
    }

    ///// HELPER METHODS /////

    private static Timeline createTimeline(ElementWithDisabler element) {
        if (element.setTextMethod() == null) return null;

        Timeline passLoadTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, _ -> element.setText("Loading")),
            new KeyFrame(LOAD_ANIM_TIME_UNIT, _ -> element.setText("Loading.")),
            new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(2), _ -> element.setText("Loading..")),
            new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(3), _ -> element.setText("Loading...")),
            new KeyFrame(LOAD_ANIM_TIME_UNIT.multiply(4), _ -> {}) // Wait another time unit before restarting
        );
        passLoadTimeline.setCycleCount(Timeline.INDEFINITE);

        return passLoadTimeline;
    }

    private record ElementWithDisabler(Object element, Method setDisableMethod, Method setTextMethod, Method setReadableMethod) {
        private static final ClassValue<MethodCache> METHOD_CACHE = new ClassValue<>() {
            @Override
            protected MethodCache computeValue(Class<?> type) {
                Method setDisableMethod = findMethod(type, "setDisable", Boolean.TYPE, Boolean.class);
                Method setTextMethod = findMethod(type, "setText", String.class);
                Method setReadableMethod = findMethod(type, "setReadable", Boolean.TYPE, Boolean.class);

                return new MethodCache(setDisableMethod, setTextMethod, setReadableMethod);
            }
        };

        public ElementWithDisabler(Object element) throws IllegalStateException {
            MethodCache methodCache = METHOD_CACHE.get(element.getClass());
            if (!methodCache.hasAnyMethod()) {
                throw new IllegalStateException("Element must have at least one of the following methods: setDisable, setText, or setReadable");
            }

            this(element, methodCache.setDisableMethod(), methodCache.setTextMethod(), methodCache.setReadableMethod());
        }

        public void setDisable(boolean disable) {
            if (setDisableMethod == null) return;

            try {
                setDisableMethod.invoke(element, disable);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to invoke setDisable method", e);
            }
        }

        public void setText(String text) {
            if (setTextMethod == null) return;

            try {
                setTextMethod.invoke(element, text);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to invoke setText method", e);
            }
        }

        public void setReadable(boolean readable) {
            if (setReadableMethod == null) return;

            try {
                setReadableMethod.invoke(element, readable);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to invoke setReadable method", e);
            }
        }

        private static Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
            for (Class<?> parameterType : parameterTypes) {
                try {
                    return type.getMethod(methodName, parameterType);
                } catch (NoSuchMethodException e) {
                    // Try next compatible signature.
                }
            }

            try {
                if (parameterTypes.length == 0) return type.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                // No matching public method found.
            }

            return null;
        }

        private record MethodCache(Method setDisableMethod, Method setTextMethod, Method setReadableMethod) {
            private boolean hasAnyMethod() {
                return setDisableMethod != null || setTextMethod != null || setReadableMethod != null;
            }
        }
    }
}
