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

package android.support.v7.widget;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @hide
 */
public class AppCompatImageHelper {

    private final ImageView mView;
    private final AppCompatDrawableManager mDrawableManager;

    public AppCompatImageHelper(ImageView view, AppCompatDrawableManager drawableManager) {
        mView = view;
        mDrawableManager = drawableManager;
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a = null;
        try {
            Drawable drawable = mView.getDrawable();

            if (drawable == null) {
                a = TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                        R.styleable.AppCompatImageView, defStyleAttr, 0);

                // If the view doesn't already have a drawable (from android:src), try loading
                // it from srcCompat
                final int id = a.getResourceId(R.styleable.AppCompatImageView_srcCompat, -1);
                if (id != -1) {
                    drawable = mDrawableManager.getDrawable(mView.getContext(), id);
                    if (drawable != null) {
                        mView.setImageDrawable(drawable);
                    }
                }
            }

            if (drawable != null) {
                DrawableUtils.fixDrawable(drawable);
            }
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public void setImageResource(int resId) {
        if (resId != 0) {
            final Drawable d = mDrawableManager != null
                    ? mDrawableManager.getDrawable(mView.getContext(), resId)
                    : ContextCompat.getDrawable(mView.getContext(), resId);
            if (d != null) {
                DrawableUtils.fixDrawable(d);
            }
            mView.setImageDrawable(d);
        } else {
            mView.setImageDrawable(null);
        }
    }

    boolean hasOverlappingRendering() {
        final Drawable background = mView.getBackground();
        if (Build.VERSION.SDK_INT >= 21
                && background instanceof android.graphics.drawable.RippleDrawable) {
            // RippleDrawable has an issue on L+ when used with an alpha animation.
            // This workaround should be disabled when the platform bug is fixed. See b/27715789
            return false;
        }
        return true;
    }
}
