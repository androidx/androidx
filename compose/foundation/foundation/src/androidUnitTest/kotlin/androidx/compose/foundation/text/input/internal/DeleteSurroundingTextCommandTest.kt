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
internal class DeleteSurroundingTextCommandTest : ImeEditCommandTest() {

    @Test
    fun test_delete_after() {
        initialize("ABCDE", TextRange(1))

        imeScope.deleteSurroundingText(0, 1)

        assertThat(state.text.toString()).isEqualTo("ACDE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before() {
        initialize("ABCDE", TextRange(1))

        imeScope.deleteSurroundingText(1, 0)

        assertThat(state.text.toString()).isEqualTo("BCDE")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_after_multiple() {
        initialize("ABCDE", TextRange(2))

        imeScope.deleteSurroundingText(0, 2)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before_multiple() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(2, 0)

        assertThat(state.text.toString()).isEqualTo("ADE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both_multiple() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(2, 2)

        assertThat(state.text.toString()).isEqualTo("A")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_selection_preserve() {
        initialize("ABCDE", TextRange(2, 4))

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ACD")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(3)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_before_too_many() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(1000, 0)

        assertThat(state.text.toString()).isEqualTo("DE")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_after_too_many() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(0, 1000)

        assertThat(state.text.toString()).isEqualTo("ABC")
        assertThat(state.selection.start).isEqualTo(3)
        assertThat(state.selection.end).isEqualTo(3)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_both_too_many() {
        initialize("ABCDE", TextRange(3))

        imeScope.deleteSurroundingText(1000, 1000)

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_composition_no_intersection_preceding_composition() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(0, 1)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(1)
    }

    @Test
    fun test_delete_composition_no_intersection_trailing_composition() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(4, 5)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition?.start).isEqualTo(2)
        assertThat(state.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_delete_composition_intersection_preceding_composition() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(0, 3)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_delete_composition_intersection_trailing_composition() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(3, 5)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition?.start).isEqualTo(2)
        assertThat(state.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_delete_covered_composition() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(2, 3)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_delete_composition_covered() {
        initialize("ABCDE", TextRange(3))

        imeScope.setComposingRegion(0, 5)

        imeScope.deleteSurroundingText(1, 1)

        assertThat(state.text.toString()).isEqualTo("ABE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(3)
    }

    @Test
    fun throws_whenLengthBeforeInvalid() {
        initialize("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                imeScope.deleteSurroundingText(lengthBeforeCursor = -42, lengthAfterCursor = 0)
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun throws_whenLengthAfterInvalid() {
        initialize("", TextRange(0))
        val error =
            assertFailsWith<IllegalArgumentException> {
                imeScope.deleteSurroundingText(lengthBeforeCursor = 0, lengthAfterCursor = -42)
            }
        assertThat(error).hasMessageThat().contains("-42")
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows_withMaxValue() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        initialize(text, selection)

        imeScope.deleteSurroundingText(lengthBeforeCursor = 0, lengthAfterCursor = Int.MAX_VALUE)

        assertThat(state.text.toString()).isEqualTo(textAfterDelete)
        assertThat(state.selection.start).isEqualTo(textAfterDelete.length)
        assertThat(state.selection.end).isEqualTo(textAfterDelete.length)
    }

    @Test
    fun deletes_whenLengthBeforeCursorOverflows_withMaxValue() {
        val text = "abcde"
        val selection = TextRange(1)
        initialize(text, selection)

        imeScope.deleteSurroundingText(lengthBeforeCursor = Int.MAX_VALUE, lengthAfterCursor = 0)

        assertThat(state.text.toString()).isEqualTo("bcde")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }

    @Test
    fun deletes_whenLengthAfterCursorOverflows() {
        val text = "abcde"
        val textAfterDelete = "abcd"
        val selection = TextRange(textAfterDelete.length)
        initialize(text, selection)

        imeScope.deleteSurroundingText(
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

        imeScope.deleteSurroundingText(
            lengthBeforeCursor = Int.MAX_VALUE - 1,
            lengthAfterCursor = 0
        )

        assertThat(state.text.toString()).isEqualTo("bcde")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
    }
}
