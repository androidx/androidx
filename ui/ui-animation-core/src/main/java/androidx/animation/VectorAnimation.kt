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

const val DEBUG = false

/**
 * This animation interface is intended to be stateless. Once they are configured, they know how to
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
internal interface Animation<V : AnimationVector> {
    fun getValue(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V

    fun getVelocity(
        playTime: Long,
        start: V,
        end: V,
        startVelocity: V
    ): V

    fun getEndVelocity(
        start: V,
        end: V,
        startVelocity: V,
        animationDuration: Long
    ): V = getVelocity(animationDuration, start, end, startVelocity)

    fun getDurationMillis(
        start: V,
        end: V,
        startVelocity: V
    ): Long
}

/**
 * Base class for [Animation]s based on a fixed [duration].
 *
 */
internal interface DurationBasedAnimation<V : AnimationVector> : Animation<V> {
    /**
     * duration is the amount of time while animation is not yet finished.
     */
    val duration: Long
    /**
     * delay defines the amount of time that animation can be delayed.
     */
    val delay: Long

    override fun getDurationMillis(start: V, end: V, startVelocity: V): Long = delay + duration

    fun clampPlayTime(playTime: Long): Long {
        return (playTime - delay).coerceIn(0, duration)
    }
}

/**
 * [Keyframes] class manages the animation based on the values defined at different timestamps in
 * the duration of the animation (i.e. different keyframes). Each keyframe can be provided via
 * [keyframes] parameter. [Keyframes] allows very specific animation definitions with a
 * precision to millisecond.
 *
 * Use [KeyframesBuilder] to create a [Keyframes] animation.
 */
internal class Keyframes<V : AnimationVector>(
    override val duration: Long,
    override val delay: Long,
    private val keyframes: Map<Long, Pair<V, Easing>>
) : DurationBasedAnimation<V> {

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
 * [SnapAnimation] immediately snaps the animating value to the end value.
 */
internal class SnapAnimation<V : AnimationVector> : Animation<V> {

    override fun getValue(playTime: Long, start: V, end: V, startVelocity: V): V {
        return end
    }

    override fun getVelocity(playTime: Long, start: V, end: V, startVelocity: V): V {
        return startVelocity
    }

    override fun getDurationMillis(start: V, end: V, startVelocity: V): Long {
        return 0
    }
}

/**
 * This animation takes another [animation] as a parameter and repeats it [iterationCount] times.
 *
 * @param iterationCount the count of iterations. Should be at least 1. [Infinite] can
 *  be used to have an infinity repeating animation.
 * @param animation the [Animation] describing each repetition iteration.
 */
internal class Repeatable<V : AnimationVector>(
    private val iterationCount: Long,
    private val animation: DurationBasedAnimation<V>
) : Animation<V> {

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