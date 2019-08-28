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

import androidx.animation.AnimationBuilder
import androidx.compose.composer
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.onCommit
import androidx.compose.state
import androidx.ui.foundation.gestures.Draggable
import androidx.ui.foundation.animation.AnchorsFlingConfig
import androidx.ui.foundation.animation.AnimatedFloatDragController
import androidx.ui.foundation.gestures.DraggableCallback

/**
 * Effect to create special version if [AnimatedFloatDragController] that maps
 * float anchors to states
 *
 * Alongside with control that [AnimatedFloatDragController] provides, this component ensures that:
 * 1. The AnimatedFloat value will be in sync with call site state
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
 * @return pair of controller and callback to pass to [Draggable].
 */
// TODO(malkov/tianliu) (figure our how to make it better and make public)
internal fun <T> anchoredControllerByState(
    state: T,
    onStateChange: (T) -> Unit,
    anchorsToState: List<Pair<Float, T>>,
    animationBuilder: AnimationBuilder<Float>
) = effectOf<Pair<AnimatedFloatDragController, DraggableCallback>> {
    val anchors = +memo(anchorsToState) { anchorsToState.map { it.first } }
    val currentValue = anchorsToState.firstOrNull { it.second == state }!!.first

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

    val controller = +memo(anchorsToState, animationBuilder) {
        AnimatedFloatDragController(currentValue, AnchorsFlingConfig(anchors, animationBuilder))
    }
    +onCommit(currentValue, forceAnimationCheck.value) {
        controller.animatedFloat.animateTo(currentValue, animationBuilder)
    }
    controller to callback
}