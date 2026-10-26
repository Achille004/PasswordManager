/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
 */
package password.manager.app.singletons

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/** Registry with independent lifecycle, also usable by tests without touching application state. */
class SingletonRegistry {
    companion object {
        /** Registry for the application process; tests can create isolated registries. */
        @JvmField
        val global = SingletonRegistry()
    }

    private val instances: MutableMap<Class<out Singleton>, Singleton> = LinkedHashMap()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private var shutdownsInProgress = 0
    private var shutdownHook: Thread? = null

    /** Installs one JVM shutdown hook for this registry. */
    fun installShutdownHook() {
        synchronized(this) {
            if (shutdownHook != null) return

            val hook = Thread({ shutdownAll() }, "SingletonShutdownHook")
            Runtime.getRuntime().addShutdownHook(hook)
            shutdownHook = hook
        }
    }

    /**
     * Registers and returns the supplied singleton object.
     * @throws IllegalStateException if another instance of the class is already registered or
     * registration races with an in-progress shutdown.
     */
    fun <T : Singleton> register(instance: T): T {
        val cls = instance.javaClass

        lock.readLock().withLock {
            val existing = instances[cls]
            if (existing != null) {
                check(existing === instance) {
                    "A different singleton instance is already registered for ${cls.name}"
                }
                return instance
            }
        }

        lock.writeLock().withLock {
            check(shutdownsInProgress == 0) {
                "Cannot register ${cls.name} while singleton shutdown is in progress"
            }

            val existing = instances[cls]
            check(existing == null || existing === instance) {
                "A different singleton instance is already registered for ${cls.name}"
            }

            if (existing == null) instances[cls] = instance
        }

        return instance
    }

    /** Closes registered instances in reverse registration order. */
    fun shutdownAll() {
        val toClose = ArrayList<Singleton>()

        lock.writeLock().withLock {
            shutdownsInProgress++
            instances.keys.toList().asReversed().forEach { cls ->
                instances.remove(cls)?.let(toClose::add)
            }
        }

        try {
            for (instance in toClose) {
                try {
                    instance.close()
                } catch (e: Exception) {
                    System.err.println("Failed to close instance of ${instance.javaClass.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
        } finally {
            lock.writeLock().withLock { shutdownsInProgress-- }
        }
    }
}
