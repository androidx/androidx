/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tracing.perfetto.handshake

import androidx.tracing.perfetto.handshake.protocol.EnableTracingResponse
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING_COLD_START
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.KEY_PATH
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.KEY_PERSISTENT
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.RECEIVER_CLASS_NAME
import androidx.tracing.perfetto.handshake.protocol.ResponseExitCodes
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_EXIT_CODE
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_MESSAGE
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_REQUIRED_VERSION
import java.io.File

/**
 * Handshake implementation allowing to enable Perfetto SDK tracing in an app that enables it.
 *
 * @param targetPackage package name of the target app
 * @param parseJsonMap function parsing a flat map in a JSON format into a `Map<String, String>`
 * e.g. `"{ 'key 1': 'value 1', 'key 2': 'value 2' }"` ->
 * `mapOf("key 1" to "value 1", "key 2" to "value 2")`
 * @param executeShellCommand function allowing to execute `adb shell` commands on the target device
 *
 * For error handling, note that [parseJsonMap] and [executeShellCommand] will be called on the same
 * thread as [enableTracingImmediate] and [enableTracingColdStart].
 */
public class PerfettoSdkHandshake(
    private val targetPackage: String,
    private val parseJsonMap: (jsonString: String) -> Map<String, String>,
    private val executeShellCommand: ShellCommandExecutor
) {
    /**
     * Attempts to enable tracing in an app. It will wake up (or start) the app process, so it will
     * act as warm/hot tracing. For cold tracing see [enableTracingColdStart]
     *
     * Note: if the app process is not running, it will be launched making the method a bad choice
     * for cold tracing (use [enableTracingColdStart] instead.
     *
     * @param librarySource optional AAR or an APK containing `libtracing_perfetto.so`
     */
    public fun enableTracingImmediate(
        librarySource: LibrarySource? = null
    ): EnableTracingResponse = safeExecute {
        val libPath = librarySource?.run {
            PerfettoSdkSideloader(targetPackage).sideloadFromZipFile(
                libraryZip,
                tempDirectory,
                executeShellCommand,
                moveLibFileFromTmpDirToAppDir
            )
        }
        sendEnableTracingBroadcast(libPath, coldStart = false)
    }

    /**
     * Attempts to prepare cold startup tracing in an app.
     *
     * @param librarySource optional AAR or an APK containing `libtracing_perfetto.so`
     */
    public fun enableTracingColdStart(
        librarySource: LibrarySource?
    ): EnableTracingResponse = safeExecute {
        // sideload the `libtracing_perfetto.so` file if applicable
        val libPath = librarySource?.run {
            PerfettoSdkSideloader(targetPackage).sideloadFromZipFile(
                libraryZip,
                tempDirectory,
                executeShellCommand,
                moveLibFileFromTmpDirToAppDir
            )
        }

        // ensure a clean start (e.g. in case tracing is already enabled)
        killAppProcess()

        // verify (by performing a regular handshake) that we can enable tracing at app startup
        val response = sendEnableTracingBroadcast(libPath, coldStart = true, persistent = false)
        if (response.exitCode == ResponseExitCodes.RESULT_CODE_SUCCESS) {
            // terminate the app process (that we woke up by issuing a broadcast earlier)
            killAppProcess()
        }

        response
    }

    private fun sendEnableTracingBroadcast(
        libPath: File? = null,
        coldStart: Boolean,
        persistent: Boolean? = null
    ): EnableTracingResponse {
        val action = if (coldStart) ACTION_ENABLE_TRACING_COLD_START else ACTION_ENABLE_TRACING
        val commandBuilder = StringBuilder("am broadcast -a $action")
        if (persistent != null) commandBuilder.append(" --es $KEY_PERSISTENT $persistent")
        if (libPath != null) commandBuilder.append(" --es $KEY_PATH $libPath")
        commandBuilder.append(" $targetPackage/$RECEIVER_CLASS_NAME")

        val rawResponse = executeShellCommand(commandBuilder.toString())
        return try {
            parseResponse(rawResponse)
        } catch (e: Exception) {
            throw PerfettoSdkHandshakeException(
                "Exception occurred while trying to parse a response." +
                    " Error: ${e.message}. Raw response: $rawResponse."
            )
        }
    }

    private fun parseResponse(rawResponse: String): EnableTracingResponse {
        val line = rawResponse
            .split(Regex("\r?\n"))
            .firstOrNull { it.contains("Broadcast completed: result=") }
            ?: throw PerfettoSdkHandshakeException("Cannot parse: $rawResponse")

        if (line == "Broadcast completed: result=0") return EnableTracingResponse(
            ResponseExitCodes.RESULT_CODE_CANCELLED, null, null
        )

        val matchResult =
            Regex("Broadcast completed: (result=.*?)(, data=\".*?\")?(, extras: .*)?")
                .matchEntire(line)
                ?: throw PerfettoSdkHandshakeException("Cannot parse: $rawResponse")

        val broadcastResponseCode = matchResult
            .groups[1]
            ?.value
            ?.substringAfter("result=")
            ?.toIntOrNull()

        val dataString = matchResult
            .groups
            .firstOrNull { it?.value?.startsWith(", data=") ?: false }
            ?.value
            ?.substringAfter(", data=\"")
            ?.dropLast(1)
            ?: throw PerfettoSdkHandshakeException(
                "Cannot parse: $rawResponse. " +
                    "Unable to detect 'data=' section."
            )

        val dataMap = parseJsonMap(dataString)
        val response = EnableTracingResponse(
            dataMap[KEY_EXIT_CODE]?.toInt()
                ?: throw PerfettoSdkHandshakeException("Response missing $KEY_EXIT_CODE value"),
            dataMap[KEY_REQUIRED_VERSION]
                ?: throw PerfettoSdkHandshakeException(
                    "Response missing $KEY_REQUIRED_VERSION" +
                        " value"
                ),
            dataMap[KEY_MESSAGE]
        )

        if (broadcastResponseCode != response.exitCode) {
            throw PerfettoSdkHandshakeException(
                "Cannot parse: $rawResponse. Exit code not matching broadcast exit code."
            )
        }

        return response
    }

    /** Executes provided [block] and wraps exceptions in an appropriate [EnableTracingResponse] */
    private fun safeExecute(block: () -> EnableTracingResponse): EnableTracingResponse = try {
        block()
    } catch (exception: Exception) {
        EnableTracingResponse(ResponseExitCodes.RESULT_CODE_ERROR_OTHER, null, exception.message)
    }

    private fun killAppProcess() {
        // on a root session we can use `killall` which works on both system and user apps
        // `am force-stop` only works on user apps
        val isRootSession = executeShellCommand("id").contains("uid=0(root)")
        val result = when (isRootSession) {
            true -> executeShellCommand("killall $targetPackage")
            else -> executeShellCommand("am force-stop $targetPackage")
        }
        if (result.isNotBlank()) throw PerfettoSdkHandshakeException(
            "Issue while trying to kill app process: $result"
        )
    }

    /**
     * @param libraryZip either an AAR or an APK containing `libtracing_perfetto.so`
     * @param tempDirectory a directory directly accessible to the caller process (used for
     * extraction of the binaries from the zip)
     * @param moveLibFileFromTmpDirToAppDir a function capable of moving the binary file from
     * the [tempDirectory] to an app accessible folder
     */
    // TODO(245426369): consider moving to a factory pattern for constructing these and refer to
    //  this one as `aarLibrarySource` and `apkLibrarySource`
    public class LibrarySource @Suppress("StreamFiles") constructor(
        internal val libraryZip: File,
        internal val tempDirectory: File,
        internal val moveLibFileFromTmpDirToAppDir: FileMover
    )
}

/** Internal exception class for issues specific to [PerfettoSdkHandshake] */
private class PerfettoSdkHandshakeException(message: String) : Exception(message)