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

import android.os.Build
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.text2.input.internal.setInputConnectionInterceptorForTests
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.job
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to assist with intercepting calls to [View.onCreateInputConnection].
 *
 * After adding this rule to your test, assert about the state of the input session with
 * [assertSessionActive] and [assertNoSessionActive], and access the properties of the current
 * session on the UI thread via [withInputConnection] and [withEditorInfo].
 */
class InputMethodInterceptorRule(
    private val composeRule: ComposeTestRule
) : TestRule {

    private var latestEditorInfo: EditorInfo? = null
    private var latestInputConnection: InputConnection? = null

    /**
     * Keeps track of all input connections reported via [latestInputConnection] so we can close
     * them manually when the test is done, since they won't be returned to the system.
     */
    private val stolenInputConnections = mutableVectorOf<InputConnection>()

    /**
     * Asserts that there is an active input connection, i.e. that a
     * [PlatformTextInputSession.startInputMethod] is currently suspended.
     */
    fun assertSessionActive() {
        composeRule.runOnIdle {
            assertWithMessage("Expected a text input session to be active")
                .that(latestInputConnection).isNotNull()
        }
    }

    /**
     * Asserts that there is no active input connection, i.e. that no
     * [PlatformTextInputSession.startInputMethod] is currently suspended.
     */
    fun assertNoSessionActive() {
        composeRule.runOnIdle {
            assertWithMessage("Expected no text input session to be active")
                .that(latestInputConnection).isNull()
        }
    }

    /**
     * Waits for Compose to be idle, then asserts that there is an active text input session and
     * runs [block] on the UI thread with the session's [EditorInfo].
     */
    fun withEditorInfo(block: EditorInfo.() -> Unit) {
        composeRule.runOnIdle {
            val editorInfo = checkNotNull(latestEditorInfo) {
                "Tried to read latestEditorInfo before onCreateInputConnection was called"
            }
            block(editorInfo)
        }
    }

    /**
     * Waits for Compose to be idle, then asserts that there is an active text input session and
     * runs [block] on the UI thread with the session's [InputConnection].
     */
    fun withInputConnection(block: InputConnection.() -> Unit) {
        composeRule.runOnIdle {
            val inputConnection = checkNotNull(latestInputConnection) {
                "Tried to read latestInputConnection before onCreateInputConnection was called"
            }
            block(inputConnection)
        }
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                setInputConnectionInterceptorForTests { editorInfo, inputConnection, view ->
                    latestEditorInfo = editorInfo
                    latestInputConnection = inputConnection
                    stolenInputConnections += inputConnection

                    coroutineContext.job.invokeOnCompletion {
                        latestEditorInfo = null
                        latestInputConnection = null
                    }

                    // Give the system a no-op input connection so it doesn't make any calls on the
                    // connection under test.
                    BaseInputConnection(view, false)
                }

                try {
                    base.evaluate()
                } finally {
                    setInputConnectionInterceptorForTests(null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stolenInputConnections.forEach {
                            it.closeConnection()
                        }
                    }
                    stolenInputConnections.clear()
                }
            }
        }
}
