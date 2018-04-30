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
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.core.SliceHints.HINT_KEYWORDS;
import static androidx.slice.core.SliceHints.HINT_LAST_UPDATED;
import static androidx.slice.core.SliceHints.HINT_TTL;
import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.SMALL_IMAGE;

import android.app.slice.Slice;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceHints;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a grid format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GridContent {

    private boolean mAllImages;
    private SliceItem mColorItem;
    private SliceItem mPrimaryAction;
    private ArrayList<CellContent> mGridContent = new ArrayList<>();
    private SliceItem mSeeMoreItem;
    private int mMaxCellLineCount;
    private boolean mHasImage;
    private @SliceHints.ImageMode int mLargestImageMode;
    private SliceItem mContentDescr;

    private int mBigPicMinHeight;
    private int mBigPicMaxHeight;
    private int mAllImagesHeight;
    private int mImageTextHeight;
    private int mMaxHeight;
    private int mMinHeight;

    public GridContent(Context context, SliceItem gridItem) {
        populate(gridItem);

        Resources res = context.getResources();
        mBigPicMinHeight = res.getDimensionPixelSize(R.dimen.abc_slice_big_pic_min_height);
        mBigPicMaxHeight = res.getDimensionPixelSize(R.dimen.abc_slice_big_pic_max_height);
        mAllImagesHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
        mImageTextHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_text_height);
        mMinHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_min_height);
        mMaxHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_max_height);
    }

    /**
     * @return whether this grid has content that is valid to display.
     */
    private boolean populate(SliceItem gridItem) {
        mColorItem = SliceQuery.findSubtype(gridItem, FORMAT_INT, SUBTYPE_COLOR);
        mSeeMoreItem = SliceQuery.find(gridItem, null, HINT_SEE_MORE, null);
        if (mSeeMoreItem != null && FORMAT_SLICE.equals(mSeeMoreItem.getFormat())) {
            mSeeMoreItem = mSeeMoreItem.getSlice().getItems().get(0);
        }
        String[] hints = new String[] {HINT_SHORTCUT, HINT_TITLE};
        mPrimaryAction = SliceQuery.find(gridItem, FORMAT_SLICE, hints,
                new String[] {HINT_ACTIONS} /* nonHints */);
        mAllImages = true;
        if (FORMAT_SLICE.equals(gridItem.getFormat())) {
            List<SliceItem> items = gridItem.getSlice().getItems();
            if (items.size() == 1 && FORMAT_SLICE.equals(items.get(0).getFormat())) {
                // TODO: this can be removed at release
                items = items.get(0).getSlice().getItems();
            }
            items = filterAndProcessItems(items);
            // Check if it it's only one item that is a slice
            if (items.size() == 1 && items.get(0).getFormat().equals(FORMAT_SLICE)) {
                items = items.get(0).getSlice().getItems();
            }
            for (int i = 0; i < items.size(); i++) {
                SliceItem item = items.get(i);
                if (SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())) {
                    mContentDescr = item;
                } else {
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
            mGridContent.add(cc);
            if (!cc.isImageOnly()) {
                mAllImages = false;
            }
            mMaxCellLineCount = Math.max(mMaxCellLineCount, cc.getTextCount());
            mHasImage |= cc.hasImage();
            mLargestImageMode = Math.max(mLargestImageMode, cc.getImageMode());
        }
    }

    /**
     * @return the list of cell content for this grid.
     */
    @NonNull
    public ArrayList<CellContent> getGridContent() {
        return mGridContent;
    }

    /**
     * @return the color to tint content in this grid.
     */
    @Nullable
    public SliceItem getColorItem() {
        return mColorItem;
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
     * @return content description for this row.
     */
    @Nullable
    public CharSequence getContentDescription() {
        return mContentDescr != null ? mContentDescr.getText() : null;
    }

    /**
     * @return whether this grid has content that is valid to display.
     */
    public boolean isValid() {
        return mGridContent.size() > 0;
    }

    /**
     * @return whether the contents of this grid is just images.
     */
    public boolean isAllImages() {
        return mAllImages;
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
     * @return the height to display a grid row at when it is used as a small template.
     * Does not include padding that might be added by slice view attributes,
     * see {@link ListContent#getListHeight(Context, List)}.
     */
    public int getSmallHeight() {
        return getHeight(true /* isSmall */);
    }

    /**
     * @return the height the content in this template requires to be displayed.
     * Does not include padding that might be added by slice view attributes,
     * see {@link ListContent#getListHeight(Context, List)}.
     */
    public int getActualHeight() {
        return getHeight(false /* isSmall */);
    }

    private int getHeight(boolean isSmall) {
        if (!isValid()) {
            return 0;
        }
        if (mAllImages) {
            return mGridContent.size() == 1
                    ? isSmall ? mBigPicMinHeight : mBigPicMaxHeight
                    : mLargestImageMode == ICON_IMAGE ? mMinHeight : mAllImagesHeight;
        } else {
            boolean twoLines = getMaxCellLineCount() > 1;
            boolean hasImage = hasImage();
            return (twoLines && !isSmall)
                    ? hasImage ? mMaxHeight : mMinHeight
                    : mLargestImageMode == ICON_IMAGE ? mMinHeight : mImageTextHeight;
        }
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
