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

package androidx.compose.foundation.text

import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.foundation.text.input.internal.InputMethodManager
import androidx.compose.foundation.text.input.internal.LegacyTextInputMethodRequest
import androidx.compose.foundation.text.input.internal.inputMethodManagerFactory
import androidx.compose.foundation.text.input.internal.update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldInputServiceIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    @Test
    fun textField_ImeOptions_isPassedTo_platformTextInputService() {
        val testTag = "KeyboardOption"
        val value = TextFieldValue("abc")
        val imeOptions = ImeOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        )

        setContent {
            CoreTextField(
                value = value,
                imeOptions = imeOptions,
                modifier = Modifier
                    .testTag(testTag),
                onValueChange = {}
            )
        }

        rule.onNodeWithTag(testTag).performClick()

        val expectedImeOptions = EditorInfo().apply { update("", TextRange.Zero, imeOptions) }
        inputMethodInterceptor.withEditorInfo {
            assertThat(this.imeOptions).isEqualTo(expectedImeOptions.imeOptions)
            assertThat(this.inputType).isEqualTo(expectedImeOptions.inputType)
            assertThat(this.privateImeOptions).isEqualTo(expectedImeOptions.privateImeOptions)
            assertThat(this.initialSelStart).isEqualTo(expectedImeOptions.initialSelStart)
            assertThat(this.initialSelEnd).isEqualTo(expectedImeOptions.initialSelEnd)
        }
    }

    @Test
    fun textField_stopsThenStartsInput_whenFocusMovesBetweenTextFields() {
        val value = TextFieldValue("abc")
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        setContent {
            Column {
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }
        rule.runOnIdle {
            focusRequester1.requestFocus()
        }

        // Focus the other field. The IME connection should restart only once.
        rule.runOnIdle {
            focusRequester2.requestFocus()
        }

        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(2)
    }

    @Test
    fun keyboardShownOnInitialClick() {
        // Arrange.
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }

        // Act.
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun keyboardShownOnInitialFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun keyboardHiddenWhenFocusIsLost() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        inputMethodInterceptor.assertSessionActive()

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun keyboardShownAfterDismissingKeyboardAndClickingAgain() {
        var keyboardShown = false
        val fakeKeyboardController = object : SoftwareKeyboardController {
            override fun show() {
                keyboardShown = true
            }

            override fun hide() {
                keyboardShown = false
            }
        }

        // Arrange.
        setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides fakeKeyboardController
            ) {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.testTag("TextField1")
                )
            }
        }
        rule.onNodeWithTag("TextField1").requestFocus()

        // Act.
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        rule.runOnIdle { assertThat(keyboardShown).isTrue() }
    }

    @Test
    fun keyboardStaysVisibleWhenMovingFromOneTextFieldToAnother() {
        // Arrange.
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        setContent {
            Column {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }
        rule.runOnIdle { focusRequester1.requestFocus() }
        inputMethodInterceptor.assertSessionActive()

        // Act.
        rule.runOnIdle { focusRequester2.requestFocus() }

        // Assert.
        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun keyboardHiddenWhenFieldRemovedFromComposition() {
        // Arrange.
        val focusRequester = FocusRequester()
        var composeField by mutableStateOf(true)
        setContent {
            if (composeField) {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        inputMethodInterceptor.assertSessionActive()

        // Act.
        composeField = false

        // Assert.
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun keyboardHiddenWhenFieldChangedToDisabled() {
        // Arrange.
        val focusRequester = FocusRequester()
        var enabled by mutableStateOf(true)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                enabled = enabled
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        inputMethodInterceptor.assertSessionActive()

        // Act.
        enabled = false

        // Assert.
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun keyboardHiddenWhenFieldChangedToReadOnly() {
        // Arrange.
        val focusRequester = FocusRequester()
        var readOnly by mutableStateOf(false)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                readOnly = readOnly
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        inputMethodInterceptor.assertSessionActive()

        // Act.
        readOnly = true

        // Assert.
        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun keyboardShownWhenFieldChangedToWritableWhileFocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        var readOnly by mutableStateOf(true)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                readOnly = readOnly
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        inputMethodInterceptor.assertNoSessionActive()

        // Act.
        readOnly = false

        // Assert.
        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun focusedRectIsPassedOnFocus() {
        val value = TextFieldValue("abc\nefg", TextRange(6))
        lateinit var textLayoutResult: TextLayoutResult
        val focusRequester = FocusRequester()

        setContent(extraItemForInitialFocus = false) {
            CoreTextField(
                value = value,
                modifier = Modifier.focusRequester(focusRequester),
                onValueChange = { },
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        assertFocusedRect(textLayoutResult.getBoundingBox(6).roundToIntRect())
    }

    @Test
    fun focusedRectIsPassedOnGlobalPositionChanged() {
        var offset by mutableStateOf(IntOffset(0, 10))
        val value = TextFieldValue("abc\nefg", TextRange(6))
        lateinit var textLayoutResult: TextLayoutResult
        val focusRequester = FocusRequester()

        setContent(extraItemForInitialFocus = false) {
            Box(Modifier.offset { offset }) {
                CoreTextField(
                    value = value,
                    modifier = Modifier.focusRequester(focusRequester),
                    onValueChange = { },
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        assertFocusedRect(
            textLayoutResult.getBoundingBox(6).translate(offset.toOffset()).roundToIntRect()
        )

        offset = IntOffset(10, 20)

        assertFocusedRect(
            textLayoutResult.getBoundingBox(6).translate(offset.toOffset()).roundToIntRect()
        )
    }

    @Test
    fun focusedRectIsPassedOnValueChange() {
        val tag = "TextField1"
        var value by mutableStateOf(TextFieldValue(""))
        lateinit var textLayoutResult: TextLayoutResult

        setContent(extraItemForInitialFocus = false) {
            CoreTextField(
                value = value,
                modifier = Modifier.testTag(tag),
                onValueChange = { value = it },
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.onNodeWithTag(tag).performClick()
        inputMethodInterceptor.withCurrentRequest<LegacyTextInputMethodRequest> {
            assertThat(focusedRect?.toComposeIntRect()?.topLeft).isEqualTo(IntOffset.Zero)
        }

        value = TextFieldValue("a", TextRange(1))
        rule.runOnIdle {
            assertFocusedRect(textLayoutResult.getBoundingBox(0).roundToIntRect())
        }

        value = TextFieldValue("a\nbc", TextRange(4))
        rule.runOnIdle {
            assertFocusedRect(textLayoutResult.getBoundingBox(3).roundToIntRect())
        }

        value = TextFieldValue("a\nbc", TextRange(3))
        rule.runOnIdle {
            assertFocusedRect(textLayoutResult.getBoundingBox(3).roundToIntRect())
        }

        value = TextFieldValue("a\nbc", TextRange(2))
        rule.runOnIdle {
            assertFocusedRect(textLayoutResult.getBoundingBox(2).roundToIntRect())
        }

        value = TextFieldValue("a\nbc", TextRange(0))
        rule.runOnIdle {
            assertFocusedRect(textLayoutResult.getBoundingBox(0).roundToIntRect())
        }
    }

    @Test
    fun cursorAnchorInfoIsUpdated_whenMonitoringAndGlobalOffsetChanges() {
        val cursorAnchorInfos = mutableListOf<CursorAnchorInfo>()
        val fakeInputMethodManager = object : InputMethodManager {
            override fun updateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo) {
                cursorAnchorInfos += cursorAnchorInfo
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
            ) {
            }
        }
        inputMethodManagerFactory = { fakeInputMethodManager }
        var offset by mutableStateOf(IntOffset(0, 10))
        val value = TextFieldValue("abc\nefg", TextRange(6))
        val focusRequester = FocusRequester()

        setContent(extraItemForInitialFocus = false) {
            Box(Modifier.offset { offset }) {
                CoreTextField(
                    value = value,
                    modifier = Modifier.focusRequester(focusRequester),
                    onValueChange = { },
                )
            }
        }
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        // Need to turn on monitoring to get notified.
        inputMethodInterceptor.withInputConnection {
            requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
        }

        rule.runOnIdle {
            assertThat(cursorAnchorInfos).isEmpty()
        }

        offset = IntOffset(10, 20)

        rule.runOnIdle {
            assertThat(cursorAnchorInfos).hasSize(1)
        }
    }

    private fun setContent(
        extraItemForInitialFocus: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setFocusableContent(extraItemForInitialFocus) {
            inputMethodInterceptor.Content {
                focusManager = LocalFocusManager.current
                content()
            }
        }
    }

    private fun assertFocusedRect(expected: IntRect?) {
        inputMethodInterceptor.withCurrentRequest<LegacyTextInputMethodRequest> {
            assertThat(focusedRect?.toComposeIntRect()).isEqualTo(expected)
        }
    }
}
