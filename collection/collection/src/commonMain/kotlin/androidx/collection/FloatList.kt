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

/**
 * [FloatList] is a [List]-like collection for [Float] values. It allows retrieving
 * the elements without boxing. [FloatList] is always backed by a [MutableFloatList],
 * its [MutableList]-like subclass.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 */
public sealed class FloatList constructor(initialCapacity: Int) {
    @JvmField
    @PublishedApi
    internal var content: FloatArray = if (initialCapacity == 0) {
        EmptyFloatArray
    } else {
        FloatArray(initialCapacity)
    }

    @Suppress("PropertyName")
    @JvmField
    @PublishedApi
    internal var _size: Int = 0

    /**
     * The number of elements in the [FloatList].
     */
    public val size: Int
        get() = _size

    /**
     * Returns the last valid index in the [FloatList].
     */
    public inline val lastIndex: Int get() = _size - 1

    /**
     * Returns an [IntRange] of the valid indices for this [FloatList].
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
    public inline fun any(predicate: (Float) -> Boolean): Boolean {
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
    public inline fun reversedAny(predicate: (Float) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEachReversed {
            if (predicate(it)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if the [FloatList] contains [element] or `false` otherwise.
     */
    public operator fun contains(element: Float): Boolean {
        forEach {
            if (it == element) {
                return true
            }
        }
        return false
    }

    /**
     * Returns `true` if the [FloatList] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    public fun containsAll(elements: FloatList): Boolean {
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
    public inline fun count(predicate: (element: Float) -> Boolean): Int {
        var count = 0
        forEach { if (predicate(it)) count++ }
        return count
    }

    /**
     * Returns the first element in the [FloatList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public fun first(): Float {
        if (isEmpty()) {
            throw NoSuchElementException("FloatList is empty.")
        }
        return content[0]
    }

    /**
     * Returns the first element in the [FloatList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     */
    public inline fun first(predicate: (Float) -> Boolean): Float {
        contract { callsInPlace(predicate) }
        forEach { item ->
            if (predicate(item)) return item
        }
        throw NoSuchElementException("FloatList contains no element matching the predicate.")
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [FloatList] in order.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, Float) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEach { item ->
            acc = operation(acc, item)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [FloatList] in order.
     */
    public inline fun <R> foldIndexed(initial: R, operation: (index: Int, acc: R, Float) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachIndexed { i, item ->
            acc = operation(i, acc, item)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [FloatList] in reverse order.
     */
    public inline fun <R> foldRight(initial: R, operation: (Float, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversed { item ->
            acc = operation(item, acc)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [FloatList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, Float, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversedIndexed { i, item ->
            acc = operation(i, item, acc)
        }
        return acc
    }

    /**
     * Calls [block] for each element in the [FloatList], in order.
     */
    public inline fun forEach(block: (Float) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(content[i])
        }
    }

    /**
     * Calls [block] for each element in the [FloatList] along with its index, in order.
     */
    public inline fun forEachIndexed(block: (Int, Float) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(i, content[i])
        }
    }

    /**
     * Calls [block] for each element in the [FloatList] in reverse order.
     */
    public inline fun forEachReversed(block: (Float) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in _size - 1 downTo 0) {
            block(content[i])
        }
    }

    /**
     * Calls [block] for each element in the [FloatList] along with its index, in reverse
     * order.
     */
    public inline fun forEachReversedIndexed(block: (Int, Float) -> Unit) {
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
    public operator fun get(index: Int): Float {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
        }
        return content[index]
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if
     * the [index] is out of bounds of this collection.
     */
    public fun elementAt(index: Int): Float {
        if (index !in 0 until _size) {
            throw IndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
        }
        return content[index]
    }

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds
     * of the collection.
     */
    public fun elementAtOrElse(index: Int, defaultValue: Float): Float {
        if (index !in 0 until _size) {
            return defaultValue
        }
        return content[index]
    }

    /**
     * Returns the index of [element] in the [FloatList] or `-1` if [element] is not there.
     */
    public fun indexOf(element: Float): Int {
        forEachIndexed { i, item ->
            if (element == item) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the first element in the [FloatList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfFirst(predicate: (Float) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachIndexed { i, item ->
            if (predicate(item)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the last element in the [FloatList] for which [predicate]
     * returns `true`.
     */
    public inline fun indexOfLast(predicate: (Float) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachReversedIndexed { i, item ->
            if (predicate(item)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns `true` if the [FloatList] has no elements in it or `false` otherwise.
     */
    public fun isEmpty(): Boolean = _size == 0

    /**
     * Returns `true` if there are elements in the [FloatList] or `false` if it is empty.
     */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the last element in the [FloatList] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    public fun last(): Float {
        if (isEmpty()) {
            throw NoSuchElementException("FloatList is empty.")
        }
        return content[lastIndex]
    }

    /**
     * Returns the last element in the [FloatList] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     */
    public inline fun last(predicate: (Float) -> Boolean): Float {
        contract { callsInPlace(predicate) }
        forEachReversed { item ->
            if (predicate(item)) {
                return item
            }
        }
        throw NoSuchElementException("FloatList contains no element matching the predicate.")
    }

    /**
     * Returns the index of the last element in the [FloatList] that is the same as
     * [element] or `-1` if no elements match.
     */
    public fun lastIndexOf(element: Float): Int {
        forEachReversedIndexed { i, item ->
            if (item == element) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns a hash code based on the contents of the [FloatList].
     */
    override fun hashCode(): Int {
        var hashCode = 0
        forEach { element ->
            hashCode += 31 * element.hashCode()
        }
        return hashCode
    }

    /**
     * Returns `true` if [other] is a [FloatList] and the contents of this and [other] are the
     * same.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is FloatList || other._size != _size) {
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
    override fun toString(): String {
        if (isEmpty()) {
            return "[]"
        }
        val last = lastIndex
        return buildString {
            append('[')
            val content = content
            for (i in 0 until last) {
                append(content[i])
                append(',')
                append(' ')
            }
            append(content[last])
            append(']')
        }
    }
}

/**
 * [MutableFloatList] is a [MutableList]-like collection for [Float] values.
 * It allows storing and retrieving the elements without boxing. Immutable
 * access is available through its base class [FloatList], which has a [List]-like
 * interface.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the list (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. It is also not safe to mutate during reentrancy --
 * in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * @constructor Creates a [MutableFloatList] with a [capacity] of `initialCapacity`.
 */
public class MutableFloatList(
    initialCapacity: Int = DefaultCapacity
) : FloatList(initialCapacity) {
    /**
     * Returns the total number of elements that can be held before the [MutableFloatList] must
     * grow.
     *
     * @see ensureCapacity
     */
    public inline val capacity: Int
        get() = content.size

    /**
     * Adds [element] to the [MutableFloatList] and returns `true`.
     */
    public fun add(element: Float): Boolean {
        ensureCapacity(_size + 1)
        content[_size] = element
        _size++
        return true
    }

    /**
     * Adds [element] to the [MutableFloatList] at the given [index], shifting over any elements
     * that are in the way.
     */
    public fun add(index: Int, element: Float) {
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
     * Adds all [elements] to the [MutableFloatList] at the given [index], shifting over any
     * elements that are in the way.
     * @return `true` if the [MutableFloatList] was changed or `false` if [elements] was empty
     */
    public fun addAll(index: Int, elements: FloatArray): Boolean {
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
     * Adds all [elements] to the [MutableFloatList] at the given [index], shifting over any
     * elements that are in the way.
     * @return `true` if the [MutableFloatList] was changed or `false` if [elements] was empty
     */
    public fun addAll(index: Int, elements: FloatList): Boolean {
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
     * Adds all [elements] to the end of the [MutableFloatList] and returns `true` if the
     * [MutableFloatList] was changed.
     */
    public fun addAll(elements: FloatList): Boolean {
        return addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableFloatList] and returns `true` if the
     * [MutableFloatList] was changed.
     */
    public fun addAll(elements: FloatArray): Boolean {
        return addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableFloatList] and returns `true` if the
     * [MutableFloatList] was changed.
     */
    public operator fun plusAssign(elements: FloatList) {
        addAll(_size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableFloatList] and returns `true` if the
     * [MutableFloatList] was changed.
     */
    public operator fun plusAssign(elements: FloatArray) {
        addAll(_size, elements)
    }

    /**
     * Removes all elements in the [MutableFloatList]. The storage isn't released.
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
     * Ensures that there is enough space to store [capacity] elements in the [MutableFloatList].
     */
    public fun ensureCapacity(capacity: Int) {
        val oldContent = content
        if (oldContent.size < capacity) {
            val newSize = maxOf(capacity, oldContent.size * 3 / 2)
            content = oldContent.copyOf(newSize)
        }
    }

    /**
     * [add] [element] to the [MutableFloatList].
     */
    public inline operator fun plusAssign(element: Float) {
        add(element)
    }

    /**
     * [remove] [element] from the [MutableFloatList]
     */
    public inline operator fun minusAssign(element: Float) {
        remove(element)
    }

    /**
     * Removes [element] from the [MutableFloatList]. If [element] was in the [MutableFloatList]
     * and was removed, `true` will be returned, or `false` will be returned if the element
     * was not found.
     */
    public fun remove(element: Float): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    /**
     * Removes all [elements] from the [MutableFloatList] and returns `true` if anything was removed.
     */
    public fun removeAll(elements: FloatArray): Boolean {
        val initialSize = _size
        for (i in elements.indices) {
            remove(elements[i])
        }
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableFloatList] and returns `true` if anything was removed.
     */
    public fun removeAll(elements: FloatList): Boolean {
        val initialSize = _size
        for (i in 0..elements.lastIndex) {
            remove(elements[i])
        }
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableFloatList].
     */
    public operator fun minusAssign(elements: FloatArray) {
        elements.forEach { element ->
            remove(element)
        }
    }

    /**
     * Removes all [elements] from the [MutableFloatList].
     */
    public operator fun minusAssign(elements: FloatList) {
        elements.forEach { element ->
            remove(element)
        }
    }

    /**
     * Removes the element at the given [index] and returns it.
     */
    public fun removeAt(index: Int): Float {
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
     */
    public fun removeRange(start: Int, end: Int) {
        if (end > start) {
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
     * Keeps only [elements] in the [MutableFloatList] and removes all other values.
     * @return `true` if the [MutableFloatList] has changed.
     */
    public fun retainAll(elements: FloatArray): Boolean {
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
     * Keeps only [elements] in the [MutableFloatList] and removes all other values.
     * @return `true` if the [MutableFloatList] has changed.
     */
    public fun retainAll(elements: FloatList): Boolean {
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
     */
    public operator fun set(index: Int, element: Float): Float {
        val content = content
        val old = content[index]
        content[index] = element
        return old
    }

    /**
     * Sorts the [MutableFloatList] elements in ascending order.
     */
    public fun sort() {
        content.sort(fromIndex = 0, toIndex = _size)
    }

    /**
     * Sorts the [MutableFloatList] elements in descending order.
     */
    public fun sortDescending() {
        content.sortDescending(fromIndex = 0, toIndex = _size)
    }
}

@Suppress("ConstPropertyName")
private const val DefaultCapacity = 16

// Empty array used when nothing is allocated
@Suppress("PrivatePropertyName")
private val EmptyFloatArray = FloatArray(0)

/**
 * Creates and returns an empty [MutableFloatList] with a capacity of 16.
 */
public inline fun mutableFloatListOf(): MutableFloatList = MutableFloatList()

/**
 * Creates and returns a [MutableFloatList] with the given values.
 */
public inline fun mutableFloatListOf(vararg elements: Float): MutableFloatList =
    MutableFloatList(elements.size).also { it.addAll(elements) }
