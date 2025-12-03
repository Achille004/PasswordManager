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

package password.manager.app.security;

import java.security.GeneralSecurityException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Provides AES encryption and decryption methods, powered by Bouncy Castle.
 */
public final class AES {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");
    }

    /**
     * Uses AES to encrypt a password.
     *
     * @param password The password to encrypt.
     * @param key      The AES key.
     * @param iv       The initialization vector.
     * @return         The encrypted password.
     * @throws GeneralSecurityException
     */
    public static byte[] encryptAES(@NotNull String password, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Cipher object to encrypt
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

        // Encrypt the password
        return cipher.doFinal(password.getBytes());
    }

    /**
     * Decrypts an AES-encrypted password.
     *
     * @param encryptedPassword The encrypted password to decrypt.
     * @param key               The AES key.
     * @param iv                The initialization vector.
     * @return                  The decrypted password.
     * @throws GeneralSecurityException
     */
    public static @NotNull String decryptAES(byte[] encryptedPassword, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Cipher object to decrypt
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

        // Decrypt the password
        final byte[] password = cipher.doFinal(encryptedPassword);

        // Convert it to String
        return new String(password);
    }
}
