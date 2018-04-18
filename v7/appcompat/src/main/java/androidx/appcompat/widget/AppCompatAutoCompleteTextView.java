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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AutoCompleteTextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.TintableBackgroundView;

/**
 * A {@link AutoCompleteTextView} which supports compatible features on older versions of the
 * platform, including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link androidx.core.view.ViewCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#backgroundTint} and
 *     {@link R.attr#backgroundTintMode}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link AutoCompleteTextView} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatAutoCompleteTextView extends AutoCompleteTextView implements
        TintableBackgroundView {

    private static final int[] TINT_ATTRS = {
            android.R.attr.popupBackground
    };

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;

    public AppCompatAutoCompleteTextView(Context context) {
        this(context, null);
    }

    public AppCompatAutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.autoCompleteTextViewStyle);
    }

    public AppCompatAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                TINT_ATTRS, defStyleAttr, 0);
        if (a.hasValue(0)) {
            setDropDownBackgroundDrawable(a.getDrawable(0));
        }
        a.recycle();

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        mTextHelper.applyCompoundDrawablesTints();
    }

    @Override
    public void setDropDownBackgroundResource(@DrawableRes int resId) {
        setDropDownBackgroundDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintList(tint);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySupportBackgroundTint();
        }
        if (mTextHelper != null) {
            mTextHelper.applyCompoundDrawablesTints();
        }
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return AppCompatHintHelper.onCreateInputConnection(super.onCreateInputConnection(outAttrs),
                outAttrs, this);
    }
}
