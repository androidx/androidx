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

import androidx.annotation.RestrictTo
import perfetto.protos.MutableCounterDescriptor
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableThreadDescriptor
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

// This is 4 * the total number of outstanding requests that can be emitted by a track.
private const val TRACK_EVENT_POOL_SIZE = 2048
private const val TRACE_PACKET_POOL_SIZE = 2048
private const val TRACK_DESCRIPTOR_POOL_SIZE = 256
private const val PROCESS_DESCRIPTOR_POOL_SIZE = 2
private const val THREAD_DESCRIPTOR_POOL_SIZE = 4
private const val COUNTER_DESCRIPTOR_POOL_SIZE = 4
// The size of the array
// This would mean that each pool can queue up to 32 * 32 trace packets

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACE_PACKET_BUFFER_SIZE: Int = 32
// The size of the pool
private const val TRACE_PACKET_POOL_ARRAY_POOL_SIZE = 32

internal const val INVALID_INT = -1
internal const val INVALID_LONG = -1L

/** The uber proto pool that knows how to create all the necessary protos. */
internal class ProtoPool(internal val isDebug: Boolean) {

    internal val tracePacketPool: Pool<PooledTracePacket> =
        Pool(size = TRACE_PACKET_POOL_SIZE, isDebug = isDebug) { pool ->
            PooledTracePacket(
                owner = pool,
                tracePacket = MutableTracePacket(timestamp = INVALID_LONG),
            )
        }

    internal val tracePacketArrayPool: Pool<PooledTracePacketArray> =
        Pool(size = TRACE_PACKET_POOL_ARRAY_POOL_SIZE, isDebug = isDebug) { pool ->
            PooledTracePacketArray(
                owner = pool,
                pooledTracePacketArray = arrayOfNulls(TRACE_PACKET_BUFFER_SIZE)
            )
        }

    internal val trackDescriptorPool =
        Pool<PooledTrackDescriptor>(size = TRACK_DESCRIPTOR_POOL_SIZE, isDebug = isDebug) { pool ->
            PooledTrackDescriptor(owner = pool, trackDescriptor = MutableTrackDescriptor())
        }

    internal val processDescriptorPool =
        Pool<PooledProcessDescriptor>(size = PROCESS_DESCRIPTOR_POOL_SIZE, isDebug = isDebug) { pool
            ->
            PooledProcessDescriptor(
                owner = pool,
                processDescriptor = MutableProcessDescriptor(pid = INVALID_INT),
            )
        }

    internal val threadDescriptorPool =
        Pool<PooledThreadDescriptor>(size = THREAD_DESCRIPTOR_POOL_SIZE, isDebug = isDebug) { pool
            ->
            PooledThreadDescriptor(
                owner = pool,
                threadDescriptor = MutableThreadDescriptor(pid = INVALID_INT, tid = INVALID_INT),
            )
        }

    internal val counterDescriptorPool =
        Pool<PooledCounterDescriptor>(size = COUNTER_DESCRIPTOR_POOL_SIZE, isDebug = isDebug) { pool
            ->
            PooledCounterDescriptor(owner = pool, counterDescriptor = MutableCounterDescriptor())
        }

    internal val trackEventPool =
        Pool<PooledTrackEvent>(TRACK_EVENT_POOL_SIZE, isDebug = isDebug) { pool ->
            PooledTrackEvent(owner = pool, trackEvent = MutableTrackEvent())
        }

    fun obtainTracePacket(): PooledTracePacket {
        val packet = tracePacketPool.obtain()
        // Always update time when dealing with recycled packets
        // This is only being done because `timestamp` is now a required field to avoid boxing.
        packet.tracePacket.timestamp = nanoTime()
        return packet
    }

    fun obtainTracePacketArray(): PooledTracePacketArray {
        return tracePacketArrayPool.obtain()
    }

    fun obtainTrackDescriptor(): PooledTrackDescriptor {
        return trackDescriptorPool.obtain()
    }

    fun obtainProcessDescriptor(): PooledProcessDescriptor {
        return processDescriptorPool.obtain()
    }

    fun obtainThreadDescriptor(): PooledThreadDescriptor {
        return threadDescriptorPool.obtain()
    }

    fun obtainCounterDescriptor(): PooledCounterDescriptor {
        return counterDescriptorPool.obtain()
    }

    fun obtainTrackEvent(): PooledTrackEvent {
        return trackEventPool.obtain()
    }

    // Debug only
    fun poolableCount(): Long {
        if (!isDebug) {
            return 0L
        }

        var count = 0L
        count += tracePacketPool.count()
        count += tracePacketArrayPool.count()
        count += trackDescriptorPool.count()
        count += processDescriptorPool.count()
        count += threadDescriptorPool.count()
        count += counterDescriptorPool.count()
        count += trackEventPool.count()
        return count
    }
}
