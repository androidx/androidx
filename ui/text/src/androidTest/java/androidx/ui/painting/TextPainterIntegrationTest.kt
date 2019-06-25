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

import android.graphics.Bitmap
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Constraints
import androidx.ui.core.ipx
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.graphics.Color
import androidx.ui.matchers.equalToBitmap
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class TextPainterIntegrationTest {

    private lateinit var fontFamily: FontFamily

    @Before
    fun setup() {
        fontFamily = BASIC_MEASURE_FONT.asFontFamily()
        fontFamily.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun preferredLineHeight_style_set() {
        val fontSize = 20.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textPainter = TextPainter(style = textStyle)
        val preferredHeight = textPainter.preferredLineHeight

        assertThat(preferredHeight).isEqualTo(fontSize)
    }

    // TODO(Migration/qqd): The default font size should be 14.0 but it returns 15.0. Need further
    // investigation. It is being changed in the native level, and probably related to the font.
//    @Test
//    fun preferredLineHeight_style_not_set() {
//        val defaultTextStyle = TextStyle(fontFamily = fontFamily)
//        val textPainter = TextPainter(style = defaultTextStyle)
//
//        val prefferedHeight = textPainter.preferredLineHeight
//
//        assertThat(prefferedHeight).isEqualTo(14.0)
//    }

    @Test
    fun minIntrinsicWidth_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints())

        assertThat(textPainter.minIntrinsicWidth).isEqualTo(0.0f)
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints())

        assertThat(textPainter.maxIntrinsicWidth).isEqualTo(fontSize * text.length)
    }

    @Test
    fun width_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints(0.ipx, 200.ipx))

        assertThat(textPainter.width).isEqualTo(fontSize * text.length)
    }

    @Test
    fun width_getter_with_small_width() {
        val fontSize = 20.0f
        val text = "Hello"
        val width = 80.ipx
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints(maxWidth = width))

        assertThat(textPainter.width).isEqualTo(width.value.toFloat())
    }

    @Test
    fun height_getter() {
        val fontSize = 20.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val text = "hello"
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints())

        assertThat(textPainter.height).isEqualTo(fontSize)
    }

    @Test
    fun size_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        textPainter.layout(Constraints())

        assertThat(textPainter.size)
            .isEqualTo(Size(width = fontSize * text.length, height = fontSize))
    }

    @Test
    fun didExceedMaxLines_exceed() {
        var text = ""
        for (i in 1..50) text += " Hello"
        val annotatedString = AnnotatedString(text = text)
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl),
            maxLines = 2
        )

        textPainter.layout(Constraints(0.ipx, 200.ipx))

        assertThat(textPainter.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_not_exceed() {
        val text = "Hello"
        val annotatedString = AnnotatedString(text = text)
        val textPainter = TextPainter(text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl),
            maxLines = 2
        )

        textPainter.layout(Constraints(0.ipx, 200.ipx))

        assertThat(textPainter.didExceedMaxLines).isFalse()
    }

    @Test
    fun layout_build_paragraph() {
        val textPainter = TextPainter(
            text = AnnotatedString(text = "Hello"),
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )

        textPainter.layout(Constraints(0.ipx, 20.ipx))

        assertThat(textPainter.paragraph).isNotNull()
    }

    @Test
    fun getPositionForOffset_First_Character() {
        val fontSize = 20.0f
        val text = "Hello"
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(
                    TextStyle(fontSize = fontSize, fontFamily = fontFamily),
                    0,
                    text.length
                )
            )
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints())

        val selection = textPainter.getPositionForOffset(Offset(dx = 0f, dy = 0f))

        assertThat(selection.offset).isEqualTo(0)
    }

    @Test
    fun getPositionForOffset_other_Character() {
        val fontSize = 20.0f
        val characterIndex = 2 // Start from 0.
        val text = "Hello"
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(
                    TextStyle(fontSize = fontSize, fontFamily = fontFamily),
                    0,
                    text.length
                )
            )
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints())

        val selection = textPainter.getPositionForOffset(
            offset = Offset(dx = fontSize * characterIndex + 1f, dy = 0f)
        )

        assertThat(selection.offset).isEqualTo(characterIndex)
    }

    @Test
    fun hasOverflowShaderFalse() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )

        textPainter.layout(Constraints())

        assertThat(textPainter.hasVisualOverflow).isFalse()
    }

    @Test
    fun hasOverflowShaderFadeHorizontallyTrue() {
        val fontSize = 20.0f
        var text = ""
        for (i in 1..15) {
            text = text + "Hello World"
        }
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr),
            overflow = TextOverflow.Fade,
            softWrap = false,
            maxLines = 1
        )

        textPainter.layout(Constraints(maxWidth = 100.ipx))

        assertThat(textPainter.hasVisualOverflow).isTrue()
    }

    @Test
    fun hasOverflowShaderFadeVerticallyTrue() {
        val fontSize = 20.0f
        var text = ""
        for (i in 1..30) {
            text = text + "Hello World"
        }
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr),
            overflow = TextOverflow.Fade,
            maxLines = 2
        )

        textPainter.layout(Constraints(maxWidth = 100.ipx))

        assertThat(textPainter.hasVisualOverflow).isTrue()
    }

    @Test
    fun testBackgroundPaint_paint_wrap_multiLines() {
        // Setup test.
        val fontSize = 20.0f
        val text = "HelloHello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints(maxWidth = 120.ipx))

        val expectedBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
        val expectedPaint = Paint()
        val defaultSelectionColor = Color(0x6633B5E5)
        expectedPaint.color = defaultSelectionColor

        val firstLineLeft = textPainter.paragraph?.getLineLeft(0)
        val secondLineLeft = textPainter.paragraph?.getLineLeft(1)
        val firstLineRight = textPainter.paragraph?.getLineRight(0)
        val secondLineRight = textPainter.paragraph?.getLineRight(1)
        expectedCanvas.drawRect(
            Rect(firstLineLeft!!, 0f, firstLineRight!!, fontSize),
            expectedPaint
        )
        expectedCanvas.drawRect(
            Rect(
                secondLineLeft!!,
                fontSize,
                secondLineRight!!,
                textPainter.paragraph!!.height
            ),
            expectedPaint
        )

        val actualBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        // Select all.
        textPainter.paintBackground(
            start = 0,
            end = text.length,
            color = defaultSelectionColor,
            canvas = actualCanvas,
            offset = Offset.zero
        )

        // Assert.
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color() {
        // Setup test.
        val selectionStart = 0
        val selectionEnd = 3
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints())

        val expectedBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
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
            expectedPaint
        )

        val actualBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        textPainter.paintBackground(
            start = selectionStart,
            end = selectionEnd,
            color = defaultSelectionColor,
            canvas = actualCanvas,
            offset = Offset.zero
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color_bidi() {
        // Setup test.
        val textLTR = "Hello"
        // From right to left: שלום
        val textRTL = "\u05e9\u05dc\u05d5\u05dd"
        val text = textLTR + textRTL
        val selectionLTRStart = 2
        val selectionRTLEnd = 2
        val fontSize = 20.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val textPainter = TextPainter(text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints())

        val expectedBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
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
            expectedPaint
        )

        // Select "של"
        expectedCanvas.drawRect(
            Rect(
                left = (textLTR.length + textRTL.length - selectionRTLEnd) * fontSize,
                top = 0f,
                right = (textLTR.length + textRTL.length) * fontSize,
                bottom = fontSize
            ),
            expectedPaint
        )

        val actualBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        textPainter.paintBackground(
            start = selectionLTRStart,
            end = textLTR.length + selectionRTLEnd,
            color = defaultSelectionColor,
            canvas = actualCanvas,
            offset = Offset.zero
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }

    @Test
    fun testBackgroundPaint_paint_with_customized_color() {
        // Setup test.
        val selectionStart = 0
        val selectionEnd = 3
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        val selectionColor = Color(0x66AABB33)
        val textPainter = TextPainter(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )
        textPainter.layout(Constraints())

        val expectedBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
        val expectedPaint = Paint()
        expectedPaint.color = selectionColor
        expectedCanvas.drawRect(
            Rect(
                left = 0f,
                top = 0f,
                right = fontSize * (selectionEnd - selectionStart),
                bottom = fontSize
            ),
            expectedPaint
        )

        val actualBitmap = Bitmap.createBitmap(
            ceil(textPainter.width).toInt(),
            ceil(textPainter.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

        // Run.
        textPainter.paintBackground(
            start = selectionStart,
            end = selectionEnd,
            color = selectionColor,
            canvas = actualCanvas,
            offset = Offset.zero
        )

        // Assert
        Assert.assertThat(actualBitmap, equalToBitmap(expectedBitmap))
    }
}
