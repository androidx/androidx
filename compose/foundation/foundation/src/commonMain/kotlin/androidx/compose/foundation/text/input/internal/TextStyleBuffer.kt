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

import androidx.compose.foundation.internal.throwIllegalStateException
import androidx.compose.foundation.internal.throwIllegalStateExceptionForNullCheck
import androidx.compose.foundation.text.input.ExpandPolicy
import androidx.compose.foundation.text.input.TrackedRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange

/**
 * A [TextStyleBuffer] implemented as an interval tree. It is also order-aware; styles are returned
 * in the order they were added.
 *
 * @param source The [TextStyleBuffer] to copy from.
 * @param mutable Whether this [TextStyleBuffer] is mutable.
 */
internal class TextStyleBuffer<T>(
    source: TextStyleBuffer<T>? = null,
    private val mutable: Boolean = true,
) {
    internal val id: Any = Any()
    val intervalTree: IntIntervalTree<T> = source?.intervalTree?.copy() ?: IntIntervalTree()

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
     * Returns an immutable copy of this [TextStyleBuffer].
     *
     * @return A copy of this [TextStyleBuffer].
     */
    fun toImmutable(): TextStyleBuffer<T> = TextStyleBuffer(this, false)

    /**
     * Syncs this [TextStyleBuffer] with another [TextStyleBuffer].
     *
     * @param other The [TextStyleBuffer] to sync with.
     */
    fun syncTo(other: TextStyleBuffer<T>) {
        if (!mutable) {
            throwIllegalStateException("This buffer is immutable")
        }
        gapStart = other.gapStart
        gapEnd = other.gapEnd
        intervalTree.syncTo(other.intervalTree)
    }

    /**
     * Adds the style defined between an interval defined by [interval].
     *
     * The `flag1` and `flag2` properties in the [Interval] are utilized to control whether the
     * style expands when text is inserted exactly at the boundary using [replaceText].
     * - `flag1` indicates `startExpands`. If true, text inserted at `start` will be included in the
     *   style range.
     * - `flag2` indicates `endExpands`. If true, text inserted at `end` will be included in the
     *   style range.
     *
     * @param style The style to be added.
     * @param interval The interval where the style will be added.
     * @return A [TrackedRange] that tracks the range of the newly added style.
     */
    inline fun <reified R : T> addStyle(style: R, interval: Interval): TrackedRange<R> {
        if (!mutable) {
            throwIllegalStateException("This TextStyleBuffer is immutable")
        }

        val startInBuffer =
            originalIndexToGapBuffer(
                index = interval.start,
                isStart = true,
                expand = interval.startExpands,
            )
        val endInBuffer =
            originalIndexToGapBuffer(
                index = interval.end,
                isStart = false,
                expand = interval.endExpands,
            )

        val intervalInBuffer =
            Interval(startInBuffer, endInBuffer, interval.startExpands, interval.endExpands)
        val intervalHandle = intervalTree.addInterval(style, intervalInBuffer)
        return TrackedRange(id, intervalHandle)
    }

    /**
     * Remove the style associated with the [trackedRange].
     *
     * @return true if the style is found and removed, false otherwise.
     */
    fun removeStyle(trackedRange: TrackedRange<*>): Boolean {
        if (!mutable) {
            throwIllegalStateException("This buffer is immutable")
        }
        if (trackedRange.creatorId !== id) return false

        return intervalTree.removeInterval(trackedRange.intervalHandle)
    }

    /**
     * Returns the styles with type [R] that overlap with the interval defined by [start] and [end].
     * The overlap is inclusive on [start] but exclusive at the [end].
     *
     * @return a list of [TrackedRange]s representing the styles in the buffer in the order they are
     *   added.
     */
    inline fun <reified R : T> getStyles(start: Int, end: Int): List<TrackedRange<R>> {
        if (start > end) return emptyList()
        val rangeInBuffer = originalToGapBufferForGetStyle(start, end)
        return intervalTree.findIntervalsInRange<R, TrackedRange<R>>(
            rangeInBuffer.start,
            rangeInBuffer.end,
        ) { packedHandle ->
            TrackedRange(id, IntervalHandle(packedHandle))
        }
    }

    /**
     * Similar to [getStyles] but return a list of immutable [AnnotatedString.Range]. Returns the
     * styles with type [R] that overlap with the interval defined by [start] and [end]. The overlap
     * is inclusive on [start] but exclusive at the [end].
     *
     * @return a list of [AnnotatedString.Range]s representing the styles in the buffer in the order
     *   they are added.
     */
    inline fun <reified R : T> getImmutableStyles(
        start: Int,
        end: Int,
    ): List<AnnotatedString.Range<R>> {
        if (start > end) return emptyList()
        val rangeInBuffer = originalToGapBufferForGetStyle(start, end)
        return intervalTree.findIntervalsInRange<R, AnnotatedString.Range<R>>(
            rangeInBuffer.start,
            rangeInBuffer.end,
        ) { packedHandle ->
            val intervalHandle = IntervalHandle(packedHandle)
            val item =
                intervalTree.getItem<R>(intervalHandle)
                    ?: throwIllegalStateExceptionForNullCheck(
                        "IntIntervalTree's item should not be null"
                    )
            val interval = intervalTree.getInterval(intervalHandle)
            AnnotatedString.Range(
                item,
                gapBufferToOriginalIndex(interval.start),
                gapBufferToOriginalIndex(interval.end),
            )
        }
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
        intervalTree.forAllIntervals { item, packedInterval ->
            val interval = Interval(packedInterval)
            result.add(
                AnnotatedString.Range(
                    item = item,
                    start = gapBufferToOriginalIndex(interval.start),
                    end = gapBufferToOriginalIndex(interval.end),
                )
            )
        }
        return result
    }

    internal fun isValid(trackedRange: TrackedRange<*>): Boolean {
        if (trackedRange.creatorId !== id) return false
        trackedRange.intervalHandle =
            intervalTree.refreshIntervalHandle(trackedRange.intervalHandle)
        return trackedRange.intervalHandle != IntervalHandle.Invalid
    }

    internal fun setRange(trackedRange: TrackedRange<*>, range: TextRange) {
        if (!mutable) {
            throwIllegalStateException("This TextStyleBuffer is immutable")
        }
        require(range.start < range.end) { "Reversed or collapsed range is not accepted." }
        val interval = intervalTree.getInterval(trackedRange.activeIntervalHandle)
        val start =
            originalIndexToGapBuffer(range.start, isStart = true, expand = interval.startExpands)
        val end = originalIndexToGapBuffer(range.end, isStart = false, expand = interval.endExpands)

        val newInterval = Interval(start, end, interval.startExpands, interval.endExpands)
        intervalTree.updateInterval(trackedRange.activeIntervalHandle, newInterval)
    }

    internal fun getRange(trackedRange: TrackedRange<*>): TextRange {
        val interval = intervalTree.getInterval(trackedRange.activeIntervalHandle)
        val start = gapBufferToOriginalIndex(interval.start)
        val end = gapBufferToOriginalIndex(interval.end)
        return TextRange(start, end)
    }

    internal fun getExpandPolicy(trackedRange: TrackedRange<*>): ExpandPolicy {
        val interval = intervalTree.getInterval(trackedRange.activeIntervalHandle)
        return ExpandPolicy(interval.startExpands, interval.endExpands)
    }

    internal fun setExpandPolicy(trackedRange: TrackedRange<*>, expandPolicy: ExpandPolicy) {
        if (!mutable) {
            throwIllegalStateException("This TextStyleBuffer is immutable")
        }
        val interval = intervalTree.getInterval(trackedRange.activeIntervalHandle)
        val originalStart = gapBufferToOriginalIndex(interval.start)
        val originalEnd = gapBufferToOriginalIndex(interval.end)

        // When the expand policy changes, the underlying mapping of the interval boundaries to the
        // gap buffer needs to be updated. If a boundary falls exactly at the gap, its position in
        // the gap buffer (either before or after the gap) depends on its expand flag.
        val newStart =
            originalIndexToGapBuffer(
                originalStart,
                isStart = true,
                expand = expandPolicy.startExpands,
            )
        val newEnd =
            originalIndexToGapBuffer(originalEnd, isStart = false, expand = expandPolicy.endExpands)

        val newInterval =
            Interval(newStart, newEnd, expandPolicy.startExpands, expandPolicy.endExpands)
        intervalTree.updateInterval(trackedRange.activeIntervalHandle, newInterval)
    }

    internal inline fun <reified R : T> getItem(trackedRange: TrackedRange<*>): R? {
        return intervalTree.getItem<R>(trackedRange.activeIntervalHandle)
    }

    internal fun <R : T> setItem(trackedRange: TrackedRange<R>, item: R) {
        if (!mutable) {
            throwIllegalStateException("This TextStyleBuffer is immutable")
        }
        intervalTree.updateItem(trackedRange.activeIntervalHandle, item)
    }

    internal val TrackedRange<*>.activeIntervalHandle: IntervalHandle
        get() {
            require(creatorId === id) { "TrackedRange belongs to a different TextFieldBuffer." }
            intervalHandle = intervalTree.refreshIntervalHandle(intervalHandle)
            if (intervalHandle == IntervalHandle.Invalid) {
                throw IllegalStateException("TrackedRange is not found.")
            }
            return intervalHandle
        }

    /**
     * Updates the style ranges in this [TextStyleBuffer] in response to a text replacement
     * operation in the range `[start, end)` with a new string of length [newLength].
     *
     * This replacement is interpreted as deleting the text in the range `[start, end)`, followed by
     * an insertion of [newLength] characters at index [start].
     *
     * Behavior for style ranges affected by the replacement:
     * - **Deletion**: If a style's range collapses to zero length after the deletion, it is removed
     *   from the buffer.
     * - **Insertion at the start**: Inserting text exactly at the start of a style range will
     *   either expand the style to include the inserted text or shift the style to after the
     *   inserted text, depending on the `startExpands`(flag1) property of the [Interval] when the
     *   style was added. If `startExpands` is `true`, it expands. If `false`, it shifts. For
     *   example, for a style at `[5, 10)` with `startExpands = false`, calling `replaceText(start =
     *   5, end = 5, newLength = 10)` will shift the style to `[15, 20)`.
     * - **Insertion at the end**: Inserting text exactly at the end of a style range will either
     *   extend the style range to include the inserted text or remain unchanged, depending on the
     *   `endExpands`(flag2) property of the [Interval] when the style was added. If `endExpands` is
     *   `true`, it extends. If `false`, it remains unchanged. For example, for a style at `[5, 10)`
     *   with `endExpands = true`, calling `replaceText(start = 10, end = 10, newLength = 10)` will
     *   extend the style to `[5, 20)`.
     */
    fun replaceText(start: Int, end: Int, newLength: Int): Boolean {
        if (!mutable) {
            throwIllegalStateException("This buffer is immutable")
        }
        if (intervalTree.isEmpty()) return false
        enlargeGapIfNeeded(newLength - (end - start))

        deleteText(start, end)

        gapStart += newLength
        return true
    }

    /**
     * Finds the next transition point (the start or end of any style) from the given [start] up to
     * the [limit].
     *
     * A transition represents a position where the applied interval items (e.g., text styles) might
     * potentially change.
     *
     * @param start The index to start searching from (exclusive).
     * @param limit The maximum index to search up to (inclusive). If no transition is found before
     *   this index, [limit] is returned.
     * @return The index of the next transition, or [limit] if none exists.
     */
    fun nextTransition(start: Int, limit: Int): Int {
        require(limit >= start) { "limit ($limit) cannot be less than start ($start)" }
        if (start == limit) return limit

        val startInBuffer = if (start < gapStart) start else start + gapLength
        val limitInBuffer = if (limit < gapStart) limit else limit + gapLength

        val transitionInBuffer = intervalTree.nextTransition(startInBuffer, limitInBuffer)
        return gapBufferToOriginalIndex(transitionInBuffer)
    }

    /**
     * Finds the previous transition point (the start or end of any style) from the given [start]
     * down to the [limit].
     *
     * A transition represents a position where the applied interval items (e.g., text styles) might
     * potentially change.
     *
     * @param start The index to start searching backwards from (exclusive).
     * @param limit The minimum index to search down to (inclusive). If no transition is found
     *   before this index, [limit] is returned.
     * @return The index of the previous transition, or [limit] if none exists.
     */
    fun previousTransition(start: Int, limit: Int): Int {
        require(limit <= start) { "limit ($limit) cannot be greater than start ($start)" }
        if (start == limit) return limit

        val startInBuffer = if (start <= gapStart) start else start + gapLength
        val limitInBuffer = if (limit <= gapStart) limit else limit + gapLength

        val transitionInBuffer = intervalTree.previousTransition(startInBuffer, limitInBuffer)
        return gapBufferToOriginalIndex(transitionInBuffer)
    }

    /**
     * Maps an index in the original text to the corresponding index in the `gapBuffer`.
     *
     * The start and end of the range are mapped differently based on whether they expand. If the
     * gap is included in the mapped range, the newly inserted text is considered part of the range;
     * otherwise, it is excluded.
     *
     * ```
     * a b c * * * d e f
     *       [         )  expanding start
     *             [   )  non-expanding start
     *   [         )      expanding end
     *   [   )            non-expanding end
     * ```
     *
     * @param index The index in the original text.
     * @return The mapped index in the `gapBuffer`.
     */
    private fun originalIndexToGapBuffer(index: Int, isStart: Boolean, expand: Boolean): Int {
        return if (index < gapStart) {
            index
        } else if (index == gapStart) {
            // When a range start expands, it should be mapped before the gap, and vice versa.
            // When a range end expands, it should be mapped after the gap, and vice versa.
            if (isStart xor expand) {
                index + gapLength
            } else {
                index
            }
        } else {
            index + gapLength
        }
    }

    private fun gapBufferToOriginalIndex(index: Int): Int {
        return if (index <= gapStart) {
            index
        } else {
            index - gapLength
        }
    }

    /**
     * Helper method to map a range in original text to the [TextRange] in gap buffer for getStyle
     * purposes.
     */
    private fun originalToGapBufferForGetStyle(start: Int, end: Int): TextRange {
        val startInBuffer: Int
        val endInBuffer: Int
        if (start == end && start == gapStart) {
            // Handle a collapsed query exactly at the gap start. We map it to [gapEnd, gapEnd].
            //
            // Example:
            // - A style exists at [10, 20] with a non-expanding start.
            // - A gap is inserted at 10 with length 100.
            // - The style's range is mapped to [110, 120] in the gap buffer.
            // - We query for styles at the collapsed range [10, 10].
            //
            // If we used the standard mapping logic below, the query [10, 10] would be mapped
            // to [10, 110). The interval tree would fail to find an overlap between the query
            // [10, 110) and the style [110, 120].
            //
            // By explicitly mapping the query to [110, 110] (i.e., [gapEnd, gapEnd]), the
            // interval tree correctly identifies the overlap with [110, 120].
            //
            // Note: This mapping would theoretically miss a collapsed style [10, 10] that
            // mapped to [10, 110), but TextStyleBuffer does not support collapsed styles.
            startInBuffer = gapEnd
            endInBuffer = gapEnd
        } else {
            // Map the query boundaries to cover the largest possible range in the gap buffer.
            // By treating both the start and end as expanding.
            startInBuffer = originalIndexToGapBuffer(start, isStart = true, expand = true)
            endInBuffer = originalIndexToGapBuffer(end, isStart = false, expand = true)
        }
        return TextRange(startInBuffer, endInBuffer)
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
     * Moves the gap to the left by [count] characters.
     *
     * The start and end of the range are mapped differently based on whether they expand.
     *
     * Example: `moveGapLeft(3)`
     *
     * ```
     * Input:
     * a b c d e f * * * * g h i j
     *       [                   ) expanding start at 3
     *       [                   ) non-expanding start at 3
     *             [             ) expanding start at 6
     *                     [     ) non-expanding start at 6
     * [     )                     expanding end at 3
     * [     )                     non-expanding end at 3
     * [           )               non-expanding end at 6
     * [                   )       expanding end at 6
     *
     * Output:
     * a b c * * * * d e f g h i j
     *       [                   ) expanding start at 3
     *               [           ) non-expanding start at 3
     *                     [     ) expanding start at 6
     *                     [     ) non-expanding start at 6
     * [               )           expanding end at 3
     * [     )                     non-expanding end at 3
     * [                   )       expanding end at 6
     * [                   )       non-expanding end at 6
     * ```
     *
     * @param count The number of characters to shift the gap to the left. Must be non-negative and
     *   less than or equal to the number of characters preceding the gap.
     */
    private fun moveGapLeft(count: Int) {
        if (count == 0) return
        val newGapStart = gapStart - count
        intervalTree.mapIntervals(newGapStart, gapStart) { packedInterval ->
            val interval = Interval(packedInterval)
            val newStart =
                if (interval.start == newGapStart && interval.startExpands) {
                    interval.start
                } else if (interval.start in newGapStart..gapStart) {
                    interval.start + gapLength
                } else {
                    interval.start
                }

            val newEnd =
                if (interval.end == newGapStart && !interval.endExpands) {
                    interval.end
                } else if (interval.end in newGapStart..gapStart) {
                    interval.end + gapLength
                } else {
                    interval.end
                }
            Interval(newStart, newEnd, interval.startExpands, interval.endExpands).packed
        }

        gapStart -= count
        gapEnd -= count
    }

    /**
     * Moves the gap to the right by [count] characters.
     *
     * The start and end of the range are mapped differently based on whether they expand.
     *
     * Example: `moveGapRight(3)`
     *
     * ```
     * Input:
     * a b c d * * * * e f g h i j
     *         [                   ) expanding start at 4
     *                 [           ) non-expanding start at 4
     *                       [     ) expanding start at 7
     *                       [     ) non-expanding start at 7
     * [               )             expanding end at 4
     * [       )                     non-expanding end at 4
     * [                     )       expanding end at 7
     * [                     )       non-expanding end at 7
     *
     * Output:
     * a b c d e f g * * * * h i j
     *         [                   ) expanding start at 4
     *         [                   ) non-expanding start at 4
     *               [             ) expanding start at 7
     *                       [     ) non-expanding start at 7
     * [       )                     expanding end at 4
     * [       )                     non-expanding end at 4
     * [                     )       expanding end at 7
     * [             )               non-expanding end at 7
     * ```
     *
     * @param count The number of characters to shift the gap to the right. Must be non-negative and
     *   less than or equal to the number of characters following the gap.
     */
    private fun moveGapRight(count: Int) {
        if (count == 0) return
        val newGapEnd = gapEnd + count
        intervalTree.mapIntervals(gapEnd, newGapEnd) { packedInterval ->
            val interval = Interval(packedInterval)

            val newStart =
                if (interval.start == newGapEnd && interval.startExpands) {
                    interval.start - gapLength
                } else if (interval.start in gapEnd until newGapEnd) {
                    interval.start - gapLength
                } else {
                    interval.start
                }

            val newEnd =
                if (interval.end == newGapEnd && !interval.endExpands) {
                    interval.end - gapLength
                } else if (interval.end in gapEnd until newGapEnd) {
                    interval.end - gapLength
                } else {
                    interval.end
                }

            Interval(newStart, newEnd, interval.startExpands, interval.endExpands).packed
        }
        gapStart += count
        gapEnd += count
    }

    /**
     * Updates the style ranges to reflect the deletion of [count] characters before the gap.
     *
     * The start and end of the range are mapped differently based on whether they expand.
     *
     * Example: `deleteBeforeGap(3)`
     *
     * ```
     * Input:
     * a b c d e f * * * * g h i j
     *       [                   ) expanding start at 3
     *       [                   ) non-expanding start at 3
     *             [             ) expanding start at 6
     *                     [     ) non-expanding start at 6
     * [     )                     expanding end at 3
     * [     )                     non-expanding end at 3
     * [           )               non-expanding end at 6
     * [                   )       expanding end at 6
     *
     * Output:
     * a b c * * * * * * * g h i j
     *       [                   ) expanding start at 3
     *                     [     ) non-expanding start at 3
     *       [                   ) expanding start at 3 (was 6)
     *                     [     ) non-expanding start at 3 (was 6)
     * [                   )       expanding end at 3
     * [     )                     non-expanding end at 3
     * [     )                     non-expanding end at 3 (was 6)
     * [                   )       expanding end at 3 (was 6)
     * ```
     *
     * @param count The number of characters to delete before the gap. Must be non-negative and less
     *   than or equal to the number of characters preceding the gap.
     */
    private fun deleteBeforeGap(count: Int) {
        if (count == 0) return
        val newGapStart = gapStart - count
        intervalTree.mapIntervals(newGapStart, gapStart) { packedInterval ->
            val interval = Interval(packedInterval)

            val newStart =
                if (interval.start == gapStart) {
                    newGapStart
                } else if (interval.start in newGapStart until gapStart) {
                    if (interval.startExpands) {
                        newGapStart
                    } else {
                        gapEnd
                    }
                } else {
                    interval.start
                }

            val newEnd =
                if (interval.end == gapStart) {
                    newGapStart
                } else if (interval.end in newGapStart until gapStart) {
                    if (interval.endExpands) {
                        gapEnd
                    } else {
                        newGapStart
                    }
                } else {
                    interval.end
                }
            // When newStart and newEnd only covers the gap, it's essentially an empty range.
            // We collapsed it so that it'll be removed by mapIntervals.
            val newInterval =
                if (newStart >= newEnd || (newStart == newGapStart && newEnd == gapEnd)) {
                    Interval(newStart, newStart)
                } else {
                    Interval(newStart, newEnd, interval.startExpands, interval.endExpands)
                }

            // Return the raw value to avoid auto-box
            newInterval.packed
        }

        gapStart -= count
    }

    /**
     * Updates the style ranges to reflect the deletion of [count] characters after the gap.
     *
     * The start and end of the range are mapped differently based on whether they expand.
     *
     * Example: `deleteAfterGap(3)`
     *
     * ```
     * Input:
     * a b c d * * * * e f g h i j
     *         [                   ) expanding start at 4
     *                 [           ) non-expanding start at 4
     *                       [     ) expanding start at 7
     *                       [     ) non-expanding start at 7
     * [               )             expanding end at 4
     * [       )                     non-expanding end at 4
     * [                     )       expanding end at 7
     * [                     )       non-expanding end at 7
     *
     * Output:
     * a b c d * * * * * * * h i j
     *         [                 ) expanding start at 4
     *                       [   ) non-expanding start at 4
     *         [                 ) expanding start at 4 (was 7)
     *                       [   ) non-expanding start at 4 (was 7)
     * [                     )     expanding end at 4
     * [       )                   non-expanding end at 4
     * [                     )     expanding end at 4 (was 7)
     * [       )                   non-expanding end at 4 (was 7)
     * ```
     *
     * @param count The number of characters to delete after the gap. Must be non-negative and less
     *   than or equal to the number of characters following the gap.
     */
    private fun deleteAfterGap(count: Int) {
        if (count == 0) return
        val newGapEnd = gapEnd + count
        intervalTree.mapIntervals(gapEnd, newGapEnd) { packedInterval ->
            val interval = Interval(packedInterval)
            val newStart =
                if (interval.start == gapEnd) {
                    newGapEnd
                } else if (interval.start in gapEnd..newGapEnd) {
                    if (interval.startExpands) {
                        gapStart
                    } else {
                        newGapEnd
                    }
                } else {
                    interval.start
                }

            val newEnd =
                if (interval.end == gapEnd) {
                    newGapEnd
                } else if (interval.end in gapEnd..newGapEnd) {
                    if (interval.endExpands) {
                        newGapEnd
                    } else {
                        gapStart
                    }
                } else {
                    interval.end
                }

            // When newStart and newEnd only covers the gap, it's essentially an empty range.
            // We collapsed it so that it'll be removed by mapIntervals.
            val newInterval =
                if (newStart >= newEnd || (newStart == gapStart && newEnd == newGapEnd)) {
                    Interval(newStart, newStart)
                } else {
                    Interval(newStart, newEnd, interval.startExpands, interval.endExpands)
                }

            // Return the raw value to avoid auto-box.
            newInterval.packed
        }

        gapEnd += count
    }

    private fun enlargeGapIfNeeded(requiredSize: Int) {
        if (intervalTree.isEmpty()) return
        if (gapLength >= requiredSize) return
        val offset = gapLength - requiredSize + DEFAULT_GAP_LENGTH

        intervalTree.mapIntervals(gapStart, Int.MAX_VALUE) { packedInterval ->
            val interval = Interval(packedInterval)
            val newStart =
                if (interval.start > gapStart) interval.start + offset else interval.start
            val newEnd = if (interval.end > gapStart) interval.end + offset else interval.end
            Interval(newStart, newEnd, interval.startExpands, interval.endExpands).packed
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
        if (gapStart != other.gapStart) return false
        if (gapEnd != other.gapEnd) return false

        return intervalTree == other.intervalTree
    }

    override fun hashCode(): Int {
        var result = intervalTree.hashCode()
        result = 31 * result + gapStart
        result = 31 * result + gapEnd
        return result
    }
}

private const val DEFAULT_GAP_LENGTH = 1000

/**
 * In [TextStyleBuffer] the customizable [Interval.flag1] is used to indicates whether the style
 * expands when text inserted at [Interval.start].
 */
private val Interval.startExpands: Boolean
    get() = flag1

/**
 * In [TextStyleBuffer] the customizable [Interval.flag2] is used to indicates whether the style
 * expands when text inserted at [Interval.end].
 */
private val Interval.endExpands: Boolean
    get() = flag2
