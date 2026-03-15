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

package testing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import password.manager.app.base.SecurityVersion;
import password.manager.app.base.SortingOrder;
import password.manager.app.base.SupportedLocale;
import password.manager.app.security.AES;
import password.manager.app.security.UserPreferences;
import tools.jackson.databind.ObjectMapper;

public class TestUserPreferences {

    // #region DEK tests

    @Test
    void testGeneratedDEKHasCipherKeyLength() {
        UserPreferences prefs = UserPreferences.of("testPassword123");

        assertTrue(prefs.verifyPassword("testPassword123"));

        byte[] dek = prefs.getDEK();
        assertNotNull(dek);
        assertEquals(AES.AES_BITS / 8, dek.length);
    }

    @Test
    void testDEKAvailableImmediatelyAfterCreation() {
        // The DEK is generated during setPassword() and should be accessible
        // even before an explicit verifyPassword() call.
        UserPreferences prefs = UserPreferences.of("myPassword");

        byte[] dek = prefs.getDEK();
        assertNotNull(dek);
        assertEquals(AES.AES_BITS / 8, dek.length);
    }

    @Test
    void testGetDEKReturnsDefensiveCopy() {
        UserPreferences prefs = UserPreferences.of("password");

        byte[] dek1 = prefs.getDEK();
        byte[] dek2 = prefs.getDEK();

        assertNotSame(dek1, dek2, "getDEK() must return a fresh copy each time");
        assertArrayEquals(dek1, dek2, "Both copies must contain the same bytes");
    }

    @Test
    void testDifferentPreferencesHaveUniqueDEKs() {
        // Two independently created instances must generate distinct random DEKs.
        UserPreferences p1 = UserPreferences.of("samePassword");
        UserPreferences p2 = UserPreferences.of("samePassword");

        assertFalse(Arrays.equals(p1.getDEK(), p2.getDEK()),
                "Two separate instances must have different DEKs");
    }

    @Test
    void testDEKPreservedAfterPasswordChange() {
        // Changing the master password must re-encrypt the DEK but keep its value.
        UserPreferences prefs = UserPreferences.of("oldPassword");
        byte[] originalDEK = prefs.getDEK();

        assertTrue(prefs.setPasswordVerified("oldPassword", "newPassword"));
        // Re-verify with the new password so getDEK() returns the decrypted DEK.
        assertTrue(prefs.verifyPassword("newPassword"));

        assertArrayEquals(originalDEK, prefs.getDEK(),
                "The DEK must remain unchanged after a password change");
    }

    @Test
    void testEmptyPreferencesDEKIsNull() {
        UserPreferences prefs = UserPreferences.empty();
        assertNull(prefs.getDEK());
    }

    // #endregion

    // #region Password verification tests

    @Test
    void testVerifyWrongPasswordReturnsFalse() {
        UserPreferences prefs = UserPreferences.of("correctPassword");
        assertFalse(prefs.verifyPassword("wrongPassword"));
    }

    @Test
    void testVerifyNullPasswordReturnsFalse() {
        UserPreferences prefs = UserPreferences.of("somePassword");
        assertFalse(prefs.verifyPassword(null));
    }

    @Test
    void testFailedVerifyDoesNotClearDEK() {
        UserPreferences prefs = UserPreferences.of("correctPassword");
        byte[] dekBefore = prefs.getDEK();

        prefs.verifyPassword("wrongPassword");

        assertArrayEquals(dekBefore, prefs.getDEK(),
                "A failed verifyPassword() must not alter the in-memory DEK");
    }

    @Test
    void testEmptyPreferencesVerifyAlwaysReturnsTrue() {
        UserPreferences prefs = UserPreferences.empty();
        assertTrue(prefs.verifyPassword(null));
        assertTrue(prefs.verifyPassword("anyPassword"));
        assertTrue(prefs.verifyPassword(""));
    }

    // #endregion

    // #region setPasswordVerified tests

    @Test
    void testSetPasswordVerifiedSuccessWithOldPassword() {
        UserPreferences prefs = UserPreferences.of("oldPassword");

        assertTrue(prefs.setPasswordVerified("oldPassword", "newPassword"),
                "setPasswordVerified must return true for the correct old password");
        assertTrue(prefs.verifyPassword("newPassword"),
                "New password must be accepted after the change");
        assertFalse(prefs.verifyPassword("oldPassword"),
                "Old password must be rejected after the change");
    }

    @Test
    void testSetPasswordVerifiedFailureWithWrongOldPassword() {
        UserPreferences prefs = UserPreferences.of("correctPassword");

        assertFalse(prefs.setPasswordVerified("wrongPassword", "newPassword"),
                "setPasswordVerified must return false for a wrong old password");
        assertTrue(prefs.verifyPassword("correctPassword"),
                "Original password must still work after a failed change");
        assertFalse(prefs.verifyPassword("newPassword"),
                "New password must not be set after a failed change");
    }

    // #endregion

    // #region Property tests

    @Test
    void testDefaultSecurityVersionIsLatest() {
        UserPreferences prefs = UserPreferences.of("password");
        assertEquals(SecurityVersion.LATEST, prefs.getSecurityVersion());
    }

    @Test
    void testDefaultSortingOrderIsSoftware() {
        UserPreferences prefs = UserPreferences.of("password");
        assertEquals(SortingOrder.SOFTWARE, prefs.getSortingOrder());
    }

    @Test
    void testSetSortingOrder() {
        UserPreferences prefs = UserPreferences.of("password");

        prefs.setSortingOrder(SortingOrder.USERNAME);

        assertEquals(SortingOrder.USERNAME, prefs.getSortingOrder());
        assertEquals(SortingOrder.USERNAME, prefs.sortingOrderProperty().get());
    }

    @Test
    void testSetLocale() {
        UserPreferences prefs = UserPreferences.of("password");

        prefs.setLocale(SupportedLocale.ITALIAN);

        assertEquals(SupportedLocale.ITALIAN, prefs.getLocale());
        assertEquals(SupportedLocale.ITALIAN, prefs.localeProperty().get());
    }

    // #endregion

    // #region set() method tests

    @Test
    void testSetCopiesAllFields() {
        UserPreferences source = UserPreferences.of("password");
        source.setSortingOrder(SortingOrder.USERNAME);
        source.setLocale(SupportedLocale.ITALIAN);
        source.setSecurityVersion(SecurityVersion.ARGON2);
        source.verifyPassword("password");

        UserPreferences target = UserPreferences.empty();
        target.set(source);

        assertEquals(SortingOrder.USERNAME, target.getSortingOrder());
        assertEquals(SupportedLocale.ITALIAN, target.getLocale());
        assertEquals(SecurityVersion.ARGON2, target.getSecurityVersion());
        assertArrayEquals(source.getDEK(), target.getDEK(),
                "DEK must be copied by set()");
        // target must accept the same password as source after copying
        assertTrue(target.verifyPassword("password"));
    }

    @Test
    void testSetFromEmptySource() {
        UserPreferences source = UserPreferences.empty();
        UserPreferences target = UserPreferences.of("password");

        target.set(source);

        // After copying an empty preferences, any password must be accepted
        assertTrue(target.verifyPassword(null));
        assertTrue(target.verifyPassword("anything"));
        assertNull(target.getDEK());
    }

    // #endregion

    // #region of() factory tests

    @Test
    void testOfNullCreatesEmptyPreferences() {
        UserPreferences prefs = UserPreferences.of(null);
        // Behaviorally equivalent to empty(): no password set
        assertTrue(prefs.verifyPassword(null));
        assertTrue(prefs.verifyPassword("anything"));
        assertNull(prefs.getDEK());
    }

    // #endregion

    // #region Legacy hash-based migration tests (non-DEK)

    @Test
    @SuppressWarnings("deprecation")
    void testLegacyPBKDF2ExplicitUpgradesToDEK() throws Exception {
        String password = "legacyPBKDF2Password";
        UserPreferences prefs = fromLegacyHashJson(password, SecurityVersion.PBKDF2, true);

        assertEquals(SecurityVersion.PBKDF2, prefs.getSecurityVersion());
        assertEquals(SecurityVersion.PBKDF2, prefs.getLegacyVersion());
        assertNull(prefs.getDEK(), "Legacy hash format should not have in-memory DEK before verification");

        assertTrue(prefs.verifyPassword(password));

        assertEquals(SecurityVersion.LATEST, prefs.getSecurityVersion(),
                "After successful legacy verification, security version must upgrade to LATEST");
        assertNotNull(prefs.getDEK(), "DEK must be generated during migration");
        assertEquals(AES.AES_BITS / 8, prefs.getDEK().length);
        assertFalse(prefs.verifyPassword("wrongPassword"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testLegacyPBKDF2WithoutVersionFallsBackToPBKDF2ThenUpgrades() throws Exception {
        String password = "legacyFallbackPassword";
        UserPreferences prefs = fromLegacyHashJson(password, SecurityVersion.PBKDF2, false);

        assertEquals(SecurityVersion.PBKDF2, prefs.getSecurityVersion(),
                "Missing securityVersion in legacy JSON should fallback to PBKDF2");
        assertNull(prefs.getLegacyVersion(),
                "Legacy version metadata should be null when field is absent in JSON");
        assertNull(prefs.getDEK());

        assertTrue(prefs.verifyPassword(password));
        assertEquals(SecurityVersion.LATEST, prefs.getSecurityVersion());
        assertNotNull(prefs.getDEK());
    }

    @Test
    void testLegacyARGON2NonDEKUpgradesToDEK() throws Exception {
        String password = "legacyArgon2Password";
        UserPreferences prefs = fromLegacyHashJson(password, SecurityVersion.ARGON2, true);

        assertEquals(SecurityVersion.ARGON2, prefs.getSecurityVersion());
        assertEquals(SecurityVersion.ARGON2, prefs.getLegacyVersion());
        assertNull(prefs.getDEK());

        assertTrue(prefs.verifyPassword(password));

        assertEquals(SecurityVersion.LATEST, prefs.getSecurityVersion());
        assertNotNull(prefs.getDEK());
        assertEquals(AES.AES_BITS / 8, prefs.getDEK().length);
    }

    @Test
    void testLegacyWrongPasswordDoesNotUpgrade() throws Exception {
        String password = "legacyArgon2Password";
        UserPreferences prefs = fromLegacyHashJson(password, SecurityVersion.ARGON2, true);

        assertFalse(prefs.verifyPassword("notTheRightPassword"));
        assertEquals(SecurityVersion.ARGON2, prefs.getSecurityVersion(),
                "On failed legacy verification, security version must stay unchanged");
        assertNull(prefs.getDEK(), "DEK must not be created when legacy password verification fails");
    }

    // #endregion

    private static UserPreferences fromLegacyHashJson(String password, SecurityVersion legacyVersion, boolean includeSecurityVersion) throws Exception {
        byte[] salt = deterministicSalt();
        byte[] hashedPassword = legacyVersion.hash(password, salt);

        StringBuilder json = new StringBuilder("{"
                + "\"locale\":\"en\","
                + "\"sortingOrder\":\"SOFTWARE\",");

        if (includeSecurityVersion) {
            json.append("\"securityVersion\":\"").append(legacyVersion.name()).append("\",");
        }

        json.append("\"hashedPassword\":\"")
                .append(Base64.getEncoder().encodeToString(hashedPassword))
                .append("\",")
                .append("\"salt\":\"")
                .append(Base64.getEncoder().encodeToString(salt))
                .append("\"}");

        return new ObjectMapper().readValue(json.toString(), UserPreferences.class);
    }

    private static byte[] deterministicSalt() {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) (i + 1);
        }
        return salt;
    }
}