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
    "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE",
)

package androidx.collection

import androidx.annotation.IntRange
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.requirePrecondition
import androidx.collection.internal.throwNoSuchElementExceptionForInline
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

// This is a copy of ScatterMap, but without values

public actual sealed class ScatterSet<E> {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` elements, including when
    // the set is empty (see [EmptyGroup]).
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup

    @PublishedApi @JvmField internal var elements: Array<Any?> = EMPTY_OBJECTS

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the capacity
    @JvmField internal var _capacity: Int = 0

    @get:IntRange(from = 0)
    public actual val capacity: Int
        get() = _capacity

    // We use a backing field for capacity to avoid invokevirtual calls
    // every time we need to look at the size
    @JvmField internal var _size: Int = 0

    @JvmField internal var mutableSetWrapper: MutableSet<E>? = null

    @get:IntRange(from = 0)
    public actual val size: Int
        get() = _size

    public actual fun any(): Boolean = _size != 0

    public actual fun none(): Boolean = _size == 0

    public actual fun isEmpty(): Boolean = _size == 0

    public actual fun isNotEmpty(): Boolean = _size != 0

    public actual fun first(): E {
        forEach {
            return it
        }
        throwNoSuchElementExceptionForInline("The ScatterSet is empty")
    }

    public actual inline fun first(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        throwNoSuchElementExceptionForInline("Could not find a match")
    }

    public actual inline fun firstOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        return null
    }

    /** Iterates over every element stored in this set by invoking the specified [block] lambda. */
    @PublishedApi
    internal inline fun forEachIndex(block: (index: Int) -> Unit) {
        contract { callsInPlace(block) }
        val m = metadata
        val lastIndex = m.size - 2 // We always have 0 or at least 2 elements

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

    public actual inline fun forEach(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val elements = elements

        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST") block(elements[index] as E)
        }
    }

    public actual inline fun all(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (!predicate(element)) return false }
        return true
    }

    public actual inline fun any(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach { element -> if (predicate(element)) return true }
        return false
    }

    @IntRange(from = 0) public actual fun count(): Int = size

    @IntRange(from = 0)
    public actual inline fun count(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        forEach { element -> if (predicate(element)) count++ }
        return count
    }

    public actual operator fun contains(element: E): Boolean = findElementIndex(element) >= 0

    @JvmOverloads
    public actual fun joinToString(
        separator: CharSequence,
        prefix: CharSequence,
        postfix: CharSequence, // I know this should be suffix, but this is kotlin's name
        limit: Int,
        truncated: CharSequence,
        transform: ((E) -> CharSequence)?,
    ): String = buildString {
        append(prefix)
        run {
            var index = 0
            this@ScatterSet.forEach { element ->
                if (index != 0) {
                    append(separator)
                }
                if (index == limit) {
                    append(truncated)
                    return@run
                }
                if (transform == null) {
                    append(element)
                } else {
                    append(transform(element))
                }
                index++
            }
        }
        append(postfix)
    }

    public actual override fun hashCode(): Int {
        var hash = 0

        forEach { element -> hash += element.hashCode() }

        return hash
    }

    public actual override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ScatterSet<*>) {
            return false
        }
        if (other.size != size) {
            return false
        }

        @Suppress("UNCHECKED_CAST") val o = other as ScatterSet<Any?>

        forEach { element ->
            if (element !in o) {
                return false
            }
        }

        return true
    }

    actual override fun toString(): String =
        joinToString(prefix = "[", postfix = "]") { element ->
            if (element === this) {
                "(this)"
            } else {
                element.toString()
            }
        }

    /**
     * Scans the set to find the index in the backing arrays of the specified [element]. Returns -1
     * if the element is not present.
     */
    internal inline fun findElementIndex(element: E): Int {
        val hash = hash(element)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = h1(hash) and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var match = g.match(hash2)

            while (match.hasNext()) {
                val index = (probeOffset + match.get()) and probeMask
                if (elements[index] == element) {
                    return index
                }
                match = match.next()
            }

            // An Empty slot indicates the element cannot be present further along the probe chain.
            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        return -1
    }

    public actual fun asSet(): Set<E> =
        mutableSetWrapper
            ?: MutableSetWrapper(this as MutableScatterSet).also { mutableSetWrapper = it }
}

public actual class MutableScatterSet<E> @JvmOverloads actual constructor(initialCapacity: Int) :
    ScatterSet<E>() {
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
        elements = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
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

    public actual fun add(element: E): Boolean {
        val oldSize = size
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
        return size != oldSize
    }

    public actual operator fun plusAssign(element: E) {
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
    }

    public actual fun addAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual fun addAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual fun addAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual fun addAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual fun addAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual fun addAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    public actual operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual operator fun plusAssign(elements: Iterable<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual operator fun plusAssign(elements: Sequence<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual operator fun plusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual operator fun plusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual operator fun plusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    public actual fun remove(element: E): Boolean {
        val index = findElementIndex(element)
        val exists = index >= 0
        if (exists) {
            removeElementAt(index)
        }
        return exists
    }

    public actual operator fun minusAssign(element: E) {
        val index = findElementIndex(element)
        if (index >= 0) {
            removeElementAt(index)
        }
    }

    public actual fun removeAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual fun removeAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual fun removeAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual fun removeAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual fun removeAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual fun removeAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    public actual operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual operator fun minusAssign(elements: Sequence<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual operator fun minusAssign(elements: Iterable<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual operator fun minusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual operator fun minusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual operator fun minusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    public actual inline fun removeIf(predicate: (E) -> Boolean) {
        val elements = elements
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
    }

    public actual fun retainAll(elements: Collection<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            if (internalElements[index] !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    public actual fun retainAll(elements: ScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    public actual fun retainAll(elements: OrderedScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    public actual inline fun retainAll(predicate: (E) -> Boolean): Boolean {
        val elements = elements
        val startSize = size
        forEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (!predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
        return startSize != size
    }

    @PublishedApi
    internal fun removeElementAt(index: Int) {
        _size -= 1

        // TODO: We could just mark the element as empty if there's a group
        //       window around this element that was already empty
        writeMetadata(metadata, _capacity, index, Deleted)
        elements[index] = null
    }

    public actual fun clear() {
        _size = 0
        // When the set was never allocated (or capacity is 0), avoid redundant array fill calls
        if (_capacity == 0) {
            growthLimit = 0
            return
        }
        if (metadata !== EmptyGroup) {
            metadata.fill(AllEmpty)
            writeRawMetadata(metadata, _capacity, Sentinel)
        }
        elements.fill(null, 0, _capacity)
        initializeGrowth()
    }

    /**
     * Scans the set to find the index at which we can store the given [element]. If the element
     * already exists in the set, its index will be returned, otherwise the index of an empty slot
     * will be returned. Calling this function may cause the internal storage to be reallocated if
     * the set is full.
     *
     * This method executes a single-pass probe: as candidate slots matching H2 are checked for
     * [element], the first available (Empty or Deleted) slot encountered along the probe sequence
     * is tracked in `firstAvail`. Probing terminates as soon as a group containing an Empty slot is
     * encountered (as guaranteed by quadratic probing invariants), avoiding a second probe pass to
     * find a slot.
     */
    private fun findAbsoluteInsertIndex(element: E): Int {
        val hash = hash(element)
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
                if (elements[index] == element) {
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
            return insertSlowPath(element)
        }
        // H2 is the low byte of h2Pattern (BitmaskLsb * h2, with h2 <= 0x7F)
        writeMetadata(metadata, _capacity, firstAvail, h2Pattern and 0x7FL)
        _size++
        this.growthLimit = growthLimit - if (isDeleted) 0 else 1
        return firstAvail
    }

    private fun insertSlowPath(element: E): Int {
        // Recomputing the hash here (only when storage must be adjusted) keeps it out of the
        // probe loop's live set in the caller.
        val hash = hash(element)
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

    @IntRange(from = 0)
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
     * Grow internal storage if necessary. This function can instead opt to remove deleted elements
     * from the set to avoid an expensive reallocation of the underlying storage. This "rehash in
     * place" occurs when the current size is <= 25/32 of the set capacity. The choice of 25/32 is
     * detailed in the implementation of abseil's `raw_hash_map`.
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
        val elements = elements

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

            val hash = hash(elements[index])
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

                elements[targetIndex] = elements[index]
                elements[index] = null
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
        val previousElements = elements

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newElements = elements
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
                val previousElement = previousElements[previousIndex]
                val hash = hash(previousElement)
                val index = findFirstAvailableSlot(newMetadata, capacity, h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newElements[index] = previousElement

                mask = mask and (mask - 1L)
            }
            i++
        }
    }

    public actual fun asMutableSet(): MutableSet<E> =
        mutableSetWrapper ?: MutableSetWrapper(this).also { mutableSetWrapper = it }
}

private open class SetWrapper<E>(private val parent: ScatterSet<E>) : Set<E> {
    override val size: Int
        get() = parent._size

    override fun containsAll(elements: Collection<E>): Boolean {
        elements.forEach { element ->
            if (!parent.contains(element)) {
                return false
            }
        }
        return true
    }

    @Suppress("KotlinOperator")
    override fun contains(element: E): Boolean {
        return parent.contains(element)
    }

    override fun isEmpty(): Boolean = parent.isEmpty()

    override fun iterator(): Iterator<E> {
        return iterator { parent.forEach { element -> yield(element) } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is SetWrapper<*>) return parent == other.parent
        if (other is Set<*>) {
            if (parent._size != other.size) return false
            parent.forEach { value ->
                if (value !in other) return false
            }
            return true
        }

        return false
    }

    override fun hashCode(): Int {
        return parent.hashCode()
    }

    override fun toString(): String = parent.toString()
}

private class MutableSetWrapper<E>(private val parent: MutableScatterSet<E>) :
    SetWrapper<E>(parent), MutableSet<E> {
    override fun add(element: E): Boolean = parent.add(element)

    override fun addAll(elements: Collection<E>): Boolean = parent.addAll(elements)

    override fun clear() {
        parent.clear()
    }

    override fun iterator(): MutableIterator<E> =
        object : MutableIterator<E> {
            var current = -1
            val iterator = iterator {
                parent.forEachIndex { index ->
                    current = index
                    @Suppress("UNCHECKED_CAST") yield(parent.elements[index] as E)
                }
            }

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): E = iterator.next()

            override fun remove() {
                if (current != -1) {
                    parent.removeElementAt(current)
                    current = -1
                }
            }
        }

    override fun remove(element: E): Boolean = parent.remove(element)

    override fun retainAll(elements: Collection<E>): Boolean = parent.retainAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = parent.removeAll(elements)
}
