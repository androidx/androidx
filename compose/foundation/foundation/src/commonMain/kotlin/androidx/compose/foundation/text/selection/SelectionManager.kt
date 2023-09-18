/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.selection.Selection.AnchorInfo
import androidx.compose.foundation.text2.input.internal.coerceIn
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.absoluteValue

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
internal class SelectionManager(private val selectionRegistrar: SelectionRegistrarImpl) {

    private val _selection: MutableState<Selection?> = mutableStateOf(null)

    /**
     * The current selection.
     */
    var selection: Selection?
        get() = _selection.value
        set(value) {
            _selection.value = value
            if (value != null) {
                updateHandleOffsets()
            }
        }

    /**
     * Is touch mode active
     */
    private val _isInTouchMode = mutableStateOf(true)
    var isInTouchMode: Boolean
        get() = _isInTouchMode.value
        set(value) {
            if (_isInTouchMode.value != value) {
                _isInTouchMode.value = value
                updateSelectionToolbar()
            }
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
     * Focus requester used to request focus when selection becomes active.
     */
    var focusRequester: FocusRequester = FocusRequester()

    /**
     * Return true if the corresponding SelectionContainer is focused.
     */
    var hasFocus: Boolean by mutableStateOf(false)

    /**
     * Modifier for selection container.
     */
    val modifier
        get() = Modifier
            .onClearSelectionRequested { onRelease() }
            .onGloballyPositioned { containerLayoutCoordinates = it }
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && hasFocus) {
                    onRelease()
                }
                hasFocus = focusState.isFocused
            }
            .focusable()
            .updateSelectionTouchMode { isInTouchMode = it }
            .onKeyEvent {
                if (isCopyKeyEvent(it)) {
                    copy()
                    true
                } else {
                    false
                }
            }
            .then(if (shouldShowMagnifier) Modifier.selectionMagnifier(this) else Modifier)

    private var previousPosition: Offset? = null

    /**
     * Layout Coordinates of the selection container.
     */
    var containerLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            if (hasFocus && selection != null) {
                val positionInWindow = value?.positionInWindow()
                if (previousPosition != positionInWindow) {
                    previousPosition = positionInWindow
                    updateHandleOffsets()
                    updateSelectionToolbar()
                }
            }
        }

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    internal var dragBeginPosition by mutableStateOf(Offset.Zero)
        private set

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    internal var dragTotalDistance by mutableStateOf(Offset.Zero)
        private set

    /**
     * The calculated position of the start handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var startHandlePosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The calculated position of the end handle in the [SelectionContainer] coordinates. It
     * is null when handle shouldn't be displayed.
     * It is a [State] so reading it during the composition will cause recomposition every time
     * the position has been changed.
     */
    var endHandlePosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The handle that is currently being dragged, or null when no handle is being dragged. To get
     * the position of the last drag event, use [currentDragPosition].
     */
    var draggingHandle: Handle? by mutableStateOf(null)
        private set

    /**
     * When a handle is being dragged (i.e. [draggingHandle] is non-null), this is the last position
     * of the actual drag event. It is not clamped to handle positions. Null when not being dragged.
     */
    var currentDragPosition: Offset? by mutableStateOf(null)
        private set

    private val shouldShowMagnifier
        get() = draggingHandle != null && isInTouchMode && !isTriviallyCollapsedSelection()

    @VisibleForTesting
    internal var previousSelectionLayout: SelectionLayout? = null

    init {
        selectionRegistrar.onPositionChangeCallback = { selectableId ->
            if (selectableId in selectionRegistrar.subselections) {
                updateHandleOffsets()
                updateSelectionToolbar()
            }
        }

        selectionRegistrar.onSelectionUpdateStartCallback =
            { isInTouchMode, layoutCoordinates, rawPosition, selectionMode ->
                val textRect = with(layoutCoordinates.size) {
                    Rect(0f, 0f, width.toFloat(), height.toFloat())
                }

                val position = if (textRect.containsInclusive(rawPosition)) {
                    rawPosition
                } else {
                    rawPosition.coerceIn(textRect)
                }

                val positionInContainer = convertToContainerCoordinates(
                    layoutCoordinates,
                    position
                )

                if (positionInContainer.isSpecified) {
                    this.isInTouchMode = isInTouchMode
                    startSelection(
                        position = positionInContainer,
                        isStartHandle = false,
                        adjustment = selectionMode
                    )

                    focusRequester.requestFocus()
                    showToolbar = false
                }
            }

        selectionRegistrar.onSelectionUpdateSelectAll =
            { isInTouchMode, selectableId ->
                val (newSelection, newSubselection) = selectAll(
                    selectableId = selectableId,
                    previousSelection = selection,
                )
                if (newSelection != selection) {
                    selectionRegistrar.subselections = newSubselection
                    onSelectionChange(newSelection)
                }

                this.isInTouchMode = isInTouchMode
                focusRequester.requestFocus()
                showToolbar = false
            }

        selectionRegistrar.onSelectionUpdateCallback =
            { isInTouchMode,
                layoutCoordinates,
                newPosition,
                previousPosition,
                isStartHandle,
                selectionMode ->
                val newPositionInContainer =
                    convertToContainerCoordinates(layoutCoordinates, newPosition)
                val previousPositionInContainer =
                    convertToContainerCoordinates(layoutCoordinates, previousPosition)

                this.isInTouchMode = isInTouchMode
                updateSelection(
                    newPosition = newPositionInContainer,
                    previousPosition = previousPositionInContainer,
                    isStartHandle = isStartHandle,
                    adjustment = selectionMode
                )
            }

        selectionRegistrar.onSelectionUpdateEndCallback = {
            showToolbar = true
            // This property is set by updateSelection while dragging, so we need to clear it after
            // the original selection drag.
            draggingHandle = null
            currentDragPosition = null
        }

        selectionRegistrar.onSelectableChangeCallback = { selectableKey ->
            if (selectableKey in selectionRegistrar.subselections) {
                // Clear the selection range of each Selectable.
                onRelease()
                selection = null
            }
        }

        selectionRegistrar.afterSelectableUnsubscribe = { selectableId ->
            if (selectableId == selection?.start?.selectableId) {
                // The selectable that contains a selection handle just unsubscribed.
                // Hide the associated selection handle
                startHandlePosition = null
            }
            if (selectableId == selection?.end?.selectableId) {
                endHandlePosition = null
            }

            if (selectableId in selectionRegistrar.subselections) {
                // Unsubscribing the selectable may make the selection empty, which would hide it.
                updateSelectionToolbar()
            }
        }
    }

    /**
     * Returns the [Selectable] responsible for managing the given [AnchorInfo], or null if the
     * anchor is not from a currently-registered [Selectable].
     */
    internal fun getAnchorSelectable(anchor: AnchorInfo): Selectable? {
        return selectionRegistrar.selectableMap[anchor.selectableId]
    }

    private fun updateHandleOffsets() {
        val selection = selection
        val containerCoordinates = containerLayoutCoordinates
        val startSelectable = selection?.start?.let(::getAnchorSelectable)
        val endSelectable = selection?.end?.let(::getAnchorSelectable)
        val startLayoutCoordinates = startSelectable?.getLayoutCoordinates()
        val endLayoutCoordinates = endSelectable?.getLayoutCoordinates()

        if (
            selection == null ||
            containerCoordinates == null ||
            !containerCoordinates.isAttached ||
            (startLayoutCoordinates == null && endLayoutCoordinates == null)
        ) {
            this.startHandlePosition = null
            this.endHandlePosition = null
            return
        }

        val visibleBounds = containerCoordinates.visibleBounds()
        this.startHandlePosition = startLayoutCoordinates?.let { handleCoordinates ->
            // Set the new handle position only if the handle is in visible bounds or
            // the handle is still dragging. If handle goes out of visible bounds during drag,
            // handle popup is also removed from composition, halting the drag gesture. This
            // affects multiple text selection when selected text is configured with maxLines=1
            // and overflow=clip.
            val handlePosition = startSelectable.getHandlePosition(selection, isStartHandle = true)
            val position = containerCoordinates.localPositionOf(handleCoordinates, handlePosition)
            position.takeIf {
                draggingHandle == Handle.SelectionStart || visibleBounds.containsInclusive(it)
            }
        }

        this.endHandlePosition = endLayoutCoordinates?.let { handleCoordinates ->
            val handlePosition = endSelectable.getHandlePosition(selection, isStartHandle = false)
            val position = containerCoordinates.localPositionOf(handleCoordinates, handlePosition)
            position.takeIf {
                draggingHandle == Handle.SelectionEnd || visibleBounds.containsInclusive(it)
            }
        }
    }

    /**
     * Returns non-nullable [containerLayoutCoordinates].
     */
    internal fun requireContainerCoordinates(): LayoutCoordinates {
        val coordinates = containerLayoutCoordinates
        requireNotNull(coordinates) { "null coordinates" }
        require(coordinates.isAttached) { "unattached coordinates" }
        return coordinates
    }

    internal fun selectAll(
        selectableId: Long,
        previousSelection: Selection?
    ): Pair<Selection?, Map<Long, Selection>> {
        val subselections = mutableMapOf<Long, Selection>()
        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFold(null) { mergedSelection: Selection?, selectable: Selectable ->
                val selection = if (selectable.selectableId == selectableId)
                    selectable.getSelectAllSelection() else null
                selection?.let { subselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (isInTouchMode && newSelection != previousSelection) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        return Pair(newSelection, subselections)
    }

    /**
     * Returns whether the start and end anchors are equal.
     *
     * It is possible that this returns true, but the selection is still empty because it has
     * multiple collapsed selections across multiple selectables. To test for that case, use
     * [isNonEmptySelection].
     */
    internal fun isTriviallyCollapsedSelection(): Boolean {
        val selection = selection ?: return true
        return selection.start == selection.end
    }

    /**
     * Returns whether the selection selects zero characters.
     *
     * It is possible that the selection anchors are different but still result in a zero-width
     * selection. In this case, you may want to still show the selection anchors, but not allow for
     * a user to try and copy zero characters. To test for whether the anchors are equal, use
     * [isTriviallyCollapsedSelection].
     */
    internal fun isNonEmptySelection(): Boolean {
        val selection = selection ?: return false
        if (selection.start == selection.end) {
            return false
        }

        if (selection.start.selectableId == selection.end.selectableId) {
            // Selection is in the same selectable, but not the same anchors,
            // so there must be some selected text.
            return true
        }

        // All subselections associated with a selectable must be an empty selection.
        return selectionRegistrar.sort(requireContainerCoordinates()).fastAny { selectable ->
            selectionRegistrar.subselections[selectable.selectableId]
                ?.run { start.offset != end.offset }
                ?: false
        }
    }

    internal fun getSelectedText(): AnnotatedString? {
        if (selection == null || selectionRegistrar.subselections.isEmpty()) {
            return null
        }

        return buildAnnotatedString {
            selectionRegistrar.sort(requireContainerCoordinates()).fastForEach { selectable ->
                selectionRegistrar.subselections[selectable.selectableId]?.let { subSelection ->
                    val currentText = selectable.getText()
                    val currentSelectedText = if (subSelection.handlesCrossed) {
                        currentText.subSequence(
                            subSelection.end.offset,
                            subSelection.start.offset
                        )
                    } else {
                        currentText.subSequence(
                            subSelection.start.offset,
                            subSelection.end.offset
                        )
                    }

                    append(currentSelectedText)
                }
            }
        }
    }

    internal fun copy() {
        getSelectedText()?.takeIf { it.isNotEmpty() }?.let { clipboardManager?.setText(it) }
    }

    /**
     * Whether toolbar should be shown right now.
     * Examples: Show toolbar after user finishes selection.
     * Hide it during selection.
     * Hide it when no selection exists.
     */
    internal var showToolbar = false
        internal set(value) {
            field = value
            updateSelectionToolbar()
        }

    private fun toolbarCopy() {
        copy()
        onRelease()
    }

    private fun updateSelectionToolbar() {
        if (!hasFocus) {
            return
        }

        val textToolbar = textToolbar ?: return
        if (showToolbar && isInTouchMode && isNonEmptySelection()) {
            val rect = getContentRect() ?: return
            textToolbar.showMenu(rect = rect, onCopyRequested = ::toolbarCopy)
        } else if (textToolbar.status == TextToolbarStatus.Shown) {
            textToolbar.hide()
        }
    }

    /**
     * Calculate selected region as [Rect].
     * The result is the smallest [Rect] that encapsulates the entire selection,
     * coerced into visible bounds.
     */
    private fun getContentRect(): Rect? {
        selection ?: return null
        val containerCoordinates = containerLayoutCoordinates ?: return null
        if (!containerCoordinates.isAttached) return null
        val visibleBounds = containerCoordinates.visibleBounds()

        var anyExists = false
        var rootLeft = Float.POSITIVE_INFINITY
        var rootTop = Float.POSITIVE_INFINITY
        var rootRight = Float.NEGATIVE_INFINITY
        var rootBottom = Float.NEGATIVE_INFINITY

        val sortedSelectables = selectionRegistrar.sort(requireContainerCoordinates())
            .fastFilter {
                it.selectableId in selectionRegistrar.subselections
            }

        if (sortedSelectables.isEmpty()) {
            return null
        }

        val selectedSelectables = if (sortedSelectables.size == 1) {
            sortedSelectables
        } else {
            listOf(sortedSelectables.first(), sortedSelectables.last())
        }

        selectedSelectables.fastForEach { selectable ->
            val subSelection = selectionRegistrar.subselections[selectable.selectableId]
                ?: return@fastForEach

            val coordinates = selectable.getLayoutCoordinates()
                ?: return@fastForEach

            with(subSelection) {
                if (start.offset == end.offset) {
                    return@fastForEach
                }

                val minOffset = minOf(start.offset, end.offset)
                val maxOffset = maxOf(start.offset, end.offset)

                var left = Float.POSITIVE_INFINITY
                var top = Float.POSITIVE_INFINITY
                var right = Float.NEGATIVE_INFINITY
                var bottom = Float.NEGATIVE_INFINITY
                for (i in intArrayOf(minOffset, maxOffset)) {
                    val rect = selectable.getBoundingBox(i)
                    left = minOf(left, rect.left)
                    top = minOf(top, rect.top)
                    right = maxOf(right, rect.right)
                    bottom = maxOf(bottom, rect.bottom)
                }

                val localTopLeft = Offset(left, top)
                val localBottomRight = Offset(right, bottom)

                val containerTopLeft =
                    containerCoordinates.localPositionOf(coordinates, localTopLeft)
                val containerBottomRight =
                    containerCoordinates.localPositionOf(coordinates, localBottomRight)

                val rootVisibleTopLeft =
                    containerCoordinates.localToRoot(containerTopLeft.coerceIn(visibleBounds))
                val rootVisibleBottomRight =
                    containerCoordinates.localToRoot(containerBottomRight.coerceIn(visibleBounds))

                rootLeft = minOf(rootLeft, rootVisibleTopLeft.x)
                rootTop = minOf(rootTop, rootVisibleTopLeft.y)
                rootRight = maxOf(rootRight, rootVisibleBottomRight.x)
                rootBottom = maxOf(rootBottom, rootVisibleBottomRight.y)
                anyExists = true
            }
        }

        if (!anyExists) {
            return null
        }

        rootBottom += HandleHeight.value * 4
        return Rect(rootLeft, rootTop, rootRight, rootBottom)
    }

    // This is for PressGestureDetector to cancel the selection.
    fun onRelease() {
        selectionRegistrar.subselections = emptyMap()
        showToolbar = false
        if (selection != null) {
            onSelectionChange(null)
            if (isInTouchMode) {
                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    fun handleDragObserver(isStartHandle: Boolean): TextDragObserver = object : TextDragObserver {
        override fun onDown(point: Offset) {
            val selection = selection ?: return
            val anchor = if (isStartHandle) selection.start else selection.end
            val selectable = getAnchorSelectable(anchor) ?: return
            // The LayoutCoordinates of the composable where the drag gesture should begin. This
            // is used to convert the position of the beginning of the drag gesture from the
            // composable coordinates to selection container coordinates.
            val beginLayoutCoordinates = selectable.getLayoutCoordinates() ?: return

            // The position of the character where the drag gesture should begin. This is in
            // the composable coordinates.
            val beginCoordinates = getAdjustedCoordinates(
                selectable.getHandlePosition(
                    selection = selection, isStartHandle = isStartHandle
                )
            )

            // Convert the position where drag gesture begins from composable coordinates to
            // selection container coordinates.
            currentDragPosition = requireContainerCoordinates().localPositionOf(
                beginLayoutCoordinates,
                beginCoordinates
            )
            draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
            showToolbar = false
        }

        override fun onStart(startPoint: Offset) {
            val selection = selection!!
            val startSelectable =
                selectionRegistrar.selectableMap[selection.start.selectableId]
            val endSelectable =
                selectionRegistrar.selectableMap[selection.end.selectableId]
            // The LayoutCoordinates of the composable where the drag gesture should begin. This
            // is used to convert the position of the beginning of the drag gesture from the
            // composable coordinates to selection container coordinates.
            val beginLayoutCoordinates = if (isStartHandle) {
                startSelectable?.getLayoutCoordinates()!!
            } else {
                endSelectable?.getLayoutCoordinates()!!
            }

            // The position of the character where the drag gesture should begin. This is in
            // the composable coordinates.
            val beginCoordinates = getAdjustedCoordinates(
                if (isStartHandle) {
                    startSelectable!!.getHandlePosition(
                        selection = selection, isStartHandle = true
                    )
                } else {
                    endSelectable!!.getHandlePosition(
                        selection = selection, isStartHandle = false
                    )
                }
            )

            // Convert the position where drag gesture begins from composable coordinates to
            // selection container coordinates.
            dragBeginPosition = requireContainerCoordinates().localPositionOf(
                beginLayoutCoordinates,
                beginCoordinates
            )

            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            dragTotalDistance += delta
            val endPosition = dragBeginPosition + dragTotalDistance
            val consumed = updateSelection(
                newPosition = endPosition,
                previousPosition = dragBeginPosition,
                isStartHandle = isStartHandle,
                adjustment = SelectionAdjustment.CharacterWithWordAccelerate
            )
            if (consumed) {
                dragBeginPosition = endPosition
                dragTotalDistance = Offset.Zero
            }
        }

        private fun done() {
            showToolbar = true
            draggingHandle = null
            currentDragPosition = null
        }

        override fun onUp() = done()
        override fun onStop() = done()
        override fun onCancel() = done()
    }

    /**
     * Detect tap without consuming the up event.
     */
    private suspend fun PointerInputScope.detectNonConsumingTap(onTap: (Offset) -> Unit) {
        awaitEachGesture {
            waitForUpOrCancellation()?.let {
                onTap(it.position)
            }
        }
    }

    private fun Modifier.onClearSelectionRequested(block: () -> Unit): Modifier {
        return if (hasFocus) pointerInput(Unit) { detectNonConsumingTap { block() } } else this
    }

    private fun convertToContainerCoordinates(
        layoutCoordinates: LayoutCoordinates,
        offset: Offset
    ): Offset {
        val coordinates = containerLayoutCoordinates
        if (coordinates == null || !coordinates.isAttached) return Offset.Unspecified
        return requireContainerCoordinates().localPositionOf(layoutCoordinates, offset)
    }

    /**
     * Cancel the previous selection and start a new selection at the given [position].
     * It's used for long-press, double-click, triple-click and so on to start selection.
     *
     * @param position initial position of the selection. Both start and end handle is considered
     * at this position.
     * @param isStartHandle whether it's considered as the start handle moving. This parameter
     * will influence the [SelectionAdjustment]'s behavior. For example,
     * [SelectionAdjustment.Character] only adjust the moving handle.
     * @param adjustment the selection adjustment.
     */
    private fun startSelection(
        position: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ) {
        previousSelectionLayout = null
        updateSelection(
            startHandlePosition = position,
            endHandlePosition = position,
            previousHandlePosition = Offset.Unspecified,
            isStartHandle = isStartHandle,
            adjustment = adjustment
        )
    }

    /**
     * Updates the selection after one of the selection handle moved.
     *
     * @param newPosition the new position of the moving selection handle.
     * @param previousPosition the previous position of the moving selection handle.
     * @param isStartHandle whether the moving selection handle is the start handle.
     * @param adjustment the [SelectionAdjustment] used to adjust the raw selection range and
     * produce the final selection range.
     *
     * @return a boolean representing whether the movement is consumed.
     *
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        newPosition: Offset?,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        if (newPosition == null) return false
        val otherHandlePosition = selection?.let { selection ->
            val otherSelectableId = if (isStartHandle) {
                selection.end.selectableId
            } else {
                selection.start.selectableId
            }
            val otherSelectable =
                selectionRegistrar.selectableMap[otherSelectableId] ?: return@let null
            convertToContainerCoordinates(
                otherSelectable.getLayoutCoordinates()!!,
                getAdjustedCoordinates(
                    otherSelectable.getHandlePosition(selection, !isStartHandle)
                )
            )
        } ?: return false

        val startHandlePosition = if (isStartHandle) newPosition else otherHandlePosition
        val endHandlePosition = if (isStartHandle) otherHandlePosition else newPosition

        return updateSelection(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = previousPosition,
            isStartHandle = isStartHandle,
            adjustment = adjustment
        )
    }

    /**
     * Updates the selection after one of the selection handle moved.
     *
     * To make sure that [SelectionAdjustment] works correctly, it's expected that only one
     * selection handle is updated each time. The only exception is that when a new selection is
     * started. In this case, [previousHandlePosition] is always null.
     *
     * @param startHandlePosition the position of the start selection handle.
     * @param endHandlePosition the position of the end selection handle.
     * @param previousHandlePosition the position of the moving handle before the update.
     * @param isStartHandle whether the moving selection handle is the start handle.
     * @param adjustment the [SelectionAdjustment] used to adjust the raw selection range and
     * produce the final selection range.
     *
     * @return a boolean representing whether the movement is consumed. It's useful for the case
     * where a selection handle is updating consecutively. When the return value is true, it's
     * expected that the caller will update the [startHandlePosition] to be the given
     * [endHandlePosition] in following calls.
     *
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
        currentDragPosition = if (isStartHandle) startHandlePosition else endHandlePosition

        val selectionLayout = getSelectionLayout(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = previousHandlePosition,
            isStartHandle = isStartHandle,
        )

        if (!selectionLayout.shouldRecomputeSelection(previousSelectionLayout)) {
            return false
        }

        val newSelection = adjustment.adjust(selectionLayout)
        if (newSelection != selection) {
            selectionChanged(selectionLayout, newSelection)
        }
        previousSelectionLayout = selectionLayout
        return true
    }

    private fun getSelectionLayout(
        startHandlePosition: Offset,
        endHandlePosition: Offset,
        previousHandlePosition: Offset,
        isStartHandle: Boolean,
    ): SelectionLayout {
        val containerCoordinates = requireContainerCoordinates()
        val sortedSelectables = selectionRegistrar.sort(containerCoordinates)

        val idToIndexMap = mutableMapOf<Long, Int>()
        sortedSelectables.fastForEachIndexed { index, selectable ->
            idToIndexMap[selectable.selectableId] = index
        }

        val selectableIdOrderingComparator = compareBy<Long> { idToIndexMap[it] }

        // if previous handle is null, then treat this as a new selection.
        val previousSelection = if (previousHandlePosition.isUnspecified) null else selection
        val builder = SelectionLayoutBuilder(
            startHandlePosition = startHandlePosition,
            endHandlePosition = endHandlePosition,
            previousHandlePosition = previousHandlePosition,
            containerCoordinates = containerCoordinates,
            isStartHandle = isStartHandle,
            previousSelection = previousSelection,
            selectableIdOrderingComparator = selectableIdOrderingComparator,
        )

        sortedSelectables.fastForEach {
            it.appendSelectableInfoToBuilder(builder)
        }

        return builder.build()
    }

    private fun selectionChanged(selectionLayout: SelectionLayout, newSelection: Selection) {
        if (shouldPerformHaptics()) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        selectionRegistrar.subselections = selectionLayout.createSubSelections(newSelection)
        onSelectionChange(newSelection)
    }

    @VisibleForTesting
    internal fun shouldPerformHaptics(): Boolean =
        isInTouchMode && selectionRegistrar.selectables.fastAny { it.getText().isNotEmpty() }

    fun contextMenuOpenAdjustment(position: Offset) {
        val isEmptySelection = selection?.toTextRange()?.collapsed ?: true
        // TODO(b/209483184) the logic should be more complex here, it should check that current
        //  selection doesn't include click position
        if (isEmptySelection) {
            startSelection(
                position = position,
                isStartHandle = true,
                adjustment = SelectionAdjustment.Word
            )
        }
    }
}

internal fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}

internal expect fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean

internal expect fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier

internal fun calculateSelectionMagnifierCenterAndroid(
    manager: SelectionManager,
    magnifierSize: IntSize
): Offset {
    val selection = manager.selection ?: return Offset.Unspecified
    return when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.SelectionStart -> getMagnifierCenter(manager, magnifierSize, selection.start)
        Handle.SelectionEnd -> getMagnifierCenter(manager, magnifierSize, selection.end)
        Handle.Cursor -> error("SelectionContainer does not support cursor")
    }
}

private fun getMagnifierCenter(
    manager: SelectionManager,
    magnifierSize: IntSize,
    anchor: AnchorInfo
): Offset {
    val selectable = manager.getAnchorSelectable(anchor) ?: return Offset.Unspecified
    val containerCoordinates = manager.containerLayoutCoordinates ?: return Offset.Unspecified
    val selectableCoordinates = selectable.getLayoutCoordinates() ?: return Offset.Unspecified
    val offset = anchor.offset

    if (offset > selectable.getLastVisibleOffset()) return Offset.Unspecified

    // The horizontal position doesn't snap to cursor positions but should directly track the
    // actual drag.
    val localDragPosition = selectableCoordinates.localPositionOf(
        containerCoordinates,
        manager.currentDragPosition!!
    )
    val dragX = localDragPosition.x

    // But it is constrained by the horizontal bounds of the current line.
    val lineRange = selectable.getRangeOfLineContaining(offset)
    val textConstrainedX = if (lineRange.collapsed) {
        // A collapsed range implies the text is empty.
        // line left and right are equal for this offset, so use either
        selectable.getLineLeft(offset)
    } else {
        val lineStartX = selectable.getLineLeft(lineRange.start)
        val lineEndX = selectable.getLineRight(lineRange.end - 1)
        // in RTL/BiDi, lineStartX may be larger than lineEndX
        val minX = minOf(lineStartX, lineEndX)
        val maxX = maxOf(lineStartX, lineEndX)
        dragX.coerceIn(minX, maxX)
    }

    // selectable couldn't determine horizontals
    if (textConstrainedX == -1f) return Offset.Unspecified

    // Hide the magnifier when dragged too far (outside the horizontal bounds of how big the
    // magnifier actually is). See
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/widget/Editor.java;l=5228-5231;drc=2fdb6bd709be078b72f011334362456bb758922c
    if ((dragX - textConstrainedX).absoluteValue > magnifierSize.width / 2) {
        return Offset.Unspecified
    }

    val lineCenterY = selectable.getCenterYForOffset(offset)

    // selectable couldn't determine the line center
    if (lineCenterY == -1f) return Offset.Unspecified

    return containerCoordinates.localPositionOf(
        sourceCoordinates = selectableCoordinates,
        relativeToSource = Offset(textConstrainedX, lineCenterY)
    )
}

/** Returns the boundary of the visible area in this [LayoutCoordinates]. */
internal fun LayoutCoordinates.visibleBounds(): Rect {
    // globalBounds is the global boundaries of this LayoutCoordinates after it's clipped by
    // parents. We can think it as the global visible bounds of this Layout. Here globalBounds
    // is convert to local, which is the boundary of the visible area within the LayoutCoordinates.
    val boundsInWindow = boundsInWindow()
    return Rect(
        windowToLocal(boundsInWindow.topLeft),
        windowToLocal(boundsInWindow.bottomRight)
    )
}

internal fun Rect.containsInclusive(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom
