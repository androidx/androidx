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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DiscreteRotationTests {
    private val ROTATION_0 = DiscreteRotation.from(0)
    private val ROTATION_90 = DiscreteRotation.from(90)
    private val ROTATION_180 = DiscreteRotation.from(180)
    private val ROTATION_270 = DiscreteRotation.from(270)

    @Test
    fun discreteRotationCanRoundIntDegrees() {
        // Standard degrees should map exactly to their corresponding values.
        assertThat(DiscreteRotation.round(0).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(90).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(180).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(270).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(360).degrees).isEqualTo(0)

        // Negative (CCW)
        assertThat(DiscreteRotation.round(-90).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-180).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-270).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-360).degrees).isEqualTo(0)

        // 15 degrees off
        assertThat(DiscreteRotation.round(15).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(105).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(195).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(285).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(375).degrees).isEqualTo(0)

        // -15 degrees off
        assertThat(DiscreteRotation.round(-15).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(75).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(165).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(255).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(345).degrees).isEqualTo(0)

        // +44 degree increments
        assertThat(DiscreteRotation.round(44).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(134).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(224).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(314).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(404).degrees).isEqualTo(0)

        // +45 degree increments
        assertThat(DiscreteRotation.round(45).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(135).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(225).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(315).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(405).degrees).isEqualTo(90)

        // -45 degree increments
        assertThat(DiscreteRotation.round(-45).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-135).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-225).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-315).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-405).degrees).isEqualTo(0)

        // -46 degree increments
        assertThat(DiscreteRotation.round(-46).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-136).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-226).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-316).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-406).degrees).isEqualTo(270)
    }

    @Test
    fun discreteRotationCanRoundFloatDegrees() {
        // Standard degrees should map exactly to their corresponding values.
        assertThat(DiscreteRotation.round(0f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(90f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(180f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(270f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(360f).degrees).isEqualTo(0)

        // Negative (CCW)
        assertThat(DiscreteRotation.round(-90f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-180f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-270f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-360f).degrees).isEqualTo(0)

        // 15 degrees off
        assertThat(DiscreteRotation.round(15f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(105f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(195f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(285f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(375f).degrees).isEqualTo(0)

        // -15 degrees off
        assertThat(DiscreteRotation.round(-15f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(75f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(165f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(255f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(345f).degrees).isEqualTo(0)

        // +44 degree increments
        assertThat(DiscreteRotation.round(44f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(134f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(224f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(314f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(404f).degrees).isEqualTo(0)

        // +45 degree increments
        assertThat(DiscreteRotation.round(45f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(135f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(225f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(315f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(405f).degrees).isEqualTo(90)

        // -45 degree increments
        assertThat(DiscreteRotation.round(-45f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-135f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-225f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-315f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-405f).degrees).isEqualTo(0)

        // -46 degree increments
        assertThat(DiscreteRotation.round(-46f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-136f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-226f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-316f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-406f).degrees).isEqualTo(270)

        // Floating point specific rounding tests
        assertThat(DiscreteRotation.round(44.9999f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(45.0001f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(134.9999f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(135.0001f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(224.9999f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(225.0001f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(314.9999f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(315.0001f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(404.9999f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(405.0001f).degrees).isEqualTo(90)

        assertThat(DiscreteRotation.round(-44.9999f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-45.0001f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-134.9999f).degrees).isEqualTo(270)
        assertThat(DiscreteRotation.round(-135.0001f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-224.9999f).degrees).isEqualTo(180)
        assertThat(DiscreteRotation.round(-225.0001f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-314.9999f).degrees).isEqualTo(90)
        assertThat(DiscreteRotation.round(-315.0001f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-404.9999f).degrees).isEqualTo(0)
        assertThat(DiscreteRotation.round(-405.0001f).degrees).isEqualTo(270)
    }

    @Test
    fun discreteRotationCanBeCreatedFromSurfaceRotation() {
        assertThat(DiscreteRotation.fromSurfaceRotation(Surface.ROTATION_0))
            .isEqualTo(DiscreteRotation.from(0))
        assertThat(DiscreteRotation.fromSurfaceRotation(Surface.ROTATION_90))
            .isEqualTo(DiscreteRotation.from(90))
        assertThat(DiscreteRotation.fromSurfaceRotation(Surface.ROTATION_180))
            .isEqualTo(DiscreteRotation.from(180))
        assertThat(DiscreteRotation.fromSurfaceRotation(Surface.ROTATION_270))
            .isEqualTo(DiscreteRotation.from(270))
    }

    @Test(expected = IllegalArgumentException::class)
    fun discreteRotationThrowsOnInvalidSurfaceRotation() {
        DiscreteRotation.fromSurfaceRotation(42)
    }

    @Test
    fun discreteRotationCanBeAdded() {
        assertThat(ROTATION_0 + ROTATION_90).isEqualTo(ROTATION_90)
        assertThat(ROTATION_90 + ROTATION_90).isEqualTo(ROTATION_180)
        assertThat(ROTATION_180 + ROTATION_90).isEqualTo(ROTATION_270)
        assertThat(ROTATION_270 + ROTATION_90).isEqualTo(ROTATION_0)

        assertThat(ROTATION_0 + 90).isEqualTo(ROTATION_90)
        assertThat(ROTATION_90 + 90).isEqualTo(ROTATION_180)
    }

    @Test
    fun discreteRotationCanBeSubtracted() {
        assertThat(ROTATION_90 - ROTATION_90).isEqualTo(ROTATION_0)
        assertThat(ROTATION_180 - ROTATION_90).isEqualTo(ROTATION_90)
        assertThat(ROTATION_270 - ROTATION_90).isEqualTo(ROTATION_180)
        assertThat(ROTATION_0 - ROTATION_90).isEqualTo(ROTATION_270)

        assertThat(ROTATION_90 - 90).isEqualTo(ROTATION_0)
        assertThat(ROTATION_0 - 90).isEqualTo(ROTATION_270)
    }

    @Test(expected = IllegalArgumentException::class)
    fun discreteRotationThrowsOnInvalidInt() {
        DiscreteRotation.from(45)
    }

    @Test
    fun discreteRotationHasToString() {
        assertThat(ROTATION_0.toString()).isEqualTo("0°")
        assertThat(ROTATION_90.toString()).isEqualTo("90°")
        assertThat(ROTATION_180.toString()).isEqualTo("180°")
        assertThat(ROTATION_270.toString()).isEqualTo("270°")
    }
}
