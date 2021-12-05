/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.R;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class which encapsulates the logic for the TextView auto-size text feature added to
 * the Android Framework in {@link android.os.Build.VERSION_CODES#O}.
 *
 * <p>A TextView can be instructed to let the size of the text expand or contract automatically to
 * fill its layout based on the TextView's characteristics and boundaries.
 */
class AppCompatTextViewAutoSizeHelper {
    private static final String TAG = "ACTVAutoSizeHelper";
    private static final RectF TEMP_RECTF = new RectF();
    // Default minimum size for auto-sizing text in scaled pixels.
    private static final int DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP = 12;
    // Default maximum size for auto-sizing text in scaled pixels.
    private static final int DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP = 112;
    // Default value for the step size in pixels.
    private static final int DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX = 1;
    // Cache of TextView methods used via reflection; the key is the method name and the value is
    // the method itself or null if it can not be found.
    @SuppressLint("BanConcurrentHashMap")
    private static java.util.concurrent.ConcurrentHashMap<String, Method>
            sTextViewMethodByNameCache = new java.util.concurrent.ConcurrentHashMap<>();
    // Cache of TextView fields used via reflection; the key is the field name and the value is
    // the field itself or null if it can not be found.
    @SuppressLint("BanConcurrentHashMap")
    private static java.util.concurrent.ConcurrentHashMap<String, Field> sTextViewFieldByNameCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    // Use this to specify that any of the auto-size configuration int values have not been set.
    static final float UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE = -1f;
    // Ported from TextView#VERY_WIDE. Represents a maximum width in pixels the TextView takes when
    // horizontal scrolling is activated.
    private static final int VERY_WIDE = 1024 * 1024;
    // Auto-size text type.
    private int mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    // Specify if auto-size text is needed.
    private boolean mNeedsAutoSizeText = false;
    // Step size for auto-sizing in pixels.
    private float mAutoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    // Minimum text size for auto-sizing in pixels.
    private float mAutoSizeMinTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    // Maximum text size for auto-sizing in pixels.
    private float mAutoSizeMaxTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    // Contains a (specified or computed) distinct sorted set of text sizes in pixels to pick from
    // when auto-sizing text.
    private int[] mAutoSizeTextSizesInPx = new int[0];
    // Specifies whether auto-size should use the provided auto size steps set or if it should
    // build the steps set using mAutoSizeMinTextSizeInPx, mAutoSizeMaxTextSizeInPx and
    // mAutoSizeStepGranularityInPx.
    private boolean mHasPresetAutoSizeValues = false;
    private TextPaint mTempTextPaint;

    @NonNull
    private final TextView mTextView;
    private final Context mContext;

    private final Impl mImpl;

    private static class Impl {
        Impl() {}

        boolean isHorizontallyScrollable(TextView textView) {
            return invokeAndReturnWithDefault(textView, "getHorizontallyScrolling", false);
        }

        void computeAndSetTextDirection(StaticLayout.Builder layoutBuilder, TextView textView) {
        }
    }

    @RequiresApi(23)
    private static class Impl23 extends Impl {
        Impl23() {}

        @Override
        void computeAndSetTextDirection(StaticLayout.Builder layoutBuilder,
                TextView textView) {
            final TextDirectionHeuristic textDirectionHeuristic =
                    invokeAndReturnWithDefault(textView, "getTextDirectionHeuristic",
                            TextDirectionHeuristics.FIRSTSTRONG_LTR);
            layoutBuilder.setTextDirection(textDirectionHeuristic);
        }
    }

    @RequiresApi(29)
    private static class Impl29 extends Impl23 {
        Impl29() {}

        @Override
        boolean isHorizontallyScrollable(TextView textView) {
            return textView.isHorizontallyScrollable();
        }

        @Override
        void computeAndSetTextDirection(StaticLayout.Builder layoutBuilder,
                TextView textView) {
            layoutBuilder.setTextDirection(textView.getTextDirectionHeuristic());
        }
    }

    AppCompatTextViewAutoSizeHelper(@NonNull TextView textView) {
        mTextView = textView;
        mContext = mTextView.getContext();
        if (Build.VERSION.SDK_INT >= 29) {
            mImpl = new Impl29();
        } else if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new Impl23();
        } else {
            mImpl = new Impl();
        }
    }

    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        float autoSizeMinTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        float autoSizeMaxTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        float autoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;

        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.AppCompatTextView,
                defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(mTextView, mTextView.getContext(),
                    R.styleable.AppCompatTextView, attrs, a,
                    defStyleAttr, 0);
        if (a.hasValue(R.styleable.AppCompatTextView_autoSizeTextType)) {
            mAutoSizeTextType = a.getInt(R.styleable.AppCompatTextView_autoSizeTextType,
                    TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_autoSizeStepGranularity)) {
            autoSizeStepGranularityInPx = a.getDimension(
                    R.styleable.AppCompatTextView_autoSizeStepGranularity,
                    UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_autoSizeMinTextSize)) {
            autoSizeMinTextSizeInPx = a.getDimension(
                    R.styleable.AppCompatTextView_autoSizeMinTextSize,
                    UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_autoSizeMaxTextSize)) {
            autoSizeMaxTextSizeInPx = a.getDimension(
                    R.styleable.AppCompatTextView_autoSizeMaxTextSize,
                    UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_autoSizePresetSizes)) {
            final int autoSizeStepSizeArrayResId = a.getResourceId(
                    R.styleable.AppCompatTextView_autoSizePresetSizes, 0);
            if (autoSizeStepSizeArrayResId > 0) {
                final TypedArray autoSizePreDefTextSizes = a.getResources()
                        .obtainTypedArray(autoSizeStepSizeArrayResId);
                setupAutoSizeUniformPresetSizes(autoSizePreDefTextSizes);
                autoSizePreDefTextSizes.recycle();
            }
        }
        a.recycle();

        if (supportsAutoSizeText()) {
            if (mAutoSizeTextType == TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM) {
                // If uniform auto-size has been specified but preset values have not been set then
                // replace the auto-size configuration values that have not been specified with the
                // defaults.
                if (!mHasPresetAutoSizeValues) {
                    final DisplayMetrics displayMetrics =
                            mContext.getResources().getDisplayMetrics();

                    if (autoSizeMinTextSizeInPx == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) {
                        autoSizeMinTextSizeInPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP,
                                displayMetrics);
                    }

                    if (autoSizeMaxTextSizeInPx == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) {
                        autoSizeMaxTextSizeInPx = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP,
                                displayMetrics);
                    }

                    if (autoSizeStepGranularityInPx
                            == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) {
                        autoSizeStepGranularityInPx = DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX;
                    }

                    validateAndSetAutoSizeTextTypeUniformConfiguration(autoSizeMinTextSizeInPx,
                            autoSizeMaxTextSizeInPx,
                            autoSizeStepGranularityInPx);
                }

                setupAutoSizeText();
            }
        } else {
            mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        }
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds by using the default auto-size configuration.
     *
     * @param autoSizeTextType the type of auto-size. Must be one of
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * {@link R.attr#autoSizeTextType}
     *
     * @see #getAutoSizeTextType()
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setAutoSizeTextTypeWithDefaults(@TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        if (supportsAutoSizeText()) {
            switch (autoSizeTextType) {
                case TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE:
                    clearAutoSizeConfiguration();
                    break;
                case TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM:
                    final DisplayMetrics displayMetrics =
                            mContext.getResources().getDisplayMetrics();
                    final float autoSizeMinTextSizeInPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP,
                            displayMetrics);
                    final float autoSizeMaxTextSizeInPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP,
                            displayMetrics);

                    validateAndSetAutoSizeTextTypeUniformConfiguration(
                            autoSizeMinTextSizeInPx,
                            autoSizeMaxTextSizeInPx,
                            DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX);
                    if (setupAutoSizeText()) {
                        autoSizeText();
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown auto-size text type: " + autoSizeTextType);
            }
        }
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds. If all the configuration params are valid the type of auto-size is
     * set to {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}.
     *
     * @param autoSizeMinTextSize the minimum text size available for auto-size
     * @param autoSizeMaxTextSize the maximum text size available for auto-size
     * @param autoSizeStepGranularity the auto-size step granularity. It is used in conjunction with
     *                                the minimum and maximum text size in order to build the set of
     *                                text sizes the system uses to choose from when auto-sizing
     * @param unit the desired dimension unit for all sizes above. See {@link TypedValue} for the
     *             possible dimension units
     *
     * @throws IllegalArgumentException if any of the configuration params are invalid.
     *
     * {@link R.attr#autoSizeTextType}
     * {@link R.attr#autoSizeMinTextSize}
     * {@link R.attr#autoSizeMaxTextSize}
     * {@link R.attr#autoSizeStepGranularity}
     *
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     * @see #getAutoSizeMinTextSize()
     * @see #getAutoSizeMaxTextSize()
     * @see #getAutoSizeStepGranularity()
     * @see #getAutoSizeTextAvailableSizes()
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        if (supportsAutoSizeText()) {
            final DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            final float autoSizeMinTextSizeInPx = TypedValue.applyDimension(
                    unit, autoSizeMinTextSize, displayMetrics);
            final float autoSizeMaxTextSizeInPx = TypedValue.applyDimension(
                    unit, autoSizeMaxTextSize, displayMetrics);
            final float autoSizeStepGranularityInPx = TypedValue.applyDimension(
                    unit, autoSizeStepGranularity, displayMetrics);

            validateAndSetAutoSizeTextTypeUniformConfiguration(autoSizeMinTextSizeInPx,
                    autoSizeMaxTextSizeInPx,
                    autoSizeStepGranularityInPx);
            if (setupAutoSizeText()) {
                autoSizeText();
            }
        }
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds. If at least one value from the <code>presetSizes</code> is valid
     * then the type of auto-size is set to {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}.
     *
     * @param presetSizes an {@code int} array of sizes in pixels
     * @param unit the desired dimension unit for the preset sizes above. See {@link TypedValue} for
     *             the possible dimension units
     *
     * @throws IllegalArgumentException if all of the <code>presetSizes</code> are invalid.
     *_
     * {@link R.attr#autoSizeTextType}
     * {@link R.attr#autoSizePresetSizes}
     *
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #getAutoSizeMinTextSize()
     * @see #getAutoSizeMaxTextSize()
     * @see #getAutoSizeTextAvailableSizes()
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit)
            throws IllegalArgumentException {
        if (supportsAutoSizeText()) {
            final int presetSizesLength = presetSizes.length;
            if (presetSizesLength > 0) {
                int[] presetSizesInPx = new int[presetSizesLength];

                if (unit == TypedValue.COMPLEX_UNIT_PX) {
                    presetSizesInPx = Arrays.copyOf(presetSizes, presetSizesLength);
                } else {
                    final DisplayMetrics displayMetrics =
                            mContext.getResources().getDisplayMetrics();
                    // Convert all to sizes to pixels.
                    for (int i = 0; i < presetSizesLength; i++) {
                        presetSizesInPx[i] = Math.round(TypedValue.applyDimension(unit,
                                presetSizes[i], displayMetrics));
                    }
                }

                mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(presetSizesInPx);
                if (!setupAutoSizeUniformPresetSizesConfiguration()) {
                    throw new IllegalArgumentException("None of the preset sizes is valid: "
                            + Arrays.toString(presetSizes));
                }
            } else {
                mHasPresetAutoSizeValues = false;
            }

            if (setupAutoSizeText()) {
                autoSizeText();
            }
        }
    }

    /**
     * Returns the type of auto-size set for this widget.
     *
     * @return an {@code int} corresponding to one of the auto-size types:
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * {@link R.attr#autoSizeTextType}
     *
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @TextViewCompat.AutoSizeTextType
    int getAutoSizeTextType() {
        return mAutoSizeTextType;
    }

    /**
     * @return the current auto-size step granularity in pixels.
     *
     * {@link R.attr#autoSizeStepGranularity}
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int getAutoSizeStepGranularity() {
        return Math.round(mAutoSizeStepGranularityInPx);
    }

    /**
     * @return the current auto-size minimum text size in pixels (the default is 12sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * {@link R.attr#autoSizeMinTextSize}
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int getAutoSizeMinTextSize() {
        return Math.round(mAutoSizeMinTextSizeInPx);
    }

    /**
     * @return the current auto-size maximum text size in pixels (the default is 112sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * {@link R.attr#autoSizeMaxTextSize}
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int getAutoSizeMaxTextSize() {
        return Math.round(mAutoSizeMaxTextSizeInPx);
    }

    /**
     * @return the current auto-size {@code int} sizes array (in pixels).
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    int[] getAutoSizeTextAvailableSizes() {
        return mAutoSizeTextSizesInPx;
    }

    private void setupAutoSizeUniformPresetSizes(TypedArray textSizes) {
        final int textSizesLength = textSizes.length();
        final int[] parsedSizes = new int[textSizesLength];

        if (textSizesLength > 0) {
            for (int i = 0; i < textSizesLength; i++) {
                parsedSizes[i] = textSizes.getDimensionPixelSize(i, -1);
            }
            mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(parsedSizes);
            setupAutoSizeUniformPresetSizesConfiguration();
        }
    }

    private boolean setupAutoSizeUniformPresetSizesConfiguration() {
        final int sizesLength = mAutoSizeTextSizesInPx.length;
        mHasPresetAutoSizeValues = sizesLength > 0;
        if (mHasPresetAutoSizeValues) {
            mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM;
            mAutoSizeMinTextSizeInPx = mAutoSizeTextSizesInPx[0];
            mAutoSizeMaxTextSizeInPx = mAutoSizeTextSizesInPx[sizesLength - 1];
            mAutoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        }
        return mHasPresetAutoSizeValues;
    }

    // Returns distinct sorted positive values.
    private int[] cleanupAutoSizePresetSizes(int[] presetValues) {
        final int presetValuesLength = presetValues.length;
        if (presetValuesLength == 0) {
            return presetValues;
        }
        Arrays.sort(presetValues);

        final List<Integer> uniqueValidSizes = new ArrayList<>();
        for (int i = 0; i < presetValuesLength; i++) {
            final int currentPresetValue = presetValues[i];

            if (currentPresetValue > 0
                    && Collections.binarySearch(uniqueValidSizes, currentPresetValue) < 0) {
                uniqueValidSizes.add(currentPresetValue);
            }
        }

        if (presetValuesLength == uniqueValidSizes.size()) {
            return presetValues;
        } else {
            final int uniqueValidSizesLength = uniqueValidSizes.size();
            final int[] cleanedUpSizes = new int[uniqueValidSizesLength];
            for (int i = 0; i < uniqueValidSizesLength; i++) {
                cleanedUpSizes[i] = uniqueValidSizes.get(i);
            }
            return cleanedUpSizes;
        }
    }

    /**
     * If all params are valid then save the auto-size configuration.
     *
     * @throws IllegalArgumentException if any of the params are invalid
     */
    private void validateAndSetAutoSizeTextTypeUniformConfiguration(
            float autoSizeMinTextSizeInPx,
            float autoSizeMaxTextSizeInPx,
            float autoSizeStepGranularityInPx) throws IllegalArgumentException {
        // First validate.
        if (autoSizeMinTextSizeInPx <= 0) {
            throw new IllegalArgumentException("Minimum auto-size text size ("
                    + autoSizeMinTextSizeInPx  + "px) is less or equal to (0px)");
        }

        if (autoSizeMaxTextSizeInPx <= autoSizeMinTextSizeInPx) {
            throw new IllegalArgumentException("Maximum auto-size text size ("
                    + autoSizeMaxTextSizeInPx + "px) is less or equal to minimum auto-size "
                    + "text size (" + autoSizeMinTextSizeInPx + "px)");
        }

        if (autoSizeStepGranularityInPx <= 0) {
            throw new IllegalArgumentException("The auto-size step granularity ("
                    + autoSizeStepGranularityInPx + "px) is less or equal to (0px)");
        }

        // All good, persist the configuration.
        mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM;
        mAutoSizeMinTextSizeInPx = autoSizeMinTextSizeInPx;
        mAutoSizeMaxTextSizeInPx = autoSizeMaxTextSizeInPx;
        mAutoSizeStepGranularityInPx = autoSizeStepGranularityInPx;
        mHasPresetAutoSizeValues = false;
    }

    private boolean setupAutoSizeText() {
        if (supportsAutoSizeText()
                && mAutoSizeTextType == TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM) {
            // Calculate the sizes set based on minimum size, maximum size and step size if we do
            // not have a predefined set of sizes or if the current sizes array is empty.
            if (!mHasPresetAutoSizeValues || mAutoSizeTextSizesInPx.length == 0) {
                // Calculate sizes to choose from based on the current auto-size configuration.
                final int autoSizeValuesLength = ((int) Math.floor((mAutoSizeMaxTextSizeInPx
                        - mAutoSizeMinTextSizeInPx) / mAutoSizeStepGranularityInPx)) + 1;
                final int[] autoSizeTextSizesInPx = new int[autoSizeValuesLength];
                for (int i = 0; i < autoSizeValuesLength; i++) {
                    autoSizeTextSizesInPx[i] = Math.round(
                            mAutoSizeMinTextSizeInPx + (i * mAutoSizeStepGranularityInPx));
                }
                mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(autoSizeTextSizesInPx);
            }
            mNeedsAutoSizeText = true;
        } else {
            mNeedsAutoSizeText = false;
        }

        return mNeedsAutoSizeText;
    }

    /**
     * Automatically computes and sets the text size.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void autoSizeText() {
        if (!isAutoSizeEnabled()) {
            return;
        }

        if (mNeedsAutoSizeText) {
            if (mTextView.getMeasuredHeight() <= 0 || mTextView.getMeasuredWidth() <= 0) {
                return;
            }

            final boolean horizontallyScrolling = mImpl.isHorizontallyScrollable(mTextView);
            final int availableWidth = horizontallyScrolling
                    ? VERY_WIDE
                    : mTextView.getMeasuredWidth() - mTextView.getTotalPaddingLeft()
                            - mTextView.getTotalPaddingRight();
            final int availableHeight = mTextView.getHeight() - mTextView.getCompoundPaddingBottom()
                    - mTextView.getCompoundPaddingTop();

            if (availableWidth <= 0 || availableHeight <= 0) {
                return;
            }

            synchronized (TEMP_RECTF) {
                TEMP_RECTF.setEmpty();
                TEMP_RECTF.right = availableWidth;
                TEMP_RECTF.bottom = availableHeight;
                final float optimalTextSize = findLargestTextSizeWhichFits(TEMP_RECTF);
                if (optimalTextSize != mTextView.getTextSize()) {
                    setTextSizeInternal(TypedValue.COMPLEX_UNIT_PX, optimalTextSize);
                }
            }
        }
        // Always try to auto-size if enabled. Functions that do not want to trigger auto-sizing
        // after the next layout pass should set this to false.
        mNeedsAutoSizeText = true;
    }

    private void clearAutoSizeConfiguration() {
        mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        mAutoSizeMinTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeMaxTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeTextSizesInPx = new int[0];
        mNeedsAutoSizeText = false;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setTextSizeInternal(int unit, float size) {
        Resources res = mContext == null
                ? Resources.getSystem()
                : mContext.getResources();

        setRawTextSize(TypedValue.applyDimension(unit, size, res.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != mTextView.getPaint().getTextSize()) {
            mTextView.getPaint().setTextSize(size);

            boolean isInLayout = false;
            if (Build.VERSION.SDK_INT >= 18) {
                isInLayout = Api18Impl.isInLayout(mTextView);
            }

            if (mTextView.getLayout() != null) {
                // Do not auto-size right after setting the text size.
                mNeedsAutoSizeText = false;

                final String methodName = "nullLayouts";
                try {
                    Method method = getTextViewMethod(methodName);
                    if (method != null) {
                        method.invoke(mTextView);
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to invoke TextView#" + methodName + "() method", ex);
                }

                if (!isInLayout) {
                    mTextView.requestLayout();
                } else {
                    mTextView.forceLayout();
                }

                mTextView.invalidate();
            }
        }
    }

    /**
     * Performs a binary search to find the largest text size that will still fit within the size
     * available to this view.
     */
    private int findLargestTextSizeWhichFits(RectF availableSpace) {
        final int sizesCount = mAutoSizeTextSizesInPx.length;
        if (sizesCount == 0) {
            throw new IllegalStateException("No available text sizes to choose from.");
        }

        int bestSizeIndex = 0;
        int lowIndex = bestSizeIndex + 1;
        int highIndex = sizesCount - 1;
        int sizeToTryIndex;
        while (lowIndex <= highIndex) {
            sizeToTryIndex = (lowIndex + highIndex) / 2;
            if (suggestedSizeFitsInSpace(mAutoSizeTextSizesInPx[sizeToTryIndex], availableSpace)) {
                bestSizeIndex = lowIndex;
                lowIndex = sizeToTryIndex + 1;
            } else {
                highIndex = sizeToTryIndex - 1;
                bestSizeIndex = highIndex;
            }
        }

        return mAutoSizeTextSizesInPx[bestSizeIndex];
    }

    @VisibleForTesting
    void initTempTextPaint(final int suggestedSizeInPx) {
        if (mTempTextPaint == null) {
            mTempTextPaint = new TextPaint();
        } else {
            mTempTextPaint.reset();
        }
        mTempTextPaint.set(mTextView.getPaint());
        mTempTextPaint.setTextSize(suggestedSizeInPx);
    }

    @VisibleForTesting
    @NonNull
    StaticLayout createLayout(
            @NonNull CharSequence text,
            @NonNull Layout.Alignment alignment,
            int availableWidth,
            int maxLines
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.createStaticLayoutForMeasuring(
                    text, alignment, availableWidth, maxLines, mTextView, mTempTextPaint, mImpl);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return Api16Impl.createStaticLayoutForMeasuring(
                    text, alignment, availableWidth, mTextView, mTempTextPaint);
        } else {
            return createStaticLayoutForMeasuringPre16(text, alignment, availableWidth);
        }
    }

    private boolean suggestedSizeFitsInSpace(int suggestedSizeInPx, RectF availableSpace) {
        CharSequence text = mTextView.getText();
        TransformationMethod transformationMethod = mTextView.getTransformationMethod();
        if (transformationMethod != null) {
            CharSequence transformedText = transformationMethod.getTransformation(text, mTextView);
            if (transformedText != null) {
                text = transformedText;
            }
        }

        final int maxLines = Build.VERSION.SDK_INT >= 16 ? Api16Impl.getMaxLines(mTextView) : -1;
        initTempTextPaint(suggestedSizeInPx);

        // Needs reflection call due to being private.
        Layout.Alignment alignment = invokeAndReturnWithDefault(
                mTextView, "getLayoutAlignment", Layout.Alignment.ALIGN_NORMAL);
        final StaticLayout layout = createLayout(text, alignment, Math.round(availableSpace.right),
                maxLines);
        // Lines overflow.
        if (maxLines != -1 && (layout.getLineCount() > maxLines
                || layout.getLineEnd(layout.getLineCount() - 1) != text.length())) {
            return false;
        }

        // Height overflow.
        if (layout.getHeight() > availableSpace.bottom) {
            return false;
        }

        return true;
    }


    private StaticLayout createStaticLayoutForMeasuringPre16(CharSequence text,
            Layout.Alignment alignment, int availableWidth) {
        // The default values have been inlined with the StaticLayout defaults.

        final float lineSpacingMultiplier = accessAndReturnWithDefault(mTextView,
                "mSpacingMult", 1.0f);
        final float lineSpacingAdd = accessAndReturnWithDefault(mTextView,
                "mSpacingAdd", 0.0f);
        final boolean includePad = accessAndReturnWithDefault(mTextView,
                "mIncludePad", true);

        return new StaticLayout(text, mTempTextPaint, availableWidth,
                alignment,
                lineSpacingMultiplier,
                lineSpacingAdd,
                includePad);
    }

    @SuppressWarnings("unchecked")
    // This is marked package-protected so that it doesn't require a synthetic accessor
    // when being used from the Impl inner classes
    static <T> T invokeAndReturnWithDefault(@NonNull Object object,
            @NonNull final String methodName, @NonNull final T defaultValue) {
        T result = null;
        boolean exceptionThrown = false;

        try {
            // Cache lookup.
            Method method = getTextViewMethod(methodName);
            result = (T) method.invoke(object);
        } catch (Exception ex) {
            exceptionThrown = true;
            Log.w(TAG, "Failed to invoke TextView#" + methodName + "() method", ex);
        } finally {
            if (result == null && exceptionThrown) {
                result = defaultValue;
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T accessAndReturnWithDefault(@NonNull Object object,
            @NonNull final String fieldName, @NonNull final T defaultValue) {
        try {
            final Field field = getTextViewField(fieldName);
            if (field == null) {
                return defaultValue;
            }

            return (T) field.get(object);
        }  catch (IllegalAccessException e) {
            Log.w(TAG, "Failed to access TextView#" + fieldName + " member", e);
            return defaultValue;
        }
    }

    @Nullable
    private static Method getTextViewMethod(@NonNull final String methodName) {
        try {
            Method method = sTextViewMethodByNameCache.get(methodName);
            if (method == null) {
                method = TextView.class.getDeclaredMethod(methodName);
                if (method != null) {
                    method.setAccessible(true);
                    // Cache update.
                    sTextViewMethodByNameCache.put(methodName, method);
                }
            }

            return method;
        } catch (Exception ex) {
            Log.w(TAG, "Failed to retrieve TextView#" + methodName + "() method", ex);
            return null;
        }
    }

    @Nullable
    private static Field getTextViewField(@NonNull final String fieldName) {
        try {
            Field field = sTextViewFieldByNameCache.get(fieldName);
            if (field == null) {
                field = TextView.class.getDeclaredField(fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    sTextViewFieldByNameCache.put(fieldName, field);
                }
            }

            return field;
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "Failed to access TextView#" + fieldName + " member", e);
            return null;
        }
    }

    /**
     * @return {@code true} if this widget supports auto-sizing text and has been configured to
     * auto-size.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    boolean isAutoSizeEnabled() {
        return supportsAutoSizeText()
                && mAutoSizeTextType != TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    }

    /**
     * @return {@code true} if this TextView supports auto-sizing text to fit within its container.
     */
    private boolean supportsAutoSizeText() {
        // Auto-size only supports TextView and all siblings but EditText.
        return !(mTextView instanceof AppCompatEditText);
    }

    @RequiresApi(23)
    private static final class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        @NonNull
        static StaticLayout createStaticLayoutForMeasuring(
                @NonNull CharSequence text,
                @NonNull Layout.Alignment alignment,
                int availableWidth,
                int maxLines,
                @NonNull TextView textView,
                @NonNull TextPaint tempTextPaint,
                @NonNull Impl impl
        ) {
            final StaticLayout.Builder layoutBuilder = StaticLayout.Builder.obtain(
                    text, 0, text.length(),  tempTextPaint, availableWidth);

            layoutBuilder.setAlignment(alignment)
                    .setLineSpacing(
                            textView.getLineSpacingExtra(),
                            textView.getLineSpacingMultiplier())
                    .setIncludePad(textView.getIncludeFontPadding())
                    .setBreakStrategy(textView.getBreakStrategy())
                    .setHyphenationFrequency(textView.getHyphenationFrequency())
                    .setMaxLines(maxLines == -1 ? Integer.MAX_VALUE : maxLines);

            try {
                // Can use the StaticLayout.Builder (along with TextView params added in or after
                // API 23) to construct the layout.
                impl.computeAndSetTextDirection(layoutBuilder, textView);
            } catch (ClassCastException e) {
                // On some devices this exception happens, details: b/127137059.
                Log.w(TAG, "Failed to obtain TextDirectionHeuristic, auto size may be incorrect");
            }
            return layoutBuilder.build();
        }
    }

    @RequiresApi(18)
    private static final class Api18Impl {
        private Api18Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isInLayout(@NonNull View view) {
            return view.isInLayout();
        }
    }

    @RequiresApi(16)
    private static final class Api16Impl {
        private Api16Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getMaxLines(@NonNull TextView textView) {
            return textView.getMaxLines();
        }

        @DoNotInline
        @NonNull
        static StaticLayout createStaticLayoutForMeasuring(
                @NonNull CharSequence text,
                @NonNull Layout.Alignment alignment,
                int availableWidth,
                @NonNull TextView textView,
                @NonNull TextPaint tempTextPaint
        ) {
            final float lineSpacingMultiplier = textView.getLineSpacingMultiplier();
            final float lineSpacingAdd = textView.getLineSpacingExtra();
            final boolean includePad = textView.getIncludeFontPadding();

            // The layout could not be constructed using the builder so fall back to the
            // most broad constructor.
            return new StaticLayout(text, tempTextPaint, availableWidth,
                    alignment,
                    lineSpacingMultiplier,
                    lineSpacingAdd,
                    includePad);
        }
    }
}
