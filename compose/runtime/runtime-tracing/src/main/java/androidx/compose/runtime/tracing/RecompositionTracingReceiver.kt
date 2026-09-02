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

package androidx.compose.runtime.tracing

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.RecompositionTracer
import androidx.startup.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Receives broadcast intents to start and stop the [RecompositionTracer].
 *
 * [RecompositionTracer] is a higher overhead debugging tool that collects information about state
 * reads and writes that affect composition into a Perfetto trace.
 *
 * This receiver can be started at any point of time to start and stop the tracing. The
 * [RecompositionTracer] will automatically find all existing compositions and start observations
 * from there.
 *
 * Supported actions:
 * - [ACTION_START] - Starts recomposition tracing
 * - [ACTION_STOP] - Stops recomposition tracing
 *
 * Result codes:
 * - [RESULT_CODE_SUCCESS] - Successfully installed the tracer
 * - [RESULT_CODE_ALREADY_IN_PROGRESS] - Tracing is already in progress
 * - [RESULT_CODE_FAILURE] - Failed to install or stop the tracer
 */
public class RecompositionTracingReceiver : BroadcastReceiver() {
    /** Handles broadcast intents for starting or stopping recomposition tracing. */
    override fun onReceive(context: Context?, intent: Intent?) {
        val context = context ?: return
        val action = intent?.action ?: return

        when (action) {
            ACTION_START -> start(context)
            ACTION_STOP -> stop(context)
            else -> {
                Log.w("RecompositionTracer", "Unknown intent action: $action. Ignoring.")
            }
        }
    }

    @OptIn(InternalComposeTracingApi::class)
    private fun start(context: Context) {
        // Make sure the tracer is initialized
        try {
            AppInitializer.getInstance(context)
                .initializeComponent(ComposeTracingInitializer::class.java)
        } catch (e: Exception) {
            resultCode = RESULT_CODE_FAILURE
            throw e
        }

        val composeTraceSink = ComposeTracingInitializer.composeTraceSink
        if (composeTraceSink == null) {
            resultCode = RESULT_CODE_FAILURE
            error(
                "Expected Compose tracer to be initialized before starting tracing. Please report" +
                    " to Jetpack Compose team through https://goo.gle/compose-feedback"
            )
        }

        synchronized(lock) {
            if (tracingHandle == null) {
                val tracer = RecompositionTracer(composeTraceSink)
                tracingHandle = tracer.installTracing(tracingContext)
            } else {
                resultCode = RESULT_CODE_ALREADY_IN_PROGRESS
                return
            }
        }
        resultCode = RESULT_CODE_SUCCESS
    }

    private fun stop(context: Context) {
        synchronized(lock) {
            try {
                tracingHandle?.cancel()
                resultCode = RESULT_CODE_SUCCESS
            } catch (e: Exception) {
                resultCode = RESULT_CODE_FAILURE
                throw e
            } finally {
                tracingHandle = null
            }
        }
    }

    public companion object {
        // Result codes
        /** Successfully started or stopped recomposition tracing. */
        public const val RESULT_CODE_SUCCESS: Int = 1
        /** Did not start recomposition tracing. Tracing is already in progress. */
        public const val RESULT_CODE_ALREADY_IN_PROGRESS: Int = 2
        /** Failed to start or stop recomposition tracing. */
        public const val RESULT_CODE_FAILURE: Int = 3

        // Actions

        /** Start recomposition tracing. */
        // The lint is inapplicable here as we want to indicate recompose action as a separate
        // action.
        @SuppressLint("ActionValue")
        public const val ACTION_START: String =
            "androidx.compose.tracing.action.START_RECOMPOSE_TRACING"
        @SuppressLint("ActionValue")
        /** Stop recomposition tracing. */
        public const val ACTION_STOP: String =
            "androidx.compose.tracing.action.STOP_RECOMPOSE_TRACING"

        private val lock = Any()
        private val tracingContext = Dispatchers.Main + SupervisorJob()
        private var tracingHandle: CancellationHandle? = null
    }
}
