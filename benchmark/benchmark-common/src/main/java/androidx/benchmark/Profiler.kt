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

import android.annotation.SuppressLint
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.benchmark.BenchmarkState.Companion.TAG
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.json.BenchmarkData.TestResult.ProfilerOutput
import androidx.benchmark.perfetto.StackSamplingConfig
import androidx.benchmark.simpleperf.ProfileSession
import androidx.benchmark.simpleperf.RecordOptions
import androidx.benchmark.vmtrace.ArtTrace
import java.io.File
import java.io.FileOutputStream

/**
 * Profiler abstraction used for the timing stage.
 *
 * Controlled externally by `androidx.benchmark.profiling.mode` Subclasses are objects, as these
 * generally refer to device or process global state. For example, things like whether the
 * simpleperf process is running, or whether the runtime is capturing method trace.
 *
 * Note: flags on this class would be simpler if we either had a 'Default'/'Noop' profiler, or a
 * wrapper extension function (e.g. `fun Profiler? .requiresSingleMeasurementIteration`). We avoid
 * these however, in order to avoid the runtime visiting a new class in the hot path, when switching
 * from warmup -> timing phase, when [start] would be called.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class Profiler() {
    class ResultFile
    private constructor(
        val label: String,
        val type: ProfilerOutput.Type,
        val outputRelativePath: String,
        val source: Profiler?,
        val convertBeforeSync: (() -> Unit)? = null
    ) {

        fun embedInPerfettoTrace(perfettoTracePath: String) {
            source?.embedInPerfettoTrace(
                File(Outputs.outputDirectory, outputRelativePath),
                File(perfettoTracePath)
            )
        }

        companion object {
            fun ofPerfettoTrace(label: String, absolutePath: String) =
                ResultFile(
                    label = label,
                    outputRelativePath = Outputs.relativePathFor(absolutePath),
                    type = ProfilerOutput.Type.PerfettoTrace,
                    source = null
                )

            fun ofMethodTrace(label: String, absolutePath: String) =
                ResultFile(
                    label = label,
                    outputRelativePath = Outputs.relativePathFor(absolutePath),
                    type = ProfilerOutput.Type.MethodTrace,
                    source = null
                )

            fun of(
                label: String,
                type: ProfilerOutput.Type,
                outputRelativePath: String,
                source: Profiler,
                convertBeforeSync: (() -> Unit)? = null
            ) =
                ResultFile(
                    label = label,
                    outputRelativePath = outputRelativePath,
                    type = type,
                    source = source,
                    convertBeforeSync = convertBeforeSync
                )
        }
    }

    abstract fun start(traceUniqueName: String): ResultFile?

    abstract fun stop()

    internal open fun config(packageNames: List<String>): StackSamplingConfig? = null

    open fun embedInPerfettoTrace(profilerTrace: File, perfettoTrace: File) {}

    /**
     * Measure exactly one loop (one repeat, one iteration).
     *
     * Generally only set for tracing profilers.
     */
    open val requiresSingleMeasurementIteration = false

    /** Generally only set for sampling profilers. */
    open val requiresExtraRuntime = false

    /**
     * Currently, debuggable is required to support studio-connected profiling.
     *
     * Remove this once stable Studio supports profileable.
     */
    open val requiresDebuggable = false

    /** Connected modes don't need dir, since library isn't doing the capture. */
    open val requiresLibraryOutputDir = true

    companion object {
        const val CONNECTED_PROFILING_SLEEP_MS = 20_000L

        fun getByName(name: String): Profiler? =
            mapOf(
                    "MethodTracing" to MethodTracing,
                    "StackSampling" to
                        if (Build.VERSION.SDK_INT >= 29) {
                            StackSamplingSimpleperf // only supported on 29+ without
                            // root/debug/sideload
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
            return Outputs.sanitizeFilename(
                "$traceUniqueName-$traceTypeLabel-${dateToFileName()}.trace"
            )
        }
    }
}

internal fun startRuntimeMethodTracing(
    traceFileName: String,
    sampled: Boolean,
    profiler: Profiler,
): Profiler.ResultFile {
    val path = Outputs.testOutputFile(traceFileName).absolutePath

    Log.d(TAG, "Profiling output file: $path")
    InstrumentationResults.reportAdditionalFileToCopy("profiling_trace", path)

    val bufferSize = 16 * 1024 * 1024
    if (sampled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startMethodTracingSampling(path, bufferSize, Arguments.profilerSampleFrequency)
    } else {
        // NOTE: 0x10 flag enables low-overhead wall clock timing when ART module version supports
        // it. Note that this doesn't affect trace parsing, since this doesn't affect wall clock,
        // it only removes the expensive thread time clock which our parser doesn't use.
        // TODO: switch to platform-defined constant once available (b/329499422)
        Debug.startMethodTracing(path, bufferSize, 0x10)
    }

    return if (sampled) {
        Profiler.ResultFile.of(
            outputRelativePath = traceFileName,
            label = "Stack Sampling (legacy) Trace",
            type = ProfilerOutput.Type.StackSamplingTrace,
            source = profiler
        )
    } else {
        Profiler.ResultFile.of(
            outputRelativePath = traceFileName,
            label = "Method Trace",
            type = ProfilerOutput.Type.MethodTrace,
            source = profiler
        )
    }
}

internal fun stopRuntimeMethodTracing() {
    Debug.stopMethodTracing()
}

internal object StackSamplingLegacy : Profiler() {
    @get:VisibleForTesting var isRunning = false

    override fun start(traceUniqueName: String): ResultFile {
        isRunning = true
        return startRuntimeMethodTracing(
            traceFileName = traceName(traceUniqueName, "stackSamplingLegacy"),
            sampled = true,
            profiler = this
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
        hasBeenUsed = true
        return startRuntimeMethodTracing(
            traceFileName = traceName(traceUniqueName, "methodTracing"),
            sampled = false,
            profiler = this
        )
    }

    override fun stop() {
        stopRuntimeMethodTracing()
    }

    override val requiresSingleMeasurementIteration: Boolean = true

    override fun embedInPerfettoTrace(profilerTrace: File, perfettoTrace: File) {
        ArtTrace(profilerTrace)
            .writeAsPerfettoTrace(FileOutputStream(perfettoTrace, /* append= */ true))
    }

    var hasBeenUsed: Boolean = false
        private set
}

@SuppressLint("BanThreadSleep") // needed for connected profiling
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

@SuppressLint("BanThreadSleep") // needed for connected profiling
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

    @RequiresApi(29) private var session: ProfileSession? = null

    /** "security.perf_harden" must be set to "0" during simpleperf capture */
    @RequiresApi(29) private val securityPerfHarden = PropOverride("security.perf_harden", "0")

    private var outputRelativePath: String? = null

    @RequiresApi(29)
    override fun start(traceUniqueName: String): ResultFile {
        session?.stopRecording() // stop previous

        // for security perf harden, enable temporarily
        securityPerfHarden.forceValue()

        // for all other properties, simply set the values, as these don't have defaults
        Shell.executeScriptSilent("setprop debug.perf_event_max_sample_rate 10000")
        Shell.executeScriptSilent("setprop debug.perf_cpu_time_max_percent 25")
        Shell.executeScriptSilent("setprop debug.perf_event_mlock_kb 32800")

        outputRelativePath = traceName(traceUniqueName, "stackSampling")
        session =
            ProfileSession().also {
                // prepare simpleperf must be done as shell user, so do this here with other shell
                // setup
                // NOTE: this is sticky across reboots, so missing this will cause tests or
                // profiling to
                // fail, but only on devices that have not run this command since flashing (e.g. in
                // CI)
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
        return ResultFile.of(
            label = "Stack Sampling Trace",
            outputRelativePath = outputRelativePath!!,
            type = ProfilerOutput.Type.StackSamplingTrace,
            source = this,
            convertBeforeSync = this::convertBeforeSync
        )
    }

    @RequiresApi(29)
    override fun stop() {
        session!!.stopRecording()
        securityPerfHarden.resetIfOverridden()
    }

    @RequiresApi(29)
    fun convertBeforeSync() {
        Outputs.writeFile(fileName = outputRelativePath!!) {
            session!!.convertSimpleperfOutputToProto("simpleperf.data", it.absolutePath)
            session = null
        }
    }

    override fun config(packageNames: List<String>) =
        StackSamplingConfig(
            packageNames = packageNames,
            frequency = Arguments.profilerSampleFrequency.toLong(),
            duration = Arguments.profilerSampleDurationSeconds,
        )

    override val requiresLibraryOutputDir: Boolean = false

    override val requiresExtraRuntime: Boolean = true
}
