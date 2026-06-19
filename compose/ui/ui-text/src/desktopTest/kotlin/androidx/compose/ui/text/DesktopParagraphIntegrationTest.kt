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
package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.kruth.FloatSubject
import androidx.kruth.Subject
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// TODO: move to commonTest once sample_font will be available outside of JVM

// Adopted tests from androidInstrumentedTest/kotlin/androidx/compose/ui/text/ParagraphIntegrationTest.kt
class DesktopParagraphIntegrationTest {
    private val fontFamilyResolver = createFontFamilyResolver()
    private val fontFamilyMeasureFont =
        FontFamily(
            Font(
                "font_desktop/sample_font.ttf",
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )
    private val defaultDensity = Density(density = 1f)
    private val ltrLocaleList = LocaleList("en")

    private fun hasEdgeLetterSpacingBugFix(): Boolean {
        val text = "a"
        val fontSize = 10.sp
        val singleLetterLetterSpacing =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, letterSpacing = 10.sp),
                width = Float.MAX_VALUE,
            )

        val singleLetterWithoutLetterSpacing =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                width = Float.MAX_VALUE,
            )

        // If the platform has a letter spacing fix, the letter spacing will not be added before and
        // after the visually left most letter and visually right most letter. Therefore, if the fix
        // is available, the letter spacing is no-op for single letter text.
        return singleLetterLetterSpacing.getLineWidth(0) ==
            singleLetterWithoutLetterSpacing.getLineWidth(0)
    }

    @Test
    fun empty_string() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val text = ""
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = 100.0f)

            assertThat(paragraph.width).isEqualTo(100.0f)

            assertThat(paragraph.height).isEqualTo(fontSizeInPx)
            // defined in sample_font
            assertThat(paragraph.firstBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
            assertThat(paragraph.lastBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
            assertThat(paragraph.maxIntrinsicWidth).isZero()
            assertThat(paragraph.minIntrinsicWidth).isZero()
        }
    }

    @Test
    fun single_line_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        // width greater than text width - 150
                        width = 200.0f,
                    )

                assertWithMessage(text).that(paragraph.width).isEqualTo(200.0f)
                assertWithMessage(text).that(paragraph.height).isEqualTo(fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
                assertWithMessage(text).that(paragraph.lastBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.length)
                assertWithMessage(text)
                    .that(paragraph.minIntrinsicWidth)
                    .isEqualTo(text.length * fontSizeInPx)
            }
        }
    }

    @Test
    fun line_break_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        // 3 chars width
                        width = 3 * fontSizeInPx,
                    )

                // 3 chars
                assertWithMessage(text).that(paragraph.width).isEqualTo(3 * fontSizeInPx)
                // 2 lines, 1 line gap
                assertWithMessage(text).that(paragraph.height).isEqualTo(2 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.lastBaseline)
                    .isEqualToWithTolerance(fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.length)
                assertWithMessage(text)
                    .that(paragraph.minIntrinsicWidth)
                    .isEqualTo(text.length * fontSizeInPx)
            }
        }
    }

    @Test
    fun newline_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        // 3 chars width
                        width = 3 * fontSizeInPx,
                    )

                // 3 chars
                assertWithMessage(text).that(paragraph.width).isEqualTo(3 * fontSizeInPx)
                // 2 lines, 1 line gap
                assertWithMessage(text).that(paragraph.height).isEqualTo(2 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.lastBaseline)
                    .isEqualToWithTolerance(fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
                assertWithMessage(text)
                    .that(paragraph.minIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
            }
        }
    }

    @Test
    fun newline_and_line_break_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        // 2 chars width
                        width = 2 * fontSizeInPx,
                    )

                // 2 chars
                assertWithMessage(text).that(paragraph.width).isEqualTo(2 * fontSizeInPx)
                // 4 lines, 3 line gaps
                assertWithMessage(text).that(paragraph.height).isEqualTo(4 * fontSizeInPx)
                // defined in sample_font
                assertWithMessage(text).that(paragraph.firstBaseline).isEqualToWithTolerance(fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.lastBaseline)
                    .isEqualToWithTolerance(3 * fontSizeInPx + fontSizeInPx * 0.8f)
                assertWithMessage(text)
                    .that(paragraph.maxIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
                assertWithMessage(text)
                    .that(paragraph.minIntrinsicWidth)
                    .isEqualTo(fontSizeInPx * text.indexOf("\n"))
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars 0, 1, 2 ...
            for (i in 0..text.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage("offset at index $i, position $position does not match")
                    .that(offset)
                    .isEqualTo(i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars .., 2, 1, 0
            for (i in 0..text.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage("offset at index $i, position $position does not match")
                    .that(offset)
                    .isEqualTo(text.length - i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_multiline() {
        with(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in 0..secondLine.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx * 1.5f))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage(
                        "offset at index $i, position $position, second line does not match"
                    )
                    .that(offset)
                    .isEqualTo(i + firstLine.length)
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl_multiline() {
        with(defaultDensity) {
            val firstLine = "\u05D0\u05D1\u05D2"
            val secondLine = "\u05D3\u05D4\u05D5"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 5, 4, 3
            for (i in 0..secondLine.length) {
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx * 1.5f))
                val offset = paragraph.getOffsetForPosition(position)
                assertWithMessage(
                        "offset at index $i, position $position, second line does not match"
                    )
                    .that(offset)
                    .isEqualTo(text.length - i)
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_width_outOfBounds() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // greater than width
            var position = Offset((fontSizeInPx * text.length * 2), (fontSizeInPx / 2))
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isEqualTo(text.length)

            // negative
            position = Offset((-1 * fontSizeInPx), (fontSizeInPx / 2))
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isEqualTo(0)
        }
    }

    @Test
    fun getOffsetForPosition_ltr_height_outOfBounds() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // greater than height
            var position = Offset((fontSizeInPx / 2), (fontSizeInPx * text.length * 2))
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isEqualTo(0)

            // negative
            position = Offset((fontSizeInPx / 2), (-1 * fontSizeInPx))
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset).isEqualTo(0)
        }
    }

    @Test
    fun getLineForVerticalPosition_ltr() {
        with(defaultDensity) {
            val text = "abcdefgh"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.sp),
                    width = layoutWidth,
                )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // test positions are 1, lineHeight+1, 2lineHeight+1, 3lineHeight + 1 which map to line
            // 0, 1, 2, 3
            for (i in 0 until paragraph.lineCount) {
                val position = i * lineHeight.sp.toPx() + 1
                val line = paragraph.getLineForVerticalPosition(position)
                assertWithMessage("Line at line index $i, position $position does not match")
                    .that(line)
                    .isEqualTo(i)
            }
        }
    }

    @Test
    fun getLineForVerticalPosition_rtl() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.sp),
                    width = layoutWidth,
                )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // test positions are 1, lineHeight+1, 2lineHeight+1, 3lineHeight + 1 which map to line
            // 0, 1, 2, 3
            for (i in 0 until paragraph.lineCount) {
                val position = i * lineHeight.sp.toPx() + 1
                val line = paragraph.getLineForVerticalPosition(position)
                assertWithMessage("Line at line index $i, position $position does not match")
                    .that(line)
                    .isEqualTo(i)
            }
        }
    }

    @Test
    fun getLineForVerticalPosition_ltr_height_outOfBounds() {
        with(defaultDensity) {
            val text = "abcdefgh"
            val fontSize = 20f
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSize / 4
            val lineHeight = 30f

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.sp),
                    width = layoutWidth,
                )

            assertThat(paragraph.lineCount).isEqualTo(4)
            // greater than height
            var position = lineHeight.sp.toPx() * paragraph.lineCount * 2
            var line = paragraph.getLineForVerticalPosition(position)
            assertThat(line).isEqualTo(paragraph.lineCount - 1)

            // negative
            position = -1 * lineHeight.sp.toPx()
            line = paragraph.getLineForVerticalPosition(position)
            assertThat(line).isEqualTo(0)
        }
    }

    @Test
    @Ignore // TODO: Figure out why it fails on CI
    fun getLineForVerticalPosition_ltr_lineTopCenterBottom() {
        val text = "ab\ncde\n\nfg"
        // default density for the pixel 2 XL where test fails.
        val density = Density(3.5f, 1.0f)
        // font size where test fails
        val fontSize = 14.sp

        @Suppress("DEPRECATION")
        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        fontSize = fontSize,
                    ),
                density = density,
            )

        assertThat(paragraph.lineCount).isEqualTo(4)

        for (index in 0 until paragraph.lineCount) {
            assertThat(paragraph.getLineForVerticalPosition(paragraph.getLineTop(index)))
                .isEqualTo(index)

            assertThat(
                    paragraph.getLineForVerticalPosition(
                        (paragraph.getLineTop(index) + paragraph.getLineBottom(index)) / 2f
                    )
                )
                .isEqualTo(index)

            assertThat(paragraph.getLineForVerticalPosition(paragraph.getLineBottom(index) - 1f))
                .isEqualTo(index)
        }
    }

    @Test
    fun getBoundingBox_ltr_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // test positions that are 0, 1, 2 ... which maps to chars 0, 1, 2 ...
            for (i in 0..text.length - 1) {
                val box = paragraph.getBoundingBox(i)
                assertThat(box.left).isEqualToWithTolerance(i * fontSizeInPx)
                assertThat(box.right).isEqualToWithTolerance((i + 1) * fontSizeInPx)
                assertThat(box.top).isEqualToWithTolerance(0f)
                assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_rtl_singleLine() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            // test positions that are 0, 1, 2 ... which maps to chars 0, 1, 2 ...
            for (c in 0 until text.length) {
                val box = paragraph.getBoundingBox(c)
                val i = text.length - 1 - c // take the opposite side for non-relative calculation
                assertThat(box.left).isEqualToWithTolerance(i * fontSizeInPx)
                assertThat(box.right).isEqualToWithTolerance((i + 1) * fontSizeInPx)
                assertThat(box.top).isEqualToWithTolerance(0f)
                assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_multiLines() {
        with(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            // test positions are 3, 4, 5 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in secondLine.indices) {
                val textPosition = i + firstLine.length
                val box = paragraph.getBoundingBox(textPosition)
                assertThat(box.left).isEqualToWithTolerance(i * fontSizeInPx)
                assertThat(box.right).isEqualToWithTolerance((i + 1) * fontSizeInPx)
                assertThat(box.top).isEqualToWithTolerance(fontSizeInPx)
                assertThat(box.bottom).isEqualToWithTolerance(2f * fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_rtl_multiLines() {
        with(defaultDensity) {
            val firstLine = "\u05D0\u05D1\u05D2"
            val secondLine = "\u05D3\u05D4\u05D5"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            // test positions are 3, 4, 5 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in secondLine.indices) {
                val textPosition = i + firstLine.length
                val layoutPosition = secondLine.length - 1 - i
                val box = paragraph.getBoundingBox(textPosition)
                assertThat(box.left).isEqualToWithTolerance(layoutPosition * fontSizeInPx)
                assertThat(box.right).isEqualToWithTolerance((layoutPosition + 1) * fontSizeInPx)
                assertThat(box.top).isEqualToWithTolerance(fontSizeInPx)
                assertThat(box.bottom).isEqualToWithTolerance(2f * fontSizeInPx)
            }
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8589
    fun getBoundingBox_ltr_multiLines_spaceAtTheEndOfLine() {
        with(defaultDensity) {
            val firstLine = "abc "
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            val box = paragraph.getBoundingBox(3)
            assertThat(box.left).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.right).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8589
    fun getBoundingBox_rtl_multiLines_spaceAtTheEndOfLine() {
        with(defaultDensity) {
            val firstLine = "\u05D0\u05D1\u05D2 "
            val secondLine = "\u05D3\u05D4\u05D5"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = firstLine.length * fontSizeInPx,
                )

            val box = paragraph.getBoundingBox(3)
            assertThat(box.left).isEqualToWithTolerance(50f)
            assertThat(box.right).isEqualToWithTolerance(50f)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_ltr_multiLines_lineFeedEllipsized_maxLines() {
        with(defaultDensity) {
            val text = "abc def\ndef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

            val box = paragraph.getBoundingBox(9)
            assertThat(box.left).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.right).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_bidi_singleLineHeight_softWrap_ellipsized() {
        with(defaultDensity) {
            val text = "abc \u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    height = fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                )

            val box = paragraph.getBoundingBox(5)
            assertThat(box.left).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.right).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_bidi_singleLineHeight_softWrap_ellipsized_beforeLineFeed() {
        with(defaultDensity) {
            val text = "abc \u05D0\n\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    height = fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                )

            val box = paragraph.getBoundingBox(4)
            assertThat(box.left).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.right).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_rtl_multiLines_lineFeedEllipsized_maxLines() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

            val box = paragraph.getBoundingBox(9)
            assertThat(box.left).isEqualToWithTolerance(0f)
            assertThat(box.right).isEqualToWithTolerance(0f)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_ltr_multiLines_lineFeedEllipsized_maxHeight() {
        with(defaultDensity) {
            val text = "abc def\ndef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                    height = fontSizeInPx,
                )

            val box = paragraph.getBoundingBox(9)
            assertThat(box.left).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.right).isEqualToWithTolerance(3 * fontSizeInPx)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8591
    fun getBoundingBox_rtl_multiLines_lineFeedEllipsized_maxHeight() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                    overflow = TextOverflow.Ellipsis,
                    height = fontSizeInPx,
                )

            val box = paragraph.getBoundingBox(9)
            assertThat(box.left).isEqualToWithTolerance(0f)
            assertThat(box.right).isEqualToWithTolerance(0f)
            assertThat(box.top).isEqualToWithTolerance(0f)
            assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    fun getBoundingBox_ltr_textPosition_negative_throws_exception() {
        assertFailsWith<IllegalArgumentException> {
            with(defaultDensity) {
                val text = "abc"
                val fontSize = 50.sp
                val fontSizeInPx = fontSize.toPx()
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        width = text.length * fontSizeInPx,
                    )

                val textPosition = -1
                val box = paragraph.getBoundingBox(textPosition)
                assertThat(box.left).isEqualToWithTolerance(0f)
                assertThat(box.right).isEqualToWithTolerance(0f)
                assertThat(box.top).isEqualToWithTolerance(0f)
                assertThat(box.bottom).isEqualToWithTolerance(fontSizeInPx)
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_textPosition_larger_than_length_throw_exception() {
        assertFailsWith<IllegalArgumentException> {
            with(defaultDensity) {
                val text = "abc"
                val fontSize = 50.sp
                val fontSizeInPx = fontSize.toPx()
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        width = text.length * fontSizeInPx,
                    )

                val textPosition = text.length + 1
                paragraph.getBoundingBox(textPosition)
            }
        }
    }

    @Test
    fun getCursorRect_larger_than_length_throw_exception() {
        assertFailsWith<IllegalArgumentException> {
            with(defaultDensity) {
                val text = "abc"
                val fontSize = 50.sp
                val fontSizeInPx = fontSize.toPx()
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        width = text.length * fontSizeInPx,
                    )

                paragraph.getCursorRect(text.length + 1)
            }
        }
    }

    @Test
    fun getCursorRect_negative_throw_exception() {
        assertFailsWith<IllegalArgumentException> {
            with(defaultDensity) {
                val text = "abc"
                val fontSize = 50.sp
                val fontSizeInPx = fontSize.toPx()
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize),
                        width = text.length * fontSizeInPx,
                    )

                paragraph.getCursorRect(-1)
            }
        }
    }

    @Test
    fun getCursorRect_ltr_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            for (i in text.indices) {
                val cursorRect = paragraph.getCursorRect(i)
                val cursorXOffset = i * fontSizeInPx
                assertThat(cursorRect)
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = 0f,
                            right = cursorXOffset,
                            bottom = fontSizeInPx,
                        )
                    )
            }
        }
    }

    @Test
    fun getCursorRect_ltr_multiLines() {
        with(defaultDensity) {
            val text = "abcdef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val charsPerLine = 3
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = charsPerLine * fontSizeInPx,
                )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = i * fontSizeInPx
                assertThat(paragraph.getCursorRect(i))
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = 0f,
                            right = cursorXOffset,
                            bottom = fontSizeInPx,
                        )
                    )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (i % charsPerLine) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i))
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = fontSizeInPx,
                            right = cursorXOffset,
                            bottom = fontSizeInPx * 2f,
                        )
                    )
            }
        }
    }

    @Test
    fun getCursorRect_ltr_newLine() {
        with(defaultDensity) {
            val text = "abc\ndef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3))
                .isEqualToWithTolerance(
                    Rect(
                        left = 3 * fontSizeInPx,
                        top = 0f,
                        right = 3 * fontSizeInPx,
                        bottom = fontSizeInPx,
                    )
                )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4))
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = fontSizeInPx, right = 0f, bottom = fontSizeInPx * 2f)
                )
        }
    }

    @Test
    fun getCursorRect_ltr_newLine_last_char() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3))
                .isEqualToWithTolerance(
                    Rect(
                        left = 3 * fontSizeInPx,
                        top = 0f,
                        right = 3 * fontSizeInPx,
                        bottom = fontSizeInPx,
                    )
                )

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4))
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = fontSizeInPx, right = 0f, bottom = fontSizeInPx * 2f)
                )
        }
    }

    @Test
    fun getCursorRect_rtl_singleLine() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            for (i in text.indices) {
                val cursorXOffset = (text.length - i) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i))
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = 0f,
                            right = cursorXOffset,
                            bottom = fontSizeInPx,
                        )
                    )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_multiLines() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val charsPerLine = 3
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = charsPerLine * fontSizeInPx,
                )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = (charsPerLine - i) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i))
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = 0f,
                            right = cursorXOffset,
                            bottom = fontSizeInPx,
                        )
                    )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (charsPerLine - i % charsPerLine) * fontSizeInPx
                assertThat(paragraph.getCursorRect(i))
                    .isEqualToWithTolerance(
                        Rect(
                            left = cursorXOffset,
                            top = fontSizeInPx,
                            right = cursorXOffset,
                            bottom = fontSizeInPx * 2f,
                        )
                    )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_newLine() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = 3 * fontSizeInPx,
                )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3))
                .isEqualToWithTolerance(Rect(left = 0f, top = 0f, right = 0f, bottom = fontSizeInPx))

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4))
                .isEqualToWithTolerance(
                    Rect(
                        left = 3 * fontSizeInPx,
                        top = fontSizeInPx,
                        right = 3 * fontSizeInPx,
                        bottom = fontSizeInPx * 2f,
                    )
                )
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getCursorRect_rtl_newLine_last_char() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = 3 * fontSizeInPx,
                )

            // Cursor before '\n'
            assertThat(paragraph.getCursorRect(3))
                .isEqualToWithTolerance(Rect(left = 0f, top = 0f, right = 0f, bottom = fontSizeInPx))

            // Cursor after '\n'
            assertThat(paragraph.getCursorRect(4))
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = fontSizeInPx, right = 0f, bottom = fontSizeInPx * 2f)
                )
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            for (i in 1 until rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, true))
                .isEqualToWithTolerance(width)

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, true)).isZero()

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in 0..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            for (i in 1 until rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, true))
                .isEqualToWithTolerance(width)

            for (i in 1 until ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, true))
                    .isEqualToWithTolerance(rtlText.length * fontSizeInPx + i * fontSizeInPx)
            }

            for (i in 0..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, true))
                    .isEqualTo(rtlText.length * fontSizeInPx - i * fontSizeInPx)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_primary_rtl_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_ltr_newLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, true))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_primary_rtl_newLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, true)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = text.length * fontSizeInPx,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in ltrText.indices) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            for (i in 0..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, false))
                .isEqualToWithTolerance(0f)

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, false))
                .isEqualToWithTolerance(width)

            for (i in 1 until text.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
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
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in ltrText.indices) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(fontSizeInPx * i)
            }

            for (i in rtlText.indices) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualToWithTolerance(width - fontSizeInPx * i)
            }

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualToWithTolerance(width - rtlText.length * fontSizeInPx)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_notPrimary_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(0, false))
                .isEqualToWithTolerance(width - ltrText.length * fontSizeInPx)

            for (i in 1..ltrText.length) {
                assertThat(paragraph.getHorizontalPosition(i, false))
                    .isEqualToWithTolerance(rtlText.length * fontSizeInPx + i * fontSizeInPx)
            }

            for (i in 1..rtlText.length) {
                assertThat(paragraph.getHorizontalPosition(i + ltrText.length, false))
                    .isEqualToWithTolerance(rtlText.length * fontSizeInPx - i * fontSizeInPx)
            }
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8592
    fun getHorizontalPositionForOffset_notPrimary_rtl_newLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_ltr_newLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, false))
                .isEqualToWithTolerance(width)
        }
    }

    @Test
    fun getHorizontalPositionForOffset_notPrimary_rtl_newLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            assertThat(paragraph.getHorizontalPosition(text.length, false)).isZero()
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in text.indices) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in text.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in 0 until text.length - 1) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
            assertThat(paragraph.getBidiRunDirection(text.length - 1))
                .isEqualTo(ResolvedTextDirection.Ltr)
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionDefault() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionLtr() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = width,
                )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionRtl() {
        with(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val width = text.length * fontSizeInPx
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = width,
                )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Ltr)
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i)).isEqualTo(ResolvedTextDirection.Rtl)
            }
        }
    }

    @Test
    fun lineCount_withMaxLineSmallerThanTextLines() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount - 1
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
            )

        assertThat(paragraph.lineCount).isEqualTo(maxLines)
    }

    @Test
    fun lineCount_withMaxLineGreaterThanTextLines() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount + 1
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                maxLines = maxLines,
            )

        assertThat(paragraph.lineCount).isEqualTo(lineCount)
    }

    @Test
    fun maxLines_withMaxLineEqualsZero_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            simpleParagraph(text = "", maxLines = 0)
        }
    }

    @Test
    fun maxLines_withMaxLineNegative_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            simpleParagraph(text = "", maxLines = -1)
        }
    }

    @Test
    fun maxLines_withMaxLineSmallerThanTextLines_clipHeight() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount - 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                )

            val expectHeight = maxLines * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_withMaxLineSmallerThanTextLines_haveCorrectBaselines() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount - 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                )

            assertThat(paragraph.lineCount).isEqualTo(maxLines)
            val expectFirstBaseline = 0.8f * fontSizeInPx
            assertThat(paragraph.firstBaseline).isEqualToWithTolerance(expectFirstBaseline)
            val expectLastBaseline = (maxLines - 1) * fontSizeInPx + 0.8f * fontSizeInPx
            assertThat(paragraph.lastBaseline).isEqualToWithTolerance(expectLastBaseline)
        }
    }

    @Test
    fun maxLines_withMaxLineEqualsTextLine() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = text.lines().size
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                )

            val expectHeight = maxLines * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_withMaxLineGreaterThanTextLines() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount + 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                    width = 200f,
                )

            val expectHeight = lineCount * fontSizeInPx
            assertThat(paragraph.height).isEqualTo(expectHeight)
        }
    }

    @Test
    fun maxLines_withMaxLineGreaterThanTextLines_haveCorrectBaselines() {
        with(defaultDensity) {
            val text = "a\na\na"
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val lineCount = text.lines().size
            val maxLines = lineCount + 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                )

            assertThat(paragraph.lineCount).isEqualTo(lineCount)
            val expectFirstBaseline = 0.8f * fontSizeInPx
            assertThat(paragraph.firstBaseline).isEqualToWithTolerance(expectFirstBaseline)
            val expectLastBaseline = (lineCount - 1) * fontSizeInPx + 0.8f * fontSizeInPx
            assertThat(paragraph.lastBaseline).isEqualToWithTolerance(expectLastBaseline)
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        assertThat(paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                    // One line can only contain 1 character
                    width = fontSizeInPx,
                )

            assertThat(paragraph.didExceedMaxLines).isTrue()
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        with(defaultDensity) {
            val text = "aa"
            val maxLines = 3
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                    // One line can only contain 1 character
                    width = fontSizeInPx,
                )

            assertThat(paragraph.didExceedMaxLines).isFalse()
        }
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph =
            simpleParagraph(text = text, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

        assertThat(paragraph.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph =
            simpleParagraph(text = text, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph =
            simpleParagraph(text = text, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val maxLines = 1
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    // One line can only contain 1 character
                    width = fontSizeInPx,
                )

            assertThat(paragraph.didExceedMaxLines).isTrue()
        }
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph =
            simpleParagraph(text = text, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

        assertThat(paragraph.didExceedMaxLines).isFalse()
    }

    @Test
    fun didExceedMaxLines_ellipsis_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        with(defaultDensity) {
            val text = "aa"
            val maxLines = 3
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    // One line can only contain 1 character
                    width = fontSizeInPx,
                )

            assertThat(paragraph.didExceedMaxLines).isFalse()
        }
    }

    @Test
    fun textAlign_defaultValue_alignsStart() {
        with(defaultDensity) {
            val textLTR = "aa"
            val textRTL = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val layoutLTRWidth = (textLTR.length + 2) * fontSizeInPx
            val paragraphLTR =
                simpleParagraph(
                    text = textLTR,
                    style = TextStyle(fontSize = fontSize),
                    width = layoutLTRWidth,
                )

            val layoutRTLWidth = (textRTL.length + 2) * fontSizeInPx
            val paragraphRTL =
                simpleParagraph(
                    text = textRTL,
                    style = TextStyle(fontSize = fontSize),
                    width = layoutRTLWidth,
                )

            // When textAlign is TextAlign.start, LTR aligns to left, RTL aligns to right.
            assertThat(paragraphLTR.getLineLeft(0)).isZero()
            assertThat(paragraphRTL.getLineRight(0)).isEqualToWithTolerance(layoutRTLWidth)
        }
    }

    @Test
    fun textAlign_whenAlignLeft_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Left),
                        width = layoutWidth,
                    )

                assertThat(paragraph.getLineLeft(0)).isZero()
            }
        }
    }

    @Test
    fun textAlign_whenAlignRight_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Right),
                        width = layoutWidth,
                    )

                assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(layoutWidth)
            }
        }
    }

    @Test
    fun textAlign_whenAlignCenter_textIsCentered() {
        with(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Center),
                        width = layoutWidth,
                    )

                val textWidth = text.length * fontSizeInPx
                assertThat(paragraph.getLineLeft(0)).isEqualToWithTolerance(layoutWidth / 2 - textWidth / 2)
                assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(layoutWidth / 2 + textWidth / 2)
            }
        }
    }

    @Test
    fun textAlign_whenAlignStart_withLTR_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Start),
                    width = layoutWidth,
                )

            assertThat(paragraph.getLineLeft(0)).isZero()
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withLTR_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textAlign = TextAlign.End),
                    width = layoutWidth,
                )

            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(layoutWidth)
        }
    }

    @Test
    fun textAlign_whenAlignStart_withRTL_returnsLayoutWidthForGetLineRight() {
        with(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Start),
                    width = layoutWidth,
                )

            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(layoutWidth)
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withRTL_returnsZeroForGetLineLeft() {
        with(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textAlign = TextAlign.End),
                    width = layoutWidth,
                )

            assertThat(paragraph.getLineLeft(0)).isZero()
        }
    }

    @Test
    fun textAlign_whenAlignJustify_justifies() {
        with(defaultDensity) {
            val text = "a a a"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = ("a a".length + 1) * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textAlign = TextAlign.Justify),
                    width = layoutWidth,
                )

            assertThat(paragraph.getLineLeft(0)).isZero()
            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(layoutWidth)
            // Last line should align start
            assertThat(paragraph.getLineLeft(1)).isZero()
        }
    }

    @Test
    fun textDirection_whenLTR_dotIsOnRight() {
        with(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Ltr),
                    width = layoutWidth,
                )

            // The position of the last character in display order.
            val position = Offset(("a.".length * fontSizeInPx + 1), (fontSizeInPx / 2))
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex).isEqualTo(2)
        }
    }

    @Test
    fun textDirection_whenRTL_dotIsOnLeft() {
        with(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, textDirection = TextDirection.Rtl),
                    width = layoutWidth,
                )

            // The position of the first character in display order.
            val position = Offset((fontSizeInPx / 2 + 1), (fontSizeInPx / 2))
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex).isEqualTo(2)
        }
    }

    @Test
    fun textDirection_whenDefault_withoutStrongChar_directionIsLTR() {
        with(defaultDensity) {
            val text = "..."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize, localeList = ltrLocaleList),
                    width = layoutWidth,
                )

            for (i in 0..text.length) {
                // The position of the i-th character in display order.
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex).isEqualTo(i)
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharLTR_directionIsLTR() {
        with(defaultDensity) {
            val text = "a\u05D0."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = layoutWidth,
                )

            for (i in text.indices) {
                // The position of the i-th character in display order.
                val position = Offset((i * fontSizeInPx + 1), (fontSizeInPx / 2))
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex).isEqualTo(i)
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharRTL_directionIsRTL() {
        with(defaultDensity) {
            val text = "\u05D0a."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val layoutWidth = text.length * fontSizeInPx

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    width = layoutWidth,
                )

            // The first character in display order should be '.'
            val position = Offset((fontSizeInPx / 2 + 1), (fontSizeInPx / 2))
            val index = paragraph.getOffsetForPosition(position)
            assertThat(index).isEqualTo(2)
        }
    }

    @Test
    fun getLineTop() {
        with(defaultDensity) {
            val text = "aaa\nbbb"

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))
            assertThat(paragraph.getLineTop(0)).isZero()
            assertThat(paragraph.getLineTop(1)).isEqualToWithTolerance(fontSizeInPx)
        }
    }

    @Test
    fun getLineBaseline() {
        with(defaultDensity) {
            val text = "aaa\nbbb\nccc"

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))
            // 1st
            assertThat(paragraph.getLineBaseline(0)).isEqualToWithTolerance(fontSizeInPx * 0.8f)
            assertThat(paragraph.getLineBaseline(0)).isEqualToWithTolerance(paragraph.firstBaseline)
            // 2nd
            assertThat(paragraph.getLineBaseline(1)).isEqualToWithTolerance(fontSizeInPx + fontSizeInPx * 0.8f)
            // last
            assertThat(paragraph.getLineBaseline(2))
                .isEqualToWithTolerance(fontSizeInPx * 2 + fontSizeInPx * 0.8f)
            assertThat(paragraph.getLineBaseline(2)).isEqualToWithTolerance(paragraph.lastBaseline)
        }
    }

    @Test
    fun getLineBottom() {
        with(defaultDensity) {
            val text = "aaa\nbbb"

            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))
            assertThat(paragraph.getLineBottom(0)).isEqualToWithTolerance(fontSizeInPx)
            assertThat(paragraph.getLineBottom(1)).isEqualToWithTolerance(fontSize.value * 2f)
        }
    }

    @Test
    fun getLineForOffset_withNewline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)
        for (i in 0..2) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        for (i in 4..6) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(1)
        }
    }

    @Test
    fun getLineForOffset_newline_belongsToPreviousLine() {
        val text = "aaa\nbbb\n"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)
        assertThat(paragraph.getLineForOffset(3)).isEqualTo(0)
        assertThat(paragraph.getLineForOffset(7)).isEqualTo(1)
    }

    @Test
    fun getLineForOffset_outOfBoundary() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)
        assertThat(paragraph.getLineForOffset(-1)).isEqualTo(0)
        assertThat(paragraph.getLineForOffset(-2)).isEqualTo(0)

        assertThat(paragraph.getLineForOffset(text.length)).isEqualTo(1)
        assertThat(paragraph.getLineForOffset(text.length + 1)).isEqualTo(1)
    }

    @Test
    @Ignore // TODO(https://youtrack.jetbrains.com/issue/CMP-8590)
    fun getLineForOffset_ellipsisApplied() {
        val text = "aaa\nbbb"

        val paragraph =
            simpleParagraph(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(),
                width = Float.MAX_VALUE,
            )

        for (i in 0..2) {
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        assertThat(paragraph.getLineForOffset(3)).isEqualTo(0)
        for (i in 4..6) {
            // It returns 0 because the second line(index 1) is ellipsized
            assertThat(paragraph.getLineForOffset(i)).isEqualTo(0)
        }
        // It returns 0 since the paragraph actually has 1 line
        assertThat(paragraph.getLineForOffset(text.length + 1)).isEqualTo(0)

        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.length)
    }

    @Test
    fun getLineStart_linebreak() {
        val text = "aaabbb"
        val fontSize = 50f

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize.sp),
                width = fontSize * 3,
            )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(3)
    }

    @Test
    fun getLineStart_newline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(text.lines().size)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        // First char after '\n'
        assertThat(paragraph.getLineStart(1)).isEqualTo(text.indexOfFirst { ch -> ch == '\n' } + 1)
    }

    @Test
    @Ignore // TODO(https://youtrack.jetbrains.com/issue/CMP-8590)
    fun getLineStart_emptyLine() {
        val text = "aaa\n"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(4)
    }

    @Test
    fun getLineEnd_linebreak() {
        val text = "aaabbb"
        val fontSize = 50f

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize.sp),
                width = fontSize * 3,
                density = defaultDensity,
            )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineStart(1)).isEqualTo(3)
    }

    @Test
    @Ignore // FIXME: Figure out why skia reports wrong indexes
    fun getLineEnd_newline() {
        val text = "aaa\nbbb"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(text.lines().size)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.indexOfFirst { ch -> ch == '\n' } + 1)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
    }

    @Test
    fun getLineEnd_emptyLine() {
        val text = "aaa\n"

        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(4)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(4)
    }

    @Test
    @Ignore // TODO: isLineEllipsized is not implemented
    fun getLineEllipsisOffset() {
        val text = "aaa\nbbb\nccc"

        val paragraph =
            simpleParagraph(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                width = Float.MAX_VALUE,
            )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(4)
        assertThat(paragraph.getLineEnd(0, true)).isEqualTo(3) // "\n" is excluded
        assertThat(paragraph.isLineEllipsized(0)).isFalse()

        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
        assertThat(paragraph.getLineEnd(1, true)).isEqualTo(7) // "\n" is excluded
        assertThat(paragraph.isLineEllipsized(1)).isTrue()
    }

    @Test
    @Ignore // TODO: isLineEllipsized is not implemented
    fun getLineEllipsisCount() {
        val text = "aaaaabbbbbccccc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 10.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                width = 50f,
            )

        // Prerequisite check for the this test.
        assertThat(paragraph.lineCount).isEqualTo(2)

        assertThat(paragraph.isLineEllipsized(0)).isFalse()
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(5)
        assertThat(paragraph.getLineEnd(0, true)).isEqualTo(5)

        assertThat(paragraph.isLineEllipsized(1)).isTrue()
        assertThat(paragraph.getLineStart(1)).isEqualTo(5)
        assertThat(paragraph.getLineEnd(1)).isEqualTo(text.length)
        // The ellipsizer may reserve multiple characters for drawing HORIZONTAL ELLIPSIS
        // character (U+2026). We can only expect the visible end is not the end of the line.
        assertThat(paragraph.getLineEnd(1, true)).isNotEqualTo(text.length)
    }

    @Test
    @Ignore // TODO(https://youtrack.jetbrains.com/issue/CMP-6716): Properly support Middle/Start ellipsis
    fun getLineStartEllipsisCount() {
        val text = "aaaaabbbbbccccc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                width = 50f,
            )

        assertThat(paragraph.lineCount).isEqualTo(1)

        assertThat(paragraph.isLineEllipsized(0)).isTrue()
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.length)
    }

    @Test
    @Ignore // TODO(https://youtrack.jetbrains.com/issue/CMP-6716): Properly support Middle/Start ellipsis
    fun getLineMiddleEllipsisCount() {
        val text = "aaaaabbbbbccccc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                width = 50f,
            )

        assertThat(paragraph.lineCount).isEqualTo(1)

        assertThat(paragraph.isLineEllipsized(0)).isTrue()
        assertThat(paragraph.getLineStart(0)).isEqualTo(0)
        assertThat(paragraph.getLineEnd(0)).isEqualTo(text.length)
    }

    @Test
    fun lineHeight_inSp() {
        val text = "abcdefgh"
        val fontSize = 20f
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize / 4
        val lineHeight = 30f

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.sp),
                width = layoutWidth,
            )

        assertThat(paragraph.lineCount).isEqualTo(4)
        // First/last line is influenced by top/bottom padding
        for (i in 1 until paragraph.lineCount - 1) {
            val actualHeight = paragraph.getLineHeight(i)
            // In the sample_font_fork.ttf, the height of the line should be
            // fontSize + 0.2f * fontSize(line gap)
            assertWithMessage("line number $i").that(actualHeight).isEqualToWithTolerance(lineHeight)
        }
    }

    @Test
    fun lineHeight_InEm() {
        val text = "abcdefgh"
        val fontSize = 20f
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize / 4
        val lineHeight = 1.5f

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, lineHeight = lineHeight.em),
                width = layoutWidth,
            )

        assertThat(paragraph.lineCount).isEqualTo(4)
        // First/last line is influenced by top/bottom padding
        for (i in 1 until paragraph.lineCount - 1) {
            val actualHeight = paragraph.getLineHeight(i)
            // In the sample_font_fork.ttf, the height of the line should be
            // fontSize + 0.2f * fontSize(line gap)
            assertWithMessage("line number $i").that(actualHeight).isEqualToWithTolerance(lineHeight * fontSize)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)
            val paragraphWidth = fontSizeInPx * text.length

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    width = paragraphWidth,
                )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(fontSizeInPx * text.length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnPartOfText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyleFontSize = 30.sp
            val spanStyleFontSizeInPx = spanStyleFontSize.toPx()
            val spanStyle = SpanStyle(fontSize = spanStyleFontSize)
            val paragraphWidth = spanStyleFontSizeInPx * text.length

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                    style = TextStyle(fontSize = fontSize),
                    width = paragraphWidth,
                )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedLineRight =
                "abc".length * spanStyleFontSizeInPx + "de".length * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedLineRight)
        }
    }

    @Test
    fun testAnnotatedString_seFontSizeTwice_lastOneOverwrite() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val spanStyleOverwrite = SpanStyle(fontSize = fontSizeOverwrite)
            val paragraphWidth = fontSizeOverwriteInPx * text.length

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(spanStyle, 0, text.length),
                            AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length),
                        ),
                    width = paragraphWidth,
                )

            // Make sure there is only one line, so that we can use getLineRight to test fontSize.
            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = "abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx
            assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScale() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val em = 0.5.em
            val spanStyle = SpanStyle(fontSize = em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    style = TextStyle(fontSize = fontSize),
                )

            assertThat(paragraph.getLineRight(0))
                .isEqualToWithTolerance(text.length * fontSizeInPx * em.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleNested() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val em = 0.5f.em
            val spanStyle = SpanStyle(fontSize = em)

            val emNested = 2f.em
            val spanStyleNested = SpanStyle(fontSize = emNested)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(spanStyle, 0, text.length),
                            AnnotatedString.Range(spanStyleNested, 0, text.length),
                        ),
                    style = TextStyle(fontSize = fontSize),
                )

            assertThat(paragraph.getLineRight(0))
                .isEqualToWithTolerance(text.length * fontSizeInPx * em.value * emNested.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeFirst() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em = 0.5f.em
            val fontSizeScaleStyle = SpanStyle(fontSize = em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(fontSizeStyle, 0, text.length),
                            AnnotatedString.Range(fontSizeScaleStyle, 0, text.length),
                        ),
                    style = TextStyle(fontSize = paragraphFontSize),
                )

            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(text.length * fontSizeInPx * em.value)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeSecond() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em = 0.5f.em
            val fontSizeScaleStyle = SpanStyle(fontSize = em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(fontSizeScaleStyle, 0, text.length),
                            AnnotatedString.Range(fontSizeStyle, 0, text.length),
                        ),
                    style = TextStyle(fontSize = paragraphFontSize),
                )

            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(text.length * fontSizeInPx)
        }
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeNested() {
        with(defaultDensity) {
            val text = "abcde"
            val paragraphFontSize = 20.sp

            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSize)

            val em1 = 0.5f.em
            val fontSizeScaleStyle1 = SpanStyle(fontSize = em1)

            val em2 = 2f.em
            val fontSizeScaleStyle2 = SpanStyle(fontSize = em2)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(fontSizeScaleStyle1, 0, text.length),
                            AnnotatedString.Range(fontSizeStyle, 0, text.length),
                            AnnotatedString.Range(fontSizeScaleStyle2, 0, text.length),
                        ),
                    style = TextStyle(fontSize = paragraphFontSize),
                )

            assertThat(paragraph.getLineRight(0)).isEqualToWithTolerance(text.length * fontSizeInPx * em2.value)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_OnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        fontSizeInPx * text.length * (1 + letterSpacing) -
                            fontSizeInPx * letterSpacing * 0.5f - // left edge letter spacing
                            fontSizeInPx * letterSpacing * 0.5f // right edge letter spacing
                    )
            } else {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(fontSizeInPx * text.length * (1 + letterSpacing))
            }
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_OnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.sp)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        (fontSizeInPx + letterSpacing) * text.length -
                            letterSpacing * 0.5f - // left edge letter spacing
                            letterSpacing * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance((fontSizeInPx + letterSpacing) * text.length)
            }
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnPartText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth = ("abc".length * letterSpacing + text.length) * fontSizeInPx
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            letterSpacing * fontSizeInPx * 0.5f - // left edge letter spacing
                            0f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingTwice_lastOneOverwrite() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 5.0f
            val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val letterSpacingOverwrite = 10.0f
            val spanStyleOverwrite = SpanStyle(letterSpacing = letterSpacingOverwrite.em)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(spanStyle, 0, text.length),
                            AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length),
                        ),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth =
                "abc".length * (1 + letterSpacingOverwrite) * fontSizeInPx +
                    "de".length * (1 + letterSpacing) * fontSizeInPx
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            fontSizeInPx *
                                letterSpacingOverwrite *
                                0.5f - // left edge letter spacing
                            fontSizeInPx * letterSpacing * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    @Ignore // TODO
    fun testAnnotatedString_setLetterSpacing_inEm_withFontSize() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 2f
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSizeOverwrite)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                            AnnotatedString.Range(fontSizeStyle, 0, "abc".length),
                        ),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth =
                (1 + letterSpacing) *
                    ("abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx)
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            letterSpacing *
                                fontSizeOverwriteInPx *
                                0.5f - // left edge letter spacing
                            letterSpacing * fontSizeInPx * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    @Ignore // TODO
    fun testAnnotatedString_setLetterSpacing_inEm_withScaleX() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 2f
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing.em)

            val scaleX = 1.5f
            val scaleXStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX))

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                            AnnotatedString.Range(scaleXStyle, 0, "abc".length),
                        ),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth =
                (1 + letterSpacing) *
                    ("abc".length * fontSizeInPx * scaleX + "de".length * fontSizeInPx)
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            letterSpacing *
                                fontSizeInPx *
                                scaleX *
                                0.5f - // left edge letter spacing
                            letterSpacing * fontSizeInPx * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_withFontSize() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 10.sp
            val letterSpacingInPx = letterSpacing.toPx()
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing)

            val fontSizeOverwrite = 30.sp
            val fontSizeOverwriteInPx = fontSizeOverwrite.toPx()
            val fontSizeStyle = SpanStyle(fontSize = fontSizeOverwrite)

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                            AnnotatedString.Range(fontSizeStyle, 0, "abc".length),
                        ),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth =
                text.length * letterSpacingInPx +
                    ("abc".length * fontSizeOverwriteInPx + "de".length * fontSizeInPx)
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            letterSpacingInPx * 0.5f - // left edge letter spacing
                            letterSpacingInPx * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    @Ignore // TODO
    fun testAnnotatedString_setLetterSpacing_inSp_withScaleX() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()

            val letterSpacing = 10.sp
            val letterSpacingInPx = letterSpacing.toPx()
            val letterSpacingStyle = SpanStyle(letterSpacing = letterSpacing)

            val scaleX = 1.5f
            val scaleXStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX))

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(letterSpacingStyle, 0, text.length),
                            AnnotatedString.Range(scaleXStyle, 0, "abc".length),
                        ),
                    width = Float.MAX_VALUE,
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            // Notice that in this test font, the width of character equals to fontSize.
            val expectedWidth =
                text.length * letterSpacingInPx +
                    ("abc".length * fontSizeInPx * scaleX + "de".length * fontSizeInPx)
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineWidth(0))
                    .isEqualToWithTolerance(
                        expectedWidth -
                            letterSpacingInPx * 0.5f - // left edge letter spacing
                            letterSpacingInPx * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
            }
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inSp_after_inEm() {
        val text = "abcde"
        val fontSize = 20f

        val letterSpacingEm = 1f
        val letterSpacingEmStyle = SpanStyle(letterSpacing = letterSpacingEm.em)

        val letterSpacingSp = 10f
        val letterSpacingSpStyle = SpanStyle(letterSpacing = letterSpacingSp.sp)

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp),
                spanStyles =
                    listOf(
                        AnnotatedString.Range(letterSpacingEmStyle, 0, text.length),
                        AnnotatedString.Range(letterSpacingSpStyle, 0, "abc".length),
                    ),
                width = Float.MAX_VALUE,
            )

        assertThat(paragraph.lineCount).isEqualTo(1)
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth =
            fontSize * text.length +
                "abc".length * letterSpacingSp +
                "de".length * fontSize * letterSpacingEm
        if (hasEdgeLetterSpacingBugFix()) {
            assertThat(paragraph.getLineWidth(0))
                .isEqualToWithTolerance(
                    expectedWidth -
                        letterSpacingSp * 0.5f - // left edge letter spacing
                        fontSize * letterSpacingEm * 0.5f
                ) // right edge letter spacing
        } else {
            assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacing_inEm_after_inSp() {
        val text = "abcde"
        val fontSize = 20f

        val letterSpacingEm = 1f
        val letterSpacingEmStyle = SpanStyle(letterSpacing = letterSpacingEm.em)

        val letterSpacingSp = 10f
        val letterSpacingSpStyle = SpanStyle(letterSpacing = letterSpacingSp.sp)

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp),
                spanStyles =
                    listOf(
                        AnnotatedString.Range(letterSpacingSpStyle, 0, "abc".length),
                        AnnotatedString.Range(letterSpacingEmStyle, 0, text.length),
                    ),
                width = 500f,
            )

        assertThat(paragraph.lineCount).isEqualTo(1)
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth = fontSize * text.length * (1 + letterSpacingEm)
        if (hasEdgeLetterSpacingBugFix()) {
            assertThat(paragraph.getLineWidth(0))
                .isEqualToWithTolerance(
                    expectedWidth -
                        letterSpacingEm * fontSize * 0.5f - // left edge letter spacing
                        letterSpacingEm * fontSize * 0.5f
                ) // right edge letter spacing
        } else {
            assertThat(paragraph.getLineWidth(0)).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    fun textIndent_inSp_onSingleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val indent = 20.sp
            val indentInPx = indent.toPx()

            val paragraph =
                simpleParagraph(
                    text = text,
                    style =
                        TextStyle(
                            fontSize = fontSize,
                            textIndent = TextIndent(firstLine = indent),
                            fontFamily = fontFamilyMeasureFont,
                        ),
                )

            // This position should point to the first character 'a' if indent is applied.
            // Otherwise this position will point to the second character 'b'.
            val position = Offset((indentInPx + 1), (fontSizeInPx / 2))
            // The offset corresponding to the position should be the first char 'a'.
            assertThat(paragraph.getOffsetForPosition(position)).isEqualTo(0)
        }
    }

    @Test
    fun textIndent_inSp_onFirstLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 15f
        val paragraphWidth = "abcd".length * fontSize

        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        fontSize = fontSize.sp,
                        textIndent = TextIndent(firstLine = indent.sp),
                        fontFamily = fontFamilyMeasureFont,
                    ),
                width = paragraphWidth,
            )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getHorizontalPosition(0, true))
            .isEqualToWithTolerance(indent)
    }

    @Test
    @Ignore // TODO: Not sure why it fails, failed to reproduce on real app
    fun textIndent_inSp_onRestLine() {
        val text = "abcde"
        val fontSize = 20f
        val indent = 20f
        val paragraphWidth = "abc".length * fontSize

        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        textIndent = TextIndent(restLine = indent.sp),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamilyMeasureFont,
                    ),
                width = paragraphWidth,
            )

        // check the position of the first character in second line: "d" should be indented
        assertThat(paragraph.getHorizontalPosition(3, true))
            .isEqualToWithTolerance(indent)
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8593
    fun textIndent_inEm_onSingleLine() {
        val text = "abc"
        val fontSize = 20f
        val indent = 1.5f

        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        textIndent = TextIndent(firstLine = indent.em),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamilyMeasureFont,
                    ),
            )

        assertThat(paragraph.getHorizontalPosition(0, true))
            .isEqualToWithTolerance(indent * fontSize)
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8593
    fun textIndent_inEm_onFirstLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 1.5f

        val paragraphWidth = "abcd".length * fontSize

        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        textIndent = TextIndent(firstLine = indent.em),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamilyMeasureFont,
                    ),
                width = paragraphWidth,
            )

        assertThat(paragraph.lineCount).isEqualTo(2)
        assertThat(paragraph.getHorizontalPosition(0, true))
            .isEqualToWithTolerance(indent * fontSize)
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8593
    fun textIndent_inEm_onRestLine() {
        val text = "abcdef"
        val fontSize = 20f
        val indent = 1.5f

        val paragraphWidth = "abcd".length * fontSize

        val paragraph =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        textIndent = TextIndent(restLine = indent.em),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamilyMeasureFont,
                    ),
                width = paragraphWidth,
            )

        assertThat(paragraph.lineCount).isEqualTo(2)
        // check the position of the first character in second line: "e" should be indented
        assertThat(paragraph.getHorizontalPosition(4, true))
            .isEqualToWithTolerance(indent * fontSize)
    }

    @Test
    fun testDefaultSpanStyle_setLetterSpacing() {
        with(defaultDensity) {
            val text = "abc"
            // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
            val fontSize = 100.sp
            val fontSizeInPx = fontSize.toPx()
            val letterSpacing = 1f

            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(letterSpacing = letterSpacing.em, fontSize = fontSize),
                )

            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineRight(0))
                    .isEqualToWithTolerance(
                        fontSizeInPx * (1 + letterSpacing) * text.length -
                            fontSizeInPx * 0.5f - // left edge letter spacing
                            fontSizeInPx * 0.5f
                    ) // right edge letter spacing
            } else {
                assertThat(paragraph.getLineRight(0))
                    .isEqualToWithTolerance(fontSizeInPx * (1 + letterSpacing) * text.length)
            }
        }
    }

    @Test
    fun testGetPathForRange_singleLine() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight - fontSizeInPx, fontSizeInPx))

            // Select "ab"
            val actualPath = paragraph.getPathForRange(0, 2)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_multiLines() {
        with(defaultDensity) {
            val text = "abc\nabc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            val expectedPath = Path()
            val firstLineLeft = paragraph.getLineLeft(0)
            val secondLineLeft = paragraph.getLineLeft(1)
            val firstLineRight = paragraph.getLineRight(0)
            val secondLineRight = paragraph.getLineRight(1)
            expectedPath.addRect(
                Rect(firstLineLeft + fontSizeInPx, 0f, firstLineRight, fontSizeInPx)
            )
            expectedPath.addRect(
                Rect(secondLineLeft, fontSizeInPx, secondLineRight - fontSizeInPx, paragraph.height)
            )

            // Select "bc\nab"
            val actualPath = paragraph.getPathForRange(1, 6)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Bidi() {
        with(defaultDensity) {
            val textLTR = "Hello"
            val textRTL = "שלום"
            val text = textLTR + textRTL
            val selectionLTRStart = 2
            val selectionRTLEnd = 2
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft + selectionLTRStart * fontSizeInPx,
                    0f,
                    lineLeft + textLTR.length * fontSizeInPx,
                    fontSizeInPx,
                )
            )
            expectedPath.addRect(
                Rect(lineRight - selectionRTLEnd * fontSizeInPx, 0f, lineRight, fontSizeInPx)
            )

            // Select "llo..של"
            val actualPath =
                paragraph.getPathForRange(selectionLTRStart, textLTR.length + selectionRTLEnd)

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Start_Equals_End_Returns_Empty_Path() {
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            )

        val actualPath = paragraph.getPathForRange(1, 1)

        assertThat(actualPath.getBounds()).isEqualTo(Rect.Zero)
    }

    @Test
    fun testGetPathForRange_Empty_Text() {
        val text = ""
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            )

        val actualPath = paragraph.getPathForRange(0, 0)

        assertThat(actualPath.getBounds()).isEqualTo(Rect.Zero)
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_Second_Character_Selected() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            // Try to select "\uDD1E\uD834\uDD1F", only "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, 2 * fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_End_Middle_Second_Character_Selected() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            // Try to select "\uDD1E\uD834", actually "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_End_Same_Character_Returns_Line_Segment() {
        with(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            // Try to select "\uDD1E", get vertical line segment after this character.
            val actualPath = paragraph.getPathForRange(1, 2)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Emoji_Sequence() {
        with(defaultDensity) {
            val text = "\uD83D\uDE00\uD83D\uDE03\uD83D\uDE04\uD83D\uDE06"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            // Select "\u1F603\u1F604"
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val expectedPath = Path()
            expectedPath.addRect(Rect(fontSizeInPx, 0f, fontSizeInPx * 3, fontSizeInPx))

            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Unicode_200D_Return_Line_Segment() {
        with(defaultDensity) {
            val text = "\u200D"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft).isEqualTo(lineRight)
            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetPathForRange_Unicode_2066_Return_Line_Segment() {
        with(defaultDensity) {
            val text = "\u2066"
            val fontSize = 20f.sp
            val fontSizeInPx = fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft).isEqualTo(lineRight)
            val diff = Path.combine(PathOperation.Difference, expectedPath, actualPath).getBounds()
            assertThat(diff).isEqualTo(Rect.Zero)
        }
    }

    @Test
    fun testGetWordBoundary() {
        val text = "abc def"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            )

        val result = paragraph.getWordBoundary(text.indexOf('a'))

        assertThat(result.start).isEqualTo(text.indexOf('a'))
        assertThat(result.end).isEqualTo(text.indexOf(' '))
    }

    @Test
    fun testGetWordBoundary_spaces() {
        val text = "ab cd  e"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            )

        // end of word (length+1) will select word
        val singleSpaceStartResult = paragraph.getWordBoundary(text.indexOf('b') + 1)
        assertThat(singleSpaceStartResult.start).isEqualTo(text.indexOf('a'))
        assertThat(singleSpaceStartResult.end).isEqualTo(text.indexOf('b') + 1)

        // beginning of word will select word
        val singleSpaceEndResult = paragraph.getWordBoundary(text.indexOf('c'))
        assertThat(singleSpaceEndResult.start).isEqualTo(text.indexOf('c'))
        assertThat(singleSpaceEndResult.end).isEqualTo(text.indexOf('d') + 1)

        // between spaces ("_ | _") where | is the requested offset and _ is the space, will
        // return the exact collapsed range at offset/offset
        val doubleSpaceResult = paragraph.getWordBoundary(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.start).isEqualTo(text.indexOf('d') + 2)
        assertThat(doubleSpaceResult.end).isEqualTo(text.indexOf('d') + 2)
    }

    @Test
    fun testGetWordBoundary_Bidi() {
        val text = "abc \u05d0\u05d1\u05d2 def"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            )

        val resultEnglish = paragraph.getWordBoundary(text.indexOf('a'))
        val resultHebrew = paragraph.getWordBoundary(text.indexOf('\u05d1'))

        assertThat(resultEnglish.start).isEqualTo(text.indexOf('a'))
        assertThat(resultEnglish.end).isEqualTo(text.indexOf(' '))
        assertThat(resultHebrew.start).isEqualTo(text.indexOf('\u05d0'))
        assertThat(resultHebrew.end).isEqualTo(text.indexOf('\u05d2') + 1)
    }

    @Test
    fun getWordBoundary_multichar() {
        // "ab 𐐔𐐯𐑅𐐨𐑉𐐯𐐻 cd" - example of multi-char code units
        //             | (offset=3)      | (offset=6)
        val text =
            "ab \uD801\uDC14\uD801\uDC2F\uD801\uDC45\uD801\uDC28\uD801\uDC49\uD801\uDC2F\uD801\uDC3B cd"
        val paragraph = simpleParagraph(text, TextStyle())
        val result = paragraph.getWordBoundary(6)
        assertThat(result.start).isEqualTo(3)
        assertThat(result.end).isEqualTo(17)
    }

    @Test
    fun test_finalFontSizeChangesWithDensity() {
        val text = "a"
        val fontSize = 20.sp
        val densityMultiplier = 2f

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                density = Density(density = 1f, fontScale = 1f),
            )

        val doubleFontSizeParagraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize),
                density = Density(density = 1f, fontScale = densityMultiplier),
            )

        // Since Android uses non-linear font scaling, best we can do is check that the size fits a
        // range.
        assertThat(doubleFontSizeParagraph.maxIntrinsicWidth)
            .isGreaterThan(paragraph.maxIntrinsicWidth)
        assertThat(doubleFontSizeParagraph.maxIntrinsicWidth)
            .isAtMost(paragraph.maxIntrinsicWidth * densityMultiplier)

        assertThat(doubleFontSizeParagraph.height).isGreaterThan(paragraph.height)
        assertThat(doubleFontSizeParagraph.height).isAtMost(paragraph.height * densityMultiplier)
    }

    @Test
    @Ignore // TODO(https://youtrack.jetbrains.com/issue/CMP-8589) Align whitespace behavior
    fun minInstrinsicWidth_includes_white_space() {
        with(defaultDensity) {
            val fontSize = 12.sp
            val text = "b "
            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))

            val expectedWidth = text.length * fontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8589
    fun minInstrinsicWidth_returns_longest_word_width() {
        with(defaultDensity) {
            // create words with length 1, 2, 3... 50; and append all with space.
            val maxWordLength = 50
            val text =
                (1..maxWordLength).fold("") { string, next -> string + "a".repeat(next) + " " }
            val fontSize = 12.sp
            val paragraph = simpleParagraph(text = text, style = TextStyle(fontSize = fontSize))

            // +1 is for the white space
            val expectedWidth = (maxWordLength + 1) * fontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-8589
    fun minInstrinsicWidth_withStyledText() {
        with(defaultDensity) {
            val text = "a bb ccc"
            val fontSize = 12.sp
            val styledFontSize = fontSize * 2
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(
                                SpanStyle(fontSize = styledFontSize),
                                "a".length,
                                "a bb ".length,
                            )
                        ),
                )

            val expectedWidth = "bb ".length * styledFontSize.toPx()
            assertThat(paragraph.minIntrinsicWidth).isEqualToWithTolerance(expectedWidth)
        }
    }

    @Test
    fun getPathForRange_throws_exception_if_start_larger_than_end() {
        assertFailsWith<IllegalArgumentException> {
            val text = "ab"
            val textStart = 0
            val textEnd = text.length
            val paragraph = simpleParagraph(text = text)

            paragraph.getPathForRange(textEnd, textStart)
        }
    }

    @Test
    fun getPathForRange_throws_exception_if_start_is_smaller_than_zero() {
        assertFailsWith<IllegalArgumentException> {
            val text = "ab"
            val textStart = 0
            val textEnd = text.length
            val paragraph = simpleParagraph(text = text)

            paragraph.getPathForRange(textStart - 2, textEnd - 1)
        }
    }

    @Test
    fun getPathForRange_throws_exception_if_end_is_larger_than_text_length() {
        assertFailsWith<IllegalArgumentException> {
            val text = "ab"
            val textStart = 0
            val textEnd = text.length
            val paragraph = simpleParagraph(text = text)

            paragraph.getPathForRange(textStart, textEnd + 1)
        }
    }

    @Test
    fun createParagraph_with_ParagraphIntrinsics() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 14.sp
            val fontSizeInPx = fontSize.toPx()

            val paragraphIntrinsics =
                ParagraphIntrinsics(
                    text = text,
                    style = TextStyle(fontSize = fontSize, fontFamily = fontFamilyMeasureFont),
                    annotations = listOf(),
                    density = defaultDensity,
                    fontFamilyResolver = fontFamilyResolver,
                    placeholders = listOf(),
                )

            val paragraph =
                Paragraph(
                    paragraphIntrinsics = paragraphIntrinsics,
                    constraints = Constraints(maxWidth = (fontSizeInPx * text.length).ceilToInt()),
                    overflow = TextOverflow.Clip,
                )

            assertThat(paragraph.maxIntrinsicWidth).isEqualToWithTolerance(paragraphIntrinsics.maxIntrinsicWidth)
            assertThat(paragraph.width).isEqualToWithTolerance(fontSizeInPx * text.length)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeMaxLines_throwsException() {
        simpleParagraph(text = "", maxLines = -1, width = Float.MAX_VALUE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWidth_throwsException() {
        simpleParagraph(text = "", width = -1f)
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        density: Density? = null,
        width: Float = Float.MAX_VALUE,
        height: Float = Float.MAX_VALUE,
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(fontFamily = fontFamilyMeasureFont).merge(style),
            maxLines = maxLines,
            overflow = overflow,
            constraints = Constraints(maxWidth = width.ceilToInt(), maxHeight = height.ceilToInt()),
            density = density ?: defaultDensity,
            fontFamilyResolver = fontFamilyResolver,
        )
    }
}

private fun FloatSubject.isEqualToWithTolerance(expected: Float, tolerance: Float = 0.001f) =
    isWithin(tolerance).of(expected)

private fun Subject<Rect>.isEqualToWithTolerance(expected: Rect, tolerance: Float = 0.001f) {
    assertEquals(expected.left, actual!!.left, tolerance)
    assertEquals(expected.top, actual!!.top, tolerance)
    assertEquals(expected.right, actual!!.right, tolerance)
    assertEquals(expected.bottom, actual!!.bottom, tolerance)
}
