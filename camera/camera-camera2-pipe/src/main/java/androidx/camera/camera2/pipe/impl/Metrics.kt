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
import java.util.Locale

object Metrics {
    inline fun monotonicNanos(): Long = SystemClock.elapsedRealtimeNanos()
    inline fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
    inline fun nanosToMillis(duration: Long): Long = duration / 1_000_000
    inline fun nanosToMillisDouble(duration: Long): Double = duration.toDouble() / 1_000_000.0
}

inline fun Double.formatMilliTime(decimals: Int = 4) = "%.${decimals}f ms".format(Locale.ROOT, this)
inline fun Long.formatNanoTime(decimals: Int = 4) =
    (this / 1_000_000.0).formatMilliTime(decimals)