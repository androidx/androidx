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
    "NOTHING_TO_INLINE",
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

public actual sealed class ScatterMap<K, V> {
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

    public actual val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @JvmField internal var _size: Int = 0

    public actual val size: Int
        get() = _size

    private var mapWrapper: Map<K, V>? = null

    public actual fun any(): Boolean = _size != 0

    public actual fun none(): Boolean = _size == 0

    public actual fun isEmpty(): Boolean = _size == 0

    public actual fun isNotEmpty(): Boolean = _size != 0

    public actual operator fun get(key: K): V? {
        val index = findKeyIndex(key)
        @Suppress("UNCHECKED_CAST")
        return if (index >= 0) values[index] as V? else null
    }

    public actual fun getOrDefault(key: K, defaultValue: V): V {
        val index = findKeyIndex(key)
        if (index >= 0) {
            @Suppress("UNCHECKED_CAST")
            return values[index] as V
        }
        return defaultValue
    }

    public actual inline fun getOrElse(key: K, defaultValue: () -> V): V {
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

    public actual inline fun forEach(block: (key: K, value: V) -> Unit) {
        val k = keys
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K, v[index] as V) }
    }

    public actual inline fun forEachKey(block: (key: K) -> Unit) {
        val k = keys

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K) }
    }

    public actual inline fun forEachValue(block: (value: V) -> Unit) {
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(v[index] as V) }
    }

    public actual inline fun all(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (!predicate(key, value)) return false }
        return true
    }

    public actual inline fun any(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (predicate(key, value)) return true }
        return false
    }

    public actual fun count(): Int = size

    public actual inline fun count(predicate: (K, V) -> Boolean): Int {
        var count = 0
        forEach { key, value -> if (predicate(key, value)) count++ }
        return count
    }

    public actual operator fun contains(key: K): Boolean = findKeyIndex(key) >= 0

    public actual fun containsKey(key: K): Boolean = findKeyIndex(key) >= 0

    public actual fun containsValue(value: V): Boolean {
        forEachValue { v -> if (value == v) return true }
        return false
    }

    @JvmOverloads
    public actual fun joinToString(
        separator: CharSequence,
        prefix: CharSequence,
        postfix: CharSequence, // I know this should be suffix, but this is kotlin's name
        limit: Int,
        truncated: CharSequence,
        transform: ((key: K, value: V) -> CharSequence)?,
    ): String = buildString {
        append(prefix)
        run {
            var index = 0
            this@ScatterMap.forEach { key, value ->
                if (index != 0) {
                    append(separator)
                }
                if (index == limit) {
                    append(truncated)
                    return@run
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
        }
        append(postfix)
    }

    public actual override fun hashCode(): Int {
        var hash = 0

        forEach { key, value -> hash += key.hashCode() xor value.hashCode() }

        return hash
    }

    public actual override fun equals(other: Any?): Boolean {
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

    public actual override fun toString(): String {
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

    public actual fun asMap(): Map<K, V> = mapWrapper ?: MapWrapper(this).also { mapWrapper = it }
}

public actual class MutableScatterMap<K, V> @JvmOverloads actual constructor(initialCapacity: Int) :
    ScatterMap<K, V>() {
    // Number of entries we can add before we need to grow
    private var growthLimit = 0

    private var mutableMapWrapper: MutableMap<K, V>? = null

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

    public actual inline fun getOrPut(key: K, defaultValue: () -> V): V {
        val index = findInsertIndex(key)
        return if (index < 0)
            defaultValue().also {
                val insertIndex = index.inv()
                keys[insertIndex] = key
                values[insertIndex] = it
            }
        else
            @Suppress("UNCHECKED_CAST")
            values[index] as V?
                ?: defaultValue().also {
                    keys[index] = key
                    values[index] = it
                }
    }

    public actual inline fun compute(key: K, computeBlock: (key: K, value: V?) -> V): V {
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

    public actual operator fun set(key: K, value: V) {
        val index = findInsertIndex(key).let { index -> if (index < 0) index.inv() else index }
        keys[index] = key
        values[index] = value
    }

    public actual fun put(key: K, value: V): V? {
        val index = findInsertIndex(key).let { index -> if (index < 0) index.inv() else index }
        val oldValue = values[index]
        keys[index] = key
        values[index] = value

        @Suppress("UNCHECKED_CAST")
        return oldValue as V?
    }

    public actual fun putAll(@Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    public actual fun putAll(pairs: Iterable<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    public actual fun putAll(pairs: Sequence<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    public actual fun putAll(from: Map<K, V>) {
        from.forEach { (key, value) -> this[key] = value }
    }

    public actual fun putAll(from: ScatterMap<K, V>) {
        from.forEach { key, value -> this[key] = value }
    }

    public actual inline operator fun plusAssign(pair: Pair<K, V>) {
        this[pair.first] = pair.second
    }

    public actual inline operator fun plusAssign(
        @Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>
    ): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(pairs: Iterable<Pair<K, V>>): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(pairs: Sequence<Pair<K, V>>): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(from: Map<K, V>): Unit = putAll(from)

    public actual inline operator fun plusAssign(from: ScatterMap<K, V>): Unit = putAll(from)

    public actual fun remove(key: K): V? {
        val index = findKeyIndex(key)
        if (index >= 0) {
            return removeValueAt(index)
        }
        return null
    }

    public actual fun remove(key: K, value: V): Boolean {
        val index = findKeyIndex(key)
        if (index >= 0) {
            if (values[index] == value) {
                removeValueAt(index)
                return true
            }
        }
        return false
    }

    public actual inline fun removeIf(predicate: (K, V) -> Boolean) {
        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(keys[index] as K, values[index] as V)) {
                removeValueAt(index)
            }
        }
    }

    public actual inline operator fun minusAssign(key: K) {
        remove(key)
    }

    public actual inline operator fun minusAssign(@Suppress("ArrayReturn") keys: Array<out K>) {
        for (key in keys) {
            remove(key)
        }
    }

    public actual inline operator fun minusAssign(keys: Iterable<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    public actual inline operator fun minusAssign(keys: Sequence<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    public actual inline operator fun minusAssign(keys: ScatterSet<K>) {
        keys.forEach { key -> remove(key) }
    }

    public actual inline operator fun minusAssign(keys: ObjectList<K>) {
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

        @Suppress("UNCHECKED_CAST")
        return oldValue as V?
    }

    public actual fun clear() {
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

    public actual fun trim(): Int {
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

    public actual fun asMutableMap(): MutableMap<K, V> =
        mutableMapWrapper ?: MutableMapWrapper(this).also { mutableMapWrapper = it }
}

private class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V> {
    override fun toString(): String = "$key=$value"
}

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

    override fun toString(): String = joinToString(prefix = "[", postfix = "]")
}

private class Keys<K, V>(private val parent: ScatterMap<K, V>) : Set<K> {
    override val size: Int
        get() = parent._size

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<K> = iterator { parent.forEachKey { key -> yield(key) } }

    override fun containsAll(elements: Collection<K>): Boolean =
        elements.all { parent.containsKey(it) }

    override fun contains(element: K): Boolean = parent.containsKey(element)

    override fun toString(): String = joinToString(prefix = "[", postfix = "]")
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

    override fun toString(): String = joinToString(prefix = "[", postfix = "]")
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MapWrapper<*, *>

        return parent == other.parent
    }

    override fun hashCode(): Int = parent.hashCode()

    override fun toString(): String = parent.toString()
}

private class MutableMapEntry<K, V>(
    val keys: Array<Any?>,
    val values: Array<Any?>,
    val index: Int,
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
                check(current != -1) { "Call next() before removing element from the iterator." }
                parent.removeValueAt(current)
                current = -1
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
                @Suppress("UNCHECKED_CAST")
                return parent.keys[current] as K
            }

            override fun remove() {
                check(current != -1) { "Call next() before removing element from the iterator." }
                parent.removeValueAt(current)
                current = -1
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
                @Suppress("UNCHECKED_CAST")
                return parent.values[current] as V
            }

            override fun remove() {
                check(current != -1) { "Call next() before removing element from the iterator." }
                parent.removeValueAt(current)
                current = -1
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
