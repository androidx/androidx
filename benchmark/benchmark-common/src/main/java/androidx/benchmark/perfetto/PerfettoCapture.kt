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
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.ShellFile
import androidx.benchmark.UserFile
import androidx.benchmark.UserInfo
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.perfetto.handshake.PerfettoSdkHandshake
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_MISSING
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import java.io.File
import java.io.StringReader

/** Enables capturing a Perfetto trace */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PerfettoCapture(
    /**
     * Bundled is available above API 28, but we default to using unbundled as well on API 29, as
     * ProcessStatsConfig.scan_all_processes_on_start isn't supported on the bundled version.
     */
    unbundled: Boolean = Build.VERSION.SDK_INT <= 29
) {

    private val helper: PerfettoHelper = PerfettoHelper(unbundled)

    fun isRunning() = helper.isRunning()

    /** Start collecting perfetto trace. */
    fun start(config: PerfettoConfig) =
        inMemoryTrace("start perfetto") {
            // Write config proto to dir that shell can read
            //     We use `.pb` even with textproto so we'll only ever have one file
            val configProtoFile =
                if (UserInfo.currentUserId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ShellFile.inTempDir("trace_config.pb")
                } else {
                    UserFile.inOutputsDir("trace_config.pb")
                }

            try {
                inMemoryTrace("write config") { config.writeTo(configProtoFile) }
                inMemoryTrace("start perfetto process") {
                    helper.startCollecting(configProtoFile.absolutePath, config.isTextProto)
                }
            } finally {
                configProtoFile.delete()
            }
        }

    /**
     * Stop collection, and record trace to the specified file path.
     *
     * @param destinationPath Absolute path to write perfetto trace to. Must be shell-writable, such
     *   as result of `context.getExternalFilesDir(null)` or other similar `external` paths.
     */
    public fun stop(destinationPath: String, inMemoryTracingLabel: String?) =
        inMemoryTrace("stop perfetto") {
            helper.stopCollecting(destinationPath, inMemoryTracingLabel)
        }

    /**
     * Enables Perfetto SDK tracing in the [PerfettoSdkConfig.targetPackage]
     *
     * @return a pair of [androidx.tracing.perfetto.handshake.protocol.ResultCode] and a
     *   user-friendly message explaining the code
     */
    @RequiresApi(30) // TODO(234351579): Support API < 30
    @CheckResult
    fun enableAndroidxTracingPerfetto(config: PerfettoSdkConfig): Pair<Int, String> =
        enableAndroidxTracingPerfetto(
            targetPackage = config.targetPackage,
            provideBinariesIfMissing = config.provideBinariesIfMissing,
            isColdStartupTracing = config.launchWouldBeCold(),
        )

    @RequiresApi(30) // TODO(234351579): Support API < 30
    @CheckResult
    /**
     * Enables Perfetto SDK tracing in the [PerfettoSdkConfig.targetPackage]
     *
     * @return a pair of [androidx.tracing.perfetto.handshake.protocol.ResultCode] and a
     *   user-friendly message explaining the code
     */
    private fun enableAndroidxTracingPerfetto(
        targetPackage: String,
        provideBinariesIfMissing: Boolean,
        isColdStartupTracing: Boolean,
    ): Pair<Int, String> {
        if (!isAbiSupported()) {
            throw IllegalStateException("Unsupported ABI (${Build.SUPPORTED_ABIS.joinToString()})")
        }

        // construct a handshake
        val handshake =
            PerfettoSdkHandshake(
                targetPackage = targetPackage,
                parseJsonMap = { jsonString: String ->
                    sequence {
                            JsonReader(StringReader(jsonString)).use { reader ->
                                reader.beginObject()
                                while (reader.hasNext()) yield(
                                    reader.nextName() to reader.nextString()
                                )
                                reader.endObject()
                            }
                        }
                        .toMap()
                },
                executeShellCommand = { cmd ->
                    val (stdout, stderr) = Shell.executeScriptCaptureStdoutStderr(cmd)
                    listOf(stdout, stderr)
                        .filter { it.isNotBlank() }
                        .joinToString(separator = System.lineSeparator())
                },
            )

        // try without supplying external Perfetto SDK tracing binaries
        val responseNoSideloading =
            if (isColdStartupTracing) {
                handshake.enableTracingColdStart()
            } else {
                handshake.enableTracingImmediate()
            }

        // if required, retry by supplying external Perfetto SDK tracing binaries
        val response =
            if (
                responseNoSideloading.resultCode == RESULT_CODE_ERROR_BINARY_MISSING &&
                    provideBinariesIfMissing
            ) {
                val librarySource = constructLibrarySource()
                if (isColdStartupTracing) {
                    // do not support persistent for now
                    handshake.enableTracingColdStart(persistent = false, librarySource)
                } else {
                    handshake.enableTracingImmediate(librarySource)
                }
            } else {
                // no retry
                responseNoSideloading
            }

        // process the response
        val message =
            when (response.resultCode) {
                0 ->
                    "The broadcast to enable tracing was not received. This most likely means " +
                        "that the app does not contain the `androidx.tracing.tracing-perfetto` " +
                        "library as its dependency."
                RESULT_CODE_SUCCESS -> "Success"
                RESULT_CODE_ALREADY_ENABLED -> "Perfetto SDK already enabled."
                RESULT_CODE_ERROR_BINARY_MISSING ->
                    binaryMissingResponseString(response.requiredVersion, response.message)
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
                RESULT_CODE_ERROR_OTHER ->
                    if (responseNoSideloading.resultCode == RESULT_CODE_ERROR_BINARY_MISSING) {
                        binaryMissingResponseString(
                            responseNoSideloading.requiredVersion,
                            response
                                .message, // note: we're using the error from the sideloading attempt
                        )
                    } else {
                        "Error: ${response.message}."
                    }
                else -> throw RuntimeException("Unrecognized result code: ${response.resultCode}.")
            }
        return response.resultCode to message
    }

    private fun binaryMissingResponseString(requiredVersion: String?, message: String?) =
        "Perfetto SDK binary dependencies missing. " +
            "Required version: $requiredVersion. " +
            "Error: $message.\n" +
            "To fix, declare the following dependency in your" +
            " *benchmark* project (i.e. not the app under benchmark): " +
            "\nandroidTestImplementation(" +
            "\"androidx.tracing:tracing-perfetto-binary:$requiredVersion\")"

    private fun constructLibrarySource(): PerfettoSdkHandshake.LibrarySource {
        val baseApk =
            File(
                InstrumentationRegistry.getInstrumentation()
                    .context
                    .applicationInfo
                    .publicSourceDir!!
            )

        val mvTmpFileDstFile = { srcFile: File, dstFile: File ->
            Shell.mkdir(dstFile.parentFile!!.path)
            Shell.mv(srcFile.path, dstFile.path)
        }

        return PerfettoSdkHandshake.LibrarySource.apkLibrarySource(
            baseApk,
            Outputs.dirUsableByAppAndShell,
            mvTmpFileDstFile,
        )
    }

    class PerfettoSdkConfig(
        val targetPackage: String,
        val processState: InitialProcessState,
        val provideBinariesIfMissing: Boolean = true,
    ) {
        /** State of process before tracing begins. */
        enum class InitialProcessState {
            /** will schedule tracing on next cold start */
            NotAlive,

            /** enable tracing on the target process immediately */
            Alive,

            /** trigger cold start vs running tracing based on a check if process is alive */
            Unknown,
        }

        /**
         * Returns true if the target package is not running, and thus will require a cold start
         * tracing handshake
         */
        fun launchWouldBeCold(): Boolean {
            return when (processState) {
                InitialProcessState.NotAlive -> true
                InitialProcessState.Alive -> false
                InitialProcessState.Unknown -> !Shell.isPackageAlive(targetPackage)
            }
        }
    }
}
