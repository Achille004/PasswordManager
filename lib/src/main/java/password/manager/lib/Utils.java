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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.paint.Color;
import me.gosimple.nbvcxz.Nbvcxz;

public class Utils {
    private static final Nbvcxz NBVCXZ = new Nbvcxz();

    // Ideal gap is from 20 to 50, represented with linear progress bar with gaps of 1
    public static double passwordStrength(@Nullable String password) {
        return password != null ? NBVCXZ.estimate(password).getEntropy() : 0d;
    }

    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("Progress must be between 0 and 1");
        }
        StringBuilder gradientStr = new StringBuilder("linear-gradient(to right, #f00 0%, ");
        boolean isHalfProgress = progress >= 0.5;

        // .replace("0x", "#") -> change 0x to # for color
        // .replace("ffx", "") -> remove alpha from color, used with x to not remove other ff by accident
        if (isHalfProgress) {
            gradientStr.append("#ff0 50%, ");

            double halfProgress = progress - 0.5;
            gradientStr.append((Color.YELLOW.interpolate(Color.LIME, halfProgress * 2) + "x").replace("0x", "#").replace("ffx", ""));
            gradientStr.append(" 100%");
        } else {
            gradientStr.append((Color.RED.interpolate(Color.YELLOW, progress * 2) + "x").replace("0x", "#").replace("ffx", ""));
            gradientStr.append(" 100%");
        }
        gradientStr.append(")");

        return gradientStr.toString();
    }
}
