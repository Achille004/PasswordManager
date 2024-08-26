/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager.security;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public final class Account {
    private String software, username;
    private byte[] encryptedPassword;
    private final byte[] iv;

    public Account(
            @JsonProperty("software") String software,
            @JsonProperty("username") String username,
            @JsonProperty("encryptedPassword") byte[] encryptedPassword, 
            @JsonProperty("iv") byte[] iv) {
        this.software = software;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.iv = iv;
    }

    public Account(@NotNull String software, @NotNull String username, @NotNull String password, @NotNull String loginPassword) throws GeneralSecurityException {
        this.software = software;
        this.username = username;

        iv = new byte[16];
        setPassword(password, loginPassword);
    }

    private void setPassword(@NotNull String password, @NotNull String loginPassword) throws GeneralSecurityException {
        // Generate IV
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        byte[] key = Encrypter.getKey(loginPassword, getSalt());
        this.encryptedPassword = Encrypter.encryptAES(password, key, iv);
    }

    public String getPassword(@NotNull String loginPassword) throws GeneralSecurityException {
        byte[] key = Encrypter.getKey(loginPassword, getSalt());
        return Encrypter.decryptAES(encryptedPassword, key, iv);
    }

    public @NotNull Boolean setData(@NotNull String software, @NotNull String username, @NotNull String password, String loginPassword) throws GeneralSecurityException {
        if (software.isEmpty() || password.isEmpty() || username.isEmpty()) {
            return false;
        }

        // First the password, so that if any error occurs it exits before software and username get reassigned
        setPassword(password, loginPassword);
        this.software = software;
        this.username = username;

        return true;
    }

    public void changeLoginPassword(@NotNull String oldLoginPassword, @NotNull String newLoginPassword) throws GeneralSecurityException {
        setPassword(getPassword(oldLoginPassword), newLoginPassword);
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull Account of(@NotNull String software, @NotNull String username, @NotNull String password, @NotNull String loginPassword) throws GeneralSecurityException {
        // creates the account, adding its attributes by constructor
        return new Account(software, username, password, loginPassword);
    }

    /**
     * Custom salt based on software and username
     * 
     * @return The salt.
     */
    @Contract(value = " -> new", pure = true)
    private byte @NotNull [] getSalt() {
        return (software + username).getBytes();
    }
}