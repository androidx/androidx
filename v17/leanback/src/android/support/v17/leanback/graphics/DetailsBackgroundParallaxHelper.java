/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.graphics;

import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.Parallax;
import android.support.v17.leanback.widget.ParallaxRecyclerViewSource;
import android.support.v17.leanback.widget.ParallaxSource;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;

/**
 * Helper class responsible for wiring in parallax effect in
 * {@link android.support.v17.leanback.app.DetailsFragment}. The default effect will render
 * a drawable like the following -
 * <pre>
 *        ***************************
 *        *          Bitmap         *
 *        ***************************
 *        *    DetailsOverviewRow   *
 *        *                         *
 *        ***************************
 *        *        Solid Color      *
 *        *         Related         *
 *        *         Content         *
 *        ***************************
 * </pre>
 * As the user scrolls through the page, the bounds of the bitmap and related content section
 * will be updated to simulate the parallax effect. Users have to do the following to setup the
 * parallax -
 *
 * <ul>
 * <li>First users should use
 * {@link ParallaxBuilder} class to set the appropriate attributes and call build() to
 * create an instance of {@link DetailsBackgroundParallaxHelper}. Users must set the RecyclerView
 * instance on {@link ParallaxBuilder}. Then they should set the drawable obtained by
 * calling {@link #getDrawable} as the background of their current activity.
 * @<code>
 *     @Override
 *     public void onStart() {
 *         super.onStart();
 *         mParallaxHelper = DetailsBackgroundParallaxHelper.ParallaxBuilder
 *             .newBuilder()
 *             .setRecyclerView(getRowsFragment().getVerticalGridView())
 *             .setmBitmapMinVerticalOffset(-300)
 *             .build();
 *          mBackgroundManager.setDrawable(mParallaxHelper.getDrawable());
 *      }
 * </code>
 * </li>
 * </li>
 * <li>Finally, users can set the bitmap through {@link #setBitmap(Bitmap)} call.
 * @<code>
 *     public void onBitmapLoaded(Bitmap bitmap) {
 *         mParallaxHelper.setBitmap(bitmap);
 *     }
 * </code>
 * </li>
 * </ul>
 *
 * In case the color is not set, it will use defaultBrandColorDark from LeanbackTheme.
 */
public final class DetailsBackgroundParallaxHelper {
    private final RecyclerView mRecyclerView;
    private Parallax mParallax;
    private CompositeDrawable mCompositeDrawable;
    private FitWidthBitmapDrawable mFitWidthBitmapDrawable;
    private ColorDrawable mSolidColorDrawable;
    private int minTopCoverImage;

    private DetailsBackgroundParallaxHelper(
            RecyclerView recyclerView,
            int minTopCoverImage,
            int color) {
        this.mRecyclerView = recyclerView;
        this.minTopCoverImage = minTopCoverImage;
        mCompositeDrawable = new CompositeDrawable();
        mFitWidthBitmapDrawable = new FitWidthBitmapDrawable();
        mSolidColorDrawable = new ColorDrawable(color);
        mCompositeDrawable.addChildDrawable(mFitWidthBitmapDrawable);
        mCompositeDrawable.addChildDrawable(mSolidColorDrawable);
        mCompositeDrawable.getChildAt(1).getBoundsRule().mTop = BoundsRule.inheritFromParent(1f);
        setupParallaxEffect();
    }

    /**
     * Builder class used for creating an instance of {@link DetailsBackgroundParallaxHelper}.
     */
    public static class ParallaxBuilder {
        // Default value for image translation is -100px.
        private int mBitmapMinVerticalOffset = -100;
        private int mColor;
        private boolean mIsColorSet;
        private RecyclerView mRecyclerView;

        private ParallaxBuilder() {}

        /**
         * Returns an instance of itself.
         */
        public static ParallaxBuilder newBuilder() {
            return new ParallaxBuilder();
        }

        /**
         * Sets the minimum top position the image is going to translate to during the
         * parallax motion.
         */
        public ParallaxBuilder setBitmapMinVerticalOffset(int minTop) {
            this.mBitmapMinVerticalOffset = minTop;
            return this;
        }

        /**
         * Sets the color for the bottom section of the
         * {@link android.support.v17.leanback.app.DetailsFragment}.
         */
        public ParallaxBuilder setColor(int color) {
            this.mColor = color;
            mIsColorSet = true;
            return this;
        }

        /**
         * Sets the RecyclerView used in the
         * {@link android.support.v17.leanback.app.DetailsFragment}.
         */
        public ParallaxBuilder setRecyclerView(RecyclerView recyclerView) {
            this.mRecyclerView = recyclerView;
            return this;
        }

        /**
         * Builds and returns an instance of {@link DetailsBackgroundParallaxHelper}.
         */
        public DetailsBackgroundParallaxHelper build() {
            if (mRecyclerView == null) {
                throw new IllegalArgumentException("Must set RecyclerView!!!");
            }

            if (!mIsColorSet) {
                mColor = getDefaultBackgroundColor(mRecyclerView.getContext());
            }

            return new DetailsBackgroundParallaxHelper(
                    mRecyclerView, mBitmapMinVerticalOffset, mColor);
        }

        private int getDefaultBackgroundColor(Context context) {
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.defaultBrandColorDark, outValue, true)) {
                return context.getResources().getColor(outValue.resourceId);
            }
            return context.getResources().getColor(R.color.lb_default_brand_color_dark);
        }
    }

    /**
     * Returns the special drawable instance that is used to simulate the parallax effect. Users
     * must set this drawable as the background for their activity.
     */
    public Drawable getDrawable() {
        return mCompositeDrawable;
    }

    /**
     * Sets the bitmap in drawable instance returned during {@link #getDrawable()} call.
     */
    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Invalid bitmap");
        }
        mFitWidthBitmapDrawable.setBitmap(bitmap);
    }

    /**
     * Changes the background color of the related content section.
     */
    public void setColor(@ColorInt int color) {
        mSolidColorDrawable.setColor(color);
    }

    private void setupParallaxEffect() {
        ParallaxRecyclerViewSource parallaxSource = new ParallaxRecyclerViewSource(
                mRecyclerView);
        ParallaxSource.IntVariable frameTop = parallaxSource.addVariable(
                0 /* adapter pos */,
                R.id.details_frame /* view id */,
                0 /* offset */,
                0f /* fraction */);

        ParallaxSource.IntVariable frameBottom = parallaxSource.addVariable(
                0 /* adapter pos */,
                R.id.details_frame /* view id */,
                0 /* offset */,
                1.0f /* fraction */);

        mParallax = new Parallax();
        mParallax.setSource(parallaxSource);

        // Add bitmap effect.
        mParallax.addEffect(frameTop.at(0, 0.5f), frameTop.at(0, 0f))
                .target(mFitWidthBitmapDrawable,
                    PropertyValuesHolder.ofInt("verticalOffset", 0, minTopCoverImage))
                .target(mCompositeDrawable.getChildAt(0),
                    PropertyValuesHolder.ofFloat(
                        CompositeDrawable.ChildDrawable.BOTTOM_FRACTION, 0.5f, 0f));

        // Add solid color effect.
        mParallax.addEffect(frameBottom.at(0, 1f), frameBottom.at(0, 0f))
                .target(mCompositeDrawable.getChildAt(1),
                        PropertyValuesHolder.ofFloat(
                                CompositeDrawable.ChildDrawable.TOP_FRACTION, 1f, 0f));
    }
}

