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
package password.manager.app.base

import com.fasterxml.jackson.annotation.JsonValue
import javafx.scene.image.Image
import java.util.Arrays
import java.util.Locale
import java.util.Optional
import java.util.function.BiPredicate

import kotlin.jvm.optionals.getOrNull

/**
 * Lists the supported locales for the application, each associated with
 * a [Locale] and a flag image resource path.
 *
 * The serialization of this enum is "transparent", as the actual locale
 * is annotated as a JSON value, so when deserializing from JSON, it will
 * automatically convert the locale tag to the corresponding enum constant.
 */
enum class SupportedLocale(
    @JsonValue val locale: Locale,
    flagResourcePath: String
) {
    ENGLISH(Locale.ENGLISH, "/images/flags/en.png"),
    ITALIAN(Locale.ITALIAN, "/images/flags/it.png");

    @Transient
    val flagImage: Image = Image(javaClass.getResourceAsStream(flagResourcePath)!!)

    companion object {
        private val IS_LOCALE: BiPredicate<SupportedLocale, Locale> = { sl1: SupportedLocale, l2: Locale ->
            val l1: Locale = sl1.locale
            l1.language.equals(l2.language)
        }

        @JvmField
        val DEFAULT: SupportedLocale = forLocaleOptional(Locale.getDefault()).orElse(ENGLISH)!!

        /**
         * Finds the [SupportedLocale] matching the given [Locale] by language.
         * @return the optionally matched [SupportedLocale]
         */
        @JvmStatic
        fun forLocaleOptional(locale: Locale): Optional<SupportedLocale> = Arrays.stream(entries.toTypedArray())
            .filter { IS_LOCALE.test(it, locale) }
            .findFirst()

        /**
         * Finds the [SupportedLocale] matching the given [Locale] by language.
         * @return the matching [SupportedLocale], or `null` if no match is found
         */
        @JvmStatic
        fun forLocale(locale: Locale) = forLocaleOptional(locale).getOrNull()

        /**
         * Shorthand for [forLocale], which accepts a language tag [String] instead of a [Locale] object.
         * @return the matching [SupportedLocale], or [DEFAULT] if no match is found or if the input is invalid
         */
        @JvmStatic
        fun forLanguageTag(languageTag: String): SupportedLocale? {
            val loc = try {
                Locale.forLanguageTag(languageTag)
            } catch (_: IllegalArgumentException) {
                // If the locale string is invalid, return the default locale
                return null
            }

            return forLocale(loc)
        }
    }
}
