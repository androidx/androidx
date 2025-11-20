/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CurvedTextStyleTest {
    @Test
    fun `constructor with default values`() {
        val style = CurvedTextStyle()

        assertEquals(style.color, Color.Unspecified)
        assertTrue(style.fontSize.isUnspecified)
        assertEquals(style.background, Color.Unspecified)
        assertNull(style.fontWeight)
        assertNull(style.fontFamily)
        assertNull(style.fontStyle)
        assertNull(style.fontSynthesis)
        assertTrue(style.letterSpacing.isUnspecified)
        assertTrue(style.letterSpacingCounterClockwise.isUnspecified)
        assertTrue(style.lineHeight.isUnspecified)
        assertTrue(style.warpOffset.isUnspecified)
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color.Red

        val style = CurvedTextStyle(color = color)

        assertEquals(style.color, color)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val style = CurvedTextStyle(fontSize = fontSize)

        assertEquals(style.fontSize, fontSize)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color.Red

        val style = CurvedTextStyle(background = color)

        assertEquals(style.background, color)
    }

    @Test
    fun `constructor with customized font weight`() {
        val fontWeight = FontWeight.Bold

        val style = CurvedTextStyle(fontWeight = fontWeight)

        assertEquals(style.fontWeight, fontWeight)
    }

    @Test
    fun `constructor with customized font family`() {
        val fontFamily = FontFamily.Cursive

        val style = CurvedTextStyle(fontFamily = fontFamily)

        assertEquals(style.fontFamily, fontFamily)
    }

    @Test
    fun `constructor with customized font style`() {
        val fontStyle = FontStyle.Italic

        val style = CurvedTextStyle(fontStyle = fontStyle)

        assertEquals(style.fontStyle, fontStyle)
    }

    @Test
    fun `constructor with customized font synthesis`() {
        val fontSynthesis = FontSynthesis.Style

        val style = CurvedTextStyle(fontSynthesis = fontSynthesis)

        assertEquals(style.fontSynthesis, fontSynthesis)
    }

    @Test
    fun `constructor with customized letter spacing`() {
        val letterSpacing = 0.01f.em

        val style = CurvedTextStyle(letterSpacing = letterSpacing)

        assertEquals(style.letterSpacing, letterSpacing)
    }

    @Test
    fun `constructor with customized letter spacing counterclockwise`() {
        val letterSpacingCounterClockwise = 0.01f.em

        val style = CurvedTextStyle(letterSpacingCounterClockwise = letterSpacingCounterClockwise)

        assertEquals(style.letterSpacingCounterClockwise, letterSpacingCounterClockwise)
    }

    @Test
    fun `constructor with customized line height`() {
        val lineHeight = 18.sp

        val style = CurvedTextStyle(lineHeight = lineHeight)

        assertEquals(style.lineHeight, lineHeight)
    }

    @Test
    fun `constructor with customized warping`() {
        val warpOffset = CurvedTextStyle.WarpOffset.Descent

        val style = CurvedTextStyle(warpOffset = warpOffset)

        assertEquals(style.warpOffset, warpOffset)
    }

    @Test
    fun `merge with empty other should return this`() {
        val style = CurvedTextStyle()

        val newStyle = style.merge()

        assertEquals(newStyle, style)
    }

    @Test
    fun `merge with other's color is unspecified should use this' color`() {
        val style = CurvedTextStyle(color = Color.Red)

        val newStyle = style.merge(CurvedTextStyle(color = Color.Unspecified))

        assertEquals(newStyle.color, style.color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val style = CurvedTextStyle(color = Color.Red)
        val otherStyle = CurvedTextStyle(color = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.color, otherStyle.color)
    }

    @Test
    fun `merge with other's fontSize is unspecified should use this' fontSize`() {
        val style = CurvedTextStyle(fontSize = 3.5.sp)

        val newStyle = style.merge(CurvedTextStyle(fontSize = TextUnit.Unspecified))

        assertEquals(newStyle.fontSize, style.fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val style = CurvedTextStyle(fontSize = 3.5.sp)
        val otherStyle = CurvedTextStyle(fontSize = 8.7.sp)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontSize, otherStyle.fontSize)
    }

    @Test
    fun `merge with other's background is unspecified should use this' background`() {
        val style = CurvedTextStyle(background = Color.Red)

        val newStyle = style.merge(CurvedTextStyle(background = Color.Unspecified))

        assertEquals(newStyle.background, style.background)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val style = CurvedTextStyle(background = Color.Red)
        val otherStyle = CurvedTextStyle(background = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.background, otherStyle.background)
    }

    @Test
    fun `merge with other's font weight is unspecified should use this' font weight`() {
        val style = CurvedTextStyle(fontWeight = FontWeight.ExtraBold)

        val newStyle = style.merge(CurvedTextStyle(fontWeight = null))

        assertEquals(newStyle.fontWeight, style.fontWeight)
    }

    @Test
    fun `merge with other's font weight is set should use other's font weight`() {
        val style = CurvedTextStyle(fontWeight = FontWeight.ExtraBold)
        val otherStyle = CurvedTextStyle(fontWeight = FontWeight.Light)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontWeight, otherStyle.fontWeight)
    }

    @Test
    fun `merge with other's font family is unspecified should use this' font family`() {
        val style = CurvedTextStyle(fontFamily = FontFamily.SansSerif)

        val newStyle = style.merge(CurvedTextStyle(fontFamily = null))

        assertEquals(newStyle.fontFamily, style.fontFamily)
    }

    @Test
    fun `merge with other's font family is set should use other's font family`() {
        val style = CurvedTextStyle(fontFamily = FontFamily.Serif)
        val otherStyle = CurvedTextStyle(fontFamily = FontFamily.SansSerif)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontFamily, otherStyle.fontFamily)
    }

    @Test
    fun `merge with other's font style is unspecified should use this' font style`() {
        val style = CurvedTextStyle(fontStyle = FontStyle.Italic)

        val newStyle = style.merge(CurvedTextStyle(fontStyle = null))

        assertEquals(newStyle.fontStyle, style.fontStyle)
    }

    @Test
    fun `merge with other's font style is set should use other's font style`() {
        val style = CurvedTextStyle(fontStyle = FontStyle.Normal)
        val otherStyle = CurvedTextStyle(fontStyle = FontStyle.Italic)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontStyle, otherStyle.fontStyle)
    }

    @Test
    fun `merge with other's font synthesis is unspecified should use this' font synthesis`() {
        val style = CurvedTextStyle(fontSynthesis = FontSynthesis.Weight)

        val newStyle = style.merge(CurvedTextStyle(fontSynthesis = null))

        assertEquals(newStyle.fontSynthesis, style.fontSynthesis)
    }

    @Test
    fun `merge with other's font synthesis is set should use other's font synthesis`() {
        val style = CurvedTextStyle(fontSynthesis = FontSynthesis.Style)
        val otherStyle = CurvedTextStyle(fontSynthesis = FontSynthesis.Weight)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.fontSynthesis, otherStyle.fontSynthesis)
    }

    @Test
    fun `merge with other's letter spacing is unspecified should use this' letter spacing`() {
        val style = CurvedTextStyle(letterSpacing = 0.123f.em)

        val newStyle = style.merge(CurvedTextStyle(letterSpacing = TextUnit.Unspecified))

        assertEquals(newStyle.letterSpacing, style.letterSpacing)
    }

    @Test
    fun `merge with other's letter spacing is set should use other's letter spacing`() {
        val style = CurvedTextStyle(letterSpacing = 0.123f.em)
        val otherStyle = CurvedTextStyle(letterSpacing = 0.345f.em)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.letterSpacing, otherStyle.letterSpacing)
    }

    @Test
    fun `merge with other's letter spacing counterclockwise is unspecified should use this' letter spacing counterclockwise`() {
        val style = CurvedTextStyle(letterSpacingCounterClockwise = 0.123f.em)

        val newStyle =
            style.merge(CurvedTextStyle(letterSpacingCounterClockwise = TextUnit.Unspecified))

        assertEquals(newStyle.letterSpacingCounterClockwise, style.letterSpacingCounterClockwise)
    }

    @Test
    fun `merge with other's letter spacing counterclockwise is set should use other's letter spacing counterclockwise`() {
        val style = CurvedTextStyle(letterSpacingCounterClockwise = 0.123f.em)
        val otherStyle = CurvedTextStyle(letterSpacingCounterClockwise = 0.345f.em)

        val newStyle = style.merge(otherStyle)

        assertEquals(
            newStyle.letterSpacingCounterClockwise,
            otherStyle.letterSpacingCounterClockwise,
        )
    }

    @Test
    fun `merge with other's line height is unspecified should use this' line height `() {
        val style = CurvedTextStyle(lineHeight = 10.sp)

        val newStyle = style.merge(CurvedTextStyle(letterSpacing = TextUnit.Unspecified))

        assertEquals(newStyle.lineHeight, style.lineHeight)
    }

    @Test
    fun `merge with other's line height is set should use other's line height`() {
        val style = CurvedTextStyle(lineHeight = 12.sp)
        val otherStyle = CurvedTextStyle(lineHeight = 23.sp)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.lineHeight, otherStyle.lineHeight)
    }

    @Test
    fun `merge with other's warp offset is unspecified should use this' warp offset`() {
        val style = CurvedTextStyle(warpOffset = CurvedTextStyle.WarpOffset.Descent)

        val newStyle = style.merge(CurvedTextStyle(letterSpacing = TextUnit.Unspecified))

        assertEquals(newStyle.warpOffset, style.warpOffset)
    }

    @Test
    fun `merge with other's warp offset is set should use other's warp offset`() {
        val style = CurvedTextStyle(warpOffset = CurvedTextStyle.WarpOffset.Descent)
        val otherStyle = CurvedTextStyle(warpOffset = CurvedTextStyle.WarpOffset.Ascent)

        val newStyle = style.merge(otherStyle)

        assertEquals(newStyle.warpOffset, otherStyle.warpOffset)
    }
}
