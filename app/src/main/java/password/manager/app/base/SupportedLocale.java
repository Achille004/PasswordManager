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

package password.manager.app.base;

import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerates the supported locales for the application, each associated with
 * a {@link Locale} and a flag image resource path.
 */
@Getter
@RequiredArgsConstructor
public enum SupportedLocale {
    ENGLISH(Locale.ENGLISH, "/images/flags/en.png"),
    ITALIAN(Locale.ITALIAN, "/images/flags/it.png");

    private final Locale locale;
    private final transient Image flagImage;

    private SupportedLocale(Locale locale, String flagResourcePath) {
        this.locale = locale;
        this.flagImage = new Image(getClass().getResourceAsStream(flagResourcePath));
    }

    /**
     * Finds the {@link SupportedLocale} matching the given {@link Locale} by language.
     * Falls back to {@link #ENGLISH} if no match is found.
     */
    public static @NotNull SupportedLocale fromLocale(@NotNull Locale locale) {
        return Arrays.stream(values())
                .filter(sl -> sl.locale.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(ENGLISH);
    }

    /**
     * Returns the default {@link SupportedLocale} based on the system language,
     * falling back to {@link #ENGLISH}.
     */
    public static @NotNull SupportedLocale getDefault() {
        Locale systemLang = Locale.forLanguageTag(Locale.getDefault().getLanguage());
        return fromLocale(systemLang);
    }

    /**
     * Returns an array of all supported {@link Locale}s.
     */
    public static @NotNull Locale[] getLocales() {
        return Arrays.stream(values()).map(SupportedLocale::getLocale).toArray(Locale[]::new);
    }
}
