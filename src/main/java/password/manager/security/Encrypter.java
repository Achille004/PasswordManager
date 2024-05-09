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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.jetbrains.annotations.NotNull;

public class Encrypter {
    private static SecretKeyFactory keyFactory;

    private static final int ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 512;
    private static final int AES_KEY_LENGTH = 256;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");

        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hashes the given password with salt using PBKDF2 hashing.
     * 
     * @param password The password to encrypt.
     * @param salt     The salt used to encrypt.
     * @return The hashed password.
     * @throws InvalidKeySpecException
     */
    public static byte[] hash(@NotNull String password, byte[] salt) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_KEY_LENGTH);
        return keyFactory.generateSecret(spec).getEncoded();
    }

    /**
     * Derives an AES key from the login password (used as method to keep the key
     * secret) and salt (used to add randomness).
     * 
     * @param loginPassword The login password to generate the key from.
     * @param salt          The salt used to encrypt.
     * @return The hashed password.
     * @throws InvalidKeySpecException
     */
    public static byte[] getKey(@NotNull String loginPassword, byte[] salt) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(loginPassword.toCharArray(), salt, ITERATIONS, AES_KEY_LENGTH);
        SecretKey secretKey = keyFactory.generateSecret(spec);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        return secretKeySpec.getEncoded();
    }

    /**
     * Uses AES to encrypt a password.
     * 
     * @param password The password to encrypt.
     * @param key      The AES key.
     * @param iv       The initialization vector.
     * @return The encrypted password.
     * @throws GeneralSecurityException
     */
    public static byte[] encryptAES(@NotNull String password, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Cipher object to encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
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
     * @return The decrypted password.
     * @throws GeneralSecurityException
     */
    public static @NotNull String decryptAES(byte[] encryptedPassword, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Cipher object to decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

        // Decrypt the password
        byte[] password = cipher.doFinal(encryptedPassword);

        // Convert it to String
        return new String(password);
    }
}
