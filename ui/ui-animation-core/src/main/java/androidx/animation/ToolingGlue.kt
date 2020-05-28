/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.animation

import androidx.annotation.RestrictTo

/**
 * Seekable animation class provides utilities to create an animation using a state pair, and
 * supports querying animation values based on a specific time. This class is designed to be
 * entirely stateless in terms of animation lifecycle. This design makes it easy for higher level
 * stateful construct to be built on top of it.
 *
 * This API is intended for tools' use only. Hence the @RestrictTo.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SeekableAnimation<T>(
    val def: TransitionDefinition<T>,
    fromState: T,
    toState: T
) {
    private val currentValues: MutableMap<PropKey<Any, AnimationVector>, Any> = mutableMapOf()
    private val currentAnimWrappers: MutableMap<PropKey<Any, AnimationVector>,
            AnimationWrapper<Any, AnimationVector>> = mutableMapOf()
    private val toState = def.states[toState]!!
    private val fromState = def.states[fromState]!!

    init {
        currentValues.putAll(this.fromState.props)
        val transSpec: TransitionSpec<T> = def.getSpec(fromState, toState)
        // Initialize currentAnimWrappers
        for ((prop, _) in this.toState.props) {
            currentAnimWrappers[prop] = prop.createAnimationWrapper(
                transSpec.getAnimationForProp(prop), this.fromState[prop], null,
                this.toState[prop]
            )
        }
    }

    /**
     * Duration of the animation. When there are multiple properties being animated in a
     * transition, this will be the duration of the longest running animation.
     */
    val duration: Long =
            currentAnimWrappers.asSequence().map { it.value.durationMillis }.max()!!

    /**
     * Returns the animation values at the given playtime. This time could be any time between 0
     * and duration, where 0 means the beginning of the animation.
     *
     * @param playTime animation play time in [0, duration]
     */
    fun getAnimValuesAt(playTime: Long): Map<PropKey<Any, AnimationVector>, Any> {
        if (playTime <= 0) {
            currentValues.putAll(fromState.props)
        } else if (playTime >= duration) {
            currentValues.putAll(toState.props)
        } else {
            for ((prop, animation) in currentAnimWrappers) {
                currentValues[prop] = animation.getValue(playTime)
            }
        }
        return currentValues
    }
}

/**
 * Creates a [SeekableAnimation] using the same [TransitionDefinition] that the
 * [TransitionAnimation] is created from.
 *
 * Note: This API is intended for tools' use only. Hence the @RestrictTo.
 *
 * @param fromState The state that a [SeekableAnimation] will start from.
 * @param toState The state that a [SeekableAnimation] will end in.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> TransitionAnimation<T>.createSeekableAnimation(fromState: T, toState: T) =
    SeekableAnimation<T>(def, fromState, toState)

/**
 * Returns the all states available in a [TransitionDefinition].
 *
 * This API is intended for tools' use only. Hence the @RestrictTo.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> TransitionAnimation<T>.getStates(): Set<T> = def.states.keys