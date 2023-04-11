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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
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
        assertThat(state.text.toString()).isEqualTo("")
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

        assertThat(state.text.toString()).isEmpty()
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
        assertThat(state.text.toString()).isEqualTo("hello")
    }

    @Test
    fun edit_replace_doesNotChangeStateUntilReturn() {
        state.edit {
            replace(0, 0, "hello")
            assertThat(state.text.toString()).isEmpty()
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
            assertThat(state.text.toString()).isEmpty()
            placeCursorAtEnd()
        }
        assertThat(state.text.toString()).isEqualTo("hello Compose")
    }

    @Test
    fun edit_placeCursorAtEnd() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorAtEnd()
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
    }

    @Test
    fun edit_placeCursorBeforeChar_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorBeforeCharAt(2)
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun edit_placeCursorBeforeChar_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCharAt(500)
            }
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCharAt(-1)
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_placeCursorBeforeCodepoint_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorBeforeCodepointAt(2)
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun edit_placeCursorBeforeCodepoint_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCodepointAt(500)
            }
            assertFailsWith<IllegalArgumentException> {
                placeCursorBeforeCodepointAt(-1)
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
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun edit_selectChars_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            selectCharsIn(TextRange(1, 4))
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun edit_selectChars_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                selectCharsIn(TextRange(500, 501))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCharsIn(TextRange(-1, 500))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCharsIn(TextRange(500, -1))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCharsIn(TextRange(-500, -1))
            }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_selectCodepoints_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            selectCodepointsIn(TextRange(1, 4))
        }
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun edit_selectCodepoints_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> {
                selectCodepointsIn(TextRange(500, 501))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepointsIn(TextRange(-1, 500))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepointsIn(TextRange(500, -1))
            }
            assertFailsWith<IllegalArgumentException> {
                selectCodepointsIn(TextRange(-500, -1))
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
        assertThat(state.text.toString()).isEqualTo("hello world")
    }

    @Test
    fun append_char() {
        state.edit {
            append('c')
            placeCursorAtEnd()
        }
        assertThat(state.text.toString()).isEqualTo("c")
    }

    @Test
    fun append_charSequence() {
        state.edit {
            append("hello")
            placeCursorAtEnd()
        }
        assertThat(state.text.toString()).isEqualTo("hello")
    }

    @Test
    fun append_charSequence_range() {
        state.edit {
            append("hello world", 0, 5)
            placeCursorAtEnd()
        }
        assertThat(state.text.toString()).isEqualTo("hello")
    }

    @Test
    fun setTextAndPlaceCursorAtEnd_works() {
        state.setTextAndPlaceCursorAtEnd("Hello")
        assertThat(state.text.toString()).isEqualTo("Hello")
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
    }

    @Test
    fun setTextAndSelectAll_works() {
        state.setTextAndSelectAll("Hello")
        assertThat(state.text.toString()).isEqualTo("Hello")
        assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun replace_changesAreTracked() {
        val state = TextFieldState("hello world")
        state.edit {
            replace(6, 11, "Compose")
            assertThat(toString()).isEqualTo("hello Compose")
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(6, 13))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(6, 11))
            placeCursorAtEnd()
        }
    }

    @Test
    fun appendChar_changesAreTracked() {
        val state = TextFieldState("hello ")
        state.edit {
            append('c')
            assertThat(toString()).isEqualTo("hello c")
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(6, 7))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(6))
            placeCursorAtEnd()
        }
    }

    @Test
    fun appendCharSequence_changesAreTracked() {
        val state = TextFieldState("hello ")
        state.edit {
            append("world")
            assertThat(toString()).isEqualTo("hello world")
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(6, 11))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(6))
            placeCursorAtEnd()
        }
    }

    @Test
    fun appendCharSequenceRange_changesAreTracked() {
        val state = TextFieldState("hello ")
        state.edit {
            append("hello world", 6, 11)
            assertThat(toString()).isEqualTo("hello world")
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(6, 11))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(6))
            placeCursorAtEnd()
        }
    }
}