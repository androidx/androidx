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
import androidx.animation.ValueHolder
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.memo
import androidx.compose.onCommit
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.foundation.animation.AnchorsFlingConfig
import androidx.ui.foundation.animation.AnimatedFloatDragController
import androidx.ui.foundation.animation.FlingConfig
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.DraggableCallback
import androidx.ui.lerp

/**
 * High-level version of [Draggable] that allows you to write Draggable that can be dragged
 * predeterminated set states which are represented as anchors.
 *
 * Alongside with control that [Draggable] provides, this component ensures that:
 * 1. The AnimatedFloat value will be in sync with call site state
 * 2. When the anchor is reached, [onStateChange] will be called with state mapped to this anchor
 * 3. When the anchor is reached and [onStateChange] with corresponding state is called, but
 * call site didn't update state to the reached one for some reason,
 * this component performs rollback to the previous (correct) state.
 * 4. When new [state] is provided, component will be animated to state's anchor
 *
 * children of this composable will receive [ValueModel] class from which
 * they can read current value when they need.
 *
 * @param T type with which state is represented
 * @param state current state to represent Float value with
 * @param onStateChange callback to update call site's state
 * @param anchorsToState pairs of anchors to states to map anchors to state and vise versa
 * @param animationBuilder animation which will be used for animations
 * @param dragDirection direction in which drag should be happening.
 * Either [DragDirection.Vertical] or [DragDirection.Horizontal]
 * @param minValue lower bound for draggable value in this component
 * @param maxValue upper bound for draggable value in this component
 * @param enabled whether or not this Draggable is enabled and should consume events
 */
// TODO(malkov/tianliu) (figure our how to make it better and make public)
@Composable
internal fun <T> StateDraggable(
    state: T,
    onStateChange: (T) -> Unit,
    anchorsToState: List<Pair<Float, T>>,
    animationBuilder: AnimationBuilder<Float>,
    dragDirection: DragDirection,
    enabled: Boolean = true,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    children: @Composable() (ValueModel) -> Unit
) {
    val anchors = +memo(anchorsToState) { anchorsToState.map { it.first } }
    val currentValue = anchorsToState.first { it.second == state }.first

    // This state is to force this component to be recomposed and trigger +onCommit below
    // This is needed to stay in sync with drag state that caller side holds
    val forceAnimationCheck = +state { true }
    val callback = DraggableCallback(onDragSettled = { finalValue ->
        val newState = anchorsToState.firstOrNull { it.first == finalValue }?.second
        if (newState != null && newState != state) {
            onStateChange(newState)
            forceAnimationCheck.value = !forceAnimationCheck.value
        }
    })

    val model = +memo { ValueModel(currentValue) }
    val controller = +memo(anchorsToState, animationBuilder) {
        NonRecomposeDragController(
            currentValue,
            { model.value = it },
            AnchorsFlingConfig(anchors, animationBuilder)
        )
    }
    +onCommit(currentValue, forceAnimationCheck.value) {
        controller.animatedFloat.animateTo(currentValue, animationBuilder)
    }
    controller.enabled = enabled
    Draggable(
        dragDirection = dragDirection,
        minValue = minValue,
        maxValue = maxValue,
        valueController = controller,
        callback = callback
    ) {
        children(model)
    }
}

private fun NonRecomposeDragController(
    initialValue: Float,
    onValueChanged: (Float) -> Unit,
    flingConfig: FlingConfig? = null
) = AnimatedFloatDragController(
    AnimatedFloat(ListeneableValueHolder(initialValue, onValueChanged)),
    flingConfig
)

private class ListeneableValueHolder(
    var current: Float,
    val onValueChanged: (Float) -> Unit
) : ValueHolder<Float> {
    override val interpolator: (start: Float, end: Float, fraction: Float) -> Float = ::lerp
    override var value: Float
        get() = current
        set(value) {
            current = value
            onValueChanged(value)
        }
}

// TODO: unify it later
@Model
internal class ValueModel(var value: Float)