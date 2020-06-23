/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text.selection

import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.gesture.LongPressDragObserver
import androidx.ui.geometry.Offset
import androidx.ui.input.OffsetMap
import androidx.ui.input.TextFieldValue
import androidx.ui.input.getSelectedText
import androidx.ui.input.getTextAfterSelection
import androidx.ui.input.getTextBeforeSelection
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextFieldState
import androidx.ui.text.TextRange
import kotlin.math.max

/**
 * A bridge class between user interaction to the text field selection.
 */
internal class TextFieldSelectionManager {

    /**
     * The current [OffsetMap] for text field.
     */
    internal var offsetMap: OffsetMap = OffsetMap.identityOffsetMap

    /**
     * Called when the input service updates the values in [TextFieldValue].
     */
    internal var onValueChange: (TextFieldValue) -> Unit = {}

    /**
     * The current [TextFieldState].
     */
    internal var state: TextFieldState? = null

    /**
     * The current [TextFieldValue].
     */
    internal var value: TextFieldValue = TextFieldValue()

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    internal var clipboardManager: ClipboardManager? = null

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = Offset.Zero

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = Offset.Zero

    /**
     * [LongPressDragObserver] for long press and drag to select in TextField.
     */
    internal val longPressDragObserver = object : LongPressDragObserver {
        override fun onLongPress(pxPosition: Offset) {
            // selection never started
            if (value.text == "") return
            setSelectionStatus(true)
            state?.layoutResult?.let { layoutResult ->
                val offset = offsetMap.transformedToOriginal(
                    layoutResult.getOffsetForPosition(pxPosition)
                )
                updateSelection(
                    value = value,
                    startOffset = offset,
                    endOffset = offset,
                    isStartHandle = true,
                    wordBasedSelection = true
                )
            }
            dragBeginPosition = pxPosition
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(dragDistance: Offset): Offset {
            // selection never started, did not consume any drag
            if (value.text == "") return Offset.Zero

            dragTotalDistance += dragDistance
            state?.layoutResult?.let { layoutResult ->
                val startOffset = layoutResult.getOffsetForPosition(dragBeginPosition)
                val endOffset =
                    layoutResult.getOffsetForPosition(dragBeginPosition + dragTotalDistance)
                updateSelection(
                    value = value,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    isStartHandle = true,
                    wordBasedSelection = true
                )
            }
            return dragDistance
        }
    }

    /**
     * The method for copying text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager], and cancel the selection.
     * The text in the text field should be unchanged.
     * The new cursor offset should be at the end of the previous selected text.
     */
    internal fun copy() {
        if (value.selection.collapsed) return

        clipboardManager?.setText(AnnotatedString(value.getSelectedText()))

        val newCursorOffset = value.selection.end
        val newValue = TextFieldValue(
            text = value.text,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        updateTextDirections(TextRange(newCursorOffset, newCursorOffset))
        setSelectionStatus(false)
    }

    /**
     * The method for pasting text.
     *
     * Get the text from [ClipboardManager]. If it's null, return.
     * The new text should be the text before the selected text, plus the text from the
     * [ClipboardManager], and plus the text after the selected text.
     * Then the selection should collapse, and the new cursor offset should be the end of the
     * newly added text.
     */
    internal fun paste() {
        val text = clipboardManager?.getText()?.text ?: return

        val newText = value.getTextBeforeSelection(value.text.length) +
                text +
                value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.start + text.length

        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        updateTextDirections(TextRange(newCursorOffset, newCursorOffset))
        setSelectionStatus(false)
    }

    /**
     * The method for cutting text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager].
     * The new text should be the text before the selection plus the text after the selection.
     * And the new cursor offset should be between the text before the selection, and the text
     * after the selection.
     */
    internal fun cut() {
        if (value.selection.collapsed) return

        clipboardManager?.setText(AnnotatedString(value.getSelectedText()))

        val newText = value.getTextBeforeSelection(value.text.length) +
                value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.start

        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        updateTextDirections(TextRange(newCursorOffset, newCursorOffset))
        setSelectionStatus(false)
    }

    private fun updateSelection(
        value: TextFieldValue,
        startOffset: Int,
        endOffset: Int,
        isStartHandle: Boolean,
        wordBasedSelection: Boolean
    ) {
        val range = getTextFieldSelection(
            textLayoutResult = state?.layoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = if (value.selection.collapsed) null else value.selection,
            previousHandlesCrossed = value.selection.reversed,
            isStartHandle = isStartHandle,
            wordBasedSelection = wordBasedSelection
        )

        if (range == value.selection) return

        val newValue = TextFieldValue(
            text = value.text,
            selection = range
        )
        onValueChange(newValue)
        updateTextDirections(range)
    }

    private fun updateTextDirections(range: TextRange) {
        state?.let {
            it.selectionStartDirection = it.layoutResult!!.getBidiRunDirection(range.start)
            it.selectionEndDirection = it.layoutResult!!.getBidiRunDirection(max(range.end - 1, 0))
        }
    }

    private fun setSelectionStatus(on: Boolean) {
        state?.let {
            it.selectionIsOn = on
        }
    }
}
