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

import javafx.scene.paint.Color
import me.gosimple.nbvcxz.Nbvcxz
import me.gosimple.nbvcxz.resources.ConfigurationBuilder
import java.util.*
import kotlin.math.abs
import kotlin.math.ulp

object Utils {
    private val DOUBLE_EPSILON = 1.0.ulp
    private val NBVCXZ: Nbvcxz

    init {
        val config = ConfigurationBuilder()
            .setLocale(Locale.getDefault())
            .createConfiguration()

        NBVCXZ = Nbvcxz(config)
    }

    /**
     * Calculates the strength of a given password using the NBVCXZ library.
     * Ideal gap is from 20 to 50, represented with a linear progress bar with gaps of 1
     * @param password The password to evaluate.
     * @return The calculated strength as a double value.
     */
    @JvmStatic
    fun passwordStrength(password: String): Pair<Double, String?> {
        val result = NBVCXZ.estimate(password)

        val entropy = result.entropy
        val feedback = result.feedback

        val message: String? = feedback?.let { fb ->
            val messBldr = StringBuilder()
            fb.warning?.let {
                messBldr.append("Warning: ").append(it).append('\n')
            }
            fb.suggestion.forEach {
                messBldr.append("Suggestion: ").append(it).append('\n')
            }
            messBldr.toString()
        }

        return entropy to message
    }

    /**
     * Generates a CSS linear gradient string representing password strength.
     * The gradient transitions from red to yellow to lime based on the progress value.
     * @param progress A double value between 0 and 1 representing password strength.
     * @return A CSS linear gradient string.
     * @throws IllegalArgumentException if progress is not between 0 and 1.
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun passwordStrengthGradient(progress: Double): String {
        require(progress in 0.0..1.0) { "Progress must be between 0 and 1, received: $progress" }

        // Start with red
        val gradientStr = StringBuilder("linear-gradient(to right, #f00 0%, ")

        // If progress is more than halfway, add yellow there and interpolate lime
        // Otherwise, interpolate yellow
        val interpolatedColor: Color
        if (progress >= 0.5) {
            gradientStr.append("#ff0 50%, ")
            interpolatedColor = Color.YELLOW.interpolate(Color.LIME, progress * 2 - 1)
        } else {
            interpolatedColor = Color.RED.interpolate(Color.YELLOW, progress * 2)
        }

        //
        gradientStr.append("#")
            .append(toHex(interpolatedColor))
            .append(" 100%)")

        return gradientStr.toString()
    }

    /**
     * Converts a color hash code to a hexadecimal `RRGGBB` string representation.
     * @param color The color.
     * @return A character array representing the hexadecimal color.
     */
    fun toHex(color: Color): String {
        val r: Int = (color.red * 255).toInt()
        val g: Int = (color.green * 255).toInt()
        val b: Int = (color.blue * 255).toInt()
        return "%02X%02X%02X".format(r, g, b)
    }

    /**
     * Compares two double values for equality within a small epsilon range.
     * @param a The first double value.
     * @param b The second double value.
     * @return True if the values are considered equal, false otherwise.
     */
    @JvmStatic
    fun doubleEquals(a: Double, b: Double) = abs(a - b) < DOUBLE_EPSILON
}
