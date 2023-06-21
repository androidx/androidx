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

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedText
import com.google.common.truth.Truth
import java.util.concurrent.Executor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TextInputCommandExecutorTest {

    private val view = mock<View>()
    private val inputMethodManager = TestInputMethodManager()
    private val executor = Executor { runnable -> scope.launch { runnable.run() } }
    private val textInputCommandExecutor =
        TextInputCommandExecutor(
            view = view,
            inputMethodManager = inputMethodManager,
            inputCommandProcessorExecutor = executor
        )
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher + Job())

    @Before
    fun setUp() {
        // Default the view to focused because when it's not focused commands should be ignored.
        whenever(view.isFocused).thenReturn(true)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun showKeyboard_callsShowKeyboard() {
        showSoftwareKeyboard()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(1)
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun hideKeyboard_callsHideKeyboard() {
        hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(1)
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun startInput_callsRestartInput() {
        startInput()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1)
    }

    @Test
    fun startInput_callsShowKeyboard() {
        startInput()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun stopInput_callsRestartInput() {
        stopInput()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1)
    }

    @Test
    fun stopInput_callsHideKeyboard() {
        stopInput()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun startThenStopInput_onlyCallsRestartOnce() {
        startInput()
        stopInput()
        scope.advanceUntilIdle()

        // Both startInput and stopInput restart the IMM. So calling those two methods back-to-back,
        // in either order, should debounce to a single restart call. If they aren't de-duped, the
        // keyboard may flicker if one of the calls configures the IME in a non-default way (e.g.
        // number input).
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1)
    }

    @Test
    fun stopThenStartInput_onlyCallsRestartOnce() {
        stopInput()
        startInput()
        scope.advanceUntilIdle()

        // Both startInput and stopInput restart the IMM. So calling those two methods back-to-back,
        // in either order, should debounce to a single restart call. If they aren't de-duped, the
        // keyboard may flicker if one of the calls configures the IME in a non-default way (e.g.
        // number input).
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1)
    }

    @Test
    fun showKeyboard_afterStopInput_isIgnored() {
        stopInput()
        showSoftwareKeyboard()
        scope.advanceUntilIdle()

        // After stopInput, there's no input connection, so any calls to show the keyboard should
        // be ignored until the next call to startInput.
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun hideKeyboard_afterStopInput_isIgnored() {
        stopInput()
        hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        // stopInput will hide the keyboard implicitly, so both stopInput and hideSoftwareKeyboard
        // have the effect "hide the keyboard". These two effects should be debounced and the IMM
        // should only get a single hide call instead of two redundant calls.
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun multipleShowCallsAreDebounced() {
        repeat(10) {
            showSoftwareKeyboard()
        }
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun multipleHideCallsAreDebounced() {
        repeat(10) {
            hideSoftwareKeyboard()
        }
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun showThenHideAreDebounced() {
        showSoftwareKeyboard()
        hideSoftwareKeyboard()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(1)
    }

    @Test
    fun hideThenShowAreDebounced() {
        hideSoftwareKeyboard()
        showSoftwareKeyboard()
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(1)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun stopInput_isNotProcessedImmediately() {
        stopInput()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun startInput_isNotProcessedImmediately() {
        startInput()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun showSoftwareKeyboard_isNotProcessedImmediately() {
        showSoftwareKeyboard()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun hideSoftwareKeyboard_isNotProcessedImmediately() {
        hideSoftwareKeyboard()

        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
        Truth.assertThat(inputMethodManager.hideSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun commandsAreIgnored_ifFocusLostBeforeProcessing() {
        // Send command while view still has focus.
        showSoftwareKeyboard()
        // Blur the view.
        whenever(view.isFocused).thenReturn(false)
        // Process the queued commands.
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun commandsAreDrained_whenProcessedWithoutFocus() {
        whenever(view.isFocused).thenReturn(false)
        showSoftwareKeyboard()
        hideSoftwareKeyboard()
        scope.advanceUntilIdle()
        whenever(view.isFocused).thenReturn(true)
        scope.advanceUntilIdle()

        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(0)
    }

    @Test
    fun commandsAreCleared_afterProcessing() {
        startInput()
        scope.advanceUntilIdle()
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1)
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(1)

        showSoftwareKeyboard()
        scope.advanceUntilIdle()
        Truth.assertThat(inputMethodManager.restartCalls).isEqualTo(1) // does not increase
        Truth.assertThat(inputMethodManager.showSoftInputCalls).isEqualTo(2)
    }

    private fun startInput() {
        textInputCommandExecutor.send(TextInputCommand.StartInput)
    }

    private fun stopInput() {
        textInputCommandExecutor.send(TextInputCommand.StopInput)
    }

    private fun showSoftwareKeyboard() {
        textInputCommandExecutor.send(TextInputCommand.ShowKeyboard)
    }

    private fun hideSoftwareKeyboard() {
        textInputCommandExecutor.send(TextInputCommand.HideKeyboard)
    }

    private class TestInputMethodManager : ComposeInputMethodManager {
        var restartCalls = 0
        var showSoftInputCalls = 0
        var hideSoftInputCalls = 0

        override fun restartInput() {
            restartCalls++
        }

        override fun showSoftInput() {
            showSoftInputCalls++
        }

        override fun hideSoftInput() {
            hideSoftInputCalls++
        }

        override fun updateExtractedText(token: Int, extractedText: ExtractedText) {
        }

        override fun updateSelection(
            selectionStart: Int,
            selectionEnd: Int,
            compositionStart: Int,
            compositionEnd: Int
        ) {
        }

        override fun sendKeyEvent(event: KeyEvent) {
        }
    }
}