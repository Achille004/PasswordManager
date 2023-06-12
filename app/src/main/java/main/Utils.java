package main;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JPanel;

import main.accounts.Account;

public class Utils {
    /**
     * Replaces a panel with another one.
     *
     * @param actingPanel The panel on which will be done the replacement
     * @param showPanel   The new panel
     */
    public static void replacePanel(JPanel actingPanel, JPanel showPanel) {
        // removing old panel
        actingPanel.removeAll();
        actingPanel.repaint();
        actingPanel.revalidate();

        // adding new panel
        actingPanel.add(showPanel);
        actingPanel.repaint();
        actingPanel.revalidate();
    }

    public static String addZerosToIndex(int listSize, int ìndex) {
        int cifreLista = (int) Math.log10(listSize) + 1;
        return String.format("%0" + cifreLista + "d", ìndex);
    }

    public static class Exporter {
        public static String exportHtml(ArrayList<Account> accountList, String language, String loginPassword) {
            StringBuilder stb = new StringBuilder();

            stb.append("<!DOCTYPE html>\n<html>\n<style>\n");

            // css
            stb.append("""
                    body {
                        background-color: rgb(51,51,51);
                        color: rgb(204,204,204);
                        margin: 1em;
                    }

                    table, th, td {
                        border: 0.1em solid rgb(204,204,204);
                        border-collapse: collapse;
                    }

                    th, td {
                        padding: 1em;
                    }
                    """);

            stb.append("\n</style>\n\n<body>\n<table style=\"width:100%\">");

            switch (language) {
                case "e" -> {
                    stb.append(
                            "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Username</th>\n<th>Password</th>\n</tr>");
                }

                case "i" -> {
                    stb.append(
                            "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Nome Utente</th>\n<th>Password</th>\n</tr>");
                }
            }

            final int listSize = accountList.size();
            int counter = 0;
            for (Account currentAccount : accountList) {
                counter++;
                stb.append("<tr>\n<td>" + addZerosToIndex(listSize, counter) +
                        "</td>\n<td>" + currentAccount.getSoftware() +
                        "</td>\n<td>" + currentAccount.getUsername() +
                        "</td>\n<td>" + currentAccount.getPassword(loginPassword) +
                        "</td>\n</tr>");
            }

            stb.append("</table>\n</body>\n</html>");

            return stb.toString();
        }

        public static String exportCsv(ArrayList<Account> accountList, String language, String loginPassword) {
            StringBuilder stb = new StringBuilder();

            final int listSize = accountList.size();
            int counter = 0;
            for (Account currentAccount : accountList) {
                counter++;
                stb.append(addZerosToIndex(listSize, counter) + "," +
                        currentAccount.getSoftware() + "," +
                        currentAccount.getUsername() + "," +
                        currentAccount.getPassword(loginPassword) + "\n");
            }

            return stb.toString();
        }
    }

    public static class Encrypter {
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

        public static String hash(String password, byte[] salt) throws InvalidKeySpecException {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_KEY_LENGTH);
            byte[] hashedPasswordBytes = keyFactory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hashedPasswordBytes);
        }

        public static byte[] getKey(String loginPassword, byte[] salt) throws InvalidKeySpecException {
            KeySpec spec = new PBEKeySpec(loginPassword.toCharArray(), salt, ITERATIONS, AES_KEY_LENGTH);
            SecretKey secretKey = keyFactory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            return secretKeySpec.getEncoded();
        }

        public static String encryptAES(String password, byte[] key, byte[] iv) throws Exception {
            // Create Cipher object to encrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            // Encrypt the password
            byte[] encryptedPassword = cipher.doFinal(password.getBytes());

            // Base64-encode the encrypted password for a readable representation
            return Base64.getEncoder().encodeToString(encryptedPassword);
        }

        public static String decryptAES(String encryptedPasswordBase64, byte[] key, byte[] iv) throws Exception {
            // Create Cipher object to decrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            // Base64-decode the encoded encrpted password for a decryptable form
            byte[] encryptedPassword = Base64.getDecoder().decode(encryptedPasswordBase64);

            // Decrypt the password
            byte[] password = cipher.doFinal(encryptedPassword);

            // Convert it to String
            return new String(password);
        }
    }
}
