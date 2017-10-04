/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.view.menu.MenuBuilder;
import android.view.MenuItem;

/**
 * An interface notified when a menu item is hovered. Useful for cases when hover should trigger
 * some behavior at a higher level, like managing the opening and closing of submenus.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface MenuItemHoverListener {
    /**
     * Called when hover exits a menu item.
     * <p>
     * If hover is moving to another item, this method will be called before
     * {@link #onItemHoverEnter(MenuBuilder, MenuItem)} for the newly-hovered item.
     *
     * @param menu the item's parent menu
     * @param item the hovered menu item
     */
    void onItemHoverExit(@NonNull MenuBuilder menu, @NonNull MenuItem item);

    /**
     * Called when hover enters a menu item.
     *
     * @param menu the item's parent menu
     * @param item the hovered menu item
     */
    void onItemHoverEnter(@NonNull MenuBuilder menu, @NonNull MenuItem item);
}