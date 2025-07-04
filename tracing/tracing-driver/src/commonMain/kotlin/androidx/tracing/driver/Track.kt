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

package androidx.tracing.driver

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
public abstract class Track(
    /** The [TraceContext] instance. */
    @JvmField // avoid getter generation
    internal val context: TraceContext,
    /**
     * The uuid for the track descriptor.
     *
     * This ID must be unique within all [Track]s in a given trace produced by [TraceDriver] - it is
     * used to connect recorded trace events to the containing track.
     */
    @JvmField // avoid getter generation
    internal val uuid: Long,
) {
    /**
     * Any time we emit trace packets relevant to this process. We need to make sure the necessary
     * preamble packets that describe the process and threads are also emitted. This is used to make
     * sure that we only do that once.
     */
    // Every poolable that is obtained from the pool, keeps track of its owner.
    // The underlying poolable, if eventually recycled by the Sink after an emit() is complete.
    internal val pool: ProtoPool = ProtoPool(isDebug = context.isDebug)

    // this would be private, but internal prevents getters from being created
    @JvmField // avoid getter generation
    internal var currentPacketArray = pool.obtainTracePacketArray()
    @JvmField // we cache this separately to avoid having to query it with a function each time
    internal var currentPacketArraySize = currentPacketArray.packets.size

    internal fun flush() {
        context.sink.enqueue(currentPacketArray)
        currentPacketArray = pool.obtainTracePacketArray()
        currentPacketArraySize = currentPacketArray.packets.size
    }

    /** Emit is internal, but it must be sure to only access */
    internal inline fun emitTraceEvent(
        immediateDispatch: Boolean = false,
        block: (TraceEvent) -> Unit,
    ) {
        currentPacketArray.apply {
            block(packets[fillCount])
            fillCount++
            if (fillCount == currentPacketArraySize || immediateDispatch) {
                context.sink.enqueue(this)

                // greedy reset / reallocate array
                currentPacketArray = pool.obtainTracePacketArray()
                currentPacketArraySize = currentPacketArray.packets.size
            }
        }
    }

    /** Test API for benchmarking */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun enqueueSingleUnmodifiedEvent() {
        emitTraceEvent(immediateDispatch = true) {
            // noop
        }
    }

    /** Test API for benchmarking */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun resetFillCount() {
        currentPacketArray.fillCount = 0
    }
}
