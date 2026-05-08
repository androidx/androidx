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

package androidx.tracing.profiler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.tracing.Trace.TAG
import androidx.tracing.profiler.ConnectedProfilerTracing.disableTracing
import androidx.tracing.profiler.ConnectedProfilerTracing.enableTracing
import androidx.tracing.wire.TraceDriver
import androidx.tracing.wire.getOrCreateTracesDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Actions
internal const val ACTION_START = "androidx.tracing.profiler.action.START"
internal const val FLUSH_TRACES_GET_PATH = "androidx.tracing.profiler.action.FLUSH_TRACES_GET_PATH"
internal const val ACTION_STOP = "androidx.tracing.action.profiler.STOP"

// Result codes
internal const val RESULT_CODE_NO_TRACE_DRIVER = -2
internal const val RESULT_CODE_SUCCESS = 1
internal const val RESULT_CODE_FLUSH_COMPLETED = 2

/**
 * The [BroadcastReceiver] that can be used flush in-process traces. Tracing 2.0 and TracingDriver
 * record in-process traces. To get these off of the device, or make them available to testing
 * harnesses such as `androidx.benchmark`, this receiver offers start/stop/flush to file actions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConnectedProfilerTracingReceiver : BroadcastReceiver() {

    // The scope used for all work done by the receiver.
    // Dispatchers.Default is okay, given the actual flush happens on Dispatchers.IO
    private val scope = CoroutineScope(context = Dispatchers.Default)

    override fun onReceive(context: Context?, intent: Intent?) {
        val context = context ?: return
        val action = intent?.action ?: return

        when (action) {
            ACTION_START -> start(context)
            ACTION_STOP -> stop(context)
            FLUSH_TRACES_GET_PATH -> flushTraces(context)
            else -> {
                Log.w(TAG, "Unknown intent action: $action. Ignoring.")
            }
        }
    }

    internal fun start(context: Context) {
        Log.w(TAG, "Starting an in-process tracing session.")
        enableTracing(context)
        resultCode = RESULT_CODE_SUCCESS
    }

    internal fun stop(context: Context) {
        Log.w(TAG, "Stopping an in-process tracing session.")
        disableTracing(context)
        resultCode = RESULT_CODE_SUCCESS
    }

    internal fun flushTraces(context: Context) {
        // Flushing traces should not take too long. By using goAsync(), we effectively have
        // 10 seconds to respond on the main thread before we risk a background ANR.
        val result = goAsync()
        scope.launch {
            try {
                val driver = TraceDriver.getTraceDriver(context)
                if (driver == null) {
                    result.resultCode = RESULT_CODE_NO_TRACE_DRIVER
                } else {
                    driver.flush()
                    result.resultCode = RESULT_CODE_FLUSH_COMPLETED
                    // The path to find traces in.
                    result.resultData = context.getOrCreateTracesDirectory().absolutePath
                    Log.d(TAG, "Flushed traces.")
                }
            } finally {
                result.finish()
            }
        }
    }
}
