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

package androidx.benchmark.perfetto

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.benchmark.BenchmarkState
import androidx.benchmark.Shell

@RestrictTo(Scope.LIBRARY_GROUP)
public data class Response(
    val code: Int,
    val data: String? = null,
    val throwable: Throwable? = null,
) {
    fun isSuccess() = code > 0

    fun isFailure() = code <= 0
}

/**
 * Can be used to enable, and disable in-process tracing for a target application.
 *
 * @param targetPackage The target application id.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class ConnectedProfilerTracing(private val targetPackage: String) {
    /** Enables in-process tracing on the target package. */
    public fun enable(): Response {
        Log.d(BenchmarkState.TAG, "Enabling in-process tracing")
        return sendBroadcast(ACTION_START)
    }

    /** Disables in-process tracing on the target package. */
    public fun disable(): Response {
        Log.d(BenchmarkState.TAG, "Disabling in-process tracing")
        return sendBroadcast(ACTION_STOP)
    }

    /** Requests that the traces are flushed. */
    public fun flush(): Response {
        Log.d(BenchmarkState.TAG, "Flushing in-process traces")
        return sendBroadcast(ACTION_FLUSH_TRACES_GET_PATH)
    }

    internal fun sendBroadcast(action: String): Response {
        return runCatching {
                val command = "-a $action $targetPackage/$RECEIVER_NAME"
                val output = Shell.amBroadcast(command)
                return parseResponse(output)
            }
            .getOrElse { throwable -> Response(code = RESPONSE_CODE_FAILED, throwable = throwable) }
    }

    internal fun parseResponse(response: Pair<Int?, String?>): Response {
        val code = response.first ?: RESPONSE_CODE_FAILED
        val data = response.second
        return Response(code = code, data = data)
    }

    companion object {
        internal const val RECEIVER_NAME =
            "androidx.tracing.profiler.ConnectedProfilerTracingReceiver"

        // Starts a tracing session.
        internal const val ACTION_START = "androidx.tracing.profiler.action.START"

        // Flushes traces.
        internal const val ACTION_FLUSH_TRACES_GET_PATH =
            "androidx.tracing.profiler.action.FLUSH_TRACES_GET_PATH"

        // Stops a tracing session.
        internal const val ACTION_STOP = "androidx.tracing.profiler.action.STOP"

        // Response codes
        internal const val RESPONSE_CODE_FAILED = 0
    }
}
