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

import androidx.compose.Composable
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.input.ImeAction
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
    val composeTestRule = createComposeRule().also {
        it.clockTestRule.pauseClock()
    }

    @Composable
    fun TextFieldUi(
        imeAction: ImeAction = ImeAction.Unspecified,
        onImeActionPerformed: (ImeAction) -> Unit = {},
        textCallback: (String) -> Unit = {}
    ) {
        val state = state { TextFieldValue("") }
        TextField(
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

        findByTag(fieldTag)
            .doSendText("Hello!")

        runOnIdleCompose {
            assertThat(lastSeenText).isEqualTo("Hello!")
        }

        findByTag(fieldTag)
            .doClearText(alreadyHasFocus = true)

        runOnIdleCompose {
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

        findByTag(fieldTag)
            .doSendText("Hello ")

        findByTag(fieldTag)
            .doSendText("world!", alreadyHasFocus = true)

        runOnIdleCompose {
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

        findByTag(fieldTag)
            .doSendText("Hello")

        // This helps. So there must be some timing issue.
        // Thread.sleep(3000)

        findByTag(fieldTag)
            .doSendText(" world!", alreadyHasFocus = true)

        runOnIdleCompose {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    @Test
    fun sendText_noFocus_fail() {
        composeTestRule.setContent {
            TextFieldUi()
        }

        expectError<IllegalStateException> {
            findByTag(fieldTag)
                .doSendText("Hello!", alreadyHasFocus = true)
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

        findByTag(fieldTag)
            .doSendText("Hello")

        runOnIdleCompose {
            assertThat(lastSeenText).isEqualTo("Hello")
        }

        findByTag(fieldTag)
            .doReplaceText("world", alreadyHasFocus = true)

        runOnIdleCompose {
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

        findByTag(fieldTag)
            .doSendImeAction()

        runOnIdleCompose {
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
                "Failed to send IME action as current node does not specify any.\n" +
                "Semantics of the node:"
        ) {
            findByTag(fieldTag)
                .doSendImeAction()
        }
    }

    @Test
    fun sendImeAction_inputNotSupported_shouldFail() {
        composeTestRule.setContent {
            BoundaryNode(testTag = "node")
        }

        expectErrorMessageStartsWith("" +
                "Failed to send IME action.\n" +
                "Failed to assert the following: (SupportsInputMethods = 'true')\n" +
                "Semantics of the node:"
        ) {
            findByTag("node")
                .doSendImeAction()
        }
    }
}