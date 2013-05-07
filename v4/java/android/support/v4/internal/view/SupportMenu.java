/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.internal.view;

/**
 * Interface for managing the items in a menu.
 *
 * <p>By default, every Activity supports an options menu of actions or options.
 * You can add items to this menu and handle clicks on your additions. Adding
 * menu items is typically accomplished by inflating an XML file into the
 * {@link SupportMenu} by using a {@link android.view.MenuInflater}.
 *
 * <p>Different menu types support different features:
 *
 * <ol>
 * <li><b>Context menus</b>: Do not support item shortcuts and item icons.
 * <li><b>Options menus</b>: The <b>icon menus</b> do not support item check
 * marks and only show the item's
 * {@link SupportMenuItem#setTitleCondensed(CharSequence) condensed title}. The
 * <b>expanded menus</b> (only available if six or more menu items are visible,
 * reached via the 'More' item in the icon menu) do not show item icons, and
 * item check marks are discouraged.
 * <li><b>Sub menus</b>: Do not support item icons, or nested sub menus.
 * </ol>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 *
 * <p>For more information about creating menus, read the
 * <a href="{@docRoot}guide/topics/ui/menus.html">Menus</a> developer guide.
 * </div>
 */
public interface SupportMenu extends android.view.Menu {

    /**
     * This is the part of an order integer that the user can provide.
     *
     * @hide
     */
    static final int USER_MASK = 0x0000ffff;
    /**
     * Bit shift of the user portion of the order integer.
     *
     * @hide
     */
    static final int USER_SHIFT = 0;

    /**
     * This is the part of an order integer that supplies the category of the item.
     *
     * @hide
     */
    static final int CATEGORY_MASK = 0xffff0000;
    /**
     * Bit shift of the category portion of the order integer.
     *
     * @hide
     */
    static final int CATEGORY_SHIFT = 16;
}

