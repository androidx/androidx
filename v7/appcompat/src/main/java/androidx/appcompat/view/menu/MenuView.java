/*
 * Copyright (C) 2006 The Android Open Source Project
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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.drawable.Drawable;

import androidx.annotation.RestrictTo;

/**
 * Minimal interface for a menu view.  {@link #initialize(MenuBuilder)} must be called for the
 * menu to be functional.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface MenuView {
    /**
     * Initializes the menu to the given menu. This should be called after the
     * view is inflated.
     *
     * @param menu The menu that this MenuView should display.
     */
    void initialize(MenuBuilder menu);

    /**
     * Returns the default animations to be used for this menu when entering/exiting.
     * @return A resource ID for the default animations to be used for this menu.
     */
    int getWindowAnimations();

    /**
     * Minimal interface for a menu item view.  {@link #initialize(MenuItemImpl, int)} must be called
     * for the item to be functional.
     */
    interface ItemView {
        /**
         * Initializes with the provided MenuItemData.  This should be called after the view is
         * inflated.
         * @param itemData The item that this ItemView should display.
         * @param menuType The type of this menu, one of
         *            {@link MenuBuilder#TYPE_ICON}, {@link MenuBuilder#TYPE_EXPANDED},
         *            {@link MenuBuilder#TYPE_DIALOG}).
         */
        void initialize(MenuItemImpl itemData, int menuType);

        /**
         * Gets the item data that this view is displaying.
         * @return the item data, or null if there is not one
         */
        MenuItemImpl getItemData();

        /**
         * Sets the title of the item view.
         * @param title The title to set.
         */
        void setTitle(CharSequence title);

        /**
         * Sets the enabled state of the item view.
         * @param enabled Whether the item view should be enabled.
         */
        void setEnabled(boolean enabled);

        /**
         * Displays the checkbox for the item view.  This does not ensure the item view will be
         * checked, for that use {@link #setChecked}.
         * @param checkable Whether to display the checkbox or to hide it
         */
        void setCheckable(boolean checkable);

        /**
         * Checks the checkbox for the item view.  If the checkbox is hidden, it will NOT be
         * made visible, call {@link #setCheckable(boolean)} for that.
         * @param checked Whether the checkbox should be checked
         */
        void setChecked(boolean checked);

        /**
         * Sets the shortcut for the item.
         * @param showShortcut Whether a shortcut should be shown(if false, the value of
         * shortcutKey should be ignored).
         * @param shortcutKey The shortcut key that should be shown on the ItemView.
         */
        void setShortcut(boolean showShortcut, char shortcutKey);

        /**
         * Set the icon of this item view.
         * @param icon The icon of this item. null to hide the icon.
         */
        void setIcon(Drawable icon);

        /**
         * Whether this item view prefers displaying the condensed title rather
         * than the normal title. If a condensed title is not available, the
         * normal title will be used.
         *
         * @return Whether this item view prefers displaying the condensed
         *         title.
         */
        boolean prefersCondensedTitle();

        /**
         * Whether this item view shows an icon.
         *
         * @return Whether this item view shows an icon.
         */
        boolean showsIcon();
    }
}
