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

package androidx.ui.material.internal

import androidx.animation.AnimatedFloat
import androidx.animation.AnimationBuilder
import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationEndReason
import androidx.animation.Spring
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.foundation.InteractionState
import androidx.ui.foundation.animation.AnchorsFlingConfig
import androidx.ui.foundation.animation.fling
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.draggable
import androidx.ui.util.fastFirstOrNull

/**
 * Enable automatic drag and animation between predefined states.
 *
 * This can be used for example in a Switch to enable dragging between two states (true and
 * false). Additionally, it will animate correctly when the value of the state parameter is changed.
 *
 * Additional features compared to [draggable]:
 * 1. [onNewValue] provides the developer with the new value every time drag or animation (caused
 * by fling or [state] change) occurs. The developer needs to hold this state on their own
 * 2. When the anchor is reached, [onStateChange] will be called with state mapped to this anchor
 * 3. When the anchor is reached and [onStateChange] with corresponding state is called, but
 * call site didn't update state to the reached one for some reason,
 * this component performs rollback to the previous (correct) state.
 * 4. When new [state] is provided, component will be animated to state's anchor
 *
 * @param T type with which state is represented
 * @param state current state to represent Float value with
 * @param onStateChange callback to update call site's state
 * @param anchorsToState pairs of anchors to states to map anchors to state and vise versa
 * @param animationBuilder animation which will be used for animations
 * @param dragDirection direction in which drag should be happening.
 * Either [DragDirection.Vertical] or [DragDirection.Horizontal]
 * @param enabled whether or not this Draggable is enabled and should consume events
 * @param minValue lower bound for draggable value in this component
 * @param maxValue upper bound for draggable value in this component
 * @param onNewValue callback to update state that the developer owns when animation or drag occurs
 */
// TODO(malkov/tianliu) (figure our how to make it better and make public)
internal fun <T> Modifier.stateDraggable(
    state: T,
    onStateChange: (T) -> Unit,
    anchorsToState: List<Pair<Float, T>>,
    animationBuilder: AnimationBuilder<Float>,
    dragDirection: DragDirection,
    enabled: Boolean = true,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    interactionState: InteractionState? = null,
    onNewValue: (Float) -> Unit
) = composed {
    val forceAnimationCheck = state { true }

    val anchors = remember(anchorsToState) { anchorsToState.map { it.first } }
    val currentValue = anchorsToState.fastFirstOrNull { it.second == state }!!.first
    val flingConfig =
        AnchorsFlingConfig(anchors, animationBuilder, onAnimationEnd = { reason, finalValue, _ ->
            if (reason != AnimationEndReason.Interrupted) {
                val newState = anchorsToState.firstOrNull { it.first == finalValue }?.second
                if (newState != null && newState != state) {
                    onStateChange(newState)
                    forceAnimationCheck.value = !forceAnimationCheck.value
                }
            }
        })
    val clocks = AnimationClockAmbient.current
    val position = remember(clocks) {
        onNewValue(currentValue)
        NotificationBasedAnimatedFloat(currentValue, clocks, onNewValue)
    }
    position.onNewValue = onNewValue
    position.setBounds(minValue, maxValue)

    // This state is to force this component to be recomposed and trigger onCommit below
    // This is needed to stay in sync with drag state that caller side holds
    onCommit(currentValue, forceAnimationCheck.value) {
        position.animateTo(currentValue, animationBuilder)
    }
    Modifier.draggable(
        dragDirection = dragDirection,
        onDragDeltaConsumptionRequested = { delta ->
            val old = position.value
            position.snapTo(position.value + delta)
            position.value - old
        },
        onDragStopped = { position.fling(flingConfig, it) },
        enabled = enabled,
        startDragImmediately = position.isRunning,
        interactionState = interactionState
    )
}

private class NotificationBasedAnimatedFloat(
    initial: Float,
    clock: AnimationClockObservable,
    internal var onNewValue: (Float) -> Unit
) : AnimatedFloat(clock, Spring.DefaultDisplacementThreshold) {

    override var value = initial
        set(value) {
            onNewValue(value)
            field = value
        }
}