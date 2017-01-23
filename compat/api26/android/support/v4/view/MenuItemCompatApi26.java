/**
 * Copyright (C) 2017 The Android Open Source Project
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

import android.support.annotation.RequiresApi;
import android.view.MenuItem;

@RequiresApi(26)
class MenuItemCompatApi26 {
    public static void setContentDescription(MenuItem item, CharSequence contentDescription) {
        item.setContentDescription(contentDescription);
    }

    public static CharSequence getContentDescription(MenuItem item) {
        return item.getContentDescription();
    }

    public static void setTooltipText(MenuItem item, CharSequence tooltipText) {
        item.setTooltipText(tooltipText);
    }

    public static CharSequence getTooltipText(MenuItem item) {
        return item.getTooltipText();
    }
}
