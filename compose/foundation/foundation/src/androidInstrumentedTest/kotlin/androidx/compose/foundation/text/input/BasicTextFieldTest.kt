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

import android.R
import android.os.Build
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.computeSizeForDefaultText
import androidx.compose.foundation.text.contextmenu.test.FakeToolbarRequester
import androidx.compose.foundation.text.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.testutils.assertPixelColor
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalFontFamilyResolver
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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasPerformImeAction
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.toSpanned
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class BasicTextFieldTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField"

    private val imm = FakeInputMethodManager()

    @Test
    fun textField_rendersEmptyContent() {
        var textLayoutResult: (() -> TextLayoutResult?)? = null
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { textLayoutResult = it },
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
            BasicTextField(state = state, modifier = Modifier.fillMaxSize().testTag(Tag))
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.runOnIdle { assertThat(state.text.toString()).isEqualTo("Hello World!") }
    }

    @Test
    fun textFieldState_textChange_updatesSemantics() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.fillMaxSize().testTag(Tag))
        }

        rule.onNodeWithTag(Tag).performTextInput("World!")

        rule.onNodeWithTag(Tag).assertTextEquals("Hello World!")
        assertTextSelection(TextRange("Hello World!".length))
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
            BasicTextField(state = state, modifier = Modifier.fillMaxSize().testTag(Tag))
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).performTextInput("world")

        rule.onNodeWithTag(Tag).assertTextEquals("helloworld")
        rule.runOnIdle { assertThat(compositionCount).isEqualTo(1) }
    }

    @Test
    fun textField_textStyleFontSizeChange_relayouts() {
        val state = TextFieldState("Hello ", TextRange(Int.MAX_VALUE))
        var style by mutableStateOf(TextStyle(fontSize = 20.sp))
        var textLayoutResultState: (() -> TextLayoutResult?)? by mutableStateOf(null)
        val textLayoutResults = mutableListOf<TextLayoutResult?>()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResultState = it },
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
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                textStyle = style,
                onTextLayout = { textLayoutResultState = it },
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
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                onTextLayout = { textLayoutResultState = it },
            )

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
            BasicTextField(state = state, modifier = Modifier.fillMaxSize().testTag(Tag))
        }

        rule.onNodeWithTag(Tag).performClick()
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun textField_focus_doesNotShowSoftwareKeyboard_ifDisabled() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                enabled = false,
                modifier = Modifier.fillMaxSize().testTag(Tag),
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
            BasicTextField(
                state = state,
                readOnly = true,
                modifier = Modifier.fillMaxSize().testTag(Tag),
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
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false),
                modifier = Modifier.fillMaxSize().testTag(Tag).focusRequester(focusRequester),
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.onNodeWithTag(Tag).assertIsFocused()

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun textField_tap_showSoftwareKeyboard_whenNotShowSoftwareKeyboard() {
        val state = TextFieldState()
        val focusRequester = FocusRequester()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false),
                modifier = Modifier.fillMaxSize().testTag(Tag).focusRequester(focusRequester),
            )
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.onNodeWithTag(Tag).assertIsFocused()
        rule.onNodeWithTag(Tag).performClick()

        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun disposeSession_whenTextFieldIsRemoved() {
        val state = TextFieldState("initial text")
        var toggle by mutableStateOf(true)
        inputMethodInterceptor.setContent {
            if (toggle) {
                BasicTextField(state = state, modifier = Modifier.testTag("TextField"))
            }
        }

        rule.onNodeWithTag("TextField").requestFocus()
        inputMethodInterceptor.assertSessionActive()

        toggle = false

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun disposeSessionWhenFocusCleared() {
        val state = TextFieldState("initial text")
        lateinit var focusManager: FocusManager
        inputMethodInterceptor.setContent {
            focusManager = LocalFocusManager.current
            Row {
                // Extra focusable that takes initial focus when focus is cleared.
                Box(Modifier.size(10.dp).focusable())
                BasicTextField(state = state, modifier = Modifier.testTag("TextField"))
            }
        }

        rule.onNodeWithTag("TextField").requestFocus()

        inputMethodInterceptor.assertSessionActive()

        rule.runOnIdle { focusManager.clearFocus() }

        inputMethodInterceptor.assertNoSessionActive()
    }

    @Test
    fun textField_whenStateObjectChanges_newTextIsRendered() {
        val state1 = TextFieldState("Hello")
        val state2 = TextFieldState("World")
        var toggleState by mutableStateOf(true)
        val state by derivedStateOf { if (toggleState) state1 else state2 }
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                enabled = true,
                modifier = Modifier.fillMaxSize().testTag(Tag),
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
            BasicTextField(
                state = state,
                enabled = true,
                modifier = Modifier.fillMaxSize().testTag(Tag),
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
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                // We don't need to test all combinations here, that is tested in EditorInfoTest.
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Previous,
                    ),
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
            BasicTextField(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { commitText("hello") }
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toInputConnection_changingComposition() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { setComposingText("hello", 1) }
        rule.onNodeWithTag(Tag).assertTextEquals("")
        assertThat(state.composition).isNull()
    }

    @Test
    fun textField_appliesFilter_toSetTextSemanticsAction() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toInsertTextSemanticsAction() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")
        rule.onNodeWithTag(Tag).assertTextEquals("")
    }

    @Test
    fun textField_appliesFilter_toKeyEvents() {
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = RejectAllTextFilter,
                modifier = Modifier.testTag(Tag),
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
            BasicTextField(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag),
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
            BasicTextField(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag),
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
            BasicTextField(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag),
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
            BasicTextField(
                state = state,
                inputTransformation = filter,
                modifier = Modifier.testTag(Tag),
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
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { commitText("hello") }

        rule.runOnIdle {
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(0, 0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionComposes() {
        val state = TextFieldState()
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { setComposingText("hello", 1) }

        rule.runOnIdle {
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletes() {
        val state = TextFieldState("hello")
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
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
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(4, 4))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenInputConnectionDeletesViaComposition() {
        val state = TextFieldState("hello")
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
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
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(1, 1))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(1, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenKeyEventInserts() {
        val state = TextFieldState()
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }
        requestFocus(Tag)

        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.A) }

        rule.runOnIdle {
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(0, 1))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_changesAreTracked_whenKeyEventDeletes() {
        val state = TextFieldState("hello")
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(5))
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(Key.Backspace) }

        rule.runOnIdle {
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(4, 4))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        }
    }

    @Test
    fun textField_changesAreTracked_whenSemanticsActionInserts() {
        val state = TextFieldState()
        lateinit var changeList: ChangeList
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                inputTransformation = {
                    if (changes.changeCount > 0) {
                        changeList = changes
                    }
                },
                modifier = Modifier.testTag(Tag),
            )
        }

        rule.onNodeWithTag(Tag).performTextInput("hello")

        rule.runOnIdle {
            assertThat(changeList.changeCount).isEqualTo(1)
            assertThat(changeList.getRange(0)).isEqualTo(TextRange(0, 5))
            assertThat(changeList.getOriginalRange(0)).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_filterKeyboardOptions_sentToIme() {
        val filter =
            KeyboardOptionsFilter(
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Previous)
            )
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
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
            BasicTextField(
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
            BasicTextField(
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
        var filter by
            mutableStateOf(
                KeyboardOptionsFilter(
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Previous,
                    )
                )
            )
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
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

        filter =
            KeyboardOptionsFilter(
                KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Search)
            )

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_filterKeyboardOptions_applyWhenKeyboardOptionsChanged() {
        var keyboardOptionsState by
            mutableStateOf(
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Previous)
            )
        val filter =
            object : InputTransformation {
                override val keyboardOptions: KeyboardOptions
                    get() = keyboardOptionsState

                override fun TextFieldBuffer.transformInput() = Unit
            }

        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
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

        keyboardOptionsState =
            KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Search)

        inputMethodInterceptor.withEditorInfo {
            assertThat(imeOptions and EditorInfo.IME_ACTION_SEARCH).isNotEqualTo(0)
            assertThat(inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL).isNotEqualTo(0)
        }
    }

    @Test
    fun textField_showsKeyboardAgainWhenTapped_ifFocused() {
        val testKeyboardController = TestSoftwareKeyboardController(rule)
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                BasicTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
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
            BasicTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
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
            Column(Modifier.height(100.dp).verticalScroll(scrollState)) {
                BasicTextField(state = rememberTextFieldState(), modifier = Modifier.testTag(Tag))
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
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize),
                    modifier = Modifier.testTag(Tag),
                )
            }
        }

        val firstSize = rule.onNodeWithTag(Tag).fetchTextLayoutResult().size

        density = Density(2f)

        val secondSize = rule.onNodeWithTag(Tag).fetchTextLayoutResult().size

        assertThat(secondSize.width).isEqualTo(firstSize.width * 2)
        assertThat(secondSize.height).isEqualTo(firstSize.height * 2)
    }

    // Regression test for b/311834126
    @Test
    fun whenPastingTextThatIncreasesEndOffset_noCrashAndCursorAtEndOfPastedText() = runTest {
        val longText = "Text".repeat(4)
        val shortText = "Text".repeat(2)

        lateinit var tfs: TextFieldState
        val clipboard = FakeClipboard()
        inputMethodInterceptor.setTextFieldTestContent {
            tfs = rememberTextFieldState(shortText)
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = tfs, modifier = Modifier.testTag(Tag))
            }
        }
        clipboard.setClipEntry(AnnotatedString(longText).toClipEntry())
        rule.waitForIdle()

        val node = rule.onNodeWithTag(Tag)
        node.performTouchInput { longClick(center) }
        rule.waitForIdle()

        node.performSemanticsAction(SemanticsActions.PasteText) { it() }
        rule.waitForIdle()

        assertThat(tfs.text.toString()).isEqualTo(longText)
        assertThat(tfs.selection).isEqualTo(TextRange(longText.length))
    }

    @Test
    fun selectAll_contextMenuAction_informsImeOfSelectionChange() {
        immRule.setFactory { imm }
        val state = TextFieldState("Hello")
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { performContextMenuAction(R.id.selectAll) }

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(0, 5))
            assertThat(imm.expectCall("updateSelection(0, 5, -1, -1)"))
        }
    }

    @Test
    fun cut_contextMenuAction_cutsIntoClipboard() = runTest {
        val clipboard = FakeClipboard("World")
        val state = TextFieldState("Hello", initialSelection = TextRange(0, 2))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { performContextMenuAction(R.id.cut) }

        rule.waitForIdle()
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("He")
        assertThat(state.text.toString()).isEqualTo("llo")
    }

    @Test
    fun copy_contextMenuAction_copiesIntoClipboard() = runTest {
        val clipboard = FakeClipboard("World")
        val state = TextFieldState("Hello", initialSelection = TextRange(0, 2))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { performContextMenuAction(R.id.copy) }

        rule.waitForIdle()
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("He")
    }

    @Test
    fun paste_contextMenuAction_pastesFromClipboard() {
        val clipboard = FakeClipboard("World")
        val state = TextFieldState("Hello", initialSelection = TextRange(0, 4))
        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        requestFocus(Tag)

        inputMethodInterceptor.withInputConnection { performContextMenuAction(R.id.paste) }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Worldo")
            assertThat(state.selection).isEqualTo(TextRange(5))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_defaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = TEST_FONT_FAMILY,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag),
                    state = rememberTextFieldState("A"),
                    textStyle = textStyle,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertHorizontallySymmetrical(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthSmallerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = TEST_FONT_FAMILY,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth =
                computeSizeForDefaultText(
                        style = textStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        maxLines = 1,
                    )
                    .width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp / 2),
                    state = rememberTextFieldState("A"),
                    textStyle = textStyle,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertHorizontallySymmetrical(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthLargerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle =
            TextStyle(
                textAlign = TextAlign.Center,
                color = Color.Black,
                fontFamily = TEST_FONT_FAMILY,
                fontSize = fontSize.sp,
            )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth =
                computeSizeForDefaultText(
                        style = textStyle,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        maxLines = 1,
                    )
                    .width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp * 2),
                    state = rememberTextFieldState("A"),
                    textStyle = textStyle,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertHorizontallySymmetrical(fontSize)
    }

    @Test
    fun textField_state_invokesAutofill() {
        val mockLambda: () -> Unit = mock()
        var density by mutableStateOf(Density(1f))

        val manager =
            TextFieldSelectionState(
                    // other parameters not necessary to test autofill invocation
                    textFieldState =
                        TransformedTextFieldState(
                            textFieldState = TextFieldState(),
                            inputTransformation = null,
                            codepointTransformation = null,
                            outputTransformation = null,
                        ),
                    textLayoutState = TextLayoutState(),
                    density = density,
                    enabled = true,
                    readOnly = false,
                    isFocused = false,
                    isPassword = false,
                    toolbarRequester = FakeToolbarRequester(),
                    coroutineScope = CoroutineScope(EmptyCoroutineContext),
                    platformSelectionBehaviors = null,
                    clipboard = FakeClipboard(),
                )
                .apply { requestAutofillAction = mockLambda }

        manager.autofill()
        verify(mockLambda, times(1)).invoke()
    }

    @Test
    fun changingInputTransformation_doesNotRestartInput() {
        var inputTransformation by mutableStateOf(InputTransformation.maxLength(10))
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                inputTransformation = inputTransformation,
            )
        }

        requestFocus(Tag)
        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(1)

        inputTransformation = InputTransformation.maxLength(15)

        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(1)
    }

    @Test
    fun changingInputTransformation_restartsInput_ifKeyboardOptionsChange() {
        var inputTransformation by mutableStateOf<InputTransformation?>(null)
        inputMethodInterceptor.setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                inputTransformation = inputTransformation,
            )
        }

        requestFocus(Tag)
        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(1)

        inputTransformation = InputTransformation.allCaps(Locale.current)

        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(2)
    }

    @Test
    fun composingRegion_addsUnderlineSpanToLayout() {
        val state = TextFieldState("Hello, World")
        var textLayoutProvider: (() -> TextLayoutResult?)? by mutableStateOf(null)

        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                onTextLayout = { textLayoutProvider = it },
            )
        }

        requestFocus(Tag)
        inputMethodInterceptor.withInputConnection { setComposingRegion(0, 5) }
        rule.runOnIdle {
            val currentTextLayout = textLayoutProvider?.invoke()
            assertThat(currentTextLayout).isNotNull()

            val expectedSpan =
                AnnotatedString.Range(
                    item = SpanStyle(textDecoration = TextDecoration.Underline),
                    start = 0,
                    end = 5,
                )
            assertThat(currentTextLayout!!.multiParagraph.intrinsics.annotatedString.spanStyles)
                .contains(expectedSpan)
        }
    }

    @Test
    fun composingRegion_changesInvalidateLayout() {
        val state = TextFieldState("Hello, World")
        var textLayoutProvider: (() -> TextLayoutResult?)? by mutableStateOf(null)
        state.editAsUser(inputTransformation = null) { setComposition(0, 5) }

        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                onTextLayout = { textLayoutProvider = it },
            )
        }

        requestFocus(Tag)

        // assert initial composing region
        rule.runOnIdle {
            val initialTextLayout = textLayoutProvider?.invoke()
            assertThat(initialTextLayout).isNotNull()

            val expectedSpan =
                AnnotatedString.Range(
                    item = SpanStyle(textDecoration = TextDecoration.Underline),
                    start = 0,
                    end = 5,
                )
            assertThat(initialTextLayout!!.multiParagraph.intrinsics.annotatedString.spanStyles)
                .contains(expectedSpan)
        }

        // change composing region
        inputMethodInterceptor.withInputConnection { setComposingRegion(7, 12) }

        // assert the changed region
        rule.runOnIdle {
            val currentTextLayout = textLayoutProvider?.invoke()
            assertThat(currentTextLayout).isNotNull()

            val expectedSpan =
                AnnotatedString.Range(
                    item = SpanStyle(textDecoration = TextDecoration.Underline),
                    start = 7,
                    end = 12,
                )
            assertThat(currentTextLayout!!.multiParagraph.intrinsics.annotatedString.spanStyles)
                .contains(expectedSpan)
        }
    }

    @Test
    fun phoneKeyboardType_RtlLocaleLtrDigits_resolvesToLtrTextDirection() {
        val state = TextFieldState()
        var textLayoutProvider: (() -> TextLayoutResult?)? by mutableStateOf(null)

        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                textStyle = TextStyle(localeList = LocaleList("ar")),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                onTextLayout = { textLayoutProvider = it },
            )
        }

        rule.runOnIdle {
            // this would normally have been Unspecified.
            assertThat(textLayoutProvider?.invoke()?.layoutInput?.style?.textDirection)
                .isEqualTo(TextDirection.Ltr)
        }
    }

    @SdkSuppress(minSdkVersion = 31) // Adlam digits were added in API 31
    @Test
    fun phoneKeyboardType_RtlLocaleRtlDigits_resolvesToRtlTextDirection() {
        val state = TextFieldState()
        var textLayoutProvider: (() -> TextLayoutResult?)? by mutableStateOf(null)

        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().testTag(Tag),
                textStyle = TextStyle(localeList = LocaleList("ff-Adlm-BF")),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                onTextLayout = { textLayoutProvider = it },
            )
        }

        rule.runOnIdle {
            // this would normally have been Unspecified.
            assertThat(textLayoutProvider?.invoke()?.layoutInput?.style?.textDirection)
                .isEqualTo(TextDirection.Rtl)
        }
    }

    @Test
    fun longText_doesNotCrash() {
        var textLayoutProvider: (() -> TextLayoutResult?)? = null
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                rememberTextFieldState("A".repeat(100_000)),
                onTextLayout = { textLayoutProvider = it },
            )
        }

        rule.runOnIdle {
            assertThat(textLayoutProvider?.invoke()?.layoutInput?.text?.length).isEqualTo(100_000)
        }
    }

    @Test
    fun whenElementFocusLost_compositionIsCleared() {
        lateinit var focusManager: FocusManager
        val focusRequester = FocusRequester()
        val state = TextFieldState()
        inputMethodInterceptor.setTextFieldTestContent {
            focusManager = LocalFocusManager.current
            BasicTextField(state, Modifier.focusRequester(focusRequester))
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        inputMethodInterceptor.withInputConnection { setComposingText("Hello", 1) }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello")
            assertThat(state.composition).isEqualTo(TextRange(0, 5))
        }

        // setTextFieldTestContent puts a focusable box before the content that's set here
        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Previous) }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello")
            assertThat(state.composition).isNull()
        }
    }

    @Test
    fun whenWindowFocusLost_compositionRemains() {
        val focusRequester = FocusRequester()
        val state = TextFieldState()
        var windowInfo: WindowInfo by
            mutableStateOf(
                object : WindowInfo {
                    override val isWindowFocused = true
                }
            )
        inputMethodInterceptor.setContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField(state, Modifier.focusRequester(focusRequester))
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        inputMethodInterceptor.withInputConnection { setComposingText("Hello", 1) }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello")
            assertThat(state.composition).isEqualTo(TextRange(0, 5))
        }

        windowInfo =
            object : WindowInfo {
                override val isWindowFocused = false
            }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("Hello")
            assertThat(state.composition).isEqualTo(TextRange(0, 5))
        }
    }

    @Test
    fun whenWindowFocusGained_unfocusedTextFieldStateIsNotRecomposed() {
        val state = TextFieldState("Hello")
        var isWindowFocused by mutableStateOf(false)
        var windowInfo =
            object : WindowInfo {
                override val isWindowFocused: Boolean
                    get() = isWindowFocused
            }
        var decoratorCallCount = 0
        val decorator = TextFieldDecorator { innerTextField ->
            decoratorCallCount++
            innerTextField()
        }
        rule.setContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField(state = state, decorator = decorator)
            }
        }

        val initialDecoratorCallCount = rule.runOnIdle { decoratorCallCount }
        isWindowFocused = true

        rule.runOnIdle { assertThat(decoratorCallCount).isEqualTo(initialDecoratorCallCount) }
    }

    // regression test for b/355900176#comment2
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun existingInputSession_doesNotSpillOver_toAnotherTextField() {
        inputMethodInterceptor.setContent {
            Column {
                BasicTextField(rememberTextFieldState(), modifier = Modifier.testTag("btf1"))
                InterceptPlatformTextInput({ _, _ -> awaitCancellation() }) {
                    BasicTextField(rememberTextFieldState(), modifier = Modifier.testTag("btf2"))
                }
            }
        }

        rule.onNodeWithTag("btf1").requestFocus()
        inputMethodInterceptor.assertSessionActive()

        rule.onNodeWithTag("btf2").requestFocus()
        inputMethodInterceptor.assertNoSessionActive()

        imm.resetCalls()

        // successive touches should not start the input
        rule.onNodeWithTag("btf2").performClick()
        inputMethodInterceptor.assertNoSessionActive()
        // InputMethodManager should not have received a showSoftInput() call
        rule.runOnIdle { imm.expectNoMoreCalls() }
    }

    @Test
    fun outputTransformation_doesNotLoseComposingAnnotations() {
        val textFieldState = TextFieldState()
        var textLayoutProvider: (() -> TextLayoutResult?)? = null
        inputMethodInterceptor.setTextFieldTestContent {
            BasicTextField(
                textFieldState,
                onTextLayout = { textLayoutProvider = it },
                outputTransformation = { append(" world") },
            )
        }

        rule.onNode(hasPerformImeAction()).requestFocus()

        val spanned =
            SpannableStringBuilder()
                .append("Hello", BackgroundColorSpan(android.graphics.Color.RED), 0)
                .toSpanned()

        inputMethodInterceptor.withInputConnection { setComposingText(spanned, 1) }

        rule.runOnIdle {
            val textLayoutResult = textLayoutProvider?.invoke()
            assertNotNull(textLayoutResult)
            val annotatedString = textLayoutResult.layoutInput.text
            val spanStyles = annotatedString.spanStyles
            assertThat(annotatedString.toString()).isEqualTo("Hello world")
            assertThat(spanStyles.size).isEqualTo(1)
            assertThat(spanStyles.first().start).isEqualTo(0)
            assertThat(spanStyles.first().end).isEqualTo(5)
            assertThat(spanStyles.first().item.background).isEqualTo(Color.Red)
        }
    }

    // Regression test for b/431958747
    @Test
    fun whenWindowFocusLost_outputTransformationChange_restartsInputSession() {
        val state = TextFieldState("Hello")
        val focusRequester = FocusRequester()
        var windowInfo: WindowInfo by
            mutableStateOf(
                object : WindowInfo {
                    override val isWindowFocused = true
                }
            )
        var outputTransformation by
            mutableStateOf<OutputTransformation?>(OutputTransformation { append(" World") })

        inputMethodInterceptor.setTextFieldTestContent {
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester).testTag(Tag),
                    outputTransformation = outputTransformation,
                )
            }
        }

        // Focus the text field and ensure an input session starts.
        rule.onNodeWithTag(Tag).requestFocus()
        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(1)

        // Check that the initial output transformation is applied.
        rule.onNodeWithTag(Tag).assertTextEquals("Hello World")

        // Lose window focus.
        windowInfo =
            object : WindowInfo {
                override val isWindowFocused = false
            }
        rule.waitForIdle()

        // Change the output transformation while window is not focused.
        outputTransformation = OutputTransformation { append(" Compose") }
        rule.waitForIdle()

        // A new session should start, even if window is not focused, because the
        // session was active.
        inputMethodInterceptor.assertSessionActive()
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(2)

        // Check that the new output transformation is applied visually.
        rule.onNodeWithTag(Tag).assertTextEquals("Hello Compose")

        // Gain window focus back.
        windowInfo =
            object : WindowInfo {
                override val isWindowFocused = true
            }
        rule.waitForIdle()

        // Session count should still be 2.
        inputMethodInterceptor.assertThatSessionCount().isEqualTo(2)
        rule.onNodeWithTag(Tag).assertTextEquals("Hello Compose")
    }

    private fun requestFocus(tag: String) = rule.onNodeWithTag(tag).requestFocus()

    private fun assertTextSelection(expected: TextRange) {
        val selection =
            rule.onNodeWithTag(Tag).fetchSemanticsNode().config.getOrNull(TextSelectionRange)
        assertThat(selection).isEqualTo(expected)
    }

    private fun InputConnection.commitText(text: String) {
        beginBatchEdit()
        finishComposingText()
        commitText(text, 1)
        endBatchEdit()
    }

    private object RejectAllTextFilter : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            revertAllChanges()
        }
    }

    private class KeyboardOptionsFilter(override val keyboardOptions: KeyboardOptions) :
        InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            // Noop
        }
    }
}

/**
 * Checks whether the given image is horizontally symmetrical where a region that has the width of
 * [excludedWidth] around the center is excluded.
 */
private fun ImageBitmap.assertHorizontallySymmetrical(excludedWidth: Int) {
    val pixel = toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until (width - excludedWidth) / 2) {
            val leftPixel = pixel[x, y]
            pixel.assertPixelColor(leftPixel, width - 1 - x, y)
        }
    }
}
