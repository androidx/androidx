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

import androidx.tracing.driver.INVALID_LONG
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_COUNTER
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_PROCESS
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_THREAD
import androidx.tracing.driver.TraceEvent
import com.squareup.wire.ProtoWriter
import perfetto.protos.MutableCounterDescriptor
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableThreadDescriptor
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

/**
 * Optimized serializer of [androidx.tracing.driver.TraceEvent], which writes out binary Perfetto
 * trace_packet.proto with minimal allocations
 *
 * Internally uses mutable protos to avoid allocations / GC churn.
 */
internal class WireTraceEventSerializer(sequenceId: Int, val protoWriter: ProtoWriter) {
    /**
     * Private scratchpad packet, used to avoid allocating a packet for each one serialized
     *
     * Always has the same track_event set on it
     */
    private val scratchTracePacket =
        MutableTracePacket(timestamp = INVALID_LONG, trusted_packet_sequence_id = sequenceId)
    /**
     * Private scratchpad descriptor, used to avoid allocating a descriptor for each new track
     * created
     */
    private val scratchTrackDescriptor = MutableTrackDescriptor()

    private val scratchTrackEvent = MutableTrackEvent(track_uuid = INVALID_LONG)

    fun writeTraceEvent(event: TraceEvent) {
        updateScratchPacketFromTraceEvent(
            event,
            scratchTracePacket,
            scratchTrackDescriptor,
            scratchTrackEvent,
        )
        MutableTracePacket.Companion.ADAPTER.encodeWithTag(protoWriter, 1, scratchTracePacket)
    }

    companion object {
        /**
         * Update the data in [MutableTracePacket] to represent the [TraceEvent] passed in.
         *
         * While it would be more elegant to have a MutableTracePacket extension constructor that
         * takes a TraceEvent, that would cause large amounts of object churn.
         */
        @JvmStatic
        internal fun updateScratchPacketFromTraceEvent(
            event: TraceEvent,
            scratchTracePacket: MutableTracePacket,
            scratchTrackDescriptor: MutableTrackDescriptor,
            scratchTrackEvent: MutableTrackEvent,
        ) {

            scratchTracePacket.timestamp = event.timestamp

            // in the common case when the track_descriptor isn't needed, clear it on the
            // MutableTracePacket
            scratchTracePacket.track_event = null
            scratchTracePacket.track_descriptor = null
            if (event.trackDescriptor != null) {
                // If the track_descriptor is needed, update and use the scratchTrackDescriptor to
                // avoid the
                // need to allocate a new object. Theoretically, this could be extended to the
                // counter/process/thread descriptors eventually if desired.
                event.trackDescriptor?.apply {
                    scratchTrackDescriptor.thread = null
                    scratchTrackDescriptor.counter = null
                    scratchTrackDescriptor.process = null

                    when (val type = event.trackDescriptor!!.type) {
                        TRACK_DESCRIPTOR_TYPE_COUNTER -> {
                            scratchTrackDescriptor.name = name
                            scratchTrackDescriptor.uuid = uuid
                            scratchTrackDescriptor.parent_uuid = parentUuid
                            scratchTrackDescriptor.counter = MutableCounterDescriptor()
                        }
                        TRACK_DESCRIPTOR_TYPE_PROCESS -> {
                            scratchTrackDescriptor.name = null
                            scratchTrackDescriptor.uuid = uuid
                            scratchTrackDescriptor.process =
                                MutableProcessDescriptor(pid = pid, process_name = name)
                        }
                        TRACK_DESCRIPTOR_TYPE_THREAD -> {
                            scratchTrackDescriptor.name = null
                            scratchTrackDescriptor.uuid = uuid
                            scratchTrackDescriptor.thread =
                                MutableThreadDescriptor(pid = pid, tid = tid, thread_name = name)
                        }
                        else -> throw IllegalStateException("Unknown TrackDescriptor type $type")
                    }
                    scratchTracePacket.track_descriptor = scratchTrackDescriptor
                }
            } else {
                // If the track event is needed (that is, when track descriptor isn't present)
                // populate and use the scratch track event
                scratchTrackEvent.type = MutableTrackEvent.Type.fromValue(event.type)
                scratchTrackEvent.track_uuid = event.trackUuid
                scratchTrackEvent.name = event.name
                scratchTrackEvent.counter_value = event.counterLongValue
                scratchTrackEvent.double_counter_value = event.counterDoubleValue

                // While it would be simpler to simply always set this.flow_ids, we avoid it in the
                // common cases when it does need to be called, since it's already up to date, as
                // Wire will deep copy the list with `immutableCopyOf(...)`. This is only necessary
                // if either it was already non-empty, or if it's becoming non-empty
                if (scratchTrackEvent.flow_ids.isNotEmpty() || event.flowIds.isNotEmpty()) {
                    scratchTrackEvent.flow_ids = event.flowIds
                }
                scratchTracePacket.track_event = scratchTrackEvent
            }
        }
    }
}
