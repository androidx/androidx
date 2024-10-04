/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.text.android.style

import android.graphics.Paint.FontMetricsInt
import androidx.annotation.FloatRange
import androidx.compose.ui.text.internal.checkPrecondition
import kotlin.math.abs
import kotlin.math.ceil

/**
 * The span which modifies the height of the covered paragraphs. A paragraph is defined as a segment
 * of string divided by '\n' character. To make sure the span work as expected, the boundary of this
 * span should align with paragraph boundary.
 *
 * @param startIndex The starting index where the span is added to the Spannable, used to identify
 *   if the line height is requested for the first line.
 * @param endIndex The end index where the span is added to the Spannable, used to identify if the
 *   line height is requested for the last line.
 * @param trimFirstLineTop When true, the space that would be added to the top of the first line as
 *   a result of the line height is not added. Single line text is both the first and last line.
 * @param trimLastLineBottom When true, the space that would be added to the bottom of the last line
 *   as a result of the line height is not added. Single line text is both the first and last line.
 * @param lineHeight The specified line height in pixel units, which is the space between the
 *   baseline of adjacent lines.
 * @param topRatio The percentage on how to distribute the line height for a given line. 0 means all
 *   space as a result of line height is applied to the bottom. Similarly, 100 means all space as a
 *   result of line height is applied to the top.
 * @constructor Create a LineHeightSpan which sets the line height to `height` physical pixels.
 */
internal class LineHeightStyleSpan(
    val lineHeight: Float,
    private val startIndex: Int,
    private val endIndex: Int,
    private val trimFirstLineTop: Boolean,
    val trimLastLineBottom: Boolean,
    @FloatRange(from = -1.0, to = 1.0) private val topRatio: Float,
    private val preserveMinimumHeight: Boolean,
) : android.text.style.LineHeightSpan {

    private var firstAscent: Int = Int.MIN_VALUE
    private var ascent: Int = Int.MIN_VALUE
    private var descent: Int = Int.MIN_VALUE
    private var lastDescent: Int = Int.MIN_VALUE

    /** Holds the firstAscent - fontMetricsInt.ascent */
    var firstAscentDiff = 0
        private set

    /** Holds the last lastDescent - fontMetricsInt.descent */
    var lastDescentDiff = 0
        private set

    init {
        checkPrecondition(topRatio in 0f..1f || topRatio == -1f) {
            "topRatio should be in [0..1] range or -1"
        }
    }

    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanStartVertical: Int,
        lineHeight: Int,
        fontMetricsInt: FontMetricsInt
    ) {
        val currentHeight = fontMetricsInt.lineHeight()
        // If current height is not positive, do nothing.
        if (currentHeight <= 0) return

        val isFirstLine = (start == startIndex)
        val isLastLine = (end == endIndex)

        // if single line and should not apply, return
        if (isFirstLine && isLastLine && trimFirstLineTop && trimLastLineBottom) return

        if (firstAscent == Int.MIN_VALUE) {
            calculateTargetMetrics(fontMetricsInt)
        }

        fontMetricsInt.ascent = if (isFirstLine) firstAscent else ascent
        fontMetricsInt.descent = if (isLastLine) lastDescent else descent
    }

    private fun calculateTargetMetrics(fontMetricsInt: FontMetricsInt) {
        val currentHeight = fontMetricsInt.lineHeight()
        val ceiledLineHeight = ceil(lineHeight).toInt()

        // calculate the difference between the current line lineHeight and the requested lineHeight
        val diff = ceiledLineHeight - currentHeight
        if (preserveMinimumHeight && diff <= 0) {
            ascent = fontMetricsInt.ascent
            descent = fontMetricsInt.descent
            firstAscent = ascent
            lastDescent = descent
            firstAscentDiff = 0
            lastDescentDiff = 0
            return
        }

        val ascentRatio =
            if (topRatio == -1f) {
                abs(fontMetricsInt.ascent.toFloat()) / fontMetricsInt.lineHeight()
            } else {
                topRatio
            }

        val descentDiff =
            if (diff <= 0) {
                // diff * topRatio is the amount that should go to below the baseline
                ceil(diff * ascentRatio).toInt()
            } else {
                // diff * (1f - topRatio) is the amount that should go to below the baseline
                ceil(diff * (1f - ascentRatio)).toInt()
            }

        descent = fontMetricsInt.descent + descentDiff
        ascent = descent - ceiledLineHeight

        firstAscent = if (trimFirstLineTop) fontMetricsInt.ascent else ascent
        lastDescent = if (trimLastLineBottom) fontMetricsInt.descent else descent
        firstAscentDiff = fontMetricsInt.ascent - firstAscent
        lastDescentDiff = lastDescent - fontMetricsInt.descent
    }

    internal fun copy(
        startIndex: Int,
        endIndex: Int,
        trimFirstLineTop: Boolean = this.trimFirstLineTop
    ) =
        LineHeightStyleSpan(
            lineHeight = lineHeight,
            startIndex = startIndex,
            endIndex = endIndex,
            trimFirstLineTop = trimFirstLineTop,
            trimLastLineBottom = trimLastLineBottom,
            topRatio = topRatio,
            preserveMinimumHeight = preserveMinimumHeight,
        )
}

internal fun FontMetricsInt.lineHeight(): Int = this.descent - this.ascent
