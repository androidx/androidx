/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

@ExperimentalSharedTransitionApi
internal class BoundsAnimation(
    val transitionScope: SharedTransitionScope,
    val transition: Transition<Boolean>,
    animation: Transition<Boolean>.DeferredAnimation<Rect, AnimationVector4D>,
    boundsTransform: BoundsTransform
) {
    var animation: Transition<Boolean>.DeferredAnimation<Rect, AnimationVector4D> by
        mutableStateOf(animation)
        private set

    fun updateAnimation(
        animation: Transition<Boolean>.DeferredAnimation<Rect, AnimationVector4D>,
        boundsTransform: BoundsTransform,
    ) {
        if (this.animation != animation) {
            this.animation = animation
            animationState = null
            animationSpec = DefaultBoundsAnimation
        }
        this.boundsTransform = boundsTransform
    }

    private var boundsTransform: BoundsTransform by mutableStateOf(boundsTransform)

    val isRunning: Boolean
        get() {
            var parent: Transition<*> = transition
            while (parent.parentTransition != null) {
                parent = parent.parentTransition!!
            }
            return parent.currentState != parent.targetState
        }

    var animationSpec: FiniteAnimationSpec<Rect> = DefaultBoundsAnimation

    // It's important to back this state up by a mutable state, so that whoever read it when
    // it was null will get an invalidation when it's set.
    var animationState: State<Rect>? by mutableStateOf(null)
    val value: Rect?
        get() =
            if (transitionScope.isTransitionActive) {
                animationState?.value
            } else {
                null
            }

    fun animate(currentBounds: Rect, targetBounds: Rect) {
        if (transitionScope.isTransitionActive) {
            if (animationState == null) {
                // Only invoke bounds transform when animation is initialized. This means
                // boundsTransform will not participate in interruption-handling animations.
                animationSpec = boundsTransform.transform(currentBounds, targetBounds)
            }
            animationState =
                animation.animate(transitionSpec = { animationSpec }) {
                    if (it == transition.targetState) {
                        // its own bounds
                        targetBounds
                    } else {
                        currentBounds
                    }
                }
        }
    }

    val target: Boolean
        get() = transition.targetState
}

private val DefaultBoundsAnimation =
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Rect.VisibilityThreshold)
