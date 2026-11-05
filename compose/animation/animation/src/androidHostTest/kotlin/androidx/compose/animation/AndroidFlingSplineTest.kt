/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AndroidFlingSplineTest {

    @Test
    fun testClampedOutOfRangePosition() {
        val controlStart = AndroidFlingSpline.flingPosition(0f)
        val controlEnd = AndroidFlingSpline.flingPosition(1f)

        assertEquals(controlStart, AndroidFlingSpline.flingPosition(-10f))
        assertEquals(controlStart, AndroidFlingSpline.flingPosition(-1f))
        assertEquals(controlStart, AndroidFlingSpline.flingPosition(-0.06f))

        assertEquals(controlEnd, AndroidFlingSpline.flingPosition(1.5f))
        assertEquals(controlEnd, AndroidFlingSpline.flingPosition(10f))
    }

    @Test
    fun flingCalculatorNanosecondPrecision() {
        val calculator = FlingCalculator(friction = 0.015f, density = Density(1f))
        val info = calculator.flingInfo(1000f)
        val durationNanos = calculator.flingDurationNanos(1000f)
        val posAt0 = info.positionFromNanos(0L)
        val posAtHalfMs = info.positionFromNanos(500_000L)
        val velAt0 = info.velocityFromNanos(0L)
        val velAfterFirstSegment = info.velocityFromNanos(durationNanos / 100 + 100L)

        assertTrue(posAtHalfMs > posAt0)
        assertTrue(velAfterFirstSegment < velAt0)
        assertTrue(durationNanos % 1_000_000L != 0L)
        assertEquals(durationNanos, info.duration)
        assertEquals(calculator.flingDuration(1000f), durationNanos / 1_000_000L)
        assertEquals(velAt0, info.velocity(0L), 1f)
    }
}
