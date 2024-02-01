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
@file:Suppress("NOTHING_TO_INLINE", "RedundantVisibilityModifier")
@file:OptIn(ExperimentalContracts::class)

package androidx.collection

import kotlin.contracts.ExperimentalContracts
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

/**
 * [DoubleList] is a [List]-like collection for [Double] values. It allows retrieving
 * the elements without boxing. [DoubleList] is always backed by a [MutableDoubleList],
 * its [MutableList]-like subclass.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 */
public sealed class DoubleList(initialCapacity: Int) {
    @JvmField
    @PublishedApi
    internal var content: DoubleArray = if (initialCapacity == 0) {
        EmptyDoubleArray
    } else {
        DoubleArray(initialCapacity)
    }

    @Suppress("PropertyName")
    @JvmField
    @PublishedApi
    internal var _size: Int = 0

    /**
     * The number of elements in the [DoubleList].
     */
    @get:androidx.annotation.IntRange(from = 0)
    public val size: Int
        get() = _size

    /**
     * Returns the last valid index in the [DoubleList]. This can be `-1` when the list is empty.
     */
    @get:androidx.annotation.IntRange(from = -1)
    public inline val lastIndex: Int get() = _size - 1

    /**
     * Returns an [IntRange] of the valid indices for this [DoubleList].
     */
    public inline val indices: IntRange get() = 0 until _size

    /**
     * Returns `true` if the collection has no elements in it.
     */
    public fun none(): Boolean {
        return isEmpty()
    }

    /**
     * Returns `true` if there's at least one element in the collection.
     */
    public fun any(): Boolean {
        return isNotEmpty()
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate].
     */
    public inline fun any(predicate: (element: Double) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEach {
            if (predicate(it)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    public inline fun reversedAny(predicate: (element: Double) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEachReversed {
            if (predicate(it)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if the [DoubleList] contains [element] or `false` otherwise.
     */
    public operator fun contains(element: Double): Boolean {
        forEach {
            if (it == element) {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if the [DoubleList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public fun containsAll(elements: DoubleList): Boolean {
        for (i in elements.indices) {
            if (!contains(elements[i])) return false
        }
        return true
    }

    /**
     * Returns the number of elements in this list.
     */
    public fun count(): Int = _size

    /**
     * Counts the number of elements matching [predicate].
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: Double) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        forEach { if (predicate(it)) count++ }
        return count
    }

    /**
     * Returns the first element in the [DoubleList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public fun first(): Double {
        if (isEmpty()) {
            throw NoSuchElementException("DoubleList is empty.")
        }
        return content[0]
    }

    /**
     * Returns the first element in the [DoubleList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfFirst
     */
    public inline fun first(predicate: (element: Double) -> Boolean): Double {
        contract { callsInPlace(predicate) }
        forEach { item ->
            if (predicate(item)) return item
        }
        throw NoSuchElementException("DoubleList contains no element matching the predicate.")
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [DoubleList] in order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and
     * calculates the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: Double) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEach { item ->
            acc = operation(acc, item)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [DoubleList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: Double) -> R
    ): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachIndexed { i, item ->
            acc = operation(i, acc, item)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [DoubleList] in reverse order.
     * @param initial The value of `acc` for the first call to [operation] or return value if
     * there are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     * calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: Double, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversed { item ->
            acc = operation(item, acc)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [DoubleList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: Double, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversedIndexed { i, item ->
            acc = operation(i, item, acc)
        }
        return acc
    }

    /**
     * Calls [block] for each element in the [DoubleList], in order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEach(block: (element: Double) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(content[i])
        }
    }

    /**
     * Calls [block] for each element in the [DoubleList] along with its index, in order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: Double) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(i, content[i])
        }
    }

    /**
     * Calls [block] for each element in the [DoubleList] in reverse order.
     * @param block will be executed for every element in the list, accepting an element from
     * the list
     */
    public inline fun forEachReversed(block: (element: Double) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in _size - 1 downTo 0) {
            block(content[i])
        }
    }

    /**
     * Calls [block] for each element in the [DoubleList] along with its index, in reverse
     * order.
     * @param block will be executed for every element in the list, accepting the index and
     * the element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: Double) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in _size - 1 downTo 0) {
            block(i, content[i])
        }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public operator fun get(@androidx.annotation.IntRange(from = 0) index: Int): Double {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
        }
        return content[index]
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public fun elementAt(@androidx.annotation.IntRange(from = 0) index: Int): Double {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
        }
        return content[index]
    }

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds
     * of the collection.
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at
     * an index not in the list.
     */
    public inline fun elementAtOrElse(
        @androidx.annotation.IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> Double
    ): Double {
        if (index !in 0 until _size) {
            return defaultValue(index)
        }
        return content[index]
    }

    /**
     * Returns the index of [element] in the [DoubleList] or `-1` if [element] is not there.
     */
    public fun indexOf(element: Double): Int {
        forEachIndexed { i, item ->
            if (element == item) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the first element in the [DoubleList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfFirst(predicate: (element: Double) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachIndexed { i, item ->
            if (predicate(item)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the last element in the [DoubleList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfLast(predicate: (element: Double) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachReversedIndexed { i, item ->
            if (predicate(item)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns `true` if the [DoubleList] has no elements in it or `false` otherwise.
     */
    public fun isEmpty(): Boolean = _size == 0

    /**
     * Returns `true` if there are elements in the [DoubleList] or `false` if it is empty.
     */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the last element in the [DoubleList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public fun last(): Double {
        if (isEmpty()) {
            throw NoSuchElementException("DoubleList is empty.")
        }
        return content[lastIndex]
    }

    /**
     * Returns the last element in the [DoubleList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     * @see indexOfLast
     */
    public inline fun last(predicate: (element: Double) -> Boolean): Double {
        contract { callsInPlace(predicate) }
        forEachReversed { item ->
            if (predicate(item)) {
                return item
            }
        }
        throw NoSuchElementException("DoubleList contains no element matching the predicate.")
    }

    /**
     * Returns the index of the last element in the [DoubleList] that is the same as
     * [element] or `-1` if no elements match.
     */
    public fun lastIndexOf(element: Double): Int {
        forEachReversedIndexed { i, item ->
            if (item == element) {
                return i
            }
        }
        return -1
    }

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before
     * and [postfix] after, if supplied.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used
     * to generate the string. If the collection holds more than [limit] items, the string
     * is terminated with [truncated].
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
        this@DoubleList.forEachIndexed { index, element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            append(element)
        }
        append(postfix)
    }

    /**
     * Creates a String from the elements separated by [separator] and using [prefix] before
     * and [postfix] after, if supplied. [transform] dictates how each element will be represented.
     *
     * When a non-negative value of [limit] is provided, a maximum of [limit] items are used
     * to generate the string. If the collection holds more than [limit] items, the string
     * is terminated with [truncated].
     */
    @JvmOverloads
    public inline fun joinToString(
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "", // I know this should be suffix, but this is kotlin's name
        limit: Int = -1,
        truncated: CharSequence = "...",
        crossinline transform: (Double) -> CharSequence
    ): String = buildString {
        append(prefix)
        this@DoubleList.forEachIndexed { index, element ->
            if (index == limit) {
                append(truncated)
                return@buildString
            }
            if (index != 0) {
                append(separator)
            }
            append(transform(element))
        }
        append(postfix)
    }

    /**
     * Returns a hash code based on the contents of the [DoubleList].
     */
    override fun hashCode(): Int {
        var hashCode = 0
        forEach { element ->
            hashCode += 31 * element.hashCode()
        }
        return hashCode
    }

    /**
     * Returns `true` if [other] is a [DoubleList] and the contents of this and [other] are the
     * same.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is DoubleList || other._size != _size) {
            return false
        }
        val content = content
        val otherContent = other.content
        for (i in indices) {
            if (content[i] != otherContent[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Returns a String representation of the list, surrounded by "[]" and each element
     * separated by ", ".
     */
    override fun toString(): String = joinToString(prefix = "[", postfix = "]")
}

/**
 * [MutableDoubleList] is a [MutableList]-like collection for [Double] values.
 * It allows storing and retrieving the elements without boxing. Immutable
 * access is available through its base class [DoubleList], which has a [List]-like
 * interface.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * @constructor Creates a [MutableDoubleList] with a [capacity] of `initialCapacity`.
 */
public class MutableDoubleList(
    initialCapacity: Int = 16
) : DoubleList(initialCapacity) {
    /**
     * Returns the total number of elements that can be held before the [MutableDoubleList] must
     * grow.
     *
     * @see ensureCapacity
     */
    public inline val capacity: Int
        get() = content.size

    /**
     * Adds [element] to the [MutableDoubleList] and returns `true`.
     */
    public fun add(element: Double): Boolean {
        ensureCapacity(_size + 1)
        content[_size] = element
        _size++
        return true
    }

    /**
     * Adds [element] to the [MutableDoubleList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public fun add(@androidx.annotation.IntRange(from = 0) index: Int, element: Double) {
        if (index !in 0.._size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$_size")
        }
        ensureCapacity(_size + 1)
        val content = content
        if (index != _size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = _size
            )
        }
        content[index] = element
        _size++
    }

    /**
     * Adds all [elements] to the [MutableDoubleList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @return `true` if the [MutableDoubleList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive.
     */
    public fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: DoubleArray
    ): Boolean {
        if (index !in 0.._size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$_size")
        }
        if (elements.isEmpty()) return false
        ensureCapacity(_size + elements.size)
        val content = content
        if (index != _size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + elements.size,
                startIndex = index,
                endIndex = _size
            )
        }
        elements.copyInto(content, index)
        _size += elements.size
        return true
    }

    /**
     * Adds all [elements] to the [MutableDoubleList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     * @return `true` if the [MutableDoubleList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: DoubleList
    ): Boolean {
        if (index !in 0.._size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$_size")
        }
        if (elements.isEmpty()) return false
        ensureCapacity(_size + elements._size)
        val content = content
        if (index != _size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + elements._size,
                startIndex = index,
                endIndex = _size
            )
        }
        elements.content.copyInto(
            destination = content,
            destinationOffset = index,
            startIndex = 0,
            endIndex = elements._size
        )
        _size += elements._size
        return true
    }

    /**
     * Adds all [elements] to the end of the [MutableDoubleList] and returns `true` if the
     * [MutableDoubleList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: DoubleList): Boolean {
        return addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableDoubleList] and returns `true` if the
     * [MutableDoubleList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: DoubleArray): Boolean {
        return addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableDoubleList].
     */
    public operator fun plusAssign(elements: DoubleList) {
        addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableDoubleList].
     */
    public operator fun plusAssign(elements: DoubleArray) {
        addAll(_size, elements)
    }

    /**
     * Removes all elements in the [MutableDoubleList]. The storage isn't released.
     * @see trim
     */
    public fun clear() {
        _size = 0
    }

    /**
     * Reduces the internal storage. If [capacity] is greater than [minCapacity] and [size], the
     * internal storage is reduced to the maximum of [size] and [minCapacity].
     * @see ensureCapacity
     */
    public fun trim(minCapacity: Int = _size) {
        val minSize = maxOf(minCapacity, _size)
        if (capacity > minSize) {
            content = content.copyOf(minSize)
        }
    }

    /**
     * Ensures that there is enough space to store [capacity] elements in the [MutableDoubleList].
     * @see trim
     */
    public fun ensureCapacity(capacity: Int) {
        val oldContent = content
        if (oldContent.size < capacity) {
            val newSize = maxOf(capacity, oldContent.size * 3 / 2)
            content = oldContent.copyOf(newSize)
        }
    }

    /**
     * [add] [element] to the [MutableDoubleList].
     */
    public inline operator fun plusAssign(element: Double) {
        add(element)
    }

    /**
     * [remove] [element] from the [MutableDoubleList]
     */
    public inline operator fun minusAssign(element: Double) {
        remove(element)
    }

    /**
     * Removes [element] from the [MutableDoubleList]. If [element] was in the [MutableDoubleList]
     * and was removed, `true` will be returned, or `false` will be returned if the element
     * was not found.
     */
    public fun remove(element: Double): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    /**
     * Removes all [elements] from the [MutableDoubleList] and returns `true` if anything was removed.
     */
    public fun removeAll(elements: DoubleArray): Boolean {
        val initialSize = _size
        for (i in elements.indices) {
            remove(elements[i])
        }
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableDoubleList] and returns `true` if anything was removed.
     */
    public fun removeAll(elements: DoubleList): Boolean {
        val initialSize = _size
        for (i in 0..elements.lastIndex) {
            remove(elements[i])
        }
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableDoubleList].
     */
    public operator fun minusAssign(elements: DoubleArray) {
        elements.forEach { element ->
            remove(element)
        }
    }

    /**
     * Removes all [elements] from the [MutableDoubleList].
     */
    public operator fun minusAssign(elements: DoubleList) {
        elements.forEach { element ->
            remove(element)
        }
    }

    /**
     * Removes the element at the given [index] and returns it.
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public fun removeAt(@androidx.annotation.IntRange(from = 0) index: Int): Double {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
        }
        val content = content
        val item = content[index]
        if (index != lastIndex) {
            content.copyInto(
                destination = content,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = _size
            )
        }
        _size--
        return item
    }

    /**
     * Removes items from index [start] (inclusive) to [end] (exclusive).
     * @throws IndexOutOfBoundsException if [start] or [end] isn't between 0 and [size], inclusive
     * @throws IllegalArgumentException if [start] is greater than [end]
     */
    public fun removeRange(
        @androidx.annotation.IntRange(from = 0) start: Int,
        @androidx.annotation.IntRange(from = 0) end: Int
    ) {
        if (start !in 0.._size || end !in 0.._size) {
            throw IndexOutOfBoundsException("Start ($start) and end ($end) must be in 0..$_size")
        }
        if (end < start) {
            throw IllegalArgumentException("Start ($start) is more than end ($end)")
        }
        if (end != start) {
            if (end < _size) {
                content.copyInto(
                    destination = content,
                    destinationOffset = start,
                    startIndex = end,
                    endIndex = _size
                )
            }
            _size -= (end - start)
        }
    }

    /**
     * Keeps only [elements] in the [MutableDoubleList] and removes all other values.
     * @return `true` if the [MutableDoubleList] has changed.
     */
    public fun retainAll(elements: DoubleArray): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val item = content[i]
            if (elements.indexOfFirst { it == item } < 0) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Keeps only [elements] in the [MutableDoubleList] and removes all other values.
     * @return `true` if the [MutableDoubleList] has changed.
     */
    public fun retainAll(elements: DoubleList): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val item = content[i]
            if (item !in elements) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Sets the value at [index] to [element].
     * @return the previous value set at [index]
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public operator fun set(
        @androidx.annotation.IntRange(from = 0) index: Int,
        element: Double
    ): Double {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("set index $index must be between 0 .. $lastIndex")
        }
        val content = content
        val old = content[index]
        content[index] = element
        return old
    }

    /**
     * Sorts the [MutableDoubleList] elements in ascending order.
     */
    public fun sort() {
        content.sort(fromIndex = 0, toIndex = _size)
    }

    /**
     * Sorts the [MutableDoubleList] elements in descending order.
     */
    public fun sortDescending() {
        content.sortDescending(fromIndex = 0, toIndex = _size)
    }
}

private val EmptyDoubleList: DoubleList = MutableDoubleList(0)

/**
 * @return a read-only [DoubleList] with nothing in it.
 */
public fun emptyDoubleList(): DoubleList = EmptyDoubleList

/**
 * @return a read-only [DoubleList] with nothing in it.
 */
public fun doubleListOf(): DoubleList = EmptyDoubleList

/**
 * @return a new read-only [DoubleList] with [element1] as the only item in the list.
 */
public fun doubleListOf(element1: Double): DoubleList = mutableDoubleListOf(element1)

/**
 * @return a new read-only [DoubleList] with 2 elements, [element1] and [element2], in order.
 */
public fun doubleListOf(element1: Double, element2: Double): DoubleList =
    mutableDoubleListOf(element1, element2)

/**
 * @return a new read-only [DoubleList] with 3 elements, [element1], [element2], and [element3],
 * in order.
 */
public fun doubleListOf(element1: Double, element2: Double, element3: Double): DoubleList =
    mutableDoubleListOf(element1, element2, element3)

/**
 * @return a new read-only [DoubleList] with [elements] in order.
 */
public fun doubleListOf(vararg elements: Double): DoubleList =
    MutableDoubleList(elements.size).apply { plusAssign(elements) }

/**
 * @return a new empty [MutableDoubleList] with the default capacity.
 */
public inline fun mutableDoubleListOf(): MutableDoubleList = MutableDoubleList()

/**
 * @return a new [MutableDoubleList] with [element1] as the only item in the list.
 */
public fun mutableDoubleListOf(element1: Double): MutableDoubleList {
    val list = MutableDoubleList(1)
    list += element1
    return list
}

/**
 * @return a new [MutableDoubleList] with 2 elements, [element1] and [element2], in order.
 */
public fun mutableDoubleListOf(element1: Double, element2: Double): MutableDoubleList {
    val list = MutableDoubleList(2)
    list += element1
    list += element2
    return list
}

/**
 * @return a new [MutableDoubleList] with 3 elements, [element1], [element2], and [element3],
 * in order.
 */
public fun mutableDoubleListOf(
    element1: Double,
    element2: Double,
    element3: Double
): MutableDoubleList {
    val list = MutableDoubleList(3)
    list += element1
    list += element2
    list += element3
    return list
}

/**
 * @return a new [MutableDoubleList] with the given elements, in order.
 */
public inline fun mutableDoubleListOf(vararg elements: Double): MutableDoubleList =
    MutableDoubleList(elements.size).apply { plusAssign(elements) }
