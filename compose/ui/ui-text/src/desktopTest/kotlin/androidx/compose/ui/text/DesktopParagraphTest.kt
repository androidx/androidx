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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.FloatSubject
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
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
                assertThat(box.left).isEqualToWithTolerance(i * fontSizeInPx)
                assertThat(box.right).isEqualToWithTolerance((i + 1) * fontSizeInPx)
                assertThat(box.top).isZero()
                assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_lineHeight() {
        with(defaultDensity) {
            val text = "abc\ndef"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            for (lineHeightMultiplier in listOf(
                // 0f, // TODO https://youtrack.jetbrains.com/issue/CMP-7963
                0.1f, 0.5f, 1f, 1.5f, 2f, 10f,
            )) {
                val paragraph = simpleParagraph(
                    text = text,
                    style = TextStyle(
                        fontSize = fontSize,
                        lineHeight = fontSize * lineHeightMultiplier,
                        lineHeightStyle = LineHeightStyle(
                            // TODO Other alignments produce weird results in case of lineHeightMultiplier < 1f
                            alignment = Alignment.Top,
                            trim = Trim.None
                        )
                    )
                )

                val lineStart = paragraph.getLineStart(1)
                val lineEnd = paragraph.getLineEnd(1)
                for (i in lineStart..lineEnd - 1) {
                    val position = i - lineStart
                    val box = paragraph.getBoundingBox(i)
                    assertThat(box.left).isEqualToWithTolerance(position * fontSizeInPx)
                    assertThat(box.right).isEqualToWithTolerance((position + 1) * fontSizeInPx)

                    val lineHeightInPx = fontSizeInPx * lineHeightMultiplier
                    assertThat(box.top).isEqualToWithTolerance(lineHeightInPx)
                    assertThat(box.bottom).isEqualToWithTolerance(lineHeightInPx + maxOf(fontSizeInPx, lineHeightInPx))
                }
            }
        }
    }

    @Test
    fun `test cursor position of LTR text in LTR and RTL paragraphs`() {
        // LTR paragraph
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            repeat(4) {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((fontSizeInPx * it).roundToInt())
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo(paragraph.getCursorRect(it).right.roundToInt())
            }
        }

        // RTL paragraph
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val widthInPx = fontSizeInPx * 10
            val paragraph = simpleParagraph(
                text = text,
                width = widthInPx,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val leftX = paragraph.getLineLeft(0)
            assertThat(leftX.roundToInt()).isEqualTo((widthInPx - 3 * fontSizeInPx).roundToInt())
            repeat(4) {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((leftX + fontSizeInPx * it).roundToInt())
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo(paragraph.getCursorRect(it).right.roundToInt())
            }
        }
    }

    @Test
    fun `test cursor position of RTL text in LTR and RTL paragraphs`() {
        // LTR paragraph
        with(defaultDensity) {
            val text = "אסד"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            repeat(4) {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo(((3 - it) * fontSizeInPx).roundToInt())
            }
        }

        // RTL paragraph
        with(defaultDensity) {
            val text = "אסד"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val widthInPx = fontSizeInPx * 10
            val paragraph = simpleParagraph(
                text = text,
                width = widthInPx,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val leftX = paragraph.getLineLeft(0)
            assertThat(leftX.roundToInt()).isEqualTo((widthInPx - 3 * fontSizeInPx).roundToInt())
            repeat(4) {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((leftX + (3 - it) * fontSizeInPx).roundToInt())
            }
        }
    }

    @Test
    fun `test cursor position of BiDi text in LTR and RTL paragraphs`() {
        // LTR paragraph
        with(defaultDensity) {
            val text = "asd אסד"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            val rightX = paragraph.getLineRight(0)
            (0..3).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((it * fontSizeInPx).roundToInt())
            }
            (4..7).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((rightX - (it - 4) * fontSizeInPx).roundToInt())
            }
        }
        with(defaultDensity) {
            val text = "אסד asd"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            val leftX = paragraph.getLineLeft(0)
            (0..3).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((leftX + (3 - it) * fontSizeInPx).roundToInt())
            }
            (7 downTo 4).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((it * fontSizeInPx).roundToInt())
            }
        }

        // RTL paragraph
        with(defaultDensity) {
            val text = "asd אסד"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                width = 10 * fontSizeInPx,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val rightX = paragraph.getLineRight(0)
            (3 downTo 0).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((rightX - (3 - it) * fontSizeInPx).roundToInt())
            }
            (4..7).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((rightX -  it * fontSizeInPx).roundToInt())
            }
        }
        with(defaultDensity) {
            val text = "אסד asd"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                width = 10 * fontSizeInPx,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val leftX = paragraph.getLineLeft(0)
            val rightX = paragraph.getLineRight(0)
            (0..3).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((rightX - it * fontSizeInPx).roundToInt())
            }
            (4..7).forEach {
                assertThat(paragraph.getCursorRect(it).left.roundToInt())
                    .isEqualTo((leftX + (it - 4) * fontSizeInPx).roundToInt())
            }
        }
    }

    @Test
    fun `test cursor position in RTl text when clicking on an empty line`() {
        // Tests if (leftX == rightX) in getOffsetForPosition
        with(defaultDensity) {
            val text = "asd\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                width = 10 * fontSizeInPx,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val leftX = paragraph.getLineLeft(0)
            val rightX = paragraph.getLineRight(0)

            val clickX = (leftX + rightX) / 2f
            val secondLineY = (paragraph.getLineBottom(1) + paragraph.getLineTop(1)) / 2f

            assertThat(paragraph.getOffsetForPosition(Offset(clickX, secondLineY))).isEqualTo(4)
            assertThat(paragraph.getOffsetForPosition(Offset(leftX - fontSizeInPx, secondLineY))).isEqualTo(4)
            assertThat(paragraph.getOffsetForPosition(Offset(rightX + fontSizeInPx, secondLineY))).isEqualTo(4)
        }
    }

    @Test
    fun `test getOffsetByPosition`() {
        // LTR
        with(defaultDensity) {
            val text = " abc \ndef ghi"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            val firstLineY = (paragraph.getLineBottom(0) + paragraph.getLineTop(0)) / 2f
            (0..5).forEach {
                assertThat(paragraph.getOffsetForPosition(Offset(x = fontSizeInPx * it, y = firstLineY)))
                    .isEqualTo(it)
            }
            assertThat(paragraph.getOffsetForPosition(Offset(x = 1000f, y = firstLineY))).isEqualTo(5)
            assertThat(paragraph.getOffsetForPosition(Offset(x = -100f, y = firstLineY))).isEqualTo(0)

            val secondLineY = (paragraph.getLineBottom(1) + paragraph.getLineTop(1)) / 2f
            assertThat(paragraph.getOffsetForPosition(Offset(x = 1000f, y = secondLineY))).isEqualTo(13)
            assertThat(paragraph.getOffsetForPosition(Offset(x = -100f, y = secondLineY))).isEqualTo(6)

            (6..13).forEach {
                assertThat(paragraph.getOffsetForPosition(Offset(x = fontSizeInPx * (it - 6), y = secondLineY))).isEqualTo(it)
            }
        }

        // RTL
        with(defaultDensity) {
            val text = " אסד \nקשע תטו"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = 10 * fontSizeInPx
            val paragraph = simpleParagraph(
                text = text,
                width = width,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl)
            )

            val firstLineY = (paragraph.getLineBottom(0) + paragraph.getLineTop(0)) / 2f
            (0..5).forEach {
                assertThat(paragraph.getOffsetForPosition(Offset(x = width - fontSizeInPx * it, y = firstLineY)))
                    .isEqualTo(it)
            }
            assertThat(paragraph.getOffsetForPosition(Offset(x = width + fontSizeInPx, y = firstLineY))).isEqualTo(0)
            assertThat(paragraph.getOffsetForPosition(Offset(x = 0f, y = firstLineY))).isEqualTo(5)

            val secondLineY = 20f + (paragraph.getLineBottom(1) + paragraph.getLineTop(1)) / 2f
            assertThat(paragraph.getOffsetForPosition(Offset(x = width - 1f, y = secondLineY))).isEqualTo(6)
            assertThat(paragraph.getOffsetForPosition(Offset(x = -100f, y = secondLineY))).isEqualTo(13)

            (7..13).forEach {
                assertThat(paragraph.getOffsetForPosition(Offset(x = width - (it - 6) * fontSizeInPx, y = secondLineY))).isEqualTo(it)
            }
        }
    }

    @Test
    fun `test cursor position on line-break`() {
        with(defaultDensity) {
            val text = "abc abc  abc abc abc abc  abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = 8 * fontSizeInPx
            val paragraph = simpleParagraph(
                width = width,
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            assertThat(paragraph.lineCount).isEqualTo(4)
            val y = fontSizeInPx / 2f

            // first line has 2 spaces in the end
            assertThat(
                paragraph.getOffsetForPosition(Offset(x = width, y = y))
            ).isEqualTo(8)

            // seconds line has 1 space in the end
            assertThat(
                paragraph.getOffsetForPosition(Offset(x = width, y = y + fontSizeInPx))
            ).isEqualTo(16)

            // 3rd line has 2 spaces in the end
            assertThat(
                paragraph.getOffsetForPosition(Offset(x = width, y = y + 2 * fontSizeInPx))
            ).isEqualTo(25)

            // 4th line has no spaces
            assertThat(
                paragraph.getOffsetForPosition(Offset(x = width, y = y + 3 * fontSizeInPx))
            ).isEqualTo(29)
        }
    }

    @Test
    fun `test cursor position in a line with many space in the end`() {
        with(defaultDensity) {
            // 1st: 4 spaces, 2nd: 0 spaces, 3rd: 1 space, 4th: empty line
            val text = "abc    \ndef\ngh \n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = 20 * fontSizeInPx
            val paragraph = simpleParagraph(
                width = width,
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            val y = fontSizeInPx / 2f
            assertThat(paragraph.getOffsetForPosition(Offset(x = width, y = y))).isEqualTo(7)
            assertThat(paragraph.getOffsetForPosition(Offset(x = width, y = y + fontSizeInPx))).isEqualTo(11)
            assertThat(paragraph.getOffsetForPosition(Offset(x = width, y = y + 2 * fontSizeInPx))).isEqualTo(15)
            assertThat(paragraph.getOffsetForPosition(Offset(x = width, y = y + 3 * fontSizeInPx))).isEqualTo(16)
        }
    }

    @Test
    fun `test cursor position in a line with many space in the start`() {
        with(defaultDensity) {
            // 1st: 4 spaces, 2nd: 0 spaces, 3rd: 1 space, 4th: empty line
            val text = "    abc\ndef\n gh\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = 20 * fontSizeInPx
            val paragraph = simpleParagraph(
                width = width,
                text = text,
                style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr)
            )

            val y = fontSizeInPx / 2f
            assertThat(paragraph.getOffsetForPosition(Offset(x = 0f, y = y))).isEqualTo(0)
            assertThat(paragraph.getOffsetForPosition(Offset(x = 0f, y = y + fontSizeInPx))).isEqualTo(8)
            assertThat(paragraph.getOffsetForPosition(Offset(x = 0f, y = y + 2 * fontSizeInPx))).isEqualTo(12)
            assertThat(paragraph.getOffsetForPosition(Offset(x = 0f, y = y + 3 * fontSizeInPx))).isEqualTo(16)
        }
    }

    @Test
    fun getBoundingBox_multicodepoints() {
        val text = "h\uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = 50.sp)
        )

        assertThat(paragraph.getBoundingBox(1).left)
            .isEqualTo(paragraph.getBoundingBox(0).right)

        assertThat(paragraph.getBoundingBox(5))
            .isEqualTo(paragraph.getBoundingBox(1))
    }

    @Test
    fun getLineForOffset() {
        val text = "ab\na"
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = 50.sp)
        )

        assertThat(paragraph.getLineForOffset(2))
            .isEqualTo(0)
        assertThat(paragraph.getLineForOffset(3))
            .isEqualTo(1)
    }

    @Test
    fun getLineForOffset_empty() {
        val text = ""
        val paragraph = simpleParagraph(
            text = text,
            style = TextStyle(fontSize = 50.sp)
        )

        assertThat(paragraph.getLineForOffset(0))
            .isEqualTo(0)
    }

    @Test
    fun getLineForOffset_withOnlyPlaceholder() {
        val text = buildAnnotatedString {
            pushStringAnnotation("test", "a")
            append("\uFFFD")
            pop()
        }

        val intrinsics = ParagraphIntrinsics(
            text = text.text,
            style = TextStyle(
                fontSize = 50.sp,
                fontFamily = fontFamilyMeasureFont,
            ),
            annotations = listOf(
                AnnotatedString.Range(
                    item = StringAnnotation("a"),
                    start = 0,
                    end = 1,
                    tag = "test",
                )
            ),
            placeholders = listOf(
                AnnotatedString.Range(
                    item = Placeholder(40.0.sp, 16.0.sp, PlaceholderVerticalAlign.Center),
                    start = 0,
                    end = 1,
                    tag = "test",
                )
            ),
            density = defaultDensity,
            fontFamilyResolver = fontFamilyResolver,
        )
        val paragraph = simpleParagraph(intrinsics)
        assertThat(paragraph.getLineForOffset(0))
            .isEqualTo(0)
    }

    @Test
    fun getLineEnd() {
        with(defaultDensity) {
            val text = ""
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(0)
        }
        with(defaultDensity) {
            val text = "ab\n\nc"
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(2)
            assertThat(paragraph.getLineEnd(1, true))
                .isEqualTo(3)
            assertThat(paragraph.getLineEnd(2, true))
                .isEqualTo(5)
        }
        with(defaultDensity) {
            val text = "ab\n"
            val paragraph = simpleParagraph(
                text = text,
                style = TextStyle(fontSize = 50.sp)
            )

            assertThat(paragraph.getLineEnd(0, true))
                .isEqualTo(2)
            assertThat(paragraph.getLineEnd(1, true))
                .isEqualTo(3)
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
        assertThat(singleSpaceStartResult.start).isEqualTo(text.indexOf('a'))
        assertThat(singleSpaceStartResult.end).isEqualTo(text.indexOf('b') + 1)

        val singleSpaceEndResult = paragraph.getWordBoundary(text.indexOf('c'))

        assertThat(singleSpaceEndResult.start).isEqualTo(text.indexOf('c'))
        assertThat(singleSpaceEndResult.end).isEqualTo(text.indexOf('d') + 1)

        val doubleSpaceResult = paragraph.getWordBoundary(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.start).isEqualTo(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.end).isEqualTo(text.indexOf('d') + 2)
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

        assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph2.paint()
        assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph1.paint()
        assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        assertThat(paragraph2.testOffset()).isEqualTo(offset2)

        paragraph2.paint()
        assertThat(paragraph1.testOffset()).isEqualTo(offset1)
        assertThat(paragraph2.testOffset()).isEqualTo(offset2)
    }

    @Test
    fun `line heights`() {
        val paragraph = simpleParagraph(
            text = "aaa\n\naaa\n\n\naaa\n   \naaa",
            style = TextStyle(fontSize = 50.sp)
        )
        val firstLineHeight = paragraph.getLineHeight(0)

        for (i in 1 until paragraph.lineCount) {
            assertThat(paragraph.getLineHeight(i)).isEqualTo(firstLineHeight)
        }
    }

    @Test
    fun applies_baseline_shift_to_spans() {
        val helper = buildAnnotatedString {
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 16.sp)) {
                append("a")
            }
            append("\nb")
            withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 16.sp)) {
                append("c")
            }
            append("\nd")
            withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 16.sp)) {
                append("e")
            }
            withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 16.sp)) {
                append("f")
            }
        }
        val textStyle = TextStyle(
            fontFamily = fontFamilyMeasureFont,
            fontSize = 16.sp
        )
        val paragraph = simpleParagraph(text = helper.text, spanStyles = helper.spanStyles, style = textStyle)

        val a = paragraph.getBoundingBox(helper.text.indexOf("a"))
        val b = paragraph.getBoundingBox(helper.text.indexOf("b"))
        val c = paragraph.getBoundingBox(helper.text.indexOf("c"))
        val d = paragraph.getBoundingBox(helper.text.indexOf("d"))
        val e = paragraph.getBoundingBox(helper.text.indexOf("e"))
        val f = paragraph.getBoundingBox(helper.text.indexOf("f"))

        assertThat(a.top).isLessThan(b.top)
        assertThat(b.top).isLessThan(c.top)
        assertThat(e.top).isLessThan(d.top)
        assertThat(d.top).isLessThan(f.top)

        assertThat(a.bottom).isLessThan(b.bottom)
        assertThat(b.bottom).isLessThan(c.bottom)
        assertThat(e.bottom).isLessThan(d.bottom)
        assertThat(d.bottom).isLessThan(f.bottom)
    }

    @Test
    fun `applies text indent for paragraph`() {
        fun measureLines(alignment: TextAlign, direction: TextDirection): List<Int> {
            val paragraph = simpleParagraph(
                text = "sample\ntext",
                style = TextStyle(
                    fontSize = 20.sp,
                    textIndent = TextIndent(50.sp, 20.sp),
                    textAlign = alignment,
                    textDirection = direction,
                )
            )
            return listOf(
                paragraph.getLineLeft(0).roundToInt(),
                paragraph.getLineRight(0).roundToInt(),
                paragraph.getLineLeft(1).roundToInt(),
                paragraph.getLineRight(1).roundToInt()
            )
        }
        assertThat(measureLines(TextAlign.Left, TextDirection.Ltr)).isEqualTo(listOf(50, 170, 20, 100))
        assertThat(measureLines(TextAlign.Center, TextDirection.Ltr)).isEqualTo(listOf(965, 1085, 970, 1050))
        assertThat(measureLines(TextAlign.Right, TextDirection.Ltr)).isEqualTo(listOf(1830, 1950, 1900, 1980))
        assertThat(measureLines(TextAlign.Justify, TextDirection.Ltr)).isEqualTo(listOf(50, 170, 20, 100))
        assertThat(measureLines(TextAlign.Left, TextDirection.Rtl)).isEqualTo(listOf(50, 170, 20, 100))
        assertThat(measureLines(TextAlign.Center, TextDirection.Rtl)).isEqualTo(listOf(915, 1035, 950, 1030))
        assertThat(measureLines(TextAlign.Right, TextDirection.Rtl)).isEqualTo(listOf(1830, 1950, 1900, 1980))
        assertThat(measureLines(TextAlign.Justify, TextDirection.Rtl)).isEqualTo(listOf(1830, 1950, 1900, 1980))
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
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
        // TODO https://youtrack.jetbrains.com/issue/CMP-7151
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
        width: Float = 2000f
    ): Paragraph {
        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            maxLines = maxLines,
        )
    }
}

private fun FloatSubject.isEqualToWithTolerance(expected: Float, tolerance: Float = 0.001f) =
    isWithin(tolerance).of(expected)
