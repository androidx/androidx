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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.mediarouter.R;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

/**
 * Volume slider with showing, hiding, and applying alpha supports to the thumb.
 */
class MediaRouteVolumeSlider extends AppCompatSeekBar {
    private boolean mHideThumb;
    private Drawable mThumb;
    private int mColor;
    private float mDisabledAlpha;

    public MediaRouteVolumeSlider(Context context) {
        super(context, null);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarStyle);
    }

    public MediaRouteVolumeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mColor = MediaRouterThemeHelper.getControllerColor(context);
        TypedArray ta = context.obtainStyledAttributes(
                attrs, new int[] {android.R.attr.disabledAlpha}, defStyleAttr, 0);
        mDisabledAlpha = ta.getFloat(0, 0.5f);
        ta.recycle();
    }

    @Override
    public void setThumb(Drawable thumb) {
        mThumb = thumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int alpha = isEnabled() ? 0xFF : (int)(mDisabledAlpha * 0xFF);
        getProgressDrawable().setAlpha(alpha);
        mThumb.setAlpha(alpha);
        // Thumb drawable is collections of drawables which is changed per state changes.
        // We need to manually apply color filters whenever the current drawable is changed.
        getProgressDrawable().setColorFilter(mColor, PorterDuff.Mode.SRC_IN);
        mThumb.setColorFilter(mColor, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Sets whether to show/hide thumb.
     */
    public void setHideThumb(boolean hideThumb) {
        if (mHideThumb == hideThumb) {
            return;
        }
        mHideThumb = hideThumb;
        super.setThumb(mHideThumb ? null : mThumb);
    }
}
