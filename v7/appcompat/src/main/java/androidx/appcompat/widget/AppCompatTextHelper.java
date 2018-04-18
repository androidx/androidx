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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.widget.AutoSizeableTextView.PLATFORM_SUPPORTS_AUTOSIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;

import java.lang.ref.WeakReference;

class AppCompatTextHelper {

    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    private final TextView mView;

    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableTopTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableBottomTint;
    private TintInfo mDrawableStartTint;
    private TintInfo mDrawableEndTint;

    private final @NonNull AppCompatTextViewAutoSizeHelper mAutoSizeTextHelper;

    private int mStyle = Typeface.NORMAL;
    private Typeface mFontTypeface;
    private boolean mAsyncFontPending;

    AppCompatTextHelper(TextView view) {
        mView = view;
        mAutoSizeTextHelper = new AppCompatTextViewAutoSizeHelper(mView);
    }

    @SuppressLint("NewApi")
    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        final Context context = mView.getContext();
        final AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();

        // First read the TextAppearance style id
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.AppCompatTextHelper, defStyleAttr, 0);
        final int ap = a.getResourceId(R.styleable.AppCompatTextHelper_android_textAppearance, -1);
        // Now read the compound drawable and grab any tints
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableLeft)) {
            mDrawableLeftTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableLeft, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableTop)) {
            mDrawableTopTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableTop, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableRight)) {
            mDrawableRightTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableRight, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableBottom)) {
            mDrawableBottomTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableBottom, 0));
        }

        if (Build.VERSION.SDK_INT >= 17) {
            if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableStart)) {
                mDrawableStartTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableStart, 0));
            }
            if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableEnd)) {
                mDrawableEndTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableEnd, 0));
            }
        }

        a.recycle();

        // PasswordTransformationMethod wipes out all other TransformationMethod instances
        // in TextView's constructor, so we should only set a new transformation method
        // if we don't have a PasswordTransformationMethod currently...
        final boolean hasPwdTm =
                mView.getTransformationMethod() instanceof PasswordTransformationMethod;
        boolean allCaps = false;
        boolean allCapsSet = false;
        ColorStateList textColor = null;
        ColorStateList textColorHint = null;
        ColorStateList textColorLink = null;

        // First check TextAppearance's textAllCaps value
        if (ap != -1) {
            a = TintTypedArray.obtainStyledAttributes(context, ap, R.styleable.TextAppearance);
            if (!hasPwdTm && a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
                allCapsSet = true;
                allCaps = a.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
            }

            updateTypefaceAndStyle(context, a);
            if (Build.VERSION.SDK_INT < 23) {
                // If we're running on < API 23, the text color may contain theme references
                // so let's re-set using our own inflater
                if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                    textColor = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
                }
                if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                    textColorHint = a.getColorStateList(
                            R.styleable.TextAppearance_android_textColorHint);
                }
                if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                    textColorLink = a.getColorStateList(
                            R.styleable.TextAppearance_android_textColorLink);
                }
            }
            a.recycle();
        }

        // Now read the style's values
        a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.TextAppearance,
                defStyleAttr, 0);
        if (!hasPwdTm && a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            allCapsSet = true;
            allCaps = a.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
        }
        if (Build.VERSION.SDK_INT < 23) {
            // If we're running on < API 23, the text color may contain theme references
            // so let's re-set using our own inflater
            if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                textColor = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                textColorHint = a.getColorStateList(
                        R.styleable.TextAppearance_android_textColorHint);
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                textColorLink = a.getColorStateList(
                        R.styleable.TextAppearance_android_textColorLink);
            }
        }

        updateTypefaceAndStyle(context, a);
        a.recycle();

        if (textColor != null) {
            mView.setTextColor(textColor);
        }
        if (textColorHint != null) {
            mView.setHintTextColor(textColorHint);
        }
        if (textColorLink != null) {
            mView.setLinkTextColor(textColorLink);
        }
        if (!hasPwdTm && allCapsSet) {
            setAllCaps(allCaps);
        }
        if (mFontTypeface != null) {
            mView.setTypeface(mFontTypeface, mStyle);
        }

        mAutoSizeTextHelper.loadFromAttributes(attrs, defStyleAttr);

        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            // Delegate auto-size functionality to the framework implementation.
            if (mAutoSizeTextHelper.getAutoSizeTextType()
                    != TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
                final int[] autoSizeTextSizesInPx =
                        mAutoSizeTextHelper.getAutoSizeTextAvailableSizes();
                if (autoSizeTextSizesInPx.length > 0) {
                    if (mView.getAutoSizeStepGranularity() != AppCompatTextViewAutoSizeHelper
                            .UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) {
                        // Configured with granularity, preserve details.
                        mView.setAutoSizeTextTypeUniformWithConfiguration(
                                mAutoSizeTextHelper.getAutoSizeMinTextSize(),
                                mAutoSizeTextHelper.getAutoSizeMaxTextSize(),
                                mAutoSizeTextHelper.getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
                    } else {
                        mView.setAutoSizeTextTypeUniformWithPresetSizes(
                                autoSizeTextSizesInPx, TypedValue.COMPLEX_UNIT_PX);
                    }
                }
            }
        }

        // Read line and baseline heights attributes.
        a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.AppCompatTextView);
        final int firstBaselineToTopHeight = a.getDimensionPixelSize(
                R.styleable.AppCompatTextView_firstBaselineToTopHeight, -1);
        final int lastBaselineToBottomHeight = a.getDimensionPixelSize(
                R.styleable.AppCompatTextView_lastBaselineToBottomHeight, -1);
        final int lineHeight = a.getDimensionPixelSize(
                R.styleable.AppCompatTextView_lineHeight, -1);
        a.recycle();
        if (firstBaselineToTopHeight != -1) {
            TextViewCompat.setFirstBaselineToTopHeight(mView, firstBaselineToTopHeight);
        }
        if (lastBaselineToBottomHeight != -1) {
            TextViewCompat.setLastBaselineToBottomHeight(mView, lastBaselineToBottomHeight);
        }
        if (lineHeight != -1) {
            TextViewCompat.setLineHeight(mView, lineHeight);
        }
    }

    private void updateTypefaceAndStyle(Context context, TintTypedArray a) {
        mStyle = a.getInt(R.styleable.TextAppearance_android_textStyle, mStyle);

        if (a.hasValue(R.styleable.TextAppearance_android_fontFamily)
                || a.hasValue(R.styleable.TextAppearance_fontFamily)) {
            mFontTypeface = null;
            int fontFamilyId = a.hasValue(R.styleable.TextAppearance_fontFamily)
                    ? R.styleable.TextAppearance_fontFamily
                    : R.styleable.TextAppearance_android_fontFamily;
            if (!context.isRestricted()) {
                final WeakReference<TextView> textViewWeak = new WeakReference<>(mView);
                ResourcesCompat.FontCallback replyCallback = new ResourcesCompat.FontCallback() {
                    @Override
                    public void onFontRetrieved(@NonNull Typeface typeface) {
                        onAsyncTypefaceReceived(textViewWeak, typeface);
                    }

                    @Override
                    public void onFontRetrievalFailed(int reason) {
                        // Do nothing.
                    }
                };
                try {
                    // Note the callback will be triggered on the UI thread.
                    mFontTypeface = a.getFont(fontFamilyId, mStyle, replyCallback);
                    // If this call gave us an immediate result, ignore any pending callbacks.
                    mAsyncFontPending = mFontTypeface == null;
                } catch (UnsupportedOperationException | Resources.NotFoundException e) {
                    // Expected if it is not a font resource.
                }
            }
            if (mFontTypeface == null) {
                // Try with String. This is done by TextView JB+, but fails in ICS
                String fontFamilyName = a.getString(fontFamilyId);
                if (fontFamilyName != null) {
                    mFontTypeface = Typeface.create(fontFamilyName, mStyle);
                }
            }
            return;
        }

        if (a.hasValue(R.styleable.TextAppearance_android_typeface)) {
            // Ignore previous pending fonts
            mAsyncFontPending = false;
            int typefaceIndex = a.getInt(R.styleable.TextAppearance_android_typeface, SANS);
            switch (typefaceIndex) {
                case SANS:
                    mFontTypeface = Typeface.SANS_SERIF;
                    break;

                case SERIF:
                    mFontTypeface = Typeface.SERIF;
                    break;

                case MONOSPACE:
                    mFontTypeface = Typeface.MONOSPACE;
                    break;
            }
        }
    }

    private void onAsyncTypefaceReceived(WeakReference<TextView> textViewWeak, Typeface typeface) {
        if (mAsyncFontPending) {
            mFontTypeface = typeface;
            final TextView textView = textViewWeak.get();
            if (textView != null) {
                textView.setTypeface(typeface, mStyle);
            }
        }
    }

    void onSetTextAppearance(Context context, int resId) {
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context,
                resId, R.styleable.TextAppearance);
        if (a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            // This breaks away slightly from the logic in TextView.setTextAppearance that serves
            // as an "overlay" on the current state of the TextView. Since android:textAllCaps
            // may have been set to true in this text appearance, we need to make sure that
            // app:textAllCaps has the chance to override it
            setAllCaps(a.getBoolean(R.styleable.TextAppearance_textAllCaps, false));
        }
        if (Build.VERSION.SDK_INT < 23
                && a.hasValue(R.styleable.TextAppearance_android_textColor)) {
            // If we're running on < API 23, the text color may contain theme references
            // so let's re-set using our own inflater
            final ColorStateList textColor
                    = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
            if (textColor != null) {
                mView.setTextColor(textColor);
            }
        }

        updateTypefaceAndStyle(context, a);
        a.recycle();
        if (mFontTypeface != null) {
            mView.setTypeface(mFontTypeface, mStyle);
        }
    }

    void setAllCaps(boolean allCaps) {
        mView.setAllCaps(allCaps);
    }

    void applyCompoundDrawablesTints() {
        if (mDrawableLeftTint != null || mDrawableTopTint != null ||
                mDrawableRightTint != null || mDrawableBottomTint != null) {
            final Drawable[] compoundDrawables = mView.getCompoundDrawables();
            applyCompoundDrawableTint(compoundDrawables[0], mDrawableLeftTint);
            applyCompoundDrawableTint(compoundDrawables[1], mDrawableTopTint);
            applyCompoundDrawableTint(compoundDrawables[2], mDrawableRightTint);
            applyCompoundDrawableTint(compoundDrawables[3], mDrawableBottomTint);
        }
        if (Build.VERSION.SDK_INT >= 17) {
            if (mDrawableStartTint != null || mDrawableEndTint != null) {
                final Drawable[] compoundDrawables = mView.getCompoundDrawablesRelative();
                applyCompoundDrawableTint(compoundDrawables[0], mDrawableStartTint);
                applyCompoundDrawableTint(compoundDrawables[2], mDrawableEndTint);
            }
        }
    }

    private void applyCompoundDrawableTint(Drawable drawable, TintInfo info) {
        if (drawable != null && info != null) {
            AppCompatDrawableManager.tintDrawable(drawable, info, mView.getDrawableState());
        }
    }

    private static TintInfo createTintInfo(Context context,
            AppCompatDrawableManager drawableManager, int drawableId) {
        final ColorStateList tintList = drawableManager.getTintList(context, drawableId);
        if (tintList != null) {
            final TintInfo tintInfo = new TintInfo();
            tintInfo.mHasTintList = true;
            tintInfo.mTintList = tintList;
            return tintInfo;
        }
        return null;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!PLATFORM_SUPPORTS_AUTOSIZE) {
            autoSizeText();
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    void setTextSize(int unit, float size) {
        if (!PLATFORM_SUPPORTS_AUTOSIZE) {
            if (!isAutoSizeEnabled()) {
                setTextSizeInternal(unit, size);
            }
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    void autoSizeText() {
        mAutoSizeTextHelper.autoSizeText();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    boolean isAutoSizeEnabled() {
        return mAutoSizeTextHelper.isAutoSizeEnabled();
    }

    private void setTextSizeInternal(int unit, float size) {
        mAutoSizeTextHelper.setTextSizeInternal(unit, size);
    }

    void setAutoSizeTextTypeWithDefaults(@TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        mAutoSizeTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
    }

    void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithConfiguration(
                autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
    }

    void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit)
            throws IllegalArgumentException {
        mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
    }

    @TextViewCompat.AutoSizeTextType
    int getAutoSizeTextType() {
        return mAutoSizeTextHelper.getAutoSizeTextType();
    }

    int getAutoSizeStepGranularity() {
        return mAutoSizeTextHelper.getAutoSizeStepGranularity();
    }

    int getAutoSizeMinTextSize() {
        return mAutoSizeTextHelper.getAutoSizeMinTextSize();
    }

    int getAutoSizeMaxTextSize() {
        return mAutoSizeTextHelper.getAutoSizeMaxTextSize();
    }

    int[] getAutoSizeTextAvailableSizes() {
        return mAutoSizeTextHelper.getAutoSizeTextAvailableSizes();
    }
}
