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

// Note: This only applies to the top level, file-class "LongSparseArraKt" and not the entire file.
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.collection

import androidx.annotation.RestrictTo
import androidx.collection.internal.EMPTY_LONGS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.binarySearch
import androidx.collection.internal.idealLongArraySize

private val DELETED = Any()

/**
 * SparseArray mapping longs to Objects. Unlike a normal array of Objects, there can be gaps in the
 * indices. It is intended to be more memory efficient than using a HashMap to map Longs to Objects,
 * both because it avoids auto-boxing keys and its data structure doesn't rely on an extra entry
 * object for each mapping.
 *
 *
 * Note that this container keeps its mappings in an array data structure, using a binary search to
 * find keys. The implementation is not intended to be appropriate for data structures that may
 * contain large numbers of items. It is generally slower than a traditional HashMap, since lookups
 * require a binary search and adds and removes require inserting and deleting entries in the array.
 * For containers holding up to hundreds of items, the performance difference is not significant,
 * less than 50%.
 *
 *
 * To help with performance, the container includes an optimization when removing keys: instead of
 * compacting its array immediately, it leaves the removed entry marked as deleted. The entry can
 * then be re-used for the same key, or compacted later in a single garbage collection step of all
 * removed entries. This garbage collection will need to be performed at any time the array needs to
 * be grown or the map size or entry values are retrieved.
 *
 *
 * It is possible to iterate over the items in this container using [keyAt] and [valueAt]. Iterating
 * over the keys using [keyAt] with ascending values of the index will return the keys in ascending
 * order, or the values corresponding to the keys in ascending order in the case of [valueAt].
 */
public open class LongSparseArray<E>

/**
 * Creates a new [LongSparseArray] containing no mappings that will not require any additional
 * memory allocation to store the specified number of mappings. If you supply an initial capacity
 * of 0, the sparse array will be initialized with a light-weight representation not requiring any
 * additional array allocations.
 */
@JvmOverloads public constructor(initialCapacity: Int = 10) : Cloneable {
    private var garbage = false
    private var keys: LongArray
    private var values: Array<Any?>
    private var size = 0

    init {
        if (initialCapacity == 0) {
            keys = EMPTY_LONGS
            values = EMPTY_OBJECTS
        } else {
            val idealCapacity = idealLongArraySize(initialCapacity)
            keys = LongArray(idealCapacity)
            values = arrayOfNulls(idealCapacity)
        }
    }

    public override fun clone(): LongSparseArray<E> {
        @Suppress("UNCHECKED_CAST")
        val clone: LongSparseArray<E> = super.clone() as LongSparseArray<E>
        clone.keys = keys.clone()
        clone.values = values.clone()
        return clone
    }

    /**
     * Gets the value mapped from the specified [key], or `null` if no such mapping has been made.
     */
    public open operator fun get(key: Long): E? {
        return getInternal(key, null)
    }

    /**
     * Gets the value mapped from the specified [key], or [defaultValue] if no such mapping has been
     * made.
     */
    @Suppress("KotlinOperator") // Avoid confusion with matrix access syntax.
    public open fun get(key: Long, defaultValue: E): E {
        return getInternal(key, defaultValue)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T : E?> getInternal(key: Long, defaultValue: T): T {
        val i = binarySearch(keys, size, key)
        return if (i < 0 || values[i] === DELETED) {
            defaultValue
        } else {
            @Suppress("UNCHECKED_CAST")
            values[i] as T
        }
    }

    /**
     * Removes the mapping from the specified [key], if there was any.
     */
    @Deprecated("Alias for `remove(key)`.", ReplaceWith("remove(key)"))
    public open fun delete(key: Long) {
        remove(key)
    }

    /**
     * Removes the mapping from the specified [key], if there was any.
     */
    public open fun remove(key: Long) {
        val i = binarySearch(keys, size, key)
        if (i >= 0) {
            if (values[i] !== DELETED) {
                values[i] = DELETED
                garbage = true
            }
        }
    }

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns true if the mapping was removed.
     */
    public open fun remove(key: Long, value: E): Boolean {
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

    /**
     * Removes the mapping at the specified index.
     */
    public open fun removeAt(index: Int) {
        if (values[index] !== DELETED) {
            values[index] = DELETED
            garbage = true
        }
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public open fun replace(key: Long, value: E): E? {
        val index = indexOfKey(key)
        if (index >= 0) {
            @Suppress("UNCHECKED_CAST")
            val oldValue = values[index] as E?
            values[index] = value
            return oldValue
        }
        return null
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public open fun replace(key: Long, oldValue: E, newValue: E): Boolean {
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

    private fun gc() {
        // Log.e("SparseArray", "gc start with " + mSize);
        val n = size
        var newSize = 0
        val keys = keys
        val values = values
        for (i in 0 until n) {
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
        size = newSize

        // Log.e("SparseArray", "gc end with " + mSize);
    }

    /**
     * Adds a mapping from the specified key to the specified value, replacing the previous mapping
     * from the specified key if there was one.
     */
    public open fun put(key: Long, value: E) {
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
                val n = idealLongArraySize(size + 1)
                val nkeys = LongArray(n)
                val nvalues = arrayOfNulls<Any>(n)

                // Log.e("SparseArray", "grow " + mKeys.length + " to " + n);
                System.arraycopy(keys, 0, nkeys, 0, keys.size)
                System.arraycopy(values, 0, nvalues, 0, values.size)
                keys = nkeys
                values = nvalues
            }
            if (size - i != 0) {
                // Log.e("SparseArray", "move " + (mSize - i));
                System.arraycopy(keys, i, keys, i + 1, size - i)
                System.arraycopy(values, i, values, i + 1, size - i)
            }
            keys[i] = key
            values[i] = value
            size++
        }
    }

    /**
     * Copies all of the mappings from [other] to this map. The effect of this call is equivalent to
     * that of calling [put] on this map once for each mapping from key to value in [other].
     */
    public open fun putAll(other: LongSparseArray<out E>) {
        val size = other.size()
        repeat(size) { i ->
            put(other.keyAt(i), other.valueAt(i))
        }
    }

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     *
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or `null` if there was no such
     * key.
     */
    public open fun putIfAbsent(key: Long, value: E): E? {
        val mapValue = get(key)
        if (mapValue == null) {
            put(key, value)
        }
        return mapValue
    }

    /**
     * Returns the number of key-value mappings that this [LongSparseArray] currently stores.
     */
    public open fun size(): Int {
        if (garbage) {
            gc()
        }
        return size
    }

    /**
     * Return `true` if [size] is 0.
     *
     * @return `true` if [size] is 0.
     */
    public open fun isEmpty(): Boolean = size() == 0

    /**
     * Given an index in the range `0...size()-1`, returns the key from the `index`th key-value
     * mapping that this [LongSparseArray] stores.
     *
     * The keys corresponding to indices in ascending order are guaranteed to be in ascending order,
     * e.g., `keyAt(0)` will return the smallest key and `keyAt(size()-1)` will return the largest
     * key.
     *
     * @throws IllegalArgumentException if [index] is not in the range `0...size()-1`
     */
    public open fun keyAt(index: Int): Long {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        if (garbage) {
            gc()
        }
        return keys[index]
    }

    /**
     * Given an index in the range `0...size()-1`, returns the value from the `index`th key-value
     * mapping that this [LongSparseArray] stores.
     *
     * The values corresponding to indices in ascending order are guaranteed to be associated with
     * keys in ascending order, e.g., `valueAt(0)` will return the value associated with the
     * smallest key and `valueAt(size()-1)` will return the value associated with the largest key.
     *
     * @throws IllegalArgumentException if [index] is not in the range `0...size()-1`
     */
    public open fun valueAt(index: Int): E {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        if (garbage) {
            gc()
        }

        @Suppress("UNCHECKED_CAST")
        return values[index] as E
    }

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the `index`th key-value
     * mapping that this [LongSparseArray] stores.
     *
     * @throws IllegalArgumentException if [index] is not in the range `0...size()-1`
     */
    public open fun setValueAt(index: Int, value: E) {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        if (garbage) {
            gc()
        }
        values[index] = value
    }

    /**
     * Returns the index for which [keyAt] would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public open fun indexOfKey(key: Long): Int {
        if (garbage) {
            gc()
        }
        return binarySearch(keys, size, key)
    }

    /**
     * Returns an index for which [valueAt] would return the specified key, or a negative number if
     * no keys map to the specified [value].
     *
     * Beware that this is a linear search, unlike lookups by key, and that multiple keys can map to
     * the same value and this will find only one of them.
     */
    public open fun indexOfValue(value: E): Int {
        if (garbage) {
            gc()
        }
        repeat(size) { i ->
            if (values[i] === value) {
                return i
            }
        }
        return -1
    }

    /** Returns `true` if the specified [key] is mapped. */
    public open fun containsKey(key: Long): Boolean {
        return indexOfKey(key) >= 0
    }

    /** Returns `true` if the specified [value] is mapped from any key. */
    public open fun containsValue(value: E): Boolean {
        return indexOfValue(value) >= 0
    }

    /**
     * Removes all key-value mappings from this [LongSparseArray].
     */
    public open fun clear() {
        val n = size
        val values = values
        for (i in 0 until n) {
            values[i] = null
        }
        size = 0
        garbage = false
    }

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public open fun append(key: Long, value: E) {
        if (size != 0 && key <= keys[size - 1]) {
            put(key, value)
            return
        }
        if (garbage && size >= keys.size) {
            gc()
        }
        val pos = size
        if (pos >= keys.size) {
            val n = idealLongArraySize(pos + 1)
            val nkeys = LongArray(n)
            val nvalues = arrayOfNulls<Any>(n)

            // Log.e("SparseArray", "grow " + mKeys.length + " to " + n);
            System.arraycopy(keys, 0, nkeys, 0, keys.size)
            System.arraycopy(values, 0, nvalues, 0, values.size)
            keys = nkeys
            values = nvalues
        }
        keys[pos] = key
        values[pos] = value
        size = pos + 1
    }

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If this map contains
     * itself as a value, the string "(this Map)" will appear in its place.
     */
    override fun toString(): String {
        if (size() <= 0) {
            return "{}"
        }
        return buildString(size * 28) {
            append('{')
            for (i in 0 until size) {
                if (i > 0) {
                    append(", ")
                }
                val key = keyAt(i)
                append(key)
                append('=')
                val value: Any? = valueAt(i)
                if (value !== this) {
                    append(value)
                } else {
                    append("(this Map)")
                }
            }
            append('}')
        }
    }
}