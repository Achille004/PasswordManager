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

package testing;

import static org.junit.jupiter.api.Assertions.*;
import static testing.TestingUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Test;

import password.manager.app.enums.SecurityVersion;
import password.manager.app.security.Account;

public class TestAccount {

    @Test
    void testAccountCreationAndRetrieval() throws GeneralSecurityException {
        String software = "GitHub";
        String username = "testUser";
        String password = "testPassword123";
        String masterPassword = "masterPass456";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            assertEquals(software, account.getSoftware());
            assertEquals(username, account.getUsername());
            assertEquals(password, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testBLNS() throws GeneralSecurityException, IOException {
        try (InputStream stream = TestAccount.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String masterPass, pass, software, username;

                while ((masterPass = readBlnsLine(reader)) != null &&
                       (pass = readBlnsLine(reader)) != null &&
                       (software = readBlnsLine(reader)) != null &&
                       (username = readBlnsLine(reader)) != null) {

                    for (SecurityVersion version : SecurityVersion.values()) {
                        Account account = Account.of(version, software, username, pass, masterPass);
                        assertEquals(pass, account.getPassword(version, masterPass));
                    }
                }
            }
        }
    }

    @Test
    void testWrongMasterPassword() throws GeneralSecurityException {
        String software = "TestApp";
        String username = "user123";
        String password = "secretPass";
        String correctMasterPassword = "correctMaster";
        String wrongMasterPassword = "wrongMaster";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, correctMasterPassword);

            assertThrows(
                GeneralSecurityException.class,
                () -> account.getPassword(version, wrongMasterPassword)
            );
        }
    }

    @Test
    void testSetData() throws GeneralSecurityException {
        String software = "OldSoftware";
        String username = "oldUser";
        String password = "oldPassword";
        String masterPassword = "master123";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            String newSoftware = "NewSoftware";
            String newUsername = "newUser";
            String newPassword = "newPassword";

            account.setData(version, newSoftware, newUsername, newPassword, masterPassword);

            assertEquals(newSoftware, account.getSoftware());
            assertEquals(newUsername, account.getUsername());
            assertEquals(newPassword, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testChangeMasterPassword() throws GeneralSecurityException {
        String software = "TestApp";
        String username = "testUser";
        String password = "myPassword";
        String oldMasterPassword = "oldMaster";
        String newMasterPassword = "newMaster";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, oldMasterPassword);

            account.changeMasterPassword(version, oldMasterPassword, newMasterPassword);

            // Old master password should no longer work
            assertThrows(
                GeneralSecurityException.class,
                () -> account.getPassword(version, oldMasterPassword)
            );

            // New master password should work
            assertEquals(password, account.getPassword(version, newMasterPassword));
        }
    }

    @Test
    void testSetDataRollbackOnError() throws GeneralSecurityException {
        String software = "TestApp";
        String username = "testUser";
        String password = "originalPassword";
        String masterPassword = "master123";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            // Try to set data with empty fields (should be ignored)
            account.setData(version, "", username, password, masterPassword);
            assertEquals(software, account.getSoftware());

            account.setData(version, software, "", password, masterPassword);
            assertEquals(username, account.getUsername());

            account.setData(version, software, username, "", masterPassword);
            assertEquals(password, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testEmptyStrings() throws GeneralSecurityException {
        String software = "App";
        String username = "user";
        String emptyPassword = "";
        String masterPassword = "master";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, emptyPassword, masterPassword);
            assertEquals(emptyPassword, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testSetSoftwareAndUsername() throws GeneralSecurityException, InterruptedException {
        String software = "InitialApp";
        String username = "initialUser";
        String password = "password123";
        String masterPassword = "master";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            String newSoftware = "UpdatedApp";
            String newUsername = "updatedUser";

            account.setSoftware(newSoftware);
            account.setUsername(newUsername);

            // Wait for Platform.runLater to execute
            Thread.sleep(100);

            assertEquals(newSoftware, account.getSoftware());
            assertEquals(newUsername, account.getUsername());
        }
    }

    @Test
    void testIgnoreEmptySetters() throws GeneralSecurityException, InterruptedException {
        String software = "TestApp";
        String username = "testUser";
        String password = "password";
        String masterPassword = "master";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            account.setSoftware("");
            account.setUsername("");

            Thread.sleep(100);

            assertEquals(software, account.getSoftware());
            assertEquals(username, account.getUsername());
        }
    }

    @Test
    void testLargePassword() throws GeneralSecurityException {
        String software = "TestApp";
        String username = "testUser";
        String masterPassword = "master";

        // Create a 1MiB password
        int MiB = 1024 * 1024;
        StringBuilder sb = new StringBuilder(MiB);
        for (int i = 0; i < MiB; i++) sb.append((char) ('a' + (i % 26)));
        String largePassword = sb.toString();

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, largePassword, masterPassword);
            assertEquals(largePassword, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testSpecialCharacters() throws GeneralSecurityException {
        String software = "Test!@#$%^&*()";
        String username = "user<>?:\"{}|";
        String password = "pass\n\r\t\0";
        String masterPassword = "master🔒🔑";

        for (SecurityVersion version : SecurityVersion.values()) {
            Account account = Account.of(version, software, username, password, masterPassword);

            assertEquals(software, account.getSoftware());
            assertEquals(username, account.getUsername());
            assertEquals(password, account.getPassword(version, masterPassword));
        }
    }

    @Test
    void testProperties() throws GeneralSecurityException {
        Account account = new Account(SecurityVersion.LATEST, "TestSoftware", "TestUser", "TestPassword", "TestMaster");
        assertEquals("TestSoftware", account.getSoftwareProperty().get());
        assertEquals("TestUser", account.getUsernameProperty().get());

        account.setSoftware("UpdatedSoftware");
        assertEquals("UpdatedSoftware", account.getSoftwareProperty().get());

        account.setUsername("UpdatedUser");
        assertEquals("UpdatedUser", account.getUsernameProperty().get());
    }

    @Test
    void testPropertyListeners() throws GeneralSecurityException, InterruptedException {
        Account account = new Account(SecurityVersion.LATEST, "TestSoftware", "TestUser", "TestPassword", "TestMaster");

        String[] capturedValue = new String[2];
        account.getSoftwareProperty().addListener((_, _, newVal) -> capturedValue[0] = newVal);
        account.getUsernameProperty().addListener((_, _, newVal) -> capturedValue[1] = newVal);

        account.setSoftware("NewSoftware");
        Thread.sleep(100); // Wait for listener to be called
        assertEquals("NewSoftware", capturedValue[0]);

        account.setUsername("NewUsername");
        Thread.sleep(100); // Wait for listener to be called
        assertEquals("NewUsername", capturedValue[1]);
    }
}