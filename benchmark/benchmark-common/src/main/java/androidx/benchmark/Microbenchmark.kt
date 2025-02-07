/*
 * Copyright 2024 The Android Open Source Project
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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.BenchmarkState.Companion.enableMethodTracingAffectsMeasurementError
import androidx.benchmark.json.BenchmarkData.TestResult.ProfilerOutput
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.runSingleSessionServer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.Trace
import androidx.tracing.trace
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * Scope handle for pausing/resuming microbenchmark measurement.
 *
 * This is functionally an equivalent to `BenchmarkRule.Scope` which will work without the JUnit
 * dependency.
 */
open class MicrobenchmarkScope
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(internal val state: MicrobenchmarkRunningState) {
    /**
     * Disable measurement for a block of code.
     *
     * Used for disabling timing/measurement for work that isn't part of the benchmark:
     * - When constructing per-loop randomized inputs for operations with caching,
     * - Controlling which parts of multi-stage work are measured (e.g. View measure/layout)
     * - Per-loop verification
     *
     * @sample androidx.benchmark.samples.runWithMeasurementDisabledSample
     */
    inline fun <T> runWithMeasurementDisabled(block: () -> T): T {
        pauseMeasurement()
        // Note: we only bother with tracing for the runWithMeasurementDisabled function for
        // Kotlin callers, since we want to avoid corrupting the trace with incorrectly paired
        // pauseMeasurement/resumeMeasurement calls
        val ret: T =
            try {
                // have to use begin/end since block isn't crossinline
                Trace.beginSection("runWithMeasurementDisabled")
                block()
            } finally {
                Trace.endSection()
            }
        resumeMeasurement()
        return ret
    }

    /**
     * Resume measurement after a call to [pauseMeasurement].
     *
     * Kotlin callers should generally instead use [runWithMeasurementDisabled].
     */
    fun pauseMeasurement() {
        state.pauseMeasurement()
    }

    /**
     * Resume measurement after a call to [pauseMeasurement]
     *
     * Kotlin callers should generally instead use [runWithMeasurementDisabled].
     */
    fun resumeMeasurement() {
        state.resumeMeasurement()
    }
}

/**
 * State carried across multiple phases, including metric and output files
 *
 * This is maintained as a state object rather than return objects from each phase to avoid
 * allocation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MicrobenchmarkRunningState
internal constructor(metrics: MetricsContainer, val yieldThreadPeriodically: Boolean) {
    internal var warmupEstimatedIterationTimeNs: Long = 0
    internal var warmupIterations: Int = 0
    internal var totalThermalThrottleSleepSeconds: Long = 0
    internal var maxIterationsPerRepeat = 0
    internal var metrics: MetricsContainer = metrics
    internal var metricResults = mutableListOf<MetricResult>()
    internal var profilerResults = mutableListOf<Profiler.ResultFile>()
    internal var paused = false

    internal var initialTimeNs: Long = 0
    internal var softDeadlineNs: Long = 0
    internal var hardDeadlineNs: Long = 0

    fun pauseMeasurement() {
        check(!paused) { "Unable to pause the benchmark. The benchmark has already paused." }
        metrics.capturePaused()
        paused = true
    }

    fun resumeMeasurement() {
        check(paused) { "Unable to resume the benchmark. The benchmark is already running." }
        metrics.captureResumed()
        paused = false
    }

    fun beginTaskTrace() {
        if (yieldThreadPeriodically) {
            Trace.beginSection("benchmark task")
            initialTimeNs = System.nanoTime()
            // we try to stop next measurement after soft deadline...
            softDeadlineNs = initialTimeNs + TimeUnit.SECONDS.toNanos(2)
            // ... and throw if took longer than hard deadline
            hardDeadlineNs = initialTimeNs + TimeUnit.SECONDS.toNanos(10)
        }
    }

    fun endTaskTrace() {
        if (yieldThreadPeriodically) {
            Trace.endSection()
        }
    }

    internal suspend inline fun yieldThreadIfDeadlinePassed() {
        if (yieldThreadPeriodically) {
            val timeNs = System.nanoTime()
            if (timeNs >= softDeadlineNs) {

                if (timeNs > hardDeadlineNs && Arguments.measureRepeatedOnMainThrowOnDeadline) {
                    val overrunInSec = (timeNs - hardDeadlineNs) / 1_000_000_000.0
                    // note - we throw without cancelling task trace, since outer layer handles that
                    throw IllegalStateException(
                        "Benchmark loop overran hard time limit by $overrunInSec seconds"
                    )
                }

                // pause and resume task trace around yield
                endTaskTrace()
                yield()
                beginTaskTrace()
            }
        }
    }
}

private var firstBenchmark = true

private fun checkForErrors() {
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
}

internal typealias LoopedMeasurementBlock = suspend (MicrobenchmarkScope, Int) -> Unit

internal typealias ScopeFactory = (MicrobenchmarkRunningState) -> MicrobenchmarkScope

private fun <T> runBlockingOverrideMain(
    runOnMainDispatcher: Boolean,
    block: suspend CoroutineScope.() -> T
): T {
    return if (runOnMainDispatcher) {
        runBlocking(Dispatchers.Main, block)
    } else {
        runBlocking { block(this) }
    }
}

internal fun captureMicroPerfettoTrace(
    definition: TestDefinition,
    config: MicrobenchmarkConfig?,
    block: () -> Unit
): String? =
    PerfettoCaptureWrapper()
        .record(
            fileLabel = definition.traceUniqueName,
            config =
                PerfettoConfig.Benchmark(
                    appTagPackages =
                        if (config?.traceAppTagEnabled == true) {
                            listOf(InstrumentationRegistry.getInstrumentation().context.packageName)
                        } else {
                            emptyList()
                        },
                    useStackSamplingConfig = false
                ),
            // TODO(290918736): add support for Perfetto SDK Tracing in
            //  Microbenchmark in other cases, outside of MicrobenchmarkConfig
            perfettoSdkConfig =
                if (
                    config?.perfettoSdkTracingEnabled == true &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ) {
                    PerfettoCapture.PerfettoSdkConfig(
                        InstrumentationRegistry.getInstrumentation().context.packageName,
                        PerfettoCapture.PerfettoSdkConfig.InitialProcessState.Alive
                    )
                } else {
                    null
                },

            // Optimize throughput in dryRunMode, since trace isn't useful, and extremely
            //   expensive on some emulators. Could alternately use UserspaceTracing if
            // desired
            // Additionally, skip on misconfigured devices to still enable benchmarking.
            enableTracing = !Arguments.dryRunMode && !DeviceInfo.misconfiguredForTracing,
            inMemoryTracingLabel = "Microbenchmark",
            block = block
        )

/**
 * Core engine of microbenchmark, used in one of three ways:
 * 1. [measureRepeatedImplWithTracing] - standard tracing microbenchmark
 * 2. [measureRepeatedImplNoTracing] - legacy, non-tracing, suspending functionality backing
 *    [BenchmarkState] compatibility
 * 3. [measureRepeatedCheckNanosReentrant] - microbenchmark which avoids modifying global state,
 *    which runs *within* other variants to check for thermal throttling
 */
internal class Microbenchmark(
    private val definition: TestDefinition,
    private val phaseConfig: MicrobenchmarkPhase.Config,
    private val yieldThreadPeriodically: Boolean,
    private val scopeFactory: ScopeFactory,
    private val loopedMeasurementBlock: LoopedMeasurementBlock
) {
    constructor(
        definition: TestDefinition,
        config: MicrobenchmarkConfig,
        simplifiedTimingOnlyMode: Boolean,
        yieldThreadPeriodically: Boolean,
        scopeFactory: ScopeFactory = { runningState -> MicrobenchmarkScope(runningState) },
        loopedMeasurementBlock: LoopedMeasurementBlock
    ) : this(
        definition = definition,
        phaseConfig = MicrobenchmarkPhase.Config(config, simplifiedTimingOnlyMode),
        yieldThreadPeriodically = yieldThreadPeriodically,
        scopeFactory = scopeFactory,
        loopedMeasurementBlock = loopedMeasurementBlock
    )

    private var startTimeNs = System.nanoTime()

    init {
        if (!phaseConfig.simplifiedTimingOnlyMode) {
            Log.d(TAG, "-- Running ${definition.fullNameUnsanitized} --")
            checkForErrors()
        }
    }

    private val phases = phaseConfig.generatePhases()
    private val state =
        MicrobenchmarkRunningState(phases[0].metricsContainer, yieldThreadPeriodically)
    private val scope = scopeFactory(state)

    suspend fun executePhases() {
        state.beginTaskTrace()
        try {
            if (!phaseConfig.simplifiedTimingOnlyMode) {
                ThrottleDetector.computeThrottleBaselineIfNeeded()
                ThreadPriority.bumpCurrentThreadPriority()
            }
            firstBenchmark = false
            phases.forEach {
                it.execute(
                    traceUniqueName = definition.traceUniqueName,
                    scope = scope,
                    state = state,
                    loopedMeasurementBlock = loopedMeasurementBlock
                )
            }
        } finally {
            if (!phaseConfig.simplifiedTimingOnlyMode) {
                // Don't modify thread priority in simplified timing mode, since 'outer'
                // measureRepeated owns thread priority
                ThreadPriority.resetBumpedThread()
            }
            phaseConfig.warmupManager.logInfo()
            state.endTaskTrace()
        }
    }

    /** Register a PerfettoTrace to be added to outputs, and used to extract metrics. */
    @RequiresApi(23)
    fun processPerfettoTrace(perfettoTracePath: String) {
        // trace completed, and copied into shell writeable dir
        val file = File(perfettoTracePath)
        file.appendUiState(
            UiState(
                timelineStart = null,
                timelineEnd = null,
                highlightPackage = InstrumentationRegistry.getInstrumentation().context.packageName
            )
        )
        state.profilerResults.forEach { it.embedInPerfettoTrace(perfettoTracePath) }
        if (state.profilerResults.any { it.type == ProfilerOutput.Type.MethodTrace }) {
            TraceProcessor.runSingleSessionServer(absoluteTracePath = perfettoTracePath) {
                // NOTE: this query assumes that method trace only occurs once
                state.metricResults.addAll(MethodTracing.queryMetrics(this))
            }
        }

        // add at front since this affects output order
        state.profilerResults.add(
            0,
            Profiler.ResultFile.ofPerfettoTrace(label = "Trace", absolutePath = perfettoTracePath)
        )
    }

    fun output(): MicrobenchmarkOutput {
        Log.i(
            BenchmarkState.TAG,
            definition.outputTestName +
                state.metricResults.map { it.getSummary() } +
                "count=${state.maxIterationsPerRepeat}"
        )
        state.profilerResults.forEach { it.convertBeforeSync?.invoke() }
        return MicrobenchmarkOutput(
                definition = definition,
                metricResults = state.metricResults,
                profilerResults = state.profilerResults,
                totalRunTimeNs = System.nanoTime() - startTimeNs,
                warmupIterations = state.warmupIterations,
                repeatIterations = state.maxIterationsPerRepeat,
                thermalThrottleSleepSeconds = state.totalThermalThrottleSleepSeconds,
                reportMetricsInBundle = !Arguments.dryRunMode
            )
            .apply {
                InstrumentationResults.reportBundle(createBundle())
                ResultWriter.appendTestResult(createJsonTestResult())
            }
    }

    fun getMinTimeNanos(): Double {
        return state.metricResults.first { it.name == "timeNs" }.min
    }

    companion object {
        internal const val TAG = "Benchmark"
    }
}

internal inline fun measureRepeatedCheckNanosReentrant(
    crossinline measureBlock: MicrobenchmarkScope.() -> Unit
): Double {
    return Microbenchmark(
            TestDefinition(
                fullClassName = "ThrottleDetector",
                simpleClassName = "ThrottleDetector",
                methodName = "checkThrottle"
            ),
            config = MicrobenchmarkConfig(),
            simplifiedTimingOnlyMode = true,
            yieldThreadPeriodically = false,
            loopedMeasurementBlock = { scope, iterations ->
                var remainingIterations = iterations
                do {
                    measureBlock.invoke(scope)
                    remainingIterations--
                } while (remainingIterations > 0)
            }
        )
        .run {
            runBlocking { executePhases() }
            getMinTimeNanos()
        }
}

/**
 * Limited version of [measureRepeatedImplWithTracing] which doesn't capture a trace, and doesn't
 * support posting work to main thread.
 */
internal suspend fun measureRepeatedImplNoTracing(
    definition: TestDefinition,
    config: MicrobenchmarkConfig,
    loopedMeasurementBlock: LoopedMeasurementBlock
) {
    Microbenchmark(
            definition = definition,
            config = config,
            simplifiedTimingOnlyMode = false,
            yieldThreadPeriodically = false,
            loopedMeasurementBlock = loopedMeasurementBlock
        )
        .apply {
            executePhases()
            output()
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun measureRepeatedImplWithTracing(
    definition: TestDefinition,
    config: MicrobenchmarkConfig?,
    postToMainThread: Boolean,
    scopeFactory: ScopeFactory = { runningState -> MicrobenchmarkScope(runningState) },
    loopedMeasurementBlock: LoopedMeasurementBlock
) {
    val microbenchmark =
        Microbenchmark(
            definition = definition,
            config = config ?: MicrobenchmarkConfig(),
            simplifiedTimingOnlyMode = false,
            yieldThreadPeriodically = postToMainThread,
            scopeFactory = scopeFactory,
            loopedMeasurementBlock = loopedMeasurementBlock
        )
    val perfettoTracePath =
        captureMicroPerfettoTrace(definition, config) {
            trace(definition.fullNameUnsanitized) {
                runBlockingOverrideMain(runOnMainDispatcher = postToMainThread) {
                    microbenchmark.executePhases()
                }
            }
        }
    if (perfettoTracePath != null && Build.VERSION.SDK_INT > 23) {
        microbenchmark.processPerfettoTrace(perfettoTracePath)
    }
    microbenchmark.output()
}

/**
 * Top level entry point for capturing a microbenchmark with a trace.
 *
 * Eventually this method (or one like it) should be public, and also expose a results object
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun measureRepeated(
    definition: TestDefinition,
    config: MicrobenchmarkConfig? = null,
    crossinline measureBlock: MicrobenchmarkScope.() -> Unit
) {
    measureRepeatedImplWithTracing(
        postToMainThread = false,
        definition = definition,
        config = config,
        loopedMeasurementBlock = { scope, iterations ->
            var remainingIterations = iterations
            do {
                measureBlock.invoke(scope)
                remainingIterations--
            } while (remainingIterations > 0)
        }
    )
}
