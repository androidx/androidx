/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.collection.CollectionPlatformUtils.createIndexOutOfBoundsException
import kotlin.jvm.JvmOverloads

/**
 * CircularArray is a generic circular array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularArray automatically grows its capacity when number of added
 * items is over its capacity.
 */
public class CircularArray<E>

/**
 * Creates a circular array with capacity for at least [minCapacity] elements.
 *
 * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
 */
@JvmOverloads public constructor(minCapacity: Int = 8) {
    private var elements: Array<E?>
    private var head = 0
    private var tail = 0
    private var capacityBitmask: Int

    init {
        require(minCapacity >= 1) { "capacity must be >= 1" }
        require(minCapacity <= 2 shl 29) { "capacity must be <= 2^30" }

        // If minCapacity isn't a power of 2, round up to the next highest
        // power of 2.
        val arrayCapacity: Int = if (minCapacity.countOneBits() != 1) {
            (minCapacity - 1).takeHighestOneBit() shl 1
        } else {
            minCapacity
        }
        capacityBitmask = arrayCapacity - 1
        @Suppress("UNCHECKED_CAST")
        elements = arrayOfNulls<Any?>(arrayCapacity) as Array<E?>
    }

    private fun doubleCapacity() {
        val n = elements.size
        val r = n - head
        val newCapacity = n shl 1
        if (newCapacity < 0) {
            throw RuntimeException("Max array capacity exceeded")
        }
        @Suppress("UNCHECKED_CAST")
        val a = arrayOfNulls<Any?>(newCapacity) as Array<E?>
        elements.copyInto(destination = a, destinationOffset = 0, startIndex = head, endIndex = n)
        elements.copyInto(destination = a, destinationOffset = r, startIndex = 0, endIndex = head)
        elements = a
        head = 0
        tail = n
        capacityBitmask = newCapacity - 1
    }

    /**
     * Add an element in front of the [CircularArray].
     *
     * @param element Element to add.
     */
    public fun addFirst(element: E) {
        head = (head - 1) and capacityBitmask
        elements[head] = element
        if (head == tail) {
            doubleCapacity()
        }
    }

    /**
     * Add an element at end of the CircularArray.
     *
     * @param element Element to add.
     */
    public fun addLast(element: E) {
        elements[tail] = element
        tail = tail + 1 and capacityBitmask
        if (tail == head) {
            doubleCapacity()
        }
    }

    /**
     * Remove first element from front of the [CircularArray] and return it.
     *
     * @return The element removed.
     * @throws [ArrayIndexOutOfBoundsException] if [CircularArray] is empty (on jvm)
     */
    public fun popFirst(): E {
        if (head == tail) {
            throw createIndexOutOfBoundsException()
        }
        val result = elements[head]
        elements[head] = null
        head = (head + 1) and capacityBitmask

        @Suppress("UNCHECKED_CAST")
        return result as E
    }

    /**
     * Remove last element from end of the [CircularArray] and return it.
     *
     * @return The element removed.
     * @throws [ArrayIndexOutOfBoundsException] if [CircularArray] is empty
     */
    public fun popLast(): E {
        if (head == tail) {
            throw createIndexOutOfBoundsException()
        }
        val t = (tail - 1) and capacityBitmask
        val result = elements[t]
        elements[t] = null
        tail = t

        @Suppress("UNCHECKED_CAST")
        return result as E
    }

    /**
     * Remove all elements from the [CircularArray].
     */
    public fun clear() {
        removeFromStart(size())
    }

    /**
     * Remove multiple elements from front of the [CircularArray], ignore when [count]
     * is less than or equal to 0.
     *
     * @param count Number of elements to remove.
     * @throws [ArrayIndexOutOfBoundsException] if [count] is larger than [size]
     */
    public fun removeFromStart(count: Int) {
        if (count <= 0) {
            return
        }
        if (count > size()) {
            throw createIndexOutOfBoundsException()
        }

        var numOfElements = count
        var end = elements.size
        if (numOfElements < end - head) {
            end = head + numOfElements
        }
        for (i in head until end) {
            elements[i] = null
        }
        val removed = end - head
        numOfElements -= removed
        head = head + removed and capacityBitmask
        if (numOfElements > 0) {
            // head wrapped to 0
            for (i in 0 until numOfElements) {
                elements[i] = null
            }
            head = numOfElements
        }
    }

    /**
     * Remove multiple elements from end of the [CircularArray], ignore when [count]
     * is less than or equals to 0.
     *
     * @param count Number of elements to remove.
     * @throws [ArrayIndexOutOfBoundsException] if [count] is larger than [size]
     */
    public fun removeFromEnd(count: Int) {
        if (count <= 0) {
            return
        }
        if (count > size()) {
            throw createIndexOutOfBoundsException()
        }

        var numOfElements = count
        var start = 0
        if (numOfElements < tail) {
            start = tail - numOfElements
        }
        for (i in start until tail) {
            elements[i] = null
        }
        val removed = tail - start
        numOfElements -= removed
        tail -= removed
        if (numOfElements > 0) {
            // tail wrapped to elements.length
            tail = elements.size
            val newTail = tail - numOfElements
            for (i in newTail until tail) {
                elements[i] = null
            }
            tail = newTail
        }
    }

    /**
     * Get first element of the [CircularArray].
     *
     * @return The first element.
     * @throws [ArrayIndexOutOfBoundsException] if [CircularArray] is empty
     */
    public val first: E
        get() {
            if (head == tail) {
                throw createIndexOutOfBoundsException()
            }
            return elements[head]!!
        }

    /**
     * Get last element of the [CircularArray].
     *
     * @return The last element.
     * @throws [ArrayIndexOutOfBoundsException] if [CircularArray] is empty
     */
    public val last: E
        get() {
            if (head == tail) {
                throw createIndexOutOfBoundsException()
            }
            return elements[tail - 1 and capacityBitmask]!!
        }

    /**
     * Get nth (0 <= n <= size()-1) element of the [CircularArray].
     *
     * @param index The zero based element index in the [CircularArray].
     * @return The nth element.
     * @throws [ArrayIndexOutOfBoundsException] if n < 0 or n >= size()
     */
    public operator fun get(index: Int): E {
        if (index < 0 || index >= size()) {
            throw createIndexOutOfBoundsException()
        }
        return elements[(head + index) and capacityBitmask]!!
    }

    /**
     * Get number of elements in the [CircularArray].
     *
     * @return Number of elements in the [CircularArray].
     */
    public fun size(): Int {
        return (tail - head) and capacityBitmask
    }

    /**
     * Return `true` if [size] is 0.
     *
     * @return `true` if [size] is 0.
     */
    public fun isEmpty(): Boolean = head == tail
}