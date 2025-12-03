package password.manager.app.enums;

import static password.manager.app.Utils.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.jetbrains.annotations.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import password.manager.app.interfaces.TriFunction;
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
@RequiredArgsConstructor
public enum SecurityVersion {
    @Deprecated
    PBKDF2((bits, password, salt) -> {
        final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, Parameters.PBKDF2_ITERATIONS, bits);
        SecretKey secretKey;
        try {
            secretKey = Parameters.getKeyFactory().generateSecret(spec);
            return secretKey.getEncoded();
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
            return null;
        }
    }),
    ARGON2((bits, password, salt) -> {
        final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(Parameters.ARGON2_PARALLELISM)
                .withMemoryAsKB(Parameters.ARGON2_MEMORY_KIB)
                .withIterations(Parameters.ARGON2_ITERATIONS)
                .build();

        reserveMemory(Parameters.ARGON2_MEMORY_KIB * 1024);

        final Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        releaseMemory(Parameters.ARGON2_MEMORY_KIB * 1024);

        final byte[] result = new byte[bits / 8];
        generator.generateBytes(password.toCharArray(), result);
        return result;
    });

    // Latest security version
    public static final SecurityVersion LATEST = ARGON2;

    // Hash and AES key sizes
    public static final int HASH_BITS = 512;
    public static final int AES_BITS = 256;

    private final TriFunction<Integer, String, byte[], byte[]> keyDerivationFunction;

    public static SecurityVersion fromString(String version) {
        return SecurityVersion.valueOf(version);
    }

    /**
     * Hashes the given password using the embedded key derivation function.
     *
     * @param password The password to hash.
     * @param salt     The salt used for hashing.
     * @return The hashed password.
     */
    public byte[] hash(@NotNull String password, byte[] salt) {
        return keyDerivationFunction.apply(HASH_BITS, password, salt);
    }

    /**
     * Derives an AES key from the master password using the embedded key derivation function.
     *
     * @param masterPassword The master password to generate the key from.
     * @param salt           The salt used for key derivation.
     * @return The derived AES key.
     */
    public byte[] getKey(@NotNull String masterPassword, byte[] salt) {
        final byte[] keyBytes = keyDerivationFunction.apply(AES_BITS, masterPassword, salt);
        final SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        return secretKeySpec.getEncoded();
    }

    /**
     * Holds parameters for this enum.
     */
    private static class Parameters {
        @Deprecated
        private static @Getter(value = AccessLevel.PRIVATE) SecretKeyFactory keyFactory;

        // PBKDF2 parameters
        @Deprecated
        private static final int PBKDF2_ITERATIONS = 65536;

        // Argon2 parameters
        private static final int ARGON2_MEMORY_KIB = 65536;       // 64MiB in KiB
        private static final int ARGON2_ITERATIONS = 4;       // Time cost
        private static final int ARGON2_PARALLELISM = 4;      // Parallelism factor

        static {
            try {
                keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            } catch (NoSuchAlgorithmException e) {
                Logger.getInstance().addError(e);
            }
        }
    }
}