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

@file:Suppress("KotlinConstantConditions")

package androidx.compose.ui.util

import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.cbrt
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MathHelpersTest {
    // `f = 16777216f` is the first value where `f + 1 == f` due to float imprecision, so that's
    // where testing floating point errors becomes interesting
    private val testStart = 16777216L
    private val testEnd = testStart + 1000

    @Test
    fun testLerpLargeFloats() {
        val from = 1f
        for (x in testStart until testEnd) {
            val to = x.toFloat()
            assertThat(lerp(from, to, 0f)).isEqualTo(from)
            assertThat(lerp(from, to, 1f)).isEqualTo(to)
        }
    }

    @Test
    fun testLerpLargeInts() {
        val from = 1
        for (x in testStart until testEnd) {
            val to = x.toInt()
            assertThat(lerp(from, to, 0f)).isEqualTo(from)
            assertThat(lerp(from, to, 1f)).isEqualTo(to)
        }
    }

    @Test
    fun testLerpLargeLongs() {
        val from = 1L
        for (to in testStart until testEnd) {
            assertThat(lerp(from, to, 0f)).isEqualTo(from)
            assertThat(lerp(from, to, 1f)).isEqualTo(to)
        }
    }

    @Test
    fun testLerpSimpleFloats() {
        val from = 0f
        for (multiplier in 1..1000) {
            val to = (4 * multiplier).toFloat()
            assertThat(lerp(from, to, 0.00f)).isEqualTo((0 * multiplier).toFloat())
            assertThat(lerp(from, to, 0.25f)).isEqualTo((1 * multiplier).toFloat())
            assertThat(lerp(from, to, 0.50f)).isEqualTo((2 * multiplier).toFloat())
            assertThat(lerp(from, to, 0.75f)).isEqualTo((3 * multiplier).toFloat())
            assertThat(lerp(from, to, 1.00f)).isEqualTo((4 * multiplier).toFloat())
        }
    }

    @Test
    fun testLerpSimpleInts() {
        val from = 0
        for (multiplier in 1..1000) {
            val to = (4 * multiplier)
            assertThat(lerp(from, to, 0.00f)).isEqualTo((0 * multiplier))
            assertThat(lerp(from, to, 0.25f)).isEqualTo((1 * multiplier))
            assertThat(lerp(from, to, 0.50f)).isEqualTo((2 * multiplier))
            assertThat(lerp(from, to, 0.75f)).isEqualTo((3 * multiplier))
            assertThat(lerp(from, to, 1.00f)).isEqualTo((4 * multiplier))
        }
    }

    @Test
    fun testLerpSimpleLongs() {
        val from = 0L
        for (multiplier in 1..1000) {
            val to = (4 * multiplier).toLong()
            assertThat(lerp(from, to, 0.00f)).isEqualTo((0 * multiplier).toLong())
            assertThat(lerp(from, to, 0.25f)).isEqualTo((1 * multiplier).toLong())
            assertThat(lerp(from, to, 0.50f)).isEqualTo((2 * multiplier).toLong())
            assertThat(lerp(from, to, 0.75f)).isEqualTo((3 * multiplier).toLong())
            assertThat(lerp(from, to, 1.00f)).isEqualTo((4 * multiplier).toLong())
        }
    }

    @Test
    fun testNegativeFastCbrt() {
        for (i in 0..65_536) {
            val v = i / 8_192.0f // v is in the range 0f..8f
            // We do an == test ourselves to avoid any fuzzy float comparison
            assertTrue(-fastCbrt(v) == fastCbrt(-v))
        }
    }

    @Test
    fun testNonFiniteFastCbrt() {
        assertTrue(fastCbrt(Float.NaN).isNaN())
        assertTrue(fastCbrt(Float.POSITIVE_INFINITY).isNaN())
        assertTrue(fastCbrt(Float.NEGATIVE_INFINITY).isNaN())
    }

    @Test
    fun testZeroFastCbrt() {
        val zeroError = 8.35E-7f
        assertTrue(fastCbrt(0.0f) <= zeroError)
        assertTrue(fastCbrt(-0.0f) >= -zeroError)
    }

    @Test
    fun testFastCbrtError() {
        val maxError = 1.76E-6f
        for (i in 0..65_536) {
            val v = i / 8_192.0f // v is in the range 0f..8f
            val error = abs(fastCbrt(v) - cbrt(v))
            assertTrue(error <= maxError)
        }
    }
}
