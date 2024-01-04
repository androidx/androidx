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

/**
 * This allows tests to override arguments from code
 */
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
    val perfettoSdkTracingEnable: Boolean get() =
        perfettoSdkTracingEnableOverride ?: _perfettoSdkTracingEnable

    /**
     * Allows tests to override whether full tracing is enabled
     */
    @VisibleForTesting
    var perfettoSdkTracingEnableOverride: Boolean? = null

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

    // internal properties are microbenchmark only
    internal val outputEnable: Boolean
    internal val startupMode: Boolean
    internal val iterations: Int?
    internal val profiler: Profiler?
    internal val profilerDefault: Boolean
    internal val profilerSampleFrequency: Int
    internal val profilerSampleDurationSeconds: Long
    internal val thermalThrottleSleepDurationSeconds: Long
    private val cpuEventCounterEnable: Boolean
    internal val cpuEventCounterMask: Int

    internal var error: String? = null
    internal val additionalTestOutputDir: String?

    private const val prefix = "androidx.benchmark."

    private fun Bundle.getBenchmarkArgument(key: String, defaultValue: String? = null) =
        getString(prefix + key, defaultValue)

    private fun Bundle.getProfiler(outputIsEnabled: Boolean): Pair<Profiler?, Boolean> {
        val argumentName = "profiling.mode"
        val argumentValue = getBenchmarkArgument(argumentName, "DEFAULT_VAL")
        if (argumentValue == "DEFAULT_VAL") {
            return if (Build.VERSION.SDK_INT > 21) {
                MethodTracing to true
            } else {
                // Method tracing can corrupt the stack on API 21, see b/300658578
                null to true
            }
        }

        val profiler = Profiler.getByName(argumentValue)
        if (profiler == null &&
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

        startupMode = !dryRunMode &&
            (arguments.getBenchmarkArgument("startupMode.enable")?.toBoolean() ?: false)

        outputEnable = !dryRunMode &&
            (arguments.getBenchmarkArgument("output.enable")?.toBoolean() ?: true)

        iterations =
            arguments.getBenchmarkArgument("iterations")?.toInt()

        _perfettoSdkTracingEnable =
            arguments.getBenchmarkArgument("perfettoSdkTracing.enable")?.toBoolean()
                // fullTracing.enable is the legacy/compat name
                ?: arguments.getBenchmarkArgument("fullTracing.enable")?.toBoolean()
                    ?: false

        // Transform comma-delimited list into set of suppressed errors
        // E.g. "DEBUGGABLE, UNLOCKED" -> setOf("DEBUGGABLE", "UNLOCKED")
        suppressedErrors = arguments.getBenchmarkArgument("suppressErrors", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        enabledRules = arguments.getBenchmarkArgument(
            key = "enabledRules",
            defaultValue = RuleType.values().joinToString(separator = ",") { it.toString() }
        ).run {
            if (this.lowercase() == "none") {
                emptySet()
            } else {
                // parse comma-delimited list
                try {
                    this.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { arg ->
                            RuleType.values().find { arg.lowercase() == it.toString().lowercase() }
                                ?: throw Throwable("unable to find $arg")
                        }
                        .toSet()
                } catch (e: Throwable) {
                    // defer parse error, so it doesn't show up as a missing class
                    val allRules = RuleType.values()
                    val allRulesString = allRules.joinToString(",") { it.toString() }
                    error = "unable to parse enabledRules='$this', should be 'None' or" +
                        " comma-separated list of supported ruletypes: $allRulesString"
                    allRules.toSet() // don't filter tests, so we have an opportunity to throw
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
            arguments.getBenchmarkArgument("profiling.sampleFrequency")?.ifBlank { null }
                ?.toInt()
                ?: 1000
        profilerSampleDurationSeconds =
            arguments.getBenchmarkArgument("profiling.sampleDurationSeconds")?.ifBlank { null }
                ?.toLong()
                ?: 5
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
                arguments.getBenchmarkArgument("cpuEventCounter.events", "Instructions,CpuCycles")
                    .split(",").map { eventName ->
                            CpuEventCounter.Event.valueOf(eventName)
                    }.getFlags()
            } else {
                0x0
            }
        if (cpuEventCounterEnable && cpuEventCounterMask == 0x0) {
            error = "Must set a cpu event counters mask to use counters." +
                " See CpuEventCounters.Event for flag definitions."
        }

        thermalThrottleSleepDurationSeconds =
            arguments.getBenchmarkArgument("thermalThrottle.sleepDurationSeconds")?.ifBlank { null }
                ?.toLong()
                ?: 90

        additionalTestOutputDir = arguments.getString("additionalTestOutputDir")
        Log.d(BenchmarkState.TAG, "additionalTestOutputDir=$additionalTestOutputDir")

        killProcessDelayMillis =
            arguments.getBenchmarkArgument("killProcessDelayMillis")?.toLong() ?: 0L

        enableStartupProfiles =
            arguments.getBenchmarkArgument("startupProfiles.enable")?.toBoolean() ?: true
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
}
