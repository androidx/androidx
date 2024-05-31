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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.content.readPlainText
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.getSelectedText
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Deletion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Insertion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Replacement
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Untransformed
import androidx.compose.foundation.text.input.internal.SelectionWedgeAffinity
import androidx.compose.foundation.text.input.internal.TextFieldDecoratorModifierNode
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.WedgeAffinity
import androidx.compose.foundation.text.input.internal.coerceIn
import androidx.compose.foundation.text.input.internal.findClosestRect
import androidx.compose.foundation.text.input.internal.fromDecorationToTextLayout
import androidx.compose.foundation.text.input.internal.getIndexTransformationType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Cursor
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.None
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Selection
import androidx.compose.foundation.text.input.internal.undo.TextFieldEditUndoBehavior
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionLayout
import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.foundation.text.selection.getAdjustedCoordinates
import androidx.compose.foundation.text.selection.getSelectionHandleCoordinates
import androidx.compose.foundation.text.selection.getTextFieldSelectionLayout
import androidx.compose.foundation.text.selection.isPrecisePointer
import androidx.compose.foundation.text.selection.selectionGesturePointerInputBtf2
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Handles selection behaviors and gestures for a text field.
 *
 * @param textFieldState The [TransformedTextFieldState] associated with this text field.
 * @param textLayoutState The [TextLayoutState] associated with this text field.
 * @param density The [Density] used for this text field.
 * @param enabled If false, all selection behaviors and gestures will be disabled.
 * @param readOnly If true, selection behaviors still work, but the text field cannot be edited.
 * @param isFocused True iff component is focused and the window is focused.
 * @param isPassword True if the text field is for a password.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldSelectionState(
    private val textFieldState: TransformedTextFieldState,
    private val textLayoutState: TextLayoutState,
    private var density: Density,
    private var enabled: Boolean,
    private var readOnly: Boolean,
    var isFocused: Boolean,
    private var isPassword: Boolean,
) {
    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    private var hapticFeedBack: HapticFeedback? = null

    /**
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    private var textToolbar: TextToolbar? = null

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    private var clipboardManager: ClipboardManager? = null

    /**
     * Whether user is interacting with the UI in touch mode.
     */
    var isInTouchMode: Boolean by mutableStateOf(true)

    /**
     * Reduced [ReceiveContentConfiguration] from the attached modifier node hierarchy. This value
     * is set by [TextFieldDecoratorModifierNode].
     */
    var receiveContentConfiguration: (() -> ReceiveContentConfiguration?)? = null

    /**
     * The position of text layout when dragging is started by a cursor or a selection handle.
     * Total drag value needs to account for any auto scrolling that happens during dragging of a
     * handle.
     * This value is an anchor to calculate how much the text layout has moved in the window as
     * dragging continues. If a cursor or a selection handle is not dragging, this value needs to be
     * [Offset.Unspecified]. This includes long press and drag gesture defined on TextField.
     */
    private var startTextLayoutPositionInWindow by mutableStateOf(Offset.Unspecified)

    /**
     * Calculates the offset of currently visible bounds.
     */
    private val currentTextLayoutPositionInWindow: Offset
        get() = textLayoutCoordinates?.positionInWindow() ?: Offset.Unspecified

    /**
     * Current drag position of a handle for magnifier to read. Only one handle can be dragged
     * at one time. This uses raw position as in only gesture start position and delta are used to
     * calculate it. If auto-scroll happens due to selection changes while the gesture is active,
     * it is not reflected on this value. See [handleDragPosition] for such a behavior.
     *
     * This value can reflect the drag position of either a real handle like cursor or selection or
     * an acting handle when long press dragging happens directly on the text field. However, these
     * two systems (real and acting handles) use different coordinate systems. When real handles
     * set this value, they send inner text field coordinates. On the other hand, long press and
     * drag gesture defined on text field would send coordinates in the decoration coordinates.
     */
    private var rawHandleDragPosition by mutableStateOf(Offset.Unspecified)

    /**
     * Defines where the handle exactly is in text layout node coordinates. This is mainly used by
     * Magnifier to anchor itself. Also, it provides an updated total drag value to cursor and
     * selection handles to continue scrolling as they are dragged outside the visible bounds.
     *
     * This value is primarily used by Magnifier and any handle dragging gesture detector. Since
     * these calculations use inner text field coordinates, [handleDragPosition] is also always
     * represented in the same coordinate system.
     */
    val handleDragPosition: Offset
        get() = when {
            // nothing is being dragged.
            rawHandleDragPosition.isUnspecified -> {
                Offset.Unspecified
            }
            // no real handle is being dragged, we need to offset the drag position by current
            // inner-decorator relative positioning.
            startTextLayoutPositionInWindow.isUnspecified -> {
                textLayoutState.fromDecorationToTextLayout(rawHandleDragPosition)
            }
            // a cursor or a selection handle is being dragged, offset by comparing the current
            // and starting text layout positions.
            else -> {
                rawHandleDragPosition +
                    (startTextLayoutPositionInWindow - currentTextLayoutPositionInWindow)
            }
        }

    /**
     * Which selection handle is currently being dragged.
     */
    var draggingHandle by mutableStateOf<Handle?>(null)

    enum class InputType { None, Touch, Mouse }

    /**
     * The initiator of the current drag directly on the text field. This does not include drags of
     * the selection handles or cursor. This will be [InputType.None] if no drag is currently
     * ongoing. Otherwise, the result of this will be the [InputType] of the drag initiator.
     */
    var directDragGestureInitiator: InputType by mutableStateOf(InputType.None)

    /**
     * Whether to show the cursor handle below cursor indicator when the TextField is focused.
     */
    private var showCursorHandle by mutableStateOf(false)

    /**
     * Whether to show the TextToolbar according to current selection state. This is not the final
     * decider for showing the toolbar. Please refer to [observeTextToolbarVisibility] docs.
     */
    private var textToolbarState by mutableStateOf(None)

    /**
     * Access helper for text layout node coordinates that checks attached state.
     */
    private val textLayoutCoordinates: LayoutCoordinates?
        get() = textLayoutState.textLayoutNodeCoordinates?.takeIf { it.isAttached }

    /**
     * Whether the contents of this TextField can be changed by the user.
     */
    private val editable: Boolean
        get() = enabled && !readOnly

    /**
     * The most recent [SelectionLayout] that passed the [SelectionLayout.shouldRecomputeSelection]
     * check. Provides context to the next selection update such as if the selection is shrinking
     * or not.
     */
    private var previousSelectionLayout: SelectionLayout? = null

    /**
     * The previous offset of a drag, before selection adjustments.
     * Only update when a selection layout change has occurred,
     * or set to -1 if a new drag begins.
     */
    private var previousRawDragOffset: Int = -1

    private var pressInteraction: PressInteraction.Press? = null

    /**
     * State of the cursor handle that includes its visibility and position.
     *
     * Pass [includePosition] as false to omit the position from the result. This helps create a
     * derived state which does not invalidate according position changes.
     */
    internal fun getCursorHandleState(includePosition: Boolean): TextFieldHandleState {
        // For cursor handle to be visible, [showCursorHandle] must be true and the selection
        // must be collapsed.
        // Also, cursor handle should be in visible bounds of the TextField. However, if
        // cursor is dragging and gets out of bounds, we cannot remove it from composition
        // because that would stop the drag gesture defined on it. Instead, we allow the handle
        // to be visible as long as it's being dragged.
        // Visible bounds calculation lags one frame behind to let auto-scrolling settle.

        // Register all state reads here, don't let them short-circuit below.
        val text = textFieldState.visualText
        val showCursorHandle = showCursorHandle
        val notBeingDragged = directDragGestureInitiator == InputType.None
        val draggingHandle = draggingHandle

        val visible = showCursorHandle &&
            notBeingDragged &&
            text.selection.collapsed &&
            text.shouldShowSelection() &&
            text.isNotEmpty() &&
            (draggingHandle == Handle.Cursor || isCursorHandleInVisibleBounds())

        if (!visible) return TextFieldHandleState.Hidden

        // text direction is useless for cursor handle, any value is fine.
        return TextFieldHandleState(
            visible = true,
            position = if (includePosition) getCursorRect().bottomCenter else Offset.Unspecified,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false
        )
    }

    /**
     * Whether currently cursor handle is in visible bounds. This function does not register a
     * state read because every selection change is processed in layout phase by the auto-scroll
     * behavior.
     *
     * We can say that whether cursor is in visible bounds or not only after giving auto-scroll
     * time to process the cursor movement, and possibly scroll the cursor back into view. This is
     * guaranteed to happen after scroll since new [textLayoutCoordinates] are reported after the
     * layout phase ends.
     */
    private fun isCursorHandleInVisibleBounds(): Boolean {
        val position = Snapshot.withoutReadObservation { getCursorRect().bottomCenter }

        return textLayoutCoordinates?.visibleBounds()?.containsInclusive(position) ?: false
    }

    /**
     * Where the cursor should be at any given time in core node coordinates.
     *
     * Returns [Rect.Zero] if text layout has not been calculated yet or the selection is not
     * collapsed (no cursor to locate).
     */
    fun getCursorRect(): Rect {
        val layoutResult = textLayoutState.layoutResult ?: return Rect.Zero
        val value = textFieldState.visualText
        if (!value.selection.collapsed) return Rect.Zero
        val cursorRect = layoutResult.getCursorRect(value.selection.start)

        val cursorWidth = with(density) { DefaultCursorThickness.toPx() }
        // left and right values in cursorRect should be the same but in any case use the
        // logically correct anchor.
        val cursorCenterX = if (layoutResult.layoutInput.layoutDirection == LayoutDirection.Ltr) {
            (cursorRect.left + cursorWidth / 2)
        } else {
            (cursorRect.right - cursorWidth / 2)
        }

        // don't let cursor go beyond the bounds of text layout node or cursor will be clipped.
        // but also make sure that empty Text Layout still draws a cursor.
        val coercedCursorCenterX = cursorCenterX
            // do not use coerceIn because it is not guaranteed that minimum value is smaller
            // than the maximum value.
            .coerceAtMost(layoutResult.size.width - cursorWidth / 2)
            .coerceAtLeast(cursorWidth / 2)

        return Rect(
            left = coercedCursorCenterX - cursorWidth / 2,
            right = coercedCursorCenterX + cursorWidth / 2,
            top = cursorRect.top,
            bottom = cursorRect.bottom
        )
    }

    fun update(
        hapticFeedBack: HapticFeedback,
        clipboardManager: ClipboardManager,
        textToolbar: TextToolbar,
        density: Density,
        enabled: Boolean,
        readOnly: Boolean,
        isPassword: Boolean,
    ) {
        if (!enabled) {
            hideTextToolbar()
        }
        this.hapticFeedBack = hapticFeedBack
        this.clipboardManager = clipboardManager
        this.textToolbar = textToolbar
        this.density = density
        this.enabled = enabled
        this.readOnly = readOnly
        this.isPassword = isPassword
    }

    /**
     * Implements the complete set of gestures supported by the cursor handle.
     */
    suspend fun PointerInputScope.cursorHandleGestures() {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTouchMode()
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectCursorHandleDragGestures()
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTapGestures(onTap = {
                    textToolbarState = if (textToolbarState == Cursor) {
                        None
                    } else {
                        Cursor
                    }
                })
            }
        }
    }

    /**
     * Gesture detector for dragging the selection handles to change the selection in TextField.
     */
    suspend fun PointerInputScope.selectionHandleGestures(isStartHandle: Boolean) {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTouchMode()
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectPressDownGesture(
                    onDown = {
                        markStartContentVisibleOffset()
                        updateHandleDragging(
                            handle = if (isStartHandle) {
                                Handle.SelectionStart
                            } else {
                                Handle.SelectionEnd
                            },
                            position = getAdjustedCoordinates(getHandlePosition(isStartHandle))
                        )
                    },
                    onUp = {
                        clearHandleDragging()
                    }
                )
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectSelectionHandleDragGestures(isStartHandle)
            }
        }
    }

    /**
     * Starts observing changes in the current state for reactive rules. For example, the cursor
     * handle or the selection handles should hide whenever the text content changes.
     */
    suspend fun observeChanges() {
        try {
            coroutineScope {
                launch { observeTextChanges() }
                launch { observeTextToolbarVisibility() }
            }
        } finally {
            showCursorHandle = false
            if (textToolbarState != None) {
                hideTextToolbar()
            }
        }
    }

    fun updateTextToolbarState(textToolbarState: TextToolbarState) {
        this.textToolbarState = textToolbarState
    }

    fun dispose() {
        hideTextToolbar()

        textToolbar = null
        clipboardManager = null
        hapticFeedBack = null
    }

    /**
     * Detects the current pointer type in this [PointerInputScope] to update the touch mode state.
     * This helper gesture detector should be added to all TextField pointer input receivers such
     * as TextFieldDecorator, cursor handle, and selection handles.
     */
    suspend fun PointerInputScope.detectTouchMode() {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                isInTouchMode = !event.isPrecisePointer
            }
        }
    }

    suspend fun PointerInputScope.detectTextFieldTapGestures(
        interactionSource: MutableInteractionSource?,
        requestFocus: () -> Unit,
        showKeyboard: () -> Unit
    ) {
        detectTapAndPress(
            onTap = { offset ->
                logDebug { "onTapTextField" }
                requestFocus()

                if (enabled && isFocused) {
                    if (!readOnly) {
                        showKeyboard()
                        if (textFieldState.visualText.isNotEmpty()) {
                            showCursorHandle = true
                        }
                    }

                    // do not show any TextToolbar.
                    updateTextToolbarState(None)

                    val coercedOffset = textLayoutState.coercedInVisibleBoundsOfInputText(offset)

                    placeCursorAtNearestOffset(
                        textLayoutState.fromDecorationToTextLayout(coercedOffset)
                    )
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
                            val endInteraction = if (success) {
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

    /**
     * Calculates the valid cursor position nearest to [offset] and sets the cursor to it.
     * Takes into account text transformations ([TransformedTextFieldState]) to avoid putting the
     * cursor in the middle of replacements.
     *
     * If the cursor would end up in the middle of an insertion or replacement, it is instead pushed
     * to the nearest edge of the wedge to the [offset].
     *
     * @param offset Where the cursor is in text layout coordinates. If the caller has the offset
     * in decorator coordinates, [TextLayoutState.fromDecorationToTextLayout] can be used to convert
     * between the two spaces.
     * @return true if the cursor moved, false if the cursor position did not need to change.
     */
    private fun placeCursorAtNearestOffset(offset: Offset): Boolean {
        val layoutResult = textLayoutState.layoutResult ?: return false

        // First step: calculate the proposed cursor index.
        val index = layoutResult.getOffsetForPosition(offset)
        if (index == -1) return false

        // Second step: if a transformation is applied, determine if the proposed cursor position
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
                        newAffinity = if (offset.findClosestRect(
                                wedgeStartCursorRect,
                                wedgeEndCursorRect
                            ) < 0
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
        if (untransformedCursorRange == textFieldState.untransformedText.selection &&
            (newAffinity == null || newAffinity == textFieldState.selectionWedgeAffinity)
        ) {
            return false
        }

        textFieldState.selectUntransformedCharsIn(untransformedCursorRange)
        newAffinity?.let {
            textFieldState.selectionWedgeAffinity = it
        }
        return true
    }

    private suspend fun PointerInputScope.detectCursorHandleDragGestures() {
        var cursorDragStart = Offset.Unspecified
        var cursorDragDelta = Offset.Unspecified

        fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (cursorDragStart.isSpecified) {
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                clearHandleDragging()
            }
        }

        // b/288931376: detectDragGestures do not call onDragCancel when composable is disposed.
        try {
            detectDragGestures(
                onDragStart = {
                    // mark start drag point
                    cursorDragStart = getAdjustedCoordinates(getCursorRect().bottomCenter)
                    cursorDragDelta = Offset.Zero
                    isInTouchMode = true
                    markStartContentVisibleOffset()
                    updateHandleDragging(Handle.Cursor, cursorDragStart)
                },
                onDragEnd = { onDragStop() },
                onDragCancel = { onDragStop() },
                onDrag = onDrag@{ change, dragAmount ->
                    cursorDragDelta += dragAmount

                    updateHandleDragging(Handle.Cursor, cursorDragStart + cursorDragDelta)

                    if (placeCursorAtNearestOffset(handleDragPosition)) {
                        change.consume()
                        // TODO: only perform haptic feedback if filter does not override the change
                        hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            )
        } finally {
            onDragStop()
        }
    }

    /**
     * Includes:
     * * Touch
     *     * Long press selects the pressed word and then detects drags to continue selecting words.
     *     * Double tap selects the current word and immediately detects drags to continue selecting
     *       words.
     *     * Subsequent quick taps still act as a double tap.
     * * Mouse
     *     * Clicks immediately start a selection and begins detecting drags:
     *         * 1 -> click creates collapsed selection (places cursor) drags select individual
     *           chars.
     *         * 2 -> click selects current word, drags select words.
     *         * 3+ -> click selects current paragraph, drags select paragraphs.
     *
     * Notably does *not* include:
     * * Single taps from any source. This is handled by [detectTextFieldTapGestures].
     *   Only exception is the first mouse down does immediately place the cursor at the position.
     */
    suspend fun PointerInputScope.textFieldSelectionGestures(requestFocus: () -> Unit) {
        selectionGesturePointerInputBtf2(
            mouseSelectionObserver = TextFieldMouseSelectionObserver(requestFocus),
            textDragObserver = TextFieldTextDragObserver(requestFocus),
        )
    }

    private inner class TextFieldMouseSelectionObserver(
        private val requestFocus: () -> Unit
    ) : MouseSelectionObserver {
        private var dragBeginOffsetInText = -1
        private var dragBeginPosition: Offset = Offset.Unspecified

        override fun onStart(downPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            if (!enabled || textFieldState.visualText.isEmpty()) return false
            logDebug { "Mouse.onStart" }
            directDragGestureInitiator = InputType.Mouse

            requestFocus()

            previousRawDragOffset = -1
            dragBeginOffsetInText = -1
            dragBeginPosition = downPosition

            val newSelection = updateSelection(downPosition, adjustment, isStartOfSelection = true)
            dragBeginOffsetInText = newSelection.start

            return true
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            if (!enabled || textFieldState.visualText.isEmpty()) return false
            logDebug { "Mouse.onDrag $dragPosition" }
            updateSelection(dragPosition, adjustment, isStartOfSelection = false)
            return true
        }

        private fun updateSelection(
            dragPosition: Offset,
            adjustment: SelectionAdjustment,
            isStartOfSelection: Boolean,
        ): TextRange {
            val startOffset: Int = dragBeginOffsetInText.takeIf { it >= 0 }
                ?: textLayoutState.getOffsetForPosition(
                    position = dragBeginPosition,
                    coerceInVisibleBounds = false
                )

            val endOffset: Int = textLayoutState.getOffsetForPosition(
                position = dragPosition,
                coerceInVisibleBounds = false
            )

            var newSelection = updateSelection(
                textFieldCharSequence = textFieldState.visualText,
                startOffset = startOffset,
                endOffset = endOffset,
                isStartHandle = false,
                adjustment = adjustment,
                allowPreviousSelectionCollapsed = false,
                isStartOfSelection = isStartOfSelection,
            )

            // When drag starts from the end padding, we eventually need to update the start
            // point once a selection is initiated. Otherwise, startOffset is always calculated
            // from dragBeginPosition which can refer to different positions on text if
            // TextField starts scrolling.
            if (dragBeginOffsetInText == -1 && !newSelection.collapsed) {
                dragBeginOffsetInText = newSelection.start
            }

            // Although we support reversed selection, reversing the selection after it's
            // initiated via long press has a visual glitch that's hard to get rid of. When
            // handles (start/end) switch places after the selection reverts, draw happens a
            // bit late, making it obvious that selection handles switched places. We simply do
            // not allow reversed selection during long press drag.
            if (newSelection.reversed) {
                newSelection = newSelection.reverse()
            }

            textFieldState.selectCharsIn(newSelection)
            updateTextToolbarState(Selection)

            return newSelection
        }

        override fun onDragDone() {
            logDebug { "Mouse.onDragDone" }
            directDragGestureInitiator = InputType.None
        }

        override fun onExtend(downPosition: Offset): Boolean {
            logDebug { "Mouse.onExtend" }
            return true
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            logDebug { "Mouse.onExtendDrag" }
            return true
        }
    }

    private inner class TextFieldTextDragObserver(
        private val requestFocus: () -> Unit
    ) : TextDragObserver {
        private var dragBeginOffsetInText = -1
        private var dragBeginPosition: Offset = Offset.Unspecified
        private var dragTotalDistance: Offset = Offset.Zero
        private var actingHandle: Handle = Handle.SelectionEnd // start with a placeholder.

        private fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (dragBeginPosition.isSpecified) {
                logDebug { "Touch.onDragStop" }
                clearHandleDragging()
                dragBeginOffsetInText = -1
                dragBeginPosition = Offset.Unspecified
                dragTotalDistance = Offset.Zero
                previousRawDragOffset = -1

                directDragGestureInitiator = InputType.None
                requestFocus()
            }
        }

        override fun onDown(point: Offset) = Unit
        override fun onUp() = Unit
        override fun onStop() = onDragStop()
        override fun onCancel() = onDragStop()

        override fun onStart(startPoint: Offset) {
            if (!enabled) return
            logDebug { "Touch.onDragStart after longPress at $startPoint" }
            // this gesture detector is applied on the decoration box. We do not need to
            // convert the gesture offset, that's going to be calculated by [handleDragPosition]
            updateHandleDragging(
                handle = actingHandle,
                position = startPoint
            )

            showCursorHandle = false
            directDragGestureInitiator = InputType.Touch

            dragBeginPosition = startPoint
            dragTotalDistance = Offset.Zero
            previousRawDragOffset = -1

            // Long Press at the blank area, the cursor should show up at the end of the line.
            if (!textLayoutState.isPositionOnText(startPoint)) {
                val offset = textLayoutState.getOffsetForPosition(startPoint)

                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                textFieldState.placeCursorBeforeCharAt(offset)
                showCursorHandle = true
                updateTextToolbarState(Cursor)
            } else {
                if (textFieldState.visualText.isEmpty()) return
                val offset = textLayoutState.getOffsetForPosition(startPoint)
                val newSelection = updateSelection(
                    // reset selection, otherwise a previous selection may be used
                    // as context for creating the next selection
                    textFieldCharSequence = TextFieldCharSequence(
                        textFieldState.visualText,
                        TextRange.Zero
                    ),
                    startOffset = offset,
                    endOffset = offset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word,
                )
                textFieldState.selectCharsIn(newSelection)
                updateTextToolbarState(Selection)

                // For touch, set the begin offset to the adjusted selection.
                // When char based selection is used, we want to ensure we snap the
                // beginning offset to the start word boundary of the first selected word.
                dragBeginOffsetInText = newSelection.start
            }
        }

        override fun onDrag(delta: Offset) {
            // selection never started, did not consume any drag
            if (!enabled || textFieldState.visualText.isEmpty()) return

            dragTotalDistance += delta

            // "start position + total delta" is not enough to understand the current
            // pointer position relative to text layout. We need to also account for any
            // changes to visible offset that's caused by auto-scrolling while dragging.
            val currentDragPosition = dragBeginPosition + dragTotalDistance

            logDebug { "Touch.onDrag at $currentDragPosition" }

            val startOffset: Int
            val endOffset: Int
            val adjustment: SelectionAdjustment

            if (
                dragBeginOffsetInText < 0 && // drag started in end padding
                !textLayoutState.isPositionOnText(currentDragPosition) // still in end padding
            ) {
                startOffset = textLayoutState.getOffsetForPosition(dragBeginPosition)
                endOffset = textLayoutState.getOffsetForPosition(currentDragPosition)

                adjustment = if (startOffset == endOffset) {
                    // start and end is in the same end padding, keep the collapsed selection
                    SelectionAdjustment.None
                } else {
                    SelectionAdjustment.Word
                }
            } else {
                startOffset = dragBeginOffsetInText.takeIf { it >= 0 }
                    ?: textLayoutState.getOffsetForPosition(
                        position = dragBeginPosition,
                        coerceInVisibleBounds = false
                    )
                endOffset = textLayoutState.getOffsetForPosition(
                    position = currentDragPosition,
                    coerceInVisibleBounds = false
                )

                if (dragBeginOffsetInText < 0 && startOffset == endOffset) {
                    // if we are selecting starting from end padding,
                    // don't start selection until we have and un-collapsed selection.
                    return
                }

                adjustment = SelectionAdjustment.Word
                updateTextToolbarState(Selection)
            }

            val prevSelection = textFieldState.visualText.selection
            var newSelection = updateSelection(
                textFieldCharSequence = textFieldState.visualText,
                startOffset = startOffset,
                endOffset = endOffset,
                isStartHandle = false,
                adjustment = adjustment,
                allowPreviousSelectionCollapsed = false,
            )

            // When drag starts from the end padding, we eventually need to update the start
            // point once a selection is initiated. Otherwise, startOffset is always calculated
            // from dragBeginPosition which can refer to different positions on text if
            // TextField starts scrolling.
            if (dragBeginOffsetInText == -1 && !newSelection.collapsed) {
                dragBeginOffsetInText = newSelection.start
            }

            // Although we support reversed selection, reversing the selection after it's
            // initiated via long press has a visual glitch that's hard to get rid of. When
            // handles (start/end) switch places after the selection reverts, draw happens a
            // bit late, making it obvious that selection handles switched places. We simply do
            // not allow reversed selection during long press drag.
            if (newSelection.reversed) {
                newSelection = newSelection.reverse()
            }

            // if the new selection is not equal to previous selection, consider updating the
            // acting handle. Otherwise, acting handle should remain the same.
            if (newSelection != prevSelection) {
                // Find the growing direction of selection
                // - Since we do not allow reverse selection,
                //   - selection.start == selection.min
                //   - selection.end == selection.max
                // - If only start or end changes ([A, B] => [A, C]; [C, E] => [D, E])
                //   - acting handle is the changing handle.
                // - If both change, find the middle point and see how it moves.
                //   - If middle point moves right, acting handle is SelectionEnd
                //   - Otherwise, acting handle is SelectionStart
                actingHandle = when {
                    newSelection.start != prevSelection.start &&
                        newSelection.end == prevSelection.end -> Handle.SelectionStart

                    newSelection.start == prevSelection.start &&
                        newSelection.end != prevSelection.end -> Handle.SelectionEnd

                    else -> {
                        val newMiddle = (newSelection.start + newSelection.end) / 2f
                        val prevMiddle = (prevSelection.start + prevSelection.end) / 2f
                        if (newMiddle > prevMiddle) {
                            Handle.SelectionEnd
                        } else {
                            Handle.SelectionStart
                        }
                    }
                }
            }

            // Do not allow selection to collapse on itself while dragging. Selection can
            // reverse but does not collapse.
            if (prevSelection.collapsed || !newSelection.collapsed) {
                textFieldState.selectCharsIn(newSelection)
            }
            updateHandleDragging(
                handle = actingHandle,
                position = currentDragPosition
            )
        }
    }

    private suspend fun PointerInputScope.detectSelectionHandleDragGestures(
        isStartHandle: Boolean
    ) {
        var dragBeginPosition: Offset = Offset.Unspecified
        var dragTotalDistance: Offset = Offset.Zero
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

        fun onDragStop() {
            // Only execute clear-up if drag was actually ongoing.
            if (dragBeginPosition.isSpecified) {
                clearHandleDragging()
                dragBeginPosition = Offset.Unspecified
                dragTotalDistance = Offset.Zero
                previousRawDragOffset = -1
            }
        }

        // b/288931376: detectDragGestures do not call onDragCancel when composable is disposed.
        try {
            detectDragGestures(
                onDragStart = {
                    // The position of the character where the drag gesture should begin. This is in
                    // the composable coordinates.
                    dragBeginPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))

                    // no need to call markStartContentVisibleOffset, since it was called by the
                    // initial down event.
                    updateHandleDragging(handle, dragBeginPosition)

                    // Zero out the total distance that being dragged.
                    dragTotalDistance = Offset.Zero

                    previousRawDragOffset = -1
                },
                onDragEnd = { onDragStop() },
                onDragCancel = { onDragStop() },
                onDrag = onDrag@{ _, delta ->
                    dragTotalDistance += delta
                    val layoutResult = textLayoutState.layoutResult ?: return@onDrag

                    updateHandleDragging(handle, dragBeginPosition + dragTotalDistance)

                    val startOffset = if (isStartHandle) {
                        layoutResult.getOffsetForPosition(handleDragPosition)
                    } else {
                        textFieldState.visualText.selection.start
                    }

                    val endOffset = if (isStartHandle) {
                        textFieldState.visualText.selection.end
                    } else {
                        layoutResult.getOffsetForPosition(handleDragPosition)
                    }

                    val prevSelection = textFieldState.visualText.selection
                    val newSelection = updateSelection(
                        textFieldCharSequence = textFieldState.visualText,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        isStartHandle = isStartHandle,
                        adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                    )
                    // Do not allow selection to collapse on itself while dragging selection
                    // handles. Selection can reverse but does not collapse.
                    if (prevSelection.collapsed || !newSelection.collapsed) {
                        textFieldState.selectCharsIn(newSelection)
                    }
                }
            )
        } finally {
            logDebug {
                "Selection Handle drag cancelled for " +
                    "draggingHandle: $draggingHandle definedOn: $handle"
            }
            if (draggingHandle == handle) {
                onDragStop()
            }
        }
    }

    private suspend fun observeTextChanges() {
        snapshotFlow { textFieldState.visualText }
            .distinctUntilChanged(TextFieldCharSequence::contentEquals)
            // first value needs to be dropped because it cannot be compared to a prior value
            .drop(1)
            .collect {
                showCursorHandle = false
                // hide the toolbar any time text content changes.
                updateTextToolbarState(None)
            }
    }

    /**
     * Manages the visibility of text toolbar according to current state and received events from
     * various sources.
     *
     * - Tapping the cursor handle toggles the visibility of the toolbar [TextToolbarState.Cursor].
     * - Dragging the cursor handle or selection handles temporarily hides the toolbar
     * [draggingHandle].
     * - Tapping somewhere on the TextField, whether it causes a cursor position change or not,
     * fully hides the toolbar [TextToolbarState.None].
     * - When cursor or selection leaves the visible bounds, text toolbar is temporarily hidden.
     * [getContentRect]
     * - When selection is initiated via long press, double click, or semantics, text toolbar shows
     * [TextToolbarState.Selection]
     */
    private suspend fun observeTextToolbarVisibility() {
        snapshotFlow {
            val isCollapsed = textFieldState.visualText.selection.collapsed
            val textToolbarStateVisible =
                isCollapsed && textToolbarState == Cursor ||
                    !isCollapsed && textToolbarState == Selection

            val textToolbarVisible =
                // toolbar is requested specifically for the current selection state
                textToolbarStateVisible &&
                    draggingHandle == null && // not dragging any selection handles
                    isInTouchMode // toolbar hidden when not in touch mode

            // final visibility decision is made by contentRect visibility.
            // if contentRect is not in visible bounds, just pass Rect.Zero to the observer so that
            // it hides the toolbar. If Rect is successfully passed to the observer, toolbar will
            // be displayed.
            if (!textToolbarVisible) {
                Rect.Zero
            } else {
                // contentRect is calculated in root coordinates. VisibleBounds are in
                // textLayoutCoordinates. Convert visibleBounds to root before checking the overlap.
                val visibleBounds = textLayoutCoordinates?.visibleBounds()
                if (visibleBounds != null) {
                    val visibleBoundsTopLeftInRoot =
                        textLayoutCoordinates?.localToRoot(visibleBounds.topLeft)
                    val visibleBoundsInRoot =
                        Rect(visibleBoundsTopLeftInRoot!!, visibleBounds.size)

                    // contentRect can be very wide if a big part of text is selected. Our toolbar
                    // should be aligned only to visible region.
                    getContentRect()
                        .takeIf { visibleBoundsInRoot.overlaps(it) }
                        ?.intersect(visibleBoundsInRoot)
                        ?: Rect.Zero
                } else {
                    Rect.Zero
                }
            }
        }.collect { rect ->
            if (rect == Rect.Zero) {
                hideTextToolbar()
            } else {
                showTextToolbar(rect)
            }
        }
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    private fun getContentRect(): Rect {
        val text = textFieldState.visualText
        // accept cursor position as content rect when selection is collapsed
        // contentRect is defined in text layout node coordinates, so it needs to be realigned to
        // the root container.
        if (text.selection.collapsed) {
            val cursorRect = getCursorRect()
            val topLeft = textLayoutCoordinates?.localToRoot(cursorRect.topLeft) ?: Offset.Zero
            return Rect(topLeft, cursorRect.size)
        }
        val startOffset =
            textLayoutCoordinates?.localToRoot(getHandlePosition(true)) ?: Offset.Zero
        val endOffset =
            textLayoutCoordinates?.localToRoot(getHandlePosition(false)) ?: Offset.Zero
        val startTop =
            textLayoutCoordinates?.localToRoot(
                Offset(
                    0f,
                    textLayoutState.layoutResult?.getCursorRect(
                        text.selection.start
                    )?.top ?: 0f
                )
            )?.y ?: 0f
        val endTop =
            textLayoutCoordinates?.localToRoot(
                Offset(
                    0f,
                    textLayoutState.layoutResult?.getCursorRect(
                        text.selection.end
                    )?.top ?: 0f
                )
            )?.y ?: 0f

        return Rect(
            left = min(startOffset.x, endOffset.x),
            right = max(startOffset.x, endOffset.x),
            top = min(startTop, endTop),
            bottom = max(startOffset.y, endOffset.y)
        )
    }

    /**
     * Calculates and returns the [TextFieldHandleState] of the requested selection handle which is
     * specified by [isStartHandle]. Pass [includePosition] as false to omit the position from the
     * result. This helps create a derived state which does not invalidate according position
     * changes.
     */
    internal fun getSelectionHandleState(
        isStartHandle: Boolean,
        includePosition: Boolean
    ): TextFieldHandleState {
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

        val layoutResult = textLayoutState.layoutResult ?: return TextFieldHandleState.Hidden

        val selection = textFieldState.visualText.selection

        if (selection.collapsed) return TextFieldHandleState.Hidden

        val position = getHandlePosition(isStartHandle)

        val visible = directDragGestureInitiator == InputType.None && (draggingHandle == handle ||
            (textLayoutCoordinates?.visibleBounds()?.containsInclusive(position) ?: false))

        if (!visible) return TextFieldHandleState.Hidden

        if (!textFieldState.visualText.shouldShowSelection()) return TextFieldHandleState.Hidden

        val directionOffset = if (isStartHandle) selection.start else max(selection.end - 1, 0)
        val direction = layoutResult.getBidiRunDirection(directionOffset)
        val handlesCrossed = selection.reversed

        // Handle normally is visible when it's out of bounds but when the handle is being dragged,
        // we let it stay on the screen to maintain gesture continuation. However, we still want
        // to coerce handle's position to visible bounds to not let it jitter while scrolling the
        // TextField as the selection is expanding.
        val coercedPosition = if (includePosition) {
            textLayoutCoordinates?.visibleBounds()?.let { position.coerceIn(it) } ?: position
        } else {
            Offset.Unspecified
        }
        return TextFieldHandleState(
            visible = true,
            position = coercedPosition,
            direction = direction,
            handlesCrossed = handlesCrossed
        )
    }

    private fun getHandlePosition(isStartHandle: Boolean): Offset {
        val layoutResult = textLayoutState.layoutResult ?: return Offset.Zero
        val selection = textFieldState.visualText.selection
        val offset = if (isStartHandle) {
            selection.start
        } else {
            selection.end
        }
        return getSelectionHandleCoordinates(
            textLayoutResult = layoutResult,
            offset = offset,
            isStart = isStartHandle,
            areHandlesCrossed = selection.reversed
        )
    }

    /**
     * Sets currently dragging handle state to [handle] and positions it at [position]. This is
     * mostly useful for updating the magnifier.
     *
     * @param handle A real or acting handle that specifies which one is being dragged.
     * @param position Where the handle currently is
     */
    fun updateHandleDragging(
        handle: Handle,
        position: Offset
    ) {
        draggingHandle = handle
        rawHandleDragPosition = position
    }

    /**
     * When a Selection or Cursor Handle starts being dragged, this function should be called
     * to mark the current visible offset, so that if content gets scrolled during the drag, we
     * can correctly offset the actual position where drag corresponds to.
     */
    private fun markStartContentVisibleOffset() {
        startTextLayoutPositionInWindow = currentTextLayoutPositionInWindow
    }

    /**
     * Call this function when a selection or cursor handle is stopped dragging.
     */
    fun clearHandleDragging() {
        draggingHandle = null
        rawHandleDragPosition = Offset.Unspecified
        startTextLayoutPositionInWindow = Offset.Unspecified
    }

    /**
     * Whether a copy operation can execute now and modify the clipboard.
     * The paste operation requires the selection to not be collapsed,
     * the text field to be editable, and for it to NOT be a password.
     */
    fun canCut(): Boolean =
        !textFieldState.visualText.selection.collapsed && editable && !isPassword

    /**
     * The method for cutting text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager].
     * The new text should be the text before the selection plus the text after the selection.
     * And the new cursor offset should be between the text before the selection, and the text
     * after the selection.
     */
    fun cut() {
        val text = textFieldState.visualText
        if (text.selection.collapsed) return

        clipboardManager?.setText(AnnotatedString(text.getSelectedText().toString()))

        textFieldState.deleteSelectedText()
    }

    /**
     * Whether a copy operation can execute now and modify the clipboard.
     * The copy operation requires the selection to not be collapsed,
     * and the text field to NOT be a password.
     */
    fun canCopy(): Boolean = !textFieldState.visualText.selection.collapsed && !isPassword

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
    fun copy(cancelSelection: Boolean = true) {
        val text = textFieldState.visualText
        if (text.selection.collapsed) return

        clipboardManager?.setText(AnnotatedString(text.getSelectedText().toString()))

        if (!cancelSelection) return

        textFieldState.collapseSelectionToMax()
    }

    /**
     * Whether a paste operation can execute now and have a meaningful effect.
     * The paste operation requires the text field to be editable,
     * and the clipboard manager to have content to paste.
     */
    fun canPaste(): Boolean {
        if (!editable) return false
        // if receive content is not configured, we expect at least a text item to be present
        if (clipboardManager?.hasText() == true) return true
        // if receive content is configured, hasClip should be enough to show the paste option
        return receiveContentConfiguration?.invoke() != null && clipboardManager?.getClip() != null
    }

    fun paste() {
        val receiveContentConfiguration = receiveContentConfiguration?.invoke()
            ?: return pasteAsPlainText()

        val clipEntry = clipboardManager?.getClip() ?: return pasteAsPlainText()
        val clipMetadata = clipEntry.clipMetadata

        val remaining = receiveContentConfiguration.receiveContentListener.onReceive(
            TransferableContent(
                clipEntry = clipEntry,
                source = TransferableContent.Source.Clipboard,
                clipMetadata = clipMetadata
            )
        )

        // TODO(halilibo): this is not 1-to-1 compatible with ClipboardManager.getText() which
        //  returns an AnnotatedString and supports copy-pasting AnnotatedStrings inside the app.
        remaining?.clipEntry?.readPlainText()?.let { clipboardText ->
            textFieldState.replaceSelectedText(
                clipboardText,
                undoBehavior = TextFieldEditUndoBehavior.NeverMerge
            )
        }
    }

    /**
     * The method for pasting text.
     *
     * Get the text from [ClipboardManager]. If it's null, return.
     * The new content should be the text before the selected text, plus the text from the
     * [ClipboardManager], and plus the text after the selected text.
     * Then the selection should collapse, and the new cursor offset should be at the end of the
     * newly added text.
     */
    private fun pasteAsPlainText() {
        val clipboardText = clipboardManager?.getText()?.text ?: return

        textFieldState.replaceSelectedText(
            clipboardText,
            undoBehavior = TextFieldEditUndoBehavior.NeverMerge
        )
    }

    /**
     * Whether a select all operation can execute now and have a meaningful effect.
     * The select all operation requires the selection to not already be selecting the
     * entire text field.
     */
    fun canSelectAll(): Boolean =
        textFieldState.visualText.selection.length != textFieldState.visualText.length

    /**
     * The method for selecting all text.
     *
     * Expands or creates the selection to cover the entire content of the text field.
     */
    fun selectAll() {
        textFieldState.selectAll()
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy, paste and cut method as callbacks when "copy", "cut" or "paste" is clicked.
     *
     * @param contentRect Rectangle region where the toolbar will be anchored.
     */
    private fun showTextToolbar(contentRect: Rect) {
        // TODO(halilibo): Add a new TextToolbar option "paste as plain text".
        textToolbar?.showMenu(
            rect = contentRect,
            onCopyRequested = menuItem(canCopy(), None) { copy() },
            onPasteRequested = menuItem(canPaste(), None) { paste() },
            onCutRequested = menuItem(canCut(), None) { cut() },
            onSelectAllRequested = menuItem(canSelectAll(), Selection) {
                selectAll()
            },
        )
    }

    private inline fun menuItem(
        enabled: Boolean,
        desiredState: TextToolbarState,
        crossinline operation: () -> Unit
    ): (() -> Unit)? = if (!enabled) null else {
        {
            operation()
            updateTextToolbarState(desiredState)
        }
    }

    fun deselect() {
        if (!textFieldState.visualText.selection.collapsed) {
            textFieldState.collapseSelectionToEnd()
        }

        showCursorHandle = false
        updateTextToolbarState(None)
    }

    private fun hideTextToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            textToolbar?.hide()
        }
    }

    /**
     * Update the text field's selection based on new offsets.
     *
     * @param textFieldCharSequence the current text editing state
     * @param startOffset the start offset to use
     * @param endOffset the end offset to use
     * @param isStartHandle whether the start or end handle is being updated
     * @param adjustment The selection adjustment to use
     * @param allowPreviousSelectionCollapsed Allow a collapsed selection to be passed to selection
     * adjustment. In most cases, a collapsed selection should be considered "no previous
     * selection" for selection adjustment. However, in some cases - like starting a selection in
     * end padding - a collapsed selection may be necessary context to avoid selection flickering.
     * @param isStartOfSelection Whether this is, for certain, the beginning of a selection.
     */
    private fun updateSelection(
        textFieldCharSequence: TextFieldCharSequence,
        startOffset: Int,
        endOffset: Int,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        allowPreviousSelectionCollapsed: Boolean = false,
        isStartOfSelection: Boolean = false
    ): TextRange {
        val newSelection = getTextFieldSelection(
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = textFieldCharSequence.selection.takeIf {
                !isStartOfSelection && (allowPreviousSelectionCollapsed || !it.collapsed)
            },
            isStartHandle = isStartHandle,
            adjustment = adjustment,
        )

        if (newSelection == textFieldCharSequence.selection) return newSelection

        val onlyChangeIsReversed =
            newSelection.reversed != textFieldCharSequence.selection.reversed &&
                newSelection.run { TextRange(end, start) } == textFieldCharSequence.selection

        // don't haptic if we are using a mouse or if we aren't moving the selection bounds
        if (isInTouchMode && !onlyChangeIsReversed) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        return newSelection
    }

    private fun getTextFieldSelection(
        rawStartOffset: Int,
        rawEndOffset: Int,
        previousSelection: TextRange?,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ): TextRange {
        val layoutResult = textLayoutState.layoutResult ?: return TextRange.Zero

        // When the previous selection is null, it's allowed to have collapsed selection on
        // TextField. So we can ignore the SelectionAdjustment.Character.
        if (previousSelection == null && adjustment == SelectionAdjustment.Character) {
            return TextRange(rawStartOffset, rawEndOffset)
        }

        val selectionLayout = getTextFieldSelectionLayout(
            layoutResult = layoutResult,
            rawStartHandleOffset = rawStartOffset,
            rawEndHandleOffset = rawEndOffset,
            rawPreviousHandleOffset = previousRawDragOffset,
            previousSelectionRange = previousSelection ?: TextRange.Zero,
            isStartOfSelection = previousSelection == null,
            isStartHandle = isStartHandle,
        )

        if (previousSelection != null &&
            !selectionLayout.shouldRecomputeSelection(previousSelectionLayout)
        ) {
            return previousSelection
        }

        val result = adjustment.adjust(selectionLayout).toTextRange()
        previousSelectionLayout = selectionLayout
        previousRawDragOffset = if (isStartHandle) rawStartOffset else rawEndOffset

        return result
    }
}

private fun TextRange.reverse() = TextRange(end, start)

/**
 * A state that indicates when to show TextToolbar.
 *
 * - [None] Do not show the TextToolbar at all.
 * - [Cursor] if selection is collapsed and all the other criteria are met, show the TextToolbar.
 * - [Selection] if selection is expanded and all the other criteria are met, show the TextToolbar.
 *
 * @see [TextFieldSelectionState.observeTextToolbarVisibility]
 */
internal enum class TextToolbarState {
    None,
    Cursor,
    Selection,
}

private const val DEBUG = false
private const val DEBUG_TAG = "TextFieldSelectionState"

private fun logDebug(text: () -> String) {
    if (DEBUG) {
        println("$DEBUG_TAG: ${text()}")
    }
}
