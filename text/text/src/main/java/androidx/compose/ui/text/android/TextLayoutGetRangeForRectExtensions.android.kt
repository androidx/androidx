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

package androidx.compose.ui.text.android

import android.graphics.RectF
import android.text.Layout
import androidx.compose.ui.text.android.selection.SegmentFinder
import androidx.compose.ui.text.android.selection.WordSegmentFinder
import androidx.compose.ui.text.android.selection.createGraphemeClusterSegmentFinder

/**
 * Compose implementation of [Layout.getRangeForRect], it's mainly used for backward compatibility
 * under API 34.
 */
internal fun TextLayout.getRangeForRect(
    layout: Layout,
    layoutHelper: LayoutHelper,
    rect: RectF,
    @LayoutCompat.TextGranularity granularity: Int,
    inclusionStrategy: (RectF, RectF) -> Boolean
): IntArray? {
    val segmentFinder =
        when (granularity) {
            LayoutCompat.TEXT_GRANULARITY_WORD -> WordSegmentFinder(text, wordIterator)
            else -> createGraphemeClusterSegmentFinder(text, textPaint)
        }

    var startLine = layout.getLineForVertical(rect.top.toInt())
    // Double check that the rect overlaps with the start line. Because getLineForVertical
    // will return the last line if the rect's top is under the last line's bottom.
    if (rect.top > getLineBottom(startLine)) {
        ++startLine
        if (startLine >= lineCount) {
            return null
        }
    }

    var endLine = layout.getLineForVertical(rect.bottom.toInt())
    // Double check that the rect overlaps with the end line. Because getLineForVertical
    // will return the first line if the rect's bottom is above the first line's top.
    if (endLine == 0 && rect.bottom < getLineTop(0)) {
        return null
    }

    var start =
        getStartOrEndOffsetForRectWithinLine(
            layout,
            layoutHelper,
            startLine,
            rect,
            segmentFinder,
            inclusionStrategy,
            getStart = true
        )

    // If the area does not contain any text on this line, keep trying subsequent lines until
    // the end line is reached.
    while (start == -1 && startLine < endLine) {
        ++startLine
        start =
            getStartOrEndOffsetForRectWithinLine(
                layout,
                layoutHelper,
                startLine,
                rect,
                segmentFinder,
                inclusionStrategy,
                getStart = true
            )
    }
    if (start == -1) return null

    var end =
        getStartOrEndOffsetForRectWithinLine(
            layout,
            layoutHelper,
            endLine,
            rect,
            segmentFinder,
            inclusionStrategy,
            getStart = false
        )
    // If the area does not contain any text on this line, keep trying subsequent lines until
    // the end line is reached.
    while (end == -1 && startLine < endLine) {
        --endLine
        end =
            getStartOrEndOffsetForRectWithinLine(
                layout,
                layoutHelper,
                endLine,
                rect,
                segmentFinder,
                inclusionStrategy,
                getStart = false
            )
    }
    if (end == -1) return null

    // If a text segment spans multiple lines or multiple directional runs (e.g. a hyphenated
    // word), then getStartOrEndOffsetForAreaWithinLine() can return an offset in the middle of
    // a text segment. Adjust the range to include the rest of any partial text segments. If
    // start is already the start boundary of a text segment, then this is a no-op.
    start = segmentFinder.previousStartBoundary(start + 1)
    end = segmentFinder.nextEndBoundary(end - 1)

    return intArrayOf(start, end)
}

private fun TextLayout.getStartOrEndOffsetForRectWithinLine(
    layout: Layout,
    layoutHelper: LayoutHelper,
    lineIndex: Int,
    rect: RectF,
    segmentFinder: SegmentFinder,
    inclusionStrategy: (RectF, RectF) -> Boolean,
    getStart: Boolean
): Int {
    val lineTop = layout.getLineTop(lineIndex)
    // In platform, we get line bottom without the line spacing. However, this API is not public
    // under API 34, here we fallback to line bottom with line spacing instead.
    val lineBottom = layout.getLineBottom(lineIndex)

    val lineStart = layout.getLineStart(lineIndex)
    val lineEnd = layout.getLineEnd(lineIndex)
    if (lineStart == lineEnd) {
        return -1
    }
    val horizontalBounds = FloatArray(size = 2 * (lineEnd - lineStart))
    fillLineHorizontalBounds(lineIndex, horizontalBounds)

    val bidiRuns = layoutHelper.getLineBidiRuns(lineIndex)
    val range =
        if (getStart) {
            bidiRuns.indices
        } else {
            // search backwards when finding the end.
            bidiRuns.lastIndex downTo 0
        }

    for (runIndex in range) {
        val bidiRun = bidiRuns[runIndex]
        val runLeft =
            if (bidiRun.isRtl) {
                getCharacterLeftBounds(bidiRun.end - 1, lineStart, horizontalBounds)
            } else {
                getCharacterLeftBounds(bidiRun.start, lineStart, horizontalBounds)
            }

        val runRight =
            if (bidiRun.isRtl) {
                getCharacterRightBounds(bidiRun.start, lineStart, horizontalBounds)
            } else {
                getCharacterRightBounds(bidiRun.end - 1, lineStart, horizontalBounds)
            }

        val result =
            if (getStart) {
                bidiRun.getStartOffsetForRectWithinRun(
                    rect,
                    lineStart,
                    lineTop,
                    lineBottom,
                    runLeft,
                    runRight,
                    horizontalBounds,
                    segmentFinder,
                    inclusionStrategy
                )
            } else {
                bidiRun.getEndOffsetForRectWithinRun(
                    rect,
                    lineStart,
                    lineTop,
                    lineBottom,
                    runLeft,
                    runRight,
                    horizontalBounds,
                    segmentFinder,
                    inclusionStrategy
                )
            }

        if (result >= 0) return result
    }
    return -1
}

private fun LayoutHelper.BidiRun.getStartOffsetForRectWithinRun(
    rect: RectF,
    lineStart: Int,
    lineTop: Int,
    lineBottom: Int,
    runLeft: Float,
    runRight: Float,
    horizontalBounds: FloatArray,
    segmentFinder: SegmentFinder,
    inclusionStrategy: (RectF, RectF) -> Boolean
): Int {
    if (!rect.horizontalOverlap(runLeft, runRight)) {
        return -1
    }
    // Find the first character in the run whose bounds overlap with the area.
    // firstCharOffset is an offset index within the entire text.
    val firstCharOffset: Int
    if ((!isRtl && rect.left <= runLeft) || (isRtl && rect.right >= runRight)) {
        firstCharOffset = start
    } else {
        var low = start
        var high = end
        while (high - low > 1) {
            val guess = (high + low) / 2
            val position = getCharacterLeftBounds(guess, lineStart, horizontalBounds)
            if ((!isRtl && position > rect.left) || (isRtl && position < rect.right)) {
                high = guess
            } else {
                low = guess
            }
        }
        // The area edge is between the left edge of the character at low and the left edge of
        // the character at high. For LTR text, this is within the character at low. For RTL
        // text, this is within the character at high.
        firstCharOffset = if (isRtl) high else low
    }

    var segmentEnd = segmentFinder.nextEndBoundary(firstCharOffset)
    if (segmentEnd == SegmentFinder.DONE) return -1

    var segmentStart = segmentFinder.previousStartBoundary(segmentEnd)
    if (segmentStart >= end) return -1

    segmentStart = segmentStart.coerceAtLeast(start)
    segmentEnd = segmentEnd.coerceAtMost(end)

    val textBounds = RectF(0f, lineTop.toFloat(), 0f, lineBottom.toFloat())
    while (true) {
        textBounds.left =
            if (isRtl) {
                getCharacterLeftBounds(segmentEnd - 1, lineStart, horizontalBounds)
            } else {
                getCharacterLeftBounds(segmentStart, lineStart, horizontalBounds)
            }

        textBounds.right =
            if (isRtl) {
                getCharacterRightBounds(segmentStart, lineStart, horizontalBounds)
            } else {
                getCharacterRightBounds(segmentEnd - 1, lineStart, horizontalBounds)
            }

        if (inclusionStrategy.invoke(textBounds, rect)) {
            return segmentStart
        }

        segmentStart = segmentFinder.nextStartBoundary(segmentStart)
        if (segmentStart == SegmentFinder.DONE || segmentStart >= end) return -1
        segmentEnd = segmentFinder.nextEndBoundary(segmentStart).coerceAtMost(end)
    }
}

private fun LayoutHelper.BidiRun.getEndOffsetForRectWithinRun(
    rect: RectF,
    lineStart: Int,
    lineTop: Int,
    lineBottom: Int,
    runLeft: Float,
    runRight: Float,
    horizontalBounds: FloatArray,
    segmentFinder: SegmentFinder,
    inclusionStrategy: (RectF, RectF) -> Boolean
): Int {
    if (!rect.horizontalOverlap(runLeft, runRight)) {
        // This run doesn't overlap with the given rect.
        return -1
    }

    // Find the last character in the run whose bounds overlap with the area.
    // firstCharOffset is an offset index within the entire text.
    val lastCharOffset: Int
    if ((!isRtl && rect.right >= runRight) || (isRtl && rect.left <= runLeft)) {
        lastCharOffset = end - 1
    } else {
        var low = start
        var high = end
        while (high - low > 1) {
            val guess = (high + low) / 2
            val position = getCharacterLeftBounds(guess, lineStart, horizontalBounds)
            if ((!isRtl && position > rect.right) || (isRtl && position < rect.left)) {
                high = guess
            } else {
                low = guess
            }
        }
        // The area edge is between the left edge of the character at low and the left edge of
        // the character at high. For LTR text, this is within the character at low. For RTL
        // text, this is within the character at high.
        lastCharOffset = if (isRtl) high else low
    }

    var segmentStart = segmentFinder.previousStartBoundary(lastCharOffset + 1)
    if (segmentStart == SegmentFinder.DONE) return -1

    var segmentEnd = segmentFinder.nextEndBoundary(segmentStart)
    if (segmentEnd <= start) return -1

    segmentStart = segmentStart.coerceAtLeast(start)
    segmentEnd = segmentEnd.coerceAtMost(end)

    val textBounds = RectF(0f, lineTop.toFloat(), 0f, lineBottom.toFloat())
    while (true) {
        textBounds.left =
            if (isRtl) {
                getCharacterLeftBounds(segmentEnd - 1, lineStart, horizontalBounds)
            } else {
                getCharacterLeftBounds(segmentStart, lineStart, horizontalBounds)
            }

        textBounds.right =
            if (isRtl) {
                getCharacterRightBounds(segmentStart, lineStart, horizontalBounds)
            } else {
                getCharacterRightBounds(segmentEnd - 1, lineStart, horizontalBounds)
            }

        if (inclusionStrategy.invoke(textBounds, rect)) return segmentEnd

        segmentEnd = segmentFinder.previousEndBoundary(segmentEnd)
        if (segmentEnd == SegmentFinder.DONE || segmentEnd <= start) return -1
        segmentStart = segmentFinder.previousStartBoundary(segmentEnd).coerceAtLeast(start)
    }
}

/**
 * Helper function to return the given character's left bound.
 *
 * @param offset the offset of the character.
 * @param lineStart the start offset of the line the given character belongs to.
 * @param horizontalBounds the horizontal bounds of the characters in the line.
 */
private fun getCharacterLeftBounds(
    offset: Int,
    lineStart: Int,
    horizontalBounds: FloatArray
): Float {
    return horizontalBounds[2 * (offset - lineStart)]
}

/**
 * Helper function to return the given character's right bound.
 *
 * @param offset the offset of the character.
 * @param lineStart the start offset of the line the given character belongs to.
 * @param horizontalBounds the horizontal bounds of the characters in the line.
 */
private fun getCharacterRightBounds(
    offset: Int,
    lineStart: Int,
    horizontalBounds: FloatArray
): Float {
    return horizontalBounds[2 * (offset - lineStart) + 1]
}

private fun RectF.horizontalOverlap(left: Float, right: Float): Boolean {
    return right >= this.left && left <= this.right
}
