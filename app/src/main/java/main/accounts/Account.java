/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022  Francesco Marras

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
package main.accounts;

import java.io.Serializable;
import java.security.SecureRandom;

import main.Utils.Encrypter;

/**
 * @author FrA
 */
public class Account implements Serializable {
    private String software;
    private String username;
    private String encryptedPassword;
    private byte[] iv;

    /**
     * Constructor that directly sets account iformations.
     *
     * @param software The software.
     * @param username The username.
     * @param password The password.
     */
    public Account(String software, String username, String password, String loginPassword) {
        this.software = software;
        this.username = username;

        iv = new byte[16];
        setPassword(password, loginPassword);
    }

    /**
     * Gets the Account's software.
     *
     * @return The software.
     */
    public String getSoftware() {
        return software;
    }

    /**
     * Sets the Account's software.
     *
     * @param software The new software.
     */
    public void setSoftware(String software) {
        this.software = software;
    }

    public Account software(String software) {
        setSoftware(software);
        return this;
    }

    /**
     * Gets the Account's username.
     *
     * @return The username.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Sets the Account's username.
     *
     * @param software The new username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    public Account username(String username) {
        setUsername(username);
        return this;
    }

    /**
     * Gets the Account's password.
     *
     * @param software The new password.
     */
    public String getPassword(String loginPassword) {
        try {
            // Generate IV
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            byte[] key = Encrypter.getKey(loginPassword, getSalt());
            return Encrypter.decryptAES(encryptedPassword, key, iv);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Sets the Account's password.
     *
     * @param software The new password.
     */
    public void setPassword(String password, String loginPassword) {
        try {
            // Generate IV
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            byte[] key = Encrypter.getKey(loginPassword, getSalt());
            this.encryptedPassword = Encrypter.encryptAES(password, key, iv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Account password(String password, String loginPassword) {
        setPassword(password, loginPassword);
        return this;
    }

    public static Account createAccount(String software, String username, String password, String loginPassword) {
        // creates the account, adding its attributes by constructor
        return new Account(software, username, password, loginPassword);
    }

    /**
     * Custom salt based on software and username
     * 
     * @return The salt.
     */
    private byte[] getSalt() {
        return (software + username).getBytes();
    }
}
