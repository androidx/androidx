/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.performStylusClick
import androidx.compose.foundation.text.performStylusHandwriting
import androidx.compose.foundation.text.performStylusLongClick
import androidx.compose.foundation.text.performStylusLongPressAndDrag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldHandwritingTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    private val imm = FakeInputMethodManager()

    private val keyboardHelper = KeyboardHelper(rule)

    @Before
    fun setup() {
        // Test is only meaningful when stylus handwriting is supported.
        assumeTrue(isStylusHandwritingSupported)
    }

    @Test
    fun textField_startStylusHandwriting_unfocused() {
        testStylusHandwriting(stylusHandwritingStarted = true) {
            performStylusHandwriting()
        }
    }

    @Test
    fun textField_startStylusHandwriting_focused() {
        testStylusHandwriting(stylusHandwritingStarted = true) {
            requestFocus()
            performStylusHandwriting()
        }
    }

    @Test
    fun textField_click_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusClick()
        }
    }

    @Test
    fun textField_longClick_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusLongClick()
        }
    }

    @Test
    fun textField_longPressAndDrag_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) {
            performStylusLongPressAndDrag()
        }
    }

    @Test
    fun textField_disabled_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                enabled = false
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun textField_readOnly_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                readOnly = true
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()

        rule.runOnIdle {
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun textField_toggleEnabled_startStylusHandwriting() {
        immRule.setFactory { imm }
        var enabled by mutableStateOf(true)
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                enabled = enabled
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = true)

        // Toggle enabled to false, shouldn't start handwriting
        enabled = false
        rule.waitForIdle()
        performHandwritingAndExpect(stylusHandwritingStarted = false)

        // Toggle to true again, should be able to start handwriting
        enabled = true
        rule.waitForIdle()
        performHandwritingAndExpect(stylusHandwritingStarted = true)
    }

    @Test
    fun textField_toggleReadOnly_startStylusHandwriting() {
        immRule.setFactory { imm }
        var readOnly by mutableStateOf(false)
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                readOnly = readOnly
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = true)

        // Toggle enabled to true, shouldn't start handwriting
        readOnly = true
        rule.waitForIdle()
        performHandwritingAndExpect(stylusHandwritingStarted = false)

        // Toggle to true again, should be able to start handwriting
        readOnly = false
        rule.waitForIdle()
        performHandwritingAndExpect(stylusHandwritingStarted = true)
    }

    @Test
    fun textField_passwordField_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = false)
    }

    @Test
    fun textField_numberPasswordField_notStartStylusHandwriting() {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = false)
    }

    @Test
    fun coreTextField_passwordField_attemptStylusHandwritingShowSoftInput() {
        rule.setContent {
            keyboardHelper.initialize()
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()
        keyboardHelper.waitForKeyboardVisibility(true)
        Truth.assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
    }

    @Test
    fun coreTextField_numberPasswordField_attemptStylusHandwritingShowSoftInput() {
        rule.setContent {
            keyboardHelper.initialize()
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()
        keyboardHelper.waitForKeyboardVisibility(true)
        Truth.assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
    }

    private fun testStylusHandwriting(
        stylusHandwritingStarted: Boolean,
        interaction: SemanticsNodeInteraction.() -> Unit
    ) {
        immRule.setFactory { imm }
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag)
            )
        }

        interaction.invoke(rule.onNodeWithTag(Tag))

        rule.runOnIdle {
            if (stylusHandwritingStarted) {
                imm.expectCall("startStylusHandwriting")
            }
            imm.expectNoMoreCalls()
        }
    }

    private fun performHandwritingAndExpect(stylusHandwritingStarted: Boolean) {
        rule.onNodeWithTag(Tag).performStylusHandwriting()
        rule.waitForIdle()
        rule.runOnIdle {
            if (stylusHandwritingStarted) {
                imm.expectCall("startStylusHandwriting")
            } else {
                imm.expectNoMoreCalls()
            }
        }
    }
}
