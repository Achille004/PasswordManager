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

package password.manager.app.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Security;

import javax.crypto.Cipher;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.CryptoServicePurpose;
import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
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

    public static final int AES_BITS = 256;
    public static final int GCM_IV_BITS = 128; // 12 bytes for GCM IV

    public static byte[] derivateKey(@NotNull byte[] sourceKey, @NotNull byte[] salt, @NotNull String info) {
        Digest digest = SHA256Digest.newInstance(CryptoServicePurpose.KEYGEN);
        DerivationParameters params = new HKDFParameters(sourceKey, salt, info.getBytes(StandardCharsets.UTF_8));

        HKDFBytesGenerator generator = new HKDFBytesGenerator(digest);
        generator.init(params);

        byte[] derivedKey = new byte[AES_BITS / 8];
        generator.generateBytes(derivedKey, 0, derivedKey.length);
        return derivedKey;
    }

    /**
     * Uses AES to encrypt a value.
     *
     * @param value    The value to encrypt.
     * @param key      The AES key.
     * @param iv       The initialization vector.
     * @return         The encrypted value.
     * @throws GeneralSecurityException
     */
    public static byte[] encryptAES(@NotNull String value, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Key and AlgorithmParameterSpec objects
        Key secretKey = new SecretKeySpec(key, "AES");
        AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(GCM_IV_BITS, iv);

        // Create Cipher object to encrypt
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        // Get the password bytes
        byte[] passwordBytes = value.getBytes(StandardCharsets.UTF_8);

        // Encrypt them
        return cipher.doFinal(passwordBytes);
    }

    /**
     * Decrypts an AES-encrypted value.
     *
     * @param encryptedValue    The encrypted value to decrypt.
     * @param key               The AES key.
     * @param iv                The initialization vector.
     * @return                  The decrypted value.
     * @throws GeneralSecurityException
     */
    public static @NotNull String decryptAES(byte[] encryptedValue, byte[] key, byte[] iv) throws GeneralSecurityException {
        // Create Key and AlgorithmParameterSpec objects
        Key secretKey = new SecretKeySpec(key, "AES");
        AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(GCM_IV_BITS, iv);

        // Create Cipher object to decrypt
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        // Decrypt the value
        final byte[] password = cipher.doFinal(encryptedValue);

        // Convert it to String
        return new String(password, StandardCharsets.UTF_8);
    }
}
