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
@file:Suppress("NOTHING_TO_INLINE", "RedundantVisibilityModifier", "UnusedImport")
/* ktlint-disable max-line-length */
/* ktlint-disable import-ordering */

package androidx.collection.template

import androidx.collection.LongList
import androidx.collection.MutableLongList
import androidx.collection.emptyLongList
import androidx.collection.mutableLongListOf
import androidx.collection.TestValueClass
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

// To use this template, you must substitute several strings. You can copy this and search/replace
// or use a sed command. These properties must be changed:
// * androidx.collection.template - target package (e.g. androidx.compose.ui.ui.collection)
// * androidx.collection - package in which the value class resides
// * TestValueClass - the value class contained in the list (e.g. Color or Offset)
// * testValueClass - the value class, with the first letter lower case (e.g. color or offset)
// * value.toLong() - the field in TestValueClass containing the backing primitive (e.g. packedValue)
// * Long - the primitive type of the backing list (e.g. Long or Float)
// * .toULong() - an operation done on the primitive to convert to the value class parameter
//
// For example, to create a ColorList:
// sed -e "s/androidx.collection.template/androidx.compose.ui.graphics/" -e "s/TestValueClass/Color/g" \
//     -e "s/testValueClass/color/g" -e "s/value.toLong()/value.toLong()/g" -e "s/Long/Long/g" \
//     -e "s/.toULong()/.toULong()/g" -e "s/androidx.collection/androidx.compose.ui.graphics/g" \
//     collection/collection/template/ValueClassList.kt.template \
//     > compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/ColorList.kt

/**
 * [TestValueClassList] is a [List]-like collection for [TestValueClass] values. It allows retrieving
 * the elements without boxing. [TestValueClassList] is always backed by a [MutableTestValueClassList],
 * its [MutableList]-like subclass.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 */
@OptIn(ExperimentalContracts::class)
@JvmInline
internal value class TestValueClassList(val list: LongList) {
    /**
     * The number of elements in the [TestValueClassList].
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val size: Int get() = list.size

    /**
     * Returns the last valid index in the [TestValueClassList]. This can be `-1` when the list is empty.
     */
    @get:androidx.annotation.IntRange(from = -1)
    public inline val lastIndex: Int get() = list.lastIndex

    /**
     * Returns an [IntRange] of the valid indices for this [TestValueClassList].
     */
    public inline val indices: IntRange get() = list.indices

    /**
     * Returns `true` if the collection has no elements in it.
     */
    public inline fun none(): Boolean = list.none()

    /**
     * Returns `true` if there's at least one element in the collection.
     */
    public inline fun any(): Boolean = list.any()

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate].
     */
    public inline fun any(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.any { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    public inline fun reversedAny(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.reversedAny { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the [TestValueClassList] contains [element] or `false` otherwise.
     */
    public inline operator fun contains(element: TestValueClass): Boolean =
        list.contains(element.value.toLong())

    /**
     * Returns `true` if the [TestValueClassList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public inline fun containsAll(elements: TestValueClassList): Boolean =
        list.containsAll(elements.list)

    /**
     * Returns `true` if the [TestValueClassList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public inline fun containsAll(elements: MutableTestValueClassList): Boolean =
        list.containsAll(elements.list)

    /**
     * Returns the number of elements in this list.
     */
    public inline fun count(): Int = list.count()

    /**
     * Counts the number of elements matching [predicate].
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.count { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the first element in the [TestValueClassList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public inline fun first(): TestValueClass = TestValueClass(list.first().toULong())

    /**
     * Returns the first element in the [TestValueClassList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfFirst
     */
    public inline fun first(predicate: (element: TestValueClass) -> Boolean): TestValueClass {
        contract { callsInPlace(predicate) }
        return TestValueClass(list.first { predicate(TestValueClass(it.toULong())) }.toULong())
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and
     * calculates the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: TestValueClass) -> R): R {
        contract { callsInPlace(operation) }
        return list.fold(initial) { acc, element ->
            operation(acc, TestValueClass(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: TestValueClass) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldIndexed(initial) { index, acc, element ->
            operation(index, acc, TestValueClass(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in reverse order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     * calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: TestValueClass, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        return list.foldRight(initial) { element, acc ->
            operation(TestValueClass(element.toULong()), acc)
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: TestValueClass, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldRightIndexed(initial) { index, element, acc ->
            operation(index, TestValueClass(element.toULong()), acc)
        }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList], in order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEach(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEach { block(TestValueClass(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] along with its index, in order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachIndexed { index, element ->
            block(index, TestValueClass(element.toULong()))
        }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] in reverse order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEachReversed(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversed { block(TestValueClass(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] along with its index, in reverse
     * order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversedIndexed { index, element ->
            block(index, TestValueClass(element.toULong()))
        }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public inline operator fun get(
        @androidx.annotation.IntRange(from = 0) index: Int
    ): TestValueClass = TestValueClass(list[index].toULong())

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public inline fun elementAt(@androidx.annotation.IntRange(from = 0) index: Int): TestValueClass =
        TestValueClass(list[index].toULong())

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds
     * of the collection.
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at
     * an index not in the list.
     */
    public inline fun elementAtOrElse(
        @androidx.annotation.IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> TestValueClass
    ): TestValueClass =
        TestValueClass(list.elementAtOrElse(index) { defaultValue(it).value.toLong() }.toULong())

    /**
     * Returns the index of [element] in the [TestValueClassList] or `-1` if [element] is not there.
     */
    public inline fun indexOf(element: TestValueClass): Int =
        list.indexOf(element.value.toLong())

    /**
     * Returns the index if the first element in the [TestValueClassList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfFirst(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfFirst { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the index if the last element in the [TestValueClassList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfLast(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfLast { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the [TestValueClassList] has no elements in it or `false` otherwise.
     */
    public inline fun isEmpty(): Boolean = list.isEmpty()

    /**
     * Returns `true` if there are elements in the [TestValueClassList] or `false` if it is empty.
     */
    public inline fun isNotEmpty(): Boolean = list.isNotEmpty()

    /**
     * Returns the last element in the [TestValueClassList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public inline fun last(): TestValueClass = TestValueClass(list.last().toULong())

    /**
     * Returns the last element in the [TestValueClassList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfLast
     */
    public inline fun last(predicate: (element: TestValueClass) -> Boolean): TestValueClass {
        contract { callsInPlace(predicate) }
        return TestValueClass(list.last { predicate(TestValueClass(it.toULong())) }.toULong())
    }

    /**
     * Returns the index of the last element in the [TestValueClassList] that is the same as
     * [element] or `-1` if no elements match.
     */
    public inline fun lastIndexOf(element: TestValueClass): Int =
        list.lastIndexOf(element.value.toLong())

    /**
     * Returns a String representation of the list, surrounded by "[]" and each element
     * separated by ", ".
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "[]"
        }
        return buildString {
            append('[')
            forEachIndexed { index: Int, element: TestValueClass ->
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
 * [MutableTestValueClassList] is a [MutableList]-like collection for [TestValueClass] values.
 * It allows storing and retrieving the elements without boxing. Immutable
 * access is available through its base class [TestValueClassList], which has a [List]-like
 * interface.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * @constructor Creates a [MutableTestValueClassList] with a [capacity] of `initialCapacity`.
 */
@OptIn(ExperimentalContracts::class)
@JvmInline
internal value class MutableTestValueClassList(val list: MutableLongList) {
    public constructor(initialCapacity: Int = 16) : this(MutableLongList(initialCapacity))

    /**
     * The number of elements in the [TestValueClassList].
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val size: Int get() = list.size

    /**
     * Returns the last valid index in the [TestValueClassList]. This can be `-1` when the list is empty.
     */
    @get:androidx.annotation.IntRange(from = -1)
    public inline val lastIndex: Int get() = list.lastIndex

    /**
     * Returns an [IntRange] of the valid indices for this [TestValueClassList].
     */
    public inline val indices: IntRange get() = list.indices

    /**
     * Returns `true` if the collection has no elements in it.
     */
    public inline fun none(): Boolean = list.none()

    /**
     * Returns `true` if there's at least one element in the collection.
     */
    public inline fun any(): Boolean = list.any()

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate].
     */
    public inline fun any(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.any { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    public inline fun reversedAny(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return list.reversedAny { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the [TestValueClassList] contains [element] or `false` otherwise.
     */
    public inline operator fun contains(element: TestValueClass): Boolean =
        list.contains(element.value.toLong())

    /**
     * Returns `true` if the [TestValueClassList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public inline fun containsAll(elements: TestValueClassList): Boolean =
        list.containsAll(elements.list)

    /**
     * Returns `true` if the [TestValueClassList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public inline fun containsAll(elements: MutableTestValueClassList): Boolean =
        list.containsAll(elements.list)

    /**
     * Returns the number of elements in this list.
     */
    public inline fun count(): Int = list.count()

    /**
     * Counts the number of elements matching [predicate].
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.count { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the first element in the [TestValueClassList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public inline fun first(): TestValueClass = TestValueClass(list.first().toULong())

    /**
     * Returns the first element in the [TestValueClassList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfFirst
     */
    public inline fun first(predicate: (element: TestValueClass) -> Boolean): TestValueClass {
        contract { callsInPlace(predicate) }
        return TestValueClass(list.first { predicate(TestValueClass(it.toULong())) }.toULong())
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and
     * calculates the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: TestValueClass) -> R): R {
        contract { callsInPlace(operation) }
        return list.fold(initial) { acc, element ->
            operation(acc, TestValueClass(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: TestValueClass) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldIndexed(initial) { index, acc, element ->
            operation(index, acc, TestValueClass(element.toULong()))
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in reverse order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     * calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: TestValueClass, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        return list.foldRight(initial) { element, acc ->
            operation(TestValueClass(element.toULong()), acc)
        }
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [TestValueClassList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: TestValueClass, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        return list.foldRightIndexed(initial) { index, element, acc ->
            operation(index, TestValueClass(element.toULong()), acc)
        }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList], in order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEach(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEach { block(TestValueClass(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] along with its index, in order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachIndexed { index, element ->
            block(index, TestValueClass(element.toULong()))
        }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] in reverse order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEachReversed(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversed { block(TestValueClass(it.toULong())) }
    }

    /**
     * Calls [block] for each element in the [TestValueClassList] along with its index, in reverse
     * order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        list.forEachReversedIndexed { index, element ->
            block(index, TestValueClass(element.toULong()))
        }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public inline operator fun get(
        @androidx.annotation.IntRange(from = 0) index: Int
    ): TestValueClass = TestValueClass(list[index].toULong())

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public inline fun elementAt(@androidx.annotation.IntRange(from = 0) index: Int): TestValueClass =
        TestValueClass(list[index].toULong())

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds
     * of the collection.
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at
     * an index not in the list.
     */
    public inline fun elementAtOrElse(
        @androidx.annotation.IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> TestValueClass
    ): TestValueClass =
        TestValueClass(list.elementAtOrElse(index) { defaultValue(it).value.toLong() }.toULong())

    /**
     * Returns the index of [element] in the [TestValueClassList] or `-1` if [element] is not there.
     */
    public inline fun indexOf(element: TestValueClass): Int =
        list.indexOf(element.value.toLong())

    /**
     * Returns the index if the first element in the [TestValueClassList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfFirst(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfFirst { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the index if the last element in the [TestValueClassList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfLast(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return list.indexOfLast { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the [TestValueClassList] has no elements in it or `false` otherwise.
     */
    public inline fun isEmpty(): Boolean = list.isEmpty()

    /**
     * Returns `true` if there are elements in the [TestValueClassList] or `false` if it is empty.
     */
    public inline fun isNotEmpty(): Boolean = list.isNotEmpty()

    /**
     * Returns the last element in the [TestValueClassList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public inline fun last(): TestValueClass = TestValueClass(list.last().toULong())

    /**
     * Returns the last element in the [TestValueClassList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfLast
     */
    public inline fun last(predicate: (element: TestValueClass) -> Boolean): TestValueClass {
        contract { callsInPlace(predicate) }
        return TestValueClass(list.last { predicate(TestValueClass(it.toULong())) }.toULong())
    }

    /**
     * Returns the index of the last element in the [TestValueClassList] that is the same as
     * [element] or `-1` if no elements match.
     */
    public inline fun lastIndexOf(element: TestValueClass): Int =
        list.lastIndexOf(element.value.toLong())

    /**
     * Returns a String representation of the list, surrounded by "[]" and each element
     * separated by ", ".
     */
    override fun toString(): String = asTestValueClassList().toString()

    /**
     * Returns a read-only interface to the list.
     */
    public inline fun asTestValueClassList(): TestValueClassList = TestValueClassList(list)

    /**
     * Returns the total number of elements that can be held before the [MutableTestValueClassList] must
     * grow.
     *
     * @see ensureCapacity
     */
    public inline val capacity: Int
        get() = list.capacity

    /**
     * Adds [element] to the [MutableTestValueClassList] and returns `true`.
     */
    public inline fun add(element: TestValueClass): Boolean =
        list.add(element.value.toLong())

    /**
     * Adds [element] to the [MutableTestValueClassList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun add(
        @androidx.annotation.IntRange(from = 0) index: Int,
        element: TestValueClass
    ) = list.add(index, element.value.toLong())

    /**
     * Adds all [elements] to the [MutableTestValueClassList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @return `true` if the [MutableTestValueClassList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: TestValueClassList
    ): Boolean = list.addAll(index, elements.list)

    /**
     * Adds all [elements] to the [MutableTestValueClassList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @return `true` if the [MutableTestValueClassList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public inline fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: MutableTestValueClassList
    ): Boolean = list.addAll(index, elements.list)

    /**
     * Adds all [elements] to the end of the [MutableTestValueClassList] and returns `true` if the
     * [MutableTestValueClassList] was changed or `false` if [elements] was empty.
     */
    public inline fun addAll(elements: TestValueClassList): Boolean = list.addAll(elements.list)

    /**
     * Adds all [elements] to the end of the [MutableTestValueClassList].
     */
    public inline operator fun plusAssign(elements: TestValueClassList) =
        list.plusAssign(elements.list)

    /**
     * Adds all [elements] to the end of the [MutableTestValueClassList] and returns `true` if the
     * [MutableTestValueClassList] was changed or `false` if [elements] was empty.
     */
    public inline fun addAll(elements: MutableTestValueClassList): Boolean = list.addAll(elements.list)

    /**
     * Adds all [elements] to the end of the [MutableTestValueClassList].
     */
    public inline operator fun plusAssign(elements: MutableTestValueClassList) =
        list.plusAssign(elements.list)

    /**
     * Removes all elements in the [MutableTestValueClassList]. The storage isn't released.
     * @see trim
     */
    public inline fun clear() = list.clear()

    /**
     * Reduces the internal storage. If [capacity] is greater than [minCapacity] and [size], the
     * internal storage is reduced to the maximum of [size] and [minCapacity].
     * @see ensureCapacity
     */
    public inline fun trim(minCapacity: Int = size) = list.trim(minCapacity)

    /**
     * Ensures that there is enough space to store [capacity] elements in the [MutableTestValueClassList].
     * @see trim
     */
    public inline fun ensureCapacity(capacity: Int) = list.ensureCapacity(capacity)

    /**
     * [add] [element] to the [MutableTestValueClassList].
     */
    public inline operator fun plusAssign(element: TestValueClass) =
        list.plusAssign(element.value.toLong())

    /**
     * [remove] [element] from the [MutableTestValueClassList]
     */
    public inline operator fun minusAssign(element: TestValueClass) =
        list.minusAssign(element.value.toLong())

    /**
     * Removes [element] from the [MutableTestValueClassList]. If [element] was in the [MutableTestValueClassList]
     * and was removed, `true` will be returned, or `false` will be returned if the element
     * was not found.
     */
    public inline fun remove(element: TestValueClass): Boolean =
        list.remove(element.value.toLong())

    /**
     * Removes all [elements] from the [MutableTestValueClassList] and returns `true` if anything was removed.
     */
    public inline fun removeAll(elements: TestValueClassList): Boolean =
        list.removeAll(elements.list)

    /**
     * Removes all [elements] from the [MutableTestValueClassList].
     */
    public inline operator fun minusAssign(elements: TestValueClassList) =
        list.minusAssign(elements.list)

    /**
     * Removes all [elements] from the [MutableTestValueClassList] and returns `true` if anything was removed.
     */
    public inline fun removeAll(elements: MutableTestValueClassList): Boolean =
        list.removeAll(elements.list)

    /**
     * Removes all [elements] from the [MutableTestValueClassList].
     */
    public inline operator fun minusAssign(elements: MutableTestValueClassList) =
        list.minusAssign(elements.list)

    /**
     * Removes the element at the given [index] and returns it.
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public inline fun removeAt(@androidx.annotation.IntRange(from = 0) index: Int): TestValueClass =
        TestValueClass(list.removeAt(index).toULong())

    /**
     * Removes items from index [start] (inclusive) to [end] (exclusive).
     * @throws IndexOutOfBoundsException if [start] or [end] isn't between 0 and [size], inclusive
     * @throws IllegalArgumentException if [start] is greater than [end]
     */
    public inline fun removeRange(
        @androidx.annotation.IntRange(from = 0) start: Int,
        @androidx.annotation.IntRange(from = 0) end: Int
    ) = list.removeRange(start, end)

    /**
     * Keeps only [elements] in the [MutableTestValueClassList] and removes all other values.
     * @return `true` if the [MutableTestValueClassList] has changed.
     */
    public inline fun retainAll(elements: TestValueClassList): Boolean =
        list.retainAll(elements.list)

    /**
     * Keeps only [elements] in the [MutableTestValueClassList] and removes all other values.
     * @return `true` if the [MutableTestValueClassList] has changed.
     */
    public inline fun retainAll(elements: MutableTestValueClassList): Boolean =
        list.retainAll(elements.list)

    /**
     * Sets the value at [index] to [element].
     * @return the previous value set at [index]
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public inline operator fun set(
        @androidx.annotation.IntRange(from = 0) index: Int,
        element: TestValueClass
    ): TestValueClass = TestValueClass(list.set(index, element.value.toLong()).toULong())
}

/**
 * @return a read-only [TestValueClassList] with nothing in it.
 */
internal inline fun emptyTestValueClassList(): TestValueClassList = TestValueClassList(emptyLongList())

/**
 * @return a read-only [TestValueClassList] with nothing in it.
 */
internal inline fun testValueClassListOf(): TestValueClassList = TestValueClassList(emptyLongList())

/**
 * @return a new read-only [TestValueClassList] with [element1] as the only item in the list.
 */
internal inline fun testValueClassListOf(element1: TestValueClass): TestValueClassList =
    TestValueClassList(mutableLongListOf(element1.value.toLong()))

/**
 * @return a new read-only [TestValueClassList] with 2 elements, [element1] and [element2], in order.
 */
internal inline fun testValueClassListOf(element1: TestValueClass, element2: TestValueClass): TestValueClassList =
    TestValueClassList(
        mutableLongListOf(
            element1.value.toLong(),
            element2.value.toLong()
        )
    )

/**
 * @return a new read-only [TestValueClassList] with 3 elements, [element1], [element2], and [element3],
 * in order.
 */
internal inline fun testValueClassListOf(
        element1: TestValueClass,
        element2: TestValueClass,
        element3: TestValueClass
): TestValueClassList = TestValueClassList(
    mutableLongListOf(
        element1.value.toLong(),
        element2.value.toLong(),
        element3.value.toLong()
    )
)

/**
 * @return a new empty [MutableTestValueClassList] with the default capacity.
 */
internal inline fun mutableTestValueClassListOf(): MutableTestValueClassList =
    MutableTestValueClassList(MutableLongList())

/**
 * @return a new [MutableTestValueClassList] with [element1] as the only item in the list.
 */
internal inline fun mutableTestValueClassListOf(element1: TestValueClass): MutableTestValueClassList =
    MutableTestValueClassList(mutableLongListOf(element1.value.toLong()))

/**
 * @return a new [MutableTestValueClassList] with 2 elements, [element1] and [element2], in order.
 */
internal inline fun mutableTestValueClassListOf(
        element1: TestValueClass,
        element2: TestValueClass
    ): MutableTestValueClassList = MutableTestValueClassList(
        mutableLongListOf(
            element1.value.toLong(),
            element2.value.toLong()
        )
    )

/**
 * @return a new [MutableTestValueClassList] with 3 elements, [element1], [element2], and [element3],
 * in order.
 */
internal inline fun mutableTestValueClassListOf(
        element1: TestValueClass,
        element2: TestValueClass,
        element3: TestValueClass
): MutableTestValueClassList = MutableTestValueClassList(
    mutableLongListOf(
        element1.value.toLong(),
        element2.value.toLong(),
        element3.value.toLong()
    )
)
