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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class BasicSecureTextFieldTest {

    // Keyboard shortcut tests for BasicSecureTextField are in TextFieldKeyEventTest

    @get:Rule val rule = createComposeRule().apply { mainClock.autoAdvance = false }

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicSecureTextField"
    private val imm = FakeInputMethodManager()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun passwordSemanticsAreSet() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = remember { TextFieldState("Hello", initialSelection = TextRange(0, 1)) },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.waitForIdle()
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))
    }

    @Test
    fun lastTypedCharacterIsRevealedTemporarily() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterAnotherCharacterIsTyped() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            performTextInput("b")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022b")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lastTypedCharacterIsRevealed_whenInsertedInMiddle() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            performTextInputSelection(TextRange(1))
            performTextInput("d")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022d\u2022\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterFocusIsLost() {
        inputMethodInterceptor.setContent {
            Column {
                BasicSecureTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag)
                )
                Box(modifier = Modifier.size(1.dp).testTag("otherFocusable").focusable())
            }
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.onNodeWithTag("otherFocusable").requestFocus()
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterAnotherCharacterRemoved() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            performTextInput("d")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022d")
            performTextReplacement("bcd")
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
        }
    }

    @Test
    fun obfuscationMethodVisible_doesNotHideAnything() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Visible,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("abc")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodVisible_revealsEverythingWhenSwitchedTo() {
        var obfuscationMode by mutableStateOf(TextObfuscationMode.Hidden)
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = obfuscationMode,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            obfuscationMode = TextObfuscationMode.Visible
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodHidden_hidesEverything() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Hidden,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            performTextInput("d")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022\u2022")
        }
    }

    @Test
    fun obfuscationMethodHidden_hidesEverythingWhenSwitchedTo() {
        var obfuscationMode by mutableStateOf(TextObfuscationMode.Visible)
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = obfuscationMode,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("abc")
            obfuscationMode = TextObfuscationMode.Hidden
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
        }
    }

    @Test
    fun obfuscationMethodHidden_usesCustomObfuscationCharacter() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Hidden,
                textObfuscationCharacter = '&',
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&&&")
            performTextInput("d")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&&&&")
        }
    }

    @Test
    fun obfuscationMethodRevealLastTyped_usesCustomObfuscationCharacter() {
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.RevealLastTyped,
                textObfuscationCharacter = '&',
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&")
        }
    }

    @Test
    fun obfuscationMethodHidden_toggleObfuscationCharacter() {
        var character by mutableStateOf('*')
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Hidden,
                textObfuscationCharacter = character,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("***")
            character = '&'
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&&&")
        }
    }

    @Test
    fun obfuscationMethodRevealLastTyped_toggleObfuscationCharacter() {
        var character by mutableStateOf('*')
        inputMethodInterceptor.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.RevealLastTyped,
                textObfuscationCharacter = character,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("*")

            character = '&'

            performTextInput("b")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&b")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("&&")
        }
    }

    @Test
    fun semantics_copy() {
        val state = TextFieldState("Hello World!")
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
    }

    @Test
    fun semantics_cut() {
        val state = TextFieldState("Hello World!")
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))
    }

    @Test
    fun toolbarDoesNotShowCopyOrCut() {
        var copyOptionAvailable = false
        var cutOptionAvailable = false
        var showMenuRequested = false
        val textToolbar =
            FakeTextToolbar(
                onShowMenu = { _, onCopyRequested, _, onCutRequested, _ ->
                    showMenuRequested = true
                    copyOptionAvailable = onCopyRequested != null
                    cutOptionAvailable = onCutRequested != null
                },
                onHideMenu = {}
            )
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                BasicSecureTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        rule.onNodeWithTag(Tag).requestFocus()
        // We need to disable the traversalMode to show the toolbar.
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.SetSelection) {
            it(0, 5, false)
        }

        rule.runOnIdle {
            assertThat(showMenuRequested).isTrue()
            assertThat(copyOptionAvailable).isFalse()
            assertThat(cutOptionAvailable).isFalse()
        }
    }

    @Test
    fun inputMethod_doesNotRestart_inResponseToKeyEvents() {
        val state = TextFieldState("hello", initialSelection = TextRange(5))
        inputMethodInterceptor.setContent {
            BasicSecureTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        with(rule.onNodeWithTag(Tag)) {
            requestFocus()
            imm.resetCalls()

            performKeyInput { pressKey(Key.Backspace) }
            performTextInputSelection(TextRange.Zero)
            performKeyInput { pressKey(Key.Delete) }
        }

        rule.runOnIdle {
            imm.expectCall("updateSelection(4, 4, -1, -1)")
            imm.expectCall("updateSelection(0, 0, -1, -1)")
            imm.expectNoMoreCalls()
        }
    }

    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifReadOnly() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicSecureTextField(
                state = state,
                readOnly = true,
                modifier = Modifier.fillMaxSize().testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun isNotEditable_whenDisabledOrReadOnly() {
        val state = TextFieldState()
        var enabled by mutableStateOf(true)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicSecureTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly
            )
        }
        rule.onNodeWithTag(Tag).assert(isEditable())

        enabled = true
        readOnly = true
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        enabled = false
        readOnly = false
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        enabled = false
        readOnly = true
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        // Make editable again.
        enabled = true
        readOnly = false
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(Tag).assert(isEditable())
    }
}
