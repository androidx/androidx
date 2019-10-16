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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EmTest {
    @Test
    fun constructor() {
        val dim1 = Em(value = 5f)
        assertThat(dim1.value).isEqualTo(5f)

        val dim2 = Em(value = Float.POSITIVE_INFINITY)
        assertThat(dim2.value).isEqualTo(Float.POSITIVE_INFINITY)

        val dim3 = Em(value = Float.NaN)
        assertThat(dim3.value).isEqualTo(Float.NaN)
    }

    @Test
    fun emIntegerConstruction() {
        val dim = 10.em
        assertThat(dim.value).isEqualTo(10f)
    }

    @Test
    fun emFloatConstruction() {
        val dim = 10f.em
        assertThat(dim.value).isEqualTo(10f)
    }

    @Test
    fun emDoubleConstruction() {
        val dim = 10.0.em
        assertThat(dim.value).isEqualTo(10f)
    }

    @Test
    fun unaryMinus() {
        assertThat((-(1.em)).value).isEqualTo(-1f)
    }

    @Test
    fun subtractOperator() {
        assertThat((3.em - 4.em).value).isEqualTo(-1f)
        assertThat((10.em - 9.em).value).isEqualTo(1f)
    }

    @Test
    fun addOperator() {
        assertThat((1.em + 1.em).value).isEqualTo(2f)
        assertThat((6.em + 4.em).value).isEqualTo(10f)
    }

    @Test
    fun multiplyOperator() {
        assertThat((1.em * 0f).value).isZero()
        assertThat((1.em * 10f).value).isEqualTo(10f)
    }

    @Test
    fun multiplyOperatorScalar() {
        assertThat((10f * 1.em).value).isEqualTo(10f)
        assertThat((10 * 1.em).value).isEqualTo(10f)
        assertThat((10.0 * 1.em).value).isEqualTo(10f)
    }

    @Test
    fun divideOperator() {
        assertThat((100.em / 10f).value).isEqualTo(10f)
        assertThat((0.em / 10f).value).isZero()
    }

    @Test
    fun divideToScalar() {
        assertThat(1.em / 1.em).isEqualTo(1f)
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun compare() {
        assertThat(0.em < Float.MIN_VALUE.em).isTrue()
        assertThat(1.em < 3.em).isTrue()
        assertThat(1.em.compareTo(1.em)).isEqualTo(0)
        assertThat(1.em > 0.em).isTrue()
        assertThat(Float.NEGATIVE_INFINITY.em < 0.em).isTrue()

        val zeroNaN = 0f / 0f
        val infNaN = Float.POSITIVE_INFINITY / Float.NEGATIVE_INFINITY
        assertThat(zeroNaN.em.compareTo(zeroNaN.em)).isEqualTo(0)
        assertThat(infNaN.em.compareTo(infNaN.em)).isEqualTo(0)
    }

    @Test
    fun minTest() {
        assertThat(min(10.em, 20.em).value).isEqualTo(10f)
        assertThat(min(20.em, 10.em).value).isEqualTo(10f)
        assertThat(min(10.em, 10.em).value).isEqualTo(10f)
    }

    @Test
    fun maxTest() {
        assertThat(max(10.em, 20.em).value).isEqualTo(20f)
        assertThat(max(20.em, 10.em).value).isEqualTo(20f)
        assertThat(max(20.em, 20.em).value).isEqualTo(20f)
    }

    @Test
    fun coerceIn() {
        assertThat(10.em.coerceIn(0.em, 20.em).value).isEqualTo(10f)
        assertThat(20.em.coerceIn(0.em, 10.em).value).isEqualTo(10f)
        assertThat(0.em.coerceIn(10.em, 20.em).value).isEqualTo(10f)
        try {
            10.em.coerceIn(20.em, 10.em)
            Assert.fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        assertThat(0.em.coerceAtLeast(10.em).value).isEqualTo(10f)
        assertThat(10.em.coerceAtLeast(5.em).value).isEqualTo(10f)
        assertThat(10.em.coerceAtLeast(10.em).value).isEqualTo(10f)
    }

    @Test
    fun coerceAtMost() {
        assertThat(100.em.coerceAtMost(10.em).value).isEqualTo(10f)
        assertThat(10.em.coerceAtMost(20.em).value).isEqualTo(10f)
        assertThat(10.em.coerceAtMost(10.em).value).isEqualTo(10f)
    }

    @Test
    fun lerp() {
        assertThat(lerp(0.em, 10.em, 1f).value).isEqualTo(10f)
        assertThat(lerp(0.em, 10.em, 0f).value).isZero()
        assertThat(lerp(0.em, 10.em, 0.5f).value).isEqualTo(5f)
    }
}