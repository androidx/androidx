/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.window.layout.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.UiContext
import androidx.annotation.VisibleForTesting
import androidx.window.layout.WindowMetricsCalculator
import kotlin.math.pow

/** Helper class to determine the engagement mode of the device. */
internal interface EngagementModeHelper {
    /**
     * Returns `true` if the device has a display larger than or equal to
     * [EngagementModeHelperImpl.DIAGONAL_INCH_SQUARED_LARGE_LOWER_BOUND].
     */
    fun hasLargeEnoughDisplay(@UiContext uiContext: Context): Boolean

    companion object {
        /** Returns an instance of [EngagementModeHelper]. */
        fun getInstance(): EngagementModeHelper {
            return EngagementModeHelperImpl()
        }
    }
}

/** Implementation of [EngagementModeHelper]. */
internal class EngagementModeHelperImpl(
    private val windowMetricsCalculator: WindowMetricsCalculator =
        WindowMetricsCalculator.getOrCreate()
) : EngagementModeHelper {

    override fun hasLargeEnoughDisplay(uiContext: Context): Boolean {
        val windowMetrics = windowMetricsCalculator.computeMaximumWindowMetrics(uiContext)
        val metrics = getDisplayMetrics(uiContext)
        val widthInchesSquared = (windowMetrics.bounds.width().toDouble() / metrics.xdpi).pow(2)
        val heightInchesSquared = (windowMetrics.bounds.height().toDouble() / metrics.ydpi).pow(2)

        val diagonalInchesSquared = widthInchesSquared + heightInchesSquared
        return diagonalInchesSquared >= DIAGONAL_INCH_SQUARED_LARGE_LOWER_BOUND
    }

    // TODO: b/486181581 - Remove once added to WindowMetrics
    /** Returns the [DisplayMetrics] for the given [uiContext], depending on the SDK level. */
    @Suppress("DEPRECATION")
    @VisibleForTesting
    fun getDisplayMetrics(@UiContext uiContext: Context): DisplayMetrics {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val realMetrics = DisplayMetrics()
                uiContext.display?.let {
                    it.getRealMetrics(realMetrics)
                    return realMetrics
                }
            } catch (e: UnsupportedOperationException) {
                // Use original context if display access is not supported
            }
        }
        return uiContext.resources.displayMetrics
    }

    companion object {
        /**
         * Lower bound of large diagonal screen size in inches, indicating device is productivity
         * and cursor-friendly.
         */
        private const val DIAGONAL_INCH_SQUARED_LARGE_LOWER_BOUND = 11 * 11
    }
}
