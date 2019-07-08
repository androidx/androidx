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
 * Used as a iterations count for [RepeatableBuilder] to create an infinity repeating animation.
 */
const val Infinite: Int = Int.MAX_VALUE

/**
 * [KeyframesBuilder] creates a [Keyframes] animation.
 * [Keyframes] animation based on the values defined at different timestamps in
 * the duration of the animation (i.e. different keyframes). Each keyframe can be defined using
 * [at]. [Keyframes] allows very specific animation definitions with a precision to millisecond.
 *
 * The following sample creates a [Keyframes] animation for a float property.
 *     keyframes {
 *         duration = 375
 *         0f at 0 // ms  // Optional
 *         0.4f at 75 // ms
 *         0.4f at 225 // ms
 *         0f at 375 // ms  // Optional
 *     }
 *
 * You can also provide a custom [Easing] for the interval with use of [with] function applied
 * for the interval starting keyframe. In this sample [FastOutSlowInEasing] is added for
 * the interval from 0 to 100 ms.
 *     keyframes {
 *         duration = 100
 *         0f at 0 with FastOutSlowInEasing
 *         1f at 100
 *     }
 */
class KeyframesBuilder<T> : DurationBasedAnimationBuilder<T>() {

    private val keyframes = mutableMapOf<Long, KeyframeEntity>()

    /**
     * Adds a keyframe so that animation value will be [this] at time: [timeStamp]
     *
     * @param timeStamp The time in the during when animation should reach value: [this]
     * @return an [KeyframeEntity] so a custom [Easing] can be added by [with] method.
     */
    infix fun T.at(timeStamp: Int): KeyframeEntity {
        return if (timeStamp >= 0) {
            KeyframeEntity(this).also {
                keyframes[timeStamp.toLong()] = it
            }
        } else {
            // TODO: adding a timestamp < 0 should cause a compile time error
            throw IllegalArgumentException("Time cannot be negative.")
        }
    }

    /**
     * Adds an [Easing] for the interval started with the just provided timestamp.
     *
     * The following sample adds a custom Easing for the interval from 0 to 100 ms:
     *     keyframes {
     *         duration = 100
     *         0f at 0 with FastOutSlowInEasing
     *         1f at 100
     *     }
     *
     * @param easing [Easing] to be used for the next interval.
     */
    infix fun KeyframeEntity.with(easing: Easing) {
        this.easing = easing
    }

    override fun build(): DurationBasedAnimation<T> =
        Keyframes(duration.toLong(), delay.toLong(), keyframes.mapValues { it.value.toPair() })

    /**
     * Holder class for building a keyframes animation.
     */
    inner class KeyframeEntity internal constructor(
        internal val value: T,
        internal var easing: Easing = LinearEasing
    ) {
        internal fun toPair() = value to easing
    }
}

/**
 * Used for creating repeated animations where each iteration is defined by one of
 * the duration based animations like [TweenBuilder] or [KeyframesBuilder].
 */
class RepeatableBuilder<T> : AnimationBuilder<T>() {

    /**
     * The count of iterations. Can't be less then 1. Use [Infinite] to
     * have an infinity repeating animation.
     */
    var iterations: Int? = null
        set(value) {
            if (value != null && value < 1) {
                throw IllegalStateException("Iterations count can't be less than 1")
            }
            field = value
        }

    /**
     * Use [TransitionSpec.tween] or [TransitionSpec.keyframes] as a specification
     * for the animation iteration.
     */
    var animation: DurationBasedAnimationBuilder<T>? = null

    override fun build(): Animation<T> {
        val iterationsCount = iterations?.toLong()
            ?: throw IllegalStateException("The iterations count should be provided")
        val animation = animation
            ?: throw IllegalStateException("The animation should be provided")
        return Repeatable(
            iterationsCount,
            animation.build()
        )
    }
}

/**
 * Base class for an [AnimationBuilder] to create animations based on a fixed duration.
 */
abstract class DurationBasedAnimationBuilder<T> : AnimationBuilder<T>() {

    /**
     * Duration of the animation in milliseconds. Defaults to [DefaultDuration]
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
    var delay: Int = 0
        set(value) {
            if (value < 0) {
                throw IllegalStateException("Delay shouldn't be negative")
            }
            field = value
        }

    abstract override fun build(): DurationBasedAnimation<T>
}

class TweenBuilder<T> : DurationBasedAnimationBuilder<T>() {

    /**
     * Easing (a.k.a interpolator) for the Tween animation.
     * Default: [FastOutSlowInEasing]
     */
    var easing: Easing = FastOutSlowInEasing

    override fun build(): DurationBasedAnimation<T> =
        Tween(duration.toLong(), delay.toLong(), easing)
}

/**
 * PhysicsBuilder takes in the configuration of a spring as its constructor parameters.
 *
 * @param dampingRatio Damping ratio of the spring. Defaults to [DampingRatioNoBouncy]
 * @param stiffness Stiffness of the spring. Defaults to [StiffnessVeryLow]
 */
open class PhysicsBuilder<T>(
    var dampingRatio: Float = DampingRatioNoBouncy,
    var stiffness: Float = StiffnessVeryLow
) : AnimationBuilder<T>() {

    override fun build(): Animation<T> =
        Physics(dampingRatio, stiffness)
}

/**
 * Builds Snap animation for immediately switching the animating value to the end value.
 */
class SnapBuilder<T> : DurationBasedAnimationBuilder<T>() {

    override fun build(): DurationBasedAnimation<T> = Snap()
}
