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

package password.manager.app.interfaces;

import java.lang.annotation.*;

/**
 * Marker interface indicating a class follows the singleton pattern.
 * <p>
 * Implementing classes should provide:
 * <ul>
 *   <li><pre>
 *   public static synchronized void createInstance(...) throws IllegalStateException {
 *     Singletons.register(T.class, new T(...));
 *   }
 *   </pre></li>
 *   <li><pre>
 *   public static T getInstance() throws IllegalStateException {
 *     return Singletons.get(T.class);
 *   }
 *   </pre></li>
 *   <li><pre>
 *   public static synchronized void destroyInstance() throws IllegalStateException {
 *     Singletons.unregister(T.class);
 *   }
 *   </pre></li>
 * </ul>
 * </p>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SingletonPattern {
}