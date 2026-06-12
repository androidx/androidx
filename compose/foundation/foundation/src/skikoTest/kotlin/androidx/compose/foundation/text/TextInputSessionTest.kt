/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isNull
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlinx.coroutines.awaitCancellation

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class TextInputSessionTest {

    @Test
    fun deleteSurroundingTextInCodePoints_beforeCursor() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(3, 3),
    ) { state, request ->
        request.editText {
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 2, lengthAfterCursor = 0)
        }

        assertThat(state.text.toString()).isEqualTo("adef")
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_afterCursor() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(3, 3),
    ) { state, request ->
        request.editText {
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 0, lengthAfterCursor = 2)
        }

        assertThat(state.text.toString()).isEqualTo("abcf")
        assertThat(state.selection).isEqualTo(TextRange(3, 3))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_bothSides() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(3, 3),
    ) { state, request ->
        request.editText {
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 1, lengthAfterCursor = 2)
        }

        assertThat(state.text.toString()).isEqualTo("abf")
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_withSelection_deletesOutsideSelection() = runSessionTest(
        initialText = "abcdefgh",
        initialSelection = TextRange(2, 5), // "cde" is selected
    ) { state, request ->
        request.editText {
            // Should delete one char before min (2) and one char after max (5).
            // Selected "cde" must remain.
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 1, lengthAfterCursor = 1)
        }

        assertThat(state.text.toString()).isEqualTo("acdegh")
        // Selection must shift by removed chars before it.
        assertThat(state.selection).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_respectsCodePoints() = runSessionTest(
        initialText = "a\uD83D\uDE00b", // "a😀b"; length 4
        initialSelection = TextRange(3, 3), // cursor between emoji and "b"
    ) { state, request ->
        request.editText {
            // lengthBeforeCursor = 1 in code points should remove the whole emoji
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 1, lengthAfterCursor = 0)
        }

        assertThat(state.text.toString()).isEqualTo("ab")
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_zeroLengths_isNoOp() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(3, 3),
    ) { state, request ->
        request.editText {
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 0, lengthAfterCursor = 0)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(3, 3))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_clampsToBounds() = runSessionTest(
        initialText = "abc",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // Request larger deletion than available; should clamp to 0 / length.
            deleteSurroundingTextInCodePoints(lengthBeforeCursor = 100, lengthAfterCursor = 100)
        }

        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection).isEqualTo(TextRange(0, 0))
    }

    @Test
    fun selectText_normalRange() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setSelection(start = 1, end = 4)
        }

        assertThat(state.selection).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun selectText_collapsed_movesCursor() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setSelection(start = 3, end = 3)
        }

        assertThat(state.selection).isEqualTo(TextRange(3, 3))
    }

    @Test
    fun selectText_reversed_keepsDirection() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setSelection(start = 4, end = 1)
        }

        assertThat(state.selection).isEqualTo(TextRange(4, 1))
    }

    @Test
    fun selectText_outOfBounds_isCoerced() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setSelection(start = -5, end = 100)
        }

        assertThat(state.selection).isEqualTo(TextRange(0, 6))
    }

    @Test
    fun commitText_insertAtCursor() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            commitText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
        assertThat(state.composition).isNull()
    }

    @Test
    fun commitText_replacesSelection() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4), // "XY" is selected
    ) { state, request ->
        request.editText {
            commitText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun commitText_replacesCompositionNotSelection() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 6),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4)
        }

        request.editText {
            commitText("XY", 1)
        }

        // Composition "cd" is replaced, leaving "ab" + "XY" + "ef".
        assertThat(state.text.toString()).isEqualTo("abXYef")
        assertThat(state.composition).isNull()
        // Cursor anchored at end of the inserted range: insertionStart(2) + text.length(2) = 4.
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun commitText_newCursorPositionGreaterThanOne() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // insertion end = 4; cursor = 4 + 2 - 1 = 5
            commitText("cd", 2)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun commitText_newCursorPositionZero_placesCursorBeforeInsertion() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // newCursor = 4; result = 4 + 0 - 2 = 2
            commitText("cd", 0)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
    }

    @Test
    fun commitText_newCursorPositionNegative() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // newCursor = 4; result = 4 + (-1) - 2 = 1
            commitText("cd", -1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun commitText_newCursorPositionFarOutOfBounds_isCoerced() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            commitText("cd", 1000)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(6, 6))
    }

    @Test
    fun selectComposingText_setsComposition() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 1, end = 4)
        }

        assertThat(state.composition).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun setComposingRegion_reversed_setsCoercedComposition() = runSessionTest(
        initialText = "abcde",
        initialSelection = TextRange(5),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 4, end = 1)
        }

        assertThat(state.composition).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun setComposingRegion_outOfBounds_setsCoercedComposition() = runSessionTest(
        initialText = "abcde",
        initialSelection = TextRange(5),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = -1, end = 10)
        }

        assertThat(state.composition).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun setComposingRegion_emptyRange_doesNotSetComposition() = runSessionTest(
        initialText = "abcde",
        initialSelection = TextRange(5),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 2)
        }

        assertThat(state.composition).isNull()
    }

    @Test
    fun setComposingText_insertsAndMarksComposition() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            setComposingText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun setComposingText_replacesExistingComposition() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4) // compose "cd"
        }

        request.editText {
            setComposingText("XYZ", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abXYZef")
        assertThat(state.composition).isEqualTo(TextRange(2, 5))
        // Cursor anchored at end of the inserted range: insertionStart(2) + text.length(3) = 5.
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun setComposingText_replacesSelectionWhenNoComposition() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4), // "XY" selected
    ) { state, request ->
        request.editText {
            setComposingText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun setComposingText_emptyText_doesNotSetComposition() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4)
        }

        request.editText {
            // Empty replacement removes the composing range from the buffer and
            // intentionally does not re-mark any range as composition.
            setComposingText("", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abef")
        assertThat(state.composition).isNull()
    }

    @Test
    fun setComposingText_newCursorPositionGreaterThanOne() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // newCursor after replace = 4; result = 4 + 2 - 1 = 5
            setComposingText("cd", 2)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun setComposingText_newCursorPositionZero() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // newCursor after replace = 4; result = 4 + 0 - 2 = 2
            setComposingText("cd", 0)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun setComposingText_newCursorPositionNegative() = runSessionTest(
        initialText = "abef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            // newCursor after replace = 4; result = 4 + (-1) - 2 = 1
            setComposingText("cd", -1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun finishComposingText_clearsCompositionAndKeepsText() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4)
        }

        assertThat(state.composition).isEqualTo(TextRange(2, 4))

        request.editText {
            finishComposingText()
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isNull()
    }

    @Test
    fun finishComposingText_withNoComposition_isNoOp() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(2, 2),
    ) { state, request ->
        request.editText {
            finishComposingText()
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isNull()
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
    }

    @Test
    fun commitText_collapsesReversedSelection() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(4, 2), // reversed non-collapsed selection over "XY"
    ) { state, request ->
        request.editText {
            commitText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        // min(4, 2) + "cd".length = 2 + 2 = 4
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun commitText_withSelection_newCursorPositionAfterEnd() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4),
    ) { state, request ->
        request.editText {
            // insertionEnd = 4; newCursorPosition=2 => 4 + 2 - 1 = 5
            commitText("cd", 2)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun commitText_withSelection_newCursorPositionBeforeStart() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4),
    ) { state, request ->
        request.editText {
            // insertionEnd = 4; newCursorPosition=-1 => 4 + (-1) - 2 = 1
            commitText("cd", -1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun commitText_withCompositionAndSelectionOutside_cursorLandsAtCompositionEnd() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(5, 5), // cursor after the composition range
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4) // "cd"
        }

        request.editText {
            commitText("XYZ", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abXYZef")
        assertThat(state.composition).isNull()
        // insertionStart(2) + "XYZ".length(3) = 5
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun commitText_withCompositionAndSelectionBefore_cursorLandsAtCompositionEnd() = runSessionTest(
        initialText = "abcdef",
        initialSelection = TextRange(0, 0),
    ) { state, request ->
        request.editText {
            setComposingRegion(start = 2, end = 4)
        }

        request.editText {
            commitText("XYZ", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abXYZef")
        // Cursor anchored at insertionStart(2) + text.length(3) = 5, NOT carried over from the
        // pre-existing collapsed cursor at 0.
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun setComposingText_collapsesReversedSelection() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(4, 2), // reversed
    ) { state, request ->
        request.editText {
            setComposingText("cd", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
        assertThat(state.selection).isEqualTo(TextRange(4, 4))
    }

    @Test
    fun setComposingText_withSelection_newCursorPositionAfterEnd() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4),
    ) { state, request ->
        request.editText {
            // insertionEnd = 4; newCursorPosition=2 => 4 + 2 - 1 = 5
            setComposingText("cd", 2)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
        assertThat(state.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun setComposingText_withSelection_newCursorPositionBeforeStart() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4),
    ) { state, request ->
        request.editText {
            // insertionEnd = 4; newCursorPosition=-1 => 4 + (-1) - 2 = 1
            setComposingText("cd", -1)
        }

        assertThat(state.text.toString()).isEqualTo("abcdef")
        assertThat(state.composition).isEqualTo(TextRange(2, 4))
        assertThat(state.selection).isEqualTo(TextRange(1, 1))
    }

    @Test
    fun setComposingText_withExistingCompositionAndSelectionOutside_cursorLandsAtCompositionEnd() =
        runSessionTest(
            initialText = "abcdef",
            initialSelection = TextRange(5, 5),
        ) { state, request ->
            request.editText {
                setComposingRegion(start = 2, end = 4)
            }

            request.editText {
                setComposingText("XYZ", 1)
            }

            assertThat(state.text.toString()).isEqualTo("abXYZef")
            assertThat(state.composition).isEqualTo(TextRange(2, 5))
            assertThat(state.selection).isEqualTo(TextRange(5, 5))
        }

    @Test
    fun commitText_collapsesNonCollapsedSelection() = runSessionTest(
        initialText = "abcdefgh",
        initialSelection = TextRange(1, 7), // large non-collapsed selection
    ) { state, request ->
        request.editText {
            commitText("X", 1)
        }

        // Replacement range = selection (1, 7), so "bcdefg" is replaced with "X".
        assertThat(state.text.toString()).isEqualTo("aXh")
        // Cursor collapses to insertionStart(1) + "X".length(1) = 2.
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
    }

    @Test
    fun commitText_insertsEmptyString_placesCursorAtInsertionStart() = runSessionTest(
        initialText = "abXYef",
        initialSelection = TextRange(2, 4),
    ) { state, request ->
        request.editText {
            // Commit empty text effectively deletes the selection and places cursor at insertionStart.
            commitText("", 1)
        }

        assertThat(state.text.toString()).isEqualTo("abef")
        // insertionStart(2) + "".length(0) = 2
        assertThat(state.selection).isEqualTo(TextRange(2, 2))
    }

    @Suppress("DEPRECATION")
    private fun runSessionTest(
        initialText: String,
        initialSelection: TextRange,
        block: ComposeUiTest.(
            state: TextFieldState,
            request: PlatformTextInputMethodRequest,
        ) -> Unit,
    ) = runComposeUiTest {
        val state = TextFieldState(
            initialText = initialText,
            initialSelection = initialSelection,
        )
        lateinit var request: PlatformTextInputMethodRequest
        setContent {
            InterceptPlatformTextInput({ r, _ ->
                request = r
                awaitCancellation()
            }) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.testTag("input"),
                )
            }
        }

        onNodeWithTag("input").requestFocus()

        block(state, request)
    }
}
