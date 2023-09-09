/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal.selection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalFoundationApi::class
)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldSelectionOnBackTest {

    @get:Rule
    val rule = createComposeRule()

    private val Tag = "BasicTextField2"

    @Test
    fun whenBackPressed_andReleased_textFieldClearsSelection() {
        val state = TextFieldState("hello", TextRange(0, 0))
        rule.setContent {
            BasicTextField2(
                state,
                Modifier
                    .testTag(Tag)
                    .wrapContentSize()
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(0, 3))
        rule.waitForIdle()
        textNode.performKeyInput { pressKey(Key.Back) }
        val expected = TextRange(3, 3)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(expected)
        }
    }

    @Test
    fun whenBackPressed_andReleased_textFieldDoesNotPropagateBackPress() {
        val state = TextFieldState("hello", TextRange(0, 0))
        var backPressed = 0
        rule.setContent {
            BasicTextField2(
                state,
                Modifier
                    .testTag(Tag)
                    .wrapContentSize()
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyUp && it.key == Key.Back) {
                            backPressed++
                        }
                        false
                    }
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(TextRange(0, 3))
        rule.waitForIdle()
        textNode.performKeyInput { pressKey(Key.Back) }
        val expected = TextRange(3, 3)
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(expected)
            assertThat(backPressed).isEqualTo(0)
        }
    }

    @Test
    fun whenBackPressed_coreTextFieldRetainsSelection() {
        val state = TextFieldState("hello", TextRange(0, 0))
        rule.setContent {
            BasicTextField2(
                state,
                Modifier
                    .testTag(Tag)
                    .wrapContentSize()
            )
        }
        val expected = TextRange(0, 3)
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTextInputSelection(expected)
        rule.waitForIdle()
        // should have no effect
        textNode.performKeyInput { keyDown(Key.Back) }
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(expected)
        }
    }

    @Test
    fun whenBackPressed_andReleased_whenCursorHandleShown_doesNotConsumeEvent() {
        var backPressed = 0
        var softwareKeyboardController: SoftwareKeyboardController? = null
        val state = TextFieldState("Hello")
        rule.setContent {
            softwareKeyboardController = LocalSoftwareKeyboardController.current
            BasicTextField2(
                state,
                Modifier
                    .testTag(Tag)
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyUp && it.key == Key.Back) {
                            backPressed++
                        }
                        false
                    }
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            // Show the handle.
            performClick()
            rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()

            // Hide the keyboard before pressing back, since the first back should be consumed by
            // the keyboard.
            rule.runOnUiThread {
                softwareKeyboardController!!.hide()
            }

            // Press back.
            performKeyInput { pressKey(Key.Back) }

            // Ensure back event was propagated up past the text field.
            rule.runOnIdle {
                assertThat(backPressed).isEqualTo(1)
            }
        }
    }
}
