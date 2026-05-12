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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.awaitSelectionGestures
import androidx.compose.foundation.text.selection.getTextFieldSelectionLayout
import androidx.compose.foundation.text.selection.isSelectionHandleInVisibleBound
import androidx.compose.foundation.text.selection.updateSelectionTouchMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

internal fun Modifier.cupertinoTextFieldPointer(
    manager: TextFieldSelectionManager,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    state: LegacyTextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    offsetMapping: OffsetMapping
): Modifier = if (enabled) {
    this
        .updateSelectionTouchMode { state.isInTouchMode = it }
        .tapPressTextFieldModifier(interactionSource, enabled) { offset ->
            if (state.hasFocus) {
                // To show keyboard if it was hidden. Even in selection mode (like native)
                requestFocusAndShowKeyboardIfNeeded(
                    state,
                    focusRequester,
                    !readOnly
                )
                if (state.handleState != HandleState.Selection) {
                    state.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.cupertinoSetCursorOffsetFocused(
                            position = offset,
                            textLayoutResult = layoutResult,
                            editProcessor = state.processor,
                            offsetMapping = offsetMapping,
                            showContextMenu = { show ->
                                // it shouldn't be selection, but this is a way to call a context menu in BasicTextField
                                if (show) {
                                    manager.enterSelectionMode()
                                } else {
                                    manager.exitSelectionMode()
                                }
                            },
                            onValueChange = state.onValueChange
                        )
                    }
                } else {
                    manager.deselect(offset)
                }
            } else {
                requestFocusAndShowKeyboardIfNeeded(
                    state,
                    focusRequester,
                    !readOnly
                )
                state.layoutResult?.let { layoutResult ->
                    TextFieldDelegate.setCursorOffset(
                        offset,
                        layoutResult,
                        state.processor,
                        offsetMapping,
                        state.onValueChange
                    )
                }
            }
            if (state.textDelegate.text.isNotEmpty()) {
                state.handleState = HandleState.Cursor
            }
        }
        .then(CupertinoSelectionGesturesModifierElement(manager, state, offsetMapping))
        .pointerHoverIcon(PointerIcon.Text)
} else {
    this
}

private data class CupertinoSelectionGesturesModifierElement(
    private val manager: TextFieldSelectionManager,
    private val state: LegacyTextFieldState,
    private val offsetMapping: OffsetMapping,
) : ModifierNodeElement<CupertinoSelectionGesturesModifierNode>() {
    override fun create(): CupertinoSelectionGesturesModifierNode =
        CupertinoSelectionGesturesModifierNode(manager, state, offsetMapping)

    override fun update(node: CupertinoSelectionGesturesModifierNode) {
        node.update(manager, state, offsetMapping)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cupertinoSelectionGestures"
    }
}

private class CupertinoSelectionGesturesModifierNode(
    private var manager: TextFieldSelectionManager,
    private var state: LegacyTextFieldState,
    private var offsetMapping: OffsetMapping,
) : DelegatingNode() {
    private val longPressDragObserver = object : TextDragObserver {
        var dragTotalDistance = Offset.Zero
        var dragBeginOffset = Offset.Zero
        var shouldUpdateMagnifierPosition = false

        override fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment) {
            val isSingleLongPress = selectionAdjustment == SelectionAdjustment.None
            shouldUpdateMagnifierPosition = isSingleLongPress
            if (isSingleLongPress) {
                manager.draggingHandle = Handle.SelectionEnd
                manager.currentDragPosition = startPoint
                manager.hapticFeedBack?.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                manager.draggingHandle = null
                manager.currentDragPosition = null
            }

            state.layoutResult?.let { layoutResult ->
                TextFieldDelegate.setCursorOffset(
                    startPoint,
                    layoutResult,
                    state.processor,
                    offsetMapping,
                    state.onValueChange
                )
                if (selectionAdjustment != SelectionAdjustment.None) {
                    manager.doRepeatingTapSelection(startPoint, selectionAdjustment)
                }
                dragBeginOffset = startPoint
            }
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            dragTotalDistance += delta
            state.layoutResult?.let { layoutResult ->
                val currentDragPosition = dragBeginOffset + dragTotalDistance
                if (shouldUpdateMagnifierPosition) {
                    manager.currentDragPosition = currentDragPosition
                }
                TextFieldDelegate.setCursorOffset(
                    currentDragPosition,
                    layoutResult,
                    state.processor,
                    offsetMapping,
                    state.onValueChange
                )
            }
        }

        // Unnecessary here
        override fun onDown(point: Offset) {}

        override fun onUp() {}

        override fun onStop() {
            shouldUpdateMagnifierPosition = false
            manager.draggingHandle = null
            manager.currentDragPosition = null
        }

        override fun onCancel() {
            shouldUpdateMagnifierPosition = false
            manager.draggingHandle = null
            manager.currentDragPosition = null
        }
    }

    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            awaitSelectionGestures(manager.mouseSelectionObserver, longPressDragObserver)
        }
    )

    fun update(
        manager: TextFieldSelectionManager,
        state: LegacyTextFieldState,
        offsetMapping: OffsetMapping,
    ) {
        if (this.manager != manager) {
            pointerInputNode.resetPointerInputHandler()
        }
        this.manager = manager
        this.state = state
        this.offsetMapping = offsetMapping
    }
}

private fun TextFieldSelectionManager.doRepeatingTapSelection(
    touchPointOffset: Offset,
    selectionAdjustment: SelectionAdjustment
) {
    if (value.text.isEmpty()) return
    enterSelectionMode()
    updateSelection(
        value = value,
        currentPosition = touchPointOffset,
        isStartOfSelection = true,
        isStartHandle = false,
        adjustment = selectionAdjustment
    )
}

/**
 * Copied from TextFieldSelectionManager.kt
 */
private fun TextFieldSelectionManager.updateSelection(
    value: TextFieldValue,
    currentPosition: Offset,
    isStartOfSelection: Boolean,
    isStartHandle: Boolean,
    adjustment: SelectionAdjustment
) {
    val layoutResult = state?.layoutResult ?: return
    val previousTransformedSelection = TextRange(
        offsetMapping.originalToTransformed(value.selection.start),
        offsetMapping.originalToTransformed(value.selection.end)
    )

    val currentOffset = layoutResult.getOffsetForPosition(
        position = currentPosition,
        coerceInVisibleBounds = false
    )

    val rawStartHandleOffset = if (isStartHandle || isStartOfSelection) currentOffset else
        previousTransformedSelection.start

    val rawEndHandleOffset = if (!isStartHandle || isStartOfSelection) currentOffset else
        previousTransformedSelection.end

    val previousSelectionLayout = previousSelectionLayout // for smart cast
    val rawPreviousHandleOffset = if (
        isStartOfSelection ||
        previousSelectionLayout == null ||
        previousRawDragOffset == -1
    ) {
        -1
    } else {
        previousRawDragOffset
    }

    val selectionLayout = getTextFieldSelectionLayout(
        layoutResult = layoutResult.value,
        rawStartHandleOffset = rawStartHandleOffset,
        rawEndHandleOffset = rawEndHandleOffset,
        rawPreviousHandleOffset = rawPreviousHandleOffset,
        previousSelectionRange = previousTransformedSelection,
        isStartOfSelection = isStartOfSelection,
        isStartHandle = isStartHandle,
    )

    if (!selectionLayout.shouldRecomputeSelection(previousSelectionLayout)) {
        return
    }

    this.previousSelectionLayout = selectionLayout
    previousRawDragOffset = currentOffset

    val newTransformedSelection = adjustment.adjust(selectionLayout)

    val originalSelection = TextRange(
        start = offsetMapping.transformedToOriginal(newTransformedSelection.start.offset),
        end = offsetMapping.transformedToOriginal(newTransformedSelection.end.offset)
    )
    if (originalSelection == value.selection) return

    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    val newValue = createTextFieldValue(
        annotatedString = value.annotatedString,
        selection = originalSelection
    )
    onValueChange(newValue)

    // showSelectionHandleStart/End might be set to false when scrolled out of the view.
    // When the selection is updated, they must also be updated so that handles will be shown
    // or hidden correctly.
    state?.showSelectionHandleStart = isSelectionHandleInVisibleBound(true)
    state?.showSelectionHandleEnd = isSelectionHandleInVisibleBound(false)
}

/**
 * Copied from TextFieldSelectionManager.kt
 */
private fun createTextFieldValue(
    annotatedString: AnnotatedString,
    selection: TextRange
): TextFieldValue {
    return TextFieldValue(
        annotatedString = annotatedString,
        selection = selection
    )
}
