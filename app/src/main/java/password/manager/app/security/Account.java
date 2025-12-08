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

import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import password.manager.app.enums.SecurityVersion;

public final class Account {
    private final transient ReadWriteLock lock = new ReentrantReadWriteLock();
    private final transient Lock readLock = lock.readLock();
    private final transient Lock writeLock = lock.writeLock();

    // JsonProperty is redundant if there are getters
    private final transient ReadOnlyStringWrapper softwareProperty = new ReadOnlyStringWrapper(),
                                                  usernameProperty = new ReadOnlyStringWrapper();

    private @JsonProperty byte[] encryptedPassword;
    private final @JsonProperty byte[] salt, iv;

    private final boolean isDerivedSaltVersion;

    public Account(
            @JsonProperty("software") String software,
            @JsonProperty("username") String username,
            @JsonProperty("encryptedPassword") byte[] encryptedPassword,
            @JsonProperty("salt") byte[] salt,
            @JsonProperty("iv") byte[] iv) {

        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        this.isDerivedSaltVersion = (salt == null);

        this.salt = isDerivedSaltVersion ? new byte[16] : salt;
        this.iv = iv;
        this.encryptedPassword = encryptedPassword;
    }

    public Account(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        this.isDerivedSaltVersion = false;

        this.salt = new byte[16];
        this.iv = new byte[16];
        this.setPassword(securityVersion, password, masterPassword);
    }

    public ReadOnlyProperty<String> softwareProperty() {
        return softwareProperty.getReadOnlyProperty();
    }

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

    public String getUsername() {
        readLock.lock();
        try {
            return this.usernameProperty.get();
        } finally {
            readLock.unlock();
        }
    }

    public String getPassword(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        readLock.lock();
        try {
            byte[] key = securityVersion.getKey(masterPassword, salt);
            return AES.decryptAES(encryptedPassword, key, iv);
        } finally {
            readLock.unlock();
        }
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username,
                                      @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // creates the account, adding its attributes by constructor
        return new Account(securityVersion, software, username, password, masterPassword);
    }

    // #region Package-private methods (exposed to AccountRepository)
    void setSoftware(@NotNull String software) {
        writeLock.lock();
        try {
            softwareProperty.set(software);
        } finally {
            writeLock.unlock();
        }
    }

    void setUsername(@NotNull String username) {
        writeLock.lock();
        try {
            usernameProperty.set(username);
        } finally {
            writeLock.unlock();
        }
    }

    void setPassword(@NotNull SecurityVersion securityVersion, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        writeLock.lock();
        try {
            // Generate salt and IV
            final SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(iv);

            final byte[] key = securityVersion.getKey(masterPassword, salt);
            this.encryptedPassword = AES.encryptAES(password, key, iv);
        } finally {
            writeLock.unlock();
        }
    }

    void updateSecurityVersion(@NotNull SecurityVersion oldSecurityVersion, @NotNull SecurityVersion newSecurityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (isDerivedSaltVersion) return;

        writeLock.lock();
        try {
            // Get old salt based on version
            byte[] oldSalt = isDerivedSaltVersion
                ? (softwareProperty.get() + usernameProperty.get()).getBytes()
                : this.salt;

            // Decrypt with old key
            final byte[] oldKey = oldSecurityVersion.getKey(masterPassword, oldSalt);
            final String oldPassword = AES.decryptAES(encryptedPassword, oldKey, iv);

            // Generate new salt and IV
            final SecureRandom random = new SecureRandom();
            random.nextBytes(this.salt);
            random.nextBytes(this.iv);

            // Encrypt with new key using the new salt
            final byte[] newKey = newSecurityVersion.getKey(masterPassword, this.salt);
            this.encryptedPassword = AES.encryptAES(oldPassword, newKey, this.iv);
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
        writeLock.lock();
        try {
            this.softwareProperty.set(memento.software());
            this.usernameProperty.set(memento.username());

            this.encryptedPassword = memento.encryptedPassword().clone();
            System.arraycopy(memento.salt(), 0, this.salt, 0, this.salt.length);
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
        byte[] encryptedPassword,
        byte[] salt,
        byte[] iv
    ) {}
    // #endregion
}