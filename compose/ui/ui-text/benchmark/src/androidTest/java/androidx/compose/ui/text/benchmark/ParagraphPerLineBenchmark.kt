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

package androidx.compose.ui.text.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for [LineHeightStyle.Mode.PerLine] paragraph construction.
 *
 * Note: The texts used in this benchmark are static rather than randomly generated. As a result,
 * measured times are faster due to system-level text layout caching. This is intentional since this
 * benchmark specifically isolates the overhead of [LineHeightStyleSpan] application (which is not
 * cached). Because the baseline measurement is faster on this cached path, percentage regressions
 * reported here will appear larger than in real-world uncached text layout (e.g. a 10% regression
 * here may represent ~2% in practice).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ParagraphPerLineBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private val fontSize = 16.sp
    private val latinText = "Line 1\nLine 2\nLine 3"
    private val nonLatinText = "မြန်မာစာ ၁\nမြန်မာစာ ၂\nမြန်မာစာ ၃" // Myanmar text
    private var width: Int = 1000

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    private fun paragraph(text: String, hasSpans: Boolean): Paragraph {
        val style =
            TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize * 2,
                lineHeightStyle =
                    LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Proportional,
                        trim = LineHeightStyle.Trim.None,
                        mode = LineHeightStyle.Mode.PerLine,
                    ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            )

        val annotations =
            if (hasSpans) {
                listOf(
                    AnnotatedString.Range(
                        item = SpanStyle(fontSize = fontSize * 2) as AnnotatedString.Annotation,
                        start = 7,
                        end = 13,
                    )
                )
            } else {
                emptyList()
            }

        val intrinsics =
            ParagraphIntrinsics(
                text = text,
                style = style,
                annotations = annotations,
                density = Density(density = context.resources.displayMetrics.density),
                fontFamilyResolver = createFontFamilyResolver(context),
                defaultLocaleList = LocaleList("en"),
                placeholders = emptyList(),
                softWrap = true,
            )

        return Paragraph(
            paragraphIntrinsics = intrinsics,
            constraints = Constraints(maxWidth = width),
            overflow = TextOverflow.Clip,
        )
    }

    @Test
    fun construct_perLine_latin_uniform() {
        benchmarkRule.measureRepeated { paragraph(latinText, hasSpans = false) }
    }

    @Test
    fun construct_perLine_latin_withSpans() {
        benchmarkRule.measureRepeated { paragraph(latinText, hasSpans = true) }
    }

    @Test
    fun construct_perLine_nonLatin_withSpans() {
        benchmarkRule.measureRepeated { paragraph(nonLatinText, hasSpans = true) }
    }
}
