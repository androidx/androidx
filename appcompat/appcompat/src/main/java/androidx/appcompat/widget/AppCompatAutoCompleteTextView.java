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
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.TintableBackgroundView;
import androidx.core.widget.TextViewCompat;
import androidx.core.widget.TintableCompoundDrawablesView;
import androidx.resourceinspection.annotation.AppCompatShadowedAttributes;

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
@AppCompatShadowedAttributes
public class AppCompatAutoCompleteTextView extends AutoCompleteTextView implements
        TintableBackgroundView, EmojiCompatConfigurationView, TintableCompoundDrawablesView {

    private static final int[] TINT_ATTRS = {
            android.R.attr.popupBackground
    };

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    @NonNull
    private final AppCompatEmojiEditTextHelper mAppCompatEmojiEditTextHelper;

    public AppCompatAutoCompleteTextView(@NonNull Context context) {
        this(context, null);
    }

    public AppCompatAutoCompleteTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.autoCompleteTextViewStyle);
    }

    public AppCompatAutoCompleteTextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

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

        mAppCompatEmojiEditTextHelper = new AppCompatEmojiEditTextHelper(this);
        mAppCompatEmojiEditTextHelper.loadFromAttributes(attrs, defStyleAttr);
        initEmojiKeyListener(mAppCompatEmojiEditTextHelper);
    }

    /**
     * Call from the constructor to safely add KeyListener for emoji2.
     *
     * This will always call super methods to avoid leaking a partially constructed this to
     * overrides of non-final methods.
     *
     * @param appCompatEmojiEditTextHelper emojicompat helper
     */
    void initEmojiKeyListener(AppCompatEmojiEditTextHelper appCompatEmojiEditTextHelper) {
        // setKeyListener will cause a reset both focusable and the inputType to the most basic
        // style for the key listener. Since we're calling this from the View constructor, this
        // will cause both focusable and inputType to reset from the XML attributes.
        // See: b/191061070 and b/188049943 for details
        //
        // We will only reset this during ctor invocation, and default to the platform behavior
        // for later calls to setKeyListener, to emulate the exact behavior that a regular
        // EditText would provide.
        //
        // Since we're calling non-final methods from a ctor (setKeyListener, setRawInputType,
        // setFocusable) move this out of AppCompatEmojiEditTextHelper and into the respective
        // views to ensure we only call the super methods during construction (b/208480173).
        KeyListener currentKeyListener = getKeyListener();
        if (appCompatEmojiEditTextHelper.isEmojiCapableKeyListener(currentKeyListener)) {
            boolean wasFocusable = super.isFocusable();
            boolean wasClickable = super.isClickable();
            boolean wasLongClickable = super.isLongClickable();
            int inputType = super.getInputType();
            KeyListener wrappedKeyListener = appCompatEmojiEditTextHelper.getKeyListener(
                    currentKeyListener);
            // don't call parent setKeyListener if it's not wrapped
            if (wrappedKeyListener == currentKeyListener) return;
            super.setKeyListener(wrappedKeyListener);
            // reset the input type and focusable attributes after calling setKeyListener
            super.setRawInputType(inputType);
            super.setFocusable(wasFocusable);
            super.setClickable(wasClickable);
            super.setLongClickable(wasLongClickable);
        }
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
    public void setBackgroundDrawable(@Nullable Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
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
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
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
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
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
        InputConnection inputConnection = AppCompatHintHelper.onCreateInputConnection(
                super.onCreateInputConnection(outAttrs), outAttrs, this);
        return mAppCompatEmojiEditTextHelper.onCreateInputConnection(inputConnection, outAttrs);
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            @Nullable ActionMode.Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(
                TextViewCompat.wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    @Override
    @Nullable
    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        return TextViewCompat.unwrapCustomSelectionActionModeCallback(
                super.getCustomSelectionActionModeCallback());
    }

    /**
     * Adds EmojiCompat KeyListener to correctly edit multi-codepoint emoji when they've been
     * converted to spans.
     *
     * {@inheritDoc}
     */
    @Override
    public void setKeyListener(@Nullable KeyListener keyListener) {
        super.setKeyListener(mAppCompatEmojiEditTextHelper.getKeyListener(keyListener));
    }

    @Override
    public void setEmojiCompatEnabled(boolean enabled) {
        mAppCompatEmojiEditTextHelper.setEnabled(enabled);
    }

    @Override
    public boolean isEmojiCompatEnabled() {
        return mAppCompatEmojiEditTextHelper.isEnabled();
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
    @Nullable
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public ColorStateList getSupportCompoundDrawablesTintList() {
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
    @Nullable
    @Override
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public PorterDuff.Mode getSupportCompoundDrawablesTintMode() {
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
    public void setSupportCompoundDrawablesTintMode(@Nullable PorterDuff.Mode tintMode) {
        mTextHelper.setCompoundDrawableTintMode(tintMode);
        mTextHelper.applyCompoundDrawablesTints();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        int threshold = getThreshold();
        setThreshold(Integer.MAX_VALUE);
        super.onRestoreInstanceState(state);
        setThreshold(threshold);
    }
}
