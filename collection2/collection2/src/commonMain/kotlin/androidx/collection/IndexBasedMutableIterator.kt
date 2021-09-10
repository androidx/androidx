/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.collection

internal abstract class IndexBasedMutableIterator<E>(
    size: Int
) : MutableIterator<E> {
    protected var index = -1
    private var end = size - 1
    private var canRemove = false

    abstract fun get(index: Int): E
    abstract fun remove(index: Int)

    override fun hasNext() = index < end

    override fun next(): E {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val value = get(++index)
        canRemove = true
        return value
    }

    override fun remove() {
        check(canRemove)

        // Attempt removal first so an UnsupportedOperationException retains a valid state.
        remove(index--)
        end--
        canRemove = false
    }
}
