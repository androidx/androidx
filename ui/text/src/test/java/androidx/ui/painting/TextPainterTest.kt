/*
* Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.sp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.window.Locale
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextPainterTest() {
    private val density = Density(density = 1f)

    @Test
    fun `constructor with default values`() {
        val textPainter = TextPainter(density = density)

        assertThat(textPainter.text).isNull()
        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Start)
        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Ltr)
        assertThat(textPainter.textScaleFactor).isEqualTo(1.0f)
        assertThat(textPainter.maxLines).isNull()
        assertThat(textPainter.overflow).isEqualTo(TextOverflow.Clip)
        assertThat(textPainter.locale).isNull()
    }

    @Test
    fun `constructor with customized text(TextSpan)`() {
        val text = AnnotatedString("Hello")
        val textPainter = TextPainter(text = text, density = density)

        assertThat(textPainter.text).isEqualTo(text)
    }

    @Test
    fun `constructor with customized textAlign`() {
        val textPainter = TextPainter(
            paragraphStyle = ParagraphStyle(textAlign = TextAlign.Left),
            density = density
        )

        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Left)
    }

    @Test
    fun `constructor with customized textDirection`() {
        val textPainter = TextPainter(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl),
            density = density
        )

        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun `constructor with customized textScaleFactor`() {
        val scaleFactor = 2.0f

        val textPainter = TextPainter(textScaleFactor = scaleFactor, density = density)

        assertThat(textPainter.textScaleFactor).isEqualTo(scaleFactor)
    }

    @Test
    fun `constructor with customized maxLines`() {
        val maxLines = 8

        val textPainter = TextPainter(maxLines = maxLines, density = density)

        assertThat(textPainter.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor with customized overflow`() {
        val overflow = TextOverflow.Ellipsis

        val textPainter = TextPainter(overflow = overflow, density = density)

        assertThat(textPainter.overflow).isEqualTo(overflow)
    }

    @Test
    fun `constructor with customized locale`() {
        val locale = Locale("en", "US")

        val textPainter = TextPainter(locale = locale, density = density)

        assertThat(textPainter.locale).isEqualTo(locale)
    }

    @Test
    fun `text setter`() {
        val textPainter = TextPainter(density = density)
        val text = AnnotatedString(text = "Hello")

        textPainter.text = text

        assertThat(textPainter.text).isEqualTo(text)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textAlign setter`() {
        val textPainter = TextPainter(density = density)

        textPainter.textAlign = TextAlign.Left

        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Left)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textDirection setter`() {
        val textPainter = TextPainter(density = density)

        textPainter.textDirection = TextDirection.Rtl

        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.layoutTemplate).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textScaleFactor setter`() {
        val textPainter = TextPainter(density = density)
        val scaleFactor = 3.0f

        textPainter.textScaleFactor = scaleFactor

        assertThat(textPainter.textScaleFactor).isEqualTo(scaleFactor)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.layoutTemplate).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `maxLines setter`() {
        val textPainter = TextPainter(density = density)
        val maxLines = 5

        textPainter.maxLines = maxLines

        assertThat(textPainter.maxLines).isEqualTo(maxLines)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `overflow setter`() {
        val textPainter = TextPainter(density = density)
        val overflow = TextOverflow.Ellipsis

        textPainter.overflow = overflow

        assertThat(textPainter.overflow).isEqualTo(overflow)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `locale setter`() {
        val textPainter = TextPainter(density = density)
        val locale = Locale("en", "US")

        textPainter.locale = locale

        assertThat(textPainter.locale).isEqualTo(locale)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `createParagraphStyle without TextStyle in AnnotatedText`() {
        val scaleFactor = 3.0f
        val maxLines = 5
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")
        val text = AnnotatedString(text = "Hello")
        val textPainter = TextPainter(
            text = text,
            paragraphStyle = ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            textScaleFactor = scaleFactor,
            maxLines = maxLines,
            overflow = overflow,
            locale = locale,
            density = Density(density = 1f)
        )

        val paragraphStyle = textPainter.createParagraphStyle()

        assertThat(paragraphStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraphStyle.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(paragraphStyle.maxLines).isEqualTo(maxLines)
        assertThat(paragraphStyle.ellipsis).isEqualTo(true)
    }

    @Test
    fun `createParagraphStyle with defaultTextDirection`() {
        val fontSize = 15.sp
        val scaleFactor = 3.0f
        val maxLines = 5
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")
        val textStyle = TextStyle(fontSize = fontSize)
        val text = AnnotatedString(text = "Hello")
        val textPainter = TextPainter(
            text = text,
            style = textStyle,
            paragraphStyle = ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            textScaleFactor = scaleFactor,
            maxLines = maxLines,
            overflow = overflow,
            locale = locale,
            density = density
        )

        val paragraphStyle = textPainter.createParagraphStyle()

        assertThat(paragraphStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraphStyle.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(paragraphStyle.maxLines).isEqualTo(maxLines)
        assertThat(paragraphStyle.ellipsis).isEqualTo(true)
    }

    @Test
    fun `applyFloatingPointHack with value is integer toDouble`() {
        assertThat(applyFloatingPointHack(2f)).isEqualTo(2.0f)
    }

    @Test
    fun `applyFloatingPointHack with value smaller than half`() {
        assertThat(applyFloatingPointHack(2.2f)).isEqualTo(3.0f)
    }

    @Test
    fun `applyFloatingPointHack with value larger than half`() {
        assertThat(applyFloatingPointHack(2.8f)).isEqualTo(3.0f)
    }

    @Test(expected = AssertionError::class)
    fun `minIntrinsicWidth without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)

        textPainter.minIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)

        textPainter.maxIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `width without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)

        textPainter.width
    }

    @Test(expected = AssertionError::class)
    fun `height without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)

        textPainter.height
    }

    @Test(expected = AssertionError::class)
    fun `size without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)

        textPainter.size
    }

    @Test(expected = AssertionError::class)
    fun `layout without text assertion should fail`() {
        val textPainter = TextPainter(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr),
            density = density
        )

        textPainter.layout(Constraints())
    }

    @Test(expected = AssertionError::class)
    fun `paint without layout assertion should fail`() {
        val textPainter = TextPainter(density = density)
        val canvas = mock<Canvas>()

        textPainter.paint(canvas, Offset(0.0f, 0.0f))
    }
}
