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

package androidx.ui.rendering.paragraph

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.graphics.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RenderParagraphTest {
    @Test
    fun `RenderParagraph constructor with minimum amount of parameters`() {
        val text = TextSpan()

        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        assertThat(paragraph.text).isEqualTo(text)
        assertThat(paragraph.textAlign).isEqualTo(TextAlign.Start)
        assertThat(paragraph.textDirection).isEqualTo(TextDirection.LTR)
        assertThat(paragraph.softWrap).isTrue()
        assertThat(paragraph.overflow).isEqualTo(TextOverflow.CLIP)
        assertThat(paragraph.textScaleFactor).isEqualTo(1.0f)
        assertThat(paragraph.maxLines).isNull()
        assertThat(paragraph.textPainter.text).isEqualTo(text)
        assertThat(paragraph.textPainter.textAlign).isEqualTo(TextAlign.Start)
        assertThat(paragraph.textPainter.textDirection).isEqualTo(TextDirection.LTR)
        assertThat(paragraph.textPainter.textScaleFactor).isEqualTo(1.0f)
        assertThat(paragraph.textPainter.maxLines).isNull()
        assertThat(paragraph.textPainter.ellipsis).isFalse()
    }

    @Test
    fun `RenderParagraph constructor with customized values`() {
        val text = TextSpan()
        val textScaleFactor = 5.0f
        val maxLines = 7

        val paragraph =
            RenderParagraph(
                text = text,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.RTL,
                softWrap = false,
                overflow = TextOverflow.ELLIPSIS,
                textScaleFactor = textScaleFactor,
                maxLines = maxLines
            )

        assertThat(paragraph.text).isEqualTo(text)
        assertThat(paragraph.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraph.textDirection).isEqualTo(TextDirection.RTL)
        assertThat(paragraph.softWrap).isFalse()
        assertThat(paragraph.overflow).isEqualTo(TextOverflow.ELLIPSIS)
        assertThat(paragraph.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(paragraph.maxLines).isEqualTo(maxLines)
        assertThat(paragraph.textPainter.text).isEqualTo(text)
        assertThat(paragraph.textPainter.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraph.textPainter.textDirection).isEqualTo(TextDirection.RTL)
        assertThat(paragraph.textPainter.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(paragraph.textPainter.maxLines).isEqualTo(maxLines)
        assertThat(paragraph.textPainter.ellipsis).isEqualTo(true)
    }

    @Test
    fun `RenderParagraph text set nothing new causes RenderComparison IDENTICAL`() {
        val initText = TextSpan()
        val paragraph = RenderParagraph(text = initText, textDirection = TextDirection.LTR)
        val newText = TextSpan()

        paragraph.text = newText

        assertThat(paragraph.text).isSameInstanceAs(initText)
    }

    @Test
    fun `RenderParagraph text set different color causes RenderComparison PAINT`() {
        val initText = TextSpan()
        val paragraph = RenderParagraph(text = initText, textDirection = TextDirection.LTR)
        val newText = TextSpan(TextStyle(color = Color(0x0111)))

        paragraph.text = newText

        assertThat(paragraph.text).isEqualTo(newText)
    }

    @Test
    fun `RenderParagraph text set different letterSpacing causes RenderComparison LAYOUT`() {
        val initText = TextSpan()
        val paragraph = RenderParagraph(text = initText, textDirection = TextDirection.LTR)
        val newText = TextSpan(TextStyle(letterSpacing = 5.0f))

        paragraph.text = newText

        assertThat(paragraph.text).isEqualTo(newText)
    }

    @Test
    fun `RenderParagraph textAlign setter`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        paragraph.textAlign = TextAlign.Justify

        assertThat(paragraph.textAlign).isEqualTo(TextAlign.Justify)
    }

    @Test
    fun `RenderParagraph textDirection setter`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        paragraph.textDirection = TextDirection.RTL

        assertThat(paragraph.textDirection).isEqualTo(TextDirection.RTL)
    }

    @Test
    fun `RenderParagraph softWrap setter`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        paragraph.softWrap = false

        assertThat(paragraph.softWrap).isFalse()
    }

    @Test
    fun `RenderParagraph overflow setter not to ellipsis`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        paragraph.overflow = TextOverflow.FADE

        assertThat(paragraph.overflow).isEqualTo(TextOverflow.FADE)
        assertThat(paragraph.textPainter.ellipsis).isFalse()
    }

    @Test
    fun `RenderParagraph overflow setter to ellipsis`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)

        paragraph.overflow = TextOverflow.ELLIPSIS

        assertThat(paragraph.overflow).isEqualTo((TextOverflow.ELLIPSIS))
        assertThat(paragraph.textPainter.ellipsis).isEqualTo(true)
    }

    @Test
    fun `RenderParagraph textScaleFactor setter`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)
        val textScaleFactor = 5.0f

        paragraph.textScaleFactor = textScaleFactor

        assertThat(paragraph.textScaleFactor).isEqualTo(textScaleFactor)
    }

    @Test
    fun `RenderParagraph maxLines setter`() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.LTR)
        val maxLines = 5

        paragraph.maxLines = maxLines

        assertThat(paragraph.maxLines).isEqualTo(maxLines)
    }
}
