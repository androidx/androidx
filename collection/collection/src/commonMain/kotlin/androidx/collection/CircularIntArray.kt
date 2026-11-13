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
 * [CircularIntArray] is a circular integer array data structure that provides O(1) random read,
 * O(1) prepend and O(1) append. The CircularIntArray automatically grows its capacity when number
 * of added integers is over its capacity.
 *
 * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
 * @constructor Creates a circular array with capacity for at least [minCapacity] elements.
 */
public class CircularIntArray @JvmOverloads public constructor(minCapacity: Int = 8) {
    private var elements: IntArray
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
        elements = IntArray(arrayCapacity)
    }

    private fun doubleCapacity() {
        val n = elements.size
        val h = head and capacityBitmask
        val r = n - h
        val newCapacity = n shl 1
        if (newCapacity < 0) {
            throw RuntimeException("Max array capacity exceeded")
        }
        val a = IntArray(newCapacity)
        elements.copyInto(destination = a, destinationOffset = 0, startIndex = h, endIndex = n)
        elements.copyInto(destination = a, destinationOffset = r, startIndex = 0, endIndex = h)
        elements = a
        head = 0
        tail = n
        capacityBitmask = newCapacity - 1
    }

    /**
     * Add an integer in front of the [CircularIntArray].
     *
     * @param element [Int] to add.
     */
    public fun addFirst(element: Int) {
        val h = head - 1
        head = h
        val mask = capacityBitmask
        elements[h and mask] = element
        if (tail - h > mask) {
            doubleCapacity()
        }
    }

    /**
     * Add an integer at end of the [CircularIntArray].
     *
     * @param element [Int] to add.
     */
    public fun addLast(element: Int) {
        val t = tail
        val mask = capacityBitmask
        elements[t and mask] = element
        tail = t + 1
        if (t - head == mask) {
            doubleCapacity()
        }
    }

    /**
     * Remove first integer from front of the [CircularIntArray] and return it.
     *
     * @return The integer removed.
     * @throws IndexOutOfBoundsException if [CircularIntArray] is empty.
     */
    public fun popFirst(): Int {
        val h = head
        if (h == tail) throw createIndexOutOfBoundsException()
        val result = elements[h and capacityBitmask]
        head = h + 1
        return result
    }

    /**
     * Remove last integer from end of the [CircularIntArray] and return it.
     *
     * @return The integer removed.
     * @throws IndexOutOfBoundsException if [CircularIntArray] is empty.
     */
    public fun popLast(): Int {
        val t = tail
        if (head == t) throw createIndexOutOfBoundsException()
        val newTail = t - 1
        val result = elements[newTail and capacityBitmask]
        tail = newTail
        return result
    }

    /** Remove all integers from the [CircularIntArray]. */
    public fun clear() {
        tail = head
    }

    /**
     * Remove multiple integers from front of the [CircularIntArray], ignore when [count] is less
     * than or equals to 0.
     *
     * @param count Number of integers to remove.
     * @throws IndexOutOfBoundsException if numOfElements is larger than [size]
     */
    public fun removeFromStart(count: Int) {
        if (count <= 0) {
            return
        }
        val h = head
        if (count > tail - h) {
            throw createIndexOutOfBoundsException()
        }
        head = h + count
    }

    /**
     * Remove multiple elements from end of the [CircularIntArray], ignore when [count] is less than
     * or equals to 0.
     *
     * @param count Number of integers to remove.
     * @throws IndexOutOfBoundsException if [count] is larger than [size]
     */
    public fun removeFromEnd(count: Int) {
        if (count <= 0) {
            return
        }
        val t = tail
        if (count > t - head) {
            throw createIndexOutOfBoundsException()
        }
        tail = t - count
    }

    /**
     * Get first integer of the [CircularIntArray].
     *
     * @return The first integer.
     * @throws [IndexOutOfBoundsException] if [CircularIntArray] is empty.
     */
    public val first: Int
        get() {
            val h = head
            if (h == tail) throw createIndexOutOfBoundsException()
            return elements[h and capacityBitmask]
        }

    /**
     * Get last integer of the [CircularIntArray].
     *
     * @return The last integer.
     * @throws [IndexOutOfBoundsException] if [CircularIntArray] is empty.
     */
    public val last: Int
        get() {
            val t = tail
            if (head == t) throw createIndexOutOfBoundsException()
            return elements[(t - 1) and capacityBitmask]
        }

    /**
     * Get nth (0 <= n <= size()-1) integer of the [CircularIntArray].
     *
     * @param index The zero based element index in the [CircularIntArray].
     * @return The nth integer.
     * @throws [IndexOutOfBoundsException] if n < 0 or n >= size().
     */
    public operator fun get(index: Int): Int {
        // Modulo 2^32, `pos - tail == index - (tail - head) == index - size`.
        // Signed `(index or (index - size)) < index` iff `index >= 0` and `index - size < 0`.
        val pos = index + head
        if ((index or (pos - tail)) >= index) {
            throw createIndexOutOfBoundsException()
        }
        return elements[pos and capacityBitmask]
    }

    /**
     * Get number of integers in the [CircularIntArray].
     *
     * @return Number of integers in the [CircularIntArray].
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
