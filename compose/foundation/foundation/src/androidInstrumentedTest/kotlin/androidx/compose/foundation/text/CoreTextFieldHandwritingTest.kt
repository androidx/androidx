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

package androidx.compose.foundation.text

import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.foundation.text.input.internal.InputMethodManager
import androidx.compose.foundation.text.input.internal.inputMethodManagerFactory
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldHandwritingTest {
    @get:Rule val rule = createComposeRule()
    private val inputMethodInterceptor = InputMethodInterceptor(rule)
    private val keyboardHelper = KeyboardHelper(rule)

    private val Tag = "CoreTextField"

    private val fakeImm =
        object : InputMethodManager {
            private var stylusHandwritingStartCount = 0

            fun expectStylusHandwriting(started: Boolean) {
                if (started) {
                    assertThat(stylusHandwritingStartCount).isEqualTo(1)
                    stylusHandwritingStartCount = 0
                } else {
                    assertThat(stylusHandwritingStartCount).isZero()
                }
            }

            override fun isActive(): Boolean = true

            override fun restartInput() {}

            override fun showSoftInput() {}

            override fun hideSoftInput() {}

            override fun updateExtractedText(token: Int, extractedText: ExtractedText) {}

            override fun updateSelection(
                selectionStart: Int,
                selectionEnd: Int,
                compositionStart: Int,
                compositionEnd: Int
            ) {}

            override fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {}

            override fun startStylusHandwriting() {
                ++stylusHandwritingStartCount
            }
        }

    @Before
    fun setup() {
        // Test is only meaningful when stylusHandwriting is supported.
        assumeTrue(isStylusHandwritingSupported)
    }

    @Test
    fun coreTextField_startHandwriting_unfocused() {
        testStylusHandwriting(stylusHandwritingStarted = true) { performStylusHandwriting() }
    }

    @Test
    fun coreTextField_startStylusHandwriting_unfocused() {
        testStylusHandwriting(stylusHandwritingStarted = true) { performStylusHandwriting() }
    }

    @Test
    fun coreTextField_startStylusHandwriting_focused() {
        testStylusHandwriting(stylusHandwritingStarted = true) {
            requestFocus()
            performStylusHandwriting()
        }
    }

    @Test
    fun coreTextField_click_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) { performStylusClick() }
    }

    @Test
    fun coreTextField_longClick_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) { performStylusLongClick() }
    }

    @Test
    fun coreTextField_longPressAndDrag_notStartStylusHandwriting() {
        testStylusHandwriting(stylusHandwritingStarted = false) { performStylusLongPressAndDrag() }
    }

    @Test
    fun coreTextField_toggleEnabled_startStylusHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        var enabled by mutableStateOf(true)

        setContent {
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
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
    fun coreTextField_toggleReadOnly_startStylusHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        var readOnly by mutableStateOf(false)

        setContent {
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
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
    fun coreTextField_toggleWindowFocus_startStylusHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        val focusWindow = mutableStateOf(true)
        fun createWindowInfo(focused: Boolean) =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = focused
            }

        setContent {
            CompositionLocalProvider(LocalWindowInfo provides createWindowInfo(focusWindow.value)) {
                val value = remember { TextFieldValue() }
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.fillMaxSize().testTag(Tag),
                )
            }
        }

        performHandwritingAndExpect(stylusHandwritingStarted = true)

        focusWindow.value = false
        rule.waitForIdle()
        // only losing window focus does not stop the ongoing input session
        performHandwritingAndExpect(stylusHandwritingStarted = true)
    }

    @Test
    fun coreTextField_passwordField_notStartStylusHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        setContent {
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
                imeOptions = ImeOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxSize().testTag(Tag),
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = false)
    }

    @Test
    fun coreTextField_numberPasswordField_notStartStylusHandwriting() {
        inputMethodManagerFactory = { fakeImm }

        setContent {
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
                imeOptions = ImeOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxSize().testTag(Tag),
            )
        }

        performHandwritingAndExpect(stylusHandwritingStarted = false)
    }

    @Test
    fun coreTextField_passwordField_attemptStylusHandwritingShowSoftInput() {
        rule.setContent {
            keyboardHelper.initialize()
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
                imeOptions = ImeOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxSize().testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()
        keyboardHelper.waitForKeyboardVisibility(true)
        assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
    }

    @Test
    fun coreTextField_numberPasswordField_attemptStylusHandwritingShowSoftInput() {
        rule.setContent {
            keyboardHelper.initialize()
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
                imeOptions = ImeOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxSize().testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performStylusHandwriting()
        keyboardHelper.waitForKeyboardVisibility(true)
        assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
    }

    private fun testStylusHandwriting(
        stylusHandwritingStarted: Boolean,
        interaction: SemanticsNodeInteraction.() -> Unit
    ) {
        inputMethodManagerFactory = { fakeImm }

        setContent {
            val value = remember { TextFieldValue() }
            CoreTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxSize().testTag(Tag)
            )
        }

        interaction.invoke(rule.onNodeWithTag(Tag))
        rule.waitForIdle()
        fakeImm.expectStylusHandwriting(stylusHandwritingStarted)
    }

    private fun setContent(
        extraItemForInitialFocus: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setFocusableContent(extraItemForInitialFocus) {
            inputMethodInterceptor.Content { content() }
        }
    }

    private fun performHandwritingAndExpect(stylusHandwritingStarted: Boolean) {
        rule.onNodeWithTag(Tag).performStylusHandwriting()
        rule.waitForIdle()
        fakeImm.expectStylusHandwriting(stylusHandwritingStarted)
    }
}
