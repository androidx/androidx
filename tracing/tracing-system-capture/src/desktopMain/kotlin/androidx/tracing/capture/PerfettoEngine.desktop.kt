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

package androidx.tracing.capture

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

// The process timeout
internal const val TIMEOUT_SECONDS = 5L

/** @return `true` iff `tracebox` is on the `$PATH`. */
internal fun isTraceboxOnPath(): Boolean {
    return processBuilder("which tracebox") { output ->
        val process = output.builder.start()
        process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        process.exitValue() == 0
    }
}

/**
 * The [SystemTracing] implementation on Linux.
 *
 * The library itself does **not** bundle Perfetto binaries. Instead, the user is responsible for
 * bootstrapping the latest release of `tracebox` from the latest Perfetto
 * [release](https://github.com/google/perfetto/releases).
 *
 * **Note**: Ensure that `tracebox` is an **executable**, and has been added to your shell's `PATH`.
 */
internal class PerfettoEngine : AutoCloseable {
    /* `true` when a tracing session is active. */
    @Volatile private var started: Boolean = false

    // The Perfetto sub-process
    @Volatile private var process: Process? = null

    @Volatile private var config: File? = null

    private val lock: Any = Any()

    public fun start(configProtoText: File? = null, output: File) {
        if (started) return

        synchronized(lock) {
            if (started) return

            started = true
            // Start the tracing session.
            checkPrerequisites()
            traceBox(configProtoText, output)
        }
    }

    override fun close() {
        if (!started) return

        synchronized(lock) {
            if (!started) return

            started = false
            // Send a SIGTERM
            process?.destroy()
            process?.waitFor(/* timeout= */ TIMEOUT_SECONDS, /* unit= */ TimeUnit.SECONDS)
            config?.delete()
        }
    }

    internal fun traceBox(protoTxt: File?, output: File) {
        setupPerfettoConfig(protoTxt)
        // Delete the output file if one already exists.
        if (output.exists()) output.delete()
        // Make output directories if necessary.
        output.parentFile?.mkdirs()

        processBuilder(
            command = "tracebox --txt -c ${config?.absolutePath} -o ${output.absolutePath}"
        ) { output ->
            output.use {
                output.builder.inheritIO()
                process = output.builder.start()
            }
        }
    }

    internal fun setupPerfettoConfig(protoTxt: File?) {
        val pbTxt =
            protoTxt?.readText()
                ?: Thread.currentThread()
                    .contextClassLoader
                    .getResourceAsStream("config.textproto")
                    ?.bufferedReader()
                    ?.readText()

        val config = Files.createTempFile(/* prefix= */ "perfetto", /* suffix= */ "config").toFile()
        this.config = config
        require(pbTxt != null)
        config.writeText(pbTxt)
    }

    /** Runs preflight checks for Perfetto. */
    internal fun checkPrerequisites() {
        checkTracingMarkers()
        check(isTraceboxOnPath()) { "Cannot find `tracebox` on \$PATH" }
    }

    private fun checkTracingMarkers() {
        processBuilder("ls -ld /sys/kernel/tracing") { output ->
            output.use {
                val process = output.builder.start()
                val success =
                    process.waitFor(/* timeout= */ TIMEOUT_SECONDS, /* unit= */ TimeUnit.SECONDS)
                check(success) { "Unable to check for tracing marker `/sys/kernel/tracing`." }
                val exitCode = process.exitValue()
                check(exitCode == 0) { // Abnormal termination
                    val message = output.stderr.readText()
                    "`ls -ld /sys/kernel/tracing` failed: $message"
                }
            }
        }
    }
}
