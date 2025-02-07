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

import androidx.collection.mutableScatterMapOf

/** Represents a track for a process in a perfetto trace. */
public open class ProcessTrack(
    /** The tracing context. */
    context: TraceContext,
    /** The process id */
    internal val id: Int,
    /** The name of the process. */
    internal val name: String,
    hasPreamble: Boolean = true,
) : EventTrack(context = context, hasPreamble = hasPreamble, uuid = monotonicId(), parent = null) {
    internal val lock = Lock()
    internal val threads = mutableScatterMapOf<String, ThreadTrack>()
    internal val counters = mutableScatterMapOf<String, CounterTrack>()

    override fun preamblePacket(): PooledTracePacket? {
        val packet = pool.obtainTracePacket()
        val track = pool.obtainTrackDescriptor()
        val process = pool.obtainProcessDescriptor()
        packet.trackPoolableForOwnership(track)
        packet.trackPoolableForOwnership(process)
        // Populate process details
        process.processDescriptor.pid = id
        process.processDescriptor.process_name = name
        // Link
        track.trackDescriptor.uuid = uuid
        track.trackDescriptor.process = process.processDescriptor
        packet.tracePacket.timestamp = nanoTime()
        packet.tracePacket.track_descriptor = track.trackDescriptor
        packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
        return packet
    }

    /** @return A [ThreadTrack] for a given [ProcessTrack] using the unique thread [id]. */
    public open fun ThreadTrack(id: Int, name: String): ThreadTrack {
        // Thread ids are only unique for lifetime of the thread and can be potentially reused.
        // Therefore we end up combining the `name` of the thread and its `id` as a key.
        val key = "$id/$name"
        return threads[key]
            ?: lock.withLock {
                val track =
                    threads.getOrPut(key) { ThreadTrack(id = id, name = name, process = this) }
                check(track.name == name)
                track
            }
    }

    /** @return A [CounterTrack] for a given [ProcessTrack] with the provided [name]. */
    public open fun CounterTrack(name: String): CounterTrack {
        return counters[name]
            ?: lock.withLock {
                counters.getOrPut(name) { CounterTrack(name = name, parent = this) }
            }
    }
}

// An empty process track when tracing is disabled.

private const val EMPTY_PROCESS_ID = -1
private const val EMPTY_PROCESS_NAME = "Empty Process"

internal class EmptyProcessTrack(context: EmptyTraceContext) :
    ProcessTrack(
        context = context,
        id = EMPTY_PROCESS_ID,
        name = EMPTY_PROCESS_NAME,
        hasPreamble = false
    ) {

    private val emptyContext: EmptyTraceContext = context

    override fun preamblePacket(): PooledTracePacket? = null

    override fun ThreadTrack(id: Int, name: String): ThreadTrack = emptyContext.thread

    override fun CounterTrack(name: String): CounterTrack = emptyContext.counter
}
