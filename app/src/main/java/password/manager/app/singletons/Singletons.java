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
        if (inst == null) throw new IllegalStateException(cls.getName() + " is not yet registered");
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
