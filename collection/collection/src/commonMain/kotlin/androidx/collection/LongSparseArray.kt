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

import androidx.collection.internal.binarySearch
import androidx.collection.internal.idealLongArraySize
import androidx.collection.internal.requirePrecondition
import kotlin.DeprecationLevel.HIDDEN

private val DELETED = Any()

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
 *   additional memory allocation to store the specified number of mappings. If you supply an
 *   initial capacity of 0, the sparse array will be initialized with a light-weight representation
 *   not requiring any additional array allocations.
 */
public expect open class LongSparseArray<E> public constructor(initialCapacity: Int = 10) {
    internal var garbage: Boolean
    internal var keys: LongArray
    internal var values: Array<Any?>
    internal var size: Int

    /**
     * Gets the value mapped from the specified [key], or `null` if no such mapping has been made.
     */
    public open operator fun get(key: Long): E?

    /**
     * Gets the value mapped from the specified [key], or [defaultValue] if no such mapping has been
     * made.
     */
    @Suppress("KotlinOperator") // Avoid confusion with matrix access syntax.
    public open fun get(key: Long, defaultValue: E): E

    /** Removes the mapping from the specified [key], if there was any. */
    @Deprecated("Alias for `remove(key)`.", ReplaceWith("remove(key)"))
    public open fun delete(key: Long): Unit

    /** Removes the mapping from the specified [key], if there was any. */
    public open fun remove(key: Long): Unit

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns `true` if the mapping was removed.
     */
    public open fun remove(key: Long, value: E): Boolean

    /** Removes the mapping at the specified [index]. */
    public open fun removeAt(index: Int): Unit

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public open fun replace(key: Long, value: E): E?

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public open fun replace(key: Long, oldValue: E, newValue: E): Boolean

    /**
     * Adds a mapping from the specified key to the specified value, replacing the previous mapping
     * from the specified key if there was one.
     */
    public open fun put(key: Long, value: E): Unit

    /**
     * Copies all of the mappings from [other] to this map. The effect of this call is equivalent to
     * that of calling [put] on this map once for each mapping from key to value in [other].
     */
    public open fun putAll(other: LongSparseArray<out E>): Unit

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     *
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or `null` if there was no such
     *   key.
     */
    public open fun putIfAbsent(key: Long, value: E): E?

    /** Returns the number of key-value mappings that this [LongSparseArray] currently stores. */
    public open fun size(): Int

    /**
     * Return `true` if [size] is 0.
     *
     * @return `true` if [size] is 0.
     */
    public open fun isEmpty(): Boolean

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
    public open fun keyAt(index: Int): Long

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
    public open fun valueAt(index: Int): E

    /**
     * Given an index in the range `0...size()-1`, sets a new value for the `index`th key-value
     * mapping that this [LongSparseArray] stores.
     *
     * @throws IllegalArgumentException if [index] is not in the range `0...size()-1`
     */
    public open fun setValueAt(index: Int, value: E): Unit

    /**
     * Returns the index for which [keyAt] would return the specified key, or a negative number if
     * the specified key is not mapped.
     */
    public open fun indexOfKey(key: Long): Int

    /**
     * Returns an index for which [valueAt] would return the specified key, or a negative number if
     * no keys map to the specified [value].
     *
     * Beware that this is a linear search, unlike lookups by key, and that multiple keys can map to
     * the same value and this will find only one of them.
     */
    public open fun indexOfValue(value: E): Int

    /** Returns `true` if the specified [key] is mapped. */
    public open fun containsKey(key: Long): Boolean

    /** Returns `true` if the specified [value] is mapped from any key. */
    public open fun containsValue(value: E): Boolean

    /** Removes all key-value mappings from this [LongSparseArray]. */
    public open fun clear(): Unit

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public open fun append(key: Long, value: E): Unit

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If this map contains
     * itself as a value, the string "(this Map)" will appear in its place.
     */
    override fun toString(): String
}

// TODO(KT-20427): Move these into the expect once support is added for default implementations.

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonGet(key: Long): E? {
    return commonGetInternal(key, null)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonGet(key: Long, defaultValue: E): E {
    return commonGetInternal(key, defaultValue)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T : E?, E> LongSparseArray<E>.commonGetInternal(
    key: Long,
    defaultValue: T
): T {
    val i = binarySearch(keys, size, key)
    return if (i < 0 || values[i] === DELETED) {
        defaultValue
    } else {
        @Suppress("UNCHECKED_CAST")
        values[i] as T
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long) {
    val i = binarySearch(keys, size, key)
    if (i >= 0) {
        if (values[i] !== DELETED) {
            values[i] = DELETED
            garbage = true
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonRemove(key: Long, value: E): Boolean {
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
internal inline fun <E> LongSparseArray<E>.commonRemoveAt(index: Int) {
    if (values[index] !== DELETED) {
        values[index] = DELETED
        garbage = true
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonReplace(key: Long, value: E): E? {
    val index = indexOfKey(key)
    if (index >= 0) {
        @Suppress("UNCHECKED_CAST") val oldValue = values[index] as E?
        values[index] = value
        return oldValue
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonReplace(
    key: Long,
    oldValue: E,
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

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonGc() {
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
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonPut(key: Long, value: E) {
    var index = binarySearch(keys, size, key)
    if (index >= 0) {
        values[index] = value
    } else {
        index = index.inv()
        if (index < size && values[index] === DELETED) {
            keys[index] = key
            values[index] = value
            return
        }
        if (garbage && size >= keys.size) {
            commonGc()

            // Search again because indices may have changed.
            index = binarySearch(keys, size, key).inv()
        }
        if (size >= keys.size) {
            val newSize = idealLongArraySize(size + 1)
            keys = keys.copyOf(newSize)
            values = values.copyOf(newSize)
        }
        if (size - index != 0) {
            keys.copyInto(keys, destinationOffset = index + 1, startIndex = index, endIndex = size)
            values.copyInto(
                values,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = size
            )
        }
        keys[index] = key
        values[index] = value
        size++
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonPutAll(other: LongSparseArray<out E>) {
    val size = other.size()
    repeat(size) { i -> put(other.keyAt(i), other.valueAt(i)) }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonPutIfAbsent(key: Long, value: E): E? {
    val mapValue = get(key)
    if (mapValue == null) {
        put(key, value)
    }
    return mapValue
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonSize(): Int {
    if (garbage) {
        commonGc()
    }
    return size
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonIsEmpty(): Boolean = size() == 0

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonKeyAt(index: Int): Long {
    requirePrecondition(index in 0 until size) {
        "Expected index to be within 0..size()-1, but was $index"
    }

    if (garbage) {
        commonGc()
    }
    return keys[index]
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonValueAt(index: Int): E {
    requirePrecondition(index in 0 until size) {
        "Expected index to be within 0..size()-1, but was $index"
    }

    if (garbage) {
        commonGc()
    }

    @Suppress("UNCHECKED_CAST") return values[index] as E
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonSetValueAt(index: Int, value: E) {
    requirePrecondition(index in 0 until size) {
        "Expected index to be within 0..size()-1, but was $index"
    }

    if (garbage) {
        commonGc()
    }
    values[index] = value
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonIndexOfKey(key: Long): Int {
    if (garbage) {
        commonGc()
    }
    return binarySearch(keys, size, key)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonIndexOfValue(value: E): Int {
    if (garbage) {
        commonGc()
    }
    repeat(size) { i ->
        if (values[i] === value) {
            return i
        }
    }
    return -1
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonContainsKey(key: Long): Boolean {
    return indexOfKey(key) >= 0
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonContainsValue(value: E): Boolean {
    return indexOfValue(value) >= 0
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonClear() {
    val n = size
    val values = values
    for (i in 0 until n) {
        values[i] = null
    }
    size = 0
    garbage = false
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonAppend(key: Long, value: E) {
    if (size != 0 && key <= keys[size - 1]) {
        put(key, value)
        return
    }
    if (garbage && size >= keys.size) {
        commonGc()
    }
    val pos = size
    if (pos >= keys.size) {
        val newSize = idealLongArraySize(pos + 1)
        keys = keys.copyOf(newSize)
        values = values.copyOf(newSize)
    }
    keys[pos] = key
    values[pos] = value
    size = pos + 1
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <E> LongSparseArray<E>.commonToString(): String {
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

/** Returns the number of key/value pairs in the collection. */
@Suppress("NOTHING_TO_INLINE")
public inline val <T> LongSparseArray<T>.size: Int
    get() = size()

/** Returns true if the collection contains [key]. */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> LongSparseArray<T>.contains(key: Long): Boolean = containsKey(key)

/** Allows the use of the index operator for storing values in the collection. */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> LongSparseArray<T>.set(key: Long, value: T): Unit = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
public operator fun <T> LongSparseArray<T>.plus(other: LongSparseArray<T>): LongSparseArray<T> {
    val new = LongSparseArray<T>(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Return the value corresponding to [key], or [defaultValue] when not present. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> LongSparseArray<T>.getOrDefault(key: Long, defaultValue: T): T =
    get(key, defaultValue)

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> LongSparseArray<T>.getOrElse(key: Long, defaultValue: () -> T): T =
    get(key) ?: defaultValue()

/** Return true when the collection contains elements. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> LongSparseArray<T>.isNotEmpty(): Boolean = !isEmpty()

/** Removes the entry for [key] only if it is mapped to [value]. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // Binary API compatibility.
@Deprecated(message = "Replaced with member function. Remove extension import!", level = HIDDEN)
public fun <T> LongSparseArray<T>.remove(key: Long, value: T): Boolean = remove(key, value)

/** Performs the given [action] for each key/value entry. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> LongSparseArray<T>.forEach(action: (key: Long, value: T) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
public fun <T> LongSparseArray<T>.keyIterator(): LongIterator =
    object : LongIterator() {
        var index = 0

        override fun hasNext() = index < size()

        override fun nextLong() = keyAt(index++)
    }

/** Return an iterator over the collection's values. */
public fun <T> LongSparseArray<T>.valueIterator(): Iterator<T> =
    object : Iterator<T> {
        var index = 0

        override fun hasNext() = index < size()

        override fun next() = valueAt(index++)
    }
