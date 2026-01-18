/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import androidx.annotation.VisibleForTesting
import kotlinx.atomicfu.atomic

/**
 * Stateful comparison tool for matching output numbers.
 *
 * Exact matching will only honor timestamps that match the nanosecond timestamp exactly. Fuzzy
 * matching will match timestamps within an error margin. Fuzzy matching with offset will match
 * timestamps within an error margin and offset applied.
 *
 * Offset should be given such that `sensorTimestampNs + offsetNs = imageTimestampNs`.
 *
 * @param initialOffset given such that sensorTimestampNs + offsetNs = imageTimestampNs
 * @param errorDelta given such that sensorTimestampNs + offsetNs = imageTimestampNs +- errorNs
 */
internal class OutputMatcher
@VisibleForTesting
internal constructor(initialOffset: Long = 0L, private val errorDelta: Long) {
    private val currentOffset = atomic<Long>(initialOffset)

    /**
     * Check to see if a sensor timestamp is equal to an image timestamp. If they match, the offset
     * is updated.
     */
    fun fuzzyEqual(cameraOutputNumber: Long, outputNumber: Long): Boolean {
        val offsetNs = currentOffset.value
        val delta = cameraOutputNumber - outputNumber + offsetNs
        if (delta == 0L) {
            // Timestamps match exactly
            return true
        } else if (errorDelta != 0L && delta < errorDelta && delta > -errorDelta) {
            // Inexact match within error margins: update currentOffsetNs
            currentOffset.compareAndSet(offsetNs, offsetNs - delta)
            return true
        }
        // Did not match
        return false
    }

    /** Check to see if an image timestamp is less than a sensor timestamp. */
    fun fuzzyLessThan(cameraOutputNumber: Long, outputNumber: Long): Boolean {
        // Example:
        // img   ssr   off   error   v  <=
        // 0   - 100 - 105 + 6 =  -199  true
        // 198 - 100 - 105 + 6 =    -1  true  // less than
        // 205 - 100 - 105 + 6 =     6  false // exact equals
        // 207 - 100 - 105 + 6 =     8  false // fuzzy equals
        // 215 - 100 - 105 + 6 =    16  false // greater than
        return outputNumber - cameraOutputNumber - currentOffset.value + errorDelta < 0
    }

    /** Check to see if an image timestamp is less than a sensor timestamp. */
    fun fuzzyLessThanOrEqual(cameraOutputNumber: Long, outputNumber: Long): Boolean {
        // Example:
        // img   ssr   off   error   v  <=
        // 0   - 100 - 105 - 6 =  -211  true
        // 198 - 100 - 105 - 6 =   -13  true  // less than
        // 205 - 100 - 105 - 6 =    -6  true  // exact equals
        // 207 - 100 - 105 - 6 =    -4  true  // fuzzy equals
        // 215 - 100 - 105 - 6 =     4  false // greater than
        return outputNumber - cameraOutputNumber - currentOffset.value - errorDelta <= 0
    }

    fun fuzzyGreaterThanOrEqual(sensorTimestampNs: Long, imageTimestampNs: Long): Boolean {
        return !fuzzyLessThan(sensorTimestampNs, imageTimestampNs)
    }

    fun fuzzyGreaterThan(sensorTimestampNs: Long, imageTimestampNs: Long): Boolean {
        return !fuzzyLessThanOrEqual(sensorTimestampNs, imageTimestampNs)
    }

    companion object {
        val EXACT = OutputMatcher(0, 0)

        /**
         * Create an [OutputMatcher] designed to match based on nanosecond-precision timestamps that
         * may skew over time and expects the frame rate to be less than 60fps.
         */
        fun forTimestampsWithOffset(
            initialOffset: Long,
            errorDelta: Long = DEFAULT_ERROR_NS,
        ): OutputMatcher = OutputMatcher(initialOffset, errorDelta)

        private const val NS_PER_SECOND: Long = 1000000000
        private const val ERROR_DETECTION_FPS: Long = 60

        // This default assumes that a camera will run at (at most) 60fps
        private const val DEFAULT_ERROR_NS = NS_PER_SECOND / (ERROR_DETECTION_FPS * 2)
    }
}
