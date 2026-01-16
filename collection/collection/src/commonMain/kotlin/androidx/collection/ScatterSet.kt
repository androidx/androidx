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
    "KotlinRedundantDiagnosticSuppress",
    "KotlinConstantConditions",
    "PropertyName",
    "ConstPropertyName",
    "PrivatePropertyName",
    "NOTHING_TO_INLINE",
    "FacadeClassJvmName",
)

package androidx.collection

import androidx.annotation.IntRange

// This is a copy of ScatterMap, but without values

// Default empty set to avoid allocations
private val EmptyScatterSet = MutableScatterSet<Any?>(0)

/** Returns an empty, read-only [ScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> emptyScatterSet(): ScatterSet<E> = EmptyScatterSet as ScatterSet<E>

/** Returns an empty, read-only [ScatterSet]. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(): ScatterSet<E> = EmptyScatterSet as ScatterSet<E>

/** Returns a new read-only [ScatterSet] with only [element1] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E): ScatterSet<E> = mutableScatterSetOf(element1)

/** Returns a new read-only [ScatterSet] with only [element1] and [element2] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E, element2: E): ScatterSet<E> =
    mutableScatterSetOf(element1, element2)

/** Returns a new read-only [ScatterSet] with only [element1], [element2], and [element3] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(element1: E, element2: E, element3: E): ScatterSet<E> =
    mutableScatterSetOf(element1, element2, element3)

/** Returns a new read-only [ScatterSet] with only [elements] in it. */
@Suppress("UNCHECKED_CAST")
public fun <E> scatterSetOf(vararg elements: E): ScatterSet<E> =
    MutableScatterSet<E>(elements.size).apply { plusAssign(elements) }

/** Returns a new [MutableScatterSet]. */
public fun <E> mutableScatterSetOf(): MutableScatterSet<E> = MutableScatterSet()

/** Returns a new [MutableScatterSet] with only [element1] in it. */
public fun <E> mutableScatterSetOf(element1: E): MutableScatterSet<E> =
    MutableScatterSet<E>(1).apply { plusAssign(element1) }

/** Returns a new [MutableScatterSet] with only [element1] and [element2] in it. */
public fun <E> mutableScatterSetOf(element1: E, element2: E): MutableScatterSet<E> =
    MutableScatterSet<E>(2).apply {
        plusAssign(element1)
        plusAssign(element2)
    }

/** Returns a new [MutableScatterSet] with only [element1], [element2], and [element3] in it. */
public fun <E> mutableScatterSetOf(element1: E, element2: E, element3: E): MutableScatterSet<E> =
    MutableScatterSet<E>(3).apply {
        plusAssign(element1)
        plusAssign(element2)
        plusAssign(element3)
    }

/** Returns a new [MutableScatterSet] with the specified contents. */
public fun <E> mutableScatterSetOf(vararg elements: E): MutableScatterSet<E> =
    MutableScatterSet<E>(elements.size).apply { plusAssign(elements) }

/** Returns a new read-only [ScatterSet] with the specified contents. */
public fun <E> Collection<E>.toScatterSet(): ScatterSet<E> =
    if (isEmpty()) emptyScatterSet() else toMutableScatterSet()

/**
 * Returns a new [MutableScatterSet] with the specified contents.
 *
 * The [MutableScatterSet] is created with an initial capacity sufficient to hold the content in the
 * specified [Collection].
 */
public fun <E> Collection<E>.toMutableScatterSet(): MutableScatterSet<E> =
    MutableScatterSet<E>(size).also { it.addAll(this) }

/** Returns a new read-only [ScatterSet] with the specified contents. */
public fun <E> ScatterSet<E>.toScatterSet(): ScatterSet<E> =
    if (isEmpty()) emptyScatterSet() else toMutableScatterSet()

/**
 * Returns a new [MutableScatterSet] with the specified contents.
 *
 * The [MutableScatterSet] is created with an initial capacity sufficient to hold the content in the
 * specified [ScatterSet].
 */
public fun <E> ScatterSet<E>.toMutableScatterSet(): MutableScatterSet<E> =
    MutableScatterSet<E>(size).also { it.addAll(this) }

/**
 * [ScatterSet] is a container with a [Set]-like interface based on a flat hash table
 * implementation. The underlying implementation is designed to avoid all allocations on insertion,
 * removal, retrieval, and iteration. Allocations may still happen on insertion when the underlying
 * storage needs to grow to accommodate newly added elements to the set.
 *
 * This implementation makes no guarantee as to the order of the elements, nor does it make
 * guarantees that the order remains constant over time. If the order of the elements must be
 * preserved, please refer to [OrderedScatterSet].
 *
 * Though [ScatterSet] offers a read-only interface, it is always backed by a [MutableScatterSet].
 * Read operations alone are thread-safe. However, any mutations done through the backing
 * [MutableScatterSet] while reading on another thread are not safe and the developer must protect
 * the set from such changes during read operations.
 *
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around a [ScatterSet]. Please refer to [asSet] for more details and caveats.
 *
 * @see [MutableScatterSet]
 */
public expect sealed class ScatterSet<E> {
    /**
     * Returns the number of elements that can be stored in this set without requiring internal
     * storage reallocation.
     */
    @get:IntRange(from = 0) public val capacity: Int

    /** Returns the number of elements in this set. */
    @get:IntRange(from = 0) public val size: Int

    /** Returns `true` if this set has at least one element. */
    public fun any(): Boolean

    /** Returns `true` if this set has no elements. */
    public fun none(): Boolean

    /** Indicates whether this set is empty. */
    public fun isEmpty(): Boolean

    /** Returns `true` if this set is not empty. */
    public fun isNotEmpty(): Boolean

    /**
     * Returns the first element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public fun first(): E

    /**
     * Returns the first element in the collection for which [predicate] returns `true`
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     *   collection is empty.
     */
    public inline fun first(predicate: (element: E) -> Boolean): E

    /**
     * Returns the first element in the collection for which [predicate] returns `true` or `null` if
     * there are no elements that match [predicate].
     *
     * @param predicate called with each element until it returns `true`.
     * @return The element for which [predicate] returns `true` or `null` if there are no elements
     *   in the set or [predicate] returned `false` for every element in the set.
     */
    public inline fun firstOrNull(predicate: (element: E) -> Boolean): E?

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda. It
     * is safe to remove the element passed to [block] during iteration.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: E) -> Unit)

    /**
     * Returns true if all elements match the given [predicate]. If there are no elements in the
     * set, `true` is returned.
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: E) -> Boolean): Boolean

    /**
     * Returns true if at least one element matches the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: E) -> Boolean): Boolean

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public fun count(): Int

    /**
     * Returns the number of elements matching the given [predicate].
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0) public inline fun count(predicate: (element: E) -> Boolean): Int

    /**
     * Returns true if the specified [element] is present in this hash set, false otherwise.
     *
     * @param element The element to look for in this set
     */
    public operator fun contains(element: E): Boolean

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
    public fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        transform: ((E) -> CharSequence)? = null,
    ): String

    /**
     * Returns the hash code value for this set. The hash code of a set is based on the sum of the
     * hash codes of the elements in the set, where the hash code of a null element is defined to be
     * zero.
     */
    public override fun hashCode(): Int

    /**
     * Compares the specified object [other] with this hash set for equality. The two objects are
     * considered equal if [other]:
     * - Is a [ScatterSet]
     * - Has the same [size] as this set
     * - Contains elements equal to this set's elements
     */
    public override fun equals(other: Any?): Boolean

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `[]`.
     * Each element is separated by `, `.
     */
    override fun toString(): String

    /**
     * Wraps this [ScatterSet] with a [Set] interface. The [Set] is backed by the [ScatterSet], so
     * changes to the [ScatterSet] are reflected in the [Set]. If the [ScatterSet] is modified while
     * an iteration over the [Set] is in progress, the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [ScatterSet] with APIs accepting [Set]
     * interfaces, it is less efficient to do so than to use [ScatterSet]'s APIs directly. While the
     * [Set] implementation returned by this method tries to be as efficient as possible, the
     * semantics of [Set] may require the allocation of temporary objects for access and iteration.
     */
    public fun asSet(): Set<E>
}

/**
 * [MutableScatterSet] is a container with a [MutableSet]-like interface based on a flat hash table
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
 * **Note**: when a [Set] is absolutely necessary, you can use the method [asSet] to create a thin
 * wrapper around a [MutableScatterSet]. Please refer to [asSet] for more details and caveats.
 *
 * **Note**: when a [MutableSet] is absolutely necessary, you can use the method [asMutableSet] to
 * create a thin wrapper around a [MutableScatterSet]. Please refer to [asMutableSet] for more
 * details and caveats.
 *
 * @param initialCapacity The initial desired capacity for this container. the container will honor
 *   this value by guaranteeing its internal structures can hold that many elements without
 *   requiring any allocations. The initial capacity can be set to 0.
 * @constructor Creates a new [MutableScatterSet]
 * @see Set
 */
public expect class MutableScatterSet<E>(initialCapacity: Int = DefaultScatterCapacity) :
    ScatterSet<E> {
    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     * @return `true` if the element has been added or `false` if the element is already contained
     *   within the set.
     */
    public fun add(element: E): Boolean

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     */
    public operator fun plusAssign(element: E)

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Iterable<E>): Boolean

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: Sequence<E>): Boolean

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ScatterSet<E>): Boolean

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: OrderedScatterSet<E>): Boolean

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public fun addAll(elements: ObjectList<E>): Boolean

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements An array of elements to add to the set.
     */
    public operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<out E>)

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements Iterable elements to add to the set.
     */
    public operator fun plusAssign(elements: Iterable<E>)

    /**
     * Adds all the [elements] into this set.
     *
     * @param elements The sequence of elements to add to the set.
     */
    public operator fun plusAssign(elements: Sequence<E>)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ScatterSet<E>)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [OrderedScatterSet] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: OrderedScatterSet<E>)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements An [ObjectList] whose elements are to be added to the set
     */
    public operator fun plusAssign(elements: ObjectList<E>)

    /**
     * Removes the specified [element] from the set.
     *
     * @param element The element to be removed from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't present
     *   before removal.
     */
    public fun remove(element: E): Boolean

    /**
     * Removes the specified [element] from the set if it is present.
     *
     * @param element The element to be removed from the set.
     */
    public operator fun minusAssign(element: E)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Sequence<E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: Iterable<E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ScatterSet<E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: OrderedScatterSet<E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public fun removeAll(elements: ObjectList<E>): Boolean

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An array of elements to be removed from the set.
     */
    public operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<out E>)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A sequence of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Sequence<E>)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A Iterable of elements to be removed from the set.
     */
    public operator fun minusAssign(elements: Iterable<E>)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [ScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ScatterSet<E>)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements A [OrderedScatterSet] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: OrderedScatterSet<E>)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ObjectList] whose elements should be removed from the set.
     */
    public operator fun minusAssign(elements: ObjectList<E>)

    /** Removes any values for which the specified [predicate] returns true. */
    public inline fun removeIf(predicate: (E) -> Boolean)

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @param elements A collection of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: Collection<E>): Boolean

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: ScatterSet<E>): Boolean

    /**
     * Removes all the entries in this set that are not contained in [elements].
     *
     * @params elements A set of elements to preserve in this set.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public fun retainAll(elements: OrderedScatterSet<E>): Boolean

    /**
     * Removes all the elements in this set for which the specified [predicate] is `true`. For each
     * element in the set, the predicate is invoked with that element as the sole parameter.
     *
     * @param predicate Predicate invoked for each element in the set. When the predicate returns
     *   `true`, the element is kept in the set, otherwise it is removed.
     * @return `true` if this set was modified, `false` otherwise.
     */
    public inline fun retainAll(predicate: (E) -> Boolean): Boolean

    /** Removes all elements from this set. */
    public fun clear()

    /**
     * Trims this [MutableScatterSet]'s storage so it is sized appropriately to hold the current
     * elements.
     *
     * Returns the number of empty elements removed from this set's storage. Returns 0 if no
     * trimming is necessary or possible.
     */
    @IntRange(from = 0) public fun trim(): Int

    /**
     * Wraps this [ScatterSet] with a [MutableSet] interface. The [MutableSet] is backed by the
     * [ScatterSet], so changes to the [ScatterSet] are reflected in the [MutableSet] and
     * vice-versa. If the [ScatterSet] is modified while an iteration over the [MutableSet] is in
     * progress (and vice- versa), the results of the iteration are undefined.
     *
     * **Note**: while this method is useful to use this [MutableScatterSet] with APIs accepting
     * [MutableSet] interfaces, it is less efficient to do so than to use [MutableScatterSet]'s APIs
     * directly. While the [MutableSet] implementation returned by this method tries to be as
     * efficient as possible, the semantics of [MutableSet] may require the allocation of temporary
     * objects for access and iteration.
     */
    public fun asMutableSet(): MutableSet<E>
}
