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

import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.tooling.RecompositionTracer
import androidx.compose.runtime.tracing.stack.Stack
import androidx.tracing.EventMetadata
import androidx.tracing.ExperimentalContextPropagation
import androidx.tracing.Tracer

// The category being used for Composition/Recomposition tracing.
internal const val COMPOSE_TRACING_CATEGORY = "androidx.compose"

@OptIn(InternalComposeTracingApi::class, ExperimentalContextPropagation::class)
internal class ComposeTracer(private val tracer: Tracer) :
    RecompositionTracer.TraceCollector, CompositionTracer {
    @JvmField val closeables = Stack<AutoCloseable>()

    override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
        closeables +=
            tracer.beginSection(COMPOSE_TRACING_CATEGORY, info, token = null, metadataBlock = {})
    }

    override fun traceEventEnd() {
        endSection()
    }

    override fun isTraceInProgress(): Boolean = tracer.isCategoryEnabled(COMPOSE_TRACING_CATEGORY)

    override fun beginSection(sectionName: String, flowIds: List<Long>) {
        if (!isEnabled()) return
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
