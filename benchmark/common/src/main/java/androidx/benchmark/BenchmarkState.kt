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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.benchmark.Errors.PREFIX
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.Trace
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Control object for benchmarking in the code in Java.
 *
 * Query a state object with [androidx.benchmark.junit4.BenchmarkRule.getState], and use it to
 * measure a block of Java with [BenchmarkState.keepRunning]:
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
 * @see androidx.benchmark.junit4.BenchmarkRule#getState()
 */
class BenchmarkState @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor() {

    private var stages = listOf(
        MetricsContainer(arrayOf(TimeCapture()), 1),
        MetricsContainer(arrayOf(TimeCapture()), REPEAT_COUNT_TIME),
        MetricsContainer(arrayOf(AllocationCountCapture()), REPEAT_COUNT_ALLOCATION)
    )

    private var metrics = stages[0]

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var traceUniqueName = "benchmark"

    private var warmupRepeats = 0 // number of warmup repeats that occurred

    /**
     * Decreasing iteration count used when [state] == [RUNNING_TIME_STAGE]
     * Used to determine when a main measurement stage finishes.
     */
    @JvmField // Used by [BenchmarkState.keepRunningInline()]
    @PublishedApi
    internal var iterationsRemaining = -1

    /**
     * Number of iterations in a repeat.
     *
     * This value is overridden by the end of the warmup stage. The default value defines
     * behavior for modes that bypass warmup (dryRun and startup).
     */
    private var iterationsPerRepeat = 1

    private var state = NOT_STARTED // Current benchmark state.

    private val warmupManager = WarmupManager()

    private var paused = false
    private var thermalThrottleSleepSeconds: Long =
        0 // The duration of sleep due to thermal throttling.
    private var totalRunTimeStartNs: Long = 0 // System.nanoTime() at start of benchmark.
    private var totalRunTimeNs: Long = 0 // Total run time of a benchmark.

    private var repeatCount = 0

    /**
     * Set this to true to run a simplified timing loop - no allocation tracking, and no global
     * state set/reset (such as thread priorities)
     *
     * This var is used in one of two cases, either set to true by [ThrottleDetector.measureWorkNs]
     * when device performance testing for thermal throttling in between benchmarks, or in
     * correctness tests of this library.
     *
     * When set to true, indicates that this BenchmarkState **should not**:
     * - touch thread priorities
     * - perform allocation counting (only timing results matter)
     * - call [ThrottleDetector], since it would infinitely recurse
     */
    internal var simplifiedTimingOnlyMode = false
    private var throttleRemainingRetries = THROTTLE_MAX_RETRIES

    private var stats = mutableListOf<Stats>()
    private var allData = mutableListOf<LongArray>()

    @SuppressLint("MethodNameUnits")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getMinTimeNanos(): Long {
        checkState() // this method is not triggerable externally, but that could change
        return stats.first { it.name == "timeNs" }.min
    }

    private fun checkState() {
        check(state != NOT_STARTED) {
            "The benchmark wasn't started! Every test in a class " +
                    "with a BenchmarkRule must contain a benchmark. In Kotlin, call " +
                    "benchmarkRule.measureRepeated {}, or in Java, call " +
                    "benchmarkRule.getState().keepRunning() to run your benchmark."
        }
        check(state == FINISHED) {
            "The benchmark hasn't finished! In Java, use " +
                    "while(BenchmarkState.keepRunning()) to ensure keepRunning() returns " +
                    "false before ending your test. In Kotlin, just use " +
                    "benchmarkRule.measureRepeated {} to avoid the problem."
        }
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
     * @throws [IllegalStateException] if the benchmark is already paused.
     *
     * @see resumeTiming
     */
    fun pauseTiming() {
        check(!paused) { "Unable to pause the benchmark. The benchmark has already paused." }
        if (state != RUNNING_WARMUP_STAGE) {
            // only pause/resume metrics during non-warmup stages.
            // warmup should ignore pause so that benchmarks which are paused the vast majority
            // of time don't appear to have run much faster, from the perspective of WarmupManager.
            metrics.capturePaused()
        }
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
     *
     * @throws [IllegalStateException] if the benchmark is already running.
     *
     * ```
     *
     * @see pauseTiming
     */
    fun resumeTiming() {
        check(paused) { "Unable to resume the benchmark. The benchmark is already running." }
        if (state != RUNNING_WARMUP_STAGE) {
            // only pause/resume metrics during non-warmup stages. See pauseTiming.
            metrics.captureResumed()
        }
        paused = false
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun startProfilingTimeStageIfRequested() {
        when (Arguments.profilingMode) {
            ProfilingMode.Sampled, ProfilingMode.Method -> {
                val path = File(
                    Arguments.testOutputDir,
                    "$traceUniqueName-${Arguments.profilingMode}.trace"
                ).absolutePath

                Log.d(TAG, "Profiling output file: $path")

                val bufferSize = 16 * 1024 * 1024
                if (Arguments.profilingMode == ProfilingMode.Sampled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ) {
                    Debug.startMethodTracingSampling(path, bufferSize, 100)
                } else {
                    Debug.startMethodTracing(path, bufferSize, 0)
                }
            }
            ProfilingMode.ConnectedAllocation, ProfilingMode.ConnectedSampled -> {
                Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
            }
            else -> {
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun stopProfilingTimeStageIfRequested() {
        when (Arguments.profilingMode) {
            ProfilingMode.Sampled, ProfilingMode.Method -> {
                Debug.stopMethodTracing()
            }
            ProfilingMode.ConnectedAllocation, ProfilingMode.ConnectedSampled -> {
                Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
            }
            else -> {
            }
        }
    }

    private fun beginRunningStage() {
        metrics = stages[state]
        repeatCount = 0
        metrics.captureInit()

        when (state) {
            RUNNING_WARMUP_STAGE -> {
                // Run GC to avoid memory pressure from previous run from affecting this one.
                // Note, we don't use System.gc() because it doesn't always have consistent behavior
                Runtime.getRuntime().gc()
                iterationsPerRepeat = 1
                Trace.beginSection("Warmup")
            }
            RUNNING_TIME_STAGE -> {
                startProfilingTimeStageIfRequested()
                Trace.beginSection("Benchmark Time")
            }
            RUNNING_ALLOCATION_STAGE -> {
                Trace.beginSection("Benchmark Allocations")
            }
        }
        iterationsRemaining = iterationsPerRepeat
        metrics.captureStart()
    }

    /**
     * @return Whether this benchmarking stage was actually ended
     */
    private fun endRunningStage(): Boolean {
        if (state != RUNNING_WARMUP_STAGE &&
            !simplifiedTimingOnlyMode &&
            throttleRemainingRetries > 0 &&
            sleepIfThermalThrottled(THROTTLE_BACKOFF_S)
        ) {
            // We've slept due to thermal throttle - retry benchmark!
            throttleRemainingRetries -= 1
            metrics.captureInit()
            repeatCount = 0
            return false
        }
        Trace.endSection() // paired with start in beginRunningStage()
        when (state) {
            RUNNING_WARMUP_STAGE -> {
                warmupRepeats = repeatCount
                iterationsPerRepeat = computeMaxIterations()
            }
            RUNNING_TIME_STAGE, RUNNING_ALLOCATION_STAGE -> {
                if (state == RUNNING_TIME_STAGE) {
                    stopProfilingTimeStageIfRequested()
                }

                stats.addAll(metrics.captureFinished(maxIterations = iterationsPerRepeat))
                allData.addAll(metrics.data)
            }
        }
        state++
        if (state == RUNNING_ALLOCATION_STAGE) {
            // skip allocation stage if we are only doing minimal looping (startupMode, dryRunMode,
            // profilingMode), or if we only care about timing (checkForThermalThrottling)
            if (simplifiedTimingOnlyMode ||
                Arguments.startupMode ||
                Arguments.dryRunMode ||
                Arguments.profilingMode != ProfilingMode.None
            ) {
                state++
            }
        }
        return true
    }

    /**
     * @return whether the entire, multi-stage benchmark still has anything left to do
     */
    private fun startNextRepeat(): Boolean {
        metrics.captureStop()
        repeatCount++
        // overwrite existing data, we don't keep data for warmup
        if (state == RUNNING_WARMUP_STAGE) { metrics.captureInit()
            if (warmupManager.onNextIteration(metrics.data.last()[0])) {
                endRunningStage()
                beginRunningStage()
            }
        } else if (state == RUNNING_TIME_STAGE && repeatCount >= REPEAT_COUNT_TIME ||
            state == RUNNING_ALLOCATION_STAGE && repeatCount >= REPEAT_COUNT_ALLOCATION
        ) {
            if (endRunningStage()) {
                if (state == FINISHED) {
                    afterBenchmark()
                    return false
                }
                beginRunningStage()
            }
        }
        iterationsRemaining = iterationsPerRepeat
        metrics.captureStart()
        return true
    }

    /**
     * Inline fast-path function for inner benchmark loop.
     *
     * Kotlin users should use `BenchmarkRule.measureRepeated`
     *
     * This codepath uses exclusively @JvmField/const members, so there are no method calls at all
     * in the inlined loop. On recent Android Platform versions, ART inlines these accessors anyway,
     * but we want to be sure it's as simple as possible.
     *
     * @suppress
     */
    @Suppress("NOTHING_TO_INLINE")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    inline fun keepRunningInline(): Boolean {
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

    /**
     * Reimplementation of Kotlin check, which also resets thread priority, since we don't want
     * to leave a thread with bumped thread priority
     */
    private inline fun check(value: Boolean, lazyMessage: () -> String) {
        if (!value) {
            ThreadPriority.resetBumpedThread()
            throw IllegalStateException(lazyMessage())
        }
    }

    /**
     * Internal loop control for benchmarks - will return true as long as there are more
     * measurements to perform.
     *
     * Actual benchmarks should always go through [keepRunning] or [keepRunningInline], since
     * they optimize the *Iteration* step to have extremely minimal logic performed.
     *
     * The looping behavior is functionally multiple nested loops:
     * - Stage - RUNNING_WARMUP vs RUNNING_TIME
     * - Repeat - how many measurements are made
     * - Iteration - how many loops are run within each measurement
     *
     * This has the effect of a 3 layer nesting loop structure, but all condensed to a single
     * method returning true/false to simplify the entry point.
     *
     * @return whether the benchmarking system has anything left to do
     */
    @PublishedApi
    internal fun keepRunningInternal(): Boolean {
        when (state) {
            NOT_STARTED -> {
                beforeBenchmark()
                beginRunningStage()
                return true
            }
            RUNNING_WARMUP_STAGE, RUNNING_TIME_STAGE, RUNNING_ALLOCATION_STAGE -> {
                iterationsRemaining--
                if (iterationsRemaining <= 0) {
                    throwIfPaused() // only check at end of loop to save cycles
                    return startNextRepeat()
                }
                return true
            }
            else -> throw IllegalStateException("The benchmark is in an invalid state.")
        }
    }

    private fun beforeBenchmark() {
        Errors.throwIfError()
        if (!firstBenchmark && Arguments.startupMode) {
            throw AssertionError(
                "Error - multiple benchmarks in startup mode. Only one " +
                        "benchmark may be run per 'am instrument' call, to ensure result " +
                        "isolation."
            )
        }
        firstBenchmark = false

        thermalThrottleSleepSeconds = 0

        if (!simplifiedTimingOnlyMode) {
            if (!CpuInfo.locked &&
                !IsolationActivity.sustainedPerformanceModeInUse &&
                !Errors.isEmulator
            ) {
                ThrottleDetector.computeThrottleBaseline()
            }

            ThreadPriority.bumpCurrentThreadPriority()
        }

        totalRunTimeStartNs = System.nanoTime() // Record this time to find total duration
        state = RUNNING_WARMUP_STAGE // begin benchmarking
        if (Arguments.dryRunMode || Arguments.startupMode) state = RUNNING_TIME_STAGE
    }

    private fun afterBenchmark() {
        totalRunTimeNs = System.nanoTime() - totalRunTimeStartNs

        if (!simplifiedTimingOnlyMode) {
            // Don't modify thread priority when checking for thermal throttling, since 'outer'
            // BenchmarkState owns thread priority
            ThreadPriority.resetBumpedThread()
        }
        warmupManager.logInfo()
    }

    private fun computeMaxIterations(): Int {
        var idealIterations =
            (REPEAT_DURATION_TARGET_NS / warmupManager.estimatedIterationTimeNs).toInt()
        idealIterations = idealIterations.coerceIn(MIN_TEST_ITERATIONS, MAX_TEST_ITERATIONS)
        OVERRIDE_ITERATIONS?.let { idealIterations = OVERRIDE_ITERATIONS }
        return idealIterations
    }

    private fun throwIfPaused() = check(!paused) {
        "Benchmark loop finished in paused state." +
                " Call BenchmarkState.resumeTiming() before BenchmarkState.keepRunning()."
    }

    internal data class Report(
        val className: String,
        val testName: String,
        val totalRunTimeNs: Long,
        val data: List<List<Long>>,
        val stats: List<Stats>,
        val repeatIterations: Int,
        val thermalThrottleSleepSeconds: Long,
        val warmupIterations: Int
    ) {
        fun getStats(which: String): Stats {
            return stats.first { it.name == which }
        }
    }

    private fun getReport(testName: String, className: String) = Report(
        className = className,
        testName = testName,
        totalRunTimeNs = totalRunTimeNs,
        data = allData.map { it.toList() },
        stats = stats,
        repeatIterations = iterationsPerRepeat,
        thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
        warmupIterations = warmupRepeats
    )

    internal fun getReport() = checkState().run { getReport("", "") }

    /**
     * Acquires a status report bundle
     *
     * @param key Run identifier, prepended to bundle properties.
     * @param includeStats True if stats should be included in the output bundle.
     */
    internal fun getFullStatusReport(key: String, includeStats: Boolean): Bundle {
        Log.i(TAG, key + stats.map { it.getSummary() } + "count=$iterationsPerRepeat")
        val status = Bundle()
        if (includeStats) {
            // these 'legacy' CI output stats are considered output
            stats.forEach { it.putInBundle(status, PREFIX) }
        }
        status.putIdeSummaryLine(
            testName = key,
            nanos = getMinTimeNanos(),
            allocations = stats.firstOrNull { it.name == "allocationCount" }?.median
        )
        return status
    }

    private fun reportResultsBundle(testName: String) {
        val bundle = getFullStatusReport(key = testName, includeStats = Arguments.outputEnable)

        // Before addResults() was added in the platform, we use sendStatus(). The constant '2'
        // comes from IInstrumentationResultParser.StatusCodes.IN_PROGRESS, and signals the
        // test infra that this is an "additional result" bundle, equivalent to addResults()
        // NOTE: we should a version check to call addResults(), but don't yet due to b/155103514
        InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
    }

    private fun sleepIfThermalThrottled(sleepSeconds: Long) = when {
        ThrottleDetector.isDeviceThermalThrottled() -> {
            Log.d(TAG, "THERMAL THROTTLE DETECTED, SLEEPING FOR $sleepSeconds SECONDS")
            val startTimeNs = System.nanoTime()
            Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds))
            val sleepTimeNs = System.nanoTime() - startTimeNs
            thermalThrottleSleepSeconds += TimeUnit.NANOSECONDS.toSeconds(sleepTimeNs)
            true
        }
        else -> false
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun report(
        fullClassName: String,
        simpleClassName: String,
        methodName: String
    ) {
        checkState() // this method is triggered externally
        val fullTestName = "$PREFIX$simpleClassName.$methodName"
        reportResultsBundle(fullTestName)

        ResultWriter.appendReport(
            getReport(
                testName = PREFIX + methodName,
                className = fullClassName
            )
        )
    }

    companion object {
        internal const val TAG = "Benchmark"

        private const val NOT_STARTED = -1 // The benchmark has not started yet.
        private const val RUNNING_WARMUP_STAGE = 0 // The benchmark warmup stage is running.
        private const val RUNNING_TIME_STAGE = 1 // The time benchmarking stage is running.
        private const val RUNNING_ALLOCATION_STAGE = 2 // The alloc benchmarking stage is running.
        private const val FINISHED = 3 // The benchmark has stopped; all stages are finished.

        private const val CONNECTED_PROFILING_SLEEP_MS = 20_000L

        // Values determined empirically.
        @VisibleForTesting
        internal val REPEAT_COUNT_TIME = when {
            Arguments.dryRunMode -> 1
            Arguments.profilingMode == ProfilingMode.ConnectedAllocation -> 1
            Arguments.startupMode -> 10
            else -> 50
        }

        internal const val REPEAT_COUNT_ALLOCATION = 10

        private val OVERRIDE_ITERATIONS = if (
            Arguments.dryRunMode ||
            Arguments.startupMode ||
            Arguments.profilingMode == ProfilingMode.ConnectedAllocation
        ) 1 else null

        internal val REPEAT_DURATION_TARGET_NS = when (Arguments.profilingMode) {
            ProfilingMode.None, ProfilingMode.Method -> TimeUnit.MICROSECONDS.toNanos(500)
            // longer measurements while profiling to ensure we have enough data
            else -> TimeUnit.MILLISECONDS.toNanos(20)
        }
        internal const val MAX_TEST_ITERATIONS = 1_000_000
        internal const val MIN_TEST_ITERATIONS = 1

        private const val THROTTLE_MAX_RETRIES = 3
        private const val THROTTLE_BACKOFF_S = 90L

        private var firstBenchmark = true

        @Suppress("DEPRECATION")
        @Experimental
        @Retention(AnnotationRetention.BINARY)
        @Target(AnnotationTarget.FUNCTION)
        annotation class ExperimentalExternalReport

        /**
         * Hooks for benchmarks not using [androidx.benchmark.junit4.BenchmarkRule] to register
         * results.
         *
         * Results are printed to Studio console, and added to the output JSON file.
         *
         * @param className Name of class the benchmark runs in
         * @param testName Name of the benchmark
         * @param totalRunTimeNs The total run time of the benchmark
         * @param dataNs List of all measured timing results, in nanoseconds
         * @param warmupIterations Number of iterations of warmup before measurements started.
         * Should be no less than 0.
         * @param thermalThrottleSleepSeconds Number of seconds benchmark was paused during thermal
         * throttling.
         * @param repeatIterations Number of iterations in between each measurement. Should be no
         * less than 1.
         */
        @JvmStatic
        @ExperimentalExternalReport
        fun reportData(
            className: String,
            testName: String,
            @IntRange(from = 0) totalRunTimeNs: Long,
            dataNs: List<Long>,
            @IntRange(from = 0) warmupIterations: Int,
            @IntRange(from = 0) thermalThrottleSleepSeconds: Long,
            @IntRange(from = 1) repeatIterations: Int
        ) {
            val metricsContainer = MetricsContainer(REPEAT_COUNT = dataNs.size)
            metricsContainer.data[metricsContainer.data.lastIndex] = dataNs.toLongArray()
            val report = Report(
                className = className,
                testName = testName,
                totalRunTimeNs = totalRunTimeNs,
                data = metricsContainer.data.map { it.toList() },
                stats = metricsContainer.captureFinished(maxIterations = 1),
                repeatIterations = repeatIterations,
                thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
                warmupIterations = warmupIterations
            )
            // Report value to Studio console
            val bundle = Bundle()
            val fullTestName = PREFIX +
                    if (className.isNotEmpty()) "$className.$testName" else testName
            bundle.putIdeSummaryLine(
                testName = fullTestName,
                nanos = report.getStats("timeNs").min,
                allocations = null
            )
            InstrumentationRegistry.getInstrumentation().sendStatus(Activity.RESULT_OK, bundle)

            // Report values to file output
            ResultWriter.appendReport(report)
        }
    }
}
