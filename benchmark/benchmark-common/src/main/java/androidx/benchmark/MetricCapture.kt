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

package androidx.benchmark

import android.os.Debug

internal abstract class MetricCapture(
    val names: List<String>
) {
    /**
     * Starts collecting data for a run.
     *
     * Must be called at the start of each run.
     */
    abstract fun captureStart(timeNs: Long)

    /**
     * Marks the end of a run, and stores one metric value in the output array since the last call
     * to start.
     *
     * Should be called when a run stops.
     */
    abstract fun captureStop(timeNs: Long, output: LongArray, offset: Int)

    /**
     * Pauses data collection.
     *
     * Call when you want to not capture the following part of a run.
     */
    abstract fun capturePaused()

    /**
     * Resumes data collection.
     *
     * Call when you want to resume capturing a capturePaused-ed run.
     */
    abstract fun captureResumed()

    override fun equals(other: Any?): Boolean {
        return (other is MetricCapture && other.names == this.names)
    }

    override fun hashCode(): Int {
        return names.hashCode() // This is the only true state retained, and hashCode must match ==
    }
}

internal class TimeCapture : MetricCapture(
    names = listOf("timeNs")
) {
    private var currentStarted = 0L
    private var currentPausedStarted = 0L
    private var currentTotalPaused = 0L

    override fun captureStart(timeNs: Long) {
        currentTotalPaused = 0
        currentStarted = timeNs
    }

    override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
        output[offset] = timeNs - currentStarted - currentTotalPaused
    }

    override fun capturePaused() {
        currentPausedStarted = System.nanoTime()
    }

    override fun captureResumed() {
        currentTotalPaused += System.nanoTime() - currentPausedStarted
    }
}

@Suppress("DEPRECATION")
internal class AllocationCountCapture : MetricCapture(
    names = listOf("allocationCount")
) {
    private var currentPausedStarted = 0
    private var currentTotalPaused = 0

    override fun captureStart(timeNs: Long) {
        currentTotalPaused = 0
        Debug.startAllocCounting()
    }

    override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
        Debug.stopAllocCounting()
        output[offset] = (Debug.getGlobalAllocCount() - currentTotalPaused).toLong()
    }

    override fun capturePaused() {
        // Note - can't start/stop allocation counting to pause/resume, since that would clear
        // the current counter (and is likely more disruptive than just querying count)
        currentPausedStarted = Debug.getGlobalAllocCount()
    }

    override fun captureResumed() {
        currentTotalPaused += Debug.getGlobalAllocCount() - currentPausedStarted
    }
}

@Suppress
internal class CpuEventCounterCapture(
    private val cpuEventCounter: CpuEventCounter,
    private val events: List<CpuEventCounter.Event>
) : MetricCapture(events.map { it.name }) {
    constructor(
        cpuEventCounter: CpuEventCounter,
        mask: Int
    ) : this(cpuEventCounter, CpuEventCounter.Event.values().filter {
        it.flag.and(mask) != 0
    })

    private val values = CpuEventCounter.Values()
    private val flags = events.getFlags()
    private var hasResetEvents = false

    override fun captureStart(timeNs: Long) {
        if (!hasResetEvents) {
            // must be called on measure thread, so we wait until after init (which can be separate)
            cpuEventCounter.resetEvents(flags)
            hasResetEvents = true
        } else {
            // flags already set, fast path
            cpuEventCounter.reset()
        }
        cpuEventCounter.start()
    }

    override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
        cpuEventCounter.stop()
        cpuEventCounter.read(values)
        events.forEachIndexed { index, event ->
            output[offset + index] = values.getValue(event)
        }
    }

    override fun capturePaused() {
        cpuEventCounter.stop()
    }

    override fun captureResumed() {
        cpuEventCounter.start()
    }
}
