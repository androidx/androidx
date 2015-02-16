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

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An tint aware {@link android.widget.ImageView}
 *
 * @hide
 */
public class TintImageView extends ImageView {

    private static final int[] TINT_ATTRS = {
            android.R.attr.background,
            android.R.attr.src
    };

    private final TintManager mTintManager;

    private ColorStateList mDrawableTintList = null;
    private PorterDuff.Mode mDrawableTintMode = null;
    private boolean mHasDrawableTint = false;
    private boolean mHasDrawableTintMode = false;

    public TintImageView(Context context) {
        this(context, null);
    }

    public TintImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs, TINT_ATTRS,
                defStyleAttr, 0);
        if (a.length() > 0) {
            if (a.hasValue(0)) {
                setBackgroundDrawable(a.getDrawable(0));
            }
            if (a.hasValue(1)) {
                setImageDrawable(a.getDrawable(1));
            }
        }
        a.recycle();

        // Keep the TintManager in case we need it later
        mTintManager = a.getTintManager();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        // Intercept this call and instead retrieve the Drawable via the tint manager
        setImageDrawable(mTintManager.getDrawable(resId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        applyImageTint();
    }

    public void setImageTintList(@Nullable ColorStateList tint) {
        if (Build.VERSION.SDK_INT >= 21) {
            super.setImageTintList(tint);
        } else {
            mDrawableTintList = tint;
            mHasDrawableTint = true;
            applyImageTint();
        }
    }

    public void setImageTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (Build.VERSION.SDK_INT >= 21) {
            super.setImageTintMode(tintMode);
        } else {
            mDrawableTintMode = tintMode;
            mHasDrawableTintMode = true;
            applyImageTint();
        }
    }

    private void applyImageTint() {
        Drawable drawable = getDrawable();
        if (drawable != null && (mHasDrawableTint || mHasDrawableTintMode)) {
            drawable = DrawableCompat.wrap(drawable.mutate());
            if (mHasDrawableTint) {
                DrawableCompat.setTintList(drawable, mDrawableTintList);
            }
            if (mHasDrawableTintMode) {
                DrawableCompat.setTintMode(drawable, mDrawableTintMode);
            }
            // Drawable may have changed, make sure we re-set
            super.setImageDrawable(drawable);
        }
    }

}
