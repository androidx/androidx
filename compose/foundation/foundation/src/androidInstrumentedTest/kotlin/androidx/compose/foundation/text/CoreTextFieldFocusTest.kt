/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertTrue
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldFocusTest {
    @get:Rule
    val rule = createComposeRule()

    val keyboardHelper = KeyboardHelper(rule)

    @Test
    fun hideKeyboardWhenDisposed() {
        val value = TextFieldValue("initial text")
        var toggle by mutableStateOf(true)
        rule.setContent {
            keyboardHelper.initialize()

            if (toggle) {
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.testTag("TextField")
                )
            }
        }

        rule.onNodeWithTag("TextField").requestFocus()
        keyboardHelper.waitForKeyboardVisibility(true)
        assertTrue(keyboardHelper.isSoftwareKeyboardShown())

        toggle = false
        rule.waitForIdle()

        keyboardHelper.waitForKeyboardVisibility(false)
        Assert.assertFalse(keyboardHelper.isSoftwareKeyboardShown())
    }

    @Test
    fun hideKeyboardWhenFocusCleared() {
        val value = TextFieldValue("initial text")
        lateinit var focusManager: FocusManager
        rule.setContent {
            keyboardHelper.initialize()
            focusManager = LocalFocusManager.current
            Row {
                // Extra focusable that takes initial focus when focus is cleared.
                Box(Modifier.size(10.dp).focusable())
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.testTag("TextField")
                )
            }
        }

        rule.onNodeWithTag("TextField").requestFocus()
        keyboardHelper.waitForKeyboardVisibility(true)
        assertTrue(keyboardHelper.isSoftwareKeyboardShown())

        rule.runOnIdle {
            focusManager.clearFocus()
        }

        keyboardHelper.waitForKeyboardVisibility(false)
        Assert.assertFalse(keyboardHelper.isSoftwareKeyboardShown())
    }

    @Test
    fun tapToFocus() {
        // Arrange.
        val value = TextFieldValue("initial text")
        lateinit var recomposeScope: RecomposeScope
        rule.setContent {
            recomposeScope = currentRecomposeScope
            CoreTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.testTag("TextField")
            )
        }

        // Act.
        recomposeScope.invalidate()
        rule.onNodeWithTag("TextField").performClick()

        // Assert
        rule.onNodeWithTag("TextField").assertIsFocused()
    }

    @Test
    fun tapToFocus_whenReadOnly() {
        // Arrange.
        val value = TextFieldValue("initial text")
        rule.setContent {
            CoreTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.testTag("TextField"),
                readOnly = true
            )
        }

        // Act.
        rule.onNodeWithTag("TextField").performClick()

        // Assert
        rule.onNodeWithTag("TextField").assertIsFocused()
    }
}
