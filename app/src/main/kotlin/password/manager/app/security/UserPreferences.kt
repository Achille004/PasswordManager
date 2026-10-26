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
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import org.jetbrains.annotations.Contract
import password.manager.app.base.*
import password.manager.app.singletons.Logger
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.node.ObjectNode
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

@JsonDeserialize(using = UserPreferences.Deserializer::class)
class UserPreferences {

    // Runtime properties
    @Transient
    private val localeProperty: ObjectProperty<SupportedLocale> = SimpleObjectProperty(SupportedLocale.DEFAULT)

    @Transient
    private val sortingOrderProperty: ObjectProperty<SortingOrder> = SimpleObjectProperty(SortingOrder.SOFTWARE)

    @Transient
    private val securityVersionProperty: ObjectProperty<SecurityVersion> = SimpleObjectProperty(SecurityVersion.LATEST)

    // Serialized fields
    @JsonProperty("pwEncDek")
    private val pwEncDek = ByteArray(ENC_DEK_LENGTH)

    @JsonProperty("pwSalt")
    private val pwSalt = ByteArray(SALT_LENGTH)

    @JsonProperty("pwIv")
    private val pwIv = ByteArray(IV_LENGTH)

    // In-memory only — never serialized
    @Transient
    private var dekLocal: ByteArray? = null

    @Transient
    private var legacyHashedPassword: ByteArray? = null

    @Transient
    private var legacySalt: ByteArray? = null

    /**
     * Return the legacy security version used for hashing the password in the old format.
     * This method is visible only to classes in the same package.
     *
     * @return the legacy security version, or `null` if this instance was not loaded from a legacy file.
     */
    @Transient
    var legacyVersion: SecurityVersion? = null
        private set
        @Synchronized @JsonIgnore
        get

    private var isPasswordSet = false

    private constructor()

    constructor(password: String) {
        setPassword(password)
    }

    /**
     * New DEK-based format — called by [Deserializer].
     */
    private constructor(
        locale: SupportedLocale,
        sortingOrder: SortingOrder,
        securityVersion: SecurityVersion,
        pwEncDek: ByteArray,
        pwSalt: ByteArray,
        pwIv: ByteArray
    ) {
        require(pwEncDek.size == ENC_DEK_LENGTH) { "Encrypted DEK must be exactly $ENC_DEK_LENGTH bytes" }
        require(pwSalt.size == SALT_LENGTH) { "Salt must be exactly $SALT_LENGTH bytes" }
        require(pwIv.size == IV_LENGTH) { "IV must be exactly $IV_LENGTH bytes" }

        this.localeProperty.set(locale)
        this.sortingOrderProperty.set(sortingOrder)
        this.securityVersionProperty.set(securityVersion)

        System.arraycopy(pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH)
        System.arraycopy(pwSalt, 0, this.pwSalt, 0, SALT_LENGTH)
        System.arraycopy(pwIv, 0, this.pwIv, 0, IV_LENGTH)

        isPasswordSet = true
    }

    /**
     * Legacy hash-based format — called by [Deserializer] when `hashedPassword`
     * is present in JSON instead of `pwEncDek`.
     * The instance stays in legacy mode until [.verifyPassword] is called
     * with the correct password, at which point it is transparently upgraded.
     */
    private constructor(
        locale: SupportedLocale,
        sortingOrder: SortingOrder,
        securityVersion: SecurityVersion?,
        hashedPassword: ByteArray,
        salt: ByteArray
    ) {
        require(hashedPassword.isNotEmpty()) { "Hashed password cannot be null or empty" }
        require(salt.isNotEmpty()) { "Salt cannot be null or empty" }

        this.localeProperty.set(locale)
        this.sortingOrderProperty.set(sortingOrder)
        // securityVersion may be absent in very old files — fall back to PBKDF2
        this.securityVersionProperty.set(securityVersion ?: SecurityVersion.PBKDF2)

        this.legacyHashedPassword = hashedPassword
        this.legacySalt = salt
        this.legacyVersion = securityVersion

        isPasswordSet = true
    }

    @Synchronized
    fun set(other: UserPreferences) {
        this.locale = other.locale
        this.sortingOrder = other.sortingOrder
        this.securityVersion = other.securityVersion

        System.arraycopy(other.pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH)
        System.arraycopy(other.pwSalt, 0, this.pwSalt, 0, SALT_LENGTH)
        System.arraycopy(other.pwIv, 0, this.pwIv, 0, IV_LENGTH)

        this.dekLocal = other.dekLocal?.clone()
        this.legacyHashedPassword = other.legacyHashedPassword?.clone()
        this.legacySalt = other.legacySalt?.clone()
        this.legacyVersion = other.legacyVersion

        this.isPasswordSet = other.isPasswordSet
    }

    // #region Properties
    fun localeProperty(): ObjectProperty<SupportedLocale> = localeProperty
    var locale: SupportedLocale
        @JsonProperty("locale")
        get() = localeProperty.get()
        set(locale) = localeProperty.set(locale)

    fun sortingOrderProperty(): ObjectProperty<SortingOrder> = sortingOrderProperty
    var sortingOrder: SortingOrder
        @JsonProperty("sortingOrder")
        get() = sortingOrderProperty.get()
        set(sortingOrder) = sortingOrderProperty.set(sortingOrder)

    fun securityVersionProperty(): ObjectProperty<SecurityVersion> = securityVersionProperty
    var securityVersion: SecurityVersion
        @JsonProperty("securityVersion")
        get() = securityVersionProperty.get()
        set(securityVersion) = securityVersionProperty.set(securityVersion)
    // #endregion

    // #region Package-private getters for crypto fields
    internal val dek: ByteArray
        /**
         * Returns the DEK. This method is visible only to classes in the same module.
         * For security reasons (regarding having multiple copies of sensitive data in-memory), the returned array is
         * the same instance used internally, but it should be treated as read-only by callers outside this class.
         *
         * @return the not-null DEK
         * @throws IllegalStateException if the DEK is not available (i.e. the password has not been verified yet).
         */
        @JvmName("getDEK") @JvmSynthetic @Synchronized @JsonIgnore
        get() {
            checkNotNull(dekLocal) { "DEK is not available until the password has been verified" }
            return dekLocal!!
        }
    // #endregion

    // #region Password management
    /**
     * Verifies the given password against the stored credential.
     *
     * For instances loaded from the legacy hash-based format, the password is checked
     * against the stored hash; on success the instance is transparently upgraded to the
     * DEK-based format with a freshly generated salt (keeping the same master password).
     *
     * For instances in the current DEK-based format, AES-GCM authentication serves as
     * the implicit password check: if decryption succeeds the DEK is retained in memory
     * and can be retrieved via [.getDEK].
     *
     * @return `true` if the password is not null and correct, `false` otherwise.
     * @throws IllegalStateException if no password has been set.
     * @throws RuntimeException if an unexpected error occurs during hashing or decryption.
     */
    @Synchronized
    fun verifyPassword(passwordToVerify: String?): Boolean {
        check(isPasswordSet) { "No password is set" }
        if (passwordToVerify == null) return false

        // Since the legacy hashed password is not nullified after security version upgrade,
        // we can detect legacy mode by also checking that the DEK is still null.
        val legacyFieldsEmpty = (legacyHashedPassword == null) || (legacySalt == null)
        if (dekLocal == null && !legacyFieldsEmpty) {
            // Legacy mode: verify by comparing hashes
            val hashedInput = this.securityVersion.hash(passwordToVerify, legacySalt!!)
            val match = MessageDigest.isEqual(legacyHashedPassword, hashedInput)

            if (match) {
                // Upgrade to DEK format; setPassword generates a new random salt
                this.securityVersion = SecurityVersion.LATEST
                setPassword(passwordToVerify)
                // Empty the legacy fields
                this.legacyHashedPassword = null
                this.legacySalt = null
            }

            return match
        }

        // DEK-based mode: AEAD bad tag exception indicates wrong password
        try {
            this.dekLocal = decryptDEK(this.securityVersion, passwordToVerify, pwEncDek, pwSalt, pwIv)
            return true
        } catch (_: AEADBadTagException) {
            return false
        } catch (e: GeneralSecurityException) {
            Logger.addError(e)
            throw RuntimeException("Failed to decrypt DEK", e)
        }
    }

    @Synchronized
    fun setPasswordVerified(oldPassword: String?, newPassword: String): Boolean {
        // If no password is set yet, we allow setting a new one without verification (first-time setup).
        val canSet = !isPasswordSet || verifyPassword(oldPassword)
        if (canSet) setPassword(newPassword)
        return canSet
    }

    /**
     * Sets (or changes) the master password.
     *
     *  * If no DEK exists yet (first-time setup), a new random DEK is generated.
     *  * Otherwise the existing DEK is re-encrypted under the new password,
     * so already-encrypted account data remains intact.
     *
     * A fresh random salt and IV are always generated so the derived key changes
     * even when the same password is reused (important for the legacy → DEK upgrade path).
     */
    @Synchronized
    private fun setPassword(password: String) {
        val random: SecureRandom = AES.strongRandom

        random.nextBytes(this.pwSalt)
        random.nextBytes(this.pwIv)

        // Reuse existing DEK when changing the password; generate a new one on first setup.
        if (this.dekLocal == null) {
            this.dekLocal = ByteArray(DEK_LENGTH)
            random.nextBytes(this.dekLocal)
        }

        this.securityVersion = SecurityVersion.LATEST

        try {
            val pwEncDek: ByteArray =
                encryptDEK(SecurityVersion.LATEST, password, this.dekLocal!!, this.pwSalt, this.pwIv)
            System.arraycopy(pwEncDek, 0, this.pwEncDek, 0, ENC_DEK_LENGTH)
            this.isPasswordSet = true
        } catch (e: GeneralSecurityException) {
            Logger.addError(e)
            throw RuntimeException("Failed to encrypt DEK", e)
        }
    }

    /**
     * Custom Jackson deserializer that handles both the legacy hash-based format
     * (JSON field `hashedPassword`) and the current DEK-based format
     * (JSON field `pwEncDek`), allowing existing data files to be loaded
     * and transparently upgraded on first authenticated access.
     */
    class Deserializer : ValueDeserializer<UserPreferences?>() {
        @Throws(JacksonException::class)
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): UserPreferences {
            val node = p.readValueAsTree<ObjectNode>()

            val locale = if (node.has("locale"))
                SupportedLocale.forLanguageTag(node.get("locale").asString())
            else
                SupportedLocale.DEFAULT

            val sortingOrder = if (node.has("sortingOrder"))
                SortingOrder.valueOf(node.get("sortingOrder").asString())
            else
                SortingOrder.SOFTWARE

            val securityVersion = if (node.has("securityVersion"))
                SecurityVersion.fromString(node.get("securityVersion").asString())
            else
                null

            if (node.has("hashedPassword")) {
                val hashedPassword = node.get("hashedPassword").binaryValue()
                val salt = node.get("salt").binaryValue()
                return UserPreferences(locale, sortingOrder, securityVersion, hashedPassword, salt)
            } else {
                requireNotNull(securityVersion) { "Missing securityVersion for DEK-based format" }
                val pwEncDek = node.get("pwEncDek").binaryValue()
                val pwSalt = node.get("pwSalt").binaryValue()
                val pwIv = node.get("pwIv").binaryValue()
                return UserPreferences(locale, sortingOrder, securityVersion, pwEncDek, pwSalt, pwIv)
            }
        }
    }
    // #endregion

    companion object {
        private const val DEK_LENGTH = AES.AES_BITS / 8 // 32 bytes
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16

        private const val ENC_DEK_LENGTH: Int = DEK_LENGTH + (AES.GCM_TAG_BITS / 8) // 48 bytes

        /**
         * Encrypt `dek` bytes using AES-GCM with a KEK derived from `password` + `salt`.
         */
        @Throws(GeneralSecurityException::class)
        private fun encryptDEK(
            version: SecurityVersion,
            password: String,
            dek: ByteArray,
            salt: ByteArray,
            iv: ByteArray
        ): ByteArray {
            val kekBytes = version.getKey(password, salt)
            return AES.encryptAES(dek, kekBytes, iv)
        }

        /**
         * Decrypt the wrapped DEK. AES-GCM tag verification provides implicit password
         * authentication — a wrong password produces a [GeneralSecurityException].
         */
        @Throws(GeneralSecurityException::class)
        private fun decryptDEK(
            version: SecurityVersion,
            password: String,
            pwEncDek: ByteArray,
            salt: ByteArray,
            pwIv: ByteArray
        ): ByteArray {
            val kekBytes = version.getKey(password, salt)
            return AES.decryptAES(pwEncDek, kekBytes, pwIv)
        }

        @JvmStatic
        @Contract("_ -> new")
        fun of(password: String?): UserPreferences =
            if (password != null) UserPreferences(password)
            else UserPreferences()

        @JvmStatic
        @Contract("-> new")
        fun empty() = UserPreferences()
    }
}
