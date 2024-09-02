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
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphFillBoundingBoxesTest {
    private val fontFamilyMeasureFont = BASIC_MEASURE_FONT.toFontFamily()
    val fontFamilyResolver =
        createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
    private val defaultDensity = Density(density = 1f)
    private val fontSize = 10.sp
    private val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

    @Test
    fun singleParagraphLtr() {
        val text = createAnnotatedString("ab")
        val paragraph = simpleMultiParagraph(text)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        assertThat(result).isEqualToWithTolerance(ltrCharacterBoundariesForTestFont(text.text))
    }

    @Test
    fun singleParagraphRtl() {
        val text = createAnnotatedString("\u05D0\u05D1")
        val width = fontSizeInPx * 3
        val paragraph = simpleMultiParagraph(text, width = width)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        assertThat(result)
            .isEqualToWithTolerance(rtlCharacterBoundariesForTestFont(text.text, width))
    }

    @Test
    fun ltr() {
        val paragraph1 = "a"
        val paragraph2 = "b"
        val text = createAnnotatedString(paragraph1, paragraph2)
        val paragraph = simpleMultiParagraph(text)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        val paragraph1Rects = ltrCharacterBoundariesForTestFont(paragraph1)
        val paragraph2Rects = ltrCharacterBoundariesForTestFont(paragraph2)
        assertThat(result)
            .isEqualToWithTolerance(paragraph1Rects.offsetVerticalAndAppend(paragraph2Rects))
    }

    @Test
    fun rtl() {
        val paragraph1 = "\u05D0"
        val paragraph2 = "\u05D1"
        val text = createAnnotatedString(paragraph1, paragraph2)
        val width = fontSizeInPx * 3
        val paragraph = simpleMultiParagraph(text, width = width)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        val paragraph1Rects = rtlCharacterBoundariesForTestFont(paragraph1, width)
        val paragraph2Rects = rtlCharacterBoundariesForTestFont(paragraph2, width)
        assertThat(result)
            .isEqualToWithTolerance(paragraph1Rects.offsetVerticalAndAppend(paragraph2Rects))
    }

    @Test
    fun multiLineLtr() {
        val paragraph1 = "a\nb"
        val paragraph2 = "c\nd"
        val text = createAnnotatedString(paragraph1, paragraph2)
        val paragraph = simpleMultiParagraph(text)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        val paragraph1Rects = ltrCharacterBoundariesForTestFont(paragraph1)
        val paragraph2Rects = ltrCharacterBoundariesForTestFont(paragraph2)
        assertThat(result)
            .isEqualToWithTolerance(paragraph1Rects.offsetVerticalAndAppend(paragraph2Rects))
    }

    @Test
    fun multiLineRtl() {
        val paragraph1 = "\u05D0\n\u05D1"
        val paragraph2 = "\u05D2\n\u05D3"
        val text = createAnnotatedString(paragraph1, paragraph2)
        val width = fontSizeInPx * 3
        val paragraph = simpleMultiParagraph(text, width = width)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        val paragraph1Rects = rtlCharacterBoundariesForTestFont(paragraph1, width)
        val paragraph2Rects = rtlCharacterBoundariesForTestFont(paragraph2, width)
        assertThat(result)
            .isEqualToWithTolerance(paragraph1Rects.offsetVerticalAndAppend(paragraph2Rects))
    }

    @Test
    fun ltrAndRtlParagraphs() {
        val paragraph1 = "a\nb"
        var paragraph2 = "\u05D0\n\u05D1"
        val text = createAnnotatedString(paragraph1, paragraph2)
        val width = fontSizeInPx * 3
        val paragraph = simpleMultiParagraph(text, width = width)

        val result = paragraph.getBoundingBoxes(TextRange(0, text.length))

        val paragraph1Rects = ltrCharacterBoundariesForTestFont(paragraph1)
        val paragraph2Rects = rtlCharacterBoundariesForTestFont(paragraph2, width)
        assertThat(result).isEqualTo(paragraph1Rects.offsetVerticalAndAppend(paragraph2Rects))
    }

    private fun MultiParagraph.getBoundingBoxes(range: TextRange): Array<Rect> {
        val arraySize = range.length * 4
        val array = FloatArray(arraySize)
        fillBoundingBoxes(range, array, 0)
        return array.asRectArray()
    }

    /**
     * Updates the vertical positions of the [other] rectangle array based on the bottom of this
     * rectangle array; then appends to the current one.
     */
    private fun Array<Rect>.offsetVerticalAndAppend(other: Array<Rect>): Array<Rect> {
        return this + other.offsetVerticalBy(this.last().bottom)
    }

    /** Offsets top and bottom positions of rectangles in this array with [value]. */
    private fun Array<Rect>.offsetVerticalBy(value: Float): Array<Rect> {
        return this.map { Rect(it.left, it.top + value, it.right, it.bottom + value) }
            .toTypedArray()
    }

    private fun ltrCharacterBoundariesForTestFont(text: String): Array<Rect> =
        getLtrCharacterBoundariesForTestFont(text, fontSizeInPx)

    private fun rtlCharacterBoundariesForTestFont(text: String, width: Float): Array<Rect> =
        getRtlCharacterBoundariesForTestFont(text, width, fontSizeInPx)

    private fun createAnnotatedString(vararg paragraphs: String) =
        createAnnotatedString(paragraphs.toList())

    private fun createAnnotatedString(paragraphs: List<String>): AnnotatedString {
        return buildAnnotatedString {
            for (paragraph in paragraphs) {
                pushStyle(ParagraphStyle())
                append(paragraph)
                pop()
            }
        }
    }

    private fun simpleMultiParagraph(
        text: AnnotatedString,
        width: Float = Float.MAX_VALUE
    ): MultiParagraph {
        return MultiParagraph(
            annotatedString = text,
            style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Clip
        )
    }
}
