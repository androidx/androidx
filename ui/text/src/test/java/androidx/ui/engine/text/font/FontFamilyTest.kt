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
package androidx.ui.engine.text.font

import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontFamilyTest {

    @Test(expected = AssertionError::class)
    fun `cannot be instantiated with empty font list`() {
        FontFamily(listOf())
    }

    @Test
    fun `two equal family declarations are equal`() {
        val fontFamily = FontFamily(
            Font(
                name = "fontName",
                weight = FontWeight.w900,
                style = FontStyle.Italic,
                ttcIndex = 1,
                fontVariationSettings = "'wdth' 150"
            )
        )

        val otherFontFamily = FontFamily(
            Font(
                name = "fontName",
                weight = FontWeight.w900,
                style = FontStyle.Italic,
                ttcIndex = 1,
                fontVariationSettings = "'wdth' 150"
            )
        )

        assertThat(fontFamily).isEqualTo(otherFontFamily)
    }

    @Test
    fun `two non equal family declarations are not equal`() {
        val fontFamily = FontFamily(
            Font(
                name = "fontName",
                weight = FontWeight.w900,
                style = FontStyle.Italic,
                ttcIndex = 1,
                fontVariationSettings = "'wdth' 150"
            )
        )

        val otherFontFamily = FontFamily(
            Font(
                name = "fontName",
                weight = FontWeight.w900,
                style = FontStyle.Italic,
                ttcIndex = 1,
                fontVariationSettings = "'wdth' 151" // this is different
            )
        )

        assertThat(fontFamily).isNotEqualTo(otherFontFamily)
    }

    @Test(expected = AssertionError::class)
    fun `cannot add two fonts that have the same FontWeight and FontStyle`() {
        FontFamily(
            Font(
                name = "fontName1",
                weight = FontWeight.w900,
                style = FontStyle.Italic
            ),
            Font(
                name = "fontName2",
                weight = FontWeight.w900,
                style = FontStyle.Italic
            )
        )
    }

    @Test
    fun `can create FontFamily with genericFamily even though there are no custom fonts`() {
        // main expectation is that there should be no exception
        val fontFamily = FontFamily(genericFamily = "sans-serif")
        assertThat(fontFamily.genericFamily).isEqualTo("sans-serif")
    }
}