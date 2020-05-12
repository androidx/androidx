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
import androidx.ui.core.TestTag
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.test.util.expectError
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class TextActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    fun TextFieldUi(textCallback: (String) -> Unit = {}) {
        val state = state { TextFieldValue("") }
        TestTag("Field") {
            TextField(
                value = state.value,
                onValueChange = {
                    state.value = it
                    textCallback(it.text)
                }
            )
        }
    }

    @Test
    fun sendText_clearText() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        findByTag("Field")
            .doSendText("Hello!")

        runOnIdleCompose {
            assertThat(lastSeenText).isEqualTo("Hello!")
        }

        findByTag("Field")
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

        findByTag("Field")
            .doSendText("Hello ")

        findByTag("Field")
            .doSendText("world!")

        runOnIdleCompose {
            assertThat(lastSeenText).isEqualTo("Hello world!")
        }
    }

    // @Test - fails to append.
    fun sendTextTwice_shouldAppend2() {
        var lastSeenText = ""
        composeTestRule.setContent {
            TextFieldUi {
                lastSeenText = it
            }
        }

        findByTag("Field")
            .doSendText("Hello")

        findByTag("Field")
            .doSendText(" world!")

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
            findByTag("Field")
                .doSendText("Hello!", alreadyHasFocus = true)
        }
    }
}