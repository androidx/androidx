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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A [MutableTextFieldValue] that also provides both read and write access to the current selection
 * used by [TextEditFilter.filter].
 */
@ExperimentalFoundationApi
class MutableTextFieldValueWithSelection internal constructor(value: TextFieldValue) :
    MutableTextFieldValue(value) {

    /**
     * True if the selection range has non-zero length. If this is false, then the selection
     * represents the cursor.
     */
    val hasSelection: Boolean
        get() = !selectionInChars.collapsed

    /**
     * The selected range of characters.
     *
     * @see selectionInCodepoints
     */
    var selectionInChars: TextRange = value.selection
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
     * [MutableTextFieldValue.codepointLength].
     *
     * @param index Codepoint index to place cursor before, should be in range 0 to
     * [MutableTextFieldValue.codepointLength], inclusive.
     *
     * @see placeCursorBeforeCharAt
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
     * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the end
     * of the field, after the last character, pass index [MutableTextFieldValue.length].
     *
     * @param index Codepoint index to place cursor before, should be in range 0 to
     * [MutableTextFieldValue.length], inclusive.
     *
     * @see placeCursorBeforeCodepointAt
     */
    fun placeCursorBeforeCharAt(index: Int) {
        requireValidIndex(index, inCodepoints = false)
        selectionInChars = TextRange(index)
    }

    /**
     * Places the cursor at the end of the text.
     */
    fun placeCursorAtEnd() {
        selectionInChars = TextRange(length)
    }

    /**
     * Places the selection around the given [range] in codepoints.
     *
     * If the start or end of [range] fall inside invalid runs, the values will be adjusted to the
     * nearest earlier and later codepoints, respectively.
     *
     * To place the start of the selection at the beginning of the field, pass index 0. To place the
     * end of the selection at the end of the field, after the last codepoint, pass index
     * [MutableTextFieldValue.codepointLength]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCodepointAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [MutableTextFieldValue.codepointLength], inclusive.
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
     * [MutableTextFieldValue.length]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCharAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [MutableTextFieldValue.length], inclusive.
     *
     * @see selectCharsIn
     */
    fun selectCharsIn(range: TextRange) {
        requireValidRange(range, inCodepoints = false)
        selectionInChars = range
    }

    /**
     * Places the selection around all the text.
     */
    fun selectAll() {
        selectionInChars = TextRange(0, length)
    }

    /**
     * Resets this value to [value].
     */
    fun resetTo(value: TextFieldValue) {
        replaceWithoutNotifying(0, length, value.text)
        selectionInChars = value.selection
    }

    override fun onTextWillChange(rangeToBeReplaced: TextRange, newLength: Int) {
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

    internal fun toTextFieldValue(composition: TextRange? = null): TextFieldValue =
        toTextFieldValue(selection = selectionInChars, composition = composition)
}