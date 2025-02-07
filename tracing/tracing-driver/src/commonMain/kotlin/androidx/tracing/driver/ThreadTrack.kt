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

/** Represents a track for a Thread like construct in Perfetto. */
public open class ThreadTrack(
    /** The thread id. */
    internal val id: Int,
    /** The name of the thread. */
    internal val name: String,
    /** The process track that the thread belongs to. */
    internal val process: ProcessTrack,
    hasPreamble: Boolean = true,
) :
    EventTrack(
        context = process.context,
        hasPreamble = hasPreamble,
        uuid = monotonicId(),
        parent = process
    ) {
    override fun preamblePacket(): PooledTracePacket? {
        val packet = pool.obtainTracePacket()
        val track = pool.obtainTrackDescriptor()
        val thread = pool.obtainThreadDescriptor()
        packet.trackPoolableForOwnership(track)
        packet.trackPoolableForOwnership(thread)
        // Populate thread details
        thread.threadDescriptor.pid = process.id
        thread.threadDescriptor.tid = id
        thread.threadDescriptor.thread_name = name
        // Link
        track.trackDescriptor.uuid = uuid
        track.trackDescriptor.thread = thread.threadDescriptor
        packet.tracePacket.timestamp = nanoTime()
        packet.tracePacket.track_descriptor = track.trackDescriptor
        packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
        return packet
    }
}

// An empty thread track when tracing is disabled

private const val EMPTY_THREAD_ID = -1
private const val EMPTY_THREAD_NAME = "Empty Thread"

internal class EmptyThreadTrack(process: EmptyProcessTrack) :
    ThreadTrack(
        id = EMPTY_THREAD_ID,
        name = EMPTY_THREAD_NAME,
        process = process,
        hasPreamble = false
    ) {
    override fun preamblePacket(): PooledTracePacket? = null
}
