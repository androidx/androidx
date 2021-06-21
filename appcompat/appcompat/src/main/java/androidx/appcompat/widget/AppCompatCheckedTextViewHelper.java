/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.CheckedTextViewCompat;

/** @hide */
@RestrictTo(LIBRARY)
class AppCompatCheckedTextViewHelper {
    @NonNull
    private final CheckedTextView mView;

    private ColorStateList mCheckMarkTintList = null;
    private PorterDuff.Mode mCheckMarkTintMode = null;
    private boolean mHasCheckMarkTint = false;
    private boolean mHasCheckMarkTintMode = false;

    private boolean mSkipNextApply;

    AppCompatCheckedTextViewHelper(@NonNull CheckedTextView view) {
        mView = view;
    }

    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a =
                TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                        R.styleable.CheckedTextView, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(mView, mView.getContext(),
                R.styleable.CheckedTextView, attrs, a.getWrappedTypeArray(), defStyleAttr, 0);
        try {
            boolean checkMarkDrawableLoaded = false;
            if (a.hasValue(R.styleable.CheckedTextView_checkMarkCompat)) {
                final int resourceId = a.getResourceId(R.styleable.CheckedTextView_checkMarkCompat,
                        0);
                if (resourceId != 0) {
                    try {
                        mView.setCheckMarkDrawable(
                                AppCompatResources.getDrawable(mView.getContext(), resourceId));
                        checkMarkDrawableLoaded = true;
                    } catch (Resources.NotFoundException ignore) {
                        // Animated checkMarkCompat relies on AAPT2 features. If not found then
                        // swallow this error and fall back to the regular drawable.
                    }
                }
            }
            if (!checkMarkDrawableLoaded && a.hasValue(
                    R.styleable.CheckedTextView_android_checkMark)) {
                final int resourceId = a.getResourceId(
                        R.styleable.CheckedTextView_android_checkMark, 0);
                if (resourceId != 0) {
                    mView.setCheckMarkDrawable(
                            AppCompatResources.getDrawable(mView.getContext(), resourceId));
                }
            }
            if (a.hasValue(R.styleable.CheckedTextView_checkMarkTint)) {
                CheckedTextViewCompat.setCheckMarkTintList(mView,
                        a.getColorStateList(R.styleable.CheckedTextView_checkMarkTint));
            }
            if (a.hasValue(R.styleable.CheckedTextView_checkMarkTintMode)) {
                CheckedTextViewCompat.setCheckMarkTintMode(mView,
                        DrawableUtils.parseTintMode(
                                a.getInt(R.styleable.CheckedTextView_checkMarkTintMode, -1),
                                null));
            }
        } finally {
            a.recycle();
        }
    }

    void setSupportCheckMarkTintList(ColorStateList tint) {
        mCheckMarkTintList = tint;
        mHasCheckMarkTint = true;

        applyCheckMarkTint();
    }

    ColorStateList getSupportCheckMarkTintList() {
        return mCheckMarkTintList;
    }

    void setSupportCheckMarkTintMode(@Nullable PorterDuff.Mode tintMode) {
        mCheckMarkTintMode = tintMode;
        mHasCheckMarkTintMode = true;

        applyCheckMarkTint();
    }

    PorterDuff.Mode getSupportCheckMarkTintMode() {
        return mCheckMarkTintMode;
    }

    void onSetCheckMarkDrawable() {
        if (mSkipNextApply) {
            mSkipNextApply = false;
            return;
        }

        mSkipNextApply = true;
        applyCheckMarkTint();
    }

    void applyCheckMarkTint() {
        Drawable checkMarkDrawable = CheckedTextViewCompat.getCheckMarkDrawable(mView);

        if (checkMarkDrawable != null && (mHasCheckMarkTint || mHasCheckMarkTintMode)) {
            checkMarkDrawable = DrawableCompat.wrap(checkMarkDrawable);
            checkMarkDrawable = checkMarkDrawable.mutate();
            if (mHasCheckMarkTint) {
                DrawableCompat.setTintList(checkMarkDrawable, mCheckMarkTintList);
            }
            if (mHasCheckMarkTintMode) {
                DrawableCompat.setTintMode(checkMarkDrawable, mCheckMarkTintMode);
            }
            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (checkMarkDrawable.isStateful()) {
                checkMarkDrawable.setState(mView.getDrawableState());
            }
            mView.setCheckMarkDrawable(checkMarkDrawable);
        }
    }
}
