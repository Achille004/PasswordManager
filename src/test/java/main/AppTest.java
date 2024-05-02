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

package main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import main.security.Encrypter;

class AppTest {
    @Test
    void testAES() throws Exception {
        String loginPassword = "logPass";
        String s = "soft", u = "user", p = "pass";

        // Generate IV
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Generate salt
        byte[] salt = (s + u).getBytes();

        byte[] key = Encrypter.getKey(loginPassword, salt);

        byte[] e = Encrypter.encryptAES(p, key, iv);
        String d = Encrypter.decryptAES(e, key, iv);
        assertEquals(p, d, "Decrypted password (" + d + ") doesn't match the original one (" + p + ")");
    }
}
