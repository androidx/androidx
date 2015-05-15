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

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.graphics.drawable.DrawableUtils;
import android.support.v7.internal.widget.TintInfo;
import android.support.v7.internal.widget.TintManager;
import android.util.AttributeSet;
import android.view.View;

class AppCompatBackgroundHelper {

    private final View mView;
    private final TintManager mTintManager;

    private TintInfo mInternalBackgroundTint;
    private TintInfo mBackgroundTint;

    AppCompatBackgroundHelper(View view, TintManager tintManager) {
        mView = view;
        mTintManager = tintManager;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = mView.getContext().obtainStyledAttributes(attrs,
                R.styleable.ViewBackgroundHelper, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.ViewBackgroundHelper_android_background)) {
                ColorStateList tint = mTintManager.getTintList(
                        a.getResourceId(R.styleable.ViewBackgroundHelper_android_background, -1));
                if (tint != null) {
                    setInternalBackgroundTint(tint);
                }
            }
            if (a.hasValue(R.styleable.ViewBackgroundHelper_backgroundTint)) {
                ViewCompat.setBackgroundTintList(mView,
                        a.getColorStateList(R.styleable.ViewBackgroundHelper_backgroundTint));
            }
            if (a.hasValue(R.styleable.ViewBackgroundHelper_backgroundTintMode)) {
                ViewCompat.setBackgroundTintMode(mView,
                        DrawableUtils.parseTintMode(
                                a.getInt(R.styleable.ViewBackgroundHelper_backgroundTintMode, -1),
                                null));
            }
        } finally {
            a.recycle();
        }
    }

    void onSetBackgroundResource(int resId) {
        // Update the default background tint
        setInternalBackgroundTint(mTintManager != null ? mTintManager.getTintList(resId) : null);
    }

    void onSetBackgroundDrawable(Drawable background) {
        // We don't know that this drawable is, so we need to clear the default background tint
        setInternalBackgroundTint(null);
    }

    void setSupportBackgroundTintList(ColorStateList tint) {
        if (mBackgroundTint == null) {
            mBackgroundTint = new TintInfo();
        }
        mBackgroundTint.mTintList = tint;
        mBackgroundTint.mHasTintList = true;

        applySupportBackgroundTint();
    }

    ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTint != null ? mBackgroundTint.mTintList : null;
    }

    void setSupportBackgroundTintMode(PorterDuff.Mode tintMode) {
        if (mBackgroundTint == null) {
            mBackgroundTint = new TintInfo();
        }
        mBackgroundTint.mTintMode = tintMode;
        mBackgroundTint.mHasTintMode = true;

        applySupportBackgroundTint();
    }

    PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTint != null ? mBackgroundTint.mTintMode : null;
    }

    void applySupportBackgroundTint() {
        if (mView.getBackground() != null) {
            if (mBackgroundTint != null) {
                TintManager.tintViewBackground(mView, mBackgroundTint);
            } else if (mInternalBackgroundTint != null) {
                TintManager.tintViewBackground(mView, mInternalBackgroundTint);
            }
        }
    }

    void setInternalBackgroundTint(ColorStateList tint) {
        if (tint != null) {
            if (mInternalBackgroundTint == null) {
                mInternalBackgroundTint = new TintInfo();
            }
            mInternalBackgroundTint.mTintList = tint;
            mInternalBackgroundTint.mHasTintList = true;
        } else {
            mInternalBackgroundTint = null;
        }
        applySupportBackgroundTint();
    }
}
