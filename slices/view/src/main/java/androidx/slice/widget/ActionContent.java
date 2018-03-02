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

import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_PRIORITY;
import static android.app.slice.Slice.SUBTYPE_TOGGLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;

/**
 * Extracts information required to present an action button from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActionContent {
    private SliceItem mSliceItem;
    private SliceItem mIconItem;
    private SliceItem mActionItem;
    private SliceItem mTitleItem;
    private SliceItem mContentDescItem;
    private boolean mIsToggle;
    private boolean mIsChecked;
    private int mPriority;

    public ActionContent(@NonNull SliceItem slice) {
        populate(slice);
    }

    /**
     * @return whether this slice is a valid action.
     */
    private boolean populate(@NonNull SliceItem slice) {
        mSliceItem = slice;
        if (slice.hasHint(HINT_SHORTCUT) && FORMAT_SLICE.equals(slice.getFormat())) {
            mActionItem = SliceQuery.find(slice, FORMAT_ACTION);
            if (mActionItem == null) {
                // Can't have action slice without action
                return false;
            }
            mIconItem = SliceQuery.find(mActionItem.getSlice(), FORMAT_IMAGE);
            mTitleItem = SliceQuery.find(mActionItem.getSlice(), FORMAT_TEXT, HINT_TITLE,
                    null /* nonHints */);
            mContentDescItem = SliceQuery.findSubtype(mActionItem.getSlice(), FORMAT_TEXT,
                    SUBTYPE_CONTENT_DESCRIPTION);
            mIsToggle = SUBTYPE_TOGGLE.equals(mActionItem.getSubType());
            if (mIsToggle) {
                mIsChecked = mActionItem.hasHint(HINT_SELECTED);
            }
            SliceItem priority = SliceQuery.findSubtype(mActionItem.getSlice(), FORMAT_INT,
                    SUBTYPE_PRIORITY);
            mPriority = priority != null ? priority.getInt() : -1;
            return true;
        }
        return false;
    }

    /**
     * @return the SliceItem used to construct this ActionContent.
     */
    @NonNull
    public SliceItem getSliceItem() {
        return mSliceItem;
    }

    /**
     * @return the pending intent associated with this action.
     */
    @Nullable
    public SliceItem getActionItem() {
        return mActionItem;
    }

    /**
     * @return the icon associated with this action.
     */
    @Nullable
    public SliceItem getIconItem() {
        return mIconItem;
    }

    /**
     * @return the title associated with this action.
     */
    @Nullable
    public SliceItem getTitleItem() {
        return mTitleItem;
    }

    /**
     * @return the subtitle associated with this action.
     */
    @Nullable
    public SliceItem getContentDescriptionItem() {
        return mContentDescItem;
    }

    /**
     * @return the priority associated with this action, if it exists.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * @return Whether this action is toggleable.
     */
    public boolean isToggle() {
        return mIsToggle;
    }

    /**
     * @return Whether this action represents a custom toggle.
     */
    public boolean isCustomToggle() {
        return mIconItem != null && mIsToggle;
    }

    /**
     * @return Whether this action is 'checked' or not (i.e. if toggle is on).
     */
    public boolean isChecked() {
        return mIsChecked;
    }
}
