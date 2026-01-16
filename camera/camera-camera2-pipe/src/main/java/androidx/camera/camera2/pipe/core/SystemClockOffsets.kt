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

package androidx.camera.camera2.pipe.core

import android.os.SystemClock

/**
 * Represents fixed, precomputed offsets for various Android system clocks and time sources.
 *
 * Generally everything in Camera operates in either REALTIME timestamps or MONOTONIC timestamps.
 * Most of the time, this is fine, but there are subtle problems because the two values can skew
 * over time and as the device sleeps. In general, while the camera is open, the device never sleeps
 * and so it's fine to cache these values while the camera is open. However, these skews must be
 * recomputed each time the process wakes up and the camera starts again.
 *
 * Additionally, which timebase the camera uses can change depending on what device it's running on.
 * While not documented, most cameras will operate on MONOTONIC timestamps unless they are
 * calibrated and report that the camera timestamps are "realtime".
 *
 * Source:
 * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/av/services/camera/libcameraservice/device3/Camera3Device.cpp;l=404
 * - See Android's Camera3Device.cpp -> getMonoToBoottimeOffset
 * - See Android's Thread.cpp -> adjustTimebaseOffset
 */
internal class SystemClockOffsets
private constructor(val realtimeNsToUtcMs: Long, val realtimeNsToMonotonicNs: Long) {
    companion object {
        private const val NS_PER_MS: Long = 1_000_000
        private const val NS_PER_MS_X_2: Long = NS_PER_MS * 2
        private const val MEASUREMENT_ITERATIONS: Int = 3

        /** Estimate system clock offsets by sampling and measuring the clock differences. */
        @JvmStatic
        fun estimate(): SystemClockOffsets {
            val realtimeNsToUtcMs = estimateRealtimeNsToUtcMs()
            val realtimeNsToMonotonicNs = estimateRealtimeNsToMonotonicNs()
            return SystemClockOffsets(realtimeNsToUtcMs, realtimeNsToMonotonicNs)
        }

        /** Create a set of fixed system clock offsets. */
        @JvmStatic
        fun fixed(realtimeNsToUtcMs: Long, realtimeNsToMonotonicNs: Long): SystemClockOffsets =
            SystemClockOffsets(realtimeNsToUtcMs, realtimeNsToMonotonicNs)

        @JvmStatic
        private fun estimateRealtimeNsToUtcMs(): Long {
            var realtimeNanosA: Long
            var realtimeNanosB: Long
            var utcMillis: Long

            // See class comment for a detailed description of this measurement approach.
            var bestDeltaNanos = Long.MAX_VALUE
            var offsetMillis: Long = 0
            for (i in 0..<MEASUREMENT_ITERATIONS) {
                // Nanoseconds since boot, (Including time spent in sleep)
                realtimeNanosA = SystemClock.elapsedRealtimeNanos()

                // Milliseconds since midnight, January 1, 1970 UTC
                // Note that this value can change if the users adjusts the system clock, or during
                // timezone changes. In general, it should not change while an app is being used,
                // although it's possible.
                utcMillis = System.currentTimeMillis()
                realtimeNanosB = SystemClock.elapsedRealtimeNanos()

                val deltaNanos = realtimeNanosB - realtimeNanosA
                if (deltaNanos < bestDeltaNanos) {
                    bestDeltaNanos = deltaNanos
                    // Since we have three measurements A, B, C, we compute the average between A
                    // and C, which gets us closer to B, then compute the difference between them to
                    // compute the offset:
                    //
                    // offset = ((A + C) / 2) - B
                    //
                    // Since the clock for B is in Milli's and A and C are Nanos, and because we
                    // need to work in millis for utcTime, we convert A and B to millis.
                    //
                    // offset = (((A + C) / 2) / Nanos/milli) - B
                    // offset = ((A + C) / (Nanos/milli * 2) - B
                    offsetMillis = ((realtimeNanosA + realtimeNanosB) / NS_PER_MS_X_2) - utcMillis
                }
            }
            return offsetMillis
        }

        @JvmStatic
        private fun estimateRealtimeNsToMonotonicNs(): Long {
            var monotonicNanosA: Long
            var monotonicNanosB: Long
            var elapsedRealTimeNanos: Long

            // Compute the delta by sampling several times, computing the delta between two
            // identical calls and assuming that the smaller the difference the more accurate the
            // skew is.
            //
            // While this may appear to be hacked together, there's no canonical method to get the
            // current skew between the clocks. Android itself has internal methods to compute clock
            // skew this way.
            var bestDeltaNanos = Long.MAX_VALUE
            var offsetNanos: Long = 0
            for (i in 0..<MEASUREMENT_ITERATIONS) {
                // Nanoseconds since boot, (Excluding time spent in sleep)
                monotonicNanosA = System.nanoTime()

                // Nanoseconds since boot (Including the time spent in sleep)
                elapsedRealTimeNanos = SystemClock.elapsedRealtimeNanos()
                monotonicNanosB = System.nanoTime()

                // Compute the delta between two clock measurements, A and B. The assumption is that
                // the smaller the difference, the closer the measurements are, and thus, the more
                // accurate they are. We compute this several times and pick the smallest delta.
                val deltaNanos = monotonicNanosB - monotonicNanosA
                if (deltaNanos < bestDeltaNanos) {
                    bestDeltaNanos = deltaNanos

                    // Since we have three measurements A, B, C, we compute the average between A
                    // and C, which gets us closer to B, then compute the difference between them to
                    // compute the offset: offset = B - ((A + C) / 2)
                    offsetNanos = elapsedRealTimeNanos - ((monotonicNanosA + monotonicNanosB) / 2)
                }
            }
            return offsetNanos
        }

        /** Convert a timestamp from "elapsedRealtimeNanos" to "uptimeMillis" * NS_PER_MS. */
        @JvmStatic
        fun SystemClockOffsets.realtimeNsToMonotonicNs(realtimeNs: Long): Long {
            return realtimeNs - this.realtimeNsToMonotonicNs
        }

        /** Convert a timestamp from "elapsedRealtimeNanos" to "uptimeMillis" */
        @JvmStatic
        fun SystemClockOffsets.realtimeNsToMonotonicMs(realtimeNs: Long): Long {
            return this.realtimeNsToMonotonicNs(realtimeNs) / NS_PER_MS
        }

        /** Convert a timestamp from "elapsedRealtimeNanos" to utc milliseconds. */
        @JvmStatic
        fun SystemClockOffsets.realtimeNsToUtcMs(realtimeNs: Long): Long {
            return (realtimeNs / NS_PER_MS) - this.realtimeNsToUtcMs
        }

        /** Convert a timestamp from "elapsedRealtime" to "uptimeMillis" * NS_PER_MS. */
        @JvmStatic
        fun SystemClockOffsets.realtimeMsToMonotonicNs(realtimeMs: Long): Long {
            return realtimeMs * NS_PER_MS - this.realtimeNsToMonotonicNs
        }

        /** Convert a timestamp from "elapsedRealtime" to "uptimeMillis". */
        @JvmStatic
        fun SystemClockOffsets.realtimeMsToMonotonicMs(realtimeMs: Long): Long {
            return this.realtimeMsToMonotonicNs(realtimeMs) / NS_PER_MS
        }

        /** Convert a timestamp in the "elapsedRealtimeNanos" to utc milliseconds. */
        @JvmStatic
        fun SystemClockOffsets.realtimeMsToUtcMs(realtimeMs: Long): Long {
            return realtimeMs - this.realtimeNsToUtcMs
        }

        /** Convert a timestamp in the "uptimeMillis * NS_PER_MS" to elapsedRealtimeNanos. */
        @JvmStatic
        fun SystemClockOffsets.monotonicNsToRealtimeNs(monotonicNs: Long): Long {
            return this.realtimeNsToMonotonicNs + monotonicNs
        }

        /** Convert a timestamp in the "uptimeMillis * NS_PER_MS" to elapsedRealtime. */
        @JvmStatic
        fun SystemClockOffsets.monotonicNsToRealtimeMs(monotonicNs: Long): Long {
            return this.monotonicNsToRealtimeNs(monotonicNs) / NS_PER_MS
        }

        /** Convert a timestamp in the "uptimeMillis * NS_PER_MS" to utc milliseconds. */
        @JvmStatic
        fun SystemClockOffsets.monotonicNsToUtcMs(monotonicNs: Long): Long {
            return this.realtimeNsToUtcMs(this.monotonicNsToRealtimeNs(monotonicNs))
        }

        /** Convert a timestamp in the "uptimeMillis" timebase to elapsedRealtimeNanos. */
        @JvmStatic
        fun SystemClockOffsets.monotonicMsToRealtimeNs(monotonicMs: Long): Long {
            return this.realtimeNsToMonotonicNs + (monotonicMs * NS_PER_MS)
        }

        /** Convert a timestamp in the "uptimeMillis" timebase to elapsedRealtimeNanos. */
        @JvmStatic
        fun SystemClockOffsets.monotonicMsToRealtimeMs(monotonicMs: Long): Long {
            return this.monotonicMsToRealtimeNs(monotonicMs) / NS_PER_MS
        }

        /** Convert a timestamp in the "uptimeMillis" timebase to utc milliseconds. */
        @JvmStatic
        fun SystemClockOffsets.monotonicMsToUtcMs(monotonicMs: Long): Long {
            return this.realtimeNsToUtcMs(this.monotonicMsToRealtimeNs(monotonicMs))
        }
    }
}
