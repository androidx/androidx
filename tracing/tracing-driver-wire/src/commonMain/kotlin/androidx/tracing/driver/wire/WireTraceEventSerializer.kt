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

import androidx.tracing.driver.AtomicInteger
import androidx.tracing.driver.DEFAULT_LONG
import androidx.tracing.driver.DEFAULT_STRING
import androidx.tracing.driver.LAST_INDEX_WHEN_EMPTY
import androidx.tracing.driver.METADATA_ENTRIES_EXPECTED_SIZE
import androidx.tracing.driver.METADATA_TYPE_BOOLEAN
import androidx.tracing.driver.METADATA_TYPE_DOUBLE
import androidx.tracing.driver.METADATA_TYPE_LONG
import androidx.tracing.driver.METADATA_TYPE_STRING
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_COUNTER
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_PROCESS
import androidx.tracing.driver.TRACK_DESCRIPTOR_TYPE_THREAD
import androidx.tracing.driver.TraceEvent
import com.squareup.wire.ProtoWriter
import perfetto.protos.MutableCounterDescriptor
import perfetto.protos.MutableDebugAnnotation
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableThreadDescriptor
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("NOTHING_TO_INLINE", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")

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
        MutableTracePacket(timestamp = DEFAULT_LONG, trusted_packet_sequence_id = sequenceId)

    /** Private scratchpad of debug annotations. */
    private var scratchAnnotations: MutableList<MutableDebugAnnotation> =
        MutableList(size = METADATA_ENTRIES_EXPECTED_SIZE) { MutableDebugAnnotation() }

    /** This is passed by ref, to avoid unnecessary computation. */
    // Using a Long for convenience, given we already have a commonized definition.
    private val scratchAnnotationIndex = AtomicInteger(/* initialValue= */ -1)

    /**
     * Private scratchpad descriptor, used to avoid allocating a descriptor for each new track
     * created
     */
    private val scratchTrackDescriptor = MutableTrackDescriptor()

    private val scratchTrackEvent = MutableTrackEvent(track_uuid = DEFAULT_LONG)

    fun writeTraceEvent(event: TraceEvent, reportDroppedTraceEvent: Boolean = false) {
        updateScratchPacketFromTraceEvent(
            event = event,
            reportDroppedTraceEvent = reportDroppedTraceEvent,
            scratchTracePacket = scratchTracePacket,
            scratchTrackDescriptor = scratchTrackDescriptor,
            scratchTrackEvent = scratchTrackEvent,
            scratchAnnotations = scratchAnnotations,
            scratchAnnotationIndex = scratchAnnotationIndex,
        )
        MutableTracePacket.Companion.ADAPTER.encodeWithTag(
            writer = protoWriter,
            tag = 1,
            value = scratchTracePacket,
        )
        resetScratchAnnotations()
    }

    /** Reset and resize scratch annotations when necessary. */
    inline fun resetScratchAnnotations() {
        val index = scratchAnnotationIndex.get()
        val size = index + 1
        // Reset
        repeat(size) {
            val scratchAnnotation = scratchAnnotations[it]
            scratchAnnotation.name = null
            scratchAnnotation.bool_value = null
            scratchAnnotation.int_value = null
            scratchAnnotation.double_value = null
            scratchAnnotation.string_value = null
        }
        // Resize
        if (size > METADATA_ENTRIES_EXPECTED_SIZE) {
            scratchAnnotations = scratchAnnotations.subList(0, METADATA_ENTRIES_EXPECTED_SIZE)
        }
        scratchAnnotationIndex.set(-1)
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
            reportDroppedTraceEvent: Boolean,
            scratchTracePacket: MutableTracePacket,
            scratchTrackDescriptor: MutableTrackDescriptor,
            scratchTrackEvent: MutableTrackEvent,
            scratchAnnotations: MutableList<MutableDebugAnnotation>,
            scratchAnnotationIndex: AtomicInteger,
        ) {
            scratchTracePacket.timestamp = event.timestamp
            // in the common case when the track_descriptor isn't needed, clear it on the
            // MutableTracePacket
            scratchTracePacket.track_event = null
            scratchTracePacket.track_descriptor = null
            scratchTracePacket.previous_packet_dropped = reportDroppedTraceEvent

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
                            scratchTrackDescriptor.name = DEFAULT_STRING
                            scratchTrackDescriptor.uuid = uuid
                            scratchTrackDescriptor.process =
                                MutableProcessDescriptor(pid = pid, process_name = name)
                        }
                        TRACK_DESCRIPTOR_TYPE_THREAD -> {
                            scratchTrackDescriptor.name = DEFAULT_STRING
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
                scratchTrackEvent.type = MutableTrackEvent.Type.fromValue(event.type)!!
                scratchTrackEvent.track_uuid = event.trackUuid
                scratchTrackEvent.name = event.name
                scratchTrackEvent.counter_value = event.counterLongValue
                scratchTrackEvent.double_counter_value = event.counterDoubleValue
                scratchTrackEvent.flow_ids = event.flowIds
                // Categories
                if (
                    // Only has a primary category
                    event.primaryCategory.isNotEmpty() &&
                        event.lastCategoryIndex <= LAST_INDEX_WHEN_EMPTY
                ) {
                    event.categories[0] = event.primaryCategory
                    event.lastCategoryIndex += 1
                    scratchTrackEvent.categories =
                        event.categories.subList(fromIndex = 0, toIndex = 1)
                } else if (event.lastCategoryIndex > LAST_INDEX_WHEN_EMPTY) {
                    // Has primary and secondary categories
                    // Add the primary category
                    event.lastCategoryIndex += 1
                    if (event.lastCategoryIndex >= event.categories.size) {
                        event.categories += DEFAULT_STRING
                    }
                    event.categories[event.lastCategoryIndex] = event.primaryCategory
                    // Categories should only be set when we actually have incoming categories
                    scratchTrackEvent.categories =
                        event.categories.subList(
                            fromIndex = 0,
                            toIndex = event.lastCategoryIndex + 1,
                        )
                } else {
                    scratchTrackEvent.categories = emptyList()
                }
                // Debug annotations
                var index = -1
                event.forEachMetadataEntry { metadataEntry ->
                    index += 1
                    if (index >= scratchAnnotations.size) {
                        scratchAnnotations += MutableDebugAnnotation()
                    }
                    val debugAnnotation = scratchAnnotations[index]
                    debugAnnotation.name = metadataEntry.name
                    when (metadataEntry.type) {
                        METADATA_TYPE_BOOLEAN ->
                            debugAnnotation.bool_value = metadataEntry.booleanValue

                        METADATA_TYPE_LONG -> debugAnnotation.int_value = metadataEntry.longValue

                        METADATA_TYPE_DOUBLE ->
                            debugAnnotation.double_value = metadataEntry.doubleValue

                        METADATA_TYPE_STRING ->
                            debugAnnotation.string_value = metadataEntry.stringValue

                        else -> {
                            // Should never happen
                        }
                    }
                }
                scratchAnnotationIndex.set(index)
                if (index >= 0) {
                    // The actual usable annotations in the pool.
                    // The actual resizing happens once we have finished the write.
                    val debugAnnotations = scratchAnnotations.subList(0, index + 1)
                    scratchTrackEvent.debug_annotations = debugAnnotations
                }
                // Update trace packet
                scratchTracePacket.track_event = scratchTrackEvent
            }
        }
    }
}
