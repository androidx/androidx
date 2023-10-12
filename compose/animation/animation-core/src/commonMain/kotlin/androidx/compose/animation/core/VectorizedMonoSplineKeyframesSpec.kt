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

import androidx.compose.ui.util.fastForEachIndexed

/**
 * Implementation of [VectorizedMonoSplineKeyframesSpec] using [MonoSpline].
 */
// TODO(b/292114811): Find a way to comply and preserve insertion order
@SuppressWarnings("PrimitiveInCollection")
@ExperimentalAnimationSpecApi
internal class VectorizedMonoSplineKeyframesSpec<V : AnimationVector>(
    private val keyframes: Map<Int, V>,
    override val durationMillis: Int,
    override val delayMillis: Int = 0
) : VectorizedDurationBasedAnimationSpec<V> {
    private lateinit var valueVector: V
    private lateinit var velocityVector: V

    // Objects for MonoSpline
    private lateinit var lastInitialValue: V
    private lateinit var lastTargetValue: V
    private lateinit var posArray: FloatArray
    private lateinit var slopeArray: FloatArray
    private lateinit var monoSpline: MonoSpline

    private fun init(initialValue: V, targetValue: V, initialVelocity: V) {
        if (!::valueVector.isInitialized) {
            valueVector = initialValue.newInstance()
            velocityVector = initialVelocity.newInstance()
        }
        if (!::monoSpline.isInitialized ||
            lastInitialValue != initialValue || lastTargetValue != targetValue
        ) {
            lastInitialValue = initialValue
            lastTargetValue = targetValue
            posArray = FloatArray(initialValue.size)
            slopeArray = FloatArray(initialVelocity.size)

            val arraySize = keyframes.size + 2
            val times = FloatArray(arraySize)
            val values = MutableList(arraySize) {
                FloatArray(initialValue.size)
            }

            // Initialize start/end timestamp and values
            times[0] = 0f
            times[arraySize - 1] = durationMillis.toFloat() / SecondsToMillis
            val vectorStart = values[0]
            val vectorEnd = values[arraySize - 1]
            for (i in 0 until initialValue.size) {
                vectorStart[i] = initialValue[i]
                vectorEnd[i] = targetValue[i]
            }

            // Need to set times/values arrays in the order of the timestamps
            keyframes.entries.sortedBy { it.key }
                .fastForEachIndexed { index, (frameMillis, valueVector) ->
                    times[index + 1] = frameMillis.toFloat() / SecondsToMillis
                    val vector = values[index + 1]
                    for (i in vector.indices) {
                        vector[i] = valueVector[i]
                    }
                }
            monoSpline = MonoSpline(times, values)
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
            return keyframes[clampedPlayTime]!!
        }

        if (clampedPlayTime >= durationMillis) {
            return targetValue
        } else if (clampedPlayTime <= 0) return initialValue

        init(initialValue, targetValue, initialVelocity)

        monoSpline.getPos(
            t = clampedPlayTime.toFloat() / SecondsToMillis,
            v = posArray
        )
        for (i in posArray.indices) {
            valueVector[i] = posArray[i]
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

        monoSpline.getSlope(
            time = clampedPlayTime.toFloat() / SecondsToMillis,
            v = slopeArray
        )
        for (i in slopeArray.indices) {
            velocityVector[i] = slopeArray[i]
        }
        return velocityVector
    }
}
