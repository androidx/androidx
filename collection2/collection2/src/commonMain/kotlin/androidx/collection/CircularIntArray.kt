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

import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * CircularIntArray is a circular integer array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularIntArray automatically grows its capacity when number of
 * added integers is over its capacity.
 */
class CircularIntArray
/**
 * Creates a circular array with capacity for at least `minCapacity`
 * elements.
 *
 * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
 */
@JvmOverloads constructor(minCapacity: Int = 8) {
    private var elements: IntArray
    private var head = 0
    private var tail = 0
    private var capacityBitmask: Int

    init {
        require(minCapacity >= 1) { "capacity must be >= 1" }
        require(minCapacity <= 2 shl 29) { "capacity must be <= 2^30" }

        // If minCapacity isn't a power of 2, round up to the next highest
        // power of 2.
        val arrayCapacity = if (minCapacity.countOneBits() != 1) {
            (minCapacity - 1).takeHighestOneBit() shl 1
        } else {
            minCapacity
        }

        capacityBitmask = arrayCapacity - 1
        elements = IntArray(arrayCapacity)
    }

    /**
     * Get first integer of the CircularIntArray.
     *
     * @return The first integer.
     * @throws [IndexOutOfBoundsException] if CircularIntArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val first: Int
        get() {
            if (head == tail) throw indexOutOfBounds()
            return elements[head]
        }

    /**
     * Get last integer of the CircularIntArray.
     *
     * @return The last integer.
     * @throws [IndexOutOfBoundsException] if CircularIntArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val last: Int
        get() {
            if (head == tail) throw indexOutOfBounds()
            return elements[tail - 1 and capacityBitmask]
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
        val a = IntArray(newCapacity)
        elements.copyInto(a, startIndex = head, endIndex = n)
        elements.copyInto(a, destinationOffset = r, startIndex = 0, endIndex = head)
        elements = a
        head = 0
        tail = n
        capacityBitmask = newCapacity - 1
    }

    /**
     * Add an integer in front of the CircularIntArray.
     *
     * @param e Integer to add.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun addFirst(e: Int) {
        head = head - 1 and capacityBitmask
        elements[head] = e
        if (head == tail) {
            doubleCapacity()
        }
    }

    /**
     * Add an integer at end of the CircularIntArray.
     *
     * @param e Integer to add.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun addLast(e: Int) {
        elements[tail] = e
        tail = tail + 1 and capacityBitmask
        if (tail == head) {
            doubleCapacity()
        }
    }

    /**
     * Remove first integer from front of the CircularIntArray and return it.
     *
     * @return The integer removed.
     * @throws IndexOutOfBoundsException if CircularIntArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun popFirst(): Int {
        if (head == tail) throw indexOutOfBounds()
        val result = elements[head]
        head = head + 1 and capacityBitmask
        return result
    }

    /**
     * Remove last integer from end of the CircularIntArray and return it.
     * @return The integer removed.
     * @throws IndexOutOfBoundsException if CircularIntArray is empty.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun popLast(): Int {
        if (head == tail) throw indexOutOfBounds()
        val t = tail - 1 and capacityBitmask
        val result = elements[t]
        tail = t
        return result
    }

    /**
     * Remove all integers from the CircularIntArray.
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun clear() {
        tail = head
    }

    /**
     * Remove multiple integers from front of the CircularIntArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of integers to remove.
     * @throws IndexOutOfBoundsException if numOfElements is larger than
     * [.size]
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeFromStart(numOfElements: Int) {
        if (numOfElements <= 0) {
            return
        }
        if (numOfElements > size) {
            throw indexOutOfBounds()
        }
        head = head + numOfElements and capacityBitmask
    }

    /**
     * Remove multiple elements from end of the CircularIntArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of integers to remove.
     * @throws IndexOutOfBoundsException if numOfElements is larger than [size]
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open fun removeFromEnd(numOfElements: Int) {
        if (numOfElements <= 0) {
            return
        }
        if (numOfElements > size) {
            throw indexOutOfBounds()
        }
        tail = tail - numOfElements and capacityBitmask
    }

    /**
     * Get nth (0 <= n <= size()-1) integer of the CircularIntArray.
     * @param n  The zero based element index in the CircularIntArray.
     * @return The nth integer.
     * @throws [IndexOutOfBoundsException] if n < 0 or n >= size().
     */
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open operator fun get(n: Int): Int {
        if (n < 0 || n >= size) throw indexOutOfBounds()
        return elements[head + n and capacityBitmask]
    }

    /** Get number of integers in the CircularIntArray. */
    @get:JvmName("size") // For binary compatibility.
    @Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // For japicmp.
    open val size: Int get() {
        return tail - head and capacityBitmask
    }
}
