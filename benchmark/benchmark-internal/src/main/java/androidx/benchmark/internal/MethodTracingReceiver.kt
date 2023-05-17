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

package androidx.benchmark.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.benchmark.internal.MethodTracingApi21.getFirstMountedMediaDir
import java.io.File

/**
 * Used to toggle method tracing on the application.
 */
class MethodTracingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val intentAction = intent?.action ?: ""
        if (intentAction != ACTION_METHOD_TRACE) {
            val message = "Unknown action $intentAction"
            Log.w(TAG, message)
            resultCode = RESULT_CODE_FAILED
            return
        }
        methodTracingRequest(context, intent)
    }

    private fun methodTracingRequest(context: Context, intent: Intent?) {
        val packageName = context.packageName
        val extras = intent?.extras
        val action = extras?.getString(ACTION)
        val fileName = extras?.getString(UNIQUE_NAME)
        if (action.isNullOrBlank()) {
            Log.d(TAG, "Unknown action.")
            resultCode = RESULT_CODE_FAILED
            return
        }
        when (action) {
            METHOD_TRACE_START_SAMPLED -> {
                require(!fileName.isNullOrBlank()) {
                    Log.d(TAG, "Missing output file name")
                    resultCode = RESULT_CODE_FAILED
                    return
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    MethodTracingApi21.startMethodTracingSampling(
                        tracePath = outputTracePath(context, fileName),
                        bufferSize = BUFFER_SIZE_BYTES,
                        intervalUs = INTERVAL_MICRO_SECONDS
                    )
                } else {
                    // Fallback
                    startMethodTracing(fileName, context)
                }
            }

            METHOD_TRACE_START -> {
                startMethodTracing(fileName, context)
            }

            METHOD_TRACE_END -> {
                Log.d(TAG, "Stopping method tracing for $packageName")
                Debug.stopMethodTracing()
                resultCode = RESULT_CODE_SUCCESS
            }

            else -> {
                Log.w(TAG, "Unknown request: $intent")
                resultCode = RESULT_CODE_FAILED
            }
        }
    }

    private fun startMethodTracing(fileName: String?, context: Context) {
        require(!fileName.isNullOrBlank()) {
            Log.d(TAG, "Missing output file name")
            resultCode = RESULT_CODE_FAILED
            return
        }
        Debug.startMethodTracing(
            outputTracePath(context, fileName),
            BUFFER_SIZE_BYTES
        )
        Log.d(TAG, "Enabling method tracing on ${context.packageName} ($fileName)")
        resultCode = RESULT_CODE_SUCCESS
    }

    companion object {
        private const val TAG = "MethodTracingReceiver"

        // Intents
        private const val ACTION_METHOD_TRACE = "androidx.benchmark.experiments.ACTION_METHOD_TRACE"

        // Extras
        private const val ACTION = "ACTION"
        private const val UNIQUE_NAME = "UNIQUE_NAME"

        // Actions
        private const val METHOD_TRACE_START = "METHOD_TRACE_START"
        private const val METHOD_TRACE_START_SAMPLED = "METHOD_TRACE_START_SAMPLED"
        private const val METHOD_TRACE_END = "ACTION_METHOD_TRACE_END"

        // Result codes
        private const val RESULT_CODE_FAILED = 0
        private const val RESULT_CODE_SUCCESS = 10

        // Tracing
        internal fun outputTracePath(context: Context, fileName: String): String {
            val basePath = if (Build.VERSION.SDK_INT >= 21) {
                context.getFirstMountedMediaDir()
            } else {
                null
            }
            val file = File(basePath, fileName)
            // Delete file if already exists.
            file.delete()
            return file.absolutePath
        }

        // The maximum size of a trace.
        // Setting this to 1G because that ought to be enough.
        private const val BUFFER_SIZE_BYTES = 1024 * 1024 * 1024 // bytes

        // The sampling internal. Set to 1/10th of a millisecond.
        private const val INTERVAL_MICRO_SECONDS = 1000 // micro-seconds
    }
}

@RequiresApi(21)
internal object MethodTracingApi21 {
    @DoNotInline
    fun startMethodTracingSampling(
        tracePath: String,
        bufferSize: Int,
        intervalUs: Int
    ) {
        Debug.startMethodTracingSampling(tracePath, bufferSize, intervalUs)
    }

    @DoNotInline
    @Suppress("DEPRECATION")
    fun Context.getFirstMountedMediaDir(): File? {
        // This is identical to the snippet from Outputs.dirUsableByAppAndShell.
        // This is the only way to write a trace that can be subsequently copied cleanly
        // by the Shell.
        return externalMediaDirs.firstOrNull {
            Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
        }
    }
}
