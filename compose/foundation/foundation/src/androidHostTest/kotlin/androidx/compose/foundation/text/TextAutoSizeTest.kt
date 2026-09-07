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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.modifiers.SimpleTextAutoSizeLayoutScope
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class TextAutoSizeTest {

    @Test
    fun stepBased_valid_args() {
        // we shouldn't throw here
        TextAutoSize.StepBased(1.sp, 2.sp, 3.sp)

        TextAutoSize.StepBased(0.sp, 0.1.sp, 0.0001.sp)

        TextAutoSize.StepBased(1.em, 2.em, 0.1.em)

        TextAutoSize.StepBased(2.sp, 1.em, 0.1.sp)
    }

    @Test
    fun stepBased_minFontSize_greaterThan_maxFontSize_coercesTo_maxFontSize() {
        var textAutoSize1 = TextAutoSize.StepBased(2.sp, 1.sp)
        var textAutoSize2 = TextAutoSize.StepBased(1.sp, 1.sp)
        assertThat(textAutoSize1).isEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isEqualTo(textAutoSize1)

        textAutoSize1 = TextAutoSize.StepBased(3.6.em, 2.em)
        textAutoSize2 = TextAutoSize.StepBased(2.em, 2.em)
        assertThat(textAutoSize1).isEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isEqualTo(textAutoSize1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_stepSize_tooSmall() {
        TextAutoSize.StepBased(stepSize = 0.00000134.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_minFontSize_unspecified() {
        TextAutoSize.StepBased(minFontSize = TextUnit.Unspecified, maxFontSize = 1.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_maxFontSize_unspecified() {
        TextAutoSize.StepBased(minFontSize = 2.sp, maxFontSize = TextUnit.Unspecified)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_stepSize_unspecified() {
        TextAutoSize.StepBased(stepSize = TextUnit.Unspecified)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_minFontSize_negative() {
        TextAutoSize.StepBased(minFontSize = (-1).sp, maxFontSize = 0.sp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun stepBased_maxFontSize_negative() {
        TextAutoSize.StepBased(minFontSize = 0.sp, maxFontSize = (-1).sp)
    }

    @Test
    fun stepBased_equals() {
        var textAutoSize1 =
            TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 10.sp, stepSize = 2.sp)
        var textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 1.0.sp, maxFontSize = 10.0.sp, stepSize = 2.0.sp)
        assertThat(textAutoSize1).isEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isEqualTo(textAutoSize1)

        textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 1.1.sp, maxFontSize = 10.sp, stepSize = 2.sp)
        assertThat(textAutoSize1).isNotEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isNotEqualTo(textAutoSize1)

        textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 11.1.sp, stepSize = 2.sp)
        assertThat(textAutoSize1).isNotEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isNotEqualTo(textAutoSize1)

        textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 10.sp, stepSize = 2.5.sp)
        assertThat(textAutoSize1).isNotEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isNotEqualTo(textAutoSize1)

        textAutoSize2 = TestAutoSize(7)
        assertThat(textAutoSize1).isNotEqualTo(textAutoSize2)

        textAutoSize1 =
            TextAutoSize.StepBased(minFontSize = 1.em, maxFontSize = 2.em, stepSize = 0.1.em)
        textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 1.0.em, maxFontSize = 2.0.em, stepSize = 0.1.em)
        assertThat(textAutoSize1).isEqualTo(textAutoSize2)
        assertThat(textAutoSize2).isEqualTo(textAutoSize1)
    }

    @Test
    fun stepBased_getFontSize_alwaysOverflows() {
        val textAutoSize =
            TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)
        val searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(0.sp)
        with(textAutoSize) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(12)
        }
    }

    @Test
    fun stepBased_getFontSize_neverOverflows() {
        val textAutoSize =
            TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)
        val searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(Float.POSITIVE_INFINITY.sp)
        with(textAutoSize) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(112)
        }
    }

    @Test
    fun stepBased_getFontSize_cappedAtMaxSize_beforeOverflow() {
        val textAutoSize =
            TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 112.sp, stepSize = 0.25.sp)

        val searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(60.sp)
        with(textAutoSize) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(60)
        }
    }

    @Test
    fun stepBased_getFontSize_searchRangeMidpoint_overflows() {
        val textAutoSize =
            TextAutoSize.StepBased(minFontSize = 0.sp, maxFontSize = 100.sp, stepSize = 70.sp)
        val searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(60.sp)
        // Here we're testing when (max - min) / 2 overflows and min doesn't overflow
        with(textAutoSize) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(0)
        }
    }

    @Test
    fun stepBased_getFontSize_differentStepSizes() {
        val textAutoSize1 =
            TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 100.sp, stepSize = 10.sp)

        val textAutoSize2 =
            TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 100.sp, stepSize = 20.sp)

        val searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(60.sp)

        with(textAutoSize1) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(60)
        }
        with(textAutoSize2) {
            val fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(50)
        }
    }

    @Test
    fun stepBased_getFontSize_stepSize_greaterThan_maxFontSize_minus_minFontSize() {
        // regardless of the bounds of the container, the only potential font size is minFontSize
        val textAutoSize =
            TextAutoSize.StepBased(minFontSize = 45.sp, maxFontSize = 55.sp, stepSize = 15.sp)

        with(textAutoSize) {
            var searchScope: TextAutoSizeLayoutScope = OverflowOnFontSize(0.sp)
            var fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(45)

            searchScope = OverflowOnFontSize(Float.POSITIVE_INFINITY.sp)
            fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(45)

            searchScope = OverflowOnFontSize(60.sp)
            fontSize =
                searchScope.getFontSize(
                    Constraints(maxWidth = 200, maxHeight = 200),
                    AnnotatedString("test"),
                )
            assertThat(fontSize.value).isEqualTo(45)
        }
    }

    private class TestAutoSize(private val testParam: Int) : TextAutoSize {
        override fun TextAutoSizeLayoutScope.getFontSize(
            constraints: Constraints,
            text: AnnotatedString,
        ): TextUnit {
            val textLayoutResult = performLayout(constraints, text, testParam.toSp())
            val didOverflow =
                textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight
            return if (!didOverflow) testParam.sp else 3.sp
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TestAutoSize) return false

            return testParam == other.testParam
        }

        override fun hashCode(): Int {
            return testParam
        }
    }

    @Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
    private class OverflowOnFontSize(
        private val fontSize: TextUnit,
        private val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        private val fontFamilyResolver: FontFamily.Resolver = mock(),
        private val softWrap: Boolean = true,
        private val textOverflow: TextOverflow = TextOverflow.Clip,
        private val maxLines: Int = Int.MAX_VALUE,
        private val style: TextStyle = TextStyle.Default,
        densityDelegate: Density = Density(1f, 1f),
    ) : SimpleTextAutoSizeLayoutScope(), Density by densityDelegate {

        override fun performLayout(
            constraints: Constraints,
            text: AnnotatedString,
            fontSize: TextUnit,
        ): TextLayoutResult {
            val size =
                if (fontSize < this.fontSize) {
                    IntSize(constraints.maxWidth - 1, constraints.maxHeight - 1)
                } else if (fontSize == this.fontSize) {
                    IntSize(constraints.maxWidth, constraints.maxHeight)
                } else {
                    IntSize(constraints.maxWidth + 1, constraints.maxHeight + 1)
                }
            // We mock MultiParagraph as there is no way to create a fake without measuring the text
            val mockMultiParagraph: MultiParagraph = mock {
                on { width } doReturn size.width.toFloat()
                on { height } doReturn size.height.toFloat()
            }
            return TextLayoutResult(
                layoutInput =
                    TextLayoutInput(
                        text = text,
                        style = style,
                        density = this,
                        fontFamilyResolver = fontFamilyResolver,
                        softWrap = softWrap,
                        overflow = textOverflow,
                        maxLines = maxLines,
                        placeholders = emptyList(),
                        constraints = constraints,
                        layoutDirection = layoutDirection,
                    ),
                multiParagraph = mockMultiParagraph,
                size = IntSize(constraints.maxWidth, constraints.maxHeight),
            )
        }
    }
}
