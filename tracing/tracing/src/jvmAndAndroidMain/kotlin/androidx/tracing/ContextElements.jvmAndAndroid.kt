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

package androidx.tracing

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

internal class PropagationElement(
    override val tracer: PerfettoTracer,
    override var category: String,
    override var name: String,
    override val flowIds: List<Long>,
) :
    ThreadContextElement<Unit>,
    PlatformThreadContextElement(
        tracer = tracer,
        category = category,
        name = name,
        flowIds = flowIds,
    ) {
    override fun updateThreadContext(context: CoroutineContext) {
        // Do nothing
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {
        // Do nothing
    }
}

internal class CoroutinePropagationElement(
    override val tracer: PerfettoTracer,
    override var category: String,
    override var name: String,
    override val flowIds: List<Long>,
) :
    ThreadContextElement<Unit>,
    PlatformThreadContextElement(
        tracer = tracer,
        category = category,
        name = name,
        flowIds = flowIds,
    ) {
    // This method is called before a coroutine is resumed on a thread that
    // belongs to a dispatcher. This can be called more than once. So avoid creating
    // slices unless we transition to `STATE_END`.
    override fun updateThreadContext(context: CoroutineContext) {
        val contextElement = context.platformThreadContextElement()
        val category = contextElement?.category
        val name = contextElement?.name
        if (
            contextElement != null &&
                category != null &&
                name != null &&
                contextElement.synchronizedCompareAndSet(
                    expected = STATE_END,
                    newValue = STATE_BEGIN,
                )
        ) {
            val result =
                contextElement.tracer.process
                    .currentThreadTrack()
                    .beginCoroutineSection(category = category, name = name, token = contextElement)
            result.metadata.dispatchToTraceSink()
        }
    }

    // This method is called **after** a coroutine is suspended on the current thread.
    // This method might be called more than once as well. So we want to be
    // idempotent.
    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {
        val contextElement = context.platformThreadContextElement()
        val name = contextElement?.name
        if (
            contextElement != null &&
                name != null &&
                contextElement.synchronizedCompareAndSet(
                    expected = STATE_BEGIN,
                    newValue = STATE_END,
                )
        ) {
            contextElement.tracer.process.currentThreadTrack().endSection()
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun buildPropagationElement(
    tracer: PerfettoTracer,
    category: String,
    name: String,
    flowIds: List<Long>,
): PlatformThreadContextElement {
    return PropagationElement(tracer = tracer, category = category, name = name, flowIds = flowIds)
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun buildCoroutinePropagationElement(
    tracer: PerfettoTracer,
    category: String,
    name: String,
    flowIds: List<Long>,
): PlatformThreadContextElement {
    return CoroutinePropagationElement(
        tracer = tracer,
        category = category,
        name = name,
        flowIds = flowIds,
    )
}
