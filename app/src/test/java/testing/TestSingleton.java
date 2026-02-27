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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singleton;
import password.manager.app.singletons.Singletons;

public class TestSingleton {

    @Test
    public void testImplementing() {
        assertNotNull(ImplementingClass.getInstance());
    }

    @Test
    public void testNonImplementing() {
        assertThrows(UnsupportedOperationException.class, NonImplementingClass::getInstance);
    }

    private static class ImplementingClass extends Singleton {
        @Override
        public void close() {}

        public static @NotNull ImplementingClass getInstance() {
            return Singletons.get(ImplementingClass.class);
        }
    }

    private static class NonImplementingClass extends Singleton {
        @Override
        public void close() {}
    }
}
