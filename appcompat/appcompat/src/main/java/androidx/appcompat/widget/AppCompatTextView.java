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
import static androidx.appcompat.widget.ViewUtils.SDK_LEVEL_SUPPORTS_AUTOSIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.view.TintableBackgroundView;
import androidx.core.widget.AutoSizeableTextView;
import androidx.core.widget.TextViewCompat;
import androidx.core.widget.TintableCompoundDrawablesView;
import androidx.resourceinspection.annotation.AppCompatShadowedAttributes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A {@link TextView} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 * <li>Allows dynamic tint of its background via the background tint methods in
 * {@link androidx.core.view.ViewCompat}.</li>
 * <li>Allows setting of the background tint using
 * {@link androidx.appcompat.R.attr#backgroundTint} and
 * {@link androidx.appcompat.R.attr#backgroundTintMode}.</li>
 * <li>Supports auto-sizing via {@link androidx.core.widget.TextViewCompat} by allowing to instruct
 * a {@link TextView} to let the size of the text expand or contract automatically to fill its
 * layout based on the TextView's characteristics and boundaries. The style attributes associated
 * with auto-sizing are
 * {@link androidx.appcompat.R.attr#autoSizeTextType},
 * {@link androidx.appcompat.R.attr#autoSizeMinTextSize},
 * {@link androidx.appcompat.R.attr#autoSizeMaxTextSize},
 * {@link androidx.appcompat.R.attr#autoSizeStepGranularity} and
 * {@link androidx.appcompat.R.attr#autoSizePresetSizes}, all of which work back to
 * {@link VERSION_CODES#ICE_CREAM_SANDWICH Ice Cream Sandwich}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link TextView} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
@AppCompatShadowedAttributes
public class AppCompatTextView extends TextView implements TintableBackgroundView,
        TintableCompoundDrawablesView, AutoSizeableTextView, EmojiCompatConfigurationView {

    private static final String TAG = "AppCompatTextView";
    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    private final AppCompatTextClassifierHelper mTextClassifierHelper;
    @SuppressWarnings("NotNullFieldNotInitialized") // initialized in getter
    private @NonNull AppCompatEmojiTextHelper mEmojiTextViewHelper;

    private boolean mIsSetTypefaceProcessing = false;

    /**
     * Equivalent to Typeface.mOriginalTypeface.
     * Used to correctly emulate the behavior of getTypeface(), because we need to call setTypeface
     * directly in order to implement caching of variation instances of typefaces.
     */
    private Typeface mOriginalTypeface;

    /**
     * The last Typeface we are aware of being set on {@link #getPaint()}.
     * Used to detect if it has been changed out from under us via directly calling
     * {@link android.graphics.Paint#setTypeface(Typeface)} or
     * {@link android.graphics.Paint#setFontVariationSettings(String)}
     * (which is not supported, so this is a best-effort workaround).
     *
     * @see #setTypefaceInternal(Typeface)
     */
    private Typeface mLastKnownTypefaceSetOnPaint;

    /**
     * The currently applied font variation settings.
     * Used to make getFontVariationSettings somewhat more accurate with Typeface instance caching,
     * as we don't call super.setFontVariationSettings.
     */
    private String mFontVariationSettings;

    private @Nullable SuperCaller mSuperCaller = null;

    private @Nullable Future<PrecomputedTextCompat> mPrecomputedTextFuture;

    public AppCompatTextView(@NonNull Context context) {
        this(context, null);
    }

    public AppCompatTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppCompatTextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        ThemeUtils.checkAppCompatTheme(this, getContext());

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        mTextHelper.applyCompoundDrawablesTints();

        mTextClassifierHelper = new AppCompatTextClassifierHelper(this);

        AppCompatEmojiTextHelper emojiTextViewHelper = getEmojiTextViewHelper();
        emojiTextViewHelper.loadFromAttributes(attrs, defStyleAttr);
    }

    /**
     * This may be called from super constructors.
     */
    private @NonNull AppCompatEmojiTextHelper getEmojiTextViewHelper() {
        //noinspection ConstantConditions
        if (mEmojiTextViewHelper == null) {
            mEmojiTextViewHelper = new AppCompatEmojiTextHelper(this);
        }
        return mEmojiTextViewHelper;
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
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public @Nullable ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
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
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public PorterDuff.@Nullable Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }

    /**
     * Set font variation settings.
     * See {@link TextView#setFontVariationSettings(String)} for details.
     * <p>
     * <em>Note:</em> Due to performance optimizations,
     * {@code getPaint().getFontVariationSettings()} will be less reliable than if not using
     * AppCompatTextView.  You should prefer {@link #getFontVariationSettings()}, which will be more
     * accurate. However, neither approach will work correctly if using Typeface objects with
     * embedded font variation settings.
     */
    // Reference comparison with mLastKnownTypefaceSetOnPaint is intended;
    // it should in fact be the exact instance, because we set it.
    @SuppressWarnings("ReferenceEquality")
    @RequiresApi(26)
    @Override
    public boolean setFontVariationSettings(@Nullable String fontVariationSettings) {
        Typeface baseTypeface = mOriginalTypeface;
        // Try to work around apps mutating the result of getPaint()
        // See setTypefaceInternal doc comment for details.
        if (mLastKnownTypefaceSetOnPaint != getPaint().getTypeface()) {
            Log.w(TAG, "getPaint().getTypeface() changed unexpectedly."
                    + " App code should not modify the result of getPaint().");
            // Best effort: use that new Typeface instead.
            baseTypeface = getPaint().getTypeface();
        }
        Typeface variationTypefaceInstance = AppCompatTextHelper.Api26Impl.createVariationInstance(
                baseTypeface, fontVariationSettings);
        if (variationTypefaceInstance != null) {
            setTypefaceInternal(variationTypefaceInstance);
            mFontVariationSettings = fontVariationSettings;
            return true;
        } else {
            return false;
        }
    }

    @RequiresApi(26)
    @Override
    public @Nullable String getFontVariationSettings() {
        return mFontVariationSettings;
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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onLayout(changed, left, top, right, bottom);
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            super.setTextSize(unit, size);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setTextSize(unit, size);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        boolean useHelper = mTextHelper != null && !SDK_LEVEL_SUPPORTS_AUTOSIZE
                && mTextHelper.isAutoSizeEnabled();
        if (useHelper) {
            mTextHelper.autoSizeText();
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeWithDefaults(
     *TextView, int)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setAutoSizeTextTypeWithDefaults(
            @TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            getSuperCaller().setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeUniformWithConfiguration(
     *TextView, int, int, int, int)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            getSuperCaller().setAutoSizeTextTypeUniformWithConfiguration(autoSizeMinTextSize,
                    autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithConfiguration(
                        autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeUniformWithPresetSizes(
     *TextView, int[], int)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public void setAutoSizeTextTypeUniformWithPresetSizes(int @NonNull [] presetSizes, int unit)
            throws IllegalArgumentException {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            getSuperCaller().setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeTextType(TextView)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    @TextViewCompat.AutoSizeTextType
    // Suppress lint error for TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM [WrongConstant]
    @SuppressLint("WrongConstant")
    public int getAutoSizeTextType() {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            return getSuperCaller().getAutoSizeTextType() == TextView
                    .AUTO_SIZE_TEXT_TYPE_UNIFORM
                    ? TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    : TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextType();
            }
        }
        return TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeStepGranularity(TextView)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int getAutoSizeStepGranularity() {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            return getSuperCaller().getAutoSizeStepGranularity();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeStepGranularity();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeMinTextSize(TextView)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int getAutoSizeMinTextSize() {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            return getSuperCaller().getAutoSizeMinTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMinTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeMaxTextSize(TextView)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int getAutoSizeMaxTextSize() {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            return getSuperCaller().getAutoSizeMaxTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMaxTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeTextAvailableSizes(TextView)}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public int[] getAutoSizeTextAvailableSizes() {
        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            return getSuperCaller().getAutoSizeTextAvailableSizes();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextAvailableSizes();
            }
        }
        return new int[0];
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        mTextHelper.populateSurroundingTextIfNeeded(this, ic, outAttrs);
        return AppCompatHintHelper.onCreateInputConnection(ic, outAttrs, this);
    }

    @Override
    public void setFirstBaselineToTopHeight(@Px @IntRange(from = 0) int firstBaselineToTopHeight) {
        if (Build.VERSION.SDK_INT >= 28) {
            getSuperCaller().setFirstBaselineToTopHeight(firstBaselineToTopHeight);
        } else {
            TextViewCompat.setFirstBaselineToTopHeight(this, firstBaselineToTopHeight);
        }
    }

    @Override
    public void setLastBaselineToBottomHeight(
            @Px @IntRange(from = 0) int lastBaselineToBottomHeight) {
        if (Build.VERSION.SDK_INT >= 28) {
            getSuperCaller().setLastBaselineToBottomHeight(lastBaselineToBottomHeight);
        } else {
            TextViewCompat.setLastBaselineToBottomHeight(this,
                    lastBaselineToBottomHeight);
        }
    }

    @Override
    public int getFirstBaselineToTopHeight() {
        return TextViewCompat.getFirstBaselineToTopHeight(this);
    }

    @Override
    public int getLastBaselineToBottomHeight() {
        return TextViewCompat.getLastBaselineToBottomHeight(this);
    }

    @Override
    public void setLineHeight(@Px @IntRange(from = 0) int lineHeight) {
        TextViewCompat.setLineHeight(this, lineHeight);
    }

    @Override
    public void setLineHeight(int unit, @FloatRange(from = 0) float lineHeight) {
        if (Build.VERSION.SDK_INT >= 34) {
            getSuperCaller().setLineHeight(unit, lineHeight);
        } else {
            TextViewCompat.setLineHeight(this, unit, lineHeight);
        }
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            ActionMode.@Nullable Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(
                TextViewCompat.wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    @Override
    public ActionMode.@Nullable Callback getCustomSelectionActionModeCallback() {
        return TextViewCompat.unwrapCustomSelectionActionModeCallback(
                super.getCustomSelectionActionModeCallback());
    }

    /**
     * Gets the parameters for text layout precomputation, for use with
     * {@link PrecomputedTextCompat}.
     *
     * @return a current {@link PrecomputedTextCompat.Params}
     * @see PrecomputedTextCompat
     */
    public PrecomputedTextCompat.@NonNull Params getTextMetricsParamsCompat() {
        return TextViewCompat.getTextMetricsParams(this);
    }

    /**
     * Apply the text layout parameter.
     *
     * Update the TextView parameters to be compatible with {@link PrecomputedTextCompat.Params}.
     *
     * @see PrecomputedTextCompat
     */
    public void setTextMetricsParamsCompat(PrecomputedTextCompat.@NonNull Params params) {
        TextViewCompat.setTextMetricsParams(this, params);
    }

    /**
     * Sets the PrecomputedTextCompat to the TextView.
     *
     * If the given PrecomputeTextCompat is not compatible with textView, throws an
     * IllegalArgumentException.
     *
     * @param precomputed the precomputed text
     * @throws IllegalArgumentException if precomputed text is not compatible with textView.
     */
    public void setPrecomputedText(@NonNull PrecomputedTextCompat precomputed) {
        TextViewCompat.setPrecomputedText(this, precomputed);
    }

    private void consumeTextFutureAndSetBlocking() {
        if (mPrecomputedTextFuture != null) {
            try {
                Future<PrecomputedTextCompat> future = mPrecomputedTextFuture;
                mPrecomputedTextFuture = null;
                TextViewCompat.setPrecomputedText(this, future.get());
            } catch (InterruptedException | ExecutionException e) {
                // ignore
            }
        }
    }

    @Override
    public CharSequence getText() {
        consumeTextFutureAndSetBlocking();
        return super.getText();
    }

    /**
     * Sets the {@link TextClassifier} for this TextView.
     */
    @Override
    @RequiresApi(api = 26)
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            getSuperCaller().setTextClassifier(textClassifier);
            return;
        }
        mTextClassifierHelper.setTextClassifier(textClassifier);
    }

    /**
     * Returns the {@link TextClassifier} used by this TextView.
     * If no TextClassifier has been set, this TextView uses the default set by the
     * {@link android.view.textclassifier.TextClassificationManager}.
     */
    @Override
    @RequiresApi(api = 26)
    public @NonNull TextClassifier getTextClassifier() {
        // The null check is necessary because getTextClassifier is called when we are invoking
        // the super class's constructor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            return getSuperCaller().getTextClassifier();
        }
        return mTextClassifierHelper.getTextClassifier();
    }

    /**
     * Set the precomputed text future.
     *
     * This method sets future of the precomputed text instead of immediately applying text to the
     * TextView. Anything layout related property changes, text size, typeface, letter spacing, etc
     * after this method call will causes IllegalArgumentException during View measurement.
     *
     * See {@link PrecomputedTextCompat#getTextFuture} for more detail.
     *
     * @param future a future for the precomputed text
     * @see PrecomputedTextCompat#getTextFuture
     */
    public void setTextFuture(@Nullable Future<PrecomputedTextCompat> future) {
        mPrecomputedTextFuture = future;
        if (future != null) {
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        consumeTextFutureAndSetBlocking();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(@Nullable Drawable left,
            @Nullable Drawable top, @Nullable Drawable right, @Nullable Drawable bottom) {
        super.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(
                left != 0 ? AppCompatResources.getDrawable(context, left) : null,
                top != 0 ? AppCompatResources.getDrawable(context, top) : null,
                right != 0 ? AppCompatResources.getDrawable(context, right) : null,
                bottom != 0 ? AppCompatResources.getDrawable(context, bottom) : null);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    @Override
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(@Nullable Drawable start,
            @Nullable Drawable top, @Nullable Drawable end, @Nullable Drawable bottom) {
        super.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        if (mTextHelper != null) {
            mTextHelper.onSetCompoundDrawables();
        }
    }

    @Override
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(
            int start, int top, int end, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(
                start != 0 ? AppCompatResources.getDrawable(context, start) : null,
                top != 0 ? AppCompatResources.getDrawable(context, top) : null,
                end != 0 ? AppCompatResources.getDrawable(context, end) : null,
                bottom != 0 ? AppCompatResources.getDrawable(context, bottom) : null);
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

    // Never call super.setTypeface directly, always use this or setTypefaceInternal
    // See docs on setTypefaceInternal for the differences
    @Override
    public void setTypeface(@Nullable Typeface tf) {
        mOriginalTypeface = tf;
        setTypefaceInternal(tf);
    }

    /**
     * Call this when setting the typeface in any way that the user didn't directly ask for
     * (that is, any case where TextView itself does not call through to setTypeface or otherwise
     * set its mOriginalTypeface).  Otherwise, use {@link #setTypeface(Typeface)} (or something
     * that calls it).
     * <p>
     * Calls the superclass setTypeface, but does not set mOriginalTypeface.
     * Also tracks what we set it to, in order to detect when it's been changed out from under us
     * via modifying the Paint object directly.
     * This isn't officially supported ({@link TextView#getPaint()} specifically says not to modify
     * it), but at least one app is known to have done this, so we're providing best-effort support.
     */
    private void setTypefaceInternal(@Nullable Typeface tf) {
        mLastKnownTypefaceSetOnPaint = tf;
        super.setTypeface(tf);
    }

    @Override
    // Code inspection reveals that the superclass method can return null.
    @SuppressWarnings("InvalidNullabilityOverride")
    public @Nullable Typeface getTypeface() {
        return mOriginalTypeface;
    }

    @Override
    public void setTypeface(@Nullable Typeface tf, int style) {
        if (mIsSetTypefaceProcessing) {
            // b/151782655
            // Some device up to API19 recursively calls setTypeface. To avoid infinity recursive
            // setTypeface call, exit if we know this is re-entrant call.
            // TODO(nona): Remove this once Android X minSdkVersion moves to API21.
            return;
        }
        final Typeface finalTypeface;
        if (tf != null && style > 0) {
            finalTypeface = TypefaceCompat.create(getContext(), tf, style);
        } else {
            finalTypeface = tf;
        }

        mIsSetTypefaceProcessing = true;
        try {
            super.setTypeface(finalTypeface, style);
        } finally {
            mIsSetTypefaceProcessing = false;
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT < 33 && onCheckIsTextEditor()) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            // If the AppCompatTextView is editable, user can input text on it.
            // Calling isActive() here implied a checkFocus() call to update the active served
            // view for input method. This is a backport for mServedView was detached, but the
            // next served view gets mistakenly cleared as well.
            // https://android.googlesource.com/platform/frameworks/base/+/734613a500fb
            imm.isActive(this);
        }
    }

    @UiThread
    @RequiresApi(api = 26)
    SuperCaller getSuperCaller() {
        if (mSuperCaller == null) {
            if (Build.VERSION.SDK_INT >= 34) {
                mSuperCaller = new SuperCallerApi34();
            } else if (Build.VERSION.SDK_INT >= 28) {
                mSuperCaller = new SuperCallerApi28();
            } else if (Build.VERSION.SDK_INT >= 26) {
                mSuperCaller = new SuperCallerApi26();
            }
        }
        return mSuperCaller;
    }



    private interface SuperCaller {
        // api 26
        int getAutoSizeMaxTextSize();
        int getAutoSizeMinTextSize();
        int getAutoSizeStepGranularity();
        int[] getAutoSizeTextAvailableSizes();
        int getAutoSizeTextType();
        TextClassifier getTextClassifier();
        void setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize,
                int autoSizeMaxTextSize, int autoSizeStepGranularity, int unit);
        void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit);
        void setAutoSizeTextTypeWithDefaults(int autoSizeTextType);
        void setTextClassifier(@Nullable TextClassifier textClassifier);

        // api 28
        void setFirstBaselineToTopHeight(@Px int firstBaselineToTopHeight);
        void setLastBaselineToBottomHeight(@Px int lastBaselineToBottomHeight);

        // api 34
        void setLineHeight(int unit, @FloatRange(from = 0) float lineHeight);
    }

    @RequiresApi(api = 26)
    class SuperCallerApi26 implements SuperCaller {
        @Override
        public int getAutoSizeMaxTextSize() {
            return AppCompatTextView.super.getAutoSizeMaxTextSize();
        }

        @Override
        public int getAutoSizeMinTextSize() {
            return AppCompatTextView.super.getAutoSizeMinTextSize();
        }

        @Override
        public int getAutoSizeStepGranularity() {
            return AppCompatTextView.super.getAutoSizeStepGranularity();
        }

        @Override
        public int[] getAutoSizeTextAvailableSizes() {
            return AppCompatTextView.super.getAutoSizeTextAvailableSizes();
        }

        @Override
        public int getAutoSizeTextType() {
            return AppCompatTextView.super.getAutoSizeTextType();
        }

        @Override
        public TextClassifier getTextClassifier() {
            return AppCompatTextView.super.getTextClassifier();
        }

        @Override
        public void setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize,
                int autoSizeMaxTextSize, int autoSizeStepGranularity, int unit) {
            AppCompatTextView.super.setAutoSizeTextTypeUniformWithConfiguration(autoSizeMinTextSize,
                    autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        }

        @Override
        public void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit) {
            AppCompatTextView.super.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        }

        @Override
        public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType) {
            AppCompatTextView.super.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        }

        @Override
        public void setTextClassifier(@Nullable TextClassifier textClassifier) {
            AppCompatTextView.super.setTextClassifier(textClassifier);
        }

        @Override
        public void setFirstBaselineToTopHeight(int firstBaselineToTopHeight) {}

        @Override
        public void setLastBaselineToBottomHeight(int lastBaselineToBottomHeight) {}

        @Override
        public void setLineHeight(int unit, float lineHeight) {}
    }

    @RequiresApi(api = 28)
    class SuperCallerApi28 extends SuperCallerApi26 {

        @Override
        public void setFirstBaselineToTopHeight(@Px int firstBaselineToTopHeight) {
            AppCompatTextView.super.setFirstBaselineToTopHeight(firstBaselineToTopHeight);
        }

        @Override
        public void setLastBaselineToBottomHeight(@Px int lastBaselineToBottomHeight) {
            AppCompatTextView.super.setLastBaselineToBottomHeight(lastBaselineToBottomHeight);
        }
    }

    @RequiresApi(api = 34)
    class SuperCallerApi34 extends SuperCallerApi28 {
        @Override
        public void setLineHeight(int unit, float lineHeight) {
            AppCompatTextView.super.setLineHeight(unit, lineHeight);
        }
    }
}
