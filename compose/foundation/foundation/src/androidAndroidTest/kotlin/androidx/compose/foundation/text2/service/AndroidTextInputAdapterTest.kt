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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.service

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.TextFieldState
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidTextInputAdapterTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var adapter: AndroidTextInputAdapter

    @Before
    fun setup() {
        rule.setContent {
            val adapterProvider = LocalPlatformTextInputPluginRegistry.current
            adapter = adapterProvider.rememberAdapter(AndroidTextInputPlugin)
        }
    }

    @Test
    fun startInputSession_returnsOpenSession() {
        val state = TextFieldState()
        rule.runOnUiThread {
            val session = adapter.startInputSession(state, ImeOptions.Default)
            assertThat(session.isOpen).isTrue()
        }
    }

    @Test
    fun disposedSession_returnsClosed() {
        val state = TextFieldState()
        rule.runOnUiThread {
            val session = adapter.startInputSession(state, ImeOptions.Default)
            session.dispose()
            assertThat(session.isOpen).isFalse()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun startingInputSessionOnNonMainThread_throwsIllegalStateException() {
        adapter.startInputSession(TextFieldState(), ImeOptions.Default)
    }

    @Test
    fun creatingSecondInputSession_closesFirstOne() {
        val state = TextFieldState()
        rule.runOnUiThread {
            val session1 = adapter.startInputSession(state, ImeOptions.Default)
            val session2 = adapter.startInputSession(state, ImeOptions.Default)

            assertThat(session1.isOpen).isFalse()
            assertThat(session2.isOpen).isTrue()
        }
    }

    @Test
    fun createInputConnection_modifiesEditorInfo() {
        val state = TextFieldState(TextFieldValue("hello", selection = TextRange(0, 5)))
        rule.runOnUiThread {
            adapter.startInputSession(state, ImeOptions.Default)
            val editorInfo = EditorInfo()
            adapter.createInputConnection(editorInfo)

            assertThat(editorInfo.initialSelStart).isEqualTo(0)
            assertThat(editorInfo.initialSelEnd).isEqualTo(5)
            assertThat(editorInfo.inputType).isEqualTo(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            )
            assertThat(editorInfo.imeOptions).isEqualTo(
                    EditorInfo.IME_FLAG_NO_FULLSCREEN or
                    EditorInfo.IME_FLAG_NO_ENTER_ACTION
            )
        }
    }

    @Test
    fun inputConnection_sendsUpdates_toActiveSession() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        rule.runOnUiThread {
            adapter.startInputSession(state1, ImeOptions.Default)
            adapter.startInputSession(state2, ImeOptions.Default)

            val connection = adapter.createInputConnection(EditorInfo())

            connection.commitText("Hello", 0)

            assertThat(state1.value.text).isEqualTo("")
            assertThat(state2.value.text).isEqualTo("Hello")
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo() {
        val editorInfo = EditorInfo()
        rule.runOnUiThread {
            adapter.startInputSession(
                TextFieldState(),
                ImeOptions(
                    singleLine = true,
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false,
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                )
            )

            adapter.createInputConnection(editorInfo)

            assertThat(editorInfo.inputType).isEqualTo(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            )
            assertThat(editorInfo.imeOptions).isEqualTo(
                EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN
            )
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo_withTheLatestSession() {
        val editorInfo = EditorInfo()
        rule.runOnUiThread {
            adapter.startInputSession(
                TextFieldState(),
                ImeOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            adapter.startInputSession(
                TextFieldState(),
                ImeOptions(
                    singleLine = true,
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false,
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                )
            )

            adapter.createInputConnection(editorInfo)

            assertThat(editorInfo.inputType).isEqualTo(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            )
            assertThat(editorInfo.imeOptions).isEqualTo(
                EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN
            )
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo_noActiveSession() {
        val noActiveSessionEI = EditorInfo()
        val activeSessionEI = EditorInfo()
        val disposedSessionEI = EditorInfo()
        rule.runOnUiThread {
            adapter.createInputConnection(noActiveSessionEI)
            val session = adapter.startInputSession(
                TextFieldState(),
                ImeOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            adapter.createInputConnection(activeSessionEI)
            session.dispose()
            adapter.createInputConnection(disposedSessionEI)

            assertThat(noActiveSessionEI.inputType).isNotEqualTo(activeSessionEI.inputType)
            assertThat(noActiveSessionEI.imeOptions).isNotEqualTo(activeSessionEI.imeOptions)

            assertThat(noActiveSessionEI.inputType).isEqualTo(disposedSessionEI.inputType)
            assertThat(noActiveSessionEI.imeOptions).isEqualTo(disposedSessionEI.imeOptions)
        }
    }
}