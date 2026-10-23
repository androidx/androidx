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

package androidx.wear.compose.material3

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ColorSchemeTest {

    @Test
    fun complementaryColor_isComplementOfPrimaryWithIncreasedChroma() {
        val primary = Color(0xffeee1c5)
        val colorScheme = ColorScheme(primary = primary)

        val complement = TemperatureCache(Hct.fromInt(primary.toArgb())).complement
        val expected = Hct.from(complement.hue, complement.chroma + 10.0, complement.tone)

        assertNotEquals(Color(complement.toInt()), colorScheme.complementaryColor)
        assertEquals(Color(expected.toInt()), colorScheme.complementaryColor)
    }

    @Test
    fun complementaryColor_copy_recomputesForNewPrimary() {
        val colorScheme = ColorScheme(primary = Color(0xffeee1c5))
        colorScheme.complementaryColor

        val newPrimary = Color(0xffdee3e6)
        val copy = colorScheme.copy(primary = newPrimary)

        val complement = TemperatureCache(Hct.fromInt(newPrimary.toArgb())).complement
        val expected = Hct.from(complement.hue, complement.chroma + 10.0, complement.tone)
        assertNotEquals(Color(complement.toInt()), copy.complementaryColor)
        assertEquals(Color(expected.toInt()), copy.complementaryColor)
    }
}
