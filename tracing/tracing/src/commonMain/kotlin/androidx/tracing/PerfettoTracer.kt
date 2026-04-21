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

package androidx.tracing

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import kotlinx.coroutines.currentCoroutineContext

/** @return the [ProcessTrack] for the current process. */
internal expect inline fun TraceContext.currentProcessTrack(): ProcessTrack

@RestrictTo(Scope.LIBRARY_GROUP)
public class PerfettoTracer(context: TraceContext) : Tracer(isEnabled = context.isEnabled) {
    // The process track
    @JvmField internal var process: ProcessTrack = context.currentProcessTrack()

    // Testing API
    @RestrictTo(Scope.LIBRARY_GROUP)
    public fun resetTraceEvents() {
        process.currentThreadTrack().resetTraceEvents()
    }

    // Testing API
    @RestrictTo(Scope.LIBRARY_GROUP)
    public fun enqueueSingleUnmodifiedEvent() {
        process.enqueueSingleUnmodifiedEvent()
    }

    @DelicateTracingApi
    override fun tokenFromThreadContext(): PropagationToken {
        return PropagationUnsupportedToken
    }

    @ExperimentalContextPropagation
    override fun tokenForManualPropagation(): PropagationToken {
        return inheritedPropagationToken(parent = null, tracer = this)
    }

    @DelicateTracingApi
    override suspend fun tokenFromCoroutineContext(): PlatformThreadContextElement {
        val parent = currentCoroutineContext().platformThreadContextElement()
        val current = inheritedCoroutinePropagationToken(parent = parent, tracer = this)
        return current
    }

    @DelicateTracingApi
    override fun beginSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable {
        // Out of the box we don't support propagation at all outside of suspending contexts.
        return if (token == null || token == PropagationUnsupportedToken) {
            val track = process.currentThreadTrack()
            track.beginSection(
                category = category,
                name = name,
                token = PropagationUnsupportedToken,
            )
        } else {
            @Suppress("UNCHECKED_CAST")
            val parent =
                token as? PlatformThreadContextElement
                    ?: throw IllegalArgumentException("Unsupported token type $token")
            val track = process.currentThreadTrack()
            val tokenElement = inheritedPropagationToken(parent = parent, tracer = this)
            track.beginCoroutineSection(category = category, name = name, token = tokenElement)
        }
    }

    @DelicateTracingApi
    override suspend fun beginCoroutineSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken?,
        isRoot: Boolean,
    ): EventMetadataCloseable {
        return if (token == PropagationUnsupportedToken) {
            val eventMetadataCloseable =
                beginSectionWithMetadata(
                    category = category,
                    name = name,
                    token = PropagationUnsupportedToken,
                    isRoot = isRoot,
                )
            eventMetadataCloseable
        } else {
            val tokenElement =
                if (token == null) {
                    // Context Propagation is implicit here.
                    // When context propagation is implicit, don't re-use flowIds from the
                    // CoroutineContext. Instead, allocate a new flowId for every child coroutine
                    // unless explicit propagation tokens are used.
                    inheritedCoroutinePropagationToken(parent = null, tracer = this)
                } else {
                    // Context Propagation is explicit.
                    @Suppress("UNCHECKED_CAST")
                    val parent =
                        token as? PlatformThreadContextElement
                            ?: throw IllegalArgumentException("Unsupported token type $token")
                    inheritedCoroutinePropagationToken(parent = parent, tracer = this)
                }
            tokenElement.name = name
            tokenElement.category = category
            val track = tokenElement.tracer.process.currentThreadTrack()
            track.beginCoroutineSection(category = category, name = name, token = tokenElement)
        }
    }

    override fun counter(category: String, name: String): Counter {
        // getOrCreateCounterTrack() is synchronized, so we get the same instance of the counter
        // for the provided name.
        return process.getOrCreateCounterTrack(name)
    }

    @DelicateTracingApi
    override fun instant(category: String, name: String): EventMetadataCloseable {
        val track = process.currentThreadTrack()
        return track.instant(category = category, name = name)
    }
}
