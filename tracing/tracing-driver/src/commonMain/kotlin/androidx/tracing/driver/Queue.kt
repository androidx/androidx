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

internal const val QUEUE_CAPACITY = 64

/** An actual thread safe queue implementation. */
internal class Queue<T>(capacity: Int = QUEUE_CAPACITY) {
    private val lock = Lock()
    private val queue: ArrayDeque<T> = ArrayDeque(capacity)

    internal fun isEmpty(): Boolean {
        return lock.withLock { queue.isEmpty() }
    }

    internal fun isNotEmpty(): Boolean {
        return lock.withLock { queue.isNotEmpty() }
    }

    internal val size
        get() = { lock.withLock { queue.size } }

    internal fun addFirst(value: T) {
        lock.withLock { queue.addFirst(value) }
    }

    internal fun removeFirstOrNull(): T? {
        return lock.withLock { queue.removeFirstOrNull() }
    }
}
