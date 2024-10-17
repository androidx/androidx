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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class DeleteSurroundingTextInCodePointsCommandTest : ImeEditCommandTest() {
    val CH1 = "\uD83D\uDE00" // U+1F600
    val CH2 = "\uD83D\uDE01" // U+1F601
    val CH3 = "\uD83D\uDE02" // U+1F602
    val CH4 = "\uD83D\uDE03" // U+1F603
    val CH5 = "\uD83D\uDE04" // U+1F604

    @Test
    fun test_delete_after() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        imeScope.deleteSurroundingTextInCodePoints(0, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH3$CH4$CH5")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        imeScope.deleteSurroundingTextInCodePoints(1, 0)

        assertThat(state.text.toString()).isEqualTo("$CH2$CH3$CH4$CH5")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_after_multiple() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(4))

        imeScope.deleteSurroundingTextInCodePoints(0, 2)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before_multiple() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(2, 0)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH4$CH5")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both_multiple() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(2, 2)

        assertThat(state.text.toString()).isEqualTo(CH1)
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_selection_preserve() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(4, 8))

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH3$CH4")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(6)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before_too_many() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(1000, 0)

        assertThat(state.text.toString()).isEqualTo("$CH4$CH5")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_after_too_many() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(0, 1000)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH3")
        assertThat(state.selection.start).isEqualTo(6)
        assertThat(state.selection.end).isEqualTo(6)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both_too_many() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.deleteSurroundingTextInCodePoints(1000, 1000)

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(0, 2)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(8, 10)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition?.start).isEqualTo(4)
        assertThat(state.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(0, 6)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(4)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(6, 10)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition?.start).isEqualTo(4)
        assertThat(state.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_delete_covered_composition() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(4, 6)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_composition_covered() {
        initialize("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        imeScope.setComposingRegion(0, 10)

        imeScope.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(state.text.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(6)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        initialize("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                imeScope.deleteSurroundingTextInCodePoints(
                    lengthBeforeCursor = 0,
                    lengthAfterCursor = -42
                )
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        initialize("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                imeScope.deleteSurroundingTextInCodePoints(
                    lengthBeforeCursor = -42,
                    lengthAfterCursor = 0
                )
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows_withMaxValue() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = 0,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(state.text.toString()).isEqualTo(textAfterDelete)
        assertThat(state.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(state.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows_withMaxValue() {
        val text = "abcde"
        val selection = TextRange(1)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = 0
        )

        assertThat(state.text.toString()).isEqualTo("bcde")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenBothOverflow_withMaxValue_cursorAtStart() {
        val text = "abcde"
        val selection = TextRange(0)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenBothOverflow_withMaxValue_cursorAtEnd() {
        val text = "abcde"
        val selection = TextRange(5)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = 0,
            lengthAfterCursor = Int.MAX_VALUE - 1
        )

        assertThat(state.text.toString()).isEqualTo(textAfterDelete)
        assertThat(state.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(state.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows() {
        val text = "abcde"
        val selection = TextRange(1)
        initialize(text, selection)

        imeScope.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE - 1,
            lengthAfterCursor = 0
        )

        assertThat(state.text.toString()).isEqualTo("bcde")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }
}
