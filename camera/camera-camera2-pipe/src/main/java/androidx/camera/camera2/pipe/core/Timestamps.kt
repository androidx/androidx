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
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.core

import android.os.SystemClock
import androidx.annotation.RequiresApi

/**
 * A nanosecond timestamp
 */
@JvmInline
public value class TimestampNs constructor(public val value: Long) {
    public inline operator fun minus(other: TimestampNs): DurationNs =
        DurationNs(value - other.value)

    public inline operator fun plus(other: DurationNs): TimestampNs =
        TimestampNs(value + other.value)
}

@JvmInline
public value class DurationNs(public val value: Long) {
    public inline operator fun minus(other: DurationNs): DurationNs =
        DurationNs(value - other.value)

    public inline operator fun plus(other: DurationNs): DurationNs =
        DurationNs(value + other.value)

    public inline operator fun plus(other: TimestampNs): TimestampNs =
        TimestampNs(value + other.value)
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public object Timestamps {
    public inline fun now(): TimestampNs = TimestampNs(SystemClock.elapsedRealtimeNanos())

    public inline fun DurationNs.formatNs(): String = "$this ns"
    public inline fun DurationNs.formatMs(decimals: Int = 3): String =
        "%.${decimals}f ms".format(null, this.value / 1_000_000.0)

    public inline fun TimestampNs.formatNs(): String = "$this ns"
    public inline fun TimestampNs.formatMs(): String = "${this.value / 1_000_000} ms"

    public inline fun TimestampNs.measureNow(): DurationNs = now() - this
}
