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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.Closeable

/**
 * Exposes CPU counters from perf_event_open based on libs/utils/src/Profiler.cpp from
 * Google/Filament.
 *
 * This layer is extremely simple to reduce overhead, though it does not yet use fast/critical JNI.
 *
 * This counter must be closed to avoid leaking the associated native allocation.
 *
 * This class does not yet help callers with prerequisites to getting counter values on API 30+:
 * - setenforce 0 (requires root)
 * - security.perf_harden 0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CpuEventCounter : Closeable {
    private var profilerPtr = CpuCounterJni.newProfiler()
    private var hasReset = false
    internal var currentEventFlags = 0
        private set

    fun resetEvents(events: List<Event>) {
        resetEvents(events.getFlags())
    }

    fun resetEvents(eventFlags: Int) {
        if (currentEventFlags != eventFlags) {
            // set up the flags
            CpuCounterJni.resetEvents(profilerPtr, eventFlags)
            currentEventFlags = eventFlags
        } else {
            // fast path when re-using same flags
            reset()
        }
        hasReset = true
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

    enum class Event(val id: Int) {
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

        /**
         * Forces system properties and selinux into correct mode for capture
         *
         * Reset still required if failure occurs partway through
         */
        fun forceEnable(): String? {
            if (Build.VERSION.SDK_INT >= 29) {
                Api29Enabler.forceEnable()?.let {
                    return it
                }
            }
            return checkPerfEventSupport()
        }

        fun reset() {
            if (Build.VERSION.SDK_INT >= 29) {
                Api29Enabler.reset()
            }
        }

        /**
         * Enable setenforce 0 and setprop perf_harden to 0, only observed this required on API 29+
         */
        @RequiresApi(29)
        object Api29Enabler {
            private val perfHardenProp = PropOverride("security.perf_harden", "0")
            private var shouldResetEnforce1 = false

            fun forceEnable(): String? {
                if (Shell.isSELinuxEnforced()) {
                    if (DeviceInfo.isRooted) {
                        Shell.executeScriptSilent("setenforce 0")
                        shouldResetEnforce1 = true
                    } else {
                        return "blocked by selinux, can't `setenforce 0` without rooted device"
                    }
                }
                perfHardenProp.forceValue()
                return null
            }

            fun reset() {
                perfHardenProp.resetIfOverridden()
                if (shouldResetEnforce1) {
                    Shell.executeScriptSilent("setenforce 1")
                    shouldResetEnforce1 = false
                }
            }
        }
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

internal fun List<CpuEventCounter.Event>.getFlags() = fold(0) { acc, event -> acc.or(event.flag) }
