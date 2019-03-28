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

package androidx.animation

import androidx.animation.Physics.Companion.DampingRatioNoBouncy
import androidx.animation.Physics.Companion.StiffnessVeryLow

abstract class AnimationBuilder<T> {
    internal abstract fun build(): Animation<T>
}

/**
 * The default duration used in [Animation]s.
 */
const val DefaultDuration: Int = 300

/**
 * [KeyframesBuilder] creates a [Keyframes] animation.
 * [Keyframes] animation based on the values defined at different timestamps in
 * the duration of the animation (i.e. different keyframes). Each keyframe can be defined using
 * [at]. [Keyframes] allows very specific animation definitions with a precision to millisecond.
 *
 * The following sample creates a [Keyframes] animation for a float property.
 * `
 * keyframes {
 *      duration = 375
 *      0f at 0 // ms  // Optional
 *      0.4f at 75 // ms
 *      0.4f at 225 // ms
 *      0f at 375 // ms  // Optional
 * }
 * `
 * // TODO: support different easing for each keyframe interval
 */
class KeyframesBuilder<T> : AnimationBuilder<T>() {

    /**
     * Duration of Keyframes animation in milliseconds. Defaults to [DefaultDuration]
     */
    var duration: Int = DefaultDuration
        set(value) {
            if (value < 0) {
                throw IllegalStateException("Duration shouldn't be negative")
            }
            field = value
        }

    private val keyframes = mutableMapOf<Long, T>()

    /**
     * Adds a keyframe so that animation value will be [this] at time: [timeStamp]
     *
     * @param timeStamp The time in the during when animation should reach value: [this]
     */
    infix fun T.at(timeStamp: Int) {
        if (timeStamp >= 0) {
            keyframes[timeStamp.toLong()] = this
        } else {
            // TODO: adding a timestamp < 0 should cause a compile time error
            throw IllegalArgumentException("Time cannot be negative.")
        }
    }

    override fun build(): Animation<T> =
        Keyframes(duration.toLong(), keyframes)
}

class TweenBuilder<T> : AnimationBuilder<T>() {

    /**
     * Duration of Keyframes animation in milliseconds. Defaults to [DefaultDuration]
     */
    var duration: Int = DefaultDuration
        set(value) {
            if (value < 0) {
                throw IllegalStateException("Duration shouldn't be negative")
            }
            field = value
        }

    /**
     * The amount of time that the animation should be delayed.
     */
    var delay: Long = 0
        set(value) {
            if (value < 0) {
                throw IllegalStateException("Delay shouldn't be negative")
            }
            field = value
        }

    /**
     * Easing (a.k.a interpolator) for the Tween animation.
     * Default: [FastOutSlowInEasing]
     */
    var easing: Easing = FastOutSlowInEasing

    override fun build(): Animation<T> =
        Tween(duration.toLong(), delay, easing)
}

open class PhysicsBuilder<T> : AnimationBuilder<T>() {

    /**
     * Damping ratio of the spring. Defaults to [DampingRatioNoBouncy]
     */
    var dampingRatio = DampingRatioNoBouncy

    /**
     * Stiffness of the spring. Defaults to [StiffnessVeryLow]
     */
    var stiffness = StiffnessVeryLow

    override fun build(): Animation<T> =
        Physics(dampingRatio, stiffness)
}
