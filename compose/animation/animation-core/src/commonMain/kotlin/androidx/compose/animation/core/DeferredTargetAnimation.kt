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

package androidx.compose.animation.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@RequiresOptIn(
    message = "This is an experimental animation API for Transition. It may change in the future."
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAnimatableApi

/**
 * [DeferredTargetAnimation] is intended for animations where the target is unknown at the time
 * of instantiation. Such use cases include, but are not limited to, size or position animations
 * created during composition or the initialization of a Modifier.Node, yet the target size or
 * position stays unknown until the later measure and placement phase.
 *
 * [DeferredTargetAnimation] offers a declarative [updateTarget] function, which requires a
 * target to either set up the animation or update the animation, and to read the current value
 * of the animation.
 *
 * @sample androidx.compose.animation.core.samples.DeferredTargetAnimationSample
 */
@ExperimentalAnimatableApi
class DeferredTargetAnimation<T, V : AnimationVector>(
    private val vectorConverter: TwoWayConverter<T, V>
) {
    /**
     * Returns the target value from the most recent [updateTarget] call.
     */
    val pendingTarget: T?
        get() = _pendingTarget

    private var _pendingTarget: T? by mutableStateOf(null)
    private val target: T?
        get() = animatable?.targetValue
    private var animatable: Animatable<T, V>? = null

    /**
     * [updateTarget] sets up an animation, or updates an already running animation, based on the
     * [target] in the given [coroutineScope]. [pendingTarget] will be updated to track the last
     * seen [target].
     *
     * [updateTarget] will return the current value of the animation after launching the animation
     * in the given [coroutineScope].
     *
     * @return current value of the animation
     */
    fun updateTarget(
        target: T,
        coroutineScope: CoroutineScope,
        animationSpec: FiniteAnimationSpec<T> = spring()
    ): T {
        _pendingTarget = target
        val anim = animatable ?: Animatable(target, vectorConverter).also { animatable = it }
        coroutineScope.launch {
            if (anim.targetValue != _pendingTarget) {
                anim.animateTo(target, animationSpec)
            }
        }
        return anim.value
    }

    /**
     * [isIdle] returns true when the animation has finished running and reached its
     * [pendingTarget], or when the animation has not been set up (i.e. [updateTarget] has never
     * been called).
     */
    val isIdle: Boolean
        get() = _pendingTarget == target && animatable?.isRunning != true
}
