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
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Ideal gap is from 20 to 50, represented with linear progress bar with gaps of 1
    public static double passwordStrength(@Nullable String password) {
        return password != null ? NBVCXZ.estimate(password).getEntropy() : 0d;
    }

    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0 || progress > 1) throw new IllegalArgumentException("Progress must be between 0 and 1");

        StringBuilder gradientStr = new StringBuilder("linear-gradient(to right, #f00 0%, ");

        Color interpolatedColor;
        if (progress >= 0.5) {
            gradientStr.append("#ff0 50%, ");
            interpolatedColor = Color.YELLOW.interpolate(Color.LIME, progress * 2 - 1);
        } else {
            interpolatedColor = Color.RED.interpolate(Color.YELLOW, progress * 2);
        }
        
        gradientStr.append("#")
                .append(toHex(interpolatedColor.hashCode()))
                .append(" 100%)");

        return gradientStr.toString();
    }

    public static char[] toHex(int colorHashCode) {
        colorHashCode >>= 8;
        char[] hexChars = new char[6];
        for (int j = 5; j >= 0; j--) {
            hexChars[j] = hexArray[colorHashCode & 0xF];
            colorHashCode >>= 4;
        }
        return hexChars;
    }
}
