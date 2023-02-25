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

package androidx.compose.foundation.text2.service

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.text2.TextFieldState
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
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
    @OptIn(ExperimentalTextApi::class)
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
            val session = adapter.startInputSession(state)
            assertThat(session.isOpen).isTrue()
        }
    }

    @Test
    fun disposedSession_returnsClosed() {
        val state = TextFieldState()
        rule.runOnUiThread {
            val session = adapter.startInputSession(state)
            session.dispose()
            assertThat(session.isOpen).isFalse()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun startingInputSessionOnNonMainThread_throwsIllegalStateException() {
        adapter.startInputSession(TextFieldState())
    }

    @Test
    fun creatingSecondInputSession_closesFirstOne() {
        val state = TextFieldState()
        rule.runOnUiThread {
            val session1 = adapter.startInputSession(state)
            val session2 = adapter.startInputSession(state)

            assertThat(session1.isOpen).isFalse()
            assertThat(session2.isOpen).isTrue()
        }
    }

    // TODO: split into multiple tests when ImeOptions are added
    @Test
    fun createInputConnection_modifiesEditorInfo() {
        val state = TextFieldState(TextFieldValue("hello", selection = TextRange(0, 5)))
        rule.runOnUiThread {
            adapter.startInputSession(state)
            val editorInfo = EditorInfo()
            adapter.createInputConnection(editorInfo)

            assertThat(editorInfo.initialSelStart).isEqualTo(0)
            assertThat(editorInfo.initialSelEnd).isEqualTo(5)
            assertThat(editorInfo.inputType).isEqualTo(InputType.TYPE_CLASS_TEXT)
            assertThat(editorInfo.imeOptions).isEqualTo(
                EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN
            )
        }
    }

    @Test
    fun inputConnection_sendsUpdates_toActiveSession() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        rule.runOnUiThread {
            adapter.startInputSession(state1)
            adapter.startInputSession(state2)

            val connection = adapter.createInputConnection(EditorInfo())

            connection.commitText("Hello", 0)

            assertThat(state1.value.text).isEqualTo("")
            assertThat(state2.value.text).isEqualTo("Hello")
        }
    }
}