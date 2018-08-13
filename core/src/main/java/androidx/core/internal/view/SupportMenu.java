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

package androidx.core.internal.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.view.KeyEvent;

import androidx.annotation.RestrictTo;

/**
 * Interface for managing the items in a menu.
 *
 * This version extends the one available in the framework to ensures that any necessary
 * elements added in later versions of the framework, are available for all platforms.
 *
 * @see android.view.Menu
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface SupportMenu extends android.view.Menu {

    /**
     * This is the part of an order integer that the user can provide.
     */
    int USER_MASK = 0x0000ffff;

    /**
     * Bit shift of the user portion of the order integer.
     */
    int USER_SHIFT = 0;

    /**
     * This is the part of an order integer that supplies the category of the item.
     */
    int CATEGORY_MASK = 0xffff0000;

    /**
     * Bit shift of the category portion of the order integer.
     */
    int CATEGORY_SHIFT = 16;

    /**
     * A mask of all supported modifiers for MenuItem's keyboard shortcuts
     */
    int SUPPORTED_MODIFIERS_MASK = KeyEvent.META_META_ON | KeyEvent.META_CTRL_ON
            | KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SYM_ON
            | KeyEvent.META_FUNCTION_ON;

    /**
     * Flag which stops the Menu being closed when a sub menu is opened
     */
    int FLAG_KEEP_OPEN_ON_SUBMENU_OPENED = 4;

    /**
     * Enable or disable the group dividers.
     *
     * @param enabled True if enabled.
     */
    @Override
    void setGroupDividerEnabled(boolean enabled);
}

