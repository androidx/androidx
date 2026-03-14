/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * Tracks are a horizontal track of time in the trace that contains trace events - often counters
 * (`setCounter`), or slices (`beginSection` / `endSection`) - which stack together to form the
 * timeline view.
 *
 * Tracks can have parents/children, such as a [ProcessTrack] having several child [ThreadTrack]s.
 * * Use [ProcessTrack] for trace slices and events scoped to a process, but not a specific thread.
 * * Use [CounterTrack] (often created as a child of a [ProcessTrack]) to trace integer or floating
 *   point values that can be updated over time.
 * * Use [ThreadTrack] (generally created as a child of a [ProcessTrack]) to trace what is happening
 *   on a specific thread. With synchronous (non-coroutine) code, this is where most trace events
 *   should go.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Track(
    /** The [TraceContext] instance. */
    @field:Suppress("ShowingMemberInHiddenClass")
    @JvmField // avoid getter generation
    @PublishedApi
    internal val context: TraceContext,
    /**
     * The uuid for the track descriptor.
     *
     * This ID must be unique within all [Track]s in a given trace produced by
     * [AbstractTraceDriver] - it is used to connect recorded trace events to the containing track.
     */
    @field:Suppress("ShowingMemberInHiddenClass")
    @JvmField // avoid getter generation
    @PublishedApi
    internal val uuid: Long,
) : AutoCloseable {
    /**
     * Any time we emit trace packets relevant to this process. We need to make sure the necessary
     * preamble packets that describe the process and threads are also emitted. This is used to make
     * sure that we only do that once.
     */
    // Every poolable that is obtained from the pool, keeps track of its owner.
    // The underlying poolable, if eventually recycled by the Sink after an emit() is complete.
    @JvmField // avoid getter generation
    internal val pool: ProtoPool = ProtoPool(isDebug = context.isDebug)

    // this would be private, but internal prevents getters from being created
    @JvmField // avoid getter generation
    @Volatile
    internal var currentPacketArray: PooledTracePacketArray? = pool.obtainTracePacketArray()

    internal fun flush() {
        val currentPacketArray = this.currentPacketArray
        if (currentPacketArray != null) {
            context.sink.enqueue(currentPacketArray)
            // Try obtaining a new pooled trace packet.
            this.currentPacketArray = pool.obtainTracePacketArray()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun obtainTraceEvent(): TraceEvent? {
        if (currentPacketArray == null) {
            // Try obtaining another pooled trace packet array.
            currentPacketArray = pool.obtainTracePacketArray()
        }
        // If we still cannot obtain a PooledTracePacketArray, then just mark the trace event
        // as lost.
        val currentPacketArray = currentPacketArray
        return if (currentPacketArray == null) {
            context.sink.onDroppedTraceEvent()
            null
        } else {
            currentPacketArray.packets[currentPacketArray.fillCount]
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun dispatchTraceEvent(event: TraceEvent?, immediateDispatch: Boolean = false) {
        if (event == null) return
        val currentTracePacketArray = currentPacketArray ?: return
        val currentPacketArraySize = currentPacketArray?.packets?.size
        currentTracePacketArray.apply {
            fillCount += 1
            if (fillCount == currentPacketArraySize || immediateDispatch) {
                context.sink.enqueue(pooledPacketArray = this)
                // greedy reset / reallocate array
                this@Track.currentPacketArray = pool.obtainTracePacketArray()
            }
        }
    }

    /** Test API for benchmarking */
    public fun enqueueSingleUnmodifiedEvent() {
        dispatchTraceEvent(event = obtainTraceEvent(), immediateDispatch = true)
    }

    /** Test API for benchmarking */
    public fun resetFillCount() {
        currentPacketArray?.fillCount = 0
    }

    override fun close() {
        // Does nothing
    }
}
