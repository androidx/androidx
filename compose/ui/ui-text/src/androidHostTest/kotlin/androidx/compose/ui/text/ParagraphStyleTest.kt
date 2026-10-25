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

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlin.test.assertFailsWith
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class ParagraphStyleTest {

    @Test(expected = IllegalStateException::class)
    fun `negative lineHeight throws IllegalStateException`() {
        ParagraphStyle(lineHeight = (-1).sp)
    }

    @Test(expected = IllegalStateException::class)
    fun `nan lineHeight throws IllegalStateException`() {
        ParagraphStyle(lineHeight = Float.NaN.sp)
    }

    @Test
    fun `merge textAlign uses other's textAlign`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(otherStyle.textAlign)
    }

    @Test
    fun `merge textAlign other unspecified, return original`() {
        val style = ParagraphStyle(textAlign = TextAlign.Justify)
        val otherStyle = ParagraphStyle(textAlign = TextAlign.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(style.textAlign)
    }

    @Test
    fun `merge textAlign both unspecified returns unspecified`() {
        val style = ParagraphStyle(textAlign = TextAlign.Unspecified)
        val otherStyle = ParagraphStyle(textAlign = TextAlign.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(TextAlign.Unspecified)
    }

    @Test
    fun `merge textDirection uses other's textDirection`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = TextDirection.Ltr)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(otherStyle.textDirection)
    }

    @Test
    fun `merge textDirection other null, returns original`() {
        val style = ParagraphStyle(textDirection = TextDirection.Rtl)
        val otherStyle = ParagraphStyle(textDirection = TextDirection.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(style.textDirection)
    }

    @Test
    fun `merge textDirection both unspecified returns unspecified`() {
        val style = ParagraphStyle(textDirection = TextDirection.Unspecified)
        val otherStyle = ParagraphStyle(textDirection = TextDirection.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(TextDirection.Unspecified)
    }

    @Test
    fun `merge non-null hyphens uses other's hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(otherStyle.hyphens)
    }

    @Test
    fun `merge hyphens other unspecified, returns original`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun `merge unspecified hyphens other non-null, returns other's hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(otherStyle.hyphens)
    }

    @Test
    fun `merge hyphens both unspecified returns unspecified`() {
        val style = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Unspecified)
    }

    @Test
    fun `merge lineHeight uses other's lineHeight`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = 20.sp)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(otherStyle.lineHeight)
    }

    @Test
    fun `merge lineHeight other unspecified, return original`() {
        val style = ParagraphStyle(lineHeight = 12.sp)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(style.lineHeight)
    }

    @Test
    fun `merge lineHeight both unspecified returns unspecified`() {
        val style = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val otherStyle = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `merge textIndent uses other's textIndent`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(otherStyle.textIndent)
    }

    @Test
    fun `merge textIndent other null, return original`() {
        val style = ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(style.textIndent)
    }

    @Test
    fun `merge textIndent both null returns null`() {
        val style = ParagraphStyle(textIndent = null)
        val otherStyle = ParagraphStyle(textIndent = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isNull()
    }

    @Test
    fun `merge unspecified with non-null lineBreak uses other's lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge non-null with unspecified lineBreak returns original's lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Unspecified)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `merge unspecified with unspecified lineBreak returns unspecified`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Unspecified)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `merge non-null with non-null lineBreak returns other's lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge null platformStyles`() {
        val style1 = ParagraphStyle(platformStyle = null)
        val style2 = ParagraphStyle(platformStyle = null)

        assertThat(style1.merge(style2).platformStyle).isNull()
    }

    @Test
    fun `plus operator merges`() {
        val style =
            ParagraphStyle(textAlign = TextAlign.Center, textDirection = TextDirection.Rtl) +
                ParagraphStyle(textAlign = TextAlign.Justify, lineHeight = 12.sp)

        assertThat(style)
            .isEqualTo(
                ParagraphStyle(
                    textAlign = TextAlign.Justify, // overridden by RHS
                    textDirection = TextDirection.Rtl, // from LHS,
                    lineHeight = 12.sp, // from RHS
                )
            )
    }

    @Test
    fun `lerp textAlign with a unspecified, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Unspecified)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(TextAlign.Unspecified)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(style1.textAlign)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textAlign = TextAlign.Left)
        val style2 = ParagraphStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textAlign).isEqualTo(style2.textAlign)
    }

    @Test
    fun `lerp textDirection with a unspecified, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Unspecified)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(TextDirection.Unspecified)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is smaller than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(style1.textDirection)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(textDirection = TextDirection.Ltr)
        val style2 = ParagraphStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textDirection).isEqualTo(style2.textDirection)
    }

    @Test
    fun `lerp hyphens with a unspecified, b not null and t is smaller than half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val style2 = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Unspecified)
    }

    @Test
    fun `lerp hyphens with a unspecified, b not null and t is equal to half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val style2 = ParagraphStyle(hyphens = Hyphens.Auto)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.5f)

        assertThat(newStyle.hyphens).isEqualTo(style2.hyphens)
    }

    @Test
    fun `lerp hyphens with a and b are not null and t is smaller than half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Auto)
        val style2 = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.hyphens).isEqualTo(style1.hyphens)
    }

    @Test
    fun `lerp hyphens with a and b are not Null and t is larger than half`() {
        val style1 = ParagraphStyle(hyphens = Hyphens.Auto)
        val style2 = ParagraphStyle(hyphens = Hyphens.None)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.hyphens).isEqualTo(style2.hyphens)
    }

    @Test
    fun `lerp textIndent with a null, b not null and t is smaller than half returns null`() {
        val style1 = ParagraphStyle(textIndent = null)
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(lerp(TextIndent(), style2.textIndent!!, fraction))
    }

    @Test
    fun `lerp textIndent with a and b are not Null`() {
        val style1 = ParagraphStyle(textIndent = TextIndent(firstLine = 10.sp))
        val style2 = ParagraphStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.6f
        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent)
            .isEqualTo(lerp(style1.textIndent!!, style2.textIndent!!, fraction))
    }

    @Test
    fun `lerp lineHeight with a and b are specified`() {
        val style1 = ParagraphStyle(lineHeight = 10.sp)
        val style2 = ParagraphStyle(lineHeight = 20.sp)
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.lineHeight)
            .isEqualTo(lerp(style1.lineHeight, style2.lineHeight, fraction))
    }

    @Test
    fun `lerp lineHeight with a and b are unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `lerp lineHeight with either a or b is unspecified`() {
        val style1 = ParagraphStyle(lineHeight = TextUnit.Unspecified)
        val style2 = ParagraphStyle(lineHeight = 22.sp)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)
        val anotherNewStyle = lerp(start = style1, stop = style2, fraction = 0.8f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
        assertThat(anotherNewStyle.lineHeight).isEqualTo(22.sp)
    }

    @Test
    fun `lerp with null platformStyles has null platformStyle`() {
        val style = ParagraphStyle(platformStyle = null)
        val otherStyle = ParagraphStyle(platformStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.platformStyle).isNull()
    }

    @Test
    fun `lerp with null lineHeightStyles has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with non-null start, null end, closer to start has non-null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(style.lineHeightStyle)
    }

    @Test
    fun `lerp with non-null start, null end, closer to end has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to start has null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to end has non-null lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(otherStyle.lineHeightStyle)
    }

    @Test
    fun `lerp with non-null start, unspecified end, closer to start has non-null lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Heading)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Unspecified)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `lerp with non-null start, unspecified end, closer to end has unspecified lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Heading)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Unspecified)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `lerp with unspecified start, non-null end, closer to start has unspecified lineBreak`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `lerp with unspecified start, non-null end, closer to end has non-null lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `equals return false for different line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return true for same line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `hashCode is same for same line height behavior`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different line height behavior`() {
        val style =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.None)
            )
        val otherStyle =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.Both)
            )

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `copy with lineHeightStyle returns new lineHeightStyle`() {
        val style =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.None)
            )
        val newLineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.Both)
        val newStyle = style.copy(lineHeightStyle = newLineHeightStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(newLineHeightStyle)
    }

    @Test
    fun `copy without lineHeightStyle uses existing lineHeightStyle`() {
        val style =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.Both)
            )
        val newStyle = style.copy()

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `copy with hyphens returns new hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val newStyle = style.copy(hyphens = Hyphens.Auto)

        assertThat(newStyle.hyphens).isEqualTo(Hyphens.Auto)
    }

    @Test
    fun `copy without hyphens uses existing hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val newStyle = style.copy()

        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun `equals return false for different and non-null hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return false for unspecified and non-null hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return true for same and non-null hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `equals return true for both unspecified hyphens`() {
        val style = ParagraphStyle(hyphens = Hyphens.Unspecified)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Unspecified)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `hashCode is same for same hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.Auto)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different hyphens behavior`() {
        val style = ParagraphStyle(hyphens = Hyphens.None)
        val otherStyle = ParagraphStyle(hyphens = Hyphens.Auto)

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `merge with null lineHeightStyle uses other's lineHeightStyle`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `merge with non-null lineHeightStyle, returns original`() {
        val style = ParagraphStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `merge with both null lineHeightStyle returns null`() {
        val style = ParagraphStyle(lineHeightStyle = null)
        val otherStyle = ParagraphStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `merge with both non-null lineHeightStyle returns other's lineHeightStyle`() {
        val style =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Center, trim = Trim.None)
            )
        val otherStyle =
            ParagraphStyle(
                lineHeightStyle = LineHeightStyle(alignment = Alignment.Bottom, trim = Trim.Both)
            )

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `constructor without lineHeightStyle sets lineHeightStyle to null`() {
        val style = ParagraphStyle(textAlign = TextAlign.Start)

        assertThat(style.lineHeightStyle).isNull()
    }

    @Test
    fun `copy with lineBreak returns new lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val newStyle = style.copy(lineBreak = LineBreak.Heading)

        assertThat(newStyle.lineBreak).isEqualTo(LineBreak.Heading)
    }

    @Test
    fun `copy without lineBreak uses existing lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val newStyle = style.copy()

        assertThat(newStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `hashCode is same for same lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Paragraph)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different lineBreak`() {
        val style = ParagraphStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = ParagraphStyle(lineBreak = LineBreak.Heading)

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun lineHeightStyle_keepsCallerRef_andMergesBothWays() {
        val customStyle = LineHeightStyle(Alignment(0.25f), Trim.Both)
        val preset =
            ParagraphStyle(lineHeightStyle = LineHeightStyle(Alignment.Proportional, Trim.Both))
        val custom = ParagraphStyle(lineHeightStyle = customStyle)
        assertThat(custom.lineHeightStyle).isSameInstanceAs(customStyle)
        assertThat(preset.merge(custom)).isEqualTo(custom)
        assertThat(custom.merge(preset)).isEqualTo(preset)
        assertThat(custom.merge(preset).hashCode()).isEqualTo(preset.hashCode())
    }

    @Test
    fun presetInOther_replacesCustomInThis() {
        val custom =
            ParagraphStyle(
                textIndent = TextIndent(4.sp, 8.sp),
                lineHeight = 17.5.sp,
                textAlign = TextAlign.End,
                textDirection = TextDirection.Content,
            )
        val presets = ParagraphStyle(textIndent = TextIndent.None, lineHeight = 0.5.em)
        val merged = custom.merge(presets)
        assertThatParagraphStyle(merged)
            .hasFields(
                textIndent = TextIndent.None,
                lineHeight = 0.5.em,
                textAlign = TextAlign.End,
                textDirection = TextDirection.Content,
            )
        assertThat(presets.merge(custom)).isEqualTo(custom)
    }

    @Test
    fun negativeZeroLineHeight_preservesRawBits() {
        val negativeZero = ParagraphStyle(lineHeight = (-0f).sp)
        val positiveZero = ParagraphStyle(lineHeight = 0f.sp)
        assertThat(negativeZero.lineHeight.value.toRawBits()).isEqualTo((-0f).toRawBits())
        assertThat(negativeZero.lineHeight).isEqualTo((-0f).sp)
        assertThat(negativeZero).isNotEqualTo(positiveZero)
    }

    @Test
    fun platformParagraphStyle_defaultPacksWithoutExtras() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val defaultPlatform = ParagraphStyle(platformStyle = PlatformParagraphStyle.Default)
        assertThat(defaultPlatform.platformStyle).isEqualTo(PlatformParagraphStyle.Default)
        assertThat(defaultPlatform.extras).isNull()

        val nonDefault =
            PlatformParagraphStyle(
                includeFontPadding = true,
                emojiSupportMatch = EmojiSupportMatch.All,
            )
        val withExtras = ParagraphStyle(platformStyle = nonDefault)
        assertThat(withExtras.platformStyle).isEqualTo(nonDefault)
        assertThat(withExtras.extras).isNotNull()
    }

    @Test
    fun isDefault_andMerge_withExtrasOnlyStyles() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val defaultStyle = ParagraphStyle()
        assertThat(defaultStyle.isDefault()).isTrue()

        // Styles where only an `extras`-backed field (or preset for an extras-backed field) is set
        // must set `PARA_HAS_EXTRAS` / presence bits in `bits`, so `isDefault()` is false and
        // `merge` preserves them in both directions.
        val extrasOnlyStyles =
            listOf(
                ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp, restLine = 4.sp)),
                ParagraphStyle(textIndent = TextIndent.None),
                ParagraphStyle(
                    platformStyle =
                        PlatformParagraphStyle(
                            includeFontPadding = true,
                            emojiSupportMatch = EmojiSupportMatch.All,
                        )
                ),
                ParagraphStyle(platformStyle = PlatformParagraphStyle.Default),
                ParagraphStyle(textMotion = TextMotion(TextMotion.Linearity.None, true)),
                ParagraphStyle(textMotion = TextMotion.Static),
                ParagraphStyle(textMotion = TextMotion.Animated),
                // Also verify direct internal construction where only `extras` is non-null and
                // caller passes `0L` for `packedBits`: the primary constructor forces bit 0
                // (`PARA_HAS_EXTRAS`) to 1 so `isDefault()` is still false.
                ParagraphStyle(
                    bits = 0L,
                    lineHeightStyle = null,
                    extras =
                        ParagraphStyleExtras(
                            textIndent = TextIndent(firstLine = 6.sp, restLine = 2.sp),
                            platformStyle = null,
                            textMotion = null,
                        ),
                ),
            )

        for (style in extrasOnlyStyles) {
            assertThat(style.isDefault()).isFalse()
            assertThat(defaultStyle.merge(style)).isSameInstanceAs(style)
            assertThat(style.merge(defaultStyle)).isSameInstanceAs(style)
        }
    }

    @Test
    fun saverRoundTrip_preservesPresetAndCustomValues() {
        val saverScope = SaverScope { true }
        val styles =
            listOf(
                ParagraphStyle(),
                ParagraphStyle(textIndent = TextIndent.None),
                ParagraphStyle(textMotion = TextMotion.Static),
                ParagraphStyle(textMotion = TextMotion.Animated),
                ParagraphStyle(lineHeightStyle = LineHeightStyle.Default),
                ParagraphStyle(platformStyle = PlatformParagraphStyle.Default),
                fullyPopulatedParagraphStyle(),
            )
        for (style in styles) {
            val saved = with(ParagraphStyleSaver) { saverScope.save(style) }!!
            val restored = ParagraphStyleSaver.restore(saved)!!
            assertThat(restored).isEqualTo(style)
            assertThat(restored.hashCode()).isEqualTo(style.hashCode())
        }
    }

    @Test
    fun seededDifferential_equalsHashCodeAndMerge_matchReferenceParagraphStyle() {
        val random = Random(0x5EED_9A7AL)
        val samples =
            List(160) {
                val style = randomParagraphStyle(random)
                style to style.toReference()
            }

        for (i in samples.indices) {
            val (styleA, referenceA) = samples[i]
            val resolvedA = resolveParagraphStyleDefaults(styleA, LayoutDirection.Ltr)
            assertThatParagraphStyle(resolvedA)
                .matchesReference(referenceA.resolveDefaults(LayoutDirection.Ltr))

            for ((styleB, referenceB) in samples) {
                assertThat(styleA == styleB).isEqualTo(referenceA == referenceB)
                if (styleA == styleB) {
                    assertThat(styleA.hashCode()).isEqualTo(styleB.hashCode())
                }
            }

            val (styleB, referenceB) = samples[(i * 37 + 11) % samples.size]
            val merged = styleA.merge(styleB)
            val fastMerged =
                styleA.fastMergeOnly(
                    textAlign = styleB.textAlign,
                    textDirection = styleB.textDirection,
                    lineHeight = styleB.lineHeight,
                    textIndent = styleB.textIndent,
                    platformStyle = styleB.platformStyle,
                    lineHeightStyle = styleB.lineHeightStyle,
                    lineBreak = styleB.lineBreak,
                    hyphens = styleB.hyphens,
                    textMotion = styleB.textMotion,
                )
            assertThat(merged).isEqualTo(fastMerged)
            assertThat(merged.hashCode()).isEqualTo(fastMerged.hashCode())
            assertThatParagraphStyle(merged).matchesReference(referenceA.merge(referenceB))
        }
    }

    @Test
    fun hashCode_avoidsLineHeightCollision_andDistributesLow16Bits() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val textAligns =
            listOf(TextAlign.Unspecified, TextAlign.Start, TextAlign.Center, TextAlign.End)
        val textDirections = listOf(TextDirection.Unspecified, TextDirection.Ltr, TextDirection.Rtl)
        val lineBreaks = listOf(LineBreak.Unspecified, LineBreak.Simple, LineBreak.Heading)
        val hyphensOptions = listOf(Hyphens.Unspecified, Hyphens.None, Hyphens.Auto)
        val lineHeights = listOf(TextUnit.Unspecified, 0.sp, 14.sp, 20.sp, 1.5.em, 2.em)

        val styles = buildList {
            for (textAlign in textAligns) {
                for (textDirection in textDirections) {
                    for (lineBreak in lineBreaks) {
                        for (hyphens in hyphensOptions) {
                            val zeroSp =
                                ParagraphStyle(
                                    textAlign = textAlign,
                                    textDirection = textDirection,
                                    lineHeight = 0.sp,
                                    lineBreak = lineBreak,
                                    hyphens = hyphens,
                                )
                            val twoEm = zeroSp.copy(lineHeight = 2.em)
                            assertThat(zeroSp.hashCode()).isNotEqualTo(twoEm.hashCode())
                            for (lineHeight in lineHeights) {
                                add(zeroSp.copy(lineHeight = lineHeight))
                            }
                        }
                    }
                }
            }
        }
        val hashes = styles.map { it.hashCode() }.toSet()
        assertThat(hashes).hasSize(styles.size)
        val low16Buckets = hashes.map { it and 0xFFFF }.toSet()
        assertThat(low16Buckets.size).isAtLeast(styles.size * 9 / 10)
        assertThat(murmurFinalize32(0)).isEqualTo(0)
        assertThat(ParagraphStyle().hashCode()).isEqualTo(0)
        assertThat(ParagraphStyle(lineHeight = TextUnit.Unspecified).hashCode())
            .isNotEqualTo(ParagraphStyle(lineHeight = 2.sp).hashCode())
        val loose12 =
            ParagraphStyle(
                lineHeight = 12.sp,
                lineBreak =
                    LineBreak(
                        LineBreak.Strategy.Simple,
                        LineBreak.Strictness.Loose,
                        LineBreak.WordBreak.Default,
                    ),
            )
        val normal13 =
            ParagraphStyle(
                lineHeight = 13.sp,
                lineBreak =
                    LineBreak(
                        LineBreak.Strategy.Simple,
                        LineBreak.Strictness.Normal,
                        LineBreak.WordBreak.Default,
                    ),
            )
        assertThat(loose12.hashCode()).isNotEqualTo(normal13.hashCode())
    }

    @Test
    fun primaryConstructor_andCreateParagraphStyle_enforceBitInvariantsAndSingletons() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        // Primary constructor clears bit 0 when `extras == null` and forces bit 0 when `extras !=
        // null`.
        val clearedFromOne = ParagraphStyle(bits = 1L, lineHeightStyle = null, extras = null)
        assertThat(clearedFromOne.bits).isEqualTo(0L)
        assertParagraphStyleInvariants(clearedFromOne)

        val textAlignBits =
            packParagraphStyleBits(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Unspecified,
                lineHeight = TextUnit.Unspecified,
                textIndent = null,
                platformStyle = null,
                lineBreak = LineBreak.Unspecified,
                hyphens = Hyphens.Unspecified,
                textMotion = null,
            )
        val clearedWithAlign =
            ParagraphStyle(bits = textAlignBits or 1L, lineHeightStyle = null, extras = null)
        assertThat(clearedWithAlign.textAlign).isEqualTo(TextAlign.Center)
        assertParagraphStyleInvariants(clearedWithAlign)

        val extras = ParagraphStyleExtras(TextIndent(8.sp, 4.sp), null, null)
        val forcedFromZero = ParagraphStyle(bits = 0L, lineHeightStyle = null, extras = extras)
        assertThat(forcedFromZero.bits).isEqualTo(1L)
        assertThat(forcedFromZero.isDefault()).isFalse()
        assertThat(forcedFromZero.extras).isSameInstanceAs(extras)

        val textIndentBitsWithoutBit0 =
            packParagraphStyleBits(
                textAlign = TextAlign.Unspecified,
                textDirection = TextDirection.Unspecified,
                lineHeight = TextUnit.Unspecified,
                textIndent = extras.textIndent,
                platformStyle = null,
                lineBreak = LineBreak.Unspecified,
                hyphens = Hyphens.Unspecified,
                textMotion = null,
            ) and 1L.inv()
        val forcedWithValidBits =
            ParagraphStyle(
                bits = textIndentBitsWithoutBit0,
                lineHeightStyle = null,
                extras = extras,
            )
        assertThat(forcedWithValidBits.extras).isSameInstanceAs(extras)
        assertParagraphStyleInvariants(forcedWithValidBits)

        // `createParagraphStyle` branches: Default singleton, lineHeightStyle-only, packed-only,
        // preset-normalized extras, and custom extras.
        assertThat(createParagraphStyle()).isSameInstanceAs(DefaultParagraphStyle)
        assertParagraphStyleInvariants(DefaultParagraphStyle)

        val variants =
            listOf(
                createParagraphStyle(lineHeightStyle = LineHeightStyle.Default),
                createParagraphStyle(textAlign = TextAlign.End),
                createParagraphStyle(
                    textIndent = TextIndent.None,
                    platformStyle = PlatformParagraphStyle.Default,
                    textMotion = TextMotion.Animated,
                ),
                createParagraphStyle(textIndent = TextIndent(10.sp, 5.sp)),
            )
        assertThat(variants[0].bits).isEqualTo(0L)
        assertThat(variants[0].lineHeightStyle).isEqualTo(LineHeightStyle.Default)
        assertThat(variants[2].extras).isNull()
        assertThat(variants[3].extras).isNotNull()
        variants.forEach(::assertParagraphStyleInvariants)
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedBoxingGetters_returnUnpackedProperties() {
        for ((style, expected) in
            listOf(
                ParagraphStyle() to
                    listOf(
                        TextAlign.Unspecified,
                        TextDirection.Unspecified,
                        Hyphens.Unspecified,
                        LineBreak.Unspecified,
                    ),
                ParagraphStyle(
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.ContentOrRtl,
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Heading,
                ) to
                    listOf(
                        TextAlign.Justify,
                        TextDirection.ContentOrRtl,
                        Hyphens.Auto,
                        LineBreak.Heading,
                    ),
            )) {
            assertThat(
                    listOf(
                        style.deprecated_boxing_textAlign,
                        style.deprecated_boxing_textDirection,
                        style.deprecated_boxing_hyphens,
                        style.deprecated_boxing_lineBreak,
                    )
                )
                .isEqualTo(expected)
        }
    }

    @Test
    fun lineHeight_emNegativeAndNaNThrow_andSpecialFloatsRoundTripLosslessly() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        for (invalid in listOf((-0.1f).em, Float.NaN.em, Float.NEGATIVE_INFINITY.sp)) {
            assertFailsWith<IllegalStateException> { ParagraphStyle(lineHeight = invalid) }
        }

        val edgeUnits =
            listOf(
                0f.sp,
                (-0f).sp,
                0f.em,
                (-0f).em,
                Float.fromBits(1).sp,
                Float.fromBits(1).em,
                Float.MAX_VALUE.sp,
                Float.MAX_VALUE.em,
                Float.POSITIVE_INFINITY.sp,
                Float.POSITIVE_INFINITY.em,
            )
        val styles = edgeUnits.map { ParagraphStyle(lineHeight = it) }
        for (i in edgeUnits.indices) {
            val expected = edgeUnits[i]
            val actual = styles[i].lineHeight
            assertParagraphStyleInvariants(styles[i])
            assertThat(actual).isEqualTo(expected)
            assertThat(actual.isEm).isEqualTo(expected.isEm)
            assertThat(actual.value.toRawBits()).isEqualTo(expected.value.toRawBits())
            for (j in edgeUnits.indices) {
                if (i != j) assertThat(styles[i]).isNotEqualTo(styles[j])
            }
        }
    }

    @Test
    fun packParagraphStyleBits_normalizesStructurallyEqualPresets_andDistinguishesEmZeroIndent() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        // Freshly allocated instances structurally equal to presets must avoid `extras` allocation.
        val equalPresets =
            ParagraphStyle(
                textIndent = TextIndent(0.sp, 0.sp),
                platformStyle =
                    PlatformParagraphStyle(
                        includeFontPadding = false,
                        emojiSupportMatch = EmojiSupportMatch.Default,
                    ),
                textMotion = TextMotion(TextMotion.Linearity.FontHinting, false),
            )
        assertThat(equalPresets.extras).isNull()
        assertThatParagraphStyle(equalPresets)
            .hasFields(
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                textMotion = TextMotion.Static,
            )
        assertParagraphStyleInvariants(equalPresets)

        // `TextIndent(0.em, 0.sp)` is not equal to `TextIndent.None` (`0.sp, 0.sp`), so it uses
        // `extras`.
        val emZeroIndent = TextIndent(firstLine = 0.em, restLine = 0.sp)
        val emIndentStyle = ParagraphStyle(textIndent = emZeroIndent)
        assertThat(emIndentStyle.extras).isNotNull()
        assertThat(emIndentStyle.textIndent).isEqualTo(emZeroIndent)
        assertThat(emIndentStyle).isNotEqualTo(ParagraphStyle(textIndent = TextIndent.None))
        assertParagraphStyleInvariants(emIndentStyle)
    }

    @Test
    fun paragraphStyleExtras_matchesEqualsAndHashCode_andCrossFieldGetters() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val textIndent = TextIndent(4.sp, 8.sp)
        val platformStyle = PlatformParagraphStyle(includeFontPadding = true)
        val textMotion = TextMotion(TextMotion.Linearity.None, true)
        val extras = ParagraphStyleExtras(textIndent, platformStyle, textMotion)

        // `matches` checks reference identity (`===`); `equals` checks structural equality (`==`).
        assertThat(extras.matches(textIndent, platformStyle, textMotion)).isTrue()
        assertThat(extras.matches(TextIndent(4.sp, 8.sp), platformStyle, textMotion)).isFalse()
        assertThat(
                extras.matches(
                    textIndent,
                    PlatformParagraphStyle(includeFontPadding = true),
                    textMotion,
                )
            )
            .isFalse()
        assertThat(
                extras.matches(
                    textIndent,
                    platformStyle,
                    TextMotion(TextMotion.Linearity.None, true),
                )
            )
            .isFalse()

        val equalCopy =
            ParagraphStyleExtras(
                TextIndent(4.sp, 8.sp),
                PlatformParagraphStyle(includeFontPadding = true),
                TextMotion(TextMotion.Linearity.None, true),
            )
        assertThat(extras).isEqualTo(extras)
        assertThat(extras).isEqualTo(equalCopy)
        assertThat(extras.hashCode()).isEqualTo(equalCopy.hashCode())
        assertThat(extras.equals(null)).isFalse()
        assertThat(extras.equals("other")).isFalse()
        for (differentExtras in
            listOf(
                ParagraphStyleExtras(TextIndent(6.sp, 8.sp), platformStyle, textMotion),
                ParagraphStyleExtras(textIndent, PlatformParagraphStyle(false), textMotion),
                ParagraphStyleExtras(
                    textIndent,
                    platformStyle,
                    TextMotion(TextMotion.Linearity.None, false),
                ),
                ParagraphStyleExtras(null, platformStyle, textMotion),
                ParagraphStyleExtras(textIndent, null, textMotion),
                ParagraphStyleExtras(textIndent, platformStyle, null),
            )) {
            assertThat(extras).isNotEqualTo(differentExtras)
        }
        assertThat(ParagraphStyleExtras(null, null, null).hashCode()).isEqualTo(0)

        // Cross-field fallback when `extras != null` (allocated for one field) while other fields
        // are presets or null encoded in `bits`.
        val withCustomIndent =
            ParagraphStyle(
                textIndent = textIndent,
                platformStyle = PlatformParagraphStyle.Default,
                textMotion = TextMotion.Static,
            )
        assertThat(withCustomIndent.extras!!.platformStyle).isNull()
        assertThat(withCustomIndent.extras!!.textMotion).isNull()
        assertThat(withCustomIndent.textIndent).isSameInstanceAs(textIndent)
        assertThatParagraphStyle(withCustomIndent)
            .hasFields(
                textIndent = textIndent,
                platformStyle = PlatformParagraphStyle.Default,
                textMotion = TextMotion.Static,
            )

        val withCustomPlatform =
            ParagraphStyle(
                textIndent = TextIndent.None,
                platformStyle = platformStyle,
                textMotion = TextMotion.Animated,
            )
        assertThat(withCustomPlatform.extras!!.textIndent).isNull()
        assertThat(withCustomPlatform.platformStyle).isSameInstanceAs(platformStyle)
        assertThatParagraphStyle(withCustomPlatform)
            .hasFields(
                textIndent = TextIndent.None,
                platformStyle = platformStyle,
                textMotion = TextMotion.Animated,
            )
    }

    @Test
    fun isDefault_andMergeParagraphBits_singleFieldIsolation() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val base = fullyPopulatedParagraphStyle()
        assertParagraphStyleInvariants(base)

        val singleFieldOverrides =
            listOf(
                ParagraphStyle(hyphens = Hyphens.None),
                ParagraphStyle(platformStyle = PlatformParagraphStyle.Default),
                ParagraphStyle(textMotion = TextMotion.Animated),
                ParagraphStyle(textAlign = TextAlign.Left),
                ParagraphStyle(textDirection = TextDirection.Ltr),
                ParagraphStyle(textIndent = TextIndent.None),
                ParagraphStyle(lineBreak = LineBreak.Simple),
                ParagraphStyle(lineHeight = 1.5.em),
                ParagraphStyle(lineHeightStyle = LineHeightStyle.Default),
            )
        for (override in singleFieldOverrides) {
            assertThat(override.isDefault()).isFalse()
            assertParagraphStyleInvariants(override)
            val merged = base.merge(override)
            assertParagraphStyleInvariants(merged)
            assertThatParagraphStyle(merged)
                .matchesReference(base.toReference().merge(override.toReference()))
        }
    }

    @Test
    fun merge_andFastMerge_reuseInstancesAndTransitionExtrasCleanly() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val customTextIndent = TextIndent(4.sp, 8.sp)
        val customPlatformStyle = PlatformParagraphStyle(includeFontPadding = true)
        val customTextMotion = TextMotion(TextMotion.Linearity.None, true)
        val lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.Both)

        val baseNoExtras =
            ParagraphStyle(
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                lineHeightStyle = lineHeightStyle,
                hyphens = Hyphens.Auto,
            )
        val baseWithExtras =
            ParagraphStyle(
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                textIndent = customTextIndent,
                platformStyle = customPlatformStyle,
                lineHeightStyle = lineHeightStyle,
                textMotion = customTextMotion,
            )

        // `this.sameAs` and `other.sameAs` instance reuse in `merge`.
        val subset = ParagraphStyle(textAlign = TextAlign.Center)
        assertThat(baseWithExtras.merge(subset)).isSameInstanceAs(baseWithExtras)
        assertThat(subset.merge(baseWithExtras)).isSameInstanceAs(baseWithExtras)

        // `thisExtras.matches` and `otherExtras.matches` reuse `extras` instance.
        assertThat(baseWithExtras.merge(ParagraphStyle(textAlign = TextAlign.Right)).extras)
            .isSameInstanceAs(baseWithExtras.extras)
        val otherWithExtras =
            ParagraphStyle(
                textIndent = TextIndent(12.sp, 16.sp),
                platformStyle = customPlatformStyle,
                textMotion = customTextMotion,
            )
        assertThat(baseWithExtras.merge(otherWithExtras).extras)
            .isSameInstanceAs(otherWithExtras.extras)

        // `fastMerge` `onlyLineHeightMayChange` fast path (unspecified, same, and changed).
        for (base in listOf(baseNoExtras, baseWithExtras)) {
            assertThat(base.fastMergeOnly()).isSameInstanceAs(base)
            assertThat(base.fastMergeOnly(lineHeight = 16.sp)).isSameInstanceAs(base)
            val updatedLineHeight = base.fastMergeOnly(lineHeight = 2.em)
            assertThat(updatedLineHeight.lineHeight).isEqualTo(2.em)
            assertThat(updatedLineHeight.lineHeightStyle).isSameInstanceAs(base.lineHeightStyle)
            assertThat(updatedLineHeight.extras).isSameInstanceAs(base.extras)
            assertThat(updatedLineHeight).isEqualTo(base.merge(ParagraphStyle(lineHeight = 2.em)))
        }

        // Each of the 8 non-lineHeight fields individually enters the full-merge path of
        // `fastMerge`.
        val singleFieldFastMerges =
            listOf(
                baseWithExtras.fastMergeOnly(textAlign = TextAlign.End) to
                    ParagraphStyle(textAlign = TextAlign.End),
                baseWithExtras.fastMergeOnly(textDirection = TextDirection.Rtl) to
                    ParagraphStyle(textDirection = TextDirection.Rtl),
                baseWithExtras.fastMergeOnly(textIndent = TextIndent.None) to
                    ParagraphStyle(textIndent = TextIndent.None),
                baseWithExtras.fastMergeOnly(platformStyle = PlatformParagraphStyle.Default) to
                    ParagraphStyle(platformStyle = PlatformParagraphStyle.Default),
                baseWithExtras.fastMergeOnly(lineHeightStyle = LineHeightStyle.Default) to
                    ParagraphStyle(lineHeightStyle = LineHeightStyle.Default),
                baseWithExtras.fastMergeOnly(lineBreak = LineBreak.Heading) to
                    ParagraphStyle(lineBreak = LineBreak.Heading),
                baseWithExtras.fastMergeOnly(hyphens = Hyphens.None) to
                    ParagraphStyle(hyphens = Hyphens.None),
                baseWithExtras.fastMergeOnly(textMotion = TextMotion.Static) to
                    ParagraphStyle(textMotion = TextMotion.Static),
            )
        for ((fastMerged, overrideStyle) in singleFieldFastMerges) {
            assertThat(fastMerged).isEqualTo(baseWithExtras.merge(overrideStyle))
            assertParagraphStyleInvariants(fastMerged)
        }

        // Overriding all custom `extras` with presets collapses `extras` to `null` in both `merge`
        // and `fastMerge`; adding custom `extras` promotes `extras` from `null` to non-null.
        val presetOverrides =
            ParagraphStyle(
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                textMotion = TextMotion.Animated,
            )
        val collapsedMerge = baseWithExtras.merge(presetOverrides)
        val collapsedFast =
            baseWithExtras.fastMergeOnly(
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                textMotion = TextMotion.Animated,
            )
        assertThat(collapsedMerge.extras).isNull()
        assertThat(collapsedFast.extras).isNull()
        assertThat(collapsedFast).isEqualTo(collapsedMerge)
        assertThat(baseNoExtras.fastMergeOnly(textIndent = customTextIndent).extras).isNotNull()
    }

    @Test
    fun resolveParagraphStyleDefaults_coversLtrAndRtlSingletons_andAllTextDirections() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        assertThat(resolveParagraphStyleDefaults(ParagraphStyle(), LayoutDirection.Ltr))
            .isSameInstanceAs(BoxedStyleValues.ResolvedDefaultParagraphStyleLtr)
        assertThat(resolveParagraphStyleDefaults(ParagraphStyle(), LayoutDirection.Rtl))
            .isSameInstanceAs(BoxedStyleValues.ResolvedDefaultParagraphStyleRtl)

        val expectedDirections =
            mapOf(
                (TextDirection.Unspecified to LayoutDirection.Ltr) to TextDirection.Ltr,
                (TextDirection.Unspecified to LayoutDirection.Rtl) to TextDirection.Rtl,
                (TextDirection.Content to LayoutDirection.Ltr) to TextDirection.ContentOrLtr,
                (TextDirection.Content to LayoutDirection.Rtl) to TextDirection.ContentOrRtl,
                (TextDirection.Ltr to LayoutDirection.Ltr) to TextDirection.Ltr,
                (TextDirection.Ltr to LayoutDirection.Rtl) to TextDirection.Ltr,
                (TextDirection.Rtl to LayoutDirection.Ltr) to TextDirection.Rtl,
                (TextDirection.Rtl to LayoutDirection.Rtl) to TextDirection.Rtl,
                (TextDirection.ContentOrLtr to LayoutDirection.Ltr) to TextDirection.ContentOrLtr,
                (TextDirection.ContentOrLtr to LayoutDirection.Rtl) to TextDirection.ContentOrLtr,
                (TextDirection.ContentOrRtl to LayoutDirection.Ltr) to TextDirection.ContentOrRtl,
                (TextDirection.ContentOrRtl to LayoutDirection.Rtl) to TextDirection.ContentOrRtl,
            )
        for ((input, expectedDirection) in expectedDirections) {
            val (textDirection, layoutDirection) = input
            val resolved =
                resolveParagraphStyleDefaults(
                    ParagraphStyle(textDirection = textDirection, lineHeight = 18.sp),
                    layoutDirection,
                )
            assertParagraphStyleInvariants(resolved)
            assertThatParagraphStyle(resolved)
                .hasFields(
                    textDirection = expectedDirection,
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp,
                    textIndent = TextIndent.None,
                    lineBreak = LineBreak.Simple,
                    hyphens = Hyphens.None,
                    textMotion = TextMotion.Static,
                )
            assertThat(resolved.extras).isNull()
            assertThat(resolved.withResolvedDefaults(layoutDirection)).isSameInstanceAs(resolved)
        }

        val customStyle =
            ParagraphStyle(
                textIndent = TextIndent(4.sp, 8.sp),
                textMotion = TextMotion(TextMotion.Linearity.None, true),
            )
        val resolvedCustom = resolveParagraphStyleDefaults(customStyle, LayoutDirection.Rtl)
        assertThat(resolvedCustom.extras).isSameInstanceAs(customStyle.extras)
        assertThat(resolvedCustom.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun equalsHashCodeAndToString_distinguishCustomExtrasWithIdenticalBits() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val styleA =
            ParagraphStyle(
                textIndent = TextIndent(4.sp, 8.sp),
                platformStyle = PlatformParagraphStyle(includeFontPadding = true),
                textMotion = TextMotion(TextMotion.Linearity.None, true),
            )
        val styleB = styleA.copy(textIndent = TextIndent(10.sp, 8.sp))
        assertThat(styleA.bits).isEqualTo(styleB.bits)
        assertThat(styleA).isNotEqualTo(styleB)
        assertThat(styleA.hashCode()).isNotEqualTo(styleB.hashCode())
        assertThat(styleA.toString())
            .isEqualTo(
                "ParagraphStyle(" +
                    "textAlign=${styleA.textAlign}, " +
                    "textDirection=${styleA.textDirection}, " +
                    "lineHeight=${styleA.lineHeight}, " +
                    "textIndent=${styleA.textIndent}, " +
                    "platformStyle=${styleA.platformStyle}, " +
                    "lineHeightStyle=${styleA.lineHeightStyle}, " +
                    "lineBreak=${styleA.lineBreak}, " +
                    "hyphens=${styleA.hyphens}, " +
                    "textMotion=${styleA.textMotion})"
            )
    }

    @Test
    fun lerp_coversPlatformStyle_textMotion_andLineHeightStopUnspecified() {
        val platformStyleTrue =
            PlatformParagraphStyle(
                includeFontPadding = true,
                emojiSupportMatch = EmojiSupportMatch.All,
            )
        val platformStyleFalse =
            PlatformParagraphStyle(
                includeFontPadding = false,
                emojiSupportMatch = EmojiSupportMatch.None,
            )
        val customTextMotion = TextMotion(TextMotion.Linearity.None, true)
        val start =
            ParagraphStyle(
                lineHeight = 22.sp,
                platformStyle = platformStyleTrue,
                textMotion = TextMotion.Static,
            )
        val stop =
            ParagraphStyle(
                lineHeight = TextUnit.Unspecified,
                platformStyle = null,
                textMotion = customTextMotion,
            )

        val beforeHalf = lerp(start, stop, 0.4f)
        assertThatParagraphStyle(beforeHalf)
            .hasFields(
                lineHeight = 22.sp,
                platformStyle = lerp(platformStyleTrue, PlatformParagraphStyle.Default, 0.4f),
                textMotion = TextMotion.Static,
            )

        val atHalf = lerp(start, stop, 0.5f)
        assertThatParagraphStyle(atHalf)
            .hasFields(
                lineHeight = TextUnit.Unspecified,
                platformStyle = lerp(platformStyleTrue, PlatformParagraphStyle.Default, 0.5f),
                textMotion = customTextMotion,
            )

        assertThat(lerp(ParagraphStyle(platformStyle = null), start, 0.6f).platformStyle)
            .isEqualTo(lerp(PlatformParagraphStyle.Default, platformStyleTrue, 0.6f))
        assertThat(
                lerp(start, ParagraphStyle(platformStyle = platformStyleFalse), 0.6f).platformStyle
            )
            .isEqualTo(lerp(platformStyleTrue, platformStyleFalse, 0.6f))
    }

    private fun fullyPopulatedParagraphStyle(): ParagraphStyle =
        ParagraphStyle(
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl,
            lineHeight = 20.sp,
            textIndent = TextIndent(4.sp, 8.sp),
            platformStyle =
                PlatformParagraphStyle(
                    includeFontPadding = true,
                    emojiSupportMatch = EmojiSupportMatch.All,
                ),
            lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.FirstLineTop),
            lineBreak = LineBreak.Heading,
            hyphens = Hyphens.Auto,
            textMotion = TextMotion(TextMotion.Linearity.None, true),
        )

    private fun randomParagraphStyle(random: Random): ParagraphStyle =
        ParagraphStyle(
            textAlign =
                listOf(
                        TextAlign.Unspecified,
                        TextAlign.Left,
                        TextAlign.Right,
                        TextAlign.Center,
                        TextAlign.Justify,
                        TextAlign.Start,
                        TextAlign.End,
                    )
                    .random(random),
            textDirection =
                listOf(
                        TextDirection.Unspecified,
                        TextDirection.Ltr,
                        TextDirection.Rtl,
                        TextDirection.Content,
                        TextDirection.ContentOrLtr,
                        TextDirection.ContentOrRtl,
                    )
                    .random(random),
            lineHeight =
                listOf(TextUnit.Unspecified, 0.sp, (-0f).sp, 16.sp, 1.25.em).random(random),
            textIndent = listOf(null, TextIndent.None, TextIndent(4.sp, 8.sp)).random(random),
            platformStyle =
                listOf(
                        null,
                        PlatformParagraphStyle.Default,
                        PlatformParagraphStyle(includeFontPadding = true),
                        PlatformParagraphStyle(
                            includeFontPadding = false,
                            emojiSupportMatch = EmojiSupportMatch.None,
                        ),
                        PlatformParagraphStyle(
                            includeFontPadding = true,
                            emojiSupportMatch = EmojiSupportMatch.All,
                        ),
                    )
                    .random(random),
            lineHeightStyle =
                listOf(null, LineHeightStyle.Default, LineHeightStyle(Alignment.Center, Trim.None))
                    .random(random),
            lineBreak =
                listOf(
                        LineBreak.Unspecified,
                        LineBreak.Simple,
                        LineBreak.Heading,
                        LineBreak.Paragraph,
                    )
                    .random(random),
            hyphens = listOf(Hyphens.Unspecified, Hyphens.None, Hyphens.Auto).random(random),
            textMotion =
                listOf(
                        null,
                        TextMotion.Static,
                        TextMotion.Animated,
                        TextMotion(TextMotion.Linearity.None, true),
                    )
                    .random(random),
        )

    private fun assertParagraphStyleInvariants(style: ParagraphStyle) {
        // Bit 0 (`PARA_HAS_EXTRAS`) is set iff `extras != null`; unused bits 25..29 are always 0.
        assertThat(style.bits and 1L != 0L).isEqualTo(style.extras != null)
        assertThat(style.bits and (0x1FL shl 25)).isEqualTo(0L)
        assertThat(style.isDefault()).isEqualTo(style.bits == 0L && style.lineHeightStyle == null)
        assertThat(style.copy()).isEqualTo(style)
        assertThat(style.copy().hashCode()).isEqualTo(style.hashCode())
        assertThat(style.merge(null)).isSameInstanceAs(style)
        assertThat(style.merge(style)).isSameInstanceAs(style)
        assertThat(style.merge(ParagraphStyle())).isSameInstanceAs(style)
        if (!style.isDefault()) {
            assertThat(ParagraphStyle().merge(style)).isSameInstanceAs(style)
        }
        assertThat(
                style.fastMergeOnly(
                    textAlign = style.textAlign,
                    textDirection = style.textDirection,
                    lineHeight = style.lineHeight,
                    textIndent = style.textIndent,
                    platformStyle = style.platformStyle,
                    lineHeightStyle = style.lineHeightStyle,
                    lineBreak = style.lineBreak,
                    hyphens = style.hyphens,
                    textMotion = style.textMotion,
                )
            )
            .isSameInstanceAs(style)
    }

    private fun ParagraphStyle.fastMergeOnly(
        textAlign: TextAlign = TextAlign.Unspecified,
        textDirection: TextDirection = TextDirection.Unspecified,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak = LineBreak.Unspecified,
        hyphens: Hyphens = Hyphens.Unspecified,
        textMotion: TextMotion? = null,
    ): ParagraphStyle =
        fastMerge(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion,
        )
}

private fun assertThatParagraphStyle(actual: ParagraphStyle): ParagraphStyleSubject =
    ParagraphStyleSubject(actual)

/**
 * Test helper for [ParagraphStyle] that checks all 9 fields against [RefParagraphStyle] or target
 * values.
 */
private class ParagraphStyleSubject(private val actual: ParagraphStyle) {
    fun matchesReference(expected: RefParagraphStyle) {
        hasFields(
            textAlign = expected.textAlign,
            textDirection = expected.textDirection,
            lineHeight = expected.lineHeight,
            textIndent = expected.textIndent,
            platformStyle = expected.platformStyle,
            lineHeightStyle = expected.lineHeightStyle,
            lineBreak = expected.lineBreak,
            hyphens = expected.hyphens,
            textMotion = expected.textMotion,
        )
    }

    fun hasFields(
        textAlign: TextAlign = actual.textAlign,
        textDirection: TextDirection = actual.textDirection,
        lineHeight: TextUnit = actual.lineHeight,
        textIndent: TextIndent? = actual.textIndent,
        platformStyle: PlatformParagraphStyle? = actual.platformStyle,
        lineHeightStyle: LineHeightStyle? = actual.lineHeightStyle,
        lineBreak: LineBreak = actual.lineBreak,
        hyphens: Hyphens = actual.hyphens,
        textMotion: TextMotion? = actual.textMotion,
    ) {
        assertThat(actual.textAlign).isEqualTo(textAlign)
        assertThat(actual.textDirection).isEqualTo(textDirection)
        assertThat(actual.lineHeight).isEqualTo(lineHeight)
        assertThat(actual.textIndent).isEqualTo(textIndent)
        assertThat(actual.platformStyle).isEqualTo(platformStyle)
        assertThat(actual.lineHeightStyle).isEqualTo(lineHeightStyle)
        assertThat(actual.lineBreak).isEqualTo(lineBreak)
        assertThat(actual.hyphens).isEqualTo(hyphens)
        assertThat(actual.textMotion).isEqualTo(textMotion)
    }
}

private fun ParagraphStyle.toReference(): RefParagraphStyle =
    RefParagraphStyle(
        textAlign = textAlign,
        textDirection = textDirection,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = lineBreak,
        hyphens = hyphens,
        textMotion = textMotion,
    )

/**
 * Unpacked fake for [ParagraphStyle].
 *
 * Production [ParagraphStyle] packs its fields into `bits` and uses [ParagraphStyleExtras] for rare
 * objects. This class stores all 9 fields as plain values. It runs [merge], [resolveDefaults],
 * [equals], and [hashCode] field by field so tests can check the packed math against a simple rule.
 */
private class RefParagraphStyle(
    val textAlign: TextAlign,
    val textDirection: TextDirection,
    val lineHeight: TextUnit,
    val textIndent: TextIndent?,
    val platformStyle: PlatformParagraphStyle?,
    val lineHeightStyle: LineHeightStyle?,
    val lineBreak: LineBreak,
    val hyphens: Hyphens,
    val textMotion: TextMotion?,
) {
    /** Merges fields one by one to check [ParagraphStyle.merge]. */
    fun merge(other: RefParagraphStyle): RefParagraphStyle =
        RefParagraphStyle(
            lineHeight = if (other.lineHeight.isUnspecified) lineHeight else other.lineHeight,
            textIndent = other.textIndent ?: textIndent,
            textAlign =
                if (other.textAlign == TextAlign.Unspecified) textAlign else other.textAlign,
            textDirection =
                if (other.textDirection == TextDirection.Unspecified) {
                    textDirection
                } else {
                    other.textDirection
                },
            platformStyle =
                if (platformStyle == null) {
                    other.platformStyle
                } else if (other.platformStyle == null) {
                    platformStyle
                } else {
                    platformStyle.merge(other.platformStyle)
                },
            lineHeightStyle = other.lineHeightStyle ?: lineHeightStyle,
            lineBreak =
                if (other.lineBreak == LineBreak.Unspecified) lineBreak else other.lineBreak,
            hyphens = if (other.hyphens == Hyphens.Unspecified) hyphens else other.hyphens,
            textMotion = other.textMotion ?: textMotion,
        )

    /** Resolves defaults one by one to check [resolveParagraphStyleDefaults]. */
    fun resolveDefaults(direction: LayoutDirection): RefParagraphStyle =
        RefParagraphStyle(
            textAlign = if (textAlign == TextAlign.Unspecified) TextAlign.Start else textAlign,
            textDirection = resolveTextDirection(direction, textDirection),
            lineHeight = if (lineHeight.isUnspecified) TextUnit.Unspecified else lineHeight,
            textIndent = textIndent ?: TextIndent.None,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = if (lineBreak == LineBreak.Unspecified) LineBreak.Simple else lineBreak,
            hyphens = if (hyphens == Hyphens.Unspecified) Hyphens.None else hyphens,
            textMotion = textMotion ?: TextMotion.Static,
        )

    /** Checks if all 9 unpacked fields match. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefParagraphStyle) return false
        return textAlign == other.textAlign &&
            textDirection == other.textDirection &&
            lineHeight == other.lineHeight &&
            textIndent == other.textIndent &&
            platformStyle == other.platformStyle &&
            lineHeightStyle == other.lineHeightStyle &&
            lineBreak == other.lineBreak &&
            hyphens == other.hyphens &&
            textMotion == other.textMotion
    }

    /** Hashes all 9 unpacked fields. */
    override fun hashCode(): Int {
        var result = textAlign.hashCode()
        result = 31 * result + textDirection.hashCode()
        result = 31 * result + lineHeight.hashCode()
        result = 31 * result + (textIndent?.hashCode() ?: 0)
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (lineHeightStyle?.hashCode() ?: 0)
        result = 31 * result + lineBreak.hashCode()
        result = 31 * result + hyphens.hashCode()
        result = 31 * result + (textMotion?.hashCode() ?: 0)
        return result
    }
}
