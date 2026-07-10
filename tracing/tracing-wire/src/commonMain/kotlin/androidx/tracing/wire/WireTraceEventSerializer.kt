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

import androidx.tracing.ATTRIBUTES_EXPECTED_SIZE
import androidx.tracing.DEFAULT_LONG
import androidx.tracing.DEFAULT_STRING
import androidx.tracing.FRAMES_EXPECTED_SIZE
import androidx.tracing.LAST_CATEGORY_INDEX
import androidx.tracing.METADATA_ENTRIES_EXPECTED_SIZE
import androidx.tracing.METADATA_TYPE_BOOLEAN
import androidx.tracing.METADATA_TYPE_DOUBLE
import androidx.tracing.METADATA_TYPE_LONG
import androidx.tracing.METADATA_TYPE_STRING
import androidx.tracing.TRACK_DESCRIPTOR_TYPE_COUNTER
import androidx.tracing.TRACK_DESCRIPTOR_TYPE_PROCESS
import androidx.tracing.TRACK_DESCRIPTOR_TYPE_THREAD
import androidx.tracing.TraceEvent
import androidx.tracing.wire.protos.MutableCallstack
import androidx.tracing.wire.protos.MutableCounterDescriptor
import androidx.tracing.wire.protos.MutableDebugAnnotation
import androidx.tracing.wire.protos.MutableProcessDescriptor
import androidx.tracing.wire.protos.MutableThreadDescriptor
import androidx.tracing.wire.protos.MutableTraceAttributes
import androidx.tracing.wire.protos.MutableTracePacket
import androidx.tracing.wire.protos.MutableTrackDescriptor
import androidx.tracing.wire.protos.MutableTrackEvent
import com.squareup.wire.ProtoWriter

/**
 * Optimized serializer of [androidx.tracing.TraceEvent], which writes out binary Perfetto
 * trace_packet.proto with minimal allocations
 *
 * Internally uses mutable protos to avoid allocations / GC churn.
 */
internal class WireTraceEventSerializer(sequenceId: Int) {
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
    private val scratchAnnotationIndex = IntArray(1) { _ -> -1 }

    /** Scratch call stack */
    private var scratchCallStack = MutableCallstack()

    /** Private scratchpad of callstack information. */
    private var scratchFrames: MutableList<MutableCallstack.MutableFrame> =
        MutableList(size = FRAMES_EXPECTED_SIZE) { MutableCallstack.MutableFrame() }

    /** This is passed by ref, to avoid unnecessary computation. */
    private val scratchFrameIndex = IntArray(1) { _ -> -1 }

    /** Scratch trace attributes. */
    private val scratchTraceAttributes = MutableTraceAttributes()

    /** Private scratchpad of trace attribute information. */
    private var scratchAttributes: MutableList<MutableTraceAttributes.MutableAttribute> =
        MutableList(size = ATTRIBUTES_EXPECTED_SIZE) { MutableTraceAttributes.MutableAttribute() }

    /** This is passed by ref, to avoid unnecessary computation. */
    private val scratchAttributeIndex = IntArray(1) { _ -> -1 }

    /**
     * Private scratchpad descriptor, used to avoid allocating a descriptor for each new track
     * created
     */
    private val scratchTrackDescriptor = MutableTrackDescriptor()

    private val scratchTrackEvent = MutableTrackEvent(track_uuid = DEFAULT_LONG)

    /**
     * Currently trace attributes are a part of the [TraceEvent]. This is being done, given it
     * allows for use-cases where attributes are not emitted at `TraceDriver` init. Instead, they
     * can be emitted later. This typically comes up in apps where experiments and feature flags
     * need to be sync-ed with a server (network requests, FCM) so the state of these attributes can
     * change during the lifetime of the process. This way we can capture the last known value of
     * the attribute.
     *
     * This API does not currently exist on [androidx.tracing.Tracer], but it can in the future.
     */
    fun writeTraceEvent(
        protoWriter: ProtoWriter,
        event: TraceEvent,
        reportDroppedTraceEvent: Boolean = false,
    ) {
        updateScratchPacketFromTraceEvent(
            event = event,
            reportDroppedTraceEvent = reportDroppedTraceEvent,
            scratchTracePacket = scratchTracePacket,
            scratchTrackDescriptor = scratchTrackDescriptor,
            scratchTrackEvent = scratchTrackEvent,
            scratchAnnotations = scratchAnnotations,
            scratchAnnotationIndex = scratchAnnotationIndex,
            scratchCallStack = scratchCallStack,
            scratchFrames = scratchFrames,
            scratchFrameIndex = scratchFrameIndex,
            scratchTraceAttributes = scratchTraceAttributes,
            scratchAttributes = scratchAttributes,
            scratchAttributeIndex = scratchAttributeIndex,
        )
        MutableTracePacket.Companion.ADAPTER.encodeWithTag(
            writer = protoWriter,
            tag = 1,
            value = scratchTracePacket,
        )
        resetScratchAnnotations()
        resetScratchFrames()
        resetTraceAttributes()
    }

    /** Reset and resize scratch annotations when necessary. */
    @Suppress("NOTHING_TO_INLINE")
    inline fun resetScratchAnnotations() {
        val index = scratchAnnotationIndex[0]
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
        scratchAnnotationIndex[0] = -1
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun resetScratchFrames() {
        if (scratchCallStack.frames.isNotEmpty()) {
            scratchCallStack.frames = emptyList()
        }
        val index = scratchFrameIndex[0]
        val size = index + 1
        // Reset
        repeat(size) {
            val scratchFrame = scratchFrames[it]
            scratchFrame.function_name = null
            scratchFrame.source_file = null
            scratchFrame.line_number = null
        }
        // Resize
        if (size > FRAMES_EXPECTED_SIZE) {
            scratchFrames = scratchFrames.subList(0, FRAMES_EXPECTED_SIZE)
        }
        scratchFrameIndex[0] = -1
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun resetTraceAttributes() {
        if (scratchTraceAttributes.attribute.isNotEmpty()) {
            scratchTraceAttributes.attribute = emptyList()
        }
        val index = scratchAttributeIndex[0]
        val size = index + 1
        // Reset
        repeat(size) {
            val scratchAttribute = scratchAttributes[it]
            scratchAttribute.key = null
            scratchAttribute.string_value = null
            scratchAttribute.long_value = null
        }
        // Resize
        if (size > ATTRIBUTES_EXPECTED_SIZE) {
            scratchAttributes = scratchAttributes.subList(0, ATTRIBUTES_EXPECTED_SIZE)
        }
        scratchAttributeIndex[0] = -1
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
            scratchAnnotationIndex: IntArray,
            scratchCallStack: MutableCallstack,
            scratchFrames: MutableList<MutableCallstack.MutableFrame>,
            scratchFrameIndex: IntArray,
            scratchTraceAttributes: MutableTraceAttributes,
            scratchAttributes: MutableList<MutableTraceAttributes.MutableAttribute>,
            scratchAttributeIndex: IntArray,
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
                scratchTrackEvent.correlation_id = event.correlationId
                scratchTrackEvent.correlation_id_str = event.correlationIdString
                scratchTrackEvent.flow_ids = event.flowIds
                // Categories
                if (
                    event.primaryCategory.isNotEmpty() &&
                        event.lastCategoryIndex <= LAST_CATEGORY_INDEX
                ) {
                    // Only has a primary category
                    event.categories[0] = event.primaryCategory
                    scratchTrackEvent.categories =
                        event.categories.subList(fromIndex = 0, toIndex = 1)
                } else if (event.lastCategoryIndex > LAST_CATEGORY_INDEX) {
                    // Has primary and secondary categories
                    event.categories[0] = event.primaryCategory
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
                scratchAnnotationIndex[0] = index
                if (index >= 0) {
                    // The actual usable annotations in the pool.
                    // The actual resizing happens once we have finished writing.
                    val debugAnnotations = scratchAnnotations.subList(0, index + 1)
                    scratchTrackEvent.debug_annotations = debugAnnotations
                }
                // Call Stack Frames
                index = -1
                repeat(event.lastFrameIndex + 1) {
                    index += 1
                    if (index >= scratchFrames.size) {
                        scratchFrames += MutableCallstack.MutableFrame()
                    }
                    val frameEntry = event.frames[it]
                    val frame = scratchFrames[it]
                    frame.function_name = frameEntry.name
                    frame.source_file = frameEntry.sourceFile
                    frame.line_number = frameEntry.lineNumber
                }
                scratchFrameIndex[0] = index
                if (index >= 0) {
                    // The actual number of frames in the pool
                    // The actual resizing happens once we have finished writing.
                    val frames = scratchFrames.subList(0, index + 1)
                    scratchCallStack.frames = frames
                    scratchTrackEvent.callstack = scratchCallStack
                } else {
                    scratchTrackEvent.callstack = null
                }
                // Trace Attributes
                index = -1
                repeat(event.lastAttributeIndex + 1) {
                    index += 1
                    if (index >= scratchAttributes.size) {
                        scratchAttributes += MutableTraceAttributes.MutableAttribute()
                    }
                    val attributeEntry = event.attributes[it]
                    val attribute = scratchAttributes[it]
                    attribute.key = attributeEntry.name
                    attribute.string_value = attributeEntry.stringValue
                    attribute.long_value = attributeEntry.longValue
                }
                scratchAttributeIndex[0] = index
                if (index >= 0) {
                    // The actual number of attributes in the pool
                    // The actual resizing happens once we have finished writing.
                    val traceAttributes = scratchAttributes.subList(0, index + 1)
                    scratchTraceAttributes.attribute = traceAttributes
                    scratchTracePacket.trace_attributes = scratchTraceAttributes
                } else {
                    scratchTracePacket.trace_attributes = null
                }
                // Update trace packet
                scratchTracePacket.track_event = scratchTrackEvent
            }
        }
    }
}
