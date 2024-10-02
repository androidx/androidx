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
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.benchmark.Arguments
import androidx.benchmark.ConfigurationError
import androidx.benchmark.DeviceInfo
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Profiler
import androidx.benchmark.ResultWriter
import androidx.benchmark.Shell
import androidx.benchmark.checkAndGetSuppressionState
import androidx.benchmark.conditionalError
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.json.BenchmarkData
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeFalse
import perfetto.protos.AndroidStartupMetric

/** Get package ApplicationInfo, throw if not found. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("DEPRECATION")
fun getInstalledPackageInfo(packageName: String): ApplicationInfo {
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

/** @return `true` if the [ApplicationInfo] instance is referring to a system app. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ApplicationInfo.isSystemApp(): Boolean {
    return flags and (FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP) > 0
}

internal fun checkErrors(packageName: String): ConfigurationError.SuppressionState? {
    Arguments.throwIfError()

    val applicationInfo = getInstalledPackageInfo(packageName)

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val errors =
        DeviceInfo.errors +
            // TODO: Merge this debuggable check / definition with Errors.kt in benchmark-common
            listOfNotNull(
                    conditionalError(
                        hasError = applicationInfo.flags.and(FLAG_DEBUGGABLE) != 0,
                        id = "DEBUGGABLE",
                        summary = "Benchmark Target is Debuggable",
                        message =
                            """
                    Target package $packageName is running with debuggable=true in its manifest,
                    which drastically reduces runtime performance in order to support debugging
                    features. Run benchmarks with debuggable=false. Debuggable affects execution
                    speed in ways that mean benchmark improvements might not carry over to a
                    real user's experience (or even regress release performance).
                """
                                .trimIndent()
                    ),
                    conditionalError(
                        // Profileable is currently only needed on API 29+30, since app trace tag no
                        // longer
                        // requires profileable on API 31, and macrobench doesn't currently offer
                        // other
                        // means of profiling (like simpleperf) that need the flag.
                        hasError =
                            DeviceInfo.profileableEnforced &&
                                Build.VERSION.SDK_INT in 29..30 &&
                                applicationInfo.isNotProfileableByShell(),
                        id = "NOT-PROFILEABLE",
                        summary = "Benchmark Target is NOT profileable",
                        message =
                            """
                    Target package $packageName is running without <profileable shell=true>.
                    Profileable is required on Android 10 & 11 to enable macrobenchmark to capture
                    detailed trace information from the target process, such as System tracing
                    sections defined in the app, or libraries.

                    To make the target profileable, add the following in your target app's
                    main AndroidManifest.xml, within the application tag:

                    <!--suppress AndroidElementNotAllowed -->
                    <profileable android:shell="true"/>
                """
                                .trimIndent()
                    ),
                    conditionalError(
                        hasError =
                            instrumentation.targetContext.packageName !=
                                instrumentation.context.packageName,
                        id = "NOT-SELF-INSTRUMENTING",
                        summary = "Benchmark manifest is instrumenting separate process",
                        message =
                            """
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
                """
                                .trimIndent()
                    ),
                    conditionalError(
                        hasError = DeviceInfo.misconfiguredForTracing,
                        id = "DEVICE-TRACING-MISCONFIGURED",
                        summary = "This ${DeviceInfo.typeLabel}'s OS is misconfigured for tracing",
                        message =
                            """
                    This ${DeviceInfo.typeLabel}'s OS image has not correctly mounted the tracing
                    file system, which prevents macrobenchmarking, and Perfetto/atrace trace capture
                    in general. You can try a different device, or experiment with an emulator
                    (though that will not give timing measurements representative of real device
                    experience).
                    This error may not be suppressed.
                """
                                .trimIndent()
                    ),
                    conditionalError(
                        hasError = Arguments.macrobenchMethodTracingEnabled(),
                        id = "METHOD-TRACING-ENABLED",
                        summary = "Method tracing is enabled during a Macrobenchmark",
                        message =
                            """
                    The Macrobenchmark run for $packageName has method tracing enabled.
                    This causes the VM to run more slowly than usual, so the metrics from the
                    trace files should only be considered in relative terms
                    (e.g. was run #1 faster than run #2). Also, these metrics cannot be compared
                    with benchmark runs that don't have method tracing enabled.
                """
                                .trimIndent()
                    ),
                )
                .sortedBy { it.id }

    // These error ids are really warnings. In that, we don't need developers to have to
    // explicitly suppress them using test instrumentation arguments.
    // TODO: Introduce a better way to surface warnings.
    val alwaysSuppressed = setOf("METHOD-TRACING-ENABLED")
    val neverSuppressed = setOf("DEVICE-TRACING-MISCONFIGURED")

    return errors.checkAndGetSuppressionState(
        Arguments.suppressedErrors + alwaysSuppressed - neverSuppressed
    )
}

/**
 * macrobenchmark test entrypoint, which doesn't depend on JUnit.
 *
 * This function is a building block for public testing APIs
 */
@ExperimentalBenchmarkConfigApi
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
    experimentalConfig: ExperimentalConfig?,
    perfettoSdkConfig: PerfettoSdkConfig?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
): BenchmarkData.TestResult {
    require(iterations > 0) { "Require iterations > 0 (iterations = $iterations)" }
    require(metrics.isNotEmpty()) {
        "Empty list of metrics passed to metrics param, must pass at least one Metric"
    }

    // When running on emulator and argument `skipOnEmulator` is passed, the test is skipped.
    if (Arguments.skipBenchmarksOnEmulator) {
        assumeFalse(
            "Skipping test because it's running on emulator and `skipOnEmulator` is enabled",
            DeviceInfo.isEmulator
        )
    }

    val suppressionState = checkErrors(packageName)
    var warningMessage = suppressionState?.warningMessage ?: ""
    // skip benchmark if not supported by vm settings
    compilationMode.assumeSupportedWithVmSettings()

    val startTime = System.nanoTime()
    // Ensure method tracing is explicitly enabled and that we are not running in dry run mode.
    val requestMethodTracing = Arguments.macrobenchMethodTracingEnabled()
    val applicationInfo = getInstalledPackageInfo(packageName)
    val scope = MacrobenchmarkScope(packageName, launchWithClearTask = launchWithClearTask)
    // Capture if the app being benchmarked is a system app.
    scope.isSystemApp = applicationInfo.isSystemApp()

    // Ensure the device is awake
    scope.device.wakeUp()

    // Stop Background Dexopt during a Macrobenchmark to improve stability.
    if (Build.VERSION.SDK_INT >= 33) {
        scope.cancelBackgroundDexopt()
    }

    // Always kill the process at beginning of test
    scope.killProcess()

    inMemoryTrace("compile $packageName") {
        compilationMode.resetAndCompile(scope) {
            setupBlock(scope)
            measureBlock(scope)
        }
    }

    // package name for macrobench process, so it's captured as well
    val macrobenchPackageName = InstrumentationRegistry.getInstrumentation().context.packageName
    val outputs = mutableListOf<PhaseResult>()

    PerfettoTraceProcessor.runServer {
        // Measurement Phase
        outputs +=
            runPhase(
                uniqueName = uniqueName,
                packageName = packageName,
                macrobenchmarkPackageName = macrobenchPackageName,
                iterations = if (Arguments.dryRunMode) 1 else iterations,
                startupMode = startupModeMetricHint,
                scope = scope,
                profiler = null, // Don't profile when measuring
                metrics = metrics,
                experimentalConfig = experimentalConfig,
                perfettoSdkConfig = perfettoSdkConfig,
                setupBlock = setupBlock,
                measureBlock = measureBlock
            )
        // Profiling Phase
        if (requestMethodTracing) {
            outputs +=
                runPhase(
                    uniqueName = uniqueName,
                    packageName = packageName,
                    macrobenchmarkPackageName = macrobenchPackageName,
                    // We should open up an API to control the number of iterations here.
                    // Run profiling for 1 additional iteration.
                    iterations = 1,
                    startupMode = startupModeMetricHint,
                    scope = scope,
                    profiler = MethodTracingProfiler(scope),
                    metrics = emptyList(), // Nothing to measure
                    experimentalConfig = experimentalConfig,
                    perfettoSdkConfig = perfettoSdkConfig,
                    setupBlock = setupBlock,
                    measureBlock = measureBlock
                )
        }
    }

    val tracePaths = mutableListOf<String>()
    val profilerResults = mutableListOf<Profiler.ResultFile>()
    val measurementsList = mutableListOf<List<Metric.Measurement>>()
    val insightsList = mutableListOf<List<AndroidStartupMetric.SlowStartReason>>()

    outputs.forEach {
        tracePaths += it.tracePaths
        profilerResults += it.profilerResults
        measurementsList += it.measurements
        insightsList += it.insights
    }

    // Merge measurements
    val measurements = measurementsList.mergeMultiIterResults()
    require(measurements.isNotEmpty()) {
        """
            Unable to read any metrics during benchmark (metric list: $metrics).
            Check that you're performing the operations to be measured. For example, if
            using StartupTimingMetric, are you starting an activity for the specified package
            in the measure block?
        """
            .trimIndent()
    }

    InstrumentationResults.instrumentationReport {
        reportSummaryToIde(
            warningMessage = warningMessage,
            testName = uniqueName,
            measurements = measurements,
            insights =
                createInsightsIdeSummary(
                    insightsList,
                    experimentalConfig?.startupInsightsConfig,
                    tracePaths
                ),
            iterationTracePaths = tracePaths,
            profilerResults = profilerResults,
            useTreeDisplayFormat = experimentalConfig?.startupInsightsConfig?.isEnabled == true
        )

        warningMessage = "" // warning only printed once
        measurements.singleMetrics.forEach {
            it.putInBundle(bundle, suppressionState?.prefix ?: "")
        }
        measurements.sampledMetrics.forEach {
            it.putPercentilesInBundle(bundle, suppressionState?.prefix ?: "")
        }
    }

    val warmupIterations =
        when (compilationMode) {
            is CompilationMode.Partial -> compilationMode.warmupIterations
            else -> 0
        }

    val mergedProfilerOutputs =
        (tracePaths.mapIndexed { index, it ->
                Profiler.ResultFile.ofPerfettoTrace(
                    label = "Trace Iteration $index",
                    absolutePath = it
                )
            } + profilerResults)
            .map { BenchmarkData.TestResult.ProfilerOutput(it) }

    val testResult =
        BenchmarkData.TestResult(
            className = className,
            name = testName,
            totalRunTimeNs = System.nanoTime() - startTime,
            metrics = measurements.singleMetrics + measurements.sampledMetrics,
            repeatIterations = iterations,
            thermalThrottleSleepSeconds = 0,
            warmupIterations = warmupIterations,
            profilerOutputs = mergedProfilerOutputs
        )
    ResultWriter.appendTestResult(testResult)
    return testResult
}

/** Run a macrobenchmark with the specified StartupMode */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalBenchmarkConfigApi
fun macrobenchmarkWithStartupMode(
    uniqueName: String,
    className: String,
    testName: String,
    packageName: String,
    metrics: List<Metric>,
    compilationMode: CompilationMode,
    iterations: Int,
    experimentalConfig: ExperimentalConfig?,
    startupMode: StartupMode?,
    setupBlock: MacrobenchmarkScope.() -> Unit,
    measureBlock: MacrobenchmarkScope.() -> Unit
): BenchmarkData.TestResult {
    val perfettoSdkConfig =
        if (Arguments.perfettoSdkTracingEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PerfettoSdkConfig(
                packageName,
                when (startupMode) {
                    null -> InitialProcessState.Unknown
                    StartupMode.COLD -> InitialProcessState.NotAlive
                    StartupMode.HOT,
                    StartupMode.WARM -> InitialProcessState.Alive
                }
            )
        } else null
    return macrobenchmark(
        uniqueName = uniqueName,
        className = className,
        testName = testName,
        packageName = packageName,
        metrics = metrics,
        compilationMode = compilationMode,
        iterations = iterations,
        startupModeMetricHint = startupMode,
        experimentalConfig = experimentalConfig,
        perfettoSdkConfig = perfettoSdkConfig,
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
