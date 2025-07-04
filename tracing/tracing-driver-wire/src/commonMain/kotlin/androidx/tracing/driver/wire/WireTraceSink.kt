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

package androidx.tracing.driver.wire

import androidx.annotation.GuardedBy
import androidx.tracing.driver.PooledTracePacketArray
import androidx.tracing.driver.Queue
import androidx.tracing.driver.TraceEvent
import androidx.tracing.driver.TraceSink
import com.squareup.wire.ProtoWriter
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okio.BufferedSink

/**
 * The trace sink that writes [BufferedSink], to a new file per trace session.
 *
 * This implementation converts [TraceEvent]s into binary protos using
 * [the Wire library](https://square.github.io/wire/).
 *
 * The outputs created by `WireTraceSync` can be visualized with
 * [ui.perfetto.dev](https://ui.perfetto.dev/), and queried by
 * [TraceProcessor](https://developer.android.com/reference/androidx/benchmark/traceprocessor/TraceProcessor)
 * from the `androidx.benchmark:benchmark-traceprocessor` library, the
 * [C++](https://perfetto.dev/docs/analysis/trace-processor) tool it's built on, or the
 * [Python](https://perfetto.dev/docs/analysis/trace-processor-python) wrapper.
 *
 * As binary protos embed strings as UTF-8, note that any strings serialized by WireTraceSink will
 * be serialized as UTF-8.
 *
 * To create a WireTraceSink for a File, you can use `File("myFile").appendingSink().buffer()`.
 */
public class WireTraceSink(
    /**
     * ID which uniquely identifies the trace capture system, within which uuids are guaranteed to
     * be unique.
     *
     * This is only relevant when merging traces across multiple sources (e.g. combining the trace
     * output of this library with a trace captured on Android with Perfetto).
     */
    sequenceId: Int,

    /** Output [BufferedSink] the trace will be written to. */
    private val bufferedSink: BufferedSink,

    /** Coroutine context to execute the serialization on. */
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) : TraceSink() {
    private val wireTraceEventSerializer =
        WireTraceEventSerializer(sequenceId, ProtoWriter(bufferedSink))

    // There are 2 distinct mechanisms for thread safety here, and they are not necessarily in sync.
    // The Queue by itself is thread-safe, but after we drain the queue we mark drainRequested
    // to false (not an atomic operation). So a writer can come along and add a pooled array of
    // trace packets. That is still okay given, those packets will get picked during the next
    // drain request; or on flush() prior to the close() of the Sink.
    // No packets are lost or dropped; and therefore we are still okay with this small
    // compromise with thread safety.
    private val queue = Queue<PooledTracePacketArray>()

    private val drainLock = Any() // Lock used to keep drainRequested, resumeDrain in sync.

    @GuardedBy("drainLock") private var drainRequested = false

    // Once the sink is marked as closed. No more enqueue()'s are allowed. This way we can never
    // race between a new drainRequest() after the last request for flush() happened. This
    // is because we simply disallow adding more items to the underlying queue.
    @Volatile private var closed = false

    @GuardedBy("drainLock") private var resumeDrain: Continuation<Unit>? = null

    init {
        resumeDrain =
            suspend {
                    coroutineContext[Job]?.invokeOnCompletion { makeDrainRequest() }
                    while (true) {
                        drainQueue() // Sets drainRequested to false on completion
                        suspendCoroutine<Unit> { continuation ->
                            synchronized(drainLock) { resumeDrain = continuation }
                            COROUTINE_SUSPENDED // Suspend
                        }
                    }
                }
                .createCoroutineUnintercepted(Continuation(context = coroutineContext) {})

        // Kick things off and suspend
        makeDrainRequest()
    }

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        if (!closed) {
            queue.addLast(pooledPacketArray)
            makeDrainRequest()
        }
    }

    override fun flush() {
        makeDrainRequest()
        while (queue.isNotEmpty() && synchronized(drainLock) { drainRequested }) {
            // Await completion of the drain.
        }
        bufferedSink.flush()
    }

    private fun makeDrainRequest() {
        // Only make a request if one is not already ongoing
        synchronized(drainLock) {
            if (!drainRequested) {
                drainRequested = true
                resumeDrain?.resume(Unit)
            }
        }
    }

    private fun drainQueue() {
        while (queue.isNotEmpty()) {
            val pooledPacketArray = queue.removeFirstOrNull()
            if (pooledPacketArray != null) {
                pooledPacketArray.forEach { wireTraceEventSerializer.writeTraceEvent(it) }
                pooledPacketArray.recycle()
            }
        }
        synchronized(drainLock) {
            drainRequested = false
            // Mark resumeDrain as consumed because the Coroutines Machinery might still consider
            // the Continuation as resumed after drainQueue() completes. This was the Atomics
            // drainRequested, and the Continuation resumeDrain are in sync.
            resumeDrain = null
        }
    }

    override fun close() {
        // Mark closed.
        // We don't need a critical section here, given we have one final flush() that blocks
        // until the queue is drained. So even if we are racing against additions to the queue,
        // that should still be okay, because enqueue()'s will eventually start no-oping.
        closed = true
        flush()
        bufferedSink.close()
    }
}
