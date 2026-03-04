/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.latency.aggregators.internal

import android.util.Log
import androidx.annotation.VisibleForTesting
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A concurrent queue of [Interval]s, with a behind-the-scenes preallocated pool to avoid new
 * allocations. To write into the queue, use [record]. To read and manipulate recorded intervals,
 * use either [applyToOldestAndRecycle] or [recycleOldest].
 */
internal class ConcurrentIntervalQueue(val numPreallocatedIntervals: Int) {
    /** Intervals recorded by [record]. */
    private val intervals = ConcurrentLinkedQueue<Interval>()

    /** Pool of recycled Interval instances for populating the active queue. */
    private val intervalPool = ConcurrentLinkedQueue<Interval>()

    /** Number of times [record] had to allocate a new [Interval]. */
    @VisibleForTesting
    internal var numLateAllocations = 0
        private set

    init {
        repeat(numPreallocatedIntervals) { intervalPool.add(Interval()) }
    }

    /** Records a single interval and puts it in the queue. */
    fun record(startNanos: Long, endNanos: Long) {
        val interval =
            intervalPool.poll()
                ?: Interval().also {
                    numLateAllocations += 1
                    Log.w(
                        this::class.simpleName,
                        "Pool is empty; allocating an Interval. Did you preallocate enough? Did you recycle?",
                    )
                }
        interval.startNanos = startNanos
        interval.endNanos = endNanos
        intervals.offer(interval)
    }

    /** Whether the queue is empty. */
    fun isEmpty(): Boolean = intervals.isEmpty()

    /** The number of intervals in the queue. */
    fun size(): Int = intervals.size

    /**
     * Gets the oldest interval, applies [action] to it, and recycles it. If the queue is empty,
     * this is a no-op. Marked `inline` so that if [action] calls (or is) a `suspend` function, it
     * will adopt the coroutine context from the call site of this function.
     */
    inline fun applyToOldestAndRecycle(action: (Interval) -> Unit) {
        intervals.poll()?.let {
            action(it)
            intervalPool.offer(it)
        }
    }

    /**
     * Recycles the oldest interval without acting on it. If the queue is empty, this is a no-op.
     */
    fun recycleOldest() {
        intervals.poll()?.let { intervalPool.offer(it) }
    }

    /** A span of time between two nanosecond timestamps with a common time base. */
    data class Interval(var startNanos: Long = Long.MIN_VALUE, var endNanos: Long = Long.MIN_VALUE)
}
