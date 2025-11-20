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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class SetComposingTextCommandTest : ImeEditCommandTest() {

    @Test
    fun test_insert_empty() {
        initialize("", TextRange.Zero)

        imeScope.setComposingText("X", 1)

        assertThat(state.text.toString()).isEqualTo("X")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(0)
        assertThat(state.composition?.end).isEqualTo(1)
    }

    @Test
    fun test_insert_cursor_tail() {
        initialize("A", TextRange(1))

        imeScope.setComposingText("X", 1)

        assertThat(state.text.toString()).isEqualTo("AX")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_head() {
        initialize("A", TextRange(1))

        imeScope.setComposingText("X", 0)

        assertThat(state.text.toString()).isEqualTo("AX")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_tail() {
        initialize("ABCDE", TextRange(1))

        imeScope.setComposingText("X", 2)

        assertThat(state.text.toString()).isEqualTo("AXBCDE")
        assertThat(state.selection.start).isEqualTo(3)
        assertThat(state.selection.end).isEqualTo(3)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_head() {
        initialize("ABCDE", TextRange(4))

        imeScope.setComposingText("X", -2)

        assertThat(state.text.toString()).isEqualTo("ABCDXE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(4)
        assertThat(state.composition?.end).isEqualTo(5)
    }

    @Test
    fun test_insert_empty_text_cursor_head() {
        initialize("ABCDE", TextRange(1))

        imeScope.setComposingText("", 0)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_insert_empty_text_cursor_tail() {
        initialize("ABCDE", TextRange(1))

        imeScope.setComposingText("", 1)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_insert_empty_text_cursor_far_tail() {
        initialize("ABCDE", TextRange(1))

        imeScope.setComposingText("", 2)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_insert_empty_text_cursor_far_head() {
        initialize("ABCDE", TextRange(4))

        imeScope.setComposingText("", -2)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_cancel_composition() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setComposingRegion(1, 4) // Mark "BCD" as composition
        imeScope.setComposingText("X", 1)

        assertThat(state.text.toString()).isEqualTo("AXE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_replace_selection() {
        initialize("ABCDE", TextRange(1, 4)) // select "BCD"

        imeScope.setComposingText("X", 1)

        assertThat(state.text.toString()).isEqualTo("AXE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(2)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_composition_and_selection() {
        initialize("ABCDE", TextRange(1, 3)) // select "BC"

        imeScope.setComposingRegion(2, 4) // Mark "CD" as composition
        imeScope.setComposingText("X", 1)

        // If composition and selection exists at the same time, replace composition and cancel
        // selection and place cursor.
        assertThat(state.text.toString()).isEqualTo("ABXE")
        assertThat(state.selection.start).isEqualTo(3)
        assertThat(state.selection.end).isEqualTo(3)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(2)
        assertThat(state.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_cursor_position_too_small() {
        initialize("ABCDE", TextRange(5))

        imeScope.setComposingText("X", -1000)

        assertThat(state.text.toString()).isEqualTo("ABCDEX")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(5)
        assertThat(state.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_cursor_position_too_large() {
        initialize("ABCDE", TextRange(5))

        imeScope.setComposingText("X", 1000)

        assertThat(state.text.toString()).isEqualTo("ABCDEX")
        assertThat(state.selection.start).isEqualTo(6)
        assertThat(state.selection.end).isEqualTo(6)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(5)
        assertThat(state.composition?.end).isEqualTo(6)
    }
}
