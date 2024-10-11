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
    "NOTHING_TO_INLINE",
    "RedundantVisibilityModifier",
    "UNCHECKED_CAST",
    "KotlinRedundantDiagnosticSuppress"
)
@file:OptIn(ExperimentalContracts::class)

package androidx.collection

import androidx.collection.internal.throwIllegalArgumentException
import androidx.collection.internal.throwIndexOutOfBoundsException
import androidx.collection.internal.throwNoSuchElementException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.max

/**
 * [ObjectList] is a [List]-like collection for reference types. It is optimized for fast access,
 * avoiding virtual and interface method access. Methods avoid allocation whenever possible. For
 * example [forEach] does not need allocate an [Iterator].
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the list (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. It is also not safe to mutate during
 * reentrancy -- in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * **Note** [List] access is available through [asList] when developers need access to the common
 * API.
 *
 * It is best to use this for all internal implementations where a list of reference types is
 * needed. Use [List] in public API to take advantage of the commonly-used interface. It is common
 * to use [ObjectList] internally and use [asList] to get a [List] interface for interacting with
 * public APIs.
 *
 * @see MutableObjectList
 * @see FloatList
 * @see IntList
 * @eee LongList
 */
public sealed class ObjectList<E>(initialCapacity: Int) {
    @JvmField
    @PublishedApi
    internal var content: Array<Any?> =
        if (initialCapacity == 0) {
            EmptyArray
        } else {
            arrayOfNulls(initialCapacity)
        }

    @Suppress("PropertyName") @JvmField @PublishedApi internal var _size: Int = 0

    /** The number of elements in the [ObjectList]. */
    @get:androidx.annotation.IntRange(from = 0)
    public val size: Int
        get() = _size

    /**
     * Returns the last valid index in the [ObjectList]. This can be `-1` when the list is empty.
     */
    @get:androidx.annotation.IntRange(from = -1)
    public inline val lastIndex: Int
        get() = _size - 1

    /** Returns an [IntRange] of the valid indices for this [ObjectList]. */
    public inline val indices: IntRange
        get() = 0 until _size

    /** Returns `true` if the collection has no elements in it. */
    public fun none(): Boolean {
        return isEmpty()
    }

    /** Returns `true` if there's at least one element in the collection. */
    public fun any(): Boolean {
        return isNotEmpty()
    }

    /** Returns `true` if any of the elements give a `true` return value for [predicate]. */
    public inline fun any(predicate: (element: E) -> Boolean): Boolean {
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
    public inline fun reversedAny(predicate: (element: E) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        forEachReversed {
            if (predicate(it)) {
                return true
            }
        }
        return false
    }

    /** Returns `true` if the [ObjectList] contains [element] or `false` otherwise. */
    public operator fun contains(element: E): Boolean {
        return indexOf(element) >= 0
    }

    /**
     * Returns `true` if the [ObjectList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public fun containsAll(@Suppress("ArrayReturn") elements: Array<E>): Boolean {
        for (i in elements.indices) {
            if (!contains(elements[i])) return false
        }
        return true
    }

    /**
     * Returns `true` if the [ObjectList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public fun containsAll(elements: List<E>): Boolean {
        for (i in elements.indices) {
            if (!contains(elements[i])) return false
        }
        return true
    }

    /**
     * Returns `true` if the [ObjectList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public fun containsAll(elements: Iterable<E>): Boolean {
        elements.forEach { element -> if (!contains(element)) return false }
        return true
    }

    /**
     * Returns `true` if the [ObjectList] contains all elements in [elements] or `false` if one or
     * more are missing.
     */
    public fun containsAll(elements: ObjectList<E>): Boolean {
        elements.forEach { element -> if (!contains(element)) return false }
        return true
    }

    /** Returns the number of elements in this list. */
    public fun count(): Int = _size

    /**
     * Counts the number of elements matching [predicate].
     *
     * @return The number of elements in this list for which [predicate] returns true.
     */
    public inline fun count(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        var count = 0
        forEach { if (predicate(it)) count++ }
        return count
    }

    /**
     * Returns the first element in the [ObjectList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public fun first(): E {
        if (isEmpty()) {
            throwNoSuchElementException("ObjectList is empty.")
        }
        return content[0] as E
    }

    /**
     * Returns the first element in the [ObjectList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfFirst
     * @see firstOrNull
     */
    public inline fun first(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEach { element -> if (predicate(element)) return element }
        throw NoSuchElementException("ObjectList contains no element matching the predicate.")
    }

    /** Returns the first element in the [ObjectList] or `null` if it [isEmpty]. */
    public inline fun firstOrNull(): E? = if (isEmpty()) null else get(0)

    /**
     * Returns the first element in the [ObjectList] for which [predicate] returns `true` or `null`
     * if nothing matches.
     *
     * @see indexOfFirst
     */
    public inline fun firstOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEach { element -> if (predicate(element)) return element }
        return null
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ObjectList] in order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes current accumulator value and an element, and calculates
     *   the next accumulator value.
     */
    public inline fun <R> fold(initial: R, operation: (acc: R, element: E) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEach { element -> acc = operation(acc, element) }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ObjectList] in order.
     */
    public inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, element: E) -> R
    ): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachIndexed { i, element -> acc = operation(i, acc, element) }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ObjectList] in reverse order.
     *
     * @param initial The value of `acc` for the first call to [operation] or return value if there
     *   are no elements in this list.
     * @param operation function that takes an element and the current accumulator value, and
     *   calculates the next accumulator value.
     */
    public inline fun <R> foldRight(initial: R, operation: (element: E, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversed { element -> acc = operation(element, acc) }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element in the
     * [ObjectList] in reverse order.
     */
    public inline fun <R> foldRightIndexed(
        initial: R,
        operation: (index: Int, element: E, acc: R) -> R
    ): R {
        contract { callsInPlace(operation) }
        var acc = initial
        forEachReversedIndexed { i, element -> acc = operation(i, element, acc) }
        return acc
    }

    /**
     * Calls [block] for each element in the [ObjectList], in order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEach(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(content[i] as E)
        }
    }

    /**
     * Calls [block] for each element in the [ObjectList] along with its index, in order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachIndexed(block: (index: Int, element: E) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in 0 until _size) {
            block(i, content[i] as E)
        }
    }

    /**
     * Calls [block] for each element in the [ObjectList] in reverse order.
     *
     * @param block will be executed for every element in the list, accepting an element from the
     *   list
     */
    public inline fun forEachReversed(block: (element: E) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in _size - 1 downTo 0) {
            block(content[i] as E)
        }
    }

    /**
     * Calls [block] for each element in the [ObjectList] along with its index, in reverse order.
     *
     * @param block will be executed for every element in the list, accepting the index and the
     *   element at that index.
     */
    public inline fun forEachReversedIndexed(block: (index: Int, element: E) -> Unit) {
        contract { callsInPlace(block) }
        val content = content
        for (i in _size - 1 downTo 0) {
            block(i, content[i] as E)
        }
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public operator fun get(@androidx.annotation.IntRange(from = 0) index: Int): E {
        if (index !in 0 until _size) {
            throwIndexOutOfBoundsExclusiveException(index)
        }
        return content[index] as E
    }

    /**
     * Returns the element at the given [index] or throws [IndexOutOfBoundsException] if the [index]
     * is out of bounds of this collection.
     */
    public fun elementAt(@androidx.annotation.IntRange(from = 0) index: Int): E {
        if (index !in 0 until _size) {
            throwIndexOutOfBoundsExclusiveException(index)
        }
        return content[index] as E
    }

    internal fun throwIndexOutOfBoundsExclusiveException(index: Int) {
        throwIndexOutOfBoundsException("Index $index must be in 0..$lastIndex")
    }

    /**
     * Returns the element at the given [index] or [defaultValue] if [index] is out of bounds of the
     * collection.
     *
     * @param index The index of the element whose value should be returned
     * @param defaultValue A lambda to call with [index] as a parameter to return a value at an
     *   index not in the list.
     */
    public inline fun elementAtOrElse(
        @androidx.annotation.IntRange(from = 0) index: Int,
        defaultValue: (index: Int) -> E
    ): E {
        if (index !in 0 until _size) {
            return defaultValue(index)
        }
        return content[index] as E
    }

    /** Returns the index of [element] in the [ObjectList] or `-1` if [element] is not there. */
    public fun indexOf(element: E): Int {
        // Comparing with == for each element is slower than comparing with .equals().
        // We split the iteration for null and for non-null to speed it up.
        // See ObjectListBenchmarkTest.contains()
        if (element == null) {
            forEachIndexed { i, item ->
                if (item == null) {
                    return i
                }
            }
        } else {
            forEachIndexed { i, item ->
                @Suppress("ReplaceCallWithBinaryOperator")
                if (element.equals(item)) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Returns the index if the first element in the [ObjectList] for which [predicate] returns
     * `true` or -1 if there was no element for which predicate returned `true`.
     */
    public inline fun indexOfFirst(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachIndexed { i, element ->
            if (predicate(element)) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the last element in the [ObjectList] for which [predicate] returns
     * `true` or -1 if there was no element for which predicate returned `true`.
     */
    public inline fun indexOfLast(predicate: (element: E) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        forEachReversedIndexed { i, element ->
            if (predicate(element)) {
                return i
            }
        }
        return -1
    }

    /** Returns `true` if the [ObjectList] has no elements in it or `false` otherwise. */
    public fun isEmpty(): Boolean = _size == 0

    /** Returns `true` if there are elements in the [ObjectList] or `false` if it is empty. */
    public fun isNotEmpty(): Boolean = _size != 0

    /**
     * Returns the last element in the [ObjectList] or throws a [NoSuchElementException] if it
     * [isEmpty].
     */
    public fun last(): E {
        if (isEmpty()) {
            throwNoSuchElementException("ObjectList is empty.")
        }
        return content[lastIndex] as E
    }

    /**
     * Returns the last element in the [ObjectList] for which [predicate] returns `true` or throws
     * [NoSuchElementException] if nothing matches.
     *
     * @see indexOfLast
     * @see lastOrNull
     */
    public inline fun last(predicate: (element: E) -> Boolean): E {
        contract { callsInPlace(predicate) }
        forEachReversed { element ->
            if (predicate(element)) {
                return element
            }
        }
        throw NoSuchElementException("ObjectList contains no element matching the predicate.")
    }

    /** Returns the last element in the [ObjectList] or `null` if it [isEmpty]. */
    public inline fun lastOrNull(): E? = if (isEmpty()) null else content[lastIndex] as E

    /**
     * Returns the last element in the [ObjectList] for which [predicate] returns `true` or `null`
     * if nothing matches.
     *
     * @see indexOfLast
     */
    public inline fun lastOrNull(predicate: (element: E) -> Boolean): E? {
        contract { callsInPlace(predicate) }
        forEachReversed { element ->
            if (predicate(element)) {
                return element
            }
        }
        return null
    }

    /**
     * Returns the index of the last element in the [ObjectList] that is the same as [element] or
     * `-1` if no elements match.
     */
    public fun lastIndexOf(element: E): Int {
        // Comparing with == for each element is slower than comparing with .equals().
        // We split the iteration for null and for non-null to speed it up.
        // See ObjectListBenchmarkTest.contains()
        if (element == null) {
            forEachReversedIndexed { i, item ->
                if (item == null) {
                    return i
                }
            }
        } else {
            forEachReversedIndexed { i, item ->
                @Suppress("ReplaceCallWithBinaryOperator")
                if (element.equals(item)) {
                    return i
                }
            }
        }
        return -1
    }

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
        this@ObjectList.forEachIndexed { index, element ->
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
        }
        append(postfix)
    }

    /**
     * Returns a [List] view into the [ObjectList]. All access to the collection will be less
     * efficient and abides by the allocation requirements of the [List]. For example,
     * [List.forEach] will allocate an iterator. All access will go through the more expensive
     * interface calls. Critical performance areas should use the [ObjectList] API rather than
     * [List] API, when possible.
     */
    public abstract fun asList(): List<E>

    /** Returns a hash code based on the contents of the [ObjectList]. */
    override fun hashCode(): Int {
        var hashCode = 0
        forEach { element -> hashCode += 31 * element.hashCode() }
        return hashCode
    }

    /**
     * Returns `true` if [other] is a [ObjectList] and the contents of this and [other] are the
     * same.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ObjectList<*> || other._size != _size) {
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
     * Returns a String representation of the list, surrounded by "[]" and each element separated by
     * ", ".
     */
    override fun toString(): String =
        joinToString(prefix = "[", postfix = "]") { element ->
            if (element === this) {
                "(this)"
            } else {
                element.toString()
            }
        }
}

/**
 * [MutableObjectList] is a [MutableList]-like collection for reference types. It is optimized for
 * fast access, avoiding virtual and interface method access. Methods avoid allocation whenever
 * possible. For example [forEach] does not need allocate an [Iterator].
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the list (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. It is also not safe to mutate during
 * reentrancy -- in the middle of a [forEach], for example. However, concurrent reads are safe.
 *
 * **Note** [List] access is available through [asList] when developers need access to the common
 * API.
 *
 * **Note** [MutableList] access is available through [asMutableList] when developers need access to
 * the common API.
 *
 * It is best to use this for all internal implementations where a list of reference types is
 * needed. Use [MutableList] in public API to take advantage of the commonly-used interface. It is
 * common to use [MutableObjectList] internally and use [asMutableList] or [asList] to get a
 * [MutableList] or [List] interface for interacting with public APIs.
 *
 * @see ObjectList
 * @see MutableFloatList
 * @see MutableIntList
 * @eee MutableLongList
 */
public class MutableObjectList<E>(initialCapacity: Int = 16) : ObjectList<E>(initialCapacity) {
    private var list: ObjectListMutableList<E>? = null

    /**
     * Returns the total number of elements that can be held before the [MutableObjectList] must
     * grow.
     *
     * @see ensureCapacity
     */
    public inline val capacity: Int
        get() = content.size

    /** Adds [element] to the [MutableObjectList] and returns `true`. */
    public fun add(element: E): Boolean {
        ensureCapacity(_size + 1)
        content[_size] = element
        _size++
        return true
    }

    /**
     * Adds [element] to the [MutableObjectList] at the given [index], shifting over any elements at
     * [index] and after, if any.
     *
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public fun add(@androidx.annotation.IntRange(from = 0) index: Int, element: E) {
        if (index !in 0.._size) {
            throwIndexOutOfBoundsInclusiveException(index)
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
     * Adds all [elements] to the [MutableObjectList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     *
     * @return `true` if the [MutableObjectList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive.
     */
    public fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        @Suppress("ArrayReturn") elements: Array<E>
    ): Boolean {
        if (index !in 0.._size) {
            throwIndexOutOfBoundsInclusiveException(index)
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
     * Adds all [elements] to the [MutableObjectList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     *
     * @return `true` if the [MutableObjectList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive.
     */
    public fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: Collection<E>
    ): Boolean {
        if (index !in 0.._size) {
            throwIndexOutOfBoundsInclusiveException(index)
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
        elements.forEachIndexed { i, element -> content[index + i] = element }
        _size += elements.size
        return true
    }

    /**
     * Adds all [elements] to the [MutableObjectList] at the given [index], shifting over any
     * elements at [index] and after, if any.
     *
     * @return `true` if the [MutableObjectList] was changed or `false` if [elements] was empty
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [size], inclusive
     */
    public fun addAll(
        @androidx.annotation.IntRange(from = 0) index: Int,
        elements: ObjectList<E>
    ): Boolean {
        if (index !in 0.._size) {
            throwIndexOutOfBoundsInclusiveException(index)
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

    private fun throwIndexOutOfBoundsInclusiveException(index: Int) {
        throwIndexOutOfBoundsException("Index $index must be in 0..$_size")
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: ObjectList<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: ScatterSet<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(@Suppress("ArrayReturn") elements: Array<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: List<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: Iterable<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /**
     * Adds all [elements] to the end of the [MutableObjectList] and returns `true` if the
     * [MutableObjectList] was changed or `false` if [elements] was empty.
     */
    public fun addAll(elements: Sequence<E>): Boolean {
        val oldSize = _size
        plusAssign(elements)
        return oldSize != _size
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(elements: ObjectList<E>) {
        if (elements.isEmpty()) return
        ensureCapacity(_size + elements._size)
        val content = content
        elements.content.copyInto(
            destination = content,
            destinationOffset = _size,
            startIndex = 0,
            endIndex = elements._size
        )
        _size += elements._size
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(elements: ScatterSet<E>) {
        if (elements.isEmpty()) return
        ensureCapacity(_size + elements.size)
        elements.forEach { element -> plusAssign(element) }
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<E>) {
        if (elements.isEmpty()) return
        ensureCapacity(_size + elements.size)
        val content = content
        elements.copyInto(content, _size)
        _size += elements.size
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(elements: List<E>) {
        if (elements.isEmpty()) return
        val size = _size
        ensureCapacity(size + elements.size)
        val content = content
        for (i in elements.indices) {
            content[i + size] = elements[i]
        }
        _size += elements.size
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(elements: Iterable<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /** Adds all [elements] to the end of the [MutableObjectList]. */
    public operator fun plusAssign(elements: Sequence<E>) {
        elements.forEach { element -> plusAssign(element) }
    }

    /**
     * Removes all elements in the [MutableObjectList]. The storage isn't released.
     *
     * @see trim
     */
    public fun clear() {
        content.fill(null, fromIndex = 0, toIndex = _size)
        _size = 0
    }

    /**
     * Reduces the internal storage. If [capacity] is greater than [minCapacity] and [size], the
     * internal storage is reduced to the maximum of [size] and [minCapacity].
     *
     * @see ensureCapacity
     */
    public fun trim(minCapacity: Int = _size) {
        val minSize = maxOf(minCapacity, _size)
        if (capacity > minSize) {
            content = content.copyOf(minSize)
        }
    }

    /**
     * Ensures that there is enough space to store [capacity] elements in the [MutableObjectList].
     *
     * @see trim
     */
    public inline fun ensureCapacity(capacity: Int) {
        val oldContent = content
        if (oldContent.size < capacity) {
            resizeStorage(capacity, oldContent)
        }
    }

    @PublishedApi
    internal fun resizeStorage(capacity: Int, oldContent: Array<Any?>) {
        val oldSize = oldContent.size
        val newSize = max(capacity, oldSize * 3 / 2)
        val newContent = arrayOfNulls<Any?>(newSize)
        content = oldContent.copyInto(newContent, 0, 0, oldSize)
    }

    /** [add] [element] to the [MutableObjectList]. */
    public inline operator fun plusAssign(element: E) {
        add(element)
    }

    /** [remove] [element] from the [MutableObjectList] */
    public inline operator fun minusAssign(element: E) {
        remove(element)
    }

    /**
     * Removes [element] from the [MutableObjectList]. If [element] was in the [MutableObjectList]
     * and was removed, `true` will be returned, or `false` will be returned if the element was not
     * found.
     */
    public fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    /** Removes all elements in this list for which [predicate] returns `true`. */
    public inline fun removeIf(predicate: (element: E) -> Boolean) {
        var gap = 0
        val size = _size
        val content = content
        for (i in indices) {
            content[i - gap] = content[i]
            if (predicate(content[i] as E)) {
                gap++
            }
        }
        content.fill(null, fromIndex = size - gap, toIndex = size)
        _size -= gap
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(@Suppress("ArrayReturn") elements: Array<E>): Boolean {
        val initialSize = _size
        for (i in elements.indices) {
            remove(elements[i])
        }
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(elements: ObjectList<E>): Boolean {
        val initialSize = _size
        minusAssign(elements)
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(elements: ScatterSet<E>): Boolean {
        val initialSize = _size
        minusAssign(elements)
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(elements: List<E>): Boolean {
        val initialSize = _size
        minusAssign(elements)
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(elements: Iterable<E>): Boolean {
        val initialSize = _size
        minusAssign(elements)
        return initialSize != _size
    }

    /**
     * Removes all [elements] from the [MutableObjectList] and returns `true` if anything was
     * removed.
     */
    public fun removeAll(elements: Sequence<E>): Boolean {
        val initialSize = _size
        minusAssign(elements)
        return initialSize != _size
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(elements: ObjectList<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(elements: ScatterSet<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(elements: List<E>) {
        for (i in elements.indices) {
            minusAssign(elements[i])
        }
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(elements: Iterable<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /** Removes all [elements] from the [MutableObjectList]. */
    public operator fun minusAssign(elements: Sequence<E>) {
        elements.forEach { element -> minusAssign(element) }
    }

    /**
     * Removes the element at the given [index] and returns it.
     *
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public fun removeAt(@androidx.annotation.IntRange(from = 0) index: Int): E {
        if (index !in 0 until _size) {
            throwIndexOutOfBoundsExclusiveException(index)
        }
        val content = content
        val element = content[index]
        if (index != lastIndex) {
            content.copyInto(
                destination = content,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = _size
            )
        }
        _size--
        content[_size] = null
        return element as E
    }

    /**
     * Removes elements from index [start] (inclusive) to [end] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [start] or [end] isn't between 0 and [size], inclusive
     * @throws IllegalArgumentException if [start] is greater than [end]
     */
    public fun removeRange(
        @androidx.annotation.IntRange(from = 0) start: Int,
        @androidx.annotation.IntRange(from = 0) end: Int
    ) {
        if (start !in 0.._size || end !in 0.._size) {
            throwIndexOutOfBoundsException("Start ($start) and end ($end) must be in 0..$_size")
        }
        if (end < start) {
            throwIllegalArgumentException("Start ($start) is more than end ($end)")
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
            val newSize = _size - (end - start)
            content.fill(null, fromIndex = newSize, toIndex = _size)
            _size = newSize
        }
    }

    /**
     * Keeps only [elements] in the [MutableObjectList] and removes all other values.
     *
     * @return `true` if the [MutableObjectList] has changed.
     */
    public fun retainAll(@Suppress("ArrayReturn") elements: Array<E>): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val element = content[i]
            if (elements.indexOf(element) < 0) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Keeps only [elements] in the [MutableObjectList] and removes all other values.
     *
     * @return `true` if the [MutableObjectList] has changed.
     */
    public fun retainAll(elements: ObjectList<E>): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val element = content[i] as E
            if (element !in elements) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Keeps only [elements] in the [MutableObjectList] and removes all other values.
     *
     * @return `true` if the [MutableObjectList] has changed.
     */
    public fun retainAll(elements: Collection<E>): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val element = content[i] as E
            if (element !in elements) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Keeps only [elements] in the [MutableObjectList] and removes all other values.
     *
     * @return `true` if the [MutableObjectList] has changed.
     */
    public fun retainAll(elements: Iterable<E>): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val element = content[i] as E
            if (element !in elements) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Keeps only [elements] in the [MutableObjectList] and removes all other values.
     *
     * @return `true` if the [MutableObjectList] has changed.
     */
    public fun retainAll(elements: Sequence<E>): Boolean {
        val initialSize = _size
        val content = content
        for (i in lastIndex downTo 0) {
            val element = content[i] as E
            if (element !in elements) {
                removeAt(i)
            }
        }
        return initialSize != _size
    }

    /**
     * Sets the value at [index] to [element].
     *
     * @return the previous value set at [index]
     * @throws IndexOutOfBoundsException if [index] isn't between 0 and [lastIndex], inclusive
     */
    public operator fun set(@androidx.annotation.IntRange(from = 0) index: Int, element: E): E {
        if (index !in 0 until _size) {
            throwIndexOutOfBoundsExclusiveException(index)
        }
        val content = content
        val old = content[index]
        content[index] = element
        return old as E
    }

    override fun asList(): List<E> = asMutableList()

    /**
     * Returns a [MutableList] view into the [MutableObjectList]. All access to the collection will
     * be less efficient and abides by the allocation requirements of the [MutableList]. For
     * example, [MutableList.forEach] will allocate an iterator. All access will go through the more
     * expensive interface calls. Critical performance areas should use the [MutableObjectList] API
     * rather than [MutableList] API, when possible.
     */
    public fun asMutableList(): MutableList<E> =
        list ?: ObjectListMutableList(this).also { list = it }

    private class MutableObjectListIterator<T>(private val list: MutableList<T>, index: Int) :
        MutableListIterator<T> {
        private var prevIndex = index - 1

        override fun hasNext(): Boolean {
            return prevIndex < list.size - 1
        }

        override fun next(): T {
            return list[++prevIndex]
        }

        override fun remove() {
            list.removeAt(prevIndex)
            prevIndex--
        }

        override fun hasPrevious(): Boolean {
            return prevIndex >= 0
        }

        override fun nextIndex(): Int {
            return prevIndex + 1
        }

        override fun previous(): T {
            return list[prevIndex--]
        }

        override fun previousIndex(): Int {
            return prevIndex
        }

        override fun add(element: T) {
            list.add(++prevIndex, element)
        }

        override fun set(element: T) {
            list[prevIndex] = element
        }
    }

    /** [MutableList] implementation for a [MutableObjectList], used in [asMutableList]. */
    private class ObjectListMutableList<T>(private val objectList: MutableObjectList<T>) :
        MutableList<T> {
        override val size: Int
            get() = objectList.size

        override fun contains(element: T): Boolean = objectList.contains(element)

        override fun containsAll(elements: Collection<T>): Boolean =
            objectList.containsAll(elements)

        override fun get(index: Int): T {
            checkIndex(index)
            return objectList[index]
        }

        override fun indexOf(element: T): Int = objectList.indexOf(element)

        override fun isEmpty(): Boolean = objectList.isEmpty()

        override fun iterator(): MutableIterator<T> = MutableObjectListIterator(this, 0)

        override fun lastIndexOf(element: T): Int = objectList.lastIndexOf(element)

        override fun add(element: T): Boolean = objectList.add(element)

        override fun add(index: Int, element: T) = objectList.add(index, element)

        override fun addAll(index: Int, elements: Collection<T>): Boolean =
            objectList.addAll(index, elements)

        override fun addAll(elements: Collection<T>): Boolean = objectList.addAll(elements)

        override fun clear() = objectList.clear()

        override fun listIterator(): MutableListIterator<T> = MutableObjectListIterator(this, 0)

        override fun listIterator(index: Int): MutableListIterator<T> =
            MutableObjectListIterator(this, index)

        override fun remove(element: T): Boolean = objectList.remove(element)

        override fun removeAll(elements: Collection<T>): Boolean = objectList.removeAll(elements)

        override fun removeAt(index: Int): T {
            checkIndex(index)
            return objectList.removeAt(index)
        }

        override fun retainAll(elements: Collection<T>): Boolean = objectList.retainAll(elements)

        override fun set(index: Int, element: T): T {
            checkIndex(index)
            return objectList.set(index, element)
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
            checkSubIndex(fromIndex, toIndex)
            return SubList(this, fromIndex, toIndex)
        }
    }

    /**
     * A view into an underlying [MutableList] that directly accesses the underlying [MutableList].
     * This is important for the implementation of [List.subList]. A change to the [SubList] also
     * changes the referenced [MutableList].
     */
    private class SubList<T>(
        private val list: MutableList<T>,
        private val start: Int,
        private var end: Int
    ) : MutableList<T> {
        override val size: Int
            get() = end - start

        override fun contains(element: T): Boolean {
            for (i in start until end) {
                if (list[i] == element) {
                    return true
                }
            }
            return false
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            elements.forEach {
                if (!contains(it)) {
                    return false
                }
            }
            return true
        }

        override fun get(index: Int): T {
            checkIndex(index)
            return list[index + start]
        }

        override fun indexOf(element: T): Int {
            for (i in start until end) {
                if (list[i] == element) {
                    return i - start
                }
            }
            return -1
        }

        override fun isEmpty(): Boolean = end == start

        override fun iterator(): MutableIterator<T> = MutableObjectListIterator(this, 0)

        override fun lastIndexOf(element: T): Int {
            for (i in end - 1 downTo start) {
                if (list[i] == element) {
                    return i - start
                }
            }
            return -1
        }

        override fun add(element: T): Boolean {
            list.add(end++, element)
            return true
        }

        override fun add(index: Int, element: T) {
            list.add(index + start, element)
            end++
        }

        override fun addAll(index: Int, elements: Collection<T>): Boolean {
            list.addAll(index + start, elements)
            end += elements.size
            return elements.size > 0
        }

        override fun addAll(elements: Collection<T>): Boolean {
            list.addAll(end, elements)
            end += elements.size
            return elements.size > 0
        }

        override fun clear() {
            for (i in end - 1 downTo start) {
                list.removeAt(i)
            }
            end = start
        }

        override fun listIterator(): MutableListIterator<T> = MutableObjectListIterator(this, 0)

        override fun listIterator(index: Int): MutableListIterator<T> =
            MutableObjectListIterator(this, index)

        override fun remove(element: T): Boolean {
            for (i in start until end) {
                if (list[i] == element) {
                    list.removeAt(i)
                    end--
                    return true
                }
            }
            return false
        }

        override fun removeAll(elements: Collection<T>): Boolean {
            val originalEnd = end
            elements.forEach { remove(it) }
            return originalEnd != end
        }

        override fun removeAt(index: Int): T {
            checkIndex(index)
            val element = list.removeAt(index + start)
            end--
            return element
        }

        override fun retainAll(elements: Collection<T>): Boolean {
            val originalEnd = end
            for (i in end - 1 downTo start) {
                val element = list[i]
                if (element !in elements) {
                    list.removeAt(i)
                    end--
                }
            }
            return originalEnd != end
        }

        override fun set(index: Int, element: T): T {
            checkIndex(index)
            return list.set(index + start, element)
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
            checkSubIndex(fromIndex, toIndex)
            return SubList(this, fromIndex, toIndex)
        }
    }
}

private fun List<*>.checkIndex(index: Int) {
    val size = size
    if (index < 0 || index >= size) {
        throwIndexOutOfBoundsException(
            "Index $index is out of bounds. The list has $size elements."
        )
    }
}

private fun List<*>.checkSubIndex(fromIndex: Int, toIndex: Int) {
    val size = size
    if (fromIndex > toIndex) {
        throwIllegalArgumentException(
            "Indices are out of order. fromIndex ($fromIndex) is greater than toIndex ($toIndex)."
        )
    }
    if (fromIndex < 0) {
        throwIndexOutOfBoundsException("fromIndex ($fromIndex) is less than 0.")
    }
    if (toIndex > size) {
        throwIndexOutOfBoundsException("toIndex ($toIndex) is more than than the list size ($size)")
    }
}

// Empty array used when nothing is allocated
private val EmptyArray = arrayOfNulls<Any>(0)

private val EmptyObjectList: ObjectList<Any?> = MutableObjectList(0)

/** @return a read-only [ObjectList] with nothing in it. */
public fun <E> emptyObjectList(): ObjectList<E> = EmptyObjectList as ObjectList<E>

/** @return a read-only [ObjectList] with nothing in it. */
public fun <E> objectListOf(): ObjectList<E> = EmptyObjectList as ObjectList<E>

/** @return a new read-only [ObjectList] with [element1] as the only element in the list. */
public fun <E> objectListOf(element1: E): ObjectList<E> = mutableObjectListOf(element1)

/** @return a new read-only [ObjectList] with 2 elements, [element1] and [element2], in order. */
public fun <E> objectListOf(element1: E, element2: E): ObjectList<E> =
    mutableObjectListOf(element1, element2)

/**
 * @return a new read-only [ObjectList] with 3 elements, [element1], [element2], and [element3], in
 *   order.
 */
public fun <E> objectListOf(element1: E, element2: E, element3: E): ObjectList<E> =
    mutableObjectListOf(element1, element2, element3)

/** @return a new read-only [ObjectList] with [elements] in order. */
public fun <E> objectListOf(vararg elements: E): ObjectList<E> =
    MutableObjectList<E>(elements.size).apply { plusAssign(elements as Array<E>) }

/** @return a new empty [MutableObjectList] with the default capacity. */
public inline fun <E> mutableObjectListOf(): MutableObjectList<E> = MutableObjectList()

/** @return a new [MutableObjectList] with [element1] as the only element in the list. */
public fun <E> mutableObjectListOf(element1: E): MutableObjectList<E> {
    val list = MutableObjectList<E>(1)
    list += element1
    return list
}

/** @return a new [MutableObjectList] with 2 elements, [element1] and [element2], in order. */
public fun <E> mutableObjectListOf(element1: E, element2: E): MutableObjectList<E> {
    val list = MutableObjectList<E>(2)
    list += element1
    list += element2
    return list
}

/**
 * @return a new [MutableObjectList] with 3 elements, [element1], [element2], and [element3], in
 *   order.
 */
public fun <E> mutableObjectListOf(element1: E, element2: E, element3: E): MutableObjectList<E> {
    val list = MutableObjectList<E>(3)
    list += element1
    list += element2
    list += element3
    return list
}

/** @return a new [MutableObjectList] with the given elements, in order. */
public inline fun <E> mutableObjectListOf(vararg elements: E): MutableObjectList<E> =
    MutableObjectList<E>(elements.size).apply { plusAssign(elements as Array<E>) }
