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

package password.manager.app.security;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import password.manager.app.enums.SortingOrder;
import password.manager.app.security.UserPreferences.UserPreferencesDeserializer;
import password.manager.app.utils.Utils;

@Getter
@JsonDeserialize(using = UserPreferencesDeserializer.class)
public final class UserPreferences {
    private final @JsonIgnore ObjectProperty<Locale> localeProperty;
    private final @JsonIgnore ObjectProperty<SortingOrder> sortingOrderProperty;
    private SecurityVersion securityVersion;
    private byte[] hashedPassword;
    private final byte[] salt;

    public UserPreferences() {
        this.localeProperty = new SimpleObjectProperty<>(Utils.DEFAULT_LOCALE);
        this.sortingOrderProperty = new SimpleObjectProperty<>(SortingOrder.SOFTWARE);

        this.securityVersion = SecurityVersion.ARGON2;

        this.salt = new byte[16];
        this.hashedPassword = null;
    }

    public UserPreferences(@NotNull String password) {
        this();
        setPassword(password);
    }

    private UserPreferences(Locale locale, SortingOrder sortingOrder, SecurityVersion securityVersion, byte[] hashedPassword, byte[] salt) {
        this.localeProperty = new SimpleObjectProperty<>(locale);
        this.sortingOrderProperty = new SimpleObjectProperty<>(sortingOrder);

        this.securityVersion = securityVersion;

        this.hashedPassword = hashedPassword;
        this.salt = salt;
    }

    public Locale getLocale() {
        return localeProperty.get();
    }

    public void setLocale(@NotNull Locale locale) {
        localeProperty.set(locale);
    }

    public SortingOrder getSortingOrder() {
        return sortingOrderProperty.get();
    }

    public void setSortingOrder(@NotNull SortingOrder sortingOrder) {
        sortingOrderProperty.set(sortingOrder);
    }

    public @NotNull @JsonIgnore Boolean isEmpty() {
        return this.hashedPassword == null;
    }

    public @NotNull @JsonIgnore Boolean isLatestVersion() {
        return this.securityVersion == SecurityVersion.ARGON2;
    }

    public @NotNull Boolean verifyPassword(String passwordToVerify) throws InvalidKeySpecException {
        if (hashedPassword == null) {
            return true;
        }

        if (passwordToVerify == null) {
            return false;
        }

        // Bacwards compatibility
        boolean isLatestSecurityVersion = isLatestVersion();
        byte[] hashedPasswordToVerify;
        if (isLatestSecurityVersion) {
            hashedPasswordToVerify = Encrypter.hash(passwordToVerify, salt);
        } else {
            hashedPasswordToVerify = Encrypter.hashOld(passwordToVerify, salt);
        }

        boolean res = Arrays.equals(hashedPassword, hashedPasswordToVerify);
        if (res && !isLatestSecurityVersion) {
            setPassword(passwordToVerify);
        }
        return res;
    }

    public @NotNull Boolean setPasswordVerified(String oldPassword, @NotNull String newPassword) throws InvalidKeySpecException {
        boolean res = verifyPassword(oldPassword);
        if (res) {
            setPassword(newPassword);
        }
        return res;
    }

    private void setPassword(@NotNull String password) {
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        this.securityVersion = SecurityVersion.ARGON2;
        hashedPassword = Encrypter.hash(password, salt);
    }

    @Contract("_ -> new")
    public static @NotNull UserPreferences of(String password) throws InvalidKeySpecException {
        return password != null ? new UserPreferences(password) : new UserPreferences();
    }

    @Contract("-> new")
    public static @NotNull UserPreferences empty() {
        return new UserPreferences();
    }

    protected static class UserPreferencesDeserializer extends StdDeserializer<UserPreferences> {
        protected UserPreferencesDeserializer() {
            super(UserPreferences.class);
        }

        @Override
        public UserPreferences deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);

            Locale locale = Locale.forLanguageTag(node.get("locale").asText());
            SortingOrder sortingOrder = SortingOrder.valueOf(node.get("sortingOrder").asText());
            byte[] hashedPassword = Utils.base64ToByte(node.get("hashedPassword").asText());
            byte[] salt = Utils.base64ToByte(node.get("salt").asText());

            SecurityVersion securityVersion = node.has("securityVersion")
                    ? SecurityVersion.fromString(node.get("securityVersion").asText())
                    : SecurityVersion.PBKDF2;

            return new UserPreferences(locale, sortingOrder, securityVersion, hashedPassword, salt);
        }
    }

    protected enum SecurityVersion {
        PBKDF2, ARGON2;

        public static SecurityVersion fromString(String version) {
            return SecurityVersion.valueOf(version);
        }
    }
}