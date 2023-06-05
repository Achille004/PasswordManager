package main.accounts;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import main.Utils.Encrypter;

public class LoginAccount implements Serializable {
    private String savingOrder;
    private String language;
    private String hashedPassword;
    private byte[] salt;

    public LoginAccount(String savingOrder, String language, String password) throws NoSuchAlgorithmException {
        this.savingOrder = savingOrder;
        this.language = language;

        this.salt = new byte[16];
        setPassword(password);
    }

    public String getSavingOrder() {
        return this.savingOrder;
    }

    public void setSavingOrder(String savingOrder) {
        this.savingOrder = savingOrder;
    }

    public LoginAccount savingOrder(String savingOrder) {
        setSavingOrder(savingOrder);
        return this;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LoginAccount language(String language) {
        setLanguage(language);
        return this;
    }

    public boolean verifyPassword(String passwordToVerify) {
        try {
            String hashedPasswordToVerify = Encrypter.hash(passwordToVerify, salt);

            if (hashedPassword.equals(hashedPasswordToVerify)) {
                return true;
            }
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void setPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            hashedPassword = Encrypter.hash(password, salt);

            // PROVA
            System.out.println("Password: " + hashedPassword);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public LoginAccount password(String hashedPassword) {
        setPassword(hashedPassword);
        return this;
    }

    public static LoginAccount createAccount(String savingOrder, String language, String password)
            throws NoSuchAlgorithmException {
        // creates the account, adding its attributes by constructor
        return new LoginAccount(savingOrder, language, password);
    }
}