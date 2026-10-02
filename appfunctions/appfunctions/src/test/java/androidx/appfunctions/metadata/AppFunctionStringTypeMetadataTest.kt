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
import androidx.appfunctions.internal.InvalidPatternMatcherException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionStringTypeMetadataTest {

    @Test
    fun regexPattern_noPatterns_isNull() {
        assertThat(AppFunctionStringTypeMetadata(isNullable = false).regexPattern).isNull()
    }

    @Test
    fun regexPattern_multiplePatterns_joinsWithAlternation() {
        val metadata =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers =
                    listOf(
                        PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                        PatternMatcher("file:", PatternMatcher.PATTERN_PREFIX),
                    ),
            )

        assertThat(metadata.regexPattern).isEqualTo("(?:^content:)|(?:^file:)")
    }

    @Test
    fun pattern_noPatterns_isNull() {
        assertThat(AppFunctionStringTypeMetadata(isNullable = false).pattern).isNull()
    }

    @Test
    fun pattern_singlePrefixPattern_returnsRegexPattern() {
        val metadata =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers = listOf(PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX)),
            )

        assertThat(metadata.pattern).isEqualTo("^content:")
        assertThat(metadata.pattern).isEqualTo(metadata.regexPattern)
    }

    @Test
    fun pattern_multiplePatterns_returnsRegexPattern() {
        val metadata =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers =
                    listOf(
                        PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                        PatternMatcher("file:", PatternMatcher.PATTERN_PREFIX),
                    ),
            )

        assertThat(metadata.pattern).isEqualTo("(?:^content:)|(?:^file:)")
        assertThat(metadata.pattern).isEqualTo(metadata.regexPattern)
    }

    @Test
    fun constructor_emptyPatternMatchers_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionStringTypeMetadata(isNullable = false, patternMatchers = emptyList())
        }
    }

    @Test
    fun legacyPatternConstructor_ignoresPattern() {
        val metadata =
            AppFunctionStringTypeMetadata(isNullable = false, pattern = "^content:", format = "uri")

        assertThat(metadata.patternMatchers).isNull()
        assertThat(metadata.regexPattern).isNull()
        assertThat(metadata.pattern).isNull()
        assertThat(metadata.format).isEqualTo("uri")
        assertThat(metadata)
            .isEqualTo(AppFunctionStringTypeMetadata(isNullable = false, format = "uri"))
    }

    @Test
    fun legacyPatternConstructor_passesThroughDescriptionAndEnumValues() {
        val metadata =
            AppFunctionStringTypeMetadata(
                isNullable = true,
                description = "A content URI",
                enumValues = setOf("content://a", "content://b"),
                pattern = "^content:",
            )

        assertThat(metadata.isNullable).isTrue()
        assertThat(metadata.description).isEqualTo("A content URI")
        assertThat(metadata.enumValues).containsExactly("content://a", "content://b")
        assertThat(metadata.patternMatchers).isNull()
        assertThat(metadata.format).isNull()
    }

    @Test
    fun primaryConstructor_withoutPattern_resolvesUnambiguously() {
        val namedArgs = AppFunctionStringTypeMetadata(isNullable = false)
        val positionalNullArgs = AppFunctionStringTypeMetadata(false, "", null, null)

        assertThat(namedArgs.patternMatchers).isNull()
        assertThat(positionalNullArgs.patternMatchers).isNull()
    }

    @Test
    fun toDocumentAndBack_preservesPatterns() {
        val metadata =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers =
                    listOf(
                        PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                        PatternMatcher(".png", PatternMatcher.PATTERN_SUFFIX),
                        PatternMatcher("[0-9]+", PatternMatcher.PATTERN_ADVANCED_GLOB),
                    ),
                format = "uri",
            )

        val document = metadata.toAppFunctionDataTypeMetadataDocument()

        assertThat(document.patterns)
            .containsExactly(
                AppFunctionStringPatternDocument(
                    value = "content:",
                    type = PatternMatcher.PATTERN_PREFIX.toLong(),
                ),
                AppFunctionStringPatternDocument(
                    value = ".png",
                    type = PatternMatcher.PATTERN_SUFFIX.toLong(),
                ),
                AppFunctionStringPatternDocument(
                    value = "[0-9]+",
                    type = PatternMatcher.PATTERN_ADVANCED_GLOB.toLong(),
                ),
            )
            .inOrder()
        assertThat(document.toAppFunctionDataTypeMetadata()).isEqualTo(metadata)
    }

    @Test
    fun toAppFunctionDataTypeMetadata_invalidPattern_throws() {
        val document =
            AppFunctionDataTypeMetadataDocument(
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
                patterns =
                    listOf(
                        AppFunctionStringPatternDocument(
                            value = "[",
                            type = PatternMatcher.PATTERN_ADVANCED_GLOB.toLong(),
                        ),
                        AppFunctionStringPatternDocument(
                            value = "content:",
                            type = PatternMatcher.PATTERN_PREFIX.toLong(),
                        ),
                    ),
            )

        assertThrows(InvalidPatternMatcherException::class.java) {
            document.toAppFunctionDataTypeMetadata()
        }
    }

    @Test
    fun toAppFunctionDataTypeMetadata_unknownType_keepsMatcherWithoutRegex() {
        val document =
            AppFunctionDataTypeMetadataDocument(
                type = AppFunctionDataTypeMetadata.TYPE_STRING,
                patterns =
                    listOf(
                        AppFunctionStringPatternDocument(value = "x", type = 99L),
                        AppFunctionStringPatternDocument(
                            value = "content:",
                            type = PatternMatcher.PATTERN_PREFIX.toLong(),
                        ),
                    ),
            )

        val metadata = document.toAppFunctionDataTypeMetadata() as AppFunctionStringTypeMetadata

        assertThat(checkNotNull(metadata.patternMatchers).map { it.path to it.type })
            .containsExactly("x" to 99, "content:" to PatternMatcher.PATTERN_PREFIX)
            .inOrder()
        assertThat(metadata.regexPattern).isNull()
    }

    @Test
    fun equalsAndHashCode_comparesPatternsByPathAndTypeIgnoringOrder() {
        val prefix = PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX)
        val literal = PatternMatcher("exact", PatternMatcher.PATTERN_LITERAL)
        val metadata1 =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers = listOf(prefix, literal),
            )
        val metadata2 =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers =
                    listOf(
                        PatternMatcher("exact", PatternMatcher.PATTERN_LITERAL),
                        PatternMatcher("content:", PatternMatcher.PATTERN_PREFIX),
                    ),
            )
        val metadata3 =
            AppFunctionStringTypeMetadata(
                isNullable = false,
                patternMatchers =
                    listOf(PatternMatcher("content:", PatternMatcher.PATTERN_LITERAL)),
            )

        assertThat(metadata1).isEqualTo(metadata2)
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode())
        assertThat(metadata1).isNotEqualTo(metadata3)
    }
}
