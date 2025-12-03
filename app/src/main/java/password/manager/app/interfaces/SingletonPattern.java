package password.manager.app.interfaces;

import java.lang.annotation.*;

/**
 * Marker interface indicating a class follows the singleton pattern.
 * <p>
 * Implementing classes should provide:
 * <ul>
 *   <li>
 *   public static synchronized void createInstance(...) throws IllegalStateException {
 *     Singletons.register(T.class, new T(...));
 *   }
 *   </li>
 *   <li>
 *   public static T getInstance() throws IllegalStateException {
 *     return Singletons.get(T.class);
 *   }
 *   </li>
 *   <li>
 *   public static synchronized void destroyInstance() throws IllegalStateException {
 *     Singletons.unregister(T.class);
 *   }
 *   </li>
 * </ul>
 * </p>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SingletonPattern {
}