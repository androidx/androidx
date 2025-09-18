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
        val ranges =
            listOf(
                // Empty or inverted ranges
                0 to 0,
                66 to 66,
                130 to 130,
                4 to 2,
                66 to 60,
                130 to 128,
                // 1 item ranges
                5 to 6,
                71 to 72,
                132 to 132,
                // Larger ranges that fit in a single word
                2 to 30,
                70 to 83,
                130 to 150,
                // Larger ranges that cross word boundaries
                60 to 80,
                120 to 140,
                60 to 140,
            )

        for (range in ranges) {
            val vector = BitVector()
            val (start, end) = range
            vector.setRange(start, end)
            for (bit in 0 until vector.size) {
                assertEquals(bit in start until end, vector[bit])
            }
        }
    }

    @Test
    fun canFindTheNextSetBit() {
        val vector = BitVector()
        vector.setRange(2, 5)
        vector.setRange(10, 12)
        vector.setRange(80, 82)
        vector.setRange(130, 132)
        vector.setRange(260, 262)
        vector.setRange(1030, 1032)

        val received = mutableListOf<Int>()
        var current = vector.nextSet(0)
        while (current < vector.size) {
            received.add(current)
            current = vector.nextSet(current + 1)
        }

        assertEquals(listOf(2, 3, 4, 10, 11, 80, 81, 130, 131, 260, 261, 1030, 1031), received)
    }

    @Test
    fun canFindTheNextClearBit() {
        val vector = BitVector()
        vector.setRange(2, 5)
        vector.setRange(10, 12)

        var max = 15
        val received = mutableListOf<Int>()
        var current = vector.nextClear(0)
        while (current < max) {
            received.add(current)
            current = vector.nextClear(current + 1)
        }

        assertEquals(listOf(0, 1, 5, 6, 7, 8, 9, 12, 13, 14), received)

        received.clear()
        val vector2 = BitVector()
        vector2.setRange(70, 72)

        max = 74
        current = vector2.nextClear(64)
        while (current < max) {
            received.add(current)
            current = vector2.nextClear(current + 1)
        }

        assertEquals(listOf(64, 65, 66, 67, 68, 69, 72, 73), received)

        received.clear()
        val vector3 = BitVector()
        vector3.setRange(128, 130)

        max = 132
        current = vector3.nextClear(126)
        while (current < max) {
            received.add(current)
            current = vector3.nextClear(current + 1)
        }

        assertEquals(listOf(126, 127, 130, 131), received)

        received.clear()
        val vector4 = BitVector()
        vector4.setRange(0, 256)

        max = 260
        current = vector4.nextClear(0)
        while (current < max) {
            received.add(current)
            current = vector4.nextClear(current + 1)
        }

        assertTrue(received.isEmpty())

        received.clear()
        val vector5 = BitVector()
        vector5.setRange(384, 512)

        max = 260
        current = vector5.nextClear(256)
        while (current < max) {
            received.add(current)
            current = vector5.nextClear(current + 1)
        }

        assertEquals(listOf(256, 257, 258, 259), received)
    }
}
