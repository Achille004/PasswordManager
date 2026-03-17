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

package testing.security;

import static org.junit.jupiter.api.Assertions.*;
import static testing.TestingUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import password.manager.app.base.SecurityVersion;
import password.manager.app.security.AES;

public class TestAES {

    static SecureRandom random;

    private byte[] salt, iv;

    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            random = new SecureRandom();
        }
    }

    @BeforeEach
    void setUp() {
        salt = newRandom16();
        iv = newRandom16();
    }

    @Test
    void testBLNS() throws GeneralSecurityException, IOException {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        try (InputStream stream = TestAES.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String masterPass, pass;

                while ((masterPass = readBlnsLine(reader)) != null && (pass = readBlnsLine(reader)) != null) {
                    String currentMasterPass = masterPass;
                    String currentPass = pass;
                    tasks.add(() -> {
                        testEncryptionDecryption(currentPass, currentMasterPass);
                        return null;
                    });
                }
            }

            List<Future<Void>> results = executor.invokeAll(tasks);
            for (Future<Void> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof GeneralSecurityException securityException) {
                        throw securityException;
                    }
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new RuntimeException(cause);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("BLNS parallel execution interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testEmptyString() throws GeneralSecurityException {
        String empty = "";
        String password = "testPassword123";

        testEncryptionDecryption(empty, password);
    }

    @Test
    void testIVUniqueness() throws GeneralSecurityException {
        byte[] localSalt = newRandom16();

        String plaintext = "Test message";
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, localSalt);

            byte[] iv1 = newRandom16();
            byte[] encrypted1 = AES.encryptStringAES(plaintext, key, iv1);

            byte[] iv2 = newRandom16();
            byte[] encrypted2 = AES.encryptStringAES(plaintext, key, iv2);

            // Different IVs should produce different ciphertexts
            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Test
    void testWrongKeyDecryption() throws GeneralSecurityException {
        String plaintext = "Secret message";
        String password1 = "correctPassword";
        String password2 = "wrongPassword";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key1 = version.getKey(password1, salt);
            byte[] key2 = version.getKey(password2, salt);

            byte[] encrypted = AES.encryptStringAES(plaintext, key1, iv);

            // Decrypting with wrong key should either throw exception or return garbage
            assertThrows(GeneralSecurityException.class, () -> {
                String decrypted = AES.decryptStringAES(encrypted, key2, iv);
                // If no exception, decrypted text should not match original
                assertNotEquals(plaintext, decrypted);
            });
        }
    }

    @Test
    void testCorruptedCiphertext() throws GeneralSecurityException {
        String plaintext = "Test message";
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);
            byte[] encrypted = AES.encryptStringAES(plaintext, key, iv);

            // Corrupt the ciphertext
            encrypted[encrypted.length / 2] ^= 0xFF;

            assertThrows(
                GeneralSecurityException.class,
                () -> AES.decryptStringAES(encrypted, key, iv)
            );
        }
    }

    @Test
    void testLargeString() throws GeneralSecurityException {
        // Create a 1MiB string
        int MiB = 1024 * 1024;
        StringBuilder sb = new StringBuilder(MiB);
        for (int i = 0; i < MiB; i++) sb.append((char) ('a' + (i % 26)));

        String largeText = sb.toString();
        String password = "testPassword123";

        testEncryptionDecryption(largeText, password);
    }

    @Test
    void testNullInputs() {
        String password = "testPassword123";

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, salt);

            assertThrows(
                Exception.class,
                () -> AES.encryptStringAES(null, key, iv)
            );

            assertThrows(
                Exception.class,
                () -> AES.decryptStringAES(null, key, iv)
            );
        }
    }

    private void testEncryptionDecryption(String plaintext, String password) throws GeneralSecurityException {
        byte[] localSalt = newRandom16();
        byte[] localIv = newRandom16();

        for (SecurityVersion version : SecurityVersion.values()) {
            byte[] key = version.getKey(password, localSalt);
            byte[] encrypted = AES.encryptStringAES(plaintext, key, localIv);
            String decrypted = AES.decryptStringAES(encrypted, key, localIv);
            assertEquals(plaintext, decrypted);
        }
    }

    private byte[] newRandom16() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytes;
    }
}
