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
import kotlin.concurrent.Volatile
import kotlinx.coroutines.currentCoroutineContext

/** @return the [ProcessTrack] for the current process. */
internal expect inline fun TraceContext.currentProcessTrack(): ProcessTrack

// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@RestrictTo(Scope.LIBRARY_GROUP)
public class PerfettoTracer(context: TraceContext) : Tracer(isEnabled = context.isEnabled) {
    // The process track
    @JvmField internal var process: ProcessTrack = context.currentProcessTrack()

    @JvmField @Volatile internal var l1ThreadTrack: ThreadTrack? = null
    @JvmField @Volatile internal var l2ThreadTrack: ThreadTrack? = null

    // We have a small cache of ThreadTracks here. This is because in particularly hot code
    // on the same thread, or in suspending contexts where a lot of the work ends up happening on
    // the same dispatcher, we can avoid looking up a map for the last used thread tracks. So we
    // maintain the 2 most recently used ThreadTracks.
    /** @return The [ThreadTrack] instance based on the current execution context. */
    @Suppress("NOTHING_TO_INLINE", "DEPRECATION")
    internal inline fun currentThreadTrack(): ThreadTrack {
        val current = Thread.currentThread()
        val id = current.id.toInt()
        val l1 = l1ThreadTrack
        val l2 = l2ThreadTrack
        return when {
            l1 != null && l1.id == id -> l1
            l2 != null && l2.id == id -> l2
            else -> {
                var track = process.threads[id]
                if (track == null) {
                    track = process.getOrCreateThreadTrack(id = id, name = current.name)
                    l2ThreadTrack = l1ThreadTrack
                    l1ThreadTrack = track
                }
                track
            }
        }
    }

    // Testing API
    @RestrictTo(Scope.LIBRARY_GROUP)
    public fun resetFillCount() {
        currentThreadTrack().resetFillCount()
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
    override suspend fun tokenFromCoroutineContext():
        PlatformThreadContextElement<*, PerfettoTracer> {
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
            val track = currentThreadTrack()
            track.beginSection(
                category = category,
                name = name,
                token = PropagationUnsupportedToken,
            )
        } else {
            @Suppress("UNCHECKED_CAST")
            val parent =
                token as? PlatformThreadContextElement<*, PerfettoTracer>
                    ?: throw IllegalArgumentException("Unsupported token type $token")
            val track = currentThreadTrack()
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
                        token as? PlatformThreadContextElement<*, PerfettoTracer>
                            ?: throw IllegalArgumentException("Unsupported token type $token")
                    inheritedCoroutinePropagationToken(parent = parent, tracer = this)
                }
            tokenElement.name = name
            tokenElement.category = category
            val track = tokenElement.tracer.currentThreadTrack()
            track.beginCoroutineSection(category = category, name = name, token = tokenElement)
        }
    }

    override fun counter(category: String, name: String): Counter {
        // getOrCreateCounterTrack() is synchronized, so we get the same instance of the counter
        // for the provided name.
        val counter = process.counters.getOrPut(name) { process.getOrCreateCounterTrack(name) }
        return PerfettoCounter(category = category, track = counter)
    }

    @DelicateTracingApi
    override fun instant(category: String, name: String): EventMetadataCloseable {
        val track = currentThreadTrack()
        return track.instant(category = category, name = name)
    }
}
