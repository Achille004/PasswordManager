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
package password.manager.app.security

import org.bouncycastle.crypto.CryptoServicePurpose
import org.bouncycastle.crypto.DerivationParameters
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Provides AES encryption and decryption methods, powered by Bouncy Castle.
 */
object AES {
    init {
        Security.addProvider(BouncyCastleProvider())
        Security.setProperty("crypto.policy", "unlimited")
    }

    const val AES_BITS: Int = 256
    const val GCM_TAG_BITS: Int = 128 // 16 bytes for GCM TAG

    /**
     * The process-wide strong random number generator.
     *
     * It is initialized lazily so that the seeding, which can block waiting for entropy on some platforms, happens on
     * first use rather than when this class is loaded.
     */
    @JvmStatic
    val strongRandom: SecureRandom by lazy {
        try {
            SecureRandom.getInstanceStrong()
        } catch (_: NoSuchAlgorithmException) {
            SecureRandom()
        }
    }

    /**
     * Derives a key from the given source key, salt, and additional information using the HKDF (HMAC-based Key Derivation Function) algorithm.
     *
     * @param sourceKey The source key material (input-keying material) as a byte array.
     * @param salt The optional salt value used for key derivation, provided as a byte array.
     * @param info The context and application-specific information string associated with the key derivation process.
     * @return The derived key as a byte array.
     */
    @JvmStatic
    fun deriveKey(sourceKey: ByteArray, salt: ByteArray, info: String): ByteArray {
        val digest: Digest = SHA256Digest.newInstance(CryptoServicePurpose.KEYGEN)
        val params: DerivationParameters = HKDFParameters(sourceKey, salt, info.toByteArray(StandardCharsets.UTF_8))

        return ByteArray(AES_BITS / 8).also { out: ByteArray ->
            HKDFBytesGenerator(digest).run {
                init(params)
                generateBytes(out, 0, out.size)
            }
        }
    }

    /**
     * Performs an AES encryption or decryption action in GCM mode with no padding.
     *
     * @param opmode The operation mode of the cipher. Use `Cipher.ENCRYPT_MODE` for encryption
     *               and `Cipher.DECRYPT_MODE` for decryption.
     * @param value The byte array containing the input data to be encrypted or decrypted.
     * @param key The byte array representing the AES key to be used for the operation.
     * @param iv The byte array representing the initialization vector (IV) to be used in GCM mode.
     * @return The resulting byte array after the encryption or decryption operation.
     * @throws GeneralSecurityException If any error occurs during the cryptographic operation.
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    private fun aesAction(opmode: Int, value: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // Create Key and AlgorithmParameterSpec objects
        val secretKey: Key = SecretKeySpec(key, "AES")
        val gcmSpec: AlgorithmParameterSpec = GCMParameterSpec(GCM_TAG_BITS, iv)

        return Cipher.getInstance("AES/GCM/NoPadding", "BC").run {
            // Initialize
            init(opmode, secretKey, gcmSpec)
            // Run the action
            doFinal(value)
        }
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
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun encryptAES(value: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = aesAction(
        Cipher.ENCRYPT_MODE,
        value, key, iv
    )

    /**
     * Decrypts an AES-encrypted value.
     *
     * @param encryptedValue    The encrypted value to decrypt.
     * @param key               The AES key.
     * @param iv                The initialization vector.
     * @return                  The decrypted value.
     * @throws GeneralSecurityException
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun decryptAES(encryptedValue: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = aesAction(
        Cipher.DECRYPT_MODE,
        encryptedValue, key, iv
    )

    /**
     * Shorthand method to encrypt a string using AES, see [encryptAES].
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun encryptStringAES(value: String, key: ByteArray, iv: ByteArray): ByteArray =
        encryptAES(value.toByteArray(StandardCharsets.UTF_8), key, iv)

    /**
     * Shorthand method to decrypt a string using AES, see [decryptAES].
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class)
    fun decryptStringAES(encryptedValue: ByteArray, key: ByteArray, iv: ByteArray): String =
        String(decryptAES(encryptedValue, key, iv), StandardCharsets.UTF_8)
}
