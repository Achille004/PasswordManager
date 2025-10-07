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

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import password.manager.app.enums.SecurityVersion;
import password.manager.app.security.Encrypter;

class AppTest {
    @Test
    void testAES() {
        String masterPassword = "myTestPassword123!", password = "aintThisStrong?";

        // Generate salt and initialization vector
        byte[] iv = new byte[16];
        byte[] salt = new byte[16];

        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        Arrays.asList(SecurityVersion.values()).forEach(securityVersion -> {
            try {
                byte[] key = securityVersion.getKey(masterPassword, salt);
                byte[] encrypted = Encrypter.encryptAES(password, key, iv);
                String decrypted = Encrypter.decryptAES(encrypted, key, iv);
                assertEquals(password, decrypted, "Decrypted password (" + decrypted + ") doesn't match the original one (" + password + ")");
            } catch (GeneralSecurityException e) {
                fail("Encryption/Decryption failed: " + e.getMessage());
            }
        });
    }
}
