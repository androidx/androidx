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

package androidx.compose.runtime.tracing.internal

import android.content.Context
import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.RecompositionTracer
import androidx.compose.runtime.tracing.ComposeTracerInitializer
import androidx.startup.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Internal state for [androidx.compose.runtime.tooling.RecompositionTracer]. */
internal object RecompositionTracerState {
    private val lock = Any()
    private val tracingContext = Dispatchers.Main + SupervisorJob()
    private var tracingHandle: CancellationHandle? = null

    /**
     * Start tracing, returns `true` if tracing started successfully or `false` if it is already in
     * progress.
     */
    @OptIn(InternalComposeTracingApi::class)
    fun startTracing(context: Context): Boolean {
        synchronized(lock) {
            if (tracingHandle == null) {
                // Make sure the tracer is initialized
                val composeTraceCollector =
                    AppInitializer.getInstance(context)
                        .initializeComponent(ComposeTracerInitializer::class.java)
                val tracer = RecompositionTracer(composeTraceCollector)
                tracingHandle = tracer.installTracing(tracingContext)
                RecompositionTracingEnabledReceiver.enable(context)
                return true
            } else {
                return false
            }
        }
    }

    fun stopTracing(context: Context) {
        synchronized(lock) {
            try {
                tracingHandle?.cancel()
            } finally {
                RecompositionTracingEnabledReceiver.disable(context)
                tracingHandle = null
            }
        }
    }
}
