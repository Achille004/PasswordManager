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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Very small singleton registry to centralize create/getInstance behaviour used
 * across the project. This keeps the repetitive createInstance/getInstance
 * pattern in one place while remaining explicit in callers.
 * <p>
 * Singletons are closed in reverse order of registration when {@link #shutdownAll()} is called.
 */
public final class Singletons {

    // LinkedHashMap maintains insertion order, allowing reverse-order shutdown
    private static final Map<Class<? extends AutoCloseable>, AutoCloseable> INSTANCES = 
        Collections.synchronizedMap(new LinkedHashMap<>());

    static {
        // Register shutdown hook to ensure clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdownAll();
            } catch (Exception e) {
                System.err.println("Error during singleton shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SingletonShutdownHook"));
    }

    private Singletons() {}

    /**
     * Registers the class {@code cls} as a singleton using the default constructor.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @param supplier the supplier to create the instance
     * @throws IllegalStateException if the class is already registered as singleton candidate
     */
    public static synchronized <T extends AutoCloseable> void register(@NotNull Class<T> cls) {
        if (INSTANCES.containsKey(cls)) throw new IllegalStateException(cls.getName() + " is already registered as singleton candidate");

        try {
            INSTANCES.put(cls, cls.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + cls.getName(), e);
        }
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
    public static @NotNull <T extends AutoCloseable> T get(@NotNull Class<T> cls) {
        T inst = (T) INSTANCES.get(cls);
        if (inst == null) throw new IllegalStateException(cls.getName() + " is not registered as singleton candidate");
        return inst;
    }

    /**
     * Shuts down all registered singletons in reverse order of registration.
     * This ensures that singletons created later (which may depend on earlier ones)
     * are closed first.
     * <p>
     * This method is idempotent and can be called multiple times safely.
     * After calling this method, all singletons are unregistered.
     */
    public static void shutdownAll() {
        synchronized (INSTANCES) {
            // Create a list in reverse order
            List<Map.Entry<Class<? extends AutoCloseable>, AutoCloseable>> entries = 
                new ArrayList<>(INSTANCES.entrySet());
            Collections.reverse(entries);

            // Close each singleton in reverse order
            for (Map.Entry<Class<? extends AutoCloseable>, AutoCloseable> entry : entries) {
                try {
                    entry.getValue().close();
                } catch (Exception e) {
                    // Log error but continue closing others
                    System.err.println("Failed to close instance of " + entry.getKey().getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Clear all instances after shutdown
            INSTANCES.clear();
        }
    }
}
