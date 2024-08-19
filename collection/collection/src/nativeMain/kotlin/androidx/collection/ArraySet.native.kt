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

package androidx.collection

import androidx.collection.internal.EMPTY_INTS
import androidx.collection.internal.EMPTY_OBJECTS

/**
 * ArraySet is a generic set data structure that is designed to be more memory efficient than a
 * traditional [HashSet]. The design is very similar to [ArrayMap], with all of the caveats
 * described there. This implementation is separate from ArrayMap, however, so the Object array
 * contains only one item for each entry in the set (instead of a pair for a mapping).
 *
 * Note that this implementation is not intended to be appropriate for data structures that may
 * contain large numbers of items. It is generally slower than a traditional HashSet, since lookups
 * require a binary search and adds and removes require inserting and deleting entries in the array.
 * For containers holding up to hundreds of items, the performance difference is not significant,
 * less than 50%.
 *
 * Because this container is intended to better balance memory use, unlike most other standard Java
 * containers it will shrink its array as items are removed from it. Currently you have no control
 * over this shrinking -- if you set a capacity and then remove an item, it may reduce the capacity
 * to better match the current size. In the future an explicit call to set the capacity should turn
 * off this aggressive shrinking behavior.
 *
 * This structure is **NOT** thread-safe.
 *
 * @constructor Creates a new empty ArraySet. The default capacity of an array map is 0, and will
 *   grow once items are added to it.
 */
// JvmOverloads is required on constructor to match expect declaration
public actual class ArraySet<E> actual constructor(capacity: Int) :
    MutableCollection<E>, MutableSet<E> {

    internal actual var hashes: IntArray = EMPTY_INTS
    internal actual var array: Array<Any?> = EMPTY_OBJECTS

    internal actual var _size = 0
    actual override val size: Int
        get() = _size

    /** Create a new ArraySet with the mappings from the given ArraySet. */
    public actual constructor(set: ArraySet<out E>?) : this(capacity = 0) {
        if (set != null) {
            addAll(set)
        }
    }

    /** Create a new ArraySet with the mappings from the given [Collection]. */
    public actual constructor(set: Collection<E>?) : this(capacity = 0) {
        if (set != null) {
            addAll(set)
        }
    }

    /** Create a new ArraySet with items from the given array. */
    public actual constructor(array: Array<out E>?) : this(capacity = 0) {
        if (array != null) {
            for (value in array) {
                add(value)
            }
        }
    }

    init {
        if (capacity > 0) {
            allocArrays(capacity)
        }
    }

    /**
     * Make the array map empty. All storage is released.
     *
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    actual override fun clear() {
        clearInternal()
    }

    /**
     * Ensure the array map can hold at least [minimumCapacity] items.
     *
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public actual fun ensureCapacity(minimumCapacity: Int) {
        ensureCapacityInternal(minimumCapacity)
    }

    /**
     * Check whether a value exists in the set.
     *
     * @param element The value to search for.
     * @return Returns true if the value exists, else false.
     */
    actual override operator fun contains(element: E): Boolean {
        return containsInternal(element)
    }

    /**
     * Returns the index of a value in the set.
     *
     * @param key The value to search for.
     * @return Returns the index of the value if it exists, else a negative integer.
     */
    public actual fun indexOf(key: Any?): Int {
        return indexOfInternal(key)
    }

    /**
     * Return the value at the given index in the array.
     *
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value stored at the given index.
     */
    public actual fun valueAt(index: Int): E {
        return valueAtInternal(index)
    }

    /** Return `true` if the array map contains no items. */
    actual override fun isEmpty(): Boolean {
        return isEmptyInternal()
    }

    /**
     * Adds the specified object to this set. The set is not modified if it already contains the
     * object.
     *
     * @param element the object to add.
     * @return `true` if this set is modified, `false` otherwise.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    actual override fun add(element: E): Boolean {
        return addInternal(element)
    }

    /**
     * Perform a [add] of all values in [array]
     *
     * @param array The array whose contents are to be retrieved.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public actual fun addAll(array: ArraySet<out E>) {
        addAllInternal(array)
    }

    /**
     * Removes the specified object from this set.
     *
     * @param element the object to remove.
     * @return `true` if this set was modified, `false` otherwise.
     */
    actual override fun remove(element: E): Boolean {
        return removeInternal(element)
    }

    /**
     * Remove the key/value mapping at the given index.
     *
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value that was stored at this index.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public actual fun removeAt(index: Int): E {
        return removeAtInternal(index)
    }

    /**
     * Perform a [remove] of all values in [array]
     *
     * @param array The array whose contents are to be removed.
     */
    public actual fun removeAll(array: ArraySet<out E>): Boolean {
        return removeAllInternal(array)
    }

    /**
     * This implementation returns false if the object is not a set, or if the sets have different
     * sizes. Otherwise, for each value in this set, it checks to make sure the value also exists in
     * the other set. If any value doesn't exist, the method returns false; otherwise, it returns
     * true.
     *
     * @see Any.equals
     */
    actual override fun equals(other: Any?): Boolean {
        return equalsInternal(other)
    }

    /** @see Any.hashCode */
    actual override fun hashCode(): Int {
        return hashCodeInternal()
    }

    /**
     * This implementation composes a string by iterating over its values. If this set contains
     * itself as a value, the string "(this Set)" will appear in its place.
     */
    actual override fun toString(): String {
        return toStringInternal()
    }

    /**
     * Return a [MutableIterator] over all values in the set.
     *
     * **Note:** this is a less efficient way to access the array contents compared to looping from
     * 0 until [size] and calling [valueAt].
     */
    actual override fun iterator(): MutableIterator<E> = ElementIterator()

    private inner class ElementIterator : IndexBasedArrayIterator<E>(_size) {
        override fun elementAt(index: Int): E = valueAt(index)

        override fun removeAt(index: Int) {
            this@ArraySet.removeAt(index)
        }
    }

    /**
     * Determine if the array set contains all of the values in the given collection.
     *
     * @param elements The collection whose contents are to be checked against.
     * @return Returns true if this array set contains a value for every entry in [elements] else
     *   returns false.
     */
    actual override fun containsAll(elements: Collection<E>): Boolean {
        return containsAllInternal(elements)
    }

    /**
     * Perform an [add] of all values in [elements]
     *
     * @param elements The collection whose contents are to be retrieved.
     */
    actual override fun addAll(elements: Collection<E>): Boolean {
        return addAllInternal(elements)
    }

    /**
     * Remove all values in the array set that exist in the given collection.
     *
     * @param elements The collection whose contents are to be used to remove values.
     * @return Returns true if any values were removed from the array set, else false.
     */
    actual override fun removeAll(elements: Collection<E>): Boolean {
        return removeAll(elements)
    }

    /**
     * Remove all values in the array set that do **not** exist in the given collection.
     *
     * @param elements The collection whose contents are to be used to determine which values to
     *   keep.
     * @return Returns true if any values were removed from the array set, else false.
     */
    actual override fun retainAll(elements: Collection<E>): Boolean {
        return retainAllInternal(elements)
    }
}
