/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
 */
package testing.singletons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singleton;
import password.manager.app.singletons.SingletonRegistry;

class TestSingletonRegistry {
    private final SingletonRegistry registry = new SingletonRegistry();

    @AfterEach
    void cleanup() {
        registry.shutdownAll();
    }

    @Test
    void registersAndReturnsTheSuppliedInstance() {
        TestResource resource = new TestResource();

        assertSame(resource, registry.register(resource));
        assertSame(resource, registry.register(resource));
    }

    @Test
    void rejectsDifferentInstancesOfTheSameClass() {
        TestResource first = new TestResource();
        TestResource second = new TestResource();

        registry.register(first);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> registry.register(second));
        assertTrue(error.getMessage().contains(TestResource.class.getName()));
        assertEquals(0, second.closeCalls.get(), "Rejected instances remain owned by their caller");
    }

    @Test
    void closesInReverseRegistrationOrderAndOnlyOnce() {
        List<String> closeOrder = new ArrayList<>();
        FirstResource first = new FirstResource(closeOrder);
        SecondResource second = new SecondResource(closeOrder);
        registry.register(first);
        registry.register(second);

        registry.shutdownAll();
        registry.shutdownAll();

        assertEquals(List.of("second", "first"), closeOrder);
    }

    @Test
    void kotlinObjectInstanceCanBeRegisteredAndClosed() {
        KotlinObjectStyleResource resource = KotlinObjectStyleResource.INSTANCE;

        assertSame(resource, registry.register(resource));
        registry.shutdownAll();

        assertEquals(1, resource.closeCalls.get());
    }

    @Test
    void canRegisterAgainAfterShutdown() {
        TestResource first = new TestResource();
        registry.register(first);
        registry.shutdownAll();

        TestResource second = new TestResource();
        assertSame(second, registry.register(second));
        registry.shutdownAll();
        assertEquals(1, first.closeCalls.get());
        assertEquals(1, second.closeCalls.get());
    }

    @Test
    void rejectsRegistrationWhileShutdownIsClosingInstances() throws Exception {
        CountDownLatch closeStarted = new CountDownLatch(1);
        CountDownLatch finishClose = new CountDownLatch(1);
        registry.register(new BlockingCloseResource(closeStarted, finishClose));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> shutdown = executor.submit(registry::shutdownAll);
            assertTrue(closeStarted.await(5, TimeUnit.SECONDS));

            TestResource lateResource = new TestResource();
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> registry.register(lateResource));
            assertTrue(error.getMessage().contains("shutdown is in progress"));
            assertEquals(0, lateResource.closeCalls.get());

            finishClose.countDown();
            shutdown.get(5, TimeUnit.SECONDS);
        } finally {
            finishClose.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static final class TestResource extends Singleton {
        private final AtomicInteger closeCalls = new AtomicInteger();
        @Override public void close() { closeCalls.incrementAndGet(); }
    }

    private static final class FirstResource extends Singleton {
        private final List<String> order;
        private FirstResource(List<String> order) { this.order = order; }
        @Override public void close() { order.add("first"); }
    }

    private static final class SecondResource extends Singleton {
        private final List<String> order;
        private SecondResource(List<String> order) { this.order = order; }
        @Override public void close() { order.add("second"); }
    }

    private static final class BlockingCloseResource extends Singleton {
        private final CountDownLatch closeStarted;
        private final CountDownLatch finishClose;
        private BlockingCloseResource(CountDownLatch closeStarted, CountDownLatch finishClose) {
            this.closeStarted = closeStarted;
            this.finishClose = finishClose;
        }
        @Override public void close() {
            closeStarted.countDown();
            try {
                if (!finishClose.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Test did not release close()");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Mirrors the public static INSTANCE field emitted for a Kotlin object. */
    private static final class KotlinObjectStyleResource extends Singleton {
        private static final KotlinObjectStyleResource INSTANCE = new KotlinObjectStyleResource();
        private final AtomicInteger closeCalls = new AtomicInteger();
        private KotlinObjectStyleResource() {}
        @Override public void close() { closeCalls.incrementAndGet(); }
    }
}
