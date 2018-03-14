/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.browser.browseractions;

import android.app.PendingIntent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * A wrapper class holding custom item of Browser Actions menu.
 * The Bitmap is optional for a BrowserActionItem.
 */
public class BrowserActionItem {
    private final String mTitle;
    private final PendingIntent mAction;
    @DrawableRes
    private final int mIconId;

    /**
     * Constructor for BrowserActionItem with icon, string and action provided.
     * @param title The string shown for a custom item.
     * @param action The PendingIntent executed when a custom item is selected
     * @param iconId The resource id of the icon shown for a custom item.
     */
    public BrowserActionItem(
            @NonNull String title, @NonNull PendingIntent action, @DrawableRes int iconId) {
        mTitle = title;
        mAction = action;
        mIconId = iconId;
    }

    /**
     * Constructor for BrowserActionItem with only string and action provided.
     * @param title The icon shown for a custom item.
     * @param action The string shown for a custom item.
     */
    public BrowserActionItem(@NonNull String title, @NonNull PendingIntent action) {
        this(title, action, 0);
    }

    /**
     * @return The resource id of the icon.
     */
    public int getIconId() {
        return mIconId;
    }

    /**
     * @return The title of a custom item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return The action of a custom item.
     */
    public PendingIntent getAction() {
        return mAction;
    }
}
