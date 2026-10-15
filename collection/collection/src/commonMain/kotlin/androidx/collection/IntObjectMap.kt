/*
 * Copyright 2023 The Android Open Source Project
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

// Facade class name cannot be updated, the Kt name has been released
@file:Suppress(
    "RedundantVisibilityModifier",
    "NOTHING_TO_INLINE",
    "FacadeClassJvmName",
)
@file:OptIn(ExperimentalContracts::class)

package androidx.collection

import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.requirePrecondition
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to the Kotlin source file.
//
// This file was generated from a template in the template directory.
// Make a change to the original template and run the generateCollections.sh script
// to ensure the change is available on all versions of the map.
//
// Note that there are 3 templates for maps, one for object-to-primitive, one
// for primitive-to-object and one for primitive-to-primitive. Also, the
// object-to-object is ScatterMap.kt, which doesn't have a template.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

// Default empty map to avoid allocations
private val EmptyIntObjectMap = MutableIntObjectMap<Nothing>(0)

/** Returns an empty, read-only [IntObjectMap]. */
@Suppress("UNCHECKED_CAST")
public fun <V> emptyIntObjectMap(): IntObjectMap<V> = EmptyIntObjectMap as IntObjectMap<V>

/** Returns an empty, read-only [IntObjectMap]. */
@Suppress("UNCHECKED_CAST")
public fun <V> intObjectMapOf(): IntObjectMap<V> = EmptyIntObjectMap as IntObjectMap<V>

/** Returns a new [IntObjectMap] with [key1] associated with [value1]. */
public fun <V> intObjectMapOf(
    key1: Int,
    value1: V,
): IntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
    }

/**
 * Returns a new [IntObjectMap] with [key1], and [key2] associated with [value1], and [value2],
 * respectively.
 */
public fun <V> intObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
): IntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
    }

/**
 * Returns a new [IntObjectMap] with [key1], [key2], and [key3] associated with [value1], [value2],
 * and [value3], respectively.
 */
public fun <V> intObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
): IntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
    }

/**
 * Returns a new [IntObjectMap] with [key1], [key2], [key3], and [key4] associated with [value1],
 * [value2], [value3], and [value4], respectively.
 */
public fun <V> intObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
    key4: Int,
    value4: V,
): IntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
        map[key4] = value4
    }

/**
 * Returns a new [IntObjectMap] with [key1], [key2], [key3], [key4], and [key5] associated with
 * [value1], [value2], [value3], [value4], and [value5], respectively.
 */
public fun <V> intObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
    key4: Int,
    value4: V,
    key5: Int,
    value5: V,
): IntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
        map[key4] = value4
        map[key5] = value5
    }

/** Returns a new [MutableIntObjectMap]. */
public fun <V> mutableIntObjectMapOf(): MutableIntObjectMap<V> = MutableIntObjectMap()

/** Returns a new [MutableIntObjectMap] with [key1] associated with [value1]. */
public fun <V> mutableIntObjectMapOf(
    key1: Int,
    value1: V,
): MutableIntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
    }

/**
 * Returns a new [MutableIntObjectMap] with [key1], and [key2] associated with [value1], and
 * [value2], respectively.
 */
public fun <V> mutableIntObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
): MutableIntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
    }

/**
 * Returns a new [MutableIntObjectMap] with [key1], [key2], and [key3] associated with [value1],
 * [value2], and [value3], respectively.
 */
public fun <V> mutableIntObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
): MutableIntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
    }

/**
 * Returns a new [MutableIntObjectMap] with [key1], [key2], [key3], and [key4] associated with
 * [value1], [value2], [value3], and [value4], respectively.
 */
public fun <V> mutableIntObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
    key4: Int,
    value4: V,
): MutableIntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
        map[key4] = value4
    }

/**
 * Returns a new [MutableIntObjectMap] with [key1], [key2], [key3], [key4], and [key5] associated
 * with [value1], [value2], [value3], [value4], and [value5], respectively.
 */
public fun <V> mutableIntObjectMapOf(
    key1: Int,
    value1: V,
    key2: Int,
    value2: V,
    key3: Int,
    value3: V,
    key4: Int,
    value4: V,
    key5: Int,
    value5: V,
): MutableIntObjectMap<V> =
    MutableIntObjectMap<V>().also { map ->
        map[key1] = value1
        map[key2] = value2
        map[key3] = value3
        map[key4] = value4
        map[key5] = value5
    }

/**
 * Builds a new [IntObjectMap] by populating a [MutableIntObjectMap] using the given
 * [builderAction].
 *
 * The instance passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside the function produces an unspecified behavior.
 *
 * @param builderAction Lambda in which the [MutableIntObjectMap] can be populated.
 */
public inline fun <V> buildIntObjectMap(
    builderAction: MutableIntObjectMap<V>.() -> Unit
): IntObjectMap<V> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableIntObjectMap<V>().apply(builderAction)
}

/**
 * Builds a new [IntObjectMap] by populating a [MutableIntObjectMap] using the given
 * [builderAction].
 *
 * The instance passed as a receiver to the [builderAction] is valid only inside that function.
 * Using it outside the function produces an unspecified behavior.
 *
 * @param initialCapacity Hint for the expected number of pairs added in the [builderAction].
 * @param builderAction Lambda in which the [MutableIntObjectMap] can be populated.
 */
public inline fun <V> buildIntObjectMap(
    initialCapacity: Int,
    builderAction: MutableIntObjectMap<V>.() -> Unit,
): IntObjectMap<V> {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableIntObjectMap<V>(initialCapacity).apply(builderAction)
}

/**
 * [IntObjectMap] is a container with a [Map]-like interface for keys with [Int] primitives and
 * reference type values.
 *
 * The underlying implementation is designed to avoid allocations from boxing, and insertion,
 * removal, retrieval, and iteration operations. Allocations may still happen on insertion when the
 * underlying storage needs to grow to accommodate newly added entries to the table. In addition,
 * this implementation minimizes memory usage by avoiding the use of separate objects to hold
 * key/value pairs.
 *
 * This implementation makes no guarantee as to the order of the keys and values stored, nor does it
 * make guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the map (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Multiple threads are safe to read from
 * this map concurrently if no write is happening.
 *
 * This implementation is read-only and only allows data to be queried. A mutable implementation is
 * provided by [MutableIntObjectMap].
 *
 * @see [MutableIntObjectMap]
 */
public sealed class IntObjectMap<V> {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` entries, including when
    // the table is empty (see [EmptyGroup]).
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup

    @PublishedApi @JvmField internal var keys: IntArray = EmptyIntArray

    @PublishedApi @JvmField internal var values: Array<Any?> = EMPTY_OBJECTS

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the capacity
    @Suppress("PropertyName") @JvmField internal var _capacity: Int = 0

    /**
     * Returns the number of key-value pairs that can be stored in this map without requiring
     * internal storage reallocation.
     */
    public val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @Suppress("PropertyName") @JvmField internal var _size: Int = 0

    /** Returns the number of key-value pairs in this map. */
    public val size: Int
        get() = _size

    /** Returns `true` if this map has at least one entry. */
    public fun any(): Boolean = _size != 0

    /** Returns `true` if this map has no entries. */
    public fun none(): Boolean = _size == 0

    /** Indicates whether this map is empty. */
    public fun isEmpty(): Boolean = _size == 0

    /** Returns `true` if this map is not empty. */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in
     * the map.
     */
    public operator fun get(key: Int): V? {
        val index = findKeyIndex(key)
        @Suppress("UNCHECKED_CAST")
        return if (index >= 0) values[index] as V? else null
    }

    /**
     * Returns the value to which the specified [key] is mapped, or [defaultValue] if this map
     * contains no mapping for the key.
     */
    public fun getOrDefault(key: Int, defaultValue: V): V {
        val index = findKeyIndex(key)
        if (index >= 0) {
            @Suppress("UNCHECKED_CAST")
            return values[index] as V
        }
        return defaultValue
    }

    /**
     * Returns the value for the given [key] if the value is present and not null. Otherwise,
     * returns the result of the [defaultValue] function.
     */
    public inline fun getOrElse(key: Int, defaultValue: () -> V): V {
        return get(key) ?: defaultValue()
    }

    /**
     * Iterates over every key/value pair stored in this map by invoking the specified [block]
     * lambda.
     */
    @PublishedApi
    internal inline fun forEachIndexed(block: (index: Int) -> Unit) {
        val m = metadata
        val lastIndex = m.size - 2 // We always have 0 or at least 2 entries

        var i = 0
        while (i <= lastIndex) {
            val slot = m[i]
            // In metadata, Empty (0x80), Deleted (0xFE), and Sentinel (0xFF) all have bit 7
            // set; Full slots have bit 7 unset (< 0x80). Inverting the word and masking
            // with BitmaskMsb sets bit (8j + 7) if slot j is Full. Sentinel (0xFF) at
            // capacity index is naturally skipped since its bit 7 is set.
            var mask = slot.inv() and BitmaskMsb
            while (mask != 0L) {
                val index = (i shl 3) + mask.lowestBitSet()
                block(index)
                mask = mask and (mask - 1L)
            }
            i++
        }
    }

    /**
     * Iterates over every key/value pair stored in this map by invoking the specified [block]
     * lambda.
     */
    public inline fun forEach(block: (key: Int, value: V) -> Unit) {
        val k = keys
        val v = values

        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST") block(k[index], v[index] as V)
        }
    }

    /** Iterates over every key stored in this map by invoking the specified [block] lambda. */
    public inline fun forEachKey(block: (key: Int) -> Unit) {
        val k = keys

        forEachIndexed { index ->
            block(k[index])
        }
    }

    /** Iterates over every value stored in this map by invoking the specified [block] lambda. */
    public inline fun forEachValue(block: (value: V) -> Unit) {
        val v = values

        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST") block(v[index] as V)
        }
    }

    /** Returns true if all entries match the given [predicate]. */
    public inline fun all(predicate: (Int, V) -> Boolean): Boolean {
        forEach { key, value ->
            if (!predicate(key, value)) return false
        }
        return true
    }

    /** Returns true if at least one entry matches the given [predicate]. */
    public inline fun any(predicate: (Int, V) -> Boolean): Boolean {
        forEach { key, value ->
            if (predicate(key, value)) return true
        }
        return false
    }

    /** Returns the number of entries in this map. */
    public fun count(): Int = size

    /** Returns the number of entries matching the given [predicate]. */
    public inline fun count(predicate: (Int, V) -> Boolean): Int {
        var count = 0
        forEach { key, value ->
            if (predicate(key, value)) count++
        }
        return count
    }

    /** Returns true if the specified [key] is present in this map, false otherwise. */
    public inline operator fun contains(key: Int): Boolean = containsKey(key)

    /** Returns true if the specified [key] is present in this map, false otherwise. */
    public fun containsKey(key: Int): Boolean = findKeyIndex(key) >= 0

    /** Returns true if the specified [value] is present in this map, false otherwise. */
    public fun containsValue(value: V): Boolean {
        forEachValue { v ->
            if (value == v) return true
        }
        return false
    }

    /**
     * Creates a String from the entries, separated by [separator] and using [prefix] before and
     * [postfix] after, if supplied.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used to
     * generate the string. If the collection holds more than [limit] items, the string is
     * terminated with [truncated].
     */
    @JvmOverloads
    public fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be "suffix", but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
    ): String = buildString {
        append(prefix)
        run {
            var index = 0
            this@IntObjectMap.forEach { key, value ->
                if (index != 0) {
                    append(separator)
                }
                if (index == limit) {
                    append(truncated)
                    return@run
                }
                append(key)
                append('=')
                append(value)
                index++
            }
        }
        append(postfix)
    }

    /**
     * Creates a String from the entries, separated by [separator] and using [prefix] before and
     * [postfix] after, if supplied. Each entry is created with [transform].
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used to
     * generate the string. If the collection holds more than [limit] items, the string is
     * terminated with [truncated].
     */
    @JvmOverloads
    public inline fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be "suffix", but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        crossinline transform: (key: Int, value: V) -> CharSequence,
    ): String = buildString {
        append(prefix)
        run {
            var index = 0
            this@IntObjectMap.forEach { key, value ->
                if (index != 0) {
                    append(separator)
                }
                if (index == limit) {
                    append(truncated)
                    return@run
                }
                append(transform(key, value))
                index++
            }
        }
        append(postfix)
    }

    /**
     * Returns the hash code value for this map. The hash code the sum of the hash codes of each
     * key/value pair.
     */
    public override fun hashCode(): Int {
        var hash = 0

        forEach { key, value ->
            hash += key.hashCode() xor value.hashCode()
        }

        return hash
    }

    /**
     * Compares the specified object [other] with this hash map for equality. The two objects are
     * considered equal if [other]:
     * - Is a [IntObjectMap]
     * - Has the same [size] as this map
     * - Contains key/value pairs equal to this map's pair
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is IntObjectMap<*>) {
            return false
        }
        if (other.size != size) {
            return false
        }

        forEach { key, value ->
            if (value == null) {
                if (other[key] != null || !other.containsKey(key)) {
                    return false
                }
            } else if (value != other[key]) {
                return false
            }
        }

        return true
    }

    /**
     * Returns a string representation of this map. The map is denoted in the string by the `{}`.
     * Each key/value pair present in the map is represented inside '{}' by a substring of the form
     * `key=value`, and pairs are separated by `, `.
     */
    public override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }

        val s = StringBuilder().append('{')
        var i = 0
        forEach { key, value ->
            s.append(key)
            s.append('=')
            s.append(if (value === this) "(this)" else value)
            i++
            if (i < _size) {
                s.append(',').append(' ')
            }
        }

        return s.append('}').toString()
    }

    /**
     * Scans the hash table to find the index in the backing arrays of the specified [key]. Returns
     * -1 if the key is not present.
     */
    internal inline fun findKeyIndex(key: Int): Int {
        val hash = hash(key)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = h1(hash) and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var match = g.match(hash2)

            while (match.hasNext()) {
                val index = (probeOffset + match.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                match = match.next()
            }

            // An Empty slot indicates the key cannot be present further along the probe chain.
            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        return -1
    }
}

/**
 * [MutableIntObjectMap] is a container with a [MutableMap]-like interface for keys with [Int]
 * primitives and reference type values.
 *
 * The underlying implementation is designed to avoid allocations from boxing, and insertion,
 * removal, retrieval, and iteration operations. Allocations may still happen on insertion when the
 * underlying storage needs to grow to accommodate newly added entries to the table. In addition,
 * this implementation minimizes memory usage by avoiding the use of separate objects to hold
 * key/value pairs.
 *
 * This implementation makes no guarantee as to the order of the keys and values stored, nor does it
 * make guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the map (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Multiple threads are safe to read from
 * this map concurrently if no write is happening.
 *
 * @param initialCapacity The initial desired capacity for this container. the container will honor
 *   this value by guaranteeing its internal structures can hold that many entries without requiring
 *   any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableIntObjectMap]
 * @see ScatterMap
 */
public class MutableIntObjectMap<V>(initialCapacity: Int = DefaultScatterCapacity) :
    IntObjectMap<V>() {
    // Number of entries we can add before we need to grow
    private var growthLimit = 0

    init {
        requirePrecondition(initialCapacity >= 0) { "Capacity must be a positive value." }
        initializeStorage(unloadedCapacity(initialCapacity))
    }

    private fun initializeStorage(initialCapacity: Int) {
        val newCapacity =
            if (initialCapacity > 0) {
                // Since we use longs for storage, our capacity is never < 7, enforce
                // it here. We do have a special case for 0 to create small empty maps
                maxOf(7, normalizeCapacity(initialCapacity))
            } else {
                0
            }
        _capacity = newCapacity
        initializeMetadata(newCapacity)
        keys = IntArray(newCapacity)
        values = arrayOfNulls(newCapacity)
    }

    private fun initializeMetadata(capacity: Int) {
        metadata =
            if (capacity == 0) {
                EmptyGroup
            } else {
                // Round up to the next multiple of 8 and find how many longs we need
                val size = (((capacity + 1 + ClonedMetadataCount) + 7) and 0x7.inv()) shr 3
                LongArray(size).apply {
                    fill(AllEmpty)
                }
            }
        writeRawMetadata(metadata, capacity, Sentinel)
        initializeGrowth()
    }

    private fun initializeGrowth() {
        growthLimit = loadedCapacity(capacity) - _size
    }

    /**
     * Returns the value to which the specified [key] is mapped, if the value is present in the map
     * and not `null`. Otherwise, calls `defaultValue()` and puts the result in the map associated
     * with [key].
     */
    public inline fun getOrPut(key: Int, defaultValue: () -> V): V {
        val index = findInsertIndex(key)
        if (index >= 0) {
            @Suppress("UNCHECKED_CAST") val oldValue = values[index] as V?
            if (oldValue != null) {
                return oldValue
            }
            val value = defaultValue()
            keys[index] = key
            values[index] = value
            return value
        } else {
            val value = defaultValue()
            val insertionIndex = index.inv()
            keys[insertionIndex] = key
            values[insertionIndex] = value
            return value
        }
    }

    /**
     * Creates a new mapping from [key] to [value] in this map. If [key] is already present in the
     * map, the association is modified and the previously associated value is replaced with
     * [value]. If [key] is not present, a new entry is added to the map, which may require to grow
     * the underlying storage and cause allocations.
     */
    public operator fun set(key: Int, value: V) {
        val index = findInsertIndex(key)
        val i = if (index < 0) index.inv() else index
        keys[i] = key
        values[i] = value
    }

    /**
     * Creates a new mapping from [key] to [value] in this map. If [key] is already present in the
     * map, the association is modified and the previously associated value is replaced with
     * [value]. If [key] is not present, a new entry is added to the map, which may require to grow
     * the underlying storage and cause allocations. Return the previous value associated with the
     * [key], or `null` if the key was not present in the map.
     */
    public fun put(key: Int, value: V): V? {
        val index = findInsertIndex(key)
        val i = if (index < 0) index.inv() else index
        val oldValue = values[i]
        keys[i] = key
        values[i] = value

        @Suppress("UNCHECKED_CAST")
        return oldValue as V?
    }

    /** Puts all the key/value mappings in the [from] map into this map. */
    public fun putAll(from: IntObjectMap<V>) {
        val newCapacity = normalizeCapacity(unloadedCapacity(_size))
        if (newCapacity < _capacity) resizeStorage(newCapacity)
        from.forEach { key, value ->
            this[key] = value
        }
    }

    /** Puts all the key/value mappings in the [from] map into this map. */
    public inline operator fun plusAssign(from: IntObjectMap<V>): Unit = putAll(from)

    /**
     * Removes the specified [key] and its associated value from the map. If the [key] was present
     * in the map, this function returns the value that was present before removal.
     */
    public fun remove(key: Int): V? {
        val index = findKeyIndex(key)
        if (index >= 0) {
            return removeValueAt(index)
        }
        return null
    }

    /**
     * Removes the specified [key] and its associated value from the map if the associated value
     * equals [value]. Returns whether the removal happened.
     */
    public fun remove(key: Int, value: V): Boolean {
        val index = findKeyIndex(key)
        if (index >= 0) {
            if (values[index] == value) {
                removeValueAt(index)
                return true
            }
        }
        return false
    }

    /** Removes any mapping for which the specified [predicate] returns true. */
    public inline fun removeIf(predicate: (Int, V) -> Boolean) {
        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(keys[index], values[index] as V)) {
                removeValueAt(index)
            }
        }
    }

    /** Removes the specified [key] and its associated value from the map. */
    public inline operator fun minusAssign(key: Int) {
        remove(key)
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(@Suppress("ArrayReturn") keys: IntArray) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: IntSet) {
        keys.forEach { key ->
            minusAssign(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: IntList) {
        keys.forEach { key ->
            minusAssign(key)
        }
    }

    @PublishedApi
    internal fun removeValueAt(index: Int): V? {
        _size -= 1

        // If the group of size GroupWidth around the entry we are deleting is already empty,
        // we can just mark the entry as empty instead of marking it Deleted. This will avoid
        // unnecessary and expensive cleanup (dropDeletes()) and resize phases
        val m = metadata
        val capacity = _capacity
        val groupEmptyAfter = group(m, index).maskEmpty()
        val groupEmptyBefore = group(m, (index - GroupWidth) and capacity).maskEmpty()
        val trigger =
            (groupEmptyAfter != 0L) and
                (groupEmptyBefore != 0L) and
                (((groupEmptyAfter.countTrailingZeroBits() +
                    groupEmptyBefore.countLeadingZeroBits()) ushr 3) < GroupWidth)
        writeMetadata(m, capacity, index, if (trigger) Empty else Deleted)
        growthLimit += if (trigger) 1 else 0

        val oldValue = values[index]
        values[index] = null

        @Suppress("UNCHECKED_CAST")
        return oldValue as V?
    }

    /** Removes all mappings from this map. */
    public fun clear() {
        _size = 0
        // When the map was never allocated (or capacity is 0), avoid redundant array fill calls
        if (_capacity == 0) {
            growthLimit = 0
            return
        }
        if (metadata !== EmptyGroup) {
            metadata.fill(AllEmpty)
            writeRawMetadata(metadata, _capacity, Sentinel)
        }
        values.fill(null, 0, _capacity)
        initializeGrowth()
    }

    /**
     * Scans the hash table to find the index at which we can store a value for the give [key]. If
     * the key already exists in the table, its index will be returned, otherwise the `index.inv()`
     * of an empty slot will be returned. Calling this function may cause the internal storage to be
     * reallocated if the table is full.
     *
     * This method executes a single-pass probe: as candidate slots matching H2 are checked for
     * [key], the first available (Empty or Deleted) slot encountered along the probe sequence is
     * tracked in `firstAvail`. Probing terminates as soon as a group containing an Empty slot is
     * encountered (as guaranteed by quadratic probing invariants), avoiding a second probe pass to
     * find a slot.
     *
     * Only the storage-adjusting path, [insertSlowPath], is kept out of line.
     */
    @PublishedApi
    internal fun findInsertIndex(key: Int): Int {
        val hash = hash(key)
        val hash1 = h1(hash)
        val hash2 = h2(hash).toLong()

        // There is no `probeMask == 0` early return: an unallocated table uses [EmptyGroup]
        // (Sentinel, Empty x7) as metadata, so the probe below terminates in its first group and
        // the insertion sees growthLimit == 0, routing to [insertSlowPath]. No hash value is live
        // across the probe loop: insertion takes H2 from the low byte of h2Pattern, and only the
        // cold [insertSlowPath] recomputes the hash, which avoids register spills around equals().
        val probeMask = _capacity

        val h2Pattern = BitmaskLsb * hash2
        var probeOffset = hash1 and probeMask
        var probeIndex = 0
        var firstAvail = -1

        while (true) {
            val g = group(metadata, probeOffset)
            var match = g.matchWithPattern(h2Pattern)
            while (match.hasNext()) {
                val index = (probeOffset + match.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                match = match.next()
            }

            // Test for empty or deleted slots in the current group
            val emptyOrDeleted = g.maskEmptyOrDeleted()
            if (emptyOrDeleted != 0L) {
                if (firstAvail == -1) {
                    firstAvail = (probeOffset + emptyOrDeleted.lowestBitSet()) and probeMask
                }
                if (g.maskEmpty() != 0L) {
                    break
                }
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        val growthLimit = growthLimit
        // `firstAvail` is guaranteed to be `Empty` or `Deleted` when `_capacity > 0`, and wraps to
        // slot 0 (`Sentinel`) when `_capacity == 0` (`EmptyGroup`, where `growthLimit == 0`).
        // In both cases `!isDeleted` is equivalent to `isEmpty` (or unallocated), allowing a single
        // metadata read to serve both the `growthLimit == 0` check and the decrement below.
        val isDeleted = isDeleted(metadata, firstAvail)
        if (growthLimit == 0 && !isDeleted) {
            return insertSlowPath(key).inv()
        }
        // H2 is the low byte of h2Pattern (BitmaskLsb * h2, with h2 <= 0x7F)
        writeMetadata(metadata, _capacity, firstAvail, h2Pattern and 0x7FL)
        _size++
        this.growthLimit = growthLimit - if (isDeleted) 0 else 1
        return firstAvail.inv()
    }

    /**
     * Cold reallocation helper for [findInsertIndex]: resizes or compacts storage via
     * [adjustStorage], finds a newly available slot, and claims it with the H2 bits of [key]'s
     * hash.
     */
    private fun insertSlowPath(key: Int): Int {
        // Recomputing the hash here (only when storage must be adjusted) keeps it out of the
        // probe loop's live set in the caller.
        val hash = hash(key)
        adjustStorage()
        val m = metadata
        val capacity = _capacity
        val index = findFirstAvailableSlot(m, capacity, h1(hash))
        // After `adjustStorage()`, `capacity > 0`, so `index` is guaranteed to be `Empty` (0x80) or
        // `Deleted` (0xFE) (never `Full` or `Sentinel`), making `!isDeleted` equivalent to
        // `isEmpty`.
        val notDeleted = if (isDeleted(m, index)) 0 else 1
        writeMetadata(m, capacity, index, h2(hash).toLong())
        _size++
        growthLimit -= notDeleted
        return index
    }

    /**
     * Trims this [MutableIntObjectMap]'s storage so it is sized appropriately to hold the current
     * mappings.
     *
     * Returns the number of empty entries removed from this map's storage. Returns be 0 if no
     * trimming is necessary or possible.
     */
    public fun trim(): Int {
        val previousCapacity = _capacity
        val newCapacity = normalizeCapacity(unloadedCapacity(_size))
        if (newCapacity < previousCapacity) {
            resizeStorage(newCapacity)
            return previousCapacity - _capacity
        }
        return 0
    }

    /**
     * Grow internal storage if necessary. This function can instead opt to remove deleted entries
     * from the table to avoid an expensive reallocation of the underlying storage. This "rehash in
     * place" occurs when the current size is <= 25/32 of the table capacity. The choice of 25/32 is
     * detailed in the implementation of abseil's `raw_hash_set`.
     */
    internal fun adjustStorage() { // Internal to prevent inlining
        if (_capacity > GroupWidth && _size.toLong() * 32L <= _capacity.toLong() * 25L) {
            // Fast-path when size is 0: directly reset metadata to AllEmpty and restore Sentinel
            // without slot-by-slot rehashing.
            if (_size == 0) {
                metadata.fill(AllEmpty)
                writeRawMetadata(metadata, _capacity, Sentinel)
                initializeGrowth()
            } else {
                dropDeletes()
            }
        } else {
            resizeStorage(nextCapacity(_capacity))
        }
    }

    // Internal to prevent inlining
    internal fun dropDeletes() {
        val metadata = metadata
        val capacity = _capacity
        val keys = keys
        val values = values

        // Converts Sentinel and Deleted to Empty, and Full to Deleted
        convertMetadataForCleanup(metadata, capacity)

        // Track remaining live elements so iteration terminates early as soon as all active
        // entries are placed, skipping remaining empty and deleted slots.
        var remaining = _size
        var index = 0

        // Drop deleted items and re-hashes surviving entries
        while (index != capacity) {
            var m = readRawMetadata(metadata, index)
            // Formerly Deleted entry, we can use it as a swap spot
            if (m == Empty) {
                index++
                continue
            }

            // Formerly Full entries are now marked Deleted. If we see an
            // entry that's not marked Deleted, we can ignore it completely
            if (m != Deleted) {
                index++
                continue
            }

            val hash = hash(keys[index])
            val hash1 = h1(hash)
            val targetIndex = findFirstAvailableSlot(metadata, capacity, hash1)

            // Test if the current index (i) and the new index (targetIndex) fall
            // within the same group based on the hash. If the group doesn't change,
            // we don't move the entry
            val probeOffset = hash1 and capacity
            val newProbeIndex = ((targetIndex - probeOffset) and capacity) / GroupWidth
            val oldProbeIndex = ((index - probeOffset) and capacity) / GroupWidth

            if (newProbeIndex == oldProbeIndex) {
                val hash2 = h2(hash)
                writeRawMetadata(metadata, index, hash2.toLong())

                // Copies the metadata into the clone area
                metadata[metadata.lastIndex] = metadata[0]

                remaining--
                if (remaining == 0) break
                index++
                continue
            }

            m = readRawMetadata(metadata, targetIndex)
            if (m == Empty) {
                // The target is empty so we can transfer directly
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())
                writeRawMetadata(metadata, index, Empty)

                keys[targetIndex] = keys[index]
                keys[index] = 0

                values[targetIndex] = values[index]
                values[index] = null
            } else /* m == Deleted */ {
                // The target isn't empty so we use an empty slot denoted by
                // swapIndex to perform the swap
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())

                val oldKey = keys[targetIndex]
                keys[targetIndex] = keys[index]
                keys[index] = oldKey

                val oldValue = values[targetIndex]
                values[targetIndex] = values[index]
                values[index] = oldValue

                // Since we exchanged two slots we must repeat the process with
                // element we just moved in the current location
                index--
            }

            // Copies the metadata into the clone area
            metadata[metadata.lastIndex] = metadata[0]

            remaining--
            if (remaining == 0) break
            index++
        }

        initializeGrowth()
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousKeys = keys
        val previousValues = values

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newKeys = keys
        val newValues = values
        val capacity = _capacity

        // Scans previous metadata in 8-slot groups using bitmasks (`slot.inv() and BitmaskMsb`)
        // rather than probing slot-by-slot. Empty groups produce 0L and are skipped in one check.
        val lastIndex = previousMetadata.size - 2 // We always have 0 or at least 2 entries
        var i = 0
        while (i <= lastIndex) {
            val slot = previousMetadata[i]
            var mask = slot.inv() and BitmaskMsb
            while (mask != 0L) {
                val previousIndex = (i shl 3) + mask.lowestBitSet()
                val previousKey = previousKeys[previousIndex]
                val hash = hash(previousKey)
                val index = findFirstAvailableSlot(newMetadata, capacity, h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newKeys[index] = previousKey
                newValues[index] = previousValues[previousIndex]

                mask = mask and (mask - 1L)
            }
            i++
        }
    }
}
