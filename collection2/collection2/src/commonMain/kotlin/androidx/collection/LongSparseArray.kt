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

/** Avoid conflict (and R8 dup. classes failures) with collection-ktx. */
// TODO: Remove this after collection-ktx is merged
@file:JvmName("LongSparseArray_Ext")
@file:Suppress("NOTHING_TO_INLINE") // Avoiding additional invocation indirection.

package androidx.collection

import kotlin.jvm.JvmName

/**
 * [SparseArray] mapping [Long]s to values of any type.  Unlike a normal array,
 * there can be gaps in the indices.  It is intended to be more memory efficient
 * than using a HashMap to map [Long]s to Objects, both because it avoids
 * auto-boxing keys and its data structure doesn't rely on an extra entry object
 * for each mapping.
 *
 * Note that this container keeps its mappings in an array data structure,
 * using a binary search to find keys.  The implementation is not intended to be appropriate for
 * data structures
 * that may contain large numbers of items.  It is generally slower than a traditional
 * HashMap, since lookups require a binary search and adds and removes require inserting
 * and deleting entries in the array.  For containers holding up to hundreds of items,
 * the performance difference is not significant, less than 50%.
 *
 * To help with performance, the container includes an optimization when removing
 * keys: instead of compacting its array immediately, it leaves the removed entry marked
 * as deleted.  The entry can then be re-used for the same key, or compacted later in
 * a single garbage collection step of all removed entries.  This garbage collection will
 * need to be performed at any time the array needs to be grown or the map size or
 * entry values are retrieved.
 *
 * It is possible to iterate over the items in this container using
 * [keyAt] and [valueAt]. Iterating over the keys using
 * [keyAt] with ascending values of the index will return the
 * keys in ascending order, or the values corresponding to the keys in ascending
 * order in the case of [valueAt].
 *
 * @constructor Creates a new [LongSparseArray] containing no mappings.
 * @param E the type of values stored
 * @param initialCapacity the structure will not require any additional memory allocation to store
 *     the specified number of mappings.  If you supply an [initialCapacity] of 0, the sparse array
 *     will be initialized with a light-weight representation not requiring any additional array
 *     allocations.
 */
expect class LongSparseArray<E>(initialCapacity: Int = 10) {
    internal var keys: LongArray
    internal var values: Array<Any?>
    internal var garbage: Boolean

    @Suppress("PropertyName") // Normal backing field name but internal for common code.
    internal var _size: Int

    constructor(array: LongSparseArray<E>)

    /**
     * The number of key-value mappings that this [LongSparseArray] currently stores.
     */
    val size: Int

    /**
     * @return `true` if [size] is 0.
     */
    fun isEmpty(): Boolean

    /**
     * Gets the value mapped from the specified [key], or `null` if no such mapping has been made.
     */
    operator fun get(key: Long): E?

    /**
     * Gets the value mapped from the specified [key], or the specified [default] value if no such
     * mapping has been made.
     */
    fun get(key: Long, default: E): E

    /**
     * Adds a mapping from the specified [key] to the specified [value],
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    fun put(key: Long, value: E) // TODO operator

    /**
     * Copies all of the mappings from the [other] to this map. The effect of this call is
     * equivalent to that of calling [put] on this map once for each mapping
     * from key to value in [other].
     */
    fun putAll(other: LongSparseArray<out E>)

    /**
     * Add a new [value] to the array map only if the [key] does not already have a value or it is
     * mapped to `null`.
     * @param key The key under which to store the [value].
     * @param value The value to store for the given [key].
     * @return Returns the value that was stored for the given [key], or `null` if there
     * was no such key.
     */
    fun putIfAbsent(key: Long, value: E): E?

    /**
     * Puts a [key]/[value] pair into the array, optimizing for the case where
     * the [key] is greater than all existing keys in the array.
     */
    fun append(key: Long, value: E)

    /**
     * Given an [index] in the range `0...size-1`, returns
     * the key from the [index]th key-value mapping that this
     * [LongSparseArray] stores.
     *
     * The keys corresponding to indices in ascending order are guaranteed to
     * be in ascending order, e.g., `keyAt(0)` will return the
     * smallest key and `keyAt(size-1)` will return the largest
     * key.
     *
     * @see [size]
     */
    fun keyAt(index: Int): Long

    /**
     * Given an [index] in the range `0...size-1`, returns
     * the value from the [index]th key-value mapping that this
     * [LongSparseArray] stores.
     *
     * The values corresponding to indices in ascending order are guaranteed
     * to be associated with keys in ascending order, e.g.,
     * `valueAt(0)` will return the value associated with the
     * smallest key and `valueAt(size-1)` will return the value
     * associated with the largest key.
     *
     * @see [size]
     */
    fun valueAt(index: Int): E

    /**
     * Given an index in the range `0...size-1`, sets a new
     * value for the [index]th key-value mapping that this
     * [LongSparseArray] stores.
     */
    fun setValueAt(index: Int, value: E)

    /**
     * Returns the index for which [keyAt] would return the
     * specified [key], or a negative number if the specified
     * [key] is not mapped.
     */
    fun indexOfKey(key: Long): Int

    /**
     * Returns an index for which [valueAt] would return the
     * specified [value], or a negative number if no keys map to the
     * specified [value].
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same [value] and this will
     * find only one of them.
     */
    fun indexOfValue(value: E): Int

    /** @return `true` if the specified [key] is mapped. */
    fun containsKey(key: Long): Boolean

    /** @return `true` if the specified [value] is mapped from any key. */
    fun containsValue(value: E): Boolean

    /**
     * Removes all key-value mappings from this [LongSparseArray].
     */
    fun clear()

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    fun remove(key: Long)

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns true if the mapping was removed.
     */
    fun remove(key: Long, value: Any?): Boolean

    /**
     * Removes the mapping at the specified index.
     */
    fun removeAt(index: Int)

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or null.
     */
    fun replace(key: Long, value: E): E?

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns true if the value was replaced.
     */
    fun replace(key: Long, oldValue: E?, newValue: E): Boolean

    /**
     * This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a value, the string "(this Map)"
     * will appear in its place.
     */
    override fun toString(): String
}

internal inline fun <E> LongSparseArray<E>.commonSize(): Int {
    if (garbage) {
        gc()
    }
    return _size
}

internal inline fun <E> LongSparseArray<E>.commonIsEmpty(): Boolean {
    return size == 0
}

internal inline fun <T, E : T> LongSparseArray<E>.commonGet(key: Long, default: T): T {
    val i = keys.binarySearch(_size, key)
    if (i >= 0) {
        val value = values[i]
        if (value !== DELETED) {
            @Suppress("UNCHECKED_CAST") // Guaranteed by positive index and DELETED check.
            return value as E
        }
    }
    return default
}

internal inline fun <E> LongSparseArray<E>.commonPut(key: Long, value: E) {
    var index = keys.binarySearch(_size, key)
    if (index >= 0) {
        values[index] = value
    } else {
        index = index.inv()
        if (index < _size && values[index] === DELETED) {
            keys[index] = key
            values[index] = value
            return
        }
        if (garbage && _size >= keys.size) {
            gc()
            // Search again because indices may have changed.
            index = keys.binarySearch(_size, key).inv()
        }
        if (_size >= keys.size) {
            val newSize = idealIntArraySize(_size + 1)
            keys = keys.copyOf(newSize)
            values = values.copyOf(newSize)
        }
        if (_size - index != 0) {
            keys.copyInto(keys, destinationOffset = index + 1, startIndex = index, endIndex = _size)
            values.copyInto(
                values,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = _size
            )
        }
        keys[index] = key
        values[index] = value
        _size++
    }
}

internal inline fun <E> LongSparseArray<E>.commonPutAll(other: LongSparseArray<out E>) {
    for (i in 0 until other.size) {
        @Suppress("UNCHECKED_CAST") // Guaranteed by valid index.
        put(other.keys[i], other.values[i] as E)
    }
}

internal inline fun <E> LongSparseArray<E>.commonPutIfAbsent(key: Long, value: E): E? {
    val mapValue = get(key)
    if (mapValue == null) {
        // TODO avoid double binary search here
        put(key, value)
    }
    return mapValue
}

internal inline fun <E> LongSparseArray<E>.commonAppend(key: Long, value: E) {
    if (_size != 0 && key <= keys[_size - 1]) {
        put(key, value)
        return
    }
    if (garbage && _size >= keys.size) {
        gc()
    }
    val pos = _size
    if (pos >= keys.size) {
        val newSize = idealIntArraySize(pos + 1)
        keys = keys.copyOf(newSize)
        values = values.copyOf(newSize)
    }
    keys[pos] = key
    values[pos] = value
    _size = pos + 1
}

internal inline fun <E> LongSparseArray<E>.commonKeyAt(index: Int): Long {
    if (garbage) {
        gc()
    }
    return keys[index]
}

internal inline fun <E> LongSparseArray<E>.commonValueAt(index: Int): E {
    if (garbage) {
        gc()
    }
    @Suppress("UNCHECKED_CAST") // Guaranteed by having run GC.
    return values[index] as E
}

internal inline fun <E> LongSparseArray<E>.commonSetValueAt(index: Int, value: E) {
    if (garbage) {
        gc()
    }
    values[index] = value
}

internal inline fun <E> LongSparseArray<E>.commonIndexOfKey(key: Long): Int {
    if (garbage) {
        gc()
    }
    return keys.binarySearch(_size, key)
}

internal inline fun <E> LongSparseArray<E>.commonIndexOfValue(value: E): Int {
    if (garbage) {
        gc()
    }
    for (i in 0 until _size) {
        if (values[i] === value) {
            return i
        }
    }
    return -1
}

internal inline fun <E> LongSparseArray<E>.commonContainsKey(key: Long): Boolean {
    return indexOfKey(key) >= 0
}

internal inline fun <E> LongSparseArray<E>.commonContainsValue(value: E): Boolean {
    return indexOfValue(value) >= 0
}

internal inline fun <E> LongSparseArray<E>.commonClear() {
    values.fill(null, toIndex = _size)
    _size = 0
    garbage = false
}

internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long) {
    val index = keys.binarySearch(_size, key)
    if (index >= 0 && values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long, value: Any?): Boolean {
    val index = indexOfKey(key)
    if (index >= 0) {
        val mapValue = valueAt(index)
        if (value == mapValue) {
            removeAt(index)
            return true
        }
    }
    return false
}

internal inline fun <E> LongSparseArray<E>.commonRemoveAt(index: Int) {
    if (values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

internal inline fun <E> LongSparseArray<E>.commonReplace(key: Long, value: E): E? {
    val index = indexOfKey(key)
    if (index >= 0) {
        @Suppress("UNCHECKED_CAST") // Guaranteed by index which would have run GC.
        val oldValue = values[index] as E
        values[index] = value
        return oldValue
    }
    return null
}

internal inline fun <E> LongSparseArray<E>.commonReplace(
    key: Long,
    oldValue: E?,
    newValue: E
): Boolean {
    val index = indexOfKey(key)
    if (index >= 0) {
        val mapValue = values[index]
        if (mapValue == oldValue) {
            values[index] = newValue
            return true
        }
    }
    return false
}

internal inline fun <E> LongSparseArray<E>.commonClone(): LongSparseArray<E> {
    val new = LongSparseArray<E>(0)
    new._size = _size
    new.keys = keys.copyOf()
    new.values = values.copyOf()
    new.garbage = garbage
    new.gc()
    return new
}

internal inline fun <E> LongSparseArray<E>.commonToString(): String {
    if (size == 0) {
        return "{}"
    }
    return buildString(_size * 20) {
        append('{')
        for (i in 0 until _size) {
            if (i > 0) {
                append(", ")
            }
            val key = keyAt(i)
            append(key)
            append('=')
            val value = valueAt(i)
            if (value !== this) {
                append(value)
            } else {
                append("(this Map)")
            }
        }
        append('}')
    }
}

internal fun <E> LongSparseArray<E>.gc() {
    var newSize = 0
    val keys = keys
    val values = values
    for (i in 0 until _size) {
        val value = values[i]
        if (value !== DELETED) {
            if (i != newSize) {
                keys[newSize] = keys[i]
                values[newSize] = value
                values[i] = null
            }
            newSize++
        }
    }
    garbage = false
    _size = newSize
}
