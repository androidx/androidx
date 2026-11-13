/*
 * Copyright 2020 The Android Open Source Project
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class CircularIntArrayTest {

    @Test
    public fun creatingWithZeroCapacity() {
        assertFailsWith<IllegalArgumentException> { CircularIntArray(0) }
    }

    @Test
    public fun creatingWithOverCapacity() {
        assertFailsWith<IllegalArgumentException> { CircularIntArray(Int.MAX_VALUE) }
    }

    @Test
    public fun basicOperations() {
        val array = CircularIntArray()
        assertTrue { array.isEmpty() }
        assertEquals(0, array.size())
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)
        assertFalse(array.isEmpty())
        assertEquals(3, array.size())
        assertEquals(43, array.first)
        assertEquals(-1, array.last)
        assertEquals(42, array[1])
        assertEquals(43, array.popFirst())
        assertEquals(-1, array.popLast())
        assertEquals(42, array.first)
        assertEquals(42, array.last)
        assertEquals(42, array.popFirst())
        assertTrue { array.isEmpty() }
        assertEquals(0, array.size())
    }

    @Test
    public fun overpoppingFromStart() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.popFirst()
        assertFailsWith<IndexOutOfBoundsException> { array.popFirst() }
    }

    @Test
    public fun overpoppingFromEnd() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.popLast()
        assertFailsWith<IndexOutOfBoundsException> { array.popLast() }
    }

    @Test
    public fun removeFromEitherEnd() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)

        // These are no-ops.
        array.removeFromStart(0)
        array.removeFromStart(-1)
        array.removeFromEnd(0)
        array.removeFromEnd(-1)
        array.removeFromStart(2)
        assertEquals(-1, array.first)
        array.removeFromEnd(1)
        assertTrue { array.isEmpty() }

        assertEquals(0, array.size())
    }

    @Test
    public fun overremovalFromStart() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)
        assertFailsWith<IndexOutOfBoundsException> { array.removeFromStart(4) }
    }

    @Test
    public fun overremovalFromEnd() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)
        assertFailsWith<IndexOutOfBoundsException> { array.removeFromEnd(4) }
    }

    @Test
    public fun grow() {
        val array = CircularIntArray(1)
        val expectedSize = 32768
        repeat(expectedSize) { array.addFirst(it) }
        assertEquals(expectedSize, array.size())
    }

    @Test
    public fun getBounds() {
        val empty = CircularIntArray()
        assertFailsWith<IndexOutOfBoundsException> { empty[0] }
        assertFailsWith<IndexOutOfBoundsException> { empty[-1] }
        assertFailsWith<IndexOutOfBoundsException> { empty[1] }
        assertFailsWith<IndexOutOfBoundsException> { empty[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { empty[Int.MAX_VALUE] }

        val wrapped = CircularIntArray(8)
        repeat(6) {
            wrapped.addLast(it)
            wrapped.popFirst()
        }
        for (i in 0 until 5) {
            wrapped.addLast(100 + i)
        }
        assertEquals(5, wrapped.size())
        for (i in 0 until 5) {
            assertEquals(100 + i, wrapped[i])
        }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[-1] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[5] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[8] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[Int.MAX_VALUE] }

        wrapped.clear()
        assertFailsWith<IndexOutOfBoundsException> { wrapped[0] }

        // Negative head via addFirst + doubleCapacity + bulk removals across wrap boundaries.
        val backward = CircularIntArray(4)
        for (i in 0 until 3) {
            backward.addFirst(200 + i) // head = -3, tail = 0 -> [202, 201, 200]
        }
        assertEquals(3, backward.size())
        assertEquals(202, backward.first)
        assertEquals(200, backward.last)
        assertEquals(202, backward[0])
        assertEquals(201, backward[1])
        assertEquals(200, backward[2])
        assertFailsWith<IndexOutOfBoundsException> { backward[-1] }
        assertFailsWith<IndexOutOfBoundsException> { backward[3] }
        assertFailsWith<IndexOutOfBoundsException> { backward[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { backward[Int.MAX_VALUE] }

        // Trigger doubleCapacity while head < 0, then wrap tail to an exact multiple of capacity.
        backward.addLast(300) // size reaches 4 -> doubleCapacity to 8: [202, 201, 200, 300]
        for (i in 1..4) {
            backward.addLast(300 + i) // size reaches 8 -> doubleCapacity to 16
        }
        assertEquals(8, backward.size())
        assertEquals(202, backward[0])
        assertEquals(304, backward[7])
        backward.removeFromStart(3) // [300, 301, 302, 303, 304]
        backward.removeFromEnd(2) // [300, 301, 302]
        assertEquals(3, backward.size())
        assertEquals(300, backward[0])
        assertEquals(301, backward[1])
        assertEquals(302, backward[2])
        assertFailsWith<IndexOutOfBoundsException> { backward[3] }

        // Exercise removeFromStart and removeFromEnd when tail wraps and lands on capacity
        // multiple.
        val ring = CircularIntArray(8)
        for (i in 0 until 6) {
            ring.addLast(i)
            ring.popFirst()
        }
        // head = 6, tail = 6; add 4 elements -> head = 6, tail = 10 (physical slots 6, 7, 0, 1).
        for (i in 0 until 4) {
            ring.addLast(10 + i)
        }
        ring.removeFromEnd(2) // removes slots 1, 0 -> tail = 8 (exact multiple of capacity 8!)
        assertEquals(2, ring.size())
        assertEquals(10, ring[0])
        assertEquals(11, ring[1])
        ring.removeFromEnd(1) // removes slot 7 when (tail and mask) == 0 -> tail = 7
        assertEquals(1, ring.size())
        assertEquals(10, ring.first)
        assertEquals(10, ring.last)
        ring.addLast(20)
        ring.addLast(21)
        ring.addLast(22) // physical slots 6, 7, 0, 1
        ring.removeFromStart(3) // removes across wrap (slots 6, 7, 0) -> leaves slot 1 (22)
        assertEquals(1, ring.size())
        assertEquals(22, ring[0])
    }
}
