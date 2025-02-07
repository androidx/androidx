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

import androidx.annotation.RestrictTo

/** Entities that we can attach traces to. */
public abstract class Track(
    /** The [TraceContext] instance. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val context: TraceContext,
    /** `true` iff we need to emit some preamble packets. */
    internal val hasPreamble: Boolean,
    /** The uuid for the track descriptor. */
    internal val uuid: Long,
    /** The parent traceable. */
    private val parent: Track?
) {
    /**
     * Any time we emit trace packets relevant to this process. We need to make sure the necessary
     * preamble packets that describe the process and threads are also emitted. This is used to make
     * sure that we only do that once.
     */
    private val preamble: AtomicBoolean = AtomicBoolean(false)
    private val flushRequested = AtomicBoolean(false)
    // Every poolable that is obtained from the pool, keeps track of its owner.
    // The underlying poolable, if eventually recycled by the Sink after an emit() is complete.
    internal val pool: ProtoPool = ProtoPool(isDebug = context.isDebug)
    private val queue: ArrayDeque<PooledTracePacket> = ArrayDeque(TRACE_PACKET_BUFFER_SIZE * 2)

    /** @return The [PooledTracePacket] which is a preamble packet for the [Track]. */
    public abstract fun preamblePacket(): PooledTracePacket?

    internal fun emitPreamble() {
        parent?.emitPreamble()
        if (preamble.compareAndSet(expected = false, newValue = true)) {
            val packet = preamblePacket()
            if (packet != null) {
                emit(packet)
            }
        }
    }

    internal fun flush() {
        if (flushRequested.compareAndSet(expected = false, newValue = true)) {
            transferPooledPacketArray()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun emit(packet: PooledTracePacket) {
        emitPreamble()
        queue.add(packet)
        if (queue.size >= TRACE_PACKET_BUFFER_SIZE) {
            flush()
        }
    }

    private fun transferPooledPacketArray() {
        do {
            val pooledPacketArray = pool.obtainTracePacketArray()
            var i = 0
            while (queue.isNotEmpty() && i < pooledPacketArray.pooledTracePacketArray.size) {
                val pooledTracePacket = queue.removeFirstOrNull()
                if (pooledTracePacket != null) {
                    pooledPacketArray.pooledTracePacketArray[i] = pooledTracePacket
                    i += 1
                }
            }
            context.sink.emit(pooledPacketArray)
        } while (queue.isNotEmpty()) // There might still be more packets.
        flushRequested.set(newValue = false)
    }
}
