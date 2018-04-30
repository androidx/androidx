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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.view.R;

import java.util.List;

/**
 * Base class for children views of {@link SliceView}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class SliceChildView extends FrameLayout {

    protected SliceView.OnSliceActionListener mObserver;
    protected int mMode;
    protected int mTintColor = -1;
    protected int mTitleColor;
    protected int mSubtitleColor;
    protected int mHeaderTitleSize;
    protected int mHeaderSubtitleSize;
    protected int mVerticalHeaderTextPadding;
    protected int mTitleSize;
    protected int mSubtitleSize;
    protected int mVerticalTextPadding;
    protected int mGridTitleSize;
    protected int mGridSubtitleSize;
    protected int mVerticalGridTextPadding;
    protected int mGridTopPadding;
    protected int mGridBottomPadding;
    protected boolean mShowLastUpdated;
    protected long mLastUpdated = -1;

    public SliceChildView(@NonNull Context context) {
        super(context);
    }

    public SliceChildView(Context context, AttributeSet attributeSet) {
        this(context);
    }

    /**
     * Called when the view should be reset.
     */
    public abstract void resetView();

    /**
     * Sets the content to display in this slice.
     */
    public void setSliceContent(ListContent content) {
        // Do nothing
    }

    /**
     * Called when the slice being displayed in this view is an element of a larger list.
     */
    public void setSliceItem(SliceItem slice, boolean isHeader, int rowIndex,
            int rowCount, SliceView.OnSliceActionListener observer) {
        // Do nothing
    }

    /**
     * Sets the slice actions for this view.
     */
    public void setSliceActions(List<SliceItem> actions) {
        // Do nothing
    }

    /**
     * @return the height of the view when displayed in {@link SliceView#MODE_SMALL}.
     */
    public int getSmallHeight() {
        return 0;
    }

    /**
     * @return the height of the view when displayed in {@link SliceView#MODE_LARGE}.
     */
    public int getActualHeight() {
        return 0;
    }

    /**
     * Set the mode of the slice being presented.
     */
    public void setMode(int mode) {
        mMode = mode;
    }

    /**
     * @return the mode of the slice being presented.
     */
    @SliceView.SliceMode
    public int getMode() {
        return mMode;
    }

    /**
     * Sets a custom color to use for tinting elements like icons for this view.
     */
    public void setTint(@ColorInt int tintColor) {
        mTintColor = tintColor;
    }

    /**
     * Sets whether the last updated time should be displayed.
     */
    public void setShowLastUpdated(boolean showLastUpdated) {
        mShowLastUpdated = showLastUpdated;
    }

    /**
     * Sets when the content of this view was last updated.
     */
    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    /**
     * Sets the observer to notify when an interaction events occur on the view.
     */
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
    }

    /**
     * Populates style information for this view.
     */
    public void setStyle(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SliceView,
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
            int defaultVerticalGridPadding = getContext().getResources().getDimensionPixelSize(
                    R.dimen.abc_slice_grid_text_inner_padding);
            mVerticalGridTextPadding = (int) a.getDimension(
                    R.styleable.SliceView_gridTextVerticalPadding, defaultVerticalGridPadding);
            mGridTopPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0);
            mGridBottomPadding = (int) a.getDimension(R.styleable.SliceView_gridTopPadding, 0);
        } finally {
            a.recycle();
        }
    }
}
