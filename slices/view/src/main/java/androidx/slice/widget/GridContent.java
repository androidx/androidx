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
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.SMALL_IMAGE;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;

import android.app.slice.Slice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a grid format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(19)
public class GridContent extends SliceContent {

    private boolean mAllImages;
    private SliceItem mPrimaryAction;
    private ArrayList<CellContent> mGridContent = new ArrayList<>();
    private SliceItem mSeeMoreItem;
    private int mMaxCellLineCount;
    private boolean mHasImage;
    private int mLargestImageMode = UNKNOWN_IMAGE;
    private boolean mIsLastIndex;

    private SliceItem mTitleItem;

    public GridContent(SliceItem gridItem, int position) {
        super(gridItem, position);
        populate(gridItem);
    }

    /**
     * @return whether this grid has content that is valid to display.
     */
    private boolean populate(SliceItem gridItem) {
        mSeeMoreItem = SliceQuery.find(gridItem, null, HINT_SEE_MORE, null);
        if (mSeeMoreItem != null && FORMAT_SLICE.equals(mSeeMoreItem.getFormat())) {
            List<SliceItem> seeMoreItems = mSeeMoreItem.getSlice().getItems();
            if (seeMoreItems != null && seeMoreItems.size() > 0) {
                mSeeMoreItem = seeMoreItems.get(0);
            }
        }
        String[] hints = new String[] {HINT_SHORTCUT, HINT_TITLE};
        mPrimaryAction = SliceQuery.find(gridItem, FORMAT_SLICE, hints,
                new String[] {HINT_ACTIONS} /* nonHints */);
        mAllImages = true;
        if (FORMAT_SLICE.equals(gridItem.getFormat())) {
            List<SliceItem> items = gridItem.getSlice().getItems();
            items = filterAndProcessItems(items);
            for (int i = 0; i < items.size(); i++) {
                SliceItem item = items.get(i);
                if (!SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())) {
                    CellContent cc = new CellContent(item);
                    processContent(cc);
                }
            }
        } else {
            CellContent cc = new CellContent(gridItem);
            processContent(cc);
        }
        return isValid();
    }

    private void processContent(CellContent cc) {
        if (cc.isValid()) {
            if ((mTitleItem == null && cc.getTitleItem() != null)) {
                mTitleItem = cc.getTitleItem();
            }
            mGridContent.add(cc);
            if (!cc.isImageOnly()) {
                mAllImages = false;
            }
            mMaxCellLineCount = Math.max(mMaxCellLineCount, cc.getTextCount());
            mHasImage |= cc.hasImage();
            mLargestImageMode = mLargestImageMode == UNKNOWN_IMAGE
                    ? cc.getImageMode()
                    : Math.max(mLargestImageMode, cc.getImageMode());
        }
    }

    /**
     * @return the title of this grid row, if it exists.
     */
    @Nullable
    public CharSequence getTitle() {
        if (mTitleItem != null) {
            return mTitleItem.getSanitizedText();
        } else if (mPrimaryAction != null) {
            return new SliceActionImpl(mPrimaryAction).getTitle();
        }
        return null;
    }

    /**
     * @return the list of cell content for this grid.
     */
    @NonNull
    public ArrayList<CellContent> getGridContent() {
        return mGridContent;
    }

    /**
     * @return the content intent item for this grid.
     */
    @Nullable
    public SliceItem getContentIntent() {
        return mPrimaryAction;
    }

    /**
     * @return the see more item to use when not all items in the grid can be displayed.
     */
    @Nullable
    public SliceItem getSeeMoreItem() {
        return mSeeMoreItem;
    }

    /**
     * @return whether this grid has content that is valid to display.
     */
    @Override
    public boolean isValid() {
        return super.isValid() && mGridContent.size() > 0;
    }

    /**
     * @return whether the contents of this grid is just images.
     */
    public boolean isAllImages() {
        return mAllImages;
    }

    /**
     * @return the largest image size in this row, if there are images.
     */
    public int getLargestImageMode() {
        return mLargestImageMode;
    }

    /**
     * Filters non-cell items out of the list of items and finds content description.
     */
    private List<SliceItem> filterAndProcessItems(List<SliceItem> items) {
        List<SliceItem> filteredItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            // TODO: This see more can be removed at release
            boolean containsSeeMore = SliceQuery.find(item, null, HINT_SEE_MORE, null) != null;
            boolean isNonCellContent = containsSeeMore
                    || item.hasAnyHints(HINT_SHORTCUT, HINT_SEE_MORE, HINT_KEYWORDS, HINT_TTL,
                            HINT_LAST_UPDATED);
            if (SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())) {
                mContentDescr = item;
            } else if (!isNonCellContent) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    /**
     * @return the max number of lines of text in the cells of this grid row.
     */
    public int getMaxCellLineCount() {
        return mMaxCellLineCount;
    }

    /**
     * @return whether this row contains an image.
     */
    public boolean hasImage() {
        return mHasImage;
    }

    /**
     * @return whether this content is being displayed last in a list.
     */
    public boolean getIsLastIndex() { return mIsLastIndex; }

    /**
     * Sets whether this content is being displayed last in a list.
     */
    public void setIsLastIndex(boolean isLast) {
        mIsLastIndex = isLast;
    }

    @Override
    public int getHeight(SliceStyle style, SliceViewPolicy policy) {
        return style.getGridHeight(this, policy);
    }

    /**
     * Extracts information required to present content in a cell.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class CellContent {
        private SliceItem mContentIntent;
        private ArrayList<SliceItem> mCellItems = new ArrayList<>();
        private SliceItem mContentDescr;
        private int mTextCount;
        private boolean mHasImage;
        private int mImageMode = -1;
        private SliceItem mTitleItem;

        public CellContent(SliceItem cellItem) {
            populate(cellItem);
        }

        /**
         * @return whether this row has content that is valid to display.
         */
        public boolean populate(SliceItem cellItem) {
            final String format = cellItem.getFormat();
            if (!cellItem.hasHint(HINT_SHORTCUT)
                    && (FORMAT_SLICE.equals(format) || FORMAT_ACTION.equals(format))) {
                List<SliceItem> items = cellItem.getSlice().getItems();
                // If we've only got one item that's a slice / action use those items instead
                if (items.size() == 1 && (FORMAT_ACTION.equals(items.get(0).getFormat())
                        || FORMAT_SLICE.equals(items.get(0).getFormat()))) {
                    mContentIntent = items.get(0);
                    items = items.get(0).getSlice().getItems();
                }
                if (FORMAT_ACTION.equals(format)) {
                    mContentIntent = cellItem;
                }
                mTextCount = 0;
                int imageCount = 0;
                for (int i = 0; i < items.size(); i++) {
                    final SliceItem item = items.get(i);
                    final String itemFormat = item.getFormat();
                    if (SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())) {
                        mContentDescr = item;
                    } else if (mTextCount < 2 && (FORMAT_TEXT.equals(itemFormat)
                            || FORMAT_LONG.equals(itemFormat))) {
                        mTextCount++;
                        mCellItems.add(item);
                        if (mTitleItem == null
                                || (!mTitleItem.hasHint(HINT_TITLE) && item.hasHint(HINT_TITLE))) {
                            mTitleItem = item;
                        }
                    } else if (imageCount < 1 && FORMAT_IMAGE.equals(item.getFormat())) {
                        if (item.hasHint(Slice.HINT_NO_TINT)) {
                            mImageMode = item.hasHint(Slice.HINT_LARGE)
                                    ? LARGE_IMAGE
                                    : SMALL_IMAGE;
                        } else {
                            mImageMode = ICON_IMAGE;
                        }
                        imageCount++;
                        mHasImage = true;
                        mCellItems.add(item);
                    }
                }
            } else if (isValidCellContent(cellItem)) {
                mCellItems.add(cellItem);
            }
            return isValid();
        }

        /**
         * @return title text slice item if this cell has one.
         */
        @Nullable
        public SliceItem getTitleItem() {
            return mTitleItem;
        }

        /**
         * @return the action to activate when this cell is tapped.
         */
        public SliceItem getContentIntent() {
            return mContentIntent;
        }

        /**
         * @return the slice items to display in this cell.
         */
        public ArrayList<SliceItem> getCellItems() {
            return mCellItems;
        }

        /**
         * @return whether this is content that is valid to show in a grid cell.
         */
        private boolean isValidCellContent(SliceItem cellItem) {
            final String format = cellItem.getFormat();
            boolean isNonCellContent = SUBTYPE_CONTENT_DESCRIPTION.equals(cellItem.getSubType())
                    || cellItem.hasAnyHints(HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED);
            return !isNonCellContent
                    && (FORMAT_TEXT.equals(format)
                    || FORMAT_LONG.equals(format)
                    || FORMAT_IMAGE.equals(format));
        }

        /**
         * @return whether this grid has content that is valid to display.
         */
        public boolean isValid() {
            return mCellItems.size() > 0 && mCellItems.size() <= 3;
        }

        /**
         * @return whether this cell contains just an image.
         */
        public boolean isImageOnly() {
            return mCellItems.size() == 1 && FORMAT_IMAGE.equals(mCellItems.get(0).getFormat());
        }

        /**
         * @return number of text items in this cell.
         */
        public int getTextCount() {
            return mTextCount;
        }

        /**
         * @return whether this cell contains an image.
         */
        public boolean hasImage() {
            return mHasImage;
        }

        /**
         * @return the mode of the image.
         */
        public int getImageMode() {
            return mImageMode;
        }

        @Nullable
        public CharSequence getContentDescription() {
            return mContentDescr != null ? mContentDescr.getText() : null;
        }
    }
}
