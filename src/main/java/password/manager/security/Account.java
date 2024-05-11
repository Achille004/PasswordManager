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

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class Account implements Serializable {
    private @Getter String software, username;
    private byte[] encryptedPassword;
    private final byte[] iv;

    public Account(String software, String username, String password, String loginPassword) throws GeneralSecurityException {
        this.software = software;
        this.username = username;

        iv = new byte[16];
        setPassword(password, loginPassword);
    }

    private void setPassword(String password, String loginPassword) throws GeneralSecurityException {
        // Generate IV
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        byte[] key = Encrypter.getKey(loginPassword, getSalt());
        this.encryptedPassword = Encrypter.encryptAES(password, key, iv);
    }

    public String getPassword(String loginPassword) throws GeneralSecurityException {
        byte[] key = Encrypter.getKey(loginPassword, getSalt());
        return Encrypter.decryptAES(encryptedPassword, key, iv);
    }

    public boolean setData(@Nullable String software, @Nullable String username, @Nullable String password,
            String loginPassword) throws GeneralSecurityException {
        if (password == null) {
            String oldPassword = getPassword(loginPassword);

            if (oldPassword == null) {
                return false;
            }

            password = oldPassword;
        }

        if (software != null) {
            this.software = software;
        }

        if (username != null) {
            this.username = username;
        }

        setPassword(password, loginPassword);

        return true;
    }

    public void changeLoginPassword(String oldLoginPassword, String newLoginPassword) throws GeneralSecurityException {
        setPassword(getPassword(oldLoginPassword), newLoginPassword);
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull Account of(String software, String username, String password, String loginPassword) throws GeneralSecurityException {
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