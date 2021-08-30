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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircularIntArrayKotlinTest {
    @Test
    fun creatingWithZeroCapacity() {
        assertThrows<IllegalArgumentException> { CircularIntArray(0) }
    }

    @Test
    fun creatingWithOverCapacity() {
        assertThrows<IllegalArgumentException> { CircularIntArray(Int.MAX_VALUE) }
    }

    @Test
    fun basicOperations() {
        val array = CircularIntArray()
        assertTrue(array.isEmpty())
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)
        assertFalse(array.isEmpty())
        assertEquals(43, array.first)
        assertEquals(-1, array.last)
        assertEquals(42, array[1])
        assertEquals(43, array.popFirst())
        assertEquals(-1, array.popLast())
        assertEquals(42, array.first)
        assertEquals(42, array.last)
        assertEquals(42, array.popFirst())
        assertTrue(array.isEmpty())

        assertThrows<IndexOutOfBoundsException> { array.popFirst() }
    }

    @Test
    fun removeFromEitherEnd() {
        val array = CircularIntArray()
        array.addFirst(42)
        array.addFirst(43)
        array.addLast(-1)

        // These are no-ops.
        array.removeFromStart(0)
        array.removeFromStart(-1)
        array.removeFromEnd(0)
        array.removeFromEnd(-1)

        assertThrows<IndexOutOfBoundsException> { array.removeFromStart(4) }
        assertThrows<IndexOutOfBoundsException> { array.removeFromEnd(4) }

        array.removeFromStart(2)
        assertEquals(-1, array.first)
        array.removeFromEnd(1)
        assertTrue(array.isEmpty())
    }

    @Test
    fun grow() {
        val array = CircularIntArray(1)
        val expectedSize = 32768
        for (i in 0 until expectedSize) {
            array.addFirst(i)
        }
        assertEquals(expectedSize, array.size)
    }
}