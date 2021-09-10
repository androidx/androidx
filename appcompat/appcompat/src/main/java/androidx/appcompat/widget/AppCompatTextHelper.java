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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.core.widget.AutoSizeableTextView.PLATFORM_SUPPORTS_AUTOSIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.widget.TextViewCompat;

import java.lang.ref.WeakReference;
import java.util.Locale;

class AppCompatTextHelper {

    private static final int TEXT_FONT_WEIGHT_UNSPECIFIED = -1;

    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    @NonNull
    private final TextView mView;

    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableTopTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableBottomTint;
    private TintInfo mDrawableStartTint;
    private TintInfo mDrawableEndTint;
    private TintInfo mDrawableTint; // Tint used for all compound drawables

    @NonNull
    private final AppCompatTextViewAutoSizeHelper mAutoSizeTextHelper;

    private int mStyle = Typeface.NORMAL;
    private int mFontWeight = TEXT_FONT_WEIGHT_UNSPECIFIED;
    private Typeface mFontTypeface;
    private boolean mAsyncFontPending;

    AppCompatTextHelper(@NonNull TextView view) {
        mView = view;
        mAutoSizeTextHelper = new AppCompatTextViewAutoSizeHelper(mView);
    }

    @SuppressLint("NewApi")
    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        final Context context = mView.getContext();
        final AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();

        // First read the TextAppearance style id
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.AppCompatTextHelper, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(mView, mView.getContext(),
                R.styleable.AppCompatTextHelper, attrs, a.getWrappedTypeArray(),
                defStyleAttr, 0);

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
        String fontVariation = null;
        String localeListString = null;

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
            if (a.hasValue(R.styleable.TextAppearance_textLocale)) {
                localeListString = a.getString(R.styleable.TextAppearance_textLocale);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && a.hasValue(R.styleable.TextAppearance_fontVariationSettings)) {
                fontVariation = a.getString(R.styleable.TextAppearance_fontVariationSettings);
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
        if (a.hasValue(R.styleable.TextAppearance_textLocale)) {
            localeListString = a.getString(R.styleable.TextAppearance_textLocale);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && a.hasValue(R.styleable.TextAppearance_fontVariationSettings)) {
            fontVariation = a.getString(R.styleable.TextAppearance_fontVariationSettings);
        }
        // In P, when the text size attribute is 0, this would not be set. Fix this here.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && a.hasValue(R.styleable.TextAppearance_android_textSize)) {
            if (a.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, -1) == 0) {
                mView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.0f);
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
            if (mFontWeight == TEXT_FONT_WEIGHT_UNSPECIFIED) {
                mView.setTypeface(mFontTypeface, mStyle);
            } else {
                mView.setTypeface(mFontTypeface);
            }
        }
        if (fontVariation != null) {
            mView.setFontVariationSettings(fontVariation);
        }
        if (localeListString != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                mView.setTextLocales(LocaleList.forLanguageTags(localeListString));
            } else if (Build.VERSION.SDK_INT >= 21) {
                final String firstLanTag =
                        localeListString.substring(0, localeListString.indexOf(','));
                mView.setTextLocale(Locale.forLanguageTag(firstLanTag));
            }
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

        // Load compat compound drawables, allowing vector backport
        Drawable drawableLeft = null, drawableTop = null, drawableRight = null,
                drawableBottom = null, drawableStart = null, drawableEnd = null;
        final int drawableLeftId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableLeftCompat, -1);
        if (drawableLeftId != -1) {
            drawableLeft = drawableManager.getDrawable(context, drawableLeftId);
        }
        final int drawableTopId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableTopCompat, -1);
        if (drawableTopId != -1) {
            drawableTop = drawableManager.getDrawable(context, drawableTopId);
        }
        final int drawableRightId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableRightCompat, -1);
        if (drawableRightId != -1) {
            drawableRight = drawableManager.getDrawable(context, drawableRightId);
        }
        final int drawableBottomId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableBottomCompat, -1);
        if (drawableBottomId != -1) {
            drawableBottom = drawableManager.getDrawable(context, drawableBottomId);
        }
        final int drawableStartId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableStartCompat, -1);
        if (drawableStartId != -1) {
            drawableStart = drawableManager.getDrawable(context, drawableStartId);
        }
        final int drawableEndId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableEndCompat, -1);
        if (drawableEndId != -1) {
            drawableEnd = drawableManager.getDrawable(context, drawableEndId);
        }
        setCompoundDrawables(drawableLeft, drawableTop, drawableRight, drawableBottom,
                drawableStart, drawableEnd);

        if (a.hasValue(R.styleable.AppCompatTextView_drawableTint)) {
            final ColorStateList tintList = a.getColorStateList(
                    R.styleable.AppCompatTextView_drawableTint);
            TextViewCompat.setCompoundDrawableTintList(mView, tintList);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_drawableTintMode)) {
            final PorterDuff.Mode tintMode = DrawableUtils.parseTintMode(
                    a.getInt(R.styleable.AppCompatTextView_drawableTintMode, -1), null);
            TextViewCompat.setCompoundDrawableTintMode(mView, tintMode);
        }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mFontWeight = a.getInt(R.styleable.TextAppearance_android_textFontWeight,
                    TEXT_FONT_WEIGHT_UNSPECIFIED);
            if (mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                mStyle = Typeface.NORMAL | (mStyle & Typeface.ITALIC);
            }
        }

        if (a.hasValue(R.styleable.TextAppearance_android_fontFamily)
                || a.hasValue(R.styleable.TextAppearance_fontFamily)) {
            mFontTypeface = null;
            int fontFamilyId = a.hasValue(R.styleable.TextAppearance_fontFamily)
                    ? R.styleable.TextAppearance_fontFamily
                    : R.styleable.TextAppearance_android_fontFamily;
            final int fontWeight = mFontWeight;
            final int style = mStyle;
            if (!context.isRestricted()) {
                final WeakReference<TextView> textViewWeak = new WeakReference<>(mView);
                ResourcesCompat.FontCallback replyCallback = new ResourcesCompat.FontCallback() {
                    @Override
                    public void onFontRetrieved(@NonNull Typeface typeface) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (fontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                                typeface = Typeface.create(typeface, fontWeight,
                                        (style & Typeface.ITALIC) != 0);
                            }
                        }
                        onAsyncTypefaceReceived(textViewWeak, typeface);
                    }

                    @Override
                    public void onFontRetrievalFailed(int reason) {
                        // Do nothing.
                    }
                };
                try {
                    // Note the callback will be triggered on the UI thread.
                    final Typeface typeface = a.getFont(fontFamilyId, mStyle, replyCallback);
                    if (typeface != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                && mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                            mFontTypeface = Typeface.create(
                                    Typeface.create(typeface, Typeface.NORMAL), mFontWeight,
                                    (mStyle & Typeface.ITALIC) != 0);
                        } else {
                            mFontTypeface = typeface;
                        }
                    }
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                            && mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                        mFontTypeface = Typeface.create(
                                Typeface.create(fontFamilyName, Typeface.NORMAL), mFontWeight,
                                (mStyle & Typeface.ITALIC) != 0);
                    } else {
                        mFontTypeface = Typeface.create(fontFamilyName, mStyle);
                    }
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAsyncTypefaceReceived(WeakReference<TextView> textViewWeak, final Typeface typeface) {
        if (mAsyncFontPending) {
            mFontTypeface = typeface;
            final TextView textView = textViewWeak.get();
            if (textView != null) {
                if (ViewCompat.isAttachedToWindow(textView)) {
                    final int style = mStyle;
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setTypeface(typeface, style);
                        }
                    });
                } else {
                    textView.setTypeface(typeface, mStyle);
                }
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
        if (Build.VERSION.SDK_INT < 23) {
            // If we're running on < API 23, the text colors may contain theme references
            // so let's re-set using our own inflater
            if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                final ColorStateList textColor =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColor);
                if (textColor != null) {
                    mView.setTextColor(textColor);
                }
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                final ColorStateList textColorLink =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColorLink);
                if (textColorLink != null) {
                    mView.setLinkTextColor(textColorLink);
                }
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                final ColorStateList textColorHint =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColorHint);
                if (textColorHint != null) {
                    mView.setHintTextColor(textColorHint);
                }
            }
        }
        // For SDK <= P, when the text size attribute is 0, this would not be set. Fix this here.
        if (a.hasValue(R.styleable.TextAppearance_android_textSize)) {
            if (a.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, -1) == 0) {
                mView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.0f);
            }
        }

        updateTypefaceAndStyle(context, a);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && a.hasValue(R.styleable.TextAppearance_fontVariationSettings)) {
            final String fontVariation = a.getString(
                    R.styleable.TextAppearance_fontVariationSettings);
            if (fontVariation != null) {
                mView.setFontVariationSettings(fontVariation);
            }
        }
        a.recycle();
        if (mFontTypeface != null) {
            mView.setTypeface(mFontTypeface, mStyle);
        }
    }

    void setAllCaps(boolean allCaps) {
        mView.setAllCaps(allCaps);
    }

    void onSetCompoundDrawables() {
        applyCompoundDrawablesTints();
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
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!PLATFORM_SUPPORTS_AUTOSIZE) {
            autoSizeText();
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setTextSize(int unit, float size) {
        if (!PLATFORM_SUPPORTS_AUTOSIZE) {
            if (!isAutoSizeEnabled()) {
                setTextSizeInternal(unit, size);
            }
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void autoSizeText() {
        mAutoSizeTextHelper.autoSizeText();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
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

    @Nullable
    ColorStateList getCompoundDrawableTintList() {
        return mDrawableTint != null ? mDrawableTint.mTintList : null;
    }

    void setCompoundDrawableTintList(@Nullable ColorStateList tintList) {
        if (mDrawableTint == null) {
            mDrawableTint = new TintInfo();
        }
        mDrawableTint.mTintList = tintList;
        mDrawableTint.mHasTintList = tintList != null;
        setCompoundTints();
    }

    @Nullable
    PorterDuff.Mode getCompoundDrawableTintMode() {
        return mDrawableTint != null ? mDrawableTint.mTintMode : null;
    }

    void setCompoundDrawableTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mDrawableTint == null) {
            mDrawableTint = new TintInfo();
        }
        mDrawableTint.mTintMode = tintMode;
        mDrawableTint.mHasTintMode = tintMode != null;
        setCompoundTints();
    }

    private void setCompoundTints() {
        mDrawableLeftTint = mDrawableTint;
        mDrawableTopTint = mDrawableTint;
        mDrawableRightTint = mDrawableTint;
        mDrawableBottomTint = mDrawableTint;
        mDrawableStartTint = mDrawableTint;
        mDrawableEndTint = mDrawableTint;
    }

    private void setCompoundDrawables(Drawable drawableLeft, Drawable drawableTop,
            Drawable drawableRight, Drawable drawableBottom, Drawable drawableStart,
            Drawable drawableEnd) {
        // Mirror TextView logic: if start/end drawables supplied, ignore left/right
        if (Build.VERSION.SDK_INT >= 17 && (drawableStart != null || drawableEnd != null)) {
            final Drawable[] existingRel = mView.getCompoundDrawablesRelative();
            mView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart != null ? drawableStart : existingRel[0],
                    drawableTop != null ? drawableTop : existingRel[1],
                    drawableEnd != null ? drawableEnd : existingRel[2],
                    drawableBottom != null ? drawableBottom : existingRel[3]
            );
        } else if (drawableLeft != null || drawableTop != null
                || drawableRight != null || drawableBottom != null) {
            // If have non-compat relative drawables, then ignore leftCompat/rightCompat
            if (Build.VERSION.SDK_INT >= 17) {
                final Drawable[] existingRel = mView.getCompoundDrawablesRelative();
                if (existingRel[0] != null || existingRel[2] != null) {
                    mView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            existingRel[0],
                            drawableTop != null ? drawableTop : existingRel[1],
                            existingRel[2],
                            drawableBottom != null ? drawableBottom : existingRel[3]
                    );
                    return;
                }
            }
            // No relative drawables, so just set any compat drawables
            final Drawable[] existingAbs = mView.getCompoundDrawables();
            mView.setCompoundDrawablesWithIntrinsicBounds(
                    drawableLeft != null ? drawableLeft : existingAbs[0],
                    drawableTop != null ? drawableTop : existingAbs[1],
                    drawableRight != null ? drawableRight : existingAbs[2],
                    drawableBottom != null ? drawableBottom : existingAbs[3]
            );
        }
    }

    /**
     * For SDK < R(API 30), populates the {@link EditorInfo}'s initial surrounding text from the
     * given {@link TextView} if it created an {@link InputConnection}.
     *
     * <p>
     * Use {@link EditorInfoCompat#setInitialSurroundingText(EditorInfo, CharSequence)} to provide
     * initial input text when {@link TextView#onCreateInputConnection(EditorInfo). This method
     * would only be used when running on < R since {@link TextView} already does this on R.
     *
     * @param textView the {@code TextView} to extract the initial surrounding text from
     * @param editorInfo the {@link EditorInfo} on which to set the surrounding text
     */
    void populateSurroundingTextIfNeeded(
            @NonNull TextView textView,
            @Nullable InputConnection inputConnection,
            @NonNull EditorInfo editorInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && inputConnection != null) {
            EditorInfoCompat.setInitialSurroundingText(editorInfo, textView.getText());
        }
    }
}
