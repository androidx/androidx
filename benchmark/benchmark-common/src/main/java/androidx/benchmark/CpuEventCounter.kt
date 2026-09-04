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
import java.util.Locale

/**
 * Exposes CPU counters from perf_event_open based on libs/utils/src/Profiler.cpp from
 * Google/Filament.
 *
 * This layer is extremely simple to reduce overhead, though it does not yet use fast/critical JNI.
 *
 * This counter must be closed to avoid leaking the associated native allocation.
 *
 * This class does not yet help callers with prerequisites to getting counter values on API 23+:
 * - setenforce 0 (requires root)
 * - security.perf_harden 0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CpuEventCounter : Closeable {
    private var profilerPtr = CpuCounterJni.newProfiler()
    private var hasReset = false
    internal var currentEventFlags = 0
        private set

    public fun resetEvents(events: List<Event>) {
        resetEvents(events.getFlags())
    }

    public fun resetEvents(eventFlags: Int) {
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

    public override fun close() {
        CpuCounterJni.freeProfiler(profilerPtr)
        profilerPtr = 0
    }

    public fun reset() {
        CpuCounterJni.reset(profilerPtr)
    }

    public fun start(): Unit = CpuCounterJni.start(profilerPtr)

    public fun stop(): Unit = CpuCounterJni.stop(profilerPtr)

    /**
     * Read the values from the native profiler implementation and write them into [outValues]. Does
     * not throw.
     *
     * @param outValues The object to write the output into
     */
    public fun read(outValues: Values) {
        check(profilerPtr != 0L) { "Error: attempted to read counters after close" }
        check(hasReset) { "Error: attempted to read counters without reset" }
        CpuCounterJni.read(profilerPtr, outValues.longArray)
    }

    /**
     * Validates captured CPU counter values.
     *
     * Returns an [IllegalStateException] if both [Event.Instructions] and [Event.CpuCycles] are
     * requested but both measure 0. This indicates a hardware capture failure (e.g., from SELinux
     * or permission limits).
     *
     * @param values captured counter values
     * @param events performance events requested for capture
     * @return validation exception if verification fails, or null if successful
     */
    public fun validateValues(values: Values, events: List<Event>): IllegalStateException? {
        val eventFlags = events.getFlags()
        val validateFlags = eventFlags.and(Event.CpuCycles.flag.or(Event.Instructions.flag))
        if (validateFlags != 0) {
            val hasInstructionError =
                validateFlags.and(Event.Instructions.flag) != 0 &&
                    values.getValue(Event.Instructions) == 0L
            val hasCpuCyclesError =
                validateFlags.and(Event.CpuCycles.flag) != 0 &&
                    values.getValue(Event.CpuCycles) == 0L
            if (hasInstructionError && hasCpuCyclesError) {
                val valuesString =
                    events.joinToString(",") { "${it.outputName}=${values.getValue(it)}" }
                return IllegalStateException(
                    "Observed 0 for instructions/cpuCycles, capture appeared to fail, values=[$valuesString]"
                )
            }
        }
        return null
    }

    public enum class Event(public val id: Int) {
        Instructions(0),
        CpuCycles(1),
        L1DReferences(2),
        L1DMisses(3),
        BranchInstructions(4),
        BranchMisses(5),
        L1IReferences(6),
        L1IMisses(7);

        public val flag: Int
            inline get() = 1 shl id

        public val outputName: String = name.replaceFirstChar { it.lowercase(Locale.US) }
    }

    /**
     * Holder class for querying all counter values at once out of native, to avoid multiple JNI
     * transitions.
     */
    @JvmInline
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public value class Values(public val longArray: LongArray = LongArray(19)) {
        init {
            // See CountersLongCount static_assert in native
            require(longArray.size == 19)
        }

        public inline val numberOfCounters: Long
            get() = longArray[0]

        public inline val timeEnabled: Long
            get() = longArray[1]

        public inline val timeRunning: Long
            get() = longArray[2]

        @Suppress("NOTHING_TO_INLINE")
        public inline fun getValue(spec: Event): Long = longArray[3 + (2 * spec.id)]
    }

    public companion object {
        public fun checkPerfEventSupport(): String? = CpuCounterJni.checkPerfEventSupport()

        /**
         * Forces system properties and selinux into correct mode for capture
         *
         * Reset still required if failure occurs partway through
         */
        public fun forceEnable(): String? {
            Api23Enabler.forceEnable()?.let {
                return it
            }
            return checkPerfEventSupport()
        }

        public fun reset() {
            Api23Enabler.reset()
        }

        /**
         * Enable setenforce 0 and setprop perf_harden to 0, have observed this required on API 23+
         *
         * Lower APIs not tested, but selinux is documented to be enforced starting in Android 5
         * (API 23).
         */
        public object Api23Enabler {
            private val perfHardenProp = PropOverride("security.perf_harden", "0")
            private var shouldResetEnforce1 = false

            public fun forceEnable(): String? {
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

            public fun reset() {
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
    public external fun checkPerfEventSupport(): String?

    public external fun newProfiler(): Long

    public external fun freeProfiler(profilerPtr: Long)

    public external fun resetEvents(profilerPtr: Long, mask: Int): Int

    public external fun reset(profilerPtr: Long)

    public external fun start(profilerPtr: Long)

    public external fun stop(profilerPtr: Long)

    public external fun read(profilerPtr: Long, outData: LongArray)
}

internal fun List<CpuEventCounter.Event>.getFlags() = fold(0) { acc, event -> acc.or(event.flag) }
