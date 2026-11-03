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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.internal.DefaultImeEditCommandScope
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.commitText
import androidx.compose.foundation.text.input.internal.setComposingText
import androidx.compose.foundation.text.input.internal.withImeScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class, ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class TextFieldStateTest {

    private val state = TextFieldState()

    @Test
    fun defaultInitialTextAndSelection() {
        val state = TextFieldState()
        assertThat(state.text.toString()).isEqualTo("")
        assertThat(state.selection).isEqualTo(TextRange.Zero)
    }

    @Test
    fun customInitialTextAndDefaultSelection() {
        val state = TextFieldState(initialText = "hello")
        assertThat(state.text.toString()).isEqualTo("hello")
        assertThat(state.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun customInitialTextAndSelection() {
        val state = TextFieldState(initialText = "hello", initialSelection = TextRange(0, 1))
        assertThat(state.text.toString()).isEqualTo("hello")
        assertThat(state.selection).isEqualTo(TextRange(0, 1))
    }

    @Test
    fun edit_doesNotAllow_reentrantBehavior() {
        assertFailsWith<IllegalStateException>(
            "TextFieldState does not support concurrent or nested editing."
        ) {
            state.edit {
                replace(0, 0, "hello")
                state.edit { replace(0, 0, "hello") }
            }
        }
        assertThat(state.text.toString()).isEmpty()
    }

    @Test
    fun edit_doesNotAllow_concurrentAccess() {
        assertFailsWith<IllegalStateException>(
            "TextFieldState does not support concurrent or nested editing."
        ) {
            runTest {
                var edit2Started = false
                launch {
                    state.edit {
                        replace(0, 0, "hello")
                        while (!edit2Started) delay(10)
                    }
                }
                launch {
                    state.edit {
                        edit2Started = true
                        replace(0, 0, "hello")
                    }
                }
                advanceUntilIdle()
                runCurrent()
            }
        }
        assertThat(state.text.toString()).isEmpty()
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
    fun edit_canEditAgain_ifFirstOneThrows() {
        class ExpectedException : RuntimeException()

        assertFailsWith<ExpectedException> {
            state.edit {
                replace(0, 0, "hello")
                throw ExpectedException()
            }
        }
        assertThat(state.text.toString()).isEmpty()

        state.edit { replace(0, 0, "hello") }
        assertThat(state.text.toString()).isEqualTo("hello")
    }

    @Test
    fun edit_invalidates_whenSelectionChanged() = runTestWithSnapshotsThenCancelChildren {
        val text = "hello"
        val state = TextFieldState(text, initialSelection = TextRange(0))
        var invalidationCount = 0
        val observer = SnapshotStateObserver(onChangedExecutor = { it() })
        val observeState: () -> Unit = { state.text }
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = {
                    invalidationCount++
                    observeState()
                },
                block = observeState,
            )
            assertThat(invalidationCount).isEqualTo(0)

            // Act.
            state.edit { selection = TextRange(0, length) }
            advanceUntilIdle()
            runCurrent()

            // Assert.
            assertThat(invalidationCount).isEqualTo(1)
        } finally {
            observer.stop()
        }
    }

    @Test
    fun edit_invalidates_whenTextChanged() = runTestWithSnapshotsThenCancelChildren {
        val text = "hello"
        val state = TextFieldState(text, initialSelection = TextRange(0))
        var invalidationCount = 0
        val observer = SnapshotStateObserver(onChangedExecutor = { it() })
        val observeState: () -> Unit = { state.text }
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = {
                    invalidationCount++
                    observeState()
                },
                block = observeState,
            )
            assertThat(invalidationCount).isEqualTo(0)

            // Act.
            state.edit { append("1") }
            advanceUntilIdle()
            runCurrent()

            // Assert.
            assertThat(invalidationCount).isEqualTo(1)
        } finally {
            observer.stop()
        }
    }

    @Test
    fun edit_doesNotInvalidate_whenNoChangesMade() = runTestWithSnapshotsThenCancelChildren {
        val text = "hello"
        val state = TextFieldState(text, initialSelection = TextRange(0))
        var invalidationCount = 0
        val observer = SnapshotStateObserver(onChangedExecutor = { it() })
        val observeState: () -> Unit = { state.text }
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = {
                    invalidationCount++
                    observeState()
                },
                block = observeState,
            )
            assertThat(invalidationCount).isEqualTo(0)

            // Act.
            state.edit {
                // Change the selection but restore it before returning.
                val originalSelection = selection
                selection = TextRange(0, length)
                selection = originalSelection

                // This will be a no-op too.
                setTextIfChanged(text)
            }
            advanceUntilIdle()
            runCurrent()

            // Assert.
            assertThat(invalidationCount).isEqualTo(0)
        } finally {
            observer.stop()
        }
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
        assertThat(state.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun edit_placeCursorBeforeChar_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            placeCursorBeforeCharAt(2)
        }
        assertThat(state.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun edit_placeCursorBeforeChar_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> { placeCursorBeforeCharAt(500) }
            assertFailsWith<IllegalArgumentException> { placeCursorBeforeCharAt(-1) }
            placeCursorAtEnd()
        }
    }

    @Test
    fun edit_selectAll() {
        state.edit {
            replace(0, 0, "hello")
            selectAll()
        }
        assertThat(state.selection).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun edit_selectChars_simpleCase() {
        state.edit {
            replace(0, 0, "hello")
            selection = TextRange(1, 4)
        }
        assertThat(state.selection).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun edit_selectChars_throws_whenInvalid() {
        state.edit {
            assertFailsWith<IllegalArgumentException> { selection = TextRange(500, 501) }
            assertFailsWith<IllegalArgumentException> { selection = TextRange(-1, 500) }
            assertFailsWith<IllegalArgumentException> { selection = TextRange(500, -1) }
            assertFailsWith<IllegalArgumentException> { selection = TextRange(-500, -1) }
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
    fun noOpEdit_doesNot_commitComposition() {
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))
        scope.setComposingText("Hello", 1)

        assertThat(state.composition).isEqualTo(TextRange(0, 5))

        state.edit { /*no-op*/ }
        assertThat(state.composition).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun editContentChange_commitsComposition() {
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))
        scope.setComposingText("Hello", 1)

        assertThat(state.composition).isEqualTo(TextRange(0, 5))

        state.edit { append(" world") }
        assertThat(state.composition).isNull()
    }

    @Test
    fun editSelectionChange_commitsComposition() {
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))
        scope.setComposingText("Hello", 1)

        assertThat(state.composition).isEqualTo(TextRange(0, 5))

        state.edit { selection = TextRange(3) }
        assertThat(state.composition).isNull()
    }

    @Test
    fun setTextAndPlaceCursorAtEnd_works() {
        state.setTextAndPlaceCursorAtEnd("Hello")
        assertThat(state.text.toString()).isEqualTo("Hello")
        assertThat(state.selection).isEqualTo(TextRange(5))
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

    @Test
    fun snapshotFlow_fires_immediately() = runTestWithSnapshotsThenCancelChildren {
        val state = TextFieldState("hello", initialSelection = TextRange(5))
        val texts = mutableListOf<TextFieldCharSequence>()

        launch(Dispatchers.Unconfined) {
            snapshotFlow { state.value }.collectLatest { texts += it }
        }

        assertThat(texts).hasSize(1)
        assertThat(texts.single()).isSameInstanceAs(state.value)
        assertThat(texts.single().toString()).isEqualTo("hello")
        assertThat(texts.single().selection).isEqualTo(TextRange(5))
    }

    @Test
    fun snapshotFlow_fires_whenTextChanged() = runTestWithSnapshotsThenCancelChildren {
        val state = TextFieldState(initialSelection = TextRange(0))
        val texts = mutableListOf<TextFieldCharSequence>()
        val initialSelection = state.selection

        launch(Dispatchers.Unconfined) {
            snapshotFlow { state.value }.collectLatest { texts += it }
        }

        state.edit {
            append("hello")
            placeCursorBeforeCharAt(0)
        }

        assertThat(texts).hasSize(2)
        assertThat(texts.last()).isSameInstanceAs(state.value)
        assertThat(texts.last().toString()).isEqualTo("hello")
        assertThat(texts.last().selection).isEqualTo(initialSelection)
    }

    @Test
    fun snapshotFlow_fires_whenSelectionChanged() = runTestWithSnapshotsThenCancelChildren {
        val state = TextFieldState("hello", initialSelection = TextRange(0))
        val texts = mutableListOf<TextFieldCharSequence>()

        launch(Dispatchers.Unconfined) {
            snapshotFlow { state.value }.collectLatest { texts += it }
        }

        state.edit { placeCursorAtEnd() }

        assertThat(texts).hasSize(2)
        assertThat(texts.last()).isSameInstanceAs(state.value)
        assertThat(texts.last().toString()).isEqualTo("hello")
        assertThat(texts.last().selection).isEqualTo(TextRange(5))
    }

    @Test
    fun snapshotFlow_firesTwice_whenEditCalledTwice() = runTestWithSnapshotsThenCancelChildren {
        val state = TextFieldState()
        val texts = mutableListOf<TextFieldCharSequence>()

        launch(Dispatchers.Unconfined) {
            snapshotFlow { state.value }.collectLatest { texts += it }
        }

        state.edit {
            append("hello")
            placeCursorAtEnd()
        }

        state.edit {
            append(" world")
            placeCursorAtEnd()
        }

        assertThat(texts).hasSize(3)
        assertThat(texts[1].toString()).isEqualTo("hello")
        assertThat(texts[2]).isSameInstanceAs(state.value)
        assertThat(texts[2].toString()).isEqualTo("hello world")
    }

    @Test
    fun snapshotFlow_firesOnce_whenMultipleChangesMadeInSingleEdit() =
        runTestWithSnapshotsThenCancelChildren {
            val state = TextFieldState()
            val texts = mutableListOf<TextFieldCharSequence>()

            launch(Dispatchers.Unconfined) {
                snapshotFlow { state.value }.collectLatest { texts += it }
            }

            state.edit {
                append("hello")
                append(" world")
                placeCursorAtEnd()
            }

            assertThat(texts.last()).isSameInstanceAs(state.value)
            assertThat(texts.last().toString()).isEqualTo("hello world")
        }

    @Test
    fun snapshotFlow_fires_whenChangeMadeInSnapshotIsApplied() =
        runTestWithSnapshotsThenCancelChildren {
            val state = TextFieldState()
            val texts = mutableListOf<TextFieldCharSequence>()

            launch(Dispatchers.Unconfined) {
                snapshotFlow { state.value }.collectLatest { texts += it }
            }

            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.enter {
                state.edit {
                    append("hello")
                    placeCursorAtEnd()
                }
            }

            snapshot.apply()
            snapshot.dispose()

            assertThat(texts).hasSize(2)
            assertThat(texts.last()).isSameInstanceAs(state.value)
        }

    @Test
    fun snapshotFlow_notFired_whenChangeMadeInSnapshotThenDisposed() =
        runTestWithSnapshotsThenCancelChildren {
            val state = TextFieldState()
            val texts = mutableListOf<TextFieldCharSequence>()

            launch(Dispatchers.Unconfined) {
                snapshotFlow { state.value }.collectLatest { texts += it }
            }

            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.enter {
                state.edit {
                    append("hello")
                    placeCursorAtEnd()
                }
            }
            snapshot.dispose()

            // Only contains initial value.
            assertThat(texts).hasSize(1)
            assertThat(texts.single().toString()).isEmpty()
        }

    @Test
    fun snapshotFlow_cancelsPreviousHandler_whenChangeMadeWhileSuspended() =
        runTestWithSnapshotsThenCancelChildren {
            val state = TextFieldState()
            val texts = mutableListOf<TextFieldCharSequence>()

            launch(Dispatchers.Unconfined) {
                snapshotFlow { state.value }
                    .collectLatest {
                        texts += it
                        awaitCancellation()
                    }
            }

            state.setTextAndPlaceCursorAtEnd("hello")
            state.setTextAndPlaceCursorAtEnd("world")

            assertThat(texts.map { it.toString() }).containsExactly("", "hello", "world").inOrder()
        }

    @Test
    fun snapshotFlowOfText_onlyFiresIfContentChanges() {
        runTestWithSnapshotsThenCancelChildren {
            val state = TextFieldState()
            val texts = mutableListOf<CharSequence>()

            launch(Dispatchers.Unconfined) { snapshotFlow { state.text }.collect { texts += it } }

            state.edit { append("a") }
            state.edit { append("b") }
            state.edit { placeCursorBeforeCharAt(0) }
            state.edit { placeCursorAtEnd() }
            state.edit { append("c") }

            assertThat(texts.map { it.toString() }).containsExactly("", "a", "ab", "abc").inOrder()
        }
    }

    @Test
    fun toString_doesNotReadSnapshotState() {
        val state = TextFieldState("hello")
        var isRead = false
        Snapshot.observe(readObserver = { isRead = true }) { state.toString() }

        assertThat(isRead).isFalse()
    }

    @Test
    fun isEditing_doesNotRegisterSnapshotRead() {
        val state = TextFieldState("hello")
        var isRead = false
        Snapshot.observe(
            readObserver = {
                // if read object is a Boolean, it might be `isEditing`.
                isRead = it is Boolean
            }
        ) {
            state.edit { append(" world") }
        }

        assertThat(isRead).isEqualTo(false)
    }

    @Test
    fun onlyHighlightChange_doesNotTriggerInputTransformation() {
        val state = TextFieldState("abc def ghi")
        var transformationCalled = 0
        val inputTransformation = InputTransformation { transformationCalled++ }
        state.editAsUser(inputTransformation) {
            setHighlight(TextHighlightType.HandwritingSelectPreview, 0, 3)
        }
        state.editAsUser(inputTransformation) {
            setHighlight(TextHighlightType.HandwritingDeletePreview, 4, 7)
        }
        assertThat(transformationCalled).isEqualTo(0)
    }

    @Test
    fun inputTransformationRejectsChanges_removesComposition() {
        val state = TextFieldState()
        val inputTransformation = InputTransformation { revertAllChanges() }
        state.withImeScope(inputTransformation) { setComposingText("hello", 1) }
        assertThat(state.text).isEqualTo("")
        assertThat(state.selection).isEqualTo(TextRange.Zero)
        assertThat(state.composition).isNull()
    }

    @Test
    fun programmaticEditAndCompositionDoesNotSetUserCommit() {
        val state = TextFieldState("hello")
        assertThat(state.composition).isNull()

        state.editAsUser(null) { setComposition(0, 5) }
        assertThat(state.composition).isEqualTo(TextRange(0, 5))

        state.edit { append(" World") }
        assertThat(state.userCommit).isFalse()
    }

    @Test
    fun userCommitTextAndCompositionSetUserCommit() {
        val state = TextFieldState("hello")
        assertThat(state.composition).isNull()

        state.editAsUser(null) { setComposition(0, 5) }
        assertThat(state.composition).isEqualTo(TextRange(0, 5))

        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText(" World", 6)
        assertThat(state.userCommit).isTrue()
    }

    @Test
    fun userComposingTextAndCompositionSetUserCommit() {
        assertThat(state.composition).isNull()
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).setComposingText("Hello", 1)
        assertThat(state.composition).isEqualTo(TextRange(0, 5))
        assertThat(state.userCommit).isTrue()
    }

    @Test
    fun notifyImeListener_firesAfterProgrammaticEdit() {
        val state = TextFieldState("Hello")
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        state.edit { append(" World") }

        assertThat(oldValueCalled.toString()).isEqualTo("Hello")
        assertThat(newValueCalled.toString()).isEqualTo("Hello World")
        assertThat(restartImeCalled).isFalse()
    }

    @Test
    fun notifyImeListener_firesAfterProgrammaticEdit_restartsImeIfComposing() {
        val state = TextFieldState("Hello")
        // We need a composing region to fire restartIme
        state.editAsUser(null) { setComposition(0, 5) }
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        state.edit { append(" World") }

        assertThat(oldValueCalled.toString()).isEqualTo("Hello")
        assertThat(newValueCalled.toString()).isEqualTo("Hello World")
        assertThat(restartImeCalled).isTrue()
    }

    @Test
    fun notifyImeListener_firesAfterProgrammaticEdit_doesNotRestartIfContentIsSame() {
        val state = TextFieldState("Hello")
        // We need a composing region to fire restartIme
        state.editAsUser(null) { setComposition(0, 5) }
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        state.edit {
            // this ends up being no-op
            append(" World")
            delete(5, length)
        }

        assertThat(oldValueCalled.toString()).isEqualTo("Hello")
        assertThat(newValueCalled.toString()).isEqualTo("Hello")
        assertThat(restartImeCalled).isFalse()
    }

    @Test
    fun notifyImeListener_firesAfterUserEdit() {
        val state = TextFieldState("Hello")
        // We need a composing region for restartIme to be true. It's not going to be but let's
        // cover all corners
        state.editAsUser(null) { setComposition(0, 5) }
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        DefaultImeEditCommandScope(TransformedTextFieldState(state)).setComposingText("World", 1)

        assertThat(oldValueCalled.toString()).isEqualTo("Hello")
        assertThat(newValueCalled.toString()).isEqualTo("World")
        // Even though content changes and there was a composing region, IME is not restarted
        assertThat(restartImeCalled).isFalse()
    }

    @Test
    fun noOpUserEdit_duringComposition_doesNotUpdateValueOrNotifyIme() {
        val state = TextFieldState("Hello ")
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).setComposingText("ab", 1)
        assertThat(state.composition).isEqualTo(TextRange(6, 8))
        val valueBefore = state.value
        var listenerCalled = false
        state.addNotifyImeListener { _, _, _ -> listenerCalled = true }

        state.editAsUser(null) { selection = TextRange(8) }

        assertThat(state.value).isSameInstanceAs(valueBefore)
        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun noOpUserEdit_duringStyledComposition_doesNotUpdateValueOrNotifyIme() {
        val state = TextFieldState("Hello ")
        val annotations =
            listOf<PlacedAnnotation>(
                AnnotatedString.Range(SpanStyle(background = Color.Yellow), 0, 2)
            )
        DefaultImeEditCommandScope(TransformedTextFieldState(state))
            .setComposingText("ab", 1, annotations)
        assertThat(state.value.composingAnnotations)
            .containsExactly(AnnotatedString.Range(SpanStyle(background = Color.Yellow), 6, 8))
        val valueBefore = state.value
        var listenerCalled = false
        state.addNotifyImeListener { _, _, _ -> listenerCalled = true }

        state.editAsUser(null) { selection = TextRange(8) }

        assertThat(state.value).isSameInstanceAs(valueBefore)
        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun userEdit_changingOnlyComposingAnnotations_updatesValue() {
        val state = TextFieldState("Hello ")
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).setComposingText("ab", 1)
        var listenerCalled = false
        state.addNotifyImeListener { _, _, _ -> listenerCalled = true }

        state.editAsUser(null) {
            setComposition(
                6,
                8,
                listOf(AnnotatedString.Range(SpanStyle(background = Color.Yellow), 0, 2)),
            )
        }

        assertThat(state.composition).isEqualTo(TextRange(6, 8))
        assertThat(state.value.composingAnnotations)
            .containsExactly(AnnotatedString.Range(SpanStyle(background = Color.Yellow), 6, 8))
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun noOpUserEdit_withoutComposition_doesNotNotifyIme() {
        val state = TextFieldState("Hello")
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText(" World", 1)
        assertThat(state.composition).isNull()
        assertThat(state.value.composingAnnotations).isEmpty()
        val valueBefore = state.value
        var listenerCalled = false
        state.addNotifyImeListener { _, _, _ -> listenerCalled = true }

        state.editAsUser(null) { selection = TextRange(11) }

        assertThat(state.value).isSameInstanceAs(valueBefore)
        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun noOpUserEdit_afterProgrammaticEdit_doesNotNotifyIme() {
        val state = TextFieldState("Hello")
        state.edit { replace(length, length, " World") }
        assertThat(state.value.composingAnnotations).isNull()
        var listenerCalled = false
        state.addNotifyImeListener { _, _, _ -> listenerCalled = true }

        state.editAsUser(null) { selection = TextRange(11) }

        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun notifyImeListener_firesAfterUndoRedo() {
        val state = TextFieldState("Hello")
        state.editAsUser(null) { append(" World") }
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        state.undoState.undo() // should remove " World"

        assertThat(oldValueCalled.toString()).isEqualTo("Hello World")
        assertThat(newValueCalled.toString()).isEqualTo("Hello")
        assertThat(restartImeCalled).isFalse()
    }

    @Test
    fun notifyImeListener_restartImeIsFalse_ifOnlySelectionIsChanged() {
        val state = TextFieldState("Hello", TextRange(3))
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener = TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
            oldValueCalled = oldValue
            newValueCalled = newValue
            restartImeCalled = restartIme
        }
        state.addNotifyImeListener(listener)

        state.editAsUser(null, restartImeIfContentChanges = true) { selection = TextRange.Zero }

        assertThat(oldValueCalled?.selection).isEqualTo(TextRange(3))
        assertThat(newValueCalled?.selection).isEqualTo(TextRange(0))
        assertThat(restartImeCalled).isFalse()
    }

    @Test
    fun textStyles_reflectsAddedSpanStyle() {
        val state = TextFieldState("hello")
        state.edit { addStyle(SpanStyle(color = Color.Red), 0, 5) }

        val spanStyles = state.textStyles.getSpanStyles(TextRange(0, 5))
        assertThat(spanStyles).hasSize(1)
        assertThat(spanStyles[0].item).isEqualTo(SpanStyle(color = Color.Red))
        assertThat(spanStyles[0].start).isEqualTo(0)
        assertThat(spanStyles[0].end).isEqualTo(5)
    }

    @Test
    fun textStyles_reflectsAddedParagraphStyle() {
        val state = TextFieldState("hello")
        state.edit { addStyle(ParagraphStyle(textAlign = TextAlign.Center), 0, 5) }

        val paragraphStyles = state.textStyles.getParagraphStyles(TextRange(0, 5))
        assertThat(paragraphStyles).hasSize(1)
        assertThat(paragraphStyles[0].item).isEqualTo(ParagraphStyle(textAlign = TextAlign.Center))
        assertThat(paragraphStyles[0].start).isEqualTo(0)
        assertThat(paragraphStyles[0].end).isEqualTo(5)
    }

    @Test
    fun textStyles_isObservable() = runTestWithSnapshotsThenCancelChildren {
        val state = TextFieldState("hello")
        var recordedStyles: TextFieldTextStyles? = null

        launch(Dispatchers.Unconfined) {
            snapshotFlow { state.textStyles }.collect { recordedStyles = it }
        }

        assertThat(recordedStyles).isNotNull()
        assertThat(recordedStyles?.getSpanStyles(TextRange(0, 5))).isEmpty()

        state.edit { addStyle(SpanStyle(color = Color.Blue), 0, 5) }

        assertThat(recordedStyles?.getSpanStyles(TextRange(0, 5))).hasSize(1)
        assertThat(recordedStyles?.getSpanStyles(TextRange(0, 5))?.get(0)?.item)
            .isEqualTo(SpanStyle(color = Color.Blue))
    }

    fun userCommitTextWithTextAttribute_textSuggestionSelected() {
        val state = TextFieldState("hello")

        DefaultImeEditCommandScope(TransformedTextFieldState(state))
            .commitText("Hello", 1, isTextSuggestionSelected = true)

        assertThat(state.userCommit).isTrue()
        assertThat(state.suggestionSelected).isTrue()

        DefaultImeEditCommandScope(TransformedTextFieldState(state))
            .commitText("world", 6, isTextSuggestionSelected = false)

        assertThat(state.userCommit).isTrue()
        assertThat(state.suggestionSelected).isFalse()
    }

    @Test
    fun userSetComposingTextWithTextAttribute_textSuggestionSelected() {
        assertThat(state.composition).isNull()

        DefaultImeEditCommandScope(TransformedTextFieldState(state))
            .setComposingText("Hello", 1, null, isTextSuggestionSelected = true)

        assertThat(state.composition).isEqualTo(TextRange(0, 5))
        assertThat(state.suggestionSelected).isTrue()
        assertThat(state.userCommit).isTrue()

        DefaultImeEditCommandScope(TransformedTextFieldState(state))
            .setComposingText("world", 1, null, isTextSuggestionSelected = false)

        assertThat(state.composition).isEqualTo(TextRange(0, 5))
        assertThat(state.userCommit).isTrue()
        assertThat(state.suggestionSelected).isFalse()
    }

    // region onTextChanged

    @Test
    fun onTextChanged_calledOnEdit_withNewText() {
        val calls = mutableListOf<String>()
        val state =
            TextFieldState("hello", TextRange(5), onTextChanged = { calls += it.toString() })

        state.edit { replace(length, length, " world") }

        assertThat(calls).containsExactly("hello world")
    }

    @Test
    fun onTextChanged_argumentIsSameInstanceAsText() {
        lateinit var state: TextFieldState
        var argument: CharSequence? = null
        state = TextFieldState(onTextChanged = { argument = it })

        state.edit { replace(length, length, "hello") }

        assertThat(argument).isSameInstanceAs(state.text)
    }

    @Test
    fun onTextChanged_notCalledOnSelectionOnlyEdit() {
        var calls = 0
        val state = TextFieldState("hello", TextRange(0), onTextChanged = { calls++ })

        state.edit { selection = TextRange(2, 4) }

        assertThat(calls).isEqualTo(0)
        assertThat(state.selection).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun onTextChanged_notCalledOnStyleOnlyEdit() {
        var calls = 0
        val state = TextFieldState("hello", onTextChanged = { calls++ })

        state.edit { addStyle(SpanStyle(color = Color.Red), 0, 5) }

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_notCalledWhenEditLeavesTextEqual() {
        var calls = 0
        val state = TextFieldState("hello", onTextChanged = { calls++ })

        state.edit {
            replace(length, length, " world")
            delete(5, length)
        }
        state.edit { replace(0, 5, "hello") }

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_notCalledOnCompositionOnlyChange() {
        var calls = 0
        val state = TextFieldState("hello", onTextChanged = { calls++ })

        state.editAsUser(null) { setComposition(0, 5) }
        state.editAsUser(null) { commitComposition() }

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_calledOnImeCommit() {
        val calls = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { calls += it.toString() })

        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText("hello", 1)

        assertThat(calls).containsExactly("hello")
    }

    @Test
    fun onTextChanged_calledOnEachComposingTextUpdate_thatChangesText() {
        val calls = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { calls += it.toString() })
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))

        scope.setComposingText("h", 1)
        scope.setComposingText("he", 1)
        // Same text, only the composing region is finalized.
        scope.commitText("he", 1)

        assertThat(calls).containsExactly("h", "he").inOrder()
    }

    @Test
    fun onTextChanged_calledOnUserSelectionChange_isNotCalled() {
        var calls = 0
        val state = TextFieldState("hello", TextRange(3), onTextChanged = { calls++ })

        state.editAsUser(null) { selection = TextRange.Zero }

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_calledOnUndoAndRedo() {
        val calls = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { calls += it.toString() })

        state.edit { replace(length, length, "a") }
        state.edit { replace(length, length, "b") }
        state.undoState.undo()
        state.undoState.redo()

        assertThat(calls).containsExactly("a", "ab", "a", "ab").inOrder()
    }

    @Test
    fun onTextChanged_calledWithInputTransformationResult() {
        val calls = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { calls += it.toString() })
        val uppercase = InputTransformation {
            replace(0, length, asCharSequence().toString().uppercase())
        }

        DefaultImeEditCommandScope(
                TransformedTextFieldState(state, inputTransformation = uppercase)
            )
            .commitText("hello", 1)

        assertThat(calls).containsExactly("HELLO")
    }

    @Test
    fun onTextChanged_notCalled_whenInputTransformationRevertsAllChanges() {
        var calls = 0
        val state = TextFieldState("hello", onTextChanged = { calls++ })
        val rejectAll = InputTransformation { revertAllChanges() }

        state.editAsUser(rejectAll) { replace(length, length, " world") }

        assertThat(state.text.toString()).isEqualTo("hello")
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_notCalled_whenInputTransformationOnlyMovesSelection() {
        var calls = 0
        val state = TextFieldState("hello", TextRange(5), onTextChanged = { calls++ })
        val moveCursor = InputTransformation { selection = TextRange(0) }

        state.editAsUser(moveCursor) { selection = TextRange(2) }

        assertThat(state.selection).isEqualTo(TextRange(0))
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun onTextChanged_calledOnConvenienceMethods() {
        val calls = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { calls += it.toString() })

        state.setTextAndPlaceCursorAtEnd("hello")
        state.setTextAndSelectAll("world")
        state.clearText()

        assertThat(calls).containsExactly("hello", "world", "").inOrder()
    }

    @Test
    fun onTextChanged_selectionIsAlreadyUpdated() {
        lateinit var state: TextFieldState
        var observedSelection: TextRange? = null
        state = TextFieldState(onTextChanged = { observedSelection = state.selection })

        state.edit {
            replace(length, length, "hello")
            selection = TextRange(1, 3)
        }

        assertThat(observedSelection).isEqualTo(TextRange(1, 3))
    }

    @Test
    fun onTextChanged_runsAfterImeListener() {
        val order = mutableListOf<String>()
        val state = TextFieldState(onTextChanged = { order += "onTextChanged" })
        state.addNotifyImeListener { _, _, _ -> order += "ime" }

        state.edit { replace(length, length, "hello") }

        assertThat(order).containsExactly("ime", "onTextChanged").inOrder()
    }

    @Test
    fun onTextChanged_undoStateIncludesCommit_whenCallbackRuns() {
        val canUndoInCallback = mutableListOf<Boolean>()
        lateinit var state: TextFieldState
        state = TextFieldState(onTextChanged = { canUndoInCallback += state.undoState.canUndo })

        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText("a", 1)
        state.undoState.clearHistory()
        state.edit { replace(length, length, "b") }

        assertThat(canUndoInCallback).containsExactly(true, true).inOrder()
    }

    @Test
    fun onTextChanged_editOnSameState_throws() {
        var editInCallback = true
        val calls = mutableListOf<String>()
        lateinit var state: TextFieldState
        state =
            TextFieldState(
                onTextChanged = {
                    calls += it.toString()
                    if (editInCallback) state.edit { replace(length, length, "!") }
                }
            )

        val error =
            assertFailsWith<IllegalStateException> { state.edit { replace(length, length, "a") } }

        assertThat(error)
            .hasMessageThat()
            .isEqualTo(
                "TextFieldState cannot be edited from its onTextChanged callback. Use an " +
                    "InputTransformation to intercept and modify user input."
            )
        // The commit that called onTextChanged stays committed.
        assertThat(state.text.toString()).isEqualTo("a")
        // The state is usable afterwards and onTextChanged runs again.
        editInCallback = false
        state.edit { replace(length, length, "b") }
        assertThat(state.text.toString()).isEqualTo("ab")
        assertThat(calls).containsExactly("a", "ab").inOrder()
    }

    @Test
    fun onTextChanged_editOnSameState_throws_fromUserInput() {
        var editInCallback = true
        val calls = mutableListOf<String>()
        lateinit var state: TextFieldState
        state =
            TextFieldState(
                onTextChanged = {
                    calls += it.toString()
                    if (editInCallback) state.edit { replace(length, length, "!") }
                }
            )
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))

        assertFailsWith<IllegalStateException> { scope.commitText("a", 1) }

        assertThat(state.text.toString()).isEqualTo("a")
        editInCallback = false
        // The IME scope that saw the exception keeps its pending command, so use a new scope.
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText("b", 1)
        state.edit { replace(length, length, "c") }
        assertThat(state.text.toString()).isEqualTo("abc")
        assertThat(calls).containsExactly("a", "ab", "abc").inOrder()
    }

    @Test
    fun onTextChanged_userInputOnSameState_throws() {
        lateinit var scope: DefaultImeEditCommandScope
        val state = TextFieldState(onTextChanged = { scope.commitText("!", 1) })
        scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))

        assertFailsWith<IllegalStateException> { state.edit { replace(length, length, "a") } }

        assertThat(state.text.toString()).isEqualTo("a")
    }

    @Test
    fun onTextChanged_editHelpersOnSameState_throw() {
        val helpers =
            listOf<TextFieldState.() -> Unit>(
                { setTextAndPlaceCursorAtEnd("x") },
                { setTextAndSelectAll("x") },
                { clearText() },
            )
        helpers.forEach { helper ->
            lateinit var state: TextFieldState
            state = TextFieldState(onTextChanged = { state.helper() })

            assertFailsWith<IllegalStateException> { state.edit { replace(length, length, "a") } }
            assertThat(state.text.toString()).isEqualTo("a")
        }
    }

    @Test
    fun onTextChanged_undoOnSameState_throws() {
        var undoInCallback = false
        lateinit var state: TextFieldState
        state = TextFieldState(onTextChanged = { if (undoInCallback) state.undoState.undo() })
        state.edit { replace(length, length, "a") }

        undoInCallback = true
        assertFailsWith<IllegalStateException> { state.edit { replace(length, length, "b") } }
        assertThat(state.text.toString()).isEqualTo("ab")

        // Undo history is not changed by the failed call.
        undoInCallback = false
        state.undoState.undo()
        assertThat(state.text.toString()).isEqualTo("a")
        state.undoState.undo()
        assertThat(state.text.toString()).isEmpty()
    }

    @Test
    fun onTextChanged_redoOnSameState_throws() {
        var redoInCallback = false
        lateinit var state: TextFieldState
        state = TextFieldState(onTextChanged = { if (redoInCallback) state.undoState.redo() })
        state.edit { replace(length, length, "a") }
        state.edit { replace(length, length, "b") }
        state.undoState.undo()

        redoInCallback = true
        assertFailsWith<IllegalStateException> { state.undoState.undo() }
        assertThat(state.text.toString()).isEmpty()

        redoInCallback = false
        state.undoState.redo()
        assertThat(state.text.toString()).isEqualTo("a")
    }

    @Test
    fun onTextChanged_allowsEditingAnotherState() {
        lateinit var first: TextFieldState
        lateinit var second: TextFieldState
        val secondCalls = mutableListOf<String>()
        first = TextFieldState(onTextChanged = { second.setTextAndPlaceCursorAtEnd(it.toString()) })
        second = TextFieldState(onTextChanged = { secondCalls += it.toString() })

        first.edit { replace(length, length, "hello") }

        assertThat(second.text.toString()).isEqualTo("hello")
        assertThat(secondCalls).containsExactly("hello")
    }

    @Test
    fun onTextChanged_statesThatWriteToEachOther_throwInsteadOfLooping() {
        lateinit var first: TextFieldState
        lateinit var second: TextFieldState
        first = TextFieldState(onTextChanged = { second.edit { replace(length, length, "1") } })
        second = TextFieldState(onTextChanged = { first.edit { replace(length, length, "2") } })

        val error =
            assertFailsWith<IllegalStateException> { first.edit { replace(length, length, "a") } }

        assertThat(error)
            .hasMessageThat()
            .isEqualTo(
                "TextFieldState cannot be edited from its onTextChanged callback. Use an " +
                    "InputTransformation to intercept and modify user input."
            )
        assertThat(first.text.toString()).isEqualTo("a")
        assertThat(second.text.toString()).isEqualTo("1")
        // Both states are usable afterwards.
        first.onTextChanged = null
        second.onTextChanged = null
        first.edit { replace(length, length, "b") }
        second.edit { replace(length, length, "c") }
        assertThat(first.text.toString()).isEqualTo("ab")
        assertThat(second.text.toString()).isEqualTo("1c")
    }

    @Test
    fun onTextChanged_stateIsUsable_afterCallbackThrows() {
        class ExpectedException : RuntimeException()
        var shouldThrow = true
        val state = TextFieldState(onTextChanged = { if (shouldThrow) throw ExpectedException() })

        assertFailsWith<ExpectedException> { state.edit { replace(length, length, "first") } }
        // The change is committed before the callback runs.
        assertThat(state.text.toString()).isEqualTo("first")

        shouldThrow = false
        state.edit { replace(length, length, "second") }
        assertThat(state.text.toString()).isEqualTo("firstsecond")
    }

    @Test
    fun onTextChanged_stateIsUsable_afterCallbackThrows_fromUserInput() {
        class ExpectedException : RuntimeException()
        var shouldThrow = true
        val state = TextFieldState(onTextChanged = { if (shouldThrow) throw ExpectedException() })
        val scope = DefaultImeEditCommandScope(TransformedTextFieldState(state))

        assertFailsWith<ExpectedException> { scope.commitText("first", 1) }
        assertThat(state.text.toString()).isEqualTo("first")

        shouldThrow = false
        // The IME scope that saw the exception keeps its pending command, so use a new scope.
        DefaultImeEditCommandScope(TransformedTextFieldState(state)).commitText("second", 1)
        assertThat(state.text.toString()).isEqualTo("firstsecond")
        state.undoState.undo()
        assertThat(state.text.toString()).isEmpty()
    }

    @Test
    fun onTextChanged_editInsideEditBlock_stillThrows() {
        lateinit var state: TextFieldState
        state = TextFieldState(onTextChanged = {})

        assertFailsWith<IllegalStateException> {
            state.edit { state.edit { replace(length, length, "x") } }
        }
    }

    @Test
    fun onTextChanged_canBeReassignedAndCleared() {
        val state = TextFieldState("abc")
        var calls = 0

        state.onTextChanged = { calls++ }
        state.edit { replace(length, length, "d") }
        assertThat(calls).isEqualTo(1)

        state.onTextChanged = null
        state.edit { replace(length, length, "e") }
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun onTextChanged_isSnapshotIsolated() {
        val calls = mutableListOf<String>()
        val state = TextFieldState("a", onTextChanged = { calls += "initial:$it" })

        // Uncommitted write in a disposed snapshot does not leak onto the global state.
        val discarded = Snapshot.takeMutableSnapshot()
        try {
            discarded.enter { state.onTextChanged = { calls += "discarded:$it" } }
            state.edit { replace(length, length, "b") }
            assertThat(calls).containsExactly("initial:ab")
        } finally {
            discarded.dispose()
        }

        state.edit { replace(length, length, "c") }
        assertThat(calls).containsExactly("initial:ab", "initial:abc").inOrder()

        // Committed snapshot write updates the callback for subsequent edits.
        val applied = Snapshot.takeMutableSnapshot()
        try {
            applied.enter { state.onTextChanged = { calls += "applied:$it" } }
            applied.apply().check()
        } finally {
            applied.dispose()
        }

        state.edit { replace(length, length, "d") }
        assertThat(calls).containsExactly("initial:ab", "initial:abc", "applied:abcd").inOrder()
    }

    @Test
    fun onTextChanged_editDoesNotRecordReadObservationOnCallback() {
        val state = TextFieldState("a", onTextChanged = {})
        val readObjects = mutableSetOf<Any>()

        Snapshot.observe(readObserver = { readObjects += it }) {
            state.edit { replace(length, length, "b") }
            state.editAsUser(null) { replace(length, length, "c") }
            state.undoState.undo()
        }

        val modifiedByCallbackReassign = mutableSetOf<Any>()
        val handle = Snapshot.registerApplyObserver { changed, _ ->
            modifiedByCallbackReassign.addAll(changed)
        }
        try {
            Snapshot.withMutableSnapshot { state.onTextChanged = {} }
        } finally {
            handle.dispose()
        }

        assertThat(modifiedByCallbackReassign).isNotEmpty()
        assertThat(readObjects.intersect(modifiedByCallbackReassign)).isEmpty()
    }

    @Test
    fun onTextChanged_longText_handlesAllEditPaths() {
        val longPrefix = "a".repeat(250)
        val calls = mutableListOf<Int>()
        val state =
            TextFieldState(
                initialText = longPrefix,
                initialSelection = TextRange(longPrefix.length),
                onTextChanged = { calls += it.length },
            )

        // Appending 1 character at the end changes length (250 -> 251).
        state.editAsUser(null) { replace(length, length, "b") }
        assertThat(calls).containsExactly(251)

        // Selection-only and composition-only user edits on long text do not call onTextChanged.
        state.editAsUser(null) { selection = TextRange(100) }
        state.editAsUser(null) { setComposition(0, 10) }
        state.editAsUser(null) { commitComposition() }
        assertThat(calls).containsExactly(251)

        // Single-char in-place replacement with identical char does not call onTextChanged.
        state.edit { replace(250, 251, "b") }
        state.editAsUser(null) { replace(250, 251, "b") }
        assertThat(calls).containsExactly(251)

        // Single-char in-place replacement with different char calls onTextChanged.
        state.editAsUser(null) { replace(250, 251, "c") }
        assertThat(calls).containsExactly(251, 251).inOrder()

        // Disjoint edits whose length deltas cancel out still detect text change vs no-op.
        state.edit {
            delete(0, 1)
            replace(length, length, "a")
        }
        // Deleting 'a' at 0 and appending 'a' after 'c' turns "aaa...ac" into "aaa...ca", which
        // differs at indices 249..250.
        assertThat(calls).hasSize(3)

        // Undo and redo on long text call onTextChanged.
        state.undoState.undo()
        state.undoState.redo()
        assertThat(calls).hasSize(5)

        // InputTransformation reverting user edit on long text does not call onTextChanged.
        state.editAsUser(InputTransformation { revertAllChanges() }) {
            replace(length, length, "z")
        }
        assertThat(calls).hasSize(5)

        // InputTransformation modifying text when user only moved selection calls onTextChanged.
        state.editAsUser(InputTransformation { replace(length, length, "!") }) {
            selection = TextRange(0)
        }
        assertThat(calls).hasSize(6)
        assertThat(calls.last()).isEqualTo(252)
    }

    @Test
    fun saver_withOnTextChanged_attachesCallbackToRestoredState() {
        val original = TextFieldState("hello", TextRange(2, 4))
        original.edit { replace(length, length, " world") }
        val calls = mutableListOf<String>()
        val saver = TextFieldState.Saver(onTextChanged = { calls += it.toString() })

        val saved = with(saver) { SaverScope { true }.save(original) }
        val restored = saver.restore(saved!!)!!

        assertThat(restored.text.toString()).isEqualTo("hello world")
        restored.edit { replace(length, length, "!") }
        assertThat(calls).containsExactly("hello world!")
        // Undo history is restored as well.
        restored.undoState.undo()
        restored.undoState.undo()
        assertThat(restored.text.toString()).isEqualTo("hello")
    }

    @Test
    fun saver_withoutOnTextChanged_restoresStateWithoutCallback() {
        var calls = 0
        val original = TextFieldState("hello", onTextChanged = { calls++ })

        val saved = with(TextFieldState.Saver) { SaverScope { true }.save(original) }
        val restored = TextFieldState.Saver.restore(saved!!)!!

        restored.edit { replace(length, length, "!") }
        assertThat(calls).isEqualTo(0)
        assertThat(restored.onTextChanged).isNull()
    }

    @Test
    fun saver_withNullOnTextChanged_savesSameFormatAsSaver() {
        val original = TextFieldState("hello", TextRange(1, 2))

        val saved = with(TextFieldState.Saver) { SaverScope { true }.save(original) }
        val savedWithNull = with(TextFieldState.Saver(null)) { SaverScope { true }.save(original) }

        assertThat(savedWithNull).isEqualTo(saved)
    }

    // endregion

    private fun runTestWithSnapshotsThenCancelChildren(testBody: suspend TestScope.() -> Unit) {
        val globalWriteObserverHandle = Snapshot.registerGlobalWriteObserver {
            // This is normally done by the compose runtime.
            Snapshot.sendApplyNotifications()
        }
        try {
            runTest {
                testBody()
                coroutineContext.job.cancelChildren()
            }
        } finally {
            globalWriteObserverHandle.dispose()
        }
    }
}
