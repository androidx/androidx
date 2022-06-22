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

package androidx.collection

import androidx.collection.internal.EMPTY_INTS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.binarySearch
import kotlin.jvm.JvmOverloads

/** Returns an empty new [ArraySet]. */
@Suppress("NOTHING_TO_INLINE") // Alias to public API.
public inline fun <T> arraySetOf(): ArraySet<T> = ArraySet()

/** Returns a new [ArraySet] with the specified contents. */
public fun <T> arraySetOf(vararg values: T): ArraySet<T> {
    val set = ArraySet<T>(values.size)
    @Suppress("LoopToCallChain") // Causes needless copy to a list.
    for (value in values) {
        set.add(value)
    }
    return set
}

/**
 * ArraySet is a generic set data structure that is designed to be more memory efficient than a
 * traditional [HashSet].  The design is very similar to
 * [ArrayMap], with all of the caveats described there.  This implementation is
 * separate from ArrayMap, however, so the Object array contains only one item for each
 * entry in the set (instead of a pair for a mapping).
 *
 * Note that this implementation is not intended to be appropriate for data structures
 * that may contain large numbers of items.  It is generally slower than a traditional
 * HashSet, since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array.  For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.
 *
 * Because this container is intended to better balance memory use, unlike most other
 * standard Java containers it will shrink its array as items are removed from it.  Currently
 * you have no control over this shrinking -- if you set a capacity and then remove an
 * item, it may reduce the capacity to better match the current size.  In the future an
 * explicit call to set the capacity should turn off this aggressive shrinking behavior.
 *
 * This structure is **NOT** thread-safe.
 *
 * @constructor Creates a new empty ArraySet. The default capacity of an array map is 0, and
 * will grow once items are added to it.
 */
public class ArraySet<E> @JvmOverloads constructor(capacity: Int = 0) :
    AbstractMutableCollection<E>(), MutableSet<E> {

    private var hashes: IntArray = EMPTY_INTS
    private var array: Array<Any?> = EMPTY_OBJECTS

    private var _size = 0

    override val size: Int
        get() = _size

    /**
     * Create a new ArraySet with the mappings from the given ArraySet.
     */
    public constructor(set: ArraySet<out E>?) : this(capacity = 0) {
        if (set != null) {
            addAll(set)
        }
    }

    /**
     * Create a new ArraySet with the mappings from the given [Collection].
     */
    public constructor(set: Collection<E>?) : this(capacity = 0) {
        if (set != null) {
            addAll(set)
        }
    }

    /**
     * Create a new ArraySet with items from the given array.
     */
    public constructor(array: Array<out E>?) : this(capacity = 0) {
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

    private fun binarySearchInternal(hash: Int): Int =
        try {
            binarySearch(hashes, _size, hash)
        } catch (e: IndexOutOfBoundsException) {
            throw ConcurrentModificationException()
        }

    private fun indexOf(key: Any?, hash: Int): Int {
        val n = _size

        // Important fast case: if nothing is in here, nothing to look for.
        if (n == 0) {
            return -1
        }
        val index = binarySearchInternal(hash)

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index
        }

        // If the key at the returned index matches, that's what we want.
        if (key == array[index]) {
            return index
        }

        // Search for a matching key after the index.
        var end = index + 1
        while (end < n && hashes[end] == hash) {
            if (key == array[end]) {
                return end
            }
            end++
        }

        // Search for a matching key before the index.
        var i = index - 1
        while (i >= 0 && hashes[i] == hash) {
            if (key == array[i]) {
                return i
            }
            i--
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    private fun indexOfNull(): Int = indexOf(key = null, hash = 0)

    private fun allocArrays(size: Int) {
        hashes = IntArray(size)
        array = arrayOfNulls(size)
    }

    private inline fun printlnIfDebug(message: () -> String) {
        if (DEBUG) {
            println(message())
        }
    }

    /**
     * Make the array map empty.  All storage is released.
     *
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    override fun clear() {
        if (_size != 0) {
            hashes = EMPTY_INTS
            array = EMPTY_OBJECTS
            _size = 0
        }
        @Suppress("KotlinConstantConditions")
        if (_size != 0) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Ensure the array map can hold at least [minimumCapacity]
     * items.
     *
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public fun ensureCapacity(minimumCapacity: Int) {
        val oSize: Int = _size
        if (hashes.size < minimumCapacity) {
            val ohashes = hashes
            val oarray = array
            allocArrays(minimumCapacity)
            if (_size > 0) {
                ohashes.copyInto(destination = hashes, endIndex = _size)
                oarray.copyInto(destination = array, endIndex = _size)
            }
        }
        if (_size != oSize) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Check whether a value exists in the set.
     *
     * @param element The value to search for.
     * @return Returns true if the value exists, else false.
     */
    override operator fun contains(element: E): Boolean =
        indexOf(element) >= 0

    /**
     * Returns the index of a value in the set.
     *
     * @param key The value to search for.
     * @return Returns the index of the value if it exists, else a negative integer.
     */
    public fun indexOf(key: Any?): Int =
        if (key == null) indexOfNull() else indexOf(key = key, hash = key.hashCode())

    /**
     * Return the value at the given index in the array.
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value stored at the given index.
     */
    @Suppress("UNCHECKED_CAST")
    public fun valueAt(index: Int): E =
        array[index] as E

    /**
     * Return true if the array map contains no items.
     */
    override fun isEmpty(): Boolean =
        _size <= 0

    /**
     * Adds the specified object to this set. The set is not modified if it
     * already contains the object.
     *
     * @param element the object to add.
     * @return `true` if this set is modified, `false` otherwise.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    override fun add(element: E): Boolean {
        val oSize = _size
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
        if (oSize >= hashes.size) {
            val n =
                when {
                    oSize >= BASE_SIZE * 2 -> oSize + (oSize shr 1)
                    oSize >= BASE_SIZE -> BASE_SIZE * 2
                    else -> BASE_SIZE
                }

            printlnIfDebug { "$TAG add: grow from ${hashes.size} to $n" }

            val ohashes = hashes
            val oarray = array
            allocArrays(n)

            if (oSize != _size) {
                throw ConcurrentModificationException()
            }

            if (hashes.isNotEmpty()) {
                printlnIfDebug { "$TAG add: copy 0-$oSize to 0" }
                ohashes.copyInto(destination = hashes, endIndex = ohashes.size)
                oarray.copyInto(destination = array, endIndex = oarray.size)
            }
        }

        if (index < oSize) {
            printlnIfDebug { "$TAG add: move $index-${oSize - index} to ${index + 1}" }

            hashes.copyInto(
                destination = hashes,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = oSize
            )
            array.copyInto(
                destination = array,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = oSize
            )
        }

        if (oSize != _size || index >= hashes.size) {
            throw ConcurrentModificationException()
        }

        hashes[index] = hash
        array[index] = element
        _size++
        return true
    }

    /**
     * Perform a [add] of all values in [array]
     * @param array The array whose contents are to be retrieved.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public fun addAll(array: ArraySet<out E>) {
        val n = array._size
        ensureCapacity(_size + n)
        if (_size == 0) {
            if (n > 0) {
                array.hashes.copyInto(destination = hashes, endIndex = n)
                array.array.copyInto(destination = this.array, endIndex = n)
                if (0 != _size) {
                    throw ConcurrentModificationException()
                }
                _size = n
            }
        } else {
            for (i in 0 until n) {
                add(array.valueAt(i))
            }
        }
    }

    /**
     * Removes the specified object from this set.
     *
     * @param element the object to remove.
     * @return `true` if this set was modified, `false` otherwise.
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
     * Remove the key/value mapping at the given index.
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value that was stored at this index.
     * @throws ConcurrentModificationException if concurrent modifications detected.
     */
    public fun removeAt(index: Int): E {
        val oSize = _size
        val old = array[index]
        if (oSize <= 1) {
            // Now empty.
            printlnIfDebug { "$TAG remove: shrink from ${hashes.size} to 0" }
            clear()
        } else {
            val nSize = oSize - 1
            if (hashes.size > (BASE_SIZE * 2) && (_size < hashes.size / 3)) {
                // Shrunk enough to reduce size of arrays.  We don't allow it to
                // shrink smaller than (BASE_SIZE*2) to avoid flapping between
                // that and BASE_SIZE.
                val n = if (_size > BASE_SIZE * 2) _size + (_size shr 1) else BASE_SIZE * 2
                printlnIfDebug { "$TAG remove: shrink from ${hashes.size} to $n" }
                val ohashes = hashes
                val oarray = array
                allocArrays(n)
                if (index > 0) {
                    printlnIfDebug { "$TAG remove: copy from 0-$index to 0" }
                    ohashes.copyInto(destination = hashes, endIndex = index)
                    oarray.copyInto(destination = array, endIndex = index)
                }
                if (index < nSize) {
                    printlnIfDebug { "$TAG remove: copy from ${index + 1}-$nSize to $index" }
                    ohashes.copyInto(
                        destination = hashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nSize + 1
                    )
                    oarray.copyInto(
                        destination = array,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nSize + 1
                    )
                }
            } else {
                if (index < nSize) {
                    printlnIfDebug { "$TAG remove: move ${index + 1}-$nSize to $index" }
                    hashes.copyInto(
                        destination = hashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nSize + 1
                    )
                    array.copyInto(
                        destination = array,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nSize + 1
                    )
                }
                array[nSize] = null
            }
            if (oSize != _size) {
                throw ConcurrentModificationException()
            }
            _size = nSize
        }
        @Suppress("UNCHECKED_CAST")
        return old as E
    }

    /**
     * Perform a [remove] of all values in [array]
     * @param array The array whose contents are to be removed.
     */
    public fun removeAll(array: ArraySet<out E>): Boolean {
        // TODO: If array is sufficiently large, a marking approach might be beneficial. In a first
        //       pass, use the property that the sets are sorted by hash to make this linear passes
        //       (except for hash collisions, which means worst case still n*m), then do one
        //       collection pass into a new array. This avoids binary searches and excessive memcpy.
        val n = array._size

        // Note: ArraySet does not make thread-safety guarantees. So instead of OR-ing together all
        //       the single results, compare size before and after.
        val originalSize = _size
        for (i in 0 until n) {
            remove(array.valueAt(i))
        }
        return originalSize != _size
    }

    /**
     * This implementation returns false if the object is not a set, or
     * if the sets have different sizes.  Otherwise, for each value in this
     * set, it checks to make sure the value also exists in the other set.
     * If any value doesn't exist, the method returns false; otherwise, it
     * returns true.
     *
     * @see Any.equals
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
                for (i in 0 until _size) {
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

    /**
     * @see Any.hashCode
     */
    override fun hashCode(): Int {
        val hashes = hashes
        val s = _size
        var result = 0
        for (i in 0 until s) {
            result += hashes[i]
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

        return buildString(capacity = _size * 14) {
            append('{')
            for (i in 0 until _size) {
                if (i > 0) {
                    append(", ")
                }
                val value = valueAt(i)
                if (value !== this@ArraySet) {
                    append(value)
                } else {
                    append("(this Set)")
                }
            }
            append('}')
        }
    }

    /**
     * Return a [MutableIterator] over all values in the set.
     *
     * **Note:** this is a less efficient way to access the array contents compared to
     * looping from 0 until [size] and calling [valueAt].
     */
    override fun iterator(): MutableIterator<E> =
        ElementIterator()

    private inner class ElementIterator : IndexBasedArrayIterator<E>(_size) {
        override fun elementAt(index: Int): E =
            valueAt(index)

        override fun removeAt(index: Int) {
            this@ArraySet.removeAt(index)
        }
    }

    /**
     * Determine if the array set contains all of the values in the given collection.
     * @param elements The collection whose contents are to be checked against.
     * @return Returns true if this array set contains a value for every entry
     * in [elements] else returns false.
     */
    override fun containsAll(elements: Collection<E>): Boolean {
        for (item in elements) {
            if (!contains(item)) {
                return false
            }
        }
        return true
    }

    /**
     * Perform an [add] of all values in [elements]
     * @param elements The collection whose contents are to be retrieved.
     */
    override fun addAll(elements: Collection<E>): Boolean {
        ensureCapacity(_size + elements.size)
        var added = false
        for (value in elements) {
            added = add(value) or added
        }
        return added
    }

    /**
     * Remove all values in the array set that exist in the given collection.
     * @param elements The collection whose contents are to be used to remove values.
     * @return Returns true if any values were removed from the array set, else false.
     */
    override fun removeAll(elements: Collection<E>): Boolean {
        var removed = false
        for (value in elements) {
            removed = removed or remove(value)
        }
        return removed
    }

    /**
     * Remove all values in the array set that do **not** exist in the given collection.
     * @param elements The collection whose contents are to be used to determine which
     * values to keep.
     * @return Returns true if any values were removed from the array set, else false.
     */
    override fun retainAll(elements: Collection<E>): Boolean {
        var removed = false
        for (i in _size - 1 downTo 0) {
            if (array[i] !in elements) {
                removeAt(i)
                removed = true
            }
        }
        return removed
    }

    private companion object {
        private const val DEBUG = false
        private const val TAG = "ArraySet"

        /**
         * The minimum amount by which the capacity of a ArraySet will increase.
         * This is tuned to be relatively space-efficient.
         */
        private const val BASE_SIZE = 4
    }
}
