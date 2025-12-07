package testing;

import static org.junit.jupiter.api.Assertions.*;
import static testing.TestingUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import password.manager.app.enums.SecurityVersion;
import password.manager.app.security.Account;
import password.manager.app.security.AccountRepository;
import password.manager.app.singletons.Logger;

public class TestAccountRepository {

    private static final List<Account> accounts = new ArrayList<>();
    private static final String masterPassword = "MasterPassword123!";

    private AccountRepository repository;

    @BeforeAll
    static void populate() throws GeneralSecurityException, IOException {
        try (InputStream stream = TestAccount.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String pass, software, username;

                while ((pass = readBlnsLine(reader)) != null &&
                       (software = readBlnsLine(reader)) != null &&
                       (username = readBlnsLine(reader)) != null) {

                    Account account = Account.of(SecurityVersion.LATEST, software, username, pass, masterPassword);
                    accounts.add(account);
                }
            }
        }

    }

    @BeforeEach
    void setUp() {
        Logger.createInstance(TestingUtils.LOG_PATH);
        repository = new AccountRepository();
    }

    @AfterEach
    void tearDown() {
        repository.close();
        Logger.destroyInstance();
    }

    @Test
    void testInitiallyEmpty() {
        assertTrue(repository.findAll().isEmpty(), "Repository should be empty on initialization");
    }

    @Test
    void testAdd() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Account> future = repository.add(
            SecurityVersion.LATEST,
            masterPassword,
            "GitHub",
            "testuser",
            "testpass123"
        );

        Account account = future.get(5, TimeUnit.SECONDS);
        assertNotNull(account, "Account should be created successfully");
        assertEquals(1, repository.findAll().size(), "Repository should contain one account");
        assertEquals("GitHub", account.getSoftware());
        assertEquals("testuser", account.getUsername());
    }

    @Test
    void testAddMultiple() throws ExecutionException, InterruptedException, TimeoutException {
        int count = Math.min(10, accounts.size());

        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository.add(
                SecurityVersion.LATEST,
                masterPassword,
                source.getSoftware(),
                source.getUsername(),
                "password" + i
            ).get(5, TimeUnit.SECONDS);
        }

        assertEquals(count, repository.findAll().size(), "Repository should contain all added accounts");
    }

    @Test
    void testEdit() throws ExecutionException, InterruptedException, TimeoutException {
        Account account = repository.add(
            SecurityVersion.LATEST,
            masterPassword,
            "OldSoftware",
            "olduser",
            "oldpass"
        ).get(5, TimeUnit.SECONDS);

        Account edited = repository.edit(
            SecurityVersion.LATEST,
            masterPassword,
            account,
            "NewSoftware",
            "newuser",
            "newpass"
        ).get(5, TimeUnit.SECONDS);

        assertNotNull(edited, "Edit should return updated account");
        assertEquals("NewSoftware", edited.getSoftware());
        assertEquals("newuser", edited.getUsername());
        assertEquals(1, repository.findAll().size(), "Repository should still contain one account");
    }

    @Test
    void testEditNonExistentAccount() {
        Account account = accounts.get(0);

        assertThrows(IllegalArgumentException.class, () -> {
            repository.edit(
                SecurityVersion.LATEST,
                masterPassword,
                account,
                "Software",
                "user",
                "pass"
            );
        }, "Editing non-existent account should throw IllegalArgumentException");
    }

    @Test
    void testRemove() throws ExecutionException, InterruptedException, TimeoutException {
        Account account = repository.add(
            SecurityVersion.LATEST,
            masterPassword,
            "TestSoftware",
            "testuser",
            "testpass"
        ).get(5, TimeUnit.SECONDS);

        Boolean removed = repository.remove(account).get(5, TimeUnit.SECONDS);

        assertTrue(removed, "Remove should return true");
        assertTrue(repository.findAll().isEmpty(), "Repository should be empty after removal");
    }

    @Test
    void testRemoveNonExistentAccount() {
        Account account = accounts.get(0);

        assertThrows(
            IllegalArgumentException.class,
            () -> repository.remove(account),
            "Removing non-existent account should throw IllegalArgumentException"
        );
    }

    @Test
    void testGetPassword() throws ExecutionException, InterruptedException, TimeoutException {
        String expectedPassword = "SecurePassword123!";
        Account account = repository.add(
            SecurityVersion.LATEST,
            masterPassword,
            "TestSoftware",
            "testuser",
            expectedPassword
        ).get(5, TimeUnit.SECONDS);

        String decryptedPassword = repository.getPassword(
            SecurityVersion.LATEST,
            masterPassword,
            account
        ).get(5, TimeUnit.SECONDS);

        assertEquals(expectedPassword, decryptedPassword, "Decrypted password should match original");
    }

    @Test
    void testSetAll() throws GeneralSecurityException {
        List<Account> testAccounts = accounts.subList(0, Math.min(5, accounts.size()));

        repository.setAll(testAccounts);

        assertEquals(testAccounts.size(), repository.findAll().size(), "Repository should contain all set accounts");
    }

    @Test
    void testFindAllIsUnmodifiable() throws ExecutionException, InterruptedException, TimeoutException {
        repository.add(
            SecurityVersion.LATEST,
            masterPassword,
            "TestSoftware",
            "testuser",
            "testpass"
        ).get(5, TimeUnit.SECONDS);

        assertThrows(
            UnsupportedOperationException.class,
            () -> repository.findAll().add(accounts.get(0)),
            "findAll should return unmodifiable list"
        );
    }

    @Test
    void testConcurrentOperations() throws ExecutionException, InterruptedException, TimeoutException {
        int operationCount = 10;
        CompletableFuture<?>[] futures = new CompletableFuture[operationCount];

        for (int i = 0; i < operationCount; i++) {
            System.out.println("Adding account " + i);
            final int index = i;
            futures[i] = repository.add(
                SecurityVersion.LATEST,
                masterPassword,
                "Software" + index,
                "user" + index,
                "pass" + index
            );
        }

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        assertEquals(operationCount, repository.findAll().size(),
            "All concurrent add operations should complete successfully");
    }

    @Test
    void testChangeMasterPassword() throws Exception {
        // Add accounts
        int count = Math.min(5, accounts.size());
        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository.add(
                SecurityVersion.LATEST,
                masterPassword,
                source.getSoftware(),
                source.getUsername(),
                "password" + i
            ).get(5, TimeUnit.SECONDS);
        }

        String newMasterPassword = "NewMasterPassword456!";
        List<Account> beforeChange = repository.findAll();

        CompletableFuture<Boolean> future = repository.changeMasterPassword(
            SecurityVersion.LATEST,
            masterPassword,
            newMasterPassword
        );
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result, "Master password should be changed successfully");

        // Try to get password with new master password
        for (Account account : beforeChange) {
            String password = repository.getPassword(
                SecurityVersion.LATEST,
                newMasterPassword,
                account
            ).get(5, TimeUnit.SECONDS);
            assertNotNull(password, "Password should be retrievable with new master password");
        }
    }

    @Test
    void testUpdateToLatestSecurityVersion() throws Exception {
        // Add accounts to repository
        int count = Math.min(5, accounts.size());
        List<String> expectedPasswords = new ArrayList<>();
        SecurityVersion initialVersion = SecurityVersion.PBKDF2;

        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            String password = "password_" + i;
            expectedPasswords.add(password);

            repository.add(
                initialVersion,
                masterPassword,
                source.getSoftware(),
                source.getUsername(),
                password
            ).get(5, TimeUnit.SECONDS);
        }

        List<Account> beforeUpdate = new ArrayList<>(repository.findAll());

        // Update all accounts to latest security version
        CompletableFuture<Boolean> future = repository.updateToLatestSecurityVersion(initialVersion, masterPassword);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result, "Update to latest security version should succeed");

        // Verify all passwords are still accessible and correct
        List<Account> afterUpdate = repository.findAll();
        assertEquals(beforeUpdate.size(), afterUpdate.size(), "Account count should remain the same");

        for (int i = 0; i < afterUpdate.size(); i++) {
            Account account = afterUpdate.get(i);
            String password = repository
                .getPassword(SecurityVersion.LATEST, masterPassword, account)
                .get(5, TimeUnit.SECONDS);

            assertEquals(expectedPasswords.get(i), password,
                "Password should remain the same after security version update");
        }
    }

    @Test
    void testUpdateToLatestSecurityVersionWithEmptyRepository() throws Exception {
        CompletableFuture<Boolean> future = repository.updateToLatestSecurityVersion(SecurityVersion.LATEST, masterPassword);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result, "Update on empty repository should succeed");
        assertEquals(0, repository.findAll().size(), "Repository should remain empty");
    }
}
