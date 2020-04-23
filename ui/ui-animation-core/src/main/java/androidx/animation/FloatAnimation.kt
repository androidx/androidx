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

/**
 * FloatAnimation interface to avoid boxing/unboxing on floats.
 */
internal interface FloatAnimation {
    fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float

    fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float

    /**
     * Function to help snap velocity to a specific value after the animation is done. A specific
     * use case is for springs, where transient trailing velocity should be snapped to zero.
     *
     * @return velocity for all time >= [animationDuration], or null if the function is to
     * default to [getVelocity].
     */
    fun getEndVelocity(
        start: Float,
        end: Float,
        startVelocity: Float,
        animationDuration: Long
    ): Float = getVelocity(animationDuration, start, end, startVelocity)

    /**
     * Note that this may be a computation that is expensive - especially with spring based
     * animations
     */
    fun getDurationMillis(
        start: Float,
        end: Float,
        startVelocity: Float
    ): Long
}

/**
 * Physics class contains a number of recommended configurations for physics animations.
 */
// TODO: Consider making all animations public, and fold this companion object into SpringAnimation
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
 * [SpringAnimation] animation is in its core a spring animation. It is the default animation that
 * the animation system uses to animate from [TransitionState] to [TransitionState] when no
 * animations are specified. Its configuration can be tuned via adjusting the spring parameters,
 * namely damping ratio and stiffness.
 */
internal class SpringAnimation(
    /**
     * Damping ratio of the spring. Defaults to [Spring.DampingRatioNoBouncy]
     */
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    /**
     * Stiffness of the spring. Defaults to [Spring.StiffnessVeryLow]
     */
    stiffness: Float = Spring.StiffnessMedium,
    /**
     * The value threshold such that the animation is no longer significant. An example would be
     * 1px for translation animations. Defaults to [Spring.DefaultDisplacementThreshold]
     */
    private val displacementThreshold: Float = Spring.DefaultDisplacementThreshold
) : FloatAnimation {

    private val spring = SpringSimulation(1f).also {
        it.dampingRatio = dampingRatio
        it.stiffness = stiffness
    }

    override fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        spring.finalPosition = end
        val value = spring.updateValues(start, startVelocity, playTime).value
        return value
    }

    override fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        spring.finalPosition = end
        val velocity = spring.updateValues(start, startVelocity, playTime).velocity
        return velocity
    }

    override fun getEndVelocity(
        start: Float,
        end: Float,
        startVelocity: Float,
        animationDuration: Long
    ): Float = 0f

    override fun getDurationMillis(start: Float, end: Float, startVelocity: Float): Long =
        estimateAnimationDurationMillis(
            stiffness = spring.stiffness,
            dampingRatio = spring.dampingRatio,
            initialDisplacement = (start - end) / displacementThreshold,
            initialVelocity = startVelocity / displacementThreshold,
            delta = 1f)
}

/**
 * [Tween] is responsible for animating from one value to another using a provided [easing].
 * The duration (in milliseconds) for such an animation can be adjusted via [duration]. The
 * animation can be delayed via [delay].
 */
internal class Tween(
    val duration: Long,
    val delay: Long,
    private val easing: Easing
) : FloatAnimation {
    override fun getValue(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        val clampedPlayTime = clampPlayTime(playTime)
        val rawFraction = if (duration == 0L) 1f else clampedPlayTime / duration.toFloat()
        val fraction = easing(rawFraction.coerceIn(0f, 1f))
        return lerp(start, end, fraction)
    }

    private fun clampPlayTime(playTime: Long): Long {
        return (playTime - delay).coerceIn(0, duration)
    }

    override fun getDurationMillis(start: Float, end: Float, startVelocity: Float): Long {
        return delay + duration
    }

    /**
     * Calculate velocity by difference between the current value and the value 1 ms ago. This is a
     * preliminary way of calculating velocity used by easing curve based animations, and keyframe
     * animations. Physics-based animations give a much more accurate velocity.
     */
    override fun getVelocity(
        playTime: Long,
        start: Float,
        end: Float,
        startVelocity: Float
    ): Float {
        val clampedPlayTime = clampPlayTime(playTime)
        if (clampedPlayTime < 0) {
            return 0f
        } else if (clampedPlayTime == 0L) {
            return startVelocity
        }
        val startNum = getValue(clampedPlayTime - 1, start, end, startVelocity)
        val endNum = getValue(clampedPlayTime, start, end, startVelocity)
        return (endNum - startNum) * 1000f
    }
}
