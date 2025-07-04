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

public class PooledTracePacketArray
internal constructor(
    owner: Pool<PooledTracePacketArray>,

    /**
     * Array of packets, all pre-allocated, and never modified.
     *
     * This is an Array to simplify data access.
     */
    @JvmField public val packets: Array<TraceEvent>,

    /**
     * Number of items present in [packets] with valid data - all others have been reset with
     * [TraceEvent.reset]
     */
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var fillCount: Int,
) : Poolable<PooledTracePacketArray>(owner) {
    public inline fun forEach(block: (packet: TraceEvent) -> Unit) {
        repeat(fillCount) { block(packets[it]) }
    }

    override fun recycle() {
        forEach { it.reset() }
        fillCount = 0
        owner.release(this)
    }
}
