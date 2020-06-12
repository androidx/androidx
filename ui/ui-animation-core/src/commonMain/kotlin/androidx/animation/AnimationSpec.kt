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

import kotlin.math.min

/**
 * [AnimationSpec]s are stateless, as they do not assume any starting/ending conditions. Nor do they
 * manage a lifecycle. All it stores is the particular animation configuration, e.g. easing and
 * duration for [FloatTweenSpec]s, or spring constants for [FloatSpringSpec]s. As a result, the
 * same [AnimationSpec] can be reused by a few different running animations with different
 * starting and ending values.
 *
 * Since [AnimationSpec]s are stateless, it requires starting value/velocity and ending value to
 * be passed in, along with playtime, to calculate the value or velocity at that time. Play time
 * here is the progress of the animation in terms of milliseconds. It will be clamped to between
 * 0 and duration.
 *
 * __Note__: For use cases where the starting values/velocity and ending values aren't expected
 * to change, it is recommended to use [Animation] that caches these static values and hence
 * does not require them to be supplied in the value/velocity calculation.
 *
 * @see Animation
 */
interface AnimationSpec<V : AnimationVector> {
    /**
     * Calculates the value of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTime time since the start of the animation
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getValue(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V

    /**
     * Calculates the velocity of the animation at given the playtime, with the provided start/end
     * values, and start velocity.
     *
     * @param playTime time since the start of the animation
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getVelocity(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V

    /**
     * Calculates the duration of an animation. For duration-based animations, this will return the
     * pre-defined duration. For physics-based animations, the duration will be estimated based on
     * the physics configuration (such as spring stiffness, damping ratio, visibility threshold)
     * as well as the [start], [end] values, and [startVelocity].
     *
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getDurationMillis(
        start: V,
        end: V,
        startVelocity: V
    ): Long

    /**
     * Calculates the end velocity of the animation with the provided start/end values, and start
     * velocity. For duration-based animations, end velocity will be the velocity of the
     * animation at the duration time. This is also the default assumption. However, for
     * physics-based animations, end velocity is an [AnimationVector] of 0s.
     *
     * @param start start value of the animation
     * @param end end value of the animation
     * @param startVelocity start velocity of the animation
     */
    fun getEndVelocity(
        start: V,
        end: V,
        startVelocity: V
    ): V = getVelocity(getDurationMillis(start, end, startVelocity), start, end, startVelocity)
}

/**
 * Base class for [AnimationSpec]s that are based on a fixed [duration].
 */
interface DurationBasedAnimationSpec<V : AnimationVector> : AnimationSpec<V> {
    /**
     * duration is the amount of time while animation is not yet finished.
     */
    val duration: Long
    /**
     * delay defines the amount of time that animation can be delayed.
     */
    val delay: Long

    override fun getDurationMillis(start: V, end: V, startVelocity: V): Long = delay + duration
}

/**
 * Clamps the input [playTime] to the duration range of the given [DurationBasedAnimationSpec].
 */
private fun DurationBasedAnimationSpec<*>.clampPlayTime(playTime: Long): Long {
    return (playTime - delay).coerceIn(0, duration)
}

/**
 * [KeyframesSpec] class manages the animation based on the values defined at different timestamps
 * in the duration of the animation (i.e. different keyframes). Each keyframe can be provided via
 * [keyframes] parameter. [KeyframesSpec] allows very specific animation definitions with a
 * precision to millisecond.
 *
 * Here's an example of creating a [KeyframesSpec] animation: ([KeyframesBuilder] could make
 * defining key frames more readable.)
 *
 *     val delay = 120L
 *     val startValue = AnimationVector3D(100f, 200f, 300f)
 *     val endValue = AnimationVector3D(200f, 100f, 0f)
 *     val keyframes = Keyframes<AnimationVector3D>(
 *          mutableMapOf (
 *               0L to (startValue to LinearEasing),
 *               100L to (startValue to FastOutLinearInEasing)
 *          ),
 *          200L,
 *          delay
 *     )
 *
 * @param keyframes a map from time to a value/easing function pair. The value in each entry
 *                  defines the animation value at that time, and the easing curve is used in the
 *                  interval starting from that time.
 * @param duration total duration of the animation
 * @param delay the amount of the time the animation should wait before it starts. Defaults to 0.
 *
 * @see [KeyframesBuilder]
 */
class KeyframesSpec<V : AnimationVector>(
    private val keyframes: Map<Long, Pair<V, Easing>>,
    override val duration: Long,
    override val delay: Long = 0L
) : DurationBasedAnimationSpec<V> {

    private lateinit var valueVector: V
    private lateinit var velocityVector: V

    override fun getValue(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V {
        val clampedPlayTime = clampPlayTime(playTime)
        // If there is a key frame defined with the given time stamp, return that value
        if (keyframes.containsKey(clampedPlayTime)) {
            return keyframes.getValue(clampedPlayTime).first
        }

        if (clampedPlayTime >= duration) {
            return end
        } else if (clampedPlayTime <= 0) return start

        var startTime = 0L
        var startVal = start
        var endVal = end
        var endTime: Long = duration
        var easing: Easing = LinearEasing
        for ((timestamp, value) in keyframes) {
            if (clampedPlayTime > timestamp && timestamp >= startTime) {
                startTime = timestamp
                startVal = value.first
                easing = value.second
            } else if (clampedPlayTime < timestamp && timestamp <= endTime) {
                endTime = timestamp
                endVal = value.first
            }
        }

        // Now interpolate
        val fraction = easing((clampedPlayTime - startTime) / (endTime - startTime).toFloat())
        init(start)
        for (i in 0 until startVal.size) {
            valueVector[i] = lerp(startVal[i], endVal[i], fraction)
        }
        return valueVector
    }

    private fun init(value: V) {
        if (!::valueVector.isInitialized) {
            valueVector = value.newInstance()
            velocityVector = value.newInstance()
        }
    }

    override fun getVelocity(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V {
        val clampedPlayTime = clampPlayTime(playTime)
        if (clampedPlayTime <= 0L) {
            return startVelocity
        }
        val startNum = getValue(clampedPlayTime - 1, start, end, startVelocity)
        val endNum = getValue(clampedPlayTime, start, end, startVelocity)

        init(start)
        for (i in 0 until startNum.size) {
            velocityVector[i] = (startNum[i] - endNum[i]) * 1000f
        }
        return velocityVector
    }
}

/**
 * [SnapSpec] immediately snaps the animating value to the end value.
 *
 * @param delay the amount of time (in milliseconds) that the animation should wait before it
 *              starts. Defaults to 0.
 */
class SnapSpec<V : AnimationVector>(
    override val delay: Long = 0L
) : DurationBasedAnimationSpec<V> {

    override fun getValue(playTime: Long, start: V, end: V, startVelocity: V): V {
        if (playTime < delay) {
            return start
        } else {
            return end
        }
    }

    override fun getVelocity(playTime: Long, start: V, end: V, startVelocity: V): V {
        return startVelocity
    }

    override val duration: Long
        get() = 0L
}

/**
 * This animation takes another [DurationBasedAnimationSpec] as a parameter and plays it
 * [iterationCount] times.
 *
 * @param iterationCount the count of iterations. Should be at least 1. [Infinite] can
 *  be used to have an infinity repeating animation.
 * @param animation the [AnimationSpec] describing each repetition iteration.
 */
class RepeatableSpec<V : AnimationVector>(
    private val iterationCount: Long,
    private val animation: DurationBasedAnimationSpec<V>
) : AnimationSpec<V> {

    init {
        if (iterationCount < 1) {
            throw IllegalArgumentException("Iterations count can't be less than 1")
        }
    }

    private val duration: Long = animation.delay + animation.duration

    private fun repetitionPlayTime(playTime: Long): Long {
        val repeatsCount = min(playTime / duration, iterationCount - 1L)
        return playTime - repeatsCount * duration
    }

    private fun repetitionStartVelocity(playTime: Long, start: V, startVelocity: V, end: V): V =
        if (playTime > duration) {
            // Start velocity of the 2nd and subsequent iteration will be the velocity at the end
            // of the first iteration, instead of the initial velocity.
            getVelocity(duration, start, startVelocity, end)
        } else
            startVelocity

    override fun getValue(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V {
        return animation.getValue(
            repetitionPlayTime(playTime),
            start,
            end,
            repetitionStartVelocity(playTime, start, startVelocity, end)
        )
    }

    override fun getVelocity(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V {
        return animation.getVelocity(
            repetitionPlayTime(playTime),
            start,
            end,
            repetitionStartVelocity(playTime, start, startVelocity, end)
        )
    }

    override fun getDurationMillis(start: V, end: V, startVelocity: V): Long {
        return iterationCount * duration
    }
}

/**
 * Physics class contains a number of recommended configurations for physics animations.
 */
object Spring {
    /**
     * Stiffness constant for extremely stiff spring
     */
    const val StiffnessHigh = 10_000f
    /**
     * Stiffness constant for medium stiff spring. This is the default stiffness for spring
     * force.
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
     * Damping ratio for a medium bouncy spring. This is also the default damping ratio for
     * spring force. Note for under-damped springs (i.e. damping ratio < 1), the lower the
     * damping ratio, the more bouncy the spring.
     */
    const val DampingRatioMediumBouncy = 0.5f
    /**
     * Damping ratio for a spring with low bounciness. Note for under-damped springs
     * (i.e. damping ratio < 1), the lower the damping ratio, the higher the bounciness.
     */
    const val DampingRatioLowBouncy = 0.75f
    /**
     * Damping ratio for a spring with no bounciness. This damping ratio will create a
     * critically damped spring that returns to equilibrium within the shortest amount of time
     * without oscillating.
     */
    const val DampingRatioNoBouncy = 1f
    /**
     * Default cutoff for rounding off physics based animations
     */
    const val DefaultDisplacementThreshold = 0.01f
}

/**
 * Internal data structure for storing different FloatAnimations for different dimensions.
 */
internal interface Animations {
    operator fun get(index: Int): FloatAnimationSpec
}

/**
 * [SpringSpec] uses spring animations to animate (each dimension of) [AnimationVector]s.
 */
class SpringSpec<V : AnimationVector> private constructor(
    val dampingRatio: Float,
    val stiffness: Float,
    anims: Animations
) : AnimationSpec<V> by VectorizedAnimationSpec<V>(anims) {

    /**
     * Creates a [SpringSpec] that uses the same spring constants (i.e. [dampingRatio] and
     * [stiffness] on all dimensions. The optional [visibilityThreshold] defines when the animation
     * should be considered to be visually close enough to target to stop. By default,
     * [Spring.DefaultDisplacementThreshold] is used on all dimensions of the
     * [AnimationVector].
     *
     * @param dampingRatio damping ratio of the spring
     * @param stiffness stiffness of the spring
     * @param visibilityThreshold specifies the visibility threshold for each dimension.
     */
    constructor(
        dampingRatio: Float,
        stiffness: Float,
        visibilityThreshold: V? = null
    ) : this(
        dampingRatio, stiffness,
        createSpringAnimations(visibilityThreshold, dampingRatio, stiffness)
    )
}

private fun <V : AnimationVector> createSpringAnimations(
    visibilityThreshold: V?,
    dampingRatio: Float,
    stiffness: Float
): Animations {
    if (visibilityThreshold != null) {
        return object : Animations {
            private val anims = (0 until visibilityThreshold.size).map { index ->
                FloatSpringSpec(dampingRatio, stiffness, visibilityThreshold[index])
            }

            override fun get(index: Int): FloatSpringSpec = anims[index]
        }
    } else {
        return object : Animations {
            private val anim = FloatSpringSpec(dampingRatio, stiffness)
            override fun get(index: Int): FloatSpringSpec = anim
        }
    }
}

/**
 * [TweenSpec] animates a [AnimationVector] value by interpolating the start and end value, in the
 * given [duration] using the given [easing] curve.
 *
 * @param duration duration of the [TweenSpec] animation. Defaults to [DefaultDuration].
 * @param delay the amount of time the animation should wait before it starts running, 0 by default.
 * @param easing the easing curve used by the animation. [FastOutSlowInEasing] by default.
 */
// TODO: Support different tween on different dimens
class TweenSpec<V : AnimationVector>(
    override val duration: Long = DefaultDuration.toLong(),
    override val delay: Long = 0,
    val easing: Easing = FastOutSlowInEasing
) : DurationBasedAnimationSpec<V> {
    private val anim = VectorizedAnimationSpec<V>(FloatTweenSpec(duration, delay, easing))
    override fun getValue(playTime: Long, start: V, end: V, startVelocity: V): V {
        return anim.getValue(playTime, start, end, startVelocity)
    }

    override fun getVelocity(playTime: Long, start: V, end: V, startVelocity: V): V {
        return anim.getVelocity(playTime, start, end, startVelocity)
    }
}

/**
 * Internal impl to turn a [FloatAnimationSpec] into a multi-dimensional [AnimationSpec].
 */
internal class VectorizedAnimationSpec<V : AnimationVector>(
    val anims: Animations
) : AnimationSpec<V> {
    private lateinit var valueVector: V
    private lateinit var velocityVector: V
    private lateinit var endVelocityVector: V

    constructor(anim: FloatAnimationSpec) : this(object : Animations {
        override fun get(index: Int): FloatAnimationSpec {
            return anim
        }
    })

    override fun getValue(playTime: Long, start: V, end: V, startVelocity: V): V {
        if (!::valueVector.isInitialized) {
            valueVector = start.newInstance()
        }
        for (i in 0 until valueVector.size) {
            valueVector[i] = anims[i].getValue(playTime, start[i], end[i], startVelocity[i])
        }
        return valueVector
    }

    override fun getVelocity(playTime: Long, start: V, end: V, startVelocity: V): V {
        if (!::velocityVector.isInitialized) {
            velocityVector = startVelocity.newInstance()
        }
        for (i in 0 until velocityVector.size) {
            velocityVector[i] = anims[i].getVelocity(playTime, start[i], end[i], startVelocity[i])
        }
        return velocityVector
    }

    override fun getEndVelocity(start: V, end: V, startVelocity: V): V {
        if (!::endVelocityVector.isInitialized) {
            endVelocityVector = startVelocity.newInstance()
        }
        for (i in 0 until endVelocityVector.size) {
            endVelocityVector[i] =
                anims[i].getEndVelocity(start[i], end[i], startVelocity[i])
        }
        return endVelocityVector
    }

    override fun getDurationMillis(start: V, end: V, startVelocity: V): Long {
        var maxDuration = 0L
        (0 until start.size).forEach {
            maxDuration = maxOf(
                maxDuration,
                anims[it].getDurationMillis(start[it], end[it], startVelocity[it])
            )
        }
        return maxDuration
    }
}
