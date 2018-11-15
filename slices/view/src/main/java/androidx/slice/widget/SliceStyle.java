/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.UNKNOWN_IMAGE;
import static androidx.slice.widget.SliceView.MODE_LARGE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.view.R;

/**
 * Holds style information shared between child views of a slice
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class SliceStyle {
    private int mTintColor = -1;
    private int mTitleColor;
    private int mSubtitleColor;
    private int mHeaderTitleSize;
    private int mHeaderSubtitleSize;
    private int mVerticalHeaderTextPadding;
    private int mTitleSize;
    private int mSubtitleSize;
    private int mVerticalTextPadding;
    private int mGridTitleSize;
    private int mGridSubtitleSize;
    private int mVerticalGridTextPadding;
    private int mGridTopPadding;
    private int mGridBottomPadding;

    private int mRowMaxHeight;
    private int mRowTextWithRangeHeight;
    private int mRowSingleTextWithRangeHeight;
    private int mRowMinHeight;
    private int mRowRangeHeight;

    private int mGridBigPicMinHeight;
    private int mGridBigPicMaxHeight;
    private int mGridAllImagesHeight;
    private int mGridImageTextHeight;
    private int mGridMaxHeight;
    private int mGridMinHeight;

    public SliceStyle(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliceView,
                defStyleAttr, defStyleRes);
        try {
            int themeColor = a.getColor(R.styleable.SliceView_tintColor, -1);
            mTintColor = themeColor != -1 ? themeColor : mTintColor;
            mTitleColor = a.getColor(R.styleable.SliceView_titleColor, 0);
            mSubtitleColor = a.getColor(R.styleable.SliceView_subtitleColor, 0);

            mHeaderTitleSize = (int) a.getDimension(
                    R.styleable.SliceView_headerTitleSize, 0);
            mHeaderSubtitleSize = (int) a.getDimension(
                    R.styleable.SliceView_headerSubtitleSize, 0);
            mVerticalHeaderTextPadding = (int) a.getDimension(
                    R.styleable.SliceView_headerTextVerticalPadding, 0);

            mTitleSize = (int) a.getDimension(R.styleable.SliceView_titleSize, 0);
            mSubtitleSize = (int) a.getDimension(
                    R.styleable.SliceView_subtitleSize, 0);
            mVerticalTextPadding = (int) a.getDimension(
                    R.styleable.SliceView_textVerticalPadding, 0);

            mGridTitleSize = (int) a.getDimension(R.styleable.SliceView_gridTitleSize, 0);
            mGridSubtitleSize = (int) a.getDimension(
                    R.styleable.SliceView_gridSubtitleSize, 0);
            int defaultVerticalGridPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.abc_slice_grid_text_inner_padding);
            mVerticalGridTextPadding = (int) a.getDimension(
                    R.styleable.SliceView_gridTextVerticalPadding, defaultVerticalGridPadding);
            mGridTopPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0);
            mGridBottomPadding = (int) a.getDimension(R.styleable.SliceView_gridBottomPadding, 0);
        } finally {
            a.recycle();
        }

        // Note: The above colors and dimensions are styleable, but the below ones are not.

        final Resources r = context.getResources();

        mRowMaxHeight = r.getDimensionPixelSize(R.dimen.abc_slice_row_max_height);
        mRowTextWithRangeHeight = r.getDimensionPixelSize(
                R.dimen.abc_slice_row_range_multi_text_height);
        mRowSingleTextWithRangeHeight = r.getDimensionPixelSize(
                R.dimen.abc_slice_row_range_single_text_height);
        mRowMinHeight = r.getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
        mRowRangeHeight = r.getDimensionPixelSize(R.dimen.abc_slice_row_range_height);

        mGridBigPicMinHeight = r.getDimensionPixelSize(R.dimen.abc_slice_big_pic_min_height);
        mGridBigPicMaxHeight = r.getDimensionPixelSize(R.dimen.abc_slice_big_pic_max_height);
        mGridAllImagesHeight = r.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
        mGridImageTextHeight = r.getDimensionPixelSize(R.dimen.abc_slice_grid_image_text_height);
        mGridMinHeight = r.getDimensionPixelSize(R.dimen.abc_slice_grid_min_height);
        mGridMaxHeight = r.getDimensionPixelSize(R.dimen.abc_slice_grid_max_height);
    }

    public int getRowMaxHeight() {
        return mRowMaxHeight;
    }

    public void setTintColor(int tint) {
        mTintColor = tint;
    }

    public int getTintColor() {
        return mTintColor;
    }

    public int getTitleColor() {
        return mTitleColor;
    }

    public int getSubtitleColor() {
        return mSubtitleColor;
    }

    public int getHeaderTitleSize() {
        return mHeaderTitleSize;
    }

    public int getHeaderSubtitleSize() {
        return mHeaderSubtitleSize;
    }

    public int getVerticalHeaderTextPadding() {
        return mVerticalHeaderTextPadding;
    }

    public int getTitleSize() {
        return mTitleSize;
    }

    public int getSubtitleSize() {
        return mSubtitleSize;
    }

    public int getVerticalTextPadding() {
        return mVerticalTextPadding;
    }

    public int getGridTitleSize() {
        return mGridTitleSize;
    }

    public int getGridSubtitleSize() {
        return mGridSubtitleSize;
    }

    public int getVerticalGridTextPadding() {
        return mVerticalGridTextPadding;
    }

    public int getGridTopPadding() {
        return mGridTopPadding;
    }

    public int getGridBottomPadding() {
        return mGridBottomPadding;
    }

    public int getRowHeight(RowContent row, SliceViewPolicy policy) {
        int maxHeight = policy.getMaxSmallHeight() > 0 ? policy.getMaxSmallHeight() : mRowMaxHeight;
        if (row.getRange() != null || policy.getMode() == MODE_LARGE) {
            if (row.getRange() != null) {
                // Range element always has set height and then the height of the text
                // area on the row will vary depending on if 1 or 2 lines of text.
                int textAreaHeight = row.getLineCount() > 1 ? mRowTextWithRangeHeight
                        : mRowSingleTextWithRangeHeight;
                return textAreaHeight + mRowRangeHeight;
            } else {
                return (row.getLineCount() > 1 || row.getIsHeader()) ? maxHeight : mRowMinHeight;
            }
        } else {
            return maxHeight;
        }
    }

    public int getGridHeight(GridContent grid, SliceViewPolicy policy) {
        boolean isSmall = policy.getMode() == MODE_SMALL;
        if (!grid.isValid()) {
            return 0;
        }
        int largestImageMode = grid.getLargestImageMode();
        int height;
        if (grid.isAllImages()) {
            height = grid.getGridContent().size() == 1
                    ? isSmall ? mGridBigPicMinHeight : mGridBigPicMaxHeight
                    : largestImageMode == ICON_IMAGE ? mGridMinHeight
                            : mGridAllImagesHeight;
        } else {
            boolean twoLines = grid.getMaxCellLineCount() > 1;
            boolean hasImage = grid.hasImage();
            boolean iconImagesOrNone = largestImageMode == ICON_IMAGE
                    || largestImageMode == UNKNOWN_IMAGE;
            height = (twoLines && !isSmall)
                    ? hasImage ? mGridMaxHeight : mGridMinHeight
                    : iconImagesOrNone ? mGridMinHeight : mGridImageTextHeight;
        }
        int topPadding = grid.isAllImages() && grid.getRowIndex() == 0
                ? mGridTopPadding : 0;
        int bottomPadding = grid.isAllImages() && grid.getIsLastIndex()
                ? mGridBottomPadding : 0;
        return height + topPadding + bottomPadding;
    }
}
