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

package androidx.tracing.wire

import androidx.annotation.GuardedBy
import androidx.annotation.IntRange
import androidx.tracing.PooledTracePacketArray
import androidx.tracing.TraceEvent
import androidx.tracing.TraceSink
import androidx.tracing.synchronized
import com.squareup.wire.ProtoWriter
import kotlin.concurrent.Volatile
import okio.BufferedSink

/**
 * A [TraceSink] that stores [TraceEvent]s in a fixed-size ring buffer to minimize allocations.
 *
 * This buffer is designed to hold trace events temporarily before they are flushed. When the buffer
 * is full, it overwrites the oldest events.
 *
 * A standard [flush] or [close] will simply clear the buffer and drop the events. To persist the
 * trace data, you must provide a [BufferedSink] by calling [flushTo] or [closeAndFlushTo].
 *
 * This implementation converts [androidx.tracing.TraceEvent]s into binary protos using
 * [the Wire library](https://square.github.io/wire/).
 */
@ExperimentalRingBufferApi
public class InMemoryRingBufferTraceSink(
    /**
     * ID which uniquely identifies the trace capture system, within which uuids are guaranteed to
     * be unique.
     *
     * This is only relevant when merging traces across multiple sources (e.g. combining the trace
     * output of this library with a trace captured on Android with Perfetto).
     *
     * Value must be greater than 0.
     */
    @IntRange(from = 1) sequenceId: Int,
    /**
     * The estimated total capacity of the buffer in bytes. The number of events the buffer can hold
     * is estimated based on an average event size of 1KB.
     */
    capacityInBytes: Long,
) : TraceSink() {
    // Estimate 1KB per event
    private val countLimit = (capacityInBytes / 1024).toInt().coerceAtLeast(1)
    @GuardedBy("lock") private val events = Array(countLimit) { TraceEvent() }
    @GuardedBy("lock") private var writeIndex = 0
    @GuardedBy("lock") private var readIndex = 0
    // Count is the number of events currently in the buffer, and is reset to 0 after flushing.
    @GuardedBy("lock") private var count = 0
    @Volatile private var isDroppedTraceEvent = false
    private val lock = Any()

    // Once the sink is marked as closed. No more enqueue()'s are allowed.
    @Volatile private var closed = false

    init {
        require(sequenceId > 0) {
            "Provided sequenceId was $sequenceId, it must be greater than 0."
        }
    }

    private val wireTraceEventSerializer = WireTraceEventSerializer(sequenceId)

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        if (closed) return

        synchronized(lock) {
            pooledPacketArray.forEach { event ->
                val dest = events[writeIndex]
                dest.copyFrom(event)

                writeIndex++
                if (writeIndex == countLimit) {
                    writeIndex = 0
                }

                if (count < countLimit) {
                    count++
                } else {
                    // We are overflowing and overwriting the oldest event.
                    // This means we dropped data.
                    readIndex++
                    if (readIndex == countLimit) {
                        readIndex = 0
                    }
                    isDroppedTraceEvent = true
                }
            }
        }
        pooledPacketArray.recycle()
    }

    override fun onDroppedTraceEvent() {
        if (closed) return
        isDroppedTraceEvent = true
    }

    /** Clears the current buffer and drops the events without writing them out. */
    override fun flush() {
        flushInternal(null)
    }

    /**
     * Flushes the current buffer to the provided [BufferedSink].
     *
     * This writes out the ring buffer's contents to the sink and then clears the ring buffer.
     */
    public fun flushTo(bufferedSink: BufferedSink) {
        flushInternal(bufferedSink)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun flushInternal(sink: BufferedSink?) {
        synchronized(lock) {
            var index = readIndex

            val reportDroppedTraceEvent = isDroppedTraceEvent
            isDroppedTraceEvent = false
            var firstEventInFlush = true

            val protoWriter = sink?.let { ProtoWriter(it) }

            repeat(count) {
                val event = events[index]
                val isDropped = if (firstEventInFlush) reportDroppedTraceEvent else false
                firstEventInFlush = false

                if (protoWriter != null) {
                    wireTraceEventSerializer.writeTraceEvent(
                        protoWriter = protoWriter,
                        event = event,
                        reportDroppedTraceEvent = isDropped,
                    )
                }

                // Reset the event to free up memory (strings, arrays, etc.)
                // since it's no longer logically in the ring buffer.
                event.reset()

                index++
                if (index == countLimit) {
                    index = 0
                }
            }

            count = 0
            writeIndex = 0
            readIndex = 0

            sink?.flush()
        }
    }

    /**
     * Marks the [InMemoryRingBufferTraceSink] as closed and clears any remaining buffered events
     * without writing them out.
     */
    override fun close() {
        if (closed) return
        closed = true
        flushInternal(null)
    }

    /**
     * Marks the [InMemoryRingBufferTraceSink] as closed and flushes the remaining buffered events
     * to the provided [BufferedSink] but does not close it.
     */
    public fun closeAndFlushTo(bufferedSink: BufferedSink) {
        if (closed) return
        closed = true
        flushInternal(bufferedSink)
    }
}
