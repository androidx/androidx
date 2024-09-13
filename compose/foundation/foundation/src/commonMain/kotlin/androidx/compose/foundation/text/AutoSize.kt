/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/** Interface used by Text composables to automatically size their text. */
internal interface AutoSize {
    /**
     * Calculates font size. Use utility function [FontSizeSearchScope.performLayoutAndGetOverflow]
     * to lay out the text and check if it overflows. The expectation is that
     * implementation-specific constraints should be used in unison with
     * [FontSizeSearchScope.performLayoutAndGetOverflow] to determine a suitable font size to be
     * used.
     *
     * @return The derived optimal font size.
     * @see [FontSizeSearchScope.performLayoutAndGetOverflow]
     */
    // TODO(b/362904946): Add sample
    fun FontSizeSearchScope.getFontSize(): TextUnit

    /**
     * Require equals() to be implemented for performance purposes. Using a data class is
     * sufficient. Singletons may implement this function with referential equality (`this ===
     * other`). Instances with no properties may implement this function by checking the type of the
     * other object.
     *
     * @return true if both AutoSize instances are structurally identical.
     */
    override fun equals(other: Any?): Boolean

    companion object {
        /**
         * Automatically size the text to attempt to fit its container. This uses a
         * step-based/granular implementation where potential font sizes are uniformly spread out
         * between [minFontSize] and [maxFontSize]. [stepSize] is the smallest difference between
         * two distinct font sizes. e.g. if `minFontSize = 1.sp`, `maxFontSize = 2.sp` and `stepSize
         * = 0.5.sp`, the potential font sizes are `1.sp`, `1.5.sp`, and `2.sp`. In cases where
         * [stepSize] is strictly greater than (not equal to) the difference between [minFontSize]
         * and [maxFontSize], the only potential font size is [minFontSize].
         *
         * Both or neither [minFontSize] and [maxFontSize] must be declared.
         *
         * @param minFontSize The smallest potential font size of the text. Default = 12.sp. This
         *   must be smaller than [maxFontSize]; an [IllegalArgumentException] will be thrown
         *   otherwise.
         * @param maxFontSize The largest potential font size of the text. Default = 112.sp. This
         *   must be larger than [minFontSize]; an [IllegalArgumentException] will be thrown
         *   otherwise.
         * @param stepSize The smallest difference between potential font sizes. Specifically, every
         *   font size, when subtracted by [minFontSize], is divisible by [stepSize]. Default =
         *   0.25.sp. This must not be less than `0.0001f.sp`; an [IllegalArgumentException] will be
         *   thrown otherwise.
         * @return AutoSize instance with the step-based configuration. Using this in a compatible
         *   composable will cause its text to be sized as above.
         */
        fun StepBased(
            minFontSize: TextUnit,
            maxFontSize: TextUnit,
            stepSize: TextUnit = 0.25.sp
        ): AutoSize {
            return AutoSizeStepBased(minFontSize, maxFontSize, stepSize)
        }

        /**
         * Automatically size the text to attempt to fit its container. This uses a
         * step-based/granular implementation where potential font sizes are uniformly spread out
         * between `minFontSize` and `maxFontSize`. [stepSize] is the smallest difference between
         * two distinct font sizes. e.g. if `minFontSize = 1.sp`, `maxFontSize = 2.sp` and `stepSize
         * = 0.5.sp`, the potential font sizes are `1.sp`, `1.5.sp`, and `2.sp`. In cases where
         * [stepSize] is strictly greater than (not equal to) the difference between `minFontSize`
         * and `maxFontSize`, the only potential font size is `minFontSize`.
         *
         * Both or neither `minFontSize` and `maxFontSize` must be declared.
         *
         * @param stepSize The smallest difference between potential font sizes. Specifically, every
         *   font size, when subtracted by `minFontSize` (`12.sp`), is divisible by [stepSize].
         *   Default = 0.25.sp. [stepSize] must not be less than `0.0001f.sp`, an
         *   [IllegalArgumentException] will be thrown otherwise.
         * @return AutoSize instance with the step-based configuration. Using this in a compatible
         *   composable will cause its text to be sized as above.
         */
        fun StepBased(stepSize: TextUnit = 0.25.sp): AutoSize {
            return AutoSizeStepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = stepSize)
        }
    }
}

/**
 * This interface is used by classes responsible for laying out text. Layout will be performed here
 * alongside logic that checks if the text overflows.
 *
 * These methods are used by [AutoSize] in the [AutoSize.getFontSize] method, where developers can
 * lay out text with different font sizes and do certain logic depending on whether or not the text
 * overflows.
 *
 * This may be implemented in unit tests when testing [AutoSize.getFontSize] to see if the method
 * works as intended.
 */
internal interface FontSizeSearchScope : Density {
    /**
     * Lays out the text with the given font size.
     *
     * @return true if the text overflows.
     */
    fun performLayoutAndGetOverflow(fontSize: TextUnit): Boolean
}

private class AutoSizeStepBased(
    private var minFontSize: TextUnit,
    private val maxFontSize: TextUnit,
    private val stepSize: TextUnit
) : AutoSize {
    init {
        // Checks for validity of AutoSize instance
        // Unspecified check
        if (minFontSize == TextUnit.Unspecified) {
            throw IllegalArgumentException(
                "AutoSize.StepBased: TextUnit.Unspecified is not a valid value for minFontSize. " +
                    "Try using other values e.g. 10.sp"
            )
        }
        if (maxFontSize == TextUnit.Unspecified) {
            throw IllegalArgumentException(
                "AutoSize.StepBased: TextUnit.Unspecified is not a valid value for maxFontSize. " +
                    "Try using other values e.g. 100.sp"
            )
        }
        if (stepSize == TextUnit.Unspecified) {
            throw IllegalArgumentException(
                "AutoSize.StepBased: TextUnit.Unspecified is not a valid value for stepSize. " +
                    "Try using other values e.g. 0.25.sp"
            )
        }

        // minFontSize maxFontSize comparison check
        if (minFontSize.type == maxFontSize.type && minFontSize > maxFontSize) {
            minFontSize = maxFontSize
        }

        // check if stepSize is too small
        if (stepSize.type == TextUnitType.Sp && stepSize < 0.0001f.sp) {
            throw IllegalArgumentException(
                "AutoSize.StepBased: stepSize must be greater than or equal to 0.0001f.sp"
            )
        }

        // check if minFontSize or maxFontSize are negative
        if (minFontSize.value < 0) {
            throw IllegalArgumentException("AutoSize.StepBased: minFontSize must not be negative")
        }
        if (maxFontSize.value < 0) {
            throw IllegalArgumentException("AutoSize.StepBased: maxFontSize must not be negative")
        }
    }

    override fun FontSizeSearchScope.getFontSize(): TextUnit {
        val stepSize = stepSize.toPx()
        val smallest = minFontSize.toPx()
        val largest = maxFontSize.toPx()
        var min = smallest
        var max = largest

        var current = (min + max) / 2

        while ((max - min) >= stepSize) {
            // overflow indicates that whole text doesn't fit
            if (performLayoutAndGetOverflow(current.toSp())) {
                max = current
            } else {
                min = current
            }
            current = (min + max) / 2
        }
        // used size minus minFontSize must be divisible by stepSize
        current = (floor((min - smallest) / stepSize) * stepSize + smallest)

        // try the next size up and see if it fits
        if (
            (current + stepSize) <= largest &&
                !performLayoutAndGetOverflow((current + stepSize).toSp())
        ) {
            current += stepSize
        }

        return current.toSp()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null) return false
        if (other !is AutoSizeStepBased) return false

        if (other.minFontSize != minFontSize) return false
        if (other.maxFontSize != maxFontSize) return false
        if (other.stepSize != stepSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minFontSize.hashCode()
        result = 31 * result + maxFontSize.hashCode()
        result = 31 * result + stepSize.hashCode()
        return result
    }
}
