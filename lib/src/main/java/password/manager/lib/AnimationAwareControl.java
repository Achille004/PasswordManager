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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Control;
import javafx.util.Duration;

public interface AnimationAwareControl {
    public record ControlPropertyPair<T>(Property <T> property, BooleanProperty listenerBound) {}

    // The pairs are finally mapped, while the timelines can be replaced
    static Map<AnimationAwareControl, ControlPropertyPair<?>> animPairs = new IdentityHashMap<>();
    static Map<Control, Timeline> animTimelines = new IdentityHashMap<>();

    ///// TO-IMPLEMENT METHODS /////

    /**
     * Runs extra steps when stopping listening to source property.
     * This method should not be called directly; use {@link #startListening} instead.
     */
    public void doExtraStopSteps();

    /**
     * Runs extra steps when re-starting listening to source property.
     * This method should not be called directly; use {@link #stopListening} instead.
     */
    public void doExtraStartSteps();

    ///// ANIMATION-AWARE PROPERTY METHODS /////

    /**
     * Registers a generic property to be animated when its source property changes.
     * @param <T> type of the source property
     * @param sourceProperty {@code T} source property to listen to
     * @param progressExtractor function that extracts the {@link Double} progress value from the {@code T} source property value
     * @param progressProperty {@link DoubleProperty} to animate
     * @param styleFunction function that generates the style {@link KeyValue} based on current {@link Double} progress
     * @param destinationControl control whose skin will be updated
     */
    public default <T> void addAnimAwareProperty(
            Property<T> sourceProperty,
            Function<T, Double> progressExtractor, 
            DoubleProperty progressProperty,
            Function<Double, KeyValue> styleFunction,
            Control destinationControl) {
        if(animPairs.containsKey(this))
            throw new IllegalArgumentException("The given control is already registered as animable element.");

        final ChangeListener<T> listener = (_, _, newValue) -> {
            Timeline newTimeline = genTimeline(progressProperty, progressExtractor.apply(newValue), styleFunction);
            if (newTimeline.getKeyFrames().isEmpty()) return;

            // Don't need to remove since it will be replaced
            Timeline previousTimeline = animTimelines.get(destinationControl);
            if (previousTimeline != null) previousTimeline.stop();

            animTimelines.put(destinationControl, newTimeline);
            newTimeline.playFromStart();
        };

        final BooleanProperty listenerBound = new SimpleBooleanProperty();
        listenerBound.addListener((_, _, newValue) -> {
            if(newValue) sourceProperty.addListener(listener);
            else sourceProperty.removeListener(listener);
        });
                
        // Listen for text changes
        listenerBound.set(true);

        // Trigger initial update once the ProgressBar skin is ready
        // This is a workaround for the fact that the skin may not be ready immediately
        destinationControl.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin == null) return;
            if(listenerBound.get()) listener.changed(sourceProperty, null, sourceProperty.getValue());
        });

        // Store the pair before setting up skin listener
        animPairs.put(this, new ControlPropertyPair<>(sourceProperty, listenerBound));
    }

    /**
     * Stops listening to source property.
     * Only affects controls that have registered listeners.
     */
    public default void stopListening() {
        ControlPropertyPair<?> animPair = animPairs.get(this);
        if (animPair != null) {
            // Remove listener from source property
            BooleanProperty listenerBound = animPair.listenerBound();
            if (listenerBound != null) listenerBound.set(false);
            doExtraStopSteps();
        }
    }

    /**
     * Re-starts listening to source property.
     * Only affects controls that have registered using 
     */
    public default void startListening() {
        ControlPropertyPair<?> animPair = animPairs.get(this);
        if (animPair != null) {
            // Re-add listener to source property
            BooleanProperty listenerBound = animPair.listenerBound();
            if (listenerBound == null) return;

            listenerBound.set(true);
            doExtraStartSteps();
        }
    }

    ///// HELPER METHODS /////

    /**
     * Provides a timeline for animating a dobule property of the while also updating its style.
     * Keyframes are 1ms apart, and 200 of them are generated for a smooth transition.
     * 
     * @param dProperty Double property to animate
     * @param destination Destination value of the double property
     * @param styleFunction Function that generates the style {@link KeyValue} based on current progress
     * @return Timeline
     */
    private static @NotNull Timeline genTimeline(DoubleProperty dProperty, Double destination, Function<Double, KeyValue> styleFunction) {
        double curProg = dProperty.get();
        if(destination == curProg) return new Timeline();

        KeyFrame[] keyFrames = new KeyFrame[200];
        double increment = (destination - curProg) / keyFrames.length;

        for (int i = 0; i < keyFrames.length; i++) {
            curProg += increment;

            // Ensure we don't overshoot due to floating point precision
            if(curProg > 1d) curProg = 1d;

            keyFrames[i] = new KeyFrame(Duration.millis(i),
                    new KeyValue(dProperty, curProg),
                    // If the keyframe is null we don't care as it won't be used by the constructor
                    styleFunction.apply(curProg)
            );
        }

        return new Timeline(keyFrames);
    }
}