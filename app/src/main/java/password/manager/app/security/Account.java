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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import password.manager.app.base.SecurityVersion;

public final class Account {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    private final transient ReadWriteLock lock = new ReentrantReadWriteLock(false);
    private final transient Lock readLock = lock.readLock();
    private final transient Lock writeLock = lock.writeLock();

    // JsonProperty is redundant if there are getters
    private final transient ReadOnlyStringWrapper softwareProperty = new ReadOnlyStringWrapper(),
                                                  usernameProperty = new ReadOnlyStringWrapper();

    // Salt for deriving source key
    private @JsonProperty("salt") byte[] salt;

    // Encrypted fields and their salts used for key derivation
    private @JsonProperty("software") byte[] software;
    private final @JsonProperty("sSalt") byte[] sSalt;
    private final @JsonProperty("sIv") byte[] sIv;

    private @JsonProperty("username") byte[] username;
    private final @JsonProperty("uSalt") byte[] uSalt;
    private final @JsonProperty("uIv") byte[] uIv;

    private @JsonProperty("password") byte[] password;
    private @JsonProperty("pSalt") byte[] pSalt;
    private final @JsonProperty("pIv") byte[] pIv;

    public Account() {
        this.salt = new byte[SALT_LENGTH];
        
        this.software = null;
        this.sSalt = new byte[SALT_LENGTH];
        this.sIv = new byte[IV_LENGTH];

        this.username = null;
        this.uSalt = new byte[SALT_LENGTH];
        this.uIv = new byte[IV_LENGTH];

        this.password = null;
        this.pSalt = new byte[SALT_LENGTH];
        this.pIv = new byte[IV_LENGTH];
    }

    public Account(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        this();
        this.setData(securityVersion, data, masterPassword);
    }

    @SuppressWarnings("unused") // Used by Jackson for deserialization
    private Account(
            @JsonProperty(value = "salt", required = true) @Nullable byte[] salt,
            @JsonProperty(value = "software", required = true) @NotNull byte[] software,
            @JsonProperty(value = "sSalt", required = true) @Nullable byte[] sSalt,
            @JsonProperty(value = "sIv", required = true) @NotNull byte[] sIv,
            @JsonProperty(value = "username", required = true) @NotNull  byte[] username,
            @JsonProperty(value = "uSalt", required = true) @NotNull byte[] uSalt,
            @JsonProperty(value = "uIv", required = true) @NotNull byte[] uIv,
            @JsonProperty(value = "password", required = true) @NotNull byte[] password,
            @JsonProperty(value = "pSalt", required = true) @Nullable byte[] pSalt,
            @JsonProperty(value = "pIv", required = true) @NotNull byte[] pIv,
            @JsonProperty(value = "injectedMasterPassword", required = true) @Nullable String injectedMasterPassword) throws GeneralSecurityException {

        this();

        // Here we consider salt as present as this constructor is used only for latest security versions where salt is mandatory
        if (salt.length != SALT_LENGTH) throw new IllegalArgumentException("Salt should be " + SALT_LENGTH + " bytes long");
        if (sSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Software salt should be " + SALT_LENGTH + " bytes long");
        if (sIv.length != IV_LENGTH) throw new IllegalArgumentException("Software IV should be " + IV_LENGTH + " bytes long");
        if (uSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Username salt should be " + SALT_LENGTH + " bytes long");
        if (uIv.length != IV_LENGTH) throw new IllegalArgumentException("Username IV should be " + IV_LENGTH + " bytes long");
        if (pSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Password salt should be " + SALT_LENGTH + " bytes long");
        if (pIv.length != IV_LENGTH) throw new IllegalArgumentException("Password IV should be " + IV_LENGTH + " bytes long");

        // Wrap the provided values into a memento and copy them to this account, centralizing the logic in copyMemento to avoid code duplication
        AccountMemento memento = new AccountMemento(salt, software, sSalt, sIv, username, uSalt, uIv, password, pSalt, pIv);
        copyMemento(memento);
    }

    @Deprecated // This constructor is used only for backward compatibility
    @SuppressWarnings("unused") // Used by Jackson for deserialization
    private Account(
            @JsonProperty(value = "software", required = true) @NotNull String software,
            @JsonProperty(value = "username", required = true) @NotNull  String username,
            @JsonProperty(value = "encryptedPassword", required = true) @NotNull byte[] encryptedPassword,
            @JsonProperty(value = "salt", required = false) @Nullable byte[] salt,
            @JsonProperty(value = "iv", required = true) @NotNull byte[] iv) {

        if (iv.length != IV_LENGTH) throw new IllegalArgumentException("IV should be " + IV_LENGTH + " bytes long");

        this();

        this.software = software.getBytes(StandardCharsets.UTF_8);
        this.softwareProperty.set(software);

        this.username = username.getBytes(StandardCharsets.UTF_8);
        this.usernameProperty.set(username);

        // If salt is not provided, derive it from software and username (backward compatibility:
        // older versions didn't store salt and used software+username bytes as salt instead).
        // IMPORTANT: the derived value is written back into this.pSalt so it is persisted on the
        // next save, at which point the account behaves identically to a modern one.
        this.salt = (salt != null && salt.length == SALT_LENGTH) ? salt.clone() : (software + username).getBytes(StandardCharsets.UTF_8);
        System.arraycopy(iv, 0, this.pIv, 0, this.pIv.length);

        this.password = encryptedPassword;
    }

    public void unlock(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        final AccountData data = getData(securityVersion, masterPassword);

        // Update properties for UI
        softwareProperty.set(data.software());
        usernameProperty.set(data.username());
    }

    /**
     * Gets the software name property for this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the software name property, read-only
     */
    public ReadOnlyProperty<String> softwareProperty() {
        return softwareProperty.getReadOnlyProperty();
    }

    /**
     * Gets the software name associated with this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the software name
     */
    @JsonIgnore
    public String getSoftware() {
        return softwareProperty.get();
    }

    /**
     * Gets the username property for this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the username property, read-only
     */
    public ReadOnlyProperty<String> usernameProperty() {
        return usernameProperty.getReadOnlyProperty();
    }

    /**
     * Gets the username associated with this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the username
     */
    public String getUsername() {
        return usernameProperty.get();
    }

    @JsonIgnore
    public AccountData getData(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        String plainSoftware, plainUsername, plainPassword;

        readLock.lock();
        try {
            byte[] key = securityVersion.getKey(masterPassword, salt);

            // If software or username salt are null, it means that this account was created with
            // an older security version where these fields were not encrypted.
            if(sSalt != null || uSalt != null) {
                // Decrypt all fields
                byte[] sKey = AES.derivateKey(key, sSalt, "software");
                plainSoftware = AES.decryptAES(software, sKey, sIv);

                byte[] uKey = AES.derivateKey(key, uSalt, "username");
                plainUsername = AES.decryptAES(username, uKey, uIv);

                byte[] pKey = AES.derivateKey(key, pSalt, "password");
                plainPassword = AES.decryptAES(password, pKey, pIv);
            } else {
                // Decrypt only password
                plainSoftware = new String(software, StandardCharsets.UTF_8);
                plainUsername = new String(username, StandardCharsets.UTF_8);
                plainPassword = AES.decryptAES(password, key, pIv);
            }
        } finally {
            readLock.unlock();
        }

        return new AccountData(plainSoftware, plainUsername, plainPassword);
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        // creates the account, adding its attributes by constructor
        return new Account(securityVersion, data, masterPassword);
    }

    // #region Package-private methods (exposed to AccountRepository)
    void setData(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            if (pSalt.length != SALT_LENGTH) pSalt = new byte[SALT_LENGTH]; // Avoid reallocating if not necessary

            // Derive key
            final byte[] key = securityVersion.getKey(masterPassword, salt);
            
            software = encryptData(securityVersion, data.software(), key, sSalt, sIv, "software");
            username = encryptData(securityVersion, data.username(), key, uSalt, uIv, "username");
            password = encryptData(securityVersion, data.password(), key, pSalt, pIv, "password");

            // Update properties for UI
            softwareProperty.set(data.software());
            usernameProperty.set(data.username());
        } finally {
            writeLock.unlock();
        }
    }

    private byte[] encryptData(@NotNull SecurityVersion securityVersion, String newVal, byte[] sourceKey, byte[] salt, byte[] iv, String info) throws GeneralSecurityException {
        // Generate salt and IV
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derive key and encrypt password
        final byte[] key = AES.derivateKey(sourceKey, salt, info);
        return AES.encryptAES(newVal, key, iv);
    }

    void updateSecurityVersion(@NotNull SecurityVersion oldSecurityVersion, @NotNull SecurityVersion newSecurityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (oldSecurityVersion == null) throw new NullPointerException("Old security version cannot be null");
        if (newSecurityVersion == null) throw new NullPointerException("New security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            final AccountData data = getData(oldSecurityVersion, masterPassword);
            setData(newSecurityVersion, data, masterPassword);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Captures the current state of this account for rollback purposes.
     *
     * @return a memento object containing the account's current state
     */
    @NotNull AccountMemento captureState() {
        readLock.lock();
        try {
            return new AccountMemento(
                this.salt.clone(),
                this.software.clone(),
                this.sSalt.clone(),
                this.sIv.clone(),
                this.username.clone(),
                this.uSalt.clone(),
                this.uIv.clone(),
                this.password.clone(),
                this.pSalt.clone(),
                this.pIv.clone()
            );
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Restores the account's state from a memento.
     * @param memento the memento containing the state to restore
     */
    void restoreState(@NotNull AccountMemento memento) {
        if (memento == null) throw new NullPointerException("Memento cannot be null");

        writeLock.lock();
        try {
            copyMemento(memento);
        } finally {
            writeLock.unlock();
        }
    }

    private void copyMemento(AccountMemento memento) {
        this.softwareProperty.set("unavailable while locked"); // Invalidate properties to prevent reading inconsistent data during the copy
        this.usernameProperty.set("unavailable while locked"); // Invalidate properties to prevent reading inconsistent data during the copy

        this.salt = memento.salt().clone();
        this.software = memento.software().clone();
        System.arraycopy(memento.sSalt(), 0, this.sSalt, 0, this.sSalt.length);
        System.arraycopy(memento.sIv(), 0, this.sIv, 0, this.sIv.length);
        this.username = memento.username().clone();
        System.arraycopy(memento.uSalt(), 0, this.uSalt, 0, this.uSalt.length);
        System.arraycopy(memento.uIv(), 0, this.uIv, 0, this.uIv.length);
        this.password = memento.password().clone();
        System.arraycopy(memento.pSalt(), 0, this.pSalt, 0, this.pSalt.length);
        System.arraycopy(memento.pIv(), 0, this.pIv, 0, this.pIv.length);
    }

    /**
     * Wrapper record used for moving data in-and-out of this account.
     * This holds sensitive data and should not be passed around.
     */
    public record AccountData(
        @NotNull String software,
        @NotNull String username,
        @NotNull String password
    ) {}

    /**
     * Memento record that captures the state of an Account for rollback purposes.
     * This implements the Memento pattern for transactional support.
     */
    public record AccountMemento(
        @NotNull byte[] salt,
        @NotNull byte[] software, @NotNull byte[] sSalt, @NotNull byte[] sIv,
        @NotNull byte[] username, @NotNull byte[] uSalt, @NotNull byte[] uIv,
        @NotNull byte[] password, @NotNull byte[] pSalt, @NotNull byte[] pIv
    ) {}

    // #endregion
}