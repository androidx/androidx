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

package androidx.benchmark.macro

import androidx.benchmark.Arguments
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.MemoryProfilingConfig
import androidx.benchmark.Outputs
import androidx.benchmark.Profiler
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.traceprocessor.Insight
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.StartupInsights
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.tracing.trace

/** A Profiler being used during a Macro Benchmark Phase. */
internal interface PhaseProfiler {
    /** Starts profiling for the iteration. */
    fun start() {}

    /** Stops profiling for the iteration. */
    fun stop() {}

    /**
     * Custom [PerfettoConfig] to use for the primary trace during this phase, or `null` to use the
     * default benchmark configuration.
     */
    val perfettoConfig: PerfettoConfig?
        get() = null

    /**
     * Creates or returns the [Profiler.ResultFile]s produced for this iteration.
     *
     * @param tracePath The absolute path to the Perfetto trace captured during this iteration.
     * @param iteration The current iteration index.
     */
    fun createProfilerResultFiles(
        tracePath: String,
        iteration: Int,
    ): List<Profiler.ResultFile>
}

/** A [PhaseProfiler] that performs method tracing. */
internal class MethodTracingProfiler(private val scope: MacrobenchmarkScope) : PhaseProfiler {
    private var resultFiles: List<Profiler.ResultFile> = emptyList()

    override fun start() {
        resultFiles = emptyList()
        scope.startMethodTracing()
    }

    override fun stop() {
        resultFiles = scope.stopMethodTracing()
    }

    override fun createProfilerResultFiles(
        tracePath: String,
        iteration: Int,
    ): List<Profiler.ResultFile> =
        listOf(
            Profiler.ResultFile.ofPerfettoTrace(
                label = "Method Trace Perfetto Iteration $iteration",
                absolutePath = tracePath,
            )
        ) + resultFiles
}

/** A [PhaseProfiler] that performs memory profiling via Perfetto heap profiling (heapprofd). */
@OptIn(ExperimentalBenchmarkConfigApi::class)
internal class MemoryProfilingProfiler(
    scope: MacrobenchmarkScope,
    memoryProfilingConfig: MemoryProfilingConfig,
) : PhaseProfiler {
    override val perfettoConfig: PerfettoConfig =
        PerfettoConfig.MemoryProfiling(scope.packageName, memoryProfilingConfig)

    override fun createProfilerResultFiles(
        tracePath: String,
        iteration: Int,
    ): List<Profiler.ResultFile> =
        listOf(
            Profiler.ResultFile.ofPerfettoTrace(
                label = "Memory Profiling Iteration $iteration",
                absolutePath = tracePath,
            )
        )
}

internal data class IterationResult(
    /**
     * Absolute path to the Perfetto trace captured during this iteration, or `null` if this
     * iteration was run for a phase profiler.
     *
     * Profiling phases (e.g. method tracing or memory profiling) route all their trace artifacts
     * (including their primary Perfetto trace) through [profilerResultFiles] rather than
     * [tracePath], so that regular measurement iteration traces and profiling traces are reported
     * under separate sections in benchmark results and IDE summaries.
     */
    val tracePath: String?,
    val profilerResultFiles: List<Profiler.ResultFile>,
    val measurements: List<Metric.Measurement>,
    val insights: List<Insight>,
)

/** Run a Macrobenchmark Phase and collect a list of [IterationResult]. */
@ExperimentalBenchmarkConfigApi
internal fun TraceProcessor.runPhase(
    uniqueName: String,
    packageName: String,
    macrobenchmarkPackageName: String,
    iterations: Int,
    startupMode: StartupMode?,
    scope: MacrobenchmarkScope,
    profiler: PhaseProfiler?,
    metrics: List<Metric>,
    experimentalConfig: ExperimentalConfig?,
    tracingLibraryConfig: PerfettoCapture.TracingLibraryConfig?,
    traceSuffix: String? = null,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit,
): List<IterationResult> {
    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    val captureInfo =
        Metric.CaptureInfo.forLocalCapture(
            targetPackageName = packageName,
            startupMode = startupMode,
        )
    try {
        // Configure metrics in the Phase.
        metrics.forEach { it.configure(captureInfo) }
        return List(iterations) { iteration ->
            // Wake the device to ensure it stays awake with large iteration count
            inMemoryTrace("wake device") { scope.device.wakeUp() }

            scope.iteration = iteration

            inMemoryTrace("setupBlock") { setupBlock(scope) }

            // Setup file labels.
            val iterString = iteration.toString().padStart(3, '0')
            val fileSuffix = if (traceSuffix != null) "-$traceSuffix" else ""
            scope.fileLabel = "${uniqueName}_iter$iterString$fileSuffix"

            val tracePath =
                perfettoCollector.record(
                    fileLabel = scope.fileLabel,
                    config =
                        profiler?.perfettoConfig
                            ?: experimentalConfig?.perfettoConfig
                            ?: PerfettoConfig.Benchmark(
                                appTagPackages = listOf(packageName, macrobenchmarkPackageName),
                                useStackSamplingConfig = true,
                            ),
                    tracingLibraryConfig = tracingLibraryConfig,
                    // Macrobench avoids in-memory tracing, as it doesn't want to either the
                    // parsing errors from out of order events, or risk the memory cost of full
                    // ordering during trace analysis. If in-memory tracing would be useful,
                    // this full ordering cost should be evaluated.
                    inMemoryTracingLabel = null,
                ) {
                    try {
                        trace("start metrics") { metrics.forEach { it.start() } }
                        profiler?.let { trace("start profiler") { it.start() } }
                        trace("measureBlock") { measureBlock(scope) }
                    } finally {
                        profiler?.let { trace("stop profiler") { it.stop() } }
                        trace("stop metrics") { metrics.forEach { it.stop() } }
                    }
                }!!

            val profilerResultFiles =
                profiler?.createProfilerResultFiles(tracePath, iteration) ?: emptyList()
            // When a profiler is active, all resulting traces (both the primary Perfetto trace and
            // any profiler-specific trace files) are reported via [profilerResultFiles]. We set
            // [iterationTracePath] to null so that profiling traces are not output under the
            // standard measurement "Traces: Iteration X" list.
            val iterationTracePath = if (profiler != null) null else tracePath

            // When metrics are empty (such as during a dedicated profiling phase like memory
            // profiling),
            // skip loading the trace into TraceProcessor to avoid unnecessary trace analysis
            // overhead.
            if (metrics.isEmpty()) {
                IterationResult(
                    tracePath = iterationTracePath,
                    profilerResultFiles = profilerResultFiles,
                    measurements = emptyList(),
                    insights = emptyList(),
                )
            } else {
                // Accumulate measurements
                loadTrace(PerfettoTrace(tracePath)) {
                    IterationResult(
                        tracePath = iterationTracePath,
                        profilerResultFiles = profilerResultFiles,
                        measurements =
                            inMemoryTrace("extract metrics") {
                                metrics
                                    // capture list of Measurements
                                    .map { it.getMeasurements(captureInfo, this) }
                                    // merge together
                                    .reduceOrNull { sum, element -> sum.merge(element) }
                                    ?: emptyList()
                            },
                        insights =
                            if (experimentalConfig?.startupInsightsConfig?.isEnabled == true) {
                                StartupInsights(helpUrlBase = Arguments.startupInsightsHelpUrlBase)
                                    .queryInsights(
                                        session = this,
                                        packageName = packageName,
                                        traceLinkTitle = "$iteration",
                                        traceLinkPath = Outputs.relativePathFor(tracePath),
                                    )
                            } else {
                                emptyList()
                            },
                    )
                }
            }
        }
    } finally {
        scope.killProcess()
    }
}
