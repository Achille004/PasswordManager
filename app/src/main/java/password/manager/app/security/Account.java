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
import java.security.NoSuchAlgorithmException;
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

    // Salt for deriving source key
    private @JsonProperty("salt") byte[] salt;

    // Encrypted fields and their salts used for key derivation
    private @JsonProperty("software") byte[] software;
    private final @JsonProperty("softIv") byte[] sIv;

    private @JsonProperty("username") byte[] username;
    private final @JsonProperty("userIv") byte[] uIv;

    private @JsonProperty("password") byte[] password;
    private final @JsonProperty("passIv") byte[] pIv;

    private final transient ReadOnlyStringWrapper softwareProperty = new ReadOnlyStringWrapper(),
                                                  usernameProperty = new ReadOnlyStringWrapper();

    // This flag is used to determine if this account was created with an older version where software and username were not encrypted.
    // It is set in the constructor and never updated, as it is only used to determine how to read existing data, while all new data is always fully encrypted.
    private transient boolean isFullyEncrypted;

    public Account() {
        this.salt = new byte[SALT_LENGTH];

        this.software = null;
        this.sIv = new byte[IV_LENGTH];

        this.username = null;
        this.uIv = new byte[IV_LENGTH];

        this.password = null;
        this.pIv = new byte[IV_LENGTH];

        // By default, we assume the account is not fully encrypted until proven otherwise (i.e. when reading data, if software and username salts are present, we set this flag to true)
        this.isFullyEncrypted = false;
    }

    public Account(@NotNull AccountData data, @NotNull byte[] DEK) throws GeneralSecurityException {
        if (data == null) throw new NullPointerException("Data cannot be null");
        if (DEK == null) throw new NullPointerException("Data encryption key cannot be null");

        this();

        // If this constructor is used, it means that the account is being created with the latest version, so we can safely set this flag to true
        this.isFullyEncrypted = true;
        this.setData(data, DEK);
    }

    private Account(
            @NotNull byte[] salt,
            @NotNull byte[] software, @NotNull byte[] sIv,
            @NotNull byte[] username, @NotNull byte[] uIv,
            @NotNull byte[] password, @NotNull byte[] pIv) throws GeneralSecurityException {

        // Here we consider salt as present as this constructor is used only for latest versions where salt is mandatory
        if (salt == null || salt.length != SALT_LENGTH) throw new IllegalArgumentException("Salt should be not null and " + SALT_LENGTH + " bytes long");
        if (software == null || software.length == 0) throw new IllegalArgumentException("Software cannot be null or empty");
        if (sIv == null || sIv.length != IV_LENGTH) throw new IllegalArgumentException("Software IV should be not null and " + IV_LENGTH + " bytes long");
        if (username == null || username.length == 0) throw new IllegalArgumentException("Username cannot be null or empty");
        if (uIv == null || uIv.length != IV_LENGTH) throw new IllegalArgumentException("Username IV should be not null and " + IV_LENGTH + " bytes long");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password cannot be null or empty");
        if (pIv == null || pIv.length != IV_LENGTH) throw new IllegalArgumentException("Password IV should be not null and " + IV_LENGTH + " bytes long");

        this();

        // Wrap the provided values into a memento and copy them to this account, centralizing the logic in copyMemento to avoid code duplication
        AccountMemento memento = new AccountMemento(salt, software, sIv, username, uIv, password, pIv);
        copyMemento(memento);

        // If this constructor is used, it means that software and username are encrypted, so we can safely set this flag to true
        this.isFullyEncrypted = true;
    }

    @Deprecated // This constructor is used only for backward compatibility
    private Account(@NotNull String software, @NotNull  String username, @NotNull byte[] encryptedPassword, @Nullable byte[] salt, @NotNull byte[] iv) {

        if (software == null || software.isEmpty()) throw new NullPointerException("Software cannot be null or empty");
        if (username == null || username.isEmpty()) throw new NullPointerException("Username cannot be null or empty");
        if (encryptedPassword == null || encryptedPassword.length == 0) throw new NullPointerException("Encrypted password cannot be null or empty");
        if (salt != null && salt.length != SALT_LENGTH) throw new IllegalArgumentException("Salt should be " + SALT_LENGTH + " bytes long, if provided");
        if (iv == null || iv.length != IV_LENGTH) throw new IllegalArgumentException("IV should be not null and " + IV_LENGTH + " bytes long");

        this();

        this.software = software.getBytes(StandardCharsets.UTF_8);
        this.username = username.getBytes(StandardCharsets.UTF_8);

        runOnFx(() -> {
            this.softwareProperty.set(software);
            this.usernameProperty.set(username);
        });

        // If salt is not provided, derive it from software and username (backward compatibility:
        // older versions didn't store salt and used software+username bytes as salt instead).
        // IMPORTANT: the derived value is written back into this.salt so it is persisted on the
        // next save, at which point the account behaves identically to a modern one.
        this.salt = (salt != null && salt.length == SALT_LENGTH) ? salt.clone() : (software + username).getBytes(StandardCharsets.UTF_8);
        System.arraycopy(iv, 0, this.pIv, 0, this.pIv.length);

        this.password = encryptedPassword;
    }

    /**
     * Unlocks this account by decrypting its data with the provided DEK.
     * If the account was created with an older version where software and username were not encrypted, this method also upgrades the account to the latest standard by encrypting all fields with the current one.
     * @param DEK the data encryption key to use for decryption
     * @param legacyVersion the security version to use for deriving the legacy key, can be null if this account is guaranteed to be created with the latest version
     * @param legacyMasterPassword the legacy master password to use for decryption if this account is not fully encrypted, can be null if all accounts are guaranteed to be created with the latest version
     * @throws GeneralSecurityException if decryption fails (e.g. due to wrong master password or corrupted data)
     */
    public void unlock(@NotNull byte[] DEK, @Nullable SecurityVersion legacyVersion, @Nullable String legacyMasterPassword) throws GeneralSecurityException {
        if (DEK == null) throw new NullPointerException("Data encryption key cannot be null");

        // If the account is not fully encrypted, it means that it was created with an older version where software and username were not encrypted.
        // In this case, we encrypt all fields with the current one to upgrade the account to the latest standard.
        final AccountData data;
        if (this.isFullyEncrypted) {
            data = getData(DEK);
        } else {
            if (legacyMasterPassword == null) throw new IllegalStateException("Legacy master password cannot be null when unlocking an account created with an older version");

            // Use old master password as DEK to read existing data
            byte[] legacyKey = legacyVersion.getKey(legacyMasterPassword, salt);
            data = getData(legacyKey);

            setData(data, DEK);
        }

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
    public AccountData getData(@NotNull byte[] DEK) throws GeneralSecurityException {
        if (DEK == null) throw new NullPointerException("Data encryption key cannot be null");

        String plainSoftware, plainUsername, plainPassword;

        readLock.lock();
        try {
            if (this.isFullyEncrypted) {
                // Decrypt all fields
                byte[] sKey = AES.derivateKey(DEK, salt, "software");
                plainSoftware = AES.decryptStringAES(software, sKey, sIv);

                byte[] uKey = AES.derivateKey(DEK, salt, "username");
                plainUsername = AES.decryptStringAES(username, uKey, uIv);

                byte[] pKey = AES.derivateKey(DEK, salt, "password");
                plainPassword = AES.decryptStringAES(password, pKey, pIv);
            } else {
                // Decrypt only password
                plainSoftware = new String(software, StandardCharsets.UTF_8);
                plainUsername = new String(username, StandardCharsets.UTF_8);
                plainPassword = AES.decryptStringAES(password, DEK, pIv); // Use DEK as it was with master password derived key
            }
        } finally {
            readLock.unlock();
        }

        return new AccountData(plainSoftware, plainUsername, plainPassword);
    }

    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static @NotNull Account of(@NotNull AccountData data, @NotNull byte[] DEK) throws GeneralSecurityException {
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (DEK == null) throw new NullPointerException("Data encryption key cannot be null");

        // creates the account, adding its attributes by constructor
        return new Account(data, DEK);
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
    void setData(@NotNull AccountData data, @NotNull byte[] DEK) throws GeneralSecurityException {
        if (data == null) throw new NullPointerException("Account data cannot be null");
        if (DEK == null) throw new NullPointerException("Data encryption key cannot be null");

        writeLock.lock();
        try {
            if (salt.length != SALT_LENGTH) salt = new byte[SALT_LENGTH]; // Avoid reallocating if not necessary

            SecureRandom random;
            try {
                random = SecureRandom.getInstanceStrong();
            } catch (NoSuchAlgorithmException e) {
                random = new SecureRandom();
            }

            // Generate unique salt
            random.nextBytes(salt);

            // Derive key
            software = encryptData(data.software(), DEK, salt, sIv, "software");
            username = encryptData(data.username(), DEK, salt, uIv, "username");
            password = encryptData(data.password(), DEK, salt, pIv, "password");

            this.isFullyEncrypted = true;

            // Update properties for UI
            runOnFx(() -> {
                softwareProperty.set(data.software());
                usernameProperty.set(data.username());
            });
        } finally {
            writeLock.unlock();
        }
    }

    private byte[] encryptData(String newVal, byte[] sourceKey, byte[] salt, byte[] iv, String info) throws GeneralSecurityException {
        SecureRandom random;
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            random = new SecureRandom();
        }

        // Generate IV
        random.nextBytes(iv);

        // Derive key and encrypt
        final byte[] key = AES.derivateKey(sourceKey, salt, info);
        return AES.encryptStringAES(newVal, key, iv);
    }

    /**
     * Captures the current state of this account for rollback purposes.
     * @return a memento object containing the account's current state
     */
    @NotNull AccountMemento captureState() {
        readLock.lock();
        try {
            return new AccountMemento(
                this.salt.clone(),
                this.software.clone(),
                this.sIv.clone(),
                this.username.clone(),
                this.uIv.clone(),
                this.password.clone(),
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

        this.salt = memento.salt().clone();
        this.software = memento.software().clone();
        System.arraycopy(memento.sIv(), 0, this.sIv, 0, this.sIv.length);
        this.username = memento.username().clone();
        System.arraycopy(memento.uIv(), 0, this.uIv, 0, this.uIv.length);
        this.password = memento.password().clone();
        System.arraycopy(memento.pIv(), 0, this.pIv, 0, this.pIv.length);
    }

    /**
     * Memento record that captures the state of an Account for rollback purposes.
     * This implements the Memento pattern for transactional support.
     */
    public record AccountMemento(
        @NotNull byte[] salt,
        @NotNull byte[] software, @NotNull byte[] sIv,
        @NotNull byte[] username, @NotNull byte[] uIv,
        @NotNull byte[] password, @NotNull byte[] pIv
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
                // If encryptedPassword is present, it's the legacy format; otherwise, it's the modern format
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
                    byte[] salt = node.get("salt").binaryValue();
                    byte[] software = node.get("software").binaryValue();
                    byte[] softIv = node.get("softIv").binaryValue();
                    byte[] username = node.get("username").binaryValue();
                    byte[] userIv = node.get("userIv").binaryValue();
                    byte[] password = node.get("password").binaryValue();
                    byte[] passIv = node.get("passIv").binaryValue();
                    return new Account(salt, software, softIv, username, userIv, password, passIv);
                }
            } catch (GeneralSecurityException e) {
                throw ctxt.instantiationException(Account.class, e);
            }
        }
    }
}