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

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo

internal const val QUEUE_INITIAL_CAPACITY = 64

/** An actual thread safe queue implementation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Queue<T>(capacity: Int = QUEUE_INITIAL_CAPACITY) {
    private val queue: ArrayDeque<T> = ArrayDeque(capacity)

    @GuardedBy("queue") private var droppedTraceEvent: Boolean = false

    public fun isEmpty(): Boolean {
        return synchronized(queue) { queue.isEmpty() }
    }

    public fun isNotEmpty(): Boolean {
        return synchronized(queue) { queue.isNotEmpty() }
    }

    public val size: Int
        get() {
            return synchronized(queue) { queue.size }
        }

    public fun addLast(value: T) {
        synchronized(queue) { queue.addLast(value) }
    }

    public fun setDroppedTraceEvent(droppedTraceEvent: Boolean) {
        synchronized(queue) { this.droppedTraceEvent = droppedTraceEvent }
    }

    public val isDroppedTraceEvent: Boolean
        get() = synchronized(queue) { droppedTraceEvent }

    public fun firstOrNull(): T? {
        return synchronized(queue) { queue.firstOrNull() }
    }

    public fun removeFirst() {
        return synchronized(queue) { queue.removeFirst() }
    }
}
