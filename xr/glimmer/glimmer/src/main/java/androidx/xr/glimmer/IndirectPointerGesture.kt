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
 * A [Modifier] that listens for and detects high-level gestures from an [IndirectPointerEvent]
 * source. The component (or one of its descendants) using this modifier **must be focused** to
 * intercept and process indirect pointer events.
 *
 * This modifier is designed to be used near the top of the composable hierarchy to handle gestures.
 *
 * @sample androidx.xr.glimmer.samples.OnIndirectPointerGestureSample
 * @param enabled Controls whether gesture detection is active. When `false`, this modifier has no
 *   effect and no callbacks will be invoked.
 * @param onClick Invoked when a successful click is detected.
 * @param onSwipeForward Invoked when a successful forward swipe is detected.
 * @param onSwipeBackward Invoked when a successful backward swipe is detected.
 */
public fun Modifier.onIndirectPointerGesture(
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onSwipeForward: () -> Unit = {},
    onSwipeBackward: () -> Unit = {},
): Modifier =
    this then
        IndirectPointerGestureElement(
            enabled = enabled,
            onClick = onClick,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
        )

private class IndirectPointerGestureElement(
    private val enabled: Boolean,
    private val onClick: () -> Unit,
    private val onSwipeForward: () -> Unit,
    private val onSwipeBackward: () -> Unit,
) : ModifierNodeElement<IndirectPointerGestureNode>() {

    override fun create(): IndirectPointerGestureNode =
        IndirectPointerGestureNode(
            enabled = enabled,
            onClick = onClick,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
        )

    override fun update(node: IndirectPointerGestureNode) {
        node.update(
            enabled = enabled,
            onClick = onClick,
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectPointerGestureElement) return false

        if (enabled != other.enabled) return false
        if (onClick !== other.onClick) return false
        if (onSwipeForward !== other.onSwipeForward) return false
        if (onSwipeBackward !== other.onSwipeBackward) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onClick.hashCode()
        result = 31 * result + onSwipeForward.hashCode()
        result = 31 * result + onSwipeBackward.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onIndirectPointerGesture"
        properties["enabled"] = enabled
        properties["onClick"] = onClick
        properties["onSwipeForward"] = onSwipeForward
        properties["onSwipeBackward"] = onSwipeBackward
    }
}

private class IndirectPointerGestureNode(
    private var enabled: Boolean,
    private var onClick: () -> Unit,
    private var onSwipeForward: () -> Unit,
    private var onSwipeBackward: () -> Unit,
) : IndirectPointerInputModifierNode, CompositionLocalConsumerModifierNode, Modifier.Node() {

    private var pointerId: PointerId = PointerId(UnassignedPointerId)
    private var initialPosition = Offset.Unspecified
    private var isPointerInClickRegion = false
    private var previousValidPositionX = 0f
    private var totalHorizontalDistanceTraveled = 0f

    private var velocityTracker: VelocityTracker? = null

    fun update(
        enabled: Boolean,
        onClick: () -> Unit,
        onSwipeForward: () -> Unit,
        onSwipeBackward: () -> Unit,
    ) {
        if (this.enabled != enabled) {
            resetGestureState()
        }
        this.enabled = enabled
        this.onClick = onClick
        this.onSwipeForward = onSwipeForward
        this.onSwipeBackward = onSwipeBackward
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        // TODO(b/446641623): Temporary usage of Initial, use PointerEventPass.Main instead.
        if (!enabled || pass != PointerEventPass.Initial) {
            return
        }

        var anyConsumed = false

        event.changes.fastForEach { change ->
            if (change.isConsumed) {
                return@fastForEach
            }

            // TODO(b/446865091): Consume every event due to no modifier available to disable input
            // for descendants
            change.consume()
            anyConsumed = true

            if (pointerId.value == UnassignedPointerId) {
                if (!change.pressed) {
                    // This change already has an 'up' action, so we can ignore it.
                    return@fastForEach
                }
                pointerId = change.id
            }

            if (pointerId != change.id) {
                // This 'change' is for a different pointer than the one we're tracking.
                return@fastForEach // Continue to the next change
            }

            if (velocityTracker == null) velocityTracker = VelocityTracker()

            requireVelocityTracker().addPosition(change.uptimeMillis, change.position)

            handleInputChange(change)
        }

        if (!anyConsumed) {
            resetGestureState()
        }
    }

    override fun onCancelIndirectPointerInput() {
        resetGestureState()
    }

    private fun handleInputChange(inputChange: IndirectPointerInputChange) {
        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        val touchSlopSquared = touchSlop * touchSlop

        if (inputChange.changedToDownIgnoreConsumed()) {
            initialPosition = inputChange.position
            isPointerInClickRegion = true
            previousValidPositionX = inputChange.position.x
        } else if (inputChange.isMoveIgnoreConsumed()) {
            if (initialPosition == Offset.Unspecified) {
                resetGestureState()
                return
            }

            // This is a 'move' event.
            totalHorizontalDistanceTraveled += abs(inputChange.position.x - previousValidPositionX)
            if (isPointerInClickRegion) {
                val displacementFromInitial = inputChange.position - initialPosition
                if (displacementFromInitial.getDistanceSquared() > touchSlopSquared) {
                    // We've moved outside the click region.
                    isPointerInClickRegion = false
                }
            }
            previousValidPositionX = inputChange.position.x
        } else if (inputChange.changedToUpIgnoreConsumed()) {
            if (initialPosition == Offset.Unspecified) {
                resetGestureState()
                return
            }

            val swipeDistanceThresholdPx = touchSlop * TouchSlopToSwipeDistanceThresholdRatio
            val finalHorizontalDisplacement = inputChange.position.x - initialPosition.x

            if (abs(finalHorizontalDisplacement) > swipeDistanceThresholdPx) {
                // We've moved enough to be considered a swipe but not a click.
                val horizontalVelocity = requireVelocityTracker().calculateVelocity().x
                totalHorizontalDistanceTraveled +=
                    abs(inputChange.position.x - previousValidPositionX)
                if (
                    abs(horizontalVelocity) >= SwipeVelocityThresholdPxPerSec &&
                        !isSwipeBacktracking(
                            touchSlop,
                            totalDistanceTraveled = totalHorizontalDistanceTraveled,
                            displacement = finalHorizontalDisplacement,
                        )
                ) {
                    // It's a valid swipe (no backtrack) and it's fast enough.
                    if (finalHorizontalDisplacement < 0) {
                        onSwipeBackward()
                    } else if (finalHorizontalDisplacement > 0) {
                        onSwipeForward()
                    }
                }
            } else if (isPointerInClickRegion) {
                // It's a click
                onClick()
            }

            resetGestureState()
        } else {
            // Unknown state
            resetGestureState()
        }
    }

    private fun resetGestureState() {
        pointerId = PointerId(UnassignedPointerId)
        initialPosition = Offset.Unspecified
        isPointerInClickRegion = false
        previousValidPositionX = 0f
        totalHorizontalDistanceTraveled = 0f
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

    private fun requireVelocityTracker(): VelocityTracker =
        requireNotNull(velocityTracker) { "Velocity Tracker not initialized." }

    private fun IndirectPointerInputChange.changedToDownIgnoreConsumed() =
        !previousPressed && pressed

    private fun IndirectPointerInputChange.isMoveIgnoreConsumed() = previousPressed && pressed

    private fun IndirectPointerInputChange.changedToUpIgnoreConsumed() = previousPressed && !pressed

    companion object {
        private const val UnassignedPointerId = -1L
        // TODO(b/446216019): Hardcoded constants for now. Use them from ViewConfiguration.
        private const val SwipeVelocityThresholdPxPerSec = 100f
        // A swipe must be longer than a scroll to be recognized. This value is multiplied by the
        // system's touch slop, ensuring that a user's intent to scroll isn't interpreted as swipe.
        private const val TouchSlopToSwipeDistanceThresholdRatio = 1.3f
    }
}
