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

package androidx.slice.widget;

import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;
import static androidx.slice.widget.SliceViewUtil.resolveLayoutDirection;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceUtils;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

/**
 * Base class representing content that can be displayed.
 */
@RequiresApi(19)
public class SliceContent {

    /**
     * @hide
     */
    protected SliceItem mSliceItem;
    /**
     * @hide
     */
    protected SliceItem mColorItem;
    /**
     * @hide
     */
    protected SliceItem mLayoutDirItem;
    /**
     * @hide
     */
    protected SliceItem mContentDescr;
    /**
     * @hide
     */
    protected int mRowIndex;

    public SliceContent(@Nullable Slice slice) {
        if (slice == null) return;
        init(new SliceItem(slice, FORMAT_SLICE, null, slice.getHints()));
        // Built from a slice implies it's top level and index shouldn't matter
        mRowIndex = -1;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SliceContent(@Nullable SliceItem item, int rowIndex) {
        if (item == null) return;
        init(item);
        mRowIndex = rowIndex;
    }

    private void init(SliceItem item) {
        mSliceItem = item;
        if (FORMAT_SLICE.equals(item.getFormat()) || FORMAT_ACTION.equals(item.getFormat())) {
            mColorItem = SliceQuery.findTopLevelItem(item.getSlice(), FORMAT_INT, SUBTYPE_COLOR,
                    null, null);
            mLayoutDirItem = SliceQuery.findTopLevelItem(item.getSlice(), FORMAT_INT,
                    SUBTYPE_LAYOUT_DIRECTION, null, null);
        }
        mContentDescr = SliceQuery.findSubtype(item, FORMAT_TEXT, SUBTYPE_CONTENT_DESCRIPTION);
    }

    /**
     * @return the slice item used to construct this content.
     * @hide
     */
    @Nullable
    public SliceItem getSliceItem() {
        return mSliceItem;
    }

    /**
     * @return the accent color to use for this content or -1 if no color is set.
     * @hide
     */
    public int getAccentColor() {
        return mColorItem != null ? mColorItem.getInt() : -1;
    }

    /**
     * @return the layout direction to use for this content or -1 if no direction set.
     * @hide
     */
    public int getLayoutDir() {
        return mLayoutDirItem != null ? resolveLayoutDirection(mLayoutDirItem.getInt()) : -1;
    }

    /**
     * @return the content description to use for this row if set.
     * @hide
     */
    @Nullable
    public CharSequence getContentDescription() {
        return mContentDescr != null ? mContentDescr.getText() : null;
    }

    /**
     * @return the row index of this content, or -1 if no row index is set.
     * @hide
     */
    public int getRowIndex() { return mRowIndex; }

    /**
     * @return the desired height of this content based on the provided mode and context or the
     * default height if context is null.
     * @hide
     */
    public int getHeight(SliceStyle style, SliceViewPolicy policy) {
        return 0;
    }

    /**
     * @return whether this content is valid to display or not.
     * @hide
     */
    public boolean isValid() {
        return mSliceItem != null;
    }

    /**
     * @return the action that represents the shortcut.
     * @hide
     */
    @Nullable
    public SliceAction getShortcut(@Nullable Context context) {
        if (mSliceItem == null) {
            // Can't make something from nothing
            return null;
        }
        SliceItem iconItem = null;
        SliceItem labelItem = null;
        int imageMode = UNKNOWN_IMAGE;

        // Prefer something properly hinted
        String[] hints = new String[]{HINT_TITLE, HINT_SHORTCUT};
        SliceItem actionItem =  SliceQuery.find(mSliceItem, FORMAT_ACTION, hints, null);
        if (actionItem != null) {
            iconItem = SliceQuery.find(actionItem, FORMAT_IMAGE, HINT_TITLE, null);
            labelItem = SliceQuery.find(actionItem, FORMAT_TEXT, (String) null, null);
        }
        if (actionItem == null) {
            // No hinted action; just use the first one
            actionItem = SliceQuery.find(mSliceItem, FORMAT_ACTION, (String) null, null);
        }

        // First fallback: any hinted image and text
        if (iconItem == null) {
            iconItem = SliceQuery.find(mSliceItem, FORMAT_IMAGE, HINT_TITLE, null);
        }
        if (labelItem == null) {
            labelItem = SliceQuery.find(mSliceItem, FORMAT_TEXT, HINT_TITLE, null);
        }

        // Second fallback: first image and text
        if (iconItem == null) {
            iconItem = SliceQuery.find(mSliceItem, FORMAT_IMAGE, (String) null, null);
        }
        if (labelItem == null) {
            labelItem = SliceQuery.find(mSliceItem, FORMAT_TEXT, (String) null, null);
        }

        // Fill in anything we don't have with app data
        if (iconItem != null) {
            imageMode = SliceUtils.parseImageMode(iconItem);
        }
        if (context != null) {
            return fallBackToAppData(context, labelItem, iconItem, imageMode, actionItem);
        }
        if (iconItem != null && actionItem != null && labelItem != null) {
            return new SliceActionImpl(actionItem.getAction(), iconItem.getIcon(), imageMode,
                    labelItem.getText());
        }
        return null;
    }

    private SliceAction fallBackToAppData(Context context, SliceItem textItem, SliceItem iconItem,
            int iconMode, SliceItem actionItem) {
        SliceItem slice = SliceQuery.find(mSliceItem, FORMAT_SLICE, (String) null, null);
        if (slice == null) {
            // Can't make something out of nothing
            return null;
        }
        Uri uri = slice.getSlice().getUri();
        IconCompat shortcutIcon = iconItem != null ? iconItem.getIcon() : null;
        CharSequence shortcutAction = textItem != null ? textItem.getText() : null;
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            ProviderInfo providerInfo = pm.resolveContentProvider(uri.getAuthority(), 0);
            ApplicationInfo appInfo = providerInfo != null ? providerInfo.applicationInfo : null;
            if (appInfo != null) {
                if (shortcutIcon == null) {
                    Drawable icon = pm.getApplicationIcon(appInfo);
                    shortcutIcon = SliceViewUtil.createIconFromDrawable(icon);
                    iconMode = LARGE_IMAGE;
                }
                if (shortcutAction == null) {
                    shortcutAction = pm.getApplicationLabel(appInfo);
                }
                if (actionItem == null) {
                    Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                    if (launchIntent != null) {
                        actionItem = new SliceItem(
                                PendingIntent.getActivity(context, 0, launchIntent,
                                PendingIntent.FLAG_IMMUTABLE),
                                new Slice.Builder(uri).build(), FORMAT_ACTION,
                                null /* subtype */, new String[]{});
                    }
                }
            }
        }
        if (actionItem == null) {
            Intent intent = new Intent();
            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE);
            actionItem = new SliceItem(pi, null, FORMAT_ACTION, null, null);
        }
        if (shortcutAction != null && shortcutIcon != null && actionItem != null) {
            return new SliceActionImpl(actionItem.getAction(), shortcutIcon, iconMode,
                    shortcutAction);
        }
        return null;
    }
}
