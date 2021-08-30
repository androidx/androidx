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
    private val REPEAT_COUNT: Int
) {

    internal val data: Array<LongArray> = Array(metrics.size) {
        LongArray(REPEAT_COUNT)
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
        for (i in 0..metrics.lastIndex) {
            metrics[i].captureStart() // put the most sensitive benchmark last to avoid overhead
        }
    }

    /**
     * Marks the end of a run, and stores the metric value changes since the last start.
     *
     * Should be called when a run stops.
     */
    fun captureStop() {
        for (i in metrics.lastIndex downTo 0) { // stop in reverse order
            data[i][runNum] = metrics[i].captureStop()
        }
        runNum += 1
    }

    /**
     * Pauses data collection.
     *
     * Call when you want to not capture the following part of a run.
     */
    fun capturePaused() {
        for (i in metrics.lastIndex downTo 0) { // like stop, pause in reverse order
            metrics[metrics.lastIndex - i].capturePaused()
        }
    }

    /**
     * Resumes data collection.
     *
     * Call when you want to resume capturing a capturePaused-ed run.
     */
    fun captureResumed() {
        for (i in 0..metrics.lastIndex) {
            metrics[i].captureResumed()
        }
    }

    /**
     * Finishes and cleans up a benchmark, and returns statistics about all that benchmark's data.
     *
     * Call exactly once at the end of a benchmark.
     */
    fun captureFinished(maxIterations: Int): List<Stats> {
        for (eachArray in data) { // do this at the end for efficiency
            for (i in 0..eachArray.lastIndex) {
                eachArray[i] = eachArray[i] / maxIterations
            }
        }
        doLog()
        return metrics.mapIndexed { i, it -> Stats(data[i], it.name) }
    }

    private fun doLog() {
        metrics.forEachIndexed { i, metric ->
            data[i].toList().chunked(10)
                .forEachIndexed { chunkNum, chunk ->
                    Log.d(
                        BenchmarkState.TAG,
                        metric.name + "[%2d:%2d]: %s".format(
                            chunkNum * 10,
                            (chunkNum + 1) * 10,
                            chunk.joinToString()
                        )
                    )
                }
        }
    }
}
