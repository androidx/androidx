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

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectRepeatingTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.getTextFieldSelectionLayout
import androidx.compose.foundation.text.selection.isSelectionHandleInVisibleBound
import androidx.compose.foundation.text.selection.selectionGestureInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

@Composable
internal fun Modifier.cupertinoTextFieldPointer(
    manager: TextFieldSelectionManager,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    state: TextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    offsetMapping: OffsetMapping
): Modifier = if (enabled) {
    // TODO switch to ".updateSelectionTouchMode { state.isInTouchMode = it }" as in defaultTextFieldPointer
    if (isInTouchMode) {
        val longPressHandlerModifier = getLongPressHandlerModifier(state, offsetMapping)
        val tapHandlerModifier = getTapHandlerModifier(
            interactionSource,
            state,
            focusRequester,
            readOnly,
            offsetMapping,
            manager
        )
        this
            .then(tapHandlerModifier)
            .then(longPressHandlerModifier)
            .pointerHoverIcon(textPointerIcon)
    } else {
        this
            .selectionGestureInput(
                mouseSelectionObserver = manager.mouseSelectionObserver,
                textDragObserver = manager.touchSelectionObserver,
            )
            .pointerHoverIcon(textPointerIcon)
    }
} else {
    this
}

@Composable
@OptIn(InternalFoundationTextApi::class)
private fun getTapHandlerModifier(
    interactionSource: MutableInteractionSource?,
    state: TextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    offsetMapping: OffsetMapping,
    manager: TextFieldSelectionManager
): Modifier {
    val currentState by rememberUpdatedState(state)
    val currentFocusRequester by rememberUpdatedState(focusRequester)
    val currentReadOnly by rememberUpdatedState(readOnly)
    val currentOffsetMapping by rememberUpdatedState(offsetMapping)
    val currentManager by rememberUpdatedState(manager)
    /*
    We need to move tap recognizer here from selection modifier (as it is in common) because:
    1) we need to handle triple tap
    2) without rewriting, we have onDoubleTap call and onTap call, and onDoubleTap will execute
    before onTap.
    */
    return Modifier.pointerInput(interactionSource) {
        detectRepeatingTapGestures(
            onTap = { touchPointOffset ->
                if (currentState.hasFocus) {
                    // To show keyboard if it was hidden. Even in selection mode (like native)
                    requestFocusAndShowKeyboardIfNeeded(
                        currentState,
                        currentFocusRequester,
                        !currentReadOnly
                    )
                    if (currentState.handleState != HandleState.Selection) {
                        currentState.layoutResult?.let { layoutResult ->
                            TextFieldDelegate.cupertinoSetCursorOffsetFocused(
                                position = touchPointOffset,
                                textLayoutResult = layoutResult,
                                editProcessor = currentState.processor,
                                offsetMapping = currentOffsetMapping,
                                showContextMenu = {},
                                onValueChange = currentState.onValueChange
                            )
                        }
                    } else {
                        currentManager.deselect(touchPointOffset)
                    }
                } else {
                    requestFocusAndShowKeyboardIfNeeded(
                        currentState,
                        currentFocusRequester,
                        !currentReadOnly
                    )
                    currentState.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.setCursorOffset(
                            touchPointOffset,
                            layoutResult,
                            currentState.processor,
                            currentOffsetMapping,
                            currentState.onValueChange
                        )
                    }
                }
                if (currentState.textDelegate.text.isNotEmpty()) {
                    currentState.handleState = HandleState.Cursor
                }
            },
            onDoubleTap = {
                currentManager.doRepeatingTapSelection(it, SelectionAdjustment.Word)
            },
            onTripleTap = {
                currentManager.doRepeatingTapSelection(it, SelectionAdjustment.Paragraph)
            }
        )
    }
}

/**
 * Returns a modifier which allows to precisely move the caret in the text by drag gesture after long press
 *
 * @param state The state of the text field.
 * @param offsetMapping The offset mapping of the text field.
 * @return A modifier that handles long press and drag gestures.
 */
@Composable
private fun getLongPressHandlerModifier(
    state: TextFieldState,
    offsetMapping: OffsetMapping
): Modifier {
    val currentState by rememberUpdatedState(state)
    val currentOffsetMapping by rememberUpdatedState(offsetMapping)

    return Modifier.pointerInput(Unit) {
        val longTapActionsObserver =
            object : TextDragObserver {
                var dragTotalDistance = Offset.Zero
                var dragBeginOffset = Offset.Zero

                override fun onStart(startPoint: Offset) {
                    currentState.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.setCursorOffset(
                            startPoint,
                            layoutResult,
                            currentState.processor,
                            currentOffsetMapping,
                            currentState.onValueChange
                        )
                        dragBeginOffset = startPoint
                    }
                    dragTotalDistance = Offset.Zero
                }

                override fun onDrag(delta: Offset) {
                    dragTotalDistance += delta
                    currentState.layoutResult?.let { layoutResult ->
                        val currentDragPosition = dragBeginOffset + dragTotalDistance
                        TextFieldDelegate.setCursorOffset(
                            currentDragPosition,
                            layoutResult,
                            currentState.processor,
                            currentOffsetMapping,
                            currentState.onValueChange
                        )
                    }
                }

                // Unnecessary here
                override fun onDown(point: Offset) {}

                override fun onUp() {}

                override fun onStop() {}

                override fun onCancel() {}
            }

        detectDragGesturesAfterLongPress(
            onDragStart = { longTapActionsObserver.onStart(it) },
            onDrag = { _, delta -> longTapActionsObserver.onDrag(delta = delta) },
            onDragCancel = { longTapActionsObserver.onCancel() },
            onDragEnd = { longTapActionsObserver.onStop() }
        )
    }
}

private fun TextFieldSelectionManager.doRepeatingTapSelection(
    touchPointOffset: Offset,
    selectionAdjustment: SelectionAdjustment
) {
    if (value.text.isEmpty()) return
    enterSelectionMode()
    state?.layoutResult?.let { layoutResult ->
        updateSelection(
            value = value,
            currentPosition = touchPointOffset,
            isStartOfSelection = true,
            isStartHandle = false,
            adjustment = selectionAdjustment
        )
    }
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