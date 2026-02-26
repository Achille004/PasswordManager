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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private @JsonProperty("encryptedPassword") byte[] encryptedPassword;
    private @JsonProperty("salt") byte[] salt;
    private final @JsonProperty("iv") byte[] iv;

    public Account() {
        this.encryptedPassword = null;
        this.salt = new byte[SALT_LENGTH];
        this.iv = new byte[IV_LENGTH];
    }

    public Account(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (software == null) throw new NullPointerException("Software cannot be null");
        if (username == null) throw new NullPointerException("Username cannot be null");
        if (password == null) throw new NullPointerException("Password cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        this();

        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        this.setPassword(securityVersion, password, masterPassword);
    }

    @SuppressWarnings("unused") // Used by Jackson for deserialization
    private Account(
            @JsonProperty(value = "software", required = true) @NotNull String software,
            @JsonProperty(value = "username", required = true) @NotNull  String username,
            @JsonProperty(value = "encryptedPassword", required = true) @NotNull byte[] encryptedPassword,
            @JsonProperty(value = "salt", required = false) @Nullable byte[] salt,
            @JsonProperty(value = "iv", required = true) @NotNull byte[] iv) {

        if (software == null) throw new NullPointerException("Software cannot be null");
        if (username == null) throw new NullPointerException("Username cannot be null");
        if (encryptedPassword == null) throw new NullPointerException("Encrypted password cannot be null");
        if (iv == null || iv.length != IV_LENGTH) throw new NullPointerException("IV cannot be null or not " + IV_LENGTH + " bytes long");

        this();

        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        // If salt is not provided, derive it from software and username (backward compatibility:
        // older versions didn't store salt and used software+username bytes as salt instead).
        // IMPORTANT: the derived value is written back into this.salt so it is persisted on the
        // next save, at which point the account behaves identically to a modern one.
        this.salt = (salt != null && salt.length == SALT_LENGTH) ? salt.clone() : (software + username).getBytes();
        System.arraycopy(iv, 0, this.iv, 0, this.iv.length);

        this.encryptedPassword = encryptedPassword;
    }

    public ReadOnlyProperty<String> softwareProperty() {
        return softwareProperty.getReadOnlyProperty();
    }

    @JsonProperty("software")
    public String getSoftware() {
        readLock.lock();
        try {
            return this.softwareProperty.get();
        } finally {
            readLock.unlock();
        }
    }

    public ReadOnlyProperty<String> usernameProperty() {
        return usernameProperty.getReadOnlyProperty();
    }

    @JsonProperty("username")
    public String getUsername() {
        readLock.lock();
        try {
            return this.usernameProperty.get();
        } finally {
            readLock.unlock();
        }
    }

    public String getPassword(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        readLock.lock();
        try {
            // Get key and decrypt password
            byte[] key = securityVersion.getKey(masterPassword, salt);
            return AES.decryptAES(encryptedPassword, key, iv);
        } finally {
            readLock.unlock();
        }
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username,
                                      @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (software == null) throw new NullPointerException("Software cannot be null");
        if (username == null) throw new NullPointerException("Username cannot be null");
        if (password == null) throw new NullPointerException("Password cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        // creates the account, adding its attributes by constructor
        return new Account(securityVersion, software, username, password, masterPassword);
    }

    // #region Package-private methods (exposed to AccountRepository)

    void setSoftware(@NotNull String software) {
        if (software == null) throw new NullPointerException("Software cannot be null");

        writeLock.lock();
        try {
            softwareProperty.set(software);
        } finally {
            writeLock.unlock();
        }
    }

    void setUsername(@NotNull String username) {
        if (username == null) throw new NullPointerException("Username cannot be null");

        writeLock.lock();
        try {
            usernameProperty.set(username);
        } finally {
            writeLock.unlock();
        }
    }

    void setPassword(@NotNull SecurityVersion securityVersion, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (password == null) throw new NullPointerException("Password cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            // Generate salt and IV
            final SecureRandom random = new SecureRandom();
            if (salt.length != SALT_LENGTH) salt = new byte[SALT_LENGTH]; // Avoid reallocating if not necessary
            random.nextBytes(this.salt);
            random.nextBytes(iv);

            // Derive key and encrypt password
            final byte[] key = securityVersion.getKey(masterPassword, salt);
            this.encryptedPassword = AES.encryptAES(password, key, iv);
        } finally {
            writeLock.unlock();
        }
    }

    void updateSecurityVersion(@NotNull SecurityVersion oldSecurityVersion, @NotNull SecurityVersion newSecurityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (oldSecurityVersion == null) throw new NullPointerException("Old security version cannot be null");
        if (newSecurityVersion == null) throw new NullPointerException("New security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            final String password = getPassword(oldSecurityVersion, masterPassword);
            setPassword(newSecurityVersion, password, masterPassword);
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
                this.softwareProperty.get(),
                this.usernameProperty.get(),
                encryptedPassword.clone(),
                salt.clone(),
                iv.clone()
            );
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Restores the account's state from a memento.
     *
     * @param memento the memento containing the state to restore
     */
    void restoreState(@NotNull AccountMemento memento) {
        if (memento == null) throw new NullPointerException("Memento cannot be null");

        writeLock.lock();
        try {
            this.softwareProperty.set(memento.software());
            this.usernameProperty.set(memento.username());

            this.encryptedPassword = memento.encryptedPassword().clone();
            this.salt = memento.salt().clone();
            System.arraycopy(memento.iv(), 0, this.iv, 0, this.iv.length);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Memento record that captures the state of an Account for rollback purposes.
     * This implements the Memento pattern for transactional support.
     */
    public record AccountMemento(
        @NotNull String software,
        @NotNull String username,
        @NotNull byte[] encryptedPassword,
        @NotNull byte[] salt,
        @NotNull byte[] iv
    ) {}

    // #endregion
}