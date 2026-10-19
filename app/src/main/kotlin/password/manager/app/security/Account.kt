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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import password.manager.app.Utils.runOnFx
import password.manager.app.base.SecurityVersion
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.node.ObjectNode
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

@JsonDeserialize(using = Account.Deserializer::class)
class Account {
    @Transient
    private val lock: ReadWriteLock = ReentrantReadWriteLock(false)

    @Transient
    private val readLock: Lock = lock.readLock()

    @Transient
    private val writeLock: Lock = lock.writeLock()

    // Salt used to derive the per-field keys from the DEK.
    @JsonProperty("kdfSalt")
    private var kdfSalt: ByteArray

    // Encrypted fields and their salts used for key derivation.
    // An empty array means "not set yet": it is never a valid ciphertext, since every constructor that accepts
    // external data rejects empty values, so there is no need for a nullable type here.
    @JsonProperty("software")
    private var encSoftware: ByteArray

    @JsonProperty("softIv")
    private val sIv: ByteArray

    @JsonProperty("username")
    private var encUsername: ByteArray

    @JsonProperty("userIv")
    private val uIv: ByteArray

    @JsonProperty("password")
    private var encPassword: ByteArray

    @JsonProperty("passIv")
    private val pIv: ByteArray

    // Writable backing wrappers for the software and username properties exposed at runtime.
    // They are initialized to an empty string so that the plain values exposed to the UI are never null.
    @Transient
    private val softwareWrapper = ReadOnlyStringWrapper("")

    @Transient
    private val usernameWrapper = ReadOnlyStringWrapper("")

    // This flag is used to determine if this account was created with an older version where software and username were not encrypted.
    // It is set in the constructor, turned on by setData once the account is written in the latest format, and restored
    // by copyMemento on rollback, so it must always travel together with the encrypted fields it describes.
    @Transient
    private var isFullyEncrypted = false

    /**
     * Creates an empty account with default cryptographic fields.
     *
     * This is the single initialization point of the class: every other constructor delegates to it and then fills in
     * the fields, so an account in this empty state is never observable from the outside.
     *
     * The account is initially considered not fully encrypted. This flag is set to true when the account is created
     * using the latest format or when encrypted software and username data are available while reading data.
     */
    private constructor() {
        this.kdfSalt = ByteArray(SALT_LENGTH)

        this.encSoftware = ByteArray(0)
        this.sIv = ByteArray(IV_LENGTH)

        this.encUsername = ByteArray(0)
        this.uIv = ByteArray(IV_LENGTH)

        this.encPassword = ByteArray(0)
        this.pIv = ByteArray(IV_LENGTH)

        // By default, we assume the account is not fully encrypted until proven otherwise (i.e., when reading data, if software and username salts are present, we set this flag to true)
        this.isFullyEncrypted = false
    }

    /**
     * Creates a new account from the provided data using the specified data encryption key.
     *
     * Since this constructor is intended for accounts created using the latest security version, the account is
     * considered fully encrypted.
     *
     * @param data the account data to store
     * @param dek the data encryption key used to encrypt the account data
     */
    constructor(data: AccountData, dek: ByteArray) : this() {
        this.setData(data, dek)
    }

    /**
     * Creates an account from its encrypted representation.
     *
     * This constructor is intended for the latest account format, where the KDF salt is mandatory and the software,
     * username, and password fields are encrypted.
     *
     * All provided cryptographic values are validated before being copied into the account through an [AccountMemento].
     *
     * @param kdfSalt the salt used to derive the per-field keys; must contain exactly [SALT_LENGTH] bytes
     * @param software the encrypted software data
     * @param sIv the initialization vector used for the software data; must contain [IV_LENGTH] bytes
     * @param username the encrypted username data
     * @param uIv the initialization vector used for the username data; must contain [IV_LENGTH] bytes
     * @param password the encrypted password data
     * @param pIv the initialization vector used for the password data; must contain [IV_LENGTH] bytes
     * @throws IllegalArgumentException if any provided value does not satisfy the required size or is empty
     */
    private constructor(
        kdfSalt: ByteArray,
        software: ByteArray, sIv: ByteArray,
        username: ByteArray, uIv: ByteArray,
        password: ByteArray, pIv: ByteArray
    ) : this() {
        require(kdfSalt.size == SALT_LENGTH) { "Salt should be exactly $SALT_LENGTH bytes long" }
        require(software.isNotEmpty()) { "Software cannot be empty" }
        require(sIv.size == IV_LENGTH) { "Software IV should be exactly $IV_LENGTH bytes long" }
        require(username.isNotEmpty()) { "Username cannot be empty" }
        require(uIv.size == IV_LENGTH) { "Username IV should be exactly $IV_LENGTH bytes long" }
        require(password.isNotEmpty()) { "Password cannot be empty" }
        require(pIv.size == IV_LENGTH) { "Password IV should be exactly $IV_LENGTH bytes long" }

        // Wrap the provided values into a memento and copy them to this account, centralizing the logic in copyMemento to avoid code duplication.
        // If this constructor is used, it means that software and username are encrypted, so the flag is set to true.
        val memento = AccountMemento(kdfSalt, software, sIv, username, uIv, password, pIv, true)
        copyMemento(memento)
    }

    /**
     * Creates an account from the legacy account format for backward compatibility.
     *
     * Older versions did not always persist the KDF salt. When it is missing or has an invalid size, it is derived
     * from the software and username to preserve compatibility with the legacy format. The derived salt is stored back
     * into the account so that it will be persisted on the next save, allowing the account to behave like a modern
     * account afterward.
     *
     * @param software the software associated with the account
     * @param username the account username
     * @param encryptedPassword the encrypted password
     * @param kdfSalt the persisted KDF salt, or null when using the legacy salt derivation
     * @param iv the initialization vector associated with the encrypted password; must contain [IV_LENGTH] bytes
     * @throws IllegalArgumentException if software or username is empty, the encrypted password is empty,
     *                                  or the IV does not have the required size
     */
    @Deprecated("") // This constructor is used only for backward compatibility
    private constructor(
        software: String,
        username: String,
        encryptedPassword: ByteArray,
        kdfSalt: ByteArray?,
        iv: ByteArray
    ) : this() {
        require(software.isNotEmpty()) { "Software cannot be empty" }
        require(username.isNotEmpty()) { "Username cannot be empty" }
        require(encryptedPassword.isNotEmpty()) { "Encrypted password cannot be empty" }
        require(iv.size == IV_LENGTH) { "IV should be exactly $IV_LENGTH bytes long" }

        this.encSoftware = software.toByteArray(StandardCharsets.UTF_8)
        this.encUsername = username.toByteArray(StandardCharsets.UTF_8)

        runOnFx {
            this.softwareWrapper.set(software)
            this.usernameWrapper.set(username)
        }

        // If the salt is not provided, derive it from software and username (backward compatibility:
        // older versions did not store it and used software+username bytes as the salt instead).
        // IMPORTANT: the derived value is written back into this.kdfSalt so it is persisted on the
        // next save, at which point the account behaves identically to a modern one.
        this.kdfSalt =
            if (kdfSalt?.size == SALT_LENGTH) kdfSalt.clone()
            else (software + username).toByteArray(StandardCharsets.UTF_8)
        iv.copyInto(this.pIv)

        this.encPassword = encryptedPassword
    }

    /**
     * Unlocks this account by decrypting its data with the provided DEK.
     * If the account was created with an older version where software and username were not encrypted, this method also
     * upgrades the account to the latest standard by encrypting all fields with the current one.
     *
     * @param dek the data encryption key to use for decryption
     * @param legacyVersion the security version to use for deriving the legacy key can be null if this account is
     *                      guaranteed to be created with the latest version
     * @param legacyMasterPassword the legacy master password to use for decryption if this account is not fully
     *                             encrypted, can be null if all accounts are guaranteed to be created with the
     *                             latest version
     * @throws GeneralSecurityException if decryption fails (e.g., due to wrong master password or corrupted data)
     */
    @Throws(GeneralSecurityException::class)
    fun unlock(dek: ByteArray, legacyVersion: SecurityVersion?, legacyMasterPassword: String?) {
        val data: AccountData =
            // Fast path: a fully encrypted account only needs to be read, so this path never holds the write lock.
            readLock.withLock {
                if (this.isFullyEncrypted) getData(dek)
                else null
            }
            // Slow path: the account was created with an older version where software and username were not encrypted, so
            // it has to be upgraded to the latest standard under the write lock.
            ?: writeLock.withLock {
                // Re-check under the write lock: another thread may have upgraded this account in the meantime.
                if (this.isFullyEncrypted) getData(dek)
                else {
                    checkNotNull(legacyVersion) { "Legacy version cannot be null when unlocking an account created with an older version" }
                    checkNotNull(legacyMasterPassword) { "Legacy master password cannot be null when unlocking an account created with an older version" }

                    // Use old master password as DEK to read existing data
                    val legacyKey = legacyVersion.getKey(legacyMasterPassword, kdfSalt)
                    getData(legacyKey).also { setData(it, dek) }
                }
            }

        // Schedule properties update for UI
        runOnFx {
            softwareWrapper.set(data.software)
            usernameWrapper.set(data.username)
        }
    }

    /**
     * The software name property for this account, read-only.
     * This is a UI-facing property, and therefore its updates should be considered low-priority.
     *
     * The getter is named after the JavaFX convention so that Java callers still see `softwareProperty()`.
     */
    val softwareProperty: ReadOnlyStringProperty
        @JvmName("softwareProperty")
        get() = softwareWrapper.readOnlyProperty

    /**
     * The software name associated with this account, empty until this account is unlocked.
     * This is a UI-facing property, and therefore its updates should be considered low-priority.
     */
    val software: String
        @JsonIgnore
        get() = softwareWrapper.get()

    /**
     * The username property for this account, read-only.
     * This is a UI-facing property, and therefore its updates should be considered low-priority.
     *
     * The getter is named after the JavaFX convention so that Java callers still see `usernameProperty()`.
     */
    val usernameProperty: ReadOnlyStringProperty
        @JvmName("usernameProperty")
        get() = usernameWrapper.readOnlyProperty

    /**
     * The username associated with this account, empty until this account is unlocked.
     * This is a UI-facing property, and therefore its updates should be considered low-priority.
     */
    val username: String
        @JsonIgnore
        get() = usernameWrapper.get()

    @JsonIgnore
    @Throws(GeneralSecurityException::class)
    fun getData(dek: ByteArray): AccountData = readLock.withLock {
        var plainSoftware: String
        var plainUsername: String
        var plainPassword: String

        if (this.isFullyEncrypted) {
            // Decrypt all fields
            val sKey: ByteArray = AES.deriveKey(dek, kdfSalt, "software")
            plainSoftware = AES.decryptStringAES(encSoftware, sKey, sIv)

            val uKey: ByteArray = AES.deriveKey(dek, kdfSalt, "username")
            plainUsername = AES.decryptStringAES(encUsername, uKey, uIv)

            val pKey: ByteArray = AES.deriveKey(dek, kdfSalt, "password")
            plainPassword = AES.decryptStringAES(encPassword, pKey, pIv)
        } else {
            // Decrypt only password
            plainSoftware = String(encSoftware, StandardCharsets.UTF_8)
            plainUsername = String(encUsername, StandardCharsets.UTF_8)
            // Use DEK as it was with master password derived key
            plainPassword = AES.decryptStringAES(encPassword, dek, pIv)
        }

        AccountData(plainSoftware, plainUsername, plainPassword)
    }

    /**
     * Wrapper record used for moving data in-and-out of this account.
     * This holds sensitive data and should not be passed around.
     */
    @JvmRecord
    data class AccountData(
        @JvmField val software: String,
        @JvmField val username: String,
        @JvmField val password: String
    )

    // #region Package-private methods (exposed to AccountRepository)
    @Throws(GeneralSecurityException::class)
    fun setData(data: AccountData, dek: ByteArray) {
        // Generate a unique KDF salt and per-field IVs for this rewrite.
        val newKdfSalt = ByteArray(SALT_LENGTH).also { AES.strongRandom.nextBytes(it) }
        val newSIv = ByteArray(IV_LENGTH)
        val newUIv = ByteArray(IV_LENGTH)
        val newPIv = ByteArray(IV_LENGTH)

        // Derive key and encrypt
        val newSoftware = encryptData(data.software, dek, newKdfSalt, newSIv, "software")
        val newUsername = encryptData(data.username, dek, newKdfSalt, newUIv, "username")
        val newPassword = encryptData(data.password, dek, newKdfSalt, newPIv, "password")

        // Commit under the write lock: every statement below is an assignment or an array copy, so this block cannot
        // fail halfway and no reader can observe a half-rewritten account.
        writeLock.withLock {
            kdfSalt = newKdfSalt

            encSoftware = newSoftware
            newSIv.copyInto(sIv)
            encUsername = newUsername
            newUIv.copyInto(uIv)
            encPassword = newPassword
            newPIv.copyInto(pIv)

            isFullyEncrypted = true
        }

        // Update properties for UI. This is only a dispatch, so it does not need the lock.
        runOnFx {
            softwareWrapper.set(data.software)
            usernameWrapper.set(data.username)
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun encryptData(
        newVal: String,
        sourceKey: ByteArray,
        kdfSalt: ByteArray,
        iv: ByteArray,
        info: String
    ): ByteArray {
        // Generate IV
        AES.strongRandom.nextBytes(iv)

        // Derive key and encrypt
        val key: ByteArray = AES.deriveKey(sourceKey, kdfSalt, info)
        return AES.encryptStringAES(newVal, key, iv)
    }

    /**
     * Memento record that captures the state of an Account for rollback purposes.
     * This implements the Memento pattern for transactional support.
     * This holds encrypted data for the account and can therefore be safely passed around.
     */
    @JvmRecord
    data class AccountMemento(
        val kdfSalt: ByteArray,
        val encSoftware: ByteArray, val sIv: ByteArray,
        val encUsername: ByteArray, val uIv: ByteArray,
        val encPassword: ByteArray, val pIv: ByteArray,
        val isFullyEncrypted: Boolean
    )

    /**
     * Captures the current state of this account for rollback purposes.
     * @return a memento object containing the account's current state
     */
    fun captureState(): AccountMemento = readLock.withLock {
        AccountMemento(
            this.kdfSalt.clone(),
            this.encSoftware.clone(),
            this.sIv.clone(),
            this.encUsername.clone(),
            this.uIv.clone(),
            this.encPassword.clone(),
            this.pIv.clone(),
            this.isFullyEncrypted
        )
    }

    /**
     * Restores the account's state from a memento.
     * @param memento the memento containing the state to restore
     */
    fun restoreState(memento: AccountMemento) = writeLock.withLock {
        copyMemento(memento)
    }

    private fun copyMemento(memento: AccountMemento) {
        runOnFx {
            // Invalidate properties to prevent reading inconsistent data during the copy
            this.softwareWrapper.set("unavailable while locked")
            this.usernameWrapper.set("unavailable while locked")
        }

        this.kdfSalt = memento.kdfSalt.clone()
        this.encSoftware = memento.encSoftware.clone()
        memento.sIv.copyInto(this.sIv)
        this.encUsername = memento.encUsername.clone()
        memento.uIv.copyInto(this.uIv)
        this.encPassword = memento.encPassword.clone()
        memento.pIv.copyInto(this.pIv)
        this.isFullyEncrypted = memento.isFullyEncrypted
    }

    // #endregion
    /**
     * Custom Jackson deserializer that distinguishes between the legacy (v1) format,
     * where `software` and `username` were plain strings and only the password
     * was encrypted, and the modern format where all three fields are encrypted byte arrays.
     * The discriminator is the presence of the `encryptedPassword` property.
     */
    class Deserializer : ValueDeserializer<Account>() {
        @Throws(JacksonException::class)
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Account {
            val node = p.readValueAsTree<ObjectNode>()

            // If encryptedPassword is present, it's the legacy format; otherwise, it's the modern format
            if (node.has("encryptedPassword")) {
                // Legacy format: software/username are plain strings, only password is encrypted
                val software = node.get("software").asString()
                val username = node.get("username").asString()
                val encryptedPassword = node.get("encryptedPassword").binaryValue()
                val kdfSalt = node.readKdfSalt()
                val iv = node.get("iv").binaryValue()
                return Account(software, username, encryptedPassword, kdfSalt, iv)
            } else {
                // Modern format: all fields are encrypted byte arrays
                val kdfSalt = node.readKdfSalt() ?: throw IllegalArgumentException("Account data is missing its KDF salt")
                val software = node.get("software").binaryValue()
                val softIv = node.get("softIv").binaryValue()
                val username = node.get("username").binaryValue()
                val userIv = node.get("userIv").binaryValue()
                val password = node.get("password").binaryValue()
                val passIv = node.get("passIv").binaryValue()
                return Account(kdfSalt, software, softIv, username, userIv, password, passIv)
            }
        }

        /**
         * Reads the KDF salt, accepting both the current `kdfSalt` property and the `salt` name used before it was
         * renamed, so that files written by any earlier version keep loading. Both formats are covered: the legacy one
         * persisted the salt only sometimes, hence the nullable result.
         */
        private fun ObjectNode.readKdfSalt(): ByteArray? = (get("kdfSalt") ?: get("salt"))?.binaryValue()
    }

    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16

        @JvmStatic
        @Throws(GeneralSecurityException::class)
        fun of(data: AccountData, dek: ByteArray) = Account(data, dek)
    }
}
