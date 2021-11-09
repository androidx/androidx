/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.collection

import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * CircularArray is a generic circular array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularArray automatically grows its capacity when number of added
 * items is over its capacity.
 */
class CircularArray<E>
/**
 * Creates a circular array with capacity for at least `minCapacity`
 * elements.
 *
 * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
 */
@JvmOverloads
constructor(minCapacity: Int = 8) {
    private var elements: Array<E?>
    private var head = 0
    private var tail = 0
    private var capacityBitmask: Int

    init {
        require(minCapacity >= 1) { "capacity must be >= 1" }
        require(minCapacity <= 2 shl 29) { "capacity must be <= 2^30" }

        // If minCapacity isn't a power of 2, round up to the next highest power of 2.
        val arrayCapacity = if (minCapacity.countOneBits() != 1) {
            (minCapacity - 1).takeHighestOneBit() shl 1
        } else {
            minCapacity
        }

        capacityBitmask = arrayCapacity - 1
        @Suppress("UNCHECKED_CAST") // We only get/set "E"s and nulls.
        elements = arrayOfNulls<Any>(arrayCapacity) as Array<E?>
    }

    /**
     * Get first element of the CircularArray.
     *
     * @return The first element.
     * @throws [IndexOutOfBoundsException] if CircularArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val first: E
        get() {
            if (head == tail) throw indexOutOfBounds()
            @Suppress("UNCHECKED_CAST") // Guarded by above non-empty conditional.
            return elements[head] as E
        }

    /**
     * Get last element of the CircularArray.
     *
     * @return The last element.
     * @throws [IndexOutOfBoundsException] if CircularArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val last: E
        get() {
            if (head == tail) throw indexOutOfBounds()
            @Suppress("UNCHECKED_CAST") // Guarded by above non-empty conditional.
            return elements[tail - 1 and capacityBitmask] as E
        }

    /** Return true if `size()` is 0. */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun isEmpty(): Boolean = head == tail

    private fun doubleCapacity() {
        val n = elements.size
        val r = n - head
        val newCapacity = n shl 1
        if (newCapacity < 0) {
            throw RuntimeException("Max array capacity exceeded")
        }
        @Suppress("UNCHECKED_CAST") // We only get/set "E"s and nulls.
        val a = arrayOfNulls<Any>(newCapacity) as Array<E?>
        elements.copyInto(a, startIndex = head, endIndex = n)
        elements.copyInto(a, destinationOffset = r, startIndex = 0, endIndex = head)
        elements = a
        head = 0
        tail = n
        capacityBitmask = newCapacity - 1
    }

    /**
     * Add an element in front of the CircularArray.
     *
     * @param e Element to add.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    @JsName("addFirst")
    open fun addFirst(e: E) {
        head = head - 1 and capacityBitmask
        elements[head] = e
        if (head == tail) {
            doubleCapacity()
        }
    }

    /**
     * Add an element at end of the CircularArray.
     *
     * @param e Element to add.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    @JsName("addLast")
    open fun addLast(e: E) {
        elements[tail] = e
        tail = tail + 1 and capacityBitmask
        if (tail == head) {
            doubleCapacity()
        }
    }

    /**
     * Remove first element from front of the CircularArray and return it.
     *
     * @return The element removed.
     * @throws IndexOutOfBoundsException if CircularArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun popFirst(): E {
        if (head == tail) {
            throw indexOutOfBounds()
        }
        @Suppress("UNCHECKED_CAST") // Guarded by above non-empty conditional.
        val result = elements[head] as E
        elements[head] = null
        head = head + 1 and capacityBitmask
        return result
    }

    /**
     * Remove last element from end of the CircularArray and return it.
     *
     * @return The element removed.
     * @throws IndexOutOfBoundsException if CircularArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun popLast(): E {
        if (head == tail) {
            throw indexOutOfBounds()
        }
        val t = tail - 1 and capacityBitmask
        @Suppress("UNCHECKED_CAST") // Guarded by above non-empty conditional.
        val result = elements[t] as E
        elements[t] = null
        tail = t
        return result
    }

    /** Remove all elements from the CircularArray. */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun clear() {
        removeFromStart(size)
    }

    /**
     * Remove multiple elements from front of the CircularArray, ignored when [numOfElements]
     * is less than or equal to 0.
     *
     * @param numOfElements  Number of elements to remove.
     * @throws IndexOutOfBoundsException if [numOfElements] is larger than [size]
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeFromStart(numOfElements: Int) {
        if (numOfElements <= 0) {
            return
        }
        if (numOfElements > size) {
            throw indexOutOfBounds()
        }
        var end = elements.size
        if (numOfElements < end - head) {
            end = head + numOfElements
        }
        for (i in head until end) {
            elements[i] = null
        }
        val removed = end - head
        head = head + removed and capacityBitmask
        val remaining = numOfElements - removed
        if (remaining > 0) {
            // head wrapped to 0
            for (i in 0 until remaining) {
                elements[i] = null
            }
            head = remaining
        }
    }

    /**
     * Remove multiple elements from end of the CircularArray, ignored when [numOfElements]
     * is less than or equal to 0.
     *
     * @param numOfElements  Number of elements to remove.
     * @throws IndexOutOfBoundsException if [numOfElements] is larger than [size]
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeFromEnd(numOfElements: Int) {
        if (numOfElements <= 0) {
            return
        }
        if (numOfElements > size) {
            throw indexOutOfBounds()
        }
        var start = 0
        if (numOfElements < tail) {
            start = tail - numOfElements
        }
        for (i in start until tail) {
            elements[i] = null
        }
        val removed = tail - start
        tail -= removed
        val remaining = numOfElements - removed
        if (remaining > 0) {
            // tail wrapped to elements.length
            tail = elements.size
            val newTail = tail - remaining
            for (i in newTail until tail) {
                elements[i] = null
            }
            tail = newTail
        }
    }

    /**
     * Get nth (0 <= n <= size()-1) element of the CircularArray.
     *
     * @param n  The zero based element index in the CircularArray.
     * @return The nth element.
     * @throws [IndexOutOfBoundsException] if n < 0 or n >= size().
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open operator fun get(n: Int): E {
        if (n < 0 || n >= size) {
            throw indexOutOfBounds()
        }
        @Suppress("UNCHECKED_CAST") // Guarded by above valid range conditional.
        return elements[head + n and capacityBitmask] as E
    }

    /** Get number of elements in the CircularArray. */
    @get:JvmName("size") // For binary compatibility.
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val size: Int get() {
        return tail - head and capacityBitmask
    }
}
