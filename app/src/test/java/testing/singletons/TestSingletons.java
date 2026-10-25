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

package testing.singletons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singleton;
import password.manager.app.singletons.Singletons;

@SuppressWarnings("unused")
public class TestSingletons {

    @AfterEach
    public void cleanup() {
        // Ensure each test starts with a clean slate
        Singletons.shutdownAll();
    }

    @Test
    public void testSingletonLifecycle() {
        TestResource instance1 = Singletons.get(TestResource.class);
        assertNotNull(instance1);

        // Test get returns same instance
        TestResource instance2 = Singletons.get(TestResource.class);
        assertSame(instance1, instance2);

        // Test shutdownAll closes the instance
        assertFalse(instance1.isClosed());
        Singletons.shutdownAll();
        assertTrue(instance1.isClosed());
    }

    @Test
    public void testMultipleTypes() {
        TestResource r1 = Singletons.get(TestResource.class);
        TestResource2 r2 = Singletons.get(TestResource2.class);
        TestResource3 r3 = Singletons.get(TestResource3.class);

        // Verify all instances are retrievable with correct values
        assertNotNull(r1);
        assertNotNull(r2);
        assertNotNull(r3);

        // Verify they return the same instances
        assertSame(r1, Singletons.get(TestResource.class));
        assertSame(r2, Singletons.get(TestResource2.class));
        assertSame(r3, Singletons.get(TestResource3.class));

        // Verify none are closed yet
        assertFalse(r1.isClosed());
        assertFalse(r2.isClosed());
        assertFalse(r3.isClosed());

        // Clean up all singletons
        Singletons.shutdownAll();

        // Verify they were all closed
        assertTrue(r1.isClosed());
        assertTrue(r2.isClosed());
        assertTrue(r3.isClosed());
    }

    @Test
    public void testShutdownOrder() {
        OrderedResource r1 = Singletons.get(OrderedResource.class);
        OrderedResource2 r2 = Singletons.get(OrderedResource2.class);
        OrderedResource3 r3 = Singletons.get(OrderedResource3.class);

        // Clear the close order tracker
        OrderedResource.closeOrder.clear();

        // Shutdown all
        Singletons.shutdownAll();

        // Verify they were closed in reverse order: r3, r2, r1
        assertEquals(3, OrderedResource.closeOrder.size());
        assertEquals("OrderedResource3", OrderedResource.closeOrder.get(0));
        assertEquals("OrderedResource2", OrderedResource.closeOrder.get(1));
        assertEquals("OrderedResource", OrderedResource.closeOrder.get(2));
    }

    @Test
    public void testNoValidConstructor() {
        // Test with a class that truly has no no-arg constructor (not even private)
        Exception exception = assertThrows(
            RuntimeException.class,
            () -> Singletons.get(NoNoArgConstructorResource.class)
        );

        String expectedMessage = "Failed to create instance of";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage),
            "Expected message to contain '" + expectedMessage + "' but was: " + actualMessage);

        // Verify the cause is NoSuchMethodException
        assertNotNull(exception.getCause());
        assertInstanceOf(
            NoSuchMethodException.class,
            exception.getCause(),
            "Expected cause to be NoSuchMethodException but was: " + exception.getCause().getClass().getName()
        );
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<TestResource> instances = new ArrayList<>();
        final List<Thread> threads = new ArrayList<>();

        // Create multiple threads that all try to get the singleton
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                TestResource instance = Singletons.get(TestResource.class);
                synchronized (instances) {
                    instances.add(instance);
                }
                latch.countDown();
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        latch.await();
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all threads got the exact same instance
        assertEquals(threadCount, instances.size());
        TestResource first = instances.getFirst();
        for (TestResource instance : instances) {
            assertSame(first, instance, "All threads should get the same singleton instance");
        }
    }

    @Test
    public void testConcurrentDifferentSingletons() throws InterruptedException {
        final int threadCount = 15;
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger constructorCalls = new AtomicInteger(0);

        // Create threads that create different singletons simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    barrier.await(); // Sync all threads to start at the same time
                    switch (index % 3) {
                        case 0 -> Singletons.get(ConcurrentResource1.class);
                        case 1 -> Singletons.get(ConcurrentResource2.class);
                        default -> Singletons.get(ConcurrentResource3.class);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Verify each singleton was created exactly once
        assertNotNull(Singletons.get(ConcurrentResource1.class));
        assertNotNull(Singletons.get(ConcurrentResource2.class));
        assertNotNull(Singletons.get(ConcurrentResource3.class));

        // Verify they're singletons
        assertSame(
            Singletons.get(ConcurrentResource1.class),
            Singletons.get(ConcurrentResource1.class)
        );
    }

    @Test
    public void testNestedSingletonCreation() {
        // Test that creating a singleton that depends on another singleton works
        NestedDependentResource dependent = Singletons.get(NestedDependentResource.class);
        assertNotNull(dependent);
        assertNotNull(dependent.getDependency());

        // Verify the dependency is the same singleton
        assertSame(dependent.getDependency(), Singletons.get(NestedDependencyResource.class));
    }

    @Test
    public void testShutdownIdempotent() {
        TestResource resource = Singletons.get(TestResource.class);
        assertFalse(resource.isClosed());

        Singletons.shutdownAll();
        assertTrue(resource.isClosed());

        // Calling shutdownAll again should not throw
        assertDoesNotThrow(Singletons::shutdownAll);
    }

    @Test
    public void testOverlappingShutdownClosesEachInstanceAtMostOnce() throws Exception {
        BlockingCloseResource.closeCalls.set(0);
        BlockingCloseResource.closeStarted = new CountDownLatch(1);
        BlockingCloseResource.allowClose = new CountDownLatch(1);
        Singletons.get(BlockingCloseResource.class);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstShutdown = executor.submit(Singletons::shutdownAll);
            assertTrue(BlockingCloseResource.closeStarted.await(5, TimeUnit.SECONDS),
                    "The first shutdown should enter close()");

            Future<?> overlappingShutdown = executor.submit(Singletons::shutdownAll);
            overlappingShutdown.get(5, TimeUnit.SECONDS);
            assertEquals(1, BlockingCloseResource.closeCalls.get(),
                    "The overlapping shutdown must not close the drained instance again");

            BlockingCloseResource.allowClose.countDown();
            firstShutdown.get(5, TimeUnit.SECONDS);
            assertEquals(1, BlockingCloseResource.closeCalls.get());
        } finally {
            BlockingCloseResource.allowClose.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testCloseCanLookUpSingletonWithoutDeadlocking() throws Exception {
        DelayedLookupDependency.constructorEntered = new CountDownLatch(1);
        DelayedLookupDependency.allowConstructorToFinish = new CountDownLatch(1);
        LookupDuringCloseResource.failure.set(null);
        Singletons.get(LookupDuringCloseResource.class);

        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "SingletonShutdownTest");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> shutdown = executor.submit(Singletons::shutdownAll);
            shutdown.get(5, TimeUnit.SECONDS);
            assertNull(LookupDuringCloseResource.failure.get(),
                    "close() should finish its dependency lookup without errors");
        } finally {
            DelayedLookupDependency.allowConstructorToFinish.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testRecreateAfterShutdown() {
        TestResource resource1 = Singletons.get(TestResource.class);
        Singletons.shutdownAll();
        assertTrue(resource1.isClosed());

        // After shutdown, getting the singleton again should create a new instance
        TestResource resource2 = Singletons.get(TestResource.class);
        assertNotNull(resource2);
        assertNotSame(resource1, resource2);
        assertFalse(resource2.isClosed());
    }

    // Test resources to verify singleton behavior
    @Getter
    private static class TestResource extends Singleton {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    @Getter
    private static class TestResource2 extends Singleton {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    @Getter
    private static class TestResource3 extends Singleton {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    // Test resources to verify shutdown order
    private static class OrderedResource extends Singleton {
        static final List<String> closeOrder = new ArrayList<>();

        @Override
        public void close() {
            closeOrder.add("OrderedResource");
        }
    }

    private static class OrderedResource2 extends Singleton {
        @Override
        public void close() {
            OrderedResource.closeOrder.add("OrderedResource2");
        }
    }

    private static class OrderedResource3 extends Singleton {
        @Override
        public void close() {
            OrderedResource.closeOrder.add("OrderedResource3");
        }
    }

    private static class BlockingCloseResource extends Singleton {
        static final AtomicInteger closeCalls = new AtomicInteger();
        static CountDownLatch closeStarted;
        static CountDownLatch allowClose;

        @Override
        public void close() {
            closeCalls.incrementAndGet();
            closeStarted.countDown();
            try {
                allowClose.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class LookupDuringCloseResource extends Singleton {
        static final AtomicReference<Throwable> failure = new AtomicReference<>();

        @Override
        public void close() {
            Thread concurrentLookup = new Thread(
                    () -> Singletons.get(DelayedLookupDependency.class), "SingletonDependencyLookup");
            concurrentLookup.setDaemon(true);
            concurrentLookup.start();

            try {
                if (!DelayedLookupDependency.constructorEntered.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Dependency construction did not start");
                }

                // Let the other thread finish construction and attempt registry insertion while
                // this close() also requests that same singleton.
                DelayedLookupDependency.allowConstructorToFinish.countDown();
                assertNotNull(Singletons.get(DelayedLookupDependency.class));
                concurrentLookup.join(TimeUnit.SECONDS.toMillis(5));
                if (concurrentLookup.isAlive()) {
                    throw new AssertionError("Concurrent dependency lookup did not finish");
                }
            } catch (Throwable e) {
                failure.set(e);
            }
        }
    }

    private static class DelayedLookupDependency extends Singleton {
        static CountDownLatch constructorEntered;
        static CountDownLatch allowConstructorToFinish;

        public DelayedLookupDependency() {
            constructorEntered.countDown();
            try {
                allowConstructorToFinish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}
    }

    // Test resource that truly has NO no-arg constructor (not even private)
    private static class NoNoArgConstructorResource extends Singleton {
        // Only parameterized constructor - no no-arg constructor at all
        public NoNoArgConstructorResource(int x) {}

        @Override
        public void close() {}
    }

    // Test resources for concurrent access
    private static class ConcurrentResource1 extends Singleton {
        @Override
        public void close() {}
    }

    private static class ConcurrentResource2 extends Singleton {
        @Override
        public void close() {}
    }

    private static class ConcurrentResource3 extends Singleton {
        @Override
        public void close() {}
    }

    // Test resources for nested singleton creation
    @Getter
    private static class NestedDependencyResource extends Singleton {
        @Override
        public void close() {}
    }

    @Getter
    private static class NestedDependentResource extends Singleton {
        private final NestedDependencyResource dependency;

        public NestedDependentResource() {
            // This constructor calls Singletons.get() - testing nested calls
            this.dependency = Singletons.get(NestedDependencyResource.class);
        }

        @Override
        public void close() {}
    }
}
