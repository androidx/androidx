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

/** Represents a Perfetto Counter track. */
public open class CounterTrack(
    /** The name of the counter track */
    private val name: String,
    /** The parent track the counter belongs to. */
    private val parent: Track,
    hasPreamble: Boolean = true,
) :
    Track(
        context = parent.context,
        hasPreamble = hasPreamble,
        uuid = monotonicId(),
        parent = parent
    ) {
    override fun preamblePacket(): PooledTracePacket? {
        val packet = pool.obtainTracePacket()
        val track = pool.obtainTrackDescriptor()
        val counter = pool.obtainCounterDescriptor()
        packet.trackPoolableForOwnership(track)
        packet.trackPoolableForOwnership(counter)
        track.trackDescriptor.uuid = uuid
        track.trackDescriptor.name = name
        track.trackDescriptor.parent_uuid = parent.uuid
        track.trackDescriptor.counter = counter.counterDescriptor
        packet.tracePacket.timestamp = nanoTime()
        packet.tracePacket.track_descriptor = track.trackDescriptor
        return packet
    }

    public fun emitLongCounterPacket(value: Long) {
        if (context.isEnabled) {
            emit(longCounterPacket(value))
        }
    }

    public fun emitDoubleCounterPacket(value: Double) {
        if (context.isEnabled) {
            emit(doubleCounterPacket(value))
        }
    }
}

// An empty counter track when tracing is disabled

private const val EMPTY_COUNTER_NAME = "Empty Counter"

internal class EmptyCounterTrack(process: EmptyProcessTrack) :
    CounterTrack(name = EMPTY_COUNTER_NAME, parent = process, hasPreamble = false) {
    override fun preamblePacket(): PooledTracePacket? = null
}
