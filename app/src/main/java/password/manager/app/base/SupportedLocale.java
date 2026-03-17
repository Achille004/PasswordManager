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

package password.manager.app.base;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerates the supported locales for the application, each associated with
 * a {@link Locale} and a flag image resource path.
 * <p>
 * The serialization of this enum is "transparent", as the actual locale
 * is annotated as json value, so when deserializing from JSON it will
 * automatically convert the locale tag to the corresponding enum constant.
 */
@Getter
@RequiredArgsConstructor
public enum SupportedLocale {
    ENGLISH(Locale.ENGLISH, "/images/flags/en.png"),
    ITALIAN(Locale.ITALIAN, "/images/flags/it.png");

    private static final Comparator<Locale> LOCALE_COMPARATOR = Comparator.comparing(Locale::getLanguage);

    public static final SupportedLocale DEFAULT = Arrays.stream(values())
            .filter(sl -> LOCALE_COMPARATOR.compare(sl.locale, Locale.getDefault()) == 0)
            .findFirst()
            .orElse(ENGLISH);

    private final @JsonValue Locale locale;
    private final transient Image flagImage;

    private SupportedLocale(Locale locale, String flagResourcePath) {
        this.locale = locale;
        this.flagImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(flagResourcePath)));
    }

    /**
     * Finds the {@link SupportedLocale} matching the given {@link Locale} by language.
     * @return the matching {@link SupportedLocale}, or {@link #DEFAULT} if no match is found
     */
    public static @NotNull SupportedLocale forLocale(@NotNull Locale locale) {
        return Arrays.stream(values())
                .filter(sl -> LOCALE_COMPARATOR.compare(sl.locale, locale) == 0)
                .findFirst()
                .orElse(DEFAULT);
    }

    /**
     * Shorthand for {@link #forLocale}, which accepts a language tag string instead of a {@link Locale} object.
     * @return the matching {@link SupportedLocale}, or {@link #DEFAULT} if no match is found or if the input is invalid
     */
    public static @NotNull SupportedLocale forLanguageTag(@NotNull String languageTag) {
        try {
            Locale l = Locale.forLanguageTag(languageTag);
            return forLocale(l);
        } catch (IllegalArgumentException e) {
            // If the locale string is invalid, return the default locale
            return DEFAULT;
        }
    }
}
