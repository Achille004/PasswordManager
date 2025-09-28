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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
public final class Account {
    private String software, username;
    private byte[] encryptedPassword;
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final byte[] salt, iv;

    @ToString.Exclude @EqualsAndHashCode.Exclude @JsonIgnore
    private final Boolean isOlderVersion;

    @ToString.Exclude @EqualsAndHashCode.Exclude @JsonIgnore
    private final StringProperty softwareProperty = new SimpleStringProperty(), usernameProperty = new SimpleStringProperty();

    public Account(
            @JsonProperty("software") String software,
            @JsonProperty("username") String username,
            @JsonProperty("encryptedPassword") byte[] encryptedPassword,
            @JsonProperty("salt") byte[] salt,
            @JsonProperty("iv") byte[] iv) {
        propertiesSetup(software, username);

        this.encryptedPassword = encryptedPassword;
        this.iv = iv;

        isOlderVersion = salt == null;
        if(isOlderVersion) {
            salt = new byte[16];
        }
        this.salt = salt;
    }

    public Account(@NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        propertiesSetup(software, username);

        salt = new byte[16];
        iv = new byte[16];
        setPassword(password, masterPassword);

        isOlderVersion = false;
    }

    public void setSoftware(@NotNull String software) {
        if (!software.isEmpty()) {
            this.software = software;
            Platform.runLater(() -> softwareProperty.set(software));
        }
    }

    public void setUsername(@NotNull String username) {
        if (!username.isEmpty()) {
            this.username = username;
            Platform.runLater(() -> usernameProperty.set(username));
        }
    }

    private void setPassword(@NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // Generate salt and IV
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        final byte[] key = Encrypter.getKey(masterPassword, salt);
        this.encryptedPassword = Encrypter.encryptAES(password, key, iv);
    }

    public String getPassword(@NotNull String masterPassword) throws GeneralSecurityException {
        byte[] key = Encrypter.getKey(masterPassword, salt);
        return Encrypter.decryptAES(encryptedPassword, key, iv);
    }

    public @NotNull Boolean setData(@NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        if (software.isEmpty() || password.isEmpty() || username.isEmpty()) {
            return false;
        }

        // Save current password in case of rollback
        final byte[] oldEncryptedPassword = this.encryptedPassword;
        final byte[] oldSalt = this.salt.clone();
        final byte[] oldIv = this.iv.clone();

        try {
            setPassword(password, masterPassword);
        } catch (GeneralSecurityException e) {
            this.encryptedPassword = oldEncryptedPassword;
            System.arraycopy(oldSalt, 0, salt, 0, oldSalt.length);
            System.arraycopy(oldIv, 0, iv, 0, oldIv.length);
            throw e;
        }

        // Update software and username later to avoid having to rollback in case of error
        setSoftware(software);
        setUsername(username);
        
        return true;
    }

    public void changeMasterPassword(@NotNull String oldMasterPassword, @NotNull String newMasterPassword) throws GeneralSecurityException {
        setPassword(getPassword(oldMasterPassword), newMasterPassword);
    }

    public void updateToLatestVersion(@NotNull String masterPassword) throws GeneralSecurityException {
        if (isOlderVersion) {
            final byte[] key = Encrypter.getKeyOld(masterPassword, (software + username).getBytes());
            final String oldPassword = Encrypter.decryptAES(encryptedPassword, key, iv);
            setPassword(oldPassword, masterPassword);
        }
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull Account of(@NotNull String software, @NotNull String username, @NotNull String password, @NotNull String masterPassword) throws GeneralSecurityException {
        // creates the account, adding its attributes by constructor
        return new Account(software, username, password, masterPassword);
    }

    private void propertiesSetup(String software, String username) {
        this.software = software;
        this.username = username;

        softwareProperty.set(software);
        softwareProperty.addListener((_, _, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                this.software = newValue;
            }
        });

        usernameProperty.set(username);
        usernameProperty.addListener((_, _, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                this.username = newValue;
            }
        });
    }
}