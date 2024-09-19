/*
 * Copyright 2023 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.input.pointer.PointerId

/**
 * This collection is specifically for dealing with [PointerId] values. We know that they contain
 * [Long] values, so we store them in an underlying LongArray. We want to be able to resize the
 * array if there are many ids to be stored, so we recreate the internal LongArray as necessary
 * (since LongArray is not itself resizable).
 */
internal class PointerIdArray {
    /**
     * The size of this [PointerIdArray], which is equal to the number of ids stored in the array.
     */
    // Note that this is different than the size of the backing LongArray, which may allocate more
    // entries to avoid resizing for every additional id that is added.
    var size = 0
        private set

    /** Returns index of last item in array */
    inline val lastIndex: Int
        get() = size - 1

    /**
     * The ids are stored as Long values in a LongArray. LongArray is not resizable, and we may need
     * to expand this array if there are many pointer ids in use at any given time, so we keep the
     * LongArray private and resize the PointerIdArray by allocating a larger LongArray (and copying
     * existing values to it) as necessary.
     *
     * By default, we allocate the underlying array with 2 elements, since it is uncommon (though
     * possible) to have more than two ids at a time.
     */
    private var internalArray = LongArray(2)

    /**
     * Returns the PointerId at the given index. This getter allows use of [] syntax to retrieve
     * values.
     */
    operator fun get(index: Int): PointerId {
        return PointerId(internalArray[index])
    }

    /**
     * Removes the given [PointerId] from this array, if it exists.
     *
     * @return true if [pointerId] was in the array, false otherwise
     */
    inline fun remove(pointerId: PointerId): Boolean {
        return remove(pointerId.value)
    }

    /**
     * Removes a [PointerId] with the given value from this array, if it exists.
     *
     * @return true if a [PointerId] with the value [pointerIdValue] was in the array, false
     *   otherwise
     */
    fun remove(pointerIdValue: Long): Boolean {
        for (i in 0 until size) {
            if (pointerIdValue == internalArray[i]) {
                for (j in i until size - 1) {
                    internalArray[j] = internalArray[j + 1]
                }
                size--
                return true
            }
        }
        return false
    }

    /**
     * Removes the [PointerId] at the given index value, if the index is less than the size of the
     * array.
     *
     * @return true if a [PointerId] at that index was removed, false otherwise
     */
    fun removeAt(index: Int): Boolean {
        if (index < size) {
            for (i in index until size - 1) {
                internalArray[i] = internalArray[i + 1]
            }
            size--
            return true
        }
        return false
    }

    /** Returns the current size of the array */
    fun isEmpty() = size == 0

    /**
     * Adds the given pointerId value to this array unless it is already there.
     *
     * @return true if id was added, false otherwise
     */
    fun add(value: Long): Boolean {
        if (!contains(value)) {
            set(size, value)
            return true
        }
        return false
    }

    /**
     * Adds the given pointerId value to this array unless it is already there.
     *
     * @return true if id was added, false otherwise
     */
    inline fun add(pointerId: PointerId): Boolean {
        return add(pointerId.value)
    }

    /**
     * Sets the value at the given index to a [PointerId] with the value [value]. The index must be
     * less than or equal to the current size of the array. If it is equal to the size of the array,
     * the storage in the array will be expanded to ensure that the item can be added to the end of
     * it.
     */
    operator fun set(index: Int, value: Long) {
        var internalArray = internalArray
        if (index >= internalArray.size) {
            // Increase the size of the backing array
            internalArray = resizeStorage(index + 1)
        }
        internalArray[index] = value
        if (index >= size) size = index + 1
    }

    private fun resizeStorage(minSize: Int): LongArray {
        return internalArray.copyOf(maxOf(minSize, internalArray.size * 2)).apply {
            internalArray = this
        }
    }

    /**
     * Sets the value at the given index to [pointerId]. The index must be less than or equal to the
     * current size of the array. If it is equal to the size of the array, the storage in the array
     * will be expanded to ensure that the item can be added to the end of it.
     */
    inline operator fun set(index: Int, pointerId: PointerId) {
        set(index, pointerId.value)
    }

    /** Clears the array. The new [size] of the array will be 0. */
    fun clear() {
        // No need to clear, just reset the size. Elements beyond the current size are ignored.
        size = 0
    }

    /** Returns true if [pointerId] is in the array, false otherwise */
    inline fun contains(pointerId: PointerId): Boolean {
        return contains(pointerId.value)
    }

    /** Returns true if a [PointerId] with the given value is in the array, false otherwise */
    fun contains(pointerIdValue: Long): Boolean {
        for (i in 0 until size) {
            if (internalArray[i] == pointerIdValue) return true
        }
        return false
    }
}
