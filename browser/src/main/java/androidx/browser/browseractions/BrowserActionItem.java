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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.PendingIntent;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A wrapper class holding custom item of Browser Actions menu.
 * The Bitmap is optional for a BrowserActionItem.
 *
 * @deprecated Browser Actions are deprecated as of release 1.2.0.
 */
@Deprecated
public class BrowserActionItem {
    private final String mTitle;
    private final PendingIntent mAction;
    @DrawableRes
    private int mIconId;
    private Uri mIconUri;
    private Runnable mRunnableAction;

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
     * Constructor for BrowserActionItem with icon access through a uri.
     * @param title The string shown for a custom item.
     * @param action The PendingIntent executed when a custom item is selected
     * @param iconUri The {@link Uri} used to access the icon file. Note: make sure this is
     * generated from {@link BrowserServiceFileProvider.generateUri(Context, Bitmap, String,
     * int, List<ResolveInfo>)}.
     */
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public BrowserActionItem(
            @NonNull String title, @NonNull PendingIntent action, @NonNull Uri iconUri) {
        mTitle = title;
        mAction = action;
        mIconUri = iconUri;
    }

    /**
     * Constructs a predefined fallback menu item with a Runnable action. The item will have no
     * icon and no custom PendingIntent action.
     * @param title The title of the menu item.
     * @param action The {@link Runnable} action to be executed when user choose the item.
     */
    BrowserActionItem(@NonNull String title, @NonNull Runnable action) {
        mTitle = title;
        mAction = null;
        mRunnableAction = action;
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
    @NonNull
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return The action of a custom item.
     */
    @NonNull
    public PendingIntent getAction() {
        return mAction;
    }

    /**
     * @return The uri used to get the icon of a custom item.
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public Uri getIconUri() {
        return mIconUri;
    }

    /**
     * @return The {@link Runnable} action of a predefined fallback menu item.
     */
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Nullable
    Runnable getRunnableAction() {
        return mRunnableAction;
    }
}
