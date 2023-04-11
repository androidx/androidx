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

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text2.input.TextFieldBufferWithSelection
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.AndroidTextInputAdapter
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextField2Test {
    @get:Rule
    val rule = createComposeRule()

    private val Tag = "BasicTextField2"

    @After
    fun tearDown() {
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests(null)
    }

    @Test
    fun textField_rendersEmptyContent() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult?.layoutInput?.text).isEqualTo(AnnotatedString(""))
        }
    }

    @Test
    fun textField_contentChange_updatesState() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        rule.setContent {
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

        rule.onNodeWithTag(Tag).assertTextEquals("Hello World!")
        val selection = rule.onNodeWithTag(Tag).fetchSemanticsNode()
            .config.getOrNull(TextSelectionRange)
        assertThat(selection).isEqualTo(TextRange("Hello World!".length))
    }

    /**
     * This is a goal that we set for ourselves. Only updating the editing buffer should not cause
     * BasicTextField to recompose.
     */
    @Test
    fun textField_imeUpdatesDontCauseRecomposition() {
        val state = TextFieldState()
        var compositionCount = 0
        var textLayoutResultCount = 0
        rule.setContent {
            compositionCount++
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                onTextLayout = { textLayoutResultCount++ }
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("hello")
        }

        rule.runOnIdle {
            assertThat(compositionCount).isEqualTo(1)
            assertThat(textLayoutResultCount).isEqualTo(2)
        }
    }

    @Test
    fun textField_textStyleFontSizeChange_relayouts() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        var style by mutableStateOf(TextStyle(fontSize = 20.sp))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResults += it }
            )
        }

        style = TextStyle(fontSize = 30.sp)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults.map { it.layoutInput.style.fontSize })
                .isEqualTo(listOf(20.sp, 30.sp))
        }
    }

    @Test
    fun textField_textStyleColorChange_doesNotRelayout() {
        val state = TextFieldState("Hello")
        var style by mutableStateOf(TextStyle(color = Color.Red))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResults += it }
            )
        }

        style = TextStyle(color = Color.Blue)

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults[0].multiParagraph)
                .isSameInstanceAs(textLayoutResults[1].multiParagraph)
        }
    }

    @Test
    fun textField_contentChange_relayouts() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag),
                onTextLayout = { textLayoutResults += it }
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle {
            assertThat(textLayoutResults.size).isEqualTo(2)
            assertThat(textLayoutResults.map { it.layoutInput.text.text })
                .isEqualTo(listOf("Hello ", "Hello World!"))
        }
    }

    @Test
    fun textField_focus_showsSoftwareKeyboard() {
        val state = TextFieldState()
        val keyboardHelper = KeyboardHelper(rule)
        rule.setContent {
            keyboardHelper.initialize()
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        keyboardHelper.waitForKeyboardVisibility(true)

        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
        }
    }

    @Ignore // b/273412941
    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifDisabled() {
        val state = TextFieldState()
        val keyboardHelper = KeyboardHelper(rule)
        rule.setContent {
            keyboardHelper.initialize()
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
        rule.onNodeWithTag(Tag).assertIsNotFocused()

        keyboardHelper.waitForKeyboardVisibility(false)

        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
        }
    }

    @Test
    fun textField_whenStateObjectChanges_newTextIsRendered() {
        val state1 = TextFieldState("Hello")
        val state2 = TextFieldState("World")
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        rule.setContent {
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
        rule.setContent {
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
        var editorInfo: EditorInfo? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { info, _ ->
            editorInfo = info
        }
        val state = TextFieldState()
        rule.setContent {
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

        rule.runOnIdle {
            assertThat(editorInfo).isNotNull()
            assertThat(editorInfo!!.imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(editorInfo!!.inputType and EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .isNotEqualTo(0)
            assertThat(editorInfo!!.inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
                .isNotEqualTo(0)
        }
    }

    @Test
    fun textField_appliesFilter_toInputConnection() {
        var inputConnection: InputConnection? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        rule.runOnIdle { inputConnection!!.commitText("hello") }
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toSetTextSemanticsAction() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toInsertTextSemanticsAction() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toKeyEvents() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Ignore("b/276932521")
    @Test
    fun textField_appliesFilter_toInputConnection_afterChanging() {
        var inputConnection: InputConnection? = null
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }

        val state = TextFieldState()
        var filter by mutableStateOf<TextEditFilter?>(null)
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = filter,
                modifier = Modifier.testTag(Tag)
            )
        }
        requestFocus(Tag)

        rule.runOnIdle { inputConnection!!.commitText("hello") }
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = RejectAllTextFilter

        rule.runOnIdle { inputConnection!!.commitText("world") }
        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        filter = null

        rule.runOnIdle { inputConnection!!.commitText("world") }
        rule.onNodeWithTag(Tag).assertTextEquals("helloworld")
    }

    @Test
    fun textField_appliesFilter_toSetTextSemanticsAction_afterChanging() {
        val state = TextFieldState()
        var filter by mutableStateOf<TextEditFilter?>(null)
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = filter,
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
        var filter by mutableStateOf<TextEditFilter?>(null)
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = filter,
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
        var filter by mutableStateOf<TextEditFilter?>(null)
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = filter,
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
        lateinit var inputConnection: InputConnection
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        val state = TextFieldState()
        lateinit var changes: ChangeList
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle { inputConnection.commitText("hello") }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0, 0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionComposes() {
        lateinit var inputConnection: InputConnection
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        val state = TextFieldState()
        lateinit var changes: ChangeList
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle { inputConnection.setComposingText("hello", 1) }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletes() {
        lateinit var inputConnection: InputConnection
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        val state = TextFieldState("hello")
        lateinit var changes: ChangeList
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            inputConnection.beginBatchEdit()
            inputConnection.finishComposingText()
            inputConnection.setSelection(5, 5)
            inputConnection.deleteSurroundingText(1, 0)
            inputConnection.endBatchEdit()
        }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(4, 4))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletesViaComposition() {
        lateinit var inputConnection: InputConnection
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { _, ic ->
            inputConnection = ic
        }
        val state = TextFieldState("hello")
        lateinit var changes: ChangeList
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
                    if (new.changes.changeCount > 0) {
                        changes = new.changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            inputConnection.beginBatchEdit()
            inputConnection.setComposingRegion(0, 5)
            inputConnection.setComposingText("h", 1)
            inputConnection.endBatchEdit()
        }

        rule.runOnIdle {
            assertThat(changes.changeCount).isEqualTo(1)
            assertThat(changes.getRange(0)).isEqualTo(TextRange(0, 1))
            assertThat(changes.getOriginalRange(0)).isEqualTo(TextRange(0, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenKeyEventInserts() {
        val state = TextFieldState()
        lateinit var changes: ChangeList
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
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
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
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
        rule.setContent {
            BasicTextField2(
                state = state,
                filter = { _, new ->
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
        lateinit var editorInfo: EditorInfo
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        val filter = KeyboardOptionsFilter(
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Previous
            )
        )
        rule.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                filter = filter,
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(editorInfo.imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(editorInfo.inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_mergedWithParams() {
        lateinit var editorInfo: EditorInfo
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        val filter = KeyboardOptionsFilter(KeyboardOptions(imeAction = ImeAction.Previous))
        rule.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                filter = filter,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(editorInfo.imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(editorInfo.inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_overriddenByParams() {
        lateinit var editorInfo: EditorInfo
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        val filter = KeyboardOptionsFilter(KeyboardOptions(imeAction = ImeAction.Previous))
        rule.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                filter = filter,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(editorInfo.imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_applyWhenFilterChanged() {
        lateinit var editorInfo: EditorInfo
        AndroidTextInputAdapter.setInputConnectionCreatedListenerForTests { ei, _ ->
            editorInfo = ei
        }
        var filter by mutableStateOf(
            KeyboardOptionsFilter(
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Previous
                )
            )
        )
        rule.setContent {
            BasicTextField2(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag),
                filter = filter,
            )
        }
        requestFocus(Tag)

        rule.runOnIdle {
            assertThat(editorInfo.imeOptions and EditorInfo.IME_ACTION_PREVIOUS).isNotEqualTo(0)
            assertThat(editorInfo.inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .isNotEqualTo(0)
        }

        filter = KeyboardOptionsFilter(
            KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Search
            )
        )

        rule.runOnIdle {
            assertThat(editorInfo.imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
            assertThat(editorInfo.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .isNotEqualTo(0)
        }
    }

    private fun requestFocus(tag: String) =
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)

    private fun InputConnection.commitText(text: String) {
        beginBatchEdit()
        finishComposingText()
        commitText(text, 1)
        endBatchEdit()
    }

    private object RejectAllTextFilter : TextEditFilter {
        override fun filter(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBufferWithSelection
        ) {
            valueWithChanges.revertAllChanges()
        }
    }

    private class KeyboardOptionsFilter(override val keyboardOptions: KeyboardOptions) :
        TextEditFilter {
        override fun filter(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBufferWithSelection
        ) {
            // Noop
        }
    }
}
