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

package androidx.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.test.filters.MediumTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BaseTextField
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.ui.test.util.BoundaryNode
import androidx.ui.test.util.expectError
import androidx.ui.test.util.expectErrorMessageStartsWith
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class TextActionsTest {

    private val fieldTag = "Field"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    fun TextFieldUi(
        imeAction: ImeAction = ImeAction.Unspecified,
        onImeActionPerformed: (ImeAction) -> Unit = {},
        textCallback: (String) -> Unit = {}
    ) {
        val state = state { TextFieldValue("") }
        BaseTextField(
            modifier = Modifier.testTag(fieldTag),
            value = state.value,
            imeAction = imeAction,
            onImeActionPerformed = onImeActionPerformed,
            onValueChange = {
                state.value = it
                textCallback(it.text)
            }
        )
    }

    @Test
    fun sendText_clearText() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        onNodeWithTag(fieldTag)
            .performTextInput("Hello!")

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello!")
        }

        onNodeWithTag(fieldTag)
            .performTextClearance(alreadyHasFocus = true)

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("")
        }
    }

    @Test
    fun sendTextTwice_shouldAppend() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        onNodeWithTag(fieldTag)
            .performTextInput("Hello ")

        onNodeWithTag(fieldTag)
            .performTextInput("world!", alreadyHasFocus = true)

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    // @Test - not always appends, seems to be flaky
    fun sendTextTwice_shouldAppend_ver2() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        // This helps. So there must be some timing issue.
        // Thread.sleep(3000)

        onNodeWithTag(fieldTag)
            .performTextInput(" world!", alreadyHasFocus = true)

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    @Test
    fun sendText_noFocus_fail() {
        composeTestRule.setContent {
            TextFieldUi()
        }

        expectError<IllegalStateException> {
            onNodeWithTag(fieldTag)
                .performTextInput("Hello!", alreadyHasFocus = true)
        }
    }

    @Test
    fun replaceText() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        onNodeWithTag(fieldTag)
            .performTextInput("Hello")

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("Hello")
        }

        onNodeWithTag(fieldTag)
            .performTextReplacement("world", alreadyHasFocus = true)

        runOnIdle {
            assertThat(lastSeenText).isEqualTo("world")
        }
    }

    @Test
    fun sendImeAction_search() {
        var actionPerformed: ImeAction = ImeAction.Unspecified
        composeTestRule.setContent {
            TextFieldUi(imeAction = ImeAction.Search,
                onImeActionPerformed = { actionPerformed = it })
        }
        assertThat(actionPerformed).isEqualTo(ImeAction.Unspecified)

        onNodeWithTag(fieldTag)
            .performImeAction()

        runOnIdle {
            assertThat(actionPerformed).isEqualTo(ImeAction.Search)
        }
    }

    @Test
    fun sendImeAction_actionNotDefined_shouldFail() {
        var actionPerformed: ImeAction = ImeAction.Unspecified
        composeTestRule.setContent {
            TextFieldUi(imeAction = ImeAction.Unspecified,
                onImeActionPerformed = { actionPerformed = it })
        }
        assertThat(actionPerformed).isEqualTo(ImeAction.Unspecified)

        expectErrorMessageStartsWith("" +
                "Failed to perform IME action as current node does not specify any.\n" +
                "Semantics of the node:"
        ) {
            onNodeWithTag(fieldTag)
                .performImeAction()
        }
    }

    @Test
    fun sendImeAction_inputNotSupported_shouldFail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "node")
        }

        expectErrorMessageStartsWith("" +
                "Failed to perform IME action.\n" +
                "Failed to assert the following: (SupportsInputMethods is defined)\n" +
                "Semantics of the node:"
        ) {
            onNodeWithTag("node")
                .performImeAction()
        }
    }
}