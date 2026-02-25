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

@file:JvmName("TraceTokens")

package androidx.tracing

import androidx.tracing.PlatformThreadContextElement.Companion.STATE_BEGIN
import androidx.tracing.PlatformThreadContextElement.Companion.STATE_END
import kotlin.coroutines.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
internal inline fun CoroutineContext.platformThreadContextElement():
    PlatformThreadContextElement<*, PerfettoTracer>? {
    // This is a safe thing to do, given `PlatformThreadContextElement` is always `PerfettoTracer`
    // aware.
    @Suppress("UNCHECKED_CAST")
    return this[PlatformThreadContextElement.KEY]
        as? PlatformThreadContextElement<*, PerfettoTracer>
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun inheritedPropagationToken(
    parent: PlatformThreadContextElement<*, PerfettoTracer>?,
    tracer: PerfettoTracer,
): PlatformThreadContextElement<*, PerfettoTracer> {
    val token =
        buildThreadContextElement(
            // Placeholder to be filled in by beginSection* APIs.
            // Start off with the parent category and names so we have something consistent
            // when using the PlatformThreadContextElement for explicit trace propagation.
            tracer = tracer,
            category = parent?.category ?: DEFAULT_STRING,
            name = parent?.name ?: DEFAULT_STRING,
            flowIds = parent?.flowIds ?: listOf(monotonicId()),
            updateThreadContextBlock = {}, // Not used
            restoreThreadContextBlock = {}, // Not used
            close = { element ->
                if (
                    element.synchronizedCompareAndSet(expected = STATE_BEGIN, newValue = STATE_END)
                ) {
                    element.tracer.currentThreadTrack().endSection()
                }
            },
        )
    return token
}

@Suppress("NOTHING_TO_INLINE")
internal fun inheritedCoroutinePropagationToken(
    parent: PlatformThreadContextElement<*, PerfettoTracer>?,
    tracer: PerfettoTracer,
): PlatformThreadContextElement<*, PerfettoTracer> {
    val token =
        buildThreadContextElement(
            // Placeholder to be filled in by beginSection* APIs.
            // Start off with the parent category and names so we have something consistent
            // when using the PlatformThreadContextElement for explicit trace propagation.
            tracer = tracer,
            category = parent?.category ?: DEFAULT_STRING,
            name = parent?.name ?: DEFAULT_STRING,
            flowIds = parent?.flowIds ?: listOf(monotonicId()),
            // This method is called before a coroutine is resumed on a thread that
            // belongs to a dispatcher. This can be called more than once. So avoid creating
            // slices unless we transition to `STATE_END`.
            updateThreadContextBlock = { context: CoroutineContext ->
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
                        contextElement.tracer
                            .currentThreadTrack()
                            .beginCoroutineSection(
                                category = category,
                                name = name,
                                token = contextElement,
                            )
                    result.metadata.dispatchToTraceSink()
                }
            },
            // This method is called **after** a coroutine is suspended on the current thread.
            // This method might be called more than once as well. So we want to be
            // idempotent.
            restoreThreadContextBlock = { context ->
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
                    contextElement.tracer.currentThreadTrack().endSection()
                }
            },
            close = { platformThreadContextElement ->
                // Only close if the threadContextElement is still in STATE_BEGIN
                if (
                    platformThreadContextElement.synchronizedCompareAndSet(
                        expected = STATE_BEGIN,
                        newValue = STATE_END,
                    )
                ) {
                    platformThreadContextElement.tracer.currentThreadTrack().endSection()
                }
            },
        )
    return token
}
