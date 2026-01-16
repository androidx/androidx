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

import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AngleTest {

    @Test
    fun degreesToRadians() {
        assertThat(Angle.degreesToRadians(180f)).isEqualTo(PI.toFloat())
        assertThat(Angle.degreesToRadians(Angle.ZERO_DEGREES)).isEqualTo(Angle.ZERO_RADIANS)
        assertThat(Angle.degreesToRadians(Angle.HALF_TURN_DEGREES))
            .isEqualTo(Angle.HALF_TURN_RADIANS)
        assertThat(Angle.degreesToRadians(Angle.QUARTER_TURN_DEGREES))
            .isEqualTo(Angle.QUARTER_TURN_RADIANS)
        assertThat(Angle.degreesToRadians(Angle.FULL_TURN_DEGREES))
            .isEqualTo(Angle.FULL_TURN_RADIANS)
    }

    @Test
    fun radiansToDegrees() {
        assertThat(Angle.radiansToDegrees(PI.toFloat())).isEqualTo(180f)
        assertThat(Angle.radiansToDegrees(Angle.ZERO_RADIANS)).isEqualTo(Angle.ZERO_DEGREES)
        assertThat(Angle.radiansToDegrees(Angle.HALF_TURN_RADIANS))
            .isEqualTo(Angle.HALF_TURN_DEGREES)
        assertThat(Angle.radiansToDegrees(Angle.QUARTER_TURN_RADIANS))
            .isEqualTo(Angle.QUARTER_TURN_DEGREES)
        assertThat(Angle.radiansToDegrees(Angle.FULL_TURN_RADIANS))
            .isEqualTo(Angle.FULL_TURN_DEGREES)
    }

    @Test
    fun constants_areCorrect() {
        assertThat(Angle.ZERO_RADIANS).isEqualTo(0f)
        assertThat(Angle.HALF_TURN_RADIANS).isEqualTo(PI.toFloat())
        assertThat(Angle.FULL_TURN_RADIANS).isEqualTo((PI * 2).toFloat())
        assertThat(Angle.QUARTER_TURN_RADIANS).isEqualTo((PI / 2).toFloat())
    }

    @Test
    fun normalizedRadians_returnsValueFromJni() {
        assertThat(Angle.normalizedRadians(Angle.ZERO_RADIANS)).isEqualTo(0f)
        assertThat(Angle.normalizedRadians(-Angle.HALF_TURN_RADIANS))
            .isWithin(1e-6F)
            .of(PI.toFloat())
    }

    @Test
    fun normalizedAboutZeroRadians_returnsValueFromJni() {
        assertThat(Angle.normalizedAboutZeroRadians(Angle.ZERO_RADIANS)).isEqualTo(0f)
        assertThat(
                Angle.normalizedAboutZeroRadians(
                    Angle.FULL_TURN_RADIANS - Angle.QUARTER_TURN_RADIANS
                )
            )
            .isWithin(1e-6F)
            .of(-PI.toFloat() / 2F)
    }

    @Test
    fun normalizedDegrees_returnsValueFromJni() {
        assertThat(Angle.normalizedDegrees(Angle.ZERO_DEGREES)).isEqualTo(0f)
        assertThat(Angle.normalizedDegrees(-Angle.HALF_TURN_DEGREES)).isEqualTo(180f)
    }

    @Test
    fun normalizedAboutZeroDegrees_returnsValueFromJni() {
        assertThat(Angle.normalizedAboutZeroDegrees(Angle.ZERO_DEGREES)).isEqualTo(0f)
        assertThat(
                Angle.normalizedAboutZeroDegrees(
                    Angle.FULL_TURN_DEGREES - Angle.QUARTER_TURN_DEGREES
                )
            )
            .isWithin(1e-4F)
            .of(-90f)
    }
}
