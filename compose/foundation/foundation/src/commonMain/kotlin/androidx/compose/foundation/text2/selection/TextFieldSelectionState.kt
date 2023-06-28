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
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.foundation.text.selection.getAdjustedCoordinates
import androidx.compose.foundation.text.selection.getSelectionHandleCoordinates
import androidx.compose.foundation.text.selection.isPrecisePointer
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.TextLayoutState
import androidx.compose.foundation.text2.input.selectAll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
    var editable: Boolean
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
     * The gesture detector state, to indicate whether to show the appropriate handles for current
     * selection or just the cursor.
     *
     * In the false state, no selection or cursor handle is shown, only the cursor is shown.
     * TextField is initially in this state. To enter this state, input anything from the
     * keyboard and modify the text.
     *
     * In the true state, either selection or cursor handle is shown according to current selection
     * state of the TextField.
     */
    var showHandles by mutableStateOf(false)

    /**
     * Whether cursor handle is currently being dragged.
     */
    private var isCursorDragging by mutableStateOf(false)

    /**
     * Request to show the text toolbar right now. This is not the final decider for showing the
     * toolbar. Please refer to [observeTextToolbarVisibility] docs.
     */
    private var textToolbarVisible by mutableStateOf(false)

    /**
     * True if the position of the cursor is within a visible part of the window (i.e. not scrolled
     * out of view) and the handle should be drawn.
     */
    val cursorHandleVisible: Boolean by derivedStateOf {
        val existsCondition = showHandles && textFieldState.text.selectionInChars.collapsed
        if (!existsCondition) return@derivedStateOf false

        // either cursor is dragging or inside visible bounds.
        return@derivedStateOf isCursorDragging ||
            textLayoutState.innerTextFieldCoordinates
                ?.visibleBounds()
                // Visibility of cursor handle should only be decided by changes to showHandles and
                // innerTextFieldCoordinates. If we also react to position changes of cursor, cursor
                // handle may start flickering while moving and scrolling the text field.
                ?.containsInclusive(Snapshot.withoutReadObservation { cursorRect.bottomCenter })
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

        val coercedCursorCenterX = textLayoutState.innerTextFieldCoordinates?.let { coordinates ->
            // don't let cursor go beyond the bounds of inner text field or cursor will be clipped.
            // but also make sure that empty Text Layout still draws a cursor.
            cursorCenterX
                // do not use coerceIn because it is not guaranteed that minimum value is smaller
                // than the maximum value.
                .coerceAtMost(coordinates.size.width - cursorWidth / 2)
                .coerceAtLeast(cursorWidth / 2)
        } ?: cursorCenterX

        Rect(
            left = coercedCursorCenterX - cursorWidth / 2,
            right = coercedCursorCenterX + cursorWidth / 2,
            top = cursorRect.top,
            bottom = cursorRect.bottom
        )
    }

    /**
     * Responsible for responding to tap events on TextField.
     */
    fun onTapTextField(offset: Offset) {
        if (textFieldState.text.isNotEmpty()) {
            showHandles = true
        }

        textToolbarVisible = false

        // find the cursor position
        val cursorIndex = textLayoutState.getOffsetForPosition(offset)
        // update the state
        if (cursorIndex >= 0) {
            editWithFilter {
                selectCharsIn(TextRange(cursorIndex))
            }
        }
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
                detectTapAndPress { textToolbarVisible = !textToolbarVisible }
            }
        }
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
            showHandles = false
            if (textToolbarVisible) {
                hideTextToolbar()
            }
        }
    }

    fun dispose() {
        if (textToolbarVisible) {
            hideTextToolbar()
        }

        textToolbar = null
        clipboardManager = null
        hapticFeedBack = null
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
                startContentVisibleOffset = textLayoutState.innerTextFieldCoordinates
                    ?.takeIf { textLayoutState.innerTextFieldCoordinates?.isAttached == true }
                    ?.visibleBounds()
                    ?.topLeft ?: Offset.Zero
                isInTouchMode = true
                isCursorDragging = true
            },
            onDragEnd = {
                // clear any dragging state
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
                isCursorDragging = false
            },
            onDragCancel = {
                // another gesture consumed the pointer, or composable is disposed
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
                isCursorDragging = false
            },
            onDrag = onDrag@{ change, dragAmount ->
                cursorDragDelta += dragAmount

                val currentContentVisibleOffset = textLayoutState.innerTextFieldCoordinates
                    ?.visibleBounds()
                    ?.takeIf { textLayoutState.innerTextFieldCoordinates?.isAttached == true }
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
                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                editWithFilter {
                    selectCharsIn(newSelection)
                }
            }
        )
    }

    private suspend fun observeTextChanges() {
        snapshotFlow { textFieldState.text }
            .distinctUntilChanged(TextFieldCharSequence::contentEquals)
            // first value needs to be dropped because it cannot be compared to a prior value
            .drop(1)
            .collect {
                showHandles = false
                textToolbarVisible = false
            }
    }

    /**
     * Manages the visibility of text toolbar according to current state and received events from
     * various sources.
     *
     * - Tapping the cursor handle toggles the visibility of the toolbar [textToolbarVisible].
     * - Dragging the cursor handle temporarily hides the toolbar [isCursorDragging].
     * - Tapping somewhere on the textfield, whether it causes a cursor position change or not,
     * fully hides the toolbar [textToolbarVisible].
     * - Scrolling the textfield temporarily hides the toolbar [getContentRect].
     * - When cursor leaves the visible bounds, text toolbar is temporarily hidden.
     */
    private suspend fun observeTextToolbarVisibility() {
        snapshotFlow {
            val toolbarVisibility = textToolbarVisible && // toolbar is requested
                !isCursorDragging && // not dragging the cursor handle
                isInTouchMode

            // final visibility decision is made by contentRect visibility.
            // if contentRect is not in visible bounds, just pass Rect.Zero to the observer so that
            // it hides the toolbar. If Rect is successfully passed to the observer, toolbar will
            // be shown.
            if (!toolbarVisibility) {
                Rect.Zero
            } else {
                // contentRect is calculated in root coordinates. VisibleBounds are in parent
                // coordinates. Convert visibleBounds to root before checking the overlap.
                val visibleBounds = textLayoutState.innerTextFieldCoordinates?.visibleBounds()
                if (visibleBounds != null) {
                    val visibleBoundsTopLeftInRoot = textLayoutState
                        .innerTextFieldCoordinates
                        ?.localToRoot(visibleBounds.topLeft)
                    val visibleBoundsInRoot = Rect(visibleBoundsTopLeftInRoot!!, visibleBounds.size)
                    getContentRect().takeIf { visibleBoundsInRoot.overlaps(it) } ?: Rect.Zero
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
            val topLeft = textLayoutState.innerTextFieldCoordinates?.localToRoot(
                cursorRect.topLeft
            ) ?: Offset.Zero
            return Rect(topLeft, cursorRect.size)
        }
        val startOffset =
            textLayoutState.innerTextFieldCoordinates?.localToRoot(getHandlePosition(true))
                ?: Offset.Zero
        val endOffset =
            textLayoutState.innerTextFieldCoordinates?.localToRoot(getHandlePosition(false))
                ?: Offset.Zero
        val startTop =
            textLayoutState.innerTextFieldCoordinates?.localToRoot(
                Offset(
                    0f,
                    textLayoutState.layoutResult?.getCursorRect(
                        text.selectionInChars.start
                    )?.top ?: 0f
                )
            )?.y ?: 0f
        val endTop =
            textLayoutState.innerTextFieldCoordinates?.localToRoot(
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
            bottom = max(startOffset.y, endOffset.y) + with(density) { 25.dp.roundToPx() }
        )
    }

    private fun getHandlePosition(isStartHandle: Boolean): Offset {
        val offset = if (isStartHandle) {
            textFieldState.text.selectionInChars.start
        } else {
            textFieldState.text.selectionInChars.end
        }
        return getSelectionHandleCoordinates(
            textLayoutResult = textLayoutState.layoutResult!!,
            offset = offset,
            isStart = isStartHandle,
            areHandlesCrossed = textFieldState.text.selectionInChars.reversed
        )
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
    private fun paste() {
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

        showHandles = false
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
                textToolbarVisible = false
            }
        } else null

        val selectAll: (() -> Unit)? = if (selection.length != textFieldState.text.length) {
            {
                editWithFilter { selectAll() }
            }
        } else null

        textToolbar?.showMenu(
            rect = contentRect,
            onPasteRequested = paste,
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
}