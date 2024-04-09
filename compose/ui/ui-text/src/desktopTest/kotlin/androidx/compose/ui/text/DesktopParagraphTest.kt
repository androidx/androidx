/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopParagraphTest {
    @get:Rule
    val rule = createComposeRule()

    private val fontFamilyResolver = createFontFamilyResolver()
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyMeasureFont =
        FontFamily(
            Font(
                "font/sample_font.ttf",
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )

    @Test
    @Ignore("b/271123970 Fails in AOSP. Will be fixed after upstreaming Compose for Desktop")
    fun getBoundingBox_basic() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize)
            )

            for (i in 0..text.length - 1) {
                val box = paragraph.getBoundingBox(i)
                Truth.assertThat(box.left).isEqualTo(i * fontSizeInPx)
                Truth.assertThat(box.right).isEqualTo((i + 1) * fontSizeInPx)
                Truth.assertThat(box.top).isZero()
                Truth.assertThat(box.bottom).isEqualTo(fontSizeInPx + 10)
            }
        }
    }

    @Test
    @Ignore("b/271123970 Fails in AOSP. Will be fixed after upstreaming Compose for Desktop")
    fun getBoundingBox_multicodepoints() {
        assumeTrue(isLinux)
        with(defaultDensity) {
            val text = "h\uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            Truth.assertThat(paragraph.getBoundingBox(0))
                .isEqualTo(Rect(0f, 0f, fontSizeInPx, 60f))

            Truth.assertThat(paragraph.getBoundingBox(1))
                .isEqualTo(Rect(fontSizeInPx, 0f, fontSizeInPx * 2.5f, 60f))

            Truth.assertThat(paragraph.getBoundingBox(5))
                .isEqualTo(Rect(fontSizeInPx, 0f, fontSizeInPx * 2.5f, 60f))
        }
    }

    @Test
    fun getLineForOffset() {
        val text = "ab\na"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = 50.sp)
        )

        Truth.assertThat(paragraph.getLineForOffset(2))
            .isEqualTo(0)
        Truth.assertThat(paragraph.getLineForOffset(3))
            .isEqualTo(1)
    }

    @Test
    fun getLineEnd() {
        with(defaultDensity) {
            val text = ""
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            Truth.assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(0)
        }
        with(defaultDensity) {
            val text = "ab\n\nc"
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            Truth.assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(2)
            Truth.assertThat(paragraph.getLineEnd(1, true))
                .isEqualTo(3)
            Truth.assertThat(paragraph.getLineEnd(2, true))
                .isEqualTo(5)
        }
        with(defaultDensity) {
            val text = "ab\n"
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            Truth.assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(2)
            Truth.assertThat(paragraph.getLineEnd(1, true))
                .isEqualTo(3)
        }
    }

    @Test
    fun getLineBaseline() {
        val text = "abc\nabc\nabc"
        val paragraph = simpleParagraph(text = text)

        Truth.assertThat(paragraph.getLineBaseline(0)).isEqualTo(paragraph.firstBaseline)
        Truth.assertThat(paragraph.getLineBaseline(2)).isEqualTo(paragraph.lastBaseline)
    }

    @Test
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = width
            )

            for (i in 0..ltrText.length) {
                Truth.assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in 1 until rtlText.length) {
                Truth.assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualTo(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(
                    fontSize = fontSize,
                    textDirection = TextDirection.Ltr
                ),
                width = width
            )

            for (i in ltrText.indices) {
                Truth.assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualTo(fontSizeInPx * i)
            }

            for (i in rtlText.indices) {
                Truth.assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualTo(width - fontSizeInPx * i)
            }

            Truth.assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualTo(width - rtlText.length * fontSizeInPx)
        }
    }

    @Test
    fun getWordBoundary_spaces() {
        val text = "ab cd  e"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            )
        )

        val singleSpaceStartResult = paragraph.getWordBoundary(text.indexOf('b') + 1)
        Truth.assertThat(singleSpaceStartResult.start).isEqualTo(text.indexOf('a'))
        Truth.assertThat(singleSpaceStartResult.end).isEqualTo(text.indexOf('b') + 1)

        val singleSpaceEndResult = paragraph.getWordBoundary(text.indexOf('c'))

        Truth.assertThat(singleSpaceEndResult.start).isEqualTo(text.indexOf('c'))
        Truth.assertThat(singleSpaceEndResult.end).isEqualTo(text.indexOf('d') + 1)

        val doubleSpaceResult = paragraph.getWordBoundary(text.indexOf('d') + 2)
        Truth.assertThat(doubleSpaceResult.start).isEqualTo(text.indexOf('d') + 2)
        Truth.assertThat(doubleSpaceResult.end).isEqualTo(text.indexOf('d') + 2)
    }

    @Test
    fun two_paragraphs_use_common_intrinsics() {
        fun Paragraph.testOffset() = getOffsetForPosition(Offset(0f, 100000f))
        fun Paragraph.paint() = paint(Canvas(ImageBitmap(100, 100)))

        val intrinsics = simpleIntrinsics((1..1000).joinToString(" "))

        val paragraph1 = simpleParagraph(intrinsics, width = 100f)
        val offset1 = paragraph1.testOffset()

        val paragraph2 = simpleParagraph(intrinsics, width = 100000f)
        val offset2 = paragraph2.testOffset()

        Truth.assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        Truth.assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph2.paint()
        Truth.assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        Truth.assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph1.paint()
        Truth.assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        Truth.assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph2.paint()
        Truth.assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        Truth.assertThat(paragraph2.testOffset()).isEqualTo(offset2)
    }

    @Test
    fun `line heights`() {
        val paragraph = simpleParagraph(
            text = "aaa\n\naaa\n\n\naaa\n   \naaa",
            style = TextStyle(fontSize = 50.sp)
        )
        val firstLineHeight = paragraph.getLineHeight(0)

        for (i in 1 until paragraph.lineCount) {
            Truth.assertThat(paragraph.getLineHeight(i)).isEqualTo(firstLineHeight)
        }
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        density: Density? = null,
        width: Float = 2000f
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont
            ).merge(style),
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = density ?: defaultDensity,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    private fun simpleIntrinsics(
        text: String = "",
        style: TextStyle? = null,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        density: Density? = null
    ): ParagraphIntrinsics {
        return ParagraphIntrinsics(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont
            ).merge(style),
            density = density ?: defaultDensity,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    private fun simpleParagraph(
        intrinsics: ParagraphIntrinsics,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        width: Float = 2000f
    ): Paragraph {
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt()),
        )
    }
}
