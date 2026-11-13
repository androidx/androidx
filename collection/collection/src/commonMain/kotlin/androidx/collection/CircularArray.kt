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
import androidx.collection.internal.requirePrecondition
import kotlin.jvm.JvmOverloads

/**
 * CircularArray is a generic circular array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularArray automatically grows its capacity when number of added
 * items is over its capacity.
 *
 * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
 * @constructor Creates a circular array with capacity for at least [minCapacity] elements.
 */
public class CircularArray<E> @JvmOverloads public constructor(minCapacity: Int = 8) {
    private var elements: Array<E?>
    // Unmasked 32-bit wrapping counters: physical slot is `counter and capacityBitmask`
    // and logical size is `tail - head` (always in `0..elements.size`, `elements.size <= 2^30`).
    private var head = 0
    private var tail = 0
    private var capacityBitmask: Int

    init {
        requirePrecondition(minCapacity >= 1) { "capacity must be >= 1" }
        requirePrecondition(minCapacity <= 2 shl 29) { "capacity must be <= 2^30" }

        // If minCapacity isn't a power of 2, round up to the next highest
        // power of 2.
        val arrayCapacity: Int =
            if (minCapacity.countOneBits() != 1) {
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
        val h = head and capacityBitmask
        val r = n - h
        val newCapacity = n shl 1
        if (newCapacity < 0) {
            throw RuntimeException("Max array capacity exceeded")
        }
        @Suppress("UNCHECKED_CAST") val a = arrayOfNulls<Any?>(newCapacity) as Array<E?>
        elements.copyInto(destination = a, destinationOffset = 0, startIndex = h, endIndex = n)
        elements.copyInto(destination = a, destinationOffset = r, startIndex = 0, endIndex = h)
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
        val h = head - 1
        head = h
        val mask = capacityBitmask
        elements[h and mask] = element
        if (tail - h > mask) {
            doubleCapacity()
        }
    }

    /**
     * Add an element at end of the CircularArray.
     *
     * @param element Element to add.
     */
    public fun addLast(element: E) {
        val t = tail
        val mask = capacityBitmask
        elements[t and mask] = element
        tail = t + 1
        if (t - head == mask) {
            doubleCapacity()
        }
    }

    /**
     * Remove first element from front of the [CircularArray] and return it.
     *
     * @return The element removed.
     * @throws [IndexOutOfBoundsException] if [CircularArray] is empty (on jvm)
     */
    public fun popFirst(): E {
        val h = head
        if (h == tail) {
            throw createIndexOutOfBoundsException()
        }
        val index = h and capacityBitmask
        val result = elements[index]
        elements[index] = null
        head = h + 1

        @Suppress("UNCHECKED_CAST")
        return result as E
    }

    /**
     * Remove last element from end of the [CircularArray] and return it.
     *
     * @return The element removed.
     * @throws [IndexOutOfBoundsException] if [CircularArray] is empty
     */
    public fun popLast(): E {
        val t = tail
        if (head == t) {
            throw createIndexOutOfBoundsException()
        }
        val newTail = t - 1
        val index = newTail and capacityBitmask
        val result = elements[index]
        elements[index] = null
        tail = newTail

        @Suppress("UNCHECKED_CAST")
        return result as E
    }

    /** Remove all elements from the [CircularArray]. */
    public fun clear() {
        removeFromStart(size())
    }

    /**
     * Remove multiple elements from front of the [CircularArray], ignore when [count] is less than
     * or equal to 0.
     *
     * @param count Number of elements to remove.
     * @throws [IndexOutOfBoundsException] if [count] is larger than [size]
     */
    public fun removeFromStart(count: Int) {
        if (count <= 0) {
            return
        }
        val h = head
        if (count > tail - h) {
            throw createIndexOutOfBoundsException()
        }
        val start = h and capacityBitmask
        val cap = elements.size
        val firstEnd = minOf(cap, start + count)
        for (i in start until firstEnd) {
            elements[i] = null
        }
        val remaining = count - (firstEnd - start)
        for (i in 0 until remaining) {
            elements[i] = null
        }
        head = h + count
    }

    /**
     * Remove multiple elements from end of the [CircularArray], ignore when [count] is less than or
     * equals to 0.
     *
     * @param count Number of elements to remove.
     * @throws [IndexOutOfBoundsException] if [count] is larger than [size]
     */
    public fun removeFromEnd(count: Int) {
        if (count <= 0) {
            return
        }
        val t = tail
        if (count > t - head) {
            throw createIndexOutOfBoundsException()
        }
        val end = t and capacityBitmask
        val firstStart = maxOf(0, end - count)
        for (i in firstStart until end) {
            elements[i] = null
        }
        val remaining = count - (end - firstStart)
        if (remaining > 0) {
            val cap = elements.size
            for (i in cap - remaining until cap) {
                elements[i] = null
            }
        }
        tail = t - count
    }

    /**
     * Get first element of the [CircularArray].
     *
     * @return The first element.
     * @throws [IndexOutOfBoundsException] if [CircularArray] is empty
     */
    public val first: E
        get() {
            val h = head
            if (h == tail) {
                throw createIndexOutOfBoundsException()
            }
            @Suppress("UNCHECKED_CAST")
            return elements[h and capacityBitmask] as E
        }

    /**
     * Get last element of the [CircularArray].
     *
     * @return The last element.
     * @throws [IndexOutOfBoundsException] if [CircularArray] is empty
     */
    public val last: E
        get() {
            val t = tail
            if (head == t) {
                throw createIndexOutOfBoundsException()
            }
            @Suppress("UNCHECKED_CAST")
            return elements[(t - 1) and capacityBitmask] as E
        }

    /**
     * Get nth (0 <= n <= size()-1) element of the [CircularArray].
     *
     * @param index The zero based element index in the [CircularArray].
     * @return The nth element.
     * @throws [IndexOutOfBoundsException] if n < 0 or n >= size()
     */
    public operator fun get(index: Int): E {
        // Modulo 2^32, `pos - tail == index - (tail - head) == index - size`.
        // Signed `(index or (index - size)) < index` iff `index >= 0` and `index - size < 0`.
        val pos = index + head
        if ((index or (pos - tail)) >= index) {
            throw createIndexOutOfBoundsException()
        }
        @Suppress("UNCHECKED_CAST")
        return elements[pos and capacityBitmask] as E
    }

    /**
     * Get number of elements in the [CircularArray].
     *
     * @return Number of elements in the [CircularArray].
     */
    public fun size(): Int {
        return tail - head
    }

    /**
     * Return `true` if [size] is 0.
     *
     * @return `true` if [size] is 0.
     */
    public fun isEmpty(): Boolean = head == tail
}
