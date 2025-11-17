/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

import static password.manager.lib.Utils.*;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.util.Duration;

public class AnimationController<T> {
    // Number of keyframes for the animation (200ms at ~60fps)
    private static final int ANIM_DURATION_MS = 200;
    private static final int ANIM_FPS = 60;
    private static final int ANIM_KEYFRAMES = Math.round(ANIM_FPS * ANIM_DURATION_MS / 1000f);
    private static final Duration ANIM_FRAME_TIME = Duration.millis((double) ANIM_DURATION_MS / ANIM_KEYFRAMES);

    private final Property<T> sourceProperty;
    private final DoubleProperty progressProperty;
    private final Function<T, Double> progressExtractor;
    private final Function<Double, KeyValue> styleFunction;

    private final BooleanProperty attached;

    private Timeline currentTimeline;

    /**
     * Registers a generic property to be animated when its source property changes.
     * @param <T> type of the source property
     * @param sourceProperty {@code T} source property to listen to
     * @param progressExtractor function that extracts the {@link Double} progress value from the {@code T} source property value
     * @param progressProperty {@link DoubleProperty} to animate
     * @param styleFunction function that generates the style {@link KeyValue} based on current {@link Double} progress
     * @param destinationControl control whose skin will be updated
     */
    public AnimationController(Property<T> source, 
                               Function<T, Double> progressExtractor,
                               DoubleProperty progressProperty,
                               Function<Double, KeyValue> styleFunction, 
                               Control destinationControl) {
        this.sourceProperty = source;
        this.progressProperty = progressProperty;
        this.progressExtractor = progressExtractor;
        this.styleFunction = styleFunction;

        attached = new SimpleBooleanProperty();
        attached.addListener((_, _, newValue) -> {
            if(newValue) sourceProperty.addListener(this::handleChange);
            else sourceProperty.removeListener(this::handleChange);
        });

        // Trigger initial update once the ProgressBar skin is ready
        // This is a workaround for the fact that the skin may not be ready immediately
        destinationControl.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin == null) return;
            if(attached.get()) handleChange(sourceProperty, null, sourceProperty.getValue());
        });

        attach();
    }

    public void attach() {
        sourceProperty.addListener(this::handleChange);
    }

    public void detach() {
        sourceProperty.removeListener(this::handleChange);
        if (currentTimeline != null) currentTimeline.stop();
    }

    private void handleChange(ObservableValue<? extends T> obs, T old, T newVal) {
        // Don't need to remove since it will be replaced
        if (currentTimeline != null) currentTimeline.stop();

        currentTimeline = genTimeline(progressProperty, progressExtractor.apply(newVal), styleFunction);
        if (currentTimeline == null) return;

        currentTimeline.playFromStart();
    }

    ///// HELPER METHODS /////

    /**
     * Provides a timeline for animating a dobule property of the while also updating its style.
     * The animation will target to about {@code ANIM_FPS} fps over {@code ANIM_DURATION_MS} milliseconds.
     * 
     * @param property {@link DoubleProperty} to animate
     * @param destination destination value of {@code property}
     * @param styleFunction function that generates the style {@link KeyValue} based on current {@code property} progress
     * @return the generated {@link Timeline}, or {@code null} if no animation is needed
     */
    private static @Nullable Timeline genTimeline(DoubleProperty property, Double destination, Function<Double, KeyValue> styleFunction) {
        double curProg = property.get();
        if(doubleEquals(curProg, destination)) return null;

        KeyFrame[] keyFrames = new KeyFrame[ANIM_KEYFRAMES];
        double step = (destination - curProg) / keyFrames.length;

        for(int i = 0; i < keyFrames.length; i++) {
            curProg = doubleSquash(0d, curProg + step, 1d);
            keyFrames[i] = new KeyFrame(
                ANIM_FRAME_TIME.multiply(i + 1),
                new KeyValue(property, curProg),
                styleFunction.apply(curProg)
            );
        }

        return new Timeline(keyFrames);
    }
}