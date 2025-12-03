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
import java.util.concurrent.atomic.AtomicInteger;

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
        if (!repository.isShutdown()) repository.shutdown();
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
    void testExecuteOnAll() throws ExecutionException, InterruptedException, TimeoutException {
        int count = 5;
        for (int i = 0; i < count; i++) {
            repository.add(
                SecurityVersion.LATEST,
                masterPassword,
                "Software" + i,
                "user" + i,
                "pass" + i
            ).get(5, TimeUnit.SECONDS);
        }

        AtomicInteger counter = new AtomicInteger(0);
        repository.executeOnAll(account -> counter.incrementAndGet());

        // Give executor time to process
        Thread.sleep(1000);

        assertEquals(count, counter.get(), "ExecuteOnAll should process all accounts");
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
    void testShutdown() {
        assertFalse(repository.isShutdown(), "Repository should not be shut down initially");

        repository.shutdown();

        assertTrue(repository.isShutdown(), "Repository should be shut down after shutdown() call");
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
}
