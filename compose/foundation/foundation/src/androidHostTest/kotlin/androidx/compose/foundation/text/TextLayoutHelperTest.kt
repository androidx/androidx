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

package androidx.compose.foundation.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class TextLayoutHelperTest {

    private val defaultText = AnnotatedString.Builder("Hello, World").toAnnotatedString()
    private lateinit var fontFamilyResolver: FontFamily.Resolver
    private lateinit var multiParagraph: MultiParagraph
    private lateinit var referenceResult: TextLayoutResult

    @Before
    fun setUp() {
        fontFamilyResolver = mock()

        val intrinsics = mock<MultiParagraphIntrinsics>()
        multiParagraph = mock<MultiParagraph>()
        whenever(multiParagraph.intrinsics).thenReturn(intrinsics)
        whenever(intrinsics.hasStaleResolvedFonts).thenReturn(false)
        referenceResult =
            TextLayoutResult(text = defaultText, constraints = Constraints.fixedWidth(100))
    }

    @Test
    fun testCanReuse_same() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isTrue()
    }

    @Test
    fun testCanReuse_different_text() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = AnnotatedString.Builder("Hello, Android").toAnnotatedString(),
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_style() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(fontSize = 1.5.em),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_maxLines() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_softWrap() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_overflow() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_density() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(2.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_layoutDirection() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Rtl,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_resourceLoader() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = mock(),
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_constraintsWidth() {
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = Constraints.fixedWidth(200),
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_constraintsHeight() {
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = Constraints.fixedHeight(200),
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_different_placeholders() {
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders =
                        listOf(
                            AnnotatedString.Range(
                                item =
                                    Placeholder(
                                        10.sp,
                                        20.sp,
                                        PlaceholderVerticalAlign.AboveBaseline,
                                    ),
                                start = 0,
                                end = 5,
                            )
                        ),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = Constraints.fixedWidth(200),
                )
            )
            .isFalse()
    }

    @Test
    fun testCanReuse_notLatestTypefaces_isFalse() {
        val constraints = Constraints.fixedWidth(100)
        whenever(referenceResult.multiParagraph.intrinsics.hasStaleResolvedFonts).thenReturn(true)
        assertThat(
                referenceResult.canReuse(
                    text = defaultText,
                    style = TextStyle(),
                    placeholders = emptyList(),
                    maxLines = 1,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    density = Density(1.0f),
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints,
                )
            )
            .isFalse()
    }

    @Test
    fun getLineHeight_valid() {
        val offset = defaultText.indexOf('W')
        val line = 1

        whenever(multiParagraph.getLineForOffset(offset)).thenReturn(line)
        whenever(multiParagraph.getLineHeight(line)).thenReturn(10f)
        whenever(multiParagraph.getLineEnd(line)).thenReturn(defaultText.length)
        whenever(multiParagraph.lineCount).thenReturn(5)
        whenever(multiParagraph.maxLines).thenReturn(5)

        assertThat(referenceResult.getLineHeight(offset)).isEqualTo(10f)
    }

    @Test
    fun getLineHeight_zero_length_text_return_zero() {
        referenceResult = TextLayoutResult(AnnotatedString(""))

        val offset = 0
        val line = 1

        whenever(multiParagraph.getLineForOffset(offset)).thenReturn(line)
        whenever(multiParagraph.getLineHeight(line)).thenReturn(10f)
        whenever(multiParagraph.lineCount).thenReturn(5)

        assertThat(referenceResult.getLineHeight(offset)).isZero()
    }

    @Test
    fun getLineHeight_line_index_more_then_lines_limit_should_return_zero() {
        val offset = defaultText.indexOf('W')
        val line = 1

        whenever(multiParagraph.getLineForOffset(offset)).thenReturn(line)
        whenever(multiParagraph.getLineHeight(line)).thenReturn(10f)
        whenever(multiParagraph.lineCount).thenReturn(5)
        whenever(multiParagraph.maxLines).thenReturn(1)

        assertThat(referenceResult.getLineHeight(offset)).isZero()
    }

    private fun TextLayoutResult(
        text: AnnotatedString,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        multiParagraph: MultiParagraph = this.multiParagraph,
        constraints: Constraints = Constraints(),
    ): TextLayoutResult {
        return TextLayoutResult(
            TextLayoutInput(
                text = text,
                style = TextStyle(),
                placeholders = listOf(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints,
            ),
            multiParagraph = multiParagraph,
            size = IntSize(1000, 1000),
        )
    }
}
