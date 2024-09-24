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
import androidx.collection.LongList
import androidx.collection.MutableLongList
import androidx.collection.emptyLongList
import androidx.collection.mutableLongListOf
import androidx.compose.ui.graphics.Color
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to this kotlin source file.
//
// This file was generated from a template:
//   collection/collection/template/ValueClassList.kt.template
// Make a change to the original template and run the generateValueClassCollections.sh script
// to ensure the change is available on all versions of the list.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/**
 * [ColorList] is a [List]-like collection for [Color] values. It allows retrieving the elements
 * without boxing. [ColorList] is always backed by a [MutableColorList], its [MutableList]-like
 * subclass.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the list (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. It is also not safe to mutate during
 * reentrancy -- in the middle of a [forEach], for example. However, concurrent reads are safe.
 */
@JvmInline
internal value class ColorList(val list: LongList) {
    /** The number of elements in the [ColorList]. */
    @get:IntRange(from = 0)
    public inline val size: Int
        get() = list.size

    /** Returns the last valid index in the [ColorList]. This can be `-1` when the list is empty. */
    @get:IntRange(from = -1)
    public inline val lastIndex: Int
        get() = list.lastIndex

    /** Returns an [IntRange] of the valid indices for this [ColorList]. */
    public inline val indices: kotlin.ranges.IntRange
        get() = list.indices

    /** Returns `true` if the collection has no elements in it. */
    public inline fun none(): Boolean = list.none()

    /** Returns `true` if there's at least one element in the collection. */
    public inline fun any(): Boolean = list.any()

    /** Returns `true` if any of the elements give a `true` return value for [predicate]. */
    public inline fun any(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.any { predicate(Color(it.toULong())) }
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    public inline fun reversedAny(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.reversedAny { predicate(Color(it.toULong())) }
    }

    /** Returns `true` if the [ColorList] contains [element] or `false` otherwise. */
    public inline operator fun contains(element: Color): Boolean =
        list.contains(element.value.toLong())

    /**
     * Returns `true` if the [ColorList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public inline fun containsAll(elements: ColorList): Boolean = list.containsAll(elements.list)

    /**
     * Returns `true` if the [ColorList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public inline fun containsAll(elements: MutableColorList): Boolean =
        list.containsAll(elements.list)

    /** Returns the number of elements in this list. */
    public inline fun count(): Int = list.count()

    /**
     * Counts the number of elements matching [predicate].
     *
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.count { predicate(Color(it.toULong())) }
    }

    /**
     * Returns the first element in the [ColorList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public inline fun first(): Color = Color(list.first().toULong())

    /**
     * Returns the first element in the [ColorList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfFirst
     */
    public inline fun first(predicate: (element: Color) -> Boolean): Color {
        contract { callsInPlace(predicate) }
        return Color(list.first { predicate(Color(it.toULong())) }.toULong())
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and calculates
     *   the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: Color) -> R): R {
        contract { callsInPlace(operation) }
        return list.fold(initial) { acc, element -> operation(acc, Color(element.toULong())) }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: Color) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldIndexed(initial) { index, acc, element ->
            operation(index, acc, Color(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in reverse order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     *   calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: Color, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        return list.foldRight(initial) { element, acc -> operation(Color(element.toULong()), acc) }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: Color, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldRightIndexed(initial) { index, element, acc ->
            operation(index, Color(element.toULong()), acc)
        }
    }

    /**
     * Calls [block] for each element in the [ColorList], in order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEach(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEach { block(Color(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] along with its index, in order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachIndexed { index, element -> block(index, Color(element.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] in reverse order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEachReversed(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversed { block(Color(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] along with its index, in reverse order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversedIndexed { index, element -> block(index, Color(element.toULong())) }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public inline operator fun get(@IntRange(from = 0) index: Int): Color =
        Color(list[index].toULong())

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public inline fun elementAt(@IntRange(from = 0) index: Int): Color =
        Color(list[index].toULong())

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds of the
     * collection.
     *
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at an
     *   index not in the list.
     */
    public inline fun elementAtOrElse(
        @IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> Color
    ): Color = Color(list.elementAtOrElse(index) { defaultValue(it).value.toLong() }.toULong())

    /** Returns the index of [element] in the [ColorList] or `-1` if [element] is not there. */
    public inline fun indexOf(element: Color): Int = list.indexOf(element.value.toLong())

    /**
     * Returns the index if the first element in the [ColorList] for which [predicate] returns
     * `true`.
     */
    public inline fun indexOfFirst(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfFirst { predicate(Color(it.toULong())) }
    }

    /**
     * Returns the index if the last element in the [ColorList] for which [predicate] returns
     * `true`.
     */
    public inline fun indexOfLast(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfLast { predicate(Color(it.toULong())) }
    }

    /** Returns `true` if the [ColorList] has no elements in it or `false` otherwise. */
    public inline fun isEmpty(): Boolean = list.isEmpty()

    /** Returns `true` if there are elements in the [ColorList] or `false` if it is empty. */
    public inline fun isNotEmpty(): Boolean = list.isNotEmpty()

    /**
     * Returns the last element in the [ColorList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public inline fun last(): Color = Color(list.last().toULong())

    /**
     * Returns the last element in the [ColorList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfLast
     */
    public inline fun last(predicate: (element: Color) -> Boolean): Color {
        contract { callsInPlace(predicate) }
        return Color(list.last { predicate(Color(it.toULong())) }.toULong())
    }

    /**
     * Returns the index of the last element in the [ColorList] that is the same as [element] or
     * `-1` if no elements match.
     */
    public inline fun lastIndexOf(element: Color): Int = list.lastIndexOf(element.value.toLong())

    /**
     * Returns a String representation of the list, surrounded by "[]" and each element separated by
     * ", ".
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "[]"
        }
        return buildString {
            append('[')
            forEachIndexed { index: Int, element: Color ->
                if (index != 0) {
                    append(',').append(' ')
                }
                append(element)
            }
            append(']')
        }
    }
}

/**
 * [MutableColorList] is a [MutableList]-like collection for [Color] values. It allows storing and
 * retrieving the elements without boxing. Immutable access is available through its base class
 * [ColorList], which has a [List]-like interface.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the list (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. It is also not safe to mutate during
 * reentrancy -- in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * @constructor Creates a [MutableColorList] with a [capacity] of `initialCapacity`.
 */
@JvmInline
internal value class MutableColorList(val list: MutableLongList) {
    public constructor(initialCapacity: Int = 16) : this(MutableLongList(initialCapacity))

    /** The number of elements in the [ColorList]. */
    @get:IntRange(from = 0)
    public inline val size: Int
        get() = list.size

    /** Returns the last valid index in the [ColorList]. This can be `-1` when the list is empty. */
    @get:IntRange(from = -1)
    public inline val lastIndex: Int
        get() = list.lastIndex

    /** Returns an [IntRange] of the valid indices for this [ColorList]. */
    public inline val indices: kotlin.ranges.IntRange
        get() = list.indices

    /** Returns `true` if the collection has no elements in it. */
    public inline fun none(): Boolean = list.none()

    /** Returns `true` if there's at least one element in the collection. */
    public inline fun any(): Boolean = list.any()

    /** Returns `true` if any of the elements give a `true` return value for [predicate]. */
    public inline fun any(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.any { predicate(Color(it.toULong())) }
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    public inline fun reversedAny(predicate: (element: Color) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.reversedAny { predicate(Color(it.toULong())) }
    }

    /** Returns `true` if the [ColorList] contains [element] or `false` otherwise. */
    public inline operator fun contains(element: Color): Boolean =
        list.contains(element.value.toLong())

    /**
     * Returns `true` if the [ColorList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public inline fun containsAll(elements: ColorList): Boolean = list.containsAll(elements.list)

    /**
     * Returns `true` if the [ColorList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public inline fun containsAll(elements: MutableColorList): Boolean =
        list.containsAll(elements.list)

    /** Returns the number of elements in this list. */
    public inline fun count(): Int = list.count()

    /**
     * Counts the number of elements matching [predicate].
     *
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.count { predicate(Color(it.toULong())) }
    }

    /**
     * Returns the first element in the [ColorList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public inline fun first(): Color = Color(list.first().toULong())

    /**
     * Returns the first element in the [ColorList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfFirst
     */
    public inline fun first(predicate: (element: Color) -> Boolean): Color {
        contract { callsInPlace(predicate) }
        return Color(list.first { predicate(Color(it.toULong())) }.toULong())
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and calculates
     *   the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: Color) -> R): R {
        contract { callsInPlace(operation) }
        return list.fold(initial) { acc, element -> operation(acc, Color(element.toULong())) }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: Color) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldIndexed(initial) { index, acc, element ->
            operation(index, acc, Color(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in reverse order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     *   calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: Color, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        return list.foldRight(initial) { element, acc -> operation(Color(element.toULong()), acc) }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ColorList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: Color, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldRightIndexed(initial) { index, element, acc ->
            operation(index, Color(element.toULong()), acc)
        }
    }

    /**
     * Calls [block] for each element in the [ColorList], in order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEach(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEach { block(Color(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] along with its index, in order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachIndexed { index, element -> block(index, Color(element.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] in reverse order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEachReversed(block: (element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversed { block(Color(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [ColorList] along with its index, in reverse order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: Color) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversedIndexed { index, element -> block(index, Color(element.toULong())) }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public inline operator fun get(@IntRange(from = 0) index: Int): Color =
        Color(list[index].toULong())

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public inline fun elementAt(@IntRange(from = 0) index: Int): Color =
        Color(list[index].toULong())

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds of the
     * collection.
     *
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at an
     *   index not in the list.
     */
    public inline fun elementAtOrElse(
        @IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> Color
    ): Color = Color(list.elementAtOrElse(index) { defaultValue(it).value.toLong() }.toULong())

    /** Returns the index of [element] in the [ColorList] or `-1` if [element] is not there. */
    public inline fun indexOf(element: Color): Int = list.indexOf(element.value.toLong())

    /**
     * Returns the index if the first element in the [ColorList] for which [predicate] returns
     * `true`.
     */
    public inline fun indexOfFirst(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfFirst { predicate(Color(it.toULong())) }
    }

    /**
     * Returns the index if the last element in the [ColorList] for which [predicate] returns
     * `true`.
     */
    public inline fun indexOfLast(predicate: (element: Color) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfLast { predicate(Color(it.toULong())) }
    }

    /** Returns `true` if the [ColorList] has no elements in it or `false` otherwise. */
    public inline fun isEmpty(): Boolean = list.isEmpty()

    /** Returns `true` if there are elements in the [ColorList] or `false` if it is empty. */
    public inline fun isNotEmpty(): Boolean = list.isNotEmpty()

    /**
     * Returns the last element in the [ColorList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public inline fun last(): Color = Color(list.last().toULong())

    /**
     * Returns the last element in the [ColorList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfLast
     */
    public inline fun last(predicate: (element: Color) -> Boolean): Color {
        contract { callsInPlace(predicate) }
        return Color(list.last { predicate(Color(it.toULong())) }.toULong())
    }

    /**
     * Returns the index of the last element in the [ColorList] that is the same as [element] or
     * `-1` if no elements match.
     */
    public inline fun lastIndexOf(element: Color): Int = list.lastIndexOf(element.value.toLong())

    /**
     * Returns a String representation of the list, surrounded by "[]" and each element separated by
     * ", ".
     */
    override fun toString(): String = asColorList().toString()

    /** Returns a read-only interface to the list. */
    public inline fun asColorList(): ColorList = ColorList(list)

    /**
     * Returns the total number of elements that can be held before the [MutableColorList] must
     * grow.
     *
     * @see ensureCapacity
     */
    public inline val capacity: Int
        get() = list.capacity

    /** Adds [element] to the [MutableColorList] and returns `true`. */
    public inline fun add(element: Color): Boolean = list.add(element.value.toLong())

    /**
     * Adds [element] to the [MutableColorList] at the given [index], shifting over any elements at
     * [index] and after, if any.
     *
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun add(@IntRange(from = 0) index: Int, element: Color) =
        list.add(index, element.value.toLong())

    /**
     * Adds all [elements] to the [MutableColorList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     *
     * @return `true` if the [MutableColorList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun addAll(@IntRange(from = 0) index: Int, elements: ColorList): Boolean =
        list.addAll(index, elements.list)

    /**
     * Adds all [elements] to the [MutableColorList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     *
     * @return `true` if the [MutableColorList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun addAll(@IntRange(from = 0) index: Int, elements: MutableColorList): Boolean =
        list.addAll(index, elements.list)

    /**
     * Adds all [elements] to the end of the [MutableColorList] and returns `true` if the
     * [MutableColorList] was changed or `false` if [elements] was empty.
     */
    public inline fun addAll(elements: ColorList): Boolean = list.addAll(elements.list)

    /** Adds all [elements] to the end of the [MutableColorList]. */
    public inline operator fun plusAssign(elements: ColorList) = list.plusAssign(elements.list)

    /**
     * Adds all [elements] to the end of the [MutableColorList] and returns `true` if the
     * [MutableColorList] was changed or `false` if [elements] was empty.
     */
    public inline fun addAll(elements: MutableColorList): Boolean = list.addAll(elements.list)

    /** Adds all [elements] to the end of the [MutableColorList]. */
    public inline operator fun plusAssign(elements: MutableColorList) =
        list.plusAssign(elements.list)

    /**
     * Removes all elements in the [MutableColorList]. The storage isn't released.
     *
     * @see trim
     */
    public inline fun clear() = list.clear()

    /**
     * Reduces the internal storage. If [capacity] is greater than [minCapacity] and [size], the
     * internal storage is reduced to the maximum of [size] and [minCapacity].
     *
     * @see ensureCapacity
     */
    public inline fun trim(minCapacity: Int = size) = list.trim(minCapacity)

    /**
     * Ensures that there is enough space to store [capacity] elements in the [MutableColorList].
     *
     * @see trim
     */
    public inline fun ensureCapacity(capacity: Int) = list.ensureCapacity(capacity)

    /** [add] [element] to the [MutableColorList]. */
    public inline operator fun plusAssign(element: Color) = list.plusAssign(element.value.toLong())

    /** [remove] [element] from the [MutableColorList] */
    public inline operator fun minusAssign(element: Color) =
        list.minusAssign(element.value.toLong())

    /**
     * Removes [element] from the [MutableColorList]. If [element] was in the [MutableColorList] and
     * was removed, `true` will be returned, or `false` will be returned if the element was not
     * found.
     */
    public inline fun remove(element: Color): Boolean = list.remove(element.value.toLong())

    /**
     * Removes all [elements] from the [MutableColorList] and returns `true` if anything was
     * removed.
     */
    public inline fun removeAll(elements: ColorList): Boolean = list.removeAll(elements.list)

    /** Removes all [elements] from the [MutableColorList]. */
    public inline operator fun minusAssign(elements: ColorList) = list.minusAssign(elements.list)

    /**
     * Removes all [elements] from the [MutableColorList] and returns `true` if anything was
     * removed.
     */
    public inline fun removeAll(elements: MutableColorList): Boolean = list.removeAll(elements.list)

    /** Removes all [elements] from the [MutableColorList]. */
    public inline operator fun minusAssign(elements: MutableColorList) =
        list.minusAssign(elements.list)

    /**
     * Removes the element at the given [index] and returns it.
     *
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public inline fun removeAt(@IntRange(from = 0) index: Int): Color =
        Color(list.removeAt(index).toULong())

    /**
     * Removes items from index [start] (inclusive) to [end] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [start] or [end] isn't between 0 and [size], inclusive
     * @throws IllegalArgumentException if [start] is greater than [end]
     */
    public inline fun removeRange(@IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int) =
        list.removeRange(start, end)

    /**
     * Keeps only [elements] in the [MutableColorList] and removes all other values.
     *
     * @return `true` if the [MutableColorList] has changed.
     */
    public inline fun retainAll(elements: ColorList): Boolean = list.retainAll(elements.list)

    /**
     * Keeps only [elements] in the [MutableColorList] and removes all other values.
     *
     * @return `true` if the [MutableColorList] has changed.
     */
    public inline fun retainAll(elements: MutableColorList): Boolean = list.retainAll(elements.list)

    /**
     * Sets the value at [index] to [element].
     *
     * @return the previous value set at [index]
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public inline operator fun set(@IntRange(from = 0) index: Int, element: Color): Color =
        Color(list.set(index, element.value.toLong()).toULong())
}

/** @return a read-only [ColorList] with nothing in it. */
internal inline fun emptyColorList(): ColorList = ColorList(emptyLongList())

/** @return a read-only [ColorList] with nothing in it. */
internal inline fun colorListOf(): ColorList = ColorList(emptyLongList())

/** @return a new read-only [ColorList] with [element1] as the only item in the list. */
internal inline fun colorListOf(element1: Color): ColorList =
    ColorList(mutableLongListOf(element1.value.toLong()))

/** @return a new read-only [ColorList] with 2 elements, [element1] and [element2], in order. */
internal inline fun colorListOf(element1: Color, element2: Color): ColorList =
    ColorList(mutableLongListOf(element1.value.toLong(), element2.value.toLong()))

/**
 * @return a new read-only [ColorList] with 3 elements, [element1], [element2], and [element3], in
 *   order.
 */
internal inline fun colorListOf(element1: Color, element2: Color, element3: Color): ColorList =
    ColorList(
        mutableLongListOf(element1.value.toLong(), element2.value.toLong(), element3.value.toLong())
    )

/** @return a new empty [MutableColorList] with the default capacity. */
internal inline fun mutableColorListOf(): MutableColorList = MutableColorList(MutableLongList())

/** @return a new [MutableColorList] with [element1] as the only item in the list. */
internal inline fun mutableColorListOf(element1: Color): MutableColorList =
    MutableColorList(mutableLongListOf(element1.value.toLong()))

/** @return a new [MutableColorList] with 2 elements, [element1] and [element2], in order. */
internal inline fun mutableColorListOf(element1: Color, element2: Color): MutableColorList =
    MutableColorList(mutableLongListOf(element1.value.toLong(), element2.value.toLong()))

/**
 * @return a new [MutableColorList] with 3 elements, [element1], [element2], and [element3], in
 *   order.
 */
internal inline fun mutableColorListOf(
    element1: Color,
    element2: Color,
    element3: Color
): MutableColorList =
    MutableColorList(
        mutableLongListOf(element1.value.toLong(), element2.value.toLong(), element3.value.toLong())
    )

/**
 * Builds a new [ColorList] by populating a [MutableColorList] using the given [builderAction].
 *
 * The list passed as a receiver to the [builderAction] is valid only inside that function. Using it
 * outside of the function produces an unspecified behavior.
 *
 * @param builderAction Lambda in which the [MutableColorList] can be populated.
 */
internal inline fun buildColorList(
    builderAction: MutableColorList.() -> Unit,
): ColorList {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableColorList().apply(builderAction).asColorList()
}

/**
 * Builds a new [ColorList] by populating a [MutableColorList] using the given [builderAction].
 *
 * The list passed as a receiver to the [builderAction] is valid only inside that function. Using it
 * outside of the function produces an unspecified behavior.
 *
 * @param initialCapacity Hint for the expected number of elements added in the [builderAction].
 * @param builderAction Lambda in which the [MutableColorList] can be populated.
 */
internal inline fun buildColorList(
    initialCapacity: Int,
    builderAction: MutableColorList.() -> Unit,
): ColorList {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableColorList(initialCapacity).apply(builderAction).asColorList()
}
