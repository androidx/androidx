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
class DeleteSurroundingTextInCodePointsCommandTest {
    val CH1 = "\uD83D\uDE00" // U+1F600
    val CH2 = "\uD83D\uDE01" // U+1F601
    val CH3 = "\uD83D\uDE02" // U+1F602
    val CH4 = "\uD83D\uDE03" // U+1F603
    val CH5 = "\uD83D\uDE04" // U+1F604

    @Test
    fun test_delete_after() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        eb.deleteSurroundingTextInCodePoints(0, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH3$CH4$CH5")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(2))

        eb.deleteSurroundingTextInCodePoints(1, 0)

        assertThat(eb.toString()).isEqualTo("$CH2$CH3$CH4$CH5")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4))

        eb.deleteSurroundingTextInCodePoints(0, 2)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(2, 0)

        assertThat(eb.toString()).isEqualTo("$CH1$CH4$CH5")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(2, 2)

        assertThat(eb.toString()).isEqualTo(CH1)
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(4, 8))

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH3$CH4")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(6)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(1000, 0)

        assertThat(eb.toString()).isEqualTo("$CH4$CH5")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(0, 1000)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH3")
        assertThat(eb.selection.start).isEqualTo(6)
        assertThat(eb.selection.end).isEqualTo(6)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.deleteSurroundingTextInCodePoints(1000, 1000)

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 2)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(8, 10)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.composition?.start).isEqualTo(4)
        assertThat(eb.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 6)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(4)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(6, 10)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.composition?.start).isEqualTo(4)
        assertThat(eb.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(4, 6)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = TextFieldBuffer("$CH1$CH2$CH3$CH4$CH5", TextRange(6))

        eb.setComposition(0, 10)

        eb.deleteSurroundingTextInCodePoints(1, 1)

        assertThat(eb.toString()).isEqualTo("$CH1$CH2$CH5")
        assertThat(eb.selection.start).isEqualTo(4)
        assertThat(eb.selection.end).isEqualTo(4)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(6)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        val eb = TextFieldBuffer("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                eb.deleteSurroundingTextInCodePoints(
                    lengthBeforeCursor = 0,
                    lengthAfterCursor = -42
                )
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        val eb = TextFieldBuffer("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                eb.deleteSurroundingTextInCodePoints(
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
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = 0,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(eb.toString()).isEqualTo(textAfterDelete)
        assertThat(eb.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(eb.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows_withMaxValue() {
        val text = "abcde"
        val selection = TextRange(1)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = 0
        )

        assertThat(eb.toString()).isEqualTo("bcde")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenBothOverflow_withMaxValue_cursorAtStart() {
        val text = "abcde"
        val selection = TextRange(0)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenBothOverflow_withMaxValue_cursorAtEnd() {
        val text = "abcde"
        val selection = TextRange(5)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE,
            lengthAfterCursor = Int.MAX_VALUE
        )

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = 0,
            lengthAfterCursor = Int.MAX_VALUE - 1
        )

        assertThat(eb.toString()).isEqualTo(textAfterDelete)
        assertThat(eb.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(eb.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows() {
        val text = "abcde"
        val selection = TextRange(1)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingTextInCodePoints(
            lengthBeforeCursor = Int.MAX_VALUE - 1,
            lengthAfterCursor = 0
        )

        assertThat(eb.toString()).isEqualTo("bcde")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }
}
