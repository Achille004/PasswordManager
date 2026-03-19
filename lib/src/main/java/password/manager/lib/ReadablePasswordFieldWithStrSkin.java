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

import static password.manager.lib.Utils.*;

import java.util.function.Function;

import javafx.animation.KeyValue;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Background;

public class ReadablePasswordFieldWithStrSkin extends ReadablePasswordFieldSkin implements AnimationAwareControl {

    private static final Function<String, Double> PROGRESS_EXTRACTOR = pass -> {
        double passwordStrength = passwordStrength(pass);
        return doubleSquash(0d, (passwordStrength - 20d) / 30d, 1d);
    };

    private final ProgressBar strengthBar;
    private final AnimationController<String> animationController;

    public ReadablePasswordFieldWithStrSkin(CustomPasswordField control) {
        this(control, false);
    }

    public ReadablePasswordFieldWithStrSkin(CustomPasswordField control, boolean initialReadable) {
        super(control, initialReadable);

        this.strengthBar = new ProgressBar(0);

        strengthBar.setManaged(false);
        strengthBar.setFocusTraversable(false);
        strengthBar.setPickOnBounds(false);
        
        strengthBar.setVisible(true);
        strengthBar.setBackground(Background.EMPTY);

        strengthBar.toFront();
        getChildren().add(strengthBar);

        final Function<Double, KeyValue> STYLE_FUNCTION = prog -> {
            Node bar = strengthBar.lookup(".bar");
            if(bar == null) return null;
            return new KeyValue(bar.styleProperty(), "-fx-background-color:" + passwordStrengthGradient(prog) + ";");
        };

        this.animationController = new AnimationController<>(
            control.textProperty(),
            PROGRESS_EXTRACTOR,
            strengthBar.progressProperty(),
            STYLE_FUNCTION,
            strengthBar
        );
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        double controlW = getSkinnable().getWidth();
        double controlH = getSkinnable().getHeight();

        double barH = 10;
        double spacing = barH / 4;
        strengthBar.resizeRelocate(spacing / 2, y + controlH + 2 * spacing - barH, controlW - spacing, barH);
    }

    @Override
    public void dispose() {
        animationController.detach();
        super.dispose();
    }

    ///// ANIMATION AWARE CONTROL METHODS /////

    @Override
    public AnimationController<?> getAnimationController() {
        return animationController;
    }

    @Override
    public void onListenerDetached() {
        strengthBar.setVisible(false);
    }

    @Override
    public void onListenerAttached() {
        strengthBar.setVisible(true);
    }
}
