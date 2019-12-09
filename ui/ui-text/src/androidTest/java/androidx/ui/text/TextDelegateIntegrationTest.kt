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

package androidx.ui.text

import android.graphics.Bitmap
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.matchers.assertThat
import androidx.ui.text.matchers.isZero
import androidx.ui.text.style.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class TextDelegateIntegrationTest {

    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun minIntrinsicWidth_getter() {
        withDensity(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layoutIntrinsics()

            assertThat(textDelegate.minIntrinsicWidth)
                .isEqualTo((fontSize.toPx().value * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        withDensity(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )

            textDelegate.layoutIntrinsics()

            assertThat(textDelegate.maxIntrinsicWidth)
                .isEqualTo((fontSize.toPx().value * text.length).toIntPx())
        }
    }

    @Test
    fun width_getter() {
        withDensity(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )

            textDelegate.layout(Constraints(0.ipx, 200.ipx))

            assertThat(textDelegate.width).isEqualTo(
                (fontSize.toPx().value * text.length).toIntPx()
            )
        }
    }

    @Test
    fun width_getter_with_small_width() {
        val text = "Hello"
        val width = 80.ipx
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        textDelegate.layout(Constraints(maxWidth = width))

        assertThat(textDelegate.width).isEqualTo(width)
    }

    @Test
    fun height_getter() {
        withDensity(density) {
            val fontSize = 20.sp
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val text = "hello"
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )

            textDelegate.layout(Constraints())

            assertThat(textDelegate.height).isEqualTo((fontSize.toPx().value).toIntPx())
        }
    }

    @Test
    fun layout_build_layoutResult() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello"),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        textDelegate.layout(Constraints(0.ipx, 20.ipx))

        assertThat(textDelegate.layoutResult).isNotNull()
    }

    @Test
    fun getPositionForOffset_First_Character() {
        val text = "Hello"
        val annotatedString = AnnotatedString(
            text,
            SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        )

        val textDelegate = TextDelegate(
            text = annotatedString,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        textDelegate.layout(Constraints())

        val selection = textDelegate.getOffsetForPosition(PxPosition.Origin)

        assertThat(selection).isZero()
    }

    @Test
    fun getPositionForOffset_other_Character() {
        withDensity(density) {
            val fontSize = 20.sp
            val characterIndex = 2 // Start from 0.
            val text = "Hello"

            val annotatedString = AnnotatedString(
                text,
                SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            )

            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layout(Constraints())

            val selection = textDelegate.getOffsetForPosition(
                position = PxPosition((fontSize.toPx().value * characterIndex + 1).px, 0.px)
            )

            assertThat(selection).isEqualTo(characterIndex)
        }
    }

    @Test
    fun hasOverflowShaderFalse() {
        val text = "Hello"
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        textDelegate.layout(Constraints())

        assertThat(textDelegate.layoutResult?.hasVisualOverflow).isFalse()

        // paint should not throw exception
        textDelegate.paint(Canvas(android.graphics.Canvas()))
    }

    @Test
    fun hasOverflowShaderFadeHorizontallyTrue() {
        val text = "Hello World".repeat(15)
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)

        val textDelegate = TextDelegate(
            text = annotatedString,
            overflow = TextOverflow.Fade,
            softWrap = false,
            maxLines = 1,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        textDelegate.layout(Constraints(maxWidth = 100.ipx))

        assertThat(textDelegate.layoutResult?.hasVisualOverflow).isTrue()

        // paint should not throw exception
        textDelegate.paint(Canvas(android.graphics.Canvas()))
    }

    @Test
    fun hasOverflowShaderFadeVerticallyTrue() {
        val text = "Hello World".repeat(30)
        val spanStyle = SpanStyle(fontSize = 20.sp, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate = TextDelegate(
            text = annotatedString,
            overflow = TextOverflow.Fade,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        textDelegate.layout(Constraints(maxWidth = 100.ipx))

        assertThat(textDelegate.layoutResult?.hasVisualOverflow).isTrue()

        // paint should not throw exception
        textDelegate.paint(Canvas(android.graphics.Canvas()))
    }

    @Test
    fun testBackgroundPaint_paint_wrap_multiLines() {
        withDensity(density) {
            // Setup test.
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val text = "HelloHello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layout(Constraints(maxWidth = 120.ipx))

            val expectedBitmap = textDelegate.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            expectedPaint.color = defaultSelectionColor

            val firstLineLeft = textDelegate.layoutResult?.multiParagraph?.getLineLeft(0)
            val secondLineLeft = textDelegate.layoutResult?.multiParagraph?.getLineLeft(1)
            val firstLineRight = textDelegate.layoutResult?.multiParagraph?.getLineRight(0)
            val secondLineRight = textDelegate.layoutResult?.multiParagraph?.getLineRight(1)
            expectedCanvas.drawRect(
                Rect(firstLineLeft!!, 0f, firstLineRight!!, fontSizeInPx),
                expectedPaint
            )
            expectedCanvas.drawRect(
                Rect(
                    secondLineLeft!!,
                    fontSizeInPx,
                    secondLineRight!!,
                    textDelegate.layoutResult!!.multiParagraph.height
                ),
                expectedPaint
            )

            val actualBitmap = textDelegate.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            // Select all.
            textDelegate.paintBackground(
                start = 0,
                end = text.length,
                color = defaultSelectionColor,
                canvas = actualCanvas
            )

            // Assert.
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color() {
        withDensity(density) {
            // Setup test.
            val selectionStart = 0
            val selectionEnd = 3
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layout(Constraints())

            val expectedBitmap = textDelegate.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            expectedPaint.color = defaultSelectionColor
            expectedCanvas.drawRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fontSizeInPx * (selectionEnd - selectionStart),
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = textDelegate.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            textDelegate.paintBackground(
                start = selectionStart,
                end = selectionEnd,
                color = defaultSelectionColor,
                canvas = actualCanvas
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_default_color_bidi() {
        withDensity(density) {
            // Setup test.
            val textLTR = "Hello"
            // From right to left: שלום
            val textRTL = "\u05e9\u05dc\u05d5\u05dd"
            val text = textLTR + textRTL
            val selectionLTRStart = 2
            val selectionRTLEnd = 2
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layout(Constraints())

            val expectedBitmap = textDelegate.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            val defaultSelectionColor = Color(0x6633B5E5)
            expectedPaint.color = defaultSelectionColor
            // Select "llo".
            expectedCanvas.drawRect(
                Rect(
                    left = fontSizeInPx * selectionLTRStart,
                    top = 0f,
                    right = textLTR.length * fontSizeInPx,
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            // Select "של"
            expectedCanvas.drawRect(
                Rect(
                    left = (textLTR.length + textRTL.length - selectionRTLEnd) * fontSizeInPx,
                    top = 0f,
                    right = (textLTR.length + textRTL.length) * fontSizeInPx,
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = textDelegate.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            textDelegate.paintBackground(
                start = selectionLTRStart,
                end = textLTR.length + selectionRTLEnd,
                color = defaultSelectionColor,
                canvas = actualCanvas
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun testBackgroundPaint_paint_with_customized_color() {
        withDensity(density) {
            // Setup test.
            val selectionStart = 0
            val selectionEnd = 3
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val selectionColor = Color(0x66AABB33)
            val textDelegate = TextDelegate(
                text = annotatedString,
                density = density,
                resourceLoader = resourceLoader,
                layoutDirection = LayoutDirection.Ltr
            )
            textDelegate.layout(Constraints())

            val expectedBitmap = textDelegate.toBitmap()
            val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
            val expectedPaint = Paint()
            expectedPaint.color = selectionColor
            expectedCanvas.drawRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = fontSizeInPx * (selectionEnd - selectionStart),
                    bottom = fontSizeInPx
                ),
                expectedPaint
            )

            val actualBitmap = textDelegate.toBitmap()
            val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))

            // Run.
            textDelegate.paintBackground(
                start = selectionStart,
                end = selectionEnd,
                color = selectionColor,
                canvas = actualCanvas
            )

            // Assert
            assertThat(actualBitmap).isEqualToBitmap(expectedBitmap)
        }
    }

    @Test
    fun multiParagraphIntrinsics_isReused() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "abc"),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        // create the intrinsics object
        textDelegate.layoutIntrinsics()
        val multiParagraphIntrinsics = textDelegate.paragraphIntrinsics

        // layout should create the MultiParagraph. The final MultiParagraph is expected to use
        // the previously calculated intrinsics
        textDelegate.layout(Constraints())
        val layoutIntrinsics = textDelegate.layoutResult?.multiParagraph?.intrinsics

        // primary assertions to make sure that the objects are not null
        assertThat(layoutIntrinsics?.infoList?.get(0)).isNotNull()
        assertThat(multiParagraphIntrinsics?.infoList?.get(0)).isNotNull()

        // the intrinsics passed to multi paragraph should be the same instance
        assertThat(layoutIntrinsics).isSameInstanceAs(multiParagraphIntrinsics)
        // the ParagraphIntrinsic in the MultiParagraphIntrinsic should be the same instance
        assertThat(layoutIntrinsics?.infoList?.get(0))
            .isSameInstanceAs(multiParagraphIntrinsics?.infoList?.get(0))
    }
}

private fun TextDelegate.toBitmap() = Bitmap.createBitmap(
    width.value,
    height.value,
    Bitmap.Config.ARGB_8888
)
