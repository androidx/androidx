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

package androidx.benchmark

import androidx.annotation.RestrictTo
import java.io.Closeable

/**
 * Exposes CPU counters from perf_event_open based on libs/utils/src/Profiler.cpp from
 * Google/Filament.
 *
 * This layer is extremely simple to reduce overhead, though it does not yet use
 * fast/critical JNI.
 *
 * This counter must be closed to avoid leaking the associated native allocation.
 *
 * This class does not yet help callers with prerequisites to getting counter values on API 30+:
 *  - setenforce 0 (requires root)
 *  - security.perf_harden 0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CpuCounter : Closeable {
    private var profilerPtr = CpuCounterJni.newProfiler()
    private var hasReset = false

    fun resetEvents(events: List<Event>) {
        hasReset = true
        val flags = events.fold(0) { acc, event ->
            acc.or(event.flag)
        }
        CpuCounterJni.resetEvents(profilerPtr, flags)
    }

    override fun close() {
        CpuCounterJni.freeProfiler(profilerPtr)
        profilerPtr = 0
    }

    fun reset() {
        CpuCounterJni.reset(profilerPtr)
    }
    fun start() = CpuCounterJni.start(profilerPtr)
    fun stop() = CpuCounterJni.stop(profilerPtr)

    fun read(outValues: Values) {
        check(profilerPtr != 0L) { "Error: attempted to read counters after close" }
        check(hasReset) { "Error: attempted to read counters without reset" }
        CpuCounterJni.read(profilerPtr, outValues.longArray)
    }

    enum class Event(
        val id: Int
    ) {
        Instructions(0),
        CpuCycles(1),
        L1DReferences(2),
        L1DMisses(3),
        BranchInstructions(4),
        BranchMisses(5),
        L1IReferences(6),
        L1IMisses(7);

        val flag: Int
            inline get() = 1 shl id
    }

    /**
     * Holder class for querying all counter values at once out of native, to avoid multiple JNI
     * transitions.
     */
    @JvmInline
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    value class Values(val longArray: LongArray = LongArray(19)) {
        init {
            // See CountersLongCount static_assert in native
            require(longArray.size == 19)
        }

        inline val numberOfCounters: Long
            get() = longArray[0]
        inline val timeEnabled: Long
            get() = longArray[1]
        inline val timeRunning: Long
            get() = longArray[2]

        @Suppress("NOTHING_TO_INLINE")
        inline fun getValue(spec: Event): Long = longArray[3 + (2 * spec.id)]
    }

    companion object {
        fun checkPerfEventSupport(): String? = CpuCounterJni.checkPerfEventSupport()
    }
}

private object CpuCounterJni {
    init {
        System.loadLibrary("benchmarkNative")
    }

    // Profiler methods
    external fun checkPerfEventSupport(): String?
    external fun newProfiler(): Long
    external fun freeProfiler(profilerPtr: Long)
    external fun resetEvents(profilerPtr: Long, mask: Int): Int
    external fun reset(profilerPtr: Long)
    external fun start(profilerPtr: Long)
    external fun stop(profilerPtr: Long)
    external fun read(profilerPtr: Long, outData: LongArray)
}
