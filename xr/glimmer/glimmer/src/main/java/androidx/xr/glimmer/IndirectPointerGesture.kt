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

package androidx.xr.glimmer

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.indirect.nativeEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * A [Modifier] that detects high-level click and horizontal swipe gestures from an
 * [IndirectPointerEvent] source. The component (or one of its descendants) using this modifier
 * **must be focused** to intercept events.
 *
 * This modifier allows optionality for swipe gesture callbacks. If a specific swipe gesture
 * callback is `null`, the corresponding swipe events will not be consumed by this modifier. For
 * example:
 * - When nesting `onIndirectPointerGesture` modifiers, if the inner modifier provides an
 *   [onSwipeBackward] callback but leaves [onSwipeForward] as `null`, the outer modifier can still
 *   detect and handle the forward swipe, and vice versa.
 *
 * Note that the initial `down` event is always consumed by this modifier (if not already consumed)
 * as long as at least one callback ([onClick], [onSwipeForward], or [onSwipeBackward]) is provided.
 *
 * @sample androidx.xr.glimmer.samples.OnIndirectPointerGestureSample
 * @param enabled Controls whether gesture detection is active. When `false`, this modifier has no
 *   effect and no callbacks will be invoked.
 * @param onSwipeForward Invoked when a successful forward swipe is detected.
 * @param onSwipeBackward Invoked when a successful backward swipe is detected.
 * @param onClick Invoked when a successful click is detected.
 */
public fun Modifier.onIndirectPointerGesture(
    enabled: Boolean = true,
    onSwipeForward: (() -> Unit)? = null,
    onSwipeBackward: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
): Modifier =
    this then
        IndirectPointerGestureElement(
            enabled = enabled,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
            onClick = onClick,
        )

private class IndirectPointerGestureElement(
    private val enabled: Boolean,
    private val onSwipeForward: (() -> Unit)?,
    private val onSwipeBackward: (() -> Unit)?,
    private val onClick: (() -> Unit)?,
) : ModifierNodeElement<IndirectPointerGestureNode>() {

    override fun create(): IndirectPointerGestureNode =
        IndirectPointerGestureNode(
            enabled = enabled,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
            onClick = onClick,
        )

    override fun update(node: IndirectPointerGestureNode) {
        node.update(
            enabled = enabled,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
            onClick = onClick,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectPointerGestureElement) return false

        if (enabled != other.enabled) return false
        if (onSwipeForward !== other.onSwipeForward) return false
        if (onSwipeBackward !== other.onSwipeBackward) return false
        if (onClick !== other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + (onSwipeForward?.hashCode() ?: 0)
        result = 31 * result + (onSwipeBackward?.hashCode() ?: 0)
        result = 31 * result + (onClick?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onIndirectPointerGesture"
        properties["enabled"] = enabled
        properties["onSwipeForward"] = onSwipeForward
        properties["onSwipeBackward"] = onSwipeBackward
        properties["onClick"] = onClick
    }
}

private class IndirectPointerGestureNode(
    private var enabled: Boolean,
    private var onSwipeForward: (() -> Unit)?,
    private var onSwipeBackward: (() -> Unit)?,
    private var onClick: (() -> Unit)?,
) : IndirectPointerInputModifierNode, CompositionLocalConsumerModifierNode, Modifier.Node() {

    private var pointerId: PointerId = PointerId(UnassignedPointerId)
    private var initialPosition = Offset.Unspecified
    private var ignoreClickForGestureStream = false
    private var ignoreSwipeForGestureStream = false
    private var previousValidPositionX = 0f
    private var totalHorizontalDistanceTraveled = 0f

    private var velocityTracker: VelocityTracker? = null

    fun update(
        enabled: Boolean,
        onSwipeForward: (() -> Unit)?,
        onSwipeBackward: (() -> Unit)?,
        onClick: (() -> Unit)?,
    ) {
        val hasNoCallbacks = onClick == null && onSwipeForward == null && onSwipeBackward == null
        if (this.enabled != enabled || hasNoCallbacks) {
            resetGestureState()
        }
        this.enabled = enabled
        this.onSwipeForward = onSwipeForward
        this.onSwipeBackward = onSwipeBackward
        this.onClick = onClick
    }

    override fun onDetach() {
        super.onDetach()
        resetGestureState()
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        if (!enabled || pass != PointerEventPass.Main) {
            return
        }

        if (onClick == null && onSwipeForward == null && onSwipeBackward == null) {
            return
        }

        var isTrackedPointerInEvent = false

        event.changes.fastForEach { change ->
            if (pointerId.value == UnassignedPointerId) {
                if (!change.changedToDownIgnoreConsumed()) {
                    // Gesture stream has not been tracked from the initial down event.
                    return@fastForEach
                }
                pointerId = change.id
            }

            if (pointerId != change.id) {
                // This 'change' is for a different pointer than the one we're tracking.
                return@fastForEach
            }

            isTrackedPointerInEvent = true

            if (
                (onSwipeForward != null || onSwipeBackward != null) && !ignoreSwipeForGestureStream
            ) {
                // TODO(b/526962189): Use `VelocityTracker#addIndirectPointerInputChange` to
                // calculate the velocity instead of providing historical events manually
                val nativeEvent = event.nativeEvent
                val pointerIndex = nativeEvent.findPointerIndex(pointerId.value.toInt())
                if (pointerIndex >= 0) {
                    val historySize = nativeEvent.historySize
                    for (historicalEventIndex in 0 until historySize) {
                        getVelocityTracker()
                            .addPosition(
                                nativeEvent.getHistoricalEventTime(historicalEventIndex),
                                Offset(
                                    nativeEvent.getHistoricalX(pointerIndex, historicalEventIndex),
                                    nativeEvent.getHistoricalY(pointerIndex, historicalEventIndex),
                                ),
                            )
                    }
                }
                getVelocityTracker().addPosition(change.uptimeMillis, change.position)
            }
            handleInputChange(change)
        }

        if (!isTrackedPointerInEvent) {
            resetGestureState()
        }
    }

    override fun onCancelIndirectPointerInput() {
        resetGestureState()
    }

    private fun handleInputChange(inputChange: IndirectPointerInputChange) {
        when {
            inputChange.changedToDownIgnoreConsumed() -> handleDownIgnoreConsumed(inputChange)
            inputChange.isMovingIgnoreConsumed() -> handleMoveIgnoreConsumed(inputChange)
            inputChange.changedToUp() -> handleUp(inputChange)
            else -> resetGestureState()
        }
    }

    private fun handleDownIgnoreConsumed(inputChange: IndirectPointerInputChange) {
        if (!inputChange.isConsumed) {
            inputChange.consume()
        } else {
            // If the down event is consumed by a child, we don't want to trigger a click.
            // However, we continue tracking the pointer's movement to determine if it resolves
            // into a swipe for the current onIndirectPointerGesture.
            ignoreClickForGestureStream = true
        }
        initialPosition = inputChange.position
        previousValidPositionX = inputChange.position.x
    }

    private fun handleMoveIgnoreConsumed(inputChange: IndirectPointerInputChange) {
        if (inputChange.isConsumed) {
            // If a move event is consumed, should not trigger a swipe.
            ignoreSwipeForGestureStream = true
        }

        totalHorizontalDistanceTraveled += abs(inputChange.position.x - previousValidPositionX)

        val displacementFromInitial = inputChange.position - initialPosition

        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        val touchSlopSquared = touchSlop * touchSlop

        if (
            !ignoreSwipeForGestureStream &&
                isSwipeBacktracking(
                    touchSlop,
                    totalDistanceTraveled = totalHorizontalDistanceTraveled,
                    displacement = displacementFromInitial.x,
                )
        ) {
            // The pointer has backtracked beyond the touch slop threshold. Stop tracking swipe for
            // the remainder of this gesture.
            ignoreSwipeForGestureStream = true
        }

        if (displacementFromInitial.getDistanceSquared() > touchSlopSquared) {
            // We've moved outside the click region.
            ignoreClickForGestureStream = true

            if (!ignoreSwipeForGestureStream) {
                // Pointer has moved enough to be a swipe, and the swipe is still valid.
                if (onSwipeBackward != null && displacementFromInitial.x < 0) {
                    inputChange.consume()
                } else if (onSwipeForward != null && displacementFromInitial.x > 0) {
                    inputChange.consume()
                }
            }
        }

        previousValidPositionX = inputChange.position.x
    }

    private fun handleUp(inputChange: IndirectPointerInputChange) {
        if (!ignoreClickForGestureStream) {
            onClick?.let {
                inputChange.consume()
                it()
            }
        } else if (!ignoreSwipeForGestureStream) {
            val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
            val swipeDistanceThresholdPx = touchSlop * TouchSlopToSwipeDistanceThresholdRatio
            val finalHorizontalDisplacement = inputChange.position.x - initialPosition.x

            if (abs(finalHorizontalDisplacement) > swipeDistanceThresholdPx) {
                // We've moved enough to be considered a swipe but not a click.
                val horizontalVelocity = getVelocityTracker().calculateVelocity().x
                if (abs(horizontalVelocity) >= SwipeVelocityThresholdPxPerSec) {
                    // It's a valid swipe (no backtrack) and it's fast enough.
                    val swipeCallback =
                        if (finalHorizontalDisplacement < 0) onSwipeBackward else onSwipeForward
                    swipeCallback?.let {
                        inputChange.consume()
                        it()
                    }
                }
            }
        }
        resetGestureState()
    }

    private fun resetGestureState() {
        pointerId = PointerId(UnassignedPointerId)
        initialPosition = Offset.Unspecified
        previousValidPositionX = 0f
        totalHorizontalDistanceTraveled = 0f
        ignoreClickForGestureStream = false
        ignoreSwipeForGestureStream = false
        velocityTracker?.resetTracking()
    }

    // Checks if the pointer moved significantly in the opposite direction
    private fun isSwipeBacktracking(
        backtrackingThreshold: Float,
        totalDistanceTraveled: Float,
        displacement: Float,
    ): Boolean {
        return abs(totalDistanceTraveled - abs(displacement)) > backtrackingThreshold
    }

    private fun getVelocityTracker(): VelocityTracker {
        return velocityTracker ?: VelocityTracker().also { velocityTracker = it }
    }

    private fun IndirectPointerInputChange.changedToDownIgnoreConsumed() =
        !previousPressed && pressed

    private fun IndirectPointerInputChange.isMovingIgnoreConsumed() = previousPressed && pressed

    private fun IndirectPointerInputChange.changedToUp() =
        !isConsumed && previousPressed && !pressed

    companion object {
        private const val UnassignedPointerId = -1L
        // TODO(b/446216019): Hardcoded constants for now. Use them from ViewConfiguration.
        private const val SwipeVelocityThresholdPxPerSec = 34f
        // A swipe must be longer than a scroll to be recognized. This value is multiplied by the
        // system's touch slop, ensuring that a user's intent to scroll isn't interpreted as swipe.
        private const val TouchSlopToSwipeDistanceThresholdRatio = 1.3f
    }
}
