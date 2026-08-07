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
    fun withToneAndMaxChroma_resolvesToCorrectColor() {
        val testCases =
            listOf(
                // Input Color, Target Tone, Expected Color
                ColorCase(Color.Red, 50.0f, Color(0xFFEF0000)),
                ColorCase(Color.Green, 70.0f, Color(0xFF03C700)),
                ColorCase(Color.Blue, 30.0f, Color(0xFF0000EF)),
                ColorCase(Color.Yellow, 80.0f, Color(0xFFCDCD00)),
                ColorCase(Color.Cyan, 60.0f, Color(0xFF00A1A1)),
                ColorCase(Color.Magenta, 40.0f, Color(0xFFA900A9)),
                ColorCase(Color(0xFF34E0A1), 100.0f, Color.White),
                ColorCase(Color(0xFF34E0A1), 85.0f, Color(0xFF00F1AA)),
                ColorCase(Color(0xFF34E0A1), 69.0f, Color(0xFF00C086)),
                ColorCase(Color(0xFF34E0A1), 77.0f, Color(0xFF00D898)),
            )

        for (case in testCases) {
            val resolved = case.input.withToneAndChroma(newTone = case.tone)

            assertEquals(
                "Color mismatch for case ${case.input} at tone ${case.tone}.",
                case.expected,
                resolved,
            )
        }
    }

    @Test
    fun withToneAndChroma_defaultMaxChroma_preservesAlpha() {
        val originalColor = Color.Red.copy(alpha = 0.45f)
        val resolved = originalColor.withToneAndChroma(newTone = 60.0f)
        assertEquals(originalColor.alpha, resolved.alpha, 0.001f)
    }

    @Test
    fun withToneAndChroma_resolvesToCorrectColor() {
        val originalColor = Color(0xFF9BBFFF) // Primary color default
        val resolved = originalColor.withToneAndChroma(newChroma = 29.0f, newTone = 33.0f)
        // Ensure hue is preserved while chroma is changed to 29 and tone is set to 33
        val argb = resolved.toArgb()
        val originalHue = HctUtils.argbToHue(originalColor.toArgb())
        val resolvedHue = HctUtils.argbToHue(argb)
        val resolvedTone = HctUtils.argbToTone(argb)

        assertEquals("Hue should be preserved within tolerance", originalHue, resolvedHue, 1.0)
        assertEquals("Tone should match 33 within tolerance", 33.0, resolvedTone, 1.0)
    }

    @Test
    fun withToneAndChroma_preservesAlpha() {
        val originalColor = Color(0xFF9BBFFF).copy(alpha = 0.6f)
        val resolved = originalColor.withToneAndChroma(newChroma = 29.0f, newTone = 33.0f)
        assertEquals(originalColor.alpha, resolved.alpha, 0.001f)
    }

    @Test
    fun withToneAndChroma_specificColor_resolvesToExpectedColor() {
        val input = Color(0xFF34E0A1)
        val expected = Color(0xFF245740)
        val resolved = input.withToneAndChroma(newChroma = 29.0f, newTone = 33.0f)
        assertEquals(expected, resolved)
    }

    private data class ColorCase(val input: Color, val tone: Float, val expected: Color)
}
