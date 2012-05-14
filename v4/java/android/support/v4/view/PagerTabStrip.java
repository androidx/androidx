/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * PagerTabStrip is an interactive indicator of the current, next,
 * and previous pages of a {@link ViewPager}. It is intended to be used as a
 * child view of a ViewPager widget in your XML layout.
 * Add it as a child of a ViewPager in your layout file and set its
 * android:layout_gravity to TOP or BOTTOM to pin it to the top or bottom
 * of the ViewPager. The title from each page is supplied by the method
 * {@link PagerAdapter#getPageTitle(int)} in the adapter supplied to
 * the ViewPager.
 *
 * <p>For a non-interactive indicator, see {@link PagerTitleStrip}.</p>
 */
public class PagerTabStrip extends PagerTitleStrip {
    private static final int INDICATOR_HEIGHT = 3; // dp
    private static final int MIN_PADDING_BOTTOM = INDICATOR_HEIGHT + 3; // dp
    private static final int TAB_PADDING = 16; // dp
    private static final int TAB_SPACING = 32; // dp
    private static final int MIN_TEXT_SPACING = TAB_SPACING + TAB_PADDING * 2; // dp

    private int mIndicatorColor;
    private int mIndicatorHeight;

    private int mMinPaddingBottom;
    private int mMinTextSpacing;

    private int mTabPadding;

    private final Paint mTabPaint = new Paint();
    private final Rect mTempRect = new Rect();

    private int mTabAlpha = 0xFF;

    public PagerTabStrip(Context context) {
        this(context, null);
    }

    public PagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIndicatorColor = mTextColor;
        mTabPaint.setColor(mIndicatorColor);

        // Note: this follows the rules for Resources#getDimensionPixelOffset/Size:
        //       sizes round up, offsets round down.
        final float density = context.getResources().getDisplayMetrics().density;
        mIndicatorHeight = (int) (INDICATOR_HEIGHT * density + 0.5f);
        mMinPaddingBottom = (int) (MIN_PADDING_BOTTOM * density + 0.5f);
        mMinTextSpacing = (int) (MIN_TEXT_SPACING * density);
        mTabPadding = (int) (TAB_PADDING * density + 0.5f);

        // Enforce restrictions
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        setTextSpacing(getTextSpacing());

        setWillNotDraw(false);

        mPrevText.setFocusable(true);
        mPrevText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
            }
        });

        mNextText.setFocusable(true);
        mNextText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
        });
    }

    /**
     * Set the color of the tab indicator bar.
     *
     * @param color Color to set as an 0xRRGGBB value. The high byte (alpha) is ignored.
     */
    public void setTabIndicatorColor(int color) {
        mIndicatorColor = color;
        mTabPaint.setColor(mIndicatorColor);
        invalidate();
    }

    /**
     * Set the color of the tab indicator bar from a color resource.
     *
     * @param resId Resource ID of a color resource to load
     */
    public void setTabIndicatorColorResource(int resId) {
        setTabIndicatorColor(getContext().getResources().getColor(resId));
    }

    /**
     * @return The current tab indicator color as an 0xRRGGBB value.
     */
    public int getTabIndicatorColor() {
        return mIndicatorColor;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (bottom < mMinPaddingBottom) {
            bottom = mMinPaddingBottom;
        }
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public void setTextSpacing(int textSpacing) {
        if (textSpacing < mMinTextSpacing) {
            textSpacing = mMinTextSpacing;
        }
        super.setTextSpacing(textSpacing);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int bottom = getHeight();
        final int left = mCurrText.getLeft() - mTabPadding;
        final int right = mCurrText.getRight() + mTabPadding;
        final int top = bottom - mIndicatorHeight;

        mTabPaint.setColor(mTabAlpha << 24 | (mIndicatorColor & 0xFFFFFF));
        canvas.drawRect(left, top, right, bottom, mTabPaint);
    }

    @Override
    void updateTextPositions(int position, float positionOffset, boolean force) {
        final Rect r = mTempRect;
        int bottom = getHeight();
        int left = mCurrText.getLeft() - mTabPadding;
        int right = mCurrText.getRight() + mTabPadding;
        int top = bottom - mIndicatorHeight;

        r.set(left, top, right, bottom);

        super.updateTextPositions(position, positionOffset, force);
        mTabAlpha = (int) (Math.abs(positionOffset - 0.5f) * 2 * 0xFF);

        left = mCurrText.getLeft() - mTabPadding;
        right = mCurrText.getRight() + mTabPadding;
        r.union(left, top, right, bottom);

        invalidate(r);
    }
}
