package password.manager.app.enums;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import password.manager.app.interfaces.TriFunction;
import password.manager.app.singletons.Logger;

import static password.manager.app.security.Encrypter.*;

@RequiredArgsConstructor
public enum SecurityVersion {
    @Deprecated
    PBKDF2((bits, password, salt) -> {
        final KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, bits);
        SecretKey secretKey;
        try {
            secretKey = getKeyFactory().generateSecret(spec);
            return secretKey.getEncoded();
        } catch (InvalidKeySpecException e) {
            Logger.getInstance().addError(e);
            return null;
        }
    }),
    ARGON2((bits, password, salt) -> {
        final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(ARGON2_PARALLELISM)
            .withMemoryAsKB(ARGON2_MEMORY)
            .withIterations(ARGON2_ITERATIONS)
            .build();

        final Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        final byte[] result = new byte[bits / 8];
        generator.generateBytes(password.toCharArray(), result);
        return result;
    });

    // Latest security version
    public static final SecurityVersion LATEST = ARGON2;

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
}