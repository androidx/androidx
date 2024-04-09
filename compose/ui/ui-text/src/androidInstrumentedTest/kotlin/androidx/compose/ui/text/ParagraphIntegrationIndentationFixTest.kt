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
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntegrationIndentationFixTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val lastLine = 2
    private val fontSize = 10
    private val letterSpacing = 5
    private val emLetterSpacing = (letterSpacing.toFloat() / fontSize).em
    private val charWidth = fontSize + letterSpacing
    private val repeatCount = 20
    private val ltrChar = "a"
    private val rtlChar = "\u05D0"
    private val lineStartOffsets = arrayOf(0, 3, 6)

    private fun hasEdgeLetterSpacingBugFix(): Boolean {
        val text = "a"
        val singleLetterLetterSpacing = paragraph(
            text = text,
            letterSpacing = 10.sp)

        val singleLetterWithoutLetterSpacing = paragraph(
            text = text,
            letterSpacing = 0.sp)

        // If the platform has a letter spacing fix, the letter spacing will not be added before and
        // after the visually left most letter and visually right most letter. Therefore, if the fix
        // is available, the letter spacing is no-op for single letter text.
        return singleLetterLetterSpacing.getLineWidth(0) ==
            singleLetterWithoutLetterSpacing.getLineWidth(0)
    }

    @Test
    fun getLineLeftAndGetLineRight_Ltr() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount))
        for (line in 0 until paragraph.lineCount) {
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
                assertThat(paragraph.getLineLeft(line)).isEqualTo(letterSpacing.toFloat())
            } else {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
                assertThat(paragraph.getLineLeft(line)).isEqualTo(0f)
            }
        }
    }

    @Test
    fun getLineLeftAndGetLineRight_Rtl() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount))
        for (line in 0 until paragraph.lineCount) {
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineLeft(line)).isEqualTo(0)
                assertThat(paragraph.getLineRight(line))
                    .isEqualTo(paragraph.width - letterSpacing.toFloat())
            } else {
                assertThat(paragraph.getLineLeft(line)).isEqualTo(0)
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 23) // b/266743243
    @Test
    fun getLineLeftAndGetLineRight_Ltr_TextIndent() {
        val paragraph = paragraph(
            text = ltrChar.repeat(repeatCount),
            textIndent = TextIndent(firstLine = charWidth.sp, restLine = charWidth.sp)
        )
        for (line in 0 until paragraph.lineCount) {
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
                val expectedLeft =
                    if (line == paragraph.lineCount - 1) {
                        letterSpacing - (charWidth - letterSpacing.toFloat() / 2)
                    } else {
                        letterSpacing
                    }
                assertThat(paragraph.getLineLeft(line)).isEqualTo(expectedLeft)
            } else {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
                val expectedLeft = if (line == paragraph.lineCount - 1) -charWidth else 0f
                assertThat(paragraph.getLineLeft(line)).isEqualTo(expectedLeft)
            }
        }
    }

    @Test
    fun getHorizontalPosition_Ltr() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount))
        if (hasEdgeLetterSpacingBugFix()) {
            lineStartOffsets.forEach { offset ->
                // If the platform has letter spacing fix, the letter spacings for the left most and
                // right most letters are omitted. Therefore the total line lengths becomes shorter
                // and line starting positions becomes right because the paragraph is end (right)
                // aligned.
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(letterSpacing.toFloat())

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(letterSpacing.toFloat())
            }
        } else {
            lineStartOffsets.forEach { offset ->
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(0f)

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(0f)
            }
        }
    }

    @Test
    fun getHorizontalPosition_Rtl() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount))
        if (hasEdgeLetterSpacingBugFix()) {
            lineStartOffsets.forEach { offset ->
                // If the platform has letter spacing fix, the letter spacings for the left most and
                // right most letters are omitted. Therefore the total line lengths becomes shorter
                // and line starting positions becomes left because the paragraph is end (left)
                // aligned.
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(paragraph.width - letterSpacing.toFloat())

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(paragraph.width - letterSpacing.toFloat())
            }
        } else {
            lineStartOffsets.forEach { offset ->
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(paragraph.width)

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(paragraph.width)
            }
        }
    }

    @Test
    fun getOffsetForPosition_Ltr() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount))
        for (line in 0 until paragraph.lineCount) {
            assertThat(
                paragraph.getOffsetForPosition(Offset(1f, line * fontSize + 1f))
            ).isEqualTo(lineStartOffsets[line])
        }
    }

    @Test
    fun getOffsetForPosition_Rtl() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount))
        for (line in 0 until paragraph.lineCount) {
            assertThat(
                paragraph.getOffsetForPosition(Offset(paragraph.width - 1f, line * fontSize + 1f))
            ).isEqualTo(lineStartOffsets[line])
        }
    }

    // letterSpacing in SP is handled by spans, therefore the results are a little off when
    // letterSpacing is in SP. This is actually a bug, but adding tests for visibility
    @Test
    fun getLineLeftAndGetLineRight_Ltr_sp_letterspacing() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        for (line in 0 until paragraph.lineCount) {
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)

                assertThat(paragraph.getLineLeft(line)).isEqualTo(letterSpacing.toFloat())
            } else {
                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)

                assertThat(paragraph.getLineLeft(line)).isEqualTo(0f)
            }
        }
    }

    @Test
    fun getLineLeftAndGetLineRight_Rtl_sp_letterspacing() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        for (line in 0 until paragraph.lineCount) {
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(paragraph.getLineLeft(line)).isEqualTo(0)

                // If the platform has letter spacing fix, the letter spacings for the left most and
                // right most letters are omitted. Therefore the total line lengths becomes shorter
                // and line starting positions becomes left because the paragraph is end (left)
                // aligned.
                assertThat(paragraph.getLineRight(line))
                    .isEqualTo(paragraph.width - letterSpacing.toFloat())
            } else {
                assertThat(paragraph.getLineLeft(line)).isEqualTo(0)

                assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 23) // b/266743243
    @Test
    fun getLineLeftAndGetLineRight_Ltr_TextIndent_sp_letterspacing() {
        val paragraph = paragraph(
            text = ltrChar.repeat(repeatCount),
            textIndent = TextIndent(firstLine = charWidth.sp, restLine = charWidth.sp),
            letterSpacing = letterSpacing.sp
        )
        for (line in 0 until paragraph.lineCount) {
            assertThat(paragraph.getLineRight(line)).isEqualTo(paragraph.width)

            val expectedLeft = if (hasEdgeLetterSpacingBugFix()) {
                 if (line == paragraph.lineCount - 1) {
                    -fontSize + letterSpacing.toFloat() / 2
                } else {
                    letterSpacing
                }
            } else {
                if (line == paragraph.lineCount - 1) {
                    -fontSize - letterSpacing
                } else {
                    0f
                }
            }
            assertThat(paragraph.getLineLeft(line)).isEqualTo(expectedLeft)
        }
    }

    @Test
    fun getHorizontalPosition_Ltr_sp_letterspacing() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        lineStartOffsets.forEach { offset ->
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(letterSpacing.toFloat())

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(letterSpacing.toFloat())
            } else {
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(0f)

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(0f)
            }
        }
    }

    @Test
    fun getHorizontalPosition_Rtl_sp_letterspacing() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        lineStartOffsets.forEach { offset ->
            if (hasEdgeLetterSpacingBugFix()) {
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(paragraph.width - letterSpacing.toFloat())

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(paragraph.width - letterSpacing.toFloat())
            } else {
                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = true)
                ).isEqualTo(paragraph.width)

                assertThat(
                    paragraph.getHorizontalPosition(offset, usePrimaryDirection = false)
                ).isEqualTo(paragraph.width)
            }
        }
    }

    @Test
    fun getOffsetForPosition_Ltr_sp_letterspacing() {
        val paragraph = paragraph(ltrChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        for (line in 0 until paragraph.lineCount) {
            assertThat(
                paragraph.getOffsetForPosition(Offset(1f, line * fontSize + 1f))
            ).isEqualTo(lineStartOffsets[line])
        }
    }

    @Test
    fun getOffsetForPosition_Rtl_sp_letterspacing() {
        val paragraph = paragraph(rtlChar.repeat(repeatCount), letterSpacing = letterSpacing.sp)
        for (line in 0 until paragraph.lineCount) {
            assertThat(
                paragraph.getOffsetForPosition(Offset(paragraph.width - 1f, line * fontSize + 1f))
            ).isEqualTo(lineStartOffsets[line])
        }
    }

    @Test
    fun constructWithEmptyString() {
        // main issue that this is testing is that fact that paragraph construction does not
        // throw exception for empty text
        paragraph(text = "", letterSpacing = letterSpacing.sp)
    }

    private fun paragraph(
        text: String = "",
        textIndent: TextIndent = TextIndent.None,
        letterSpacing: TextUnit = emLetterSpacing
    ): Paragraph {
        val width = charWidth * 3

        return Paragraph(
            text = text,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize.sp,
                textAlign = TextAlign.End,
                letterSpacing = letterSpacing,
                textIndent = textIndent
            ),
            maxLines = lastLine + 1,
            ellipsis = true,
            constraints = Constraints(maxWidth = width),
            density = Density(density = 1f),
            fontFamilyResolver = UncachedFontFamilyResolver(
                InstrumentationRegistry.getInstrumentation().context
            )
        )
    }
}
