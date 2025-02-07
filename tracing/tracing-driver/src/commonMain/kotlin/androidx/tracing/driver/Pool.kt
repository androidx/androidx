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

/**
 * An Object pool that keeps track of scrap objects in a fixed size [ArrayDeque].
 *
 * @param size represents the size of the Object pool.
 * @param factory is a function that can be used to create the instances of the Object for the pool.
 * @param T represents an object that be re-used.
 */
internal class Pool<T>(
    private val size: Int,
    private val isDebug: Boolean,
    private val factory: (owner: Pool<T>) -> T
) {
    private var counter: AtomicLong? = null

    init {
        if (isDebug) {
            counter = AtomicLong(0L)
        }
    }

    // This class is intentionally lock free.
    // This is because, the only place where we recycle objects in the pool is in the TraceSink
    // and that effectively behaves as-if it were single threaded.
    private val scrapPool: ArrayDeque<T> = ArrayDeque(size)

    init {
        // Eagerly create the objects for the pool
        for (i in 0 until size) {
            scrapPool.addFirst(factory(this))
        }
    }

    /** Obtain an instance of the object from the pool. */
    internal fun obtain(): T {
        if (isDebug) {
            counter?.incrementAndGet()
        }
        return scrapPool.removeFirstOrNull() ?: factory(this)
    }

    internal fun release(element: T) {
        if (isDebug) {
            counter?.decrement()
        }
        if (scrapPool.size < size) {
            scrapPool.addFirst(element)
        }
    }

    internal fun count(): Long {
        return counter?.get() ?: 0L
    }
}
