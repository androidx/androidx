/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A pool of accounted memory, which supports acquiring and releasing a memory chunk. The current
 * capacity can be queried using the [capacityFlow]. It also accounts for evictable memory, i.e. a
 * memory block that can be released safely when needed without affecting any external resource in
 * use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MemoryEstimator {
    /** Flow of the total memory usage. */
    public val memoryUsage: StateFlow<Long>

    /** Flow of available capacity. */
    public val capacityFlow: Flow<Long>

    /** Flow of total evictable memory usage. */
    public val evictableMemory: StateFlow<Long>

    /**
     * The total available memory at this point of time. This also includes the memory that is
     * evictable.
     */
    public val availableMemory: Long

    /**
     * Acquires a resource of given size, irrespective of the available capacity. This can cause the
     * capacity to temporarily go negative.
     */
    public fun incrementUsage(size: Long)

    /** Releases a resource of given size back to the pool. */
    public fun decrementUsage(size: Long)

    /** Increment the amount of evictable memory in bytes. */
    public fun incrementEvictableBytes(size: Long)

    /** Decrement the amount of evictable memory in bytes. */
    public fun decrementEvictableBytes(size: Long)

    /**
     * Checks if the current capacity is large enough for an immediate allocation of a given size.
     * Note that this doesn't factor in evictable memory, since this meant to decide if memory is
     * available right away, without any other process cleaning up the evictable memory.
     */
    public fun canAllocateNow(size: Long): Boolean

    public companion object {
        @JvmStatic
        /**
         * Create an instance of [MemoryEstimator] with a given totalCapacity bound. The default is
         * unbounded.
         */
        public fun create(initialCapacity: Long = Long.MAX_VALUE): MemoryEstimator =
            MemoryEstimatorImpl(initialCapacity)
    }
}

internal class MemoryEstimatorImpl(private val initialCapacity: Long) : MemoryEstimator {
    private val _usage = MutableStateFlow(0L)
    override val memoryUsage: StateFlow<Long> = _usage.asStateFlow()

    override val capacityFlow: Flow<Long> = _usage.map { usage -> initialCapacity - usage }

    private val _evictable = MutableStateFlow(0L)
    override val evictableMemory: StateFlow<Long> = _evictable.asStateFlow()

    override val availableMemory: Long
        get() = initialCapacity - _usage.value + _evictable.value

    override fun incrementUsage(size: Long) {
        _usage.update { current -> current + size }
    }

    override fun decrementUsage(size: Long) {
        _usage.update { current -> current - size }
    }

    override fun incrementEvictableBytes(size: Long) {
        _evictable.update { current -> current + size }
    }

    override fun decrementEvictableBytes(size: Long) {
        _evictable.update { current -> current - size }
    }

    override fun canAllocateNow(size: Long): Boolean {
        return (initialCapacity - _usage.value) >= size
    }
}
