/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.performImeAction
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextActionsTest {

    private val fieldTag = "Field"

    @get:Rule
    val rule = createComposeRule()

    @Composable
    fun TextFieldUi(
        imeAction: ImeAction = ImeAction.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        textCallback: (String) -> Unit = {}
    ) {
        val state = remember { mutableStateOf("") }
        BasicTextField(
            modifier = Modifier
                .testTag(fieldTag)
                .border(0.dp, Color.Black),
            value = state.value,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = keyboardActions,
            onValueChange = {
                state.value = it
                textCallback(it)
            }
        )
    }

    @Test
    fun sendText_requestFocusNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(testTag = "node", Modifier.semantics {
                setText { true }
            })
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (RequestFocus is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag("node")
                .performTextInput("hello")
        }
    }

    @Test
    fun performTextInput_setTextNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(fieldTag, Modifier.semantics {
                insertTextAtCursor { true }
            })
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (SetText is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performTextInput("")
        }
    }

    @Test
    fun performTextInput_insertTextAtCursorNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(fieldTag, Modifier.semantics {
                setText { true }
                requestFocus { true }
            })
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (InsertTextAtCursor is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performTextInput("")
        }
    }

    @Test
    fun sendText_clearText() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello!")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello!")
        }

        rule.onNodeWithTag(fieldTag)
            .performTextClearance()

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("")
        }
    }

    @Test
    fun sendTextRepeatedly_shouldAppend() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        // "Type" one character at a time.
        " world!".forEach {
            rule.onNodeWithTag(fieldTag)
                .performTextInput(it.toString())
        }

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    @Test
    fun replaceText() {
        var lastSeenText = ""
        rule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        rule.onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello")
        }

        rule.onNodeWithTag(fieldTag)
            .performTextReplacement("world")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("world")
        }
    }

    @Test
    fun performImeAction_search() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Search,
                keyboardActions = KeyboardActions(onSearch = { actionPerformed = true })
            )
        }
        assertThat(actionPerformed).isFalse()

        rule.onNodeWithTag(fieldTag)
            .performImeAction()

        rule.runOnIdle {
            assertThat(actionPerformed).isTrue()
        }
    }

    @Test
    fun performImeAction_actionNotDefined_shouldFail() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Default,
                keyboardActions = KeyboardActions { actionPerformed = true }
            )
        }
        assertThat(actionPerformed).isFalse()

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (NOT (ImeAction = 'Default'))\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performImeAction()
        }
    }

    @Test
    fun performImeAction_actionReturnsFalse_shouldFail() {
        rule.setContent {
            BoundaryNode(fieldTag, Modifier.semantics {
                setText { true }
                requestFocus { true }
                insertTextAtCursor { true }
                performImeAction { false }
            })
        }

        expectErrorMessageStartsWith(
            "Failed to perform IME action, handler returned false.\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performImeAction()
        }
    }

    @Test
    fun performImeAction_inputNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(fieldTag)
        }

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (PerformImeAction is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag)
                .performImeAction()
        }
    }

    @Test
    fun performImeAction_focusNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(testTag = "node", Modifier.semantics {
                setText { true }
                performImeAction { true }
            })
        }

        expectErrorMessageStartsWith(
                "Failed to perform IME action.\n" +
                "Failed to assert the following: (RequestFocus is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag("node")
                .performImeAction()
        }
    }
}