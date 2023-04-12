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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.internal.ChangeTracker
import androidx.compose.ui.text.TextRange

/**
 * A [TextFieldBuffer] that also provides both read and write access to the current selection,
 * used by [TextEditFilter.filter].
 */
@ExperimentalFoundationApi
class TextFieldBufferWithSelection internal constructor(
    value: TextFieldCharSequence,
    /** The value reverted to when [revertAllChanges] is called. */
    private val sourceValue: TextFieldCharSequence = TextFieldCharSequence(),
    initialChanges: ChangeTracker? = null
) : TextFieldBuffer(value, initialChanges) {

    /**
     * True if the selection range has non-zero length. If this is false, then the selection
     * represents the cursor.
     *
     * @see selectionInChars
     */
    val hasSelection: Boolean
        get() = !selectionInChars.collapsed

    /**
     * The selected range of characters.
     *
     * @see selectionInCodepoints
     */
    var selectionInChars: TextRange = value.selectionInChars
        private set

    /**
     * The selected range of codepoints.
     *
     * @see selectionInChars
     */
    val selectionInCodepoints: TextRange
        get() = charsToCodepoints(selectionInChars)

    /**
     * Places the cursor before the codepoint at the given index.
     *
     * If [index] is inside an invalid run, the cursor will be placed at the nearest earlier index.
     *
     * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the
     * end of the field, after the last character, pass index
     * [TextFieldBuffer.codepointLength] or call [placeCursorAtEnd].
     *
     * @param index Codepoint index to place cursor before, should be in range 0 to
     * [TextFieldBuffer.codepointLength], inclusive.
     *
     * @see placeCursorBeforeCharAt
     * @see placeCursorAfterCodepointAt
     */
    fun placeCursorBeforeCodepointAt(index: Int) {
        requireValidIndex(index, inCodepoints = true)
        val charIndex = codepointIndexToCharIndex(index)
        selectionInChars = TextRange(charIndex)
    }

    /**
     * Places the cursor before the character at the given index.
     *
     * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
     * nearest earlier index.
     *
     * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the
     * end of the field, after the last character, pass index [TextFieldBuffer.length] or call
     * [placeCursorAtEnd].
     *
     * @param index Character index to place cursor before, should be in range 0 to
     * [TextFieldBuffer.length], inclusive.
     *
     * @see placeCursorBeforeCodepointAt
     * @see placeCursorAfterCharAt
     */
    fun placeCursorBeforeCharAt(index: Int) {
        requireValidIndex(index, inCodepoints = false)
        selectionInChars = TextRange(index)
    }

    /**
     * Places the cursor after the codepoint at the given index.
     *
     * If [index] is inside an invalid run, the cursor will be placed at the nearest later index.
     *
     * To place the cursor at the end of the field, after the last character, pass index
     * [TextFieldBuffer.codepointLength] or call [placeCursorAtEnd].
     *
     * @param index Codepoint index to place cursor after, should be in range 0 (inclusive) to
     * [TextFieldBuffer.codepointLength] (exclusive).
     *
     * @see placeCursorAfterCharAt
     * @see placeCursorBeforeCodepointAt
     */
    fun placeCursorAfterCodepointAt(index: Int) {
        requireValidIndex(index, inCodepoints = true)
        val charIndex = codepointIndexToCharIndex((index + 1).coerceAtMost(codepointLength))
        selectionInChars = TextRange(charIndex)
    }

    /**
     * Places the cursor after the character at the given index.
     *
     * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
     * nearest later index.
     *
     * To place the cursor at the end of the field, after the last character, pass index
     * [TextFieldBuffer.length] or call [placeCursorAtEnd].
     *
     * @param index Character index to place cursor after, should be in range 0 (inclusive) to
     * [TextFieldBuffer.length] (exclusive).
     *
     * @see placeCursorAfterCodepointAt
     * @see placeCursorBeforeCharAt
     */
    fun placeCursorAfterCharAt(index: Int) {
        requireValidIndex(index, inCodepoints = false)
        selectionInChars = TextRange((index + 1).coerceAtMost(length))
    }

    /**
     * Places the cursor at the end of the text.
     *
     * @see placeCursorBeforeFirstChange
     * @see placeCursorAfterLastChange
     */
    fun placeCursorAtEnd() {
        selectionInChars = TextRange(length)
    }

    /**
     * Places the cursor after the last change made to this [TextFieldBufferWithSelection].
     *
     * @see placeCursorAtEnd
     * @see placeCursorBeforeFirstChange
     */
    fun placeCursorAfterLastChange() {
        if (changes.changeCount > 0) {
            placeCursorBeforeCharAt(changes.getRange(changes.changeCount).max)
        }
    }

    /**
     * Places the cursor before the first change made to this [TextFieldBufferWithSelection].
     *
     * @see placeCursorAfterLastChange
     */
    fun placeCursorBeforeFirstChange() {
        if (changes.changeCount > 0) {
            placeCursorBeforeCharAt(changes.getRange(0).min)
        }
    }

    /**
     * Places the selection around the given [range] in codepoints.
     *
     * If the start or end of [range] fall inside invalid runs, the values will be adjusted to the
     * nearest earlier and later codepoints, respectively.
     *
     * To place the start of the selection at the beginning of the field, pass index 0. To place the
     * end of the selection at the end of the field, after the last codepoint, pass index
     * [TextFieldBuffer.codepointLength]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCodepointAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [TextFieldBuffer.codepointLength], inclusive.
     *
     * @see selectCharsIn
     */
    fun selectCodepointsIn(range: TextRange) {
        requireValidRange(range, inCodepoints = true)
        selectionInChars = codepointsToChars(range)
    }

    /**
     * Places the selection around the given [range] in characters.
     *
     * If the start or end of [range] fall inside surrogate pairs or other invalid runs, the values will
     * be adjusted to the nearest earlier and later characters, respectively.
     *
     * To place the start of the selection at the beginning of the field, pass index 0. To place the end
     * of the selection at the end of the field, after the last character, pass index
     * [TextFieldBuffer.length]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCharAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [TextFieldBuffer.length], inclusive.
     *
     * @see selectCodepointsIn
     */
    fun selectCharsIn(range: TextRange) {
        requireValidRange(range, inCodepoints = false)
        selectionInChars = range
    }

    /**
     * Places the selection around all the text.
     *
     * @see selectAllChanges
     */
    fun selectAll() {
        selectionInChars = TextRange(0, length)
    }

    /**
     * Places the selection before the first change and after the last change.
     *
     * @see selectAll
     */
    fun selectAllChanges() {
        if (changes.changeCount > 0) {
            selectCharsIn(
                TextRange(
                    changes.getRange(0).min,
                    changes.getRange(changes.changeCount).max
                )
            )
        }
    }

    /**
     * Revert all changes made to this value since it was created.
     *
     * After calling this method, this object will be in the same state it was when it was initially
     * created, and [changes] will be empty.
     */
    fun revertAllChanges() {
        replace(0, length, sourceValue.toString())
        selectionInChars = sourceValue.selectionInChars
        clearChangeList()
    }

    override fun onTextWillChange(rangeToBeReplaced: TextRange, newLength: Int) {
        super.onTextWillChange(rangeToBeReplaced, newLength)

        // Adjust selection.
        val start = rangeToBeReplaced.min
        val end = rangeToBeReplaced.max
        var selStart = selectionInChars.min
        var selEnd = selectionInChars.max

        if (selEnd < start) {
            // The entire selection is before the insertion point – we don't have to adjust the
            // mark at all, so skip the math.
            return
        }

        if (selStart <= start && end <= selEnd) {
            // The insertion is entirely inside the selection, move the end only.
            val diff = newLength - (end - start)
            // Preserve "cursorness".
            if (selStart == selEnd) {
                selStart += diff
            }
            selEnd += diff
        } else if (selStart > start && selEnd < end) {
            // Selection is entirely inside replacement, move it to the end.
            selStart = start + newLength
            selEnd = start + newLength
        } else if (selStart >= end) {
            // The entire selection is after the insertion, so shift everything forward.
            val diff = newLength - (end - start)
            selStart += diff
            selEnd += diff
        } else if (start < selStart) {
            // Insertion is around start of selection, truncate start of selection.
            selStart = start + newLength
            selEnd += newLength - (end - start)
        } else {
            // Insertion is around end of selection, truncate end of selection.
            selEnd = start
        }
        selectionInChars = TextRange(selStart, selEnd)
    }

    internal fun toTextFieldCharSequence(composition: TextRange? = null): TextFieldCharSequence =
        toTextFieldCharSequence(selection = selectionInChars, composition = composition)
}