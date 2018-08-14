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

package androidx.core.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface which allows a {@link android.widget.TextView} to receive background auto-sizing calls
 * from {@link TextViewCompat} when running on API v26 devices or lower.
 *
 * @hide Internal use only
 */
@RestrictTo(LIBRARY_GROUP)
public interface AutoSizeableTextView {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    boolean PLATFORM_SUPPORTS_AUTOSIZE = Build.VERSION.SDK_INT >= 27;

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds by using the default auto-size configuration.
     *
     * @param autoSizeTextType the type of auto-size. Must be one of
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * @see #getAutoSizeTextType()
     */
    void setAutoSizeTextTypeWithDefaults(@TextViewCompat.AutoSizeTextType int autoSizeTextType);

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
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     * @see #getAutoSizeMinTextSize()
     * @see #getAutoSizeMaxTextSize()
     * @see #getAutoSizeStepGranularity()
     * @see #getAutoSizeTextAvailableSizes()
     */
    void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException;

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
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #getAutoSizeMinTextSize()
     * @see #getAutoSizeMaxTextSize()
     * @see #getAutoSizeTextAvailableSizes()
     */
    void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit)
            throws IllegalArgumentException;

    /**
     * Returns the type of auto-size set for this widget.
     *
     * @return an {@code int} corresponding to one of the auto-size types:
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * @see #setAutoSizeTextTypeWithDefaults(int)
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     */
    @TextViewCompat.AutoSizeTextType
    int getAutoSizeTextType();

    /**
     * @return the current auto-size step granularity in pixels.
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     */
    int getAutoSizeStepGranularity();

    /**
     * @return the current auto-size minimum text size in pixels (the default is 12sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     */
    int getAutoSizeMinTextSize();

    /**
     * @return the current auto-size maximum text size in pixels (the default is 112sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     */
    int getAutoSizeMaxTextSize();

    /**
     * @return the current auto-size {@code int} sizes array (in pixels).
     *
     * @see #setAutoSizeTextTypeUniformWithConfiguration(int, int, int, int)
     * @see #setAutoSizeTextTypeUniformWithPresetSizes(int[], int)
     */
    int[] getAutoSizeTextAvailableSizes();
}
