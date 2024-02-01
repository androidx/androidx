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

package androidx.appcompat.widget;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.CompoundButtonCompat;

class AppCompatCompoundButtonHelper {
    @NonNull
    private final CompoundButton mView;

    private ColorStateList mButtonTintList = null;
    private PorterDuff.Mode mButtonTintMode = null;
    private boolean mHasButtonTint = false;
    private boolean mHasButtonTintMode = false;

    private boolean mSkipNextApply;

    AppCompatCompoundButtonHelper(@NonNull CompoundButton view) {
        mView = view;
    }

    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a =
                TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                        R.styleable.CompoundButton, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(mView, mView.getContext(),
                R.styleable.CompoundButton, attrs, a.getWrappedTypeArray(), defStyleAttr, 0);
        try {
            boolean buttonDrawableLoaded = false;
            if (a.hasValue(R.styleable.CompoundButton_buttonCompat)) {
                final int resourceId = a.getResourceId(R.styleable.CompoundButton_buttonCompat, 0);
                if (resourceId != 0) {
                    try {
                        mView.setButtonDrawable(
                                AppCompatResources.getDrawable(mView.getContext(), resourceId));
                        buttonDrawableLoaded = true;
                    } catch (Resources.NotFoundException nfe) {
                        // Animated buttonCompat relies on AAPT2 features. If not found then swallow
                        // this error and fall back to the regular drawable.
                    }
                }
            }
            if (!buttonDrawableLoaded && a.hasValue(R.styleable.CompoundButton_android_button)) {
                final int resourceId = a.getResourceId(
                        R.styleable.CompoundButton_android_button, 0);
                if (resourceId != 0) {
                    mView.setButtonDrawable(
                            AppCompatResources.getDrawable(mView.getContext(), resourceId));
                }
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTint)) {
                CompoundButtonCompat.setButtonTintList(mView,
                        a.getColorStateList(R.styleable.CompoundButton_buttonTint));
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTintMode)) {
                CompoundButtonCompat.setButtonTintMode(mView,
                        DrawableUtils.parseTintMode(
                                a.getInt(R.styleable.CompoundButton_buttonTintMode, -1),
                                null));
            }
        } finally {
            a.recycle();
        }
    }

    void setSupportButtonTintList(ColorStateList tint) {
        mButtonTintList = tint;
        mHasButtonTint = true;

        applyButtonTint();
    }

    ColorStateList getSupportButtonTintList() {
        return mButtonTintList;
    }

    void setSupportButtonTintMode(@Nullable PorterDuff.Mode tintMode) {
        mButtonTintMode = tintMode;
        mHasButtonTintMode = true;

        applyButtonTint();
    }

    PorterDuff.Mode getSupportButtonTintMode() {
        return mButtonTintMode;
    }

    void onSetButtonDrawable() {
        if (mSkipNextApply) {
            mSkipNextApply = false;
            return;
        }

        mSkipNextApply = true;
        applyButtonTint();
    }

    void applyButtonTint() {
        Drawable buttonDrawable = CompoundButtonCompat.getButtonDrawable(mView);

        if (buttonDrawable != null && (mHasButtonTint || mHasButtonTintMode)) {
            buttonDrawable = DrawableCompat.wrap(buttonDrawable);
            buttonDrawable = buttonDrawable.mutate();
            if (mHasButtonTint) {
                DrawableCompat.setTintList(buttonDrawable, mButtonTintList);
            }
            if (mHasButtonTintMode) {
                DrawableCompat.setTintMode(buttonDrawable, mButtonTintMode);
            }
            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (buttonDrawable.isStateful()) {
                buttonDrawable.setState(mView.getDrawableState());
            }
            mView.setButtonDrawable(buttonDrawable);
        }
    }

}
