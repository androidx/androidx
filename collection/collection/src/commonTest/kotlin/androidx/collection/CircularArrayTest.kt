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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

public class CircularArrayTest {
    private val ELEMENT_X = "x"
    private val ELEMENT_Y = "y"
    private val ELEMENT_Z = "z"

    @Test
    public fun creatingWithZeroCapacity() {
        assertFailsWith<IllegalArgumentException> { CircularArray<String>(0) }
        assertFailsWith<IllegalArgumentException> { CircularArray<String>(-1) }
    }

    @Test
    public fun creatingWithOverCapacity() {
        assertFailsWith<IllegalArgumentException> { CircularArray<String>((1 shl 30) + 1) }
        assertFailsWith<IllegalArgumentException> { CircularArray<String>(Int.MAX_VALUE) }
    }

    @Test
    public fun variousInitialCapacities() {
        for (minCapacity in 1..33) {
            val array = CircularArray<Int>(minCapacity)
            for (i in 0 until minCapacity) {
                if (i % 2 == 0) {
                    array.addLast(i)
                } else {
                    array.addFirst(-i)
                }
            }
            assertEquals(minCapacity, array.size())
            repeat(minCapacity) { array.popFirst() }
            assertTrue(array.isEmpty())
        }
    }

    @Test
    public fun basicOperations() {
        val array = CircularArray<String>()
        assertTrue(array.isEmpty())
        assertEquals(0, array.size())
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)
        assertFalse(array.isEmpty())
        assertEquals(3, array.size())
        assertEquals(ELEMENT_Y, array.first)
        assertEquals(ELEMENT_Z, array.last)
        assertEquals(ELEMENT_X, array[1])
        assertEquals(ELEMENT_Y, array.popFirst())
        assertEquals(ELEMENT_Z, array.popLast())
        assertEquals(ELEMENT_X, array.first)
        assertEquals(ELEMENT_X, array.last)
        assertEquals(ELEMENT_X, array.popFirst())
        assertTrue(array.isEmpty())
        assertEquals(0, array.size())
    }

    @Test
    public fun overpoppingFromStart() {
        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.popFirst()
        assertFailsWith<IndexOutOfBoundsException> { array.popFirst() }
    }

    @Test
    public fun overpoppingFromEnd() {

        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.popLast()
        assertFailsWith<IndexOutOfBoundsException> { array.popLast() }
    }

    @Test
    public fun removeFromEitherEnd() {
        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)

        // These are no-ops.
        array.removeFromStart(0)
        array.removeFromStart(-1)
        array.removeFromEnd(0)
        array.removeFromEnd(-1)
        array.removeFromStart(2)
        assertEquals(ELEMENT_Z, array.first)
        array.removeFromEnd(1)
        assertTrue(array.isEmpty())
        assertEquals(0, array.size())
    }

    @Test
    public fun overremovalFromStart() {

        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)
        assertFailsWith<IndexOutOfBoundsException> { array.removeFromStart(4) }
    }

    @Test
    public fun overremovalFromEnd() {

        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)
        assertFailsWith<IndexOutOfBoundsException> { array.removeFromEnd(4) }
    }

    @Test
    public fun grow() {
        val array = CircularArray<String>(1)
        val expectedSize = 32768
        repeat(expectedSize) { array.addFirst("String $it") }
        assertEquals(expectedSize, array.size())
    }

    @Test
    public fun storeAndRetrieveNull() {

        val array = CircularArray<String?>(1)
        array.addFirst(null)
        assertNull(array.popFirst())
        array.addLast(null)
        assertNull(array.popLast())

        // Collection is empty so this should throw.
        assertFailsWith<IndexOutOfBoundsException> { array.popLast() }
    }

    @Test
    public fun firstReturnsNull() {
        val array = CircularArray<String?>(4)
        array.addLast(null)
        array.addLast(ELEMENT_X)
        assertNull(array.first)
    }

    @Test
    public fun lastReturnsNull() {
        val array = CircularArray<String?>(4)
        array.addLast(ELEMENT_X)
        array.addLast(null)
        assertNull(array.last)
    }

    @Test
    public fun getReturnsNull() {
        val array = CircularArray<String?>(4)
        array.addLast(ELEMENT_X)
        array.addLast(null)
        array.addLast(ELEMENT_Y)
        assertEquals(ELEMENT_X, array[0])
        assertNull(array[1])
        assertEquals(ELEMENT_Y, array[2])
    }

    @Test
    public fun getBounds() {
        val empty = CircularArray<String>()
        assertFailsWith<IndexOutOfBoundsException> { empty[0] }
        assertFailsWith<IndexOutOfBoundsException> { empty[-1] }
        assertFailsWith<IndexOutOfBoundsException> { empty[1] }
        assertFailsWith<IndexOutOfBoundsException> { empty[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { empty[Int.MAX_VALUE] }

        val wrapped = CircularArray<String>(8)
        repeat(6) {
            wrapped.addLast("pad-$it")
            wrapped.popFirst()
        }
        for (i in 0 until 5) {
            wrapped.addLast("item-$i")
        }
        assertEquals(5, wrapped.size())
        for (i in 0 until 5) {
            assertEquals("item-$i", wrapped[i])
        }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[-1] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[5] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[8] }
        assertFailsWith<IndexOutOfBoundsException> { wrapped[Int.MAX_VALUE] }

        wrapped.clear()
        assertFailsWith<IndexOutOfBoundsException> { wrapped[0] }

        // Negative head via addFirst + doubleCapacity + bulk removals across wrap boundaries.
        val backward = CircularArray<String>(4)
        for (i in 0 until 3) {
            backward.addFirst("pre-$i") // head = -3, tail = 0 -> [pre-2, pre-1, pre-0]
        }
        assertEquals(3, backward.size())
        assertEquals("pre-2", backward.first)
        assertEquals("pre-0", backward.last)
        assertEquals("pre-2", backward[0])
        assertEquals("pre-1", backward[1])
        assertEquals("pre-0", backward[2])
        assertFailsWith<IndexOutOfBoundsException> { backward[-1] }
        assertFailsWith<IndexOutOfBoundsException> { backward[3] }
        assertFailsWith<IndexOutOfBoundsException> { backward[Int.MIN_VALUE] }
        assertFailsWith<IndexOutOfBoundsException> { backward[Int.MAX_VALUE] }

        // Trigger doubleCapacity while head < 0, then remove from both ends.
        backward.addLast("post-0") // size reaches 4 -> doubleCapacity to 8
        for (i in 1..4) {
            backward.addLast("post-$i") // size reaches 8 -> doubleCapacity to 16
        }
        assertEquals(8, backward.size())
        assertEquals("pre-2", backward[0])
        assertEquals("post-4", backward[7])
        backward.removeFromStart(3) // [post-0, post-1, post-2, post-3, post-4]
        backward.removeFromEnd(2) // [post-0, post-1, post-2]
        assertEquals(3, backward.size())
        assertEquals("post-0", backward[0])
        assertEquals("post-1", backward[1])
        assertEquals("post-2", backward[2])
        assertFailsWith<IndexOutOfBoundsException> { backward[3] }

        // Exercise removeFromStart and removeFromEnd when tail wraps and lands on capacity
        // multiple.
        val ring = CircularArray<String>(8)
        for (i in 0 until 6) {
            ring.addLast("skip-$i")
            ring.popFirst()
        }
        // head = 6, tail = 6; add 4 elements -> head = 6, tail = 10 (physical slots 6, 7, 0, 1).
        for (i in 0 until 4) {
            ring.addLast("v-$i")
        }
        ring.removeFromEnd(2) // removes slots 1, 0 -> tail = 8 (exact multiple of capacity 8!)
        assertEquals(2, ring.size())
        assertEquals("v-0", ring[0])
        assertEquals("v-1", ring[1])
        ring.removeFromEnd(1) // removes slot 7 when (tail and mask) == 0 -> tail = 7
        assertEquals(1, ring.size())
        assertEquals("v-0", ring.first)
        assertEquals("v-0", ring.last)
        ring.addLast("w-0")
        ring.addLast("w-1")
        ring.addLast("w-2") // physical slots 6, 7, 0, 1
        ring.removeFromStart(3) // removes across wrap (slots 6, 7, 0) -> leaves slot 1 ("w-2")
        assertEquals(1, ring.size())
        assertEquals("w-2", ring[0])
    }
}
