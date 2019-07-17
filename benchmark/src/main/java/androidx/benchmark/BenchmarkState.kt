/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.text.NumberFormat
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Control object for benchmarking in the code in Java.
 *
 * Query a state object with [BenchmarkRule.state], and use it to measure a block of Java with
 * [BenchmarkState.keepRunning]:
 * ```
 * @Rule
 * public BenchmarkRule benchmarkRule = new BenchmarkRule();
 *
 * @Test
 * public void sampleMethod() {
 *     BenchmarkState state = benchmarkRule.getState();
 *
 *     int[] src = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
 *     while (state.keepRunning()) {
 *         int[] dest = new int[src.length];
 *         System.arraycopy(src, 0, dest, 0, src.length);
 *     }
 * }
 * ```
 *
 * @see BenchmarkRule#getState()
 */
class BenchmarkState internal constructor() {
    private var warmupIteration = 0 // increasing iteration count during warmup

    /**
     * Decreasing iteration count used when [state] == [RUNNING], used to determine when main
     * measurement loop finishes.
     */
    @JvmField // Used by [BenchmarkState.keepRunningInline()]
    @PublishedApi
    internal var iterationsRemaining = -1

    private var maxIterations = 0

    private var state = NOT_STARTED // Current benchmark state.

    private val warmupManager = WarmupManager()

    private var startTimeNs: Long = 0 // System.nanoTime() at start of last warmup/test iter.

    private var paused = false
    private var pausedTimeNs: Long = 0 // The System.nanoTime() when the pauseTiming() is called.
    private var pausedDurationNs: Long = 0 // The duration of paused state in nano sec.
    private var thermalThrottleSleepSeconds: Long =
        0 // The duration of sleep due to thermal throttling.
    private var totalRunTimeStartNs: Long = 0 // System.nanoTime() at start of benchmark.
    private var totalRunTimeNs: Long = 0 // Total run time of a benchmark.

    private var repeatCount = 0

    // Statistics. These values will be filled when the benchmark has finished.
    // The computation needs double precision, but long int is fine for final reporting.
    private var internalStats: Stats? = null

    // Individual duration in nano seconds.
    private val results = ArrayList<Long>()

    internal var performThrottleChecks = true
    private var throttleRemainingRetries = THROTTLE_MAX_RETRIES

    /**
     * Get the end of run benchmark statistics.
     *
     *
     * This method may only be called keepRunning() returns `false`.
     *
     * @return Stats from run.
     */
    internal val stats: Stats
        get() {
            if (state == NOT_STARTED) {
                throw IllegalStateException(
                    "The benchmark wasn't started! Every test in a class " +
                            "with a BenchmarkRule must contain a benchmark. In Kotlin, call " +
                            "benchmarkRule.measureRepeated {}, or in Java, call " +
                            "benchmarkRule.getState().keepRunning() to run your benchmark."
                )
            }
            if (state != FINISHED) {
                throw IllegalStateException(
                    "The benchmark hasn't finished! In Java, use " +
                            "while(BenchmarkState.keepRunning()) to ensure keepRunning() returns " +
                            "false before ending your test. In Kotlin, just use " +
                            "benchmarkRule.measureRepeated {} to avoid the problem."
                )
            }
            return internalStats!!
        }

    /**
     * Stops the benchmark timer.
     *
     * This method can be called only when the timer is running.
     *
     * ```
     * @Test
     * public void bitmapProcessing() {
     *     final BenchmarkState state = mBenchmarkRule.getState();
     *     while (state.keepRunning()) {
     *         state.pauseTiming();
     *         // disable timing while constructing test input
     *         Bitmap input = constructTestBitmap();
     *         state.resumeTiming();
     *
     *         processBitmap(input);
     *     }
     * }
     * ```
     *
     * @see resumeTiming
     */
    fun pauseTiming() {
        if (paused) {
            throw IllegalStateException(
                "Unable to pause the benchmark. The benchmark has already paused."
            )
        }
        pausedTimeNs = System.nanoTime()
        paused = true
    }

    /**
     * Resumes the benchmark timer.
     *
     * This method can be called only when the timer is stopped.
     *
     * ```
     * @Test
     * public void bitmapProcessing() {
     *     final BenchmarkState state = mBenchmarkRule.getState();
     *     while (state.keepRunning()) {
     *         state.pauseTiming();
     *         // disable timing while constructing test input
     *         Bitmap input = constructTestBitmap();
     *         state.resumeTiming();
     *
     *         processBitmap(input);
     *     }
     * }
     * ```
     *
     * @see pauseTiming
     */
    fun resumeTiming() {
        if (!paused) {
            throw IllegalStateException(
                "Unable to resume the benchmark. The benchmark is already running."
            )
        }
        pausedDurationNs += System.nanoTime() - pausedTimeNs
        pausedTimeNs = 0
        paused = false
    }

    private fun beginWarmup() {
        startTimeNs = System.nanoTime()
        warmupIteration = 0
        state = WARMUP
    }

    private fun beginBenchmark() {
        if (ENABLE_PROFILING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // TODO: support data dir for old platforms
            val f = File(
                InstrumentationRegistry.getInstrumentation().context.dataDir,
                "benchprof"
            )
            Log.d(TAG, "Tracing to: " + f.absolutePath)
            Debug.startMethodTracingSampling(f.absolutePath, 16 * 1024 * 1024, 100)
        }
        val idealIterations =
            (TARGET_TEST_DURATION_NS / warmupManager.estimatedIterationTime).toInt()
        maxIterations = Math.min(
            MAX_TEST_ITERATIONS,
            Math.max(idealIterations, MIN_TEST_ITERATIONS)
        )
        pausedDurationNs = 0
        iterationsRemaining = maxIterations
        repeatCount = 0
        thermalThrottleSleepSeconds = 0
        state = RUNNING
        startTimeNs = System.nanoTime()
    }

    private fun startNextTestRun(): Boolean {
        val currentTime = System.nanoTime()

        results.add((currentTime - startTimeNs - pausedDurationNs) / maxIterations)
        repeatCount++

        if (repeatCount >= REPEAT_COUNT) {
            if (performThrottleChecks &&
                throttleRemainingRetries > 0 &&
                sleepIfThermalThrottled(THROTTLE_BACKOFF_S)
            ) {
                // We've slept due to thermal throttle - retry benchmark!
                throttleRemainingRetries -= 1
                results.clear()
                repeatCount = 0
            } else {
                // finished!
                if (ENABLE_PROFILING) {
                    Debug.stopMethodTracing()
                }
                internalStats = Stats(results)
                state = FINISHED
                totalRunTimeNs = System.nanoTime() - totalRunTimeStartNs
                return false
            }
        }
        pausedDurationNs = 0
        iterationsRemaining = maxIterations
        startTimeNs = System.nanoTime()
        return true
    }

    /**
     * Inline fast-path function for inner benchmark loop.
     *
     * Kotlin users should use [BenchmarkRule.measureRepeated]
     *
     * This codepath uses exclusively @JvmField/const members, so there are no method calls at all
     * in the inlined loop. On recent Android Platform versions, ART inlines these accessors anyway,
     * but we want to be sure it's as simple as possible.
     */
    @Suppress("NOTHING_TO_INLINE")
    @PublishedApi
    internal inline fun keepRunningInline(): Boolean {
        if (iterationsRemaining > 1) {
            iterationsRemaining--
            return true
        }
        return keepRunningInternal()
    }

    /**
     * Returns true if the benchmark needs more samples - use this as the condition of a while loop.
     *
     * ```
     * while (state.keepRunning()) {
     *     int[] dest = new int[src.length];
     *     System.arraycopy(src, 0, dest, 0, src.length);
     * }
     * ```
     */
    fun keepRunning(): Boolean {
        if (iterationsRemaining > 1) {
            iterationsRemaining--
            return true
        }
        return keepRunningInternal()
    }

    @PublishedApi
    internal fun keepRunningInternal(): Boolean {
        when (state) {
            NOT_STARTED -> {
                if (totalRunTimeStartNs == 0L) {
                    // This is the beginning of the benchmark, we remember it.
                    totalRunTimeStartNs = System.nanoTime()
                }
                if (performThrottleChecks &&
                    !CpuInfo.locked &&
                    !AndroidBenchmarkRunner.sustainedPerformanceModeInUse &&
                    !WarningState.isEmulator
                ) {
                    ThrottleDetector.computeThrottleBaseline()
                }
                beginWarmup()
                return true
            }
            WARMUP -> {
                warmupIteration++
                // Only check nanoTime on every iteration in WARMUP since we
                // don't yet have a target iteration count.
                val time = System.nanoTime()
                val lastDuration = time - startTimeNs
                startTimeNs = time
                throwIfPaused() // check each loop during warmup
                if (warmupManager.onNextIteration(lastDuration)) {
                    beginBenchmark()
                }
                return true
            }
            RUNNING -> {
                iterationsRemaining--
                if (iterationsRemaining <= 0) {
                    throwIfPaused() // only check at end of loop to save cycles
                    return startNextTestRun()
                }
                return true
            }
            FINISHED -> throw IllegalStateException("The benchmark has finished.")
            else -> throw IllegalStateException("The benchmark is in unknown state.")
        }
    }

    private fun throwIfPaused() {
        if (paused) {
            throw IllegalStateException(
                "Benchmark step finished with paused state." +
                        " Resume the benchmark before finishing each step."
            )
        }
    }

    internal data class Report(
        val className: String,
        val testName: String,
        val totalRunTimeNs: Long,
        val data: List<Long>,
        val repeatIterations: Int,
        val thermalThrottleSleepSeconds: Long,
        val warmupIterations: Int
    ) {
        val stats = Stats(data)
    }

    internal fun getReport(testName: String, className: String) = Report(
        className = className,
        testName = testName,
        totalRunTimeNs = totalRunTimeNs,
        data = results,
        repeatIterations = maxIterations,
        thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
        warmupIterations = warmupIteration
    )

    private fun summaryLine() = "Summary: " +
            "median=${stats.median}ns, " +
            "mean=${stats.mean.toLong()}ns, " +
            "min=${stats.min}ns, " +
            "stddev=${stats.standardDeviation.toLong()}ns, " +
            "count=$maxIterations"

    /**
     * Acquires a status report bundle
     *
     * @param key Run identifier, prepended to bundle properties.
     */
    internal fun getFullStatusReport(key: String): Bundle {
        Log.i(TAG, key + summaryLine())
        val status = Bundle()

        val prefix = WarningState.WARNING_PREFIX
        status.putLong("${prefix}median", stats.median)
        status.putLong("${prefix}mean", stats.mean.toLong())
        status.putLong("${prefix}min", stats.min)
        status.putLong("${prefix}standardDeviation", stats.standardDeviation.toLong())
        status.putLong("${prefix}count", maxIterations.toLong())
        status.putIdeSummaryLine(key, stats.min)
        return status
    }

    internal fun sendStatus(testName: String) {
        val bundle = getFullStatusReport(testName)
        InstrumentationRegistry.getInstrumentation().sendStatus(Activity.RESULT_OK, bundle)
    }

    private fun sleepIfThermalThrottled(sleepSeconds: Long) = when {
        ThrottleDetector.isDeviceThermalThrottled() -> {
            Log.d(TAG, "THERMAL THROTTLE DETECTED, SLEEPING FOR $sleepSeconds SECONDS")
            val startTime = System.nanoTime()
            Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds))
            val sleepTime = System.nanoTime() - startTime
            thermalThrottleSleepSeconds += TimeUnit.NANOSECONDS.toSeconds(sleepTime)
            true
        }
        else -> false
    }

    internal companion object {
        private const val TAG = "Benchmark"
        private const val STUDIO_OUTPUT_KEY_PREFIX = "android.studio.display."
        private const val STUDIO_OUTPUT_KEY_ID = "benchmark"

        private const val ENABLE_PROFILING = false

        private const val NOT_STARTED = 0 // The benchmark has not started yet.
        private const val WARMUP = 1 // The benchmark is warming up.
        private const val RUNNING = 2 // The benchmark is running.
        private const val FINISHED = 3 // The benchmark has stopped.

        // Values determined empirically.
        @VisibleForTesting
        internal const val REPEAT_COUNT = 50
        private val TARGET_TEST_DURATION_NS = TimeUnit.MICROSECONDS.toNanos(500)
        private const val MAX_TEST_ITERATIONS = 1000000
        private const val MIN_TEST_ITERATIONS = 1

        private const val THROTTLE_MAX_RETRIES = 3
        private const val THROTTLE_BACKOFF_S = 90L

        /**
         * Hooks for benchmarks not using [BenchmarkRule] to register results.
         *
         * Results are printed to Studio console, and added to the output JSON file.
         *
         * @param className Name of class the benchmark runs in
         * @param testName Name of the benchmark
         * @param totalRunTimeNs The total run time of the benchmark
         * @param dataNs List of all measured results, in nanoseconds
         * @param warmupIterations Number of iterations of warmup before measurements started.
         *                         Should be no less than 0.
         * @param thermalThrottleSleepSeconds Number of seconds benchmark was paused during thermal
         *                                    throttling.
         * @param repeatIterations Number of iterations in between each measurement. Should be no
         *                         less than 1.
         */
        @Suppress("unused")
        @JvmStatic
        fun reportData(
            className: String,
            testName: String,
            totalRunTimeNs: Long,
            dataNs: List<Long>,
            @IntRange(from = 0) warmupIterations: Int,
            @IntRange(from = 0) thermalThrottleSleepSeconds: Long,
            @IntRange(from = 1) repeatIterations: Int
        ) {
            val report = Report(
                className = className,
                testName = testName,
                totalRunTimeNs = totalRunTimeNs,
                data = dataNs,
                repeatIterations = repeatIterations,
                thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
                warmupIterations = warmupIterations
            )

            // Report value to Studio console
            val bundle = Bundle()
            val fullTestName = WarningState.WARNING_PREFIX +
                    if (className.isNotEmpty()) "$className.$testName" else testName
            bundle.putIdeSummaryLine(fullTestName, report.stats.min)
            InstrumentationRegistry.getInstrumentation().sendStatus(Activity.RESULT_OK, bundle)

            // Report values to file output
            ResultWriter.appendReport(report)
        }

        internal fun ideSummaryLineWrapped(key: String, nanos: Long): String {
            val warningLines =
                WarningState.acquireWarningStringForLogging()?.split("\n") ?: listOf()
            return (warningLines + ideSummaryLine(key, nanos))
                // remove first line if empty
                .filterIndexed { index, it -> index != 0 || !it.isEmpty() }
                // join, prepending key to everything but first string,
                // to make each line look the same
                .joinToString("\n$STUDIO_OUTPUT_KEY_ID: ")
        }

        // NOTE: this summary line will use default locale to determine separators. As
        // this line is only meant for human eyes, we don't worry about consistency here.
        fun ideSummaryLine(key: String, nanos: Long) = String.format(
            // 13 is used for alignment here, because it's enough that 9.99sec will still
            // align with any other output, without moving data too far to the right
            "%13s ns %s",
            NumberFormat.getNumberInstance().format(nanos),
            key
        )

        fun Bundle.putIdeSummaryLine(testName: String, nanos: Long) {
            putString(
                STUDIO_OUTPUT_KEY_PREFIX + STUDIO_OUTPUT_KEY_ID,
                ideSummaryLineWrapped(testName, nanos)
            )
        }
    }
}
