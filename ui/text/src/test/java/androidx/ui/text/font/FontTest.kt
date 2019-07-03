/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.text.font

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontTest {

    @Test(expected = AssertionError::class)
    fun `cannot be instantiated with empty name`() {
        Font(name = "")
    }

    @Test
    fun `default values`() {
        val font = Font(name = "fontName")
        assertThat(font.weight).isEqualTo(FontWeight.normal)
        assertThat(font.style).isEqualTo(FontStyle.Normal)
        assertThat(font.ttcIndex).isEqualTo(0)
        assertThat(font.fontVariationSettings).isEmpty()
    }

    @Test
    fun `two equal font declarations are equal`() {
        val font = Font(
            name = "fontName",
            weight = FontWeight.w900,
            style = FontStyle.Italic,
            ttcIndex = 1,
            fontVariationSettings = "'wdth' 150"
        )

        val otherFont = Font(
            name = "fontName",
            weight = FontWeight.w900,
            style = FontStyle.Italic,
            ttcIndex = 1,
            fontVariationSettings = "'wdth' 150"
        )

        assertThat(font).isEqualTo(otherFont)
    }

    @Test
    fun `two non equal font declarations are not equal`() {
        val font = Font(
            name = "fontName",
            weight = FontWeight.w900,
            style = FontStyle.Italic,
            ttcIndex = 1,
            fontVariationSettings = "'wdth' 150"
        )

        val otherFont = Font(
            name = "fontName",
            weight = FontWeight.w900,
            style = FontStyle.Italic,
            ttcIndex = 1,
            fontVariationSettings = "'wdth' 151" // this is different
        )

        assertThat(font).isNotEqualTo(otherFont)
    }

    @Test
    fun `asFontFamilyList returns a FontFamily`() {
        val font = Font(
            name = "fontName1",
            weight = FontWeight.w900,
            style = FontStyle.Italic
        )

        val fontFamily = font.asFontFamily()

        assertThat(fontFamily).isNotNull()
        assertThat(fontFamily).isNotEmpty()
        assertThat(fontFamily[0]).isSameInstanceAs(font)
    }
}