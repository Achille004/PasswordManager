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
     * <p>
     * This method must be called exactly once per class, else it will throw an {@link IllegalStateException}.
     * </p>
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @param instance the instance to register
     * @throws IllegalStateException if the class is already registered
     */
    public static <T> void register(@NotNull Class<T> cls, @NotNull T instance) {
        if (isRegistered(cls)) throw new IllegalStateException(cls.getName() + " is already registered");
        INSTANCES.put(cls, instance);
    }

    /**
     * Returns the singleton {@code cls} instance.
     * <p>
     * This method should only be called after {@link #register} has been invoked for the provided class,
     * else it will throw an {@link IllegalStateException}.
     *
     * @param <T> the type of the class
     * @param cls the class of the instance
     * @return the singleton instance
     * @throws IllegalStateException if the class is not yet registered
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T get(@NotNull Class<T> cls) {
        Object inst = INSTANCES.get(cls);
        if (inst == null) throw new IllegalStateException(cls.getName() + " is not yet registered");
        return (T) inst;
    }

    /**
     * Returns whether the {@code cls} is already registered.
     *
     * @param <T> the type of the class
     * @param cls the class to check
     * @return true if the class is registered, false otherwise
     */
    public static <T> boolean isRegistered(@NotNull Class<T> cls) {
        return INSTANCES.containsKey(cls);
    }
}
