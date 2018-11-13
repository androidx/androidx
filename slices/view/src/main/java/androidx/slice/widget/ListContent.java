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
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(19)
public class ListContent extends SliceContent {

    private SliceAction mPrimaryAction;
    private RowContent mHeaderContent;
    private RowContent mSeeMoreContent;
    private ArrayList<SliceContent> mRowItems = new ArrayList<>();
    private List<SliceAction> mSliceActions;
    private int mMinScrollHeight;
    private int mLargeHeight;
    private Context mContext;

    public ListContent(Context context, @NonNull Slice slice) {
        super(slice);
        if (mSliceItem == null) {
            return;
        }
        mContext = context;
        if (context != null) {
            mMinScrollHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
            mLargeHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.abc_slice_large_height);
        }
        populate(slice);
    }

    private void populate(Slice slice) {
        if (slice == null) return;
        mSliceActions = SliceMetadata.getSliceActions(slice);
        final SliceItem headerItem = findHeaderItem(slice);
        if (headerItem != null) {
            mHeaderContent = new RowContent(headerItem, 0);
            mRowItems.add(mHeaderContent);
        }
        final SliceItem seeMoreItem = getSeeMoreItem(slice);
        if (seeMoreItem != null) {
            mSeeMoreContent = new RowContent(seeMoreItem, -1);
        }

        // Filter + create row items
        List<SliceItem> children = slice.getItems();
        for (int i = 0; i < children.size(); i++) {
            final SliceItem child = children.get(i);
            final String format = child.getFormat();
            boolean isNonRowContent = child.hasAnyHints(HINT_ACTIONS, HINT_SEE_MORE, HINT_KEYWORDS,
                    HINT_TTL, HINT_LAST_UPDATED);
            if (!isNonRowContent && (FORMAT_ACTION.equals(format) || FORMAT_SLICE.equals(format))) {
                if (mHeaderContent == null && !child.hasHint(HINT_LIST_ITEM)) {
                    mHeaderContent = new RowContent(child, 0);
                    mRowItems.add(0, mHeaderContent);
                } else if (child.hasHint(HINT_LIST_ITEM)) {
                    if (child.hasHint(HINT_HORIZONTAL)) {
                        mRowItems.add(new GridContent(child, i));
                    } else {
                        mRowItems.add(new RowContent(child, i));
                    }
                }
            }
        }
        // Ensure we have something for the header -- use first row
        if (mHeaderContent == null && mRowItems.size() >= 1) {
            // We enforce RowContent has first item on builder side; if that changes this
            // could be an issue
            mHeaderContent = (RowContent) mRowItems.get(0);
            mHeaderContent.setIsHeader(true);
        }
        if (mRowItems.size() > 0 && mRowItems.get(mRowItems.size() - 1) instanceof GridContent) {
            // Grid item is the last item, note that.
            ((GridContent) mRowItems.get(mRowItems.size() - 1)).setIsLastIndex(true);
        }
        mPrimaryAction = findPrimaryAction();
    }

    @Override
    public int getHeight(SliceStyle style, SliceViewPolicy policy) {
        if (policy.getMode() == MODE_SMALL) {
            return mHeaderContent.getHeight(style, policy);
        }
        int maxHeight = policy.getMaxHeight();
        boolean scrollable = policy.isScrollable();

        int desiredHeight = getListHeight(mRowItems, style, policy);
        if (maxHeight > 0) {
            // Always ensure we're at least the height of our small version.
            int smallHeight = mHeaderContent.getHeight(style, policy);
            maxHeight = Math.max(smallHeight, maxHeight);
        }
        int maxLargeHeight = maxHeight > 0
                ? maxHeight
                : mLargeHeight;
        // Do we have enough content to reasonably scroll in our max?
        boolean bigEnoughToScroll = desiredHeight - maxLargeHeight >= mMinScrollHeight;

        // Adjust for scrolling
        int height = bigEnoughToScroll ? maxLargeHeight
                : maxHeight <= 0 ? desiredHeight
                : Math.min(maxLargeHeight, desiredHeight);
        if (!scrollable) {
            height = getListHeight(getItemsForNonScrollingList(height, style, policy),
                    style, policy);
        }
        return height;
    }

    /**
     * Gets the row items to display in this list.
     *
     * @param availableHeight the available height for displaying the list.
     * @param style the style info to use when determining row items to return.
     * @param policy the policy info (scrolling, mode) to use when determining row items to return.
     *
     * @return the row items that should be shown based on the provided configuration.
     */
    public ArrayList<SliceContent> getRowItems(int availableHeight, SliceStyle style,
            SliceViewPolicy policy) {
        if (policy.getMode() == MODE_SMALL) {
            return new ArrayList(Arrays.asList(getHeader()));
        } else if (!policy.isScrollable() && availableHeight > 0) {
            return getItemsForNonScrollingList(availableHeight, style, policy);
        }
        return getRowItems();
    }

    /**
     * Returns a list of items that can fit in the provided height. If this list
     * has a see more item this will be displayed in the list if appropriate.
     *
     * @param availableHeight to use to determine the row items to return.
     * @param style the style info to use when determining row items to return.
     * @param policy the policy info (scrolling, mode) to use when determining row items to return.
     *
     * @return the list of items that can be displayed in the provided height.
     */
    @NonNull
    private ArrayList<SliceContent> getItemsForNonScrollingList(int availableHeight,
            SliceStyle style, SliceViewPolicy policy) {
        ArrayList<SliceContent> visibleItems = new ArrayList<>();
        if (mRowItems == null || mRowItems.size() == 0) {
            return visibleItems;
        }
        final int minItemCountForSeeMore = mHeaderContent != null ? 2 : 1;
        int visibleHeight = 0;
        // Need to show see more
        if (mSeeMoreContent != null) {
            visibleHeight += mSeeMoreContent.getHeight(style, policy);
        }
        int rowCount = mRowItems.size();
        for (int i = 0; i < rowCount; i++) {
            int itemHeight = mRowItems.get(i).getHeight(style, policy);
            if (availableHeight > 0 && visibleHeight + itemHeight > availableHeight) {
                break;
            } else {
                visibleHeight += itemHeight;
                visibleItems.add(mRowItems.get(i));
            }
        }
        if (mSeeMoreContent != null && visibleItems.size() >= minItemCountForSeeMore
                && visibleItems.size() != rowCount) {
            // Only add see more if we're at least showing one item and it's not the header
            visibleItems.add(mSeeMoreContent);
        }
        if (visibleItems.size() == 0) {
            // Didn't have enough space to show anything; should still show something
            visibleItems.add(mRowItems.get(0));
        }
        return visibleItems;
    }

    /**
     * @return whether this list has content that is valid to display.
     */
    @Override
    public boolean isValid() {
        return super.isValid() && mRowItems.size() > 0;
    }

    @Nullable
    public RowContent getHeader() {
        return mHeaderContent;
    }

    @Nullable
    public List<SliceAction> getSliceActions() {
        return mSliceActions;
    }

    @NonNull
    public ArrayList<SliceContent> getRowItems() {
        return mRowItems;
    }

    /**
     * @return the type of template that the header represents.
     */
    public int getHeaderTemplateType() {
        return getRowType(mHeaderContent, true, mSliceActions);
    }

    @Override
    @Nullable
    public SliceAction getShortcut(@Nullable Context context) {
        return mPrimaryAction != null ? mPrimaryAction : super.getShortcut(context);
    }

    /**
     * @return suitable action to use for a tap on the slice template or for the shortcut.
     */
    @Nullable
    private SliceAction findPrimaryAction() {
        SliceItem action = null;
        if (mHeaderContent != null) {
            action = mHeaderContent.getPrimaryAction();
        }
        if (action == null) {
            String[] hints = new String[]{HINT_SHORTCUT, HINT_TITLE};
            action = SliceQuery.find(mSliceItem, FORMAT_ACTION, hints, null);
        }
        if (action == null) {
            action = SliceQuery.find(mSliceItem, FORMAT_ACTION, (String) null, null);
        }
        return action != null ? new SliceActionImpl(action) : null;
    }

    /**
     * The type of template that the provided row item represents.
     *
     * @param content the content to determine the template type of.
     * @param isHeader whether this row item is used as a header.
     * @param actions the actions associated with this slice, only matter if this row is the header.
     * @return the type of template the provided row item represents.
     */
    public static int getRowType(SliceContent content, boolean isHeader,
                                 List<SliceAction> actions) {
        if (content != null) {
            if (content instanceof GridContent) {
                return EventInfo.ROW_TYPE_GRID;
            } else {
                RowContent rc = (RowContent) content;
                SliceItem actionItem = rc.getPrimaryAction();
                SliceAction primaryAction = null;
                if (actionItem != null) {
                    primaryAction = new SliceActionImpl(actionItem);
                }
                if (rc.getRange() != null) {
                    return FORMAT_ACTION.equals(rc.getRange().getFormat())
                            ? EventInfo.ROW_TYPE_SLIDER
                            : EventInfo.ROW_TYPE_PROGRESS;
                } else if (primaryAction != null && primaryAction.isToggle()) {
                    return EventInfo.ROW_TYPE_TOGGLE;
                } else if (isHeader && actions != null) {
                    for (int i = 0; i < actions.size(); i++) {
                        if (actions.get(i).isToggle()) {
                            return EventInfo.ROW_TYPE_TOGGLE;
                        }
                    }
                    return EventInfo.ROW_TYPE_LIST;
                } else {
                    return rc.getToggleItems().size() > 0
                            ? EventInfo.ROW_TYPE_TOGGLE
                            : EventInfo.ROW_TYPE_LIST;
                }
            }
        }
        return EventInfo.ROW_TYPE_LIST;
    }

    /**
     * @return the total height of all the rows contained in the provided list.
     */
    public static int getListHeight(List<SliceContent> listItems, SliceStyle style,
            SliceViewPolicy policy) {
        if (listItems == null) {
            return 0;
        }
        int height = 0;
        SliceContent maybeHeader = null;
        if (!listItems.isEmpty()) {
            maybeHeader = listItems.get(0);
        }
        if (listItems.size() == 1 && !maybeHeader.getSliceItem().hasHint(HINT_HORIZONTAL)) {
            return maybeHeader.getHeight(style, policy);
        }
        for (int i = 0; i < listItems.size(); i++) {
            height += listItems.get(i).getHeight(style, policy);
        }
        return height;
    }

    @Nullable
    private static SliceItem findHeaderItem(@NonNull Slice slice) {
        // See if header is specified
        String[] nonHints = new String[] {HINT_LIST_ITEM, HINT_SHORTCUT, HINT_ACTIONS,
                HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED, HINT_HORIZONTAL};
        SliceItem header = SliceQuery.find(slice, FORMAT_SLICE, null, nonHints);
        if (header != null && isValidHeader(header)) {
            return header;
        }
        return null;
    }

    @Nullable
    private static SliceItem getSeeMoreItem(@NonNull Slice slice) {
        SliceItem item = SliceQuery.findTopLevelItem(slice, null, null,
                new String[] {HINT_SEE_MORE}, null);
        if (item != null) {
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

    /**
     * @return whether the provided slice item is a valid header.
     */
    private static boolean isValidHeader(SliceItem sliceItem) {
        if (FORMAT_SLICE.equals(sliceItem.getFormat())
                && !sliceItem.hasAnyHints(HINT_ACTIONS, HINT_KEYWORDS, HINT_SEE_MORE)) {
             // Minimum valid header is a slice with text
            SliceItem item = SliceQuery.find(sliceItem, FORMAT_TEXT, (String) null, null);
            return item != null;
        }
        return false;
    }
}
