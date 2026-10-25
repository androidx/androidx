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

package androidx.compose.ui.text

import android.graphics.Typeface
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.text.style.modulate
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.random.Random
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.whenever

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class SpanStyleTest {
    @Test
    fun `constructor with default values`() {
        val style = SpanStyle()

        assertThat(style.brush).isNull()
        assertThat(style.color).isEqualTo(Color.Unspecified)
        assertThat(style.fontSize.isUnspecified).isTrue()
        assertThat(style.fontWeight).isNull()
        assertThat(style.fontStyle).isNull()
        assertThat(style.letterSpacing.isUnspecified).isTrue()
        assertThat(style.localeList).isNull()
        assertThat(style.background).isEqualTo(Color.Unspecified)
        assertThat(style.textDecoration).isNull()
        assertThat(style.fontFamily).isNull()
        assertThat(style.drawStyle).isNull()
    }

    @Test
    fun `constructor with customized brush`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)

        assertThat(style.brush).isEqualTo(brush)
    }

    @Test
    fun `constructor with customized brush and alpha`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)

        assertThat(style.brush).isEqualTo(brush)
        assertThat(style.alpha).isEqualTo(0.3f)
    }

    @Test
    fun `constructor with gradient brush has unspecified color`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `constructor with SolidColor converts to regular color`() {
        val brush = SolidColor(Color.Red)

        val style = SpanStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Red)
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color.Red

        val style = SpanStyle(color = color)

        assertThat(style.color).isEqualTo(color)
    }

    @Test
    fun `constructor with half-transparent color`() {
        val color = Color.Red.copy(alpha = 0.5f)

        val style = SpanStyle(color = color)

        assertThat(style.color).isEqualTo(color)
        assertThat(style.alpha).isWithin(1f / 256f).of(0.5f)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val style = SpanStyle(fontSize = fontSize)

        assertThat(style.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `constructor with customized fontWeight`() {
        val fontWeight = FontWeight.W500

        val style = SpanStyle(fontWeight = fontWeight)

        assertThat(style.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `constructor with customized fontStyle`() {
        val fontStyle = FontStyle.Italic

        val style = SpanStyle(fontStyle = fontStyle)

        assertThat(style.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `constructor with customized letterSpacing`() {
        val letterSpacing = 1.em

        val style = SpanStyle(letterSpacing = letterSpacing)

        assertThat(style.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `constructor with customized baselineShift`() {
        val baselineShift = BaselineShift.Superscript

        val style = SpanStyle(baselineShift = baselineShift)

        assertThat(style.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `constructor with customized locale`() {
        val localeList = LocaleList("en-US")

        val style = SpanStyle(localeList = localeList)

        assertThat(style.localeList).isEqualTo(localeList)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color.Red

        val style = SpanStyle(background = color)

        assertThat(style.background).isEqualTo(color)
    }

    @Test
    fun `constructor with customized textDecoration`() {
        val decoration = TextDecoration.Underline

        val style = SpanStyle(textDecoration = decoration)

        assertThat(style.textDecoration).isEqualTo(decoration)
    }

    @Test
    fun `constructor with customized fontFamily`() {
        val fontFamily = FontFamily.SansSerif

        val style = SpanStyle(fontFamily = fontFamily)

        assertThat(style.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `constructor with customized drawStyle`() {
        val stroke = Stroke(width = 4f)

        val style = SpanStyle(drawStyle = stroke)

        assertThat(style.drawStyle).isEqualTo(stroke)
    }

    @Test
    fun `merge with empty other should return this`() {
        val style = SpanStyle()

        val newSpanStyle = style.merge()

        assertThat(newSpanStyle).isEqualTo(style)
    }

    @Test
    fun `merge with other's color is null should use this color`() {
        val style = SpanStyle(color = Color.Red)

        val newSpanStyle = style.merge(SpanStyle(color = Color.Unspecified))

        assertThat(newSpanStyle.color).isEqualTo(style.color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val style = SpanStyle(color = Color.Red)
        val otherStyle = SpanStyle(color = Color.Green)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.color).isEqualTo(otherStyle.color)
    }

    @Test
    fun `merge with other's fontFamily is unspecified should use this' fontFamily`() {
        val style = SpanStyle(fontFamily = FontFamily.SansSerif)

        val newSpanStyle = style.merge(SpanStyle(fontFamily = null))

        assertThat(newSpanStyle.fontFamily).isEqualTo(style.fontFamily)
    }

    @Test
    fun `merge with other's fontFamily is set should use other's fontFamily`() {
        val style = SpanStyle(fontFamily = FontFamily.SansSerif)
        val otherStyle = SpanStyle(fontFamily = FontFamily.Serif)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontFamily).isEqualTo(otherStyle.fontFamily)
    }

    @Test
    fun `merge with other's fontSize is null should use this' fontSize`() {
        val style = SpanStyle(fontSize = 3.5.sp)

        val newSpanStyle = style.merge(SpanStyle(fontSize = TextUnit.Unspecified))

        assertThat(newSpanStyle.fontSize).isEqualTo(style.fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val style = SpanStyle(fontSize = 3.5.sp)
        val otherStyle = SpanStyle(fontSize = 8.7.sp)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontSize).isEqualTo(otherStyle.fontSize)
    }

    @Test
    fun `merge with other's fontWeight is null should use this' fontWeight`() {
        val style = SpanStyle(fontWeight = FontWeight.W300)

        val newSpanStyle = style.merge(SpanStyle(fontWeight = null))

        assertThat(newSpanStyle.fontWeight).isEqualTo(style.fontWeight)
    }

    @Test
    fun `merge with other's fontWeight is set should use other's fontWeight`() {
        val style = SpanStyle(fontWeight = FontWeight.W300)
        val otherStyle = SpanStyle(fontWeight = FontWeight.W500)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontWeight).isEqualTo(otherStyle.fontWeight)
    }

    @Test
    fun `merge with other's fontStyle is null should use this' fontStyle`() {
        val style = SpanStyle(fontStyle = FontStyle.Italic)

        val newSpanStyle = style.merge(SpanStyle(fontStyle = null))

        assertThat(newSpanStyle.fontStyle).isEqualTo(style.fontStyle)
    }

    @Test
    fun `merge with other's fontStyle is set should use other's fontStyle`() {
        val style = SpanStyle(fontStyle = FontStyle.Normal)
        val otherStyle = SpanStyle(fontStyle = FontStyle.Italic)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontStyle).isEqualTo(otherStyle.fontStyle)
    }

    @Test
    fun `merge with other's fontSynthesis is null should use this' fontSynthesis`() {
        val style = SpanStyle(fontSynthesis = FontSynthesis.Style)

        val newSpanStyle = style.merge(SpanStyle(fontSynthesis = null))

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(style.fontSynthesis)
    }

    @Test
    fun `merge with other's fontSynthesis is set should use other's fontSynthesis`() {
        val style = SpanStyle(fontSynthesis = FontSynthesis.Style)
        val otherStyle = SpanStyle(fontSynthesis = FontSynthesis.Weight)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(otherStyle.fontSynthesis)
    }

    @Test
    fun `merge with other's fontFeature is null should use this' fontSynthesis`() {
        val style = SpanStyle(fontFeatureSettings = "\"kern\" 0")

        val newSpanStyle = style.merge(SpanStyle(fontFeatureSettings = null))

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(style.fontFeatureSettings)
    }

    @Test
    fun `merge with other's fontFeature is set should use other's fontSynthesis`() {
        val style = SpanStyle(fontFeatureSettings = "\"kern\" 0")
        val otherStyle = SpanStyle(fontFeatureSettings = "\"kern\" 1")

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(otherStyle.fontFeatureSettings)
    }

    @Test
    fun `merge with other's letterSpacing is unspecified should use this' letterSpacing`() {
        val style = SpanStyle(letterSpacing = 1.2.em)

        val newSpanStyle = style.merge(SpanStyle(letterSpacing = TextUnit.Unspecified))

        assertThat(newSpanStyle.letterSpacing).isEqualTo(style.letterSpacing)
    }

    @Test
    fun `merge with other's letterSpacing is set should use other's letterSpacing`() {
        val style = SpanStyle(letterSpacing = 1.2.em)
        val otherStyle = SpanStyle(letterSpacing = 1.5.em)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.letterSpacing).isEqualTo(otherStyle.letterSpacing)
    }

    @Test
    fun `merge with other's baselineShift is null should use this' baselineShift`() {
        val style = SpanStyle(baselineShift = BaselineShift.Superscript)

        val newSpanStyle = style.merge(SpanStyle(baselineShift = null))

        assertThat(newSpanStyle.baselineShift).isEqualTo(style.baselineShift)
    }

    @Test
    fun `merge with other's baselineShift is set should use other's baselineShift`() {
        val style = SpanStyle(baselineShift = BaselineShift.Superscript)
        val otherStyle = SpanStyle(baselineShift = BaselineShift.Subscript)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.baselineShift).isEqualTo(otherStyle.baselineShift)
    }

    @Test
    fun `merge with other's background is null should use this' background`() {
        val style = SpanStyle(background = Color.Red)

        val newSpanStyle = style.merge(SpanStyle(background = Color.Unspecified))

        assertThat(newSpanStyle.background).isEqualTo(style.background)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val style = SpanStyle(background = Color.Red)
        val otherStyle = SpanStyle(background = Color.Green)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.background).isEqualTo(otherStyle.background)
    }

    @Test
    fun `merge with other's textDecoration is null should use this' textDecoration`() {
        val style = SpanStyle(textDecoration = TextDecoration.LineThrough)

        val newSpanStyle = style.merge(SpanStyle(textDecoration = null))

        assertThat(newSpanStyle.textDecoration).isEqualTo(style.textDecoration)
    }

    @Test
    fun `merge with other's textDecoration is set should use other's textDecoration`() {
        val style = SpanStyle(textDecoration = TextDecoration.LineThrough)
        val otherStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.textDecoration).isEqualTo(otherStyle.textDecoration)
    }

    @Test
    fun `merge with other's locale is null should use this' locale`() {
        val style = SpanStyle(localeList = LocaleList("en-US"))

        val newSpanStyle = style.merge(SpanStyle(localeList = null))

        assertThat(newSpanStyle.localeList).isEqualTo(style.localeList)
    }

    @Test
    fun `merge with other's locale is set should use other's locale`() {
        val style = SpanStyle(localeList = LocaleList("en-US"))
        val otherStyle = SpanStyle(localeList = LocaleList("ja-JP"))

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.localeList).isEqualTo(otherStyle.localeList)
    }

    @Test
    fun `merge with null platformStyles has null platformStyle`() {
        val style = SpanStyle(platformStyle = null)
        val otherStyle = SpanStyle(platformStyle = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle).isNull()
    }

    @Test
    fun `merge with brush has other brush and no color`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(color = Color.Red)
        val otherStyle = SpanStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @Test
    fun `merge with unspecified brush has original brush`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush)
        val otherStyle = SpanStyle()

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @Test
    fun `merge brush with brush uses other's alpha`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)
        val otherStyle = SpanStyle(brush = brush, alpha = 0.6f)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.6f)
    }

    @Test
    fun `merge brush with brush uses current alpha if other's is NaN`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = SpanStyle(brush = brush, alpha = 0.3f)
        val otherStyle = SpanStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.3f)
    }

    @Test
    fun `merge with other's drawStyle is null should use this' drawStyle`() {
        val drawStyle1 = Stroke(cap = StrokeCap.Butt)
        val style = SpanStyle(drawStyle = drawStyle1)

        val newSpanStyle = style.merge(SpanStyle(drawStyle = null))

        assertThat(newSpanStyle.drawStyle).isEqualTo(drawStyle1)
    }

    @Test
    fun `merge with other's drawStyle is set should use other's drawStyle`() {
        val drawStyle1 = Stroke(cap = StrokeCap.Butt)
        val drawStyle2 = Fill
        val style = SpanStyle(drawStyle = drawStyle1)
        val otherStyle = SpanStyle(drawStyle = drawStyle2)

        val newSpanStyle = style.merge(otherStyle)

        assertThat(newSpanStyle.drawStyle).isEqualTo(otherStyle.drawStyle)
    }

    @Test
    fun `plus operator merges`() {
        val style =
            SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold) +
                SpanStyle(color = Color.Green, fontFamily = FontFamily.Cursive)

        assertThat(style)
            .isEqualTo(
                SpanStyle(
                    color = Color.Green, // overridden by RHS
                    fontWeight = FontWeight.Bold, // from LHS,
                    fontFamily = FontFamily.Cursive, // from RHS
                )
            )
    }

    @Test
    fun `lerp color with a and b are not Null`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.3f
        val style1 = SpanStyle(color = color1)
        val style2 = SpanStyle(color = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp color with a and b are unspecified`() {
        val style1 = SpanStyle(color = Color.Unspecified)
        val style2 = SpanStyle(color = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = 0.3f)

        assertThat(newSpanStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `when lerp from Specified to Unspecified color, uses Color lerp logic`() {
        val t = 0.3f
        val color1 = Color.Red
        val color2 = Color.Unspecified
        val style1 = SpanStyle(color = color1)
        val style2 = SpanStyle(color = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is smaller than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.3f
        val style1 = SpanStyle(fontFamily = fontFamily1)
        val style2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily1)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is larger than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.8f
        val style1 = SpanStyle(fontFamily = fontFamily1)
        val style2 = SpanStyle(fontFamily = fontFamily2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFamily).isEqualTo(fontFamily2)
    }

    @Test
    fun `lerp fontSize with a and b are not Null`() {
        val fontSize1 = 8.sp
        val fontSize2 = 16.sp
        val t = 0.8f
        val style1 = SpanStyle(fontSize = fontSize1)
        val style2 = SpanStyle(fontSize = fontSize2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        // a + (b - a) * t = 8.0f + (16.0f  - 8.0f) * 0.8f = 14.4f
        assertThat(newSpanStyle.fontSize).isEqualTo(14.4.sp)
    }

    @Test
    fun `lerp fontWeight with a and b are not Null`() {
        val fontWeight1 = FontWeight.W200
        val fontWeight2 = FontWeight.W500
        val t = 0.8f
        val style1 = SpanStyle(fontWeight = fontWeight1)
        val style2 = SpanStyle(fontWeight = fontWeight2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontWeight).isEqualTo(lerp(fontWeight1, fontWeight2, t))
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is smaller than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.3f
        val style1 = SpanStyle(fontStyle = fontStyle1)
        val style2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle1)
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is larger than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.8f
        val style1 = SpanStyle(fontStyle = fontStyle1)
        val style2 = SpanStyle(fontStyle = fontStyle2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontStyle).isEqualTo(fontStyle2)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is smaller than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val style2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis1)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is larger than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontSynthesis = fontSynthesis1)
        val style2 = SpanStyle(fontSynthesis = fontSynthesis2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontSynthesis).isEqualTo(fontSynthesis2)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is smaller than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings1)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is larger than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = SpanStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = SpanStyle(fontFeatureSettings = fontFeatureSettings2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings2)
    }

    @Test
    fun `lerp baselineShift with a and b are not Null`() {
        val baselineShift1 = BaselineShift(1.0f)
        val baselineShift2 = BaselineShift(2.0f)
        val t = 0.3f
        val style1 = SpanStyle(baselineShift = baselineShift1)
        val style2 = SpanStyle(baselineShift = baselineShift2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.baselineShift).isEqualTo(lerp(baselineShift1, baselineShift2, t))
    }

    @Test
    fun `lerp textGeometricTransform with a and b are not Null`() {
        val textTransform1 = TextGeometricTransform(scaleX = 1.5f, skewX = 0.1f)
        val textTransform2 = TextGeometricTransform(scaleX = 1.0f, skewX = 0.3f)
        val t = 0.3f
        val style1 = SpanStyle(textGeometricTransform = textTransform1)
        val style2 = SpanStyle(textGeometricTransform = textTransform2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textGeometricTransform)
            .isEqualTo(lerp(textTransform1, textTransform2, t))
    }

    @Test
    fun `lerp locale with a and b are not Null and t is smaller than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.3f
        val style1 = SpanStyle(localeList = localeList1)
        val style2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList1)
    }

    @Test
    fun `lerp locale with a and b are not Null and t is larger than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.8f
        val style1 = SpanStyle(localeList = localeList1)
        val style2 = SpanStyle(localeList = localeList2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.localeList).isEqualTo(localeList2)
    }

    @Test
    fun `lerp background with a and b are Null and t is smaller than half`() {
        val style1 = SpanStyle(background = Color.Unspecified)
        val style2 = SpanStyle(background = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = 0.1f)

        assertThat(newSpanStyle.background).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp background with a is Null and b is not Null`() {
        val t = 0.1f
        val color1 = Color.Unspecified
        val style1 = SpanStyle(background = color1)
        val color2 = Color.Red
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a is Not Null and b is Null`() {
        val t = 0.1f
        val color1 = Color.Red
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = Color.Unspecified)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, Color.Unspecified, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is smaller than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.2f
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is larger than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.8f
        val style1 = SpanStyle(background = color1)
        val style2 = SpanStyle(background = color2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is smaller than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.2f
        val style1 = SpanStyle(textDecoration = decoration1)
        val style2 = SpanStyle(textDecoration = decoration2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textDecoration).isEqualTo(decoration1)
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is larger than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.8f
        val style1 = SpanStyle(textDecoration = decoration1)
        val style2 = SpanStyle(textDecoration = decoration2)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newSpanStyle.textDecoration).isEqualTo(decoration2)
    }

    @Test
    fun `lerp with null platformStyles have null platformStyle`() {
        val style = SpanStyle(platformStyle = null)
        val otherStyle = SpanStyle(platformStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.platformStyle).isNull()
    }

    @Test
    fun `lerp brush with a specified, b specified and t is smaller than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.brush).isEqualTo(brush)
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp brush with a specified, b specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isEqualTo(null)
        assertThat(newStyle.color).isEqualTo(Color.Red)
    }

    @Test
    fun `lerp brush with a specified, b not specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = SpanStyle(brush = brush)
        val style2 = SpanStyle()

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isNull()
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun lerp_shadow_start_null_interpolates() {
        val start = Shadow(Color.Red, Offset(10f, 10f), 4f)
        val fraction = 0.5f
        val style1 = SpanStyle(shadow = start)
        val style2 = SpanStyle(shadow = null)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newSpanStyle.shadow)
            .isEqualTo(lerp(start, start.copy(color = start.color.copy(alpha = 0.0f)), fraction))
    }

    @Test
    fun lerp_shadow_null_stop_interpolates() {
        val stop = Shadow(Color.Blue, Offset(20f, 20f), 8f)
        val fraction = 0.5f
        val style1 = SpanStyle(shadow = null)
        val style2 = SpanStyle(shadow = stop)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newSpanStyle.shadow)
            .isEqualTo(lerp(stop.copy(color = stop.color.copy(alpha = 0.0f)), stop, fraction))
    }

    @Test
    fun lerp_shadow_start_stop_interpolates() {
        val start = Shadow(Color.Red, Offset(10f, 10f), 4f)
        val stop = Shadow(Color.Blue, Offset(20f, 20f), 8f)
        val fraction = 0.5f
        val style1 = SpanStyle(shadow = start)
        val style2 = SpanStyle(shadow = stop)

        val newSpanStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newSpanStyle.shadow).isEqualTo(lerp(start, stop, fraction))
    }

    @Test
    fun fontWeightRef_andLetterSpacing_surviveMerge() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val customWeight = FontWeight(350)
        assertThat(SpanStyle(fontWeight = customWeight).fontWeight).isSameInstanceAs(customWeight)

        val merged =
            SpanStyle(fontSize = 16.sp, letterSpacing = 0.37.sp)
                .merge(SpanStyle(fontWeight = FontWeight.Bold))
        assertThat(merged.fontWeight).isSameInstanceAs(FontWeight.Bold)
        assertThat(merged.letterSpacing).isEqualTo(0.37.sp)
        assertThat(merged.extras).isNull()
    }

    @Test
    fun semanticFlags_eachMatchOnlyTheirOwnField() {
        assertThat(SpanStyle()).hasNoSemanticFlags().doesNotHaveMetricAffectingAttributes()

        for (shift in listOf(BaselineShift.None, BaselineShift.Superscript, BaselineShift(0.3f))) {
            assertThat(SpanStyle(baselineShift = shift)).hasOnlyBaselineShift(shift)
        }
        for (decoration in
            listOf(
                TextDecoration.None,
                TextDecoration.Underline,
                TextDecoration.LineThrough,
                TextDecoration.Underline + TextDecoration.LineThrough,
            )) {
            assertThat(SpanStyle(textDecoration = decoration)).hasOnlyTextDecoration(decoration)
        }
        for (transform in listOf(TextGeometricTransform.None, TextGeometricTransform(2f, 0.5f))) {
            assertThat(SpanStyle(textGeometricTransform = transform))
                .hasOnlyTextGeometricTransform(transform)
                .hasMetricAffectingAttributes()
        }
        for (synthesis in
            listOf(
                FontSynthesis.None,
                FontSynthesis.Weight,
                FontSynthesis.Style,
                FontSynthesis.All,
            )) {
            assertThat(SpanStyle(fontSynthesis = synthesis)).hasOnlyFontSynthesis(synthesis)
        }
        for (fontStyle in listOf(FontStyle.Normal, FontStyle.Italic)) {
            assertThat(SpanStyle(fontStyle = fontStyle)).hasOnlyFontStyle(fontStyle)
        }
    }

    @Test
    fun metricAffectingAttributes_matchFieldsThatChangeGlyphMetrics() {
        assertThat(SpanStyle(fontSize = 12.sp)).hasMetricAffectingAttributes()
        assertThat(SpanStyle(letterSpacing = 1.sp)).hasMetricAffectingAttributes()
        assertThat(SpanStyle(fontFeatureSettings = "")).hasMetricAffectingAttributes()
        assertThat(SpanStyle(localeList = LocaleList("en"))).hasMetricAffectingAttributes()
        assertThat(SpanStyle(baselineShift = BaselineShift.Subscript))
            .hasMetricAffectingAttributes()
        // None and Unspecified do not move the baseline, like BaselineShift.isApplicable.
        assertThat(SpanStyle(baselineShift = BaselineShift.None))
            .doesNotHaveMetricAffectingAttributes()
        assertThat(SpanStyle(color = Color.Red)).doesNotHaveMetricAffectingAttributes()
        assertThat(SpanStyle(textDecoration = TextDecoration.Underline))
            .doesNotHaveMetricAffectingAttributes()
        assertThat(SpanStyle(background = Color.Red)).doesNotHaveMetricAffectingAttributes()
    }

    @Test
    fun merge_presetInOtherReplacesCustomInThis() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val custom =
            SpanStyle(
                baselineShift = BaselineShift(0.3f),
                shadow = Shadow(Color.Red, blurRadius = 2f),
                background = Color.Red,
            )
        val presets =
            SpanStyle(
                baselineShift = BaselineShift.None,
                shadow = Shadow.None,
                background = Color.Transparent,
            )
        val merged = custom.merge(presets)
        assertThat(merged).isEqualTo(presets)
        assertThat(merged.extras).isNull()
        assertThat(presets.merge(custom)).isEqualTo(custom)
    }

    @Test
    fun resolvedDefaults_unspecifiedBaselineShiftBecomesNone() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val resolved =
            resolveSpanStyleDefaults(SpanStyle(baselineShift = BaselineShift.Unspecified))
        val expected = resolveSpanStyleDefaults(SpanStyle(baselineShift = BaselineShift.None))
        assertThat(resolved.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(resolved.extras).isNull()
        assertThat(resolved.bits and 1).isEqualTo(0)
        assertThat(resolved).hasEqualValueAndHashCode(expected)
        assertThat(resolved.merge(SpanStyle())).isEqualTo(expected)
    }

    @Suppress("DEPRECATION")
    @Test
    fun fontStyle_validValuesRoundTrip_andInvalidValuesThrow() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val saverScope = SaverScope { true }
        for (raw in listOf(0, 1)) {
            val style = SpanStyle(fontStyle = FontStyle(raw))
            assertThat(style).hasOnlyFontStyle(FontStyle(raw))
            assertThat(style.extras).isNull()
            val saved = with(SpanStyleSaver) { saverScope.save(style) }!!
            val restored = SpanStyleSaver.restore(saved)!!
            assertThat(restored).isEqualTo(style)
            assertThat(restored.fontStyle).isEqualTo(FontStyle(raw))
        }
    }

    @Test
    fun merge_colorOntoBrush_preservesBrush() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val shaderBrush = Brush.linearGradient(listOf(Color.Blue, Color.Cyan))
        val brushStyle = SpanStyle(brush = shaderBrush, alpha = 0.75f, fontSize = 16.sp)

        // Merging a color-only style onto a brush style must preserve the brush and alpha
        // (regression test for b/230787077 across both merge and fastMerge).
        val mergedColorOnly = brushStyle.merge(SpanStyle(color = Color.Red))
        val fastMergedColorOnly = brushStyle.callFastMerge(color = Color.Red)
        assertThat(mergedColorOnly).isSameInstanceAs(brushStyle)
        assertThat(fastMergedColorOnly).isSameInstanceAs(brushStyle)
        assertThat(mergedColorOnly).hasBrushForeground(shaderBrush, 0.75f)

        // Merging a color + layout attribute onto a brush style updates the layout attribute
        // while keeping the brush and zeroing colorXor.
        val mergedWithWeight =
            brushStyle.merge(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold))
        val fastMergedWithWeight =
            brushStyle.callFastMerge(color = Color.Red, fontWeight = FontWeight.Bold)
        assertThat(mergedWithWeight)
            .hasEqualValueAndHashCode(fastMergedWithWeight)
            .hasBrushForeground(shaderBrush, 0.75f)
        assertThat(mergedWithWeight.fontWeight).isEqualTo(FontWeight.Bold)
    }

    @Test
    fun negativeZeroBaselineShift_preservesRawBitsInExtras() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val negativeZeroStyle = SpanStyle(baselineShift = BaselineShift(-0f))
        val positiveZeroStyle = SpanStyle(baselineShift = BaselineShift.None)
        assertThat(negativeZeroStyle.baselineShift?.multiplier?.toRawBits())
            .isEqualTo((-0f).toRawBits())
        assertThat(negativeZeroStyle.baselineShiftOrNone.multiplier.toRawBits())
            .isEqualTo((-0f).toRawBits())
        assertThat(negativeZeroStyle).isNotEqualTo(positiveZeroStyle)
        assertThat(positiveZeroStyle.extras).isNull()
        assertThat(negativeZeroStyle.extras).isNotNull()
    }

    @Test
    fun saverRoundTrip_preservesPresetAndCustomValues() {
        val saverScope = SaverScope { true }
        val styles =
            listOf(
                SpanStyle(),
                SpanStyle(color = Color.Black),
                SpanStyle(color = Color.Red),
                SpanStyle(fontFeatureSettings = ""),
                SpanStyle(background = Color.Transparent),
                SpanStyle(shadow = Shadow.None),
                SpanStyle(baselineShift = BaselineShift.None),
                SpanStyle(baselineShift = BaselineShift(-0f)),
                SpanStyle(textGeometricTransform = TextGeometricTransform.None),
                SpanStyle(fontStyle = FontStyle.Italic, fontSynthesis = FontSynthesis.Weight),
                SpanStyle(
                    color = Color.Cyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.Style,
                    fontFeatureSettings = "smcp",
                    letterSpacing = 0.25.em,
                    baselineShift = BaselineShift.Superscript,
                    textGeometricTransform = TextGeometricTransform(1.2f, -0.1f),
                    localeList = LocaleList("en-US"),
                    background = Color.Yellow,
                    textDecoration = TextDecoration.Underline + TextDecoration.LineThrough,
                    shadow = Shadow(Color.Magenta, Offset(2f, 3f), 4f),
                ),
            )
        for (style in styles) {
            val saved = with(SpanStyleSaver) { saverScope.save(style) }!!
            val restored = SpanStyleSaver.restore(saved)!!
            assertThat(restored).hasEqualValueAndHashCode(style)
        }
    }

    @Test
    fun invokeResolveTypeface_passesBoxedValueClassesToGenericFunction4() {
        var capturedFamily: Any? = null
        var capturedWeight: Any? = null
        var capturedStyle: Any? = null
        var capturedSynthesis: Any? = null
        val genericCallback =
            object : Function4<Any?, Any?, Any?, Any?, Typeface?> {
                override fun invoke(
                    fontFamily: Any?,
                    fontWeight: Any?,
                    fontStyle: Any?,
                    fontSynthesis: Any?,
                ): Typeface? {
                    capturedFamily = fontFamily
                    capturedWeight = fontWeight
                    capturedStyle = fontStyle
                    capturedSynthesis = fontSynthesis
                    return null
                }
            }
        @Suppress("UNCHECKED_CAST")
        val typedCallback =
            genericCallback as (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface

        val style =
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
            )
        style.invokeResolveTypeface(typedCallback)
        assertThat(capturedFamily).isEqualTo(FontFamily.Monospace)
        assertThat(capturedWeight).isEqualTo(FontWeight.Bold)
        assertThat(capturedStyle).isEqualTo(FontStyle.Italic)
        assertThat(capturedSynthesis).isEqualTo(FontSynthesis.Weight)

        SpanStyle().invokeResolveTypeface(typedCallback)
        assertThat(capturedFamily).isNull()
        assertThat(capturedWeight).isEqualTo(FontWeight.Normal)
        assertThat(capturedStyle).isEqualTo(FontStyle.Normal)
        assertThat(capturedSynthesis).isEqualTo(FontSynthesis.All)
    }

    @Test
    fun seededDifferential_equalsHashCodeAndMerge_matchReferenceSpanStyle() {
        val random = Random(0x5EED_59A4L)
        val samples = List(160) { nextRandomSpanStylePair(random) }

        for (i in samples.indices) {
            val (first, firstRef) = samples[i]
            assertThat(resolveSpanStyleDefaults(first)).matchesReference(firstRef.resolveDefaults())

            for ((second, secondRef) in samples) {
                assertThat(first).matchesReferenceEquivalenceWith(second, firstRef, secondRef)
            }

            val (other, otherRef) = samples[(i * 41 + 7) % samples.size]
            val merged = first.merge(other)
            val fastMerged = first.fastMergeFrom(other)
            assertThat(merged)
                .hasEqualValueAndHashCode(fastMerged)
                .matchesReference(firstRef.merge(otherRef))
        }
    }

    @Test
    fun hashCodeLayoutAffectingAttributes_isEqual_whenExtrasContainOnlyNonLayoutFields() {
        val colorStyle = SpanStyle(color = Color.Red)
        val brushStyle =
            SpanStyle(brush = Brush.linearGradient(listOf(Color.Blue, Color.Red)), alpha = 0.5f)
        assertThat(colorStyle.hasSameLayoutAffectingAttributes(brushStyle)).isTrue()
        assertThat(colorStyle.hashCodeLayoutAffectingAttributes())
            .isEqualTo(brushStyle.hashCodeLayoutAffectingAttributes())
    }

    @Test
    fun hashCode_andHashCodeLayoutAffectingAttributes_distributeLowBitsWithoutCollisions() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val layoutStyles = buildList {
            for (size in listOf(TextUnit.Unspecified, 12.sp, 14.sp, 16.sp, 20.sp, 1.25.em)) {
                for (weight in
                    listOf(null, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold)) {
                    for (style in listOf(null, FontStyle.Normal, FontStyle.Italic)) {
                        for (synthesis in
                            listOf(
                                null,
                                FontSynthesis.None,
                                FontSynthesis.Weight,
                                FontSynthesis.All,
                            )) {
                            for (spacing in listOf(TextUnit.Unspecified, 0.sp, 0.5.sp)) {
                                add(
                                    SpanStyle(
                                        fontSize = size,
                                        fontWeight = weight,
                                        fontStyle = style,
                                        fontSynthesis = synthesis,
                                        letterSpacing = spacing,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        val layoutHashes = layoutStyles.map { it.hashCodeLayoutAffectingAttributes() }.toSet()
        assertThat(layoutHashes.size).isEqualTo(layoutStyles.size)
        val low16LayoutBuckets = layoutHashes.map { it and 0xFFFF }.toSet()
        assertThat(low16LayoutBuckets.size).isAtLeast(layoutStyles.size * 9 / 10)

        val fullStyles = layoutStyles.flatMap { base ->
            listOf(
                base,
                base.copy(color = Color.Red),
                base.copy(color = Color.Blue, textDecoration = TextDecoration.Underline),
            )
        }
        val fullHashes = fullStyles.map { it.hashCode() }.toSet()
        assertThat(fullHashes.size).isEqualTo(fullStyles.size)
        val low16FullBuckets = fullHashes.map { it and 0xFFFF }.toSet()
        assertThat(low16FullBuckets.size).isAtLeast(fullStyles.size * 9 / 10)
        assertThat(SpanStyle(color = Color(0xFFFFFFFF), fontWeight = FontWeight(400)).hashCode())
            .isNotEqualTo(
                SpanStyle(color = Color(0xFFFFFFFE), fontWeight = FontWeight(401)).hashCode()
            )
        assertThat(SpanStyle(color = Color(0xFF000001), fontWeight = FontWeight(400)).hashCode())
            .isNotEqualTo(
                SpanStyle(color = Color(0xFF000000), fontWeight = FontWeight(401)).hashCode()
            )
    }

    @Test
    fun internalAndPublicConstructors_enforceInvariantsAndNormalizeForeground() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        // Internal 7-arg constructor clears bit 0 when extras == null and forces bit 0 when extras
        // != null.
        val withoutExtras =
            SpanStyle(
                colorXor = 0L,
                bits = (1 shl 1) or 1,
                extras = null,
                fontSize = TextUnit.Unspecified,
                letterSpacing = TextUnit.Unspecified,
                fontWeight = null,
                fontFamily = null,
            )
        assertThat(withoutExtras.extras).isNull()
        assertThat(withoutExtras.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(withoutExtras).hasValidInvariants()

        val extras =
            SpanStyleExtras(
                null,
                Float.NaN,
                "smcp",
                null,
                null,
                null,
                Color.Unspecified,
                null,
                null,
            )
        val withExtras =
            SpanStyle(
                colorXor = 0L,
                bits = 1 shl 10,
                extras = extras,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        assertThat(withExtras.extras).isSameInstanceAs(extras)
        assertThat(withExtras.fontFeatureSettings).isEqualTo("smcp")
        assertThat(withExtras).hasValidInvariants()

        // Internal TextForegroundStyle constructor & Brush constructor with SolidColor /
        // ShaderBrush.
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        assertThat(SpanStyle(textForegroundStyle = TextForegroundStyle.Unspecified))
            .isEqualTo(SpanStyle())
        assertThat(
                SpanStyle(
                    textForegroundStyle = TextForegroundStyle.from(Color.Green),
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                )
            )
            .isEqualTo(
                SpanStyle(color = Color.Green, fontSize = 16.sp, fontStyle = FontStyle.Italic)
            )
        val fromShader =
            SpanStyle(
                textForegroundStyle = TextForegroundStyle.from(shaderBrush, 0.4f),
                fontWeight = FontWeight.Bold,
            )
        assertThat(fromShader)
            .hasEqualValueAndHashCode(
                SpanStyle(brush = shaderBrush, alpha = 0.4f, fontWeight = FontWeight.Bold)
            )
            .hasValidInvariants()

        for (alpha in listOf(Float.NaN, 1f, 0.5f, 0f)) {
            val solidStyle = SpanStyle(brush = SolidColor(Color.Red), alpha = alpha)
            val expectedColor =
                if (alpha.isNaN() || alpha >= 1f) Color.Red
                else Color.Red.copy(alpha = Color.Red.alpha * alpha)
            assertThat(solidStyle.color).isEqualTo(expectedColor)
            assertThat(solidStyle.brush).isNull()
            assertThat(solidStyle.extras).isNull()
            assertThat(solidStyle.alpha).isEqualTo(expectedColor.alpha)
            assertThat(solidStyle).hasValidInvariants()
        }
        for (emptyBrushStyle in
            listOf(
                SpanStyle(brush = SolidColor(Color.Unspecified), alpha = 0.5f),
                SpanStyle(brush = null, alpha = 0.5f),
            )) {
            assertThat(emptyBrushStyle.color).isEqualTo(Color.Unspecified)
            assertThat(emptyBrushStyle.brush).isNull()
            assertThat(emptyBrushStyle.alpha).isNaN()
            assertThat(emptyBrushStyle.extras).isNull()
        }
    }

    @Test
    fun packSpanColorXor_andColorBrushAlphaGetters_coverAllBranches() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        assertThat(packSpanColorXor(Color.Unspecified)).isEqualTo(0L)
        assertThat(packSpanColorXor(Color.Transparent)).isEqualTo(0x10L)
        assertThat(packSpanColorXor(Color.Black)).isNotEqualTo(0L)

        val displayP3Color = Color(0.25f, 0.5f, 0.75f, 0.8f, ColorSpaces.DisplayP3)
        for (color in
            listOf(
                Color.Unspecified,
                Color.Transparent,
                Color.Black,
                Color(0x80112233),
                displayP3Color,
            )) {
            val style = SpanStyle(color = color)
            assertThat(style.color).isEqualTo(color)
            assertThat(style.colorXor).isEqualTo(packSpanColorXor(color))
            if (color == Color.Unspecified) assertThat(style.alpha).isNaN()
            else assertThat(style.alpha).isEqualTo(color.alpha)
        }

        // Branches where extras != null (from localeList) but extras.brush == null:
        val unspecifiedColorWithExtras =
            SpanStyle(color = Color.Unspecified, localeList = LocaleList("en"))
        assertThat(unspecifiedColorWithExtras.extras).isNotNull()
        assertThat(unspecifiedColorWithExtras.alpha).isNaN()

        val colorWithExtras =
            SpanStyle(color = Color.Red.copy(alpha = 0.25f), localeList = LocaleList("en"))
        assertThat(colorWithExtras.extras).isNotNull()
        assertThat(colorWithExtras.alpha).isWithin(1f / 256f).of(0.25f)

        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        for (alpha in listOf(Float.NaN, 0f, 0.5f, 1f)) {
            val style = SpanStyle(brush = shaderBrush, alpha = alpha)
            assertThat(style).hasBrushForeground(shaderBrush, alpha)
        }
    }

    @Test
    fun packSpanStyleBits_createSpanStyleExtras_andCreateSpanStyle_normalizePresetsAndSingletons() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val allPresets =
            createSpanStyle(
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFeatureSettings = "",
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                background = Color.Transparent,
                textDecoration = TextDecoration.None,
                shadow = Shadow.None,
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Fill,
            )
        assertThat(allPresets.extras).isNull()
        assertThat(allPresets).hasValidInvariants()
        assertThat(allPresets.fontStyle).isEqualTo(BoxedStyleValues.NormalFontStyle)
        assertThat(allPresets.fontSynthesis).isEqualTo(BoxedStyleValues.NoneFontSynthesis)
        assertThat(allPresets.fontFeatureSettings).isEqualTo("")
        assertThat(allPresets.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(allPresets.baselineShiftOrNone).isEqualTo(BaselineShift.None)
        assertThat(allPresets.textGeometricTransform).isSameInstanceAs(TextGeometricTransform.None)
        assertThat(allPresets.background).isEqualTo(Color.Transparent)
        assertThat(allPresets.textDecoration).isSameInstanceAs(TextDecoration.None)
        assertThat(allPresets.shadow).isSameInstanceAs(Shadow.None)
        assertThat(allPresets.platformStyle).isSameInstanceAs(PlatformSpanStyle.Default)
        assertThat(allPresets.drawStyle).isSameInstanceAs(Fill)

        // BoxedStyleValues equality for remaining enum/flag variants:
        assertThat(SpanStyle(fontStyle = FontStyle.Italic).fontStyle)
            .isEqualTo(BoxedStyleValues.ItalicFontStyle)
        assertThat(SpanStyle(fontSynthesis = FontSynthesis.Weight).fontSynthesis)
            .isEqualTo(BoxedStyleValues.WeightFontSynthesis)
        assertThat(SpanStyle(fontSynthesis = FontSynthesis.Style).fontSynthesis)
            .isEqualTo(BoxedStyleValues.StyleFontSynthesis)
        assertThat(SpanStyle(fontSynthesis = FontSynthesis.All).fontSynthesis)
            .isEqualTo(BoxedStyleValues.AllFontSynthesis)
        assertThat(
                SpanStyle(textDecoration = TextDecoration.Underline + TextDecoration.LineThrough)
                    .textDecoration
            )
            .isSameInstanceAs(BoxedStyleValues.CombinedTextDecoration)

        // Freshly allocated instances equal to presets normalize to extras == null.
        for (equalPreset in
            listOf(
                SpanStyle(textGeometricTransform = TextGeometricTransform(1f, 0f)),
                SpanStyle(textGeometricTransform = TextGeometricTransform(1f, -0.0f)),
                SpanStyle(shadow = Shadow(Color.Black, Offset.Zero, 0f)),
                SpanStyle(baselineShift = BaselineShift(0.0f)),
            )) {
            assertThat(equalPreset.extras).isNull()
        }

        // Presets combined with a custom property (localeList) still unpack from bits when extras
        // != null.
        val presetsWithLocale = allPresets.copy(localeList = LocaleList.Empty)
        assertThat(presetsWithLocale.extras).isNotNull()
        assertThat(presetsWithLocale.extras!!.fontFeatureSettings).isNull()
        assertThat(presetsWithLocale.extras!!.baselineShift).isNull()
        assertThat(presetsWithLocale.extras!!.textGeometricTransform).isNull()
        assertThat(presetsWithLocale.extras!!.background).isEqualTo(Color.Unspecified)
        assertThat(presetsWithLocale.extras!!.shadow).isNull()
        assertThat(presetsWithLocale.extras!!.drawStyle).isNull()
        assertThat(presetsWithLocale.fontFeatureSettings).isEqualTo("")
        assertThat(presetsWithLocale.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(presetsWithLocale.baselineShiftOrNone).isEqualTo(BaselineShift.None)
        assertThat(presetsWithLocale.textGeometricTransform).isEqualTo(TextGeometricTransform.None)
        assertThat(presetsWithLocale.localeList).isEqualTo(LocaleList.Empty)
        assertThat(presetsWithLocale.background).isEqualTo(Color.Transparent)
        assertThat(presetsWithLocale.shadow).isEqualTo(Shadow.None)
        assertThat(presetsWithLocale.drawStyle).isEqualTo(Fill)

        // createSpanStyleExtras normalizes alpha to Float.NaN when brush == null.
        val extrasNoBrushWithAlpha =
            createSpanStyleExtras(
                null,
                0.75f,
                "smcp",
                null,
                null,
                null,
                Color.Unspecified,
                null,
                null,
            )!!
        assertThat(extrasNoBrushWithAlpha.alpha).isNaN()
        assertThat(extrasNoBrushWithAlpha.hashCodeNonLayoutAttributes()).isEqualTo(0)

        // createSpanStyle returns shared DefaultSpanStyle when all arguments are default.
        assertThat(createSpanStyle()).isSameInstanceAs(createSpanStyle(color = Color.Unspecified))
        assertThat(createSpanStyle(brush = SolidColor(Color.Unspecified)))
            .isSameInstanceAs(createSpanStyle())

        // Exact bit layout of packSpanStyleBits.
        assertThat(
                packSpanStyleBits(
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.All,
                    fontFeatureSettings = "",
                    textGeometricTransform = TextGeometricTransform.None,
                    background = Color.Transparent,
                    textDecoration = TextDecoration.Underline + TextDecoration.LineThrough,
                    shadow = Shadow.None,
                    platformStyle = PlatformSpanStyle.Default,
                    drawStyle = Fill,
                    baselineShift = BaselineShift.None,
                )
            )
            .isEqualTo(
                (1 shl 1) or
                    (0b111 shl 3) or
                    (0b111 shl 6) or
                    (1 shl 9) or
                    (1 shl 10) or
                    (1 shl 11) or
                    (1 shl 12) or
                    (1 shl 13) or
                    (1 shl 14) or
                    (0b11 shl 15)
            )
    }

    @Test
    fun spanStyleExtras_matches_equals_hashCode_andLayoutVsNonLayoutMethods() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val brush1 = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val brush2 = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
        val fontFeatureSettings = "smcp"
        val baselineShift = BaselineShift(0.25f)
        val geometricTransform = TextGeometricTransform(1.5f, 0.1f)
        val localeList = LocaleList("en-US")
        val background = Color.Red
        val shadow = Shadow(Color.Black, Offset(1f, 2f), 3f)
        val drawStyle = Stroke(width = 2f)

        val base =
            SpanStyleExtras(
                brush1,
                0.5f,
                fontFeatureSettings,
                baselineShift,
                geometricTransform,
                localeList,
                background,
                shadow,
                drawStyle,
            )
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isTrue()
        assertThat(
                base.matches(
                    brush2,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.6f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    Float.NaN,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    "liga",
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    BaselineShift(-0.25f),
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    TextGeometricTransform(1.2f, -0.2f),
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    LocaleList("ja-JP"),
                    background,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    Color.Blue,
                    shadow,
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    Shadow(Color.Gray),
                    drawStyle,
                )
            )
            .isFalse()
        assertThat(
                base.matches(
                    brush1,
                    0.5f,
                    fontFeatureSettings,
                    baselineShift,
                    geometricTransform,
                    localeList,
                    background,
                    shadow,
                    Stroke(width = 4f),
                )
            )
            .isFalse()

        val positiveZeroAlphaExtras =
            SpanStyleExtras(brush1, +0.0f, null, null, null, null, Color.Unspecified, null, null)
        val negativeZeroAlphaExtras =
            SpanStyleExtras(brush1, -0.0f, null, null, null, null, Color.Unspecified, null, null)
        assertThat(
                positiveZeroAlphaExtras.matches(
                    brush1,
                    -0.0f,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                )
            )
            .isFalse()
        assertThat(positiveZeroAlphaExtras.hasSameNonLayoutAttributes(negativeZeroAlphaExtras))
            .isFalse()

        for (singleLayout in
            listOf(
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    fontFeatureSettings,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                ),
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    null,
                    baselineShift,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                ),
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    null,
                    null,
                    geometricTransform,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                ),
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    null,
                    null,
                    null,
                    localeList,
                    Color.Unspecified,
                    null,
                    null,
                ),
                SpanStyleExtras(null, Float.NaN, null, null, null, null, background, null, null),
            )) {
            assertThat(singleLayout.hasSameLayoutAttributes(null)).isFalse()
            assertThat(singleLayout.hasSameNonLayoutAttributes(null)).isTrue()
            assertThat(singleLayout.hashCodeNonLayoutAttributes()).isEqualTo(0)
        }
        for (singleNonLayout in
            listOf(
                SpanStyleExtras(
                    brush1,
                    0.5f,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                ),
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    shadow,
                    null,
                ),
                SpanStyleExtras(
                    null,
                    Float.NaN,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    drawStyle,
                ),
            )) {
            assertThat(singleNonLayout.hasSameNonLayoutAttributes(null)).isFalse()
            assertThat(singleNonLayout.hasSameLayoutAttributes(null)).isTrue()
            assertThat(singleNonLayout.hashCodeLayoutAffectingAttributes()).isEqualTo(0)
        }

        val baseCopy =
            SpanStyleExtras(
                brush1,
                0.5f,
                fontFeatureSettings,
                baselineShift,
                geometricTransform,
                localeList,
                background,
                shadow,
                drawStyle,
            )
        assertThat(base).isEqualTo(baseCopy)
        assertThat(base.hashCode()).isEqualTo(baseCopy.hashCode())
        assertThat(base.equals(null)).isFalse()
        assertThat(base.equals("other")).isFalse()
    }

    @Test
    fun copy_andCopyFontAttributes_andIsDefault_coverAllBranches() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val otherShader = Brush.linearGradient(listOf(Color.Cyan, Color.Magenta))
        val brushStyle =
            SpanStyle(
                brush = shaderBrush,
                alpha = 0.6f,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                fontFamily = FontFamily.Serif,
                fontFeatureSettings = "smcp",
                letterSpacing = 0.2.em,
                baselineShift = BaselineShift.Superscript,
                textGeometricTransform = TextGeometricTransform(1.2f, 0.1f),
                localeList = LocaleList("en-US"),
                background = Color.Yellow,
                textDecoration = TextDecoration.Underline,
                shadow = Shadow(Color.Black, Offset(1f, 1f), 2f),
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Stroke(width = 2f),
            )

        // 1. copy() with default `color` preserves brush and alpha; explicit `color` replaces
        // brush.
        val copiedKeepBrush = brushStyle.copy(fontSize = 20.sp)
        assertThat(copiedKeepBrush.brush).isSameInstanceAs(shaderBrush)
        assertThat(copiedKeepBrush.alpha).isEqualTo(0.6f)
        assertThat(copiedKeepBrush.fontSize).isEqualTo(20.sp)

        val copiedReplaceWithColor = brushStyle.copy(color = Color.Green)
        assertThat(copiedReplaceWithColor.brush).isNull()
        assertThat(copiedReplaceWithColor.color).isEqualTo(Color.Green)
        assertThat(copiedReplaceWithColor.alpha).isEqualTo(1f)

        // 2. copy(brush = ...) inherits `this.alpha` by default, or uses new alpha if provided.
        assertThat(brushStyle.copy(brush = otherShader).alpha).isEqualTo(0.6f)
        assertThat(brushStyle.copy(brush = otherShader, alpha = 0.25f).alpha).isEqualTo(0.25f)

        // 3. copyFontAttributes() retains ONLY fontStyle, fontSynthesis, fontWeight, and
        // fontFamily.
        val fontAttributesOnly = brushStyle.copyFontAttributes()
        assertThat(fontAttributesOnly)
            .isEqualTo(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.Weight,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                )
            )
        assertThat(fontAttributesOnly.extras).isNull()

        // 4. isDefault() across all 6 core fields.
        assertThat(SpanStyle().isDefault()).isTrue()
        for (nonDefault in
            listOf(
                SpanStyle(color = Color.Transparent),
                SpanStyle(fontStyle = FontStyle.Normal),
                SpanStyle(localeList = LocaleList("en")),
                SpanStyle(fontSize = 12.sp),
                SpanStyle(letterSpacing = 0.sp),
                SpanStyle(fontWeight = FontWeight.Normal),
                SpanStyle(fontFamily = FontFamily.Default),
            )) {
            assertThat(nonDefault.isDefault()).isFalse()
            assertThat(nonDefault).hasValidInvariants()
        }
    }

    @Test
    fun singleAttributeFlipMatrix_equals_layout_nonLayout_andHashCode() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val shader1 = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val shader2 = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
        data class FlipCase(
            val name: String,
            val first: SpanStyle,
            val second: SpanStyle,
            val isLayout: Boolean,
        )

        val cases =
            listOf(
                FlipCase(
                    "fontSize: Unspecified vs 14.sp",
                    SpanStyle(),
                    SpanStyle(fontSize = 14.sp),
                    true,
                ),
                FlipCase(
                    "fontSize: 14.sp vs 16.sp",
                    SpanStyle(fontSize = 14.sp),
                    SpanStyle(fontSize = 16.sp),
                    true,
                ),
                FlipCase(
                    "fontSize: 14.sp vs 14.em",
                    SpanStyle(fontSize = 14.sp),
                    SpanStyle(fontSize = 14.em),
                    true,
                ),
                FlipCase(
                    "letterSpacing: Unspecified vs 0.sp",
                    SpanStyle(),
                    SpanStyle(letterSpacing = 0.sp),
                    true,
                ),
                FlipCase(
                    "letterSpacing: 0.sp vs 1.sp",
                    SpanStyle(letterSpacing = 0.sp),
                    SpanStyle(letterSpacing = 1.sp),
                    true,
                ),
                FlipCase(
                    "fontWeight: null vs Normal",
                    SpanStyle(),
                    SpanStyle(fontWeight = FontWeight.Normal),
                    true,
                ),
                FlipCase(
                    "fontWeight: Normal vs Bold",
                    SpanStyle(fontWeight = FontWeight.Normal),
                    SpanStyle(fontWeight = FontWeight.Bold),
                    true,
                ),
                FlipCase(
                    "fontFamily: null vs SansSerif",
                    SpanStyle(),
                    SpanStyle(fontFamily = FontFamily.SansSerif),
                    true,
                ),
                FlipCase(
                    "fontFamily: SansSerif vs Serif",
                    SpanStyle(fontFamily = FontFamily.SansSerif),
                    SpanStyle(fontFamily = FontFamily.Serif),
                    true,
                ),
                FlipCase(
                    "fontStyle: null vs Normal",
                    SpanStyle(),
                    SpanStyle(fontStyle = FontStyle.Normal),
                    true,
                ),
                FlipCase(
                    "fontStyle: Normal vs Italic",
                    SpanStyle(fontStyle = FontStyle.Normal),
                    SpanStyle(fontStyle = FontStyle.Italic),
                    true,
                ),
                FlipCase(
                    "fontSynthesis: null vs None",
                    SpanStyle(),
                    SpanStyle(fontSynthesis = FontSynthesis.None),
                    true,
                ),
                FlipCase(
                    "fontSynthesis: None vs Weight",
                    SpanStyle(fontSynthesis = FontSynthesis.None),
                    SpanStyle(fontSynthesis = FontSynthesis.Weight),
                    true,
                ),
                FlipCase(
                    "fontSynthesis: Weight vs Style",
                    SpanStyle(fontSynthesis = FontSynthesis.Weight),
                    SpanStyle(fontSynthesis = FontSynthesis.Style),
                    true,
                ),
                FlipCase(
                    "fontSynthesis: Style vs All",
                    SpanStyle(fontSynthesis = FontSynthesis.Style),
                    SpanStyle(fontSynthesis = FontSynthesis.All),
                    true,
                ),
                FlipCase(
                    "platformStyle: null vs Default",
                    SpanStyle(),
                    SpanStyle(platformStyle = PlatformSpanStyle.Default),
                    true,
                ),
                FlipCase(
                    "fontFeatureSettings: null vs empty",
                    SpanStyle(),
                    SpanStyle(fontFeatureSettings = ""),
                    true,
                ),
                FlipCase(
                    "fontFeatureSettings: empty vs custom",
                    SpanStyle(fontFeatureSettings = ""),
                    SpanStyle(fontFeatureSettings = "smcp"),
                    true,
                ),
                FlipCase(
                    "fontFeatureSettings: custom1 vs custom2",
                    SpanStyle(fontFeatureSettings = "smcp"),
                    SpanStyle(fontFeatureSettings = "liga"),
                    true,
                ),
                FlipCase(
                    "baselineShift: null vs None",
                    SpanStyle(),
                    SpanStyle(baselineShift = BaselineShift.None),
                    true,
                ),
                FlipCase(
                    "baselineShift: None vs -0.0f",
                    SpanStyle(baselineShift = BaselineShift.None),
                    SpanStyle(baselineShift = BaselineShift(-0.0f)),
                    true,
                ),
                FlipCase(
                    "baselineShift: None vs Superscript",
                    SpanStyle(baselineShift = BaselineShift.None),
                    SpanStyle(baselineShift = BaselineShift.Superscript),
                    true,
                ),
                FlipCase(
                    "baselineShift: Superscript vs Subscript",
                    SpanStyle(baselineShift = BaselineShift.Superscript),
                    SpanStyle(baselineShift = BaselineShift.Subscript),
                    true,
                ),
                FlipCase(
                    "textGeometricTransform: null vs None",
                    SpanStyle(),
                    SpanStyle(textGeometricTransform = TextGeometricTransform.None),
                    true,
                ),
                FlipCase(
                    "textGeometricTransform: None vs custom",
                    SpanStyle(textGeometricTransform = TextGeometricTransform.None),
                    SpanStyle(textGeometricTransform = TextGeometricTransform(1.5f, 0f)),
                    true,
                ),
                FlipCase(
                    "textGeometricTransform: custom1 vs custom2",
                    SpanStyle(textGeometricTransform = TextGeometricTransform(1.5f, 0f)),
                    SpanStyle(textGeometricTransform = TextGeometricTransform(1.5f, 0.2f)),
                    true,
                ),
                FlipCase(
                    "background: Unspecified vs Transparent",
                    SpanStyle(),
                    SpanStyle(background = Color.Transparent),
                    true,
                ),
                FlipCase(
                    "background: Transparent vs Red",
                    SpanStyle(background = Color.Transparent),
                    SpanStyle(background = Color.Red),
                    true,
                ),
                FlipCase(
                    "background: Red vs Blue",
                    SpanStyle(background = Color.Red),
                    SpanStyle(background = Color.Blue),
                    true,
                ),
                FlipCase(
                    "localeList: null vs en-US",
                    SpanStyle(),
                    SpanStyle(localeList = LocaleList("en-US")),
                    true,
                ),
                FlipCase(
                    "localeList: null (with shadow) vs en-US (with shadow)",
                    SpanStyle(shadow = Shadow(Color.Black, blurRadius = 2f)),
                    SpanStyle(
                        localeList = LocaleList("en-US"),
                        shadow = Shadow(Color.Black, blurRadius = 2f),
                    ),
                    true,
                ),
                FlipCase(
                    "localeList: en-US vs ja-JP",
                    SpanStyle(localeList = LocaleList("en-US")),
                    SpanStyle(localeList = LocaleList("ja-JP")),
                    true,
                ),
                FlipCase(
                    "color: Unspecified vs Transparent",
                    SpanStyle(),
                    SpanStyle(color = Color.Transparent),
                    false,
                ),
                FlipCase(
                    "color: Transparent vs Black",
                    SpanStyle(color = Color.Transparent),
                    SpanStyle(color = Color.Black),
                    false,
                ),
                FlipCase(
                    "color: Red vs Blue",
                    SpanStyle(color = Color.Red),
                    SpanStyle(color = Color.Blue),
                    false,
                ),
                FlipCase("brush: null vs shader1", SpanStyle(), SpanStyle(brush = shader1), false),
                FlipCase(
                    "brush: shader1 vs shader2",
                    SpanStyle(brush = shader1),
                    SpanStyle(brush = shader2),
                    false,
                ),
                FlipCase(
                    "brush alpha: NaN vs 0.5f",
                    SpanStyle(brush = shader1, alpha = Float.NaN),
                    SpanStyle(brush = shader1, alpha = 0.5f),
                    false,
                ),
                FlipCase(
                    "brush alpha: 0.3f vs 0.7f",
                    SpanStyle(brush = shader1, alpha = 0.3f),
                    SpanStyle(brush = shader1, alpha = 0.7f),
                    false,
                ),
                FlipCase(
                    "brush alpha: +0.0f vs -0.0f",
                    SpanStyle(brush = shader1, alpha = +0.0f),
                    SpanStyle(brush = shader1, alpha = -0.0f),
                    false,
                ),
                FlipCase(
                    "textDecoration: null vs None",
                    SpanStyle(),
                    SpanStyle(textDecoration = TextDecoration.None),
                    false,
                ),
                FlipCase(
                    "textDecoration: None vs Underline",
                    SpanStyle(textDecoration = TextDecoration.None),
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    false,
                ),
                FlipCase(
                    "textDecoration: Underline vs LineThrough",
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    false,
                ),
                FlipCase(
                    "textDecoration: LineThrough vs Combined",
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    SpanStyle(
                        textDecoration = TextDecoration.Underline + TextDecoration.LineThrough
                    ),
                    false,
                ),
                FlipCase(
                    "shadow: null vs None",
                    SpanStyle(),
                    SpanStyle(shadow = Shadow.None),
                    false,
                ),
                FlipCase(
                    "shadow: None vs custom",
                    SpanStyle(shadow = Shadow.None),
                    SpanStyle(shadow = Shadow(Color.Red, Offset(1f, 2f), 3f)),
                    false,
                ),
                FlipCase(
                    "shadow: custom1 vs custom2",
                    SpanStyle(shadow = Shadow(Color.Red, Offset(1f, 2f), 3f)),
                    SpanStyle(shadow = Shadow(Color.Blue, Offset(1f, 2f), 3f)),
                    false,
                ),
                FlipCase(
                    "drawStyle: null vs Fill",
                    SpanStyle(),
                    SpanStyle(drawStyle = Fill),
                    false,
                ),
                FlipCase(
                    "drawStyle: Fill vs Stroke",
                    SpanStyle(drawStyle = Fill),
                    SpanStyle(drawStyle = Stroke(width = 2f)),
                    false,
                ),
                FlipCase(
                    "drawStyle: Stroke(2f) vs Stroke(4f)",
                    SpanStyle(drawStyle = Stroke(width = 2f)),
                    SpanStyle(drawStyle = Stroke(width = 4f)),
                    false,
                ),
            )

        for (case in cases) {
            assertWithMessage(case.name).that(case.first == case.second).isFalse()
            assertWithMessage(case.name).that(case.second == case.first).isFalse()
            assertWithMessage(case.name)
                .that(case.first.hasSameLayoutAffectingAttributes(case.second))
                .isEqualTo(!case.isLayout)
            assertWithMessage(case.name)
                .that(case.second.hasSameLayoutAffectingAttributes(case.first))
                .isEqualTo(!case.isLayout)
            assertWithMessage(case.name)
                .that(case.first.hasSameNonLayoutAttributes(case.second))
                .isEqualTo(case.isLayout)
            assertWithMessage(case.name)
                .that(case.second.hasSameNonLayoutAttributes(case.first))
                .isEqualTo(case.isLayout)
            if (!case.isLayout) {
                assertWithMessage(case.name)
                    .that(case.first.hashCodeLayoutAffectingAttributes())
                    .isEqualTo(case.second.hashCodeLayoutAffectingAttributes())
            } else {
                assertWithMessage(case.name)
                    .that(case.first.hashCodeLayoutAffectingAttributes())
                    .isNotEqualTo(case.second.hashCodeLayoutAffectingAttributes())
            }
            assertWithMessage(case.name)
                .that(case.first.hashCode())
                .isNotEqualTo(case.second.hashCode())
        }
    }

    @Test
    fun hasMetricAffectingAttributes_sameAs_hashCode_andToString() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        for (metricStyle in
            listOf(
                SpanStyle(fontSize = 0.sp),
                SpanStyle(letterSpacing = 0.sp),
                SpanStyle(fontWeight = FontWeight.Normal),
                SpanStyle(fontFamily = FontFamily.Default),
                SpanStyle(fontStyle = FontStyle.Normal),
                SpanStyle(fontSynthesis = FontSynthesis.None),
                SpanStyle(fontFeatureSettings = ""),
                SpanStyle(textGeometricTransform = TextGeometricTransform.None),
                SpanStyle(localeList = LocaleList.Empty),
                SpanStyle(baselineShift = BaselineShift.Superscript),
            )) {
            assertThat(metricStyle).hasMetricAffectingAttributes()
        }
        for (nonMetricStyle in
            listOf(
                SpanStyle(),
                SpanStyle(color = Color.Red),
                SpanStyle(brush = shaderBrush, alpha = 0.5f),
                SpanStyle(baselineShift = BaselineShift.None),
                SpanStyle(baselineShift = BaselineShift(-0.0f)),
                SpanStyle(baselineShift = BaselineShift.Unspecified),
                SpanStyle(background = Color.Transparent),
                SpanStyle(background = Color.Red),
                SpanStyle(textDecoration = TextDecoration.Underline),
                SpanStyle(shadow = Shadow.None),
                SpanStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)),
                SpanStyle(drawStyle = Fill),
                SpanStyle(drawStyle = Stroke(width = 2f)),
                SpanStyle(platformStyle = PlatformSpanStyle.Default),
            )) {
            assertThat(nonMetricStyle).doesNotHaveMetricAffectingAttributes()
        }

        // `sameAs` uses reference equality (`===`) for `fontWeight`, so distinct `FontWeight(400)`
        // allocates a new `SpanStyle` that is structurally `==` to `FontWeight.Normal`.
        val distinctWeight400 = FontWeight(400)
        val styleWithNormal = SpanStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp)
        val mergedDistinctRef = styleWithNormal.merge(SpanStyle(fontWeight = distinctWeight400))
        assertThat(mergedDistinctRef).isEqualTo(styleWithNormal)
        assertThat(mergedDistinctRef).isNotSameInstanceAs(styleWithNormal)
        assertThat(mergedDistinctRef.fontWeight).isSameInstanceAs(distinctWeight400)

        // Preset-normalized styles have identical equals, hashCode, and
        // hashCodeLayoutAffectingAttributes.
        for ((first, second) in
            listOf(
                SpanStyle(color = Color.Red) to
                    SpanStyle(brush = SolidColor(Color.Red), alpha = 1f),
                SpanStyle(baselineShift = BaselineShift.None) to
                    SpanStyle(baselineShift = BaselineShift(0f)),
                SpanStyle(textGeometricTransform = TextGeometricTransform.None) to
                    SpanStyle(textGeometricTransform = TextGeometricTransform(1f, 0f)),
                SpanStyle(shadow = Shadow.None) to
                    SpanStyle(shadow = Shadow(Color.Black, Offset.Zero, 0f)),
            )) {
            assertThat(first).hasEqualValueAndHashCode(second)
            assertThat(first.hashCodeLayoutAffectingAttributes())
                .isEqualTo(second.hashCodeLayoutAffectingAttributes())
        }

        val style =
            SpanStyle(
                color = Color.Red,
                fontSize = 14.sp,
                baselineShift = BaselineShift.None,
                drawStyle = Fill,
            )
        val expectedRawLayoutHash =
            (((style.bits and 0xFFFF9E3E.toInt()) * 31 + mixLongHash(style.fontSize)) * 31 +
                mixLongHash(style.letterSpacing)) * 31 * 31 * 31
        assertThat(style.hashCodeLayoutAffectingAttributes())
            .isEqualTo(murmurFinalize32(expectedRawLayoutHash))
        assertThat(style.toString())
            .isEqualTo(
                "SpanStyle(" +
                    "color=${style.color}, " +
                    "brush=${style.brush}, " +
                    "alpha=${style.alpha}, " +
                    "fontSize=${style.fontSize}, " +
                    "fontWeight=${style.fontWeight}, " +
                    "fontStyle=${style.fontStyle}, " +
                    "fontSynthesis=${style.fontSynthesis}, " +
                    "fontFamily=${style.fontFamily}, " +
                    "fontFeatureSettings=${style.fontFeatureSettings}, " +
                    "letterSpacing=${style.letterSpacing}, " +
                    "baselineShift=${style.baselineShift}, " +
                    "textGeometricTransform=${style.textGeometricTransform}, " +
                    "localeList=${style.localeList}, " +
                    "background=${style.background}, " +
                    "textDecoration=${style.textDecoration}, " +
                    "shadow=${style.shadow}, " +
                    "platformStyle=${style.platformStyle}, " +
                    "drawStyle=${style.drawStyle})"
            )
    }

    @Test
    fun hashCodeAndLayoutHash_refinedMixers_preventFloatFlagAndBackgroundCollisions() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        // 1. 31 * fontSize + letterSpacing float step pairs:
        val baseFloatPair = SpanStyle(fontSize = 16.sp, letterSpacing = 1.25.sp)
        val steppedFloatPair31 =
            SpanStyle(
                fontSize = Float.fromBits(0x41810000).sp,
                letterSpacing = Float.fromBits(0x3F810000).sp,
            )
        val steppedFloatPair1 =
            SpanStyle(
                fontSize = Float.fromBits(0x41810000).sp,
                letterSpacing = Float.fromBits(0x3F9F0000).sp,
            )
        assertThat(steppedFloatPair31.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(baseFloatPair.hashCodeLayoutAffectingAttributes())
        assertThat(steppedFloatPair31.hashCode()).isNotEqualTo(baseFloatPair.hashCode())
        assertThat(steppedFloatPair1.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(baseFloatPair.hashCodeLayoutAffectingAttributes())
        assertThat(steppedFloatPair1.hashCode()).isNotEqualTo(baseFloatPair.hashCode())

        // 2. 31 * layoutBits + fontWeight linear cancellation pairs:
        val weight710 = SpanStyle(fontWeight = FontWeight(710))
        val bits10Weight400 =
            SpanStyle(
                baselineShift = BaselineShift.None,
                fontSynthesis = FontSynthesis.None,
                fontWeight = FontWeight(400),
            )
        assertThat(weight710.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(bits10Weight400.hashCodeLayoutAffectingAttributes())
        assertThat(weight710.hashCode()).isNotEqualTo(bits10Weight400.hashCode())

        val weight462 = SpanStyle(fontWeight = FontWeight(462))
        val bits2Weight400 =
            SpanStyle(baselineShift = BaselineShift.None, fontWeight = FontWeight(400))
        assertThat(weight462.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(bits2Weight400.hashCodeLayoutAffectingAttributes())
        assertThat(weight462.hashCode()).isNotEqualTo(bits2Weight400.hashCode())

        // 3. Background Color(0x00000010) vs Color.Transparent:
        val transparentBg = SpanStyle(background = Color.Transparent)
        val lowBitBg = SpanStyle(background = Color(0x00000010))
        assertThat(lowBitBg.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(transparentBg.hashCodeLayoutAffectingAttributes())
        assertThat(lowBitBg.hashCode()).isNotEqualTo(transparentBg.hashCode())

        val locale = LocaleList("en-US")
        val transparentBgWithExtras = SpanStyle(background = Color.Transparent, localeList = locale)
        val lowBitBgWithExtras = SpanStyle(background = Color(0x00000010), localeList = locale)
        assertThat(lowBitBgWithExtras.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(transparentBgWithExtras.hashCodeLayoutAffectingAttributes())
        assertThat(lowBitBgWithExtras.hashCode()).isNotEqualTo(transparentBgWithExtras.hashCode())

        // 4. TextUnit .sp vs .em low-bit swap:
        val spSize = SpanStyle(fontSize = Float.fromBits(0x41400000).sp)
        val emSize = SpanStyle(fontSize = Float.fromBits(0x41400003).em)
        assertThat(spSize.hashCodeLayoutAffectingAttributes())
            .isNotEqualTo(emSize.hashCodeLayoutAffectingAttributes())
        assertThat(spSize.hashCode()).isNotEqualTo(emSize.hashCode())
    }

    private fun SpanStyle.callFastMerge(
        color: Color = Color.Unspecified,
        brush: Brush? = null,
        alpha: Float = Float.NaN,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        platformStyle: PlatformSpanStyle? = null,
        drawStyle: DrawStyle? = null,
    ): SpanStyle =
        fastMerge(
            color = color,
            brush = brush,
            alpha = alpha,
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
            textDecoration = textDecoration,
            shadow = shadow,
            platformStyle = platformStyle,
            drawStyle = drawStyle,
        )

    private fun SpanStyle.fastMergeFrom(other: SpanStyle): SpanStyle =
        fastMerge(
            color = other.color,
            brush = other.brush,
            alpha = other.alpha,
            fontSize = other.fontSize,
            fontWeight = other.fontWeight,
            fontStyle = other.fontStyle,
            fontSynthesis = other.fontSynthesis,
            fontFamily = other.fontFamily,
            fontFeatureSettings = other.fontFeatureSettings,
            letterSpacing = other.letterSpacing,
            baselineShift = other.baselineShift,
            textGeometricTransform = other.textGeometricTransform,
            localeList = other.localeList,
            background = other.background,
            textDecoration = other.textDecoration,
            shadow = other.shadow,
            platformStyle = other.platformStyle,
            drawStyle = other.drawStyle,
        )

    @Test
    fun mergeAndFastMerge_colorXorBoundaries_andTransparentVsUnspecified() {
        // Color.Transparent has raw ULong bits 0UL (colorXor = 0x10L != 0L), so it must override
        // a specified color and must not be treated as Color.Unspecified (colorXor = 0L).
        val boundaryColors =
            listOf(
                Color.Transparent, // raw = 0UL -> colorXor = 0x10L
                Color.Black,
                Color.White,
                Color(0x0000_0000_0000_0011UL), // colorXor = 1L (only LSB set)
                Color(0x8000_0000_0000_0010UL), // colorXor = Long.MIN_VALUE (-other == other)
                Color(0x4000_0000_0000_0010UL), // colorXor = 1L shl 62 (positive, -other negative)
            )
        val baseSpecified = SpanStyle(color = Color.Red)
        val baseUnspecified = SpanStyle(color = Color.Unspecified)

        for (boundaryColor in boundaryColors) {
            val other = SpanStyle(color = boundaryColor)
            assertThat(baseSpecified.merge(other).color).isEqualTo(boundaryColor)
            assertThat(baseSpecified.callFastMerge(color = boundaryColor).color)
                .isEqualTo(boundaryColor)
            assertThat(baseUnspecified.merge(other).color).isEqualTo(boundaryColor)
            assertThat(baseUnspecified.callFastMerge(color = boundaryColor).color)
                .isEqualTo(boundaryColor)

            // Merging Color.Unspecified onto a boundary color preserves the boundary color.
            assertThat(other.merge(baseUnspecified).color).isEqualTo(boundaryColor)
            assertThat(other.callFastMerge(color = Color.Unspecified)).isSameInstanceAs(other)
        }
    }

    @Test
    fun mergeAndFastMerge_brushAndColorTransitions_matchTextForegroundStyleRules() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val brush1 = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val brush2 = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
        val colorStyle = SpanStyle(color = Color.Magenta)
        val brush1WithAlpha = SpanStyle(brush = brush1, alpha = 0.35f)
        val brush2NoAlpha = SpanStyle(brush = brush2, alpha = Float.NaN)
        val brush2WithAlpha = SpanStyle(brush = brush2, alpha = 0.8f)

        // 1. Distinct ShaderBrush with NaN alpha over ShaderBrush with 0.35f alpha:
        // replaces brush with brush2 and inherits 0.35f alpha.
        for (merged in
            listOf(
                brush1WithAlpha.merge(brush2NoAlpha),
                brush1WithAlpha.callFastMerge(brush = brush2, alpha = Float.NaN),
            )) {
            assertThat(merged).hasBrushForeground(brush2, 0.35f)
        }

        // 2. Distinct ShaderBrush with explicit alpha over ShaderBrush: uses new brush and alpha.
        for (merged in
            listOf(
                brush1WithAlpha.merge(brush2WithAlpha),
                brush1WithAlpha.callFastMerge(brush = brush2, alpha = 0.8f),
            )) {
            assertThat(merged).hasBrushForeground(brush2, 0.8f)
        }

        // 3. ShaderBrush (NaN alpha) over Color (or SolidColor with alpha): replaces Color with
        // ShaderBrush and keeps alpha = NaN (does not leak SolidColor's modulated alpha).
        val solidWithAlpha = SpanStyle(brush = SolidColor(Color.Red), alpha = 0.4f)
        for (base in listOf(colorStyle, solidWithAlpha)) {
            for (merged in
                listOf(
                    base.merge(brush2NoAlpha),
                    base.callFastMerge(brush = brush2, alpha = Float.NaN),
                )) {
                assertThat(merged).hasBrushForeground(brush2, Float.NaN)
            }
        }

        // 4. SolidColor with alpha over Color: modulates SolidColor and keeps extras == null.
        val expectedModulated = Color.Cyan.modulate(0.5f)
        for (merged in
            listOf(
                colorStyle.merge(SpanStyle(brush = SolidColor(Color.Cyan), alpha = 0.5f)),
                colorStyle.callFastMerge(brush = SolidColor(Color.Cyan), alpha = 0.5f),
            )) {
            assertThat(merged.color).isEqualTo(expectedModulated)
            assertThat(merged.brush).isNull()
            assertThat(merged.extras).isNull()
        }

        // 5. SolidColor over ShaderBrush: SolidColor is normalized to a Color, so ShaderBrush wins.
        for (merged in
            listOf(
                brush1WithAlpha.merge(SpanStyle(brush = SolidColor(Color.Cyan), alpha = 0.5f)),
                brush1WithAlpha.callFastMerge(brush = SolidColor(Color.Cyan), alpha = 0.5f),
            )) {
            assertThat(merged).hasBrushForeground(brush1, 0.35f)
        }
    }

    @Test
    fun mergeSpanBits_allMultiBitTransitions_andAdjacentFieldIsolation() {
        val fontStyles = listOf(null, FontStyle.Normal, FontStyle.Italic)
        val syntheses =
            listOf(
                null,
                FontSynthesis.None,
                FontSynthesis.Weight,
                FontSynthesis.Style,
                FontSynthesis.All,
            )
        val decorations =
            listOf(
                null,
                TextDecoration.None,
                TextDecoration.Underline,
                TextDecoration.LineThrough,
                TextDecoration.Underline + TextDecoration.LineThrough,
            )

        // Exhaustively test all transitions for each multi-bit field while adjacent 1-bit and
        // multi-bit neighbors are populated, verifying high-bit clearing (e.g. Italic -> Normal,
        // All -> None, Combined -> None) and zero SWAR carry bleed across bit boundaries.
        val neighbors =
            SpanStyle(
                baselineShift = BaselineShift.None, // bit 1
                platformStyle =
                    PlatformSpanStyle.Default, // bit 9 (adjacent to textDecoration 6..8)
                drawStyle = Fill, // bit 14 (adjacent to fontStyle 15..16)
            )

        for (baseFontStyle in fontStyles) {
            for (otherFontStyle in fontStyles) {
                val base =
                    neighbors.copy(fontStyle = baseFontStyle, fontSynthesis = FontSynthesis.All)
                val other = SpanStyle(fontStyle = otherFontStyle)
                val merged = base.merge(other)
                val fastMerged = base.callFastMerge(fontStyle = otherFontStyle)
                val expectedFontStyle = otherFontStyle ?: baseFontStyle
                assertThat(merged.fontStyle).isEqualTo(expectedFontStyle)
                assertThat(fastMerged.fontStyle).isEqualTo(expectedFontStyle)
                assertThat(merged.fontSynthesis).isEqualTo(FontSynthesis.All)
                assertThat(merged.drawStyle).isEqualTo(Fill)
                assertThat(merged.platformStyle).isEqualTo(PlatformSpanStyle.Default)
            }
        }

        for (baseSynthesis in syntheses) {
            for (otherSynthesis in syntheses) {
                for (baseDecoration in decorations) {
                    for (otherDecoration in decorations) {
                        val base =
                            neighbors.copy(
                                fontSynthesis = baseSynthesis,
                                textDecoration = baseDecoration,
                                fontStyle = FontStyle.Italic,
                            )
                        val other =
                            SpanStyle(
                                fontSynthesis = otherSynthesis,
                                textDecoration = otherDecoration,
                            )
                        val merged = base.merge(other)
                        val fastMerged =
                            base.callFastMerge(
                                fontSynthesis = otherSynthesis,
                                textDecoration = otherDecoration,
                            )
                        assertThat(merged.fontSynthesis).isEqualTo(otherSynthesis ?: baseSynthesis)
                        assertThat(merged.textDecoration)
                            .isEqualTo(otherDecoration ?: baseDecoration)
                        assertThat(merged.fontStyle).isEqualTo(FontStyle.Italic)
                        assertThat(merged.baselineShift).isEqualTo(BaselineShift.None)
                        assertThat(merged.platformStyle).isEqualTo(PlatformSpanStyle.Default)
                        assertThat(merged.drawStyle).isEqualTo(Fill)
                        assertThat(fastMerged).isEqualTo(merged)
                    }
                }
            }
        }
    }

    @Test
    fun mergeAndFastMerge_allPresetNormalizedFields_crossCancelWithCustomExtras() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val customShift = BaselineShift(0.5f)
        val negativeZeroShift = BaselineShift(-0.0f)
        val customTransform = TextGeometricTransform(scaleX = 1.5f, skewX = 0.25f)
        val customShadow = Shadow(Color.Blue, Offset(2f, 4f), 6f)
        val customStroke = Stroke(width = 3f, cap = StrokeCap.Round)
        val customLocale = LocaleList("en-US")

        val allCustom =
            SpanStyle(
                fontFeatureSettings = "smcp",
                baselineShift = customShift,
                textGeometricTransform = customTransform,
                localeList = customLocale,
                background = Color.Yellow,
                shadow = customShadow,
                drawStyle = customStroke,
            )
        val allPresets =
            SpanStyle(
                fontFeatureSettings = "",
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                background = Color.Transparent,
                shadow = Shadow.None,
                drawStyle = Fill,
            )
        assertThat(allCustom.extras).isNotNull()
        assertThat(allPresets.extras).isNull()

        // 1. All presets overriding all custom extras when localeList is absent: extras collapses
        // to null!
        val customNoLocale = allCustom.copy(localeList = null)
        val presetsOverCustom = customNoLocale.merge(allPresets)
        val fastPresetsOverCustom =
            customNoLocale.callFastMerge(
                fontFeatureSettings = "",
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                background = Color.Transparent,
                shadow = Shadow.None,
                drawStyle = Fill,
            )
        assertThat(presetsOverCustom).isEqualTo(allPresets)
        assertThat(presetsOverCustom.extras).isNull()
        assertThat(fastPresetsOverCustom).isEqualTo(allPresets)
        assertThat(fastPresetsOverCustom.extras).isNull()

        // 2. All custom extras overriding all presets:
        val customOverPresets = allPresets.merge(allCustom)
        val fastCustomOverPresets =
            allPresets.callFastMerge(
                fontFeatureSettings = "smcp",
                baselineShift = customShift,
                textGeometricTransform = customTransform,
                localeList = customLocale,
                background = Color.Yellow,
                shadow = customShadow,
                drawStyle = customStroke,
            )
        assertThat(customOverPresets).isEqualTo(allCustom)
        assertThat(fastCustomOverPresets).isEqualTo(allCustom)

        // 3. Partial collapse: overriding one custom extra with its preset while another extra
        // (localeList) remains in extras, and preserving preset bits when merging unrelated extras.
        val presetsOverCustomWithLocale = allCustom.merge(allPresets)
        assertThat(presetsOverCustomWithLocale.extras).isNotNull()
        assertThat(presetsOverCustomWithLocale.localeList).isEqualTo(customLocale)
        assertThat(presetsOverCustomWithLocale.fontFeatureSettings).isEqualTo("")
        assertThat(presetsOverCustomWithLocale.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(presetsOverCustomWithLocale.textGeometricTransform)
            .isEqualTo(TextGeometricTransform.None)
        assertThat(presetsOverCustomWithLocale.background).isEqualTo(Color.Transparent)
        assertThat(presetsOverCustomWithLocale.shadow).isEqualTo(Shadow.None)
        assertThat(presetsOverCustomWithLocale.drawStyle).isEqualTo(Fill)

        val unrelatedExtraOverPresets = allPresets.merge(SpanStyle(localeList = customLocale))
        assertThat(unrelatedExtraOverPresets).isEqualTo(presetsOverCustomWithLocale)

        // 4. Negative-zero BaselineShift(-0.0f) in extras vs BaselineShift.None (+0.0f) in bits:
        val negativeOverPositive =
            SpanStyle(baselineShift = BaselineShift.None)
                .merge(SpanStyle(baselineShift = negativeZeroShift))
        assertThat(negativeOverPositive.baselineShift?.multiplier?.toRawBits())
            .isEqualTo((-0f).toRawBits())
        assertThat(negativeOverPositive.extras).isNotNull()

        val positiveOverNegative =
            negativeOverPositive.merge(SpanStyle(baselineShift = BaselineShift.None))
        assertThat(positiveOverNegative.baselineShift?.multiplier?.toRawBits()).isEqualTo(0)
        assertThat(positiveOverNegative.extras).isNull()

        // 5. LocaleList.Empty vs non-empty LocaleList:
        val emptyOverLocale =
            SpanStyle(localeList = customLocale).merge(SpanStyle(localeList = LocaleList.Empty))
        assertThat(emptyOverLocale.localeList).isEqualTo(LocaleList.Empty)
        val localeOverEmpty = emptyOverLocale.merge(SpanStyle(localeList = customLocale))
        assertThat(localeOverEmpty.localeList).isEqualTo(customLocale)
    }

    @Test
    fun merge_reusesSpanStyleAndExtrasInstancesWhenUnchanged() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val defaultStyle = SpanStyle()
        val custom =
            SpanStyle(
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(Color.Black, Offset(1f, 1f), 2f),
            )
        // Early returns: null, self, other.isDefault(), this.isDefault()
        assertThat(custom.merge(null)).isSameInstanceAs(custom)
        assertThat(custom.merge(custom)).isSameInstanceAs(custom)
        assertThat(custom.merge(defaultStyle)).isSameInstanceAs(custom)
        assertThat(defaultStyle.merge(custom)).isSameInstanceAs(custom)

        // Post-merge sameAs(this): other sets a subset of fields to the exact same values
        assertThat(custom.merge(SpanStyle(color = Color.Red, fontSize = 16.sp)))
            .isSameInstanceAs(custom)

        // Post-merge other.sameAs(...): other supersedes all fields set in this
        val baseSubset = SpanStyle(color = Color.Blue, fontSize = 12.sp)
        assertThat(baseSubset.merge(custom)).isSameInstanceAs(custom)

        // thisExtras reuse: other changes only scalar/bit fields, so merged reuses this.extras
        val scalarOverride = custom.merge(SpanStyle(fontSize = 20.sp))
        assertThat(scalarOverride.extras).isSameInstanceAs(custom.extras)

        // otherExtras reuse: this has only scalar/bit fields, so merged reuses other.extras
        val otherExtrasOverride = SpanStyle(fontSize = 20.sp).merge(custom)
        assertThat(otherExtrasOverride.extras).isSameInstanceAs(custom.extras)
    }

    @Test
    fun fastMerge_all16Parameters_identityFastReturnAndDifferentialEquivalence() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val baseWithExtras =
            SpanStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.Weight,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "smcp",
                letterSpacing = 0.1.em,
                baselineShift = BaselineShift(0.2f),
                textGeometricTransform = TextGeometricTransform(1.1f, 0.1f),
                localeList = LocaleList("en-US"),
                background = Color.Yellow,
                textDecoration = TextDecoration.Underline,
                shadow = Shadow(Color.Black, Offset(1f, 2f), 3f),
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Stroke(width = 2f),
            )

        // 1. All-unspecified fastMerge returns `this` on the onlyColorMayChange fast path.
        assertThat(baseWithExtras.callFastMerge()).isSameInstanceAs(baseWithExtras)
        assertThat(baseWithExtras.callFastMerge(color = Color.Red)).isSameInstanceAs(baseWithExtras)

        // 2. Color-only change on a style with non-brush extras takes onlyColorMayChange fast path
        // and reuses `this.extras` by reference.
        val recolored = baseWithExtras.callFastMerge(color = Color.Green)
        assertThat(recolored.color).isEqualTo(Color.Green)
        assertThat(recolored.extras).isSameInstanceAs(baseWithExtras.extras)
        assertThat(recolored).isEqualTo(baseWithExtras.merge(SpanStyle(color = Color.Green)))

        // 3. Each of the 16 parameters individually:
        //    (a) passing the same value returns `baseWithExtras` (===),
        //    (b) passing a new value matches `baseWithExtras.merge(SpanStyle(...))`.
        val singleParamCases: List<Pair<SpanStyle, SpanStyle>> =
            listOf(
                // 1. color
                baseWithExtras.callFastMerge(color = Color.Blue) to
                    baseWithExtras.merge(SpanStyle(color = Color.Blue)),
                // 2. brush + alpha
                baseWithExtras.callFastMerge(brush = brush, alpha = 0.6f) to
                    baseWithExtras.merge(SpanStyle(brush = brush, alpha = 0.6f)),
                // 3. fontSize
                baseWithExtras.callFastMerge(fontSize = 22.sp) to
                    baseWithExtras.merge(SpanStyle(fontSize = 22.sp)),
                // 4. fontWeight
                baseWithExtras.callFastMerge(fontWeight = FontWeight.Bold) to
                    baseWithExtras.merge(SpanStyle(fontWeight = FontWeight.Bold)),
                // 5. fontStyle (Normal -> Italic and Italic -> Normal)
                baseWithExtras.callFastMerge(fontStyle = FontStyle.Italic) to
                    baseWithExtras.merge(SpanStyle(fontStyle = FontStyle.Italic)),
                // 6. fontSynthesis
                baseWithExtras.callFastMerge(fontSynthesis = FontSynthesis.None) to
                    baseWithExtras.merge(SpanStyle(fontSynthesis = FontSynthesis.None)),
                // 7. fontFamily
                baseWithExtras.callFastMerge(fontFamily = FontFamily.Serif) to
                    baseWithExtras.merge(SpanStyle(fontFamily = FontFamily.Serif)),
                // 8. fontFeatureSettings (custom -> preset "" and "" -> custom)
                baseWithExtras.callFastMerge(fontFeatureSettings = "") to
                    baseWithExtras.merge(SpanStyle(fontFeatureSettings = "")),
                // 9. letterSpacing
                baseWithExtras.callFastMerge(letterSpacing = 0.5.em) to
                    baseWithExtras.merge(SpanStyle(letterSpacing = 0.5.em)),
                // 10. baselineShift (custom -> preset None)
                baseWithExtras.callFastMerge(baselineShift = BaselineShift.None) to
                    baseWithExtras.merge(SpanStyle(baselineShift = BaselineShift.None)),
                // 11. textGeometricTransform (custom -> preset None)
                baseWithExtras.callFastMerge(
                    textGeometricTransform = TextGeometricTransform.None
                ) to
                    baseWithExtras.merge(
                        SpanStyle(textGeometricTransform = TextGeometricTransform.None)
                    ),
                // 12. localeList
                baseWithExtras.callFastMerge(localeList = LocaleList("ja-JP")) to
                    baseWithExtras.merge(SpanStyle(localeList = LocaleList("ja-JP"))),
                // 13. background (custom -> preset Transparent)
                baseWithExtras.callFastMerge(background = Color.Transparent) to
                    baseWithExtras.merge(SpanStyle(background = Color.Transparent)),
                // 14. textDecoration
                baseWithExtras.callFastMerge(textDecoration = TextDecoration.None) to
                    baseWithExtras.merge(SpanStyle(textDecoration = TextDecoration.None)),
                // 15. shadow (custom -> preset Shadow.None)
                baseWithExtras.callFastMerge(shadow = Shadow.None) to
                    baseWithExtras.merge(SpanStyle(shadow = Shadow.None)),
                // 16. drawStyle (custom -> preset Fill)
                baseWithExtras.callFastMerge(drawStyle = Fill) to
                    baseWithExtras.merge(SpanStyle(drawStyle = Fill)),
            )
        for ((fastResult, mergeResult) in singleParamCases) {
            assertThat(fastResult).hasEqualValueAndHashCode(mergeResult)
        }

        // 4. Full-path identity fast-return (`sameAs(this)`) when non-color args match `this`:
        assertThat(
                baseWithExtras.callFastMerge(
                    fontSize = baseWithExtras.fontSize,
                    fontWeight = baseWithExtras.fontWeight,
                    fontStyle = baseWithExtras.fontStyle,
                    fontSynthesis = baseWithExtras.fontSynthesis,
                    fontFamily = baseWithExtras.fontFamily,
                    fontFeatureSettings = baseWithExtras.fontFeatureSettings,
                    letterSpacing = baseWithExtras.letterSpacing,
                    baselineShift = baseWithExtras.baselineShift,
                    textGeometricTransform = baseWithExtras.textGeometricTransform,
                    localeList = baseWithExtras.localeList,
                    background = baseWithExtras.background,
                    textDecoration = baseWithExtras.textDecoration,
                    shadow = baseWithExtras.shadow,
                    platformStyle = baseWithExtras.platformStyle,
                    drawStyle = baseWithExtras.drawStyle,
                )
            )
            .isSameInstanceAs(baseWithExtras)
    }

    @Test
    fun resolveSpanStyleDefaults_andWithResolvedDefaults_coverAllBranchesAndIdempotence() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        // 1. Default style resolves to cached singleton and is idempotent on withResolvedDefaults.
        val resolvedDefault = resolveSpanStyleDefaults(SpanStyle())
        assertThat(resolvedDefault).isSameInstanceAs(BoxedStyleValues.ResolvedDefaultSpanStyle)
        assertThat(resolvedDefault.withResolvedDefaults()).isSameInstanceAs(resolvedDefault)
        assertThat(resolveSpanStyleDefaults(resolvedDefault)).isSameInstanceAs(resolvedDefault)
        assertThat(resolvedDefault.extras).isNull()

        // 2. BaselineShift.Unspecified paired with another cold extra (localeList) clears
        // extras.baselineShift while preserving extras for localeList.
        val unspecifiedShiftWithLocale =
            SpanStyle(
                baselineShift = BaselineShift.Unspecified,
                localeList = LocaleList("fr-FR"),
            )
        val resolvedWithLocale = resolveSpanStyleDefaults(unspecifiedShiftWithLocale)
        assertThat(resolvedWithLocale.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(resolvedWithLocale.localeList).isEqualTo(LocaleList("fr-FR"))
        assertThat(resolvedWithLocale.extras).isNotNull()
        assertThat(resolvedWithLocale.extras!!.baselineShift).isNull()
        // Calling withResolvedDefaults again on an already-resolved style with extras returns
        // `this`.
        assertThat(resolvedWithLocale.withResolvedDefaults()).isSameInstanceAs(resolvedWithLocale)

        // 3. colorXor == 0L with extras != null & brush == null resolves color to Color.Black,
        // whereas colorXor == 0L with brush != null keeps colorXor == 0L (Color.Unspecified).
        assertThat(resolvedWithLocale.color).isEqualTo(Color.Black)

        val shaderBrush = Brush.linearGradient(listOf(Color.Cyan, Color.Magenta))
        val resolvedBrush = resolveSpanStyleDefaults(SpanStyle(brush = shaderBrush, alpha = 0.4f))
        assertThat(resolvedBrush).hasBrushForeground(shaderBrush, 0.4f)
        assertThat(resolvedBrush.withResolvedDefaults()).isSameInstanceAs(resolvedBrush)

        // 4. All bit-field states in resolveSpanBits:
        for (fontStyle in listOf(null, FontStyle.Normal, FontStyle.Italic)) {
            val resolved = resolveSpanStyleDefaults(SpanStyle(fontStyle = fontStyle))
            assertThat(resolved.fontStyle).isEqualTo(fontStyle ?: FontStyle.Normal)
        }
        for (fontSynthesis in
            listOf(
                null,
                FontSynthesis.None,
                FontSynthesis.Weight,
                FontSynthesis.Style,
                FontSynthesis.All,
            )) {
            val resolved = resolveSpanStyleDefaults(SpanStyle(fontSynthesis = fontSynthesis))
            assertThat(resolved.fontSynthesis).isEqualTo(fontSynthesis ?: FontSynthesis.All)
        }
        for (textDecoration in
            listOf(
                null,
                TextDecoration.None,
                TextDecoration.Underline,
                TextDecoration.LineThrough,
                TextDecoration.Underline + TextDecoration.LineThrough,
            )) {
            val resolved = resolveSpanStyleDefaults(SpanStyle(textDecoration = textDecoration))
            assertThat(resolved.textDecoration).isEqualTo(textDecoration ?: TextDecoration.None)
        }
        for (platformStyle in listOf(null, PlatformSpanStyle.Default)) {
            val resolved = resolveSpanStyleDefaults(SpanStyle(platformStyle = platformStyle))
            assertThat(resolved.platformStyle).isEqualTo(platformStyle)
        }
    }

    @Test
    fun invokeResolveTypeface_allVariantsPassPreBoxedSingletonsAndReturnResult() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val expectedReturn = Typeface.DEFAULT
        var capturedFamily: Any? = null
        var capturedWeight: Any? = null
        var capturedStyle: Any? = null
        var capturedSynthesis: Any? = null
        val genericCallback =
            object : Function4<Any?, Any?, Any?, Any?, Typeface?> {
                override fun invoke(
                    fontFamily: Any?,
                    fontWeight: Any?,
                    fontStyle: Any?,
                    fontSynthesis: Any?,
                ): Typeface? {
                    capturedFamily = fontFamily
                    capturedWeight = fontWeight
                    capturedStyle = fontStyle
                    capturedSynthesis = fontSynthesis
                    return expectedReturn
                }
            }
        @Suppress("UNCHECKED_CAST")
        val typedCallback =
            genericCallback as (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface?

        val styleCases =
            listOf(
                null to BoxedStyleValues.NormalFontStyle,
                FontStyle.Normal to BoxedStyleValues.NormalFontStyle,
                FontStyle.Italic to BoxedStyleValues.ItalicFontStyle,
            )
        val synthesisCases =
            listOf(
                null to BoxedStyleValues.AllFontSynthesis,
                FontSynthesis.None to BoxedStyleValues.NoneFontSynthesis,
                FontSynthesis.Weight to BoxedStyleValues.WeightFontSynthesis,
                FontSynthesis.Style to BoxedStyleValues.StyleFontSynthesis,
                FontSynthesis.All to BoxedStyleValues.AllFontSynthesis,
            )

        for ((fontStyle, expectedBoxedStyle) in styleCases) {
            for ((fontSynthesis, expectedBoxedSynthesis) in synthesisCases) {
                val span =
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.W600,
                        fontStyle = fontStyle,
                        fontSynthesis = fontSynthesis,
                    )
                val result = span.invokeResolveTypeface(typedCallback)
                assertThat(result).isSameInstanceAs(expectedReturn)
                assertThat(capturedFamily).isSameInstanceAs(FontFamily.Serif)
                assertThat(capturedWeight).isSameInstanceAs(FontWeight.W600)
                assertThat(capturedStyle).isEqualTo(expectedBoxedStyle)
                assertThat(capturedSynthesis).isEqualTo(expectedBoxedSynthesis)
            }
        }
    }

    @Test
    fun lerp_brushToBrush_colorToBrush_inheritableTextUnits_platformStyle_andPresets() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val brush1 = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val brush2 = Brush.linearGradient(listOf(Color.Green, Color.Yellow))

        // 1. Brush to Brush: interpolates alpha continuously and switches brush discretely at 0.5f.
        val firstBrushStyle = SpanStyle(brush = brush1, alpha = 0.2f)
        val secondBrushStyle = SpanStyle(brush = brush2, alpha = 0.8f)
        val lerpBrushEarly = lerp(firstBrushStyle, secondBrushStyle, 0.25f)
        val lerpBrushLate = lerp(firstBrushStyle, secondBrushStyle, 0.75f)
        assertThat(lerpBrushEarly.brush).isSameInstanceAs(brush1)
        assertThat(lerpBrushEarly.alpha).isWithin(1e-5f).of(lerp(0.2f, 0.8f, 0.25f))
        assertThat(lerpBrushEarly.color).isEqualTo(Color.Unspecified)
        assertThat(lerpBrushLate.brush).isSameInstanceAs(brush2)
        assertThat(lerpBrushLate.alpha).isWithin(1e-5f).of(lerp(0.2f, 0.8f, 0.75f))
        assertThat(lerpBrushLate.color).isEqualTo(Color.Unspecified)

        // 2. Color to Brush (start = Color, stop = Brush): switches discretely at 0.5f.
        val colorSpan = SpanStyle(color = Color.Red)
        val colorToBrushEarly = lerp(colorSpan, secondBrushStyle, 0.4f)
        val colorToBrushLate = lerp(colorSpan, secondBrushStyle, 0.5f)
        assertThat(colorToBrushEarly.color).isEqualTo(Color.Red)
        assertThat(colorToBrushEarly.brush).isNull()
        assertThat(colorToBrushLate).hasBrushForeground(brush2, 0.8f)

        // 3. lerpTextUnitInheritable on fontSize and letterSpacing when one side is Unspecified:
        val withUnits = SpanStyle(fontSize = 12.sp, letterSpacing = 0.2.em)
        val withoutUnits = SpanStyle()
        assertThat(lerp(withUnits, withoutUnits, 0.4f).fontSize).isEqualTo(12.sp)
        assertThat(lerp(withUnits, withoutUnits, 0.4f).letterSpacing).isEqualTo(0.2.em)
        assertThat(lerp(withUnits, withoutUnits, 0.5f).fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(lerp(withUnits, withoutUnits, 0.5f).letterSpacing)
            .isEqualTo(TextUnit.Unspecified)
        assertThat(lerp(withoutUnits, withUnits, 0.4f).fontSize).isEqualTo(TextUnit.Unspecified)
        assertThat(lerp(withoutUnits, withUnits, 0.5f).fontSize).isEqualTo(12.sp)
        assertThat(
                lerp(withUnits, SpanStyle(fontSize = 20.sp, letterSpacing = 0.6.em), 0.5f)
                    .letterSpacing
            )
            .isEqualTo(0.4.em)

        // 4. lerpPlatformStyle when one or both sides have PlatformSpanStyle.Default:
        val withPlatform = SpanStyle(platformStyle = PlatformSpanStyle.Default)
        assertThat(lerp(withPlatform, withoutUnits, 0.25f).platformStyle)
            .isEqualTo(PlatformSpanStyle.Default)
        assertThat(lerp(withoutUnits, withPlatform, 0.75f).platformStyle)
            .isEqualTo(PlatformSpanStyle.Default)
        assertThat(lerp(withPlatform, withPlatform, 0.5f).platformStyle)
            .isEqualTo(PlatformSpanStyle.Default)

        // 5. Preset-normalized values in lerp:
        //    BaselineShift.None to custom, TextGeometricTransform.None to custom, Shadow.None to
        //    custom, and Fill to Stroke.
        val presetStyle =
            SpanStyle(
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                shadow = Shadow.None,
                drawStyle = Fill,
            )
        val customStyle =
            SpanStyle(
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift(0.8f),
                textGeometricTransform = TextGeometricTransform(scaleX = 2.0f, skewX = 0.4f),
                shadow = Shadow(Color.Black, Offset(4f, 8f), 10f),
                drawStyle = Stroke(width = 4f),
            )
        val midpointStyle = lerp(presetStyle, customStyle, 0.25f)
        assertThat(midpointStyle.fontWeight)
            .isEqualTo(lerp(FontWeight.Normal, FontWeight.Bold, 0.25f))
        assertThat(midpointStyle.baselineShift).isEqualTo(BaselineShift(0.2f))
        assertThat(midpointStyle.textGeometricTransform)
            .isEqualTo(TextGeometricTransform(scaleX = 1.25f, skewX = 0.1f))
        assertThat(midpointStyle.shadow)
            .isEqualTo(lerp(Shadow.None, Shadow(Color.Black, Offset(4f, 8f), 10f), 0.25f))
        assertThat(midpointStyle.drawStyle).isEqualTo(Fill)
        assertThat(lerp(presetStyle, customStyle, 0.75f).drawStyle).isEqualTo(Stroke(width = 4f))

        // Lerping preset-to-preset normalizes back to bit-only representation (extras == null).
        val presetToPreset = lerp(presetStyle, presetStyle, 0.5f)
        assertThat(presetToPreset.extras).isNull()
        assertThat(presetToPreset.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(presetToPreset.textGeometricTransform).isEqualTo(TextGeometricTransform.None)
        assertThat(presetToPreset.shadow).isEqualTo(Shadow.None)
        assertThat(presetToPreset.drawStyle).isEqualTo(Fill)
    }

    @Test
    fun platformStyle_defaultKeepsExtrasNull_andCustomPreservesInExtrasMergeLerpEqualsAndHashCode() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)

        // 1. Default and equal Android PlatformSpanStyle instances normalize to extras == null.
        val defaultStyle = SpanStyle(platformStyle = PlatformSpanStyle.Default)
        val freshAndroidDefault = SpanStyle(platformStyle = PlatformSpanStyle())
        assertThat(defaultStyle.extras).isNull()
        assertThat(freshAndroidDefault.extras).isNull()
        assertThat(defaultStyle.platformStyle).isSameInstanceAs(PlatformSpanStyle.Default)
        assertThat(freshAndroidDefault.platformStyle).isSameInstanceAs(PlatformSpanStyle.Default)
        assertThat(defaultStyle).hasEqualValueAndHashCode(freshAndroidDefault).hasValidInvariants()

        // 2. Custom (stateful) PlatformSpanStyle instances are preserved in SpanStyleExtras.
        val custom1 = mock<PlatformSpanStyle>()
        val custom2 = mock<PlatformSpanStyle>()
        val merged12 = mock<PlatformSpanStyle>()
        val merged1Default = mock<PlatformSpanStyle>()
        whenever(custom1.merge(same(custom1))).thenReturn(custom1)
        whenever(custom1.merge(same(custom2))).thenReturn(merged12)
        whenever(custom1.merge(same(PlatformSpanStyle.Default))).thenReturn(merged1Default)
        whenever(custom2.merge(same(custom1))).thenReturn(PlatformSpanStyle.Default)

        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val customColorStyle = SpanStyle(color = Color.Red, platformStyle = custom1)
        val customBrushStyle = SpanStyle(brush = shaderBrush, alpha = 0.5f, platformStyle = custom1)
        val customForegroundStyle =
            SpanStyle(
                textForegroundStyle = TextForegroundStyle.from(Color.Red),
                platformStyle = custom1,
            )
        val customCreated = createSpanStyle(platformStyle = custom1)
        assertThat(customColorStyle.extras).isNotNull()
        assertThat(customColorStyle.extras!!.platformStyle).isSameInstanceAs(custom1)
        assertThat(customColorStyle.platformStyle).isSameInstanceAs(custom1)
        assertThat(customBrushStyle.platformStyle).isSameInstanceAs(custom1)
        assertThat(customForegroundStyle.platformStyle).isSameInstanceAs(custom1)
        assertThat(customCreated.platformStyle).isSameInstanceAs(custom1)
        assertThat(customCreated).hasValidInvariants()
        assertThat(customCreated.copy(color = Color.Blue).platformStyle).isSameInstanceAs(custom1)

        // withResolvedDefaults preserves custom platformStyle when stripping Unspecified shift.
        val resolvedCustom =
            SpanStyle(baselineShift = BaselineShift.Unspecified, platformStyle = custom1)
                .withResolvedDefaults()
        assertThat(resolvedCustom.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(resolvedCustom.platformStyle).isSameInstanceAs(custom1)

        // 3. merge and fastMerge delegate to PlatformSpanStyle.merge and normalize Default to null.
        val style1 = SpanStyle(platformStyle = custom1)
        val style2 = SpanStyle(platformStyle = custom2)
        assertThat(SpanStyle().merge(style1).platformStyle).isSameInstanceAs(custom1)
        assertThat(style1.merge(SpanStyle()).platformStyle).isSameInstanceAs(custom1)
        assertThat(style1.merge(style2).platformStyle).isSameInstanceAs(merged12)
        assertThat(style1.merge(defaultStyle).platformStyle).isSameInstanceAs(merged1Default)
        val mergedBackToDefault = style2.merge(style1)
        assertThat(mergedBackToDefault.extras).isNull()
        assertThat(mergedBackToDefault.platformStyle).isSameInstanceAs(PlatformSpanStyle.Default)

        assertThat(
                SpanStyle()
                    .fastMerge(
                        color = Color.Unspecified,
                        brush = null,
                        alpha = Float.NaN,
                        fontSize = TextUnit.Unspecified,
                        fontWeight = null,
                        fontStyle = null,
                        fontSynthesis = null,
                        fontFamily = null,
                        fontFeatureSettings = null,
                        letterSpacing = TextUnit.Unspecified,
                        baselineShift = null,
                        textGeometricTransform = null,
                        localeList = null,
                        background = Color.Unspecified,
                        textDecoration = null,
                        shadow = null,
                        platformStyle = custom1,
                        drawStyle = null,
                    )
                    .platformStyle
            )
            .isSameInstanceAs(custom1)
        assertThat(
                style1
                    .fastMerge(
                        color = Color.Green,
                        brush = null,
                        alpha = Float.NaN,
                        fontSize = TextUnit.Unspecified,
                        fontWeight = null,
                        fontStyle = null,
                        fontSynthesis = null,
                        fontFamily = null,
                        fontFeatureSettings = null,
                        letterSpacing = TextUnit.Unspecified,
                        baselineShift = null,
                        textGeometricTransform = null,
                        localeList = null,
                        background = Color.Unspecified,
                        textDecoration = null,
                        shadow = null,
                        platformStyle = null,
                        drawStyle = null,
                    )
                    .platformStyle
            )
            .isSameInstanceAs(custom1)
        val fastMergedKeepExtras =
            style1.fastMerge(
                color = Color.Unspecified,
                brush = null,
                alpha = Float.NaN,
                fontSize = 18.sp,
                fontWeight = null,
                fontStyle = null,
                fontSynthesis = null,
                fontFamily = null,
                fontFeatureSettings = null,
                letterSpacing = TextUnit.Unspecified,
                baselineShift = null,
                textGeometricTransform = null,
                localeList = null,
                background = Color.Unspecified,
                textDecoration = null,
                shadow = null,
                platformStyle = null,
                drawStyle = null,
            )
        assertThat(fastMergedKeepExtras.extras).isSameInstanceAs(style1.extras)
        assertThat(
                style1
                    .fastMerge(
                        color = Color.Unspecified,
                        brush = null,
                        alpha = Float.NaN,
                        fontSize = TextUnit.Unspecified,
                        fontWeight = null,
                        fontStyle = null,
                        fontSynthesis = null,
                        fontFamily = null,
                        fontFeatureSettings = null,
                        letterSpacing = TextUnit.Unspecified,
                        baselineShift = null,
                        textGeometricTransform = null,
                        localeList = null,
                        background = Color.Unspecified,
                        textDecoration = null,
                        shadow = null,
                        platformStyle = custom2,
                        drawStyle = null,
                    )
                    .platformStyle
            )
            .isSameInstanceAs(merged12)
        assertThat(
                style1
                    .fastMerge(
                        color = Color.Unspecified,
                        brush = null,
                        alpha = Float.NaN,
                        fontSize = TextUnit.Unspecified,
                        fontWeight = null,
                        fontStyle = null,
                        fontSynthesis = null,
                        fontFamily = null,
                        fontFeatureSettings = null,
                        letterSpacing = TextUnit.Unspecified,
                        baselineShift = null,
                        textGeometricTransform = null,
                        localeList = null,
                        background = Color.Unspecified,
                        textDecoration = null,
                        shadow = null,
                        platformStyle = PlatformSpanStyle.Default,
                        drawStyle = null,
                    )
                    .platformStyle
            )
            .isSameInstanceAs(merged1Default)
        val fastMergedBackToDefault =
            style2.fastMerge(
                color = Color.Unspecified,
                brush = null,
                alpha = Float.NaN,
                fontSize = TextUnit.Unspecified,
                fontWeight = null,
                fontStyle = null,
                fontSynthesis = null,
                fontFamily = null,
                fontFeatureSettings = null,
                letterSpacing = TextUnit.Unspecified,
                baselineShift = null,
                textGeometricTransform = null,
                localeList = null,
                background = Color.Unspecified,
                textDecoration = null,
                shadow = null,
                platformStyle = custom1,
                drawStyle = null,
            )
        assertThat(fastMergedBackToDefault.extras).isNull()
        assertThat(fastMergedBackToDefault.platformStyle)
            .isSameInstanceAs(PlatformSpanStyle.Default)

        // 4. lerp preserves custom PlatformSpanStyle returned by lerpPlatformStyle.
        assertThat(lerp(style1, style2, 0.5f).platformStyle).isSameInstanceAs(custom1)

        // 5. equals, hashCode, and layout vs non-layout attribute comparisons.
        val style1Duplicate = SpanStyle(platformStyle = custom1)
        assertThat(style1).hasEqualValueAndHashCode(style1Duplicate)
        assertThat(style1.hashCodeLayoutAffectingAttributes())
            .isEqualTo(style1Duplicate.hashCodeLayoutAffectingAttributes())
        assertThat(style1.hasSameLayoutAffectingAttributes(style1Duplicate)).isTrue()
        assertThat(style1.hasSameNonLayoutAttributes(style1Duplicate)).isTrue()

        assertThat(style1).isNotEqualTo(style2)
        assertThat(style1.hasSameLayoutAffectingAttributes(style2)).isFalse()
        assertThat(style1.hasSameNonLayoutAttributes(style2)).isTrue()

        assertThat(style1).isNotEqualTo(defaultStyle)
        assertThat(defaultStyle).isNotEqualTo(style1)
        assertThat(style1.hasSameLayoutAffectingAttributes(defaultStyle)).isFalse()
        assertThat(defaultStyle.hasSameLayoutAffectingAttributes(style1)).isFalse()
        assertThat(style1.hasSameNonLayoutAttributes(defaultStyle)).isTrue()
        assertThat(defaultStyle.hasSameNonLayoutAttributes(style1)).isTrue()

        val customExtras = style1.extras!!
        assertThat(customExtras.hasSameLayoutAttributes(null)).isFalse()
        assertThat(customExtras.hasSameNonLayoutAttributes(null)).isTrue()
        assertThat(customExtras.hashCodeNonLayoutAttributes()).isEqualTo(0)
        assertThat(
                customExtras.matches(
                    null,
                    Float.NaN,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                    custom1,
                )
            )
            .isTrue()
        assertThat(
                customExtras.matches(
                    null,
                    Float.NaN,
                    null,
                    null,
                    null,
                    null,
                    Color.Unspecified,
                    null,
                    null,
                    custom2,
                )
            )
            .isFalse()
    }

    private fun nextRandomSpanStylePair(random: Random): Pair<SpanStyle, RefSpanStyle> {
        val useBrush = random.nextInt(4) == 0
        val brush = if (useBrush) SAMPLE_BRUSHES.random(random) else null
        val alpha = if (useBrush) SAMPLE_ALPHAS.random(random) else Float.NaN
        val color = if (useBrush) Color.Unspecified else SAMPLE_COLORS.random(random)
        val fontSize = SAMPLE_FONT_SIZES.random(random)
        val fontWeight = SAMPLE_FONT_WEIGHTS.random(random)
        val fontStyle = SAMPLE_FONT_STYLES.random(random)
        val fontSynthesis = SAMPLE_FONT_SYNTHESES.random(random)
        val fontFamily = SAMPLE_FONT_FAMILIES.random(random)
        val fontFeatureSettings = SAMPLE_FONT_FEATURES.random(random)
        val letterSpacing = SAMPLE_LETTER_SPACINGS.random(random)
        val baselineShift = SAMPLE_BASELINE_SHIFTS.random(random)
        val textGeometricTransform = SAMPLE_GEOMETRIC_TRANSFORMS.random(random)
        val localeList = SAMPLE_LOCALE_LISTS.random(random)
        val background = SAMPLE_BACKGROUNDS.random(random)
        val textDecoration = SAMPLE_TEXT_DECORATIONS.random(random)
        val shadow = SAMPLE_SHADOWS.random(random)
        val platformStyle = SAMPLE_PLATFORM_STYLES.random(random)
        val drawStyle = SAMPLE_DRAW_STYLES.random(random)

        val actual =
            if (useBrush) {
                SpanStyle(
                    brush = brush,
                    alpha = alpha,
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
                    textDecoration = textDecoration,
                    shadow = shadow,
                    platformStyle = platformStyle,
                    drawStyle = drawStyle,
                )
            } else {
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
                    textDecoration = textDecoration,
                    shadow = shadow,
                    platformStyle = platformStyle,
                    drawStyle = drawStyle,
                )
            }
        val reference =
            RefSpanStyle(
                textForegroundStyle =
                    if (useBrush) TextForegroundStyle.from(brush, alpha)
                    else TextForegroundStyle.from(color),
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
                textDecoration = textDecoration,
                shadow = shadow,
                platformStyle = platformStyle,
                drawStyle = drawStyle,
            )
        return actual to reference
    }

    companion object {
        private val SAMPLE_BRUSHES =
            listOf(
                null,
                SolidColor(Color.Red),
                Brush.linearGradient(listOf(Color.Blue, Color.Cyan)),
            )
        private val SAMPLE_COLORS =
            listOf(Color.Unspecified, Color.Black, Color.Transparent, Color.Red, Color(0x80112233))
        private val SAMPLE_ALPHAS = listOf(Float.NaN, 0f, 0.5f, 1f)
        private val SAMPLE_FONT_SIZES = listOf(TextUnit.Unspecified, 0.sp, 14.sp, 1.2.em)
        private val SAMPLE_FONT_WEIGHTS =
            listOf(null, FontWeight.Normal, FontWeight.Bold, FontWeight(350))
        private val SAMPLE_FONT_STYLES = listOf(null, FontStyle.Normal, FontStyle.Italic)
        private val SAMPLE_FONT_SYNTHESES =
            listOf(
                null,
                FontSynthesis.None,
                FontSynthesis.Weight,
                FontSynthesis.Style,
                FontSynthesis.All,
            )
        private val SAMPLE_FONT_FAMILIES =
            listOf(null, FontFamily.Default, FontFamily.SansSerif, FontFamily.Monospace)
        private val SAMPLE_FONT_FEATURES = listOf(null, "", "smcp")
        private val SAMPLE_LETTER_SPACINGS = listOf(TextUnit.Unspecified, 0.sp, 0.5.sp, 0.1.em)
        private val SAMPLE_BASELINE_SHIFTS =
            listOf(
                null,
                BaselineShift.None,
                BaselineShift(-0f),
                BaselineShift.Superscript,
                BaselineShift.Unspecified,
            )
        private val SAMPLE_GEOMETRIC_TRANSFORMS =
            listOf(null, TextGeometricTransform.None, TextGeometricTransform(1.2f, 0.3f))
        private val SAMPLE_LOCALE_LISTS = listOf(null, LocaleList("en"), LocaleList("ja"))
        private val SAMPLE_BACKGROUNDS = listOf(Color.Unspecified, Color.Transparent, Color.Yellow)
        private val SAMPLE_TEXT_DECORATIONS =
            listOf(
                null,
                TextDecoration.None,
                TextDecoration.Underline,
                TextDecoration.LineThrough,
                TextDecoration.Underline + TextDecoration.LineThrough,
            )
        private val SAMPLE_SHADOWS =
            listOf(null, Shadow.None, Shadow(Color.Black, Offset(1f, 2f), 3f))
        private val SAMPLE_PLATFORM_STYLES = listOf(null, PlatformSpanStyle.Default)
        private val SAMPLE_DRAW_STYLES =
            listOf(null, Fill, Stroke(width = 2f, cap = StrokeCap.Round))
    }

    private fun assertThat(actual: SpanStyle): SpanStyleSubject =
        assertAbout(SpanStyleSubject.SUBJECT_FACTORY).that(actual)!!
}

@Suppress("unused")
private fun assertThatSpanStyle(actual: SpanStyle): SpanStyleSubject =
    assertAbout(SpanStyleSubject.SUBJECT_FACTORY).that(actual)!!

/** Truth [Subject] for clean checks on [SpanStyle] and [RefSpanStyle]. */
private class SpanStyleSubject
private constructor(failureMetadata: FailureMetadata?, private val actual: SpanStyle) :
    Subject(failureMetadata, actual) {

    fun hasNoSemanticFlags(): SpanStyleSubject = hasSemanticFlags()

    fun hasSemanticFlags(
        hasBaselineShift: Boolean = false,
        hasTextDecoration: Boolean = false,
        hasTextGeometricTransform: Boolean = false,
        hasFontSynthesis: Boolean = false,
        hasFontStyle: Boolean = false,
    ): SpanStyleSubject = apply {
        check("hasBaselineShift").that(actual.hasBaselineShift).isEqualTo(hasBaselineShift)
        check("hasTextDecoration").that(actual.hasTextDecoration).isEqualTo(hasTextDecoration)
        check("hasTextGeometricTransform")
            .that(actual.hasTextGeometricTransform)
            .isEqualTo(hasTextGeometricTransform)
        check("hasFontSynthesis").that(actual.hasFontSynthesis).isEqualTo(hasFontSynthesis)
        check("hasFontStyle").that(actual.hasFontStyle).isEqualTo(hasFontStyle)
    }

    fun hasOnlyBaselineShift(expected: BaselineShift): SpanStyleSubject = apply {
        hasSemanticFlags(hasBaselineShift = true)
        check("baselineShift").that(actual.baselineShift).isEqualTo(expected)
    }

    fun hasOnlyTextDecoration(expected: TextDecoration): SpanStyleSubject = apply {
        hasSemanticFlags(hasTextDecoration = true)
        check("textDecoration").that(actual.textDecoration).isEqualTo(expected)
    }

    fun hasOnlyTextGeometricTransform(expected: TextGeometricTransform): SpanStyleSubject = apply {
        hasSemanticFlags(hasTextGeometricTransform = true)
        check("textGeometricTransform").that(actual.textGeometricTransform).isEqualTo(expected)
    }

    fun hasOnlyFontSynthesis(expected: FontSynthesis): SpanStyleSubject = apply {
        hasSemanticFlags(hasFontSynthesis = true)
        check("fontSynthesis").that(actual.fontSynthesis).isEqualTo(expected)
    }

    fun hasOnlyFontStyle(expected: FontStyle): SpanStyleSubject = apply {
        hasSemanticFlags(hasFontStyle = true)
        check("fontStyle").that(actual.fontStyle).isEqualTo(expected)
    }

    fun hasMetricAffectingAttributes(): SpanStyleSubject = apply {
        check("hasMetricAffectingAttributes").that(actual.hasMetricAffectingAttributes).isTrue()
    }

    fun doesNotHaveMetricAffectingAttributes(): SpanStyleSubject = apply {
        check("hasMetricAffectingAttributes").that(actual.hasMetricAffectingAttributes).isFalse()
    }

    fun hasBrushForeground(expectedBrush: Brush, expectedAlpha: Float): SpanStyleSubject = apply {
        check("brush").that(actual.brush).isSameInstanceAs(expectedBrush)
        check("alpha").that(actual.alpha.compareTo(expectedAlpha)).isEqualTo(0)
        check("color").that(actual.color).isEqualTo(Color.Unspecified)
        check("colorXor").that(actual.colorXor).isEqualTo(0L)
    }

    fun hasEqualValueAndHashCode(expected: SpanStyle): SpanStyleSubject = apply {
        check("equals").that(actual).isEqualTo(expected)
        check("hashCode").that(actual.hashCode()).isEqualTo(expected.hashCode())
    }

    fun hasValidInvariants(): SpanStyleSubject = apply {
        check("SPAN_HAS_EXTRAS matches extras != null")
            .that(actual.bits and 1 != 0)
            .isEqualTo(actual.extras != null)
        check("unused bits are zero").that(actual.bits and 0xFFFE_0004.toInt()).isEqualTo(0)
        check("copy()").that(actual.copy()).isEqualTo(actual)
        check("copy().hashCode()").that(actual.copy().hashCode()).isEqualTo(actual.hashCode())
        check("merge(null)").that(actual.merge(null)).isSameInstanceAs(actual)
        check("merge(actual)").that(actual.merge(actual)).isSameInstanceAs(actual)
        check("merge(SpanStyle())").that(actual.merge(SpanStyle())).isSameInstanceAs(actual)
        if (!actual.isDefault()) {
            check("SpanStyle().merge(actual)")
                .that(SpanStyle().merge(actual))
                .isSameInstanceAs(actual)
        }
    }

    fun matchesReference(expected: RefSpanStyle): SpanStyleSubject = apply {
        check("color").that(actual.color).isEqualTo(expected.textForegroundStyle.color)
        check("brush").that(actual.brush).isEqualTo(expected.textForegroundStyle.brush)
        check("alpha").that(actual.alpha.compareTo(expected.textForegroundStyle.alpha)).isEqualTo(0)
        check("fontSize").that(actual.fontSize).isEqualTo(expected.fontSize)
        check("fontWeight").that(actual.fontWeight).isEqualTo(expected.fontWeight)
        check("fontStyle").that(actual.fontStyle).isEqualTo(expected.fontStyle)
        check("fontSynthesis").that(actual.fontSynthesis).isEqualTo(expected.fontSynthesis)
        check("fontFamily").that(actual.fontFamily).isEqualTo(expected.fontFamily)
        check("fontFeatureSettings")
            .that(actual.fontFeatureSettings)
            .isEqualTo(expected.fontFeatureSettings)
        check("letterSpacing").that(actual.letterSpacing).isEqualTo(expected.letterSpacing)
        check("baselineShift").that(actual.baselineShift).isEqualTo(expected.baselineShift)
        check("textGeometricTransform")
            .that(actual.textGeometricTransform)
            .isEqualTo(expected.textGeometricTransform)
        check("localeList").that(actual.localeList).isEqualTo(expected.localeList)
        check("background").that(actual.background).isEqualTo(expected.background)
        check("textDecoration").that(actual.textDecoration).isEqualTo(expected.textDecoration)
        check("shadow").that(actual.shadow).isEqualTo(expected.shadow)
        check("platformStyle").that(actual.platformStyle).isEqualTo(expected.platformStyle)
        check("drawStyle").that(actual.drawStyle).isEqualTo(expected.drawStyle)
    }

    fun matchesReferenceEquivalenceWith(
        other: SpanStyle,
        thisRef: RefSpanStyle,
        otherRef: RefSpanStyle,
    ): SpanStyleSubject = apply {
        check("equals").that(actual == other).isEqualTo(thisRef == otherRef)
        if (actual == other) {
            check("hashCode").that(actual.hashCode()).isEqualTo(other.hashCode())
        }
        check("hasSameLayoutAffectingAttributes")
            .that(actual.hasSameLayoutAffectingAttributes(other))
            .isEqualTo(thisRef.hasSameLayoutAffectingAttributes(otherRef))
        if (actual.hasSameLayoutAffectingAttributes(other)) {
            check("hashCodeLayoutAffectingAttributes")
                .that(actual.hashCodeLayoutAffectingAttributes())
                .isEqualTo(other.hashCodeLayoutAffectingAttributes())
        }
        check("hasSameNonLayoutAttributes")
            .that(actual.hasSameNonLayoutAttributes(other))
            .isEqualTo(thisRef.hasSameNonLayoutAttributes(otherRef))
    }

    companion object {
        val SUBJECT_FACTORY: Factory<SpanStyleSubject, SpanStyle> =
            Factory { failureMetadata, actual ->
                SpanStyleSubject(failureMetadata, actual!!)
            }
    }
}

/**
 * Unpacked fake of [SpanStyle] used to test packed math.
 *
 * Production [SpanStyle] packs colors into `colorXor`, packs small fields into `bits`, and keeps
 * presets out of [SpanStyleExtras]. This class stores each field as a plain value and uses
 * [TextForegroundStyle]. Tests compare both classes to catch bit mask, sign, or sidecar bugs.
 */
@Suppress("DEPRECATION")
private class RefSpanStyle(
    val textForegroundStyle: TextForegroundStyle,
    val fontSize: TextUnit,
    val fontWeight: FontWeight?,
    val fontStyle: FontStyle?,
    val fontSynthesis: FontSynthesis?,
    val fontFamily: FontFamily?,
    val fontFeatureSettings: String?,
    val letterSpacing: TextUnit,
    val baselineShift: BaselineShift?,
    val textGeometricTransform: TextGeometricTransform?,
    val localeList: LocaleList?,
    val background: Color,
    val textDecoration: TextDecoration?,
    val shadow: Shadow?,
    val platformStyle: PlatformSpanStyle?,
    val drawStyle: DrawStyle?,
) {
    /**
     * Unpacked model for [SpanStyle.merge] and [SpanStyle.fastMerge].
     *
     * Checks that `mergeSpanColorXor`, `mergeSpanBits`, and [SpanStyleExtras] preset rules match
     * plain field merging and [TextForegroundStyle.merge].
     */
    fun merge(other: RefSpanStyle): RefSpanStyle =
        RefSpanStyle(
            textForegroundStyle = textForegroundStyle.merge(other.textForegroundStyle),
            fontFamily = other.fontFamily ?: fontFamily,
            fontSize = if (other.fontSize.isUnspecified) fontSize else other.fontSize,
            fontWeight = other.fontWeight ?: fontWeight,
            fontStyle = other.fontStyle ?: fontStyle,
            fontSynthesis = other.fontSynthesis ?: fontSynthesis,
            fontFeatureSettings = other.fontFeatureSettings ?: fontFeatureSettings,
            letterSpacing =
                if (other.letterSpacing.isUnspecified) letterSpacing else other.letterSpacing,
            baselineShift = other.baselineShift ?: baselineShift,
            textGeometricTransform = other.textGeometricTransform ?: textGeometricTransform,
            localeList = other.localeList ?: localeList,
            background =
                if (other.background != Color.Unspecified) other.background else background,
            textDecoration = other.textDecoration ?: textDecoration,
            shadow = other.shadow ?: shadow,
            platformStyle =
                if (platformStyle == null) other.platformStyle
                else if (other.platformStyle == null) platformStyle
                else platformStyle.merge(other.platformStyle),
            drawStyle = other.drawStyle ?: drawStyle,
        )

    /**
     * Unpacked model for [resolveSpanStyleDefaults] and [SpanStyle.withResolvedDefaults].
     *
     * Checks that [resolveSpanBits], `DEFAULT_COLOR_XOR`, and [BaselineShift.Unspecified] sidecar
     * rules match plain per-field defaults.
     */
    fun resolveDefaults(): RefSpanStyle =
        RefSpanStyle(
            textForegroundStyle =
                textForegroundStyle.takeOrElse { TextForegroundStyle.from(Color.Black) },
            fontSize = if (fontSize.isUnspecified) 14.sp else fontSize,
            fontWeight = fontWeight ?: FontWeight.Normal,
            fontStyle = fontStyle ?: FontStyle.Normal,
            fontSynthesis = fontSynthesis ?: FontSynthesis.All,
            fontFamily = fontFamily ?: FontFamily.Default,
            fontFeatureSettings = fontFeatureSettings ?: "",
            letterSpacing = if (letterSpacing.isUnspecified) 0.sp else letterSpacing,
            baselineShift =
                if (baselineShift == null || baselineShift == BaselineShift.Unspecified) {
                    BaselineShift.None
                } else {
                    baselineShift
                },
            textGeometricTransform = textGeometricTransform ?: TextGeometricTransform.None,
            localeList = localeList,
            background = if (background == Color.Unspecified) Color.Transparent else background,
            textDecoration = textDecoration ?: TextDecoration.None,
            shadow = shadow ?: Shadow.None,
            platformStyle = platformStyle,
            drawStyle = drawStyle ?: Fill,
        )

    /**
     * Unpacked model for [SpanStyle.equals].
     *
     * Checks that `colorXor`, `bits`, scalar fields, and [SpanStyleExtras] match all plain fields.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefSpanStyle) return false
        return hasSameLayoutAffectingAttributes(other) && hasSameNonLayoutAttributes(other)
    }

    /**
     * Unpacked model for [SpanStyle.hasSameLayoutAffectingAttributes].
     *
     * Checks that `LAYOUT_BITS` and [SpanStyleExtras.hasSameLayoutAttributes] compare every layout
     * field and ignore draw-only fields (`color`, `brush`, `alpha`, `textDecoration`, `shadow`,
     * `drawStyle`).
     */
    fun hasSameLayoutAffectingAttributes(other: RefSpanStyle): Boolean {
        if (this === other) return true
        return fontSize == other.fontSize &&
            fontWeight == other.fontWeight &&
            fontStyle == other.fontStyle &&
            fontSynthesis == other.fontSynthesis &&
            fontFamily == other.fontFamily &&
            fontFeatureSettings == other.fontFeatureSettings &&
            letterSpacing == other.letterSpacing &&
            baselineShift == other.baselineShift &&
            textGeometricTransform == other.textGeometricTransform &&
            localeList == other.localeList &&
            background == other.background &&
            platformStyle == other.platformStyle
    }

    /**
     * Unpacked model for [SpanStyle.hasSameNonLayoutAttributes].
     *
     * Checks that `colorXor`, `NON_LAYOUT_BITS`, and [SpanStyleExtras.hasSameNonLayoutAttributes]
     * check only draw fields and ignore layout fields.
     */
    fun hasSameNonLayoutAttributes(other: RefSpanStyle): Boolean {
        return textForegroundStyle == other.textForegroundStyle &&
            textDecoration == other.textDecoration &&
            shadow == other.shadow &&
            drawStyle == other.drawStyle
    }

    /**
     * Unpacked model for [SpanStyle.hashCode] in tests.
     *
     * Lets [RefSpanStyle] work in sets or maps and gives an unpacked hash baseline.
     */
    override fun hashCode(): Int {
        var result = textForegroundStyle.color.hashCode()
        result = 31 * result + (textForegroundStyle.brush?.hashCode() ?: 0)
        result = 31 * result + textForegroundStyle.alpha.hashCode()
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + (fontSynthesis?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (fontFeatureSettings?.hashCode() ?: 0)
        result = 31 * result + letterSpacing.hashCode()
        result = 31 * result + (baselineShift?.hashCode() ?: 0)
        result = 31 * result + (textGeometricTransform?.hashCode() ?: 0)
        result = 31 * result + (localeList?.hashCode() ?: 0)
        result = 31 * result + background.hashCode()
        result = 31 * result + (textDecoration?.hashCode() ?: 0)
        result = 31 * result + (shadow?.hashCode() ?: 0)
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (drawStyle?.hashCode() ?: 0)
        return result
    }

    /**
     * Unpacked model for [SpanStyle.hashCodeLayoutAffectingAttributes].
     *
     * Checks that styles that match in [hasSameLayoutAffectingAttributes] never hash draw-only
     * fields.
     */
    fun hashCodeLayoutAffectingAttributes(): Int {
        var result = fontSize.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + (fontSynthesis?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (fontFeatureSettings?.hashCode() ?: 0)
        result = 31 * result + letterSpacing.hashCode()
        result = 31 * result + (baselineShift?.hashCode() ?: 0)
        result = 31 * result + (textGeometricTransform?.hashCode() ?: 0)
        result = 31 * result + (localeList?.hashCode() ?: 0)
        result = 31 * result + background.hashCode()
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        return result
    }
}
