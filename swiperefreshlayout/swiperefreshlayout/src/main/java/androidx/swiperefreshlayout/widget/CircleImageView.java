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

package androidx.swiperefreshlayout.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.R;

/**
 * Private class created to work around issues with AnimationListeners being
 * called before the animation is actually complete and support shadows on older
 * platforms.
 */
class CircleImageView extends ImageView {
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFAFAFA;

    // PX
    private static final int SHADOW_ELEVATION = 4;

    private Animation.AnimationListener mListener;
    private int mBackgroundColor;

    CircleImageView(Context context) {
        super(context);

        final float density = getContext().getResources().getDisplayMetrics().density;

        // The style attribute is named SwipeRefreshLayout instead of CircleImageView because
        // CircleImageView is not part of the public api.
        @SuppressLint("CustomViewStyleable")
        TypedArray colorArray = getContext().obtainStyledAttributes(R.styleable.SwipeRefreshLayout);
        mBackgroundColor = colorArray.getColor(
                R.styleable.SwipeRefreshLayout_swipeRefreshLayoutProgressSpinnerBackgroundColor,
                DEFAULT_BACKGROUND_COLOR);
        colorArray.recycle();

        ShapeDrawable circle;
        circle = new ShapeDrawable(new OvalShape());
        ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
        circle.getPaint().setColor(mBackgroundColor);
        setBackground(circle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        if (getBackground() instanceof ShapeDrawable) {
            ((ShapeDrawable) getBackground()).getPaint().setColor(color);
            mBackgroundColor = color;
        }
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }
}
