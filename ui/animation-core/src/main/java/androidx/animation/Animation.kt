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

import android.animation.TimeInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.ui.lerp
import java.lang.IllegalArgumentException

const val DEBUG = false

/**
 * This animation class is intended to be stateless. Once they are configured, they know how to
 * calculate animation values at any given time, when provided with start/end values and velocity.
 * It is stateless in that it doesn't manage its own lifecycle: it doesn't know when it started, or
 * should finish. It only reacts to the given playtime (i.e. time elapsed since the start of the
 * animation). It also doesn't anticipate the input play time to be in any sort of sequence.
 * The intended use is to query [Animation] objects on what the animation value would be.This
 * design makes it straightforward to coordinate different animations, and they are more testable.
 * In our specific use case, this design also has the added benefit of reusing the same default
 * animation to createAnimation all properties.
 */
// TODO: Use Duration or TimeStamp for playtime once they are inlined.
sealed class Animation<T> {
    internal abstract fun isFinished(
        playTime: Long,
        start: T,
        end: T,
        startVelocity: Float = 0f
    ): Boolean

    internal abstract fun getVelocity(
        playTime: Long,
        start: T,
        end: T,
        startVelocity: Float = 0f
    ): Float

    internal abstract fun getValue(
        playTime: Long,
        start: T,
        end: T,
        startVelocity: Float = 0f,
        valueInterpolator: (T, T, Float) -> T
    ): T
}

/**
 * [Keyframes] class manages the animation based on the values defined at different timestamps in
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
open class Keyframes<T : Any> : Animation<T>() {
    /**
     * Duration of Keyframes animation in milliseconds. Defaults to 300
     */
    var duration = 300
        set(value) {
            if (value >= 0) {
                field = value
            }
        }
    private val keyframes = mutableMapOf<Int, T>()

    /**
     * Adds a keyframe so that animation value will be [this] at time: [timeStamp]
     *
     * @param timeStamp The time in the during when animation should reach value: [this]
     */
    infix fun T.at(timeStamp: Int) {
        if (timeStamp >= 0) {
            keyframes[timeStamp] = this
        } else {
            // TODO: adding a timestamp < 0 should cause a compile time error
            throw IllegalArgumentException("Time cannot be negative.")
        }
    }

    /****** Below are internal functions. ****/

    override fun isFinished(playTime: Long, start: T, end: T, startVelocity: Float): Boolean {
        return playTime >= duration
    }

    override fun getVelocity(playTime: Long, start: T, end: T, startVelocity: Float): Float {
        return 0f
    }

    override fun getValue(
        playTime: Long,
        start: T,
        end: T,
        startVelocity: Float,
        valueInterpolator: (T, T, Float) -> T
    ): T {
        // Find the range where playtime fits
        val playTime: Int = playTime.toInt().coerceIn(0, duration)
        if (keyframes.containsKey(playTime)) {
            return keyframes[playTime]!!
        }

        var startTime = 0
        var startVal = start
        var endVal = end
        var endTime: Int = duration
        for ((timestamp, value) in keyframes) {
            if (playTime > timestamp && timestamp > startTime) {
                startTime = timestamp
                startVal = value
            } else if (playTime < timestamp && timestamp < endTime) {
                endTime = timestamp
                endVal = value
            }
        }

        // Now interpolate
        val fraction = (playTime - startTime) / (endTime - startTime).toFloat()
        return valueInterpolator(startVal, endVal, fraction)
    }
}

// TODO: Come up with a better way to separate the float value special handling from the generic
// types, and not expose this as a public class
class FloatKeyframes : Keyframes<Float>() {
    override fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        val currentValue = getValue(playTime, start, end, startVelocity, ::lerp)
        val previousValue = getValue(playTime - 1, start, end, startVelocity, ::lerp)
        return (currentValue - previousValue) / 1000f
    }
}

/**
 * [Tween] is responsible for animating from one value to another using a provided easing curve,
 * or the default [AccelerateDecelerateInterpolator] when none is specified. The duration for such
 * an animation can be adjusted via [duration] from its default value of 300 milliseconds.
 */
class Tween : Animation<Any>() {

    /**
     * Duration of [Tween] animations in milliseconds. Defaults to 300
     */
    var duration = 300
    /**
     * Interpolator (a.k.a easing curve) for the [Tween] animation.
     * Default: [AccelerateDecelerateInterpolator]
     */
    var interpolator: TimeInterpolator = AccelerateDecelerateInterpolator()
    /**
     * The amount of time that the animation should be delayed.
     */
    var delay: Long = 0
        set(value) {
            if (value >= 0) {
                field = value
            }
        }

    private fun getInterpolation(playTime: Long): Float {
        if (playTime < delay) {
            return 0f
        }
        return interpolator.getInterpolation((playTime - delay) / duration.toFloat())
    }

    override fun isFinished(playTime: Long, start: Any, end: Any, startVelocity: Float): Boolean {
        return playTime >= (duration + delay)
    }

    // Velocity, unit: /s
    override fun getVelocity(playTime: Long, start: Any, end: Any, startVelocity: Float): Float {
        var playTime = playTime - delay
        return (getInterpolation(playTime) - getInterpolation(playTime - 1)) * 1000
    }

    override fun getValue(
        playTime: Long,
        start: Any,
        end: Any,
        startVelocity: Float,
        valueInterpolator: (Any, Any, Float) -> Any
    ): Any {
        return valueInterpolator.invoke(start, end, getInterpolation(playTime))
    }
}

/**
 * [Physics] animation is in its core a spring animation. It is the default animation that the
 * animation system uses to createAnimation from [TransitionState] to [TransitionState] when no
 * animations are specified. Its configuration can be tuned via adjusting the spring parameters,
 * namely [dampingRatio] and [stiffness]. By default, [Physics] animation uses a spring with
 * [dampingRatio] = [DampingRatioNoBouncy]  and [stiffness] = [StiffnessVeryLow].
 */
class Physics : Animation<Any>() {

    companion object {
        /**
         * Stiffness constant for extremely stiff spring
         */
        const val StiffnessHigh = 10_000f
        /**
         * Stiffness constant for medium stiff spring. This is the default stiffness for spring force.
         */
        const val StiffnessMedium = 1500f
        /**
         * Stiffness constant for a spring with low stiffness.
         */
        const val StiffnessLow = 200f
        /**
         * Stiffness constant for a spring with very low stiffness.
         */
        const val StiffnessVeryLow = 50f

        /**
         * Damping ratio for a very bouncy spring. Note for under-damped springs
         * (i.e. damping ratio < 1), the lower the damping ratio, the more bouncy the spring.
         */
        const val DampingRatioHighBouncy = 0.2f
        /**
         * Damping ratio for a medium bouncy spring. This is also the default damping ratio for spring
         * force. Note for under-damped springs (i.e. damping ratio < 1), the lower the damping ratio,
         * the more bouncy the spring.
         */
        const val DampingRatioMediumBouncy = 0.5f
        /**
         * Damping ratio for a spring with low bounciness. Note for under-damped springs
         * (i.e. damping ratio < 1), the lower the damping ratio, the higher the bounciness.
         */
        const val DampingRatioLowBouncy = 0.75f
        /**
         * Damping ratio for a spring with no bounciness. This damping ratio will create a critically
         * damped spring that returns to equilibrium within the shortest amount of time without
         * oscillating.
         */
        const val DampingRatioNoBouncy = 1f
    }

    /**
     * Damping ratio of the spring. Defaults to [DampingRatioNoBouncy]
     */
    var dampingRatio = DampingRatioNoBouncy
        set(value) {
            field = value
            spring.dampingRatio = value
        }
    /**
     * Stiffness of the spring. Defaults to [StiffnessVeryLow]
     */
    var stiffness = StiffnessVeryLow
        set(value) {
            field = value
            spring.stiffness = value
        }
    private val spring = SpringSimulation(1f)

    override fun getValue(
        playTime: Long,
        start: Any,
        end: Any,
        startVelocity: Float,
        valueInterpolator: (Any, Any, Float) -> Any
    ): Any {
        if (start is Float && end is Float) {
            spring.finalPosition = end
            val (value, _) = spring.updateValues(start, startVelocity, playTime)
            return value
        } else {
            spring.finalPosition = 1f
            val (fraction, _) = spring.updateValues(0f, 0f, playTime)
            return valueInterpolator.invoke(start, end, fraction)
        }
    }

    override fun isFinished(playTime: Long, start: Any, end: Any, startVelocity: Float): Boolean {
        if (start is Float && end is Float) {
            spring.finalPosition = end
            return spring.isAtEquilibrium(start, startVelocity, playTime)
        } else {
            spring.finalPosition = 1f
            return spring.isAtEquilibrium(0f, startVelocity, playTime)
        }
    }

    override fun getVelocity(playTime: Long, start: Any, end: Any, startVelocity: Float): Float {
        if (start is Float && end is Float) {
            spring.finalPosition = end
            val (_, velocity) = spring.updateValues(start, startVelocity, playTime)
            return velocity
        } else {
            // If the values are not floats, the float velocity makes very little sense
            return 0f
        }
    }
}
