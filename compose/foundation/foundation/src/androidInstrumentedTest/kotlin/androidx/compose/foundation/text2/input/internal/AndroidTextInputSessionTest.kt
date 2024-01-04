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
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.textInputSession
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidTextInputSessionTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var hostView: View
    private lateinit var textInputNode: PlatformTextInputModifierNode

    @Before
    fun setup() {
        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            hostView = LocalView.current
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .testTag("tag")
                    .then(TestTextElement())
                    .focusable()
            )
        }
        rule.onNodeWithTag("tag").requestFocus()
        rule.waitForIdle()
    }

    @Test
    fun createInputConnection_modifiesEditorInfo() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(0, 5))
        launchInputSessionWithDefaultsForTest(state)
        val editorInfo = EditorInfo()

        rule.runOnUiThread {
            hostView.onCreateInputConnection(editorInfo)
        }

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

    @Test
    fun inputConnection_sendsUpdates_toActiveSession() {
        val state1 = TextFieldState()
        val state2 = TextFieldState()
        launchInputSessionWithDefaultsForTest(state1)

        rule.runOnIdle {
            hostView.onCreateInputConnection(EditorInfo())
                .commitText("hello", 1)

            assertThat(state1.text.toString()).isEqualTo("hello")
            assertThat(state2.text.toString()).isEqualTo("")
        }

        launchInputSessionWithDefaultsForTest(state2)

        rule.runOnIdle {
            hostView.onCreateInputConnection(EditorInfo())
                .commitText("world", 1)

            assertThat(state1.text.toString()).isEqualTo("hello")
            assertThat(state2.text.toString()).isEqualTo("world")
        }
    }

    @Test
    fun inputConnection_sendsEditorAction_toActiveSession() {
        var imeActionFromOne: ImeAction? = null
        var imeActionFromTwo: ImeAction? = null

        launchInputSessionWithDefaultsForTest(
            imeOptions = ImeOptions(imeAction = ImeAction.Done),
            onImeAction = { imeActionFromOne = it }
        )

        rule.runOnIdle {
            hostView.onCreateInputConnection(EditorInfo())
                .performEditorAction(EditorInfo.IME_ACTION_DONE)

            assertThat(imeActionFromOne).isEqualTo(ImeAction.Done)
            assertThat(imeActionFromTwo).isNull()
        }

        launchInputSessionWithDefaultsForTest(
            imeOptions = ImeOptions(imeAction = ImeAction.Go),
            onImeAction = { imeActionFromTwo = it }
        )

        rule.runOnIdle {
            hostView.onCreateInputConnection(EditorInfo())
                .performEditorAction(EditorInfo.IME_ACTION_GO)

            assertThat(imeActionFromOne).isEqualTo(ImeAction.Done)
            assertThat(imeActionFromTwo).isEqualTo(ImeAction.Go)
        }
    }

    @Test
    fun createInputConnection_updatesEditorInfo() {
        launchInputSessionWithDefaultsForTest(
            imeOptions = ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Email,
                autoCorrect = false,
                imeAction = ImeAction.Search,
                capitalization = KeyboardCapitalization.Words
            )
        )
        val editorInfo = EditorInfo()

        rule.runOnIdle {
            hostView.onCreateInputConnection(editorInfo)
        }

        assertThat(editorInfo.inputType).isEqualTo(
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )
        assertThat(editorInfo.imeOptions).isEqualTo(
            EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN
        )
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

    private fun launchInputSessionWithDefaultsForTest(
        state: TextFieldState = TextFieldState(),
        imeOptions: ImeOptions = ImeOptions.Default,
        onImeAction: (ImeAction) -> Unit = {}
    ) {
        coroutineScope.launch {
            textInputNode.textInputSession {
                inputSessionWithDefaultsForTest(
                    state,
                    imeOptions,
                    onImeAction
                )
            }
        }
    }

    private suspend fun PlatformTextInputSession.inputSessionWithDefaultsForTest(
        state: TextFieldState = TextFieldState(),
        imeOptions: ImeOptions = ImeOptions.Default,
        onImeAction: (ImeAction) -> Unit = {}
    ): Nothing = platformSpecificTextInputSession(
        state = TransformedTextFieldState(
            textFieldState = state,
            inputTransformation = null,
            codepointTransformation = null
        ),
        imeOptions = imeOptions,
        onImeAction = onImeAction
    )

    private inner class TestTextElement : ModifierNodeElement<TestTextNode>() {
        override fun create(): TestTextNode = TestTextNode()
        override fun update(node: TestTextNode) {}
        override fun hashCode(): Int = 0
        override fun equals(other: Any?): Boolean = other is TestTextElement
    }

    private inner class TestTextNode : Modifier.Node(), PlatformTextInputModifierNode {
        override fun onAttach() {
            textInputNode = this
        }
    }
}
