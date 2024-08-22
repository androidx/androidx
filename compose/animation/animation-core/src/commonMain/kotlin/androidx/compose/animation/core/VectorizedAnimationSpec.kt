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

package androidx.compose.animation.core

import androidx.collection.IntList
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.util.fastCoerceIn
import kotlin.jvm.JvmInline
import kotlin.math.min

/**
 * [VectorizedAnimationSpec]s are stateless vector based animation specifications. They do not
 * assume any starting/ending conditions. Nor do they manage a lifecycle. All it stores is the
 * configuration that is particular to the type of the animation. easing and duration for
 * [VectorizedTweenSpec]s, or spring constants for [VectorizedSpringSpec]s. Its stateless nature
 * allows the same [VectorizedAnimationSpec] to be reused by a few different running animations with
 * different starting and ending values. More importantly, it allows the system to reuse the same
 * animation spec when the animation target changes in-flight.
 *
 * Since [VectorizedAnimationSpec]s are stateless, it requires starting value/velocity and ending
 * value to be passed in, along with playtime, to calculate the value or velocity at that time. Play
 * time here is the progress of the animation in terms of milliseconds, where 0 means the start of
 * the animation and [getDurationNanos] returns the play time for the end of the animation.
 *
 * __Note__: For use cases where the starting values/velocity and ending values aren't expected to
 * change, it is recommended to use [Animation] that caches these static values and hence does not
 * require them to be supplied in the value/velocity calculation.
 *
 * @see Animation
 */
@JvmDefaultWithCompatibility
public interface VectorizedAnimationSpec<V : AnimationVector> {
    /**
     * Whether or not the [VectorizedAnimationSpec] specifies an infinite animation. That is, one
     * that will not finish by itself, one that needs an external action to stop. For examples, an
     * indeterminate progress bar, which will only stop when it is removed from the composition.
     */
    public val isInfinite: Boolean

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
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V

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
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V

    /**
     * Calculates the duration of an animation. For duration-based animations, this will return the
     * pre-defined duration. For physics-based animations, the duration will be estimated based on
     * the physics configuration (such as spring stiffness, damping ratio, visibility threshold) as
     * well as the [initialValue], [targetValue] values, and [initialVelocity].
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    @Suppress("MethodNameUnits")
    public fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long

    /**
     * Calculates the end velocity of the animation with the provided start/end values, and start
     * velocity. For duration-based animations, end velocity will be the velocity of the animation
     * at the duration time. This is also the default assumption. However, for physics-based
     * animations, end velocity is an [AnimationVector] of 0s.
     *
     * @param initialValue start value of the animation
     * @param targetValue end value of the animation
     * @param initialVelocity start velocity of the animation
     */
    public fun getEndVelocity(initialValue: V, targetValue: V, initialVelocity: V): V =
        getVelocityFromNanos(
            getDurationNanos(initialValue, targetValue, initialVelocity),
            initialValue,
            targetValue,
            initialVelocity
        )
}

/**
 * Calculates the duration of an animation. For duration-based animations, this will return the
 * pre-defined duration. For physics-based animations, the duration will be estimated based on the
 * physics configuration (such as spring stiffness, damping ratio, visibility threshold) as well as
 * the [initialValue], [targetValue] values, and [initialVelocity].
 *
 * @param initialValue start value of the animation
 * @param targetValue end value of the animation
 * @param initialVelocity start velocity of the animation
 */
internal fun <V : AnimationVector> VectorizedAnimationSpec<V>.getDurationMillis(
    initialValue: V,
    targetValue: V,
    initialVelocity: V
): Long = getDurationNanos(initialValue, targetValue, initialVelocity) / MillisToNanos

/**
 * Calculates the value of the animation at given the playtime, with the provided start/end values,
 * and start velocity.
 *
 * @param playTimeMillis time since the start of the animation
 * @param start start value of the animation
 * @param end end value of the animation
 * @param startVelocity start velocity of the animation
 */
// TODO: Move tests off this API
internal fun <V : AnimationVector> VectorizedAnimationSpec<V>.getValueFromMillis(
    playTimeMillis: Long,
    start: V,
    end: V,
    startVelocity: V
): V = getValueFromNanos(playTimeMillis * MillisToNanos, start, end, startVelocity)

/**
 * All the finite [VectorizedAnimationSpec]s implement this interface, including:
 * [VectorizedKeyframesSpec], [VectorizedTweenSpec], [VectorizedRepeatableSpec],
 * [VectorizedSnapSpec], [VectorizedSpringSpec], etc. The [VectorizedAnimationSpec] that does
 * __not__ implement this is: [InfiniteRepeatableSpec].
 */
@JvmDefaultWithCompatibility
public interface VectorizedFiniteAnimationSpec<V : AnimationVector> : VectorizedAnimationSpec<V> {
    override val isInfinite: Boolean
        get() = false
}

/** Base class for [VectorizedAnimationSpec]s that are based on a fixed [durationMillis]. */
@JvmDefaultWithCompatibility
public interface VectorizedDurationBasedAnimationSpec<V : AnimationVector> :
    VectorizedFiniteAnimationSpec<V> {
    /** duration is the amount of time while animation is not yet finished. */
    public val durationMillis: Int

    /** delay defines the amount of time that animation can be delayed. */
    public val delayMillis: Int

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        (delayMillis + durationMillis) * MillisToNanos
}

/**
 * Clamps the input [playTime] to the duration range of the given
 * [VectorizedDurationBasedAnimationSpec].
 */
internal fun VectorizedDurationBasedAnimationSpec<*>.clampPlayTime(playTime: Long): Long {
    return (playTime - delayMillis).fastCoerceIn(0, durationMillis.toLong())
}

/**
 * [VectorizedKeyframesSpec] class manages the animation based on the values defined at different
 * timestamps in the duration of the animation (i.e. different keyframes). Each keyframe can be
 * provided via [keyframes] parameter. [VectorizedKeyframesSpec] allows very specific animation
 * definitions with a precision to millisecond.
 *
 * Here's an example of creating a [VectorizedKeyframesSpec] animation: ([keyframes] and
 * [KeyframesSpec.KeyframesSpecConfig] could make defining key frames much more readable.)
 *
 *     val delay = 120
 *     val startValue = AnimationVector3D(100f, 200f, 300f)
 *     val endValue = AnimationVector3D(200f, 100f, 0f)
 *     val keyframes = VectorizedKeyframesSpec<AnimationVector3D>(
 *          keyframes = mutableMapOf (
 *               0 to (startValue to LinearEasing),
 *               100 to (startValue to FastOutLinearInEasing)
 *          ),
 *          durationMillis = 200,
 *          delayMillis = delay
 *     )
 *
 * The interpolation between each value is dictated by [VectorizedKeyframeSpecElementInfo.arcMode]
 * on each keyframe. If no keyframe information is provided, [initialArcMode] is used.
 *
 * @see [KeyframesSpec]
 */
public class VectorizedKeyframesSpec<V : AnimationVector>
internal constructor(
    // List of all timestamps. Must include start (time = 0), end (time = durationMillis) and all
    // other timestamps found in [keyframes].
    private val timestamps: IntList,
    private val keyframes: IntObjectMap<VectorizedKeyframeSpecElementInfo<V>>,
    override val durationMillis: Int,
    override val delayMillis: Int,
    // Easing used for any segment of time not covered by [keyframes].
    private val defaultEasing: Easing,
    // The [ArcMode] used from time `0` until the first keyframe. So, it applies
    // for the entire duration if [keyframes] is empty.
    private val initialArcMode: ArcMode
) : VectorizedDurationBasedAnimationSpec<V> {
    /**
     * @param keyframes a map from time to a value/easing function pair. The value in each entry
     *   defines the animation value at that time, and the easing curve is used in the interval
     *   starting from that time.
     * @param durationMillis total duration of the animation
     * @param delayMillis the amount of the time the animation should wait before it starts.
     *   Defaults to 0.
     */
    public constructor(
        keyframes: Map<Int, Pair<V, Easing>>,
        durationMillis: Int,
        delayMillis: Int = 0
    ) : this(
        timestamps =
            kotlin.run {
                val times = MutableIntList(keyframes.size + 2)
                keyframes.forEach { (t, _) -> times.add(t) }
                if (!keyframes.containsKey(0)) {
                    times.add(0, 0)
                }
                if (!keyframes.containsKey(durationMillis)) {
                    times.add(durationMillis)
                }
                times.sort()
                return@run times
            },
        keyframes =
            kotlin.run {
                val timeToInfoMap = MutableIntObjectMap<VectorizedKeyframeSpecElementInfo<V>>()
                keyframes.forEach { (time, valueEasing) ->
                    timeToInfoMap[time] =
                        VectorizedKeyframeSpecElementInfo(
                            vectorValue = valueEasing.first,
                            easing = valueEasing.second,
                            arcMode = ArcMode.ArcLinear
                        )
                }

                return@run timeToInfoMap
            },
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        defaultEasing = LinearEasing,
        initialArcMode = ArcMode.ArcLinear
    )

    /**
     * List of time range for the given keyframes.
     *
     * This will be used to do a faster lookup for the corresponding Easing curves.
     */
    private var modes: IntArray = EmptyIntArray
    private var times: FloatArray = EmptyFloatArray
    private var valueVector: V? = null
    private var velocityVector: V? = null

    // Objects for ArcSpline
    private var lastInitialValue: V? = null
    private var lastTargetValue: V? = null
    private var posArray: FloatArray = EmptyFloatArray
    private var slopeArray: FloatArray = EmptyFloatArray
    private var arcSpline: ArcSpline = EmptyArcSpline

    private fun init(initialValue: V, targetValue: V, initialVelocity: V) {
        var requiresArcSpline = arcSpline !== EmptyArcSpline

        // Only need to initialize once
        if (valueVector == null) {
            valueVector = initialValue.newInstance()
            velocityVector = initialVelocity.newInstance()

            times = FloatArray(timestamps.size) { timestamps[it].toFloat() / SecondsToMillis }

            modes =
                IntArray(timestamps.size) {
                    val mode = (keyframes[timestamps[it]]?.arcMode ?: initialArcMode)
                    if (mode != ArcMode.ArcLinear) {
                        requiresArcSpline = true
                    }

                    mode.value
                }
        }

        if (!requiresArcSpline) {
            return
        }

        // Initialize variables dependent on initial and/or target value
        if (
            arcSpline === EmptyArcSpline ||
                lastInitialValue != initialValue ||
                lastTargetValue != targetValue
        ) {
            lastInitialValue = initialValue
            lastTargetValue = targetValue

            // Force to the next even dimension
            val dimensionCount = initialValue.size % 2 + initialValue.size
            posArray = FloatArray(dimensionCount)
            slopeArray = FloatArray(dimensionCount)

            // TODO(b/299477780): Re-use objects, after the first pass, only the initial and target
            //  may change, and only if the keyframes does not overwrite it
            val values =
                Array(timestamps.size) {
                    val timestamp = timestamps[it]
                    val info = keyframes[timestamp]
                    // Start (zero) and end (durationMillis) may not have been declared in
                    // keyframes
                    if (timestamp == 0 && info == null) {
                        FloatArray(dimensionCount) { i -> initialValue[i] }
                    } else if (timestamp == durationMillis && info == null) {
                        FloatArray(dimensionCount) { i -> targetValue[i] }
                    } else {
                        // All other values are guaranteed to exist
                        val vectorValue = info!!.vectorValue
                        FloatArray(dimensionCount) { i -> vectorValue[i] }
                    }
                }
            arcSpline = ArcSpline(arcModes = modes, timePoints = times, y = values)
        }
    }

    /**
     * @Throws IllegalStateException When the initial or final value to animate within a keyframe is
     *   missing.
     */
    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis).toInt()

        // If there is a key frame defined with the given time stamp, return that value
        val keyframe = keyframes[clampedPlayTime]
        if (keyframe != null) {
            return keyframe.vectorValue
        }

        if (clampedPlayTime >= durationMillis) {
            return targetValue
        } else if (clampedPlayTime <= 0) {
            return initialValue
        }

        init(initialValue, targetValue, initialVelocity)

        // Cannot be null after calling init()
        val valueVector = valueVector!!

        // ArcSpline is only initialized when necessary
        if (arcSpline !== EmptyArcSpline) {
            // ArcSpline requires eased play time in seconds
            val easedTime = getEasedTime(clampedPlayTime)

            val posArray = posArray
            arcSpline.getPos(time = easedTime, v = posArray)
            for (i in posArray.indices) {
                valueVector[i] = posArray[i]
            }
            return valueVector
        }

        // If ArcSpline is not required we do a simple linear interpolation
        val index = findEntryForTimeMillis(clampedPlayTime)

        // For the `lerp` method we need the eased time as a fraction
        val easedTime = getEasedTimeFromIndex(index, clampedPlayTime, true)

        val timestampStart = timestamps[index]
        val startKeyframe = keyframes[timestampStart]
        // Use initial value if it wasn't overwritten by the user
        // This is always the correct fallback assuming timestamps and keyframes were populated
        // as expected
        val startValue: V = startKeyframe?.vectorValue ?: initialValue

        val timestampEnd = timestamps[index + 1]
        val endKeyframe = keyframes[timestampEnd]
        // Use target value if it wasn't overwritten by the user
        // This is always the correct fallback assuming timestamps and keyframes were populated
        // as expected
        val endValue: V = endKeyframe?.vectorValue ?: targetValue

        for (i in 0 until valueVector.size) {
            valueVector[i] = lerp(startValue[i], endValue[i], easedTime)
        }
        return valueVector
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis)
        if (clampedPlayTime < 0L) {
            return initialVelocity
        }

        init(initialValue, targetValue, initialVelocity)

        // Cannot be null after calling init()
        val velocityVector = velocityVector!!

        // ArcSpline is only initialized when necessary
        if (arcSpline !== EmptyArcSpline) {
            val easedTime = getEasedTime(clampedPlayTime.toInt())
            val slopeArray = slopeArray
            arcSpline.getSlope(time = easedTime, v = slopeArray)
            for (i in slopeArray.indices) {
                velocityVector[i] = slopeArray[i]
            }
            return velocityVector
        }

        // Velocity calculation when ArcSpline is not used
        val startNum =
            getValueFromMillis(clampedPlayTime - 1, initialValue, targetValue, initialVelocity)
        val endNum = getValueFromMillis(clampedPlayTime, initialValue, targetValue, initialVelocity)
        for (i in 0 until startNum.size) {
            velocityVector[i] = (startNum[i] - endNum[i]) * 1000f
        }
        return velocityVector
    }

    private fun getEasedTime(timeMillis: Int): Float {
        // There's no promise on the nature of the given time, so we need to search for the correct
        // time range at every call
        val index = findEntryForTimeMillis(timeMillis)
        return getEasedTimeFromIndex(index, timeMillis, false)
    }

    private fun getEasedTimeFromIndex(index: Int, timeMillis: Int, asFraction: Boolean): Float {
        if (index >= timestamps.lastIndex) {
            // Return the same value. This may only happen at the end of the animation.
            return timeMillis.toFloat() / SecondsToMillis
        }
        val timeMin = timestamps[index]
        val timeMax = timestamps[index + 1]

        if (timeMillis == timeMin) {
            return timeMin.toFloat() / SecondsToMillis
        }

        val timeRange = timeMax - timeMin
        val easing = keyframes[timeMin]?.easing ?: defaultEasing
        val rawFraction = (timeMillis - timeMin).toFloat() / timeRange
        val easedFraction = easing.transform(rawFraction)

        if (asFraction) {
            return easedFraction
        }
        return (timeRange * easedFraction + timeMin) / SecondsToMillis
    }

    /**
     * Returns the entry index such that:
     *
     * [timeMillis] >= Entry(i).key && [timeMillis] < Entry(i+1).key
     */
    private fun findEntryForTimeMillis(timeMillis: Int): Int {
        val index = timestamps.binarySearch(timeMillis)
        return if (index < -1) -(index + 2) else index
    }
}

internal data class VectorizedKeyframeSpecElementInfo<V : AnimationVector>(
    val vectorValue: V,
    val easing: Easing,
    val arcMode: ArcMode
)

/**
 * Interpolation mode for Arc-based animation spec.
 *
 * @see ArcAbove
 * @see ArcBelow
 * @see ArcLinear
 * @see ArcAnimationSpec
 */
@JvmInline
public value class ArcMode internal constructor(internal val value: Int) {

    public companion object {
        /**
         * Interpolates using a quarter of an Ellipse where the curve is "above" the center of the
         * Ellipse.
         */
        public val ArcAbove: ArcMode = ArcMode(ArcSplineArcAbove)

        /**
         * Interpolates using a quarter of an Ellipse where the curve is "below" the center of the
         * Ellipse.
         */
        public val ArcBelow: ArcMode = ArcMode(ArcSplineArcBelow)

        /**
         * An [ArcMode] that forces linear interpolation.
         *
         * You'll likely only use this mode within a keyframe.
         */
        public val ArcLinear: ArcMode = ArcMode(ArcSplineArcStartLinear)
    }
}

/**
 * [VectorizedSnapSpec] immediately snaps the animating value to the end value.
 *
 * @param delayMillis the amount of time (in milliseconds) that the animation should wait before it
 *   starts. Defaults to 0.
 */
public class VectorizedSnapSpec<V : AnimationVector>(override val delayMillis: Int = 0) :
    VectorizedDurationBasedAnimationSpec<V> {

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return if (playTimeNanos < delayMillis * MillisToNanos) {
            initialValue
        } else {
            targetValue
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return initialVelocity
    }

    override val durationMillis: Int
        get() = 0
}

/**
 * This animation takes another [VectorizedDurationBasedAnimationSpec] and plays it __infinite__
 * times.
 *
 * initialStartOffset can be used to either delay the start of the animation or to fast forward the
 * animation to a given play time. This start offset will **not** be repeated, whereas the delay in
 * the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @param animation the [VectorizedAnimationSpec] describing each repetition iteration.
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
public class VectorizedInfiniteRepeatableSpec<V : AnimationVector>(
    private val animation: VectorizedDurationBasedAnimationSpec<V>,
    private val repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
) : VectorizedAnimationSpec<V> {
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message =
            "This method has been deprecated in favor of the constructor that" +
                " accepts start offset."
    )
    public constructor(
        animation: VectorizedDurationBasedAnimationSpec<V>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(animation, repeatMode, StartOffset(0))

    override val isInfinite: Boolean
        get() = true

    /** Single iteration duration */
    internal val durationNanos: Long =
        (animation.delayMillis + animation.durationMillis) * MillisToNanos

    private val initialOffsetNanos = initialStartOffset.value * MillisToNanos

    private fun repetitionPlayTimeNanos(playTimeNanos: Long): Long {
        if (playTimeNanos + initialOffsetNanos <= 0) {
            return 0
        } else {
            val postOffsetPlayTimeNanos = playTimeNanos + initialOffsetNanos
            val repeatsCount = postOffsetPlayTimeNanos / durationNanos
            if (repeatMode == RepeatMode.Restart || repeatsCount % 2 == 0L) {
                return postOffsetPlayTimeNanos - repeatsCount * durationNanos
            } else {
                return (repeatsCount + 1) * durationNanos - postOffsetPlayTimeNanos
            }
        }
    }

    private fun repetitionStartVelocity(
        playTimeNanos: Long,
        start: V,
        startVelocity: V,
        end: V
    ): V =
        if (playTimeNanos + initialOffsetNanos > durationNanos) {
            // Start velocity of the 2nd and subsequent iteration will be the velocity at the end
            // of the first iteration, instead of the initial velocity.
            animation.getVelocityFromNanos(
                playTimeNanos = durationNanos - initialOffsetNanos,
                initialValue = start,
                targetValue = end,
                initialVelocity = startVelocity
            )
        } else {
            startVelocity
        }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getValueFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getVelocityFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        Long.MAX_VALUE
}

/**
 * This animation takes another [VectorizedDurationBasedAnimationSpec] and plays it [iterations]
 * times. For infinitely repeating animation spec, [VectorizedInfiniteRepeatableSpec] is
 * recommended.
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * initialStartOffset can be used to either delay the start of the animation or to fast forward the
 * animation to a given play time. This start offset will **not** be repeated, whereas the delay in
 * the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @param iterations the count of iterations. Should be at least 1.
 * @param animation the [VectorizedAnimationSpec] describing each repetition iteration.
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
public class VectorizedRepeatableSpec<V : AnimationVector>(
    private val iterations: Int,
    private val animation: VectorizedDurationBasedAnimationSpec<V>,
    private val repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
) : VectorizedFiniteAnimationSpec<V> {
    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message =
            "This method has been deprecated in favor of the constructor that accepts" +
                " start offset."
    )
    public constructor(
        iterations: Int,
        animation: VectorizedDurationBasedAnimationSpec<V>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(iterations, animation, repeatMode, StartOffset(0))

    init {
        if (iterations < 1) {
            throw IllegalArgumentException("Iterations count can't be less than 1")
        }
    }

    // Per-iteration duration
    internal val durationNanos: Long =
        (animation.delayMillis + animation.durationMillis) * MillisToNanos

    // Fast forward amount. Delay type => negative offset
    private val initialOffsetNanos = initialStartOffset.value * MillisToNanos

    private fun repetitionPlayTimeNanos(playTimeNanos: Long): Long {
        if (playTimeNanos + initialOffsetNanos <= 0) {
            return 0
        } else {
            val postOffsetPlayTimeNanos = playTimeNanos + initialOffsetNanos
            val repeatsCount = min(postOffsetPlayTimeNanos / durationNanos, iterations - 1L)
            return if (repeatMode == RepeatMode.Restart || repeatsCount % 2 == 0L) {
                postOffsetPlayTimeNanos - repeatsCount * durationNanos
            } else {
                (repeatsCount + 1) * durationNanos - postOffsetPlayTimeNanos
            }
        }
    }

    private fun repetitionStartVelocity(
        playTimeNanos: Long,
        start: V,
        startVelocity: V,
        end: V
    ): V =
        if (playTimeNanos + initialOffsetNanos > durationNanos) {
            // Start velocity of the 2nd and subsequent iteration will be the velocity at the end
            // of the first iteration, instead of the initial velocity.
            getVelocityFromNanos(durationNanos - initialOffsetNanos, start, startVelocity, end)
        } else startVelocity

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getValueFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return animation.getVelocityFromNanos(
            repetitionPlayTimeNanos(playTimeNanos),
            initialValue,
            targetValue,
            repetitionStartVelocity(playTimeNanos, initialValue, initialVelocity, targetValue)
        )
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        return iterations * durationNanos - initialOffsetNanos
    }
}

/** Physics class contains a number of recommended configurations for physics animations. */
public object Spring {
    /** Stiffness constant for extremely stiff spring */
    public const val StiffnessHigh: Float = 10_000f

    /**
     * Stiffness constant for medium stiff spring. This is the default stiffness for spring force.
     */
    public const val StiffnessMedium: Float = 1500f

    /**
     * Stiffness constant for medium-low stiff spring. This is the default stiffness for springs
     * used in enter/exit transitions.
     */
    public const val StiffnessMediumLow: Float = 400f

    /** Stiffness constant for a spring with low stiffness. */
    public const val StiffnessLow: Float = 200f

    /** Stiffness constant for a spring with very low stiffness. */
    public const val StiffnessVeryLow: Float = 50f

    /**
     * Damping ratio for a very bouncy spring. Note for under-damped springs (i.e. damping ratio <
     * 1), the lower the damping ratio, the more bouncy the spring.
     */
    public const val DampingRatioHighBouncy: Float = 0.2f

    /**
     * Damping ratio for a medium bouncy spring. This is also the default damping ratio for spring
     * force. Note for under-damped springs (i.e. damping ratio < 1), the lower the damping ratio,
     * the more bouncy the spring.
     */
    public const val DampingRatioMediumBouncy: Float = 0.5f

    /**
     * Damping ratio for a spring with low bounciness. Note for under-damped springs (i.e. damping
     * ratio < 1), the lower the damping ratio, the higher the bounciness.
     */
    public const val DampingRatioLowBouncy: Float = 0.75f

    /**
     * Damping ratio for a spring with no bounciness. This damping ratio will create a critically
     * damped spring that returns to equilibrium within the shortest amount of time without
     * oscillating.
     */
    public const val DampingRatioNoBouncy: Float = 1f

    /** Default cutoff for rounding off physics based animations */
    public const val DefaultDisplacementThreshold: Float = 0.01f
}

/** Internal data structure for storing different FloatAnimations for different dimensions. */
internal interface Animations {
    operator fun get(index: Int): FloatAnimationSpec
}

/**
 * [VectorizedSpringSpec] uses spring animations to animate (each dimension of) [AnimationVector]s.
 */
public class VectorizedSpringSpec<V : AnimationVector>
private constructor(
    public val dampingRatio: Float,
    public val stiffness: Float,
    anims: Animations
) : VectorizedFiniteAnimationSpec<V> by VectorizedFloatAnimationSpec(anims) {

    /**
     * Creates a [VectorizedSpringSpec] that uses the same spring constants (i.e. [dampingRatio] and
     * [stiffness] on all dimensions. The optional [visibilityThreshold] defines when the animation
     * should be considered to be visually close enough to target to stop. By default,
     * [Spring.DefaultDisplacementThreshold] is used on all dimensions of the [AnimationVector].
     *
     * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
     * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
     * @param visibilityThreshold specifies the visibility threshold for each dimension.
     */
    public constructor(
        dampingRatio: Float = Spring.DampingRatioNoBouncy,
        stiffness: Float = Spring.StiffnessMedium,
        visibilityThreshold: V? = null
    ) : this(
        dampingRatio,
        stiffness,
        createSpringAnimations(visibilityThreshold, dampingRatio, stiffness)
    )
}

private fun <V : AnimationVector> createSpringAnimations(
    visibilityThreshold: V?,
    dampingRatio: Float,
    stiffness: Float
): Animations {
    return if (visibilityThreshold != null) {
        object : Animations {
            private val anims =
                Array(visibilityThreshold.size) { index ->
                    FloatSpringSpec(dampingRatio, stiffness, visibilityThreshold[index])
                }

            override fun get(index: Int): FloatSpringSpec = anims[index]
        }
    } else {
        object : Animations {
            private val anim = FloatSpringSpec(dampingRatio, stiffness)

            override fun get(index: Int): FloatSpringSpec = anim
        }
    }
}

/**
 * [VectorizedTweenSpec] animates a [AnimationVector] value by interpolating the start and end
 * value, in the given [durationMillis] using the given [easing] curve.
 *
 * @param durationMillis duration of the [VectorizedTweenSpec] animation. Defaults to
 *   [DefaultDurationMillis].
 * @param delayMillis the amount of time the animation should wait before it starts running, 0 by
 *   default.
 * @param easing the easing curve used by the animation. [FastOutSlowInEasing] by default.
 */
// TODO: Support different tween on different dimens
public class VectorizedTweenSpec<V : AnimationVector>(
    override val durationMillis: Int = DefaultDurationMillis,
    override val delayMillis: Int = 0,
    public val easing: Easing = FastOutSlowInEasing
) : VectorizedDurationBasedAnimationSpec<V> {

    private val anim =
        VectorizedFloatAnimationSpec<V>(FloatTweenSpec(durationMillis, delayMillis, easing))

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return anim.getValueFromNanos(playTimeNanos, initialValue, targetValue, initialVelocity)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        return anim.getVelocityFromNanos(playTimeNanos, initialValue, targetValue, initialVelocity)
    }
}

/**
 * A convenient implementation of [VectorizedFloatAnimationSpec] that turns a [FloatAnimationSpec]
 * into a multi-dimensional [VectorizedFloatAnimationSpec], by using the same [FloatAnimationSpec]
 * on each dimension of the [AnimationVector] that is being animated.
 */
public class VectorizedFloatAnimationSpec<V : AnimationVector>
internal constructor(private val anims: Animations) : VectorizedFiniteAnimationSpec<V> {
    private lateinit var valueVector: V
    private lateinit var velocityVector: V
    private lateinit var endVelocityVector: V

    /**
     * Creates a [VectorizedAnimationSpec] from a [FloatAnimationSpec]. The given
     * [FloatAnimationSpec] will be used to animate every dimension of the [AnimationVector].
     *
     * @param anim the animation spec for animating each dimension of the [AnimationVector]
     */
    public constructor(
        anim: FloatAnimationSpec
    ) : this(
        object : Animations {
            override fun get(index: Int): FloatAnimationSpec {
                return anim
            }
        }
    )

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
        }
        for (i in 0 until valueVector.size) {
            valueVector[i] =
                anims[i].getValueFromNanos(
                    playTimeNanos,
                    initialValue[i],
                    targetValue[i],
                    initialVelocity[i]
                )
        }
        return valueVector
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        if (!::velocityVector.isInitialized) {
            velocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until velocityVector.size) {
            velocityVector[i] =
                anims[i].getVelocityFromNanos(
                    playTimeNanos,
                    initialValue[i],
                    targetValue[i],
                    initialVelocity[i]
                )
        }
        return velocityVector
    }

    override fun getEndVelocity(initialValue: V, targetValue: V, initialVelocity: V): V {
        if (!::endVelocityVector.isInitialized) {
            endVelocityVector = initialVelocity.newInstance()
        }
        for (i in 0 until endVelocityVector.size) {
            endVelocityVector[i] =
                anims[i].getEndVelocity(initialValue[i], targetValue[i], initialVelocity[i])
        }
        return endVelocityVector
    }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        var maxDuration = 0L
        for (i in 0 until initialValue.size) {
            maxDuration =
                maxOf(
                    maxDuration,
                    anims[i].getDurationNanos(initialValue[i], targetValue[i], initialVelocity[i])
                )
        }
        return maxDuration
    }
}

private val EmptyIntArray: IntArray = IntArray(0)
private val EmptyFloatArray: FloatArray = FloatArray(0)
private val EmptyArcSpline =
    ArcSpline(IntArray(2), FloatArray(2), arrayOf(FloatArray(2), FloatArray(2)))
