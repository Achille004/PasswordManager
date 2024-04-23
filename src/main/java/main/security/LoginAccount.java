/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

import java.io.Serializable;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import main.enums.SortingOrder;

public class LoginAccount implements Serializable {
    private SortingOrder sortingOrder;
    private Locale locale;
    private byte[] hashedPassword;
    private final byte[] salt;

    public LoginAccount(SortingOrder sortingOrder, Locale locale, String password) {
        this.sortingOrder = sortingOrder;
        this.locale = locale;

        this.salt = new byte[16];
        setPassword(password);
    }

    public SortingOrder getSortingOrder() {
        return this.sortingOrder;
    }

    public void setSortingOrder(SortingOrder sortingOrder) {
        this.sortingOrder = sortingOrder;
    }
    
    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
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
    public static @NotNull LoginAccount of(SortingOrder sortingOrder, Locale locale, String password) {
        // creates the account, adding its attributes by constructor
        return new LoginAccount(sortingOrder, locale, password);
    }
}