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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpTest {
    @Test
    fun constructor() {
        val dim1 = Sp(value = 5f)
        Assert.assertEquals(5f, dim1.value, 0f)

        val dim2 = Sp(value = Float.POSITIVE_INFINITY)
        Assert.assertEquals(Float.POSITIVE_INFINITY, dim2.value, 0f)

        val dim3 = Sp(value = Float.NaN)
        Assert.assertEquals(Float.NaN, dim3.value, 0f)
    }

    @Test
    fun spIntegerConstruction() {
        val dim = 10.sp
        Assert.assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun spFloatConstruction() {
        val dim = 10f.sp
        Assert.assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun spDoubleConstruction() {
        val dim = 10.0.sp
        Assert.assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun subtractOperator() {
        Assert.assertEquals(-1f, (3.sp - 4.sp).value)
        Assert.assertEquals(1f, (10.sp - 9.sp).value, 0f)
    }

    @Test
    fun addOperator() {
        Assert.assertEquals(2f, (1.sp + 1.sp).value, 0f)
        Assert.assertEquals(10f, (6.sp + 4.sp).value, 0f)
    }

    @Test
    fun multiplyOperator() {
        Assert.assertEquals(0f, (1.sp * 0f).value, 0f)
        Assert.assertEquals(10f, (1.sp * 10f).value, 0f)
    }

    @Test
    fun multiplyOperatorScalar() {
        Assert.assertEquals(10f, (10f * 1.sp).value, 0f)
        Assert.assertEquals(10f, (10 * 1.sp).value, 0f)
        Assert.assertEquals(10f, (10.0 * 1.sp).value, 0f)
    }

    @Test
    fun divideOperator() {
        Assert.assertEquals(10f, (100.sp / 10f).value, 0f)
        Assert.assertEquals(0f, (0.sp / 10f).value, 0f)
    }

    @Test
    fun divideToScalar() {
        Assert.assertEquals(1f, 1.sp / 1.sp, 0f)
    }

    @Test
    fun infinite() {
        Assert.assertEquals(Float.POSITIVE_INFINITY, Sp.Infinity.value, 0f)
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun compare() {
        Assert.assertTrue(0.sp < Float.MIN_VALUE.sp)
        Assert.assertTrue(1.sp < 3.sp)
        Assert.assertEquals(0, 1.sp.compareTo(1.sp))
        Assert.assertTrue(1.sp > 0.sp)
        Assert.assertTrue(Float.NEGATIVE_INFINITY.sp < Sp.Infinity)
        Assert.assertTrue(Float.NEGATIVE_INFINITY.sp < 0.sp)
        Assert.assertTrue(Sp.Infinity > Float.MAX_VALUE.sp)

        val zeroNaN = 0f / 0f
        val infNaN = Float.POSITIVE_INFINITY / Float.NEGATIVE_INFINITY
        Assert.assertEquals(0, zeroNaN.sp.compareTo(zeroNaN.sp))
        Assert.assertEquals(0, infNaN.sp.compareTo(infNaN.sp))
    }

    @Test
    fun minTest() {
        Assert.assertEquals(10f, min(10.sp, 20.sp).value, 0f)
        Assert.assertEquals(10f, min(20.sp, 10.sp).value, 0f)
        Assert.assertEquals(10f, min(10.sp, 10.sp).value, 0f)
    }

    @Test
    fun maxTest() {
        Assert.assertEquals(20f, max(10.sp, 20.sp).value, 0f)
        Assert.assertEquals(20f, max(20.sp, 10.sp).value, 0f)
        Assert.assertEquals(20f, max(20.sp, 20.sp).value, 0f)
    }

    @Test
    fun coerceIn() {
        Assert.assertEquals(10f, 10.sp.coerceIn(0.sp, 20.sp).value, 0f)
        Assert.assertEquals(10f, 20.sp.coerceIn(0.sp, 10.sp).value, 0f)
        Assert.assertEquals(10f, 0.sp.coerceIn(10.sp, 20.sp).value, 0f)
        try {
            10.sp.coerceIn(20.sp, 10.sp)
            Assert.fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        Assert.assertEquals(10f, 0.sp.coerceAtLeast(10.sp).value, 0f)
        Assert.assertEquals(10f, 10.sp.coerceAtLeast(5.sp).value, 0f)
        Assert.assertEquals(10f, 10.sp.coerceAtLeast(10.sp).value, 0f)
    }

    @Test
    fun coerceAtMost() {
        Assert.assertEquals(10f, 100.sp.coerceAtMost(10.sp).value, 0f)
        Assert.assertEquals(10f, 10.sp.coerceAtMost(20.sp).value, 0f)
        Assert.assertEquals(10f, 10.sp.coerceAtMost(10.sp).value, 0f)
    }

    @Test
    fun lerp() {
        Assert.assertEquals(10f, lerp(0.sp, 10.sp, 1f).value, 0f)
        Assert.assertEquals(0f, lerp(0.sp, 10.sp, 0f).value, 0f)
        Assert.assertEquals(5f, lerp(0.sp, 10.sp, 0.5f).value, 0f)
    }
}