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

import kotlin.jvm.JvmOverloads

/**
 * ArraySet is a generic set data structure that is designed to be more memory efficient than a
 * traditional [HashSet].  The design is very similar to `ArrayMap`, with all of the caveats
 * described there.  This implementation is separate from `ArrayMap`, however, so the array
 * contains only one item for each entry in the set (instead of a pair for a mapping).
 *
 * Note that this implementation is not intended to be appropriate for data structures
 * that may contain large numbers of items.  It is generally slower than a traditional
 * [HashSet], since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array.  For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.
 *
 * Because this container is intended to better balance memory use, unlike most other
 * standard Java containers it will shrink its array as items are removed from it.  Currently
 * you have no control over this shrinking -- if you set a capacity and then remove an
 * item, it may reduce the capacity to better match the current size.  In the future an
 * explicit call to set the capacity should turn off this aggressive shrinking behavior.
 *
 * This structure is *NOT* thread-safe.
 *
 * @param capacity initial capacity of the set.  The default capacity of an array set is 0, and it
 * will grow once items are added to it.
 */
class ArraySet<E>
@JvmOverloads constructor(
    capacity: Int = 0
) : MutableCollection<E>, MutableSet<E> {

    /**
     * Create a new ArraySet with the elements from the given ArraySet.
     */
    constructor(arraySet: ArraySet<out E>?) : this(0) {
        if (arraySet != null) {
            addAll(arraySet)
        }
    }

    /**
     * Create a new ArraySet with the elements from the given [Collection].
     */
    constructor(c: Collection<E>?) : this(0) {
        if (c != null) {
            addAll(c)
        }
    }

    /**
     * Create a new ArraySet with the elements from the given [Array].
     */
    // Suppress this lint error.  In general, we want APIs to use collections instead of
    // arrays, but that means we need to have ways to build collections from arrays
    constructor(@Suppress("ArrayReturn") array: Array<E>?) : this(0) {
        if (array != null) {
            addAll(array)
        }
    }

    private var hashes: IntArray
    private var values: Array<E?>
    init {
        if (capacity == 0) {
            hashes = EMPTY_INTS
            @Suppress("UNCHECKED_CAST") // Empty array.
            values = EMPTY_OBJECTS as Array<E?>
        } else {
            hashes = IntArray(capacity)
            @Suppress("UNCHECKED_CAST") // We only get/set "E"s and nulls.
            values = arrayOfNulls<Any>(capacity) as Array<E?>
        }
    }

    private var _size: Int = 0

    /**
     * The number of items in this set.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open override val size: Int get() = _size

    /**
     * Return `true` if the set contains no items.
     */
    override fun isEmpty(): Boolean = _size == 0

    /**
     * Ensure the set can hold at least [minimumCapacity] items.
     *
     * @throws ConcurrentModificationException if the set has been concurrently modified.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun ensureCapacity(minimumCapacity: Int) {
        val oldSize = _size
        if (hashes.size < minimumCapacity) {
            hashes = hashes.copyOf(minimumCapacity)
            values = values.copyOf(minimumCapacity)
        }
        if (_size != oldSize) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Returns the index of [element] in the set.
     *
     * @param element The value to search for.
     * @return the index of [element] if it exists, else a negative integer.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun indexOf(element: E): Int {
        return if (element == null) {
            indexOfNull()
        } else {
            indexOf(element, element.hashCode())
        }
    }

    private fun binarySearch(hash: Int): Int {
        try {
            return hashes.binarySearch(size, hash)
        } catch (_: IndexOutOfBoundsException) {
            throw ConcurrentModificationException()
        }
    }

    private fun indexOf(element: E, hash: Int) = indexOf(hash) { it == element }
    private fun indexOfNull() = indexOf(0) { it == null }

    private inline fun indexOf(hash: Int, predicate: (E?) -> Boolean): Int {
        val size = _size
        if (size == 0) {
            // Important fast case: if nothing is in here, nothing to look for.
            return 0.inv()
        }

        val index = binarySearch(hash)
        if (index < 0) {
            return index // Not found.
        }

        if (predicate(values[index])) {
            return index // Found!
        }

        // Search matching hashes after the index.
        var end = index + 1
        while (end < size && hashes[end] == hash) {
            if (predicate(values[end])) {
                return end
            }
            end++
        }

        // Search matching hashes before the index.
        for (i in index - 1 downTo 0) {
            if (hashes[i] != hash) {
                break
            }
            if (predicate(values[i])) {
                return i
            }
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    /**
     * Return the value at the given index in the array.
     *
     * @param index The desired index, must be between 0 and [size]-1.
     * @return the value stored at [index].
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun valueAt(index: Int): E? = values[index]

    /**
     * Adds the specified [element] to this set. The set is not modified if it
     * already contains [element].
     *
     * @param element the object to add.
     * @return `true` if this set is modified, `false` otherwise.
     * @throws ConcurrentModificationException if the set has been concurrently modified.
     */
    override fun add(element: E): Boolean {
        val oldSize = _size

        val hash: Int
        var index: Int
        if (element == null) {
            hash = 0
            index = indexOfNull()
        } else {
            hash = element.hashCode()
            index = indexOf(element, hash)
        }

        if (index >= 0) {
            return false
        }
        index = index.inv()

        // Grow the array if needed.
        if (oldSize == hashes.size) {
            val newSize = when {
                oldSize >= BASE_SIZE_2X -> oldSize + (oldSize shr 1)
                oldSize >= BASE_SIZE -> BASE_SIZE_2X
                else -> BASE_SIZE
            }
            hashes = hashes.copyOf(newSize)
            values = values.copyOf(newSize)
        }

        // Shift the array if needed.
        if (index < oldSize) {
            hashes.copyInto(
                hashes,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = oldSize
            )
            values.copyInto(
                values,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = oldSize
            )
        }

        if (oldSize != _size || index >= hashes.size) {
            throw ConcurrentModificationException()
        }

        hashes[index] = hash
        values[index] = element

        _size++
        return true
    }

    /**
     * [add] all values in [elements]
     * @param elements The collection whose contents are to be added.
     */
    override fun addAll(elements: Collection<E>): Boolean {
        ensureCapacity(_size + elements.size)
        var added = false
        for (element in elements) {
            added = added or add(element)
        }
        return added
    }

    /**
     * [add] all values in [elements]
     * @param elements The array whose contents are to be added.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun addAll(elements: ArraySet<E>) {
        val arraySize = elements._size
        if (_size == 0) {
            if (arraySize > 0) {
                hashes = elements.hashes.copyOf()
                values = elements.values.copyOf()
                _size = arraySize
            }
        } else {
            for (i in 0 until arraySize) {
                add(elements.valueAt(i)!!)
            }
        }
    }

    /**
     * Removes the specified [element] from this set.
     *
     * @param element the object to remove.
     * @return `true` if this set is modified, `false` otherwise.
     */
    override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    /**
     * Remove the element at the given [index].
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value that was stored at this [index].
     *
     * @throws ConcurrentModificationException if the set has been concurrently modified.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeAt(index: Int): E? {
        val oldSize = _size
        val value = values[index]

        // TODO assert oldSize > 0, safe because we would have IOOBE'd above
        if (oldSize == 1) {
            clear()
        } else {
            val newSize = oldSize - 1

            val oldHashes = hashes
            val oldValues = values
            val oldStorageSize = oldHashes.size
            if (oldStorageSize > BASE_SIZE_2X && oldSize < oldStorageSize / 3) {

                // Shrinking enough to reduce size of arrays.  We don't allow it to
                // shrink smaller than BASE_SIZE_2X to avoid flapping between that and BASE_SIZE.
                val newStorageSize = when {
                    oldSize > BASE_SIZE_2X -> oldSize + (oldSize shr 1)
                    else -> BASE_SIZE_2X
                }
                val newHashes = IntArray(newStorageSize)
                @Suppress("UNCHECKED_CAST") // We only get/set "E"s and nulls.
                val newValues = arrayOfNulls<Any?>(newStorageSize) as Array<E?>

                if (index > 0) {
                    oldHashes.copyInto(newHashes, endIndex = index)
                    oldValues.copyInto(newValues, endIndex = index)
                }
                if (index < newSize) {
                    oldHashes.copyInto(
                        newHashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = oldSize
                    )
                    oldValues.copyInto(
                        newValues,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = oldSize
                    )
                }

                hashes = newHashes
                values = newValues
            } else {
                if (index < newSize) {
                    oldHashes.copyInto(
                        oldHashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = oldSize
                    )
                    oldValues.copyInto(
                        oldValues,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = oldSize
                    )
                }
                oldValues[newSize] = null
            }

            if (oldSize != _size) {
                throw ConcurrentModificationException()
            }
            _size = newSize
        }
        return value
    }

    /**
     * [remove] all values in [elements]
     * @return `true` if any values were removed from the array set, else `false`.
     */
    override fun removeAll(elements: Collection<E>): Boolean {
        var removed = false
        for (element in elements) {
            removed = removed or remove(element)
        }
        return removed
    }

    /**
     * [remove] all values in [elements]
     * @return `true` if any values were removed from the array set, else `false`.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeAll(elements: ArraySet<out E>): Boolean {
        // TODO: If array is sufficiently large, a marking approach might be beneficial. In a first
        //  pass, use the property that the sets are sorted by hash to make this linear passes
        //  (except for hash collisions, which means worst case still n*m), then do one collection
        //  pass into a new array. This avoids binary searches and excessive memcpy.

        val elementsSize = elements._size
        val originalSize = _size
        for (i in 0 until elementsSize) {
            remove(elements.valueAt(i)!!) // TODO unsafeCast?
        }
        return originalSize != _size
    }

    /**
     * Make the set empty.  All storage is released.
     */
    override fun clear() {
        hashes = EMPTY_INTS
        @Suppress("UNCHECKED_CAST") // Empty array.
        values = EMPTY_OBJECTS as Array<E?>
        _size = 0
    }

    /**
     * Remove all values in the array set that do *not* exist in the given [elements].
     * @param elements The collection whose contents are to be used to determine which
     * values to keep.
     * @return `true` if any values were removed from the array set, else `false`.
     */
    override fun retainAll(elements: Collection<E>): Boolean {
        var removed = false
        for (i in 0 until _size) {
            if (values[i] !in elements) {
                removeAt(i)
                removed = true
            }
        }
        return removed
    }

    /**
     * Check whether a value exists in the set.
     *
     * @param element The value to search for.
     * @return whether [element] exists in the set.
     */
    override operator fun contains(element: E): Boolean = indexOf(element) >= 0

    /**
     * Determine if the array set contains all of the values in the given collection.
     *
     * @param elements The collection whose contents are to be checked against.
     * @return `true` if this array set contains a value for every entry
     * in [elements], else `false`.
     */
    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { it in this }
    }

    /**
     * Return an [MutableIterator] over all values in the set.
     *
     * *Note:* this is a less efficient way to access the array contents compared to
     * looping from 0 until [size] and calling [valueAt].
     */
    override fun iterator(): MutableIterator<E> {
        return Iterator(_size)
    }

    /**
     * This implementation returns false if the object is not a set, or
     * if the sets have different sizes.  Otherwise, for each value in this
     * set, it checks to make sure the value also exists in the other set.
     * If any value doesn't exist, the method returns false; otherwise, it
     * returns true.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is Set<*>) {
            if (size != other.size) {
                return false
            }
            try {
                for (i in 0 until size) {
                    val mine = valueAt(i)
                    if (!other.contains(mine)) {
                        return false
                    }
                }
            } catch (ignored: NullPointerException) {
                return false
            } catch (ignored: ClassCastException) {
                return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        var result = 0
        var i = 0
        val s: Int = size
        while (i < s) {
            result += hashes[i]
            i++
        }
        return result
    }

    /**
     * This implementation composes a string by iterating over its values. If
     * this set contains itself as a value, the string "(this Set)"
     * will appear in its place.
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }
        val buffer = StringBuilder(size * 14)
        buffer.append('{')
        for (i in 0 until size) {
            if (i > 0) {
                buffer.append(", ")
            }
            val value: Any? = valueAt(i)
            if (value !== this) {
                buffer.append(value)
            } else {
                buffer.append("(this Set)")
            }
        }
        buffer.append('}')
        return buffer.toString()
    }

    private inner class Iterator(size: Int) : IndexBasedMutableIterator<E>(size) {
        @Suppress("UNCHECKED_CAST") // Assume base iterator only accessing valid indices.
        override fun get(index: Int) = values[index] as E

        override fun remove(index: Int) {
            removeAt(index)
        }
    }

    private companion object {
        /**
         * The minimum amount by which the capacity of a [ArraySet] will increase.
         * This is tuned to be relatively space-efficient.
         */
        private const val BASE_SIZE = 4
        private const val BASE_SIZE_2X = BASE_SIZE * 2
    }
}
