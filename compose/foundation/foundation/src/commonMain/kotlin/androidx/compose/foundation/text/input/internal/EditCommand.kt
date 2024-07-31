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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.text.findFollowingBreak
import androidx.compose.foundation.text.findPrecedingBreak
import androidx.compose.foundation.text.input.PlacedAnnotation

/**
 * Commit final [text] to the text box and set the new cursor position.
 *
 * See
 * [`commitText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)).
 *
 * @param text The text to commit.
 * @param newCursorPosition The cursor position after inserted text.
 */
internal fun EditingBuffer.commitText(text: String, newCursorPosition: Int) {
    // API description says replace ongoing composition text if there. Then, if there is no
    // composition text, insert text into cursor position or replace selection.
    if (hasComposition()) {
        replace(compositionStart, compositionEnd, text)
    } else {
        // In this editing buffer, insert into cursor or replace selection are equivalent.
        replace(selectionStart, selectionEnd, text)
    }

    // After replace function is called, the editing buffer places the cursor at the end of the
    // modified range.
    val newCursor = cursor

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer =
        if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

    cursor = newCursorInBuffer.coerceIn(0, length)
}

/**
 * Mark a certain region of text as composing text.
 *
 * See
 * [`setComposingRegion`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingRegion(int,%2520int)).
 *
 * @param start The inclusive start offset of the composing region.
 * @param end The exclusive end offset of the composing region
 */
internal fun EditingBuffer.setComposingRegion(start: Int, end: Int) {
    // The API description says, different from SetComposingText, SetComposingRegion must
    // preserve the ongoing composition text and set new composition.
    if (hasComposition()) {
        commitComposition()
    }

    // Sanitize the input: reverse if reversed, clamped into valid range, ignore empty range.
    val clampedStart = start.coerceIn(0, length)
    val clampedEnd = end.coerceIn(0, length)
    if (clampedStart == clampedEnd) {
        // do nothing. empty composition range is not allowed.
    } else if (clampedStart < clampedEnd) {
        setComposition(clampedStart, clampedEnd)
    } else {
        setComposition(clampedEnd, clampedStart)
    }
}

/**
 * Replace the currently composing text with the given text, and set the new cursor position. Any
 * composing text set previously will be removed automatically.
 *
 * See
 * [`setComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingText(java.lang.CharSequence,%2520int)).
 *
 * @param text The composing text.
 * @param newCursorPosition The cursor position after setting composing text.
 * @param annotations Text annotations that IME attaches to the composing region. e.g. background
 *   color or underline styling.
 */
internal fun EditingBuffer.setComposingText(
    text: String,
    newCursorPosition: Int,
    annotations: List<PlacedAnnotation>? = null
) {
    if (hasComposition()) {
        // API doc says, if there is ongoing composing text, replace it with new text.
        val compositionStart = compositionStart
        replace(compositionStart, compositionEnd, text)
        if (text.isNotEmpty()) {
            setComposition(compositionStart, compositionStart + text.length, annotations)
        }
    } else {
        // If there is no composing text, insert composing text into cursor position with
        // removing selected text if any.
        val selectionStart = selectionStart
        replace(selectionStart, selectionEnd, text)
        if (text.isNotEmpty()) {
            setComposition(selectionStart, selectionStart + text.length, annotations)
        }
    }

    // After replace function is called, the editing buffer places the cursor at the end of the
    // modified range.
    val newCursor = cursor

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer =
        if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

    cursor = newCursorInBuffer.coerceIn(0, length)
}

/**
 * Delete [lengthBeforeCursor] characters of text before the current cursor position, and delete
 * [lengthAfterCursor] characters of text after the current cursor position, excluding the
 * selection.
 *
 * Before and after refer to the order of the characters in the string, not to their visual
 * representation.
 *
 * See
 * [`deleteSurroundingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingText(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in UTF-16 before the cursor to be deleted.
 *   Must be non-negative.
 * @param lengthAfterCursor The number of characters in UTF-16 after the cursor to be deleted. Must
 *   be non-negative.
 */
internal fun EditingBuffer.deleteSurroundingText(lengthBeforeCursor: Int, lengthAfterCursor: Int) {
    requirePrecondition(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
        "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
            "$lengthBeforeCursor and $lengthAfterCursor respectively."
    }

    // calculate the end with safe addition since lengthAfterCursor can be set to e.g. Int.MAX
    // by the input
    val end = selectionEnd.addExactOrElse(lengthAfterCursor) { length }
    delete(selectionEnd, minOf(end, length))

    // calculate the start with safe subtraction since lengthBeforeCursor can be set to e.g.
    // Int.MAX by the input
    val start = selectionStart.subtractExactOrElse(lengthBeforeCursor) { 0 }
    delete(maxOf(0, start), selectionStart)
}

/**
 * A variant of [deleteSurroundingText]. The difference is that
 * * The lengths are supplied in code points, not in chars.
 * * This command does nothing if there are one or more invalid surrogate pairs in the requested
 *   range.
 *
 * See
 * [`deleteSurroundingTextInCodePoints`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingTextInCodePoints(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in Unicode code points before the cursor to be
 *   deleted. Must be non-negative.
 * @param lengthAfterCursor The number of characters in Unicode code points after the cursor to be
 *   deleted. Must be non-negative.
 */
internal fun EditingBuffer.deleteSurroundingTextInCodePoints(
    lengthBeforeCursor: Int,
    lengthAfterCursor: Int
) {
    requirePrecondition(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
        "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
            "$lengthBeforeCursor and $lengthAfterCursor respectively."
    }

    // Convert code point length into character length. Then call the common logic of the
    // DeleteSurroundingTextEditOp
    var beforeLenInChars = 0
    for (i in 0 until lengthBeforeCursor) {
        beforeLenInChars++
        if (selectionStart > beforeLenInChars) {
            val lead = this[selectionStart - beforeLenInChars - 1]
            val trail = this[selectionStart - beforeLenInChars]

            if (isSurrogatePair(lead, trail)) {
                beforeLenInChars++
            }
        } else {
            // overflowing
            beforeLenInChars = selectionStart
            break
        }
    }

    var afterLenInChars = 0
    for (i in 0 until lengthAfterCursor) {
        afterLenInChars++
        if (selectionEnd + afterLenInChars < length) {
            val lead = this[selectionEnd + afterLenInChars - 1]
            val trail = this[selectionEnd + afterLenInChars]

            if (isSurrogatePair(lead, trail)) {
                afterLenInChars++
            }
        } else {
            // overflowing
            afterLenInChars = length - selectionEnd
            break
        }
    }

    delete(selectionEnd, selectionEnd + afterLenInChars)
    delete(selectionStart - beforeLenInChars, selectionStart)
}

/**
 * Finishes the composing text that is currently active. This simply leaves the text as-is, removing
 * any special composing styling or other state that was around it. The cursor position remains
 * unchanged.
 *
 * See
 * [`finishComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#finishComposingText()).
 */
internal fun EditingBuffer.finishComposingText() {
    commitComposition()
}

/**
 * Represents a backspace operation at the cursor position.
 *
 * If there is composition, delete the text in the composition range. If there is no composition but
 * there is selection, delete whole selected range. If there is no composition and selection,
 * perform backspace key event at the cursor position.
 */
internal fun EditingBuffer.backspace() {
    if (hasComposition()) {
        delete(compositionStart, compositionEnd)
    } else if (cursor == -1) {
        val delStart = selectionStart
        val delEnd = selectionEnd
        cursor = selectionStart
        delete(delStart, delEnd)
    } else if (cursor != 0) {
        val prevCursorPos = toString().findPrecedingBreak(cursor)
        delete(prevCursorPos, cursor)
    }
}

/**
 * Moves the cursor with [amount] characters.
 *
 * If there is selection, cancel the selection first and move the cursor to the selection start
 * position. Then perform the cursor movement.
 *
 * @param amount The amount of cursor movement. If you want to move backward, pass negative value.
 */
internal fun EditingBuffer.moveCursor(amount: Int) {
    if (cursor == -1) {
        cursor = selectionStart
    }

    var newCursor = selectionStart
    val bufferText = toString()
    if (amount > 0) {
        for (i in 0 until amount) {
            val next = bufferText.findFollowingBreak(newCursor)
            if (next == -1) break
            newCursor = next
        }
    } else {
        for (i in 0 until -amount) {
            val prev = bufferText.findPrecedingBreak(newCursor)
            if (prev == -1) break
            newCursor = prev
        }
    }

    cursor = newCursor
}

/** Deletes all the text in the buffer. */
internal fun EditingBuffer.deleteAll() {
    replace(0, length, "")
}

/**
 * Helper function that returns true when [high] is a Unicode high-surrogate code unit and [low] is
 * a Unicode low-surrogate code unit.
 */
private fun isSurrogatePair(high: Char, low: Char): Boolean =
    high.isHighSurrogate() && low.isLowSurrogate()
