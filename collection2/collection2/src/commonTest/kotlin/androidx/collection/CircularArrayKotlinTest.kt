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
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CircularArrayKotlinTest {
    val ELEMENT_X = Any()
    val ELEMENT_Y = Any()
    val ELEMENT_Z = Any()

    @Test
    fun creatingWithZeroCapacity() {
        assertThrows<IllegalArgumentException> { CircularArray<Any>(0) }
    }

    @Test
    fun creatingWithOverCapacity() {
        assertThrows<IllegalArgumentException> { CircularArray<Any>(Int.MAX_VALUE) }
    }

    @Test
    fun basicOperations() {
        val array = CircularArray<Any>()
        assertTrue(array.isEmpty())
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)
        assertFalse(array.isEmpty())
        assertSame(ELEMENT_Y, array.first)
        assertSame(ELEMENT_Z, array.last)
        assertSame(ELEMENT_X, array[1])
        assertSame(ELEMENT_Y, array.popFirst())
        assertSame(ELEMENT_Z, array.popLast())
        assertSame(ELEMENT_X, array.first)
        assertSame(ELEMENT_X, array.last)
        assertSame(ELEMENT_X, array.popFirst())
        assertTrue(array.isEmpty())

        assertThrows<IndexOutOfBoundsException> { array.popFirst() }
    }

    @Test
    fun removeFromEitherEnd() {
        val array = CircularArray<Any>()
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)

        // These are no-ops.
        array.removeFromStart(0)
        array.removeFromStart(-1)
        array.removeFromEnd(0)
        array.removeFromEnd(-1)

        assertThrows<IndexOutOfBoundsException> { array.removeFromStart(4) }
        assertThrows<IndexOutOfBoundsException> { array.removeFromEnd(4) }

        array.removeFromStart(2)
        assertSame(ELEMENT_Z, array.first)
        array.removeFromEnd(1)
        assertTrue(array.isEmpty())
    }

    @Test
    fun grow() {
        val array = CircularArray<Any>(1)
        val expectedSize = 32768
        for (i in 0 until expectedSize) {
            array.addFirst(i)
        }
        assertEquals(expectedSize, array.size)
    }
}
