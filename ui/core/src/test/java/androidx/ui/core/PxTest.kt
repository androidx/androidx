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

import androidx.ui.engine.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PxTest {
    @Test
    fun constructor() {
        val dim1 = Px(value = 5f)
        assertEquals(5f, dim1.value, 0f)

        val dim2 = Px(value = Float.POSITIVE_INFINITY)
        assertEquals(Float.POSITIVE_INFINITY, dim2.value, 0f)

        val dim3 = Px(value = Float.NaN)
        assertEquals(Float.NaN, dim3.value, 0f)
    }

    @Test
    fun pxIntegerConstruction() {
        val dim = 10.px
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun pxFloatConstruction() {
        val dim = 10f.px
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun pxDoubleConstruction() {
        val dim = 10.0.px
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun subtractOperator() {
        assertEquals(-1f, (3.px - 4.px).value)
        assertEquals(1f, (10.px - 9.px).value, 0f)
    }

    @Test
    fun addOperator() {
        assertEquals(2.5f, (1.5.px + 1.ipx).value)
        assertEquals(10.25f, (6.25.px + 4.ipx).value)
    }

    @Test
    fun subtractIntPxOperator() {
        assertEquals(-.5f, (3.5.px - 4.ipx).value)
        assertEquals(1.25f, (10.25.px - 9.ipx).value)
    }

    @Test
    fun addIntPxOperator() {
        assertEquals(2f, (1.px + 1.px).value, 0f)
        assertEquals(10f, (6.px + 4.px).value, 0f)
    }

    @Test
    fun multiplyOperator() {
        assertEquals(0f, (1.px * 0f).value, 0f)
        assertEquals(10f, (1.px * 10f).value, 0f)
    }

    @Test
    fun multiplyOperatorScalar() {
        assertEquals(10f, 10f * 1.px.value, 0f)
        assertEquals(10f, 10 * 1.px.value, 0f)
        assertEquals(10f, (10.0 * 1.px).value, 0f)
    }

    @Test
    fun multiplyDimension() {
        assertEquals(PxSquared(40f), 10.px * 4.px)
    }

    @Test
    fun multiplyDimensionSquared() {
        assertEquals(PxCubed(40f), 10.px * (2.px * 2.px))
    }

    @Test
    fun divideOperator() {
        assertEquals(10f, 100.px / 10f.px, 0f)
        assertEquals(0f, 0.px / 10f.px, 0f)
    }

    @Test
    fun divideOperatorInverse() {
        assertEquals(PxInverse(10f), 100f / 10.px)
        assertEquals(PxInverse(10f), 100.0 / 10.px)
        assertEquals(PxInverse(10f), 100 / 10.px)
    }

    @Test
    fun divideToScalar() {
        assertEquals(1f, 1.px / 1.px, 0f)
    }

    @Test
    fun divideToInverse() {
        assertEquals(PxInverse(10f), 100.px / (5.px * 2.px))
    }

    @Test
    fun infinite() {
        assertEquals(Float.POSITIVE_INFINITY, Px.Infinity.value, 0f)
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun compare() {
        assertTrue(0.px < Float.MIN_VALUE.px)
        assertTrue(1.px < 3.px)
        assertEquals(0, 1.px.compareTo(1.px))
        assertTrue(1.px > 0.px)
        assertTrue(Float.NEGATIVE_INFINITY.px < Px.Infinity)
        assertTrue(Float.NEGATIVE_INFINITY.px < 0.px)
        assertTrue(Px.Infinity > Float.MAX_VALUE.px)

        val zeroNaN = 0f / 0f
        val infNaN = Float.POSITIVE_INFINITY / Float.NEGATIVE_INFINITY
        assertEquals(0, zeroNaN.px.compareTo(zeroNaN.px))
        assertEquals(0, infNaN.px.compareTo(infNaN.px))
    }

    @Test
    fun addDimension2() {
        assertEquals(PxSquared(4f), (2.px * 1.px) + (1.px * 2.px))
    }

    @Test
    fun subtractDimension2() {
        assertEquals(PxSquared(0f), (2.px * 3.px) - (3.px * 2.px))
    }

    @Test
    fun divideDimension2() {
        assertEquals(PxSquared(1f), (2.px * 5.px) / 10f)
    }

    @Test
    fun divideDimension2Dimension() {
        assertEquals(1f, ((2.px * 2.px) / 4.px).value, 0f)
    }

    @Test
    fun divideDimension2Dimension2() {
        assertEquals(1f, (2.px * 2.px) / (2.px * 2.px))
    }

    @Test
    fun divideDimension2Dimension3() {
        assertEquals(PxInverse(0.5f), (2.px * 2.px) / (2.px * 2.px * 2.px))
    }

    @Test
    fun multiplyDimension2() {
        assertEquals(PxSquared(4f), (2.px * 1.px) * 2f)
    }

    @Test
    fun multiplyDimension2Dimension() {
        assertEquals(PxCubed(4f), (2.px * 1.px) * 2.px)
    }

    @Test
    fun compareDimension2() {
        assertTrue(PxSquared(0f) < PxSquared(Float.MIN_VALUE))
        assertTrue(PxSquared(1f) < PxSquared(3f))
        assertTrue(PxSquared(1f) == PxSquared(1f))
        assertTrue(PxSquared(1f) > PxSquared(0f))
    }

    @Test
    fun addDimension3() {
        assertEquals(PxCubed(4f), (2.px * 1.px * 1.px) + (1.px * 2.px * 1.px))
    }

    @Test
    fun subtractDimension3() {
        assertEquals(PxCubed(0f), (2.px * 3.px * 1.px) - (3.px * 2.px * 1.px))
    }

    @Test
    fun divideDimension3() {
        assertEquals(PxCubed(1f), (2.px * 5.px * 1.px) / 10f)
    }

    @Test
    fun divideDimension3Dimension() {
        assertEquals(PxSquared(1f), (2.px * 2.px * 1.px) / 4.px)
    }

    @Test
    fun divideDimension3Dimension2() {
        assertEquals(1f, ((2.px * 2.px * 1.px) / (2.px * 2.px)).value, 0f)
    }

    @Test
    fun divideDimension3Dimension3() {
        assertEquals(1f, (2.px * 2.px * 1.px) / (2.px * 2.px * 1.px))
    }

    @Test
    fun multiplyDimension3() {
        assertEquals(PxCubed(4f), (2.px * 1.px * 1.px) * 2f)
    }

    @Test
    fun compareDimension3() {
        assertTrue(PxCubed(0f) < PxCubed(Float.MIN_VALUE))
        assertTrue(PxCubed(1f) < PxCubed(3f))
        assertTrue(PxCubed(1f) == PxCubed(1f))
        assertTrue(PxCubed(1f) > PxCubed(0f))
    }

    @Test
    fun addDimensionInverse() {
        assertEquals(PxInverse(1f), 1 / 2.px + 1 / 2.px)
    }

    @Test
    fun subtractDimensionInverse() {
        assertEquals(PxInverse(0f), 1 / 2.px - 1 / 2.px)
    }

    @Test
    fun divideDimensionInverse() {
        assertEquals(PxInverse(1f), (10 / 1.px) / 10f)
    }

    @Test
    fun multiplyDimensionInverse() {
        assertEquals(PxInverse(4f), (1 / 2.px) * 8f)
    }

    @Test
    fun multiplyDimensionInverseDimension() {
        assertEquals(4f, (1 / 2.px) * 8.px)
    }

    @Test
    fun multiplyDimensionInverseDimension2() {
        assertEquals(4f, ((1 / 2.px) * (8.px * 1.px)).value, 0f)
    }

    @Test
    fun multiplyDimensionInverseDimension3() {
        assertEquals(PxSquared(4f), (1 / 2.px) * (8.px * 1.px * 1.px))
    }

    @Test
    fun compareDimensionInverse() {
        assertTrue(PxInverse(0f) < PxInverse(Float.MIN_VALUE))
        assertTrue(PxInverse(1f) < PxInverse(3f))
        assertTrue(PxInverse(1f) == PxInverse(1f))
        assertTrue(PxInverse(1f) > PxInverse(0f))
    }

    @Test
    fun minTest() {
        assertEquals(10f, min(10.px, 20.px).value, 0f)
        assertEquals(10f, min(20.px, 10.px).value, 0f)
        assertEquals(10f, min(10.px, 10.px).value, 0f)
    }

    @Test
    fun maxTest() {
        assertEquals(20f, max(10.px, 20.px).value, 0f)
        assertEquals(20f, max(20.px, 10.px).value, 0f)
        assertEquals(20f, max(20.px, 20.px).value, 0f)
    }

    @Test
    fun coerceIn() {
        assertEquals(10f, 10.px.coerceIn(0.px, 20.px).value, 0f)
        assertEquals(10f, 20.px.coerceIn(0.px, 10.px).value, 0f)
        assertEquals(10f, 0.px.coerceIn(10.px, 20.px).value, 0f)
        try {
            10.px.coerceIn(20.px, 10.px)
            fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        assertEquals(10f, 0.px.coerceAtLeast(10.px).value, 0f)
        assertEquals(10f, 10.px.coerceAtLeast(5.px).value, 0f)
        assertEquals(10f, 10.px.coerceAtLeast(10.px).value, 0f)
    }

    @Test
    fun coerceAtMost() {
        assertEquals(10f, 100.px.coerceAtMost(10.px).value, 0f)
        assertEquals(10f, 10.px.coerceAtMost(20.px).value, 0f)
        assertEquals(10f, 10.px.coerceAtMost(10.px).value, 0f)
    }

    @Test
    fun sizeCenter() {
        val size = PxSize(width = 10.px, height = 20.px)
        assertEquals(PxPosition(5.px, 10.px), size.center())
    }

    @Test
    fun positionDistance() {
        val position = PxPosition(3.px, 4.px)
        assertEquals(5.px, position.getDistance())
    }

    @Test
    fun lerpPosition() {
        val a = PxPosition(3.px, 10.px)
        val b = PxPosition(5.px, 8.px)
        assertEquals(PxPosition(4.px, 9.px), lerp(a, b, 0.5f))
        assertEquals(PxPosition(3.px, 10.px), lerp(a, b, 0f))
        assertEquals(PxPosition(5.px, 8.px), lerp(a, b, 1f))
    }

    @Test
    fun positionMinus() {
        val a = PxPosition(3.px, 10.px)
        val b = PxPosition(5.px, 8.px)
        assertEquals(PxPosition(-2.px, 2.px), a - b)
        assertEquals(PxPosition(2.px, -2.px), b - a)
    }

    @Test
    fun positionPlus() {
        val a = PxPosition(3.px, 10.px)
        val b = PxPosition(5.px, 8.px)
        assertEquals(PxPosition(8.px, 18.px), a + b)
        assertEquals(PxPosition(8.px, 18.px), b + a)
    }

    @Test
    fun pxPositionMinusIntPxPosition() {
        val a = PxPosition(3.px, 10.px)
        val b = IntPxPosition(5.ipx, 8.ipx)
        assertEquals(PxPosition(-2.px, 2.px), a - b)
    }

    @Test
    fun pxPositionPlusIntPxPosition() {
        val a = PxPosition(3.px, 10.px)
        val b = IntPxPosition(5.ipx, 8.ipx)
        assertEquals(PxPosition(8.px, 18.px), a + b)
    }

    @Test
    fun boundsWidth() {
        val bounds = PxBounds(10.px, 5.px, 25.px, 15.px)
        assertEquals(15.px, bounds.width)
    }

    @Test
    fun boundsHeight() {
        val bounds = PxBounds(10.px, 5.px, 25.px, 15.px)
        assertEquals(10.px, bounds.height)
    }

    @Test
    fun toSize() {
        val size = PxSize(15.px, 10.px)
        val bounds = PxBounds(10.px, 5.px, 25.px, 15.px)
        assertEquals(size, bounds.toSize())
    }

    @Test
    fun toBounds() {
        val size = PxSize(15.px, 10.px)
        val bounds = PxBounds(0.px, 0.px, 15.px, 10.px)
        assertEquals(bounds, size.toBounds())
    }

    @Test
    fun boundsToRect() {
        val bounds = PxBounds(10.px, 5.px, 25.px, 15.px)
        val rect = Rect(10f, 5f, 25f, 15f)
        assertEquals(rect, bounds.toRect())
    }

    @Test
    fun sizeToRect() {
        val size = PxSize(10.px, 5.px)
        val rect = Rect(0f, 0f, 10f, 5f)
        assertEquals(rect, size.toRect())
    }
}