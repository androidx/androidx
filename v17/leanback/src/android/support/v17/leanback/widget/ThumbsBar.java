/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ThumbsBar extends LinearLayout {

    static final int DEFAULT_NUM_OF_THUMBS = 7;

    int mMinimalMargin = 16;
    int mNumOfThumbs;
    int mThumbWidth = 160;
    int mThumbHeight = 160;
    int mHeroThumbWidth = 240;
    int mHeroThumbHeight = 240;
    int mMeasuredMargin;
    final SparseArray<Bitmap> mBitmaps = new SparseArray<>();

    public ThumbsBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbsBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setNumberOfThumbs(DEFAULT_NUM_OF_THUMBS);
    }

    /**
     * Get hero index which is the middle child.
     */
    public int getHeroIndex() {
        return getChildCount() / 2;
    }

    /**
     * Set size of thumb view in pixels
     */
    public void setThumbSize(int width, int height) {
        mThumbHeight = height;
        mThumbWidth = width;
        int heroIndex = getHeroIndex();
        for (int i = 0; i < getChildCount(); i++) {
            if (heroIndex != i) {
                View child = getChildAt(i);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                boolean changed = false;
                if (lp.height != height) {
                    lp.height = height;
                    changed = true;
                }
                if (lp.width != width) {
                    lp.width = width;
                    changed = true;
                }
                if (changed) {
                    child.setLayoutParams(lp);
                }
            }
        }
    }

    /**
     * Set size of hero thumb view in pixels, it is usually larger than other thumbs.
     */
    public void setHeroThumbSize(int width, int height) {
        mHeroThumbHeight = height;
        mHeroThumbWidth = width;
        int heroIndex = getHeroIndex();
        for (int i = 0; i < getChildCount(); i++) {
            if (heroIndex == i) {
                View child = getChildAt(i);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                boolean changed = false;
                if (lp.height != height) {
                    lp.height = height;
                    changed = true;
                }
                if (lp.width != width) {
                    lp.width = width;
                    changed = true;
                }
                if (changed) {
                    child.setLayoutParams(lp);
                }
            }
        }
    }

    /**
     * Set number of thumb views. It must be odd or it will be increasing one.
     */
    public void setNumberOfThumbs(int numOfThumbs) {
        if (numOfThumbs < 0) {
            throw new IllegalArgumentException();
        }
        if ((numOfThumbs & 1) == 0) {
            // make it odd number
            numOfThumbs++;
        }
        mNumOfThumbs = numOfThumbs;
        while (getChildCount() > mNumOfThumbs) {
            removeView(getChildAt(getChildCount() - 1));
        }
        while (getChildCount() < mNumOfThumbs) {
            View view = createThumbView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mThumbWidth, mThumbHeight);
            addView(view, lp);
        }
        int heroIndex = getHeroIndex();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            if (heroIndex == i) {
                lp.width = mHeroThumbWidth;
                lp.height = mHeroThumbHeight;
            } else {
                lp.width = mThumbWidth;
                lp.height = mThumbHeight;
            }
            child.setLayoutParams(lp);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int spaceForMargin = 0;
        while (mNumOfThumbs > 1) {
            spaceForMargin = width - mHeroThumbWidth - mThumbWidth * (mNumOfThumbs - 1);
            if (spaceForMargin < mMinimalMargin * (mNumOfThumbs - 1)) {
                setNumberOfThumbs(mNumOfThumbs - 2);
            } else {
                break;
            }
        }
        mMeasuredMargin = mNumOfThumbs > 0 ? spaceForMargin / (mNumOfThumbs - 1) : 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int heroIndex = getHeroIndex();
        View heroView = getChildAt(heroIndex);
        int heroLeft = getWidth() / 2 - heroView.getMeasuredWidth() / 2;
        int heroRight = getWidth() / 2 + heroView.getMeasuredWidth() / 2;
        heroView.layout(heroLeft, getPaddingTop(), heroRight,
                getPaddingTop() + heroView.getMeasuredHeight());
        int heroCenter = getPaddingTop() + heroView.getMeasuredHeight() / 2;

        for (int i = heroIndex - 1; i >= 0; i--) {
            heroLeft -= mMeasuredMargin;
            View child = getChildAt(i);
            child.layout(heroLeft - child.getMeasuredWidth(),
                    heroCenter - child.getMeasuredHeight() / 2,
                    heroLeft,
                    heroCenter + child.getMeasuredHeight() / 2);
            heroLeft -= child.getMeasuredWidth();
        }
        for (int i = heroIndex + 1; i < mNumOfThumbs; i++) {
            heroRight += mMeasuredMargin;
            View child = getChildAt(i);
            child.layout(heroRight,
                    heroCenter - child.getMeasuredHeight() / 2,
                    heroRight + child.getMeasuredWidth(),
                    heroCenter + child.getMeasuredHeight() / 2);
            heroRight += child.getMeasuredWidth();
        }
    }

    /**
     * Create a thumb view, it's by default a ImageView.
     */
    protected View createThumbView(ViewGroup parent) {
        return new ImageView(parent.getContext());
    }

    /**
     * Clear all thumb bitmaps set on thumb views.
     */
    public void clearThumbBitmaps() {
        for (int i = 0; i < getChildCount(); i++) {
            setThumbBitmap(i, null);
        }
        mBitmaps.clear();
    }


    /**
     * Get bitmap of given child index.
     */
    public Bitmap getThumbBitmap(int index) {
        return mBitmaps.get(index);
    }

    /**
     * Set thumb bitmap for a given index of child.
     */
    public void setThumbBitmap(int index, Bitmap bitmap) {
        mBitmaps.put(index, bitmap);
        ((ImageView) getChildAt(index)).setImageBitmap(bitmap);
    }
}
