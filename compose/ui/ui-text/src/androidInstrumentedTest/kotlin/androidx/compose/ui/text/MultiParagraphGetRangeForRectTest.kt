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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphGetRangeForRectTest {
    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val basicFontFamily = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val defaultDensity = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun getRangeForRect_characterGranularity_rectCoversAllParagraphs() {
        val fontSize = 10f
        val text = createAnnotatedString("abc", "def", "ghi")

        // This paragraph is rendered as:
        //   abc
        //   def
        //   ghi
        val paragraph = simpleMultiParagraph(text = text, style = TextStyle(fontSize = fontSize.sp))

        // Precondition check: there 3 lines each corresponding to a paragraph
        assertThat(paragraph.lineCount).isEqualTo(3)

        val top = paragraph.getLineTop(0)
        val bottom = paragraph.getLineBottom(2)
        val left = 10f
        val right = 20f
        // The rect covers character 'b' and 'h'.
        val rect = Rect(left, top, right, bottom)

        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )
        assertThat(range).isEqualTo(text.rangeOf('b', 'h'))
    }

    @Test
    fun getRangeForRect_characterGranularity_rectCoversNothing() {
        val fontSize = 10f
        val text = createAnnotatedString("abc", "def")

        // This paragraph is rendered as:
        //   abc
        //   def
        val paragraph = simpleMultiParagraph(text = text, style = TextStyle(fontSize = fontSize.sp))

        // Precondition check: there 2 lines each corresponding to a paragraph
        assertThat(paragraph.lineCount).isEqualTo(2)

        val top = paragraph.getLineTop(0)
        val bottom = paragraph.getLineBottom(1)
        val left = 10f
        val right = 14f
        // This rectangle doesn't cover any character's center point, return null.
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
    fun getRangeForRect_characterGranularity_rectCoversSingleParagraph() {
        val fontSize = 10f
        val text = createAnnotatedString("abcd", "efg")
        val charPerLine = 3

        val paragraph =
            simpleMultiParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize.sp),
                width = charPerLine * fontSize
            )

        // The input text is rendered as following:
        //   abc
        //   d
        //   efg

        // Precondition check: first paragraph has 2 lines and the second paragraph has 1 line
        assertThat(paragraph.lineCount).isEqualTo(3)

        val top = paragraph.getLineTop(1)
        val bottom = paragraph.getLineBottom(2)
        val left = 10f
        val right = 20f
        // This rectangle doesn't cover anything in the first paragraph.
        // And it covers character 'f' in the second paragraph, the result is [5, 6).
        val rect = Rect(left, top, right, bottom)

        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Character,
                TextInclusionStrategy.ContainsCenter
            )
        assertThat(range).isEqualTo(text.rangeOf('f'))
    }

    @Test
    fun getRangeForRect_wordLevel_rectCoversAllParagraphs() {
        val fontSize = 10f
        val text = createAnnotatedString("ab cd", "ef", "gh ij")

        val paragraph = simpleMultiParagraph(text = text, style = TextStyle(fontSize = fontSize.sp))

        // The input text is rendered as following:
        //   ab cd
        //   ef
        //   gh ij
        // Precondition check: there 3 lines each corresponding to a paragraph
        assertThat(paragraph.lineCount).isEqualTo(3)

        val top = paragraph.getLineTop(0)
        val bottom = paragraph.getLineBottom(2)
        val left = 30f
        val right = 50f
        // The rect covers character 'cd' and 'ij'.
        val rect = Rect(left, top, right, bottom)

        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )
        assertThat(range).isEqualTo(text.rangeOf('c', 'j'))
    }

    @Test
    fun getRangeForRect_wordLevel_excludeSpaces() {
        val fontSize = 10f
        val text = createAnnotatedString("ab cd", "ef gh", "ij kl")

        val paragraph = simpleMultiParagraph(text = text, style = TextStyle(fontSize = fontSize.sp))

        // The input text is rendered as following:
        //   ab cd
        //   ef gh
        //   ij kl
        // Precondition check: there 3 lines each corresponding to a paragraph
        assertThat(paragraph.lineCount).isEqualTo(3)

        val top = paragraph.getLineTop(0)
        val bottom = paragraph.getLineBottom(2)
        val left = 20f
        val right = 30f
        // The rect covers only the spaces. It should return null.
        val rect = Rect(left, top, right, bottom)

        val range =
            paragraph.getRangeForRect(
                rect,
                TextGranularity.Word,
                TextInclusionStrategy.ContainsCenter
            )

        assertThat(range).isEqualTo(TextRange.Zero)
    }

    private fun simpleMultiParagraph(
        text: AnnotatedString,
        style: TextStyle? = null,
        fontSize: TextUnit = TextUnit.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        width: Float = Float.MAX_VALUE,
        localeList: LocaleList? = null
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = text,
            style =
                TextStyle(
                        fontFamily = basicFontFamily,
                        fontSize = fontSize,
                        localeList = localeList
                    )
                    .merge(style),
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = UncachedFontFamilyResolver(context),
            overflow = TextOverflow.Clip
        )
    }

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(vararg paragraphs: String) =
        createAnnotatedString(paragraphs.toList())

    /**
     * Helper function which creates an AnnotatedString where each input string becomes a paragraph.
     */
    private fun createAnnotatedString(paragraphs: List<String>): AnnotatedString {
        return buildAnnotatedString {
            for (paragraph in paragraphs) {
                pushStyle(ParagraphStyle())
                append(paragraph)
                pop()
            }
        }
    }
}
