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

package password.manager.app.security;

import java.security.SecureRandom;
import java.util.Arrays;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import password.manager.app.base.SecurityVersion;
import password.manager.app.base.SortingOrder;
import password.manager.app.base.SupportedLocale;

public final class UserPreferences {

    private static final int SALT_LENGTH = 16;

    private final transient ObjectProperty<SupportedLocale> localeProperty;
    private final transient ObjectProperty<SortingOrder> sortingOrderProperty;
    private final transient ObjectProperty<SecurityVersion> securityVersionProperty;

    private final @JsonProperty("hashedPassword") byte[] hashedPassword;
    private final @JsonProperty("salt") byte[] salt;

    private boolean isPasswordSet;

    public UserPreferences() {
        this.localeProperty = new SimpleObjectProperty<>(SupportedLocale.DEFAULT);
        this.sortingOrderProperty = new SimpleObjectProperty<>(SortingOrder.SOFTWARE);
        this.securityVersionProperty = new SimpleObjectProperty<>(SecurityVersion.LATEST);

        this.salt = new byte[SALT_LENGTH];
        this.hashedPassword = new byte[SecurityVersion.HASH_BITS / 8];
        isPasswordSet = false;
    }

    public UserPreferences(@NotNull String password) {
        this();
        setPassword(password);
    }

    @SuppressWarnings("unused") // Used by Jackson for deserialization
    private UserPreferences(
            @JsonProperty(value = "locale", required = false) @Nullable SupportedLocale locale,
            @JsonProperty(value = "sortingOrder", required = false) @Nullable SortingOrder sortingOrder,
            @JsonProperty(value = "securityVersion", required = false) @Nullable SecurityVersion securityVersion,
            @JsonProperty(value = "hashedPassword", required = true) @NotNull byte[] hashedPassword,
            @JsonProperty(value = "salt", required = true) @NotNull byte[] salt) {

        this();

        this.localeProperty.set(locale == null ? SupportedLocale.DEFAULT : locale);
        this.sortingOrderProperty.set(sortingOrder == null ? SortingOrder.SOFTWARE : sortingOrder);
        // This field has been added since Argon2 was implemented, so if it isn't present we'll assume it's older than that
        this.securityVersionProperty.set(securityVersion == null ? SecurityVersion.PBKDF2 : securityVersion);

        System.arraycopy(hashedPassword, 0, this.hashedPassword, 0, this.hashedPassword.length);
        System.arraycopy(salt, 0, this.salt, 0, this.salt.length);
        isPasswordSet = true;
    }

    public synchronized void set(@NotNull UserPreferences userPreferences) {
        setLocale(userPreferences.getLocale());
        setSortingOrder(userPreferences.getSortingOrder());
        setSecurityVersion(userPreferences.getSecurityVersion());

        System.arraycopy(userPreferences.hashedPassword, 0, this.hashedPassword, 0, this.hashedPassword.length);
        System.arraycopy(userPreferences.salt, 0, this.salt, 0, this.salt.length);

        this.isPasswordSet = userPreferences.isPasswordSet;
    }

    public ObjectProperty<SupportedLocale> localeProperty() {
        return localeProperty;
    }

    @JsonProperty("locale")
    public SupportedLocale getLocale() {
        return localeProperty.get();
    }

    public void setLocale(@NotNull SupportedLocale locale) {
        localeProperty.set(locale);
    }

    public ObjectProperty<SortingOrder> sortingOrderProperty() {
        return sortingOrderProperty;
    }

    @JsonProperty("sortingOrder")
    public SortingOrder getSortingOrder() {
        return sortingOrderProperty.get();
    }

    public void setSortingOrder(@NotNull SortingOrder sortingOrder) {
        sortingOrderProperty.set(sortingOrder);
    }

    public ObjectProperty<SecurityVersion> securityVersionProperty() {
        return securityVersionProperty;
    }

    @JsonProperty("securityVersion")
    public SecurityVersion getSecurityVersion() {
        return securityVersionProperty.get();
    }

    public void setSecurityVersion(@NotNull SecurityVersion securityVersion) {
        securityVersionProperty.set(securityVersion);
    }

    public synchronized @NotNull Boolean verifyPassword(@Nullable String passwordToVerify) {
        if (!isPasswordSet) return true; // No password set, so any password is valid
        if (passwordToVerify == null) return false;

        byte[] hashedPasswordToVerify = getSecurityVersion().hash(passwordToVerify, salt);
        final boolean res = Arrays.equals(hashedPassword, hashedPasswordToVerify);

        if (res && !SecurityVersion.LATEST.equals(getSecurityVersion())) {
            setSecurityVersion(SecurityVersion.LATEST);
            setPassword(passwordToVerify);
        }

        return res;
    }

    public synchronized @NotNull Boolean setPasswordVerified(@Nullable String oldPassword, @NotNull String newPassword) {
        final boolean res = verifyPassword(oldPassword);
        if (res) setPassword(newPassword);
        return res;
    }

    private synchronized void setPassword(@NotNull String password) {
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        final byte[] hashedPassword = getSecurityVersion().hash(password, salt);
        System.arraycopy(hashedPassword, 0, this.hashedPassword, 0, this.hashedPassword.length);
        isPasswordSet = true;
    }

    @Contract("_ -> new")
    public static @NotNull UserPreferences of(String password) {
        return password != null ? new UserPreferences(password) : new UserPreferences();
    }

    @Contract("-> new")
    public static @NotNull UserPreferences empty() {
        return new UserPreferences();
    }
}