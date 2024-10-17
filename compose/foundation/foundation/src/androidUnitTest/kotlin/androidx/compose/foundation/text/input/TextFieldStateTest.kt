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
import androidx.compose.foundation.text.input.internal.setComposingText
import androidx.compose.foundation.text.input.internal.withImeScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
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
                block = observeState
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
                block = observeState
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
                block = observeState
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
                assertThat(texts.isEmpty())
            }
            assertThat(texts.isEmpty())

            snapshot.apply()
            snapshot.dispose()

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
    fun notifyImeListener_firesAfterProgrammaticEdit() {
        val state = TextFieldState("Hello")
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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
    fun notifyImeListener_firesAfterUndoRedo() {
        val state = TextFieldState("Hello")
        state.editAsUser(null) { append(" World") }
        var oldValueCalled: TextFieldCharSequence? = null
        var newValueCalled: TextFieldCharSequence? = null
        var restartImeCalled: Boolean? = null
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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
        val listener =
            TextFieldState.NotifyImeListener { oldValue, newValue, restartIme ->
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

    private fun runTestWithSnapshotsThenCancelChildren(testBody: suspend TestScope.() -> Unit) {
        val globalWriteObserverHandle =
            Snapshot.registerGlobalWriteObserver {
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
