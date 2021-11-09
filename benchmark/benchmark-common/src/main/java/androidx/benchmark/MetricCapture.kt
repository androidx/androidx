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

internal abstract class MetricCapture {
    abstract val name: String

    /**
     * Starts collecting data for a run.
     *
     * Must be called at the start of each run.
     */
    abstract fun captureStart()

    /**
     * Marks the end of a run, and stores the metric value changes since the last start.
     *
     * Should be called when a run stops.
     */
    abstract fun captureStop(): Long

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
        return (other is MetricCapture && other.name == this.name)
    }

    override fun hashCode(): Int {
        return name.hashCode() // This is the only true state retained, and hashCode must match ==
    }
}

internal class TimeCapture : MetricCapture() {
    override val name: String = "timeNs"
    private var currentStarted = 0L
    private var currentPausedStarted = 0L
    private var currentTotalPaused = 0L

    override fun captureStart() {
        currentTotalPaused = 0
        currentStarted = System.nanoTime()
    }

    override fun captureStop(): Long {
        return System.nanoTime() - currentStarted - currentTotalPaused
    }

    override fun capturePaused() {
        currentPausedStarted = System.nanoTime()
    }

    override fun captureResumed() {
        currentTotalPaused += System.nanoTime() - currentPausedStarted
    }
}

@Suppress("DEPRECATION")
internal class AllocationCountCapture : MetricCapture() {
    override val name = "allocationCount"
    private var currentPausedStarted = 0
    private var currentTotalPaused = 0

    override fun captureStart() {
        currentTotalPaused = 0
        Debug.startAllocCounting()
    }

    override fun captureStop(): Long {
        Debug.stopAllocCounting()
        return (Debug.getGlobalAllocCount() - currentTotalPaused).toLong()
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
