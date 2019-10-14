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

import androidx.annotation.FloatRange
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sign

/**
 * This animation interface is intended to be stateless, just like Animation<T>. But unlike
 * Animation<T>, DecayAnimation does not have an end value defined. The end value is a
 * result of the animation rather than an input.
 */
// TODO: Figure out a better story for non-floats
interface DecayAnimation {
    /**
     * This is the absolute value of a velocity threshold, below which the animation is considered
     * finished.
     */
    val absVelocityThreshold: Float

    /**
     * Returns whether the animation is finished at the given time.
     *
     * @param playTime The time elapsed in milliseconds since the start of the animation
     * @param start The start value of the animation
     * @param startVelocity The start velocity of the animation
     */
    fun isFinished(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Boolean

    /**
     * Returns the value of the animation at the given time.
     *
     * @param playTime The time elapsed in milliseconds since the start of the animation
     * @param start The start value of the animation
     * @param startVelocity The start velocity of the animation
     */
    fun getValue(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Float

    /**
     * Returns the velocity of the animation at the given time.
     *
     * @param playTime The time elapsed in milliseconds since the start of the animation
     * @param start The start value of the animation
     * @param startVelocity The start velocity of the animation
     */
    fun getVelocity(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Float

    /**
     * Returns the target value of the animation based on the starting condition of the animation (
     * i.e. start value and start velocity).
     *
     * @param start The start value of the animation
     * @param startVelocity The start velocity of the animation
     */
    fun getTarget(
        start: Float,
        startVelocity: Float
    ): Float
}

private const val ExponentialDecayFriction = -4.2f

/**
 * This is a decay animation where the friction/deceleration is always proportional to the velocity.
 * As a result, the velocity goes under an exponential decay. The constructor parameter, friction
 * multiplier, can be tuned to adjust the amount of friction applied in the decay. The higher the
 * multiplier, the higher the friction, the sooner the animation will stop, and the shorter distance
 * the animation will travel with the same starting condition.
 */
class ExponentialDecay(
    @FloatRange(from = 0.0, fromInclusive = false) frictionMultiplier: Float = 1f,
    @FloatRange(from = 0.0, fromInclusive = false) absVelocityThreshold: Float = 0.1f
) : DecayAnimation {

    override val absVelocityThreshold: Float = max(0.0000001f, abs(absVelocityThreshold))
    private val friction: Float = ExponentialDecayFriction * max(0.0001f, frictionMultiplier)

    override fun isFinished(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Boolean {
        return abs(getVelocity(playTime, start, startVelocity)) <= absVelocityThreshold
    }

    override fun getValue(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Float {
        return start - startVelocity / friction +
                startVelocity / friction * exp(friction * playTime / 1000f)
    }

    override fun getVelocity(
        playTime: Long,
        start: Float,
        startVelocity: Float
    ): Float {
        return (startVelocity * exp(((playTime / 1000f) * friction)))
    }

    override fun getTarget(
        start: Float,
        startVelocity: Float
    ): Float {
        if (abs(startVelocity) <= absVelocityThreshold) {
            return start
        }
        val duration: Double =
            ln(abs(absVelocityThreshold / startVelocity).toDouble()) / friction * 1000

        return start - startVelocity / friction +
                startVelocity / friction * exp((friction * duration / 1000f)).toFloat()
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
) : AnimationWrapper<Float> {
    private val target: Float = anim.getTarget(startValue, startVelocity)

    override fun getValue(playTime: Long): Float {
        if (!isFinished(playTime)) {
            return anim.getValue(playTime, startValue, startVelocity)
        } else {
            return target
        }
    }
    override fun getVelocity(playTime: Long): Float {
        if (!isFinished(playTime)) {
            return anim.getVelocity(playTime, startValue, startVelocity)
        } else {
            return anim.absVelocityThreshold * sign(startVelocity)
        }
    }
    override fun isFinished(playTime: Long): Boolean =
        anim.isFinished(playTime, startValue, startVelocity)
}

internal fun DecayAnimation.createWrapper(
    startValue: Float,
    startVelocity: Float = 0f
): AnimationWrapper<Float> {
    return DecayAnimationWrapper(startValue, startVelocity, this)
}
