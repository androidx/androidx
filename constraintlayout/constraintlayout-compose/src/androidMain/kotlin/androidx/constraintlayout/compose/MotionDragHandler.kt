/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

/**
 * Helper modifier for MotionLayout to support OnSwipe in Transitions.
 *
 * @see Modifier.pointerInput
 * @see TransitionHandler
 */
internal fun Modifier.motionPointerInput(
    key: Any,
    motionProgress: MutableFloatState,
    measurer: MotionMeasurer
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "motionPointerInput"
        properties["key"] = key
        properties["motionProgress"] = motionProgress
        properties["measurer"] = measurer
    }
) {
    if (!measurer.transition.hasOnSwipe()) {
        return@composed this
    }
    val swipeHandler = remember(key) {
        TransitionHandler(motionMeasurer = measurer, motionProgress = motionProgress)
    }
    val dragChannel = remember(key) { Channel<MotionDragState>(Channel.CONFLATED) }

    LaunchedEffect(key1 = key) effectScope@{
        var isTouchUp = false
        var dragState: MotionDragState? = null
        while (coroutineContext.isActive) {
            if (isTouchUp && swipeHandler.pendingProgressWhileTouchUp()) {
                // Loop until there's no need to update the progress or the there's a touch down
                swipeHandler.updateProgressWhileTouchUp()
            } else {
                if (dragState == null) {
                    dragState = dragChannel.receive()
                }
                coroutineContext.ensureActive()
                isTouchUp = !dragState.isDragging
                if (isTouchUp) {
                    swipeHandler.onTouchUp(velocity = dragState.velocity)
                } else {
                    swipeHandler.updateProgressOnDrag(dragAmount = dragState.dragAmount)
                }
                dragState = null
            }

            // To be able to interrupt the free-form progress of 'isUp', check if there's another
            // dragState that initiated a new drag
            val channelResult = dragChannel.tryReceive()
            if (channelResult.isSuccess) {
                val receivedState = channelResult.getOrThrow()
                if (receivedState.isDragging) {
                    // If another drag is initiated, switching 'isUp' interrupts the
                    // 'getTouchUpProgress' loop
                    isTouchUp = false
                }
                // Just save the received state, don't 'consume' it
                dragState = receivedState
            }
        }
    }
    return@composed this.pointerInput(key) {
        val velocityTracker = VelocityTracker()
        detectDragGesturesWhenNeeded(
            onAcceptFirstDown = { offset ->
                swipeHandler.onAcceptFirstDownForOnSwipe(offset)
            },
            onDragStart = { _ ->
                velocityTracker.resetTracking()
            },
            onDragEnd = {
                dragChannel.trySend(
                    // Indicate that the swipe has ended, MotionLayout should animate the rest.
                    MotionDragState.onDragEnd(velocityTracker.calculateVelocity())
                )
            },
            onDragCancel = {
                dragChannel.trySend(
                    // Indicate that the swipe has ended, MotionLayout should animate the rest.
                    MotionDragState.onDragEnd(velocityTracker.calculateVelocity())
                )
            },
            onDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)
                // As dragging is done, pass the dragAmount to update the MotionLayout progress.
                dragChannel.trySend(MotionDragState.onDrag(dragAmount))
            }
        )
    }
}

/**
 * Data class with the relevant values of a touch input event used for OnSwipe support.
 */
internal data class MotionDragState(
    val isDragging: Boolean,
    val dragAmount: Offset,
    val velocity: Velocity
) {
    companion object {

        fun onDrag(dragAmount: Offset) =
            MotionDragState(
                isDragging = true,
                dragAmount = dragAmount,
                velocity = Velocity.Zero
            )

        fun onDragEnd(velocity: Velocity) =
            MotionDragState(
                isDragging = false,
                dragAmount = Offset.Unspecified,
                velocity = velocity
            )
    }
}

/**
 * Copy of [androidx.compose.foundation.gestures.detectDragGestures] with the opportunity to decide
 * whether we consume the rest of the drag with [onAcceptFirstDown].
 */
@SuppressLint("PrimitiveInLambda")
private suspend fun PointerInputScope.detectDragGesturesWhenNeeded(
    onAcceptFirstDown: (Offset) -> Boolean,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        if (!onAcceptFirstDown(down.position)) {
            return@awaitEachGesture
        }
        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        do {
            drag = awaitTouchSlopOrCancellation(
                pointerId = down.id
            ) { change, over ->
                change.consume()
                overSlop = over
            }
        } while (drag != null && !drag.isConsumed)
        if (drag != null) {
            onDragStart.invoke(drag.position)
            onDrag(drag, overSlop)
            if (
                !drag(drag.id) {
                    onDrag(it, it.positionChange())
                    it.consume()
                }
            ) {
                onDragCancel()
            } else {
                onDragEnd()
            }
        }
    }
}
