/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.utility

import android.os.Build
import android.os.Trace
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Wrapper around [Trace.beginSection] and [Trace.endSection] which helps reduce boilerplate by
 * taking advantage of RAII like [Closeable] in a try block.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TraceEvent(traceName: String) : Closeable {
    init {
        Trace.beginSection(traceName)
    }

    public override fun close() {
        Trace.endSection()
    }
}

/**
 * Wrapper around [Trace.beginAsyncSection] which helps reduce boilerplate by taking advantage of
 * RAII like [Trace.endAsyncSection] in a try block, and by dealing with API version support.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AsyncTraceEvent(private val traceName: String) : Closeable {
    internal companion object {
        private val lock = Any()
        private var nextTraceId = 0

        internal fun getTraceId() = synchronized(lock) { nextTraceId++ }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private object Api29Impl {
        @JvmStatic
        @DoNotInline
        fun callBeginAsyncSection(traceName: String, traceId: Int) {
            Trace.beginAsyncSection(traceName, traceId)
        }

        @JvmStatic
        @DoNotInline
        fun callEndAsyncSection(traceName: String, traceId: Int) {
            Trace.endAsyncSection(traceName, traceId)
        }
    }

    private val traceId = getTraceId()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.callBeginAsyncSection(traceName, traceId)
        }
    }

    public override fun close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.callEndAsyncSection(traceName, traceId)
        }
    }
}

/** Wrapper around [CoroutineScope.launch] with an async trace event. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CoroutineScope.launchWithTracing(
    traceEventName: String,
    block: suspend CoroutineScope.() -> Unit
): Job = launch { TraceEvent(traceEventName).use { block.invoke(this) } }
