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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.HandleState
import androidx.compose.foundation.text.HandleState.Cursor
import androidx.compose.foundation.text.HandleState.None
import androidx.compose.foundation.text.HandleState.Selection
import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.UndoManager
import androidx.compose.foundation.text.ValidatingEmptyOffsetMappingIdentity
import androidx.compose.foundation.text.detectDownAndDragGesturesWithObserver
import androidx.compose.foundation.text.isPositionInsideSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text field selection.
 */
internal class TextFieldSelectionManager(
    val undoManager: UndoManager? = null
) {

    /**
     * The current [OffsetMapping] for text field.
     */
    internal var offsetMapping: OffsetMapping = ValidatingEmptyOffsetMappingIdentity

    /**
     * Called when the input service updates the values in [TextFieldValue].
     */
    internal var onValueChange: (TextFieldValue) -> Unit = {}

    /**
     * The current [LegacyTextFieldState].
     */
    internal var state: LegacyTextFieldState? = null

    /**
     * The current [TextFieldValue]. This contains the original text, not the transformed text.
     * Transformed text can be found with [transformedText].
     */
    internal var value: TextFieldValue by mutableStateOf(TextFieldValue())

    /**
     * The current transformed text from the [LegacyTextFieldState].
     * The original text can be found in [value].
     */
    internal val transformedText get() = state?.textDelegate?.text

    /**
     * Visual transformation of the text field's text. Used to check if certain toolbar options
     * are permitted. For example, 'cut' will not be available is it is password transformation.
     */
    internal var visualTransformation: VisualTransformation = VisualTransformation.None

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
     * [FocusRequester] used to request focus for the TextField.
     */
    var focusRequester: FocusRequester? = null

    /**
     * Defines if paste and cut toolbar menu actions should be shown
     */
    var editable by mutableStateOf(true)

    /**
     * Whether the text field should be selectable at all.
     */
    var enabled by mutableStateOf(true)

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = Offset.Zero

    /**
     * The beginning offset of the drag gesture translated into position in text. Every time a
     * new drag gesture starts, it wil be recalculated.
     * Unlike [dragBeginPosition] that is relative to the decoration box,
     * [dragBeginOffsetInText] represents index in text. Essentially, it is equal to
     * `layoutResult.getOffsetForPosition(dragBeginPosition)`.
     */
    private var dragBeginOffsetInText: Int? = null

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = Offset.Zero

    /**
     * A flag to check if a selection or cursor handle is being dragged, and which handle is being
     * dragged.
     * If this value is non-null, then onPress will not select any text.
     * This value will be set to non-null when either handle is being dragged, and be reset to null
     * when the dragging is stopped.
     */
    var draggingHandle: Handle? by mutableStateOf(null)
        internal set

    /**
     * The current position of a drag, in decoration box coordinates.
     */
    var currentDragPosition: Offset? by mutableStateOf(null)
        internal set

    /**
     * The previous offset of a drag, before selection adjustments.
     * Only update when a selection layout change has occurred,
     * or set to -1 if a new drag begins.
     */
    internal var previousRawDragOffset: Int = -1

    /**
     * The old [TextFieldValue] before entering the selection mode on long press. Used to exit
     * the selection mode.
     */
    private var oldValue: TextFieldValue = TextFieldValue()

    /**
     * The previous [SelectionLayout] where [SelectionLayout.shouldRecomputeSelection] was true.
     */
    internal var previousSelectionLayout: SelectionLayout? = null

    /**
     * [TextDragObserver] for long press and drag to select in TextField.
     */
    internal val touchSelectionObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            // Not supported for long-press-drag.
        }

        override fun onUp() {
            // Nothing to do.
        }

        override fun onStart(startPoint: Offset) {
            if (!enabled || draggingHandle != null) return
            // While selecting by long-press-dragging, the "end" of the selection is always the one
            // being controlled by the drag.
            draggingHandle = Handle.SelectionEnd
            previousRawDragOffset = -1

            // ensuring that current action mode (selection toolbar) is invalidated
            hideSelectionToolbar()

            // Long Press at the blank area, the cursor should show up at the end of the line.
            if (state?.layoutResult?.isPositionOnText(startPoint) != true) {
                state?.layoutResult?.let { layoutResult ->
                    val transformedOffset = layoutResult.getOffsetForPosition(startPoint)
                    val offset = offsetMapping.transformedToOriginal(transformedOffset)

                    val newValue = createTextFieldValue(
                        annotatedString = value.annotatedString,
                        selection = TextRange(offset, offset)
                    )

                    enterSelectionMode(showFloatingToolbar = false)
                    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(newValue)
                }
            } else {
                if (value.text.isEmpty()) return
                enterSelectionMode(showFloatingToolbar = false)
                val adjustedStartSelection = updateSelection(
                    // reset selection, otherwise a previous selection may be used
                    // as context for creating the next selection
                    value = value.copy(selection = TextRange.Zero),
                    currentPosition = startPoint,
                    isStartOfSelection = true,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word,
                    isTouchBasedSelection = true,
                )
                // For touch, set the begin offset to the adjusted selection.
                // When char based selection is used, we want to ensure we snap the
                // beginning offset to the start word boundary of the first selected word.
                dragBeginOffsetInText = adjustedStartSelection.start
            }

            // don't set selection handle state until drag ends
            setHandleState(None)

            dragBeginPosition = startPoint
            currentDragPosition = dragBeginPosition
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            // selection never started, did not consume any drag
            if (!enabled || value.text.isEmpty()) return

            dragTotalDistance += delta
            state?.layoutResult?.let { layoutResult ->
                currentDragPosition = dragBeginPosition + dragTotalDistance

                if (
                    dragBeginOffsetInText == null &&
                    !layoutResult.isPositionOnText(currentDragPosition!!)
                ) {
                    // both start and end of drag is in end padding.
                    val startOffset = offsetMapping.transformedToOriginal(
                        layoutResult.getOffsetForPosition(dragBeginPosition)
                    )

                    val endOffset = offsetMapping.transformedToOriginal(
                        layoutResult.getOffsetForPosition(currentDragPosition!!)
                    )

                    val adjustment = if (startOffset == endOffset) {
                        // start and end is in the same end padding, keep the collapsed selection
                        SelectionAdjustment.None
                    } else {
                        SelectionAdjustment.Word
                    }

                    updateSelection(
                        value = value,
                        currentPosition = currentDragPosition!!,
                        isStartOfSelection = false,
                        isStartHandle = false,
                        adjustment = adjustment,
                        isTouchBasedSelection = true,
                    )
                } else {
                    val startOffset = dragBeginOffsetInText ?: layoutResult.getOffsetForPosition(
                        position = dragBeginPosition,
                        coerceInVisibleBounds = false
                    )
                    val endOffset = layoutResult.getOffsetForPosition(
                        position = currentDragPosition!!,
                        coerceInVisibleBounds = false
                    )

                    if (dragBeginOffsetInText == null && startOffset == endOffset) {
                        // if we are selecting starting from end padding,
                        // don't start selection until we have and un-collapsed selection.
                        return
                    }

                    updateSelection(
                        value = value,
                        currentPosition = currentDragPosition!!,
                        isStartOfSelection = false,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.Word,
                        isTouchBasedSelection = true,
                    )
                }
            }
            updateFloatingToolbar(show = false)
        }

        override fun onStop() = onEnd()
        override fun onCancel() = onEnd()
        private fun onEnd() {
            draggingHandle = null
            currentDragPosition = null
            updateFloatingToolbar(show = true)
            dragBeginOffsetInText = null

            val collapsed = value.selection.collapsed
            setHandleState(if (collapsed) Cursor else Selection)
            state?.showSelectionHandleStart =
                !collapsed && isSelectionHandleInVisibleBound(isStartHandle = true)
            state?.showSelectionHandleEnd =
                !collapsed && isSelectionHandleInVisibleBound(isStartHandle = false)
            state?.showCursorHandle =
                collapsed && isSelectionHandleInVisibleBound(isStartHandle = true)
        }
    }

    internal val mouseSelectionObserver = object : MouseSelectionObserver {
        override fun onExtend(downPosition: Offset): Boolean {
            // can't update selection without a layoutResult, so don't consume
            state?.layoutResult ?: return false
            if (!enabled) return false
            previousRawDragOffset = -1
            updateMouseSelection(
                value = value,
                currentPosition = downPosition,
                isStartOfSelection = false,
                adjustment = SelectionAdjustment.None,
            )
            return true
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            if (!enabled || value.text.isEmpty()) return false
            // can't update selection without a layoutResult, so don't consume
            state?.layoutResult ?: return false

            updateMouseSelection(
                value = value,
                currentPosition = dragPosition,
                isStartOfSelection = false,
                adjustment = SelectionAdjustment.None,
            )
            return true
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            if (!enabled || value.text.isEmpty()) return false
            // can't update selection without a layoutResult, so don't consume
            state?.layoutResult ?: return false

            focusRequester?.requestFocus()
            dragBeginPosition = downPosition
            previousRawDragOffset = -1
            enterSelectionMode()
            updateMouseSelection(
                value = value,
                currentPosition = dragBeginPosition,
                isStartOfSelection = true,
                adjustment = adjustment,
            )
            return true
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            if (!enabled || value.text.isEmpty()) return false
            // can't update selection without a layoutResult, so don't consume
            state?.layoutResult ?: return false

            updateMouseSelection(
                value = value,
                currentPosition = dragPosition,
                isStartOfSelection = false,
                adjustment = adjustment,
            )
            return true
        }

        fun updateMouseSelection(
            value: TextFieldValue,
            currentPosition: Offset,
            isStartOfSelection: Boolean,
            adjustment: SelectionAdjustment,
        ) {
            val newSelection = updateSelection(
                value = value,
                currentPosition = currentPosition,
                isStartOfSelection = isStartOfSelection,
                isStartHandle = false,
                adjustment = adjustment,
                isTouchBasedSelection = false,
            )
            setHandleState(if (newSelection.collapsed) Cursor else Selection)
        }

        override fun onDragDone() {
            /* Nothing to do */
        }
    }

    /**
     * [TextDragObserver] for dragging the selection handles to change the selection in TextField.
     */
    internal fun handleDragObserver(isStartHandle: Boolean): TextDragObserver =
        object : TextDragObserver {
            override fun onDown(point: Offset) {
                draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

                // The position of the character where the drag gesture should begin. This is in
                // the inner text field coordinates.
                val handleCoordinates = getAdjustedCoordinates(getHandlePosition(isStartHandle))

                // translate to decoration box coordinates
                val layoutResult = state?.layoutResult ?: return
                val translatedPosition =
                    layoutResult.translateInnerToDecorationCoordinates(handleCoordinates)

                dragBeginPosition = translatedPosition
                currentDragPosition = translatedPosition

                // Zero out the total distance that being dragged.
                dragTotalDistance = Offset.Zero
                previousRawDragOffset = -1

                state?.isInTouchMode = true
                updateFloatingToolbar(show = false)
            }

            override fun onUp() {
                draggingHandle = null
                currentDragPosition = null
                updateFloatingToolbar(show = true)
            }

            override fun onStart(startPoint: Offset) {
                // handled in onDown
            }

            override fun onDrag(delta: Offset) {
                dragTotalDistance += delta

                currentDragPosition = dragBeginPosition + dragTotalDistance
                updateSelection(
                    value = value,
                    currentPosition = currentDragPosition!!,
                    isStartOfSelection = false,
                    isStartHandle = isStartHandle,
                    adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                    isTouchBasedSelection = true, // handle drag infers touch
                )
                updateFloatingToolbar(show = false)
            }

            override fun onStop() {
                draggingHandle = null
                currentDragPosition = null
                updateFloatingToolbar(show = true)
            }

            override fun onCancel() {
                draggingHandle = null
                currentDragPosition = null
            }
        }

    /**
     * [TextDragObserver] for dragging the cursor to change the selection in TextField.
     */
    internal fun cursorDragObserver(): TextDragObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            // Nothing
        }

        override fun onUp() {
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onStart(startPoint: Offset) {
            // The position of the character where the drag gesture should begin. This is in
            // the inner text field coordinates.
            val handleCoordinates = getAdjustedCoordinates(getHandlePosition(true))

            // translate to decoration box coordinates
            val layoutResult = state?.layoutResult ?: return
            val translatedPosition =
                layoutResult.translateInnerToDecorationCoordinates(handleCoordinates)

            dragBeginPosition = translatedPosition
            currentDragPosition = translatedPosition
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
            draggingHandle = Handle.Cursor
            updateFloatingToolbar(show = false)
        }

        override fun onDrag(delta: Offset) {
            dragTotalDistance += delta

            state?.layoutResult?.let { layoutResult ->
                currentDragPosition = dragBeginPosition + dragTotalDistance
                val offset = offsetMapping.transformedToOriginal(
                    layoutResult.getOffsetForPosition(currentDragPosition!!)
                )

                val newSelection = TextRange(offset, offset)

                // Nothing changed, skip onValueChange hand hapticFeedback.
                if (newSelection == value.selection) return

                if (state?.isInTouchMode != false) {
                    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                onValueChange(
                    createTextFieldValue(
                        annotatedString = value.annotatedString,
                        selection = newSelection
                    )
                )
            }
        }

        override fun onStop() {
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onCancel() {}
    }

    /**
     * The method to record the required state values on entering the selection mode.
     *
     * Is triggered on long press or accessibility action.
     *
     * @param showFloatingToolbar whether to show the floating toolbar when entering selection mode
     */
    internal fun enterSelectionMode(showFloatingToolbar: Boolean = true) {
        if (state?.hasFocus == false) {
            focusRequester?.requestFocus()
        }
        oldValue = value
        updateFloatingToolbar(showFloatingToolbar)
        setHandleState(Selection)
    }

    /**
     * The method to record the corresponding state values on exiting the selection mode.
     *
     * Is triggered on accessibility action.
     */
    internal fun exitSelectionMode() {
        updateFloatingToolbar(show = false)
        setHandleState(None)
    }

    internal fun deselect(position: Offset? = null) {
        if (!value.selection.collapsed) {
            // if selection was not collapsed, set a default cursor location, otherwise
            // don't change the location of the cursor.
            val layoutResult = state?.layoutResult
            val newCursorOffset = if (position != null && layoutResult != null) {
                offsetMapping.transformedToOriginal(
                    layoutResult.getOffsetForPosition(position)
                )
            } else {
                value.selection.max
            }
            val newValue = value.copy(selection = TextRange(newCursorOffset))
            onValueChange(newValue)
        }

        // If a new cursor position is given and the text is not empty, enter the Cursor state.
        val selectionMode = if (position != null && value.text.isNotEmpty()) Cursor else None
        setHandleState(selectionMode)
        updateFloatingToolbar(show = false)
    }

    internal fun setSelectionPreviewHighlight(range: TextRange) {
        state?.selectionPreviewHighlightRange = range
        state?.deletionPreviewHighlightRange = TextRange.Zero
        if (!range.collapsed) exitSelectionMode()
    }

    internal fun setDeletionPreviewHighlight(range: TextRange) {
        state?.deletionPreviewHighlightRange = range
        state?.selectionPreviewHighlightRange = TextRange.Zero
        if (!range.collapsed) exitSelectionMode()
    }

    internal fun clearPreviewHighlight() {
        state?.deletionPreviewHighlightRange = TextRange.Zero
        state?.selectionPreviewHighlightRange = TextRange.Zero
    }

    /**
     * The method for copying text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager], and cancel the selection, if
     * [cancelSelection] is true.
     * The text in the text field should be unchanged.
     * If [cancelSelection] is true, the new cursor offset should be at the end of the previous
     * selected text.
     */
    internal fun copy(cancelSelection: Boolean = true) {
        if (value.selection.collapsed) return

        // TODO(b/171947959) check if original or transformed should be copied
        clipboardManager?.setText(value.getSelectedText())

        if (!cancelSelection) return

        val newCursorOffset = value.selection.max
        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(None)
    }

    internal fun onCopyWithResult(cancelSelection: Boolean = true): String? {
        if (value.selection.collapsed) return null
        val selectedText = value.getSelectedText().text

        if (!cancelSelection) return selectedText

        val newCursorOffset = value.selection.max
        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
        return selectedText
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
        val text = clipboardManager?.getText() ?: return

        val newText = value.getTextBeforeSelection(value.text.length) +
            text +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min + text.length

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(None)
        undoManager?.forceNextSnapshot()
    }

    internal fun paste(text: AnnotatedString) {
        val newText = value.getTextBeforeSelection(value.text.length) +
            text +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min + text.length

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
        undoManager?.forceNextSnapshot()
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

        // TODO(b/171947959) check if original or transformed should be cut
        clipboardManager?.setText(value.getSelectedText())

        val newText = value.getTextBeforeSelection(value.text.length) +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(None)
        undoManager?.forceNextSnapshot()
    }

    internal fun onCutWithResult(): String? {
        if (value.selection.collapsed) return null
        val selectedText = value.getSelectedText().text

        val newText = value.getTextBeforeSelection(value.text.length) +
            value.getTextAfterSelection(value.text.length)
        val newCursorOffset = value.selection.min

        val newValue = createTextFieldValue(
            annotatedString = newText,
            selection = TextRange(newCursorOffset, newCursorOffset)
        )
        onValueChange(newValue)
        setHandleState(HandleState.None)
        undoManager?.forceNextSnapshot()

        return selectedText
    }

    /*@VisibleForTesting*/
    internal fun selectAll() {
        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = TextRange(0, value.text.length)
        )
        onValueChange(newValue)
        oldValue = oldValue.copy(selection = newValue.selection)
        enterSelectionMode(showFloatingToolbar = true)
    }

    internal fun getHandlePosition(isStartHandle: Boolean): Offset {
        val textLayoutResult = state?.layoutResult?.value ?: return Offset.Unspecified

        // If layout and value are out of sync, return unspecified.
        // This will be called again once they are in sync.
        val transformedText = transformedText ?: return Offset.Unspecified
        val layoutInputText = textLayoutResult.layoutInput.text.text
        if (transformedText.text != layoutInputText) return Offset.Unspecified

        val offset = if (isStartHandle) value.selection.start else value.selection.end
        return getSelectionHandleCoordinates(
            textLayoutResult = textLayoutResult,
            offset = offsetMapping.originalToTransformed(offset),
            isStart = isStartHandle,
            areHandlesCrossed = value.selection.reversed
        )
    }

    internal fun getHandleLineHeight(isStartHandle: Boolean): Float {
        val layoutResult = state?.layoutResult ?: return 0f
        val offset = if (isStartHandle) value.selection.start else value.selection.end
        val line = layoutResult.value.getLineForOffset(
            offset = offsetMapping.originalToTransformed(offset)
        )
        return layoutResult.value.multiParagraph.getLineHeight(line)
    }

    internal fun getCursorPosition(density: Density): Offset {
        val offset = offsetMapping.originalToTransformed(value.selection.start)
        val layoutResult = state?.layoutResult!!.value
        val cursorRect = layoutResult.getCursorRect(
            offset.coerceIn(0, layoutResult.layoutInput.text.length)
        )
        val x = with(density) {
            cursorRect.left + DefaultCursorThickness.toPx() / 2
        }
        return Offset(x, cursorRect.bottom)
    }

    /**
     * Update the [LegacyTextFieldState.showFloatingToolbar] state and show/hide the toolbar.
     *
     * You may want to call [showSelectionToolbar] and [hideSelectionToolbar] directly without
     * updating the [LegacyTextFieldState.showFloatingToolbar] if you are simply hiding all touch
     * selection behaviors (toolbar, handles, cursor, magnifier), but want the toolbar to come
     * back when you un-hide all those behaviors.
     */
    private fun updateFloatingToolbar(show: Boolean) {
        state?.showFloatingToolbar = show
        if (show) showSelectionToolbar() else hideSelectionToolbar()
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy, paste and cut method as callbacks when "copy", "cut" or "paste" is clicked.
     */
    internal fun showSelectionToolbar() {
        if (!enabled || state?.isInTouchMode == false) return
        val isPassword = visualTransformation is PasswordVisualTransformation
        val copy: (() -> Unit)? = if (!value.selection.collapsed && !isPassword) {
            {
                copy()
                hideSelectionToolbar()
            }
        } else null

        val cut: (() -> Unit)? = if (!value.selection.collapsed && editable && !isPassword) {
            {
                cut()
                hideSelectionToolbar()
            }
        } else null

        val paste: (() -> Unit)? = if (editable && clipboardManager?.hasText() == true) {
            {
                paste()
                hideSelectionToolbar()
            }
        } else null

        val selectAll: (() -> Unit)? = if (value.selection.length != value.text.length) {
            {
                selectAll()
            }
        } else null

        textToolbar?.showMenu(
            rect = getContentRect(),
            onCopyRequested = copy,
            onPasteRequested = paste,
            onCutRequested = cut,
            onSelectAllRequested = selectAll
        )
    }

    internal fun hideSelectionToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            textToolbar?.hide()
        }
    }

    /**
     * Implements the macOS select-word-on-right-click behavior.
     *
     * If the current selection does not already include [position], select the word at [position].
     */
    fun selectWordAtPositionIfNotAlreadySelected(position: Offset) {
        val layoutResult = state?.layoutResult ?: return
        val isClickedPositionInsideSelection =
            layoutResult.value.isPositionInsideSelection(
                position = layoutResult.translateDecorationToInnerCoordinates(position),
                selectionRange = value.selection,
            )
        if (!isClickedPositionInsideSelection) {
            updateSelection(
                value = value,
                currentPosition = position,
                isStartOfSelection = true,
                isStartHandle = false,
                adjustment = SelectionAdjustment.Word,
                isTouchBasedSelection = false,
            )
        }
    }

    /**
     * Check if the text in the text field changed.
     * When the content in the text field is modified, this method returns true.
     */
    internal fun isTextChanged(): Boolean {
        return oldValue.text != value.text
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    private fun getContentRect(): Rect {
        // if it's stale layout, return empty Rect
        state?.takeIf { !it.isLayoutResultStale }?.let {
            // value.selection is from the original representation.
            // we need to convert original offsets into transformed offsets to query
            // layoutResult because layoutResult belongs to the transformed text.
            val transformedStart = offsetMapping.originalToTransformed(value.selection.start)
            val transformedEnd = offsetMapping.originalToTransformed(value.selection.end)
            val startOffset =
                state?.layoutCoordinates?.localToRoot(getHandlePosition(true))
                    ?: Offset.Zero
            val endOffset =
                state?.layoutCoordinates?.localToRoot(getHandlePosition(false))
                    ?: Offset.Zero
            val startTop =
                state?.layoutCoordinates?.localToRoot(
                    Offset(
                        0f,
                        it.layoutResult?.value?.getCursorRect(transformedStart)?.top ?: 0f
                    )
                )?.y ?: 0f
            val endTop =
                state?.layoutCoordinates?.localToRoot(
                    Offset(
                        x = 0f,
                        y = it.layoutResult?.value?.getCursorRect(transformedEnd)?.top ?: 0f
                    )
                )?.y ?: 0f

            val left = min(startOffset.x, endOffset.x)
            val right = max(startOffset.x, endOffset.x)
            val top = min(startTop, endTop)
            val bottom = max(startOffset.y, endOffset.y) +
                25.dp.value * it.textDelegate.density.density

            return Rect(left, top, right, bottom)
        }

        return Rect.Zero
    }

    /**
     * Update the text field's selection based on new offsets.
     *
     * @param value the current [TextFieldValue]
     * @param currentPosition the current position of the cursor/drag in the decoration box
     * coordinates
     * @param isStartOfSelection whether this is the first updateSelection of a selection gesture.
     * If true, will ignore any previous selection context.
     * @param isStartHandle whether the start handle is being updated
     * @param adjustment The selection adjustment to use
     * @param isTouchBasedSelection Whether this is a touch based selection
     */
    private fun updateSelection(
        value: TextFieldValue,
        currentPosition: Offset,
        isStartOfSelection: Boolean,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        isTouchBasedSelection: Boolean,
    ): TextRange {
        val layoutResult = state?.layoutResult ?: return TextRange.Zero
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
            return value.selection
        }

        this.previousSelectionLayout = selectionLayout
        previousRawDragOffset = currentOffset

        val newTransformedSelection = adjustment.adjust(selectionLayout)
        val newSelection = TextRange(
            start = offsetMapping.transformedToOriginal(newTransformedSelection.start.offset),
            end = offsetMapping.transformedToOriginal(newTransformedSelection.end.offset)
        )

        if (newSelection == value.selection) return value.selection

        val onlyChangeIsReversed = newSelection.reversed != value.selection.reversed &&
            with(newSelection) { TextRange(end, start) } == value.selection

        val bothSelectionsCollapsed = newSelection.collapsed && value.selection.collapsed
        if (isTouchBasedSelection &&
            value.text.isNotEmpty() &&
            !onlyChangeIsReversed &&
            !bothSelectionsCollapsed
        ) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        val newValue = createTextFieldValue(
            annotatedString = value.annotatedString,
            selection = newSelection
        )
        onValueChange(newValue)

        if (!isTouchBasedSelection) {
            updateFloatingToolbar(show = !newSelection.collapsed)
        }

        state?.isInTouchMode = isTouchBasedSelection

        // showSelectionHandleStart/End might be set to false when scrolled out of the view.
        // When the selection is updated, they must also be updated so that handles will be shown
        // or hidden correctly.
        state?.showSelectionHandleStart =
            !newSelection.collapsed && isSelectionHandleInVisibleBound(isStartHandle = true)
        state?.showSelectionHandleEnd =
            !newSelection.collapsed && isSelectionHandleInVisibleBound(isStartHandle = false)
        state?.showCursorHandle =
            newSelection.collapsed && isSelectionHandleInVisibleBound(isStartHandle = true)

        return newSelection
    }

    private fun setHandleState(handleState: HandleState) {
        state?.takeUnless { it.handleState == handleState }?.let { it.handleState = handleState }
    }

    private fun createTextFieldValue(
        annotatedString: AnnotatedString,
        selection: TextRange
    ): TextFieldValue {
        return TextFieldValue(
            annotatedString = annotatedString,
            selection = selection
        )
    }
}

@Composable
internal fun TextFieldSelectionHandle(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    manager: TextFieldSelectionManager
) {
    val observer = remember(isStartHandle, manager) {
        manager.handleDragObserver(isStartHandle)
    }

    SelectionHandle(
        offsetProvider = { manager.getHandlePosition(isStartHandle) },
        isStartHandle = isStartHandle,
        direction = direction,
        handlesCrossed = manager.value.selection.reversed,
        lineHeight = manager.getHandleLineHeight(isStartHandle),
        modifier = Modifier.pointerInput(observer) {
            detectDownAndDragGesturesWithObserver(observer)
        },
    )
}

/**
 * Whether the selection handle is in the visible bound of the TextField.
 */
internal expect fun TextFieldSelectionManager.isSelectionHandleInVisibleBound(
    isStartHandle: Boolean
): Boolean


internal fun TextFieldSelectionManager.isSelectionHandleInVisibleBoundDefault(
    isStartHandle: Boolean
): Boolean = state?.layoutCoordinates?.visibleBounds()?.containsInclusive(
    getHandlePosition(isStartHandle)
) ?: false

/**
 * Optionally shows a magnifier widget, if the current platform supports it, for the current state
 * of a [TextFieldSelectionManager]. Should check [TextFieldSelectionManager.draggingHandle] to see
 * which handle is being dragged and then calculate the magnifier position for that handle.
 *
 * Actual implementations should as much as possible actually live in this common source set, _not_
 * the platform-specific source sets. The actual implementations of this function should then just
 * delegate to those functions.
 */
internal expect fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier

/**
 * @return the location of the magnifier relative to the inner text field coordinates
 */
internal fun calculateSelectionMagnifierCenterAndroid(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize
): Offset {
    // state read of currentDragPosition so that we always recompose on drag position changes
    val localDragPosition = manager.currentDragPosition ?: return Offset.Unspecified

    // Never show the magnifier in an empty text field.
    if (manager.transformedText?.isEmpty() != false) return Offset.Unspecified
    val rawTextOffset = when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.Cursor,
        Handle.SelectionStart -> manager.value.selection.start

        Handle.SelectionEnd -> manager.value.selection.end
    }
    // If the text hasn't been laid out yet, don't show the magnifier.
    val textLayoutResultProxy = manager.state?.layoutResult ?: return Offset.Unspecified
    val transformedText = manager.state?.textDelegate?.text ?: return Offset.Unspecified

    val textOffset = manager.offsetMapping
        .originalToTransformed(rawTextOffset)
        .coerceIn(0, transformedText.length)

    val dragX = textLayoutResultProxy.translateDecorationToInnerCoordinates(localDragPosition).x

    val layoutResult = textLayoutResultProxy.value
    val line = layoutResult.getLineForOffset(textOffset)
    val lineStart = layoutResult.getLineLeft(line)
    val lineEnd = layoutResult.getLineRight(line)
    val lineMin = minOf(lineStart, lineEnd)
    val lineMax = maxOf(lineStart, lineEnd)
    val centerX = dragX.coerceIn(lineMin, lineMax)

    // Hide the magnifier when dragged too far (outside the horizontal bounds of how big the
    // magnifier actually is). See
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/Editor.java;l=5228-5231;drc=2fdb6bd709be078b72f011334362456bb758922c
    // Also check whether magnifierSize is calculated. A platform magnifier instance is not
    // created until it's requested for the first time. So the size will only be calculated after we
    // return a specified offset from this function.
    // It is very unlikely that this behavior would cause a flicker since magnifier immediately
    // shows up where the pointer is being dragged. The pointer needs to drag further than the half
    // of magnifier's width to hide by the following logic.
    if (magnifierSize != IntSize.Zero &&
        (dragX - centerX).absoluteValue > magnifierSize.width / 2
    ) {
        return Offset.Unspecified
    }

    // Center vertically on the current line.
    val top = layoutResult.getLineTop(line)
    val bottom = layoutResult.getLineBottom(line)
    val centerY = ((bottom - top) / 2) + top

    return Offset(centerX, centerY)
}
