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
        assertThat(Angle.degreesToRadians(180f)).isEqualTo(Math.PI.toFloat())
    }

    @Test
    fun radiansToDegrees() {
        assertThat(Angle.radiansToDegrees(Math.PI.toFloat())).isEqualTo(180f)
    }

    @Test
    fun constants_areCorrect() {
        assertThat(Angle.ZERO).isEqualTo(0f)
        assertThat(Angle.HALF_TURN_RADIANS).isEqualTo(Math.PI.toFloat())
        assertThat(Angle.FULL_TURN_RADIANS).isEqualTo((Math.PI * 2).toFloat())
        assertThat(Angle.QUARTER_TURN_RADIANS).isEqualTo((Math.PI / 2).toFloat())
    }

    @Test
    fun normalized_returnsValueFromJni() {
        assertThat(Angle.normalized(Angle.ZERO)).isEqualTo(0f)
        assertThat(Angle.normalized(-Angle.HALF_TURN_RADIANS)).isWithin(1e-6F).of(Math.PI.toFloat())
    }

    @Test
    fun normalizedAboutZero_returnsValueFromJni() {
        assertThat(Angle.normalizedAboutZero(Angle.ZERO)).isEqualTo(0f)
        assertThat(Angle.normalizedAboutZero(Angle.FULL_TURN_RADIANS - Angle.QUARTER_TURN_RADIANS))
            .isWithin(1e-6F)
            .of(-Math.PI.toFloat() / 2F)
    }
}
