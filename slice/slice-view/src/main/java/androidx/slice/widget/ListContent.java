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

import static androidx.slice.core.SliceHints.HINT_SELECTION_OPTION;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts information required to present content in a list format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class ListContent extends SliceContent {

    private SliceAction mPrimaryAction;
    private RowContent mHeaderContent;
    private RowContent mSeeMoreContent;
    private ArrayList<SliceContent> mRowItems = new ArrayList<>();
    private List<SliceAction> mSliceActions;

    public ListContent(@NonNull Slice slice) {
        super(slice);
        if (mSliceItem == null) {
            return;
        }
        populate(slice);
    }

    @Deprecated
    public ListContent(Context context, @NonNull Slice slice) {
        super(slice);
        if (mSliceItem == null) {
            return;
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
        return style.getListHeight(this, policy);
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
    public DisplayedListItems getRowItems(int availableHeight, SliceStyle style,
            SliceViewPolicy policy) {
        if (policy.getMode() == MODE_SMALL) {
            return new DisplayedListItems(
                new ArrayList<>(Arrays.asList(getHeader())),
                /* hiddenItemCount= */ mRowItems.size() - 1);
        } else if (!policy.isScrollable() && availableHeight > 0) {
            return style.getListItemsForNonScrollingList(this, availableHeight, policy);
        }
        return new DisplayedListItems(
            style.getListItemsToDisplay(this), /* hiddenItemCount= */ 0);
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

    public SliceContent getSeeMoreItem() {
        return mSeeMoreContent;
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
     * Whether the first row should show title items on the start.
     */
    public void showTitleItems(boolean enabled) {
        if (mHeaderContent != null) {
            mHeaderContent.showTitleItems(enabled);
        }
    }

    /**
     * Whether the header row should show the bottom divider.
     */
    public void showHeaderDivider(boolean enabled) {
        if (mHeaderContent != null && mRowItems.size() > 1) {
            mHeaderContent.showBottomDivider(enabled);
        }
    }

    /**
     * Whether all the row contents should show action dividers.
     */
    public void showActionDividers(boolean enabled) {
        for (SliceContent item : mRowItems) {
            if (item instanceof RowContent) {
                ((RowContent) item).showActionDivider(enabled);
            }
        }
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
                } else if (rc.getSelection() != null) {
                    return EventInfo.ROW_TYPE_SELECTION;
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
        return style.getListItemsHeight(listItems, policy);
    }

    @Nullable
    private static SliceItem findHeaderItem(@NonNull Slice slice) {
        // See if header is specified
        String[] nonHints = new String[] {HINT_LIST_ITEM, HINT_SHORTCUT, HINT_ACTIONS,
                HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED, HINT_HORIZONTAL, HINT_SELECTION_OPTION};
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
