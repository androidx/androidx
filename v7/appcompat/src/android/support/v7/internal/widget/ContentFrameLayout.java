/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.internal.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.FrameLayout;

import static android.view.View.MeasureSpec.*;

/**
 * @hide
 */
public class ContentFrameLayout extends FrameLayout {

    private TypedValue mMinWidthMajor;
    private TypedValue mMinWidthMinor;
    private TypedValue mFixedWidthMajor;
    private TypedValue mFixedWidthMinor;
    private TypedValue mFixedHeightMajor;
    private TypedValue mFixedHeightMinor;

    public ContentFrameLayout(Context context) {
        this(context, null);
    }

    public ContentFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContentFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @hide
     */
    public void dispatchFitSystemWindows(Rect insets) {
        fitSystemWindows(insets);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        final boolean isPortrait = metrics.widthPixels < metrics.heightPixels;

        final int widthMode = getMode(widthMeasureSpec);
        final int heightMode = getMode(heightMeasureSpec);

        boolean fixedWidth = false;
        if (widthMode == AT_MOST) {
            final TypedValue tvw = isPortrait ? mFixedWidthMinor : mFixedWidthMajor;
            if (tvw != null && tvw.type != TypedValue.TYPE_NULL) {
                final int w;
                if (tvw.type == TypedValue.TYPE_DIMENSION) {
                    w = (int) tvw.getDimension(metrics);
                } else if (tvw.type == TypedValue.TYPE_FRACTION) {
                    w = (int) tvw.getFraction(metrics.widthPixels, metrics.widthPixels);
                } else {
                    w = 0;
                }

                if (w > 0) {
                    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            Math.min(w, widthSize), EXACTLY);
                    fixedWidth = true;
                }
            }
        }

        if (heightMode == AT_MOST) {
            final TypedValue tvh = isPortrait ? mFixedHeightMajor : mFixedHeightMinor;
            if (tvh != null && tvh.type != TypedValue.TYPE_NULL) {
                final int h;
                if (tvh.type == TypedValue.TYPE_DIMENSION) {
                    h = (int) tvh.getDimension(metrics);
                } else if (tvh.type == TypedValue.TYPE_FRACTION) {
                    h = (int) tvh.getFraction(metrics.heightPixels, metrics.heightPixels);
                } else {
                    h = 0;
                }
                if (h > 0) {
                    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            Math.min(h, heightSize), EXACTLY);
                }
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        boolean measure = false;

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, EXACTLY);

        if (!fixedWidth && widthMode == AT_MOST) {
            final TypedValue tv = isPortrait ? mMinWidthMinor : mMinWidthMajor;
            if (tv != null && tv.type != TypedValue.TYPE_NULL) {
                final int min;
                if (tv.type == TypedValue.TYPE_DIMENSION) {
                    min = (int)tv.getDimension(metrics);
                } else if (tv.type == TypedValue.TYPE_FRACTION) {
                    min = (int)tv.getFraction(metrics.widthPixels, metrics.widthPixels);
                } else {
                    min = 0;
                }

                if (width < min) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(min, EXACTLY);
                    measure = true;
                }
            }
        }

        if (measure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public TypedValue getMinWidthMajor() {
        if (mMinWidthMajor == null) mMinWidthMajor = new TypedValue();
        return mMinWidthMajor;
    }

    public TypedValue getMinWidthMinor() {
        if (mMinWidthMinor == null) mMinWidthMinor = new TypedValue();
        return mMinWidthMinor;
    }

    public TypedValue getFixedWidthMajor() {
        if (mFixedWidthMajor == null) mFixedWidthMajor = new TypedValue();
        return mFixedWidthMajor;
    }

    public TypedValue getFixedWidthMinor() {
        if (mFixedWidthMinor == null) mFixedWidthMinor = new TypedValue();
        return mFixedWidthMinor;
    }

    public TypedValue getFixedHeightMajor() {
        if (mFixedHeightMajor == null) mFixedHeightMajor = new TypedValue();
        return mFixedHeightMajor;
    }

    public TypedValue getFixedHeightMinor() {
        if (mFixedHeightMinor == null) mFixedHeightMinor = new TypedValue();
        return mFixedHeightMinor;
    }
}
