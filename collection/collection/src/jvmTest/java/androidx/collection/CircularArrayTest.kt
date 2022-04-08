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
        assertFailsWith<IllegalArgumentException> {
            CircularArray<String>(0)
        }
    }

    @Test
    public fun creatingWithOverCapacity() {
        assertFailsWith<IllegalArgumentException> {
            CircularArray<String>(Int.MAX_VALUE)
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
        assertFailsWith<IndexOutOfBoundsException> {
            array.popFirst()
        }
    }

    @Test
    public fun overpoppingFromEnd() {

        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.popLast()
        assertFailsWith<IndexOutOfBoundsException> {
            array.popLast()
        }
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
        assertFailsWith<IndexOutOfBoundsException> {
            array.removeFromStart(4)
        }
    }

    @Test
    public fun overremovalFromEnd() {

        val array = CircularArray<String>()
        array.addFirst(ELEMENT_X)
        array.addFirst(ELEMENT_Y)
        array.addLast(ELEMENT_Z)
        assertFailsWith<IndexOutOfBoundsException> {
            array.removeFromEnd(4)
        }
    }

    @Test
    public fun grow() {
        val array = CircularArray<String>(1)
        val expectedSize = 32768
        repeat(expectedSize) {
            array.addFirst("String $it")
        }
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
        assertFailsWith<IndexOutOfBoundsException> {
            array.popLast()
        }
    }
}