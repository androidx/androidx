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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.animation.core

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.util.fastCoerceIn

/**
 * [FloatAnimationSpec] interface is similar to [VectorizedAnimationSpec], except it deals
 * exclusively with floats.
 *
 * Like [VectorizedAnimationSpec], [FloatAnimationSpec] is entirely stateless as well. It requires
 * start/end values and start velocity to be passed in for the query of velocity and value of the
 * animation. The [FloatAnimationSpec] itself stores only the animation configuration (such as the
 * delay, duration and easing curve for [FloatTweenSpec], or spring constants for [FloatSpringSpec].
 *
 * A [FloatAnimationSpec] can be converted to an [VectorizedAnimationSpec] using [vectorize].
 *
 * @see [VectorizedAnimationSpec]
 */
@JvmDefaultWithCompatibility
public interface FloatAnimationSpec : AnimationSpec<Float> {
    /**
     * Calculates the value of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTimeNanos time since the start of the animation
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    public fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float

    /**
     * Calculates the velocity of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTimeNanos time since the start of the animation
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    public fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float

    /**
     * Calculates the end velocity of the animation with the provided start/end values, and start
     * velocity. For duration-based animations, end velocity will be the velocity of the animation
     * at the duration time. This is also the default assumption. However, for spring animations,
     * the transient trailing velocity will be snapped to zero.
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    public fun getEndVelocity(
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float =
        getVelocityFromNanos(
            getDurationNanos(initialValue, targetValue, initialVelocity),
            initialValue,
            targetValue,
            initialVelocity
        )

    /**
     * Calculates the duration of an animation. For duration-based animations, this will return the
     * pre-defined duration. For physics-based animations, the duration will be estimated based on
     * the physics configuration (such as spring stiffness, damping ratio, visibility threshold) as
     * well as the [initialValue], [targetValue] values, and [initialVelocity].
     *
     * __Note__: this may be a computation that is expensive - especially with spring based
     * animations
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    @Suppress("MethodNameUnits")
    public fun getDurationNanos(
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Long

    /**
     * Create an [VectorizedAnimationSpec] that animates [AnimationVector] from a
     * [FloatAnimationSpec]. Every dimension of the [AnimationVector] will be animated using the
     * given [FloatAnimationSpec].
     */
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedFloatAnimationSpec<V> = VectorizedFloatAnimationSpec<V>(this)
}

/**
 * [FloatSpringSpec] animation uses a spring animation to animate a [Float] value. Its configuration
 * can be tuned via adjusting the spring parameters, namely damping ratio and stiffness.
 *
 * @param dampingRatio damping ratio of the spring. Defaults to [Spring.DampingRatioNoBouncy]
 * @param stiffness Stiffness of the spring. Defaults to [Spring.StiffnessMedium]
 * @param visibilityThreshold The value threshold such that the animation is no longer significant.
 *   e.g. 1px for translation animations. Defaults to [Spring.DefaultDisplacementThreshold]
 */
public class FloatSpringSpec(
    public val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    public val stiffness: Float = Spring.StiffnessMedium,
    private val visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : FloatAnimationSpec {

    private val spring =
        SpringSimulation(1f).also {
            it.dampingRatio = dampingRatio
            it.stiffness = stiffness
        }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float {
        // TODO: Properly support Nanos in the spring impl
        val playTimeMillis = playTimeNanos / MillisToNanos
        spring.finalPosition = targetValue
        return spring.updateValues(initialValue, initialVelocity, playTimeMillis).value
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float {
        // TODO: Properly support Nanos in the spring impl
        val playTimeMillis = playTimeNanos / MillisToNanos
        spring.finalPosition = targetValue
        return spring.updateValues(initialValue, initialVelocity, playTimeMillis).velocity
    }

    override fun getEndVelocity(
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float = 0f

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Long =
        estimateAnimationDurationMillis(
            stiffness = spring.stiffness,
            dampingRatio = spring.dampingRatio,
            initialDisplacement = (initialValue - targetValue) / visibilityThreshold,
            initialVelocity = initialVelocity / visibilityThreshold,
            delta = 1f
        ) * MillisToNanos
}

/**
 * [FloatTweenSpec] animates a Float value from any start value to any end value using a provided
 * [easing] function. The animation will finish within the [duration] time. Unless a [delay] is
 * specified, the animation will start right away.
 *
 * @param duration the amount of time (in milliseconds) the animation will take to finish. Defaults
 *   to [DefaultDurationMillis]
 * @param delay the amount of time the animation will wait before it starts running. Defaults to 0.
 * @param easing the easing function that will be used to interoplate between the start and end
 *   value of the animation. Defaults to [FastOutSlowInEasing].
 */
public class FloatTweenSpec(
    public val duration: Int = DefaultDurationMillis,
    public val delay: Int = 0,
    private val easing: Easing = FastOutSlowInEasing
) : FloatAnimationSpec {
    private val durationNanos: Long = duration * MillisToNanos

    private val delayNanos: Long = delay * MillisToNanos

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float {
        val clampedPlayTimeNanos = clampPlayTimeNanos(playTimeNanos)
        val rawFraction = if (duration == 0) 1f else clampedPlayTimeNanos / durationNanos.toFloat()
        val fraction = easing.transform(rawFraction)
        return lerp(initialValue, targetValue, fraction)
    }

    private inline fun clampPlayTimeNanos(playTimeNanos: Long): Long {
        return (playTimeNanos - delayNanos).fastCoerceIn(0, durationNanos)
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Long {
        return delayNanos + durationNanos
    }

    // Calculate velocity by difference between the current value and the value 1 ms ago. This is a
    // preliminary way of calculating velocity used by easing curve based animations, and keyframe
    // animations. Physics-based animations give a much more accurate velocity.
    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        targetValue: Float,
        initialVelocity: Float
    ): Float {
        val clampedPlayTimeNanos = clampPlayTimeNanos(playTimeNanos)
        if (clampedPlayTimeNanos == 0L) {
            return initialVelocity
        }
        val startNum =
            getValueFromNanos(
                clampedPlayTimeNanos - MillisToNanos,
                initialValue,
                targetValue,
                initialVelocity
            )
        val endNum =
            getValueFromNanos(clampedPlayTimeNanos, initialValue, targetValue, initialVelocity)
        return (endNum - startNum) * 1000f
    }
}
