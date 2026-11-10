/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.SystemClock
import android.util.Log
import androidx.benchmark.Arguments
import androidx.benchmark.Outputs
import androidx.benchmark.PropOverride
import androidx.benchmark.Shell
import androidx.benchmark.StartedShellScript
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.runServer
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import java.io.File

internal fun MacrobenchmarkScope.buildNativeLoadOrder(
    maxLoadOrderIterations: Int,
    profileBlock: MacrobenchmarkScope.() -> Unit,
    restrictToDataDirectory: Boolean = true,
): File? {
    if (Build.VERSION.SDK_INT < 31) return null
    val mode =
        CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 1,
        )
    val results = mutableListOf<File>()
    val lastIteration = this.iteration ?: 0
    var iteration = 1
    while (iteration <= maxLoadOrderIterations) {
        this.killProcess()
        this.dropKernelPageCache()
        // Use the benchmark package name
        val profiler = NativeFileLoadOrder(this.packageName, iteration)
        mode.resetAndCompile(this, allowCompilationSkipping = false) {
            this.iteration = lastIteration + iteration
            try {
                Log.d(
                    TAG,
                    "Starting profiler to capture native file load order (iteration $iteration)",
                )
                profiler.start()
                this.profileBlock()
            } finally {
                Log.d(
                    TAG,
                    "Stopping profiler capture for native file load order (iteration $iteration)",
                )
                val resultPath = profiler.stop()
                if (resultPath != null) {
                    results += resultPath
                }
            }
        }
        iteration += 1
    }
    if (results.isEmpty()) return null
    Log.d(TAG, "Extracting load order from $results")
    return extractNativeLoadOrder(packageName, results, restrictToDataDirectory)
}

internal fun extractNativeLoadOrder(
    packageName: String,
    results: List<File>,
    restrictToDataDirectory: Boolean = true,
): File {
    val entryMap = mutableMapOf<String, Sequence<LoadOrderEntry>>()
    TraceProcessor.runServer {
        results.forEach { result ->
            loadTrace(PerfettoTrace(result.absolutePath)) {
                entryMap[result.name] =
                    NativeLoadOrderQuery.nativeLoadOrderEntries(
                        session = this,
                        targetPackageName = packageName,
                        restrictToDataDirectory = restrictToDataDirectory,
                    )
            }
        }
    }
    val output = "nativeLoadOrder-${Outputs.dateToFileName()}.txt"
    val path =
        Outputs.writeFile(output) { outputFile ->
            outputFile.bufferedWriter().use { writer ->
                // Unicode record separator
                val separator = "\u001E"
                var iteration = 0
                entryMap.forEach { (_, entries) ->
                    entries.forEach { entry ->
                        val file = entry.file.substringAfterLast("/").substringAfterLast("!")
                        // Format is: iteration | so_name | build_id | rel_pc | so_symbol_name
                        // The name tells you the iteration
                        writer
                            .append(iteration.toString())
                            .append(separator)
                            .append(file)
                            .append(separator)
                            .append(entry.buildId)
                            .append(separator)
                            .append(entry.relPc.toString())
                            .append(separator)
                            .append(entry.name)
                            .append(separator)
                            .append("\n")
                    }
                    iteration += 1
                }
            }
        }
    return File(path)
}

internal class NativeFileLoadOrder(
    private val packageName: String,
    iteration: Int,
) {
    private val output = "nativeLoadOrder-${Outputs.dateToFileName()}-$iteration.data"
    private val outputPath = "/data/local/tmp/$output"
    private var startedShellScript: StartedShellScript? = null
    private var pid: Int? = null

    fun start() {
        // For security perf harden, enable temporarily
        securityPerfHarden.forceValue()

        // For all other properties, simply set the values, as these don't have defaults
        Shell.executeScriptSilent("setprop debug.perf_event_max_sample_rate 10000")
        Shell.executeScriptSilent("setprop debug.perf_cpu_time_max_percent 25")
        Shell.executeScriptSilent("setprop debug.perf_event_mlock_kb 32800")

        val script =
            """
            echo pid:$$
            exec simpleperf record \
              --app $packageName \
              -e page-faults \
              -c ${Arguments.loadOrderSampleFreq} \
              -g \
              -o $outputPath >/dev/null
            """
                .trimIndent()
        val startedScript = Shell.createShellScript(script).start()
        startedShellScript = startedScript
        val processId =
            startedScript
                .stdOutLineSequence()
                .first { it.startsWith("pid:") }
                .substringAfter("pid:")
                .trim()
                .toInt()
        pid = processId
        waitForSimpleperfStart(processId)
        Log.d(TAG, "Started a native load order profiler at pid: $processId")
    }

    private fun waitForSimpleperfStart(processId: Int) {
        repeat(POLL_START_MAX_COUNT) {
            if (Shell.isProcessAlive(processId, PROCESS_NAME)) {
                return
            }
            inMemoryTrace("wait for $PROCESS_NAME to start") {
                SystemClock.sleep(POLL_START_PERIOD_MS)
            }
        }
        Log.w(TAG, "$PROCESS_NAME (pid=$processId) was not detected as alive after startup")
    }

    fun stop(): File? {
        try {
            val processId = pid ?: return null
            // Kill `simpleperf` and wait for it to flush the output file and exit.
            Shell.killProcessesAndWait(
                listOf(Shell.ProcessPid(processName = PROCESS_NAME, pid = processId))
            )
            if (!Shell.pathExists(outputPath)) {
                Log.w(TAG, "Native load order output file $outputPath was not created")
                return null
            }
            val path =
                Outputs.writeFile(output) { file ->
                    Shell.cp(from = outputPath, to = file.absolutePath)
                }
            Log.d(TAG, "Copied native load order file to $path")
            return File(path)
        } finally {
            // Cleanup
            Shell.rm(outputPath)
            startedShellScript?.close()
            startedShellScript = null
            pid = null
            securityPerfHarden.resetIfOverridden()
        }
    }

    companion object {
        private const val PROCESS_NAME = "simpleperf"
        private const val POLL_START_PERIOD_MS = 50L
        private const val POLL_START_MAX_COUNT = 20

        /** "security.perf_harden" must be set to "0" during simpleperf capture */
        private val securityPerfHarden = PropOverride("security.perf_harden", "0")
    }
}
