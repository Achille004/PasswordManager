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
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import password.manager.app.enums.SecurityVersion;
import password.manager.app.security.AES;

public class TestAES {

    static final byte[] salt = new byte[16], iv = new byte[16];
    static final SecureRandom random = new SecureRandom();

    @Test
    void testBLNS() throws GeneralSecurityException, IOException {
        try (InputStream stream = TestAES.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String masterPass, pass;

                while ((masterPass = readBlnsLine(reader)) != null && (pass = readBlnsLine(reader)) != null) {
                    random.nextBytes(salt);
                    random.nextBytes(iv);

                    for (SecurityVersion version : SecurityVersion.values()) {
                        byte[] key = version.getKey(masterPass, salt);
                        byte[] e = AES.encryptAES(pass, key, iv);
                        String d = AES.decryptAES(e, key, iv);
                        assertEquals(d, pass);
                    }
                }
            }
        }
    }

    @Test
    void testEmptyString() throws GeneralSecurityException {
        random.nextBytes(salt);
        random.nextBytes(iv);

        String empty = "";
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);
            byte[] encrypted = AES.encryptAES(empty, key, iv);
            String decrypted = AES.decryptAES(encrypted, key, iv);
            assertEquals(empty, decrypted);
        }
    }

    @Test
    void testIVUniqueness() throws GeneralSecurityException {
        random.nextBytes(salt);
        String plaintext = "Test message";
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);

            byte[] iv1 = new byte[16];
            random.nextBytes(iv1);
            byte[] encrypted1 = AES.encryptAES(plaintext, key, iv1);

            byte[] iv2 = new byte[16];
            random.nextBytes(iv2);
            byte[] encrypted2 = AES.encryptAES(plaintext, key, iv2);

            // Different IVs should produce different ciphertexts
            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Test
    void testWrongKeyDecryption() throws GeneralSecurityException {
        random.nextBytes(salt);
        random.nextBytes(iv);

        String plaintext = "Secret message";
        String password1 = "correctPassword";
        String password2 = "wrongPassword";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key1 = version.getKey(password1, salt);
            byte[] key2 = version.getKey(password2, salt);

            byte[] encrypted = AES.encryptAES(plaintext, key1, iv);

            // Decrypting with wrong key should either throw exception or return garbage
            assertThrows(GeneralSecurityException.class, () -> {
                String decrypted = AES.decryptAES(encrypted, key2, iv);
                // If no exception, decrypted text should not match original
                assertNotEquals(plaintext, decrypted);
            });
        }
    }

    @Test
    void testCorruptedCiphertext() throws GeneralSecurityException {
        random.nextBytes(salt);
        random.nextBytes(iv);

        String plaintext = "Test message";
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);
            byte[] encrypted = AES.encryptAES(plaintext, key, iv);

            // Corrupt the ciphertext
            encrypted[encrypted.length / 2] ^= 0xFF;

            assertThrows(
                GeneralSecurityException.class,
                () -> AES.decryptAES(encrypted, key, iv)
            );
        }
    }

    @Test
    void testLargeString() throws GeneralSecurityException {
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Create a 1MiB string
        int MiB = 1024 * 1024;
        StringBuilder sb = new StringBuilder(MiB);
        for (int i = 0; i < MiB; i++) sb.append((char) ('a' + (i % 26)));

        String largeText = sb.toString();
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);
            byte[] encrypted = AES.encryptAES(largeText, key, iv);
            String decrypted = AES.decryptAES(encrypted, key, iv);
            assertEquals(largeText, decrypted);
        }
    }

    @Test
    void testNullInputs() {
        random.nextBytes(salt);
        random.nextBytes(iv);

        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);

            assertThrows(
                Exception.class,
                () -> AES.encryptAES(null, key, iv)
            );

            assertThrows(
                Exception.class,
                () -> AES.decryptAES(null, key, iv)
            );
        }
    }
}
