/*
 * Copyright 2020 The Android Open Source Project
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
import android.os.Debug
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.benchmark.BenchmarkState.Companion.TAG
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.simpleperf.ProfileSession
import androidx.benchmark.simpleperf.RecordOptions

/**
 * Profiler abstraction used for the timing stage.
 *
 * Controlled externally by `androidx.benchmark.profiling.mode`
 * Subclasses are objects, as these generally refer to device or process global state. For
 * example, things like whether the simpleperf process is running, or whether the runtime is
 * capturing method trace.
 *
 * Note: flags on this class would be simpler if we either had a 'Default'/'Noop' profiler, or a
 * wrapper extension function (e.g. `fun Profiler? .requiresSingleMeasurementIteration`). We
 * avoid these however, in order to avoid the runtime visiting a new class in the hot path, when
 * switching from warmup -> timing phase, when [start] would be called.
 */
internal sealed class Profiler {
    class ResultFile(
        val label: String,
        val outputRelativePath: String
    )

    abstract fun start(traceUniqueName: String): ResultFile?
    abstract fun stop()

    /**
     * Measure exactly one loop (one repeat, one iteration).
     *
     * Generally only set for tracing profilers.
     */
    open val requiresSingleMeasurementIteration = false

    /**
     * Generally only set for sampling profilers.
     */
    open val requiresExtraRuntime = false

    /**
     * Currently, debuggable is required to support studio-connected profiling.
     *
     * Remove this once stable Studio supports profileable.
     */
    open val requiresDebuggable = false

    /**
     * Connected modes don't need dir, since library isn't doing the capture.
     */
    open val requiresLibraryOutputDir = true

    companion object {
        const val CONNECTED_PROFILING_SLEEP_MS = 20_000L

        fun getByName(name: String): Profiler? = mapOf(
            "MethodTracing" to MethodTracing,

            "StackSampling" to if (Build.VERSION.SDK_INT >= 29) {
                StackSamplingSimpleperf // only supported on 29+ without root/debug/sideload
            } else {
                StackSamplingLegacy
            },

            "ConnectedAllocation" to ConnectedAllocation,
            "ConnectedSampling" to ConnectedSampling,

            // Below are compat codepaths for old names. Remove before 1.1 stable.

            "MethodSampling" to StackSamplingLegacy,
            "MethodSamplingSimpleperf" to StackSamplingSimpleperf,
            "Method" to MethodTracing,
            "Sampled" to StackSamplingLegacy,
            "ConnectedSampled" to ConnectedSampling
        )
            .mapKeys { it.key.lowercase() }[name.lowercase()]

        fun traceName(traceUniqueName: String, traceTypeLabel: String): String {
            return "$traceUniqueName-$traceTypeLabel-${dateToFileName()}.trace"
        }
    }
}

internal fun startRuntimeMethodTracing(
    traceFileName: String,
    sampled: Boolean
): Profiler.ResultFile {
    val path = Outputs.testOutputFile(traceFileName).absolutePath

    Log.d(TAG, "Profiling output file: $path")
    InstrumentationResults.reportAdditionalFileToCopy("profiling_trace", path)

    val bufferSize = 16 * 1024 * 1024
    if (sampled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    ) {
        startMethodTracingSampling(path, bufferSize, Arguments.profilerSampleFrequency)
    } else {
        Debug.startMethodTracing(path, bufferSize, 0)
    }

    return Profiler.ResultFile(
        outputRelativePath = traceFileName,
        label = if (sampled) "Stack Sampling (legacy) Trace" else "Method Trace"
    )
}

internal fun stopRuntimeMethodTracing() {
    Debug.stopMethodTracing()
}

internal object StackSamplingLegacy : Profiler() {
    @get:VisibleForTesting
    var isRunning = false

    override fun start(traceUniqueName: String): ResultFile {
        isRunning = true
        return startRuntimeMethodTracing(
            traceFileName = traceName(traceUniqueName, "stackSamplingLegacy"),
            sampled = true
        )
    }

    override fun stop() {
        stopRuntimeMethodTracing()
        isRunning = false
    }

    override val requiresExtraRuntime: Boolean = true
}

internal object MethodTracing : Profiler() {
    override fun start(traceUniqueName: String): ResultFile {
        return startRuntimeMethodTracing(
            traceFileName = traceName(traceUniqueName, "methodTracing"),
            sampled = false
        )
    }

    override fun stop() {
        stopRuntimeMethodTracing()
    }

    override val requiresSingleMeasurementIteration: Boolean = true
}

internal object ConnectedAllocation : Profiler() {
    override fun start(traceUniqueName: String): ResultFile? {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
        return null
    }

    override fun stop() {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
    }

    override val requiresSingleMeasurementIteration: Boolean = true
    override val requiresDebuggable: Boolean = true
    override val requiresLibraryOutputDir: Boolean = false
}

internal object ConnectedSampling : Profiler() {
    override fun start(traceUniqueName: String): ResultFile? {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
        return null
    }

    override fun stop() {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
    }

    override val requiresDebuggable: Boolean = true
    override val requiresLibraryOutputDir: Boolean = false
}

/**
 * Simpleperf profiler.
 *
 * API 29+ currently, since it relies on the platform system image simpleperf.
 *
 * Could potentially lower, but that would require root or debuggable.
 */
internal object StackSamplingSimpleperf : Profiler() {
    @RequiresApi(29)
    private var session: ProfileSession? = null

    /** "security.perf_harden" must be set to "0" during simpleperf capture */
    @RequiresApi(29)
    private val securityPerfHarden = PropOverride("security.perf_harden", "0")

    var outputRelativePath: String? = null

    @RequiresApi(29)
    override fun start(traceUniqueName: String): ResultFile? {
        session?.stopRecording() // stop previous

        // for security perf harden, enable temporarily
        securityPerfHarden.forceValue()

        // for all other properties, simply set the values, as these don't have defaults
        Shell.executeScriptSilent("setprop debug.perf_event_max_sample_rate 10000")
        Shell.executeScriptSilent("setprop debug.perf_cpu_time_max_percent 25")
        Shell.executeScriptSilent("setprop debug.perf_event_mlock_kb 32800")

        outputRelativePath = traceName(traceUniqueName, "stackSampling")
        session = ProfileSession().also {
            // prepare simpleperf must be done as shell user, so do this here with other shell setup
            // NOTE: this is sticky across reboots, so missing this will cause tests or profiling to
            // fail, but only on devices that have not run this command since flashing (e.g. in CI)
            Shell.executeScriptSilent(it.findSimpleperf() + " api-prepare")
            it.startRecording(
                RecordOptions()
                    .setSampleFrequency(Arguments.profilerSampleFrequency)
                    .recordDwarfCallGraph() // enable Java/Kotlin callstacks
                    .setEvent("cpu-clock") // Required on API 33 to enable traceOffCpu
                    .traceOffCpu() // track time sleeping
                    .setSampleCurrentThread() // sample stacks from this thread only
                    .setOutputFilename("simpleperf.data")
            )
        }
        return ResultFile(
            label = "Stack Sampling Trace",
            outputRelativePath = outputRelativePath!!
        )
    }

    @RequiresApi(29)
    override fun stop() {
        session!!.stopRecording()
        Outputs.writeFile(
            fileName = outputRelativePath!!,
            reportKey = "simpleperf_trace"
        ) {
            session!!.convertSimpleperfOutputToProto("simpleperf.data", it.absolutePath)
        }

        session = null
        securityPerfHarden.resetIfOverridden()
    }

    override val requiresLibraryOutputDir: Boolean = false
}
