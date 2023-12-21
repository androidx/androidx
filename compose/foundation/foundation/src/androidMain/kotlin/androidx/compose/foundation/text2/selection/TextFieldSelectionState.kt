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
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.foundation.text.selection.getAdjustedCoordinates
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextOnlyMutationPolicy
import androidx.compose.foundation.text2.input.internal.TextLayoutState
import androidx.compose.foundation.text2.input.selectCharsIn
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldSelectionState(
    private val textFieldState: TextFieldState,
    private val textLayoutState: TextLayoutState,
    private val density: Density
) {
    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

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
     * The location where cursor handle dragging has started. [Offset.Unspecified] means there is no
     * active dragging.
     */
    var cursorDragStart by mutableStateOf(Offset.Unspecified)

    /**
     * Displacement of cursor handle compared to [cursorDragStart] while dragging.
     * [Offset.Unspecified] means there is no active dragging.
     */
    var cursorDragDelta by mutableStateOf(Offset.Unspecified)

    suspend fun observeTextChanges() {
        val derivedTextState = derivedStateOf(TextOnlyMutationPolicy) { textFieldState.text }
        snapshotFlow { derivedTextState.value }
            // first value needs to be dropped because it cannot be compared to a prior value
            .drop(1)
            .collect {
                showHandles = false
            }
    }

    /**
     * True if the position of the cursor is within a visible part of the window (i.e. not scrolled
     * out of view) and the handle should be drawn.
     */
    val cursorHandleVisible: Boolean by derivedStateOf {
        val existsCondition = showHandles && textFieldState.text.selectionInChars.collapsed
        if (!existsCondition) return@derivedStateOf false

        // either cursor is dragging or inside visible bounds.
        return@derivedStateOf cursorDragStart.isSpecified ||
            textLayoutState.innerTextFieldCoordinates
                ?.visibleBounds()
                // Visibility of cursor handle should only be decided by changes to showHandles and
                // innerTextFieldCoordinates. If we also react to position changes of cursor, cursor
                // handle may start flickering while moving and scrolling the text field.
                ?.containsInclusive(Snapshot.withoutReadObservation { cursorRect.bottomCenter })
            ?: false
    }

    suspend fun PointerInputScope.detectCursorHandleDragGestures() {
        // keep track of how visible bounds change while moving the cursor handle.
        var startContentVisibleOffset: Offset = Offset.Zero
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
            },
            onDragEnd = {
                // clear any dragging state
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
            },
            onDragCancel = {
                // another gesture consumed the pointer, or composable is disposed
                cursorDragStart = Offset.Unspecified
                cursorDragDelta = Offset.Unspecified
                startContentVisibleOffset = Offset.Zero
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
                textFieldState.edit {
                    selectCharsIn(newSelection)
                }
            }
        )
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
}