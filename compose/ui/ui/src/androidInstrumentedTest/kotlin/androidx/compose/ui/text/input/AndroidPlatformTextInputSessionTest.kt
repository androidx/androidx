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

package androidx.compose.ui.text.input

import android.view.View
import androidx.compose.ui.platform.AndroidPlatformTextInputSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidPlatformTextInputSessionTest {

    private var startInputCalls = 0
    private var stopInputCalls = 0
    private var showKeyboardCalls = 0
    private var hideKeyboardCalls = 0

    private val view = View(InstrumentationRegistry.getInstrumentation().context)
    private val inputService = TextInputService(object : PlatformTextInputService {
        override fun startInput() {
            startInputCalls++
        }

        override fun stopInput() {
            stopInputCalls++
        }

        override fun showSoftwareKeyboard() {
            showKeyboardCalls++
        }

        override fun hideSoftwareKeyboard() {
            hideKeyboardCalls++
        }

        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            fail("Not supported")
        }

        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
            fail("not supported")
        }
    })

    @Test
    fun keyboardNotShown_whenStartInputMethodNotCalled() = runTest {
        AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
        assertThat(startInputCalls).isEqualTo(0)
        assertThat(stopInputCalls).isEqualTo(0)
    }

    @Test
    fun keyboardShown_whenStartInputMethodCalled() = runTest {
        val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)

        launch {
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(0)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardHidden_whenInnerSessionCanceled() = runTest {
        val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
        val sessionJob = launch {
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        sessionJob.cancel()
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)
    }

    @Test
    fun keyboardHidden_whenOuterSessionCanceled() = runTest {
        val sessionJob = launch {
            val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        sessionJob.cancel()
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)
    }

    @Test
    fun keyboardNotHidden_whenInnerSessionInterrupted() = runTest {
        val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
        launch {
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        launch {
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(2)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardNotHidden_whenOuterSessionInterrupted_innerSessionImmediatelyStarted() = runTest {
        val session1Job = launch {
            val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        // Interrupt outer session.
        launch {
            val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
            session1Job.cancelAndJoin()
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(2)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun keyboardHidden_whenOuterSessionInterrupted_innerSessionNotRestarted() = runTest {
        val session1Job = launch {
            val session = AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
            session.startInputMethod(TestInputMethodRequest(view))
        }
        advanceUntilIdle()

        // Interrupt outer session.
        launch {
            AndroidPlatformTextInputSession(view, inputService, coroutineScope = this)
            session1Job.cancelAndJoin()
        }
        advanceUntilIdle()

        assertThat(startInputCalls).isEqualTo(1)
        assertThat(stopInputCalls).isEqualTo(1)

        coroutineContext.job.cancelChildren()
    }
}
