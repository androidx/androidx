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
class SetComposingTextCommandTest {

    @Test
    fun test_insert_empty() {
        val eb = TextFieldBuffer("", TextRange.Zero)

        eb.setComposingText("X", 1)

        assertThat(eb.toString()).isEqualTo("X")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(0)
        assertThat(eb.composition?.end).isEqualTo(1)
    }

    @Test
    fun test_insert_cursor_tail() {
        val eb = TextFieldBuffer("A", TextRange(1))

        eb.setComposingText("X", 1)

        assertThat(eb.toString()).isEqualTo("AX")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_head() {
        val eb = TextFieldBuffer("A", TextRange(1))

        eb.setComposingText("X", 0)

        assertThat(eb.toString()).isEqualTo("AX")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_tail() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.setComposingText("X", 2)

        assertThat(eb.toString()).isEqualTo("AXBCDE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_insert_cursor_far_head() {
        val eb = TextFieldBuffer("ABCDE", TextRange(4))

        eb.setComposingText("X", -2)

        assertThat(eb.toString()).isEqualTo("ABCDXE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(4)
        assertThat(eb.composition?.end).isEqualTo(5)
    }

    @Test
    fun test_insert_empty_text_cursor_head() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.setComposingText("", 0)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_tail() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.setComposingText("", 1)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(1)
        assertThat(eb.selection.end).isEqualTo(1)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_far_tail() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1))

        eb.setComposingText("", 2)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_insert_empty_text_cursor_far_head() {
        val eb = TextFieldBuffer("ABCDE", TextRange(4))

        eb.setComposingText("", -2)

        assertThat(eb.toString()).isEqualTo("ABCDE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isFalse()
    }

    @Test
    fun test_cancel_composition() {
        val eb = TextFieldBuffer("ABCDE", TextRange.Zero)

        eb.setComposition(1, 4) // Mark "BCD" as composition
        eb.setComposingText("X", 1)

        assertThat(eb.toString()).isEqualTo("AXE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_replace_selection() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1, 4)) // select "BCD"

        eb.setComposingText("X", 1)

        assertThat(eb.toString()).isEqualTo("AXE")
        assertThat(eb.selection.start).isEqualTo(2)
        assertThat(eb.selection.end).isEqualTo(2)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(1)
        assertThat(eb.composition?.end).isEqualTo(2)
    }

    @Test
    fun test_composition_and_selection() {
        val eb = TextFieldBuffer("ABCDE", TextRange(1, 3)) // select "BC"

        eb.setComposition(2, 4) // Mark "CD" as composition
        eb.setComposingText("X", 1)

        // If composition and selection exists at the same time, replace composition and cancel
        // selection and place cursor.
        assertThat(eb.toString()).isEqualTo("ABXE")
        assertThat(eb.selection.start).isEqualTo(3)
        assertThat(eb.selection.end).isEqualTo(3)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(2)
        assertThat(eb.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_cursor_position_too_small() {
        val eb = TextFieldBuffer("ABCDE", TextRange(5))

        eb.setComposingText("X", -1000)

        assertThat(eb.toString()).isEqualTo("ABCDEX")
        assertThat(eb.selection.start).isEqualTo(0)
        assertThat(eb.selection.end).isEqualTo(0)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(5)
        assertThat(eb.composition?.end).isEqualTo(6)
    }

    @Test
    fun test_cursor_position_too_large() {
        val eb = TextFieldBuffer("ABCDE", TextRange(5))

        eb.setComposingText("X", 1000)

        assertThat(eb.toString()).isEqualTo("ABCDEX")
        assertThat(eb.selection.start).isEqualTo(6)
        assertThat(eb.selection.end).isEqualTo(6)
        assertThat(eb.hasComposition()).isTrue()
        assertThat(eb.composition?.start).isEqualTo(5)
        assertThat(eb.composition?.end).isEqualTo(6)
    }
}
