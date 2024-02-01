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

import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_RANGE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.core.SliceHints.HINT_END_OF_SECTION;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION_OPTION_KEY;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION_OPTION_VALUE;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a row format from a slice.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated
public class RowContent extends SliceContent {
    private static final String TAG = "RowContent";

    private SliceItem mPrimaryAction;
    private SliceItem mStartItem;
    private SliceItem mTitleItem;
    private SliceItem mSubtitleItem;
    private SliceItem mSummaryItem;
    private final ArrayList<SliceItem> mEndItems = new ArrayList<>();
    private final ArrayList<SliceAction> mToggleItems = new ArrayList<>();
    private SliceItem mRange;
    private SliceItem mSelection;
    private boolean mIsHeader;
    private int mLineCount = 0;
    private boolean mShowTitleItems;
    private boolean mShowBottomDivider;
    private boolean mShowActionDivider;

    public RowContent(SliceItem rowSlice, int position) {
        super(rowSlice, position);
        populate(rowSlice, position == 0);
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    private boolean populate(SliceItem rowSlice, boolean isHeader) {
        if (rowSlice.hasHint(HINT_END_OF_SECTION)) {
            showBottomDivider(true);
        }

        mIsHeader = isHeader;
        if (!isValidRow(rowSlice)) {
            Log.w(TAG, "Provided SliceItem is invalid for RowContent");
            return false;
        }
        determineStartAndPrimaryAction(rowSlice);

        // Filter anything not viable for displaying in a row
        ArrayList<SliceItem> rowItems = filterInvalidItems(rowSlice);
        // If we've only got one item that's a slice / action use those items instead
        boolean isOneItem = false;
        if (rowItems.size() == 1 && (FORMAT_ACTION.equals(rowItems.get(0).getFormat())
                || FORMAT_SLICE.equals(rowItems.get(0).getFormat()))
                && !rowItems.get(0).hasAnyHints(HINT_SHORTCUT, HINT_TITLE)) {
            if (isValidRow(rowItems.get(0))) {
                isOneItem = true;
                rowSlice = rowItems.get(0);
                rowItems = filterInvalidItems(rowSlice);
            }
        }
        if (SUBTYPE_RANGE.equals(rowSlice.getSubType())) {
            // It must be a Range, InputRange, or StarRating without StartItem/EndItem.
            if (SliceQuery.findSubtype(rowSlice, FORMAT_ACTION, SUBTYPE_RANGE) == null
                    || isOneItem) {
                mRange = rowSlice;
            } else {
                // Remove the startItem we already know about
                rowItems.remove(mStartItem);
                // After removing startItem, if size == 1, then it must be action<range>
                if (rowItems.size() == 1) {
                    if (isValidRow(rowItems.get(0))) {
                        rowSlice = rowItems.get(0);
                        rowItems = filterInvalidItems(rowSlice);
                        mRange = rowSlice;
                        // Remove thumb icon, don't let it be added into endItems
                        rowItems.remove(getInputRangeThumb());
                    }
                } else { // It must has end items.
                    mRange = SliceQuery.findSubtype(rowSlice, FORMAT_ACTION, SUBTYPE_RANGE);
                    // Refactor the rowItems, then it can be parsed correctly
                    ArrayList<SliceItem> rangeItems = filterInvalidItems(mRange);
                    rangeItems.remove(getInputRangeThumb());
                    rowItems.remove(mRange);
                    rowItems.addAll(rangeItems);
                }
            }
        }
        if (SUBTYPE_SELECTION.equals(rowSlice.getSubType())) {
            mSelection = rowSlice;
        }
        if (rowItems.size() > 0) {
            // Remove the things we already know about
            if (mStartItem != null) {
                rowItems.remove(mStartItem);
            }
            if (mPrimaryAction != null) {
                rowItems.remove(mPrimaryAction);
            }

            // Text + end items
            ArrayList<SliceItem> endItems = new ArrayList<>();
            for (int i = 0; i < rowItems.size(); i++) {
                final SliceItem item = rowItems.get(i);
                if (FORMAT_TEXT.equals(item.getFormat())) {
                    if ((mTitleItem == null || !mTitleItem.hasHint(HINT_TITLE))
                            && item.hasHint(HINT_TITLE) && !item.hasHint(HINT_SUMMARY)) {
                        mTitleItem = item;
                    } else if (mSubtitleItem == null && !item.hasHint(HINT_SUMMARY)) {
                        mSubtitleItem = item;
                    } else if (mSummaryItem == null && item.hasHint(HINT_SUMMARY)) {
                        mSummaryItem = item;
                    }
                } else {
                    endItems.add(item);
                }
            }
            if (hasText(mTitleItem)) {
                mLineCount++;
            }
            if (hasText(mSubtitleItem)) {
                mLineCount++;
            }
            // Special rules for end items: only one timestamp
            boolean hasTimestamp = mStartItem != null
                    && FORMAT_LONG.equals(mStartItem.getFormat());
            for (int i = 0; i < endItems.size(); i++) {
                final SliceItem item = endItems.get(i);
                boolean isAction = SliceQuery.find(item, FORMAT_ACTION) != null;
                if (FORMAT_LONG.equals(item.getFormat())) {
                    if (!hasTimestamp) {
                        hasTimestamp = true;
                        mEndItems.add(item);
                    }
                } else {
                    processContent(item, isAction);
                }
            }
        }
        return isValid();
    }

    private void processContent(@NonNull SliceItem item, boolean isAction) {
        if (isAction) {
            SliceAction ac = new SliceActionImpl(item);
            if (ac.isToggle()) {
                mToggleItems.add(ac);
            }
        }
        mEndItems.add(item);
    }

    /**
     * Sets the {@link #getPrimaryAction()} and {@link #getStartItem()} for this row.
     */
    private void determineStartAndPrimaryAction(@NonNull SliceItem rowSlice) {
        List<SliceItem> possibleStartItems = SliceQuery.findAll(rowSlice, null, HINT_TITLE, null);
        if (possibleStartItems.size() > 0) {
            // The start item will be at position 0 if it exists
            String format = possibleStartItems.get(0).getFormat();
            if ((FORMAT_ACTION.equals(format)
                    && SliceQuery.find(possibleStartItems.get(0), FORMAT_IMAGE) != null)
                    || FORMAT_SLICE.equals(format)
                    || FORMAT_LONG.equals(format)
                    || FORMAT_IMAGE.equals(format)) {
                mStartItem = possibleStartItems.get(0);
            }
        }

        // Possible primary actions could be in the format of slice or action.
        String[] hints = new String[] {HINT_SHORTCUT, HINT_TITLE};
        List<SliceItem> possiblePrimaries = SliceQuery.findAll(rowSlice, FORMAT_SLICE, hints, null);
        possiblePrimaries.addAll(SliceQuery.findAll(rowSlice, FORMAT_ACTION, hints, null));

        if (possiblePrimaries.isEmpty() && FORMAT_ACTION.equals(rowSlice.getFormat())
                && rowSlice.getSlice().getItems().size() == 1) {
            mPrimaryAction = rowSlice;
        } else if (mStartItem != null && possiblePrimaries.size() > 1
                && possiblePrimaries.get(0) == mStartItem) {
            // Next item is the primary action
            mPrimaryAction = possiblePrimaries.get(1);
        } else if (possiblePrimaries.size() > 0) {
            mPrimaryAction = possiblePrimaries.get(0);
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid()
                && (mStartItem != null
                || mPrimaryAction != null
                || mTitleItem != null
                || mSubtitleItem != null
                || mEndItems.size() > 0
                || mRange != null
                || mSelection != null
                || isDefaultSeeMore());
    }

    /**
     * @return whether this row represents a header or not.
     */
    public boolean getIsHeader() { return mIsHeader; }

    /**
     * Sets whether this row represents a header or not.
     */
    public void setIsHeader(boolean isHeader) {
        mIsHeader = isHeader;
    }

    /**
     * @return the {@link SliceItem} representing the range in the row; can be null.
     */
    @Nullable
    public SliceItem getRange() {
        return mRange;
    }

    /**
     * @return the {@link SliceItem} representing the selection in the row; can be null.
     */
    @Nullable
    public SliceItem getSelection() {
        return mSelection;
    }

    /**
     * @return the {@link SliceItem} for the icon to use for the input range thumb drawable.
     */
    @Nullable
    public SliceItem getInputRangeThumb() {
        if (mRange != null) {
            List<SliceItem> items = mRange.getSlice().getItems();
            for (int i = 0; i < items.size(); i++) {
                if (FORMAT_IMAGE.equals(items.get(i).getFormat())) {
                    return items.get(i);
                }
            }
        }
        return null;
    }

    /**
     * @return the {@link SliceItem} used for the main intent for this row; can be null.
     */
    @Nullable
    public SliceItem getPrimaryAction() {
        return mPrimaryAction;
    }

    /**
     * @return the {@link SliceItem} to display at the start of this row; can be null.
     */
    @Nullable
    public SliceItem getStartItem() {
        return mIsHeader && !mShowTitleItems ? null : mStartItem;
    }

    /**
     * @return the {@link SliceItem} representing the title text for this row; can be null.
     */
    @Nullable
    public SliceItem getTitleItem() {
        return mTitleItem;
    }

    /**
     * @return the {@link SliceItem} representing the subtitle text for this row; can be null.
     */
    @Nullable
    public SliceItem getSubtitleItem() {
        return mSubtitleItem;
    }

    @Nullable
    public SliceItem getSummaryItem() {
        return mSummaryItem == null ? mSubtitleItem : mSummaryItem;
    }

    /**
     * @return the list of {@link SliceItem} that can be shown as items at the end of the row.
     */
    public List<SliceItem> getEndItems() {
        return mEndItems;
    }

    /**
     * @return a list of toggles associated with this row.
     */
    public List<SliceAction> getToggleItems() {
        return mToggleItems;
    }

    /**
     * @return the number of lines of text contained in this row.
     */
    public int getLineCount() {
        return mLineCount;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public int getHeight(SliceStyle style, SliceViewPolicy policy) {
        return style.getRowHeight(this, policy);
    }

    /**
     * @return whether this row content represents a default see more item.
     */
    public boolean isDefaultSeeMore() {
        return FORMAT_ACTION.equals(mSliceItem.getFormat())
                && mSliceItem.getSlice().hasHint(HINT_SEE_MORE)
                && mSliceItem.getSlice().getItems().isEmpty();
    }

    /**
     * Set whether this row content needs to show the title items on the start.
     */
    public void showTitleItems(boolean enabled) {
        mShowTitleItems = enabled;
    }

    /**
     * @return whether this row content needs to show the title items on the start.
     */
    public boolean hasTitleItems() {
        return mShowTitleItems;
    }

    /**
     * Set whether this row content needs to show the bottom divider.
     */
    public void showBottomDivider(boolean enabled) {
        mShowBottomDivider = enabled;
    }

    /**
     * @return whether this row content needs to show the bottom divider.
     */
    public boolean hasBottomDivider() {
        return mShowBottomDivider;
    }

    /**
     * Set whether this row content needs to show the action divider.
     */
    public void showActionDivider(boolean enabled) {
        mShowActionDivider = enabled;
    }

    /**
     * @return whether this row content needs to show the action divider.
     */
    public boolean hasActionDivider() {
        return mShowActionDivider;
    }

    /**
     * @return whether this slice item has text that is present or will be present (i.e. loading).
     */
    private static boolean hasText(SliceItem textSlice) {
        return textSlice != null
                && (textSlice.hasHint(HINT_PARTIAL)
                || !TextUtils.isEmpty(textSlice.getText()));
    }

    /**
     * @return whether this is a valid item to use to populate a row of content.
     */
    private static boolean isValidRow(SliceItem rowSlice) {
        if (rowSlice == null) {
            return false;
        }
        // Must be slice or action
        if (FORMAT_SLICE.equals(rowSlice.getFormat())
                || FORMAT_ACTION.equals(rowSlice.getFormat())) {
            List<SliceItem> rowItems = rowSlice.getSlice().getItems();
            // Special case: default see more just has an action but no other items
            if (rowSlice.hasHint(HINT_SEE_MORE) && rowItems.isEmpty()) {
                return true;
            }
            // Must have at least one legitimate child
            for (int i = 0; i < rowItems.size(); i++) {
                if (isValidRowContent(rowSlice, rowItems.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return list of {@link SliceItem}s that are valid to display in a row according
     * to {@link #isValidRowContent(SliceItem, SliceItem)}.
     */
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
     * @return whether this item is valid content to visibly appear in a row.
     */
    private static boolean isValidRowContent(SliceItem slice, SliceItem item) {
        // XXX: This is fragile -- new subtypes may be erroneously displayed by old clients, since
        // this is effectively a blocklist, not an allowlist. I'm not sure if SELECTION_OPTION_KEY
        // needs to be here, but better safe than sorry.
        if (item.hasAnyHints(HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED, HINT_HORIZONTAL)
                || SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())
                || SUBTYPE_SELECTION_OPTION_KEY.equals(item.getSubType())
                || SUBTYPE_SELECTION_OPTION_VALUE.equals(item.getSubType())) {
            return false;
        }
        final String itemFormat = item.getFormat();
        // XXX: This is confusing -- the FORMAT_INTs in a SUBTYPE_RANGE Slice aren't visible
        // themselves, they're just used to inform rendering.
        return FORMAT_IMAGE.equals(itemFormat)
                || FORMAT_TEXT.equals(itemFormat)
                || FORMAT_LONG.equals(itemFormat)
                || FORMAT_ACTION.equals(itemFormat)
                || FORMAT_REMOTE_INPUT.equals(itemFormat)
                || FORMAT_SLICE.equals(itemFormat)
                || (FORMAT_INT.equals(itemFormat) && SUBTYPE_RANGE.equals(slice.getSubType()));
    }
}
