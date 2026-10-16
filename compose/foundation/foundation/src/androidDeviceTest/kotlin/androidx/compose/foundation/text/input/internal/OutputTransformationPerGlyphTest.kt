/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.OutputTransformationPerGlyph
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.test.withEmojiCompat
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class OutputTransformationPerGlyphTest {

    @Test
    fun replacesAllCharacters_withMask() {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("*****")
    }

    @Test
    fun doesNotReplace_whenTransformReturnsNull() {
        val transformation = OutputTransformationPerGlyph { null }
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("hello")
    }

    @Test
    fun replacesOnlySpecificCharacters() {
        val transformation = OutputTransformationPerGlyph { range ->
            if (asCharSequence().substring(range.min, range.max) == "l") {
                "L"
            } else {
                null
            }
        }
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("heLLo")
    }

    @Test
    fun replacesComplexEmoji_asSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${COMPLEX_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun replacesRegularEmoji_asSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${REGULAR_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun replacesMixedContent_correctly() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { range ->
            when (asCharSequence().substring(range.min, range.max)) {
                "a" -> "A"
                COMPLEX_EMOJI -> "C"
                REGULAR_EMOJI -> "R"
                else -> null // "b" should not be replaced
            }
        }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${COMPLEX_EMOJI}b${REGULAR_EMOJI}"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("ACbR")
    }

    @Test
    fun replacesGlyph_withLongerString() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "xx" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${REGULAR_EMOJI}c"))
        with(transformation) { buffer.transformOutput() }
        // each of the 3 glyphs is replaced by "xx"
        assertThat(buffer.toString()).isEqualTo("xxxxxx")
    }

    @Test
    fun replacesGlyph_withEmptyString() {
        val transformation = OutputTransformationPerGlyph { range ->
            if (asCharSequence().substring(range.min, range.max) == "l") {
                ""
            } else {
                null
            }
        }
        val buffer = TextFieldBuffer(TextFieldCharSequence("hello"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("heo")
    }

    @Test
    fun emptyString_doesNothing() {
        var invocationCount = 0
        val transformation = OutputTransformationPerGlyph {
            invocationCount++
            "*"
        }
        val buffer = TextFieldBuffer(TextFieldCharSequence(""))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("")
        assertThat(invocationCount).isEqualTo(0)
    }

    @Test
    fun singleCharacter_replacesCorrectly() {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("*")
    }

    @Test
    fun singleComplexEmojiOnly_replacesAsSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence(COMPLEX_EMOJI))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("*")
    }

    @Test
    fun consecutiveEmojis_replacesEachAsSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer =
            TextFieldBuffer(
                TextFieldCharSequence("$COMPLEX_EMOJI$COMPLEX_EMOJI$REGULAR_EMOJI$COMPLEX_EMOJI")
            )
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("****")
    }

    @Test
    fun combiningDiacriticalMark_replacesAsSingleGlyph() {
        // 'e' followed by combining acute accent (U+0301) forms a single grapheme cluster
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("ae\u0301b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun skinToneModifiedEmoji_replacesAsSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${SKIN_TONE_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun regionalIndicatorFlagEmoji_replacesAsSingleGlyph() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${REGIONAL_FLAG_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun crlf_replacesAsSingleGlyph() {
        // CR+LF (\r\n) is a single grapheme cluster per Unicode UAX #29
        val transformation = OutputTransformationPerGlyph { "*" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a\r\nb"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
    }

    @Test
    fun replacesAllGlyphs_withEmptyString() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "" }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${COMPLEX_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("")
    }

    @Test
    fun replacesGlyphs_withComplexEmojiMask() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { COMPLEX_EMOJI }
        val buffer = TextFieldBuffer(TextFieldCharSequence("a${REGULAR_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("$COMPLEX_EMOJI$COMPLEX_EMOJI$COMPLEX_EMOJI")
    }

    @Test
    fun passesCorrectGlyphRanges_inReverseOrder() = withEmojiCompat {
        val visitedRanges = mutableListOf<TextRange>()
        val transformation = OutputTransformationPerGlyph { range ->
            visitedRanges.add(range)
            "*"
        }
        // "a" [0, 1), REGULAR_EMOJI [1, 3), "e\u0301" [3, 5), COMPLEX_EMOJI [5, 11), "b" [11, 12)
        val buffer =
            TextFieldBuffer(TextFieldCharSequence("a${REGULAR_EMOJI}e\u0301${COMPLEX_EMOJI}b"))
        with(transformation) { buffer.transformOutput() }

        assertThat(visitedRanges)
            .containsExactly(
                TextRange(11, 12),
                TextRange(5, 11),
                TextRange(3, 5),
                TextRange(1, 3),
                TextRange(0, 1),
            )
            .inOrder()
    }

    @Test
    fun adjustsSelection_whenGlyphLengthsChange() = withEmojiCompat {
        val transformation = OutputTransformationPerGlyph { "*" }
        // "a" [0, 1), COMPLEX_EMOJI [1, 7), "b" [7, 8); select COMPLEX_EMOJI at [1, 7)
        val buffer =
            TextFieldBuffer(
                TextFieldCharSequence(
                    text = "a${COMPLEX_EMOJI}b",
                    selection = TextRange(1, 7),
                )
            )
        with(transformation) { buffer.transformOutput() }
        assertThat(buffer.toString()).isEqualTo("***")
        assertThat(buffer.selection).isEqualTo(TextRange(1, 2))
    }

    @Test
    fun transformedTextFieldState_mapsOffsetsForwardAndBackward() = withEmojiCompat {
        // Source: "a" [0, 1), COMPLEX_EMOJI [1, 7), "b" [7, 8)
        // Visual (each glyph replaced with 2-char REGULAR_EMOJI):
        // REGULAR_EMOJI [0, 2), REGULAR_EMOJI [2, 4), REGULAR_EMOJI [4, 6)
        val state = TextFieldState("a${COMPLEX_EMOJI}b")
        val transformation = OutputTransformationPerGlyph { REGULAR_EMOJI }
        val transformedState =
            TransformedTextFieldState(
                textFieldState = state,
                outputTransformation = transformation,
            )

        assertThat(transformedState.visualText.toString())
            .isEqualTo("$REGULAR_EMOJI$REGULAR_EMOJI$REGULAR_EMOJI")

        // Forward mapping (untransformed -> transformed)
        assertThat(transformedState.mapToTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapToTransformed(1)).isEqualTo(TextRange(2))
        assertThat(transformedState.mapToTransformed(3)).isEqualTo(TextRange(2, 4))
        assertThat(transformedState.mapToTransformed(7)).isEqualTo(TextRange(4))
        assertThat(transformedState.mapToTransformed(8)).isEqualTo(TextRange(6))

        // Backward mapping (transformed -> untransformed)
        assertThat(transformedState.mapFromTransformed(0)).isEqualTo(TextRange(0))
        assertThat(transformedState.mapFromTransformed(1)).isEqualTo(TextRange(0, 1))
        assertThat(transformedState.mapFromTransformed(2)).isEqualTo(TextRange(1))
        assertThat(transformedState.mapFromTransformed(3)).isEqualTo(TextRange(1, 7))
        assertThat(transformedState.mapFromTransformed(4)).isEqualTo(TextRange(7))
        assertThat(transformedState.mapFromTransformed(5)).isEqualTo(TextRange(7, 8))
        assertThat(transformedState.mapFromTransformed(6)).isEqualTo(TextRange(8))
    }

    companion object {
        // Rainbow flag (🏳️‍🌈): white flag (U+1F3F3), variation selector-16 (U+FE0F),
        // zero-width joiner (U+200D), rainbow (U+1F308). Multiple codepoints (6 UTF-16 chars).
        private const val COMPLEX_EMOJI = "\ud83c\udff3\ufe0f\u200d\ud83c\udf08"
        // Grinning face (😀, U+1F600). Single codepoint (2 UTF-16 chars / surrogate pair).
        private const val REGULAR_EMOJI = "\uD83D\uDE00"
        // Thumbs up with medium skin tone (👍🏽): U+1F44D + U+1F3FD (4 UTF-16 chars).
        private const val SKIN_TONE_EMOJI = "\uD83D\uDC4D\uD83C\uDFFD"
        // Flag: United States (🇺🇸): regional indicators U+1F1FA + U+1F1F8 (4 UTF-16 chars).
        private const val REGIONAL_FLAG_EMOJI = "\uD83C\uDDFA\uD83C\uDDF8"
    }
}
