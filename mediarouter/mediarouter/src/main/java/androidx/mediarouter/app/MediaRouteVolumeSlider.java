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

package androidx.mediarouter.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * Volume slider with showing, hiding, and applying alpha supports to the thumb.
 */
class MediaRouteVolumeSlider extends AppCompatSeekBar {
    private static final String TAG = "MediaRouteVolumeSlider";
    private final float mDisabledAlpha;

    private boolean mHideThumb;
    private Drawable mThumb;
    private int mProgressAndThumbColor;
    private int mBackgroundColor;

    public MediaRouteVolumeSlider(Context context) {
        this(context, null);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.seekBarStyle);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(context);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int alpha = isEnabled() ? 0xFF : (int) (0xFF * mDisabledAlpha);

        // The thumb drawable is a collection of drawables and its current drawables are changed per
        // state. Apply the color filter and alpha on every state change.
        mThumb.setColorFilter(mProgressAndThumbColor, PorterDuff.Mode.SRC_IN);
        mThumb.setAlpha(alpha);

        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) getProgressDrawable();
            progressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
            Drawable backgroundDrawable = ld.findDrawableByLayerId(android.R.id.background);

            backgroundDrawable.setColorFilter(mBackgroundColor, PorterDuff.Mode.SRC_IN);
        }
        progressDrawable.setColorFilter(mProgressAndThumbColor, PorterDuff.Mode.SRC_IN);
        progressDrawable.setAlpha(alpha);
    }

    @Override
    public void setThumb(Drawable thumb) {
        mThumb = thumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }

    /**
     * Sets whether to show or hide thumb.
     */
    public void setHideThumb(boolean hideThumb) {
        if (mHideThumb == hideThumb) {
            return;
        }
        mHideThumb = hideThumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }

    public void setColor(int color) {
        setColor(color, color);
    }

    /**
     * Sets the color of progress/thumb and background of progress bar. The change takes effect
     * next time drawable state is changed.
     * <p>
     * The color cannot be translucent, otherwise the underlying progress bar will be seen through
     * the thumb.
     * </p>
     */
    public void setColor(int progressAndThumbColor, int backgroundColor) {
        // Sets the color of progress part of progress bar and thumb.
        if (mProgressAndThumbColor != progressAndThumbColor) {
            if (Color.alpha(progressAndThumbColor) != 0xFF) {
                Log.e(TAG, "Volume slider progress and thumb color cannot be translucent: #"
                        + Integer.toHexString(progressAndThumbColor));
            }
            mProgressAndThumbColor = progressAndThumbColor;
        }

        // Sets the color of background part of progress bar.
        if (mBackgroundColor != backgroundColor) {
            if (Color.alpha(backgroundColor) != 0xFF) {
                Log.e(TAG, "Volume slider background color cannot be translucent: #"
                        + Integer.toHexString(backgroundColor));
            }
            mBackgroundColor = backgroundColor;
        }
    }
}
