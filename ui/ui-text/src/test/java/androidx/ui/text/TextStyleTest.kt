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
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.graphics.lerp
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.lerp
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextGeometricTransform
import androidx.ui.text.style.lerp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleTest {
    @Test
    fun `constructor with default values`() {
        val textStyle = TextStyle()

        assertThat(textStyle.color).isNull()
        assertThat(textStyle.fontSize.isInherit).isTrue()
        assertThat(textStyle.fontWeight).isNull()
        assertThat(textStyle.fontStyle).isNull()
        assertThat(textStyle.letterSpacing.isInherit).isTrue()
        assertThat(textStyle.localeList).isNull()
        assertThat(textStyle.background).isNull()
        assertThat(textStyle.decoration).isNull()
        assertThat(textStyle.fontFamily).isNull()
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color(0xFF00FF00)

        val textStyle = TextStyle(color = color)

        assertThat(textStyle.color).isEqualTo(color)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val textStyle = TextStyle(fontSize = fontSize)

        assertThat(textStyle.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `constructor with customized fontWeight`() {
        val fontWeight = FontWeight.W500

        val textStyle = TextStyle(fontWeight = fontWeight)

        assertThat(textStyle.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `constructor with customized fontStyle`() {
        val fontStyle = FontStyle.Italic

        val textStyle = TextStyle(fontStyle = fontStyle)

        assertThat(textStyle.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `constructor with customized letterSpacing`() {
        val letterSpacing = 1.em

        val textStyle = TextStyle(letterSpacing = letterSpacing)

        assertThat(textStyle.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `constructor with customized baselineShift`() {
        val baselineShift = BaselineShift.Superscript

        val textStyle = TextStyle(baselineShift = baselineShift)

        assertThat(textStyle.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `constructor with customized locale`() {
        val localeList = LocaleList("en-US")

        val textStyle = TextStyle(localeList = localeList)

        assertThat(textStyle.localeList).isEqualTo(localeList)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color(0xFF00FF00)

        val textStyle = TextStyle(background = color)

        assertThat(textStyle.background).isEqualTo(color)
    }

    @Test
    fun `constructor with customized decoration`() {
        val decoration = TextDecoration.Underline

        val textStyle = TextStyle(decoration = decoration)

        assertThat(textStyle.decoration).isEqualTo(decoration)
    }

    @Test
    fun `constructor with customized fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")

        val textStyle = TextStyle(fontFamily = fontFamily)

        assertThat(textStyle.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with empty other should return this`() {
        val textStyle = TextStyle()

        val newTextStyle = textStyle.merge()

        assertThat(newTextStyle).isEqualTo(textStyle)
    }

    @Test
    fun `merge with other's color is null should use this' color`() {
        val color = Color(0xFF00FF00)
        val textStyle = TextStyle(color = color)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.color).isEqualTo(color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val color = Color(0xFF00FF00)
        val otherColor = Color(0x00FFFF00)
        val textStyle = TextStyle(color = color)
        val otherTextStyle = TextStyle(color = otherColor)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.color).isEqualTo(otherColor)
    }

    @Test
    fun `merge with other's fontFamily is null should use this' fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")
        val textStyle = TextStyle(fontFamily = fontFamily)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with other's fontFamily is set should use other's fontFamily`() {
        val fontFamily = FontFamily(genericFamily = "sans-serif")
        val otherFontFamily = FontFamily(genericFamily = "serif")
        val textStyle = TextStyle(fontFamily = fontFamily)
        val otherTextStyle = TextStyle(fontFamily = otherFontFamily)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontFamily).isEqualTo(otherFontFamily)
    }

    @Test
    fun `merge with other's fontSize is null should use this' fontSize`() {
        val fontSize = 3.5.sp
        val textStyle = TextStyle(fontSize = fontSize)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val fontSize = 3.5.sp
        val otherFontSize = 8.7.sp
        val textStyle = TextStyle(fontSize = fontSize)
        val otherTextStyle = TextStyle(fontSize = otherFontSize)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontSize).isEqualTo(otherFontSize)
    }

    @Test
    fun `merge with other's fontWeight is null should use this' fontWeight`() {
        val fontWeight = FontWeight.W300
        val textStyle = TextStyle(fontWeight = fontWeight)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `merge with other's fontWeight is set should use other's fontWeight`() {
        val fontWeight = FontWeight.W300
        val otherFontWeight = FontWeight.W500
        val textStyle = TextStyle(fontWeight = fontWeight)
        val otherTextStyle = TextStyle(fontWeight = otherFontWeight)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontWeight).isEqualTo(otherFontWeight)
    }

    @Test
    fun `merge with other's fontStyle is null should use this' fontStyle`() {
        val fontStyle = FontStyle.Italic
        val textStyle = TextStyle(fontStyle = fontStyle)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `merge with other's fontStyle is set should use other's fontStyle`() {
        val fontStyle = FontStyle.Italic
        val otherFontStyle = FontStyle.Normal
        val textStyle = TextStyle(fontStyle = fontStyle)
        val otherTextStyle = TextStyle(fontStyle = otherFontStyle)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontStyle).isEqualTo(otherFontStyle)
    }

    @Test
    fun `merge with other's fontSynthesis is null should use this' fontSynthesis`() {
        val fontSynthesis = FontSynthesis.Style
        val textStyle = TextStyle(fontSynthesis = fontSynthesis)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontSynthesis).isEqualTo(fontSynthesis)
    }

    @Test
    fun `merge with other's fontSynthesis is set should use other's fontSynthesis`() {
        val fontSynthesis = FontSynthesis.Style
        val otherFontSynthesis = FontSynthesis.Weight

        val textStyle = TextStyle(fontSynthesis = fontSynthesis)
        val otherTextStyle = TextStyle(fontSynthesis = otherFontSynthesis)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontSynthesis).isEqualTo(otherFontSynthesis)
    }

    @Test
    fun `merge with other's fontFeature is null should use this' fontSynthesis`() {
        val fontFeatureSettings = "\"kern\" 0"
        val textStyle = TextStyle(fontFeatureSettings = fontFeatureSettings)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }

    @Test
    fun `merge with other's fontFeature is set should use other's fontSynthesis`() {
        val fontFeatureSettings = "\"kern\" 0"
        val otherFontFeatureSettings = "\"kern\" 1"

        val textStyle = TextStyle(fontFeatureSettings = fontFeatureSettings)
        val otherTextStyle =
            TextStyle(fontFeatureSettings = otherFontFeatureSettings)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.fontFeatureSettings).isEqualTo(otherFontFeatureSettings)
    }

    @Test
    fun `merge with other's letterSpacing is null should use this' letterSpacing`() {
        val letterSpacing = 1.2.em
        val textStyle = TextStyle(letterSpacing = letterSpacing)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `merge with other's letterSpacing is set should use other's letterSpacing`() {
        val letterSpacing = 1.2.em
        val otherLetterSpacing = 1.5.em
        val textStyle = TextStyle(letterSpacing = letterSpacing)
        val otherTextStyle = TextStyle(letterSpacing = otherLetterSpacing)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.letterSpacing).isEqualTo(otherLetterSpacing)
    }

    @Test
    fun `merge with other's baselineShift is null should use this' baselineShift`() {
        val baselineShift = BaselineShift.Superscript
        val textStyle = TextStyle(baselineShift = baselineShift)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `merge with other's baselineShift is set should use other's baselineShift`() {
        val baselineShift = BaselineShift.Superscript
        val otherBaselineShift = BaselineShift.Subscript
        val textStyle = TextStyle(baselineShift = baselineShift)
        val otherTextStyle = TextStyle(baselineShift = otherBaselineShift)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.baselineShift).isEqualTo(otherBaselineShift)
    }

    @Test
    fun `merge with other's background is null should use this' background`() {
        val color = Color(0xFF00FF00)
        val textStyle = TextStyle(background = color)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.background).isEqualTo(color)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val color = Color(0xFF00FF00)
        val otherColor = Color(0xFF0000FF)
        val textStyle = TextStyle(background = color)
        val otherTextStyle = TextStyle(background = otherColor)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.background).isEqualTo(otherColor)
    }

    @Test
    fun `merge with other's decoration is null should use this' decoration`() {
        val decoration = TextDecoration.LineThrough
        val textStyle = TextStyle(decoration = decoration)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.decoration).isEqualTo(decoration)
    }

    @Test
    fun `merge with other's decoration is set should use other's decoration`() {
        val decoration = TextDecoration.LineThrough
        val otherDecoration = TextDecoration.Underline
        val textStyle = TextStyle(decoration = decoration)
        val otherTextStyle = TextStyle(decoration = otherDecoration)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.decoration).isEqualTo(otherDecoration)
    }

    @Test
    fun `merge with other's locale is null should use this' locale`() {
        val localeList = LocaleList("en-US")
        val textStyle = TextStyle(localeList = localeList)
        val otherTextStyle = TextStyle()

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.localeList).isEqualTo(localeList)
    }

    @Test
    fun `merge with other's locale is set should use other's locale`() {
        val localeList = LocaleList("en-US")
        val otherlocaleList = LocaleList("ja-JP")
        val textStyle = TextStyle(localeList = localeList)
        val otherTextStyle = TextStyle(localeList = otherlocaleList)

        val newTextStyle = textStyle.merge(otherTextStyle)

        assertThat(newTextStyle.localeList).isEqualTo(otherlocaleList)
    }

    @Test
    fun `lerp color with a and b are not Null`() {
        val color1 = Color(0xFF00FF00)
        val color2 = Color(0x00FFFF00)
        val t = 0.3f
        val textStyle1 = TextStyle(color = color1)
        val textStyle2 = TextStyle(color = color2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is smaller than half`() {
        val fontFamily1 = FontFamily(genericFamily = "sans-serif")
        val fontFamily2 = FontFamily(genericFamily = "serif")
        val t = 0.3f
        val textStyle1 = TextStyle(fontFamily = fontFamily1)
        val textStyle2 = TextStyle(fontFamily = fontFamily2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontFamily).isEqualTo(fontFamily1)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is larger than half`() {
        val fontFamily1 = FontFamily(genericFamily = "sans-serif")
        val fontFamily2 = FontFamily(genericFamily = "serif")
        val t = 0.8f
        val textStyle1 = TextStyle(fontFamily = fontFamily1)
        val textStyle2 = TextStyle(fontFamily = fontFamily2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontFamily).isEqualTo(fontFamily2)
    }

    @Test
    fun `lerp fontSize with a and b are not Null`() {
        val fontSize1 = 8.sp
        val fontSize2 = 16.sp
        val t = 0.8f
        val textStyle1 = TextStyle(fontSize = fontSize1)
        val textStyle2 = TextStyle(fontSize = fontSize2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        // a + (b - a) * t = 8.0f + (16.0f  - 8.0f) * 0.8f = 14.4f
        assertThat(newTextStyle.fontSize).isEqualTo(14.4.sp)
    }

    @Test
    fun `lerp fontWeight with a and b are not Null`() {
        val fontWeight1 = FontWeight.W200
        val fontWeight2 = FontWeight.W500
        val t = 0.8f
        val textStyle1 = TextStyle(fontWeight = fontWeight1)
        val textStyle2 = TextStyle(fontWeight = fontWeight2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontWeight).isEqualTo(lerp(fontWeight1, fontWeight2, t))
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is smaller than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.3f
        val textStyle1 = TextStyle(fontStyle = fontStyle1)
        val textStyle2 = TextStyle(fontStyle = fontStyle2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontStyle).isEqualTo(fontStyle1)
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is larger than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.8f
        val textStyle1 = TextStyle(fontStyle = fontStyle1)
        val textStyle2 = TextStyle(fontStyle = fontStyle2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontStyle).isEqualTo(fontStyle2)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is smaller than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val textStyle1 = TextStyle(fontSynthesis = fontSynthesis1)
        val textStyle2 = TextStyle(fontSynthesis = fontSynthesis2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontSynthesis).isEqualTo(fontSynthesis1)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is larger than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val textStyle1 = TextStyle(fontSynthesis = fontSynthesis1)
        val textStyle2 = TextStyle(fontSynthesis = fontSynthesis2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontSynthesis).isEqualTo(fontSynthesis2)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is smaller than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val textStyle1 = TextStyle(fontFeatureSettings = fontFeatureSettings1)
        val textStyle2 = TextStyle(fontFeatureSettings = fontFeatureSettings2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings1)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is larger than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val textStyle1 = TextStyle(fontFeatureSettings = fontFeatureSettings1)
        val textStyle2 = TextStyle(fontFeatureSettings = fontFeatureSettings2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings2)
    }

    @Test
    fun `lerp baselineShift with a and b are not Null`() {
        val baselineShift1 = BaselineShift(1.0f)
        val baselineShift2 = BaselineShift(2.0f)
        val t = 0.3f
        val textStyle1 = TextStyle(baselineShift = baselineShift1)
        val textStyle2 = TextStyle(baselineShift = baselineShift2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.baselineShift)
            .isEqualTo(lerp(baselineShift1, baselineShift2, t))
    }

    @Test
    fun `lerp textGeometricTransform with a and b are not Null`() {
        val textTransform1 =
            TextGeometricTransform(scaleX = 1.5f, skewX = 0.1f)
        val textTransform2 =
            TextGeometricTransform(scaleX = 1.0f, skewX = 0.3f)
        val t = 0.3f
        val textStyle1 = TextStyle(textGeometricTransform = textTransform1)
        val textStyle2 = TextStyle(textGeometricTransform = textTransform2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.textGeometricTransform)
            .isEqualTo(lerp(textTransform1, textTransform2, t))
    }

    @Test
    fun `lerp locale with a and b are not Null and t is smaller than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.3f
        val textStyle1 = TextStyle(localeList = localeList1)
        val textStyle2 = TextStyle(localeList = localeList2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.localeList).isEqualTo(localeList1)
    }

    @Test
    fun `lerp locale with a and b are not Null and t is larger than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.8f
        val textStyle1 = TextStyle(localeList = localeList1)
        val textStyle2 = TextStyle(localeList = localeList2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.localeList).isEqualTo(localeList2)
    }

    @Test
    fun `lerp background with a and b are Null and t is smaller than half`() {
        val textStyle1 = TextStyle(background = null)
        val textStyle2 = TextStyle(background = null)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = 0.1f)

        assertThat(newTextStyle.background).isEqualTo(Color.Transparent)
    }

    @Test
    fun `lerp background with a is Null and b is not Null`() {
        val t = 0.1f
        val textStyle1 = TextStyle(background = null)
        val color2 = Color(0xf)
        val textStyle2 = TextStyle(background = color2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.background).isEqualTo(lerp(Color.Transparent, color2, t))
    }

    @Test
    fun `lerp background with a is Not Null and b is Null`() {
        val t = 0.1f
        val color1 = Color(0xf)
        val textStyle1 = TextStyle(background = color1)
        val textStyle2 = TextStyle(background = null)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.background).isEqualTo(lerp(color1, Color.Transparent, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is smaller than half`() {
        val color1 = Color(0x0)
        val color2 = Color(0xf)
        val t = 0.2f
        val textStyle1 = TextStyle(background = color1)
        val textStyle2 = TextStyle(background = color2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is larger than half`() {
        val color1 = Color(0x0)
        val color2 = Color(0xf)
        val t = 0.8f
        val textStyle1 = TextStyle(background = color1)
        val textStyle2 = TextStyle(background = color2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp decoration with a and b are not Null and t is smaller than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.2f
        val textStyle1 = TextStyle(decoration = decoration1)
        val textStyle2 = TextStyle(decoration = decoration2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.decoration).isEqualTo(decoration1)
    }

    @Test
    fun `lerp decoration with a and b are not Null and t is larger than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.8f
        val textStyle1 = TextStyle(decoration = decoration1)
        val textStyle2 = TextStyle(decoration = decoration2)

        val newTextStyle = lerp(start = textStyle1, stop = textStyle2, fraction = t)

        assertThat(newTextStyle.decoration).isEqualTo(decoration2)
    }

    @Test
    fun `toSpanStyle return attributes with correct values`() {
        val color = Color.Red
        val fontSize = 56.sp
        val fontWeight = FontWeight.Bold
        val fontStyle = FontStyle.Italic
        val fontSynthesis = FontSynthesis.All
        val fontFamily = FontFamily("myfontfamily")
        val fontFeatureSettings = "font feature settings"
        val letterSpacing = 0.2.sp
        val baselineShift = BaselineShift.Subscript
        val textGeometricTransform = TextGeometricTransform(scaleX = 0.5f, skewX = 0.6f)
        val localeList = LocaleList("tr-TR")
        val background = Color.Yellow
        val decoration = TextDecoration.Underline
        val shadow = Shadow(color = Color.Green, offset = Offset(2f, 4f))

        val textStyle = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            decoration = decoration,
            shadow = shadow
        )

        assertThat(textStyle.toSpanStyle()).isEqualTo(
            SpanStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                decoration = decoration,
                shadow = shadow
            )
        )
    }
}
