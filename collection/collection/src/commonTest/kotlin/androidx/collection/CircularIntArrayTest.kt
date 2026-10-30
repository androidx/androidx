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
    public fun isNotEmptyTest() {
        val array = CircularIntArray()
        assertFalse(array.isNotEmpty())
        array.addLast(42)
        assertTrue(array.isNotEmpty())
    }

    @Test
    public fun containsAndIndexOfTest() {
        val array = CircularIntArray()
        assertFalse(42 in array)
        assertEquals(-1, array.indexOf(42))

        array.addLast(10)
        array.addLast(20)
        array.addLast(30)

        assertTrue(10 in array)
        assertTrue(20 in array)
        assertTrue(30 in array)
        assertEquals(0, array.indexOf(10))
        assertEquals(1, array.indexOf(20))
        assertEquals(2, array.indexOf(30))

        array.popFirst() // Remove 10
        assertFalse(10 in array)
        assertEquals(-1, array.indexOf(10))
        assertEquals(0, array.indexOf(20))
    }

    @Test
    public fun forEachTest() {
        val array = CircularIntArray()
        array.addLast(10)
        array.addLast(20)
        array.addLast(30)

        val collected = mutableListOf<Int>()
        array.forEach { collected.add(it) }

        assertEquals(listOf(10, 20, 30), collected)
    }

    @Test
    public fun extensionPropertiesTest() {
        val array = CircularIntArray()
        assertEquals(0, array.size)
        assertEquals(-1, array.lastIndex)
        assertEquals(0 until 0, array.indices)

        array.addLast(10)
        array.addLast(20)

        assertEquals(2, array.size)
        assertEquals(1, array.lastIndex)
        assertEquals(0..1, array.indices)
    }
}
