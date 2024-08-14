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
import androidx.collection.internal.idealIntArraySize

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
public actual open class SparseArrayCompat<E>
// JvmOverloads is required on constructor to match expect declaration
@kotlin.jvm.JvmOverloads
public actual constructor(initialCapacity: Int) {
    internal actual var garbage = false
    internal actual var keys: IntArray
    internal actual var values: Array<Any?>
    internal actual var size = 0

    init {
        if (initialCapacity == 0) {
            keys = EMPTY_INTS
            values = EMPTY_OBJECTS
        } else {
            val capacity = idealIntArraySize(initialCapacity)
            keys = IntArray(capacity)
            values = arrayOfNulls(capacity)
        }
    }

    /**
     * Gets the Object mapped from the specified key, or `null` if no such mapping has been made.
     */
    public actual open operator fun get(key: Int): E? = commonGet(key)

    /**
     * Gets the Object mapped from the specified [key], or [defaultValue] if no such mapping has
     * been made.
     */
    public actual open fun get(key: Int, defaultValue: E): E = commonGet(key, defaultValue)

    /** Removes the mapping from the specified key, if there was any. */
    public actual open fun remove(key: Int): Unit = commonRemove(key)

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns `true` if the mapping was removed.
     */
    // Note: value is Any? here for JVM source compatibility.
    public actual open fun remove(key: Int, value: Any?): Boolean = commonRemove(key, value)

    /** Removes the mapping at the specified index. */
    public actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)

    /**
     * Remove a range of mappings as a batch.
     *
     * @param index Index to begin at
     * @param size Number of mappings to remove
     */
    public actual open fun removeAtRange(index: Int, size: Int): Unit =
        commonRemoveAtRange(index, size)

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public actual open fun replace(key: Int, value: E): E? = commonReplace(key, value)

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public actual open fun replace(key: Int, oldValue: E, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    /**
     * Adds a mapping from the specified key to the specified [value], replacing the previous
     * mapping from the specified [key] if there was one.
     */
    public actual open fun put(key: Int, value: E): Unit = commonPut(key, value)

    /**
     * Copies all of the mappings from the [other] to this map. The effect of this call is
     * equivalent to that of calling [put] on this map once for each mapping from key to value in
     * [other].
     */
    public actual open fun putAll(other: SparseArrayCompat<out E>): Unit = commonPutAll(other)

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     *
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or `null` if there was no such
     *   key.
     */
    public actual open fun putIfAbsent(key: Int, value: E): E? = commonPutIfAbsent(key, value)

    /** Returns the number of key-value mappings that this SparseArray currently stores. */
    public actual open fun size(): Int = commonSize()

    /**
     * Return true if [size] is 0.
     *
     * @return true if [size] is 0.
     */
    public actual open fun isEmpty(): Boolean = commonIsEmpty()

    /**
     * Given an index in the range `0...size()-1`, returns the key from the [index]th key-value
     * mapping that this SparseArray stores.
     */
    public actual open fun keyAt(index: Int): Int = commonKeyAt(index)

    /**
     * Given an index in the range `0...size()-1`, returns the value from the [index]th key-value
     * mapping that this SparseArray stores.
     */
    public actual open fun valueAt(index: Int): E = commonValueAt(index)

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the [index]th key-value
     * mapping that this SparseArray stores.
     */
    public actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)

    /**
     * Returns the index for which [keyAt] would return the specified [key], or a negative number if
     * the specified [key] is not mapped.
     *
     * @param key the key to search for
     * @return the index for which [keyAt] would return the specified [key], or a negative number if
     *   the specified [key] is not mapped
     */
    public actual open fun indexOfKey(key: Int): Int = commonIndexOfKey(key)

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
    public actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)

    /** Returns true if the specified key is mapped. */
    public actual open fun containsKey(key: Int): Boolean = commonContainsKey(key)

    /** Returns true if the specified value is mapped from any key. */
    public actual open fun containsValue(value: E): Boolean = commonContainsValue(value)

    /** Removes all key-value mappings from this SparseArray. */
    public actual open fun clear(): Unit = commonClear()

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public actual open fun append(key: Int, value: E): Unit = commonAppend(key, value)

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If this map contains
     * itself as a value, the string "(this Map)" will appear in its place.
     */
    public actual override fun toString(): String = commonToString()
}
