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

package androidx.compose.foundation.text.input.internal.undo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.internal.DefaultImeEditCommandScope
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.commitText
import androidx.compose.foundation.text.input.internal.finishComposingText
import androidx.compose.foundation.text.input.setSelectionCoerced
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextUndoTest {

    @Test
    fun insertionFromEndPointCanMerge() {
        val state = TextFieldState()
        state.typeAtEnd("a")
        state.typeAtEnd("b")
        state.typeAtEnd("c")

        assertThat(state.text.toString()).isEqualTo("abc")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()

        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun insertionFromStartPointCannotMerge() {
        val state = TextFieldState()
        state.typeAtStart("a")
        state.typeAtStart("b")
        state.typeAtStart("c")

        assertThat(state.text.toString()).isEqualTo("cba")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ba")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("a")
    }

    @Test
    fun insertionFromMiddleCannotMerge() {
        val state = TextFieldState()
        state.typeAtEnd("a")
        state.typeAtEnd("c")
        state.typeAt(1, "b")

        assertThat(state.text.toString()).isEqualTo("abc")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ac")
        assertThat(state.selection).isEqualTo(TextRange(1))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun deletionFromEndPointCanMerge() {
        val state = TextFieldState("abc")
        state.placeCursorAt(3)
        state.deleteAt(2)
        state.deleteAt(1)
        state.deleteAt(0)

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()

        assertThat(state.text.toString()).isEqualTo("abc")
    }

    @Test
    fun deletionFromStartPointCanMerge() {
        val state = TextFieldState("abc")
        state.placeCursorAt(0)
        state.deleteAt(0)
        state.deleteAt(0)
        state.deleteAt(0)

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abc")
    }

    @Test
    fun deletionFromMiddleCannotMerge() {
        val state = TextFieldState("abc")
        state.placeCursorAt(2) // "ab|c"
        state.deleteAt(1) // "a|c"
        state.deleteAt(1) // "a|"
        state.deleteAt(0) // "|"

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("a")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ac")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abc")
    }

    @Test
    fun deletionsWithDifferentDirectionsCannotMerge() {
        val state = TextFieldState("abcdef")
        state.placeCursorAt(3) // "abc|def"
        state.deleteAt(2) // "ab|def"
        state.deleteAt(2) // "ab|ef"

        assertThat(state.text.toString()).isEqualTo("abef")
        assertThat(state.undoState.canUndo).isTrue()

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abdef")

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abcdef")
    }

    @Test
    fun insertionAndDeletionNeverMerge() {
        val state = TextFieldState()
        state.typeAtEnd("a") // "a|"
        state.typeAtEnd("b") // "ab|"
        state.deleteAt(1) // "a|"
        state.typeAtStart("c") // "|a" -> "c|a"

        assertThat(state.text.toString()).isEqualTo("ca")
        assertThat(state.selection).isEqualTo(TextRange(1))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("a")
        assertThat(state.selection).isEqualTo(TextRange(0))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ab")
        assertThat(state.selection).isEqualTo(TextRange(2))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun replaceDoesNotMergeWithInsertion() {
        val state = TextFieldState("abc")
        state.typeAtEnd("d") // "abcd|"
        state.replaceAt(0, 4, "def") // "def|"
        state.typeAtEnd("g") // "defg|"

        assertThat(state.text.toString()).isEqualTo("defg")
        assertThat(state.selection).isEqualTo(TextRange(4))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("def")
        assertThat(state.selection).isEqualTo(TextRange(3))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abcd")
        assertThat(state.selection).isEqualTo(TextRange(4))
    }

    @Test
    fun replaceDoesNotMergeWithDeletion() {
        val state = TextFieldState("abc")
        state.placeCursorAt(3)
        state.deleteAt(2) // "ab|"
        state.replaceAt(0, 2, "def") // "def|"
        state.deleteAt(2) // "de|"

        assertThat(state.text.toString()).isEqualTo("de")
        assertThat(state.selection).isEqualTo(TextRange(2))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("def")
        assertThat(state.selection).isEqualTo(TextRange(3))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ab")
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun newLineInsert_doesNotMerge() {
        val state = TextFieldState()
        state.typeAtEnd("a") // "a|"
        state.typeAtEnd("b") // "ab|"
        state.typeAtEnd("\n") // "ab\n|"
        state.typeAtEnd("c") // "ab\nc|"

        assertThat(state.text.toString()).isEqualTo("ab\nc")
        assertThat(state.selection).isEqualTo(TextRange(4))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ab\n")
        assertThat(state.selection).isEqualTo(TextRange(3))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("ab")
        assertThat(state.selection).isEqualTo(TextRange(2))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun undoRecoversSelectionState() {
        val state = TextFieldState("abc")
        state.select(2, 0) // "|ab|c"
        state.type("d") // "d|c"

        assertThat(state.text.toString()).isEqualTo("dc")
        assertThat(state.selection).isEqualTo(TextRange(1))

        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("abc")
        assertThat(state.selection).isEqualTo(TextRange(2, 0))
    }

    @Test
    fun redoDoesNotRecoverSelectionState() {
        val state = TextFieldState("abc")
        state.typeAtEnd("d") // "abcd|"
        state.select(2, 0) // "|ab|cd"
        state.type("e") // "e|cd"

        assertThat(state.text.toString()).isEqualTo("ecd")
        assertThat(state.selection).isEqualTo(TextRange(1))

        state.undoState.undo() // "|ab|cd"
        state.undoState.undo() // "|abc"
        state.undoState.redo() // "abcd|"

        assertThat(state.text.toString()).isEqualTo("abcd")
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun undoHistoryIncludesInputTransformation() {
        val allCapsTransformation = InputTransformation.allCaps(Locale.current)
        val state = TextFieldState("abc", TextRange(3))

        // this test also tests for AllCapsTransformation
        state.editAsUser(inputTransformation = allCapsTransformation) {
            commitComposition()
            append("d")
        } // "abcD|"
        state.editAsUser(inputTransformation = allCapsTransformation) {
            commitComposition()
            append("e")
        } // "abcDE|"

        state.undoState.undo() // "abc|"
        assertThat(state.text.toString()).isEqualTo("abc")

        state.undoState.redo() // "abcDE|"
        assertThat(state.text.toString()).isEqualTo("abcDE")
    }

    @Test
    fun directEdits_clearTheUndoHistory() {
        val state = TextFieldState("abc")
        state.typeAtEnd("d")
        state.typeAtStart("e")
        state.typeAtEnd("f")

        state.edit { replace(0, 1, "x") }

        assertThat(state.undoState.canUndo).isEqualTo(false)
        assertThat(state.undoState.canRedo).isEqualTo(false)
    }

    @Test
    fun directEdit_onlySelectionChange_doesNotClearUndoHistory() {
        val state = TextFieldState("abc")
        state.typeAtEnd("d")
        state.typeAtStart("e")
        state.typeAtEnd("f")

        state.edit { selection = TextRange(0) }

        assertThat(state.undoState.canUndo).isEqualTo(true)
    }

    @Test
    fun directEdit_replaceButNoContentChange_clearsUndoHistory() {
        val state = TextFieldState("abc")
        state.typeAtEnd("d")
        state.typeAtStart("e")
        state.typeAtEnd("f")

        val before = state.text.toString()

        state.edit { replace(0, 6, "eabcdf") }

        val after = state.text.toString()

        assertThat(before).isEqualTo(after)
        assertThat(state.undoState.canUndo).isEqualTo(false)
    }

    companion object {

        private fun TextFieldState.typeAtEnd(text: String) {
            placeCursorAt(this.text.length)
            typeAt(this.text.length, text)
        }

        private fun TextFieldState.typeAtStart(text: String) {
            placeCursorAt(0)
            typeAt(0, text)
        }

        private fun TextFieldState.typeAt(index: Int, text: String) {
            placeCursorAt(index)
            editAsUser(inputTransformation = null) { replace(index, index, text) }
        }

        private fun TextFieldState.type(text: String) {
            with(DefaultImeEditCommandScope(TransformedTextFieldState(this))) {
                beginBatchEdit()
                finishComposingText()
                commitText(text, 1)
                endBatchEdit()
            }
        }

        private fun TextFieldState.deleteAt(index: Int) {
            editAsUser(inputTransformation = null) { delete(index, index + 1) }
        }

        private fun TextFieldState.placeCursorAt(index: Int) {
            select(index, index)
        }

        private fun TextFieldState.select(start: Int, end: Int) {
            editAsUser(inputTransformation = null) { setSelectionCoerced(start, end) }
        }

        private fun TextFieldState.replaceAt(start: Int, end: Int, newText: String) {
            editAsUser(inputTransformation = null) { replace(start, end, newText) }
        }
    }
}
