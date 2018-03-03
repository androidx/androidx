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

package androidx.slice.widget;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceUtils;
import androidx.slice.core.SliceQuery;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListContent {

    private SliceItem mHeaderItem;
    private SliceItem mColorItem;
    private SliceItem mSeeMoreItem;
    private ArrayList<SliceItem> mRowItems = new ArrayList<>();
    private List<SliceItem> mSliceActions;
    private Context mContext;

    public ListContent(Context context, Slice slice) {
        mContext = context;
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
    private boolean populate(Slice slice) {
        reset();
        mColorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        // Find slice actions
        mSliceActions = SliceUtils.getSliceActions(slice);
        // Find header
        mHeaderItem = findHeaderItem(slice);
        if (mHeaderItem != null) {
            mRowItems.add(mHeaderItem);
        }
        mSeeMoreItem = getSeeMoreItem(slice);
        // Filter + create row items
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            final SliceItem child = children.get(i);
            final String format = child.getFormat();
            if (!child.hasAnyHints(HINT_ACTIONS, HINT_SEE_MORE)
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
     * @return the total height of all the rows contained in the provided list.
     */
    public static int getListHeight(Context context, List<SliceItem> listItems) {
        int height = 0;
        for (int i = 0; i < listItems.size(); i++) {
            height += getHeight(context, listItems.get(i), i == 0 /* isHeader */);
        }
        return height;
    }

    /**
     * Returns a list of items that can be displayed in the provided height. If this list
     * has a {@link #getSeeMoreItem()} this will be returned in the list if appropriate.
     *
     * @param height the height to restrict the items, -1 to use default sizings for non-scrolling
     *               templates.
     * @return the list of items that can be displayed in the provided  height.
     */
    @NonNull
    public List<SliceItem> getItemsForNonScrollingList(int height) {
        ArrayList<SliceItem> visibleItems = new ArrayList<>();
        if (mRowItems == null || mRowItems.size() == 0) {
            return visibleItems;
        }
        final int idealItemCount = hasHeader() ? 4 : 3;
        final int minItemCount = hasHeader() ? 2 : 1;
        int visibleHeight = 0;
        // Need to show see more
        if (mSeeMoreItem != null) {
            RowContent rc = new RowContent(mContext, mSeeMoreItem, false /* isHeader */);
            visibleHeight += rc.getActualHeight();
        }
        for (int i = 0; i < mRowItems.size(); i++) {
            int itemHeight = getHeight(mContext, mRowItems.get(i), i == 0 /* isHeader */);
            if ((height == -1 && i > idealItemCount)
                    || (height > 0 && visibleHeight + itemHeight > height)) {
                break;
            } else {
                visibleHeight += itemHeight;
                visibleItems.add(mRowItems.get(i));
            }
        }
        if (mSeeMoreItem != null && visibleItems.size() >= minItemCount) {
            // Only add see more if we're at least showing one item and it's not the header
            visibleItems.add(mSeeMoreItem);
        }
        if (visibleItems.size() == 0) {
            // Didn't have enough space to show anything; should still show something
            visibleItems.add(mRowItems.get(0));
        }
        return visibleItems;
    }

    private static int getHeight(Context context, SliceItem item, boolean isHeader) {
        if (item.hasHint(HINT_HORIZONTAL)) {
            GridContent gc = new GridContent(context, item);
            return gc.getActualHeight();
        } else {
            RowContent rc = new RowContent(context, item, isHeader);
            return rc.getActualHeight();
        }
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

    @Nullable
    public SliceItem getSeeMoreItem() {
        return mSeeMoreItem;
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
        String[] nonHints = new String[] {HINT_LIST_ITEM, HINT_SHORTCUT, HINT_ACTIONS};
        SliceItem header = SliceQuery.find(slice, FORMAT_SLICE, null, nonHints);
        if (header != null && isValidHeader(header)) {
            return header;
        }
        return null;
    }

    @Nullable
    private static SliceItem getSeeMoreItem(@NonNull Slice slice) {
        SliceItem item = SliceQuery.find(slice, null, HINT_SEE_MORE, null);
        if (item != null && item.hasHint(HINT_SEE_MORE)) {
            if (FORMAT_SLICE.equals(item.getFormat())) {
                List<SliceItem> items = item.getSlice().getItems();
                if (items.size() == 1 && FORMAT_ACTION.equals(items.get(0).getFormat())) {
                    return items.get(0);
                }
                return item;
            }
        }
        return null;
    }

    private static boolean isValidHeader(SliceItem sliceItem) {
        if (FORMAT_SLICE.equals(sliceItem.getFormat()) && !sliceItem.hasHint(HINT_LIST_ITEM)
                && !sliceItem.hasHint(HINT_ACTIONS)) {
             // Minimum valid header is a slice with text
            SliceItem item = SliceQuery.find(sliceItem, FORMAT_TEXT, (String) null, null);
            return item != null;
        }
        return false;
    }
}
