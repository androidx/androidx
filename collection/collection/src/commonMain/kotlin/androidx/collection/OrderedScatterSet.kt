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
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.requirePrecondition
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

// Default empty set to avoid allocations
private val EmptyOrderedScatterSet = MutableOrderedScatterSet<Any?>(0)

/** Returns an empty, read-only [OrderedScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> emptyOrderedScatterSet(): OrderedScatterSet<E> =
    EmptyOrderedScatterSet as OrderedScatterSet<E>

/** Returns an empty, read-only [OrderedScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> orderedScatterSetOf(): OrderedScatterSet<E> =
    EmptyOrderedScatterSet as OrderedScatterSet<E>

/** Returns a new read-only [OrderedScatterSet] with only [element1] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> orderedScatterSetOf(element1: E): OrderedScatterSet<E> =
    mutableOrderedScatterSetOf(element1)

/** Returns a new read-only [OrderedScatterSet] with only [element1] and [element2] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> orderedScatterSetOf(element1: E, element2: E): OrderedScatterSet<E> =
    mutableOrderedScatterSetOf(element1, element2)

/**
 * Returns a new read-only [OrderedScatterSet] with only [element1], [element2], and [element3] in
 * it.
 */
@Suppress("UNCHECKED_CAST")
public fun <E> orderedScatterSetOf(element1: E, element2: E, element3: E): OrderedScatterSet<E> =
    mutableOrderedScatterSetOf(element1, element2, element3)

/** Returns a new read-only [OrderedScatterSet] with only [elements] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> orderedScatterSetOf(vararg elements: E): OrderedScatterSet<E> =
    MutableOrderedScatterSet<E>(elements.size).apply { plusAssign(elements) }

/** Returns a new [MutableOrderedScatterSet]. */
public fun <E> mutableOrderedScatterSetOf(): MutableOrderedScatterSet<E> =
    MutableOrderedScatterSet()

/** Returns a new [MutableOrderedScatterSet] with only [element1] in it. */
public fun <E> mutableOrderedScatterSetOf(element1: E): MutableOrderedScatterSet<E> =
    MutableOrderedScatterSet<E>(1).apply { plusAssign(element1) }

/** Returns a new [MutableOrderedScatterSet] with only [element1] and [element2] in it. */
public fun <E> mutableOrderedScatterSetOf(element1: E, element2: E): MutableOrderedScatterSet<E> =
    MutableOrderedScatterSet<E>(2).apply {
        plusAssign(element1)
        plusAssign(element2)
    }

/**
 * Returns a new [MutableOrderedScatterSet] with only [element1], [element2], and [element3] in it.
 */
public fun <E> mutableOrderedScatterSetOf(
    element1: E,
    element2: E,
    element3: E
): MutableOrderedScatterSet<E> =
    MutableOrderedScatterSet<E>(3).apply {
        plusAssign(element1)
        plusAssign(element2)
        plusAssign(element3)
    }

/** Returns a new [MutableOrderedScatterSet] with the specified contents. */
public fun <E> mutableOrderedScatterSetOf(vararg elements: E): MutableOrderedScatterSet<E> =
    MutableOrderedScatterSet<E>(elements.size).apply { plusAssign(elements) }

/**
 * [OrderedScatterSet] is a container with a [Set]-like interface based on a flat hash table
 * implementation that preserves the insertion order for iteration. The underlying implementation is
 * designed to avoid all allocations on insertion, removal, retrieval, and iteration. Allocations
 * may still happen on insertion when the underlying storage needs to grow to accommodate newly
 * added elements to the set.
 *
 * This implementation guarantees the order of the elements when iterating over them using [forEach]
 * or the iterator provided by [asSet].
 *
 * Though [OrderedScatterSet] offers a read-only interface, it is always backed by a
 * [MutableOrderedScatterSet]. Read operations alone are thread-safe. However, any mutations done
 * through the backing [MutableScatterSet] while reading on another thread are not safe and the
 * developer must protect the set from such changes during read operations.
 *
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around an [OrderedScatterSet]. Please refer to [asSet] for more details and caveats.
 *
 * @see [MutableOrderedScatterSet]
 */
public sealed class OrderedScatterSet<E> {
    // NOTE: Our arrays are marked internal to implement inlined forEach{}
    // The backing array for the metadata bytes contains
    // `capacity + 1 + ClonedMetadataCount` elements, including when
    // the set is empty (see [EmptyGroup]).
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup

    @PublishedApi @JvmField internal var elements: Array<Any?> = EMPTY_OBJECTS

    @PublishedApi @JvmField internal var nodes: LongArray = EmptyNodes
    @PublishedApi @JvmField internal var head: Int = NodeInvalidLink
    @PublishedApi @JvmField internal var tail: Int = NodeInvalidLink

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
    public fun first(): E {
        forEach {
            return it
        }
        throw NoSuchElementException("The OrderedScatterSet is empty")
    }

    /**
     * Returns the first element in insertion order in the collection for which [predicate] returns
     * `true`.
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     *   collection is empty.
     */
    public fun first(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        throw NoSuchElementException("Could not find a match")
    }

    /**
     * Returns the first element in insertion order in the collection for which [predicate] returns
     * `true` or `null` if there are no elements that match [predicate].
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true` or `null` if there are no elements
     *   in the set or [predicate] returned `false` for every element in the set.
     */
    public inline fun firstOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEach { if (predicate(it)) return it }
        return null
    }

    /**
     * Returns the last element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public fun last(): E {
        forEachReverse {
            return it
        }
        throw NoSuchElementException("The OrderedScatterSet is empty")
    }

    /**
     * Returns the last element in insertion order in the collection for which [predicate] returns
     * `true`.
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     *   collection is empty.
     */
    public fun last(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEachReverse { if (predicate(it)) return it }
        throw NoSuchElementException("Could not find a match")
    }

    /**
     * Returns the first element in insertion order in the collection for which [predicate] returns
     * `true` or `null` if there are no elements that match [predicate].
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true` or `null` if there are no elements
     *   in the set or [predicate] returned `false` for every element in the set.
     */
    public inline fun lastOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEachReverse { if (predicate(it)) return it }
        return null
    }

    internal inline fun forEachIndex(block: (index: Int) -> Unit) {
        contract { callsInPlace(block) }
        val nodes = nodes

        var candidate = tail
        while (candidate != NodeInvalidLink) {
            val previousNode = nodes[candidate].previousNode
            @Suppress("UNCHECKED_CAST") block(candidate)
            candidate = previousNode
        }
    }

    /** Iterates over every element stored in this set by invoking the specified [block] lambda. */
    @PublishedApi
    internal inline fun unorderedForEachIndex(block: (index: Int) -> Unit) {
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
     * Iterates over every element stored in this set by invoking the specified [block] lambda. The
     * iteration order is the same as the insertion order. It is safe to remove the element passed
     * to [block] during iteration.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val elements = elements
        val nodes = nodes

        var candidate = tail
        while (candidate != NodeInvalidLink) {
            val previousNode = nodes[candidate].previousNode
            @Suppress("UNCHECKED_CAST") block(elements[candidate] as E)
            candidate = previousNode
        }
    }

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda. The
     * iteration order is the reverse of the insertion order. It is safe to remove the element
     * passed to [block] during iteration.
     *
     * @param block called with each element in the set
     */
    public inline fun forEachReverse(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val elements = elements
        val nodes = nodes

        var candidate = head
        while (candidate != NodeInvalidLink) {
            val nextNode = nodes[candidate].nextNode
            @Suppress("UNCHECKED_CAST") block(elements[candidate] as E)
            candidate = nextNode
        }
    }

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda. The
     * elements are iterated in arbitrary order.
     *
     * @param block called with each element in the set
     */
    @PublishedApi
    internal inline fun unorderedForEach(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val elements = elements
        unorderedForEachIndex { index -> @Suppress("UNCHECKED_CAST") block(elements[index] as E) }
    }

    /** Returns a new list containing this set's entries, in insertion order. */
    public inline fun toList(): List<E> {
        return ArrayList<E>(size).apply {
            this@OrderedScatterSet.forEach { element -> add(element) }
        }
    }

    /**
     * Returns true if all elements match the given [predicate]. If there are no elements in the
     * set, `true` is returned. The order in which the predicate is called over the elements is
     * arbitrary.
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        unorderedForEach { element -> if (!predicate(element)) return false }
        return true
    }

    /**
     * Returns true if at least one element matches the given [predicate]. The order in which the
     * predicate is called over the elements is arbitrary.
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        unorderedForEach { element -> if (predicate(element)) return true }
        return false
    }

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public fun count(): Int = size

    /**
     * Returns the number of elements matching the given [predicate].The order in which the
     * predicate is called over the elements is arbitrary.
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0)
    public inline fun count(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        unorderedForEach { element -> if (predicate(element)) count++ }
        return count
    }

    /**
     * Returns true if the specified [element] is present in this hash set, false otherwise.
     *
     * @param element The element to look for in this set
     */
    public operator fun contains(element: E): Boolean = findElementIndex(element) >= 0

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
        transform: ((E) -> CharSequence)? = null
    ): String = buildString {
        append(prefix)
        var index = 0
        this@OrderedScatterSet.forEach { element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            if (transform == null) {
                append(element)
            } else {
                append(transform(element))
            }
            index++
        }
        append(postfix)
    }

    /**
     * Returns the hash code value for this set. The hash code of a set is based on the sum of the
     * hash codes of the elements in the set, where the hash code of a null element is defined to be
     * zero.
     */
    public override fun hashCode(): Int {
        var hash = _capacity
        hash = 31 * hash + _size

        unorderedForEach { element ->
            if (element != this) {
                hash += element.hashCode()
            }
        }

        return hash
    }

    /**
     * Compares the specified object [other] with this hash set for equality. The two objects are
     * considered equal if [other]:
     * - Is a [OrderedScatterSet]
     * - Has the same [size] as this set
     * - Contains elements equal to this set's elements
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is OrderedScatterSet<*>) {
            return false
        }
        if (other.size != size) {
            return false
        }

        @Suppress("UNCHECKED_CAST") val o = other as OrderedScatterSet<Any?>

        unorderedForEach { element ->
            if (element !in o) {
                return false
            }
        }

        return true
    }

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `[]`.
     * Each element is separated by `, `.
     */
    override fun toString(): String =
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

    /**
     * Wraps this [OrderedScatterSet] with a [Set] interface. The [Set] is backed by the
     * [OrderedScatterSet], so changes to the [OrderedScatterSet] are reflected in the [Set]. If the
     * [OrderedScatterSet] is modified while an iteration over the [Set] is in progress, the results
     * of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [OrderedScatterSet] with APIs accepting
     * [Set] interfaces, it is less efficient to do so than to use [OrderedScatterSet]'s APIs
     * directly. While the [Set] implementation returned by this method tries to be as efficient as
     * possible, the semantics of [Set] may require the allocation of temporary objects for access
     * and iteration.
     */
    public fun asSet(): Set<E> = SetWrapper()

    internal open inner class SetWrapper : Set<E> {
        override val size: Int
            get() = this@OrderedScatterSet._size

        override fun containsAll(elements: Collection<E>): Boolean {
            elements.forEach { element ->
                if (!this@OrderedScatterSet.contains(element)) {
                    return false
                }
            }
            return true
        }

        @Suppress("KotlinOperator")
        override fun contains(element: E): Boolean {
            return this@OrderedScatterSet.contains(element)
        }

        override fun isEmpty(): Boolean = this@OrderedScatterSet.isEmpty()

        override fun iterator(): Iterator<E> {
            return iterator { this@OrderedScatterSet.forEach { element -> yield(element) } }
        }
    }
}

/**
 * [MutableOrderedScatterSet] is a container with a [MutableSet]-like interface based on a flat hash
 * table implementation that preserves the insertion order for iteration. The underlying
 * implementation is designed to avoid all allocations on insertion, removal, retrieval, and
 * iteration. Allocations may still happen on insertion when the underlying storage needs to grow to
 * accommodate newly added elements to the set.
 *
 * This implementation guarantees the order of the elements when iterating over them using [forEach]
 * or the iterator provided by [asMutableSet].
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the set (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Concurrent reads are however safe.
 *
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around a [MutableOrderedScatterSet]. Please refer to [asSet] for more details and
 * caveats.
 *
 * **Note**: when a [MutableSet] is absolutely necessary, you can use the method [asMutableSet] to
 * create a thin wrapper around a [MutableOrderedScatterSet]. Please refer to [asMutableSet] for
 * more details and caveats.
 *
 * @param initialCapacity The initial desired capacity for this container. the container will honor
 *   this value by guaranteeing its internal structures can hold that many elements without
 *   requiring any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableOrderedScatterSet]
 * @see Set
 */
public class MutableOrderedScatterSet<E>(initialCapacity: Int = DefaultScatterCapacity) :
    OrderedScatterSet<E>() {
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
        nodes =
            if (newCapacity == 0) EmptyNodes else LongArray(newCapacity).apply { fill(EmptyNode) }
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
    public fun add(element: E): Boolean {
        val oldSize = size
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
        moveNodeToHead(index)
        return size != oldSize
    }

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     */
    public operator fun plusAssign(element: E) {
        val index = findAbsoluteInsertIndex(element)
        elements[index] = element
        moveNodeToHead(index)
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        plusAssign(elements)
        return oldSize != size
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     */
    public operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     */
    public operator fun plusAssign(elements: Iterable<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     */
    public operator fun plusAssign(elements: Sequence<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Removes the specified [element] from the set.
     *
     * @param element The element to be removed from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't present
     *   before removal.
     */
    public fun remove(element: E): Boolean {
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
     * @param element The element to be removed from the set.
     */
    public operator fun minusAssign(element: E) {
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
    public fun removeAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Sequence<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Iterable<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: OrderedScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ScatterSet<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ObjectList<E>): Boolean {
        val oldSize = size
        minusAssign(elements)
        return oldSize != size
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     */
    public operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Sequence<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Iterable<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: OrderedScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes any values for which the specified [predicate] returns true. */
    public inline fun removeIf(predicate: (E) -> Boolean) {
        val elements = elements
        unorderedForEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @param elements A collection of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: Collection<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        unorderedForEachIndex { index ->
            if (internalElements[index] !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: OrderedScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        unorderedForEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: ScatterSet<E>): Boolean {
        val internalElements = this.elements
        val startSize = _size
        unorderedForEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (internalElements[index] as E !in elements) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    /**
     * Removes all the elements in this set for which the specified [predicate] is `true`. For each
     * element in the set, the predicate is invoked with that element as the sole parameter.
     *
     * @param predicate Predicate invoked for each element in the set. When the predicate returns
     *   `true`, the element is kept in the set, otherwise it is removed.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(predicate: (E) -> Boolean): Boolean {
        val elements = elements
        val startSize = _size
        unorderedForEachIndex { index ->
            @Suppress("UNCHECKED_CAST")
            if (!predicate(elements[index] as E)) {
                removeElementAt(index)
            }
        }
        return startSize != _size
    }

    @PublishedApi
    internal fun removeElementAt(index: Int) {
        _size -= 1

        // TODO: We could just mark the element as empty if there's a group
        //       window around this element that was already empty
        writeMetadata(metadata, _capacity, index, Deleted)
        elements[index] = null

        removeNode(index)
    }

    private inline fun moveNodeToHead(index: Int) {
        nodes[index] = createLinkToNext(head)

        if (head != NodeInvalidLink) {
            nodes[head] = setLinkToPrevious(nodes[head], index)
        }
        head = index

        if (tail == NodeInvalidLink) {
            tail = index
        }
    }

    private inline fun removeNode(index: Int) {
        val nodes = nodes
        val node = nodes[index]
        val previousIndex = node.previousNode
        val nextIndex = node.nextNode

        if (previousIndex != NodeInvalidLink) {
            nodes[previousIndex] = setLinkToNext(nodes[previousIndex], nextIndex)
        } else {
            head = nextIndex
        }

        if (nextIndex != NodeInvalidLink) {
            nodes[nextIndex] = setLinkToPrevious(nodes[nextIndex], previousIndex)
        } else {
            tail = previousIndex
        }

        nodes[index] = EmptyNode
    }

    /** Removes all elements from this set. */
    public fun clear() {
        _size = 0
        if (metadata !== EmptyGroup) {
            metadata.fill(AllEmpty)
            writeRawMetadata(metadata, _capacity, Sentinel)
        }
        elements.fill(null, 0, _capacity)
        nodes.fill(EmptyNode)
        head = NodeInvalidLink
        tail = NodeInvalidLink
        initializeGrowth()
    }

    /**
     * Scans the set to find the index at which we can store the given [element]. If the element
     * already exists in the set, its index will be returned, otherwise the index of an empty slot
     * will be returned. Calling this function may cause the internal storage to be reallocated if
     * the set is full.
     */
    private fun findAbsoluteInsertIndex(element: E): Int {
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
     * Finds the first empty or deleted slot in the set in which we can store an element without
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
     * Trims this [MutableOrderedScatterSet]'s storage so it is sized appropriately to hold the
     * current elements.
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
     * Remove entries until the total size of the remaining entries is less than or equal to
     * [maxSize]. Entries added last are removed first. Calling this method has no effect if
     * [maxSize] is greater than [size].
     */
    public fun trimToSize(maxSize: Int) {
        val nodes = nodes
        var candidate = head
        while (candidate != NodeInvalidLink && _size > maxSize && _size != 0) {
            val nextNode = nodes[candidate].nextNode
            removeElementAt(candidate)
            candidate = nextNode
        }
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
        // TODO: This shouldn't be required, but without it the compiler generates an extra
        //       200+ aarch64 instructions to generate a NullPointerException.
        @Suppress("SENSELESS_COMPARISON") if (metadata == null) return

        val capacity = _capacity
        val elements = elements
        val nodes = nodes

        val indexMapping = IntArray(capacity)

        // Converts Sentinel and Deleted to Empty, and Full to Deleted
        convertMetadataForCleanup(metadata, capacity)

        var swapIndex = -1
        var index = 0

        // Drop deleted items and re-hashes surviving entries
        while (index != capacity) {
            var m = readRawMetadata(metadata, index)
            // Formerly Deleted entry, we can use it as a swap spot
            if (m == Empty) {
                swapIndex = index
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

                indexMapping[index] = index

                // Copies the metadata into the clone area
                metadata[metadata.size - 1] = metadata[0]

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

                nodes[targetIndex] = nodes[index]
                nodes[index] = EmptyNode

                indexMapping[index] = targetIndex

                swapIndex = index
            } else /* m == Deleted */ {
                // The target isn't empty so we use an empty slot denoted by
                // swapIndex to perform the swap
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())

                if (swapIndex == -1) {
                    swapIndex = findEmptySlot(metadata, index + 1, capacity)
                }

                elements[swapIndex] = elements[targetIndex]
                elements[targetIndex] = elements[index]
                elements[index] = elements[swapIndex]

                nodes[swapIndex] = nodes[targetIndex]
                nodes[targetIndex] = nodes[index]
                nodes[index] = nodes[swapIndex]

                indexMapping[index] = targetIndex
                indexMapping[targetIndex] = index

                // Since we exchanged two slots we must repeat the process with
                // element we just moved in the current location
                index--
            }

            // Copies the metadata into the clone area
            metadata[metadata.size - 1] = metadata[0]

            index++
        }

        initializeGrowth()

        fixupNodes(indexMapping)
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousElements = elements
        val previousNodes = nodes
        val previousCapacity = _capacity

        val indexMapping = IntArray(previousCapacity)

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newElements = elements
        val newNodes = nodes
        val capacity = _capacity

        for (i in 0 until previousCapacity) {
            if (isFull(previousMetadata, i)) {
                val previousKey = previousElements[i]
                val hash = hash(previousKey)
                val index = findFirstAvailableSlot(h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newElements[index] = previousKey
                newNodes[index] = previousNodes[i]

                indexMapping[i] = index
            }
        }

        fixupNodes(indexMapping)
    }

    private fun fixupNodes(mapping: IntArray) {
        val nodes = nodes
        for (i in nodes.indices) {
            val node = nodes[i]
            val previous = node.previousNode
            val next = node.nextNode
            nodes[i] = createLinks(node, previous, next, mapping)
        }
        if (head != NodeInvalidLink) head = mapping[head]
        if (tail != NodeInvalidLink) tail = mapping[tail]
    }

    /**
     * Wraps this [OrderedScatterSet] with a [MutableSet] interface. The [MutableSet] is backed by
     * the [OrderedScatterSet], so changes to the [OrderedScatterSet] are reflected in the
     * [MutableSet] and vice-versa. If the [OrderedScatterSet] is modified while an iteration over
     * the [MutableSet] is in progress (and vice- versa), the results of the iteration are
     * undefined.
     *
     * **Note**: while this method is useful to use this [MutableOrderedScatterSet] with APIs
     * accepting [MutableSet] interfaces, it is less efficient to do so than to use
     * [MutableOrderedScatterSet]'s APIs directly. While the [MutableSet] implementation returned by
     * this method tries to be as efficient as possible, the semantics of [MutableSet] may require
     * the allocation of temporary objects for access and iteration.
     */
    public fun asMutableSet(): MutableSet<E> = MutableSetWrapper()

    private inner class MutableSetWrapper : SetWrapper(), MutableSet<E> {
        override fun add(element: E): Boolean = this@MutableOrderedScatterSet.add(element)

        override fun addAll(elements: Collection<E>): Boolean =
            this@MutableOrderedScatterSet.addAll(elements)

        override fun clear() {
            this@MutableOrderedScatterSet.clear()
        }

        override fun iterator(): MutableIterator<E> =
            object : MutableIterator<E> {
                var current = -1
                val iterator = iterator {
                    this@MutableOrderedScatterSet.forEachIndex { index ->
                        current = index
                        @Suppress("UNCHECKED_CAST") yield(elements[index] as E)
                    }
                }

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): E = iterator.next()

                override fun remove() {
                    if (current != -1) {
                        this@MutableOrderedScatterSet.removeElementAt(current)
                        current = -1
                    }
                }
            }

        override fun remove(element: E): Boolean = this@MutableOrderedScatterSet.remove(element)

        override fun retainAll(elements: Collection<E>): Boolean =
            this@MutableOrderedScatterSet.retainAll(elements)

        override fun removeAll(elements: Collection<E>): Boolean =
            this@MutableOrderedScatterSet.removeAll(elements)
    }
}
