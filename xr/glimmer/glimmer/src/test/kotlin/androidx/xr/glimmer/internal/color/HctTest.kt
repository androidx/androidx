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

package androidx.xr.glimmer.internal.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HctTest {

    @Test
    fun withTone_resolvesToCorrectColor() {
        val testCases =
            listOf(
                // Input Color, Target Tone, Expected Color
                ColorCase(Color.Black, 34.0f, Color(0xFF505050)),
                ColorCase(Color.DarkGray, 34.0f, Color(0xFF505050)),
                ColorCase(Color.Gray, 34.0f, Color(0xFF4F5050)),
                ColorCase(Color.LightGray, 34.0f, Color(0xFF4F5050)),
                ColorCase(Color.White, 34.0f, Color(0xFF4E5051)),
                ColorCase(Color.Red, 34.0f, Color(0xFFA50100)),
                ColorCase(Color.Green, 70.0f, Color(0xFF03C700)),
                ColorCase(Color.Blue, 30.0f, Color(0xFF0000EF)),
                ColorCase(Color.Yellow, 80.0f, Color(0xFFCDCD00)),
                ColorCase(Color.Cyan, 60.0f, Color(0xFF00A1A1)),
                ColorCase(Color.Magenta, 40.0f, Color(0xFFA900A9)),
                ColorCase(Color(0xFF303030), 34.0f, Color(0xFF505050)),
                ColorCase(Color(0xFF34E0A1), 34.0f, Color(0xFF005C3E)),
                ColorCase(Color(0xFFD93B1A), 34.0f, Color(0xFF9D1C00)),
                ColorCase(Color(0xFF887DC3), 34.0f, Color(0xFF514788)),
                ColorCase(Color(0xFF34E0A1), 100.0f, Color.White),
                ColorCase(Color(0xFF34E0A1), 85.0f, Color(0xFF49EFAE)),
                ColorCase(Color(0xFF34E0A1), 69.0f, Color(0xFF00C086)),
                ColorCase(Color(0xFF34E0A1), 77.0f, Color(0xFF24D799)),
                ColorCase(Color(0xFF303030), 20.0f, Color(0xFF303030)),
            )

        for (case in testCases) {
            val resolved = case.input.withTone(newTone = case.tone)

            assertEquals(
                "Color mismatch for case ${case.input} at tone ${case.tone}.",
                case.expected,
                resolved,
            )
        }
    }

    @Test
    fun withTone_preservesHueAndChroma() {
        val originalColor = Color(0xFF9BBFFF) // Primary color default
        val resolved = originalColor.withTone(newTone = 33.0f)
        val argb = resolved.toArgb()
        val originalHueAndChroma = HctUtils.argbToHueAndChroma(originalColor.toArgb())
        val resolvedHueAndChroma = HctUtils.argbToHueAndChroma(argb)
        val resolvedTone = HctUtils.argbToTone(argb)

        assertEquals(
            "Hue should be preserved within tolerance",
            originalHueAndChroma.hue,
            resolvedHueAndChroma.hue,
            1.0,
        )
        assertEquals(
            "Chroma should be preserved within tolerance",
            originalHueAndChroma.chroma,
            resolvedHueAndChroma.chroma,
            1.0,
        )
        assertEquals("Tone should match 33 within tolerance", 33.0, resolvedTone, 1.0)
    }

    @Test
    fun withTone_preservesAlpha() {
        val originalColor = Color(0xFF9BBFFF).copy(alpha = 0.6f)
        val resolved = originalColor.withTone(newTone = 33.0f)
        assertEquals(originalColor.alpha, resolved.alpha, 0.001f)
    }

    @Test
    fun hueAndChroma_packingIntegrity() {
        val testCases = listOf(0.0 to 0.0, 360.0 to 0.0, 180.5 to 75.25, 240.0 to 120.0)
        for ((hue, chroma) in testCases) {
            val packed = HueAndChroma.create(hue, chroma)
            assertEquals(hue, packed.hue, 0.001)
            assertEquals(chroma, packed.chroma, 0.001)
        }
    }

    private data class ColorCase(val input: Color, val tone: Float, val expected: Color)
}
