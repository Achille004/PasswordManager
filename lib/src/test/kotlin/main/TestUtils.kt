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
package main

import javafx.scene.paint.Color
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import password.manager.lib.Utils

internal class AppTest {
    @Test
    fun testToHex() {
        val colors: Array<String> = arrayOf(
            "0xFFFF0000",
            "0xFF00FF00",
            "0xFF0000FF",
            "0x63F9E7C1"
        )

        colors.forEach {
            val color = Color.web(it)
            assertEquals(
                it.substring(2,8),
                Utils.toHex(color)
            )
        }
    }

    @Test
    fun calcPassStr() {
        val passwords: Array<String> = arrayOf(
            "C",
            "E$",
            "}18",
            "0s(C",
            "oA633=",
            "mZ/66am5",
            "F1/nro1u4Y",
            "5£@>4}7>\$Hv7",
            "2rq8KU*5E!)'*bal"
        )

        passwords.forEach {
            val passStr = Utils.passwordStrength(it)
            println("Strength of '$it': $passStr.first")
        }
    }
}
