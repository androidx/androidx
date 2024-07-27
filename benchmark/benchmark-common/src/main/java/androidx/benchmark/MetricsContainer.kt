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

import android.util.Log

internal class MetricsContainer(
    /**
     * Each MetricCapture represents a single metric to be captured. It is possible this may change.
     *
     * Metrics are usually indexed by the names provided for them by the MetricCaptures, or an index
     */
    private val metrics: Array<MetricCapture> = arrayOf(TimeCapture()),
    private val repeatCount: Int
) {

    internal val names: List<String> = metrics.flatMap { it.names }

    /**
     * Each entry in the top level list is a multi-metric set of measurements.
     *
     * ```
     * Example layout:
     *     repeatCount = 2
     *     metrics = [ MetricCapture(names = "X","Y"), MetricCapture(names = "Z") ]
     *
     * names = [ "X", "Y", "Z" ]
     * data = [
     *    // NOTE: Z start()'d first, but stop()'d last
     *    [X1, Y1, Z1]
     *    [X2, Y2, Z2]
     * ]
     * ```
     *
     * NOTE: Performance of warmup is very dependent on this structure, be very careful changing
     * changing this. E.g. using a single linear LongArray or an Array<LongArray> both caused warmup
     * measurements of a noop loop to fluctuate, and increase significantly (from 75ns to 450ns on
     * an API 30 Bramble).
     */
    internal val data: List<LongArray> = List(repeatCount) { LongArray(names.size) }

    /**
     * Array of start / stop time, per measurement, to be passed to [InMemoryTracing].
     *
     * These values are used both in metric calculation and trace data, so tracing is extremely low
     * overhead - just the cost of storing the timing data in an additional place in memory.
     */
    private val repeatTiming = LongArray(repeatCount * 2)

    fun peekSingleRepeatTime(): Long {
        check(repeatCount == 1) { "Observed repeat count $repeatCount, expected 1" }
        return repeatTiming[1] - repeatTiming[0]
    }

    private var runNum: Int = 0

    /**
     * Sets up the parameters for this benchmark, and clears leftover data.
     *
     * Call when initializing a benchmark.
     */
    fun captureInit() {
        runNum = 0
    }

    /**
     * Starts collecting data for a run.
     *
     * Must be called at the start of each run.
     */
    fun captureStart() {
        val timeNs = System.nanoTime()
        repeatTiming[runNum * 2] = timeNs

        // reverse order so first metric sees least interference
        for (i in metrics.lastIndex downTo 0) {
            metrics[i].captureStart(timeNs) // put the most sensitive metric first to avoid overhead
        }
    }

    /**
     * Marks the end of a run, and stores the metric value changes since the last start.
     *
     * Should be called when a run stops.
     */
    fun captureStop() {
        val timeNs = System.nanoTime()
        var offset = 0
        for (i in 0..metrics.lastIndex) { // stop in reverse order from start
            metrics[i].captureStop(timeNs, data[runNum], offset)
            offset += metrics[i].names.size
        }
        repeatTiming[runNum * 2 + 1] = timeNs
        runNum += 1
    }

    /**
     * Pauses data collection.
     *
     * Call when you want to not capture the following part of a run.
     */
    fun capturePaused() {
        for (i in 0..metrics.lastIndex) { // like stop, pause in reverse order from start
            metrics[i].capturePaused()
        }
    }

    /**
     * Resumes data collection.
     *
     * Call when you want to resume capturing a capturePaused-ed run.
     */
    fun captureResumed() {
        for (i in metrics.lastIndex downTo 0) { // same order as start
            metrics[i].captureResumed()
        }
    }

    /**
     * Finishes and cleans up a benchmark, and returns statistics about all that benchmark's data.
     *
     * Call exactly once at the end of a benchmark.
     */
    fun captureFinished(maxIterations: Int): List<MetricResult> {
        for (i in 0..repeatTiming.lastIndex step 2) {
            InMemoryTracing.beginSection("measurement ${i / 2}", nanoTime = repeatTiming[i])
            InMemoryTracing.endSection(nanoTime = repeatTiming[i + 1])
        }

        return names.mapIndexed { index, name ->
            val metricData =
                List(repeatCount) {
                    // convert to floats and divide by iter count here for efficiency
                    data[it][index] / maxIterations.toDouble()
                }
            metricData.chunked(10).forEachIndexed { chunkNum, chunk ->
                Log.d(
                    BenchmarkState.TAG,
                    name +
                        "[%2d:%2d]: %s"
                            .format(
                                chunkNum * 10,
                                (chunkNum + 1) * 10,
                                chunk.joinToString(" ") { it.toLong().toString() }
                            )
                )
            }
            MetricResult(name, metricData)
        }
    }
}
