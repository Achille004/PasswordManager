package main;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import main.security.Encrypter;

class AppTest {
    @Test
    void testAES() throws Exception {
        String loginPassword = "logPass";
        String s = "soft", u = "user", p = "pass";

        // Generate IV
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Generate salt
        byte[] salt = (s + u).getBytes();

        byte[] key = Encrypter.getKey(loginPassword, salt);

        byte[] e = Encrypter.encryptAES(p, key, iv);
        String d = Encrypter.decryptAES(e, key, iv);
        assertEquals(p, d, "Crittografia fallita");
    }
}
