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
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import password.manager.app.singletons.Logger;

/**
 * Cryptographic utility class for password hashing and key derivation.
 *
 * <p><b>Design Note (Java 25+ HKDF consideration):</b>
 * While Java 25 introduces native HKDF support, this implementation continues to use
 * Argon2 for both password hashing and key derivation. This design choice maintains:
 * <ul>
 *   <li>Optimal security for password-based operations (Argon2's memory-hard properties)</li>
 *   <li>Consistent performance characteristics across the application</li>
 *   <li>Simplified architecture with lazy decryption (decrypt-on-demand per account)</li>
 * </ul>
 *
 * HKDF would only provide benefits for bulk operations (e.g., mass password changes),
 * which are infrequent in typical password manager usage patterns.
 * </p>
 */
public final class Encrypter {
    @Deprecated
    private static @Getter SecretKeyFactory keyFactory;

    public static final int HASH_BITS = 512;
    public static final int AES_BITS = 256;

    // PBKDF2 parameters
    @Deprecated
    public static final int PBKDF2_ITERATIONS = 65536;

    // Argon2 parameters
    public static final int ARGON2_MEMORY = 65536;       // 64MB in KB
    public static final int ARGON2_ITERATIONS = 4;       // Time cost
    public static final int ARGON2_PARALLELISM = 4;      // Parallelism factor

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");

        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            Logger.getInstance().addError(e);
        }
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
     * Decrypts and AES encrypted password.
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
