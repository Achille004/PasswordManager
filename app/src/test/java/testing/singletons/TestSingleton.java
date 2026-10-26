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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singleton;
import password.manager.app.singletons.SingletonRegistry;

public class TestSingleton {
    private final SingletonRegistry registry = new SingletonRegistry();

    @AfterEach
    public void cleanup() {
        registry.shutdownAll();
    }

    @Test
    public void testShutdownContinuesWhenCloseThrows() {
        FailingResource failing = new FailingResource();
        ClosingResource closing = new ClosingResource();
        registry.register(failing);
        registry.register(closing);

        assertDoesNotThrow(registry::shutdownAll);
        assertEquals(1, failing.closeCalls);
        assertEquals(1, closing.closeCalls);
    }

    private static final class FailingResource extends Singleton {
        private int closeCalls;
        @Override public void close() {
            closeCalls++;
            throw new IllegalStateException("expected test failure");
        }
    }

    private static final class ClosingResource extends Singleton {
        private int closeCalls;
        @Override public void close() { closeCalls++; }
    }
}
