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
package androidx.ui.engine.text

import androidx.ui.engine.geometry.Offset
import com.nhaarman.mockitokotlin2.mock
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

@RunWith(JUnit4::class)
class ParagraphTest {

    @Test
    fun `width default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.width, equalTo(-1.0))
    }

    @Test
    fun `height default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.height, equalTo(0.0))
    }

    @Test
    fun `minIntrinsicWidth default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0))
    }

    @Test
    fun `maxIntrinsicWidth  default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0))
    }

    @Test
    fun `alphabeticBaseline default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.alphabeticBaseline, equalTo(Double.MAX_VALUE))
    }

    @Test
    fun `ideographicBaseline default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
    }

    @Test
    fun `didExceedMaxLines default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test(expected = IllegalStateException::class)
    fun `paint throws exception if layout is not called`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        paragraph.paint(mock(), 0.0, 0.0)
    }

    @Test(expected = IllegalStateException::class)
    fun `getPositionForOffset throws exception if layout is not called`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        paragraph.getPositionForOffset(Offset(0.0, 0.0))
    }

    private fun createParagraphStyle(): ParagraphStyle {
        val textAlign = TextAlign.end
        val textDirection = TextDirection.RTL
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.italic
        val maxLines = 2
        val fontFamily = FontFallback()
        val fontSize = 1.0
        val lineHeight = 2.0
        val ellipsis = "dot dot"
        val locale = Locale.ENGLISH

        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale
        )
    }
}