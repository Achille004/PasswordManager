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
    private static final Map<PasswordInputControl, Timeline> timelines = new IdentityHashMap<>();

    public static <T extends PasswordInputControl> void start(@NotNull T element) {
        Timeline timeline = timelines.get(element);

        if(timeline == null) {
            timeline = createTimeline(element);
            timelines.put(element, timeline);    
        } else if(timeline.getStatus() == Timeline.Status.RUNNING) {
            return;
        }

        element.setDisable(true);
        element.setReadable(true);
        if(element instanceof AnimationAwareControl aControl) aControl.onListenerDetached();

        timeline.playFromStart();
    }

    public static <T extends PasswordInputControl> void stop(@NotNull T element) {
        Timeline timeline = timelines.get(element);
        if(timeline == null) return;

        timeline.stop();

        element.setDisable(false);
        element.setReadable(false);
        if(element instanceof AnimationAwareControl aControl) aControl.onListenerAttached();
    }

    ///// HELPER METHODS /////

    private static Timeline createTimeline(PasswordInputControl element) {
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
}
