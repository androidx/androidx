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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.gestures.DragEvent.DragCancelled
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.gestures.DragEvent.DragStarted
import androidx.compose.foundation.gestures.DragEvent.DragStopped
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import kotlin.math.absoluteValue

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal class IndirectTouchInputDragCycleDetector(val node: DragGestureNode) {
    /** Store non-initialized states for re-use */
    private var _awaitDownState: DragDetectionState.AwaitDown? = null
    private val awaitDownState: DragDetectionState.AwaitDown
        get() = _awaitDownState ?: DragDetectionState.AwaitDown().also { _awaitDownState = it }

    private var _draggingState: DragDetectionState.Dragging? = null
    private val draggingState: DragDetectionState.Dragging
        get() = _draggingState ?: DragDetectionState.Dragging().also { _draggingState = it }

    private var _awaitTouchSlopState: DragDetectionState.AwaitTouchSlop? = null
    private val awaitTouchSlopState: DragDetectionState.AwaitTouchSlop
        get() =
            _awaitTouchSlopState
                ?: DragDetectionState.AwaitTouchSlop().also { _awaitTouchSlopState = it }

    private var _awaitGesturePickupState: DragDetectionState.AwaitGesturePickup? = null
    private val awaitGesturePickupState: DragDetectionState.AwaitGesturePickup
        get() =
            _awaitGesturePickupState
                ?: DragDetectionState.AwaitGesturePickup().also { _awaitGesturePickupState = it }

    private var currentDragState: DragDetectionState? = null
    private var velocityTracker: VelocityTracker? = null
    private var previousPositionOnScreen = Offset.Unspecified
    private var touchSlopDetector: TouchSlopDetector? = null
    private val touchSmooth = TouchInputEventSmoother()
    private val offsetSmoother = OffsetSmoother()

    /**
     * Accumulated position offset of this [Modifier.Node] that happened during a drag cycle. This
     * is used to correct the pointer input events that are added to the Velocity Tracker. If this
     * Node is static during the drag cycle, nothing will happen. On the other hand, if the position
     * of this node changes during the drag cycle, we need to correct the Pointer Input used for the
     * drag events, this is because Velocity Tracker doesn't have the knowledge about changes in the
     * position of the container that uses it, and because each Pointer Input event is related to
     * the container's root.
     */
    private var nodeOffset = Offset.Zero

    private fun requireTouchSlopDetector(): TouchSlopDetector =
        requireNotNull(touchSlopDetector) { "Touch slop detector not initialized." }

    private fun requireVelocityTracker(): VelocityTracker =
        requireNotNull(velocityTracker) { "Velocity Tracker not initialized." }

    fun processIndirectPointerInputEvent(
        indirectPointerInputEvent: IndirectTouchEvent,
        pass: PointerEventPass,
    ) {
        // initialize current state
        if (currentDragState == null) currentDragState = awaitDownState

        when (
            val state = requireNotNull(currentDragState) { "currentDragState should not be null" }
        ) {
            is DragDetectionState.AwaitDown ->
                processInitialDownState(indirectPointerInputEvent, pass, state)
            is DragDetectionState.AwaitTouchSlop ->
                processAwaitTouchSlop(indirectPointerInputEvent, pass, state)
            is DragDetectionState.AwaitGesturePickup ->
                processAwaitGesturePickup(indirectPointerInputEvent, pass, state)
            is DragDetectionState.Dragging ->
                processDraggingState(indirectPointerInputEvent, pass, state)
        }
    }

    fun resetDragDetectionState() {
        moveToAwaitDownState()
        if (node.isListeningForEvents) sendDragCancelled()
        velocityTracker = null
        offsetSmoother.reset()
    }

    private fun moveToAwaitTouchSlopState(
        initialDown: IndirectPointerInputChange,
        pointerId: PointerId,
        initialTouchSlopPositionChange: Offset = Offset.Zero,
        verifyConsumptionInFinalPass: Boolean = false,
    ) {
        currentDragState =
            awaitTouchSlopState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                if (touchSlopDetector == null) {
                    touchSlopDetector = TouchSlopDetector(node.orientationLock)
                } else {
                    touchSlopDetector?.orientation = node.orientationLock
                    touchSlopDetector?.reset(initialTouchSlopPositionChange)
                }
                this.verifyConsumptionInFinalPass = verifyConsumptionInFinalPass
            }
    }

    private fun moveToDraggingState(pointerId: PointerId) {
        currentDragState = draggingState.apply { this.pointerId = pointerId }
    }

    private fun moveToAwaitDownState() {
        currentDragState =
            awaitDownState.apply {
                awaitTouchSlop = DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized
                consumedOnInitial = false
            }
    }

    private fun moveToAwaitGesturePickupState(
        initialDown: IndirectPointerInputChange,
        pointerId: PointerId,
        touchSlopDetector: TouchSlopDetector,
    ) {
        currentDragState =
            awaitGesturePickupState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                this.touchSlopDetector = touchSlopDetector.also { it.reset() }
            }
    }

    private fun processInitialDownState(
        indirectPointerInputEvent: IndirectTouchEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitDown,
    ) {
        /** Wait for a down event in any pass. */
        if (indirectPointerInputEvent.changes.isEmpty()) return
        if (!indirectPointerInputEvent.changes.fastAll { it.changedToDownIgnoreConsumed() }) return

        val firstDown = indirectPointerInputEvent.changes.first()
        val awaitTouchSlop =
            when (state.awaitTouchSlop) {
                DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized -> {
                    if (!node.startDragImmediately()) {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.Yes
                    } else {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.No
                    }
                }
                else -> state.awaitTouchSlop
            }

        // update the touch slop in the current state
        state.awaitTouchSlop = awaitTouchSlop

        if (pass == PointerEventPass.Initial) {
            // If we shouldn't await touch slop, we consume the event immediately.
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.No) {
                firstDown.consume()

                // Change state properties so we dispatch only later, this aligns with the previous
                // behavior where dispatching only happened during the main pass
                state.consumedOnInitial = true
            }
        }

        if (pass == PointerEventPass.Main) {
            /**
             * At this point we detected a Down event, if we should await the slop we move to the
             * next state. If we shouldn't await the slop and we already consumed the event we
             * dispatch the drag start events and start the dragging state.
             */
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.Yes) {
                moveToAwaitTouchSlopState(firstDown, firstDown.id)
            } else if (state.consumedOnInitial) {
                sendDragStart(
                    firstDown,
                    firstDown,
                    indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    Offset.Zero,
                )
                sendDragEvent(
                    firstDown,
                    indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    Offset.Zero,
                )
                moveToDraggingState(firstDown.id)
            }
        }
    }

    private fun processAwaitTouchSlop(
        indirectPointerInputEvent: IndirectTouchEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitTouchSlop,
    ) {
        /** Slop detection only cares about the main and final passes */
        if (pass == PointerEventPass.Initial) return
        val eventFromPointerId =
            indirectPointerInputEvent.changes.fastFirstOrNull { it.id == state.pointerId }

        /**
         * We lost this pointer, try to replace it. This is to cover the case where multiple
         * pointers were down, but the original one we tracked (state.pointerId) is no longer down,
         * try to move tracking to a different pointer
         */
        val dragEvent =
            if (eventFromPointerId == null) {
                val otherDown = indirectPointerInputEvent.changes.fastFirstOrNull { it.pressed }
                if (otherDown == null) {
                    // There are no other pointers down, reset the state
                    moveToAwaitDownState()
                    return
                } else {
                    // a new pointer was found, update the current state.
                    state.pointerId = otherDown.id
                }
                otherDown
            } else {
                eventFromPointerId
            }

        /**
         * Slop detection routines happens during the Main pass. Do we have unconsumed events for
         * this pointer?
         */
        if (pass == PointerEventPass.Main) {
            if (!dragEvent.isConsumed) {
                if (dragEvent.changedToUpIgnoreConsumed()) {
                    /** The pointer lifted, look for another pointer */
                    val otherDown = indirectPointerInputEvent.changes.fastFirstOrNull { it.pressed }
                    if (otherDown == null) {
                        // There are no other pointers down, reset the state
                        moveToAwaitDownState()
                    } else {
                        // a new pointer was found, update the current state.
                        state.pointerId = otherDown.id
                    }
                } else {
                    // this is a regular event (MOVE)
                    val touchSlop =
                        node.currentValueOf(LocalViewConfiguration).pointerSlop(PointerType.Touch)

                    // add data to the slop detector
                    val postSlopOffset =
                        requireTouchSlopDetector()
                            .addPositions(
                                dragEvent.primaryAxisPosition(
                                    node.orientationLock,
                                    indirectPointerInputEvent.primaryDirectionalMotionAxis,
                                ),
                                dragEvent.primaryAxisPreviousPosition(
                                    node.orientationLock,
                                    indirectPointerInputEvent.primaryDirectionalMotionAxis,
                                ),
                                touchSlop,
                            )

                    // slop was crossed, dispatch the drag start event and change to dragging state
                    if (postSlopOffset.isSpecified) {
                        dragEvent.consume()
                        sendDragStart(
                            state.initialDown!!,
                            dragEvent,
                            indirectPointerInputEvent.primaryDirectionalMotionAxis,
                            postSlopOffset,
                        )
                        sendDragEvent(
                            dragEvent,
                            indirectPointerInputEvent.primaryDirectionalMotionAxis,
                            postSlopOffset,
                        )
                        moveToDraggingState(dragEvent.id)
                    } else {
                        state.verifyConsumptionInFinalPass = true
                    }
                }
            } else {
                // This draggable "lost" the event as it was consumed by someone else, enter the
                // gesture pickup state if the feature is enabled.
                // Someone consumed this gesture, move this to the await pickup state.
                moveToAwaitGesturePickupState(
                    requireNotNull(state.initialDown) {
                        "AwaitTouchSlop.initialDown was not initialized"
                    },
                    state.pointerId,
                    requireNotNull(touchSlopDetector) {
                        "AwaitTouchSlop.touchSlopDetector was not initialized"
                    },
                )
            }
        }

        /**
         * This checks 2 cases: 1) A parent consumed in the main pass and this child can only see
         * that consumption during the final pass. 2) The parent actually consumed during the final
         * pass.
         */
        if (pass == PointerEventPass.Final && state.verifyConsumptionInFinalPass) {
            if (dragEvent.isConsumed) {
                // This draggable "lost" the event as it was consumed by someone else, enter the
                // gesture pickup state if the feature is enabled.
                // Someone consumed this gesture, move this to the await pickup state.
                moveToAwaitGesturePickupState(
                    requireNotNull(state.initialDown) {
                        "AwaitTouchSlop.initialDown was not initialized"
                    },
                    state.pointerId,
                    requireNotNull(touchSlopDetector) {
                        "AwaitTouchSlop.touchSlopDetector was not initialized"
                    },
                )
            } else {
                state.verifyConsumptionInFinalPass = false
            }
        }
    }

    private fun processAwaitGesturePickup(
        indirectPointerInputEvent: IndirectTouchEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitGesturePickup,
    ) {
        /**
         * Drag pickup only happens during the final pass so we're sure nobody else was interested
         * in this gesture.
         */
        if (pass != PointerEventPass.Final) return
        val hasUnconsumedDrag = indirectPointerInputEvent.changes.fastAll { !it.isConsumed }
        val hasDownPointers = indirectPointerInputEvent.changes.fastAny { it.pressed }
        // all pointers are up, reset
        if (!hasDownPointers || indirectPointerInputEvent.changes.isEmpty()) {
            moveToAwaitDownState()
        } else if (hasUnconsumedDrag) {
            // has pointers down with unconsumed events, a chance to pick up this gesture,
            // move to the touch slop detection phase
            val initialPositionChange =
                indirectPointerInputEvent.changes
                    .first()
                    .primaryAxisPosition(
                        node.orientationLock,
                        indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    ) -
                    state.initialDown!!.primaryAxisPosition(
                        node.orientationLock,
                        indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    )

            // await touch slop again, using the initial down as starting point.
            // For most cases this should return immediately since we probably moved
            // far enough from the initial down event.
            moveToAwaitTouchSlopState(
                requireNotNull(state.initialDown) {
                    "AwaitGesturePickup.initialDown was not initialized."
                },
                state.pointerId,
                initialPositionChange,
            )
        }
    }

    private fun processDraggingState(
        indirectPointerInputEvent: IndirectTouchEvent,
        pass: PointerEventPass,
        state: DragDetectionState.Dragging,
    ) {
        if (pass != PointerEventPass.Main) return

        val pointer = state.pointerId
        val dragEvent =
            indirectPointerInputEvent.changes.fastFirstOrNull { it.id == pointer } ?: return
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = indirectPointerInputEvent.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                if (!dragEvent.isConsumed && dragEvent.changedToUpIgnoreConsumed()) {
                    sendDragStopped(
                        dragEvent,
                        indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    )
                } else {
                    sendDragCancelled()
                }
                moveToAwaitDownState()
            } else {
                state.pointerId = otherDown.id
            }
        } else {
            if (dragEvent.isConsumed) {
                sendDragCancelled()
            } else {
                val positionChange =
                    dragEvent.positionChangeIgnoreConsumed(
                        node.orientationLock,
                        indirectPointerInputEvent.primaryDirectionalMotionAxis,
                    )

                /**
                 * During the gesture pickup we can pickup events at any direction so disable the
                 * orientation lock.
                 */
                val motionChange = positionChange.getDistance()
                if (motionChange != 0.0f) {
                    val positionChange =
                        dragEvent.positionChange(
                            node.orientationLock,
                            indirectPointerInputEvent.primaryDirectionalMotionAxis,
                        )
                    sendDragEvent(
                        dragEvent,
                        indirectPointerInputEvent.primaryDirectionalMotionAxis,
                        positionChange,
                    )
                    dragEvent.consume()
                }
            }
        }
    }

    private fun sendDragStart(
        down: IndirectPointerInputChange,
        slopTriggerChange: IndirectPointerInputChange,
        primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
        overSlopOffset: Offset,
    ) {
        if (velocityTracker == null) velocityTracker = VelocityTracker()
        nodeOffset = Offset.Zero // restart node offset
        requireVelocityTracker()
            .addIndirectTouchInputChange(
                down,
                node.orientationLock,
                primaryDirectionalMotionAxis,
                touchSmooth,
                nodeOffset,
            )
        val dragStartedOffset =
            slopTriggerChange.primaryAxisPosition(
                node.orientationLock,
                primaryDirectionalMotionAxis,
            ) - overSlopOffset
        // the drag start event offset is the down event + touch slop value
        // or in this case the event that triggered the touch slop minus
        // the post slop offset
        if (node.canDrag(PointerType.Touch)) {
            previousPositionOnScreen = node.requireLayoutCoordinates().positionOnScreen()
            node.onDragEvent(DragStarted(dragStartedOffset))
        }
        offsetSmoother.reset()
    }

    private fun sendDragEvent(
        change: IndirectPointerInputChange,
        primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
        dragAmount: Offset,
    ) {
        val currentPositionOnScreen = node.requireLayoutCoordinates().positionOnScreen()
        // container changed positions
        if (
            previousPositionOnScreen != Offset.Unspecified &&
                currentPositionOnScreen != previousPositionOnScreen
        ) {
            val delta = currentPositionOnScreen - previousPositionOnScreen
            nodeOffset += delta
        }
        previousPositionOnScreen = currentPositionOnScreen

        if (dragAmount.toFloat(node.orientationLock!!).absoluteValue > PixelSensibility) {
            requireVelocityTracker()
                .addIndirectTouchInputChange(
                    event = change,
                    node.orientationLock,
                    primaryDirectionalMotionAxis,
                    touchSmooth,
                    nodeOffset = nodeOffset,
                )
            node.onDragEvent(DragDelta(offsetSmoother.smoothEventPosition(dragAmount), true))
        }
    }

    private fun sendDragStopped(
        change: IndirectPointerInputChange,
        primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
    ) {
        requireVelocityTracker()
            .addIndirectTouchInputChange(
                change,
                node.orientationLock,
                primaryDirectionalMotionAxis,
                touchSmooth,
                nodeOffset,
            )
        val maximumVelocity = node.currentValueOf(LocalViewConfiguration).maximumFlingVelocity
        val velocity =
            requireVelocityTracker().calculateVelocity(Velocity(maximumVelocity, maximumVelocity))
        requireVelocityTracker().resetTracking()
        node.onDragEvent(DragStopped(velocity.toValidVelocity(), true))
    }

    private fun sendDragCancelled() {
        node.onDragEvent(DragCancelled)
    }

    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    sealed class DragDetectionState {
        /**
         * Starter state for any drag gesture cycle. At this state we're waiting for a Down event to
         * indicate that a drag gesture may start. Since drag gesture start at the initial pass we
         * have the option to indicate if we consumed the event during the initial pass using
         * [consumedOnInitial]. We also save the [awaitTouchSlop] between passes so we don't call
         * the [DragGestureNode.startDragImmediately] as often.
         */
        class AwaitDown(
            var awaitTouchSlop: AwaitTouchSlop = AwaitTouchSlop.NotInitialized,
            var consumedOnInitial: Boolean = false,
        ) : DragDetectionState() {

            enum class AwaitTouchSlop {
                Yes,
                No,
                NotInitialized,
            }
        }

        /**
         * If drag should wait for touch slop, after the initial down recognition we move to this
         * state. Here we will collect drag events until touch slop is crossed.
         */
        class AwaitTouchSlop(
            var initialDown: IndirectPointerInputChange? = null,
            var pointerId: PointerId = PointerId(Long.MAX_VALUE),
            var verifyConsumptionInFinalPass: Boolean = false,
        ) : DragDetectionState()

        /**
         * Alternative state that implements the gesture pick up feature. If a draggable loses an
         * event because someone else consumed it, it can still pick it up later if the consumer
         * "gives up" on that gesture. Once a gesture is lost the draggable will pass on to this
         * state until all fingers are up.
         */
        class AwaitGesturePickup(
            var initialDown: IndirectPointerInputChange? = null,
            var pointerId: PointerId = PointerId(Long.MAX_VALUE),
            var touchSlopDetector: TouchSlopDetector? = null,
        ) : DragDetectionState()

        /** State where dragging is happening. */
        class Dragging(var pointerId: PointerId = PointerId(Long.MAX_VALUE)) : DragDetectionState()
    }
}

// these should probably go into IndirectPointerInputChange in UI
@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.positionChange(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
) = this.positionChangeInternal(orientation, primaryDirectionalMotionAxis, false)

@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.positionChangeIgnoreConsumed(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
) = this.positionChangeInternal(orientation, primaryDirectionalMotionAxis, true)

@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.changedToUpIgnoreConsumed() = previousPressed && !pressed

@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.changedToDown() = !isConsumed && !previousPressed && pressed

@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.changedToDownIgnoreConsumed() = !previousPressed && pressed

@ExperimentalIndirectTouchTypeApi
private fun IndirectPointerInputChange.positionChangeInternal(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
    ignoreConsumed: Boolean = false,
): Offset {
    val previousPosition = primaryAxisPreviousPosition(orientation, primaryDirectionalMotionAxis)
    val currentPosition = primaryAxisPosition(orientation, primaryDirectionalMotionAxis)

    val offset = currentPosition - previousPosition

    return if (!ignoreConsumed && isConsumed) {
        Offset.Zero
    } else {
        offset
    }
}

/**
 * Returns a modified position for this [IndirectTouchEvent] accounting for
 * [IndirectTouchEvent.primaryDirectionalMotionAxis]. When we no longer need to smooth positions, we
 * should instead only use the primary axis to resolve delta changes, as changing the entire event
 * in this way will affect the start position we report to onDragStarted. Until we can remove
 * smoothing logic, it's complicated to manage primary axis as well as smoothed positions, so we
 * just make the change here for simplicity.
 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
private fun IndirectPointerInputChange.primaryAxisPosition(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
): Offset {
    if (orientation == null) return position
    val delta =
        when (primaryDirectionalMotionAxis) {
            IndirectTouchEventPrimaryDirectionalMotionAxis.X -> position.x
            IndirectTouchEventPrimaryDirectionalMotionAxis.Y -> position.y
            // No primary axis, so don't change the offset
            else -> return position
        }
    return if (orientation == Orientation.Horizontal) {
        Offset(x = delta, y = 0f)
    } else {
        Offset(x = 0f, y = delta)
    }
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
private fun Offset.primaryAxisPosition(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
): Offset {
    if (orientation == null) return this
    val delta =
        when (primaryDirectionalMotionAxis) {
            IndirectTouchEventPrimaryDirectionalMotionAxis.X -> x
            IndirectTouchEventPrimaryDirectionalMotionAxis.Y -> y
            // No primary axis, so don't change the offset
            else -> return this
        }
    return if (orientation == Orientation.Horizontal) {
        Offset(x = delta, y = 0f)
    } else {
        Offset(x = 0f, y = delta)
    }
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
private fun IndirectPointerInputChange.primaryAxisPreviousPosition(
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
): Offset {
    if (orientation == null) return previousPosition
    val delta =
        when (primaryDirectionalMotionAxis) {
            IndirectTouchEventPrimaryDirectionalMotionAxis.X -> previousPosition.x
            IndirectTouchEventPrimaryDirectionalMotionAxis.Y -> previousPosition.y
            // No primary axis, so don't change the offset
            else -> return previousPosition
        }
    return if (orientation == Orientation.Horizontal) {
        Offset(x = delta, y = 0f)
    } else {
        Offset(x = 0f, y = delta)
    }
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
private fun VelocityTracker.addIndirectTouchInputChange(
    event: IndirectPointerInputChange,
    orientation: Orientation?,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis?,
    smoother: TouchInputEventSmoother,
    nodeOffset: Offset,
) {
    val smoothedPosition =
        smoother
            .smoothEventPosition(event)
            .primaryAxisPosition(orientation, primaryDirectionalMotionAxis)
    addPosition(event.uptimeMillis, smoothedPosition + nodeOffset)
}

// TODO(levima) Remove once ExperimentalIndirectTouchTypeApi stable b/426155641
/**
 * Smoothes touch input events that are too frequent and noisy
 *
 * TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently.
 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal class TouchInputEventSmoother() {
    private var eventRotatingIndex = 0
    private var eventRotatingArray = mutableListOf<IndirectPointerInputChange>()

    fun smoothEventPosition(change: IndirectPointerInputChange): Offset {

        var xPosition = change.position.x
        var yPosition = change.position.y

        if (change.changedToDownIgnoreConsumed()) {
            eventRotatingIndex = 0
            eventRotatingArray.clear()
        }

        if (!change.changedToUpIgnoreConsumed() && !change.changedToDownIgnoreConsumed()) {
            if (eventRotatingArray.size == SmoothingFactor) {
                eventRotatingArray[eventRotatingIndex++] = change
            } else {
                eventRotatingArray.add(change)
            }

            if (eventRotatingIndex == SmoothingFactor) {
                eventRotatingIndex = 0
            }
            xPosition = eventRotatingArray.fastMap { it.position.x }.average().toFloat()
            yPosition = eventRotatingArray.fastMap { it.position.y }.average().toFloat()
        }

        return Offset(xPosition, yPosition)
    }

    /**
     * TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently.
     */
    companion object {
        private const val SmoothingFactor = 3
    }
}

@Suppress("PrimitiveInCollection")
internal class OffsetSmoother() {
    private var eventRotatingIndex = 0
    private var eventRotatingArray = mutableListOf<Offset>()

    fun smoothEventPosition(offset: Offset): Offset {
        if (eventRotatingArray.size == SmoothingFactor) {
            eventRotatingArray[eventRotatingIndex++] = offset
        } else {
            eventRotatingArray.add(offset)
        }

        if (eventRotatingIndex == SmoothingFactor) {
            eventRotatingIndex = 0
        }
        val xPosition: Float = eventRotatingArray.fastMap { it.x }.average().toFloat()
        val yPosition: Float = eventRotatingArray.fastMap { it.y }.average().toFloat()

        return Offset(xPosition, yPosition)
    }

    fun reset() {
        eventRotatingIndex = 0
        eventRotatingArray.clear()
    }
}

/** TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently. */
private const val SmoothingFactor = 3
private const val PixelSensibility = 2
