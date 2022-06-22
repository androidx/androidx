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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.userspaceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.TracingReceiver.Companion.RESULT_CODE_SUCCESS
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

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

        val handshake = PerfettoHandshake(targetPackage)
        val response = handshake.requestEnable(null).let {
            if (it.exitCode == RESULT_CODE_ERROR_BINARY_MISSING && provideBinariesIfMissing)
                handshake.requestEnable(pushLibrary(targetPackage)) // provide binaries and retry
            else
                it // no retry
        }

        return when (response.exitCode) {
            0, null -> "The broadcast to enable tracing was not received. This most likely means " +
                "that the app does not contain the `androidx.tracing.tracing-perfetto` " +
                "library as its dependency."
            RESULT_CODE_SUCCESS -> null
            RESULT_CODE_ALREADY_ENABLED -> "Perfetto SDK already enabled."
            RESULT_CODE_ERROR_BINARY_MISSING ->
                "Perfetto SDK binary dependencies missing. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.errorMessage}."
            RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH ->
                "Perfetto SDK binary mismatch. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.errorMessage}."
            RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR ->
                "Perfetto SDK binary verification failed. " +
                    "Required version: ${response.requiredVersion}. " +
                    "Error: ${response.errorMessage}. " +
                    "If working with an unreleased snapshot, ensure all modules are built " +
                    "against the same snapshot (e.g. clear caches and rebuild)."
            RESULT_CODE_ERROR_OTHER -> "Error: ${response.errorMessage}."
            else -> throw RuntimeException("Unrecognized exit code: ${response.exitCode}.")
        }
    }

    private fun pushLibrary(targetPackage: String): String {
        val context = InstrumentationRegistry.getInstrumentation().context

        val libFileName = "libtracing_perfetto.so"
        val abiDirName = File(context.applicationInfo.nativeLibraryDir).name
        val baseApk = File(context.applicationInfo.publicSourceDir!!)

        val shellWriteableAppReadableDir = File("/sdcard/Android/media/$targetPackage/files")
        val dstDir = shellWriteableAppReadableDir.resolve("lib/$abiDirName")
        val dstFile = dstDir.resolve(libFileName)
        val tmpFile = Outputs.dirUsableByAppAndShell.resolve(".tmp_$libFileName")

        val rxLibPathInsideZip = Regex(".*lib/[^/]*$abiDirName[^/]*/$libFileName")

        ZipInputStream(FileInputStream(baseApk)).use { stream ->
            findEntry@ while (true) {
                val entry = stream.nextEntry ?: break@findEntry
                if (!entry.name.matches(rxLibPathInsideZip)) continue@findEntry

                // found the right entry, so copying it to destination
                BufferedOutputStream(tmpFile.outputStream()).use { dstStream ->
                    val buffer = ByteArray(1024)
                    writing@ while (true) {
                        val readCount = stream.read(buffer)
                        if (readCount <= 0) break@writing
                        dstStream.write(buffer, 0, readCount)
                    }
                }
                executeShellCommand("mkdir -p ${dstDir.path}", Regex("^$"))
                executeShellCommand("mv ${tmpFile.path} ${dstFile.path}", Regex("^$"))

                return dstFile.path
            }
        }

        throw IllegalStateException(
            "Unable to locate $libFileName to enable Perfetto SDK. Tried inside ${baseApk.path}."
        )
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
