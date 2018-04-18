/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view;

import android.annotation.SuppressLint;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.internal.view.SupportMenu;
import androidx.core.os.BuildCompat;

/**
 * Helper for accessing features in {@link android.view.Menu}.
 */
public final class MenuCompat {
    /**
     * Call {@link MenuItem#setShowAsAction(int) MenuItem.setShowAsAction()}.
     *
     * @deprecated Use {@link MenuItem#setShowAsAction(int)} directly.
     */
    @Deprecated
    public static void setShowAsAction(MenuItem item, int actionEnum) {
        item.setShowAsAction(actionEnum);
    }

    /**
     * Enable or disable the group dividers.
     *
     * @param menu Menu to enable/disable dividers on.
     * @param enabled True if enabled
     */
    @SuppressLint("NewApi")
    public static void setGroupDividerEnabled(Menu menu, boolean enabled) {
        if (menu instanceof SupportMenu) {
            ((SupportMenu) menu).setGroupDividerEnabled(enabled);
        } else if (BuildCompat.isAtLeastP()) {
            menu.setGroupDividerEnabled(enabled);
        }
    }

    private MenuCompat() {}
}
