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

package androidx.compose.foundation.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Sets and adjusts the cursor offset when the TextField is focused in a Cupertino style.
 * Cursor final offset depends on position of the cursor and content of the textfield.
 *
 * See **determineCursorDesiredPosition()** for more details.
 * @param position The position of the cursor in the TextField.
 * @param textLayoutResult The TextLayoutResultProxy object that contains the layout information of the TextField text.
 * @param editProcessor The EditProcessor object that manages the editing operations of the TextField.
 * @param offsetMapping The OffsetMapping object that maps the transformed offset to the original offset.
 * @param showContextMenu The function that displays the context menu at the given rectangular area.
 * @param onValueChange The function that is called when the TextField value changes.
 */
internal fun TextFieldDelegate.Companion.cupertinoSetCursorOffsetFocused(
    position: Offset,
    textLayoutResult: TextLayoutResultProxy,
    editProcessor: EditProcessor,
    offsetMapping: OffsetMapping,
    showContextMenu: (Boolean) -> Unit,
    onValueChange: (TextFieldValue) -> Unit
) {
    val offset =
        offsetMapping.transformedToOriginal(textLayoutResult.getOffsetForPosition(position))
    val currentValue = editProcessor.toTextFieldValue()
    val currentText = textLayoutResult.value.layoutInput.text.toString()
    val previousOffset = currentValue.selection.start

    val cursorDesiredOffset = determineCursorDesiredOffset(
        offset,
        previousOffset,
        textLayoutResult,
        currentText
    )

    showContextMenu(cursorDesiredOffset == offset && cursorDesiredOffset == previousOffset)
    onValueChange(
        editProcessor.toTextFieldValue().copy(selection = TextRange(cursorDesiredOffset))
    )
}

/**
 * Determines the desired cursor position based on the given parameters.
 *
 * The rules for determining position of the caret are as follows:
 * - When you make a single tap on a word, the caret moves to the end of this word.
 * - If there’s a punctuation mark after the word, the caret is between the word and the punctuation mark.
 * - If you tap on a whitespace, the caret is placed before the word. Same for many whitespaces in a row. (and punctuation marks)
 * - If there’s a punctuation mark before the word, the caret is between the punctuation mark and the word.
 * - When you make a single tap on the first half of the word, the caret is placed before this word.
 * - If you tap on the left edge of the TextField, the caret is placed before the first word on this line. The same is for the right edge.
 * - If you tap at the caret placed in the middle of the word, it will jump to the end of the word.
 * @param offset The current offset position.
 * @param previousOffset The previous offset position (where caret was before incoming tap).
 * @param textLayoutResult The TextLayoutResultProxy representing the layout of the text.
 * @param currentText The current text in the TextField.
 * @return The desired cursor position after evaluating the given parameters.
 */
internal fun determineCursorDesiredOffset(
    offset: Int,
    previousOffset: Int,
    textLayoutResult: TextLayoutResultProxy,
    currentText: String
): Int {
    val caretOffsetPosition = when {
        offset == previousOffset -> offset
        textLayoutResult.isLeftEdgeTapped(offset) -> {
            val lineNumber = textLayoutResult.value.getLineForOffset(offset)
            textLayoutResult.value.getLineStart(lineNumber)
        }

        textLayoutResult.isRightEdgeTapped(offset) -> {
            val lineNumber = textLayoutResult.value.getLineForOffset(offset)
            textLayoutResult.value.getLineEnd(lineNumber)
        }

        currentText.isWhitespaceOrPunctuation(offset) -> findNextNonWhitespaceSymbolsSubsequenceStartOffset(
            offset,
            currentText
        )

        textLayoutResult.isFirstHalfOfWordTapped(
            offset,
            currentText
        ) -> textLayoutResult.value.getWordBoundary(offset).start

        else -> textLayoutResult.value.getWordBoundary(offset).end
    }
    return caretOffsetPosition
}

private fun TextLayoutResultProxy.isFirstHalfOfWordTapped(
    caretOffset: Int,
    currentText: String
): Boolean {
    val wordBoundary = value.getWordBoundary(caretOffset)
    // current text is taken from value.layoutInput.text, so value.getWordBoundary() should work correctly.
    val word = currentText.substring(wordBoundary.start, wordBoundary.end)
    val middleIndex = wordBoundary.start + word.midpointPositionWithUnicodeSymbols()
    return caretOffset < middleIndex
}

private fun TextLayoutResultProxy.isLeftEdgeTapped(caretOffset: Int): Boolean {
    val lineNumber = value.getLineForOffset(caretOffset)
    val lineStartOffset = value.getLineStart(lineNumber)
    return lineStartOffset == caretOffset
}

private fun TextLayoutResultProxy.isRightEdgeTapped(caretOffset: Int): Boolean {
    val lineNumber = value.getLineForOffset(caretOffset)
    val lineEndOffset = value.getLineEnd(lineNumber)
    return lineEndOffset == caretOffset
}