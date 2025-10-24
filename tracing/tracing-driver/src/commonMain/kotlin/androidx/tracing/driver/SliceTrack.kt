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

/**
 * Horizontal track of time in a trace which contains slice events (`beginSection` / `endSection`).
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@RestrictTo(Scope.LIBRARY_GROUP)
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

    // Use a single shared trace event scope to avoid allocations.
    @JvmField
    internal val traceEventScope: TraceEventScope =
        TraceEventScope().apply { owner = this@SliceTrack }

    // Use a single shared instance of MetadataCloseable
    @JvmField internal val metadataCloseable: MetadataHandleCloseable = MetadataHandleCloseable()

    /**
     * Writes a trace message indicating that a given section of code has begun.
     *
     * Should be followed by a corresponding call to [endSection] on the same [SliceTrack]. If a
     * corresponding [endSection] is missing, the section will be present in the trace, but
     * non-terminating (generally shown as fading out to the left).
     *
     * @param name The name of the code section to appear in the trace.
     * @param token A [PropagationToken] that can be used for context propagation. The default
     *   implementation uses a list of [Long]s which will connect this trace section to other
     *   sections in the trace, potentially on different Tracks. The start and end of each trace
     *   `flow` (connection) between trace sections must share an ID, so each `Long` must be unique
     *   to each `flow` in the trace.
     * @param metadataBlock The block that can include metadata about to the [TraceEvent].
     */
    internal inline fun beginSection(
        category: String,
        name: String,
        token: PropagationToken,
        crossinline metadataBlock: (MetadataHandle.() -> Unit) = {},
    ): MetadataHandleCloseable {
        // Its okay to return the instance of the MetadataCloseable outside this method because
        // the only way to dispatch the call to beginSection is by using the currentThreadTrack()
        // so effectively the call to the outer beginSection on the track is being executed on
        // the same thread.
        return synchronized(traceEventScope) {
            metadataCloseable.metadata = EmptyMetadataHandle
            metadataCloseable.closeable = EmptyCloseable
            if (context.isEnabled) {
                val event = obtainTraceEvent()
                if (event != null) {
                    event.primaryCategory = category
                    traceEventScope.event = event
                    metadataCloseable.metadata = traceEventScope
                    metadataCloseable.closeable =
                        when (token) {
                            is PropagationUnsupportedToken -> {
                                event.setBeginSection(trackUuid = uuid, name = name)
                                metadataBlock(traceEventScope)
                                // The closeable will just end up calling endSection()
                                this
                            }
                            is PlatformThreadContextElement<*> -> {
                                event.setBeginSectionWithFlows(
                                    trackUuid = uuid,
                                    name = name,
                                    flowIds = token.flowIds,
                                )
                                metadataBlock(traceEventScope)
                                // The context element knows how to endSection() while tracking
                                // suspension and resumption points
                                token
                            }
                            else -> {
                                throw IllegalArgumentException(
                                    "Unsupported PropagationToken: $token"
                                )
                            }
                        }
                }
            }
            metadataCloseable
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
            synchronized(traceEventScope) {
                val event = obtainTraceEvent()
                event?.apply {
                    event.setEndSection(trackUuid = uuid)
                    dispatchTraceEvent(event)
                }
            }
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
    public fun instant(name: String) {
        if (context.isEnabled) {
            synchronized(traceEventScope) {
                val event = obtainTraceEvent()
                event?.apply {
                    setInstant(trackUuid = uuid, name = name)
                    dispatchTraceEvent(event)
                }
            }
        }
    }

    override fun close() {
        // Used when the token is an instance of PropagationUnsupportedToken
        endSection()
    }
}
