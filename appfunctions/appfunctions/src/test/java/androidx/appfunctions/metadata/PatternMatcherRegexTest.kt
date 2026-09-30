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

package androidx.appfunctions.metadata

import android.os.PatternMatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class PatternMatcherRegexTest {

    @Test
    fun toRegexPatternOrNull_empty_returnsNull() {
        assertThat(emptyList<PatternMatcher>().toRegexPatternOrNull()).isNull()
    }

    @Test
    fun toRegexPatternOrNull_eachType_convertsToRegex() {
        val expected =
            mapOf(
                PatternMatcher("a.b", PatternMatcher.PATTERN_LITERAL) to "^a\\.b$",
                PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX) to "^content:",
                PatternMatcher(".png", PatternMatcher.PATTERN_SUFFIX) to "\\.png$",
                PatternMatcher("a.*b\\.", PatternMatcher.PATTERN_SIMPLE_GLOB) to "^a.*b\\.$",
                PatternMatcher("[a-z]+(x)?", PatternMatcher.PATTERN_ADVANCED_GLOB) to
                    "^[a-z]+\\(x\\)\\?$",
            )

        expected.forEach { (matcher, regex) ->
            assertThat(listOf(matcher).toRegexPatternOrNull()).isEqualTo(regex)
        }
    }

    @Test
    fun toRegexPatternOrNull_multipleMatchers_joinsWithAlternation() {
        val matchers =
            listOf(
                PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                PatternMatcher(".png", PatternMatcher.PATTERN_SUFFIX),
            )

        assertThat(matchers.toRegexPatternOrNull()).isEqualTo("(?:^content:)|(?:\\.png$)")
    }

    @Test
    fun toRegexPatternOrNull_unknownType_returnsNull() {
        val matchers =
            listOf(
                PatternMatcher("x", 99),
                PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
            )

        assertThat(matchers.toRegexPatternOrNull()).isNull()
        assertThat(listOf(PatternMatcher("x", 99)).toRegexPatternOrNull()).isNull()
    }

    @Test
    fun toRegexPatternOrNull_escapesRegexSpecialCharacters() {
        assertThat(
                listOf(PatternMatcher("a^$.|?*+()[]{}/\\b", PatternMatcher.PATTERN_LITERAL))
                    .toRegexPatternOrNull()
            )
            .isEqualTo("^a\\^\\$\\.\\|\\?\\*\\+\\(\\)\\[\\]\\{\\}\\/\\\\b$")
    }

    @Test
    fun toRegexPatternOrNull_matchesSameValuesAsPatternMatcher() {
        val matchers =
            listOf(
                PatternMatcher("exa.ct", PatternMatcher.PATTERN_LITERAL),
                PatternMatcher("content://", PatternMatcher.PATTERN_PREFIX),
                PatternMatcher(".png", PatternMatcher.PATTERN_SUFFIX),
                PatternMatcher("ab*c.d", PatternMatcher.PATTERN_SIMPLE_GLOB),
                PatternMatcher("[0-9]+-[^a-c]{2,3}", PatternMatcher.PATTERN_ADVANCED_GLOB),
            )
        val inputs =
            listOf(
                "exa.ct",
                "exaXct",
                "content://media/1",
                "xcontent://",
                "image.png",
                "imagexpng",
                "acXd",
                "abbbc.d",
                "abXd",
                "123-xyz",
                "123-ab",
                "12-x",
                "",
            )

        matchers.forEach { matcher ->
            val regex = Regex(checkNotNull(listOf(matcher).toRegexPatternOrNull()))
            inputs.forEach { input ->
                assertThat(regex.containsMatchIn(input)).isEqualTo(matcher.match(input))
            }
        }
        val unionRegex = Regex(checkNotNull(matchers.toRegexPatternOrNull()))
        inputs.forEach { input ->
            assertThat(unionRegex.containsMatchIn(input))
                .isEqualTo(matchers.any { it.match(input) })
        }
    }
}
