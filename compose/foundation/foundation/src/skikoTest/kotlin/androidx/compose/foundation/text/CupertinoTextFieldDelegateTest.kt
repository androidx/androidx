/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.test.IgnoreWasmTarget

class CupertinoTextFieldDelegateTest {
    private val sampleText =
        "aaaa bbb cccc dd e fffffffff?????????!!!!!!!          ...\n" + "ggggggg tttt\n" +
            "Family emoji: \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66, and some text at the end\n" +
            "Split family emoji: \uD83D\uDC68 \uD83D\uDC69 \uD83D\uDC67 \uD83D\uDC66, and some text at the end\n" +
            "Emoji sequence: \uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02\n"
    private val defaultDensity = Density(density = 1f)
    private val fontFamilyResolver = createFontFamilyResolver()

    fun testDetermineCursorDesiredOffset(
        givenOffset: Int,
        expected: Int,
        sampleString: String,
        cursorOffset: Int? = null
    ) {
        val actual = determineCursorDesiredOffset(
            offset = givenOffset,
            createSimpleTextFieldValue(text = sampleText, cursorOffset = cursorOffset),
            textLayoutResult = createSimpleTextLayoutResultProxy(sampleText),
            currentText = sampleText
        )

        // add the substring "<here>" at the expected and actual offsets
        val expectedString = sampleString.substring(0, expected) + "<expected here($expected)>" + sampleString.substring(expected)
        val actualString = sampleString.substring(0, actual) + "<got here($actual)>" + sampleString.substring(actual)

        assertEquals(expected, actual, "$expectedString\n<${"=".repeat(20)}>\n$actualString")
    }

    @Test
    fun determineCursorDesiredOffset_tap_in_the_middle_of_the_word() {
        val givenOffset = 3
        val desiredOffset = 4

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    // TODO: ticket for reviewing our approach will be made and covered in this test
    @Test
    fun determineCursorDesiredOffset_tap_before_punctuation() {
        // https://github.com/JetBrains/compose-multiplatform/issues/4388
        val text = "0@1"
        val givenOffset = 1
        val desiredOffset = 0 // TODO: define what should happen exactly, this test only assures that no crash happens

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, text)
    }

    @Test
    fun determineCursorDesiredOffset_tap_in_the_first_half_of_word() {
        val givenOffset = 23
        val desiredOffset = 19

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    @Ignore // TODO: Remove ignore https://youtrack.jetbrains.com/issue/COMPOSE-1214/
    fun determineCursorDesiredOffset_tap_in_the_middle_of_the_symbols_sequence() {
        val givenOffset = 34
        val desiredOffset = 53

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    @Ignore // TODO: Remove ignore https://youtrack.jetbrains.com/issue/COMPOSE-1214/
    fun determineCursorDesiredOffset_tap_in_the_middle_of_the_whitespaces() {
        val givenOffset = 48
        val desiredOffset = 53

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    @Ignore // TODO: Remove ignore https://youtrack.jetbrains.com/issue/COMPOSE-1214/
    fun determineCursorDesiredOffset_tap_on_the_standalone_symbols_sequence() {
        val givenOffset = 56
        val desiredOffset = 57

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    fun determineCursorDesiredOffset_tap_on_the_right_edge() {
        // Tap was on the first line in the given example string
        val givenOffset = 57
        val desiredOffset = 57

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    @IgnoreWasmTarget
    fun determineCursorDesiredOffset_tap_on_the_right_edge_empty_line() {
        // Tap was on the last line in the given example string
        val givenOffset = 231
        val desiredOffset = 231

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    fun determineCursorDesiredOffset_tap_on_the_left_edge() {
        // Tap was on the second line in the given example string
        val givenOffset = 58
        val desiredOffset = 58

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    @IgnoreWasmTarget
    fun determineCursorDesiredOffset_tap_on_the_left_edge_empty_line() {
        // Tap was on the last line in the given example string
        val givenOffset = 231
        val desiredOffset = 231

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText)
    }

    @Test
    fun determineCursorDesiredOffset_tap_on_the_caret_at_the_same_position() {
        val givenOffset = 24
        val desiredOffset = 24

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText, 24)
    }

    @Test
    fun determineCursorDesiredOffset_tap_between_two__emoji() {
        val givenOffset = 218
        val desiredOffset = 218

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText, 24)
    }

    // TODO: remove ignore after fix two compound emojis considered as one whole word
    @Ignore
    @Test
    fun determineCursorDesiredOffset_tap_between_two_compound_emoji() {
        val givenOffset = 96
        val desiredOffset = 96

        testDetermineCursorDesiredOffset(givenOffset, desiredOffset, sampleText, 24)
    }

    private fun createSimpleTextFieldValue(text: String, cursorOffset: Int?) =
        TextFieldValue(text, selection = TextRange(cursorOffset ?: 0))

    private fun createSimpleTextLayoutResultProxy(text: String) = TextLayoutResultProxy(
        value = TextLayoutResult(
            layoutInput = simpleTextLayoutInput(text),
            multiParagraph = simpleMultiParagraph(text),
            size = IntSize(1083, 150)
        )
    )

    private fun simpleTextLayoutInput(text: String) = TextLayoutInput(
        text = AnnotatedString(text),
        style = TextStyle(),
        placeholders = listOf(),
        maxLines = Int.MAX_VALUE,
        softWrap = false,
        overflow = TextOverflow.Visible,
        density = defaultDensity,
        layoutDirection = LayoutDirection.Ltr,
        fontFamilyResolver = fontFamilyResolver,
        constraints = Constraints(maxWidth = 1000)
    )

    private fun simpleMultiParagraph(text: String) = MultiParagraph(
        annotatedString = AnnotatedString(text),
        style = TextStyle(),
        constraints = Constraints(maxWidth = 1000),
        density = defaultDensity,
        fontFamilyResolver = fontFamilyResolver
    )
}