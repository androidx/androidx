/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntPxTest {
    @Test
    fun constructor() {
        val dim1 = IntPx(value = 5)
        assertEquals(5, dim1.value)

        val dim2 = IntPx(value = Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, dim2.value)
    }

    @Test
    fun pxIntegerConstruction() {
        val dim = 10.ipx
        assertEquals(10, dim.value)
    }

    @Test
    fun subtractOperator() {
        assertEquals(-1, (3.ipx - 4.ipx).value)
        assertEquals(1, (10.ipx - 9.ipx).value)
        assertEquals(IntPx.Infinity, 10.ipx - IntPx.Infinity)
        assertEquals(IntPx.Infinity, IntPx.Infinity - 10.ipx)
    }

    @Test
    fun addOperator() {
        assertEquals(2, (1.ipx + 1.ipx).value)
        assertEquals(10, (6.ipx + 4.ipx).value)
        assertEquals(IntPx.Infinity, 10.ipx + IntPx.Infinity)
        assertEquals(IntPx.Infinity, IntPx.Infinity + 10.ipx)
    }

    @Test
    fun multiplyOperator() {
        assertEquals(0, (1.ipx * 0).value)
        assertEquals(0, (1.ipx * 0f).value)
        assertEquals(0, (1.ipx * 0.0).value)
        assertEquals(10, (1.ipx * 10).value)
        assertEquals(10, (1.ipx * 10f).value)
        assertEquals(10, (1.ipx * 9.5f).value)
        assertEquals(10, (1.ipx * 10.0).value)
        assertEquals(10, (1.ipx * 9.5).value)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 10)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 10f)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 10.0)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 0)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 0f)
        assertEquals(IntPx.Infinity, IntPx.Infinity * 0.0)
    }

    @Test
    fun multiplyOperatorScalar() {
        assertEquals(10, (10f * 1.ipx).value)
        assertEquals(10, (10 * 1.ipx).value)
        assertEquals(10, (10.0 * 1.ipx).value)

        assertEquals(IntPx.Infinity, 10 * IntPx.Infinity)
        assertEquals(IntPx.Infinity, 10f * IntPx.Infinity)
        assertEquals(IntPx.Infinity, 10.0 * IntPx.Infinity)
        assertEquals(IntPx.Infinity, 0 * IntPx.Infinity)
        assertEquals(IntPx.Infinity, 0f * IntPx.Infinity)
        assertEquals(IntPx.Infinity, 0.0 * IntPx.Infinity)
    }

    @Test
    fun infinity() {
        assertEquals(Int.MAX_VALUE, IntPx.Infinity.value)
        assertFalse(IntPx.Infinity.isFinite())
        assertTrue(30.ipx.isFinite())
    }

    @Test
    fun compare() {
        assertTrue(1.ipx < 3.ipx)
        assertEquals(0, 1.ipx.compareTo(1.ipx))
        assertTrue(1.ipx > 0.ipx)
        assertTrue(10000.ipx < IntPx.Infinity)
        assertTrue(IntPx.Infinity > 10000.ipx)
    }

    @Test
    fun minTest() {
        assertEquals(10, min(10.ipx, 20.ipx).value)
        assertEquals(10, min(20.ipx, 10.ipx).value)
        assertEquals(10, min(10.ipx, 10.ipx).value)
        assertEquals(10.ipx, min(10.ipx, IntPx.Infinity))
    }

    @Test
    fun maxTest() {
        assertEquals(20, max(10.ipx, 20.ipx).value)
        assertEquals(20, max(20.ipx, 10.ipx).value)
        assertEquals(20, max(20.ipx, 20.ipx).value)
        assertEquals(IntPx.Infinity, max(IntPx.Infinity, 20.ipx))
        assertEquals(IntPx.Infinity, max(20.ipx, IntPx.Infinity))
    }

    @Test
    fun coerceIn() {
        assertEquals(10, 10.ipx.coerceIn(0.ipx, 20.ipx).value)
        assertEquals(10, 20.ipx.coerceIn(0.ipx, 10.ipx).value)
        assertEquals(10.ipx, 10.ipx.coerceIn(0.ipx, IntPx.Infinity))
        assertEquals(10.ipx, 0.ipx.coerceIn(10.ipx, IntPx.Infinity))
        assertEquals(10, 0.ipx.coerceIn(10.ipx, 20.ipx).value)
        try {
            10.ipx.coerceIn(20.ipx, 10.ipx)
            fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        assertEquals(10, 0.ipx.coerceAtLeast(10.ipx).value)
        assertEquals(10, 10.ipx.coerceAtLeast(5.ipx).value)
        assertEquals(10, 10.ipx.coerceAtLeast(10.ipx).value)
        assertEquals(IntPx.Infinity, 10.ipx.coerceAtLeast(IntPx.Infinity))
    }

    @Test
    fun coerceAtMost() {
        assertEquals(10, 100.ipx.coerceAtMost(10.ipx).value)
        assertEquals(10, 10.ipx.coerceAtMost(20.ipx).value)
        assertEquals(10, 10.ipx.coerceAtMost(10.ipx).value)
        assertEquals(10, 10.ipx.coerceAtMost(IntPx.Infinity).value)
    }

    @Test
    fun sizeCenter() {
        val size = IntPxSize(width = 10.ipx, height = 20.ipx)
        assertEquals(IntPxPosition(5.ipx, 10.ipx), size.center())
        val size2 = IntPxSize(width = IntPx.Infinity, height = IntPx.Infinity)
        assertEquals(IntPxPosition(IntPx.Infinity, IntPx.Infinity), size2.center())
    }

    @Test
    fun lerp() {
        assertEquals(10.ipx, lerp(10.ipx, 20.ipx, 0f))
        assertEquals(20.ipx, lerp(10.ipx, 20.ipx, 1f))
        assertEquals(15.ipx, lerp(10.ipx, 20.ipx, 0.5f))
        assertEquals(IntPx.Infinity, lerp(10.ipx, IntPx.Infinity, 0.5f))
        assertEquals(IntPx.Infinity, lerp(IntPx.Infinity, 10.ipx, 0.5f))
    }

    @Test
    fun lerpPosition() {
        val a = IntPxPosition(3.ipx, 10.ipx)
        val b = IntPxPosition(5.ipx, 8.ipx)
        assertEquals(IntPxPosition(4.ipx, 9.ipx), lerp(a, b, 0.5f))
        assertEquals(IntPxPosition(3.ipx, 10.ipx), lerp(a, b, 0f))
        assertEquals(IntPxPosition(5.ipx, 8.ipx), lerp(a, b, 1f))
    }

    @Test
    fun positionMinus() {
        val a = IntPxPosition(3.ipx, 10.ipx)
        val b = IntPxPosition(5.ipx, 8.ipx)
        assertEquals(IntPxPosition(-2.ipx, 2.ipx), a - b)
        assertEquals(IntPxPosition(2.ipx, -2.ipx), b - a)
    }

    @Test
    fun positionPlus() {
        val a = IntPxPosition(3.ipx, 10.ipx)
        val b = IntPxPosition(5.ipx, 8.ipx)
        assertEquals(IntPxPosition(8.ipx, 18.ipx), a + b)
        assertEquals(IntPxPosition(8.ipx, 18.ipx), b + a)
    }

    @Test
    fun boundsWidth() {
        val bounds = IntPxBounds(10.ipx, 5.ipx, 25.ipx, 15.ipx)
        assertEquals(15.ipx, bounds.width)
    }

    @Test
    fun boundsHeight() {
        val bounds = IntPxBounds(10.ipx, 5.ipx, 25.ipx, 15.ipx)
        assertEquals(10.ipx, bounds.height)
    }

    @Test
    fun toSize() {
        val size = IntPxSize(15.ipx, 10.ipx)
        val bounds = IntPxBounds(10.ipx, 5.ipx, 25.ipx, 15.ipx)
        assertEquals(size, bounds.toSize())
    }

    @Test
    fun round() {
        assertEquals(0, 0.px.round().value)
        assertEquals(0, 0.25.px.round().value)
        assertEquals(1, 0.5.px.round().value)
        assertEquals(99, 99.49.px.round().value)
    }

    @Test
    fun createSize() {
        assertEquals(PxSize(10.px, 20.px), PxSize(10.ipx, 20.ipx))
    }

    @Test
    fun createPosition() {
        assertEquals(PxPosition(10.px, 20.px), PxPosition(10.ipx, 20.ipx))
    }
}