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
import androidx.tracing.perfetto.PerfettoHandshake.RequestKeys.ACTION_ENABLE_TRACING
import androidx.tracing.perfetto.PerfettoHandshake.RequestKeys.KEY_PATH
import androidx.tracing.perfetto.PerfettoHandshake.RequestKeys.RECEIVER_CLASS_NAME
import androidx.tracing.perfetto.PerfettoHandshake.ResponseKeys.KEY_EXIT_CODE
import androidx.tracing.perfetto.PerfettoHandshake.ResponseKeys.KEY_MESSAGE
import androidx.tracing.perfetto.PerfettoHandshake.ResponseKeys.KEY_REQUIRED_VERSION
import java.io.File
import java.util.zip.ZipFile

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
 * thread as [enableTracing].
 */
public class PerfettoHandshake(
    private val targetPackage: String,
    private val parseJsonMap: (jsonString: String) -> Map<String, String>,
    private val executeShellCommand: (command: String) -> String
) {
    /**
     * Requests that tracing is enabled in the target app.
     *
     * @param libraryProvider optional provider of Perfetto SDK binaries allowing to sideload them
     * if not already present in the target app
     */
    public fun enableTracing(
        libraryProvider: ExternalLibraryProvider? = null
    ): EnableTracingResponse {
        val pathExtra = libraryProvider?.let {
            val libPath = it.pushLibrary(targetPackage, getDeviceAbi())
            """--es $KEY_PATH $libPath"""
        } ?: ""
        val command = "am broadcast -a $ACTION_ENABLE_TRACING" +
            " $pathExtra " +
            "$targetPackage/$RECEIVER_CLASS_NAME"
        val rawResponse = executeShellCommand(command)

        return try {
            parseResponse(rawResponse)
        } catch (e: IllegalArgumentException) {
            val message = "Exception occurred while trying to parse a response." +
                " Error: ${e.message}. Raw response: $rawResponse."
            EnableTracingResponse(ResponseExitCodes.RESULT_CODE_ERROR_OTHER, null, message)
        }
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
     * @param libraryZip - zip containing the library (e.g. tracing-perfetto-binary-<version>.aar
     * or an APK already containing the library)
     * @param tempDirectory - temporary folder where we can extract the library file from
     * [libraryZip]; they need to be on the same device
     * @param moveTempDirectoryFileToDestination - function copying the library file from a location
     * in [tempDirectory] to a location on the device.
     */
    public class ExternalLibraryProvider @Suppress("StreamFiles") constructor(
        private val libraryZip: File,
        private val tempDirectory: File,
        private val moveTempDirectoryFileToDestination: (
            /** File located in a previously supplied [tempDirectory] */ tempFile: File,
            /** Destination location for the file */ destinationFile: File
        ) -> Unit
    ) {
        internal fun pushLibrary(targetPackage: String, abi: String): String {
            val libFileName = "libtracing_perfetto.so"

            val shellWriteableAppReadableDir = File("/sdcard/Android/media/$targetPackage/files")
            val dstDir = shellWriteableAppReadableDir.resolve("lib/$abi")
            val dstFile = dstDir.resolve(libFileName)
            val tmpFile = tempDirectory.resolve(".tmp_$libFileName")

            val rxLibPathInsideZip = Regex(".*(lib|jni)/[^/]*$abi[^/]*/$libFileName")

            val zipFile = ZipFile(libraryZip)
            val entry = zipFile
                .entries()
                .asSequence()
                .firstOrNull { it.name.matches(rxLibPathInsideZip) }
                ?: throw IllegalStateException(
                    "Unable to locate $libFileName to enable Perfetto SDK. " +
                        "Tried inside ${libraryZip.path}."
                )

            zipFile.getInputStream(entry).use { inputStream ->
                tmpFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            moveTempDirectoryFileToDestination(tmpFile, dstFile)
            return dstFile.path
        }
    }

    private fun getDeviceAbi(): String =
        executeShellCommand("getprop ro.product.cpu.abilist").split(",")
            .plus(executeShellCommand("getprop ro.product.cpu.abi"))
            .first()
            .trim()

    @RestrictTo(LIBRARY_GROUP)
    public object RequestKeys {
        public const val RECEIVER_CLASS_NAME: String = "androidx.tracing.perfetto.TracingReceiver"

        /**
         * Request to enable tracing.
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

        /** Path to tracing native binary file (optional). */
        public const val KEY_PATH: String = "path"
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
         * This most likely means that the app does not expose a [PerfettoHandshake] compatible
         * receiver.
         */
        public const val RESULT_CODE_CANCELLED: Int = 0

        public const val RESULT_CODE_SUCCESS: Int = 1
        public const val RESULT_CODE_ALREADY_ENABLED: Int = 2

        /**
         * Required version described in [EnableTracingResponse.requiredVersion].
         * A follow-up [enableTracing] request expected with [ExternalLibraryProvider] specified.
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
        public val requiredVersion: String?,
        public val message: String?
    )
}
