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

import static androidx.slice.core.SliceHints.HINT_OVERLAY;
import static androidx.slice.core.SliceHints.SUBTYPE_DATE_PICKER;
import static androidx.slice.core.SliceHints.SUBTYPE_TIME_PICKER;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.SliceItem;
import androidx.slice.SliceUtils;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a grid format from a slice.
 */
@RequiresApi(19)
public class GridContent extends SliceContent {

    private boolean mAllImages;
    private SliceItem mPrimaryAction;
    private final ArrayList<CellContent> mGridContent = new ArrayList<>();
    private SliceItem mSeeMoreItem;
    private int mMaxCellLineCount;
    private int mLargestImageMode = UNKNOWN_IMAGE;
    private boolean mIsLastIndex;
    private IconCompat mFirstImage = null;
    private Point mFirstImageSize = null;

    private SliceItem mTitleItem;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public GridContent(@NonNull SliceItem gridItem, int position) {
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
            if (mFirstImage == null && cc.hasImage()) {
                mFirstImage = cc.getImageIcon();
            }
            mLargestImageMode = mLargestImageMode == UNKNOWN_IMAGE
                    ? cc.getImageMode()
                    : Math.max(mLargestImageMode, cc.getImageMode());
        }
    }

    /**
     * @return the title of this grid row, if it exists.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    public ArrayList<CellContent> getGridContent() {
        return mGridContent;
    }

    /**
     * @return the content intent item for this grid.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Nullable
    public SliceItem getContentIntent() {
        return mPrimaryAction;
    }

    /**
     * @return the see more item to use when not all items in the grid can be displayed.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public boolean isAllImages() {
        return mAllImages;
    }

    /**
     * @return the largest image size in this row, if there are images.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public int getLargestImageMode() {
        return mLargestImageMode;
    }

    /**
     * @return the first image dimensions in this row, if there are images. If there are no images,
     * return {-1, -1}.
     */
    @NonNull
    public Point getFirstImageSize(@NonNull Context context) {
        if (mFirstImage == null) {
            return new Point(-1, -1);
        }
        if (mFirstImageSize == null) {
            Drawable d = mFirstImage.loadDrawable(context);
            mFirstImageSize = new Point(d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
        return mFirstImageSize;
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
                    HINT_LAST_UPDATED, HINT_OVERLAY);
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public int getMaxCellLineCount() {
        return mMaxCellLineCount;
    }

    /**
     * @return whether this row contains an image.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public boolean hasImage() {
        return mFirstImage != null;
    }

    /**
     * @return whether this content is being displayed last in a list.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public boolean getIsLastIndex() { return mIsLastIndex; }

    /**
     * Sets whether this content is being displayed last in a list.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public void setIsLastIndex(boolean isLast) {
        mIsLastIndex = isLast;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public int getHeight(@NonNull SliceStyle style, @NonNull SliceViewPolicy policy) {
        return style.getGridHeight(this, policy);
    }

    /**
     * Extracts information required to present content in a cell.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static class CellContent {
        private SliceItem mContentIntent;
        private SliceItem mPicker;
        private final ArrayList<SliceItem> mCellItems = new ArrayList<>();
        private SliceItem mContentDescr;
        private int mTextCount;
        private int mImageCount;
        private IconCompat mImage;
        private SliceItem mOverlayItem;
        private int mImageMode = -1;
        private SliceItem mTitleItem;
        private SliceItem mToggleItem;

        public CellContent(@NonNull SliceItem cellItem) {
            populate(cellItem);
        }

        /**
         * @return whether this row has content that is valid to display.
         */
        public boolean populate(@NonNull SliceItem cellItem) {
            final String format = cellItem.getFormat();
            if (!cellItem.hasHint(HINT_SHORTCUT)
                    && (FORMAT_SLICE.equals(format) || FORMAT_ACTION.equals(format))) {
                List<SliceItem> items = cellItem.getSlice().getItems();
                List<SliceItem> sliceActionItems = null;

                // Fill the sliceActionItems with the first showing SliceAction in items.
                for (SliceItem item : items) {
                    if ((FORMAT_ACTION.equals(item.getFormat())
                            || FORMAT_SLICE.equals(item.getFormat()))
                            && !(SUBTYPE_DATE_PICKER.equals(item.getSubType())
                            || SUBTYPE_TIME_PICKER.equals(item.getSubType()))) {
                        sliceActionItems = item.getSlice().getItems();
                        SliceAction ac = new SliceActionImpl(item);
                        if (ac.isToggle()) {
                            mToggleItem = item;
                        } else {
                            mContentIntent = items.get(0);
                        }
                        break;
                    }
                }
                if (FORMAT_ACTION.equals(format)) {
                    mContentIntent = cellItem;
                }
                mTextCount = 0;
                mImageCount = 0;
                fillCellItems(items);

                if (mTextCount == 0 && mImageCount == 0 && sliceActionItems != null) {
                    fillCellItems(sliceActionItems);
                }
            } else if (isValidCellContent(cellItem)) {
                mCellItems.add(cellItem);
            }
            return isValid();
        }

        private void fillCellItems(List<SliceItem> items) {
            for (int i = 0; i < items.size(); i++) {
                final SliceItem item = items.get(i);
                final String itemFormat = item.getFormat();
                if (mPicker == null && (SUBTYPE_DATE_PICKER.equals(item.getSubType())
                        || SUBTYPE_TIME_PICKER.equals(item.getSubType()))) {
                    mPicker = item;
                } else if (SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType())) {
                    mContentDescr = item;
                } else if (mTextCount < 2 && (FORMAT_TEXT.equals(itemFormat)
                        || FORMAT_LONG.equals(itemFormat))) {
                    if (mTitleItem == null
                            || (!mTitleItem.hasHint(HINT_TITLE) && item.hasHint(HINT_TITLE))) {
                        mTitleItem = item;
                    }
                    if (item.hasHint(HINT_OVERLAY)) {
                        if (mOverlayItem == null) {
                            mOverlayItem = item;
                        }
                    } else {
                        mTextCount++;
                        mCellItems.add(item);
                    }
                } else if (mImageCount < 1 && FORMAT_IMAGE.equals(item.getFormat())) {
                    mImageMode = SliceUtils.parseImageMode(item);
                    mImageCount++;
                    mImage = item.getIcon();
                    mCellItems.add(item);
                }
            }
        }


        /**
         * @return toggle slice item if this cell has one.
         */
        @Nullable
        public SliceItem getToggleItem() {
            return mToggleItem;
        }

        /**
         * @return title text slice item if this cell has one.
         */
        @Nullable
        public SliceItem getTitleItem() {
            return mTitleItem;
        }

        /**
         * @return image overlay text slice item if this cell has one.
         */
        @Nullable
        public SliceItem getOverlayItem() {
            return mOverlayItem;
        }

        /**
         * @return the action to activate when this cell is tapped.
         */
        @Nullable
        public SliceItem getContentIntent() {
            return mContentIntent;
        }

        /**
         * @return the Picker to use when this cell is tapped.
         */
        @Nullable
        public SliceItem getPicker() {
            return mPicker;
        }

        /**
         * @return the slice items to display in this cell.
         */
        @NonNull
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
            return mPicker != null || (mCellItems.size() > 0 && mCellItems.size() <= 3);
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
            return mImage != null;
        }

        /**
         * @return the mode of the image.
         */
        public int getImageMode() {
            return mImageMode;
        }

        /**
         * @return the IconCompat of the image.
         */
        @Nullable
        public IconCompat getImageIcon() {
            return mImage;
        }

        @Nullable
        public CharSequence getContentDescription() {
            return mContentDescr != null ? mContentDescr.getText() : null;
        }
    }
}
