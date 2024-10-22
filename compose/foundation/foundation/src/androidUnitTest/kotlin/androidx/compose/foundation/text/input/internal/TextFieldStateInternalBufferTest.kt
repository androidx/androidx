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

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextFieldStateInternalBufferTest {

    @Test
    fun initializeValue() {
        val firstValue = TextFieldCharSequence("ABCDE", TextRange.Zero)
        val state = TextFieldState(firstValue)

        assertThat(state.value).isEqualTo(firstValue)
    }

    @Test
    fun apply_commitTextCommand_changesValue() {
        val firstValue = TextFieldCharSequence("ABCDE", TextRange.Zero)
        val state = TextFieldState(firstValue)

        var resetCalled = 0
        var selectionCalled = 0
        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }

        state.withImeScope { commitText("X", 1) }
        val newState = state.value

        assertThat(newState.toString()).isEqualTo("XABCDE")
        assertThat(newState.selection.min).isEqualTo(1)
        assertThat(newState.selection.max).isEqualTo(1)
        // edit command updates should not trigger reset listeners.
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun apply_setSelectionCommand_changesValue() {
        val firstValue = TextFieldCharSequence("ABCDE", TextRange.Zero)
        val state = TextFieldState(firstValue)

        var resetCalled = 0
        var selectionCalled = 0
        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }

        state.withImeScope { setSelection(0, 2) }
        val newState = state.value

        assertThat(newState.toString()).isEqualTo("ABCDE")
        assertThat(newState.selection.min).isEqualTo(0)
        assertThat(newState.selection.max).isEqualTo(2)
        // edit command updates should not trigger reset listeners.
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun testNewState_bufferNotUpdated_ifSameModelStructurally() {
        val state = TextFieldState()

        var resetCalled = 0
        var selectionCalled = 0

        val initialBuffer = state.mainBuffer
        state.syncMainBufferToTemporaryBuffer(
            TextFieldCharSequence("qwerty", TextRange.Zero, TextRange.Zero)
        )
        assertThat(state.mainBuffer).isNotSameInstanceAs(initialBuffer)

        val updatedBuffer = state.mainBuffer

        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }
        state.syncMainBufferToTemporaryBuffer(
            TextFieldCharSequence("qwerty", TextRange.Zero, TextRange.Zero)
        )
        assertThat(state.mainBuffer).isSameInstanceAs(updatedBuffer)

        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun testNewState_new_buffer_created_if_text_is_different() {
        val state = TextFieldState()

        var resetCalled = 0
        var selectionCalled = 0
        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }

        val textFieldValue = TextFieldCharSequence("qwerty", TextRange.Zero, TextRange.Zero)
        state.syncMainBufferToTemporaryBuffer(textFieldValue)
        val initialBuffer = state.mainBuffer

        val newTextFieldValue = TextFieldCharSequence("abc")
        state.syncMainBufferToTemporaryBuffer(newTextFieldValue)

        assertThat(state.mainBuffer).isNotSameInstanceAs(initialBuffer)
        // no composing region, reset shouldn't be called
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(2)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_selection_is_different() {
        val state = TextFieldState()

        var resetCalled = 0
        var selectionCalled = 0

        val textFieldValue = TextFieldCharSequence("qwerty", TextRange.Zero, TextRange.Zero)
        state.syncMainBufferToTemporaryBuffer(textFieldValue)
        val initialBuffer = state.mainBuffer

        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }
        val newTextFieldValue = TextFieldCharSequence(textFieldValue, selection = TextRange(1))
        state.syncMainBufferToTemporaryBuffer(newTextFieldValue)

        assertThat(state.mainBuffer).isSameInstanceAs(initialBuffer)
        assertThat(newTextFieldValue.selection.start).isEqualTo(state.mainBuffer.selection.start)
        assertThat(newTextFieldValue.selection.end).isEqualTo(state.mainBuffer.selection.end)
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_composition_is_different() {
        val state = TextFieldState()

        var resetCalled = 0
        var selectionCalled = 0

        val textFieldValue = TextFieldCharSequence("qwerty", TextRange.Zero, TextRange(1))
        state.syncMainBufferToTemporaryBuffer(textFieldValue)
        val initialBuffer = state.mainBuffer

        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }
        // composition can not be set from app, IME owns it.
        assertThat(initialBuffer.composition).isNull()

        val newTextFieldValue = TextFieldCharSequence("qwerty", TextRange.Zero, composition = null)
        state.syncMainBufferToTemporaryBuffer(newTextFieldValue)

        assertThat(state.mainBuffer).isSameInstanceAs(initialBuffer)
        assertThat(state.mainBuffer.composition).isNull()
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun testNewState_reversedSelection_setsTheSelection() {
        val initialSelection = TextRange(2, 1)
        val textFieldValue = TextFieldCharSequence("qwerty", initialSelection, TextRange(1))
        val state = TextFieldState(textFieldValue)

        var resetCalled = 0
        var selectionCalled = 0
        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }

        val initialBuffer = state.mainBuffer

        assertThat(initialSelection.start).isEqualTo(initialBuffer.selection.start)
        assertThat(initialSelection.end).isEqualTo(initialBuffer.selection.end)

        val updatedSelection = TextRange(3, 0)
        val newTextFieldValue = TextFieldCharSequence(textFieldValue, selection = updatedSelection)
        // set the new selection
        state.syncMainBufferToTemporaryBuffer(newTextFieldValue)

        assertThat(state.mainBuffer).isSameInstanceAs(initialBuffer)
        assertThat(updatedSelection.start).isEqualTo(initialBuffer.selection.start)
        assertThat(updatedSelection.end).isEqualTo(initialBuffer.selection.end)
        // content does not change so restart input is unexpected
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun compositionIsCleared_when_textChanged() {
        val state = TextFieldState()

        var resetCalled = 0
        var selectionCalled = 0
        state.addImeContentListener { _, _, restart ->
            if (restart) resetCalled++ else selectionCalled++
        }

        // set the initial value
        state.withImeScope {
            commitText("ab", 0)
            setComposingRegion(0, 2)
        }
        assertThat(resetCalled).isEqualTo(0)
        assertThat(selectionCalled).isEqualTo(1)

        // change the text
        val newValue = TextFieldCharSequence("cd", state.selection, state.composition)
        state.syncMainBufferToTemporaryBuffer(newValue)

        assertThat(state.text.toString()).isEqualTo(newValue.toString())
        assertThat(state.composition).isNull()
        assertThat(resetCalled).isEqualTo(1)
        assertThat(selectionCalled).isEqualTo(1)
    }

    @Test
    fun compositionIsCleared_when_textIsSame() {
        val state = TextFieldState()
        val composition = TextRange(0, 2)

        // set the initial value
        state.withImeScope {
            commitText("ab", 0)
            setComposingRegion(composition.start, composition.end)
        }

        // use the same TextFieldValue
        val newValue = TextFieldCharSequence(state.text, state.selection, state.composition)
        state.syncMainBufferToTemporaryBuffer(newValue)

        assertThat(state.text.toString()).isEqualTo(newValue.toString())
        assertThat(state.composition).isNull()
    }

    @Test
    fun compositionIsCleared_when_compositionReset() {
        val state = TextFieldState()

        // set the initial value
        state.withImeScope {
            commitText("ab", 0)
            setComposingRegion(-1, -1)
        }

        // change the composition
        val newValue =
            TextFieldCharSequence(state.text, state.selection, composition = TextRange(0, 2))
        state.syncMainBufferToTemporaryBuffer(newValue)

        assertThat(state.text.toString()).isEqualTo(newValue.toString())
        assertThat(state.composition).isNull()
    }

    @Test
    fun compositionIsCleared_when_compositionChanged() {
        val state = TextFieldState()

        // set the initial value
        state.withImeScope {
            commitText("ab", 0)
            setComposingRegion(0, 2)
        }

        // change the composition
        val newValue =
            TextFieldCharSequence(state.text, state.selection, composition = TextRange(0, 1))
        state.syncMainBufferToTemporaryBuffer(newValue)

        assertThat(state.text.toString()).isEqualTo(newValue.toString())
        assertThat(state.composition).isNull()
    }

    @Test
    fun compositionIsCleared_when_onlySelectionChanged() {
        val state = TextFieldState()

        val composition = TextRange(0, 2)
        val selection = TextRange(0, 2)

        // set the initial value
        state.withImeScope {
            commitText("ab", 0)
            setComposingRegion(composition.start, composition.end)
            setSelection(selection.start, selection.end)
        }

        // change selection
        val newSelection = TextRange(1)
        val newValue =
            TextFieldCharSequence(
                state.text,
                selection = newSelection,
                composition = state.composition
            )
        state.syncMainBufferToTemporaryBuffer(newValue)

        assertThat(state.text.toString()).isEqualTo(newValue.toString())
        assertThat(state.composition).isNull()
        assertThat(state.selection).isEqualTo(newSelection)
    }

    @Test
    fun filterThatDoesNothing_doesNotResetBuffer() {
        val state =
            TextFieldState(
                TextFieldCharSequence(
                    "abc",
                    selection = TextRange(3),
                    composition = TextRange(0, 3)
                )
            )

        val initialBuffer = state.mainBuffer

        state.withImeScope { commitText("d", 4) }

        val value = state.text

        assertThat(value.toString()).isEqualTo("abcd")
        assertThat(state.mainBuffer).isSameInstanceAs(initialBuffer)
    }

    @Test
    fun returningTheEquivalentValueFromFilter_doesNotResetBuffer() {
        val state =
            TextFieldState(
                TextFieldCharSequence(
                    "abc",
                    selection = TextRange(3),
                    composition = TextRange(0, 3)
                )
            )

        val initialBuffer = state.mainBuffer

        state.withImeScope { commitText("d", 4) }

        val value = state.text

        assertThat(value.toString()).isEqualTo("abcd")
        assertThat(state.mainBuffer).isSameInstanceAs(initialBuffer)
    }

    @Test
    fun returningOldValueFromFilter_resetsTheBuffer() {
        val state =
            TextFieldState(
                TextFieldCharSequence(
                    "abc",
                    selection = TextRange(3),
                    composition = TextRange(0, 3)
                )
            )

        var resetCalledOld: TextFieldCharSequence? = null
        var resetCalledNew: TextFieldCharSequence? = null
        state.addImeContentListener { old, new, _ ->
            resetCalledOld = old
            resetCalledNew = new
        }

        val initialBuffer = state.mainBuffer

        state.withImeScope(inputTransformation = { revertAllChanges() }) { commitText("d", 4) }

        val value = state.text

        assertThat(value.toString()).isEqualTo("abc")
        assertThat(state.mainBuffer).isNotSameInstanceAs(initialBuffer)
        assertThat(resetCalledOld?.toString()).isEqualTo("abcd") // what IME applied
        assertThat(resetCalledNew?.toString()).isEqualTo("abc") // what is decided by filter
    }

    @Test
    fun filterNotRan_whenNoCommands() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = null)
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            fail("filter ran, old=\"${originalValue}\", new=\"${toTextFieldCharSequence()}\"")
        }

        state.withImeScope(inputTransformation) {}
    }

    @Test
    fun filterNotRan_whenOnlyFinishComposingTextCommand_noComposition() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = null)
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            fail("filter ran, old=\"${originalValue}\", new=\"${toTextFieldCharSequence()}\"")
        }

        state.withImeScope(inputTransformation = inputTransformation) { finishComposingText() }
    }

    @Test
    fun filterNotRan_whenOnlyFinishComposingTextCommand_withComposition() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = TextRange(0, 5))
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            fail("filter ran, old=\"${originalValue}\", new=\"${toTextFieldCharSequence()}\"")
        }

        state.withImeScope(inputTransformation = inputTransformation) { finishComposingText() }
    }

    @Test
    fun filterNotRan_whenCommandsResultInInitialValue() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = TextRange(0, 5))
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            val old = originalValue
            val new = toTextFieldCharSequence()
            fail(
                "filter ran, old=\"$old\" (${old.selection}), " + "new=\"$new\" (${new.selection})"
            )
        }

        state.withImeScope(inputTransformation = inputTransformation) {
            setComposingRegion(0, 5)
            commitText("hello", 1)
            setSelection(2, 2)
        }
    }

    @Test
    fun filterRan_whenOnlySelectionChanges() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = null)
        var filterRan = false
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            val old = originalValue
            val new = toTextFieldCharSequence()
            // Filter should only run once.
            assertThat(filterRan).isFalse()
            filterRan = true
            assertThat(new.toString()).isEqualTo(old.toString())
            assertThat(old.selection).isEqualTo(TextRange(2))
            assertThat(new.selection).isEqualTo(TextRange(0, 5))
        }

        state.withImeScope(inputTransformation = inputTransformation) { setSelection(0, 5) }
    }

    @Test
    fun filterRan_whenOnlyTextChanges() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(2), composition = null)
        var filterRan = false
        val state = TextFieldState(initialValue)
        val inputTransformation = InputTransformation {
            val old = originalValue
            val new = toTextFieldCharSequence()
            // Filter should only run once.
            assertThat(filterRan).isFalse()
            filterRan = true
            assertThat(new.selection).isEqualTo(old.selection)
            assertThat(old.toString()).isEqualTo("hello")
            assertThat(new.toString()).isEqualTo("world")
        }

        state.withImeScope(inputTransformation = inputTransformation) {
            edit { delete(0, length) }
            commitText("world", 1)
            setSelection(2, 2)
        }
    }

    @Test
    fun stateUpdated_whenOnlyCompositionChanges_noFilter() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(5), composition = TextRange(0, 5))
        val state = TextFieldState(initialValue)

        state.withImeScope { setComposingRegion(2, 3) }

        assertThat(state.composition).isEqualTo(TextRange(2, 3))
    }

    @Test
    fun stateUpdated_whenOnlyCompositionChanges_withFilter() {
        val initialValue =
            TextFieldCharSequence("hello", selection = TextRange(5), composition = TextRange(0, 5))
        val state = TextFieldState(initialValue)

        state.withImeScope { setComposingRegion(2, 3) }

        assertThat(state.composition).isEqualTo(TextRange(2, 3))
    }

    private fun TextFieldState(value: TextFieldCharSequence) =
        TextFieldState(value.toString(), value.selection)

    private fun TextFieldState.addImeContentListener(
        listener: (TextFieldCharSequence, TextFieldCharSequence, Boolean) -> Unit
    ) {
        addNotifyImeListener { oldValue, newValue, restartImeIfContentChanges ->
            listener(oldValue, newValue, restartImeIfContentChanges)
        }
    }

    private fun TextFieldState.syncMainBufferToTemporaryBuffer(
        textFieldCharSequence: TextFieldCharSequence
    ) {
        syncMainBufferToTemporaryBuffer(
            temporaryBuffer = TextFieldBuffer(textFieldCharSequence),
            textChanged = !textFieldCharSequence.contentEquals(mainBuffer.toString()),
            selectionChanged = textFieldCharSequence.selection != mainBuffer.selection,
        )
    }
}

internal fun TextFieldState.withImeScope(
    inputTransformation: InputTransformation? = null,
    block: ImeEditCommandScope.() -> Unit
) {
    val transformedState = TransformedTextFieldState(this, inputTransformation)
    with(DefaultImeEditCommandScope(transformedState)) {
        beginBatchEdit()
        block()
        endBatchEdit()
    }
}
