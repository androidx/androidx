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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.WeakReference
import androidx.compose.runtime.identityHashCode

private const val INITIAL_CAPACITY = 16

/**
 * A set of values references where the values are held weakly.
 *
 * This doesn't implement the entire Set<T> API and only implements those methods that are needed
 * for use in [Snapshot].
 *
 * [add], [find] and [findExactIndex] are copied from IdentityArraySet and refined to use weak
 * references. Any bugs found in these methods are likely to also be in IdentityArraySet and vis
 * versa.
 */
internal class SnapshotWeakSet<T : Any> {
    /**
     * The size of the set. The set has at most [size] entries but could have fewer if any of the
     * entries have been collected.
     */
    internal var size: Int = 0

    /**
     * Hashes are kept separately as the original object might not be available but its hash is
     * required to be available as the entries are stored in hash order and found via a binary
     * search.
     */
    internal var hashes = IntArray(INITIAL_CAPACITY)
    internal var values: Array<WeakReference<T>?> = arrayOfNulls(INITIAL_CAPACITY)

    /**
     * Add [value] to the set and return `true` if it was added or `false` if it already existed.
     */
    fun add(value: T): Boolean {
        val index: Int
        val size = size
        val hash = identityHashCode(value)
        if (size > 0) {
            index = find(value, hash)

            if (index >= 0) {
                return false
            }
        } else {
            index = -1
        }

        val insertIndex = -(index + 1)
        val capacity = values.size
        if (size == capacity) {
            val newCapacity = capacity * 2
            val newValues = arrayOfNulls<WeakReference<T>?>(newCapacity)
            val newHashes = IntArray(newCapacity)
            values.copyInto(
                destination = newValues,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            values.copyInto(
                destination = newValues,
                endIndex = insertIndex
            )
            hashes.copyInto(
                destination = newHashes,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            hashes.copyInto(
                destination = newHashes,
                endIndex = insertIndex
            )
            values = newValues
            hashes = newHashes
        } else {
            values.copyInto(
                destination = values,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            hashes.copyInto(
                destination = hashes,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
        }

        // A hole for the new items has been opened with the arrays, add the element there.
        values[insertIndex] = WeakReference(value)
        hashes[insertIndex] = hash
        this.size++
        return true
    }

    /**
     * Remove an entry from the set if [block] returns true.
     *
     * This also will discard any weak references that are no longer referring to their objects.
     *
     * This call is inline to avoid allocations while enumerating the set.
     */
     inline fun removeIf(block: (T) -> Boolean) {
        val size = size
        var currentUsed = 0
        // Call `block` on all entries that still have a valid reference
        // removing entries that are not valid or return `true` from block.
        for (i in 0 until size) {
            val entry = values[i]
            val value = entry?.get()
            if (value != null && !block(value)) {
                // We are keeping this entry
                if (currentUsed != i) {
                    values[currentUsed] = entry
                    hashes[currentUsed] = hashes[i]
                }
                currentUsed++
            }
        }

        // Clear the remaining entries
        for (i in currentUsed until size) {
            values[i] = null
            hashes[i] = 0
        }

        // Adjust the size to match number of slots left.
        if (currentUsed != size) {
            this.size = currentUsed
        }
    }

    /**
     * Returns the index of [value] in the set or the negative index - 1 of the location where
     * it would have been if it had been in the set.
     */
    private fun find(value: T, hash: Int): Int {
        var low = 0
        var high = size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midHash = hashes[mid]
            when {
                midHash < hash -> low = mid + 1
                midHash > hash -> high = mid - 1
                else -> {
                    val midVal = values[mid]?.get()
                    if (value === midVal) return mid
                    return findExactIndex(mid, value, hash)
                }
            }
        }
        return -(low + 1)
    }

    /**
     * When multiple items share the same [identityHashCode], then we must find the specific
     * index of the target item. This method assumes that [midIndex] has already been checked
     * for an exact match for [value], but will look at nearby values to find the exact item index.
     * If no match is found, the negative index - 1 of the position in which it would be will
     * be returned, which is always after the last item with the same [identityHashCode].
     */
    private fun findExactIndex(midIndex: Int, value: T, valueHash: Int): Int {
        // hunt down first
        for (i in midIndex - 1 downTo 0) {
            if (hashes[i] != valueHash) {
                break // we've gone too far
            }
            val v = values[i]?.get()
            if (v === value) {
                return i
            }
        }

        for (i in midIndex + 1 until size) {
            if (hashes[i] != valueHash) {
                // We've gone too far. We should insert here.
                return -(i + 1)
            }
            val v = values[i]?.get()
            if (v === value) {
                return i
            }
        }

        // We should insert at the end
        return -(size + 1)
    }

    @TestOnly
    internal fun isValid(): Boolean {
        val size = size
        val values = values
        val hashes = hashes
        val capacity = values.size

        // Validate that the size is less than or equal to the capacity
        if (size > capacity) return false

        // Validate that the hashes are in order and they match identity hash of the value or
        // the value has been collected.
        var previous = Int.MIN_VALUE
        for (i in 0 until size) {
            val hash = hashes[i]
            if (hash < previous) return false
            val entry = values[i] ?: return false
            val value = entry.get()
            if (value != null && hash != identityHashCode(value)) return false
            previous = hash
        }

        // Validate that all hashes and entries size and above are empty
        for (i in size until capacity) {
            if (hashes[i] != 0) return false
            if (values[i] != null) return false
        }

        return true
    }
}