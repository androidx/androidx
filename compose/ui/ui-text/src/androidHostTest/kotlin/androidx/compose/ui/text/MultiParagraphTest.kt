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

import android.text.Spannable
import android.text.Spanned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.createCharSequence
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.isApplicable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.emoji2.text.EmojiCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class MultiParagraphTest {
    @Test
    fun findParagraphByIndex() {
        val paragraphNumber = 5
        val paragraphLength = 10
        var start = 0
        val paragraphInfoList =
            List(paragraphNumber) {
                val end = start + paragraphLength
                ParagraphInfo(mock(), start, end).also { start = end }
            }

        for (i in 0 until paragraphNumber * paragraphLength) {
            assertThat(findParagraphByIndex(paragraphInfoList, i)).isEqualTo(i / paragraphLength)
        }
    }

    @Test
    fun findParagraphByLineIndex() {
        val paragraphNumber = 5
        val paragraphLineCount = 10
        var startLine = 0
        val paragraphInfoList =
            List(paragraphNumber) {
                val endLine = startLine + paragraphLineCount
                // StartIndex and endIndex doesn't matter in this test
                ParagraphInfo(mock(), 0, 0, startLine, endLine).also { startLine = endLine }
            }

        for (i in 0 until paragraphNumber * paragraphLineCount) {
            assertThat(findParagraphByLineIndex(paragraphInfoList, i))
                .isEqualTo(i / paragraphLineCount)
        }
    }

    @Test
    fun findParagraphByYPosition() {
        val paragraphNumber = 5
        val paragraphHeight = 10
        var top = 0.0f
        val paragraphInfoList =
            List(paragraphNumber) {
                val bottom = top + paragraphHeight
                // StartIndex and endIndex doesn't matter in this test
                ParagraphInfo(mock(), 0, 0, top = top, bottom = bottom).also { top = bottom }
            }

        for (i in 0 until paragraphNumber * paragraphHeight) {
            assertThat(findParagraphByY(paragraphInfoList, i.toFloat()))
                .isEqualTo(i / paragraphHeight)
        }
    }

    @Test
    fun paragraphInfo_toLocalLineIndex() {
        val startLineIndex = 10
        val endLineIndex = 20
        val paragraphInfo =
            ParagraphInfo(
                paragraph = mock(),
                startIndex = 0,
                endIndex = 0,
                startLineIndex = startLineIndex,
                endLineIndex = endLineIndex,
            )

        with(paragraphInfo) {
            assertThat(10.toLocalLineIndex()).isEqualTo(0)
            assertThat(15.toLocalLineIndex()).isEqualTo(5)
            assertThat(19.toLocalLineIndex()).isEqualTo(9)
        }
    }

    @Test
    fun multiParagraphIntrinsics_minAndMaxIntrinsicWidth_cacheOnFirstReadAndHandleEmptyInfoList() {
        var minCalls1 = 0
        var maxCalls1 = 0
        val p1 =
            mock<ParagraphIntrinsics> {
                on { minIntrinsicWidth }
                    .thenAnswer {
                        minCalls1++
                        25f
                    }
                on { maxIntrinsicWidth }
                    .thenAnswer {
                        maxCalls1++
                        80f
                    }
            }
        var minCalls2 = 0
        var maxCalls2 = 0
        val p2 =
            mock<ParagraphIntrinsics> {
                on { minIntrinsicWidth }
                    .thenAnswer {
                        minCalls2++
                        42f
                    }
                on { maxIntrinsicWidth }
                    .thenAnswer {
                        maxCalls2++
                        60f
                    }
            }

        val intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = AnnotatedString(""),
                placeholders = emptyList(),
                infoList =
                    listOf(
                        ParagraphIntrinsicInfo(p1, 0, 5),
                        ParagraphIntrinsicInfo(p2, 5, 10),
                    ),
            )

        // First access computes max across infoList reading each paragraph once; second access
        // returns cached value without re-reading
        assertThat(intrinsics.minIntrinsicWidth).isEqualTo(42f)
        assertThat(intrinsics.minIntrinsicWidth).isEqualTo(42f)
        assertThat(minCalls1).isEqualTo(1)
        assertThat(minCalls2).isEqualTo(1)

        assertThat(intrinsics.maxIntrinsicWidth).isEqualTo(80f)
        assertThat(intrinsics.maxIntrinsicWidth).isEqualTo(80f)
        assertThat(maxCalls1).isEqualTo(1)
        assertThat(maxCalls2).isEqualTo(1)

        // Empty infoList falls back to 0f and caches 0f
        val emptyIntrinsics =
            MultiParagraphIntrinsics(
                annotatedString = AnnotatedString(""),
                placeholders = emptyList(),
                infoList = emptyList(),
            )
        assertThat(emptyIntrinsics.minIntrinsicWidth).isEqualTo(0f)
        assertThat(emptyIntrinsics.maxIntrinsicWidth).isEqualTo(0f)
    }

    @Test
    fun multiParagraphIntrinsics_getLocalPlaceholders_handlesEmptyNoMatchStartZeroShiftedAndOverlap() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        val placeholderA = Placeholder(10.sp, 10.sp, PlaceholderVerticalAlign.AboveBaseline)
        val placeholderB = Placeholder(20.sp, 12.sp, PlaceholderVerticalAlign.Bottom)
        val rangeAt0 = AnnotatedString.Range(placeholderA, 0, 2)
        val rangeAt5 = AnnotatedString.Range(placeholderA, 5, 7)
        val rangeAt8 = AnnotatedString.Range(placeholderB, 8, 10)

        // 1. Empty list returns emptyList()
        assertThat(emptyList<AnnotatedString.Range<Placeholder>>().getLocalPlaceholders(0, 10))
            .isEmpty()

        // 2. Non-empty list with no intersecting placeholders (including adjacent boundaries)
        // returns emptyList()
        assertThat(listOf(rangeAt0, rangeAt8).getLocalPlaceholders(2, 8)).isEmpty()

        // 3. Intersecting placeholder when start == 0 reuses the exact Range instance without
        // allocating
        val atZeroResult = listOf(rangeAt0, rangeAt5).getLocalPlaceholders(0, 4)
        assertThat(atZeroResult).hasSize(1)
        assertThat(atZeroResult[0]).isSameInstanceAs(rangeAt0)

        // 4. Multiple intersecting placeholders when start > 0 shifts offsets by -start
        val shiftedResult = listOf(rangeAt0, rangeAt5, rangeAt8).getLocalPlaceholders(4, 10)
        assertThat(shiftedResult)
            .containsExactly(
                AnnotatedString.Range(placeholderA, 1, 3),
                AnnotatedString.Range(placeholderB, 4, 6),
            )
            .inOrder()

        // 5. Placeholder partially overlapping paragraph start or end throws
        // IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            listOf(AnnotatedString.Range(placeholderA, 3, 6)).getLocalPlaceholders(4, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            listOf(AnnotatedString.Range(placeholderA, 8, 12)).getLocalPlaceholders(4, 10)
        }
    }

    @Test
    fun multiParagraphIntrinsics_singleParagraphFastPath_validatesOutOfBoundsPlaceholdersAndMatchesUnspecifiedTextDirection() {
        val placeholder = Placeholder(10.sp, 10.sp, PlaceholderVerticalAlign.AboveBaseline)
        // Out-of-bounds placeholder on a single-paragraph AnnotatedString must throw
        // IllegalArgumentException before ParagraphIntrinsics is constructed
        assertThrows(IllegalArgumentException::class.java) {
            MultiParagraphIntrinsics(
                annotatedString = AnnotatedString("hello"),
                style = TextStyle(),
                placeholders = listOf(AnnotatedString.Range(placeholder, 0, 10)),
                density = Density(1f),
                fontFamilyResolver = mock<FontFamily.Resolver>(),
                softWrap = true,
                defaultLocaleList = LocaleList("en-US"),
            )
        }

        // Verify that for a single paragraph, resolveTextDirection + style.merge is an identity on
        // TextStyle even when textDirection is TextDirection.Unspecified
        for (dir in listOf(TextDirection.Unspecified, TextDirection.Ltr, TextDirection.Rtl)) {
            val style = TextStyle(textAlign = TextAlign.Center, textDirection = dir)
            val paragraphStyle = style.toParagraphStyle()
            val legacyMergedStyle =
                style.merge(resolveTextDirection(paragraphStyle, paragraphStyle))
            assertThat(legacyMergedStyle).isEqualTo(style)
        }
    }

    @Test
    fun multiParagraphIntrinsics_resolveTextDirection_preservesSpecifiedOrCopiesDefault() {
        val defaultStyle = ParagraphStyle(textDirection = TextDirection.Rtl)

        val specifiedStyle =
            ParagraphStyle(textAlign = TextAlign.Center, textDirection = TextDirection.Ltr)
        assertThat(resolveTextDirection(specifiedStyle, defaultStyle))
            .isSameInstanceAs(specifiedStyle)

        val unspecifiedStyle =
            ParagraphStyle(textAlign = TextAlign.Center, textDirection = TextDirection.Unspecified)
        val resolved = resolveTextDirection(unspecifiedStyle, defaultStyle)
        assertThat(resolved)
            .isEqualTo(
                ParagraphStyle(textAlign = TextAlign.Center, textDirection = TextDirection.Rtl)
            )
    }

    @Test
    fun createCharSequence_plainTextFastPath_handlesNullAndNoneTextIndentAndPreservesUnderlineNoopSpan() {
        val rawText = "PlainFastPath"
        val density = Density(1f)
        val localeList = LocaleList("en-US")

        // 1. Both textIndent = null and textIndent = TextIndent.None take the plain-text String
        // fast path when there are no annotations, placeholders, lineHeight, or underline
        for (style in listOf(TextStyle(), TextStyle(textIndent = TextIndent.None))) {
            val seq =
                createCharSequence(
                    text = rawText,
                    contextFontSize = 16f,
                    contextTextStyle = style,
                    annotations = emptyList(),
                    userAnnotations = emptyList(),
                    placeholders = emptyList(),
                    density = density,
                    resolveTypeface = { _, _, _, _ ->
                        throw AssertionError("Must not resolve typeface on plain-text fast path")
                    },
                    useEmojiCompat = false,
                    softWrap = true,
                    mayHaveNewLine = false,
                    defaultLocaleList = localeList,
                )
            assertThat(seq).isSameInstanceAs(rawText)
        }

        // 2. When textDecoration == TextDecoration.Underline (whether textIndent is null or
        // TextIndent.None), createCharSequence must NOT skip NoopSpan and must use
        // spannableString.length (even if EmojiCompat.process changed the CharSequence length)
        for (style in
            listOf(
                TextStyle(textDecoration = TextDecoration.Underline),
                TextStyle(
                    textIndent = TextIndent.None,
                    textDecoration = TextDecoration.Underline,
                ),
            )) {
            val processedSpannable = mock<Spannable> { on { length } doReturn 20 }
            val emojiMock = mock<EmojiCompat>()
            whenever(emojiMock.process(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(processedSpannable)
            EmojiCompat.reset(emojiMock)
            try {
                val seq =
                    createCharSequence(
                        text = rawText,
                        contextFontSize = 16f,
                        contextTextStyle = style,
                        annotations = emptyList(),
                        userAnnotations = emptyList(),
                        placeholders = emptyList(),
                        density = density,
                        resolveTypeface = { _, _, _, _ ->
                            throw AssertionError("Must not resolve typeface without font spans")
                        },
                        useEmojiCompat = true,
                        softWrap = true,
                        mayHaveNewLine = false,
                        defaultLocaleList = localeList,
                    )
                assertThat(seq).isSameInstanceAs(processedSpannable)
                verify(processedSpannable)
                    .setSpan(any(), eq(0), eq(20), eq(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE))
            } finally {
                EmojiCompat.reset(null)
            }
        }
    }

    @Test
    fun baselineShift_isApplicable_falseForNoneAndUnspecifiedAndTrueForNonZero() {
        assertThat(BaselineShift.None.isApplicable).isFalse()
        assertThat(BaselineShift.Unspecified.isApplicable).isFalse()
        assertThat(TextStyle().baselineShift?.isApplicable == true).isFalse()
        assertThat(
                TextStyle(baselineShift = BaselineShift.None).baselineShift?.isApplicable == true
            )
            .isFalse()
        assertThat(
                TextStyle(baselineShift = BaselineShift.Unspecified).baselineShift?.isApplicable ==
                    true
            )
            .isFalse()
        assertThat(BaselineShift.Superscript.isApplicable).isTrue()
        assertThat(BaselineShift.Subscript.isApplicable).isTrue()
        assertThat(BaselineShift(0.25f).isApplicable).isTrue()
    }
}
