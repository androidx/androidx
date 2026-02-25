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

package androidx.datastore.core

import android.annotation.SuppressLint
import androidx.tracing.DelicateTracingApi

/**
 * Actual definition for the Tracer.
 *
 * On Android, this will be typealiased to androidx.tracing.Tracer. On other platforms, this will be
 * a No-Op class.
 */
internal actual typealias DataStoreTracer = androidx.tracing.Tracer

/**
 * Actual definition for the Token. On Android, this will be typealiased to
 * androidx.tracing.PropagationToken.
 *
 * It is No-Op on all other platforms.
 */
internal actual typealias DataStoreTraceToken = androidx.tracing.PropagationToken

/**
 * Wrapper to execute a block with tracing. If [tracer] is null (or on non-Android), this simply
 * executes [block].
 */
internal actual suspend fun <R> trace(
    tracer: DataStoreTracer?,
    name: String,
    token: DataStoreTraceToken?,
    block: suspend () -> R,
): R {
    return if (tracer != null) {
        if (token != null) {
            tracer.traceCoroutine(name = name, category = TRACE_CATEGORY, token = token) { block() }
        } else {
            tracer.traceCoroutine(name = name, category = TRACE_CATEGORY) { block() }
        }
    } else {
        block()
    }
}

@SuppressLint("NullAnnotationGroup")
@OptIn(DelicateTracingApi::class)
internal actual suspend fun captureTraceToken(tracer: DataStoreTracer?): DataStoreTraceToken? {
    return tracer?.tokenFromCoroutineContext()
}

private const val TRACE_CATEGORY = "Jetpack DataStore"
