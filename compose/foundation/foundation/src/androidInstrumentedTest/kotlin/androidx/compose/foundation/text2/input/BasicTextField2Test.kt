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

package androidx.compose.foundation.text2.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text2.input.internal.selection.FakeClipboardManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.drop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextField2Test {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    private val imm = FakeInputMethodManager()

    @Test
    fun textField_rendersEmptyContent() {
        var textLayoutResult: (() -> TextLayoutResult?)? = null
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult?.invoke()?.layoutInput?.text).isEqualTo(AnnotatedString(""))
        }
    }

    @Test
    fun textFieldState_textChange_updatesState() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello World!")
        }
    }

    @Test
    fun textFieldState_textChange_updatesSemantics() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.onNodeWithTag(Tag).assertTextEquals("Hello World!")
        assertTextSelection(TextRange("Hello World!".length))
    }

    @Test
    fun stringValue_textChange_updatesState() {
        var state by mutableStateOf("Hello ")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                value = state,
                onValueChange = { state = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(state).isEqualTo("Hello World!")
        }
    }

    /**
     * This is a goal that we set for ourselves. Only updating the editing buffer should not cause
     * BasicTextField to recompose.
     */
    @Test
    fun textField_imeUpdatesDontCauseRecomposition() {
        val state = TextFieldState()
        var compositionCount = 0
        inputMethodInterceptor.setTextFieldTestContent {
            compositionCount++
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).performTextInput("world")

        rule.onNodeWithTag(Tag).assertTextEquals("helloworld")
        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
        }
    }

    @Test
    fun textField_textStyleFontSizeChange_relayouts() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        var style by mutableStateOf(TextStyle(fontSize = 20.sp))
        var textLayoutResultState: (() -> TextLayoutResult?)? by mutableStateOf(null)
        val textLayoutResults = mutableListOf<TextLayoutResult?>()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResultState = it }
            )

            LaunchedEffect(Unit) {
                snapshotFlow { textLayoutResultState?.invoke() }
                    .drop(1)
                    .collect { textLayoutResults += it }
            }
        }

        style = TextStyle(fontSize = 30.sp)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults.map { it?.layoutInput?.style?.fontSize })
                .containsExactly(20.sp, 30.sp)
                .inOrder()
        }
    }

    @Test
    fun textField_textStyleColorChange_doesNotRelayout() {
        val state = TextFieldState("Hello")
        var style by mutableStateOf(TextStyle(color = Color.Red))
        var textLayoutResultState: (() -> TextLayoutResult?)? by mutableStateOf(null)
        val textLayoutResults = mutableListOf<TextLayoutResult?>()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResultState = it }
            )

            LaunchedEffect(Unit) {
                snapshotFlow { textLayoutResultState?.invoke() }
                    .drop(1)
                    .collect { textLayoutResults += it }
            }
        }

        style = TextStyle(color = Color.Blue)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults[0]?.multiParagraph)
                .isSameInstanceAs(textLayoutResults[1]?.multiParagraph)
            assertThat(textLayoutResults[0]?.layoutInput?.style?.color).isEqualTo(Color.Red)
            assertThat(textLayoutResults[1]?.layoutInput?.style?.color).isEqualTo(Color.Blue)
        }
    }

    @Test
    fun textField_contentChange_relayouts() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        var textLayoutResultState: (() -> TextLayoutResult?)? by mutableStateOf(null)
        val textLayoutResults = mutableListOf<TextLayoutResult?>()
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalWindowInfo provides object : WindowInfo {
                override val isWindowFocused = true
            }) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(Tag),
                    onTextLayout = { textLayoutResultState = it }
                )
            }

            LaunchedEffect(Unit) {
                snapshotFlow { textLayoutResultState?.invoke() }
                    .drop(1)
                    .collect { textLayoutResults += it }
            }
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(textLayoutResults.map { it?.layoutInput?.text?.text })
                .containsExactly("Hello ", "Hello World!")
                .inOrder()
        }
    }

    @Test
    fun textField_focus_showsSoftwareKeyboard() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifDisabled() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                enabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertIsNotEnabled()
        rule.onNodeWithTag(Tag).performClick()

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifReadOnly() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                readOnly = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_whenNotShowSoftwareKeyboard() {
        val state = TextFieldState()
        val focusRequester = FocusRequester()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                keyboardOptions = KeyboardOptions(shouldShowKeyboardOnFocus = false),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
                    .focusRequester(focusRequester)
            )
        }
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun textField_tap_showSoftwareKeyboard_whenNotShowSoftwareKeyboard() {
        val state = TextFieldState()
        val focusRequester = FocusRequester()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                keyboardOptions = KeyboardOptions(shouldShowKeyboardOnFocus = false),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
                    .focusRequester(focusRequester)
            )
        }
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()
        rule.onNodeWithTag(Tag).assertIsFocused()
        rule.onNodeWithTag(Tag).performClick()

        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun hideKeyboardWhenDisposed() {
        val keyboardHelper = KeyboardHelper(rule)
        val state = TextFieldState("initial text")
        var toggle by mutableStateOf(true)
        rule.setContent {
            keyboardHelper.initialize()

            if (toggle) {
                BasicTextField2(
                    state = state,
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
        assertFalse(keyboardHelper.isSoftwareKeyboardShown())
    }

    @Test
    fun hideKeyboardWhenFocusCleared() {
        val keyboardHelper = KeyboardHelper(rule)
        val state = TextFieldState("initial text")
        lateinit var focusManager: FocusManager
        rule.setContent {
            keyboardHelper.initialize()
            focusManager = LocalFocusManager.current
            Row {
                // Extra focusable that takes initial focus when focus is cleared.
                Box(
                    Modifier
                        .size(10.dp)
                        .focusable())
                BasicTextField2(
                    state = state,
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
        assertFalse(keyboardHelper.isSoftwareKeyboardShown())
    }

    @Test
    fun textField_whenStateObjectChanges_newTextIsRendered() {
        val state1 = TextFieldState("Hello")
        val state2 = TextFieldState("World")
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                enabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assertTextEquals("Hello")
        toggleState = !toggleState
        rule.onNodeWithTag(Tag).assertTextEquals("World")
    }

    @Test
    fun textField_whenStateObjectChanges_restartsInput() {
        val state1 = TextFieldState("Hello")
        val state2 = TextFieldState("World")
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                enabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextReplacement("Compose")
            assertTextEquals("Compose")
        }
        toggleState = !toggleState
        with(rule.onNodeWithTag(Tag)) {
            performTextReplacement("Compose2")
            assertTextEquals("Compose2")
        }
        assertThat(state1.text.toString()).isEqualTo("Compose")
        assertThat(state2.text.toString()).isEqualTo("Compose2")
    }

    @Test
    fun textField_passesKeyboardOptionsThrough() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                // We don't need to test all combinations here, that is tested in EditorInfoTest.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Previous
                )
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(inputType and EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_appliesFilter_toInputConnection() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { commitText("hello") }
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toSetTextSemanticsAction() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toInsertTextSemanticsAction() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toKeyEvents() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toInputConnection_afterChanging() {
        val state = TextFieldState()
        var filter by mutableStateOf<InputTransformation?>(null)
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { commitText("hello") }
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = RejectAllTextFilter

        inputMethodInterceptor.withInputConnection { commitText("world") }
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = null

        inputMethodInterceptor.withInputConnection { commitText("world") }
        rule.onNodeWithTag(Tag).assertTextEquals("helloworld")
    }

    @Test
    fun textField_appliesFilter_toSetTextSemanticsAction_afterChanging() {
        val state = TextFieldState()
        var filter by mutableStateOf<InputTransformation?>(null)
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = RejectAllTextFilter

        rule.onNodeWithTag(Tag).performTextReplacement("world")
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = null

        rule.onNodeWithTag(Tag).performTextReplacement("world")
        rule.onNodeWithTag(Tag).assertTextEquals("world")
    }

    @Test
    fun textField_appliesFilter_toInsertTextSemanticsAction_afterChanging() {
        val state = TextFieldState()
        var filter by mutableStateOf<InputTransformation?>(null)
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = RejectAllTextFilter

        rule.onNodeWithTag(Tag).performTextInput("world")
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = null

        rule.onNodeWithTag(Tag).performTextInput("world")
        rule.onNodeWithTag(Tag).assertTextEquals("helloworld")
    }

    @Test
    fun textField_appliesFilter_toKeyEvents_afterChanging() {
        val state = TextFieldState()
        var filter by mutableStateOf<InputTransformation?>(null)
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = RejectAllTextFilter

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Spacebar) }
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = null

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Spacebar) }
        rule.onNodeWithTag(Tag).assertTextEquals("hello ")
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionCommits() {
        val state = TextFieldState()
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { commitText("hello") }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0, 0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionComposes() {
        val state = TextFieldState()
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { setComposingText("hello", 1) }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletes() {
        val state = TextFieldState("hello")
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            beginBatchEdit()
            finishComposingText()
            setSelection(5, 5)
            deleteSurroundingText(1, 0)
            endBatchEdit()
        }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(4, 4))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletesViaComposition() {
        val state = TextFieldState("hello")
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            beginBatchEdit()
            setComposingRegion(0, 5)
            setComposingText("h", 1)
            endBatchEdit()
        }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(1, 1))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(1, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenKeyEventInserts() {
        val state = TextFieldState()
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 1))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenKeyEventDeletes() {
        val state = TextFieldState("hello")
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(5))
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Backspace) }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(4, 4))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenSemanticsActionInserts() {
        val state = TextFieldState()
        lateinit var changes: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                inputTransformation = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_filterKeyboardOptions_sentToIme() {
        val filter = KeyboardOptionsFilter(
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Previous
            )
        )
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                inputTransformation = filter,
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_mergedWithParams() {
        val filter = KeyboardOptionsFilter(KeyboardOptions(imeAction = ImeAction.Previous))
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                inputTransformation = filter,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_overriddenByParams() {
        val filter = KeyboardOptionsFilter(KeyboardOptions(imeAction = ImeAction.Previous))
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                inputTransformation = filter,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_applyWhenFilterChanged() {
        var filter by mutableStateOf(
            KeyboardOptionsFilter(
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Previous
                )
            )
        )
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalWindowInfo provides object : WindowInfo {
                override val isWindowFocused = true
            }) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = filter,
                )
            }
        }
        requestFocus(Tag)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).isNotEqualTo(0)
        }

        filter = KeyboardOptionsFilter(
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Search
            )
        )

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL).isNotEqualTo(0)
        }
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun textField_showsKeyboardAgainWhenTapped_ifFocused() {
        val testKeyboardController = TestSoftwareKeyboardController(rule)
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag)
                )
            }
        }
        // Focusing the field will show the keyboard without using the SoftwareKeyboardController.
        rule.onNodeWithTag(Tag).requestFocus()
        testKeyboardController.hide()

        // This will go through the SoftwareKeyboardController to show the keyboard, since a session
        // is already active.
        rule.onNodeWithTag(Tag).performClick()

        testKeyboardController.assertShown()
    }

    @Test
    fun swipingThroughTextField_doesNotGainFocus() {
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTouchInput {
            // swipe through
            swipeRight(endX = right + 200, durationMillis = 100)
        }
        rule.onNodeWithTag(Tag).assertIsNotFocused()
    }

    @Test
    fun swipingTextFieldInScrollableContainer_doesNotGainFocus() {
        val scrollState = ScrollState(0)
        inputMethodInterceptor.setTextFieldTestContent {
            Column(
                Modifier
                    .height(100.dp)
                    .verticalScroll(scrollState)
            ) {
                BasicTextField2(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag)
                )
                Box(Modifier.height(200.dp))
            }
        }

        rule.onNodeWithTag(Tag).performTouchInput { swipeUp() }
        rule.onNodeWithTag(Tag).assertIsNotFocused()
        assertThat(scrollState.value).isNotEqualTo(0)
    }

    @Test
    fun densityChanges_causesRelayout() {
        val state = TextFieldState("Hello")
        var density by mutableStateOf(Density(1f))
        val fontSize = 20.sp
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField2(
                    state = state,
                    textStyle = TextStyle(
                        fontFamily = TEST_FONT_FAMILY,
                        fontSize = fontSize
                    ),
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        val firstSize = rule.onNodeWithTag(Tag).fetchTextLayoutResult().size

        density = Density(2f)

        val secondSize = rule.onNodeWithTag(Tag).fetchTextLayoutResult().size

        assertThat(secondSize.width).isEqualTo(firstSize.width * 2)
        assertThat(secondSize.height).isEqualTo(firstSize.height * 2)
    }

    @Test
    fun stringValue_updatesFieldText_whenTextChangedFromCode_whileUnfocused() {
        var text by mutableStateOf("hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.runOnIdle {
            text = "world"
        }

        rule.onNodeWithTag(Tag).assertTextEquals("world")
    }

    @Test
    fun stringValue_doesNotUpdateField_whenTextChangedFromCode_whileFocused() {
        var text by mutableStateOf("hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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
    fun stringValue_doesNotInvokeCallback_onFocus() {
        var text by mutableStateOf("")
        var onValueChangedCount = 0
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
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

    // Regression test for b/311834126
    @Test
    fun whenPastingTextThatIncreasesEndOffset_noCrashAndCursorAtEndOfPastedText() {
        val longText = "Text".repeat(4)
        val shortText = "Text".repeat(2)

        lateinit var tfs: TextFieldState
        val clipboardManager = object : ClipboardManager {
            var contents: AnnotatedString? = null

            override fun setText(annotatedString: AnnotatedString) {
                contents = annotatedString
            }

            override fun getText(): AnnotatedString? {
                return contents
            }
        }
        inputMethodInterceptor.setTextFieldTestContent {
            tfs = rememberTextFieldState(shortText)
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = tfs,
                    modifier = Modifier.testTag(Tag),
                )
            }
        }
        clipboardManager.setText(AnnotatedString(longText))
        rule.waitForIdle()

        val node = rule.onNodeWithTag(Tag)
        node.performTouchInput { longClick(center) }
        rule.waitForIdle()

        node.performSemanticsAction(SemanticsActions.PasteText) { it() }
        rule.waitForIdle()

        assertThat(tfs.text.toString()).isEqualTo(longText)
        assertThat(tfs.text.selectionInChars).isEqualTo(TextRange(longText.length))
    }

    @Test
    fun selectAll_contextMenuAction_informsImeOfSelectionChange() {
        immRule.setFactory { imm }
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag)
            )
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            performContextMenuAction(android.R.id.selectAll)
        }

        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
            assertThat(imm.expectCall("updateSelection(0, 5, -1, -1)"))
        }
    }

    @Test
    fun cut_contextMenuAction_cutsIntoClipboard() {
        val clipboardManager = FakeClipboardManager("World")
        val state = TextFieldState("Hello", initialSelectionInChars = TextRange(0, 2))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            performContextMenuAction(android.R.id.cut)
        }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.text).isEqualTo("He")
            assertThat(state.text.toString()).isEqualTo("llo")
        }
    }

    @Test
    fun copy_contextMenuAction_copiesIntoClipboard() {
        val clipboardManager = FakeClipboardManager("World")
        val state = TextFieldState("Hello", initialSelectionInChars = TextRange(0, 2))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            performContextMenuAction(android.R.id.copy)
        }

        rule.runOnIdle {
            assertThat(clipboardManager.getText()?.text).isEqualTo("He")
        }
    }

    @Test
    fun paste_contextMenuAction_pastesFromClipboard() {
        val clipboardManager = FakeClipboardManager("World")
        val state = TextFieldState("Hello", initialSelectionInChars = TextRange(0, 4))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                BasicTextField2(
                    state = state,
                    modifier = Modifier.testTag(Tag)
                )
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection {
            performContextMenuAction(android.R.id.paste)
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Worldo")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(5))
        }
    }

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).requestFocus()

    private fun assertTextSelection(expected: TextRange) {
        val selection = rule.onNodeWithTag(Tag).fetchSemanticsNode()
            .config.getOrNull(TextSelectionRange)
        assertThat(selection).isEqualTo(expected)
    }

    private fun InputConnection.commitText(text: String) {
        beginBatchEdit()
        finishComposingText()
        commitText(text, 1)
        endBatchEdit()
    }

    private object RejectAllTextFilter : InputTransformation {
        override fun transformInput(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBuffer
        ) {
            valueWithChanges.revertAllChanges()
        }
    }

    private class KeyboardOptionsFilter(override val keyboardOptions: KeyboardOptions) :
        InputTransformation {
        override fun transformInput(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBuffer
        ) {
            // Noop
        }
    }
}
