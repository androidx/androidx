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

/**
 * Implementation of [VectorizedMonoSplineKeyframesSpec] using [MonoSpline].
 */
@ExperimentalAnimationSpecApi
internal class VectorizedMonoSplineKeyframesSpec<V : AnimationVector>(
    private val timestamps: IntList,
    private val keyframes: IntObjectMap<Pair<V, Easing>>,
    override val durationMillis: Int,
    override val delayMillis: Int,
    private val periodicBias: Float,
) : VectorizedDurationBasedAnimationSpec<V> {
    // Objects initialized lazily once
    private lateinit var valueVector: V
    private lateinit var velocityVector: V

    // Time values passed to MonoSpline.
    private lateinit var times: FloatArray

    // Objects for MonoSpline
    private lateinit var monoSpline: MonoSpline
    // [values] are not modified by MonoSpline so we can safely re-use it to re-instantiate it
    private lateinit var values: Array<FloatArray>
    private var lastInitialValue: V? = null
    private var lastTargetValue: V? = null

    private fun init(initialValue: V, targetValue: V, initialVelocity: V) {
        // Only need to initialize once
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
            velocityVector = initialVelocity.newInstance()

            times = FloatArray(timestamps.size) {
                timestamps[it].toFloat() / SecondsToMillis
            }
        }

        // Need to re-initialize based on initial/target values
        if (!::monoSpline.isInitialized ||
            lastInitialValue != initialValue || lastTargetValue != targetValue
        ) {
            val initialChanged = lastInitialValue != initialValue
            val targetChanged = lastTargetValue != targetValue
            lastInitialValue = initialValue
            lastTargetValue = targetValue

            val dimension = initialValue.size

            if (!::values.isInitialized) {
                values = Array(timestamps.size) {
                    when (val timestamp = timestamps[it]) {
                        // Start (zero) and end (durationMillis) may not have been declared in keyframes
                        0 -> {
                            if (!keyframes.contains(timestamp)) {
                                FloatArray(dimension, initialValue::get)
                            } else {
                                FloatArray(dimension, keyframes[timestamp]!!.first::get)
                            }
                        }

                        durationMillis -> {
                            if (!keyframes.contains(timestamp)) {
                                FloatArray(dimension, targetValue::get)
                            } else {
                                FloatArray(dimension, keyframes[timestamp]!!.first::get)
                            }
                        }

                        // All other values are guaranteed to exist
                        else -> FloatArray(dimension, keyframes[timestamp]!!.first::get)
                    }
                }
            } else {
                // We can re-use most of the objects. Only the start and end may need to be replaced
                if (initialChanged && !keyframes.contains(0)) {
                    values[timestamps.binarySearch(0)] = FloatArray(dimension, initialValue::get)
                }
                if (targetChanged && !keyframes.contains(durationMillis)) {
                    values[timestamps.binarySearch(durationMillis)] =
                        FloatArray(dimension, targetValue::get)
                }
            }
            monoSpline = MonoSpline(times, values, periodicBias)
        }
    }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis).toInt()
        // If there is a key frame defined with the given time stamp, return that value
        if (keyframes.containsKey(clampedPlayTime)) {
            return keyframes[clampedPlayTime]!!.first
        }

        if (clampedPlayTime >= durationMillis) {
            return targetValue
        } else if (clampedPlayTime <= 0) return initialValue

        init(initialValue, targetValue, initialVelocity)

        // There's no promise on the nature of the given time, so we need to search for the correct
        // time range at every call
        val index = findEntryForTimeMillis(clampedPlayTime)

        monoSpline.getPos(
            index = index,
            time = getEasedTimeFromIndex(index, clampedPlayTime),
            v = valueVector
        )
        return valueVector
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        val playTimeMillis = playTimeNanos / MillisToNanos
        val clampedPlayTime = clampPlayTime(playTimeMillis).toInt()
        if (clampedPlayTime < 0) {
            return initialVelocity
        }

        init(initialValue, targetValue, initialVelocity)

        // There's no promise on the nature of the given time, so we need to search for the correct
        // time range at every call
        val index = findEntryForTimeMillis(clampedPlayTime)

        monoSpline.getSlope(
            index = index,
            time = getEasedTimeFromIndex(index, clampedPlayTime),
            v = velocityVector
        )
        return velocityVector
    }

    private fun getEasing(index: Int): Easing {
        val timestamp = timestamps[index]
        // Default to LinearEasing when absent
        return keyframes[timestamp]?.second ?: LinearEasing
    }

    private fun getEasedTimeFromIndex(
        index: Int,
        timeMillis: Int
    ): Float {
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
        val easing = getEasing(index)
        val rawFraction = (timeMillis - timeMin).toFloat() / timeRange
        val easedFraction = easing.transform(rawFraction)

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
