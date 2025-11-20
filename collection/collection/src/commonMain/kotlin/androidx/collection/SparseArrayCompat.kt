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

import androidx.collection.internal.binarySearch
import androidx.collection.internal.idealIntArraySize
import kotlin.math.min

private val DELETED = Any()

/**
 * SparseArrays map integers to Objects. Unlike a normal array of Objects, there can be gaps in the
 * indices. It is intended to be more memory efficient than using a HashMap to map Integers to
 * Objects, both because it avoids auto-boxing keys and its data structure doesn't rely on an extra
 * entry object for each mapping.
 *
 * Note that this container keeps its mappings in an array data structure, using a binary search to
 * find keys. The implementation is not intended to be appropriate for data structures that may
 * contain large numbers of items. It is generally slower than a traditional HashMap, since lookups
 * require a binary search and adds and removes require inserting and deleting entries in the array.
 * For containers holding up to hundreds of items, the performance difference is not significant,
 * less than 50%.
 *
 * To help with performance, the container includes an optimization when removing keys: instead of
 * compacting its array immediately, it leaves the removed entry marked as deleted. The entry can
 * then be re-used for the same key, or compacted later in a single garbage collection step of all
 * removed entries. This garbage collection will need to be performed at any time the array needs to
 * be grown or the map size or entry values are retrieved.
 *
 * It is possible to iterate over the items in this container using [keyAt] and [valueAt]. Iterating
 * over the keys using [keyAt] with ascending values of the index will return the keys in ascending
 * order, or the values corresponding to the keys in ascending order in the case of [valueAt].
 *
 * @param initialCapacity initial capacity of the array. The array will not require any additional
 *   memory allocation to store the specified number of mappings. If you supply an initialCapacity
 *   of 0, the sparse array will be initialized with a light-weight representation not requiring any
 *   additional array allocations. Default initialCapacity is 10.
 * @constructor Creates a new SparseArray containing no mappings that will not require any
 *   additional memory allocation to store the specified number of mappings. If you supply an
 *   initial capacity of 0, the sparse array will be initialized with a light-weight representation
 *   not requiring any additional array allocations.
 */
public expect open class SparseArrayCompat<E> public constructor(initialCapacity: Int = 10) {
    internal var garbage: Boolean
    internal var keys: IntArray
    internal var values: Array<Any?>
    internal var size: Int

    /**
     * Gets the Object mapped from the specified key, or `null` if no such mapping has been made.
     */
    public open operator fun get(key: Int): E?

    /**
     * Gets the Object mapped from the specified [key], or [defaultValue] if no such mapping has
     * been made.
     */
    public open fun get(key: Int, defaultValue: E): E

    /** Removes the mapping from the specified key, if there was any. */
    public open fun remove(key: Int): Unit

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns `true` if the mapping was removed.
     */
    // Note: value is Any? here for source compatibility.
    public open fun remove(key: Int, value: Any?): Boolean

    /** Removes the mapping at the specified index. */
    public open fun removeAt(index: Int): Unit

    /**
     * Remove a range of mappings as a batch.
     *
     * @param index Index to begin at
     * @param size Number of mappings to remove
     */
    public open fun removeAtRange(index: Int, size: Int): Unit

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public open fun replace(key: Int, value: E): E?

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public open fun replace(key: Int, oldValue: E, newValue: E): Boolean

    /**
     * Adds a mapping from the specified key to the specified [value], replacing the previous
     * mapping from the specified [key] if there was one.
     */
    public open fun put(key: Int, value: E): Unit

    /**
     * Copies all of the mappings from the [other] to this map. The effect of this call is
     * equivalent to that of calling [put] on this map once for each mapping from key to value in
     * [other].
     */
    public open fun putAll(other: SparseArrayCompat<out E>): Unit

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     *
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or `null` if there was no such
     *   key.
     */
    public open fun putIfAbsent(key: Int, value: E): E?

    /** Returns the number of key-value mappings that this SparseArray currently stores. */
    public open fun size(): Int

    /**
     * Return true if [size] is 0.
     *
     * @return true if [size] is 0.
     */
    public open fun isEmpty(): Boolean

    /**
     * Given an index in the range `0...[size]-1`, returns the key from the [index]th key-value
     * mapping that this SparseArray stores.
     *
     * @throws IndexOutOfBoundsException if index is not within `0..[size]-1`
     */
    public open fun keyAt(index: Int): Int

    /**
     * Given an index in the range `0...[size]-1`, returns the value from the [index]th key-value
     * mapping that this SparseArray stores.
     *
     * @throws IndexOutOfBoundsException if index is not within `0..[size]-1`
     */
    public open fun valueAt(index: Int): E

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the [index]th key-value
     * mapping that this SparseArray stores.
     */
    public open fun setValueAt(index: Int, value: E): Unit

    /**
     * Returns the index for which [keyAt] would return the specified [key], or a negative number if
     * the specified [key] is not mapped.
     *
     * @param key the key to search for
     * @return the index for which [keyAt] would return the specified [key], or a negative number if
     *   the specified [key] is not mapped
     */
    public open fun indexOfKey(key: Int): Int

    /**
     * Returns an index for which [valueAt] would return the specified key, or a negative number if
     * no keys map to the specified [value].
     *
     * Beware that this is a linear search, unlike lookups by key, and that multiple keys can map to
     * the same value and this will find only one of them.
     *
     * Note also that unlike most collections' [indexOf] methods, this method compares values using
     * `===` rather than [equals].
     */
    public open fun indexOfValue(value: E): Int

    /** Returns true if the specified key is mapped. */
    public open fun containsKey(key: Int): Boolean

    /** Returns true if the specified value is mapped from any key. */
    public open fun containsValue(value: E): Boolean

    /** Removes all key-value mappings from this SparseArray. */
    public open fun clear(): Unit

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public open fun append(key: Int, value: E): Unit

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If this map contains
     * itself as a value, the string "(this Map)" will appear in its place.
     */
    override fun toString(): String
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <E, T : E?> SparseArrayCompat<E>.internalGet(key: Int, defaultValue: T): T {
    val i = binarySearch(keys, size, key)
    return if (i < 0 || values[i] === DELETED) {
        defaultValue
    } else {
        @Suppress("UNCHECKED_CAST")
        values[i] as T
    }
}

@Suppress("NOTHING_TO_INLINE")
internal fun <E> SparseArrayCompat<E>.commonGet(key: Int): E? {
    // TODO(b/375562182) revert the change done here in aosp/375562182 after lib targets K2
    return internalGet<E, E?>(key, null)
}

@Suppress("NOTHING_TO_INLINE")
internal fun <E> SparseArrayCompat<E>.commonGet(key: Int, defaultValue: E): E {
    // TODO(b/375562182) revert the change done here in aosp/375562182 after lib targets K2
    return internalGet<E, E>(key, defaultValue)
}

@Suppress("NOTHING_TO_INLINE")
internal fun <E> SparseArrayCompat<E>.commonRemove(key: Int) {
    val i = binarySearch(keys, size, key)
    if (i >= 0 && values[i] !== DELETED) {
        values[i] = DELETED
        garbage = true
    }
}

// Note: value is Any? here for JVM source compatibility.
@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonRemove(key: Int, value: Any?): Boolean {
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

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonRemoveAt(index: Int) {
    if (values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonRemoveAtRange(index: Int, size: Int) {
    val end = min(size, index + size)
    for (i in index until end) {
        removeAt(i)
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonReplace(key: Int, value: E): E? {
    val index = indexOfKey(key)
    if (index >= 0) {
        @Suppress("UNCHECKED_CAST") val oldValue = values[index] as E
        values[index] = value
        return oldValue
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonReplace(
    key: Int,
    oldValue: E,
    newValue: E,
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

private fun <E> SparseArrayCompat<E>.gc() {
    val n = size
    var o = 0
    val keys = keys
    val values = values
    for (i in 0 until n) {
        val value = values[i]
        if (value !== DELETED) {
            if (i != o) {
                keys[o] = keys[i]
                values[o] = value
                values[i] = null
            }
            o++
        }
    }
    garbage = false
    size = o
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonPut(key: Int, value: E) {
    var i = binarySearch(keys, size, key)
    if (i >= 0) {
        values[i] = value
    } else {
        i = i.inv()
        if (i < size && values[i] === DELETED) {
            keys[i] = key
            values[i] = value
            return
        }
        if (garbage && size >= keys.size) {
            gc()

            // Search again because indices may have changed.
            i = binarySearch(keys, size, key).inv()
        }
        if (size >= keys.size) {
            val n = idealIntArraySize(size + 1)
            keys = keys.copyOf(newSize = n)
            values = values.copyOf(newSize = n)
        }
        if (size - i != 0) {
            keys.copyInto(keys, destinationOffset = i + 1, startIndex = i, endIndex = size)
            values.copyInto(values, destinationOffset = i + 1, startIndex = i, endIndex = size)
        }
        keys[i] = key
        values[i] = value
        size++
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonPutAll(other: SparseArrayCompat<out E>) {
    for (i in 0 until other.size()) {
        commonPut(other.keyAt(i), other.valueAt(i))
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonPutIfAbsent(key: Int, value: E): E? {
    val mapValue = commonGet(key)
    if (mapValue == null) {
        commonPut(key, value)
    }
    return mapValue
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonSize(): Int {
    if (garbage) {
        gc()
    }
    return size
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonIsEmpty(): Boolean = size() == 0

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonKeyAt(index: Int): Int {
    if (garbage) {
        gc()
    }
    if (index >= size || index < 0) {
        throw CollectionPlatformUtils.createIndexOutOfBoundsException()
    }
    return keys[index]
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonValueAt(index: Int): E {
    if (garbage) {
        gc()
    }
    if (index >= size || index < 0) {
        throw CollectionPlatformUtils.createIndexOutOfBoundsException()
    }
    @Suppress("UNCHECKED_CAST")
    return values[index] as E
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonSetValueAt(index: Int, value: E) {
    if (garbage) {
        gc()
    }
    values[index] = value
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonIndexOfKey(key: Int): Int {
    if (garbage) {
        gc()
    }
    return binarySearch(keys, size, key)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonIndexOfValue(value: E): Int {
    if (garbage) {
        gc()
    }
    for (i in 0 until size) {
        if (values[i] === value) {
            return i
        }
    }
    return -1
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonContainsKey(key: Int): Boolean {
    return indexOfKey(key) >= 0
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonContainsValue(value: E): Boolean {
    return commonIndexOfValue(value) >= 0
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonClear() {
    val n = size
    val values = values
    for (i in 0 until n) {
        values[i] = null
    }
    size = 0
    garbage = false
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonAppend(key: Int, value: E) {
    if (size != 0 && key <= keys[size - 1]) {
        put(key, value)
        return
    }
    if (garbage && size >= keys.size) {
        gc()
    }
    val pos = size
    if (pos >= keys.size) {
        val n = idealIntArraySize(pos + 1)
        keys = keys.copyOf(n)
        values = values.copyOf(n)
    }
    keys[pos] = key
    values[pos] = value
    size = pos + 1
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> SparseArrayCompat<E>.commonToString(): String {
    if (size() <= 0) {
        return "{}"
    }
    val buffer = StringBuilder(size * 28)
    buffer.append('{')
    for (i in 0 until size) {
        if (i > 0) {
            buffer.append(", ")
        }
        val key = keyAt(i)
        buffer.append(key)
        buffer.append('=')
        val value: Any? = valueAt(i)
        if (value !== this) {
            buffer.append(value)
        } else {
            buffer.append("(this Map)")
        }
    }
    buffer.append('}')
    return buffer.toString()
}
