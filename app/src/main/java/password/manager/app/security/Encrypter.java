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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

public final class Encrypter {
    private static SecretKeyFactory keyFactory;

    // Keep old parameters for compatibility
    private static final int ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 512;
    private static final int AES_KEY_LENGTH = 256;

    // Argon2 parameters
    private static final int ARGON2_MEMORY = 65536;       // 64MB in KB
    private static final int ARGON2_ITERATIONS = 4;       // Time cost
    private static final int ARGON2_PARALLELISM = 4;      // Parallelism factor
    private static final int ARGON2_HASH_LENGTH = 64;     // 512 bits
    private static final int ARGON2_AES_KEY_LENGTH = 32;  // 256 bits

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");

        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hashes the given password using Argon2id.
     * 
     * @param password The password to hash.
     * @param salt     The salt used for hashing.
     * @return The hashed password.
     */
    public static byte[] hash(String password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(ARGON2_PARALLELISM)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withIterations(ARGON2_ITERATIONS)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] result = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(password.toCharArray(), result);
        return result;
    }

    /**
     * Derives an AES key from the master password using Argon2id.
     * 
     * @param masterPassword The master password to generate the key from.
     * @param salt           The salt used for key derivation.
     * @return The derived AES key.
     */
    public static byte[] getKey(@NotNull String masterPassword, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(ARGON2_PARALLELISM)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withIterations(ARGON2_ITERATIONS)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] keyBytes = new byte[ARGON2_AES_KEY_LENGTH];
        generator.generateBytes(masterPassword.toCharArray(), keyBytes);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
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

    /* OLD PBKDF2 METHODS */

    /**
     * Hashes the given password with salt using PBKDF2.
     * @deprecated Should only be used to compare old hashes, use {@link #hash(String, byte[])} instead.
     * 
     * @param password The password to hash.
     * @param salt     The salt used for hashing.
     * @return The hashed password.
     */
    @Deprecated
    public static byte[] hashOld(String password, byte[] salt) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_KEY_LENGTH);
        return keyFactory.generateSecret(spec).getEncoded();
    }

    /**
     * Derives an AES key from the master password (used as method to keep the key
     * secret) and salt (used to add randomness).
     * @deprecated Should only be used to read old passwords, use {@link #getKey(String, byte[])} instead.
     * 
     * @param masterPassword The master password to generate the key from.
     * @param salt          The salt used to encrypt.
     * @return The hashed password.
     * @throws InvalidKeySpecException if the key specification derived from the masterPassword is inappropriate
     */
    @Deprecated
    public static byte[] getKeyOld(@NotNull String masterPassword, byte[] salt) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), salt, ITERATIONS, AES_KEY_LENGTH);
        SecretKey secretKey = keyFactory.generateSecret(spec);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        return secretKeySpec.getEncoded();
    }
}
