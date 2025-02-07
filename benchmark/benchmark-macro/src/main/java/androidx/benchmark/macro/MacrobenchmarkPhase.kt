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

import android.os.Build
import android.util.Log
import androidx.benchmark.Arguments
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.Outputs
import androidx.benchmark.Profiler
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.benchmark.traceprocessor.Insight
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.StartupInsights
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.tracing.trace
import java.io.File

/** A Profiler being used during a Macro Benchmark Phase. */
internal interface PhaseProfiler {
    /** Starts a Phase profiler. */
    fun start()

    /** Stops a Phase profiler. */
    fun stop(): List<Profiler.ResultFile>
}

/** A [PhaseProfiler] that performs method tracing. */
internal class MethodTracingProfiler(private val scope: MacrobenchmarkScope) : PhaseProfiler {
    override fun start() {
        scope.startMethodTracing()
    }

    override fun stop(): List<Profiler.ResultFile> {
        return scope.stopMethodTracing()
    }
}

internal data class IterationResult(
    val tracePath: String,
    val profilerResultFiles: List<Profiler.ResultFile>,
    val measurements: List<Metric.Measurement>,
    val insights: List<Insight>
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
    perfettoSdkConfig: PerfettoCapture.PerfettoSdkConfig?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
): List<IterationResult> {
    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    val captureInfo =
        Metric.CaptureInfo.forLocalCapture(
            targetPackageName = packageName,
            startupMode = startupMode
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
            scope.fileLabel = "${uniqueName}_iter$iterString"

            var profilerResultFiles: List<Profiler.ResultFile> = emptyList()

            val tracePath =
                perfettoCollector.record(
                    fileLabel = scope.fileLabel,
                    config =
                        experimentalConfig?.perfettoConfig
                            ?: PerfettoConfig.Benchmark(
                                /**
                                 * Prior to API 24, every package name was joined into a single
                                 * setprop which can overflow, and disable *ALL* app level tracing.
                                 *
                                 * For safety here, we only trace the macrobench package on newer
                                 * platforms, and use reflection in the macrobench test process to
                                 * trace important sections
                                 *
                                 * @see androidx.benchmark.macro.perfetto.ForceTracing
                                 */
                                appTagPackages =
                                    if (Build.VERSION.SDK_INT >= 24) {
                                        listOf(packageName, macrobenchmarkPackageName)
                                    } else {
                                        listOf(packageName)
                                    },
                                useStackSamplingConfig = true
                            ),
                    perfettoSdkConfig = perfettoSdkConfig,
                    // Macrobench avoids in-memory tracing, as it doesn't want to either the parsing
                    // errors from out of order events, or risk the memory cost of full ordering
                    // during
                    // trace analysis. If in-memory tracing would be useful, this full ordering cost
                    // should be evaluated.
                    inMemoryTracingLabel = null
                ) {
                    try {
                        trace("start metrics") { metrics.forEach { it.start() } }
                        profiler?.let { trace("start profiler") { it.start() } }
                        trace("measureBlock") { measureBlock(scope) }
                    } finally {
                        profiler?.let {
                            trace("stop profiler") {
                                // Keep track of Profiler Results.
                                profilerResultFiles = it.stop()
                            }
                        }
                        trace("stop metrics") { metrics.forEach { it.stop() } }
                    }
                }!!

            // Append UI state to trace, so tools opening trace will highlight relevant
            // parts in UI.
            val uiState = UiState(highlightPackage = packageName)
            Log.d(TAG, "Iteration $iteration captured $uiState")
            File(tracePath).apply { appendUiState(uiState) }

            // Accumulate measurements
            loadTrace(PerfettoTrace(tracePath)) {
                IterationResult(
                    tracePath = tracePath,
                    profilerResultFiles = profilerResultFiles,
                    measurements =
                        inMemoryTrace("extract metrics") {
                            metrics
                                // capture list of Measurements
                                .map { it.getMeasurements(captureInfo, this) }
                                // merge together
                                .reduceOrNull() { sum, element -> sum.merge(element) }
                                ?: emptyList()
                        },
                    insights =
                        if (experimentalConfig?.startupInsightsConfig?.isEnabled == true) {
                            StartupInsights(helpUrlBase = Arguments.startupInsightsHelpUrlBase)
                                .queryInsights(
                                    session = this,
                                    packageName = packageName,
                                    traceLinkTitle = "$iteration",
                                    traceLinkPath = Outputs.relativePathFor(tracePath)
                                )
                        } else {
                            emptyList()
                        }
                )
            }
        }
    } finally {
        scope.killProcess()
    }
}
