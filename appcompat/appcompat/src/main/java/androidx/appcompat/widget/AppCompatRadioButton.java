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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.TintableBackgroundView;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TintableCompoundButton;
import androidx.core.widget.TintableCompoundDrawablesView;
import androidx.resourceinspection.annotation.AppCompatShadowedAttributes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link RadioButton} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 *     <li>Allows dynamic tint of its background via the background tint methods in
 *     {@link androidx.core.widget.CompoundButtonCompat}.</li>
 *     <li>Allows setting of the background tint using {@link R.attr#buttonTint} and
 *     {@link R.attr#buttonTintMode}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link RadioButton} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
@AppCompatShadowedAttributes
public class AppCompatRadioButton extends RadioButton implements TintableCompoundButton,
        TintableBackgroundView, EmojiCompatConfigurationView, TintableCompoundDrawablesView {

    private final AppCompatCompoundButtonHelper mCompoundButtonHelper;
    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    private AppCompatEmojiTextHelper mAppCompatEmojiTextHelper;

    public AppCompatRadioButton(Context context) {
        this(context, null);
    }

    public AppCompatRadioButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.radioButtonStyle);
    }

    public AppCompatRadioButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

        mCompoundButtonHelper = new AppCompatCompoundButtonHelper(this);
        mCompoundButtonHelper.loadFromAttributes(attrs, defStyleAttr);

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        AppCompatEmojiTextHelper emojiTextViewHelper = getEmojiTextViewHelper();
        emojiTextViewHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    /**
     * This may be called from super constructors.
     */
    private @NonNull AppCompatEmojiTextHelper getEmojiTextViewHelper() {
        if (mAppCompatEmojiTextHelper == null) {
            mAppCompatEmojiTextHelper = new AppCompatEmojiTextHelper(this);
        }
        return mAppCompatEmojiTextHelper;
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
        setButtonDrawable(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * This should be accessed from {@link androidx.core.widget.CompoundButtonCompat}
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportButtonTintList(@Nullable ColorStateList tint) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintList(tint);
        }
    }

    /**
     * This should be accessed from {@link androidx.core.widget.CompoundButtonCompat}
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public @Nullable ColorStateList getSupportButtonTintList() {
        return mCompoundButtonHelper != null
                ? mCompoundButtonHelper.getSupportButtonTintList()
                : null;
    }

    /**
     * This should be accessed from {@link androidx.core.widget.CompoundButtonCompat}
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportButtonTintMode(PorterDuff.@Nullable Mode tintMode) {
        if (mCompoundButtonHelper != null) {
            mCompoundButtonHelper.setSupportButtonTintMode(tintMode);
        }
    }

    /**
     * This should be accessed from {@link androidx.core.widget.CompoundButtonCompat}
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public PorterDuff.@Nullable Mode getSupportButtonTintMode() {
        return mCompoundButtonHelper != null
                ? mCompoundButtonHelper.getSupportButtonTintMode()
                : null;
    }

    /**
     * This should be accessed via
     * {@link ViewCompat#setBackgroundTintList(View, ColorStateList)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
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
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public @Nullable ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link ViewCompat#setBackgroundTintMode(View, PorterDuff.Mode)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setSupportBackgroundTintMode(PorterDuff.@Nullable Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public PorterDuff.@Nullable Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    public void setBackgroundDrawable(@Nullable Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
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
    public void setFilters(@SuppressWarnings("ArrayReturn") InputFilter @NonNull [] filters) {
        super.setFilters(getEmojiTextViewHelper().getFilters(filters));
    }

    @Override
    public void setAllCaps(boolean allCaps) {
        super.setAllCaps(allCaps);
        getEmojiTextViewHelper().setAllCaps(allCaps);
    }

    @Override
    public void setEmojiCompatEnabled(boolean enabled) {
        getEmojiTextViewHelper().setEnabled(enabled);
    }

    @Override
    public boolean isEmojiCompatEnabled() {
        return getEmojiTextViewHelper().isEnabled();
    }

    @Override
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
            @Nullable Drawable right, @Nullable Drawable bottom) {
        super.setCompoundDrawables(left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    @Override
    public void setCompoundDrawablesRelative(@Nullable Drawable start, @Nullable Drawable top,
            @Nullable Drawable end, @Nullable Drawable bottom) {
        super.setCompoundDrawablesRelative(start, top, end, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getCompoundDrawableTintList(TextView)}
     *
     * @return the tint applied to the compound drawables
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTint
     * @see #setSupportCompoundDrawablesTintList(ColorStateList)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @Nullable ColorStateList getSupportCompoundDrawablesTintList() {
        return mTextHelper.getCompoundDrawableTintList();
    }

    /**
     * This should be accessed via {@link
     * androidx.core.widget.TextViewCompat#setCompoundDrawableTintList(TextView, ColorStateList)}
     *
     * Applies a tint to the compound drawables. Does not modify the current tint mode, which is
     * {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setCompoundDrawables(Drawable, Drawable, Drawable, Drawable)} and
     * related methods will automatically mutate the drawables and apply the specified tint and tint
     * mode using {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tintList the tint to apply, may be {@code null} to clear tint
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTint
     * @see #getSupportCompoundDrawablesTintList()
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setSupportCompoundDrawablesTintList(@Nullable ColorStateList tintList) {
        mTextHelper.setCompoundDrawableTintList(tintList);
        mTextHelper.applyCompoundDrawablesTints();
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getCompoundDrawableTintMode(TextView)}
     *
     * Returns the blending mode used to apply the tint to the compound drawables, if specified.
     *
     * @return the blending mode used to apply the tint to the compound drawables
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTintMode
     * @see #setSupportCompoundDrawablesTintMode(PorterDuff.Mode)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public PorterDuff.@Nullable Mode getSupportCompoundDrawablesTintMode() {
        return mTextHelper.getCompoundDrawableTintMode();
    }

    /**
     * This should be accessed via {@link
     * androidx.core.widget.TextViewCompat#setCompoundDrawableTintMode(TextView, PorterDuff.Mode)}
     *
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setSupportCompoundDrawablesTintList(ColorStateList)} to the compound drawables. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be {@code null} to clear tint
     * @attr ref androidx.appcompat.R.styleable#AppCompatTextView_drawableTintMode
     * @see #setSupportCompoundDrawablesTintList(ColorStateList)
     *
     */
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setSupportCompoundDrawablesTintMode(PorterDuff.@Nullable Mode tintMode) {
        mTextHelper.setCompoundDrawableTintMode(tintMode);
        mTextHelper.applyCompoundDrawablesTints();
    }
}
