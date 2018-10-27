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
package androidx.ui.port.engine.text

import android.app.Instrumentation
import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.Paragraph
import androidx.ui.engine.text.ParagraphConstraints
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.engine.text.platform.StaticLayoutFactory
import androidx.ui.port.bitmap
import androidx.ui.port.matchers.equalToBitmap
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class ParagraphTest {
    private lateinit var instrumentation: Instrumentation
    private lateinit var fontFallback: FontFallback

    @Before
    fun setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val font = Typeface.createFromAsset(instrumentation.context.assets, "sample_font.ttf")!!
        fontFallback = FontFallback(font)
    }

    @Test
    fun empty_string() {
        val fontSize = 50.0
        val text = StringBuilder("")
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = 100.0))

        assertThat(paragraph.width, equalTo(100.0))
        assertThat(paragraph.height, equalTo(fontSize))
        // defined in sample_font
        assertThat(paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0))
        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0))
        assertThat(paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        // TODO(Migration/siyamed): no baseline query per line?
        // TODO(Migration/siyamed): no line count?
    }

    @Test
    fun single_line_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
            val paragraph = simpleParagraph(text, fontSize)

            // width greater than text width - 150
            paragraph.layout(ParagraphConstraints(width = 200.0))

            assertThat(text, paragraph.width, equalTo(200.0))
            assertThat(text, paragraph.height, equalTo(fontSize))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.length))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text, fontSize)

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize))
            // 2 lines, 1 line gap
            assertThat(text, paragraph.height, equalTo(2 * fontSize + fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.length))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun newline_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text, fontSize)

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize))
            // 2 lines, 1 line gap
            assertThat(text, paragraph.height, equalTo(2 * fontSize + fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.indexOf("\n")))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun newline_and_line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text, fontSize)

            // 2 chars width
            paragraph.layout(ParagraphConstraints(width = 2 * fontSize))

            // 2 chars
            assertThat(text, paragraph.width, equalTo(2 * fontSize))
            // 4 lines, 3 line gaps
            assertThat(text, paragraph.height, equalTo(4 * fontSize + 3 * fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.indexOf("\n")))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun draw_with_newline_and_line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text, fontSize)

            // 2 chars width
            paragraph.layout(ParagraphConstraints(width = 2 * fontSize))

            val textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = fontSize.toFloat()
            textPaint.typeface = fontFallback.typeface

            val staticLayout = StaticLayoutFactory.create(
                textPaint = textPaint,
                charSequence = text,
                width = ceil(paragraph.width).toInt(),
                ellipsizeWidth = ceil(paragraph.width).toInt()
            )

            assertThat(paragraph.bitmap(), equalToBitmap(staticLayout.bitmap()))
        }
    }

    @Test
    fun getPositionForOffset_ltr() {
        val text = "abc"
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize))
        // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars 0, 1, 2 ...
        for (i in 0..text.length) {
            val offset = Offset(i * fontSize + 1, fontSize / 2)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset does not match",
                position,
                equalTo(TextPosition(i, TextAffinity.upstream))
            )
        }
    }

    @Test
    fun getPositionForOffset_rtl() {
        val text = "\u05D0\u05D1\u05D2"
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize))

        // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars .., 2, 1, 0
        for (i in 0..text.length) {
            val offset = Offset(i * fontSize + 1, fontSize / 2)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset does not match",
                position,
                equalTo(TextPosition(text.length - i, TextAffinity.upstream))
            )
        }
    }

    @Test
    fun getPositionForOffset_ltr_multiline() {
        val firstLine = "abc"
        val secondLine = "def"
        val text = firstLine + secondLine
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = firstLine.length * fontSize))

        // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
        // which maps to chars 3, 4, 5
        for (i in 0..secondLine.length) {
            val offset = Offset(i * fontSize + 1, fontSize * 1.5)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset, second line does not match",
                position,
                equalTo(TextPosition(i + firstLine.length, TextAffinity.upstream))
            )
        }
    }

    @Test
    fun getPositionForOffset_rtl_multiline() {
        val firstLine = "\u05D0\u05D1\u05D2"
        val secondLine = "\u05D3\u05D4\u05D5"
        val text = firstLine + secondLine
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize))

        // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
        // which maps to chars 5, 4, 3
        for (i in 0..secondLine.length) {
            val offset = Offset(i * fontSize + 1, fontSize / 2)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset, second line does not match",
                position,
                equalTo(TextPosition(text.length - i, TextAffinity.upstream))
            )
        }
    }

    @Test
    fun getPositionForOffset_ltr_width_outOfBounds() {
        val text = "abc"
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize))

        // greater than width
        var offset = Offset(fontSize * text.length * 2, fontSize / 2)
        var position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(TextPosition(text.length, TextAffinity.upstream)))

        // negative
        offset = Offset(-1 * fontSize, fontSize / 2)
        position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(TextPosition(0, TextAffinity.upstream)))
    }

    @Test
    fun getPositionForOffset_ltr_height_outOfBounds() {
        val text = "abc"
        val fontSize = 50.0
        val paragraph = simpleParagraph(text, fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize))

        // greater than height
        var offset = Offset(fontSize / 2, fontSize * text.length * 2)
        var position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(TextPosition(0, TextAffinity.upstream)))

        // negative
        offset = Offset(fontSize / 2, -1 * fontSize)
        position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(TextPosition(0, TextAffinity.upstream)))
    }

    // TODO(migration/siyamed) add test
    @Test
    fun getWordBoundary() {
    }

    fun simpleParagraph(text: CharSequence, fontSize: Double): Paragraph {
        return Paragraph(
            text = StringBuilder(text),
            textStyles = listOf(),
            paragraphStyle = ParagraphStyle(
                fontFamily = fontFallback,
                fontSize = fontSize
            )
        )
    }
}