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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class AppCompatImageHelper {
    private final ImageView mView;

    private TintInfo mInternalImageTint;
    private TintInfo mImageTint;
    private TintInfo mTmpInfo;

    public AppCompatImageHelper(ImageView view) {
        mView = view;
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                R.styleable.AppCompatImageView, defStyleAttr, 0);
        try {
            Drawable drawable = mView.getDrawable();
            if (drawable == null) {
                // If the view doesn't already have a drawable (from android:src), try loading
                // it from srcCompat
                final int id = a.getResourceId(R.styleable.AppCompatImageView_srcCompat, -1);
                if (id != -1) {
                    drawable = AppCompatResources.getDrawable(mView.getContext(), id);
                    if (drawable != null) {
                        mView.setImageDrawable(drawable);
                    }
                }
            }

            if (drawable != null) {
                DrawableUtils.fixDrawable(drawable);
            }

            if (a.hasValue(R.styleable.AppCompatImageView_tint)) {
                ImageViewCompat.setImageTintList(mView,
                        a.getColorStateList(R.styleable.AppCompatImageView_tint));
            }
            if (a.hasValue(R.styleable.AppCompatImageView_tintMode)) {
                ImageViewCompat.setImageTintMode(mView,
                        DrawableUtils.parseTintMode(
                                a.getInt(R.styleable.AppCompatImageView_tintMode, -1), null));
            }
        } finally {
            a.recycle();
        }
    }

    public void setImageResource(int resId) {
        if (resId != 0) {
            final Drawable d = AppCompatResources.getDrawable(mView.getContext(), resId);
            if (d != null) {
                DrawableUtils.fixDrawable(d);
            }
            mView.setImageDrawable(d);
        } else {
            mView.setImageDrawable(null);
        }

        applySupportImageTint();
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

    void setSupportImageTintList(ColorStateList tint) {
        if (mImageTint == null) {
            mImageTint = new TintInfo();
        }
        mImageTint.mTintList = tint;
        mImageTint.mHasTintList = true;
        applySupportImageTint();
    }

    ColorStateList getSupportImageTintList() {
        return mImageTint != null ? mImageTint.mTintList : null;
    }

    void setSupportImageTintMode(PorterDuff.Mode tintMode) {
        if (mImageTint == null) {
            mImageTint = new TintInfo();
        }
        mImageTint.mTintMode = tintMode;
        mImageTint.mHasTintMode = true;

        applySupportImageTint();
    }

    PorterDuff.Mode getSupportImageTintMode() {
        return mImageTint != null ? mImageTint.mTintMode : null;
    }

    void applySupportImageTint() {
        final Drawable imageViewDrawable = mView.getDrawable();
        if (imageViewDrawable != null) {
            DrawableUtils.fixDrawable(imageViewDrawable);
        }

        if (imageViewDrawable != null) {
            if (shouldApplyFrameworkTintUsingColorFilter()
                    && applyFrameworkTintUsingColorFilter(imageViewDrawable)) {
                // This needs to be called before the internal tints below so it takes
                // effect on any widgets using the compat tint on API 21
                return;
            }

            if (mImageTint != null) {
                AppCompatDrawableManager.tintDrawable(imageViewDrawable, mImageTint,
                        mView.getDrawableState());
            } else if (mInternalImageTint != null) {
                AppCompatDrawableManager.tintDrawable(imageViewDrawable, mInternalImageTint,
                        mView.getDrawableState());
            }
        }
    }

    void setInternalImageTint(ColorStateList tint) {
        if (tint != null) {
            if (mInternalImageTint == null) {
                mInternalImageTint = new TintInfo();
            }
            mInternalImageTint.mTintList = tint;
            mInternalImageTint.mHasTintList = true;
        } else {
            mInternalImageTint = null;
        }
        applySupportImageTint();
    }

    private boolean shouldApplyFrameworkTintUsingColorFilter() {
        final int sdk = Build.VERSION.SDK_INT;
        if (sdk > 21) {
            // On API 22+, if we're using an internal compat image source tint, we're also
            // responsible for applying any custom tint set via the framework impl
            return mInternalImageTint != null;
        } else if (sdk == 21) {
            // GradientDrawable doesn't implement setTintList on API 21, and since there is
            // no nice way to unwrap DrawableContainers we have to blanket apply this
            // on API 21
            return true;
        } else {
            // API 19 and below doesn't have framework tint
            return false;
        }
    }

    /**
     * Applies the framework image source tint to a view, but using the compat method (ColorFilter)
     *
     * @return true if a tint was applied
     */
    private boolean applyFrameworkTintUsingColorFilter(@NonNull Drawable imageSource) {
        if (mTmpInfo == null) {
            mTmpInfo = new TintInfo();
        }
        final TintInfo info = mTmpInfo;
        info.clear();

        final ColorStateList tintList = ImageViewCompat.getImageTintList(mView);
        if (tintList != null) {
            info.mHasTintList = true;
            info.mTintList = tintList;
        }
        final PorterDuff.Mode mode = ImageViewCompat.getImageTintMode(mView);
        if (mode != null) {
            info.mHasTintMode = true;
            info.mTintMode = mode;
        }

        if (info.mHasTintList || info.mHasTintMode) {
            AppCompatDrawableManager.tintDrawable(imageSource, info, mView.getDrawableState());
            return true;
        }

        return false;
    }
}
