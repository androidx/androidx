/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation

/**
 * A list optimized for containment queries.
 *
 * Consider using an [ObserverList] instead of a [List] when the number of
 * [contains] calls dominates the number of [add] and [remove] calls.
 * TODO(ianh): Use DelegatingIterable, possibly moving it from the collection
 * package to foundation, or to dart:collection.
 */
class ObserverList<T> : Collection<T> {

    private val list = mutableListOf<T>()
    private var isDirty = false
    private var set: MutableSet<T>? = null

    /** Adds an item to the end of this list. */
    fun add(item: T) {
        isDirty = true
        list.add(item)
    }

    /**
     * Removes an item from the list.
     *
     * This is O(N) in the number of items in the list.
     *
     * Returns whether the item was present in the list.
     */
    fun remove(item: T): Boolean {
        isDirty = true
        return list.remove(item)
    }

    override fun contains(element: T): Boolean {
        if (list.size < 3)
            return list.contains(element)

        if (isDirty) {
            val valSet = set
            if (valSet == null) {
                set = HashSet(list)
            } else {
                valSet.clear()
                valSet.addAll(list)
            }
            isDirty = false
        }

        return set!!.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach { if (!contains(it)) return false }
        return true
    }

    override fun iterator() = list.iterator()

    override val size: Int
        get() = list.size

    override fun isEmpty() = list.isEmpty()
}