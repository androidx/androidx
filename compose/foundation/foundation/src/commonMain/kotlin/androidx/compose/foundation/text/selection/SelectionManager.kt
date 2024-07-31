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
import androidx.collection.LongObjectMap
import androidx.collection.emptyLongObjectMap
import androidx.collection.mutableLongIntMapOf
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.input.internal.coerceIn
import androidx.compose.foundation.text.isPositionInsideSelection
import androidx.compose.foundation.text.selection.Selection.AnchorInfo
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMapNotNull
import kotlin.math.absoluteValue

/** A bridge class between user interaction to the text composables for text selection. */
internal class SelectionManager(private val selectionRegistrar: SelectionRegistrarImpl) {

    private val _selection: MutableState<Selection?> = mutableStateOf(null)

    /** The current selection. */
    var selection: Selection?
        get() = _selection.value
        set(value) {
            _selection.value = value
            if (value != null) {
                updateHandleOffsets()
            }
        }

    /** Is touch mode active */
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
    var onSelectionChange: (Selection?) -> Unit = { selection = it }
        set(newOnSelectionChange) {
            // Wrap the given lambda with one that sets the selection immediately.
            // The onSelectionChange loop requires a composition to happen for the selection
            // to be updated, so we want to shorten that loop for gesture use cases where
            // multiple selection changing events can be acted on within a single composition
            // loop. Previous selection is used as part of that loop so keeping it up to date
            // is important.
            field = { newSelection ->
                selection = newSelection
                newOnSelectionChange(newSelection)
            }
        }

    /** [HapticFeedback] handle to perform haptic feedback. */
    var hapticFeedBack: HapticFeedback? = null

    /** [ClipboardManager] to perform clipboard features. */
    var clipboardManager: ClipboardManager? = null

    /** [TextToolbar] to show floating toolbar(post-M) or primary toolbar(pre-M). */
    var textToolbar: TextToolbar? = null

    /** Focus requester used to request focus when selection becomes active. */
    var focusRequester: FocusRequester = FocusRequester()

    /** Return true if the corresponding SelectionContainer is focused. */
    var hasFocus: Boolean by mutableStateOf(false)

    /** Modifier for selection container. */
    val modifier
        get() =
            Modifier.onClearSelectionRequested { onRelease() }
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

    /** Layout Coordinates of the selection container. */
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
     * The calculated position of the start handle in the [SelectionContainer] coordinates. It is
     * null when handle shouldn't be displayed. It is a [State] so reading it during the composition
     * will cause recomposition every time the position has been changed.
     */
    var startHandlePosition: Offset? by mutableStateOf(null)
        private set

    /**
     * The calculated position of the end handle in the [SelectionContainer] coordinates. It is null
     * when handle shouldn't be displayed. It is a [State] so reading it during the composition will
     * cause recomposition every time the position has been changed.
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

    @VisibleForTesting internal var previousSelectionLayout: SelectionLayout? = null

    init {
        selectionRegistrar.onPositionChangeCallback = { selectableId ->
            if (selectableId in selectionRegistrar.subselections) {
                updateHandleOffsets()
                updateSelectionToolbar()
            }
        }

        selectionRegistrar.onSelectionUpdateStartCallback =
            { isInTouchMode, layoutCoordinates, rawPosition, selectionMode ->
                val textRect =
                    with(layoutCoordinates.size) { Rect(0f, 0f, width.toFloat(), height.toFloat()) }

                val position =
                    if (textRect.containsInclusive(rawPosition)) {
                        rawPosition
                    } else {
                        rawPosition.coerceIn(textRect)
                    }

                val positionInContainer = convertToContainerCoordinates(layoutCoordinates, position)

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

        selectionRegistrar.onSelectionUpdateSelectAll = { isInTouchMode, selectableId ->
            val (newSelection, newSubselection) =
                selectAllInSelectable(
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
            {
                isInTouchMode,
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

        // This function is meant to handle changes in the selectable content,
        // such as the text changing.
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
        this.startHandlePosition =
            startLayoutCoordinates?.let { handleCoordinates ->
                // Set the new handle position only if the handle is in visible bounds or
                // the handle is still dragging. If handle goes out of visible bounds during drag,
                // handle popup is also removed from composition, halting the drag gesture. This
                // affects multiple text selection when selected text is configured with maxLines=1
                // and overflow=clip.
                val handlePosition =
                    startSelectable.getHandlePosition(selection, isStartHandle = true)
                if (handlePosition.isUnspecified) return@let null
                val position =
                    containerCoordinates.localPositionOf(handleCoordinates, handlePosition)
                position.takeIf {
                    draggingHandle == Handle.SelectionStart || visibleBounds.containsInclusive(it)
                }
            }

        this.endHandlePosition =
            endLayoutCoordinates?.let { handleCoordinates ->
                val handlePosition =
                    endSelectable.getHandlePosition(selection, isStartHandle = false)
                if (handlePosition.isUnspecified) return@let null
                val position =
                    containerCoordinates.localPositionOf(handleCoordinates, handlePosition)
                position.takeIf {
                    draggingHandle == Handle.SelectionEnd || visibleBounds.containsInclusive(it)
                }
            }
    }

    /** Returns non-nullable [containerLayoutCoordinates]. */
    internal fun requireContainerCoordinates(): LayoutCoordinates {
        val coordinates = containerLayoutCoordinates
        requirePreconditionNotNull(coordinates) { "null coordinates" }
        requirePrecondition(coordinates.isAttached) { "unattached coordinates" }
        return coordinates
    }

    internal fun selectAllInSelectable(
        selectableId: Long,
        previousSelection: Selection?
    ): Pair<Selection?, LongObjectMap<Selection>> {
        val subselections = mutableLongObjectMapOf<Selection>()
        val newSelection =
            selectionRegistrar.sort(requireContainerCoordinates()).fastFold(null) {
                mergedSelection: Selection?,
                selectable: Selectable ->
                val selection =
                    if (selectable.selectableId == selectableId) selectable.getSelectAllSelection()
                    else null
                selection?.let { subselections[selectable.selectableId] = it }
                merge(mergedSelection, selection)
            }
        if (isInTouchMode && newSelection != previousSelection) {
            hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        return Pair(newSelection, subselections)
    }

    /** Returns whether the selection encompasses the entire container. */
    internal fun isEntireContainerSelected(): Boolean {
        val selectables = selectionRegistrar.sort(requireContainerCoordinates())

        // If there are no selectables, then an empty selection spans the entire container.
        if (selectables.isEmpty()) return true

        // Since some text exists, we must make sure that every selectable is fully selected.
        return selectables.fastAll {
            val text = it.getText()
            if (text.isEmpty()) return@fastAll true // empty text is inherently fully selected

            // If a non-empty selectable isn't included in the sub-selections,
            // then some text in the container is not selected.
            val subSelection =
                selectionRegistrar.subselections[it.selectableId] ?: return@fastAll false

            val selectionStart = subSelection.start.offset
            val selectionEnd = subSelection.end.offset

            // The selection could be reversed,
            // so just verify that the difference between the two offsets matches the text length
            (selectionStart - selectionEnd).absoluteValue == text.length
        }
    }

    /** Creates and sets a selection spanning the entire container. */
    internal fun selectAll() {
        val selectables = selectionRegistrar.sort(requireContainerCoordinates())
        if (selectables.isEmpty()) return

        var firstSubSelection: Selection? = null
        var lastSubSelection: Selection? = null
        val newSubSelections =
            mutableLongObjectMapOf<Selection>().apply {
                selectables.fastForEach { selectable ->
                    val subSelection = selectable.getSelectAllSelection() ?: return@fastForEach
                    if (firstSubSelection == null) firstSubSelection = subSelection
                    lastSubSelection = subSelection
                    put(selectable.selectableId, subSelection)
                }
            }

        if (newSubSelections.isEmpty()) return

        // first/last sub selections are implied to be non-null from here on out
        val newSelection =
            if (firstSubSelection === lastSubSelection) {
                firstSubSelection
            } else {
                Selection(
                    start = firstSubSelection!!.start,
                    end = lastSubSelection!!.end,
                    handlesCrossed = false,
                )
            }

        selectionRegistrar.subselections = newSubSelections
        onSelectionChange(newSelection)
        previousSelectionLayout = null
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
            selectionRegistrar.subselections[selectable.selectableId]?.run {
                start.offset != end.offset
            } ?: false
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
                    val currentSelectedText =
                        if (subSelection.handlesCrossed) {
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
     * Whether toolbar should be shown right now. Examples: Show toolbar after user finishes
     * selection. Hide it during selection. Hide it when no selection exists.
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
        if (showToolbar && isInTouchMode) {
            val rect = getContentRect() ?: return
            textToolbar.showMenu(
                rect = rect,
                onCopyRequested = if (isNonEmptySelection()) ::toolbarCopy else null,
                onSelectAllRequested = if (isEntireContainerSelected()) null else ::selectAll,
            )
        } else if (textToolbar.status == TextToolbarStatus.Shown) {
            textToolbar.hide()
        }
    }

    /**
     * Calculate selected region as [Rect]. The result is the smallest [Rect] that encapsulates the
     * entire selection, coerced into visible bounds.
     */
    private fun getContentRect(): Rect? {
        selection ?: return null
        val containerCoordinates = containerLayoutCoordinates ?: return null
        if (!containerCoordinates.isAttached) return null

        val selectableSubSelections =
            selectionRegistrar
                .sort(requireContainerCoordinates())
                .fastMapNotNull { selectable ->
                    selectionRegistrar.subselections[selectable.selectableId]?.let {
                        selectable to it
                    }
                }
                .firstAndLast()

        if (selectableSubSelections.isEmpty()) return null
        val selectedRegionRect =
            getSelectedRegionRect(selectableSubSelections, containerCoordinates)

        if (selectedRegionRect == invertedInfiniteRect) return null

        val visibleRect = containerCoordinates.visibleBounds().intersect(selectedRegionRect)
        // if the rectangles do not at least touch at the edges, we shouldn't show the toolbar
        if (visibleRect.width < 0 || visibleRect.height < 0) return null

        val rootRect = visibleRect.translate(containerCoordinates.positionInRoot())
        return rootRect.copy(bottom = rootRect.bottom + HandleHeight.value * 4)
    }

    // This is for PressGestureDetector to cancel the selection.
    fun onRelease() {
        selectionRegistrar.subselections = emptyLongObjectMap()
        showToolbar = false
        if (selection != null) {
            onSelectionChange(null)
            if (isInTouchMode) {
                hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    fun handleDragObserver(isStartHandle: Boolean): TextDragObserver =
        object : TextDragObserver {
            override fun onDown(point: Offset) {
                // if the handle position is null, then it is invisible, so ignore the gesture
                (if (isStartHandle) startHandlePosition else endHandlePosition) ?: return

                val selection = selection ?: return
                val anchor = if (isStartHandle) selection.start else selection.end
                val selectable = getAnchorSelectable(anchor) ?: return
                // The LayoutCoordinates of the composable where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // composable coordinates to selection container coordinates.
                val beginLayoutCoordinates = selectable.getLayoutCoordinates() ?: return

                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                val handlePosition = selectable.getHandlePosition(selection, isStartHandle)
                if (handlePosition.isUnspecified) return
                val beginCoordinates = getAdjustedCoordinates(handlePosition)

                // Convert the position where drag gesture begins from composable coordinates to
                // selection container coordinates.
                currentDragPosition =
                    requireContainerCoordinates()
                        .localPositionOf(beginLayoutCoordinates, beginCoordinates)
                draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
                showToolbar = false
            }

            override fun onStart(startPoint: Offset) {
                draggingHandle ?: return

                val selection = selection!!
                val anchor = if (isStartHandle) selection.start else selection.end
                val selectable =
                    checkPreconditionNotNull(
                        selectionRegistrar.selectableMap[anchor.selectableId]
                    ) {
                        "SelectionRegistrar should contain the current selection's selectableIds"
                    }

                // The LayoutCoordinates of the composable where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // composable coordinates to selection container coordinates.
                val beginLayoutCoordinates =
                    checkPreconditionNotNull(selectable.getLayoutCoordinates()) {
                        "Current selectable should have layout coordinates."
                    }

                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                val handlePosition = selectable.getHandlePosition(selection, isStartHandle)
                if (handlePosition.isUnspecified) return
                val beginCoordinates = getAdjustedCoordinates(handlePosition)

                // Convert the position where drag gesture begins from composable coordinates to
                // selection container coordinates.
                dragBeginPosition =
                    requireContainerCoordinates()
                        .localPositionOf(beginLayoutCoordinates, beginCoordinates)

                // Zero out the total distance that being dragged.
                dragTotalDistance = Offset.Zero
            }

            override fun onDrag(delta: Offset) {
                draggingHandle ?: return

                dragTotalDistance += delta
                val endPosition = dragBeginPosition + dragTotalDistance
                val consumed =
                    updateSelection(
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

    /** Detect tap without consuming the up event. */
    private suspend fun PointerInputScope.detectNonConsumingTap(onTap: (Offset) -> Unit) {
        awaitEachGesture { waitForUpOrCancellation()?.let { onTap(it.position) } }
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
     * Cancel the previous selection and start a new selection at the given [position]. It's used
     * for long-press, double-click, triple-click and so on to start selection.
     *
     * @param position initial position of the selection. Both start and end handle is considered at
     *   this position.
     * @param isStartHandle whether it's considered as the start handle moving. This parameter will
     *   influence the [SelectionAdjustment]'s behavior. For example,
     *   [SelectionAdjustment.Character] only adjust the moving handle.
     * @param adjustment the selection adjustment.
     */
    private fun startSelection(
        position: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment
    ) {
        previousSelectionLayout = null
        updateSelection(
            position = position,
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
     *   produce the final selection range.
     * @return a boolean representing whether the movement is consumed.
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        newPosition: Offset?,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        if (newPosition == null) return false
        return updateSelection(
            position = newPosition,
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
     * @param position the position of the current gesture.
     * @param previousHandlePosition the position of the moving handle before the update.
     * @param isStartHandle whether the moving selection handle is the start handle.
     * @param adjustment the [SelectionAdjustment] used to adjust the raw selection range and
     *   produce the final selection range.
     * @return a boolean representing whether the movement is consumed. It's useful for the case
     *   where a selection handle is updating consecutively. When the return value is true, it's
     *   expected that the caller will update the [startHandlePosition] to be the given
     *   [endHandlePosition] in following calls.
     * @see SelectionAdjustment
     */
    internal fun updateSelection(
        position: Offset,
        previousHandlePosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
    ): Boolean {
        draggingHandle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd
        currentDragPosition = position

        val selectionLayout =
            getSelectionLayout(position, previousHandlePosition, isStartHandle) ?: return false
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
        position: Offset,
        previousHandlePosition: Offset,
        isStartHandle: Boolean,
    ): SelectionLayout? {
        val containerCoordinates = requireContainerCoordinates()
        val sortedSelectables = selectionRegistrar.sort(containerCoordinates)

        val idToIndexMap = mutableLongIntMapOf()
        sortedSelectables.fastForEachIndexed { index, selectable ->
            idToIndexMap[selectable.selectableId] = index
        }

        val selectableIdOrderingComparator = compareBy<Long> { idToIndexMap[it] }

        // if previous handle is null, then treat this as a new selection.
        val previousSelection = if (previousHandlePosition.isUnspecified) null else selection
        val builder =
            SelectionLayoutBuilder(
                currentPosition = position,
                previousHandlePosition = previousHandlePosition,
                containerCoordinates = containerCoordinates,
                isStartHandle = isStartHandle,
                previousSelection = previousSelection,
                selectableIdOrderingComparator = selectableIdOrderingComparator,
            )

        sortedSelectables.fastForEach { it.appendSelectableInfoToBuilder(builder) }

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

    /**
     * Implements the macOS select-word-on-right-click behavior.
     *
     * If the current selection does not already include [position], select the word at [position].
     */
    fun selectWordAtPositionIfNotAlreadySelected(position: Offset) {
        val containerCoordinates = containerLayoutCoordinates ?: return
        if (!containerCoordinates.isAttached) return

        val isClickedPositionInsideSelection =
            selectionRegistrar.selectables.fastAny { selectable ->
                val selection =
                    selectionRegistrar.subselections[selectable.selectableId]
                        ?: return@fastAny false
                val selectableLayoutCoords =
                    selectable.getLayoutCoordinates() ?: return@fastAny false
                val positionInSelectable =
                    selectableLayoutCoords.localPositionOf(containerCoordinates, position)
                val textLayoutResult = selectable.textLayoutResult() ?: return@fastAny false
                textLayoutResult.isPositionInsideSelection(
                    position = positionInSelectable,
                    selectionRange = selection.toTextRange()
                )
            }
        if (!isClickedPositionInsideSelection) {
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

private val invertedInfiniteRect =
    Rect(
        left = Float.POSITIVE_INFINITY,
        top = Float.POSITIVE_INFINITY,
        right = Float.NEGATIVE_INFINITY,
        bottom = Float.NEGATIVE_INFINITY
    )

private fun <T> List<T>.firstAndLast(): List<T> =
    when (size) {
        0,
        1 -> this
        else -> listOf(first(), last())
    }

/**
 * Get the selected region rect in the given [containerCoordinates]. This will compute the smallest
 * rect that contains every first/last character bounding box of each selectable. If for any reason
 * there are no bounding boxes, then the [invertedInfiniteRect] is returned.
 */
@VisibleForTesting
internal fun getSelectedRegionRect(
    selectableSubSelectionPairs: List<Pair<Selectable, Selection>>,
    containerCoordinates: LayoutCoordinates,
): Rect {
    if (selectableSubSelectionPairs.isEmpty()) return invertedInfiniteRect
    var (containerLeft, containerTop, containerRight, containerBottom) = invertedInfiniteRect
    selectableSubSelectionPairs.fastForEach { (selectable, subSelection) ->
        val startOffset = subSelection.start.offset
        val endOffset = subSelection.end.offset
        if (startOffset == endOffset) return@fastForEach
        val localCoordinates = selectable.getLayoutCoordinates() ?: return@fastForEach

        val minOffset = minOf(startOffset, endOffset)
        val maxOffset = maxOf(startOffset, endOffset)
        val offsets =
            if (minOffset == maxOffset - 1) {
                intArrayOf(minOffset)
            } else {
                intArrayOf(minOffset, maxOffset - 1)
            }
        var (left, top, right, bottom) = invertedInfiniteRect
        for (i in offsets) {
            val rect = selectable.getBoundingBox(i)
            left = minOf(left, rect.left)
            top = minOf(top, rect.top)
            right = maxOf(right, rect.right)
            bottom = maxOf(bottom, rect.bottom)
        }

        val localTopLeft = Offset(left, top)
        val localBottomRight = Offset(right, bottom)

        val containerTopLeft = containerCoordinates.localPositionOf(localCoordinates, localTopLeft)
        val containerBottomRight =
            containerCoordinates.localPositionOf(localCoordinates, localBottomRight)

        containerLeft = minOf(containerLeft, containerTopLeft.x)
        containerTop = minOf(containerTop, containerTopLeft.y)
        containerRight = maxOf(containerRight, containerBottomRight.x)
        containerBottom = maxOf(containerBottom, containerBottomRight.y)
    }
    return Rect(containerLeft, containerTop, containerRight, containerBottom)
}

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
    val localDragPosition =
        selectableCoordinates.localPositionOf(containerCoordinates, manager.currentDragPosition!!)
    val dragX = localDragPosition.x

    // But it is constrained by the horizontal bounds of the current line.
    val lineRange = selectable.getRangeOfLineContaining(offset)
    val textConstrainedX =
        if (lineRange.collapsed) {
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
    // Also check whether magnifierSize is calculated. A platform magnifier instance is not
    // created until it's requested for the first time. So the size will only be calculated after we
    // return a specified offset from this function.
    // It is very unlikely that this behavior would cause a flicker since magnifier immediately
    // shows up where the pointer is being dragged. The pointer needs to drag further than the half
    // of magnifier's width to hide by the following logic.
    if (
        magnifierSize != IntSize.Zero &&
            (dragX - textConstrainedX).absoluteValue > magnifierSize.width / 2
    ) {
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
    return Rect(windowToLocal(boundsInWindow.topLeft), windowToLocal(boundsInWindow.bottomRight))
}

internal fun Rect.containsInclusive(offset: Offset): Boolean =
    offset.x in left..right && offset.y in top..bottom
