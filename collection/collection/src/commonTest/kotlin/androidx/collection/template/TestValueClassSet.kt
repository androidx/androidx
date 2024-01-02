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
    "UnusedImport"
)

package androidx.collection.template

/* ktlint-disable max-line-length */
/* ktlint-disable import-ordering */

import androidx.collection.LongSet
import androidx.collection.MutableLongSet
import androidx.collection.emptyLongSet
import androidx.collection.mutableLongSetOf
import androidx.collection.TestValueClass
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

/* ktlint-disable max-line-length */
// To use this template, you must substitute several strings. You can copy this and search/replace
// or use a sed command. These properties must be changed:
// * androidx.collection.template - target package (e.g. androidx.compose.ui.ui.collection)
// * androidx.collection - package in which the value class resides
// * TestValueClass - the value class contained in the set (e.g. Color or Offset)
// * testValueClass - the value class, with the first letter lower case (e.g. color or offset)
// * value.toLong() - the field in TestValueClass containing the backing primitive (e.g. packedValue)
// * Long - the primitive type of the backing set (e.g. Long or Float)
// * .toULong() - an operation done on the primitive to convert to the value class parameter
//
// For example, to create a ColorSet:
// sed -e "s/androidx.collection.template/androidx.compose.ui.graphics/" -e "s/TestValueClass/Color/g" \
//     -e "s/testValueClass/color/g" -e "s/value.toLong()/value.toLong()/g" -e "s/Long/Long/g" \
//     -e "s/.toULong()/.toULong()/g" -e "s/androidx.collection/androidx.collection/g" \
//     collection/collection/template/ValueClassSet.kt.template \
//     > compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/ColorSet.kt

/**
 * Returns an empty, read-only [TestValueClassSet].
 */
internal inline fun emptyTestValueClassSet(): TestValueClassSet = TestValueClassSet(emptyLongSet())

/**
 * Returns an empty, read-only [TestValueClassSet].
 */
internal inline fun testValueClassSetOf(): TestValueClassSet = TestValueClassSet(emptyLongSet())

/**
 * Returns a new read-only [TestValueClassSet] with only [element1] in it.
 */
internal inline fun testValueClassSetOf(element1: TestValueClass): TestValueClassSet =
    TestValueClassSet(mutableLongSetOf(element1.value.toLong()))

/**
 * Returns a new read-only [TestValueClassSet] with only [element1] and [element2] in it.
 */
@Suppress("UNCHECKED_CAST")
internal fun testValueClassSetOf(
    element1: TestValueClass,
    element2: TestValueClass
): TestValueClassSet =
    TestValueClassSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
        )
    )

/**
 * Returns a new read-only [TestValueClassSet] with only [element1], [element2], and [element3] in it.
 */
@Suppress("UNCHECKED_CAST")
internal fun testValueClassSetOf(
    element1: TestValueClass,
    element2: TestValueClass,
    element3: TestValueClass
): TestValueClassSet = TestValueClassSet(
    mutableLongSetOf(
        element1.value.toLong(),
        element2.value.toLong(),
        element3.value.toLong(),
    )
)

/**
 * Returns a new [MutableTestValueClassSet].
 */
internal fun mutableTestValueClassSetOf(): MutableTestValueClassSet = MutableTestValueClassSet(
    MutableLongSet()
)

/**
 * Returns a new [MutableTestValueClassSet] with only [element1] in it.
 */
internal fun mutableTestValueClassSetOf(element1: TestValueClass): MutableTestValueClassSet =
    MutableTestValueClassSet(mutableLongSetOf(element1.value.toLong()))

/**
 * Returns a new [MutableTestValueClassSet] with only [element1] and [element2] in it.
 */
internal fun mutableTestValueClassSetOf(
    element1: TestValueClass,
    element2: TestValueClass
): MutableTestValueClassSet =
    MutableTestValueClassSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
        )
    )

/**
 * Returns a new [MutableTestValueClassSet] with only [element1], [element2], and [element3] in it.
 */
internal fun mutableTestValueClassSetOf(
    element1: TestValueClass,
    element2: TestValueClass,
    element3: TestValueClass
): MutableTestValueClassSet =
    MutableTestValueClassSet(
        mutableLongSetOf(
            element1.value.toLong(),
            element2.value.toLong(),
            element3.value.toLong(),
        )
    )

/**
 * [TestValueClassSet] is a container with a [Set]-like interface designed to avoid
 * allocations, including boxing.
 *
 * This implementation makes no guarantee as to the order of the elements,
 * nor does it make guarantees that the order remains constant over time.
 *
 * Though [TestValueClassSet] offers a read-only interface, it is always backed
 * by a [MutableTestValueClassSet]. Read operations alone are thread-safe. However,
 * any mutations done through the backing [MutableTestValueClassSet] while reading
 * on another thread are not safe and the developer must protect the set
 * from such changes during read operations.
 *
 * @see [MutableTestValueClassSet]
 */
@OptIn(ExperimentalContracts::class)
@JvmInline
internal value class TestValueClassSet(val set: LongSet) {
    /**
     * Returns the number of elements that can be stored in this set
     * without requiring internal storage reallocation.
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val capacity: Int
        get() = set.capacity

    /**
     * Returns the number of elements in this set.
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val size: Int
        get() = set.size

    /**
     * Returns `true` if this set has at least one element.
     */
    public inline fun any(): Boolean = set.any()

    /**
     * Returns `true` if this set has no elements.
     */
    public inline fun none(): Boolean = set.none()

    /**
     * Indicates whether this set is empty.
     */
    public inline fun isEmpty(): Boolean = set.isEmpty()

    /**
     * Returns `true` if this set is not empty.
     */
    public inline fun isNotEmpty(): Boolean = set.isNotEmpty()

    /**
     * Returns the first element in the collection.
     * @throws NoSuchElementException if the collection is empty
     */
    public inline fun first(): TestValueClass = TestValueClass(set.first().toULong())

    /**
     * Returns the first element in the collection for which [predicate] returns `true`.
     *
     * **Note** There is no mechanism for both determining if there is an element that matches
     * [predicate] _and_ returning it if it exists. Developers should use [forEach] to achieve
     * this behavior.
     *
     * @param predicate Called on elements of the set, returning `true` for an element that matches
     * or `false` if it doesn't
     * @return An element in the set for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     * collection is empty.
     */
    public inline fun first(predicate: (element: TestValueClass) -> Boolean): TestValueClass =
        TestValueClass(set.first { predicate(TestValueClass(it.toULong())) }.toULong())

    /**
     * Iterates over every element stored in this set by invoking
     * the specified [block] lambda.
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        set.forEach { block(TestValueClass(it.toULong())) }
    }

    /**
     * Returns true if all elements match the given [predicate].
     * @param predicate called for elements in the set to determine if it returns return `true` for
     * all elements.
     */
    public inline fun all(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.all { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     * @param predicate called for elements in the set to determine if it returns `true` for any
     * elements.
     */
    public inline fun any(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.any { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the number of elements in this set.
     */
    @androidx.annotation.IntRange(from = 0)
    public inline fun count(): Int = set.count()

    /**
     * Returns the number of elements matching the given [predicate].
     * @param predicate Called for all elements in the set to count the number for which it returns
     * `true`.
     */
    @androidx.annotation.IntRange(from = 0)
    public inline fun count(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return set.count { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the specified [element] is present in this set, `false`
     * otherwise.
     * @param element The element to look for in this set
     */
    public inline operator fun contains(element: TestValueClass): Boolean =
        set.contains(element.value.toLong())

    /**
     * Returns a string representation of this set. The set is denoted in the
     * string by the `{}`. Each element is separated by `, `.
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
 * [MutableTestValueClassSet] is a container with a [MutableSet]-like interface based on a flat
 * hash table implementation. The underlying implementation is designed to avoid
 * all allocations on insertion, removal, retrieval, and iteration. Allocations
 * may still happen on insertion when the underlying storage needs to grow to
 * accommodate newly added elements to the set.
 *
 * This implementation makes no guarantee as to the order of the elements stored,
 * nor does it make guarantees that the order remains constant over time.
 *
 * This implementation is not thread-safe: if multiple threads access this
 * container concurrently, and one or more threads modify the structure of
 * the set (insertion or removal for instance), the calling code must provide
 * the appropriate synchronization. Concurrent reads are however safe.
 */
@OptIn(ExperimentalContracts::class)
@JvmInline
internal value class MutableTestValueClassSet(val set: MutableLongSet) {
    /**
     * Returns the number of elements that can be stored in this set
     * without requiring internal storage reallocation.
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val capacity: Int
        get() = set.capacity

    /**
     * Returns the number of elements in this set.
     */
    @get:androidx.annotation.IntRange(from = 0)
    public inline val size: Int
        get() = set.size

    /**
     * Returns `true` if this set has at least one element.
     */
    public inline fun any(): Boolean = set.any()

    /**
     * Returns `true` if this set has no elements.
     */
    public inline fun none(): Boolean = set.none()

    /**
     * Indicates whether this set is empty.
     */
    public inline fun isEmpty(): Boolean = set.isEmpty()

    /**
     * Returns `true` if this set is not empty.
     */
    public inline fun isNotEmpty(): Boolean = set.isNotEmpty()

    /**
     * Returns the first element in the collection.
     * @throws NoSuchElementException if the collection is empty
     */
    public inline fun first(): TestValueClass = TestValueClass(set.first().toULong())

    /**
     * Returns the first element in the collection for which [predicate] returns `true`.
     *
     * **Note** There is no mechanism for both determining if there is an element that matches
     * [predicate] _and_ returning it if it exists. Developers should use [forEach] to achieve
     * this behavior.
     *
     * @param predicate Called on elements of the set, returning `true` for an element that matches
     * or `false` if it doesn't
     * @return An element in the set for which [predicate] returns `true`.
     * @throws NoSuchElementException if [predicate] returns `false` for all elements or the
     * collection is empty.
     */
    public inline fun first(predicate: (element: TestValueClass) -> Boolean): TestValueClass =
        TestValueClass(set.first { predicate(TestValueClass(it.toULong())) }.toULong())

    /**
     * Iterates over every element stored in this set by invoking
     * the specified [block] lambda.
     * @param block called with each element in the set
     */
    public inline fun forEach(block: (element: TestValueClass) -> Unit) {
        contract { callsInPlace(block) }
        set.forEach { block(TestValueClass(it.toULong())) }
    }

    /**
     * Returns true if all elements match the given [predicate].
     * @param predicate called for elements in the set to determine if it returns return `true` for
     * all elements.
     */
    public inline fun all(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.all { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns true if at least one element matches the given [predicate].
     * @param predicate called for elements in the set to determine if it returns `true` for any
     * elements.
     */
    public inline fun any(predicate: (element: TestValueClass) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        return set.any { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns the number of elements in this set.
     */
    @androidx.annotation.IntRange(from = 0)
    public inline fun count(): Int = set.count()

    /**
     * Returns the number of elements matching the given [predicate].
     * @param predicate Called for all elements in the set to count the number for which it returns
     * `true`.
     */
    @androidx.annotation.IntRange(from = 0)
    public inline fun count(predicate: (element: TestValueClass) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        return set.count { predicate(TestValueClass(it.toULong())) }
    }

    /**
     * Returns `true` if the specified [element] is present in this set, `false`
     * otherwise.
     * @param element The element to look for in this set
     */
    public inline operator fun contains(element: TestValueClass): Boolean =
        set.contains(element.value.toLong())

    /**
     * Returns a string representation of this set. The set is denoted in the
     * string by the `{}`. Each element is separated by `, `.
     */
    public override fun toString(): String = asTestValueClassSet().toString()

    /**
     * Creates a new [MutableTestValueClassSet]
     * @param initialCapacity The initial desired capacity for this container.
     * The container will honor this value by guaranteeing its internal structures
     * can hold that many elements without requiring any allocations. The initial
     * capacity can be set to 0.
     */
    public constructor(initialCapacity: Int = 6) : this(MutableLongSet(initialCapacity))

    /**
     * Returns a read-only interface to the set.
     */
    public inline fun asTestValueClassSet(): TestValueClassSet = TestValueClassSet(set)

    /**
     * Adds the specified element to the set.
     * @param element The element to add to the set.
     * @return `true` if the element has been added or `false` if the element is already
     * contained within the set.
     */
    public inline fun add(element: TestValueClass): Boolean = set.add(element.value.toLong())

    /**
     * Adds the specified element to the set.
     * @param element The element to add to the set.
     */
    public inline operator fun plusAssign(element: TestValueClass) =
        set.plusAssign(element.value.toLong())

    /**
     * Adds all the elements in the [elements] set into this set.
     * @param elements A [TestValueClassSet] of elements to add to this set.
     * @return `true` if any of the specified elements were added to the collection,
     * `false` if the collection was not modified.
     */
    public inline fun addAll(elements: TestValueClassSet): Boolean = set.addAll(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     * @param elements A [TestValueClassSet] of elements to add to this set.
     * @return `true` if any of the specified elements were added to the collection,
     * `false` if the collection was not modified.
     */
    public inline fun addAll(elements: MutableTestValueClassSet): Boolean = set.addAll(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     * @param elements A [TestValueClassSet] of elements to add to this set.
     */
    public inline operator fun plusAssign(elements: TestValueClassSet) =
        set.plusAssign(elements.set)

    /**
     * Adds all the elements in the [elements] set into this set.
     * @param elements A [TestValueClassSet] of elements to add to this set.
     */
    public inline operator fun plusAssign(elements: MutableTestValueClassSet) =
        set.plusAssign(elements.set)

    /**
     * Removes the specified [element] from the set.
     * @param element The element to remove from the set.
     * @return `true` if the [element] was present in the set, or `false` if it wasn't
     * present before removal.
     */
    public inline fun remove(element: TestValueClass): Boolean = set.remove(element.value.toLong())

    /**
     * Removes the specified [element] from the set if it is present.
     * @param element The element to remove from the set.
     */
    public inline operator fun minusAssign(element: TestValueClass) =
        set.minusAssign(element.value.toLong())

    /**
     * Removes the specified [elements] from the set, if present.
     * @param elements An [TestValueClassSet] of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public inline fun removeAll(elements: TestValueClassSet): Boolean = set.removeAll(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     * @param elements An [TestValueClassSet] of elements to be removed from the set.
     * @return `true` if the set was changed or `false` if none of the elements were present.
     */
    public inline fun removeAll(elements: MutableTestValueClassSet): Boolean =
        set.removeAll(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     * @param elements An [TestValueClassSet] of elements to be removed from the set.
     */
    public inline operator fun minusAssign(elements: TestValueClassSet) =
        set.minusAssign(elements.set)

    /**
     * Removes the specified [elements] from the set, if present.
     * @param elements An [TestValueClassSet] of elements to be removed from the set.
     */
    public inline operator fun minusAssign(elements: MutableTestValueClassSet) =
        set.minusAssign(elements.set)

    /**
     * Removes all elements from this set.
     */
    public inline fun clear() = set.clear()

    /**
     * Trims this [MutableTestValueClassSet]'s storage so it is sized appropriately
     * to hold the current elements.
     *
     * Returns the number of empty elements removed from this set's storage.
     * Returns 0 if no trimming is necessary or possible.
     */
    @androidx.annotation.IntRange(from = 0)
    public inline fun trim(): Int = set.trim()
}
