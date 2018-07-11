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

import androidx.annotation.RestrictTo;
import androidx.slice.view.R;

/**
 * Holds style information shared between child views of a slice
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
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
}
