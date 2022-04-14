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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.tracing.perfetto.Tracing.EnableTracingResponse
import androidx.tracing.perfetto.TracingReceiver.Companion.ACTION_ENABLE_TRACING
import java.io.File

/** Allows for enabling tracing in an app using a broadcast. @see [ACTION_ENABLE_TRACING] */
@RestrictTo(LIBRARY)
class TracingReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Request to enable tracing.
         *
         * Request can include [KEY_PATH] as an optional extra.
         *
         * Response to the request is a JSON string (to allow for CLI support) with the following:
         * - [KEY_EXIT_CODE] (always)
         * - [KEY_REQUIRED_VERSION] (always)
         * - [KEY_ERROR_MESSAGE] (optional)
         */
        const val ACTION_ENABLE_TRACING = "androidx.tracing.perfetto.action.ENABLE_TRACING"

        /** Path to tracing native binary file (optional). */
        const val KEY_PATH = "path"

        /** Request exit code. */
        const val KEY_EXIT_CODE = "exitCode"

        /**
         * Required version of the binaries. Java and binary library versions have to match to
         * ensure compatibility.
         */
        const val KEY_REQUIRED_VERSION = "requiredVersion"

        /** Response error message if present. */
        const val KEY_ERROR_MESSAGE = "errorMessage"

        const val RESULT_CODE_SUCCESS = 1
        const val RESULT_CODE_ALREADY_ENABLED = 2
        const val RESULT_CODE_ERROR_BINARY_MISSING = 11
        const val RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH = 12
        const val RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR = 13
        const val RESULT_CODE_ERROR_OTHER = 99
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        RESULT_CODE_SUCCESS,
        RESULT_CODE_ALREADY_ENABLED,
        RESULT_CODE_ERROR_BINARY_MISSING,
        RESULT_CODE_ERROR_BINARY_VERSION_MISMATCH,
        RESULT_CODE_ERROR_BINARY_VERIFICATION_ERROR,
        RESULT_CODE_ERROR_OTHER
    )
    internal annotation class EnableTracingResultCode

    // TODO: check value on app start
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action != ACTION_ENABLE_TRACING) return

        // Path to the provided library binary file (optional). If not provided, local library files
        // will be used if present.
        val srcPath = intent.extras?.getString(KEY_PATH)

        val response: EnableTracingResponse = when {
            srcPath != null && context != null -> {
                try {
                    val dstFile = copyExternalLibraryFile(context, srcPath)
                    Tracing.enable(dstFile.path)
                } catch (e: Exception) {
                    EnableTracingResponse(
                        exitCode = RESULT_CODE_ERROR_OTHER,
                        errorMessage = e.toErrorMessage()
                    )
                }
            }
            srcPath != null && context == null -> {
                EnableTracingResponse(
                    exitCode = RESULT_CODE_ERROR_OTHER,
                    errorMessage = "Cannot copy source file: $srcPath without access to" +
                        " a Context instance."
                )
            }
            else -> {
                // Library path was not provided, trying to resolve using app's local library files.
                Tracing.enable(null)
            }
        }

        setResult(response.exitCode, response.toJsonString(), null)
    }

    private fun copyExternalLibraryFile(
        context: Context,
        srcPath: String
    ): File {
        // Prepare a location to copy the library into with the following properties:
        // 1) app has exclusive write access in
        // 2) app can load binaries from
        val abi: String = File(context.applicationInfo.nativeLibraryDir).name // e.g. arm64
        val dstDir = context.cacheDir.resolve("lib/$abi")
        dstDir.mkdirs()

        // Copy the library file over
        //
        // TODO: load into memory and verify in-memory to prevent from copying a malicious
        // library into app's local files. Use SHA or Signature to verify the binaries.
        val srcFile = File(srcPath)
        val dstFile = dstDir.resolve(srcFile.name)
        srcFile.copyTo(dstFile, overwrite = true)

        return dstFile
    }
}