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

@file:JvmName("Timestamps")
@file:Suppress("NOTHING_TO_INLINE")

package androidx.ui.core

/**
 * Convert a [Long] value in nanoseconds to a [Timestamp].
 */
// TODO(inline)
fun Long.nanosecondsToTimestamp() = Timestamp(this)

/**
 * Convert a [Long] value in milliseconds to a [Timestamp].
 */
// TODO(inline)
fun Long.millisecondsToTimestamp() = Timestamp(this * NanosecondsPerMillisecond)

/**
 * Convert a [Long] value in seconds to a [Timestamp].
 */
// TODO(inline)
fun Long.secondsToTimestamp() = Timestamp(this * NanosecondsPerSecond)

/**
 * A single point in time expressed in [nanoseconds]. Compare to [Duration].
 * [Timestamp] permits arithmetic between absolute timestamps from the same time base
 * yielding [Duration] where deltas from two timestamps are involved.
 *
 * The time base of any given `Timestamp` is defined by the context where the `Timestamp`
 * was obtained.
 */
data class Timestamp(val nanoseconds: Long) : Comparable<Timestamp> {

    /**
     * Adds a [Duration] to this timestamp and returns the result.
     */
    operator fun plus(duration: Duration) = Timestamp(nanoseconds + duration.nanoseconds)

    /**
     * Subtracts a [Duration] from this timestamp and returns the result.
     */
    operator fun minus(duration: Duration) = Timestamp(nanoseconds - duration.nanoseconds)

    /**
     * Returns the [Duration] between this timestamp and another.
     */
    operator fun minus(other: Timestamp) = Duration(nanoseconds - other.nanoseconds)

    override fun compareTo(other: Timestamp): Int = when {
        nanoseconds < other.nanoseconds -> -1
        nanoseconds == other.nanoseconds -> 0
        else -> 1
    }
}
