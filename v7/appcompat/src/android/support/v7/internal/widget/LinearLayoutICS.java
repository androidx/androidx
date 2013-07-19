/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * @hide
 */
public class LinearLayoutICS extends LinearLayout {

    private static final int SHOW_DIVIDER_NONE = 0;
    private static final int SHOW_DIVIDER_BEGINNING = 1;
    private static final int SHOW_DIVIDER_MIDDLE = 2;
    private static final int SHOW_DIVIDER_END = 4;

    private final Drawable mDivider;
    private final int mDividerWidth, mDividerHeight;
    private final int mShowDividers;
    private final int mDividerPadding;

    public LinearLayoutICS(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LinearLayoutICS);

        mDivider = a.getDrawable(R.styleable.LinearLayoutICS_divider);
        if (mDivider != null) {
            mDividerWidth = mDivider.getIntrinsicWidth();
            mDividerHeight = mDivider.getIntrinsicHeight();
        } else {
            mDividerHeight = mDividerWidth = 0;
        }

        mShowDividers = a.getInt(R.styleable.LinearLayoutICS_showDividers, SHOW_DIVIDER_NONE);
        mDividerPadding = a.getDimensionPixelSize(R.styleable.LinearLayoutICS_dividerPadding, 0);

        a.recycle();

        setWillNotDraw(mDivider == null);
    }

    public int getSupportDividerWidth() {
        return mDividerWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDivider == null) {
            return;
        }

        if (getOrientation() == VERTICAL) {
            drawSupportDividersVertical(canvas);
        } else {
            drawSupportDividersHorizontal(canvas);
        }
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {

        if (mDivider != null) {
            final int childIndex = indexOfChild(child);
            final int count = getChildCount();
            final LayoutParams params = (LayoutParams) child.getLayoutParams();

            // To display the dividers in-between the child views, we modify their margins
            // to create space.
            if (getOrientation() == VERTICAL) {
                if (hasSupportDividerBeforeChildAt(childIndex)) {
                    params.topMargin = mDividerHeight;
                } else if (childIndex == count - 1 && hasSupportDividerBeforeChildAt(count)) {
                    params.bottomMargin = mDividerHeight;
                }
            } else {
                if (hasSupportDividerBeforeChildAt(childIndex)) {
                    params.leftMargin = mDividerWidth;
                } else if (childIndex == count - 1 && hasSupportDividerBeforeChildAt(count)) {
                    params.rightMargin = mDividerWidth;
                }
            }
        }

        super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed,
                parentHeightMeasureSpec, heightUsed);
    }

    void drawSupportDividersVertical(Canvas canvas) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE &&
                    hasSupportDividerBeforeChildAt(i)) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                drawSupportHorizontalDivider(canvas, child.getTop() - lp.topMargin);
            }
        }

        if (hasSupportDividerBeforeChildAt(count)) {
            final View child = getChildAt(count - 1);
            int bottom = 0;
            if (child == null) {
                bottom = getHeight() - getPaddingBottom() - mDividerHeight;
            } else {
                bottom = child.getBottom();
            }
            drawSupportHorizontalDivider(canvas, bottom);
        }
    }

    void drawSupportDividersHorizontal(Canvas canvas) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE &&
                    hasSupportDividerBeforeChildAt(i)) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                drawSupportVerticalDivider(canvas, child.getLeft() - lp.leftMargin);
            }
        }

        if (hasSupportDividerBeforeChildAt(count)) {
            final View child = getChildAt(count - 1);
            int right = 0;
            if (child == null) {
                right = getWidth() - getPaddingRight() - mDividerWidth;
            } else {
                right = child.getRight();
            }
            drawSupportVerticalDivider(canvas, right);
        }
    }

    void drawSupportHorizontalDivider(Canvas canvas, int top) {
        mDivider.setBounds(getPaddingLeft() + mDividerPadding, top,
                getWidth() - getPaddingRight() - mDividerPadding, top + mDividerHeight);
        mDivider.draw(canvas);
    }

    void drawSupportVerticalDivider(Canvas canvas, int left) {
        mDivider.setBounds(left, getPaddingTop() + mDividerPadding,
                left + mDividerWidth, getHeight() - getPaddingBottom() - mDividerPadding);
        mDivider.draw(canvas);
    }

    /**
     * Determines where to position dividers between children.
     *
     * @param childIndex Index of child to check for preceding divider
     * @return true if there should be a divider before the child at childIndex
     */
    protected boolean hasSupportDividerBeforeChildAt(int childIndex) {
        if (childIndex == 0) {
            return (mShowDividers & SHOW_DIVIDER_BEGINNING) != 0;
        } else if (childIndex == getChildCount()) {
            return (mShowDividers & SHOW_DIVIDER_END) != 0;
        } else if ((mShowDividers & SHOW_DIVIDER_MIDDLE) != 0) {
            boolean hasVisibleViewBefore = false;
            for (int i = childIndex - 1; i >= 0; i--) {
                if (getChildAt(i).getVisibility() != GONE) {
                    hasVisibleViewBefore = true;
                    break;
                }
            }
            return hasVisibleViewBefore;
        }
        return false;
    }

}
