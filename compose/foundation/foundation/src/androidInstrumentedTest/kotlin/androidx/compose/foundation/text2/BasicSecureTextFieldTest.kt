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

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.FakeTextToolbar
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextObfuscationMode
import androidx.compose.foundation.text2.input.internal.selection.FakeClipboardManager
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class BasicSecureTextFieldTest {

    // Keyboard shortcut tests for BasicSecureTextField are in TextFieldKeyEventTest

    @get:Rule
    val rule = createComposeRule().apply {
        mainClock.autoAdvance = false
    }

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    @get:Rule
    val inputMethodInterceptor = InputMethodInterceptorRule(rule)

    private val Tag = "BasicSecureTextField"
    private val imm = FakeInputMethodManager()

    @Before
    fun setUp() {
        immRule.setFactory { imm }
    }

    @Test
    fun passwordSemanticsAreSet() {
        rule.setContent {
            BasicSecureTextField(
                state = remember {
                    TextFieldState("Hello", initialSelectionInChars = TextRange(0, 1))
                },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.waitForIdle()
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))
        // temporarily define copy and cut actions on BasicSecureTextField but make them no-op
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CopyText))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CutText))
    }

    @Test
    fun lastTypedCharacterIsRevealedTemporarily() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
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
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
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
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
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
        rule.setContent {
            Column {
                BasicSecureTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag)
                )
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .testTag("otherFocusable")
                        .focusable()
                )
            }
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.onNodeWithTag("otherFocusable")
                .requestFocus()
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterAnotherCharacterRemoved() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
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
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Visible,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodVisible_revealsEverythingWhenSwitchedTo() {
        var obfuscationMode by mutableStateOf(TextObfuscationMode.Hidden)
        rule.setContent {
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
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodHidden_hidesEverything() {
        rule.setContent {
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
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = obfuscationMode,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
            obfuscationMode = TextObfuscationMode.Hidden
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_copy() {
        val state = TextFieldState("Hello World!")
        val clipboardManager = FakeClipboardManager("initial")
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicSecureTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 5))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText)

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("initial")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun semantics_cut() {
        val state = TextFieldState("Hello World!")
        val clipboardManager = FakeClipboardManager("initial")
        rule.setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicSecureTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 5))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText)

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.toString()).isEqualTo("initial")
            assertThat(state.text.toString()).isEqualTo("Hello World!")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolbarDoesNotShowCopyOrCut() {
        var copyOptionAvailable = false
        var cutOptionAvailable = false
        var showMenuRequested = false
        val textToolbar = FakeTextToolbar(
            onShowMenu = { _, onCopyRequested, _, onCutRequested, _ ->
                showMenuRequested = true
                copyOptionAvailable = onCopyRequested != null
                cutOptionAvailable = onCutRequested != null
            },
            onHideMenu = {}
        )
        val state = TextFieldState("Hello")
        rule.setContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                BasicSecureTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 5))

        rule.runOnIdle {
            assertThat(showMenuRequested).isTrue()
            assertThat(copyOptionAvailable).isFalse()
            assertThat(cutOptionAvailable).isFalse()
        }
    }

    @Test
    fun stringValue_updatesFieldText_whenTextChangedFromCode_whileUnfocused() {
        var text by mutableStateOf("hello")
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.runOnIdle {
            text = "world"
        }
        // Auto-advance is disabled.
        rule.mainClock.advanceTimeByFrame()

        assertThat(
            rule.onNodeWithTag(Tag).fetchSemanticsNode().config[SemanticsProperties.EditableText]
                .text
        ).isEqualTo("world")
    }

    @Test
    fun textFieldValue_updatesFieldText_whenTextChangedFromCode_whileUnfocused() {
        var text by mutableStateOf(TextFieldValue("hello"))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.runOnIdle {
            text = text.copy(text = "world")
        }
        // Auto-advance is disabled.
        rule.mainClock.advanceTimeByFrame()

        assertThat(
            rule.onNodeWithTag(Tag).fetchSemanticsNode().config[SemanticsProperties.EditableText]
                .text
        ).isEqualTo("world")
    }

    @Test
    fun textFieldValue_updatesFieldSelection_whenSelectionChangedFromCode_whileUnfocused() {
        var text by mutableStateOf(TextFieldValue("hello", selection = TextRange(1)))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.runOnIdle {
            text = text.copy(selection = TextRange(2))
        }
        // Auto-advance is disabled.
        rule.mainClock.advanceTimeByFrame()

        assertTextSelection(TextRange(2))
    }

    @Test
    fun stringValue_doesNotUpdateField_whenTextChangedFromCode_whileFocused() {
        var text by mutableStateOf("hello")
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            text = "world"
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
    }

    @Test
    fun textFieldValue_doesNotUpdateField_whenTextChangedFromCode_whileFocused() {
        var text by mutableStateOf(TextFieldValue("hello", selection = TextRange(1)))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            text = TextFieldValue(text = "world", selection = TextRange(2))
        }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
    }

    @Test
    fun stringValue_doesNotInvokeCallback_onFocus() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChangedCount++
                },
                modifier = Modifier.testTag(Tag)
            )
        }
        assertThat(onValueChangedCount).isEqualTo(0)

        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(onValueChangedCount).isEqualTo(0)
        }
    }

    @Test
    fun stringValue_doesNotInvokeCallback_whenOnlySelectionChanged() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChangedCount++
                },
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)
        assertThat(onValueChangedCount).isEqualTo(0)

        // Act: wiggle the cursor around a bit.
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(5))

        rule.runOnIdle {
            assertThat(onValueChangedCount).isEqualTo(0)
        }
    }

    @Test
    fun stringValue_doesNotInvokeCallback_whenOnlyCompositionChanged() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChangedCount++
                },
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)
        assertThat(onValueChangedCount).isEqualTo(0)

        // Act: wiggle the composition around a bit
        inputMethodInterceptor.withInputConnection { setComposingRegion(0, 0) }
        inputMethodInterceptor.withInputConnection { setComposingRegion(3, 5) }

        rule.runOnIdle {
            assertThat(onValueChangedCount).isEqualTo(0)
        }
    }

    @Test
    fun stringValue_doesNotInvokeCallback_whenTextChangedFromCode_whileUnfocused() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChangedCount++
                },
                modifier = Modifier.testTag(Tag)
            )
        }
        assertThat(onValueChangedCount).isEqualTo(0)

        rule.runOnIdle {
            text = "hello"
        }

        rule.runOnIdle {
            assertThat(onValueChangedCount).isEqualTo(0)
        }
    }

    @Test
    fun stringValue_doesNotInvokeCallback_whenTextChangedFromCode_whileFocused() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChangedCount++
                },
                modifier = Modifier.testTag(Tag)
            )
        }
        assertThat(onValueChangedCount).isEqualTo(0)
        requestFocus(Tag)

        rule.runOnIdle {
            text = "hello"
        }

        rule.runOnIdle {
            assertThat(onValueChangedCount).isEqualTo(0)
        }
    }

    @Test
    fun textFieldValue_usesInitialSelectionFromValue() {
        var text by mutableStateOf(TextFieldValue("hello", selection = TextRange(2)))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        assertTextSelection(TextRange(2))
    }

    @Test
    fun textFieldValue_reportsSelectionChangesInCallback() {
        var text by mutableStateOf(TextFieldValue("hello", selection = TextRange(1)))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2))

        rule.runOnIdle {
            assertThat(text.selection).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun textFieldValue_reportsCompositionChangesInCallback() {
        var text by mutableStateOf(TextFieldValue("hello", selection = TextRange(1)))
        rule.setContent {
            BasicSecureTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { setComposingRegion(0, 0) }
        rule.runOnIdle {
            assertWithMessage(
                "After setting composing region to 0, 0, TextFieldState's composition is:"
            ).that(text.composition).isNull()
        }

        inputMethodInterceptor.withInputConnection { setComposingRegion(1, 4) }
        rule.runOnIdle {
            assertWithMessage(
                "After setting composing region to 1, 4, TextFieldState's composition is:"
            ).that(text.composition).isEqualTo(TextRange(1, 4))
        }
    }

    @Test
    fun inputMethod_doesNotRestart_inResponseToKeyEvents() {
        val state = TextFieldState("hello", initialSelectionInChars = TextRange(5))
        rule.setContent {
            BasicSecureTextField(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
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

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).requestFocus()

    private fun assertTextSelection(expected: TextRange) {
        val selection = rule.onNodeWithTag(Tag).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertWithMessage("Expected selection to be $expected")
            .that(selection).isEqualTo(expected)
    }
}
