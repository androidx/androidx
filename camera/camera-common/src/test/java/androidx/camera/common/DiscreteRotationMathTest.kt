/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.view.Surface
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscreteRotationMathTest {

    @Test
    fun discreteRotationCanRoundIntDegrees() {
        // Standard degrees should map exactly to their corresponding values.
        assertThat(DiscreteRotationMath.round(0)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(90)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(180)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(270)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(360)).isEqualTo(0)

        // Negative (CCW)
        assertThat(DiscreteRotationMath.round(-90)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-180)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-270)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-360)).isEqualTo(0)

        // 15 degrees off
        assertThat(DiscreteRotationMath.round(15)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(105)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(195)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(285)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(375)).isEqualTo(0)

        // -15 degrees off
        assertThat(DiscreteRotationMath.round(-15)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(75)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(165)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(255)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(345)).isEqualTo(0)

        // +44 degree increments
        assertThat(DiscreteRotationMath.round(44)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(134)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(224)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(314)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(404)).isEqualTo(0)

        // +45 degree increments
        assertThat(DiscreteRotationMath.round(45)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(135)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(225)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(315)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(405)).isEqualTo(90)

        // -45 degree increments
        assertThat(DiscreteRotationMath.round(-45)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-135)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-225)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-315)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-405)).isEqualTo(0)

        // -46 degree increments
        assertThat(DiscreteRotationMath.round(-46)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-136)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-226)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-316)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-406)).isEqualTo(270)
    }

    @Test
    fun discreteRotationCanRoundFloatDegrees() {
        // Standard degrees should map exactly to their corresponding values.
        assertThat(DiscreteRotationMath.round(0f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(90f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(180f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(270f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(360f)).isEqualTo(0)

        // Negative (CCW)
        assertThat(DiscreteRotationMath.round(-90f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-180f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-270f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-360f)).isEqualTo(0)

        // 15 degrees off
        assertThat(DiscreteRotationMath.round(15f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(105f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(195f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(285f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(375f)).isEqualTo(0)

        // -15 degrees off
        assertThat(DiscreteRotationMath.round(-15f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(75f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(165f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(255f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(345f)).isEqualTo(0)

        // +44 degree increments
        assertThat(DiscreteRotationMath.round(44f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(134f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(224f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(314f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(404f)).isEqualTo(0)

        // +45 degree increments
        assertThat(DiscreteRotationMath.round(45f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(135f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(225f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(315f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(405f)).isEqualTo(90)

        // -45 degree increments
        assertThat(DiscreteRotationMath.round(-45f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-135f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-225f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-315f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-405f)).isEqualTo(0)

        // -46 degree increments
        assertThat(DiscreteRotationMath.round(-46f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-136f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-226f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-316f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-406f)).isEqualTo(270)

        // Floating point specific rounding tests
        assertThat(DiscreteRotationMath.round(44.9999f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(45.0001f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(134.9999f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(135.0001f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(224.9999f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(225.0001f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(314.9999f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(315.0001f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(404.9999f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(405.0001f)).isEqualTo(90)

        assertThat(DiscreteRotationMath.round(-44.9999f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-45.0001f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-134.9999f)).isEqualTo(270)
        assertThat(DiscreteRotationMath.round(-135.0001f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-224.9999f)).isEqualTo(180)
        assertThat(DiscreteRotationMath.round(-225.0001f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-314.9999f)).isEqualTo(90)
        assertThat(DiscreteRotationMath.round(-315.0001f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-404.9999f)).isEqualTo(0)
        assertThat(DiscreteRotationMath.round(-405.0001f)).isEqualTo(270)
    }

    @Test
    fun discreteRotationCanBeCreatedFromSurfaceRotation() {
        assertThat(DiscreteRotationMath.fromSurfaceRotation(Surface.ROTATION_0)).isEqualTo(0)
        assertThat(DiscreteRotationMath.fromSurfaceRotation(Surface.ROTATION_90)).isEqualTo(90)
        assertThat(DiscreteRotationMath.fromSurfaceRotation(Surface.ROTATION_180)).isEqualTo(180)
        assertThat(DiscreteRotationMath.fromSurfaceRotation(Surface.ROTATION_270)).isEqualTo(270)
    }
}
