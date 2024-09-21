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
    "UnusedImport",
)
@file:OptIn(ExperimentalContracts::class)

package androidx.compose.foundation.demos.collection

import androidx.annotation.IntRange
import androidx.collection.LongSet
import androidx.collection.MutableLongSet
import androidx.collection.emptyLongSet
import androidx.collection.mutableLongSetOf
import androidx.compose.ui.graphics.Color
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to this kotlin source file.
//
// This file was generated from a template:
//   collection/collection/template/ValueClassSet.kt.template
// Make a change to the original template and run the generateValueClassCollections.sh script
// to ensure the change is available on all versions of the set.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/** Returns an empty, read-only [ColorSet]. */
internal inline fun emptyColorSet(): ColorSet = ColorSet(emptyLongSet())

/** Returns an empty, read-only [ColorSet]. */
internal inline fun colorSetOf(): ColorSet = ColorSet(emptyLongSet())

/** Returns a new read-only [ColorSet] with only [element1] in it. */
internal inline fun colorSetOf(element1: Color): ColorSet =
    ColorSet(mutableLongSetOf(element1.value.toLong()))

/** Returns a new read-only [ColorSet] with only [element1] and [element2] in it. */
@Suppress("UNCHECKED_CAST")
internal fun colorSetOf(element1: Color, element2: Color): ColorSet =
    ColorSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
        )
    )

/** Returns a new read-only [ColorSet] with only [element1], [element2], and [element3] in it. */
@Suppress("UNCHECKED_CAST")
internal fun colorSetOf(element1: Color, element2: Color, element3: Color): ColorSet =
    ColorSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
            element3.value.toLong(),
        )
    )

/** Returns a new [MutableColorSet]. */
internal fun mutableColorSetOf(): MutableColorSet = MutableColorSet(MutableLongSet())

/** Returns a new [MutableColorSet] with only [element1] in it. */
internal fun mutableColorSetOf(element1: Color): MutableColorSet =
    MutableColorSet(mutableLongSetOf(element1.value.toLong()))

/** Returns a new [MutableColorSet] with only [element1] and [element2] in it. */
internal fun mutableColorSetOf(element1: Color, element2: Color): MutableColorSet =
    MutableColorSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
        )
    )

/** Returns a new [MutableColorSet] with only [element1], [element2], and [element3] in it. */
internal fun mutableColorSetOf(element1: Color, element2: Color, element3: Color): MutableColorSet =
    MutableColorSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
            element3.value.toLong(),
        )
    )

/**
 * Builds a new [ColorSet] by populating a [MutableColorSet] using the given [builderAction].
 *
 * The set passed as a receiver to the [builderAction] is valid only inside that function. Using it
 * outside of the function produces an unspecified behavior.
 */
internal inline fun buildColorSet(
    builderAction: MutableColorSet.() -> Unit,
): ColorSet {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableColorSet().apply(builderAction).asColorSet()
}

/**
 * Builds a new [ColorSet] by populating a [MutableColorSet] using the given [builderAction].
 *
 * The set passed as a receiver to the [builderAction] is valid only inside that function. Using it
 * outside of the function produces an unspecified behavior.
 *
 * @param initialCapacity Hint for the expected number of elements added in the [builderAction].
 */
internal inline fun buildColorSet(
    initialCapacity: Int,
    builderAction: MutableColorSet.() -> Unit,
): ColorSet {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableColorSet(initialCapacity).apply(builderAction).asColorSet()
}

/**
 * [ColorSet] is a container with a [Set]-like interface designed to avoid allocations, including
 * boxing.
 *
 * This implementation makes no guarantee as to the order of the elements, nor does it make
 * guarantees that the order remains constant over time.
 *
 * Though [ColorSet] offers a read-only interface, it is always backed by a [MutableColorSet]. Read
 * operations alone are thread-safe. However, any mutations done through the backing
 * [MutableColorSet] while reading on another thread are not safe and the developer must protect the
 * set from such changes during read operations.
 *
 * @see [MutableColorSet]
 */
@JvmInline
internal value class ColorSet(val set: LongSet) {
    /**
     * Returns the number of elements that can be stored in this set without requiring internal
     * storage reallocation.
     */
    @get:IntRange(from = 0)
    public inline val capacity: Int
        get() = set.capacity

    /** Returns the number of elements in this set. */
    @get:IntRange(from = 0)
    public inline val size: Int
        get() = set.size

    /** Returns `true` if this set has at least one element. */
    public inline fun any(): Boolean = set.any()

    /** Returns `true` if this set has no elements. */
    public inline fun none(): Boolean = set.none()

    /** Indicates whether this set is empty. */
    public inline fun isEmpty(): Boolean = set.isEmpty()

    /** Returns `true` if this set is not empty. */
    public inline fun isNotEmpty(): Boolean = set.isNotEmpty()

    /**
     * Returns the first element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public inline fun first(): Color = Color(set.first().toULong())

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
    public inline fun first(predicate: (element: Color) -> Boolean): Color =
        Color(set.first { predicate(Color(it.toULong())) }.toULong())

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        set.forEach { block(Color(it.toULong())) }
    }

    /**
     * Returns true if all elements match the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.all { predicate(Color(it.toULong())) }
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.any { predicate(Color(it.toULong())) }
    }

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public inline fun count(): Int = set.count()

    /**
     * Returns the number of elements matching the given [predicate].
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0)
    public inline fun count(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return set.count { predicate(Color(it.toULong())) }
    }

    /**
     * Returns `true` if the specified [element] is present in this set, `false` otherwise.
     *
     * @param element The element to look for in this set
     */
    public inline operator fun contains(element: Color): Boolean =
        set.contains(element.value.toLong())

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `{}`.
     * Each element is separated by `, `.
     */
    public override fun toString(): String {
        if (isEmpty()) {
            return "[]"
        }

        val s = StringBuilder().append('[')
        var index = 0
        forEach { element ->
            if (index++ != 0) {
                s.append(',').append(' ')
            }
            s.append(element)
        }
        return s.append(']').toString()
    }
}

/**
 * [MutableColorSet] is a container with a [MutableSet]-like interface based on a flat hash table
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
 */
@JvmInline
internal value class MutableColorSet(val set: MutableLongSet) {
    /**
     * Returns the number of elements that can be stored in this set without requiring internal
     * storage reallocation.
     */
    @get:IntRange(from = 0)
    public inline val capacity: Int
        get() = set.capacity

    /** Returns the number of elements in this set. */
    @get:IntRange(from = 0)
    public inline val size: Int
        get() = set.size

    /** Returns `true` if this set has at least one element. */
    public inline fun any(): Boolean = set.any()

    /** Returns `true` if this set has no elements. */
    public inline fun none(): Boolean = set.none()

    /** Indicates whether this set is empty. */
    public inline fun isEmpty(): Boolean = set.isEmpty()

    /** Returns `true` if this set is not empty. */
    public inline fun isNotEmpty(): Boolean = set.isNotEmpty()

    /**
     * Returns the first element in the collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    public inline fun first(): Color = Color(set.first().toULong())

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
    public inline fun first(predicate: (element: Color) -> Boolean): Color =
        Color(set.first { predicate(Color(it.toULong())) }.toULong())

    /**
     * Iterates over every element stored in this set by invoking the specified [block] lambda.
     *
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        set.forEach { block(Color(it.toULong())) }
    }

    /**
     * Returns true if all elements match the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns return `true` for
     *   all elements.
     */
    public inline fun all(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.all { predicate(Color(it.toULong())) }
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     *
     * @param predicate called for elements in the set to determine if it returns `true` for any
     *   elements.
     */
    public inline fun any(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.any { predicate(Color(it.toULong())) }
    }

    /** Returns the number of elements in this set. */
    @IntRange(from = 0) public inline fun count(): Int = set.count()

    /**
     * Returns the number of elements matching the given [predicate].
     *
     * @param predicate Called for all elements in the set to count the number for which it returns
     *   `true`.
     */
    @IntRange(from = 0)
    public inline fun count(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return set.count { predicate(Color(it.toULong())) }
    }

    /**
     * Returns `true` if the specified [element] is present in this set, `false` otherwise.
     *
     * @param element The element to look for in this set
     */
    public inline operator fun contains(element: Color): Boolean =
        set.contains(element.value.toLong())

    /**
     * Returns a string representation of this set. The set is denoted in the string by the `{}`.
     * Each element is separated by `, `.
     */
    public override fun toString(): String = asColorSet().toString()

    /**
     * Creates a new [MutableColorSet]
     *
     * @param initialCapacity The initial desired capacity for this container. The container will
     *   honor this value by guaranteeing its internal structures can hold that many elements
     *   without requiring any allocations. The initial capacity can be set to 0.
     */
    public constructor(initialCapacity: Int = 6) : this(MutableLongSet(initialCapacity))

    /** Returns a read-only interface to the set. */
    public inline fun asColorSet(): ColorSet = ColorSet(set)

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     * @return `true` if the element has been added or `false` if the element is already contained
     *   within the set.
     */
    public inline fun add(element: Color): Boolean = set.add(element.value.toLong())

    /**
     * Adds the specified element to the set.
     *
     * @param element The element to add to the set.
     */
    public inline operator fun plusAssign(element: Color) = set.plusAssign(element.value.toLong())

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ColorSet] of elements to add to this set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public inline fun addAll(elements: ColorSet): Boolean = set.addAll(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ColorSet] of elements to add to this set.
     * @return `true` if any of the specified elements were added to the collection, `false` if the
     *   collection was not modified.
     */
    public inline fun addAll(elements: MutableColorSet): Boolean = set.addAll(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ColorSet] of elements to add to this set.
     */
    public inline operator fun plusAssign(elements: ColorSet) = set.plusAssign(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     *
     * @param elements A [ColorSet] of elements to add to this set.
     */
    public inline operator fun plusAssign(elements: MutableColorSet) = set.plusAssign(elements.set)

    /**
     * Removes the specified [element] from the set.
     *
     * @param element The element to remove from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't present
     *   before removal.
     */
    public inline fun remove(element: Color): Boolean = set.remove(element.value.toLong())

    /**
     * Removes the specified [element] from the set if it is present.
     *
     * @param element The element to remove from the set.
     */
    public inline operator fun minusAssign(element: Color) = set.minusAssign(element.value.toLong())

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ColorSet] of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public inline fun removeAll(elements: ColorSet): Boolean = set.removeAll(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ColorSet] of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public inline fun removeAll(elements: MutableColorSet): Boolean = set.removeAll(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ColorSet] of elements to be removed from the set.
     */
    public inline operator fun minusAssign(elements: ColorSet) = set.minusAssign(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     *
     * @param elements An [ColorSet] of elements to be removed from the set.
     */
    public inline operator fun minusAssign(elements: MutableColorSet) =
        set.minusAssign(elements.set)

    /** Removes all elements from this set. */
    public inline fun clear() = set.clear()

    /**
     * Trims this [MutableColorSet]'s storage so it is sized appropriately to hold the current
     * elements.
     *
     * Returns the number of empty elements removed from this set's storage. Returns 0 if no
     * trimming is necessary or possible.
     */
    @IntRange(from = 0) public inline fun trim(): Int = set.trim()
}
