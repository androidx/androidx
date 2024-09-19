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

@file:Suppress(
    "RedundantVisibilityModifier",
    "KotlinRedundantDiagnosticSuppress",
    "KotlinConstantConditions",
    "PropertyName",
    "ConstPropertyName",
    "PrivatePropertyName",
    "NOTHING_TO_INLINE"
)

package androidx.collection

import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.requirePrecondition
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.max

// A "flat" hash map based on abseil's flat_hash_map
// (see https://abseil.io/docs/cpp/guides/container). Unlike its C++
// equivalent, this hash map doesn't (and cannot) store the keys and values
// directly inside a table. Instead the references and keys are stored in
// 2 separate tables. The implementation could be made "flatter" by storing
// both keys and values in the same array but this yields no improvement.
//
// The main design goal of this container is to provide a generic, cache-
// friendly, *allocation free* hash map, with performance on par with
// LinkedHashMap to act as a suitable replacement for the common
// mutableMapOf() in Kotlin.
//
// The implementation is very similar, and is based, as the name suggests,
// on a flat table of values. To understand the implementation, let's first
// define the terminology used throughout this file:
//
// - Slot
//       An entry in the backing table; in practice a slot is a pair of
//       (key, value) stored in two separate allocations.
// - Metadata
//       Indicates the state of a slot (available, etc. see below) but
//       can also store part of the slot's hash.
// - Group
//       Metadata for multiple slots that can be manipulated as a unit to
//       speed up processing.
//
// To quickly and efficiently find any given slot, the implementation uses
// groups to compare up to 8 entries at a time. To achieve this, we use
// open-addressing probing quadratic probing
// (https://en.wikipedia.org/wiki/Quadratic_probing). See "A note on
// probing" down below for more information.
//
// The table's memory layout is organized around 3 arrays:
//
// - metadata
//       An array of metadata bytes, encoded as a LongArray (see below).
//       The size of this array depends on capacity, but is smaller since
//       the array encodes 8 metadata per Long. There is also padding at
//       the end to permit branchless probing.
// - keys
//       Holds references to the key stored in the map. An index i in
//       this array maps to the corresponding values in the values array.
//       This array always has the same size as the capacity of the map.
// - values
//       Holds references to the key stored in the map. An index i in
//       this array maps to the corresponding values in the keys array
//       This array always has the same size as the capacity of the map.
//
// A key's hash code is separated into two distinct hashes:
//
// - H1: the hash code's 25 most significant bits
// - H2: the hash code's 7 least significant bits
//
// H1 is used as an index into the slots, and a starting point for a probe
// whenever we need to seek an entry in the table. H2 is used to quickly
// filter out slots when looking for a specific key in the table.
//
// While H1 is used to initiate a probing sequence, it is never stored in
// the table. H2 is however stored in the metadata of a slot. The metadata
// for any given slot is a single byte, which can have one of four states:
//
// - Empty: unused slot
// - Deleted: previously used slot
// - Full: used slot
// - Sentinel: marker to avoid branching, used to stop iterations
//
// They have the following bit patterns:
//
//      Empty: 1 0 0 0 0 0 0 0
//    Deleted: 1 1 1 1 1 1 1 0
//       Full: 0 h h h h h h h  // h represents the lower 7 hash bits
//   Sentinel: 1 1 1 1 1 1 1 1
//
// Insertions, reads, removals, and replacements all need to perform the
// same basic operation: finding a specific slot in the table. This `find`
// operation works like this:
//
// - Compute H1 from the key's hash code
// - Initialize a probe sequence from H1, which will potentially visit
//   every group in the map (but usually stops at the first one)
// - For each probe offset, select an entire group (8 entries) and find
//   candidate slots in that group. This means finding slots with a
//   matching H2 hash. We then iterate over the matching slots and compare
//   the slot's key to the find's key. If we have a final match, we know
//   the index of the key/value pair in the table. If there is no match
//   and the entire group is empty, the key does not exist in the table.
//
// Matching a Group with H2 ensures that one of the matching slots is
// likely to hold the same key as the one we are looking for. It also lets
// us quickly skip entire chunks of the map (for instance during iteration
// if a Group contains only empty slots, we can ignore it entirely).
//
// Since the metadata of a slot is made of a single byte, we could use
// a ByteArray instead of a LongArray. However, using a LongArray makes
// constructing a group cheaper and guarantees aligned reads. As a result
// we use a form of virtual addressing: when looking for a group starting
// at index 3 for instance, we do not fetch the 4th entry in the array of
// metadata, but instead find the Long that holds the 4th byte and create
// a Group of 8 bytes starting from that byte. The details are explained
// below in the group() function.
//
// ** A note on probing **
//
// A probe is a virtual construct used to iterate over the groups in the
// hash table in some interesting order. To probe the tables, we must
// initiate probing using the hash at which we want to start, using a
// suitable mask, in our case the table's capacity.
//
// The sequence is a triangular progression of the form:
//
// `p(i) = GroupWidth * (i ^ 2 + i) / 2 + hash (mod mask + 1)`
//
// The first few entries in the metadata table are mirrored at the end of
// the table so when we inspect those candidates we must make sure to not
// use their offset directly but instead the "wrap around" values, hence
// the `mask + 1` modulo.
//
// This probe sequence visits every group exactly once if the number of
// groups is a power of two, since `(i ^ 2 + i) / 2` is a bijection in
// `Z / (2 ^ m)`. See https://en.wikipedia.org/wiki/Quadratic_probing
//
// Reference:
//    Designing a Fast, Efficient, Cache-friendly Hash Table, Step by Step
//    2017, Matt Kulukundis, https://www.youtube.com/watch?v=ncHmEUmJZf4

// Indicates that all the slot in a [Group] are empty
// 0x8080808080808080UL, see explanation in [BitmaskMsb]
internal const val AllEmpty = -0x7f7f7f7f_7f7f7f80L

internal const val Empty = 0b10000000L
internal const val Deleted = 0b11111110L

// Used to mark the end of the actual storage, used to end iterations
@PublishedApi internal const val Sentinel: Long = 0b11111111L

// The number of entries depends on [GroupWidth]. Since our group width
// is fixed to 8 currently, we add 7 entries after the sentinel. To
// satisfy the case of a 0 capacity map, we also add another entry full
// of sentinels. Since our lookups always fetch 2 longs from the array,
// we make sure we have enough
@JvmField
internal val EmptyGroup =
    longArrayOf(
        // NOTE: the first byte in the array's logical order is in the LSB
        -0x7f7f7f7f_7f7f7f01L, // Sentinel, Empty, Empty... or 0xFF80808080808080UL
        -1L // 0xFFFFFFFFFFFFFFFFUL
    )

// Width of a group, in bytes. Since we can only use types as large as
// Long we must fit our metadata bytes in a 64-bit word or smaller, which
// means we can only store up to 8 slots in a group. Ideally we could use
// 128-bit data types to benefit from NEON/SSE instructions and manipulate
// groups of 16 slots at a time.
internal const val GroupWidth = 8

// A group is made of 8 metadata, or 64 bits
internal typealias Group = Long

// Number of metadata present both at the beginning and at the end of
// the metadata array so we can use a [GroupWidth] probing window from
// any index in the table.
internal const val ClonedMetadataCount = GroupWidth - 1

// Capacity to use as the first bump when capacity is initially 0
// We choose 6 so that the "unloaded" capacity maps to 7
internal const val DefaultScatterCapacity = 6

// Default empty map to avoid allocations
private val EmptyScatterMap = MutableScatterMap<Any?, Nothing>(0)

/** Returns an empty, read-only [ScatterMap]. */
@Suppress("UNCHECKED_CAST")
public fun <K, V> emptyScatterMap(): ScatterMap<K, V> = EmptyScatterMap as ScatterMap<K, V>

/** Returns a new [MutableScatterMap]. */
public fun <K, V> mutableScatterMapOf(): MutableScatterMap<K, V> = MutableScatterMap()

/**
 * Returns a new [MutableScatterMap] with the specified contents, given as a list of pairs where the
 * first component is the key and the second is the value. If multiple pairs have the same key, the
 * resulting map will contain the value from the last of those pairs.
 */
public fun <K, V> mutableScatterMapOf(vararg pairs: Pair<K, V>): MutableScatterMap<K, V> =
    MutableScatterMap<K, V>(pairs.size).apply { putAll(pairs) }

/**
 * [ScatterMap] is a container with a [Map]-like interface based on a flat hash table implementation
 * (the key/value mappings are not stored by nodes but directly into arrays). The underlying
 * implementation is designed to avoid all allocations on insertion, removal, retrieval, and
 * iteration. Allocations may still happen on insertion when the underlying storage needs to grow to
 * accommodate newly added entries to the table. In addition, this implementation minimizes memory
 * usage by avoiding the use of separate objects to hold key/value pairs.
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
 * provided by [MutableScatterMap].
 *
 * **Note**: when a [Map] is absolutely necessary, you can use the method [asMap] to create a thin
 * wrapper around a [ScatterMap]. Please refer to [asMap] for more details and caveats.
 *
 * **ScatterMap and SimpleArrayMap**: like [SimpleArrayMap], [ScatterMap]/[MutableScatterMap] is
 * designed to avoid the allocation of extra objects when inserting new entries in the map. However,
 * the implementation of [ScatterMap]/[MutableScatterMap] offers better performance characteristics
 * compared to [SimpleArrayMap] and is thus generally preferable. If memory usage is a concern,
 * [SimpleArrayMap] automatically shrinks its storage to avoid using more memory than necessary. You
 * can also control memory usage with [MutableScatterMap] by manually calling
 * [MutableScatterMap.trim].
 *
 * @see [MutableScatterMap]
 */
public sealed class ScatterMap<K, V> {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` entries, including when
    // the table is empty (see [EmptyGroup]).
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup

    @PublishedApi @JvmField internal var keys: Array<Any?> = EMPTY_OBJECTS

    @PublishedApi @JvmField internal var values: Array<Any?> = EMPTY_OBJECTS

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the capacity
    @JvmField internal var _capacity: Int = 0

    /**
     * Returns the number of key-value pairs that can be stored in this map without requiring
     * internal storage reallocation.
     */
    public val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @JvmField internal var _size: Int = 0

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
    public operator fun get(key: K): V? {
        val index = findKeyIndex(key)
        @Suppress("UNCHECKED_CAST") return if (index >= 0) values[index] as V? else null
    }

    /**
     * Returns the value to which the specified [key] is mapped, or [defaultValue] if this map
     * contains no mapping for the key.
     */
    public fun getOrDefault(key: K, defaultValue: V): V {
        val index = findKeyIndex(key)
        if (index >= 0) {
            @Suppress("UNCHECKED_CAST") return values[index] as V
        }
        return defaultValue
    }

    /**
     * Returns the value for the given [key] if the value is present and not null. Otherwise,
     * returns the result of the [defaultValue] function.
     */
    public inline fun getOrElse(key: K, defaultValue: () -> V): V {
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

        for (i in 0..lastIndex) {
            var slot = m[i]
            if (slot.maskEmptyOrDeleted() != BitmaskMsb) {
                // Branch-less if (i == lastIndex) 7 else 8
                // i - lastIndex returns a negative value when i < lastIndex,
                // so 1 is set as the MSB. By inverting and shifting we get
                // 0 when i < lastIndex, 1 otherwise.
                val bitCount = 8 - ((i - lastIndex).inv() ushr 31)
                for (j in 0 until bitCount) {
                    if (isFull(slot and 0xffL)) {
                        val index = (i shl 3) + j
                        block(index)
                    }
                    slot = slot shr 8
                }
                if (bitCount != 8) return
            }
        }
    }

    /**
     * Iterates over every key/value pair stored in this map by invoking the specified [block]
     * lambda.
     */
    public inline fun forEach(block: (key: K, value: V) -> Unit) {
        val k = keys
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K, v[index] as V) }
    }

    /** Iterates over every key stored in this map by invoking the specified [block] lambda. */
    public inline fun forEachKey(block: (key: K) -> Unit) {
        val k = keys

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K) }
    }

    /** Iterates over every value stored in this map by invoking the specified [block] lambda. */
    public inline fun forEachValue(block: (value: V) -> Unit) {
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(v[index] as V) }
    }

    /** Returns true if all entries match the given [predicate]. */
    public inline fun all(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (!predicate(key, value)) return false }
        return true
    }

    /** Returns true if at least one entry matches the given [predicate]. */
    public inline fun any(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (predicate(key, value)) return true }
        return false
    }

    /** Returns the number of entries in this map. */
    public fun count(): Int = size

    /** Returns the number of entries matching the given [predicate]. */
    public inline fun count(predicate: (K, V) -> Boolean): Int {
        var count = 0
        forEach { key, value -> if (predicate(key, value)) count++ }
        return count
    }

    /** Returns true if the specified [key] is present in this hash map, false otherwise. */
    public operator fun contains(key: K): Boolean = findKeyIndex(key) >= 0

    /** Returns true if the specified [key] is present in this hash map, false otherwise. */
    public fun containsKey(key: K): Boolean = findKeyIndex(key) >= 0

    /** Returns true if the specified [value] is present in this hash map, false otherwise. */
    public fun containsValue(value: V): Boolean {
        forEachValue { v -> if (value == v) return true }
        return false
    }

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before and
     * [postfix] after, if supplied.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used to
     * generate the string. If the collection holds more than [limit] items, the string is
     * terminated with [truncated].
     *
     * [transform] may be supplied to convert each element to a custom String.
     */
    @JvmOverloads
    public fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        transform: ((key: K, value: V) -> CharSequence)? = null
    ): String = buildString {
        append(prefix)
        var index = 0
        this@ScatterMap.forEach { key, value ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            if (transform == null) {
                append(key)
                append('=')
                append(value)
            } else {
                append(transform(key, value))
            }
            index++
        }
        append(postfix)
    }

    /**
     * Returns the hash code value for this map. The hash code the sum of the hash codes of each
     * key/value pair.
     */
    public override fun hashCode(): Int {
        var hash = 0

        forEach { key, value -> hash += key.hashCode() xor value.hashCode() }

        return hash
    }

    /**
     * Compares the specified object [other] with this hash map for equality. The two objects are
     * considered equal if [other]:
     * - Is a [ScatterMap]
     * - Has the same [size] as this map
     * - Contains key/value pairs equal to this map's pair
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ScatterMap<*, *>) {
            return false
        }
        if (other.size != size) {
            return false
        }

        @Suppress("UNCHECKED_CAST") val o = other as ScatterMap<Any?, Any?>

        forEach { key, value ->
            if (value == null) {
                if (o[key] != null || !o.containsKey(key)) {
                    return false
                }
            } else if (value != o[key]) {
                return false
            }
        }

        return true
    }

    /**
     * Returns a string representation of this map. The map is denoted in the string by the `{}`.
     * Each key/value pair present in the map is represented inside '{}` by a substring of the form
     * `key=value`, and pairs are separated by `, `.
     */
    public override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }

        val s = StringBuilder().append('{')
        var i = 0
        forEach { key, value ->
            s.append(if (key === this) "(this)" else key)
            s.append("=")
            s.append(if (value === this) "(this)" else value)
            i++
            if (i < _size) {
                s.append(',').append(' ')
            }
        }

        return s.append('}').toString()
    }

    internal fun asDebugString(): String = buildString {
        append('{')
        append("metadata=[")
        for (i in 0 until capacity) {
            when (val metadata = readRawMetadata(metadata, i)) {
                Empty -> append("Empty")
                Deleted -> append("Deleted")
                else -> append(metadata)
            }
            append(", ")
        }
        append("], ")
        append("keys=[")
        for (i in keys.indices) {
            append(keys[i])
            append(", ")
        }
        append("], ")
        append("values=[")
        for (i in values.indices) {
            append(values[i])
            append(", ")
        }
        append("]")
        append('}')
    }

    /**
     * Scans the hash table to find the index in the backing arrays of the specified [key]. Returns
     * -1 if the key is not present.
     */
    internal inline fun findKeyIndex(key: K): Int {
        val hash = hash(key)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = h1(hash) and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                m = m.next()
            }

            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        return -1
    }

    /**
     * Wraps this [ScatterMap] with a [Map] interface. The [Map] is backed by the [ScatterMap], so
     * changes to the [ScatterMap] are reflected in the [Map]. If the [ScatterMap] is modified while
     * an iteration over the [Map] is in progress, the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [ScatterMap] with APIs accepting [Map]
     * interfaces, it is less efficient to do so than to use [ScatterMap]'s APIs directly. While the
     * [Map] implementation returned by this method tries to be as efficient as possible, the
     * semantics of [Map] may require the allocation of temporary objects for access and iteration.
     */
    public fun asMap(): Map<K, V> = MapWrapper(this)
}

/**
 * [MutableScatterMap] is a container with a [Map]-like interface based on a flat hash table
 * implementation (the key/value mappings are not stored by nodes but directly into arrays). The
 * underlying implementation is designed to avoid all allocations on insertion, removal, retrieval,
 * and iteration. Allocations may still happen on insertion when the underlying storage needs to
 * grow to accommodate newly added entries to the table. In addition, this implementation minimizes
 * memory usage by avoiding the use of separate objects to hold key/value pairs.
 *
 * This implementation makes no guarantee as to the order of the keys and values stored, nor does it
 * make guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the map (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Multiple threads are safe to read from
 * this map concurrently if no write is happening.
 *
 * **Note**: when a [Map] is absolutely necessary, you can use the method [asMap] to create a thin
 * wrapper around a [MutableScatterMap]. Please refer to [asMap] for more details and caveats.
 *
 * **Note**: when a [MutableMap] is absolutely necessary, you can use the method [asMutableMap] to
 * create a thin wrapper around a [MutableScatterMap]. Please refer to [asMutableMap] for more
 * details and caveats.
 *
 * **MutableScatterMap and SimpleArrayMap**: like [SimpleArrayMap], [MutableScatterMap] is designed
 * to avoid the allocation of extra objects when inserting new entries in the map. However, the
 * implementation of [MutableScatterMap] offers better performance characteristics compared to
 * [SimpleArrayMap] and is thus generally preferable. If memory usage is a concern, [SimpleArrayMap]
 * automatically shrinks its storage to avoid using more memory than necessary. You can also control
 * memory usage with [MutableScatterMap] by manually calling [MutableScatterMap.trim].
 *
 * @param initialCapacity The initial desired capacity for this container. The container will honor
 *   this value by guaranteeing its internal structures can hold that many entries without requiring
 *   any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableScatterMap]
 * @see Map
 */
public class MutableScatterMap<K, V>(initialCapacity: Int = DefaultScatterCapacity) :
    ScatterMap<K, V>() {
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
                max(7, normalizeCapacity(initialCapacity))
            } else {
                0
            }
        _capacity = newCapacity
        initializeMetadata(newCapacity)
        keys = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
        values = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
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
                    writeRawMetadata(this, capacity, Sentinel)
                }
            }
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
    public inline fun getOrPut(key: K, defaultValue: () -> V): V {
        return get(key) ?: defaultValue().also { set(key, it) }
    }

    /**
     * Retrieves a value for [key] and computes a new value based on the existing value (or `null`
     * if the key is not in the map). The computed value is then stored in the map for the given
     * [key].
     *
     * @return value computed by `computeBlock`.
     */
    public inline fun compute(key: K, computeBlock: (key: K, value: V?) -> V): V {
        val index = findInsertIndex(key)
        val inserting = index < 0

        @Suppress("UNCHECKED_CAST")
        val computedValue = computeBlock(key, if (inserting) null else values[index] as V)

        // Skip Array.set() if key is already there
        if (inserting) {
            val insertionIndex = index.inv()
            keys[insertionIndex] = key
            values[insertionIndex] = computedValue
        } else {
            values[index] = computedValue
        }
        return computedValue
    }

    /**
     * Creates a new mapping from [key] to [value] in this map. If [key] is already present in the
     * map, the association is modified and the previously associated value is replaced with
     * [value]. If [key] is not present, a new entry is added to the map, which may require to grow
     * the underlying storage and cause allocations.
     */
    public operator fun set(key: K, value: V) {
        val index = findInsertIndex(key).let { index -> if (index < 0) index.inv() else index }
        keys[index] = key
        values[index] = value
    }

    /**
     * Creates a new mapping from [key] to [value] in this map. If [key] is already present in the
     * map, the association is modified and the previously associated value is replaced with
     * [value]. If [key] is not present, a new entry is added to the map, which may require to grow
     * the underlying storage and cause allocations. Return the previous value associated with the
     * [key], or `null` if the key was not present in the map.
     */
    public fun put(key: K, value: V): V? {
        val index = findInsertIndex(key).let { index -> if (index < 0) index.inv() else index }
        val oldValue = values[index]
        keys[index] = key
        values[index] = value

        @Suppress("UNCHECKED_CAST") return oldValue as V?
    }

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public fun putAll(@Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public fun putAll(pairs: Iterable<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public fun putAll(pairs: Sequence<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /** Puts all the key/value mappings in the [from] map into this map. */
    public fun putAll(from: Map<K, V>) {
        from.forEach { (key, value) -> this[key] = value }
    }

    /** Puts all the key/value mappings in the [from] map into this map. */
    public fun putAll(from: ScatterMap<K, V>) {
        from.forEach { key, value -> this[key] = value }
    }

    /**
     * Puts the key/value mapping from the [pair] in this map, using the first element as the key,
     * and the second element as the value.
     */
    public inline operator fun plusAssign(pair: Pair<K, V>) {
        this[pair.first] = pair.second
    }

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public inline operator fun plusAssign(
        @Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>
    ): Unit = putAll(pairs)

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public inline operator fun plusAssign(pairs: Iterable<Pair<K, V>>): Unit = putAll(pairs)

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value.
     */
    public inline operator fun plusAssign(pairs: Sequence<Pair<K, V>>): Unit = putAll(pairs)

    /** Puts all the key/value mappings in the [from] map into this map. */
    public inline operator fun plusAssign(from: Map<K, V>): Unit = putAll(from)

    /** Puts all the key/value mappings in the [from] map into this map. */
    public inline operator fun plusAssign(from: ScatterMap<K, V>): Unit = putAll(from)

    /**
     * Removes the specified [key] and its associated value from the map. If the [key] was present
     * in the map, this function returns the value that was present before removal.
     */
    public fun remove(key: K): V? {
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
    public fun remove(key: K, value: V): Boolean {
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
    public inline fun removeIf(predicate: (K, V) -> Boolean) {
        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(keys[index] as K, values[index] as V)) {
                removeValueAt(index)
            }
        }
    }

    /** Removes the specified [key] and its associated value from the map. */
    public inline operator fun minusAssign(key: K) {
        remove(key)
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(@Suppress("ArrayReturn") keys: Array<out K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: Iterable<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: Sequence<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: ScatterSet<K>) {
        keys.forEach { key -> remove(key) }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: ObjectList<K>) {
        keys.forEach { key -> remove(key) }
    }

    @PublishedApi
    internal fun removeValueAt(index: Int): V? {
        _size -= 1

        // TODO: We could just mark the entry as empty if there's a group
        //       window around this entry that was already empty
        writeMetadata(metadata, _capacity, index, Deleted)
        keys[index] = null
        val oldValue = values[index]
        values[index] = null

        @Suppress("UNCHECKED_CAST") return oldValue as V?
    }

    /** Removes all mappings from this map. */
    public fun clear() {
        _size = 0
        if (metadata !== EmptyGroup) {
            metadata.fill(AllEmpty)
            writeRawMetadata(metadata, _capacity, Sentinel)
        }
        values.fill(null, 0, _capacity)
        keys.fill(null, 0, _capacity)
        initializeGrowth()
    }

    /**
     * Scans the hash table to find the index at which we can store a value for the give [key]. If
     * the key already exists in the table, its index will be returned, otherwise the `index.inv()`
     * of an empty slot will be returned. Calling this function may cause the internal storage to be
     * reallocated if the table is full.
     */
    @PublishedApi
    internal fun findInsertIndex(key: K): Int {
        val hash = hash(key)
        val hash1 = h1(hash)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                m = m.next()
            }

            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        var index = findFirstAvailableSlot(hash1)
        if (growthLimit == 0 && !isDeleted(metadata, index)) {
            adjustStorage()
            index = findFirstAvailableSlot(hash1)
        }

        _size += 1
        growthLimit -= if (isEmpty(metadata, index)) 1 else 0
        writeMetadata(metadata, _capacity, index, hash2.toLong())

        return index.inv()
    }

    /**
     * Finds the first empty or deleted slot in the table in which we can store a value without
     * resizing the internal storage.
     */
    private fun findFirstAvailableSlot(hash1: Int): Int {
        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            val m = g.maskEmptyOrDeleted()
            if (m != 0L) {
                return (probeOffset + m.lowestBitSet()) and probeMask
            }
            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }
    }

    /**
     * Trims this [MutableScatterMap]'s storage so it is sized appropriately to hold the current
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
        if (_capacity > GroupWidth && _size.toULong() * 32UL <= _capacity.toULong() * 25UL) {
            dropDeletes()
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
            val targetIndex = findFirstAvailableSlot(hash1)

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
                keys[index] = null

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

            index++
        }

        initializeGrowth()
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousKeys = keys
        val previousValues = values
        val previousCapacity = _capacity

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newKeys = keys
        val newValues = values
        val capacity = _capacity

        for (i in 0 until previousCapacity) {
            if (isFull(previousMetadata, i)) {
                val previousKey = previousKeys[i]
                val hash = hash(previousKey)
                val index = findFirstAvailableSlot(h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newKeys[index] = previousKey
                newValues[index] = previousValues[i]
            }
        }
    }

    /**
     * Wraps this [ScatterMap] with a [MutableMap] interface. The [MutableMap] is backed by the
     * [ScatterMap], so changes to the [ScatterMap] are reflected in the [MutableMap] and
     * vice-versa. If the [ScatterMap] is modified while an iteration over the [MutableMap] is in
     * progress (and vice- versa), the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [MutableScatterMap] with APIs accepting
     * [MutableMap] interfaces, it is less efficient to do so than to use [MutableScatterMap]'s APIs
     * directly. While the [MutableMap] implementation returned by this method tries to be as
     * efficient as possible, the semantics of [MutableMap] may require the allocation of temporary
     * objects for access and iteration.
     */
    public fun asMutableMap(): MutableMap<K, V> = MutableMapWrapper(this)
}

internal inline fun convertMetadataForCleanup(metadata: LongArray, capacity: Int) {
    val end = (capacity + 7) shr 3
    for (i in 0 until end) {
        // Converts Sentinel and Deleted to Empty, and Full to Deleted
        val maskedGroup = metadata[i] and BitmaskMsb
        metadata[i] = (maskedGroup.inv() + (maskedGroup ushr 7)) and BitmaskLsb.inv()
    }

    val lastIndex = metadata.lastIndex
    // Restores the sentinel that we overwrote above
    metadata[lastIndex - 1] =
        (Sentinel shl 56) or (metadata[lastIndex - 1] and 0x00ffffff_ffffffffL)
    // Copies the metadata into the clone area
    metadata[lastIndex] = metadata[0]
}

internal fun findEmptySlot(metadata: LongArray, start: Int, end: Int): Int {
    for (i in start until end) {
        if (readRawMetadata(metadata, i) == Empty) {
            return i
        }
    }
    return -1
}

/**
 * Returns the hash code of [k]. The hash spreads low bits to to minimize collisions in high 25-bits
 * that are used for probing.
 */
internal inline fun hash(k: Any?): Int {
    // scramble bits to account for collisions between similar hash values.
    val hash = k.hashCode() * MurmurHashC1
    // spread low bits into high bits that are used for probing
    return hash xor (hash shl 16)
}

// C1 constant from MurmurHash implementation: https://en.wikipedia.org/wiki/MurmurHash#Algorithm
internal const val MurmurHashC1: Int = 0xcc9e2d51.toInt()

// Returns the "H1" part of the specified hash code. In our implementation,
// it is simply the top-most 25 bits
internal inline fun h1(hash: Int) = hash ushr 7

// Returns the "H2" part of the specified hash code. In our implementation,
// this corresponds to the lower 7 bits
internal inline fun h2(hash: Int) = hash and 0x7f

// Assumes [capacity] was normalized with [normalizedCapacity].
// Returns the next 2^m - 1
internal fun nextCapacity(capacity: Int) =
    if (capacity == 0) {
        DefaultScatterCapacity
    } else {
        capacity * 2 + 1
    }

// n -> nearest 2^m - 1
internal fun normalizeCapacity(n: Int) =
    if (n > 0) (0xffffffff.toInt() ushr n.countLeadingZeroBits()) else 0

// Computes the growth based on a load factor of 7/8 for the general case.
// When capacity is < GroupWidth - 1, we use a load factor of 1 instead
internal fun loadedCapacity(capacity: Int): Int {
    // Special cases where x - x / 8 fails
    if (GroupWidth <= 8 && capacity == 7) {
        return 6
    }
    // If capacity is < GroupWidth - 1 we end up here and this formula
    // will return `capacity` in this case, which is what we want
    return capacity - capacity / 8
}

// Inverse of loadedCapacity()
internal fun unloadedCapacity(capacity: Int): Int {
    // Special cases where x + (x - 1) / 7
    if (GroupWidth <= 8 && capacity == 7) {
        return 8
    }
    return capacity + (capacity - 1) / 7
}

/** Reads a single byte from the long array at the specified [offset] in *bytes*. */
@PublishedApi
internal inline fun readRawMetadata(data: LongArray, offset: Int): Long {
    // Take the Long at index `offset / 8` and shift by `offset % 8`
    // A longer explanation can be found in [group()].
    return (data[offset shr 3] shr ((offset and 0x7) shl 3)) and 0xff
}

/**
 * Writes a single byte into the long array at the specified [offset] in *bytes* and copies it, if
 * necessary, into the cloned bytes section at the end of the array.
 *
 * NOTE: [value] must be a single byte, accepted here as a Long to avoid unnecessary conversions.
 */
internal inline fun writeMetadata(data: LongArray, capacity: Int, offset: Int, value: Long) {
    writeRawMetadata(data, offset, value)

    // Mirroring
    // We could/should write a single byte when cloning the metadata by calling
    // writeRawMetadata(), but since our implementation uses Longs for storage,
    // we always write a full Long. We can skip a bit of unnecessary work by just
    // copying the whole group the index falls into.
    // When index is in 0..7, we copy the group over the control bytes at the end of
    // the array, otherwise the group is copied onto itself (cloneIndex shr 3 == index shr 3)
    // TODO: We could further reduce the work we do by always copying index 0 to
    //       lastIndex, but is it interesting in terms of data caches?
    val cloneIndex =
        ((offset - ClonedMetadataCount) and capacity) + (ClonedMetadataCount and capacity)
    data[cloneIndex shr 3] = data[offset shr 3]
}

/**
 * Writes a single byte into the long array at the specified [offset] in *bytes*.
 *
 * NOTE: [value] must be a single byte, accepted here as a Long to avoid unnecessary conversions.
 */
internal inline fun writeRawMetadata(data: LongArray, offset: Int, value: Long) {
    // See [group()] for details. First find the index i in the LongArray,
    // then find the number of bits we need to shift by
    val i = offset shr 3
    val b = (offset and 0x7) shl 3
    // Mask the source data with 0xFF in the right place, then and [value]
    // moved to the right spot
    data[i] = (data[i] and (0xffL shl b).inv()) or (value shl b)
}

internal inline fun isEmpty(metadata: LongArray, index: Int) =
    readRawMetadata(metadata, index) == Empty

internal inline fun isDeleted(metadata: LongArray, index: Int) =
    readRawMetadata(metadata, index) == Deleted

internal inline fun isFull(metadata: LongArray, index: Int): Boolean =
    readRawMetadata(metadata, index) < 0x80L

@PublishedApi internal inline fun isFull(value: Long): Boolean = value < 0x80L

// Bitmasks in our context are abstract bitmasks. They represent a bitmask
// for a Group. i.e. bit 1 is the second least significant byte in the group.
// These bits are also called "abstract bits". For example, given the
// following group of metadata and a group width of 8:
//
// 0x7700550033001100
//   |   |   |   | |___ bit 0 = 0x00
//   |   |   |   |_____ bit 1 = 0x11
//   |   |   |_________ bit 3 = 0x33
//   |   |_____________ bit 5 = 0x55
//   |_________________ bit 7 = 0x77
//
// This is useful when performing group operations to figure out, for
// example, which metadata is set or not.
//
// A static bitmask is a read-only bitmask that allows performing simple
// queries such as [lowestBitSet].
internal typealias StaticBitmask = Long

// A dynamic bitmask is a bitmask that can be iterated on to retrieve,
// for instance, the index of all the "abstract bits" set on the group.
// This assumes the abstract bits are set to either 0x00 (for unset) and
// 0x80 (for set).
internal typealias Bitmask = Long

@PublishedApi internal inline fun StaticBitmask.lowestBitSet(): Int = countTrailingZeroBits() shr 3

/**
 * Returns the index of the next set bit in this mask. If invoked before checking [hasNext], this
 * function returns an invalid index (8).
 */
internal inline fun Bitmask.get() = lowestBitSet()

/**
 * Moves to the next set bit and returns the modified bitmask, call [get] to get the actual index.
 * If this function is called before checking [hasNext], the result is invalid.
 */
internal inline fun Bitmask.next() = this and (this - 1L)

/** Returns true if this [Bitmask] contains more set bits. */
internal inline fun Bitmask.hasNext() = this != 0L

// Least significant bits in the bitmask, one for each metadata in the group
@PublishedApi internal const val BitmaskLsb: Long = 0x01010101_01010101L

// Most significant bits in the bitmask, one for each metadata in the group
//
// NOTE: Ideally we'd use a ULong here, defined as 0x8080808080808080UL but
// using ULong/UByte makes us take a ~10% performance hit on get/set compared to
// a Long. And since Kotlin hates signed constants, we have to use
// -0x7f7f7f7f7f7f7f80L instead of the more sensible 0x8080808080808080L (and
// 0x8080808080808080UL.toLong() isn't considered a constant)
@PublishedApi internal const val BitmaskMsb: Long = -0x7f7f7f7f_7f7f7f80L // srsly Kotlin @#!

/**
 * Creates a [Group] from a metadata array, starting at the specified offset. [offset] must be a
 * valid index in the source array.
 */
internal inline fun group(metadata: LongArray, offset: Int): Group {
    // A Group is a Long read at an arbitrary byte-grained offset inside the
    // Long array. To read the Group, we need to read 2 Longs: one for the
    // most significant bits (MSBs) and one for the least significant bits
    // (LSBs).
    // Let's take an example, with a LongArray of 2 and an offset set to 1
    // byte. We need to read 7 bytes worth of LSBs in Long 0 and 1 byte worth
    // of MSBs in Long 1 (remember we index the bytes from LSB to MSB so in
    // the example below byte 0 is 0x11 and byte 11 is 0xAA):
    //
    //  ___________________ LongArray ____________________
    // |                                                  |
    // [88 77 66 55 44 33 22 11], [FF EE DD CC BB AA 00 99]
    // |_________Long0_______ _|  |_________Long1_______ _|
    //
    // To retrieve the Group we first find the index of Long0 by taking the
    // offset divided by 8. Then offset modulo 8 gives us how many bits we
    // need to shift by. With offset = 1:
    //
    // index = offset / 8 == 0
    // remainder = offset % 8 == 1
    // bitsToShift = remainder * 8
    //
    // LSBs = LongArray[index] >>> bitsToShift
    // MSBs = LongArray[index + 1] << (64 - bitsToShift)
    //
    // We now have:
    //
    // LSBs == 0x0088776655443322
    // MSBs == 0x9900000000000000
    //
    // However we can't just combine MSBs and LSBs with an OR when the offset
    // is a multiple of 8, because we would be attempting to shift left by 64
    // which is a no-op. This means we need to mask the MSBs with 0x0 when
    // offset is 0, and with 0xFF…FF when offset is != 0. We do this by taking
    // the negative value of `bitsToShift`, which will set the MSB when the value
    // is not 0, and doing a signed shift to the right to duplicate it:
    //
    // Group = LSBs | (MSBs & (-b >> 63)
    //
    // Note: since b is only ever 0, 8, 16, 24, 32, 48, 56, or 64, we don't
    // need to shift by 63, we could shift by only 5
    val i = offset shr 3
    val b = (offset and 0x7) shl 3
    return (metadata[i] ushr b) or (metadata[i + 1] shl (64 - b) and (-(b.toLong()) shr 63))
}

/**
 * Returns a [Bitmask] in which every abstract bit set means the corresponding metadata in that slot
 * is equal to [m].
 */
@PublishedApi
internal inline fun Group.match(m: Int): Bitmask {
    // BitmaskLsb * m replicates the byte `m` on every byte of the Long
    // and XOR-ing with `this` will give us a Long in which every non-zero
    // byte indicates a match
    val x = this xor (BitmaskLsb * m)
    // Turn every non-zero byte into 0x80
    return (x - BitmaskLsb) and x.inv() and BitmaskMsb
}

/** Returns a [Bitmask] in which every abstract bit set indicates an empty slot. */
internal inline fun Group.maskEmpty(): Bitmask {
    return (this and (this.inv() shl 6)) and BitmaskMsb
}

/** Returns a [Bitmask] in which every abstract bit set indicates an empty or deleted slot. */
@PublishedApi
internal inline fun Group.maskEmptyOrDeleted(): Bitmask {
    return (this and (this.inv() shl 7)) and BitmaskMsb
}

private class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

private class Entries<K, V>(private val parent: ScatterMap<K, V>) : Set<Map.Entry<K, V>> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return iterator {
            parent.forEachIndexed { index ->
                @Suppress("UNCHECKED_CAST")
                yield(MapEntry(parent.keys[index] as K, parent.values[index] as V))
            }
        }
    }

    override fun containsAll(elements: Collection<Map.Entry<K, V>>): Boolean =
        elements.all { parent[it.key] == it.value }

    override fun contains(element: Map.Entry<K, V>): Boolean = parent[element.key] == element.value
}

private class Keys<K, V>(private val parent: ScatterMap<K, V>) : Set<K> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<K> = iterator { parent.forEachKey { key -> yield(key) } }

    override fun containsAll(elements: Collection<K>): Boolean =
        elements.all { parent.containsKey(it) }

    override fun contains(element: K): Boolean = parent.containsKey(element)
}

private class Values<K, V>(private val parent: ScatterMap<K, V>) : Collection<V> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<V> = iterator {
        parent.forEachValue { value -> yield(value) }
    }

    override fun containsAll(elements: Collection<V>): Boolean =
        elements.all { parent.containsValue(it) }

    override fun contains(element: V): Boolean = parent.containsValue(element)
}

// TODO: While not mandatory, it would be pertinent to throw a
//       ConcurrentModificationException when the underlying ScatterMap
//       is modified while iterating over keys/values/entries. To do
//       this we should probably have some kind of generation ID in
//       ScatterMap that would be incremented on any add/remove/clear
//       or rehash.
private open class MapWrapper<K, V>(private val parent: ScatterMap<K, V>) : Map<K, V> {
    private var _entries: Entries<K, V>? = null
    override val entries: Set<Map.Entry<K, V>>
        get() = _entries ?: Entries(parent).apply { _entries = this }

    private var _keys: Keys<K, V>? = null
    override val keys: Set<K>
        get() = _keys ?: Keys(parent).apply { _keys = this }

    private var _values: Values<K, V>? = null
    override val values: Collection<V>
        get() = _values ?: Values(parent).apply { _values = this }

    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun get(key: K): V? = parent[key]

    override fun containsValue(value: V): Boolean = parent.containsValue(value)

    override fun containsKey(key: K): Boolean = parent.containsKey(key)
}

private class MutableMapEntry<K, V>(
    val keys: Array<Any?>,
    val values: Array<Any?>,
    val index: Int
) : MutableMap.MutableEntry<K, V> {

    @Suppress("UNCHECKED_CAST")
    override fun setValue(newValue: V): V {
        val oldValue = values[index]
        values[index] = newValue
        return oldValue as V
    }

    @Suppress("UNCHECKED_CAST")
    override val key: K
        get() = keys[index] as K

    @Suppress("UNCHECKED_CAST")
    override val value: V
        get() = values[index] as V
}

private class MutableEntries<K, V>(private val parent: MutableScatterMap<K, V>) :
    MutableSet<MutableMap.MutableEntry<K, V>> {

    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
        object : MutableIterator<MutableMap.MutableEntry<K, V>> {
            var iterator: Iterator<MutableMap.MutableEntry<K, V>>
            var current = -1

            init {
                iterator = iterator {
                    parent.forEachIndexed { index ->
                        current = index
                        yield(MutableMapEntry(parent.keys, parent.values, current))
                    }
                }
            }

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): MutableMap.MutableEntry<K, V> = iterator.next()

            override fun remove() {
                if (current != -1) {
                    parent.removeValueAt(current)
                    current = -1
                }
            }
        }

    override fun clear() = parent.clear()

    override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        return elements.all { parent[it.key] == it.value }
    }

    override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
        parent[element.key] == element.value

    override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            var found = false
            for (entry in elements) {
                if (entry.key == parent.keys[index] && entry.value == parent.values[index]) {
                    found = true
                    break
                }
            }
            if (!found) {
                parent.removeValueAt(index)
                changed = true
            }
        }
        return changed
    }

    override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            for (entry in elements) {
                if (entry.key == parent.keys[index] && entry.value == parent.values[index]) {
                    parent.removeValueAt(index)
                    changed = true
                    break
                }
            }
        }
        return changed
    }

    override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
        val index = parent.findKeyIndex(element.key)
        if (index >= 0 && parent.values[index] == element.value) {
            parent.removeValueAt(index)
            return true
        }
        return false
    }
}

private class MutableKeys<K, V>(private val parent: MutableScatterMap<K, V>) : MutableSet<K> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): MutableIterator<K> =
        object : MutableIterator<K> {
            val iterator = iterator { parent.forEachIndexed { index -> yield(index) } }
            var current: Int = -1

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): K {
                current = iterator.next()
                @Suppress("UNCHECKED_CAST") return parent.keys[current] as K
            }

            override fun remove() {
                if (current >= 0) {
                    parent.removeValueAt(current)
                    current = -1
                }
            }
        }

    override fun clear() = parent.clear()

    override fun addAll(elements: Collection<K>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(element: K): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            if (parent.keys[index] !in elements) {
                parent.removeValueAt(index)
                changed = true
            }
        }
        return changed
    }

    override fun removeAll(elements: Collection<K>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            if (parent.keys[index] in elements) {
                parent.removeValueAt(index)
                changed = true
            }
        }
        return changed
    }

    override fun remove(element: K): Boolean {
        val index = parent.findKeyIndex(element)
        if (index >= 0) {
            parent.removeValueAt(index)
            return true
        }
        return false
    }

    override fun containsAll(elements: Collection<K>): Boolean =
        elements.all { parent.containsKey(it) }

    override fun contains(element: K): Boolean = parent.containsKey(element)
}

private class MutableValues<K, V>(private val parent: MutableScatterMap<K, V>) :
    MutableCollection<V> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): MutableIterator<V> =
        object : MutableIterator<V> {
            val iterator = iterator { parent.forEachIndexed { index -> yield(index) } }
            var current: Int = -1

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): V {
                current = iterator.next()
                @Suppress("UNCHECKED_CAST") return parent.values[current] as V
            }

            override fun remove() {
                if (current >= 0) {
                    parent.removeValueAt(current)
                    current = -1
                }
            }
        }

    override fun clear() = parent.clear()

    override fun addAll(elements: Collection<V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(element: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            if (parent.values[index] !in elements) {
                parent.removeValueAt(index)
                changed = true
            }
        }
        return changed
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        var changed = false
        parent.forEachIndexed { index ->
            if (parent.values[index] in elements) {
                parent.removeValueAt(index)
                changed = true
            }
        }
        return changed
    }

    override fun remove(element: V): Boolean {
        parent.forEachIndexed { index ->
            if (parent.values[index] == element) {
                parent.removeValueAt(index)
                return true
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean =
        elements.all { parent.containsValue(it) }

    override fun contains(element: V): Boolean = parent.containsValue(element)
}

private class MutableMapWrapper<K, V>(private val parent: MutableScatterMap<K, V>) :
    MapWrapper<K, V>(parent), MutableMap<K, V> {

    private var _entries: MutableEntries<K, V>? = null
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = _entries ?: MutableEntries(parent).apply { _entries = this }

    private var _keys: MutableKeys<K, V>? = null
    override val keys: MutableSet<K>
        get() = _keys ?: MutableKeys(parent).apply { _keys = this }

    private var _values: MutableValues<K, V>? = null
    override val values: MutableCollection<V>
        get() = _values ?: MutableValues(parent).apply { _values = this }

    override fun clear() = parent.clear()

    override fun remove(key: K): V? = parent.remove(key)

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) -> parent[key] = value }
    }

    override fun put(key: K, value: V): V? = parent.put(key, value)
}
