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

package android.support.v7.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.widget.RatingBar;

/**
 * A {@link RatingBar} which supports compatible features on older version of the platform.
 *
 * <p>This will automatically be used when you use {@link RatingBar} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatRatingBar extends RatingBar {

    private AppCompatProgressBarHelper mAppCompatProgressBarHelper;
    private AppCompatDrawableManager mDrawableManager;

    public AppCompatRatingBar(Context context) {
        this(context, null);
    }

    public AppCompatRatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.ratingBarStyle);
    }

    public AppCompatRatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDrawableManager = AppCompatDrawableManager.get();

        mAppCompatProgressBarHelper = new AppCompatProgressBarHelper(this, mDrawableManager);
        mAppCompatProgressBarHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Bitmap sampleTile = mAppCompatProgressBarHelper.getSampleTime();
        if (sampleTile != null) {
            final int width = sampleTile.getWidth() * getNumStars();
            setMeasuredDimension(ViewCompat.resolveSizeAndState(width, widthMeasureSpec, 0),
                    getMeasuredHeight());
        }
    }

}
