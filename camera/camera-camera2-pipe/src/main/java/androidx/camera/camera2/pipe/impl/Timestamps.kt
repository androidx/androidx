/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.pipe.impl

import android.os.SystemClock

/**
 * A nanosecond timestamp
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class TimestampNs constructor(val value: Long) {
    inline operator fun minus(other: TimestampNs): DurationNs =
        DurationNs(value - other.value)

    inline operator fun plus(other: DurationNs): TimestampNs =
        TimestampNs(value + other.value)
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class DurationNs(val value: Long) {
    inline operator fun minus(other: DurationNs): DurationNs =
        DurationNs(value - other.value)

    inline operator fun plus(other: DurationNs): DurationNs =
        DurationNs(value + other.value)

    inline operator fun plus(other: TimestampNs): TimestampNs =
        TimestampNs(value + other.value)
}

object Timestamps {
    inline fun now(): TimestampNs = TimestampNs(SystemClock.elapsedRealtimeNanos())

    inline fun DurationNs.formatNs() = "$this ns"
    inline fun DurationNs.formatMs(decimals: Int = 3) =
        "%.${decimals}f ms".format(null, this.value / 1_000_000.0)

    inline fun TimestampNs.formatNs() = "$this ns"
    inline fun TimestampNs.formatMs() = "${this.value / 1_000_000} ms"

    inline fun TimestampNs.measureNow(): DurationNs = now() - this
}
