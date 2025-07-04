/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.camera.integration.core.button

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatButton
import androidx.arch.core.util.Function
import androidx.core.util.Consumer

/**
 * A custom button that displays a popup menu to select from a list of items.
 *
 * @param <T> The type of items that can be selected.
 */
abstract class SelectButton<T>
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatButton(context, attrs, defStyleAttr) {

    private val popupMenu: PopupMenu = PopupMenu(getContext(), this)
    private var selectedItem: T? = null
    private var allowedItems: List<T?> = listOf(null)
    private var itemChangedListener: Consumer<T?>? = null
    private var iconNameProvider: Function<T?, String>? = null
    private var menuItemNameProvider: Function<T?, String>? = null
    private val refreshPopupMenuRunnable = Runnable { refreshPopupMenu() }

    init {
        setOnClickListener { popupMenu.show() }
    }

    /**
     * Sets the list of allowed items for selection.
     *
     * @param allowedItems The collection of allowed items.
     */
    fun setAllowedItems(allowedItems: Collection<T?>) {
        this.allowedItems =
            allowedItems.toList().also {
                if (!it.contains(selectedItem)) {
                    selectedItem = it.getOrNull(0)
                }
            }

        postRefreshPopupMenu()
    }

    /**
     * Sets the provider function for generating the button's icon name based on the selected item.
     *
     * @param provider The function to provide icon names.
     */
    fun setIconNameProvider(provider: Function<T?, String>) {
        iconNameProvider = provider
        postRefreshPopupMenu()
    }

    /**
     * Sets the provider function for generating the menu item names based on the allowed items.
     *
     * @param provider The function to provide menu item names.
     */
    fun setMenuItemNameProvider(provider: Function<T?, String>) {
        menuItemNameProvider = provider
        postRefreshPopupMenu()
    }

    /**
     * Gets the currently selected item.
     *
     * @return The selected item, or null if none is selected.
     */
    fun getSelectedItem(): T? {
        return selectedItem
    }

    /**
     * Sets the selected item.
     *
     * @param itemToSelect The item to select.
     * @throws IllegalArgumentException if the selected item is not in the allowed items list.
     */
    fun setSelectedItem(itemToSelect: T?) {
        if (itemToSelect == selectedItem) {
            return
        }
        require(allowedItems.contains(itemToSelect)) { "Not allowed item: $itemToSelect" }
        selectedItem = itemToSelect
        itemChangedListener?.accept(itemToSelect)
        postRefreshPopupMenu()
    }

    /**
     * Sets the listener for item selection changes.
     *
     * @param listener The listener to be notified when the selected item changes.
     */
    fun setOnItemChangedListener(listener: Consumer<T?>?) {
        itemChangedListener = listener
    }

    private fun postRefreshPopupMenu() {
        removeCallbacks(refreshPopupMenuRunnable)
        post(refreshPopupMenuRunnable)
    }

    private fun refreshPopupMenu() {
        val menu: Menu = popupMenu.menu
        menu.clear()

        // Add target qualities
        val groupId = Menu.NONE
        var isItemFound = false
        for (itemId in allowedItems.indices) {
            val item = allowedItems[itemId]
            menu.add(groupId, itemId, itemId, menuItemNameProvider?.apply(item) ?: item.toString())
            if (selectedItem == item) {
                isItemFound = true
                menu.findItem(itemId).isChecked = true
            }
        }
        if (!isItemFound) {
            selectedItem =
                if (allowedItems.isEmpty()) {
                    null
                } else {
                    allowedItems[0].also { menu.findItem(0).isChecked = true }
                }
        }

        // Make menu single checkable
        menu.setGroupCheckable(groupId, true, true)

        text = iconNameProvider?.apply(selectedItem) ?: selectedItem?.toString() ?: ""
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val item = allowedItems[menuItem.itemId]
            setSelectedItem(item)
            true
        }
    }
}
