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
package android.support.v17.leanback.app;

import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.BoundsRule;
import android.support.v17.leanback.graphics.CompositeDrawable;
import android.support.v17.leanback.graphics.FitWidthBitmapDrawable;
import android.support.v17.leanback.widget.Parallax;
import android.support.v17.leanback.widget.ParallaxRecyclerViewSource;
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
 * <li>First users should use {@link ParallaxBuilder} class to set the appropriate attributes
 * and call build() to create an instance of {@link DetailsBackgroundParallaxHelper}.
 * Users must set {@link DetailsParallaxManager} on {@link ParallaxBuilder} for it to obtain the
 * {@link Parallax} instance. Finally they should set the drawable obtained by calling
 * {@link #getDrawable} as the background of their current activity.
 * <pre>
 * {@code
 *     public void onStart() {
 *         super.onStart();
 *         mParallaxHelper = DetailsBackgroundParallaxHelper.ParallaxBuilder
 *             .newBuilder(parallaxManager, context)
 *             .setBitmapMinVerticalOffset(-300)
 *             .build();
 *          mBackgroundManager.setDrawable(mParallaxHelper.getDrawable());
 *      }
 * }
 * </pre>
 * </li>
 * </li>
 * <li>Finally, users can set the bitmap through {@link #setBitmap(Bitmap)} call.
 * <pre>
 * {@code
 *     public void onBitmapLoaded(Bitmap bitmap) {
 *         mParallaxHelper.setBitmap(bitmap);
 *     }
 * }
 * </pre>
 * </li>
 * </ul>
 *
 * In case the color is not set, it will use defaultBrandColorDark from LeanbackTheme.
 */
public final class DetailsBackgroundParallaxHelper {
    private DetailsParallaxManager mDetailsParallaxManager;
    private CompositeDrawable mCompositeDrawable;
    private FitWidthBitmapDrawable mFitWidthBitmapDrawable;
    private ColorDrawable mSolidColorDrawable;
    private int mBitmapMinVerticalOffset;

    private DetailsBackgroundParallaxHelper(
            DetailsParallaxManager detailsParallaxManager,
            int bitmapMinVerticalOffset,
            int color) {
        this.mBitmapMinVerticalOffset = bitmapMinVerticalOffset;
        mCompositeDrawable = new CompositeDrawable();
        mFitWidthBitmapDrawable = new FitWidthBitmapDrawable();
        mSolidColorDrawable = new ColorDrawable(color);
        mCompositeDrawable.addChildDrawable(mFitWidthBitmapDrawable);
        mCompositeDrawable.addChildDrawable(mSolidColorDrawable);
        mCompositeDrawable.getChildAt(1).getBoundsRule().mTop = BoundsRule.inheritFromParent(1f);
        mDetailsParallaxManager = detailsParallaxManager;
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
        private final DetailsParallaxManager mDetailsParallaxManager;
        private final Context mContext;

        /**
         * Returns an instance of itself.
         *
         * @param detailsParallaxManager class responsible for creating {@link Parallax} instance.
         * @param context Context used for loading resources.
         */
        public ParallaxBuilder(@NonNull Context context,
                               @NonNull DetailsParallaxManager detailsParallaxManager) {
            if (detailsParallaxManager == null || context == null) {
                throw new IllegalArgumentException("Must set DetailsParallaxManager and Context.");
            }
            this.mDetailsParallaxManager = detailsParallaxManager;
            this.mContext = context;
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
         * Builds and returns an instance of {@link DetailsBackgroundParallaxHelper}.
         */
        public DetailsBackgroundParallaxHelper build() {
            if (!mIsColorSet) {
                mColor = getDefaultBackgroundColor(mContext);
            }

            return new DetailsBackgroundParallaxHelper(
                    mDetailsParallaxManager, mBitmapMinVerticalOffset, mColor);
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

    /**
     * Sets up the cover image parallax effect in {@link DetailsFragment}.
     */
    private void setupParallaxEffect() {
        // Add bitmap parallax effect:
        // When frameTop moves from half of the screen to top of the screen,
        // change vertical offset of Bitmap from 0 to -100

        Parallax parallax = mDetailsParallaxManager.getParallax();
        ParallaxRecyclerViewSource.ChildPositionProperty frameTop =
                mDetailsParallaxManager.getFrameTop();
        ParallaxRecyclerViewSource.ChildPositionProperty frameBottom =
                mDetailsParallaxManager.getFrameBottom();
        parallax.addEffect(frameTop.atFraction(0.5f), frameTop.atFraction(0f))
                .target(mFitWidthBitmapDrawable,
                    PropertyValuesHolder.ofInt("verticalOffset", 0, mBitmapMinVerticalOffset))
                .target(mCompositeDrawable.getChildAt(0),
                    PropertyValuesHolder.ofFloat(
                        CompositeDrawable.ChildDrawable.BOTTOM_FRACTION, 0.5f, 0f));

        // Add solid color parallax effect:
        // When frameBottom moves from bottom of the screen to top of the screen,
        // change solid ColorDrawable's top from bottom of screen to top of the screen.
        parallax.addEffect(frameBottom.atFraction(1f), frameBottom.atFraction(0f))
                .target(mCompositeDrawable.getChildAt(1),
                        PropertyValuesHolder.ofFloat(
                                CompositeDrawable.ChildDrawable.TOP_FRACTION, 1f, 0f));
    }
}
