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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.paint.Color;
import me.gosimple.nbvcxz.Nbvcxz;

public class Utils {
    private Utils() {} // Prevent instantiation

    private static final Nbvcxz NBVCXZ = new Nbvcxz();
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final double DOUBLE_EPSILON = Math.ulp(1.0);

    /**
     * Calculates the strength of a given password using the NBVCXZ library.
     * Ideal gap is from 20 to 50, represented with linear progress bar with gaps of 1
     * @param password The password to evaluate.
     * @return The calculated strength as a double value.
     */
    public static double passwordStrength(@Nullable String password) {
        return password != null ? NBVCXZ.estimate(password).getEntropy() : 0d;
    }

    /**
     * Generates a CSS linear gradient string representing password strength.
     * The gradient transitions from red to yellow to lime based on the progress value.
     * @param progress A double value between 0 and 1 representing password strength.
     * @return A CSS linear gradient string.
     * @throws IllegalArgumentException if progress is not between 0 and 1.
     */
    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0d || progress > 1d) throw new IllegalArgumentException("Progress must be between 0 and 1, received: " + progress);

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

    /**
     * Converts a color hash code to a hexadecimal string representation.
     * @param colorHashCode The hash code of the color.
     * @return A character array representing the hexadecimal color.
     */
    public static char[] toHex(int colorHashCode) {
        colorHashCode >>= 8;
        char[] hexChars = new char[6];
        for (int j = 5; j >= 0; j--) {
            hexChars[j] = HEX_ARRAY[colorHashCode & 0xF];
            colorHashCode >>= 4;
        }
        return hexChars;
    }

    /**
     * Compares two double values for equality within a small epsilon range.
     * @param a The first double value.
     * @param b The second double value.
     * @return True if the values are considered equal, false otherwise.
     */
    public static boolean doubleEquals(double a, double b) {
        return Math.abs(a - b) < DOUBLE_EPSILON;
    }

    /**
     * Clamps an integer value between a lower and upper bound.
     * @param lowerBound The lower bound.
     * @param value The value to clamp.
     * @param upperBound The upper bound.
     * @return The clamped integer value.
     */
    public static int intSquash(int lowerBound, int value, int upperBound) {
        return Math.min(Math.max(value, lowerBound), upperBound);
    }

    /**
     * Clamps a double value between a lower and upper bound.
     * @param lowerBound The lower bound.
     * @param value The value to clamp.
     * @param upperBound The upper bound.
     * @return The clamped double value.
     */
    public static double doubleSquash(double lowerBound, double value, double upperBound) {
        return Math.min(Math.max(value, lowerBound), upperBound);
    }
}
