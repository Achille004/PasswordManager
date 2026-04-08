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
package password.manager.app.base

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import password.manager.app.security.AES
import password.manager.app.singletons.Logger
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.Function3

/**
 * Cryptographic utility class for password hashing and key derivation.
 * @param hashingFunction The hashing function used to hash passwords.
 */
enum class SecurityVersion(
    private val hashingFunction: Function3<Int, String, ByteArray, ByteArray>
) {
    @Deprecated("Older security version, not used anymore.")
    PBKDF2({ bits: Int, field: String, salt: ByteArray ->
        val keyFactory: SecretKeyFactory?
        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        } catch (e: NoSuchAlgorithmException) {
            Logger.getInstance().addError(e)
            throw RuntimeException(e)
        }

        val spec: KeySpec = PBEKeySpec(field.toCharArray(), salt, PBKDF2_ITERATIONS, bits)
        try {
            val secretKey = keyFactory.generateSecret(spec)
            secretKey.encoded
        } catch (e: InvalidKeySpecException) {
            Logger.getInstance().addError(e)
            throw RuntimeException(e)
        }
    }),
    ARGON2({ bits: Int, field: String, salt: ByteArray ->
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withParallelism(ARGON2_PARALLELISM)
            .withMemoryAsKB(ARGON2_MEMORY_KIB)
            .withIterations(ARGON2_ITERATIONS)
            .withSalt(salt)
            .build()
        val generator = Argon2BytesGenerator()

        /*
         * From empirical testing, about 69.47 MB of memory is allocated during Argon2 initialization with the above parameters.
         * This translates to about 67.84 MiB, which is slightly above the 64 MiB specified in Parameters.ARGON2_MEMORY_KIB due to overhead.
         */
        generator.init(params)

        val result = ByteArray(bits / 8)
        generator.generateBytes(field.toCharArray(), result)
        result
    });

    /**
     * Hashes the given password using the embedded hashing function.
     * 
     * @param masterPassword The master password to hash.
     * @param salt  The salt used for hashing.
     * @return The hashed master password.
     */
    fun hash(masterPassword: String, salt: ByteArray) = hashingFunction.invoke(HASH_BITS, masterPassword, salt)

    /**
     * Derives an AES key from the master password using the embedded hashing function.
     * 
     * @param masterPassword The master password to derive the key from.
     * @param salt  The salt used for key derivation.
     * @return The derived AES key.
     */
    fun getKey(masterPassword: String, salt: ByteArray): ByteArray {
        val keyBytes: ByteArray = hashingFunction.invoke(KEY_BITS, masterPassword, salt)
        val secretKeySpec = SecretKeySpec(keyBytes, "AES")
        return secretKeySpec.encoded
    }

    companion object {
        // PBKDF2 parameters
        @Deprecated("PBKDF2 parameter.")
        private const val PBKDF2_ITERATIONS = 65536

        // Argon2 parameters
        private const val ARGON2_MEMORY_KIB = 65536 // 64MiB in KiB
        private const val ARGON2_ITERATIONS = 4 // Time cost
        private const val ARGON2_PARALLELISM = 4 // Parallelism factor

        // Latest security version
        @JvmField
        val LATEST: SecurityVersion = ARGON2

        // Hash and key sizes
        const val HASH_BITS: Int = 512
        const val KEY_BITS: Int = AES.AES_BITS

        @JvmStatic
        fun fromString(version: String) = SecurityVersion.valueOf(version)
    }
}