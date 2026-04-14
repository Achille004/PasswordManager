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
package password.manager.app.controllers

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Pane
import javafx.util.Callback
import password.manager.app.Utils
import java.util.IdentityHashMap
import java.util.function.BiConsumer

/**
 * Manages tabs within a [TabPane], allowing dynamic creation, selection, and closure of tabs associated with specific items.
 *
 * _This class does not handle the tabs directly, so you can still manage special tabs (like a "+" tab) externally._
 * @param T the type of the items associated with the tabs
 * @param U the type of the controllers associated with the tabs
 */
class TabManager<T, U : AbstractController>(
    private val tabPane: TabPane,
    private val controllerConstructor: Callback<T, U>,
    private val tabInitializer: BiConsumer<Tab, T>
) {
    private val tabsMap = IdentityHashMap<T, Tab>() // Map for two-way association and caching

    init {
        val tabFocusHandler = ChangeListener { _: ObservableValue<out Tab>, _: Tab, newTab: Tab ->
            getController<U>(newTab).reset()
        }
        tabPane.selectionModel.selectedItemProperty().addListener(tabFocusHandler)
    }

    fun openTab(item: T) = selectTab(
        tabsMap.computeIfAbsent(item, this::createTab),
        true
    )

    fun createTab(item: T) = loadTab(
        controllerConstructor.call(item)
    ) .also {
        tabInitializer.accept(it, item)
    }

    @JvmOverloads
    fun selectTab(tab: Tab, addIfMissing: Boolean = false) {
        if (addIfMissing && !tabPane.tabs.contains(tab)) {
            tabPane.tabs.add(tabPane.tabs.size - 1, tab)
        }
        tabPane.selectionModel.select(tab)
    }

    fun selectAdjacentTab(direction: Int) {
        val currentIndex = tabPane.selectionModel.selectedIndex
        val newIndex = (currentIndex + direction) % tabPane.tabs.size // Circular
        tabPane.selectionModel.select(newIndex)
    }

    fun closeTab(tab: Tab) = tabPane.tabs.remove(tab)

    fun closeTab(item: T) = tabsMap[item]
        ?.let { closeTab(it) }
        ?: false

    fun removeTab(item: T) = tabsMap.remove(item)
        ?.let { closeTab(it) }
        ?: false

    // Utility methods for tab management
    companion object {

        /**
         * Load a new tab with the given controller.
         * @param U the type of the controller
         * @param controller the controller to associate with the tab
         */
        @JvmStatic
        fun <U : AbstractController> loadTab(controller: U) = loadTab(Tab(), controller)

        /**
         * Load a new tab with the given controller.
         * @param U the type of the controller
         * @param controller the controller to associate with the tab
         */
        @JvmStatic
        fun <U : AbstractController> loadTab(tab: Tab, controller: U) = tab .also {
            it.content = Utils.loadFxml(controller) as Pane
            it.properties["controller"] = controller
        }

        /**
         * Get the controller associated with a tab.
         * @param U the type of the controller
         * @param tab the tab
         * @return the controller associated with the tab
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <U : AbstractController> getController(tab: Tab) = tab.properties["controller"] as U
    }
}