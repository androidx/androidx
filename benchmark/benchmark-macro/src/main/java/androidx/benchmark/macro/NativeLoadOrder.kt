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
import android.system.Os
import android.system.OsConstants._SC_NPROCESSORS_CONF
import android.system.OsConstants._SC_PAGESIZE
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
                // Drop caches after the resetAndCompile() broadcast.
                this.dropKernelPageCache()
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
        // `simpleperf record -m <pages>` allocates per-CPU kernel ring buffers pinned in RAM.
        // Because the kernel caps how much RAM `perf_event_open` can pin (~516 KB per CPU by
        // default), raise `debug.perf_event_mlock_kb` and disable `security.perf_harden` first so
        // `init` updates the kernel limit before `simpleperf` starts. Both `debug.perf_*` and
        // `security.perf_harden` are writable by ADB `shell` on unrooted `user` builds.
        perfEventMaxSampleRate.forceValue()
        perfCpuTimeMaxPercent.forceValue()
        perfEventMLockKb.forceValue()
        securityPerfHarden.forceValue()

        // Watch all supported events (`iTLB-load-misses`,`L1-icache-load-misses`, `instructions`)
        // and pick call-graph unwinding (`fp` vs `dwarf`).
        val supportedEvents = supportedHwCacheEvents()
        val eventArgs =
            buildEventArgs(
                period = Arguments.loadOrderSampleFreq,
                supportedEvents = supportedEvents,
            )
        val callGraphArg = callGraph(packageName)

        // `-m` sets the per-CPU kernel ring-buffer size (in pages) so startup bursts aren't lost.
        // NOTE: We don't need to explicitly set `--user-buffer-size` given it already defaults to
        // 64M on devices with low memory, and 256M on more capable devices.
        val script =
            """
            echo pid:$$
            exec simpleperf record \
              --log-to-android-buffer \
              --app $packageName \
              $eventArgs \
              $callGraphArg \
              --cpu-percent $CPU_TIME_MAX_PERCENT \
              --no-dump-symbols \
              --no-dump-kernel-symbols \
              -m $MMAP_PAGES \
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
        readAheadOverride.forceValue(pageSize = pageSize)
        Log.d(
            TAG,
            "Started a native load order profiler at pid: $processId (events: $eventArgs, callGraph: $callGraphArg)",
        )
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
            // Restore read-ahead and fault-around before stopping `simpleperf` so its DWARF
            // post-processing, build-id scanning, and output flush run at normal I/O speed.
            readAheadOverride.resetIfOverridden()
            // Kill `simpleperf` and wait for it to post-process samples and flush the output file.
            Shell.killProcessesAndWait(
                processes = listOf(Shell.ProcessPid(processName = PROCESS_NAME, pid = processId)),
                waitPollPeriodMs = STOP_POLL_PERIOD_MS,
                waitPollMaxCount = STOP_POLL_MAX_COUNT,
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
            readAheadOverride.resetIfOverridden()
            Shell.rm(outputPath)
            startedShellScript?.close()
            startedShellScript = null
            pid = null
            securityPerfHarden.resetIfOverridden()
            perfEventMLockKb.resetIfOverridden()
            perfCpuTimeMaxPercent.resetIfOverridden()
            perfEventMaxSampleRate.resetIfOverridden()
        }
    }

    companion object {
        private const val PROCESS_NAME = "simpleperf"
        private const val POLL_START_PERIOD_MS = 50L
        private const val POLL_START_MAX_COUNT = 20
        private const val STOP_POLL_PERIOD_MS = 250L
        private const val STOP_POLL_MAX_COUNT = 150
        private const val DEFAULT_PAGE_SIZE = 4096L
        /* Per-CPU perf_event MMAP ring-buffer size in pages (power of 2) to prevent drops. */
        private const val MMAP_PAGES = 2048L
        private const val MAX_EVENT_CONTEXTS = 4L
        private const val MAX_SAMPLE_RATE = 100_000
        private const val CPU_TIME_MAX_PERCENT = 75
        private const val MIN_PERF_EVENT_MLOCK_KB = 131_200L
        private const val INTRA_SAMPLE_FREQ_HZ = 5_000
        private const val DWARF_STACK_BYTES = 1024

        private val cpuCount = Os.sysconf(_SC_NPROCESSORS_CONF).coerceAtLeast(1L)
        private val pageSize = Os.sysconf(_SC_PAGESIZE).coerceAtLeast(DEFAULT_PAGE_SIZE)
        private val pageSizeKb = (pageSize / 1024L).coerceAtLeast(4L)
        private val mLockKb =
            maxOf(
                MIN_PERF_EVENT_MLOCK_KB,
                cpuCount * MAX_EVENT_CONTEXTS * (MMAP_PAGES + 16L) * pageSizeKb,
            )

        private val perfEventMaxSampleRate =
            PropOverride(
                propName = "debug.perf_event_max_sample_rate",
                overrideValue = MAX_SAMPLE_RATE.toString(),
            )
        private val perfCpuTimeMaxPercent =
            PropOverride(
                propName = "debug.perf_cpu_time_max_percent",
                overrideValue = CPU_TIME_MAX_PERCENT.toString(),
            )
        private val perfEventMLockKb =
            PropOverride(
                propName = "debug.perf_event_mlock_kb",
                overrideValue = mLockKb.toString(),
            )

        private val securityPerfHarden =
            PropOverride(
                propName = "security.perf_harden",
                overrideValue = "0",
            )

        /** Disables block read-ahead and kernel fault-around during load order capture */
        private val readAheadOverride = ReadAheadOverride()

        @Volatile private var supportedEvents: Set<String>? = null

        /**
         * Use `simpleperf list hw cache` to conditionally query support for hardware, cache PMU
         * events. We use this to improve intra-page load coverage.
         */
        private fun supportedHwCacheEvents(): Set<String> {
            if (supportedEvents != null) return supportedEvents!!
            val output = Shell.executeScriptCaptureStdoutStderr("simpleperf list hw cache")
            val parsed = parseHwCacheEvents(output.stdout)
            supportedEvents = parsed
            return parsed
        }

        private fun parseHwCacheEvents(listOutput: String): Set<String> =
            listOutput
                .lineSequence()
                .map { it.trim() }
                .filterNot { it.isEmpty() || it.startsWith("#") || it.endsWith(":") }
                .map { line -> line.takeWhile { !it.isWhitespace() } }
                .filter { it.isNotEmpty() }
                .toSet()

        /**
         * Combines software page-fault events with hardware PMU events (when supported) to maximize
         * both cross-page and intra-page `rel_pc` coverage: a single page fault maps an entire 4 KB
         * / 16 KB code page, hiding subsequent functions on that same page, and some kernels or
         * unrooted fault-around settings skip user-space faults on cold code pages.
         *
         * Note: In `simpleperf record`, each `-c` (period) or `-f` (frequency) flag applies to the
         * `-e` events that follow it until the next `-c` / `-f`.
         */
        private fun buildEventArgs(
            period: Int,
            supportedEvents: Set<String>,
            intraSampleFreq: Int = INTRA_SAMPLE_FREQ_HZ,
        ): String {
            // We need page-faults, and if possible `iTLB-load-misses`.
            // `iTLB-load-misses` bridges the gap when we cannot reset the values for read-ahead.
            val events = StringBuilder("page-faults:u")
            if ("iTLB-load-misses" in supportedEvents) events.append(",iTLB-load-misses:u")
            // For intra-page events, we need to make sure we sample for `L1-icache-load-misses`,
            // and `instructions`.
            val intraPageEvents = mutableListOf<String>()
            if ("L1-icache-load-misses" in supportedEvents) {
                intraPageEvents += "L1-icache-load-misses:u"
            }
            if ("instructions" in supportedEvents) {
                intraPageEvents += "instructions:u"
            }
            val arguments = "-c $period -e $events"
            val intraArgs =
                if (intraPageEvents.isNotEmpty()) {
                    " -f $intraSampleFreq -e ${intraPageEvents.joinToString(",")}"
                } else {
                    ""
                }
            return "$arguments$intraArgs"
        }

        private fun callGraph(packageName: String): String {
            return if (supportsFpUnwinding(packageName)) {
                "--call-graph fp"
            } else {
                // Only 64-bit ARM guarantees frame pointers (`--call-graph fp`). On
                // 32-bit ARM and x86, frame pointers are omitted by default, so unwind using DWARF
                // debug info (`--call-graph dwarf,<bytes>`) and cap the per-sample raw stack copy
                // to 1024 bytes (down from the 8192-byte default) to avoid overflowing the buffer.
                "--call-graph dwarf,$DWARF_STACK_BYTES"
            }
        }

        private fun supportsFpUnwinding(packageName: String): Boolean {
            // Follows PerfettoHelper's ABI check order:
            // check `x86_64` / `x86` before `arm64`, because x86 emulators with ARM translation may
            // still advertise arm64-v8a in SUPPORTED_64_BIT_ABIS.
            val isArm64Device =
                when {
                    Build.SUPPORTED_64_BIT_ABIS.any { it.startsWith("x86_64") } -> false
                    Build.SUPPORTED_32_BIT_ABIS.any { it.startsWith("x86") } -> false
                    Build.SUPPORTED_64_BIT_ABIS.any { it.startsWith("arm64") } -> true
                    else -> false
                }
            val isArm64App = runCatching {
                val nativeLibDir = getInstalledPackageInfo(packageName).nativeLibraryDir
                // Apps running as 64-bit ARM have `.../lib/arm64` (or null when no native
                // lib dir is set); 32-bit ARM apps on a 64-bit device have `.../lib/arm`.
                nativeLibDir == null || nativeLibDir.endsWith("/arm64")
            }
                .getOrDefault(false)
            return isArm64Device && isArm64App
        }
    }
}

/**
 * Temporarily disables block-device read-ahead (`/sys/block/.../queue/read_ahead_kb = 0`) and
 * shrinks kernel fault-around (`/sys/kernel/debug/fault_around_bytes = pageSize`) when rooted,
 * maximizing per-page fault coverage during native library load order profiling.
 */
internal class ReadAheadOverride {
    private var values: List<Pair<String, String>> = emptyList()

    fun forceValue(pageSize: Long = Os.sysconf(_SC_PAGESIZE).coerceAtLeast(4096L)) {
        if (values.isNotEmpty()) {
            // Already overridden; skip so we don't overwrite the saved original values.
            return
        }
        if (!Shell.isSessionRooted()) {
            // Overriding sysfs `read_ahead_kb` and debugfs `fault_around_bytes` requires `root`.
            // Warning and continuing is fine when unrooted because we can still rely on
            // `iTLB-load-misses:u`, `L1-icache-load-misses:u` and `instructions:u` to capture cold
            // code pages and intra-page `rel_pc`s.
            Log.w(TAG, "Session is not rooted; skipping disabling kernel read-ahead.")
            return
        }
        // Disable block read-ahead (`0`) and shrink VMA fault-around to 1 page so each cold page
        // faults individually. Echo `<path> <old_value>` for each updated path so we can restore.
        val script =
            $$"""
            {
              for f in /sys/block/*/queue/read_ahead_kb; do
                if [ -f "$f" ]; then
                  prev=$(cat "$f")
                  if [ -n "$prev" ] && echo 0 > "$f"; then
                    echo "$f $prev"
                  fi
                fi
              done
              f="/sys/kernel/debug/fault_around_bytes"
              if [ -f "$f" ]; then
                prev=$(cat "$f")
                if [ -n "$prev" ] && echo $$pageSize > "$f"; then
                  echo "$f $prev"
                fi
              fi
            } 2>/dev/null || true
            """
                .trimIndent()

        val output = Shell.executeScriptCaptureStdout(script)
        values = parseReadAheadValues(output)
        Log.d(TAG, "Disabled read-ahead.")
    }

    fun resetIfOverridden() {
        val restore = values
        if (restore.isEmpty()) return
        values = emptyList()
        val commands =
            restore.joinToString(separator = "\n") { (path, value) ->
                "echo $value > \"$path\""
            }
        val script = "{\n$commands\n} 2>/dev/null || true"
        Shell.executeScriptSilent(script)
        Log.d(TAG, "Restored read-ahead")
    }

    companion object {
        /**
         * Parses `<block> <value>` lines emitted to `stdout` when [ReadAheadOverride] is used.
         *
         * The output looks something along the lines of:
         * ```
         * /sys/block/sda/queue/read_ahead_kb 128
         * /sys/block/dm-0/queue/read_ahead_kb 512
         * /sys/kernel/debug/fault_around_bytes 65536
         * ```
         */
        private fun parseReadAheadValues(output: String): List<Pair<String, String>> =
            output
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val parts = line.split(' ', limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        parts[0] to parts[1]
                    } else {
                        null
                    }
                }
                .toList()
    }
}
