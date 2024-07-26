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
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.benchmark.Errors.PREFIX
import androidx.benchmark.InstrumentationResults.instrumentationReport
import androidx.benchmark.InstrumentationResults.reportBundle
import androidx.benchmark.json.BenchmarkData
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
class BenchmarkState internal constructor(phaseConfig: MicrobenchmarkPhase.Config) {

    /**
     * Create a BenchmarkState for custom measurement behavior.
     *
     * @param warmupCount Number of non-measured warmup iterations to perform, leave null to
     *   determine automatically
     * @param repeatCount Number of measurements to perform, leave null for default behavior
     */
    @ExperimentalBenchmarkStateApi
    constructor(
        @SuppressWarnings("AutoBoxing") // allocations for tests not relevant, not in critical path
        warmupCount: Int? = null,
        @SuppressWarnings("AutoBoxing") // allocations for tests not relevant, not in critical path
        repeatCount: Int? = null
    ) : this(
        warmupCount = warmupCount,
        measurementCount = repeatCount,
        simplifiedTimingOnlyMode = false
    )

    /** Constructor used for standard uses of BenchmarkState, e.g. in BenchmarkRule */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        config: MicrobenchmarkConfig? = null
    ) : this(warmupCount = null, simplifiedTimingOnlyMode = false, config = config)

    internal constructor(
        warmupCount: Int? = null,
        measurementCount: Int? = null,
        simplifiedTimingOnlyMode: Boolean = false,
        config: MicrobenchmarkConfig? = null
    ) : this(
        MicrobenchmarkPhase.Config(
            dryRunMode = Arguments.dryRunMode,
            startupMode = Arguments.startupMode,
            profiler = config?.profiler?.profiler ?: Arguments.profiler,
            profilerPerfCompareMode = Arguments.profilerPerfCompareEnable,
            warmupCount = warmupCount,
            measurementCount = Arguments.iterations ?: measurementCount,
            simplifiedTimingOnlyMode = simplifiedTimingOnlyMode,
            metrics =
                config?.metrics?.toTypedArray()
                    ?: if (Arguments.cpuEventCounterMask != 0) {
                        arrayOf(
                            TimeCapture(),
                            CpuEventCounterCapture(
                                MicrobenchmarkPhase.cpuEventCounter,
                                Arguments.cpuEventCounterMask
                            )
                        )
                    } else {
                        arrayOf(TimeCapture())
                    }
        )
    )

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
    private val simplifiedTimingOnlyMode = phaseConfig.simplifiedTimingOnlyMode

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var traceUniqueName: String = "benchmark"

    internal var warmupRepeats = 0 // number of warmup repeats that occurred

    /**
     * Decreasing iteration count used when running a multi-iteration measurement phase Used to
     * determine when a main measurement stage finishes.
     */
    @JvmField // Used by [BenchmarkState.keepRunningInline()]
    @PublishedApi
    internal var iterationsRemaining: Int = -1

    @Suppress("NOTHING_TO_INLINE")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    inline fun getIterationsRemaining() = iterationsRemaining

    /**
     * Number of iterations in a repeat.
     *
     * This value is defined in the json, but is written as maximum iterationsPerRepeat across
     * phases, since nowadays there can be an arbitrary number of phases.
     *
     * This is fully compatible for now since e.g. timing and allocation measurement use the same
     * value, but we should consider tracking and reporting this differently in the json if this
     * changes.
     */
    @VisibleForTesting internal var iterationsPerRepeat = 1

    private val warmupManager = phaseConfig.warmupManager

    private var paused = false

    /** The total duration of sleep due to thermal throttling. */
    private var thermalThrottleSleepSeconds: Long = 0
    private var totalRunTimeStartNs: Long = 0 // System.nanoTime() at start of benchmark.
    private var totalRunTimeNs: Long = 0 // Total run time of a benchmark.

    private var warmupEstimatedIterationTimeNs: Long = -1L

    private val metricResults = mutableListOf<MetricResult>()
    private var profilerResult: Profiler.ResultFile? = null
    private val phases = phaseConfig.generatePhases()

    // tracking current phase state
    private var phaseIndex = -1
    private var currentPhase: MicrobenchmarkPhase = phases[0]
    private var currentMetrics: MetricsContainer = phases[0].metricsContainer
    private var currentMeasurement = 0
    private var currentLoopsPerMeasurement = 0

    @SuppressLint("MethodNameUnits")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getMinTimeNanos(): Double {
        checkFinished()
        return metricResults.first { it.name == "timeNs" }.min
    }

    private fun checkFinished() {
        check(phaseIndex >= 0) { "Attempting to interact with a benchmark that wasn't started!" }
        check(phaseIndex >= phases.size) {
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
     * @see resumeTiming
     */
    fun pauseTiming() {
        check(!paused) { "Unable to pause the benchmark. The benchmark has already paused." }
        currentMetrics.capturePaused()
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
     * @throws [IllegalStateException] if the benchmark is already running.
     * @see pauseTiming
     */
    fun resumeTiming() {
        check(paused) { "Unable to resume the benchmark. The benchmark is already running." }
        currentMetrics.captureResumed()
        paused = false
    }

    private fun startNextPhase(): Boolean {
        check(phaseIndex < phases.size)

        if (phaseIndex >= 0) {
            currentPhase.profiler?.run { inMemoryTrace("profiler.stop()") { stop() } }
            InMemoryTracing.endSection() // end phase
            thermalThrottleSleepSeconds += currentPhase.thermalThrottleSleepSeconds
            if (currentPhase.loopMode.warmupManager == null) {
                // Save captured metrics except during warmup, where we intentionally discard
                metricResults.addAll(
                    currentMetrics.captureFinished(maxIterations = currentLoopsPerMeasurement)
                )
            }
        }
        phaseIndex++
        if (phaseIndex == phases.size) {
            afterBenchmark()
            return false
        }
        currentPhase = phases[phaseIndex]
        currentMetrics = currentPhase.metricsContainer
        currentMeasurement = 0

        currentMetrics.captureInit()
        if (currentPhase.gcBeforePhase) {
            // Run GC to avoid memory pressure from previous run from affecting this one.
            // Note, we don't use System.gc() because it doesn't always have consistent behavior
            Runtime.getRuntime().gc()
        }

        currentLoopsPerMeasurement =
            currentPhase.loopMode.getIterations(warmupEstimatedIterationTimeNs)

        iterationsPerRepeat = iterationsPerRepeat.coerceAtLeast(currentLoopsPerMeasurement)

        InMemoryTracing.beginSection(currentPhase.label)
        val phaseProfilerResult =
            currentPhase.profiler?.run {
                val estimatedMethodTraceDurNs =
                    warmupEstimatedIterationTimeNs * METHOD_TRACING_ESTIMATED_SLOWDOWN_FACTOR
                if (
                    this == MethodTracing &&
                        Looper.myLooper() == Looper.getMainLooper() &&
                        estimatedMethodTraceDurNs > METHOD_TRACING_MAX_DURATION_NS &&
                        Arguments.profilerSkipWhenDurationRisksAnr
                ) {
                    val expectedDurSec = estimatedMethodTraceDurNs / 1_000_000_000.0
                    InstrumentationResults.scheduleIdeWarningOnNextReport(
                        """
                        Skipping method trace of estimated duration $expectedDurSec sec to avoid ANR

                        To disable this behavior, set instrumentation arg:
                            androidx.benchmark.profiling.skipWhenDurationRisksAnr = false
                    """
                            .trimIndent()
                    )
                    null
                } else {
                    inMemoryTrace("start profiling") { start(traceUniqueName) }
                }
            }
        if (phaseProfilerResult != null) {
            require(profilerResult == null) {
                "ProfileResult already set, only support one profiling phase"
            }
            profilerResult = phaseProfilerResult
        }

        currentMetrics.captureStart()
        return true
    }

    /** @return true if the benchmark should still keep running */
    private fun onMeasurementComplete(): Boolean {
        currentMetrics.captureStop()
        throwIfPaused()
        currentMeasurement++

        val tryStartNextPhase =
            currentPhase.loopMode.let {
                if (it.warmupManager != null) {
                    // warmup phase
                    currentMetrics.captureInit()
                    // Note that warmup is based on repeat time, *not* the timeNs metric, since we
                    // want
                    // to account for paused time during warmup (paused work should stabilize too)
                    val lastMeasuredWarmupValue = currentMetrics.peekSingleRepeatTime()
                    if (it.warmupManager.onNextIteration(lastMeasuredWarmupValue)) {
                        warmupEstimatedIterationTimeNs = lastMeasuredWarmupValue
                        warmupRepeats = currentMeasurement
                        true
                    } else {
                        false
                    }
                } else {
                    currentMeasurement == currentPhase.measurementCount
                }
            }
        return if (tryStartNextPhase) {
            if (currentPhase.tryEnd()) {
                startNextPhase()
            } else {
                // failed capture (due to thermal throttling), restart profiler and metrics
                currentPhase.profiler?.apply {
                    stop()
                    profilerResult = inMemoryTrace("start profiling") { start(traceUniqueName) }
                }
                currentMetrics.captureInit()
                currentMeasurement = 0
                true
            }
        } else {
            currentMetrics.captureStart()
            true
        }
    }

    /**
     * Inline fast-path function for inner benchmark loop.
     *
     * Kotlin users should use `BenchmarkRule.measureRepeated`
     *
     * This code path uses exclusively @JvmField/const members, so there are no method calls at all
     * in the inlined loop. On recent Android Platform versions, ART inlines these accessors anyway,
     * but we want to be sure it's as simple as possible.
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
     * Reimplementation of Kotlin check, which also resets thread priority, since we don't want to
     * leave a thread with bumped thread priority
     */
    private inline fun check(value: Boolean, lazyMessage: () -> String) {
        if (!value) {
            cleanupBeforeThrow()
            throw IllegalStateException(lazyMessage())
        }
    }

    /**
     * Ideally this would only be called when an exception is observed in measureRepeated, but to
     * account for java callers, we explicitly trigger before throwing as well.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun cleanupBeforeThrow() {
        if (phaseIndex >= 0 && phaseIndex <= phases.size) {
            Log.d(TAG, "aborting and cancelling benchmark")
            // current phase cancelled, complete current phase cleanup (trace event and profiling)
            InMemoryTracing.endSection()
            currentPhase.profiler?.run { inMemoryTrace("profiling stop") { stop() } }

            // for safety, set other state to done and do broader cleanup
            phaseIndex = phases.size
            afterBenchmark()
        }
    }

    /**
     * Internal loop control for benchmarks - will return true as long as there are more
     * measurements to perform.
     *
     * Actual benchmarks should always go through [keepRunning] or [keepRunningInline], since they
     * optimize the *Iteration* step to have extremely minimal logic performed.
     *
     * The looping behavior is functionally multiple nested loops, e.g.:
     * - Stage - RUNNING_WARMUP vs RUNNING_TIME
     * - Measurement - how many times iterations are measured
     * - Iteration - how many iterations/loops are run between each measurement
     *
     * This has the effect of a 3 layer nesting loop structure, but all condensed to a single method
     * returning true/false to simplify the entry point.
     *
     * @return whether the benchmarking system has anything left to do
     */
    @PublishedApi
    internal fun keepRunningInternal(): Boolean {
        val shouldKeepRunning =
            if (phaseIndex == -1) {
                // Initialize
                beforeBenchmark()
                startNextPhase()
            } else {
                // Trigger another repeat within current phase
                onMeasurementComplete()
            }

        iterationsRemaining = currentLoopsPerMeasurement
        return shouldKeepRunning
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
        check(DeviceInfo.artMainlineVersion != DeviceInfo.ART_MAINLINE_VERSION_UNDETECTED_ERROR) {
            "Unable to detect ART mainline module version to check for interference from method" +
                " tracing, please see logcat for details, and/or file a bug with logcat."
        }
        check(
            !enableMethodTracingAffectsMeasurementError ||
                !DeviceInfo.methodTracingAffectsMeasurements ||
                !MethodTracing.hasBeenUsed
        ) {
            "Measurement prevented by method trace - Running on a device/configuration where " +
                "method tracing affects measurements, and a method trace has been captured " +
                "- no additional benchmarks can be run without restarting the test suite. Use " +
                "ProfilerConfig.MethodTracing.affectsMeasurementOnThisDevice to detect affected " +
                "devices, see its documentation for more info."
        }

        thermalThrottleSleepSeconds = 0

        if (!simplifiedTimingOnlyMode) {
            ThrottleDetector.computeThrottleBaselineIfNeeded()
            ThreadPriority.bumpCurrentThreadPriority()
        }

        totalRunTimeStartNs = System.nanoTime() // Record this time to find total duration
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

    private fun throwIfPaused() =
        check(!paused) {
            "Benchmark loop finished in paused state." +
                " Call BenchmarkState.resumeTiming() before BenchmarkState.keepRunning()."
        }

    private fun getTestResult(testName: String, className: String, perfettoTracePath: String?) =
        BenchmarkData.TestResult(
            name = testName,
            className = className,
            totalRunTimeNs = totalRunTimeNs,
            metrics = metricResults,
            warmupIterations = warmupRepeats,
            repeatIterations = iterationsPerRepeat,
            thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
            profilerOutputs =
                listOfNotNull(
                    perfettoTracePath?.let {
                        BenchmarkData.TestResult.ProfilerOutput(
                            Profiler.ResultFile.ofPerfettoTrace(
                                label = "Trace",
                                absolutePath = perfettoTracePath
                            )
                        )
                    },
                    profilerResult?.let { BenchmarkData.TestResult.ProfilerOutput(it) }
                )
        )

    @ExperimentalBenchmarkStateApi
    fun getMeasurementTimeNs(): List<Double> = metricResults.first { it.name == "timeNs" }.data

    internal fun peekTestResult() =
        checkFinished().run {
            getTestResult(testName = "", className = "", perfettoTracePath = null)
        }

    /**
     * Acquires a status report bundle
     *
     * @param key Run identifier, prepended to bundle properties.
     * @param reportMetrics True if stats should be included in the output bundle.
     */
    internal fun getFullStatusReport(
        key: String,
        reportMetrics: Boolean,
        tracePath: String?
    ): Bundle {
        Log.i(TAG, key + metricResults.map { it.getSummary() } + "count=$iterationsPerRepeat")
        val status = Bundle()
        if (reportMetrics) {
            // these 'legacy' CI output metrics are considered output
            metricResults.forEach { it.putInBundle(status, PREFIX) }
        }
        InstrumentationResultScope(status)
            .reportSummaryToIde(
                testName = key,
                measurements =
                    Measurements(singleMetrics = metricResults, sampledMetrics = emptyList()),
                profilerResults =
                    listOfNotNull(
                        tracePath?.let {
                            Profiler.ResultFile.ofPerfettoTrace(
                                label = "Trace",
                                absolutePath = tracePath
                            )
                        },
                        profilerResult
                    )
            )
        return status
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun report(
        fullClassName: String,
        simpleClassName: String,
        methodName: String,
        perfettoTracePath: String?
    ) {
        if (phaseIndex == -1) {
            return // nothing to report, BenchmarkState wasn't used
        }

        profilerResult?.convertBeforeSync?.invoke()
        if (perfettoTracePath != null) {
            profilerResult?.embedInPerfettoTrace(perfettoTracePath)
        }

        checkFinished() // this method is triggered externally
        val fullTestName = "$PREFIX$simpleClassName.$methodName"
        val bundle =
            getFullStatusReport(
                key = fullTestName,
                reportMetrics = !Arguments.dryRunMode,
                tracePath = perfettoTracePath
            )
        reportBundle(bundle)
        ResultWriter.appendTestResult(
            getTestResult(
                testName = PREFIX + methodName,
                className = fullClassName,
                perfettoTracePath = perfettoTracePath
            )
        )
    }

    companion object {
        internal const val TAG = "Benchmark"

        internal const val REPEAT_COUNT_ALLOCATION = 5

        /**
         * Conservative estimate for how much method tracing slows down runtime how much longer will
         * `methodTrace {x()}` be than `x()`
         *
         * This is a conservative estimate, better version of this would account for OS/Art version
         *
         * Value derived from observed numbers on bramble API 31 (600-800x slowdown)
         */
        internal const val METHOD_TRACING_ESTIMATED_SLOWDOWN_FACTOR = 1000

        /**
         * Maximum duration to trace on main thread to avoid ANRs
         *
         * In practice, other types of tracing can be equally dangerous for ANRs, but method tracing
         * is the default tracing mode.
         */
        internal const val METHOD_TRACING_MAX_DURATION_NS = 4_000_000_000

        internal val DEFAULT_MEASUREMENT_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(100)
        internal val SAMPLED_PROFILER_DURATION_NS =
            TimeUnit.SECONDS.toNanos(Arguments.profilerSampleDurationSeconds)

        private var firstBenchmark = true

        /**
         * Used to disable error to enable internal correctness tests, which need to use method
         * tracing and can safely ignore measurement accuracy
         *
         * Ideally this would function as a true suppressible error like in Errors.kt, but existing
         * error functionality doesn't handle changing error states dynamically
         */
        internal var enableMethodTracingAffectsMeasurementError = true

        @RequiresOptIn
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
         *   Should be no less than 0.
         * @param thermalThrottleSleepSeconds Number of seconds benchmark was paused during thermal
         *   throttling.
         * @param repeatIterations Number of iterations in between each measurement. Should be no
         *   less than 1.
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
            val metricsContainer = MetricsContainer(repeatCount = dataNs.size)
            dataNs.forEachIndexed { index, value -> metricsContainer.data[index][0] = value }
            val metrics = metricsContainer.captureFinished(maxIterations = 1)
            val report =
                BenchmarkData.TestResult(
                    className = className,
                    name = testName,
                    totalRunTimeNs = totalRunTimeNs,
                    metrics = metrics,
                    repeatIterations = repeatIterations,
                    thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
                    warmupIterations = warmupIterations,
                    profilerOutputs = null,
                )
            // Report value to Studio console
            val fullTestName =
                PREFIX + if (className.isNotEmpty()) "$className.$testName" else testName

            instrumentationReport {
                reportSummaryToIde(
                    testName = fullTestName,
                    measurements = Measurements(metrics, emptyList()),
                )
            }

            // Report values to file output
            ResultWriter.appendTestResult(report)
        }
    }
}
