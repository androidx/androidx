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

package androidx.compose.foundation.text2.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.foundation.text.selection.getAdjustedCoordinates
import androidx.compose.foundation.text.selection.getSelectionHandleCoordinates
import androidx.compose.foundation.text.selection.getTextFieldSelection
import androidx.compose.foundation.text.selection.isPrecisePointer
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.getSelectedText
import androidx.compose.foundation.text2.input.internal.TextLayoutState
import androidx.compose.foundation.text2.input.selectAll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
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

@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldSelectionState(
    private val textFieldState: TextFieldState,
    private val textLayoutState: TextLayoutState,
    var textEditFilter: TextEditFilter?,
    var density: Density,
    var editable: Boolean,
    var isFocused: Boolean
) {
    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

    /**
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    var textToolbar: TextToolbar? = null

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    var clipboardManager: ClipboardManager? = null

    /**
     * Whether user is interacting with the UI in touch mode.
     */
    var isInTouchMode: Boolean by mutableStateOf(true)

    /**
     * Whether to show the cursor handle below cursor indicator when the TextField is focused.
     */
    private var showCursorHandle by mutableStateOf(false)

    /**
     * Request to show the text toolbar right now, anchored to the cursor handle. This is not the
     * final decider for showing the toolbar. Please refer to [observeTextToolbarVisibility] docs.
     */
    private var cursorHandleShowToolbar by mutableStateOf(false)

    /**
     * Which selection handle is currently being dragged.
     */
    private var draggingHandle by mutableStateOf<Handle?>(null)

    /**
     * Access helper for inner text field coordinates that checks attached state.
     */
    private val innerCoordinates: LayoutCoordinates?
        get() = textLayoutState.innerTextFieldCoordinates?.takeIf { it.isAttached }

    /**
     * State of the cursor handle that includes its visibility and position.
     */
    val cursorHandle by derivedStateOf {
        // For cursor handle to be visible, [showCursorHandle] must be true and the selection
        // must be collapsed.
        // Also, cursor handle should be in visible bounds of inner TextField. However, if cursor
        // is dragging and gets out of bounds, we cannot remove it from composition because that
        // would stop the drag gesture defined on it. Instead, we allow the handle to be visible
        // as long as it's being dragged.
        // Visible bounds calculation lags one frame behind to let auto-scrolling settle.
        val visible = showCursorHandle && textFieldState.text.selectionInChars.collapsed &&
            (draggingHandle == Handle.Cursor || cursorHandleInBounds)

        if (!visible) return@derivedStateOf TextFieldHandleState.Hidden

        // text direction is useless for cursor handle, any value is fine.
        TextFieldHandleState(
            visible = true,
            position = cursorRect.bottomCenter,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false
        )
    }

    /**
     * Whether currently cursor handle is in visible bounds. This derived state does not react to
     * selection changes immediately because every selection change is processed in layout phase
     * by auto-scroll behavior.
     */
    private val cursorHandleInBounds by derivedStateOf(policy = structuralEqualityPolicy()) {
        val position = Snapshot.withoutReadObservation { cursorRect.bottomCenter }

        innerCoordinates
            ?.visibleBounds()
            ?.containsInclusive(position)
            ?: false
    }

    /**
     * Where the cursor should be at any given time in InnerTextField coordinates.
     */
    val cursorRect: Rect by derivedStateOf {
        val layoutResult = textLayoutState.layoutResult ?: return@derivedStateOf Rect.Zero
        val value = textFieldState.text
        // layoutResult could be lagging one frame behind. In any case, make sure that we are not
        // querying an out-of-bounds index.
        val cursorRect = layoutResult.getCursorRect(
            value.selectionInChars.start.coerceIn(0, layoutResult.layoutInput.text.length)
        )

        val cursorWidth = with(density) { DefaultCursorThickness.toPx() }
        val cursorCenterX = if (layoutResult.layoutInput.layoutDirection == LayoutDirection.Ltr) {
            (cursorRect.left + cursorWidth / 2)
        } else {
            (cursorRect.right - cursorWidth / 2)
        }

        // don't let cursor go beyond the bounds of inner text field or cursor will be clipped.
        // but also make sure that empty Text Layout still draws a cursor.
        val coercedCursorCenterX = cursorCenterX
            // do not use coerceIn because it is not guaranteed that minimum value is smaller
            // than the maximum value.
            .coerceAtMost(layoutResult.size.width - cursorWidth / 2)
            .coerceAtLeast(cursorWidth / 2)

        Rect(
            left = coercedCursorCenterX - cursorWidth / 2,
            right = coercedCursorCenterX + cursorWidth / 2,
            top = cursorRect.top,
            bottom = cursorRect.bottom
        )
    }

    val startSelectionHandle by derivedStateOf {
        getSelectionHandleState(isStartHandle = true)
    }

    val endSelectionHandle by derivedStateOf {
        getSelectionHandleState(isStartHandle = false)
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
                    cursorHandleShowToolbar = !cursorHandleShowToolbar
                })
            }
        }
    }

    /**
     * Implements the complete set of gestures supported by the TextField area.
     */
    suspend fun PointerInputScope.textFieldGestures(
        requestFocus: () -> Unit,
        showKeyboard: () -> Unit
    ) {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTouchMode()
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTextFieldTapGestures(requestFocus, showKeyboard)
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                detectTextFieldLongPressAndAfterDrag(requestFocus)
            }
        }
    }

    /**
     * Gesture detector for dragging the selection handles to change the selection in TextField.
     */
    suspend fun PointerInputScope.detectSelectionHandleDrag(isStartHandle: Boolean) {
        // keep track of how visible bounds change while moving the selection handle.
        var startContentVisibleOffset: Offset = Offset.Zero

        var dragBeginPosition: Offset = Offset.Unspecified
        var dragTotalDistance: Offset = Offset.Unspecified
        var previousDragOffset = -1
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

        // b/288931376: detectDragGestures do not call onDragCancel when composable is disposed.
        try {
            detectDragGestures(
                onDragStart = {
                    draggingHandle = handle
                    // The position of the character where the drag gesture should begin. This is in
                    // the composable coordinates.
                    dragBeginPosition = getAdjustedCoordinates(getHandlePosition(isStartHandle))

                    startContentVisibleOffset = innerCoordinates
                        ?.visibleBounds()
                        ?.topLeft ?: Offset.Zero

                    // Zero out the total distance that being dragged.
                    dragTotalDistance = Offset.Zero
                    previousDragOffset = if (isStartHandle) {
                        textFieldState.text.selectionInChars.start
                    } else {
                        textFieldState.text.selectionInChars.end
                    }
                },
                onDragEnd = {
                    draggingHandle = null
                    dragBeginPosition = Offset.Unspecified
                    dragTotalDistance = Offset.Zero
                    startContentVisibleOffset = Offset.Zero
                },
                onDragCancel = {
                    draggingHandle = null
                    dragBeginPosition = Offset.Unspecified
                    dragTotalDistance = Offset.Zero
                    startContentVisibleOffset = Offset.Zero
                },
                onDrag = onDrag@{ _, delta ->
                    draggingHandle = handle
                    dragTotalDistance += delta
                    val layoutResult = textLayoutState.layoutResult ?: return@onDrag

                    val currentContentVisibleOffset = innerCoordinates
                        ?.visibleBounds()
                        ?.topLeft ?: startContentVisibleOffset

                    // "start position + total delta" is not enough to understand the current
                    // pointer position relative to text layout. We need to also account for any
                    // changes to visible offset that's caused by auto-scrolling while dragging.
                    val currentDragPosition = dragBeginPosition + dragTotalDistance +
                        (currentContentVisibleOffset - startContentVisibleOffset)

                    val startOffset = if (isStartHandle) {
                        layoutResult.getOffsetForPosition(currentDragPosition)
                    } else {
                        textFieldState.text.selectionInChars.start
                    }

                    val endOffset = if (isStartHandle) {
                        textFieldState.text.selectionInChars.end
                    } else {
                        layoutResult.getOffsetForPosition(currentDragPosition)
                    }

                    val prevSelection = textFieldState.text.selectionInChars
                    var newSelection = updateSelection(
                        textFieldCharSequence = textFieldState.text,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        isStartHandle = isStartHandle,
                        previousHandleOffset = previousDragOffset,
                        adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                    )
                    // selection drag should never reverse the selection. It can only collapse the
                    // selection as the handles are about to pass each other.
                    if (newSelection.reversed != prevSelection.reversed) {
                        // the common offset should be the one that is not being dragged
                        val offset = if (isStartHandle) prevSelection.end else prevSelection.start
                        newSelection = TextRange(offset)
                    }
                    editWithFilter {
                        selectCharsIn(newSelection)
                    }
                    previousDragOffset = if (isStartHandle) startOffset else endOffset
                }
            )
        } finally {
            logDebug {
                "Selection Handle drag cancelled for " +
                    "draggingHandle: $draggingHandle definedOn: $handle"
            }
            if (draggingHandle == handle) draggingHandle = null
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
            if (cursorHandleShowToolbar) {
                hideTextToolbar()
            }
        }
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
    private suspend fun PointerInputScope.detectTouchMode() {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                isInTouchMode = !event.isPrecisePointer
            }
        }
    }

    private suspend fun PointerInputScope.detectTextFieldTapGestures(
        requestFocus: () -> Unit,
        showKeyboard: () -> Unit
    ) {
        detectTapAndDoubleTap(
            onTap = { offset ->
                logDebug { "onTapTextField" }
                requestFocus()

                if (editable && isFocused) {
                    showKeyboard()
                    if (textFieldState.text.isNotEmpty()) {
                        showCursorHandle = true
                    }

                    cursorHandleShowToolbar = false

                    // find the cursor position
                    val cursorIndex = textLayoutState.getOffsetForPosition(offset)
                    // update the state
                    if (cursorIndex >= 0) {
                        editWithFilter {
                            selectCharsIn(TextRange(cursorIndex))
                        }
                    }
                }
            },
            onDoubleTap = { offset ->
                logDebug { "onDoubleTapTextField" }
                // onTap is already called at this point. Focus is requested.

                showCursorHandle = false
                cursorHandleShowToolbar = false

                val index = textLayoutState.getOffsetForPosition(offset)
                val newSelection = updateSelection(
                    // reset selection, otherwise a previous selection may be used
                    // as context for creating the next selection
                    textFieldCharSequence = TextFieldCharSequence(
                        textFieldState.text,
                        TextRange.Zero
                    ),
                    startOffset = index,
                    endOffset = index,
                    isStartHandle = false,
                    previousHandleOffset = -1, // there is no previous drag.
                    adjustment = SelectionAdjustment.Word,
                )
                editWithFilter {
                    selectCharsIn(newSelection)
                }
            }
        )
    }

    private suspend fun PointerInputScope.detectCursorHandleDragGestures() {
        // keep track of how visible bounds change while moving the cursor handle.
        var startContentVisibleOffset: Offset = Offset.Zero

        var cursorDragStart = Offset.Unspecified
        var cursorDragDelta = Offset.Unspecified

        detectDragGestures(
            onDragStart = {
                // mark start drag point
                cursorDragStart = getAdjustedCoordinates(cursorRect.bottomCenter)
                cursorDragDelta = Offset.Zero
                startContentVisibleOffset = innerCoordinates
                    ?.visibleBounds()
                    ?.topLeft ?: Offset.Zero
                isInTouchMode = true
                draggingHandle = Handle.Cursor
            },
            onDragEnd = {
                // clear any dragging state
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
                draggingHandle = null
            },
            onDragCancel = {
                // another gesture consumed the pointer, or composable is disposed
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
                draggingHandle = null
            },
            onDrag = onDrag@{ change, dragAmount ->
                cursorDragDelta += dragAmount

                val currentContentVisibleOffset = innerCoordinates
                    ?.visibleBounds()
                    ?.topLeft ?: startContentVisibleOffset

                // "start position + total delta" is not enough to understand the current pointer
                // position relative to text layout. We need to also account for any changes to
                // visible offset that's caused by auto-scrolling while dragging.
                val currentDragPosition = cursorDragStart + cursorDragDelta +
                    (currentContentVisibleOffset - startContentVisibleOffset)

                val layoutResult = textLayoutState.layoutResult ?: return@onDrag
                val offset = layoutResult.getOffsetForPosition(currentDragPosition)

                val newSelection = TextRange(offset)

                // Nothing changed, skip onValueChange hand hapticFeedback.
                if (newSelection == textFieldState.text.selectionInChars) return@onDrag

                change.consume()
                // TODO: only perform haptic feedback if filter does not override the change
                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                editWithFilter {
                    selectCharsIn(newSelection)
                }
            }
        )
    }

    private suspend fun PointerInputScope.detectTextFieldLongPressAndAfterDrag(
        requestFocus: () -> Unit
    ) {
        detectDragGesturesAfterLongPress(
            onDragStart = onDragStart@{ dragStartOffset ->
                logDebug { "onDragStart after longPress" }
                // at the beginning of selection disable toolbar, re-evaluate visibility after
                // drag gesture is finished
                cursorHandleShowToolbar = false
                requestFocus()

                // Long Press at the blank area, the cursor should show up at the end of the line.
                if (!textLayoutState.isPositionOnText(dragStartOffset)) {
                    val offset = textLayoutState.getOffsetForPosition(dragStartOffset)

                    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    editWithFilter {
                        selectCharsIn(TextRange(offset))
                    }
                    showCursorHandle = true
                } else {
                    if (textFieldState.text.isEmpty()) return@onDragStart
                    val offset = textLayoutState.getOffsetForPosition(dragStartOffset)
                    val newSelection = updateSelection(
                        // reset selection, otherwise a previous selection may be used
                        // as context for creating the next selection
                        textFieldCharSequence = TextFieldCharSequence(
                            textFieldState.text,
                            TextRange.Zero
                        ),
                        startOffset = offset,
                        endOffset = offset,
                        isStartHandle = false,
                        previousHandleOffset = -1, // there is no previous drag.
                        adjustment = SelectionAdjustment.CharacterWithWordAccelerate,
                    )
                    editWithFilter {
                        selectCharsIn(newSelection)
                    }
                    showCursorHandle = false
                }
            },
            onDragEnd = {},
            onDragCancel = {},
            onDrag = onDrag@{ _, _ -> }
        )
    }

    private suspend fun observeTextChanges() {
        snapshotFlow { textFieldState.text }
            .distinctUntilChanged(TextFieldCharSequence::contentEquals)
            // first value needs to be dropped because it cannot be compared to a prior value
            .drop(1)
            .collect {
                showCursorHandle = false
                cursorHandleShowToolbar = false
            }
    }

    /**
     * Manages the visibility of text toolbar according to current state and received events from
     * various sources.
     *
     * - Tapping the cursor handle toggles the visibility of the toolbar [cursorHandleShowToolbar].
     * - Dragging the cursor handle or selection handles temporarily hides the toolbar
     * [draggingHandle].
     * - Tapping somewhere on the textfield, whether it causes a cursor position change or not,
     * fully hides the toolbar [cursorHandleShowToolbar].
     * - Scrolling the textfield temporarily hides the toolbar [getContentRect].
     * - When cursor leaves the visible bounds, text toolbar is temporarily hidden.
     */
    private suspend fun observeTextToolbarVisibility() {
        snapshotFlow {
            val isCollapsed = textFieldState.text.selectionInChars.collapsed
            val toolbarVisibility =
                // either toolbar is requested specifically or selection is active
                (cursorHandleShowToolbar || !isCollapsed) &&
                    draggingHandle == null && // not dragging any selection handles
                    isInTouchMode

            // final visibility decision is made by contentRect visibility.
            // if contentRect is not in visible bounds, just pass Rect.Zero to the observer so that
            // it hides the toolbar. If Rect is successfully passed to the observer, toolbar will
            // be displayed.
            if (!toolbarVisibility) {
                Rect.Zero
            } else {
                // contentRect is calculated in root coordinates. VisibleBounds are in parent
                // coordinates. Convert visibleBounds to root before checking the overlap.
                val visibleBounds = innerCoordinates?.visibleBounds()
                if (visibleBounds != null) {
                    val visibleBoundsTopLeftInRoot =
                        innerCoordinates?.localToRoot(visibleBounds.topLeft)
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
        val text = textFieldState.text
        // TODO(halilibo): better stale layout result check
        // this is basically testing whether current layoutResult was created for the current
        // text in TextFieldState. This is a temporary check that should be improved.
        if (textLayoutState.layoutResult?.layoutInput?.text?.length != text.length) {
            return Rect.Zero
        }
        // accept cursor position as content rect when selection is collapsed
        // contentRect is defined in innerTextField coordinates, so it needs to be realigned to
        // root container.
        if (text.selectionInChars.collapsed) {
            val topLeft = innerCoordinates?.localToRoot(cursorRect.topLeft) ?: Offset.Zero
            return Rect(topLeft, cursorRect.size)
        }
        val startOffset =
            innerCoordinates?.localToRoot(getHandlePosition(true)) ?: Offset.Zero
        val endOffset =
            innerCoordinates?.localToRoot(getHandlePosition(false)) ?: Offset.Zero
        val startTop =
            innerCoordinates?.localToRoot(
                Offset(
                    0f,
                    textLayoutState.layoutResult?.getCursorRect(
                        text.selectionInChars.start
                    )?.top ?: 0f
                )
            )?.y ?: 0f
        val endTop =
            innerCoordinates?.localToRoot(
                Offset(
                    0f,
                    textLayoutState.layoutResult?.getCursorRect(
                        text.selectionInChars.end
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

    private fun getSelectionHandleState(isStartHandle: Boolean): TextFieldHandleState {
        val handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd

        val layoutResult = textLayoutState.layoutResult ?: return TextFieldHandleState.Hidden

        val selection = textFieldState.text.selectionInChars

        if (selection.collapsed) return TextFieldHandleState.Hidden

        val position = getHandlePosition(isStartHandle)

        val visible = draggingHandle == handle ||
            (innerCoordinates
                ?.visibleBounds()
                ?.containsInclusive(position)
                ?: false)

        if (!visible) return TextFieldHandleState.Hidden

        val directionOffset = if (isStartHandle) selection.start else max(selection.end - 1, 0)
        val direction = layoutResult.getBidiRunDirection(directionOffset)
        val handlesCrossed = selection.reversed
        return TextFieldHandleState(true, position, direction, handlesCrossed)
    }

    private fun getHandlePosition(isStartHandle: Boolean): Offset {
        val layoutResult = textLayoutState.layoutResult ?: return Offset.Zero
        val selection = textFieldState.text.selectionInChars
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
     * The method for cutting text.
     *
     * If there is no selection, return.
     * Put the selected text into the [ClipboardManager].
     * The new text should be the text before the selection plus the text after the selection.
     * And the new cursor offset should be between the text before the selection, and the text
     * after the selection.
     */
    internal fun cut() {
        val text = textFieldState.text
        if (text.selectionInChars.collapsed) return

        clipboardManager?.setText(AnnotatedString(text.getSelectedText().toString()))

        editWithFilter {
            replace(selectionInChars.min, selectionInChars.max, "")
            selectCharsIn(TextRange(selectionInChars.min))
        }
        // TODO(halilibo): undoManager force snapshot
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
    fun copy(cancelSelection: Boolean = true) {
        val text = textFieldState.text
        if (text.selectionInChars.collapsed) return

        clipboardManager?.setText(AnnotatedString(text.getSelectedText().toString()))

        if (!cancelSelection) return

        editWithFilter {
            selectCharsIn(TextRange(selectionInChars.max))
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
    fun paste() {
        val clipboardText = clipboardManager?.getText()?.text ?: return

        editWithFilter {
            val selection = textFieldState.text.selectionInChars
            replace(
                selection.min,
                selection.max,
                clipboardText
            )
            selectCharsIn(TextRange(selection.min + clipboardText.length))
        }
        // TODO(halilibo): undoManager force snapshot
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy, paste and cut method as callbacks when "copy", "cut" or "paste" is clicked.
     *
     * @param contentRect Rectangle region where the toolbar will be anchored.
     */
    private fun showTextToolbar(contentRect: Rect) {
        val selection = textFieldState.text.selectionInChars

        val paste: (() -> Unit)? = if (editable && clipboardManager?.hasText() == true) {
            {
                paste()
                cursorHandleShowToolbar = false
            }
        } else null

        val copy: (() -> Unit)? = if (!selection.collapsed) {
            {
                copy()
                cursorHandleShowToolbar = false
            }
        } else null

        val cut: (() -> Unit)? = if (!selection.collapsed && editable) {
            {
                cut()
                cursorHandleShowToolbar = false
            }
        } else null

        val selectAll: (() -> Unit)? = if (selection.length != textFieldState.text.length) {
            {
                editWithFilter { selectAll() }
                cursorHandleShowToolbar = false
            }
        } else null

        textToolbar?.showMenu(
            rect = contentRect,
            onCopyRequested = copy,
            onPasteRequested = paste,
            onCutRequested = cut,
            onSelectAllRequested = selectAll
        )
    }

    /**
     * Edits the TextFieldState content with a filter applied if available.
     */
    private fun editWithFilter(block: TextFieldBuffer.() -> Unit) {
        val filter = textEditFilter
        if (filter == null) {
            textFieldState.edit(block)
        } else {
            val originalValue = textFieldState.text
            // create a new buffer to pass to TextEditFilter after edit ops
            val buffer = TextFieldBuffer(originalValue)
            buffer.block()

            // finally filter the buffer's current status
            textEditFilter?.filter(originalValue, buffer)

            // reset the TextFieldState with the buffer's final value
            val newValue = buffer.toTextFieldCharSequence(originalValue.compositionInChars)
            textFieldState.editProcessor.reset(newValue)
        }
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
     * @param isStartHandle whether the start handle is being updated
     * @param adjustment The selection adjustment to use
     * @param allowPreviousSelectionCollapsed Allow a collapsed selection to be passed to selection
     * adjustment. In most cases, a collapsed selection should be considered "no previous
     * selection" for selection adjustment. However, in some cases - like starting a selection in
     * end padding - a collapsed selection may be necessary context to avoid selection flickering.
     */
    private fun updateSelection(
        textFieldCharSequence: TextFieldCharSequence,
        startOffset: Int,
        endOffset: Int,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        previousHandleOffset: Int,
        allowPreviousSelectionCollapsed: Boolean = false,
    ): TextRange {
        val newSelection = getTextFieldSelection(
            textLayoutResult = textLayoutState.layoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousHandleOffset = previousHandleOffset,
            previousSelection = textFieldCharSequence.selectionInChars
                .takeIf { allowPreviousSelectionCollapsed || !it.collapsed },
            isStartHandle = isStartHandle,
            adjustment = adjustment,
        )

        if (newSelection == textFieldCharSequence.selectionInChars) return newSelection

        val onlyChangeIsReversed =
            newSelection.reversed != textFieldCharSequence.selectionInChars.reversed &&
                newSelection.run { TextRange(end, start) } == textFieldCharSequence.selectionInChars

        // don't haptic if we are using a mouse or if we aren't moving the selection bounds
        if (isInTouchMode && !onlyChangeIsReversed) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        return newSelection
    }
}

private val DEBUG = true
private val DEBUG_TAG = "TextFieldSelectionState"

private fun logDebug(text: () -> String) {
    if (DEBUG) {
        println("$DEBUG_TAG: ${text()}")
    }
}
