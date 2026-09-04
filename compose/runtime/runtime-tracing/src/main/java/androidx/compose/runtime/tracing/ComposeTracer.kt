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

import android.os.Looper
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.RecompositionTracer
import androidx.compose.runtime.tracing.collections.Stack
import androidx.tracing.EventMetadata
import androidx.tracing.ExperimentalContextPropagation
import androidx.tracing.Tracer

// The category being used for Composition/Recomposition tracing.
internal const val COMPOSE_TRACING_CATEGORY = "androidx.compose"

@OptIn(InternalComposeTracingApi::class, ExperimentalContextPropagation::class)
internal class ComposeTracer(private val tracer: Tracer) :
    RecompositionTracer.TraceCollector, CompositionTracer {
    @JvmField val stackMap = mutableLongObjectMapOf<Stack<AutoCloseable>>()
    @JvmField
    internal val mainStack: Stack<AutoCloseable> = Stack(tid = Looper.getMainLooper().thread.id)
    @JvmField @Volatile internal var l1Stack: Stack<AutoCloseable>? = null
    @JvmField @Volatile internal var l2Stack: Stack<AutoCloseable>? = null

    // Note: We are doing this to make sure our usage of Stacks are threadsafe,
    // even though in practice most of the usage is confined to a UI thread. Some
    // advanced use-cases support composition off main thread, and in order to
    // keep supporting composition tracing (while maintaining a low overhead) we
    // have to use a "thread local" like implementation. We have a similar
    // implementation in tracing with benchmarks to prove that the overhead here
    // is minimal.
    @Suppress("NOTHING_TO_INLINE", "DEPRECATION")
    internal inline fun currentThreadStack(): Stack<AutoCloseable> {
        val current = Thread.currentThread()
        val tid = current.id
        val l1 = l1Stack
        val l2 = l2Stack
        return when {
            // Guarantee a no contention slot for the UI thread.
            tid == mainStack.tid -> mainStack
            l1 != null && l1.tid == tid -> l1
            // Ideally, we could switch l2 with l1, but there is no real
            // benefit (perf-wise) in doing so.
            l2 != null && l2.tid == tid -> l2
            else -> currentThreadTrackSlow(tid)
        }
    }

    internal fun currentThreadTrackSlow(tid: Long): Stack<AutoCloseable> {
        return synchronized(stackMap) {
            val stack =
                stackMap.getOrPut(tid) {
                    Stack(tid = tid)
                }
            l2Stack = l1Stack
            l1Stack = stack
            stack
        }
    }

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        if (!isEnabled()) return
        val closeables = currentThreadStack()
        closeables +=
            tracer.beginSection(COMPOSE_TRACING_CATEGORY, info, token = null, metadataBlock = {})
    }

    override fun traceEventEnd() {
        endSection()
    }

    override fun isTraceInProgress(): Boolean = tracer.isCategoryEnabled(COMPOSE_TRACING_CATEGORY)

    override fun beginSection(sectionName: String, flowIds: List<Long>) {
        if (!isEnabled()) return
        val closeables = currentThreadStack()
        closeables +=
            tracer.beginSection(
                COMPOSE_TRACING_CATEGORY,
                sectionName,
                tracer.tokenForManualPropagation(flowIds),
                metadataBlock = {},
            )
    }

    override fun endSection() {
        if (!isEnabled()) return
        val closeables = currentThreadStack()
        closeables.removeLastOrNull()?.close()
    }

    override fun instantEvent(
        sectionName: String,
        stackTrace: List<StackTraceElement>,
        id: Int,
        flowIds: List<Long>,
    ) {
        if (!isEnabled()) return
        tracer.instant(
            COMPOSE_TRACING_CATEGORY,
            sectionName,
            tracer.tokenForManualPropagation(flowIds),
        ) {
            addStackTrace(stackTrace)
            addCorrelationId(id.toLong())
        }
    }

    override fun isEnabled(): Boolean = isTraceInProgress()

    private fun EventMetadata.addStackTrace(stackTrace: List<StackTraceElement>) {
        stackTrace.forEach {
            addCallStackEntry(
                name =
                    buildString(it.className.length + it.methodName.length + 1) {
                        append(it.className)
                        append(".")
                        append(it.methodName)
                    },
                sourceFile = it.fileName,
                lineNumber = it.lineNumber,
            )
        }
    }
}
