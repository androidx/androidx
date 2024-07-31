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

import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.google.common.truth.IntegerSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import kotlin.test.assertNotNull
import kotlinx.coroutines.awaitCancellation

/**
 * Helper class for testing integration of BasicTextField and Legacy BasicTextField with the
 * platform IME.
 */
@OptIn(ExperimentalComposeUiApi::class)
class InputMethodInterceptor(private val rule: ComposeContentTestRule) {

    private var currentRequest: PlatformTextInputMethodRequest? = null
    private val editorInfo = EditorInfo()
    private var inputConnection: InputConnection? = null
    private val interceptor = PlatformTextInputInterceptor { request, _ ->
        currentRequest = request
        sessionCount++
        try {
            inputConnection = request.createInputConnection(editorInfo)
            // Don't forward the request, block it, so that tests don't have to deal with the actual
            // IME sending commands.
            awaitCancellation()
        } finally {
            currentRequest = null
            inputConnection = null
        }
    }

    /**
     * The total number of sessions that have been requested on this interceptor, including the
     * current one if active.
     */
    private var sessionCount = 0

    /**
     * Asserts that there is an active session.
     *
     * Can be called from any thread, including main and test runner.
     */
    fun assertSessionActive() {
        runOnIdle {
            assertWithMessage("Expected a text input session to be active")
                .that(currentRequest)
                .isNotNull()
        }
    }

    /**
     * Asserts that there is no active session.
     *
     * Can be called from any thread, including main and test runner.
     */
    fun assertNoSessionActive() {
        runOnIdle {
            assertWithMessage("Expected no text input session to be active")
                .that(currentRequest)
                .isNull()
        }
    }

    /**
     * Returns a subject that will assert on the total number of sessions requested on this
     * interceptor, including the current one if active.
     */
    fun assertThatSessionCount(): IntegerSubject = assertThat(runOnIdle { sessionCount })

    /**
     * Runs [block] on the main thread and passes it the [PlatformTextInputMethodRequest] for the
     * current input session.
     *
     * @throws AssertionError if no session is active.
     */
    inline fun <reified T : PlatformTextInputMethodRequest> withCurrentRequest(
        noinline block: T.() -> Unit
    ) {
        withCurrentRequest(T::class, block)
    }

    /**
     * Runs [block] on the main thread and passes it the [PlatformTextInputMethodRequest] for the
     * current input session.
     *
     * @throws AssertionError if no session is active.
     */
    fun <T : PlatformTextInputMethodRequest> withCurrentRequest(
        asClass: KClass<T>,
        block: T.() -> Unit
    ) {
        runOnIdle {
            val currentRequest =
                assertNotNull(currentRequest, "Expected a text input session to be active")
            assertThat(currentRequest).isInstanceOf(asClass.java)
            @Suppress("UNCHECKED_CAST") block(currentRequest as T)
        }
    }

    /**
     * Runs [block] on the main thread and passes it the [EditorInfo] configured by the current
     * input session.
     *
     * @throws AssertionError if no session is active.
     */
    fun withEditorInfo(block: EditorInfo.() -> Unit) {
        runOnIdle {
            assertWithMessage("Expected a text input session to be active")
                .that(currentRequest)
                .isNotNull()
            block(editorInfo)
        }
    }

    /**
     * Runs [block] on the main thread and passes it the [InputConnection] created by the current
     * input session.
     *
     * @throws AssertionError if no session is active.
     */
    fun withInputConnection(block: InputConnection.() -> Unit) {
        runOnIdle {
            val inputConnection =
                checkPreconditionNotNull(inputConnection) {
                    "Tried to read inputConnection while no session was active"
                }
            block(inputConnection)
        }
    }

    /**
     * Sets the content of the test, overriding the [PlatformTextInputSession] handler.
     *
     * This is just a convenience method for calling `rule.setContent` and then calling this class's
     * [Content] method yourself.
     */
    fun setContent(content: @Composable () -> Unit) {
        rule.setContent { Content(content) }
    }

    /**
     * Wraps the content of the test to override the [PlatformTextInputSession] handler.
     *
     * @see setContent
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Content(content: @Composable () -> Unit) {
        InterceptPlatformTextInput(interceptor, content)
    }

    private fun <T> runOnIdle(block: () -> T): T {
        return if (Looper.myLooper() != Looper.getMainLooper()) {
            rule.runOnIdle(block)
        } else {
            block()
        }
    }
}
