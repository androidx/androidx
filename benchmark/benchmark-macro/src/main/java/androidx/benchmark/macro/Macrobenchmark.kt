/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.BenchmarkResult
import androidx.benchmark.ConfigurationError
import androidx.benchmark.DeviceInfo
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.ResultWriter
import androidx.benchmark.UserspaceTracing
import androidx.benchmark.checkAndGetSuppressionState
import androidx.benchmark.conditionalError
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.benchmark.userspaceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.File

@Suppress("DEPRECATION")
internal fun checkErrors(packageName: String): ConfigurationError.SuppressionState? {
    val pm = InstrumentationRegistry.getInstrumentation().context.packageManager

    val applicationInfo = try {
        pm.getApplicationInfo(packageName, 0)
    } catch (notFoundException: PackageManager.NameNotFoundException) {
        throw AssertionError(
            "Unable to find target package $packageName, is it installed?",
            notFoundException
        )
    }

    val errorNotProfileable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        applicationInfo.isNotProfileableByShell()
    } else {
        false
    }

    val errors = DeviceInfo.errors +
        // TODO: Merge this debuggable check / definition with Errors.kt in benchmark-common
        listOfNotNull(
            conditionalError(
                hasError = applicationInfo.flags.and(FLAG_DEBUGGABLE) != 0,
                id = "DEBUGGABLE",
                summary = "Benchmark Target is Debuggable",
                message = """
                    Target package $packageName
                    is running with debuggable=true, which drastically reduces
                    runtime performance in order to support debugging features. Run
                    benchmarks with debuggable=false. Debuggable affects execution speed
                    in ways that mean benchmark improvements might not carry over to a
                    real user's experience (or even regress release performance).
                """.trimIndent()
            ),
            conditionalError(
                hasError = errorNotProfileable,
                id = "NOT-PROFILEABLE",
                summary = "Benchmark Target is NOT profileable",
                message = """
                    Target package $packageName
                    is running without profileable. Profileable is required to enable
                    macrobenchmark to capture detailed trace information from the target process,
                    such as System tracing sections defined in the app, or libraries.

                    To make the target profileable, add the following in your target app's
                    main AndroidManifest.xml, within the application tag:

                    <!--suppress AndroidElementNotAllowed -->
                    <profileable android:shell="true"/>
                """.trimIndent()
            )
        ).sortedBy { it.id }

    return errors.checkAndGetSuppressionState(Arguments.suppressedErrors)
}

/**
 * macrobenchmark test entrypoint, which doesn't depend on JUnit.
 *
 * This function is a building block for public testing APIs
 */
private fun macrobenchmark(
    uniqueName: String,
    className: String,
    testName: String,
    packageName: String,
    metrics: List<Metric>,
    compilationMode: CompilationMode,
    iterations: Int,
    launchWithClearTask: Boolean,
    startupModeMetricHint: StartupMode?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    require(iterations > 0) {
        "Require iterations > 0 (iterations = $iterations)"
    }
    require(metrics.isNotEmpty()) {
        "Empty list of metrics passed to metrics param, must pass at least one Metric"
    }

    // skip benchmark if not supported by vm settings
    compilationMode.assumeSupportedWithVmSettings()

    val suppressionState = checkErrors(packageName)
    var warningMessage = suppressionState?.warningMessage ?: ""

    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(packageName, launchWithClearTask)

    // Ensure the device is awake
    scope.device.wakeUp()

    // Always kill the process at beginning of test
    scope.killProcess()

    userspaceTrace("compile $packageName") {
        compilationMode.resetAndCompile(packageName) {
            setupBlock(scope)
            measureBlock(scope)
        }
    }

    // package name for macrobench process, so it's captured as well
    val macrobenchPackageName = InstrumentationRegistry.getInstrumentation().context.packageName

    // Perfetto collector is separate from metrics, so we can control file
    // output, and give it different (test-wide) lifecycle
    val perfettoCollector = PerfettoCaptureWrapper()
    val tracePaths = mutableListOf<String>()
    try {
        metrics.forEach {
            it.configure(packageName)
        }
        val measurements = List(iterations) { iteration ->
            // Wake the device to ensure it stays awake with large iteration count
            userspaceTrace("wake device") {
                scope.device.wakeUp()
            }

            scope.iteration = iteration
            userspaceTrace("setupBlock") {
                setupBlock(scope)
            }

            val tracePath = perfettoCollector.record(
                benchmarkName = uniqueName,
                iteration = iteration,

                /**
                 * Prior to API 24, every package name was joined into a single setprop which can
                 * overflow, and disable *ALL* app level tracing.
                 *
                 * For safety here, we only trace the macrobench package on newer platforms, and use
                 * reflection in the macrobench test process to trace important sections
                 *
                 * @see androidx.benchmark.macro.perfetto.ForceTracing
                 */
                packages = if (Build.VERSION.SDK_INT >= 24) {
                    listOf(packageName, macrobenchPackageName)
                } else {
                    listOf(packageName)
                }
            ) {
                try {
                    trace("start metrics") {
                        metrics.forEach {
                            it.start()
                        }
                    }
                    trace("measureBlock") {
                        measureBlock(scope)
                    }
                } finally {
                    trace("stop metrics") {
                        metrics.forEach {
                            it.stop()
                        }
                    }
                }
            }!!

            tracePaths.add(tracePath)

            val iterationResult = userspaceTrace("extract metrics") {
                metrics
                    // capture list of Map<String,Long> per metric
                    .map { it.getMetrics(Metric.CaptureInfo(
                        targetPackageName = packageName,
                        testPackageName = macrobenchPackageName,
                        startupMode = startupModeMetricHint,
                        apiLevel = Build.VERSION.SDK_INT
                    ), tracePath) }
                    // merge into one map
                    .reduce { sum, element -> sum + element }
            }
            // append UI state to trace, so tools opening trace will highlight relevant part in UI
            val uiState = UiState(
                timelineStart = iterationResult.timelineRangeNs?.first,
                timelineEnd = iterationResult.timelineRangeNs?.last,
                highlightPackage = packageName
            )
            File(tracePath).apply {
                // Disabled currently, see b/194424816 and b/174007010
                // appendBytes(UserspaceTracing.commitToTrace().encode())
                UserspaceTracing.commitToTrace() // clear buffer

                appendUiState(uiState)
            }
            Log.d(TAG, "Iteration $iteration captured $uiState")

            // report just the metrics
            iterationResult
        }.mergeIterationMeasurements()

        require(measurements.isNotEmpty()) {
            """
                Unable to read any metrics during benchmark (metric list: $metrics).
                Check that you're performing the operations to be measured. For example, if
                using StartupTimingMetric, are you starting an activity for the specified package
                in the measure block?
            """.trimIndent()
        }
        InstrumentationResults.instrumentationReport {
            val (summaryV1, summaryV2) = ideSummaryStrings(
                warningMessage,
                uniqueName,
                measurements,
                tracePaths
            )
            ideSummaryRecord(summaryV1 = summaryV1, summaryV2 = summaryV2)
            warningMessage = "" // warning only printed once
            measurements.singleMetrics.forEach {
                it.putInBundle(bundle, suppressionState?.prefix ?: "")
            }
            measurements.sampledMetrics.forEach {
                it.putPercentilesInBundle(bundle, suppressionState?.prefix ?: "")
            }
        }

        val warmupIterations = when (compilationMode) {
            is CompilationMode.Partial -> compilationMode.warmupIterations
            else -> 0
        }

        ResultWriter.appendReport(
            BenchmarkResult(
                className = className,
                testName = testName,
                totalRunTimeNs = System.nanoTime() - startTime,
                metrics = measurements,
                repeatIterations = iterations,
                thermalThrottleSleepSeconds = 0,
                warmupIterations = warmupIterations
            )
        )
    } finally {
        scope.killProcess()
    }
}

/**
 * Run a macrobenchmark with the specified StartupMode
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun macrobenchmarkWithStartupMode(
    uniqueName: String,
    className: String,
    testName: String,
    packageName: String,
    metrics: List<Metric>,
    compilationMode: CompilationMode,
    iterations: Int,
    startupMode: StartupMode?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    macrobenchmark(
        uniqueName = uniqueName,
        className = className,
        testName = testName,
        packageName = packageName,
        metrics = metrics,
        compilationMode = compilationMode,
        iterations = iterations,
        startupModeMetricHint = startupMode,
        setupBlock = {
            if (startupMode == StartupMode.COLD) {
                killProcess()
                // Shader caches are stored in the code cache directory. Make sure that
                // they are cleared every iteration.
                dropShaderCache()
                // drop app pages from page cache to ensure it is loaded from disk, from scratch

                // resetAndCompile uses ProfileInstallReceiver to write a skip file.
                // This is done to reduce the interference from ProfileInstaller,
                // so long-running benchmarks don't get optimized due to a background dexopt.

                // To restore the state of the process we need to drop app pages so its
                // loaded from disk, from scratch.
                dropKernelPageCache()
            } else if (iteration == 0 && startupMode != null) {
                try {
                    iteration = null // override to null for warmup, before starting measurements

                    // warmup process by running the measure block once unmeasured
                    setupBlock(this)
                    measureBlock()
                } finally {
                    iteration = 0
                }
            }
            setupBlock(this)
        },
        // Don't reuse activities by default in COLD / WARM
        launchWithClearTask = startupMode == StartupMode.COLD || startupMode == StartupMode.WARM,
        measureBlock = measureBlock
    )
}
