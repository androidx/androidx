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

internal class Animation1D(
    val anim: FloatAnimation = SpringAnimation()
) : Animation<AnimationVector1D> {
    private val tempVelocityVector = AnimationVector1D(0f)
    private val tempValueVector = AnimationVector1D(0f)

    override fun getValue(
        playTime: Long,
        start: AnimationVector1D,
        end: AnimationVector1D,
        startVelocity: AnimationVector1D
    ): AnimationVector1D =
        tempValueVector.apply {
            value = anim.getValue(playTime, start.value, end.value, startVelocity.value)
        }

    override fun getVelocity(
        playTime: Long,
        start: AnimationVector1D,
        end: AnimationVector1D,
        startVelocity: AnimationVector1D
    ): AnimationVector1D =
        tempVelocityVector.apply {
            value = anim.getVelocity(playTime, start.value, end.value, startVelocity.value)
        }

    override fun getDurationMillis(
        start: AnimationVector1D,
        end: AnimationVector1D,
        startVelocity: AnimationVector1D
    ): Long {
        return anim.getDurationMillis(start.value, end.value, startVelocity.value)
    }
}

/**
 * This creates a 2D animation from Float animations for each of the 2 dimensions.
 */
internal class Animation2D(
    val a1: FloatAnimation = SpringAnimation(),
    val a2: FloatAnimation = SpringAnimation()
) : Animation<AnimationVector2D> {
    private var velocityVector: AnimationVector2D = AnimationVector2D(0f, 0f)
    private var valueVector: AnimationVector2D = AnimationVector2D(0f, 0f)

    override fun getValue(
        playTime: Long,
        start: AnimationVector2D,
        end: AnimationVector2D,
        startVelocity: AnimationVector2D
    ): AnimationVector2D {
        valueVector.v1 = a1.getValue(playTime, start.v1, end.v1, startVelocity.v1)
        valueVector.v2 = a2.getValue(playTime, start.v2, end.v2, startVelocity.v2)
        return valueVector
    }

    override fun getVelocity(
        playTime: Long,
        start: AnimationVector2D,
        end: AnimationVector2D,
        startVelocity: AnimationVector2D
    ): AnimationVector2D {
        velocityVector.v1 = a1.getVelocity(playTime, start.v1, end.v1, startVelocity.v1)
        velocityVector.v2 = a2.getVelocity(playTime, start.v2, end.v2, startVelocity.v2)
        return velocityVector
    }

    override fun getDurationMillis(
        start: AnimationVector2D,
        end: AnimationVector2D,
        startVelocity: AnimationVector2D
    ): Long {
        return maxOf(
            a1.getDurationMillis(start.v1, end.v1, startVelocity.v1),
            a2.getDurationMillis(start.v2, end.v2, startVelocity.v2)
        )
    }
}

/**
 * This creates a 3D animation from Float animations for each of the 3 dimensions.
 */
internal class Animation3D(
    val a1: FloatAnimation = SpringAnimation(),
    val a2: FloatAnimation = SpringAnimation(),
    val a3: FloatAnimation = SpringAnimation()
) : Animation<AnimationVector3D> {
    private var velocityVector: AnimationVector3D = AnimationVector3D(0f, 0f, 0f)
    private var valueVector: AnimationVector3D = AnimationVector3D(0f, 0f, 0f)

    override fun getValue(
        playTime: Long,
        start: AnimationVector3D,
        end: AnimationVector3D,
        startVelocity: AnimationVector3D
    ): AnimationVector3D {
        valueVector.v1 = a1.getValue(playTime, start.v1, end.v1, startVelocity.v1)
        valueVector.v2 = a2.getValue(playTime, start.v2, end.v2, startVelocity.v2)
        valueVector.v3 = a3.getValue(playTime, start.v3, end.v3, startVelocity.v3)
        return valueVector
    }

    override fun getVelocity(
        playTime: Long,
        start: AnimationVector3D,
        end: AnimationVector3D,
        startVelocity: AnimationVector3D
    ): AnimationVector3D {
        velocityVector.v1 = a1.getVelocity(playTime, start.v1, end.v1, startVelocity.v1)
        velocityVector.v2 = a2.getVelocity(playTime, start.v2, end.v2, startVelocity.v2)
        velocityVector.v3 = a3.getVelocity(playTime, start.v3, end.v3, startVelocity.v3)
        return velocityVector
    }

    override fun getDurationMillis(
        start: AnimationVector3D,
        end: AnimationVector3D,
        startVelocity: AnimationVector3D
    ): Long {
        return maxOf(
            a1.getDurationMillis(start.v1, end.v1, startVelocity.v1),
            a2.getDurationMillis(start.v2, end.v2, startVelocity.v2),
            a3.getDurationMillis(start.v3, end.v3, startVelocity.v3)
        )
    }
}

/**
 * This creates a 4D animation from Float animations for each of the 4 dimensions.
 */
internal class Animation4D(
    val a1: FloatAnimation = SpringAnimation(),
    val a2: FloatAnimation = SpringAnimation(),
    val a3: FloatAnimation = SpringAnimation(),
    val a4: FloatAnimation = SpringAnimation()
) : Animation<AnimationVector4D> {
    private var velocityVector: AnimationVector4D = AnimationVector4D(0f, 0f, 0f, 0f)
    private var valueVector: AnimationVector4D = AnimationVector4D(0f, 0f, 0f, 0f)

    override fun getValue(
        playTime: Long,
        start: AnimationVector4D,
        end: AnimationVector4D,
        startVelocity: AnimationVector4D
    ): AnimationVector4D {
        valueVector.v1 = a1.getValue(playTime, start.v1, end.v1, startVelocity.v1)
        valueVector.v2 = a2.getValue(playTime, start.v2, end.v2, startVelocity.v2)
        valueVector.v3 = a3.getValue(playTime, start.v3, end.v3, startVelocity.v3)
        valueVector.v4 = a4.getValue(playTime, start.v4, end.v4, startVelocity.v4)
        return valueVector
    }

    override fun getVelocity(
        playTime: Long,
        start: AnimationVector4D,
        end: AnimationVector4D,
        startVelocity: AnimationVector4D
    ): AnimationVector4D {
        velocityVector.v1 = a1.getVelocity(playTime, start.v1, end.v1, startVelocity.v1)
        velocityVector.v2 = a2.getVelocity(playTime, start.v2, end.v2, startVelocity.v2)
        velocityVector.v3 = a3.getVelocity(playTime, start.v3, end.v3, startVelocity.v3)
        velocityVector.v4 = a4.getVelocity(playTime, start.v4, end.v4, startVelocity.v4)
        return velocityVector
    }

    override fun getDurationMillis(
        start: AnimationVector4D,
        end: AnimationVector4D,
        startVelocity: AnimationVector4D
    ): Long {
        return maxOf(
            a1.getDurationMillis(start.v1, end.v1, startVelocity.v1),
            a2.getDurationMillis(start.v2, end.v2, startVelocity.v2),
            maxOf(
                a3.getDurationMillis(start.v3, end.v3, startVelocity.v3),
                a4.getDurationMillis(start.v4, end.v4, startVelocity.v4)
            )
        )
    }
}

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
    endValue: T,
    private val animation: Animation<V>,
    private val typeConverter: TwoWayConverter<T, V>
) : AnimationWrapper<T, V> {
    private val startValueVector = typeConverter.convertToVector.invoke(startValue)
    private val endValueVector = typeConverter.convertToVector.invoke(endValue)

    override fun getValue(playTime: Long): T =
        typeConverter.convertFromVector.invoke(
            animation.getValue(
                playTime, startValueVector,
                endValueVector, startVelocity
            )
        )

    override val durationMillis: Long = animation.getDurationMillis(
        start = startValueVector,
        end = endValueVector,
        startVelocity = startVelocity
    )

    override fun getVelocity(playTime: Long): V =
        animation.getVelocity(playTime, startValueVector, endValueVector, startVelocity)
}