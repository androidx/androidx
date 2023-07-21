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

import android.content.pm.ApplicationInfo
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
import androidx.benchmark.Shell
import androidx.benchmark.UserspaceTracing
import androidx.benchmark.checkAndGetSuppressionState
import androidx.benchmark.conditionalError
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoTrace
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.UiState
import androidx.benchmark.perfetto.appendUiState
import androidx.benchmark.userspaceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.File

/**
 * Get package ApplicationInfo, throw if not found
 */
@Suppress("DEPRECATION")
internal fun getInstalledPackageInfo(packageName: String): ApplicationInfo {
    val pm = InstrumentationRegistry.getInstrumentation().context.packageManager
    try {
        return pm.getApplicationInfo(packageName, 0)
    } catch (notFoundException: PackageManager.NameNotFoundException) {
        throw AssertionError(
            "Unable to find target package $packageName, is it installed?",
            notFoundException
        )
    }
}

internal fun checkErrors(packageName: String): ConfigurationError.SuppressionState? {
    Arguments.throwIfError()

    val applicationInfo = getInstalledPackageInfo(packageName)

    val errorNotProfileable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        applicationInfo.isNotProfileableByShell()
    } else {
        false
    }

    val instrumentation = InstrumentationRegistry.getInstrumentation()
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
            ),
            conditionalError(
                hasError = instrumentation.targetContext.packageName !=
                    instrumentation.context.packageName,
                id = "NOT-SELF-INSTRUMENTING",
                summary = "Benchmark manifest is instrumenting separate process",
                message = """
                    Macrobenchmark instrumentation target in manifest
                    ${instrumentation.targetContext.packageName} does not match macrobenchmark
                    package ${instrumentation.context.packageName}. While macrobenchmarks 'target' a
                    separate app they measure, they can not declare it as their instrumentation
                    targetPackage in their manifest. Doing so would cause the macrobenchmark test
                    app to be loaded into the target application process, which would prevent
                    macrobenchmark from killing, compiling, or launching the target process.

                    Ensure your macrobenchmark test apk's manifest matches the manifest package, and
                    instrumentation target package, also called 'self-instrumenting':

                    <manifest
                        package="com.mymacrobenchpackage" ...>
                        <instrumentation
                            android:name="androidx.benchmark.junit4.AndroidBenchmarkRunner"
                            android:targetPackage="mymacrobenchpackage"/>

                    In gradle library modules, this is the default behavior. In gradle test modules,
                    specify the experimental self-instrumenting property:
                    android {
                        targetProjectPath = ":app"
                        // Enable the benchmark to run separately from the app process
                        experimentalProperties["android.experimental.self-instrumenting"] = true
                    }
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
    userspaceTracingPackage: String?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
) {
    require(iterations > 0) {
        "Require iterations > 0 (iterations = $iterations)"
    }
    require(metrics.isNotEmpty()) {
        "Empty list of metrics passed to metrics param, must pass at least one Metric"
    }

    val suppressionState = checkErrors(packageName)
    var warningMessage = suppressionState?.warningMessage ?: ""

    // skip benchmark if not supported by vm settings
    compilationMode.assumeSupportedWithVmSettings()

    val startTime = System.nanoTime()
    val scope = MacrobenchmarkScope(packageName, launchWithClearTask)

    // Ensure the device is awake
    scope.device.wakeUp()

    // Always kill the process at beginning of test
    scope.killProcess()

    userspaceTrace("compile $packageName") {
        compilationMode.resetAndCompile(packageName, killProcessBlock = scope::killProcess) {
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
        val measurements = PerfettoTraceProcessor.runServer {
            List(if (Arguments.dryRunMode) 1 else iterations) { iteration ->
                // Wake the device to ensure it stays awake with large iteration count
                userspaceTrace("wake device") {
                    scope.device.wakeUp()
                }

                scope.iteration = iteration
                userspaceTrace("setupBlock") {
                    setupBlock(scope)
                }

                val iterString = iteration.toString().padStart(3, '0')
                val tracePath = perfettoCollector.record(
                    fileLabel = "${uniqueName}_iter$iterString",
                    config = PerfettoConfig.Benchmark(
                        /**
                         * Prior to API 24, every package name was joined into a single setprop
                         * which can overflow, and disable *ALL* app level tracing.
                         *
                         * For safety here, we only trace the macrobench package on newer platforms,
                         * and use reflection in the macrobench test process to trace important
                         * sections
                         *
                         * @see androidx.benchmark.macro.perfetto.ForceTracing
                         */
                        appTagPackages = if (Build.VERSION.SDK_INT >= 24) {
                            listOf(packageName, macrobenchPackageName)
                        } else {
                            listOf(packageName)
                        },
                    ),
                    userspaceTracingPackage = userspaceTracingPackage
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

                val measurementList = loadTrace(PerfettoTrace(tracePath)) {
                    // Extracts the metrics using the perfetto trace processor
                    userspaceTrace("extract metrics") {
                        metrics
                            // capture list of Measurements
                            .map {
                                it.getResult(
                                    Metric.CaptureInfo(
                                        targetPackageName = packageName,
                                        testPackageName = macrobenchPackageName,
                                        startupMode = startupModeMetricHint,
                                        apiLevel = Build.VERSION.SDK_INT
                                    ),
                                    this
                                )
                            }
                            // merge together
                            .reduce { sum, element -> sum.merge(element) }
                    }
                }

                // append UI state to trace, so tools opening trace will highlight relevant part in UI
                val uiState = UiState(
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
                measurementList
            }.mergeMultiIterResults()
        }

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
    val userspaceTracingPackage = if (Arguments.fullTracingEnable &&
        startupMode != StartupMode.COLD // can't use with COLD, since the broadcast wakes up target
    ) {
        packageName
    } else {
        null
    }
    macrobenchmark(
        uniqueName = uniqueName,
        className = className,
        testName = testName,
        packageName = packageName,
        metrics = metrics,
        compilationMode = compilationMode,
        iterations = iterations,
        startupModeMetricHint = startupMode,
        userspaceTracingPackage = userspaceTracingPackage,
        setupBlock = {
            if (startupMode == StartupMode.COLD) {
                // Run setup before killing process
                setupBlock(this)

                // Shader caches are stored in the code cache directory. Make sure that
                // they are cleared every iteration. Must be done before kill, since on user builds
                // this broadcasts to the target app
                dropShaderCache()

                // Kill - code below must not wake process!
                killProcess()

                // Ensure app's pages are not cached in memory for a true _cold_ start.
                dropKernelPageCache()

                // validate process is not running just before returning
                check(!Shell.isPackageAlive(packageName)) {
                    "Package $packageName must not be running prior to cold start!"
                }
            } else {
                if (iteration == 0 && startupMode != null) {
                    try {
                        iteration = null // override to null for warmup

                        // warmup process by running the measure block once unmeasured
                        setupBlock(this)
                        measureBlock()
                    } finally {
                        iteration = 0 // resume counting
                    }
                }
                setupBlock(this)
            }
        },
        // Don't reuse activities by default in COLD / WARM
        launchWithClearTask = startupMode == StartupMode.COLD || startupMode == StartupMode.WARM,
        measureBlock = measureBlock
    )
}
