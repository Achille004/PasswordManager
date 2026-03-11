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

import static password.manager.app.Utils.runOnFx;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.ObjectNode;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import password.manager.app.base.SecurityVersion;

@JsonDeserialize(using = Account.Deserializer.class)
public final class Account {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    private final transient ReadWriteLock lock = new ReentrantReadWriteLock(false);
    private final transient Lock readLock = lock.readLock();
    private final transient Lock writeLock = lock.writeLock();

    // JsonProperty is redundant if there are getters
    private final transient ReadOnlyStringWrapper softwareProperty = new ReadOnlyStringWrapper(),
                                                  usernameProperty = new ReadOnlyStringWrapper();

    // Salt for deriving source key
    private @JsonProperty("keySalt") byte[] keySalt;

    // Encrypted fields and their salts used for key derivation
    private @JsonProperty("software") byte[] software;
    private final @JsonProperty("sSalt") byte[] sSalt;
    private final @JsonProperty("sIv") byte[] sIv;

    private @JsonProperty("username") byte[] username;
    private final @JsonProperty("uSalt") byte[] uSalt;
    private final @JsonProperty("uIv") byte[] uIv;

    private @JsonProperty("password") byte[] password;
    private @JsonProperty("pSalt") byte[] pSalt;
    private final @JsonProperty("pIv") byte[] pIv;
    
    // This flag is used to determine if this account was created with an older security version where software and username were not encrypted.
    // It is set in the constructor and never updated, as it is only used to determine how to read existing data, while all new data is always fully encrypted.
    private transient boolean isFullyEncrypted; 

    public Account() {
        this.keySalt = new byte[SALT_LENGTH];

        this.software = null;
        this.sSalt = new byte[SALT_LENGTH];
        this.sIv = new byte[IV_LENGTH];

        this.username = null;
        this.uSalt = new byte[SALT_LENGTH];
        this.uIv = new byte[IV_LENGTH];

        this.password = null;
        this.pSalt = new byte[SALT_LENGTH];
        this.pIv = new byte[IV_LENGTH];

        // By default, we assume the account is not fully encrypted until proven otherwise (i.e. when reading data, if software and username salts are present, we set this flag to true)
        this.isFullyEncrypted = false;
    }

    public Account(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        this();

        // If this constructor is used, it means that the account is being created with the latest security version, so we can safely set this flag to true
        this.isFullyEncrypted = true; 
        this.setData(securityVersion, data, masterPassword);
    }

    private Account(
            @NotNull byte[] keySalt,
            @NotNull byte[] software, @NotNull byte[] sSalt, @NotNull byte[] sIv,
            @NotNull byte[] username, @NotNull byte[] uSalt, @NotNull byte[] uIv,
            @NotNull byte[] password, @NotNull byte[] pSalt, @NotNull byte[] pIv) throws GeneralSecurityException {

        // Here we consider salt as present as this constructor is used only for latest security versions where salt is mandatory
        if (keySalt == null || keySalt.length != SALT_LENGTH) throw new IllegalArgumentException("Key salt should be not null and " + SALT_LENGTH + " bytes long");
        if (software == null || software.length == 0) throw new IllegalArgumentException("Software cannot be null or empty");
        if (sSalt == null || sSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Software salt should be not null and " + SALT_LENGTH + " bytes long");
        if (sIv == null || sIv.length != IV_LENGTH) throw new IllegalArgumentException("Software IV should be not null and " + IV_LENGTH + " bytes long");
        if (username == null || username.length == 0) throw new IllegalArgumentException("Username cannot be null or empty");
        if (uSalt == null || uSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Username salt should be not null and " + SALT_LENGTH + " bytes long");
        if (uIv == null || uIv.length != IV_LENGTH) throw new IllegalArgumentException("Username IV should be not null and " + IV_LENGTH + " bytes long");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password cannot be null or empty");
        if (pSalt == null || pSalt.length != SALT_LENGTH) throw new IllegalArgumentException("Password salt should be not null and " + SALT_LENGTH + " bytes long");
        if (pIv == null || pIv.length != IV_LENGTH) throw new IllegalArgumentException("Password IV should be not null and " + IV_LENGTH + " bytes long");
                
        this();

        // Wrap the provided values into a memento and copy them to this account, centralizing the logic in copyMemento to avoid code duplication
        AccountMemento memento = new AccountMemento(keySalt, software, sSalt, sIv, username, uSalt, uIv, password, pSalt, pIv);
        copyMemento(memento);

        // If this constructor is used, it means that software and username are encrypted, as the presence of their salts is mandatory, so we can safely set this flag to true
        this.isFullyEncrypted = true;
    }

    @Deprecated // This constructor is used only for backward compatibility
    private Account(@NotNull String software, @NotNull  String username, @NotNull byte[] encryptedPassword, @Nullable byte[] keySalt, @NotNull byte[] iv) {

        if (software == null || software.isEmpty()) throw new NullPointerException("Software cannot be null or empty");
        if (username == null || username.isEmpty()) throw new NullPointerException("Username cannot be null or empty");
        if (encryptedPassword == null || encryptedPassword.length == 0) throw new NullPointerException("Encrypted password cannot be null or empty");
        if (keySalt != null && keySalt.length != SALT_LENGTH) throw new IllegalArgumentException("Key salt should be " + SALT_LENGTH + " bytes long, if provided");
        if (iv == null || iv.length != IV_LENGTH) throw new IllegalArgumentException("IV should be not null and " + IV_LENGTH + " bytes long");

        this();

        this.software = software.getBytes(StandardCharsets.UTF_8);
        this.username = username.getBytes(StandardCharsets.UTF_8);

        runOnFx(() -> {
            this.softwareProperty.set(software);
            this.usernameProperty.set(username);
        });
        
        // If keySalt is not provided, derive it from software and username (backward compatibility:
        // older versions didn't store keySalt and used software+username bytes as keySalt instead).
        // IMPORTANT: the derived value is written back into this.keySalt so it is persisted on the
        // next save, at which point the account behaves identically to a modern one.
        this.keySalt = (keySalt != null && keySalt.length == SALT_LENGTH) ? keySalt.clone() : (software + username).getBytes(StandardCharsets.UTF_8);
        System.arraycopy(iv, 0, this.pIv, 0, this.pIv.length);

        this.password = encryptedPassword;
    }

    /**
     * Unlocks this account by decrypting its data with the provided master password and security version.
     * If the account was created with an older security version where software and username were not encrypted, this method also upgrades the account to the latest standard by encrypting all fields with the current security version.
     * @param securityVersion the security version to use for decryption and potential upgrade
     * @param masterPassword the master password to use for decryption
     * @throws GeneralSecurityException if decryption fails (e.g. due to wrong master password or corrupted data)
     */
    public void unlock(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        final AccountData data = getData(securityVersion, masterPassword);

        // If the account is not fully encrypted, it means that it was created with an older security version where software and username were not encrypted.
        // In this case, we encrypt all fields with the current security version to upgrade the account to the latest standard.
        if (!isFullyEncrypted) setData(securityVersion, data, masterPassword);

        // Update properties for UI
        runOnFx(() -> {
            softwareProperty.set(data.software());
            usernameProperty.set(data.username());
        });
    }

    /**
     * Gets the software name property for this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the software name property, read-only
     */
    public ReadOnlyProperty<String> softwareProperty() {
        return softwareProperty.getReadOnlyProperty();
    }

    /**
     * Gets the software name associated with this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the software name
     */
    @JsonIgnore
    public String getSoftware() {
        return softwareProperty.get();
    }

    /**
     * Gets the username property for this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the username property, read-only
     */
    public ReadOnlyProperty<String> usernameProperty() {
        return usernameProperty.getReadOnlyProperty();
    }

    /**
     * Gets the username associated with this account.
     * This is a UI-facing property and therefore its updates should are considered low-priority.
     * @return the username
     */
    @JsonIgnore
    public String getUsername() {
        return usernameProperty.get();
    }

    @JsonIgnore
    public AccountData getData(@NotNull SecurityVersion securityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        String plainSoftware, plainUsername, plainPassword;

        readLock.lock();
        try {
            byte[] key = securityVersion.getKey(masterPassword, keySalt);

            // If software or username salt are null, it means that this account was created with
            // an older security version where these fields were not encrypted.
            if (isFullyEncrypted) {
                // Decrypt all fields
                byte[] sKey = AES.derivateKey(key, sSalt, "software");
                plainSoftware = AES.decryptAES(software, sKey, sIv);

                byte[] uKey = AES.derivateKey(key, uSalt, "username");
                plainUsername = AES.decryptAES(username, uKey, uIv);

                byte[] pKey = AES.derivateKey(key, pSalt, "password");
                plainPassword = AES.decryptAES(password, pKey, pIv);
            } else {
                // Decrypt only password
                plainSoftware = new String(software, StandardCharsets.UTF_8);
                plainUsername = new String(username, StandardCharsets.UTF_8);
                plainPassword = AES.decryptAES(password, key, pIv);
            }
        } finally {
            readLock.unlock();
        }

        return new AccountData(plainSoftware, plainUsername, plainPassword);
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        // creates the account, adding its attributes by constructor
        return new Account(securityVersion, data, masterPassword);
    }

    /**
     * Wrapper record used for moving data in-and-out of this account.
     * This holds sensitive data and should not be passed around.
     */
    public record AccountData(
        @NotNull String software,
        @NotNull String username,
        @NotNull String password
    ) {}

    // #region Package-private methods (exposed to AccountRepository)
    void setData(@NotNull SecurityVersion securityVersion, @NotNull AccountData data, @NotNull String masterPassword) throws GeneralSecurityException {
        if (securityVersion == null) throw new NullPointerException("Security version cannot be null");
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            if (pSalt.length != SALT_LENGTH) pSalt = new byte[SALT_LENGTH]; // Avoid reallocating if not necessary

            // Derive key
            final byte[] key = securityVersion.getKey(masterPassword, keySalt);
            
            software = encryptData(securityVersion, data.software(), key, sSalt, sIv, "software");
            username = encryptData(securityVersion, data.username(), key, uSalt, uIv, "username");
            password = encryptData(securityVersion, data.password(), key, pSalt, pIv, "password");

            // Update properties for UI
            runOnFx(() -> {
                softwareProperty.set(data.software());
                usernameProperty.set(data.username());
            });
        } finally {
            writeLock.unlock();
        }
    }

    private byte[] encryptData(@NotNull SecurityVersion securityVersion, String newVal, byte[] sourceKey, byte[] salt, byte[] iv, String info) throws GeneralSecurityException {
        // Generate salt and IV
        final SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derive key and encrypt password
        final byte[] key = AES.derivateKey(sourceKey, salt, info);
        return AES.encryptAES(newVal, key, iv);
    }

    void updateSecurityVersion(@NotNull SecurityVersion oldSecurityVersion, @NotNull SecurityVersion newSecurityVersion, @NotNull String masterPassword) throws GeneralSecurityException {
        if (oldSecurityVersion == null) throw new NullPointerException("Old security version cannot be null");
        if (newSecurityVersion == null) throw new NullPointerException("New security version cannot be null");
        if (masterPassword == null) throw new NullPointerException("Master password cannot be null");

        writeLock.lock();
        try {
            final AccountData data = getData(oldSecurityVersion, masterPassword);
            setData(newSecurityVersion, data, masterPassword);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Captures the current state of this account for rollback purposes.
     * @return a memento object containing the account's current state
     */
    @NotNull AccountMemento captureState() {
        readLock.lock();
        try {
            return new AccountMemento(
                this.keySalt.clone(),
                this.software.clone(),
                this.sSalt.clone(),
                this.sIv.clone(),
                this.username.clone(),
                this.uSalt.clone(),
                this.uIv.clone(),
                this.password.clone(),
                this.pSalt.clone(),
                this.pIv.clone()
            );
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Restores the account's state from a memento.
     * @param memento the memento containing the state to restore
     */
    void restoreState(@NotNull AccountMemento memento) {
        if (memento == null) throw new NullPointerException("Memento cannot be null");

        writeLock.lock();
        try {
            copyMemento(memento);
        } finally {
            writeLock.unlock();
        }
    }

    private void copyMemento(AccountMemento memento) {
        runOnFx(() -> {
            // Invalidate properties to prevent reading inconsistent data during the copy
            this.softwareProperty.set("unavailable while locked");
            this.usernameProperty.set("unavailable while locked");
        });

        this.keySalt = memento.keySalt().clone();
        this.software = memento.software().clone();
        System.arraycopy(memento.sSalt(), 0, this.sSalt, 0, this.sSalt.length);
        System.arraycopy(memento.sIv(), 0, this.sIv, 0, this.sIv.length);
        this.username = memento.username().clone();
        System.arraycopy(memento.uSalt(), 0, this.uSalt, 0, this.uSalt.length);
        System.arraycopy(memento.uIv(), 0, this.uIv, 0, this.uIv.length);
        this.password = memento.password().clone();
        System.arraycopy(memento.pSalt(), 0, this.pSalt, 0, this.pSalt.length);
        System.arraycopy(memento.pIv(), 0, this.pIv, 0, this.pIv.length);
    }

    /**
     * Memento record that captures the state of an Account for rollback purposes.
     * This implements the Memento pattern for transactional support.
     */
    public record AccountMemento(
        @NotNull byte[] keySalt,
        @NotNull byte[] software, @NotNull byte[] sSalt, @NotNull byte[] sIv,
        @NotNull byte[] username, @NotNull byte[] uSalt, @NotNull byte[] uIv,
        @NotNull byte[] password, @NotNull byte[] pSalt, @NotNull byte[] pIv
    ) {}
    // #endregion

    /**
     * Custom Jackson deserializer that distinguishes between the legacy (v1) format,
     * where {@code software} and {@code username} were plain strings and only the password
     * was encrypted, and the modern format where all three fields are encrypted byte arrays.
     * The discriminator is the presence of the {@code encryptedPassword} property.
     */
    public static final class Deserializer extends ValueDeserializer<Account> {
        @Override
        public Account deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            ObjectNode node = p.readValueAsTree();

            try {
                if (node.has("encryptedPassword")) {
                    // Legacy format: software/username are plain strings, only password is encrypted
                    String software = node.get("software").asString();
                    String username = node.get("username").asString();
                    byte[] encryptedPassword = node.get("encryptedPassword").binaryValue();
                    byte[] salt = node.has("salt") ? node.get("salt").binaryValue() : null;
                    byte[] iv = node.get("iv").binaryValue();
                    return new Account(software, username, encryptedPassword, salt, iv);
                } else {
                    // Modern format: all fields are encrypted byte arrays
                    byte[] keySalt = node.get("keySalt").binaryValue();
                    byte[] software = node.get("software").binaryValue();
                    byte[] sSalt = node.get("sSalt").binaryValue();
                    byte[] sIv = node.get("sIv").binaryValue();
                    byte[] username = node.get("username").binaryValue();
                    byte[] uSalt = node.get("uSalt").binaryValue();
                    byte[] uIv = node.get("uIv").binaryValue();
                    byte[] password = node.get("password").binaryValue();
                    byte[] pSalt = node.get("pSalt").binaryValue();
                    byte[] pIv = node.get("pIv").binaryValue();
                    return new Account(keySalt, software, sSalt, sIv, username, uSalt, uIv, password, pSalt, pIv);
                }
            } catch (GeneralSecurityException e) {
                throw ctxt.instantiationException(Account.class, e);
            }
        }
    }
}