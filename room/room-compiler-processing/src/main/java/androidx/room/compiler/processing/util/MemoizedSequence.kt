/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.util

/**
 * A [Sequence] implementation that caches values so that another collector can avoid
 * re-computing the source sequence.
 *
 * Note that collecting on these sequence is not thread safe.
 */
internal class MemoizedSequence<T>(
    private val buildSequence: () -> Sequence<T>
) : Sequence<T> {

    /**
     * Shared cache between iterators.
     */
    private val cache = mutableListOf<T>()

    private val delegateIterator by lazy {
        buildSequence().iterator()
    }

    private inner class CachedIterator : Iterator<T> {
        private var yieldedCount = 0
        override fun hasNext(): Boolean {
            return yieldedCount < cache.size || delegateIterator.hasNext()
        }

        override fun next(): T {
            if (cache.size == yieldedCount) {
                // no point in checking hasNext, let it throw if we get a next() call without a
                // previous hasNext call.
                cache.add(delegateIterator.next())
            }
            return cache[yieldedCount].also {
                yieldedCount++
            }
        }
    }

    override fun iterator(): Iterator<T> {
        return CachedIterator()
    }
}
