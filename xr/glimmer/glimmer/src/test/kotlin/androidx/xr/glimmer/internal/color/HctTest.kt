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
            val resolved = case.input.withToneAndMaxChroma(case.tone)

            assertEquals(
                "Color mismatch for case ${case.input} at tone ${case.tone}.",
                case.expected,
                resolved,
            )
        }
    }

    @Test
    fun withToneAndMaxChroma_preservesAlpha() {
        val originalColor = Color.Red.copy(alpha = 0.45f)
        val resolved = originalColor.withToneAndMaxChroma(60.0f)
        assertEquals(originalColor.alpha, resolved.alpha, 0.001f)
    }

    private data class ColorCase(val input: Color, val tone: Float, val expected: Color)
}
