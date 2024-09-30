/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AndroidParagraph
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextGranularity
import androidx.compose.ui.text.TextInclusionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UncachedFontFamilyResolver
import androidx.compose.ui.text.ceilToInt
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.rangeOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.min
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidParagraphGetRangeForRectTest {
    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val basicFontFamily = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun getRangeForRect_characterGranularity_containsCenter_singleLine() {
        val fontSize = 10f
        val text = "abcdef"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 lines is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)
        for (start in text.indices) {
            for (end in start + 1..text.length) {
                val startBoundingBox = paragraph.getBoundingBox(start)
                val endBoundingBox = paragraph.getBoundingBox(end - 1)
                val rect = boundingBoxOf(startBoundingBox.center, endBoundingBox.center)
                val range =
                    paragraph.getRangeForRect(
                        rect,
                        TextGranularity.Character,
                        TextInclusionStrategy.ContainsCenter
                    )
                assertThat(range).isEqualTo(TextRange(start, end))
            }
        }
    }

    @Test
    fun getRangeForRect_characterGranularity_containsAll_singleLine() {
        val fontSize = 10f
        val text = "abcdef"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 lines is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)
        for (start in text.indices) {
            for (end in start + 1..text.length) {
                val startBoundingBox = paragraph.getBoundingBox(start)
                val endBoundingBox = paragraph.getBoundingBox(end - 1)
                val rect = boundingBoxOf(startBoundingBox, endBoundingBox)
                val range =
                    paragraph.getRangeForRect(
                        rect,
                        TextGranularity.Character,
                        TextInclusionStrategy.ContainsAll
                    )
                assertThat(range).isEqualTo(TextRange(start, end))
            }
        }
    }

    @Test
    fun getRangeForRect_characterGranularity_anyOverlap_singleLine() {
        val fontSize = 10f
        val text = "abcdef"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 lines is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)
        val lineTop = paragraph.getLineTop(0)
        for (start in text.indices) {
            for (end in start + 1..text.length) {
                val startBoundingBox = paragraph.getBoundingBox(start)
                val endBoundingBox = paragraph.getBoundingBox(end - 1)

                // It's testing any overlap, make top and bottom 1 pixel away.
                val rect =
                    boundingBoxOf(startBoundingBox, endBoundingBox)
                        .copy(top = lineTop, bottom = lineTop + 1)
                val range =
                    paragraph.getRangeForRect(
                        rect,
                        TextGranularity.Character,
                        TextInclusionStrategy.AnyOverlap
                    )
                assertThat(range).isEqualTo(TextRange(start, end))
            }
        }
    }

    @Test
    fun getRangeForRect_characterGranularity_multiLine() {
        val text = "abcdef"
        val fontSize = 10f
        val charPerLine = 3
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = fontSize * charPerLine
            )

        // Precondition check: 2 lines are laid out.
        assertThat(paragraph.lineCount).isEqualTo(2)

        // Character 'b' and 'e' are covered in the rectangle.
        val rect = paragraph.boundingBoxOf(text.indexOf('b'), text.indexOf('e'))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('b', 'e'))
    }

    @Test
    fun getRangeForRect_characterGranularity_singleLine_compoundCharacter() {
        val fontSize = 10f
        val text = "ab\uD83D\uDE03def" // \uD83D\uDE03 is the smiling face emoji
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rect should covers character 'b' and the following smiling emoji.
        val rect = paragraph.boundingBoxOf(text.indexOf('b'), text.indexOf('\uDE03'))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('b', '\uDE03'))
    }

    @Test
    fun getRangeForRect_characterGranularity_BiDi() {
        val fontSize = 10f
        val text = "abc\u05D1\u05D2\u05D3" // rendered in the order of: a b c \u05D3 \u05D2 \u05D1

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rectangle covers character 'c' and also the character \u05D3, characters between
        // them are also included in the final range. The final range is [2, 6)
        val rect = paragraph.boundingBoxOf(text.indexOf('c'), text.indexOf('\u05D3'))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('c', '\u05D3'))
    }

    @Test
    fun getRangeForRect_characterGranularity_singleLine_empty() {
        val fontSize = 10f
        val text = "abcdef"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        val top = paragraph.getLineTop(0)
        val bottom = paragraph.getLineBottom(0)

        val left = text.indexOf('c') * fontSize
        // No character's center point is covered by the rectangle, return null.
        val right = left + fontSize * 0.4f

        val rect = Rect(left, top, right, bottom)
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(TextRange.Zero)
    }

    @Test
    fun getRangeForRect_wordLevel_singleLine_containsCenter() {
        val fontSize = 10f
        val text = "abc def"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        val bBoundingBox = paragraph.getBoundingBox(text.indexOf('b'))

        // This rectangle covers the center of the word "abc".
        val rect = boundingBoxOf(bBoundingBox.center)
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('a', 'c'))
    }

    @Test
    fun getRangeForRect_wordLevel_singleLine_containsAll() {
        val fontSize = 10f
        val text = "abc def hij"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rectangle covers the word "def"'s bounds and partially covers "hij".
        val rect = paragraph.boundingBoxOf(text.indexOf('d'), text.indexOf('i'))
        val range =
            paragraph.getRangeForRect(rect, TextGranularity.Word, TextInclusionStrategy.ContainsAll)

        assertThat(range).isEqualTo(text.rangeOf('d', 'f'))
    }

    @Test
    fun getRangeForRect_wordLevel_singleLine_anyOverlap() {
        val fontSize = 10f
        val text = "abc def hij"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rectangle overlaps with "def"
        val dBoundingBox = paragraph.getBoundingBox(text.indexOf('d'))

        val rect = boundingBoxOf(dBoundingBox.topLeft)
        val range =
            paragraph.getRangeForRect(rect, TextGranularity.Word, TextInclusionStrategy.AnyOverlap)

        assertThat(range).isEqualTo(text.rangeOf('d', 'f'))
    }

    @Test
    fun getRangeForRect_wordLevel_singleLine_excludeSpace() {
        val fontSize = 10f
        val text = "abc def"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rectangle covers only the space character, returned range is null
        val rect = paragraph.getBoundingBox(text.indexOf(' '))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(TextRange.Zero)
    }

    @Test
    fun getRangeForRect_wordLevel_multiLine() {
        val fontSize = 10f
        val charPerLine = 7
        val text = "abc def ghk lmn"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = fontSize * charPerLine
            )

        // Precondition check: 2 lines are laid out.
        assertThat(paragraph.lineCount).isEqualTo(2)

        // This rectangle covers the center of the word "abc" and "ghk".
        val rect = paragraph.boundingBoxOf(text.indexOf('b'), text.indexOf('h'))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('a', 'k'))
    }

    @Test
    fun getRangeForRect_wordLevel_multiLine_excludeSpace() {
        val fontSize = 10f
        val charPerLine = 7
        val text = "abc def g hi"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = fontSize * charPerLine
            )

        // This paragraph is rendered like this:
        //   abc def
        //   g hi

        // Precondition check: 2 lines are laid out.
        assertThat(paragraph.lineCount).isEqualTo(2)

        // The index of the space in the second line is 9.
        // This rectangle covers the center of the word "abc" but only covers the space at the
        // second line, the returned range only covers "abc".
        val rect = paragraph.boundingBoxOf(text.indexOf('b'), 9)
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(text.rangeOf('a', 'c'))
    }

    @Test
    fun getRangeForRect_wordLevel_singleLine_BiDi() {
        val fontSize = 10f
        // it's rendered in the order of:
        //   abc \u05D4\u05D3 \u05D2\u05D1
        val text = "abc \u05D1\u05D2 \u05D3\u05D4"

        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp, fontFamily = basicFontFamily),
                width = Float.MAX_VALUE
            )

        // Precondition check: only 1 line is laid out.
        assertThat(paragraph.lineCount).isEqualTo(1)

        // This rectangle covers the center of the word "abc" and "\u05D3\u05D4",
        // the returned range is [0, text.length)
        val rect = paragraph.boundingBoxOf(text.indexOf('b'), text.indexOf('\u05D3'))
        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(TextRange(0, text.length))
    }

    /**
     * Helper function that returns the minimal [Rect] which contains all the given characters
     * referred by the [offsets].
     */
    private fun Paragraph.boundingBoxOf(vararg offsets: Int): Rect {
        return boundingBoxOf(*offsets.map { getBoundingBox(it) }.toTypedArray())
    }

    /** Helper function that returns the minimal [Rect] which contains all the given [rects]. */
    private fun boundingBoxOf(vararg rects: Rect): Rect {
        return Rect(
            left = rects.minOf { it.left },
            top = rects.minOf { it.top },
            right = rects.maxOf { it.right },
            bottom = rects.maxOf { it.bottom }
        )
    }

    /**
     * Helper function that returns a minimal [Rect] which contains the given point represented in
     * Offset.
     */
    private fun boundingBoxOf(offset: Offset): Rect {
        return Rect(offset.x, offset.y, offset.x + 0.01f, offset.y + 0.01f)
    }

    /**
     * Helper function that returns a minimal [Rect] which contains the given points represented in
     * Offset.
     */
    private fun boundingBoxOf(offset1: Offset, offset2: Offset): Rect {
        val left = min(offset1.x, offset2.x)
        val top = min(offset1.y, offset2.y)
        val right = maxOf(left, offset1.x, offset2.x) + 0.01f
        val bottom = maxOf(top, offset1.y, offset2.y) + 0.01f
        return Rect(left, top, right, bottom)
    }

    private fun simpleParagraph(
        text: String = "",
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        textIndent: TextIndent? = null,
        textAlign: TextAlign = TextAlign.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        width: Float,
        height: Float = Float.POSITIVE_INFINITY,
        style: TextStyle? = null,
        fontFamilyResolver: FontFamily.Resolver = UncachedFontFamilyResolver(context)
    ): AndroidParagraph {
        return AndroidParagraph(
            text = text,
            annotations = spanStyles,
            placeholders = listOf(),
            style =
                TextStyle(
                        fontFamily = basicFontFamily,
                        textAlign = textAlign,
                        textIndent = textIndent
                    )
                    .merge(style),
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = width.ceilToInt(), maxHeight = height.ceilToInt()),
            density = Density(density = 1f),
            fontFamilyResolver = fontFamilyResolver
        )
    }
}
