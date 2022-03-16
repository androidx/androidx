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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.collection

import androidx.annotation.RestrictTo
import androidx.collection.internal.EMPTY_INTS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.binarySearch

private const val DEBUG = false
private const val TAG = "ArrayMap"

/**
 * Attempt to spot concurrent modifications to this data structure.
 *
 * It's best-effort, but any time we can throw something more diagnostic than an
 * ArrayIndexOutOfBoundsException deep in the ArrayMap internals it's going to
 * save a lot of development time.
 *
 * Good times to look for CME include after any allocArrays() call and at the end of
 * functions that change mSize (put/remove/clear).
 */
private const val CONCURRENT_MODIFICATION_EXCEPTIONS = true

/**
 * The minimum amount by which the capacity of a ArrayMap will increase.
 * This is tuned to be relatively space-efficient.
 */
private const val BASE_SIZE = 4

/**
 * Base implementation of [ArrayMap] that doesn't include any standard Java
 * container API interoperability. These features are generally heavier-weight ways
 * to interact with the container, so discouraged, but they can be useful to make it
 * easier to use as a drop-in replacement for HashMap. If you don't need them, this
 * class can be preferable since it doesn't bring in any of the implementation of those
 * APIs, allowing that code to be stripped by ProGuard.
 */
public open class SimpleArrayMap<K, V>

/**
 * Create a new [SimpleArrayMap] with a given initial capacity. The default capacity of an array
 * map is 0, and will grow once items are added to it.
 */
@JvmOverloads public constructor(capacity: Int = 0) {
    private var hashes: IntArray = when (capacity) {
        0 -> EMPTY_INTS
        else -> IntArray(capacity)
    }

    private var array: Array<Any?> = when (capacity) {
        0 -> EMPTY_OBJECTS
        else -> arrayOfNulls<Any?>(capacity shl 1)
    }

    private var size: Int = 0

    /**
     * Create a new [SimpleArrayMap] with the mappings from the given [map].
     */
    public constructor(map: SimpleArrayMap<out K, out V>?) : this() {
        if (map != null) {
            this.putAll(map)
        }
    }

    /**
     * Returns the index of a key in the set, given its [hashCode]. This is a helper for the
     * non-null case of [indexOfKey].
     *
     * @param key The key to search for.
     * @param hash Pre-computed [hashCode] of [key].
     * @return Returns the index of the key if it exists, else a negative integer.
     */
    private fun indexOf(key: K, hash: Int): Int {
        val n = size

        // Important fast case: if nothing is in here, nothing to look for.
        if (n == 0) {
            return 0.inv()
        }
        val index = binarySearch(hashes, n, hash)

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index
        }

        // If the key at the returned index matches, that's what we want.
        if (key == array[index shl 1]) {
            return index
        }

        // Search for a matching key after the index.
        var end: Int = index + 1
        while (end < n && hashes[end] == hash) {
            if (key == array[end shl 1]) return end
            end++
        }

        // Search for a matching key before the index.
        var i = index - 1
        while (i >= 0 && hashes[i] == hash) {
            if (key == array[i shl 1]) {
                return i
            }
            i--
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go. We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    private fun indexOfNull(): Int {
        val n = size

        // Important fast case: if nothing is in here, nothing to look for.
        if (n == 0) {
            return 0.inv()
        }
        val index = binarySearch(hashes, n, 0)

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index
        }

        // If the key at the returned index matches, that's what we want.
        if (null == array[index shl 1]) {
            return index
        }

        // Search for a matching key after the index.
        var end: Int = index + 1
        while (end < n && hashes[end] == 0) {
            if (null == array[end shl 1]) return end
            end++
        }

        // Search for a matching key before the index.
        var i = index - 1
        while (i >= 0 && hashes[i] == 0) {
            if (null == array[i shl 1]) return i
            i--
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go. We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    /**
     * Make the array map empty. All storage is released.
     *
     * @throws ConcurrentModificationException if it was detected that this [SimpleArrayMap] was
     * written to while this operation was running.
     */
    public open fun clear() {
        if (size > 0) {
            hashes = EMPTY_INTS
            array = EMPTY_OBJECTS
            size = 0
        }
        @Suppress("KotlinConstantConditions")
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && size > 0) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Ensure the array map can hold at least [minimumCapacity] items.
     *
     * @throws ConcurrentModificationException if it was detected that this [SimpleArrayMap] was
     * written to while this operation was running.
     */
    public open fun ensureCapacity(minimumCapacity: Int) {
        val osize = size
        if (hashes.size < minimumCapacity) {
            hashes = hashes.copyOf(minimumCapacity)
            array = array.copyOf(minimumCapacity)
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && size != osize) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Check whether a key exists in the array.
     *
     * @param key The key to search for.
     * @return Returns `true` if the key exists, else `false`.
     */
    public open fun containsKey(key: K): Boolean {
        return indexOfKey(key) >= 0
    }

    /**
     * Returns the index of a key in the set.
     *
     * @param key The key to search for.
     * @return Returns the index of the key if it exists, else a negative integer.
     */
    public open fun indexOfKey(key: K): Int = when (key) {
        null -> indexOfNull()
        else -> indexOf(key, key.hashCode())
    }

    // @RestrictTo is required since internal is implemented as public with name mangling in Java
    // and we are overriding the name mangling to make it callable from a Java subclass in this
    // package (ArrayMap).
    @JvmName("__restricted\$indexOfValue")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun indexOfValue(value: V): Int {
        val n = size * 2
        val array = array
        if (value == null) {
            var i = 1
            while (i < n) {
                if (array[i] == null) {
                    return i shr 1
                }
                i += 2
            }
        } else {
            var i = 1
            while (i < n) {
                if (value == array[i]) {
                    return i shr 1
                }
                i += 2
            }
        }
        return -1
    }

    /**
     * Check whether a value exists in the array. This requires a linear search
     * through the entire array.
     *
     * @param value The value to search for.
     * @return Returns `true` if the value exists, else `false`.
     */
    public open fun containsValue(value: V): Boolean {
        return indexOfValue(value) >= 0
    }

    /**
     * Retrieve a value from the array.
     *
     * @param key The key of the value to retrieve.
     * @return Returns the value associated with the given key, or `null` if there is no such key.
     */
    public open operator fun get(key: K): V? {
        return getOrDefaultInternal(key, null)
    }

    /**
     * Retrieve a value from the array, or [defaultValue] if there is no mapping for the key.
     *
     * @param key The key of the value to retrieve.
     * @param defaultValue The default mapping of the key
     * @return Returns the value associated with the given key, or [defaultValue] if there is no
     * mapping for the key.
     */
    // Unfortunately key must stay of type Any? otherwise it will not register as an override of
    // Java's Map interface, which is necessary since ArrayMap is written in Java and implements
    // both Map and SimpleArrayMap.
    public open fun getOrDefault(key: Any?, defaultValue: V): V {
        return getOrDefaultInternal(key, defaultValue)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T : V?> getOrDefaultInternal(key: Any?, defaultValue: T): T {
        @Suppress("UNCHECKED_CAST")
        val index = indexOfKey(key as K)
        @Suppress("UNCHECKED_CAST")
        return when {
            index >= 0 -> array[(index shl 1) + 1] as T
            else -> defaultValue
        }
    }

    /**
     * Return the key at the given index in the array.
     *
     * @param index The desired index, must be between 0 and [size]-1 (inclusive).
     * @return Returns the key stored at the given index.
     * @throws IllegalArgumentException if [index] is not between 0 and [size]-1
     */
    public open fun keyAt(index: Int): K {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        @Suppress("UNCHECKED_CAST")
        return array[index shl 1] as K
    }

    /**
     * Return the value at the given index in the array.
     *
     * @param index The desired index, must be between 0 and [size]-1 (inclusive).
     * @return Returns the value stored at the given index.
     * @throws IllegalArgumentException if [index] is not between 0 and [size]-1
     */
    public open fun valueAt(index: Int): V {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        @Suppress("UNCHECKED_CAST")
        return array[(index shl 1) + 1] as V
    }

    /**
     * Set the value at a given index in the array.
     *
     * @param index The desired index, must be between 0 and [size]-1 (inclusive).
     * @param value The new value to store at this index.
     * @return Returns the previous value at the given index.
     * @throws IllegalArgumentException if [index] is not between 0 and [size]-1
     */
    public open fun setValueAt(index: Int, value: V): V {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        val indexInArray = (index shl 1) + 1

        @Suppress("UNCHECKED_CAST")
        val old = array[indexInArray] as V
        array[indexInArray] = value
        return old
    }

    /**
     * Return `true` if the array map contains no items.
     */
    public open fun isEmpty(): Boolean = size <= 0

    /**
     * Add a new value to the array map.
     *
     * @param key The key under which to store the value. If this key already exists in the array,
     * its value will be replaced.
     * @param value The value to store for the given key.
     * @return Returns the old value that was stored for the given key, or `null` if there
     * was no such key.
     * @throws ConcurrentModificationException if it was detected that this [SimpleArrayMap] was
     * written to while this operation was running.
     */
    public open fun put(key: K, value: V): V? {
        val osize = size
        val hash: Int = key?.hashCode() ?: 0
        var index: Int = key?.let { indexOf(it, hash) } ?: indexOfNull()

        if (index >= 0) {
            index = (index shl 1) + 1
            @Suppress("UNCHECKED_CAST")
            val old = array[index] as V?
            array[index] = value
            return old
        }

        index = index.inv()
        if (osize >= hashes.size) {
            val n = when {
                osize >= BASE_SIZE * 2 -> osize + (osize shr 1)
                osize >= BASE_SIZE -> BASE_SIZE * 2
                else -> BASE_SIZE
            }

            if (DEBUG) {
                println("$TAG put: grow from ${hashes.size} to $n")
            }
            hashes = hashes.copyOf(n)
            array = array.copyOf(n shl 1)

            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != size) {
                throw ConcurrentModificationException()
            }
        }

        if (index < osize) {
            if (DEBUG) {
                println("$TAG put: move $index-${osize - index} to ${index + 1}")
            }
            hashes.copyInto(hashes, index + 1, index, osize)
            array.copyInto(array, (index + 1) shl 1, index shl 1, size shl 1)
        }

        if (CONCURRENT_MODIFICATION_EXCEPTIONS && (osize != size || index >= hashes.size)) {
            throw ConcurrentModificationException()
        }

        hashes[index] = hash
        array[index shl 1] = key
        array[(index shl 1) + 1] = value
        size++
        return null
    }

    /**
     * Perform a [put] of all key/value pairs in [map]
     *
     * @param map The array whose contents are to be retrieved.
     */
    public open fun putAll(map: SimpleArrayMap<out K, out V>) {
        val n = map.size
        ensureCapacity(size + n)
        if (size == 0) {
            if (n > 0) {
                map.hashes.copyInto(
                    destination = hashes,
                    destinationOffset = 0,
                    startIndex = 0,
                    endIndex = n
                )
                map.array.copyInto(
                    destination = array,
                    destinationOffset = 0,
                    startIndex = 0,
                    endIndex = n shl 1
                )
                size = n
            }
        } else {
            for (i in 0 until n) {
                put(map.keyAt(i), map.valueAt(i))
            }
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
    public open fun putIfAbsent(key: K, value: V): V? {
        var mapValue = get(key)
        if (mapValue == null) {
            mapValue = put(key, value)
        }
        return mapValue
    }

    /**
     * Remove an existing key from the array map.
     *
     * @param key The key of the mapping to remove.
     * @return Returns the value that was stored under the key, or `null` if there was no such key.
     */
    public open fun remove(key: K): V? {
        val index = indexOfKey(key)
        return if (index >= 0) {
            removeAt(index)
        } else null
    }

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     *
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns `true` if the mapping was removed.
     */
    public open fun remove(key: K, value: V): Boolean {
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
     * Remove the key/value mapping at the given index.
     *
     * @param index The desired index, must be between 0 and [size]-1 (inclusive).
     * @return Returns the value that was stored at this index.
     * @throws ConcurrentModificationException if it was detected that this [SimpleArrayMap] was
     * written to while this operation was running.
     * @throws IllegalArgumentException if [index] is not between 0 and [size]-1
     */
    public open fun removeAt(index: Int): V {
        require(index in 0 until size) {
            "Expected index to be within 0..size()-1, but was $index"
        }

        val old = array[(index shl 1) + 1]
        val osize = size
        if (osize <= 1) {
            // Now empty.
            if (DEBUG) {
                println("$TAG remove: shrink from ${hashes.size} to 0")
            }
            clear()
        } else {
            val nsize = osize - 1
            if (hashes.size > (BASE_SIZE * 2) && osize < hashes.size / 3) {
                // Shrunk enough to reduce size of arrays. We don't allow it to
                // shrink smaller than (BASE_SIZE*2) to avoid flapping between
                // that and BASE_SIZE.
                val n = when {
                    osize > (BASE_SIZE * 2) -> osize + (osize shr 1)
                    else -> BASE_SIZE * 2
                }

                if (DEBUG) {
                    println("$TAG remove: shrink from ${hashes.size} to $n")
                }

                val ohashes = hashes
                val oarray: Array<Any?> = array
                hashes = hashes.copyOf(n)
                array = array.copyOf(n shl 1)

                if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != size) {
                    throw ConcurrentModificationException()
                }

                if (index > 0) {
                    if (DEBUG) {
                        println("$TAG remove: copy from 0-$index to 0")
                    }
                    ohashes.copyInto(
                        destination = hashes,
                        destinationOffset = 0,
                        startIndex = 0,
                        endIndex = index
                    )
                    oarray.copyInto(
                        destination = array,
                        destinationOffset = 0,
                        startIndex = 0,
                        endIndex = index shl 1
                    )
                }

                if (index < nsize) {
                    if (DEBUG) {
                        println("$TAG remove: copy from ${index + 1}-$nsize to $index")
                    }
                    ohashes.copyInto(
                        destination = hashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nsize + 1
                    )
                    oarray.copyInto(
                        destination = array,
                        destinationOffset = index shl 1,
                        startIndex = (index + 1) shl 1,
                        endIndex = (nsize + 1) shl 1
                    )
                }
            } else {
                if (index < nsize) {
                    if (DEBUG) {
                        println("$TAG remove: move ${index + 1}-$nsize to $index")
                    }

                    hashes.copyInto(
                        destination = hashes,
                        destinationOffset = index,
                        startIndex = index + 1,
                        endIndex = nsize + 1
                    )
                    array.copyInto(
                        destination = array,
                        destinationOffset = index shl 1,
                        startIndex = (index + 1) shl 1,
                        endIndex = (nsize + 1) shl 1
                    )
                }
                array[nsize shl 1] = null
                array[(nsize shl 1) + 1] = null
            }
            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != size) {
                throw ConcurrentModificationException()
            }
            size = nsize
        }

        @Suppress("UNCHECKED_CAST")
        return old as V
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or `null`.
     */
    public open fun replace(key: K, value: V): V? {
        val index = indexOfKey(key)
        return when {
            index >= 0 -> setValueAt(index, value)
            else -> null
        }
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to [oldValue].
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns `true` if the value was replaced.
     */
    public open fun replace(key: K, oldValue: V, newValue: V): Boolean {
        val index = indexOfKey(key)
        if (index >= 0) {
            val mapValue = valueAt(index)
            if (oldValue == mapValue) {
                setValueAt(index, newValue)
                return true
            }
        }
        return false
    }

    /**
     * Return the number of items in this array map.
     */
    public open fun size(): Int {
        return size
    }

    /**
     * This implementation returns `false` if the object is not a [Map] or
     * [SimpleArrayMap], or if the maps have different sizes. Otherwise, for each
     * key in this map, values of both maps are compared. If the values for any
     * key are not equal, the method returns false, otherwise it returns `true`.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        try {
            if (other is SimpleArrayMap<*, *>) {
                if (size() != other.size()) {
                    return false
                }

                @Suppress("UNCHECKED_CAST")
                val otherSimpleArrayMap = other as SimpleArrayMap<Any?, Any?>
                for (i in 0 until size) {
                    val key = keyAt(i)
                    val mine = valueAt(i)
                    // TODO use index-based ops for this
                    val theirs = otherSimpleArrayMap[key]
                    if (mine == null) {
                        if (theirs != null || !otherSimpleArrayMap.containsKey(key)) {
                            return false
                        }
                    } else if (mine != theirs) {
                        return false
                    }
                }
                return true
            } else if (other is Map<*, *>) {
                if (size() != other.size) {
                    return false
                }
                for (i in 0 until size) {
                    val key = keyAt(i)
                    val mine = valueAt(i)
                    val theirs = other[key]
                    if (mine == null) {
                        if (theirs != null || !other.containsKey(key)) {
                            return false
                        }
                    } else if (mine != theirs) {
                        return false
                    }
                }
                return true
            }
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }
        return false
    }

    override fun hashCode(): Int {
        val hashes = hashes
        val array = array
        var result = 0
        var i = 0
        var v = 1
        val s = size
        while (i < s) {
            val value = array[v]
            result += hashes[i] xor (value?.hashCode() ?: 0)
            i++
            v += 2
        }
        return result
    }

    /**
     * Returns a string representation of the object.
     *
     * This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a key or a value, the string "(this Map)"
     * will appear in its place.
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }

        return buildString(size * 28) {
            append('{')
            for (i in 0 until size) {
                if (i > 0) {
                    append(", ")
                }
                val key = keyAt(i)
                if (key !== this) {
                    append(key)
                } else {
                    append("(this Map)")
                }
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
}
