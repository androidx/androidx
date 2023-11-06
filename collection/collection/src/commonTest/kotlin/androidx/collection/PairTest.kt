/*
 * Copyright 2023 The Android Open Source Project
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
import kotlin.test.assertNotEquals

class PairTest {

    @Test
    fun intCreation() {
        val pair = IntIntPair(3, 5)
        assertEquals(3, pair.first)
        assertEquals(5, pair.second)
    }

    @Test
    fun intEquality() {
        val pair = IntIntPair(3, 5)
        val pairEqual = IntIntPair(3, 5)
        val pairUnequal1 = IntIntPair(4, 5)
        val pairUnequal2 = IntIntPair(3, 6)
        val pairUnequal3 = IntIntPair(4, 6)

        assertEquals(pair, pairEqual)
        assertNotEquals(pair, pairUnequal1)
        assertNotEquals(pair, pairUnequal2)
        assertNotEquals(pair, pairUnequal3)
    }

    @Test
    fun intDestructing() {
        val pair = IntIntPair(3, 5)
        val (first, second) = pair
        assertEquals(3, first)
        assertEquals(5, second)
    }

    @Test
    fun floatCreation() {
        val pair = FloatFloatPair(3f, 5f)
        assertEquals(3f, pair.first)
        assertEquals(5f, pair.second)
    }

    @Test
    fun floatEquality() {
        val pair = FloatFloatPair(3f, 5f)
        val pairEqual = FloatFloatPair(3f, 5f)
        val pairUnequal1 = FloatFloatPair(4f, 5f)
        val pairUnequal2 = FloatFloatPair(3f, 6f)
        val pairUnequal3 = FloatFloatPair(4f, 6f)

        assertEquals(pair, pairEqual)
        assertNotEquals(pair, pairUnequal1)
        assertNotEquals(pair, pairUnequal2)
        assertNotEquals(pair, pairUnequal3)
    }

    @Test
    fun floatDestructing() {
        val pair = FloatFloatPair(3f, 5f)
        val (first, second) = pair
        assertEquals(3f, first)
        assertEquals(5f, second)
    }

    @Test
    fun longCreation() {
        val pair = LongLongPair(3, 5)
        assertEquals(3, pair.first)
        assertEquals(5, pair.second)
    }

    @Test
    fun longEquality() {
        val pair = LongLongPair(3, 5)
        val pairEqual = LongLongPair(3, 5)
        val pairUnequal1 = LongLongPair(4, 5)
        val pairUnequal2 = LongLongPair(3, 6)
        val pairUnequal3 = LongLongPair(4, 6)

        assertEquals(pair, pairEqual)
        assertNotEquals(pair, pairUnequal1)
        assertNotEquals(pair, pairUnequal2)
        assertNotEquals(pair, pairUnequal3)
    }

    @Test
    fun longDestructing() {
        val pair = LongLongPair(3, 5)
        val (first, second) = pair
        assertEquals(3L, first)
        assertEquals(5L, second)
    }
}
