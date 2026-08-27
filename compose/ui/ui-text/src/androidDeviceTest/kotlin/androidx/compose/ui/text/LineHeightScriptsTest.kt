/*
 * Copyright 2026 The Android Open Source Project
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

import android.graphics.Rect
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import java.text.BreakIterator
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class LineHeightScriptsTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)

    // We use FontFamily.Default to allow system fallback for different scripts
    private val fontFamily = FontFamily.Default

    private fun createNaturalParagraph(
        text: String,
        largeWord: String,
        width: Int = Constraints.Infinity,
    ): AndroidParagraph {
        return createStyledParagraph(
            text = text,
            largeWord = largeWord,
            width = width,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            mode = LineHeightStyle.Mode.PerLine,
        )
    }

    private fun createStyledParagraph(
        text: String,
        largeWord: String,
        width: Int = Constraints.Infinity,
        lineHeight: androidx.compose.ui.unit.TextUnit = 1.sp,
        mode: LineHeightStyle.Mode = LineHeightStyle.Mode.PerLine,
    ): AndroidParagraph {
        val start = text.indexOf(largeWord)
        require(start != -1) { "Could not find '$largeWord' in text" }
        val intrinsics =
            AndroidParagraphIntrinsics(
                text = text,
                style =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontSize = 10.sp,
                        lineHeight = lineHeight,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = Alignment.Proportional,
                                trim = Trim.None,
                                mode = mode,
                            ),
                    ),
                annotations =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(fontSize = 30.sp),
                            start,
                            start + largeWord.length,
                        )
                    ),
                placeholders = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context),
                softWrap = true,
            )
        return AndroidParagraph(
            paragraphIntrinsics = intrinsics,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = width),
        )
    }

    @Test
    fun script_latin() {
        val text = "Latin Large\nLatin Small\nLatin Small"
        val largeWord = "Latin Large"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_arabic() {
        val text = "جميل كبير\nجميل صغير\nجميل صغير"
        val largeWord = "جميل كبير"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_thai() {
        val text = "ที่นี่ใหญ่\nที่นี่เล็ก\nที่นี่เล็ก"
        val largeWord = "ที่นี่ใหญ่"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun script_myanmar() {
        val text = "မြန်မာကြီး\nမြန်မာငယ်\nမြန်မာငယ်"
        val largeWord = "မြန်မာကြီး"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_devanagari() {
        val text = "हिन्दी बड़ा\nहिन्दी छोटा\nहिन्दी छोटा"
        val largeWord = "हिन्दी बड़ा"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_bengali() {
        val text = "বাংলা বড়\nবাংলা ছোট\nবাংলা ছোট"
        val largeWord = "বাংলা বড়"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_chinese() {
        val text = "中文大\n中文小\n中文小"
        val largeWord = "中文大"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_japanese() {
        val text = "日本語大\n日本語小\n日本語小"
        val largeWord = "日本語大"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_korean() {
        val text = "한국어대\n한국어소\n한국어소"
        val largeWord = "한국어대"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_hebrew() {
        val text = "עברית גדול\nעברית קטן\nעברית קטן"
        val largeWord = "עברית גדול"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_greek() {
        val text = "Ελληνικά μεγάλο\nΕλληνικά μικρό\nΕλληνικά μικρό"
        val largeWord = "Ελληνικά μεγάλο"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_cyrillic() {
        val text = "Русский большой\nРусский маленький\nРусский маленький"
        val largeWord = "Русский большой"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun script_tibetan_extremeOverflow() {
        // SMT Finding: Tibetan head mark & stacked clusters (Ratio: 1.368x, +700 units)
        val text = "Ȁༀའ ཆེན་པོ\nȀༀ ཆུང་ངུ\nȀༀ ཆུང་ངུ"
        val largeWord = "Ȁༀའ ཆེན་པོ"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun script_mixed_tallTibetanLineFollowedByCompactLatinLines_noSpans() {
        val text = "Ȁༀའ ཆེན་པོ\nCompact Latin Line 1\nCompact Latin Line 2"
        val intrinsics =
            AndroidParagraphIntrinsics(
                text = text,
                style =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = Alignment.Proportional,
                                trim = Trim.None,
                                mode = LineHeightStyle.Mode.PerLine,
                            ),
                    ),
                annotations = emptyList(),
                placeholders = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context),
                softWrap = true,
            )
        val paragraph =
            AndroidParagraph(
                paragraphIntrinsics = intrinsics,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
                constraints = Constraints(),
            )

        val line0Height = paragraph.getLineHeight(0)
        val line1Height = paragraph.getLineHeight(1)
        val line2Height = paragraph.getLineHeight(2)

        // Lines 1 and 2 (compact Latin) remain at target 20px and do NOT inherit/grow from Tibetan
        // height
        assertThat(line1Height).isEqualTo(20f)
        assertThat(line2Height).isEqualTo(20f)
        assertThat(line0Height).isGreaterThan(line1Height)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun script_mixed_latinFirst_twoTibetanMiddle_twoLatinEnd_noSpans() {
        val text =
            "Latin Start Line 0\nȀༀའ ཆེན་པོ Middle 1\nȀༀའ ཆེན་པོ Middle 2\nLatin End Line 3\nLatin End Line 4"
        val intrinsics =
            AndroidParagraphIntrinsics(
                text = text,
                style =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = Alignment.Proportional,
                                trim = Trim.None,
                                mode = LineHeightStyle.Mode.PerLine,
                            ),
                    ),
                annotations = emptyList(),
                placeholders = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context),
                softWrap = true,
            )
        val paragraph =
            AndroidParagraph(
                paragraphIntrinsics = intrinsics,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
                constraints = Constraints(),
            )

        assertThat(paragraph.lineCount).isEqualTo(5)
        val h0 = paragraph.getLineHeight(0)
        val h1 = paragraph.getLineHeight(1)
        val h2 = paragraph.getLineHeight(2)
        val h3 = paragraph.getLineHeight(3)
        val h4 = paragraph.getLineHeight(4)

        // Line 0 (Latin Start) = 20px
        assertThat(h0).isEqualTo(20f)
        // Lines 1 & 2 (Tibetan Middle) expand naturally above 20px
        assertThat(h1).isGreaterThan(20f)
        assertThat(h2).isGreaterThan(20f)
        // Lines 3 & 4 (Latin End) remain at clean 20px without growing or inheriting metrics
        assertThat(h3).isEqualTo(20f)
        assertThat(h4).isEqualTo(20f)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun script_mixed_latinFirst_twoTibetanMiddle_twoLatinEnd_trimBoth_noSpans() {
        val text =
            "Latin Start Line 0\nȀༀའ ཆེན་པོ Middle 1\nȀༀའ ཆེན་པོ Middle 2\nLatin End Line 3\nLatin End Line 4"
        val intrinsics =
            AndroidParagraphIntrinsics(
                text = text,
                style =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = Alignment.Proportional,
                                trim = Trim.Both,
                                mode = LineHeightStyle.Mode.PerLine,
                            ),
                    ),
                annotations = emptyList(),
                placeholders = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context),
                softWrap = true,
            )
        val paragraph =
            AndroidParagraph(
                paragraphIntrinsics = intrinsics,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
                constraints = Constraints(),
            )

        assertThat(paragraph.lineCount).isEqualTo(5)
        val h0 = paragraph.getLineHeight(0)
        val h1 = paragraph.getLineHeight(1)
        val h2 = paragraph.getLineHeight(2)
        val h3 = paragraph.getLineHeight(3)
        val h4 = paragraph.getLineHeight(4)

        // Line 0 (Latin Start, first line) has top padding trimmed: height < 20px
        assertThat(h0).isLessThan(20f)
        // Lines 1 & 2 (Tibetan Middle) expand naturally above 20px
        assertThat(h1).isGreaterThan(20f)
        assertThat(h2).isGreaterThan(20f)
        // Line 3 (Latin Middle) is untrimmed: full 20px
        assertThat(h3).isEqualTo(20f)
        // Line 4 (Latin End, last line) has bottom padding trimmed: height < 20px
        assertThat(h4).isLessThan(20f)
    }

    @Test
    fun script_khmer_zwj_stack() {
        // SMT Finding: Khmer stacked sub-consonants with ZWJ (Ratio: 1.231x, +450 units)
        val text = "ធំ ភាសាខ្មែរ\nតូច ភាសាខ្មែរ\nតូច ភាសាខ្មែរ"
        val largeWord = "ធំ ភាសាខ្មែរ"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_telugu_fallback_stack() {
        // SMT Finding: Telugu complex stacked glyphs (Ratio: 1.263x, +500 units)
        val text = "పెద్ద తెలుగు\nచిన్న తెలుగు\nచిన్న తెలుగు"
        val largeWord = "పెద్ద తెలుగు"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_emoji_mixed_extreme() {
        // SMT Finding: Multi-codepoint Emoji + Latin mixed envelope (Ratio: 1.132x, +250 units)
        val text = "🏗️ Large Ȁ\n🏗️ Small Ȁ\n🏗️ Small Ȁ"
        val largeWord = "🏗️ Large Ȁ"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun boundary_tallOnLastLine() {
        val text = "Small\nSmall\nLarge"
        val largeWord = "Large"
        val naturalParagraph = createNaturalParagraph(text, largeWord)
        val styledParagraph = createStyledParagraph(text, largeWord)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun boundary_softWrap_tallInMiddle() {
        val text = "SmallFirst\nLargeOne LargeTwo\nSmallLast"
        val largeWord = "LargeOne LargeTwo"
        val width = 150
        val naturalParagraph = createNaturalParagraph(text, largeWord, width)
        val styledParagraph = createStyledParagraph(text, largeWord, width)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun boundary_softWrap_insideTall() {
        val text = "Small LargeOne LargeTwo"
        val largeWord = "LargeOne LargeTwo"
        val width = 150
        val naturalParagraph = createNaturalParagraph(text, largeWord, width)
        val styledParagraph = createStyledParagraph(text, largeWord, width)

        assertParagraph(styledParagraph)
            .assertPerLineMetricsAndDrawRequirements(naturalParagraph, largeWord)
    }

    @Test
    fun script_middleLarge() {
        val text = "Medium\nLarge\nSmall"
        val naturalParagraph =
            createNaturalMixedSizeParagraph(
                text = text,
                mediumWord = "Medium",
                largeWord = "Large",
                smallWord = "Small",
            )
        val paragraph =
            createMixedSizeParagraph(
                text = text,
                mediumWord = "Medium",
                largeWord = "Large",
                smallWord = "Small",
                lineHeight = 24.sp,
            )

        assertThat(paragraph.lineCount).isEqualTo(3)

        // Verify natural heights first: Line 3 (Small, L2) natural < Line 1 (Medium, L0) natural
        val l0Natural = naturalParagraph.getLineHeight(0)
        val l2Natural = naturalParagraph.getLineHeight(2)
        assertThat(l2Natural).isLessThan(l0Natural)

        // L0 (Medium) natural (~22) < Specified 24 -> expands to 24.
        assertThat(paragraph.getLineHeight(0)).isWithin(1f).of(24f)
        // L1 (Large) natural (~36) > Specified 24 -> keeps natural.
        assertThat(paragraph.getLineHeight(1)).isWithin(1f).of(naturalParagraph.getLineHeight(1))
        // L2 (Small) natural (~12) < Specified 24 -> expands to 24.
        assertThat(paragraph.getLineHeight(2)).isWithin(1f).of(24f)

        // PerLine has no overlap and no clipping
        assertParagraph(paragraph).hasNoOverlap()
        assertParagraph(paragraph).hasNoClipping()
    }

    @Test
    fun script_middleLarge_fixed() {
        val text = "Medium\nLarge\nSmall"
        val paragraph =
            createMixedSizeParagraph(
                text = text,
                mediumWord = "Medium",
                largeWord = "Large",
                smallWord = "Small",
                lineHeight = 24.sp,
                mode = LineHeightStyle.Mode.Fixed,
            )

        assertThat(paragraph.lineCount).isEqualTo(3)
        assertThat(paragraph.getLineHeight(0)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(1)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(2)).isWithin(1f).of(24f)

        // Fixed forces L1 to 24f, causing clipping
        assertParagraph(paragraph).hasClippingOnLine(1)
    }

    @Test
    fun script_middleLarge_minimum() {
        val text = "Medium\nLarge\nSmall"
        val paragraph =
            createMixedSizeParagraph(
                text = text,
                mediumWord = "Medium",
                largeWord = "Large",
                smallWord = "Small",
                lineHeight = 24.sp,
                mode = LineHeightStyle.Mode.Minimum,
            )

        assertThat(paragraph.lineCount).isEqualTo(3)
        assertThat(paragraph.getLineHeight(0)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(1)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(2)).isWithin(1f).of(24f)

        // Minimum forces L1 to 24f, causing clipping
        assertParagraph(paragraph).hasClippingOnLine(1)
    }

    @Test
    fun script_middleLarge_tight() {
        val text = "Medium\nLarge\nSmall"
        val paragraph =
            createMixedSizeParagraph(
                text = text,
                mediumWord = "Medium",
                largeWord = "Large",
                smallWord = "Small",
                lineHeight = 24.sp,
                mode = LineHeightStyle.Mode.Tight,
            )

        assertThat(paragraph.lineCount).isEqualTo(3)
        assertThat(paragraph.getLineHeight(0)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(1)).isWithin(1f).of(24f)
        assertThat(paragraph.getLineHeight(2)).isWithin(1f).of(24f)

        // Tight forces L1 to 24f, causing clipping
        assertParagraph(paragraph).hasClippingOnLine(1)
    }

    private fun createMixedSizeParagraph(
        text: String,
        mediumWord: String,
        largeWord: String,
        smallWord: String,
        width: Int = Constraints.Infinity,
        lineHeight: androidx.compose.ui.unit.TextUnit = 1.sp,
        mode: LineHeightStyle.Mode = LineHeightStyle.Mode.PerLine,
    ): AndroidParagraph {
        val mediumStart = text.indexOf(mediumWord)
        val largeStart = text.indexOf(largeWord)
        val smallStart = text.indexOf(smallWord)

        require(mediumStart != -1) { "Could not find '$mediumWord' in text" }
        require(largeStart != -1) { "Could not find '$largeWord' in text" }
        require(smallStart != -1) { "Could not find '$smallWord' in text" }

        val intrinsics =
            AndroidParagraphIntrinsics(
                text = text,
                style =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontSize = 10.sp,
                        lineHeight = lineHeight,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = Alignment.Proportional,
                                trim = Trim.None,
                                mode = mode,
                            ),
                    ),
                annotations =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(fontSize = 18.sp),
                            mediumStart,
                            mediumStart + mediumWord.length,
                        ),
                        AnnotatedString.Range(
                            SpanStyle(fontSize = 30.sp),
                            largeStart,
                            largeStart + largeWord.length,
                        ),
                        AnnotatedString.Range(
                            SpanStyle(fontSize = 10.sp),
                            smallStart,
                            smallStart + smallWord.length,
                        ),
                    ),
                placeholders = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = UncachedFontFamilyResolver(context),
                softWrap = true,
            )
        return AndroidParagraph(
            paragraphIntrinsics = intrinsics,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = width),
        )
    }

    private fun createNaturalMixedSizeParagraph(
        text: String,
        mediumWord: String,
        largeWord: String,
        smallWord: String,
        width: Int = Constraints.Infinity,
    ): AndroidParagraph {
        return createMixedSizeParagraph(
            text = text,
            mediumWord = mediumWord,
            largeWord = largeWord,
            smallWord = smallWord,
            width = width,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            mode = LineHeightStyle.Mode.PerLine,
        )
    }
}

internal fun assertParagraph(actual: AndroidParagraph?): AndroidParagraphSubject =
    assertAbout(AndroidParagraphSubject.SUBJECT_FACTORY).that(actual)!!

internal class AndroidParagraphSubject
private constructor(metadata: FailureMetadata, private val actual: AndroidParagraph?) :
    Subject(metadata, actual) {

    fun assertPerLineMetricsAndDrawRequirements(expected: AndroidParagraph, largeWord: String) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val text = actual.charSequence.toString()
        val start = text.indexOf(largeWord)
        require(start != -1) { "Could not find '$largeWord' in text" }
        val end = start + largeWord.length

        matchesMetrics(expected)
        hasTallerLinesForSpan(start, end)
        hasNaturalHeightForSmallLines(start, end, expected)
        hasNoClipping()
        hasNoOverlap()
    }

    fun matchesMetrics(expected: AndroidParagraph, tolerance: Float = 1f) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        check("lineCount").that(actual.lineCount).isEqualTo(expected.lineCount)
        for (i in 0 until actual.lineCount) {
            check("line $i height")
                .that(actual.getLineHeight(i))
                .isWithin(tolerance)
                .of(expected.getLineHeight(i))
            check("line $i ascent")
                .that(actual.getLineAscent(i))
                .isWithin(tolerance)
                .of(expected.getLineAscent(i))
            check("line $i descent")
                .that(actual.getLineDescent(i))
                .isWithin(tolerance)
                .of(expected.getLineDescent(i))
        }
    }

    fun hasTallerLinesForSpan(spanStart: Int, spanEnd: Int) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val (largeLines, smallLines) =
            (0 until actual.lineCount).partition { i ->
                val start = actual.getLineStart(i)
                val end = actual.getLineEnd(i)
                start < spanEnd && end > spanStart
            }

        check("largeLines").that(largeLines).isNotEmpty()
        check("smallLines").that(smallLines).isNotEmpty()

        for (largeLine in largeLines) {
            val largeHeight = actual.getLineHeight(largeLine)
            for (smallLine in smallLines) {
                val smallHeight = actual.getLineHeight(smallLine)
                check("lineHeight($largeLine) > lineHeight($smallLine)")
                    .that(largeHeight)
                    .isGreaterThan(smallHeight)
            }
        }
    }

    fun hasNaturalHeightForSmallLines(
        spanStart: Int,
        spanEnd: Int,
        natural: AndroidParagraph,
        tolerance: Float = 1f,
    ) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val (_, smallLines) =
            (0 until actual.lineCount).partition { i ->
                val start = actual.getLineStart(i)
                val end = actual.getLineEnd(i)
                start < spanEnd && end > spanStart
            }

        check("smallLines").that(smallLines).isNotEmpty()

        for (line in smallLines) {
            val actualHeight = actual.getLineHeight(line)
            val expectedHeight = natural.getLineHeight(line)
            check("small line $line height (actual=$actualHeight, expected=$expectedHeight)")
                .that(actualHeight)
                .isWithin(tolerance)
                .of(expectedHeight)
        }
    }

    fun hasNoClipping(tolerance: Float = 2f) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val inkBounds = computeInkBounds()
        for (i in inkBounds.indices) {
            val bounds = inkBounds[i]
            check("line $i top clipping (inkTop=${bounds.inkTop}, lineTop=${bounds.lineTop})")
                .that(bounds.inkTop)
                .isAtLeast(bounds.lineTop - tolerance)
            check(
                    "line $i bottom clipping (inkBottom=${bounds.inkBottom}, lineBottom=${bounds.lineBottom})"
                )
                .that(bounds.inkBottom)
                .isAtMost(bounds.lineBottom + tolerance)
        }
    }

    fun hasClippingOnLine(line: Int, tolerance: Float = 2f) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val inkBounds = computeInkBounds()
        val bounds = inkBounds[line]
        val topClipped = bounds.inkTop < (bounds.lineTop - tolerance)
        val bottomClipped = bounds.inkBottom > (bounds.lineBottom + tolerance)
        check(
                "line $line is clipped (inkTop=${bounds.inkTop}, lineTop=${bounds.lineTop}, inkBottom=${bounds.inkBottom}, lineBottom=${bounds.lineBottom})"
            )
            .that(topClipped || bottomClipped)
            .isTrue()
    }

    fun hasNoOverlap(tolerance: Float = 2f) {
        if (actual == null) {
            failWithActual(Fact.simpleFact("expected not to be null"))
            return
        }
        val inkBounds = computeInkBounds()
        for (i in 0 until inkBounds.size - 1) {
            val current = inkBounds[i]
            val next = inkBounds[i + 1]

            check(
                    "overlap between line $i and ${i + 1} (L${i}Bottom=${current.inkBottom}, L${i+1}Top=${next.inkTop})"
                )
                .that(current.inkBottom)
                .isAtMost(next.inkTop + tolerance)
        }
    }

    private class LineInkBounds(
        val inkTop: Float,
        val inkBottom: Float,
        val lineTop: Float,
        val lineBottom: Float,
    )

    private fun computeInkBounds(): List<LineInkBounds> {
        val paragraph = actual ?: return emptyList()
        val text = paragraph.paragraphIntrinsics.text
        val paint = TextPaint(paragraph.paragraphIntrinsics.textPaint)

        val result = mutableListOf<LineInkBounds>()
        val boundary = BreakIterator.getCharacterInstance()

        val spanned = paragraph.charSequence as? Spanned

        for (i in 0 until paragraph.lineCount) {
            val start = paragraph.getLineStart(i)
            val end = paragraph.getLineEnd(i)
            val lineText = text.substring(start, end)

            if (lineText.trim().isEmpty()) {
                val top = paragraph.getLineTop(i)
                val bottom = paragraph.getLineBottom(i)
                result.add(LineInkBounds(top, bottom, top, bottom))
                continue
            }

            var minTop = 0f
            var maxBottom = 0f
            var hasText = false

            boundary.setText(lineText)
            var current = boundary.first()
            while (current != BreakIterator.DONE) {
                val next = boundary.next()
                if (next == BreakIterator.DONE) break

                val cluster = lineText.substring(current, next)
                if (cluster.trim().isNotEmpty()) {
                    hasText = true
                    val globalOffset = start + current

                    paint.set(paragraph.paragraphIntrinsics.textPaint)

                    if (spanned != null) {
                        val spans =
                            spanned.getSpans(
                                globalOffset,
                                globalOffset + 1,
                                MetricAffectingSpan::class.java,
                            )
                        for (span in spans) {
                            span.updateMeasureState(paint)
                        }
                    }

                    val path = android.graphics.Path()
                    val rectF = android.graphics.RectF()
                    paint.getTextPath(cluster, 0, cluster.length, 0f, 0f, path)
                    path.computeBounds(rectF, true)

                    if (!rectF.isEmpty) {
                        minTop = minOf(minTop, rectF.top)
                        maxBottom = maxOf(maxBottom, rectF.bottom)
                    } else {
                        val charBounds = android.graphics.Rect()
                        paint.getTextBounds(cluster, 0, cluster.length, charBounds)
                        minTop = minOf(minTop, charBounds.top.toFloat())
                        maxBottom = maxOf(maxBottom, charBounds.bottom.toFloat())
                    }
                }
                current = next
            }

            val baseline = paragraph.getLineBaseline(i)
            val inkTop = if (hasText) baseline + minTop else paragraph.getLineTop(i)
            val inkBottom = if (hasText) baseline + maxBottom else paragraph.getLineBottom(i)

            result.add(
                LineInkBounds(
                    inkTop = inkTop,
                    inkBottom = inkBottom,
                    lineTop = paragraph.getLineTop(i),
                    lineBottom = paragraph.getLineBottom(i),
                )
            )
        }
        return result
    }

    companion object {
        internal val SUBJECT_FACTORY: Factory<AndroidParagraphSubject?, AndroidParagraph?> =
            Factory { metadata, actual ->
                AndroidParagraphSubject(metadata, actual)
            }
    }
}
