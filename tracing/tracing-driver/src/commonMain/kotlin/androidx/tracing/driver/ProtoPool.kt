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

// The size of the array
// This would mean that each pool can queue up to 32 * 32 trace packets
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACE_PACKET_BUFFER_SIZE: Int = 32
// The size of the pool
private const val TRACE_PACKET_POOL_ARRAY_POOL_SIZE = 32

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val INVALID_INT: Int = -1

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val INVALID_LONG: Long = -1L

/** The uber proto pool that knows how to create all the necessary protos. */
internal class ProtoPool(internal val isDebug: Boolean) {
    internal val tracePacketArrayPool: Pool<PooledTracePacketArray> =
        Pool(size = TRACE_PACKET_POOL_ARRAY_POOL_SIZE, isDebug = isDebug) { pool ->
            PooledTracePacketArray(
                owner = pool,
                packets = Array(TRACE_PACKET_BUFFER_SIZE) { TraceEvent() },
                fillCount = 0,
            )
        }

    fun obtainTracePacketArray(): PooledTracePacketArray {
        return tracePacketArrayPool.obtain()
    }

    // Debug only
    fun poolableCount(): Long {
        if (!isDebug) {
            return 0L
        }

        return tracePacketArrayPool.count()
    }
}
