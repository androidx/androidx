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

import androidx.tracing.perfetto.handshake.protocol.RequestKeys.ACTION_DISABLE_TRACING_COLD_START
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING_COLD_START
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.KEY_PATH
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.KEY_PERSISTENT
import androidx.tracing.perfetto.handshake.protocol.RequestKeys.RECEIVER_CLASS_NAME
import androidx.tracing.perfetto.handshake.protocol.Response
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_MESSAGE
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_REQUIRED_VERSION
import androidx.tracing.perfetto.handshake.protocol.ResponseKeys.KEY_RESULT_CODE
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes
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
     * Attempts to enable tracing in the app. It will wake up (or start) the app process, so it will
     * act as warm/hot tracing. For cold tracing see [enableTracingColdStart]
     *
     * Note: if the app process is not running, it will be launched making the method a bad choice
     * for cold tracing (use [enableTracingColdStart] instead.
     *
     * @param librarySource optional AAR or an APK containing `libtracing_perfetto.so`
     */
    public fun enableTracingImmediate(
        librarySource: LibrarySource? = null
    ): Response = safeExecute {
        val libPath = librarySource?.run {
            when (this) {
                is LibrarySource.ZipLibrarySource -> {
                    PerfettoSdkSideloader(targetPackage).sideloadFromZipFile(
                        libraryZip,
                        tempDirectory,
                        executeShellCommand,
                        moveLibFileFromTmpDirToAppDir
                    )
                }
            }
        }
        sendTracingBroadcast(ACTION_ENABLE_TRACING, libPath)
    }

    /**
     * Attempts to prepare cold startup tracing in the app.
     *
     * Leaves the app process in a terminated state.
     *
     * @param persistent if set to true, cold start tracing mode is persisted between app runs and
     * must be cleared using [disableTracingColdStart]. Otherwise, cold start tracing is enabled
     * only for the first app start since enabling.
     * While persistent mode reduces some overhead of setting up tracing, it recommended to use
     * non-persistent mode as it does not pose the risk of leaving cold start tracing persistently
     * enabled in case of a failure to clean-up with [disableTracingColdStart].
     *
     * @param librarySource optional AAR or an APK containing `libtracing_perfetto.so`
     */
    @JvmOverloads
    public fun enableTracingColdStart(
        persistent: Boolean = false,
        librarySource: LibrarySource? = null
    ): Response = safeExecute {
        // sideload the `libtracing_perfetto.so` file if applicable
        val libPath = librarySource?.run {
            when (this) {
                is LibrarySource.ZipLibrarySource -> {
                    PerfettoSdkSideloader(targetPackage).sideloadFromZipFile(
                        libraryZip,
                        tempDirectory,
                        executeShellCommand,
                        moveLibFileFromTmpDirToAppDir
                    )
                }
            }
        }

        // ensure a clean start (e.g. in case tracing is already enabled)
        killAppProcess()

        // verify (by performing a regular handshake) that we can enable tracing at app startup
        val response = sendTracingBroadcast(
            ACTION_ENABLE_TRACING_COLD_START,
            libPath,
            persistent = persistent
        )

        // Terminate the app process regardless of the response:
        // - if enabling tracing is successful, the process needs to be terminated for cold tracing
        // - if enabling tracing is unsuccessful, we still want to terminate the app process to
        // achieve deterministic behaviour of this method
        killAppProcess()

        response
    }

    /**
     * Disables cold start tracing in the app if previously enabled by [enableTracingColdStart].
     *
     * No-op if cold start tracing was not enabled in the app, or if it was enabled in
     * the non-`persistent` mode and the app has already been started at least once.
     *
     * The function initially enables the app process (if not already enabled), but leaves it in
     * a terminated state after executing.
     *
     * @see [enableTracingColdStart]
     */
    public fun disableTracingColdStart(): Response = safeExecute {
        sendTracingBroadcast(ACTION_DISABLE_TRACING_COLD_START).also {
            killAppProcess()
        }
    }

    private fun sendTracingBroadcast(
        action: String,
        libPath: File? = null,
        persistent: Boolean? = null
    ): Response {
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

    private fun parseResponse(rawResponse: String): Response {
        val line = rawResponse
            .split(Regex("\r?\n"))
            .firstOrNull { it.contains("Broadcast completed: result=") }
            ?: throw PerfettoSdkHandshakeException("Cannot parse: $rawResponse")

        if (line == "Broadcast completed: result=0") return Response(
            ResponseResultCodes.RESULT_CODE_CANCELLED, null, null
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
        val response = Response(
            dataMap[KEY_RESULT_CODE]?.toInt()
                ?: throw PerfettoSdkHandshakeException("Response missing $KEY_RESULT_CODE value"),
            dataMap[KEY_REQUIRED_VERSION]
                ?: throw PerfettoSdkHandshakeException(
                    "Response missing $KEY_REQUIRED_VERSION" +
                        " value"
                ),
            dataMap[KEY_MESSAGE]
        )

        if (broadcastResponseCode != response.resultCode) {
            throw PerfettoSdkHandshakeException(
                "Cannot parse: $rawResponse. Result code not matching broadcast result code."
            )
        }

        return response
    }

    /** Executes provided [block] and wraps exceptions in an appropriate [Response] */
    private fun safeExecute(block: () -> Response): Response = try {
        block()
    } catch (exception: Exception) {
        Response(ResponseResultCodes.RESULT_CODE_ERROR_OTHER, null, exception.message)
    }

    private fun killAppProcess() {
        // on a root session we can use `killall` which works on both system and user apps
        // `am force-stop` only works on user apps
        val isRootSession = executeShellCommand("id").contains("uid=0(root)")
        val result = when (isRootSession) {
            true -> executeShellCommand("killall $targetPackage")
            else -> executeShellCommand("am force-stop $targetPackage")
        }
        if (result.isNotBlank() && !result.contains("No such process")) {
            throw PerfettoSdkHandshakeException("Issue while trying to kill app process: $result")
        }
    }

    /** Provides means to sideload Perfetto SDK native binaries */
    public sealed class LibrarySource {
        internal class ZipLibrarySource @Suppress("StreamFiles") constructor(
            internal val libraryZip: File,
            internal val tempDirectory: File,
            internal val moveLibFileFromTmpDirToAppDir: FileMover
        ) : LibrarySource()

        public companion object {
            /**
             * Provides means to sideload Perfetto SDK native binaries with a library AAR used as
             * a source
             *
             * @param aarFile an AAR file containing `libtracing_perfetto.so`
             * @param tempDirectory a directory directly accessible to the caller process (used for
             * extraction of the binaries from the zip)
             * @param moveLibFileFromTmpDirToAppDir a function capable of moving the binary file
             * from the [tempDirectory] to an app accessible folder
             */
            @Suppress("StreamFiles")
            @JvmStatic
            public fun aarLibrarySource(
                aarFile: File,
                tempDirectory: File,
                moveLibFileFromTmpDirToAppDir: FileMover
            ): LibrarySource =
                ZipLibrarySource(aarFile, tempDirectory, moveLibFileFromTmpDirToAppDir)

            /**
             * Provides means to sideload Perfetto SDK native binaries with an APK containing
             * the library used as a source
             *
             * @param apkFile an APK file containing `libtracing_perfetto.so`
             * @param tempDirectory a directory directly accessible to the caller process (used for
             * extraction of the binaries from the zip)
             * @param moveLibFileFromTmpDirToAppDir a function capable of moving the binary file
             * from the [tempDirectory] to an app accessible folder
             */
            @Suppress("StreamFiles")
            @JvmStatic
            public fun apkLibrarySource(
                apkFile: File,
                tempDirectory: File,
                moveLibFileFromTmpDirToAppDir: FileMover
            ): LibrarySource =
                ZipLibrarySource(apkFile, tempDirectory, moveLibFileFromTmpDirToAppDir)
        }
    }
}

/** Internal exception class for issues specific to [PerfettoSdkHandshake] */
private class PerfettoSdkHandshakeException(message: String) : Exception(message)
