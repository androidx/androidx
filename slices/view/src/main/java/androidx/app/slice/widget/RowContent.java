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
import static android.app.slice.Slice.SUBTYPE_SLIDER;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
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
    private ArrayList<SliceItem> mEndItems = new ArrayList<>();
    private boolean mEndItemsContainAction;
    private SliceItem mSlider;

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
        ArrayList<SliceItem> rowItems = filterInvalidItems(rowSlice);
        // If we've only got one item that's a slice / action use those items instead
        if (rowItems.size() == 1 && (FORMAT_ACTION.equals(rowItems.get(0).getFormat())
                || FORMAT_SLICE.equals(rowItems.get(0).getFormat()))) {
            if (isValidRow(rowItems.get(0))) {
                rowSlice = rowItems.get(0);
                rowItems = filterInvalidItems(rowSlice);
            }
        }
        // Content intent
        if (FORMAT_ACTION.equals(rowSlice.getFormat())) {
            mContentIntent = rowSlice;
        }
        if (SUBTYPE_SLIDER.equals(rowSlice.getSubType())) {
            mSlider = rowSlice;
        }
        if (rowItems.size() > 0) {
            // Start item
            if (isStartType(rowItems.get(0))) {
                if (showStartItem) {
                    mStartItem = rowItems.get(0);
                }
                rowItems.remove(0);
            }
            // Text + end items
            ArrayList<SliceItem> endItems = new ArrayList<>();
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
                    endItems.add(item);
                }
            }
            // Special rules for end items: only one timestamp, can't be mixture of icons / actions
            boolean hasTimestamp = mStartItem != null
                    && FORMAT_TIMESTAMP.equals(mStartItem.getFormat());
            String desiredFormat = null;
            for (int i = 0; i < endItems.size(); i++) {
                final SliceItem item = endItems.get(i);
                if (FORMAT_TIMESTAMP.equals(item.getFormat())) {
                    if (!hasTimestamp) {
                        hasTimestamp = true;
                        mEndItems.add(item);
                    }
                } else if (desiredFormat == null) {
                    desiredFormat = item.getFormat();
                    mEndItems.add(item);
                } else if (desiredFormat.equals(item.getFormat())) {
                    mEndItems.add(item);
                    mEndItemsContainAction |= FORMAT_ACTION.equals(item.getFormat());
                }
            }
        }
        return isValid();
    }

    /**
     * @return the {@link SliceItem} representing the slider in this row; can be null
     */
    @Nullable
    public SliceItem getSlider() {
        return mSlider;
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

    public ArrayList<SliceItem> getEndItems() {
        return mEndItems;
    }

    /**
     * @return whether {@link #getEndItems()} contains a SliceItem with FORMAT_ACTION
     */
    public boolean endItemsContainAction() {
        return mEndItemsContainAction;
    }

    /**
     * @return whether this is a valid item to use to populate a row of content.
     */
    private static boolean isValidRow(SliceItem rowSlice) {
        // Must be slice or action
        if (FORMAT_SLICE.equals(rowSlice.getFormat())
                || FORMAT_ACTION.equals(rowSlice.getFormat())) {
            // Must have at least one legitimate child
            List<SliceItem> rowItems = rowSlice.getSlice().getItems();
            for (int i = 0; i < rowItems.size(); i++) {
                if (isValidRowContent(rowSlice, rowItems.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<SliceItem> filterInvalidItems(SliceItem rowSlice) {
        ArrayList<SliceItem> filteredList = new ArrayList<>();
        for (SliceItem i : rowSlice.getSlice().getItems()) {
            if (isValidRowContent(rowSlice, i)) {
                filteredList.add(i);
            }
        }
        return filteredList;
    }

    /**
     * @return whether this item has valid content to display in a row.
     */
    private static boolean isValidRowContent(SliceItem slice, SliceItem item) {
        // TODO -- filter for shortcut once that's in
        final String itemFormat = item.getFormat();
        // Must be a format that is presentable
        return FORMAT_TEXT.equals(itemFormat)
                || FORMAT_IMAGE.equals(itemFormat)
                || FORMAT_TIMESTAMP.equals(itemFormat)
                || FORMAT_REMOTE_INPUT.equals(itemFormat)
                || FORMAT_ACTION.equals(itemFormat)
                || (FORMAT_INT.equals(itemFormat) && SUBTYPE_SLIDER.equals(slice.getSubType()));
    }

    /**
     * @return Whether this item is appropriate to be considered a "start" item, i.e. go in the
     *         front slot of a row.
     */
    private static boolean isStartType(SliceItem item) {
        final String type = item.getFormat();
        return (!item.hasHint(SliceHints.SUBTYPE_TOGGLE)
                && (FORMAT_ACTION.equals(type) && (SliceQuery.find(item, FORMAT_IMAGE) != null)))
                || FORMAT_IMAGE.equals(type)
                || FORMAT_TIMESTAMP.equals(type);
    }
}
