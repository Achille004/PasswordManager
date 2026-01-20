/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singletons;

public class TestSingletons {

    @Test
    public void testSingletonLifecycle() {
        // Test registration - register creates the instance automatically
        Singletons.register(TestResource.class);
        
        TestResource instance1 = Singletons.get(TestResource.class);
        assertNotNull(instance1);
        assertEquals("Hello", instance1.getValue());

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
        // Register different class types - instances created automatically
        Singletons.register(TestResource.class);
        Singletons.register(TestResourceWithNumber.class);
        Singletons.register(AnotherTestResource.class);

        // Get references to the created instances
        TestResource resource1 = Singletons.get(TestResource.class);
        TestResourceWithNumber resource2 = Singletons.get(TestResourceWithNumber.class);
        AnotherTestResource resource3 = Singletons.get(AnotherTestResource.class);

        // Verify all instances are retrievable with correct values
        assertEquals("Hello", resource1.getValue());
        assertEquals(42, resource2.getNumber());
        assertNotNull(resource3);

        // Verify they return the same instances
        assertSame(resource1, Singletons.get(TestResource.class));
        assertSame(resource2, Singletons.get(TestResourceWithNumber.class));
        assertSame(resource3, Singletons.get(AnotherTestResource.class));

        // Verify none are closed yet
        assertFalse(resource1.isClosed());
        assertFalse(resource2.isClosed());
        assertFalse(resource3.isClosed());

        // Clean up all singletons
        Singletons.shutdownAll();

        // Verify they were all closed
        assertTrue(resource1.isClosed());
        assertTrue(resource2.isClosed());
        assertTrue(resource3.isClosed());
    }

    @Test
    public void testShutdownOrder() {
        // Test that singletons are closed in reverse order (LIFO)
        Singletons.register(OrderedResource.class);
        Singletons.register(OrderedResource2.class);
        Singletons.register(OrderedResource3.class);

        // Retrieve to mock instance utilization
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
    public void testDoubleRegistrationThrowsException() {
        Singletons.register(TestResource.class);
        
        // Trying to register the same class again should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            Singletons.register(TestResource.class);
        });

        Singletons.shutdownAll();
    }

    @Test
    public void testGetNonRegisteredThrowsException() {
        // Trying to get a non-registered singleton should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            Singletons.get(TestResource.class);
        });
    }

    @Getter
    private static class TestResource implements AutoCloseable {
        private final String value;
        private boolean closed = false;

        public TestResource() {
            this.value = "Hello";
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Getter
    private static class TestResourceWithNumber implements AutoCloseable {
        private final int number;
        private boolean closed = false;

        public TestResourceWithNumber() {
            this.number = 42;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Getter
    private static class AnotherTestResource implements AutoCloseable {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    // Test resources to verify shutdown order
    private static class OrderedResource implements AutoCloseable {
        static final List<String> closeOrder = new ArrayList<>();

        @Override
        public void close() {
            closeOrder.add("OrderedResource");
        }
    }

    private static class OrderedResource2 implements AutoCloseable {
        @Override
        public void close() {
            OrderedResource.closeOrder.add("OrderedResource2");
        }
    }

    private static class OrderedResource3 implements AutoCloseable {
        @Override
        public void close() {
            OrderedResource.closeOrder.add("OrderedResource3");
        }
    }
}
