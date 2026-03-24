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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.AnnotatedString

/**
 * A [TextStyleBuffer] implemented as an interval tree. It is also order-aware; styles are returned
 * in the order they were added.
 */
internal class TextStyleBuffer<T>(source: TextStyleBuffer<T>? = null) {
    private val intervalTree: IntIntervalTree<T> =
        source?.let { IntIntervalTree(it.intervalTree) } ?: IntIntervalTree()

    /**
     * Similar to a [GapBuffer], this buffer utilizes a "gap" to optimize performance when
     * insertions and deletions are localized around the cursor index. This allows consecutive edits
     * to simply move the gap instead of iterating over and updating the ranges of all styles
     * following the edit index.
     */
    var gapStart: Int
    var gapEnd: Int
    private val gapLength: Int
        get() = gapEnd - gapStart

    init {
        if (source != null) {
            gapStart = source.gapStart
            gapEnd = source.gapEnd
        } else {
            gapStart = 0
            gapEnd = DEFAULT_GAP_LENGTH
        }
    }

    /**
     * Adds the style defined between an interval defined by [start] and [end].
     *
     * @param style The style to be added.
     * @param start The start index of the interval, inclusive.
     * @param end The end index of the interval, must be > [start], exclusive.
     * @return true if the style is added, false otherwise.
     */
    fun addStyle(style: T, start: Int, end: Int): Boolean {
        val startInBuffer = originalIndexToGapBuffer(start)
        val endInBuffer = originalIndexToGapBuffer(end)
        return intervalTree.addInterval(style, startInBuffer, endInBuffer)
    }

    /*
     * Returns the styles that overlap with the interval defined by [start] and [end]. The overlap
     * is inclusive on [start] but exclusive at the [end].
     *
     * @return a list of [AnnotatedString.Range]s representing the styles in the buffer in the order
     *   they are added.
     */
    fun getStyles(start: Int, end: Int): List<AnnotatedString.Range<T>> {
        if (start > end) return emptyList()
        val startInBuffer = originalIndexToGapBuffer(start)
        val endInBuffer = originalIndexToGapBuffer(end)

        val result = mutableListOf<AnnotatedString.Range<T>>()
        intervalTree.forEachIntervalInRange(startInBuffer, endInBuffer) {
            item,
            intervalStart,
            intervalEnd ->
            result.add(
                AnnotatedString.Range(
                    item = item,
                    start = gapBufferToOriginalIndex(intervalStart),
                    end = gapBufferToOriginalIndex(intervalEnd),
                )
            )
        }
        return result
    }

    /**
     * Returns all styles in the buffer.
     *
     * This method is equivalent to call [getStyles] with full range but is optimized to be faster,
     * especially when a large number of styles are stored.
     *
     * @return A list of [AnnotatedString.Range]s representing the styles in the buffer in the order
     *   they were added.
     */
    fun getAllStyles(): List<AnnotatedString.Range<T>> {
        val result = mutableListOf<AnnotatedString.Range<T>>()
        intervalTree.forAllIntervals { item, start, end ->
            result.add(
                AnnotatedString.Range(
                    item = item,
                    start = gapBufferToOriginalIndex(start),
                    end = gapBufferToOriginalIndex(end),
                )
            )
        }
        return result
    }

    /**
     * Removes the style defined between a [start] and an [end] coordinate.
     *
     * @param style The style to be removed.
     * @param start The start index of the interval
     * @param end The end index of the interval, must be > [start]
     * @return true if the style is removed, false otherwise.
     */
    fun removeStyle(style: T, start: Int, end: Int): Boolean {
        val startInBuffer = originalIndexToGapBuffer(start)
        val endInBuffer = originalIndexToGapBuffer(end)

        return intervalTree.removeInterval(style, startInBuffer, endInBuffer)
    }

    /**
     * Updates the style ranges in this [TextStyleBuffer] in response to a text replacement
     * operation between [start] and [end] with a new string of length [newLength].
     *
     * This replacement is interpreted as deleting the range `[start, end)` followed by an insertion
     * at index [start] of [newLength] characters.
     *
     * If a style's range collapses to zero length after the deletion, it is removed from the
     * buffer. Inserting text exactly at the start of a style range will shift the entire style
     * range. For example, for a style at `[5, 10]`, calling `replaceText(start = 5, end = 5,
     * newLength = 10)` will shift the style range to `[15, 20]`.
     *
     * Inserting text exactly at the end of a style range will extend the style range. For example,
     * for a style at `[5, 10]`, calling `replaceText(start = 10, end = 10, newLength = 10)` will
     * extend the style range to `[5, 20]`.
     *
     * TODO(491490169): We currently treat inserted text at [start] as shifting the range, while
     *   inserted text at [end] extends the range. Ideally, this behavior should be customizable.
     */
    fun replaceText(start: Int, end: Int, newLength: Int): Boolean {
        if (intervalTree.isEmpty()) return false
        enlargeGapIfNeeded(newLength - (end - start))

        deleteText(start, end)

        gapStart += newLength
        return true
    }

    // TODO(491490169): This index mapping dictates the range updating behavior described in
    //  replaceText() (i.e., whether insertions shift or extend the style range).
    //  Specifically, an index equal to `gapStart` is mapped after the gap (`index + gapLength`).
    //  This implies:
    //  - A range start == gapStart is mapped after the gap, so the range is shifted after text
    //    insertion.
    //  - A range end == gapStart is mapped after the gap, so the range is extended after text
    //    insertion.
    //  If this behavior is made customizable, this mapping logic will need to be updated to
    //  reflect that.
    private fun originalIndexToGapBuffer(index: Int): Int {
        return if (index < gapStart) {
            index
        } else {
            index + gapLength
        }
    }

    private fun gapBufferToOriginalIndex(index: Int): Int {
        return if (index < gapStart) {
            index
        } else {
            index - gapLength
        }
    }

    /**
     * Updates the style ranges corresponding to deleting the range defined by [start] and [end].
     * This is a helper function for [replaceText]. If you intended to update the [TextStyleBuffer]
     * after text is deleted, call [replaceText] instead.
     */
    private fun deleteText(start: Int, end: Int) {
        if (start < gapStart && end <= gapStart) {
            // The remove happens in the head buffer. Copy the tail part of the head buffer to the
            // tail buffer.
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKLMNOPQ*************RSTUVWXYZ
            //   del region:     |-----|
            //
            // First, move the remaining part of the head buffer to the tail buffer.
            //   buffer:     ABCDEFGHIJKLMNOPQ*****KLKMNOPQRSTUVWXYZ
            //   move data:            ^^^^^^^ =>  ^^^^^^^^
            //
            // Then, delete the given range. (just updating gap positions)
            //   buffer:     ABCD******************KLKMNOPQRSTUVWXYZ
            //   del region:     |-----|
            //
            // Output:       ABCD******************KLKMNOPQRSTUVWXYZ
            moveGapLeft(gapStart - end)
            deleteBeforeGap(end - start)
        } else if (start < gapStart && end >= gapStart) {
            // The remove happens with crossing the gap region. Just update the gap position
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKLMNOPQ************RSTUVWXYZ
            //   del region:             |-------------------|
            //
            // Output:       ABCDEFGHIJKL********************UVWXYZ
            val deleteCountBeforeGap = gapStart - start
            val deleteCountAfterGap = end - gapStart

            deleteBeforeGap(deleteCountBeforeGap)
            deleteAfterGap(deleteCountAfterGap)
        } else { // start > gapStart && end > gapStart
            // The remove happens in the tail buffer. Copy the head part of the tail buffer to the
            // head buffer.
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKL************MNOPQRSTUVWXYZ
            //   del region:                            |-----|
            //
            // First, move the remaining part in the tail buffer to the head buffer.
            //   buffer:     ABCDEFGHIJKLMNO*********MNOPQRSTUVWXYZ
            //   move dat:               ^^^    <=   ^^^
            //
            // Then, delete the given range. (just updating gap positions)
            //   buffer:     ABCDEFGHIJKLMNO******************VWXYZ
            //   del region:                            |-----|
            //
            // Output:       ABCDEFGHIJKLMNO******************VWXYZ

            // Originally it's startInBuffer - gapEnd which equals to
            // start + gapLength - (gapStart + gapLength) and also equals to the start - gapStart
            moveGapRight(start - gapStart)
            deleteAfterGap(end - start)
        }
    }

    /**
     * Move the gap to the left by [count] number of characters. This operation won't change the
     * order of the intervals in this [TextStyleBuffer]. And the red-black tree properties should be
     * well maintained after this operation.
     */
    private fun moveGapLeft(count: Int) {
        if (count == 0) return
        intervalTree.mapIntervals(gapStart - count, gapStart) { value ->
            if (value in gapStart - count until gapStart) {
                value + gapLength
            } else {
                value
            }
        }

        gapStart -= count
        gapEnd -= count
    }

    /** Move the gap to the right by [count] number of characters. */
    private fun moveGapRight(count: Int) {
        if (count == 0) return
        intervalTree.mapIntervals(gapEnd, gapEnd + count) { value ->
            if (value in gapEnd until gapEnd + count) {
                value - gapLength
            } else {
                value
            }
        }
        gapStart += count
        gapEnd += count
    }

    /**
     * Update the style intervals corresponding to deleting [count] characters before the gap this
     * operation won't change the order of the intervals in this [TextStyleBuffer]. And thus the
     * red-black tree properties should be well maintained after this operation.
     */
    private fun deleteBeforeGap(count: Int) {
        if (count == 0) return
        val newGapStart = gapStart - count
        intervalTree.mapIntervals(newGapStart, gapStart) { value ->
            if (value in newGapStart until gapStart) {
                gapEnd
            } else {
                value
            }
        }

        gapStart -= count
    }

    /** Update the style intervals corresponding to deleting [count] characters after the gap. */
    private fun deleteAfterGap(count: Int) {
        if (count == 0) return
        intervalTree.mapIntervals(gapEnd, gapEnd + count) { value ->
            if (value in gapEnd until gapEnd + count) {
                gapEnd + count
            } else {
                value
            }
        }

        gapEnd += count
    }

    private fun enlargeGapIfNeeded(requiredSize: Int) {
        if (intervalTree.isEmpty()) return
        if (gapLength >= requiredSize) return
        val offset = gapLength - requiredSize + DEFAULT_GAP_LENGTH

        intervalTree.mapIntervals(gapStart, Int.MAX_VALUE) { value ->
            if (value >= gapStart) value + offset else value
        }
        gapEnd += offset
    }

    /** Clears all styles from this buffer and prepares it for reuse. */
    fun clear() {
        intervalTree.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextStyleBuffer<T>) return false

        return intervalTree == other.intervalTree
    }

    override fun hashCode(): Int {
        return intervalTree.hashCode()
    }
}

private const val DEFAULT_GAP_LENGTH = 1000
