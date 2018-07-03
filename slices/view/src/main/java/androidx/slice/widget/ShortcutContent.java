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

import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;


/**
 * Extracts information required to present content in shortcut format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(19)
public class ShortcutContent {

    private SliceItem mIcon;
    private SliceItem mLabel;
    private SliceItem mColorItem;
    private SliceItem mActionItem;
    private final boolean mHasTopLevelColorItem;

    public ShortcutContent(@NonNull ListContent content) {
        Slice slice = content.getSlice();
        mColorItem = content.getColorItem();
        mHasTopLevelColorItem = mColorItem != null;
        if (!mHasTopLevelColorItem) {
            mColorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        }

        // Preferred case: slice has a primary action
        SliceItem primaryAction = content.getPrimaryAction();
        if (primaryAction != null) {
            SliceActionImpl sliceAction = new SliceActionImpl(primaryAction);
            mActionItem = sliceAction.getActionItem();
            mIcon = SliceQuery.find(sliceAction.getSliceItem(), FORMAT_IMAGE, HINT_TITLE, null);
            mLabel = SliceQuery.find(sliceAction.getSliceItem(), FORMAT_TEXT, (String) null, null);
        }
        if (mActionItem == null) {
            // No hinted action; just use the first one
            mActionItem = SliceQuery.find(slice, FORMAT_ACTION, (String) null, null);
        }

        // First fallback: any hinted image and text
        if (mIcon == null || mIcon.getIcon() == null) {
            mIcon = SliceQuery.find(slice, FORMAT_IMAGE, HINT_TITLE, null);
        }
        if (mLabel == null) {
            mLabel = SliceQuery.find(slice, FORMAT_TEXT, HINT_TITLE, null);
        }

        // Second fallback: first image and text
        if (mIcon == null || mIcon.getIcon() == null) {
            mIcon = SliceQuery.find(slice, FORMAT_IMAGE, (String) null, null);
        }
        if (mLabel == null) {
            mLabel = SliceQuery.find(slice, FORMAT_TEXT, (String) null, null);
        }
    }

    public SliceItem getActionItem() {
        return mActionItem;
    }

    public SliceItem getLabel() {
        return mLabel;
    }

    public SliceItem getIcon() {
        return mIcon;
    }

    public SliceItem getColorItem() {
        return mColorItem;
    }

    /**
     * @return the slice that contains shortcut view contents.
     */
    public Slice buildSlice(Slice.Builder s) {
        Slice.Builder slice = new Slice.Builder(s);
        if (mActionItem != null) {
            slice.addItem(mActionItem);
        }
        if (mLabel != null) {
            slice.addItem(mLabel);
        }
        if (mIcon != null) {
            slice.addItem(mIcon);
        }
        // Only add color item if the parent slice doesn't contain a top level color item.
        if (!mHasTopLevelColorItem && mColorItem != null) {
            slice.addItem(mColorItem);
        }
        return s.addSubSlice(slice.build()).build();
    }
}
