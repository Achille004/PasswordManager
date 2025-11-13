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

package password.manager.lib;

/**
 * Interface for controls that need to hide/show additional elements during loading animations.
 * For example, password strength indicators that should be hidden while loading.
 */
public interface AnimationAwareControl {
    /**
     * Hides additional UI elements when loading animation starts.
     */
    void hideExtraElements();
    
    /**
     * Shows additional UI elements when loading animation stops.
     */
    void showExtraElements();
}