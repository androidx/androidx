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

package androidx.benchmark.perfetto

import android.os.Build
import android.util.JsonReader
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.userspaceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.perfetto.PerfettoHandshake
import androidx.tracing.perfetto.PerfettoHandshake.ExternalLibraryProvider
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.PerfettoHandshake.ResponseExitCodes.RESULT_CODE_SUCCESS
import java.io.File
import java.io.StringReader

/**
 * Enables capturing a Perfetto trace
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public class PerfettoCapture(
    /**
     * Bundled is available above API 28, but we default to using unbundled as well on API 29, as
     * ProcessStatsConfig.scan_all_processes_on_start isn't supported on the bundled version.
     */
    unbundled: Boolean = Build.VERSION.SDK_INT in 21..29
) {

    private val helper: PerfettoHelper = PerfettoHelper(unbundled)

    public fun isRunning() = helper.isRunning()

    /**
     * Start collecting perfetto trace.
     *
     * TODO: provide configuration options
     */
    public fun start(packages: List<String>) = userspaceTrace("start perfetto") {
        // Write binary proto to dir that shell can read
        // TODO: cache on disk
        val configProtoFile = File(Outputs.dirUsableByAppAndShell, "trace_config.pb")
        try {
            userspaceTrace("write config") {
                val atraceApps = if (Build.VERSION.SDK_INT <= 28 || packages.isEmpty()) {
                    packages
                } else {
                    listOf("*")
                }
                configProtoFile.writeBytes(perfettoConfig(atraceApps).validateAndEncode())
            }
            userspaceTrace("start perfetto process") {
                helper.startCollecting(configProtoFile.absolutePath, false)
            }
        } finally {
            configProtoFile.delete()
        }
    }

    /**
     * Stop collection, and record trace to the specified file path.
     *
     * @param destinationPath Absolute path to write perfetto trace to. Must be shell-writable,
     * such as result of `context.getExternalFilesDir(null)` or other similar `external` paths.
     */
    public fun stop(destinationPath: String) = userspaceTrace("stop perfetto") {
        helper.stopCollecting(destinationPath)
    }

    /**
     * Enables Perfetto SDK tracing in an app if present. Provides required binary dependencies to
     * the app if they're missing and the [provideBinariesIfMissing] parameter is set to `true`.
     */
    @RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
    fun enableAndroidxTracingPerfetto(
        targetPackage: String,
        provideBinariesIfMissing: Boolean
    ): String? {
        if (!isAbiSupported()) {
            throw IllegalStateException("Unsupported ABI (${Build.SUPPORTED_ABIS.joinToString()})")
        }

        // construct a handshake
        val handshake = PerfettoHandshake(
            targetPackage = targetPackage,
            parseJsonMap = { jsonString: String ->
                sequence {
                    JsonReader(StringReader(jsonString)).use { reader ->
                        reader.beginObject()
                        while (reader.hasNext()) yield(reader.nextName() to reader.nextString())
                        reader.endObject()
                    }
                }.toMap()
            },
            executeShellCommand = Shell::executeCommand
        )

        // negotiate enabling tracing in the app
        val response = handshake.enableTracing(null).let {
            if (it.exitCode == RESULT_CODE_ERROR_BINARY_MISSING && provideBinariesIfMissing) {
                val baseApk = File(
                    InstrumentationRegistry.getInstrumentation()
                        .context.applicationInfo.publicSourceDir!!
                )
                val libraryProvider = ExternalLibraryProvider(
                    baseApk,
                    Outputs.dirUsableByAppAndShell
                ) { tmpFile, dstFile ->
                    executeShellCommand("mkdir -p ${dstFile.parentFile!!.path}", Regex("^$"))
                    executeShellCommand("mv ${tmpFile.path} ${dstFile.path}", Regex("^$"))
                }
                handshake.enableTracing(libraryProvider)
            } // provide binaries and retry
            else
                it // no retry
        }

        // process the response
        return when (response.exitCode) {
            0 -> "The broadcast to enable tracing was not received. This most likely means " +
                "that the app does not contain the `androidx.tracing.tracing-perfetto` " +
                "library as its dependency."
            RESULT_CODE_SUCCESS -> null
            RESULT_CODE_ALREADY_ENABLED -> "Perfetto SDK already enabled."
            RESULT_CODE_ERROR_BINARY_MISSING ->
                "Perfetto SDK binary dependencies missing. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.message}."
            RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH ->
                "Perfetto SDK binary mismatch. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.message}."
            RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR ->
                "Perfetto SDK binary verification failed. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.message}. " +
                    "If working with an unreleased snapshot, ensure all modules are built " +
                    "against the same snapshot (e.g. clear caches and rebuild)."
            RESULT_CODE_ERROR_OTHER -> "Error: ${response.message}."
            else -> throw RuntimeException("Unrecognized exit code: ${response.exitCode}.")
        }
    }

    private fun executeShellCommand(command: String, expectedResponse: Regex) {
        val response = Shell.executeCommand(command)
        if (!response.matches(expectedResponse)) throw IllegalStateException(
            "Command response not matching expected." +
                " Command: $command." +
                " Expected response: ${expectedResponse.pattern}."
        )
    }
}
