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

import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * Interface used by Text composables to override text size to automatically grow or shrink text to
 * fill the layout bounds.
 *
 * @sample androidx.compose.foundation.samples.TextAutoSizeBasicTextSample
 */
interface TextAutoSize {
    /**
     * Calculates font size and provides access to [TextAutoSizeLayoutScope], which offers
     * [TextAutoSizeLayoutScope.performLayout] to lay out the text and use the measured size.
     *
     * @sample androidx.compose.foundation.samples.CustomTextAutoSizeSample
     * @return The derived optimal font size
     * @see [TextAutoSizeLayoutScope.performLayout]
     */
    fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString
    ): TextUnit

    /**
     * This type is used in performance-sensitive paths and requires providing equality guarantees.
     * Using a data class is sufficient. Singletons may implement this function with referential
     * equality (`this === other`). Instances with no properties may implement this function by
     * checking the type of the other object.
     *
     * @return true if both AutoSize instances are identical.
     */
    override fun equals(other: Any?): Boolean

    /**
     * This type is used in performance-sensitive paths and requires providing identity guarantees.
     *
     * @return a unique hashcode for this AutoSize instance.
     */
    override fun hashCode(): Int

    companion object {
        /**
         * Automatically size the text with the biggest font size that fits the available space.
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
         *   thrown otherwise. If [stepSize] is greater than the difference between [minFontSize]
         *   and [maxFontSize], [minFontSize] will be used for the layout.
         * @return AutoSize instance with the step-based configuration. Using this in a compatible
         *   composable will cause its text to be sized as above.
         */
        fun StepBased(
            minFontSize: TextUnit = TextAutoSizeDefaults.MinFontSize,
            maxFontSize: TextUnit = TextAutoSizeDefaults.MaxFontSize,
            stepSize: TextUnit = 0.25.sp
        ): TextAutoSize =
            AutoSizeStepBased(
                minFontSize = minFontSize,
                maxFontSize = maxFontSize,
                stepSize = stepSize
            )
    }
}

/** Contains defaults for [TextAutoSize] APIs. */
object TextAutoSizeDefaults {
    /** The default minimum font size for [TextAutoSize]. */
    val MinFontSize = 12.sp

    /** The default maximum font size for [TextAutoSize]. */
    val MaxFontSize = 112.sp
}

private class AutoSizeStepBased(
    private var minFontSize: TextUnit,
    private val maxFontSize: TextUnit,
    private val stepSize: TextUnit
) : TextAutoSize {
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

    private val TextLayoutResult.isLastLineEllipsized: Boolean
        get() = if (lineCount > 0) isLineEllipsized(lineCount - 1) else false

    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString
    ): TextUnit {
        val stepSize = stepSize.toPx()
        val smallest = minFontSize.toPx()
        val largest = maxFontSize.toPx()
        var min = smallest
        var max = largest

        var current = (min + max) / 2

        while ((max - min) >= stepSize) {
            val layoutResult = performLayout(constraints, text, current.toSp())
            val didOverflow =
                when (layoutResult.layoutInput.overflow) {
                    TextOverflow.Clip,
                    TextOverflow.Visible -> {
                        layoutResult.didOverflowWidth || layoutResult.didOverflowHeight
                    }
                    TextOverflow.StartEllipsis,
                    TextOverflow.MiddleEllipsis,
                    TextOverflow.Ellipsis -> {
                        // If any line was ellipsized, we've overflowed.
                        var lineIndex = 0
                        while (lineIndex < layoutResult.lineCount) {
                            if (layoutResult.isLineEllipsized(lineIndex)) break
                            lineIndex++
                        }
                        lineIndex > 0
                    }
                    else ->
                        throw IllegalArgumentException(
                            "TextOverflow type" +
                                " ${layoutResult.layoutInput.overflow} is not supported."
                        )
                }
            if (didOverflow) {
                max = current
            } else {
                min = current
            }
            current = (min + max) / 2
        }
        // used size minus minFontSize must be divisible by stepSize
        current = (floor((min - smallest) / stepSize) * stepSize + smallest)

        // We have found a size that fits, but we can still try one step up
        if ((current + stepSize) <= largest) {
            val layoutResult = performLayout(constraints, text, (current + stepSize).toSp())
            val didOverflow = layoutResult.didOverflowWidth || layoutResult.didOverflowHeight
            if (!didOverflow) {
                current += stepSize
            }
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
