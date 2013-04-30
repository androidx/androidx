/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.view.MenuItem;

/**
 * Helper for accessing features in {@link android.view.Menu}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class MenuCompat {
    /**
     * Call {@link MenuItem#setShowAsAction(int) MenuItem.setShowAsAction()}.
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB} device,
     * does nothing and returns false.  Otherwise returns true.
     *
     * @deprecated Use {@link MenuItemCompat#setShowAsAction(MenuItem, int)
     *     MenuItemCompat.setShowAsAction(MenuItem, int)}
     */
    @Deprecated
    public static boolean setShowAsAction(MenuItem item, int actionEnum) {
        return MenuItemCompat.setShowAsAction(item, actionEnum);
    }
}
