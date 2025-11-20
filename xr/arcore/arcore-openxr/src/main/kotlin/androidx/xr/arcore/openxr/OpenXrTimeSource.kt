/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import kotlin.time.AbstractLongTimeSource
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit

/** A time source that uses the native OpenXR time as the clock. */
internal class OpenXrTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
    // Should be initialized when the base class initializes its [zero] field.
    private var zeroXrTime: Long? = null
    private lateinit var zeroTimeMark: ComparableTimeMark

    override fun read(): Long {
        val reading = nativeGetXrTimeNow()
        zeroXrTime = zeroXrTime ?: reading
        return reading
    }

    override fun markNow(): ComparableTimeMark {
        val timeMark = super.markNow()
        zeroTimeMark = if (::zeroTimeMark.isInitialized) zeroTimeMark else timeMark
        return timeMark
    }

    /**
     * Returns the XrTime corresponding to [timeMark]. This calculation is only valid if [timeMark]
     * was created with this [TimeSource].
     */
    internal fun getXrTime(timeMark: ComparableTimeMark): Long {
        check(zeroXrTime != null && ::zeroTimeMark.isInitialized)
        val elapsedNanos = (timeMark - zeroTimeMark).inWholeNanoseconds
        return zeroXrTime!! + elapsedNanos
    }

    private external fun nativeGetXrTimeNow(): Long
}
