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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import password.manager.app.enums.SecurityVersion;
import password.manager.app.security.Account;
import password.manager.app.security.AccountRepository;
import password.manager.app.singletons.Logger;

public class TestAccountRepository {

    private static final List<Account> accounts = new ArrayList<>();

    private AccountRepository repository;
    private ObjectProperty<SecurityVersion> securityVersionProperty;
    private StringProperty masterPasswordProperty;

    @BeforeAll
    static void populate() throws GeneralSecurityException, IOException {
        try (InputStream stream = TestAccount.class.getResourceAsStream("/blns.txt")) {
            assert stream != null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String pass, software, username;

                while ((pass = readBlnsLine(reader)) != null &&
                       (software = readBlnsLine(reader)) != null &&
                       (username = readBlnsLine(reader)) != null) {

                    Account account = Account.of(SecurityVersion.LATEST, software, username, pass, "MasterPassword123!");
                    accounts.add(account);
                }
            }
        }

    }

    @BeforeEach
    void setUp() {
        securityVersionProperty = new SimpleObjectProperty<>(SecurityVersion.LATEST);
        masterPasswordProperty = new SimpleStringProperty("MasterPassword123!");
        repository = new AccountRepository(securityVersionProperty, masterPasswordProperty);
    }

    @AfterEach
    void tearDown() {
        repository.close();
        Logger.destroyInstance();
    }

    @Test
    void testInitiallyEmpty() {
        TestingUtils.initLogger();

        assertTrue(repository.findAll().isEmpty(), "Repository should be empty on initialization");
    }

    @Test
    void testAdd() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        CompletableFuture<Account> future = repository.add("GitHub", "testuser", "testpass123");
        Account account = future.get(5, TimeUnit.SECONDS);

        assertNotNull(account, "Account should be created successfully");
        assertEquals(1, repository.findAll().size(), "Repository should contain one account");
        assertEquals("GitHub", account.getSoftware());
        assertEquals("testuser", account.getUsername());
    }

    @Test
    void testAddMultiple() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        int count = Math.min(10, accounts.size());

        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository
                    .add(source.getSoftware(), source.getUsername(), "password" + i)
                    .get(5, TimeUnit.SECONDS);
        }

        assertEquals(count, repository.findAll().size(), "Repository should contain all added accounts");
    }

    @Test
    void testEdit() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        Account account = repository
                .add("OldSoftware", "olduser", "oldpass")
                .get(5, TimeUnit.SECONDS);

        Account edited = repository
                .edit(account, "NewSoftware", "newuser", "newpass")
                .get(5, TimeUnit.SECONDS);

        assertNotNull(edited, "Edit should return updated account");
        assertEquals("NewSoftware", edited.getSoftware());
        assertEquals("newuser", edited.getUsername());
        assertEquals(1, repository.findAll().size(), "Repository should still contain one account");
    }

    @Test
    void testEditNonExistentAccount() {
        TestingUtils.initLogger();

        Account account = accounts.get(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.edit(account, "Software", "user", "pass"),
                "Editing non-existent account should throw IllegalArgumentException"
        );
    }

    @Test
    void testRemove() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        Account account = repository
                .add("TestSoftware", "testuser", "testpass")
                .get(5, TimeUnit.SECONDS);

        Boolean removed = repository.remove(account).get(5, TimeUnit.SECONDS);

        assertTrue(removed, "Remove should return true");
        assertTrue(repository.findAll().isEmpty(), "Repository should be empty after removal");
    }

    @Test
    void testRemoveNonExistentAccount() {
        TestingUtils.initLogger();

        Account account = accounts.get(0);

        assertThrows(
            IllegalArgumentException.class,
            () -> repository.remove(account),
            "Removing non-existent account should throw IllegalArgumentException"
        );
    }

    @Test
    void testGetPassword() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        String expectedPassword = "SecurePassword123!";
        Account account = repository
                .add("TestSoftware", "testuser", expectedPassword)
                .get(5, TimeUnit.SECONDS);

        String decryptedPassword = repository
                .getPassword(account)
                .get(5, TimeUnit.SECONDS);

        assertEquals(expectedPassword, decryptedPassword, "Decrypted password should match original");
    }

    @Test
    void testSetAll() throws GeneralSecurityException {
        TestingUtils.initLogger();

        List<Account> testAccounts = accounts.subList(0, Math.min(5, accounts.size()));

        repository.setAll(testAccounts);

        assertEquals(testAccounts.size(), repository.findAll().size(), "Repository should contain all set accounts");
    }

    @Test
    void testNonEmptySetAll() throws GeneralSecurityException, ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        repository
                .add("InitialSoftware", "initialuser", "initialpass")
                .get(5, TimeUnit.SECONDS);

        List<Account> testAccounts = accounts.subList(0, Math.min(5, accounts.size()));

        assertThrows(
            IllegalStateException.class,
            () -> repository.setAll(testAccounts),
            "setAll on non-empty repository should throw IllegalStateException"
        );
    }

    @Test
    void testFindAllIsUnmodifiable() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        repository
                .add("TestSoftware", "testuser", "testpass")
                .get(5, TimeUnit.SECONDS);

        assertThrows(
            UnsupportedOperationException.class,
            () -> repository.findAll().add(accounts.get(0)),
            "findAll should return unmodifiable list"
        );
    }

    @Test
    void testConcurrentOperations() throws ExecutionException, InterruptedException, TimeoutException {
        TestingUtils.initLogger();

        int operationCount = 10;
        CompletableFuture<?>[] futures = new CompletableFuture[operationCount];

        for (int i = 0; i < operationCount; i++) {
            System.out.println("Adding account " + i);
            final int index = i;
            futures[i] = repository.add("Software" + index, "user" + index, "pass" + index);
        }

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        assertEquals(operationCount, repository.findAll().size(),
            "All concurrent add operations should complete successfully");
    }

    @Test
    void testChangeMasterPassword() throws Exception {
        TestingUtils.initLogger();

        // Add accounts
        int count = Math.min(5, accounts.size());
        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository
                    .add(source.getSoftware(), source.getUsername(), "password" + i)
                    .get(5, TimeUnit.SECONDS);
        }

        // Change master password
        masterPasswordProperty.set("NewMasterPassword456!");

        // Try to get password with new master password
        for (Account account : repository.findAll()) {
            Logger.getInstance().addDebug("Testing account: " + account.getSoftware() + " / " +  account.getUsername());
            String password = repository.getPassword(account).get(5, TimeUnit.SECONDS);
            assertNotNull(password, "Password should be retrievable with new master password");
        }
    }

    @Test
    void testChangeMasterPasswordFromNull() throws Exception {
        TestingUtils.initLogger();

        // Set initial master password to null
        masterPasswordProperty.set(null);

        // Add accounts
        int count = Math.min(5, accounts.size());
        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository
                    .add(source.getSoftware(), source.getUsername(), "password" + i)
                    .get(5, TimeUnit.SECONDS);
        }

        // Change master password from null to a valid password
        masterPasswordProperty.set("InitialMasterPassword123!");

        // Try to get password with new master password
        for (Account account : repository.findAll()) {
            Logger.getInstance().addDebug("Testing account: " + account.getSoftware() + " / " +  account.getUsername());
            String password = repository.getPassword(account).get(5, TimeUnit.SECONDS);
            assertNotNull(password, "Password should be retrievable with new master password");
        }
    }

    @Test
    void testUpdateToLatestSecurityVersion() throws Exception {
        TestingUtils.initLogger();

        // Set initial security version to PBKDF2, future execute on the empty repository (no changes affecting accounts)
        securityVersionProperty.set(SecurityVersion.PBKDF2);

        // Add accounts to repository
        int count = Math.min(5, accounts.size());
        List<String> expectedPasswords = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            String password = "password_" + i;
            expectedPasswords.add(password);

            repository
                .add(source.getSoftware(), source.getUsername(), password)
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
            String password = repository
                .getPassword(account)
                .get(5, TimeUnit.SECONDS);

            assertEquals(expectedPasswords.get(i), password,
                "Password should remain the same after security version update");
        }
    }

    @Test
    void testUpdateToLatestSecurityVersionWithEmptyRepository() throws Exception {
        TestingUtils.initLogger();

        securityVersionProperty.set(SecurityVersion.PBKDF2);
        securityVersionProperty.set(SecurityVersion.LATEST);

        assertEquals(0, repository.findAll().size(), "Repository should remain empty");
    }

    @Test
    void testUpdateToLatestSecurityVersionThenChangeMasterPassword() throws Exception {
        TestingUtils.initLogger();

        securityVersionProperty.set(SecurityVersion.PBKDF2);

        // Add accounts
        int count = Math.min(5, accounts.size());
        for (int i = 0; i < count; i++) {
            Account source = accounts.get(i);
            repository
                    .add(source.getSoftware(), source.getUsername(), "password" + i)
                    .get(5, TimeUnit.SECONDS);
        }

        List<Account> accounts = repository.findAll();
        securityVersionProperty.set(SecurityVersion.LATEST);
        masterPasswordProperty.set("NewMasterPassword456!");

        // Try to get password with new master password
        for (Account account : accounts) {
            String password = repository.getPassword(account).get(5, TimeUnit.SECONDS);
            assertNotNull(password, "Password should be retrievable with new master password");
        }
    }
}
