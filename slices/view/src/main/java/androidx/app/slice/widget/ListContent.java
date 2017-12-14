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
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceHints;
import androidx.app.slice.core.SliceQuery;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListContent {

    private SliceItem mColorItem;
    private SliceItem mSummaryItem;
    private ArrayList<SliceItem> mRowItems = new ArrayList<>();
    private boolean mHasHeader;

    public ListContent(Slice slice) {
        populate(slice);
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    public boolean populate(Slice slice) {
        mColorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        // Find summary
        SliceItem summaryItem = getSummaryItem(slice);
        mSummaryItem = summaryItem;
        // Filter + create row items
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            final SliceItem child = children.get(i);
            final String format = child.getFormat();
            if (!child.hasAnyHints(SliceHints.HINT_SUMMARY, HINT_ACTIONS)
                    && (FORMAT_ACTION.equals(format) || FORMAT_SLICE.equals(format))) {
                if (!mHasHeader && !child.hasHint(HINT_LIST_ITEM)) {
                    mHasHeader = true;
                    mRowItems.add(0, child);
                } else {
                    mRowItems.add(child);
                }
            }
        }
        return isValid();
    }

    /**
     * @return whether this list has content that is valid to display.
     */
    public boolean isValid() {
        return mSummaryItem != null
                || mRowItems.size() > 0;
    }

    @Nullable
    public SliceItem getColorItem() {
        return mColorItem;
    }

    @Nullable
    public SliceItem getSummaryItem() {
        return mSummaryItem;
    }

    public ArrayList<SliceItem> getRowItems() {
        return mRowItems;
    }

    /**
     * @return whether this list has a header or not.
     */
    public boolean hasHeader() {
        return mHasHeader;
    }

    /**
     * @return A slice item of format slice that is hinted to be shown when the slice is in small
     * format, or is the best option if nothing is appropriately hinted.
     */
    private static SliceItem getSummaryItem(@NonNull Slice slice) {
        List<SliceItem> items = slice.getItems();
        // See if a summary is specified
        SliceItem summary = SliceQuery.find(slice, FORMAT_SLICE, SliceHints.HINT_SUMMARY, null);
        if (summary != null) {
            return summary;
        }
        // Otherwise use the first non-color item and use it if it's a slice
        SliceItem firstSlice = null;
        for (int i = 0; i < items.size(); i++) {
            if (!FORMAT_INT.equals(items.get(i).getFormat())) {
                firstSlice = items.get(i);
                break;
            }
        }
        if (firstSlice != null && FORMAT_SLICE.equals(firstSlice.getFormat())) {
            // Check if this slice is appropriate to use to populate small template
            if (firstSlice.hasHint(HINT_LIST)) {
                // Check for header, use that if it exists
                SliceItem listHeader = SliceQuery.find(firstSlice, FORMAT_SLICE,
                        null,
                        new String[] {
                                HINT_LIST_ITEM, HINT_LIST
                        });
                if (listHeader != null) {
                    return findFirstSlice(listHeader);
                } else {
                    // Otherwise use the first list item
                    SliceItem newFirst = firstSlice.getSlice().getItems().get(0);
                    return findFirstSlice(newFirst);
                }
            } else {
                // Not a list, find first slice with non-slice children
                return findFirstSlice(firstSlice);
            }
        }
        // Fallback, just use this and convert to SliceItem type slice
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        Slice s = sb.addSubSlice(slice).build();
        return s.getItems().get(0);
    }

    /**
     * @return Finds the first slice that has non-slice children.
     */
    private static SliceItem findFirstSlice(SliceItem slice) {
        if (!FORMAT_SLICE.equals(slice.getFormat())) {
            return slice;
        }
        List<SliceItem> items = slice.getSlice().getItems();
        for (int i = 0; i < items.size(); i++) {
            if (FORMAT_SLICE.equals(items.get(i).getFormat())) {
                SliceItem childSlice = items.get(i);
                return findFirstSlice(childSlice);
            } else {
                // Doesn't have slice children so return it
                return slice;
            }
        }
        // Slices all the way down, just return it
        return slice;
    }
}
