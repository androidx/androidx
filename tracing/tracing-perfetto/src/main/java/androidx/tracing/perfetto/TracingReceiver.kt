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
import android.os.Build
import android.util.JsonWriter
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.tracing.perfetto.PerfettoSdkTrace.Response
import androidx.tracing.perfetto.StartupTracingConfigStore.store
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.ACTION_DISABLE_TRACING_COLD_START
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.ACTION_ENABLE_TRACING_COLD_START
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.KEY_PATH
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.KEY_PERSISTENT
import androidx.tracing.perfetto.internal.handshake.protocol.Response
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseKeys
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_ERROR_OTHER
import androidx.tracing.perfetto.internal.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import java.io.File
import java.io.StringWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Allows for enabling tracing in an app using a broadcast. @see [ACTION_ENABLE_TRACING] */
@RestrictTo(LIBRARY)
class TracingReceiver : BroadcastReceiver() {
    private val executor by lazy {
        ThreadPoolExecutor(
            /* corePoolSize = */ 0,
            /* maximumPoolSize = */ 1,
            /* keepAliveTime = */ 10, // gives time for tooling to side-load the .so file
            /* unit = */ TimeUnit.SECONDS,
            /* workQueue = */ LinkedBlockingQueue()
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action !in listOf(
                ACTION_ENABLE_TRACING,
                ACTION_ENABLE_TRACING_COLD_START,
                ACTION_DISABLE_TRACING_COLD_START
            )
        ) return

        // Path to the provided library binary file (optional). If not provided, local library files
        // will be used if present.
        val srcPath = intent.extras?.getString(KEY_PATH)

        val pendingResult = goAsync()
        executor.execute {
            try {
                val response = when (intent.action) {
                    ACTION_ENABLE_TRACING -> enableTracingImmediate(srcPath, context)
                    ACTION_ENABLE_TRACING_COLD_START ->
                        enableTracingColdStart(
                            context,
                            srcPath,
                            intent.extras?.getString(KEY_PERSISTENT).toBoolean()
                        )
                    ACTION_DISABLE_TRACING_COLD_START -> disableTracingColdStart(context)
                    else -> throw IllegalStateException() // supported actions checked earlier
                }

                pendingResult.setResult(response.resultCode, response.toJsonString(), null)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Enables Perfetto SDK tracing in the app
     */
    private fun enableTracingImmediate(srcPath: String?, context: Context?): Response =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                // TODO(234351579): Support API < 30
                Response(
                    RESULT_CODE_ERROR_OTHER,
                    "SDK version not supported. Current minimum SDK = ${Build.VERSION_CODES.R}"
                )
            }
            srcPath != null && context != null -> {
                try {
                    PerfettoSdkTrace.enable(File(srcPath), context)
                } catch (e: Exception) {
                    Response(RESULT_CODE_ERROR_OTHER, e)
                }
            }
            srcPath != null && context == null -> {
                Response(
                    RESULT_CODE_ERROR_OTHER,
                    "Cannot copy source file: $srcPath without access to a Context instance."
                )
            }
            else -> {
                // Library path was not provided, trying to resolve using app's local library files.
                PerfettoSdkTrace.enable()
            }
        }

    /**
     * Handles [ACTION_ENABLE_TRACING_COLD_START]
     *
     * See [ACTION_ENABLE_TRACING_COLD_START] documentation for steps required before and after.
     */
    private fun enableTracingColdStart(
        context: Context?,
        srcPath: String?,
        isPersistent: Boolean
    ): Response = enableTracingImmediate(srcPath, context).also {
        if (it.resultCode == RESULT_CODE_SUCCESS) {
            val config = StartupTracingConfig(libFilePath = srcPath, isPersistent = isPersistent)
            if (context == null) return Response(
                RESULT_CODE_ERROR_OTHER,
                "Cannot set up cold start tracing without a Context instance."
            )
            config.store(context)
        }
    }

    private fun disableTracingColdStart(context: Context?): Response = when {
        context != null -> {
            StartupTracingConfigStore.clear(context)
            Response(RESULT_CODE_SUCCESS)
        }
        else ->
            Response(
                RESULT_CODE_ERROR_OTHER,
                "Cannot ensure we can disable cold start tracing without access to an app Context" +
                    " instance"
            )
    }

    private fun Response.toJsonString(): String {
        val output = StringWriter()

        JsonWriter(output).use {
            it.beginObject()

            it.name(ResponseKeys.KEY_RESULT_CODE)
            it.value(resultCode)

            it.name(ResponseKeys.KEY_REQUIRED_VERSION)
            it.value(requiredVersion)

            message?.let { msg ->
                it.name(ResponseKeys.KEY_MESSAGE)
                it.value(msg)
            }

            it.endObject()
        }

        return output.toString()
    }
}
