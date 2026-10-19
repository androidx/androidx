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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun `merge with other's line height is unspecified should use this' line height`() {
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

    @Test
    fun `constructor from TextStyle maps matching properties`() {
        val background = Color.Green
        val color = Color.Blue
        val fontSize = 18.sp
        val fontFamily = FontFamily.Monospace
        val fontWeight = FontWeight.Medium
        val fontStyle = FontStyle.Italic
        val fontSynthesis = FontSynthesis.Style
        val letterSpacing = 0.5.em
        val lineHeight = 24.sp
        val textStyle =
            TextStyle(
                background = background,
                color = color,
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
            )

        val curvedTextStyle = CurvedTextStyle(textStyle)

        assertEquals(background, curvedTextStyle.background)
        assertEquals(color, curvedTextStyle.color)
        assertEquals(fontSize, curvedTextStyle.fontSize)
        assertEquals(fontFamily, curvedTextStyle.fontFamily)
        assertEquals(fontWeight, curvedTextStyle.fontWeight)
        assertEquals(fontStyle, curvedTextStyle.fontStyle)
        assertEquals(fontSynthesis, curvedTextStyle.fontSynthesis)
        assertEquals(letterSpacing, curvedTextStyle.letterSpacing)
        assertEquals(letterSpacing, curvedTextStyle.letterSpacingCounterClockwise)
        assertEquals(lineHeight, curvedTextStyle.lineHeight)
        assertEquals(CurvedTextStyle.WarpOffset.Unspecified, curvedTextStyle.warpOffset)
    }

    @Test
    fun `plus operator combining styles produces same result as merge`() {
        val style1 =
            CurvedTextStyle(
                color = Color.Red,
                fontSize = 14.sp,
                warpOffset = CurvedTextStyle.WarpOffset.None,
            )
        val style2 =
            CurvedTextStyle(
                background = Color.Yellow,
                fontSize = 18.sp,
                warpOffset = CurvedTextStyle.WarpOffset.Baseline,
            )

        val plusResult = style1 + style2
        val mergeResult = style1.merge(style2)

        assertEquals(mergeResult, plusResult)
    }

    @Test
    fun `copy with default arguments preserves all properties`() {
        val original =
            CurvedTextStyle(
                background = Color.DarkGray,
                color = Color.LightGray,
                fontSize = 16.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                letterSpacing = 2.sp,
                letterSpacingCounterClockwise = 3.sp,
                lineHeight = 22.sp,
                warpOffset = CurvedTextStyle.WarpOffset.HalfAscent,
            )

        val copied = original.copy()

        assertEquals(original, copied)
        assertEquals(original.background, copied.background)
        assertEquals(original.color, copied.color)
        assertEquals(original.fontSize, copied.fontSize)
        assertEquals(original.fontFamily, copied.fontFamily)
        assertEquals(original.fontWeight, copied.fontWeight)
        assertEquals(original.fontStyle, copied.fontStyle)
        assertEquals(original.fontSynthesis, copied.fontSynthesis)
        assertEquals(original.letterSpacing, copied.letterSpacing)
        assertEquals(
            original.letterSpacingCounterClockwise,
            copied.letterSpacingCounterClockwise,
        )
        assertEquals(original.lineHeight, copied.lineHeight)
        assertEquals(original.warpOffset, copied.warpOffset)
    }

    @Test
    fun `copy with background specified updates background only`() {
        val original =
            CurvedTextStyle(
                background = Color.Red,
                color = Color.Green,
                fontSize = 14.sp,
            )
        val newBackground = Color.Blue

        val copied = original.copy(background = newBackground)

        assertEquals(newBackground, copied.background)
        assertEquals(original.color, copied.color)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with color specified updates color only`() {
        val original =
            CurvedTextStyle(
                background = Color.Red,
                color = Color.Green,
                fontSize = 14.sp,
            )
        val newColor = Color.Magenta

        val copied = original.copy(color = newColor)

        assertEquals(newColor, copied.color)
        assertEquals(original.background, copied.background)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with fontSize specified updates fontSize only`() {
        val original =
            CurvedTextStyle(
                color = Color.Green,
                fontSize = 14.sp,
            )
        val newFontSize = 22.sp

        val copied = original.copy(fontSize = newFontSize)

        assertEquals(newFontSize, copied.fontSize)
        assertEquals(original.color, copied.color)
    }

    @Test
    fun `copy with fontFamily specified updates fontFamily only`() {
        val original =
            CurvedTextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
            )
        val newFontFamily = FontFamily.Monospace

        val copied = original.copy(fontFamily = newFontFamily)

        assertEquals(newFontFamily, copied.fontFamily)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with fontWeight specified updates fontWeight only`() {
        val original =
            CurvedTextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
            )
        val newFontWeight = FontWeight.Bold

        val copied = original.copy(fontWeight = newFontWeight)

        assertEquals(newFontWeight, copied.fontWeight)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with fontStyle specified updates fontStyle only`() {
        val original =
            CurvedTextStyle(
                fontStyle = FontStyle.Normal,
                fontSize = 14.sp,
            )
        val newFontStyle = FontStyle.Italic

        val copied = original.copy(fontStyle = newFontStyle)

        assertEquals(newFontStyle, copied.fontStyle)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with fontSynthesis specified updates fontSynthesis only`() {
        val original =
            CurvedTextStyle(
                fontSynthesis = FontSynthesis.None,
                fontSize = 14.sp,
            )
        val newFontSynthesis = FontSynthesis.All

        val copied = original.copy(fontSynthesis = newFontSynthesis)

        assertEquals(newFontSynthesis, copied.fontSynthesis)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with letterSpacing specified updates letterSpacing only`() {
        val original =
            CurvedTextStyle(
                letterSpacing = 1.sp,
                letterSpacingCounterClockwise = 2.sp,
            )
        val newLetterSpacing = 5.sp

        val copied = original.copy(letterSpacing = newLetterSpacing)

        assertEquals(newLetterSpacing, copied.letterSpacing)
        assertEquals(
            original.letterSpacingCounterClockwise,
            copied.letterSpacingCounterClockwise,
        )
    }

    @Test
    fun `copy with letterSpacingCounterClockwise specified updates letterSpacingCounterClockwise only`() {
        val original =
            CurvedTextStyle(
                letterSpacing = 1.sp,
                letterSpacingCounterClockwise = 2.sp,
            )
        val newLetterSpacingCounterClockwise = 6.sp

        val copied = original.copy(letterSpacingCounterClockwise = newLetterSpacingCounterClockwise)

        assertEquals(
            newLetterSpacingCounterClockwise,
            copied.letterSpacingCounterClockwise,
        )
        assertEquals(original.letterSpacing, copied.letterSpacing)
    }

    @Test
    fun `copy with lineHeight specified updates lineHeight only`() {
        val original =
            CurvedTextStyle(
                lineHeight = 16.sp,
                fontSize = 14.sp,
            )
        val newLineHeight = 28.sp

        val copied = original.copy(lineHeight = newLineHeight)

        assertEquals(newLineHeight, copied.lineHeight)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `copy with warpOffset specified updates warpOffset only`() {
        val original =
            CurvedTextStyle(
                warpOffset = CurvedTextStyle.WarpOffset.Baseline,
                fontSize = 14.sp,
            )
        val newWarpOffset = CurvedTextStyle.WarpOffset.Ascent

        val copied = original.copy(warpOffset = newWarpOffset)

        assertEquals(newWarpOffset, copied.warpOffset)
        assertEquals(original.fontSize, copied.fontSize)
    }

    @Test
    fun `warpOffset isSpecified returns true for specified and false for unspecified`() {
        val unspecified = CurvedTextStyle.WarpOffset.Unspecified
        val specified = CurvedTextStyle.WarpOffset.Baseline

        assertFalse(unspecified.isSpecified)
        assertTrue(specified.isSpecified)
    }

    @Test
    fun `warpOffset isUnspecified returns true for unspecified and false for specified`() {
        val unspecified = CurvedTextStyle.WarpOffset.Unspecified
        val specified = CurvedTextStyle.WarpOffset.HalfOpticalHeight

        assertTrue(unspecified.isUnspecified)
        assertFalse(specified.isUnspecified)
    }

    @Test
    fun `warpOffset takeOrElse specified offset returns this without evaluating fallback`() {
        val offset = CurvedTextStyle.WarpOffset.Ascent
        var fallbackEvaluated = false

        val result = offset.takeOrElse {
            fallbackEvaluated = true
            CurvedTextStyle.WarpOffset.Descent
        }

        assertEquals(CurvedTextStyle.WarpOffset.Ascent, result)
        assertFalse(fallbackEvaluated)
    }

    @Test
    fun `warpOffset takeOrElse unspecified offset evaluates and returns fallback`() {
        val offset = CurvedTextStyle.WarpOffset.Unspecified
        var fallbackEvaluated = false

        val result = offset.takeOrElse {
            fallbackEvaluated = true
            CurvedTextStyle.WarpOffset.Descent
        }

        assertEquals(CurvedTextStyle.WarpOffset.Descent, result)
        assertTrue(fallbackEvaluated)
    }

    @Test
    fun `determineWarpRadiusOffset for baseline returns zero`() {
        val offset = CurvedTextStyle.WarpOffset.Baseline
        val ascent = -24f
        val descent = 6f

        val radiusOffset = offset.determineWarpRadiusOffset(ascent, descent)

        assertEquals(0f, radiusOffset, 0.0001f)
    }

    @Test
    fun `determineWarpRadiusOffset for halfAscent returns negative halfAscent`() {
        val offset = CurvedTextStyle.WarpOffset.HalfAscent
        val ascent = -24f
        val descent = 6f

        val radiusOffset = offset.determineWarpRadiusOffset(ascent, descent)

        assertEquals(12f, radiusOffset, 0.0001f)
    }

    @Test
    fun `determineWarpRadiusOffset for halfOpticalHeight returns negative halfOpticalHeight`() {
        val offset = CurvedTextStyle.WarpOffset.HalfOpticalHeight
        val ascent = -24f
        val descent = 6f

        val radiusOffset = offset.determineWarpRadiusOffset(ascent, descent)

        assertEquals(9f, radiusOffset, 0.0001f)
    }

    @Test
    fun `determineWarpRadiusOffset for ascent returns negative ascent`() {
        val offset = CurvedTextStyle.WarpOffset.Ascent
        val ascent = -24f
        val descent = 6f

        val radiusOffset = offset.determineWarpRadiusOffset(ascent, descent)

        assertEquals(24f, radiusOffset, 0.0001f)
    }

    @Test
    fun `determineWarpRadiusOffset for descent returns negative descent`() {
        val offset = CurvedTextStyle.WarpOffset.Descent
        val ascent = -24f
        val descent = 6f

        val radiusOffset = offset.determineWarpRadiusOffset(ascent, descent)

        assertEquals(-6f, radiusOffset, 0.0001f)
    }

    @Test
    fun `determineWarpRadiusOffset for none throws IllegalArgumentException`() {
        val offset = CurvedTextStyle.WarpOffset.None

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                offset.determineWarpRadiusOffset(-20f, 5f)
            }

        assertTrue(exception.message!!.contains("Illegal warp offset: None"))
    }

    @Test
    fun `determineWarpRadiusOffset for unspecified throws IllegalArgumentException`() {
        val offset = CurvedTextStyle.WarpOffset.Unspecified

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                offset.determineWarpRadiusOffset(-20f, 5f)
            }

        assertTrue(exception.message!!.contains("Illegal warp offset: Unspecified"))
    }

    @Test
    fun `determineWarpRadiusOffset for unknown offset throws IllegalArgumentException`() {
        val offset = CurvedTextStyle.WarpOffset(120.toByte())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                offset.determineWarpRadiusOffset(-20f, 5f)
            }

        assertTrue(exception.message!!.contains("Illegal warp offset: Unknown: 120"))
    }

    @Test
    fun `equals with same instance evaluates to true`() {
        val style =
            CurvedTextStyle(
                background = Color.Yellow,
                color = Color.Blue,
                fontSize = 16.sp,
                warpOffset = CurvedTextStyle.WarpOffset.Baseline,
            )

        val areEqual = style == style

        assertTrue(areEqual)
    }

    @Test
    fun `equals and hashCode with default properties evaluates to true and matches hashCode`() {
        val style1 = CurvedTextStyle()
        val style2 = CurvedTextStyle()

        val hashCode1 = style1.hashCode()
        val hashCode2 = style2.hashCode()

        assertEquals(style1, style2)
        assertEquals(style2, style1)
        assertEquals(hashCode1, hashCode2)
    }

    @Test
    fun `equals and hashCode with identical properties evaluates to true and matches hashCode`() {
        val style1 =
            CurvedTextStyle(
                background = Color.Yellow,
                color = Color.Blue,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                letterSpacing = 1.sp,
                letterSpacingCounterClockwise = 2.sp,
                lineHeight = 20.sp,
                warpOffset = CurvedTextStyle.WarpOffset.Ascent,
            )
        val style2 =
            CurvedTextStyle(
                background = Color.Yellow,
                color = Color.Blue,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                letterSpacing = 1.sp,
                letterSpacingCounterClockwise = 2.sp,
                lineHeight = 20.sp,
                warpOffset = CurvedTextStyle.WarpOffset.Ascent,
            )

        val hashCode1 = style1.hashCode()
        val hashCode2 = style2.hashCode()

        assertEquals(style1, style2)
        assertEquals(style2, style1)
        assertEquals(hashCode1, hashCode2)
    }

    @Test
    fun `equals with different background returns false`() {
        val style1 = CurvedTextStyle(background = Color.Red)
        val style2 = CurvedTextStyle(background = Color.Blue)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different color returns false`() {
        val style1 = CurvedTextStyle(color = Color.Red)
        val style2 = CurvedTextStyle(color = Color.Blue)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different fontSize returns false`() {
        val style1 = CurvedTextStyle(fontSize = 12.sp)
        val style2 = CurvedTextStyle(fontSize = 14.sp)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different fontFamily returns false`() {
        val style1 = CurvedTextStyle(fontFamily = FontFamily.Serif)
        val style2 = CurvedTextStyle(fontFamily = FontFamily.Monospace)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different fontWeight returns false`() {
        val style1 = CurvedTextStyle(fontWeight = FontWeight.Normal)
        val style2 = CurvedTextStyle(fontWeight = FontWeight.Bold)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different fontStyle returns false`() {
        val style1 = CurvedTextStyle(fontStyle = FontStyle.Normal)
        val style2 = CurvedTextStyle(fontStyle = FontStyle.Italic)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different fontSynthesis returns false`() {
        val style1 = CurvedTextStyle(fontSynthesis = FontSynthesis.None)
        val style2 = CurvedTextStyle(fontSynthesis = FontSynthesis.All)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different letterSpacing returns false`() {
        val style1 = CurvedTextStyle(letterSpacing = 1.sp)
        val style2 = CurvedTextStyle(letterSpacing = 2.sp)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different letterSpacingCounterClockwise returns false`() {
        val style1 = CurvedTextStyle(letterSpacingCounterClockwise = 1.sp)
        val style2 = CurvedTextStyle(letterSpacingCounterClockwise = 2.sp)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different lineHeight returns false`() {
        val style1 = CurvedTextStyle(lineHeight = 16.sp)
        val style2 = CurvedTextStyle(lineHeight = 20.sp)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with different warpOffset returns false`() {
        val style1 = CurvedTextStyle(warpOffset = CurvedTextStyle.WarpOffset.Baseline)
        val style2 = CurvedTextStyle(warpOffset = CurvedTextStyle.WarpOffset.Ascent)

        assertNotEquals(style1, style2)
    }

    @Test
    fun `equals with null other returns false`() {
        val style = CurvedTextStyle(color = Color.Red)

        assertFalse(style.equals(null))
    }

    @Test
    fun `equals with different type returns false`() {
        val style = CurvedTextStyle(color = Color.Red)
        val other: Any = "NotACurvedTextStyle"

        assertFalse(style.equals(other))
    }
}
