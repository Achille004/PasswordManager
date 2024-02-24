/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2023  Francesco Marras

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

package main.security;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import main.enums.Language;
import main.enums.SavingOrder;

import java.io.Serializable;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class LoginAccount implements Serializable {
    private SavingOrder savingOrder;
    private Language language;
    private byte[] hashedPassword;
    private final byte[] salt;

    public LoginAccount(SavingOrder savingOrder, Language language, String password) {
        this.savingOrder = savingOrder;
        this.language = language;

        this.salt = new byte[16];
        setPassword(password);
    }

    public SavingOrder getSavingOrder() {
        return this.savingOrder;
    }

    public void setSavingOrder(SavingOrder savingOrder) {
        this.savingOrder = savingOrder;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public boolean verifyPassword(String passwordToVerify) {
        try {
            byte[] hashedPasswordToVerify = Encrypter.hash(passwordToVerify, salt);

            if (Arrays.equals(hashedPassword, hashedPasswordToVerify)) {
                return true;
            }
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean setPasswordVerified(String oldPassword, String newPassword) {
        if (!verifyPassword(oldPassword)) {
            return false;
        }

        setPassword(newPassword);
        return true;
    }

    private void setPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            hashedPassword = Encrypter.hash(password, salt);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Contract("_, _, _ -> new")
    public static @NotNull LoginAccount of(SavingOrder savingOrder, Language language, String password) {
        // creates the account, adding its attributes by constructor
        return new LoginAccount(savingOrder, language, password);
    }
}