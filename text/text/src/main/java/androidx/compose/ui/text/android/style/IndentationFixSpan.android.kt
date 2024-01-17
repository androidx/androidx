/*
 * Copyright 2022 The Android Open Source Project
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

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Layout.Alignment
import android.text.style.LeadingMarginSpan
import androidx.compose.ui.text.android.isLineEllipsized
import kotlin.math.abs

/**
 * When letter spacing, align and ellipsize applied to text, the ellipsized line is indented wrong.
 * For example for an LTR text, the last line is indented in a way where the beginning of the line
 * is less than 0 and the text is cut.
 *
 * This class fixes the indentation for the last broken line by translating the canvas on the
 * opposite direction.
 *
 * It should be applied to a text only when those three attributes are set.
 */
internal class IndentationFixSpan : LeadingMarginSpan {
    override fun getLeadingMargin(firstLine: Boolean): Int {
        return 0
    }

    override fun drawLeadingMargin(
        canvas: Canvas?,
        paint: Paint?,
        left: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?
    ) {
        if (layout != null && paint != null) {
            val lineIndex = layout.getLineForOffset(start)
            if (lineIndex == layout.lineCount - 1 && layout.isLineEllipsized(lineIndex)) {
                val padding = layout.getEllipsizedLeftPadding(lineIndex, paint) +
                    layout.getEllipsizedRightPadding(lineIndex, paint)
                if (padding != 0f) {
                    canvas!!.translate(padding, 0f)
                }
            }
        }
    }
}

/**
 * Returns a positive number that describes the error amount so that we can translate the canvas
 * with the padding amount.
 *
 * Returns a non-zero value only if the line is ellipsized, the alignment is opposite and there is
 * an error in the line position calculation.
 *
 * Used to correct the LTR line with the line position calculation error.
 */
internal fun Layout.getEllipsizedLeftPadding(lineIndex: Int, paint: Paint = this.paint): Float {
    val lineLeft = getLineLeft(lineIndex)
    if (isLineEllipsized(lineIndex) &&
        getParagraphDirection(lineIndex) == Layout.DIR_LEFT_TO_RIGHT &&
        lineLeft < 0
    ) {
        val ellipsisIndex = getLineStart(lineIndex) + getEllipsisStart(lineIndex)
        val horizontal = getPrimaryHorizontal(ellipsisIndex)
        val length = (horizontal - lineLeft) + paint.measureText(EllipsisChar)

        return when (getParagraphAlignment(lineIndex)) {
            Alignment.ALIGN_CENTER -> {
                abs(lineLeft) + ((width - length) / 2f)
            }
            else -> { // align right
                abs(lineLeft) + (width - length)
            }
        }
    }
    return 0f
}

/**
 * Returns a negative number that describes the error amount so that we can translate the canvas
 * with the padding amount.
 *
 * Returns a non-zero value only if the line is ellipsized, the alignment is opposite and there is
 * an error in the line position calculation.
 *
 * Used to correct the RTL line with the line position calculation error.
 */
internal fun Layout.getEllipsizedRightPadding(lineIndex: Int, paint: Paint = this.paint): Float {
    if (isLineEllipsized(lineIndex) &&
        getParagraphDirection(lineIndex) == Layout.DIR_RIGHT_TO_LEFT &&
        width < getLineRight(lineIndex)
    ) {
        val ellipsisIndex = getLineStart(lineIndex) + getEllipsisStart(lineIndex)
        val horizontal = getPrimaryHorizontal(ellipsisIndex)
        val length = (getLineRight(lineIndex) - horizontal) + paint.measureText(EllipsisChar)

        return when (getParagraphAlignment(lineIndex)) {
            Alignment.ALIGN_CENTER -> {
                width - getLineRight(lineIndex) - ((width - length) / 2f)
            }
            else -> { // align left
                width - getLineRight(lineIndex) - (width - length)
            }
        }
    }
    return 0f
}

private const val EllipsisChar = "\u2026"
