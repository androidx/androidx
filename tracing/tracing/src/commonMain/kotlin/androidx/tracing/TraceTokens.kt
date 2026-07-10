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

import kotlin.coroutines.CoroutineContext

@Suppress("NOTHING_TO_INLINE")
internal inline fun CoroutineContext.platformThreadContextElement(): PlatformThreadContextElement? {
    return this[PlatformThreadContextElement.KEY]
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun inheritedPropagationToken(
    parent: PlatformThreadContextElement?,
    tracer: PerfettoTracer,
): PlatformThreadContextElement {
    val token =
        buildPropagationElement(
            // Placeholder to be filled in by beginSection* APIs.
            // Start off with the parent category and names so we have something consistent
            // when using the PlatformThreadContextElement for explicit trace propagation.
            tracer = tracer,
            category = parent?.category ?: DEFAULT_STRING,
            name = parent?.name ?: DEFAULT_STRING,
            flowIds = parent?.flowIds ?: listOf(monotonicId()),
        )
    return token
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun inheritedCoroutinePropagationToken(
    parent: PlatformThreadContextElement?,
    tracer: PerfettoTracer,
): PlatformThreadContextElement {
    val token =
        buildCoroutinePropagationElement(
            // Placeholder to be filled in by beginSection* APIs.
            // Start off with the parent category and names so we have something consistent
            // when using the PlatformThreadContextElement for explicit trace propagation.
            tracer = tracer,
            category = parent?.category ?: DEFAULT_STRING,
            name = parent?.name ?: DEFAULT_STRING,
            flowIds = parent?.flowIds ?: listOf(monotonicId()),
        )
    return token
}
