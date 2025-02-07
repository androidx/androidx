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

package androidx.tracing.driver

import com.squareup.wire.ProtoWriter
import okio.ByteString
import perfetto.protos.MutableCounterDescriptor
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableThreadDescriptor
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

// Pooled versions of the underlying protos being used.

internal class PooledTrackEvent(owner: Pool<PooledTrackEvent>, val trackEvent: MutableTrackEvent) :
    Poolable<PooledTrackEvent>(owner) {
    override fun recycle() {
        trackEvent.name_iid = null
        trackEvent.name = null
        trackEvent.type = null
        trackEvent.track_uuid = null
        trackEvent.counter_value = null
        trackEvent.double_counter_value = null
        trackEvent.unknownFields = ByteString.EMPTY
        trackEvent.categories = emptyList()
        trackEvent.extra_counter_track_uuids = emptyList()
        trackEvent.extra_counter_values = emptyList()
        trackEvent.extra_double_counter_track_uuids = emptyList()
        trackEvent.extra_double_counter_values = emptyList()
        trackEvent.flow_ids = emptyList()
        owner.release(this)
    }
}

public class PooledTracePacket
internal constructor(
    owner: Pool<PooledTracePacket>,
    // Internal for testing
    internal val tracePacket: MutableTracePacket,
    // We are keeping track of some associated Poolables this way, so they can consistently
    // be recycled correctly. A size of `4` ought to be big enough for our needs.
    private val nested: Array<Poolable<*>?> = arrayOfNulls(4)
) : Poolable<PooledTracePacket>(owner) {
    private var nestedIndex = 0

    override fun recycle() {
        for (i in 0 until nestedIndex) {
            val poolable = nested[i]
            poolable?.recycle()
            nested[i] = null
        }
        nestedIndex = 0
        tracePacket.timestamp = INVALID_LONG
        tracePacket.timestamp_clock_id = null
        tracePacket.track_event = null
        tracePacket.track_descriptor = null
        tracePacket.trace_uuid = null
        tracePacket.compressed_packets = null
        tracePacket.trusted_packet_sequence_id = null
        tracePacket.interned_data = null
        tracePacket.sequence_flags = null
        tracePacket.incremental_state_cleared = null
        tracePacket.trace_packet_defaults = null
        tracePacket.unknownFields = ByteString.EMPTY
        owner.release(this)
    }

    internal fun trackPoolableForOwnership(poolable: Poolable<*>) {
        nested[nestedIndex] = poolable
        nestedIndex += 1
    }

    public fun encodeTracePacket(writer: ProtoWriter) {
        MutableTracePacket.ADAPTER.encodeWithTag(writer, 1, tracePacket)
    }
}

internal class PooledTrackDescriptor(
    owner: Pool<PooledTrackDescriptor>,
    val trackDescriptor: MutableTrackDescriptor
) : Poolable<PooledTrackDescriptor>(owner) {
    override fun recycle() {
        trackDescriptor.uuid = null
        trackDescriptor.name = null
        trackDescriptor.parent_uuid = null
        trackDescriptor.process = null
        trackDescriptor.thread = null
        trackDescriptor.counter = null
        trackDescriptor.disallow_merging_with_system_tracks = null
        trackDescriptor.unknownFields = ByteString.EMPTY
        owner.release(this)
    }
}

internal class PooledProcessDescriptor(
    owner: Pool<PooledProcessDescriptor>,
    val processDescriptor: MutableProcessDescriptor
) : Poolable<PooledProcessDescriptor>(owner) {
    override fun recycle() {
        processDescriptor.pid = INVALID_INT
        processDescriptor.cmdline = emptyList()
        processDescriptor.process_name = null
        processDescriptor.process_labels = emptyList()
        processDescriptor.unknownFields = ByteString.EMPTY
        owner.release(this)
    }
}

internal class PooledThreadDescriptor(
    owner: Pool<PooledThreadDescriptor>,
    val threadDescriptor: MutableThreadDescriptor
) : Poolable<PooledThreadDescriptor>(owner) {
    override fun recycle() {
        threadDescriptor.pid = INVALID_INT
        threadDescriptor.tid = INVALID_INT
        threadDescriptor.thread_name = null
        threadDescriptor.unknownFields = ByteString.EMPTY
        owner.release(this)
    }
}

internal class PooledCounterDescriptor(
    owner: Pool<PooledCounterDescriptor>,
    val counterDescriptor: MutableCounterDescriptor
) : Poolable<PooledCounterDescriptor>(owner) {
    override fun recycle() {
        counterDescriptor.type = null
        counterDescriptor.categories = emptyList()
        counterDescriptor.unit = null
        counterDescriptor.unit_name = null
        counterDescriptor.unit_multiplier = null
        counterDescriptor.is_incremental = null
        counterDescriptor.unknownFields = ByteString.EMPTY
        owner.release(this)
    }
}

public class PooledTracePacketArray
internal constructor(
    owner: Pool<PooledTracePacketArray>,
    @get:SuppressWarnings("NullableCollectionElement") // Object pooling to avoid allocations
    public val pooledTracePacketArray: Array<PooledTracePacket?>
) : Poolable<PooledTracePacketArray>(owner) {
    override fun recycle() {
        for (i in 0 until pooledTracePacketArray.size) {
            // Don't recycle the underlying tracePacket because that will happen
            // once we serialize the packet to the ProtoStream.
            pooledTracePacketArray[i] = null
        }
        owner.release(this)
    }
}
