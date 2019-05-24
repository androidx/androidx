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

import android.graphics.Bitmap
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Constraints
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.matchers.equalToBitmap
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.services.text_editing.TextSelection
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil
import kotlin.math.floor

@RunWith(JUnit4::class)
@SmallTest
class RenderParagraphIntegrationTest {
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
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

        assertThat(paragraph.computeMinIntrinsicWidth()).isEqualTo(0.0f)
    }

    @Test
    fun computeMaxIntrinsicWidth_returnParagraphWidth() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

        assertThat(paragraph.computeMaxIntrinsicWidth()).isEqualTo(fontSize * text.length)
    }

    @Test
    fun computeIntrinsicHeight_wrap() {
        val fontSize = 16.8f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)
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
            RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr, softWrap = false)
        val maxWidth = 38.0f

        assertThat(paragraph.computeIntrinsicHeight(maxWidth)).isEqualTo(fontSize)
    }

    @Test
    fun textSizeGetter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

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
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

        paragraph.layoutText()

        assertThat(paragraph.width).isEqualTo(fontSize * text.length)
    }

    @Test
    fun textHeightGetter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

        paragraph.layoutText()

        assertThat(paragraph.height).isEqualTo(fontSize)
    }

    @Test
    fun hasOverflowShaderFalse() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)

        paragraph.performLayout(Constraints())

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
                overflow = TextOverflow.Fade,
                textDirection = TextDirection.Ltr,
                softWrap = false,
                maxLines = 1)

        paragraph.performLayout(Constraints(maxWidth = 100.ipx))

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
                overflow = TextOverflow.Fade,
                textDirection = TextDirection.Ltr,
                maxLines = 2)

        paragraph.performLayout(Constraints(maxWidth = 100.ipx))

        assertThat(paragraph.debugHasOverflowShader).isTrue()
    }

    @Test
    fun testGetPathForSelection_wrap_multiLines() {
        // Setup test.
        val fontSize = 20.0f
        val text = "HelloHello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)
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

    @Test
    fun testSelectionPaint_default_color() {
        val text = TextSpan()
        val paragraph = RenderParagraph(text = text, textDirection = TextDirection.Ltr)
        val defaultSelectionColor = Color(0x6633B5E5)

        assertThat(paragraph.selectionPaint.color).isEqualTo(defaultSelectionColor)
    }

    @Test
    fun testSelectionPaint_customized_color() {
        val text = TextSpan()
        val selectionColor = Color(0x66AABB33)

        val paragraph = RenderParagraph(
            text = text,
            textDirection = TextDirection.Ltr,
            selectionColor = selectionColor)

        assertThat(paragraph.selectionPaint.color).isEqualTo(selectionColor)
    }

    @Test
    fun testSelectionPaint_paint_with_default_color() {
        // Setup test.
        val selectionStart = 0
        val selectionEnd = 3
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)
        paragraph.layoutText()

        val expectedBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(expectedBitmap))
        val expectedPaint = Paint()
        val defaultSelectionColor = Color(0x6633B5E5)
        expectedPaint.color = defaultSelectionColor
        expectedCanvas.drawRect(
            Rect(
                left = 0f,
                top = 0f,
                right = fontSize * (selectionEnd - selectionStart),
                bottom = fontSize
            ),
            expectedPaint)

        val actualBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        paragraph.paintSelection(
            actualCanvas,
            TextSelection(selectionStart, selectionEnd)
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }

    @Test
    fun testSelectionPaint_paint_with_default_color_bidi() {
        // Setup test.
        val textLTR = "Hello"
        // From right to left: שלום
        val textRTL = "\u05e9\u05dc\u05d5\u05dd"
        val text = textLTR + textRTL
        val selectionLTRStart = 2
        val selectionRTLEnd = 2
        val fontSize = 20.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.Ltr)
        paragraph.layoutText()

        val expectedBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(expectedBitmap))
        val expectedPaint = Paint()
        val defaultSelectionColor = Color(0x6633B5E5)
        expectedPaint.color = defaultSelectionColor
        // Select "llo".
        expectedCanvas.drawRect(
            Rect(
                left = fontSize * selectionLTRStart,
                top = 0f,
                right = textLTR.length * fontSize,
                bottom = fontSize
            ),
            expectedPaint)
        // Select "של"
        expectedCanvas.drawRect(
            Rect(
                left = (textLTR.length + textRTL.length - selectionRTLEnd) * fontSize,
                top = 0f,
                right = (textLTR.length + textRTL.length) * fontSize,
                bottom = fontSize
            ),
            expectedPaint)

        val actualBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        paragraph.paintSelection(
            actualCanvas,
            TextSelection(selectionLTRStart, textLTR.length + selectionRTLEnd)
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }

    @Test
    fun testSelectionPaint_paint_with_customized_color() {
        // Setup test.
        val selectionStart = 0
        val selectionEnd = 3
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val selectionColor = Color(0x66AABB33)
        val paragraph = RenderParagraph(
            text = textSpan,
            textDirection = TextDirection.Ltr,
            selectionColor = selectionColor)
        paragraph.layoutText()

        val expectedBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(expectedBitmap))
        val expectedPaint = Paint()
        expectedPaint.color = selectionColor
        expectedCanvas.drawRect(
            Rect(
                left = 0f,
                top = 0f,
                right = fontSize * (selectionEnd - selectionStart),
                bottom = fontSize
            ),
            expectedPaint)

        val actualBitmap = Bitmap.createBitmap(
            ceil(paragraph.width).toInt(),
            ceil(paragraph.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = androidx.ui.painting.Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        paragraph.paintSelection(
            actualCanvas,
            TextSelection(selectionStart, selectionEnd)
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }
}
