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
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.appcompat.R;
import android.support.v7.graphics.drawable.DrawableUtils;
import android.util.AttributeSet;
import android.widget.CompoundButton;

class AppCompatCompoundButtonHelper {

    private final CompoundButton mView;
    private final AppCompatDrawableManager mDrawableManager;

    private ColorStateList mButtonTintList = null;
    private PorterDuff.Mode mButtonTintMode = null;
    private boolean mHasButtonTint = false;
    private boolean mHasButtonTintMode = false;

    private boolean mSkipNextApply;

    /**
     * Interface which allows us to directly set a button, bypass any calls back to ourselves.
     */
    interface DirectSetButtonDrawableInterface {
        void setButtonDrawable(Drawable buttonDrawable);
    }

    AppCompatCompoundButtonHelper(CompoundButton view, AppCompatDrawableManager drawableManager) {
        mView = view;
        mDrawableManager = drawableManager;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = mView.getContext().obtainStyledAttributes(attrs, R.styleable.CompoundButton,
                defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.CompoundButton_android_button)) {
                final int resourceId = a.getResourceId(
                        R.styleable.CompoundButton_android_button, 0);
                if (resourceId != 0) {
                    mView.setButtonDrawable(
                            mDrawableManager.getDrawable(mView.getContext(), resourceId));
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

    int getCompoundPaddingLeft(int superValue) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Before JB-MR1 the button drawable wasn't taken into account for padding. We'll
            // workaround that here
            Drawable buttonDrawable = CompoundButtonCompat.getButtonDrawable(mView);
            if (buttonDrawable != null) {
                superValue += buttonDrawable.getIntrinsicWidth();
            }
        }
        return superValue;
    }
}
