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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import password.manager.app.enums.SecurityVersion;

public final class Account {
    // JsonProperty is redundant if there are getters
    private final @JsonIgnore @Getter StringProperty softwareProperty = new SimpleStringProperty(), 
                                                     usernameProperty = new SimpleStringProperty();
    private @JsonProperty byte[] encryptedPassword;
    private final @JsonProperty byte[] salt, iv;

    private final boolean isDerivedSaltVersion;

    public Account(
            @JsonProperty("software") String software,
            @JsonProperty("username") String username,
            @JsonProperty("encryptedPassword") byte[] encryptedPassword,
            @JsonProperty("salt") byte[] salt,
            @JsonProperty("iv") byte[] iv) {
        // Don't use Platform.runLater, it causes synchronization issues
        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        this.isDerivedSaltVersion = salt == null;

        this.salt = isDerivedSaltVersion ? new byte[16] : salt;
        this.iv = iv;
        this.encryptedPassword = encryptedPassword;
    }

    public Account(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // Don't use Platform.runLater, it causes synchronization issues
        this.softwareProperty.set(software);
        this.usernameProperty.set(username);

        this.isDerivedSaltVersion = false;

        salt = new byte[16];
        iv = new byte[16];
        setPassword(securityVersion, password, masterPassword);
    }

    public String getSoftware() {
        return this.softwareProperty.get();
    }

    public void setSoftware(@NotNull String software) {
        if (software.isEmpty()) return;
        Platform.runLater(() -> softwareProperty.set(software));
    }

    public String getUsername() {
        return this.usernameProperty.get();
    }

    public void setUsername(@NotNull String username) {
        if (username.isEmpty()) return;
        Platform.runLater(() -> usernameProperty.set(username));
    }

    public String getPassword(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        byte[] key = securityVersion.getKey(masterPassword, salt);
        return Encrypter.decryptAES(encryptedPassword, key, iv);
    }

    private void setPassword(@NotNull SecurityVersion securityVersion, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // Generate salt and IV
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        final byte[] key = securityVersion.getKey(masterPassword, salt);
        this.encryptedPassword = Encrypter.encryptAES(password, key, iv);
    }

    public void setData(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        if (software.isEmpty() || password.isEmpty() || username.isEmpty()) return;

        // Save current password in case of rollback
        final byte[] oldEncryptedPassword = this.encryptedPassword;
        final byte[] oldSalt = this.salt.clone();
        final byte[] oldIv = this.iv.clone();

        try {
            setPassword(securityVersion, password, masterPassword);
        } catch (GeneralSecurityException e) {
            this.encryptedPassword = oldEncryptedPassword;
            System.arraycopy(oldSalt, 0, salt, 0, oldSalt.length);
            System.arraycopy(oldIv, 0, iv, 0, oldIv.length);
            throw e;
        }

        // Update software and username after updating the password to avoid having to rollback in case of error
        setSoftware(software);
        setUsername(username);
    }

    public void changeMasterPassword(@NotNull SecurityVersion securityVersion, @NotNull String oldMasterPassword, @NotNull String newMasterPassword) throws GeneralSecurityException {
        setPassword(securityVersion, getPassword(securityVersion, oldMasterPassword), newMasterPassword);
    }

    public void updateToLatestVersion(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (isDerivedSaltVersion) {
            final byte[] key = securityVersion.getKey(masterPassword, (getSoftware() + getUsername()).getBytes());
            final String oldPassword = Encrypter.decryptAES(encryptedPassword, key, iv);
            setPassword(securityVersion, oldPassword, masterPassword);
        }
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull SecurityVersion securityVersion, @NotNull String software, @NotNull String username, 
                                      @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // creates the account, adding its attributes by constructor
        return new Account(securityVersion, software, username, password, masterPassword);
    }
}