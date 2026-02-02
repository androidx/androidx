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

/**
 * Microbenchmark metric.
 *
 * Note that the API is designed around low overhead, even in the case of multiple submetrics (such
 * as cpu perf event counters) that must be started/stopped together for efficiency.
 *
 * This class may be initialized on a different thread from where measurement occurs, but all
 * `capture` methods must be invoked from the same thread.
 */
@ExperimentalBenchmarkConfigApi
abstract class MetricCapture(
    /**
     * List of names of metrics produced by this MetricCapture.
     *
     * The length of this list defines how many metrics will be produced by [captureStart] and
     * [captureStop].
     */
    val names: List<String>
) {
    /**
     * Starts collecting data for a run.
     *
     * Called at the start of each run.
     *
     * @param timeNs Current time, just before starting metrics. Can be used directly to drive a
     *   timing metric produced.
     */
    abstract fun captureStart(timeNs: Long)

    /**
     * Mark the end of a run, and store offset metrics in the output array, per sub metric.
     *
     * To output values, store them in the output array offset by both the parameter offset, and
     * their submetric index, for example:
     * ```
     * class MyMetricCapture("firstSubMetricName", "secondSubMetricName") {
     *     //...
     *     override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
     *         output[offset + 0] = firstSubMetricValue
     *         output[offset + 1] = secondSubMetricValue
     *     }
     * }
     * ```
     *
     * @param timeNs Time of metric capture start, in monotonic time ([java.lang.System.nanoTime])
     * @param output LongArray sized to hold all simultaneous sub metric outputs, use `offset` as
     *   the initial position in `output` to start writing submetrics.
     * @param offset Offset into the output array to start writing sub metrics.
     */
    abstract fun captureStop(timeNs: Long, output: LongArray, offset: Int)

    /** Pause data collection. */
    abstract fun capturePaused()

    /** Resume data collection */
    abstract fun captureResumed()

    override fun equals(other: Any?): Boolean {
        return (other is MetricCapture && other.names == this.names)
    }

    override fun hashCode(): Int {
        return names.hashCode() // This is the only true state retained, and hashCode must match ==
    }
}

/**
 * Time metric, which reports time in nanos, based on the time passed to [captureStop].
 *
 * Reports elapsed time with the label from `name`, which defaults to `timeNs`.
 *
 * @param name Metric name of the measured time, defaults to `timeNs`.
 */
@ExperimentalBenchmarkConfigApi
class TimeCapture @JvmOverloads constructor(name: String = "timeNs") :
    MetricCapture(names = listOf(name)) {
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
internal class AllocationCountCapture : MetricCapture(names = listOf("allocationCount")) {
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
) : MetricCapture(events.map { it.outputName }) {
    constructor(
        cpuEventCounter: CpuEventCounter,
        mask: Int
    ) : this(cpuEventCounter, CpuEventCounter.Event.values().filter { it.flag.and(mask) != 0 })

    private val values = CpuEventCounter.Values()
    private val flags = events.getFlags()

    override fun captureStart(timeNs: Long) {
        // must be called on measure thread, so we wait until after init (which can be separate)
        cpuEventCounter.resetEvents(flags)
        cpuEventCounter.start()
    }

    override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
        cpuEventCounter.stop()
        cpuEventCounter.read(values)
        events.forEachIndexed { index, event -> output[offset + index] = values.getValue(event) }
    }

    override fun capturePaused() {
        cpuEventCounter.stop()
    }

    override fun captureResumed() {
        cpuEventCounter.start()
    }
}
