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
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.inputDeviceBottom
import androidx.compose.ui.test.inputDeviceLeft
import androidx.compose.ui.test.inputDeviceRight
import androidx.compose.ui.test.inputDeviceTop
import androidx.compose.ui.test.inputDeviceTopLeft
import androidx.compose.ui.test.inputDeviceTopRight
import androidx.compose.ui.test.sendIndirectPointerInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.IntSize

// Horizontal external indirect pointer input device
internal val horizontalExternalInputDeviceSize = IntSize(3082, 616)

// Vertical external indirect pointer input device
internal val verticalExternalInputDeviceSize = IntSize(616, 3082)

// Square external indirect pointer input device
internal val squareExternalInputDeviceSize = IntSize(3082, 3082)

internal const val defaultPeriodBetweenEventsMillis = 16L

// Use the two below together to trigger swipe in foundation.
internal const val defaultStepCountForMoveToVelocityTrigger: Int = 10
internal const val defaultDelayForMoveToVelocityTrigger: Long = 10

internal const val defaultDelayForIncrementedMove: Long = 200L

// Provides standard glimmer defaults to simplify calls
fun SemanticsNodeInteractionsProvider.sendIndirectPointerInput(
    indirectPointerEventPrimaryDirectionalMotionAxis:
        IndirectPointerEventPrimaryDirectionalMotionAxis =
        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
    inputDeviceSize: IntSize = horizontalExternalInputDeviceSize,
    block: IndirectPointerInjectionScope.() -> Unit,
) {
    sendIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            indirectPointerEventPrimaryDirectionalMotionAxis,
        inputDeviceSize = inputDeviceSize,
        block = block,
    )
}

// Swipe along the x-axis with only one move.
internal fun SemanticsNodeInteractionsProvider.oneMoveSwipeAlongXAxis(
    xDistance: Float,
    moveDuration: Long = defaultDelayForIncrementedMove,
    inputDeviceSize: IntSize = horizontalExternalInputDeviceSize,
) {
    sendIndirectPointerInput(inputDeviceSize = inputDeviceSize) {
        val start =
            if (xDistance >= 0f) {
                inputDeviceTopLeft
            } else {
                inputDeviceTopRight
            }

        // This gesture is specifically designed to properly create a simple, valid swipe gesture.
        // Thus, the capping logic (as not to trigger out of physical bounds error).
        val end = xDistance.coerceIn(-inputDeviceTopRight.x, inputDeviceTopRight.x)

        down(start)
        moveBy(Offset(end, 0f), moveDuration)
        advanceEventTime(defaultPeriodBetweenEventsMillis)
        up()
    }
}

// Moves the distance over a number of steps (evenly divided per step).
internal fun IndirectPointerInjectionScope.evenlyDividedMoveX(
    distance: Float,
    steps: Int = defaultStepCountForMoveToVelocityTrigger,
    duration: Long = defaultDelayForIncrementedMove,
) {
    val distancePerStep = distance / steps
    val durationPerStep = duration / steps
    repeat(steps) { moveBy(delta = Offset(distancePerStep, 0f), delayMillis = durationPerStep) }
}

internal fun IndirectPointerInjectionScope.swipeUp() {
    swipeUp(inputDeviceBottom, inputDeviceTop)
}

internal fun IndirectPointerInjectionScope.swipeDown() {
    swipeDown(inputDeviceTop, inputDeviceBottom)
}

internal fun IndirectPointerInjectionScope.swipeLeft() {
    swipeLeft(inputDeviceRight, inputDeviceLeft)
}

internal fun IndirectPointerInjectionScope.swipeRight() {
    swipeRight(inputDeviceLeft, inputDeviceRight)
}

/**
 * Create a modifier for testing indirect pointer input. For production use, use foundation gestures
 * detectors or make your own Modifier.Node implementing [IndirectPointerInputModifierNode], see
 * [IndirectPointerInputNode] below for details.
 *
 * @param onEvent A callback that is invoked when an indirect pointer event is received.
 * @param onCancel A callback that is invoked when the pointer input is cancelled.
 */
internal fun Modifier.onIndirectPointerInput(
    onEvent: (event: IndirectPointerEvent, pass: PointerEventPass) -> Unit,
    onCancel: () -> Unit = {},
): Modifier = this.then(IndirectPointerInputElement(onEvent, onCancel))

internal class IndirectPointerInputElement(
    val onEvent: (IndirectPointerEvent, PointerEventPass) -> Unit,
    val onCancel: () -> Unit,
) : ModifierNodeElement<IndirectPointerInputNode>() {

    override fun create(): IndirectPointerInputNode {
        return IndirectPointerInputNode(onEvent, onCancel)
    }

    override fun update(node: IndirectPointerInputNode) {
        node.onEvent = onEvent
        node.onCancel = onCancel
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indirectPointerInput"
        properties["onEvent"] = onEvent
        properties["onCancel"] = onCancel
    }

    override fun hashCode(): Int {
        var result = onEvent.hashCode()
        result = 31 * result + onCancel.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndirectPointerInputElement) return false
        if (onEvent !== other.onEvent) return false
        if (onCancel !== other.onCancel) return false
        return true
    }
}

/**
 * A [Modifier.Node] that can be used to test indirect pointer events. This is a very simple version
 * that doesn't track state (which you would need for production).
 */
internal class IndirectPointerInputNode(
    var onEvent: (IndirectPointerEvent, PointerEventPass) -> Unit,
    var onCancel: () -> Unit,
) : IndirectPointerInputModifierNode, Modifier.Node() {
    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        onEvent(event, pass)
    }

    override fun onCancelIndirectPointerInput() {
        onCancel()
    }
}

internal fun IndirectPointerEvent.consumeMatchingPress(pointerIdToMatch: Long): Boolean {
    var consumed = false
    val pointerId = PointerId(pointerIdToMatch)
    if (type == IndirectPointerEventType.Press) {
        val matchingChange = changes.find { it.id == pointerId }
        matchingChange?.let { nonNullMatchingChange ->
            // Verifies that the selected change was in a press
            if (nonNullMatchingChange.pressed && !nonNullMatchingChange.previousPressed) {
                changes.forEach { it.consume() }
                consumed = true
            }
        }
    }
    return consumed
}

internal fun IndirectPointerEvent.consumeMatchingMove(pointerIdToMatch: Long): Boolean {
    var consumed = false
    val pointerId = PointerId(pointerIdToMatch)
    if (type == IndirectPointerEventType.Move) {
        val matchingChange = changes.find { it.id == pointerId }
        matchingChange?.let { nonNullMatchingChange ->
            // Verifies that the selected change was a move
            if (nonNullMatchingChange.pressed) {
                changes.forEach { it.consume() }
                consumed = true
            }
        }
    }
    return consumed
}

internal fun IndirectPointerEvent.consumeMatchingRelease(pointerIdToMatch: Long): Boolean {
    var consumed = false
    val pointerId = PointerId(pointerIdToMatch)
    if (type == IndirectPointerEventType.Release) {
        val matchingChange = changes.find { it.id == pointerId }
        matchingChange?.let { nonNullMatchingChange ->
            // Verifies that the selected change was a release
            if (!nonNullMatchingChange.pressed && nonNullMatchingChange.previousPressed) {
                changes.forEach { it.consume() }
                consumed = true
            }
        }
    }
    return consumed
}
