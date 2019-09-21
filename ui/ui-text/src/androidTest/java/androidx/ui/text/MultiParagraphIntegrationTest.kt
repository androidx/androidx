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

import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.filters.Suppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.core.Sp
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathOperation
import androidx.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class MultiParagraphIntegrationTest {
    private val fontFamilyMeasureFont = BASIC_MEASURE_FONT.asFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val cursorWidth = 4f

    @Test
    fun empty_string() {
        withDensity(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val text = ""
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = 100.0f)
            )

            assertThat(paragraph.width, equalTo(100.0f))

            assertThat(paragraph.height, equalTo(fontSizeInPx))
            // defined in sample_font
            assertThat(paragraph.firstBaseline, equalTo(fontSizeInPx * 0.8f))
            assertThat(paragraph.lastBaseline, equalTo(fontSizeInPx * 0.8f))
            assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0f))
            assertThat(paragraph.minIntrinsicWidth, equalTo(0.0f))
        }
    }

    @Test
    fun single_line_default_values() {
        withDensity(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value

            for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
                val paragraph = simpleMultiParagraph(
                    text = text,
                    fontSize = fontSize,
                    // width greater than text width - 150
                    constraints = ParagraphConstraints(width = 200.0f)
                )

                assertThat(text, paragraph.width, equalTo(200.0f))
                assertThat(text, paragraph.height, equalTo(fontSizeInPx))
                // defined in sample_font
                assertThat(text, paragraph.firstBaseline, equalTo(fontSizeInPx * 0.8f))
                assertThat(text, paragraph.lastBaseline, equalTo(fontSizeInPx * 0.8f))
                assertThat(
                    text,
                    paragraph.maxIntrinsicWidth,
                    equalTo(fontSizeInPx * text.length)
                )
                assertThat(
                    text, paragraph.minIntrinsicWidth,
                    equalTo(text.length * fontSizeInPx)
                )
            }
        }
    }

    @Test
    fun line_break_default_values() {
        withDensity(defaultDensity) {
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value

            for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
                val paragraph = simpleMultiParagraph(
                    text = text,
                    fontSize = fontSize,
                    // 3 chars width
                    constraints = ParagraphConstraints(width = 3 * fontSizeInPx)
                )

                // 3 chars
                assertThat(text, paragraph.width, equalTo(3 * fontSizeInPx))
                // 2 lines, 1 line gap
                assertThat(
                    text,
                    paragraph.height,
                    equalTo(2 * fontSizeInPx + fontSizeInPx / 5.0f)
                )
                // defined in sample_font
                assertThat(text, paragraph.firstBaseline, equalTo(fontSizeInPx * 0.8f))
                assertThat(
                    text,
                    paragraph.lastBaseline,
                    equalTo(fontSizeInPx + fontSizeInPx / 5.0f + fontSizeInPx * 0.8f)
                )
                assertThat(
                    text,
                    paragraph.maxIntrinsicWidth,
                    equalTo(fontSizeInPx * text.length)
                )
                assertThat(text, paragraph.minIntrinsicWidth, equalTo(text.length * fontSizeInPx))
            }
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph = simpleMultiParagraph(
            text = text,
            maxLines = maxLines,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.didExceedMaxLines, equalTo(true))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph = simpleMultiParagraph(
            text = text,
            maxLines = maxLines,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph = simpleMultiParagraph(
            text = text,
            maxLines = maxLines,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        withDensity(defaultDensity) {
            val text = "aa"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val maxLines = 1
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                maxLines = maxLines,
                // One line can only contain 1 character
                constraints = ParagraphConstraints(width = fontSizeInPx)
            )

            assertThat(paragraph.didExceedMaxLines, equalTo(true))
        }
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph = simpleMultiParagraph(
            text = text,
            fontSize = 50.sp,
            maxLines = maxLines,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        withDensity(defaultDensity) {
            val text = "aa"
            val maxLines = 3
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                maxLines = maxLines,
                // One line can only contain 1 character
                constraints = ParagraphConstraints(width = fontSizeInPx)
            )

            assertThat(paragraph.didExceedMaxLines, equalTo(false))
        }
    }

    @Test
    fun getOffsetForPosition_ltr() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars 0, 1, 2 ...
            for (i in 0..text.length) {
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx / 2).px)
                val offset = paragraph.getOffsetForPosition(position)
                assertThat(
                    "offset at index $i, position $position does not match",
                    offset,
                    equalTo(i)
                )
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars .., 2, 1, 0
            for (i in 0..text.length) {
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx / 2).px)
                val offset = paragraph.getOffsetForPosition(position)
                assertThat(
                    "offset at index $i, position $position does not match",
                    offset,
                    equalTo(text.length - i)
                )
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_multiline() {
        withDensity(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = firstLine.length * fontSizeInPx)
            )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in 0..secondLine.length) {
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx * 1.5f).px)
                val offset = paragraph.getOffsetForPosition(position)
                assertThat(
                    "offset at index $i, position $position, second line does not match",
                    offset,
                    equalTo(i + firstLine.length)
                )
            }
        }
    }

    @Test
    fun getOffsetForPosition_rtl_multiline() {
        withDensity(defaultDensity) {
            val firstLine = "\u05D0\u05D1\u05D2"
            val secondLine = "\u05D3\u05D4\u05D5"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = firstLine.length * fontSizeInPx)
            )

            // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
            // which maps to chars 5, 4, 3
            for (i in 0..secondLine.length) {
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx * 1.5f).px)
                val offset = paragraph.getOffsetForPosition(position)
                assertThat(
                    "offset at index $i, position $position, second line does not match",
                    offset,
                    equalTo(text.length - i)
                )
            }
        }
    }

    @Test
    fun getOffsetForPosition_ltr_width_outOfBounds() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            // greater than width
            var position = PxPosition((fontSizeInPx * text.length * 2).px, (fontSizeInPx / 2).px)
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset, equalTo(text.length))

            // negative
            position = PxPosition((-1 * fontSizeInPx).px, (fontSizeInPx / 2).px)
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset, equalTo(0))
        }
    }

    @Test
    fun getOffsetForPosition_ltr_height_outOfBounds() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            // greater than height
            var position = PxPosition((fontSizeInPx / 2).px, (fontSizeInPx * text.length * 2).px)
            var offset = paragraph.getOffsetForPosition(position)
            assertThat(offset, equalTo(0))

            // negative
            position = PxPosition((fontSizeInPx / 2).px, (-1 * fontSizeInPx).px)
            offset = paragraph.getOffsetForPosition(position)
            assertThat(offset, equalTo(0))
        }
    }

    @Test
    fun getOffsetForPosition_lineBreak() {
        withDensity(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            assertThat(
                paragraph.getOffsetForPosition(PxPosition((3 * fontSizeInPx).px, 0.px)),
                equalTo(3)
            )

            assertThat(
                paragraph.getOffsetForPosition(PxPosition(0.px, (fontSizeInPx * 1.5f).px)),
                equalTo(4)
            )
        }
    }

    @Test
    fun getOffsetForPosition_multiple_paragraph() {
        withDensity(defaultDensity) {
            val text = "abcdef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                paragraphStyles = listOf(
                    AnnotatedString.Item(ParagraphStyle(), 0, 3)
                ),
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in 0 until 3) {
                assertThat(
                    paragraph.getOffsetForPosition(PxPosition((i * fontSizeInPx).px, 0.px)),
                    equalTo(i)
                )
            }

            for (i in 3 until 6) {
                assertThat(
                    paragraph.getOffsetForPosition(
                        PxPosition(((i - 3) * fontSizeInPx).px, fontSizeInPx.px)
                    ),
                    equalTo(i)
                )
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_singleLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in text.indices) {
                val box = paragraph.getBoundingBox(i)
                assertThat(box.left, equalTo(i * fontSizeInPx))
                assertThat(box.right, equalTo((i + 1) * fontSizeInPx))
                assertThat(box.top, equalTo(0f))
                assertThat(box.bottom, equalTo(fontSizeInPx))
            }
        }
    }

    @Test
    fun getBoundingBox_ltr_multiLines() {
        withDensity(defaultDensity) {
            val firstLine = "abc"
            val secondLine = "def"
            val text = firstLine + secondLine
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = firstLine.length * fontSizeInPx)
            )

            // test positions are 3, 4, 5 and always on the second line
            // which maps to chars 3, 4, 5
            for (i in secondLine.indices) {
                val textPosition = i + firstLine.length
                val box = paragraph.getBoundingBox(textPosition)
                assertThat(box.left, equalTo(i * fontSizeInPx))
                assertThat(box.right, equalTo((i + 1) * fontSizeInPx))
                assertThat(box.top, equalTo(fontSizeInPx))
                assertThat(box.bottom, equalTo((2f + 1 / 5f) * fontSizeInPx))
            }
        }
    }

    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    fun getBoundingBox_ltr_textPosition_negative() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            paragraph.getBoundingBox(-1)
        }
    }

    @Suppress
    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    fun getBoundingBox_ltr_textPosition_larger_than_length_throw_exception() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            val textPosition = text.length + 1
            paragraph.getBoundingBox(textPosition)
        }
    }

    @Test(expected = java.lang.AssertionError::class)
    fun getCursorRect_larger_than_length_throw_exception() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text, fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            paragraph.getCursorRect(text.length + 1)
        }
    }

    @Test(expected = java.lang.AssertionError::class)
    fun getCursorRect_negative_throw_exception() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            paragraph.getCursorRect(-1)
        }
    }

    @Test
    fun getCursorRect_ltr_singleLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in text.indices) {
                val cursorRect = paragraph.getCursorRect(i)
                val cursorXOffset = i * fontSizeInPx
                assertThat(
                    cursorRect,
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = 0f,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx
                        )
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_ltr_multiLines() {
        withDensity(defaultDensity) {
            val text = "abcdef"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val charsPerLine = 3
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = charsPerLine * fontSizeInPx)
            )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = i * fontSizeInPx
                assertThat(
                    paragraph.getCursorRect(i),
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = 0f,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx
                        )
                    )
                )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (i % charsPerLine) * fontSizeInPx
                assertThat(
                    paragraph.getCursorRect(i),
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = fontSizeInPx,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx * 2.2f
                        )
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_singleLine() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in text.indices) {
                val cursorXOffset = (text.length - i) * fontSizeInPx
                assertThat(
                    paragraph.getCursorRect(i),
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = 0f,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx
                        )
                    )
                )
            }
        }
    }

    @Test
    fun getCursorRect_rtl_multiLines() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val charsPerLine = 3
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = charsPerLine * fontSizeInPx)
            )

            for (i in 0 until charsPerLine) {
                val cursorXOffset = (charsPerLine - i) * fontSizeInPx
                assertThat(
                    paragraph.getCursorRect(i),
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = 0f,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx
                        )
                    )
                )
            }

            for (i in charsPerLine until text.length) {
                val cursorXOffset = (charsPerLine - i % charsPerLine) * fontSizeInPx
                assertThat(
                    paragraph.getCursorRect(i),
                    equalTo(
                        Rect(
                            left = cursorXOffset - cursorWidth / 2,
                            top = fontSizeInPx,
                            right = cursorXOffset + cursorWidth / 2,
                            bottom = fontSizeInPx * 2.2f
                        )
                    )
                )
            }
        }
    }

    @Test
    fun getPrimaryHorizontal_ltr_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in 0..text.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }
        }
    }

    @Test
    fun getPrimaryHorizontal_rtl_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(width - fontSizeInPx * i)
                )
            }
        }
    }

    @Test
    fun getPrimaryHorizontal_Bidi_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..ltrText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            for (i in 1 until rtlText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i + ltrText.length),
                    equalTo(width - fontSizeInPx * i)
                )
            }

            assertThat(
                paragraph.getPrimaryHorizontal(text.length),
                equalTo(width)
            )
        }
    }

    @Test
    fun getPrimaryHorizontal_ltr_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(0), equalTo(width))

            for (i in 1 until text.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getPrimaryHorizontal_rtl_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(0), equalTo(0f))

            for (i in 1 until text.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(width - fontSizeInPx * i)
                )
            }

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(width))
        }
    }

    @Test
    fun getPrimaryHorizontal_Bidi_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..ltrText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            for (i in 1 until rtlText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i + ltrText.length),
                    equalTo(width - fontSizeInPx * i)
                )
            }

            assertThat(
                paragraph.getPrimaryHorizontal(text.length),
                equalTo(width)
            )
        }
    }

    @Test
    fun getPrimaryHorizontal_Bidi_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(0), equalTo(width))
            // Notice that abc is
            for (i in 1 until ltrText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i),
                    equalTo(rtlText.length * fontSizeInPx + i * fontSizeInPx)
                )
            }

            for (i in 0..rtlText.length) {
                assertThat(
                    paragraph.getPrimaryHorizontal(i + ltrText.length),
                    equalTo(rtlText.length * fontSizeInPx - i * fontSizeInPx)
                )
            }
        }
    }

    @Test
    fun getPrimaryHorizontal_ltr_newLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getPrimaryHorizontal_rtl_newLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getPrimaryHorizontal_ltr_newLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(width))
        }
    }

    @Test
    fun getPrimaryHorizontal_rtl_newLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getPrimaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getSecondaryHorizontal_ltr_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = text.length * fontSizeInPx)
            )

            for (i in 0..text.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }
        }
    }

    @Test
    fun getSecondaryHorizontal_rtl_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(width - fontSizeInPx * i)
                )
            }
        }
    }

    @Test
    fun getSecondaryHorizontal_Bidi_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in ltrText.indices) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            for (i in 0..rtlText.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i + ltrText.length),
                    equalTo(width - fontSizeInPx * i)
                )
            }
        }
    }

    @Test
    fun getSecondaryHorizontal_ltr_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(0), equalTo(0f))

            for (i in 1 until text.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(width))
        }
    }

    @Test
    fun getSecondaryHorizontal_rtl_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(0), equalTo(width))

            for (i in 1 until text.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(width - fontSizeInPx * i)
                )
            }

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getSecondaryHorizontal_Bidi_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in ltrText.indices) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(fontSizeInPx * i)
                )
            }

            for (i in rtlText.indices) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i + ltrText.length),
                    equalTo(width - fontSizeInPx * i)
                )
            }

            assertThat(
                paragraph.getSecondaryHorizontal(text.length),
                equalTo(width - rtlText.length * fontSizeInPx)
            )
        }
    }

    @Test
    fun getSecondaryHorizontal_Bidi_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(
                paragraph.getSecondaryHorizontal(0),
                equalTo(width - ltrText.length * fontSizeInPx)
            )
            for (i in 1..ltrText.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i),
                    equalTo(rtlText.length * fontSizeInPx + i * fontSizeInPx)
                )
            }

            for (i in 1..rtlText.length) {
                assertThat(
                    paragraph.getSecondaryHorizontal(i + ltrText.length),
                    equalTo(rtlText.length * fontSizeInPx - i * fontSizeInPx)
                )
            }
        }
    }

    @Test
    fun getSecondaryHorizontal_ltr_newLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getSecondaryHorizontal_rtl_newLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getSecondaryHorizontal_ltr_newLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(width))
        }
    }

    @Test
    fun getSecondaryHorizontal_rtl_newLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getSecondaryHorizontal(text.length), equalTo(0f))
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getParagraphDirection_ltr_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in text.indices) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getParagraphDirection_rtl_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getParagraphDirection_Bidi_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getParagraphDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getBidiRunDirection_ltr_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.length) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Ltr))
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in text.indices) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getBidiRunDirection_rtl_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2\n"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0 until text.length - 1) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Rtl))
            }
            assertThat(paragraph.getBidiRunDirection(text.length - 1), equalTo(TextDirection.Ltr))
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionDefault() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Ltr))
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionLtr() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                constraints = ParagraphConstraints(width)
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Ltr))
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getBidiRunDirection_Bidi_singleLine_textDirectionRtl() {
        withDensity(defaultDensity) {
            val ltrText = "abc"
            val rtlText = "\u05D0\u05D1\u05D2"
            val text = ltrText + rtlText
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                constraints = ParagraphConstraints(width)
            )

            for (i in ltrText.indices) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Ltr))
            }

            for (i in ltrText.length until text.length) {
                assertThat(paragraph.getBidiRunDirection(i), equalTo(TextDirection.Rtl))
            }
        }
    }

    @Test
    fun getLineForOffset_singleLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.lastIndex) {
                assertThat(paragraph.getLineForOffset(i), equalTo(0))
            }
        }
    }

    @Test
    fun getLineForOffset_multiLines() {
        withDensity(defaultDensity) {
            val text = "a\nb\nc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            for (i in 0..text.lastIndex) {
                assertThat(paragraph.getLineForOffset(i), equalTo(i / 2))
            }
        }
    }

    @Test
    fun getLineForOffset_multiParagraph() {
        withDensity(defaultDensity) {
            val text = "abcd"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                paragraphStyles = listOf(AnnotatedString.Item(ParagraphStyle(), 0, 2)),
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getLineForOffset(0), equalTo(0))
            assertThat(paragraph.getLineForOffset(1), equalTo(0))
            assertThat(paragraph.getLineForOffset(2), equalTo(1))
            assertThat(paragraph.getLineForOffset(3), equalTo(1))
        }
    }

    @Test
    fun getLineForOffset_emptyParagraph() {
        withDensity(defaultDensity) {
            val text = "abcd"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                paragraphStyles = listOf(AnnotatedString.Item(ParagraphStyle(), 2, 2)),
                constraints = ParagraphConstraints(width)
            )

            assertThat(paragraph.getLineForOffset(0), equalTo(0))
            assertThat(paragraph.getLineForOffset(1), equalTo(0))
            // The empty paragraph takes one line
            assertThat(paragraph.getLineForOffset(2), equalTo(2))
            assertThat(paragraph.getLineForOffset(3), equalTo(2))
        }
    }

    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    fun getLineForOffset_negativeOffset() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            paragraph.getLineForOffset(-1)
        }
    }

    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    fun getLineForOffset_outOfBoundary() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 50.sp
            val fontSizeInPx = fontSize.toPx().value
            val width = text.length * fontSizeInPx
            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width)
            )

            paragraph.getLineForOffset(text.length)
        }
    }

    @Test
    fun testGetPathForRange_singleLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft,
                    0f,
                    lineRight - fontSizeInPx,
                    fontSizeInPx
                )
            )

            // Select "ab"
            val actualPath = paragraph.getPathForRange(0, 2)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_multiLines() {
        withDensity(defaultDensity) {
            val text = "abc\nabc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val firstLineLeft = paragraph.getLineLeft(0)
            val secondLineLeft = paragraph.getLineLeft(1)
            val firstLineRight = paragraph.getLineRight(0)
            val secondLineRight = paragraph.getLineRight(1)
            expectedPath.addRect(
                Rect(
                    firstLineLeft + fontSizeInPx,
                    0f,
                    firstLineRight,
                    fontSizeInPx
                )
            )
            expectedPath.addRect(
                Rect(
                    secondLineLeft,
                    fontSizeInPx,
                    secondLineRight - fontSizeInPx,
                    paragraph.height
                )
            )

            // Select "bc\nab"
            val actualPath = paragraph.getPathForRange(1, 6)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Bidi() {
        withDensity(defaultDensity) {
            val textLTR = "Hello"
            val textRTL = "שלום"
            val text = textLTR + textRTL
            val selectionLTRStart = 2
            val selectionRTLEnd = 2
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft + selectionLTRStart * fontSizeInPx,
                    0f,
                    lineLeft + textLTR.length * fontSizeInPx,
                    fontSizeInPx
                )
            )
            expectedPath.addRect(
                Rect(
                    lineRight - selectionRTLEnd * fontSizeInPx,
                    0f,
                    lineRight,
                    fontSizeInPx
                )
            )

            // Select "llo..של"
            val actualPath =
                paragraph.getPathForRange(selectionLTRStart, textLTR.length + selectionRTLEnd)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Start_Equals_End_Returns_Empty_Path() {
        val text = "abc"
        val paragraph = simpleMultiParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        val actualPath = paragraph.getPathForRange(1, 1)

        assertThat(actualPath.getBounds(), equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Empty_Text() {
        val text = ""
        val paragraph = simpleMultiParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        val actualPath = paragraph.getPathForRange(0, 0)

        assertThat(actualPath.getBounds(), equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_Second_Character_Selected() {
        withDensity(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight, fontSizeInPx))

            // Try to select "\uDD1E\uD834\uDD1F", only "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_End_Middle_Second_Character_Selected() {
        withDensity(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight, fontSizeInPx))

            // Try to select "\uDD1E\uD834", actually "\uD834\uDD1F" is selected.
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_End_Same_Character_Returns_Line_Segment() {
        withDensity(defaultDensity) {
            val text = "\uD834\uDD1E\uD834\uDD1F"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight / 2, fontSizeInPx))

            // Try to select "\uDD1E", get vertical line segment after this character.
            val actualPath = paragraph.getPathForRange(1, 2)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Emoji_Sequence() {
        withDensity(defaultDensity) {
            val text = "\u1F600\u1F603\u1F604\u1F606"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(
                Rect(
                    lineLeft + fontSizeInPx,
                    0f,
                    lineRight - fontSizeInPx,
                    fontSizeInPx
                )
            )

            // Select "\u1F603\u1F604"
            val actualPath = paragraph.getPathForRange(1, text.length - 1)

            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Unicode_200D_Return_Line_Segment() {
        withDensity(defaultDensity) {
            val text = "\u200D"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft, equalTo(lineRight))
            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetPathForRange_Unicode_2066_Return_Line_Segment() {
        withDensity(defaultDensity) {
            val text = "\u2066"
            val fontSize = 20f.sp
            val fontSizeInPx = fontSize.toPx().value
            val paragraph = simpleMultiParagraph(
                text = text,
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            val expectedPath = Path()
            val lineLeft = paragraph.getLineLeft(0)
            val lineRight = paragraph.getLineRight(0)
            expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSizeInPx))

            val actualPath = paragraph.getPathForRange(0, 1)

            assertThat(lineLeft, equalTo(lineRight))
            val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
            assertThat(diff, equalTo(Rect.zero))
        }
    }

    @Test
    fun testGetWordBoundary() {
        val text = "abc def"
        val paragraph = simpleMultiParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        val result = paragraph.getWordBoundary(text.indexOf('a'))

        assertThat(result.start, equalTo(text.indexOf('a')))
        assertThat(result.end, equalTo(text.indexOf(' ')))
    }

    @Test
    fun testGetWordBoundary_Bidi() {
        val text = "abc \u05d0\u05d1\u05d2 def"
        val paragraph = simpleMultiParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        val resultEnglish = paragraph.getWordBoundary(text.indexOf('a'))
        val resultHebrew = paragraph.getWordBoundary(text.indexOf('\u05d1'))

        assertThat(resultEnglish.start, equalTo(text.indexOf('a')))
        assertThat(resultEnglish.end, equalTo(text.indexOf(' ')))
        assertThat(resultHebrew.start, equalTo(text.indexOf('\u05d0')))
        assertThat(resultHebrew.end, equalTo(text.indexOf('\u05d2') + 1))
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_start_larger_than_end() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(
            text = text,
            constraints = ParagraphConstraints(Float.MAX_VALUE)
        )

        paragraph.getPathForRange(textEnd, textStart)
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_start_is_smaller_than_zero() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(
            text = text,
            constraints = ParagraphConstraints(Float.MAX_VALUE)
        )

        paragraph.getPathForRange(textStart - 2, textEnd - 1)
    }

    @Test(expected = AssertionError::class)
    fun getPathForRange_throws_exception_if_end_is_larger_than_text_length() {
        val text = "ab"
        val textStart = 0
        val textEnd = text.length
        val paragraph = simpleMultiParagraph(
            text = text,
            constraints = ParagraphConstraints(Float.MAX_VALUE)
        )

        paragraph.getPathForRange(textStart, textEnd + 1)
    }

    @Test
    fun textAlign_defaultValue_alignsStart() {
        withDensity(defaultDensity) {
            val textLTR = "aa"
            val textRTL = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val layoutLTRWidth = (textLTR.length + 2) * fontSizeInPx
            val paragraphLTR = simpleMultiParagraph(
                text = textLTR,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutLTRWidth)
            )

            val layoutRTLWidth = (textRTL.length + 2) * fontSizeInPx
            val paragraphRTL = simpleMultiParagraph(
                text = textRTL,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutRTLWidth)
            )

            // When textAlign is TextAlign.start, LTR aligns to left, RTL aligns to right.
            assertThat(paragraphLTR.getLineLeft(0), equalTo(0.0f))
            assertThat(paragraphRTL.getLineRight(0), equalTo(layoutRTLWidth))
        }
    }

    @Test
    fun textAlign_whenAlignLeft_returnsZeroForGetLineLeft() {
        withDensity(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleMultiParagraph(
                    text = text,
                    textAlign = TextAlign.Left,
                    fontSize = fontSize,
                    constraints = ParagraphConstraints(width = layoutWidth)
                )

                assertThat(paragraph.getLineLeft(0), equalTo(0.0f))
            }
        }
    }

    @Test
    fun textAlign_whenAlignRight_returnsLayoutWidthForGetLineRight() {
        withDensity(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleMultiParagraph(
                    text = text,
                    textAlign = TextAlign.Right,
                    fontSize = fontSize,
                    constraints = ParagraphConstraints(width = layoutWidth)
                )

                assertThat(paragraph.getLineRight(0), equalTo(layoutWidth))
            }
        }
    }

    @Test
    fun textAlign_whenAlignCenter_textIsCentered() {
        withDensity(defaultDensity) {
            val texts = listOf("aa", "\u05D0\u05D0")
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            texts.map { text ->
                val layoutWidth = (text.length + 2) * fontSizeInPx
                val paragraph = simpleMultiParagraph(
                    text = text,
                    textAlign = TextAlign.Center,
                    fontSize = fontSize,
                    constraints = ParagraphConstraints(width = layoutWidth)
                )

                val textWidth = text.length * fontSizeInPx
                assertThat(
                    paragraph.getLineLeft(0),
                    equalTo(layoutWidth / 2 - textWidth / 2)
                )
                assertThat(
                    paragraph.getLineRight(0),
                    equalTo(layoutWidth / 2 + textWidth / 2)
                )
            }
        }
    }

    @Test
    fun textAlign_whenAlignStart_withLTR_returnsZeroForGetLineLeft() {
        withDensity(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textAlign = TextAlign.Start,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.getLineLeft(0), equalTo(0.0f))
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withLTR_returnsLayoutWidthForGetLineRight() {
        withDensity(defaultDensity) {
            val text = "aa"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textAlign = TextAlign.End,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.getLineRight(0), equalTo(layoutWidth))
        }
    }

    @Test
    fun textAlign_whenAlignStart_withRTL_returnsLayoutWidthForGetLineRight() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textAlign = TextAlign.Start,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.getLineRight(0), equalTo(layoutWidth))
        }
    }

    @Test
    fun textAlign_whenAlignEnd_withRTL_returnsZeroForGetLineLeft() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D0"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = (text.length + 2) * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textAlign = TextAlign.End,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.getLineLeft(0), equalTo(0.0f))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    // We have to test justification above API 28 because of this bug b/68009059, where devices
    // before API 28 may have an extra space at the end of line.
    fun textAlign_whenAlignJustify_justifies() {
        withDensity(defaultDensity) {
            val text = "a a a"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = ("a a".length + 1) * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textAlign = TextAlign.Justify,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.getLineLeft(0), equalTo(0.0f))
            assertThat(paragraph.getLineRight(0), equalTo(layoutWidth))
            // Last line should align start
            assertThat(paragraph.getLineLeft(1), equalTo(0.0f))
        }
    }

    @Test
    fun textDirection_whenLTR_dotIsOnRight() {
        withDensity(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceLtr,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            // The position of the last character in display order.
            val position = PxPosition(("a.".length * fontSizeInPx + 1).px, (fontSizeInPx / 2).px)
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex, equalTo(2))
        }
    }

    @Test
    fun textDirection_whenRTL_dotIsOnLeft() {
        withDensity(defaultDensity) {
            val text = "a.."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textDirectionAlgorithm = TextDirectionAlgorithm.ForceRtl,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            // The position of the first character in display order.
            val position = PxPosition((fontSizeInPx / 2 + 1).px, (fontSizeInPx / 2).px)
            val charIndex = paragraph.getOffsetForPosition(position)
            assertThat(charIndex, equalTo(2))
        }
    }

    @Test
    fun textDirection_whenDefault_withoutStrongChar_directionIsLTR() {
        withDensity(defaultDensity) {
            val text = "..."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            for (i in 0..text.length) {
                // The position of the i-th character in display order.
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx / 2).px)
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex, equalTo(i))
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharLTR_directionIsLTR() {
        withDensity(defaultDensity) {
            val text = "a\u05D0."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            for (i in text.indices) {
                // The position of the i-th character in display order.
                val position = PxPosition((i * fontSizeInPx + 1).px, (fontSizeInPx / 2).px)
                val charIndex = paragraph.getOffsetForPosition(position)
                assertThat(charIndex, equalTo(i))
            }
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharRTL_directionIsRTL() {
        withDensity(defaultDensity) {
            val text = "\u05D0a."
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = text.length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            // The first character in display order should be '.'
            val position = PxPosition((fontSizeInPx / 2 + 1).px, (fontSizeInPx / 2).px)
            val index = paragraph.getOffsetForPosition(position)
            assertThat(index, equalTo(2))
        }
    }

    @Test
    fun lineHeight_returnsSameAsGiven() {
        withDensity(defaultDensity) {
            val text = "abcdefgh"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            // Make the layout 4 lines
            val layoutWidth = text.length * fontSizeInPx / 4
            val lineHeight = 1.5f

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            assertThat(paragraph.lineCount, equalTo(4))
            // TODO(Migration/haoyuchang): Due to bug b/120530738, the height of the first line is
            // wrong in the framework. Will fix it when the lineHeight in TextSpan is implemented.
            for (i in 1 until paragraph.lineCount - 1) {
                val actualHeight = paragraph.getLineHeight(i)
                // In the sample_font.ttf, the height of the line should be
                // fontSize + 0.2f * fontSize(line gap)
                assertThat(
                    "line number $i",
                    actualHeight,
                    equalTo(1.2f * fontSizeInPx * lineHeight)
                )
            }
        }
    }

    @Test
    fun lineHeight_hasNoEffectOnLastLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val layoutWidth = (text.length - 1) * fontSizeInPx
            val lineHeight = 1.5f

            val paragraph = simpleMultiParagraph(
                text = text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                constraints = ParagraphConstraints(width = layoutWidth)
            )

            val lastLine = paragraph.lineCount - 1
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2 * fontSize(line gap)
            assertThat(paragraph.getLineHeight(lastLine), equalTo(1.2f * fontSizeInPx))
        }
    }

    @Test
    fun textIndent_onSingleLine() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val indent = 20.0f

            val paragraph = simpleMultiParagraph(
                text = text,
                textIndent = TextIndent(firstLine = indent.px),
                fontSize = fontSize,
                fontFamily = fontFamilyMeasureFont,
                constraints = ParagraphConstraints(width = Float.MAX_VALUE)
            )

            // This position should point to the first character 'a' if indent is applied.
            // Otherwise this position will point to the second character 'b'.
            val position = PxPosition((indent + 1).px, (fontSizeInPx / 2).px)
            // The offset corresponding to the position should be the first char 'a'.
            assertThat(paragraph.getOffsetForPosition(position), equalTo(0))
        }
    }

    @Test
    fun textIndent_onFirstLine() {
        withDensity(defaultDensity) {
            val text = "abcdef"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val indent = 20.0f
            val paragraphWidth = "abcd".length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textIndent = TextIndent(firstLine = indent.px),
                fontSize = fontSize,
                fontFamily = fontFamilyMeasureFont,
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            assertThat(paragraph.lineCount, equalTo(2))
            // This position should point to the first character of the first line if indent is
            // applied. Otherwise this position will point to the second character of the second line.
            val position = PxPosition((indent + 1).px, (fontSizeInPx / 2).px)
            // The offset corresponding to the position should be the first char 'a'.
            assertThat(paragraph.getOffsetForPosition(position), equalTo(0))
        }
    }

    @Test
    fun textIndent_onRestLine() {
        withDensity(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val indent = 20.0f
            val paragraphWidth = "abc".length * fontSizeInPx

            val paragraph = simpleMultiParagraph(
                text = text,
                textIndent = TextIndent(
                    firstLine = 0.px,
                    restLine = indent.px
                ),
                fontSize = fontSize,
                fontFamily = fontFamilyMeasureFont,
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            // This position should point to the first character of the second line if indent is
            // applied. Otherwise this position will point to the second character of the second line.
            val position = PxPosition((indent + 1).px, (fontSizeInPx / 2 + fontSizeInPx).px)
            // The offset corresponding to the position should be the 'd' in the second line.
            assertThat(
                paragraph.getOffsetForPosition(position),
                equalTo("abcd".length - 1)
            )
        }
    }

    private fun simpleMultiParagraph(
        text: String = "",
        textIndent: TextIndent? = null,
        textAlign: TextAlign? = null,
        fontSize: Sp? = null,
        maxLines: Int? = null,
        lineHeight: Float? = null,
        textStyles: List<AnnotatedString.Item<TextStyle>> = listOf(),
        paragraphStyles: List<AnnotatedString.Item<ParagraphStyle>> = listOf(),
        fontFamily: FontFamily = fontFamilyMeasureFont,
        localeList: LocaleList? = null,
        textStyle: TextStyle? = null,
        constraints: ParagraphConstraints,
        density: Density? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        textDirectionAlgorithm: TextDirectionAlgorithm? = null
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = AnnotatedString(
                text = text,
                textStyles = textStyles,
                paragraphStyles = paragraphStyles
            ),
            textStyle = TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize,
                localeList = localeList
            ).merge(textStyle),
            paragraphStyle = ParagraphStyle(
                textIndent = textIndent,
                textDirectionAlgorithm = textDirectionAlgorithm,
                textAlign = textAlign,
                lineHeight = lineHeight
            ),
            maxLines = maxLines,
            constraints = constraints,
            density = density ?: defaultDensity,
            layoutDirection = layoutDirection,
            resourceLoader = TestFontResourceLoader(context)
        )
    }
}
