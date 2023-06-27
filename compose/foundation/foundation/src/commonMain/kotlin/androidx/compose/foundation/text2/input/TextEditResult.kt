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
 * A value to be returned from [TextFieldState.edit] that specifies where to place the cursor or
 * selection.
 *
 * Predefined results include:
 *  - [TextFieldBuffer.placeCursorAtEnd]
 *  - [TextFieldBuffer.placeCursorBeforeFirstChange]
 *  - [TextFieldBuffer.placeCursorAfterLastChange]
 *  - [TextFieldBuffer.placeCursorBeforeCharAt]
 *  - [TextFieldBuffer.placeCursorAfterCharAt]
 *  - [TextFieldBuffer.placeCursorBeforeCodepointAt]
 *  - [TextFieldBuffer.placeCursorAfterCodepointAt]
 *  - [TextFieldBuffer.selectAll]
 *  - [TextFieldBuffer.selectAllChanges]
 *  - [TextFieldBuffer.selectCharsIn]
 *  - [TextFieldBuffer.selectCodepointsIn]
 */
@ExperimentalFoundationApi
sealed class TextEditResult {
    /**
     * Returns the [TextRange] to set as the new selection. If the selection is collapsed, it will
     * set the cursor instead.
     *
     * @return The new range of the selection, in character offsets.
     */
    internal abstract fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange
}

/**
 * Returns a [TextEditResult] that places the cursor before the codepoint at the given index.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
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
 * @see placeCursorAfterCodepointAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeCodepointAt(index: Int): TextEditResult =
    PlaceCursorResult(this, index, after = false, inCodepoints = true)

/**
 * Returns a [TextEditResult] that places the cursor after the codepoint at the given index.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * If [index] is inside an invalid run, the cursor will be placed at the nearest earlier index.
 *
 * To place the cursor at the end of the field, after the last character, pass index
 * [TextFieldBuffer.codepointLength] or call [placeCursorAtEnd].
 *
 * @param index Codepoint index to place cursor after, should be in range 0 (inclusive) to
 * [TextFieldBuffer.codepointLength] (exclusive).
 *
 * @see placeCursorBeforeCharAt
 * @see placeCursorAfterCodepointAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAfterCodepointAt(index: Int): TextEditResult =
    PlaceCursorResult(this, index, after = true, inCodepoints = true)

/**
 * Returns a [TextEditResult] that places the cursor before the character at the given index.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
 * nearest earlier index.
 *
 * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the end
 * of the field, after the last character, pass index [TextFieldBuffer.length] or call
 * [placeCursorAtEnd].
 *
 * @param index Character index to place cursor before, should be in range 0 to
 * [TextFieldBuffer.length], inclusive.
 *
 * @see placeCursorBeforeCodepointAt
 * @see placeCursorAfterCharAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeCharAt(index: Int): TextEditResult =
    PlaceCursorResult(this, index, after = false, inCodepoints = false)

/**
 * Returns a [TextEditResult] that places the cursor after the character at the given index.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
 * nearest earlier index.
 *
 * To place the cursor at the end of the field, after the last character, pass index
 * [TextFieldBuffer.length] or call [placeCursorAtEnd].
 *
 * @param index Character index to place cursor after, should be in range 0 (inclusive) to
 * [TextFieldBuffer.length] (exclusive).
 *
 * @see placeCursorAfterCodepointAt
 * @see placeCursorBeforeCharAt
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAfterCharAt(index: Int): TextEditResult =
    PlaceCursorResult(this, index, after = true, inCodepoints = false)

/**
 * Returns a [TextEditResult] that places the cursor at the end of the text.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * @see placeCursorAfterLastChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAtEnd(): TextEditResult = PlaceCursorAtEndResult

/**
 * Returns a [TextEditResult] that places the cursor after the last change made to this
 * [TextFieldBuffer].
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * @see placeCursorAtEnd
 * @see placeCursorBeforeFirstChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAfterLastChange(): TextEditResult =
    PlaceCursorAfterLastChangeResult

/**
 * Returns a [TextEditResult] that places the cursor before the first change made to this
 * [TextFieldBuffer].
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * @see placeCursorAfterLastChange
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorBeforeFirstChange(): TextEditResult =
    PlaceCursorBeforeFirstChangeResult

/**
 * Returns a [TextEditResult] that places the selection around the given [range] in codepoints.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
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
fun TextFieldBuffer.selectCodepointsIn(range: TextRange): TextEditResult =
    SelectRangeResult(this, range, inCodepoints = true)

/**
 * Returns a [TextEditResult] that places the selection around the given [range] in characters.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
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
fun TextFieldBuffer.selectCharsIn(range: TextRange): TextEditResult =
    SelectRangeResult(this, range, inCodepoints = false)

/**
 * Returns a [TextEditResult] that places the selection before the first change and after the
 * last change.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * @see selectAll
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.selectAllChanges(): TextEditResult = SelectAllChangesResult

/**
 * Returns a [TextEditResult] that places the selection around all the text.
 *
 * _Note: This method has no side effects – just calling it will not perform the action, its return
 * value must be returned from [TextFieldState.edit]._
 *
 * @see selectAllChanges
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun TextFieldBuffer.selectAll(): TextEditResult = SelectAllResult

private object PlaceCursorAtEndResult : TextEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = TextRange(newValue.length)

    override fun toString(): String = "placeCursorAtEnd()"
}

private object PlaceCursorAfterLastChangeResult : TextEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        return if (newValue.changes.changeCount == 0) {
            oldValue.selectionInChars
        } else {
            TextRange(newValue.changes.getRange(newValue.changes.changeCount).max)
        }
    }

    override fun toString(): String = "placeCursorAfterLastChange()"
}

private object PlaceCursorBeforeFirstChangeResult : TextEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        return if (newValue.changes.changeCount == 0) {
            oldValue.selectionInChars
        } else {
            TextRange(newValue.changes.getRange(0).min)
        }
    }

    override fun toString(): String = "placeCursorBeforeFirstChange()"
}

private object SelectAllChangesResult : TextEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        val changes = newValue.changes
        return if (changes.changeCount == 0) {
            oldValue.selectionInChars
        } else {
            TextRange(
                changes.getRange(0).min,
                changes.getRange(changes.changeCount).max
            )
        }
    }

    override fun toString(): String = "selectAllChanges()"
}

private object SelectAllResult : TextEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = TextRange(0, newValue.length)

    override fun toString(): String = "selectAll()"
}

private class PlaceCursorResult(
    value: TextFieldBuffer,
    private val rawIndex: Int,
    private val after: Boolean,
    private val inCodepoints: Boolean
) : TextEditResult() {

    init {
        value.requireValidIndex(rawIndex, inCodepoints)
    }

    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange {
        var index = if (after) rawIndex + 1 else rawIndex
        index = index.coerceAtMost(if (inCodepoints) newValue.codepointLength else newValue.length)
        index = if (inCodepoints) newValue.codepointIndexToCharIndex(index) else index
        return TextRange(index)
    }

    override fun toString(): String = buildString {
        append("placeCursor")
        append(if (after) "After" else "Before")
        append(if (inCodepoints) "Codepoint" else "Char")
        append("At(index=$rawIndex)")
    }
}

private class SelectRangeResult(
    value: TextFieldBuffer,
    private val rawRange: TextRange,
    private val inCodepoints: Boolean
) : TextEditResult() {

    init {
        value.requireValidRange(rawRange, inCodepoints)
    }

    override fun calculateSelection(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldBuffer
    ): TextRange = if (inCodepoints) newValue.codepointsToChars(rawRange) else rawRange

    override fun toString(): String = buildString {
        append("select")
        append(if (inCodepoints) "Codepoints" else "Chars")
        append("In(range=$rawRange)")
    }
}