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
package androidx.ui.engine.text

import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.TextStyle
import com.nhaarman.mockitokotlin2.mock
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphTest {

    @Test
    fun `width default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.width, equalTo(-1.0f))
    }

    @Test
    fun `height default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.height, equalTo(0.0f))
    }

    @Test
    fun `minIntrinsicWidth default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0f))
    }

    @Test
    fun `maxIntrinsicWidth  default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0f))
    }

    @Test
    fun `alphabeticBaseline default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.baseline, equalTo(Float.MAX_VALUE))
    }

    @Test
    fun `didExceedMaxLines default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test(expected = IllegalStateException::class)
    fun `paint throws exception if layout is not called`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        paragraph.paint(mock(), 0.0f, 0.0f)
    }

    @Test(expected = IllegalStateException::class)
    fun `getPositionForOffset throws exception if layout is not called`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        paragraph.getPositionForOffset(Offset(0.0f, 0.0f))
    }

    @Test(expected = AssertionError::class)
    fun `getPathForRange throws exception if start larger than end`() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        paragraph.getPathForRange(textEnd, textStart)
    }

    @Test(expected = AssertionError::class)
    fun `getPathForRange throws exception if start is smaller than 0`() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        paragraph.getPathForRange(textStart - 2, textEnd - 1)
    }

    @Test(expected = AssertionError::class)
    fun `getPathForRange throws exception if end is larger than text length`() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraphStyle = createParagraphStyle()
        val paragraph = createParagraph(paragraphStyle)

        paragraph.getPathForRange(textStart, textEnd + 1)
    }

    private fun createParagraph(paragraphStyle: ParagraphStyle): Paragraph {
        return Paragraph(text = "",
            style = TextStyle(),
            paragraphStyle = paragraphStyle,
            textStyles = listOf())
    }

    private fun createParagraphStyle(): ParagraphStyle {
        val textAlign = TextAlign.End
        val textDirection = TextDirection.Rtl
        val maxLines = 2
        val lineHeight = 2.0f
        val ellipsis = false

        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            maxLines = maxLines,
            lineHeight = lineHeight,
            ellipsis = ellipsis
        )
    }
}