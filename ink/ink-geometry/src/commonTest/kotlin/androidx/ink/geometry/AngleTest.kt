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

package androidx.ink.geometry

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class AngleTest {

    @Test
    fun degreesToRadians() {
        assertEquals(Angle.degreesToRadians(180f), PI.toFloat())
        assertEquals(Angle.degreesToRadians(Angle.ZERO_DEGREES), Angle.ZERO_RADIANS)
        assertEquals(Angle.degreesToRadians(Angle.HALF_TURN_DEGREES), Angle.HALF_TURN_RADIANS)
        assertEquals(Angle.degreesToRadians(Angle.QUARTER_TURN_DEGREES), Angle.QUARTER_TURN_RADIANS)
        assertEquals(Angle.degreesToRadians(Angle.FULL_TURN_DEGREES), Angle.FULL_TURN_RADIANS)
    }

    @Test
    fun radiansToDegrees() {
        assertEquals(Angle.radiansToDegrees(PI.toFloat()), 180f)
        assertEquals(Angle.radiansToDegrees(Angle.ZERO_RADIANS), Angle.ZERO_DEGREES)
        assertEquals(Angle.radiansToDegrees(Angle.HALF_TURN_RADIANS), Angle.HALF_TURN_DEGREES)
        assertEquals(Angle.radiansToDegrees(Angle.QUARTER_TURN_RADIANS), Angle.QUARTER_TURN_DEGREES)
        assertEquals(Angle.radiansToDegrees(Angle.FULL_TURN_RADIANS), Angle.FULL_TURN_DEGREES)
    }

    @Test
    fun constants_areCorrect() {
        assertEquals(Angle.ZERO_RADIANS, 0f)
        assertEquals(Angle.HALF_TURN_RADIANS, PI.toFloat())
        assertEquals(Angle.FULL_TURN_RADIANS, (PI * 2).toFloat())
        assertEquals(Angle.QUARTER_TURN_RADIANS, (PI / 2).toFloat())
    }

    @Test
    fun normalizedRadians_returnsValueFromJni() {
        assertEquals(Angle.normalizedRadians(Angle.ZERO_RADIANS), 0f)
        assertEquals(Angle.normalizedRadians(-Angle.HALF_TURN_RADIANS), PI.toFloat())
    }

    @Test
    fun normalizedAboutZeroRadians_returnsValueFromJni() {
        assertEquals(Angle.normalizedAboutZeroRadians(Angle.ZERO_RADIANS), 0f)
        assertEquals(
            Angle.normalizedAboutZeroRadians(Angle.FULL_TURN_RADIANS - Angle.QUARTER_TURN_RADIANS),
            -PI.toFloat() / 2F,
            1e-6f,
        )
    }

    @Test
    fun normalizedDegrees_returnsValueFromJni() {
        assertEquals(Angle.normalizedDegrees(Angle.ZERO_DEGREES), 0f)
        assertEquals(Angle.normalizedDegrees(-Angle.HALF_TURN_DEGREES), 180f)
    }

    @Test
    fun normalizedAboutZeroDegrees_returnsValueFromJni() {
        assertEquals(Angle.normalizedAboutZeroDegrees(Angle.ZERO_DEGREES), 0f)
        assertEquals(
            Angle.normalizedAboutZeroDegrees(Angle.FULL_TURN_DEGREES - Angle.QUARTER_TURN_DEGREES),
            -90f,
            1e-4f,
        )
    }
}
