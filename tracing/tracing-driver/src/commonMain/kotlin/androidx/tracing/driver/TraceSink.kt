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

/**
 * Receives [PooledTracePacketArray]s from [Track]s and asynchronously serializes them to a file or
 * buffer, depending on implementation.
 *
 * Note that while serialized trace events are typically written as [Perfetto TracePacket protos] so
 * that they may be read by [ui.perfetto.dev](https://ui.perfetto.dev/) and queried with the
 * corresponding `TraceProcessor` tools, the final serialization format is up to the TraceSink's
 * implementation.
 */
public abstract class TraceSink : AutoCloseable {
    /**
     * Enqueue a [PooledTracePacketArray] to be written to the trace.
     *
     * This function may be called from any thread.
     */
    public abstract fun enqueue(pooledPacketArray: PooledTracePacketArray)

    /**
     * Flush any enqueued trace events to the [TraceSink].
     *
     * This function may be called from any thread.
     */
    public abstract fun flush()

    /**
     * Close the [TraceSink], completing any enqueued writes.
     *
     * This function may be called from any thread.
     */
    public abstract override fun close()
}

/** An empty trace sink that writes nowhere. */
internal class EmptyTraceSink : TraceSink() {
    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        pooledPacketArray.recycle()
    }

    override fun flush() {
        // Does nothing
    }

    override fun close() {
        // Does nothing
    }
}
