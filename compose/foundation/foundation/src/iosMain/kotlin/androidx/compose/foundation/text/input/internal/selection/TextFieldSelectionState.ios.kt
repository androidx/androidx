/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.addTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.determineCursorDesiredOffset
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Deletion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Insertion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Replacement
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Untransformed
import androidx.compose.foundation.text.input.internal.SelectionWedgeAffinity
import androidx.compose.foundation.text.input.internal.WedgeAffinity
import androidx.compose.foundation.text.input.internal.findClosestRect
import androidx.compose.foundation.text.input.internal.fromDecorationToTextLayout
import androidx.compose.foundation.text.input.internal.getIndexTransformationType
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState.InputType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Cursor
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.None
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Selection
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.awaitSelectionGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Runs platform-specific text tap gestures logic. */
internal actual suspend fun TextFieldSelectionState.detectTextFieldTapGestures(
    pointerInputScope: PointerInputScope,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit
) {
    pointerInputScope.detectTapAndPress(
        onTap = { offset ->
            requestFocus()

            if (enabled && isFocused) {
                if (!readOnly) {
                    showKeyboard()
                    if (textFieldState.visualText.isNotEmpty()) {
                        showCursorHandle = true
                    }
                }

                updateTextToolbarState(None)

                val coercedOffset =
                    textLayoutState.coercedInVisibleBoundsOfInputText(offset)
                val cursorMoved = placeCursorAtDesiredOffset(
                    textLayoutState.fromDecorationToTextLayout(coercedOffset)
                )

                // TODO: It should be toggleable
                if (!cursorMoved) {
                    updateTextToolbarState(Cursor)
                }
            }
        },
        onPress = { offset ->
            interactionSource?.let { interactionSource ->
                coroutineScope {
                    launch {
                        // Remove any old interactions if we didn't fire stop / cancel properly
                        pressInteraction?.let { oldValue ->
                            val interaction = PressInteraction.Cancel(oldValue)
                            interactionSource.emit(interaction)
                            pressInteraction = null
                        }

                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        pressInteraction = press
                    }
                    val success = tryAwaitRelease()
                    pressInteraction?.let { pressInteraction ->
                        val endInteraction =
                            if (success) {
                                PressInteraction.Release(pressInteraction)
                            } else {
                                PressInteraction.Cancel(pressInteraction)
                            }
                        interactionSource.emit(endInteraction)
                    }
                    pressInteraction = null
                }
            }
        }
    )
}

// Copied from common part and added adjusting cursor position in iOS-style
private fun TextFieldSelectionState.placeCursorAtDesiredOffset(offset: Offset): Boolean {
    val layoutResult = textLayoutState.layoutResult ?: return false

    // First step: calculate the proposed cursor index.
    val proposedIndex = layoutResult.getOffsetForPosition(offset)
    if (proposedIndex == -1) return false

    // Second step: adjust proposed cursor position as iOS does
    val previousIndex = layoutResult.getOffsetForPosition(handleDragPosition)
    val currentText = textFieldState.untransformedText.text as String
    val index = if (textFieldState.untransformedText != textFieldState.visualText) {
        // BTF1 on iOS doesn't apply custom cursor positioning in case of any transformation,
        // so BTF2 should handle cursor positioning the same
        proposedIndex
    } else {
        determineCursorDesiredOffset(proposedIndex, previousIndex, layoutResult, currentText)
    }

    // Third step: if a transformation is applied, determine if the proposed cursor position
    // would be in a range where the cursor is not allowed to be. If so, push it to the
    // appropriate edge of that range.
    var newAffinity: SelectionWedgeAffinity? = null
    val untransformedCursor =
        textFieldState.getIndexTransformationType(index) { type, untransformed, retransformed ->
            when (type) {
                Untransformed -> untransformed.start

                // Deletion. Doesn't matter which end of the deleted range we put the cursor,
                // they'll both map to the same transformed offset.
                Deletion -> untransformed.start

                // The untransformed offset will be the same no matter which side we put the
                // cursor on, so we need to set the affinity to the closer edge.
                Insertion -> {
                    val wedgeStartCursorRect = layoutResult.getCursorRect(retransformed.start)
                    val wedgeEndCursorRect = layoutResult.getCursorRect(retransformed.end)
                    newAffinity =
                        if (
                            offset.findClosestRect(wedgeStartCursorRect, wedgeEndCursorRect) < 0
                        ) {
                            SelectionWedgeAffinity(WedgeAffinity.Start)
                        } else {
                            SelectionWedgeAffinity(WedgeAffinity.End)
                        }
                    untransformed.start
                }

                // Set the untransformed cursor to the edge that corresponds to the closer edge
                // in the transformed text.
                Replacement -> {
                    val wedgeStartCursorRect = layoutResult.getCursorRect(retransformed.start)
                    val wedgeEndCursorRect = layoutResult.getCursorRect(retransformed.end)
                    if (offset.findClosestRect(wedgeStartCursorRect, wedgeEndCursorRect) < 0) {
                        untransformed.start
                    } else {
                        untransformed.end
                    }
                }
            }
        }
    val untransformedCursorRange = TextRange(untransformedCursor)

    // Nothing changed, skip onValueChange and hapticFeedback.
    if (
        untransformedCursorRange == textFieldState.untransformedText.selection &&
        (newAffinity == null || newAffinity == textFieldState.selectionWedgeAffinity)
    ) {
        return false
    }

    textFieldState.selectUntransformedCharsIn(untransformedCursorRange)
    newAffinity?.let { textFieldState.selectionWedgeAffinity = it }
    return true
}

/** Runs platform-specific text selection gestures logic. */
internal actual suspend fun TextFieldSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) {
    val selectionState = this
    val uiKitTextDragObserver = UIKitTextFieldTextDragObserver(selectionState)
    pointerInputScope.awaitSelectionGestures(
        mouseSelectionObserver = mouseSelectionObserver,
        textDragObserver = uiKitTextDragObserver,
    )
}

private fun TextFieldSelectionState.moveCaretByLongPress(
    touchPointOffset: Offset,
) {
    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.LongPress)
    // Long Press at the blank area, the cursor should show up at the end of the line.
    if (!textLayoutState.isPositionOnText(touchPointOffset)) {
        val offset = textLayoutState.getOffsetForPosition(touchPointOffset)
        textFieldState.placeCursorBeforeCharAt(offset)
    } else {
        if (textFieldState.visualText.isEmpty()) return
        val coercedOffset =
            textLayoutState.coercedInVisibleBoundsOfInputText(touchPointOffset)
        placeCursorAtNearestOffset(
            textLayoutState.fromDecorationToTextLayout(coercedOffset)
        )
    }
    showCursorHandle = true
    updateHandleDragging(Handle.Cursor, touchPointOffset)
}

private fun TextFieldSelectionState.doRepeatingTapSelection(
    touchPointOffset: Offset,
    selectionAdjustment: SelectionAdjustment
) {
    clearSelection(touchPointOffset) // otherwise it won't be changed by triple tap

    val selectionOffset = textLayoutState.getOffsetForPosition(
        position = touchPointOffset
    )

    val newSelection = updateSelection(
        textFieldState.visualText,
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = selectionAdjustment,
        hapticFeedbackType = HapticFeedbackType.TextHandleMove,
    )

    textFieldState.selectCharsIn(newSelection)
    updateTextToolbarState(Selection)
}

private fun TextFieldSelectionState.clearSelection(
    touchPointOffset: Offset,
) {
    val selectionOffset = textLayoutState.getOffsetForPosition(
        position = touchPointOffset
    )
    val clearedSelection = updateSelection(
        TextFieldCharSequence(textFieldState.visualText, TextRange.Zero),
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = SelectionAdjustment.None,
        hapticFeedbackType = HapticFeedbackType.TextHandleMove,
    )
    textFieldState.selectCharsIn(clearedSelection)
}

private class UIKitTextFieldTextDragObserver(
    private val textFieldSelectionState: TextFieldSelectionState,
) : TextDragObserver {
    private var dragBeginPosition: Offset = Offset.Unspecified
    private var dragTotalDistance: Offset = Offset.Zero
    private var selectionAtLongPressStart: TextRange? = null

    private fun onDragStop(showContextMenu: Boolean) {
        // Only execute clear-up if drag was actually ongoing.
        if (dragBeginPosition.isSpecified) {
            textFieldSelectionState.clearHandleDragging()
            dragBeginPosition = Offset.Unspecified
            dragTotalDistance = Offset.Zero
            textFieldSelectionState.directDragGestureInitiator = InputType.None
            textFieldSelectionState.clearHandleDragging()

            val isSelectionUnchanged =
                textFieldSelectionState.textFieldState.visualText.selection == selectionAtLongPressStart
            if (showContextMenu && isSelectionUnchanged) {
                textFieldSelectionState.updateTextToolbarState(Cursor)
            }
        }
        selectionAtLongPressStart = null
    }

    override fun onDown(point: Offset) = Unit

    override fun onUp() = Unit

    override fun onStop() = onDragStop(showContextMenu = true)

    override fun onCancel() = onDragStop(showContextMenu = false)

    override fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment) {
        if (!textFieldSelectionState.enabled) return

        textFieldSelectionState.directDragGestureInitiator = InputType.Touch

        dragBeginPosition = startPoint
        dragTotalDistance = Offset.Zero

        if (selectionAdjustment != SelectionAdjustment.None) {
            textFieldSelectionState.doRepeatingTapSelection(startPoint, selectionAdjustment)
            selectionAtLongPressStart = null
        } else {
            textFieldSelectionState.moveCaretByLongPress(startPoint)
            selectionAtLongPressStart = textFieldSelectionState.textFieldState.visualText.selection
        }
    }

    override fun onDrag(delta: Offset) {
        if (!textFieldSelectionState.enabled || textFieldSelectionState.textFieldState.visualText.isEmpty()) return

        dragTotalDistance += delta

        val currentDragPosition = dragBeginPosition + dragTotalDistance

        val coercedOffset =
            textFieldSelectionState.textLayoutState.coercedInVisibleBoundsOfInputText(
                currentDragPosition
            )
        // A common function must be used here because in iOS during a drag the cursor should move without adjustments,
        // as it does with a single tap
        textFieldSelectionState.placeCursorAtNearestOffset(
            textFieldSelectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
        )
        textFieldSelectionState.updateHandleDragging(Handle.Cursor, currentDragPosition)
    }
}

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    actual suspend fun update() {
        val nativeClipboard = clipboard.nativeClipboard
        _hasClip = nativeClipboard.numberOfItems > 0
        _hasText = nativeClipboard.hasStrings
    }
}

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope
): Modifier = addTextContextMenuComponents {
    fun TextContextMenuBuilderScope.textFieldItem(
        key: Any,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        addComponent(
            TextContextMenuItemWithComposableLeadingIcon(
                key = key,
                label = "$key",
                enabled = enabled,
                onClick = {
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        onClick()
                        close()
                    }
                })
        )
    }

    with(state) {
        separator()
        textFieldItem(TextContextMenuKeys.CutKey, enabled = canShowCutMenuItem()) { cut() }
        textFieldItem(TextContextMenuKeys.CopyKey, enabled = canShowCopyMenuItem()) { copy(cancelSelection = false) }
        textFieldItem(TextContextMenuKeys.PasteKey, enabled = canShowPasteMenuItem()) { paste() }
        textFieldItem(TextContextMenuKeys.SelectAllKey, enabled = canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}
