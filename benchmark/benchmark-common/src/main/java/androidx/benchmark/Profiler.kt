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
import androidx.benchmark.BenchmarkState.Companion.TAG
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
    abstract fun start(traceUniqueName: String)
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
    }
}

internal fun startRuntimeMethodTracing(traceFileName: String, sampled: Boolean) {
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
}

internal fun stopRuntimeMethodTracing() {
    Debug.stopMethodTracing()
}

internal object StackSamplingLegacy : Profiler() {
    override fun start(traceUniqueName: String) {
        startRuntimeMethodTracing(
            traceFileName = "$traceUniqueName-stackSamplingLegacy.trace",
            sampled = true
        )
    }

    override fun stop() {
        stopRuntimeMethodTracing()
    }

    override val requiresExtraRuntime: Boolean = true
}

internal object MethodTracing : Profiler() {
    override fun start(traceUniqueName: String) {
        startRuntimeMethodTracing(
            traceFileName = "$traceUniqueName-methodTracing.trace",
            sampled = false
        )
    }

    override fun stop() {
        stopRuntimeMethodTracing()
    }

    override val requiresSingleMeasurementIteration: Boolean = true
}

internal object ConnectedAllocation : Profiler() {
    override fun start(traceUniqueName: String) {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
    }

    override fun stop() {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
    }

    override val requiresSingleMeasurementIteration: Boolean = true
    override val requiresDebuggable: Boolean = true
    override val requiresLibraryOutputDir: Boolean = false
}

internal object ConnectedSampling : Profiler() {
    override fun start(traceUniqueName: String) {
        Thread.sleep(CONNECTED_PROFILING_SLEEP_MS)
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

    var traceUniqueName: String? = null

    @RequiresApi(29)
    override fun start(traceUniqueName: String) {
        session?.stopRecording() // stop previous

        // for security perf harden, enable temporarily
        securityPerfHarden.forceValue()

        // for all other properties, simply set the values, as these don't have defaults
        Shell.executeCommand("setprop debug.perf_event_max_sample_rate 10000")
        Shell.executeCommand("setprop debug.perf_cpu_time_max_percent 25")
        Shell.executeCommand("setprop debug.perf_event_mlock_kb 32800")

        this.traceUniqueName = traceUniqueName
        session = ProfileSession().also {
            // prepare simpleperf must be done as shell user, so do this here with other shell setup
            // NOTE: this is sticky across reboots, so missing this will cause tests or profiling to
            // fail, but only on devices that have not run this command since flashing (e.g. in CI)
            Shell.executeCommand(it.findSimpleperf() + " api-prepare")
            it.startRecording(
                RecordOptions()
                    .setSampleFrequency(Arguments.profilerSampleFrequency)
                    .recordDwarfCallGraph() // enable Java/Kotlin callstacks
                    .traceOffCpu() // track time sleeping
                    .setOutputFilename("simpleperf.data")
                    .apply {
                        // some emulators don't support cpu-cycles, the default event, so instead we
                        // use cpu-clock, which is a software perf event using kernel hrtimer to
                        // generate interrupts
                        val hwEventsOutput = Shell.executeCommand("simpleperf list hw").trim()
                        check(hwEventsOutput.startsWith("List of hardware events:"))
                        val events = hwEventsOutput
                            .split("\n")
                            .drop(1)
                            .map { line -> line.trim() }
                        if (!events.any { hwEvent -> hwEvent.trim() == "cpu-cycles" }) {
                            Log.d(TAG, "cpu-cycles not found - using cpu-clock (events = $events)")
                            setEvent("cpu-clock")
                        }
                    }
            )
        }
    }

    @RequiresApi(29)
    override fun stop() {
        session!!.stopRecording()
        Outputs.writeFile(
            fileName = "$traceUniqueName-stackSampling.trace",
            reportKey = "simpleperf_trace"
        ) {
            session!!.convertSimpleperfOutputToProto("simpleperf.data", it.absolutePath)
        }

        session = null
        securityPerfHarden.resetIfOverridden()
    }

    override val requiresLibraryOutputDir: Boolean = false
}
