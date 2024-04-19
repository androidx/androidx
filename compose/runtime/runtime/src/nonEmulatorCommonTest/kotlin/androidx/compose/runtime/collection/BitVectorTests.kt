/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.runtime.collection

import androidx.compose.runtime.BitVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitVectorTests {
    @Test
    fun canCreateABitVector() {
        val vector = BitVector()
        assertTrue(vector.nextSet(0) > 0)
    }

    @Test
    fun canSetABit() {
        val vector = BitVector()
        for (bit in listOf(10, 72, 150, 200, 400)) {
            vector[bit] = true
            assertTrue(vector[bit])
        }
    }

    @Test
    fun canClearABit() {
        val vector = BitVector()
        for (bit in listOf(10, 72, 150, 200, 400)) {
            vector[bit] = true
            vector[bit] = false
            assertFalse(vector[bit])
        }
    }

    @Test
    fun canSetARange() {
        val vector = BitVector()
        vector.setRange(2, 30)
        for (bit in 0 until vector.size) {
            assertEquals(bit in 2 until 30, vector[bit])
        }
    }

    @Test
    fun canFindTheNextSetBit() {
        val vector = BitVector()
        vector.setRange(2, 5)
        vector.setRange(10, 12)
        val received = mutableListOf<Int>()
        var current = vector.nextSet(0)
        while (current < vector.size) {
            received.add(current)
            current = vector.nextSet(current + 1)
        }
        assertEquals(listOf(2, 3, 4, 10, 11), received)
    }

    @Test
    fun canFindTheNextClearBit() {
        val max = 15
        val vector = BitVector()
        vector.setRange(2, 5)
        vector.setRange(10, 12)
        val received = mutableListOf<Int>()
        var current = vector.nextClear(0)
        while (current < max) {
            received.add(current)
            current = vector.nextClear(current + 1)
        }
        assertEquals(listOf(0, 1, 5, 6, 7, 8, 9, 12, 13, 14), received)
    }
}
