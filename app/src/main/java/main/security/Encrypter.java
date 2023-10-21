package main.security;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Encrypter {
    private static SecretKeyFactory keyFactory;

    private static final int ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 512;
    private static final int AES_KEY_LENGTH = 256;

    static {
        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hashes the given password with salt using PBKDF2 hasing.
     * 
     * @param password The password to encrypt.
     * @param salt     The salt used to encrypt.
     * @return The hashed password.
     * @throws InvalidKeySpecException
     */
    public static byte[] hash(String password, byte[] salt) throws InvalidKeySpecException {
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
    public static byte[] getKey(String loginPassword, byte[] salt) throws InvalidKeySpecException {
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
     * @throws Exception
     */
    public static byte[] encryptAES(String password, byte[] key, byte[] iv) throws Exception {
        // Create Cipher object to encrypt
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

        // Encrypt the password
        return cipher.doFinal(password.getBytes());
    }

    /**
     * Decrypts and AES-encrypted password.
     * 
     * @param encryptedPasswordBase64 The encrypted password to decrypt.
     * @param key                     The AES key.
     * @param iv                      The initialization vector.
     * @return The decrypted password.
     * @throws Exception
     */
    public static String decryptAES(byte[] encryptedPassword, byte[] key, byte[] iv) throws Exception {
        // Create Cipher object to decrypt
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

        // Decrypt the password
        byte[] password = cipher.doFinal(encryptedPassword);

        // Convert it to String
        return new String(password);
    }
}
