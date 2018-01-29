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

package androidx.app.slice.widget;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListContent {

    private SliceItem mHeaderItem;
    private SliceItem mColorItem;
    private ArrayList<SliceItem> mRowItems = new ArrayList<>();
    private List<SliceItem> mSliceActions;

    public ListContent(Slice slice) {
        populate(slice);
    }

    /**
     * Resets the content.
     */
    public void reset() {
        mColorItem = null;
        mHeaderItem = null;
        mRowItems.clear();
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    public boolean populate(Slice slice) {
        reset();
        mColorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        // Find slice actions
        SliceItem actionGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_ACTIONS, null);
        if (actionGroup != null) {
            // TODO: actually use the actions
            mSliceActions = SliceQuery.findAll(actionGroup, FORMAT_ACTION, HINT_ACTIONS, null);
        }
        // Find header
        mHeaderItem = findHeaderItem(slice);
        if (mHeaderItem != null) {
            mRowItems.add(mHeaderItem);
        }
        // Filter + create row items
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            final SliceItem child = children.get(i);
            final String format = child.getFormat();
            if (!child.hasAnyHints(HINT_ACTIONS)
                    && (FORMAT_ACTION.equals(format) || FORMAT_SLICE.equals(format))) {
                if (mHeaderItem == null && !child.hasHint(HINT_LIST_ITEM)) {
                    mHeaderItem = child;
                    mRowItems.add(0, child);
                } else if (child.hasHint(HINT_LIST_ITEM)) {
                    mRowItems.add(child);
                }
            }
        }
        // Ensure we have something for the header -- use first row
        if (mHeaderItem == null && mRowItems.size() >= 1) {
            mHeaderItem = mRowItems.get(0);
        }
        return isValid();
    }

    /**
     * @return whether this list has content that is valid to display.
     */
    public boolean isValid() {
        return mRowItems.size() > 0;
    }

    @Nullable
    public SliceItem getColorItem() {
        return mColorItem;
    }

    @Nullable
    public SliceItem getHeaderItem() {
        return mHeaderItem;
    }

    @Nullable
    public List<SliceItem> getSliceActions() {
        return mSliceActions;
    }

    public ArrayList<SliceItem> getRowItems() {
        return mRowItems;
    }

    /**
     * @return whether this list has an explicit header (i.e. row item without HINT_LIST_ITEM)
     */
    public boolean hasHeader() {
        return mHeaderItem != null && isValidHeader(mHeaderItem);
    }

    @Nullable
    private static SliceItem findHeaderItem(@NonNull Slice slice) {
        // See if header is specified
        SliceItem header = SliceQuery.find(slice, FORMAT_SLICE, null, HINT_LIST_ITEM);
        if (header != null && isValidHeader(header)) {
            return header;
        }
        return null;
    }

    private static boolean isValidHeader(SliceItem sliceItem) {
        if (FORMAT_SLICE.equals(sliceItem.getFormat()) && !sliceItem.hasHint(HINT_LIST_ITEM)) {
             // Minimum valid header is a slice with text
            SliceItem item = SliceQuery.find(sliceItem, FORMAT_TEXT, (String) null, null);
            return item != null;
        }
        return false;
    }
}
