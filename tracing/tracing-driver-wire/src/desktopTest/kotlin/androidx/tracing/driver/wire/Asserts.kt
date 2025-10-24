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

import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackEvent

internal fun TestSink.firstStartStopWithName(
    name: String
): Pair<MutableTracePacket, MutableTracePacket> {
    val start = packets.find { packet -> packet.track_event?.name == name }
    check(start != null) { "Cannot find a trace packet with name $name " }
    var end: MutableTracePacket? = null
    var starts = 0
    for (packet in packets) {
        if (packet.track_event?.track_uuid != start.track_event?.track_uuid) continue
        if (packet.timestamp <= start.timestamp) continue
        if (packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_BEGIN) {
            starts += 1
        } else if (packet.track_event?.type == MutableTrackEvent.Type.TYPE_SLICE_END) {
            starts -= 1
            if (starts <= 0) {
                end = packet
            }
        }
    }
    check(end != null) { "Cannot find an end marker for a trace packet with $name" }
    return start to end
}

internal fun TestSink.firstStartStopWithName(
    name: String,
    block: (start: MutableTracePacket, end: MutableTracePacket) -> Unit,
) {
    val (start, end) = firstStartStopWithName(name)
    block(start, end)
}
