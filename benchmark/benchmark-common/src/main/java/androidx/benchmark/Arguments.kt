/*
 * Copyright 2019 The Android Open Source Project
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
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry

/** This allows tests to override arguments from code */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@get:RestrictTo(RestrictTo.Scope.LIBRARY)
@set:RestrictTo(RestrictTo.Scope.LIBRARY)
@VisibleForTesting
public var argumentSource: Bundle? = null

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Arguments {
    // public properties are shared by micro + macro benchmarks
    val suppressedErrors: Set<String>

    /**
     * Set to true to enable androidx.tracing.perfetto tracepoints (such as composition tracing)
     *
     * Note that when StartupMode.COLD is used, additional work must be performed during target app
     * startup to initialize tracing.
     */
    private val _perfettoSdkTracingEnable: Boolean
    val perfettoSdkTracingEnable: Boolean
        get() = perfettoSdkTracingEnableOverride ?: _perfettoSdkTracingEnable

    /** Allows tests to override whether full tracing is enabled */
    @VisibleForTesting var perfettoSdkTracingEnableOverride: Boolean? = null

    val enabledRules: Set<RuleType>

    enum class RuleType {
        Microbenchmark,
        Macrobenchmark,
        BaselineProfile
    }

    val enableCompilation: Boolean
    val killProcessDelayMillis: Long
    val enableStartupProfiles: Boolean
    val dryRunMode: Boolean
    val dropShadersEnable: Boolean
    val dropShadersThrowOnFailure: Boolean
    val skipBenchmarksOnEmulator: Boolean

    // internal properties are microbenchmark only
    internal val outputEnable: Boolean
    internal val startupMode: Boolean
    internal val iterations: Int?
    internal val profiler: Profiler?
    internal val profilerDefault: Boolean
    internal val profilerSampleFrequency: Int
    internal val profilerSampleDurationSeconds: Long
    internal val profilerSkipWhenDurationRisksAnr: Boolean
    internal val profilerPerfCompareEnable: Boolean
    internal val thermalThrottleSleepDurationSeconds: Long
    private val cpuEventCounterEnable: Boolean
    internal val cpuEventCounterMask: Int
    val runOnMainDeadlineSeconds: Long // non-internal, used in BenchmarkRule

    internal var error: String? = null
    internal val additionalTestOutputDir: String?

    private val targetPackageName: String?

    private const val prefix = "androidx.benchmark."

    private fun Bundle.getBenchmarkArgument(key: String, defaultValue: String? = null) =
        getString(prefix + key, defaultValue)

    private fun Bundle.getProfiler(outputIsEnabled: Boolean): Pair<Profiler?, Boolean> {
        val argumentName = "profiling.mode"
        val argumentValue = getBenchmarkArgument(argumentName, "DEFAULT_VAL")
        if (argumentValue == "DEFAULT_VAL") {
            return if (Build.VERSION.SDK_INT <= 21) {
                // Have observed stack corruption on API 21, we haven't spent the time to find out
                // why, or if it's better on other low API levels. See b/300658578
                // TODO: consider adding warning here
                null to true
            } else if (DeviceInfo.methodTracingAffectsMeasurements) {
                // We warn here instead of in Errors since this doesn't affect all measurements -
                // BenchmarkState throws rather than measuring incorrectly, and the first benchmark
                // can still measure with a trace safely
                InstrumentationResults.scheduleIdeWarningOnNextReport(
                    """
                    NOTE: Your device is running a version of ART where method tracing is known to
                    affect performance measurement after trace capture, so method tracing is
                    off by default.

                    To use method tracing, either flash this device, use a different device, or
                    enable method tracing with MicrobenchmarkConfig / instrumentation argument, and
                    only run one test at a time.

                    For more information, see https://issuetracker.google.com/issues/316174880
                    """
                        .trimIndent()
                )
                null to true
            } else MethodTracing to true
        }

        val profiler = Profiler.getByName(argumentValue)
        if (
            profiler == null &&
                argumentValue.isNotEmpty() &&
                // 'none' is documented as noop (and works better in gradle than
                // an empty string, if a value must be specified)
                argumentValue.trim().lowercase() != "none"
        ) {
            error = "Could not parse $prefix$argumentName=$argumentValue"
            return null to false
        }
        if (profiler?.requiresLibraryOutputDir == true && !outputIsEnabled) {
            error = "Output is not enabled, so cannot profile with mode $argumentValue"
            return null to false
        }
        return profiler to false
    }

    // note: initialization may happen at any time
    init {
        val arguments = argumentSource ?: InstrumentationRegistry.getArguments()

        dryRunMode = arguments.getBenchmarkArgument("dryRunMode.enable")?.toBoolean() ?: false

        startupMode =
            !dryRunMode &&
                (arguments.getBenchmarkArgument("startupMode.enable")?.toBoolean() ?: false)

        outputEnable =
            !dryRunMode && (arguments.getBenchmarkArgument("output.enable")?.toBoolean() ?: true)

        iterations = arguments.getBenchmarkArgument("iterations")?.toInt()

        targetPackageName = arguments.getBenchmarkArgument("targetPackageName", defaultValue = null)

        _perfettoSdkTracingEnable =
            arguments.getBenchmarkArgument("perfettoSdkTracing.enable")?.toBoolean()
                // fullTracing.enable is the legacy/compat name
                ?: arguments.getBenchmarkArgument("fullTracing.enable")?.toBoolean()
                ?: false

        // Transform comma-delimited list into set of suppressed errors
        // E.g. "DEBUGGABLE, UNLOCKED" -> setOf("DEBUGGABLE", "UNLOCKED")
        suppressedErrors =
            arguments
                .getBenchmarkArgument("suppressErrors", "")
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

        skipBenchmarksOnEmulator =
            arguments.getBenchmarkArgument("skipBenchmarksOnEmulator")?.toBoolean() ?: false

        enabledRules =
            arguments
                .getBenchmarkArgument(
                    key = "enabledRules",
                    defaultValue = RuleType.values().joinToString(separator = ",") { it.toString() }
                )
                .run {
                    if (this.lowercase() == "none") {
                        emptySet()
                    } else {
                        // parse comma-delimited list
                        try {
                            this.split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .map { arg ->
                                    RuleType.values().find {
                                        arg.lowercase() == it.toString().lowercase()
                                    } ?: throw Throwable("unable to find $arg")
                                }
                                .toSet()
                        } catch (e: Throwable) {
                            // defer parse error, so it doesn't show up as a missing class
                            val allRules = RuleType.values()
                            val allRulesString = allRules.joinToString(",") { it.toString() }
                            error =
                                "unable to parse enabledRules='$this', should be 'None' or" +
                                    " comma-separated list of supported ruletypes: $allRulesString"
                            allRules
                                .toSet() // don't filter tests, so we have an opportunity to throw
                        }
                    }
                }

        // compilation defaults to disabled if dryRunMode is on
        enableCompilation =
            arguments.getBenchmarkArgument("compilation.enabled")?.toBoolean() ?: !dryRunMode

        val profilerState = arguments.getProfiler(outputEnable)
        profiler = profilerState.first
        profilerDefault = profilerState.second
        profilerSampleFrequency =
            arguments.getBenchmarkArgument("profiling.sampleFrequency")?.ifBlank { null }?.toInt()
                ?: 1000
        profilerSampleDurationSeconds =
            arguments
                .getBenchmarkArgument("profiling.sampleDurationSeconds")
                ?.ifBlank { null }
                ?.toLong() ?: 5
        profilerSkipWhenDurationRisksAnr =
            arguments.getBenchmarkArgument("profiling.skipWhenDurationRisksAnr")?.toBoolean()
                ?: true
        profilerPerfCompareEnable =
            arguments.getBenchmarkArgument("profiling.perfCompare.enable")?.toBoolean() ?: false
        if (profiler != null) {
            Log.d(
                BenchmarkState.TAG,
                "Profiler ${profiler.javaClass.simpleName}, freq " +
                    "$profilerSampleFrequency, duration $profilerSampleDurationSeconds"
            )
        }

        cpuEventCounterEnable =
            arguments.getBenchmarkArgument("cpuEventCounter.enable")?.toBoolean() ?: false
        cpuEventCounterMask =
            if (cpuEventCounterEnable) {
                arguments
                    .getBenchmarkArgument("cpuEventCounter.events", "Instructions,CpuCycles")
                    .split(",")
                    .map { eventName -> CpuEventCounter.Event.valueOf(eventName) }
                    .getFlags()
            } else {
                0x0
            }
        if (cpuEventCounterEnable && cpuEventCounterMask == 0x0) {
            error =
                "Must set a cpu event counters mask to use counters." +
                    " See CpuEventCounters.Event for flag definitions."
        }

        thermalThrottleSleepDurationSeconds =
            arguments
                .getBenchmarkArgument("thermalThrottle.sleepDurationSeconds")
                ?.ifBlank { null }
                ?.toLong() ?: 90

        additionalTestOutputDir = arguments.getString("additionalTestOutputDir")
        Log.d(BenchmarkState.TAG, "additionalTestOutputDir=$additionalTestOutputDir")

        killProcessDelayMillis =
            arguments.getBenchmarkArgument("killProcessDelayMillis")?.toLong() ?: 0L

        enableStartupProfiles =
            arguments.getBenchmarkArgument("startupProfiles.enable")?.toBoolean() ?: true

        dropShadersEnable =
            arguments.getBenchmarkArgument("dropShaders.enable")?.toBoolean() ?: true
        dropShadersThrowOnFailure =
            arguments.getBenchmarkArgument("dropShaders.throwOnFailure")?.toBoolean() ?: true

        // very relaxed default to start, ideally this would be less than 5 (ANR timeout),
        // but configurability should help experimenting / narrowing over time
        runOnMainDeadlineSeconds =
            arguments.getBenchmarkArgument("runOnMainDeadlineSeconds")?.toLong() ?: 30
        Log.d(BenchmarkState.TAG, "runOnMainDeadlineSeconds $runOnMainDeadlineSeconds")

        if (arguments.getString("orchestratorService") != null) {
            InstrumentationResults.scheduleIdeWarningOnNextReport(
                """
                    AndroidX Benchmark does not support running with the AndroidX Test Orchestrator.

                    AndroidX benchmarks (micro and macro) produce one JSON file per test module,
                    which together with Test Orchestrator restarting the process frequently causes
                    benchmark output JSON files to be repeatedly overwritten during the test.
                    """
                    .trimIndent()
            )
        }
    }

    fun macrobenchMethodTracingEnabled(): Boolean {
        return when {
            dryRunMode -> false
            profilerDefault -> false // don't enable tracing by default in macrobench
            else -> profiler == MethodTracing
        }
    }

    fun throwIfError() {
        if (error != null) {
            throw AssertionError(error)
        }
    }

    /**
     * Retrieves the target app package name from the instrumentation runner arguments. Note that
     * this is supported only when MacrobenchmarkRule and BaselineProfileRule are used with the
     * baseline profile gradle plugin. This feature requires AGP 8.3.0-alpha10 as minimum version.
     */
    fun getTargetPackageNameOrThrow(): String =
        targetPackageName
            ?: throw IllegalArgumentException(
                """
        Can't retrieve the target package name from instrumentation arguments.
        This feature requires the baseline profile gradle plugin with minimum version 1.3.0-alpha01
        and the Android Gradle Plugin minimum version 8.3.0-alpha10.
        Please ensure your project has the correct versions in order to use this feature.
    """
                    .trimIndent()
            )
}
