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

/** Entities that we can attach traces to to be visualized on the timeline with slices & flows. */
public abstract class EventTrack(
    /** The [TraceContext] instance. */
    context: TraceContext,
    /** `true` iff we need to emit some preamble packets. */
    hasPreamble: Boolean,
    /** The uuid for the track descriptor. */
    uuid: Long,
    /** The parent traceable. */
    parent: Track?
) : Track(context = context, hasPreamble = hasPreamble, uuid = uuid, parent = parent) {

    public fun beginPacket(name: String, flowIds: List<Long> = emptyList()): PooledTracePacket? {
        return if (!context.isEnabled) {
            null
        } else {
            trackBeginPacket(name, flowIds)
        }
    }

    public fun endPacket(name: String): PooledTracePacket? {
        return if (!context.isEnabled) {
            null
        } else {
            trackEndPacket(name)
        }
    }

    public fun emitInstantPacket() {
        if (context.isEnabled) {
            instantPacket()
        }
    }

    public inline fun <T> trace(name: String, crossinline block: () -> T): T {
        if (context.isEnabled) {
            val packet = beginPacket(name)
            if (packet != null) {
                emit(packet)
            }
        }
        try {
            return block()
        } finally {
            if (context.isEnabled) {
                val packet = endPacket(name)
                if (packet != null) {
                    emit(packet)
                }
            }
        }
    }

    /** [Track] scoped async trace slices. */
    public suspend fun <T> traceFlow(
        name: String,
        flowId: Long = monotonicId(),
        block: suspend () -> T
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
        block: suspend () -> T
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
            val begin = beginPacket(name = name, flowIds = newFlowIds)
            if (begin != null) {
                emit(begin)
            }
            try {
                block()
            } finally {
                val end = endPacket(name)
                if (end != null) {
                    emit(end)
                }
            }
        }
    }
}
