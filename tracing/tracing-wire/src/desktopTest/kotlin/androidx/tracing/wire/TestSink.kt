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

package androidx.tracing.wire

import androidx.tracing.wire.protos.MutableTraceAttributes
import androidx.tracing.wire.protos.MutableTracePacket
import androidx.tracing.wire.protos.MutableTrackEvent

internal fun TestSink.firstStartStopWithName(
    name: String
): Pair<MutableTracePacket, MutableTracePacket> {
    val result = findPacket(packets = packets, startIndex = 0, name = name)
    check(result.start != null) { "Cannot find a trace packet with name $name " }
    check(result.end != null) { "Cannot find an end marker for a trace packet with $name" }
    return result.start to result.end
}

internal data class FindPacketResult(
    val start: MutableTracePacket?,
    val end: MutableTracePacket?,
    val nextIndex: Int,
)

internal fun findPacket(
    packets: List<MutableTracePacket>,
    startIndex: Int,
    name: String,
): FindPacketResult {
    return findPacket(packets = packets, startIndex = startIndex) { start ->
        name == start.track_event?.name
    }
}

internal fun findAllPackets(
    packets: List<MutableTracePacket>,
    predicate: (start: MutableTracePacket) -> Boolean,
): List<MutableTracePacket> {
    return findAllPackets(
        packets = packets,
        predicate = predicate,
        startIndex = 0,
        accumulator = mutableListOf(),
    )
}

internal tailrec fun findAllPackets(
    packets: List<MutableTracePacket>,
    predicate: (start: MutableTracePacket) -> Boolean,
    startIndex: Int,
    accumulator: MutableList<MutableTracePacket>,
): MutableList<MutableTracePacket> {
    val result = findPacket(packets = packets, startIndex = startIndex, predicate = predicate)
    if (result.start != null) accumulator += result.start
    if (result.end != null && result.start != result.end) accumulator += result.end
    return if (result.nextIndex == packets.size) accumulator
    else
        findAllPackets(
            packets = packets,
            startIndex = result.nextIndex,
            predicate = predicate,
            accumulator = accumulator,
        )
}

internal fun findPacket(
    packets: List<MutableTracePacket>,
    startIndex: Int,
    predicate: (start: MutableTracePacket) -> Boolean,
): FindPacketResult {
    var index = startIndex
    var starts = 0
    var startPacket: MutableTracePacket? = null
    var endPacket: MutableTracePacket? = null
    while (index < packets.size) {
        val packet = packets[index]
        val instant = packet.track_event?.type == MutableTrackEvent.Type.TYPE_INSTANT
        val begin = packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_BEGIN
        val sameTrack =
            if (startPacket == null) {
                // We don't know which track yet.
                true
            } else {
                packet.track_event?.track_uuid == startPacket.track_event?.track_uuid &&
                    packet.timestamp >= startPacket.timestamp
            }
        val end = sameTrack && packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_END
        // We found our start packet
        if ((instant || begin) && predicate(packet) && startPacket == null) {
            startPacket = packet
            if (instant) {
                // If it's an instant packet, we have our end as well. No need to look further.
                endPacket = packet
                break
            }
        }
        // We have a start packet already
        // Make sure the packet being evaluated is on the same track
        if (startPacket != null && packet != startPacket && sameTrack) {
            if (begin) starts += 1
            if (end) starts -= 1
        }

        if (startPacket != null && end && starts < 0) {
            endPacket = packet
            break
        }
        index += 1
    }
    val nextIndex = (index + 1).coerceAtMost(packets.size)
    return FindPacketResult(start = startPacket, end = endPacket, nextIndex = nextIndex)
}

internal fun TestSink.attributes(): Map<String, Any?> {
    val attributes: List<MutableTraceAttributes> =
        packets.mapNotNull { packet -> packet.trace_attributes }
    val attributeMap = mutableMapOf<String, Any?>()
    attributes.forEach { traceAttributes ->
        traceAttributes.attribute.forEach { attribute ->
            val key = attribute.key
            if (key != null) {
                attributeMap[key] = attribute.string_value ?: attribute.long_value
            }
        }
    }
    return attributeMap
}

internal fun TestSink.firstStartStopWithName(
    name: String,
    block: (start: MutableTracePacket, end: MutableTracePacket) -> Unit,
) {
    val (start, end) = firstStartStopWithName(name)
    block(start, end)
}
