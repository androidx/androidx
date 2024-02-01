/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange

/**
 * The editing buffer
 *
 * This class manages the all editing relate states, editing buffers, selection, styles, etc.
 */
internal class EditingBuffer(
    /**
     * The initial text of this editing buffer
     */
    text: AnnotatedString,
    /**
     * The initial selection range of this buffer.
     * If you provide collapsed selection, it is treated as the cursor position. The cursor and
     * selection cannot exists at the same time.
     * The selection must points the valid index of the initialText, otherwise
     * IndexOutOfBoundsException will be thrown.
     */
    selection: TextRange
) {
    internal companion object {
        const val NOWHERE = -1
    }

    private val gapBuffer = PartialGapBuffer(text.text)

    val changeTracker = ChangeTracker()

    /**
     * The inclusive selection start offset
     */
    var selectionStart = selection.start
        private set(value) {
            require(value >= 0) { "Cannot set selectionStart to a negative value: $value" }
            field = value
        }

    /**
     * The exclusive selection end offset
     */
    var selectionEnd = selection.end
        private set(value) {
            require(value >= 0) { "Cannot set selectionEnd to a negative value: $value" }
            field = value
        }

    /**
     * The inclusive composition start offset
     *
     * If there is no composing text, returns -1
     */
    var compositionStart = NOWHERE
        private set

    /**
     * The exclusive composition end offset
     *
     * If there is no composing text, returns -1
     */
    var compositionEnd = NOWHERE
        private set

    /**
     * Helper function that returns true if the editing buffer has composition text
     */
    fun hasComposition(): Boolean = compositionStart != NOWHERE

    /**
     * Returns the composition information as TextRange. Returns null if no
     * composition is set.
     */
    val composition: TextRange?
        get() = if (hasComposition()) {
            TextRange(compositionStart, compositionEnd)
        } else null

    /**
     * Returns the selection information as TextRange
     */
    val selection: TextRange
        get() = TextRange(selectionStart, selectionEnd)

    /**
     * Helper accessor for cursor offset
     */
    /*VisibleForTesting*/
    var cursor: Int
        /**
         * Return the cursor offset.
         *
         * Since selection and cursor cannot exist at the same time, return -1 if there is a
         * selection.
         */
        get() = if (selectionStart == selectionEnd) selectionEnd else -1
        /**
         * Set the cursor offset.
         *
         * Since selection and cursor cannot exist at the same time, cancel selection if there is.
         */
        set(cursor) = setSelection(cursor, cursor)

    /**
     * [] operator for the character at the index.
     */
    operator fun get(index: Int): Char = gapBuffer[index]

    /**
     * Returns the length of the buffer.
     */
    val length: Int get() = gapBuffer.length

    constructor(
        text: String,
        selection: TextRange
    ) : this(AnnotatedString(text), selection)

    init {
        checkRange(selection.start, selection.end)
    }

    /**
     * Replace the text and move the cursor to the end of inserted text.
     *
     * This function cancels selection if there is any.
     *
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer
     */
    fun replace(start: Int, end: Int, text: CharSequence) {
        checkRange(start, end)
        val min = minOf(start, end)
        val max = maxOf(start, end)

        // coerce the replacement bounds before tracking change. This is mostly necessary for
        // composition based typing when each keystroke may trigger a replace function that looks
        // like "abcd" => "abcde".

        // coerce min
        var i = 0
        var cMin = min
        while (cMin < max && i < text.length && text[i] == gapBuffer[cMin]) {
            i++
            cMin++
        }
        // coerce max
        var j = text.length
        var cMax = max
        while (cMax > min && j > i && text[j - 1] == gapBuffer[cMax - 1]) {
            j--
            cMax--
        }

        changeTracker.trackChange(cMin, cMax, j - i)

        gapBuffer.replace(min, max, text)

        // On Android, all text modification APIs also provides explicit cursor location. On the
        // other hand, desktop applications usually don't. So, here tentatively move the cursor to
        // the end offset of the editing area for desktop like application. In case of Android,
        // implementation will call setSelection immediately after replace function to update this
        // tentative cursor location.
        selectionStart = min + text.length
        selectionEnd = min + text.length

        // Similarly, if text modification happens, cancel ongoing composition. If caller wants to
        // change the composition text, it is caller's responsibility to call setComposition again
        // to set composition range after replace function.
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    /**
     * Remove the given range of text.
     *
     * Different from replace method, this doesn't move cursor location to the end of modified text.
     * Instead, preserve the selection with adjusting the deleted text.
     */
    fun delete(start: Int, end: Int) {
        checkRange(start, end)
        val deleteRange = TextRange(start, end)

        changeTracker.trackChange(start, end, 0)

        gapBuffer.replace(deleteRange.min, deleteRange.max, "")

        val newSelection = updateRangeAfterDelete(
            TextRange(selectionStart, selectionEnd),
            deleteRange
        )
        selectionStart = newSelection.start
        selectionEnd = newSelection.end

        if (hasComposition()) {
            val compositionRange = TextRange(compositionStart, compositionEnd)
            val newComposition = updateRangeAfterDelete(compositionRange, deleteRange)
            if (newComposition.collapsed) {
                commitComposition()
            } else {
                compositionStart = newComposition.min
                compositionEnd = newComposition.max
            }
        }
    }

    /**
     * Mark the specified area of the text as selected text.
     *
     * You can set cursor by specifying the same value to `start` and `end`.
     * The reversed range is not allowed.
     * @param start the inclusive start offset of the selection
     * @param end the exclusive end offset of the selection
     */
    fun setSelection(start: Int, end: Int) {
        val clampedStart = start.coerceIn(0, length)
        val clampedEnd = end.coerceIn(0, length)

        selectionStart = clampedStart
        selectionEnd = clampedEnd
    }

    /**
     * Mark the specified area of the text as composition text.
     *
     * The empty range or reversed range is not allowed.
     * Use clearComposition in case of clearing composition.
     *
     * @param start the inclusive start offset of the composition
     * @param end the exclusive end offset of the composition
     *
     * @throws IndexOutOfBoundsException if start or end offset is ouside of current buffer
     * @throws IllegalArgumentException if start is larger than or equal to end. (reversed or
     *                                  collapsed range)
     */
    fun setComposition(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (end < 0 || end > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}"
            )
        }
        if (start >= end) {
            throw IllegalArgumentException("Do not set reversed or empty range: $start > $end")
        }

        compositionStart = start
        compositionEnd = end
    }

    /**
     * Commits the ongoing composition text and reset the composition range.
     */
    fun commitComposition() {
        compositionStart = NOWHERE
        compositionEnd = NOWHERE
    }

    override fun toString(): String = gapBuffer.toString()

    fun toAnnotatedString(): AnnotatedString = AnnotatedString(toString())

    private fun checkRange(start: Int, end: Int) {
        if (start < 0 || start > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${gapBuffer.length}"
            )
        }

        if (end < 0 || end > gapBuffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${gapBuffer.length}"
            )
        }
    }
}

/**
 * Returns the updated TextRange for [target] after the [deleted] TextRange is deleted as a Pair.
 *
 * If the [deleted] Range covers the whole target, Pair(-1,-1) is returned.
 */
/*@VisibleForTesting*/
internal fun updateRangeAfterDelete(target: TextRange, deleted: TextRange): TextRange {
    var targetMin = target.min
    var targetMax = target.max

    // Following figure shows the deletion range and composition range.
    // |---| represents deleted range.
    // |===| represents target range.
    if (deleted.intersects(target)) {
        if (deleted.contains(target)) {
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :      |-------------|
            //   Target     :          |======|
            //
            // Result:
            //   Buffer     : ABCDETUVWXYZ
            //   Target     :
            targetMin = deleted.min
            targetMax = targetMin
        } else if (target.contains(deleted)) {
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :          |------|
            //   Target     :        |==========|
            //
            // Result:
            //   Buffer     : ABCDEFGHIQRSTUVWXYZ
            //   Target     :        |===|
            targetMax -= deleted.length
        } else if (deleted.contains(targetMin)) {
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :      |---------|
            //   Target     :            |========|
            //
            // Result:
            //   Buffer     : ABCDEFPQRSTUVWXYZ
            //   Target     :       |=====|
            targetMin = deleted.min
            targetMax -= deleted.length
        } else { // deleteRange contains myMax
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :         |---------|
            //   Target     :    |=======|
            //
            // Result:
            //   Buffer     : ABCDEFGHSTUVWXYZ
            //   Target     :    |====|
            targetMax = deleted.min
        }
    } else {
        if (targetMax <= deleted.min) {
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :            |-------|
            //   Target     :  |=======|
            //
            // Result:
            //   Buffer     : ABCDEFGHIJKLTUVWXYZ
            //   Target     :  |=======|
            // do nothing
        } else {
            // Input:
            //   Buffer     : ABCDEFGHIJKLMNOPQRSTUVWXYZ
            //   Deleted    :  |-------|
            //   Target     :            |=======|
            //
            // Result:
            //   Buffer     : AJKLMNOPQRSTUVWXYZ
            //   Target     :    |=======|
            targetMin -= deleted.length
            targetMax -= deleted.length
        }
    }

    return TextRange(targetMin, targetMax)
}
