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

import kotlin.math.sign

/**
 * Stateless wrapper around a (target based, or decay) animation, that caches the start value and
 * velocity, and target value for target based animations. This wrapper is purely for the
 * convenience for 1) not having to pass in the same static set of values for each query, 2) not
 * needing to distinguish target-based or decay animations at the call site.
 */
internal interface AnimationWrapper<T, V : AnimationVector> {
    fun getValue(playTime: Long): T
    fun getVelocity(playTime: Long): V
    fun isFinished(playTime: Long): Boolean {
        return playTime >= durationMillis
    }
    val durationMillis: Long
}

/**
 * This is a custom animation wrapper for all target based animations, i.e. animations that have a
 * target value defined. All the values that don't change throughout the animation, such as start
 * value, end value, start velocity, and interpolator are cached in this wrapper. So once
 * the wrapper is setup, the getValue/Velocity calls should only need to provide the changing input
 * into the animation, i.e. play time.
 */
internal class TargetBasedAnimationWrapper<T, V : AnimationVector>(
    startValue: T,
    private val startVelocity: V,
    val endValue: T,
    private val animation: Animation<V>,
    private val typeConverter: TwoWayConverter<T, V>
) : AnimationWrapper<T, V> {
    private val startValueVector = typeConverter.convertToVector.invoke(startValue)
    private val endValueVector = typeConverter.convertToVector.invoke(endValue)

    override fun getValue(playTime: Long): T {
        return if (playTime < durationMillis) {
            typeConverter.convertFromVector.invoke(
                animation.getValue(
                    playTime, startValueVector,
                    endValueVector, startVelocity
                )
            )
        } else {
            endValue
        }
    }

    override val durationMillis: Long = animation.getDurationMillis(
        start = startValueVector,
        end = endValueVector,
        startVelocity = startVelocity
    )

    private val endVelocity = animation.getEndVelocity(
        startValueVector,
        endValueVector,
        startVelocity,
        durationMillis
    )

    override fun getVelocity(playTime: Long): V {
        return if (playTime < durationMillis) {
            animation.getVelocity(
                playTime,
                startValueVector,
                endValueVector,
                startVelocity
            )
        } else {
            endVelocity
        }
    }
}

/**
 * Decay animation wrapper contains a decay animation as well as the animations values that remain
 * the same throughout the animation: start value/velocity.
 */
internal class DecayAnimationWrapper(
    private val startValue: Float,
    private val startVelocity: Float = 0f,
    private val anim: DecayAnimation
) : AnimationWrapper<Float, AnimationVector1D> {
    private val target: Float = anim.getTarget(startValue, startVelocity)
    private val velocityVector: AnimationVector1D = AnimationVector1D(0f)

    override fun getValue(playTime: Long): Float {
        if (!isFinished(playTime)) {
            return anim.getValue(playTime, startValue, startVelocity)
        } else {
            return target
        }
    }

    override fun getVelocity(playTime: Long): AnimationVector1D {
        if (!isFinished(playTime)) {
            velocityVector.value = anim.getVelocity(playTime, startValue, startVelocity)
        } else {
            velocityVector.value = anim.absVelocityThreshold * sign(startVelocity)
        }
        return velocityVector
    }

    override val durationMillis: Long = anim.getDurationMillis(startValue, startVelocity)
}