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

package testing.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import password.manager.app.security.Account;
import password.manager.app.security.Account.AccountData;
import password.manager.app.security.AccountRepository;
import password.manager.app.security.UserPreferences;
import password.manager.app.singletons.Singletons;
import testing.TestingUtils;

public class TestAccountRepository {

    private static final String DEFAULT_MASTER_PASSWORD = "MasterPassword123!";
    private static final byte[] DEFAULT_DEK = DEFAULT_MASTER_PASSWORD.getBytes(StandardCharsets.UTF_8);

    private UserPreferences userPreferences;
    private AccountRepository repository;

    @BeforeEach
    void setUp() {
        userPreferences = UserPreferences.of(DEFAULT_MASTER_PASSWORD);
        repository = new AccountRepository(userPreferences);
    }

    @AfterEach
    void tearDown() {
        repository.close();
        Singletons.shutdownAll();
    }

    @Test
    void testInitiallyEmpty() {
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
        Account account = Account.of(data, DEFAULT_DEK);

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
        Account account = Account.of(data, DEFAULT_DEK);

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
        List<Account> testAccounts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String software = "software" + i;
            String username = "user" + i;
            String password = "password" + i;

            AccountData testData = new AccountData(software, username, password);
            Account account = Account.of(testData, DEFAULT_DEK);

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
            Account account = Account.of(testData, DEFAULT_DEK);

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
        AccountData data = new AccountData("TestSoftware", "TestUser", "TestPass");
        repository
                .add(data)
                .get(5, TimeUnit.SECONDS);

        AccountData anotherData = new AccountData("AnotherSoftware", "AnotherUser", "AnotherPass");
        Account account = Account.of(anotherData, DEFAULT_DEK);

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
    void testEmptyUserPreferences() throws GeneralSecurityException {
        // Just set the same data every time, we just want to test the user preferences check
        AccountData data = new AccountData("TestSoftware", "TestUser", "TestPass");

        Account account = Account.of(data, DEFAULT_DEK);
        repository.setAll(account);

        userPreferences.set(UserPreferences.empty());

        CompletableFuture<Account> addFuture = repository.add(data);
        CompletableFuture<Account> editFuture = repository.edit(account, data); // Just set the same data, we just want to test the user preferences check
        CompletableFuture<AccountData> getDataFuture = repository.getData(account);
        CompletableFuture<Boolean> unlockFuture = repository.unlockAll(DEFAULT_MASTER_PASSWORD);

        assertThrows(
            CompletionException.class,
            addFuture::join,
            "Adding account with empty user preferences should fail exceptionally"
        );

        assertThrows(
            CompletionException.class,
            editFuture::join,
            "Editing account with empty user preferences should fail exceptionally"
        );

        assertThrows(
            IllegalStateException.class,
            () -> repository.remove(account),
            "Removing account with empty user preferences should throw IllegalStateException"
        );

        assertThrows(
            CompletionException.class,
            getDataFuture::join,
            "Getting account data with empty user preferences should fail exceptionally");

        assertThrows(
            CompletionException.class,
            unlockFuture::join,
            "Unlocking accounts with empty user preferences should fail exceptionally"
        );
    }
}
