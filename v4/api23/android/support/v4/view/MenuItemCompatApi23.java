/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.view;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.view.MenuItem;

class MenuItemCompatApi23 {
    public static MenuItem setIconTintList(MenuItem item, ColorStateList tint) {
        return item.setIconTintList(tint);
    }

    public static MenuItem setIconTintMode(MenuItem item, PorterDuff.Mode tintMode) {
        return item.setIconTintMode(tintMode);
    }
}
