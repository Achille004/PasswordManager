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
package password.manager.app.singletons

import java.lang.AutoCloseable

/** Base type for application objects managed and closed by [SingletonRegistry]. */
abstract class Singleton : AutoCloseable {
    /**
     * Registers this fully initialized object with the application registry.
     * Call from the end of a concrete object's initializer, not from this base class constructor.
     */
    protected fun registerSingleton() {
        SingletonRegistry.global.register(this)
    }
}
