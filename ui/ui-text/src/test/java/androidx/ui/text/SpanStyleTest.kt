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

package androidx.ui.text

import androidx.ui.core.em
import androidx.ui.core.sp
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextGeometricTransform
import androidx.ui.text.font.FontFamily
import androidx.ui.text.style.lerp
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.text.font.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpanStyleTest {
    @Test
    fun `constructor with default values`() {
        val spanStyle = SpanStyle()

        assertThat(spanStyle.color).isNull()
        assertThat(spanStyle.fontSize.isInherit).isTrue()
        assertThat(spanStyle.fontWeight).isNull()
        assertThat(spanStyle.fontStyle).isNull()
        assertThat(spanStyle.letterSpacing.isInherit).isTrue()
        assertThat(spanStyle.localeList).isNull()
        assertThat(spanStyle.background).isNull()
        assertThat(spanStyle.decoration).isNull()
        assertThat(spanStyle.fontFamily).isNull()
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color(0xFF00FF00)

        val spanStyle = SpanStyle(color = color)

        assertThat(spanStyle.color).isEqualTo(color)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val spanStyle = SpanStyle(fontSize = fontSize)

        assertThat(spanStyle.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `constructor with customized fontWeight`() {
        val fontWeight = FontWeight.W500

        val spanStyle = SpanStyle(fontWeight = fontWeight)

        assertThat(spanStyle.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `constructor with customized fontStyle`() {
        val fontStyle = FontStyle.Italic

        val spanStyle = SpanStyle(fontStyle = fontStyle)

        assertThat(spanStyle.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `constructor with customized letterSpacing`() {
        val letterSpacing = 1.em

        val spanStyle = SpanStyle(letterSpacing = letterSpacing)

        assertThat(spanStyle.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `constructor with customized baselineShift`() {
        val baselineShift = BaselineShift.Superscript

        val spanStyle = SpanStyle(baselineShift = baselineShift)

        assertThat(spanStyle.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `constructor with customized locale`() {
        val localeList = LocaleList("en-US")

        val spanStyle = SpanStyle(localeList = localeList)

        assertThat(spanStyle.localeList).isEqualTo(localeList)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color(0xFF00FF00)

        val spanStyle = SpanStyle(background = color)

        assertThat(spanStyle.background).isEqualTo(color)
    }

    @Test
    fun `constructor with customized decoration`() {
        val decoration = TextDecoration.Underline

        val spanStyle = SpanStyle(decoration = decoration)

        assertThat(spanStyle.decoration).isEqualTo(decoration)
    }

    @Test
    fun `constructor with customized fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")

        val spanStyle = SpanStyle(fontFamily = fontFamily)

        assertThat(spanStyle.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with empty other should return this`() {
        val spanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge()

        assertThat(newSpanStyle).isEqualTo(spanStyle)
    }

    @Test
    fun `merge with other's color is null should use this' color`() {
        val color = Color(0xFF00FF00)
        val spanStyle = SpanStyle(color = color)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.color).isEqualTo(color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val color = Color(0xFF00FF00)
        val otherColor = Color(0x00FFFF00)
        val spanStyle = SpanStyle(color = color)
        val otherSpanStyle = SpanStyle(color = otherColor)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.color).isEqualTo(otherColor)
    }

    @Test
    fun `merge with other's fontFamily is null should use this' fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")
        val spanStyle = SpanStyle(fontFamily = fontFamily)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with other's fontFamily is set should use other's fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")
        val otherFontFamily = FontFamily(genericFamily = "serif")
        val spanStyle = SpanStyle(fontFamily = fontFamily)
        val otherSpanStyle = SpanStyle(fontFamily = otherFontFamily)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontFamily).isEqualTo(otherFontFamily)
    }

    @Test
    fun `merge with other's fontSize is null should use this' fontSize`() {
        val fontSize = 3.5.sp
        val spanStyle = SpanStyle(fontSize = fontSize)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val fontSize = 3.5.sp
        val otherFontSize = 8.7.sp
        val spanStyle = SpanStyle(fontSize = fontSize)
        val otherSpanStyle = SpanStyle(fontSize = otherFontSize)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontSize).isEqualTo(otherFontSize)
    }

    @Test
    fun `merge with other's fontWeight is null should use this' fontWeight`() {
        val fontWeight = FontWeight.W300
        val spanStyle = SpanStyle(fontWeight = fontWeight)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `merge with other's fontWeight is set should use other's fontWeight`() {
        val fontWeight = FontWeight.W300
        val otherFontWeight = FontWeight.W500
        val spanStyle = SpanStyle(fontWeight = fontWeight)
        val otherSpanStyle = SpanStyle(fontWeight = otherFontWeight)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontWeight).isEqualTo(otherFontWeight)
    }

    @Test
    fun `merge with other's fontStyle is null should use this' fontStyle`() {
        val fontStyle = FontStyle.Italic
        val spanStyle = SpanStyle(fontStyle = fontStyle)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `merge with other's fontStyle is set should use other's fontStyle`() {
        val fontStyle = FontStyle.Italic
        val otherFontStyle = FontStyle.Normal
        val spanStyle = SpanStyle(fontStyle = fontStyle)
        val otherSpanStyle = SpanStyle(fontStyle = otherFontStyle)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontStyle).isEqualTo(otherFontStyle)
    }

    @Test
    fun `merge with other's fontSynthesis is null should use this' fontSynthesis`() {
        val fontSynthesis = FontSynthesis.Style
        val spanStyle = SpanStyle(fontSynthesis = fontSynthesis)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis)
    }

    @Test
    fun `merge with other's fontSynthesis is set should use other's fontSynthesis`() {
        val fontSynthesis = FontSynthesis.Style
        val otherFontSynthesis = FontSynthesis.Weight

        val spanStyle = SpanStyle(fontSynthesis = fontSynthesis)
        val otherSpanStyle = SpanStyle(fontSynthesis = otherFontSynthesis)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(otherFontSynthesis)
    }

    @Test
    fun `merge with other's fontFeature is null should use this' fontSynthesis`() {
        val fontFeatureSettings = "\"kern\" 0"
        val spanStyle = SpanStyle(fontFeatureSettings = fontFeatureSettings)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }

    @Test
    fun `merge with other's fontFeature is set should use other's fontSynthesis`() {
        val fontFeatureSettings = "\"kern\" 0"
        val otherFontFeatureSettings = "\"kern\" 1"

        val spanStyle = SpanStyle(fontFeatureSettings = fontFeatureSettings)
        val otherSpanStyle =
            SpanStyle(fontFeatureSettings = otherFontFeatureSettings)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(otherFontFeatureSettings)
    }

    @Test
    fun `merge with other's letterSpacing is null should use this' letterSpacing`() {
        val letterSpacing = 1.2.em
        val spanStyle = SpanStyle(letterSpacing = letterSpacing)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `merge with other's letterSpacing is set should use other's letterSpacing`() {
        val letterSpacing = 1.2.em
        val otherLetterSpacing = 1.5.em
        val spanStyle = SpanStyle(letterSpacing = letterSpacing)
        val otherSpanStyle = SpanStyle(letterSpacing = otherLetterSpacing)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.letterSpacing).isEqualTo(otherLetterSpacing)
    }

    @Test
    fun `merge with other's baselineShift is null should use this' baselineShift`() {
        val baselineShift = BaselineShift.Superscript
        val spanStyle = SpanStyle(baselineShift = baselineShift)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `merge with other's baselineShift is set should use other's baselineShift`() {
        val baselineShift = BaselineShift.Superscript
        val otherBaselineShift = BaselineShift.Subscript
        val spanStyle = SpanStyle(baselineShift = baselineShift)
        val otherSpanStyle = SpanStyle(baselineShift = otherBaselineShift)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.baselineShift).isEqualTo(otherBaselineShift)
    }

    @Test
    fun `merge with other's background is null should use this' background`() {
        val color = Color(0xFF00FF00)
        val spanStyle = SpanStyle(background = color)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.background).isEqualTo(color)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val color = Color(0xFF00FF00)
        val otherColor = Color(0xFF0000FF)
        val spanStyle = SpanStyle(background = color)
        val otherSpanStyle = SpanStyle(background = otherColor)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.background).isEqualTo(otherColor)
    }

    @Test
    fun `merge with other's decoration is null should use this' decoration`() {
        val decoration = TextDecoration.LineThrough
        val spanStyle = SpanStyle(decoration = decoration)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.decoration).isEqualTo(decoration)
    }

    @Test
    fun `merge with other's decoration is set should use other's decoration`() {
        val decoration = TextDecoration.LineThrough
        val otherDecoration = TextDecoration.Underline
        val spanStyle = SpanStyle(decoration = decoration)
        val otherSpanStyle = SpanStyle(decoration = otherDecoration)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.decoration).isEqualTo(otherDecoration)
    }

    @Test
    fun `merge with other's locale is null should use this' locale`() {
        val localeList = LocaleList("en-US")
        val spanStyle = SpanStyle(localeList = localeList)
        val otherSpanStyle = SpanStyle()

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList)
    }

    @Test
    fun `merge with other's locale is set should use other's locale`() {
        val localeList = LocaleList("en-US")
        val otherlocaleList = LocaleList("ja-JP")
        val spanStyle = SpanStyle(localeList = localeList)
        val otherSpanStyle = SpanStyle(localeList = otherlocaleList)

        val newSpanStyle = spanStyle.merge(otherSpanStyle)

        assertThat(newSpanStyle.localeList).isEqualTo(otherlocaleList)
    }

    @Test
    fun `lerp color with a and b are not Null`() {
        val color1 = Color(0xFF00FF00)
        val color2 = Color(0x00FFFF00)
        val t = 0.3f
        val spanStyle1 = SpanStyle(color = color1)
        val spanStyle2 = SpanStyle(color = color2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is smaller than half`() {
        val fontFamily1 = FontFamily(genericFamily = "sans-serif")
        val fontFamily2 = FontFamily(genericFamily = "serif")
        val t = 0.3f
        val spanStyle1 = SpanStyle(fontFamily = fontFamily1)
        val spanStyle2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily1)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is larger than half`() {
        val fontFamily1 = FontFamily(genericFamily = "sans-serif")
        val fontFamily2 = FontFamily(genericFamily = "serif")
        val t = 0.8f
        val spanStyle1 = SpanStyle(fontFamily = fontFamily1)
        val spanStyle2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily2)
    }

    @Test
    fun `lerp fontSize with a and b are not Null`() {
        val fontSize1 = 8.sp
        val fontSize2 = 16.sp
        val t = 0.8f
        val spanStyle1 = SpanStyle(fontSize = fontSize1)
        val spanStyle2 = SpanStyle(fontSize = fontSize2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        // a + (b - a) * t = 8.0f + (16.0f  - 8.0f) * 0.8f = 14.4f
        assertThat(newSpanStyle.fontSize).isEqualTo(14.4.sp)
    }

    @Test
    fun `lerp fontWeight with a and b are not Null`() {
        val fontWeight1 = FontWeight.W200
        val fontWeight2 = FontWeight.W500
        val t = 0.8f
        val spanStyle1 = SpanStyle(fontWeight = fontWeight1)
        val spanStyle2 = SpanStyle(fontWeight = fontWeight2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontWeight).isEqualTo(lerp(fontWeight1, fontWeight2, t))
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is smaller than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.3f
        val spanStyle1 = SpanStyle(fontStyle = fontStyle1)
        val spanStyle2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle1)
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is larger than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.8f
        val spanStyle1 = SpanStyle(fontStyle = fontStyle1)
        val spanStyle2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle2)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is smaller than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val spanStyle1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val spanStyle2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis1)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is larger than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val spanStyle1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val spanStyle2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis2)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is smaller than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val spanStyle1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val spanStyle2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings1)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is larger than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val spanStyle1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val spanStyle2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings2)
    }

    @Test
    fun `lerp baselineShift with a and b are not Null`() {
        val baselineShift1 = BaselineShift(1.0f)
        val baselineShift2 = BaselineShift(2.0f)
        val t = 0.3f
        val spanStyle1 = SpanStyle(baselineShift = baselineShift1)
        val spanStyle2 = SpanStyle(baselineShift = baselineShift2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.baselineShift)
            .isEqualTo(lerp(baselineShift1, baselineShift2, t))
    }

    @Test
    fun `lerp textGeometricTransform with a and b are not Null`() {
        val textTransform1 = TextGeometricTransform(scaleX = 1.5f, skewX = 0.1f)
        val textTransform2 = TextGeometricTransform(scaleX = 1.0f, skewX = 0.3f)
        val t = 0.3f
        val spanStyle1 = SpanStyle(textGeometricTransform = textTransform1)
        val spanStyle2 = SpanStyle(textGeometricTransform = textTransform2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.textGeometricTransform)
            .isEqualTo(lerp(textTransform1, textTransform2, t))
    }

    @Test
    fun `lerp locale with a and b are not Null and t is smaller than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.3f
        val spanStyle1 = SpanStyle(localeList = localeList1)
        val spanStyle2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList1)
    }

    @Test
    fun `lerp locale with a and b are not Null and t is larger than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.8f
        val spanStyle1 = SpanStyle(localeList = localeList1)
        val spanStyle2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList2)
    }

    @Test
    fun `lerp background with a and b are Null and t is smaller than half`() {
        val spanStyle1 = SpanStyle(background = null)
        val spanStyle2 = SpanStyle(background = null)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = 0.1f)

        assertThat(newSpanStyle.background).isEqualTo(Color.Transparent)
    }

    @Test
    fun `lerp background with a is Null and b is not Null`() {
        val t = 0.1f
        val spanStyle1 = SpanStyle(background = null)
        val color2 = Color(0xf)
        val spanStyle2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(Color.Transparent, color2, t))
    }

    @Test
    fun `lerp background with a is Not Null and b is Null`() {
        val t = 0.1f
        val color1 = Color(0xf)
        val spanStyle1 = SpanStyle(background = color1)
        val spanStyle2 = SpanStyle(background = null)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, Color.Transparent, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is smaller than half`() {
        val color1 = Color(0x0)
        val color2 = Color(0xf)
        val t = 0.2f
        val spanStyle1 = SpanStyle(background = color1)
        val spanStyle2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is larger than half`() {
        val color1 = Color(0x0)
        val color2 = Color(0xf)
        val t = 0.8f
        val spanStyle1 = SpanStyle(background = color1)
        val spanStyle2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp decoration with a and b are not Null and t is smaller than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.2f
        val spanStyle1 = SpanStyle(decoration = decoration1)
        val spanStyle2 = SpanStyle(decoration = decoration2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.decoration).isEqualTo(decoration1)
    }

    @Test
    fun `lerp decoration with a and b are not Null and t is larger than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.8f
        val spanStyle1 = SpanStyle(decoration = decoration1)
        val spanStyle2 = SpanStyle(decoration = decoration2)

        val newSpanStyle = lerp(start = spanStyle1, stop = spanStyle2, fraction = t)

        assertThat(newSpanStyle.decoration).isEqualTo(decoration2)
    }
}
