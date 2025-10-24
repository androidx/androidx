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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.tracing.driver.PlatformThreadContextElement.Companion.STATE_BEGIN
import androidx.tracing.driver.PlatformThreadContextElement.Companion.STATE_END
import kotlin.concurrent.Volatile
import kotlinx.coroutines.currentCoroutineContext

/** @return the [ProcessTrack] for the current process. */
internal expect inline fun TraceContext.currentProcessTrack(): ProcessTrack

// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@RestrictTo(Scope.LIBRARY_GROUP)
public class PerfettoTracer(context: TraceContext, name: String) :
    Tracer(name = name, isEnabled = context.isEnabled) {
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
                    track = process.getOrCreateThreadTrack(id = id, name = name)
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

    @DelicateTracingApi
    override suspend fun tokenFromCoroutineContext(): PlatformThreadContextElement<*> {
        val track = currentThreadTrack()
        // Currently, Perfetto flows cannot fully represent fanouts and fanin's.
        // Therefore we simply propagate a single flowId from the parent to the child
        // and carry that throughout. This way, there is only 1 flow id that is used.
        val parent = currentCoroutineContext()[PlatformThreadContextElement.KEY]
        val current =
            buildThreadContextElement(
                // Placeholder to be filled in by beginSectionWithMetadata
                category = DEFAULT_STRING,
                name = DEFAULT_STRING,
                flowIds = parent?.flowIds ?: listOf(monotonicId()),
                // This method is called before a coroutine is resumed on a thread that
                // belongs to a dispatcher. This can be called more than once. So avoid creating
                // slices unless we transition to `STATE_END`.
                updateThreadContextBlock = { context ->
                    val contextElement = context[PlatformThreadContextElement.KEY]
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
                            contextElement.owner.beginSection(
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
                    val contextElement = context[PlatformThreadContextElement.KEY]
                    val name = contextElement?.name
                    if (
                        contextElement != null &&
                            name != null &&
                            contextElement.synchronizedCompareAndSet(
                                expected = STATE_BEGIN,
                                newValue = STATE_END,
                            )
                    ) {
                        contextElement.owner.endSection()
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
                        platformThreadContextElement.owner.endSection()
                    }
                },
            )
        current.owner = track
        return current
    }

    @DelicateTracingApi
    override fun beginSectionWithMetadata(
        category: String,
        name: String,
        token: PropagationToken,
    ): MetadataHandleCloseable {
        return when (token) {
            is PropagationUnsupportedToken -> {
                val track = currentThreadTrack()
                track.beginSection(category = category, name = name, token = token)
            }
            is PlatformThreadContextElement<*> -> {
                token.name = name
                token.category = category
                val track = token.owner
                track.beginSection(category = category, name = name, token = token)
            }
            else -> {
                throw IllegalArgumentException("Unsupported token type $token")
            }
        }
    }

    @DelicateTracingApi
    override fun counter(name: String): Counter {
        // Intentionally not synchronized, given context.currentThreadTrack() will return
        // the same instance of the underlying track.
        val counter = process.counters.getOrPut(name) { process.getOrCreateCounterTrack(name) }
        return PerfettoCounter(track = counter)
    }

    @DelicateTracingApi
    override fun instant(name: String) {
        val track = currentThreadTrack()
        track.instant(name = name)
    }

    override fun close() {
        // Does nothing
    }
}
