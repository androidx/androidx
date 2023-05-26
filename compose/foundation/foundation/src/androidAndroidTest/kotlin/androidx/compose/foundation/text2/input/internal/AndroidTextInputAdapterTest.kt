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

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalPlatformTextInputPluginRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidTextInputAdapterTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var adapter: AndroidTextInputAdapter

    private val keyboardHelper = KeyboardHelper(rule)

    private val focusRequester = FocusRequester()

    @Before
    fun setup() {
        rule.setContent {
            keyboardHelper.initialize()
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            Box(modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .focusable()
            ) {
                val adapterProvider = LocalPlatformTextInputPluginRegistry.current
                adapter = adapterProvider.rememberAdapter(AndroidTextInputPlugin)
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun startInputSession_returnsOpenSession() {
        rule.runOnUiThread {
            val session = adapter.startInputSessionWithDefaultsForTest()
            assertThat(session.isOpen).isTrue()
        }
    }

    @Test
    fun disposedSession_returnsClosed() {
        rule.runOnUiThread {
            val session = adapter.startInputSessionWithDefaultsForTest()
            session.dispose()
            assertThat(session.isOpen).isFalse()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun startingInputSessionOnNonMainThread_throwsIllegalStateException() {
        adapter.startInputSessionWithDefaultsForTest()
    }

    @Test
    fun creatingSecondInputSession_closesFirstOne() {
        rule.runOnUiThread {
            val session1 = adapter.startInputSessionWithDefaultsForTest()
            val session2 = adapter.startInputSessionWithDefaultsForTest()

            assertThat(session1.isOpen).isFalse()
            assertThat(session2.isOpen).isTrue()
        }
    }

    @Test
    fun createInputConnection_modifiesEditorInfo() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(0, 5))
        rule.runOnUiThread {
            adapter.startInputSessionWithDefaultsForTest(state)
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
            adapter.startInputSessionWithDefaultsForTest(state1)
            adapter.startInputSessionWithDefaultsForTest(state2)

            val connection = adapter.createInputConnection(EditorInfo())

            connection.commitText("Hello", 0)

            assertThat(state1.text.toString()).isEqualTo("")
            assertThat(state2.text.toString()).isEqualTo("Hello")
        }
    }

    @Test
    fun inputConnection_sendsEditorAction_toActiveSession() {
        var imeActionFromOne: ImeAction? = null
        var imeActionFromTwo: ImeAction? = null
        rule.runOnUiThread {
            adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(imeAction = ImeAction.Done),
                onImeAction = {
                    imeActionFromOne = it
                }
            )

            val connection = adapter.createInputConnection(EditorInfo())
            connection.performEditorAction(EditorInfo.IME_ACTION_DONE)

            assertThat(imeActionFromOne).isEqualTo(ImeAction.Done)
            assertThat(imeActionFromTwo).isNull()

            adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(imeAction = ImeAction.Go),
                onImeAction = {
                    imeActionFromTwo = it
                }
            )
            connection.performEditorAction(EditorInfo.IME_ACTION_GO)

            assertThat(imeActionFromOne).isEqualTo(ImeAction.Done)
            assertThat(imeActionFromTwo).isEqualTo(ImeAction.Go)
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo() {
        var editorInfo: EditorInfo? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        rule.runOnUiThread {
            adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(
                    singleLine = true,
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false,
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                )
            )
        }

        // wait until input gets started to make sure we are not running assertions before
        // the listener triggers.
        keyboardHelper.waitForKeyboardVisibility(true)

        rule.runOnIdle {
            assertThat(editorInfo?.inputType).isEqualTo(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            )
            assertThat(editorInfo?.imeOptions).isEqualTo(
                EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN
            )
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo_withTheLatestSession() {
        var editorInfo: EditorInfo? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        rule.runOnUiThread {
            adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(
                    singleLine = true,
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false,
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                )
            )
        }

        // wait until input gets started to make sure we are not running assertions before
        // the listener triggers.
        keyboardHelper.waitForKeyboardVisibility(true)

        rule.runOnIdle {
            assertThat(editorInfo?.inputType).isEqualTo(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            )
            assertThat(editorInfo?.imeOptions).isEqualTo(
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
            val session = adapter.startInputSessionWithDefaultsForTest(
                imeOptions = ImeOptions(
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

    @Test
    fun showSoftwareKeyboard_fromActiveInputSession_showsTheKeyboard() {
        var session: TextInputSession? = null

        rule.runOnUiThread {
            session = adapter.startInputSessionWithDefaultsForTest()
        }

        keyboardHelper.hideKeyboardIfShown()
        keyboardHelper.waitForKeyboardVisibility(false)

        session?.showSoftwareKeyboard()
        keyboardHelper.waitForKeyboardVisibility(true)
        assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun hideSoftwareKeyboard_fromActiveInputSession_hidesTheKeyboard() {
        var session: TextInputSession? = null

        rule.runOnUiThread {
            session = adapter.startInputSessionWithDefaultsForTest()
        }

        keyboardHelper.waitForKeyboardVisibility(true)

        session?.hideSoftwareKeyboard()
        keyboardHelper.waitForKeyboardVisibility(false)
        assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
    }

    @Test
    fun debugMode_isDisabled() {
        // run this in presubmit to check that we are not accidentally enabling logs on prod
        assertFalse(
            TIA_DEBUG,
            "Oops, looks like you accidentally enabled logging. Don't worry, we've all " +
                "been there. Just remember to turn it off before you deploy your code."
        )
    }

    private fun AndroidTextInputAdapter.startInputSessionWithDefaultsForTest(
        state: TextFieldState = TextFieldState(),
        imeOptions: ImeOptions = ImeOptions.Default,
        initialFilter: TextEditFilter? = null,
        onImeAction: (ImeAction) -> Unit = {}
    ) = startInputSession(state, imeOptions, initialFilter, onImeAction)
}