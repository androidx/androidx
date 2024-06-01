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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldSelectionOnBackTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private val Tag = "textField"
    private val backKeyDown =
        KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_BACK))
    private val backKeyUp =
        KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_UP, NativeKeyEvent.KEYCODE_BACK))

    @Test
    fun whenBackPressed_andReleased_coreTextFieldClearsSelection() {
        var textFieldValue by mutableStateOf(TextFieldValue("hello"))
        rule.setTextFieldTestContent {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        val textNode = rule.onNodeWithTag(Tag)
        val middleCharPosition = textNode.fetchTextLayoutResult().getBoundingBox(2).centerLeft

        textNode.performTouchInput { longClick(middleCharPosition) }
        rule.waitForIdle()
        assertThat(textFieldValue.selection).isEqualTo(TextRange(0, 5))

        textNode.performKeyPress(backKeyDown)
        rule.waitForIdle()
        assertThat(textFieldValue.selection).isEqualTo(TextRange(0, 5))

        textNode.performKeyPress(backKeyUp)
        rule.waitForIdle()
        assertThat(textFieldValue.selection).isEqualTo(TextRange(5, 5))
    }

    @Test
    fun whenBackPressed_andReleased_whenCursorHandleShown_doesNotConsumeEvent() {
        var backPressCount = 0
        var softwareKeyboardController: SoftwareKeyboardController? = null
        rule.setTextFieldTestContent {
            softwareKeyboardController = LocalSoftwareKeyboardController.current
            BasicTextField(
                "hello world",
                onValueChange = {},
                Modifier.testTag(Tag).onKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Back) {
                        backPressCount++
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
            rule.runOnUiThread { softwareKeyboardController!!.hide() }

            // Press back.
            performKeyPress(backKeyDown)
            performKeyPress(backKeyUp)

            // Ensure back event was propagated up past the text field.
            rule.runOnIdle { assertThat(backPressCount).isEqualTo(1) }
        }
    }
}
