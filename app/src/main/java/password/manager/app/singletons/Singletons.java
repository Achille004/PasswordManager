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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.NotNull;

/**
 * Very small singleton registry to centralize create/getInstance behaviour used
 * across the project. This keeps the repetitive createInstance/getInstance
 * pattern in one place while remaining explicit in callers.
 * <p>
 * Singletons are closed in reverse order of registration when {@link #shutdownAll()} is called.
 * <p>
 * Thread-safe implementation using ReadWriteLock: multiple threads can read concurrently
 * (fast path when instances already exist), while writes are exclusive and per-class synchronized.
 */
public final class Singletons {

    // LinkedHashMap maintains insertion order for reverse-order shutdown
    private static final Map<Class<? extends Singleton>, Singleton> INSTANCES = new LinkedHashMap<>();
    private static final ReadWriteLock INSTANCES_LOCK = new ReentrantReadWriteLock();

    static {
        // Register shutdown hook to ensure clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(Singletons::shutdownAll, "SingletonShutdownHook"));
    }

    private Singletons() {}

    /**
     * Returns the singleton {@code cls} instance.
     * <p>
     * Uses read-write locking with per-class synchronization:
     * - Read lock for the fast path (checking if instance exists)
     * - Write lock + per-class sync for lazy initialization
     * This allows multiple threads to read concurrently while ensuring safe lazy initialization.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @return the singleton instance
     * @throws RuntimeException if instantiation fails
     */
    @SuppressWarnings("unchecked")
    public static @NotNull <T extends Singleton> T get(@NotNull Class<T> cls) throws RuntimeException {
        // Fast path: read lock allows multiple concurrent reads
        INSTANCES_LOCK.readLock().lock();
        try {
            Singleton inst = INSTANCES.get(cls);
            if (inst != null) return (T) inst;
        } finally {
            INSTANCES_LOCK.readLock().unlock();
        }

        // Slow path: synchronize on the specific class to allow concurrent initialization of different singletons
        synchronized (cls) {
            // Double-check with read lock: another thread might have created it
            INSTANCES_LOCK.readLock().lock();
            try {
                Singleton inst = INSTANCES.get(cls);
                if (inst != null) return (T) inst;
            } finally {
                INSTANCES_LOCK.readLock().unlock();
            }

            // Create the instance (outside of INSTANCES_LOCK to avoid holding lock during construction)
            Singleton inst;
            try {
                Constructor<T> constr = cls.getDeclaredConstructor();
                constr.setAccessible(true);
                inst = constr.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to create instance of " + cls.getName(), e);
            }

            // Acquire write lock to store the instance
            INSTANCES_LOCK.writeLock().lock();
            try {
                // Triple-check: another thread might have stored it while we were constructing
                Singleton existing = INSTANCES.get(cls);
                if (existing != null) {
                    // Another thread beat us to it, close ours and return the existing one
                    try {
                        inst.close();
                    } catch (Exception e) {
                        System.err.println("Failed to close duplicate instance of " + cls.getName() + ": " + e.getMessage());
                    }
                    return (T) existing;
                }

                // Store the new instance
                INSTANCES.put(cls, inst);
                return (T) inst;
            } finally {
                INSTANCES_LOCK.writeLock().unlock();
            }
        }
    }

    /**
     * Shuts down all registered singletons in reverse order of registration,
     * thus enabling simple linear dependencies between singletons.
     * <p>
     * This method is idempotent and can be called multiple times safely.
     * After calling this method, all singletons are unregistered.
     */
    public static void shutdownAll() {
        INSTANCES_LOCK.writeLock().lock();
        try {
            // Get classes in reverse order of registration
            List<Class<? extends Singleton>> classes = new ArrayList<>(INSTANCES.keySet());
            Collections.reverse(classes);

            // Close each singleton in reverse order
            for (Class<? extends Singleton> cls : classes) {
                try {
                    Singleton inst = INSTANCES.get(cls);
                    if (inst != null) {
                        inst.close();
                    }
                } catch (Exception e) {
                    // Log error but continue closing others
                    System.err.println("Failed to close instance of " + cls.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Clear all instances after shutdown
            INSTANCES.clear();
        } finally {
            INSTANCES_LOCK.writeLock().unlock();
        }
    }
}
