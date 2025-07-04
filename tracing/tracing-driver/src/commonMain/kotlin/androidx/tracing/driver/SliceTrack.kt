/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing.driver

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

/**
 * Horizontal track of time in a trace which contains slice events (`beginSection` / `endSection`).
 */
public abstract class SliceTrack(
    /** The [TraceContext] instance. */
    context: TraceContext,
    /**
     * The uuid for the track descriptor.
     *
     * This ID must be unique within all [Track]s in a given trace produced by [TraceDriver] - it is
     * used to connect recorded trace events to the containing track.
     */
    uuid: Long,
) : Track(context = context, uuid = uuid) {

    /**
     * Writes a trace message indicating that a given section of code has begun.
     *
     * Should be followed by a corresponding call to [endSection] on the same [SliceTrack]. If a
     * corresponding [endSection] is missing, the section will be present in the trace, but
     * non-terminating (generally shown as fading out to the left).
     *
     * @param name The name of the code section to appear in the trace.
     */
    public open fun beginSection(name: String) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setBeginSection(uuid, name) }
        }
    }

    /**
     * Writes a trace message indicating that a given section of code has begun.
     *
     * Should be followed by a corresponding call to [endSection] on the same [SliceTrack]. If a
     * corresponding [endSection] is missing, the section will be present in the trace, but
     * non-terminating (generally shown as fading out to the left).
     *
     * @param name The name of the code section to appear in the trace.
     * @param flowIds A list of [Long]s which will connect this trace section to other sections in
     *   the trace, potentially on different Tracks. The start and end of each trace `flow`
     *   (connection) between trace sections must share an ID, so each `Long` must be unique to each
     *   `flow` in the trace.
     */
    public open fun beginSection(name: String, flowIds: List<Long>) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setBeginSectionWithFlows(uuid, name, flowIds) }
        }
    }

    /**
     * Writes a trace message indicating that a given section of code has ended.
     *
     * Must be preceded by a corresponding call to [beginSection] on the same [SliceTrack]. Any
     * un-paired calls to [endSection] are ignored when the trace is displayed.
     */
    public open fun endSection() {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setEndSection(uuid) }
        }
    }

    /**
     * Writes a zero duration section to the [SliceTrack].
     *
     * Similar to calling:
     * ```
     * beginSection(name)
     * endSection()
     * ```
     *
     * Except it is faster to write, and guaranteed zero duration.
     */
    public open fun instant(name: String) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setInstant(uuid, name) }
        }
    }

    /**
     * Traces the [block] as a named section of code in the trace - this is one of the primary entry
     * points for tracing synchronous blocks of code.
     */
    public inline fun <T> trace(name: String, crossinline block: () -> T): T {
        beginSection(name)
        try {
            return block()
        } finally {
            endSection()
        }
    }

    /** [Track] scoped async trace slices. */
    public suspend fun <T> traceFlow(
        name: String,
        flowId: Long = monotonicId(),
        block: suspend () -> T,
    ): T {
        return if (!context.isEnabled) {
            block()
        } else {
            traceFlow(name = name, flowIds = listOf(flowId), block = block)
        }
    }

    /** [Track] scoped async trace slices. */
    private suspend fun <T, R : Track> R.traceFlow(
        name: String,
        flowIds: List<Long>,
        block: suspend () -> T,
    ): T {
        val element = obtainFlowContext()
        val newFlowIds =
            if (element == null) {
                flowIds
            } else {
                element.flowIds + flowIds
            }
        val newElement = FlowContextElement(flowIds = newFlowIds)
        return withContext(coroutineContext + newElement) {
            beginSection(name = name, flowIds = newFlowIds)
            try {
                block()
            } finally {
                endSection()
            }
        }
    }
}
