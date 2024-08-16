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

import androidx.annotation.IntRange
import androidx.collection.internal.requirePrecondition
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to the kotlin source file.
//
// This file was generated from a template in the template directory.
// Make a change to the original template and run the generateCollections.sh script
// to ensure the change is available on all versions of the map.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

// This is a copy of ScatterSet, but with primitive elements

// Default empty set to avoid allocations
private val EmptyLongSet = MutableLongSet(0)

// An empty array of longs
internal val EmptyLongArray = LongArray(0)

/** Returns an empty, read-only [LongSet]. */
public fun emptyLongSet(): LongSet = EmptyLongSet

/** Returns an empty, read-only [ScatterSet]. */
@Suppress("UNCHECKED_CAST") public fun longSetOf(): LongSet = EmptyLongSet

/** Returns a new read-only [LongSet] with only [element1] in it. */
@Suppress("UNCHECKED_CAST")
public fun longSetOf(element1: Long): LongSet = mutableLongSetOf(element1)

/** Returns a new read-only [LongSet] with only [element1] and [element2] in it. */
@Suppress("UNCHECKED_CAST")
public fun longSetOf(element1: Long, element2: Long): LongSet = mutableLongSetOf(element1, element2)

/** Returns a new read-only [LongSet] with only [element1], [element2], and [element3] in it. */
@Suppress("UNCHECKED_CAST")
public fun longSetOf(element1: Long, element2: Long, element3: Long): LongSet =
    mutableLongSetOf(element1, element2, element3)

/** Returns a new read-only [LongSet] with only [elements] in it. */
@Suppress("UNCHECKED_CAST")
public fun longSetOf(vararg elements: Long): LongSet =
    MutableLongSet(elements.size).apply { plusAssign(elements) }

/** Returns a new [MutableLongSet]. */
public fun mutableLongSetOf(): MutableLongSet = MutableLongSet()

/** Returns a new [MutableLongSet] with only [element1] in it. */
public fun mutableLongSetOf(element1: Long): MutableLongSet =
    MutableLongSet(1).apply { plusAssign(element1) }

/** Returns a new [MutableLongSet] with only [element1] and [element2] in it. */
public fun mutableLongSetOf(element1: Long, element2: Long): MutableLongSet =
    MutableLongSet(2).apply {
        plusAssign(element1)
        plusAssign(element2)
    }

/** Returns a new [MutableLongSet] with only [element1], [element2], and [element3] in it. */
public fun mutableLongSetOf(element1: Long, element2: Long, element3: Long): MutableLongSet =
    MutableLongSet(3).apply {
        plusAssign(element1)
        plusAssign(element2)
        plusAssign(element3)
    }

/** Returns a new [MutableLongSet] with the specified elements. */
public fun mutableLongSetOf(vararg elements: Long): MutableLongSet =
    MutableLongSet(elements.size).apply { plusAssign(elements) }

/**
 * [LongSet] is a container with a [Set]-like interface designed to avoid allocations, including
 * boxing.
 *
 * This implementation makes no guarantee as to the order of the elements, nor does it make
 * guarantees that the order remains constant over time.
 *
 * Though [LongSet] offers a read-only interface, it is always backed by a [MutableLongSet]. Read
 * operations alone are thread-safe. However, any mutations done through the backing
 * [MutableLongSet] while reading on another thread are not safe and the developer must protect the
 * set from such changes during read operations.
 *
 * @see [MutableLongSet]
 */
public sealed class LongSet {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` elements, including when
    // the set is empty (see [EmptyGroup]).
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup

    @PublishedApi @JvmField internal var elements: LongArray = EmptyLongArray

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the capacity
    @JvmField internal var _capacity: Int = 0

    /**
     * Returns the number of elements that can be stored in this set without requiring internal
     * storage reallocation.
     */
    @get:IntRange(from = 0)
    public val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @JvmField internal var _size: Int = 0

    /** Returns the number of elements in this set. */
    @get:IntRange(from = 0)
    public val size: Int
        get() = _size

    /** Returns `true` if this set has at least one element. */
    public fun any(): Boolean = _size != 0

    /** Returns `true` if this set has no elements. */
    public fun none(): Boolean = _size == 0

    /** Indicates whether this set is empty. */
    public fun isEmpty(): Boolean = _size == 0

    /** Returns `true` if this set is not empty. */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the first element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public inline fun first(): Long {
        forEach {
            return it
        }
        throw NoSuchElementException("The LongSet is empty")
    }

    /**
     * Returns the first element in the collection for which [predicate] returns `true`.
     *
     * **Note** There is no mechanism for both determining if there is an element that matches
     * [predicate] _and_ returning it if it exists. Developers should use [forEach] to achieve this
     * behavior.
     *
     * @param predicate Called on elements of the set, returning `true` for an element that matches
     *   or `false` if it doesn't
     * @return An element in the set for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     *   collection is empty.
     */
    public inline fun first(predicate: (element: Long) -> Boolean): Long {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        throw NoSuchElementException("Could not find a match")
    }

    /** Iterates over every element stored in this set by invoking the specified [block] lambda. */
    @PublishedApi
    internal inline fun forEachIndex(block: (index: Int) -> Unit) {
        contract { callsInPlace(block) }
        val m = metadata
        val lastIndex = m.size - 2 // We always have 0 or at least 2 elements

        for (i in 0..lastIndex) {
            var slot = m[i]
            if (slot.maskEmptyOrDeleted() != BitmaskMsb) {
                // Branch-less if (i == lastIndex) 7 else 8
                // i - lastIndex returns a negative value when i < lastIndex,
                // so 1 is set as the MSB. By inverting and shifting we get
                // 0 when i < lastIndex, 1 otherwise.
                val bitCount = 8 - ((i - lastIndex).inv() ushr 31)
                for (j in 0 until bitCount) {
                    if (isFull(slot and 0xFFL)) {
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
     * Iterates over every element stored in this set by invoking the specified [block] lambda.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: Long) -> Unit) {
        contract { callsInPlace(block) }
        val k = elements

        forEachIndex { index -> block(k[index]) }
    }

    /**
     * Returns true if all elements match the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: Long) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (!predicate(element)) return false }
        return true
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: Long) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (predicate(element)) return true }
        return false
    }

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public fun count(): Int = _size

    /**
     * Returns the number of elements matching the given [predicate].
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0)
    public inline fun count(predicate: (element: Long) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        forEach { element -> if (predicate(element)) count++ }
        return count
    }

    /**
     * Returns `true` if the specified [element] is present in this set, `false` otherwise.
     *
     * @param element The element to look for in this set
     */
    public operator fun contains(element: Long): Boolean = findElementIndex(element) >= 0

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before and
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
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
    ): String = buildString {
        append(prefix)
        var index = 0
        this@LongSet.forEach { element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            append(element)
            index++
        }
        append(postfix)
    }

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before and
     * [postfix] after, if supplied. [transform] dictates how each element will be represented.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used to
     * generate the string. If the collection holds more than [limit] items, the string is
     * terminated with [truncated].
     */
    @JvmOverloads
    public inline fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        crossinline transform: (Long) -> CharSequence
    ): String = buildString {
        append(prefix)
        var index = 0
        this@LongSet.forEach { element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            append(transform(element))
            index++
        }
        append(postfix)
    }

    /**
     * Returns the hash code value for this set. The hash code of a set is defined to be the sum of
     * the hash codes of the elements in the set.
     */
    public override fun hashCode(): Int {
        var hash = 0

        forEach { element -> hash += element.hashCode() }

        return hash
    }

    /**
     * Compares the specified object [other] with this set for equality. The two objects are
     * considered equal if [other]:
     * - Is a [LongSet]
     * - Has the same [size] as this set
     * - Contains elements equal to this set's elements
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is LongSet) {
            return false
        }
        if (other._size != _size) {
            return false
        }

        forEach { element ->
            if (element !in other) {
                return false
            }
        }

        return true
    }

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `{}`.
     * Each element is separated by `, `.
     */
    override fun toString(): String = joinToString(prefix = "[", postfix = "]")

    /**
     * Scans the set to find the index in the backing arrays of the specified [element]. Returns -1
     * if the element is not present.
     */
    internal inline fun findElementIndex(element: Long): Int {
        val hash = hash(element)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = h1(hash) and probeMask
        var probeIndex = 0
        while (true) {
            val g = group(metadata, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (elements[index] == element) {
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
}

/**
 * [MutableLongSet] is a container with a [MutableSet]-like interface based on a flat hash table
 * implementation. The underlying implementation is designed to avoid all allocations on insertion,
 * removal, retrieval, and iteration. Allocations may still happen on insertion when the underlying
 * storage needs to grow to accommodate newly added elements to the set.
 *
 * This implementation makes no guarantee as to the order of the elements stored, nor does it make
 * guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the set (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Concurrent reads are however safe.
 *
 * @param initialCapacity The initial desired capacity for this container. The container will honor
 *   this value by guaranteeing its internal structures can hold that many elements without
 *   requiring any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableLongSet]
 */
public class MutableLongSet(initialCapacity: Int = DefaultScatterCapacity) : LongSet() {
    // Number of elements we can add before we need to grow
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
        elements = LongArray(newCapacity)
    }

    private fun initializeMetadata(capacity: Int) {
        metadata =
            if (capacity == 0) {
                EmptyGroup
            } else {
                // Round up to the next multiple of 8 and find how many longs we need
                val size = (((capacity + 1 + ClonedMetadataCount) + 7) and 0x7.inv()) shr 3
                LongArray(size).apply { fill(AllEmpty) }
            }
        writeRawMetadata(metadata, capacity, Sentinel)
        initializeGrowth()
    }

    private fun initializeGrowth() {
        growthLimit = loadedCapacity(capacity) - _size
    }

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     * @return `true` if the element has been added or `false` if the element is already contained
     *   within the set.
     */
    public fun add(element: Long): Boolean {
        val oldSize = _size
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
        return _size != oldSize
    }

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     */
    public operator fun plusAssign(element: Long) {
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(@Suppress("ArrayReturn") elements: LongArray): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     */
    public operator fun plusAssign(@Suppress("ArrayReturn") elements: LongArray) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [LongSet] of elements to add to this set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: LongSet): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [LongSet] of elements to add to this set.
     */
    public operator fun plusAssign(elements: LongSet) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Removes the specified [element] from the set.
     *
     * @param element The element to remove from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't present
     *   before removal.
     */
    public fun remove(element: Long): Boolean {
        val index = findElementIndex(element)
        val exists = index >= 0
        if (exists) {
            removeElementAt(index)
        }
        return exists
    }

    /**
     * Removes the specified [element] from the set if it is present.
     *
     * @param element The element to remove from the set.
     */
    public operator fun minusAssign(element: Long) {
        val index = findElementIndex(element)
        if (index >= 0) {
            removeElementAt(index)
        }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(@Suppress("ArrayReturn") elements: LongArray): Boolean {
        val oldSize = _size
        minusAssign(elements)
        return oldSize != _size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     */
    public operator fun minusAssign(@Suppress("ArrayReturn") elements: LongArray) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [LongSet] of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: LongSet): Boolean {
        val oldSize = _size
        minusAssign(elements)
        return oldSize != _size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [LongSet] of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: LongSet) {
        elements.forEach { element -> minusAssign(element) }
    }

    private fun removeElementAt(index: Int) {
        _size -= 1

        // TODO: We could just mark the element as empty if there's a group
        //       window around this element that was already empty
        writeMetadata(metadata, _capacity, index, Deleted)
    }

    /** Removes all elements from this set. */
    public fun clear() {
        _size = 0
        if (metadata !== EmptyGroup) {
            metadata.fill(AllEmpty)
            writeRawMetadata(metadata, _capacity, Sentinel)
        }
        initializeGrowth()
    }

    /**
     * Scans the set to find the index at which we can store a given [element]. If the element
     * already exists in the set, its index will be returned, otherwise the index of an empty slot
     * will be returned. Calling this function may cause the internal storage to be reallocated if
     * the set is full.
     */
    private fun findAbsoluteInsertIndex(element: Long): Int {
        val hash = hash(element)
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
                if (elements[index] == element) {
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

        return index
    }

    /**
     * Finds the first empty or deleted slot in the set in which we can store a value without
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
     * Trims this [MutableLongSet]'s storage so it is sized appropriately to hold the current
     * elements.
     *
     * Returns the number of empty elements removed from this set's storage. Returns 0 if no
     * trimming is necessary or possible.
     */
    @IntRange(from = 0)
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
     * Grow internal storage if necessary. This function can instead opt to remove deleted elements
     * from the set to avoid an expensive reallocation of the underlying storage. This "rehash in
     * place" occurs when the current size is <= 25/32 of the set capacity. The choice of 25/32 is
     * detailed in the implementation of abseil's `raw_hash_map`.
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
        val elements = elements

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

            val hash = hash(elements[index])
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
                metadata[metadata.lastIndex] =
                    (Empty shl 56) or (metadata[0] and 0x00ffffff_ffffffffL)

                index++
                continue
            }

            m = readRawMetadata(metadata, targetIndex)
            if (m == Empty) {
                // The target is empty so we can transfer directly
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())
                writeRawMetadata(metadata, index, Empty)

                elements[targetIndex] = elements[index]
                elements[index] = 0L
            } else /* m == Deleted */ {
                // The target isn't empty so we use an empty slot denoted by
                // swapIndex to perform the swap
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())

                val oldElement = elements[targetIndex]
                elements[targetIndex] = elements[index]
                elements[index] = oldElement

                // Since we exchanged two slots we must repeat the process with
                // element we just moved in the current location
                index--
            }

            // Copies the metadata into the clone area
            metadata[metadata.lastIndex] = (Empty shl 56) or (metadata[0] and 0x00ffffff_ffffffffL)

            index++
        }

        initializeGrowth()
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousElements = elements
        val previousCapacity = _capacity

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newElements = elements
        val capacity = _capacity

        for (i in 0 until previousCapacity) {
            if (isFull(previousMetadata, i)) {
                val previousElement = previousElements[i]
                val hash = hash(previousElement)
                val index = findFirstAvailableSlot(h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newElements[index] = previousElement
            }
        }
    }
}

/**
 * Returns the hash code of [k]. The hash spreads low bits to minimize collisions in high 25-bits
 * that are used for probing.
 */
internal inline fun hash(k: Long): Int {
    // scramble bits to account for collisions between similar hash values.
    val hash = k.hashCode() * MurmurHashC1
    // spread low bits into high bits that are used for probing
    return hash xor (hash shl 16)
}
