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

import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphIntegrationBoundingBoxTest {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = UncachedFontFamilyResolver(context)
    private val defaultDensity = Density(density = 1f)
    private val fontSize = 10.sp
    private val fontSizeInPx = with(defaultDensity) { fontSize.roundToPx() }

    @Test
    fun ltr_noBreak_noMaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.No, widthInFontSize = 3)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun ltr_noBreak_noMaxLines_smallWidth_smallHeight() {
        testParagraph(
                textDirection = TextDirection.Ltr,
                lineBreakFrom = LineBreakFrom.No,
                widthInFontSize = 3,
                heightIntFontSize = 1,
            )
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_noBreak_noMaxLines_largeWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.No, widthInFontSize = 7)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 5,
                right = fontSizeInPx * 6,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_noBreak_1MaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.No, widthInFontSize = 3, maxLines = 1)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_noBreak_1MaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Ltr,
                LineBreakFrom.No,
                widthInFontSize = 3,
                heightIntFontSize = 1,
                maxLines = 1,
            )
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_firstBreak_noMaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.First, widthInFontSize = 3)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun ltr_firstBreak_noMaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Ltr,
                LineBreakFrom.First,
                widthInFontSize = 3,
                heightIntFontSize = 1,
            )
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_firstBreak_noMaxLines_largeWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.First, widthInFontSize = 7)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun ltr_firstBreak_1MaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.First, widthInFontSize = 3, maxLines = 1)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_firstBreak_1MaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Ltr,
                LineBreakFrom.First,
                widthInFontSize = 3,
                heightIntFontSize = 1,
                maxLines = 1,
            )
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_secondBreak_noMaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Ltr,
                LineBreakFrom.Second,
                widthInFontSize = 3,
                heightIntFontSize = 1,
            )
            .assertBoxLRTB(
                offset = 9,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_secondBreak_noMaxLines_largeWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.Second, widthInFontSize = 7)
            .assertBoxLRTB(
                offset = 9,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun ltr_secondBreak_1MaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Ltr, LineBreakFrom.Second, widthInFontSize = 3, maxLines = 1)
            .assertBoxLRTB(
                offset = 9,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun ltr_secondBreak_1MaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Ltr,
                LineBreakFrom.Second,
                widthInFontSize = 3,
                heightIntFontSize = 1,
                maxLines = 1,
            )
            .assertBoxLRTB(
                offset = 9,
                left = fontSizeInPx * 3,
                right = fontSizeInPx * 3,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun rtl_noBreak_noMaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.No, widthInFontSize = 3)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun rtl_noBreak_noMaxLines_smallWidth_smallHeight() {
        testParagraph(
                textDirection = TextDirection.Rtl,
                lineBreakFrom = LineBreakFrom.No,
                widthInFontSize = 3,
                heightIntFontSize = 1,
            )
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    @Test
    fun rtl_noBreak_noMaxLines_largeWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.No, widthInFontSize = 7)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx,
                right = fontSizeInPx * 2,
                top = 0,
                bottom = fontSizeInPx,
            )
    }

    @Test
    fun rtl_noBreak_1MaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.No, widthInFontSize = 3, maxLines = 1)
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    @Test
    fun rtl_noBreak_1MaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Rtl,
                LineBreakFrom.No,
                widthInFontSize = 3,
                heightIntFontSize = 1,
                maxLines = 1,
            )
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    @Test
    fun rtl_firstBreak_noMaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.First, widthInFontSize = 3)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 1,
                right = fontSizeInPx * 2,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun rtl_firstBreak_noMaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Rtl,
                LineBreakFrom.First,
                widthInFontSize = 3,
                heightIntFontSize = 1,
            )
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    @Test
    fun rtl_firstBreak_noMaxLines_largeWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.First, widthInFontSize = 7)
            .assertBoxLRTB(
                offset = 5,
                left = fontSizeInPx * 5,
                right = fontSizeInPx * 6,
                top = fontSizeInPx,
                bottom = fontSizeInPx * 2,
            )
    }

    @Test
    fun rtl_firstBreak_1MaxLines_smallWidth_noHeight() {
        testParagraph(TextDirection.Rtl, LineBreakFrom.First, widthInFontSize = 3, maxLines = 1)
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    @Test
    fun rtl_firstBreak_1MaxLines_smallWidth_smallHeight() {
        testParagraph(
                TextDirection.Rtl,
                LineBreakFrom.First,
                widthInFontSize = 3,
                heightIntFontSize = 1,
                maxLines = 1,
            )
            .assertBoxLRTB(offset = 5, left = 0, right = 0, top = 0, bottom = fontSizeInPx)
    }

    private fun testParagraph(
        textDirection: TextDirection,
        lineBreakFrom: LineBreakFrom,
        widthInFontSize: Int,
        heightIntFontSize: Int? = null,
        maxLines: Int = Int.MAX_VALUE,
    ) =
        Paragraph(
            text = TEST_CONTENT_MAP[textDirection]!![lineBreakFrom]!!,
            style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            constraints =
                Constraints(
                    maxWidth = (widthInFontSize * fontSizeInPx),
                    maxHeight = (heightIntFontSize?.times(fontSizeInPx)) ?: Constraints.Infinity,
                ),
            density = defaultDensity,
            fontFamilyResolver = fontFamilyResolver,
        )

    private fun Paragraph.assertBoxLRTB(offset: Int, left: Int, right: Int, top: Int, bottom: Int) {
        val box = getBoundingBox(offset)
        assertThat(box.left).isEqualTo(left)
        assertThat(box.right).isEqualTo(right)
        assertThat(box.top).isEqualTo(top)
        assertThat(box.bottom).isEqualTo(bottom)
    }
}

private enum class LineBreakFrom {
    No,
    First,
    Second,
}

private val TEST_CONTENT_MAP =
    mapOf(
        TextDirection.Ltr to
            mapOf(
                LineBreakFrom.No to "abc def abc",
                LineBreakFrom.First to "abc\ndef abc",
                LineBreakFrom.Second to "abc def\nabc",
            ),
        TextDirection.Rtl to
            mapOf(
                LineBreakFrom.No to "\u05D0\u05D2\u05D2 \u05D3\u05D4\u05D5 \u05D0\u05D2\u05D2",
                LineBreakFrom.First to "\u05D0\u05D2\u05D2\n\u05D3\u05D4\u05D5 \u05D0\u05D2\u05D2",
                LineBreakFrom.Second to "\u05D0\u05D2\u05D2 \u05D3\u05D4\u05D5\n\u05D0\u05D2\u05D2",
            ),
    )
