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

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.LongPressDragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.selection.SelectionHandle
import androidx.ui.core.selection.SelectionHandleLayout
import androidx.ui.core.selection.getAdjustedCoordinates
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.core.texttoolbar.TextToolbarStatus
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.input.OffsetMap
import androidx.ui.input.TextFieldValue
import androidx.ui.input.getSelectedText
import androidx.ui.input.getTextAfterSelection
import androidx.ui.input.getTextBeforeSelection
import androidx.ui.text.AnnotatedString
import androidx.ui.text.InternalTextApi
import androidx.ui.text.TextFieldState
import androidx.ui.text.TextRange
import androidx.ui.text.style.ResolvedTextDirection
import androidx.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text field selection.
 */
@OptIn(InternalTextApi::class)
internal class TextFieldSelectionManager() {

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
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    var textToolbar: TextToolbar? = null

    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

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
            state?.let {
                if (it.draggingHandle) return
            }
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
     * [DragObserver] for dragging the selection handles to change the selection in TextField.
     */
    internal fun handleDragObserver(isStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart(downPosition: Offset) {
                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                dragBeginPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))
                // Zero out the total distance that being dragged.
                dragTotalDistance = Offset.Zero
                state?.draggingHandle = true
            }

            override fun onDrag(dragDistance: Offset): Offset {
                dragTotalDistance += dragDistance

                state?.layoutResult?.let { layoutResult ->
                    val startOffset =
                        if (isStartHandle)
                            layoutResult.getOffsetForPosition(dragBeginPosition + dragTotalDistance)
                        else
                            value.selection.start

                    val endOffset =
                        if (isStartHandle)
                            value.selection.end
                        else
                            layoutResult.getOffsetForPosition(dragBeginPosition + dragTotalDistance)

                    updateSelection(
                        value = value,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        isStartHandle = isStartHandle,
                        wordBasedSelection = false
                    )
                }
                return dragDistance
            }

            override fun onStop(velocity: Offset) {
                super.onStop(velocity)
                state?.draggingHandle = false
            }
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
        setSelectionStatus(false)
    }

    internal fun getHandlePosition(isStartHandle: Boolean): Offset {
        return if (isStartHandle)
            getSelectionHandleCoordinates(
                textLayoutResult = state?.layoutResult!!,
                offset = value.selection.start,
                isStart = true,
                areHandlesCrossed = value.selection.reversed
            )
        else
            getSelectionHandleCoordinates(
                textLayoutResult = state?.layoutResult!!,
                offset = value.selection.end,
                isStart = false,
                areHandlesCrossed = value.selection.reversed
            )
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy, paste and cut method as callbacks when "copy", "cut" or "paste" is clicked.
     */
    internal fun showSelectionToolbar() {
        if (!value.selection.collapsed) {
            textToolbar?.showPasteMenu(
                rect = getContentRect(),
                onCopyRequested = {
                    copy()
                    hideSelectionToolbar()
                },
                onPasteRequested = {
                    paste()
                    hideSelectionToolbar()
                },
                onCutRequested = {
                    cut()
                    hideSelectionToolbar()
                }
            )
        }
    }

    private fun hideSelectionToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            if (value.selection.collapsed) {
                textToolbar?.hide()
            }
        }
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    private fun getContentRect(): Rect {
        state?.layoutResult?.let {
            val startOffset = getHandlePosition(true)
            val endOffset = getHandlePosition(false)
            val startTop =
                it.getBoundingBox(value.selection.start.coerceIn(0, value.text.length - 1)).top
            val endTop =
                it.getBoundingBox(value.selection.end.coerceIn(0, value.text.length - 1)).top

            val left = min(startOffset.x, endOffset.x)
            val right = max(startOffset.x, endOffset.x)
            val top = min(startTop, endTop)
            val bottom = max(startOffset.y, endOffset.y) + (25.dp.value * 4.0).toFloat()

            return Rect(left, top, right, bottom)
        }

        return Rect.zero
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

        hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        val newValue = TextFieldValue(
            text = value.text,
            selection = range
        )
        onValueChange(newValue)
    }

    private fun setSelectionStatus(on: Boolean) {
        state?.let {
            it.selectionIsOn = on
        }
    }
}

@Composable
@OptIn(InternalTextApi::class)
internal fun SelectionHandle(
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    manager: TextFieldSelectionManager
) {
    SelectionHandleLayout(
        startHandlePosition = manager.getHandlePosition(true),
        endHandlePosition = manager.getHandlePosition(false),
        isStartHandle = isStartHandle,
        directions = directions,
        handlesCrossed = manager.value.selection.reversed
    ) {
        SelectionHandle(
            modifier =
            Modifier.dragGestureFilter(manager.handleDragObserver(isStartHandle)),
            isStartHandle = isStartHandle,
            directions = directions,
            handlesCrossed = manager.value.selection.reversed
        )
    }
}
