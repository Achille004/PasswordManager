/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

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

import password.manager.app.base.SecurityVersion;
import password.manager.app.security.Account;
import password.manager.app.security.Account.AccountData;

public class TestAccount {

    @Test
    void testAccountCreationAndRetrieval() throws GeneralSecurityException {
        String software = "GitHub";
        String username = "testUser";
        String password = "testPassword123";
        String masterPassword = "masterPass456";

        for (SecurityVersion version : SecurityVersion.values()) {
            AccountData expectedData = new AccountData(software, username, password);
            Account account = Account.of(version, expectedData, masterPassword);

            AccountData actualData = account.getData(version, masterPassword);
            assertEquals(expectedData.software(), actualData.software());
            assertEquals(expectedData.username(), actualData.username());
            assertEquals(expectedData.password(), actualData.password());
        }
    }

    @Test
    void testBLNS() throws GeneralSecurityException, IOException {
        try (InputStream stream = TestAccount.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String masterPass, password, software, username;

                while ((masterPass = readBlnsLine(reader)) != null &&
                       (password = readBlnsLine(reader)) != null &&
                       (software = readBlnsLine(reader)) != null &&
                       (username = readBlnsLine(reader)) != null) {

                    for (SecurityVersion version : SecurityVersion.values()) {
                        AccountData data = new AccountData(software, username, password);
                        Account account = Account.of(version, data, masterPass);

                        AccountData actualData = account.getData(version, masterPass);
                        assertEquals(data.software(), actualData.software());
                        assertEquals(data.username(), actualData.username());
                        assertEquals(data.password(), actualData.password());
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
            AccountData data = new AccountData(software, username, password);
            Account account = Account.of(version, data, correctMasterPassword);

            assertThrows(
                GeneralSecurityException.class,
                () -> account.getData(version, wrongMasterPassword)
            );
        }
    }

    // Note: changeMasterPassword is now tested in TestAccountRepository
    // as it has been moved to the AccountRepository class

    @Test
    void testNullValues() {
        AccountData data = new AccountData("software", "user", "password");
        assertThrows(NullPointerException.class, () -> Account.of(null, data, "master"));

        for(SecurityVersion version : SecurityVersion.values()) {
            assertThrows(NullPointerException.class, () -> Account.of(version, null, "master"));
            assertThrows(NullPointerException.class, () -> Account.of(version, data, null));
        }
    }

    @Test
    void testEmptyStrings() throws GeneralSecurityException {
        String software = "App";
        String username = "user";
        String emptyPassword = "";
        String masterPassword = "master";

        for (SecurityVersion version : SecurityVersion.values()) {
            AccountData expectedData = new AccountData(software, username, emptyPassword);
            Account account = Account.of(version, expectedData, masterPassword);

            AccountData actualData = account.getData(version, masterPassword);
            assertEquals(expectedData.software(), actualData.software());
            assertEquals(expectedData.username(), actualData.username());
            assertEquals(expectedData.password(), actualData.password());
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
            AccountData expectedData = new AccountData(software, username, largePassword);
            Account account = Account.of(version, expectedData, masterPassword);

            AccountData actualData = account.getData(version, masterPassword);
            assertEquals(expectedData.software(), actualData.software());
            assertEquals(expectedData.username(), actualData.username());
            assertEquals(expectedData.password(), actualData.password());
        }
    }

    @Test
    void testSpecialCharacters() throws GeneralSecurityException {
        String software = "Test!@#$%^&*()";
        String username = "user<>?:\"{}|";
        String password = "pass\n\r\t\0";
        String masterPassword = "master🔒🔑";

        for (SecurityVersion version : SecurityVersion.values()) {
            AccountData expectedData = new AccountData(software, username, password);
            Account account = Account.of(version, expectedData, masterPassword);

            AccountData actualData = account.getData(version, masterPassword);
            assertEquals(expectedData.software(), actualData.software());
            assertEquals(expectedData.username(), actualData.username());
            assertEquals(expectedData.password(), actualData.password());
        }
    }
}