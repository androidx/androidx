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

internal abstract class IndexBasedArrayIterator<T>(startingSize: Int) : MutableIterator<T> {

    private var size = startingSize
    private var index = 0
    private var canRemove = false

    protected abstract fun elementAt(index: Int): T
    protected abstract fun removeAt(index: Int)

    override fun hasNext(): Boolean = index < size

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val res = elementAt(index)
        index++
        canRemove = true
        return res
    }

    override fun remove() {
        check(canRemove) { "Call next() before removing an element." }
        // Attempt removal first so an UnsupportedOperationException retains a valid state.
        removeAt(--index)
        size--
        canRemove = false
    }
}
