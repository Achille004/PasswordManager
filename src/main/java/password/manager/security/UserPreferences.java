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

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import password.manager.enums.SortingOrder;
import password.manager.utils.Utils;

public class UserPreferences implements Serializable {
    private transient @Getter ObjectProperty<Locale> localeProperty;
    private transient @Getter ObjectProperty<SortingOrder> sortingOrderProperty;
    private byte[] hashedPassword;
    private final byte[] salt;

    public UserPreferences(String password) throws InvalidKeySpecException {
        this.localeProperty = new SimpleObjectProperty<>(Utils.DEFAULT_LOCALE);
        this.sortingOrderProperty = new SimpleObjectProperty<>(SortingOrder.SOFTWARE);

        this.salt = new byte[16];
        setPassword(password);
    }

    public Locale getLocale() {
        return localeProperty.get();
    }

    public void setLocale(Locale locale) {
        localeProperty.set(locale);
    }

    public SortingOrder getSortingOrder() {
        return sortingOrderProperty.get();
    }

    public void setSortingOrder(SortingOrder sortingOrder) {
        sortingOrderProperty.set(sortingOrder);
    }

    public boolean verifyPassword(String passwordToVerify) throws InvalidKeySpecException {
        byte[] hashedPasswordToVerify = Encrypter.hash(passwordToVerify, salt);
        return Arrays.equals(hashedPassword, hashedPasswordToVerify);
    }

    public boolean setPasswordVerified(String oldPassword, String newPassword) throws InvalidKeySpecException {
        if (!verifyPassword(oldPassword)) {
            return false;
        }

        setPassword(newPassword);
        return true;
    }

    private void setPassword(String password) throws InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        hashedPassword = Encrypter.hash(password, salt);
    }

    @Contract("_ -> new")
    public static @NotNull UserPreferences of(String password) throws InvalidKeySpecException {
        return new UserPreferences(password);
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(getLocale());
        out.writeObject(getSortingOrder());
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        Locale localeValue = (Locale) in.readObject();
        this.localeProperty = new SimpleObjectProperty<>(localeValue);

        SortingOrder sortingOrderValue = (SortingOrder) in.readObject();
        this.sortingOrderProperty = new SimpleObjectProperty<>(sortingOrderValue);

        in.defaultReadObject();
    }
}