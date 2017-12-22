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

import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceHints;
import androidx.app.slice.core.SliceQuery;

/**
 * Extracts information required to present content in a row format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RowContent {
    private static final String TAG = "RowContent";

    private SliceItem mContentIntent;
    private SliceItem mStartItem;
    private SliceItem mTitleItem;
    private SliceItem mSubtitleItem;
    private SliceItem mToggleItem;
    private ArrayList<SliceItem> mEndItems = new ArrayList<>();

    public RowContent(SliceItem rowSlice, boolean showStartItem) {
        populate(rowSlice, showStartItem);
    }

    /**
     * Resets the content.
     */
    public void reset() {
        mContentIntent = null;
        mStartItem = null;
        mTitleItem = null;
        mSubtitleItem = null;
        mToggleItem = null;
        mEndItems.clear();
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    public boolean populate(SliceItem rowSlice, boolean showStartItem) {
        reset();
        if (!isValidRow(rowSlice)) {
            Log.w(TAG, "Provided SliceItem is invalid for RowContent");
            return false;
        }
        // Filter anything not viable for displaying in a row
        ArrayList<SliceItem> rowItems = filterInvalidItems(rowSlice.getSlice().getItems());
        // If we've only got one item that's a slice / action use those items instead
        if (rowItems.size() == 1 && (FORMAT_ACTION.equals(rowItems.get(0).getFormat())
                || FORMAT_SLICE.equals(rowItems.get(0).getFormat()))) {
            if (isValidRow(rowItems.get(0))) {
                rowSlice = rowItems.get(0);
                rowItems = filterInvalidItems(rowSlice.getSlice().getItems());
            }
        }
        // Content intent
        if (FORMAT_ACTION.equals(rowSlice.getFormat())) {
            mContentIntent = rowSlice;
        }
        if (rowItems.size() > 0) {
            // Start item
            if (showStartItem && isStartType(rowItems.get(0))) {
                mStartItem = rowItems.get(0);
                rowItems.remove(mStartItem);
            }
            // Text + end items
            for (int i = 0; i < rowItems.size(); i++) {
                final SliceItem item = rowItems.get(i);
                if (FORMAT_TEXT.equals(item.getFormat())) {
                    if ((mTitleItem == null || !mTitleItem.hasHint(HINT_TITLE))
                            && item.hasHint(HINT_TITLE)) {
                        mTitleItem = item;
                    } else if (mSubtitleItem == null) {
                        mSubtitleItem = item;
                    }
                } else {
                    mEndItems.add(item);
                }
            }
        }
        checkForToggle();
        return isValid();
    }

    private void checkForToggle() {
        // Check if we have a content intent that is for a toggle
        if (mContentIntent != null && SliceQuery.hasHints(mContentIntent.getSlice(),
                SliceHints.SUBTYPE_TOGGLE)) {
            mToggleItem = mContentIntent;
            return;
        }
        // Check if there's a toggle in our end items
        ArrayList<SliceItem> endItems = getEndItems();
        for (int i = 0; i < endItems.size(); i++) {
            final SliceItem endItem = endItems.get(i);
            if (FORMAT_ACTION.equals(endItem.getFormat())
                    && (endItem.hasHint(SliceHints.SUBTYPE_TOGGLE)
                    || SliceQuery.hasHints(endItem.getSlice(), SliceHints.SUBTYPE_TOGGLE))) {
                mToggleItem = endItem;
                return;
            }
        }
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    public boolean isValid() {
        return mStartItem != null
                || mTitleItem != null
                || mSubtitleItem != null
                || mEndItems.size() > 0;
    }

    @Nullable
    public SliceItem getContentIntent() {
        return mContentIntent;
    }

    @Nullable
    public SliceItem getStartItem() {
        return mStartItem;
    }

    @Nullable
    public SliceItem getTitleItem() {
        return mTitleItem;
    }

    @Nullable
    public SliceItem getSubtitleItem() {
        return mSubtitleItem;
    }

    @Nullable
    public SliceItem getToggleItem() {
        return mToggleItem;
    }

    public ArrayList<SliceItem> getEndItems() {
        return mEndItems;
    }

    /**
     * @return whether this is a valid item to use to populate a row of content.
     */
    private static boolean isValidRow(SliceItem item) {
        // Must be slice or action
        if (FORMAT_SLICE.equals(item.getFormat()) || FORMAT_ACTION.equals(item.getFormat())) {
            // Must have at least one legitimate child
            List<SliceItem> rowItems = item.getSlice().getItems();
            for (int i = 0; i < rowItems.size(); i++) {
                if (isValidRowContent(rowItems.get(i))) {
                    return true;
                }
            }
        }
        Log.w(TAG, "invalid row content because not a slice or action");
        return false;
    }

    private static ArrayList<SliceItem> filterInvalidItems(List<SliceItem> items) {
        ArrayList<SliceItem> filteredList = new ArrayList<>();
        for (SliceItem i : items) {
            if (isValidRowContent(i)) {
                filteredList.add(i);
            }
        }
        return filteredList;
    }

    /**
     * @return whether this item has valid content to display in a row.
     */
    private static boolean isValidRowContent(SliceItem item) {
        // TODO -- filter for shortcut once that's in
        final String itemFormat = item.getFormat();
        // Must be a format that is presentable
        return FORMAT_TEXT.equals(itemFormat)
                || FORMAT_IMAGE.equals(itemFormat)
                || FORMAT_TIMESTAMP.equals(itemFormat)
                || FORMAT_REMOTE_INPUT.equals(itemFormat)
                || FORMAT_ACTION.equals(itemFormat);
    }

    /**
     * @return Whether this item is appropriate to be considered a "start" item, i.e. go in the
     *         front slot of a row.
     */
    private static boolean isStartType(SliceItem item) {
        final String type = item.getFormat();
        return (!item.hasHint(SliceHints.SUBTYPE_TOGGLE)
                && item.hasHint(HINT_TITLE)
                && (FORMAT_ACTION.equals(type) && (SliceQuery.find(item, FORMAT_IMAGE) != null)))
                || FORMAT_IMAGE.equals(type)
                || FORMAT_TIMESTAMP.equals(type);
    }
}
