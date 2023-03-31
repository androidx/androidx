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
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A value to be returned from [TextFieldState.edit] that specifies where the place the cursor or
 * selection.
 */
@ExperimentalFoundationApi
sealed class TextFieldEditResult {
    /**
     * Returns the [TextRange] to set as the new selection. If the selection is collapsed, it will
     * set the cursor instead.
     */
    internal abstract fun calculateSelection(
        oldValue: TextFieldValue,
        newValue: MutableTextFieldValue
    ): TextRange
}

/**
 * Returns a [TextFieldEditResult] that places the cursor before the codepoint at the given index.
 * If [index] is inside an invalid run, the cursor will be placed at the nearest earlier index.
 *
 * @see placeCursorBeforeChar
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun MutableTextFieldValue.placeCursorBeforeCodepoint(index: Int): TextFieldEditResult =
    selectCodepoints(TextRange(index))

/**
 * Returns a [TextFieldEditResult] that places the cursor before the character at the given index.
 * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
 * nearest earlier index.
 *
 * @see placeCursorBeforeCodepoint
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun MutableTextFieldValue.placeCursorBeforeChar(index: Int): TextFieldEditResult =
    selectChars(TextRange(index))

/**
 * Returns a [TextFieldEditResult] that places the cursor at the end of the text.
 *
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun MutableTextFieldValue.placeCursorAtEnd(): TextFieldEditResult = PlaceCursorAtEndResult

/**
 * Returns a [TextFieldEditResult] that places the selection around the given [range] in codepoints.
 * If the start or end of [range] fall inside invalid runs, the values will be adjusted to the
 * nearest earlier and later codepoints, respectively.
 *
 * @see selectChars
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun MutableTextFieldValue.selectCodepoints(range: TextRange): TextFieldEditResult =
    SelectRangeResult(this, range, inCodepoints = true)

/**
 * Returns a [TextFieldEditResult] that places the selection around the given [range] in characters.
 * If the start or end of [range] fall inside surrogate pairs or other invalid runs, the values will
 * be adjusted to the nearest earlier and later characters, respectively.
 *
 * @see selectChars
 * @see TextFieldState.edit
 */
@ExperimentalFoundationApi
fun MutableTextFieldValue.selectChars(range: TextRange): TextFieldEditResult =
    SelectRangeResult(this, range, inCodepoints = false)

/**
 * Returns a [TextFieldEditResult] that places the selection around all the text.
 *
 * @see TextFieldState.edit
 */
@Suppress("UnusedReceiverParameter")
@ExperimentalFoundationApi
fun MutableTextFieldValue.selectAll(): TextFieldEditResult = SelectAllResult

private object PlaceCursorAtEndResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldValue,
        newValue: MutableTextFieldValue
    ): TextRange = TextRange(newValue.length)
}

private object SelectAllResult : TextFieldEditResult() {
    override fun calculateSelection(
        oldValue: TextFieldValue,
        newValue: MutableTextFieldValue
    ): TextRange = TextRange(0, newValue.length)
}

private class SelectRangeResult(
    value: MutableTextFieldValue,
    private val rawRange: TextRange,
    private val inCodepoints: Boolean
) : TextFieldEditResult() {

    val charRange: TextRange = if (inCodepoints) codepointsToChars(rawRange) else rawRange

    init {
        val validRange = TextRange(0, value.length)
        require(charRange in validRange) {
            val expectedRange = if (inCodepoints) charsToCodepoints(validRange) else validRange
            "Expected $rawRange to be in $expectedRange"
        }
    }

    override fun calculateSelection(
        oldValue: TextFieldValue,
        newValue: MutableTextFieldValue
    ): TextRange = charRange

    private fun codepointsToChars(range: TextRange): TextRange = TextRange(
        codepointIndexToCharIndex(range.start),
        codepointIndexToCharIndex(range.end)
    )

    private fun charsToCodepoints(range: TextRange): TextRange = TextRange(
        charIndexToCodepointIndex(range.start),
        charIndexToCodepointIndex(range.end),
    )

    // TODO Support actual codepoints.
    private fun codepointIndexToCharIndex(index: Int): Int = index
    private fun charIndexToCodepointIndex(index: Int): Int = index
}