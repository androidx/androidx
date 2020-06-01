/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.compose.State
import androidx.compose.StructurallyEqual
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.LongPressDragObserver
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.hapticfeedback.HapticFeedbackType
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.core.texttoolbar.TextToolbarStatus
import androidx.ui.geometry.Rect
import androidx.ui.text.AnnotatedString
import androidx.ui.text.length
import androidx.ui.text.subSequence
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import kotlin.math.max
import kotlin.math.min

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
internal class SelectionManager(private val selectionRegistrar: SelectionRegistrarImpl) {
    /**
     * The current selection.
     */
    var selection: Selection? = null
        set(value) {
            field = value
            updateHandleOffsets()
            hideSelectionToolbar()
        }

    /**
     * The manager will invoke this every time it comes to the conclusion that the selection should
     * change. The expectation is that this callback will end up causing `setSelection` to get
     * called. This is what makes this a "controlled component".
     */
    var onSelectionChange: (Selection?) -> Unit = {}

    /**
     * [HapticFeedback] handle to perform haptic feedback.
     */
    var hapticFeedBack: HapticFeedback? = null

    /**
     * [ClipboardManager] to perform clipboard features.
     */
    var clipboardManager: ClipboardManager? = null

    /**
     * [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M).
     */
    var textToolbar: TextToolbar? = null

    /**
     * Layout Coordinates of the selection container.
     */
    var containerLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updateHandleOffsets()
            updateSelectionToolbarPosition()
        }

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = PxPosition.Origin

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = PxPosition.Origin

    /**
     * A flag to check if the selection start or end handle is being dragged.
     * If this value is true, then onPress will not select any text.
     * This value will be set to true when either handle is being dragged, and be reset to false
     * when the dragging is stopped.
     */
    private var draggingHandle = false

    /**
     * The calculated position of the start handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var startHandlePosition by mutableStateOf<PxPosition?>(null, areEquivalent = StructurallyEqual)
        private set

    /**
     * The calculated position of the end handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var endHandlePosition by mutableStateOf<PxPosition?>(null, areEquivalent = StructurallyEqual)
        private set

    init {
        selectionRegistrar.onPositionChangeCallback = {
            updateHandleOffsets()
            hideSelectionToolbar()
        }
    }

    private fun updateHandleOffsets() {
        val selection = selection
        val containerCoordinates = containerLayoutCoordinates
        if (selection != null && containerCoordinates != null && containerCoordinates.isAttached) {
            val startLayoutCoordinates = selection.start.selectable.getLayoutCoordinates()
            val endLayoutCoordinates = selection.end.selectable.getLayoutCoordinates()

            if (startLayoutCoordinates != null && endLayoutCoordinates != null) {
                startHandlePosition = containerCoordinates.childToLocal(
                    startLayoutCoordinates,
                    selection.start.selectable.getHandlePosition(
                        selection = selection,
                        isStartHandle = true
                    )
                )
                endHandlePosition = containerCoordinates.childToLocal(
                    endLayoutCoordinates,
                    selection.end.selectable.getHandlePosition(
                        selection = selection,
                        isStartHandle = false
                    )
                )
                return
            }
        }
        startHandlePosition = null
        endHandlePosition = null
    }

    /**
     * Returns non-nullable [containerLayoutCoordinates].
     */
    internal fun requireContainerCoordinates(): LayoutCoordinates {
        val coordinates = containerLayoutCoordinates
        require(coordinates != null)
        require(coordinates.isAttached)
        return coordinates
    }

    /**
     * Iterates over the handlers, gets the selection for each Composable, and merges all the
     * returned [Selection]s.
     *
     * @param startPosition [PxPosition] for the start of the selection
     * @param endPosition [PxPosition] for the end of the selection
     * @param longPress the selection is a result of long press
     * @param previousSelection previous selection
     *
     * @return [Selection] object which is constructed by combining all Composables that are
     * selected.
     */
    // This function is internal for testing purposes.
    internal fun mergeSelections(
        startPosition: PxPosition,
        endPosition: PxPosition,
        longPress: Boolean = false,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Selection? {

        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fold(null) { mergedSelection: Selection?,
                          handler: Selectable ->
                merge(
                    mergedSelection,
                    handler.getSelection(
                        startPosition = startPosition,
                        endPosition = endPosition,
                        containerLayoutCoordinates = requireContainerCoordinates(),
                        longPress = longPress,
                        previousSelection = previousSelection,
                        isStartHandle = isStartHandle
                    )
                )
            }
        if (previousSelection != newSelection) hapticFeedBack?.performHapticFeedback(
            HapticFeedbackType.TextHandleMove
        )
        return newSelection
    }

    internal fun getSelectedText(): AnnotatedString? {
        val selectables = selectionRegistrar.sort(requireContainerCoordinates())
        var selectedText: AnnotatedString? = null

        selection?.let {
            for (handler in selectables) {
                // Continue if the current selectable is before the selection starts.
                if (handler != it.start.selectable && handler != it.end.selectable &&
                    selectedText == null
                ) continue

                val currentSelectedText = getCurrentSelectedText(
                    selectable = handler,
                    selection = it
                )
                selectedText = selectedText?.plus(currentSelectedText) ?: currentSelectedText

                // Break if the current selectable is the last selected selectable.
                if (handler == it.end.selectable && !it.handlesCrossed ||
                    handler == it.start.selectable && it.handlesCrossed
                ) break
            }
        }
        return selectedText
    }

    internal fun copy() {
        val selectedText = getSelectedText()
        selectedText?.let { clipboardManager?.setText(it) }
    }

    /**
     * This function get the selected region as a Rectangle region, and pass it to [TextToolbar]
     * to make the FloatingToolbar show up in the proper place. In addition, this function passes
     * the copy method as a callback when "copy" is clicked.
     */
    internal fun showSelectionToolbar() {
        selection?.let {
            textToolbar?.showCopyMenu(
                getContentRect(),
                onCopyRequested = { copy() },
                onDeselectRequested = { onRelease() }
            )
        }
    }

    private fun hideSelectionToolbar() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            val selection = selection
            if (selection == null) {
                textToolbar?.hide()
            }
        }
    }

    private fun updateSelectionToolbarPosition() {
        if (textToolbar?.status == TextToolbarStatus.Shown) {
            showSelectionToolbar()
        }
    }

    /**
     * Calculate selected region as [Rect]. The top is the top of the first selected
     * line, and the bottom is the bottom of the last selected line. The left is the leftmost
     * handle's horizontal coordinates, and the right is the rightmost handle's coordinates.
     */
    private fun getContentRect(): Rect {
        val selection = selection ?: return Rect.zero
        val startLayoutCoordinates =
            selection.start.selectable.getLayoutCoordinates() ?: return Rect.zero
        val endLayoutCoordinates =
            selection.end.selectable.getLayoutCoordinates() ?: return Rect.zero

        val localLayoutCoordinates = containerLayoutCoordinates
        if (localLayoutCoordinates != null && localLayoutCoordinates.isAttached) {
            var startOffset = localLayoutCoordinates.childToLocal(
                startLayoutCoordinates,
                selection.start.selectable.getHandlePosition(
                    selection = selection,
                    isStartHandle = true
                )
            )
            var endOffset = localLayoutCoordinates.childToLocal(
                endLayoutCoordinates,
                selection.end.selectable.getHandlePosition(
                    selection = selection,
                    isStartHandle = false
                )
            )

            startOffset = localLayoutCoordinates.localToRoot(startOffset)
            endOffset = localLayoutCoordinates.localToRoot(endOffset)

            val left = min(startOffset.x, endOffset.x)
            val right = max(startOffset.x, endOffset.x)

            var startTop = localLayoutCoordinates.childToLocal(
                startLayoutCoordinates,
                PxPosition(
                    0f,
                    selection.start.selectable.getBoundingBox(selection.start.offset).top
                )
            )

            var endTop = localLayoutCoordinates.childToLocal(
                endLayoutCoordinates,
                PxPosition(
                    0.0f,
                    selection.end.selectable.getBoundingBox(selection.end.offset).top
                )
            )

            startTop = localLayoutCoordinates.localToRoot(startTop)
            endTop = localLayoutCoordinates.localToRoot(endTop)

            val top = min(startTop.y, endTop.y)
            val bottom = max(startOffset.y, endOffset.y) + (HANDLE_HEIGHT.value * 4.0).px.value

            return Rect(
                left,
                top,
                right,
                bottom
            )
        }
        return Rect.zero
    }

    // This is for PressGestureDetector to cancel the selection.
    fun onRelease() {
        // Call mergeSelections with an out of boundary input to inform all text widgets to
        // cancel their individual selection.
        mergeSelections(
            startPosition = PxPosition(-1f, -1f),
            endPosition = PxPosition(-1f, -1f),
            previousSelection = selection
        )
        if (selection != null) onSelectionChange(null)
    }

    val longPressDragObserver = object : LongPressDragObserver {
        override fun onLongPress(pxPosition: PxPosition) {
            if (draggingHandle) return
            val coordinates = containerLayoutCoordinates
            if (coordinates == null || !coordinates.isAttached) return
            val newSelection = mergeSelections(
                startPosition = pxPosition,
                endPosition = pxPosition,
                longPress = true,
                previousSelection = selection
            )
            if (newSelection != selection) onSelectionChange(newSelection)
            dragBeginPosition = pxPosition
        }

        override fun onDragStart() {
            super.onDragStart()
            // selection never started
            if (selection == null) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = PxPosition.Origin
        }

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            // selection never started, did not consume any drag
            if (selection == null) return PxPosition.Origin

            dragTotalDistance += dragDistance
            val newSelection = mergeSelections(
                startPosition = dragBeginPosition,
                endPosition = dragBeginPosition + dragTotalDistance,
                longPress = true,
                previousSelection = selection
            )

            if (newSelection != selection) onSelectionChange(newSelection)
            return dragDistance
        }
    }

    /**
     * Adjust coordinates for given text offset.
     *
     * Currently [android.text.Layout.getLineBottom] returns y coordinates of the next
     * line's top offset, which is not included in current line's hit area. To be able to
     * hit current line, move up this y coordinates by 1 pixel.
     */
    private fun getAdjustedCoordinates(position: PxPosition): PxPosition {
        return PxPosition(position.x, position.y - 1f)
    }

    fun handleDragObserver(isStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart(downPosition: PxPosition) {
                val selection = selection!!
                // The LayoutCoordinates of the composable where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // composable coordinates to selection container coordinates.
                val beginLayoutCoordinates = if (isStartHandle) {
                    selection.start.selectable.getLayoutCoordinates()!!
                } else {
                    selection.end.selectable.getLayoutCoordinates()!!
                }

                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                val beginCoordinates = getAdjustedCoordinates(
                    if (isStartHandle)
                        selection.start.selectable.getHandlePosition(
                            selection = selection, isStartHandle = true
                        ) else
                        selection.end.selectable.getHandlePosition(
                            selection = selection, isStartHandle = false
                        )
                )

                // Convert the position where drag gesture begins from composable coordinates to
                // selection container coordinates.
                dragBeginPosition = requireContainerCoordinates().childToLocal(
                    beginLayoutCoordinates,
                    beginCoordinates
                )

                // Zero out the total distance that being dragged.
                dragTotalDistance = PxPosition.Origin
                draggingHandle = true
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val selection = selection!!
                dragTotalDistance += dragDistance

                val currentStart = if (isStartHandle) {
                    dragBeginPosition + dragTotalDistance
                } else {
                    requireContainerCoordinates().childToLocal(
                        selection.start.selectable.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            selection.start.selectable.getHandlePosition(
                                selection = selection,
                                isStartHandle = true
                            )
                        )
                    )
                }

                val currentEnd = if (isStartHandle) {
                    requireContainerCoordinates().childToLocal(
                        selection.end.selectable.getLayoutCoordinates()!!,
                        getAdjustedCoordinates(
                            selection.end.selectable.getHandlePosition(
                                selection = selection,
                                isStartHandle = false
                            )
                        )
                    )
                } else {
                    dragBeginPosition + dragTotalDistance
                }

                val finalSelection = mergeSelections(
                    startPosition = currentStart,
                    endPosition = currentEnd,
                    previousSelection = selection,
                    isStartHandle = isStartHandle
                )
                onSelectionChange(finalSelection)
                return dragDistance
            }

            override fun onStop(velocity: PxPosition) {
                super.onStop(velocity)
                draggingHandle = false
            }
        }
    }
}

private fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}

private fun getCurrentSelectedText(
    selectable: Selectable,
    selection: Selection
): AnnotatedString {
    val currentText = selectable.getText()

    return if (
        selectable != selection.start.selectable &&
        selectable != selection.end.selectable
    ) {
        // Select the full text content if the current selectable is between the
        // start and the end selectables.
        currentText
    } else if (
        selectable == selection.start.selectable &&
        selectable == selection.end.selectable
    ) {
        // Select partial text content if the current selectable is the start and
        // the end selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(selection.end.offset, selection.start.offset)
        } else {
            currentText.subSequence(selection.start.offset, selection.end.offset)
        }
    } else if (selectable == selection.start.selectable) {
        // Select partial text content if the current selectable is the start
        // selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(0, selection.start.offset)
        } else {
            currentText.subSequence(selection.start.offset, currentText.length)
        }
    } else {
        // Selectable partial text content if the current selectable is the end
        // selectable.
        if (selection.handlesCrossed) {
            currentText.subSequence(selection.end.offset, currentText.length)
        } else {
            currentText.subSequence(0, selection.end.offset)
        }
    }
}
