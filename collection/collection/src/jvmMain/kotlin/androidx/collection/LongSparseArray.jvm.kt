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

import androidx.collection.internal.EMPTY_LONGS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.idealLongArraySize

/**
 * SparseArray mapping longs to Objects. Unlike a normal array of Objects, there can be gaps in the
 * indices. It is intended to be more memory efficient than using a HashMap to map Longs to Objects,
 * both because it avoids auto-boxing keys and its data structure doesn't rely on an extra entry
 * object for each mapping.
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
 * @constructor Creates a new [LongSparseArray] containing no mappings that will not require any
 * additional memory allocation to store the specified number of mappings. If you supply an initial
 * capacity of 0, the sparse array will be initialized with a light-weight representation not
 * requiring any additional array allocations.
 */
public actual open class LongSparseArray<E>
@JvmOverloads public actual constructor(
    initialCapacity: Int
) : Cloneable {
    @JvmSynthetic // Hide from Java callers.
    @JvmField
    internal actual var garbage = false

    @JvmSynthetic // Hide from Java callers.
    @JvmField
    internal actual var keys: LongArray

    @JvmSynthetic // Hide from Java callers.
    @JvmField
    internal actual var values: Array<Any?>

    @JvmSynthetic // Hide from Java callers.
    @JvmField
    internal actual var size = 0

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
    public actual open operator fun get(key: Long): E? = commonGet(key)

    /**
     * Gets the value mapped from the specified [key], or [defaultValue] if no such mapping has been
     * made.
     */
    @Suppress("KotlinOperator") // Avoid confusion with matrix access syntax.
    public actual open fun get(key: Long, defaultValue: E): E = commonGet(key, defaultValue)

    /**
     * Removes the mapping from the specified [key], if there was any.
     */
    @Deprecated("Alias for `remove(key)`.", ReplaceWith("remove(key)"))
    public actual open fun delete(key: Long): Unit = commonRemove(key)

    /**
     * Removes the mapping from the specified [key], if there was any.
     */
    public actual open fun remove(key: Long): Unit = commonRemove(key)

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns true if the mapping was removed.
     */
    public actual open fun remove(key: Long, value: E): Boolean = commonRemove(key, value)

    /**
     * Removes the mapping at the specified index.
     */
    public actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public actual open fun replace(key: Long, value: E): E? = commonReplace(key, value)

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public actual open fun replace(key: Long, oldValue: E, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    /**
     * Adds a mapping from the specified key to the specified value, replacing the previous mapping
     * from the specified key if there was one.
     */
    public actual open fun put(key: Long, value: E): Unit = commonPut(key, value)

    /**
     * Copies all of the mappings from [other] to this map. The effect of this call is equivalent to
     * that of calling [put] on this map once for each mapping from key to value in [other].
     */
    public actual open fun putAll(other: LongSparseArray<out E>): Unit = commonPutAll(other)

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     *
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or `null` if there was no such
     * key.
     */
    public actual open fun putIfAbsent(key: Long, value: E): E? = commonPutIfAbsent(key, value)

    /**
     * Returns the number of key-value mappings that this [LongSparseArray] currently stores.
     */
    public actual open fun size(): Int = commonSize()

    /**
     * Return `true` if [size] is 0.
     *
     * @return `true` if [size] is 0.
     */
    public actual open fun isEmpty(): Boolean = commonIsEmpty()

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
    public actual open fun keyAt(index: Int): Long = commonKeyAt(index)

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
    public actual open fun valueAt(index: Int): E = commonValueAt(index)

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the `index`th key-value
     * mapping that this [LongSparseArray] stores.
     *
     * @throws IllegalArgumentException if [index] is not in the range `0...size()-1`
     */
    public actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)

    /**
     * Returns the index for which [keyAt] would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public actual open fun indexOfKey(key: Long): Int = commonIndexOfKey(key)

    /**
     * Returns an index for which [valueAt] would return the specified key, or a negative number if
     * no keys map to the specified [value].
     *
     * Beware that this is a linear search, unlike lookups by key, and that multiple keys can map to
     * the same value and this will find only one of them.
     */
    public actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)

    /** Returns `true` if the specified [key] is mapped. */
    public actual open fun containsKey(key: Long): Boolean = commonContainsKey(key)

    /** Returns `true` if the specified [value] is mapped from any key. */
    public actual open fun containsValue(value: E): Boolean = commonContainsValue(value)

    /**
     * Removes all key-value mappings from this [LongSparseArray].
     */
    public actual open fun clear(): Unit = commonClear()

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public actual open fun append(key: Long, value: E): Unit = commonAppend(key, value)

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If this map contains
     * itself as a value, the string "(this Map)" will appear in its place.
     */
    actual override fun toString(): String = commonToString()
}
