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

package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.widget.CheckBox;

/**
 * A {@link CheckBox} which supports compatible features on older version of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of it background via the background tint methods in
 *     {@link android.support.v4.widget.CompoundButtonCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#buttonTint} and
 *     {@link R.attr#buttonTintMode}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link CheckBox} in your layouts.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatCheckBox extends CheckBox implements TintableCompoundButton {

    private AppCompatDrawableManager mDrawableManager;
    private AppCompatCompoundButtonHelper mCompoundButtonHelper;

    public AppCompatCheckBox(Context context) {
        this(context, null);
    }

    public AppCompatCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkboxStyle);
    }

    public AppCompatCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);
        mDrawableManager = AppCompatDrawableManager.get();
        mCompoundButtonHelper = new AppCompatCompoundButtonHelper(this, mDrawableManager);
        mCompoundButtonHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    @Override
    public void setButtonDrawable(Drawable buttonDrawable) {
        super.setButtonDrawable(buttonDrawable);
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.onSetButtonDrawable();
        }
    }

    @Override
    public void setButtonDrawable(@DrawableRes int resId) {
        setButtonDrawable(mDrawableManager != null
                ? mDrawableManager.getDrawable(getContext(), resId)
                : ContextCompat.getDrawable(getContext(), resId));
    }

    @Override
    public int getCompoundPaddingLeft() {
        final int value = super.getCompoundPaddingLeft();
        return mCompoundButtonHelper != null
                ? mCompoundButtonHelper.getCompoundPaddingLeft(value)
                : value;
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * @hide
     */
    @Override
    public void setSupportButtonTintList(@Nullable ColorStateList tint) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintList(tint);
        }
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * @hide
     */
    @Nullable
    @Override
    public ColorStateList getSupportButtonTintList() {
        return mCompoundButtonHelper != null
                ? mCompoundButtonHelper.getSupportButtonTintList()
                : null;
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * @hide
     */
    @Override
    public void setSupportButtonTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintMode(tintMode);
        }
    }

    /**
     * This should be accessed from {@link android.support.v4.widget.CompoundButtonCompat}
     * @hide
     */
    @Nullable
    @Override
    public PorterDuff.Mode getSupportButtonTintMode() {
        return mCompoundButtonHelper != null
                ? mCompoundButtonHelper.getSupportButtonTintMode()
                : null;
    }
}
