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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.TextRange

/**
 * A value to be returned from [TextFieldState.edit] that specifies where the place the cursor or
 * selection.
 *
 * Predefined results include:
 *  - [TextFieldBuffer.placeCursorAtEnd]
 *  - [TextFieldBuffer.placeCursorBeforeFirstChange]
 *  - [TextFieldBuffer.placeCursorAfterLastChange]
 *  - [TextFieldBuffer.placeCursorBeforeCharAt]
 *  - [TextFieldBuffer.placeCursorBeforeCodepointAt]
 *  - [TextFieldBuffer.selectAll]
 *  - [TextFieldBuffer.selectAllChanges]
 *  - [TextFieldBuffer.selectCharsIn]
 *  - [TextFieldBuffer.selectCodepointsIn]
 */
@ExperimentalFoundationApi
sealed class TextFieldEditResult {
    /**
     * Returns the [TextRange] to set as the new selection. If the selection is collapsed, it will
     * set the cursor instead.
     */
    internal abstract fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange
}

/**
 * Returns a [TextFieldEditResult] that places the cursor before the codepoint at the given index.
 *
 * If [index] is inside an invalid run, the cursor will be placed at the nearest earlier index.
 *
 * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the end
 * of the field, after the last character, pass index [TextFieldBuffer.codepointLength].
 *
 * @param index Codepoint index to place cursor before, should be in range 0 to
 * [TextFieldBuffer.codepointLength], inclusive.
 *
 * @see placeCursorBeforeCharAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeCodepointAt(index: Int): TextFieldEditResult =
    object : SelectRangeResult(this, TextRange(index), inCodepoints = true) {
        override fun toString(): String = "placeCursorBeforeCodepoint(index=$index)"
    }

/**
 * Returns a [TextFieldEditResult] that places the cursor before the character at the given index.
 *
 * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
 * nearest earlier index.
 *
 * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the end
 * of the field, after the last character, pass index [TextFieldBuffer.length].
 *
 * @param index Codepoint index to place cursor before, should be in range 0 to
 * [TextFieldBuffer.length], inclusive.
 *
 * @see placeCursorBeforeCodepointAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeCharAt(index: Int): TextFieldEditResult =
    object : SelectRangeResult(this, TextRange(index), inCodepoints = false) {
        override fun toString(): String = "placeCursorBeforeChar(index=$index)"
    }

/**
 * Returns a [TextFieldEditResult] that places the cursor at the end of the text.
 *
 * @see placeCursorAfterLastChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAtEnd(): TextFieldEditResult = PlaceCursorAtEndResult

/**
 * Returns a [TextFieldEditResult] that places the cursor after the last change made to this
 * [TextFieldBuffer].
 *
 * @see placeCursorAtEnd
 * @see placeCursorBeforeFirstChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAfterLastChange(): TextFieldEditResult =
    PlaceCursorAfterLastChangeResult

/**
 * Returns a [TextFieldEditResult] that places the cursor before the first change made to this
 * [TextFieldBuffer].
 *
 * @see placeCursorAfterLastChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeFirstChange(): TextFieldEditResult =
    PlaceCursorBeforeFirstChangeResult

/**
 * Returns a [TextFieldEditResult] that places the selection around the given [range] in codepoints.
 *
 * If the start or end of [range] fall inside invalid runs, the values will be adjusted to the
 * nearest earlier and later codepoints, respectively.
 *
 * To place the start of the selection at the beginning of the field, pass index 0. To place the end
 * of the selection at the end of the field, after the last codepoint, pass index
 * [TextFieldBuffer.codepointLength]. Passing a zero-length range is the same as calling
 * [placeCursorBeforeCodepointAt].
 *
 * @param range Codepoint range of the selection, should be in range 0 to
 * [TextFieldBuffer.codepointLength], inclusive.
 *
 * @see selectCharsIn
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.selectCodepointsIn(range: TextRange): TextFieldEditResult =
    object : SelectRangeResult(this, range, inCodepoints = true) {
        override fun toString(): String = "selectCodepoints(range=$range)"
    }

/**
 * Returns a [TextFieldEditResult] that places the selection around the given [range] in characters.
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
 * @see selectCharsIn
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.selectCharsIn(range: TextRange): TextFieldEditResult =
    object : SelectRangeResult(this, range, inCodepoints = false) {
        override fun toString(): String = "selectChars(range=$range)"
    }

/**
 * Returns a [TextFieldEditResult] that places the selection before the first change and after the
 * last change.
 *
 * @see selectAll
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.selectAllChanges(): TextFieldEditResult = SelectAllChangesResult

/**
 * Returns a [TextFieldEditResult] that places the selection around all the text.
 *
 * @see selectAllChanges
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.selectAll(): TextFieldEditResult = SelectAllResult

private object PlaceCursorAtEndResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = TextRange(newValue.length)

    override fun toString(): String = "placeCursorAtEnd()"
}

private object PlaceCursorAfterLastChangeResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        return if (newValue.changes.changeCount == 0) {
            oldValue.selection
        } else {
            TextRange(newValue.changes.getRange(newValue.changes.changeCount).max)
        }
    }

    override fun toString(): String = "placeCursorAfterLastChange()"
}

private object PlaceCursorBeforeFirstChangeResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        return if (newValue.changes.changeCount == 0) {
            oldValue.selection
        } else {
            TextRange(newValue.changes.getRange(0).min)
        }
    }

    override fun toString(): String = "placeCursorBeforeFirstChange()"
}

private object SelectAllChangesResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        val changes = newValue.changes
        return if (changes.changeCount == 0) {
            oldValue.selection
        } else {
            TextRange(
                changes.getRange(0).min,
                changes.getRange(changes.changeCount).max
            )
        }
    }

    override fun toString(): String = "selectAllChanges()"
}

private object SelectAllResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = TextRange(0, newValue.length)

    override fun toString(): String = "selectAll()"
}

/** Open for [toString] overrides. */
private open class SelectRangeResult(
    value: TextFieldBuffer,
    private val rawRange: TextRange,
    private val inCodepoints: Boolean
) : TextFieldEditResult() {

    init {
        value.requireValidRange(rawRange, inCodepoints)
    }

    final override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = if (inCodepoints) newValue.codepointsToChars(rawRange) else rawRange
}