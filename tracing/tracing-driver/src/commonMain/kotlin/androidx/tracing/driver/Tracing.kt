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

import perfetto.protos.MutableTrackEvent

/** A [PooledTracePacket] that represents a starting point of a trace span. */
internal fun <T : EventTrack> T.trackBeginPacket(
    name: String,
    flowIds: List<Long> = emptyList()
): PooledTracePacket {
    val packet = pool.obtainTracePacket()
    val event = pool.obtainTrackEvent()
    packet.trackPoolableForOwnership(event)
    event.trackEvent.type = MutableTrackEvent.Type.TYPE_SLICE_BEGIN
    event.trackEvent.track_uuid = uuid
    event.trackEvent.name = name
    // Wire creates an `immutableCopyOf(...) the incoming list.
    // Only set the flowIds if they exist.
    if (flowIds.isNotEmpty()) {
        event.trackEvent.flow_ids = flowIds
    }
    packet.tracePacket.timestamp = nanoTime()
    packet.tracePacket.track_event = event.trackEvent
    packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
    return packet
}

/** A [PooledTracePacket] that represents an end point of a trace span. */
internal fun <T : EventTrack> T.trackEndPacket(name: String): PooledTracePacket {
    val packet = pool.obtainTracePacket()
    val event = pool.obtainTrackEvent()
    packet.trackPoolableForOwnership(event)
    event.trackEvent.type = MutableTrackEvent.Type.TYPE_SLICE_END
    event.trackEvent.track_uuid = uuid
    event.trackEvent.name = name
    packet.tracePacket.timestamp = nanoTime()
    packet.tracePacket.track_event = event.trackEvent
    packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
    return packet
}

internal fun <T : EventTrack> T.instantPacket(): PooledTracePacket {
    val packet = pool.obtainTracePacket()
    val event = pool.obtainTrackEvent()
    packet.trackPoolableForOwnership(event)
    event.trackEvent.type = MutableTrackEvent.Type.TYPE_INSTANT
    event.trackEvent.track_uuid = uuid
    packet.tracePacket.track_event = event.trackEvent
    packet.tracePacket.timestamp = nanoTime()
    packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
    return packet
}

internal fun CounterTrack.longCounterPacket(value: Long): PooledTracePacket {
    val packet = pool.obtainTracePacket()
    val event = pool.obtainTrackEvent()
    packet.trackPoolableForOwnership(event)
    event.trackEvent.type = MutableTrackEvent.Type.TYPE_COUNTER
    event.trackEvent.track_uuid = uuid
    event.trackEvent.counter_value = value
    packet.tracePacket.track_event = event.trackEvent
    packet.tracePacket.timestamp = nanoTime()
    packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
    return packet
}

internal fun CounterTrack.doubleCounterPacket(value: Double): PooledTracePacket {
    val packet = pool.obtainTracePacket()
    val event = pool.obtainTrackEvent()
    packet.trackPoolableForOwnership(event)
    event.trackEvent.type = MutableTrackEvent.Type.TYPE_COUNTER
    event.trackEvent.track_uuid = uuid
    event.trackEvent.double_counter_value = value
    packet.tracePacket.track_event = event.trackEvent
    packet.tracePacket.timestamp = nanoTime()
    packet.tracePacket.trusted_packet_sequence_id = context.sequenceId
    return packet
}
