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

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import password.manager.app.base.SecurityVersion;
import password.manager.app.security.Account;
import password.manager.app.security.Account.AccountData;
import password.manager.app.security.AccountRepository;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.Singletons;

@SuppressWarnings("deprecation")
public class TestAccountRepository {

    private static final String DEFAULT_MASTER_PASSWORD = "MasterPassword123!";

    private AccountRepository repository;
    private ObjectProperty<SecurityVersion> securityVersionProperty;
    private StringProperty masterPasswordProperty;

    @BeforeEach
    void setUp() {
        securityVersionProperty = new SimpleObjectProperty<>(SecurityVersion.LATEST);
        masterPasswordProperty = new SimpleStringProperty(DEFAULT_MASTER_PASSWORD);
        repository = new AccountRepository(securityVersionProperty, masterPasswordProperty);
    }

    @AfterEach
    void tearDown() {
        repository.close();
        Singletons.shutdownAll();
    }

    @Test
    void testInitiallyEmpty() {
        TestingUtils.injectBasePath();

        assertTrue(repository.findAll().isEmpty(), "Repository should be empty on initialization");
    }

    @Test
    void testAdd() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("TestSoftware", "TestUser", "TestPass123!");
        CompletableFuture<Account> future = repository.add(data);
        Account account = future.get(5, TimeUnit.SECONDS);

        assertNotNull(account, "Account should be created successfully");
        assertEquals(1, repository.findAll().size(), "Repository should contain one account");
        assertEquals("TestSoftware", account.getSoftware());
        assertEquals("TestUser", account.getUsername());
    }

    @Test
    void testAddMultiple() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        int count = 10;
        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData data = new AccountData(software, username, password);
            repository
                    .add(data)
                    .get(5, TimeUnit.SECONDS);
        }

        assertEquals(count, repository.findAll().size(), "Repository should contain all added accounts");
    }

    @Test
    void testEdit() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AccountData oldData = new AccountData("OldSoftware", "OldUser", "OldPass");
        Account account = repository
                .add(oldData)
                .get(5, TimeUnit.SECONDS);

        AccountData newData = new AccountData("NewSoftware", "NewUser", "NewPass");
        Account edited = repository
                .edit(account, newData)
                .get(5, TimeUnit.SECONDS);

        assertNotNull(edited, "Edit should return updated account");
        assertEquals("NewSoftware", edited.getSoftware());
        assertEquals("NewUser", edited.getUsername());
        assertEquals(1, repository.findAll().size(), "Repository should still contain one account");
    }

    @Test
    void testEditNonExistentAccount() throws GeneralSecurityException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("NonExistentSoftware", "NonExistentUser", "NonExistentPass");
        Account account = Account.of(SecurityVersion.LATEST, data, DEFAULT_MASTER_PASSWORD);

        AccountData newData = new AccountData("NewSoftware", "NewUser", "NewPass");
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.edit(account, newData),
                "Editing non-existent account should throw IllegalArgumentException"
        );
    }

    @Test
    void testRemove() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("TestSoftware", "TestUser", "TestPass");
        Account account = repository
                .add(data)
                .get(5, TimeUnit.SECONDS);

        boolean removed = repository.remove(account).get(5, TimeUnit.SECONDS);

        assertTrue(removed, "Remove should return true");
        assertTrue(repository.findAll().isEmpty(), "Repository should be empty after removal");
    }

    @Test
    void testRemoveNonExistentAccount() throws GeneralSecurityException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("NonExistentSoftware", "NonExistentUser", "NonExistentPass");
        Account account = Account.of(SecurityVersion.LATEST, data, DEFAULT_MASTER_PASSWORD);

        assertThrows(
            IllegalArgumentException.class,
            () -> repository.remove(account),
            "Removing non-existent account should throw IllegalArgumentException"
        );
    }

    @Test
    void testGetData() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        String expectedPassword = "SecurePassword123!";
        AccountData data = new AccountData("TestSoftware", "TestUser", expectedPassword);

        Account account = repository
                .add(data)
                .get(5, TimeUnit.SECONDS);

        AccountData decryptedData = repository
                .getData(account)
                .get(5, TimeUnit.SECONDS);

        assertEquals(data.password(), decryptedData.password(), "Decrypted password should match original");
    }

    @Test
    void testSetAll() throws GeneralSecurityException {
        TestingUtils.injectBasePath();

        List<Account> testAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData testData = new AccountData(software, username, password);
            Account account = Account.of(SecurityVersion.LATEST, testData, DEFAULT_MASTER_PASSWORD);

            testAccounts.add(account);
        }

        repository.setAll(testAccounts);

        assertEquals(testAccounts.size(), repository.findAll().size(), "Repository should contain all set accounts");
    }

    @Test
    void testNonEmptySetAll() throws ExecutionException, InterruptedException, TimeoutException, GeneralSecurityException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("InitialSoftware", "InitialUser", "InitialPass");
        repository
                .add(data)
                .get(5, TimeUnit.SECONDS);

        List<Account> testAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String software = "Soft" + i;
            String username = "User" + i;
            String password = "Pass" + i;

            AccountData testData = new AccountData(software, username, password);
            Account account = Account.of(SecurityVersion.LATEST, testData, DEFAULT_MASTER_PASSWORD);

            testAccounts.add(account);
        }

        assertThrows(
            IllegalStateException.class,
            () -> repository.setAll(testAccounts),
            "setAll on non-empty repository should throw IllegalStateException"
        );
    }

    @Test
    void testFindAllIsUnmodifiable() throws ExecutionException, InterruptedException, TimeoutException, GeneralSecurityException {
        TestingUtils.injectBasePath();

        AccountData data = new AccountData("TestSoftware", "TestUser", "TestPass");
        repository
                .add(data)
                .get(5, TimeUnit.SECONDS);

        AccountData anotherData = new AccountData("AnotherSoftware", "AnotherUser", "AnotherPass");
        Account account = Account.of(SecurityVersion.LATEST, anotherData, DEFAULT_MASTER_PASSWORD);

        assertThrows(
            UnsupportedOperationException.class,
            () -> repository.findAll().add(account),
            "findAll should return unmodifiable list"
        );
    }

    @Test
    void testConcurrentOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        int operationCount = 10;
        CompletableFuture<?>[] futures = new CompletableFuture[operationCount];

        for (int i = 0; i < operationCount; i++) {
            AccountData data = new AccountData("Soft" + i, "User" + i, "Pass" + i);
            futures[i] = repository.add(data);
        }

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        assertEquals(operationCount, repository.findAll().size(),
            "All concurrent add operations should complete successfully");
    }

    @Test
    void testManyConcurrentOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.injectBasePath();

        Runtime runtime = Runtime.getRuntime();
        int operationCount = runtime.availableProcessors() * 10; // 10 operations per CPU core

        CompletableFuture<?>[] futures = new CompletableFuture[operationCount];

        for (int i = 0; i < operationCount; i++) {
            AccountData data = new AccountData("Soft" + i, "User" + i, "Pass" + i);
            futures[i] = repository.add(data);
        }

        CompletableFuture.allOf(futures).get(operationCount, TimeUnit.SECONDS); // 1s per operation should be more than enough

        assertEquals(operationCount, repository.findAll().size(),
            "All concurrent add operations should complete successfully");
    }

    @Test
    void testChangeMasterPassword() throws Exception {
        TestingUtils.injectBasePath();

        // Add accounts
        int count = 10;
        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData data = new AccountData(software, username, password);
            repository
                    .add(data)
                    .get(5, TimeUnit.SECONDS);
        }

        // Change master password
        masterPasswordProperty.set("NewMasterPassword456!");

        // Try to get password with new master password
        for (Account account : repository.findAll()) {
            Logger.getInstance().addDebug("Testing account: " + account.getSoftware() + " / " +  account.getUsername());
            AccountData data = repository.getData(account).get(30, TimeUnit.SECONDS);
            assertNotNull(data.software(), "Software should be retrievable with new master password");
            assertNotNull(data.username(), "Username should be retrievable with new master password");
            assertNotNull(data.password(), "Password should be retrievable with new master password");
        }
    }

    @Test
    void testNullMasterPassword() throws Exception {
        TestingUtils.injectBasePath();

        // Set initial master password to null
        masterPasswordProperty.set(null);

        // Add accounts
        int count = 10;
        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            // Add with null master password should:
            AccountData data = new AccountData(software, username, password);
            CompletableFuture<Account> future = repository.add(data);

            // - Return null
            assertNull(
                future.get(5, TimeUnit.SECONDS),
                "Adding account with null master password should return null"
            );

            // - Not add account to repository
            assertTrue(repository.findAll().isEmpty(), "Repository should remain empty when adding with null master password");
        }
    }

    @Test
    void testChangeMasterPasswordWithEmptyRepository() {
        TestingUtils.injectBasePath();

        masterPasswordProperty.set("NewMasterPassword456!");

        assertEquals(0, repository.findAll().size(), "Repository should remain empty");
    }


    @Test
    void testUpdateToLatestSecurityVersion() throws Exception {
        TestingUtils.injectBasePath();

        // Set initial security version to PBKDF2, future execute on the empty repository (no changes affecting accounts)
        securityVersionProperty.set(SecurityVersion.PBKDF2);

        // Add accounts to repository
        int count = 10;
        List<AccountData> expectedData = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData data = new AccountData(software, username, password);
            expectedData.add(data);

            repository
                .add(data)
                .get(5, TimeUnit.SECONDS);
        }

        Logger.getInstance().addDebug("Accounts added with security version: " + securityVersionProperty.get().name());
        List<Account> beforeUpdate = new ArrayList<>(repository.findAll());

        // Update all accounts to latest security version
        securityVersionProperty.set(SecurityVersion.LATEST);
        Logger.getInstance().addDebug("Security version updated to: " + securityVersionProperty.get().name());

        // Verify all passwords are still accessible and correct
        List<Account> afterUpdate = repository.findAll();
        assertEquals(beforeUpdate.size(), afterUpdate.size(), "Account count should remain the same");

        for (int i = 0; i < afterUpdate.size(); i++) {
            Account account = afterUpdate.get(i);
            AccountData data = repository
                .getData(account)
                .get(30, TimeUnit.SECONDS);

            assertEquals(expectedData.get(i).password(), data.password(),
                "Password should remain the same after security version update");
        }
    }

    @Test
    void testNullSecurityVersion() throws Exception {
        TestingUtils.injectBasePath();

        // Set initial security version to null
        securityVersionProperty.set(null);

        // Add accounts
        int count = 10;
        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            // Add with null security version should:
            AccountData data = new AccountData(software, username, password);
            CompletableFuture<Account> future = repository.add(data);

            // - Return null
            assertNull(
                future.get(5, TimeUnit.SECONDS),
                "Adding account with null master password should return null"
            );

            // - Not add account to repository
            assertTrue(repository.findAll().isEmpty(), "Repository should remain empty when adding with null master password");
        }
    }

    @Test
    void testUpdateToLatestSecurityVersionWithEmptyRepository() {
        TestingUtils.injectBasePath();

        securityVersionProperty.set(SecurityVersion.PBKDF2);
        securityVersionProperty.set(SecurityVersion.LATEST);

        assertEquals(0, repository.findAll().size(), "Repository should remain empty");
    }

    @Test
    void testUpdateToLatestSecurityVersionThenChangeMasterPassword() throws Exception {
        TestingUtils.injectBasePath();

        securityVersionProperty.set(SecurityVersion.PBKDF2);

        // Add accounts
        int count = 10;
        for (int i = 0; i < count; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData data = new AccountData(software, username, password);
            repository
                    .add(data)
                    .get(5, TimeUnit.SECONDS);
        }

        List<Account> accounts = repository.findAll();
        securityVersionProperty.set(SecurityVersion.LATEST);
        masterPasswordProperty.set("NewMasterPassword456!");

        // Try to get password with new master password
        for (Account account : accounts) {
            AccountData data = repository.getData(account).get(30, TimeUnit.SECONDS);
            assertNotNull(data.software(), "Software should be retrievable with new master password");
            assertNotNull(data.username(), "Username should be retrievable with new master password");
            assertNotNull(data.password(), "Password should be retrievable with new master password");
        }
    }
}
