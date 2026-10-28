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
package password.manager.app.base

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.SingleSelectionModel

/**
 * A [SingleSelectionModel] backed by an observable list that preserves its selected item while the
 * list is reordered.
 *
 * A ComboBox skin may clear its selection temporarily when a list permutation moves the selected
 * item. That transient clear can propagate as a null value even though the item is still present.
 * This model tracks the selected item across list changes and ignores clear or null-selection
 * requests while that item remains in [items]. If the item is removed, the selection can be cleared
 * normally.
 */
class NonNullSelectionModel<T>(
    private val items: ObservableList<T>
) : SingleSelectionModel<T>() {
    init {
        // A permutation changes the selected item's index without changing the selected item itself.
        items.addListener(ListChangeListener { preserveSelectedItem() })
    }

    override fun getModelItem(index: Int): T? = items.getOrNull(index)

    override fun getItemCount(): Int = items.size

    override fun clearSelection() {
        // Ignore the ComboBox skin's transient clear during a permutation.
        if (hasSelectedItemInList()) return
        super.clearSelection()
    }

    override fun select(obj: T?) {
        // Keep the existing selection if the skin tries to replace it with null during that clear.
        if (obj == null && hasSelectedItemInList()) return
        super.select(obj)
    }

    private fun preserveSelectedItem() {
        val selected = selectedItem ?: return
        val newIndex = items.indexOf(selected)
        if (newIndex == selectedIndex) return
        // Re-select by item so the model updates the index to the item's position after sorting.
        if (newIndex < 0) clearSelection() else select(newIndex)
    }

    private fun hasSelectedItemInList(): Boolean =
        selectedItem?.let(items::contains) == true
}
