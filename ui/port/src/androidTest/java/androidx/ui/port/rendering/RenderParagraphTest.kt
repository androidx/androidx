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

package androidx.ui.port.rendering

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.Font
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.services.text_editing.TextSelection
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.text.Typography.paragraph

@RunWith(JUnit4::class)
@SmallTest
class RenderParagraphTest {
    private val BASIC_MEASURE_FONT = Font(
        name = "sample_font.ttf",
        weight = FontWeight.normal,
        style = FontStyle.normal
    )

    private lateinit var fontFamily: FontFamily

    @Before
    fun setup() {
        fontFamily = BASIC_MEASURE_FONT.asFontFamily()
        fontFamily.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun computeMinIntrinsicWidth_returnMinWidth() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        assertThat(paragraph.computeMinIntrinsicWidth(0.0f)).isEqualTo(0.0f)
    }

    @Test
    fun computeMaxIntrinsicWidth_returnParagraphWidth() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        assertThat(paragraph.computeMaxIntrinsicWidth(0.0f)).isEqualTo(fontSize * text.length)
    }

    @Test
    fun computeIntrinsicHeight_wrap() {
        val fontSize = 16.8f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)
        val maxWidth = 38.0f
        val charPerLine = floor(maxWidth / fontSize)
        val numberOfLines = ceil(text.length / charPerLine)

        assertThat(paragraph.computeIntrinsicHeight(maxWidth))
            .isEqualTo(
                ceil(
                    fontSize * numberOfLines +
                            (fontSize / 5 /*gap due to font*/) * (numberOfLines - 1)
                )
            )
    }

    @Test
    fun computeIntrinsicHeight_not_wrap() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph =
            RenderParagraph(text = textSpan, textDirection = TextDirection.LTR, softWrap = false)
        val maxWidth = 38.0f

        assertThat(paragraph.computeIntrinsicHeight(maxWidth)).isEqualTo(fontSize)
    }

    @Test
    fun textSizeGetter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.textSize)
            .isEqualTo(Size(width = fontSize * text.length, height = fontSize))
    }

    @Test
    fun textWidthGetter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.width).isEqualTo(fontSize * text.length)
    }

    @Test
    fun textHeightGetter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.height).isEqualTo(fontSize)
    }

    @Test
    fun hasOverflowShaderFalse() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.performLayout(BoxConstraints())

        assertThat(paragraph.debugHasOverflowShader).isFalse()
    }

    @Test
    fun hasOverflowShaderFadeHorizontallyTrue() {
        val fontSize = 20.0f
        var text = ""
        for (i in 1..15) {
            text = text + "Hello World"
        }
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(
                text = textSpan,
                overflow = TextOverflow.FADE,
                textDirection = TextDirection.LTR,
                softWrap = false,
                maxLines = 1)

        paragraph.performLayout(BoxConstraints(maxWidth = 100.0f))

        assertThat(paragraph.debugHasOverflowShader).isTrue()
    }

    @Test
    fun hasOverflowShaderFadeVerticallyTrue() {
        val fontSize = 20.0f
        var text = ""
        for (i in 1..30) {
            text = text + "Hello World"
        }
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(
                text = textSpan,
                overflow = TextOverflow.FADE,
                textDirection = TextDirection.LTR,
                maxLines = 2)

        paragraph.performLayout(BoxConstraints(maxWidth = 100.0f))

        assertThat(paragraph.debugHasOverflowShader).isTrue()
    }

    @Test
    fun testGetPathForSelection_wrap_multiLines() {
        // Setup test.
        val fontSize = 20.0f
        val text = "HelloHello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)
        paragraph.layoutText(maxWidth = 120f)

        val expectedPath = Path()
        val firstLineLeft = paragraph.textPainter.paragraph?.getLineLeft(0)
        val secondLineLeft = paragraph.textPainter.paragraph?.getLineLeft(1)
        val firstLineRight = paragraph.textPainter.paragraph?.getLineRight(0)
        val secondLineRight = paragraph.textPainter.paragraph?.getLineRight(1)
        expectedPath.addRect(Rect(firstLineLeft!!, 0f, firstLineRight!!, fontSize))
        expectedPath.addRect(Rect(
                secondLineLeft!!,
                fontSize,
                secondLineRight!! - 2 * fontSize,
                paragraph.height))

        // Run.
        // Select all.
        val actualPath = paragraph.getPathForSelection(TextSelection(0, text.length))

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff).isEqualTo(Rect.zero)
    }
}