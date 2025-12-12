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

package password.manager.app.singletons;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

/**
 * Very small singleton registry to centralize create/getInstance behaviour used
 * across the project. This keeps the repetitive createInstance/getInstance
 * pattern in one place while remaining explicit in callers.
 */
public final class Singletons {
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    private Singletons() {}

    /**
     * Registers the singleton {@code cls} instance.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @param instance the instance to register
     * @throws IllegalStateException if the class is already registered
     */
    public static <T extends AutoCloseable> void register(@NotNull Class<T> cls, @NotNull T instance) {
        if (isRegistered(cls)) throw new IllegalStateException(cls.getName() + " is already registered");
        INSTANCES.put(cls, instance);
    }

    /**
     * Returns the singleton {@code cls} instance.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @return the singleton instance
     * @throws IllegalStateException if the class is not yet registered
     */
    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> @NotNull T get(@NotNull Class<T> cls) {
        Object inst = INSTANCES.get(cls);
        if (inst == null) throw new IllegalStateException(cls.getName() + " is not registered");
        return (T) inst;
    }

    /**
     * Returns whether the {@code cls} is already registered.
     *
     * @param cls the class to check
     * @return true if the class is registered, false otherwise
     */
    public static <T extends AutoCloseable> boolean isRegistered(@NotNull Class<T> cls) {
        return INSTANCES.containsKey(cls);
    }

    /**
     * Unregisters the singleton {@code cls} instance.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @throws IllegalStateException if the class is not registered
     */
    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> void unregister(@NotNull Class<T> cls) {
        if(!isRegistered(cls)) throw new IllegalStateException(cls.getName() + " is not registered");
        T instance =  (T) INSTANCES.remove(cls);

        try {
            instance.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close instance of " + cls.getName(), e);
        }
    }
}
