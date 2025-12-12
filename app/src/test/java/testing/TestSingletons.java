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

import lombok.Getter;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singletons;

public class TestSingletons {

    @Test
    public void testSingletonLifecycle() {
        // Test registration
        assertFalse(Singletons.isRegistered(TestResource.class));

        TestResource instance1 = new TestResource("Hello");
        Singletons.register(TestResource.class, instance1);
        assertNotNull(Singletons.get(TestResource.class));
        assertEquals("Hello", Singletons.get(TestResource.class).getValue());

        // Test get returns same instance
        TestResource instance2 = Singletons.get(TestResource.class);
        assertSame(instance1, instance2);

        // Test unregister
        assertFalse(instance1.isClosed());
        Singletons.unregister(TestResource.class);
        assertTrue(instance1.isClosed());
        assertFalse(Singletons.isRegistered(TestResource.class));
    }

    @Test
    public void testMultipleTypes() {
        // Create instances of different AutoCloseable test types
        TestResource resource1 = new TestResource("Test1");
        TestResourceWithNumber resource2 = new TestResourceWithNumber(42);
        AnotherTestResource resource3 = new AnotherTestResource();

        // Register using different class types
        Singletons.register(TestResource.class, resource1);
        Singletons.register(TestResourceWithNumber.class, resource2);
        Singletons.register(AnotherTestResource.class, resource3);

        // Verify all instances are retrievable
        assertEquals("Test1", Singletons.get(TestResource.class).getValue());
        assertEquals(42, Singletons.get(TestResourceWithNumber.class).getNumber());
        assertNotNull(Singletons.get(AnotherTestResource.class));

        // Verify they return the same instances
        assertSame(resource1, Singletons.get(TestResource.class));
        assertSame(resource2, Singletons.get(TestResourceWithNumber.class));
        assertSame(resource3, Singletons.get(AnotherTestResource.class));

        // Verify none are closed yet
        assertFalse(resource1.isClosed());
        assertFalse(resource2.isClosed());
        assertFalse(resource3.isClosed());

        // Clean up
        Singletons.unregister(TestResource.class);
        Singletons.unregister(TestResourceWithNumber.class);
        Singletons.unregister(AnotherTestResource.class);

        // Verify they were closed
        assertTrue(resource1.isClosed());
        assertTrue(resource2.isClosed());
        assertTrue(resource3.isClosed());

        // Verify they're no longer registered
        assertFalse(Singletons.isRegistered(TestResource.class));
        assertFalse(Singletons.isRegistered(TestResourceWithNumber.class));
        assertFalse(Singletons.isRegistered(AnotherTestResource.class));
    }

    @Getter
    private static class TestResource implements AutoCloseable {
        private final String value;
        private boolean closed = false;

        public TestResource(String value) {
            this.value = value;
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

        public TestResourceWithNumber(int number) {
            this.number = number;
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
}
