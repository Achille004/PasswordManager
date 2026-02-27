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

package password.manager.app.singletons;

/**
 * Abstract base class for components that are intended to be used as singletons.
 * Subclasses are expected to implement {@link #getInstance} method to expose a 
 * single instance of the subclass.
 */
public abstract class Singleton implements AutoCloseable {
    /**
     * Returns the singleton instance of the class.
     *
     * @return the singleton instance
     */    
    public static Singleton getInstance() {
        throw new UnsupportedOperationException("This method should be overridden in subclasses.");
    }
}