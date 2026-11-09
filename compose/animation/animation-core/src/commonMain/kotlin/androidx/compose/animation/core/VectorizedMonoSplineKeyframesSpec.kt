/*
 * Copyright 2023 The Android Open Source Project
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

/** Implementation of [VectorizedMonoSplineKeyframesSpec] using [MonoSpline]. */
internal class VectorizedMonoSplineKeyframesSpec<V : AnimationVector>(
    private val timestamps: IntList,
    private val keyframes: IntObjectMap<Pair<V, Easing>>,
    override val durationMillis: Int,
    override val delayMillis: Int,
    private val periodicBias: Float,
) : VectorizedDurationBasedAnimationSpec<V> {
    // Objects initialized lazily once
    private var valueVector: V? = null
    private var velocityVector: V? = null

    // Time values passed to MonoSpline.
    private lateinit var times: FloatArray

    // Objects for MonoSpline
    private var monoSpline: MonoSpline? = null
    // [values] are not modified by MonoSpline so we can safely re-use it to re-instantiate it
    private var values: Array<FloatArray>? = null
    private var lastInitialValue: V? = null
    private var lastTargetValue: V? = null

    private fun init(initialValue: V, targetValue: V, initialVelocity: V) {
        // Only need to initialize once
        if (valueVector == null) {
            valueVector = initialValue.newInstance()
            velocityVector = initialVelocity.newInstance()

            times = FloatArray(timestamps.size) { timestamps[it].toFloat() / SecondsToMillis }
        }

        // Need to re-initialize based on initial/target values
        if (
            monoSpline == null || lastInitialValue != initialValue || lastTargetValue != targetValue
        ) {
            val initialChanged = lastInitialValue != initialValue
            val targetChanged = lastTargetValue != targetValue
            lastInitialValue = initialValue
            lastTargetValue = targetValue

            val dimension = initialValue.size

            var values = values
            if (values == null) {
                values =
                    Array(timestamps.size) {
                        val timestamp = timestamps[it]
                        val keyframe = keyframes[timestamp]
                        if (timestamp == 0 && keyframe == null) {
                            FloatArray(dimension) { i -> initialValue[i] }
                        } else if (timestamp == durationMillis && keyframe == null) {
                            FloatArray(dimension) { i -> targetValue[i] }
                        } else {
                            val vectorValue = keyframe!!.first
                            FloatArray(dimension) { i -> vectorValue[i] }
                        }
                    }
                this.values = values
            } else {
                // We can re-use most of the objects. Only the start and end may need to be replaced
                if (initialChanged && !keyframes.contains(0)) {
                    val index = timestamps.binarySearch(0)
                    values[index] = FloatArray(dimension) { i -> initialValue[i] }
                }
                if (targetChanged && !keyframes.contains(durationMillis)) {
                    val index = timestamps.binarySearch(durationMillis)
                    values[index] = FloatArray(dimension) { i -> targetValue[i] }
                }
            }
            monoSpline = MonoSpline(times, values, periodicBias)
        }
    }

    @OptIn(ExperimentalAnimationCoreApi::class)
    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        val clampedPlayTimeNanos =
            if (ComposeAnimationCoreFlags.isNanosecondAnimationSpecEnabled) {
                clampPlayTimeNanos(playTimeNanos)
            } else {
                clampPlayTimeMillis(playTimeNanos / MillisToNanos) * MillisToNanos
            }
        // If there is a key frame defined with the given time stamp, return that value
        if (clampedPlayTimeNanos % MillisToNanos == 0L) {
            val keyframe = keyframes[(clampedPlayTimeNanos / MillisToNanos).toInt()]
            if (keyframe != null) {
                return keyframe.first
            }
        }

        if (clampedPlayTimeNanos >= durationMillis * MillisToNanos) {
            return targetValue
        } else if (clampedPlayTimeNanos <= 0L) {
            return initialValue
        }

        init(initialValue, targetValue, initialVelocity)

        // There's no promise on the nature of the given time, so we need to search for the correct
        // time range at every call
        val index = findEntryForTimeMillis((clampedPlayTimeNanos / MillisToNanos).toInt())

        val valueVector = valueVector!!
        monoSpline!!.getPos(
            index = index,
            time = getEasedTimeFromIndexNanos(index, clampedPlayTimeNanos),
            v = valueVector,
        )
        return valueVector
    }

    @OptIn(ExperimentalAnimationCoreApi::class)
    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        val clampedPlayTimeNanos =
            if (ComposeAnimationCoreFlags.isNanosecondAnimationSpecEnabled) {
                clampPlayTimeNanos(playTimeNanos)
            } else {
                clampPlayTimeMillis(playTimeNanos / MillisToNanos) * MillisToNanos
            }

        init(initialValue, targetValue, initialVelocity)

        // There's no promise on the nature of the given time, so we need to search for the correct
        // time range at every call
        val index = findEntryForTimeMillis((clampedPlayTimeNanos / MillisToNanos).toInt())

        val velocityVector = velocityVector!!
        monoSpline!!.getSlope(
            index = index,
            time = getEasedTimeFromIndexNanos(index, clampedPlayTimeNanos),
            v = velocityVector,
        )
        return velocityVector
    }

    private fun getEasing(index: Int): Easing {
        val timestamp = timestamps[index]
        // Default to LinearEasing when absent
        return keyframes[timestamp]?.second ?: LinearEasing
    }

    private fun getEasedTimeFromIndexNanos(index: Int, timeNanos: Long): Float {
        if (index >= timestamps.lastIndex) {
            // Return the same value. This may only happen at the end of the animation.
            return (timeNanos.toDouble() / SecondsToNanos).toFloat()
        }
        val timeMinMillis = timestamps[index]
        val timeMaxMillis = timestamps[index + 1]
        val timeMinNanos = timeMinMillis * MillisToNanos

        if (timeNanos == timeMinNanos) {
            return timeMinMillis.toFloat() / SecondsToMillis
        }

        val timeRangeMillis = timeMaxMillis - timeMinMillis
        val timeRangeNanos = timeRangeMillis * MillisToNanos
        val easing = getEasing(index)
        val rawFraction = ((timeNanos - timeMinNanos).toDouble() / timeRangeNanos).toFloat()
        val easedFraction = easing.transform(rawFraction)

        return (timeRangeMillis * easedFraction + timeMinMillis) / SecondsToMillis
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
