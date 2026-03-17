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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.ObjectNode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import password.manager.app.base.SecurityVersion;
import password.manager.app.base.SortingOrder;
import password.manager.app.base.SupportedLocale;
import password.manager.app.singletons.Logger;

@JsonDeserialize(using = UserPreferences.Deserializer.class)
public final class UserPreferences {

    private static final int DEK_LENGTH = AES.AES_BITS / 8; // 32 bytes
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    private static final int ENC_DEK_LENGTH = DEK_LENGTH + (AES.GCM_TAG_BITS / 8); // 48 bytes

    private final transient ObjectProperty<SupportedLocale> localeProperty;
    private final transient ObjectProperty<SortingOrder> sortingOrderProperty;
    private final transient ObjectProperty<SecurityVersion> securityVersionProperty;

    // Serialized fields
    private final @JsonProperty("pwEncDek") byte[] pwEncDek;
    private final @JsonProperty("pwSalt") byte[] pwSalt;
    private final @JsonProperty("pwIv") byte[] pwIv;

    // In-memory only — never serialized
    private transient byte[] dek;
    private transient byte[] legacyHashedPassword;
    private transient byte[] legacySalt;
    private transient SecurityVersion legacySecurityVersion;

    private boolean isPasswordSet;

    public UserPreferences() {
        this.localeProperty = new SimpleObjectProperty<>(SupportedLocale.DEFAULT);
        this.sortingOrderProperty = new SimpleObjectProperty<>(SortingOrder.SOFTWARE);
        this.securityVersionProperty = new SimpleObjectProperty<>(SecurityVersion.LATEST);

        this.pwEncDek = new byte[ENC_DEK_LENGTH];
        this.pwSalt = new byte[SALT_LENGTH];
        this.pwIv = new byte[IV_LENGTH];

        this.dek = null;
        this.legacyHashedPassword = null;
        this.legacySalt = null;
        this.legacySecurityVersion = null;

        this.isPasswordSet = false;
    }

    public UserPreferences(@NotNull String password) {
        this();
        setPassword(password);
    }

    /** New DEK-based format — called by {@link Deserializer}. */
    private UserPreferences(
            @NotNull SupportedLocale locale,
            @NotNull SortingOrder sortingOrder,
            @Nullable SecurityVersion securityVersion,
            @NotNull byte[] pwEncDek,
            @NotNull byte[] pwSalt,
            @NotNull byte[] pwIv) {

        if (locale == null) throw new IllegalArgumentException("Locale cannot be null");
        if (sortingOrder == null) throw new IllegalArgumentException("Sorting order cannot be null");
        if (pwEncDek == null || pwEncDek.length != ENC_DEK_LENGTH) throw new IllegalArgumentException("Encrypted DEK must be exactly " + ENC_DEK_LENGTH + " bytes");
        if (pwSalt == null || pwSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Salt must be exactly " + SALT_LENGTH + " bytes");
        if (pwIv == null || pwIv.length != IV_LENGTH) throw new IllegalArgumentException("IV must be exactly " + IV_LENGTH + " bytes");

        this();

        this.localeProperty.set(locale);
        this.sortingOrderProperty.set(sortingOrder);
        // Default to ARGON2 since this constructor was created for the new format (should never be PBKDF2), but allow possible new versions
        this.securityVersionProperty.set(securityVersion == null ? SecurityVersion.ARGON2 : securityVersion);

        System.arraycopy(pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH);
        System.arraycopy(pwSalt, 0, this.pwSalt, 0, SALT_LENGTH);
        System.arraycopy(pwIv, 0, this.pwIv, 0, IV_LENGTH);

        isPasswordSet = true;
    }

    /**
     * Legacy hash-based format — called by {@link Deserializer} when {@code hashedPassword}
     * is present in JSON instead of {@code pwEncDek}.
     * The instance stays in legacy mode until {@link #verifyPassword} is called
     * with the correct password, at which point it is transparently upgraded.
     */
    @SuppressWarnings("deprecation")
    private UserPreferences(
            @NotNull SupportedLocale locale,
            @NotNull SortingOrder sortingOrder,
            @Nullable SecurityVersion securityVersion,
            @NotNull byte[] hashedPassword,
            @NotNull byte[] salt) {

        if (locale == null) throw new IllegalArgumentException("Locale cannot be null");
        if (sortingOrder == null) throw new IllegalArgumentException("Sorting order cannot be null");
        if (hashedPassword == null || hashedPassword.length == 0) throw new IllegalArgumentException("Hashed password cannot be null or empty");
        if (salt == null || salt.length == 0) throw new IllegalArgumentException("Salt cannot be null or empty");

        this();

        this.localeProperty.set(locale);
        this.sortingOrderProperty.set(sortingOrder);
        // securityVersion may be absent in very old files — fall back to PBKDF2
        this.securityVersionProperty.set(securityVersion == null ? SecurityVersion.PBKDF2 : securityVersion);

        this.legacyHashedPassword = hashedPassword;
        this.legacySalt = salt;
        this.legacySecurityVersion = securityVersion;

        isPasswordSet = true;
    }

    public synchronized void set(@NotNull UserPreferences other) {
        setLocale(other.getLocale());
        setSortingOrder(other.getSortingOrder());
        setSecurityVersion(other.getSecurityVersion());

        System.arraycopy(other.pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH);
        System.arraycopy(other.pwSalt, 0, this.pwSalt, 0, SALT_LENGTH);
        System.arraycopy(other.pwIv, 0, this.pwIv, 0, IV_LENGTH);

        this.dek = (other.dek != null) ? other.dek.clone() : null;
        this.legacyHashedPassword = (other.legacyHashedPassword != null) ? other.legacyHashedPassword.clone() : null;
        this.legacySalt = (other.legacySalt != null) ? other.legacySalt.clone() : null;
        this.legacySecurityVersion = other.legacySecurityVersion;

        this.isPasswordSet = other.isPasswordSet;
    }

    // #region Properties

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

    // #endregion

    // #region Package-private getters for crypto fields

    /**
     * Returns the DEK. This method is visible only to classes in the same package.
     * The returned array is the
     * @return a copy of the DEK, or {@code null} if the password has not been verified yet.
     * @throws IllegalStateException if the DEK is not available (i.e. the password has not been verified yet).
     */
    @JsonIgnore
    synchronized @NotNull byte[] getDEK() {
        if (dek == null) throw new IllegalStateException("DEK is not available until the password has been verified");
        return dek;
    }

    /**
     * Return the legacy security version used for hashing the password in the old format.
     * This method is visible only to classes in the same package.
     * @return the legacy security version, or {@code null} if this instance was not loaded from a legacy file.
     */
    @JsonIgnore
    synchronized @Nullable SecurityVersion getLegacyVersion() {
        return legacySecurityVersion;
    }

    // #endregion

    // #region Password management

    /**
     * Verifies the given password against the stored credential.
     * <p>
     * For instances loaded from the legacy hash-based format, the password is checked
     * against the stored hash; on success the instance is transparently upgraded to the
     * DEK-based format with a freshly generated salt (keeping the same master password).
     * </p>
     * <p>
     * For instances in the current DEK-based format, AES-GCM authentication serves as
     * the implicit password check: if decryption succeeds the DEK is retained in memory
     * and can be retrieved via {@link #getDEK()}.
     * </p>
     *
     * @return {@code true} if the password is correct (or no password has been set),
     *         {@code false} otherwise.
     */
    public synchronized boolean verifyPassword(@NotNull String passwordToVerify) {
        if (!isPasswordSet) throw new IllegalStateException("No password is set");
        if (passwordToVerify == null) return false;

        // Since the legacy hashed password is not nullified after upgrade, we can detect legacy mode by also checking that the DEK is still null.
        if (dek == null && legacyHashedPassword != null) {
            // Legacy mode: verify by comparing hashes
            final byte[] hashedInput = getSecurityVersion().hash(passwordToVerify, legacySalt);
            final boolean match = Arrays.equals(legacyHashedPassword, hashedInput);
            if (match) {
                // Upgrade to DEK format; setPassword generates a new random salt
                setSecurityVersion(SecurityVersion.LATEST);
                setPassword(passwordToVerify);
            }
            return match;
        }

        // DEK-based mode: AES-GCM decryption failure = wrong password
        try {
            this.dek = decryptDEK(getSecurityVersion(), passwordToVerify, pwEncDek, pwSalt, pwIv);
            return true;
        } catch (AEADBadTagException e) {
            return false;
        } catch (GeneralSecurityException e) {
            Logger.getInstance().addError(e);
            throw new RuntimeException("Failed to decrypt DEK", e);
        }
    }

    public synchronized boolean setPasswordVerified(@Nullable String oldPassword, @NotNull String newPassword) {
        // If no password is set yet, we allow setting a new one without verification (first-time setup).
        final boolean canSet = !isPasswordSet || verifyPassword(oldPassword);
        if (canSet) setPassword(newPassword);
        return canSet;
    }

    /**
     * Sets (or changes) the master password.
     * <ul>
     *   <li>If no DEK exists yet (first-time setup), a new random DEK is generated.</li>
     *   <li>Otherwise the existing DEK is re-encrypted under the new password,
     *       so already-encrypted account data remains intact.</li>
     * </ul>
     * A fresh random salt and IV are always generated so the derived key changes
     * even when the same password is reused (important for the legacy → DEK upgrade path).
     */
    private synchronized void setPassword(@NotNull String password) {
        SecureRandom random;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            random = new SecureRandom();
        }

        random.nextBytes(this.pwSalt);
        random.nextBytes(this.pwIv);

        // Reuse existing DEK when changing the password; generate a new one on first setup.
        if (this.dek == null) {
            this.dek = new byte[DEK_LENGTH];
            random.nextBytes(this.dek);
        }

        setSecurityVersion(SecurityVersion.LATEST);

        try {
            byte[] pwEncDek = encryptDEK(SecurityVersion.LATEST, password, this.dek, this.pwSalt, this.pwIv);
            System.arraycopy(pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH);
            this.isPasswordSet = true;
        } catch (GeneralSecurityException e) {
            Logger.getInstance().addError(e);
            throw new RuntimeException("Failed to encrypt DEK", e);
        }
    }

    // #endregion

    // #region Crypto helpers

    /** Encrypt {@code dek} bytes using AES-GCM with a KEK derived from {@code password} + {@code salt}. */
    private static byte[] encryptDEK(SecurityVersion version, String password, byte[] dek, byte[] salt, byte[] iv) throws GeneralSecurityException {
        final byte[] kekBytes = version.getKey(password, salt);
        return AES.encryptAES(dek, kekBytes, iv);
    }

    /**
     * Decrypt the wrapped DEK.  AES-GCM tag verification provides implicit password
     * authentication — a wrong password produces a {@link GeneralSecurityException}.
     */
    private static byte[] decryptDEK(SecurityVersion version, String password, byte[] pwEncDek, byte[] salt, byte[] pwIv) throws GeneralSecurityException {
        final byte[] kekBytes = version.getKey(password, salt);
        return AES.decryptAES(pwEncDek, kekBytes, pwIv);
    }

    // #endregion

    @Contract("_ -> new")
    public static @NotNull UserPreferences of(String password) {
        return password != null ? new UserPreferences(password) : new UserPreferences();
    }

    @Contract("-> new")
    public static @NotNull UserPreferences empty() {
        return new UserPreferences();
    }

    /**
     * Custom Jackson deserializer that handles both the legacy hash-based format
     * (JSON field {@code hashedPassword}) and the current DEK-based format
     * (JSON field {@code pwEncDek}), allowing existing data files to be loaded
     * and transparently upgraded on first authenticated access.
     */
    public static final class Deserializer extends ValueDeserializer<UserPreferences> {
        @Override
        public UserPreferences deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            final ObjectNode node = p.readValueAsTree();

            final SupportedLocale locale = node.has("locale")
                    ? SupportedLocale.forLanguageTag(node.get("locale").asString())
                    : SupportedLocale.DEFAULT;

            final SortingOrder sortingOrder = node.has("sortingOrder")
                    ? SortingOrder.valueOf(node.get("sortingOrder").asString())
                    : SortingOrder.SOFTWARE;

            final SecurityVersion securityVersion = node.has("securityVersion")
                    ? SecurityVersion.fromString(node.get("securityVersion").asString())
                    : null;

            if (node.has("hashedPassword")) {
                final byte[] hashedPassword = node.get("hashedPassword").binaryValue();
                final byte[] salt = node.get("salt").binaryValue();
                return new UserPreferences(locale, sortingOrder, securityVersion, hashedPassword, salt);
            } else {
                final byte[] pwEncDek = node.get("pwEncDek").binaryValue();
                final byte[] pwSalt = node.get("pwSalt").binaryValue();
                final byte[] pwIv = node.get("pwIv").binaryValue();
                return new UserPreferences(locale, sortingOrder, securityVersion, pwEncDek, pwSalt, pwIv);
            }
        }
    }
}