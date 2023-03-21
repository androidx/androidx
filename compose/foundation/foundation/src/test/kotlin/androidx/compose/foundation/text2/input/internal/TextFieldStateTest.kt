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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.placeCursorAtEnd
import androidx.compose.foundation.text2.input.placeCursorBeforeChar
import androidx.compose.foundation.text2.input.placeCursorBeforeCodepoint
import androidx.compose.foundation.text2.input.selectAll
import androidx.compose.foundation.text2.input.selectChars
import androidx.compose.foundation.text2.input.selectCodepoints
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text2.input.setTextAndSelectAll
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextFieldStateTest {

    private val state = TextFieldState()

    @Test
    fun initialValue() {
        assertThat(state.value.text).isEqualTo("")
    }

    @Test
    fun edit_doesNotChange_whenThrows() {
        class ExpectedException : RuntimeException()

        assertFailsWith<ExpectedException> {
            state.edit {
                replace(0, 0, "hello")
                throw ExpectedException()
            }
        }

        assertThat(state.value.text).isEmpty()
    }

    @Test
    fun edit_replace_changesValueInPlace() {
        state.edit {
            replace(0, 0, "hello")
            assertThat(toString()).isEqualTo("hello")
            assertThat(length).isEqualTo(5)
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_replace_changesStateAfterReturn() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("hello")
    }

    @Test
    fun edit_replace_doesNotChangeStateUntilReturn() {
        state.edit {
            replace(0, 0, "hello")
            assertThat(state.value.text).isEmpty()
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_multipleOperations() {
        state.edit {
            replace(0, 0, "hello")
            replace(5, 5, "world")
            replace(5, 5, " ")
            replace(6, 11, "Compose")
            assertThat(toString()).isEqualTo("hello Compose")
            assertThat(state.value.text).isEmpty()
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("hello Compose")
    }

    @Test
    fun edit_placeCursorAtEnd() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorAtEnd()
        }
        assertThat(state.value.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun edit_placeCursorBeforeChar_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorBeforeChar(2)
        }
        assertThat(state.value.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun edit_placeCursorBeforeChar_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeChar(500)
            }
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeChar(-1)
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_placeCursorBeforeCodepoint_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorBeforeCodepoint(2)
        }
        assertThat(state.value.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun edit_placeCursorBeforeCodepoint_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCodepoint(500)
            }
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCodepoint(-1)
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_selectAll() {
        state.edit {
            replace(0, 0, "hello")
            selectAll()
        }
        assertThat(state.value.selection).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun edit_selectChars_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            selectChars(TextRange(1, 4))
        }
        assertThat(state.value.selection).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun edit_selectChars_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                selectChars(TextRange(500, 501))
            }
            assertFailsWith<IllegalArgumentException> {
                selectChars(TextRange(-1, 500))
            }
            assertFailsWith<IllegalArgumentException> {
                selectChars(TextRange(500, -1))
            }
            assertFailsWith<IllegalArgumentException> {
                selectChars(TextRange(-500, -1))
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_selectCodepoints_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            selectCodepoints(TextRange(1, 4))
        }
        assertThat(state.value.selection).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun edit_selectCodepoints_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                selectCodepoints(TextRange(500, 501))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepoints(TextRange(-1, 500))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepoints(TextRange(500, -1))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepoints(TextRange(-500, -1))
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_afterEdit() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorAtEnd()
        }
        state.edit {
            assertThat(toString()).isEqualTo("hello")
            replace(5, 5, " world")
            assertThat(toString()).isEqualTo("hello world")
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("hello world")
    }

    @Test
    fun append_char() {
        state.edit {
            append('c')
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("c")
    }

    @Test
    fun append_charSequence() {
        state.edit {
            append("hello")
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("hello")
    }

    @Test
    fun append_charSequence_range() {
        state.edit {
            append("hello world", 0, 5)
            placeCursorAtEnd()
        }
        assertThat(state.value.text).isEqualTo("hello")
    }

    @Test
    fun setTextAndPlaceCursorAtEnd_works() {
        state.setTextAndPlaceCursorAtEnd("Hello")
        assertThat(state.value.text).isEqualTo("Hello")
        assertThat(state.value.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun setTextAndSelectAll_works() {
        state.setTextAndSelectAll("Hello")
        assertThat(state.value.text).isEqualTo("Hello")
        assertThat(state.value.selection).isEqualTo(TextRange(0, 5))
    }
}