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

package androidx.compose.foundation.text2

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.PlatformTextInputMethodTestOverride
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.awaitCancellation

/**
 * Helper class for testing integration of [BasicTextField2] with the platform IME.
 */
class InputMethodInterceptor(private val rule: ComposeContentTestRule) {

    private var currentRequest: PlatformTextInputMethodRequest? = null
    private val editorInfo = EditorInfo()
    private var inputConnection: InputConnection? = null

    fun assertSessionActive() {
        rule.runOnIdle {
            assertWithMessage("Expected a text input session to be active")
                .that(currentRequest).isNotNull()
        }
    }

    fun assertNoSessionActive() {
        rule.runOnIdle {
            assertWithMessage("Expected no text input session to be active")
                .that(currentRequest).isNull()
        }
    }

    fun withEditorInfo(block: EditorInfo.() -> Unit) {
        rule.runOnIdle {
            assertWithMessage("Expected a text input session to be active")
                .that(currentRequest).isNotNull()
            block(editorInfo)
        }
    }

    fun withInputConnection(block: InputConnection.() -> Unit) {
        rule.runOnIdle {
            val inputConnection = checkNotNull(inputConnection) {
                "Tried to read inputConnection while no session was active"
            }
            block(inputConnection)
        }
    }

    /**
     * Sets the content of the test, overriding the [PlatformTextInputSession] handler.
     */
    @OptIn(ExperimentalTestApi::class)
    fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            val view = LocalView.current
            val sessionHandler = remember { SessionHandler(view) }
            PlatformTextInputMethodTestOverride(
                sessionHandler = sessionHandler,
                content = content
            )
        }
    }

    private inner class SessionHandler(override val view: View) : PlatformTextInputSession {
        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            currentRequest = request
            try {
                inputConnection = request.createInputConnection(editorInfo)
                awaitCancellation()
            } finally {
                currentRequest = null
                inputConnection = null
            }
        }
    }
}
