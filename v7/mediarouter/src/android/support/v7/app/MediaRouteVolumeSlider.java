/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.mediarouter.R;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

/**
 * Volume slider with showing, hiding, and applying alpha supports to the thumb.
 */
class MediaRouteVolumeSlider extends AppCompatSeekBar {
    private boolean mShowThumb = true;
    private Drawable mThumb;
    private float mDisabledAlpha;

    public MediaRouteVolumeSlider(Context context) {
        super(context, null);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThumb = ContextCompat.getDrawable(context, R.drawable.mr_seekbar_thumb);
        setThumb(mThumb);
        int color = MediaRouterThemeHelper.getControllerColor(context);
        ColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        getProgressDrawable().setColorFilter(colorFilter);
        mThumb.setColorFilter(colorFilter);
        TypedArray ta = context.obtainStyledAttributes(
                attrs, new int[] {android.R.attr.disabledAlpha}, defStyleAttr, 0);
        mDisabledAlpha = ta.getFloat(0, 0.5f);
        ta.recycle();
    }

    /**
     * Sets whether to show/hide thumb.
     */
    public void setShowThumb(boolean showThumb) {
        if (mShowThumb == showThumb) {
            return;
        }
        mShowThumb = showThumb;
        setThumb(mShowThumb ? mThumb : null);
    }

    @Override
    protected void drawableStateChanged() {
        mThumb.setAlpha(isEnabled() ? 0xFF : (int)(0xFF * mDisabledAlpha));
        super.drawableStateChanged();
    }
}
