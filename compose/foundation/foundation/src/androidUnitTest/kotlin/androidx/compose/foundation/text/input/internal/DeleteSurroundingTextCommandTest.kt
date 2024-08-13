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
class DeleteSurroundingTextCommandTest {

    @Test
    fun test_delete_after() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.deleteSurroundingText(0, 1)

        assertThat(eb.toString()).isEqualTo("ACDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.deleteSurroundingText(1, 0)

        assertThat(eb.toString()).isEqualTo("BCDE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_multiple() {
        val eb = TextFieldBuffer("ABCDE", TextRange(2))

        eb.deleteSurroundingText(0, 2)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_multiple() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(2, 0)

        assertThat(eb.toString()).isEqualTo("ADE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_multiple() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(2, 2)

        assertThat(eb.toString()).isEqualTo("A")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_selection_preserve() {
        val eb = TextFieldBuffer("ABCDE", TextRange(2, 4))

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ACD")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_before_too_many() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(1000, 0)

        assertThat(eb.toString()).isEqualTo("DE")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_after_too_many() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(0, 1000)

        assertThat(eb.toString()).isEqualTo("ABC")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_both_too_many() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.deleteSurroundingText(1000, 1000)

        assertThat(eb.toString()).isEqualTo("")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 1)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(1)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(4, 5)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 3)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(3, 5)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_delete_covered_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(2, 3)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_delete_composition_covered() {
        val eb = TextFieldBuffer("ABCDE", TextRange(3))

        eb.setComposition(0, 5)

        eb.deleteSurroundingText(1, 1)

        assertThat(eb.toString()).isEqualTo("ABE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        val eb = TextFieldBuffer("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                eb.deleteSurroundingText(lengthBeforeCursor = -42, lengthAfterCursor = 0)
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        val eb = TextFieldBuffer("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                eb.deleteSurroundingText(lengthBeforeCursor = 0, lengthAfterCursor = -42)
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows_withMaxValue() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingText(lengthBeforeCursor = 0, lengthAfterCursor = Int.MAX_VALUE)

        assertThat(eb.toString()).isEqualTo(textAfterDelete)
        assertThat(eb.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(eb.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows_withMaxValue() {
        val text = "abcde"
        val selection = TextRange(1)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingText(lengthBeforeCursor = Int.MAX_VALUE, lengthAfterCursor = 0)

        assertThat(eb.toString()).isEqualTo("bcde")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingText(lengthBeforeCursor = 0, lengthAfterCursor = Int.MAX_VALUE - 1)

        assertThat(eb.toString()).isEqualTo(textAfterDelete)
        assertThat(eb.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(eb.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows() {
        val text = "abcde"
        val selection = TextRange(1)
        val eb = TextFieldBuffer(text, selection)

        eb.deleteSurroundingText(lengthBeforeCursor = Int.MAX_VALUE - 1, lengthAfterCursor = 0)

        assertThat(eb.toString()).isEqualTo("bcde")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
    }
}
