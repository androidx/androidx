/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tracing.perfetto

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.tracing.perfetto.PerfettoSdkHandshake.RequestKeys.ACTION_ENABLE_TRACING
import androidx.tracing.perfetto.PerfettoSdkHandshake.RequestKeys.ACTION_ENABLE_TRACING_COLD_START
import androidx.tracing.perfetto.PerfettoSdkHandshake.RequestKeys.KEY_PATH
import androidx.tracing.perfetto.PerfettoSdkHandshake.RequestKeys.KEY_PERSISTENT
import androidx.tracing.perfetto.PerfettoSdkHandshake.RequestKeys.RECEIVER_CLASS_NAME
import androidx.tracing.perfetto.PerfettoSdkHandshake.ResponseKeys.KEY_EXIT_CODE
import androidx.tracing.perfetto.PerfettoSdkHandshake.ResponseKeys.KEY_MESSAGE
import androidx.tracing.perfetto.PerfettoSdkHandshake.ResponseKeys.KEY_REQUIRED_VERSION
import java.io.File
import java.lang.StringBuilder

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
    ): EnableTracingResponse {
        val libPath = librarySource?.run {
            PerfettoSdkSideloader(targetPackage).sideloadFromZipFile(
                libraryZip,
                tempDirectory,
                executeShellCommand,
                moveLibFileFromTmpDirToAppDir
            )
        }
        return sendEnableTracingBroadcast(libPath, coldStart = false)
    }

    /**
     * Attempts to prepare cold startup tracing in an app.
     *
     * @param killAppProcess function responsible for terminating the app process (no-op if the
     * process is already terminated)
     * @param librarySource optional AAR or an APK containing `libtracing_perfetto.so`
     */
    public fun enableTracingColdStart(
        killAppProcess: () -> Unit,
        librarySource: LibrarySource?
    ): EnableTracingResponse {
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

        return response
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

        val response = try {
            parseResponse(rawResponse)
        } catch (e: IllegalArgumentException) {
            val message = "Exception occurred while trying to parse a response." +
                " Error: ${e.message}. Raw response: $rawResponse."
            EnableTracingResponse(ResponseExitCodes.RESULT_CODE_ERROR_OTHER, null, message)
        }
        return response
    }

    private fun parseResponse(rawResponse: String): EnableTracingResponse {
        val line = rawResponse
            .split(Regex("\r?\n"))
            .firstOrNull { it.contains("Broadcast completed: result=") }
            ?: throw IllegalArgumentException("Cannot parse: $rawResponse")

        if (line == "Broadcast completed: result=0") return EnableTracingResponse(
            ResponseExitCodes.RESULT_CODE_CANCELLED, null, null
        )

        val matchResult =
            Regex("Broadcast completed: (result=.*?)(, data=\".*?\")?(, extras: .*)?")
                .matchEntire(line)
                ?: throw IllegalArgumentException("Cannot parse: $rawResponse")

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
            ?: throw IllegalArgumentException("Cannot parse: $rawResponse. " +
                "Unable to detect 'data=' section."
            )

        val dataMap = parseJsonMap(dataString)
        val response = EnableTracingResponse(
            dataMap[KEY_EXIT_CODE]?.toInt()
                ?: throw IllegalArgumentException("Response missing $KEY_EXIT_CODE value"),
            dataMap[KEY_REQUIRED_VERSION]
                ?: throw IllegalArgumentException("Response missing $KEY_REQUIRED_VERSION value"),
            dataMap[KEY_MESSAGE]
        )

        if (broadcastResponseCode != response.exitCode) {
            throw IllegalStateException(
                "Cannot parse: $rawResponse. Exit code " +
                    "not matching broadcast exit code."
            )
        }

        return response
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

    @RestrictTo(LIBRARY_GROUP)
    public object RequestKeys {
        public const val RECEIVER_CLASS_NAME: String = "androidx.tracing.perfetto.TracingReceiver"

        /**
         * Request to enable tracing in an app.
         *
         * The action is performed straight away allowing for warm / hot tracing. For cold start
         * tracing see [ACTION_ENABLE_TRACING_COLD_START]
         *
         * Request can include [KEY_PATH] as an optional extra.
         *
         * Response to the request is a JSON string (to allow for CLI support) with the following:
         * - [ResponseKeys.KEY_EXIT_CODE] (always)
         * - [ResponseKeys.KEY_REQUIRED_VERSION] (always)
         * - [ResponseKeys.KEY_MESSAGE] (optional)
         */
        public const val ACTION_ENABLE_TRACING: String =
            "androidx.tracing.perfetto.action.ENABLE_TRACING"

        /**
         * Request to enable cold start tracing in an app.
         *
         * For warm / hot tracing, see [ACTION_ENABLE_TRACING].
         *
         * The action must be performed in the following order, otherwise its effects are
         * unspecified:
         * - the app process must be killed before performing the action
         * - the action must then follow
         * - the app process must be killed after performing the action
         *
         * Request can include [KEY_PATH] as an optional extra.
         * Request can include [KEY_PERSISTENT] as an optional extra.
         *
         * Response to the request is a JSON string (to allow for CLI support) with the following:
         * - [ResponseKeys.KEY_EXIT_CODE] (always)
         * - [ResponseKeys.KEY_REQUIRED_VERSION] (always)
         * - [ResponseKeys.KEY_MESSAGE] (optional)
         */
        public const val ACTION_ENABLE_TRACING_COLD_START: String =
            "androidx.tracing.perfetto.action.ENABLE_TRACING_COLD_START"

        /**
         * Request to disable cold start tracing (previously enabled with
         * [ACTION_ENABLE_TRACING_COLD_START]).
         *
         * The action is particularly useful when cold start tracing was enabled in
         * [KEY_PERSISTENT] mode.
         *
         * The action must be performed in the following order, otherwise its effects are
         * unspecified:
         * - the app process must be killed before performing the action
         * - the action must then follow
         * - the app process must be killed after performing the action
         *
         * Request can include [KEY_PATH] as an optional extra.
         * Request can include [KEY_PERSISTENT] as an optional extra.
         *
         * Response to the request is a JSON string (to allow for CLI support) with the following:
         * - [ResponseKeys.KEY_EXIT_CODE] (always)
         */
        public const val ACTION_DISABLE_TRACING_COLD_START: String =
            "androidx.tracing.perfetto.action.DISABLE_TRACING_COLD_START"

        /** Path to tracing native binary file */
        public const val KEY_PATH: String = "path"

        /**
         * Boolean flag to signify whether the operation should be persistent between runs
         * (or only performed once).
         *
         * Applies to [ACTION_ENABLE_TRACING_COLD_START]
         */
        public const val KEY_PERSISTENT: String = "persistent"
    }

    @RestrictTo(LIBRARY_GROUP)
    public object ResponseKeys {
        /** Exit code as listed in [ResponseExitCodes]. */
        public const val KEY_EXIT_CODE: String = "exitCode"

        /**
         * Required version of the binaries. Java and binary library versions have to match to
         * ensure compatibility. In the Maven format, e.g. 1.2.3-beta01.
         */
        public const val KEY_REQUIRED_VERSION: String = "requiredVersion"

        /**
         * Message string that gives more information about the response, e.g. recovery steps
         * if applicable.
         */
        public const val KEY_MESSAGE: String = "message"
    }

    public object ResponseExitCodes {
        /**
         * Indicates that the broadcast resulted in `result=0`, which is an equivalent
         * of [android.app.Activity.RESULT_CANCELED].
         *
         * This most likely means that the app does not expose a [PerfettoSdkHandshake] compatible
         * receiver.
         */
        @Suppress("KDocUnresolvedReference")
        public const val RESULT_CODE_CANCELLED: Int = 0

        public const val RESULT_CODE_SUCCESS: Int = 1
        public const val RESULT_CODE_ALREADY_ENABLED: Int = 2

        /**
         * Required version described in [EnableTracingResponse.requiredVersion].
         * A follow-up [enableTracingImmediate] request expected with binaries to sideload specified.
         */
        public const val RESULT_CODE_ERROR_BINARY_MISSING: Int = 11

        /** Required version described in [EnableTracingResponse.requiredVersion]. */
        public const val RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH: Int = 12

        /**
         * Could be a result of a stale version of the binary cached locally.
         * Retrying with a freshly downloaded library likely to fix the issue.
         * More specific information in [EnableTracingResponse.message]
         */
        public const val RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR: Int = 13

        /** More specific information in [EnableTracingResponse.message] */
        public const val RESULT_CODE_ERROR_OTHER: Int = 99
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ResponseExitCodes.RESULT_CODE_CANCELLED,
        ResponseExitCodes.RESULT_CODE_SUCCESS,
        ResponseExitCodes.RESULT_CODE_ALREADY_ENABLED,
        ResponseExitCodes.RESULT_CODE_ERROR_BINARY_MISSING,
        ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
        ResponseExitCodes.RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR,
        ResponseExitCodes.RESULT_CODE_ERROR_OTHER
    )
    private annotation class EnableTracingResultCode

    public class EnableTracingResponse @RestrictTo(LIBRARY_GROUP) constructor(
        @EnableTracingResultCode public val exitCode: Int,

        /**
         * This can be `null` iff we cannot communicate with the broadcast receiver of the target
         * process (e.g. app does not offer Perfetto tracing) or if we cannot parse the response
         * from the receiver. In either case, tracing is unlikely to work under these circumstances,
         * and more context on how to proceed can be found in [exitCode] or [message] properties.
         */
        public val requiredVersion: String?,

        public val message: String?
    )
}
